package com.esri.core.geometry.ogc;

import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;

public abstract class OGCCurve extends OGCGeometry {
	public abstract double length();

	public abstract OGCPoint startPoint();

	public abstract OGCPoint endPoint();

	public abstract boolean isClosed();

	public boolean isRing() {
		return isSimple() && isClosed();
	}

	@Override
	public OGCGeometry boundary() {
		if (isClosed())
			return new OGCMultiPoint(new MultiPoint(getEsriGeometry()
					.getDescription()), esriSR);// return empty multipoint;
		else
			return new OGCMultiPoint(startPoint(), endPoint());
	}
}
