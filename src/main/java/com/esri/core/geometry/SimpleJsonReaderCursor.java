/*
 Copyright 1995-2017 Esri

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

class SimpleJsonReaderCursor extends JsonReaderCursor {

	JsonReader m_jsonParser;
	JsonReader[] m_jsonParserArray;

	int m_index;
	int m_count;

	public SimpleJsonReaderCursor(JsonReader jsonString) {
		m_jsonParser = jsonString;
		m_index = -1;
		m_count = 1;
	}

	public SimpleJsonReaderCursor(JsonReader[] jsonStringArray) {
		m_jsonParserArray = jsonStringArray;
		m_index = -1;
		m_count = jsonStringArray.length;
	}

	@Override
	public int getID() {
		return m_index;
	}

	@Override
	public JsonReader next() {
		if (m_index < m_count - 1) {
			m_index++;
			return m_jsonParser != null ? m_jsonParser
					: m_jsonParserArray[m_index];
		}

		return null;
	}

}
