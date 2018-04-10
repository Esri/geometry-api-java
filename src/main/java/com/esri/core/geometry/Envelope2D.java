/*
 Copyright 1995-2018 Esri

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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import static com.esri.core.geometry.SizeOf.SIZE_OF_ENVELOPE2D;

/**
 * An axis parallel 2-dimensional rectangle.
 */
public final class Envelope2D implements Serializable {
	private static final long serialVersionUID = 1L;

	private final static int XLESSXMIN = 1;
	// private final int XGREATERXMAX = 2;
	private final static int YLESSYMIN = 4;
	// private final int YGREATERYMAX = 8;
	private final static int XMASK = 3;
	private final static int YMASK = 12;

	public double xmin;

	public double ymin;

	public double xmax;

	public double ymax;

	public static Envelope2D construct(double _xmin, double _ymin,
			double _xmax, double _ymax) {
		Envelope2D env = new Envelope2D();
		env.xmin = _xmin;
		env.ymin = _ymin;
		env.xmax = _xmax;
		env.ymax = _ymax;
		return env;
	}

	public static Envelope2D construct(Envelope2D other) {
		Envelope2D env = new Envelope2D();
		env.setCoords(other);
		return env;
	}
	
	public Envelope2D() {
		setEmpty();
	}

	public Envelope2D(double _xmin, double _ymin, double _xmax, double _ymax) {
		xmin = _xmin;
		ymin = _ymin;
		xmax = _xmax;
		ymax = _ymax;
	}

	public Envelope2D(Envelope2D other) {
		setCoords(other);
	}

	public int estimateMemorySize()
	{
		return SIZE_OF_ENVELOPE2D;
	}
	
	public void setCoords(double _x, double _y) {
		xmin = _x;
		ymin = _y;
		xmax = _x;
		ymax = _y;
	}

	public void setCoords(double _xmin, double _ymin, double _xmax, double _ymax) {
		xmin = _xmin;
		ymin = _ymin;
		xmax = _xmax;
		ymax = _ymax;
		normalize();
	}

	public void setCoords(Point2D center, double width, double height) {
		xmin = center.x - width * 0.5;
		xmax = xmin + width;
		ymin = center.y - height * 0.5;
		ymax = ymin + height;
		normalize();
	}

	public void setCoords(Point2D pt) {
		xmin = pt.x;
		ymin = pt.y;
		xmax = pt.x;
		ymax = pt.y;
	}

	public void setCoords(Envelope2D envSrc) {
		setCoords(envSrc.xmin, envSrc.ymin, envSrc.xmax, envSrc.ymax);
	}

	public Envelope2D getInflated(double dx, double dy) {
		Envelope2D env = new Envelope2D();
		env.setCoords(this.xmin, this.ymin, this.xmax, this.ymax);
		env.inflate(dx, dy);
		return env;
	}

	/**
	 * Sets the envelope from the array of points. The envelope will be set to
	 * empty if the array is null.
	 * @param points The points to set the envelope from. No element in the array can be null.
	 */
	public void setFromPoints(Point2D[] points) {
		if (points == null || points.length == 0) {
			setEmpty();
			return;
		}

		Point2D pt = points[0];
		setCoords(pt.x, pt.y);
		for (int i = 1; i < points.length; i++) {
			Point2D pt2d = points[i];
			mergeNE(pt2d.x, pt2d.y);
		}
	}

	public void setEmpty() {
		xmin = NumberUtils.TheNaN;
		ymin = NumberUtils.TheNaN;
		xmax = NumberUtils.TheNaN;
		ymax = NumberUtils.TheNaN;
	}

	public void setInfinite() {
		xmin = NumberUtils.negativeInf();
		xmax = NumberUtils.positiveInf();
		ymin = NumberUtils.negativeInf();
		ymax = NumberUtils.positiveInf();
	}

	public boolean isEmpty() {
		return NumberUtils.isNaN(xmin) || NumberUtils.isNaN(ymin) || NumberUtils.isNaN(xmax) || NumberUtils.isNaN(ymax);
	}

	public void setCoords(Envelope1D xinterval, Envelope1D yinterval) {
		if (xinterval.isEmpty() || yinterval.isEmpty()) {
			setEmpty();
			return;
		}

		xmin = xinterval.vmin;
		xmax = xinterval.vmax;
		ymin = yinterval.vmin;
		ymax = yinterval.vmax;
	}

