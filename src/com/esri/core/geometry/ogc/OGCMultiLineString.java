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
