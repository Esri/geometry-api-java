package com.esri.core.geometry.ogc;

public abstract class OGCMultiSurface extends OGCGeometryCollection {
	public double area() {
		return getEsriGeometry().calculateArea2D();
	}

	public OGCPoint centroid() {
		// TODO
		throw new UnsupportedOperationException();
	}

	public OGCPoint pointOnSurface() {
		// TODO
		throw new UnsupportedOperationException();
	}
}
