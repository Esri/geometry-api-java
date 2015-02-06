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

final class PlaneSweepCrackerHelper {
	PlaneSweepCrackerHelper() {
		m_edges = new StridedIndexTypeCollection(8);
		m_clusters = new StridedIndexTypeCollection(5);
		m_cluster_vertices = new IndexMultiList();
		m_edge_vertices = new IndexMultiList();
		m_complications = false;
		m_sweep_point = new Point2D();
		m_sweep_point.setCoords(0, 0);
		m_tolerance = 0;
		m_vertex_cluster_index = -1;
		m_b_cracked = false;
		m_shape = null;

		m_event_q = new Treap();
		m_sweep_structure = new Treap();
		m_edges_to_insert_in_sweep_structure = new AttributeStreamOfInt32(0);
		m_segment_intersector = new SegmentIntersector();
		m_temp_edge_buffer = new AttributeStreamOfInt32(0);
		m_modified_clusters = new AttributeStreamOfInt32(0);
		m_helper_point = new Point();
	}

	// For use in Cluster/Cracker loop
	boolean sweep(EditShape shape, double tolerance) {
		Transformation2D transform = new Transformation2D();
		transform.setSwapCoordinates();
		shape.applyTransformation(transform);// swap coordinates for the sweep
												// along x
		setEditShape_(shape);
		m_b_cracked = false;
		m_tolerance = tolerance;
		m_tolerance_sqr = tolerance * tolerance;

		boolean b_cracked = sweepImpl_();
		shape.applyTransformation(transform);
		if (!b_cracked) {
			fillEventQueuePass2();
			b_cracked |= sweepImpl_();
		}
		
		if (m_vertex_cluster_index != -1) {
			m_shape.removeUserIndex(m_vertex_cluster_index);
			m_vertex_cluster_index = -1;
		}
		
		m_shape = null;
		return m_b_cracked;
	}

	// Does one pass sweep vertically
	boolean sweepVertical(EditShape shape, double tolerance) {
		setEditShape_(shape);
		m_b_cracked = false;
		m_tolerance = tolerance;
		m_tolerance_sqr = tolerance * tolerance;
		m_complications = false;
		boolean bresult = sweepImpl_();
		if (!m_complications) {
			int filtered = shape.filterClosePoints(tolerance, true, false);
			m_complications = filtered == 1;
			bresult |= filtered == 1;
		}
		
		if (m_vertex_cluster_index != -1) {
			m_shape.removeUserIndex(m_vertex_cluster_index);
			m_vertex_cluster_index = -1;
		}
		
		m_shape = null;
		return bresult;
	}

	boolean hadCompications() {
		return m_complications;
	}

	private EditShape m_shape;
	private StridedIndexTypeCollection m_edges;
	private StridedIndexTypeCollection m_clusters;
	private IndexMultiList m_cluster_vertices;
	private IndexMultiList m_edge_vertices;
	private Point m_helper_point;

	private Treap m_event_q;
	private Treap m_sweep_structure;

	boolean m_complications;

	static final class SimplifySweepComparator extends SweepComparator {
		PlaneSweepCrackerHelper m_parent;

		SimplifySweepComparator(PlaneSweepCrackerHelper parent) {
			super(parent.m_shape, parent.m_tolerance, false);
			m_parent = parent;
		}

		@Override
		int compare(Treap treap, int elm, int node) {
			// Compares two segments on a sweep line passing through m_sweep_y,
			// m_sweep_x.
			if (m_b_intersection_detected)
				return -1;

			int vertex_list_left = m_parent.getEdgeOriginVertices(elm);
			int left = m_parent.m_edge_vertices
					.getFirstElement(vertex_list_left);

			int right_elm = treap.getElement(node);
			assert (m_parent.getEdgeSweepNode(right_elm) == node);
			int vertex_list_right = m_parent.getEdgeOriginVertices(right_elm);
			int right = m_parent.m_edge_vertices
					.getFirstElement(vertex_list_right);

			m_current_node = node;
			return compareSegments(elm, left, right_elm, right);
		}
	};

	static final class SimplifySweepMonikerComparator extends
			SweepMonkierComparator {
		PlaneSweepCrackerHelper m_parent;

		SimplifySweepMonikerComparator(PlaneSweepCrackerHelper parent) {
			super(parent.m_shape, parent.m_tolerance);
			m_parent = parent;
		}

		@Override
		int compare(Treap treap, int node) {
			// Compares two segments on a sweep line passing through m_sweep_y,
			// m_sweep_x.
			if (m_b_intersection_detected)
				return -1;

			int elm = treap.getElement(node);
			int vertexList = m_parent.getEdgeOriginVertices(elm);
			int vertex = m_parent.m_edge_vertices.getFirstElement(vertexList);

			m_current_node = node;
			return compareVertex_(treap, node, vertex);
		}
	};

	SimplifySweepComparator m_sweep_comparator;

	AttributeStreamOfInt32 m_temp_edge_buffer;
	AttributeStreamOfInt32 m_modified_clusters;
	AttributeStreamOfInt32 m_edges_to_insert_in_sweep_structure;

	int m_prev_neighbour;
	int m_next_neighbour;
	boolean m_b_continuing_segment_chain_optimization;// set to true, when the
														// cluster has two edges
														// attached, one is
														// below and another
														// above the sweep line

	SegmentIntersector m_segment_intersector;

	Line m_line_1;
	Line m_line_2;

	Point2D m_sweep_point;
	double m_tolerance;
	double m_tolerance_sqr;

	int m_sweep_point_cluster;
	int m_vertex_cluster_index;

	boolean m_b_cracked;
	boolean m_b_sweep_point_cluster_was_modified;// set to true if the
													// coordinates of the
													// cluster, where the sweep
													// line was, has been
													// changed.

	int getEdgeCluster(int edge, int end) {
		assert (end == 0 || end == 1);
		return m_edges.getField(edge, 0 + end);
	}

	void setEdgeCluster_(int edge, int end, int cluster) {
		assert (end == 0 || end == 1);
		m_edges.setField(edge, 0 + end, cluster);
	}

	// Edge may have several origin vertices, when there are two or more equal
	// segements in that edge
	// We have to store edge origin separately from the cluster vertices,
	// because cluster can have several different edges started on it.
	int getEdgeOriginVertices(int edge) {
		return m_edges.getField(edge, 2);
	}

	void setEdgeOriginVertices_(int edge, int vertices) {
		m_edges.setField(edge, 2, vertices);
	}

	int getNextEdgeEx(int edge, int end) {
		assert (end == 0 || end == 1);
		return m_edges.getField(edge, 3 + end);
	}

	void setNextEdgeEx_(int edge, int end, int next_edge) {
		assert (end == 0 || end == 1);
		m_edges.setField(edge, 3 + end, next_edge);
	}

