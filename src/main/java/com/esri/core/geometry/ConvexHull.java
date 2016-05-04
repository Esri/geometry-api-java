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

class ConvexHull {
	/*
	 * Constructor for a Convex_hull object. Used for dynamic insertion of geometries to create a convex hull.
	 */
	ConvexHull() {
		m_tree_hull = new Treap();
		m_tree_hull.setCapacity(20);
		m_shape = new EditShape();
		m_geometry_handle = m_shape.createGeometry(Geometry.Type.MultiPoint);
		m_path_handle = m_shape.insertPath(m_geometry_handle, -1);
		m_call_back = new CallBackShape(this);
	}

	private ConvexHull(AttributeStreamOfDbl stream, int n) {
		m_tree_hull = new Treap();
		m_tree_hull.setCapacity(Math.min(20, n));
		m_stream = stream;
		m_call_back = new CallBackStream(this);
	}

	private ConvexHull(Point2D[] points, int n) {
		m_tree_hull = new Treap();
		m_tree_hull.setCapacity(Math.min(20, n));
		m_points = points;
		m_call_back = new CallBackPoints(this);
	}

	/**
	 * Adds a geometry to the current bounding geometry using an incremental algorithm for dynamic insertion.
	 * \param geometry The geometry to add to the bounding geometry.
	 */

	void addGeometry(Geometry geometry) {
		int type = geometry.getType().value();

		if (MultiVertexGeometry.isMultiVertex(type))
			addMultiVertexGeometry_((MultiVertexGeometry) geometry);
		else if (MultiPath.isSegment(type))
			addSegment_((Segment) geometry);
		else if (type == Geometry.GeometryType.Envelope)
			addEnvelope_((Envelope) geometry);
		else if (type == Geometry.GeometryType.Point)
			addPoint_((Point) geometry);
		else
			throw new IllegalArgumentException("invalid shape type");
	}

	/**
	 * Gets the current bounding geometry.
	 * Returns a Geometry.
	 */

	Geometry getBoundingGeometry() {
		// Extracts the convex hull from the tree. Reading the tree in order from first to last is the resulting convex hull.
		Point point = new Point();
		int first = m_tree_hull.getFirst(-1);
		Polygon hull = new Polygon(m_shape.getVertexDescription());
		m_shape.queryPoint(m_tree_hull.getElement(first), point);
		hull.startPath(point);

		for (int i = m_tree_hull.getNext(first); i != -1; i = m_tree_hull.getNext(i)) {
			m_shape.queryPoint(m_tree_hull.getElement(i), point);
			hull.lineTo(point);
		}

		return hull;
	}

	/**
	 * Static method to construct the convex hull of a Multi_vertex_geometry.
	 * Returns a Geometry.
	 * \param mvg The geometry used to create the convex hull.
	 */

