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
import org.json.JSONException;


abstract class JsonReader {

	abstract JsonToken nextToken() throws Exception;

	abstract JsonToken currentToken() throws Exception;

	abstract void skipChildren() throws Exception;

	abstract String currentString() throws Exception;

	abstract double currentDoubleValue() throws Exception;

	abstract int currentIntValue() throws Exception;
}
final class JsonParserReader extends JsonReader {

	private JsonParser m_jsonParser;

	JsonParserReader(JsonParser jsonParser) {
		m_jsonParser = jsonParser;
	}

	@Override
	JsonToken nextToken() throws Exception {
		JsonToken token = m_jsonParser.nextToken();
		return token;
	}

	@Override
	JsonToken currentToken() throws Exception {
		return m_jsonParser.getCurrentToken();
	}

	@Override
	void skipChildren() throws Exception {
		m_jsonParser.skipChildren();
	}

	@Override
	String currentString() throws Exception {
		return m_jsonParser.getText();
	}

	@Override
	double currentDoubleValue() throws Exception {
		return m_jsonParser.getValueAsDouble();
	}

	@Override
	int currentIntValue() throws Exception {
		return m_jsonParser.getValueAsInt();
	}
}

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

		try {
			return m_jsonObject.get(m_keys[m_currentIndex]);
		}
		catch (JSONException e) {
			throw new GeometryException("invalid call");
		}
	}

	boolean next() {
		if (!m_bStarted) {
			m_currentIndex = 0;
			m_keys = JSONObject.getNames(m_jsonObject);
			m_bStarted = true;
		} else if (m_currentIndex != m_jsonObject.length()) {
			m_currentIndex++;
		}

		return m_currentIndex != m_jsonObject.length();
	}
}

final class JSONArrayEnumerator {

	private JSONArray m_jsonArray;
	private boolean m_bStarted;
	private int m_currentIndex;

	JSONArrayEnumerator(JSONArray jsonArray) {
		m_bStarted = false;
		m_currentIndex = -1;
		m_jsonArray = jsonArray;
	}

	Object getCurrentObject() {
		if (!m_bStarted) {
			throw new GeometryException("invalid call");
		}

		if (m_currentIndex == m_jsonArray.length()) {
			throw new GeometryException("invalid call");
		}

		try {
			return m_jsonArray.get(m_currentIndex);
		}
		catch (JSONException e) {
			throw new GeometryException("invalid call");
		}
	}

	boolean next() {
		if (!m_bStarted) {
			m_currentIndex = 0;
			m_bStarted = true;
		} else if (m_currentIndex != m_jsonArray.length()) {
			m_currentIndex++;
		}

		return m_currentIndex != m_jsonArray.length();
	}
}
