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

import org.json.JSONObject;

import java.util.Iterator;

final class JSONObjectEnumerator {

	private JSONObject m_jsonObject;
	private boolean m_bStarted;
	private int m_currentIndex;
	private String[] m_keys;

	JSONObjectEnumerator(JSONObject jsonObject) {
		m_bStarted = false;
		m_currentIndex = -1;
		m_jsonObject = jsonObject;
	}

	String getCurrentKey() {
		if (!m_bStarted) {
			throw new GeometryException("invalid call");
		}

		if (m_currentIndex == m_jsonObject.length()) {
			throw new GeometryException("invalid call");
		}

		return m_keys[m_currentIndex];
	}

	Object getCurrentObject() {
		if (!m_bStarted) {
			throw new GeometryException("invalid call");
		}

		if (m_currentIndex == m_jsonObject.length()) {
			throw new GeometryException("invalid call");
		}

		return m_jsonObject.opt(m_keys[m_currentIndex]);
	}

	boolean next() {
		if (!m_bStarted) {
			m_currentIndex = 0;
			m_keys = getNames(m_jsonObject);
			m_bStarted = true;
		} else if (m_currentIndex != m_jsonObject.length()) {
			m_currentIndex++;
		}

		return m_currentIndex != m_jsonObject.length();
	}

	// copied from https://github.com/Esri/JSON-java/blob/master/JSONObject.java
	private static String[] getNames(JSONObject jo) {
		int length = jo.length();
		if (length == 0) {
			return null;
		}
		//noinspection unchecked - sic
		Iterator<String> iterator = jo.keys();
		String[] names = new String[length];
		int i = 0;
		while (iterator.hasNext()) {
			names[i] = iterator.next();
			i += 1;
		}
		return names;
	}
}
