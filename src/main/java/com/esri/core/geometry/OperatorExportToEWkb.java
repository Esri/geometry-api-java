package com.esri.core.geometry;

import java.nio.ByteBuffer;

/**
 *Export to PostGIS specific Extended WKB format.
 *See also {@link OperatorImportFromEWkb} for import.
 *
 *For ISO WKB, use {@link OperatorExportToWkb} instead.
 */
public abstract class OperatorExportToEWkb extends Operator {
	@Override
	public Type getType() {
		return Type.ExportToWkb;
	}

	/**
	 * Performs the Export To EWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param spatialReference Spatial reference to assign in SRID (wkid) field of Extended WKB. Can be null.
	 * @return Returns a ByteBuffer object containing the Geometry in EWKB format
	 */
	public abstract ByteBuffer execute(int exportFlags, Geometry geometry, SpatialReference spatialReference, ProgressTracker progressTracker);

	/**
	 * Performs the Export To EWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param spatialReference spatial reference to assign in SRID field of extended wkb. Can be null.
	 * @param wkbBuffer The ByteBuffer to export Geometry to in extended WKB format.
	 * @return If the input buffer is null, then the size needed for the buffer is returned. Otherwise the number of bytes written to the buffer is returned.
	 */
	public abstract int execute(int exportFlags, Geometry geometry, SpatialReference spatialReference, ByteBuffer wkbBuffer, ProgressTracker progressTracker);

	public static OperatorExportToEWkb local() {
		return (OperatorExportToEWkb) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ExportToEWkb);
	}
}
