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


/**
 * Implementation for the segment cracking.
 * 
 * Finds and splits all intersecting segments. Used by the TopoGraph and
 * Simplify.
 */
final class Cracker {
	private EditShape m_shape;
	private ProgressTracker m_progress_tracker;
	private NonSimpleResult m_non_simple_result;
	private double m_tolerance;
	private Treap m_sweep_structure;
	private SweepComparator m_sweep_comparator;
	private boolean m_bAllowCoincident;

	private Segment getSegment_(int vertex, Line lineHelper) {
		Segment seg = m_shape.getSegment(vertex);
		if (seg == null) {
			if (!m_shape.queryLineConnector(vertex, lineHelper))
				return null;
			
			seg = (Segment)lineHelper;
		}
		
		return seg;
	}
	
	private boolean crackBruteForce_() {
		EditShape.VertexIterator iter_1 = m_shape.queryVertexIterator(false);
		boolean b_cracked = false;
		Line line_1 = new Line();
		Line line_2 = new Line();
		Envelope2D seg_1_env = new Envelope2D();
		seg_1_env.setEmpty();
		Envelope2D seg_2_env = new Envelope2D();
		seg_2_env.setEmpty();
		boolean assume_intersecting = false;
		Point helper_point = new Point();
		SegmentIntersector segment_intersector = new SegmentIntersector();

		for (int vertex_1 = iter_1.next(); vertex_1 != -1; vertex_1 = iter_1
				.next()) {
			ProgressTracker.checkAndThrow(m_progress_tracker);

			int GT_1 = m_shape.getGeometryType(iter_1.currentGeometry());

			Segment seg_1 = null;
			boolean seg_1_zero = false;
			if (!Geometry.isPoint(GT_1)) {
				seg_1 = getSegment_(vertex_1, line_1);
				if (seg_1 == null)
					continue;
				
				seg_1.queryEnvelope2D(seg_1_env);
				seg_1_env.inflate(m_tolerance, m_tolerance);

				if (seg_1.isDegenerate(m_tolerance))// do not crack with
													// degenerate segments
				{
					if (seg_1.isDegenerate(0)) {
						seg_1_zero = true;
						seg_1 = null;
					}
					else {
						continue;
					}
				}
			}

			EditShape.VertexIterator iter_2 = m_shape
					.queryVertexIterator(iter_1);
			int vertex_2 = iter_2.next();
			if (vertex_2 != -1)
				vertex_2 = iter_2.next();

			for (; vertex_2 != -1; vertex_2 = iter_2.next()) {
				int GT_2 = m_shape.getGeometryType(iter_2.currentGeometry());

				Segment seg_2 = null;
				boolean seg_2_zero = false;
				if (!Geometry.isPoint(GT_2)) {
					seg_2 = getSegment_(vertex_2, line_2);
					if (seg_2 == null) {
						continue;
					}
					
					seg_2.queryEnvelope2D(seg_2_env);
					if (seg_2.isDegenerate(m_tolerance))// do not crack with
														// degenerate segments
					{
						if (seg_2.isDegenerate(0)) {
							seg_2_zero = true;
							seg_2 = null;
						}
						else {
							continue;
						}
					}
				}

				int split_count_1 = 0;
				int split_count_2 = 0;
				if (seg_1 != null && seg_2 != null) {
					if (seg_1_env.isIntersectingNE(seg_2_env)) {
						segment_intersector.pushSegment(seg_1);
						segment_intersector.pushSegment(seg_2);
						segment_intersector.intersect(m_tolerance,
								assume_intersecting);
						split_count_1 = segment_intersector
								.getResultSegmentCount(0);
						split_count_2 = segment_intersector
								.getResultSegmentCount(1);
						if (split_count_1 + split_count_2 > 0) {
							m_shape.splitSegment_(vertex_1,
									segment_intersector, 0, true);
							m_shape.splitSegment_(vertex_2,
									segment_intersector, 1, true);
						}
						segment_intersector.clear();
					}
				} else {
					if (seg_1 != null) {
						Point2D pt = new Point2D();
						m_shape.getXY(vertex_2, pt);
						if (seg_1_env.contains(pt)) {
							segment_intersector.pushSegment(seg_1);
							m_shape.queryPoint(vertex_2, helper_point);
							segment_intersector.intersect(m_tolerance,
									helper_point, 0, 1.0, assume_intersecting);
							split_count_1 = segment_intersector
									.getResultSegmentCount(0);
							if (split_count_1 > 0) {
								m_shape.splitSegment_(vertex_1,
										segment_intersector, 0, true);
								if (seg_2_zero) {
				                    //seg_2 was zero length. Need to change all coincident points
				                    //segment at vertex_2 is dzero length, change all attached zero length segments
				                    int v_to = -1;
				                    for (int v = m_shape.getNextVertex(vertex_2); v != -1 && v != vertex_2; v = m_shape.getNextVertex(v))
				                    {
				                      seg_2 = getSegment_(v, line_2);
				                      v_to = v;
				                      if (seg_2 == null || !seg_2.isDegenerate(0))
				                        break;
				                    }
				                    //change from vertex_2 to v_to (inclusive).
				                    for (int v = vertex_2; v != -1; v = m_shape.getNextVertex(v))
				                    {
				                      m_shape.setPoint(v, segment_intersector.getResultPoint());
				                      if (v == v_to)
				                        break;
				                    }									
								}
								else {
									m_shape.setPoint(vertex_2,
											segment_intersector.getResultPoint());
								}
							}
							segment_intersector.clear();
						}
					} else if (seg_2 != null) {
						Point2D pt = new Point2D();
						m_shape.getXY(vertex_1, pt);
						seg_2_env.inflate(m_tolerance, m_tolerance);
						if (seg_2_env.contains(pt)) {
							segment_intersector.pushSegment(seg_2);
							m_shape.queryPoint(vertex_1, helper_point);
							segment_intersector.intersect(m_tolerance,
									helper_point, 0, 1.0, assume_intersecting);
							split_count_2 = segment_intersector
									.getResultSegmentCount(0);
							if (split_count_2 > 0) {
								m_shape.splitSegment_(vertex_2,
										segment_intersector, 0, true);
								if (seg_1_zero) {
				                    //seg_1 was zero length. Need to change all coincident points
				                    //segment at vertex_2 is dzero length, change all attached zero length segments
				                    int v_to = -1;
				                    for (int v = m_shape.getNextVertex(vertex_1); v != -1 && v != vertex_1; v = m_shape.getNextVertex(v)) {
				                      seg_2 = getSegment_(v, line_2);//using here seg_2 for seg_1
				                      v_to = v;
				                      if (seg_2 == null || !seg_2.isDegenerate(0))
				                        break;
				                    }
				                    //change from vertex_2 to v_to (inclusive).
				                    for (int v = vertex_1; v != -1; v = m_shape.getNextVertex(v)) {
				                      m_shape.setPoint(v, segment_intersector.getResultPoint());
				                      if (v == v_to)
				                        break;
				                    }
								}
								else {
									m_shape.setPoint(vertex_1,
											segment_intersector.getResultPoint());
								}
							}
							segment_intersector.clear();
						}
					} else {
						continue;// points on points
					}
				}

				if (split_count_1 + split_count_2 != 0) {
					if (split_count_1 != 0) {
						seg_1 = m_shape.getSegment(vertex_1);// reload segment
																// after split
						if (seg_1 == null) {
							if (!m_shape.queryLineConnector(vertex_1, line_1))
								continue;
							seg_1 = line_1;
							line_1.queryEnvelope2D(seg_1_env);
						} else
							seg_1.queryEnvelope2D(seg_1_env);

						if (seg_1.isDegenerate(m_tolerance))// do not crack with
															// degenerate
															// segments
						{
							break;
						}
					}

					b_cracked = true;
				}
			}
		}

		return b_cracked;
	}

