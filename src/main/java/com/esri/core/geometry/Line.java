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

import com.esri.core.geometry.VertexDescription.Semantics;

import java.io.Serializable;

import static com.esri.core.geometry.SizeOf.SIZE_OF_LINE;

/**
 * A straight line between a pair of points.
 * 
 */
public final class Line extends Segment implements Serializable {

	@Override
	public Geometry.Type getType() {
		return Type.Line;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_LINE + estimateMemorySize(m_attributes);
	}

	@Override
	public double calculateLength2D() {
		double dx = m_xStart - m_xEnd;
		double dy = m_yStart - m_yEnd;
		return Math.sqrt(dx * dx + dy * dy);
	}

	@Override
	boolean isDegenerate(double tolerance) {
		double dx = m_xStart - m_xEnd;
		double dy = m_yStart - m_yEnd;
		return Math.sqrt(dx * dx + dy * dy) <= tolerance;
	}

	/**
	 * Indicates if the line segment is a curve.
	 */
	@Override
	public boolean isCurve() {
		return false;
	}

	@Override
	Point2D _getTangent(double t) {
		Point2D pt = new Point2D();
		pt.sub(getEndXY(), getStartXY());
		return pt;
	}

	@Override
	boolean _isDegenerate(double tolerance) {
		return calculateLength2D() <= tolerance;
	}

	// HEADER DEF

	// Cpp
	/**
	 * Creates a line segment.
	 */
	public Line() {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
	}

	Line(VertexDescription vd) {
		m_description = vd;
	}

