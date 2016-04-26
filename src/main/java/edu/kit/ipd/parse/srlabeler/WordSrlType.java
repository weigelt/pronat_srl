package edu.kit.ipd.parse.srlabeler;

import java.util.ArrayList;
import java.util.Arrays;
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

	public void append(WordSrlType type) {
		ArrayList<String> combinedWords = new ArrayList<String>(Arrays.asList(this.words));
		for (String s : type.getWords()) {
			combinedWords.add(s);
		}
		this.words = combinedWords.toArray(new String[combinedWords.size()]);

		this.srl.addAll(type.getSrl());

	}
}