	boolean crackerPlaneSweep_() {
		boolean b_cracked = planeSweep_();
		return b_cracked;
	}

	boolean planeSweep_() {
		PlaneSweepCrackerHelper plane_sweep = new PlaneSweepCrackerHelper();
		boolean b_cracked = plane_sweep.sweep(m_shape, m_tolerance);
		return b_cracked;
	}

	boolean needsCrackingImpl_() {
		boolean b_needs_cracking = false;
		
		if (m_sweep_structure == null)
			m_sweep_structure = new Treap();

		AttributeStreamOfInt32 event_q = new AttributeStreamOfInt32(0);
		event_q.reserve(m_shape.getTotalPointCount() + 1);

		EditShape.VertexIterator iter = m_shape.queryVertexIterator();
		for (int vert = iter.next(); vert != -1; vert = iter.next()) {
			event_q.add(vert);
		}
		assert (m_shape.getTotalPointCount() == event_q.size());

		m_shape.sortVerticesSimpleByY_(event_q, 0, event_q.size());
		event_q.add(-1);// for termination;
		// create user indices to store edges that end at vertices.
		int edge_index_1 = m_shape.createUserIndex();
		int edge_index_2 = m_shape.createUserIndex();
		m_sweep_comparator = new SweepComparator(m_shape, m_tolerance, !m_bAllowCoincident);
		m_sweep_structure.setComparator(m_sweep_comparator);

		AttributeStreamOfInt32 swept_edges_to_delete = new AttributeStreamOfInt32(
				0);
		AttributeStreamOfInt32 edges_to_insert = new AttributeStreamOfInt32(0);

		// Go throught the sorted vertices
		int event_q_index = 0;
		Point2D cluster_pt = new Point2D();

		// sweep-line algorithm:
		for (int vertex = event_q.get(event_q_index++); vertex != -1;) {
			m_shape.getXY(vertex, cluster_pt);
			
			do {
				int next_vertex = m_shape.getNextVertex(vertex);
				int prev_vertex = m_shape.getPrevVertex(vertex);

				if (next_vertex != -1
						&& m_shape.compareVerticesSimpleY_(vertex, next_vertex) < 0) {
					edges_to_insert.add(vertex);
					edges_to_insert.add(next_vertex);
				}

				if (prev_vertex != -1
						&& m_shape.compareVerticesSimpleY_(vertex, prev_vertex) < 0) {
					edges_to_insert.add(prev_vertex);
					edges_to_insert.add(prev_vertex);
				}

				// Continue accumulating current cluster
				int attached_edge_1 = m_shape
						.getUserIndex(vertex, edge_index_1);
				if (attached_edge_1 != -1) {
					swept_edges_to_delete.add(attached_edge_1);
					m_shape.setUserIndex(vertex, edge_index_1, -1);
				}
				int attached_edge_2 = m_shape
						.getUserIndex(vertex, edge_index_2);
				if (attached_edge_2 != -1) {
					swept_edges_to_delete.add(attached_edge_2);
					m_shape.setUserIndex(vertex, edge_index_2, -1);
				}
				vertex = event_q.get(event_q_index++);
			} while (vertex != -1 && m_shape.isEqualXY(vertex, cluster_pt));

			boolean b_continuing_segment_chain_optimization = swept_edges_to_delete
					.size() == 1 && edges_to_insert.size() == 2;

			int new_left = -1;
			int new_right = -1;
			// Process the cluster
			for (int i = 0, n = swept_edges_to_delete.size(); i < n; i++) {
				// Find left and right neighbour of the edges that terminate at
				// the cluster (there will be atmost only one left and one
				// right).
				int edge = swept_edges_to_delete.get(i);
				int left = m_sweep_structure.getPrev(edge);
				if (left != -1 && !swept_edges_to_delete.hasElement(left))// Note:
																			// for
																			// some
																			// heavy
																			// cases,
																			// it
																			// could
																			// be
																			// better
																			// to
																			// use
																			// binary
																			// search.
				{
					assert (new_left == -1);
					new_left = left;
				}

				int right = m_sweep_structure.getNext(edge);
				if (right != -1 && !swept_edges_to_delete.hasElement(right)) {
					assert (new_right == -1);
					new_right = right;
				}
//#ifdef NDEBUG				
				if (new_left != -1 && new_right != -1)
					break;
//#endif
			}

			assert (new_left == -1 || new_left != new_right);

			m_sweep_comparator.setSweepY(cluster_pt.y, cluster_pt.x);

			// Delete the edges that terminate at the cluster.
			for (int i = 0, n = swept_edges_to_delete.size(); i < n; i++) {
				int edge = swept_edges_to_delete.get(i);
				m_sweep_structure.deleteNode(edge, -1);
			}
			swept_edges_to_delete.clear(false);

			if (!b_continuing_segment_chain_optimization && new_left != -1 && new_right != -1) {
				if (checkForIntersections_(new_left, new_right)) {
					b_needs_cracking = true;
					m_non_simple_result = m_sweep_comparator.getResult();
					break;
				}
			}

			for (int i = 0, n = edges_to_insert.size(); i < n; i += 2) {
				int v = edges_to_insert.get(i);
				int otherv = edges_to_insert.get(i + 1);

				int new_edge_1 = -1;
				if (b_continuing_segment_chain_optimization) {
					new_edge_1 = m_sweep_structure.addElementAtPosition(
							new_left, new_right, v, true, true, -1);
					b_continuing_segment_chain_optimization = false;
				} else {
					new_edge_1 = m_sweep_structure.addElement(v, -1); // the
																		// sweep
																		// structure
																		// consist
																		// of
																		// the
																		// origin
																		// vertices
																		// for
																		// edges.
																		// One
																		// can
																		// always
																		// get
																		// the
																		// other
																		// endpoint
																		// as
																		// the
																		// next
																		// vertex.
				}

				if (m_sweep_comparator.intersectionDetected()) {
					m_non_simple_result = m_sweep_comparator.getResult();
					b_needs_cracking = true;
					break;
				}

				int e_1 = m_shape.getUserIndex(otherv, edge_index_1);
				if (e_1 == -1)
					m_shape.setUserIndex(otherv, edge_index_1, new_edge_1);
				else {
					assert (m_shape.getUserIndex(otherv, edge_index_2) == -1);
					m_shape.setUserIndex(otherv, edge_index_2, new_edge_1);
				}
			}

			if (b_needs_cracking)
				break;

			// Start accumulating new cluster
			edges_to_insert.resizePreserveCapacity(0);
		}

		m_shape.removeUserIndex(edge_index_1);
		m_shape.removeUserIndex(edge_index_2);
		return b_needs_cracking;
	}

