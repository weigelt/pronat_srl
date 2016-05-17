package edu.kit.ipd.parse.srlabeler.propbank;

/**
 * This class represents a {@link Roleset} with its confidence in the current
 * setting
 * 
 * @author Tobias Hey
 *
 */
public class RolesetConfidence {

	private Roleset rs;

	private float confidence;

	private int numberOfCorrectArguments;

	/**
	 * @return the rs
	 */
	public Roleset getRoleset() {
		return rs;
	}

	/**
	 * @return the confidence
	 */
	public float getConfidence() {
		return confidence;
	}

	/**
	 * @return the numberOfCorrectArguments
	 */
	public int getNumberOfCorrectArguments() {
		return numberOfCorrectArguments;
	}

	/**
	 * @param rs
	 * @param confidence
	 * @param numberOfCorrectArguments
	 */
	public RolesetConfidence(Roleset roleset, float confidence, int numberOfCorrectArguments) {
		this.rs = roleset;
		this.confidence = confidence;
		this.numberOfCorrectArguments = numberOfCorrectArguments;
	}

	/**
	 * Sets the Confidence
	 * 
	 * @param confidence
	 */
	public void setConfidence(float confidence) {
		this.confidence = confidence;

	}

}