	static Geometry construct(MultiVertexGeometry mvg) {
		if (mvg.isEmpty())
			return new Polygon(mvg.getDescription());

		MultiVertexGeometryImpl mvg_impl = (MultiVertexGeometryImpl) mvg._getImpl();
		int N = mvg_impl.getPointCount();

		if (N <= 2) {
			if (N == 1 || mvg_impl.getXY(0).equals(mvg_impl.getXY(1))) {
				Point point = new Point(mvg_impl.getDescription());
				mvg_impl.getPointByVal(0, point);
				return point;
			} else {
				Point pt = new Point();
				Polyline polyline = new Polyline(mvg_impl.getDescription());
				mvg_impl.getPointByVal(0, pt);
				polyline.startPath(pt);
				mvg_impl.getPointByVal(1, pt);
				polyline.lineTo(pt);
				return polyline;
			}
		}

		AttributeStreamOfDbl stream = (AttributeStreamOfDbl) mvg_impl.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		ConvexHull convex_hull = new ConvexHull(stream, N);

		int t0 = 0, tm = 1;
		Point2D pt_0 = new Point2D();
		Point2D pt_m = new Point2D();
		Point2D pt_p = new Point2D();

		stream.read(t0 << 1, pt_0);

		while (true) {
			if (tm >= N)
				break;

			stream.read(tm << 1, pt_m);
			if (!pt_m.isEqual(pt_0, NumberUtils.doubleEps()))
				break;

			tm++; // We don't want to close the gap between t0 and tm.
		}

		convex_hull.m_tree_hull.addElement(t0, -1);

		if (tm < N) {
			convex_hull.m_tree_hull.addBiggestElement(tm, -1);

			for (int tp = tm + 1; tp < mvg_impl.getPointCount(); tp++) {// Dynamically insert into the current convex hull

				stream.read(tp << 1, pt_p);
				int p = convex_hull.treeHull_(pt_p);

				if (p != -1)
					convex_hull.m_tree_hull.setElement(p, tp); // reset the place holder to the point index.
			}
		}

		// Extracts the convex hull from the tree. Reading the tree in order from first to last is the resulting convex hull.

		VertexDescription description = mvg_impl.getDescription();
		boolean b_has_attributes = (description.getAttributeCount() > 1);
		int point_count = convex_hull.m_tree_hull.size(-1);

		Geometry hull;

		if (point_count >= 2) {
			if (point_count >= 3)
				hull = new Polygon(description);
			else
				hull = new Polyline(description);

			MultiPathImpl hull_impl = (MultiPathImpl) hull._getImpl();
			hull_impl.addPath((Point2D[]) null, 0, true);

			Point point = null;
			if (b_has_attributes)
				point = new Point();

			for (int i = convex_hull.m_tree_hull.getFirst(-1); i != -1; i = convex_hull.m_tree_hull.getNext(i)) {
				if (b_has_attributes) {
					mvg_impl.getPointByVal(convex_hull.m_tree_hull.getElement(i), point);
					hull_impl.insertPoint(0, -1, point);
				} else {
					stream.read(convex_hull.m_tree_hull.getElement(i) << 1, pt_p);
					hull_impl.insertPoint(0, -1, pt_p);
				}
			}
		} else {
			assert (point_count == 1);

			if (b_has_attributes) {
				Point point = new Point(description);
				mvg_impl.getPointByVal(convex_hull.m_tree_hull.getElement(convex_hull.m_tree_hull.getFirst(-1)), point);
				hull = point;
			} else {
				stream.read(convex_hull.m_tree_hull.getElement(convex_hull.m_tree_hull.getFirst(-1)) << 1, pt_p);
				hull = new Point(pt_p);
			}
		}

		return hull;
	}

	/**
	 * Static method to construct the convex hull from an array of points. The
	 * out_convex_hull array will be populated with the subset of index
	 * positions which contribute to the convex hull.
	 * Returns the number of points in the convex hull.
	 * \param points The points used to create the convex hull.
	 * \param count The number of points in the input Point2D array.
	 * \param out_convex_hull An index array allocated by the user at least as big as the size of the input points array.
	 */
	static int construct(Point2D[] points, int count, int[] out_convex_hull) {
		ConvexHull convex_hull = new ConvexHull(points, count);

		int t0 = 0, tm = 1;
		Point2D pt_0 = points[t0];

		while (tm < count && points[tm].isEqual(pt_0, NumberUtils.doubleEps()))
			tm++; // We don't want to close the gap between t0 and tm.

		convex_hull.m_tree_hull.addElement(t0, -1);

		if (tm < count) {
			convex_hull.m_tree_hull.addBiggestElement(tm, -1);

			for (int tp = tm + 1; tp < count; tp++) {// Dynamically insert into the current convex hull.

				Point2D pt_p = points[tp];
				int p = convex_hull.treeHull_(pt_p);

				if (p != -1)
					convex_hull.m_tree_hull.setElement(p, tp); // reset the place holder to the point index.
			}
		}

		// Extracts the convex hull from the tree. Reading the tree in order from first to last is the resulting convex hull.
		int out_count = 0;
		for (int i = convex_hull.m_tree_hull.getFirst(-1); i != -1; i = convex_hull.m_tree_hull.getNext(i))
			out_convex_hull[out_count++] = convex_hull.m_tree_hull.getElement(i);

		return out_count;
	}

