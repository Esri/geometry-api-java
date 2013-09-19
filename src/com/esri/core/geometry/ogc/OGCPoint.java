package com.esri.core.geometry.ogc;

import java.nio.ByteBuffer;

import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

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
