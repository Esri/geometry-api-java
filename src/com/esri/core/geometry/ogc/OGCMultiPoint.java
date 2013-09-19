package com.esri.core.geometry.ogc;

import java.nio.ByteBuffer;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.OperatorUnion;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WkbExportFlags;
import com.esri.core.geometry.WktExportFlags;

public class OGCMultiPoint extends OGCGeometryCollection {
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
		return "MultiPoint";
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
	
	private com.esri.core.geometry.MultiPoint multiPoint;
}
