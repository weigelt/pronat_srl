package edu.kit.ipd.parse.srlabeler;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class SRLabelerTest {

	ShallowNLP snlp;
	SRLabeler srLabeler;
	String input;
	PrePipelineData ppd;

	@Before
	public void setUp() {
		srLabeler = new SRLabeler();
		srLabeler.init();
		snlp = new ShallowNLP();
		snlp.init();
	}

	@Test
	public void singleInputTest() {
		ppd = new PrePipelineData();
		//input = "okay Armar go to the table";
		input = "okay Armar go to the table take the green cup go to the dishwasher open it put the green cup into the dishwasher";
		ppd.setTranscription(input);
		

		try {
			snlp.exec(ppd);
		} catch (PipelineStageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			srLabeler.exec(ppd);
		} catch (PipelineStageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
