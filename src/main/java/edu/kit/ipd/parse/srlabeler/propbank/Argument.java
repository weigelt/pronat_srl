package edu.kit.ipd.parse.srlabeler.propbank;

/**
 * This class represents an Argument of a {@link Roleset}
 * 
 * @author Tobias Hey
 *
 */
public class Argument {

	private int number;

	private String descr;

	private String[] vnRoles;

	private String[] fnRoles;

	/**
	 * Constructs a new {@link Argument}
	 * 
	 * @param number
	 *            the number of the Argument
	 * @param descr
	 *            The Description of the Argument
	 * @param vnRoles
	 *            The Verb Net Roles
	 * @param fnRoles
	 *            The Frame Net Roles
	 */
	public Argument(int number, String descr, String[] vnRoles, String[] fnRoles) {
		this.number = number;
		this.descr = descr;
		this.vnRoles = vnRoles;
		this.fnRoles = fnRoles;
	}

	/**
	 * @return the number of the Argument
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @return the description of the Arguments role
	 */
	public String getDescr() {
		return descr;
	}

	/**
	 * @return the verbNetRoles
	 */
	public String[] getVnRoles() {
		return vnRoles;
	}

	/**
	 * @return the frameNetRoles
	 */
	public String[] getFnRoles() {
		return fnRoles;
	}

}
