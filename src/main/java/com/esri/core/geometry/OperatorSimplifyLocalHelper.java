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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.esri.core.geometry.MultiVertexGeometryImpl.GeometryXSimple;

class OperatorSimplifyLocalHelper {
	private static final class Edge {
		Edge() {
			m_flags = 0;
			// m_segment.createInstance();
		}

		Segment m_segment;
		int m_vertexIndex;
		int m_pathIndex;
		int m_flags;

		void setReversed(boolean bYesNo) {
			m_flags &= (~1);
			m_flags = m_flags | (bYesNo ? 1 : 0);
		}

		// The value returned by GetReversed is interpreted differently in
		// checkSelfIntersections_ and checkValidRingOrientation_
		boolean getReversed() /* const */
		{
			return (m_flags & 1) != 0;
		}

		int getRightSide() /* const */
		{
			return getReversed() ? 0 : 1; // 0 means there should be an
											// emptiness on the right side of
											// the edge, 1 means there is
											// interior
		}
	}

	private final VertexDescription m_description;
	private Geometry m_geometry;
	private SpatialReferenceImpl m_sr;
	private int m_dbgCounter; // debugging counter(for breakpoints)
	private double m_toleranceIsSimple;
	private double m_toleranceSimplify;
	// private double m_toleranceCluster; //cluster tolerance needs to be
	// sqrt(2) times larger than the tolerance of the other simplify processes.
	private int m_knownSimpleResult;
	private int m_attributeCount;

	private ArrayList<Edge> m_edges;
	private AttributeStreamOfInt32 m_FreeEdges;
	private ArrayList<Edge> m_lineEdgesRecycle;
	private AttributeStreamOfInt32 m_newEdges;
	private SegmentIteratorImpl m_recycledSegIter;
	private IndexMultiDCList m_crossOverHelperList;
	private AttributeStreamOfInt32 m_paths_for_OGC_tests;

	private ProgressTracker m_progressTracker;

	private Treap m_AET;
	private AttributeStreamOfInt32 m_xyToNode1; // for each vertex, contains -1,
												// or the edge node.
	private AttributeStreamOfInt32 m_xyToNode2; // for each vertex, contains -1,
												// or the edge node.
	private AttributeStreamOfInt32 m_pathOrientations; // 0 if undefined, -1 for
														// counterclockwise, 1
														// for clockwise.
	private AttributeStreamOfInt32 m_pathParentage;
	private int m_unknownOrientationPathCount;
	private double m_yScanline;

	private AttributeStreamOfDbl m_xy;
	private AttributeStreamOfInt32 m_pairs;
	private AttributeStreamOfInt32 m_pairIndices;

	private EditShape m_editShape;
	private boolean m_bOGCRestrictions;
	private boolean m_bPlanarSimplify;

	private int isSimplePlanarImpl_() {
		m_bPlanarSimplify = true;
		if (Geometry.isMultiPath(m_geometry.getType().value())) {
			if (!checkStructure_()) // check structure of geometry(no zero
									// length paths, etc)
				return 0;

			if (!checkDegenerateSegments_(false)) // check for degenerate
													// segments(only 2D,no zs or
													// other attributes)
				return 0;
		}

		if (!checkClustering_()) // check clustering(points are either
									// coincident,or further than tolerance)
			return 0;

		if (!Geometry.isMultiPath(m_geometry.getType().value()))
			return 2; // multipoint is simple

		if (!checkCracking_()) // check that there are no self intersections and
								// overlaps among segments.
			return 0;

		if (m_geometry.getType() == Geometry.Type.Polyline) {
			if (!checkSelfIntersectionsPolylinePlanar_())
				return 0;

			return 2; // polyline is simple
		}

		if (!checkSelfIntersections_()) // check that there are no other self
										// intersections (for the cases of
										// several segments connect in a point)
			return 0;

		// check that every hole is counterclockwise, and every exterior is
		// clockwise.
		// for the strong simple also check that exterior rings are followed by
		// the interior rings.
		return checkValidRingOrientation_();
	}

	private boolean testToleranceDistance_(int xyindex1, int xyindex2) {
		double x1 = m_xy.read(2 * xyindex1);
		double y1 = m_xy.read(2 * xyindex1 + 1);
		double x2 = m_xy.read(2 * xyindex2);
		double y2 = m_xy.read(2 * xyindex2 + 1);
		boolean b = !Clusterer.isClusterCandidate_(x1, y1, x2, y2,
				m_toleranceIsSimple * m_toleranceIsSimple);
		if (!b) {
			if (m_geometry.getDimension() == 0)
				return false;

			return (x1 == x2 && y1 == y2); // points either coincide or
											// further,than the tolerance
		}

		return b;
	}

	private boolean checkStructure_() {
		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();
		int minsize = multiPathImpl.m_bPolygon ? 3 : 2;
		for (int ipath = 0, npath = multiPathImpl.getPathCount(); ipath < npath; ipath++) {
			if (multiPathImpl.getPathSize(ipath) < minsize) {
				m_nonSimpleResult = new NonSimpleResult(
						NonSimpleResult.Reason.Structure, ipath, 0);
				return false;
			}
		}

		return true;
	}

	private boolean checkDegenerateSegments_(boolean bTestZs) {
		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();
		SegmentIteratorImpl segIter = multiPathImpl.querySegmentIterator();
		// Envelope2D env2D;
		boolean bHasZ = multiPathImpl
				.hasAttribute(VertexDescription.Semantics.Z);
		double ztolerance = !bHasZ ? 0 : InternalUtils
				.calculateZToleranceFromGeometry(m_sr, multiPathImpl, false);
		while (segIter.nextPath()) {
			while (segIter.hasNextSegment()) {
				/* const */Segment seg = segIter.nextSegment();
				double length = seg.calculateLength2D();
				if (length > m_toleranceIsSimple)
					continue;

				if (bTestZs && bHasZ) {
					double z0 = seg.getStartAttributeAsDbl(
							VertexDescription.Semantics.Z, 0);
					double z1 = seg.getStartAttributeAsDbl(
							VertexDescription.Semantics.Z, 0);
					if (Math.abs(z1 - z0) > ztolerance)
						continue;
				}

				m_nonSimpleResult = new NonSimpleResult(
						NonSimpleResult.Reason.DegenerateSegments,
						segIter.getStartPointIndex(), -1);
				return false;
			}
		}

		return true;
	}

	private boolean checkClustering_() {
		MultiVertexGeometryImpl multiVertexImpl = (MultiVertexGeometryImpl) m_geometry
				._getImpl();

		MultiPathImpl multiPathImpl = null;
		if (Geometry.isMultiPath(m_geometry.getType().value()))
			multiPathImpl = (MultiPathImpl) m_geometry._getImpl();

		boolean get_paths = (m_bPlanarSimplify || m_bOGCRestrictions)
				&& multiPathImpl != null;

		int pointCount = multiVertexImpl.getPointCount();
		m_xy = (AttributeStreamOfDbl) multiVertexImpl
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		m_pairs = new AttributeStreamOfInt32(0);
		m_pairs.reserve(pointCount * 2);
		m_pairIndices = new AttributeStreamOfInt32(0);
		m_pairIndices.reserve(pointCount * 2);
		if (get_paths) {
			if (m_paths_for_OGC_tests == null)
				m_paths_for_OGC_tests = new AttributeStreamOfInt32(0);
			m_paths_for_OGC_tests.reserve(pointCount);
		}
		int ipath = 0;
		for (int i = 0; i < pointCount; i++) {
			m_pairs.add(2 * i); // y - tol(BOTTOM)
			m_pairs.add(2 * i + 1); // y + tol(TOP)
			m_pairIndices.add(2 * i);
			m_pairIndices.add(2 * i + 1);
			if (get_paths) {
				while (i >= multiPathImpl.getPathEnd(ipath))
					ipath++;

				m_paths_for_OGC_tests.add(ipath);
			}

		}

		BucketSort sorter = new BucketSort();
		sorter.sort(m_pairIndices, 0, 2 * pointCount, new IndexSorter(this,
				get_paths));

		m_AET.clear();
		m_AET.setComparator(new ClusterTestComparator(this));
		m_AET.setCapacity(pointCount);
		for (int index = 0, n = pointCount * 2; index < n; index++) {
			int pairIndex = m_pairIndices.get(index);
			int pair = m_pairs.get(pairIndex);
			int xyindex = pair >> 1; // k = 2n or 2n + 1 represent a vertical
										// segment for the same vertex.
										// Therefore, k / 2 represents a vertex
										// index
			// Points need to be either exactly equal or further than 2 *
			// tolerance apart.
			if ((pair & 1) == 0) {// bottom element
				int aetNode = m_AET.addElement(xyindex, -1);
				// add it to the AET,end test it against its left and right
				// neighbours.
				int leftneighbour = m_AET.getPrev(aetNode);
				if (leftneighbour != Treap.nullNode()
						&& !testToleranceDistance_(
								m_AET.getElement(leftneighbour), xyindex)) {
					m_nonSimpleResult = new NonSimpleResult(
							NonSimpleResult.Reason.Clustering, xyindex,
							m_AET.getElement(leftneighbour));
					return false;
				}
				int rightneighbour = m_AET.getNext(aetNode);
				if (rightneighbour != Treap.nullNode()
						&& !testToleranceDistance_(
								m_AET.getElement(rightneighbour), xyindex)) {
					m_nonSimpleResult = new NonSimpleResult(
							NonSimpleResult.Reason.Clustering, xyindex,
							m_AET.getElement(rightneighbour));
					return false;
				}
			} else { // top
						// get left and right neighbours, and remove the element
						// from AET. Then test the neighbours with the
						// tolerance.
				int aetNode = m_AET.search(xyindex, -1);
				int leftneighbour = m_AET.getPrev(aetNode);
				int rightneighbour = m_AET.getNext(aetNode);
				m_AET.deleteNode(aetNode, -1);
				if (leftneighbour != Treap.nullNode()
						&& rightneighbour != Treap.nullNode()
						&& !testToleranceDistance_(
								m_AET.getElement(leftneighbour),
								m_AET.getElement(rightneighbour))) {
					m_nonSimpleResult = new NonSimpleResult(
							NonSimpleResult.Reason.Clustering,
							m_AET.getElement(leftneighbour),
							m_AET.getElement(rightneighbour));
					return false;
				}
			}
		}

		return true;
	}

