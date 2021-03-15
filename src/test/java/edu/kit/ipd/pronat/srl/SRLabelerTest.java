package edu.kit.ipd.pronat.srl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.token.SRLToken;
import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.tools.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.luna.tools.StringToHypothesis;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

//TODO: Create useful tests!
public class SRLabelerTest {

	ShallowNLP snlp;
	SRLabeler srLabeler;
	String input;
	PrePipelineData ppd;
	HashMap<String, String> hm;
	private static Properties props;

	@Before
	public void setUp() {
		props = ConfigManager.getConfiguration(SRLabeler.class);
		srLabeler = new SRLabeler();
		srLabeler.init();
		snlp = new ShallowNLP();
		snlp.init();
		hm = new HashMap<>();
		try {
			File file = new File(SRLabelerTest.class.getResource("/korpus.xml").toURI());
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			NodeList nl = doc.getElementsByTagName("text");
			for (int i = 0; i < nl.getLength(); i++) {
				Element node = (Element) nl.item(i);
				String name = node.getAttribute("name");
				String text = node.getTextContent();
				hm.put(name, text);
			}
		} catch (URISyntaxException e) {
			// TODO Auto
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto
			e.printStackTrace();
		}

	}

	@Test
	public void oneTwo() {
		ppd = new PrePipelineData();
		String input = hm.get("1.2");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void twoThree() {
		ppd = new PrePipelineData();
		String input = hm.get("2.3");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void threeTwo() {
		ppd = new PrePipelineData();
		String input = hm.get("3.2");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void doubleWordTest() {
		ppd = new PrePipelineData();
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis("put the green green cup into the dishwasher"));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void itsTest() {
		ppd = new PrePipelineData();
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis("Armar put the green cup on the table it's next to the popcorn"));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void fourOne() {
		ppd = new PrePipelineData();
		String input = hm.get("4.1");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void fiveThree() {
		ppd = new PrePipelineData();
		String input = hm.get("5.3");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void ifFiveThree() {
		ppd = new PrePipelineData();
		String input = hm.get("if.5.3");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void ifFourTen() {
		ppd = new PrePipelineData();
		String input = hm.get("if.4.10");
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input));
		executeSNLPandSRL(ppd);
	}

	@Test
	public void testPosVerb() {
		props.setProperty("USE_POS_TAGGER_VERBS", "true");
		props.setProperty("PARSE_PER_INSTRUCTION", "false");
		srLabeler.init();
		ppd = new PrePipelineData();
		String input = "hey armar please open the dishwasher";
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input, false));
		executeSNLPandSRL(ppd);
		try {
			List<Token> tokenList = ppd.getTaggedHypotheses().get(0);
			ppd.getTaggedHypotheses().get(0).stream().filter(e -> e instanceof SRLToken).map(SRLToken.class::cast)
					.forEach(e -> System.out.println(e.getPbRolesetDescr()));
		} catch (MissingDataException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPosVerbMultipleInstructions() {
		props.setProperty("USE_POS_TAGGER_VERBS", "true");
		props.setProperty("PARSE_PER_INSTRUCTION", "true");
		srLabeler.init();
		ppd = new PrePipelineData();
		String input = "hey armar please open the dishwasher then close it";
		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input, false));
		executeSNLPandSRL(ppd);
		try {
			List<Token> tokenList = ppd.getTaggedHypotheses().get(0);
			ppd.getTaggedHypotheses().get(0).stream().filter(e -> e instanceof SRLToken).map(SRLToken.class::cast)
					.forEach(e -> System.out.println(e.getPbRolesetDescr()));
		} catch (MissingDataException e) {
			e.printStackTrace();
		}
	}

	private void executeSNLPandSRL(PrePipelineData ppd) {
		try {
			snlp.exec(ppd);
		} catch (PipelineStageException e) {
			// TODO Auto
			e.printStackTrace();
		}
		try {
			srLabeler.exec(ppd);
		} catch (PipelineStageException e) {
			// TODO Auto
			e.printStackTrace();
		}
	}

}
