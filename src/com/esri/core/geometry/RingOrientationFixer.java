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

class RingOrientationFixer {
	EditShape m_shape;
	Treap m_AET;
	double m_y_scanline;
	int m_geometry;
	int m_unknown_ring_orientation_count;
	IndexMultiDCList m_sorted_vertices;
	AttributeStreamOfInt32 m_unknown_nodes;
	int m_node_1_user_index;
	int m_node_2_user_index;
	int m_path_orientation_index;
	int m_path_parentage_index;

	static final class Edges {
		EditShape m_shape;
		AttributeStreamOfInt32 m_end_1_nodes;
		AttributeStreamOfInt32 m_end_2_nodes;
		AttributeStreamOfInt8 m_directions;
		Point2D pt_1 = new Point2D();
		Point2D pt_2 = new Point2D();
		int m_first_free;

		boolean getDirection_(int index) {
			return m_shape.getNextVertex(getEnd1(index)) == getEnd2(index);
		}

		int getEnd_(int index) {
			int v_1 = getEnd1(index);
			int v_2 = getEnd2(index);
			if (m_shape.getNextVertex(v_1) == v_2)
				return v_2;
			else
				return v_1;
		}

		Edges(EditShape shape) {
			m_shape = shape;
			m_first_free = -1;
		}

		Segment getSegment(int index) {
			return m_shape.getSegment(getStart(index));
		}

		// True if the start vertex is the lower point of the edge.
		boolean isBottomUp(int index) {
			int v_1 = getEnd1(index);
			int v_2 = getEnd2(index);
			if (m_shape.getPrevVertex(v_1) == v_2) {
				int temp = v_1;
				v_1 = v_2;
				v_2 = temp;
			}
			m_shape.getXY(v_1, pt_1);
			m_shape.getXY(v_2, pt_2);
			return pt_1.y < pt_2.y;
		}

		int getStart(int index) {
			int v_1 = getEnd1(index);
			int v_2 = getEnd2(index);
			return (m_shape.getNextVertex(v_1) == v_2) ? v_1 : v_2;
		}

		int getEnd1(int index) {
			return m_end_1_nodes.get(index);
		}

		int getEnd2(int index) {
			return m_end_2_nodes.get(index);
		}

		void freeEdge(int edge) {
			m_end_1_nodes.set(edge, m_first_free);
			m_first_free = edge;
		}

		int newEdge(int vertex) {
			if (m_first_free != -1) {
				int index = m_first_free;
				m_first_free = m_end_1_nodes.get(index);
				m_end_1_nodes.set(index, vertex);
				m_end_2_nodes.set(index, m_shape.getNextVertex(vertex));
				return index;
			} else if (m_end_1_nodes == null) {
				m_end_1_nodes = new AttributeStreamOfInt32(0);
				m_end_2_nodes = new AttributeStreamOfInt32(0);
			}

			int index = m_end_1_nodes.size();
			m_end_1_nodes.add(vertex);
			m_end_2_nodes.add(m_shape.getNextVertex(vertex));
			return index;
		}

		EditShape getShape() {
			return m_shape;
		}

		int getPath(int index) {
			return m_shape.getPathFromVertex(getEnd1(index));
		}
	}

	Edges m_edges;

	class RingOrientationTestComparator extends Treap.Comparator {
		RingOrientationFixer m_helper;
		Line m_line_1;
		Line m_line_2;
		int m_left_elm;
		double m_leftx;
		Segment m_seg_1;

		RingOrientationTestComparator(RingOrientationFixer helper) {
			m_helper = helper;
			m_line_1 = new Line();
			m_line_2 = new Line();
			m_leftx = 0;
			m_seg_1 = null;
			m_left_elm = -1;
		}