	private boolean checkCracking_() {
		MultiVertexGeometryImpl multiVertexImpl = (MultiVertexGeometryImpl) m_geometry
				._getImpl();
		int pointCount = multiVertexImpl.getPointCount();
		if (pointCount < 10)// use brute force for smaller polygons
		{
			return checkCrackingBrute_();
		} else {
			return checkCrackingPlanesweep_();
		}
	}

	private boolean checkCrackingPlanesweep_() // cracker,that uses planesweep
												// algorithm.
	{
		EditShape editShape = new EditShape();
		editShape.addGeometry(m_geometry);
		NonSimpleResult result = new NonSimpleResult();
		boolean bNonSimple = Cracker.needsCracking(false, editShape,
				m_toleranceIsSimple, result, m_progressTracker);
		if (bNonSimple) {
			result.m_vertexIndex1 = editShape
					.getVertexIndex(result.m_vertexIndex1);
			result.m_vertexIndex2 = editShape
					.getVertexIndex(result.m_vertexIndex2);
			m_nonSimpleResult.Assign(result);
			return false;
		} else
			return true;
	}

	private boolean checkCrackingBrute_() // cracker, that uses brute force (a
											// double loop) to find segment
											// intersections.
	{
		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();
		// Implementation without a QuadTreeImpl accelerator
		SegmentIteratorImpl segIter1 = multiPathImpl.querySegmentIterator();
		SegmentIteratorImpl segIter2 = multiPathImpl.querySegmentIterator();
		// Envelope2D env2D;
		while (segIter1.nextPath()) {
			while (segIter1.hasNextSegment()) {
				/* const */Segment seg1 = segIter1.nextSegment();
				if (!segIter1.isLastSegmentInPath() || !segIter1.isLastPath()) {
					segIter2.resetTo(segIter1);
					do {
						while (segIter2.hasNextSegment()) {
							/* const */Segment seg2 = segIter2.nextSegment();
							int res = seg1._isIntersecting(seg2,
									m_toleranceIsSimple, true);
							if (res != 0) {
								NonSimpleResult.Reason reason = res == 2 ? NonSimpleResult.Reason.CrossOver
										: NonSimpleResult.Reason.Cracking;
								m_nonSimpleResult = new NonSimpleResult(reason,
										segIter1.getStartPointIndex(),
										segIter2.getStartPointIndex());
								return false;
							}
						}
					} while (segIter2.nextPath());
				}
			}
		}
		return true;
	}

	private boolean checkSelfIntersections_() {
		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();
		m_edges.clear();
		m_edges.ensureCapacity(20);// we reuse the edges while going through a
		// polygon.
		m_lineEdgesRecycle.clear();
		m_lineEdgesRecycle.ensureCapacity(20);// we reuse the edges while going
												// through a polygon.

		m_recycledSegIter = multiPathImpl.querySegmentIterator();
		m_recycledSegIter.setCirculator(true);

		AttributeStreamOfInt32 bunch = new AttributeStreamOfInt32(0);// stores
																		// coincident
																		// vertices
		bunch.reserve(10);
		int pointCount = multiPathImpl.getPointCount();
		double xprev = NumberUtils.TheNaN;
		double yprev = 0;
		// We already have a sorted list of vertices from clustering check.
		for (int index = 0, n = pointCount * 2; index < n; index++) {
			int pairIndex = m_pairIndices.get(index);
			int pair = m_pairs.get(pairIndex);
			if ((pair & 1) != 0)
				continue; // m_pairs array is redundant. See checkClustering_.

			int xyindex = pair >> 1;

			double x = m_xy.read(2 * xyindex);
			double y = m_xy.read(2 * xyindex + 1);
			if (bunch.size() != 0) {
				if (x != xprev || y != yprev) {
					if (!processBunchForSelfIntersectionTest_(bunch))
						return false;
					if (bunch != null)
						bunch.clear(false);
				}
			}

			bunch.add(xyindex);
			xprev = x;
			yprev = y;
		}

		assert (bunch.size() > 0);// cannot be empty

		if (!processBunchForSelfIntersectionTest_(bunch))
			return false;

		return true;
	}

	static final class Vertex_info {
		double x, y;
		int ipath;
		int ivertex;
		boolean boundary;
	};

	static final class Vertex_info_pl {
		double x;
		double y;
		int ipath;
		int ivertex;
		boolean boundary;
		boolean end_point;
	};

	boolean checkSelfIntersectionsPolylinePlanar_() {
		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();

		boolean closedPaths[] = new boolean[multiPathImpl.getPathCount()];
		for (int ipath = 0, npaths = multiPathImpl.getPathCount(); ipath < npaths; ipath++) {
			closedPaths[ipath] = multiPathImpl.isClosedPathInXYPlane(ipath);
		}

		Vertex_info_pl vi_prev = new Vertex_info_pl();
		boolean is_closed_path;
		int path_start;
		int path_last;

		Point2D pt = new Point2D();

		{// scope
			int pairIndex = m_pairIndices.get(0);
			int pair = m_pairs.get(pairIndex);
			int xyindex = pair >> 1;
			m_xy.read(2 * xyindex, pt);
			int ipath = m_paths_for_OGC_tests.get(xyindex);
			is_closed_path = closedPaths[ipath];
			path_start = multiPathImpl.getPathStart(ipath);
			path_last = multiPathImpl.getPathEnd(ipath) - 1;
			vi_prev.end_point = (xyindex == path_start)
					|| (xyindex == path_last);
			if (m_bOGCRestrictions)
				vi_prev.boundary = !is_closed_path && vi_prev.end_point;
			else
				// for regular planar simplify, only the end points are allowed
				// to coincide
				vi_prev.boundary = vi_prev.end_point;
			vi_prev.ipath = ipath;
			vi_prev.x = pt.x;
			vi_prev.y = pt.y;
			vi_prev.ivertex = xyindex;
		}

		Vertex_info_pl vi = new Vertex_info_pl();

		for (int index = 1, n = m_pairIndices.size(); index < n; index++) {
			int pairIndex = m_pairIndices.get(index);
			int pair = m_pairs.get(pairIndex);
			if ((pair & 1) != 0)
				continue;

			int xyindex = pair >> 1;
			m_xy.read(2 * xyindex, pt);
			int ipath = m_paths_for_OGC_tests.get(xyindex);
			if (ipath != vi_prev.ipath) {
				is_closed_path = closedPaths[ipath];
				path_start = multiPathImpl.getPathStart(ipath);
				path_last = multiPathImpl.getPathEnd(ipath) - 1;
			}
			boolean boundary;
			boolean end_point = (xyindex == path_start)
					|| (xyindex == path_last);
			if (m_bOGCRestrictions)
				boundary = !is_closed_path && vi_prev.end_point;
			else
				// for regular planar simplify, only the end points are allowed
				// to coincide
				boundary = vi_prev.end_point;

			vi.x = pt.x;
			vi.y = pt.y;
			vi.ipath = ipath;
			vi.ivertex = xyindex;
			vi.boundary = boundary;
			vi.end_point = end_point;

			if (vi.x == vi_prev.x && vi.y == vi_prev.y) {
				if (m_bOGCRestrictions) {
					if (!vi.boundary || !vi_prev.boundary) {
						if ((vi.ipath != vi_prev.ipath)
								|| (!vi.end_point && !vi_prev.end_point))// check
																			// that
																			// this
																			// is
																			// not
																			// the
																			// endpoints
																			// of
																			// a
																			// closed
																			// path
						{
							// one of coincident vertices is not on the boundary
							// this is either Non_simple_result::cross_over or
							// Non_simple_result::ogc_self_tangency.
							// too expensive to distinguish between the two.
							m_nonSimpleResult = new NonSimpleResult(
									NonSimpleResult.Reason.OGCPolylineSelfTangency,
									vi.ivertex, vi_prev.ivertex);
							return false;// common point not on the boundary
						}
					}
				} else {
					if (!vi.end_point || !vi_prev.end_point) {// one of
																// coincident
																// vertices is
																// not an
																// endpoint
						m_nonSimpleResult = new NonSimpleResult(
								NonSimpleResult.Reason.CrossOver, vi.ivertex,
								vi_prev.ivertex);
						return false;// common point not on the boundary
					}
				}
			}

			Vertex_info_pl tmp = vi_prev;
			vi_prev = vi;
			vi = tmp;
		}

		return true;
	}

	final static class Vertex_info_pg {
		double x;
		double y;
		int ipath;
		int ivertex;
		int ipolygon;

		Vertex_info_pg(double x_, double y_, int ipath_, int xyindex_,
				int polygon_) {
			x = x_;
			y = y_;
			ipath = ipath_;
			ivertex = xyindex_;
			ipolygon = polygon_;
		}

		boolean is_equal(Vertex_info_pg other) {
			return x == other.x && y == other.y && ipath == other.ipath
					&& ivertex == other.ivertex && ipolygon == other.ipolygon;
		}
	};

