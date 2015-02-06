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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.esri.core.geometry;

import java.util.ArrayList;

class SweepComparator extends Treap.Comparator {
	static final class SimpleEdge {
		int m_value;
		Line m_line;
		Segment m_segment;
		Envelope1D m_env;
		double m_dxdy;
		boolean m_b_horizontal;
		boolean m_b_curve;

		SimpleEdge() {
			m_value = -1;
			m_line = new Line();
			m_dxdy = 55555555;
			m_b_horizontal = false;
			m_b_curve = false;

			m_env = new Envelope1D();
			m_env.setCoordsNoNaN_(0, 0);
		}
	}

	private EditShape m_shape;
	boolean m_b_intersection_detected;
	NonSimpleResult m_non_simple_result;
	// Index 1 corresponds to the left segments, index 2 - right, e.g. m_line_1,
	// m_line_2
	SimpleEdge m_temp_simple_edge_1;
	SimpleEdge m_temp_simple_edge_2;

	int m_prev_1;
	int m_prev_2;
	int m_vertex_1;
	int m_vertex_2;
	int m_current_node;
	double m_prevx_1;
	double m_prevx_2;
	double m_prev_y;
	double m_prev_x;
	double m_sweep_y;
	double m_sweep_x;
	double m_tolerance;
	double m_tolerance_10;
	boolean m_b_is_simple;

	ArrayList<SimpleEdge> m_simple_edges_cache;
	ArrayList<SimpleEdge> m_simple_edges_recycle;
	ArrayList<SimpleEdge> m_simple_edges_buffer;

	// Returns a cached edge for the given value. May return NULL.
	SimpleEdge tryGetCachedEdge_(int value) {
		SimpleEdge se = m_simple_edges_cache.get((value & NumberUtils.intMax())
				% m_simple_edges_cache.size());
		if (se != null) {
			if (se.m_value == value)
				return se;
			else {
				// int i = 0;
				// cache collision
			}
		}
		return null;
	}

	// Removes cached edge from the cache for the given value.
	void tryDeleteCachedEdge_(int value) {
		int ind = (value & NumberUtils.intMax()) % m_simple_edges_cache.size();
		SimpleEdge se = m_simple_edges_cache.get(ind);
		if (se != null && se.m_value == value) {// this value is cached
			m_simple_edges_recycle.add(se);
			m_simple_edges_cache.set(ind, null);
		} else {
			// The value has not been cached
		}
	}

	// Creates a cached edge. May fail and return NULL.
	SimpleEdge tryCreateCachedEdge_(int value) {
		int ind = (value & NumberUtils.intMax()) % m_simple_edges_cache.size();
		SimpleEdge se = m_simple_edges_cache.get(ind);
		if (se == null) {
			if (m_simple_edges_recycle.isEmpty()) {
				// assert(m_simple_edges_buffer.size() <
				// m_simple_edges_buffer.capacity());//should never happen
				// assert(m_simple_edges_buffer.size() <
				// m_simple_edges_cache.size());//should never happen
				m_simple_edges_buffer.add(new SimpleEdge());
				se = m_simple_edges_buffer
						.get(m_simple_edges_buffer.size() - 1);
			} else {
				se = m_simple_edges_recycle
						.get(m_simple_edges_recycle.size() - 1);
				m_simple_edges_recycle
						.remove(m_simple_edges_recycle.size() - 1);
			}

			se.m_value = value;
			m_simple_edges_cache.set(ind, se);
			return se;
		} else {
			assert (se.m_value != value);// do not call TryCreateCachedEdge
											// twice.
		}

		return null;
	}

