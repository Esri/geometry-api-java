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

import java.util.ArrayList;

// Note: m_distance<0 offsets to the left, m_distance>0 offsets to the right

class ConstructOffset {
	ProgressTracker m_progressTracker;
	Geometry m_inputGeometry;
	double m_distance;
	double m_tolerance;
	OperatorOffset.JoinType m_joins;
	double m_miterLimit;

	// multipath offset
	static class GraphicPoint {
		double x, y;
		int m_next, m_prev;
		double m;
		int type;

		GraphicPoint(double x_, double y_) {
			x = x_;
			y = y_;
			type = 0;
			m = 0;
		}

		GraphicPoint(Point2D r) {
			x = r.x;
			y = r.y;
			type = 0;
			m = 0;
		}

		GraphicPoint(GraphicPoint pt) {
			x = pt.x;
			y = pt.y;
			type = pt.type;
			m = pt.m;
		}

		GraphicPoint(GraphicPoint srcPt, double d, double angle) {
			x = srcPt.x + d * Math.cos(angle);
			y = srcPt.y + d * Math.sin(angle);
			type = srcPt.type;
			m = srcPt.m;
		}

		GraphicPoint(GraphicPoint pt1, GraphicPoint pt2) {
			x = (pt1.x + pt2.x) * 0.5;
			y = (pt1.y + pt2.y) * 0.5;
			type = pt1.type;
			m = pt1.m;
		}

		GraphicPoint(GraphicPoint pt1, GraphicPoint pt2, double ratio) {
			x = pt1.x + (pt2.x - pt1.x) * ratio;
			y = pt1.y + (pt2.y - pt1.y) * ratio;
			type = pt1.type;
			m = pt1.m;
		}

	};

	static class GraphicRect {
		double x1, x2, y1, y2;
	};

	static class IntersectionInfo {
		GraphicPoint pt;
		double rFirst;
		double rSecond;
		boolean atExistingPt;
	};

	ArrayList<GraphicPoint> m_srcPts;
	int m_srcPtCount;
	ArrayList<GraphicPoint> m_offsetPts;
	int m_offsetPtCount;

	MultiPath m_resultPath;
	int m_resultPoints;
	double m_a1, m_a2;
	boolean m_bBadSegs;

	ConstructOffset(ProgressTracker progressTracker) {
		m_progressTracker = progressTracker;
	}

	// static
	static Geometry execute(Geometry inputGeometry, double distance,
			OperatorOffset.JoinType joins, double miterLimit, double tolerance,
			ProgressTracker progressTracker) {
		if (inputGeometry == null)
			throw new IllegalArgumentException();
		if (inputGeometry.getDimension() < 1)// can offset Polygons and
												// Polylines only
			throw new IllegalArgumentException();
		if (distance == 0 || inputGeometry.isEmpty())
			return inputGeometry;
		ConstructOffset offset = new ConstructOffset(progressTracker);
		offset.m_inputGeometry = inputGeometry;
		offset.m_distance = distance;
		offset.m_tolerance = tolerance;
		offset.m_joins = joins;
		offset.m_miterLimit = miterLimit;
		return offset._ConstructOffset();
	}

	Geometry _OffsetLine() {
		Line line = (Line) m_inputGeometry;
		Point2D start = line.getStartXY();
		Point2D end = line.getEndXY();
		Point2D v = new Point2D();
		v.sub(end, start);
		v.normalize();
		v.leftPerpendicular();
		v.scale(m_distance);
		start.add(v);
		end.add(v);
		Line resLine = (Line) line.createInstance();
		line.setStartXY(start);
		line.setEndXY(end);
		return resLine;
	}

	Geometry _OffsetEnvelope() {
		Envelope envelope = (Envelope) m_inputGeometry;
		if ((m_distance > 0) && (m_joins != OperatorOffset.JoinType.Miter)) {
			Polygon poly = new Polygon();
			poly.addEnvelope(envelope, false);
			m_inputGeometry = poly;
			return _ConstructOffset();
		}

		Envelope resEnv = new Envelope(envelope.m_envelope);
		resEnv.inflate(m_distance, m_distance);
		return resEnv;
	}