	boolean check_self_intersections_polygons_OGC_() {
		MultiPathImpl multiPathImpl = (MultiPathImpl) (m_geometry._getImpl());
		// OGC MultiPolygon is simple when each Polygon is simple and Polygons a
		// allowed only touch at finite number of vertices.
		// OGC Polygon is simple if it consist of simple LinearRings.
		// LinearRings cannot cross.
		// Any two LinearRings of a OGC Polygon are allowed to touch at single
		// vertex only.
		// The OGC Polygon interior has to be a connected set.

		// At this point we assume that the ring order has to be correct (holes
		// follow corresponding exterior ring).
		// No Rings cross. Exterior rings can only touch at finite number of
		// vertices.

		// Fill a mapping of ring to
		int[] ring_to_polygon = new int[multiPathImpl.getPathCount()];
		int exteriors = -1;
		boolean has_holes = false;
		for (int ipath = 0, n = multiPathImpl.getPathCount(); ipath < n; ipath++) {
			if (multiPathImpl.isExteriorRing(ipath)) {
				has_holes = false;
				exteriors++;
				if (ipath < n - 1) {
					if (!multiPathImpl.isExteriorRing(ipath + 1))
						has_holes = true;
				}
			}

			// For OGC polygons with no holes, store -1.
			// For polygons with holes, store polygon index for each ring.
			ring_to_polygon[ipath] = has_holes ? exteriors : -1;
		}

		// Use already sorted m_pairIndices
		Vertex_info_pg vi_prev = null;
		Point2D pt = new Point2D();
		{// scope
			int pairIndex = m_pairIndices.get(0);
			int pair = m_pairs.get(pairIndex);
			int xyindex = pair >> 1;
			m_xy.read(2 * xyindex, pt);
			int ipath = m_paths_for_OGC_tests.get(xyindex);
			vi_prev = new Vertex_info_pg(pt.x, pt.y, ipath, xyindex,
					ring_to_polygon[ipath]);
		}

		ArrayList<Vertex_info_pg> intersections = new ArrayList<Vertex_info_pg>(
				multiPathImpl.getPathCount() * 2);
		for (int index = 1, n = m_pairIndices.size(); index < n; index++) {
			int pairIndex = m_pairIndices.get(index);
			int pair = m_pairs.get(pairIndex);
			if ((pair & 1) != 0)
				continue;
			int xyindex = pair >> 1;
			m_xy.read(2 * xyindex, pt);
			int ipath = m_paths_for_OGC_tests.get(xyindex);
			Vertex_info_pg vi = new Vertex_info_pg(pt.x, pt.y, ipath, xyindex,
					ring_to_polygon[ipath]);

			if (vi.x == vi_prev.x && vi.y == vi_prev.y) {
				if (vi.ipath == vi_prev.ipath) {// the ring has self tangency
					m_nonSimpleResult = new NonSimpleResult(
							NonSimpleResult.Reason.OGCPolygonSelfTangency,
							vi.ivertex, vi_prev.ivertex);
					return false;
				} else if (ring_to_polygon[vi.ipath] >= 0
						&& ring_to_polygon[vi.ipath] == ring_to_polygon[vi_prev.ipath]) {// only
																							// add
																							// rings
																							// from
																							// polygons
																							// with
																							// holes.
																							// Only
																							// interested
																							// in
																							// touching
																							// rings
																							// that
																							// belong
																							// to
																							// the
																							// same
																							// polygon
					if (intersections.size() == 0
							|| intersections.get(intersections.size() - 1) != vi_prev)
						intersections.add(vi_prev);
					intersections.add(vi);
				}
			}

			vi_prev = vi;
		}

		if (intersections.size() == 0)
			return true;

		// Find disconnected interior cases (OGC spec: Interior of polygon has
		// to be a closed set)

		// Note: Now we'll reuse ring_to_polygon for different purpose - to
		// store mapping from the rings to the graph nodes.

		IndexMultiDCList graph = new IndexMultiDCList(true);
		Arrays.fill(ring_to_polygon, -1);
		int vnode_index = -1;
		Point2D prev = new Point2D();
		prev.setNaN();
		for (int i = 0, n = intersections.size(); i < n; i++) {
			Vertex_info_pg cur = intersections.get(i);
			if (cur.x != prev.x || cur.y != prev.y) {
				vnode_index = graph.createList(0);
				prev.x = cur.x;
				prev.y = cur.y;
			}

			int rnode_index = ring_to_polygon[cur.ipath];
			if (rnode_index == -1) {
				rnode_index = graph.createList(2);
				ring_to_polygon[cur.ipath] = rnode_index;
			}
			graph.addElement(rnode_index, vnode_index); // add to rnode
														// adjacency list the
														// current vnode
			graph.addElement(vnode_index, rnode_index); // add to vnode
														// adjacency list the
														// rnode
		}

		AttributeStreamOfInt32 depth_first_stack = new AttributeStreamOfInt32(0);
		depth_first_stack.reserve(10);

		for (int node = graph.getFirstList(); node != -1; node = graph
				.getNextList(node)) {
			int ncolor = graph.getListData(node);
			if ((ncolor & 1) != 0 || (ncolor & 2) == 0)
				continue;// already visited or this is a vnode (we do not want
							// to start from vnode).

			int bad_rnode = -1;
			depth_first_stack.add(node);
			depth_first_stack.add(-1);// parent
			while (depth_first_stack.size() > 0) {
				int cur_node_parent = depth_first_stack.getLast();
				depth_first_stack.removeLast();
				int cur_node = depth_first_stack.getLast();
				depth_first_stack.removeLast();
				int color = graph.getListData(cur_node);
				if ((color & 1) != 0) {
					// already visited this node. This means we found a loop.
					if ((color & 2) == 0) {// closing on vnode
						bad_rnode = cur_node_parent;
					} else
						bad_rnode = cur_node;

					// assert(bad_rnode != -1);
					break;
				}

				graph.setListData(cur_node, color | 1);
				for (int adjacent_node = graph.getFirst(cur_node); adjacent_node != -1; adjacent_node = graph
						.getNext(adjacent_node)) {
					int adjacent_node_data = graph.getData(adjacent_node);
					if (adjacent_node_data == cur_node_parent)
						continue;// avoid going back to where we just came from
					depth_first_stack.add(adjacent_node_data);
					depth_first_stack.add(cur_node);// push cur_node as parent
													// of adjacent_node
				}
			}

			if (bad_rnode != -1) {
				int bad_ring_index = -1;
				for (int i = 0, n = ring_to_polygon.length; i < n; i++)
					if (ring_to_polygon[i] == bad_rnode) {
						bad_ring_index = i;
						break;
					}

				// bad_ring_index is any ring in a problematic chain of touching
				// rings.
				// When chain of touching rings form a loop, the result is a
				// disconnected interior,
				// which is non-simple for OGC spec.
				m_nonSimpleResult = new NonSimpleResult(
						NonSimpleResult.Reason.OGCDisconnectedInterior,
						bad_ring_index, -1);
				return false;
			}
		}

		return true;
	}

	private int checkValidRingOrientation_() {
		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();
		double totalArea = multiPathImpl.calculateArea2D();
		if (totalArea <= 0) {
			m_nonSimpleResult = new NonSimpleResult(
					NonSimpleResult.Reason.RingOrientation,
					multiPathImpl.getPathCount() == 1 ? 1 : -1, -1);
			return 0;
		}

		if (multiPathImpl.getPathCount() == 1) {// optimization for a single
												// polygon
			if (m_bOGCRestrictions) {
				if (!check_self_intersections_polygons_OGC_())
					return 0;
			}

			return 2;
		}

		// 1.Go through all vertices in the sorted order.
		// 2.For each vertex,insert any non-horizontal segment that has the
		// vertex as low point(there can be max two segments)
		m_pathOrientations = new AttributeStreamOfInt32(
				multiPathImpl.getPathCount(), 0);

		m_pathParentage = new AttributeStreamOfInt32(
				multiPathImpl.getPathCount(), -1);

		int parent_ring = -1;
		double exteriorArea = 0;
		for (int ipath = 0, n = multiPathImpl.getPathCount(); ipath < n; ipath++) {
			double area = multiPathImpl.calculateRingArea2D(ipath);
			m_pathOrientations.write(ipath, area < 0 ? 0 : 256); // 8th bit
																	// is
																	// existing
																	// orientation
			if (area > 0) {
				parent_ring = ipath;
				exteriorArea = area;
			} else if (area == 0) {
				m_nonSimpleResult = new NonSimpleResult(
						NonSimpleResult.Reason.RingOrientation, ipath, -1);
				return 0;
			} else {
				// area < 0: this is a hole.
				// We write the parent exterior
				// ring for it (assumed to be first previous exterior ring)
				if (parent_ring < 0 || exteriorArea < Math.abs(area)) {
					// The first ring is a hole - this is a wrong ring ordering.
					// Or the hole's area is bigger than the previous exterior
					// area - this means ring order is broken,
					// because holes are always smaller. This is not the only
					// condition when ring order is broken though.
					m_nonSimpleResult = new NonSimpleResult(
							NonSimpleResult.Reason.RingOrder, ipath, -1);
					if (m_bOGCRestrictions)
						return 0;
				}
				m_pathParentage.write(ipath, parent_ring);
			}
		}

		m_unknownOrientationPathCount = multiPathImpl.getPathCount();
		m_newEdges = new AttributeStreamOfInt32(0);
		m_newEdges.reserve(10);

		int pointCount = multiPathImpl.getPointCount();
		m_yScanline = NumberUtils.TheNaN;
		AttributeStreamOfInt32 bunch = new AttributeStreamOfInt32(0); // stores
																		// coincident
																		// vertices
		bunch.reserve(10);
		// Each vertex has two edges attached.These two arrays map vertices to
		// edges as nodes in the m_AET
		m_xyToNode1 = new AttributeStreamOfInt32(pointCount, Treap.nullNode());
		m_xyToNode2 = new AttributeStreamOfInt32(pointCount, Treap.nullNode());
		if (m_FreeEdges != null)
			m_FreeEdges.clear(false);
		else
			m_FreeEdges = new AttributeStreamOfInt32(0);
		m_FreeEdges.reserve(10);

		m_AET.clear();
		m_AET.setComparator(new RingOrientationTestComparator(this));
		for (int index = 0, n = pointCount * 2; m_unknownOrientationPathCount > 0
				&& index < n; index++) {
			int pairIndex = m_pairIndices.get(index);
			int pair = m_pairs.get(pairIndex);
			if ((pair & 1) != 0)
				continue;// m_pairs array is redundant.See checkClustering_.

			int xyindex = pair >> 1;
			double y = m_xy.read(2 * xyindex + 1);

			if (y != m_yScanline && bunch.size() != 0) {
				if (!processBunchForRingOrientationTest_(bunch)) {
					// m_nonSimpleResult is set in the
					// processBunchForRingOrientationTest_
					return 0;
				}
				if (bunch != null)
					bunch.clear(false);
			}

			bunch.add(xyindex);// all vertices that have same y are added to the
			// bunch
			m_yScanline = y;
		}

		if (m_unknownOrientationPathCount > 0
				&& !processBunchForRingOrientationTest_(bunch)) {
			// m_nonSimpleResult is set in the
			// processBunchForRingOrientationTest_
			return 0;
		}

		if (m_bOGCRestrictions) {
			if (m_nonSimpleResult.m_reason != NonSimpleResult.Reason.NotDetermined)
				return 0;// cannot proceed with OGC verification if the ring
							// order is broken (cannot decide polygons then).

			if (!check_self_intersections_polygons_OGC_())
				return 0;

			return 2;// everything is good
		} else {
			if (m_nonSimpleResult.m_reason == NonSimpleResult.Reason.NotDetermined)
				return 2;// everything is good

			// weak simple
			return 1;
		}
	}

