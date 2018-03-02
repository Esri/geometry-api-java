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

import com.esri.core.geometry.GeoJsonExportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorBoundary;
import com.esri.core.geometry.OperatorExportToGeoJson;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

import java.nio.ByteBuffer;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_MULTI_LINE_STRING;

public class OGCMultiLineString extends OGCMultiCurve {
	public OGCMultiLineString(Polyline poly, SpatialReference sr) {
		polyline = poly;
		esriSR = sr;
	}

	@Override
	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(),
				WktExportFlags.wktExportMultiLineString);
	}
	@Override
    public String asGeoJson() {
        OperatorExportToGeoJson op = (OperatorExportToGeoJson) OperatorFactoryLocal
                .getInstance().getOperator(Operator.Type.ExportToGeoJson);
        return op.execute(GeoJsonExportFlags.geoJsonExportPreferMultiGeometry, null, getEsriGeometry());
    }
	@Override
	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(WkbExportFlags.wkbExportMultiLineString,
				getEsriGeometry(), null);
	}

	@Override
	public OGCGeometry geometryN(int n) {
		OGCLineString ls = new OGCLineString(polyline, n, esriSR);
		return ls;
	}

	@Override
	public String geometryType() {
		return "MultiLineString";
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_OGC_MULTI_LINE_STRING + (polyline != null ? polyline.estimateMemorySize() : 0);
	}

	@Override
	public OGCGeometry boundary() {
		OperatorBoundary op = (OperatorBoundary) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Boundary);
		Geometry g = op.execute(polyline, null);
		return OGCGeometry.createFromEsriGeometry(g, esriSR, true);
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
		return polyline;
	}

	@Override
	public OGCGeometry convertToMulti()
	{
		return this;
	}
	
	Polyline polyline;
}
