package com.esri.core.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OperatorImportFromEWkbLocal extends OperatorImportFromEWkb {
	SpatialReference m_spatialReference = null;

	@Override
	public MapGeometry execute(int importFlags, Geometry.Type type, ByteBuffer eWkbBuffer, ProgressTracker progress_tracker) {
		ByteOrder initialOrder = eWkbBuffer.order();

		// read byte ordering
		int byteOrder = eWkbBuffer.get(0);

		if (byteOrder == WkbByteOrder.wkbNDR)
			eWkbBuffer.order(ByteOrder.LITTLE_ENDIAN);
		else
			eWkbBuffer.order(ByteOrder.BIG_ENDIAN);

		OperatorImportFromWkbLocal.WkbHelper wkbHelper = new OperatorImportFromWkbLocal.WkbHelper(eWkbBuffer);

		try {
			Geometry geometry = importFromEWkb(importFlags, type, wkbHelper);
			return new MapGeometry(geometry, m_spatialReference);
		} finally {
			eWkbBuffer.order(initialOrder);
		}
	}

	private Geometry importFromEWkb(int importFlags,
	                                Geometry.Type type,
	                                OperatorImportFromWkbLocal.WkbHelper wkbHelper) {
		// read type
		int wkbType = wkbHelper.getInt(1);

		// has a spatial reference
		long wkbTypeForced = java.lang.Integer.toUnsignedLong(wkbType);
		if ((wkbTypeForced | 0x20000000) == wkbTypeForced) {
			int srid = wkbHelper.getInt(5);
			m_spatialReference = SpatialReference.create(srid);
			// offset for SRID
			wkbHelper.adjustment += 4;
		}

		boolean bEWkbZ = OperatorImportFromWkbLocal.hasEWkbZ(wkbTypeForced);
		boolean bEWkbM = OperatorImportFromWkbLocal.hasEWkbM(wkbTypeForced);

		if ((wkbTypeForced & WkbGeometryType.wkbPolygon) == WkbGeometryType.wkbPolygon) {
			if (type.value() != Geometry.GeometryType.Polygon && type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return OperatorImportFromWkbLocal.importFromWkbPolygon(false, importFlags, bEWkbZ, bEWkbM, wkbHelper);
		} else if ((wkbTypeForced & WkbGeometryType.wkbMultiPolygon) == WkbGeometryType.wkbMultiPolygon) {
			if (type.value() != Geometry.GeometryType.Polygon && type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return OperatorImportFromWkbLocal.importFromWkbPolygon(true, importFlags, bEWkbZ, bEWkbM, wkbHelper);
		} else if ((wkbTypeForced & WkbGeometryType.wkbLineString) == WkbGeometryType.wkbLineString) {
			if (type.value() != Geometry.GeometryType.Polyline && type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return OperatorImportFromWkbLocal.importFromWkbPolyline(false, importFlags, bEWkbZ, bEWkbM, wkbHelper);
		} else if ((wkbTypeForced & WkbGeometryType.wkbMultiLineString) == WkbGeometryType.wkbMultiLineString) {
			if (type.value() != Geometry.GeometryType.Polyline && type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return OperatorImportFromWkbLocal.importFromWkbPolyline(true, importFlags, bEWkbZ, bEWkbM, wkbHelper);
		} else if ((wkbTypeForced & WkbGeometryType.wkbPoint) == WkbGeometryType.wkbPoint) {
			if (type.value() != Geometry.GeometryType.Point && type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return OperatorImportFromWkbLocal.importFromWkbPoint(importFlags, bEWkbZ, bEWkbM, wkbHelper);
		} else if ((wkbTypeForced & WkbGeometryType.wkbMultiPoint) == WkbGeometryType.wkbMultiPoint) {
			if (type.value() != Geometry.GeometryType.MultiPoint && type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return OperatorImportFromWkbLocal.importFromWkbMultiPoint(importFlags, bEWkbZ, bEWkbM, wkbHelper);
		} else {
			throw new GeometryException("invalid shape type");
		}
	}
}
