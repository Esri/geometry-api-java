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

import com.esri.core.geometry.AttributeStreamOfInt32.IntComparator;
import com.esri.core.geometry.Geometry.GeometryType;
import com.esri.core.geometry.MultiVertexGeometryImpl.GeometryXSimple;

import java.util.ArrayList;

final class TopologicalOperations {
	TopoGraph m_topo_graph = null;
	Point2D m_dummy_pt_1 = new Point2D();
	Point2D m_dummy_pt_2 = new Point2D();
	int m_from_edge_for_polylines;
	boolean m_mask_lookup[] = null;
	boolean m_bOGCOutput = false;

	boolean isGoodParentage(int parentage) {
		return parentage < m_mask_lookup.length ? m_mask_lookup[parentage]
				: false;
	}

	void cut(int sideIndex, int cuttee, int cutter,
			AttributeStreamOfInt32 cutHandles) {
		int gtCuttee = m_topo_graph.getShape().getGeometryType(cuttee);
		int gtCutter = m_topo_graph.getShape().getGeometryType(cutter);
		int dimCuttee = Geometry.getDimensionFromType(gtCuttee);
		int dimCutter = Geometry.getDimensionFromType(gtCutter);

		if (dimCuttee == 2 && dimCutter == 1) {
			cutPolygonPolyline_(sideIndex, cuttee, cutter, cutHandles);
			return;
		}

		throw GeometryException.GeometryInternalError();
	}

	static final class CompareCuts extends IntComparator {
		private EditShape m_editShape;

		public CompareCuts(EditShape editShape) {
			m_editShape = editShape;
		}

		@Override
		public int compare(int c1, int c2) {
			int path1 = m_editShape.getFirstPath(c1);
			double area1 = m_editShape.getRingArea(path1);
			int path2 = m_editShape.getFirstPath(c2);
			double area2 = m_editShape.getRingArea(path2);
			if (area1 < area2)
				return -1;
			if (area1 == area2)
				return 0;
			return 1;
		}
	}

	public TopologicalOperations() {
		m_from_edge_for_polylines = -1;
	}

	void setEditShape(EditShape shape, ProgressTracker progressTracker) {
		if (m_topo_graph == null)
			m_topo_graph = new TopoGraph();
		m_topo_graph.setEditShape(shape, progressTracker);
	}

	void setEditShapeCrackAndCluster(EditShape shape, double tolerance,
			ProgressTracker progressTracker) {
		CrackAndCluster.execute(shape, tolerance, progressTracker, true);
		for (int geometry = shape.getFirstGeometry(); geometry != -1; geometry = shape
				.getNextGeometry(geometry)) {
			if (shape.getGeometryType(geometry) == Geometry.Type.Polygon
					.value())
				Simplificator.execute(shape, geometry, -1, m_bOGCOutput, progressTracker);
		}
		
		setEditShape(shape, progressTracker);
	}

	private void collectPolygonPathsPreservingFrom_(int geometryFrom,
			int newGeometry, int visitedEdges, int visitedClusters,
			int geometry_dominant) {
		// This function tries to create polygon paths using the paths that were
		// in the input shape.
		// This way we preserve original shape as much as possible.
		EditShape shape = m_topo_graph.getShape();
		if (shape.getGeometryType(geometryFrom) != Geometry.Type.Polygon
				.value())
			return;

		for (int path = shape.getFirstPath(geometryFrom); path != -1; path = shape
				.getNextPath(path)) {
			int first_vertex = shape.getFirstVertex(path);
			int firstCluster = m_topo_graph.getClusterFromVertex(first_vertex);
			assert (firstCluster != -1);
			int secondVertex = shape.getNextVertex(first_vertex);
			int secondCluster = m_topo_graph.getClusterFromVertex(secondVertex);
			assert (secondCluster != -1);

			int firstHalfEdge = m_topo_graph
					.getHalfEdgeFromVertex(first_vertex);

			if (firstHalfEdge == -1)
				continue;// Usually there will be a half-edge that starts at
							// first_vertex and goes to secondVertex, but it
							// could happen that this half edge has been
							// removed.

			assert (m_topo_graph.getHalfEdgeTo(firstHalfEdge) == secondCluster && m_topo_graph
					.getHalfEdgeOrigin(firstHalfEdge) == firstCluster);

			int visited = m_topo_graph.getHalfEdgeUserIndex(firstHalfEdge,
					visitedEdges);
			if (visited == 1 || visited == 2)
				continue;

			int parentage = m_topo_graph
					.getHalfEdgeFaceParentage(firstHalfEdge);
			if (!isGoodParentage(parentage)) {
				m_topo_graph.setHalfEdgeUserIndex(firstHalfEdge, visitedEdges,
						2);
				continue;
			}

			m_topo_graph.setHalfEdgeUserIndex(firstHalfEdge, visitedEdges, 1);

			int newPath = shape.insertPath(newGeometry, -1);// add new path at
															// the end
			int half_edge = firstHalfEdge;
			int vertex = first_vertex;
			int cluster = m_topo_graph.getClusterFromVertex(vertex);
			int dir = 1;
			//Walk the chain of half edges, preferably selecting vertices that belong to the
			//polygon path we have started from.
			do {
				int vertex_dominant = getVertexByID_(vertex, geometry_dominant);
				shape.addVertex(newPath, vertex_dominant);
				if (visitedClusters != -1)
					m_topo_graph.setClusterUserIndex(cluster, visitedClusters,
							1);

				m_topo_graph.setHalfEdgeUserIndex(half_edge, visitedEdges, 1);
				half_edge = m_topo_graph.getHalfEdgeNext(half_edge);
				int v;
				int cv;
				do {// move in a loop through coincident vertices (probably
					// vertical segments).
					v = dir == 1 ? shape.getNextVertex(vertex) : shape
							.getPrevVertex(vertex);// if we came to the polyline
													// tail, the next may return
													// -1.
					cv = v != -1 ? m_topo_graph.getClusterFromVertex(v) : -1;
				} while (cv == cluster);

				int originCluster = m_topo_graph.getHalfEdgeOrigin(half_edge);
				if (originCluster != cv) {
					// try going opposite way
					do {// move in a loop through coincident vertices (probably
						// vertical segments).
						v = dir == 1 ? shape.getPrevVertex(vertex) : shape
								.getNextVertex(vertex);// if we came to the
														// polyline tail, the
														// next may return -1.
						cv = v != -1 ? m_topo_graph.getClusterFromVertex(v)
								: -1;
					} while (cv == cluster);

					if (originCluster != cv) {// pick any vertex.
						cv = originCluster;
						int iterator = m_topo_graph
								.getClusterVertexIterator(cv);
						v = m_topo_graph.getVertexFromVertexIterator(iterator);
					} else {
						dir = -dir;// remember direction we were going for
									// performance
					}
				}
				cluster = cv;
				vertex = v;
			} while (half_edge != firstHalfEdge);

			shape.setClosedPath(newPath, true);
		}
	}

