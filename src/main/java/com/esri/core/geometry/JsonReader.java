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

/**
 * An abstract reader for Json.
 * 
 * See JsonParserReader for a concrete implementation around JsonParser.
 */
abstract public interface JsonReader {
	public static enum Token {
		END_ARRAY,
		END_OBJECT,
		FIELD_NAME,
		START_ARRAY,
		START_OBJECT,
		VALUE_FALSE,
		VALUE_NULL,
		VALUE_NUMBER_FLOAT,
		VALUE_NUMBER_INT,
		VALUE_STRING,
		VALUE_TRUE
	}

	abstract public Token nextToken() throws JsonGeometryException;

	abstract public Token currentToken() throws JsonGeometryException;

	abstract public void skipChildren() throws JsonGeometryException;

	abstract public String currentString() throws JsonGeometryException;

	abstract public double currentDoubleValue() throws JsonGeometryException;

	abstract public int currentIntValue() throws JsonGeometryException;
	
	abstract public boolean currentBooleanValue() throws JsonGeometryException;
}

