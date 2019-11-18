package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.data.AbstractPipelineData;
import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PipelineDataCastException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.data.token.POSTag;
import edu.kit.ipd.parse.luna.data.token.SRLToken;
import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.Pair;
import edu.kit.ipd.parse.luna.pipeline.IPipelineStage;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.luna.tools.ConfigManager;
import edu.kit.ipd.parse.senna_wrapper.Senna;
import edu.kit.ipd.parse.senna_wrapper.WordSennaResult;
import edu.kit.ipd.parse.srlabeler.propbank.PropBankMapper;
import edu.kit.ipd.parse.srlabeler.propbank.Roleset;
import edu.kit.ipd.parse.srlabeler.propbank.RolesetConfidence;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

/**
 * This class represents a {@link IPipelineStage} to annotate the previously
 * processed input sequences with their semantic roles. Note: Senna expects
 * OS-dependent newline chars for the input file, but LF for the verb files.
 *
 * @author Tobias Hey - (04.01.2017) updated to fit new framework (passing
 *         SRLToken)
 *
 */
@MetaInfServices(IPipelineStage.class)
public class SRLabeler implements IPipelineStage {

	private static final Logger logger = LoggerFactory.getLogger(SRLabeler.class);

	private static final String ID = "srl";

	private Properties props;

	private PrePipelineData prePipeData;

	private boolean parsePerInstruction;
	private boolean usePosTaggerVerbs;

	private PropBankMapper pbMapper;

	private Senna senna;

	private Dictionary wordNetDictionary;

	public static final String NEXT_ARCTYPE_NAME = "relation";
	public static final String TOKEN_WORD_VALUE_NAME = "value";
	public static final String INSTRUCTION_NUMBER_VALUE_NAME = "instructionNumber";
	public static final String SRL_ARCTYPE_NAME = "srl";
	public static final String ROLE_VALUE_NAME = "role";
	public static final String CORRESPONDING_VERB = "correspondingVerb";
	public static final String VN_ROLE_NAME = "vnRole";
	public static final String ROLE_CONFIDENCE_NAME = "roleConfidence";
	public static final String IOBES = "IOBES";
	public static final String PROPBANK_ROLE_DESCRIPTION = "pbRole";
	public static final String EVENT_TYPES = "eventTypes";
	public static final String FRAME_NET_FRAMES = "frameNetFrames";
	public static final String VERB_NET_FRAMES = "verbNetFrames";
	public static final String PROP_BANK_ROLESET_DESCR = "propBankRolesetDescr";
	public static final String PROP_BANK_ROLESET_ID = "propBankRolesetID";
	public static final String FN_ROLE_NAME = "fnRole";
	public static final String POS_FILE_NAME = "pos";

	private static final List<String> PUNCTUATION_MARKS = List.of(".", ":", ",", ";", "!", "?");