	private final double pi = Math.PI;// GEOMETRYX_PI;
	private final double two_pi = Math.PI * 2;// GEOMETRYX_2PI;
	private final double half_pi = Math.PI / 2;// GEOMETRYX_HalfPI;
	private final double sqrt2 = 1.4142135623730950488016887242097;
	private final double oneDegree = 0.01745329251994329576923690768489;

	private final int BAD_SEG = 0x0100;
	private final int IS_END = 0x0200;
	private final int CLOSING_SEG = 0x0400;

	void addPoint(GraphicPoint pt) {
		m_offsetPts.add(pt);
		m_offsetPtCount++;
	}

	double scal(GraphicPoint pt1, GraphicPoint pt2, GraphicPoint pt3,
			GraphicPoint pt4) {
		return (pt2.x - pt1.x) * (pt4.x - pt3.x) + (pt2.y - pt1.y)
				* (pt4.y - pt3.y);
	}

	// offPt is the point to add.
	// this point corresponds to the offset version of the end of seg1.
	// it could generate a segment going in the opposite direction of the
	// original segment
	// this situation is handled here by adding an additional "bad" segment
	void addPoint(GraphicPoint offPt, int i_src) {
		if (m_offsetPtCount == 0) // TODO: can we have this outside of this
									// method?
		{
			addPoint(offPt);
			return;
		}

		int n_src = m_srcPtCount;
		GraphicPoint pt1, pt;
		pt1 = m_srcPts.get(i_src == 0 ? n_src - 1 : i_src - 1);
		pt = m_srcPts.get(i_src);

		// calculate scalar product to determine if the offset segment goes in
		// the same/opposite direction compared to the original one
		double s = scal(pt1, pt, m_offsetPts.get(m_offsetPtCount - 1), offPt);
		if (s > 0)
		// original segment and offset segment go in the same direction. Just
		// add the point
		{
			addPoint(offPt);
			return;
		}

		if (s < 0) {
			// we will add a loop. We need to make sure the points we introduce
			// don't generate a "reversed" segment
			// let's project the first point of the reversed segment
			// (m_offsetPts + m_offsetPtCount - 1) to check
			// if it falls on the good side of the original segment (scalar
			// product sign again)
			if (scal(pt1, pt, pt, m_offsetPts.get(m_offsetPtCount - 1)) > 0) {
				GraphicPoint p;

				// change value of m_offsetPts + m_offsetPtCount - 1
				int k;
				if (i_src == 0)
					k = n_src - 2;
				else if (i_src == 1)
					k = n_src - 1;
				else
					k = i_src - 2;
				GraphicPoint pt0 = m_srcPts.get(k);

				double a = Math.atan2(pt1.y - pt0.y, pt1.x - pt0.x);
				p = new GraphicPoint(pt1, m_distance, a - half_pi);
				m_offsetPts.set(m_offsetPtCount - 1, p);

				if (m_joins == OperatorOffset.JoinType.Bevel
						|| m_joins == OperatorOffset.JoinType.Miter) {
					// this block is added as well as the commented BAD_SEG in
					// the next block
					p = new GraphicPoint(p, pt1);
					addPoint(p);

					// "bad" segment
					p = new GraphicPoint(pt1, m_distance, m_a1 + half_pi);

					GraphicPoint p_ = new GraphicPoint(p, pt1);
					p_.type |= BAD_SEG;
					addPoint(p_);

					addPoint(p);
				} else {
					// the working stuff for round and square

					// "bad" segment
					p = new GraphicPoint(pt1, m_distance, m_a1 + half_pi);
					p.type |= BAD_SEG;
					addPoint(p);
				}

				// add offPt
				addPoint(offPt, i_src);
			} else {
				GraphicPoint p;

				// we don't add offPt but the loop containing the "bad" segment
				p = new GraphicPoint(pt, m_distance, m_a1 + half_pi);
				addPoint(p);

				if (m_joins == OperatorOffset.JoinType.Bevel
						|| m_joins == OperatorOffset.JoinType.Miter) {
					// this block is added as well as the commented BAD_SEG in
					// the next block
					p = new GraphicPoint(p, pt);
					addPoint(p);

					p = new GraphicPoint(pt, m_distance, m_a2 - half_pi);
					GraphicPoint p_ = new GraphicPoint(p, pt);
					p_.type |= BAD_SEG;
					addPoint(p_);

					addPoint(p);
				} else {
					// the working stuff for round and square
					p = new GraphicPoint(pt, m_distance, m_a2 - half_pi);
					p.type |= BAD_SEG;
					addPoint(p);
				}
			}
		}
	}