	// int get_prev_edge_ex(int edge, int end)
	// {
	// assert(end == 0 || end == 1);
	// return m_edges.get_field(edge, 5 + end);
	// }
	// void set_prev_edge_ex_(int edge, int end, int prevEdge)
	// {
	// assert(end == 0 || end == 1);
	// m_edges.set_field(edge, 5 + end, prevEdge);
	// }

	int getEdgeSweepNode(int edge) {
		return m_edges.getField(edge, 7);
	}

	void setEdgeSweepNode_(int edge, int sweepNode) {
		m_edges.setField(edge, 7, sweepNode);
	}

	int getNextEdge(int edge, int cluster) {
		int end = getEdgeEnd(edge, cluster);
		assert (end == 0 || end == 1);
		return m_edges.getField(edge, 3 + end);
	}

	void setNextEdge_(int edge, int cluster, int next_edge) {
		int end = getEdgeEnd(edge, cluster);
		assert (end == 0 || end == 1);
		m_edges.setField(edge, 3 + end, next_edge);
	}

	int getPrevEdge(int edge, int cluster) {
		int end = getEdgeEnd(edge, cluster);
		assert (end == 0 || end == 1);
		return m_edges.getField(edge, 5 + end);
	}

	void setPrevEdge_(int edge, int cluster, int prevEdge) {
		int end = getEdgeEnd(edge, cluster);
		assert (end == 0 || end == 1);
		m_edges.setField(edge, 5 + end, prevEdge);
	}

	int getClusterVertices(int cluster) {
		return m_clusters.getField(cluster, 0);
	}

	void setClusterVertices_(int cluster, int vertices) {
		m_clusters.setField(cluster, 0, vertices);
	}

	int getClusterVertexIndex(int cluster) {
		return m_clusters.getField(cluster, 4);
	}

	void setClusterVertexIndex_(int cluster, int vindex) {
		m_clusters.setField(cluster, 4, vindex);
	}

	int getClusterSweepEdgeList(int cluster) {
		return m_clusters.getField(cluster, 2);
	}

	void setClusterSweepEdgeList_(int cluster, int sweep_edges) {
		m_clusters.setField(cluster, 2, sweep_edges);
	}

	int getClusterFirstEdge(int cluster) {
		return m_clusters.getField(cluster, 1);
	}

	void setClusterFirstEdge_(int cluster, int first_edge) {
		m_clusters.setField(cluster, 1, first_edge);
	}

	int getClusterEventQNode(int cluster) {
		return m_clusters.getField(cluster, 3);
	}

	void setClusterEventQNode_(int cluster, int node) {
		m_clusters.setField(cluster, 3, node);
	}

	int newCluster_(int vertex) {
		int cluster = m_clusters.newElement();
		int vertexList = m_cluster_vertices.createList();
		setClusterVertices_(cluster, vertexList);
		if (vertex != -1) {
			m_cluster_vertices.addElement(vertexList, vertex);
			assert (m_shape.getUserIndex(vertex, m_vertex_cluster_index) == -1);
			m_shape.setUserIndex(vertex, m_vertex_cluster_index, cluster);
			setClusterVertexIndex_(cluster, m_shape.getVertexIndex(vertex));
		} else {
			setClusterVertexIndex_(cluster, -1);
		}

		return cluster;
	}

	void deleteCluster_(int cluster) {
		m_clusters.deleteElement(cluster);
	}

	void addVertexToCluster_(int cluster, int vertex) {
		assert (m_shape.getUserIndex(vertex, m_vertex_cluster_index) == -1);
		int vertexList = getClusterVertices(cluster);
		m_cluster_vertices.addElement(vertexList, vertex);
		m_shape.setUserIndex(vertex, m_vertex_cluster_index, cluster);
	}

	// Creates a new unattached edge with the given origin.
	int newEdge_(int origin_vertex) {
		int edge = m_edges.newElement();
		int edgeVertices = m_edge_vertices.createList();
		setEdgeOriginVertices_(edge, edgeVertices);
		if (origin_vertex != -1)
			m_edge_vertices.addElement(edgeVertices, origin_vertex);

		return edge;
	}

	void addVertexToEdge_(int edge, int vertex) {
		int vertexList = getEdgeOriginVertices(edge);
		m_edge_vertices.addElement(vertexList, vertex);
	}

	void deleteEdge_(int edge) {
		m_edges.deleteElement(edge);
		int ind = m_edges_to_insert_in_sweep_structure.findElement(edge);
		if (ind >= 0)
			m_edges_to_insert_in_sweep_structure.popElement(ind);
	}

	void addEdgeToCluster(int edge, int cluster) {
		if (getEdgeCluster(edge, 0) == -1) {
			assert (getEdgeCluster(edge, 1) != cluster);
			setEdgeCluster_(edge, 0, cluster);
		} else if (getEdgeCluster(edge, 1) == -1) {
			assert (getEdgeCluster(edge, 0) != cluster);
			setEdgeCluster_(edge, 1, cluster);
		} else
			throw GeometryException.GeometryInternalError();

		addEdgeToClusterImpl_(edge, cluster);// simply adds the edge to the list
												// of cluster edges.
	}

	void addEdgeToClusterImpl_(int edge, int cluster) {
		int first_edge = getClusterFirstEdge(cluster);
		if (first_edge != -1) {
			int next = getNextEdge(first_edge, cluster);
			setPrevEdge_(next, cluster, edge);
			setNextEdge_(edge, cluster, next);
			setNextEdge_(first_edge, cluster, edge);
			setPrevEdge_(edge, cluster, first_edge);
		} else {
			setPrevEdge_(edge, cluster, edge);// point to itself
			setNextEdge_(edge, cluster, edge);
			setClusterFirstEdge_(cluster, edge);
		}
	}

	int getEdgeEnd(int edge, int cluster) {
		if (getEdgeCluster(edge, 0) == cluster) {
			assert (getEdgeCluster(edge, 1) != cluster);
			return 0;
		} else {
			assert (getEdgeCluster(edge, 1) == cluster);
			return 1;
		}
	}