	// processes Topo_graph and removes edges that border faces with good
	// parentage
	// If bAllowBrokenFaces is True the function will break face structure for
	// dissolved faces. Only face parentage will be uasable.
	void dissolveCommonEdges_() {
		int visitedEdges = m_topo_graph.createUserIndexForHalfEdges();
		AttributeStreamOfInt32 edgesToDelete = new AttributeStreamOfInt32(0);
		// Now extract paths that
		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int firstHalfEdge = m_topo_graph.getClusterHalfEdge(cluster);
			int half_edge = firstHalfEdge;
			if (firstHalfEdge == -1)
				continue;

			do {
				int visited = m_topo_graph.getHalfEdgeUserIndex(half_edge,
						visitedEdges);
				if (visited != 1) {
					int halfEdgeTwin = m_topo_graph.getHalfEdgeTwin(half_edge);
					m_topo_graph.setHalfEdgeUserIndex(halfEdgeTwin,
							visitedEdges, 1);
					m_topo_graph.setHalfEdgeUserIndex(half_edge, visitedEdges,
							1);
					int parentage = m_topo_graph
							.getHalfEdgeFaceParentage(half_edge);
					if (isGoodParentage(parentage)) {
						int twinParentage = m_topo_graph
								.getHalfEdgeFaceParentage(halfEdgeTwin);
						if (isGoodParentage(twinParentage)) {
							// This half_edge pair is a border between two faces
							// that share the parentage or it is a dangling edge
							edgesToDelete.add(half_edge);// remember for
															// subsequent delete
						}
					}
				}

				half_edge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(half_edge));
			} while (half_edge != firstHalfEdge);
		}

		m_topo_graph.deleteUserIndexForHalfEdges(visitedEdges);
		m_topo_graph.deleteEdgesBreakFaces_(edgesToDelete);
	}

	int getVertexByID_(int vertex, int geometry_id) {
		if (geometry_id == -1)
			return vertex;

		return getVertexByIDImpl_(vertex, geometry_id);
	}

	int getVertexByIDImpl_(int vertex, int geometry_id) {
		EditShape shape = m_topo_graph.getShape();
		int v;
		int geometry;
		int vertex_iterator = m_topo_graph
				.getClusterVertexIterator(m_topo_graph
						.getClusterFromVertex(vertex));

		do {
			v = m_topo_graph.getVertexFromVertexIterator(vertex_iterator);
			geometry = shape.getGeometryFromPath(shape.getPathFromVertex(v));

			if (geometry == geometry_id)
				return v;

			vertex_iterator = m_topo_graph
					.incrementVertexIterator(vertex_iterator);
		} while (vertex_iterator != -1);

		return vertex;
	}

	private int topoOperationPolygonPolygon_(int geometry_a, int geometry_b,
			int geometry_dominant) {
		dissolveCommonEdges_();// faces are partially broken after this call.
								// See help to this call.

		EditShape shape = m_topo_graph.getShape();
		int newGeometry = shape.createGeometry(Geometry.Type.Polygon);
		int visitedEdges = m_topo_graph.createUserIndexForHalfEdges();

		topoOperationPolygonPolygonHelper_(geometry_a, geometry_b, newGeometry,
				geometry_dominant, visitedEdges, -1);

		m_topo_graph.deleteUserIndexForHalfEdges(visitedEdges);
		Simplificator.execute(shape, newGeometry,
				MultiVertexGeometryImpl.GeometryXSimple.Weak, m_bOGCOutput, null);
		return newGeometry;
	}

	private void topoOperationPolygonPolygonHelper_(int geometry_a,
			int geometry_b, int newGeometryPolygon, int geometry_dominant,
			int visitedEdges, int visitedClusters) {
		collectPolygonPathsPreservingFrom_(geometry_a, newGeometryPolygon,
				visitedEdges, visitedClusters, geometry_dominant);
		if (geometry_b != -1)
			collectPolygonPathsPreservingFrom_(geometry_b, newGeometryPolygon,
					visitedEdges, visitedClusters, geometry_dominant);

		EditShape shape = m_topo_graph.getShape();
		// Now extract polygon paths that has not been extracted on the previous
		// step.
		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int firstHalfEdge = m_topo_graph.getClusterHalfEdge(cluster);
			if (firstHalfEdge == -1)
				continue;

			int half_edge = firstHalfEdge;
			do {
				int visited = m_topo_graph.getHalfEdgeUserIndex(half_edge,
						visitedEdges);
				if (visited != 1 && visited != 2) {
					int parentage = m_topo_graph
							.getHalfEdgeFaceParentage(half_edge);
					if (isGoodParentage(parentage)) {// Extract face.
						int newPath = shape.insertPath(newGeometryPolygon, -1);// add
																				// new
																				// path
																				// at
																				// the
																				// end
						int faceHalfEdge = half_edge;
						do {
							int viter = m_topo_graph
									.getHalfEdgeVertexIterator(faceHalfEdge);
							int v;
							if (viter != -1) {
								v = m_topo_graph
										.getVertexFromVertexIterator(viter);
							} else {
								int viter1 = m_topo_graph
										.getHalfEdgeVertexIterator(m_topo_graph
												.getHalfEdgeTwin(faceHalfEdge));
								assert (viter1 != -1);
								v = m_topo_graph
										.getVertexFromVertexIterator(viter1);
								v = m_topo_graph.getShape().getNextVertex(v);
							}

							assert (v != -1);
							int vertex_dominant = getVertexByID_(v,
									geometry_dominant);
							shape.addVertex(newPath, vertex_dominant);
							assert (isGoodParentage(m_topo_graph
									.getHalfEdgeFaceParentage(faceHalfEdge)));
							m_topo_graph.setHalfEdgeUserIndex(faceHalfEdge,
									visitedEdges, 1);//

							if (visitedClusters != -1) {
								int c = m_topo_graph
										.getClusterFromVertex(vertex_dominant);
								m_topo_graph.setClusterUserIndex(c,
										visitedClusters, 1);
							}

							faceHalfEdge = m_topo_graph
									.getHalfEdgeNext(faceHalfEdge);
						} while (faceHalfEdge != half_edge);

						shape.setClosedPath(newPath, true);
					} else {
						// cannot extract a face
						m_topo_graph.setHalfEdgeUserIndex(half_edge,
								visitedEdges, 2);
					}

				}

				half_edge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(half_edge));
			} while (half_edge != firstHalfEdge);
		}
	}

	int[] topoOperationPolygonPolygonEx_(int geometry_a, int geometry_b,
			int geometry_dominant) {
		EditShape shape = m_topo_graph.getShape();
		int newGeometryPolygon = shape.createGeometry(Geometry.Type.Polygon);
		int newGeometryPolyline = shape.createGeometry(Geometry.Type.Polyline);
		int newGeometryMultipoint = shape
				.createGeometry(Geometry.Type.MultiPoint);

		dissolveCommonEdges_();// faces are partially broken after this call.
								// See help to this call.

		int multipointPath = -1;
		int visitedEdges = m_topo_graph.createUserIndexForHalfEdges();
		int visitedClusters = m_topo_graph.createUserIndexForClusters();

		topoOperationPolygonPolygonHelper_(geometry_a, geometry_b,
				newGeometryPolygon, geometry_dominant, visitedEdges,
				visitedClusters);

		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int firstHalfEdge = m_topo_graph.getClusterHalfEdge(cluster);
			if (firstHalfEdge == -1)
				continue;

			int half_edge = firstHalfEdge;
			do {
				int visited1 = m_topo_graph.getHalfEdgeUserIndex(half_edge,
						visitedEdges);
				int visited2 = m_topo_graph.getHalfEdgeUserIndex(
						m_topo_graph.getHalfEdgeTwin(half_edge), visitedEdges);
				int visited = visited1 | visited2;
				if (visited == 2) {
					int parentage = m_topo_graph
							.getHalfEdgeParentage(half_edge);
					if (isGoodParentage(parentage)) {// Extract face.
						int newPath = shape.insertPath(newGeometryPolyline, -1);// add
																				// new
																				// path
																				// at
																				// the
																				// end
						int polyHalfEdge = half_edge;
						int vert = selectVertex_(cluster, shape);
						assert (vert != -1);
						int vertex_dominant = getVertexByID_(vert,
								geometry_dominant);
						shape.addVertex(newPath, vertex_dominant);
						m_topo_graph.setClusterUserIndex(cluster,
								visitedClusters, 1);

						do {
							int clusterTo = m_topo_graph
									.getHalfEdgeTo(polyHalfEdge);
							int vert1 = selectVertex_(clusterTo, shape);
							assert (vert1 != -1);
							int vertex_dominant1 = getVertexByID_(vert1,
									geometry_dominant);
							shape.addVertex(newPath, vertex_dominant1);
							m_topo_graph.setHalfEdgeUserIndex(polyHalfEdge,
									visitedEdges, 1);//
							m_topo_graph.setHalfEdgeUserIndex(
									m_topo_graph.getHalfEdgeTwin(polyHalfEdge),
									visitedEdges, 1);//
							m_topo_graph.setClusterUserIndex(clusterTo,
									visitedClusters, 1);

							polyHalfEdge = m_topo_graph
									.getHalfEdgeNext(polyHalfEdge);
							visited1 = m_topo_graph.getHalfEdgeUserIndex(
									polyHalfEdge, visitedEdges);
							visited2 = m_topo_graph.getHalfEdgeUserIndex(
									m_topo_graph.getHalfEdgeTwin(polyHalfEdge),
									visitedEdges);
							visited = visited1 | visited2;
							if (visited != 2)
								break;

							parentage = m_topo_graph
									.getHalfEdgeParentage(polyHalfEdge);
							if (!isGoodParentage(parentage)) {
								m_topo_graph.setHalfEdgeUserIndex(polyHalfEdge,
										visitedEdges, 1);
								m_topo_graph.setHalfEdgeUserIndex(m_topo_graph
										.getHalfEdgeTwin(polyHalfEdge),
										visitedEdges, 1);
								break;
							}

						} while (polyHalfEdge != half_edge);

					} else {
						m_topo_graph.setHalfEdgeUserIndex(half_edge,
								visitedEdges, 1);
						m_topo_graph.setHalfEdgeUserIndex(
								m_topo_graph.getHalfEdgeTwin(half_edge),
								visitedEdges, 1);
					}
				}

				half_edge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(half_edge));
			} while (half_edge != firstHalfEdge);
		}

		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int visited = m_topo_graph.getClusterUserIndex(cluster,
					visitedClusters);
			if (visited == 1)
				continue;

			int parentage = m_topo_graph.getClusterParentage(cluster);
			if (isGoodParentage(parentage)) {
				if (multipointPath == -1)
					multipointPath = shape
							.insertPath(newGeometryMultipoint, -1);
				int viter = m_topo_graph.getClusterVertexIterator(cluster);
				int v;
				if (viter != -1) {
					v = m_topo_graph.getVertexFromVertexIterator(viter);
					int vertex_dominant = getVertexByID_(v, geometry_dominant);
					shape.addVertex(multipointPath, vertex_dominant);
				}
			}
		}

		m_topo_graph.deleteUserIndexForClusters(visitedClusters);
		m_topo_graph.deleteUserIndexForHalfEdges(visitedEdges);
		Simplificator.execute(shape, newGeometryPolygon,
				MultiVertexGeometryImpl.GeometryXSimple.Weak, m_bOGCOutput, null);
		int[] result = new int[3];// always returns size 3 result.

		result[0] = newGeometryMultipoint;
		result[1] = newGeometryPolyline;
		result[2] = newGeometryPolygon;
		return result;
	}

	int selectVertex_(int cluster, EditShape shape) {
		int vert = -1;
		for (int iterator = m_topo_graph.getClusterVertexIterator(cluster); iterator != -1; iterator = m_topo_graph
				.incrementVertexIterator(iterator)) {
			int vertex = m_topo_graph.getVertexFromVertexIterator(iterator);
			if (vert == -1)
				vert = vertex;
			int geometry = shape.getGeometryFromPath(shape
					.getPathFromVertex(vertex));
			int geomID = m_topo_graph.getGeometryID(geometry);
			if (isGoodParentage(geomID)) {
				vert = vertex;
				break;
			}
		}

		return vert;
	}

	private double prevailingDirection_(EditShape shape, int half_edge) {
		int cluster = m_topo_graph.getHalfEdgeOrigin(half_edge);
		int clusterTo = m_topo_graph.getHalfEdgeTo(half_edge);
		int signTotal = 0;
		int signCorrect = 0;
		for (int iterator = m_topo_graph.getClusterVertexIterator(cluster); iterator != -1; iterator = m_topo_graph
				.incrementVertexIterator(iterator)) {
			int vertex = m_topo_graph.getVertexFromVertexIterator(iterator);
			int path = shape.getPathFromVertex(vertex);
			int geometry = shape.getGeometryFromPath(path);
			int geomID = m_topo_graph.getGeometryID(geometry);
			int nextVert = shape.getNextVertex(vertex);
			int prevVert = shape.getPrevVertex(vertex);

			int firstVert = shape.getFirstVertex(path);
			if (firstVert == vertex) {// remember the first half edge of the
										// path. We use it to produce correct
										// startpath for closed polyline loops
				m_from_edge_for_polylines = half_edge;
			}

			if (nextVert != -1
					&& m_topo_graph.getClusterFromVertex(nextVert) == clusterTo) {
				signTotal++;
				if (isGoodParentage(geomID)) {
					if (firstVert == nextVert) {// remember the first vertex of
												// the path. We use it to
												// produce correct startpath for
												// closed polyline loops
						m_from_edge_for_polylines = m_topo_graph
								.getHalfEdgeNext(half_edge);
					}

					// update the sign
					signCorrect++;
				}
			} else if (prevVert != -1
					&& m_topo_graph.getClusterFromVertex(prevVert) == clusterTo) {
				signTotal--;
				if (isGoodParentage(geomID)) {
					if (firstVert == prevVert) {// remember the first vertex of
												// the path. We use it to
												// produce correct startpath for
												// closed polyline loops
						m_from_edge_for_polylines = m_topo_graph
								.getHalfEdgeNext(half_edge);
					}

					// update the sign
					signCorrect--;
				}
			}
		}

		m_topo_graph.getXY(cluster, m_dummy_pt_1);
		m_topo_graph.getXY(clusterTo, m_dummy_pt_2);
		double len = Point2D.distance(m_dummy_pt_1, m_dummy_pt_2);
		return (signCorrect != 0 ? signCorrect : signTotal) * len;
	}

	int getCombinedHalfEdgeParentage_(int e) {
		return m_topo_graph.getHalfEdgeParentage(e)
				| m_topo_graph.getHalfEdgeFaceParentage(e)
				| m_topo_graph.getHalfEdgeFaceParentage(m_topo_graph
						.getHalfEdgeTwin(e));
	}

	int tryMoveThroughCrossroadBackwards_(int half_edge) {
		int e = m_topo_graph.getHalfEdgeTwin(m_topo_graph
				.getHalfEdgePrev(half_edge));
		int goodEdge = -1;
		while (e != half_edge) {
			int parentage = getCombinedHalfEdgeParentage_(e);
			if (isGoodParentage(parentage)) {
				if (goodEdge != -1)
					return -1;
				
				goodEdge = e;
			}

			e = m_topo_graph.getHalfEdgeTwin(m_topo_graph.getHalfEdgePrev(e));
		}

		return goodEdge != -1 ? m_topo_graph.getHalfEdgeTwin(goodEdge) : -1;
	}

	int tryMoveThroughCrossroadForward_(int half_edge) {
		int e = m_topo_graph.getHalfEdgeTwin(m_topo_graph
				.getHalfEdgeNext(half_edge));
		int goodEdge = -1;
		while (e != half_edge) {
			int parentage = getCombinedHalfEdgeParentage_(e);
			if (isGoodParentage(parentage)) {
				if (goodEdge != -1)
					return -1;// more than one way to move through the
								// intersection
				goodEdge = e;
			}

			e = m_topo_graph.getHalfEdgeTwin(m_topo_graph.getHalfEdgeNext(e));
		}

		return goodEdge != -1 ? m_topo_graph.getHalfEdgeTwin(goodEdge) : -1;
	}

	private void restorePolylineParts_(int first_edge, int newGeometry,
			int visitedEdges, int visitedClusters, int geometry_dominant) {
		assert (isGoodParentage(getCombinedHalfEdgeParentage_(first_edge)));
		EditShape shape = m_topo_graph.getShape();
		int half_edge = first_edge;
		int halfEdgeTwin = m_topo_graph.getHalfEdgeTwin(half_edge);
		m_topo_graph.setHalfEdgeUserIndex(half_edge, visitedEdges, 1);
		m_topo_graph.setHalfEdgeUserIndex(halfEdgeTwin, visitedEdges, 1);
		double prevailingLength = prevailingDirection_(shape, half_edge);// prevailing
																			// direction
																			// is
																			// used
																			// to
																			// figure
																			// out
																			// the
																			// polyline
																			// direction.
		// Prevailing length is the sum of the length of vectors that constitute
		// the polyline.
		// Vector length is positive, if the halfedge direction coincides with
		// the direction of the original geometry
		// and negative otherwise.

		m_from_edge_for_polylines = -1;
		int fromEdge = half_edge;
		int toEdge = -1;
		boolean b_found_impassable_crossroad = false;
		int edgeCount = 1;
		while (true) {
			int halfEdgePrev = m_topo_graph.getHalfEdgePrev(half_edge);
			if (halfEdgePrev == halfEdgeTwin)
				break;// the end of a polyline
			
			int halfEdgeTwinNext = m_topo_graph.getHalfEdgeNext(halfEdgeTwin);
			if (m_topo_graph.getHalfEdgeTwin(halfEdgePrev) != halfEdgeTwinNext) {
				// Crossroads is here. We can move through the crossroad only if
				// there is only a single way to pass through.
				//When doing planar_simplify we'll never go through the crossroad.
				half_edge = tryMoveThroughCrossroadBackwards_(half_edge);
				if (half_edge == -1)
					break;
				else {
					b_found_impassable_crossroad = true;
					halfEdgeTwin = m_topo_graph.getHalfEdgeTwin(half_edge);
				}
			} else {
				half_edge = halfEdgePrev;
				halfEdgeTwin = halfEdgeTwinNext;
			}

			if (half_edge == first_edge) {// we are in a loop. No need to search
											// for the toEdge. Just remember the
											// toEdge and skip the next while
											// loop.
				toEdge = first_edge;
				break;
			}
			int parentage = getCombinedHalfEdgeParentage_(half_edge);
			if (!isGoodParentage(parentage))
				break;

			m_topo_graph.setHalfEdgeUserIndex(half_edge, visitedEdges, 1);
			m_topo_graph.setHalfEdgeUserIndex(halfEdgeTwin, visitedEdges, 1);
			fromEdge = half_edge;
			prevailingLength += prevailingDirection_(shape, half_edge);
			edgeCount++;
		}

		if (toEdge == -1) {
			half_edge = first_edge;
			halfEdgeTwin = m_topo_graph.getHalfEdgeTwin(half_edge);
			toEdge = half_edge;
			while (true) {
				int halfEdgeNext = m_topo_graph.getHalfEdgeNext(half_edge);
				if (halfEdgeNext == halfEdgeTwin)
					break;
				
				int halfEdgeTwinPrev = m_topo_graph
						.getHalfEdgePrev(halfEdgeTwin);
				if (m_topo_graph.getHalfEdgeTwin(halfEdgeNext) != halfEdgeTwinPrev) {
					// Crossroads is here. We can move through the crossroad
					// only if there is only a single way to pass through.
					half_edge = tryMoveThroughCrossroadForward_(half_edge);
					if (half_edge == -1) {
						b_found_impassable_crossroad = true;
						break;
					}
					else
						halfEdgeTwin = m_topo_graph.getHalfEdgeTwin(half_edge);
				} else {
					half_edge = halfEdgeNext;
					halfEdgeTwin = halfEdgeTwinPrev;
				}

				int parentage = getCombinedHalfEdgeParentage_(half_edge);
				if (!isGoodParentage(parentage))
					break;

				m_topo_graph.setHalfEdgeUserIndex(half_edge, visitedEdges, 1);
				m_topo_graph
						.setHalfEdgeUserIndex(halfEdgeTwin, visitedEdges, 1);
				toEdge = half_edge;
				prevailingLength += prevailingDirection_(shape, half_edge);
				edgeCount++;
			}
		} else {
			// toEdge has been found in the first while loop. This happens when
			// we go around a face.
			// Closed loops need special processing as we do not know where the
			// polyline started or ended.

			if (m_from_edge_for_polylines != -1) {
				fromEdge = m_from_edge_for_polylines;
				toEdge = m_topo_graph
						.getHalfEdgePrev(m_from_edge_for_polylines);// try
																	// simply
																	// getting
																	// prev
				int fromEdgeTwin = m_topo_graph.getHalfEdgeTwin(fromEdge);
				int fromEdgeTwinNext = m_topo_graph
						.getHalfEdgeNext(fromEdgeTwin);
				if (m_topo_graph.getHalfEdgeTwin(toEdge) != fromEdgeTwinNext) {
					// Crossroads is here. Pass through the crossroad.
					toEdge = tryMoveThroughCrossroadBackwards_(fromEdge);
					if (toEdge == -1)
						throw GeometryException.GeometryInternalError();// what?
				}

				assert (isGoodParentage(getCombinedHalfEdgeParentage_(m_from_edge_for_polylines)));
				assert (isGoodParentage(getCombinedHalfEdgeParentage_(toEdge)));
			}
		}

		boolean dir = prevailingLength >= 0;
		if (!dir) {
			int e = toEdge;
			toEdge = fromEdge;
			fromEdge = e;
			toEdge = m_topo_graph.getHalfEdgeTwin(toEdge);// switch to twin so
															// that we can use
															// next instead of
															// Prev
			assert (isGoodParentage(getCombinedHalfEdgeParentage_(toEdge)));
			fromEdge = m_topo_graph.getHalfEdgeTwin(fromEdge);
			assert (isGoodParentage(getCombinedHalfEdgeParentage_(fromEdge)));
		}
		
		int newPath = shape.insertPath(newGeometry, -1);// add new path at the
														// end
		half_edge = fromEdge;
		int cluster = m_topo_graph.getHalfEdgeOrigin(fromEdge);
		int clusterLast = m_topo_graph.getHalfEdgeTo(toEdge);
		boolean b_closed = clusterLast == cluster;
		// The linestrings can touch at boundary points only, while closed path
		// has no boundary, therefore no other path can touch it.
		// Therefore, if a closed path touches another path, we need to split
		// the closed path in two to make the result OGC simple.
		boolean b_closed_linestring_touches_other_linestring = b_closed
				&& b_found_impassable_crossroad;
		
		int vert = selectVertex_(cluster, shape);
		assert(vert != -1);
		int vertex_dominant = getVertexByID_(vert, geometry_dominant);
		shape.addVertex(newPath, vertex_dominant);

		if (visitedClusters != -1) {
			m_topo_graph.setClusterUserIndex(cluster, visitedClusters, 1);
		}

		int counter = 0;
		int splitAt = b_closed_linestring_touches_other_linestring ? (edgeCount + 1) / 2 : -1;
		while (true) {
			int clusterTo = m_topo_graph.getHalfEdgeTo(half_edge);
			int vert_1 = selectVertex_(clusterTo, shape);
			vertex_dominant = getVertexByID_(vert_1, geometry_dominant);
			shape.addVertex(newPath, vertex_dominant);
			counter++;
			if (visitedClusters != -1) {
				m_topo_graph.setClusterUserIndex(clusterTo, visitedClusters, 1);
			}

			if (b_closed_linestring_touches_other_linestring
					&& counter == splitAt) {
				newPath = shape.insertPath(newGeometry, -1);// add new path at
															// the end
				shape.addVertex(newPath, vertex_dominant);
			}
			
			assert (isGoodParentage(getCombinedHalfEdgeParentage_(half_edge)));
			if (half_edge == toEdge)
				break;
			
			int halfEdgeNext = m_topo_graph.getHalfEdgeNext(half_edge);
			if (m_topo_graph.getHalfEdgePrev(m_topo_graph
					.getHalfEdgeTwin(half_edge)) != m_topo_graph
					.getHalfEdgeTwin(halfEdgeNext)) {// crossroads.
				half_edge = tryMoveThroughCrossroadForward_(half_edge);
				if (half_edge == -1)
					throw GeometryException.GeometryInternalError();// a bug. This
																	// shoulf
																	// never
																	// happen
			} else
				half_edge = halfEdgeNext;
		}
	}

	private int topoOperationPolylinePolylineOrPolygon_(int geometry_dominant) {
		EditShape shape = m_topo_graph.getShape();
		int newGeometry = shape.createGeometry(Geometry.Type.Polyline);
		int visitedEdges = m_topo_graph.createUserIndexForHalfEdges();

		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int firstClusterHalfEdge = m_topo_graph.getClusterHalfEdge(cluster);
			int clusterHalfEdge = firstClusterHalfEdge;
			do {
				int visited = m_topo_graph.getHalfEdgeUserIndex(
						clusterHalfEdge, visitedEdges);
				if (visited != 1) {
					int parentage = getCombinedHalfEdgeParentage_(clusterHalfEdge);
					if (isGoodParentage(parentage)) {
						restorePolylineParts_(clusterHalfEdge, newGeometry,
								visitedEdges, -1, geometry_dominant);
					} else {
						//
					}
				}

				clusterHalfEdge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(clusterHalfEdge));
			} while (clusterHalfEdge != firstClusterHalfEdge);
		}

		m_topo_graph.deleteUserIndexForHalfEdges(visitedEdges);
		return newGeometry;
	}

	int[] topoOperationPolylinePolylineOrPolygonEx_(int geometry_dominant) {
		EditShape shape = m_topo_graph.getShape();
		int newPolyline = shape.createGeometry(Geometry.Type.Polyline);
		int newMultipoint = shape.createGeometry(Geometry.Type.MultiPoint);
		int visitedEdges = m_topo_graph.createUserIndexForHalfEdges();
		int visitedClusters = m_topo_graph.createUserIndexForClusters();
		int multipointPath = -1;
		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int firstClusterHalfEdge = m_topo_graph.getClusterHalfEdge(cluster);
			int clusterHalfEdge = firstClusterHalfEdge;
			do {
				int visited = m_topo_graph.getHalfEdgeUserIndex(
						clusterHalfEdge, visitedEdges);
				if (visited != 1) {
					int parentage = getCombinedHalfEdgeParentage_(clusterHalfEdge);
					if (isGoodParentage(parentage)) {
						restorePolylineParts_(clusterHalfEdge, newPolyline,
								visitedEdges, visitedClusters,
								geometry_dominant);
					} else {
						//
					}
				}

				clusterHalfEdge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(clusterHalfEdge));
			} while (clusterHalfEdge != firstClusterHalfEdge);
		}

		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int visited = m_topo_graph.getClusterUserIndex(cluster,
					visitedClusters);
			if (visited != 1) {
				int parentage = m_topo_graph.getClusterParentage(cluster);
				if (isGoodParentage(parentage)) {
					if (multipointPath == -1)
						multipointPath = shape.insertPath(newMultipoint, -1);

					int viter = m_topo_graph.getClusterVertexIterator(cluster);
					int v;
					if (viter != -1) {
						v = m_topo_graph.getVertexFromVertexIterator(viter);
						int vertex_dominant = getVertexByID_(v,
								geometry_dominant);
						shape.addVertex(multipointPath, vertex_dominant);
					}
				} else {
					//
				}
			}
		}

		m_topo_graph.deleteUserIndexForHalfEdges(visitedEdges);
		m_topo_graph.deleteUserIndexForClusters(visitedClusters);
		int[] result = new int[2];
		result[0] = newMultipoint;
		result[1] = newPolyline;
		return result;
	}

	private int topoOperationMultiPoint_() {
		EditShape shape = m_topo_graph.getShape();
		int newGeometry = shape.createGeometry(Geometry.Type.MultiPoint);
		int newPath = shape.insertPath(newGeometry, -1);// add new path at the
														// end

		// Now extract paths that
		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int parentage = m_topo_graph.getClusterParentage(cluster);
			if (isGoodParentage(parentage)) {
				int vert = -1;
				for (int iterator = m_topo_graph
						.getClusterVertexIterator(cluster); iterator != -1; iterator = m_topo_graph
						.incrementVertexIterator(iterator)) {
					int vertex = m_topo_graph
							.getVertexFromVertexIterator(iterator);
					if (vert == -1)
						vert = vertex;
					int geometry = shape.getGeometryFromPath(shape
							.getPathFromVertex(vertex));
					int geomID = m_topo_graph.getGeometryID(geometry);
					if (isGoodParentage(geomID)) {
						vert = vertex;
						break;
					}
				}
				assert (vert != -1);
				shape.addVertex(newPath, vert);
			}
		}

		return newGeometry;
	}

	void initMaskLookupArray_(int len) {
		m_mask_lookup = new boolean[len];
		for (int i = 0; i < len; i++) {
			m_mask_lookup[i] = false;
		}
	}

	static MultiPoint processMultiPointIntersectOrDiff_(MultiPoint multi_point,
			Geometry intersector, double tolerance, boolean bClipIn) {
		MultiPoint multi_point_out = ((MultiPoint) multi_point.createInstance());
		Point2D[] input_points = new Point2D[1000];
		PolygonUtils.PiPResult[] test_results = new PolygonUtils.PiPResult[1000];
		int npoints = multi_point.getPointCount();
		boolean bFirstOut = true;
		boolean bArea = (intersector.getDimension() == 2);
		if (intersector.getDimension() != 1 && intersector.getDimension() != 2)
			throw GeometryException.GeometryInternalError();

		for (int ipoints = 0; ipoints < npoints;) {
			int num = multi_point.queryCoordinates(input_points, 1000, ipoints,
					-1) - ipoints;

			if (bArea)
				PolygonUtils.testPointsInArea2D(intersector, input_points,
						(int) num, tolerance, test_results);
			else
				PolygonUtils.testPointsOnLine2D(intersector, input_points,
						(int) num, tolerance, test_results);
			int i0 = 0;
			for (int i = 0; i < num; i++) {
				boolean bTest = test_results[i] == PolygonUtils.PiPResult.PiPOutside;
				if (!bClipIn)
					bTest = !bTest;

				if (bTest) {
					if (bFirstOut) {
						bFirstOut = false;
						multi_point_out.add(multi_point, 0, ipoints);
					}

					if (i0 != i)
						multi_point_out.add(multi_point, ipoints + i0, ipoints
								+ i);

					i0 = i + 1;
				}
			}

			if (!bFirstOut && i0 != num)
				multi_point_out.add(multi_point, ipoints + i0, ipoints + num);

			ipoints += num;
		}

		if (bFirstOut)
			return multi_point;

		return multi_point_out;
	}

	static MultiPoint intersection(MultiPoint multi_point, Geometry multi_path,
			double tolerance) {
		return processMultiPointIntersectOrDiff_(multi_point, multi_path,
				tolerance, true);
	}

	static MultiPoint difference(MultiPoint multi_point, Geometry multi_path,
			double tolerance) {
		return processMultiPointIntersectOrDiff_(multi_point, multi_path,
				tolerance, false);
	}

	static Point processPointIntersectOrDiff_(Point point,
			Geometry intersector, double tolerance, boolean bClipIn) {
		if (point.isEmpty())
			return ((Point) point.createInstance());
		if (intersector.isEmpty()) {
			return bClipIn ? ((Point) point.createInstance()) : null;
		}

		Point2D[] input_points = new Point2D[1];
		PolygonUtils.PiPResult[] test_results = new PolygonUtils.PiPResult[1];
		boolean bArea = intersector.getDimension() == 2;
		if (intersector.getDimension() != 1 && intersector.getDimension() != 2)
			throw GeometryException.GeometryInternalError();
		input_points[0] = point.getXY();
		if (bArea)
			PolygonUtils.testPointsInArea2D(intersector, input_points, 1,
					tolerance, test_results);
		else
			PolygonUtils.testPointsOnLine2D(intersector, input_points, 1,
					tolerance, test_results);

		boolean bTest = test_results[0] == PolygonUtils.PiPResult.PiPOutside;
		if (!bClipIn)
			bTest = !bTest;

		if (!bTest)
			return point;
		else
			return ((Point) point.createInstance());
	}

	static Point intersection(Point point, Geometry geom, double tolerance) {
		return processPointIntersectOrDiff_(point, geom, tolerance, true);
	}

	static Point difference(Point point, Geometry geom, double tolerance) {
		return processPointIntersectOrDiff_(point, geom, tolerance, false);
	}

	static Point intersection(Point point, Point point2, double tolerance) {
		if (point.isEmpty() || point2.isEmpty())
			return (Point) point.createInstance();

		if (CrackAndCluster.non_empty_points_need_to_cluster(tolerance, point,
				point2)) {
			return CrackAndCluster.cluster_non_empty_points(point, point2, 1,
					1, 1, 1);
		}

		return (Point) point.createInstance();
	}

	static Point difference(Point point, Point point2, double tolerance) {
		if (point.isEmpty())
			return (Point) point.createInstance();
		if (point2.isEmpty())
			return point;

		if (CrackAndCluster.non_empty_points_need_to_cluster(tolerance, point,
				point2)) {
			return (Point) point.createInstance();
		}

		return point;
	}

	MultiVertexGeometry planarSimplifyImpl_(MultiVertexGeometry input_geom,
			double tolerance, boolean b_use_winding_rule_for_polygons,
			boolean dirty_result, ProgressTracker progress_tracker) {
		if (input_geom.isEmpty())
			return input_geom;

		EditShape shape = new EditShape();
		int geom = shape.addGeometry(input_geom);
		return planarSimplify(shape, geom, tolerance,
				b_use_winding_rule_for_polygons, dirty_result, progress_tracker);
	}

	MultiVertexGeometry planarSimplify(EditShape shape, int geom,
			double tolerance, boolean b_use_winding_rule_for_polygons,
			boolean dirty_result, ProgressTracker progress_tracker) {
		// This method will produce a polygon from a polyline when
		// b_use_winding_rule_for_polygons is true. This is used by buffer.
		m_topo_graph = new TopoGraph();
		try
		{
			if (dirty_result
					&& shape.getGeometryType(geom) != Geometry.Type.MultiPoint
							.value()) {
				PlaneSweepCrackerHelper plane_sweeper = new PlaneSweepCrackerHelper();
				plane_sweeper.sweepVertical(shape, tolerance);
				if (plane_sweeper.hadCompications())// shame. The one pass
													// planesweep had some
													// complications. Need to do
													// full crack and cluster.
				{
					CrackAndCluster.execute(shape, tolerance, progress_tracker, true);
					dirty_result = false;
				} else {
					m_topo_graph.check_dirty_planesweep(tolerance);
				}
			} else {
				CrackAndCluster.execute(shape, tolerance, progress_tracker, true);
				dirty_result = false;
			}
			
			if (!b_use_winding_rule_for_polygons
					|| shape.getGeometryType(geom) == Geometry.Type.MultiPoint
							.value())
				m_topo_graph.setAndSimplifyEditShapeAlternate(shape, geom, progress_tracker);
			else
				m_topo_graph.setAndSimplifyEditShapeWinding(shape, geom, progress_tracker);
	
			if (m_topo_graph.dirty_check_failed()) {
				// we ran the sweep_vertical() before and it produced some
				// issues that where detected by topo graph only.
				assert (dirty_result);
				m_topo_graph.removeShape();
				m_topo_graph = null;
				// that's at most two level recursion
				return planarSimplify(shape, geom, tolerance,
						b_use_winding_rule_for_polygons, false,
						progress_tracker);
			} else {
				//can proceed
			}
			
			m_topo_graph.check_dirty_planesweep(NumberUtils.TheNaN);
			
			int ID_a = m_topo_graph.getGeometryID(geom);
			initMaskLookupArray_((ID_a) + 1);
			m_mask_lookup[ID_a] = true; // Works only when there is a single
										// geometry in the edit shape.
			// To make it work when many geometries are present, this need to be
			// modified.
	
			if (shape.getGeometryType(geom) == Geometry.Type.Polygon.value()
					|| (b_use_winding_rule_for_polygons && shape
							.getGeometryType(geom) != Geometry.Type.MultiPoint
							.value())) {
				// geom can be a polygon or a polyline.
				// It can be a polyline only when the winding rule is true.
				shape.setFillRule(geom,  Polygon.FillRule.enumFillRuleOddEven);
				int resGeom = topoOperationPolygonPolygon_(geom, -1, -1);
	
				Polygon polygon = (Polygon) shape.getGeometry(resGeom);
				polygon.setFillRule(Polygon.FillRule.enumFillRuleOddEven);//standardize the fill rule.
				if (!dirty_result) {
					((MultiVertexGeometryImpl) polygon._getImpl()).setIsSimple(
							GeometryXSimple.Strong, tolerance, false);
					((MultiPathImpl) polygon._getImpl())._updateOGCFlags();
				} else
					((MultiVertexGeometryImpl) polygon._getImpl()).setIsSimple(
							GeometryXSimple.Weak, 0.0, false);// dirty result means
																// simple but with 0
																// tolerance.
	
				return polygon;
			} else if (shape.getGeometryType(geom) == Geometry.Type.Polyline
					.value()) {
				int resGeom = topoOperationPolylinePolylineOrPolygon_(-1);
	
				Polyline polyline = (Polyline) shape.getGeometry(resGeom);
				if (!dirty_result)
					((MultiVertexGeometryImpl) polyline._getImpl()).setIsSimple(
							GeometryXSimple.Strong, tolerance, false);
	
				return polyline;
			} else if (shape.getGeometryType(geom) == Geometry.Type.MultiPoint
					.value()) {
				int resGeom = topoOperationMultiPoint_();
	
				MultiPoint mp = (MultiPoint) shape.getGeometry(resGeom);
				if (!dirty_result)
					((MultiVertexGeometryImpl) mp._getImpl()).setIsSimple(
							GeometryXSimple.Strong, tolerance, false);
	
				return mp;
			} else {
				throw GeometryException.GeometryInternalError();
			}
		}
		finally {
			m_topo_graph.removeShape();
		}
	}

	// static
	static MultiVertexGeometry planarSimplify(MultiVertexGeometry input_geom,
			double tolerance, boolean use_winding_rule_for_polygons,
			boolean dirty_result, ProgressTracker progress_tracker) {
		TopologicalOperations topoOps = new TopologicalOperations();
		return topoOps.planarSimplifyImpl_(input_geom, tolerance,
				use_winding_rule_for_polygons, dirty_result, progress_tracker);
	}

    boolean planarSimplifyNoCrackingAndCluster(boolean OGCoutput, EditShape shape, int geom, ProgressTracker progress_tracker)
    {
      m_bOGCOutput = OGCoutput;
      m_topo_graph = new TopoGraph();
      int rule = shape.getFillRule(geom);
      int gt = shape.getGeometryType(geom);
      if (rule != Polygon.FillRule.enumFillRuleWinding || gt == GeometryType.MultiPoint)
        m_topo_graph.setAndSimplifyEditShapeAlternate(shape, geom, progress_tracker);
      else
        m_topo_graph.setAndSimplifyEditShapeWinding(shape, geom, progress_tracker);

      if (m_topo_graph.dirty_check_failed())
        return false;

      m_topo_graph.check_dirty_planesweep(NumberUtils.TheNaN);

      int ID_a = m_topo_graph.getGeometryID(geom);
      initMaskLookupArray_((ID_a)+1);
      m_mask_lookup[ID_a] = true; //Works only when there is a single geometry in the edit shape.
      //To make it work when many geometries are present, this need to be modified.

      if (shape.getGeometryType(geom) == GeometryType.Polygon || (rule == Polygon.FillRule.enumFillRuleWinding && shape.getGeometryType(geom) != GeometryType.MultiPoint))
      {
        //geom can be a polygon or a polyline.
        //It can be a polyline only when the winding rule is true.
        shape.setFillRule(geom, Polygon.FillRule.enumFillRuleOddEven);
        int resGeom = topoOperationPolygonPolygon_(geom, -1, -1);
        shape.swapGeometry(resGeom, geom);
        shape.removeGeometry(resGeom);
      }
      else if (shape.getGeometryType(geom) == GeometryType.Polyline)
      {
        int resGeom = topoOperationPolylinePolylineOrPolygon_(-1);
        shape.swapGeometry(resGeom, geom);
        shape.removeGeometry(resGeom);
      }
      else if (shape.getGeometryType(geom) == GeometryType.MultiPoint)
      {
        int resGeom = topoOperationMultiPoint_();
        shape.swapGeometry(resGeom, geom);
        shape.removeGeometry(resGeom);
      }
      else
      {
        throw new GeometryException("internal error");
      }
      
      return true;
    }
	
	
    static MultiVertexGeometry simplifyOGC(MultiVertexGeometry input_geom, double tolerance, boolean dirty_result, ProgressTracker progress_tracker)
    {
      TopologicalOperations topoOps = new TopologicalOperations();
      topoOps.m_bOGCOutput = true;
      return topoOps.planarSimplifyImpl_(input_geom, tolerance, false, dirty_result, progress_tracker);
    }
	
	public int difference(int geometry_a, int geometry_b) {
		int gtA = m_topo_graph.getShape().getGeometryType(geometry_a);
		int gtB = m_topo_graph.getShape().getGeometryType(geometry_b);
		int dim_a = Geometry.getDimensionFromType(gtA);
		int dim_b = Geometry.getDimensionFromType(gtB);
		if (dim_a > dim_b) {
			return geometry_a;
		}

		int ID_a = m_topo_graph.getGeometryID(geometry_a);
		int ID_b = m_topo_graph.getGeometryID(geometry_b);
		initMaskLookupArray_((ID_a | ID_b) + 1);
		m_mask_lookup[m_topo_graph.getGeometryID(geometry_a)] = true;

		if (dim_a == 2 && dim_b == 2)
			return topoOperationPolygonPolygon_(geometry_a, geometry_b, -1);
		if (dim_a == 1 && dim_b == 2)
			return topoOperationPolylinePolylineOrPolygon_(-1);
		if (dim_a == 1 && dim_b == 1)
			return topoOperationPolylinePolylineOrPolygon_(-1);
		if (dim_a == 0)
			return topoOperationMultiPoint_();

		throw GeometryException.GeometryInternalError();
	}

	int dissolve(int geometry_a, int geometry_b) {
		int gtA = m_topo_graph.getShape().getGeometryType(geometry_a);
		int gtB = m_topo_graph.getShape().getGeometryType(geometry_b);
		int dim_a = Geometry.getDimensionFromType(gtA);
		int dim_b = Geometry.getDimensionFromType(gtB);
		if (dim_a > dim_b) {
			return geometry_a;
		}

		if (dim_a < dim_b) {
			return geometry_b;
		}

		int ID_a = m_topo_graph.getGeometryID(geometry_a);
		int ID_b = m_topo_graph.getGeometryID(geometry_b);
		initMaskLookupArray_(((ID_a | ID_b) + 1));

		m_mask_lookup[m_topo_graph.getGeometryID(geometry_a)] = true;
		m_mask_lookup[m_topo_graph.getGeometryID(geometry_b)] = true;
		m_mask_lookup[m_topo_graph.getGeometryID(geometry_a)
				| m_topo_graph.getGeometryID(geometry_b)] = true;

		if (dim_a == 2 && dim_b == 2)
			return topoOperationPolygonPolygon_(geometry_a, geometry_b, -1);
		if (dim_a == 1 && dim_b == 1)
			return topoOperationPolylinePolylineOrPolygon_(-1);
		if (dim_a == 0 && dim_b == 0)
			return topoOperationMultiPoint_();

		throw GeometryException.GeometryInternalError();
	}

	public int intersection(int geometry_a, int geometry_b) {
		int gtA = m_topo_graph.getShape().getGeometryType(geometry_a);
		int gtB = m_topo_graph.getShape().getGeometryType(geometry_b);
		int dim_a = Geometry.getDimensionFromType(gtA);
		int dim_b = Geometry.getDimensionFromType(gtB);

		int ID_a = m_topo_graph.getGeometryID(geometry_a);
		int ID_b = m_topo_graph.getGeometryID(geometry_b);
		initMaskLookupArray_(((ID_a | ID_b) + 1));

		m_mask_lookup[m_topo_graph.getGeometryID(geometry_a)
				| m_topo_graph.getGeometryID(geometry_b)] = true;

		int geometry_dominant = -1;
		boolean b_vertex_dominance = (m_topo_graph.getShape()
				.getVertexDescription().getAttributeCount() > 1);
		if (b_vertex_dominance)
			geometry_dominant = geometry_a;

		if (dim_a == 2 && dim_b == 2)// intersect two polygons
			return topoOperationPolygonPolygon_(geometry_a, geometry_b,
					geometry_dominant);
		if ((dim_a == 1 && dim_b > 0) || (dim_b == 1 && dim_a > 0))// intersect
																	// polyline
																	// with
																	// polyline
																	// or
																	// polygon
			return topoOperationPolylinePolylineOrPolygon_(geometry_dominant);
		if (dim_a == 0 || dim_b == 0)// intersect a multipoint with something
										// else
			return topoOperationMultiPoint_();

		throw GeometryException.GeometryInternalError();
	}

	int[] intersectionEx(int geometry_a, int geometry_b) {
		int gtA = m_topo_graph.getShape().getGeometryType(geometry_a);
		int gtB = m_topo_graph.getShape().getGeometryType(geometry_b);
		int dim_a = Geometry.getDimensionFromType(gtA);
		int dim_b = Geometry.getDimensionFromType(gtB);

		int ID_a = m_topo_graph.getGeometryID(geometry_a);
		int ID_b = m_topo_graph.getGeometryID(geometry_b);
		initMaskLookupArray_(((ID_a | ID_b) + 1));

		m_mask_lookup[m_topo_graph.getGeometryID(geometry_a)
				| m_topo_graph.getGeometryID(geometry_b)] = true;

		int geometry_dominant = -1;
		boolean b_vertex_dominance = (m_topo_graph.getShape()
				.getVertexDescription().getAttributeCount() > 1);
		if (b_vertex_dominance)
			geometry_dominant = geometry_a;

		if (dim_a == 2 && dim_b == 2)// intersect two polygons
			return topoOperationPolygonPolygonEx_(geometry_a, geometry_b,
					geometry_dominant);
		if ((dim_a == 1 && dim_b > 0) || (dim_b == 1 && dim_a > 0))// intersect
																	// polyline
																	// with
																	// polyline
																	// or
																	// polygon
			return topoOperationPolylinePolylineOrPolygonEx_(geometry_dominant);
		if (dim_a == 0 || dim_b == 0)// intersect a multipoint with something
										// else
		{
			int[] res = new int[1];
			res[0] = topoOperationMultiPoint_();
			return res;
		}

		throw GeometryException.GeometryInternalError();
	}

	public int symmetricDifference(int geometry_a, int geometry_b) {
		int gtA = m_topo_graph.getShape().getGeometryType(geometry_a);
		int gtB = m_topo_graph.getShape().getGeometryType(geometry_b);
		int dim_a = Geometry.getDimensionFromType(gtA);
		int dim_b = Geometry.getDimensionFromType(gtB);

		int ID_a = m_topo_graph.getGeometryID(geometry_a);
		int ID_b = m_topo_graph.getGeometryID(geometry_b);
		initMaskLookupArray_((ID_a | ID_b) + 1);

		m_mask_lookup[m_topo_graph.getGeometryID(geometry_a)] = true;
		m_mask_lookup[m_topo_graph.getGeometryID(geometry_b)] = true;

		if (dim_a == 2 && dim_b == 2)
			return topoOperationPolygonPolygon_(geometry_a, geometry_b, -1);
		if (dim_a == 1 && dim_b == 1)
			return topoOperationPolylinePolylineOrPolygon_(-1);
		if (dim_a == 0 && dim_b == 0)
			return topoOperationMultiPoint_();

		throw GeometryException.GeometryInternalError();
	}

	int extractShape(int geometry_in) {
		int gtA = m_topo_graph.getShape().getGeometryType(geometry_in);
		int dim_a = Geometry.getDimensionFromType(gtA);

		int ID_a = m_topo_graph.getGeometryID(geometry_in);
		initMaskLookupArray_((ID_a) + 1);
		m_mask_lookup[m_topo_graph.getGeometryID(geometry_in)] = true; // Works
																		// only
																		// when
																		// there
																		// is a
																		// single
																		// geometry
																		// in
																		// the
																		// edit
																		// shape.
		// To make it work when many geometries are present, this need to be
		// modified.

		if (dim_a == 2)
			return topoOperationPolygonPolygon_(geometry_in, -1, -1);
		if (dim_a == 1)
			return topoOperationPolylinePolylineOrPolygon_(-1);
		if (dim_a == 0)
			return topoOperationMultiPoint_();

		throw GeometryException.GeometryInternalError();
	}

	static Geometry normalizeInputGeometry_(Geometry geom) {
		Geometry.Type gt = geom.getType();
		if (gt == Geometry.Type.Envelope) {
			Polygon poly = new Polygon(geom.getDescription());
			if (!geom.isEmpty())
				poly.addEnvelope((Envelope) geom, false);
			return poly;
		}
		if (gt == Geometry.Type.Point) {
			MultiPoint poly = new MultiPoint(geom.getDescription());
			if (!geom.isEmpty())
				poly.add((Point) geom);
			return poly;
		}
		if (gt == Geometry.Type.Line) {
			Polyline poly = new Polyline(geom.getDescription());
			if (!geom.isEmpty())
				poly.addSegment((Segment) geom, true);
			return poly;
		}

		return geom;
	}

	static Geometry normalizeResult_(Geometry geomRes, Geometry geom_a,
			Geometry dummy, char op) {
		// assert(strchr("-&^|",op) != NULL);
		Geometry.Type gtRes = geomRes.getType();
		if (gtRes == Geometry.Type.Envelope) {
			Polygon poly = new Polygon(geomRes.getDescription());
			if (!geomRes.isEmpty())
				poly.addEnvelope((Envelope) geomRes, false);
			return poly;
		}

		if (gtRes == Geometry.Type.Point && (op == '|' || op == '^')) {
			MultiPoint poly = new MultiPoint(geomRes.getDescription());
			if (!geomRes.isEmpty())
				poly.add((Point) geomRes);
			return poly;
		}

		if (gtRes == Geometry.Type.Line) {
			Polyline poly = new Polyline(geomRes.getDescription());
			if (!geomRes.isEmpty())
				poly.addSegment((Segment) geomRes, true);
			return poly;
		}

		if (gtRes == Geometry.Type.Point && op == '-') {
			if (geom_a.getType() == Geometry.Type.Point) {
				Point pt = new Point(geomRes.getDescription());
				if (!geomRes.isEmpty()) {
					assert (((MultiPoint) geomRes).getPointCount() == 1);
					((MultiPoint) geomRes).getPointByVal(0, pt);
				}
				return pt;
			}
		}

		if (gtRes == Geometry.Type.MultiPoint && op == '&') {
			if (geom_a.getType() == Geometry.Type.Point) {
				Point pt = new Point(geomRes.getDescription());
				if (!geomRes.isEmpty()) {
					assert (((MultiPoint) geomRes).getPointCount() == 1);
					((MultiPoint) geomRes).getPointByVal(0, pt);
				}
				return pt;
			}
		}

		return geomRes;
	}

	// static
	public static Geometry difference(Geometry geometry_a, Geometry geometry_b,
			SpatialReference sr, ProgressTracker progress_tracker) {
		if (geometry_a.isEmpty() || geometry_b.isEmpty()
				|| geometry_a.getDimension() > geometry_b.getDimension())
			return normalizeResult_(normalizeInputGeometry_(geometry_a),
					geometry_a, geometry_b, '-');

		Envelope2D env2D_1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env2D_1);
		Envelope2D env2D_2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2D_2);

		if (!env2D_1.isIntersecting(env2D_2)) {
			return normalizeResult_(normalizeInputGeometry_(geometry_a),
					geometry_a, geometry_b, '-');
		}

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env2D_1);
		envMerged.merge(env2D_2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, true);// conservative to have same effect as simplify

		TopologicalOperations topoOps = new TopologicalOperations();
		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_a));
		int geom_b = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_b));
		topoOps.setEditShapeCrackAndCluster(edit_shape, tolerance,
				progress_tracker);
		int result = topoOps.difference(geom_a, geom_b);
		Geometry resGeom = edit_shape.getGeometry(result);

		Geometry res_geom = normalizeResult_(resGeom, geometry_a, geometry_b,
				'-');

		if (Geometry.isMultiPath(res_geom.getType().value())) {
			((MultiVertexGeometryImpl) res_geom._getImpl()).setIsSimple(
					GeometryXSimple.Strong, tolerance, false);
			if (res_geom.getType() == Geometry.Type.Polygon)
				((MultiPathImpl) res_geom._getImpl())._updateOGCFlags();
		}

		return res_geom;
	}

	public static Geometry dissolve(Geometry geometry_a, Geometry geometry_b,
			SpatialReference sr, ProgressTracker progress_tracker) {
		if (geometry_a.getDimension() > geometry_b.getDimension())
			return normalizeResult_(normalizeInputGeometry_(geometry_a),
					geometry_a, geometry_b, '|');

		if (geometry_a.getDimension() < geometry_b.getDimension())
			return normalizeResult_(normalizeInputGeometry_(geometry_b),
					geometry_a, geometry_b, '|');

		if (geometry_a.isEmpty())
			return normalizeResult_(normalizeInputGeometry_(geometry_b),
					geometry_a, geometry_b, '|');

		if (geometry_b.isEmpty())
			return normalizeResult_(normalizeInputGeometry_(geometry_a),
					geometry_a, geometry_b, '|');

		Envelope2D env2D_1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env2D_1);
		Envelope2D env2D_2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2D_2);

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env2D_1);
		envMerged.merge(env2D_2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, true);// conservative to have same effect as simplify

		if (!env2D_1.isIntersecting(env2D_2.getInflated(tolerance, tolerance))) {
			// TODO: add optimization here to merge two geometries if the
			// envelopes do not overlap.
			Geometry geom1 = normalizeInputGeometry_(geometry_a);
			assert (Geometry.isMultiVertex(geom1.getType().value()));
			Geometry geom2 = normalizeInputGeometry_(geometry_b);
			assert (Geometry.isMultiVertex(geom2.getType().value()));
			assert (geom1.getType() == geom2.getType());
			switch (geom1.getType().value()) {
			case Geometry.GeometryType.MultiPoint: {
				Geometry res = Geometry._clone(geom1);
				((MultiPoint) res).add((MultiPoint) geom2, 0, -1);
				return res;
			}
				// break;
			case Geometry.GeometryType.Polyline: {
				Geometry res = Geometry._clone(geom1);
				((Polyline) res).add((MultiPath) geom2, false);
				return res;
			}
				// break;
			case Geometry.GeometryType.Polygon: {
				Geometry res = Geometry._clone(geom1);
				((Polygon) res).add((MultiPath) geom2, false);
				return res;
			}
				// break;
			default:
				throw GeometryException.GeometryInternalError();
			}
		}

		TopologicalOperations topoOps = new TopologicalOperations();
		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_a));
		int geom_b = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_b));
		topoOps.setEditShapeCrackAndCluster(edit_shape, tolerance,
				progress_tracker);
		int result = topoOps.dissolve(geom_a, geom_b);

		Geometry res_geom = normalizeResult_(edit_shape.getGeometry(result),
				geometry_a, geometry_b, '|');

		if (Geometry.isMultiPath(res_geom.getType().value())) {
			((MultiVertexGeometryImpl) res_geom._getImpl()).setIsSimple(
					GeometryXSimple.Strong, tolerance, false);
			if (res_geom.getType() == Geometry.Type.Polygon)
				((MultiPathImpl) res_geom._getImpl())._updateOGCFlags();
		}

		return res_geom;
	}

	static Geometry dissolveDirty(ArrayList<Geometry> geometries,
			SpatialReference sr, ProgressTracker progress_tracker) {
		if (geometries.size() < 2)
			throw new IllegalArgumentException(
					"not enough geometries to dissolve");

		int dim = 0;
		for (int i = 0, n = geometries.size(); i < n; i++) {
			dim = Math.max(geometries.get(i).getDimension(), dim);
		}

		Envelope2D envMerged = new Envelope2D();
		envMerged.setEmpty();

		EditShape shape = new EditShape();
		int geom = -1;
		int count = 0;
		int any_index = -1;
		for (int i = 0, n = geometries.size(); i < n; i++) {
			if (geometries.get(i).getDimension() == dim) {
				if (!geometries.get(i).isEmpty()) {
					any_index = i;
					if (geom == -1)
						geom = shape
								.addGeometry(normalizeInputGeometry_(geometries
										.get(i)));
					else
						shape.appendGeometry(geom,
								normalizeInputGeometry_(geometries.get(i)));

					Envelope2D env = new Envelope2D();
					geometries.get(i).queryLooseEnvelope2D(env);
					envMerged.merge(env);
					count++;
				} else if (any_index == -1)
					any_index = i;
			}
		}

		if (count < 2) {
			return normalizeInputGeometry_(geometries.get(any_index));
		}

		boolean winding = dim == 2;

		SpatialReference psr = dim == 0 ? sr : null;// if points, then use
													// correct tolerance.
		double tolerance = InternalUtils.calculateToleranceFromGeometry(psr,
				envMerged, true);
		TopologicalOperations topoOps = new TopologicalOperations();
		return topoOps.planarSimplify(shape, geom, tolerance, winding, true,
				progress_tracker);
	}

	// static
	public static Geometry intersection(Geometry geometry_a,
			Geometry geometry_b, SpatialReference sr,
			ProgressTracker progress_tracker) {

		Envelope2D env2D_1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env2D_1);
		Envelope2D env2D_2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2D_2);

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env2D_1);
		envMerged.merge(env2D_2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, true);// conservative to have same effect as simplify

		Envelope2D e = new Envelope2D();
		e.setCoords(env2D_2);
		double tol_cluster = InternalUtils
				.adjust_tolerance_for_TE_clustering(tolerance);
		e.inflate(tol_cluster, tol_cluster);

		if (!env2D_1.isIntersecting(e))// also includes the empty geometry
										// cases
		{
			if (geometry_a.getDimension() <= geometry_b.getDimension())
				return normalizeResult_(
						normalizeInputGeometry_(geometry_a.createInstance()),
						geometry_a, geometry_b, '&');

			if (geometry_a.getDimension() > geometry_b.getDimension())
				return normalizeResult_(
						normalizeInputGeometry_(geometry_b.createInstance()),
						geometry_a, geometry_b, '&');
		}

		TopologicalOperations topoOps = new TopologicalOperations();
		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_a));
		int geom_b = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_b));

		topoOps.setEditShapeCrackAndCluster(edit_shape, tolerance,
				progress_tracker);
		int result = topoOps.intersection(geom_a, geom_b);
		Geometry res_geom = normalizeResult_(edit_shape.getGeometry(result),
				geometry_a, geometry_b, '&');

		if (Geometry.isMultiPath(res_geom.getType().value())) {
			((MultiVertexGeometryImpl) res_geom._getImpl()).setIsSimple(
					GeometryXSimple.Strong, tolerance, false);
			if (res_geom.getType() == Geometry.Type.Polygon)
				((MultiPathImpl) res_geom._getImpl())._updateOGCFlags();
		}

		return res_geom;
	}

	static Geometry[] intersectionEx(Geometry geometry_a, Geometry geometry_b,
			SpatialReference sr, ProgressTracker progress_tracker) {
		Geometry[] res_vec = new Geometry[3];

		Envelope2D env2D_1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env2D_1);
		Envelope2D env2D_2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2D_2);

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env2D_1);
		envMerged.merge(env2D_2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, true);// conservative to have same effect as simplify

		Envelope2D e = new Envelope2D();
		e.setCoords(env2D_2);
		double tol_cluster = InternalUtils
				.adjust_tolerance_for_TE_clustering(tolerance);
		e.inflate(tol_cluster, tol_cluster);

		if (!env2D_1.isIntersecting(e))// also includes the empty geometry
										// cases
		{
			if (geometry_a.getDimension() <= geometry_b.getDimension()) {
				Geometry geom = normalizeResult_(
						normalizeInputGeometry_(geometry_a.createInstance()),
						geometry_a, geometry_b, '&');
				res_vec[geom.getDimension()] = geom;
				return res_vec;
			}

			if (geometry_a.getDimension() > geometry_b.getDimension()) {
				Geometry geom = normalizeResult_(
						normalizeInputGeometry_(geometry_b.createInstance()),
						geometry_a, geometry_b, '&');
				res_vec[geom.getDimension()] = geom;
				return res_vec;
			}

		}

		TopologicalOperations topoOps = new TopologicalOperations();
		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_a));
		int geom_b = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_b));

		topoOps.setEditShapeCrackAndCluster(edit_shape, tolerance,
				progress_tracker);
		int[] result_geom_handles = topoOps.intersectionEx(geom_a, geom_b);
		for (int i = 0; i < result_geom_handles.length; i++) {
			Geometry res_geom = normalizeResult_(
					edit_shape.getGeometry(result_geom_handles[i]), geometry_a,
					geometry_b, '&');

			if (Geometry.isMultiPath(res_geom.getType().value())) {
				((MultiVertexGeometryImpl) res_geom._getImpl()).setIsSimple(
						MultiVertexGeometryImpl.GeometryXSimple.Strong,
						tolerance, false);
				if (res_geom.getType().value() == Geometry.GeometryType.Polygon)
					((MultiPathImpl) res_geom._getImpl())._updateOGCFlags();
			}

			res_vec[res_geom.getDimension()] = res_geom;
		}

		return res_vec;
	}

	// static
	public static Geometry symmetricDifference(Geometry geometry_a,
			Geometry geometry_b, SpatialReference sr,
			ProgressTracker progress_tracker) {
		if (geometry_a.getDimension() > geometry_b.getDimension())
			return normalizeResult_(normalizeInputGeometry_(geometry_a),
					geometry_a, geometry_b, '^');

		if (geometry_a.getDimension() < geometry_b.getDimension())
			return normalizeResult_(normalizeInputGeometry_(geometry_b),
					geometry_a, geometry_b, '^');

		if (geometry_a.isEmpty())
			return normalizeResult_(normalizeInputGeometry_(geometry_b),
					geometry_a, geometry_b, '^');

		if (geometry_b.isEmpty())
			return normalizeResult_(normalizeInputGeometry_(geometry_a),
					geometry_a, geometry_b, '^');

		Envelope2D env2D_1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env2D_1);
		Envelope2D env2D_2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2D_2);
		// TODO: add optimization here to merge two geometries if the envelopes
		// do not overlap.

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env2D_1);
		envMerged.merge(env2D_2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, true);// conservative to have same effect as simplify

		TopologicalOperations topoOps = new TopologicalOperations();
		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_a));
		int geom_b = edit_shape
				.addGeometry(normalizeInputGeometry_(geometry_b));
		topoOps.setEditShapeCrackAndCluster(edit_shape, tolerance,
				progress_tracker);
		int result = topoOps.symmetricDifference(geom_a, geom_b);
		Geometry res_geom = normalizeResult_(edit_shape.getGeometry(result),
				geometry_a, geometry_b, '^');

		if (Geometry.isMultiPath(res_geom.getType().value())) {
			((MultiVertexGeometryImpl) res_geom._getImpl()).setIsSimple(
					GeometryXSimple.Strong, tolerance, false);
			if (res_geom.getType() == Geometry.Type.Polygon)
				((MultiPathImpl) res_geom._getImpl())._updateOGCFlags();
		}

		return res_geom;
	}

	static Geometry _denormalizeGeometry(Geometry geom, Geometry geomA,
			Geometry geomB) {
		Geometry.Type gtA = geomA.getType();
		Geometry.Type gtB = geomB.getType();
		Geometry.Type gt = geom.getType();
		if (gt == Geometry.Type.MultiPoint) {
			if (gtA == Geometry.Type.Point || gtB == Geometry.Type.Point) {
				MultiPoint mp = (MultiPoint) geom;
				if (mp.getPointCount() <= 1) {
					Point pt = new Point(geom.getDescription());
					if (!mp.isEmpty())
						mp.getPointByVal(0, pt);
					return (Geometry) pt;
				}
			}
		}
		return geom;
	}

	private void flushVertices_(int geometry, AttributeStreamOfInt32 vertices) {
		EditShape shape = m_topo_graph.getShape();
		int path = shape.insertPath(geometry, -1);
		int size = vertices.size();
		// _ASSERT(size != 0);
		for (int i = 0; i < size; i++) {
			int vertex = vertices.get(i);
			shape.addVertex(path, vertex);
		}
		shape.setClosedPath(path, true);// need to close polygon rings
	}

	private void setHalfEdgeOrientations_(int orientationIndex, int cutter) {
		EditShape shape = m_topo_graph.getShape();

		for (int igeometry = shape.getFirstGeometry(); igeometry != -1; igeometry = shape
				.getNextGeometry(igeometry)) {
			if (igeometry != cutter)
				continue;

			for (int ipath = shape.getFirstPath(igeometry); ipath != -1; ipath = shape
					.getNextPath(ipath)) {
				int ivertex = shape.getFirstVertex(ipath);
				if (ivertex == -1)
					continue;

				int ivertexNext = shape.getNextVertex(ivertex);
				assert (ivertexNext != -1);

				while (ivertexNext != -1) {
					int clusterFrom = m_topo_graph
							.getClusterFromVertex(ivertex);
					int clusterTo = m_topo_graph
							.getClusterFromVertex(ivertexNext);
					int half_edge = m_topo_graph.getHalfEdgeConnector(
							clusterFrom, clusterTo);

					if (half_edge != -1) {
						int halfEdgeTwin = m_topo_graph
								.getHalfEdgeTwin(half_edge);
						m_topo_graph.setHalfEdgeUserIndex(half_edge,
								orientationIndex, 1);
						m_topo_graph.setHalfEdgeUserIndex(halfEdgeTwin,
								orientationIndex, 2);
					}

					ivertex = ivertexNext;
					ivertexNext = shape.getNextVertex(ivertex);
				}
			}
		}
	}

	private void processPolygonCuts_(int orientationIndex, int sideIndex,
			int cuttee, int cutter) {
		int idCuttee = m_topo_graph.getGeometryID(cuttee);
		int idCutter = m_topo_graph.getGeometryID(cutter);
		AttributeStreamOfInt32 vertices = new AttributeStreamOfInt32(0);
		vertices.reserve(256);
		EditShape shape = m_topo_graph.getShape();

		int visitedIndex = m_topo_graph.createUserIndexForHalfEdges();
		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int firstHalfEdge = m_topo_graph.getClusterHalfEdge(cluster);

			if (firstHalfEdge == -1)
				continue;

			int half_edge = firstHalfEdge;

			do {
				int visited = m_topo_graph.getHalfEdgeUserIndex(half_edge,
						visitedIndex);
				if (visited != 1) {
					int faceHalfEdge = half_edge;
					int toHalfEdge = half_edge;
					boolean bFoundCutter = false;
					int side = 0;
					do {
						m_topo_graph.setHalfEdgeUserIndex(faceHalfEdge,
								visitedIndex, 1);
						if (!bFoundCutter) {
							int edgeParentage = m_topo_graph
									.getHalfEdgeParentage(faceHalfEdge);
							if ((edgeParentage & idCutter) != 0) {
								int faceParentage = m_topo_graph
										.getHalfEdgeFaceParentage(faceHalfEdge);
								if ((faceParentage & idCuttee) != 0) {
									toHalfEdge = faceHalfEdge;// reset the loop
									bFoundCutter = true;
								}
							}
						}

						if (bFoundCutter) {
							int clusterOrigin = m_topo_graph
									.getHalfEdgeOrigin(faceHalfEdge);
							int iterator = m_topo_graph
									.getClusterVertexIterator(clusterOrigin);
							assert (iterator != -1);
							int vertex = m_topo_graph
									.getVertexFromVertexIterator(iterator);
							vertices.add(vertex);

							// get side
							if (orientationIndex != -1) {
								int edgeParentage = m_topo_graph
										.getHalfEdgeParentage(faceHalfEdge);
								if ((edgeParentage & idCutter) != 0) {
									int orientation = m_topo_graph
											.getHalfEdgeUserIndex(faceHalfEdge,
													orientationIndex);
									assert (orientation == 1 || orientation == 2);
									side |= orientation;
								}
							}
						}

						int next = m_topo_graph.getHalfEdgeNext(faceHalfEdge);
						faceHalfEdge = next;
					} while (faceHalfEdge != toHalfEdge);

					if (bFoundCutter
							&& m_topo_graph.getChainArea(m_topo_graph
									.getHalfEdgeChain(toHalfEdge)) > 0.0) {// if
																			// we
																			// found
																			// a
																			// cutter
																			// face
																			// and
																			// its
																			// area
																			// is
																			// positive,
																			// then
																			// add
																			// the
																			// cutter
																			// face
																			// as
																			// new
																			// polygon.
						int geometry = shape
								.createGeometry(Geometry.Type.Polygon);
						flushVertices_(geometry, vertices);// adds the cutter
															// face vertices to
															// the new polygon

						if (sideIndex != -1)
							shape.setGeometryUserIndex(geometry, sideIndex,
									side); // what is that?
					}

					vertices.clear(false);
				}
				half_edge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(half_edge));
			} while (half_edge != firstHalfEdge);
		}

		m_topo_graph.deleteUserIndexForHalfEdges(visitedIndex);
	}

	private void cutPolygonPolyline_(int sideIndex, int cuttee, int cutter,
			AttributeStreamOfInt32 cutHandles) {
		m_topo_graph.removeSpikes_();

		int orientationIndex = -1;
		if (sideIndex != -1) {
			orientationIndex = m_topo_graph.createUserIndexForHalfEdges();
			setHalfEdgeOrientations_(orientationIndex, cutter);
		}

		processPolygonCuts_(orientationIndex, sideIndex, cuttee, cutter);

		EditShape shape = m_topo_graph.getShape();

		int cutCount = 0;
		for (int geometry_handle = shape.getFirstGeometry(); geometry_handle != -1; geometry_handle = shape
				.getNextGeometry(geometry_handle)) {
			if (geometry_handle != cuttee && geometry_handle != cutter) {
				cutHandles.add(geometry_handle);
				cutCount++;
			}
		}

		// sort
		CompareCuts compareCuts = new CompareCuts(shape);
		cutHandles.Sort(0, cutCount, compareCuts);
	}
	
	//call this if EditShape instance has to survive the TopologicalOperations life.
	void removeShape() {
		if (m_topo_graph != null) {
			m_topo_graph.removeShape();
			m_topo_graph = null;
		}
			
	}

}
