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

final class JSONUtils {

	static boolean isObjectStart(JsonReader parser) throws Exception {
		return parser.currentToken() == null ? parser.nextToken() == JsonReader.Token.START_OBJECT
				: parser.currentToken() == JsonReader.Token.START_OBJECT;
	}

	static double readDouble(JsonReader parser) {
		if (parser.currentToken() == JsonReader.Token.VALUE_NUMBER_FLOAT)
			return parser.currentDoubleValue();
		else if (parser.currentToken() == JsonReader.Token.VALUE_NUMBER_INT)
			return parser.currentIntValue();
		else if (parser.currentToken() == JsonReader.Token.VALUE_NULL)
			return NumberUtils.NaN();
		else if (parser.currentToken() == JsonReader.Token.VALUE_STRING)
			if (parser.currentString().equals("NaN"))
				return NumberUtils.NaN();

		throw new GeometryException("invalid parameter");
	}

}