	// Merges two coincident clusters into one. The cluster2 becomes invalid.
	void mergeClusters_(int cluster_1, int cluster2) {
		// dbg_check_cluster_(cluster_1);
		// dbg_check_cluster_(cluster2);
		int eventQnode = getClusterEventQNode(cluster2);
		if (eventQnode != -1) {
			m_event_q.deleteNode(eventQnode, -1);
			setClusterEventQNode_(cluster2, -1);
		}

		int firstEdge1 = getClusterFirstEdge(cluster_1);
		int firstEdge2 = getClusterFirstEdge(cluster2);

		if (firstEdge2 != -1) {// scope
			int edge2 = firstEdge2;
			int lastEdge = firstEdge2;
			boolean bForceContinue = false;
			// Delete edges that connect cluster_1 and cluster2.
			do {
				// dbg_check_edge_(edge2);
				bForceContinue = false;
				// assert(!StridedIndexTypeCollection.isValidElement(getEdgeSweepNode(edge2)));
				int end = getEdgeEnd(edge2, cluster2);
				int nextEdge2 = getNextEdgeEx(edge2, end);
				if (getEdgeCluster(edge2, (end + 1) & 1) == cluster_1) { // Snapping
																			// clusters
																			// that
																			// are
																			// connected
																			// with
																			// an
																			// edge
																			// Delete
																			// the
																			// edge.
					disconnectEdge_(edge2);
					int edgeOrigins2 = getEdgeOriginVertices(edge2);
					m_edge_vertices.deleteList(edgeOrigins2);
					deleteEdge_(edge2);
					if (edge2 == nextEdge2) {// deleted last edge connecting to
												// the cluster2 (all connections
												// are degenerate)
						firstEdge2 = -1;
						break;
					}
					if (firstEdge2 == edge2) {
						firstEdge2 = getClusterFirstEdge(cluster2);
						lastEdge = nextEdge2;
						bForceContinue = true;
					}
				} else {
					assert (edge2 != getClusterFirstEdge(cluster_1));
				}
				edge2 = nextEdge2;
			} while (edge2 != lastEdge || bForceContinue);

			if (firstEdge2 != -1) {
				// set the cluster to the edge ends
				do {
					int end = getEdgeEnd(edge2, cluster2);
					int nextEdge2 = getNextEdgeEx(edge2, end);
					assert (edge2 != getClusterFirstEdge(cluster_1));
					setEdgeCluster_(edge2, end, cluster_1);
					edge2 = nextEdge2;
				} while (edge2 != lastEdge);

				firstEdge1 = getClusterFirstEdge(cluster_1);
				if (firstEdge1 != -1) {
					int next1 = getNextEdge(firstEdge1, cluster_1);
					int next2 = getNextEdge(firstEdge2, cluster_1);
					if (next1 == firstEdge1) {
						setClusterFirstEdge_(cluster_1, firstEdge2);
						addEdgeToClusterImpl_(firstEdge1, cluster_1);
						setClusterFirstEdge_(cluster_1, firstEdge1);
					} else if (next2 == firstEdge2) {
						addEdgeToClusterImpl_(firstEdge2, cluster_1);
					}

					setNextEdge_(firstEdge2, cluster_1, next1);
					setPrevEdge_(next1, cluster_1, firstEdge2);
					setNextEdge_(firstEdge1, cluster_1, next2);
					setPrevEdge_(next2, cluster_1, firstEdge1);
				} else {
					setClusterFirstEdge_(cluster_1, firstEdge2);
				}
			}
		}

		int vertices1 = getClusterVertices(cluster_1);
		int vertices2 = getClusterVertices(cluster2);
		// Update cluster info on vertices.
		for (int vh = m_cluster_vertices.getFirst(vertices2); vh != -1; vh = m_cluster_vertices
				.getNext(vh)) {
			int v = m_cluster_vertices.getElement(vh);
			m_shape.setUserIndex(v, m_vertex_cluster_index, cluster_1);
		}
		m_cluster_vertices.concatenateLists(vertices1, vertices2);
		deleteCluster_(cluster2);
		// dbg_check_cluster_(cluster_1);
	}

	// Merges two coincident edges into one. The edge2 becomes invalid.
	void mergeEdges_(int edge1, int edge2) {
		// dbg_check_edge_(edge1);
		int cluster_1 = getEdgeCluster(edge1, 0);
		int cluster2 = getEdgeCluster(edge1, 1);
		int cluster21 = getEdgeCluster(edge2, 0);
		int cluster22 = getEdgeCluster(edge2, 1);

		int originVertices1 = getEdgeOriginVertices(edge1);
		int originVertices2 = getEdgeOriginVertices(edge2);
		m_edge_vertices.concatenateLists(originVertices1, originVertices2);
		if (edge2 == getClusterFirstEdge(cluster_1))
			setClusterFirstEdge_(cluster_1, edge1);
		if (edge2 == getClusterFirstEdge(cluster2))
			setClusterFirstEdge_(cluster2, edge1);

		disconnectEdge_(edge2);// disconnects the edge2 from the clusters.
		deleteEdge_(edge2);

		if (!((cluster_1 == cluster21 && cluster2 == cluster22) || (cluster2 == cluster21 && cluster_1 == cluster22))) {
			// Merged edges have different clusters (clusters have not yet been
			// merged)
			// merge clusters before merging the edges
			getClusterXY(cluster_1, pt_1);
			getClusterXY(cluster21, pt_2);
			if (pt_1.isEqual(pt_2)) {
				if (cluster_1 != cluster21) {
					mergeClusters_(cluster_1, cluster21);
					assert (!m_modified_clusters.hasElement(cluster21));
				}
				if (cluster2 != cluster22) {
					mergeClusters_(cluster2, cluster22);
					assert (!m_modified_clusters.hasElement(cluster22));
				}
			} else {
				if (cluster2 != cluster21) {
					mergeClusters_(cluster2, cluster21);
					assert (!m_modified_clusters.hasElement(cluster21));
				}
				if (cluster_1 != cluster22) {
					mergeClusters_(cluster_1, cluster22);
					assert (!m_modified_clusters.hasElement(cluster22));
				}
			}
		} else {
			// Merged edges have equal clusters.
		}
		// dbg_check_edge_(edge1);
	}

	// Disconnects the edge from its clusters.
	void disconnectEdge_(int edge) {
		int cluster_1 = getEdgeCluster(edge, 0);
		int cluster2 = getEdgeCluster(edge, 1);
		disconnectEdgeFromCluster_(edge, cluster_1);
		disconnectEdgeFromCluster_(edge, cluster2);
	}

	// Disconnects the edge from a cluster it is connected to.
	void disconnectEdgeFromCluster_(int edge, int cluster) {
		int next = getNextEdge(edge, cluster);
		assert (getPrevEdge(next, cluster) == edge);
		int prev = getPrevEdge(edge, cluster);
		assert (getNextEdge(prev, cluster) == edge);
		int first_edge = getClusterFirstEdge(cluster);
		if (next != edge) {
			setNextEdge_(prev, cluster, next);
			setPrevEdge_(next, cluster, prev);
			if (first_edge == edge)
				setClusterFirstEdge_(cluster, next);
		} else
			setClusterFirstEdge_(cluster, -1);
	}