		@Override
		int compare(Treap treap, int left, int node) {
			int right = treap.getElement(node);
			RingOrientationFixer.Edges edges = m_helper.m_edges;
			double x_1;
			if (m_left_elm == left)
				x_1 = m_leftx;
			else {
				m_seg_1 = edges.getSegment(left);
				if (m_seg_1 == null) {
					EditShape shape = edges.getShape();
					shape.queryLineConnector(edges.getStart(left), m_line_1);
					m_seg_1 = m_line_1;
					x_1 = m_line_1.intersectionOfYMonotonicWithAxisX(
							m_helper.m_y_scanline, 0);
				} else
					x_1 = m_seg_1.intersectionOfYMonotonicWithAxisX(
							m_helper.m_y_scanline, 0);

				m_leftx = x_1;
				m_left_elm = left;
			}

			Segment seg_2 = edges.getSegment(right);
			double x2;
			if (seg_2 == null) {
				EditShape shape = edges.getShape();
				shape.queryLineConnector(edges.getStart(right), m_line_2);
				seg_2 = m_line_2;
				x2 = m_line_2.intersectionOfYMonotonicWithAxisX(
						m_helper.m_y_scanline, 0);
			} else
				x2 = seg_2.intersectionOfYMonotonicWithAxisX(
						m_helper.m_y_scanline, 0);

			if (x_1 == x2) {
				boolean bStartLower1 = edges.isBottomUp(left);
				boolean bStartLower2 = edges.isBottomUp(right);

				// apparently these edges originate from same vertex and the
				// scanline is on the vertex. move scanline a little.
				double y1 = !bStartLower1 ? m_seg_1.getStartY() : m_seg_1
						.getEndY();
				double y2 = !bStartLower2 ? seg_2.getStartY() : seg_2.getEndY();
				double miny = Math.min(y1, y2);
				double y = (miny + m_helper.m_y_scanline) * 0.5;
				if (y == m_helper.m_y_scanline) {
					// assert(0);//ST: not a bug. just curious to see this
					// happens.
					y = miny; // apparently, one of the segments is almost
								// horizontal line.
				}
				x_1 = m_seg_1.intersectionOfYMonotonicWithAxisX(y, 0);
				x2 = seg_2.intersectionOfYMonotonicWithAxisX(y, 0);
				assert (x_1 != x2);
			}

			return x_1 < x2 ? -1 : (x_1 > x2 ? 1 : 0);
		}

		void reset() {
			m_left_elm = -1;
		}
	}

	RingOrientationTestComparator m_sweep_comparator;

	RingOrientationFixer() {
		m_AET = new Treap();
		m_AET.disableBalancing();
		m_sweep_comparator = new RingOrientationTestComparator(this);
		m_AET.setComparator(m_sweep_comparator);
	}

	boolean fixRingOrientation_() {
		if (m_shape.getPathCount(m_geometry) == 1) {
			int path = m_shape.getFirstPath(m_geometry);
			double area = m_shape.getRingArea(path);
			m_shape.setExterior(path, true);
			if (area < 0) {
				int first = m_shape.getFirstVertex(path);
				m_shape.reverseRingInternal_(first);
				m_shape.setLastVertex_(path, m_shape.getPrevVertex(first));// fix
																			// last
																			// after
																			// the
																			// reverse
				return true;
			}

			return false;
		}

		m_path_orientation_index = m_shape.createPathUserIndex();// used to
																	// store
																	// discovered
																	// orientation
																	// (3 -
																	// extrior,
																	// 2 -
																	// interior)
		m_path_parentage_index = m_shape.createPathUserIndex();// used to
																// resolve OGC
																// order
		for (int path = m_shape.getFirstPath(m_geometry); path != -1; path = m_shape
				.getNextPath(path)) {
			m_shape.setPathUserIndex(path, m_path_orientation_index, 0);
			m_shape.setPathUserIndex(path, m_path_parentage_index, -1);
		}

		AttributeStreamOfInt32 bunch = new AttributeStreamOfInt32(0);
		boolean bFound = false;
		m_y_scanline = NumberUtils.TheNaN;
		Point2D pt = new Point2D();
		m_unknown_ring_orientation_count = m_shape.getPathCount(m_geometry);
		m_node_1_user_index = m_shape.createUserIndex();
		m_node_2_user_index = m_shape.createUserIndex();
		for (int ivertex = m_sorted_vertices.getFirst(m_sorted_vertices
				.getFirstList()); ivertex != -1; ivertex = m_sorted_vertices
				.getNext(ivertex)) {
			int vertex = m_sorted_vertices.getData(ivertex);
			m_shape.getXY(vertex, pt);
			if (pt.y != m_y_scanline && bunch.size() != 0) {
				bFound |= processBunchForRingOrientationTest_(bunch);
				m_sweep_comparator.reset();
				bunch.clear(false);
			}

			bunch.add(vertex);// all vertices that have same y are added to the
								// bunch
			m_y_scanline = pt.y;
			if (m_unknown_ring_orientation_count == 0)
				break;
		}

		if (m_unknown_ring_orientation_count > 0) {
			bFound |= processBunchForRingOrientationTest_(bunch);
			bunch.clear(false);
		}

		m_shape.removeUserIndex(m_node_1_user_index);
		m_shape.removeUserIndex(m_node_2_user_index);

		// dbg_verify_ring_orientation_();//debug

		for (int path = m_shape.getFirstPath(m_geometry); path != -1;) {
			if (m_shape.getPathUserIndex(path, m_path_orientation_index) == 3) {// exterior
				m_shape.setExterior(path, true);
				int afterPath = path;
				for (int nextHole = m_shape.getPathUserIndex(path,
						m_path_parentage_index); nextHole != -1;) {
					int p = m_shape.getPathUserIndex(nextHole,
							m_path_parentage_index);
					m_shape.movePath(m_geometry,
							m_shape.getNextPath(afterPath), nextHole);
					afterPath = nextHole;
					nextHole = p;
				}
				path = m_shape.getNextPath(afterPath);
			} else {
				m_shape.setExterior(path, false);
				path = m_shape.getNextPath(path);
			}
		}

		m_shape.removePathUserIndex(m_path_orientation_index);
		m_shape.removePathUserIndex(m_path_parentage_index);

		return bFound;
	}

