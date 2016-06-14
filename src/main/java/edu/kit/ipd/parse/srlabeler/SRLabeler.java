package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.data.AbstractPipelineData;
import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PipelineDataCastException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IArcType;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseArc;
import edu.kit.ipd.parse.luna.graph.ParseGraph;
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
 * @author Tobias Hey
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

	private static final String NEXT_ARCTYPE_NAME = "relation";

	private static final String NEXT_VALUE_NAME = "value";

	private static final String TOKEN_WORD_VALUE_NAME = "value";

	private static final String INSTRUCTION_NUMBER_VALUE_NAME = "instructionNumber";

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
		senna = new Senna(new String[] { "-srl" });
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
			IGraph graph = prePipeData.getGraph();
			List<String[]> input;
			if (graph instanceof ParseGraph) {
				ParseGraph pGraph = (ParseGraph) graph;
				input = generateInputList(pGraph);

				List<List<WordSennaResult>> result = parse(input);
				putResultIntoGraph(result, pGraph);

			} else {
				logger.error("ParseGraph object expected but not provided.");
				throw new IllegalArgumentException("ParseGraph expected but not provided.");
			}

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

	/**
	 * This method parses the specified tokens with SENNA and returns the
	 * contained words associated with their srl Tags
	 * 
	 * @param tokens
	 *            The tokens to process
	 * @return the words and their srlTags as {@link WordSennaResult}
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
	public List<List<WordSennaResult>> parse(List<String[]> tokens) throws IOException, URISyntaxException, InterruptedException {
		List<List<WordSennaResult>> result = new ArrayList<List<WordSennaResult>>();

		if (parsePerInstruction) {
			logger.info("parsing SRL for each instruction independently");
			List<String> inputList = generateInstructionInput(tokens);

			for (String input : inputList) {
				File inputTmpFile = writeToTempFile(input);
				result.add(senna.parse(inputTmpFile));

			}
		} else {
			String input = "";
			for (String[] t : tokens) {
				input += t[0] + " ";
			}
			File inputTmpFile = writeToTempFile(input);
			logger.info("parsing SRL without instructions");
			result.add(senna.parse(inputTmpFile));
		}
		return result;
	}

	private void putResultIntoGraph(List<List<WordSennaResult>> result, ParseGraph pGraph) {

		//Prepare arc type
		IArcType arcType;
		if (!pGraph.hasArcType(SRL_ARCTYPE_NAME)) {
			arcType = pGraph.createArcType(SRL_ARCTYPE_NAME);
			arcType.addAttributeToType("String", ROLE_VALUE_NAME);
			arcType.addAttributeToType("String", IOBES);
			arcType.addAttributeToType("String", PROPBANK_ROLE_DESCRIPTION);
			arcType.addAttributeToType("String", VN_ROLE_NAME);
			arcType.addAttributeToType("String", FN_ROLE_NAME);
			arcType.addAttributeToType("float", ROLE_CONFIDENCE_NAME);
			arcType.addAttributeToType("String", CORRESPONDING_VERB);
			arcType.addAttributeToType("String", PROP_BANK_ROLESET_ID);
			arcType.addAttributeToType("String", PROP_BANK_ROLESET_DESCR);
			arcType.addAttributeToType("String", VERB_NET_FRAMES);
			arcType.addAttributeToType("String", FRAME_NET_FRAMES);
			arcType.addAttributeToType("String", EVENT_TYPES);

		} else {
			arcType = pGraph.getArcType(SRL_ARCTYPE_NAME);
		}
		INode current = pGraph.getFirstUtteranceNode();
		for (List<WordSennaResult> instruction : result) {

			// get verb representing nodes
			List<WordSennaResult> verbTokens = getVerbTokensOfInstruction(instruction);
			INode[] verbNodes = getVerbNodesOfInstruction(instruction, pGraph, current, verbTokens);

			// get argument numbers per Verb

			List<Set<String>> totalArgNumbersPerVerb = new ArrayList<Set<String>>(verbTokens.size());

			for (int i = 0; i < verbTokens.size(); i++) {
				Set<String> argNumbers = new HashSet<String>();
				for (WordSennaResult token : instruction) {
					String role = token.getAnalysisResults()[i + 1];
					if (role.contains("-A") && role.substring(3).matches("\\d")) {

						argNumbers.add(role.substring(3));

					}
				}
				totalArgNumbersPerVerb.add(argNumbers);
			}

			//iterate over token in instruction
			ListIterator<WordSennaResult> iterator = instruction.listIterator();
			while (iterator.hasNext()) {
				WordSennaResult token = iterator.next();
				INode nodeForToken = getFirstMatchingNode(current, token, pGraph);

				// treat each verb separately
				for (int i = 0; i < verbNodes.length; i++) {

					// case token is Single semantic role
					if (token.getAnalysisResults()[i + 1].startsWith("S-")) {
						createSRLArc(verbNodes[i], nodeForToken, arcType, pGraph, "S", verbTokens, token, i, totalArgNumbersPerVerb.get(i));

						// case token is beginning of semantic role sequence
					} else if (token.getAnalysisResults()[i + 1].startsWith("B-")) {
						createSRLArc(verbNodes[i], nodeForToken, arcType, pGraph, "B", verbTokens, token, i, totalArgNumbersPerVerb.get(i));

						addArcToNextNotOutsideNode(pGraph, arcType, verbTokens, iterator, nodeForToken, i, totalArgNumbersPerVerb.get(i));

						// case token is inside semantic role sequence
					} else if (token.getAnalysisResults()[i + 1].startsWith("I-")) {
						addArcToNextNotOutsideNode(pGraph, arcType, verbTokens, iterator, nodeForToken, i, totalArgNumbersPerVerb.get(i));
					}
				}
				INode next = getNextNode(nodeForToken, pGraph);
				if (next != null) {
					current = next;
				} else {
					current = nodeForToken;
				}
			}

		}

	}

	private void addArcToNextNotOutsideNode(ParseGraph pGraph, IArcType arcType, List<WordSennaResult> verbTokens,
			ListIterator<WordSennaResult> iterator, INode nodeForToken, int i, Set<String> argNumbers) {
		// add arc to next node in sequence
		if (iterator.hasNext()) {
			WordSennaResult next = iterator.next();
			INode nextNode = getFirstMatchingNode(getNextNode(nodeForToken, pGraph), next, pGraph);

			// next node is inside sequence
			if (next.getAnalysisResults()[i + 1].startsWith("I-")) {
				createSRLArc(nodeForToken, nextNode, arcType, pGraph, "I", verbTokens, next, i, argNumbers);

				// next node is outside sequence -> search first following node which is not outside
			} else if (next.getAnalysisResults()[i + 1].startsWith("O")) {
				while (iterator.hasNext()) {
					WordSennaResult further = iterator.next();
					INode furtherNode = getFirstMatchingNode(nextNode, further, pGraph);
					if (further.getAnalysisResults()[i + 1].startsWith("I-")) {
						createSRLArc(nodeForToken, furtherNode, arcType, pGraph, "I", verbTokens, further, i, argNumbers);

						break;
					} else if (further.getAnalysisResults()[i + 1].startsWith("E-")) {
						createSRLArc(nodeForToken, furtherNode, arcType, pGraph, "E", verbTokens, further, i, argNumbers);

						break;
					}

				}
				// next node is end of sequence
			} else if (next.getAnalysisResults()[i + 1].startsWith("E-")) {
				createSRLArc(nodeForToken, nextNode, arcType, pGraph, "E", verbTokens, next, i, argNumbers);

			}

			// reset iterator
			iterator.previous();
		}
	}

	private void createSRLArc(INode from, INode to, IArcType type, ParseGraph pGraph, String iobes, List<WordSennaResult> verbTokens,
			WordSennaResult token, int verbNumber, Set<String> totalArgNumbers) {
		ParseArc arc = pGraph.createArc(from, to, type);
		String role = token.getAnalysisResults()[verbNumber + 1].substring(2);
		arc.setAttributeValue(ROLE_VALUE_NAME, role);
		arc.setAttributeValue(IOBES, iobes);
		//TODO Lemmatize Verb
		String verb = verbTokens.get(verbNumber).getAnalysisResults()[0];
		arc.setAttributeValue(CORRESPONDING_VERB, verb);
		String roleNumber = role.substring(1);
		if (!roleNumber.matches("\\d")) {
			roleNumber = "";
		}
		ArrayList<RolesetConfidence> rsConfidences = pbMapper.getPossibleRolesets(verb, totalArgNumbers);

		if (!rsConfidences.isEmpty()) {
			RolesetConfidence rsC = rsConfidences.get(0);
			Roleset rs = rsC.getRoleset();
			if (!(roleNumber.equals(""))) {
				arc.setAttributeValue(PROPBANK_ROLE_DESCRIPTION, rs.getRoles().get(roleNumber).getDescr());
				arc.setAttributeValue(VN_ROLE_NAME, Arrays.toString(rs.getRoles().get(roleNumber).getVnRoles()));
				arc.setAttributeValue(FN_ROLE_NAME, Arrays.toString(rs.getRoles().get(roleNumber).getFnRoles()));
			}
			arc.setAttributeValue(ROLE_CONFIDENCE_NAME, rsC.getConfidence());
			arc.setAttributeValue(PROP_BANK_ROLESET_DESCR, rs.getDescr());
			arc.setAttributeValue(EVENT_TYPES, Arrays.toString(rs.getEventTypes()));
			arc.setAttributeValue(FRAME_NET_FRAMES, Arrays.toString(rs.getFnFrames()));
			arc.setAttributeValue(PROP_BANK_ROLESET_ID, rs.getId());
			arc.setAttributeValue(VERB_NET_FRAMES, Arrays.toString(rs.getVnFrames()));
		}
	}

	private boolean isSingleOrBeginning(String srl) {
		return (srl.startsWith("S-") || srl.startsWith("B-"));
	}

	private INode getNextNode(INode current, ParseGraph pGraph) {
		IArcType arcType = pGraph.getArcType(NEXT_ARCTYPE_NAME);
		Set<? extends IArc> outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
		if (!outgoingNextArcs.isEmpty()) {
			return outgoingNextArcs.toArray(new IArc[outgoingNextArcs.size()])[0].getTargetNode();
		} else {
			return null;
		}
	}

	private INode getFirstMatchingNode(INode beginning, WordSennaResult token, ParseGraph pGraph) {
		INode current = beginning;
		IArcType arcType = pGraph.getArcType(NEXT_ARCTYPE_NAME);
		Set<? extends IArc> outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
		if (current.getAttributeValue(TOKEN_WORD_VALUE_NAME).equals(token.getWord())) {
			return current;
		} else {
			while (!outgoingNextArcs.isEmpty()) {
				current = getNextNode(current, pGraph);
				if (current.getAttributeValue(TOKEN_WORD_VALUE_NAME).equals(token.getWord())) {
					return current;
				}
				outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
			}
		}
		return null;

	}

	private INode[] getVerbNodesOfInstruction(List<WordSennaResult> instruction, ParseGraph pGraph, INode current,
			List<WordSennaResult> verbTokens) {
		int numberOfVerbs = verbTokens.size();
		INode[] verbNodes = new INode[numberOfVerbs];
		INode beginning = current;
		for (int i = 0; i < numberOfVerbs; i++) {
			verbNodes[i] = getFirstMatchingNode(beginning, verbTokens.get(i), pGraph);
			beginning = verbNodes[i];
		}
		return verbNodes;
	}

	private List<WordSennaResult> getVerbTokensOfInstruction(List<WordSennaResult> instruction) {
		List<WordSennaResult> verbs = new ArrayList<WordSennaResult>();
		for (WordSennaResult token : instruction) {
			if (!token.getAnalysisResults()[0].equals("-") && isSingleOrBeginning(token.getAnalysisResults()[1])) {
				verbs.add(token);
			}
		}
		return verbs;
	}

	private List<String> generateInstructionInput(List<String[]> tokens) {
		List<String> inputList = new ArrayList<String>();
		int instructionNumber = 0;
		String instruction = "";
		for (String[] t : tokens) {

			if (Integer.parseInt(t[1]) > instructionNumber) {
				inputList.add(instruction);
				instruction = "";
			}
			instruction += t[0] + " ";
			instructionNumber = Integer.parseInt(t[1]);
		}
		inputList.add(instruction);
		return inputList;
	}

	private List<String[]> generateInputList(ParseGraph pGraph)
			throws PipelineStageException, IOException, URISyntaxException, InterruptedException {
		List<String[]> input = new ArrayList<>();
		INode first = pGraph.getFirstUtteranceNode();
		if (first != null) {
			INode act = first;

			do {
				if (act.getAttributeNames().contains(TOKEN_WORD_VALUE_NAME)
						&& act.getAttributeNames().contains(INSTRUCTION_NUMBER_VALUE_NAME)) {
					String[] token = new String[] { act.getAttributeValue(TOKEN_WORD_VALUE_NAME).toString(),
							act.getAttributeValue(INSTRUCTION_NUMBER_VALUE_NAME).toString() };
					input.add(token);
					act = getNextNode(act, pGraph);

				} else {
					logger.error("Token node does not contain words or instructionNumber");
					throw new PipelineStageException("Token node does not contain words or instructionNumber");
				}
			} while (act != null);

		} else {
			logger.error("Graph contains no first utterance node");
			throw new PipelineStageException("Graph contains no first utterance node");
		}
		return input;
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
