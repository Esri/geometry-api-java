package com.esri.core.geometry.ogc;

public abstract class OGCGeometryCollection extends OGCGeometry {
	/**
	 * @return the number of geometries in this GeometryCollection.
	 */
	public abstract int numGeometries();

	/**
	 * @param  n
	 *         The 0 based index of the geometry.
	 *
	 * @return the Nth geometry in this GeometryCollection.
	 */
	public abstract OGCGeometry geometryN(int n);
}
