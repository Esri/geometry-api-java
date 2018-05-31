/*
 Copyright 1995-2018 Esri

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
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

import java.nio.ByteBuffer;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_POLYGON;

public class OGCPolygon extends OGCSurface {
	public static String TYPE = "Polygon";
	
	public OGCPolygon(Polygon src, int exteriorRing, SpatialReference sr) {
		polygon = new Polygon();
		for (int i = exteriorRing, n = src.getPathCount(); i < n; i++) {
			if (i > exteriorRing && src.isExteriorRing(i))
				break;
			polygon.addPath(src, i, true);
		}
		esriSR = sr;
	}

	public OGCPolygon(Polygon geom, SpatialReference sr) {
		polygon = geom;
		if (geom.getExteriorRingCount() > 1)
			throw new IllegalArgumentException(
					"Polygon has to have one exterior ring. Simplify geom with OperatorSimplify.");
		esriSR = sr;
	}

	@Override
	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(),
				WktExportFlags.wktExportPolygon);
	}

	@Override
	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(WkbExportFlags.wkbExportPolygon, getEsriGeometry(),
				null);
	}

	/**
	 * Returns the exterior ring of this Polygon.
	 * @return OGCLinearRing instance.
	 */
	public OGCLineString exteriorRing() {
		if (polygon.isEmpty())
			return new OGCLinearRing((Polygon) polygon.createInstance(), 0,
					esriSR, true);
		return new OGCLinearRing(polygon, 0, esriSR, true);
	}

	/**
	 * Returns the number of interior rings in this Polygon.
	 */
	public int numInteriorRing() {
		return polygon.getPathCount() - 1;
	}

	/**
	 * Returns the Nth interior ring for this Polygon as a LineString.
	 * @param n The 0 based index of the interior ring.
	 * @return OGCLinearRing instance.
	 */
	public OGCLineString interiorRingN(int n) {
		return new OGCLinearRing(polygon, n + 1, esriSR, true);
	}

	@Override
	public OGCMultiCurve boundary() {
		Polyline polyline = new Polyline();
		polyline.add(polygon, true); // adds reversed path
		return (OGCMultiCurve) OGCGeometry.createFromEsriGeometry(polyline,
				esriSR, true);
	}

	@Override
	public String geometryType() {
		return TYPE;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_OGC_POLYGON + (polygon != null ? polygon.estimateMemorySize() : 0);
	}

	@Override
	public OGCGeometry locateAlong(double mValue) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public OGCGeometry locateBetween(double mStart, double mEnd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Geometry getEsriGeometry() {
		return polygon;
	}

	@Override
	public OGCGeometry convertToMulti()
	{
		return new OGCMultiPolygon(polygon, esriSR);
	}
	
	@Override
	public OGCGeometry reduceFromMulti() {
		return this;
	}
	
	Polygon polygon;
}