	void applyIntersectorToEditShape_(int edgeOrigins,
			SegmentIntersector intersector, int intersector_index) {
		// Split Edit_shape segments and produce new vertices. Modify
		// coordinates as necessary. No vertices are deleted.
		int vertexHandle = m_edge_vertices.getFirst(edgeOrigins);
		int first_vertex = m_edge_vertices.getElement(vertexHandle);

		int cluster_1 = getClusterFromVertex(first_vertex);
		int cluster2 = getClusterFromVertex(m_shape.getNextVertex(first_vertex));
		boolean bComplexCase = cluster_1 == cluster2;
		assert (!bComplexCase);// if it ever asserts there will be a bug. Should
								// be a case of a curve that forms a loop.

		m_shape.splitSegment_(first_vertex, intersector, intersector_index,
				true);
		for (vertexHandle = m_edge_vertices.getNext(vertexHandle); vertexHandle != -1; vertexHandle = m_edge_vertices
				.getNext(vertexHandle)) {
			int vertex = m_edge_vertices.getElement(vertexHandle);
			boolean b_forward = getClusterFromVertex(vertex) == cluster_1;
			assert ((b_forward && getClusterFromVertex(m_shape
					.getNextVertex(vertex)) == cluster2) || (getClusterFromVertex(vertex) == cluster2 && getClusterFromVertex(m_shape
					.getNextVertex(vertex)) == cluster_1));
			m_shape.splitSegment_(vertex, intersector, intersector_index,
					b_forward);
		}

		// Now apply the updated coordinates to all vertices in the cluster_1
		// and cluster2.
		Point2D pt_0;
		Point2D pt_1;
		pt_0 = intersector.getResultSegment(intersector_index, 0).getStartXY();
		pt_1 = intersector.getResultSegment(intersector_index,
				intersector.getResultSegmentCount(intersector_index) - 1)
				.getEndXY();
		updateClusterXY(cluster_1, pt_0);
		updateClusterXY(cluster2, pt_1);
	}

	void createEdgesAndClustersFromSplitEdge_(int edge1,
			SegmentIntersector intersector, int intersector_index) {
		// dbg_check_new_edges_array_();
		// The method uses m_temp_edge_buffer for temporary storage and clears
		// it at the end.
		int edgeOrigins1 = getEdgeOriginVertices(edge1);

		// create new edges and clusters
		// Note that edge1 is disconnected from its clusters already (the
		// cluster's edge list does not contain it).
		int cluster_1 = getEdgeCluster(edge1, 0);
		int cluster2 = getEdgeCluster(edge1, 1);
		int prevEdge = newEdge_(-1);
		m_edges_to_insert_in_sweep_structure.add(prevEdge);
		int c_3 = StridedIndexTypeCollection.impossibleIndex3();
		setEdgeSweepNode_(prevEdge, c_3);// mark that its in
											// m_edges_to_insert_in_sweep_structure
		m_temp_edge_buffer.add(prevEdge);
		addEdgeToCluster(prevEdge, cluster_1);
		for (int i = 1, n = intersector
				.getResultSegmentCount(intersector_index); i < n; i++) {// each
																		// iteration
																		// adds
																		// new
																		// Cluster
																		// and
																		// Edge.
			int newCluster = newCluster_(-1);
			m_modified_clusters.add(newCluster);
			m_temp_edge_buffer.add(newCluster);
			addEdgeToCluster(prevEdge, newCluster);
			int newEdge = newEdge_(-1);
			m_edges_to_insert_in_sweep_structure.add(newEdge);
			setEdgeSweepNode_(newEdge, c_3);// mark that its in
											// m_edges_to_insert_in_sweep_structure
			m_temp_edge_buffer.add(newEdge);
			addEdgeToCluster(newEdge, newCluster);
			prevEdge = newEdge;
		}
		addEdgeToCluster(prevEdge, cluster2);
		// set the Edit_shape vertices to the new clusters and edges.
		for (int vertexHandle = m_edge_vertices.getFirst(edgeOrigins1); vertexHandle != -1; vertexHandle = m_edge_vertices
				.getNext(vertexHandle)) {
			int vertex = m_edge_vertices.getElement(vertexHandle);
			int cluster = getClusterFromVertex(vertex);
			if (cluster == cluster_1) {// connecting from cluster_1 to cluster2
				int i = 0;
				do {
					if (i > 0) {
						int c = m_temp_edge_buffer.get(i - 1);
						addVertexToCluster_(c, vertex);
						if (getClusterVertexIndex(c) == -1)
							setClusterVertexIndex_(c,
									m_shape.getVertexIndex(vertex));
					}

					int edge = m_temp_edge_buffer.get(i);
					i += 2;
					addVertexToEdge_(edge, vertex);
					vertex = m_shape.getNextVertex(vertex);
				} while (i < m_temp_edge_buffer.size());
				assert (getClusterFromVertex(vertex) == cluster2);
			} else {// connecting from cluster2 to cluster_1
				assert (cluster == cluster2);
				int i = m_temp_edge_buffer.size() - 1;
				do {
					if (i < m_temp_edge_buffer.size() - 2) {
						int c = m_temp_edge_buffer.get(i + 1);
						addVertexToCluster_(c, vertex);
						if (getClusterVertexIndex(c) < 0)
							setClusterVertexIndex_(c,
									m_shape.getVertexIndex(vertex));
					}

					assert (i % 2 == 0);
					int edge = m_temp_edge_buffer.get(i);
					i -= 2;
					addVertexToEdge_(edge, vertex);
					vertex = m_shape.getNextVertex(vertex);
				} while (i >= 0);
				assert (getClusterFromVertex(vertex) == cluster_1);
			}
		}

		// #ifdef _DEBUG_TOPO
		// for (int i = 0, j = 0, n =
		// intersector->get_result_segment_count(intersector_index); i < n; i++,
		// j+=2)
		// {
		// int edge = m_temp_edge_buffer.get(j);
		// dbg_check_edge_(edge);
		// }
		// #endif

		m_temp_edge_buffer.clear(false);
		// dbg_check_new_edges_array_();
	}

	int getVertexFromClusterIndex(int cluster) {
		int vertexList = getClusterVertices(cluster);
		int vertex = m_cluster_vertices.getFirstElement(vertexList);
		return vertex;
	}

	int getClusterFromVertex(int vertex) {
		return m_shape.getUserIndex(vertex, m_vertex_cluster_index);
	}

	static final class QComparator extends Treap.Comparator {
		EditShape m_shape;
		Point2D pt_1 = new Point2D();
		Point2D pt_2 = new Point2D();

		QComparator(EditShape shape) {
			m_shape = shape;
		}

		@Override
		int compare(Treap treap, int vertex, int node) {
			m_shape.getXY(vertex, pt_1);
			int v_2 = treap.getElement(node);
			m_shape.getXY(v_2, pt_2);
			return pt_1.compare(pt_2);
		}
	}

	static final class QMonikerComparator extends Treap.MonikerComparator {
		EditShape m_shape;
		Point2D m_point = new Point2D();
		Point2D m_pt = new Point2D();

		QMonikerComparator(EditShape shape) {
			m_shape = shape;
		}

		void setPoint(Point2D pt) {
			m_point.setCoords(pt);
		}

		@Override
		int compare(Treap treap, int node) {
			int v = treap.getElement(node);
			m_shape.getXY(v, m_pt);
			return m_point.compare(m_pt);
		}
	};