	boolean checkForIntersections_(int sweep_edge_1, int sweep_edge_2) {
		assert (sweep_edge_1 != sweep_edge_2);
		int left = m_sweep_structure.getElement(sweep_edge_1);
		assert (left != m_sweep_structure.getElement(sweep_edge_2));
		m_sweep_comparator.compare(m_sweep_structure, left, sweep_edge_2);// compare
																			// detects
																			// intersections
		boolean b_intersects = m_sweep_comparator.intersectionDetected();
		m_sweep_comparator.clearIntersectionDetectedFlag();
		return b_intersects;
	}

	// void dbg_print_sweep_edge_(int edge);
	// void dbg_print_sweep_structure_();
	// void dbg_check_sweep_structure_();
	Cracker(ProgressTracker progress_tracker) {
		m_progress_tracker = progress_tracker;
		m_bAllowCoincident = true;
	}

	static boolean canBeCracked(EditShape shape) {
		for (int geometry = shape.getFirstGeometry(); geometry != -1; geometry = shape
				.getNextGeometry(geometry)) {
			if (!Geometry.isMultiPath(shape.getGeometryType(geometry)))
				continue;
			return true;
		}
		return false;
	}

	static boolean execute(EditShape shape, Envelope2D extent,
			double tolerance, ProgressTracker progress_tracker) {
		if (!canBeCracked(shape)) // make sure it contains some segments,
									// otherwise no need to crack.
			return false;

		Cracker cracker = new Cracker(progress_tracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		// Use brute force for smaller shapes, and a planesweep for bigger
		// shapes.
		boolean b_cracked = false;
		if (shape.getTotalPointCount() < 15) // what is a good number?
		{
			b_cracked = cracker.crackBruteForce_();
		} else {
			boolean b_cracked_1 = cracker.crackerPlaneSweep_();
			return b_cracked_1;
		}
		return b_cracked;
	}

	static boolean execute(EditShape shape, double tolerance,
			ProgressTracker progress_tracker) {
		return Cracker.execute(shape, shape.getEnvelope2D(), tolerance,
				progress_tracker);
	}

	// Used for IsSimple.
	static boolean needsCracking(boolean allowCoincident, EditShape shape, double tolerance,
			NonSimpleResult result, ProgressTracker progress_tracker) {
		if (!canBeCracked(shape))
			return false;

		Cracker cracker = new Cracker(progress_tracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		cracker.m_bAllowCoincident = allowCoincident;
		if (cracker.needsCrackingImpl_()) {
			if (result != null)
				result.Assign(cracker.m_non_simple_result);
			return true;
		}

		// Now swap the coordinates to catch horizontal cases.
		Transformation2D transform = new Transformation2D();
		transform.setSwapCoordinates();
		shape.applyTransformation(transform);

		cracker = new Cracker(progress_tracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		cracker.m_bAllowCoincident = allowCoincident;
		boolean b_res = cracker.needsCrackingImpl_();

		transform.setSwapCoordinates();
		shape.applyTransformation(transform);// restore shape

		if (b_res) {
			if (result != null)
				result.Assign(cracker.m_non_simple_result);
			return true;
		}

		return false;
	}
}
