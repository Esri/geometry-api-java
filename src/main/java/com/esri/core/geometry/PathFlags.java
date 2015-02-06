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

interface PathFlags {
	public static final int enumClosed = 1;
	public static final int enumHasNonlinearSegments = 2;// set when the given
															// part has
															// non-linear
															// segments
	public static final int enumOGCStartPolygon = 4;// set at the start of a
													// Polygon when viewed as an
													// OGC MultiPolygon
	public static final int enumCalcMask = 4;// mask of flags that are obtained
												// by calculation and depend on
												// the order of MultiPath parts.

}
