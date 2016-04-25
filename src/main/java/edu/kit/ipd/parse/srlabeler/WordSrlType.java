package edu.kit.ipd.parse.srlabeler;

import java.util.List;

public class WordSrlType {

	private String[] words;
	private List<List<String>> srl;

	public WordSrlType(String[] words, List<List<String>> srl) {
		this.words = words;
		this.srl = srl;
	}

	public String[] getWords() {
		return words;
	}
	
	public List<List<String>> getSrl() {
		return this.srl;
	}
}