	boolean buildOffset() {
		// make sure we have at least three points and no identical points
		int i;
		double a1, a2;
		GraphicPoint pt, pt1, pt2;
		GraphicPoint p;

		// number of points to deal with
		int n = m_srcPtCount;

		m_offsetPtCount = 0;

		double flattenTolerance = m_tolerance * 0.5;

		double a1_0 = 0;
		double a2_0 = 0;
		for (i = 0; i < n; i++) {
			pt = m_srcPts.get(i);

			// point before
			if (i == 0)
				pt1 = m_srcPts.get(n - 1);
			else
				pt1 = m_srcPts.get(i - 1);

			// point after
			if (i == n - 1)
				pt2 = m_srcPts.get(0);
			else
				pt2 = m_srcPts.get(i + 1);

			// angles of enclosing segments
			double dx1 = pt1.x - pt.x;
			double dy1 = pt1.y - pt.y;
			double dx2 = pt2.x - pt.x;
			double dy2 = pt2.y - pt.y;
			a1 = Math.atan2(dy1, dx1);
			a2 = Math.atan2(dy2, dx2);
			m_a1 = a1;
			m_a2 = a2;
			if (i == 0) {
				a1_0 = a1;
				a2_0 = a2;
			}

			// double dot_product = dx1 * dx2 + dy1 * dy2;
			double cross_product = dx1 * dy2 - dx2 * dy1;
			// boolean bInnerAngle = (cross_product == 0) ? (m_distance > 0) :
			// (cross_product * m_distance >= 0.0);

			// check for inner angles (always managed the same, whatever the
			// type of join)
			double saved_a2 = a2;
			if (a2 < a1)
				a2 += two_pi; // this guaranties that (a1 + a2) / 2 is on the
								// right side of the curve
			if (cross_product * m_distance > 0.0) // inner angle
			{
				// inner angle
				if (m_joins == OperatorOffset.JoinType.Bevel
						|| m_joins == OperatorOffset.JoinType.Miter) {
					p = new GraphicPoint(pt, m_distance, a1 + half_pi);
					addPoint(p);

					// this block is added as well as the commented BAD_SEG in
					// the next block
					double ratio = 0.001; // TODO: the higher the ratio, the
											// better the result (shorter
											// segments)
					p = new GraphicPoint(pt, p, ratio);
					addPoint(p);

					// this is the "bad" segment
					p = new GraphicPoint(pt, m_distance, a2 - half_pi);

					GraphicPoint p_ = new GraphicPoint(pt, p, ratio);
					p_.type |= BAD_SEG;
					addPoint(p_);

					addPoint(p);
				} else {
					// this method works for square and round, but not bevel
					double r = (a2 - a1) * 0.5;
					double d = m_distance / Math.abs(Math.sin(r));
					p = new GraphicPoint(pt, d, (a1 + a2) * 0.5);
					addPoint(p, i); // will deal with reversed segments
				}
				continue;
			}

			// outer angles
			// check if we have an end point first
			if ((pt.type & IS_END) != 0) {
				// TODO: deal with other options. assume rounded and
				// perpendicular for now
				// we need to use the outer regular polygon of the round join
				// TODO: explain this in a doc

				// calculate the number of points based on a flatten tolerance
				double r = 1.0 - flattenTolerance / Math.abs(m_distance);
				long na = 1;
				double da = (m_distance < 0) ? -pi : pi; // da is negative when
															// m_offset is
															// negative (???)
				if (r > -1.0 && r < 1.0) {
					double a = Math.acos(r) * 2; // angle where "arrow?" is less
													// than flattenTolerance
					// do not consider an angle smaller than a degree
					if (a < oneDegree)
						a = oneDegree;
					na = (long) (pi / a + 1.5);
					if (na > 1)
						da /= na;
				}
				// add first point
				double a = a1 + half_pi;
				p = new GraphicPoint(pt, m_distance, a);
				if (i == 0)
					p.type |= CLOSING_SEG; // TODO: should we simplify this by
											// considering the last point
											// instead of the first one??
				addPoint(p, i); // will deal with reversed segments

				double d = m_distance / Math.cos(da / 2);
				a += da / 2;
				p = new GraphicPoint(pt, d, a);
				p.type |= CLOSING_SEG;
				addPoint(p);

				while (--na > 0) {
					a += da;
					p = new GraphicPoint(pt, d, a);
					p.type |= CLOSING_SEG;
					addPoint(p);
				}

				// last point (optional except for the first point)
				p = new GraphicPoint(pt, m_distance, a2 - half_pi); // this one
																	// is
																	// optional
																	// except
																	// for the
																	// first
																	// point
				p.type |= CLOSING_SEG;
				addPoint(p);

				continue;
			}

			else if (m_joins == OperatorOffset.JoinType.Bevel) // bevel
			{
				p = new GraphicPoint(pt, m_distance, a1 + half_pi);
				addPoint(p, i); // will deal with reversed segments
				p = new GraphicPoint(pt, m_distance, a2 - half_pi);
				addPoint(p);
				continue;
			}

			else if (m_joins == OperatorOffset.JoinType.Round) {
				// we need to use the outer regular polygon of the round join
				// TODO: explain this in a doc

				// calculate the number of points based on a flatten tolerance
				double r = 1.0 - flattenTolerance / Math.abs(m_distance);
				long na = 1;
				double da = (a2 - half_pi) - (a1 + half_pi); // da is negative
																// when
																// m_distance is
																// negative
				if (r > -1.0 && r < 1.0) {
					double a = Math.acos(r) * 2.0; // angle where "arrow?" is
													// less than
													// flattenTolerance
					// do not consider an angle smaller than a degree
					if (a < oneDegree)
						a = oneDegree;
					na = (long) (Math.abs(da) / a + 1.5);
					if (na > 1)
						da /= na;
				}
				double d = m_distance / Math.cos(da * 0.5);
				double a = a1 + half_pi + da * 0.5;
				p = new GraphicPoint(pt, d, a);
				addPoint(p, i); // will deal with reversed segments
				while (--na > 0) {
					a += da;
					p = new GraphicPoint(pt, d, a);
					addPoint(p);
				}
				continue;
			} else if (m_joins == OperatorOffset.JoinType.Miter) {
				dx1 = pt1.x - pt.x;
				dy1 = pt1.y - pt.y;
				dx2 = pt2.x - pt.x;
				dy2 = pt2.y - pt.y;
				double d1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
				double d2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
				double cosa = (dx1 * dx2 + dy1 * dy2) / d1 / d2;
				if (cosa > 1.0 - 1.0e-8) {
					// there's a spike in the polygon boundary; this could
					// happen when filtering out short segments in Init()
					p = new GraphicPoint(pt, sqrt2 * m_distance, a2 - pi * 0.25);
					addPoint(p, i);
					p = new GraphicPoint(pt, sqrt2 * m_distance, a2 + pi * 0.25);
					addPoint(p);
					continue;
				}
				// original miter code
				// if (m_miterLimit * m_miterLimit * (1 - cosa) < 2)
				// {
				// // bevel join
				// p = new GraphicPoint(pt, m_distance, a1 + half_pi);
				// AddPoint(p, src_poly, srcPtCount, i); // will deal with
				// reversed segments
				// p = new GraphicPoint(pt, m_distance, a2 - half_pi);
				// AddPoint(p);
				// continue;
				// }
				double distanceFromCorner = Math.abs(m_distance
						/ Math.sin(Math.acos(cosa) * 0.5));
				double bevelDistance = Math.abs(m_miterLimit * m_distance);
				if (distanceFromCorner > bevelDistance) {
					double r = (a2 - a1) * 0.5;
					double d = m_distance / Math.abs(Math.sin(r));
					p = new GraphicPoint(pt, d, (a1 + a2) * 0.5);

					// construct bevel points, see comment in
					// c:\ArcGIS\System\Geometry\Geometry\ConstructCurveImpl.cpp,
					// ESRI::OffsetCurve::EstimateBevelPoints
					Point2D corner = new Point2D(p.x, p.y);
					Point2D through = new Point2D(pt.x, pt.y);
					Point2D delta = new Point2D();
					delta.sub(corner, through);

					// Point2D midPoint = through + delta * (bevelDistance /
					// delta.Length());
					Point2D midPoint = new Point2D();
					midPoint.scaleAdd(bevelDistance / delta.length(), delta,
							through);

					double sideLength = Math.sqrt(distanceFromCorner
							* distanceFromCorner - m_distance * m_distance), halfWidth = (distanceFromCorner - bevelDistance)
							* Math.abs(m_distance) / sideLength;

					// delta = delta.RotateDirect(0.0, m_distance > 0.0 ? -1.0 :
					// 1.0) * (halfWidth/delta.Length());
					if (m_distance > 0.0)
						delta.leftPerpendicular();
					else
						delta.rightPerpendicular();
					delta.scale(halfWidth / delta.length());

					Point2D from = new Point2D();
					from.add(midPoint, delta);
					Point2D to = new Point2D();
					to.sub(midPoint, delta);
					p = new GraphicPoint(from);
					// _ASSERT(::_finite(p.x));
					// _ASSERT(::_finite(p.y));
					addPoint(p, i);
					p = new GraphicPoint(to);
					// _ASSERT(::_finite(p.x));
					// _ASSERT(::_finite(p.y));
					addPoint(p);
					continue;
				}
				// miter join
				double r = (a2 - a1) * 0.5;
				double d = m_distance / Math.abs(Math.sin(r)); // r should not
																// be null
																// (trapped by
																// the bevel
																// case)
				p = new GraphicPoint(pt, d, (a1 + a2) * 0.5);
				addPoint(p, i); // will deal with reversed segments
				continue;
			} else // the new "square" join
			{
				a2 = saved_a2;

				// identify if angle is less than pi/2
				// in this case, we introduce a segment that is perpendicular to
				// the bissector of the angle
				// TODO: see figure X for details
				boolean bAddSegment;
				if (m_distance > 0.0) {
					if (a2 > a1) // > and not >=
						a2 -= two_pi;
					bAddSegment = (a1 - a2 < half_pi);
				} else {
					if (a2 < a1) // < and not <=
						a2 += two_pi;
					bAddSegment = (a2 - a1 < half_pi);
				}
				if (bAddSegment) {
					// make it continuous when angle is pi/2 (but not tangent to
					// the round join)
					double d = m_distance * sqrt2;
					double a;

					if (d < 0.0)
						a = a1 + pi * 0.25;
					else
						a = a1 + 3.0 * pi * 0.25;
					p = new GraphicPoint(pt, d, a);
					addPoint(p, i);

					if (d < 0)
						a = a2 - pi * 0.25;
					else
						a = a2 - 3.0 * pi * 0.25;
					p = new GraphicPoint(pt, d, a);
					addPoint(p);
				} else // standard case: we just add the intersection point of
						// offset segments
				{
					double r = (a2 - a1) * 0.5;
					double d = m_distance / Math.abs(Math.sin(r));
					if (a2 < a1)
						a2 += two_pi; // this guaranties that (a1 + a2) / 2 is
										// on the right side with a positive
										// offset
					p = new GraphicPoint(pt, d, (a1 + a2) / 2);
					addPoint(p, i);
				}
			}
		}

		// closing point
		m_a1 = a1_0;
		m_a2 = a2_0;
		addPoint(m_offsetPts.get(0), 0);

		// make sure the first point matches the last (in case a problem of
		// reversed segment happens there)
		pt = new GraphicPoint(m_offsetPts.get(m_offsetPtCount - 1));
		m_offsetPts.set(0, pt);

		// remove loops
		return removeBadSegsFast();
	}