	private boolean processBunchForSelfIntersectionTest_(
			AttributeStreamOfInt32 bunch) {
		assert (bunch.size() > 0);
		if (bunch.size() == 1)
			return true;

		assert (m_edges.size() == 0);

		// Bunch contains vertices that have exactly same x and y.
		// We populate m_edges array with the edges that originate in the
		// vertices of the bunch.
		for (int i = 0, n = bunch.size(); i < n; i++) {
			int xyindex = bunch.get(i);
			m_recycledSegIter.resetToVertex(xyindex);// the iterator is
														// circular.
			/* const */Segment seg1 = m_recycledSegIter.previousSegment();
			m_edges.add(createEdge_(seg1, xyindex,
					m_recycledSegIter.getPathIndex(), true));
			m_recycledSegIter.nextSegment();// Need to skip one,because of the
											// previousSegment call
			// before (otherwise will get same segment again)
			/* const */Segment seg2 = m_recycledSegIter.nextSegment();
			m_edges.add(createEdge_(seg2, xyindex,
					m_recycledSegIter.getPathIndex(), false));
		}

		assert ((m_edges.size() & 1) == 0); // even size

		// Analyze the bunch edges for self intersections(the edges touch at the
		// end points only at this stage of IsSimple)
		// 1.sort the edges by angle between edge and the unit vector along axis
		// x,using the cross product sign.Precondition:no overlaps occur at this
		// stage.

		Collections.sort(m_edges, new EdgeComparerForSelfIntersection(this));

		// 2.Analyze the bunch.There can be no edges between edges that share
		// same vertex coordinates.
		// We populate a doubly linked list with the edge indices and iterate
		// over this list getting rid of the neighbouring pairs of vertices.
		// The process is similar to peeling an onion.
		// If the list becomes empty,there are no crossovers,otherwise,the
		// geometry has cross-over.
		int list = m_crossOverHelperList.getFirstList();

		if (list == -1)
			list = m_crossOverHelperList.createList(0);

		m_crossOverHelperList.reserveNodes(m_edges.size());

		for (int i = 0, n = m_edges.size(); i < n; i++) {
			m_crossOverHelperList.addElement(list, i);
		}

		// Peel the onion
		boolean bContinue = true;
		int i1 = -1;
		int i2 = -1;
		while (bContinue) {
			bContinue = false;
			int listnode = m_crossOverHelperList.getFirst(list);
			if (listnode == -1)
				break;

			int nextnode = m_crossOverHelperList.getNext(listnode);
			while (nextnode != -1) {
				int edgeindex1 = m_crossOverHelperList.getData(listnode);
				int edgeindex2 = m_crossOverHelperList.getData(nextnode);
				i1 = m_edges.get(edgeindex1).m_vertexIndex;
				i2 = m_edges.get(edgeindex2).m_vertexIndex;
				if (i1 == i2) {
					bContinue = true;
					m_crossOverHelperList.deleteElement(list, listnode);
					listnode = m_crossOverHelperList.getPrev(nextnode);
					nextnode = m_crossOverHelperList.deleteElement(list,
							nextnode);
					if (nextnode == -1 || listnode == -1)
						break;
					else
						continue;
				}
				listnode = nextnode;
				nextnode = m_crossOverHelperList.getNext(listnode);
			}
		}

		int listSize = m_crossOverHelperList.getListSize(list);
		m_crossOverHelperList.clear(list);
		if (listSize > 0) {
			// There is self-intersection here.
			m_nonSimpleResult = new NonSimpleResult(
					NonSimpleResult.Reason.CrossOver, i1, i2);
			return false;
		}

		// Recycle the bunch to save on object creation
		for (int i = 0, n = bunch.size(); i < n; i++) {
			recycleEdge_(m_edges.get(i));
		}

		m_edges.clear();
		return true;
	}

	private boolean processBunchForRingOrientationTest_(
			AttributeStreamOfInt32 bunch) {
		m_dbgCounter++;
		assert (bunch.size() > 0);

		// remove nodes that go out of scope
		for (int i = 0, n = bunch.size(); i < n; i++) {
			int xyindex = bunch.get(i);
			int aetNode = m_xyToNode1.read(xyindex);
			if (aetNode != Treap.nullNode()) {// We found that there is an edge
												// in AET, attached to the
												// xyindex vertex. This edge
												// goes out of scope. Delete it
												// from AET.
				int edgeIndex = m_AET.getElement(aetNode);
				m_FreeEdges.add(edgeIndex);
				m_AET.deleteNode(aetNode, -1);
				recycleEdge_(m_edges.get(edgeIndex));
				m_edges.set(edgeIndex, null);
				m_xyToNode1.write(xyindex, Treap.nullNode());
			}

			aetNode = m_xyToNode2.read(xyindex);
			if (aetNode != Treap.nullNode()) {// We found that there is an edge
												// in AET, attached to the
												// xyindex vertex. This edge
												// goes out of scope. Delete it
												// from AET.
				int edgeIndex = m_AET.getElement(aetNode);
				m_FreeEdges.add(edgeIndex);
				m_AET.deleteNode(aetNode, -1);
				recycleEdge_(m_edges.get(edgeIndex));
				m_edges.set(edgeIndex, null);
				m_xyToNode2.write(xyindex, Treap.nullNode());
			}
		}

		// add new edges to AET
		for (int i = 0, n = bunch.size(); i < n; i++) {
			int xyindex = bunch.get(i);
			m_recycledSegIter.resetToVertex(xyindex);// the iterator is
														// circular.
			Segment seg1 = m_recycledSegIter.previousSegment();// this
																// segment
																// has
																// end
																// point
																// at
																// xyindex
			if (seg1.getStartY() > seg1.getEndY())// do not allow horizontal
			// segments in here
			{
				// get the top vertex index.We use it to determine what segments
				// to get rid of.
				int edgeTopIndex = m_recycledSegIter.getStartPointIndex();
				Edge edge = createEdge_(seg1, xyindex,
						m_recycledSegIter.getPathIndex(), true);
				int edgeIndex;
				if (m_FreeEdges.size() > 0) {
					edgeIndex = m_FreeEdges.getLast();
					m_FreeEdges.removeLast();
					m_edges.set(edgeIndex, edge);
				} else {
					edgeIndex = m_edges.size();
					m_edges.add(edge);
				}

				int aetNode = m_AET.addElement(edgeIndex, -1);
				// Remember AET nodes in the vertex to AET node maps.
				if (m_xyToNode1.read(edgeTopIndex) == Treap.nullNode())
					m_xyToNode1.write(edgeTopIndex, aetNode);
				else {
					assert (m_xyToNode2.read(edgeTopIndex) == Treap.nullNode());
					m_xyToNode2.write(edgeTopIndex, aetNode);
				}

				// If this edge belongs to a path that has not have direction
				// figured out yet,
				// add it to m_newEdges for post processing
				if ((m_pathOrientations.read(m_recycledSegIter.getPathIndex()) & 3) == 0)
					m_newEdges.add(aetNode);
			}

			m_recycledSegIter.nextSegment();// Need to skip one,because of the
			// previousSegment call
			// before(otherwise will get same
			// segment again)
			// seg1 is invalid now
			Segment seg2 = m_recycledSegIter.nextSegment();
			// start has to be lower than end for this one
			if (seg2.getStartY() < seg2.getEndY())// do not allow horizontal
			// segments in here
			{
				// get the top vertex index.We use it to determine what segments
				// to get rid of.
				int edgeTopIndex = m_recycledSegIter.getEndPointIndex();
				Edge edge = createEdge_(seg2, xyindex,
						m_recycledSegIter.getPathIndex(), false);
				int edgeIndex;
				if (m_FreeEdges.size() > 0) {
					edgeIndex = m_FreeEdges.getLast();
					m_FreeEdges.removeLast();
					m_edges.set(edgeIndex, edge);
				} else {
					edgeIndex = m_edges.size();
					m_edges.add(edge);
				}

				int aetNode = m_AET.addElement(edgeIndex, -1);
				if (m_xyToNode1.read(edgeTopIndex) == Treap.nullNode())
					m_xyToNode1.write(edgeTopIndex, aetNode);
				else {
					assert (m_xyToNode2.read(edgeTopIndex) == Treap.nullNode());
					m_xyToNode2.write(edgeTopIndex, aetNode);
				}

				// If this edge belongs to a path that has not have direction
				// figured out yet,
				// add it to m_newEdges for post processing
				if ((m_pathOrientations.read(m_recycledSegIter.getPathIndex()) & 3) == 0)
					m_newEdges.add(aetNode);
			}
		}

		for (int i = 0, n = m_newEdges.size(); i < n
				&& m_unknownOrientationPathCount > 0; i++) {
			int aetNode = m_newEdges.get(i);
			int edgeIndexInitial = m_AET.getElement(aetNode);
			Edge edgeInitial = m_edges.get(edgeIndexInitial);
			int pathIndexInitial = edgeInitial.m_pathIndex;
			int directionInitial = m_pathOrientations.read(pathIndexInitial);
			if ((directionInitial & 3) == 0) {
				int prevExteriorPath = -1;
				int node = m_AET.getPrev(aetNode);
				int prevNode = aetNode;
				int oddEven = 0;
				{// scope
					int edgeIndex = -1;
					Edge edge = null;
					int pathIndex = -1;
					int dir = 0;
					// find the leftmost edge for which the ring orientation is
					// known
					while (node != Treap.nullNode()) {
						edgeIndex = m_AET.getElement(node);
						edge = m_edges.get(edgeIndex);
						pathIndex = edge.m_pathIndex;
						dir = m_pathOrientations.read(pathIndex);
						if ((dir & 3) != 0)
							break;

						prevNode = node;
						node = m_AET.getPrev(node);
					}

					if (node == Treap.nullNode()) {// if no edges have ring
													// orientation known, then
													// start
													// from the left most and it
													// has
													// to be exterior ring.
						oddEven = 1;
						node = prevNode;
					} else {
						if ((dir & 3) == 1) {
							prevExteriorPath = pathIndex;
						} else {
							prevExteriorPath = m_pathParentage.read(pathIndex);
						}

						oddEven = (edge.getRightSide() != 0) ? 0 : 1;
						node = m_AET.getNext(node);
					}
				}

				do {
					int edgeIndex = m_AET.getElement(node);
					Edge edge = m_edges.get(edgeIndex);
					int pathIndex = edge.m_pathIndex;
					int direction = m_pathOrientations.read(pathIndex);
					if ((direction & 3) == 0) {
						if (oddEven != edge.getRightSide()) {
							m_nonSimpleResult = new NonSimpleResult(
									NonSimpleResult.Reason.RingOrientation,
									pathIndex, -1);
							return false;// wrong ring orientation
						}

						int dir = (oddEven != 0 && !edge.getReversed()) ? 1 : 2;
						direction = (direction & 0xfc) | dir;
						m_pathOrientations.write(pathIndex, dir);
						if (dir == 2
								&& m_nonSimpleResult.m_reason == NonSimpleResult.Reason.NotDetermined) {
							// check that this hole has a correct parent
							// exterior ring.
							int parent = m_pathParentage.read(pathIndex);
							if (parent != prevExteriorPath) {
								m_nonSimpleResult = new NonSimpleResult(
										NonSimpleResult.Reason.RingOrder,
										pathIndex, -1);
								if (m_bOGCRestrictions)
									return false;
							}
						}

						m_unknownOrientationPathCount--;
						if (m_unknownOrientationPathCount == 0)// if(!m_unknownOrientationPathCount)
							return true;
					}

					if ((direction & 3) == 1) {
						prevExteriorPath = pathIndex;
					}

					prevNode = node;
					node = m_AET.getNext(node);
					oddEven = oddEven != 0 ? 0 : 1;
				} while (prevNode != aetNode);
			}
		}

		if (m_newEdges != null)
			m_newEdges.clear(false);
		else
			m_newEdges = new AttributeStreamOfInt32(0);
		return true;
	}

