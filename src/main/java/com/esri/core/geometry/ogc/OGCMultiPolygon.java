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

import com.esri.core.geometry.GeoJsonExportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorExportToGeoJson;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

import java.nio.ByteBuffer;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_MULTI_POLYGON;

public class OGCMultiPolygon extends OGCMultiSurface {
	static public String TYPE = "MultiPolygon";
	
	public OGCMultiPolygon(Polygon src, SpatialReference sr) {
		polygon = src;
		esriSR = sr;
	}

	public OGCMultiPolygon(SpatialReference sr) {
		polygon = new Polygon();
		esriSR = sr;
	}
	
	@Override
	public String asText() {
		return GeometryEngine.geometryToWkt(getEsriGeometry(),
				WktExportFlags.wktExportMultiPolygon);
	}

	@Override
	public ByteBuffer asBinary() {
		OperatorExportToWkb op = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		return op.execute(WkbExportFlags.wkbExportMultiPolygon,
				getEsriGeometry(), null);
	}
	@Override
    public String asGeoJson() {
        OperatorExportToGeoJson op = (OperatorExportToGeoJson) OperatorFactoryLocal
                .getInstance().getOperator(Operator.Type.ExportToGeoJson);
        return op.execute(GeoJsonExportFlags.geoJsonExportPreferMultiGeometry, null, getEsriGeometry());
    }
	@Override
	public int numGeometries() {
		return polygon.getExteriorRingCount();
	}

	@Override
	public OGCGeometry geometryN(int n) {
		int exterior = 0;
		for (int i = 0; i < polygon.getPathCount(); i++) {
			if (polygon.isExteriorRing(i))
				exterior++;

			if (exterior == n + 1) {
				return new OGCPolygon(polygon, i, esriSR);
			}
		}

		throw new IllegalArgumentException("geometryN: n out of range");
	}

	@Override
	public String geometryType() {
		return TYPE;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_OGC_MULTI_POLYGON + (polygon != null ? polygon.estimateMemorySize() : 0);
	}

	@Override
	public OGCGeometry boundary() {
		Polyline polyline = new Polyline();
		polyline.add(polygon, true); // adds reversed path
		return (OGCMultiCurve) OGCGeometry.createFromEsriGeometry(polyline,
				esriSR, true);
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
		return this;
	}
	
	@Override
	public OGCGeometry reduceFromMulti() {
		int n = numGeometries();
		if (n == 0) {
			return new OGCPolygon(new Polygon(polygon.getDescription()), 0, esriSR);
		}
		
		if (n == 1) {
			return geometryN(0);
		}
		
		return this;
	}
	
	Polygon polygon;
}
