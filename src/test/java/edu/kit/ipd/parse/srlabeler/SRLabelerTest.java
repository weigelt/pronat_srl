package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class SRLabelerTest {

	ShallowNLP snlp;
	SRLabeler srLabeler;
	String input;
	PrePipelineData ppd;
	HashMap<String, String> hm;

	@Before
	public void setUp() {
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
		input = hm.get("1.2");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void twoThree() {
		ppd = new PrePipelineData();
		input = hm.get("2.3");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void threeTwo() {
		ppd = new PrePipelineData();
		input = hm.get("3.2");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void doubleWordTest() {
		ppd = new PrePipelineData();
		input = "put the green green cup into the dishwasher";
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void itsTest() {
		ppd = new PrePipelineData();
		input = "Armar put the green cup on the table it's next to the popcorn";
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void fourOne() {
		ppd = new PrePipelineData();
		input = hm.get("4.1");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void fiveThree() {
		ppd = new PrePipelineData();
		input = hm.get("5.3");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void ifFiveThree() {
		ppd = new PrePipelineData();
		input = hm.get("if.5.3");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
	}

	@Test
	public void ifFourTen() {
		ppd = new PrePipelineData();
		input = hm.get("if.4.10");
		ppd.setTranscription(input);
		executeSNLPandSRL(ppd);
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
