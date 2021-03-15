package edu.kit.ipd.pronat.srl.propbank;

/**
 * This class represents a {@link Roleset} with its confidence in the current
 * setting
 * 
 * @author Sebastian Weigelt
 * @author Tobias Hey
 *
 */
public class RolesetConfidence {

	private Roleset rs;

	private double confidence;

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
	public double getConfidence() {
		return confidence;
	}

	/**
	 * @return the numberOfCorrectArguments
	 */
	public int getNumberOfCorrectArguments() {
		return numberOfCorrectArguments;
	}

	/**
	 * @param roleset
	 * @param confidence
	 * @param numberOfCorrectArguments
	 */
	public RolesetConfidence(Roleset roleset, double confidence, int numberOfCorrectArguments) {
		rs = roleset;
		this.confidence = confidence;
		this.numberOfCorrectArguments = numberOfCorrectArguments;
	}

	/**
	 * Sets the Confidence
	 * 
	 * @param confidence
	 */
	public void setConfidence(double confidence) {
		this.confidence = confidence;

	}

}
