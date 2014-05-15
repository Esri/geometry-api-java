package com.esri.core.geometry.ogc;

import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.SpatialReference;

public class OGCLinearRing extends OGCLineString {

	public OGCLinearRing(MultiPath mp, int pathIndex, SpatialReference sr,
			boolean reversed) {
		super(mp, pathIndex, sr, reversed);
		if (!mp.isClosedPath(0))
			throw new IllegalArgumentException("LinearRing path must be closed");
	}

	public int numPoints() {
		if (multiPath.isEmpty())
			return 0;
		return multiPath.getPointCount() + 1;
	}

	public boolean isClosed() {
		return true;
	}

	public boolean isRing() {
		return true;
	}

	@Override
	public OGCPoint pointN(int n) {
		int nn;
		if (n == multiPath.getPathSize(0)) {
			nn = multiPath.getPathStart(0);
		} else
			nn = multiPath.getPathStart(0) + n;

		return (OGCPoint) OGCGeometry.createFromEsriGeometry(
				multiPath.getPoint(nn), esriSR);
	}
}