	/**
	 * Returns true if the given path of the input MultiPath is convex. Returns false otherwise.
	 * \param multi_path The MultiPath to check if the path is convex.
	 * \param path_index The path of the MultiPath to check if its convex.
	 */
	static boolean isPathConvex(MultiPath multi_path, int path_index, ProgressTracker progress_tracker) {
		MultiPathImpl mimpl = (MultiPathImpl) multi_path._getImpl();
		int path_start = mimpl.getPathStart(path_index);
		int path_end = mimpl.getPathEnd(path_index);

		boolean bxyclosed = !mimpl.isClosedPath(path_index) && mimpl.isClosedPathInXYPlane(path_index);

		AttributeStreamOfDbl position = (AttributeStreamOfDbl) (mimpl.getAttributeStreamRef(VertexDescription.Semantics.POSITION));
		int position_start = 2 * path_start;
		int position_end = 2 * path_end;

		if (bxyclosed)
			position_end -= 2;

		if (position_end - position_start < 6)
			return true;

		// This matches the logic for case 1 of the tree hull algorithm. The idea is inductive. We assume we have a convex hull pt_0,...,pt_m, and we see if
		// a new point (pt_pivot) is among the transitive tournament for pt_0, knowing that pt_pivot comes after pt_m.

		// We check three conditions:
		// 1) pt_m->pt_pivot->pt_0 is clockwise (closure across the boundary is convex)
		// 2) pt_1->pt_pivot->pt_0 is clockwise (the first step forward is convex)  (pt_1 is the next point after pt_0)
		// 3) pt_m->pt_pivot->pt_m_prev is clockwise (the first step backwards is convex)  (pt_m_prev is the previous point before pt_m)

		// If all three of the above conditions are clockwise, then pt_pivot is among the transitive tournament for pt_0, and therefore the polygon pt_0, ..., pt_m, pt_pivot is convex.

		Point2D pt_0 = new Point2D(), pt_m = new Point2D(), pt_pivot = new Point2D();
		position.read(position_start, pt_0);
		position.read(position_start + 2, pt_m);
		position.read(position_start + 4, pt_pivot);

		// Initial inductive step
		ECoordinate det_ec = determinant_(pt_m, pt_pivot, pt_0);

		if (det_ec.isFuzzyZero() || !isClockwise_(det_ec.value()))
			return false;

		Point2D pt_1 = new Point2D(pt_m.x, pt_m.y);
		Point2D pt_m_prev = new Point2D();

		// Assume that pt_0,...,pt_m is convex. Check if the next point, pt_pivot, maintains the convex invariant.
		for (int i = position_start + 6; i < position_end; i += 2) {
			pt_m_prev.setCoords(pt_m);
			pt_m.setCoords(pt_pivot);
			position.read(i, pt_pivot);

			det_ec = determinant_(pt_m, pt_pivot, pt_0);

			if (det_ec.isFuzzyZero() || !isClockwise_(det_ec.value()))
				return false;

			det_ec = determinant_(pt_1, pt_pivot, pt_0);

			if (det_ec.isFuzzyZero() || !isClockwise_(det_ec.value()))
				return false;

			det_ec = determinant_(pt_m, pt_pivot, pt_m_prev);

			if (det_ec.isFuzzyZero() || !isClockwise_(det_ec.value()))
				return false;
		}

		return true;
	}

	// Dynamically inserts each geometry into the convex hull.
	private void addMultiVertexGeometry_(MultiVertexGeometry mvg) {
		Point point = new Point();
		Point2D pt_p = new Point2D();

		for (int i = 0; i < mvg.getPointCount(); i++) {
			mvg.getXY(i, pt_p);
			int p = addPoint_(pt_p);

			if (p != -1) {
				mvg.getPointByVal(i, point);
				int tp = m_shape.addPoint(m_path_handle, point);
				m_tree_hull.setElement(p, tp); // reset the place holder to tp
			}
		}
	}

	private void addEnvelope_(Envelope envelope) {
		Point point = new Point();
		Point2D pt_p = new Point2D();

		for (int i = 0; i < 4; i++) {
			envelope.queryCorner(i, pt_p);
			int p = addPoint_(pt_p);

			if (p != -1) {
				envelope.queryCornerByVal(i, point);
				int tp = m_shape.addPoint(m_path_handle, point);
				m_tree_hull.setElement(p, tp); // reset the place holder to tp
			}
		}
	}

