package edu.kit.ipd.parse.srlabeler.propbank;

import java.util.HashMap;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArgumentMapping {

	private String propBankRoleset;

	private String verbNetClass;

	private HashMap<String, String> roles;

	public ArgumentMapping(String propBankRoleSet, String verbNetClass, NodeList roles) {
		this.setPropBankRoleset(propBankRoleSet);
		this.setVerbNetClass(verbNetClass);
		this.roles = new HashMap<String, String>();
		for (int i = 0; i < roles.getLength(); i++) {
			Node node = roles.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				this.roles.put(ele.getAttribute("pb-arg"), ele.getAttribute("vn-theta"));
			}
		}
	}

	public String getPropBankRoleset() {
		return propBankRoleset;
	}

	public void setPropBankRoleset(String propBankRoleset) {
		this.propBankRoleset = propBankRoleset;
	}

	public String getVerbNetClass() {
		return verbNetClass;
	}

	public void setVerbNetClass(String verbNetClass) {
		this.verbNetClass = verbNetClass;
	}

	public String getVerbNetRole(String argNumber) {
		return roles.get(argNumber);
	}

	public Set<String> getArgNumbers() {
		return roles.keySet();
	}

}
