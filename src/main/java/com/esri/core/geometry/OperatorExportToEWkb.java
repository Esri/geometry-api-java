package com.esri.core.geometry;

import java.nio.ByteBuffer;

public abstract class OperatorExportToEWkb extends Operator {
	@Override
	public Type getType() {
		return Type.ExportToWkb;
	}

	/**
	 * Performs the ExportToEWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @return Returns a ByteBuffer object containing the Geometry in WKB format
	 */
	public abstract ByteBuffer execute(int exportFlags, Geometry geometry, ProgressTracker progressTracker);

	/**
	 * Performs the ExportToEWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param wkbBuffer The ByteBuffer to contain the exported Geometry in extended WKB format.
	 * @return If the input buffer is null, then the size needed for the buffer is returned. Otherwise the number of bytes written to the buffer is returned.
	 */
	public abstract int execute(int exportFlags, Geometry geometry, ByteBuffer wkbBuffer, ProgressTracker progressTracker);

	/**
	 * Performs the ExportToEWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param spatialReference spatial reference to assign in SRID field of extended wkb
	 * @return Returns a ByteBuffer object containing the Geometry in WKB format
	 */
	public abstract ByteBuffer execute(int exportFlags, Geometry geometry, SpatialReference spatialReference, ProgressTracker progressTracker);

	/**
	 * Performs the ExportToEWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param wkbBuffer The ByteBuffer to contain the exported Geometry in extended WKB format.
	 * @param spatialReference spatial reference to assign in SRID field of extended wkb
	 * @return If the input buffer is null, then the size needed for the buffer is returned. Otherwise the number of bytes written to the buffer is returned.
	 */
	public abstract int execute(int exportFlags, Geometry geometry, ByteBuffer wkbBuffer, SpatialReference spatialReference, ProgressTracker progressTracker);

	public static OperatorExportToEWkb local() {
		return (OperatorExportToEWkb) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ExportToEWkb);
	}

}
