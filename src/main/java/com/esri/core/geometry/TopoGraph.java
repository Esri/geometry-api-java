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

import com.esri.core.geometry.AttributeStreamOfInt32.IntComparator;

final class TopoGraph {

	static interface EnumInputMode {

		final static int enumInputModeBuildGraph = 0;
		final static int enumInputModeSimplifyAlternate = 4 + 0;
		final static int enumInputModeSimplifyWinding = 4 + 1;
		final static int enumInputModeIsSimplePolygon = 4 + 3;
	}

	EditShape m_shape;

	// cluster data: index, parentage, halfEdge, globalPrev, globalNext
	StridedIndexTypeCollection m_clusterData;
	StridedIndexTypeCollection m_clusterVertices;
	int m_firstCluster;
	int m_lastCluster;
	// edge data: index, origin, faceParentage, edgeParentage, twin, prev, next
	StridedIndexTypeCollection m_halfEdgeData;
	// chain data index, half_edge, parentage, parentChain, firstIsland,
	// nextInParent, prev, next
	StridedIndexTypeCollection m_chainData;
	AttributeStreamOfDbl m_chainAreas;
	AttributeStreamOfDbl m_chainPerimeters;

	final int c_edgeParentageMask;
	final int c_edgeBitMask;
	int m_universeChain;
	ArrayList<AttributeStreamOfInt32> m_edgeIndices;
	ArrayList<AttributeStreamOfInt32> m_clusterIndices;
	ArrayList<AttributeStreamOfInt32> m_chainIndices;

	int m_geometryIDIndex; // index of geometryIDs in the m_shape
	int m_clusterIndex; // vertex index of cluster handles in the m_shape
	int m_halfEdgeIndex; // vertex index of half-edges in the m_shape
	int m_tmpHalfEdgeParentageIndex;
	int m_tmpHalfEdgeWindingNumberIndex;
	int m_tmpHalfEdgeOddEvenNumberIndex = -1;
	
	int m_universe_geomID = -1;
	
	boolean m_buildChains = true;
	
	private boolean m_dirty_check_failed = false;
	private double m_check_dirty_planesweep_tolerance = Double.NaN;
	
	void check_dirty_planesweep(double tolerance) {
		m_check_dirty_planesweep_tolerance = tolerance;
	}

	boolean dirty_check_failed() {
		return m_dirty_check_failed;
	}
	
	NonSimpleResult m_non_simple_result = new NonSimpleResult();

	int m_pointCount;// point count processed in this Topo_graph. Used to
						// reserve data.

	static final class PlaneSweepComparator extends Treap.Comparator {
		TopoGraph m_helper;
		SegmentBuffer m_buffer_left;
		SegmentBuffer m_buffer_right;
		Envelope1D interval_left;
		Envelope1D interval_right;
		double m_y_scanline;

		PlaneSweepComparator(TopoGraph helper) {
			m_helper = helper;
			m_y_scanline = NumberUtils.TheNaN;
			m_buffer_left = new SegmentBuffer();
			m_buffer_right = new SegmentBuffer();
			interval_left = new Envelope1D();
			interval_right = new Envelope1D();
		}

		@Override
		int compare(Treap treap, int left, int node) {
			int right = treap.getElement(node);
			// can be sped up a little, because left or right stay the same
			// while an edge is inserted into the tree.
			m_helper.querySegmentXY(left, m_buffer_left);
			m_helper.querySegmentXY(right, m_buffer_right);
			Segment segLeft = m_buffer_left.get();
			Segment segRight = m_buffer_right.get();

			// Prerequisite: The segments have the start point lexicographically
			// above the end point.
			assert (segLeft.getStartXY().compare(segLeft.getEndXY()) < 0);
			assert (segRight.getStartXY().compare(segRight.getEndXY()) < 0);

			// Simple test for faraway segments
			interval_left.setCoords(segLeft.getStartX(), segLeft.getEndX());
			interval_right.setCoords(segRight.getStartX(), segRight.getEndX());
			if (interval_left.vmax < interval_right.vmin)
				return -1;
			if (interval_left.vmin > interval_right.vmax)
				return 1;

			boolean bLeftHorz = segLeft.getStartY() == segLeft.getEndY();
			boolean bRightHorz = segRight.getStartY() == segRight.getEndY();
			if (bLeftHorz || bRightHorz) {
				if (bLeftHorz && bRightHorz) {
					assert (interval_left.equals(interval_right));
					return 0;
				}

				// left segment is horizontal. The right one is not.
				// Prerequisite of this algorithm is that this can only happen
				// when:
				// left
				// |right -------------------- end == end
				// | |
				// | left |
				// -------------------- right |
				// start == start
				// or:
				// right segment is horizontal. The left one is not.
				// Prerequisite of this algorithm is that his can only happen
				// when:
				// right
				// |left -------------------- end == end
				// | |
				// | right |
				// -------------------- left |
				// start == start

				if (segLeft.getStartY() == segRight.getStartY()
						&& segLeft.getStartX() == segRight.getStartX())
					return bLeftHorz ? 1 : -1;
				else if (segLeft.getEndY() == segRight.getEndY()
						&& segLeft.getEndX() == segRight.getEndX())
					return bLeftHorz ? -1 : 1;
			}

			// Now do actual intersections
			double xLeft = segLeft.intersectionOfYMonotonicWithAxisX(
					m_y_scanline, interval_left.vmin);
			double xRight = segRight.intersectionOfYMonotonicWithAxisX(
					m_y_scanline, interval_right.vmin);

			if (xLeft == xRight) {
				// apparently these edges originate from same vertex and the
				// scanline is on the vertex. move scanline a little.
				double yLeft = segLeft.getEndY();
				double yRight = segRight.getEndY();
				double miny = Math.min(yLeft, yRight);
				double y = (miny + m_y_scanline) * 0.5;
				if (y == m_y_scanline) {
					// assert(0);//ST: not a bug. just curious to see this
					// happens.
					y = miny; // apparently, one of the segments is almost
								// horizontal line.
				}
				xLeft = segLeft.intersectionOfYMonotonicWithAxisX(y,
						interval_left.vmin);
				xRight = segRight.intersectionOfYMonotonicWithAxisX(y,
						interval_right.vmin);
			}

			return xLeft < xRight ? -1 : (xLeft > xRight ? 1 : 0);
		}

		void setY(double y) {
			m_y_scanline = y;
		}
		// void operator=(const Plane_sweep_comparator&); // do not allow
		// operator =
	};

	static final class TopoGraphAngleComparer extends IntComparator {
		TopoGraph m_parent;

		TopoGraphAngleComparer(TopoGraph parent_) {
			m_parent = parent_;
		}

		@Override
		public int compare(int v1, int v2) {
			return m_parent.compareEdgeAngles_(v1, v2);
		}
	};

	static final class ClusterSweepMonikerComparator extends
			Treap.MonikerComparator {
		TopoGraph m_parent;
		SegmentBuffer m_segment_buffer;
		Point2D m_point;
		Envelope1D m_interval;

		ClusterSweepMonikerComparator(TopoGraph parent) {
			m_parent = parent;
			m_segment_buffer = new SegmentBuffer();
			m_point = new Point2D();
			m_interval = new Envelope1D();
		}

		void setPointXY(Point2D pt) {
			m_point.setCoords(pt);
		}

		@Override
		int compare(Treap treap, int node) {
			int half_edge = treap.getElement(node);

			// can be sped up a little, because left or right stay the same
			// while an edge is inserted into the tree.
			m_parent.querySegmentXY(half_edge, m_segment_buffer);
			Segment seg = m_segment_buffer.get();

			// Simple test for faraway segments
			m_interval.setCoords(seg.getStartX(), seg.getEndX());
			if (m_point.x < m_interval.vmin)
				return -1;

			if (m_point.x > m_interval.vmax)
				return 1;

			// Now do actual intersections
			double x = seg.intersectionOfYMonotonicWithAxisX(m_point.y,
					m_point.x);

			assert (x != m_point.x);

			return m_point.x < x ? -1 : (m_point.x > x ? 1 : 0);
		}
	}

	int newCluster_() {
		if (m_clusterData == null)
			m_clusterData = new StridedIndexTypeCollection(8);

		int cluster = m_clusterData.newElement();
		// m_clusterData->add(-1);//first vertex
		m_clusterData.setField(cluster, 1, 0);// parentage
		// m_clusterData->add(-1);//first half edge
		// m_clusterData->add(-1);//prev cluster
		// m_clusterData->add(-1);//next cluster
		return cluster;
	}

	int newHalfEdgePair_() {
		if (m_halfEdgeData == null)
			m_halfEdgeData = new StridedIndexTypeCollection(8);

		int halfEdge = m_halfEdgeData.newElement();
		// m_halfEdgeData.add(-1);//origin cluster
		m_halfEdgeData.setField(halfEdge, 2, 0);// chain parentage
		m_halfEdgeData.setField(halfEdge, 3, 0);// edge parentage
		// m_halfEdgeData.add(-1);//twin
		// m_halfEdgeData.add(-1);//prev
		// m_halfEdgeData.add(-1);//next
		int twinHalfEdge = m_halfEdgeData.newElement();
		// m_halfEdgeData.add(-1);//origin cluster
		m_halfEdgeData.setField(twinHalfEdge, 2, 0);// chain parentage
		m_halfEdgeData.setField(twinHalfEdge, 3, 0);// edge parentage
		// m_halfEdgeData.add(-1);//twin
		// m_halfEdgeData.add(-1);//prev
		// m_halfEdgeData.add(-1);//next
		setHalfEdgeTwin_(halfEdge, twinHalfEdge);
		setHalfEdgeTwin_(twinHalfEdge, halfEdge);
		return halfEdge;
	}

	int newChain_() {
		if (m_chainData == null)
			m_chainData = new StridedIndexTypeCollection(8);

		int chain = m_chainData.newElement();
		// m_chainData->write(chain, + 1, -1);//half_edge
		m_chainData.setField(chain, 2, 0);// parentage (geometric)
		// m_chainData->write(m_chainReserved + 3, -1);//parent chain
		// m_chainData->write(m_chainReserved + 4, -1);//firstIsland
		// m_chainData->write(m_chainReserved + 5, -1);//nextInParent
		// m_chainData->write(m_chainReserved + 6, -1);//prev
		// m_chainData->write(m_chainReserved + 7, -1);//next
		// m_chainReserved += 8;
		return chain;
	}

	int deleteChain_(int chain) {
		// Note: this method cannot be after _PlaneSweep
		assert (m_universeChain != chain);
		int n = getChainNext(chain);
		m_chainData.deleteElement(chain);
		// Note: no need to update the first chain, because one should never try
		// deleting the first (the universe) chain.
		return n;
	}

	int getClusterIndex_(int cluster) {
		return m_clusterData.elementToIndex(cluster);
	}

	void setClusterVertexIterator_(int cluster, int verticeList) {
		m_clusterData.setField(cluster, 7, verticeList);
	}

	void setClusterHalfEdge_(int cluster, int half_edge) {
		m_clusterData.setField(cluster, 2, half_edge);
	}

	void setClusterParentage_(int cluster, int parentage) {
		m_clusterData.setField(cluster, 1, parentage);
	}

	void setPrevCluster_(int cluster, int nextCluster) {
		m_clusterData.setField(cluster, 3, nextCluster);
	}

	void setNextCluster_(int cluster, int nextCluster) {
		m_clusterData.setField(cluster, 4, nextCluster);
	}

	void setClusterVertexIndex_(int cluster, int index) {
		m_clusterData.setField(cluster, 5, index);
	}

	int getClusterVertexIndex_(int cluster) {
		return m_clusterData.getField(cluster, 5);
	}

	void setClusterChain_(int cluster, int chain) {
		m_clusterData.setField(cluster, 6, chain);
	}

	void addClusterToExteriorChain_(int chain, int cluster) {
		assert (getClusterChain(cluster) == -1);
		setClusterChain_(cluster, chain);
		// There is no link from the chain to the cluster. Only vice versa.
		// Consider for change?
	}

	int getHalfEdgeIndex_(int he) {
		return m_halfEdgeData.elementToIndex(he);
	}

