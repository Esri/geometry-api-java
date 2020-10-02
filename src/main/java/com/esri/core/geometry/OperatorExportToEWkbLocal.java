package com.esri.core.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.esri.core.geometry.OperatorExportToWkbLocal.exportToWKB;

public class OperatorExportToEWkbLocal extends OperatorExportToEWkb {
	@Override
	public ByteBuffer execute(int exportFlags, Geometry geometry, SpatialReference spatialReference, ProgressTracker progressTracker) {
		int srid = getSrid(spatialReference);

		int size = exportToWKB(exportFlags, geometry, null, srid);
		ByteBuffer wkbBuffer = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
		exportToWKB(exportFlags, geometry, wkbBuffer, srid);
		return wkbBuffer;
	}

	@Override
	public int execute(int exportFlags, Geometry geometry, SpatialReference spatialReference, ByteBuffer wkbBuffer, ProgressTracker progressTracker) {
		int srid = 0;
		if (spatialReference != null) {
			srid = spatialReference.getID();
		}

		return exportToWKB(exportFlags, geometry, wkbBuffer, srid);
	}

	public static int getSrid(SpatialReference spatialReference) {
		if (spatialReference != null && spatialReference.getText() != null) {
			throw new GeometryException("spatial reference for extended wkb export must be an integer id, wkt not permitted");
		}
		return spatialReference == null ? 0 : spatialReference.getID();
	}
}
