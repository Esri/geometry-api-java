/*
 Copyright 1995-2015 Esri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */


package com.esri.core.geometry;

import java.io.Serializable;

/**
 * A class that represents axis parallel 3D rectangle.
 */
public final class Envelope3D implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public double xmin;

	public double ymin;

	public double zmin;

	public double xmax;

	public double ymax;

	public double zmax;

	public static Envelope3D construct(double _xmin, double _ymin,
			double _zmin, double _xmax, double _ymax, double _zmax) {
		Envelope3D env = new Envelope3D(_xmin, _ymin, _zmin, _xmax, _ymax, _zmax);
		return env;
	}

	public Envelope3D(double _xmin, double _ymin, double _zmin, double _xmax, double _ymax, double _zmax) {
		setCoords(_xmin, _ymin, _zmin, _xmax, _ymax, _zmax);
	}

	public Envelope3D() {

	}

	public Envelope3D(Envelope3D other) {
		setCoords(other);
	}

	
	public void setInfinite() {
		xmin = NumberUtils.negativeInf();
		xmax = NumberUtils.positiveInf();
		ymin = NumberUtils.negativeInf();
		ymax = NumberUtils.positiveInf();
		zmin = NumberUtils.negativeInf();
		zmax = NumberUtils.positiveInf();
	}

	public void setEmpty() {
		xmin = NumberUtils.NaN();
		ymin = NumberUtils.NaN();
		zmin = NumberUtils.NaN();
		xmax = 0;
		ymax = 0;
		zmax = 0;
	}

	public boolean isEmpty() {
		return NumberUtils.isNaN(xmin);
	}

	public void setEmptyZ() {
		zmin = NumberUtils.NaN();
	}

	public boolean isEmptyZ() {
		return NumberUtils.isNaN(zmin);
	}

	public boolean hasEmptyDimension() {
		return isEmpty() || isEmptyZ();
	}

	public void setCoords(double _xmin, double _ymin, double _zmin,
			double _xmax, double _ymax, double _zmax) {
		xmin = _xmin;
		ymin = _ymin;
		zmin = _zmin;
		xmax = _xmax;
		ymax = _ymax;
		zmax = _zmax;
		normalize();
	}

	public void setCoords(double _x, double _y, double _z) {
		xmin = _x;
		ymin = _y;
		zmin = _z;
		xmax = _x;
		ymax = _y;
		zmax = _z;
	}

	public void setCoords(Point3D center, double width, double height,
			double depth) {
		xmin = center.x - width * 0.5;
		xmax = xmin + width;
		ymin = center.y - height * 0.5;
		ymax = ymin + height;
		zmin = center.z - depth * 0.5;
		zmax = zmin + depth;
		normalize();
	}

	public void setCoords(Envelope3D envSrc) {

		setCoords(envSrc.xmin, envSrc.ymin, envSrc.zmin, envSrc.xmax, envSrc.ymax, envSrc.zmax);
	}

	public double getWidth() {
		return xmax - xmin;
	}

	public double getHeight() {
		return ymax - ymin;
	}

	public double getDepth() {
		return zmax - zmin;
	}

	public void move(Point3D vector) {
		xmin += vector.x;
		ymin += vector.y;
		zmin += vector.z;
		xmax += vector.x;
		ymax += vector.y;
		zmax += vector.z;
	}

	public void normalize() {
		if (isEmpty())
			return;

		double min = Math.min(xmin, xmax);
		double max = Math.max(xmin, xmax);
		xmin = min;
		xmax = max;
		min = Math.min(ymin, ymax);
		max = Math.max(ymin, ymax);
		ymin = min;
		ymax = max;
		min = Math.min(zmin, zmax);
		max = Math.max(zmin, zmax);
		zmin = min;
		zmax = max;
	}

	public void copyTo(Envelope2D env) {
		env.xmin = xmin;
		env.ymin = ymin;
		env.xmax = xmax;
		env.ymax = ymax;
	}

	public void mergeNE(double x, double y, double z) {
		if (xmin > x)
			xmin = x;
		else if (xmax < x)
			xmax = x;

		if (ymin > y)
			ymin = y;
		else if (ymax < y)
			ymax = y;

		if (zmin != NumberUtils.NaN()) {
			if (zmin > z)
				zmin = z;
			else if (zmax < z)
				zmax = z;
		} else {
			zmin = z;
			zmax = z;
		}
	}

	public void merge(double x, double y, double z) {
		if (isEmpty()) {
			xmin = x;
			ymin = y;
			zmin = z;
			xmax = x;
			ymax = y;
			zmax = z;
		} else {
			mergeNE(x, y, z);
		}
	}

	public void merge(Point3D pt) {
		merge(pt.x, pt.y, pt.z);
	}
	
	public void merge(Envelope3D other) {
		if (other.isEmpty())
			return;

		merge(other.xmin, other.ymin, other.zmin);
		merge(other.xmax, other.ymax, other.zmax);
	}

	public void merge(double x1, double y1, double z1, double x2, double y2,
			double z2) {
		merge(x1, y1, z1);
		merge(x2, y2, z2);
	}

	public void inflate(double dx, double dy, double dz) {
		if (isEmpty())
			return;
		xmin -= dx;
		xmax += dx;
		ymin -= dy;
		ymax += dy;
		zmin -= dz;
		zmax += dz;
		if (xmin > xmax || ymin > ymax || zmin > zmax)
			setEmpty();
	}

	/**
	 * Checks if this envelope intersects the other.
	 *
	 * @return True if this envelope intersects the other.
	 */
	public boolean isIntersecting(Envelope3D other) {
		return !isEmpty() && !other.isEmpty() && ((xmin <= other.xmin) ? xmax >= other.xmin : other.xmax >= xmin) && // check that x projections overlap
			((ymin <= other.ymin) ? ymax >= other.ymin : other.ymax >= ymin) && // check that y projections overlap
			((zmin <= other.zmin) ? zmax >= other.zmin : other.zmax >= zmin); // check that z projections overlap
	}

	/**
	 * Intersects this envelope with the other and stores result in this
	 * envelope.
	 *
	 * @return True if this envelope intersects the other, otherwise sets this
	 * envelope to empty state and returns False.
	 */
	public boolean intersect(Envelope3D other) {
		if (isEmpty() || other.isEmpty())
			return false;

		if (other.xmin > xmin)
			xmin = other.xmin;

		if (other.xmax < xmax)
			xmax = other.xmax;

		if (other.ymin > ymin)
			ymin = other.ymin;

		if (other.ymax < ymax)
			ymax = other.ymax;

		if (other.zmin > zmin)
			zmin = other.zmin;

		if (other.zmax < zmax)
			zmax = other.zmax;

		boolean bIntersecting = xmin <= xmax && ymin <= ymax && zmin <= zmax;

		if (!bIntersecting)
			setEmpty();

		return bIntersecting;
	}

	/**
	 * Returns True if the envelope contains the other envelope (boundary
	 * inclusive).
	 */
	public boolean contains(Envelope3D other) {// Note: Will return False, if either envelope is empty.
		return other.xmin >= xmin && other.xmax <= xmax && other.ymin >= ymin && other.ymax <= ymax && other.zmin >= zmin && other.zmax <= zmax;
	}

	@Override
	public boolean equals(Object _other) {
		if (_other == this)
			return true;

		if (!(_other instanceof Envelope3D))
			return false;

		Envelope3D other = (Envelope3D) _other;
		if (isEmpty() && other.isEmpty())
			return true;

		if (xmin != other.xmin || ymin != other.ymin || zmin != other.zmin || xmax != other.xmax || ymax != other.ymax || zmax != other.zmax)
			return false;

		return true;
	}

	public void construct(Envelope1D xinterval, Envelope1D yinterval,
			Envelope1D zinterval) {
		if (xinterval.isEmpty() || yinterval.isEmpty()) {
			setEmpty();
			return;
		}

		xmin = xinterval.vmin;
		xmax = xinterval.vmax;
		ymin = yinterval.vmin;
		ymax = yinterval.vmax;
		zmin = zinterval.vmin;
		zmax = zinterval.vmax;
	}

	public void queryCorners(Point3D[] corners) {
		if ((corners == null) || (corners.length < 8))
			throw new IllegalArgumentException();

		corners[0] = new Point3D(xmin, ymin, zmin);
		corners[1] = new Point3D(xmin, ymax, zmin);
		corners[2] = new Point3D(xmax, ymax, zmin);
		corners[3] = new Point3D(xmax, ymin, zmin);
		corners[4] = new Point3D(xmin, ymin, zmax);
		corners[5] = new Point3D(xmin, ymax, zmax);
		corners[6] = new Point3D(xmax, ymax, zmax);
		corners[7] = new Point3D(xmax, ymin, zmax);

	}

	public void setFromPoints(Point3D[] points) {
		if (points == null || points.length == 0) {
			setEmpty();
			return;
		}

		Point3D p = points[0];
		setCoords(p.x, p.y, p.z);
		for (int i = 1; i < points.length; i++) {
			Point3D pt = points[i];
			mergeNE(pt.x, pt.y, pt.z);
		}
	}
}