	void processSplitHelper1_(int index, int edge,
			SegmentIntersector intersector) {
		int clusterStart = getEdgeCluster(edge, 0);
		Point2D ptClusterStart = new Point2D();
		getClusterXY(clusterStart, ptClusterStart);
		Point2D ptClusterEnd = new Point2D();
		int clusterEnd = getEdgeCluster(edge, 1);
		getClusterXY(clusterEnd, ptClusterEnd);
		
		// Collect all edges that are affected by the split and that are in the
		// sweep structure.
		int count = intersector.getResultSegmentCount(index);
		Segment seg = intersector.getResultSegment(index, 0);
		Point2D newStart = new Point2D();
		seg.getStartXY(newStart);
		
		if (!ptClusterStart.isEqual(newStart)) {
			if (!m_complications) {
		        int res1 = ptClusterStart.compare(m_sweep_point);
		        int res2 = newStart.compare(m_sweep_point);
		        if (res1 * res2 < 0) {
					m_complications = true;// point is not yet have been processed
											// but moved before the sweep point,
											// this will require
					// repeating the cracking step and the sweep_vertical cannot
					// help here
				}
			}
			
			// This cluster's position needs to be changed
			getAffectedEdges(clusterStart, m_temp_edge_buffer);
			m_modified_clusters.add(clusterStart);
		}

		if (!m_complications && count > 1) {
			int dir = ptClusterStart.compare(ptClusterEnd);
			Point2D midPoint = seg.getEndXY();
			if (ptClusterStart.compare(midPoint) != dir
					|| midPoint.compare(ptClusterEnd) != dir) {// split segment
																// midpoint is
																// above the
																// sweep line.
																// Therefore the
																// part of the
																// segment
				m_complications = true;
			} else {
				if (midPoint.compare(m_sweep_point) < 0) {
					// midpoint moved below sweepline.
					m_complications = true;
				}
			}
		}

		seg = intersector.getResultSegment(index, count - 1);
		Point2D newEnd = seg.getEndXY();
		if (!ptClusterEnd.isEqual(newEnd)) {
			if (!m_complications) {
		        int res1 = ptClusterEnd.compare(m_sweep_point);
		        int res2 = newEnd.compare(m_sweep_point);
		        if (res1 * res2 < 0) {			
					m_complications = true;// point is not yet have been processed
											// but moved before the sweep point.
				}
			}
			// This cluster's position needs to be changed
			getAffectedEdges(clusterEnd, m_temp_edge_buffer);
			m_modified_clusters.add(clusterEnd);
		}

		m_temp_edge_buffer.add(edge);
		// Delete all nodes from the sweep structure that are affected by the
		// change.
		for (int i = 0, n = m_temp_edge_buffer.size(); i < n; i++) {
			int e = m_temp_edge_buffer.get(i);
			int sweepNode = getEdgeSweepNode(e);
			if (StridedIndexTypeCollection.isValidElement(sweepNode)) {
				m_sweep_structure.deleteNode(sweepNode, -1);
				setEdgeSweepNode_(e, -1);
			}

			int c_3 = StridedIndexTypeCollection.impossibleIndex3();
			if (e != edge && getEdgeSweepNode(e) != c_3)// c_3 means the edge is
														// already in the
														// m_edges_to_insert_in_sweep_structure
			{
				m_edges_to_insert_in_sweep_structure.add(e);
				setEdgeSweepNode_(e, c_3);
			}
		}
		m_temp_edge_buffer.clear(false);
	}

	boolean checkAndFixIntersection_(int leftSweepNode, int rightSweepNode) {
		int leftEdge = m_sweep_structure.getElement(leftSweepNode);
		m_sweep_comparator.compare(m_sweep_structure, leftEdge, rightSweepNode);
		if (m_sweep_comparator.intersectionDetected()) {
			m_sweep_comparator.clearIntersectionDetectedFlag();
			fixIntersection_(leftSweepNode, rightSweepNode);
			return true;
		}

		return false;
	}

	void fixIntersection_(int left, int right) {
		m_b_cracked = true;
		int edge1 = m_sweep_structure.getElement(left);
		int edge2 = m_sweep_structure.getElement(right);
		assert (edge1 != edge2);
		Segment seg_1;
		Segment seg_2;
		int vertexList1 = getEdgeOriginVertices(edge1);
		int origin1 = m_edge_vertices.getFirstElement(vertexList1);
		int vertexList2 = getEdgeOriginVertices(edge2);
		int origin2 = m_edge_vertices.getFirstElement(vertexList2);
		seg_1 = m_shape.getSegment(origin1);
		if (seg_1 == null) {
			if (m_line_1 == null)
				m_line_1 = new Line();
			m_shape.queryLineConnector(origin1, m_line_1);
			seg_1 = m_line_1;
		}

		seg_2 = m_shape.getSegment(origin2);
		if (seg_2 == null) {
			if (m_line_2 == null)
				m_line_2 = new Line();
			m_shape.queryLineConnector(origin2, m_line_2);
			seg_2 = m_line_2;
		}

		// #ifdef _DEBUG_CRACKING_REPORT
		// {
		// Point_2D pt11, pt12, pt21, pt22;
		// pt11 = seg_1->get_start_xy();
		// pt12 = seg_1->get_end_xy();
		// pt21 = seg_2->get_start_xy();
		// pt22 = seg_2->get_end_xy();
		// DEBUGPRINTF(L"Intersecting %d (%0.4f, %0.4f - %0.4f, %0.4f) and %d (%0.4f, %0.4f - %0.4f, %0.4f)\n",
		// edge1, pt11.x, pt11.y, pt12.x, pt12.y, edge2, pt21.x, pt21.y, pt22.x,
		// pt22.y);
		// }
		// #endif

		m_segment_intersector.pushSegment(seg_1);
		m_segment_intersector.pushSegment(seg_2);
		if (m_segment_intersector.intersect(m_tolerance, true))
			m_complications = true;
				
				
		splitEdge_(edge1, edge2, -1, m_segment_intersector);
		m_segment_intersector.clear();
	}

	void fixIntersectionPointSegment_(int cluster, int node) {
		m_b_cracked = true;
		int edge1 = m_sweep_structure.getElement(node);
		Segment seg_1;
		int vertexList1 = getEdgeOriginVertices(edge1);
		int origin1 = m_edge_vertices.getFirstElement(vertexList1);
		seg_1 = m_shape.getSegment(origin1);
		if (seg_1 == null) {
			if (m_line_1 == null)
				m_line_1 = new Line();
			m_shape.queryLineConnector(origin1, m_line_1);
			seg_1 = m_line_1;
		}

		int clusterVertex = getClusterFirstVertex(cluster);
		m_segment_intersector.pushSegment(seg_1);

		m_shape.queryPoint(clusterVertex, m_helper_point);
		m_segment_intersector.intersect(m_tolerance, m_helper_point, 0, 1.0,
				true);

		splitEdge_(edge1, -1, cluster, m_segment_intersector);

		m_segment_intersector.clear();
	}

