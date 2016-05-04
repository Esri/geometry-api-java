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

public interface GeoJsonExportFlags {
	public static final int geoJsonExportDefaults = 0;
	/**
	 * Export MultiXXX geometries every time, by default it will export the minimum required type. 
	 */
	public static final int geoJsonExportPreferMultiGeometry = 1;
	public static final int geoJsonExportStripZs = 2;
	public static final int geoJsonExportStripMs = 4;
	public static final int geoJsonExportSkipCRS = 8;
	public static final int geoJsonExportFailIfNotSimple = 16;
	public static final int geoJsonExportPrecision16 = 0x02000;
	public static final int geoJsonExportPrecision15 = 0x04000;
	public static final int geoJsonExportPrecision14 = 0x06000;
	public static final int geoJsonExportPrecision13 = 0x08000;
	public static final int geoJsonExportPrecision12 = 0x0a000;
	public static final int geoJsonExportPrecision11 = 0x0c000;
	public static final int geoJsonExportPrecision10 = 0x0e000;
	public static final int geoJsonExportPrecision9 = 0x10000;
	public static final int geoJsonExportPrecision8 = 0x12000;
	public static final int geoJsonExportPrecision7 = 0x14000;
	public static final int geoJsonExportPrecision6 = 0x16000;
	public static final int geoJsonExportPrecision5 = 0x18000;
	public static final int geoJsonExportPrecision4 = 0x1a000;
	public static final int geoJsonExportPrecision3 = 0x1c000;
	public static final int geoJsonExportPrecision2 = 0x1e000;
	public static final int geoJsonExportPrecision1 = 0x20000;
	public static final int geoJsonExportPrecision0 = 0x22000;
	public static final int geoJsonExportPrecisionFixedPoint = 0x40000;
}