	private Edge createEdge_(/* const */Segment seg, int xyindex, int pathIndex,
			boolean bReversed) {
		Edge edge;
		Geometry.Type gt = seg.getType();
		if (gt == Geometry.Type.Line) {
			edge = createEdgeLine_(seg);
		} else {
			throw GeometryException.GeometryInternalError(); // implement
															// recycling for
															// curves
		}
		edge.m_vertexIndex = xyindex;
		edge.m_pathIndex = pathIndex;
		edge.m_flags = 0;
		edge.setReversed(bReversed);

		return edge;
	}

	private Edge createEdgeLine_(/* const */Segment seg) {
		Edge edge = null;
		if (m_lineEdgesRecycle.size() > 0) {
			int indexLast = m_lineEdgesRecycle.size() - 1;
			edge = m_lineEdgesRecycle.get(indexLast);
			m_lineEdgesRecycle.remove(indexLast);
			seg.copyTo(edge.m_segment);
		} else {
			edge = new Edge();
			edge.m_segment = (Segment) Segment._clone(seg);
		}

		return edge;
	}

	private void recycleEdge_(/* const */Edge edge) {
		Geometry.Type gt = edge.m_segment.getType();
		if (gt == Geometry.Type.Line) {
			m_lineEdgesRecycle.add(edge);
		}
	}

	private static final class ClusterTestComparator extends Treap.Comparator {
		OperatorSimplifyLocalHelper m_helper;

		ClusterTestComparator(OperatorSimplifyLocalHelper helper) {
			m_helper = helper;
		}

		@Override
		int compare(/* const */Treap treap, int xy1, int node) {
			int xy2 = treap.getElement(node);
			double x1 = m_helper.m_xy.read(2 * xy1);
			double x2 = m_helper.m_xy.read(2 * xy2);
			double dx = x1 - x2;
			return dx < 0 ? -1 : (dx > 0 ? 1 : 0);
		}
	}

	private static final class RingOrientationTestComparator extends
			Treap.Comparator {
		private OperatorSimplifyLocalHelper m_helper;

		RingOrientationTestComparator(OperatorSimplifyLocalHelper helper) {
			m_helper = helper;
		}

		@Override
		int compare(/* const */Treap treap, int left, int node) {
			int right = treap.getElement(node);
			Edge edge1 = m_helper.m_edges.get(left);
			Edge edge2 = m_helper.m_edges.get(right);
			boolean bEdge1Reversed = edge1.getReversed();
			boolean bEdge2Reversed = edge2.getReversed();

			double x1 = edge1.m_segment.intersectionOfYMonotonicWithAxisX(
					m_helper.m_yScanline, 0);
			double x2 = edge2.m_segment.intersectionOfYMonotonicWithAxisX(
					m_helper.m_yScanline, 0);

			if (x1 == x2) {
				// apparently these edges originate from same vertex and the
				// scanline is on the vertex.move scanline a little.
				double y1 = bEdge1Reversed ? edge1.m_segment.getStartY()
						: edge1.m_segment.getEndY();
				double y2 = bEdge2Reversed ? edge2.m_segment.getStartY()
						: edge2.m_segment.getEndY();
				double miny = Math.min(y1, y2);
				double y = (miny - m_helper.m_yScanline) * 0.5
						+ m_helper.m_yScanline;
				if (y == m_helper.m_yScanline) {
					// assert(0); //ST: not a bug. just curious to see this
					// happens.
					y = miny; // apparently, one of the segments is almost
								// horizontal line.
				}
				x1 = edge1.m_segment.intersectionOfYMonotonicWithAxisX(y, 0);
				x2 = edge2.m_segment.intersectionOfYMonotonicWithAxisX(y, 0);
				assert (x1 != x2);
			}

			return x1 < x2 ? -1 : (x1 > x2 ? 1 : 0);
		}
	}

	int multiPointIsSimpleAsFeature_() {
		MultiVertexGeometryImpl multiVertexImpl = (MultiVertexGeometryImpl) m_geometry
				._getImpl();
		// sort lexicographically: by y,then by x, then by other attributes in
		// the order.
		// Go through the sorted list and make sure no points coincide exactly
		// (no tolerance is taken into account).
		int pointCount = multiVertexImpl.getPointCount();

		AttributeStreamOfInt32 indices = new AttributeStreamOfInt32(0);

		for (int i = 0; i < pointCount; i++) {
			indices.add(i);
		}

		indices.Sort(0, pointCount, new MultiPointVertexComparer(this));

		for (int i = 1; i < pointCount; i++) {
			if (compareVerticesMultiPoint_(indices.get(i - 1), indices.get(i)) == 0) {
				m_nonSimpleResult = new NonSimpleResult(
						NonSimpleResult.Reason.Clustering, indices.get(i - 1),
						indices.get(i));
				return 0;// points are coincident-simplify.
			}
		}

		return 2;
	}

	int polylineIsSimpleAsFeature_() {
		if (!checkStructure_())
			return 0;
		// Non planar IsSimple.
		// Go through all line segments and make sure no line segments are
		// degenerate.
		// Degenerate segment is the one which has its length shorter than
		// tolerance or Z projection shorter than z tolerance.
		return checkDegenerateSegments_(true) ? 2 : 0;
	}

	int polygonIsSimpleAsFeature_() {
		return isSimplePlanarImpl_();
	}