	void setHalfEdgeOrigin_(int half_edge, int cluster) {
		m_halfEdgeData.setField(half_edge, 1, cluster);
	}

	void setHalfEdgeTwin_(int half_edge, int twinHalfEdge) {
		m_halfEdgeData.setField(half_edge, 4, twinHalfEdge);
	}

	void setHalfEdgePrev_(int half_edge, int prevHalfEdge) {
		m_halfEdgeData.setField(half_edge, 5, prevHalfEdge);
	}

	void setHalfEdgeNext_(int half_edge, int nextHalfEdge) {
		m_halfEdgeData.setField(half_edge, 6, nextHalfEdge);
	}

	// void set_half_edge_chain_parentage_(int half_edge, int
	// chainParentageMask) { m_halfEdgeData.setField(half_edge + 2,
	// chainParentageMask); }
	void setHalfEdgeChain_(int half_edge, int chain) {
		m_halfEdgeData.setField(half_edge, 2, chain);
	}

	void setHalfEdgeParentage_(int half_edge, int parentageMask) {
		m_halfEdgeData.setField(half_edge, 3, parentageMask);
	}

	int getHalfEdgeParentageMask_(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 3);
	}

	void setHalfEdgeVertexIterator_(int half_edge, int vertexIterator) {
		m_halfEdgeData.setField(half_edge, 7, vertexIterator);
	}

	void updateVertexToHalfEdgeConnectionHelper_(int half_edge, boolean bClear) {
		int viter = getHalfEdgeVertexIterator(half_edge);
		if (viter != -1) {
			int he = bClear ? -1 : half_edge;
			for (int viter_ = getHalfEdgeVertexIterator(half_edge); viter_ != -1; viter_ = incrementVertexIterator(viter_)) {
				int vertex = getVertexFromVertexIterator(viter_);
				m_shape.setUserIndex(vertex, m_halfEdgeIndex, he);
			}
		}
	}

	void updateVertexToHalfEdgeConnection_(int half_edge, boolean bClear) {
		if (half_edge == -1)
			return;
		updateVertexToHalfEdgeConnectionHelper_(half_edge, bClear);
		updateVertexToHalfEdgeConnectionHelper_(getHalfEdgeTwin(half_edge),
				bClear);
	}

	int getChainIndex_(int chain) {
		return m_chainData.elementToIndex(chain);
	}

	void setChainHalfEdge_(int chain, int half_edge) {
		m_chainData.setField(chain, 1, half_edge);
	}

	void setChainParentage_(int chain, int parentage) {
		m_chainData.setField(chain, 2, parentage);
	}

	void setChainParent_(int chain, int parentChain) {
		assert (m_chainData.getField(chain, 3) != parentChain);
		m_chainData.setField(chain, 3, parentChain);
		int firstIsland = getChainFirstIsland(parentChain);
		setChainNextInParent_(chain, firstIsland);
		setChainFirstIsland_(parentChain, chain);
	}

	void setChainFirstIsland_(int chain, int islandChain) {
		m_chainData.setField(chain, 4, islandChain);
	}

	void setChainNextInParent_(int chain, int nextInParent) {
		m_chainData.setField(chain, 5, nextInParent);
	}

	void setChainPrev_(int chain, int prev) {
		m_chainData.setField(chain, 6, prev);
	}

	void setChainNext_(int chain, int next) {
		m_chainData.setField(chain, 7, next);
	}

	void setChainArea_(int chain, double area) {
		int chainIndex = getChainIndex_(chain);
		m_chainAreas.write(chainIndex, area);
	}

	void setChainPerimeter_(int chain, double perimeter) {
		int chainIndex = getChainIndex_(chain);
		m_chainPerimeters.write(chainIndex, perimeter);
	}

	void updateChainAreaAndPerimeter_(int chain) {
		double area = 0;
		double perimeter = 0;
		int firstHalfEdge = getChainHalfEdge(chain);
		Point2D origin = new Point2D(), from = new Point2D(), to = new Point2D();
		getHalfEdgeFromXY(firstHalfEdge, origin);
		from.setCoords(origin);
		int half_edge = firstHalfEdge;
		do {
			getHalfEdgeToXY(half_edge, to);
			perimeter += Point2D.distance(from, to);
			int twinChain = getHalfEdgeChain(getHalfEdgeTwin(half_edge));
			if (twinChain != chain)// only count edges are not dangling segments
									// of polylines
			{
				area += ((to.x - origin.x) - (from.x - origin.x))
						* ((to.y - origin.y) + (from.y - origin.y)) * 0.5;
			}

			from.setCoords(to);
			half_edge = getHalfEdgeNext(half_edge);
		} while (half_edge != firstHalfEdge);

		int ind = getChainIndex_(chain);
		m_chainAreas.write(ind, area);
		m_chainPerimeters.write(ind, perimeter);
	}

	int getChainTopMostEdge_(int chain) {
		int firstHalfEdge = getChainHalfEdge(chain);
		Point2D top = new Point2D();
		getHalfEdgeFromXY(firstHalfEdge, top);
		int topEdge = firstHalfEdge;
		Point2D v = new Point2D();
		int half_edge = firstHalfEdge;
		do {
			getHalfEdgeFromXY(half_edge, v);
			if (v.compare(top) > 0) {
				top.setCoords(v);
				topEdge = half_edge;
			}
			half_edge = getHalfEdgeNext(half_edge);
		} while (half_edge != firstHalfEdge);
		return topEdge;
	}

	void planeSweepParentage_(int inputMode, ProgressTracker progress_tracker) {
		PlaneSweepComparator comparator = new PlaneSweepComparator(this);
		Treap aet = new Treap();
		aet.setCapacity(m_pointCount / 2);
		aet.setComparator(comparator);

		AttributeStreamOfInt32 new_edges = new AttributeStreamOfInt32(0);
		int treeNodeIndex = createUserIndexForHalfEdges();

		ClusterSweepMonikerComparator clusterMoniker = null;
		int counter = 0;
		// Clusters are sorted by the y, x coordinate in ascending order.
		Point2D pt = new Point2D();
		// Each cluster is an event of the sweep-line algorithm.
		for (int cluster = getFirstCluster(); cluster != -1; cluster = getNextCluster(cluster)) {
			counter++;
			if ((counter & 0xFF) == 0) {
				if ((progress_tracker != null)
						&& !(progress_tracker.progress(-1, -1)))
					throw new UserCancelException();
			}

			int firstHalfEdge = getClusterHalfEdge(cluster);
			if (firstHalfEdge != -1) {
				new_edges.resizePreserveCapacity(0);
				if (!tryOptimizedInsertion_(aet, treeNodeIndex, new_edges,
						cluster, firstHalfEdge))// optimized insertion is for a
												// simple chain, in that case we
												// simply replace an old edge
												// with a new one in AET - O(1)
				{// This is more complex than a simple chain of edges
					getXY(cluster, pt);
					comparator.setY(pt.y);
					int clusterHalfEdge = firstHalfEdge;
					// Delete all edges that end at the cluster.
					do {// edges that end at the cluster have been assigned an
						// AET node in the treeNodeIndex.
						int attachedTreeNode = getHalfEdgeUserIndex(
								clusterHalfEdge, treeNodeIndex);
						if (attachedTreeNode != -1) {
							assert (attachedTreeNode != StridedIndexTypeCollection
									.impossibleIndex2());
							aet.deleteNode(attachedTreeNode, -1);
							setHalfEdgeUserIndex(clusterHalfEdge,
									treeNodeIndex,
									StridedIndexTypeCollection
											.impossibleIndex2());// set it to -2
						}

						clusterHalfEdge = getHalfEdgeNext(getHalfEdgeTwin(clusterHalfEdge));
						assert (getHalfEdgeOrigin(clusterHalfEdge) == cluster);
					} while (firstHalfEdge != clusterHalfEdge);

					// insert edges that start at the cluster.
					// We need to insert only the edges that have the from point
					// below the to point.
					// This is ensured by the logic of the algorithm.
					clusterHalfEdge = firstHalfEdge;
					do {
						int attachedTreeNode = getHalfEdgeUserIndex(
								clusterHalfEdge, treeNodeIndex);
						if (attachedTreeNode == -1) {
							int newTreeNode = aet.addElement(clusterHalfEdge,
									-1);
							new_edges.add(newTreeNode);
						}
						clusterHalfEdge = getHalfEdgeNext(getHalfEdgeTwin(clusterHalfEdge));
						assert (getHalfEdgeOrigin(clusterHalfEdge) == cluster);
					} while (firstHalfEdge != clusterHalfEdge);
				}

				// Analyze new edges.
				// We go in the opposite order, because of the way how the half
				// edges are sorted on a cluster.
				// We want to go from the left to the right.
				for (int i = new_edges.size() - 1; i >= 0; i--) {
					int newTreeNode = new_edges.get(i);
					int clusterHalfEdge = aet.getElement(newTreeNode);
					int twinEdge = getHalfEdgeTwin(clusterHalfEdge);
					assert (getHalfEdgeUserIndex(twinEdge, treeNodeIndex) == -1);
					setHalfEdgeUserIndex(twinEdge, treeNodeIndex, newTreeNode);

					planeSweepParentagePropagateParentage_(aet, newTreeNode,
							inputMode);
				}
			} else if (getClusterChain(cluster) == -1) {
				// get the left half edge of a face. The point belongs to the
				// face.
				if (clusterMoniker == null)
					clusterMoniker = new ClusterSweepMonikerComparator(this);

				getXY(cluster, pt);
				clusterMoniker.setPointXY(pt);
				int leftNode = aet.searchLowerBound(clusterMoniker, -1);
				int chain = m_universeChain;

				if (leftNode != -1) {
					int edge = aet.getElement(leftNode);
					int leftChain = getHalfEdgeChain(edge);
					if (leftChain == getHalfEdgeChain(getHalfEdgeTwin(edge))) {
						edge = getLeftSkipPolylines_(aet, leftNode);
					}

					if (edge != -1)
						chain = getHalfEdgeChain(edge);
				}

				addClusterToExteriorChain_(chain, cluster);
			}
		}

		deleteUserIndexForHalfEdges(treeNodeIndex);
	}

	void planeSweepParentagePropagateParentage_(Treap aet, int treeNode,
			int inputMode) {
		int edge = aet.getElement(treeNode);
		int edgeChain = getHalfEdgeChain(edge);
		int edgeChainParent = getChainParent(edgeChain);
		if (edgeChainParent != -1)
			return;// this edge has been processed already.

		// get contributing left edge.
		int leftEdge = getLeftSkipPolylines_(aet, treeNode);

		int twinEdge = getHalfEdgeTwin(edge);
		int twinHalfEdgeChain = getHalfEdgeChain(twinEdge);

		double chainArea = getChainArea(edgeChain);
		double twinChainArea = getChainArea(twinHalfEdgeChain);

		int parentChain = getChainParent(edgeChain);
		int twinParentChain = getChainParent(twinHalfEdgeChain);
		if (leftEdge == -1 && parentChain == -1) {
			// This edge/twin pair does not have a neighbour edge to the left.
			// twin parent is not yet been assigned.
			if (twinHalfEdgeChain == edgeChain) {// set parentage of a polyline
													// edge (any edge for which
													// the edge ant its twin
													// belong to the same chain)
				setChainParent_(twinHalfEdgeChain, getFirstChain());
				twinParentChain = getFirstChain();
				parentChain = twinParentChain;
			} else {
				// We have two touching chains that do not have parent chain
				// set.
				// The edge is directed up, the twin edge is directed down.
				// There is no edge to the left. THat means there is no other
				// than the universe surrounding this edge.
				// The edge must belong to a clockwise chain, and the twin edge
				// must belong to a ccw chain that encloses this edge. This
				// follows from the way how we connect edges around clusters.
				assert (twinChainArea < 0 && chainArea > 0);
				if (twinParentChain == -1) {
					setChainParent_(twinHalfEdgeChain, m_universeChain);
					twinParentChain = m_universeChain;
				} else {
					assert (getFirstChain() == twinParentChain);
				}

				setChainParent_(edgeChain, twinHalfEdgeChain);
				parentChain = twinHalfEdgeChain;
			}
		}

		if (leftEdge != -1) {
			int leftEdgeChain = getHalfEdgeChain(leftEdge);
			// the twin edge has not been processed yet
			if (twinParentChain == -1) {
				double leftArea = getChainArea(leftEdgeChain);
				if (leftArea <= 0) {// if left Edge's chain area is negative,
									// then it is a chain that ends at the left
									// edge, so we need to get the parent of the
									// left chain and it will be the parent of
									// this one.
					int leftChainParent = getChainParent(leftEdgeChain);
					assert (leftChainParent != -1);

					setChainParent_(twinHalfEdgeChain, leftChainParent);
					twinParentChain = leftChainParent;
				} else // (leftArea > 0)
				{// left edge is an edge of positive chain. It surrounds the
					// twin chain.
					setChainParent_(twinHalfEdgeChain, leftEdgeChain);
					twinParentChain = leftEdgeChain;
				}

				if (twinHalfEdgeChain == edgeChain) // if this is a polyline
													// chain
					parentChain = twinParentChain;
			}
		}

		if (parentChain == -1) {
			trySetChainParentFromTwin_(edgeChain, twinHalfEdgeChain);
			parentChain = getChainParent(edgeChain);
		}

		assert (parentChain != -1);

		if (inputMode == EnumInputMode.enumInputModeBuildGraph) {
			propagate_parentage_build_graph_(aet, treeNode, edge, leftEdge, edgeChain, edgeChainParent, twinHalfEdgeChain);
		}
		else if (inputMode == EnumInputMode.enumInputModeSimplifyWinding) {
			propagate_parentage_winding_(aet, treeNode, edge, leftEdge, twinEdge, edgeChain, edgeChainParent, twinHalfEdgeChain);
		}		
		else if (inputMode == EnumInputMode.enumInputModeSimplifyAlternate) {
			propagate_parentage_alternate_(aet, treeNode, edge, leftEdge, twinEdge, edgeChain, edgeChainParent, twinHalfEdgeChain);
		}		
		
	}
	
    void propagate_parentage_build_graph_(Treap aet, int treeNode, int edge, int leftEdge,
    	      int edgeChain, int edgeChainParent, int twinHalfEdgeChain) {
		// Now do specific sweep calculations
		int chainParentage = getChainParentage(edgeChain);

		if (leftEdge != -1) {
			// borrow the parentage from the left edge also
			int leftEdgeChain = getHalfEdgeChain(leftEdge);
	
			// We take parentage from the left edge (that edge has been
			// already processed), and move its face parentage accross this
			// edge/twin pair.
			// While the parentage is moved, accross, any bits of the
			// parentage that is present in the twin are removed, because
			// the twin is the right edge of the current face.
			// The remaining bits are added to the face parentage of this
			// edge, indicating that the face this edge borders, belongs to
			// all the parents that are still active to the left.
			int twinChainParentage = getChainParentage(twinHalfEdgeChain);
			int leftChainParentage = getChainParentage(leftEdgeChain);

			int edgeParentage = getHalfEdgeParentage(edge);
			int spikeParentage = chainParentage & twinChainParentage
					& leftChainParentage; // parentage that needs to stay
			leftChainParentage = leftChainParentage
						^ (leftChainParentage & edgeParentage);
			leftChainParentage |= spikeParentage;

			if (leftChainParentage != 0) {
				// propagate left parentage to the current edge and its
				// twin.
				setChainParentage_(twinHalfEdgeChain, twinChainParentage
							| leftChainParentage);
				setChainParentage_(edgeChain, leftChainParentage
							| chainParentage);
				chainParentage |= leftChainParentage;
			}

				// dbg_print_edge_(edge);
		}

		for (int rightNode = aet.getNext(treeNode); rightNode != -1; rightNode = aet
					.getNext(rightNode)) {
			int rightEdge = aet.getElement(rightNode);
			int rightTwin = getHalfEdgeTwin(rightEdge);

			int rightTwinChain = getHalfEdgeChain(rightTwin);
			int rightTwinChainParentage = getChainParentage(rightTwinChain);
			int rightEdgeParentage = getHalfEdgeParentage(rightEdge);
			int rightEdgeChain = getHalfEdgeChain(rightEdge);
			int rightChainParentage = getChainParentage(rightEdgeChain);

			int spikeParentage = rightTwinChainParentage
					& rightChainParentage & chainParentage; // parentage
															// that needs to
															// stay
			chainParentage = chainParentage
					^ (chainParentage & rightEdgeParentage);// only
															// parentage
															// that is
															// abscent in
															// the twin is
															// propagated to
															// the right
			chainParentage |= spikeParentage;

			if (chainParentage == 0)
				break;

			setChainParentage_(rightTwinChain, rightTwinChainParentage
						| chainParentage);
			setChainParentage_(rightEdgeChain, rightChainParentage
						| chainParentage);
		}
	}

    void propagate_parentage_winding_(Treap aet, int treeNode, int edge, int leftEdge, int twinEdge,
  	      int edgeChain, int edgeChainParent, int twinHalfEdgeChain) {
    
    	if (edgeChain == twinHalfEdgeChain)
			return;
		// starting from the left most edge, calculate winding.
		int edgeWinding = getHalfEdgeUserIndex(edge,
				m_tmpHalfEdgeWindingNumberIndex);
		edgeWinding += getHalfEdgeUserIndex(twinEdge,
				m_tmpHalfEdgeWindingNumberIndex);
		int winding = 0;
		AttributeStreamOfInt32 chainStack = new AttributeStreamOfInt32(0);
		AttributeStreamOfInt32 windingStack = new AttributeStreamOfInt32(0);
		windingStack.add(0);
		for (int leftNode = aet.getFirst(-1); leftNode != treeNode; leftNode = aet
					.getNext(leftNode)) {
			int leftEdge1 = aet.getElement(leftNode);
			int leftTwin = getHalfEdgeTwin(leftEdge1);
			int l_chain = getHalfEdgeChain(leftEdge1);
			int lt_chain = getHalfEdgeChain(leftTwin);

			if (l_chain != lt_chain) {
				int leftWinding = getHalfEdgeUserIndex(leftEdge1,
						m_tmpHalfEdgeWindingNumberIndex);
				leftWinding += getHalfEdgeUserIndex(leftTwin,
						m_tmpHalfEdgeWindingNumberIndex);
				winding += leftWinding;

				boolean popped = false;
				if (chainStack.size() != 0
						&& chainStack.getLast() == lt_chain) {
					windingStack.removeLast();
					chainStack.removeLast();
					popped = true;
				}

				if (getChainParent(lt_chain) == -1)
					throw GeometryException.GeometryInternalError();

				if (!popped || getChainParent(lt_chain) != l_chain) {
					windingStack.add(winding);
					chainStack.add(l_chain);
				}
			}
		}

		winding += edgeWinding;

		if (chainStack.size() != 0
					&& chainStack.getLast() == twinHalfEdgeChain) {
			windingStack.removeLast();
			chainStack.removeLast();
		}

		if (winding != 0) {
			if (windingStack.getLast() == 0) {
				int geometry = m_shape.getFirstGeometry();
				int geometryID = getGeometryID(geometry);
				setChainParentage_(edgeChain, geometryID);
			}
		} else {
			if (windingStack.getLast() != 0) {
				int geometry = m_shape.getFirstGeometry();
				int geometryID = getGeometryID(geometry);
				setChainParentage_(edgeChain, geometryID);
			}
		}
	}

	void propagate_parentage_alternate_(Treap aet, int treeNode, int edge,
			int leftEdge, int twinEdge, int edgeChain, int edgeChainParent,
			int twinHalfEdgeChain) {
		// Now do specific sweep calculations
		// This one is done when we are doing a topological operation.
		int geometry = m_shape.getFirstGeometry();
		int geometryID = getGeometryID(geometry);

		if (leftEdge == -1) {
			// no left edge neighbour means the twin chain is surrounded by the
			// universe
			assert (getChainParent(twinHalfEdgeChain) == m_universeChain);
			assert (getChainParentage(twinHalfEdgeChain) == 0 || getChainParentage(twinHalfEdgeChain) == m_universe_geomID);
			assert (getChainParentage(edgeChain) == 0);
			setChainParentage_(twinHalfEdgeChain, m_universe_geomID);
			int parity = getHalfEdgeUserIndex(edge,
					m_tmpHalfEdgeOddEvenNumberIndex);
			if ((parity & 1) != 0)
				setChainParentage_(edgeChain, geometryID);// set the parenentage
															// from the parity
			else
				setChainParentage_(edgeChain, m_universe_geomID);// this chain
																	// does not
																	// belong to
																	// geometry
		} else {
			int twin_parentage = getChainParentage(twinHalfEdgeChain);
			if (twin_parentage == 0) {
				int leftEdgeChain = getHalfEdgeChain(leftEdge);
				int left_parentage = getChainParentage(leftEdgeChain);
				setChainParentage_(twinHalfEdgeChain, left_parentage);
				int parity = getHalfEdgeUserIndex(edge,
						m_tmpHalfEdgeOddEvenNumberIndex);
				if ((parity & 1) != 0)
					setChainParentage_(edgeChain,
							(left_parentage == geometryID) ? m_universe_geomID
									: geometryID);
				else
					setChainParentage_(edgeChain, left_parentage);

			} else {
				int parity = getHalfEdgeUserIndex(edge,
						m_tmpHalfEdgeOddEvenNumberIndex);
				if ((parity & 1) != 0)
					setChainParentage_(edgeChain,
							(twin_parentage == geometryID) ? m_universe_geomID
									: geometryID);
				else
					setChainParentage_(edgeChain, twin_parentage);
			}

		}
	}

	boolean tryOptimizedInsertion_(Treap aet, int treeNodeIndex,
			AttributeStreamOfInt32 new_edges, int cluster, int firstHalfEdge) {
		int clusterHalfEdge = firstHalfEdge;
		int attachedTreeNode = -1;
		int newEdge = -1;
		// Delete all edges that end at the cluster.
		int count = 0;
		do {
			if (count == 2)
				return false;
			int n = getHalfEdgeUserIndex(clusterHalfEdge, treeNodeIndex);
			if (n != -1) {
				if (attachedTreeNode != -1)
					return false;// two edges end at the cluster
				attachedTreeNode = n;
			} else {
				if (newEdge != -1)
					return false; // two edges start from the cluster
				newEdge = clusterHalfEdge;
			}
			assert (getHalfEdgeOrigin(clusterHalfEdge) == cluster);
			count++;
			clusterHalfEdge = getHalfEdgeNext(getHalfEdgeTwin(clusterHalfEdge));
		} while (firstHalfEdge != clusterHalfEdge);

		if (newEdge == -1 || attachedTreeNode == -1)
			return false;

		setHalfEdgeUserIndex(aet.getElement(attachedTreeNode), treeNodeIndex,
				StridedIndexTypeCollection.impossibleIndex2());
		aet.setElement(attachedTreeNode, newEdge);
		new_edges.add(attachedTreeNode);
		return true;
	}

	boolean trySetChainParentFromTwin_(int chainToSet, int twinChain) {
		assert (getChainParent(chainToSet) == -1);
		double area = getChainArea(chainToSet);
		if (area == 0)
			return false;
		double twinArea = getChainArea(twinChain);
		assert (twinArea != 0);
		if (area > 0 && twinArea < 0) {
			setChainParent_(chainToSet, twinChain);
			return true;
		}
		if (area < 0 && twinArea > 0) {
			setChainParent_(chainToSet, twinChain);
			return true;
		} else {
			int twinParent = getChainParent(twinChain);
			if (twinParent != -1) {
				setChainParent_(chainToSet, twinParent);
				return true;
			}
		}

		return false;
	}

	void createHalfEdges_(int inputMode, AttributeStreamOfInt32 sorted_vertices) {
		// After this loop all halfedges will be created.
		// This loop also sets the known parentage on the edges.
		// The half edges are connected with each other in a random order
		m_halfEdgeIndex = m_shape.createUserIndex();

		for (int i = 0, nvert = sorted_vertices.size(); i < nvert; i++) {
			int vertex = sorted_vertices.get(i);
			int cluster = m_shape.getUserIndex(vertex, m_clusterIndex);

			int path = m_shape.getPathFromVertex(vertex);
			int geometry = m_shape.getGeometryFromPath(path);
			int gt = m_shape.getGeometryType(geometry);
			if (Geometry.isMultiPath(gt)) {
				int next = m_shape.getNextVertex(vertex);
				if (next == -1)
					continue;

				int clusterTo = m_shape.getUserIndex(next, m_clusterIndex);
				assert (clusterTo != -1);
                if (cluster == clusterTo) {
                    if (m_shape.getSegment(vertex) != null) {
                        assert (m_shape.getSegment(vertex).calculateLength2D() == 0);
                    } else {
                        assert (m_shape.getXY(vertex).isEqual(m_shape.getXY(next)));
                    }

                    continue;
                }

				int half_edge = newHalfEdgePair_();
				int twinEdge = getHalfEdgeTwin(half_edge);

				// add vertex to the half edge.
				int vertIndex = m_clusterVertices.newElement();
				m_clusterVertices.setField(vertIndex, 0, vertex);
				m_clusterVertices.setField(vertIndex, 1, -1);
				setHalfEdgeVertexIterator_(half_edge, vertIndex);

				setHalfEdgeOrigin_(half_edge, cluster);
				int firstHalfEdge = getClusterHalfEdge(cluster);
				if (firstHalfEdge == -1) {
					setClusterHalfEdge_(cluster, half_edge);
					setHalfEdgePrev_(half_edge, twinEdge);
					setHalfEdgeNext_(twinEdge, half_edge);
				} else {
					// It does not matter what order we insert the new edges in.
					// We fix the order later.
					int firstPrev = getHalfEdgePrev(firstHalfEdge);
					assert (getHalfEdgeNext(firstPrev) == firstHalfEdge);
					setHalfEdgePrev_(firstHalfEdge, twinEdge);
					setHalfEdgeNext_(twinEdge, firstHalfEdge);
					assert (getHalfEdgePrev(firstHalfEdge) == twinEdge);
					assert (getHalfEdgeNext(twinEdge) == firstHalfEdge);
					setHalfEdgeNext_(firstPrev, half_edge);
					setHalfEdgePrev_(half_edge, firstPrev);
					assert (getHalfEdgePrev(half_edge) == firstPrev);
					assert (getHalfEdgeNext(firstPrev) == half_edge);
				}

				setHalfEdgeOrigin_(twinEdge, clusterTo);
				int firstTo = getClusterHalfEdge(clusterTo);
				if (firstTo == -1) {
					setClusterHalfEdge_(clusterTo, twinEdge);
					setHalfEdgeNext_(half_edge, twinEdge);
					setHalfEdgePrev_(twinEdge, half_edge);
				} else {
					int firstToPrev = getHalfEdgePrev(firstTo);
					assert (getHalfEdgeNext(firstToPrev) == firstTo);
					setHalfEdgePrev_(firstTo, half_edge);
					setHalfEdgeNext_(half_edge, firstTo);
					assert (getHalfEdgePrev(firstTo) == half_edge);
					assert (getHalfEdgeNext(half_edge) == firstTo);
					setHalfEdgeNext_(firstToPrev, twinEdge);
					setHalfEdgePrev_(twinEdge, firstToPrev);
					assert (getHalfEdgePrev(twinEdge) == firstToPrev);
					assert (getHalfEdgeNext(firstToPrev) == twinEdge);
				}

				int geometryID = getGeometryID(geometry);
				// No chains yet exists, so we use a temporary user index to
				// store chain parentage.
				// The input polygons has been already simplified so their edges
				// directed such that the hole is to the left from the edge
				// (each edge is directed from the "from" to "to" point).
				if (inputMode == EnumInputMode.enumInputModeBuildGraph) {
					setHalfEdgeUserIndex(twinEdge, m_tmpHalfEdgeParentageIndex,
							0); // Hole is always to the left. left side here is
								// the twin.
					setHalfEdgeUserIndex(half_edge,
							m_tmpHalfEdgeParentageIndex,
							gt == Geometry.GeometryType.Polygon ? geometryID
									: 0);
				} else if (inputMode == EnumInputMode.enumInputModeSimplifyWinding) {
					Point2D pt_1 = new Point2D();
					m_shape.getXY(vertex, pt_1);
					Point2D pt_2 = new Point2D();
					m_shape.getXY(next, pt_2);
					int windingNumber = 0;
					int windingNumberTwin = 0;
					if (pt_1.compare(pt_2) < 0) {
						// The edge is directed bottom-up. That means it has the
						// winding number of +1.
						// The half-edge direction coincides with the edge
						// direction. THe twin is directed top-down.
						// The half edge will have the winding number of 1 and
						// its twin the winding number of 0.
						// When crossing the half-edge/twin pair from left to
						// right, the winding number is changed by +1
						windingNumber = 1;
					} else {
						// The edge is directed top-down. That means it has the
						// winding number of -1.
						// The half-edge direction coincides with the edge
						// direction. The twin is directed bottom-up.
						// The half edge will have the winding number of 0 and
						// its twin the winding number of -1.
						// When crossing the half-edge/twin pair from left to
						// right, the winding number is changed by -1.
						windingNumberTwin = -1;
					}

					// When we get a half-edge/twin pair, we can determine the
					// winding number of the underlying edge
					// by summing up the half-edge and twin's
					// winding numbers.

					setHalfEdgeUserIndex(twinEdge, m_tmpHalfEdgeParentageIndex,
							0);
					setHalfEdgeUserIndex(half_edge,
							m_tmpHalfEdgeParentageIndex, 0);
					// We split the winding number between the half edge and its
					// twin.
					// This allows us to determine which half edge goes in the
					// direction of the edge, and also it allows to calculate
					// the
					// winging number by summing up the winding number of half
					// edge and its twin.
					setHalfEdgeUserIndex(half_edge,
							m_tmpHalfEdgeWindingNumberIndex, windingNumber);
					setHalfEdgeUserIndex(twinEdge,
							m_tmpHalfEdgeWindingNumberIndex, windingNumberTwin);
				} else if (inputMode == EnumInputMode.enumInputModeIsSimplePolygon) {
					setHalfEdgeUserIndex(twinEdge, m_tmpHalfEdgeParentageIndex,
							m_universe_geomID);
					setHalfEdgeUserIndex(half_edge,
							m_tmpHalfEdgeParentageIndex,
							gt == Geometry.GeometryType.Polygon ? geometryID
									: 0);
				} else if (inputMode == EnumInputMode.enumInputModeSimplifyAlternate) {
					setHalfEdgeUserIndex(twinEdge, m_tmpHalfEdgeParentageIndex,
							0);
					setHalfEdgeUserIndex(half_edge,
							m_tmpHalfEdgeParentageIndex, 0);
					setHalfEdgeUserIndex(half_edge,
							m_tmpHalfEdgeOddEvenNumberIndex, 1);
					setHalfEdgeUserIndex(twinEdge,
							m_tmpHalfEdgeOddEvenNumberIndex, 1);
				}

				int edgeBit = gt == Geometry.GeometryType.Polygon ? c_edgeBitMask
						: 0;
				setHalfEdgeParentage_(half_edge, geometryID | edgeBit);
				setHalfEdgeParentage_(twinEdge, geometryID | edgeBit);
			}
		}
	}

	void mergeVertexListsOfEdges_(int eDst, int eSrc) {
		assert (getHalfEdgeTo(eDst) == getHalfEdgeTo(eSrc));
		assert (getHalfEdgeOrigin(eDst) == getHalfEdgeOrigin(eSrc));

		{
			int vertFirst2 = getHalfEdgeVertexIterator(eSrc);
			if (vertFirst2 != -1) {
				int vertFirst1 = getHalfEdgeVertexIterator(eDst);
				m_clusterVertices.setField(vertFirst2, 1, vertFirst1);
				setHalfEdgeVertexIterator_(eDst, vertFirst2);
				setHalfEdgeVertexIterator_(eSrc, -1);
			}
		}

		int eDstTwin = getHalfEdgeTwin(eDst);
		int eSrcTwin = getHalfEdgeTwin(eSrc);
		{
			int vertFirst2 = getHalfEdgeVertexIterator(eSrcTwin);
			if (vertFirst2 != -1) {
				int vertFirst1 = getHalfEdgeVertexIterator(eDstTwin);
				m_clusterVertices.setField(vertFirst2, 1, vertFirst1);
				setHalfEdgeVertexIterator_(eDstTwin, vertFirst2);
				setHalfEdgeVertexIterator_(eSrcTwin, -1);
			}
		}
	}

	void sortHalfEdgesByAngle_(int inputMode) {
		AttributeStreamOfInt32 angleSorter = new AttributeStreamOfInt32(0);
		angleSorter.reserve(10);
		TopoGraphAngleComparer tgac = new TopoGraphAngleComparer(this);
		// Now go through the clusters, sort edges in each cluster by angle, and
		// reconnect the halfedges of sorted edges in the sorted order.
		// Also share the parentage information between coinciding edges and
		// remove duplicates.
		for (int cluster = getFirstCluster(); cluster != -1; cluster = getNextCluster(cluster)) {
			angleSorter.clear(false);
			int first = getClusterHalfEdge(cluster);
			if (first != -1) {
				// 1. sort edges originating at the cluster by angle (counter -
				// clockwise).
				int edge = first;
				do {
					angleSorter.add(edge);// edges have the cluster in their
											// origin and are directed away from
											// it. The twin edges are directed
											// towards the cluster.
					edge = getHalfEdgeNext(getHalfEdgeTwin(edge));
				} while (edge != first);

				if (angleSorter.size() > 1) {
					boolean changed_order = true;
					if (angleSorter.size() > 2) {
						angleSorter.Sort(0, angleSorter.size(),
								tgac); // std::sort(angleSorter.get_ptr(),
																	// angleSorter.get_ptr()
																	// +
																	// angleSorter.size(),
																	// TopoGraphAngleComparer(this));
						angleSorter.add(angleSorter.get(0));
					} else {
						//no need to sort most two edge cases. we only need to make sure that edges going up are sorted
						if (compareEdgeAnglesForPair_(angleSorter.get(0),
								angleSorter.get(1)) > 0) {
							int tmp = angleSorter.get(0);
							angleSorter.set(0, angleSorter.get(1));
							angleSorter.set(1, tmp);
						}
						else {
							changed_order = false;
						}
					}
					// 2. get rid of duplicate edges by merging them (duplicate
					// edges appear at this step because we converted all
					// segments into the edges, including overlapping).
					int e0 = angleSorter.get(0);
					int ePrev = e0;
					int ePrevTo = getHalfEdgeTo(ePrev);
					int ePrevTwin = getHalfEdgeTwin(ePrev);
					int prevMerged = -1;
					for (int i = 1, n = angleSorter.size(); i < n; i++) {
						int e = angleSorter.get(i);
						int eTwin = getHalfEdgeTwin(e);
						int eTo = getHalfEdgeOrigin(eTwin);
						assert (getHalfEdgeOrigin(e) == getHalfEdgeOrigin(ePrev));// e
																					// origin
																					// and
																					// ePrev
																					// origin
																					// are
																					// equal
																					// by
																					// definition
																					// (e
																					// and
																					// ePrev
																					// emanate
																					// from
																					// the
																					// same
																					// cluster)
						if (eTo == ePrevTo && e != ePrev)// e's To cluster and
															// ePrev's To
															// cluster are
															// equal, means the
															// edges coincide
															// and need to be
															// merged.
						{// remove duplicate edge. Before removing, propagate
							// the parentage to the remaning edge
							if (inputMode == EnumInputMode.enumInputModeBuildGraph) {
								int newEdgeParentage = getHalfEdgeParentageMask_(ePrev)
										| getHalfEdgeParentageMask_(e);
								setHalfEdgeParentage_(ePrev, newEdgeParentage);
								setHalfEdgeParentage_(ePrevTwin,
										newEdgeParentage);
								assert (getHalfEdgeParentageMask_(ePrev) == getHalfEdgeParentageMask_(ePrevTwin));

								setHalfEdgeUserIndex(
										ePrev,
										m_tmpHalfEdgeParentageIndex,
										getHalfEdgeUserIndex(ePrev,
												m_tmpHalfEdgeParentageIndex)
												| getHalfEdgeUserIndex(e,
														m_tmpHalfEdgeParentageIndex));
								setHalfEdgeUserIndex(
										ePrevTwin,
										m_tmpHalfEdgeParentageIndex,
										getHalfEdgeUserIndex(ePrevTwin,
												m_tmpHalfEdgeParentageIndex)
												| getHalfEdgeUserIndex(eTwin,
														m_tmpHalfEdgeParentageIndex));
							} else if (m_tmpHalfEdgeWindingNumberIndex != -1) {
								// when doing simplify the
								// m_tmpHalfEdgeWindingNumberIndex contains the
								// winding number.
								// When edges are merged their winding numbers
								// are added.
								int newHalfEdgeWinding = getHalfEdgeUserIndex(
										ePrev, m_tmpHalfEdgeWindingNumberIndex)
										+ getHalfEdgeUserIndex(e,
												m_tmpHalfEdgeWindingNumberIndex);
								int newTwinEdgeWinding = getHalfEdgeUserIndex(
										ePrevTwin,
										m_tmpHalfEdgeWindingNumberIndex)
										+ getHalfEdgeUserIndex(eTwin,
												m_tmpHalfEdgeWindingNumberIndex);
								setHalfEdgeUserIndex(ePrev,
										m_tmpHalfEdgeWindingNumberIndex,
										newHalfEdgeWinding);
								setHalfEdgeUserIndex(ePrevTwin,
										m_tmpHalfEdgeWindingNumberIndex,
										newTwinEdgeWinding);
								// The winding number of an edge is a sum of the
								// winding numbers of the half edge and its
								// twin.
								// To determine which half edge direction
								// coincides with the edge direction, determine
								// which half edge has larger abs value of
								// winding number. If half edge and twin winding
								// numbers cancel each other, the edge winding
								// number is zero, meaning there are
								// even number of edges coinciding there and
								// half of them has opposite direction to
								// another half.
							} else if (inputMode == EnumInputMode.enumInputModeIsSimplePolygon) {
								m_non_simple_result = new NonSimpleResult(NonSimpleResult.Reason.CrossOver, cluster, -1);
								return;
							}
							else if (m_tmpHalfEdgeOddEvenNumberIndex != -1) {
								int newHalfEdgeWinding = getHalfEdgeUserIndex(
										ePrev, m_tmpHalfEdgeOddEvenNumberIndex)
										+ getHalfEdgeUserIndex(e,
												m_tmpHalfEdgeOddEvenNumberIndex);
								int newTwinEdgeWinding = getHalfEdgeUserIndex(
										ePrevTwin,
										m_tmpHalfEdgeOddEvenNumberIndex)
										+ getHalfEdgeUserIndex(eTwin,
												m_tmpHalfEdgeOddEvenNumberIndex);
								setHalfEdgeUserIndex(ePrev,
										m_tmpHalfEdgeOddEvenNumberIndex,
										newHalfEdgeWinding);
								setHalfEdgeUserIndex(ePrevTwin,
										m_tmpHalfEdgeOddEvenNumberIndex,
										newTwinEdgeWinding);
							}

							mergeVertexListsOfEdges_(ePrev, e);
							deleteEdgeImpl_(e);
							assert (n < 3 || e0 == angleSorter.getLast());
							prevMerged = ePrev;
							angleSorter.set(i, -1);
							if (e == e0) {
								angleSorter.set(0, -1);
								e0 = -1;
							}

							continue;
						}
						else {
							//edges do not coincide
						}
						
						updateVertexToHalfEdgeConnection_(prevMerged, false);
						prevMerged = -1;
						ePrev = e;
						ePrevTo = eTo;
						ePrevTwin = eTwin;
					}


					updateVertexToHalfEdgeConnection_(prevMerged, false);
					prevMerged = -1;
					
					if (!changed_order) {
						//small optimization to avoid reconnecting if nothing changed
						e0 = -1;
						for (int i = 0, n = angleSorter.size(); i < n; i++) {
							int e = angleSorter.get(i);
							if (e == -1)
								continue;
							e0 = e;
							break;
						}
						
						if (first != e0)
							setClusterHalfEdge_(cluster, e0);
						
						continue; //next cluster 
					}
						

					// 3. Reconnect edges in the sorted order. The edges are
					// sorted counter clockwise.
					// We connect them such that every right turn is made in the
					// clockwise order.
					// This guarantees that the smallest faces are clockwise.
					e0 = -1;
					for (int i = 0, n = angleSorter.size(); i < n; i++) {
						int e = angleSorter.get(i);
						if (e == -1)
							continue;
						if (e0 == -1) {
							e0 = e;
							ePrev = e0;
							ePrevTo = getHalfEdgeTo(ePrev);
							ePrevTwin = getHalfEdgeTwin(ePrev);
							continue;
						}
						
						if (e == ePrev) {
							// This condition can only happen if all edges in
							// the bunch coincide.
							assert (i == n - 1);
							continue;
						}
						
						int eTwin = getHalfEdgeTwin(e);
						int eTo = getHalfEdgeOrigin(eTwin);
						assert (getHalfEdgeOrigin(e) == getHalfEdgeOrigin(ePrev));
						assert (eTo != ePrevTo);
						setHalfEdgeNext_(ePrevTwin, e);
						setHalfEdgePrev_(e, ePrevTwin);
						ePrev = e;
						ePrevTo = eTo;
						ePrevTwin = eTwin;
			            if (inputMode == EnumInputMode.enumInputModeIsSimplePolygon)
			              {
			                int par1 = getHalfEdgeUserIndex(e, m_tmpHalfEdgeParentageIndex) |
			                		getHalfEdgeUserIndex(getHalfEdgePrev(e), m_tmpHalfEdgeParentageIndex);
			                if (par1 == (m_universe_geomID | 1))
			                {
			                  //violation of face parentage
			                  m_non_simple_result = new NonSimpleResult(NonSimpleResult.Reason.CrossOver, cluster, -1);
			                  return;
			                }
			              }
						
					}

					setClusterHalfEdge_(cluster, e0);// smallest angle goes
														// first.
				}
			}
		}
	}

	void buildChains_(int inputMode) {
		// Creates chains and puts them in the list of chains.
		// Does not set the chain parentage
		// Does not connect chains

		int firstChain = -1;
		int visitedHalfEdgeIndex = createUserIndexForHalfEdges();
		// Visit all the clusters
		for (int cluster = getFirstCluster(); cluster != -1; cluster = getNextCluster(cluster)) {
			// For each cluster visit all half edges on the cluster
			int first = getClusterHalfEdge(cluster);
			if (first != -1) {
				int edge = first;
				do {
					if (getHalfEdgeUserIndex(edge, visitedHalfEdgeIndex) != 1)// check
																				// if
																				// we
																				// have
																				// visited
																				// this
																				// halfedge
																				// already
					{// if we have not visited this halfedge yet, then we have
						// not created a chain for it yet.
						int chain = newChain_();// new chain's parentage is set
												// to 0.
						setChainHalfEdge_(chain, edge);// Note, the half-edge's
														// Origin is the lowest
														// point of the chain.
						setChainNext_(chain, firstChain);// add the new chain to
															// the list of
															// chains.
						if (firstChain != -1) {
							setChainPrev_(firstChain, chain);
						}
						firstChain = chain;
						// go thorough all halfedges until return back to the
						// same one. Thus forming a chain.
						int parentage = 0;
						int e = edge;
						do {
							// accumulate chain parentage from all the chain
							// edges m_tmpHalfEdgeParentageIndex.
							parentage |= getHalfEdgeUserIndex(e,
									m_tmpHalfEdgeParentageIndex);
							assert (getHalfEdgeUserIndex(e,
									visitedHalfEdgeIndex) != 1);
							setHalfEdgeChain_(e, chain);
							setHalfEdgeUserIndex(e, visitedHalfEdgeIndex, 1);// mark
																				// the
																				// edge
																				// visited.
							e = getHalfEdgeNext(e);
						} while (e != edge);
						
						assert(inputMode != EnumInputMode.enumInputModeIsSimplePolygon || parentage != (1 | m_universe_geomID));
						
						setChainParentage_(chain, parentage);
					}

					edge = getHalfEdgeNext(getHalfEdgeTwin(edge));// next
																	// halfedge
																	// on the
																	// cluster
				} while (edge != first);
			}
		}

		// add the Universe chain. We want it to be the one that getFirstChain
		// returns.
		int chain = newChain_();
		setChainHalfEdge_(chain, -1);
		setChainNext_(chain, firstChain);
		if (firstChain != -1)
			setChainPrev_(firstChain, chain);

		m_universeChain = chain;

		m_chainAreas = new AttributeStreamOfDbl(m_chainData.size(),
				NumberUtils.TheNaN);
		m_chainPerimeters = new AttributeStreamOfDbl(m_chainData.size(),
				NumberUtils.TheNaN);

		setChainArea_(m_universeChain, NumberUtils.positiveInf());// the
																	// Universe
																	// is
																	// infinite
		setChainPerimeter_(m_universeChain, NumberUtils.positiveInf());// the
																		// Universe
																		// is
																		// infinite

		deleteUserIndexForHalfEdges(visitedHalfEdgeIndex);
	}

	void simplify_(int inputMode) {
		if (inputMode == EnumInputMode.enumInputModeSimplifyAlternate) {
			simplifyAlternate_();
		} else if (inputMode == EnumInputMode.enumInputModeSimplifyWinding) {
			simplifyWinding_();
		}
	}

	void simplifyAlternate_() {
		//there is nothing to do
	}

	void simplifyWinding_() {
		//there is nothing to do
	}

	private int getFirstUnvisitedHalfEdgeOnCluster_(int cluster, int hintEdge,
			int vistiedEdgesIndex) {
		// finds first half edge which is unvisited (index is not set to 1.
		// when hintEdge != -1, it is used to start going around the edges.

		int edge = hintEdge != -1 ? hintEdge : getClusterHalfEdge(cluster);
		if (edge == -1)
			return -1;

		int f = edge;

		while (true) {
			int v = getHalfEdgeUserIndex(edge, vistiedEdgesIndex);
			if (v != 1) {
				return edge;
			}

			int next = getHalfEdgeNext(getHalfEdgeTwin(edge));
			if (next == f)
				return -1;

			edge = next;
		}
	}

	boolean removeSpikes_() {
		boolean removed = false;
		int visitedIndex = createUserIndexForHalfEdges();
		for (int cluster = getFirstCluster(); cluster != -1; cluster = getNextCluster(cluster)) {
			int nextClusterEdge = -1; //a hint
			while (true) {
				int firstHalfEdge = getFirstUnvisitedHalfEdgeOnCluster_(cluster, nextClusterEdge, visitedIndex);
				if (firstHalfEdge == -1)
					break;
	
				nextClusterEdge = getHalfEdgeNext(getHalfEdgeTwin(firstHalfEdge));
				int faceHalfEdge = firstHalfEdge;

				while (true) {
					int faceHalfEdgeNext = getHalfEdgeNext(faceHalfEdge);
					int faceHalfEdgePrev = getHalfEdgePrev(faceHalfEdge);
					int faceHalfEdgeTwin = getHalfEdgeTwin(faceHalfEdge);
					
					if (faceHalfEdgePrev == faceHalfEdgeTwin) {
						deleteEdgeInternal_(faceHalfEdge); //deletes the edge and its twin
						removed = true;
					
						if (nextClusterEdge == faceHalfEdge || nextClusterEdge == faceHalfEdgeTwin)
							nextClusterEdge = -1; //deleted the hint edge

						if (faceHalfEdge == firstHalfEdge || faceHalfEdgePrev == firstHalfEdge) {
							firstHalfEdge = faceHalfEdgeNext;
							if (faceHalfEdge == firstHalfEdge || faceHalfEdgePrev == firstHalfEdge) {
								//deleted all edges in a face
								break;
							}
							
							faceHalfEdge = faceHalfEdgeNext;
							continue;
						}

					}
					else {
						setHalfEdgeUserIndex(faceHalfEdge, visitedIndex, 1);
					}

					faceHalfEdge = faceHalfEdgeNext;
					if (faceHalfEdge == firstHalfEdge)
						break;
				}

			}
		}

		return removed;
	}
	
	void setEditShapeImpl_(EditShape shape, int inputMode,
			AttributeStreamOfInt32 editShapeGeometries,
			ProgressTracker progress_tracker, boolean bBuildChains) {
		assert(!m_dirty_check_failed);
		assert (editShapeGeometries == null || editShapeGeometries.size() > 0);

		removeShape();
		m_buildChains = bBuildChains;
		assert (m_shape == null);
		m_shape = shape;
		m_geometryIDIndex = m_shape.createGeometryUserIndex();
		// sort vertices lexicographically
		// Firstly copy all vertices to an array.
		AttributeStreamOfInt32 verticesSorter = new AttributeStreamOfInt32(0);
		verticesSorter.reserve(editShapeGeometries != null ? m_shape
				.getPointCount(editShapeGeometries.get(0)) : m_shape
				.getTotalPointCount());
		int path_count = 0;
		int geomID = 1;
		{// scope
			int geometry = editShapeGeometries != null ? editShapeGeometries
					.get(0) : m_shape.getFirstGeometry();
			int ind = 1;
			while (geometry != -1) {
				m_shape.setGeometryUserIndex(geometry, m_geometryIDIndex,
						geomID);
				geomID = geomID << 1;
				for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
						.getNextPath(path)) {
					int vertex = m_shape.getFirstVertex(path);
					for (int index = 0, n = m_shape.getPathSize(path); index < n; index++) {
						verticesSorter.add(vertex);
						vertex = m_shape.getNextVertex(vertex);
					}
				}

				if (!Geometry.isPoint(m_shape.getGeometryType(geometry)))
					path_count += m_shape.getPathCount(geometry);

				if (editShapeGeometries != null) {
					geometry = ind < editShapeGeometries.size() ? editShapeGeometries
							.get(ind) : -1;
					ind++;
				} else
					geometry = m_shape.getNextGeometry(geometry);
			}
		}
		
		m_universe_geomID = geomID;

		m_pointCount = verticesSorter.size();

		// sort
		m_shape.sortVerticesSimpleByY_(verticesSorter, 0, m_pointCount);

		if (m_clusterVertices == null) {
			m_clusterVertices = new StridedIndexTypeCollection(2);
			m_clusterData = new StridedIndexTypeCollection(8);
			m_halfEdgeData = new StridedIndexTypeCollection(8);
			m_chainData = new StridedIndexTypeCollection(8);
		}

		m_clusterVertices.setCapacity(m_pointCount);

		ProgressTracker.checkAndThrow(progress_tracker);

		m_clusterData.setCapacity(m_pointCount + 10);// 10 for some self
														// intersections
		m_halfEdgeData.setCapacity(2 * m_pointCount + 32);
		m_chainData.setCapacity(Math.max((int) 32, path_count));

		// create all clusters
		assert (m_clusterIndex == -1);// cleanup was incorrect
		m_clusterIndex = m_shape.createUserIndex();
		Point2D ptFirst = new Point2D();
		int ifirst = 0;
		Point2D pt = new Point2D();
		ptFirst.setNaN();
		for (int i = 0; i <= m_pointCount; i++) {
			if (i < m_pointCount) {
				int vertex = verticesSorter.get(i);
				m_shape.getXY(vertex, pt);
			} else {
				pt.setNaN();// makes it to go into the following "if" statement.
			}
			if (!ptFirst.isEqual(pt)) {
				if (ifirst < i) {
					int cluster = newCluster_();
					int vertFirst = -1;
					int vert = -1;
					for (int ind = ifirst; ind < i; ind++) {
						vert = verticesSorter.get(ind);
						m_shape.setUserIndex(vert, m_clusterIndex, cluster);

						// add vertex to the cluster's vertex list
						int vertIndex = m_clusterVertices.newElement();
						m_clusterVertices.setField(vertIndex, 0, vert);
						m_clusterVertices.setField(vertIndex, 1, vertFirst);
						vertFirst = vertIndex;

						int path = m_shape.getPathFromVertex(vert);
						int geometry = m_shape.getGeometryFromPath(path);
						int geometryID = getGeometryID(geometry);
						setClusterParentage_(cluster,
								getClusterParentage(cluster) | geometryID);
					}
					setClusterVertexIterator_(cluster, vertFirst);
					setClusterVertexIndex_(cluster,
							m_shape.getVertexIndex(vert));

					if (m_lastCluster != -1)
						setNextCluster_(m_lastCluster, cluster);

					setPrevCluster_(cluster, m_lastCluster);

					m_lastCluster = cluster;
					if (m_firstCluster == -1)
						m_firstCluster = cluster;
				}
				ifirst = i;
				ptFirst.setCoords(pt);
			}
		}
		
		ProgressTracker.checkAndThrow(progress_tracker);

		m_tmpHalfEdgeParentageIndex = createUserIndexForHalfEdges();
		if (inputMode == EnumInputMode.enumInputModeSimplifyWinding) {
			m_tmpHalfEdgeWindingNumberIndex = createUserIndexForHalfEdges();
		}

		if (inputMode == EnumInputMode.enumInputModeSimplifyAlternate) {
			m_tmpHalfEdgeOddEvenNumberIndex = createUserIndexForHalfEdges();
		}
		
		createHalfEdges_(inputMode, verticesSorter);// For each geometry produce
													// clusters and half edges

		if (m_non_simple_result.m_reason != NonSimpleResult.Reason.NotDetermined)
			return;
		
		sortHalfEdgesByAngle_(inputMode);
		if (m_non_simple_result.m_reason != NonSimpleResult.Reason.NotDetermined)
			return;

		if (!NumberUtils.isNaN(m_check_dirty_planesweep_tolerance)) {
			if (!check_structure_after_dirty_sweep_())// checks the edges.
			{
				m_dirty_check_failed = true;// set m_dirty_check_failed when an
											// issue is found. We'll rerun the
											// planesweep using robust crack and
											// cluster approach.
				return;
			}
		}

		buildChains_(inputMode);
		if (m_non_simple_result.m_reason != NonSimpleResult.Reason.NotDetermined)
			return;

		deleteUserIndexForHalfEdges(m_tmpHalfEdgeParentageIndex);
		m_tmpHalfEdgeParentageIndex = -1;

		if (m_buildChains)
			planeSweepParentage_(inputMode, progress_tracker);
		

		simplify_(inputMode);
	}

	void deleteEdgeImpl_(int half_edge) {
		int halfEdgeNext = getHalfEdgeNext(half_edge);
		int halfEdgePrev = getHalfEdgePrev(half_edge);
		int halfEdgeTwin = getHalfEdgeTwin(half_edge);
		int halfEdgeTwinNext = getHalfEdgeNext(halfEdgeTwin);
		int halfEdgeTwinPrev = getHalfEdgePrev(halfEdgeTwin);

		if (halfEdgeNext != halfEdgeTwin) {
			setHalfEdgeNext_(halfEdgeTwinPrev, halfEdgeNext);
			setHalfEdgePrev_(halfEdgeNext, halfEdgeTwinPrev);
		}

		if (halfEdgePrev != halfEdgeTwin) {
			setHalfEdgeNext_(halfEdgePrev, halfEdgeTwinNext);
			setHalfEdgePrev_(halfEdgeTwinNext, halfEdgePrev);
		}

		int cluster_1 = getHalfEdgeOrigin(half_edge);
		int clusterFirstEdge1 = getClusterHalfEdge(cluster_1);
		if (clusterFirstEdge1 == half_edge) {
			if (halfEdgeTwinNext != half_edge)
				setClusterHalfEdge_(cluster_1, halfEdgeTwinNext);
			else
				setClusterHalfEdge_(cluster_1, -1);// cluster has no more edges
		}

		int cluster2 = getHalfEdgeOrigin(halfEdgeTwin);
		int clusterFirstEdge2 = getClusterHalfEdge(cluster2);
		if (clusterFirstEdge2 == halfEdgeTwin) {
			if (halfEdgeNext != halfEdgeTwin)
				setClusterHalfEdge_(cluster2, halfEdgeNext);
			else
				setClusterHalfEdge_(cluster2, -1);// cluster has no more edges
		}

		m_halfEdgeData.deleteElement(half_edge);
		m_halfEdgeData.deleteElement(halfEdgeTwin);
	}

	int getLeftSkipPolylines_(Treap aet, int treeNode) {
		int leftNode = treeNode;

		for (;;) {
			leftNode = aet.getPrev(leftNode);
			if (leftNode != -1) {
				int e = aet.getElement(leftNode);
				int leftChain = getHalfEdgeChain(e);
				if (leftChain != getHalfEdgeChain(getHalfEdgeTwin(e))) {
					return e;
				} else {
					// the left edge is a piece of polyline - does not
					// contribute to the face parentage
				}
			} else {
				return -1;
			}
		}
	}

	TopoGraph() {
		c_edgeParentageMask = ((int) -1)
				^ ((int) 1 << (NumberUtils.sizeOf((int) 0) * 8 - 1));
		c_edgeBitMask = (int) 1 << (NumberUtils.sizeOf((int) 0) * 8 - 1);
		m_firstCluster = -1;
		m_lastCluster = -1;
		m_geometryIDIndex = -1;
		m_clusterIndex = -1;
		m_halfEdgeIndex = -1;
		m_universeChain = -1;
		m_tmpHalfEdgeParentageIndex = -1;
		m_tmpHalfEdgeWindingNumberIndex = -1;
		m_pointCount = 0;
	}

	EditShape getShape() {
		return m_shape;
	}

	// Sets an edit shape. The geometry has to be cracked and clustered before
	// calling this!
	void setEditShape(EditShape shape, ProgressTracker progress_tracker) {
		setEditShapeImpl_(shape, EnumInputMode.enumInputModeBuildGraph, null,
				progress_tracker, true);
	}

	void setEditShape(EditShape shape, ProgressTracker progress_tracker, boolean bBuildChains) {
		setEditShapeImpl_(shape, EnumInputMode.enumInputModeBuildGraph, null,
				progress_tracker, bBuildChains);
	}
	
	void setAndSimplifyEditShapeAlternate(EditShape shape, int geometry, ProgressTracker progressTracker) {
		AttributeStreamOfInt32 geoms = new AttributeStreamOfInt32(0);
		geoms.add(geometry);
		setEditShapeImpl_(shape, EnumInputMode.enumInputModeSimplifyAlternate,
				geoms, progressTracker, shape.getGeometryType(geometry) == Geometry.Type.Polygon.value());
	}

	void setAndSimplifyEditShapeWinding(EditShape shape, int geometry, ProgressTracker progressTracker) {
		AttributeStreamOfInt32 geoms = new AttributeStreamOfInt32(0);
		geoms.add(geometry);
		setEditShapeImpl_(shape, EnumInputMode.enumInputModeSimplifyWinding,
				geoms, progressTracker, true);
	}

	// Removes shape from the topograph and removes any user index created on
	// the edit shape.
	void removeShape() {
		if (m_shape == null)
			return;

		if (m_geometryIDIndex != -1) {
			m_shape.removeGeometryUserIndex(m_geometryIDIndex);
			m_geometryIDIndex = -1;
		}

		if (m_clusterIndex != -1) {
			m_shape.removeUserIndex(m_clusterIndex);
			m_clusterIndex = -1;
		}

		if (m_halfEdgeIndex != -1) {
			m_shape.removeUserIndex(m_halfEdgeIndex);
			m_halfEdgeIndex = -1;
		}

		if (m_tmpHalfEdgeParentageIndex != -1) {
			deleteUserIndexForHalfEdges(m_tmpHalfEdgeParentageIndex);
			m_tmpHalfEdgeParentageIndex = -1;
		}

		if (m_tmpHalfEdgeWindingNumberIndex != -1) {
			deleteUserIndexForHalfEdges(m_tmpHalfEdgeWindingNumberIndex);
			m_tmpHalfEdgeWindingNumberIndex = -1;
		}

		if (m_tmpHalfEdgeOddEvenNumberIndex != -1) {
			deleteUserIndexForHalfEdges(m_tmpHalfEdgeOddEvenNumberIndex);
			m_tmpHalfEdgeOddEvenNumberIndex = -1;
		}
		
		m_shape = null;
		m_clusterData.deleteAll(true);
		m_clusterVertices.deleteAll(true);
		m_firstCluster = -1;
		m_lastCluster = -1;

		if (m_halfEdgeData != null)
			m_halfEdgeData.deleteAll(true);
		if (m_edgeIndices != null)
			m_edgeIndices.clear();
		if (m_clusterIndices != null)
			m_clusterIndices.clear();
		if (m_chainIndices != null)
			m_chainIndices.clear();
		if (m_chainData != null)
			m_chainData.deleteAll(true);
		m_universeChain = -1;
		m_chainAreas = null;
	}

	// Returns a half-edge emanating the cluster. All other half-edges can be
	// visited with:
	// incident_half_edge = getHalfEdgeTwin(half_edge);//get twin of the
	// half_edge, it has the vertex as the end point.
	// emanating_half_edge = getHalfEdgeTwin(incident_half_edge); //get next
	// emanating half-edge
	int getClusterHalfEdge(int cluster) {
		return m_clusterData.getField(cluster, 2);
	}

	// Returns the coordinates of the cluster
	void getXY(int cluster, Point2D pt) {
		int vindex = getClusterVertexIndex_(cluster);
		m_shape.getXYWithIndex(vindex, pt);
	}

	// Returns parentage mask of the cluster
	int getClusterParentage(int cluster) {
		return m_clusterData.getField(cluster, 1);
	}

	// Returns first cluster in the Topo_graph (has lowest y, x coordinates).
	int getFirstCluster() {
		return m_firstCluster;
	}

	// Returns previous cluster in the Topo_graph (in the sorted order of y,x
	// coordinates).
	int getPrevCluster(int cluster) {
		return m_clusterData.getField(cluster, 3);
	}

	// Returns next cluster in the Topo_graph (in the sorted order of y,x
	// coordinates).
	int getNextCluster(int cluster) {
		return m_clusterData.getField(cluster, 4);
	}

	// Returns an exterior chain of a face this cluster belongs to (belongs only
	// to interior). set only for the clusters that are standalone clusters (do
	// not have half-edges with them).
	int getClusterChain(int cluster) {
		return m_clusterData.getField(cluster, 6);
	}

	// Returns iterator for cluster vertices
	int getClusterVertexIterator(int cluster) {
		return m_clusterData.getField(cluster, 7);
	}

	// Increments iterator. Returns -1 if no more vertices in the cluster
	int incrementVertexIterator(int vertexIterator) {
		return m_clusterVertices.getField(vertexIterator, 1);
	}

	// Dereference the iterator
	int getVertexFromVertexIterator(int vertexIterator) {
		return m_clusterVertices.getField(vertexIterator, 0);
	}

	// Returns a user index value for the cluster.
	int getClusterUserIndex(int cluster, int index) {
		int i = getClusterIndex_(cluster);
		AttributeStreamOfInt32 stream = m_clusterIndices.get(index);
		if (stream.size() <= i)
			return -1;
		return stream.read(i);
	}

	// Sets a user index value for the cluster.
	void setClusterUserIndex(int cluster, int index, int value) {
		int i = getClusterIndex_(cluster);
		AttributeStreamOfInt32 stream = m_clusterIndices.get(index);
		if (stream.size() <= i)
			stream.resize(m_clusterData.size(), -1);

		stream.write(i, value);
	}

	// Creates a new user index for the cluster. The index values are set to -1.
	int createUserIndexForClusters() {
		if (m_clusterIndices == null) {
			m_clusterIndices = new ArrayList<AttributeStreamOfInt32>(3);
		}

		AttributeStreamOfInt32 new_stream = new AttributeStreamOfInt32(
				m_clusterData.capacity(), -1);
		for (int i = 0, n = m_clusterIndices.size(); i < n; i++) {
			if (m_clusterIndices.get(i) == null) {
				m_clusterIndices.set(i, new_stream);
				return i;
			}
		}
		m_clusterIndices.add(new_stream);
		return m_clusterIndices.size() - 1;
	}

	// Deletes user index
	void deleteUserIndexForClusters(int userIndex) {
		assert (m_clusterIndices.get(userIndex) != null);
		m_clusterIndices.set(userIndex, null);
	}

	// Returns origin of this half edge. To get the other end:
	// incident_half_edge = getHalfEdgeTwin(half_edge);
	// edge_end_point = getHalfEdgeOrigin(incident_half_edge);
	int getHalfEdgeOrigin(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 1);
	}

	// Returns the to point of the half edge
	int getHalfEdgeTo(int half_edge) {
		return getHalfEdgeOrigin(getHalfEdgeTwin(half_edge));
	}

	// Twin of this halfedge, it has opposite direction and same endpoints
	int getHalfEdgeTwin(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 4);
	}

	// Returns previous halfedge. It ends, where this halfedge starts.
	int getHalfEdgePrev(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 5);
	}

	// Returns next halfedge. It starts, where this halfedge ends.
	int getHalfEdgeNext(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 6);
	}

	// Returns half edge chain. Chain is on the right from the halfedge
	int getHalfEdgeChain(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 2);
	}

	// Returns half edge chain parentage. The call is implemented as as
	// getChainParentage(getHalfEdgeChain());
	int getHalfEdgeFaceParentage(int half_edge) {
		return getChainParentage(m_halfEdgeData.getField(half_edge, 2));
	}

	// Returns iterator for cluster vertices
	int getHalfEdgeVertexIterator(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 7);
	}

	// Returns the coordinates of the origin of the half_edge
	void getHalfEdgeFromXY(int half_edge, Point2D pt) {
		getXY(getHalfEdgeOrigin(half_edge), pt);
	}

	// Returns the coordinates of the end of the half_edge
	void getHalfEdgeToXY(int half_edge, Point2D pt) {
		getXY(getHalfEdgeTo(half_edge), pt);
	}

	// Returns parentage mask of this halfedge. Parentage mask of halfedge and
	// its twin are the same
	int getHalfEdgeParentage(int half_edge) {
		return m_halfEdgeData.getField(half_edge, 3) & c_edgeParentageMask;
	}

	// Returns a user index value for the half edge
	int getHalfEdgeUserIndex(int half_edge, int index) {
		int i = getHalfEdgeIndex_(half_edge);
		AttributeStreamOfInt32 stream = m_edgeIndices.get(index);
		if (stream.size() <= i)
			return -1;

		return stream.read(i);
	}

	// Sets a user index value for a half edge
	void setHalfEdgeUserIndex(int half_edge, int index, int value) {
		int i = getHalfEdgeIndex_(half_edge);
		AttributeStreamOfInt32 stream = m_edgeIndices.get(index);
		if (stream.size() <= i)
			stream.resize(m_halfEdgeData.size(), -1);

		stream.write(i, value);
	}

	// create a new user index for half edges. The index values are set to -1.
	int createUserIndexForHalfEdges() {
		if (m_edgeIndices == null)
			m_edgeIndices = new ArrayList<AttributeStreamOfInt32>(3);

		AttributeStreamOfInt32 new_stream = new AttributeStreamOfInt32(
				m_halfEdgeData.capacity(), -1);
		for (int i = 0, n = m_edgeIndices.size(); i < n; i++) {
			if (m_edgeIndices.get(i) == null) {
				m_edgeIndices.set(i, new_stream);
				return i;
			}
		}
		m_edgeIndices.add(new_stream);
		return m_edgeIndices.size() - 1;
	}

	// Deletes the given user index for half edges
	void deleteUserIndexForHalfEdges(int userIndex) {
		assert (m_edgeIndices.get(userIndex) != null);
		m_edgeIndices.set(userIndex, null);
	}

	// Deletes the half_edge and it's twin. It works presently when removing a
	// spike only.
	// Returns next valid half-edge, or -1 if no more half edges.
	// Use with care.
	int deleteEdgeInternal_(int half_edge) {
		int chain = getHalfEdgeChain(half_edge);
		int halfEdgeTwin = getHalfEdgeTwin(half_edge);
		int chainTwin = getHalfEdgeChain(halfEdgeTwin);
		// This function only works for spikes. These two asserts check for that
		assert (chainTwin == chain);
		assert (half_edge == getHalfEdgeNext(halfEdgeTwin) || halfEdgeTwin == getHalfEdgeNext(half_edge));

		int n = getHalfEdgeNext(half_edge);
		if (n == halfEdgeTwin) {
			n = getHalfEdgeNext(n);
			if (n == half_edge)
				n = -1;
		}

		if (getChainHalfEdge(chain) == half_edge) {
			setChainHalfEdge_(chain, n);
		}

		int chainIndex = getChainIndex_(chain);
		double v = m_chainAreas.read(chainIndex);
		if (!NumberUtils.isNaN(v)) {
			setChainArea_(chain, NumberUtils.TheNaN);
			setChainPerimeter_(chain, NumberUtils.TheNaN);
		}

		updateVertexToHalfEdgeConnection_(half_edge, true);

		deleteEdgeImpl_(half_edge);// does not change chain information
		return n;
	}

	// Deletes the halfEdges and their twin. The chains are broken after this
	// call.
	// For every chain the halfedges belong to, it will set the first edge to
	// -1.
	// However, the halfedge will still reference the chain so one can get the
	// parentage information still.
	void deleteEdgesBreakFaces_(AttributeStreamOfInt32 edgesToDelete) {
		for (int i = 0, n = edgesToDelete.size(); i < n; i++) {
			int half_edge = edgesToDelete.get(i);
			int chain = getHalfEdgeChain(half_edge);
			int halfEdgeTwin = getHalfEdgeTwin(half_edge);
			int chainTwin = getHalfEdgeChain(halfEdgeTwin);
			setChainHalfEdge_(chain, -1);
			setChainHalfEdge_(chainTwin, -1);
			updateVertexToHalfEdgeConnection_(half_edge, true);
			deleteEdgeImpl_(half_edge);
		}
	}

	boolean doesHalfEdgeBelongToAPolygonInterior(int half_edge, int polygonId) {
		// Half edge belongs to polygon interior if both it and its twin belong
		// to boundary of faces that have the polygon parentage (the poygon both
		// to the left and to the right of the edge).
		int p_1 = getHalfEdgeFaceParentage(half_edge);
		int p_2 = getHalfEdgeFaceParentage(getHalfEdgeTwin(half_edge));
		return (p_1 & polygonId) != 0 && (p_2 & polygonId) != 0;
	}

	boolean doesHalfEdgeBelongToAPolygonExterior(int half_edge, int polygonId) {
		// Half edge belongs to polygon interior if both it and its twin belong
		// to boundary of faces that have the polygon parentage (the poygon both
		// to the left and to the right of the edge).
		int p_1 = getHalfEdgeFaceParentage(half_edge);
		int p_2 = getHalfEdgeFaceParentage(getHalfEdgeTwin(half_edge));
		return (p_1 & polygonId) == 0 && (p_2 & polygonId) == 0;
	}

	boolean doesHalfEdgeBelongToAPolygonBoundary(int half_edge, int polygonId) {
		// Half edge overlaps polygon boundary
		int p_1 = getHalfEdgeParentage(half_edge);
		return (p_1 & polygonId) != 0;
	}

	boolean doesHalfEdgeBelongToAPolylineInterior(int half_edge, int polylineId) {
		// Half-edge belongs to a polyline interioir if it has the polyline
		// parentage (1D intersection (aka overlap)).
		int p_1 = getHalfEdgeParentage(half_edge);
		if ((p_1 & polylineId) != 0) {
			return true;
		}

		return false;
	}

	boolean doesHalfEdgeBelongToAPolylineExterior(int half_edge, int polylineId) {
		// Half-edge belongs to a polyline Exterioir if it does not have the
		// polyline parentage and both its clusters also do not have polyline's
		// parentage (to exclude touch at point).
		int p_1 = getHalfEdgeParentage(half_edge);
		if ((p_1 & polylineId) == 0) {
			int c = getHalfEdgeOrigin(half_edge);
			int pc = getClusterParentage(c);
			if ((pc & polylineId) == 0) {
				c = getHalfEdgeTo(half_edge);
				pc = getClusterParentage(c);
				if ((pc & polylineId) == 0) {
					return true;
				}
			}
		}

		return false;
	}

	boolean doesClusterBelongToAPolygonInterior(int cluster, int polygonId) {
		// cluster belongs to a polygon interior when
		// 1) It is a standalone cluster that has face parentage of this polygon
		// GetClusterFaceParentage()
		// 2) or It is a cluster with half edges attached and
		// a) It is not on the polygon boundrary (get_cluster_parentage)
		// b) Any half edge associated with it has face parentage of the polygon
		// (get_half_edge_face_parentage(getClusterHalfEdge()))

		int chain = getClusterChain(cluster);
		if (chain != -1) {
			if ((getChainParentage(chain) & polygonId) != 0) {
				return true;
			}
		} else {
			int p_1 = getClusterParentage(cluster);
			if ((p_1 & polygonId) == 0)// not on the polygon boundary
			{
				int half_edge = getClusterHalfEdge(cluster);
				assert (half_edge != -1);

				int p_2 = getHalfEdgeFaceParentage(half_edge);
				if ((p_2 & polygonId) != 0) {
					return true;
				}
			}
		}

		return false;
	}

	boolean doesClusterBelongToAPolygonExterior(int cluster, int polygonId) {
		int p_1 = getClusterParentage(cluster);
		if ((p_1 & polygonId) == 0) {
			return doesClusterBelongToAPolygonInterior(cluster, polygonId);
		}

		return false;
	}

	boolean doesClusterBelongToAPolygonBoundary(int cluster, int polygonId) {
		int p_1 = getClusterParentage(cluster);
		if ((p_1 & polygonId) != 0) {
			return true;
		}

		return false;
	}

	// bool DoesClusterBelongToAPolylineInterioir(int cluster, int polylineId);
	// bool does_cluster_belong_to_a_polyline_exterior(int cluster, int
	// polylineId);
	// bool does_cluster_belong_to_a_polyline_boundary(int cluster, int
	// polylineId);

	// Returns the first chain, which is always the Universe chain.
	int getFirstChain() {
		return m_universeChain;
	}

	// Returns the chain half edge.
	int getChainHalfEdge(int chain) {
		return m_chainData.getField(chain, 1);
	}

	// Returns the chain's face parentage. That is the parentage of a face this
	// chain borders with.
	int getChainParentage(int chain) {
		return m_chainData.getField(chain, 2);
	}

	// Returns the parent of the chain (the chain, this chain is inside of).
	int getChainParent(int chain) {
		return m_chainData.getField(chain, 3);
	}

	// Returns the first island chain in that chain. Island chains are always
	// counterclockwise.
	// Each island chain will have its complement chain, which is a chain of a
	// twin of any halfedge of that chain.
	int getChainFirstIsland(int chain) {
		return m_chainData.getField(chain, 4);
	}

	// Returns the first island chain in that chain. Island chains are always
	// counterclockwise.
	int getChainNextInParent(int chain) {
		return m_chainData.getField(chain, 5);
	}

	// Returns the next chain in arbitrary order.
	int getChainNext(int chain) {
		return m_chainData.getField(chain, 7);
	}

	// Returns the area of the chain. The area does not include any islands.
	// +Inf is returned for the universe chain.
	double getChainArea(int chain) {
		int chainIndex = getChainIndex_(chain);
		double v = m_chainAreas.read(chainIndex);
		if (NumberUtils.isNaN(v)) {
			updateChainAreaAndPerimeter_(chain);
			v = m_chainAreas.read(chainIndex);
		}

		return v;
	}

	// Returns the perimeter of the chain (> 0). +Inf is returned for the
	// universe chain.
	double getChainPerimeter(int chain) {
		int chainIndex = getChainIndex_(chain);
		double v = m_chainPerimeters.read(chainIndex);
		if (NumberUtils.isNaN(v)) {
			updateChainAreaAndPerimeter_(chain);
			v = m_chainPerimeters.read(chainIndex);
		}

		return v;
	}

	// Returns a user index value for the chain.
	int getChainUserIndex(int chain, int index) {
		int i = getChainIndex_(chain);
		AttributeStreamOfInt32 stream = m_chainIndices.get(index);
		if (stream.size() <= i)
			return -1;
		return stream.read(i);
	}

	// Sets a user index value for the chain.
	void setChainUserIndex(int chain, int index, int value) {
		int i = getChainIndex_(chain);
		AttributeStreamOfInt32 stream = m_chainIndices.get(index);
		if (stream.size() <= i)
			stream.resize(m_chainData.size(), -1);

		stream.write(i, value);
	}

	// Creates a new user index for the chains. The index values are set to -1.
	int createUserIndexForChains() {
		if (m_chainIndices == null) {
			m_chainIndices = new ArrayList<AttributeStreamOfInt32>(3);
		}

		AttributeStreamOfInt32 new_stream = new AttributeStreamOfInt32(
				m_chainData.capacity(), -1);
		for (int i = 0, n = m_chainIndices.size(); i < n; i++) {
			if (m_chainIndices.get(i) == null) {
				m_chainIndices.set(i, new_stream);
				return i;
			}
		}
		m_chainIndices.add(new_stream);
		return m_chainIndices.size() - 1;
	}

	// Deletes user index
	void deleteUserIndexForChains(int userIndex) {
		assert (m_chainIndices.get(userIndex) != null);
		m_chainIndices.set(userIndex, null);
	}

	// Returns geometry ID mask from the geometry handle.
	// Topo_graph creates a user index for geometries in the shape, which exists
	// until the topo graph is destroyed.
	int getGeometryID(int geometry) {
		return m_shape.getGeometryUserIndex(geometry, m_geometryIDIndex);
	}

	// Returns cluster from vertex handle.
	// Topo_graph creates a user index for vertices in the shape to hold cluster
	// handles. The index exists until the topo graph is destroyed.
	int getClusterFromVertex(int vertex) {
		return m_shape.getUserIndex(vertex, m_clusterIndex);
	}

	int getHalfEdgeFromVertex(int vertex) {
		return m_shape.getUserIndex(vertex, m_halfEdgeIndex);
	}

	// Finds an edge connecting the two clusters. Returns -1 if not found.
	// Could be a slow operation when valency of each cluster is high.
	int getHalfEdgeConnector(int clusterFrom, int clusterTo) {
		int first_edge = getClusterHalfEdge(clusterFrom);
		if (first_edge == -1)
			return -1;
		int edge = first_edge;
		int firstEdgeTo = -1;
		int eTo = -1;
		// Doing two loops in parallel - one on the half-edges attached to the
		// clusterFrom, another - attached to clusterTo.
		do {
			if (getHalfEdgeTo(edge) == clusterTo)
				return edge;

			if (firstEdgeTo == -1) {
				firstEdgeTo = getClusterHalfEdge(clusterTo);
				if (firstEdgeTo == -1)
					return -1;
				eTo = firstEdgeTo;
			}

			if (getHalfEdgeTo(eTo) == clusterFrom) {
				edge = getHalfEdgeTwin(eTo);
				assert (getHalfEdgeTo(edge) == clusterTo && getHalfEdgeOrigin(edge) == clusterFrom);
				return edge;
			}

			edge = getHalfEdgeNext(getHalfEdgeTwin(edge));
			eTo = getHalfEdgeNext(getHalfEdgeTwin(eTo));
		} while (edge != first_edge && eTo != firstEdgeTo);

		return -1;
	}

	// Queries segment for the edge (only xy coordinates, no attributes)
	void querySegmentXY(int half_edge, SegmentBuffer outBuffer) {
		outBuffer.createLine();
		Segment seg = outBuffer.get();
		Point2D pt = new Point2D();
		getHalfEdgeFromXY(half_edge, pt);
		seg.setStartXY(pt);
		getHalfEdgeToXY(half_edge, pt);
		seg.setEndXY(pt);
	}

	int compareEdgeAngles_(int edge1, int edge2) {
		if (edge1 == edge2)
			return 0;

		Point2D pt_1 = new Point2D();
		getHalfEdgeToXY(edge1, pt_1);

		Point2D pt_2 = new Point2D();
		getHalfEdgeToXY(edge2, pt_2);

		if (pt_1.isEqual(pt_2))
			return 0;// overlap case

		Point2D pt10 = new Point2D();
		getHalfEdgeFromXY(edge1, pt10);

		Point2D v_1 = new Point2D();
		v_1.sub(pt_1, pt10);
		Point2D v_2 = new Point2D();
		v_2.sub(pt_2, pt10);
		int result = Point2D._compareVectors(v_1, v_2);
		return result;
	}
	
	int compareEdgeAnglesForPair_(int edge1, int edge2) {
		if (edge1 == edge2)
			return 0;

		Point2D pt_1 = new Point2D();
		getHalfEdgeToXY(edge1, pt_1);

		Point2D pt_2 = new Point2D();
		getHalfEdgeToXY(edge2, pt_2);

		if (pt_1.isEqual(pt_2))
			return 0;// overlap case

		Point2D pt10 = new Point2D();
		getHalfEdgeFromXY(edge1, pt10);

		Point2D v_1 = new Point2D();
		v_1.sub(pt_1, pt10);
		Point2D v_2 = new Point2D();
		v_2.sub(pt_2, pt10);
		
		if (v_2.y >= 0 && v_1.y > 0) {
			int result = Point2D._compareVectors(v_1, v_2);
			return result;
		}
		else {
			return 0;
		}
	}
	
	boolean check_structure_after_dirty_sweep_() {
		// for each cluster go through the cluster half edges and check that
		// min(edge1_length, edge2_length) * angle_between is less than
		// m_check_dirty_planesweep_tolerance.
		// Doing this helps us weed out cases missed by the dirty plane sweep.
		// We do not need absolute accuracy here.
		assert (!m_dirty_check_failed);
		assert (!NumberUtils.isNaN(m_check_dirty_planesweep_tolerance));
		double sqr_tol = MathUtils.sqr(m_check_dirty_planesweep_tolerance);
		Point2D pt10 = new Point2D();
		Point2D pt_2 = new Point2D();
		Point2D pt_1 = new Point2D();
		Point2D v_1 = new Point2D();
		Point2D v_2 = new Point2D();
		for (int cluster = getFirstCluster(); cluster != -1; cluster = getNextCluster(cluster)) {
			int first = getClusterHalfEdge(cluster);
			if (first != -1) {
				int edge = first;
				getHalfEdgeFromXY(edge, pt10);
				getHalfEdgeToXY(edge, pt_2);
				v_2.sub(pt_2, pt10);
				double sqr_len2 = v_2.sqrLength();

				do {
					int prev = edge;
					edge = getHalfEdgeNext(getHalfEdgeTwin(edge));

					if (edge != prev) {
						getHalfEdgeToXY(edge, pt_1);
						assert (!pt_1.isEqual(pt_2));
						v_1.sub(pt_1, pt10);
						double sqr_len1 = v_1.sqrLength();

						double cross = v_1.crossProduct(v_2); // cross_prod =
																// len1 * len2 *
																// sinA => sinA
																// = cross_prod
																// / (len1 *
																// len2);
						double sqr_sinA = (cross * cross)
								/ (sqr_len1 * sqr_len2); // sqr_sinA is
															// approximately A^2
															// especially for
															// smaller angles
						double sqr_dist = Math.min(sqr_len1, sqr_len2)
								* sqr_sinA;
						if (sqr_dist <= sqr_tol) {
							// these edges incident on the cluster form a narrow
							// wedge and thei require cracking event that was
							// missed.
							return false;
						}

						v_2.setCoords(v_1);
						sqr_len2 = sqr_len1;
						pt_2.setCoords(pt_1);
					}
				} while (edge != first);
			}
		}

		return true;
	}
	
}
