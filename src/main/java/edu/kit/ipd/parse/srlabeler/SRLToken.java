package edu.kit.ipd.parse.srlabeler;

/**
 * This class represents a Token consisting of a word and its associated
 * semantic role labels
 * 
 * @author Tobias Hey
 *
 */
public class SRLToken {

	private String word;
	private int instructionNumber;

	/**
	 * Constructs a new {@link SRLToken}
	 * 
	 * @param word
	 *            the Word represented by this class
	 * @param instructionNumber
	 *            the Instruction the word belongs to
	 */
	public SRLToken(String word, int instructionNumber) {
		this.word = word;
		this.instructionNumber = instructionNumber;
	}

	/**
	 * Gets the represented Word
	 * 
	 * @return the represented Word
	 */
	public String getWord() {
		return word;
	}

	/**
	 * Sets the Word to represent
	 * 
	 * @param word
	 *            the word to represent
	 */
	public void setWord(String word) {
		this.word = word;
	}

	/**
	 * Gets the Instruction Number of this Token
	 * 
	 * @return the Instruction Number of this Token
	 */
	public int getInstructionNumber() {
		return this.instructionNumber;
	}

	/**
	 * Sets the Instruction Number of this Token
	 * 
	 * @param instructionNumber
	 *            to set
	 */
	public void setInstructionNumber(int instructionNumber) {
		this.instructionNumber = instructionNumber;
	}

	@Override
	public String toString() {

		return "(" + this.word + ", " + this.instructionNumber + ") ";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SRLToken) {
			SRLToken other = (SRLToken) obj;
			return this.word.equals(other.getWord()) && this.instructionNumber == other.getInstructionNumber();
		}
		return false;
	}

}
