package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IArcType;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseArc;
import edu.kit.ipd.parse.luna.graph.ParseGraph;
import edu.kit.ipd.parse.luna.pipeline.IPipelineStage;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

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

	private static final String NEXT_ARCTYPE_NAME = "relation";

	private static final String SRL_ARCTYPE_NAME = "srl";

	private static final String INSTRUCTION_NUMBER_VALUE_NAME = "instructionNumber";

	private static final String ROLE_VALUE_NAME = "role";

	private static final String NEXT_VALUE_NAME = "value";

	private static final String TOKEN_WORD_VALUE_NAME = "value";

	private static final String CORRESPONDING_VERB = "correspondingVerb";

	private static final String IOBES = "IOBES";

	@Override
	public void init() {
		props = ConfigManager.getConfiguration(getClass());
		parsePerInstruction = Boolean.parseBoolean(props.getProperty("PARSE_PER_INSTRUCTION"));

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
			List<SRLToken> input;
			if (graph instanceof ParseGraph) {
				ParseGraph pGraph = (ParseGraph) graph;
				input = generateInputList(pGraph);

				List<List<SRLToken>> result = parse(input);
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
	 * This method parses the specified {@link Token}s with SENNA and returns
	 * the contained words associated with their srl Tags
	 * 
	 * @param tokens
	 *            The {@link Token}s to process
	 * @return the words and their srlTags as {@link WordSrlType}
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
	public List<List<SRLToken>> parse(List<SRLToken> tokens) throws IOException, URISyntaxException, InterruptedException {
		List<List<SRLToken>> result = new ArrayList<List<SRLToken>>();
		Senna senna = new Senna();
		if (parsePerInstruction) {
			logger.info("parsing SRL for each instruction independently");
			List<String> inputList = generateInstructionInput(tokens);

			int instructionNumber = 0;
			for (String input : inputList) {
				File inputTmpFile = writeToTempFile(input);
				result.add(senna.parse(inputTmpFile, instructionNumber));
				instructionNumber++;
			}
		} else {
			String input = "";
			for (SRLToken t : tokens) {
				input += t.getWord() + " ";
			}
			File inputTmpFile = writeToTempFile(input);
			logger.info("parsing SRL without instructions");
			result.add(senna.parse(inputTmpFile, -1));
		}
		return result;
	}

	private void putResultIntoGraph(List<List<SRLToken>> result, ParseGraph pGraph) {

		//Prepare arc and token type
		IArcType arcType = pGraph.getArcType(SRL_ARCTYPE_NAME);
		arcType.addAttributeToType("String", ROLE_VALUE_NAME);
		arcType.addAttributeToType("String", IOBES);
		arcType.addAttributeToType("String", CORRESPONDING_VERB);

		INode current = pGraph.getFirstUtteranceNode();
		for (List<SRLToken> instruction : result) {

			// get verb representing nodes
			List<SRLToken> verbTokens = getVerbTokensOfInstruction(instruction);
			INode[] verbNodes = getVerbNodesOfInstruction(instruction, pGraph, current, verbTokens);

			//iterate over token in instruction
			ListIterator<SRLToken> iterator = instruction.listIterator();
			while (iterator.hasNext()) {
				SRLToken token = iterator.next();
				INode nodeForToken = firstMatchingNode(current, token, pGraph);

				// treat each verb separately
				for (int i = 0; i < verbNodes.length; i++) {

					// case token is Single semantic role
					if (token.getSrls().get(i + 1).startsWith("S-") && token.getSrls().get(0).equals("-")) {
						createSRLArc(verbNodes[i], nodeForToken, arcType, pGraph, "S", verbTokens, token, i);

						// case token is beginning of semantic role sequence
					} else if (token.getSrls().get(i + 1).startsWith("B-")) {
						createSRLArc(verbNodes[i], nodeForToken, arcType, pGraph, "B", verbTokens, token, i);

						addArcToNextNotOutsideNode(pGraph, arcType, verbTokens, iterator, nodeForToken, i);

						// case token is inside semantic role sequence
					} else if (token.getSrls().get(i + 1).startsWith("I-")) {
						addArcToNextNotOutsideNode(pGraph, arcType, verbTokens, iterator, nodeForToken, i);
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

	private void addArcToNextNotOutsideNode(ParseGraph pGraph, IArcType arcType, List<SRLToken> verbTokens, ListIterator<SRLToken> iterator,
			INode nodeForToken, int i) {
		// add arc to next node in sequence
		if (iterator.hasNext()) {
			SRLToken next = iterator.next();
			INode nextNode = firstMatchingNode(nodeForToken, next, pGraph);

			// next node is inside sequence
			if (next.getSrls().get(i + 1).startsWith("I-")) {
				createSRLArc(nodeForToken, nextNode, arcType, pGraph, "I", verbTokens, next, i);

				// next node is outside sequence -> search first following node which is not outside
			} else if (next.getSrls().get(i + 1).startsWith("O")) {
				while (iterator.hasNext()) {
					SRLToken further = iterator.next();
					INode furtherNode = firstMatchingNode(nextNode, further, pGraph);
					if (further.getSrls().get(i + 1).startsWith("I-")) {
						createSRLArc(nodeForToken, furtherNode, arcType, pGraph, "I", verbTokens, further, i);

						break;
					} else if (further.getSrls().get(i + 1).startsWith("E-")) {
						createSRLArc(nodeForToken, furtherNode, arcType, pGraph, "E", verbTokens, further, i);

						break;
					}

				}
				// next node is end of sequence
			} else if (next.getSrls().get(i + 1).startsWith("E-")) {
				createSRLArc(nodeForToken, nextNode, arcType, pGraph, "E", verbTokens, next, i);

			}

			// reset iterator
			iterator.previous();
		}
	}

	private void createSRLArc(INode from, INode to, IArcType type, ParseGraph pGraph, String iobes, List<SRLToken> verbTokens,
			SRLToken token, int verbNumber) {
		ParseArc arc = pGraph.createArc(from, to, type);
		arc.setAttributeValue(ROLE_VALUE_NAME, token.getSrls().get(verbNumber + 1).substring(2));
		arc.setAttributeValue(IOBES, iobes);
		arc.setAttributeValue(CORRESPONDING_VERB, verbTokens.get(verbNumber).getSrls().get(0));
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

	private INode firstMatchingNode(INode beginning, SRLToken token, ParseGraph pGraph) {
		INode current = beginning;
		IArcType arcType = pGraph.getArcType(NEXT_ARCTYPE_NAME);
		Set<? extends IArc> outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
		if (current.getAttributeValue(NEXT_VALUE_NAME).equals(token.getWord())) {
			return current;
		} else {
			while (!outgoingNextArcs.isEmpty()) {
				current = getNextNode(current, pGraph);
				if (current.getAttributeValue(NEXT_VALUE_NAME).equals(token.getWord())) {
					return current;
				}
				outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
			}
		}
		return null;

	}

	private INode[] getVerbNodesOfInstruction(List<SRLToken> instruction, ParseGraph pGraph, INode current, List<SRLToken> verbTokens) {
		int numberOfVerbs = verbTokens.size();
		INode[] verbNodes = new INode[numberOfVerbs];
		INode beginning = current;
		for (int i = 0; i < numberOfVerbs; i++) {
			verbNodes[i] = firstMatchingNode(beginning, verbTokens.get(i), pGraph);
			beginning = verbNodes[i];
		}
		return verbNodes;
	}

	private List<SRLToken> getVerbTokensOfInstruction(List<SRLToken> instruction) {
		List<SRLToken> verbs = new ArrayList<SRLToken>();
		for (SRLToken token : instruction) {
			if (!token.getSrls().get(0).equals("-") && isSingleOrBeginning(token.getSrls().get(1))) {
				verbs.add(token);
			}
		}
		return verbs;
	}

	private List<SRLToken> generateInputList(ParseGraph pGraph)
			throws PipelineStageException, IOException, URISyntaxException, InterruptedException {
		List<SRLToken> input = new ArrayList<>();
		INode first = pGraph.getFirstUtteranceNode();
		if (first != null) {
			INode act = first;

			do {
				if (act.getAttributeNames().contains(TOKEN_WORD_VALUE_NAME)
						&& act.getAttributeNames().contains(INSTRUCTION_NUMBER_VALUE_NAME)) {
					SRLToken token = new SRLToken(act.getAttributeValue(TOKEN_WORD_VALUE_NAME).toString(),
							Integer.parseInt(act.getAttributeValue(INSTRUCTION_NUMBER_VALUE_NAME).toString()));
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

	private List<String> generateInstructionInput(List<SRLToken> tokens) {
		List<String> inputList = new ArrayList<String>();
		int instructionNumber = 0;
		String instruction = "";
		for (SRLToken t : tokens) {

			if (t.getInstructionNumber() > instructionNumber) {
				inputList.add(instruction);
				instruction = "";
			}
			instruction += t.getWord() + " ";
			instructionNumber = t.getInstructionNumber();
		}
		inputList.add(instruction);
		return inputList;
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