	void initSimpleEdge_(SweepComparator.SimpleEdge se, int vertex) {
		se.m_segment = m_shape.getSegment(vertex);
		se.m_b_curve = se.m_segment != null;
		if (!se.m_b_curve) {
			m_shape.queryLineConnector(vertex, se.m_line);
			se.m_segment = se.m_line;
			se.m_env.setCoordsNoNaN_(se.m_line.getStartX(), se.m_line.getEndX());
			se.m_env.vmax += m_tolerance;
			se.m_line.orientBottomUp_();
			se.m_b_horizontal = se.m_line.getEndY() == se.m_line.getStartY();
			if (!se.m_b_horizontal) {
				se.m_dxdy = (se.m_line.getEndX() - se.m_line.getStartX())
						/ (se.m_line.getEndY() - se.m_line.getStartY());
			}
		} else {
			// se.m_segment = se.m_segment_sptr.get();
		}
	}

	// Compares seg_1 and seg_2 x coordinates of intersection with the line
	// parallel to axis x, passing through the coordinate y.
	// If segments intersect not at the endpoint, the m_b_intersection_detected
	// is set.
	int compareTwoSegments_(Segment seg_1, Segment seg_2) {
		int res = seg_1._isIntersecting(seg_2, m_tolerance, true);
		if (res != 0) {
			if (res == 2)
				return errorCoincident();
			else
				return errorCracking();
		}

		Point2D start_1 = seg_1.getStartXY();
		Point2D end1 = seg_1.getEndXY();
		Point2D start2 = seg_2.getStartXY();
		Point2D end2 = seg_2.getEndXY();
		Point2D ptSweep = new Point2D();
		ptSweep.setCoords(m_sweep_x, m_sweep_y);
		if (start_1.isEqual(start2) && m_sweep_y == start_1.y) {
			assert (start_1.compare(end1) < 0 && start2.compare(end2) < 0);
			if (end1.compare(end2) < 0)
				ptSweep.setCoords(end1);
			else
				ptSweep.setCoords(end2);
		} else if (start_1.isEqual(end2) && m_sweep_y == start_1.y) {
			assert (start_1.compare(end1) < 0 && start2.compare(end2) > 0);
			if (end1.compare(start2) < 0)
				ptSweep.setCoords(end1);
			else
				ptSweep.setCoords(start2);
		} else if (start2.isEqual(end1) && m_sweep_y == start2.y) {
			assert (end1.compare(start_1) < 0 && start2.compare(end2) < 0);
			if (start_1.compare(end2) < 0)
				ptSweep.setCoords(start_1);
			else
				ptSweep.setCoords(end2);
		} else if (end1.isEqual(end2) && m_sweep_y == end1.y) {
			assert (start_1.compare(end1) > 0 && start2.compare(end2) > 0);
			if (start_1.compare(start2) < 0)
				ptSweep.setCoords(start_1);
			else
				ptSweep.setCoords(start2);
		}

		double xleft = seg_1.intersectionOfYMonotonicWithAxisX(ptSweep.y,
				ptSweep.x);
		double xright = seg_2.intersectionOfYMonotonicWithAxisX(ptSweep.y,
				ptSweep.x);
		assert (xleft != xright);
		return xleft < xright ? -1 : 1;
	}

	int compareNonHorizontal_(SimpleEdge line_1, SimpleEdge line_2) {
		if (line_1.m_line.getStartY() == line_2.m_line.getStartY()
				&& line_1.m_line.getStartX() == line_2.m_line.getStartX()) {// connected
																			// at
																			// the
																			// start
																			// V
																			// shape
			if (line_1.m_line.getEndY() == line_2.m_line.getEndY()
					&& line_1.m_line.getEndX() == line_2.m_line.getEndX()) {// connected
																			// at
																			// another
																			// end
																			// also
				if (m_b_is_simple)
					return errorCoincident();
				return 0;
			}

			return compareNonHorizontalUpperEnd_(line_1, line_2);
		}

		if (line_1.m_line.getEndY() == line_2.m_line.getEndY()
				&& line_1.m_line.getEndX() == line_2.m_line.getEndX()) {
			// the case of upside-down V.
			return compareNonHorizontalLowerEnd_(line_1, line_2);
		}

		int lower = compareNonHorizontalLowerEnd_(line_1, line_2);
		int upper = compareNonHorizontalUpperEnd_(line_1, line_2);
		if (lower < 0 && upper < 0)
			return -1;
		if (lower > 0 && upper > 0)
			return 1;

		return errorCracking();
	}