	public void merge(double x, double y) {
		if (isEmpty()) {
			xmin = x;
			ymin = y;
			xmax = x;
			ymax = y;
		} else {
			if (xmin > x)
				xmin = x;
			else if (xmax < x)
				xmax = x;

			if (ymin > y)
				ymin = y;
			else if (ymax < y)
				ymax = y;
		}
	}

	/**
	 * Merges a point with this envelope without checking if the envelope is
	 * empty. Use with care.
	 * @param x The x coord of the point
	 * @param y the y coord in the point
	 */
	public void mergeNE(double x, double y) {
		if (xmin > x)
			xmin = x;
		else if (xmax < x)
			xmax = x;

		if (ymin > y)
			ymin = y;
		else if (ymax < y)
			ymax = y;
	}

	public void merge(Point2D pt) {
		merge(pt.x, pt.y);
	}

	public void merge(Point3D pt) {
		merge(pt.x, pt.y);
	}

	public void merge(Envelope2D other) {
		if (other.isEmpty())
			return;

		merge(other.xmin, other.ymin);
		merge(other.xmax, other.ymax);
	}

	public void inflate(double dx, double dy) {
		if (isEmpty())
			return;
		xmin -= dx;
		xmax += dx;
		ymin -= dy;
		ymax += dy;
		if (xmin > xmax || ymin > ymax)
			setEmpty();
	}

	public void scale(double f) {
		if (f < 0.0)
			setEmpty();

		if (isEmpty())
			return;

		xmin *= f;
		xmax *= f;
		ymin *= f;
		ymax *= f;
	}

	public void zoom(double factorX, double factorY) {
		if (!isEmpty())
			setCoords(getCenter(), factorX * getWidth(), factorY * getHeight());
	}

	/**
	 * Checks if this envelope intersects the other.
	 * @param other The other envelope.
	 * @return True if this envelope intersects the other.
	 */
	public boolean isIntersecting(Envelope2D other) {
		// No need to check if empty, this will work for empty envelopes too
		// (IEEE math)
		return ((xmin <= other.xmin) ? xmax >= other.xmin : other.xmax >= xmin)
				&& // check that x projections overlap
				((ymin <= other.ymin) ? ymax >= other.ymin : other.ymax >= ymin); // check
																					// that
																					// y
																					// projections
																					// overlap
	}

	/**
	 * Checks if this envelope intersects the other assuming neither one is empty.
	 * @param other The other envelope.
	 * @return True if this envelope intersects the other. Assumes this and
	 * other envelopes are not empty.
	 */
	public boolean isIntersectingNE(Envelope2D other) {
		return ((xmin <= other.xmin) ? xmax >= other.xmin : other.xmax >= xmin)
				&& // check that x projections overlap
				((ymin <= other.ymin) ? ymax >= other.ymin : other.ymax >= ymin); // check
																					// that
																					// y
																					// projections
																					// overlap
	}

	/**
	 * Checks if this envelope intersects the other.
	 * @param xmin_
	 * @param ymin_
	 * @param xmax_
	 * @param ymax_
	 * @return True if this envelope intersects the other.
	 */
	public boolean isIntersecting(double xmin_, double ymin_, double xmax_, double ymax_) {
		// No need to check if empty, this will work for empty geoms too (IEEE
		// math)
		return ((xmin <= xmin_) ? xmax >= xmin_ : xmax_ >= xmin) && // check
																	// that x
																	// projections
																	// overlap
				((ymin <= ymin_) ? ymax >= ymin_ : ymax_ >= ymin); // check that
																	// y
																	// projections
																	// overlap
	}
	
	/**
	 * Intersects this envelope with the other and stores result in this
	 * envelope.
	 * @param other The other envelope.
	 * @return True if this envelope intersects the other, otherwise sets this
	 *         envelope to empty state and returns False.
	 */
	public boolean intersect(Envelope2D other) {
		if (isEmpty() || other.isEmpty()) {
			setEmpty();
			return false;
		}

		if (other.xmin > xmin)
			xmin = other.xmin;

		if (other.xmax < xmax)
			xmax = other.xmax;

		if (other.ymin > ymin)
			ymin = other.ymin;

		if (other.ymax < ymax)
			ymax = other.ymax;

		boolean bIntersecting = xmin <= xmax && ymin <= ymax;

		if (!bIntersecting)
			setEmpty();

		return bIntersecting;
	}

