package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
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

/**
 * This class represents a {@link IPipelineStage} to annotate the previously
 * processed input sequences with their semantic roles.
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

	private PropBankMapper pbMapper;

	private Senna senna;

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

	@Override
	public void init() {
		props = ConfigManager.getConfiguration(getClass());
		parsePerInstruction = Boolean.parseBoolean(props.getProperty("PARSE_PER_INSTRUCTION"));
		pbMapper = new PropBankMapper();
		senna = new Senna(new String[] { "-usrtokens", "-srl" });
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
			Token[] tokens = prePipeData.getTokens();
			if (tokens.length > 0) {

				List<Token[]> instructionTokens = getInstructionTokens(tokens);

				List<List<WordSennaResult>> result = parseWithTokens(instructionTokens);

				if ((parsePerInstruction && instructionTokens.size() == result.size()) || (!parsePerInstruction && !result.isEmpty())) {

					Iterator<Token[]> tokenIt = instructionTokens.iterator();
					Iterator<List<WordSennaResult>> resultIt = result.iterator();
					while (resultIt.hasNext()) {

						List<WordSennaResult> instructionResult = resultIt.next();

						Token[] instruction;

						if (parsePerInstruction) {

							// process each instruction independently
							instruction = tokenIt.next();
						} else {
							instruction = tokens;
						}
						int index;
						List<Pair<String, Token>> verbTokens = extractVerbTokens(instructionResult, instruction);

						// for each recognized Verb
						for (int i = 0; i < verbTokens.size(); i++) {

							HashMap<String, List<Token>> roleTokens = extractRoleTokens(instructionResult, instruction, i);
							Token verbToken = verbTokens.get(i).getRight();
							String verb = verbTokens.get(i).getLeft();

							SRLToken srlToken = createSRLToken(roleTokens, verbToken, verb);

							// replace token with srl Token
							for (int j = 0; j < tokens.length; j++) {
								if (tokens[j].equals(verbToken)) {
									tokens[j] = srlToken;
								}
							}
						}

					}
				}
			}

			prePipeData.setTokens(tokens);

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

	public List<List<WordSennaResult>> parseWithTokens(List<Token[]> tokens) throws IOException, URISyntaxException, InterruptedException {
		List<List<WordSennaResult>> result = new ArrayList<List<WordSennaResult>>();

		if (parsePerInstruction) {
			logger.info("parsing SRL for each instruction independently");
			for (Token[] instruction : tokens) {
				String input = generateInstructionInputFromTokens(instruction);
				File inputTmpFile = writeToTempFile(input);
				result.add(senna.parse(inputTmpFile));
			}
		} else {
			logger.info("parsing SRL without instructions");
			String input = "";
			for (Token[] inst : tokens) {
				for (int i = 0; i < inst.length; i++) {
					input += inst[i].getWord() + " ";
				}
			}
			File inputTmpFile = writeToTempFile(input);

			result.add(senna.parse(inputTmpFile));
		}
		return result;
	}

	private SRLToken createSRLToken(HashMap<String, List<Token>> roleTokens, Token verbToken, String verb) {
		// calculate occurring role numbers
		Set<String> totalRoleNumbers = new HashSet<>();
		for (String role : roleTokens.keySet()) {
			if (role.startsWith("A")) {
				totalRoleNumbers.add(role.substring(1));
			}
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
					srlToken.addDependendToken(role, token);
				}
				if (role.startsWith("A") && rs.getRoles().containsKey(role.substring(1))) {
					srlToken.addRoleDescription(role, rs.getRoles().get(role.substring(1)).getDescr(),
							rs.getRoles().get(role.substring(1)).getVnRoles(), rs.getRoles().get(role.substring(1)).getFnRoles());
				}
			}

		}
		return srlToken;
	}

	private HashMap<String, List<Token>> extractRoleTokens(List<WordSennaResult> instructionResult, Token[] instruction, int i) {
		int index;
		HashMap<String, List<Token>> correspondingTokens = new HashMap<>();
		index = 0;
		// search Tokens belonging to semantic roles
		for (WordSennaResult wordSennaResult : instructionResult) {
			if (!wordSennaResult.getAnalysisResults()[i + 1].equals("O") && !wordSennaResult.getAnalysisResults()[i + 1].equals("-")) {
				String role = wordSennaResult.getAnalysisResults()[i + 1].substring(2);
				if (correspondingTokens.containsKey(role)) {
					correspondingTokens.get(role).add(instruction[index]);
				} else {
					List<Token> list = new ArrayList<Token>();
					list.add(instruction[index]);
					correspondingTokens.put(role, list);
				}

			}
			index++;
		}
		return correspondingTokens;
	}

	private List<Pair<String, Token>> extractVerbTokens(List<WordSennaResult> instructionResult, Token[] instruction) {
		List<Pair<String, Token>> verbTokens = new ArrayList<>();

		// search recognized verbs
		int index = 0;
		for (WordSennaResult wordSennaResult : instructionResult) {
			if (!wordSennaResult.getAnalysisResults()[0].equals("-") && isSingleOrBeginning(wordSennaResult.getAnalysisResults()[1])) {
				verbTokens.add(new Pair<String, Token>(wordSennaResult.getAnalysisResults()[0], instruction[index]));
			}
			index++;
		}
		return verbTokens;
	}

	private List<Token[]> getInstructionTokens(Token[] tokens) {
		List<Token[]> result = new ArrayList<>();
		int instructionNumber = 0;

		boolean foundToken = false;
		do {
			foundToken = false;
			List<Token> instructionTokens = new ArrayList<>();
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].getInstructionNumber() == instructionNumber) {
					instructionTokens.add(tokens[i]);

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
				result.add(instructionTokens.toArray(new Token[instructionTokens.size()]));
				foundToken = true;
			}

			instructionNumber++;
		} while (foundToken);
		return result;
	}

	private String generateInstructionInputFromTokens(Token[] tokens) {
		String resultString = "";
		for (Token token : tokens) {
			resultString += token.getWord() + " ";
		}

		return resultString;
	}

	private boolean isSingleOrBeginning(String srl) {
		return (srl.startsWith("S-") || srl.startsWith("B-"));
	}

	/**
	 * This method writes the input text into a text file. The text file is the
	 * input file for SENNA.
	 * 
	 * @param text
	 *            the text to parse
	 * @throws IOException
	 */
	private File writeToTempFile(String text) throws IOException {
		PrintWriter writer;
		File tempFile = File.createTempFile("input", "txt");
		writer = new PrintWriter(tempFile);
		writer.println(text);
		writer.close();
		return tempFile;

	}

	@Override
	public String getID() {
		return ID;
	}

}