	void addPart(int iStart, int cPts) {
		if (cPts < 2)
			return;

		for (int i = 0; i < cPts; i++) {
			GraphicPoint pt = m_offsetPts.get(iStart + i);
			if (i != 0)
				m_resultPath.lineTo(new Point2D(pt.x, pt.y));
			else
				m_resultPath.startPath(new Point2D(pt.x, pt.y));
		}
	}

	void _OffsetPath(MultiPath multiPath, int pathIndex, MultiPath resultingPath) {
		int startVertex = multiPath.getPathStart(pathIndex);
		int endVertex = multiPath.getPathEnd(pathIndex);

		m_offsetPts = new ArrayList<GraphicPoint>();

		// test if part is closed
		m_resultPath = resultingPath;
		m_resultPoints = 0;
		if (multiPath.isClosedPath(pathIndex)) {
			// check if last point is a duplicate of first
			Point2D ptStart = multiPath.getXY(startVertex);
			while (multiPath.getXY(endVertex - 1).isEqual(ptStart))
				endVertex--;

			// we need at least three points for a polygon
			if (endVertex - startVertex >= 2) {
				m_srcPtCount = endVertex - startVertex;
				m_srcPts = new ArrayList<GraphicPoint>(m_srcPtCount);
				// TODO: may throw std::bad:alloc()
				for (int i = startVertex; i < endVertex; i++)
					m_srcPts.add(new GraphicPoint(multiPath.getXY(i)));

				if (buildOffset())
					addPart(0, m_offsetPtCount - 1); // do not repeat closing
														// point
			}
		} else {
			// remove duplicate points at extremities
			Point2D ptStart = multiPath.getXY(startVertex);
			while ((startVertex < endVertex)
					&& multiPath.getXY(startVertex + 1).isEqual(ptStart))
				startVertex++;
			Point2D ptEnd = multiPath.getXY(endVertex - 1);
			while ((startVertex < endVertex)
					&& multiPath.getXY(endVertex - 2).isEqual(ptEnd))
				endVertex--;

			// we need at least two points for a polyline
			if (endVertex - startVertex >= 2) {
				// close the line and mark the opposite segments as non valid
				m_srcPtCount = (endVertex - startVertex) * 2 - 2;
				m_srcPts = new ArrayList<GraphicPoint>(m_srcPtCount);
				// TODO: may throw std::bad:alloc()

				GraphicPoint pt = new GraphicPoint(multiPath.getXY(startVertex));
				pt.type |= IS_END + CLOSING_SEG;
				m_srcPts.add(pt);

				for (int i = startVertex + 1; i < endVertex - 1; i++) {
					pt = new GraphicPoint(multiPath.getXY(i));
					m_srcPts.add(pt);
				}

				pt = new GraphicPoint(multiPath.getXY(endVertex - 1));
				pt.type |= IS_END;
				m_srcPts.add(pt);

				for (int i = endVertex - 2; i >= startVertex + 1; i--) {
					pt = new GraphicPoint(multiPath.getXY(i));
					pt.type |= CLOSING_SEG;
					m_srcPts.add(pt);
				}

				if (buildOffset())

					if (m_offsetPts.size() >= 2) {
						// extract the part that doesn't have the CLOSING_SEG
						// attribute

						int iStart = -1;
						int iEnd = -1;
						boolean prevClosed = (m_offsetPts
								.get(m_offsetPtCount - 1).type & CLOSING_SEG) != 0;
						if (!prevClosed)
							iStart = 0;
						for (int i = 1; i < m_offsetPtCount; i++) {
							boolean closed = (m_offsetPts.get(i).type & CLOSING_SEG) != 0;
							if (!closed) {
								if (prevClosed) {
									// if ((m_offsetPts[i - 1].type & MOVE_TO)
									// == 0)
									// m_offsetPts[i - 1].type += MOVE_TO -
									// LINE_TO;
									iStart = i - 1;
								}
							} else {
								if (!prevClosed) {
									iEnd = i - 1;
									// for (long i = iStart; i <= iEnd; i++)
									// m_offsetPts[i].type &= OUR_FLAGS_MASK;
									if (iEnd - iStart + 1 > 1)
										addPart(iStart, iEnd - iStart + 1);
								}
							}
							prevClosed = closed;
						}
						if (!prevClosed) {
							iEnd = m_offsetPtCount - 1;
							// for (long i = iStart; i <= iEnd; i++)
							// m_offsetPts[i].type &= OUR_FLAGS_MASK;
							if (iEnd - iStart + 1 > 1)
								addPart(iStart, iEnd - iStart + 1);
						}
					} else {
						int iStart = 0;
						int iEnd = m_offsetPtCount - 1;
						if (iStart >= 0 && iEnd - iStart >= 1) {
							// for (long i = iStart; i <= iEnd; i++)
							// m_offsetPts[i].type &= OUR_FLAGS_MASK;
							addPart(iStart, iEnd - iStart + 1);
						}
					}
			}
		}

		// clear source
		m_srcPts = null;
		m_srcPtCount = 0;
		// free offset buffer
		m_offsetPts = null;
		m_offsetPtCount = 0;
	}

