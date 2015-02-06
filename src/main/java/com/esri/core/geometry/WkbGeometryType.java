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

interface WkbGeometryType {
	public static final int wkbPoint = 1;

	public static final int wkbPointZ = 1001;

	public static final int wkbPointM = 2001;

	public static final int wkbPointZM = 3001;

	public static final int wkbLineString = 2;

	public static final int wkbLineStringZ = 1002;

	public static final int wkbLineStringM = 2002;

	public static final int wkbLineStringZM = 3002;

	public static final int wkbPolygon = 3;

	public static final int wkbPolygonZ = 1003;

	public static final int wkbPolygonM = 2003;

	public static final int wkbPolygonZM = 3003;

	public static final int wkbMultiPoint = 4;

	public static final int wkbMultiPointZ = 1004;

	public static final int wkbMultiPointM = 2004;

	public static final int wkbMultiPointZM = 3004;

	public static final int wkbMultiLineString = 5;

	public static final int wkbMultiLineStringZ = 1005;

	public static final int wkbMultiLineStringM = 2005;

	public static final int wkbMultiLineStringZM = 3005;

	public static final int wkbMultiPolygon = 6;

	public static final int wkbMultiPolygonZ = 1006;

	public static final int wkbMultiPolygonM = 2006;

	public static final int wkbMultiPolygonZM = 3006;

	public static final int wkbGeometryCollection = 7;

	public static final int wkbGeometryCollectionZ = 1007;

	public static final int wkbGeometryCollectionM = 2007;

	public static final int wkbGeometryCollectionZM = 3007;

	public static final int wkbMultiPatch = 8;
}
