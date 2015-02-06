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

interface ShapeType {
	public static final int ShapeNull = 0;
	public static final int ShapePoint = 1;
	public static final int ShapePointM = 21;
	public static final int ShapePointZM = 11;
	public static final int ShapePointZ = 9;
	public static final int ShapeMultiPoint = 8;
	public static final int ShapeMultiPointM = 28;
	public static final int ShapeMultiPointZM = 18;
	public static final int ShapeMultiPointZ = 20;
	public static final int ShapePolyline = 3;
	public static final int ShapePolylineM = 23;
	public static final int ShapePolylineZM = 13;
	public static final int ShapePolylineZ = 10;
	public static final int ShapePolygon = 5;
	public static final int ShapePolygonM = 25;
	public static final int ShapePolygonZM = 15;
	public static final int ShapePolygonZ = 19;
	public static final int ShapeMultiPatchM = 31;
	public static final int ShapeMultiPatch = 32;
	public static final int ShapeGeneralPolyline = 50;
	public static final int ShapeGeneralPolygon = 51;
	public static final int ShapeGeneralPoint = 52;
	public static final int ShapeGeneralMultiPoint = 53;
	public static final int ShapeGeneralMultiPatch = 54;
	public static final int ShapeTypeLast = 55;

}