	private void addSegment_(Segment segment) {
		Point point = new Point();

		Point2D pt_start = segment.getStartXY();
		int p_start = addPoint_(pt_start);

		if (p_start != -1) {
			segment.queryStart(point);
			int t_start = m_shape.addPoint(m_path_handle, point);
			m_tree_hull.setElement(p_start, t_start); // reset the place holder
			// to tp
		}

		Point2D pt_end = segment.getEndXY();
		int p_end = addPoint_(pt_end);

		if (p_end != -1) {
			segment.queryEnd(point);
			int t_end = m_shape.addPoint(m_path_handle, point);
			m_tree_hull.setElement(p_end, t_end); // reset the place holder to
			// tp
		}
	}

	private void addPoint_(Point point) {
		Point2D pt_p = point.getXY();
		int p = addPoint_(pt_p);

		if (p != -1) {
			int tp = m_shape.addPoint(m_path_handle, point);
			m_tree_hull.setElement(p, tp); // reset the place holder to tp
		}
	}

	private int addPoint_(Point2D pt_p) {
		int p = -1;

		if (m_tree_hull.size(-1) == 0) {
			p = m_tree_hull.addElement(-4, -1); // reset the place holder to tp
			return p;
		}

		if (m_tree_hull.size(-1) == 1) {
			int t0 = m_tree_hull.getElement(m_tree_hull.getFirst(-1));
			Point2D pt_0 = m_shape.getXY(t0);

			if (!pt_p.isEqual(pt_0, NumberUtils.doubleEps())) // We don't want to close the gap between t0 and tm.
				p = m_tree_hull.addBiggestElement(-5, -1); // set place holder to -5 to indicate the second element being added (tm).

			return p;
		}

		p = treeHull_(pt_p);
		return p;
	}

	// Algorithm taken from "Axioms and Hulls" by D.E. Knuth, Lecture Notes in Computer Science 606, page 47.
	private int treeHull_(Point2D pt_pivot) {
		assert (m_tree_hull.size(-1) >= 2);

		int p = -1;

		do {
			int first = m_tree_hull.getFirst(-1);
			int last = m_tree_hull.getLast(-1);
			int t0 = m_tree_hull.getElement(first);
			int tm = m_tree_hull.getElement(last);

			Point2D pt_0 = new Point2D(); // should the memory be cached?
			Point2D pt_m = new Point2D(); // should the memory be cached?
			m_call_back.getXY(t0, pt_0);
			m_call_back.getXY(tm, pt_m);

			assert (!pt_0.isEqual(pt_m, NumberUtils.doubleEps())); // assert that the gap is not closed

			int orient_m_p_0 = Point2D.orientationRobust(pt_m, pt_pivot, pt_0); // determines case 1, 2, 3

			if (isClockwise_(orient_m_p_0)) {// Case 1: tp->t0->tm is clockwise

				p = m_tree_hull.addBiggestElement(-1, -1); // set place holder to -1 for case 1.
				int l = treeHullWalkBackward_(pt_pivot, last, first);

				if (l != first)
					treeHullWalkForward_(pt_pivot, first, m_tree_hull.getPrev(l));

				continue;
			}

			if (isCounterClockwise_(orient_m_p_0)) {// Case 2: tp->tm->t0 is clockwise
				int k = m_tree_hull.getRoot(-1), k_min = m_tree_hull.getFirst(-1), k_max = m_tree_hull.getLast(-1), k_prev;
				int tk, tk_prev;
				Point2D pt_k = new Point2D();
				Point2D pt_k_prev = new Point2D();

				while (k_min != m_tree_hull.getPrev(k_max)) {// binary search to find k such that t0->tp->tj holds (i.e. clockwise) for j >= k. Hence, tj->tp->t0 is clockwise (or degenerate) for j < k.
					tk = m_tree_hull.getElement(k);
					m_call_back.getXY(tk, pt_k);
					int orient_k_p_0 = Point2D.orientationRobust(pt_k, pt_pivot, pt_0);

					if (isCounterClockwise_(orient_k_p_0)) {
						k_max = k;
						k = m_tree_hull.getLeft(k);
					} else {
						k_min = k;
						k = m_tree_hull.getRight(k);
					}
				}

				k = k_max;
				k_prev = k_min;
				tk = m_tree_hull.getElement(k);
				tk_prev = m_tree_hull.getElement(k_prev);
				m_call_back.getXY(tk, pt_k);
				m_call_back.getXY(tk_prev, pt_k_prev);
				assert (isCounterClockwise_(Point2D.orientationRobust(pt_k, pt_pivot, pt_0)) && !isCounterClockwise_(Point2D.orientationRobust(pt_k_prev, pt_pivot, pt_0)));
				assert (k_prev != first || isCounterClockwise_(Point2D.orientationRobust(pt_k, pt_pivot, pt_0)));

				if (k_prev != first) {
					int orient_k_prev_p_k = Point2D.orientationRobust(pt_k_prev, pt_pivot, pt_k);

					if (!isClockwise_(orient_k_prev_p_k))
						continue; // pt_pivot is inside the hull (or on the boundary)
				}

				p = m_tree_hull.addElementAtPosition(k_prev, k, -2, true, false, -1); // set place holder to -2 for case 2.
				treeHullWalkForward_(pt_pivot, k, last);
				treeHullWalkBackward_(pt_pivot, k_prev, first);

				continue;
			}

			assert (isDegenerate_(orient_m_p_0));
			{// Case 3: degenerate
				int between = isBetween_(pt_pivot, pt_m, pt_0);

				if (between == -1) {
					int l = m_tree_hull.getPrev(last);
					m_tree_hull.deleteNode(last, -1);
					p = m_tree_hull.addBiggestElement(-3, -1); // set place holder to -3 for case 3.
					treeHullWalkBackward_(pt_pivot, l, first);
				} else if (between == 1) {
					int j = m_tree_hull.getNext(first);
					m_tree_hull.deleteNode(first, -1);
					p = m_tree_hull.addElementAtPosition(-1, j, -3, true, false, -1); // set place holder to -3 for case 3.
					treeHullWalkForward_(pt_pivot, j, last);
				}

				continue;
			}

		} while (false);

		return p;
	}

