/**
 * 
 */
package edu.kit.ipd.parse.luna.data.token;

import java.util.List;

/**
 * This class represents a {@link Token} with associated semantic role labels.
 * 
 * @author Tobias Hey
 *
 */
public class SRLToken extends Token {

	private List<String> srls;
	
	/**
	 * Constructs a new {@link SRLToken} object with the previous Version of the {@link Token}
	 * and the semantic role labels to associate with this {@link Token} 
	 * 
	 * @param prev The previous version of this Token
	 * @param srls The semantic role labels to set
	 */
	public SRLToken(Token prev, List<String> srls) {
		super(prev.getWord(), prev.getPos(), prev.getChunkIOB(), prev.getChunk(), prev.getPosition(), prev.getInstructionNumber());
		this.srls = srls;
		// TODO Auto-generated constructor stub
	}

	/**
	 * Gets the semantic role labels associated with this Token
	 * @return the semantic role labels associated with this Token
	 */
	public List<String> getSrls() {
		return srls;
	}

	/**
	 * Sets the semantic role labels associated with this Token
	 * @param srls The semantic role labels to set
	 */
	public void setSrls(List<String> srls) {
		this.srls = srls;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SRLToken) {
			SRLToken other = (SRLToken) obj;
			return this.getPosition() == other.getPosition() && this.getChunk().getName().equals(other.getChunk().getName())
					&& this.getChunk().getPredecessor() == other.getChunk().getPredecessor()
					&& this.getChunk().getSuccessor() == other.getChunk().getSuccessor() && this.getChunkIOB().equals(other.getChunkIOB())
					&& this.getInstructionNumber() == other.getInstructionNumber() && this.getPos().equals(other.getPos())
					&& this.getWord().equals(other.getWord())
					&& this.getSrls().equals(other.getSrls());
		}
		return false;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString() + ":(" + this.srls.toString() + ")";
	}

}
