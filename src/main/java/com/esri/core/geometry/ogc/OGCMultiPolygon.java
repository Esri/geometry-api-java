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

public class OGCMultiPolygon extends OGCMultiSurface {

	public OGCMultiPolygon(Polygon src, SpatialReference sr) {
		polygon = src;
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
		return "MultiPolygon";
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
	
	Polygon polygon;
}