	@Override
	public void init() {
		props = ConfigManager.getConfiguration(getClass());
		parsePerInstruction = Boolean.parseBoolean(props.getProperty("PARSE_PER_INSTRUCTION"));
		usePosTaggerVerbs = Boolean.parseBoolean(props.getProperty("USE_POS_TAGGER_VERBS"));
		pbMapper = new PropBankMapper();
		if (usePosTaggerVerbs) {
			senna = null; //init later
			//senna = new Senna(new String[] { "-usrtokens", "-srl", "-usrvbs " + POS_FILE_NAME + ".txt" });
		} else {
			senna = new Senna(new String[] { "-usrtokens", "-srl" });
		}
		try {
			InputStream is = getClass().getResourceAsStream(Dictionary.DEFAULT_RESOURCE_CONFIG_PATH);
			wordNetDictionary = Dictionary.getInstance(is);
		} catch (JWNLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exec(AbstractPipelineData data) throws PipelineStageException {

		try {
			prePipeData = data.asPrePipelineData();
		} catch (PipelineDataCastException e) {
			logger.error("Cannot process on data - PipelineData unreadable", e);
			throw new PipelineStageException(e);
		}

		try {

			List<List<Token>> taggedHyp = prePipeData.getTaggedHypotheses();
			List<Token> tokens = taggedHyp.get(0);

			if (tokens.size() > 0) {

				List<List<Token>> instructionTokens = getInstructionTokens(tokens);

				List<List<WordSennaResult>> result = parseWithTokens(instructionTokens);

				if (parsePerInstruction && (instructionTokens.size() == result.size() || containsPunctuation(instructionTokens))
						|| (!parsePerInstruction && !result.isEmpty())) {

					Iterator<List<Token>> tokenIt = instructionTokens.iterator();
					Iterator<List<WordSennaResult>> resultIt = result.iterator();
					while (resultIt.hasNext()) {

						List<WordSennaResult> instructionResult = resultIt.next();

						List<Token> instruction;

						if (parsePerInstruction) {

							// process each instruction independently
							instruction = tokenIt.next();
						} else {
							instruction = tokens;
						}

						List<Pair<String, Token>> verbTokens = extractVerbTokens(instructionResult, instruction);

						// for each recognized Verb
						for (int i = 0; i < verbTokens.size(); i++) {

							HashMap<String, List<Token>> roleTokens = extractRoleTokens(instructionResult, instruction, i);
							Token verbToken = verbTokens.get(i).getRight();
							String verb = verbTokens.get(i).getLeft();

							SRLToken srlToken = createSRLToken(roleTokens, verbToken, verb);

							// replace token with srl Token
							for (int j = 0; j < tokens.size(); j++) {
								if (tokens.get(j).equals(verbToken)) {
									tokens.set(j, srlToken);
								}
							}
						}

					}
				}
			}

			taggedHyp.set(0, tokens);
			prePipeData.setTaggedHypotheses(taggedHyp);

		} catch (MissingDataException e) {
			logger.error("No graph to process, aborting ...", e);
			throw new PipelineStageException(e);
		} catch (IOException e) {
			logger.error("An IOException occured during run of SENNA", e);
			throw new PipelineStageException(e);
		} catch (URISyntaxException e) {
			logger.error("An URISyntaxException occured during initialization of SENNA", e);
			throw new PipelineStageException(e);
		} catch (InterruptedException e) {
			logger.error("The SENNA process interrupted unexpectedly", e);
			throw new PipelineStageException(e);
		}

	}

	public List<List<WordSennaResult>> parseWithTokens(List<List<Token>> tokens)
			throws IOException, URISyntaxException, InterruptedException {
		List<List<WordSennaResult>> result = new ArrayList<List<WordSennaResult>>();

		if (parsePerInstruction) {
			logger.info("parsing SRL for each instruction independently");
			List<Integer> addedSeperatorIndices = new ArrayList<>();
			File inputTmpFile = writeBatchToTempFile("input", tokens, addedSeperatorIndices);
			if (usePosTaggerVerbs) {
				String pos = "";
				for (List<Token> tokenList : tokens) {
					for (Token token : tokenList) {
						if (token.getPos().isVerb()) {
							pos += "VB" + "\n";
						} else {
							pos += "-" + "\n";
						}
					}
					pos += "-" + "\n" + "\n";
				}
				File posFile = writeToTempFile(POS_FILE_NAME, pos, "\n");
				senna = new Senna(new String[] { "-usrtokens", "-srl", "-usrvbs", posFile.getAbsolutePath() });
			}
			List<WordSennaResult> results = senna.parse(inputTmpFile);
			int countRemoved = 0;
			for (Integer integer : addedSeperatorIndices) {
				results.remove(integer.intValue() - countRemoved);
				countRemoved++;
			}
			List<WordSennaResult> tmpList = new ArrayList<>();
			int offset = 0;
			for (List<Token> inst : tokens) {
				for (int i = 0; i < inst.size() && i + offset < results.size(); i++) {
					tmpList.add(results.get(i + offset));
				}
				result.add(tmpList);
				tmpList = new ArrayList<>();
				offset += inst.size();
			}
			/*
			 * for (WordSennaResult wordSennaResult : results) { if
			 * (wordSennaResult.getWord().equals(".")) { result.add(tmpList); tmpList = new
			 * ArrayList<>(); } else { tmpList.add(wordSennaResult); } }
			 */
		} else {
			logger.info("parsing SRL without instructions");
			String input = "";
			String pos = "";
			for (List<Token> inst : tokens) {
				for (int i = 0; i < inst.size(); i++) {
					input += inst.get(i).getWord() + " ";
					if (usePosTaggerVerbs) {
						if (inst.get(i).getPos().isVerb()) {
							pos += "VB" + "\n";
						} else {
							pos += "-" + "\n";
						}
					}
				}
			}
			File inputTmpFile = writeToTempFile("input", input, System.getProperty("line.seperator"));
			if (usePosTaggerVerbs) {
				File posFile = writeToTempFile(POS_FILE_NAME, pos, "\n");
				senna = new Senna(new String[] { "-usrtokens", "-srl", "-usrvbs", posFile.getAbsolutePath() });
			}
			result.add(senna.parse(inputTmpFile));
		}
		return result;
	}

	private boolean containsPunctuation(List<List<Token>> tokens) {
		for (List<Token> list : tokens) {
			for (Token token : list) {
				if (PUNCTUATION_MARKS.contains(token.getWord().toLowerCase().trim())) {
					return true;
				}
			}
		}
		return false;
	}

	private SRLToken createSRLToken(HashMap<String, List<Token>> roleTokens, Token verbToken, String verb) {
		// calculate occurring role numbers
		Set<String> totalRoleNumbers = new HashSet<>();
		for (String role : roleTokens.keySet()) {
			if (role.matches("[A]\\d")) {
				totalRoleNumbers.add(role.substring(1));
			}
		}

		try {
			IndexWord iw = wordNetDictionary.lookupIndexWord(POS.VERB, verb.toLowerCase());
			if (iw != null) {
				verb = iw.getLemma();
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		}

		// get information from pb, vn and fn
		ArrayList<RolesetConfidence> rsConfidences = pbMapper.getPossibleRolesets(verb, totalRoleNumbers);

		// create srlToken with calculated information
		SRLToken srlToken = new SRLToken(verbToken);
		if (!rsConfidences.isEmpty()) {
			RolesetConfidence rsC = rsConfidences.get(0);
			Roleset rs = rsC.getRoleset();
			srlToken.setCorrespondingVerb(verb);
			srlToken.setRoleConfidence(rsC.getConfidence());
			srlToken.setPbRolesetDescr(rs.getDescr());
			srlToken.setPbRolesetID(rs.getId());
			srlToken.setVnFrames(rs.getVnFrames());
			srlToken.setFnFrames(rs.getFnFrames());
			srlToken.setEventTypes(rs.getEventTypes());
			for (String role : roleTokens.keySet()) {
				for (Token token : roleTokens.get(role)) {
					if (role.equals("V") && token.equals(verbToken)) {
						srlToken.addDependentToken(role, srlToken);
					} else {
						srlToken.addDependentToken(role, token);
					}

				}
				if (role.startsWith("A") && rs.getRoles().containsKey(role.substring(1))) {
					srlToken.addRoleDescription(role, rs.getRoles().get(role.substring(1)).getDescr(),
							rs.getRoles().get(role.substring(1)).getVnRoles(), rs.getRoles().get(role.substring(1)).getFnRoles());
				}
			}

		} else {
			srlToken.setCorrespondingVerb(verb);
			srlToken.addDependentToken("V", srlToken);
		}
		return srlToken;
	}

	private HashMap<String, List<Token>> extractRoleTokens(List<WordSennaResult> instructionResult, List<Token> instruction, int i) {
		int index;
		HashMap<String, List<Token>> correspondingTokens = new HashMap<>();
		index = 0;
		// search Tokens belonging to semantic roles
		for (WordSennaResult wordSennaResult : instructionResult) {
			if (i + 1 < wordSennaResult.getAnalysisResults().length && !wordSennaResult.getAnalysisResults()[i + 1].equals("O")
					&& !wordSennaResult.getAnalysisResults()[i + 1].equals("-")) {
				String role = wordSennaResult.getAnalysisResults()[i + 1].substring(2);
				if (correspondingTokens.containsKey(role)) {
					correspondingTokens.get(role).add(instruction.get(index));
				} else {
					List<Token> list = new ArrayList<Token>();
					list.add(instruction.get(index));
					correspondingTokens.put(role, list);
				}

			}
			index++;
		}
		return correspondingTokens;
	}

	private List<Pair<String, Token>> extractVerbTokens(List<WordSennaResult> instructionResult, List<Token> instruction) {
		List<Pair<String, Token>> verbTokens = new ArrayList<>();

		// search recognized verbs
		int index = 0;
		for (WordSennaResult wordSennaResult : instructionResult) {

			if (wordSennaResult.getAnalysisResults().length >= 2 && !wordSennaResult.getAnalysisResults()[0].equals("-")) {
				boolean isVerbBeginning = false;
				for (String result : wordSennaResult.getAnalysisResults()) {
					if (isSingleOrBeginningVerb(result)) {
						isVerbBeginning = true;
						break;
					}
				}
				if (isVerbBeginning) {
					Token token = instruction.get(index);
					if (isVerb(token.getPos())) {
						verbTokens.add(new Pair<String, Token>(wordSennaResult.getAnalysisResults()[0], instruction.get(index)));
					}
				}
			}
			index++;
		}
		return verbTokens;
	}

	private boolean isVerb(POSTag pos) {
		if (pos.equals(POSTag.VERB) || pos.equals(POSTag.VERB_MODAL) || pos.equals(POSTag.VERB_PARTICIPLE_PAST)
				|| pos.equals(POSTag.VERB_PARTICIPLE_PRESENT) || pos.equals(POSTag.VERB_PAST_TENSE)
				|| pos.equals(POSTag.VERB_SINGULAR_PRESENT_NONTHIRD_PERSON) || pos.equals(POSTag.VERB_SINGULAR_PRESENT_THIRD_PERSON)) {
			return true;
		}
		return false;
	}

	private List<List<Token>> getInstructionTokens(List<Token> tokens) {
		List<List<Token>> result = new ArrayList<>();
		int instructionNumber = 0;

		boolean foundToken = false;
		do {
			foundToken = false;
			List<Token> instructionTokens = new ArrayList<>();
			for (int i = 0; i < tokens.size(); i++) {
				if (tokens.get(i).getInstructionNumber() == instructionNumber) {
					instructionTokens.add(tokens.get(i));

				}
			}
			if (!instructionTokens.isEmpty()) {
				instructionTokens.sort(new Comparator<Token>() {

					@Override
					public int compare(Token o1, Token o2) {
						// TODO Auto-generated method stub
						return Integer.compare(o1.getPosition(), o2.getPosition());
					}
				});
				result.add(instructionTokens);
				foundToken = true;
			}

			instructionNumber++;
		} while (foundToken);
		return result;
	}

	private boolean isSingleOrBeginningVerb(String srl) {
		return srl.startsWith("S-V") || srl.startsWith("B-V");
	}

	/**
	 * This method writes the input text into a text file. The text file is the
	 * input file for SENNA.
	 *
	 * @param text
	 *            the text to parse
	 * @throws IOException
	 */
	private File writeToTempFile(String fileName, String text, String newline) throws IOException {
		PrintWriter writer;
		File tempFile = File.createTempFile(fileName, ".txt");
		writer = new PrintWriter(tempFile);
		writer.print(text + newline);
		writer.close();
		return tempFile;

	}

	private File writeBatchToTempFile(String fileName, List<List<Token>> instructions, List<Integer> addedSeperatorIndices)
			throws IOException {
		PrintWriter writer;
		final File tempFile = File.createTempFile(fileName, ".txt");
		writer = new PrintWriter(tempFile);
		boolean withPunct = containsPunctuation(instructions);
		int i = 0;
		for (final List<Token> instruction : instructions) {

			for (final Token inst : instruction) {
				writer.print(inst.getWord() + " ");
				i++;
			}
			if (!withPunct) {
				writer.println(". ");
				addedSeperatorIndices.add(i);
				i++;
			} else {
				writer.println();
			}
		}
		writer.close();
		return tempFile;
	}

	@Override
	public String getID() {
		return ID;
	}

}