	MultiPoint multiPointSimplifyAsFeature_() {
		MultiVertexGeometryImpl multiVertexImpl = (MultiVertexGeometryImpl) m_geometry
				._getImpl();
		// sort lexicographically:by y,then by x,then by other attributes in the
		// order.
		int pointCount = multiVertexImpl.getPointCount();
		assert (pointCount > 0);

		AttributeStreamOfInt32 indices = new AttributeStreamOfInt32(0);

		for (int i = 0; i < pointCount; i++) {
			indices.add(i);
		}

		indices.Sort(0, pointCount, new MultiPointVertexComparer2(this));

		// Mark vertices that are unique
		boolean[] indicesOut = new boolean[pointCount];

		indicesOut[indices.get(0)] = true;

		for (int i = 1; i < pointCount; i++) {
			int ind1 = indices.get(i - 1);
			int ind2 = indices.get(i);
			if (compareVerticesMultiPoint_(ind1, ind2) == 0) {
				indicesOut[ind2] = false;
				continue;
			}

			indicesOut[ind2] = true;
		}

		// get rid of non-unique vertices.
		// We preserve the order of MultiPoint vertices.Among duplicate
		// vertices,those that have
		// higher index are deleted.
		MultiPoint dst = (MultiPoint) m_geometry.createInstance();
		MultiPoint src = (MultiPoint) m_geometry;
		int istart = 0;
		int iend = 1;
		for (int i = 0; i < pointCount; i++) {
			if (indicesOut[i])
				iend = i + 1;
			else {
				if (istart < iend) {
					dst.add(src, istart, iend);
				}

				istart = i + 1;
			}
		}

		if (istart < iend) {
			dst.add(src, istart, iend);
		}

		((MultiVertexGeometryImpl) dst._getImpl()).setIsSimple(
				GeometryXSimple.Strong, m_toleranceSimplify, false);
		return dst;
	}

	Polyline polylineSimplifyAsFeature_() {
		// Non planar simplify.
		// Go through all line segments and make sure no line segments are
		// degenerate.
		// Degenerate segment is the one which has its length shorter than
		// tolerance or Z projection shorter than z tolerance.
		// The algorithm processes each path symmetrically from each end to
		// ensure the result of simplify does not depend on the direction of the
		// path.

		MultiPathImpl multiPathImpl = (MultiPathImpl) m_geometry._getImpl();
		SegmentIteratorImpl segIterFwd = multiPathImpl.querySegmentIterator();
		SegmentIteratorImpl segIterBwd = multiPathImpl.querySegmentIterator();
		Polyline dst = (Polyline) m_geometry.createInstance();
		Polyline src = (Polyline) m_geometry;
		// Envelope2D env2D;
		boolean bHasZ = multiPathImpl
				.hasAttribute(VertexDescription.Semantics.Z);
		double ztolerance = !bHasZ ? 0.0 : InternalUtils
				.calculateZToleranceFromGeometry(m_sr, multiPathImpl, true);
		AttributeStreamOfInt32 fwdStack = new AttributeStreamOfInt32(0);
		AttributeStreamOfInt32 bwdStack = new AttributeStreamOfInt32(0);
		fwdStack.reserve(multiPathImpl.getPointCount() / 2 + 1);
		bwdStack.reserve(multiPathImpl.getPointCount() / 2 + 1);
		while (segIterFwd.nextPath()) {
			segIterBwd.nextPath();
			if (multiPathImpl.getPathSize(segIterFwd.getPathIndex()) < 2)
				continue;

			segIterBwd.resetToLastSegment();
			double lengthFwd = 0;
			double lengthBwd = 0;
			boolean bFirst = true;
			while (segIterFwd.hasNextSegment()) {
				assert (segIterBwd.hasPreviousSegment());

				/* const */Segment segFwd = segIterFwd.nextSegment();
				/* const */Segment segBwd = segIterBwd.previousSegment();

				int idx1 = segIterFwd.getStartPointIndex();
				int idx2 = segIterBwd.getStartPointIndex();
				if (idx1 > idx2)
					break;

				if (bFirst) {
					// add the very first and the very last point indices
					fwdStack.add(segIterFwd.getStartPointIndex());// first goes
																	// to
																	// fwdStack
					bwdStack.add(segIterBwd.getEndPointIndex());// last goes to
																// bwdStack
					bFirst = false;
				}

				{
					int index0 = fwdStack.getLast();
					int index1 = segIterFwd.getEndPointIndex();
					if (index1 - index0 > 1) {
						Point2D pt = new Point2D();
						pt.sub(multiPathImpl.getXY(index0),
								multiPathImpl.getXY(index1));
						lengthFwd = pt.length();
					} else {
						lengthFwd = segFwd.calculateLength2D();
					}
				}

				{
					int index0 = bwdStack.getLast();
					int index1 = segIterBwd.getStartPointIndex();
					if (index1 - index0 > 1) {
						Point2D pt = new Point2D();
						pt.sub(multiPathImpl.getXY(index0),
								multiPathImpl.getXY(index1));
						lengthBwd = pt.length();
					} else {
						lengthBwd = segBwd.calculateLength2D();
					}
				}

				if (lengthFwd > m_toleranceSimplify) {
					fwdStack.add(segIterFwd.getEndPointIndex());
					lengthFwd = 0;
				} else {
					if (bHasZ) {
						double z0 = multiPathImpl.getAttributeAsDbl(
								VertexDescription.Semantics.Z,
								fwdStack.getLast(), 0);
						double z1 = segFwd.getEndAttributeAsDbl(
								VertexDescription.Semantics.Z, 0);
						if (Math.abs(z1 - z0) > ztolerance) {
							fwdStack.add(segIterFwd.getEndPointIndex());
							lengthFwd = 0;
						}
					}
				}

				if (lengthBwd > m_toleranceSimplify) {
					bwdStack.add(segIterBwd.getStartPointIndex());
					lengthBwd = 0;
				} else {
					if (bHasZ) {
						double z0 = multiPathImpl.getAttributeAsDbl(
								VertexDescription.Semantics.Z,
								bwdStack.getLast(), 0);
						double z1 = segBwd.getEndAttributeAsDbl(
								VertexDescription.Semantics.Z, 0);
						if (Math.abs(z1 - z0) > ztolerance) {
							bwdStack.add(segIterBwd.getStartPointIndex());
							lengthBwd = 0;
						}
					}
				}
			}

			// assert(fwdStack.getLast() <= bwdStack.getLast());
			if (fwdStack.getLast() < bwdStack.getLast()) {
				// There is degenerate segment in the middle. Remove.
				// If the path degenerate, this will make fwdStack.size() +
				// bwdStack.size() < 2.
				if (fwdStack.size() > bwdStack.size())
					fwdStack.removeLast();
				else
					bwdStack.removeLast();
			} else if (fwdStack.getLast() == bwdStack.getLast()) {
				bwdStack.removeLast();
			} else {
				assert (fwdStack.getLast() - bwdStack.getLast() == 1);
				bwdStack.removeLast();
				bwdStack.removeLast();
			}

			if (bwdStack.size() + fwdStack.size() >= 2) {
				// Completely ignore the curves for now.
				Point point = new Point();
				for (int i = 0, n = fwdStack.size(); i < n; i++) {
					src.getPointByVal(fwdStack.get(i), point);
					if (i == 0)
						dst.startPath(point);
					else
						dst.lineTo(point);
				}

				// int prevIdx = fwdStack.getLast();
				for (int i = bwdStack.size() - 1; i > 0; i--) {
					src.getPointByVal(bwdStack.get(i), point);
					dst.lineTo(point);
				}

				if (src.isClosedPath(segIterFwd.getPathIndex())) {
					dst.closePathWithLine();
				} else {
					if (bwdStack.size() > 0) {
						src.getPointByVal(bwdStack.get(0), point);
						dst.lineTo(point);
					}
				}
			} else {
				// degenerate path won't be added
			}

			if (fwdStack != null)
				fwdStack.clear(false);
			if (bwdStack != null)
				bwdStack.clear(false);
		}

		((MultiVertexGeometryImpl) dst._getImpl()).setIsSimple(
				GeometryXSimple.Strong, m_toleranceSimplify, false);
		return dst;
	}

	Polygon polygonSimplifyAsFeature_() {
		return (Polygon) simplifyPlanar_();
	}

	MultiVertexGeometry simplifyPlanar_() {
		// do clustering/cracking loop
		// if (false)
		// {
		// ((MultiPathImpl)m_geometry._getImpl()).saveToTextFileDbg("c:/temp/_simplifyDbg0.txt");
		// }

		if (m_geometry.getType() == Geometry.Type.Polygon) {
			if (((Polygon) m_geometry).getFillRule() == Polygon.FillRule.enumFillRuleWinding) {
				// when the fill rule is winding, we need to call a special
				// method.
				return TopologicalOperations.planarSimplify(
						(MultiVertexGeometry) m_geometry, m_toleranceSimplify,
						true, false, m_progressTracker);
			}
		}
		
		m_editShape = new EditShape();
		m_editShape.addGeometry(m_geometry);

		if (m_editShape.getTotalPointCount() != 0) {
			assert (m_knownSimpleResult != GeometryXSimple.Strong);
			if (m_knownSimpleResult != GeometryXSimple.Weak) {
				CrackAndCluster.execute(m_editShape, m_toleranceSimplify,
						m_progressTracker, true);
			}
	
			if (m_geometry.getType().equals(Geometry.Type.Polygon)) {
				Simplificator.execute(m_editShape, m_editShape.getFirstGeometry(),
						m_knownSimpleResult, false, m_progressTracker);
			}
		}
		
		m_geometry = m_editShape.getGeometry(m_editShape.getFirstGeometry()); // extract
																				// the
																				// result
																				// of
																				// simplify

		if (m_geometry.getType().equals(Geometry.Type.Polygon)) {
			((MultiPathImpl)m_geometry._getImpl())._updateOGCFlags();
			((Polygon)m_geometry).setFillRule(Polygon.FillRule.enumFillRuleOddEven);
		}

		// We have simplified the geometry using the given tolerance. Now mark
		// the geometry as strong simple,
		// So that the next call will not have to repeat these steps.

		((MultiVertexGeometryImpl) m_geometry._getImpl()).setIsSimple(
				GeometryXSimple.Strong, m_toleranceSimplify, false);

		return (MultiVertexGeometry) (m_geometry);
	}

