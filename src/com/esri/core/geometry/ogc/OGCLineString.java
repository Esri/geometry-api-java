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