	boolean removeBadSegsFast() {
		boolean bWrong = false;

		// initialize circular doubly-linked list
		// skip last point which is dup of first point
		for (int i = 0; i < m_offsetPtCount; i++) {
			GraphicPoint pt = m_offsetPts.get(i);
			pt.m_next = i + 1;
			pt.m_prev = i - 1;
			m_offsetPts.set(i, pt);
		}

		// need to update the first and last elements
		GraphicPoint pt;

		pt = m_offsetPts.get(0);
		pt.m_prev = m_offsetPtCount - 2;
		m_offsetPts.set(0, pt);

		pt = m_offsetPts.get(m_offsetPtCount - 2);
		pt.m_next = 0;
		m_offsetPts.set(m_offsetPtCount - 2, pt);

		int w = 0;
		for (int i = 0; i < m_offsetPtCount; i++) {
			if ((m_offsetPts.get(w).type & BAD_SEG) != 0) {
				int wNext = deleteClosedSeg(w);
				if (wNext != -1)
					w = wNext;
				else {
					bWrong = true;
					break;
				}
			} else
				w = m_offsetPts.get(w).m_next;
		}

		if (bWrong)
			return false;

		// w is the index of a known good (i.e. surviving ) point in the offset
		// array
		compressOffsetArray(w);
		return true;
	}

