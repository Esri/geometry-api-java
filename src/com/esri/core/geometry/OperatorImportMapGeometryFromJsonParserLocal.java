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

import org.codehaus.jackson.JsonParser;

class OperatorImportMapGeometryFromJsonParserLocal extends
		OperatorImportMapGeometryFromJsonParser {

	@Override
	public MapGeometryCursor execute(Geometry.Type type,
			JsonParserCursor jsonParserCursor) {
		return new OperatorImportMapGeometryFromJsonParserCursor(type.value(),
				jsonParserCursor);
	}

	@Override
	public MapGeometry execute(Geometry.Type type, JsonParser jsonParser) {
		SimpleJsonParserCursor jsonParserCursor = new SimpleJsonParserCursor(
				jsonParser);
		OperatorImportMapGeometryFromJsonParserCursor cursor = new OperatorImportMapGeometryFromJsonParserCursor(
				type.value(), jsonParserCursor);
		return cursor.next();
	}

}
