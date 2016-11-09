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
import java.util.List;

class Bufferer {
	Bufferer() {
		m_buffer_commands = new ArrayList<BufferCommand>(128);
		m_progress_tracker = null;
		m_tolerance = 0;
		m_small_tolerance = 0;
		m_filter_tolerance = 0;
		m_distance = 0;
		m_original_geom_type = Geometry.GeometryType.Unknown;
		m_abs_distance_reversed = 0;
		m_abs_distance = 0;
		m_densify_dist = -1;
		m_dA = -1;
		m_b_output_loops = true;
		m_bfilter = true;
		m_old_circle_template_size = 0;
	}

	
	/**
	 * Result is always a polygon. For non positive distance and non-areas
	 * returns an empty polygon. For points returns circles.
	 */
	Geometry buffer(Geometry geometry, double distance,
			SpatialReference sr, double densify_dist,
			int max_vertex_in_complete_circle, ProgressTracker progress_tracker) {
		if (geometry == null)
			throw new IllegalArgumentException();

		if (densify_dist < 0)
			throw new IllegalArgumentException();

		if (geometry.isEmpty())
			return new Polygon(geometry.getDescription());

		Envelope2D env2D = new Envelope2D();
		geometry.queryLooseEnvelope2D(env2D);
		if (distance > 0)
			env2D.inflate(distance, distance);

		m_progress_tracker = progress_tracker;

		m_original_geom_type = geometry.getType().value();
		m_geometry = geometry;
		m_tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env2D, true);// conservative to have same effect as simplify
		m_small_tolerance = InternalUtils
				.calculateToleranceFromGeometry(null, env2D, true);// conservative
																	// to have
																	// same
																	// effect as
																	// simplify

		if (max_vertex_in_complete_circle <= 0) {
			max_vertex_in_complete_circle = 96;// 96 is the value used by SG.
												// This is the number of
												// vertices in the full circle.
		}
		
		m_spatialReference = sr;
		m_distance = distance;
		m_abs_distance = Math.abs(m_distance);
		m_abs_distance_reversed = m_abs_distance != 0 ? 1.0 / m_abs_distance
				: 0;

		if (NumberUtils.isNaN(densify_dist) || densify_dist == 0) {
			densify_dist = m_abs_distance * 1e-5;
		} else {
			if (densify_dist > m_abs_distance * 0.5)
				densify_dist = m_abs_distance * 0.5;// do not allow too
																// large densify
																// distance (the
																// value will be
																// adjusted
																// anyway later)
		}

		if (max_vertex_in_complete_circle < 12)
			max_vertex_in_complete_circle = 12;

		
		double max_dd = Math.abs(distance)
				* (1 - Math.cos(Math.PI / max_vertex_in_complete_circle));

		if (max_dd > densify_dist)
			densify_dist = max_dd;// the densify distance has to agree with the
									// max_vertex_in_complete_circle
		else {
			double vertex_count = Math.PI
					/ Math.acos(1.0 - densify_dist / Math.abs(distance));
			if (vertex_count < (double) max_vertex_in_complete_circle - 1.0) {
				max_vertex_in_complete_circle = (int) vertex_count;
				if (max_vertex_in_complete_circle < 12) {
					max_vertex_in_complete_circle = 12;
					densify_dist = Math.abs(distance)
							* (1 - Math.cos(Math.PI
									/ max_vertex_in_complete_circle));
				}
			}
		}

		m_densify_dist = densify_dist;
		m_max_vertex_in_complete_circle = max_vertex_in_complete_circle;
		// when filtering close points we do not want the filter to distort
		// generated buffer too much.
		m_filter_tolerance = Math.min(m_small_tolerance,
				densify_dist * 0.25);
		
		
		m_circle_template_size = calcN_();
		if (m_circle_template_size != m_old_circle_template_size) {
			// we have an optimization for this method to be called several
			// times. Here we detected too many changes and need to regenerate
			// the data.
			m_circle_template.clear();
			m_old_circle_template_size = m_circle_template_size;
		}