	int compareHorizontal1Case1_(Line line_1, Line line_2) {
		// line_2 goes up and line_1 is horizontal connected at the start going
		// to the right.
		if (line_1.getEndX() > line_2.getEndX()) {
			// /
			// /
			// +------------------
			if (line_2.getEndX() > line_2.getStartX()
					&& line_2.getEndY() - line_2.getStartY() < 2 * m_tolerance
					&& line_1._isIntersectingPoint(line_2.getEndXY(),
							m_tolerance, true))
				return errorCracking();
		} else {
			// /
			// /
			// /
			// +--
			assert (line_2.getEndX() - line_2.getStartX() != 0);
			// Note: line_2 cannot be vertical here
			// Avoid expensive is_intersecting_ by providing a simple estimate.
			double dydx = (line_2.getEndY() - line_2.getStartY())
					/ (line_2.getEndX() - line_2.getStartX());
			double d = dydx * (line_1.getEndX() - line_1.getStartX());
			if (d < m_tolerance_10
					&& line_2._isIntersectingPoint(line_1.getEndXY(),
							m_tolerance, true))
				return errorCracking();
		}

		return 1;
	}

	int compareHorizontal1Case2_(Line line_1, Line line_2) {
		// -----------------+
		// /
		// /
		// /
		// line_2 goes up and below line_1. line_1 is horizontal connected at
		// the end to the line_2 end.
		if (line_1.getStartX() < line_2.getStartX()) {
			if (line_2.getEndX() > line_2.getStartX()
					&& line_2.getEndY() - line_2.getStartY() < 2 * m_tolerance
					&& line_1._isIntersectingPoint(line_2.getEndXY(),
							m_tolerance, true))
				return errorCracking();
		} else {
			// --+
			// /
			// /
			// /
			// Avoid expensive is_intersecting_ by providing a simple estimate.
			double dydx = (line_2.getEndY() - line_2.getStartY())
					/ (line_2.getEndX() - line_2.getStartX());
			double d = dydx * (line_1.getStartX() - line_1.getEndX());
			if (d < m_tolerance_10
					&& line_2._isIntersectingPoint(line_1.getStartXY(),
							m_tolerance, true))
				return errorCracking();
		}

		return -1;
	}

	int compareHorizontal1Case3_(Line line_1, Line line_2) {
		Point2D v0 = new Point2D();
		v0.sub(line_2.getEndXY(), line_2.getStartXY());
		v0.rightPerpendicular();
		v0.normalize();
		Point2D v_1 = new Point2D();
		v_1.sub(line_1.getStartXY(), line_2.getStartXY());
		Point2D v_2 = new Point2D();
		v_2.sub(line_1.getEndXY(), line_2.getStartXY());
		double d_1 = v_1.dotProduct(v0);
		double d_2 = v_2.dotProduct(v0);

		double ad1 = Math.abs(d_1);
		double ad2 = Math.abs(d_2);

		if (ad1 < ad2) {
			if (ad1 < m_tolerance_10
					&& line_2._isIntersectingPoint(line_1.getStartXY(),
							m_tolerance, true))
				return errorCracking();
		} else {
			if (ad2 < m_tolerance_10
					&& line_2._isIntersectingPoint(line_1.getEndXY(),
							m_tolerance, true))
				return errorCracking();
		}

		if (d_1 < 0 && d_2 < 0)
			return -1;

		if (d_1 > 0 && d_2 > 0)
			return 1;

		return errorCracking();
	}

