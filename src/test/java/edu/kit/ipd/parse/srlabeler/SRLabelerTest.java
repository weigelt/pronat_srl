package edu.kit.ipd.parse.srlabeler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			srLabeler.exec(ppd);
			printSRLGraph(ppd.getGraph());
		} catch (PipelineStageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MissingDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void printSRLGraph(IGraph graph) {
		String prettyPrint = "Graph: " + " {\n";

		Iterator<? extends IArc> e = graph.getArcs().iterator();
		while (e.hasNext()) {
			IArc arc = e.next();
			INode src = arc.getSourceNode();
			INode trg = arc.getTargetNode();
			prettyPrint = prettyPrint.concat(src.getAllAttributeValues().size() == 0 ? src.getType().getName()
					: (String) src.getAttributeValue(src.getAttributeNames().get(0)));
			prettyPrint = prettyPrint.concat(" ---" + (arc.getAllAttributeValues().size() == 0 ? arc.getType()
					: (arc.getAllAttributeValues().size() == 1 ? arc.getAttributeValue(arc.getAttributeNames().get(0))
							: arc.getAttributeValue(SRLabeler.IOBES) + "-" + arc.getAttributeValue(SRLabeler.ROLE_VALUE_NAME) + ", [PB: "
									+ arc.getAttributeValue(SRLabeler.PROPBANK_ROLE_DESCRIPTION) + "; VN:"
									+ arc.getAttributeValue(SRLabeler.VN_ROLE_NAME) + "; Conf="
									+ arc.getAttributeValue(SRLabeler.ROLE_CONFIDENCE_NAME) + "]")));
			prettyPrint = prettyPrint.concat(" --->" + (trg.getAllAttributeValues().size() == 0 ? trg.getType().getName()
					: (String) trg.getAttributeValue(trg.getAttributeNames().get(0))) + "\n");
		}

		Iterator<INode> u = graph.getNodes().iterator();
		while (u.hasNext()) {
			INode node = u.next();
			if (node.getIncomingArcs().isEmpty() && node.getOutgoingArcs().isEmpty())
				prettyPrint = prettyPrint.concat(node.getAllAttributeValues().size() == 0 ? node.getType().getName()
						: node.getAttributeValue(node.getAttributeNames().get(0)) + "\n");
		}
		prettyPrint = prettyPrint.concat("}\n");
		System.out.println(prettyPrint);
	}

}