	boolean processBunchForRingOrientationTest_(AttributeStreamOfInt32 bunch) {
		return processBunchForRingOrientationTestOddEven_(bunch);
	}

	boolean processBunchForRingOrientationTestOddEven_(
			AttributeStreamOfInt32 bunch) {
		boolean bModified = false;
		if (m_edges == null)
			m_edges = new Edges(m_shape);

		if (m_unknown_nodes == null) {
			m_unknown_nodes = new AttributeStreamOfInt32(0);
			m_unknown_nodes.reserve(16);
		} else {
			m_unknown_nodes.clear(false);
		}

		processBunchForRingOrientationRemoveEdges_(bunch);

		// add edges that come into scope
		for (int i = 0, n = bunch.size(); i < n; i++) {
			int vertex = bunch.get(i);
			if (vertex == -1)
				continue;
			insertEdge_(vertex, -1);
		}

		for (int i = 0; i < m_unknown_nodes.size()
				&& m_unknown_ring_orientation_count > 0; i++) {
			int aetNode = m_unknown_nodes.get(i);
			int edge = m_AET.getElement(aetNode);
			int path = m_edges.getPath(edge);
			int orientation = m_shape.getPathUserIndex(path,
					m_path_orientation_index);
			int prevPath = -1;
			if (orientation == 0) {
				int node = m_AET.getPrev(aetNode);
				int prevNode = aetNode;
				boolean odd_even = false;
				// find the leftmost edge for which the ring orientation is
				// known
				while (node != Treap.nullNode()) {
					int edge1 = m_AET.getElement(node);
					int path1 = m_edges.getPath(edge1);
					int orientation1 = m_shape.getPathUserIndex(path1,
							m_path_orientation_index);
					if (orientation1 != 0) {
						prevPath = path1;
						break;
					}
					prevNode = node;
					node = m_AET.getPrev(node);
				}
				if (node == Treap.nullNode()) {// if no edges have ring
												// orientation known, then start
												// from the left most and it has
												// to be exterior ring.
					odd_even = true;
					node = prevNode;
				} else {
					int edge1 = m_AET.getElement(node);
					odd_even = m_edges.isBottomUp(edge1);
					node = m_AET.getNext(node);
					odd_even = !odd_even;
				}

				do {
					int edge1 = m_AET.getElement(node);
					int path1 = m_edges.getPath(edge1);
					int orientation1 = m_shape.getPathUserIndex(path1,
							m_path_orientation_index);
					if (orientation1 == 0) {
						if (odd_even != m_edges.isBottomUp(edge1)) {
							int first = m_shape.getFirstVertex(path1);
							m_shape.reverseRingInternal_(first);
							m_shape.setLastVertex_(path1,
									m_shape.getPrevVertex(first));
							bModified = true;
						}

						m_shape.setPathUserIndex(path1,
								m_path_orientation_index, odd_even ? 3 : 2);
						if (!odd_even) {// link the holes into the linked list
										// to mantain the OGC order.
							int lastHole = m_shape.getPathUserIndex(prevPath,
									m_path_parentage_index);
							m_shape.setPathUserIndex(prevPath,
									m_path_parentage_index, path1);
							m_shape.setPathUserIndex(path1,
									m_path_parentage_index, lastHole);
						}

						m_unknown_ring_orientation_count--;
						if (m_unknown_ring_orientation_count == 0)
							return bModified;
					}

					prevPath = path1;
					prevNode = node;
					node = m_AET.getNext(node);
					odd_even = !odd_even;
				} while (prevNode != aetNode);
			}
		}

		return bModified;
	}

