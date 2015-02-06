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

class SegmentIntersector {
	private static class IntersectionPart {
		public Segment seg;
		// Weight controls the snapping. When points of the same rank are
		// snapped together,
		// The new posistion is calculated as a weighted average.
		public double weight_start;
		public double weight_end;
		// The rank controls the snapping. The point with lower rank will be
		// snapped to the point with the higher rank.
		public int rank_start; // the rank of the start point
		public int rank_end; // the rank of the end point
		public int rank_interior; // the rank of the interior point

		IntersectionPart(Segment _seg) {
			seg = _seg;
			weight_start = 1.0;
			weight_end = 1.0;
			rank_start = 0;
			rank_end = 0;
			rank_interior = 0;
		}
	}

	// typedef std::shared_ptr<Segment_buffer> segment_buffer_sptr;
	// typedef std::shared_ptr<Intersection_part> intersection_part_sptr;
	// typedef Dynamic_array<intersection_part_sptr> intersection_parts;

	private ArrayList<IntersectionPart> m_input_segments;
	private ArrayList<IntersectionPart> m_result_segments_1;
	private ArrayList<IntersectionPart> m_result_segments_2;
	private ArrayList<IntersectionPart> m_recycled_intersection_parts;
	private ArrayList<SegmentBuffer> m_recycled_segments;
	private double[] m_param_1 = new double[15];
	private double[] m_param_2 = new double[15];
	private Point m_point = new Point();

	private int m_used_recycled_segments;

	private void recycle_(ArrayList<IntersectionPart> parts) {
		if (parts == null)
			return;

		for (int i = 0, n = (int) parts.size(); i < n; i++) {
			recycle_(parts.get(i));
		}

		parts.clear();
	}

	private void recycle_(IntersectionPart part) {
		part.seg = null;
		m_recycled_intersection_parts.add(part);
	}

	private IntersectionPart newIntersectionPart_(Segment _seg) {
		if (m_recycled_intersection_parts.isEmpty()) {
			IntersectionPart part = new IntersectionPart(_seg);
			return part;
		} else {
			IntersectionPart part = m_recycled_intersection_parts
					.get(m_recycled_intersection_parts.size() - 1);
			part.seg = _seg;
			m_recycled_intersection_parts.remove(m_recycled_intersection_parts
					.size() - 1);
			return part;
		}
	}

	private IntersectionPart getResultPart_(int input_segment_index,
			int segment_index) {
		if (input_segment_index == 0) {
			return m_result_segments_1.get(segment_index);
		} else {
			assert (input_segment_index == 1);
			return m_result_segments_2.get(segment_index);
		}
	}

	private SegmentBuffer newSegmentBuffer_() {
		if (m_used_recycled_segments >= m_recycled_segments.size()) {
			m_recycled_segments.add(new SegmentBuffer());
		}

		SegmentBuffer p = m_recycled_segments.get(m_used_recycled_segments);
		m_used_recycled_segments++;
		return p;
	}

	private double m_tolerance;

	public SegmentIntersector() {
		m_used_recycled_segments = 0;
		m_tolerance = 0;
		m_input_segments = new ArrayList<IntersectionPart>();
		m_result_segments_1 = new ArrayList<IntersectionPart>();
		m_result_segments_2 = new ArrayList<IntersectionPart>();
		m_recycled_intersection_parts = new ArrayList<IntersectionPart>();
		m_recycled_segments = new ArrayList<SegmentBuffer>();
	}

	// Clears the results and input segments
	public void clear() {
		recycle_(m_input_segments);
		recycle_(m_result_segments_1);
		recycle_(m_result_segments_2);
		m_used_recycled_segments = 0;
	}

	// Adds a segment to intersect and returns an index of the segment.
	// Two segments has to be pushed for the intersect method to succeed.
	public int pushSegment(Segment seg) {
		assert (m_input_segments.size() < 2);
		m_input_segments.add(newIntersectionPart_(seg));
		// m_param_1.resize(15);
		// m_param_2.resize(15);
		return (int) m_input_segments.size() - 1;
	}

	public void setRankAndWeight(int input_segment_index, double start_weight,
			int start_rank, double end_weight, int end_rank, int interior_rank) {
		IntersectionPart part = m_input_segments.get(input_segment_index);
		part.rank_end = end_rank;
		part.weight_start = start_weight;
		part.weight_end = end_weight;
		part.rank_start = start_rank;
		part.rank_end = end_rank;
		part.rank_interior = interior_rank;
	}

