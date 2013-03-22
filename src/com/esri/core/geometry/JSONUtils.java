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

import java.io.IOException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

final class JSONUtils {

	static boolean isObjectStart(JsonParser parser) throws Exception {
		return parser.getCurrentToken() == null ? parser.nextToken() == JsonToken.START_OBJECT
				: parser.getCurrentToken() == JsonToken.START_OBJECT;
	}

	static double readDouble(JsonParser parser) throws JsonParseException,
			IOException, Exception {
		if (parser.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT)
			return parser.getDoubleValue();
		else if (parser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT)
			return parser.getIntValue();
		else if (parser.getCurrentToken() == JsonToken.VALUE_NULL)
			return NumberUtils.NaN();
		else if (parser.getCurrentToken() == JsonToken.VALUE_STRING)
			if (parser.getText().equals("NaN"))
				return NumberUtils.NaN();

		throw new GeometryException("invalid parameter");
	}

}
