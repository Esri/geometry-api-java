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

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.json.JSONObject;
import org.json.JSONException;

import com.esri.core.geometry.Operator.Type;

/**
 *Import from JSON format.
 */
public abstract class OperatorImportFromJson extends Operator {
	@Override
	public Type getType() {
		return Type.ImportFromJson;
	}

	/**
	 * Performs the ImportFromJson operation on a number of Json Strings
	 *
	 * @param  type
	 *         Use the {@link Geometry.Type} enum.
	 *
	 * @param  jsonParserCursor
	 *         a cursor to JsonParsers to use for import
	 *
	 * @return a MapGeometryCursor.
	 */
	abstract MapGeometryCursor execute(Geometry.Type type,
			JsonParserCursor jsonParserCursor);

	/**
	 * Performs the ImportFromJson operation on a single Json string
	 *
	 * @param  type
	 *         Use the {@link Geometry.Type} enum.
	 *
	 * @param  jsonParser
	 *         a JSON parser to use for import
	 *
	 * @return a MapGeometry.
	 */
	public abstract MapGeometry execute(Geometry.Type type,
			JsonParser jsonParser);

	/**
	 * Performs the ImportFromJson operation on a single Json string
	 *
	 * @param  type
	 *         Use the {@link Geometry.Type} enum.
	 *
	 * @param  string
	 *         JSON text to parse and import
	 *
	 * @return a MapGeometry.
	 *
	 * @throws JsonParseException
	 *          the json parse exception
	 */
	public abstract MapGeometry execute(Geometry.Type type, String string)
			throws JsonParseException, IOException;
	
	/**
	 *Performs the ImportFromJson operation on a JSONObject
	 *
	 * @param  type
	 *         Use the {@link Geometry.Type} enum.
	 *
	 * @param  jsonObject
	 *         JSON object to import
	 *
	 * @return a MapGeometry.
	 *
	 * @throws JsonParseException
	 *          the json parse exception
	 *
	 * @throws IOException
	 *          the IO exception from import
	 */
	public abstract MapGeometry execute(Geometry.Type type, JSONObject jsonObject)
			throws JSONException, IOException;	

	
	public static OperatorImportFromJson local() {
		return (OperatorImportFromJson) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ImportFromJson);
	}

}
