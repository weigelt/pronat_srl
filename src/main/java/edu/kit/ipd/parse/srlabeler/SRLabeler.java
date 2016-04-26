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
import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.pipeline.IPipelineStage;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

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
			parseBatch(taggedHypos);
			//TODO: put result into PrePipelineData

		} catch (MissingDataException e) {
			logger.error("No tagged hypotheses to process, aborting ...", e);
			throw new PipelineStageException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public WordSrlType parse(List<Token> tokens) throws IOException, URISyntaxException, InterruptedException {
		WordSrlType result = null;
		Senna senna = new Senna();
		if (parsePerInstruction) {
			logger.info("parsing SRL for each instruction independently");
			List<String> inputList = generateInstructionInput(tokens);
			
			if (!inputList.isEmpty()) {
				File inputTmpFile = writeToTempFile(inputList.get(0));
				result= senna.parse(inputTmpFile);
			}
			for (int i = 1; i < inputList.size(); i++) {
				File inputTmpFile = writeToTempFile(inputList.get(i));
				result.append(senna.parse(inputTmpFile));
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
	
	private List<WordSrlType> parseBatch(List<List<Token>> hypotheses) throws IOException, URISyntaxException, InterruptedException {
		List<WordSrlType> result = new ArrayList<WordSrlType>();
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