	/**
	 * Queries a corner of the envelope.
	 * 
	 * @param index
	 *            Indicates a corner of the envelope.
	 *            <p>
	 *            0 means lower left or (xmin, ymin)
	 *            <p>
	 *            1 means upper left or (xmin, ymax)
	 *            <p>
	 *            2 means upper right or (xmax, ymax)
	 *            <p>
	 *            3 means lower right or (xmax, ymin)
	 * @return Point at a corner of the envelope.
	 * 
	 */
	public Point2D queryCorner(int index) {
		switch (index) {
		case 0:
			return Point2D.construct(xmin, ymin);
		case 1:
			return Point2D.construct(xmin, ymax);
		case 2:
			return Point2D.construct(xmax, ymax);
		case 3:
			return Point2D.construct(xmax, ymin);
		default:
			throw new IndexOutOfBoundsException();

		}
	}

	/**
	 * Queries corners into a given array. The array length must be at least
	 * 4. Starts from the lower left corner and goes clockwise.
	 * @param corners The array of four points.
	 */
	public void queryCorners(Point2D[] corners) {
		if ((corners == null) || (corners.length < 4))
			throw new IllegalArgumentException();
		if (corners[0] != null)
			corners[0].setCoords(xmin, ymin);
		else
			corners[0] = new Point2D(xmin, ymin);

		if (corners[1] != null)
			corners[1].setCoords(xmin, ymax);
		else
			corners[1] = new Point2D(xmin, ymax);

		if (corners[2] != null)
			corners[2].setCoords(xmax, ymax);
		else
			corners[2] = new Point2D(xmax, ymax);

		if (corners[3] != null)
			corners[3].setCoords(xmax, ymin);
		else
			corners[3] = new Point2D(xmax, ymin);
	}

	/**
	 * Queries corners into a given array in reversed order. The array length
	 * must be at least 4. Starts from the lower left corner and goes
	 * counterclockwise.
	 * @param corners The array of four points.
	 */
	public void queryCornersReversed(Point2D[] corners) {
		if (corners == null || ((corners != null) && (corners.length < 4)))
			throw new IllegalArgumentException();
		if (corners[0] != null)
			corners[0].setCoords(xmin, ymin);
		else
			corners[0] = new Point2D(xmin, ymin);

		if (corners[1] != null)
			corners[1].setCoords(xmax, ymin);
		else
			corners[1] = new Point2D(xmax, ymin);

		if (corners[2] != null)
			corners[2].setCoords(xmax, ymax);
		else
			corners[2] = new Point2D(xmax, ymax);
		
		if (corners[3] != null)
			corners[3].setCoords(xmin, ymax);
		else
			corners[3] = new Point2D(xmin, ymax);
	}

	public double getArea() {
		if (isEmpty())
			return 0;
		return getWidth() * getHeight();
	}

	public double getLength() {
		if (isEmpty())
			return 0;
		return 2.0 * (getWidth() + getHeight());
	}

	public void setFromPoints(Point2D[] points, int count) {
		if (count == 0) {
			setEmpty();
			return;
		}
		xmin = points[0].x;
		ymin = points[0].y;
		xmax = xmin;
		ymax = ymin;
		for (int i = 1; i < count; i++) {
			Point2D pt = points[i];
			if (pt.x < xmin)
				xmin = pt.x;
			else if (pt.x > xmax)
				xmax = pt.x;
			if (pt.y < ymin)
				ymin = pt.y;
			else if (pt.y > ymax)
				ymax = pt.y;
		}
	}

	public void reaspect(double arWidth, double arHeight) {
		if (isEmpty())
			return;
		double newAspectRatio = arWidth / arHeight;
		double widthHalf = getWidth() * 0.5;
		double heightHalf = getHeight() * 0.5;

		double newWidthHalf = heightHalf * newAspectRatio;
		if (widthHalf <= newWidthHalf) {// preserve height, increase width
			double xc = getCenterX();
			xmin = xc - newWidthHalf;
			xmax = xc + newWidthHalf;
		} else {// preserve the width, increase height
			double newHeightHalf = widthHalf / newAspectRatio;
			double yc = getCenterY();
			ymin = yc - newHeightHalf;
			ymax = yc + newHeightHalf;
		}

		normalize();
	}

	public double getCenterX() {
		double cx = (xmax + xmin) / 2d;
		return cx;
	}

	public double getCenterY() {
		double cy = (ymax + ymin) / 2d;
		return cy;
	}

	public double getWidth() {
		return xmax - xmin;
	}

	public double getHeight() {
		return ymax - ymin;
	}

