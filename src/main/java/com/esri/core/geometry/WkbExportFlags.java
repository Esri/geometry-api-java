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
*Flags used by the OperatorExpotToWkb.
*/
public interface WkbExportFlags {
	public static final int wkbExportDefaults = 0;//!<Default flags
	public static final int wkbExportPoint = 1;
	public static final int wkbExportMultiPoint = 2;
	public static final int wkbExportLineString = 4;
	public static final int wkbExportMultiLineString = 8;
	public static final int wkbExportPolygon = 16;
	public static final int wkbExportMultiPolygon = 32;
	public static final int wkbExportStripZs = 64;
	public static final int wkbExportStripMs = 128;
	public static final int wkbExportFailIfNotSimple = 4096;
}