		Geometry result_geom = buffer_();
		m_geometry = null;
		return result_geom;		
	}

	private Geometry m_geometry;

	private static final class BufferCommand {
		private interface Flags {
			static final int enum_line = 1;
			static final int enum_arc = 2;
			static final int enum_connection = enum_arc | enum_line;
		}

		private Point2D m_from;
		private Point2D m_to;
		private Point2D m_center;
		private int m_next;
		private int m_prev;
		private int m_type;

		private BufferCommand(Point2D from, Point2D to, Point2D center,
				int type, int next, int prev) {
			m_from = new Point2D();
			m_to = new Point2D();
			m_center = new Point2D();
			m_from.setCoords(from);
			m_to.setCoords(to);
			m_center.setCoords(center);
			m_type = type;
			m_next = next;
			m_prev = prev;
		}

		private BufferCommand(Point2D from, Point2D to, int next, int prev,
				String dummy) {
			m_from = new Point2D();
			m_to = new Point2D();
			m_center = new Point2D();
			m_from.setCoords(from);
			m_to.setCoords(to);
			m_center.setNaN();
			m_type = 4;
			m_next = next;
			m_prev = prev;
		}
	}

	private ArrayList<BufferCommand> m_buffer_commands;

	private int m_original_geom_type;
	private ProgressTracker m_progress_tracker;
	private int m_max_vertex_in_complete_circle;
	private SpatialReference m_spatialReference;
	private double m_tolerance;
	private double m_small_tolerance;
	private double m_filter_tolerance;
	private double m_densify_dist;
	private double m_distance;
	private double m_abs_distance;
	private double m_abs_distance_reversed;
	private double m_dA;
	private boolean m_b_output_loops;
	private boolean m_bfilter;
	private ArrayList<Point2D> m_circle_template = new ArrayList<Point2D>(0);
	private ArrayList<Point2D> m_left_stack;
	private ArrayList<Point2D> m_middle_stack;
	private Line m_helper_line_1;
	private Line m_helper_line_2;
	private Point2D[] m_helper_array;
	private int m_progress_counter;
	private int m_circle_template_size;
	private int m_old_circle_template_size;

	private void generateCircleTemplate_() {
		if (!m_circle_template.isEmpty()) {
			return;
		}

		int N = m_circle_template_size;

		assert (N >= 4);
		int real_size = (N + 3) / 4;
		double dA = (Math.PI * 0.5) / real_size;
		m_dA = dA;

		for (int i = 0; i < real_size * 4; i++)
			m_circle_template.add(null);

		double dcos = Math.cos(dA);
		double dsin = Math.sin(dA);
		Point2D pt = new Point2D(0.0, 1.0);

		for (int i = 0; i < real_size; i++) {
			m_circle_template.set(i + real_size * 0, new Point2D(pt.y, -pt.x));
			m_circle_template.set(i + real_size * 1, new Point2D(-pt.x, -pt.y));
			m_circle_template.set(i + real_size * 2, new Point2D(-pt.y, pt.x));
			m_circle_template.set(i + real_size * 3, pt);
			pt = new Point2D(pt.x, pt.y);
			pt.rotateReverse(dcos, dsin);
		}
		// the template is filled with the index 0 corresponding to the point
		// (0, 0), following clockwise direction (0, -1), (-1, 0), (1, 0)
	}

	private static final class GeometryCursorForMultiPoint extends
			GeometryCursor {
		private Bufferer m_parent; 
		private int m_index;
		private Geometry m_buffered_polygon;
		private MultiPoint m_mp;
		private SpatialReference m_spatialReference;
		private double m_distance;
		private double m_densify_dist;
		private double m_x;
		private double m_y;
		private int m_max_vertex_in_complete_circle;
		private ProgressTracker m_progress_tracker;

		GeometryCursorForMultiPoint(Bufferer parent, MultiPoint mp, double distance,
				SpatialReference sr, double densify_dist,
				int max_vertex_in_complete_circle,
				ProgressTracker progress_tracker) {
			m_parent = parent;
			m_index = 0;
			m_mp = mp;
			m_x = 0;
			m_y = 0;
			m_distance = distance;
			m_spatialReference = sr;
			m_densify_dist = densify_dist;
			m_max_vertex_in_complete_circle = max_vertex_in_complete_circle;
			m_progress_tracker = progress_tracker;
		}

		@Override
		public Geometry next() {
			Point point = new Point();
			while (true) {
				if (m_index == m_mp.getPointCount())
					return null;

				m_mp.getPointByVal(m_index, point);
				m_index++;
				if (point.isEmpty())
					continue;
				break;
			}

			boolean b_first = false;
			if (m_buffered_polygon == null) {
				m_x = point.getX();
				m_y = point.getY();

				m_buffered_polygon = m_parent.buffer(point, m_distance,
						m_spatialReference, m_densify_dist,
						m_max_vertex_in_complete_circle, m_progress_tracker);
				b_first = true;
			}

			Geometry res;
			if (m_index < m_mp.getPointCount()) {
				res = new Polygon();
				m_buffered_polygon.copyTo(res);
			} else {
				res = m_buffered_polygon; // do not clone the last geometry.
			}

			if (!b_first)// don't apply transformation unnecessary
			{
				Transformation2D transform = new Transformation2D();
				double dx = point.getX() - m_x;
				double dy = point.getY() - m_y;
				transform.setShift(dx, dy);
				res.applyTransformation(transform);
			}

			return res;
		}

		@Override
		public int getGeometryID() {
			return 0;
		}
	}

	private static final class GlueingCursorForPolyline extends GeometryCursor {
		private Polyline m_polyline;
		private int m_current_path_index;

		GlueingCursorForPolyline(Polyline polyline) {
			m_polyline = polyline;
			m_current_path_index = 0;
		}

		@Override
		public Geometry next() {
			if (m_polyline == null)
				return null;

			MultiPathImpl mp = (MultiPathImpl) m_polyline._getImpl();
			int npaths = mp.getPathCount();
			if (m_current_path_index < npaths) {
				int ind = m_current_path_index;
				m_current_path_index++;
				if (!mp.isClosedPathInXYPlane(ind)) {
					// connect paths that follow one another as an optimization
					// for buffering (helps when one polyline is split into many
					// segments).
					Point2D prev_end = mp.getXY(mp.getPathEnd(ind) - 1);
					while (m_current_path_index < mp.getPathCount()) {
						Point2D start = mp.getXY(mp
								.getPathStart(m_current_path_index));
						if (mp.isClosedPathInXYPlane(m_current_path_index))
							break;
						if (start != prev_end)
							break;

						prev_end = mp
								.getXY(mp.getPathEnd(m_current_path_index) - 1);
						m_current_path_index++;
					}
				}

				if (ind == 0
						&& m_current_path_index == m_polyline.getPathCount()) {
					Polyline pol = m_polyline;
					m_polyline = null;
					return pol;
				}

				Polyline tmp_polyline = new Polyline(
						m_polyline.getDescription());
				tmp_polyline.addPath(m_polyline, ind, true);
				for (int i = ind + 1; i < m_current_path_index; i++) {
					tmp_polyline.addSegmentsFromPath(m_polyline, i, 0,
							mp.getSegmentCount(i), false);
				}

				if (false) {
					OperatorFactoryLocal.saveGeometryToEsriShapeDbg(
							"c:/temp/_geom.bin", tmp_polyline);
				}

				if (m_current_path_index == m_polyline.getPathCount())
					m_polyline = null;

				return tmp_polyline;
			} else {
				return null;
			}
		}

		@Override
		public int getGeometryID() {
			return 0;
		}
	}
	
	private static final class GeometryCursorForPolyline extends GeometryCursor {
		private Bufferer m_bufferer;
		GeometryCursor m_geoms;
		Geometry m_geometry;
		private int m_index;
		private boolean m_bfilter;

		GeometryCursorForPolyline(Bufferer bufferer, GeometryCursor geoms,
				boolean bfilter) {
			m_bufferer = bufferer;
			m_geoms = geoms;
			m_index = 0;
			m_bfilter = bfilter;
		}

		@Override
		public Geometry next() {
			if (m_geometry == null) {
				m_index = 0;
				m_geometry = m_geoms.next();
				if (m_geometry == null)
					return null;
			}

			MultiPath mp = (MultiPath) (m_geometry);
			if (m_index < mp.getPathCount()) {
				int ind = m_index;
				m_index++;
				return m_bufferer.bufferPolylinePath_((Polyline) m_geometry,
						ind, m_bfilter);
			}

			m_geometry = null;
			return next();
		}

		@Override
		public int getGeometryID() {
			return 0;
		}
	}

	private static final class GeometryCursorForPolygon extends GeometryCursor {
		private Bufferer m_bufferer;
		private int m_index;

		GeometryCursorForPolygon(Bufferer bufferer) {
			m_bufferer = bufferer;
			m_index = 0;
		}

		@Override
		public Geometry next() {
			Polygon input_polygon = (Polygon) (m_bufferer.m_geometry);
			if (m_index < input_polygon.getPathCount()) {
				int ind = m_index;
				double area = input_polygon.calculateRingArea2D(m_index);
				assert (area > 0);
				m_index++;
				while (m_index < input_polygon.getPathCount()) {
					double hole_area = input_polygon
							.calculateRingArea2D(m_index);
					if (hole_area > 0)
						break;// not a hole
					m_index++;
				}

				if (ind == 0 && m_index == input_polygon.getPathCount()) {
					return m_bufferer.bufferPolygonImpl_(input_polygon, 0,
							input_polygon.getPathCount());
				} else {
					return m_bufferer.bufferPolygonImpl_(input_polygon, ind,
							m_index);
				}
			}

			return null;
		}

		@Override
		public int getGeometryID() {
			return 0;
		}
	}

	private Geometry buffer_() {
		int gt = m_geometry.getType().value();
		if (Geometry.isSegment(gt)) {// convert segment to a polyline and repeat
										// the call
			Polyline polyline = new Polyline(m_geometry.getDescription());
			polyline.addSegment((Segment) (m_geometry), true);
			m_geometry = polyline;
			return buffer_();
		}

		if (m_distance <= m_tolerance) {
			if (Geometry.isArea(gt)) {
				if (m_distance <= 0) {
					// if the geometry is area type, then the negative distance
					// may produce a degenerate shape. Check for this and return
					// empty geometry.
					Envelope2D env = new Envelope2D();
					m_geometry.queryEnvelope2D(env);
					if (env.getWidth() <= -m_distance * 2
							|| env.getHeight() <= m_distance * 2)
						return new Polygon(m_geometry.getDescription());
				}
			} else {
				return new Polygon(m_geometry.getDescription());// return an
																// empty polygon
																// for distance
																// <=
																// m_tolerance
																// and any input
																// other than
																// polygon.
			}
		}

		// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/buffer_input.txt",
		// *m_geometry, nullptr);

		// Complex cases:
		switch (m_geometry.getType().value()) {
		case Geometry.GeometryType.Point:
			return bufferPoint_();
		case Geometry.GeometryType.MultiPoint:
			return bufferMultiPoint_();
		case Geometry.GeometryType.Polyline:
			return bufferPolyline_();
		case Geometry.GeometryType.Polygon:
			return bufferPolygon_();
		case Geometry.GeometryType.Envelope:
			return bufferEnvelope_();
		default:
			throw GeometryException.GeometryInternalError();
		}
	}

	private Geometry bufferPolyline_() {
		if (isDegenerateGeometry_(m_geometry)) {
			Point point = new Point();
			((MultiVertexGeometry) m_geometry).getPointByVal(0, point);
			Envelope2D env2D = new Envelope2D();
			m_geometry.queryEnvelope2D(env2D);
			point.setXY(env2D.getCenter());
			return bufferPoint_(point);
		}

		assert (m_distance > 0);
		Polyline poly = (Polyline)m_geometry; m_geometry = null;

		GeometryCursor glueing_cursor = new GlueingCursorForPolyline(poly);//glues paths together if they connect at one point
		poly = null;
		GeometryCursor generalized_paths = OperatorGeneralize.local().execute(glueing_cursor, m_densify_dist * 0.25, false, m_progress_tracker);
		GeometryCursor simple_paths = OperatorSimplifyOGC.local().execute(generalized_paths, null, true, m_progress_tracker);//make a planar graph.
		generalized_paths = null;
		GeometryCursor path_buffering_cursor = new GeometryCursorForPolyline(this, simple_paths, m_bfilter); simple_paths = null;
		GeometryCursor union_cursor = OperatorUnion.local().execute(path_buffering_cursor, m_spatialReference, m_progress_tracker);//(int)Operator_union::Options::enum_disable_edge_dissolver
		Geometry result = union_cursor.next();
		return result;
	}

	private Geometry bufferPolygon_() {
		if (m_distance == 0)
			return m_geometry;// return input to the output.

		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);

		generateCircleTemplate_();
		m_geometry = simplify.execute(m_geometry, null, false,
				m_progress_tracker);

		if (m_distance < 0) {
			Polygon poly = (Polygon) (m_geometry);
			Polygon buffered_result = bufferPolygonImpl_(poly, 0,
					poly.getPathCount());
			return simplify.execute(buffered_result, m_spatialReference, false,
					m_progress_tracker);
		} else {
			if (isDegenerateGeometry_(m_geometry)) {
				Point point = new Point();
				((MultiVertexGeometry) m_geometry).getPointByVal(0, point);
				Envelope2D env2D = new Envelope2D();
				m_geometry.queryEnvelope2D(env2D);
				point.setXY(env2D.getCenter());
				return bufferPoint_(point);
			}

			// For the positive distance we need to process polygon in the parts
			// such that each exterior ring with holes is processed separatelly.
			GeometryCursorForPolygon cursor = new GeometryCursorForPolygon(this);
			GeometryCursor union_cursor = ((OperatorUnion) OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Union)).execute(
					cursor, m_spatialReference, m_progress_tracker);
			Geometry result = union_cursor.next();
			return result;
		}
	}

	private Polygon bufferPolygonImpl_(Polygon input_geom, int ipath_begin,
			int ipath_end) {
		MultiPath input_mp = (MultiPath) (input_geom);
		MultiPathImpl mp_impl = (MultiPathImpl) (input_mp._getImpl());
		Polygon intermediate_polygon = new Polygon(input_geom.getDescription());
		for (int ipath = ipath_begin; ipath < ipath_end; ipath++) {
			if (mp_impl.getPathSize(ipath) < 1)
				continue;

			double path_area = mp_impl.calculateRingArea2D(ipath);
			Envelope2D env2D = new Envelope2D();
			mp_impl.queryPathEnvelope2D(ipath, env2D);

			if (m_distance > 0) {
				if (path_area > 0) {
					if (isDegeneratePath_(mp_impl, ipath)) {// if a path is
															// degenerate
															// (almost a point),
															// then we can draw
															// a circle instead
															// of it as a buffer
															// and nobody would
															// notice :)
						Point point = new Point();
						mp_impl.getPointByVal(mp_impl.getPathStart(ipath),
								point);
						point.setXY(env2D.getCenter());
						addCircle_(
								(MultiPathImpl) intermediate_polygon._getImpl(),
								point);
					} else {
						Polyline result_polyline = new Polyline(
								input_geom.getDescription());
						MultiPathImpl result_mp = (MultiPathImpl) result_polyline
								._getImpl();

						// We often see convex hulls, buffering those is an
						// extremely simple task.
						boolean bConvex = ConvexHull.isPathConvex(
								(Polygon) (m_geometry), ipath,
								m_progress_tracker);
						if (bConvex
								|| bufferClosedPath_(m_geometry, ipath,
										result_mp, true, 1) == 2) {
							Polygon buffered_path = bufferConvexPath_(input_mp,
									ipath);
							intermediate_polygon.add(buffered_path, false);
						} else {
							Polygon buffered_path = bufferCleanup_(
									result_polyline, false);
							intermediate_polygon.add(buffered_path, false);
						}
					}
				} else {
					if (env2D.getWidth() + m_tolerance <= 2 * m_abs_distance
							|| env2D.getHeight() + m_tolerance <= 2 * m_abs_distance) // skip
																						// parts
																						// that
																						// will
																						// dissapear
						continue;

					Polyline result_polyline = new Polyline(
							input_geom.getDescription());
					MultiPathImpl result_mp = (MultiPathImpl) result_polyline
							._getImpl();
					bufferClosedPath_(m_geometry, ipath, result_mp, true, 1);
					if (!result_polyline.isEmpty()) {
						Envelope2D env = new Envelope2D();
						env.setCoords(env2D);
						env.inflate(m_abs_distance, m_abs_distance);
						result_mp.addEnvelope(env, false);
						Polygon buffered_path = bufferCleanup_(result_polyline,
								false);
						// intermediate_polygon.reserve(intermediate_polygon.getPointCount()
						// + buffered_path.getPointCount() - 4);
						for (int i = 1, n = buffered_path.getPathCount(); i < n; i++)
							intermediate_polygon
									.addPath(buffered_path, i, true);
					}
				}
			} else {
				if (path_area > 0) {
					if (env2D.getWidth() + m_tolerance <= 2 * m_abs_distance
							|| env2D.getHeight() + m_tolerance <= 2 * m_abs_distance) // skip
																						// parts
																						// that
																						// will
																						// dissapear
						continue;

					Polyline result_polyline = new Polyline(
							input_geom.getDescription());
					MultiPathImpl result_mp = (MultiPathImpl) result_polyline
							._getImpl();
					bufferClosedPath_(m_geometry, ipath, result_mp, true, -1);// this
																				// will
																				// provide
																				// a
																				// shape
																				// buffered
																				// inwards.
																				// It
																				// has
																				// counterclockwise
																				// orientation
					if (!result_polyline.isEmpty()) {
						Envelope2D env = new Envelope2D();
						result_mp.queryLooseEnvelope2D(env);
						env.inflate(m_abs_distance, m_abs_distance);
						result_mp.addEnvelope(env, false);// add an envelope
															// exterior shell
						Polygon buffered_path = bufferCleanup_(result_polyline,
								false);// simplify with winding rule
						// extract all parts but the first one (which is the
						// envelope we added previously)
						for (int i = 1, npaths = buffered_path.getPathCount(); i < npaths; i++) {
							// the extracted parts have inverted orientation.
							intermediate_polygon
									.addPath(buffered_path, i, true);
						}
					} else {
						// the path has been erased
					}
				} else {
					// When buffering a hole with negative distance, buffer it
					// as if it is an exterior ring buffered with positive
					// distance
					Polyline result_polyline = new Polyline(
							input_geom.getDescription());
					MultiPathImpl result_mp = (MultiPathImpl) result_polyline
							._getImpl();
					bufferClosedPath_(m_geometry, ipath, result_mp, true, -1);// this
																				// will
																				// provide
																				// a
																				// shape
																				// buffered
																				// inwards.
					Polygon buffered_path = bufferCleanup_(result_polyline,
							false);
					for (int i = 0, npaths = buffered_path.getPathCount(); i < npaths; i++) {
						intermediate_polygon.addPath(buffered_path, i, true);// adds
																				// buffered
																				// hole
																				// reversed
																				// as
																				// if
																				// it
																				// is
																				// exteror
																				// ring
					}
				}

				// intermediate_polygon has inverted orientation.
			}
		}

		if (m_distance > 0) {
			if (intermediate_polygon.getPathCount() > 1) {
				Polygon cleaned_polygon = bufferCleanup_(intermediate_polygon,
						false);
				return cleaned_polygon;
			} else {
				return setWeakSimple_(intermediate_polygon);
			}
		} else {
			Envelope2D polyenv = new Envelope2D();
			intermediate_polygon.queryLooseEnvelope2D(polyenv);
			if (!intermediate_polygon.isEmpty()) {
				// negative buffer distance. We got buffered holes and exterior
				// rings. They all have wrong orientation.
				// we need to apply winding simplify again to ensure all holes
				// are unioned.
				// For that create a big envelope and add all rings of the
				// intermediate_polygon to it.
				polyenv.inflate(m_abs_distance, m_abs_distance);
				intermediate_polygon.addEnvelope(polyenv, false);
				Polygon cleaned_polygon = bufferCleanup_(intermediate_polygon,
						false);
				// intermediate_polygon.reset();//free memory

				Polygon result_polygon = new Polygon(
						cleaned_polygon.getDescription());
				for (int i = 1, n = cleaned_polygon.getPathCount(); i < n; i++) {
					result_polygon.addPath(cleaned_polygon, i, false);
				}
				return setWeakSimple_(result_polygon);
			} else {
				return setWeakSimple_(intermediate_polygon);
			}
		}
	}

	private Geometry bufferPoint_() {
		return bufferPoint_((Point) (m_geometry));
	}

	private Geometry bufferPoint_(Point point) {
		assert (m_distance > 0);
		Polygon resultPolygon = new Polygon(point.getDescription());
		addCircle_((MultiPathImpl) resultPolygon._getImpl(), point);
		return setStrongSimple_(resultPolygon);
	}

	private Geometry bufferMultiPoint_() {
		assert (m_distance > 0);
		GeometryCursorForMultiPoint mpCursor = new GeometryCursorForMultiPoint(this,
				(MultiPoint) (m_geometry), m_distance, m_spatialReference,
				m_densify_dist, m_max_vertex_in_complete_circle,
				m_progress_tracker);
		GeometryCursor c = ((OperatorUnion) OperatorFactoryLocal.getInstance()
				.getOperator(Operator.Type.Union)).execute(mpCursor,
				m_spatialReference, m_progress_tracker);
		return c.next();
	}

	private Geometry bufferEnvelope_() {
		Polygon polygon = new Polygon(m_geometry.getDescription());
		if (m_distance <= 0) {
			if (m_distance == 0)
				polygon.addEnvelope((Envelope) (m_geometry), false);
			else {
				Envelope env = new Envelope();
				m_geometry.queryEnvelope(env);
				env.inflate(m_distance, m_distance);
				polygon.addEnvelope(env, false);
			}

			return polygon;// nothing is easier than negative buffer on the
							// envelope.
		}

		polygon.addEnvelope((Envelope) (m_geometry), false);
		m_geometry = polygon;
		return bufferConvexPath_(polygon, 0);
	}

	private Polygon bufferConvexPath_(MultiPath src, int ipath) {
		generateCircleTemplate_();

		Polygon resultPolygon = new Polygon(src.getDescription());
		MultiPathImpl result_mp = (MultiPathImpl) resultPolygon._getImpl();

		// resultPolygon.reserve((m_circle_template.size() / 10 + 4) *
		// src.getPathSize(ipath));

		Point2D pt_1_tmp = new Point2D(), pt_1 = new Point2D();
		Point2D pt_2_tmp = new Point2D(), pt_2 = new Point2D();
		Point2D pt_3_tmp = new Point2D(), pt_3 = new Point2D();
		Point2D v_1 = new Point2D();
		Point2D v_2 = new Point2D();
		MultiPathImpl src_mp = (MultiPathImpl) src._getImpl();
		int path_size = src.getPathSize(ipath);
		int path_start = src.getPathStart(ipath);
		for (int i = 0, n = src.getPathSize(ipath); i < n; i++) {
			src_mp.getXY(path_start + i, pt_1);
			src_mp.getXY(path_start + (i + 1) % path_size, pt_2);
			src_mp.getXY(path_start + (i + 2) % path_size, pt_3);
			v_1.sub(pt_2, pt_1);
			if (v_1.length() == 0)
				throw GeometryException.GeometryInternalError();

			v_1.leftPerpendicular();
			v_1.normalize();
			v_1.scale(m_abs_distance);
			pt_1_tmp.add(v_1, pt_1);
			pt_2_tmp.add(v_1, pt_2);
			if (i == 0)
				result_mp.startPath(pt_1_tmp);
			else {
				result_mp.lineTo(pt_1_tmp);
			}

			result_mp.lineTo(pt_2_tmp);

			v_2.sub(pt_3, pt_2);
			if (v_2.length() == 0)
				throw GeometryException.GeometryInternalError();

			v_2.leftPerpendicular();
			v_2.normalize();
			v_2.scale(m_abs_distance);
			pt_3_tmp.add(v_2, pt_2);

			addJoin_(result_mp, pt_2, pt_2_tmp, pt_3_tmp, false, false);
		}

		return setWeakSimple_(resultPolygon);
	}

	private Polygon bufferPolylinePath_(Polyline polyline, int ipath,
			boolean bfilter) {
		assert (m_distance != 0);
		generateCircleTemplate_();

		MultiPath input_multi_path = polyline;
		MultiPathImpl mp_impl = (MultiPathImpl) (input_multi_path._getImpl());

		if (mp_impl.getPathSize(ipath) < 1)
			return null;

		if (isDegeneratePath_(mp_impl, ipath) && m_distance > 0) {// if a path
																	// is
																	// degenerate
																	// (almost a
																	// point),
																	// then we
																	// can draw
																	// a circle
																	// instead
																	// of it as
																	// a buffer
																	// and
																	// nobody
																	// would
																	// notice :)
			Point point = new Point();
			mp_impl.getPointByVal(mp_impl.getPathStart(ipath), point);
			Envelope2D env2D = new Envelope2D();
			mp_impl.queryPathEnvelope2D(ipath, env2D);
			point.setXY(env2D.getCenter());
			return (Polygon) (bufferPoint_(point));
		}

		Polyline result_polyline = new Polyline(polyline.getDescription());

		MultiPathImpl result_mp = (MultiPathImpl) result_polyline._getImpl();
		boolean b_closed = mp_impl.isClosedPathInXYPlane(ipath);

		if (b_closed) {
			bufferClosedPath_(input_multi_path, ipath, result_mp, bfilter, 1);
			bufferClosedPath_(input_multi_path, ipath, result_mp, bfilter, -1);
		} else {
			Polyline tmpPoly = new Polyline(input_multi_path.getDescription());
			tmpPoly.addPath(input_multi_path, ipath, false);
			((MultiPathImpl) tmpPoly._getImpl()).addSegmentsFromPath(
					(MultiPathImpl) input_multi_path._getImpl(), ipath, 0,
					input_multi_path.getSegmentCount(ipath), false);
			bufferClosedPath_(tmpPoly, 0, result_mp, bfilter, 1);
		}

		return bufferCleanup_(result_polyline, false);
	}

	private void progress_() {
		m_progress_counter++;
		if (m_progress_counter % 1024 == 0) {
			if ((m_progress_tracker != null)
					&& !(m_progress_tracker.progress(-1, -1)))
				throw new RuntimeException("user_canceled");
		}
	}

	private Polygon bufferCleanup_(MultiPath multi_path, boolean simplify_result) {
		double tol = simplify_result ? m_tolerance : m_small_tolerance;
		Polygon resultPolygon = (Polygon) (TopologicalOperations
				.planarSimplify(multi_path, tol, true, !simplify_result,
						m_progress_tracker));
		assert (InternalUtils.isWeakSimple(resultPolygon, 0.0));
		return resultPolygon;
	}

	private int calcN_() {
		//this method should be called only once m_circle_template_size is set then;
		final int minN = 4;
		if (m_densify_dist == 0)
			return m_max_vertex_in_complete_circle;

		double r = m_densify_dist * Math.abs(m_abs_distance_reversed);
		double cos_a = 1 - r;
		double N;
		if (cos_a < -1)
			N = minN;
		else
			N = 2.0 * Math.PI / Math.acos(cos_a) + 0.5;

		if (N < minN)
			N = minN;
		else if (N > m_max_vertex_in_complete_circle)
			N = m_max_vertex_in_complete_circle;

		return (int) N;
	}

	private void addJoin_(MultiPathImpl dst, Point2D center, Point2D fromPt,
			Point2D toPt, boolean bStartPath, boolean bFinishAtToPt) {
		generateCircleTemplate_();

		Point2D v_1 = new Point2D();
		v_1.sub(fromPt, center);
		v_1.scale(m_abs_distance_reversed);
		Point2D v_2 = new Point2D();
		v_2.sub(toPt, center);
		v_2.scale(m_abs_distance_reversed);
		double angle_from = Math.atan2(v_1.y, v_1.x);
		double dindex_from = angle_from / m_dA;
		if (dindex_from < 0)
			dindex_from = (double) m_circle_template.size() + dindex_from;

		dindex_from = (double) m_circle_template.size() - dindex_from;

		double angle_to = Math.atan2(v_2.y, v_2.x);
		double dindex_to = angle_to / m_dA;
		if (dindex_to < 0)
			dindex_to = (double) m_circle_template.size() + dindex_to;

		dindex_to = (double) m_circle_template.size() - dindex_to;

		if (dindex_to < dindex_from)
			dindex_to += (double) m_circle_template.size();
		assert (dindex_to >= dindex_from);

		int index_to = (int) dindex_to;
		int index_from = (int) Math.ceil(dindex_from);

		if (bStartPath) {
			dst.startPath(fromPt);
			bStartPath = false;
		}

		Point2D p = new Point2D();
		p.setCoords(m_circle_template.get(index_from % m_circle_template.size()));
		p.scaleAdd(m_abs_distance, center);
		double ddd = m_tolerance * 10;
		p.sub(fromPt);
		if (p.length() < ddd)// if too close to the fromPt, then use the next
								// point
			index_from += 1;

		p.setCoords(m_circle_template.get(index_to % m_circle_template.size()));
		p.scaleAdd(m_abs_distance, center);
		p.sub(toPt);
		if (p.length() < ddd)// if too close to the toPt, then use the prev
								// point
			index_to -= 1;

		int count = index_to - index_from;
		count++;

		for (int i = 0, j = index_from % m_circle_template.size(); i < count; i++, j = (j + 1)
				% m_circle_template.size()) {
			p.setCoords(m_circle_template.get(j));
			p.scaleAdd(m_abs_distance, center);
			dst.lineTo(p);
			progress_();
		}

		if (bFinishAtToPt) {
			dst.lineTo(toPt);
		}
	}

	private int bufferClosedPath_(Geometry input_geom, int ipath,
			MultiPathImpl result_mp, boolean bfilter, int dir) {
		// Use temporary polyline for the path buffering.
		EditShape edit_shape = new EditShape();
		int geom = edit_shape.addPathFromMultiPath((MultiPath) input_geom,
				ipath, true);
		edit_shape.filterClosePoints(m_filter_tolerance, false, false);
		if (edit_shape.getPointCount(geom) < 2) {// Got degenerate output.
													// Either bail out or
													// produce a circle.
			if (dir < 0)
				return 1;// negative direction produces nothing.

			MultiPath mpIn = (MultiPath) input_geom;
			// Add a circle
			Point pt = new Point();
			mpIn.getPointByVal(mpIn.getPathStart(ipath), pt);
			addCircle_(result_mp, pt);
			return 1;
		}

		assert (edit_shape.getFirstPath(geom) != -1);
		assert (edit_shape.getFirstVertex(edit_shape.getFirstPath(geom)) != -1);

		Point2D origin = edit_shape.getXY(edit_shape.getFirstVertex(edit_shape
				.getFirstPath(geom)));
		Transformation2D tr = new Transformation2D();
		tr.setShift(-origin.x, -origin.y);
		// move the path to origin for better accuracy in calculations.
		edit_shape.applyTransformation(tr);

		if (bfilter) {
			// try removing the noise that does not contribute to the buffer.
			int res_filter = filterPath_(edit_shape, geom, dir, true, m_abs_distance, m_filter_tolerance, m_densify_dist);
			assert (res_filter == 1);
			// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/buffer_filter.txt",
			// *edit_shape.get_geometry(geom), nullptr);
			if (edit_shape.getPointCount(geom) < 2) {// got degenerate output.
														// Wither bail out or
														// produce a circle.
				if (dir < 0)
					return 1;// negative direction produces nothing.

				MultiPath mpIn = (MultiPath) input_geom;
				// Add a circle
				Point pt = new Point();
				mpIn.getPointByVal(mpIn.getPathStart(ipath), pt);
				addCircle_(result_mp, pt);
				return 1;
			}
		}

		m_buffer_commands.clear();
		int path = edit_shape.getFirstPath(geom);
		int ivert = edit_shape.getFirstVertex(path);
		int iprev = dir == 1 ? edit_shape.getPrevVertex(ivert) : edit_shape
				.getNextVertex(ivert);
		int inext = dir == 1 ? edit_shape.getNextVertex(ivert) : edit_shape
				.getPrevVertex(ivert);
		boolean b_first = true;
		Point2D pt_current = new Point2D(), pt_after = new Point2D(), pt_before = new Point2D(), pt_left_prev = new Point2D(), pt = new Point2D(), pt1 = new Point2D();
		Point2D v_after = new Point2D(), v_before = new Point2D(), v_left = new Point2D(), v_left_prev = new Point2D();
		double abs_d = m_abs_distance;
		int ncount = edit_shape.getPathSize(path);

		// write out buffer commands as a set of arcs and line segments.
		// if we'd convert this directly to a polygon and draw using winding
		// fill rule, we'd get the buffered result.
		for (int index = 0; index < ncount; index++) {
			edit_shape.getXY(inext, pt_after);

			if (b_first) {
				edit_shape.getXY(ivert, pt_current);
				edit_shape.getXY(iprev, pt_before);
				v_before.sub(pt_current, pt_before);
				v_before.normalize();
				v_left_prev.leftPerpendicular(v_before);
				v_left_prev.scale(abs_d);
				pt_left_prev.add(v_left_prev, pt_current);
			}

			v_after.sub(pt_after, pt_current);
			v_after.normalize();

			v_left.leftPerpendicular(v_after);
			v_left.scale(abs_d);
			pt.add(pt_current, v_left);
			double cross = v_before.crossProduct(v_after);
			double dot = v_before.dotProduct(v_after);
			boolean bDoJoin = cross < 0 || (dot < 0 && cross == 0);
			if (bDoJoin) {
				m_buffer_commands.add(new BufferCommand(pt_left_prev, pt,
						pt_current, BufferCommand.Flags.enum_arc,
						m_buffer_commands.size() + 1,
						m_buffer_commands.size() - 1));
			} else if (!pt_left_prev.isEqual(pt)) {
				m_buffer_commands.add(new BufferCommand(pt_left_prev,
						pt_current, m_buffer_commands.size() + 1,
						m_buffer_commands.size() - 1, "dummy"));
				m_buffer_commands.add(new BufferCommand(pt_current, pt,
						m_buffer_commands.size() + 1,
						m_buffer_commands.size() - 1, "dummy"));
			}

			pt1.add(pt_after, v_left);
			m_buffer_commands
					.add(new BufferCommand(pt, pt1, pt_current,
							BufferCommand.Flags.enum_line, m_buffer_commands
									.size() + 1, m_buffer_commands.size() - 1));

			pt_left_prev.setCoords(pt1);
			v_left_prev.setCoords(v_left);
			pt_before.setCoords(pt_current);
			pt_current.setCoords(pt_after);
			v_before.setCoords(v_after);
			iprev = ivert;
			ivert = inext;
			b_first = false;
			inext = dir == 1 ? edit_shape.getNextVertex(ivert) : edit_shape
					.getPrevVertex(ivert);
		}

		m_buffer_commands.get(m_buffer_commands.size() - 1).m_next = 0;
		m_buffer_commands.get(0).m_prev = m_buffer_commands.size() - 1;
		processBufferCommands_(result_mp);
		tr.setShift(origin.x, origin.y);// move the path to improve precision.
		result_mp.applyTransformation(tr, result_mp.getPathCount() - 1);
		return 1;
	}

	private void processBufferCommands_(MultiPathImpl result_mp) {
		int ifirst_seg = cleanupBufferCommands_();
		boolean first = true;
		int iseg_next = ifirst_seg + 1;
		for (int iseg = ifirst_seg; iseg_next != ifirst_seg; iseg = iseg_next) {
			BufferCommand command = m_buffer_commands.get(iseg);
			iseg_next = command.m_next != -1 ? command.m_next : (iseg + 1)
					% m_buffer_commands.size();
			if (command.m_type == 0)
				continue;// deleted segment

			if (first) {
				result_mp.startPath(command.m_from);
				first = false;
			}

			if (command.m_type == BufferCommand.Flags.enum_arc) {// arc
				addJoin_(result_mp, command.m_center, command.m_from,
						command.m_to, false, true);
			} else {
				result_mp.lineTo(command.m_to);
			}
			first = false;
		}
	}

	private int cleanupBufferCommands_() {
		// The purpose of this function is to remove as many self intersections
		// from the buffered shape as possible.
		// The buffer works without cleanup also, but slower.

		if (m_helper_array == null)
			m_helper_array = new Point2D[9];

		int istart = 0;
		for (int iseg = 0, nseg = m_buffer_commands.size(); iseg < nseg;) {
			BufferCommand command = m_buffer_commands.get(iseg);
			if ((command.m_type & BufferCommand.Flags.enum_connection) != 0) {
				istart = iseg;
				break;
			}

			iseg = command.m_next;
		}

		int iseg_next = istart + 1;
		for (int iseg = istart; iseg_next != istart; iseg = iseg_next) {
			BufferCommand command = m_buffer_commands.get(iseg);
			iseg_next = command.m_next;
			int count = 1;
			BufferCommand command_next = null;
			while (iseg_next != iseg) {// find next segement
				command_next = m_buffer_commands.get(iseg_next);
				if ((command_next.m_type & BufferCommand.Flags.enum_connection) != 0)
					break;

				iseg_next = command_next.m_next;
				count++;
			}

			if (count == 1) {
				// Next segment starts where this one ends. Skip this case as it
				// is simple.
				assert (command.m_to.isEqual(command_next.m_from));
				continue;
			}

			if ((command.m_type & command_next.m_type) == BufferCommand.Flags.enum_line) {// simplest
																							// cleanup
																							// -
																							// intersect
																							// lines
				if (m_helper_line_1 == null) {
					m_helper_line_1 = new Line();
					m_helper_line_2 = new Line();
				}
				m_helper_line_1.setStartXY(command.m_from);
				m_helper_line_1.setEndXY(command.m_to);
				m_helper_line_2.setStartXY(command_next.m_from);
				m_helper_line_2.setEndXY(command_next.m_to);

				int count_ = m_helper_line_1.intersect(m_helper_line_2,
						m_helper_array, null, null, m_small_tolerance);
				if (count_ == 1) {
					command.m_to.setCoords(m_helper_array[0]);
					command_next.m_from.setCoords(m_helper_array[0]);
					command.m_next = iseg_next;// skip until iseg_next
					command_next.m_prev = iseg;
				} else if (count_ == 2) {// TODO: this case needs improvement
				}
			}
		}

		return istart;
	}

	private static void protectExtremeVertices_(EditShape edit_shape,
			int protection_index, int geom, int path) {
		// detect very narrow corners and preserve them. We cannot reliably
		// delete these.
		int vprev = -1;
		Point2D pt_prev = new Point2D();
		pt_prev.setNaN();
		Point2D pt = new Point2D();
		pt.setNaN();
		Point2D v_before = new Point2D();
		v_before.setNaN();
		Point2D pt_next = new Point2D();
		Point2D v_after = new Point2D();
		for (int i = 0, n = edit_shape.getPathSize(path), v = edit_shape
				.getFirstVertex(path); i < n; ++i) {
			if (vprev == -1) {
				edit_shape.getXY(v, pt);

				vprev = edit_shape.getPrevVertex(v);
				if (vprev != -1) {
					edit_shape.getXY(vprev, pt_prev);
					v_before.sub(pt, pt_prev);
					v_before.normalize();
				}
			}

			int vnext = edit_shape.getNextVertex(v);
			if (vnext == -1)
				break;

			edit_shape.getXY(vnext, pt_next);
			v_after.sub(pt_next, pt);
			v_after.normalize();

			if (vprev != -1) {
				double d = v_after.dotProduct(v_before);
				if (d < -0.99
						&& Math.abs(v_after.crossProduct(v_before)) < 1e-7) {
					edit_shape.setUserIndex(v, protection_index, 1);
				}
			}

			vprev = v;
			v = vnext;
			pt_prev.setCoords(pt);
			pt.setCoords(pt_next);
			v_before.setCoords(v_after);
		}
	}
	
	static private int filterPath_(EditShape edit_shape, int geom, int dir,
			boolean closed, double abs_distance, double filter_tolerance,
			double densify_distance) {
		int path = edit_shape.getFirstPath(geom);

		int concave_index = -1;
		int fixed_vertices_index = edit_shape.createUserIndex();
		protectExtremeVertices_(edit_shape, fixed_vertices_index, geom, path);

		for (int iter = 0; iter < 100; ++iter) {
			int isize = edit_shape.getPathSize(path);
			if (isize == 0) {
				edit_shape.removeUserIndex(fixed_vertices_index);
				;
				return 1;
			}

			int ivert = edit_shape.getFirstVertex(path);
			int nvertices = edit_shape.getPathSize(path);
			if (nvertices < 3) {
				edit_shape.removeUserIndex(fixed_vertices_index);
				;
				return 1;
			}

			if (closed && !edit_shape.isClosedPath(path))// the path is closed
															// only virtually
			{
				nvertices -= 1;
			}

			double abs_d = abs_distance;
			final int nfilter = 64;
			boolean filtered = false;
			int filtered_in_pass = 0;
			boolean go_back = false;
			for (int i = 0; i < nvertices && ivert != -1; i++) {
				int filtered_now = 0;
				int v = ivert; // filter == 0
				for (int filter = 1, n = (int) Math.min(nfilter, nvertices - i); filter < n; filter++) {
					v = edit_shape.getNextVertex(v, dir);
					if (filter > 1) {
						int num = clipFilter_(edit_shape,
								fixed_vertices_index, ivert, v, dir,
								abs_distance, densify_distance, nfilter);
						if (num == -1)
							break;

						filtered |= num > 0;
						filtered_now += num;
						nvertices -= num;
					}
				}

				filtered_in_pass += filtered_now;

				go_back = filtered_now > 0;

				if (go_back) {
					int prev = edit_shape.getPrevVertex(ivert, dir);
					if (prev != -1) {
						ivert = prev;
						nvertices++;
						continue;
					}
				}

				ivert = edit_shape.getNextVertex(ivert, dir);
			}

			if (filtered_in_pass == 0)
				break;
		}

		edit_shape.removeUserIndex(fixed_vertices_index);
		edit_shape.filterClosePoints(filter_tolerance, false, false);

		return 1;
	}

	// This function clips out segments connecting from_vertiex to to_vertiex if
	// they do not contribute to the buffer.
	private static int clipFilter_(EditShape edit_shape,
			int fixed_vertices_index, int from_vertex, int to_vertex, int dir,
			double abs_distance, double densify_distance, final int max_filter) {
		// Note: vertices marked with fixed_vertices_index cannot be deleted.

		Point2D pt1 = edit_shape.getXY(from_vertex);
		Point2D pt2 = edit_shape.getXY(to_vertex);
		if (pt1.equals(pt2))
			return -1;

		double densify_distance_delta = densify_distance * 0.25;// distance by
																// which we can
																// move the
																// point closer
																// to the chord
																// (introducing
																// an error into
																// the buffer).
		double erase_distance_delta = densify_distance * 0.25;// distance when
																// we can erase
																// the point
																// (introducing
																// an error into
																// the buffer).
		// This function goal is to modify or remove vertices between
		// from_vertex and to_vertex in such a way that the result would not
		// affect buffer to the left of the
		// chain.
		Point2D v_gap = new Point2D();
		v_gap.sub(pt2, pt1);
		double gap_length = v_gap.length();
		double h2_4 = gap_length * gap_length * 0.25;
		double sqr_center_to_chord = abs_distance * abs_distance - h2_4; // squared
																			// distance
																			// from
																			// the
																			// chord
																			// to
																			// the
																			// circle
																			// center
		if (sqr_center_to_chord <= h2_4)
			return -1;// center to chord distance is less than half gap, that
						// means the gap is too wide for useful filtering (maybe
						// this).

		double center_to_chord = Math.sqrt(sqr_center_to_chord); // distance
																	// from
																	// circle
																	// center to
																	// the
																	// chord.

		v_gap.normalize();
		Point2D v_gap_norm = new Point2D(v_gap);
		v_gap_norm.rightPerpendicular();
		double chord_to_corner = h2_4 / center_to_chord; // cos(a) =
															// center_to_chord /
															// distance;
															// chord_to_corner =
															// distance / cos(a)
															// -
															// center_to_chord;
		boolean can_erase_corner_point = chord_to_corner <= erase_distance_delta;
		Point2D chord_midpoint = new Point2D();
		MathUtils.lerp(pt2, pt1, 0.5, chord_midpoint);
		Point2D corner = new Point2D(v_gap_norm);
		double corrected_chord_to_corner = chord_to_corner
				- densify_distance_delta;// using slightly smaller than needed
											// distance let us filter more.
		corner.scaleAdd(Math.max(0.0, corrected_chord_to_corner),
				chord_midpoint);
		// corner = (p1 + p2) * 0.5 + v_gap_norm * chord_to_corner;

		Point2D center = new Point2D(v_gap_norm);
		center.negate();
		center.scaleAdd(center_to_chord, chord_midpoint);

		double allowed_distance = abs_distance - erase_distance_delta;
		double sqr_allowed_distance = MathUtils.sqr(allowed_distance);
		double sqr_large_distance = sqr_allowed_distance * (1.9 * 1.9);

		Point2D co_p1 = new Point2D();
		co_p1.sub(corner, pt1);
		Point2D co_p2 = new Point2D();
		co_p2.sub(corner, pt2);

		boolean large_distance = false;// set to true when distance
		int cnt = 0;
		char[] locations = new char[64];
		{
			// check all vertices in the gap verifying that the gap can be
			// clipped.
			//

			Point2D pt = new Point2D();
			// firstly remove any duplicate vertices in the end.
			for (int v = edit_shape.getPrevVertex(to_vertex, dir); v != from_vertex;) {
				if (edit_shape.getUserIndex(v, fixed_vertices_index) == 1)
					return -1;// this range contains protected vertex

				edit_shape.getXY(v, pt);
				if (pt.equals(pt2)) {
					int v1 = edit_shape.getPrevVertex(v, dir);
					edit_shape.removeVertex(v, false);
					v = v1;
					continue;
				} else {
					break;
				}
			}

			Point2D prev_prev_pt = new Point2D();
			prev_prev_pt.setNaN();
			Point2D prev_pt = new Point2D();
			prev_pt.setCoords(pt1);
			locations[cnt++] = 1;
			int prev_v = from_vertex;
			Point2D dummyPt = new Point2D();
			for (int v = edit_shape.getNextVertex(from_vertex, dir); v != to_vertex;) {
				if (edit_shape.getUserIndex(v, fixed_vertices_index) == 1)
					return -1;// this range contains protected vertex

				edit_shape.getXY(v, pt);
				if (pt.equals(prev_pt)) {
					int v1 = edit_shape.getNextVertex(v, dir);
					edit_shape.removeVertex(v, false);
					v = v1;
					continue;
				}

				locations[cnt++] = 0;

				Point2D v1 = new Point2D();
				v1.sub(pt, pt1);
				if (v1.dotProduct(v_gap_norm) < 0)// we are crossing on the
													// wrong site of the chord.
													// Just bail out earlier.
													// Maybe we could continue
													// clipping though here, but
													// it seems to be
													// unnecessary complicated.
					return 0;

				if (Point2D.sqrDistance(pt, pt1) > sqr_large_distance
						|| Point2D.sqrDistance(pt, pt2) > sqr_large_distance)
					large_distance = true; // too far from points, may
											// contribute to the outline (in
											// case of a large loop)

				char next_location = 0;

				dummyPt.sub(pt, pt1);
				double cs1 = dummyPt.crossProduct(co_p1);
				if (cs1 >= 0) {
					next_location = 1;
				}

				dummyPt.sub(pt, pt2);
				double cs2 = dummyPt.crossProduct(co_p2);
				if (cs2 <= 0) {
					next_location |= 2;
				}

				if (next_location == 0)
					return 0;

				locations[cnt - 1] = next_location;
				prev_prev_pt.setCoords(prev_pt);
				prev_pt.setCoords(pt);
				prev_v = v;
				v = edit_shape.getNextVertex(v, dir);
			}

			if (cnt == 1)
				return 0;

			assert (!pt2.equals(prev_pt));
			locations[cnt++] = 2;
		}

		boolean can_clip_all = true;
		// we can remove all points and replace them with a single corner point
		// if we are moving from location 1 via location 3 to location 2
		for (int i = 1, k = 0; i < cnt; i++) {
			if (locations[i] != locations[i - 1]) {
				k++;
				can_clip_all = k < 3
						&& ((k == 1 && locations[i] == 3) || (k == 2 && locations[i] == 2));
				if (!can_clip_all)
					return 0;
			}
		}

		if (cnt > 2 && can_clip_all && (cnt == 3 || !large_distance)) {
			int clip_count = 0;
			int v = edit_shape.getNextVertex(from_vertex, dir);
			if (!can_erase_corner_point) {
				edit_shape.setXY(v, corner);
				v = edit_shape.getNextVertex(v, dir);
			}

			// we can remove all vertices between from and to, because they
			// don't contribute
			while (v != to_vertex) {
				int v1 = edit_shape.getNextVertex(v, dir);
				edit_shape.removeVertex(v, false);
				v = v1;
				++clip_count;
			}

			return clip_count;
		}

		if (cnt == 3) {
			boolean case1 = (locations[0] == 1 && locations[1] == 2 && locations[2] == 2);
			boolean case2 = (locations[0] == 1 && locations[1] == 1 && locations[2] == 2);
			if (case1 || case2) {
				// special case, when we cannot clip, but we can move the point
				Point2D p1 = edit_shape.getXY(from_vertex);
				int v = edit_shape.getNextVertex(from_vertex, dir);
				Point2D p2 = edit_shape.getXY(v);
				Point2D p3 = edit_shape.getXY(edit_shape.getNextVertex(v, dir));
				if (case2) {
					Point2D temp = p1;
					p1 = p3;
					p3 = temp;
				}

				Point2D vec = new Point2D();
				vec.sub(p1, p2);
				p3.sub(p2);
				double veclen = vec.length();
				double w = p3.length();
				double wcosa = vec.dotProduct(p3) / veclen;
				double wsina = Math.abs(p3.crossProduct(vec) / veclen);
				double z = 2 * abs_distance - wsina;
				if (z < 0)
					return 0;

				double x = wcosa + Math.sqrt(wsina * z);
				if (x > veclen)
					return 0;

				Point2D hvec = new Point2D();
				hvec.scaleAdd(-x / veclen, vec, p3); // hvec = p3 - vec * (x /
														// veclen);
				double h = hvec.length();
				double y = -(h * h * veclen) / (2 * hvec.dotProduct(vec));

				double t = (x - y) / veclen;
				MathUtils.lerp(p2, p1, t, p2);
				edit_shape.setXY(v, p2);
				return 0;
			}
		}

		if (large_distance && cnt > 3) {
			// we are processing more than 3 points and there are some points
			// further than the
			return 0;
		}

		int v_prev = -1;
		Point2D pt_prev = new Point2D();
		int v_cur = from_vertex;
		Point2D pt_cur = new Point2D(pt1);
		int cur_location = 1;
		int prev_location = -1; // 1 - semiplane to the right of [f,c]. 3 -
								// semiplane to the right of [c,t], 2 - both
								// above fc and ct, 0 - cannot clip, -1 -
								// unknown
		int v_next = v_cur;
		int clip_count = 0;
		cnt = 1;
		while (v_next != to_vertex) {
			v_next = edit_shape.getNextVertex(v_next, dir);
			int next_location = locations[cnt++];
			if (next_location == 0) {
				if (v_next == to_vertex)
					break;

				continue;
			}

			Point2D pt_next = edit_shape.getXY(v_next);

			if (prev_location != -1) {
				int common_location = (prev_location & cur_location & next_location);
				if ((common_location & 3) != 0) {
					// prev and next are on the same semiplane as the current we
					// can safely remove the current point.
					edit_shape.removeVertex(v_cur, true);
					clip_count++;// do not change prev point.
					v_cur = v_next;
					pt_cur.setCoords(pt_next);
					cur_location = next_location;
					continue;
				}

				if (cur_location == 3 && prev_location != 0
						&& next_location != 0) {
					assert ((prev_location & next_location) == 0);// going from
																	// one semi
																	// plane to
																	// another
																	// via the
																	// mid.
					pt_cur.setCoords(corner);
					if (can_erase_corner_point || pt_cur.equals(pt_prev)) {// this
																			// point
																			// can
																			// be
																			// removed
						edit_shape.removeVertex(v_cur, true);
						clip_count++;// do not change prev point.
						v_cur = v_next;
						pt_cur.setCoords(pt_next);
						cur_location = next_location;
						continue;
					} else {
						edit_shape.setXY(v_cur, pt_cur); // snap to the corner
					}
				} else {
					if (next_location == 0
							&& cur_location != 0
							|| next_location != 0
							&& cur_location == 0
							|| ((next_location | cur_location) == 3
									&& next_location != 3 && cur_location != 3)) {
						// clip
					}
				}
			}

			prev_location = cur_location;
			v_prev = v_cur;
			pt_prev.setCoords(pt_cur);
			v_cur = v_next;
			cur_location = next_location;
			pt_cur.setCoords(pt_next);
		}

		return clip_count;
	}
	
	private boolean isDegeneratePath_(MultiPathImpl mp_impl, int ipath) {
		if (mp_impl.getPathSize(ipath) == 1)
			return true;
		Envelope2D env = new Envelope2D();
		mp_impl.queryPathEnvelope2D(ipath, env);
		if (Math.max(env.getWidth(), env.getHeight()) < m_densify_dist * 0.5)
			return true;

		return false;
	}

	private boolean isDegenerateGeometry_(Geometry geom) {
		Envelope2D env = new Envelope2D();
		geom.queryEnvelope2D(env);
		if (Math.max(env.getWidth(), env.getHeight()) < m_densify_dist * 0.5)
			return true;

		return false;
	}

	private Polyline preparePolyline_(Polyline input_geom) {
		// Generalize it firstly using 25% of the densification deviation as a
		// criterion.
		Polyline generalized_polyline = (Polyline) ((OperatorGeneralize) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Generalize)).execute(
				input_geom, m_densify_dist * 0.25, false, m_progress_tracker);

		int path_point_count = 0;
		for (int i = 0, npath = generalized_polyline.getPathCount(); i < npath; i++) {
			path_point_count = Math.max(generalized_polyline.getPathSize(i),
					path_point_count);
		}

		if (path_point_count < 32) {
			m_bfilter = false;
			return generalized_polyline;
		} else {
			m_bfilter = true;
			// If we apply a filter to the polyline, then we have to resolve all
			// self intersections.
			Polyline simple_polyline = (Polyline) (TopologicalOperations
					.planarSimplify(generalized_polyline, m_small_tolerance,
							false, true, m_progress_tracker));
			// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/buffer_simplify.txt",
			// *simple_polyline, nullptr);
			return simple_polyline;
		}
	}

	private void addCircle_(MultiPathImpl result_mp, Point point) {
		// Uses same calculations for each of the quadrants, generating a
		// symmetric distribution of points.
		Point2D center = point.getXY();
		if (m_circle_template != null && !m_circle_template.isEmpty()) {// use
																		// template
																		// if
																		// available.
			Point2D p = new Point2D();
			p.setCoords(m_circle_template.get(0));
			p.scaleAdd(m_abs_distance, center);
			result_mp.startPath(p);
			for (int i = 1, n = (int) m_circle_template.size(); i < n; i++) {
				p.setCoords(m_circle_template.get(i));
				p.scaleAdd(m_abs_distance, center);
				result_mp.lineTo(p);
			}
			return;
		}

		// avoid unnecessary memory allocation for the circle template. Just do
		// the point here.

		int N = m_circle_template_size;
		int real_size = (N + 3) / 4;
		double dA = (Math.PI * 0.5) / real_size;
		// result_mp.reserve(real_size * 4);

		double dcos = Math.cos(dA);
		double dsin = Math.sin(dA);
		Point2D pt = new Point2D();
		for (int quadrant = 3; quadrant >= 0; quadrant--) {
			pt.setCoords(0.0, m_abs_distance);
			switch (quadrant) {
			case 0: {// upper left quadrant
				for (int i = 0; i < real_size; i++) {
					result_mp.lineTo(pt.x + center.x, pt.y + center.y);
					pt.rotateReverse(dcos, dsin);
				}
				break;
			}
			case 1: {// upper left quadrant
				for (int i = 0; i < real_size; i++) {// m_circle_template.set(i
														// + real_size * 1,
														// Point_2D::construct(-pt.y,
														// pt.x));
					result_mp.lineTo(-pt.y + center.x, pt.x + center.y);
					pt.rotateReverse(dcos, dsin);
				}
				break;
			}
			case 2: {// lower left quadrant
						// m_circle_template.set(i + real_size * 2,
						// Point_2D::construct(-pt.x, -pt.y));
				for (int i = 0; i < real_size; i++) {
					result_mp.lineTo(-pt.x + center.x, -pt.y + center.y);
					pt.rotateReverse(dcos, dsin);
				}
				break;
			}
			default:// case 3:
			{// lower right quadrant
				// m_circle_template.set(i + real_size * 3,
				// Point_2D::construct(pt.y, -pt.x));
				result_mp.startPath(pt.y + center.x, -pt.x + center.y);// we
																		// start
																		// at
																		// the
																		// quadrant
																		// 3.
																		// The
																		// first
																		// point
																		// is
																		// (0,
																		// -m_distance)
																		// +
																		// center
				for (int i = 1; i < real_size; i++) {
					pt.rotateReverse(dcos, dsin);
					result_mp.lineTo(pt.y + center.x, -pt.x + center.y);
				}
				break;
			}
			}

			progress_();
		}
	}

	private static Polygon setWeakSimple_(Polygon poly) {
		((MultiPathImpl) poly._getImpl()).setIsSimple(
				MultiVertexGeometryImpl.GeometryXSimple.Weak, 0.0, false);
		return poly;
	}

	private Polygon setStrongSimple_(Polygon poly) {
		((MultiPathImpl) poly._getImpl()).setIsSimple(
				MultiVertexGeometryImpl.GeometryXSimple.Strong, m_tolerance,
				false);
		((MultiPathImpl) poly._getImpl())._updateOGCFlags();
		return poly;
	}
}