	/**
	 * Moves the Envelope by given distance.
	 * @param dx
	 * @param dy
	 */
	public void move(double dx, double dy) {
		if (isEmpty())
			return;
		xmin += dx;
		ymin += dy;
		xmax += dx;
		ymax += dy;
	}

	public void centerAt(double x, double y) {
		move(x - getCenterX(), y - getCenterY());
	}

	void centerAt(Point2D pt) {
		centerAt(pt.x, pt.y);
	}

	public void offset(double dx, double dy) {
		xmin += dx;// NaN remains NaN
		xmax += dx;
		ymin += dy;
		ymax += dy;
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
	}

	public void queryLowerLeft(Point2D pt) {
		pt.setCoords(xmin, ymin);
	}

	public void queryLowerRight(Point2D pt) {
		pt.setCoords(xmax, ymin);
	}

	public void queryUpperLeft(Point2D pt) {
		pt.setCoords(xmin, ymax);
	}

	public void queryUpperRight(Point2D pt) {
		pt.setCoords(xmax, ymax);
	}

	/**
	 * Returns True if this envelope is valid (empty, or has xmin less or equal
	 * to xmax, or ymin less or equal to ymax).
	 * @return True if the envelope is valid.
	 */
	public boolean isValid() {
		return isEmpty() || (xmin <= xmax && ymin <= ymax);
	}

	/**
	 * Gets the center point of the envelope. The Center Point occurs at: ((XMin
	 * + XMax) / 2, (YMin + YMax) / 2).
	 * 
	 * @return the center point
	 */
	public Point2D getCenter() {
		return new Point2D((xmax + xmin) / 2d, (ymax + ymin) / 2d);
	}

	public void queryCenter(Point2D center) {
		center.x = (xmax + xmin) / 2d;
		center.y = (ymax + ymin) / 2d;
	}

	public void centerAt(Point c) {
		double cx = (xmax - xmin) / 2d;
		double cy = (ymax - ymin) / 2d;

		xmin = c.getX() - cx;
		xmax = c.getX() + cx;
		ymin = c.getY() - cy;
		ymax = c.getY() + cy;
	}

	public Point2D getLowerLeft() {
		return new Point2D(xmin, ymin);
	}

	public Point2D getUpperLeft() {
		return new Point2D(xmin, ymax);
	}

	public Point2D getLowerRight() {
		return new Point2D(xmax, ymin);
	}

	public Point2D getUpperRight() {
		return new Point2D(xmax, ymax);
	}

	public boolean contains(Point p) {
		return contains(p.getX(), p.getY());
	}

	public boolean contains(Point2D p) {
		return contains(p.x, p.y);
	}

	public boolean contains(double x, double y) {
		// Note: This will return False, if envelope is empty, thus no need to
		// call is_empty().
		return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
	}

	/**
	 * Returns True if the envelope contains the other envelope (boundary
	 * inclusive).
	 * @param other The other envelope.
	 * @return True if this contains the other.
	 */
	public boolean contains(Envelope2D other) {// Note: Will return False, if
												// either envelope is empty.
		return other.xmin >= xmin && other.xmax <= xmax && other.ymin >= ymin
				&& other.ymax <= ymax;
	}

	/**
	 * Returns True if the envelope contains the point (boundary exclusive).
	 * @param x
	 * @param y
	 * @return True if this contains the point.
	 * */
	public boolean containsExclusive(double x, double y) {
		// Note: This will return False, if envelope is empty, thus no need to
		// call is_empty().
		return x > xmin && x < xmax && y > ymin && y < ymax;
	}

	/**
	 * Returns True if the envelope contains the point (boundary exclusive).
	 */
	public boolean containsExclusive(Point2D pt) {
		return containsExclusive(pt.x, pt.y);
	}

	/**
	 * Returns True if the envelope contains the other envelope (boundary
	 * exclusive).
	 * @param other The other envelope
	 * @return True if this contains the other, boundary exclusive.
	 */
	boolean containsExclusive(Envelope2D other) {
		// Note: This will return False, if either envelope is empty, thus no
		// need to call is_empty().
		return other.xmin > xmin && other.xmax < xmax && other.ymin > ymin
				&& other.ymax < ymax;
	}

	@Override
	public boolean equals(Object _other) {
		if (_other == this)
			return true;

		if (!(_other instanceof Envelope2D))
			return false;

		Envelope2D other = (Envelope2D) _other;
		if (isEmpty() && other.isEmpty())
			return true;

		if (xmin != other.xmin || ymin != other.ymin || xmax != other.xmax
				|| ymax != other.ymax)
			return false;

		return true;
	}

