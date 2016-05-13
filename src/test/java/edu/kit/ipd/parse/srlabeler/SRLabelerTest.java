package edu.kit.ipd.parse.srlabeler;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

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

	private static final String ROLE_VALUE_NAME = "role";

	private static final String VN_ROLE_NAME = "vnRole";

	private static final String VN_ROLE_CONFIDENCE_NAME = "vnRoleConfidence";

	private static final String IOBES = "IOBES";

	private static final String PROPBANK_ROLE_DESCRIPTION = "pbRoleDescr";

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
			printSRLGraph(ppd.getGraph());
		} catch (PipelineStageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MissingDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void fourOne() {
		ppd = new PrePipelineData();
		input = "Hey Armar can you go to the table and grab me the popcorn bag please and then yeah can you bring it back to me";
		ppd.setTranscription(input);

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
							: arc.getAttributeValue(IOBES) + "-" + arc.getAttributeValue(ROLE_VALUE_NAME) + ", [PB: "
									+ arc.getAttributeValue(PROPBANK_ROLE_DESCRIPTION) + "; VN:" + arc.getAttributeValue(VN_ROLE_NAME)
									+ "; Conf=" + arc.getAttributeValue(VN_ROLE_CONFIDENCE_NAME) + "]")));
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