	void insertNewEdges_() {
		if (m_edges_to_insert_in_sweep_structure.size() == 0)
			return;

		while (m_edges_to_insert_in_sweep_structure.size() != 0) {
			if (m_edges_to_insert_in_sweep_structure.size() > Math.max(
					(int) 100, m_shape.getTotalPointCount())) {
				assert (false);
				m_edges_to_insert_in_sweep_structure.clear(false);
				m_complications = true;
				break;// something strange going on here. bail out, forget about
						// these edges and continue with sweep line. We'll
						// iterate on the data one more time.
			}

			int edge = m_edges_to_insert_in_sweep_structure.getLast();
			m_edges_to_insert_in_sweep_structure.removeLast();

			assert (getEdgeSweepNode(edge) == StridedIndexTypeCollection
					.impossibleIndex3());
			setEdgeSweepNode_(edge, -1);
			int terminatingCluster = isEdgeOnSweepLine_(edge);
			if (terminatingCluster != -1) {
				insertNewEdgeToSweepStructure_(edge, terminatingCluster);
			}
			m_b_continuing_segment_chain_optimization = false;
		}
	}

	boolean insertNewEdgeToSweepStructure_(int edge, int terminatingCluster) {
		assert (getEdgeSweepNode(edge) == -1);
		int newEdgeNode;
		if (m_b_continuing_segment_chain_optimization) {
			newEdgeNode = m_sweep_structure.addElementAtPosition(
					m_prev_neighbour, m_next_neighbour, edge, true, true, -1);
			m_b_continuing_segment_chain_optimization = false;
		} else {
			newEdgeNode = m_sweep_structure.addUniqueElement(edge, -1);
		}

		if (newEdgeNode == -1) {// a coinciding edge.
			int existingNode = m_sweep_structure.getDuplicateElement(-1);
			int existingEdge = m_sweep_structure.getElement(existingNode);
			mergeEdges_(existingEdge, edge);
			return false;
		}

		// Remember the sweep structure node in the edge.
		setEdgeSweepNode_(edge, newEdgeNode);

		if (m_sweep_comparator.intersectionDetected()) {
			// The edge has been inserted into the sweep structure and an
			// intersection has beebn found. The edge will be split and removed.
			m_sweep_comparator.clearIntersectionDetectedFlag();
			int intersectionNode = m_sweep_comparator.getLastComparedNode();
			fixIntersection_(intersectionNode, newEdgeNode);
			return true;
		} else {
			// The edge has been inserted into the sweep structure without
			// problems (it does not intersect its neighbours)
		}

		return false;
	}

	Point2D pt_1 = new Point2D();
	Point2D pt_2 = new Point2D();
	int isEdgeOnSweepLine_(int edge) {
		int cluster_1 = getEdgeCluster(edge, 0);
		int cluster2 = getEdgeCluster(edge, 1);
		getClusterXY(cluster_1, pt_1);
		getClusterXY(cluster2, pt_2);
		if (Point2D.sqrDistance(pt_1, pt_2) <= m_tolerance_sqr) {// avoid
																	// degenerate
																	// segments
			m_complications = true;
			return -1;
		}
		int cmp1 = pt_1.compare(m_sweep_point);
		int cmp2 = pt_2.compare(m_sweep_point);
		if (cmp1 <= 0 && cmp2 > 0) {
			return cluster2;
		}

		if (cmp2 <= 0 && cmp1 > 0) {
			return cluster_1;
		}

		return -1;
	}

	// void set_edit_shape(Edit_shape* shape);
	// Fills the event queue and merges coincident clusters.
	void fillEventQueue() {
		AttributeStreamOfInt32 event_q = new AttributeStreamOfInt32(0);
		event_q.reserve(m_shape.getTotalPointCount());// temporary structure to
														// sort and find
														// clusters
		EditShape.VertexIterator iter = m_shape.queryVertexIterator();
		for (int vert = iter.next(); vert != -1; vert = iter.next()) {
			if (m_shape.getUserIndex(vert, m_vertex_cluster_index) != -1)
				event_q.add(vert);
		}

		// Now we can merge coincident clusters and form the envent structure.

		// sort vertices lexicographically.
		m_shape.sortVerticesSimpleByY_(event_q, 0, event_q.size());

		// The m_event_q is the event structure for the planesweep algorithm.
		// We could use any data structure that allows log(n) insertion and
		// deletion in the sorted order and
		// allow to iterate through in the sorted order.

		m_event_q.clear();
		// Populate the event structure
		m_event_q.setCapacity(event_q.size());
		{
			// The comparator is used to sort vertices by the m_event_q
			m_event_q.setComparator(new QComparator(m_shape));
		}

		// create the vertex clusters and fill the event structure m_event_q.
		// Because most vertices are expected to be non clustered, we create
		// clusters only for actual clusters to save some memory.
		Point2D cluster_pt = new Point2D();
		cluster_pt.setNaN();
		int cluster = -1;
		Point2D pt = new Point2D();
		for (int index = 0, nvertex = event_q.size(); index < nvertex; index++) {
			int vertex = event_q.get(index);
			m_shape.getXY(vertex, pt);
			if (pt.isEqual(cluster_pt)) {
				int vertexCluster = m_shape.getUserIndex(vertex,
						m_vertex_cluster_index);
				mergeClusters_(cluster, vertexCluster);
				continue;
			}

			cluster = getClusterFromVertex(vertex);
			// add a vertex to the event queue
			m_shape.getXY(vertex, cluster_pt);
			int eventQnode = m_event_q.addBiggestElement(vertex, -1); // this
																		// method
																		// does
																		// not
																		// call
																		// comparator's
																		// compare,
																		// assuming
																		// sorted
																		// order.
			setClusterEventQNode_(cluster, eventQnode);
		}
	}

	void fillEventQueuePass2() {
		AttributeStreamOfInt32 event_q = new AttributeStreamOfInt32(0);
		event_q.reserve(m_shape.getTotalPointCount());// temporary structure to
														// sort and find
														// clusters
		for (int node = m_event_q.getFirst(-1); node != -1; node = m_event_q
				.getNext(node)) {
			int v = m_event_q.getElement(node);
			event_q.add(v);
		}

		assert (event_q.size() == m_event_q.size(-1));

		m_event_q.clear();

		// sort vertices lexicographically.
		m_shape.sortVerticesSimpleByY_(event_q, 0, event_q.size());

		for (int index = 0, nvertex = event_q.size(); index < nvertex; index++) {
			int vertex = event_q.get(index);
			int cluster = getClusterFromVertex(vertex);
			int eventQnode = m_event_q.addBiggestElement(vertex, -1); // this
																		// method
																		// does
																		// not
																		// call
																		// comparator's
																		// compare,
																		// assuming
																		// sorted
																		// order.
			setClusterEventQNode_(cluster, eventQnode);
		}
	}

