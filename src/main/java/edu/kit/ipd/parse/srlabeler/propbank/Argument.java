package edu.kit.ipd.parse.srlabeler.propbank;

public class Argument {

	private int number;

	private String descr;

	private String[] vnRoles;

	private String[] fnRoles;

	/**
	 * @param number
	 * @param descr
	 * @param vnRoles
	 * @param fnRoles
	 */
	public Argument(int number, String descr, String[] vnRoles, String[] fnRoles) {
		this.number = number;
		this.descr = descr;
		this.vnRoles = vnRoles;
		this.fnRoles = fnRoles;
	}

	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @return the descr
	 */
	public String getDescr() {
		return descr;
	}

	/**
	 * @return the vnRoles
	 */
	public String[] getVnRoles() {
		return vnRoles;
	}

	/**
	 * @return the fnRoles
	 */
	public String[] getFnRoles() {
		return fnRoles;
	}

}