	@Override
	public int hashCode() {

		long bits = Double.doubleToLongBits(xmin);
		int hc = (int) (bits ^ (bits >>> 32));

		int hash = NumberUtils.hash(hc);

		bits = Double.doubleToLongBits(xmax);
		hc = (int) (bits ^ (bits >>> 32));
		hash = NumberUtils.hash(hash, hc);

		bits = Double.doubleToLongBits(ymin);
		hc = (int) (bits ^ (bits >>> 32));
		hash = NumberUtils.hash(hash, hc);

		bits = Double.doubleToLongBits(ymax);
		hc = (int) (bits ^ (bits >>> 32));
		hash = NumberUtils.hash(hash, hc);

		return hash;
	}

	Point2D _snapToBoundary(Point2D pt) {
		Point2D p = new Point2D();
		p.setCoords(pt);
		if (p._isNan())
			return p;

		if (isEmpty()) {
			p._setNan();
			return p;
		}

		if (p.x < xmin)
			p.x = xmin;
		else if (p.x > xmax)
			p.x = xmax;

		if (p.y < ymin)
			p.y = ymin;
		else if (p.y > ymax)
			p.y = ymax;

		if (!p.equals(pt))
			return p;

		// p is inside envelope
		Point2D center = getCenter();
		double deltax = p.x < center.x ? p.x - xmin : xmax - p.x;
		double deltay = p.y < center.y ? p.y - ymin : ymax - p.y;

		if (deltax < deltay)
			p.x = p.x < center.x ? xmin : xmax;
		else
			p.y = p.y < center.y ? ymin : ymax;

		return p;
	}

	// Calculates distance of point from lower left corner of envelope,
	// moving clockwise along the envelope boundary.
	// The input point is assumed to lie exactly on envelope boundary
	// If this is not the case then a projection to the nearest position on the
	// envelope boundary is performed.
	// (If the user knows that the input point does most likely not lie on the
	// boundary,
	// it is more efficient to perform ProjectToBoundary before using this
	// function).
	double _boundaryDistance(Point2D pt) {
		if (isEmpty())
			return NumberUtils.NaN();

		if (pt.x == xmin)
			return pt.y - ymin;

		double height = ymax - ymin;
		double width = xmax - xmin;

		if (pt.y == ymax)
			return height + pt.x - xmin;

		if (pt.x == xmax)
			return height + width + ymax - pt.y;

		if (pt.y == ymin)
			return height * 2.0 + width + xmax - pt.x;

		return _boundaryDistance(_snapToBoundary(pt));
	}

	// returns 0,..3 depending on which side pt lies.
	int _envelopeSide(Point2D pt) {

		if (isEmpty())
			return -1;

		double boundaryDist = _boundaryDistance(pt);
		double height = ymax - ymin;
		double width = xmax - xmin;

		if (boundaryDist < height)
			return 0;

		if ((boundaryDist -= height) < width)
			return 1;

		return boundaryDist - width < height ? 2 : 3;
	}

	double _calculateToleranceFromEnvelope() {
		if (isEmpty())
			return NumberUtils.doubleEps() * 100.0; // GEOMTERYX_EPSFACTOR
													// 100.0;
		double r = Math.abs(xmin) + Math.abs(xmax) + Math.abs(ymin)
				+ Math.abs(ymax) + 1;
		return r * NumberUtils.doubleEps() * 100.0; // GEOMTERYX_EPSFACTOR
													// 100.0;
	}