	// Returns edges already in the sweep structure that are affected by the
	// change of cluster coordinate.
	void getAffectedEdges(int cluster, AttributeStreamOfInt32 edges) {
		int first_edge = getClusterFirstEdge(cluster);
		if (first_edge == -1)
			return;

		int edge = first_edge;
		do {
			int sweepNode = getEdgeSweepNode(edge);
			if (StridedIndexTypeCollection.isValidElement(sweepNode)) {
				edges.add(edge);
			}
			edge = getNextEdge(edge, cluster);
		} while (edge != first_edge);
	}

	// Updates all vertices of the cluster to new coordinate
	void updateClusterXY(int cluster, Point2D pt) {
		int vertexList = getClusterVertices(cluster);
		for (int vh = m_cluster_vertices.getFirst(vertexList); vh != -1; vh = m_cluster_vertices
				.getNext(vh)) {
			int vertex = m_cluster_vertices.getElement(vh);
			m_shape.setXY(vertex, pt);
		}
	}

	// Modifies the given edges given the intersector class and the result
	// index.
	// The function updates the the event structure and puts new edges into the
	// m_edges_to_insert_in_sweep_structure.
	void splitEdge_(int edge1, int edge2, int intersectionCluster,
			SegmentIntersector intersector) {

		disconnectEdge_(edge1);// disconnects the edge from the clusters. The
								// edge still remembers the clusters.
		if (edge2 != -1)
			disconnectEdge_(edge2);// disconnects the edge from the clusters.
									// The edge still remembers the clusters.

		// Collect all edges that are affected when the clusters change position
		// due to snapping
		// The edges are collected in m_edges_to_insert_in_sweep_structure.
		// Collect the modified clusters in m_modified_clusters.
		processSplitHelper1_(0, edge1, intersector);
		if (edge2 != -1)
			processSplitHelper1_(1, edge2, intersector);

		if (intersectionCluster != -1) {
			intersector.getResultPoint().getXY(pt_1);
			getClusterXY(intersectionCluster, pt_2);
			if (!pt_2.isEqual(pt_1))
				m_modified_clusters.add(intersectionCluster);
		}

		// remove modified clusters from the event queue. We'll reincert them
		// later
		for (int i = 0, n = m_modified_clusters.size(); i < n; i++) {
			int cluster = m_modified_clusters.get(i);
			int eventQnode = getClusterEventQNode(cluster);
			if (eventQnode != -1) {
				m_event_q.deleteNode(eventQnode, -1);
				setClusterEventQNode_(cluster, -1);
			}
		}

		int edgeOrigins1 = getEdgeOriginVertices(edge1);
		int edgeOrigins2 = (edge2 != -1) ? getEdgeOriginVertices(edge2) : -1;

		// Adjust the vertex coordinates and split the segments in the the edit
		// shape.
		applyIntersectorToEditShape_(edgeOrigins1, intersector, 0);
		if (edge2 != -1)
			applyIntersectorToEditShape_(edgeOrigins2, intersector, 1);

		// Produce clusters, and new edges. The new edges are added to
		// m_edges_to_insert_in_sweep_structure.
		createEdgesAndClustersFromSplitEdge_(edge1, intersector, 0);
		if (edge2 != -1)
			createEdgesAndClustersFromSplitEdge_(edge2, intersector, 1);

		m_edge_vertices.deleteList(edgeOrigins1);
		deleteEdge_(edge1);

		if (edge2 != -1) {
			m_edge_vertices.deleteList(edgeOrigins2);
			deleteEdge_(edge2);
		}

		// insert clusters into the event queue and the edges into the sweep
		// structure.
		for (int i = 0, n = m_modified_clusters.size(); i < n; i++) {
			int cluster = m_modified_clusters.get(i);
			if (cluster == m_sweep_point_cluster)
				m_b_sweep_point_cluster_was_modified = true;

			int eventQnode = getClusterEventQNode(cluster);
			if (eventQnode == -1) {
				int vertex = getClusterFirstVertex(cluster);
				assert (getClusterFromVertex(vertex) == cluster);

				eventQnode = m_event_q.addUniqueElement(vertex, -1);// O(logN)
																	// operation
				if (eventQnode == -1) {// the cluster is coinciding with another
										// one. merge.
					int existingNode = m_event_q.getDuplicateElement(-1);
					int v = m_event_q.getElement(existingNode);
					assert (m_shape.isEqualXY(vertex, v));
					int existingCluster = getClusterFromVertex(v);
					mergeClusters_(existingCluster, cluster);
				} else {
					setClusterEventQNode_(cluster, eventQnode);
				}
			} else {
				// if already inserted (probably impossible) case
			}
		}

		m_modified_clusters.clear(false);
	}

	// Returns a cluster's xy.
	void getClusterXY(int cluster, Point2D ptOut) {
		int vindex = getClusterVertexIndex(cluster);
		m_shape.getXYWithIndex(vindex, ptOut);
	}

	int getClusterFirstVertex(int cluster) {
		int vertexList = getClusterVertices(cluster);
		int vertex = m_cluster_vertices.getFirstElement(vertexList);
		return vertex;
	}

