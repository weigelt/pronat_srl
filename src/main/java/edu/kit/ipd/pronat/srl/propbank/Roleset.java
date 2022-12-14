/**
 * 
 */
package edu.kit.ipd.pronat.srl.propbank;

import java.util.HashMap;
import java.util.List;

/**
 * This class represents a PropBank Roleset
 * 
 * @author Sebastian Weigelt
 * @author Tobias Hey
 *
 */
public class Roleset {

	private final String id;

	private final String descr;

	private final String lemma;

	private final String[] vnFrames;

	private final String[] fnFrames;

	private final String[] eventTypes;

	private HashMap<String, Argument> roles;

	/**
	 * Constrcuts a new {@link Roleset}
	 * 
	 * @param id
	 * @param descr
	 * @param lemma
	 * @param vnFrames
	 * @param fnFrames
	 * @param eventTypes
	 * @param argDescr
	 * @param argVNRoles
	 * @param argFNRoles
	 */
	public Roleset(String id, String descr, String lemma, String[] vnFrames, String[] fnFrames, String[] eventTypes, List<String> argDescr,
			List<String[]> argVNRoles, List<String[]> argFNRoles) {
		this.id = id;
		this.descr = descr;
		this.lemma = lemma;
		this.vnFrames = vnFrames;
		this.fnFrames = fnFrames;
		this.eventTypes = eventTypes;
		roles = new HashMap<String, Argument>(argDescr.size());
		for (int i = 0; i < argDescr.size(); i++) {
			if (argDescr.get(i) != null) {
				Argument arg = new Argument(i, argDescr.get(i), argVNRoles.get(i), argFNRoles.get(i));
				roles.put(String.valueOf(i), arg);
			}
		}
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the descr
	 */
	public String getDescr() {
		return descr;
	}

	/**
	 * @return the lemma
	 */
	public String getLemma() {
		return lemma;
	}

	/**
	 * @return the vnFrames
	 */
	public String[] getVnFrames() {
		return vnFrames;
	}

	/**
	 * @return the fnFrames
	 */
	public String[] getFnFrames() {
		return fnFrames;
	}

	/**
	 * @return the eventTypes
	 */
	public String[] getEventTypes() {
		return eventTypes;
	}

	/**
	 * @return the roles
	 */
	public HashMap<String, Argument> getRoles() {
		return roles;
	}

}
