package com.esri.core.geometry.ogc;

public abstract class OGCGeometryCollection extends OGCGeometry {
	/**
	 * Returns the number of geometries in this GeometryCollection.
	 */
	public abstract int numGeometries();

	/**
	 * Returns the Nth geometry in this GeometryCollection.
	 * @param n The 0 based index of the geometry.
	 */
	public abstract OGCGeometry geometryN(int n);
}