	public int clipLine(Point2D p1, Point2D p2)
	// Modified Cohen-Sutherland Line-Clipping Algorithm
	// returns:
	// 0 - the segment is outside of the clipping window
	// 1 - p1 was modified
	// 2 - p2 was modified
	// 3 - p1 and p2 were modified
	// 4 - the segment is complitely inside of the clipping window
	{
		int c1 = _clipCode(p1), c2 = _clipCode(p2);

		if ((c1 & c2) != 0)// (c1 & c2)
			return 0;

		if ((c1 | c2) == 0)// (!(c1 | c2))
			return 4;

		final int res = ((c1 != 0) ? 1 : 0) | ((c2 != 0) ? 2 : 0);// (c1 ? 1 :
																	// 0) | (c2
																	// ? 2 : 0);

		do {
			double dx = p2.x - p1.x, dy = p2.y - p1.y;

			boolean bDX = dx > dy;

			if (bDX) {
				if ((c1 & XMASK) != 0)// (c1 & XMASK)
				{
					if ((c1 & XLESSXMIN) != 0)// (c1 & XLESSXMIN)
					{
						p1.y += dy * (xmin - p1.x) / dx;
						p1.x = xmin;
					} else {
						p1.y += dy * (xmax - p1.x) / dx;
						p1.x = xmax;
					}

					c1 = _clipCode(p1);
				} else if ((c2 & XMASK) != 0)// (c2 & XMASK)
				{
					if ((c2 & XLESSXMIN) != 0) {
						p2.y += dy * (xmin - p2.x) / dx;
						p2.x = xmin;
					} else {
						p2.y += dy * (xmax - p2.x) / dx;
						p2.x = xmax;
					}

					c2 = _clipCode(p2);
				} else if (c1 != 0)// (c1)
				{
					if ((c1 & YLESSYMIN) != 0)// (c1 & YLESSYMIN)
					{
						p1.x += dx * (ymin - p1.y) / dy;
						p1.y = ymin;
					} else {
						p1.x += dx * (ymax - p1.y) / dy;
						p1.y = ymax;
					}

					c1 = _clipCode(p1);
				} else {
					if ((c2 & YLESSYMIN) != 0)// (c2 & YLESSYMIN)
					{
						p2.x += dx * (ymin - p2.y) / dy;
						p2.y = ymin;
					} else {
						p2.x += dx * (ymax - p2.y) / dy;
						p2.y = ymax;
					}

					c2 = _clipCode(p2);
				}
			} else {
				if ((c1 & YMASK) != 0)// (c1 & YMASK)
				{
					if ((c1 & YLESSYMIN) != 0)// (c1 & YLESSYMIN)
					{
						p1.x += dx * (ymin - p1.y) / dy;
						p1.y = ymin;
					} else {
						p1.x += dx * (ymax - p1.y) / dy;
						p1.y = ymax;
					}

					c1 = _clipCode(p1);
				} else if ((c2 & YMASK) != 0)// (c2 & YMASK)
				{
					if ((c2 & YLESSYMIN) != 0) // (c2 & YLESSYMIN)
					{
						p2.x += dx * (ymin - p2.y) / dy;
						p2.y = ymin;
					} else {
						p2.x += dx * (ymax - p2.y) / dy;
						p2.y = ymax;
					}

					c2 = _clipCode(p2);
				} else if (c1 != 0)// (c1)
				{
					if ((c1 & XLESSXMIN) != 0)// (c1 & XLESSXMIN)
					{
						p1.y += dy * (xmin - p1.x) / dx;
						p1.x = xmin;
					} else {
						p1.y += dy * (xmax - p1.x) / dx;
						p1.x = xmax;
					}

					c1 = _clipCode(p1);
				} else {
					if ((c2 & XLESSXMIN) != 0)// (c2 & XLESSXMIN)
					{
						p2.y += dy * (xmin - p2.x) / dx;
						p2.x = xmin;
					} else {
						p2.y += dy * (xmax - p2.x) / dx;
						p2.x = xmax;
					}

					c2 = _clipCode(p2);
				}

				/*
				 * if (c1) //original code. Faster, but less robust numerically.
				 * ( //The Cohen-Sutherland Line-Clipping Algorithm) { if (c1 &
				 * XLESSXMIN) { p1.y += dy * (xmin - p1.x) / dx; p1.x = xmin; }
				 * else if (c1 & XGREATERXMAX) { p1.y += dy * (xmax - p1.x) /
				 * dx; p1.x = xmax; } else if (c1 & YLESSYMIN) { p1.x += dx *
				 * (ymin - p1.y) / dy; p1.y = ymin; } else if (c1 &
				 * YGREATERYMAX) { p1.x += dx * (ymax - p1.y) / dy; p1.y = ymax;
				 * }
				 * 
				 * c1 = _clipCode(p1, ClipRect); } else { if (c2 & XLESSXMIN) {
				 * p2.y += dy * (xmin - p2.x) / dx; p2.x = xmin; } else if (c2 &
				 * XGREATERXMAX) { p2.y += dy * (xmax - p2.x) / dx; p2.x = xmax;
				 * } else if (c2 & YLESSYMIN) { p2.x += dx * (ymin - p2.y) / dy;
				 * p2.y = ymin; } else if (c2 & YGREATERYMAX) { p2.x += dx *
				 * (ymax - p2.y) / dy; p2.y = ymax; }
				 * 
				 * c2 = _clipCode(p2, ClipRect); }
				 */
			}

			if ((c1 & c2) != 0)// (c1 & c2)
				return 0;

		} while ((c1 | c2) != 0);// (c1 | c2);

		return res;
	}

