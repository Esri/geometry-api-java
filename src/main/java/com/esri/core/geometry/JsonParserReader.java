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

import com.fasterxml.jackson.core.*;

/**
 * A throw in JsonReader built around the Jackson JsonParser.
 * 
 */
public class JsonParserReader implements JsonReader {

	private JsonParser m_jsonParser;

	public JsonParserReader(JsonParser jsonParser) {
		m_jsonParser = jsonParser;
	}
	
	/**
	 * Creates a JsonReader for the string.
	 * The nextToken is called by this method.
	 */
	public static JsonReader createFromString(String str) {
		try {
			JsonFactory factory = new JsonFactory();
			JsonParser jsonParser = factory.createParser(str);
	
			jsonParser.nextToken();
			return new JsonParserReader(jsonParser);
		}
		catch (Exception ex) {
			throw new JsonGeometryException(ex.getMessage());
		}
	}
	
	/**
	 * Creates a JsonReader for the string.
	 * The nextToken is not called by this method.
	 */
	public static JsonReader createFromStringNNT(String str) {
		try {
			JsonFactory factory = new JsonFactory();
			JsonParser jsonParser = factory.createParser(str);
	
			return new JsonParserReader(jsonParser);
		}
		catch (Exception ex) {
			throw new JsonGeometryException(ex.getMessage());
		}
	}
	
	private static Token mapToken(JsonToken token) {
		if (token == JsonToken.END_ARRAY)
			return Token.END_ARRAY;
		if (token == JsonToken.END_OBJECT)
			return Token.END_OBJECT;
		if (token == JsonToken.FIELD_NAME)
			return Token.FIELD_NAME;
		if (token == JsonToken.START_ARRAY)
			return Token.START_ARRAY;
		if (token == JsonToken.START_OBJECT)
			return Token.START_OBJECT;
		if (token == JsonToken.VALUE_FALSE)
			return Token.VALUE_FALSE;
		if (token == JsonToken.VALUE_NULL)
			return Token.VALUE_NULL;
		if (token == JsonToken.VALUE_NUMBER_FLOAT)
			return Token.VALUE_NUMBER_FLOAT;
		if (token == JsonToken.VALUE_NUMBER_INT)
			return Token.VALUE_NUMBER_INT;
		if (token == JsonToken.VALUE_STRING)
			return Token.VALUE_STRING;
		if (token == JsonToken.VALUE_TRUE)
			return Token.VALUE_TRUE;
		if (token == null)
			return null;

		throw new JsonGeometryException("unexpected token");
	}

	@Override
	public Token nextToken() throws JsonGeometryException {
		try {
			JsonToken token = m_jsonParser.nextToken();
			return mapToken(token);
		} catch (Exception ex) {
			throw new JsonGeometryException(ex);
		}
	}

	@Override
	public Token currentToken() throws JsonGeometryException {
		try {
			return mapToken(m_jsonParser.getCurrentToken());
		} catch (Exception ex) {
			throw new JsonGeometryException(ex);
		}
	}

	@Override
	public void skipChildren() throws JsonGeometryException {
		try {
			m_jsonParser.skipChildren();
		} catch (Exception ex) {
			throw new JsonGeometryException(ex);
		}

	}

	@Override
	public String currentString() throws JsonGeometryException {
		try {
			return m_jsonParser.getText();
		} catch (Exception ex) {
			throw new JsonGeometryException(ex);
		}

	}

	@Override
	public double currentDoubleValue() throws JsonGeometryException {
		try {
			return m_jsonParser.getValueAsDouble();
		} catch (Exception ex) {
			throw new JsonGeometryException(ex);
		}

	}

	@Override
	public int currentIntValue() throws JsonGeometryException {
		try {
			return m_jsonParser.getValueAsInt();
		} catch (Exception ex) {
			throw new JsonGeometryException(ex);
		}
	}
	
	@Override
	public boolean currentBooleanValue() {
		Token t = currentToken();
		if (t == Token.VALUE_TRUE)
			return true;
		else if (t == Token.VALUE_FALSE)
			return false;
		throw new JsonGeometryException("Not a boolean");
	}
}

