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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

/**
 * 
 * Basic 2D point class. Contains only two double fields.
 * 
 */
public final class Point2D implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public double x;
	public double y;

	public Point2D() {
	}

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point2D(Point2D other) {
		setCoords(other);
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
	
	public boolean isEqual(double x_, double y_) { 
		return x == x_ && y == y_;
	}

	public boolean isEqual(Point2D other, double tol) {
		return (Math.abs(x - other.x) <= tol) && (Math.abs(y - other.y) <= tol);
	}

	public boolean equals(Point2D other) {
		return x == other.x && y == other.y;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;

		if (!(other instanceof Point2D))
			return false;
		
		Point2D v = (Point2D)other;
		
		return x == v.x && y == v.y;
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
		MathUtils.lerp(this, other, alpha, this);
	}

	public void interpolate(Point2D p1, Point2D p2, double alpha) {
		MathUtils.lerp(p1,  p2, alpha, this);
	}
	
	/**
	 * Calculates this = this * f + shift
	 * @param f
	 * @param shift
	 */
	public void scaleAdd(double f, Point2D shift) {
		x = x * f + shift.x;
		y = y * f + shift.y;
	}

	/**
	 * Calculates this = other * f + shift
	 * @param f
	 * @param other
	 * @param shift
	 */
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
	 * Compares two vertices lexicographically by y.
	 */
	public int compare(Point2D other) {
		return y < other.y ? -1 : (y > other.y ? 1 : (x < other.x ? -1
				: (x > other.x ? 1 : 0)));
	}
	/**
	 * Compares two vertices lexicographically by x.
	 */
	int compareX(Point2D other) {
		return x < other.x ? -1 : (x > other.x ? 1 : (y < other.y ? -1
				: (y > other.y ? 1 : 0)));
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
		return NumberUtils.isNaN(x) || NumberUtils.isNaN(y);
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
	 * Calculates the orientation of the triangle formed by p, q, r. Returns 1
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
	
	private static int inCircleRobustMP_(Point2D p, Point2D q, Point2D r, Point2D s) {
		BigDecimal sx_mp = new BigDecimal(s.x), sy_mp = new BigDecimal(s.y);

		BigDecimal psx_mp = new BigDecimal(p.x), psy_mp = new BigDecimal(p.y);
		psx_mp = psx_mp.subtract(sx_mp);
		psy_mp = psy_mp.subtract(sy_mp);

		BigDecimal qsx_mp = new BigDecimal(q.x), qsy_mp = new BigDecimal(q.y);
		qsx_mp = qsx_mp.subtract(sx_mp);
		qsy_mp = qsy_mp.subtract(sy_mp);

		BigDecimal rsx_mp = new BigDecimal(r.x), rsy_mp = new BigDecimal(r.y);
		rsx_mp = rsx_mp.subtract(sx_mp);
		rsy_mp = rsy_mp.subtract(sy_mp);

		BigDecimal pq_det_mp = psx_mp.multiply(qsy_mp).subtract(psy_mp.multiply(qsx_mp));
		BigDecimal qr_det_mp = qsx_mp.multiply(rsy_mp).subtract(qsy_mp.multiply(rsx_mp));
		BigDecimal pr_det_mp = psx_mp.multiply(rsy_mp).subtract(psy_mp.multiply(rsx_mp));

		BigDecimal p_parab_mp = psx_mp.multiply(psx_mp).add(psy_mp.multiply(psy_mp));
		BigDecimal q_parab_mp = qsx_mp.multiply(qsx_mp).add(qsy_mp.multiply(qsy_mp));
		BigDecimal r_parab_mp = rsx_mp.multiply(rsx_mp).add(rsy_mp.multiply(rsy_mp));

		BigDecimal det_mp = (p_parab_mp.multiply(qr_det_mp).subtract(q_parab_mp.multiply(pr_det_mp)))
				.add(r_parab_mp.multiply(pq_det_mp));

		return det_mp.signum();
	}

	/**
	 * Calculates if the point s is inside of the circumcircle inscribed by the clockwise oriented triangle p-q-r.
	 * Returns 1 for outside, -1 for inside, and 0 for cocircular.
	 * Note that the convention used here differs from what is commonly found in literature, which can define the relation
	 * in terms of a counter-clockwise oriented circle, and this flips the sign (think of the signed volume of the tetrahedron).
	 * May use high precision arithmetics for some special cases.
	 */
	static int inCircleRobust(Point2D p, Point2D q, Point2D r, Point2D s) {
		ECoordinate psx_ec = new ECoordinate(), psy_ec = new ECoordinate();
		psx_ec.set(p.x);
		psx_ec.sub(s.x);
		psy_ec.set(p.y);
		psy_ec.sub(s.y);

		ECoordinate qsx_ec = new ECoordinate(), qsy_ec = new ECoordinate();
		qsx_ec.set(q.x);
		qsx_ec.sub(s.x);
		qsy_ec.set(q.y);
		qsy_ec.sub(s.y);

		ECoordinate rsx_ec = new ECoordinate(), rsy_ec = new ECoordinate();
		rsx_ec.set(r.x);
		rsx_ec.sub(s.x);
		rsy_ec.set(r.y);
		rsy_ec.sub(s.y);

		ECoordinate psx_ec_qsy_ec = new ECoordinate();
		psx_ec_qsy_ec.set(psx_ec);
		psx_ec_qsy_ec.mul(qsy_ec);
		ECoordinate psy_ec_qsx_ec = new ECoordinate();
		psy_ec_qsx_ec.set(psy_ec);
		psy_ec_qsx_ec.mul(qsx_ec);
		ECoordinate qsx_ec_rsy_ec = new ECoordinate();
		qsx_ec_rsy_ec.set(qsx_ec);
		qsx_ec_rsy_ec.mul(rsy_ec);
		ECoordinate qsy_ec_rsx_ec = new ECoordinate();
		qsy_ec_rsx_ec.set(qsy_ec);
		qsy_ec_rsx_ec.mul(rsx_ec);
		ECoordinate psx_ec_rsy_ec = new ECoordinate();
		psx_ec_rsy_ec.set(psx_ec);
		psx_ec_rsy_ec.mul(rsy_ec);
		ECoordinate psy_ec_rsx_ec = new ECoordinate();
		psy_ec_rsx_ec.set(psy_ec);
		psy_ec_rsx_ec.mul(rsx_ec);

		ECoordinate pq_det_ec = new ECoordinate();
		pq_det_ec.set(psx_ec_qsy_ec);
		pq_det_ec.sub(psy_ec_qsx_ec);
		ECoordinate qr_det_ec = new ECoordinate();
		qr_det_ec.set(qsx_ec_rsy_ec);
		qr_det_ec.sub(qsy_ec_rsx_ec);
		ECoordinate pr_det_ec = new ECoordinate();
		pr_det_ec.set(psx_ec_rsy_ec);
		pr_det_ec.sub(psy_ec_rsx_ec);

		ECoordinate psx_ec_psx_ec = new ECoordinate();
		psx_ec_psx_ec.set(psx_ec);
		psx_ec_psx_ec.mul(psx_ec);
		ECoordinate psy_ec_psy_ec = new ECoordinate();
		psy_ec_psy_ec.set(psy_ec);
		psy_ec_psy_ec.mul(psy_ec);
		ECoordinate qsx_ec_qsx_ec = new ECoordinate();
		qsx_ec_qsx_ec.set(qsx_ec);
		qsx_ec_qsx_ec.mul(qsx_ec);
		ECoordinate qsy_ec_qsy_ec = new ECoordinate();
		qsy_ec_qsy_ec.set(qsy_ec);
		qsy_ec_qsy_ec.mul(qsy_ec);
		ECoordinate rsx_ec_rsx_ec = new ECoordinate();
		rsx_ec_rsx_ec.set(rsx_ec);
		rsx_ec_rsx_ec.mul(rsx_ec);
		ECoordinate rsy_ec_rsy_ec = new ECoordinate();
		rsy_ec_rsy_ec.set(rsy_ec);
		rsy_ec_rsy_ec.mul(rsy_ec);

		ECoordinate p_parab_ec = new ECoordinate();
		p_parab_ec.set(psx_ec_psx_ec);
		p_parab_ec.add(psy_ec_psy_ec);
		ECoordinate q_parab_ec = new ECoordinate();
		q_parab_ec.set(qsx_ec_qsx_ec);
		q_parab_ec.add(qsy_ec_qsy_ec);
		ECoordinate r_parab_ec = new ECoordinate();
		r_parab_ec.set(rsx_ec_rsx_ec);
		r_parab_ec.add(rsy_ec_rsy_ec);

		p_parab_ec.mul(qr_det_ec);
		q_parab_ec.mul(pr_det_ec);
		r_parab_ec.mul(pq_det_ec);

		ECoordinate det_ec = new ECoordinate();
		det_ec.set(p_parab_ec);
		det_ec.sub(q_parab_ec);
		det_ec.add(r_parab_ec);

		if (!det_ec.isFuzzyZero()) {
			double det_ec_value = det_ec.value();

			if (det_ec_value < 0.0)
				return -1;

			if (det_ec_value > 0.0)
				return 1;

			return 0;
		}

		return inCircleRobustMP_(p, q, r, s);
	}

	private static Point2D calculateCenterFromThreePointsHelperMP_(Point2D from, Point2D mid_point, Point2D to) {
		assert(!mid_point.isEqual(to) && !mid_point.isEqual(from) && !from.isEqual(to));
		BigDecimal mx = new BigDecimal(mid_point.x);
		mx = mx.subtract(new BigDecimal(from.x));
		BigDecimal my = new BigDecimal(mid_point.y);
		my = my.subtract(new BigDecimal(from.y));
		BigDecimal tx = new BigDecimal(to.x);
		tx = tx.subtract(new BigDecimal(from.x));
		BigDecimal ty = new BigDecimal(to.y);
		ty = ty.subtract(new BigDecimal(from.y));

		BigDecimal d = mx.multiply(ty);
		BigDecimal tmp = my.multiply(tx);
		d = d.subtract(tmp);

		if (d.signum() == 0) {
			return Point2D.construct(NumberUtils.NaN(), NumberUtils.NaN());
		}

		d = d.multiply(new BigDecimal(2.0));

		BigDecimal mx2 = mx.multiply(mx);
		BigDecimal my2 = my.multiply(my);
		BigDecimal m_norm2 = mx2.add(my2);
		BigDecimal tx2 = tx.multiply(tx);
		BigDecimal ty2 = ty.multiply(ty);
		BigDecimal t_norm2 = tx2.add(ty2);

		BigDecimal xo = my.multiply(t_norm2);
		tmp = ty.multiply(m_norm2);
		xo = xo.subtract(tmp);
		xo = xo.divide(d, BigDecimal.ROUND_HALF_EVEN);

		BigDecimal yo = mx.multiply(t_norm2);
		tmp = tx.multiply(m_norm2);
		yo = yo.subtract(tmp);
		yo = yo.divide(d, BigDecimal.ROUND_HALF_EVEN);

		Point2D center = Point2D.construct(from.x - xo.doubleValue(), from.y + yo.doubleValue());
		return center;
	}

	private static Point2D calculateCenterFromThreePointsHelper_(Point2D from, Point2D mid_point, Point2D to) {
		assert(!mid_point.isEqual(to) && !mid_point.isEqual(from) && !from.isEqual(to));
		ECoordinate mx = new ECoordinate(mid_point.x);
		mx.sub(from.x);
		ECoordinate my = new ECoordinate(mid_point.y);
		my.sub(from.y);
		ECoordinate tx = new ECoordinate(to.x);
		tx.sub(from.x);
		ECoordinate ty = new ECoordinate(to.y);
		ty.sub(from.y);

		ECoordinate d = new ECoordinate(mx);
		d.mul(ty);
		ECoordinate tmp = new ECoordinate(my);
		tmp.mul(tx);
		d.sub(tmp);

		if (d.value() == 0.0) {
			return Point2D.construct(NumberUtils.NaN(), NumberUtils.NaN());
		}

		d.mul(2.0);

		ECoordinate mx2 = new ECoordinate(mx);
		mx2.mul(mx);
		ECoordinate my2 = new ECoordinate(my);
		my2.mul(my);
		ECoordinate m_norm2 = new ECoordinate(mx2);
		m_norm2.add(my2);
		ECoordinate tx2 = new ECoordinate(tx);
		tx2.mul(tx);
		ECoordinate ty2 = new ECoordinate(ty);
		ty2.mul(ty);
		ECoordinate t_norm2 = new ECoordinate(tx2);
		t_norm2.add(ty2);

		ECoordinate xo = new ECoordinate(my);
		xo.mul(t_norm2);
		tmp = new ECoordinate(ty);
		tmp.mul(m_norm2);
		xo.sub(tmp);
		xo.div(d);

		ECoordinate yo = new ECoordinate(mx);
		yo.mul(t_norm2);
		tmp = new ECoordinate(tx);
		tmp.mul(m_norm2);
		yo.sub(tmp);
		yo.div(d);

		Point2D center = Point2D.construct(from.x - xo.value(), from.y + yo.value());
		double r1 = Point2D.construct(from.x - center.x, from.y - center.y).length();
		double r2 = Point2D.construct(mid_point.x - center.x, mid_point.y - center.y).length();
		double r3 = Point2D.construct(to.x - center.x, to.y - center.y).length();
		double base = r1 + Math.abs(from.x) + Math.abs(mid_point.x) + Math.abs(to.x) + Math.abs(from.y)
				+ Math.abs(mid_point.y) + Math.abs(to.y);

		double tol = 1e-15;
		if ((Math.abs(r1 - r2) <= base * tol && Math.abs(r1 - r3) <= base * tol))
			return center;//returns center value for MP_value type or when calculated radius value for from - center, mid - center, and to - center are very close.

		return Point2D.construct(NumberUtils.NaN(), NumberUtils.NaN());
	}

	static Point2D calculateCircleCenterFromThreePoints(Point2D from, Point2D mid_point, Point2D to) {
		if (from.isEqual(to) || from.isEqual(mid_point) || to.isEqual(mid_point)) {
			return new Point2D(NumberUtils.NaN(), NumberUtils.NaN());
		}

		Point2D pt = calculateCenterFromThreePointsHelper_(from, mid_point, to); //use error tracking calculations
		if (pt.isNaN())
			return calculateCenterFromThreePointsHelperMP_(from, mid_point, to); //use precise calculations
		else {
			return pt;
		}
	}
	
	@Override
	public int hashCode() {
		return NumberUtils.hash(NumberUtils.hash(x), y);
	}

	double getAxis(int ordinate) {
		assert(ordinate == 0 || ordinate == 1);
		return (ordinate == 0 ? x : y);
	}
}
