package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

	private static final String TOKEN_NAME = "token";

	private static final String NEXT_RELATION_NAME = "relation";

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

				System.out.println(input);
				List<List<SRLToken>> result = parse(input);
				putResultIntoGraph(result, pGraph);
				System.out.println(result);
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

	private void putResultIntoGraph(List<List<SRLToken>> result, ParseGraph pGraph) {
		INode current = pGraph.getFirstUtteranceNode();
		for (List<SRLToken> instruction : result) {
			List<SRLToken> verbs = getVerbsOfInstruction(instruction);
			int numberOfVerbs = verbs.size();
			INode[] verbNodes = new INode[numberOfVerbs];
			INode beginning = current;
			for (int i = 0; i < numberOfVerbs; i++) {
				verbNodes[i] = firstMatchingNode(beginning, verbs.get(i), pGraph);
				beginning = verbNodes[i];
			}
			for (SRLToken token : instruction) {
				INode nodeForToken = firstMatchingNode(current, token, pGraph);
				for (int i = 0; i < numberOfVerbs; i++) {
					if (isSingleOrBeginning(token.getSrls().get(i + 1))) {
					//	IArc arc = new ParseArc(verbNodes[i], nodeForToken, )
					} else {
						
					}
				}
			}
		}

	}
	
	private boolean isSingleOrBeginning(String s) {
		return (s.contains("S-") || s.contains("B-"));
	}

	private INode firstMatchingNode(INode beginning, SRLToken token, ParseGraph pGraph) {
		INode current = beginning;
		IArcType arcType = pGraph.getArcType(NEXT_RELATION_NAME);
		Set<? extends IArc> outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
		if (current.getAttributeValue("value").equals(token.getWord())) {
			return current;
		} else {
			while (!outgoingNextArcs.isEmpty()) {
				current = outgoingNextArcs.toArray(new IArc[outgoingNextArcs.size()])[0].getTargetNode();
				if (current.getAttributeValue("value").equals(token.getWord())) {
					return current;
				}
				outgoingNextArcs = current.getOutgoingArcsOfType(arcType);
			}
		}
		return null;

	}

	//	private int getNumberOfVerbs(List<SRLToken> verbs) {
	//		int result = 0;
	//		for (SRLToken token : verbs) {
	//			if (token.getSrls().get(1).contains("S-") || token.getSrls().get(1).contains("B-")) {
	//				result++;
	//			}
	//		}
	//		return 0;
	//	}

	private List<SRLToken> getVerbsOfInstruction(List<SRLToken> instruction) {
		List<SRLToken> verbs = new ArrayList<SRLToken>();
		for (SRLToken token : instruction) {
			if (!token.getSrls().get(0).equals("-") && (token.getSrls().get(1).contains("S-") || token.getSrls().get(1).contains("B-"))) {
				verbs.add(token);
			}
		}
		return verbs;
	}

	private List<List<SRLToken>> getResultPerInstruction(List<SRLToken> result) {
		List<List<SRLToken>> resultPerInstruction = new ArrayList<List<SRLToken>>();
		List<SRLToken> instruction = new ArrayList<SRLToken>();
		if (!result.isEmpty()) {
			int number = result.get(0).getInstructionNumber();
			for (SRLToken token : result) {
				if (token.getInstructionNumber() == number) {
					instruction.add(token);
				} else {
					resultPerInstruction.add(instruction);
					instruction = new ArrayList<SRLToken>();
					instruction.add(token);
					number = token.getInstructionNumber();
				}
			}
			resultPerInstruction.add(instruction);
		}
		return resultPerInstruction;
	}

	private List<SRLToken> generateInputList(ParseGraph pGraph)
			throws PipelineStageException, IOException, URISyntaxException, InterruptedException {
		List<SRLToken> input = new ArrayList<>();
		INode first = pGraph.getFirstUtteranceNode();
		if (first != null) {
			INode act = first;
			boolean hasNext = false;
			do {
				if (act.getAttributeNames().contains("value") && act.getAttributeNames().contains("instructionNumber")) {
					SRLToken token = new SRLToken(act.getAttributeValue("value").toString(),
							Integer.parseInt(act.getAttributeValue("instructionNumber").toString()));
					input.add(token);
					hasNext = false;
					for (IArc arc : act.getOutgoingArcsOfType(pGraph.getArcType(NEXT_RELATION_NAME))) {
						if (arc.getAttributeNames().contains("value") && arc.getAttributeValue("value").equals("NEXT")) {
							hasNext = true;
							act = arc.getTargetNode();
						} else {
							logger.error("Relation arc does not contain NEXT pointer.");
							throw new PipelineStageException("Relation arc does not contain NEXT pointer.");
						}
					}

				} else {
					logger.error("Token node does not contain words or instructionNumber");
					throw new PipelineStageException("Token node does not contain words or instructionNumber");
				}
			} while (hasNext);

		} else {
			logger.error("Graph contains no first utterance node");
			throw new PipelineStageException("Graph contains no first utterance node");
		}
		return input;
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