	int compareHorizontal1_(Line line_1, Line line_2) {
		// Two most important cases of connecting edges
		if (line_1.getStartY() == line_2.getStartY()
				&& line_1.getStartX() == line_2.getStartX()) {
			return compareHorizontal1Case1_(line_1, line_2);
		}

		if (line_1.getEndY() == line_2.getEndY()
				&& line_1.getEndX() == line_2.getEndX()) {
			return compareHorizontal1Case2_(line_1, line_2);
		}

		return compareHorizontal1Case3_(line_1, line_2);
	}

	int compareHorizontal2_(Line line_1, Line line_2) {
		if (line_1.getEndY() == line_2.getEndY()
				&& line_1.getEndX() == line_2.getEndX()
				&& line_1.getStartY() == line_2.getStartY()
				&& line_1.getStartX() == line_2.getStartX()) {// both lines
																// coincide
			if (m_b_is_simple)
				return errorCoincident();
			return 0;
		} else
			return errorCracking();
	}

	int compareNonHorizontalLowerEnd_(SimpleEdge line_1, SimpleEdge line_2) {
		int sign = 1;
		if (line_1.m_line.getStartY() < line_2.m_line.getStartY()) {
			sign = -1;
			SimpleEdge tmp = line_1;
			line_1 = line_2;
			line_2 = tmp;
		}

		Line l1 = line_1.m_line;
		Line l2 = line_2.m_line;
		// Now line_1 has Start point higher than line_2 startpoint.
		double x_1 = l1.getStartX() - l2.getStartX();
		double x2 = line_2.m_dxdy * (l1.getStartY() - l2.getStartY());
		double tol = m_tolerance_10;
		if (x_1 < x2 - tol)
			return -sign;
		else if (x_1 > x2 + tol)
			return sign;
		else // Possible problem
		{
			if (l2._isIntersectingPoint(l1.getStartXY(), m_tolerance, true))
				return errorCracking();
			return x_1 < x2 ? -sign : sign;
		}
	}

	int compareNonHorizontalUpperEnd_(SimpleEdge line_1, SimpleEdge line_2) {
		int sign = 1;
		if (line_2.m_line.getEndY() < line_1.m_line.getEndY()) {
			sign = -1;
			SimpleEdge tmp = line_1;
			line_1 = line_2;
			line_2 = tmp;
		}

		Line l1 = line_1.m_line;
		Line l2 = line_2.m_line;
		// Now line_1 has End point lower than line_2 endpoint.
		double x_1 = l1.getEndX() - l2.getStartX();
		double x2 = line_2.m_dxdy * (l1.getEndY() - l2.getStartY());
		double tol = m_tolerance_10;
		if (x_1 < x2 - tol)
			return -sign;
		else if (x_1 > x2 + tol)
			return sign;
		else // Possible problem
		{
			if (l2._isIntersectingPoint(l1.getEndXY(), m_tolerance, true))
				return errorCracking();
			return x_1 < x2 ? -sign : sign;
		}
	}

	int errorCoincident() {// two segments coincide.
		m_b_intersection_detected = true;
		assert (m_b_is_simple);
		NonSimpleResult.Reason reason = NonSimpleResult.Reason.CrossOver;
		m_non_simple_result = new NonSimpleResult(reason, m_vertex_1,
				m_vertex_2);
		return -1;
	}

	int errorCracking() {// cracking error
		m_b_intersection_detected = true;
		if (m_b_is_simple) {// only report the reason in IsSimple. Do not do
							// that for regular cracking.
			NonSimpleResult.Reason reason = NonSimpleResult.Reason.Cracking;
			m_non_simple_result = new NonSimpleResult(reason, m_vertex_1,
					m_vertex_2);
		} else {// reset cached data after detected intersection
			m_prev_1 = -1;
			m_prev_2 = -1;
			m_vertex_1 = -1;
			m_vertex_2 = -1;
		}
		return -1;
	}

