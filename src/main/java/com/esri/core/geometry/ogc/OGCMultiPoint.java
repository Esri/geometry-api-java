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
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

import java.nio.ByteBuffer;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_MULTI_POINT;

public class OGCMultiPoint extends OGCGeometryCollection {
	public static String TYPE = "MultiPoint";
	
	public int numGeometries() {
		return multiPoint.getPointCount();
	}

	@Override
	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(),
				WktExportFlags.wktExportMultiPoint);
	}

	@Override
	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(WkbExportFlags.wkbExportMultiPoint,
				getEsriGeometry(), null);
	}

	public OGCGeometry geometryN(int n) {
		return OGCGeometry.createFromEsriGeometry(multiPoint.getPoint(n),
				esriSR);
	}

	@Override
	public String geometryType() {
		return TYPE;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_OGC_MULTI_POINT + (multiPoint != null ? multiPoint.estimateMemorySize() : 0);
	}

	/**
	 * 
	 * @param mp
	 *            MultiPoint instance will be referenced by this OGC class
	 */
	public OGCMultiPoint(MultiPoint mp, SpatialReference sr) {
		multiPoint = mp;
		esriSR = sr;
	}

	public OGCMultiPoint(Point startPoint, SpatialReference sr) {
		multiPoint = new MultiPoint();
		multiPoint.add((Point) startPoint);
		esriSR = sr;
	}

	public OGCMultiPoint(OGCPoint startPoint, OGCPoint endPoint) {
		multiPoint = new MultiPoint();
		multiPoint.add((Point) startPoint.getEsriGeometry());
		multiPoint.add((Point) endPoint.getEsriGeometry());
		esriSR = startPoint.esriSR;
	}

	public OGCMultiPoint(SpatialReference sr) {
		esriSR = sr;
		multiPoint = new MultiPoint();
	}

	@Override
	public OGCGeometry boundary() {
		return new OGCMultiPoint((MultiPoint) multiPoint.createInstance(),
				esriSR);// return empty multipoint
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
		return multiPoint;
	}

	@Override
	public OGCGeometry convertToMulti()
	{
		return this;
	}
	
	@Override
	public OGCGeometry reduceFromMulti() {
		int n = numGeometries();
		if (n == 0) {
			return new OGCPoint(new Point(multiPoint.getDescription()), esriSR);
		}
		
		if (n == 1) {
			return geometryN(0);
		}
		
		return this;
	}
	
	private com.esri.core.geometry.MultiPoint multiPoint;
}
