package edu.kit.ipd.parse.srlabeler.propbank;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PropBankVerbNetMapper {

	private Document doc = null;

	private HashMap<String, Predicate> predicates;

	public PropBankVerbNetMapper() {
		predicates = new HashMap<String, Predicate>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Path resourcePath = Paths.get(getClass().getResource("/vnpbMappings.xml").toURI());
			doc = builder.parse(new File(resourcePath.toUri()));
			NodeList predicateNodes = doc.getDocumentElement().getElementsByTagName("predicate");
			for (int i = 0; i < predicateNodes.getLength(); i++) {
				Node node = predicateNodes.item(i);
				if (node.getNodeName().equals("predicate")) {
					String lemma = node.getAttributes().getNamedItem("lemma").getTextContent();
					if (node instanceof Element) {
						Element ele = (Element) node;
						NodeList nl = ele.getElementsByTagName("argmap");
						predicates.put(lemma, new Predicate(lemma, nl));
					}
				}
			}
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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

	public Document getDocument() {
		return this.doc;
	}

	public Predicate getPredicate(String verb) {
		return predicates.get(verb);
	}

}
