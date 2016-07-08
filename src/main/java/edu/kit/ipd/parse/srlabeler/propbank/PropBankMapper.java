package edu.kit.ipd.parse.srlabeler.propbank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class represents the Mapping to the PropBank rolesets.
 * 
 * @author Tobias Hey
 *
 */
public final class PropBankMapper {

	private static final HashMap<String, List<Roleset>> ROLESETS;

	static {
		BufferedReader reader;
		ROLESETS = new HashMap<String, List<Roleset>>();
		try {
			reader = new BufferedReader(new InputStreamReader(PropBankMapper.class.getResourceAsStream("/PropBank.tsv"), "UTF-8"));

			String line;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t", -1);
				if (tokens.length > 5) {
					String id = tokens[0];
					String lemma = tokens[1];
					String name = tokens[2];

					String[] vnFrames = tokens[3].split("\\|");
					String[] fnFrames = tokens[4].split("\\|");
					String[] eventTypes = tokens[5].split("\\|");

					List<String> argDescr = new ArrayList<String>();
					List<String[]> argVNRoles = new ArrayList<String[]>();
					List<String[]> argFNRoles = new ArrayList<String[]>();
					for (int i = 0; i < 6; ++i) {
						argDescr.add(null);
						argVNRoles.add(null);
						argFNRoles.add(null);
					}
					for (int i = 6; i + 3 < tokens.length; i += 4) {
						int num = Integer.parseInt(tokens[i]);
						argDescr.set(num, tokens[i + 1]);
						argVNRoles.set(num, tokens[i + 2].split("\\|"));
						argFNRoles.set(num, tokens[i + 3].split("\\|"));
					}
					Roleset rs = new Roleset(id, name, lemma, vnFrames, fnFrames, eventTypes, argDescr, argVNRoles, argFNRoles);
					if (ROLESETS.containsKey(rs.getLemma())) {
						ROLESETS.get(rs.getLemma()).add(rs);
					} else {
						List<Roleset> rsList = new ArrayList<Roleset>();
						rsList.add(rs);
						ROLESETS.put(rs.getLemma(), rsList);
					}
				}
			}

			reader.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto
			e.printStackTrace();
		}

	}

	/**
	 * Returns a list of possible Rolesets for the specified lemma and argument
	 * numbers
	 * 
	 * @param lemma
	 *            The lemma of the verb to look after
	 * @param totalArgNumbers
	 *            The total Argument numbers in this phrase
	 * @return a list of possible {@link Roleset}
	 */
	public ArrayList<RolesetConfidence> getPossibleRolesets(String lemma, Set<String> totalArgNumbers) {
		ArrayList<RolesetConfidence> result = new ArrayList<RolesetConfidence>();
		List<Roleset> rolesets = ROLESETS.get(lemma);
		if (rolesets == null) {
			return result;
		}
		for (Roleset rs : rolesets) {

			HashMap<String, Argument> roles = rs.getRoles();
			boolean candidate = true;
			int correctArguments = 0;
			for (String arg : totalArgNumbers) {
				if (!roles.containsKey(arg)) {
					candidate = false;
					break;
				}
				correctArguments++;
			}

			if (candidate) {
				result.add(new RolesetConfidence(rs, ((double) correctArguments) / (double) totalArgNumbers.size(), correctArguments));
			}

		}
		double[] confidences = new double[result.size()];
		for (int i = 0; i < result.size(); i++) {
			RolesetConfidence rc = result.get(i);
			double confidence = rc.getConfidence();
			int count = 0;
			for (RolesetConfidence rcn : result) {
				if (rcn.getConfidence() == confidence) {
					count++;
				}
			}
			confidences[i] = (count != 0 ? confidence / count : confidence);
		}
		for (int i = 0; i < result.size(); i++) {
			RolesetConfidence rc = result.get(i);
			rc.setConfidence(confidences[i]);
		}
		Collections.sort(result, new Comparator<RolesetConfidence>() {

			@Override
			public int compare(RolesetConfidence o1, RolesetConfidence o2) {
				if (o1.getConfidence() < o2.getConfidence()) {
					return 1;
				} else if (o1.getConfidence() > o2.getConfidence()) {
					return -1;
				}
				return 0;
			}
		});
		return result;
	}

}