	// Returns the number of segments the input segment has been split to.
	public int getResultSegmentCount(int input_segment_index) {
		if (input_segment_index == 0) {
			return (int) m_result_segments_1.size();
		} else {
			assert (input_segment_index == 1);
			return (int) m_result_segments_2.size();
		}
	}

	// Returns a part of the input segment that is the result of the
	// intersection with another segment.
	// input_segment_index is the index of the input segment.
	// segment_index is between 0 and
	// get_result_segment_count(input_segment_index) - 1
	public Segment getResultSegment(int input_segment_index, int segment_index) {
		return getResultPart_(input_segment_index, segment_index).seg;
	}

	// double get_result_segment_start_point_weight(int input_segment_index, int
	// segment_index);
	// int get_result_segment_start_point_rank(int input_segment_index, int
	// segment_index);
	// double get_result_segment_end_point_weight(int input_segment_index, int
	// segment_index);
	// int get_result_segment_end_point_rank(int input_segment_index, int
	// segment_index);
	// int get_result_segment_interior_rank(int input_segment_index, int
	// segment_index);
	public Point getResultPoint() {
		return m_point;
	}

	// Performs the intersection
	public boolean intersect(double tolerance, boolean b_intersecting) {
		if (m_input_segments.size() != 2)
			throw GeometryException.GeometryInternalError();

		m_tolerance = tolerance;
		double small_tolerance_sqr = MathUtils.sqr(tolerance * 0.01);
		boolean bigmove = false;
		
		IntersectionPart part1 = m_input_segments.get(0);
		IntersectionPart part2 = m_input_segments.get(1);
		if (b_intersecting
				|| (part1.seg._isIntersecting(part2.seg, tolerance, true) & 5) != 0) {
			if (part1.seg.getType().value() == Geometry.GeometryType.Line) {
				Line line_1 = (Line) part1.seg;
				if (part2.seg.getType().value() == Geometry.GeometryType.Line) {
					Line line_2 = (Line) part2.seg;
					int count = Line._intersectLineLine(line_1, line_2, null,
							m_param_1, m_param_2, tolerance);
					if (count == 0) {
						assert (count > 0);
						throw GeometryException.GeometryInternalError();
					}
					Point2D[] points = new Point2D[9];
					for (int i = 0; i < count; i++) {
						// For each point of intersection, we calculate a
						// weighted point
						// based on the ranks and weights of the endpoints and
						// the interior.
						double t1 = m_param_1[i];
						double t2 = m_param_2[i];
						int rank1 = part1.rank_interior;
						double weight1 = 1.0;

						if (t1 == 0) {
							rank1 = part1.rank_start;
							weight1 = part1.weight_start;
						} else if (t1 == 1.0) {
							rank1 = part1.rank_end;
							weight1 = part1.weight_end;
						}

						int rank2 = part2.rank_interior;
						double weight2 = 1.0;
						if (t2 == 0) {
							rank2 = part2.rank_start;
							weight2 = part2.weight_start;
						} else if (t2 == 1.0) {
							rank2 = part2.rank_end;
							weight2 = part2.weight_end;
						}

						double ptWeight;

						Point2D pt = new Point2D();
						if (rank1 == rank2) {// for equal ranks use weighted sum
							Point2D pt_1 = new Point2D();
							line_1.getCoord2D(t1, pt_1);
							Point2D pt_2 = new Point2D();
							line_2.getCoord2D(t2, pt_2);
							ptWeight = weight1 + weight2;
							double t = weight2 / ptWeight;
							MathUtils.lerp(pt_1, pt_2, t, pt);
							if (Point2D.sqrDistance(pt, pt_1)
									+ Point2D.sqrDistance(pt, pt_2) > small_tolerance_sqr)
								bigmove = true;
							
						} else {// for non-equal ranks, the higher rank wins
							if (rank1 > rank2) {
								line_1.getCoord2D(t1, pt);
								ptWeight = weight1;
								Point2D pt_2 = new Point2D();
								line_2.getCoord2D(t2, pt_2);
								if (Point2D.sqrDistance(pt, pt_2) > small_tolerance_sqr)
									bigmove = true;
							} else {
								line_2.getCoord2D(t2, pt);
								ptWeight = weight2;
								Point2D pt_1 = new Point2D();
								line_1.getCoord2D(t1, pt_1);
								if (Point2D.sqrDistance(pt, pt_1) > small_tolerance_sqr)
									bigmove = true;
							}
						}
						points[i] = pt;
					}

					// Split the line_1, making sure the endpoints are adusted
					// to the weighted
					double t0 = 0;
					int i0 = -1;
					for (int i = 0; i <= count; i++) {
						double t = i < count ? m_param_1[i] : 1.0;
						if (t != t0) {
							SegmentBuffer seg_buffer = newSegmentBuffer_();
							line_1.cut(t0, t, seg_buffer);
							if (i0 != -1)
								seg_buffer.get().setStartXY(points[i0]);
							if (i != count)
								seg_buffer.get().setEndXY(points[i]);

							t0 = t;
							m_result_segments_1
									.add(newIntersectionPart_(seg_buffer.get()));
						}
						i0 = i;
					}

					int[] indices = new int[9];
					for (int i = 0; i < count; i++)
						indices[i] = i;

					if (count > 1) {
						if (m_param_2[0] > m_param_2[1]) {
							double t = m_param_2[0];
							m_param_2[0] = m_param_2[1];
							m_param_2[1] = t;
							int i = indices[0];
							indices[0] = indices[1];
							indices[1] = i;
						}
					}

					// Split the line_2
					t0 = 0;
					i0 = -1;
					for (int i = 0; i <= count; i++) {
						double t = i < count ? m_param_2[i] : 1.0;
						if (t != t0) {
							SegmentBuffer seg_buffer = newSegmentBuffer_();
							line_2.cut(t0, t, seg_buffer);
							if (i0 != -1) {
								int ind = indices[i0];
								seg_buffer.get().setStartXY(points[ind]);
							}
							if (i != count) {
								int ind = indices[i];
								seg_buffer.get().setEndXY(points[ind]);
							}

							t0 = t;
							m_result_segments_2
									.add(newIntersectionPart_(seg_buffer.get()));
						}
						i0 = i;
					}

					return bigmove;
				}

				throw GeometryException.GeometryInternalError();
			}

			throw GeometryException.GeometryInternalError();
		}
		
		return false;
	}

