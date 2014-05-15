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
	 * @return Returns a MapGeometryCursor.
	 */
	abstract MapGeometryCursor execute(Geometry.Type type,
			JsonParserCursor jsonParserCursor);

	/**
	 *Performs the ImportFromJson operation on a single Json string
	 *@return Returns a MapGeometry.
	 */
	public abstract MapGeometry execute(Geometry.Type type,
			JsonParser jsonParser);

	/**
	 *Performs the ImportFromJson operation on a single Json string
	 *@return Returns a MapGeometry.
	 */
	public abstract MapGeometry execute(Geometry.Type type, String string)
			throws JsonParseException, IOException;
	
	/**
	 *Performs the ImportFromJson operation on a JSONObject
	 *@return Returns a MapGeometry.
	 */
	public abstract MapGeometry execute(Geometry.Type type, JSONObject jsonObject)
			throws JSONException, IOException;	

	
	public static OperatorImportFromJson local() {
		return (OperatorImportFromJson) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ImportFromJson);
	}

}