	boolean sweepImpl_() {
		m_b_sweep_point_cluster_was_modified = false;
		m_sweep_point_cluster = -1;
		if (m_sweep_comparator == null) {
			m_sweep_structure.disableBalancing();
			m_sweep_comparator = new SimplifySweepComparator(this);
			m_sweep_structure.setComparator(m_sweep_comparator);
		}

		AttributeStreamOfInt32 edgesToDelete = new AttributeStreamOfInt32(0);
		SimplifySweepMonikerComparator sweepMoniker = null;
		QMonikerComparator moniker = null;

		int iterationCounter = 0;
		m_prev_neighbour = -1;
		m_next_neighbour = -1;
		m_b_continuing_segment_chain_optimization = false;

		int c_2 = StridedIndexTypeCollection.impossibleIndex2();
		int c_3 = StridedIndexTypeCollection.impossibleIndex3();
		assert (c_2 != c_3);

		for (int eventQnode = m_event_q.getFirst(-1); eventQnode != -1;) {
			iterationCounter++;
			m_b_continuing_segment_chain_optimization = false;

			int vertex = m_event_q.getElement(eventQnode);
			m_sweep_point_cluster = getClusterFromVertex(vertex);
			m_shape.getXY(vertex, m_sweep_point);

			m_sweep_comparator.setSweepY(m_sweep_point.y, m_sweep_point.x);// move
																			// the
																			// sweep
																			// line

			boolean bDisconnectedCluster = false;
			{// scope
				int first_edge = getClusterFirstEdge(m_sweep_point_cluster);
				bDisconnectedCluster = first_edge == -1;
				if (!bDisconnectedCluster) {
					int edge = first_edge;
					do {
						int sweepNode = getEdgeSweepNode(edge);
						if (sweepNode == -1) {
							m_edges_to_insert_in_sweep_structure.add(edge);
							setEdgeSweepNode_(edge, c_3);// mark that its in
															// m_edges_to_insert_in_sweep_structure
						} else if (sweepNode != c_3) {
							assert(StridedIndexTypeCollection.isValidElement(sweepNode));
							edgesToDelete.add(sweepNode);
						}
						edge = getNextEdge(edge, m_sweep_point_cluster);
					} while (edge != first_edge);
				}
			}

			// st_counter_insertions_peaks += edgesToDelete.size() == 0 &&
			// m_edges_to_insert_in_sweep_structure.size() > 0;
			// First step is to delete the edges that terminate in the
			// cluster.
			// During that step we also determine the left and right neighbors
			// of the deleted bunch and then check if those left and right
			// intersect or not.
			if (edgesToDelete.size() > 0) {
				m_b_continuing_segment_chain_optimization = (edgesToDelete
						.size() == 1 && m_edges_to_insert_in_sweep_structure
						.size() == 1);

				// Mark nodes that need to be deleted by setting c_2 to the
				// edge's sweep node member.
				for (int i = 0, n = edgesToDelete.size(); i < n; i++) {
					int edge = m_sweep_structure.getElement(edgesToDelete
							.get(i));
					setEdgeSweepNode_(edge, c_2);
				}

				int left = c_2;
				int right = c_2;
				// Determine left and right nodes for the bunch of nodes we are
				// deleting.
				for (int i = 0, n = edgesToDelete.size(); i < n; i++) {
					int sweepNode = edgesToDelete.get(i);
					if (left == c_2) {
						int localleft = m_sweep_structure.getPrev(sweepNode);
						if (localleft != -1) {
							int edge = m_sweep_structure.getElement(localleft);
							int node = getEdgeSweepNode(edge);
							if (node != c_2)
								left = localleft;
						} else
							left = -1;
					}

					if (right == c_2) {
						int localright = m_sweep_structure.getNext(sweepNode);
						if (localright != -1) {
							int edge = m_sweep_structure.getElement(localright);
							int node = getEdgeSweepNode(edge);
							if (node != c_2)
								right = localright;
						} else
							right = -1;
					}

					if (left != c_2 && right != c_2)
						break;
				}

				assert (left != c_2 && right != c_2);
				// Now delete the bunch.
				for (int i = 0, n = edgesToDelete.size(); i < n; i++) {
					int sweepNode = edgesToDelete.get(i);
					int edge = m_sweep_structure.getElement(sweepNode);
					m_sweep_structure.deleteNode(sweepNode, -1);
					setEdgeSweepNode_(edge, -1);
				}

				edgesToDelete.clear(false);

				m_prev_neighbour = left != -1 ? left : -1;
				m_next_neighbour = right != -1 ? right : -1;

				// Now check if the left and right we found intersect or not.
				if (left != -1 && right != -1) {
					if (!m_b_continuing_segment_chain_optimization) {
						boolean bIntersected = checkAndFixIntersection_(left,
								right);
					}
				} else {
					if ((left == -1) && (right == -1))
						m_b_continuing_segment_chain_optimization = false;
				}
			} else {
				// edgesToDelete.size() == 0 - nothing to delete here. This is a
				// cluster which has all edges directed up or a disconnected
				// cluster.

				if (bDisconnectedCluster) {// check standalone cluster (point or
											// multipoint) if it cracks an edge.
					if (sweepMoniker == null)
						sweepMoniker = new SimplifySweepMonikerComparator(this);

					sweepMoniker.setPoint(m_sweep_point);
					m_sweep_structure.searchUpperBound(sweepMoniker, -1);
					if (sweepMoniker.intersectionDetected()) {
						sweepMoniker.clearIntersectionDetectedFlag();
						fixIntersectionPointSegment_(m_sweep_point_cluster,
								sweepMoniker.getCurrentNode());
					}
				}
			}

			// Now insert edges that start at the cluster and go up
			insertNewEdges_();

			if (m_b_sweep_point_cluster_was_modified) {
				m_b_sweep_point_cluster_was_modified = false;
				if (moniker == null)
					moniker = new QMonikerComparator(m_shape);
				moniker.setPoint(m_sweep_point);
				eventQnode = m_event_q.searchUpperBound(moniker, -1);
			} else
				eventQnode = m_event_q.getNext(eventQnode);
		}

		return m_b_cracked;
	}

	void setEditShape_(EditShape shape) {
		// Populate the cluster and edge structures.
		m_shape = shape;
		m_vertex_cluster_index = m_shape.createUserIndex();

		m_edges.setCapacity(shape.getTotalPointCount() + 32);

		m_clusters.setCapacity(shape.getTotalPointCount());

		m_cluster_vertices.reserveLists(shape.getTotalPointCount());
		m_cluster_vertices.reserveNodes(shape.getTotalPointCount());

		m_edge_vertices.reserveLists(shape.getTotalPointCount() + 32);
		m_edge_vertices.reserveNodes(shape.getTotalPointCount() + 32);

		for (int geometry = m_shape.getFirstGeometry(); geometry != -1; geometry = m_shape
				.getNextGeometry(geometry)) {
			boolean bMultiPath = Geometry.isMultiPath(m_shape
					.getGeometryType(geometry));

			if (!bMultiPath) {// for multipoints do not add edges.
				assert (m_shape.getGeometryType(geometry) == Geometry.GeometryType.MultiPoint);

				for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
						.getNextPath(path)) {
					int vertex = m_shape.getFirstVertex(path);
					for (int i = 0, n = m_shape.getPathSize(path); i < n; i++) {
						// int cluster
						newCluster_(vertex);
						vertex = m_shape.getNextVertex(vertex);
					}
				}
				continue;
			}

			for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
					.getNextPath(path)) {
				int path_size = m_shape.getPathSize(path);
				assert (path_size > 1);
				int first_vertex = m_shape.getFirstVertex(path);

				// first------------------
				int firstCluster = newCluster_(first_vertex);
				int first_edge = newEdge_(first_vertex);
				addEdgeToCluster(first_edge, firstCluster);
				int prevEdge = first_edge;
				int vertex = m_shape.getNextVertex(first_vertex);
				for (int index = 0, n = path_size - 2; index < n; index++) {
					int nextvertex = m_shape.getNextVertex(vertex);
					// ------------x------------
					int cluster = newCluster_(vertex);
					addEdgeToCluster(prevEdge, cluster);
					int newEdge = newEdge_(vertex);
					addEdgeToCluster(newEdge, cluster);
					prevEdge = newEdge;
					vertex = nextvertex;
				}

				// ------------------lastx
				if (m_shape.isClosedPath(path)) {
					int cluster = newCluster_(vertex);
					addEdgeToCluster(prevEdge, cluster);
					// close the path
					// lastx------------------firstx
					int newEdge = newEdge_(vertex);
					addEdgeToCluster(newEdge, cluster);
					addEdgeToCluster(newEdge, firstCluster);
				} else {
					// ------------------lastx
					int cluster = newCluster_(vertex);
					addEdgeToCluster(prevEdge, cluster);
				}
				
			}
		}

		fillEventQueue();

		// int perPoint = estimate_memory_size() /
		// m_shape.get_total_point_count();
		// perPoint = 0;
	}
}