	int deleteClosedSeg(int seg) {
		int n = m_offsetPtCount - 1; // number of segments

		// check combinations of segments
		int ip0 = seg, ip, im;

		for (int i = 1; i <= n - 2; i++) {
			ip0 = m_offsetPts.get(ip0).m_next;

			ip = ip0;
			im = seg;

			for (int j = 1; j <= i; j++) {
				im = m_offsetPts.get(im).m_prev;

				if ((m_offsetPts.get(im).type & BAD_SEG) == 0
						&& (m_offsetPts.get(ip).type & BAD_SEG) == 0) {
					int rSegNext = handleClosedIntersection(im, ip);
					if (rSegNext != -1)
						return rSegNext;
				}

				ip = m_offsetPts.get(ip).m_prev;
			}
		}

		return -1;
	}

	// line segments defined by (im-1, im) and (ip-1, ip)
	int handleClosedIntersection(int im, int ip) {
		GraphicPoint pt1, pt2, pt3, pt4;
		pt1 = m_offsetPts.get(m_offsetPts.get(im).m_prev);
		pt2 = m_offsetPts.get(im);
		pt3 = m_offsetPts.get(m_offsetPts.get(ip).m_prev);
		pt4 = m_offsetPts.get(ip);

		if (!sectGraphicRect(pt1, pt2, pt3, pt4))
			return -1;

		// intersection
		IntersectionInfo ii = new IntersectionInfo();
		if (findIntersection(pt1, pt2, pt3, pt4, ii) && !ii.atExistingPt)
			if (Math.signum((pt2.x - pt1.x) * (pt4.y - pt3.y) - (pt2.y - pt1.y)
					* (pt4.x - pt3.x)) != Math.signum(m_distance)) {
				int prev0 = m_offsetPts.get(im).m_prev;

				ii.pt.type = pt2.type;
				ii.pt.m_next = ip;
				ii.pt.m_prev = prev0;
				m_offsetPts.set(im, ii.pt);

				ii.pt = m_offsetPts.get(ip);
				ii.pt.m_prev = im;
				m_offsetPts.set(ip, ii.pt);

				return ip;
			}
		return -1;
	}

