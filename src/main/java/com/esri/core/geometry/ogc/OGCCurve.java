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

import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;

public abstract class OGCCurve extends OGCGeometry {
	public abstract double length();

	public abstract OGCPoint startPoint();

	public abstract OGCPoint endPoint();

	public abstract boolean isClosed();

	public boolean isRing() {
		return isSimple() && isClosed();
	}

	@Override
	public OGCGeometry boundary() {
		if (isClosed())
			return new OGCMultiPoint(new MultiPoint(getEsriGeometry()
					.getDescription()), esriSR);// return empty multipoint;
		else
			return new OGCMultiPoint(startPoint(), endPoint());
	}
}
