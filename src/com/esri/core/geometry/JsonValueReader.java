/*
 Copyright 1995-2013 Esri

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

import java.util.ArrayList;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONArray;
import org.json.JSONObject;


final class JsonValueReader extends JsonReader {

	private Object m_object;
	private JsonToken m_currentToken;
	private ArrayList<JsonToken> m_parentStack;
	private ArrayList<JSONObjectEnumerator> m_objIters;
	private ArrayList<JSONArrayEnumerator> m_arrIters;

	JsonValueReader(Object object) {
		m_object = object;

		boolean bJSONObject = (m_object instanceof JSONObject);
		boolean bJSONArray = (m_object instanceof JSONArray);

		if (!bJSONObject && !bJSONArray) {
			throw new IllegalArgumentException();
		}

		m_parentStack = new ArrayList<JsonToken>(0);
		m_objIters = new ArrayList<JSONObjectEnumerator>(0);
		m_arrIters = new ArrayList<JSONArrayEnumerator>(0);

		m_parentStack.ensureCapacity(4);
		m_objIters.ensureCapacity(4);
		m_arrIters.ensureCapacity(4);

		if (bJSONObject) {
			JSONObjectEnumerator objIter = new JSONObjectEnumerator((JSONObject) m_object);
			m_parentStack.add(JsonToken.START_OBJECT);
			m_objIters.add(objIter);
			m_currentToken = JsonToken.START_OBJECT;
		} else {
			JSONArrayEnumerator arrIter = new JSONArrayEnumerator((JSONArray) m_object);
			m_parentStack.add(JsonToken.START_ARRAY);
			m_arrIters.add(arrIter);
			m_currentToken = JsonToken.START_ARRAY;
		}
	}

	private void setCurrentToken_(Object obj) {
		if (obj instanceof String) {
			m_currentToken = JsonToken.VALUE_STRING;
		} else if (obj instanceof Double || obj instanceof Float) {
			m_currentToken = JsonToken.VALUE_NUMBER_FLOAT;
		} else if (obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
			m_currentToken = JsonToken.VALUE_NUMBER_INT;
		} else if (obj instanceof Boolean) {
			Boolean bObj = (Boolean) obj;
			boolean b = bObj.booleanValue();
			if (b) {
				m_currentToken = JsonToken.VALUE_TRUE;
			} else {
				m_currentToken = JsonToken.VALUE_FALSE;
			}
		} else if (obj instanceof JSONObject) {
			m_currentToken = JsonToken.START_OBJECT;
		} else if (obj instanceof JSONArray) {
			m_currentToken = JsonToken.START_ARRAY;
		} else {
			m_currentToken = JsonToken.VALUE_NULL;
		}
	}

	Object currentObject_() {
		assert (!m_parentStack.isEmpty());

		JsonToken parentType = m_parentStack.get(m_parentStack.size() - 1);

		if (parentType == JsonToken.START_OBJECT) {
			JSONObjectEnumerator objIter = m_objIters.get(m_objIters.size() - 1);
			return objIter.getCurrentObject();
		}

		JSONArrayEnumerator arrIter = m_arrIters.get(m_arrIters.size() - 1);
		return arrIter.getCurrentObject();
	}

	@Override
	JsonToken nextToken() throws Exception {
		if (m_parentStack.isEmpty()) {
			m_currentToken = JsonToken.NOT_AVAILABLE;
			return m_currentToken;
		}

		JsonToken parentType = m_parentStack.get(m_parentStack.size() - 1);

		if (parentType == JsonToken.START_OBJECT) {
			JSONObjectEnumerator iterator = m_objIters.get(m_objIters.size() - 1);

			if (m_currentToken == JsonToken.FIELD_NAME) {
				Object nextJSONValue = iterator.getCurrentObject();

				if (nextJSONValue instanceof JSONObject) {
					m_parentStack.add(JsonToken.START_OBJECT);
					m_objIters.add(new JSONObjectEnumerator((JSONObject) nextJSONValue));
					m_currentToken = JsonToken.START_OBJECT;
				} else if (nextJSONValue instanceof JSONArray) {
					m_parentStack.add(JsonToken.START_ARRAY);
					m_arrIters.add(new JSONArrayEnumerator((JSONArray) nextJSONValue));
					m_currentToken = JsonToken.START_ARRAY;
				} else {
					setCurrentToken_(nextJSONValue);
				}
			} else {
				if (iterator.next()) {
					m_currentToken = JsonToken.FIELD_NAME;
				} else {
					m_objIters.remove(m_objIters.size() - 1);
					m_parentStack.remove(m_parentStack.size() - 1);
					m_currentToken = JsonToken.END_OBJECT;
				}
			}
		} else {
			assert (parentType == JsonToken.START_ARRAY);
			JSONArrayEnumerator iterator = m_arrIters.get(m_arrIters.size() - 1);
			if (iterator.next()) {
				Object nextJSONValue = iterator.getCurrentObject();

				if (nextJSONValue instanceof JSONObject) {
					m_parentStack.add(JsonToken.START_OBJECT);
					m_objIters.add(new JSONObjectEnumerator((JSONObject) nextJSONValue));
					m_currentToken = JsonToken.START_OBJECT;
				} else if (nextJSONValue instanceof JSONArray) {
					m_parentStack.add(JsonToken.START_ARRAY);
					m_arrIters.add(new JSONArrayEnumerator((JSONArray) nextJSONValue));
					m_currentToken = JsonToken.START_ARRAY;
				} else {
					setCurrentToken_(nextJSONValue);
				}
			} else {
				m_arrIters.remove(m_arrIters.size() - 1);
				m_parentStack.remove(m_parentStack.size() - 1);
				m_currentToken = JsonToken.END_ARRAY;
			}
		}

		return m_currentToken;
	}

	@Override
	JsonToken currentToken() throws Exception {
		return m_currentToken;
	}

	@Override
	void skipChildren() throws Exception {
		assert (!m_parentStack.isEmpty());

		if (m_currentToken != JsonToken.START_OBJECT && m_currentToken != JsonToken.START_ARRAY) {
			return;
		}

		JsonToken parentType = m_parentStack.get(m_parentStack.size() - 1);

		if (parentType == JsonToken.START_OBJECT) {
			m_objIters.remove(m_objIters.size() - 1);
			m_parentStack.remove(m_parentStack.size() - 1);
			m_currentToken = JsonToken.END_OBJECT;
		} else {
			m_arrIters.remove(m_arrIters.size() - 1);
			m_parentStack.remove(m_parentStack.size() - 1);
			m_currentToken = JsonToken.END_ARRAY;
		}
	}

	@Override
	String currentString() throws Exception {
		if (m_currentToken == JsonToken.FIELD_NAME) {
			return m_objIters.get(m_objIters.size() - 1).getCurrentKey();
		}

		if (m_currentToken != JsonToken.VALUE_STRING) {
			throw new GeometryException("invalid call");
		}

		return ((String) currentObject_()).toString();
	}

	@Override
	double currentDoubleValue() throws Exception {
		if (m_currentToken != JsonToken.VALUE_NUMBER_FLOAT && m_currentToken != JsonToken.VALUE_NUMBER_INT) {
			throw new GeometryException("invalid call");
		}

		return ((Number) currentObject_()).doubleValue();
	}

	@Override
	int currentIntValue() throws Exception {
		if (m_currentToken != JsonToken.VALUE_NUMBER_INT) {
			throw new GeometryException("invalid call");
		}

		return ((Number) currentObject_()).intValue();
	}
}
