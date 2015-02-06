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

/**
*Flags used by the OperatorExportToWkt
*/
public interface WktExportFlags {
	public static final int wktExportDefaults = 0;
	public static final int wktExportPoint = 1;
	public static final int wktExportMultiPoint = 2;
	public static final int wktExportLineString = 4;
	public static final int wktExportMultiLineString = 8;
	public static final int wktExportPolygon = 16;
	public static final int wktExportMultiPolygon = 32;
	public static final int wktExportStripZs = 64;
	public static final int wktExportStripMs = 128;
	public static final int wktExportFailIfNotSimple = 4096;
	public static final int wktExportPrecision16 = 0x2000;
	public static final int wktExportPrecision15 = 0x4000;
	public static final int wktExportPrecision14 = 0x6000;
	public static final int wktExportPrecision13 = 0x8000;
	public static final int wktExportPrecision12 = 0xa000;
	public static final int wktExportPrecision11 = 0xc000;
	public static final int wktExportPrecision10 = 0xe000;
}