	int compareSegments_(int left, int right, SimpleEdge segLeft,
			SimpleEdge segRight) {
		if (m_b_intersection_detected)
			return -1;

		boolean sameY = m_prev_y == m_sweep_y && m_prev_x == m_sweep_x;
		double xleft;
		if (sameY && left == m_prev_1)
			xleft = m_prevx_1;
		else {
			xleft = NumberUtils.NaN();
			m_prev_1 = -1;
		}
		double xright;
		if (sameY && right == m_prev_2)
			xright = m_prevx_2;
		else {
			xright = NumberUtils.NaN();
			m_prev_2 = -1;
		}

		// Quickly compare x projections.
		Envelope1D envLeft = segLeft.m_segment.queryInterval(
				VertexDescription.Semantics.POSITION, 0);
		Envelope1D envRight = segRight.m_segment.queryInterval(
				VertexDescription.Semantics.POSITION, 0);
		if (envLeft.vmax < envRight.vmin)
			return -1;
		if (envRight.vmax < envLeft.vmin)
			return 1;

		m_prev_y = m_sweep_y;
		m_prev_x = m_sweep_x;

		// Now do intersection with the sweep line (it is a line parallel to the
		// axis x.)
		if (NumberUtils.isNaN(xleft)) {
			m_prev_1 = left;
			double x = segLeft.m_segment.intersectionOfYMonotonicWithAxisX(
					m_sweep_y, m_sweep_x);
			xleft = x;
			m_prevx_1 = x;
		}
		if (NumberUtils.isNaN(xright)) {
			m_prev_2 = right;
			double x = segRight.m_segment.intersectionOfYMonotonicWithAxisX(
					m_sweep_y, m_sweep_x);
			xright = x;
			m_prevx_2 = x;
		}

		if (Math.abs(xleft - xright) <= m_tolerance) {
			// special processing as we cannot decide in a simple way.
			return compareTwoSegments_(segLeft.m_segment, segRight.m_segment);
		} else {
			return xleft < xright ? -1 : xleft > xright ? 1 : 0;
		}
	}

	SweepComparator(EditShape shape, double tol, boolean bIsSimple) {
		super(true);
		m_shape = shape;
		m_sweep_y = NumberUtils.TheNaN;
		m_sweep_x = 0;
		m_prev_x = 0;
		m_prev_y = NumberUtils.TheNaN;
		m_tolerance = tol;
		m_tolerance_10 = 10 * tol;
		m_prevx_2 = NumberUtils.TheNaN;
		m_prevx_1 = NumberUtils.TheNaN;
		m_b_intersection_detected = false;
		m_prev_1 = -1;
		m_prev_2 = -1;
		m_vertex_1 = -1;
		m_vertex_2 = -1;
		m_current_node = -1;
		m_b_is_simple = bIsSimple;
		m_temp_simple_edge_1 = new SimpleEdge();
		m_temp_simple_edge_2 = new SimpleEdge();

		int s = Math.min(shape.getTotalPointCount() * 3 / 2,
				(int) (67 /* SIMPLEDGE_CACHESIZE */));
		int cache_size = Math.min((int) 7, s);
		// m_simple_edges_buffer.reserve(cache_size);//must be reserved and
		// never grow beyond reserved size

		m_simple_edges_buffer = new ArrayList<SimpleEdge>();
		m_simple_edges_recycle = new ArrayList<SimpleEdge>();
		m_simple_edges_cache = new ArrayList<SimpleEdge>();

		for (int i = 0; i < cache_size; i++)
			m_simple_edges_cache.add(null);
	}

	// Makes the comparator to forget about the last detected intersection.
	// Need to be called after the intersection has been resolved.
	void clearIntersectionDetectedFlag() {
		m_b_intersection_detected = false;
	}

	// Returns True if there has been intersection detected during compare call.
	// Once intersection is detected subsequent calls to compare method do
	// nothing until clear_intersection_detected_flag is called.
	boolean intersectionDetected() {
		return m_b_intersection_detected;
	}

	// Returns the node at which the intersection has been detected
	int getLastComparedNode() {
		return m_current_node;
	}