	int _clipCode(Point2D p)// returns a code from the Cohen-Sutherland (0000 is
							// boundary inclusive)
	{
		int left = (p.x < xmin) ? 1 : 0;
		int right = (p.x > xmax) ? 1 : 0;
		int bottom = (p.y < ymin) ? 1 : 0;
		int top = (p.y > ymax) ? 1 : 0;
		return left | right << 1 | bottom << 2 | top << 3;
	}

	// Clips and optionally extends line within envelope; modifies point 'from',
	// 'to'.
	// Algorithm: Liang-Barsky parametric line-clipping (Foley, vanDam, Feiner,
	// Hughes, second edition, 117-124)
	// lineExtension: 0 no line eExtension, 1 extend line at from point, 2
	// extend line at endpoint, 3 extend line at both ends
	// boundaryDistances can be NULLPTR.
	// returns:
	// 0 - the segment is outside of the clipping window
	// 1 - p1 was modified
	// 2 - p2 was modified
	// 3 - p1 and p2 were modified
	// 4 - the segment is complitely inside of the clipping window
	int clipLine(Point2D p0, Point2D p1, int lineExtension, double[] segParams,
			double[] boundaryDistances) {
		if (boundaryDistances != null) {
			boundaryDistances[0] = -1.0;
			boundaryDistances[1] = -1.0;
		}

		double[] tOld = new double[2];// LOCALREFCLASS1(ArrayOf(double), int,
										// tOld, 2);
		int modified = 0;

		Point2D delta = new Point2D(p1.x - p0.x, p1.y - p0.y);

		if (delta.x == 0.0 && delta.y == 0.0) // input line degenerates to a
												// point
		{
			segParams[0] = 0.0;
			segParams[1] = 0.0;
			return contains(p0) ? 4 : 0;
		}

		segParams[0] = ((lineExtension & 1) != 0) ? NumberUtils.negativeInf()
				: 0.0;
		segParams[1] = ((lineExtension & 2) != 0) ? NumberUtils.positiveInf()
				: 1.0;
		tOld[0] = segParams[0];
		tOld[1] = segParams[1];

		if (clipLineAuxiliary(delta.x, xmin - p0.x, segParams)
				&& clipLineAuxiliary(-delta.x, p0.x - xmax, segParams)
				&& clipLineAuxiliary(delta.y, ymin - p0.y, segParams)
				&& clipLineAuxiliary(-delta.y, p0.y - ymax, segParams)) {
			if (segParams[1] < tOld[1]) {
				p1.scaleAdd(segParams[1], delta, p0);
				_snapToBoundary(p1); // needed for accuracy
				modified |= 2;

				if (boundaryDistances != null)
					boundaryDistances[1] = _boundaryDistance(p1);
			}
			if (segParams[0] > tOld[0]) {
				p0.scaleAdd(segParams[0], delta, p0);
				_snapToBoundary(p0); // needed for accuracy
				modified |= 1;

				if (boundaryDistances != null)
					boundaryDistances[0] = _boundaryDistance(p0);
			}
		}

		return modified;
	}

	boolean clipLineAuxiliary(double denominator, double numerator,
			double[] segParams) {
		double t = numerator / denominator;
		if (denominator > 0.0) {
			if (t > segParams[1])
				return false;

			if (t > segParams[0]) {
				segParams[0] = t;
				return true;
			}
		} else if (denominator < 0.0) {
			if (t < segParams[0])
				return false;

			if (t < segParams[1]) {
				segParams[1] = t;
				return true;
			}
		} else
			return numerator <= 0.0;

		return true;
	}

	/**
	 * Returns True, envelope is degenerate (Width or Height are less than
	 * tolerance). Note: this returns False for Empty envelope.
	 */
	public boolean isDegenerate(double tolerance) {
		return !isEmpty()
				&& (getWidth() <= tolerance || getHeight() <= tolerance);
	}

	Point2D _snapClip(Point2D pt)// clips the point if it is outside, then snaps
									// it to the boundary.
	{
		double x = NumberUtils.snap(pt.x, xmin, xmax);
		double y = NumberUtils.snap(pt.y, ymin, ymax);
		return new Point2D(x, y);
	}