	public Line(double x1, double y1, double x2, double y2) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		setStartXY(x1, y1);
		setEndXY(x2, y2);
	}

	@Override
	public void queryEnvelope(Envelope env) {
		env.setEmpty();
		env.assignVertexDescription(m_description);
		Envelope2D env2D = new Envelope2D();
		queryEnvelope2D(env2D);
		env.setEnvelope2D(env2D);

		for (int i = 1, n = m_description.getAttributeCount(); i < n; i++) {
			int semantics = m_description.getSemantics(i);
			for (int iord = 0, nord = VertexDescription
					.getComponentCount(semantics); i < nord; i++) {
				Envelope1D interval = queryInterval(semantics, iord);
				env.setInterval(semantics, iord, interval);
			}
		}
	}

	@Override
	public void queryEnvelope2D(Envelope2D env) {
		env.setCoords(m_xStart, m_yStart, m_xEnd, m_yEnd);
		env.normalize();
	}

	@Override
	void queryEnvelope3D(Envelope3D env) {
		env.setEmpty();
		env.merge(m_xStart, m_yStart, _getAttributeAsDbl(0, Semantics.Z, 0));
		env.merge(m_xEnd, m_yEnd, _getAttributeAsDbl(1, Semantics.Z, 0));
	}

	@Override
	public void applyTransformation(Transformation2D transform) {
		_touch();
		Point2D pt = new Point2D();
		pt.x = m_xStart;
		pt.y = m_yStart;
		transform.transform(pt, pt);
		m_xStart = pt.x;
		m_yStart = pt.y;
		pt.x = m_xEnd;
		pt.y = m_yEnd;
		transform.transform(pt, pt);
		m_xEnd = pt.x;
		m_yEnd = pt.y;
	}

	@Override
	void applyTransformation(Transformation3D transform) {
		_touch();
		Point3D pt = new Point3D();
		pt.x = m_xStart;
		pt.y = m_yStart;
		pt.z = _getAttributeAsDbl(0, Semantics.Z, 0);
		pt = transform.transform(pt);
		m_xStart = pt.x;
		m_yStart = pt.y;
		_setAttribute(0, Semantics.Z, 0, pt.z);
		pt.x = m_xEnd;
		pt.y = m_yEnd;
		pt.z = _getAttributeAsDbl(1, Semantics.Z, 0);
		pt = transform.transform(pt);
		m_xEnd = pt.x;
		m_yEnd = pt.y;
		_setAttribute(1, Semantics.Z, 0, pt.z);
	}

	@Override
	public Geometry createInstance() {
		return new Line(m_description);
	}

	@Override
	double _calculateArea2DHelper(double xorg, double yorg) {
		return ((m_xEnd - xorg) - (m_xStart - xorg))
				* ((m_yEnd - yorg) + (m_yStart - yorg)) * 0.5;
	}

	@Override
	double tToLength(double t) {
		return t * calculateLength2D();
	}

	@Override
	double lengthToT(double len) {
		return len / calculateLength2D();
	}

	double getCoordX_(double t) {
		// Must match query_coord_2D and vice verse
		// Also match get_attribute_as_dbl
		return MathUtils.lerp(m_xStart,  m_xEnd, t);
	}

	double getCoordY_(double t) {
		// Must match query_coord_2D and vice verse
		// Also match get_attribute_as_dbl
		return MathUtils.lerp(m_yStart,  m_yEnd, t);
	}

	@Override
	public void getCoord2D(double t, Point2D pt) {
		// We want:
		// 1. When t == 0, get exactly Start
		// 2. When t == 1, get exactly End
		// 3. When m_x_end == m_x_start, we want m_x_start exactly
		// 4. When m_y_end == m_y_start, we want m_y_start exactly
		MathUtils.lerp(m_xStart, m_yStart, m_xEnd, m_yEnd, t, pt);
	}

	@Override
	public Segment cut(double t1, double t2) {
		SegmentBuffer segmentBuffer = new SegmentBuffer();
		cut(t1, t2, segmentBuffer);
		return segmentBuffer.get();
	}

	@Override
	void cut(double t1, double t2, SegmentBuffer subSegmentBuffer) {
		if (subSegmentBuffer == null)
			throw new IllegalArgumentException();

		subSegmentBuffer.createLine();// Make sure buffer contains Line class.
		Segment subSegment = subSegmentBuffer.get();
		subSegment.assignVertexDescription(m_description);

		Point2D point = new Point2D();
		getCoord2D(t1, point);
		subSegment.setStartXY(point.x, point.y);
		getCoord2D(t2, point);
		subSegment.setEndXY(point.x, point.y);

		for (int iattr = 1, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int ncomps = VertexDescription.getComponentCount(semantics);

			for (int ordinate = 0; ordinate < ncomps; ordinate++) {
				double value1 = getAttributeAsDbl(t1, semantics, ordinate);
				subSegment.setStartAttribute(semantics, ordinate, value1);

				double value2 = getAttributeAsDbl(t2, semantics, ordinate);
				subSegment.setEndAttribute(semantics, ordinate, value2);
			}
		}
	}

	@Override
	public double getAttributeAsDbl(double t, int semantics, int ordinate) {
		if (semantics == VertexDescription.Semantics.POSITION)
			return ordinate == 0 ? getCoord2D(t).x : getCoord2D(t).y;

		int interpolation = VertexDescription.getInterpolation(semantics);
		switch (interpolation) {
		case VertexDescription.Interpolation.NONE:
			if (t < 0.5)
				return getStartAttributeAsDbl(semantics, ordinate);
			else
				return getEndAttributeAsDbl(semantics, ordinate);
		case VertexDescription.Interpolation.LINEAR: {
			double s = getStartAttributeAsDbl(semantics, ordinate);
			double e = getEndAttributeAsDbl(semantics, ordinate);
			return MathUtils.lerp(s,  e,  t);
		}
		case VertexDescription.Interpolation.ANGULAR: {
			throw new GeometryException("not implemented");
		}
		}

		throw GeometryException.GeometryInternalError();
	}

	@Override
	public double getClosestCoordinate(Point2D inputPt, boolean bExtrapolate) {
		double vx = m_xEnd - m_xStart;
		double vy = m_yEnd - m_yStart;
		double v2 = vx * vx + vy * vy;
		if (v2 == 0)
			return 0.5;
		double rx = inputPt.x - m_xStart;
		double ry = inputPt.y - m_yStart;
		double t = (rx * vx + ry * vy) / v2;
		if (!bExtrapolate) {
			if (t < 0.0)
				t = 0.0;
			else if (t > 1.0)
				t = 1.0;
		}

		return t;
	}

	@Override
	public int intersectionWithAxis2D(boolean b_axis_x, double ordinate,
			double[] result_ordinates, double[] parameters) {
		if (b_axis_x) {
			double a = (m_yEnd - m_yStart);

			if (a == 0)
				return (ordinate == m_yEnd) ? -1 : 0;

			double t = (ordinate - m_yStart) / a;

			if (t < 0.0 || t > 1.0)
				return 0;

			if (result_ordinates != null)
				(result_ordinates)[0] = getCoordX_(t);

			if (parameters != null)
				(parameters)[0] = t;

			return 1;
		} else {
			double a = (m_xEnd - m_xStart);

			if (a == 0)
				return (ordinate == m_xEnd) ? -1 : 0;

			double t = (ordinate - m_xStart) / a;

			if (t < 0.0 || t > 1.0)
				return 0;

			if (result_ordinates != null)
				(result_ordinates)[0] = getCoordY_(t);

			if (parameters != null)
				(parameters)[0] = t;

			return 1;
		}
	}

	// line segment can have 0 or 1 intersection interval with clipEnv2D.
	// The function return 0 or 2 segParams (e.g. 0.0, 0.4; or 0.1, 0.9; or 0.6,
	// 1.0; or 0.0, 1.0)
	// segParams will be sorted in ascending order; the order of the
	// envelopeDistances will correspond (i.e. the envelopeDistances may not be
	// in ascending order);
	// an envelopeDistance can be -1.0 if the corresponding endpoint is properly
	// inside clipEnv2D.
	int intersectionWithEnvelope2D(Envelope2D clipEnv2D,
			boolean includeEnvBoundary, double[] segParams,
			double[] envelopeDistances) {
		Point2D p1 = getStartXY();
		Point2D p2 = getEndXY();

		// includeEnvBoundary xxx ???

		int modified = clipEnv2D.clipLine(p1, p2, 0, segParams,
				envelopeDistances);
		return modified != 0 ? 2 : 0;

	}

	@Override
	double intersectionOfYMonotonicWithAxisX(double y, double x_parallel) {
		double a = (m_yEnd - m_yStart);

		if (a == 0)
			return (y == m_yEnd) ? x_parallel : NumberUtils.NaN();

		double t = (y - m_yStart) / a;
		assert (t >= 0 && t <= 1.0);
		// double t_1 = 1.0 - t;
		// assert(t + t_1 == 1.0);
		double resx = getCoordX_(t);
		if (t == 1.0)
			resx = m_xEnd;
		assert ((resx >= m_xStart && resx <= m_xEnd) || (resx <= m_xStart && resx >= m_xEnd));
		return resx;
	}

	@Override
	boolean _isIntersectingPoint(Point2D pt, double tolerance,
			boolean bExcludeExactEndpoints) {
		return _intersection(pt, tolerance, bExcludeExactEndpoints) >= 0;// must
																			// use
																			// same
																			// method
																			// that
																			// the
																			// intersection
																			// routine
																			// uses.
	}

	/**
	 * Returns True if point and the segment intersect (not disjoint) for the
	 * given tolerance.
	 */
	@Override
	public boolean isIntersecting(Point2D pt, double tolerance) {
		return _isIntersectingPoint(pt, tolerance, false);
	}

	void orientBottomUp_() {
		if (m_yEnd < m_yStart || (m_yEnd == m_yStart && m_xEnd < m_xStart)) {
			double x = m_xStart;
			m_xStart = m_xEnd;
			m_xEnd = x;

			double y = m_yStart;
			m_yStart = m_yEnd;
			m_yEnd = y;
			for (int i = 0, n = m_description.getTotalComponentCount() - 2; i < n; i++) {
				double a = m_attributes[i];
				m_attributes[i] = m_attributes[i + n];
				m_attributes[i + n] = a;
			}
		}
	}

	// return -1 for the left side from the infinite line passing through thais
	// Line, 1 for the right side of the line, 0 if on the line (in the bounds
	// of the roundoff error)
	int _side(Point2D pt) {
		return _side(pt.x, pt.y);
	}

	// return -1 for the left side from the infinite line passing through thais
	// Line, 1 for the right side of the line, 0 if on the line (in the bounds
	// of the roundoff error)
	int _side(double ptX, double ptY) {
		Point2D v1 = new Point2D(ptX, ptY);
		v1.sub(getStartXY());
		Point2D v2 = new Point2D();
		v2.sub(getEndXY(), getStartXY());
		double cross = v2.crossProduct(v1);
		double crossError = 4 * NumberUtils.doubleEps()
				* (Math.abs(v2.x * v1.y) + Math.abs(v2.y * v1.x));
		return cross > crossError ? -1 : cross < -crossError ? 1 : 0;
	}

	double _intersection(Point2D pt, double tolerance,
			boolean bExcludeExactEndPoints) {
		Point2D v = new Point2D();
		Point2D start = new Point2D();

		// Test start point distance to pt.
		start.setCoords(m_xStart, m_yStart);
		v.sub(pt, start);
		double vlength = v.length();
		double vLengthError = vlength * 3 * NumberUtils.doubleEps();
		if (vlength <= Math.max(tolerance, vLengthError)) {
			assert (vlength != 0 || pt.isEqual(start));// probably never asserts
			if (bExcludeExactEndPoints && vlength == 0)
				return NumberUtils.TheNaN;
			else
				return 0;
		}

		Point2D end2D = getEndXY();
		// Test end point distance to pt.
		v.sub(pt, end2D);
		vlength = v.length();
		vLengthError = vlength * 3 * NumberUtils.doubleEps();
		if (vlength <= Math.max(tolerance, vLengthError)) {
			assert (vlength != 0 || pt.isEqual(end2D));// probably never asserts
			if (bExcludeExactEndPoints && vlength == 0)
				return NumberUtils.TheNaN;
			else
				return 1.0;
		}

		// Find a distance from the line to pt.
		v.setCoords(m_xEnd - m_xStart, m_yEnd - m_yStart);
		double len = v.length();
		if (len > 0) {
			double invertedLength = 1.0 / len;
			v.scale(invertedLength);
			Point2D relativePoint = new Point2D();
			relativePoint.sub(pt, start);
			double projection = relativePoint.dotProduct(v);
			double projectionError = 8 * relativePoint._dotProductAbs(v)
					* NumberUtils.doubleEps();// See Error Estimation Rules In
												// Borg.docx
			v.leftPerpendicular();// get left normal to v
			double distance = relativePoint.dotProduct(v);
			double distanceError = 8 * relativePoint._dotProductAbs(v)
					* NumberUtils.doubleEps();// See Error Estimation Rules In
												// Borg.docx

			double perror = Math.max(tolerance, projectionError);
			if (projection < -perror || projection > len + perror)
				return NumberUtils.TheNaN;

			double merror = Math.max(tolerance, distanceError);
			if (Math.abs(distance) <= merror) {
				double t = projection * invertedLength;
				t = NumberUtils.snap(t, 0.0, 1.0);
				Point2D ptOnLine = new Point2D();
				getCoord2D(t, ptOnLine);
				if (Point2D.distance(ptOnLine, pt) <= tolerance) {
					if (t < 0.5) {
						if (Point2D.distance(ptOnLine, start) <= tolerance)// the
																			// projected
																			// point
																			// is
																			// close
																			// to
																			// the
																			// start
																			// point.
																			// Need
																			// to
																			// return
																			// 0.
							return 0;
					} else if (Point2D.distance(ptOnLine, end2D) <= tolerance)// the
																				// projected
																				// point
																				// is
																				// close
																				// to
																				// the
																				// end
																				// point.
																				// Need
																				// to
																				// return
																				// 1.0.
						return 1.0;

					return t;
				}
			}
		}

		return NumberUtils.TheNaN;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		if (other == this)
			return true;

		if (other.getClass() != getClass())
			return false;

		return _equalsImpl((Segment)other);
	}
	
	boolean equals(Line other) {
		if (other == this)
			return true;

		if (!(other instanceof Line))
			return false;

		return _equalsImpl((Segment) other);
	}

	boolean _projectionIntersectHelper(Line other, Point2D v, boolean bStart) {
		// v is the vector in the direction of this line == end - start.
		double orgX = bStart ? m_xStart : m_xEnd;
		double orgY = bStart ? m_yStart : m_yEnd;
		Point2D m = new Point2D();
		m.x = other.getEndX() - orgX;
		m.y = other.getEndY() - orgY;
		double dot = v.dotProduct(m);
		double dotError = 3 * NumberUtils.doubleEps() * v._dotProductAbs(m);
		if (dot > dotError) {
			m.x = other.getStartX() - orgX;
			m.y = other.getStartY() - orgY;
			double dot2 = v.dotProduct(m);
			double dotError2 = 3 * NumberUtils.doubleEps()
					* v._dotProductAbs(m);
			return dot2 <= dotError2;
		}

		return true;
	}

	boolean _projectionIntersect(Line other) {
		// This function returns true, if the "other"'s projection on "this"
		Point2D v = new Point2D();
		v.x = m_xEnd - m_xStart;
		v.y = m_yEnd - m_yStart;
		if (!_projectionIntersectHelper(other, v, false))
			return false; // Both other.Start and other.End projections on
							// "this" lie to the right of the this.End

		v.negate();
		if (!_projectionIntersectHelper(other, v, true))
			return false; // Both other.Start and other.End projections on
							// "this" lie to the left of the this.End

		return true;
	}

	// Tests if two lines intersect using projection of one line to another.
	static boolean _isIntersectingHelper(Line line1, Line line2) {
		int s11 = line1._side(line2.m_xStart, line2.m_yStart);
		int s12 = line1._side(line2.m_xEnd, line2.m_yEnd);
		if (s11 < 0 && s12 < 0 || s11 > 0 && s12 > 0)
			return false;// no intersection. The line2 lies to one side of an
							// infinite line passing through line1

		int s21 = line2._side(line1.m_xStart, line1.m_yStart);
		int s22 = line2._side(line1.m_xEnd, line1.m_yEnd);
		if (s21 < 0 && s22 < 0 || s21 > 0 && s22 > 0)
			return false;// no intersection.The line1 lies to one side of an
							// infinite line passing through line2

		double len1 = line1.calculateLength2D();
		double len2 = line2.calculateLength2D();
		if (len1 > len2) {
			return line1._projectionIntersect(line2);
		} else {
			return line2._projectionIntersect(line1);
		}
	}

	static Point2D _intersectHelper1(Line line1, Line line2, double tolerance) {
		Point2D result = new Point2D(NumberUtils.NaN(), NumberUtils.NaN());
		double k1x = line1.m_xEnd - line1.m_xStart;
		double k1y = line1.m_yEnd - line1.m_yStart;
		double k2x = line2.m_xEnd - line2.m_xStart;
		double k2y = line2.m_yEnd - line2.m_yStart;

		double det = k2x * k1y - k1x * k2y;
		if (det == 0)
			return result;

		// estimate roundoff error for det:
		double errdet = 4 * NumberUtils.doubleEps()
				* (Math.abs(k2x * k1y) + Math.abs(k1x * k2y));

		double bx = line2.m_xStart - line1.m_xStart;
		double by = line2.m_yStart - line1.m_yStart;

		double a0 = (k2x * by - bx * k2y);
		double a0error = 4 * NumberUtils.doubleEps()
				* (Math.abs(k2x * by) + Math.abs(bx * k2y));
		double t0 = a0 / det;
		double absdet = Math.abs(det);
		double t0error = (a0error * absdet + errdet * Math.abs(a0))
				/ (det * det) + NumberUtils.doubleEps() * Math.abs(t0);
		if (t0 < -t0error || t0 > 1.0 + t0error)
			return result;

		double a1 = (k1x * by - bx * k1y);
		double a1error = 4 * NumberUtils.doubleEps()
				* (Math.abs(k1x * by) + Math.abs(bx * k1y));
		double t1 = a1 / det;
		double t1error = (a1error * absdet + errdet * Math.abs(a1))
				/ (det * det) + NumberUtils.doubleEps() * Math.abs(t1);

		if (t1 < -t1error || t1 > 1.0 + t1error)
			return result;

		double t0r = NumberUtils.snap(t0, 0.0, 1.0);
		double t1r = NumberUtils.snap(t1, 0.0, 1.0);
		Point2D pt0 = line1.getCoord2D(t0r);
		Point2D pt1 = line2.getCoord2D(t1r);
		Point2D pt = new Point2D();
		pt.sub(pt0, pt1);
		if (pt.length() > tolerance) {
			// Roundoff errors cause imprecise result. Try recalculate.
			// 1. Use averaged point and recalculate the t values
			// Point2D pt;
			pt.add(pt0, pt1);
			pt.scale(0.5);
			t0r = line1.getClosestCoordinate(pt, false);
			t1r = line2.getClosestCoordinate(pt, false);
			Point2D pt01 = line1.getCoord2D(t0r);
			Point2D pt11 = line2.getCoord2D(t1r);
			pt01.sub(pt11);
			if (pt01.length() > tolerance) {
				// Seems to be no intersection here actually. Return NaNs
				return result;
			}
		}

		result.setCoords(t0r, t1r);
		return result;
	}

	static int _isIntersectingLineLine(Line line1, Line line2,
			double tolerance, boolean bExcludeExactEndpoints) {
		// _ASSERT(line1 != line2);
		// Check for the endpoints.
		// The bExcludeExactEndpoints is True, means we care only about overlaps
		// and real intersections, but do not care if the endpoints are exactly
		// equal.
		// bExcludeExactEndpoints is used in Cracking check test, because during
		// cracking test all points are either coincident or further than the
		// tolerance.
		int counter = 0;
		if (line1.m_xStart == line2.m_xStart
				&& line1.m_yStart == line2.m_yStart
				|| line1.m_xStart == line2.m_xEnd
				&& line1.m_yStart == line2.m_yEnd) {
			counter++;
			if (!bExcludeExactEndpoints)
				return 1;
		}

		if (line1.m_xEnd == line2.m_xStart && line1.m_yEnd == line2.m_yStart
				|| line1.m_xEnd == line2.m_xEnd && line1.m_yEnd == line2.m_yEnd) {
			counter++;
			if (counter == 2)
				return 2; // counter == 2 means both endpoints coincide (Lines
							// overlap).
			if (!bExcludeExactEndpoints)
				return 1;
		}

		if (line2._isIntersectingPoint(line1.getStartXY(), tolerance, true))
			return 1;// return true;
		if (line2._isIntersectingPoint(line1.getEndXY(), tolerance, true))
			return 1;// return true;
		if (line1._isIntersectingPoint(line2.getStartXY(), tolerance, true))
			return 1;// return true;
		if (line1._isIntersectingPoint(line2.getEndXY(), tolerance, true))
			return 1;// return true;

		if (bExcludeExactEndpoints && (counter != 0))
			return 0;// return false;

		return _isIntersectingHelper(line1, line2) == false ? 0 : 1;
	}

	int _intersectLineLineExact(Line line1, Line line2,
			Point2D[] intersectionPoints, double[] param1, double[] param2) {
		int counter = 0;
		if (line1.m_xStart == line2.m_xStart
				&& line1.m_yStart == line2.m_yStart) {
			if (param1 != null)// if (param1)
				param1[counter] = 0.0;
			if (param2 != null)// if (param2)
				param2[counter] = 0.0;

			if (intersectionPoints != null)// if (intersectionPoints)
				intersectionPoints[counter] = Point2D.construct(line1.m_xStart,
						line1.m_yStart);

			counter++;
		}

		if (line1.m_xStart == line2.m_xEnd && line1.m_yStart == line2.m_yEnd) {
			if (param1 != null)// if (param1)
				param1[counter] = 0.0;
			if (param2 != null)// if (param2)
				param2[counter] = 1.0;

			if (intersectionPoints != null)// if (intersectionPoints)
				intersectionPoints[counter] = Point2D.construct(line1.m_xStart,
						line1.m_yStart);

			counter++;
		}

		if (line1.m_xEnd == line2.m_xStart && line1.m_yEnd == line2.m_yStart) {
			if (counter == 2) {// both segments a degenerate
				if (param1 != null)// if (param1)
				{
					param1[0] = 0.0;
					param1[1] = 1.0;
				}
				if (param2 != null)// if (param2)
				{
					param2[0] = 1.0;
				}

				if (intersectionPoints != null)// if (intersectionPoints)
				{
					intersectionPoints[0] = Point2D.construct(line1.m_xEnd,
							line1.m_yEnd);
					intersectionPoints[1] = Point2D.construct(line1.m_xEnd,
							line1.m_yEnd);
				}

				return counter;
			}

			if (param1 != null)// if (param1)
				param1[counter] = 1.0;
			if (param2 != null)// if (param2)
				param2[counter] = 0.0;

			if (intersectionPoints != null)// if (intersectionPoints)
				intersectionPoints[counter] = Point2D.construct(line1.m_xEnd,
						line1.m_yEnd);

			counter++;
		}

		if (line1.m_xEnd == line2.m_xEnd && line1.m_yEnd == line2.m_yEnd) {
			if (counter == 2) {// both segments are degenerate
				if (param1 != null)// if (param1)
				{
					param1[0] = 0.0;
					param1[1] = 1.0;
				}
				if (param2 != null)// if (param2)
				{
					param2[0] = 1.0;
				}

				if (intersectionPoints != null)// if (intersectionPoints)
				{
					intersectionPoints[0] = Point2D.construct(line1.m_xEnd,
							line1.m_yEnd);
					intersectionPoints[1] = Point2D.construct(line1.m_xEnd,
							line1.m_yEnd);
				}

				return counter;
			}

			if (param1 != null)// if (param1)
				param1[counter] = 1.0;
			if (param2 != null)// if (param2)
				param2[counter] = 1.0;

			if (intersectionPoints != null)// if (intersectionPoints)
				intersectionPoints[counter] = Point2D.construct(line1.m_xEnd,
						line1.m_yEnd);
			counter++;
		}

		return counter;
	}

	static int _intersectLineLine(Line line1, Line line2,
			Point2D[] intersectionPoints, double[] param1, double[] param2,
			double tolerance) {
		// _ASSERT(!param1 && !param2 || param1);
		int counter = 0;
		// Test the end points for exact coincidence.
		double t11 = line1._intersection(line2.getStartXY(), tolerance, false);
		double t12 = line1._intersection(line2.getEndXY(), tolerance, false);
		double t21 = line2._intersection(line1.getStartXY(), tolerance, false);
		double t22 = line2._intersection(line1.getEndXY(), tolerance, false);

		if (!NumberUtils.isNaN(t11)) {
			if (param1 != null)// if (param1)
				param1[counter] = t11;
			if (param2 != null)// if (param2)
				param2[counter] = 0;

			if (intersectionPoints != null)// if (intersectionPoints)
				intersectionPoints[counter] = Point2D.construct(line2.m_xStart,
						line2.m_yStart);
			counter++;
		}

		if (!NumberUtils.isNaN(t12)) {
			if (param1 != null)// if (param1)
				param1[counter] = t12;
			if (param2 != null)// if (param2)
				param2[counter] = 1.0;

			if (intersectionPoints != null)// if (intersectionPoints)
				intersectionPoints[counter] = Point2D.construct(line2.m_xEnd,
						line2.m_yEnd);
			counter++;
		}

		if (counter != 2 && !NumberUtils.isNaN(t21)) {
			if (!(t11 == 0 && t21 == 0) && !(t12 == 0 && t21 == 1.0))// the "if"
																		// makes
																		// sure
																		// this
																		// has
																		// not
																		// been
																		// already
																		// calculated
			{
				if (param1 != null)// if (param1)
					param1[counter] = 0;
				if (param2 != null)// if (param2)
					param2[counter] = t21;

				if (intersectionPoints != null)// if (intersectionPoints)
					intersectionPoints[counter] = Point2D.construct(
							line1.m_xStart, line1.m_yStart);
				counter++;
			}
		}

		if (counter != 2 && !NumberUtils.isNaN(t22)) {
			if (!(t11 == 1.0 && t22 == 0) && !(t12 == 1.0 && t22 == 1.0))// the
																			// "if"
																			// makes
																			// sure
																			// this
																			// has
																			// not
																			// been
																			// already
																			// calculated
			{
				if (param1 != null)// if (param1)
					param1[counter] = 1.0;
				if (param2 != null)// if (param2)
					param2[counter] = t22;

				if (intersectionPoints != null)// if (intersectionPoints)
					intersectionPoints[counter] = Point2D.construct(
							line2.m_xEnd, line2.m_yEnd);
				counter++;
			}
		}

		if (counter > 0) {
			if (counter == 2 && param1 != null && param1[0] > param1[1]) {// make
																			// sure
																			// the
																			// intersection
																			// events
																			// are
																			// sorted
																			// along
																			// the
																			// line1
																			// can't
																			// swap
																			// doulbes
																			// in
																			// java
																			// NumberUtils::Swap(param1[0],
																			// param1[1]);
				double zeroParam1 = param1[0];
				param1[0] = param1[1];
				param1[1] = zeroParam1;

				if (param2 != null)// if (param2)
				{
					double zeroParam2 = param2[0];
					param2[0] = param2[1];
					param2[1] = zeroParam2;// NumberUtils::Swap(ARRAYELEMENT(param2,
											// 0), ARRAYELEMENT(param2, 1));
				}

				if (intersectionPoints != null)// if (intersectionPoints)
				{
					Point2D tmp = new Point2D(intersectionPoints[0].x,
							intersectionPoints[0].y);
					intersectionPoints[0] = intersectionPoints[1];
					intersectionPoints[1] = tmp;
				}
			}

			return counter;
		}

		Point2D params = _intersectHelper1(line1, line2, tolerance);
		if (NumberUtils.isNaN(params.x))
			return 0;

		if (intersectionPoints != null)// if (intersectionPoints)
		{
			intersectionPoints[0] = line1.getCoord2D(params.x);
		}

		if (param1 != null)// if (param1)
		{
			param1[0] = params.x;
		}

		if (param2 != null)// if (param2)
		{
			param2[0] = params.y;
		}

		return 1;
	}
	
    @Override
    public void replaceNaNs(int semantics, double value) {
    	addAttribute(semantics);
    	if (isEmpty())
    		return;
    	
    	int ncomps = VertexDescription.getComponentCount(semantics);
    	for (int i = 0; i < ncomps; i++) {
    		double v = _getAttributeAsDbl(0, semantics, i);
    		if (Double.isNaN(v))
    			_setAttribute(0, semantics, 0, value);
    		
    		v = _getAttributeAsDbl(1, semantics, i);
    		if (Double.isNaN(v))
    			_setAttribute(1, semantics, 0, value);
    	}
    }
	

	@Override
	int getYMonotonicParts(SegmentBuffer[] monotonicSegments) {
		return 0;
	}

	@Override
	void _copyToImpl(Segment dst) {
		// TODO Auto-generated method stub

	}

	/**
	 * The output of this method can be only used for debugging. It is subject to change without notice. 
	 */
	@Override
	public String toString() {
		String s = "Line: [" + m_xStart + ", " + m_yStart + ", " + m_xEnd + ", " + m_yEnd +"]"; 
		return s;
	}
	
}