	boolean sectGraphicRect(GraphicPoint pt1, GraphicPoint pt2,
			GraphicPoint pt3, GraphicPoint pt4) {
		return (Math.max(pt1.x, pt2.x) >= Math.min(pt3.x, pt4.x)
				&& Math.max(pt3.x, pt4.x) >= Math.min(pt1.x, pt2.x)
				&& Math.max(pt1.y, pt2.y) >= Math.min(pt3.y, pt4.y) && Math
				.max(pt3.y, pt4.y) >= Math.min(pt1.y, pt2.y));
	}

	boolean findIntersection(GraphicPoint bp1, GraphicPoint bp2,
			GraphicPoint bp3, GraphicPoint bp4,
			IntersectionInfo intersectionInfo) {
		intersectionInfo.atExistingPt = false;

		// Note: test if rectangles intersect already done by caller

		// intersection
		double i, j, r, r1;
		i = (bp2.y - bp1.y) * (bp4.x - bp3.x) - (bp2.x - bp1.x)
				* (bp4.y - bp3.y);
		j = (bp3.y - bp1.y) * (bp2.x - bp1.x) - (bp3.x - bp1.x)
				* (bp2.y - bp1.y);
		if (i == 0.0)
			r = 2.0;
		else
			r = j / i;

		if ((r >= 0.0) && (r <= 1.0)) {
			r1 = r;
			i = (bp4.y - bp3.y) * (bp2.x - bp1.x) - (bp4.x - bp3.x)
					* (bp2.y - bp1.y);
			j = (bp1.y - bp3.y) * (bp4.x - bp3.x) - (bp1.x - bp3.x)
					* (bp4.y - bp3.y);

			if (i == 0.0)
				r = 2.0;
			else
				r = j / i;

			if ((r >= 0.0) && (r <= 1.0)) {
				intersectionInfo.pt = new GraphicPoint(bp1.x + r
						* (bp2.x - bp1.x), bp1.y + r * (bp2.y - bp1.y));
				intersectionInfo.pt.m = bp3.m + r1 * (bp4.m - bp3.m);
				if (((r1 == 0.0) || (r1 == 1.0)) && ((r == 0.0) || (r == 1.0)))
					intersectionInfo.atExistingPt = true;

				intersectionInfo.rFirst = r;
				intersectionInfo.rSecond = r1;

				if (((r1 == 0.0) || (r1 == 1.0)) && ((r > 0.0) && (r < 1.0))
						|| ((r == 0.0) || (r == 1.0))
						&& ((r1 > 0.0) && (r1 < 1.0))) {
					return false;
				}

				return true;
			}
		}
		return false;
	}

