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

class SimpleJsonCursor extends JsonCursor {

	String m_jsonString;
	String[] m_jsonStringArray;

	int m_index;
	int m_count;

	public SimpleJsonCursor(String jsonString) {
		m_jsonString = jsonString;
		m_index = -1;
		m_count = 1;
	}

	public SimpleJsonCursor(String[] jsonStringArray) {
		m_jsonStringArray = jsonStringArray;
		m_index = -1;
		m_count = jsonStringArray.length;
	}

	@Override
	public int getID() {
		return m_index;
	}

	@Override
	public String next() {
		if (m_index < m_count - 1) {
			m_index++;
			return m_jsonString != null ? m_jsonString
					: m_jsonStringArray[m_index];
		}

		return null;
	}

}
