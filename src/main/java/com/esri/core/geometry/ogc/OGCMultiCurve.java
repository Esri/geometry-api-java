/*
 Copyright 1995-2017 Esri

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

package com.esri.core.geometry.ogc;

import com.esri.core.geometry.MultiPath;

public abstract class OGCMultiCurve extends OGCGeometryCollection {
	public int numGeometries() {
		MultiPath mp = (MultiPath) getEsriGeometry();
		return mp.getPathCount();
	}

	public boolean isClosed() {
		MultiPath mp = (MultiPath) getEsriGeometry();
		for (int i = 0, n = mp.getPathCount(); i < n; i++) {
			if (!mp.isClosedPathInXYPlane(i))
				return false;
		}

		return true;
	}

	public double length() {
		return getEsriGeometry().calculateLength2D();
	}
}