	NonSimpleResult m_nonSimpleResult;

	OperatorSimplifyLocalHelper(Geometry geometry,
			SpatialReference spatialReference, int knownSimpleResult,
			ProgressTracker progressTracker, boolean bOGCRestrictions) {

		m_description = geometry.getDescription();
		m_geometry = geometry;
		m_sr = (SpatialReferenceImpl) spatialReference;
		m_dbgCounter = 0;
		m_toleranceIsSimple = InternalUtils.calculateToleranceFromGeometry(
				m_sr, geometry, false);
		m_toleranceSimplify = InternalUtils.calculateToleranceFromGeometry(
				m_sr, geometry, true);
		// m_toleranceCluster = m_toleranceSimplify * Math.sqrt(2.0) * 1.00001;
		m_knownSimpleResult = knownSimpleResult;
		m_attributeCount = m_description.getAttributeCount();
		m_edges = new ArrayList<Edge>();
		m_lineEdgesRecycle = new ArrayList<Edge>();
		m_crossOverHelperList = new IndexMultiDCList();
		m_AET = new Treap();
		m_nonSimpleResult = new NonSimpleResult();
		m_bOGCRestrictions = bOGCRestrictions;
		m_bPlanarSimplify = m_bOGCRestrictions;
	}

	// Returns 0 non-simple, 1 weak simple, 2 strong simple
	/**
	 * The code is executed in the 2D plane only.Attributes are ignored.
	 * MultiPoint-check for clustering. Polyline -check for clustering and
	 * cracking. Polygon -check for clustering,cracking,absence of
	 * self-intersections,and correct ring ordering.
	 */
	static protected int isSimplePlanar(/* const */Geometry geometry, /* const */
	SpatialReference spatialReference, boolean bForce,
			ProgressTracker progressTracker) {
		assert (false); // this code is not called yet.
		if (geometry.isEmpty())
			return 1;
		Geometry.Type gt = geometry.getType();
		if (gt == Geometry.Type.Point)
			return 1;
		else if (gt == Geometry.Type.Envelope) {
			Envelope2D env2D = new Envelope2D();
			geometry.queryEnvelope2D(env2D);
			boolean bReturnValue = !env2D.isDegenerate(InternalUtils
					.calculateToleranceFromGeometry(spatialReference, geometry,
							false));
			return bReturnValue ? 1 : 0;
		} else if (Geometry.isSegment(gt.value())) {
			throw GeometryException.GeometryInternalError();
			// return seg.IsSimple(m_tolerance);
		} else if (!Geometry.isMultiVertex(gt.value())) {
			throw GeometryException.GeometryInternalError();// What else?
		}

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatialReference, geometry, false);

		double geomTolerance = 0;
		int isSimple = ((MultiVertexGeometryImpl) geometry._getImpl())
				.getIsSimple(tolerance);
		int knownSimpleResult = bForce ? -1 : isSimple;
		// TODO: need to distinguish KnownSimple between SimpleAsFeature and
		// SimplePlanar. The SimplePlanar implies SimpleAsFeature.
		if (knownSimpleResult != -1)
			return knownSimpleResult;

		if (knownSimpleResult == GeometryXSimple.Weak) {
			assert (tolerance <= geomTolerance);
			tolerance = geomTolerance;// OVERRIDE the tolerance.
		}

