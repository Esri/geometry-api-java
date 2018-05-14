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

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

import java.nio.ByteBuffer;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_LINE_STRING;

public class OGCLineString extends OGCCurve {

	/**
	 * The number of Points in this LineString.
	 */
	public int numPoints() {
		if (multiPath.isEmpty())
			return 0;
		int d = multiPath.isClosedPath(0) ? 1 : 0;
		return multiPath.getPointCount() + d;
	}

	@Override
	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(),
				WktExportFlags.wktExportLineString);
	}

	@Override
	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(WkbExportFlags.wkbExportLineString,
				getEsriGeometry(), null);
	}

	/**
	 * Returns the specified Point N in this LineString.
	 * @param n The 0 based index of the Point.
	 */
	public OGCPoint pointN(int n) {
		int nn;
		if (multiPath.isClosedPath(0) && n == multiPath.getPathSize(0)) {
			nn = multiPath.getPathStart(0);
		} else
			nn = n + multiPath.getPathStart(0);

		return (OGCPoint) OGCGeometry.createFromEsriGeometry(
				multiPath.getPoint(nn), esriSR);
	}

	@Override
	public boolean isClosed() {
		if (isEmpty())
			return false;

		return multiPath.isClosedPathInXYPlane(0);
	}

	public OGCLineString(MultiPath mp, int pathIndex, SpatialReference sr) {
		multiPath = new Polyline();
		if (!mp.isEmpty())
			multiPath.addPath(mp, pathIndex, true);
		esriSR = sr;
	}

	public OGCLineString(MultiPath mp, int pathIndex, SpatialReference sr,
			boolean reversed) {
		multiPath = new Polyline();
		if (!mp.isEmpty())
			multiPath.addPath(mp, pathIndex, !reversed);
		esriSR = sr;
	}

	@Override
	public double length() {
		return multiPath.calculateLength2D();
	}

	@Override
	public OGCPoint startPoint() {
		return pointN(0);
	}

	@Override
	public OGCPoint endPoint() {
		return pointN(numPoints() - 1);
	}

	@Override
	public String geometryType() {
		return "LineString";
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_OGC_LINE_STRING + (multiPath != null ? multiPath.estimateMemorySize() : 0);
	}

	@Override
	public OGCGeometry locateAlong(double mValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public OGCGeometry locateBetween(double mStart, double mEnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Geometry getEsriGeometry() {
		return multiPath;
	}

	@Override
	public OGCGeometry convertToMulti()
	{
		return new OGCMultiLineString((Polyline)multiPath, esriSR);
	}
	
	MultiPath multiPath;
}
