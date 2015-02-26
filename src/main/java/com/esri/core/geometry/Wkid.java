/*
 Copyright 1995-2015 Esri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */

package com.esri.core.geometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public final class Wkid {
	static ArrayList<Double> readTolerances(String resourceName) {
		try {
			ArrayList<Double> tolerances = new ArrayList<Double>();
			InputStream input = Wkid.class.getResourceAsStream(resourceName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input));
			while (true) {
				String s = reader.readLine();
				if (s == null)
					break;
				int sep = s.indexOf('\t', 0);
				String id_s = s.substring(0, sep);
				int tol_index = Integer.parseInt(id_s);
				if (tol_index != tolerances.size())
					throw new IllegalArgumentException("Wkid.readTolerances");
				String tol_val = s.substring(sep + 1, s.length());
				tolerances.add(Double.parseDouble(tol_val));
			}

			return tolerances;
		} catch (IOException ex) {

		}
		return null;
	}

	static HashMap<Integer, Integer> readToleranceMap(String resourceName) {
		try {
			HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>(
					600);
			InputStream input = Wkid.class.getResourceAsStream(resourceName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input));
			while (true) {
				String s = reader.readLine();
				if (s == null)
					break;
				int sep = s.indexOf('\t', 0);
				String id_s = s.substring(0, sep);
				int wkid = Integer.parseInt(id_s);
				String id_t = s.substring(sep + 1, s.length());
				int tol = Integer.parseInt(id_t);
				hashMap.put(wkid, tol);
			}
			return hashMap;
		} catch (IOException ex) {
		}
		return null;

	}

	static ArrayList<Double> m_gcs_tolerances = readTolerances("gcs_tolerances.txt");
	static ArrayList<Double> m_pcs_tolerances = readTolerances("pcs_tolerances.txt");
	static HashMap<Integer, Integer> m_gcsToTol = readToleranceMap("gcs_id_to_tolerance.txt");
	static HashMap<Integer, Integer> m_pcsToTol = readToleranceMap("pcs_id_to_tolerance.txt");
	static HashMap<Integer, Integer> m_wkid_to_new;
	static HashMap<Integer, Integer> m_wkid_to_old;
	static {
		try {
			m_wkid_to_new = new HashMap<Integer, Integer>(100);
			m_wkid_to_old = new HashMap<Integer, Integer>(100);
			{
				InputStream input = Wkid.class
						.getResourceAsStream("new_to_old_wkid.txt");
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(input));
				while (true) {
					String s = reader.readLine();
					if (s == null)
						break;
					s = s.trim();
					if (s.length() == 0)
						continue;
					int sep = s.indexOf('\t', 0);
					String id_s = s.substring(0, sep);
					int wkid_new = Integer.parseInt(id_s);
					String id_t = s.substring(sep + 1, s.length());
					int wkid_old = Integer.parseInt(id_t);
					m_wkid_to_new.put(wkid_old, wkid_new);
					m_wkid_to_old.put(wkid_new, wkid_old);
				}
			}
			{
				InputStream input = Wkid.class
						.getResourceAsStream("intermediate_to_old_wkid.txt");
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(input));
				while (true) {
					String s = reader.readLine();
					if (s == null)
						break;
					s = s.trim();
					if (s.length() == 0)
						continue;
					int sep = s.indexOf('\t', 0);
					String id_s = s.substring(0, sep);
					int wkid = Integer.parseInt(id_s);
					String id_t = s.substring(sep + 1, s.length());
					int wkid_old = Integer.parseInt(id_t);
					m_wkid_to_old.put(wkid, wkid_old);
					m_wkid_to_new.put(wkid, m_wkid_to_new.get(wkid_old));
				}
			}

		} catch (IOException ex) {

		}
	}

	public static double find_tolerance_from_wkid(int wkid) {
		double tol = find_tolerance_from_wkid_helper(wkid);
		if (tol == 1e38) {
			int old = wkid_to_old(wkid);
			if (old != wkid)
				tol = find_tolerance_from_wkid_helper(old);
			if (tol == 1e38)
				return 1e-10;
		}

		return tol;
	}

	private static double find_tolerance_from_wkid_helper(int wkid) {
		if (m_gcsToTol.containsKey(wkid)) {
			return m_gcs_tolerances.get(m_gcsToTol.get(wkid));
		}

		if (m_pcsToTol.containsKey(wkid)) {
			return m_pcs_tolerances.get(m_pcsToTol.get(wkid));
		}

		return 1e38;
	}

	public static int wkid_to_new(int wkid) {
		if (m_wkid_to_new.containsKey(wkid)) {
			return m_wkid_to_new.get(wkid);
		}
		return wkid;
	}

	public static int wkid_to_old(int wkid) {
		if (m_wkid_to_old.containsKey(wkid)) {
			return m_wkid_to_old.get(wkid);
		}
		return wkid;
	}
}
