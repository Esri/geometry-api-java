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

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_POINT;

public final class OGCPoint extends OGCGeometry {
	public OGCPoint(Point pt, SpatialReference sr) {
		point = pt;
		esriSR = sr;
	}

	@Override
	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(),
				WktExportFlags.wktExportPoint);
	}

	@Override
	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(WkbExportFlags.wkbExportPoint, getEsriGeometry(),
				null);
	}

	public double X() {
		return point.getX();
	}

	public double Y() {
		return point.getY();
	}

	public double Z() {
		return point.getZ();
	}

	public double M() {
		return point.getM();
	}

	@Override
	public String geometryType() {
		return "Point";
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_OGC_POINT + (point != null ? point.estimateMemorySize() : 0);
	}

	@Override
	public OGCGeometry boundary() {
		return new OGCMultiPoint(new MultiPoint(getEsriGeometry()
				.getDescription()), esriSR);// return empty point
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
	public com.esri.core.geometry.Geometry getEsriGeometry() {
		return point;
	}
	
	@Override
	public OGCGeometry convertToMulti()
	{
		return new OGCMultiPoint(point, esriSR);
	}

	com.esri.core.geometry.Point point;

}