	public boolean isPointOnBoundary(Point2D pt, double tolerance) {
		return Math.abs(pt.x - xmin) <= tolerance
				|| Math.abs(pt.x - xmax) <= tolerance
				|| Math.abs(pt.y - ymin) <= tolerance
				|| Math.abs(pt.y - ymax) <= tolerance;
	}

	/**
	 * Calculates minimum distance from this envelope to the other.
	 * Returns 0 for empty envelopes.
	 * @param other The other envelope.
	 * @return Returns the distance
	 */
	public double distance(Envelope2D other)
	{
		return Math.sqrt(sqrDistance(other));
	}

	/**
	 * Calculates minimum distance from this envelope to the point.
	 * Returns 0 for empty envelopes.
	 * @param pt2D The other point.
	 * @return Returns the distance
	 */
	public double distance(Point2D pt2D)
	{
		return Math.sqrt(sqrDistance(pt2D));
	}

	/**
	 * Calculates minimum squared distance from this envelope to the other.
	 * Returns 0 for empty envelopes.
	 * @param other The other envelope.
	 * @return Returns the squared distance
	 */
	public double sqrDistance(Envelope2D other)
	{
		double dx = 0;
		double dy = 0;
		double nn;

		nn = xmin - other.xmax;
		if (nn > dx)
			dx = nn;

		nn = ymin - other.ymax;
		if (nn > dy)
			dy = nn;

		nn = other.xmin - xmax;
		if (nn > dx)
			dx = nn;

		nn = other.ymin - ymax;
		if (nn > dy)
			dy = nn;

		return dx * dx + dy * dy;
	}

	/**
	 * Calculates minimum squared distance from this envelope to the other.
	 * Returns 0 for empty envelopes.
	 * @param xmin_
	 * @param ymin_
	 * @param xmax_
	 * @param ymax_
	 * @return Returns the squared distance.
	 */
	public double sqrDistance(double xmin_, double ymin_, double xmax_, double ymax_)
	{
		double dx = 0;
		double dy = 0;
		double nn;

		nn = xmin - xmax_;
		if (nn > dx)
			dx = nn;

		nn = ymin - ymax_;
		if (nn > dy)
			dy = nn;

		nn = xmin_ - xmax;
		if (nn > dx)
			dx = nn;

		nn = ymin_ - ymax;
		if (nn > dy)
			dy = nn;

		return dx * dx + dy * dy;
	}
	
	/**
	 *Returns squared max distance between two bounding boxes. This is furthest distance between points on the two envelopes.
	 *
	 *@param other The bounding box to calculate the max distance two.
	 *@return Squared distance value.
	 */
	public double sqrMaxDistance(Envelope2D other) {
		if (isEmpty() || other.isEmpty())
			return NumberUtils.TheNaN;

		double dist = 0;
		Point2D[] points = new Point2D[4];
		queryCorners(points);
		Point2D[] points_o = new Point2D[4];
		other.queryCorners(points_o);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				double d = Point2D.sqrDistance(points[i], points_o[j]);
				if (d > dist) {
					dist = d;
				}
			}
		}

		return dist;
	}
	
	/**
	 * Calculates minimum squared distance from this envelope to the point.
	 * Returns 0 for empty envelopes.
	 * @param pt2D The point.
	 * @return Returns the squared distance
	 */
	public double sqrDistance(Point2D pt2D)
	{
		double dx = 0;
		double dy = 0;
		double nn;

		nn = xmin - pt2D.x;
		if (nn > dx)
			dx = nn;

		nn = ymin - pt2D.y;
		if (nn > dy)
			dy = nn;

		nn = pt2D.x - xmax;
		if (nn > dx)
			dx = nn;

		nn = pt2D.y - ymax;
		if (nn > dy)
			dy = nn;

		return dx * dx + dy * dy;
	}

	public void queryIntervalX(Envelope1D env1D)
	{
		if (isEmpty()) {
			env1D.setEmpty();
		} else {
			env1D.setCoords(xmin, xmax);
		}
	}

	public void queryIntervalY(Envelope1D env1D)
	{
		if (isEmpty()) {
			env1D.setEmpty();
		} else {
			env1D.setCoords(ymin, ymax);
		}
	}
	
	 private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		 out.defaultWriteObject();
	 }
	 private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		 in.defaultReadObject();
	 }
	 private void readObjectNoData() throws ObjectStreamException {
		 setEmpty();
	 }

}
