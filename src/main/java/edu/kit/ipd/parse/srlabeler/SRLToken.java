package edu.kit.ipd.parse.srlabeler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Token consisting of a word and its associated semantic role labels
 * @author Tobias Hey
 *
 */
public class SRLToken {

	private String word;
	private int instructionNumber;
	private List<String> srls;
	
	/**
	 * Constructs a new {@link SRLToken}
	 * @param word the Word represented by this class
	 * @param instructionNumber the Instruction the word belongs to
	 * @param srls the semantic role labels associated with this word
	 */
	public SRLToken(String word,int instructionNumber, List<String> srls) {
		this.word = word;
		this.instructionNumber = instructionNumber;
		this.srls = srls;
	}

	/**
	 * Constructs a new {@link SRLToken}
	 * @param word the Word represented by this class
	 * @param instructionNumber the Instruction the word belongs to
	 */
	public SRLToken(String word, int instructionNumber) {
		this.word = word;
		this.instructionNumber = instructionNumber;
		this.srls = new ArrayList<String>();
	}

	/**
	 * Gets the represented Word
	 * @return the represented Word
	 */
	public String getWord() {
		return word;
	}

	/**
	 * Sets the Word to represent
	 * @param word the word to represent
	 */
	public void setWord(String word) {
		this.word = word;
	}
	
	public int getInstructionNumber() {
		return this.instructionNumber;
	}
	
	public void setInstructionNumber(int instructionNumber) {
		this.instructionNumber = instructionNumber;
	}
	

	/**
	 * Gets the semantic role labels associated with this word
	 * @return the semantic role labels associated with this word
	 */
	public List<String> getSrls() {
		return srls;
	}

	/**
	 * Sets the associated semantic role labels for this word
	 * @param srls the associated semantic role labels
	 */
	public void setSrls(List<String> srls) {
		this.srls = srls;
	}
	
	@Override
	public String toString() {
		
		return "(" + this.word + ", " + this.instructionNumber + ", " + srls.toString() + ") ";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SRLToken) {
			SRLToken other = (SRLToken) obj;
			return this.word.equals(other.getWord()) && this.instructionNumber == other.getInstructionNumber() && this.srls.equals(other.getSrls());
		}
		return false;
	}

	

}
