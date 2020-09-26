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
	int wkbZ = 1000;
	int wkbM = 2000;
	int wkbZM = 3000;
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

	// | 0x80000000
	long eWkbZ = java.lang.Integer.toUnsignedLong(0x80000000);

	int eWkbPointZ = 0x80000001;
	int eWkbLineStringZ = 0x80000002;
	int eWkbPolygonZ = 0x80000003;
	int eWkbMultiPointZ = 0x80000004;
	int eWkbMultiLineStringZ = 0x80000005;
	int eWkbMultiPolygonZ = 0x80000006;
	int eWkbGeometryCollectionZ = 0x80000007;

	// | 0x40000000
	long eWkbM = java.lang.Integer.toUnsignedLong(0x40000000);
	int eWkbPointM = 0x40000001;
	int eWkbLineStringM = 0x40000002;
	int eWkbPolygonM = 0x40000003;
	int eWkbMultiPointM = 0x40000004;
	int eWkbMultiLineStringM = 0x40000005;
	int eWkbMultiPolygonM = 0x40000006;
	int eWkbGeometryCollectionM = 0x40000007;

	// | 0x40000000 | 0x80000000
	int eWkbPointZM = 0xC0000001;
	int eWkbLineStringZM = 0xC0000002;
	int eWkbPolygonZM = 0xC0000003;
	int eWkbMultiPointZM = 0xC0000004;
	int eWkbMultiLineStringZM = 0xC0000005;
	int eWkbMultiPolygonZM = 0xC0000006;
	int eWkbGeometryCollectionZM = 0xC0000007;

	// | 0x20000000
	int eWkbS = 0x20000000;
	int eWkbPointS = 0x20000001;
	int eWkbLineStringS = 0x20000002;
	int eWkbPolygonS = 0x20000003;
	int eWkbMultiPointS = 0x20000004;
	int eWkbMultiLineStringS = 0x20000005;
	int eWkbMultiPolygonS = 0x20000006;
	int eWkbGeometryCollectionS = 0x20000007;

	// | 0x20000000 | 0x80000000
	int eWkbPointZS = 0xA0000001;
	int eWkbLineStringZS = 0xA0000002;
	int eWkbPolygonZS = 0xA0000003;
	int eWkbMultiPointZS = 0xA0000004;
	int eWkbMultiLineStringZS = 0xA0000005;
	int eWkbMultiPolygonZS = 0xA0000006;
	int eWkbGeometryCollectionZS = 0xA0000007;

	// | 0x20000000 | 0x40000000
	int eWkbPointMS = 0x60000001;
	int eWkbLineStringMS = 0x60000002;
	int eWkbPolygonMS = 0x60000003;
	int eWkbMultiPointMS = 0x60000004;
	int eWkbMultiLineStringMS = 0x60000005;
	int eWkbMultiPolygonMS = 0x60000006;
	int eWkbGeometryCollectionMS = 0x60000007;

	// | 0x20000000 | 0x40000000 | 0x80000000
	int eWkbPointZMS = 0xE0000001;
	int eWkbLineStringZMS = 0xE0000002;
	int eWkbPolygonZMS = 0xE0000003;
	int eWkbMultiPointZMS = 0xE0000004;
	int eWkbMultiLineStringZMS = 0xE0000005;
	int eWkbMultiPolygonZMS = 0xE0000006;
	int eWkbGeometryCollectionZMS = 0xE0000007;
}
