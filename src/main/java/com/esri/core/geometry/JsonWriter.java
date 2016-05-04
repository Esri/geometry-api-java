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

abstract class JsonWriter {

	abstract Object getJson();

	abstract void startObject();

	abstract void startArray();

	abstract void endObject();

	abstract void endArray();

	abstract void addFieldName(String fieldName);

	abstract void addPairObject(String fieldName);

	abstract void addPairArray(String fieldName);

	abstract void addPairString(String fieldName, String v);

	abstract void addPairDouble(String fieldName, double v);

	abstract void addPairDouble(String fieldName, double v, int precision, boolean bFixedPoint);

	abstract void addPairInt(String fieldName, int v);

	abstract void addPairBoolean(String fieldName, boolean v);

	abstract void addPairNull(String fieldName);

	abstract void addValueObject();

	abstract void addValueArray();

	abstract void addValueString(String v);

	abstract void addValueDouble(double v);

	abstract void addValueDouble(double v, int precision, boolean bFixedPoint);

	abstract void addValueInt(int v);

	abstract void addValueBoolean(boolean v);

	abstract void addValueNull();

	protected interface Action {

		static final int accept = 0;
		static final int addObject = 1;
		static final int addArray = 2;
		static final int popObject = 4;
		static final int popArray = 8;
		static final int addKey = 16;
		static final int addTerminal = 32;
		static final int addPair = 64;
		static final int addContainer = addObject | addArray;
		static final int addValue = addContainer | addTerminal;
	}

	protected interface State {

		static final int accept = 0;
		static final int start = 1;
		static final int objectStart = 2;
		static final int arrayStart = 3;
		static final int pairEnd = 4;
		static final int elementEnd = 5;
		static final int fieldNameEnd = 6;
	}
}