	// When used in IsSimple (see corresponding parameter in ctor), returns the
	// reason of non-simplicity
	NonSimpleResult getResult() {
		return m_non_simple_result;
	}

	// Sets new sweep line position.
	void setSweepY(double y, double x) {
		// _ASSERT(m_sweep_y != y || m_sweep_x != x);
		m_sweep_y = y;
		m_sweep_x = x;
		m_prev_1 = -1;
		m_prev_2 = -1;
		m_vertex_1 = -1;
		m_vertex_2 = -1;
	}

	// The compare method. Compares x values of the edge given by its origin
	// (elm) and the edge in the sweep structure and checks them for
	// intersection at the same time.
	@Override
	int compare(Treap treap, int left, int node) {
		// Compares two segments on a sweep line passing through m_sweep_y,
		// m_sweep_x.
		if (m_b_intersection_detected)
			return -1;

		int right = treap.getElement(node);
		m_current_node = node;
		return compareSegments(left, left, right, right);
	}

	int compareSegments(int leftElm, int left_vertex, int right_elm,
			int right_vertex) {
		SimpleEdge edgeLeft = tryGetCachedEdge_(leftElm);
		if (edgeLeft == null) {
			if (m_vertex_1 == left_vertex)
				edgeLeft = m_temp_simple_edge_1;
			else {
				m_vertex_1 = left_vertex;
				edgeLeft = tryCreateCachedEdge_(leftElm);
				if (edgeLeft == null) {
					edgeLeft = m_temp_simple_edge_1;
					m_temp_simple_edge_1.m_value = leftElm;
				}
				initSimpleEdge_(edgeLeft, left_vertex);
			}
		} else
			m_vertex_1 = left_vertex;

		SimpleEdge edgeRight = tryGetCachedEdge_(right_elm);
		if (edgeRight == null) {
			if (m_vertex_2 == right_vertex)
				edgeRight = m_temp_simple_edge_2;
			else {
				m_vertex_2 = right_vertex;
				edgeRight = tryCreateCachedEdge_(right_elm);
				if (edgeRight == null) {
					edgeRight = m_temp_simple_edge_2;
					m_temp_simple_edge_2.m_value = right_elm;
				}
				initSimpleEdge_(edgeRight, right_vertex);
			}
		} else
			m_vertex_2 = right_vertex;

		if (edgeLeft.m_b_curve || edgeRight.m_b_curve)
			return compareSegments_(left_vertex, right_vertex, edgeLeft,
					edgeRight);

		// Usually we work with lines, so process them in the fastest way.
		// First check - assume segments are far apart. compare x intervals
		if (edgeLeft.m_env.vmax < edgeRight.m_env.vmin)
			return -1;
		if (edgeRight.m_env.vmax < edgeLeft.m_env.vmin)
			return 1;

		// compare case by case.
		int kind = edgeLeft.m_b_horizontal ? 1 : 0;
		kind |= edgeRight.m_b_horizontal ? 2 : 0;
		if (kind == 0)// both segments are non-horizontal
			return compareNonHorizontal_(edgeLeft, edgeRight);
		else if (kind == 1) // line_1 horizontal, line_2 is not
			return compareHorizontal1_(edgeLeft.m_line, edgeRight.m_line);
		else if (kind == 2) // line_2 horizontal, line_1 is not
			return compareHorizontal1_(edgeRight.m_line, edgeLeft.m_line) * -1;
		else
			// if (kind == 3) //both horizontal
			return compareHorizontal2_(edgeLeft.m_line, edgeRight.m_line);
	}

	@Override
	void onDelete(int elm) {
		tryDeleteCachedEdge_(elm);
	}

	@Override
	void onSet(int oldelm) {
		tryDeleteCachedEdge_(oldelm);
	}

	@Override
	void onEndSearch(int elm) {
		tryDeleteCachedEdge_(elm);
	}

	@Override
	void onAddUniqueElementFailed(int elm) {
		tryDeleteCachedEdge_(elm);
	}
}
