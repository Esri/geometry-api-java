package com.esri.core.geometry.ogc;

import com.esri.core.geometry.MultiPath;

public abstract class OGCMultiCurve extends OGCGeometryCollection {
	public int numGeometries() {
		MultiPath mp = (MultiPath) getEsriGeometry();
		return mp.getPathCount();
	}

	public boolean isClosed() {
		MultiPath mp = (MultiPath) getEsriGeometry();
		for (int i = 0, n = mp.getPathCount(); i < n; i++) {
			if (!mp.isClosedPathInXYPlane(i))
				return false;
		}

		return true;
	}

	public double length() {
		return getEsriGeometry().calculateLength2D();
	}
}