		OperatorSimplifyLocalHelper helper = new OperatorSimplifyLocalHelper(
				geometry, spatialReference, knownSimpleResult, progressTracker,
				false);
		knownSimpleResult = helper.isSimplePlanarImpl_();
		((MultiVertexGeometryImpl) geometry._getImpl()).setIsSimple(
				knownSimpleResult, tolerance, false);
		return knownSimpleResult;
	}

	/**
	 * Checks if Geometry is simple for storing in DB:
	 * 
	 * MultiPoint:check that no points coincide.tolerance is ignored.
	 * Polyline:ensure there no segments degenerate segments. Polygon:Same as
	 * IsSimplePlanar.
	 */
	static protected int isSimpleAsFeature(/* const */Geometry geometry, /* const */
	SpatialReference spatialReference, boolean bForce, NonSimpleResult result,
			ProgressTracker progressTracker) {
		if (result != null) {
			result.m_reason = NonSimpleResult.Reason.NotDetermined;
			result.m_vertexIndex1 = -1;
			result.m_vertexIndex2 = -1;
		}
		if (geometry.isEmpty())
			return 1;
		Geometry.Type gt = geometry.getType();
		if (gt == Geometry.Type.Point)
			return 1;

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatialReference, geometry, false);
		if (gt == Geometry.Type.Envelope) {
			/* const */Envelope env = (Envelope) geometry;
			Envelope2D env2D = new Envelope2D();
			env.queryEnvelope2D(env2D);
			if (env2D.isDegenerate(tolerance)) {
				if (result != null) {
					result.m_reason = NonSimpleResult.Reason.DegenerateSegments;
					result.m_vertexIndex1 = -1;
					result.m_vertexIndex2 = -1;
				}
				return 0;
			}
			return 1;
		} else if (Geometry.isSegment(gt.value())) {
			/* const */Segment seg = (Segment) geometry;
			Polyline polyline = new Polyline(seg.getDescription());
			polyline.addSegment(seg, true);
			return isSimpleAsFeature(polyline, spatialReference, bForce,
					result, progressTracker);
		}

		// double geomTolerance = 0;
		int isSimple = ((MultiVertexGeometryImpl) geometry._getImpl())
				.getIsSimple(tolerance);
		int knownSimpleResult = bForce ? -1 : isSimple;
		// TODO: need to distinguish KnownSimple between SimpleAsFeature and
		// SimplePlanar.
		// From the first sight it seems the SimplePlanar implies
		// SimpleAsFeature.
		if (knownSimpleResult != -1)
			return knownSimpleResult;

		OperatorSimplifyLocalHelper helper = new OperatorSimplifyLocalHelper(
				geometry, spatialReference, knownSimpleResult, progressTracker,
				false);

		if (gt == Geometry.Type.MultiPoint) {
			knownSimpleResult = helper.multiPointIsSimpleAsFeature_();
		} else if (gt == Geometry.Type.Polyline) {
			knownSimpleResult = helper.polylineIsSimpleAsFeature_();
		} else if (gt == Geometry.Type.Polygon) {
			knownSimpleResult = helper.polygonIsSimpleAsFeature_();
		} else {
			throw GeometryException.GeometryInternalError();// what else?
		}

		((MultiVertexGeometryImpl) (geometry._getImpl())).setIsSimple(
				knownSimpleResult, tolerance, false);
		if (result != null && knownSimpleResult == 0)
			result.Assign(helper.m_nonSimpleResult);
		return knownSimpleResult;
	}

	static int isSimpleOGC(/* const */Geometry geometry, /* const */
	SpatialReference spatialReference, boolean bForce, NonSimpleResult result,
			ProgressTracker progressTracker) {
		if (result != null) {
			result.m_reason = NonSimpleResult.Reason.NotDetermined;
			result.m_vertexIndex1 = -1;
			result.m_vertexIndex2 = -1;
		}
		if (geometry.isEmpty())
			return 1;
		Geometry.Type gt = geometry.getType();
		if (gt == Geometry.Type.Point)
			return 1;

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatialReference, geometry, false);
		if (gt == Geometry.Type.Envelope) {
			/* const */Envelope env = (Envelope) geometry;
			Envelope2D env2D = new Envelope2D();
			env.queryEnvelope2D(env2D);
			if (env2D.isDegenerate(tolerance)) {
				if (result != null) {
					result.m_reason = NonSimpleResult.Reason.DegenerateSegments;
					result.m_vertexIndex1 = -1;
					result.m_vertexIndex2 = -1;
				}
				return 0;
			}
			return 1;
		} else if (Geometry.isSegment(gt.value())) {
			/* const */Segment seg = (Segment) geometry;
			Polyline polyline = new Polyline(seg.getDescription());
			polyline.addSegment(seg, true);
			return isSimpleAsFeature(polyline, spatialReference, bForce,
					result, progressTracker);
		}

		int knownSimpleResult = -1;

		OperatorSimplifyLocalHelper helper = new OperatorSimplifyLocalHelper(
				geometry, spatialReference, knownSimpleResult, progressTracker,
				true);

		if (gt == Geometry.Type.MultiPoint || gt == Geometry.Type.Polyline
				|| gt == Geometry.Type.Polygon) {
			knownSimpleResult = helper.isSimplePlanarImpl_();
		} else {
			throw GeometryException.GeometryInternalError();// what else?
		}

		if (result != null)
			result.Assign(helper.m_nonSimpleResult);

		return knownSimpleResult;
	}

	/**
	 * Simplifies geometries for storing in DB:
	 * 
	 * MultiPoint:check that no points coincide.tolerance is ignored.
	 * Polyline:ensure there no segments degenerate segments. Polygon:cracks and
	 * clusters using cluster tolerance and resolves all self intersections,
	 * orients rings properly and arranges the rings in the OGC order.
	 * 
	 * Returns simplified geometry.
	 */
	static protected Geometry simplifyAsFeature(/* const */Geometry geometry, /* const */
	SpatialReference spatialReference, boolean bForce,
			ProgressTracker progressTracker) {
		if (geometry.isEmpty())
			return geometry;
		Geometry.Type gt = geometry.getType();
		if (gt == Geometry.Type.Point)
			return geometry;

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatialReference, geometry, false);
		if (gt == Geometry.Type.Envelope) {
			Envelope env = (Envelope) geometry;
			Envelope2D env2D = new Envelope2D();
			env.queryEnvelope2D(env2D);
			if (env2D.isDegenerate(tolerance)) {
				return (Geometry) (env.createInstance()); // return empty
															// geometry
			}
			return geometry;
		} else if (Geometry.isSegment(gt.value())) {
			Segment seg = (Segment) geometry;
			Polyline polyline = new Polyline(seg.getDescription());
			polyline.addSegment(seg, true);
			return simplifyAsFeature(polyline, spatialReference, bForce,
					progressTracker);
		}

		double geomTolerance = 0;
		int isSimple = ((MultiVertexGeometryImpl) geometry._getImpl())
				.getIsSimple(tolerance);
		int knownSimpleResult = bForce ? GeometryXSimple.Unknown : isSimple;

		// TODO: need to distinguish KnownSimple between SimpleAsFeature and
		// SimplePlanar.
		// From the first sight it seems the SimplePlanar implies
		// SimpleAsFeature.
		if (knownSimpleResult == GeometryXSimple.Strong) {
	        if (gt == Geometry.Type.Polygon && ((Polygon)geometry).getFillRule() != Polygon.FillRule.enumFillRuleOddEven)
	        {
	          Geometry res = geometry.copy();
	          ((Polygon)res).setFillRule(Polygon.FillRule.enumFillRuleOddEven);//standardize on odd_even fill rule
	          return res;
	        }			
	        
			return geometry;
		}

		OperatorSimplifyLocalHelper helper = new OperatorSimplifyLocalHelper(
				geometry, spatialReference, knownSimpleResult, progressTracker,
				false);

		Geometry result;

		if (gt == Geometry.Type.MultiPoint) {
			result = (Geometry) (helper.multiPointSimplifyAsFeature_());
		} else if (gt == Geometry.Type.Polyline) {
			result = (Geometry) (helper.polylineSimplifyAsFeature_());
		} else if (gt == Geometry.Type.Polygon) {
			result = (Geometry) (helper.polygonSimplifyAsFeature_());
		} else {
			throw GeometryException.GeometryInternalError(); // what else?
		}

		return result;
	}

	/**
	 * Simplifies geometries for storing in OGC format:
	 * 
	 * MultiPoint:check that no points coincide.tolerance is ignored.
	 * Polyline:ensure there no segments degenerate segments. Polygon:cracks and
	 * clusters using cluster tolerance and resolves all self intersections,
	 * orients rings properly and arranges the rings in the OGC order.
	 * 
	 * Returns simplified geometry.
	 */
	static Geometry simplifyOGC(/* const */Geometry geometry, /* const */
	SpatialReference spatialReference, boolean bForce,
			ProgressTracker progressTracker) {
		if (geometry.isEmpty())
			return geometry;
		Geometry.Type gt = geometry.getType();
		if (gt == Geometry.Type.Point)
			return geometry;

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatialReference, geometry, false);
		if (gt == Geometry.Type.Envelope) {
			Envelope env = (Envelope) geometry;
			Envelope2D env2D = new Envelope2D();
			env.queryEnvelope2D(env2D);
			if (env2D.isDegenerate(tolerance)) {
				return (Geometry) (env.createInstance()); // return empty
															// geometry
			}
			return geometry;
		} else if (Geometry.isSegment(gt.value())) {
			Segment seg = (Segment) geometry;
			Polyline polyline = new Polyline(seg.getDescription());
			polyline.addSegment(seg, true);
			return simplifyOGC(polyline, spatialReference, bForce,
					progressTracker);
		}

		if (!Geometry.isMultiVertex(gt.value())) {
			throw new GeometryException("OGC simplify is not implemented for this geometry type" + gt);
		}

		MultiVertexGeometry result = TopologicalOperations.simplifyOGC(
				(MultiVertexGeometry) geometry, tolerance, false, progressTracker);

		return result;
	}

	private int compareVertices_(int i1, int i2, boolean get_paths) {
		if (i1 == i2)
			return 0;

		int pair1 = m_pairs.get(i1);
		int pair2 = m_pairs.get(i2);
		int xy1 = pair1 >> 1;
		int xy2 = pair2 >> 1;
		Point2D pt1 = new Point2D();
		Point2D pt2 = new Point2D();
		m_xy.read(2 * xy1, pt1);
		pt1.y += (((pair1 & 1) != 0) ? m_toleranceIsSimple
				: -m_toleranceIsSimple);
		m_xy.read(2 * xy2, pt2);
		pt2.y += (((pair2 & 1) != 0) ? m_toleranceIsSimple
				: -m_toleranceIsSimple);
		int res = pt1.compare(pt2);
		if (res == 0 && get_paths) {
			int di = m_paths_for_OGC_tests.get(xy1)
					- m_paths_for_OGC_tests.get(xy2);
			return di < 0 ? -1 : di > 0 ? 1 : 0;
		}
		return res;
	}

	private static final class VertexComparer extends
			AttributeStreamOfInt32.IntComparator {
		OperatorSimplifyLocalHelper parent;
		boolean get_paths;

		VertexComparer(OperatorSimplifyLocalHelper parent_, boolean get_paths_) {
			parent = parent_;
			get_paths = get_paths_;
		}

		@Override
		public int compare(int i1, int i2) {
			return parent.compareVertices_(i1, i2, get_paths);
		}
	}

	private static final class IndexSorter extends ClassicSort {
		OperatorSimplifyLocalHelper parent;
		private boolean get_paths;
		private Point2D pt1_dummy = new Point2D();

		IndexSorter(OperatorSimplifyLocalHelper parent_, boolean get_paths_) {
			parent = parent_;
			get_paths = get_paths_;
		}

		@Override
		public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
			indices.Sort(begin, end, new VertexComparer(parent, get_paths));
		}

		@Override
		public double getValue(int index) /* const */
		{
			int pair = parent.m_pairs.get(index);
			int xy1 = pair >> 1;
			parent.m_xy.read(2 * xy1, pt1_dummy);
			double y = pt1_dummy.y
					+ (((pair & 1) != 0) ? parent.m_toleranceIsSimple
							: -parent.m_toleranceIsSimple);
			return y;
		}
	}

	private int compareVerticesMultiPoint_(int i1, int i2) {
		if (i1 == i2)
			return 0;
		MultiVertexGeometryImpl multiVertexImpl = (MultiVertexGeometryImpl) m_geometry
				._getImpl();
		Point2D pt1 = multiVertexImpl.getXY(i1);
		Point2D pt2 = multiVertexImpl.getXY(i2);

		if (pt1.x < pt2.x)
			return -1;
		if (pt1.x > pt2.x)
			return 1;
		if (pt1.y < pt2.y)
			return -1;
		if (pt1.y > pt2.y)
			return 1;

		for (int attrib = 1; attrib < m_attributeCount; attrib++) {
			int semantics = m_description.getSemantics(attrib);
			int nords = VertexDescription.getComponentCount(semantics);
			for (int ord = 0; ord < nords; ord++) {
				double v1 = multiVertexImpl.getAttributeAsDbl(semantics, i1,
						ord);
				double v2 = multiVertexImpl.getAttributeAsDbl(semantics, i2,
						ord);
				if (v1 < v2)
					return -1;
				if (v1 > v2)
					return 1;
			}
		}

		return 0;
	}

	private int compareVerticesMultiPoint2_(int i1, int i2) {
		int res = compareVerticesMultiPoint_(i1, i2);
		if (res == 0)
			return i1 < i2 ? -1 : 1;
		else
			return res;
	}

	private static final class EdgeComparerForSelfIntersection implements
			Comparator<Edge> {
		OperatorSimplifyLocalHelper parent;

		EdgeComparerForSelfIntersection(OperatorSimplifyLocalHelper parent_) {
			parent = parent_;
		}

		// Recall that the total ordering [<] induced by compare satisfies e1
		// [<] e2 if and only if compare(e1, e2) < 0.

		@Override
		public int compare(Edge e1, Edge e2) {
			return parent.edgeAngleCompare_(e1, e2);
		}
	}

	private static final class MultiPointVertexComparer extends
			AttributeStreamOfInt32.IntComparator {
		OperatorSimplifyLocalHelper parent;

		MultiPointVertexComparer(OperatorSimplifyLocalHelper parent_) {
			parent = parent_;
		}

		@Override
		public int compare(int i1, int i2) {
			return parent.compareVerticesMultiPoint_(i1, i2);
		}
	}

	private static final class MultiPointVertexComparer2 extends
			AttributeStreamOfInt32.IntComparator {
		OperatorSimplifyLocalHelper parent;

		MultiPointVertexComparer2(OperatorSimplifyLocalHelper parent_) {
			parent = parent_;
		}

		@Override
		public int compare(int i1, int i2) {
			return parent.compareVerticesMultiPoint2_(i1, i2);
		}
	}

	// compares angles between two edges
	private int edgeAngleCompare_(/* const */Edge edge1, /* const */Edge edge2) {
		if (edge1.equals(edge2))
			return 0;

		Point2D v1 = edge1.m_segment._getTangent(edge1.getReversed() ? 1.0
				: 0.0);
		if (edge1.getReversed())
			v1.negate();
		Point2D v2 = edge2.m_segment._getTangent(edge2.getReversed() ? 1.0
				: 0.0);
		if (edge2.getReversed())
			v2.negate();

		int q1 = v1._getQuarter();
		int q2 = v2._getQuarter();

		if (q2 == q1) {
			double cross = v1.crossProduct(v2);
			double crossError = 4 * NumberUtils.doubleEps()
					* (Math.abs(v2.x * v1.y) + Math.abs(v2.y * v1.x));
			if (Math.abs(cross) <= crossError) {
				cross--; // To avoid warning of "this line has no effect" from
							// cross = cross.
				cross++;
			}
			assert (Math.abs(cross) > crossError);
			return cross < 0 ? 1 : (cross > 0 ? -1 : 0);
		} else {
			return q1 < q2 ? -1 : 1;
		}
	}
};
