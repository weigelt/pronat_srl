package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.data.AbstractPipelineData;
import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PipelineDataCastException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.data.token.SRLToken;
import edu.kit.ipd.parse.luna.data.token.Token;
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
			List<List<Token>> taggedHypos = prePipeData.getTaggedHypotheses();
			List<List<WordSRLPair>> result = parseBatch(taggedHypos);
			List<List<Token>> tokensWithSrls = associateResultWithTokenBatch(taggedHypos, result);
			prePipeData.setTaggedHypotheses(tokensWithSrls);
			

		} catch (MissingDataException e) {
			logger.error("No tagged hypotheses to process, aborting ...", e);
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

		//		try {
		//			Token[] tokens = prePipeData.getTokens();
		//			WordSrlType result = parse(Arrays.asList(tokens));
		//			System.out.println(result.toString());
		//			//TODO: put result into PrePipelineData
		//
		//		} catch (MissingDataException e) {
		//			logger.error("No tokens to process, aborting ...", e);
		//			throw new PipelineStageException(e);
		//		} catch (IOException e) {
		//			logger.error("An IOException occured during run of SENNA", e);
		//			throw new PipelineStageException(e);
		//		} catch (URISyntaxException e) {
		//			logger.error("An URISyntaxException occured during initialization of SENNA", e);
		//			throw new PipelineStageException(e);
		//		} catch (InterruptedException e) {
		//			logger.error("The SENNA process interrupted unexpectedly", e);
		//			throw new PipelineStageException(e);
		//		}

	}

	private List<List<Token>> associateResultWithTokenBatch(List<List<Token>> taggedHypos, List<List<WordSRLPair>> wordSRLPairsList) throws IllegalArgumentException{
		List<List<Token>> result = new ArrayList<List<Token>>();
		if (taggedHypos.size() == wordSRLPairsList.size()) {
			for (int i = 0; i < taggedHypos.size(); i++) {
				result.add(associateResultWithToken(taggedHypos.get(i), wordSRLPairsList.get(i)));
			}
		} else {
			throw new IllegalArgumentException("There are more or less Hypotheses then results");
		}
		return result;

	}
	
	private List<Token> associateResultWithToken(List<Token> tokens, List<WordSRLPair> wsps) throws IllegalArgumentException{
		List<Token> srlTokenList = new ArrayList<Token>();
		if (tokens.size() == wsps.size()) {
			for (int j = 0; j < tokens.size(); j++) {
				if (tokens.get(j).getWord().equals(wsps.get(j).getWord())) {
					srlTokenList.add(new SRLToken(tokens.get(j), wsps.get(j).getSrls()));
				}
			}
		} else {
			throw new IllegalArgumentException("There is a different number of tokens then result objects");
		}
		return srlTokenList;
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
	public List<WordSRLPair> parse(List<Token> tokens) throws IOException, URISyntaxException, InterruptedException {
		List<WordSRLPair> result = new ArrayList<WordSRLPair>();
		Senna senna = new Senna();
		if (parsePerInstruction) {
			logger.info("parsing SRL for each instruction independently");
			List<String> inputList = generateInstructionInput(tokens);

			for (String input : inputList) {
				File inputTmpFile = writeToTempFile(input);
				result.addAll(senna.parse(inputTmpFile));
			}
		} else {
			String input = "";
			for (Token t : tokens) {
				input += t.getWord() + " ";
			}
			File inputTmpFile = writeToTempFile(input);
			logger.info("parsing SRL without instructions");
			result = senna.parse(inputTmpFile);
		}
		return result;
	}

	private List<String> generateInstructionInput(List<Token> tokens) {
		List<String> inputList = new ArrayList<String>();
		int instructionNumber = 0;
		String instruction = "";
		for (Token t : tokens) {

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

	private List<List<WordSRLPair>> parseBatch(List<List<Token>> hypotheses) throws IOException, URISyntaxException, InterruptedException {
		List<List<WordSRLPair>> result = new ArrayList<List<WordSRLPair>>();
		for (List<Token> lt : hypotheses) {
			result.add(parse(lt));
		}
		return result;
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