	private int treeHullWalkForward_(Point2D pt_pivot, int start, int end) {
		if (start == end)
			return end;

		int j = start;
		int tj = m_tree_hull.getElement(j);
		int j_next = m_tree_hull.getNext(j);
		Point2D pt_j = new Point2D();
		Point2D pt_j_next = new Point2D();

		m_call_back.getXY(tj, pt_j);

		while (j != end && m_tree_hull.size(-1) > 2) {//Stops when we find a clockwise triple containting the pivot point, or when the tree_hull size is 2. Deletes non-clockwise triples along the way.
			int tj_next = m_tree_hull.getElement(j_next);
			m_call_back.getXY(tj_next, pt_j_next);

			int orient_j_next_p_j = Point2D.orientationRobust(pt_j_next, pt_pivot, pt_j);

			if (isClockwise_(orient_j_next_p_j))
				break;

			int ccw = j;

			j = j_next;
			pt_j.setCoords(pt_j_next);
			j_next = m_tree_hull.getNext(j);
			m_call_back.deleteNode(ccw);
		}

		return j;
	}

	private int treeHullWalkBackward_(Point2D pt_pivot, int start, int end) {
		if (start == end)
			return end;

		int l = start;
		int tl = m_tree_hull.getElement(l);
		int l_prev = m_tree_hull.getPrev(l);
		Point2D pt_l = new Point2D();
		Point2D pt_l_prev = new Point2D();

		m_call_back.getXY(tl, pt_l);

		while (l != end && m_tree_hull.size(-1) > 2) {//Stops when we find a clockwise triple containting the pivot point, or when the tree_hull size is 2. Deletes non-clockwise triples along the way.
			int tl_prev = m_tree_hull.getElement(l_prev);
			m_call_back.getXY(tl_prev, pt_l_prev);

			int orient_l_p_l_prev = Point2D.orientationRobust(pt_l, pt_pivot, pt_l_prev);

			if (isClockwise_(orient_l_p_l_prev))
				break;

			int ccw = l;

			l = l_prev;
			pt_l.setCoords(pt_l_prev);
			l_prev = m_tree_hull.getPrev(l);
			m_call_back.deleteNode(ccw);
		}

		return l;
	}

