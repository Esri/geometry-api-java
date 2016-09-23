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
	private Iterator<?> m_keys_iter;
	private String m_current_key;

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

		return m_current_key;
	}

	Object getCurrentObject() {
		if (!m_bStarted) {
			throw new GeometryException("invalid call");
		}

		if (m_currentIndex == m_jsonObject.length()) {
			throw new GeometryException("invalid call");
		}

		return m_jsonObject.opt(m_current_key);
	}

	boolean next() {
		if (!m_bStarted) {
			m_currentIndex = 0;
			m_keys_iter = m_jsonObject.keys();
			m_bStarted = true;
			if (m_keys_iter.hasNext()) {
				m_current_key = (String)m_keys_iter.next();
			}

		} else if (m_currentIndex != m_jsonObject.length()) {
			if (m_keys_iter.hasNext()) {
				m_current_key = (String)m_keys_iter.next();
			}

			m_currentIndex++;
		}

		return m_currentIndex != m_jsonObject.length();
	}
}