	public void intersect(double tolerance, Point pt_intersector_point,
			int point_rank, double point_weight, boolean b_intersecting) {
		pt_intersector_point.copyTo(m_point);
		if (m_input_segments.size() != 1)
			throw GeometryException.GeometryInternalError();

		m_tolerance = tolerance;

		IntersectionPart part1 = m_input_segments.get(0);
		if (b_intersecting
				|| part1.seg._isIntersectingPoint(pt_intersector_point.getXY(),
						tolerance, true)) {
			if (part1.seg.getType().value() == Geometry.GeometryType.Line) {
				Line line_1 = (Line) (part1.seg);
				double t1 = line_1.getClosestCoordinate(
						pt_intersector_point.getXY(), false);
				m_param_1[0] = t1;
				// For each point of intersection, we calculate a weighted point
				// based on the ranks and weights of the endpoints and the
				// interior.
				int rank1 = part1.rank_interior;
				double weight1 = 1.0;

				if (t1 == 0) {
					rank1 = part1.rank_start;
					weight1 = part1.weight_start;
				} else if (t1 == 1.0) {
					rank1 = part1.rank_end;
					weight1 = part1.weight_end;
				}

				int rank2 = point_rank;
				double weight2 = point_weight;

				double ptWeight;

				Point2D pt = new Point2D();
				if (rank1 == rank2) {// for equal ranks use weighted sum
					Point2D pt_1 = new Point2D();
					line_1.getCoord2D(t1, pt_1);
					Point2D pt_2 = pt_intersector_point.getXY();
					ptWeight = weight1 + weight2;
					double t = weight2 / ptWeight;
					MathUtils.lerp(pt_1,  pt_2, t, pt);
				} else {// for non-equal ranks, the higher rank wins
					if (rank1 > rank2) {
						pt = new Point2D();
						line_1.getCoord2D(t1, pt);
						ptWeight = weight1;
					} else {
						pt = pt_intersector_point.getXY();
						ptWeight = weight2;
					}
				}

				// Split the line_1, making sure the endpoints are adusted to
				// the weighted
				double t0 = 0;
				int i0 = -1;
				int count = 1;
				for (int i = 0; i <= count; i++) {
					double t = i < count ? m_param_1[i] : 1.0;
					if (t != t0) {
						SegmentBuffer seg_buffer = newSegmentBuffer_();
						line_1.cut(t0, t, seg_buffer);
						if (i0 != -1)
							seg_buffer.get().setStartXY(pt);
						if (i != count)
							seg_buffer.get().setEndXY(pt);

						t0 = t;
						m_result_segments_1.add(newIntersectionPart_(seg_buffer
								.get()));
					}
					i0 = i;
				}

				m_point.setXY(pt);

				return;
			}

			throw GeometryException.GeometryInternalError();
		}
	}

	public double get_tolerance_() {
		return m_tolerance;
	}
}
