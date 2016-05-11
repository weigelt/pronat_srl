package edu.kit.ipd.parse.srlabeler.propbank;

public class VerbNetRoleConfidence {

	private String role;

	private String pbRoleset;

	private String vnClass;

	private float confidence;

	public VerbNetRoleConfidence(String role, Float confidence, String pbRoleset, String vnClass) {
		setRole(role);
		setConfidence(confidence);
		setPbRoleset(pbRoleset);
		setVnClass(vnClass);
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return the confidence
	 */
	public float getConfidence() {
		return confidence;
	}

	/**
	 * @param confidence
	 *            the confidence to set
	 */
	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}

	/**
	 * @return the pbRoleset
	 */
	public String getPbRoleset() {
		return pbRoleset;
	}

	/**
	 * @param pbRoleset
	 *            the pbRoleset to set
	 */
	public void setPbRoleset(String pbRoleset) {
		this.pbRoleset = pbRoleset;
	}

	/**
	 * @return the vnClass
	 */
	public String getVnClass() {
		return vnClass;
	}

	/**
	 * @param vnClass
	 *            the vnClass to set
	 */
	public void setVnClass(String vnClass) {
		this.vnClass = vnClass;
	}
}