	// Orientation predicates
	private static ECoordinate determinant_(Point2D p, Point2D q, Point2D r) {
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
		return det_ec;
	}

	private static boolean isClockwise_(double det) {
		return det < 0.0;
	}

	private static boolean isCounterClockwise_(double det) {
		return det > 0.0;
	}

	private static boolean isDegenerate_(double det) {
		return det == 0.0;
	}

	private static boolean isClockwise_(int orientation) {
		return orientation < 0.0;
	}

	private static boolean isCounterClockwise_(int orientation) {
		return orientation > 0.0;
	}

	private static boolean isDegenerate_(int orientation) {
		return orientation == 0.0;
	}

	private static int isBetween_(Point2D pt_pivot, Point2D pt_m, Point2D pt_0) {
		int ordinate = -1;

		if (pt_m.y == pt_0.y) {
			ordinate = 0;
		} else if (pt_m.x == pt_0.x) {
			ordinate = 1;
		} else {// use bigger ordinate, but shouldn't matter

			double diff_x = Math.abs(pt_m.x - pt_0.x);
			double diff_y = Math.abs(pt_m.y - pt_0.y);

			if (diff_x >= diff_y)
				ordinate = 0;
			else
				ordinate = 1;
		}

		int res = -1;

		if (ordinate == 0) {
			assert (pt_m.x != pt_0.x);

			if (pt_m.x < pt_0.x) {
				if (pt_pivot.x < pt_m.x)
					res = -1;
				else if (pt_0.x < pt_pivot.x)
					res = 1;
				else
					res = 0;
			} else {
				assert (pt_0.x < pt_m.x);

				if (pt_m.x < pt_pivot.x)
					res = -1;
				else if (pt_pivot.x < pt_0.x)
					res = 1;
				else
					res = 0;
			}
		} else {
			assert (pt_m.y != pt_0.y);

			if (pt_m.y < pt_0.y) {
				if (pt_pivot.y < pt_m.y)
					res = -1;
				else if (pt_0.y < pt_pivot.y)
					res = 1;
				else
					res = 0;
			} else {
				assert (pt_0.y < pt_m.y);

				if (pt_m.y < pt_pivot.y)
					res = -1;
				else if (pt_pivot.y < pt_0.y)
					res = 1;
				else
					res = 0;
			}
		}

		return res;
	}

	private static abstract class CallBack {
		abstract void getXY(int ti, Point2D pt);

		abstract void deleteNode(int i);
	}

	private static final class CallBackShape extends CallBack {
		private ConvexHull m_convex_hull;

		CallBackShape(ConvexHull convex_hull) {
			m_convex_hull = convex_hull;
		}

		@Override
		void getXY(int ti, Point2D pt) {
			m_convex_hull.m_shape.getXY(ti, pt);
		}

		@Override
		void deleteNode(int i) {
			int ti = m_convex_hull.m_tree_hull.getElement(i);
			m_convex_hull.m_tree_hull.deleteNode(i, -1);
			m_convex_hull.m_shape.removeVertex(ti, false);
		}
	}

	private static final class CallBackStream extends CallBack {
		private ConvexHull m_convex_hull;

		CallBackStream(ConvexHull convex_hull) {
			m_convex_hull = convex_hull;
		}

		@Override
		void getXY(int ti, Point2D pt) {
			m_convex_hull.m_stream.read(ti << 1, pt);
		}

		@Override
		void deleteNode(int i) {
			m_convex_hull.m_tree_hull.deleteNode(i, -1);
		}
	}

	private static final class CallBackPoints extends CallBack {
		private ConvexHull m_convex_hull;

		CallBackPoints(ConvexHull convex_hull) {
			m_convex_hull = convex_hull;
		}

		@Override
		void getXY(int ti, Point2D pt) {
			pt.setCoords(m_convex_hull.m_points[ti]);
		}

		@Override
		void deleteNode(int i) {
			m_convex_hull.m_tree_hull.deleteNode(i, -1);
		}
	}

	// Members
	private Treap m_tree_hull;
	private EditShape m_shape;
	private AttributeStreamOfDbl m_stream;
	private Point2D[] m_points;
	private int m_geometry_handle;
	private int m_path_handle;
	private Line m_line;
	private CallBack m_call_back;
}