	// i0 is the index of a known good point in the offset points array; that
	// is, its the index of a point that isn't part of a deleted loop
	void compressOffsetArray(int i0) {
		int i_ = i0;
		while (m_offsetPts.get(i_).m_prev < i_)
			i_ = m_offsetPts.get(i_).m_prev;

		int j = 0, i = i_;

		do {
			GraphicPoint pt = m_offsetPts.get(i);
			m_offsetPts.set(j, pt);
			i = pt.m_next;
			j++;
		} while (i != i_);

		m_offsetPts.set(j, m_offsetPts.get(0)); // duplicate closing point

		m_offsetPtCount = j + 1;
	}

	void _OffsetMultiPath(MultiPath resultingPath) {
		// we process all path independently, then merge the results
		MultiPath multiPath = (MultiPath) m_inputGeometry;
		SegmentIterator segmentIterator = multiPath.querySegmentIterator();
		if (segmentIterator == null)
			return; // TODO: strategy on error?

		segmentIterator.resetToFirstPath();
		int pathIndex = -1;
		while (segmentIterator.nextPath()) {
			pathIndex++;
			_OffsetPath(multiPath, pathIndex, resultingPath);
		}
	}

	Geometry _ConstructOffset() {
		int gt = m_inputGeometry.getType().value();
		if (gt == Geometry.GeometryType.Line) {
			return _OffsetLine();
		}
		if (gt == Geometry.GeometryType.Envelope) {
			return _OffsetEnvelope();
		}
		if (Geometry.isSegment(gt)) {
			Polyline poly = new Polyline();
			poly.addSegment((Segment) m_inputGeometry, true);
			m_inputGeometry = poly;
			return _ConstructOffset();
		}
		if (gt == Geometry.GeometryType.Polyline) {
			Polyline polyline = new Polyline();
			_OffsetMultiPath(polyline);
			return polyline;
		}
		if (gt == Geometry.GeometryType.Polygon) {
			Polygon polygon = new Polygon();
			_OffsetMultiPath(polygon);
			return polygon;
		}
		// throw new GeometryException("not implemented");
		return null;
	}
}
