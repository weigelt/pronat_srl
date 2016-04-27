package edu.kit.ipd.parse.srlabeler;

import java.util.List;

/**
 * This class represents a tuple consisting of a word and its associated semantic role labels
 * @author Tobias Hey
 *
 */
public class WordSRLPair {

	private String word;
	private List<String> srls;
	
	/**
	 * Constructs a new {@link WordSRLPair}
	 * @param word the Word represented by this class
	 * @param srls the semantic role labels associated with this word
	 */
	public WordSRLPair(String word, List<String> srls) {
		this.word = word;
		this.srls = srls;
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
		
		return "(" + this.word + ", " + srls.toString() + ") ";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WordSRLPair) {
			WordSRLPair other = (WordSRLPair) obj;
			return this.word.equals(other.getWord()) && this.srls.equals(other.getSrls());
		}
		return false;
	}

}
