/*
 Copyright 1995-2013 Esri

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

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * 
 * Basic 2D point class. Contains only two double fields.
 * 
 */
public final class Point2D {

	public double x;
	public double y;

	public Point2D() {
	}

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public static Point2D construct(double x, double y) {
		return new Point2D(x, y);
	}

	public void setCoords(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void setCoords(Point2D other) {
		x = other.x;
		y = other.y;
	}

	public boolean isEqual(Point2D other) {
		return x == other.x && y == other.y;
	}

	public boolean isEqual(Point2D other, double tol) {
		return (Math.abs(x - other.x) <= tol) && (Math.abs(y - other.y) <= tol);
	}

	public void sub(Point2D other) {
		x -= other.x;
		y -= other.y;
	}

	public void sub(Point2D p1, Point2D p2) {
		x = p1.x - p2.x;
		y = p1.y - p2.y;
	}

	public void add(Point2D other) {
		x += other.x;
		y += other.y;
	}

	public void add(Point2D p1, Point2D p2) {
		x = p1.x + p2.x;
		y = p1.y + p2.y;
	}

	public void negate() {
		x = -x;
		y = -y;
	}

	public void negate(Point2D other) {
		x = -other.x;
		y = -other.y;
	}

	public void interpolate(Point2D other, double alpha) {
		x = x * (1.0 - alpha) + other.x * alpha;
		y = y * (1.0 - alpha) + other.y * alpha;
	}

	public void interpolate(Point2D p1, Point2D p2, double alpha) {
		x = p1.x * (1.0 - alpha) + p2.x * alpha;
		y = p1.y * (1.0 - alpha) + p2.y * alpha;
	}

	public void scaleAdd(double f, Point2D shift) {
		x = x * f + shift.x;
		y = y * f + shift.y;
	}

	public void scaleAdd(double f, Point2D other, Point2D shift) {
		x = other.x * f + shift.x;
		y = other.y * f + shift.y;
	}

	public void scale(double f, Point2D other) {
		x = f * other.x;
		y = f * other.y;
	}

	public void scale(double f) {
		x *= f;
		y *= f;
	}

	/**
	 * Compares two vertices lexicographicaly.
	 */
	public int compare(Point2D other) {
		return y < other.y ? -1 : (y > other.y ? 1 : (x < other.x ? -1
				: (x > other.x ? 1 : 0)));
	}

	public void normalize(Point2D other) {
		double len = other.length();
		if (len == 0) {
			x = 1.0;
			y = 0.0;
		} else {
			x = other.x / len;
			y = other.y / len;
		}
	}

	public void normalize() {
		double len = length();
		if (len == 0)// (!len)
		{
			x = 1.0;
			y = 0.0;
		}
		x /= len;
		y /= len;
	}

	public double length() {
		return Math.sqrt(x * x + y * y);
	}

	public double sqrLength() {
		return x * x + y * y;
	}

	public static double distance(Point2D pt1, Point2D pt2) {
		return Math.sqrt(sqrDistance(pt1, pt2));
	}

	public double dotProduct(Point2D other) {
		return x * other.x + y * other.y;
	}

	double _dotProductAbs(Point2D other) {
		return Math.abs(x * other.x) + Math.abs(y * other.y);
	}

	public double crossProduct(Point2D other) {
		return x * other.y - y * other.x;
	}

	public void rotateDirect(double Cos, double Sin) // corresponds to the
												// Transformation2D.SetRotate(cos,
												// sin).Transform(pt)
	{
		double xx = x * Cos - y * Sin;
		double yy = x * Sin + y * Cos;
		x = xx;
		y = yy;
	}

	public void rotateReverse(double Cos, double Sin) {
		double xx = x * Cos + y * Sin;
		double yy = -x * Sin + y * Cos;
		x = xx;
		y = yy;
	}

	/**
	 * 90 degree rotation, anticlockwise. Equivalent to RotateDirect(cos(pi/2),
	 * sin(pi/2)).
	 */
	public void leftPerpendicular() {
		double xx = x;
		x = -y;
		y = xx;
	}

	/**
	 * 90 degree rotation, anticlockwise. Equivalent to RotateDirect(cos(pi/2),
	 * sin(pi/2)).
	 */
	public void leftPerpendicular(Point2D pt) {
		x = -pt.y;
		y = pt.x;
	}

	/**
	 * 270 degree rotation, anticlockwise. Equivalent to
	 * RotateDirect(-cos(pi/2), sin(-pi/2)).
	 */
	public void rightPerpendicular() {
		double xx = x;
		x = y;
		y = -xx;
	}

	/**
	 * 270 degree rotation, anticlockwise. Equivalent to
	 * RotateDirect(-cos(pi/2), sin(-pi/2)).
	 */
	public void rightPerpendicular(Point2D pt) {
		x = pt.y;
		y = -pt.x;
	}

	void _setNan() {
		x = NumberUtils.NaN();
		y = NumberUtils.NaN();
	}

	boolean _isNan() {
		return NumberUtils.isNaN(x);
	}

	// calculates which quarter of xy plane the vector lies in. First quater is
	// between vectors (1,0) and (0, 1), second between (0, 1) and (-1, 0), etc.
	// Angle intervals corresponding to quarters: 1 : [0 : 90); 2 : [90 : 180);
	// 3 : [180 : 270); 4 : [270 : 360)
	final int _getQuarter() {
		if (x > 0) {
			if (y >= 0)
				return 1; // x > 0 && y <= 0
			else
				return 4; // y < 0 && x > 0. Should be x >= 0 && y < 0. The x ==
							// 0 case is processed later.
		} else {
			if (y > 0)
				return 2; // x <= 0 && y > 0
			else
				return x == 0 ? 4 : 3; // 3: x < 0 && y <= 0. The case x == 0 &&
										// y <= 0 is attribute to the case 4.
										// The point x==0 and y==0 is a bug, but
										// will be assigned to 4.
		}
	}

	/**
	* Calculates which quarter of XY plane the vector lies in. First quarter is
	* between vectors (1,0) and (0, 1), second between (0, 1) and (-1, 0), etc.
	* The quarters are numbered counterclockwise.
	* Angle intervals corresponding to quarters: 1 : [0 : 90); 2 : [90 : 180);
	* 3 : [180 : 270); 4 : [270 : 360)
	*/
	public int getQuarter() { return _getQuarter(); }
	
	// Assume vector v1 and v2 have same origin. The function compares the
	// vectors by angle from the x axis to the vector in the counter clockwise
	// direction.
	//   >    >
	//   \   /
	// V3 \ / V1
	//     \
	//      \
	//       >V2
	// _compareVectors(V1, V2) == -1.
	// _compareVectors(V1, V3) == -1
	// _compareVectors(V2, V3) == 1
	//
	final static int _compareVectors(Point2D v1, Point2D v2) {
		int q1 = v1._getQuarter();
		int q2 = v2._getQuarter();

		if (q2 == q1) {
			double cross = v1.crossProduct(v2);
			return cross < 0 ? 1 : (cross > 0 ? -1 : 0);
		} else
			return q1 < q2 ? -1 : 1;
	}

	/**
	 * Assume vector v1 and v2 have same origin. The function compares the
	 * vectors by angle in the counter clockwise direction from the axis X.
	 * 
	 * For example, V1 makes 30 degree angle counterclockwise from horizontal x axis
	 * V2, makes 270, V3 makes 90, then 
	 * compareVectors(V1, V2) == -1.
	 * compareVectors(V1, V3) == -1.
	 * compareVectors(V2, V3) == 1.
	 * @return Returns 1 if v1 is less than v2, 0 if equal, and 1 if greater.
	 */
	public static int compareVectors(Point2D v1, Point2D v2) {
		return _compareVectors(v1, v2);
	}
	
	static class CompareVectors implements Comparator<Point2D> {
		@Override
		public int compare(Point2D v1, Point2D v2) {
			return _compareVectors((Point2D) v1, (Point2D) v2);
		}
	}

	public static double sqrDistance(Point2D pt1, Point2D pt2) {
		double dx = pt1.x - pt2.x;
		double dy = pt1.y - pt2.y;
		return dx * dx + dy * dy;
	}

	@Override
	public String toString() {
		return "(" + x + " , " + y + ")";
	}

	public void setNaN() {
		x = NumberUtils.NaN();
		y = NumberUtils.NaN();
	}

	public boolean isNaN() {
		return NumberUtils.isNaN(x) || NumberUtils.isNaN(y);
	}

	// metric = 1: Manhattan metric
	// 2: Euclidian metric (default)
	// 0: used for L-infinite (max(fabs(x), fabs(y))
	// for predefined metrics, use the DistanceMetricEnum defined in WKSPoint.h
	double _norm(int metric) {
		if (metric < 0 || _isNan())
			return NumberUtils.NaN();

		switch (metric) {
		case 0: // L-infinite
			return Math.abs(x) >= Math.abs(y) ? Math.abs(x) : Math.abs(y);

		case 1: // L1 or Manhattan metric
			return Math.abs(x) + Math.abs(y);

		case 2: // L2 or Euclidean metric
			return Math.sqrt(x * x + y * y);

		default:
			return Math
					.pow(Math.pow(x, (double) metric)
							+ Math.pow(y, (double) metric),
							1.0 / (double) metric);
		}
	}

	/**
	 * returns signed distance of point from infinite line represented by
	 * pt_1...pt_2. The returned distance is positive if this point lies on the
	 * right-hand side of the line, negative otherwise. If the two input points
	 * are equal, the (positive) distance of this point to p_1 is returned.
	 */
	double offset(/* const */Point2D pt1, /* const */Point2D pt2) {
		double newDistance = distance(pt1, pt2);
		Point2D p = construct(x, y);
		if (newDistance == 0.0)
			return distance(p, pt1);

		// get vectors relative to pt_1
		Point2D p2 = new Point2D();
		p2.setCoords(pt2);
		p2.sub(pt1);
		p.sub(pt1);

		double cross = p.crossProduct(p2);
		return cross / newDistance;
	}

	/**
	 * Calculates the orientation of the triangle formed by p->q->r. Returns 1
	 * for counter-clockwise, -1 for clockwise, and 0 for collinear. May use
	 * high precision arithmetics for some special degenerate cases.
	 */
	public static int orientationRobust(Point2D p, Point2D q, Point2D r) {
		ECoordinate det_ec = new ECoordinate();
		det_ec.set(q.x);
		det_ec.sub(p.x);

		ECoordinate rp_y_ec = new ECoordinate();
		rp_y_ec.set(r.y);
		rp_y_ec.sub(p.y);

		ECoordinate qp_y_ec = new ECoordinate();
		qp_y_ec.set(q.y);
		qp_y_ec.sub(p.y);

		ECoordinate rp_x_ec = new ECoordinate();
		rp_x_ec.set(r.x);
		rp_x_ec.sub(p.x);

		det_ec.mul(rp_y_ec);
		qp_y_ec.mul(rp_x_ec);
		det_ec.sub(qp_y_ec);

		if (!det_ec.isFuzzyZero()) {
			double det_ec_value = det_ec.value();

			if (det_ec_value < 0.0)
				return -1;

			if (det_ec_value > 0.0)
				return 1;

			return 0;
		}

		// Need extended precision

		BigDecimal det_mp = new BigDecimal(q.x);
		BigDecimal px_mp = new BigDecimal(p.x);
		BigDecimal py_mp = new BigDecimal(p.y);
		det_mp = det_mp.subtract(px_mp);

		BigDecimal rp_y_mp = new BigDecimal(r.y);
		rp_y_mp = rp_y_mp.subtract(py_mp);

		BigDecimal qp_y_mp = new BigDecimal(q.y);
		qp_y_mp = qp_y_mp.subtract(py_mp);

		BigDecimal rp_x_mp = new BigDecimal(r.x);
		rp_x_mp = rp_x_mp.subtract(px_mp);

		det_mp = det_mp.multiply(rp_y_mp);
		qp_y_mp = qp_y_mp.multiply(rp_x_mp);
		det_mp = det_mp.subtract(qp_y_mp);

		return det_mp.signum();
	}

}