	void processBunchForRingOrientationRemoveEdges_(AttributeStreamOfInt32 bunch) {
		// remove all nodes that go out of scope
		for (int i = 0, n = bunch.size(); i < n; i++) {
			int vertex = bunch.get(i);
			int node1 = m_shape.getUserIndex(vertex, m_node_1_user_index);
			int node2 = m_shape.getUserIndex(vertex, m_node_2_user_index);
			if (node1 != -1) {
				int edge = m_AET.getElement(node1);
				m_edges.freeEdge(edge);
				m_shape.setUserIndex(vertex, m_node_1_user_index, -1);
			}
			if (node2 != -1) {
				int edge = m_AET.getElement(node2);
				m_edges.freeEdge(edge);
				m_shape.setUserIndex(vertex, m_node_2_user_index, -1);
			}

			int reused_node = -1;
			if (node1 != -1 && node2 != -1) {// terminating vertex
				m_AET.deleteNode(node1, -1);
				m_AET.deleteNode(node2, -1);
				bunch.set(i, -1);
			} else
				reused_node = node1 != -1 ? node1 : node2;

			if (reused_node != -1) {// this vertex is a part of vertical chain.
									// Sorted order in AET did not change, so
									// reuse the AET node.
				if (!insertEdge_(vertex, reused_node))
					m_AET.deleteNode(reused_node, -1);// horizontal edge was not
														// inserted
				bunch.set(i, -1);
			}
		}
	}

	boolean insertEdge_(int vertex, int reused_node) {
		Point2D pt_1 = new Point2D();
		Point2D pt_2 = new Point2D();
		m_shape.getXY(vertex, pt_1);
		int next = m_shape.getNextVertex(vertex);
		m_shape.getXY(next, pt_2);
		boolean b_res = false;
		if (pt_1.y < pt_2.y) {
			b_res = true;
			int edge = m_edges.newEdge(vertex);
			int aetNode;
			if (reused_node == -1)
				aetNode = m_AET.addElement(edge, -1);
			else {
				aetNode = reused_node;
				m_AET.setElement(aetNode, edge);
			}
			int node = m_shape.getUserIndex(next, m_node_1_user_index);
			if (node == -1)
				m_shape.setUserIndex(next, m_node_1_user_index, aetNode);
			else
				m_shape.setUserIndex(next, m_node_2_user_index, aetNode);

			int path = m_shape.getPathFromVertex(vertex);
			if (m_shape.getPathUserIndex(path, m_path_orientation_index) == 0) {
				m_unknown_nodes.add(aetNode);
			}
		}

		int prev = m_shape.getPrevVertex(vertex);
		m_shape.getXY(prev, pt_2);
		if (pt_1.y < pt_2.y) {
			b_res = true;
			int edge = m_edges.newEdge(prev);
			int aetNode;
			if (reused_node == -1)
				aetNode = m_AET.addElement(edge, -1);
			else {
				aetNode = reused_node;
				m_AET.setElement(aetNode, edge);
			}
			int node = m_shape.getUserIndex(prev, m_node_1_user_index);
			if (node == -1)
				m_shape.setUserIndex(prev, m_node_1_user_index, aetNode);
			else
				m_shape.setUserIndex(prev, m_node_2_user_index, aetNode);

			int path = m_shape.getPathFromVertex(vertex);
			if (m_shape.getPathUserIndex(path, m_path_orientation_index) == 0) {
				m_unknown_nodes.add(aetNode);
			}
		}

		return b_res;
	}

	static boolean execute(EditShape shape, int geometry,
			IndexMultiDCList sorted_vertices) {
		RingOrientationFixer fixer = new RingOrientationFixer();
		fixer.m_shape = shape;
		fixer.m_geometry = geometry;
		fixer.m_sorted_vertices = sorted_vertices;
		return fixer.fixRingOrientation_();
	}

}
