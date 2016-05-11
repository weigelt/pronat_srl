package edu.kit.ipd.parse.srlabeler.propbank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Predicate {

	private String lemma;

	private ArrayList<ArgumentMapping> argMaps;

	public Predicate(String lemma, NodeList argMapNodes) {
		this.setLemma(lemma);
		argMaps = new ArrayList<ArgumentMapping>();
		for (int i = 0; i < argMapNodes.getLength(); i++) {
			Node node = argMapNodes.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				String pbRoleset = ele.getAttribute("pb-roleset");
				String vnClass = ele.getAttribute("vn-class");
				NodeList ns = ele.getElementsByTagName("role");
				argMaps.add(new ArgumentMapping(pbRoleset, vnClass, ns));
			}
		}

	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public VerbNetRoleConfidence getVNRole(String argNumber) {
		int numberOfPossibleCandidates = 0;
		String role = "-";
		String pbRoleset = "-";
		String vnClass = "-";
		for (ArgumentMapping mapping : argMaps) {
			if (mapping.getArgNumbers().contains(argNumber)) {
				numberOfPossibleCandidates++;
				if (role == "-") {
					role = mapping.getVerbNetRole(argNumber);
					pbRoleset = mapping.getPropBankRoleset();
					vnClass = mapping.getVerbNetClass();
				}
			}
		}
		return new VerbNetRoleConfidence(role,
				(float) (numberOfPossibleCandidates != 0 ? (float) (1.0f / numberOfPossibleCandidates) : 1.0f), pbRoleset, vnClass);

	}

	public ArrayList<VerbNetRoleConfidence> getPossibleVNRoles(String argNumber, Set<String> totalArgumentNumbers) {
		ArrayList<VerbNetRoleConfidence> result = new ArrayList<VerbNetRoleConfidence>();
		for (ArgumentMapping mapping : argMaps) {
			boolean candidate = true;
			int correctArguments = 0;
			for (String argument : totalArgumentNumbers) {
				if (!mapping.getArgNumbers().contains(argument)) {
					candidate = false;
					break;
				} else {
					correctArguments++;
				}
			}
			if (candidate) {
				result.add(new VerbNetRoleConfidence(mapping.getVerbNetRole(argNumber),
						((float) correctArguments) / mapping.getArgNumbers().size(), mapping.getPropBankRoleset(),
						mapping.getVerbNetClass()));
			}
		}
		float[] confidences = new float[result.size()];
		for (int i = 0; i < result.size(); i++) {
			VerbNetRoleConfidence vb = result.get(i);
			float confidence = vb.getConfidence();
			int count = 0;
			for (VerbNetRoleConfidence vbn : result) {
				if (vbn.getConfidence() == confidence) {
					count++;
				}
			}
			confidences[i] = (count != 0 ? confidence / count : confidence);
		}
		for (int i = 0; i < result.size(); i++) {
			VerbNetRoleConfidence vb = result.get(i);
			vb.setConfidence(confidences[i]);
		}
		Collections.sort(result, new Comparator<VerbNetRoleConfidence>() {

			@Override
			public int compare(VerbNetRoleConfidence o1, VerbNetRoleConfidence o2) {
				if (o1.getConfidence() < o2.getConfidence()) {
					return 1;
				} else if (o1.getConfidence() > o2.getConfidence()) {
					return -1;
				}
				return 0;
			}
		});
		return result;
	}

}
