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

class RelationalOperations {
	interface Relation {
		static final int contains = 1;
		static final int within = 2;
		static final int equals = 3;
		static final int disjoint = 4;
		static final int touches = 8;
		static final int crosses = 16;
		static final int overlaps = 32;

		static final int unknown = 0;
		static final int intersects = 0x40000000;
	}

	static boolean relate(Geometry geometry_a, Geometry geometry_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		int type_a = geometry_a.getType().value();
		int type_b = geometry_b.getType().value();

		// Give preference to the Point vs Envelope, Envelope vs Envelope and
		// Point vs Point realtions:
		if (type_a == Geometry.GeometryType.Envelope) {
			if (type_b == Geometry.GeometryType.Envelope) {
				return relate((Envelope) geometry_a, (Envelope) geometry_b, sr,
						relation, progress_tracker);
			} else if (type_b == Geometry.GeometryType.Point) {
				if (relation == Relation.within)
					relation = Relation.contains;
				else if (relation == Relation.contains)
					relation = Relation.within;

				return relate((Point) geometry_b, (Envelope) geometry_a, sr,
						relation, progress_tracker);
			} else {
				// proceed below
			}
		} else if (type_a == Geometry.GeometryType.Point) {
			if (type_b == Geometry.GeometryType.Envelope) {
				return relate((Point) geometry_a, (Envelope) geometry_b, sr,
						relation, progress_tracker);
			} else if (type_b == Geometry.GeometryType.Point) {
				return relate((Point) geometry_a, (Point) geometry_b, sr,
						relation, progress_tracker);
			} else {
				// proceed below
			}
		} else {
			// proceed below
		}

		if (geometry_a.isEmpty() || geometry_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Envelope2D env1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env1);
		Envelope2D env2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2);

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env1);
		envMerged.merge(env2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, false);

		if (envelopeDisjointEnvelope_(env1, env2, tolerance, progress_tracker)) {
			if (relation == Relation.disjoint)
				return true;

			return false;
		}

		boolean bRelation = false;

		Geometry _geometry_a;
		Geometry _geometry_b;
		Polyline polyline_a, polyline_b;

		if (MultiPath.isSegment(type_a)) {
			polyline_a = new Polyline(geometry_a.getDescription());
			polyline_a.addSegment((Segment) geometry_a, true);
			_geometry_a = polyline_a;
			type_a = Geometry.GeometryType.Polyline;
		} else {
			_geometry_a = geometry_a;
		}

		if (MultiPath.isSegment(type_b)) {
			polyline_b = new Polyline(geometry_b.getDescription());
			polyline_b.addSegment((Segment) geometry_b, true);
			_geometry_b = polyline_b;
			type_b = Geometry.GeometryType.Polyline;
		} else {
			_geometry_b = geometry_b;
		}

		if (type_a != Geometry.GeometryType.Envelope
				&& type_b != Geometry.GeometryType.Envelope) {
			if (_geometry_a.getDimension() < _geometry_b.getDimension()
					|| (type_a == Geometry.GeometryType.Point && type_b == Geometry.GeometryType.MultiPoint)) {// we
																												// will
																												// switch
																												// the
																												// order
																												// of
																												// the
																												// geometries
																												// below.
				if (relation == Relation.within)
					relation = Relation.contains;
				else if (relation == Relation.contains)
					relation = Relation.within;
			}
		} else {
			if (type_a != Geometry.GeometryType.Polygon
					&& type_b != Geometry.GeometryType.Envelope) { // we will
																	// switch
																	// the order
																	// of the
																	// geometries
																	// below.
				if (relation == Relation.within)
					relation = Relation.contains;
				else if (relation == Relation.contains)
					relation = Relation.within;
			}
		}

		switch (type_a) {
		case Geometry.GeometryType.Polygon:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePolygon_((Polygon) (_geometry_a),
						(Polygon) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polygonRelatePolyline_((Polygon) (_geometry_a),
						(Polyline) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = polygonRelatePoint_((Polygon) (_geometry_a),
						(Point) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = polygonRelateMultiPoint_((Polygon) (_geometry_a),
						(MultiPoint) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Envelope:
				bRelation = polygonRelateEnvelope_((Polygon) (_geometry_a),
						(Envelope) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Polyline:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePolyline_((Polygon) (_geometry_b),
						(Polyline) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePolyline_((Polyline) (_geometry_a),
						(Polyline) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = polylineRelatePoint_((Polyline) (_geometry_a),
						(Point) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometry_a),
						(MultiPoint) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Envelope:
				bRelation = polylineRelateEnvelope_((Polyline) (_geometry_a),
						(Envelope) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Point:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePoint_((Polygon) (_geometry_b),
						(Point) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePoint_((Polyline) (_geometry_b),
						(Point) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometry_b),
						(Point) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.MultiPoint:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelateMultiPoint_((Polygon) (_geometry_b),
						(MultiPoint) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometry_b),
						(MultiPoint) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelateMultiPoint_(
						(MultiPoint) (_geometry_a), (MultiPoint) (_geometry_b),
						tolerance, relation, progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometry_a),
						(Point) (_geometry_b), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Envelope:
				bRelation = multiPointRelateEnvelope_(
						(MultiPoint) (_geometry_a), (Envelope) (_geometry_b),
						tolerance, relation, progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Envelope:
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelateEnvelope_((Polygon) (_geometry_b),
						(Envelope) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelateEnvelope_((Polyline) (_geometry_b),
						(Envelope) (_geometry_a), tolerance, relation,
						progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelateEnvelope_(
						(MultiPoint) (_geometry_b), (Envelope) (_geometry_a),
						tolerance, relation, progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		default:
			break; // warning fix
		}

		return bRelation;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of envelope_a vs envelope_b for the given
	// relation.
	private static boolean relate(Envelope envelope_a, Envelope envelope_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		if (envelope_a.isEmpty() || envelope_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), env_merged = new Envelope2D();
		envelope_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);
		env_merged.setCoords(env_a);
		env_merged.merge(env_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env_merged, false);

		switch (relation) {
		case Relation.disjoint:
			return envelopeDisjointEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.within:
			return envelopeContainsEnvelope_(env_b, env_a, tolerance,
					progress_tracker);

		case Relation.contains:
			return envelopeContainsEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return envelopeTouchesEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return envelopeOverlapsEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return envelopeCrossesEnvelope_(env_a, env_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of point_a vs envelope_b for the given relation.
	private static boolean relate(Point point_a, Envelope envelope_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		if (point_a.isEmpty() || envelope_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Point2D pt_a = point_a.getXY();
		Envelope2D env_b = new Envelope2D(), env_merged = new Envelope2D();
		envelope_b.queryEnvelope2D(env_b);
		env_merged.setCoords(pt_a);
		env_merged.merge(env_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env_merged, false);

		switch (relation) {
		case Relation.disjoint:
			return pointDisjointEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.within:
			return pointWithinEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return pointContainsEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return pointEqualsEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return pointTouchesEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of point_a vs point_b for the given relation.
	private static boolean relate(Point point_a, Point point_b,
			SpatialReference sr, int relation, ProgressTracker progress_tracker) {
		if (point_a.isEmpty() || point_b.isEmpty()) {
			if (relation == Relation.disjoint)
				return true; // Always true

			return false; // Always false
		}

		Point2D pt_a = point_a.getXY();
		Point2D pt_b = point_b.getXY();
		Envelope2D env_merged = new Envelope2D();
		env_merged.setCoords(pt_a);
		env_merged.merge(pt_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env_merged, false);

		switch (relation) {
		case Relation.disjoint:
			return pointDisjointPoint_(pt_a, pt_b, tolerance, progress_tracker);

		case Relation.within:
			return pointContainsPoint_(pt_b, pt_a, tolerance, progress_tracker);

		case Relation.contains:
			return pointContainsPoint_(pt_a, pt_b, tolerance, progress_tracker);

		case Relation.equals:
			return pointEqualsPoint_(pt_a, pt_b, tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polygonRelatePolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.within:
			return polygonContainsPolygon_(polygon_b, polygon_a, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polygonEqualsPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polygonOverlapsPolygon_(polygon_a, polygon_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polygonRelatePolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polygonCrossesPolyline_(polygon_a, polyline_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polygonRelatePoint_(Polygon polygon_a,
			Point point_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointPoint_(polygon_a, point_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsPoint_(polygon_a, point_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesPoint_(polygon_a, point_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds
	private static boolean polygonRelateMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polygonDisjointMultiPoint_(polygon_a, multipoint_b,
					tolerance, true, progress_tracker);

		case Relation.contains:
			return polygonContainsMultiPoint_(polygon_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.touches:
			return polygonTouchesMultiPoint_(polygon_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.crosses:
			return polygonCrossesMultiPoint_(polygon_a, multipoint_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds
	private static boolean polygonRelateEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		if (polygonDisjointEnvelope_(polygon_a, envelope_b, tolerance,
				progress_tracker)) {
			if (relation == Relation.disjoint)
				return true;

			return false;
		} else if (relation == Relation.disjoint) {
			return false;
		}

		switch (relation) {
		case Relation.within:
			return polygonWithinEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polygonContainsEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polygonEqualsEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polygonTouchesEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polygonOverlapsEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polygonCrossesEnvelope_(polygon_a, envelope_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelatePolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polylineDisjointPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.within:
			return polylineContainsPolyline_(polyline_b, polyline_a, tolerance,
					progress_tracker);

		case Relation.contains:
			return polylineContainsPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polylineEqualsPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polylineTouchesPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polylineOverlapsPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polylineCrossesPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelatePoint_(Polyline polyline_a,
			Point point_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polylineDisjointPoint_(polyline_a, point_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polylineContainsPoint_(polyline_a, point_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polylineTouchesPoint_(polyline_a, point_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelateMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return polylineDisjointMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.contains:
			return polylineContainsMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.touches:
			return polylineTouchesMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.crosses:
			return polylineCrossesMultiPoint_(polyline_a, multipoint_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean polylineRelateEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		if (polylineDisjointEnvelope_(polyline_a, envelope_b, tolerance,
				progress_tracker)) {
			if (relation == Relation.disjoint)
				return true;

			return false;
		} else if (relation == Relation.disjoint) {
			return false;
		}

		switch (relation) {
		case Relation.within:
			return polylineWithinEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return polylineContainsEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return polylineEqualsEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.touches:
			return polylineTouchesEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.overlaps:
			return polylineOverlapsEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		case Relation.crosses:
			return polylineCrossesEnvelope_(polyline_a, envelope_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean multiPointRelateMultiPoint_(MultiPoint multipoint_a,
			MultiPoint multipoint_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return multiPointDisjointMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.within:
			return multiPointContainsMultiPoint_(multipoint_b, multipoint_a,
					tolerance, progress_tracker);

		case Relation.contains:
			return multiPointContainsMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.equals:
			return multiPointEqualsMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		case Relation.overlaps:
			return multiPointOverlapsMultiPoint_(multipoint_a, multipoint_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean multiPointRelatePoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return multiPointDisjointPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		case Relation.within:
			return multiPointWithinPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		case Relation.contains:
			return multiPointContainsPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		case Relation.equals:
			return multiPointEqualsPoint_(multipoint_a, point_b, tolerance,
					progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if the relation holds.
	private static boolean multiPointRelateEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance, int relation,
			ProgressTracker progress_tracker) {
		switch (relation) {
		case Relation.disjoint:
			return multiPointDisjointEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.within:
			return multiPointWithinEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.contains:
			return multiPointContainsEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.equals:
			return multiPointEqualsEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.touches:
			return multiPointTouchesEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		case Relation.crosses:
			return multiPointCrossesEnvelope_(multipoint_a, envelope_b,
					tolerance, progress_tracker);

		default:
			break; // warning fix
		}

		return false;
	}

	// Returns true if polygon_a equals polygon_b.
	private static boolean polygonEqualsPolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polygon_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		if (!envelopeEqualsEnvelope_(env_a, env_b, tolerance, progress_tracker))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains
				|| relation == Relation.within)
			return false;

		// Quick point equality check for true equality. This just checks if all
		// the points in each ring are the same (within a tolerance) and in the
		// same order
		if (multiPathExactlyEqualsMultiPath_(polygon_a, polygon_b, tolerance,
				progress_tracker))
			return true;

		double length_a = polygon_a.calculateLength2D();
		double length_b = polygon_b.calculateLength2D();
		int max_vertices = Math.max(polygon_a.getPointCount(),
				polygon_b.getPointCount());

		if (Math.abs(length_a - length_b) > max_vertices * 4.0 * tolerance)
			return false;

		return linearPathEqualsLinearPath_(polygon_a, polygon_b, tolerance, true);
	}

	// Returns true if polygon_a is disjoint from polygon_b.
	private static boolean polygonDisjointPolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b,
				tolerance, true);

		if (relation == Relation.disjoint)
			return true;

		if (relation == Relation.contains || relation == Relation.within
				|| relation == Relation.intersects)
			return false;

		return polygonDisjointMultiPath_(polygon_a, polygon_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a touches polygon_b.
	private static boolean polygonTouchesPolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains
				|| relation == Relation.within)
			return false;

		return polygonTouchesPolygonImpl_(polygon_a, polygon_b, tolerance, null);
	}

	// Returns true if polygon_a overlaps polygon_b.
	private static boolean polygonOverlapsPolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains
				|| relation == Relation.within)
			return false;

		return polygonOverlapsPolygonImpl_(polygon_a, polygon_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a contains polygon_b.
	private static boolean polygonContainsPolygon_(Polygon polygon_a,
			Polygon polygon_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polygon_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.within)
			return false;

		if (relation == Relation.contains)
			return true;

		return polygonContainsPolygonImpl_(polygon_a, polygon_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a is disjoint from polyline_b.
	private static boolean polygonDisjointPolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polyline_b,
				tolerance, true);

		if (relation == Relation.disjoint)
			return true;

		if (relation == Relation.contains || relation == Relation.intersects)
			return false;

		return polygonDisjointMultiPath_(polygon_a, polyline_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a touches polyline_b.
	private static boolean polygonTouchesPolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polyline_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains)
			return false;

		return polygonTouchesPolylineImpl_(polygon_a, polyline_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a crosses polyline_b.
	private static boolean polygonCrossesPolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polyline_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains)
			return false;

		return polygonCrossesPolylineImpl_(polygon_a, polyline_b, tolerance,
				null);
	}

	// Returns true if polygon_a contains polyline_b.
	private static boolean polygonContainsPolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, polyline_b,
				tolerance, false);

		if (relation == Relation.disjoint)
			return false;

		if (relation == Relation.contains)
			return true;

		return polygonContainsPolylineImpl_(polygon_a, polyline_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a is disjoint from point_b.
	private static boolean polygonDisjointPoint_(Polygon polygon_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(
				polygon_a, point_b, tolerance);

		if (result == PolygonUtils.PiPResult.PiPOutside)
			return true;

		return false;
	}

	// Returns true of polygon_a touches point_b.
	private static boolean polygonTouchesPoint_(Polygon polygon_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		Point2D pt_b = point_b.getXY();
		return polygonTouchesPointImpl_(polygon_a, pt_b, tolerance, null);
	}

	// Returns true if polygon_a contains point_b.
	private static boolean polygonContainsPoint_(Polygon polygon_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		Point2D pt_b = point_b.getXY();
		return polygonContainsPointImpl_(polygon_a, pt_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a is disjoint from multipoint_b.
	private static boolean polygonDisjointMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance,
			boolean bIncludeBoundaryA, ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a,
				multipoint_b, tolerance, false);

		if (relation == Relation.disjoint)
			return true;

		if (relation == Relation.contains)
			return false;

		Envelope2D env_a_inflated = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a_inflated);
		env_a_inflated.inflate(tolerance, tolerance);
		Point2D ptB = new Point2D();

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			multipoint_b.getXY(i, ptB);

			if (!env_a_inflated.contains(ptB))
				continue;

			PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(
					polygon_a, ptB, tolerance);

			if (result == PolygonUtils.PiPResult.PiPInside
					|| (bIncludeBoundaryA && result == PolygonUtils.PiPResult.PiPBoundary))
				return false;
		}

		return true;
	}

	// Returns true if polygon_a touches multipoint_b.
	private static boolean polygonTouchesMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a,
				multipoint_b, tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains)
			return false;

        Envelope2D env_a_inflated = new Envelope2D();
        polygon_a.queryEnvelope2D(env_a_inflated);
        env_a_inflated.inflate(tolerance, tolerance);

        Point2D ptB;
        boolean b_boundary = false;

        MultiPathImpl polygon_a_impl = (MultiPathImpl)polygon_a._getImpl();

        Polygon pa = null;
        Polygon p_polygon_a = polygon_a;

        boolean b_checked_polygon_a_quad_tree = false;

        for (int i = 0; i < multipoint_b.getPointCount(); i++)
        {
            ptB = multipoint_b.getXY(i);

            if (env_a_inflated.contains(ptB)) {

                PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(p_polygon_a, ptB, tolerance);

                if (result == PolygonUtils.PiPResult.PiPBoundary)
                    b_boundary = true;
                else if (result == PolygonUtils.PiPResult.PiPInside)
                    return false;
            }

            if (!b_checked_polygon_a_quad_tree) {
                if (PointInPolygonHelper.quadTreeWillHelp(polygon_a, multipoint_b.getPointCount() - 1) && (polygon_a_impl._getAccelerators() == null || polygon_a_impl._getAccelerators().getQuadTree() == null)) {
                    pa = new Polygon();
                    polygon_a.copyTo(pa);
                    ((MultiPathImpl) pa._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                    p_polygon_a = pa;
                } else {
                    p_polygon_a = polygon_a;
                }

                b_checked_polygon_a_quad_tree = true;
            }
        }

        if (b_boundary)
            return true;

        return false;
	}

	// Returns true if polygon_a crosses multipoint_b.
	private static boolean polygonCrossesMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a,
				multipoint_b, tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains)
			return false;

        Envelope2D env_a = new Envelope2D(), env_a_inflated = new Envelope2D(), env_b = new Envelope2D();
        polygon_a.queryEnvelope2D(env_a);
        multipoint_b.queryEnvelope2D(env_b);
        env_a_inflated.setCoords(env_a);
        env_a_inflated.inflate(tolerance, tolerance);

        boolean b_interior = false, b_exterior = false;

        Point2D pt_b;

        MultiPathImpl polygon_a_impl = (MultiPathImpl)polygon_a._getImpl();

        Polygon pa = null;
        Polygon p_polygon_a = polygon_a;

        boolean b_checked_polygon_a_quad_tree = false;

        for (int i = 0; i < multipoint_b.getPointCount(); i++)
        {
            pt_b = multipoint_b.getXY(i);

            if (!env_a_inflated.contains(pt_b))
            {
                b_exterior = true;
            }
            else
            {
                PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(p_polygon_a, pt_b, tolerance);

                if (result == PolygonUtils.PiPResult.PiPOutside)
                    b_exterior = true;
                else if (result == PolygonUtils.PiPResult.PiPInside)
                    b_interior = true;
            }

            if (b_interior && b_exterior)
                return true;

            if (!b_checked_polygon_a_quad_tree) {
                if (PointInPolygonHelper.quadTreeWillHelp(polygon_a, multipoint_b.getPointCount() - 1) && (polygon_a_impl._getAccelerators() == null || polygon_a_impl._getAccelerators().getQuadTree() == null)) {
                    pa = new Polygon();
                    polygon_a.copyTo(pa);
                    ((MultiPathImpl) pa._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                    p_polygon_a = pa;
                } else {
                    p_polygon_a = polygon_a;
                }

                b_checked_polygon_a_quad_tree = true;
            }
        }

        return false;
	}

	// Returns true if polygon_a contains multipoint_b.
	private static boolean polygonContainsMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a,
				multipoint_b, tolerance, false);

		if (relation == Relation.disjoint)
			return false;

		if (relation == Relation.contains)
			return true;

        boolean b_interior = false;
        Point2D ptB;

        MultiPathImpl polygon_a_impl = (MultiPathImpl)polygon_a._getImpl();

        Polygon pa = null;
        Polygon p_polygon_a = polygon_a;

        boolean b_checked_polygon_a_quad_tree = false;

        for (int i = 0; i < multipoint_b.getPointCount(); i++)
        {
            ptB = multipoint_b.getXY(i);

            if (!env_a.contains(ptB))
                return false;

            PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(p_polygon_a, ptB, tolerance);

            if (result == PolygonUtils.PiPResult.PiPInside)
                b_interior = true;
            else if (result == PolygonUtils.PiPResult.PiPOutside)
                return false;

            if (!b_checked_polygon_a_quad_tree) {
                if (PointInPolygonHelper.quadTreeWillHelp(polygon_a, multipoint_b.getPointCount() - 1) && (polygon_a_impl._getAccelerators() == null || polygon_a_impl._getAccelerators().getQuadTree() == null)) {
                    pa = new Polygon();
                    polygon_a.copyTo(pa);
                    ((MultiPathImpl) pa._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                    p_polygon_a = pa;
                } else {
                    p_polygon_a = polygon_a;
                }

                b_checked_polygon_a_quad_tree = true;
            }
        }

        return b_interior;
	}

	// Returns true if polygon_a equals envelope_b.
	private static boolean polygonEqualsEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		// This check will correctly handle degenerate envelope cases (i.e.
		// degenerate to point or line)
		if (!envelopeEqualsEnvelope_(env_a, env_b, tolerance, progress_tracker))
			return false;

		Polygon polygon_b = new Polygon();
		polygon_b.addEnvelope(envelope_b, false);

		return linearPathEqualsLinearPath_(polygon_a, polygon_b, tolerance, true);
	}

	// Returns true if polygon_a is disjoint from envelope_b.
	private static boolean polygonDisjointEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, envelope_b,
				tolerance, false);

		if (relation == Relation.disjoint)
			return true;

		if (relation == Relation.contains || relation == Relation.within)
			return false;

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		PolygonUtils.PiPResult pipres;
		Point2D pt_b = new Point2D();
		env_b.queryLowerLeft(pt_b);
		pipres = PolygonUtils.isPointInPolygon2D(polygon_a, pt_b, tolerance);
		if (pipres != PolygonUtils.PiPResult.PiPOutside)
			return false;

		env_b.queryLowerRight(pt_b);
		pipres = PolygonUtils.isPointInPolygon2D(polygon_a, pt_b, tolerance);
		if (pipres != PolygonUtils.PiPResult.PiPOutside)
			return false;

		env_b.queryUpperRight(pt_b);
		pipres = PolygonUtils.isPointInPolygon2D(polygon_a, pt_b, tolerance);
		if (pipres != PolygonUtils.PiPResult.PiPOutside)
			return false;

		env_b.queryUpperLeft(pt_b);
		pipres = PolygonUtils.isPointInPolygon2D(polygon_a, pt_b, tolerance);
		if (pipres != PolygonUtils.PiPResult.PiPOutside)
			return false;

		MultiPathImpl mimpl_a = (MultiPathImpl) polygon_a._getImpl();
		AttributeStreamOfDbl pos = (AttributeStreamOfDbl) (mimpl_a
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION));

		Envelope2D env_b_inflated = new Envelope2D();
		env_b_inflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);
		for (int ptIndex = 0, n = mimpl_a.getPointCount(); ptIndex < n; ptIndex++) {
			double x = pos.read(2 * ptIndex);
			double y = pos.read(2 * ptIndex + 1);
			if (env_b_inflated.contains(x, y))
				return false;
		}

		return !linearPathIntersectsEnvelope_(polygon_a, env_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a touches envelope_b.
	private static boolean polygonTouchesEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, envelope_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains
				|| relation == Relation.within)
			return false;

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getWidth() <= tolerance && env_b.getHeight() <= tolerance) {// treat
																				// as
																				// point
			Point2D pt_b = envelope_b.getCenterXY();
			return polygonTouchesPointImpl_(polygon_a, pt_b, tolerance,
					progress_tracker);
		}

		if (env_b.getWidth() <= tolerance || env_b.getHeight() <= tolerance) {// treat
																				// as
																				// polyline
			Polyline polyline_b = new Polyline();
			Point p = new Point();
			envelope_b.queryCornerByVal(0, p);
			polyline_b.startPath(p);
			envelope_b.queryCornerByVal(2, p);
			polyline_b.lineTo(p);
			return polygonTouchesPolylineImpl_(polygon_a, polyline_b,
					tolerance, progress_tracker);
		}

		// treat as polygon
		Polygon polygon_b = new Polygon();
		polygon_b.addEnvelope(envelope_b, false);
		return polygonTouchesPolygonImpl_(polygon_a, polygon_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a overlaps envelope_b.
	private static boolean polygonOverlapsEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, envelope_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.contains
				|| relation == Relation.within)
			return false;

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getWidth() <= tolerance || env_b.getHeight() <= tolerance)
			return false; // has no interior

		Polygon polygon_b = new Polygon();
		polygon_b.addEnvelope(envelope_b, false);
		return polygonOverlapsPolygonImpl_(polygon_a, polygon_b, tolerance,
				progress_tracker);
	}

	// Returns true if polygon_a is within envelope_b
	private static boolean polygonWithinEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);
		return envelopeInfContainsEnvelope_(env_b, env_a, tolerance);
	}

	// Returns true if polygon_a contains envelope_b.
	private static boolean polygonContainsEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick envelope rejection test

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint,
		// or if one is contained in the other.
		int relation = tryRasterizedContainsOrDisjoint_(polygon_a, envelope_b,
				tolerance, false);

		if (relation == Relation.disjoint || relation == Relation.within)
			return false;

		if (relation == Relation.contains)
			return true;

		if (env_b.getWidth() <= tolerance && env_b.getHeight() <= tolerance) {// treat
																				// as
																				// point
			Point2D pt_b = envelope_b.getCenterXY();
			return polygonContainsPointImpl_(polygon_a, pt_b, tolerance,
					progress_tracker);
		}

		if (env_b.getWidth() <= tolerance || env_b.getHeight() <= tolerance) {// treat
																				// as
																				// polyline
			Polyline polyline_b = new Polyline();
			Point p = new Point();
			envelope_b.queryCornerByVal(0, p);
			polyline_b.startPath(p);
			envelope_b.queryCornerByVal(2, p);
			polyline_b.lineTo(p);
			return polygonContainsPolylineImpl_(polygon_a, polyline_b,
					tolerance, null);
		}

		// treat as polygon
		Polygon polygon_b = new Polygon();
		polygon_b.addEnvelope(envelope_b, false);
		return polygonContainsPolygonImpl_(polygon_a, polygon_b, tolerance,
				null);
	}

	// Returns true if polygon_a crosses envelope_b.
	private static boolean polygonCrossesEnvelope_(Polygon polygon_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getHeight() > tolerance && env_b.getWidth() > tolerance)
			return false; // when treated as an area, areas cannot cross areas.

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // when treated as a point, areas cannot cross points.

		// Treat as polyline
		Polyline polyline_b = new Polyline();
		Point p = new Point();
		envelope_b.queryCornerByVal(0, p);
		polyline_b.startPath(p);
		envelope_b.queryCornerByVal(2, p);
		polyline_b.lineTo(p);
		return polygonCrossesPolylineImpl_(polygon_a, polyline_b, tolerance,
				progress_tracker);
	}

	// Returns true if polyline_a equals polyline_b.
	private static boolean polylineEqualsPolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		if (!envelopeEqualsEnvelope_(env_a, env_b, tolerance, progress_tracker))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b, tolerance,
				false) == Relation.disjoint)
			return false;

		// Quick point equality check for true equality. This just checks if all
		// the points in each ring are the same (within a tolerance) and in the
		// same order
		if (multiPathExactlyEqualsMultiPath_(polyline_a, polyline_b, tolerance,
				progress_tracker))
			return true;

		return linearPathEqualsLinearPath_(polyline_a, polyline_b, tolerance, false);
	}

	// Returns true if polyline_a is disjoint from polyline_b.
	private static boolean polylineDisjointPolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b, tolerance,
				false) == Relation.disjoint)
			return true;

        MultiPathImpl multi_path_impl_a = (MultiPathImpl)polyline_a._getImpl();
        MultiPathImpl multi_path_impl_b = (MultiPathImpl)polyline_b._getImpl();

        PairwiseIntersectorImpl intersector_paths = new PairwiseIntersectorImpl(multi_path_impl_a, multi_path_impl_b, tolerance, true);

        if (!intersector_paths.next())
            return false;

		return !linearPathIntersectsLinearPath_(polyline_a, polyline_b,
				tolerance);
	}

	// Returns true if polyline_a touches polyline_b.
	private static boolean polylineTouchesPolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b, tolerance,
				false) == Relation.disjoint)
			return false;

		AttributeStreamOfDbl intersections = new AttributeStreamOfDbl(0);

		int dim = linearPathIntersectsLinearPathMaxDim_(polyline_a, polyline_b,
				tolerance, intersections);

		if (dim != 0)
			return false;

		MultiPoint intersection = new MultiPoint();

		for (int i = 0; i < intersections.size(); i += 2) {
			double x = intersections.read(i);
			double y = intersections.read(i + 1);
			intersection.add(x, y);
		}

		MultiPoint boundary_a_b = (MultiPoint) (polyline_a.getBoundary());
		MultiPoint boundary_b = (MultiPoint) (polyline_b.getBoundary());

		boundary_a_b.add(boundary_b, 0, boundary_b.getPointCount());

		return multiPointContainsMultiPointBrute_(boundary_a_b, intersection,
				tolerance);
	}

	// Returns true if polyline_a crosses polyline_b.
	private static boolean polylineCrossesPolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b, tolerance,
				false) == Relation.disjoint)
			return false;

		AttributeStreamOfDbl intersections = new AttributeStreamOfDbl(0);

		int dim = linearPathIntersectsLinearPathMaxDim_(polyline_a, polyline_b,
				tolerance, intersections);

		if (dim != 0)
			return false;

		MultiPoint intersection = new MultiPoint();

		for (int i = 0; i < intersections.size(); i += 2) {
			double x = intersections.read(i);
			double y = intersections.read(i + 1);
			intersection.add(x, y);
		}

		MultiPoint boundary_a_b = (MultiPoint) (polyline_a.getBoundary());
		MultiPoint boundary_b = (MultiPoint) (polyline_b.getBoundary());

		boundary_a_b.add(boundary_b, 0, boundary_b.getPointCount());

		return !multiPointContainsMultiPointBrute_(boundary_a_b, intersection,
				tolerance);
	}

	// Returns true if polyline_a overlaps polyline_b.
	private static boolean polylineOverlapsPolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b, tolerance,
				false) == Relation.disjoint)
			return false;

		return linearPathOverlapsLinearPath_(polyline_a, polyline_b, tolerance);
	}

	// Returns true if polyline_a contains polyline_b.
	private static boolean polylineContainsPolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);

		// Quick envelope rejection test for false equality.
		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b, tolerance,
				false) == Relation.disjoint)
			return false;

		return linearPathWithinLinearPath_(polyline_b, polyline_a, tolerance, false);
	}

	// Returns true if polyline_a is disjoint from point_b.
	private static boolean polylineDisjointPoint_(Polyline polyline_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, point_b, tolerance,
				false) == Relation.disjoint)
			return true;

		Point2D pt_b = point_b.getXY();
		return !linearPathIntersectsPoint_(polyline_a, pt_b, tolerance);
	}

	// Returns true if polyline_a touches point_b.
	private static boolean polylineTouchesPoint_(Polyline polyline_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, point_b, tolerance,
				false) == Relation.disjoint)
			return false;

		Point2D pt_b = point_b.getXY();
		return linearPathTouchesPointImpl_(polyline_a, pt_b, tolerance);
	}

	// Returns true of polyline_a contains point_b.
	private static boolean polylineContainsPoint_(Polyline polyline_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, point_b, tolerance,
				false) == Relation.disjoint)
			return false;

		Point2D pt_b = point_b.getXY();
		return linearPathContainsPoint_(polyline_a, pt_b, tolerance);
	}

	// Returns true if polyline_a is disjoint from multipoint_b.
	private static boolean polylineDisjointMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, multipoint_b,
				tolerance, false) == Relation.disjoint)
			return true;

		return !linearPathIntersectsMultiPoint_(polyline_a, multipoint_b,
				tolerance, false);
	}

	// Returns true if polyline_a touches multipoint_b.
	private static boolean polylineTouchesMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, multipoint_b,
				tolerance, false) == Relation.disjoint) {
			return false;
		}

		SegmentIteratorImpl segIterA = ((MultiPathImpl) polyline_a._getImpl())
				.querySegmentIterator();

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		QuadTreeImpl qtA = null;
		QuadTreeImpl quadTreeA = null;
        QuadTreeImpl quadTreePathsA = null;

		GeometryAccelerators accel = ((MultiPathImpl) (polyline_a._getImpl()))
				._getAccelerators();

		if (accel != null) {
			quadTreeA = accel.getQuadTree();
            quadTreePathsA = accel.getQuadTreeForPaths();
			if (quadTreeA == null) {
				qtA = InternalUtils.buildQuadTree(
						(MultiPathImpl) polyline_a._getImpl(), envInter);
				quadTreeA = qtA;
			}
		} else {
			qtA = InternalUtils.buildQuadTree(
					(MultiPathImpl) polyline_a._getImpl(), envInter);
			quadTreeA = qtA;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterA = quadTreeA.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsA = null;
        if (quadTreePathsA != null)
            qtIterPathsA = quadTreePathsA.getIterator();

		Point2D ptB = new Point2D(), closest = new Point2D();
		boolean b_intersects = false;
		double toleranceSq = tolerance * tolerance;

		AttributeStreamOfInt8 intersects = new AttributeStreamOfInt8(
				multipoint_b.getPointCount());
		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			intersects.write(i, (byte) 0);
		}

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			multipoint_b.getXY(i, ptB);

			if (!envInter.contains(ptB)) {
				continue;
			}

			env_b.setCoords(ptB.x, ptB.y, ptB.x, ptB.y);

            if (qtIterPathsA != null) {
                qtIterPathsA.resetIterator(env_b, tolerance);

                if (qtIterPathsA.next() == -1)
                    continue;
            }

			qtIterA.resetIterator(env_b, tolerance);

			for (int elementHandleA = qtIterA.next(); elementHandleA != -1; elementHandleA = qtIterA
					.next()) {
				int vertex_a = quadTreeA.getElement(elementHandleA);
				segIterA.resetToVertex(vertex_a);

				Segment segmentA = segIterA.nextSegment();
				double t = segmentA.getClosestCoordinate(ptB, false);
				segmentA.getCoord2D(t, closest);

				if (Point2D.sqrDistance(ptB, closest) <= toleranceSq) {
					intersects.write(i, (byte) 1);
					b_intersects = true;
					break;
				}
			}
		}

		if (!b_intersects) {
			return false;
		}

		MultiPoint boundary_a = (MultiPoint) (polyline_a.getBoundary());
		MultiPoint multipoint_b_inter = new MultiPoint();
		Point2D pt = new Point2D();

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			if (intersects.read(i) == 0) {
				continue;
			}

			multipoint_b.getXY(i, pt);
			multipoint_b_inter.add(pt.x, pt.y);
		}

		return multiPointContainsMultiPointBrute_(boundary_a,
				multipoint_b_inter, tolerance);
	}

	// Returns true if polyline_a crosses multipoint_b.
	private static boolean polylineCrossesMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, multipoint_b,
				tolerance, false) == Relation.disjoint) {
			return false;
		}

		SegmentIteratorImpl segIterA = ((MultiPathImpl) polyline_a._getImpl())
				.querySegmentIterator();

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		QuadTreeImpl qtA = null;
		QuadTreeImpl quadTreeA = null;
        QuadTreeImpl quadTreePathsA = null;

		GeometryAccelerators accel = ((MultiPathImpl) (polyline_a._getImpl()))
				._getAccelerators();

		if (accel != null) {
			quadTreeA = accel.getQuadTree();
            quadTreePathsA = accel.getQuadTreeForPaths();
			if (quadTreeA == null) {
				qtA = InternalUtils.buildQuadTree(
						(MultiPathImpl) polyline_a._getImpl(), envInter);
				quadTreeA = qtA;
			}
		} else {
			qtA = InternalUtils.buildQuadTree(
					(MultiPathImpl) polyline_a._getImpl(), envInter);
			quadTreeA = qtA;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterA = quadTreeA.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsA = null;
        if (quadTreePathsA != null)
            qtIterPathsA = quadTreePathsA.getIterator();

        Point2D ptB = new Point2D(), closest = new Point2D();
		boolean b_intersects = false;
		boolean b_exterior_found = false;
		double toleranceSq = tolerance * tolerance;

		AttributeStreamOfInt8 intersects = new AttributeStreamOfInt8(
				multipoint_b.getPointCount());
		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			intersects.write(i, (byte) 0);
		}

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			multipoint_b.getXY(i, ptB);

			if (!envInter.contains(ptB)) {
				b_exterior_found = true;
				continue;
			}

			env_b.setCoords(ptB.x, ptB.y, ptB.x, ptB.y);

            if (qtIterPathsA != null) {
                qtIterPathsA.resetIterator(env_b, tolerance);

                if (qtIterPathsA.next() == -1) {
                    b_exterior_found = true;
                    continue;
                }
            }

			qtIterA.resetIterator(env_b, tolerance);

			boolean b_covered = false;

			for (int elementHandleA = qtIterA.next(); elementHandleA != -1; elementHandleA = qtIterA
					.next()) {
				int vertex_a = quadTreeA.getElement(elementHandleA);
				segIterA.resetToVertex(vertex_a);

				Segment segmentA = segIterA.nextSegment();
				double t = segmentA.getClosestCoordinate(ptB, false);
				segmentA.getCoord2D(t, closest);

				if (Point2D.sqrDistance(ptB, closest) <= toleranceSq) {
					intersects.write(i, (byte) 1);
					b_intersects = true;
					b_covered = true;
					break;
				}
			}

			if (!b_covered) {
				b_exterior_found = true;
			}
		}

		if (!b_intersects || !b_exterior_found) {
			return false;
		}

		MultiPoint boundary_a = (MultiPoint) (polyline_a.getBoundary());
		MultiPoint multipoint_b_inter = new MultiPoint();
		Point2D pt = new Point2D();

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			if (intersects.read(i) == 0) {
				continue;
			}

			multipoint_b.getXY(i, pt);
			multipoint_b_inter.add(pt.x, pt.y);
		}

		return !multiPointContainsMultiPointBrute_(boundary_a,
				multipoint_b_inter, tolerance);
	}

	// Returns true if polyline_a contains multipoint_b.
	private static boolean polylineContainsMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		// Quick rasterize test to see whether the the geometries are disjoint.
		if (tryRasterizedContainsOrDisjoint_(polyline_a, multipoint_b,
				tolerance, false) == Relation.disjoint)
			return false;

		if (!linearPathIntersectsMultiPoint_(polyline_a, multipoint_b,
				tolerance, true))
			return false;

		MultiPoint boundary_a = (MultiPoint) (polyline_a.getBoundary());
		return !multiPointIntersectsMultiPoint_(boundary_a, multipoint_b,
				tolerance, progress_tracker);
	}

	// Returns true if polyline_a equals envelope_b.
	private static boolean polylineEqualsEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (env_b.getHeight() > tolerance && env_b.getWidth() > tolerance)
			return false; // area cannot equal a line

		return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
				progress_tracker);
	}

	// Returns true if polyline_a is disjoint from envelope_b.
	private static boolean polylineDisjointEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		return !linearPathIntersectsEnvelope_(polyline_a, env_b, tolerance,
				progress_tracker);
	}

	// Returns true if polyline_a touches envelope_b.
	private static boolean polylineTouchesEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance) {// Treat
																				// as
																				// point
			Point2D pt_b = envelope_b.getCenterXY();
			return linearPathTouchesPointImpl_(polyline_a, pt_b, tolerance);
		}

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// Treat
																				// as
																				// polyline
			Polyline polyline_b = new Polyline();
			Point p = new Point();
			envelope_b.queryCornerByVal(0, p);
			polyline_b.startPath(p);
			envelope_b.queryCornerByVal(2, p);
			polyline_b.lineTo(p);
			return polylineTouchesPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);
		}

		// Treat env_b as area

		SegmentIterator seg_iter_a = polyline_a.querySegmentIterator();
		Envelope2D env_b_deflated = new Envelope2D(), env_b_inflated = new Envelope2D();
		env_b_deflated.setCoords(env_b);
		env_b_inflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);
		env_b_inflated.inflate(tolerance, tolerance);

		boolean b_boundary = false;
		Envelope2D env_segment_a = new Envelope2D();
		Envelope2D env_inter = new Envelope2D();

		while (seg_iter_a.nextPath()) {
			while (seg_iter_a.hasNextSegment()) {
				Segment segment_a = seg_iter_a.nextSegment();
				segment_a.queryEnvelope2D(env_segment_a);

				env_inter.setCoords(env_b_deflated);
				env_inter.intersect(env_segment_a);

				if (!env_inter.isEmpty()
						&& (env_inter.getHeight() > tolerance || env_inter
								.getWidth() > tolerance))
					return false; // consider segment within

				env_inter.setCoords(env_b_inflated);
				env_inter.intersect(env_segment_a);

				if (!env_inter.isEmpty())
					b_boundary = true;
			}
		}

		return b_boundary;
	}

	// Returns true if polyline_a overlaps envelope_b.
	private static boolean polylineOverlapsEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_a, env_b, tolerance)
				|| envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getHeight() > tolerance && env_b.getWidth() > tolerance)
			return false; // lines cannot overlap areas

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // lines cannot overlap points

		// Treat as polyline
		Polyline polyline_b = new Polyline();
		Point p = new Point();
		envelope_b.queryCornerByVal(0, p);
		polyline_b.startPath(p);
		envelope_b.queryCornerByVal(2, p);
		polyline_b.lineTo(p);
		return linearPathOverlapsLinearPath_(polyline_a, polyline_b, tolerance);
	}

	// Returns true if polyline_a is within envelope_b.
	private static boolean polylineWithinEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (!envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false;

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance)
			return envelopeInfContainsEnvelope_(env_b, env_a, tolerance);

		SegmentIterator seg_iter_a = polyline_a.querySegmentIterator();
		Envelope2D env_b_deflated = new Envelope2D();
		env_b_deflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);

		boolean b_interior = false;
		Envelope2D env_segment_a = new Envelope2D();
		Envelope2D env_inter = new Envelope2D();

		while (seg_iter_a.nextPath()) {
			while (seg_iter_a.hasNextSegment()) {
				Segment segment_a = seg_iter_a.nextSegment();
				segment_a.queryEnvelope2D(env_segment_a);

				if (env_b_deflated.containsExclusive(env_segment_a)) {
					b_interior = true;
					continue;
				}

				env_inter.setCoords(env_b_deflated);
				env_inter.intersect(env_segment_a);

				if (!env_inter.isEmpty()
						&& (env_inter.getHeight() > tolerance || env_inter
								.getWidth() > tolerance))
					b_interior = true;
			}
		}

		return b_interior;
	}

	// Returns true if polyline_a contains envelope_b.
	private static boolean polylineContainsEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		envelope_b.queryEnvelope2D(env_b);
		polyline_a.queryEnvelope2D(env_a);

		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		if (env_b.getHeight() > tolerance && env_b.getWidth() > tolerance)
			return false; // when treated as an area, lines cannot contain
							// areas.

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance) {// Treat
																				// as
																				// point
			Point2D pt_b = envelope_b.getCenterXY();
			return linearPathContainsPoint_(polyline_a, pt_b, tolerance);
		}

		// Treat as polyline
		Polyline polyline_b = new Polyline();
		Point p = new Point();
		envelope_b.queryCornerByVal(0, p);
		polyline_b.startPath(p);
		envelope_b.queryCornerByVal(2, p);
		polyline_b.lineTo(p);
		return linearPathWithinLinearPath_(polyline_b, polyline_a, tolerance, false);
	}

	// Returns true if polyline_a crosses envelope_b.
	private static boolean polylineCrossesEnvelope_(Polyline polyline_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // when treated as a point, lines cannot cross points.

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// Treat
																				// as
																				// polyline
			Polyline polyline_b = new Polyline();
			Point p = new Point();
			envelope_b.queryCornerByVal(0, p);
			polyline_b.startPath(p);
			envelope_b.queryCornerByVal(2, p);
			polyline_b.lineTo(p);
			return polylineCrossesPolyline_(polyline_a, polyline_b, tolerance,
					progress_tracker);
		}

		// Treat env_b as area

		SegmentIterator seg_iter_a = polyline_a.querySegmentIterator();
		Envelope2D env_b_inflated = new Envelope2D(), env_b_deflated = new Envelope2D();
		env_b_deflated.setCoords(env_b);
		env_b_inflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);
		env_b_inflated.inflate(tolerance, tolerance);

		boolean b_interior = false, b_exterior = false;
		Envelope2D env_segment_a = new Envelope2D();
		Envelope2D env_inter = new Envelope2D();

		while (seg_iter_a.nextPath()) {
			while (seg_iter_a.hasNextSegment()) {
				Segment segment_a = seg_iter_a.nextSegment();
				segment_a.queryEnvelope2D(env_segment_a);

				if (!b_exterior) {
					if (!env_b_inflated.contains(env_segment_a))
						b_exterior = true;
				}

				if (!b_interior) {
					env_inter.setCoords(env_b_deflated);
					env_inter.intersect(env_segment_a);

					if (!env_inter.isEmpty()
							&& (env_inter.getHeight() > tolerance || env_inter
									.getWidth() > tolerance))
						b_interior = true;
				}

				if (b_interior && b_exterior)
					return true;
			}
		}

		return false;
	}

	// Returns true if multipoint_a equals multipoint_b.
	private static boolean multiPointEqualsMultiPoint_(MultiPoint multipoint_a,
			MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		if (!envelopeEqualsEnvelope_(env_a, env_b, tolerance, progress_tracker))
			return false;

		if (multiPointExactlyEqualsMultiPoint_(multipoint_a, multipoint_b,
				tolerance, progress_tracker))
			return true;

		return multiPointCoverageMultiPoint_(multipoint_a, multipoint_b,
				tolerance, false, true, false, progress_tracker);
	}

	// Returns true if multipoint_a is disjoint from multipoint_b.
	private static boolean multiPointDisjointMultiPoint_(
			MultiPoint multipoint_a, MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		return !multiPointIntersectsMultiPoint_(multipoint_a, multipoint_b,
				tolerance, progress_tracker);
	}

	// Returns true if multipoint_a overlaps multipoint_b.
	private static boolean multiPointOverlapsMultiPoint_(
			MultiPoint multipoint_a, MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		return multiPointCoverageMultiPoint_(multipoint_a, multipoint_b,
				tolerance, false, false, true, progress_tracker);
	}

	// Returns true if multipoint_a contains multipoint_b.
	private static boolean multiPointContainsMultiPoint_(
			MultiPoint multipoint_a, MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		return multiPointCoverageMultiPoint_(multipoint_b, multipoint_a,
				tolerance, true, false, false, progress_tracker);
	}

	private static boolean multiPointContainsMultiPointBrute_(
			MultiPoint multipoint_a, MultiPoint multipoint_b, double tolerance) {
		double tolerance_sq = tolerance * tolerance;
		Point2D pt_a = new Point2D();
		Point2D pt_b = new Point2D();

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			multipoint_b.getXY(i, pt_b);
			boolean b_contained = false;

			for (int j = 0; j < multipoint_a.getPointCount(); j++) {
				multipoint_a.getXY(j, pt_a);

				if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance_sq) {
					b_contained = true;
					break;
				}
			}

			if (!b_contained)
				return false;
		}

		return true;
	}

	// Returns true if multipoint_a equals point_b.
	static boolean multiPointEqualsPoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		point_b.queryEnvelope2D(env_b);
		return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
				progress_tracker);
	}

	// Returns true if multipoint_a is disjoint from point_b.
	private static boolean multiPointDisjointPoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		Point2D pt_b = point_b.getXY();
		return multiPointDisjointPointImpl_(multipoint_a, pt_b, tolerance,
				progress_tracker);
	}

	// Returns true if multipoint_a is within point_b.
	private static boolean multiPointWithinPoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		return multiPointEqualsPoint_(multipoint_a, point_b, tolerance,
				progress_tracker);
	}

	// Returns true if multipoint_a contains point_b.
	private static boolean multiPointContainsPoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, ProgressTracker progress_tracker) {
		return !multiPointDisjointPoint_(multipoint_a, point_b, tolerance,
				progress_tracker);
	}

	// Returns true if multipoint_a equals envelope_b.
	private static boolean multiPointEqualsEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (env_b.getHeight() > tolerance || env_b.getWidth() > tolerance)
			return false;

		// only true if all the points of the multi_point degenerate to a point
		// equal to the envelope
		return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
				progress_tracker);
	}

	// Returns true if multipoint_a is disjoint from envelope_b.
	private static boolean multiPointDisjointEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		Envelope2D env_b_inflated = new Envelope2D();
		env_b_inflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);
		Point2D pt_a = new Point2D();

		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, pt_a);

			if (!env_b_inflated.contains(pt_a))
				continue;

			return false;
		}

		return true;
	}

	// Returns true if multipoint_a touches envelope_b.
	private static boolean multiPointTouchesEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_b = new Envelope2D(), env_b_inflated = new Envelope2D(), env_b_deflated = new Envelope2D();
		envelope_b.queryEnvelope2D(env_b);

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // there are no boundaries to intersect

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// treat
																				// as
																				// line

			Point2D pt_a = new Point2D();
			boolean b_boundary = false;

			env_b_inflated.setCoords(env_b);
			env_b_deflated.setCoords(env_b);
			env_b_inflated.inflate(tolerance, tolerance);
			if (env_b.getHeight() > tolerance)
				env_b_deflated.inflate(0, -tolerance);
			else
				env_b_deflated.inflate(-tolerance, 0);

			for (int i = 0; i < multipoint_a.getPointCount(); i++) {
				multipoint_a.getXY(i, pt_a);

				if (!env_b_inflated.contains(pt_a))
					continue;

				if (env_b.getHeight() > tolerance) {
					if (pt_a.y > env_b_deflated.ymin
							&& pt_a.y < env_b_deflated.ymax)
						return false;

					b_boundary = true;
				} else {
					if (pt_a.x > env_b_deflated.xmin
							&& pt_a.x < env_b_deflated.xmax)
						return false;

					b_boundary = true;
				}
			}

			return b_boundary;
		}

		// treat as area
		env_b_inflated.setCoords(env_b);
		env_b_deflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);
		env_b_deflated.inflate(-tolerance, -tolerance);

		Point2D pt_a = new Point2D();
		boolean b_boundary = false;

		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, pt_a);

			if (!env_b_inflated.contains(pt_a))
				continue;

			if (env_b_deflated.containsExclusive(pt_a))
				return false;

			b_boundary = true;
		}

		return b_boundary;
	}

	// Returns true if multipoint_a is within envelope_b.
	private static boolean multiPointWithinEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (!envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
					progress_tracker); // treat as point

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// treat
																				// as
																				// line

			boolean b_interior = false;

			Envelope2D env_b_deflated = new Envelope2D(), env_b_inflated = new Envelope2D();
			env_b_deflated.setCoords(env_b);
			env_b_inflated.setCoords(env_b);

			if (env_b.getHeight() > tolerance)
				env_b_deflated.inflate(0, -tolerance);
			else
				env_b_deflated.inflate(-tolerance, 0);

			env_b_inflated.inflate(tolerance, tolerance);

			Point2D pt_a = new Point2D();

			for (int i = 0; i < multipoint_a.getPointCount(); i++) {
				multipoint_a.getXY(i, pt_a);

				if (!env_b_inflated.contains(pt_a))
					return false;

				if (env_b.getHeight() > tolerance) {
					if (pt_a.y > env_b_deflated.ymin
							&& pt_a.y < env_b_deflated.ymax)
						b_interior = true;
				} else {
					if (pt_a.x > env_b_deflated.xmin
							&& pt_a.x < env_b_deflated.xmax)
						b_interior = true;
				}
			}

			return b_interior;
		}

		// treat as area

		boolean b_interior = false;

		Envelope2D env_b_deflated = new Envelope2D(), env_b_inflated = new Envelope2D();
		env_b_deflated.setCoords(env_b);
		env_b_inflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);
		env_b_inflated.inflate(tolerance, tolerance);

		Point2D pt_a = new Point2D();

		// we loop to find a proper interior intersection (i.e. something inside
		// instead of just on the boundary)
		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, pt_a);

			if (!env_b_inflated.contains(pt_a))
				return false;

			if (env_b_deflated.containsExclusive(pt_a))
				b_interior = true;
		}

		return b_interior;
	}

	// Returns true if multipoint_a contains envelope_b.
	private static boolean multiPointContainsEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		if (env_b.getHeight() > tolerance || env_b.getWidth() > tolerance)
			return false;

		Point2D pt_b = envelope_b.getCenterXY();
		return !multiPointDisjointPointImpl_(multipoint_a, pt_b, tolerance,
				progress_tracker);
	}

	// Returns true if multipoint_a crosses envelope_b.
	static boolean multiPointCrossesEnvelope_(MultiPoint multipoint_a,
			Envelope envelope_b, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		envelope_b.queryEnvelope2D(env_b);

		if (envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false;

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// treat
																				// as
																				// line
			Envelope2D env_b_deflated = new Envelope2D();
			Envelope2D env_b_inflated = new Envelope2D();
			env_b_deflated.setCoords(env_b);

			if (env_b.getHeight() > tolerance)
				env_b_deflated.inflate(0, -tolerance);
			else
				env_b_deflated.inflate(-tolerance, 0);

			env_b_inflated.setCoords(env_b);
			env_b_inflated.inflate(tolerance, tolerance);

			Point2D pt_a = new Point2D();
			boolean b_interior = false, b_exterior = false;

			for (int i = 0; i < multipoint_a.getPointCount(); i++) {
				multipoint_a.getXY(i, pt_a);

				if (!b_interior) {
					if (env_b.getHeight() > tolerance) {
						if (pt_a.y > env_b_deflated.ymin
								&& pt_a.y < env_b_deflated.ymax)
							b_interior = true;
					} else {
						if (pt_a.x > env_b_deflated.xmin
								&& pt_a.x < env_b_deflated.xmax)
							b_interior = true;
					}
				}

				if (!b_exterior && !env_b_inflated.contains(pt_a))
					b_exterior = true;

				if (b_interior && b_exterior)
					return true;
			}

			return false;
		}

		Envelope2D env_b_deflated = new Envelope2D(), env_b_inflated = new Envelope2D();
		env_b_deflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);
		assert (!env_b_deflated.isEmpty());

		env_b_inflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);

		Point2D pt_a = new Point2D();
		boolean b_interior = false, b_exterior = false;

		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, pt_a);

			if (!b_interior && env_b_deflated.containsExclusive(pt_a))
				b_interior = true;

			if (!b_exterior && !env_b_inflated.contains(pt_a))
				b_exterior = true;

			if (b_interior && b_exterior)
				return true;
		}

		return false;
	}

	// Returns true if pt_a equals pt_b.
	private static boolean pointEqualsPoint_(Point2D pt_a, Point2D pt_b,
			double tolerance, ProgressTracker progress_tracker) {
		if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance * tolerance)
			return true;

		return false;
	}

	// Returns true if pt_a is disjoint from pt_b.
	private static boolean pointDisjointPoint_(Point2D pt_a, Point2D pt_b,
			double tolerance, ProgressTracker progress_tracker) {
		if (Point2D.sqrDistance(pt_a, pt_b) > tolerance * tolerance)
			return true;

		return false;
	}

	// Returns true if pt_a contains pt_b.
	private static boolean pointContainsPoint_(Point2D pt_a, Point2D pt_b,
			double tolerance, ProgressTracker progress_tracker) {
		return pointEqualsPoint_(pt_a, pt_b, tolerance, progress_tracker);
	}

	// Returns true if pt_a equals enve_b.
	private static boolean pointEqualsEnvelope_(Point2D pt_a, Envelope2D env_b,
			double tolerance, ProgressTracker progress_tracker) {
		Envelope2D env_a = new Envelope2D();
		env_a.setCoords(pt_a);
		return envelopeEqualsEnvelope_(env_a, env_b, tolerance,
				progress_tracker);
	}

	// Returns true if pt_a is disjoint from env_b.
	static boolean pointDisjointEnvelope_(Point2D pt_a, Envelope2D env_b,
			double tolerance, ProgressTracker progress_tracker) {
		Envelope2D env_b_inflated = new Envelope2D();
		env_b_inflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);
		return !env_b_inflated.contains(pt_a);
	}

	// Returns true if pt_a touches env_b.
	private static boolean pointTouchesEnvelope_(Point2D pt_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // when treates as a point, points cannot touch points

		Envelope2D env_b_inflated = new Envelope2D(), env_b_deflated = new Envelope2D();

		env_b_inflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);

		if (!env_b_inflated.contains(pt_a))
			return false;

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {
			env_b_deflated.setCoords(env_b);

			if (env_b.getHeight() > tolerance)
				env_b_deflated.inflate(0, -tolerance);
			else
				env_b_deflated.inflate(-tolerance, 0);

			if (env_b.getHeight() > tolerance) {
				if (pt_a.y > env_b_deflated.ymin
						&& pt_a.y < env_b_deflated.ymax)
					return false;
			} else {
				if (pt_a.x > env_b_deflated.xmin
						&& pt_a.x < env_b_deflated.xmax)
					return false;
			}

			return true;
		}

		env_b_deflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);

		if (env_b_deflated.containsExclusive(pt_a))
			return false;

		return true;
	}

	// Returns true if pt_a is within env_b.
	private static boolean pointWithinEnvelope_(Point2D pt_a, Envelope2D env_b,
			double tolerance, ProgressTracker progress_tracker) {
		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance) {
			// assert(env_b_inflated.contains(pt_a)); // should contain if we
			// got to here (i.e. not disjoint)
			return true;
		}

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// treat
																				// as
																				// line
			Envelope2D env_b_deflated = new Envelope2D();
			env_b_deflated.setCoords(env_b);

			if (env_b.getHeight() > tolerance)
				env_b_deflated.inflate(0, -tolerance);
			else
				env_b_deflated.inflate(-tolerance, 0);

			boolean b_interior = false;

			if (env_b.getHeight() > tolerance) {
				if (pt_a.y > env_b_deflated.ymin
						&& pt_a.y < env_b_deflated.ymax)
					b_interior = true;
			} else {
				if (pt_a.x > env_b_deflated.xmin
						&& pt_a.x < env_b_deflated.xmax)
					b_interior = true;
			}

			return b_interior;
		}

		// treat as area

		Envelope2D env_b_deflated = new Envelope2D();
		env_b_deflated.setCoords(env_b);
		env_b_deflated.inflate(-tolerance, -tolerance);
		return env_b_deflated.containsExclusive(pt_a);
	}

	// Returns true if pt_a contains env_b.
	private static boolean pointContainsEnvelope_(Point2D pt_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		return pointEqualsEnvelope_(pt_a, env_b, tolerance, progress_tracker);
	}

	// Returns true if env_a equals env_b.
	private static boolean envelopeEqualsEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		return envelopeInfContainsEnvelope_(env_a, env_b, tolerance)
				&& envelopeInfContainsEnvelope_(env_b, env_a, tolerance);
	}

	// Returns true if env_a is disjoint from env_b.
	static boolean envelopeDisjointEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		Envelope2D env_b_inflated = new Envelope2D();
		env_b_inflated.setCoords(env_b);
		env_b_inflated.inflate(tolerance, tolerance);
		return !env_a.isIntersecting(env_b_inflated);
	}

	// Returns true if env_a touches env_b.
	private static boolean envelopeTouchesEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		if (env_a.getHeight() <= tolerance && env_a.getWidth() <= tolerance) {// treat
																				// env_a
																				// as
																				// point
			Point2D pt_a = env_a.getCenter();
			return pointTouchesEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);
		}

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance) {// treat
																				// env_b
																				// as
																				// point
			Point2D pt_b = env_b.getCenter();
			return pointTouchesEnvelope_(pt_b, env_a, tolerance,
					progress_tracker);
		}

		Envelope2D _env_a;
		Envelope2D _env_b;

		if (env_a.getHeight() > tolerance
				&& env_a.getWidth() > tolerance
				&& (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance)) {
			// swap a and b
			_env_a = env_b;
			_env_b = env_a;
		} else {
			_env_a = env_a;
			_env_b = env_b;
		}

		if (_env_a.getHeight() <= tolerance || _env_a.getWidth() <= tolerance) {// treat
																				// env_a
																				// as
																				// line

			if (_env_b.getHeight() <= tolerance
					|| _env_b.getWidth() <= tolerance) {// treat env_b as line

				Line line_a = new Line(), line_b = new Line();
				double[] scalars_a = new double[2];
				double[] scalars_b = new double[2];
				Point2D pt = new Point2D();
				_env_a.queryLowerLeft(pt);
				line_a.setStartXY(pt);
				_env_a.queryUpperRight(pt);
				line_a.setEndXY(pt);
				_env_b.queryLowerLeft(pt);
				line_b.setStartXY(pt);
				_env_b.queryUpperRight(pt);
				line_b.setEndXY(pt);

				line_a.intersect(line_b, null, scalars_a, scalars_b, tolerance);
				int count = line_a.intersect(line_b, null, null, null,
						tolerance);

				if (count != 1)
					return false;

				return scalars_a[0] == 0.0 || scalars_a[1] == 1.0
						|| scalars_b[0] == 0.0 || scalars_b[1] == 1.0;
			}

			// treat env_b as area

			Envelope2D env_b_deflated = new Envelope2D(), env_inter = new Envelope2D();
			env_b_deflated.setCoords(_env_b);
			env_b_deflated.inflate(-tolerance, -tolerance);
			env_inter.setCoords(env_b_deflated);
			env_inter.intersect(_env_a);

			if (!env_inter.isEmpty()
					&& (env_inter.getHeight() > tolerance || env_inter
							.getWidth() > tolerance))
				return false;

			assert (!envelopeDisjointEnvelope_(_env_a, _env_b, tolerance,
					progress_tracker));
			return true; // we already know they intersect within a tolerance
		}

		Envelope2D env_inter = new Envelope2D();
		env_inter.setCoords(_env_a);
		env_inter.intersect(_env_b);

		if (!env_inter.isEmpty() && env_inter.getHeight() > tolerance
				&& env_inter.getWidth() > tolerance)
			return false;

		return true; // we already know they intersect within a tolerance
	}

	// Returns true if env_a overlaps env_b.
	private static boolean envelopeOverlapsEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		if (envelopeInfContainsEnvelope_(env_a, env_b, tolerance)
				|| envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_a.getHeight() <= tolerance && env_a.getWidth() <= tolerance)
			return false; // points cannot overlap

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // points cannot overlap

		if (env_a.getHeight() <= tolerance || env_a.getWidth() <= tolerance) {// treat
																				// env_a
																				// as
																				// a
																				// line

			if (env_b.getHeight() > tolerance && env_b.getWidth() > tolerance)
				return false; // lines cannot overlap areas

			// treat both as lines

			Line line_a = new Line(), line_b = new Line();
			double[] scalars_a = new double[2];
			double[] scalars_b = new double[2];
			Point2D pt = new Point2D();
			env_a.queryLowerLeft(pt);
			line_a.setStartXY(pt);
			env_a.queryUpperRight(pt);
			line_a.setEndXY(pt);
			env_b.queryLowerLeft(pt);
			line_b.setStartXY(pt);
			env_b.queryUpperRight(pt);
			line_b.setEndXY(pt);

			line_a.intersect(line_b, null, scalars_a, scalars_b, tolerance);
			int count = line_a.intersect(line_b, null, null, null, tolerance);

			if (count != 2)
				return false;

			return (scalars_a[0] > 0.0 || scalars_a[1] < 1.0)
					&& (scalars_b[0] > 0.0 || scalars_b[1] < 1.0);
		}

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance)
			return false; // lines cannot overlap areas

		// treat both as areas

		Envelope2D env_inter = new Envelope2D();
		env_inter.setCoords(env_a);
		env_inter.intersect(env_b);

		if (env_inter.isEmpty())
			return false;

		if (env_inter.getHeight() <= tolerance
				|| env_inter.getWidth() <= tolerance)
			return false; // not an area

		assert (!envelopeInfContainsEnvelope_(env_inter, env_a, tolerance) && !envelopeInfContainsEnvelope_(
				env_inter, env_b, tolerance));

		return true;
	}

	// Returns true if env_a contains env_b.
	private static boolean envelopeContainsEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		if (!envelopeInfContainsEnvelope_(env_a, env_b, tolerance))
			return false;

		if (env_a.getHeight() <= tolerance && env_a.getWidth() <= tolerance) {
			Point2D pt_a = env_a.getCenter();
			return pointWithinEnvelope_(pt_a, env_b, tolerance,
					progress_tracker);
		}

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance) {
			Point2D pt_b = env_b.getCenter();
			return pointWithinEnvelope_(pt_b, env_a, tolerance,
					progress_tracker);
		}

		if (env_a.getHeight() <= tolerance || env_a.getWidth() <= tolerance)
			return envelopeInfContainsEnvelope_(env_a, env_b, tolerance); // treat
																			// env_b
																			// as
																			// line

		// treat env_a as area

		if (env_b.getHeight() <= tolerance || env_b.getWidth() <= tolerance) {// treat
																				// env_b
																				// as
																				// line

			Envelope2D env_a_deflated = new Envelope2D();
			env_a_deflated.setCoords(env_a);
			env_a_deflated.inflate(-tolerance, -tolerance);

			if (env_a_deflated.containsExclusive(env_b))
				return true;

			Envelope2D env_inter = new Envelope2D();
			env_inter.setCoords(env_a_deflated);
			env_inter.intersect(env_b);

			if (env_inter.isEmpty()
					|| (env_inter.getHeight() <= tolerance && env_inter
							.getWidth() <= tolerance))
				return false;

			return true;
		}

		return envelopeInfContainsEnvelope_(env_a, env_b, tolerance);
	}

	// Returns true if env_a crosses env_b.
	private static boolean envelopeCrossesEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		if (envelopeInfContainsEnvelope_(env_a, env_b, tolerance)
				|| envelopeInfContainsEnvelope_(env_b, env_a, tolerance))
			return false;

		if (env_a.getHeight() <= tolerance && env_a.getWidth() <= tolerance)
			return false; // points cannot cross

		if (env_b.getHeight() <= tolerance && env_b.getWidth() <= tolerance)
			return false; // points cannot cross

		if (env_b.getHeight() > tolerance && env_b.getWidth() > tolerance) {
			if (env_a.getHeight() > tolerance && env_a.getWidth() > tolerance)
				return false; // areas cannot cross
		}

		Envelope2D _env_a;
		Envelope2D _env_b;

		if (env_a.getHeight() > tolerance && env_a.getWidth() > tolerance) {
			// swap b and a
			_env_a = env_b;
			_env_b = env_a;
		} else {
			_env_a = env_a;
			_env_b = env_b;
		}

		if (_env_b.getHeight() > tolerance && _env_b.getWidth() > tolerance) {// treat
																				// env_b
																				// as
																				// an
																				// area
																				// (env_a
																				// as
																				// a
																				// line);

			Envelope2D env_inter = new Envelope2D(), env_b_deflated = new Envelope2D();
			env_b_deflated.setCoords(_env_b);
			env_b_deflated.inflate(-tolerance, -tolerance);
			env_inter.setCoords(env_b_deflated);
			env_inter.intersect(_env_a);

			if (env_inter.isEmpty())
				return false;

			if (env_inter.getHeight() <= tolerance
					&& env_inter.getWidth() <= tolerance)
				return false; // not a line

			assert (!envelopeInfContainsEnvelope_(env_inter, _env_a, tolerance));
			return true;
		}

		// treat both as lines

		Line line_a = new Line(), line_b = new Line();
		double[] scalars_a = new double[2];
		double[] scalars_b = new double[2];
		Point2D pt = new Point2D();
		_env_a.queryLowerLeft(pt);
		line_a.setStartXY(pt);
		_env_a.queryUpperRight(pt);
		line_a.setEndXY(pt);
		_env_b.queryLowerLeft(pt);
		line_b.setStartXY(pt);
		_env_b.queryUpperRight(pt);
		line_b.setEndXY(pt);

		line_a.intersect(line_b, null, scalars_a, scalars_b, tolerance);
		int count = line_a.intersect(line_b, null, null, null, tolerance);

		if (count != 1)
			return false;

		return scalars_a[0] > 0.0 && scalars_a[1] < 1.0 && scalars_b[0] > 0.0
				&& scalars_b[1] < 1.0;
	}

	// Returns true if polygon_a is disjoint from multipath_b.
	private static boolean polygonDisjointMultiPath_(Polygon polygon_a,
			MultiPath multipath_b, double tolerance,
			ProgressTracker progress_tracker) {
        Point2D pt_a, pt_b;
        Envelope2D env_a_inf = new Envelope2D(), env_b_inf = new Envelope2D();

        MultiPathImpl multi_path_impl_a = (MultiPathImpl)polygon_a._getImpl();
        MultiPathImpl multi_path_impl_b = (MultiPathImpl)multipath_b._getImpl();

        PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(multi_path_impl_a, multi_path_impl_b, tolerance, true);

        if (!intersector.next())
            return true; // no rings intersect

        boolean b_intersects = linearPathIntersectsLinearPath_(polygon_a, multipath_b, tolerance);

        if (b_intersects)
            return false;

        Polygon pa = null;
        Polygon p_polygon_a = polygon_a;

        Polygon pb = null;
        Polygon p_polygon_b = null;

        if (multipath_b.getType().value() == Geometry.GeometryType.Polygon)
            p_polygon_b = (Polygon)multipath_b;

        boolean b_checked_polygon_a_quad_tree = false;
        boolean b_checked_polygon_b_quad_tree = false;

        do
        {
            int path_a = intersector.getRedElement();
            int path_b = intersector.getBlueElement();

            pt_b = multipath_b.getXY(multipath_b.getPathStart(path_b));
            env_a_inf.setCoords(intersector.getRedEnvelope());
            env_a_inf.inflate(tolerance, tolerance);

            if (env_a_inf.contains(pt_b))
            {
                PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(p_polygon_a, pt_b, 0.0);

                if (result != PolygonUtils.PiPResult.PiPOutside)
                    return false;
            }

            if (multipath_b.getType().value() == Geometry.GeometryType.Polygon)
            {
                pt_a = polygon_a.getXY(polygon_a.getPathStart(path_a));
                env_b_inf.setCoords(intersector.getBlueEnvelope());
                env_b_inf.inflate(tolerance, tolerance);

                if (env_b_inf.contains(pt_a))
                {
                    PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(p_polygon_b, pt_a, 0.0);

                    if (result != PolygonUtils.PiPResult.PiPOutside)
                        return false;
                }
            }

            if (!b_checked_polygon_a_quad_tree) {
                if (PointInPolygonHelper.quadTreeWillHelp(polygon_a, multipath_b.getPathCount() - 1) && (multi_path_impl_a._getAccelerators() == null || multi_path_impl_a._getAccelerators().getQuadTree() == null)) {
                    pa = new Polygon();
                    polygon_a.copyTo(pa);
                    ((MultiPathImpl) pa._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                    p_polygon_a = pa;
                } else {
                    p_polygon_a = polygon_a;
                }

                b_checked_polygon_a_quad_tree = true;
            }

            if (multipath_b.getType().value() == Geometry.GeometryType.Polygon)
            {
                if (!b_checked_polygon_b_quad_tree) {
                    Polygon polygon_b = (Polygon) multipath_b;
                    if (PointInPolygonHelper.quadTreeWillHelp(polygon_b, polygon_a.getPathCount() - 1) && (multi_path_impl_b._getAccelerators() == null || multi_path_impl_b._getAccelerators().getQuadTree() == null)) {
                        pb = new Polygon();
                        polygon_b.copyTo(pb);
                        ((MultiPathImpl) pb._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                        p_polygon_b = pb;
                    } else {
                        p_polygon_b = (Polygon) multipath_b;
                    }

                    b_checked_polygon_b_quad_tree = true;
                }
            }

        } while (intersector.next());

        return true;
	}

	// Returns true if env_a inflated contains env_b.
	private static boolean envelopeInfContainsEnvelope_(Envelope2D env_a,
			Envelope2D env_b, double tolerance) {
		Envelope2D env_a_inflated = new Envelope2D();
		env_a_inflated.setCoords(env_a);
		env_a_inflated.inflate(tolerance, tolerance);
		return env_a_inflated.contains(env_b);
	}

	// Returns true if a coordinate of envelope A is outside of envelope B.
	private static boolean interiorEnvExteriorEnv_(Envelope2D env_a,
			Envelope2D env_b, double tolerance) {
		Envelope2D envBInflated = new Envelope2D();
		envBInflated.setCoords(env_b);
		envBInflated.inflate(tolerance, tolerance);
		Point2D pt = new Point2D();

		env_a.queryLowerLeft(pt);
		if (!envBInflated.contains(pt))
			return true;

		env_a.queryLowerRight(pt);
		if (!envBInflated.contains(pt))
			return true;

		env_a.queryUpperLeft(pt);
		if (!envBInflated.contains(pt))
			return true;

		env_a.queryUpperRight(pt);
		if (!envBInflated.contains(pt))
			return true;

		assert (envBInflated.contains(env_a));
		return false;
	}

	// Returns true if the points in each path of multipathA are the same as
	// those in multipathB, within a tolerance, and in the same order.
	private static boolean multiPathExactlyEqualsMultiPath_(
			MultiPath multipathA, MultiPath multipathB, double tolerance,
			ProgressTracker progress_tracker) {
		if (multipathA.getPathCount() != multipathB.getPathCount()
				|| multipathA.getPointCount() != multipathB.getPointCount())
			return false;

		Point2D ptA = new Point2D(), ptB = new Point2D();
		boolean bAllPointsEqual = true;
		double tolerance_sq = tolerance * tolerance;

		for (int ipath = 0; ipath < multipathA.getPathCount(); ipath++) {
			if (multipathA.getPathEnd(ipath) != multipathB.getPathEnd(ipath)) {
				bAllPointsEqual = false;
				break;
			}

			for (int i = multipathA.getPathStart(ipath); i < multipathB
					.getPathEnd(ipath); i++) {
				multipathA.getXY(i, ptA);
				multipathB.getXY(i, ptB);

				if (Point2D.sqrDistance(ptA, ptB) > tolerance_sq) {
					bAllPointsEqual = false;
					break;
				}
			}

			if (!bAllPointsEqual)
				break;
		}

		if (!bAllPointsEqual)
			return false;

		return true;
	}

	// Returns true if the points of multipoint_a are the same as those in
	// multipoint_b, within a tolerance, and in the same order.
	private static boolean multiPointExactlyEqualsMultiPoint_(
			MultiPoint multipoint_a, MultiPoint multipoint_b, double tolerance,
			ProgressTracker progress_tracker) {
		if (multipoint_a.getPointCount() != multipoint_b.getPointCount())
			return false;

		Point2D ptA = new Point2D(), ptB = new Point2D();
		boolean bAllPointsEqual = true;
		double tolerance_sq = tolerance * tolerance;

		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, ptA);
			multipoint_b.getXY(i, ptB);

			if (Point2D.sqrDistance(ptA, ptB) > tolerance_sq) {
				bAllPointsEqual = false;
				break;
			}
		}

		if (!bAllPointsEqual)
			return false;

		return true;
	}

	// By default this will perform the within operation if bEquals is false.
	// Otherwise it will do equals.
	private static boolean multiPointCoverageMultiPoint_(
			MultiPoint _multipointA, MultiPoint _multipointB, double tolerance,
			boolean bPerformWithin, boolean bPerformEquals,
			boolean bPerformOverlaps, ProgressTracker progress_tracker) {
		boolean bPerformContains = false;
		MultiPoint multipoint_a;
		MultiPoint multipoint_b;

		if (_multipointA.getPointCount() > _multipointB.getPointCount()) {
			if (bPerformWithin) {
				bPerformWithin = false;
				bPerformContains = true;
			}

			multipoint_a = _multipointB;
			multipoint_b = _multipointA;
		} else {
			multipoint_a = _multipointA;
			multipoint_b = _multipointB;
		}

		AttributeStreamOfInt8 contained = null;

		if (bPerformEquals || bPerformOverlaps || bPerformContains) {
			contained = new AttributeStreamOfInt8(multipoint_b.getPointCount());

			for (int i = 0; i < multipoint_b.getPointCount(); i++)
				contained.write(i, (byte) 0);
		}

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		Point2D ptA = new Point2D();
		Point2D ptB = new Point2D();

		boolean bWithin = true; // starts off true by default

		QuadTreeImpl quadTreeB = InternalUtils.buildQuadTree(
				(MultiPointImpl) (multipoint_b._getImpl()), envInter);
		QuadTreeImpl.QuadTreeIteratorImpl qtIterB = quadTreeB.getIterator();
		double tolerance_sq = tolerance * tolerance;

		for (int vertex_a = 0; vertex_a < multipoint_a.getPointCount(); vertex_a++) {
			multipoint_a.getXY(vertex_a, ptA);

			if (!envInter.contains(ptA)) {
				if (bPerformEquals || bPerformWithin)
					return false;
				else {
					bWithin = false;
					continue;
				}
			}

			boolean bPtACovered = false;
			env_a.setCoords(ptA.x, ptA.y, ptA.x, ptA.y);
			qtIterB.resetIterator(env_a, tolerance);
			for (int elementHandleB = qtIterB.next(); elementHandleB != -1; elementHandleB = qtIterB
					.next()) {
				int vertex_b = quadTreeB.getElement(elementHandleB);
				multipoint_b.getXY(vertex_b, ptB);

				if (Point2D.sqrDistance(ptA, ptB) <= tolerance_sq) {
					if (bPerformEquals || bPerformOverlaps || bPerformContains)
						contained.write(vertex_b, (byte) 1);

					bPtACovered = true;

					if (bPerformWithin)
						break;
				}
			}

			if (!bPtACovered) {
				bWithin = false;

				if (bPerformEquals || bPerformWithin)
					return false;
			}
		}

		if (bPerformOverlaps && bWithin)
			return false;

		if (bPerformWithin)
			return true;

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			if (contained.read(i) == 1) {
				if (bPerformOverlaps)
					return true;
			} else {
				if (bPerformEquals || bPerformContains)
					return false;
			}
		}

		if (bPerformOverlaps)
			return false;

		return true;
	}

	// Returns true if multipoint_a intersects multipoint_b.
	private static boolean multiPointIntersectsMultiPoint_(
			MultiPoint _multipointA, MultiPoint _multipointB, double tolerance,
			ProgressTracker progress_tracker) {
		MultiPoint multipoint_a;
		MultiPoint multipoint_b;

		if (_multipointA.getPointCount() > _multipointB.getPointCount()) {
			multipoint_a = _multipointB;
			multipoint_b = _multipointA;
		} else {
			multipoint_a = _multipointA;
			multipoint_b = _multipointB;
		}

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		Point2D ptA = new Point2D();
		Point2D ptB = new Point2D();
		double tolerance_sq = tolerance * tolerance;

		QuadTreeImpl quadTreeB = InternalUtils.buildQuadTree(
				(MultiPointImpl) (multipoint_b._getImpl()), envInter);
		QuadTreeImpl.QuadTreeIteratorImpl qtIterB = quadTreeB.getIterator();

		for (int vertex_a = 0; vertex_a < multipoint_a.getPointCount(); vertex_a++) {
			multipoint_a.getXY(vertex_a, ptA);

			if (!envInter.contains(ptA))
				continue;

			env_a.setCoords(ptA.x, ptA.y, ptA.x, ptA.y);
			qtIterB.resetIterator(env_a, tolerance);

			for (int elementHandleB = qtIterB.next(); elementHandleB != -1; elementHandleB = qtIterB
					.next()) {
				int vertex_b = quadTreeB.getElement(elementHandleB);
				multipoint_b.getXY(vertex_b, ptB);

				if (Point2D.sqrDistance(ptA, ptB) <= tolerance_sq)
					return true;
			}
		}

		return false;
	}

	// Returns true if multipathA equals multipathB.
	private static boolean linearPathEqualsLinearPath_(MultiPath multipathA,
			MultiPath multipathB, double tolerance, boolean bEnforceOrientation) {
		return linearPathWithinLinearPath_(multipathA, multipathB, tolerance, bEnforceOrientation)
				&& linearPathWithinLinearPath_(multipathB, multipathA,
						tolerance, bEnforceOrientation);
	}

	// Returns true if the segments of multipathA are within the segments of
	// multipathB.
	private static boolean linearPathWithinLinearPath_(MultiPath multipathA,
			MultiPath multipathB, double tolerance, boolean bEnforceOrientation) {
		boolean bWithin = true;
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		int ievent = 0;
		AttributeStreamOfInt32 eventIndices = new AttributeStreamOfInt32(0);
		RelationalOperations relOps = new RelationalOperations();
		OverlapComparer overlapComparer = new OverlapComparer(relOps);
		OverlapEvent overlapEvent;

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipathA.queryEnvelope2D(env_a);
		multipathB.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		SegmentIteratorImpl segIterA = ((MultiPathImpl) multipathA._getImpl())
				.querySegmentIterator();
		SegmentIteratorImpl segIterB = ((MultiPathImpl) multipathB._getImpl())
				.querySegmentIterator();

		QuadTreeImpl qtB = null;
		QuadTreeImpl quadTreeB = null;
        QuadTreeImpl quadTreePathsB = null;

		GeometryAccelerators accel = ((MultiPathImpl) multipathB._getImpl())
				._getAccelerators();

		if (accel != null) {
			quadTreeB = accel.getQuadTree();
            quadTreePathsB = accel.getQuadTreeForPaths();
			if (quadTreeB == null) {
				qtB = InternalUtils.buildQuadTree(
						(MultiPathImpl) multipathB._getImpl(), envInter);
				quadTreeB = qtB;
			}
		} else {
			qtB = InternalUtils.buildQuadTree(
					(MultiPathImpl) multipathB._getImpl(), envInter);
			quadTreeB = qtB;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterB = quadTreeB.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsB = null;
        if (quadTreePathsB != null)
            qtIterPathsB = quadTreePathsB.getIterator();

		while (segIterA.nextPath()) {
			while (segIterA.hasNextSegment()) {
				boolean bStringOfSegmentAsCovered = false;

				Segment segmentA = segIterA.nextSegment();
				segmentA.queryEnvelope2D(env_a);

				if (!env_a.isIntersecting(envInter)) {
					return false; // bWithin = false
				}

                if (qtIterPathsB != null) {
                    qtIterPathsB.resetIterator(env_a, tolerance);

                    if (qtIterPathsB.next() == -1) {
                        bWithin = false;
                        return false;
                    }
                }

				double lengthA = segmentA.calculateLength2D();

				qtIterB.resetIterator(segmentA, tolerance);

				for (int elementHandleB = qtIterB.next(); elementHandleB != -1; elementHandleB = qtIterB
						.next()) {
					int vertex_b = quadTreeB.getElement(elementHandleB);
					segIterB.resetToVertex(vertex_b);
					Segment segmentB = segIterB.nextSegment();

					int result = segmentA.intersect(segmentB, null, scalarsA,
							scalarsB, tolerance);

					if (result == 2 && (!bEnforceOrientation || scalarsB[0] <= scalarsB[1])) {
						double scalar_a_0 = scalarsA[0];
						double scalar_a_1 = scalarsA[1];
						double scalar_b_0 = scalarsB[0];
						double scalar_b_1 = scalarsB[1];

						// Performance enhancement for nice cases where
						// localization occurs. Increment segIterA as far as we
						// can while the current segmentA is covered.
						if (scalar_a_0 * lengthA <= tolerance
								&& (1.0 - scalar_a_1) * lengthA <= tolerance) {
							bStringOfSegmentAsCovered = true;

							ievent = 0;
							eventIndices.resize(0);
							relOps.m_overlap_events.clear();

							int ivertex_a = segIterA.getStartPointIndex();
							boolean bSegmentACovered = true;

							while (bSegmentACovered) {// keep going while the
								// current segmentA is
								// covered.
								if (segIterA.hasNextSegment()) {
									segmentA = segIterA.nextSegment();
									lengthA = segmentA.calculateLength2D();

									result = segmentA.intersect(segmentB, null,
											scalarsA, scalarsB, tolerance);

									if (result == 2 && (!bEnforceOrientation || scalarsB[0] <= scalarsB[1])) {
										scalar_a_0 = scalarsA[0];
										scalar_a_1 = scalarsA[1];

										if (scalar_a_0 * lengthA <= tolerance
												&& (1.0 - scalar_a_1) * lengthA <= tolerance) {
											ivertex_a = segIterA
													.getStartPointIndex();
											continue;
										}
									}

									if (segIterB.hasNextSegment()) {
										segmentB = segIterB.nextSegment();
										result = segmentA.intersect(segmentB,
												null, scalarsA, scalarsB,
												tolerance);

										if (result == 2 && (!bEnforceOrientation || scalarsB[0] <= scalarsB[1])) {
											scalar_a_0 = scalarsA[0];
											scalar_a_1 = scalarsA[1];

											if (scalar_a_0 * lengthA <= tolerance
													&& (1.0 - scalar_a_1)
															* lengthA <= tolerance) {
												ivertex_a = segIterA
														.getStartPointIndex();
												continue;
											}
										}
									}
								}

								bSegmentACovered = false;
							}

							if (ivertex_a != segIterA.getStartPointIndex()) {
								segIterA.resetToVertex(ivertex_a);
								segIterA.nextSegment();
							}

							break;
						} else {
							int ivertex_a = segIterA.getStartPointIndex();
							int ipath_a = segIterA.getPathIndex();
							int ivertex_b = segIterB.getStartPointIndex();
							int ipath_b = segIterB.getPathIndex();

							overlapEvent = OverlapEvent.construct(ivertex_a,
									ipath_a, scalar_a_0, scalar_a_1, ivertex_b,
									ipath_b, scalar_b_0, scalar_b_1);
							relOps.m_overlap_events.add(overlapEvent);
							eventIndices.add(eventIndices.size());
						}
					}
				}

				if (bStringOfSegmentAsCovered) {
					continue; // no need to check that segmentA is covered
				}
				if (ievent == relOps.m_overlap_events.size()) {
					return false; // bWithin = false
				}

				if (eventIndices.size() - ievent > 1) {
					eventIndices.Sort(ievent, eventIndices.size(),
							overlapComparer);
				}

				double lastScalar = 0.0;

				for (int i = ievent; i < relOps.m_overlap_events.size(); i++) {
					overlapEvent = relOps.m_overlap_events.get(eventIndices
							.get(i));

					if (overlapEvent.m_scalar_a_0 < lastScalar
							&& overlapEvent.m_scalar_a_1 < lastScalar) {
						continue;
					}

					if (lengthA * (overlapEvent.m_scalar_a_0 - lastScalar) > tolerance) {
						return false; // bWithin = false
					} else {
						lastScalar = overlapEvent.m_scalar_a_1;

						if (lengthA * (1.0 - lastScalar) <= tolerance
								|| lastScalar == 1.0) {
							break;
						}
					}
				}

				if (lengthA * (1.0 - lastScalar) > tolerance) {
					return false; // bWithin = false
				}

				ievent = 0;
				eventIndices.resize(0);
				relOps.m_overlap_events.clear();
			}
		}

		return bWithin;
	}

	// Returns true if the segments of multipathA overlap the segments of
	// multipathB.
	private static boolean linearPathOverlapsLinearPath_(MultiPath multipathA,
			MultiPath multipathB, double tolerance) {
		int dim = linearPathIntersectsLinearPathMaxDim_(multipathA, multipathB,
				tolerance, null);

		if (dim < 1)
			return false;

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipathA.queryEnvelope2D(env_a);
		multipathB.queryEnvelope2D(env_b);

		boolean bIntAExtB = interiorEnvExteriorEnv_(env_a, env_b, tolerance);
		boolean bIntBExtA = interiorEnvExteriorEnv_(env_b, env_a, tolerance);

		if (bIntAExtB && bIntBExtA)
			return true;

		if (bIntAExtB && !bIntBExtA)
			return !linearPathWithinLinearPath_(multipathB, multipathA,
					tolerance, false);

		if (bIntBExtA && !bIntAExtB)
			return !linearPathWithinLinearPath_(multipathA, multipathB,
					tolerance, false);

		return !linearPathWithinLinearPath_(multipathA, multipathB, tolerance, false)
				&& !linearPathWithinLinearPath_(multipathB, multipathA,
						tolerance, false);
	}

	// Returns true the dimension of intersection of _multipathA and
	// _multipathB.
	static int linearPathIntersectsLinearPathMaxDim_(MultiPath _multipathA,
			MultiPath _multipathB, double tolerance,
			AttributeStreamOfDbl intersections) {
		MultiPath multipathA;
		MultiPath multipathB;

		if (_multipathA.getSegmentCount() > _multipathB.getSegmentCount()) {
			multipathA = _multipathB;
			multipathB = _multipathA;
		} else {
			multipathA = _multipathA;
			multipathB = _multipathB;
		}

		SegmentIteratorImpl segIterA = ((MultiPathImpl) multipathA._getImpl())
				.querySegmentIterator();
		SegmentIteratorImpl segIterB = ((MultiPathImpl) multipathB._getImpl())
				.querySegmentIterator();
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		int dim = -1;

		int ievent = 0;
		double overlapLength;
		AttributeStreamOfInt32 eventIndices = new AttributeStreamOfInt32(0);
		RelationalOperations relOps = new RelationalOperations();
		OverlapComparer overlapComparer = new OverlapComparer(relOps);
		OverlapEvent overlapEvent;

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipathA.queryEnvelope2D(env_a);
		multipathB.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		Point2D int_point = null;

		if (intersections != null) {
			int_point = new Point2D();
		}

		QuadTreeImpl qtB = null;
		QuadTreeImpl quadTreeB = null;
        QuadTreeImpl quadTreePathsB = null;

		GeometryAccelerators accel = ((MultiPathImpl) multipathB._getImpl())
				._getAccelerators();

		if (accel != null) {
			quadTreeB = accel.getQuadTree();
            quadTreePathsB = accel.getQuadTreeForPaths();
			if (quadTreeB == null) {
				qtB = InternalUtils.buildQuadTree(
						(MultiPathImpl) multipathB._getImpl(), envInter);
				quadTreeB = qtB;
			}
		} else {
			qtB = InternalUtils.buildQuadTree(
					(MultiPathImpl) multipathB._getImpl(), envInter);
			quadTreeB = qtB;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterB = quadTreeB.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsB = null;
        if (quadTreePathsB != null)
            qtIterPathsB = quadTreePathsB.getIterator();

		while (segIterA.nextPath()) {
			overlapLength = 0.0;

			while (segIterA.hasNextSegment()) {
				Segment segmentA = segIterA.nextSegment();
				segmentA.queryEnvelope2D(env_a);

				if (!env_a.isIntersecting(envInter)) {
					continue;
				}

                if (qtIterPathsB != null) {
                    qtIterPathsB.resetIterator(env_a, tolerance);

                    if (qtIterPathsB.next() == -1)
                        continue;
                }

				double lengthA = segmentA.calculateLength2D();

				qtIterB.resetIterator(segmentA, tolerance);

				for (int elementHandleB = qtIterB.next(); elementHandleB != -1; elementHandleB = qtIterB
						.next()) {
					int vertex_b = quadTreeB.getElement(elementHandleB);
					segIterB.resetToVertex(vertex_b);

					Segment segmentB = segIterB.nextSegment();
					double lengthB = segmentB.calculateLength2D();

					int result = segmentA.intersect(segmentB, null, scalarsA,
							scalarsB, tolerance);

					if (result > 0) {
						double scalar_a_0 = scalarsA[0];
						double scalar_b_0 = scalarsB[0];
						double scalar_a_1 = (result == 2 ? scalarsA[1]
								: NumberUtils.TheNaN);
						double scalar_b_1 = (result == 2 ? scalarsB[1]
								: NumberUtils.TheNaN);

						if (result == 2) {
							if (lengthA * (scalar_a_1 - scalar_a_0) > tolerance) {
								dim = 1;
								return dim;
							}

							// Quick neighbor check
							double length = lengthA * (scalar_a_1 - scalar_a_0);

							if (segIterB.hasNextSegment()) {
								segmentB = segIterB.nextSegment();
								result = segmentA.intersect(segmentB, null,
										scalarsA, null, tolerance);

								if (result == 2) {
									double nextScalarA0 = scalarsA[0];
									double nextScalarA1 = scalarsA[1];

									double lengthNext = lengthA
											* (nextScalarA1 - nextScalarA0);

									if (length + lengthNext > tolerance) {
										dim = 1;
										return dim;
									}
								}

								segIterB.resetToVertex(vertex_b);
								segIterB.nextSegment();
							}

							if (!segIterB.isFirstSegmentInPath()) {
								segIterB.previousSegment();
								segmentB = segIterB.previousSegment();
								result = segmentA.intersect(segmentB, null,
										scalarsA, null, tolerance);

								if (result == 2) {
									double nextScalarA0 = scalarsA[0];
									double nextScalarA1 = scalarsA[1];

									double lengthPrevious = lengthA
											* (nextScalarA1 - nextScalarA0);

									if (length + lengthPrevious > tolerance) {
										dim = 1;
										return dim;
									}
								}

								segIterB.resetToVertex(vertex_b);
								segIterB.nextSegment();
							}

							if (segIterA.hasNextSegment()) {
								int vertex_a = segIterA.getStartPointIndex();
								segmentA = segIterA.nextSegment();
								result = segmentA.intersect(segmentB, null,
										scalarsA, null, tolerance);

								if (result == 2) {
									double nextScalarA0 = scalarsA[0];
									double nextScalarA1 = scalarsA[1];

									double lengthNext = lengthA
											* (nextScalarA1 - nextScalarA0);

									if (length + lengthNext > tolerance) {
										dim = 1;
										return dim;
									}
								}

								segIterA.resetToVertex(vertex_a);
								segIterA.nextSegment();
							}

							if (!segIterA.isFirstSegmentInPath()) {
								int vertex_a = segIterA.getStartPointIndex();
								segIterA.previousSegment();
								segmentA = segIterA.previousSegment();
								result = segmentA.intersect(segmentB, null,
										scalarsA, null, tolerance);

								if (result == 2) {
									double nextScalarA0 = scalarsA[0];
									double nextScalarA1 = scalarsA[1];

									double lengthPrevious = lengthB
											* (nextScalarA1 - nextScalarA0);

									if (length + lengthPrevious > tolerance) {
										dim = 1;
										return dim;
									}
								}

								segIterA.resetToVertex(vertex_a);
								segIterA.nextSegment();
							}

							int ivertex_a = segIterA.getStartPointIndex();
							int ipath_a = segIterA.getPathIndex();
							int ivertex_b = segIterB.getStartPointIndex();
							int ipath_b = segIterB.getPathIndex();

							overlapEvent = OverlapEvent.construct(ivertex_a,
									ipath_a, scalar_a_0, scalar_a_1, ivertex_b,
									ipath_b, scalar_b_0, scalar_b_1);
							relOps.m_overlap_events.add(overlapEvent);
							eventIndices.add(eventIndices.size());
						}

						dim = 0;

						if (intersections != null) {
							segmentA.getCoord2D(scalar_a_0, int_point);
							intersections.add(int_point.x);
							intersections.add(int_point.y);
						}
					}
				}

				if (ievent < relOps.m_overlap_events.size()) {
					eventIndices.Sort(ievent, eventIndices.size(),
							overlapComparer);

					double lastScalar = 0.0;
					int lastPath = relOps.m_overlap_events.get(eventIndices
							.get(ievent)).m_ipath_a;

					for (int i = ievent; i < relOps.m_overlap_events.size(); i++) {
						overlapEvent = relOps.m_overlap_events.get(eventIndices
								.get(i));

						if (overlapEvent.m_scalar_a_0 < lastScalar
								&& overlapEvent.m_scalar_a_1 < lastScalar) {
							continue;
						}

						if (lengthA * (overlapEvent.m_scalar_a_0 - lastScalar) > tolerance) {
							overlapLength = lengthA
									* (overlapEvent.m_scalar_a_1 - overlapEvent.m_scalar_a_0); // reset
							lastScalar = overlapEvent.m_scalar_a_1;
							lastPath = overlapEvent.m_ipath_a;
						} else {
							if (overlapEvent.m_ipath_a != lastPath) {
								overlapLength = lengthA
										* (overlapEvent.m_scalar_a_1 - overlapEvent.m_scalar_a_0); // reset
								lastPath = overlapEvent.m_ipath_a;
							} else {
								overlapLength += lengthA
										* (overlapEvent.m_scalar_a_1 - overlapEvent.m_scalar_a_0); // accumulate
							}
							if (overlapLength > tolerance) {
								dim = 1;
								return dim;
							}

							lastScalar = overlapEvent.m_scalar_a_1;

							if (lastScalar == 1.0) {
								break;
							}
						}
					}

					if (lengthA * (1.0 - lastScalar) > tolerance) {
						overlapLength = 0.0; // reset
					}
					ievent = 0;
					eventIndices.resize(0);
					relOps.m_overlap_events.clear();
				}
			}
		}

		return dim;
	}

	// Returns true if the line segments of _multipathA intersect the line
	// segments of _multipathB.
	private static boolean linearPathIntersectsLinearPath_(
			MultiPath multipathA, MultiPath multipathB, double tolerance) {
		MultiPathImpl multi_path_impl_a = (MultiPathImpl) multipathA._getImpl();
		MultiPathImpl multi_path_impl_b = (MultiPathImpl) multipathB._getImpl();

		SegmentIteratorImpl segIterA = multi_path_impl_a.querySegmentIterator();
		SegmentIteratorImpl segIterB = multi_path_impl_b.querySegmentIterator();

        PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(multi_path_impl_a, multi_path_impl_b, tolerance, false);

		while (intersector.next()) {
			int vertex_a = intersector.getRedElement();
			int vertex_b = intersector.getBlueElement();

			segIterA.resetToVertex(vertex_a);
			segIterB.resetToVertex(vertex_b);
			Segment segmentA = segIterA.nextSegment();
			Segment segmentB = segIterB.nextSegment();

			int result = segmentB.intersect(segmentA, null, null, null,
					tolerance);

			if (result > 0) {
				return true;
			}
		}

		return false;
	}

	// Returns true if the relation intersects, crosses, or contains holds
	// between multipathA and multipoint_b. multipathA is put in the
	// Quad_tree_impl.
	private static boolean linearPathIntersectsMultiPoint_(
			MultiPath multipathA, MultiPoint multipoint_b, double tolerance,
			boolean b_intersects_all) {
		SegmentIteratorImpl segIterA = ((MultiPathImpl) multipathA._getImpl())
				.querySegmentIterator();

		boolean bContained = true;
		boolean bInteriorHitFound = false;

		Envelope2D env_a = new Envelope2D();
		Envelope2D env_b = new Envelope2D();
		Envelope2D envInter = new Envelope2D();
		multipathA.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);

		if (!env_a.contains(env_b)) {
			bContained = false;
		}

		env_b.inflate(tolerance, tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		QuadTreeImpl qtA = null;
		QuadTreeImpl quadTreeA = null;
        QuadTreeImpl quadTreePathsA = null;

		GeometryAccelerators accel = ((MultiPathImpl) multipathA._getImpl())
				._getAccelerators();

		if (accel != null) {
			quadTreeA = accel.getQuadTree();
			if (quadTreeA == null) {
				qtA = InternalUtils.buildQuadTree(
						(MultiPathImpl) multipathA._getImpl(), envInter);
				quadTreeA = qtA;
			}
		} else {
			qtA = InternalUtils.buildQuadTree(
					(MultiPathImpl) multipathA._getImpl(), envInter);
			quadTreeA = qtA;
		}

		QuadTreeImpl.QuadTreeIteratorImpl qtIterA = quadTreeA.getIterator();

        QuadTreeImpl.QuadTreeIteratorImpl qtIterPathsA = null;
        if (quadTreePathsA != null)
            qtIterPathsA = quadTreePathsA.getIterator();

		Point2D ptB = new Point2D(), closest = new Point2D();
		boolean b_intersects = false;
		double toleranceSq = tolerance * tolerance;

		for (int i = 0; i < multipoint_b.getPointCount(); i++) {
			multipoint_b.getXY(i, ptB);

			if (!envInter.contains(ptB)) {
				continue;
			}

			env_b.setCoords(ptB.x, ptB.y, ptB.x, ptB.y);

            if (qtIterPathsA != null) {
                qtIterPathsA.resetIterator(env_b, tolerance);

                if (qtIterPathsA.next() == -1)
                    continue;
            }

			qtIterA.resetIterator(env_b, tolerance);

			boolean b_covered = false;

			for (int elementHandleA = qtIterA.next(); elementHandleA != -1; elementHandleA = qtIterA
					.next()) {
				int vertex_a = quadTreeA.getElement(elementHandleA);
				segIterA.resetToVertex(vertex_a);
				Segment segmentA = segIterA.nextSegment();

				double t = segmentA.getClosestCoordinate(ptB, false);
				segmentA.getCoord2D(t, closest);

				if (Point2D.sqrDistance(closest, ptB) <= toleranceSq) {
					b_covered = true;
					break;
				}
			}

			if (b_intersects_all) {
				if (!b_covered) {
					return false;
				}
			} else {
				if (b_covered) {
					return true;
				}
			}
		}

		if (b_intersects_all) {
			return true;
		}

		return false;
	}

	// Returns true if a segment of multipathA intersects point_b.
	static boolean linearPathIntersectsPoint_(MultiPath multipathA,
			Point2D ptB, double tolerance) {
		Point2D closest = new Point2D();
		double toleranceSq = tolerance * tolerance;
		SegmentIteratorImpl segIterA = ((MultiPathImpl) multipathA._getImpl())
				.querySegmentIterator();

		GeometryAccelerators accel = ((MultiPathImpl) multipathA._getImpl())
				._getAccelerators();

		if (accel != null) {
			QuadTreeImpl quadTreeA = accel.getQuadTree();
			if (quadTreeA != null) {
				Envelope2D env_b = new Envelope2D();
				env_b.setCoords(ptB);

				QuadTreeImpl.QuadTreeIteratorImpl qt_iter = quadTreeA
						.getIterator(env_b, tolerance);

				for (int e = qt_iter.next(); e != -1; e = qt_iter.next()) {
					segIterA.resetToVertex(quadTreeA.getElement(e));

					if (segIterA.hasNextSegment()) {
						Segment segmentA = segIterA.nextSegment();

						double t = segmentA.getClosestCoordinate(ptB, false);
						segmentA.getCoord2D(t, closest);

						if (Point2D.sqrDistance(ptB, closest) <= toleranceSq) {
							return true;
						}
					}
				}

				return false;
			}
		}
		Envelope2D env_a = new Envelope2D();

		while (segIterA.nextPath()) {
			while (segIterA.hasNextSegment()) {
				Segment segmentA = segIterA.nextSegment();
				segmentA.queryEnvelope2D(env_a);
				env_a.inflate(tolerance, tolerance);

				if (!env_a.contains(ptB)) {
					continue;
				}

				double t = segmentA.getClosestCoordinate(ptB, false);
				segmentA.getCoord2D(t, closest);

				if (Point2D.sqrDistance(ptB, closest) <= toleranceSq) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean linearPathContainsPoint_(MultiPath multipathA,
			Point2D pt_b, double tolerance) {
		return linearPathIntersectsPoint_(multipathA, pt_b, tolerance)
				&& !linearPathTouchesPointImpl_(multipathA, pt_b, tolerance);
	}

	private static boolean linearPathTouchesPointImpl_(MultiPath multipathA,
			Point2D ptB, double tolerance) {
		MultiPoint boundary = (MultiPoint) (multipathA.getBoundary());
		return !multiPointDisjointPointImpl_(boundary, ptB, tolerance, null);
	}

	// Returns true if the segments of multipathA intersects env_b
	private static boolean linearPathIntersectsEnvelope_(MultiPath multipath_a,
			Envelope2D env_b, double tolerance, ProgressTracker progress_tracker) {
		if (!multipath_a.hasNonLinearSegments()) {
			Envelope2D env_b_inflated = new Envelope2D();
			env_b_inflated.setCoords(env_b);
			env_b_inflated.inflate(tolerance, tolerance);
			MultiPathImpl mimpl_a = (MultiPathImpl) multipath_a._getImpl();
			AttributeStreamOfDbl xy = (AttributeStreamOfDbl) (mimpl_a
					.getAttributeStreamRef(VertexDescription.Semantics.POSITION));
			Point2D pt = new Point2D();
			Point2D pt_prev = new Point2D();
			Point2D pt_1 = new Point2D();
			Point2D pt_2 = new Point2D();
			for (int ipath = 0, npath = mimpl_a.getPathCount(); ipath < npath; ipath++) {
				boolean b_first = true;
				for (int i = mimpl_a.getPathStart(ipath), n = mimpl_a
						.getPathEnd(ipath); i < n; i++) {
					if (b_first) {
						xy.read(2 * i, pt_prev);
						b_first = false;
						continue;
					}

					xy.read(2 * i, pt);
					pt_1.setCoords(pt_prev);
					pt_2.setCoords(pt);
					if (env_b_inflated.clipLine(pt_1, pt_2) != 0)
						return true;

					pt_prev.setCoords(pt);
				}
			}
		} else {
			Line line_1 = new Line(env_b.xmin, env_b.ymin, env_b.xmin,
					env_b.ymax);
			Line line_2 = new Line(env_b.xmin, env_b.ymax, env_b.xmax,
					env_b.ymax);
			Line line3 = new Line(env_b.xmax, env_b.ymax, env_b.xmax,
					env_b.ymin);
			Line line4 = new Line(env_b.xmax, env_b.ymin, env_b.xmin,
					env_b.ymin);
			SegmentIterator iter = multipath_a.querySegmentIterator();
			while (iter.nextPath()) {
				while (iter.hasNextSegment()) {
					Segment polySeg = iter.nextSegment();
					if (polySeg.isIntersecting(line_1, tolerance))
						return true;

					if (polySeg.isIntersecting(line_2, tolerance))
						return true;

					if (polySeg.isIntersecting(line3, tolerance))
						return true;

					if (polySeg.isIntersecting(line4, tolerance))
						return true;
				}
			}
		}

		return false;
	}

	// Returns contains, disjoint, or within if the relationship can be
	// determined from the rasterized tests.
	// When bExtraTestForIntersects is true performs extra tests and can return
	// "intersects".
	static int tryRasterizedContainsOrDisjoint_(Geometry geom_a,
			Geometry geom_b, double tolerance, boolean bExtraTestForIntersects) {
		int gtA = geom_a.getType().value();
		int gtB = geom_b.getType().value();
		do {
			if (Geometry.isMultiVertex(gtA)) {
				MultiVertexGeometryImpl impl = (MultiVertexGeometryImpl) geom_a
						._getImpl();
				GeometryAccelerators accel = impl._getAccelerators();
				if (accel != null) {
					RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
					if (rgeom != null) {
						if (gtB == Geometry.GeometryType.Point) {
							Point2D ptB = ((Point) geom_b).getXY();
							RasterizedGeometry2D.HitType hit = rgeom
									.queryPointInGeometry(ptB.x, ptB.y);
							if (hit == RasterizedGeometry2D.HitType.Inside) {
								return Relation.contains;
							} else if (hit == RasterizedGeometry2D.HitType.Outside) {
								return Relation.disjoint;
							}
							break;
						}
						Envelope2D env_b = new Envelope2D();
						geom_b.queryEnvelope2D(env_b);
						RasterizedGeometry2D.HitType hit = rgeom
								.queryEnvelopeInGeometry(env_b);
						if (hit == RasterizedGeometry2D.HitType.Inside) {
							return Relation.contains;
						} else if (hit == RasterizedGeometry2D.HitType.Outside) {
							return Relation.disjoint;
						} else if (bExtraTestForIntersects
								&& Geometry.isMultiVertex(gtB)) {
							if (checkVerticesForIntersection_(
									(MultiVertexGeometryImpl) geom_b._getImpl(),
									rgeom)) {
								return Relation.intersects;
							}
						}

						break;
					}
				}
			}
		} while (false);

		do {
			if (Geometry.isMultiVertex(gtB)) {
				MultiVertexGeometryImpl impl = (MultiVertexGeometryImpl) geom_b
						._getImpl();
				GeometryAccelerators accel = impl._getAccelerators();
				if (accel != null) {
					RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
					if (rgeom != null) {
						if (gtA == Geometry.GeometryType.Point) {
							Point2D ptA = ((Point) geom_a).getXY();
							RasterizedGeometry2D.HitType hit = rgeom
									.queryPointInGeometry(ptA.x, ptA.y);
							if (hit == RasterizedGeometry2D.HitType.Inside) {
								return Relation.within;
							} else if (hit == RasterizedGeometry2D.HitType.Outside) {
								return Relation.disjoint;
							}
							break;
						}

						Envelope2D env_a = new Envelope2D();
						geom_a.queryEnvelope2D(env_a);
						RasterizedGeometry2D.HitType hit = rgeom
								.queryEnvelopeInGeometry(env_a);
						if (hit == RasterizedGeometry2D.HitType.Inside) {
							return Relation.within;
						} else if (hit == RasterizedGeometry2D.HitType.Outside) {
							return Relation.disjoint;
						} else if (bExtraTestForIntersects
								&& Geometry.isMultiVertex(gtA)) {
							if (checkVerticesForIntersection_(
									(MultiVertexGeometryImpl) geom_a._getImpl(),
									rgeom)) {
								return Relation.intersects;
							}
						}

						break;
					}
				}
			}
		} while (false);

		return Relation.unknown;
	}

	// Returns true if intersects and false if nothing can be determined.
	private static boolean checkVerticesForIntersection_(
			MultiVertexGeometryImpl geom, RasterizedGeometry2D rgeom) {
		// Do a quick raster test for each point. If any point is inside, then
		// there is an intersection.
		int pointCount = geom.getPointCount();
		Point2D pt = new Point2D();
		for (int ipoint = 0; ipoint < pointCount; ipoint++) {
			geom.getXY(ipoint, pt);
			RasterizedGeometry2D.HitType hit = rgeom.queryPointInGeometry(pt.x,
					pt.y);
			if (hit == RasterizedGeometry2D.HitType.Inside) {
				return true;
			}
		}

		return false;
	}

	private static boolean polygonTouchesPolygonImpl_(Polygon polygon_a,
			Polygon polygon_b, double tolerance, ProgressTracker progressTracker) {
		MultiPathImpl polygon_impl_a = (MultiPathImpl) polygon_a._getImpl();
		MultiPathImpl polygon_impl_b = (MultiPathImpl) polygon_b._getImpl();

		// double geom_tolerance;
		boolean b_geometries_simple = polygon_impl_a.getIsSimple(0.0) >= 1
				&& polygon_impl_b.getIsSimple(0.0) >= 1;

		SegmentIteratorImpl segIterA = polygon_impl_a.querySegmentIterator();
		SegmentIteratorImpl segIterB = polygon_impl_b.querySegmentIterator();
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(
				polygon_impl_a, polygon_impl_b, tolerance, false);

		boolean b_boundaries_intersect = false;

		while (intersector.next()) {
			int vertex_a = intersector.getRedElement();
			int vertex_b = intersector.getBlueElement();

			segIterA.resetToVertex(vertex_a);
			segIterB.resetToVertex(vertex_b);
			Segment segmentA = segIterA.nextSegment();
			Segment segmentB = segIterB.nextSegment();

			int result = segmentB.intersect(segmentA, null, scalarsB, scalarsA,
					tolerance);

			if (result == 2) {
				double scalar_a_0 = scalarsA[0];
				double scalar_a_1 = scalarsA[1];
				double length_a = segmentA.calculateLength2D();

				if (b_geometries_simple
						&& (scalar_a_1 - scalar_a_0) * length_a > tolerance) {
					// If the line segments overlap along the same direction,
					// then we have an Interior-Interior intersection
					return false;
				}

				b_boundaries_intersect = true;
			} else if (result != 0) {
				double scalar_a_0 = scalarsA[0];
				double scalar_b_0 = scalarsB[0];

				if (scalar_a_0 > 0.0 && scalar_a_0 < 1.0 && scalar_b_0 > 0.0
						&& scalar_b_0 < 1.0) {
					return false;
				}

				b_boundaries_intersect = true;
			}
		}

		if (!b_boundaries_intersect) {
			return false;
		}

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), envInter = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polygon_b.queryEnvelope2D(env_b);
		env_a.inflate(1000.0 * tolerance, 1000.0 * tolerance);
		env_b.inflate(1000.0 * tolerance, 1000.0 * tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		Polygon _polygonA;
		Polygon _polygonB;

		if (polygon_a.getPointCount() > 10) {
			_polygonA = (Polygon) (Clipper.clip(polygon_a, envInter, tolerance,
					0.0));
			if (_polygonA.isEmpty()) {
				return false;
			}
		} else {
			_polygonA = polygon_a;
		}

		if (polygon_b.getPointCount() > 10) {
			_polygonB = (Polygon) (Clipper.clip(polygon_b, envInter, tolerance,
					0.0));
			if (_polygonB.isEmpty()) {
				return false;
			}
		} else {
			_polygonB = polygon_b;
		}

		// We just need to determine whether interior_interior is false
		String scl = "F********";
		boolean bRelation = RelationalOperationsMatrix.polygonRelatePolygon_(
				_polygonA, _polygonB, tolerance, scl, progressTracker);

		return bRelation;
	}

	private static boolean polygonOverlapsPolygonImpl_(Polygon polygon_a,
			Polygon polygon_b, double tolerance, ProgressTracker progressTracker) {
		MultiPathImpl polygon_impl_a = (MultiPathImpl) polygon_a._getImpl();
		MultiPathImpl polygon_impl_b = (MultiPathImpl) polygon_b._getImpl();

		// double geom_tolerance;
		boolean b_geometries_simple = polygon_impl_a.getIsSimple(0.0) >= 1
				&& polygon_impl_b.getIsSimple(0.0) >= 1;

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), envInter = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polygon_b.queryEnvelope2D(env_b);

		boolean bInteriorIntersectionKnown = false;

		boolean bIntAExtB = interiorEnvExteriorEnv_(env_a, env_b, tolerance);
		boolean bExtAIntB = interiorEnvExteriorEnv_(env_b, env_a, tolerance);

		SegmentIteratorImpl segIterA = polygon_impl_a.querySegmentIterator();
		SegmentIteratorImpl segIterB = polygon_impl_b.querySegmentIterator();
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(
				polygon_impl_a, polygon_impl_b, tolerance, false);

		while (intersector.next()) {
			int vertex_a = intersector.getRedElement();
			int vertex_b = intersector.getBlueElement();

			segIterA.resetToVertex(vertex_a);
			segIterB.resetToVertex(vertex_b);
			Segment segmentA = segIterA.nextSegment();
			Segment segmentB = segIterB.nextSegment();

			int result = segmentB.intersect(segmentA, null, scalarsB, scalarsA,
					tolerance);

			if (result == 2) {
				double scalar_a_0 = scalarsA[0];
				double scalar_a_1 = scalarsA[1];
				double length_a = segmentA.calculateLength2D();

				if (b_geometries_simple
						&& (scalar_a_1 - scalar_a_0) * length_a > tolerance) {
					// When the line segments intersect along the same
					// direction, then we have an interior-interior intersection
					bInteriorIntersectionKnown = true;

					if (bIntAExtB && bExtAIntB) {
						return true;
					}
				}
			} else if (result != 0) {
				double scalar_a_0 = scalarsA[0];
				double scalar_b_0 = scalarsB[0];

				if (scalar_a_0 > 0.0 && scalar_a_0 < 1.0 && scalar_b_0 > 0.0
						&& scalar_b_0 < 1.0) {
					return true;
				}
			}
		}

		Envelope2D envAInflated = new Envelope2D(), envBInflated = new Envelope2D();
		envAInflated.setCoords(env_a);
		envAInflated.inflate(1000.0 * tolerance, 1000.0 * tolerance);
		envBInflated.setCoords(env_b);
		envBInflated.inflate(1000.0 * tolerance, 1000.0 * tolerance);

		envInter.setCoords(envAInflated);
		envInter.intersect(envBInflated);

		Polygon _polygonA;
		Polygon _polygonB;
		StringBuilder scl = new StringBuilder();

		if (!bInteriorIntersectionKnown) {
			scl.append("T*");
		} else {
			scl.append("**");
		}

		if (bIntAExtB) {
			if (polygon_b.getPointCount() > 10) {
				_polygonB = (Polygon) (Clipper.clip(polygon_b, envInter,
						tolerance, 0.0));
				if (_polygonB.isEmpty()) {
					return false;
				}
			} else {
				_polygonB = polygon_b;
			}

			scl.append("****");
		} else {
			_polygonB = polygon_b;
			scl.append("T***");
		}

		if (bExtAIntB) {
			if (polygon_a.getPointCount() > 10) {
				_polygonA = (Polygon) (Clipper.clip(polygon_a, envInter,
						tolerance, 0.0));
				if (_polygonA.isEmpty()) {
					return false;
				}
			} else {
				_polygonA = polygon_a;
			}

			scl.append("***");
		} else {
			_polygonA = polygon_a;
			scl.append("T**");
		}

		boolean bRelation = RelationalOperationsMatrix.polygonRelatePolygon_(
				_polygonA, _polygonB, tolerance, scl.toString(),
				progressTracker);
		return bRelation;
	}

	private static boolean polygonContainsPolygonImpl_(Polygon polygon_a,
			Polygon polygon_b, double tolerance, ProgressTracker progressTracker) {
        boolean[] b_result_known = new boolean[1];
        b_result_known[0] = false;
        boolean res = polygonContainsMultiPath_(polygon_a, polygon_b, tolerance, b_result_known, progressTracker);

        if (b_result_known[0])
            return res;

        // We can clip polygon_a to the extent of polyline_b

        Envelope2D envBInflated = new Envelope2D();
        polygon_b.queryEnvelope2D(envBInflated);
        envBInflated.inflate(1000.0 * tolerance, 1000.0 * tolerance);

        Polygon _polygonA = null;

        if (polygon_a.getPointCount() > 10)
        {
            _polygonA = (Polygon)Clipper.clip(polygon_a, envBInflated, tolerance, 0.0);
            if (_polygonA.isEmpty())
                return false;
        }
        else
        {
            _polygonA = polygon_a;
        }

        boolean bContains = RelationalOperationsMatrix.polygonContainsPolygon_(_polygonA, polygon_b, tolerance, progressTracker);
        return bContains;
	}

    private static boolean polygonContainsMultiPath_(Polygon polygon_a, MultiPath multi_path_b, double tolerance, boolean[] b_result_known, ProgressTracker progress_tracker)
    {
        b_result_known[0] = false;

        MultiPathImpl polygon_impl_a = (MultiPathImpl)polygon_a._getImpl();
        MultiPathImpl multi_path_impl_b = (MultiPathImpl)multi_path_b._getImpl();

        SegmentIteratorImpl segIterA = polygon_impl_a.querySegmentIterator();
        SegmentIteratorImpl segIterB = multi_path_impl_b.querySegmentIterator();
        double[] scalarsA = new double[2];
        double[] scalarsB = new double[2];

        PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(polygon_impl_a, multi_path_impl_b, tolerance, false);
        boolean b_boundaries_intersect = false;

        while (intersector.next())
        {
            int vertex_a = intersector.getRedElement();
            int vertex_b = intersector.getBlueElement();

            segIterA.resetToVertex(vertex_a, -1);
            segIterB.resetToVertex(vertex_b, -1);
            Segment segmentA = segIterA.nextSegment();
            Segment segmentB = segIterB.nextSegment();

            int result = segmentB.intersect(segmentA, null, scalarsB, scalarsA, tolerance);

            if (result != 0) {
                b_boundaries_intersect = true;
                if (result == 1) {
                    double scalar_a_0 = scalarsA[0];
                    double scalar_b_0 = scalarsB[0];

                    if (scalar_a_0 > 0.0 && scalar_a_0 < 1.0 && scalar_b_0 > 0.0 && scalar_b_0 < 1.0) {
                        b_result_known[0] = true;
                        return false;
                    }
                }
            }
        }

        if (!b_boundaries_intersect)
        {
            b_result_known[0] = true;

            //boundaries do not intersect

            Envelope2D env_a_inflated = new Envelope2D();
            polygon_a.queryEnvelope2D(env_a_inflated);
            env_a_inflated.inflate(tolerance, tolerance);

            Polygon pa = null;
            Polygon p_polygon_a = polygon_a;

            boolean b_checked_polygon_a_quad_tree = false;

            Envelope2D path_env_b = new Envelope2D();

            for (int ipath = 0, npath = multi_path_b.getPathCount(); ipath < npath; ipath++)
            {
                if (multi_path_b.getPathSize(ipath) > 0)
                {
                    multi_path_b.queryPathEnvelope2D(ipath, path_env_b);

                    if (env_a_inflated.isIntersecting(path_env_b))
                    {
                        Point2D anyPoint = multi_path_b.getXY(multi_path_b.getPathStart(ipath));
                        int res = PointInPolygonHelper.isPointInPolygon(p_polygon_a, anyPoint, 0);
                        if (res == 0)
                            return false;
                    }
                    else
                    {
                        return false;
                    }

                    if (!b_checked_polygon_a_quad_tree) {
                        if (PointInPolygonHelper.quadTreeWillHelp(polygon_a, multi_path_b.getPathCount() - 1) && (polygon_impl_a._getAccelerators() == null || polygon_impl_a._getAccelerators().getQuadTree() == null)) {
                            pa = new Polygon();
                            polygon_a.copyTo(pa);
                            ((MultiPathImpl) pa._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                            p_polygon_a = pa;
                        } else {
                            p_polygon_a = polygon_a;
                        }

                        b_checked_polygon_a_quad_tree = true;
                    }
                }
            }

            if (polygon_a.getPathCount() == 1 || multi_path_b.getType().value() == Geometry.GeometryType.Polyline)
                return true; //boundaries do not intersect. all paths of b are inside of a

            // Polygon A has multiple rings, and Multi_path B is a polygon.

            Polygon polygon_b = (Polygon)multi_path_b;

            Envelope2D env_b_inflated = new Envelope2D();
            polygon_b.queryEnvelope2D(env_b_inflated);
            env_b_inflated.inflate(tolerance, tolerance);

            Polygon pb = null;
            Polygon p_polygon_b = polygon_b;

            boolean b_checked_polygon_b_quad_tree = false;

            Envelope2D path_env_a = new Envelope2D();

            for (int ipath = 0, npath = polygon_a.getPathCount(); ipath < npath; ipath++)
            {
                if (polygon_a.getPathSize(ipath) > 0)
                {
                    polygon_a.queryPathEnvelope2D(ipath, path_env_a);

                    if (env_b_inflated.isIntersecting(path_env_a))
                    {
                        Point2D anyPoint = polygon_a.getXY(polygon_a.getPathStart(ipath));
                        int res = PointInPolygonHelper.isPointInPolygon(p_polygon_b, anyPoint, 0);
                        if (res == 1)
                            return false;
                    }

                    if (!b_checked_polygon_b_quad_tree) {
                        if (PointInPolygonHelper.quadTreeWillHelp(polygon_b, polygon_a.getPathCount() - 1) && (multi_path_impl_b._getAccelerators() == null || multi_path_impl_b._getAccelerators().getQuadTree() == null)) {
                            pb = new Polygon();
                            polygon_b.copyTo(pb);
                            ((MultiPathImpl) pb._getImpl())._buildQuadTreeAccelerator(Geometry.GeometryAccelerationDegree.enumMedium);
                            p_polygon_b = pb;
                        } else {
                            p_polygon_b = polygon_b;
                        }

                        b_checked_polygon_b_quad_tree = true;
                    }
                }
            }

            return true;
        }

        return false;
    }

	private static boolean polygonTouchesPolylineImpl_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progressTracker) {
		MultiPathImpl polygon_impl_a = (MultiPathImpl) polygon_a._getImpl();
		MultiPathImpl polyline_impl_b = (MultiPathImpl) polyline_b._getImpl();

		SegmentIteratorImpl segIterA = polygon_impl_a.querySegmentIterator();
		SegmentIteratorImpl segIterB = polyline_impl_b.querySegmentIterator();
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(
				polygon_impl_a, polyline_impl_b, tolerance, false);

		boolean b_boundaries_intersect = false;

		while (intersector.next()) {
			int vertex_a = intersector.getRedElement();
			int vertex_b = intersector.getBlueElement();

			segIterA.resetToVertex(vertex_a);
			segIterB.resetToVertex(vertex_b);
			Segment segmentA = segIterA.nextSegment();
			Segment segmentB = segIterB.nextSegment();

			int result = segmentB.intersect(segmentA, null, scalarsB, scalarsA,
					tolerance);

			if (result == 2) {
				b_boundaries_intersect = true;
			} else if (result != 0) {
				double scalar_a_0 = scalarsA[0];
				double scalar_b_0 = scalarsB[0];

				if (scalar_a_0 > 0.0 && scalar_a_0 < 1.0 && scalar_b_0 > 0.0
						&& scalar_b_0 < 1.0) {
					return false;
				}

				b_boundaries_intersect = true;
			}
		}

		if (!b_boundaries_intersect) {
			return false;
		}

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), envInter = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);
		env_a.inflate(1000.0 * tolerance, 1000.0 * tolerance);
		env_b.inflate(1000.0 * tolerance, 1000.0 * tolerance);
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		Polygon _polygonA;
		Polyline _polylineB;

		if (polygon_a.getPointCount() > 10) {
			_polygonA = (Polygon) (Clipper.clip(polygon_a, envInter, tolerance,
					0.0));
			if (_polygonA.isEmpty()) {
				return false;
			}
		} else {
			_polygonA = polygon_a;
		}

		if (polyline_b.getPointCount() > 10) {
			_polylineB = (Polyline) Clipper.clip(polyline_b, envInter,
					tolerance, 0.0);
			if (_polylineB.isEmpty()) {
				return false;
			}
		} else {
			_polylineB = polyline_b;
		}

		// We just need to determine that interior_interior is false
		String scl = "F********";
		boolean bRelation = RelationalOperationsMatrix.polygonRelatePolyline_(
				_polygonA, _polylineB, tolerance, scl, progressTracker);

		return bRelation;
	}

	private static boolean polygonCrossesPolylineImpl_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progressTracker) {
		MultiPathImpl polygon_impl_a = (MultiPathImpl) polygon_a._getImpl();
		MultiPathImpl polyline_impl_b = (MultiPathImpl) polyline_b._getImpl();

		SegmentIteratorImpl segIterA = polygon_impl_a.querySegmentIterator();
		SegmentIteratorImpl segIterB = polyline_impl_b.querySegmentIterator();
		double[] scalarsA = new double[2];
		double[] scalarsB = new double[2];

		PairwiseIntersectorImpl intersector = new PairwiseIntersectorImpl(
				polygon_impl_a, polyline_impl_b, tolerance, false);

		boolean b_boundaries_intersect = false;

		while (intersector.next()) {
			int vertex_a = intersector.getRedElement();
			int vertex_b = intersector.getBlueElement();

			segIterA.resetToVertex(vertex_a);
			segIterB.resetToVertex(vertex_b);
			Segment segmentA = segIterA.nextSegment();
			Segment segmentB = segIterB.nextSegment();

			int result = segmentB.intersect(segmentA, null, scalarsB, scalarsA,
					tolerance);

			if (result == 2) {
				b_boundaries_intersect = true;
			} else if (result != 0) {
				double scalar_a_0 = scalarsA[0];
				double scalar_b_0 = scalarsB[0];

				if (scalar_a_0 > 0.0 && scalar_a_0 < 1.0 && scalar_b_0 > 0.0
						&& scalar_b_0 < 1.0) {
					return true;
				}

				b_boundaries_intersect = true;
			}
		}

		if (!b_boundaries_intersect) {
			return false;
		}

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), envAInflated = new Envelope2D(), envBInflated = new Envelope2D(), envInter = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);

		if (interiorEnvExteriorEnv_(env_b, env_a, tolerance)) {
			envAInflated.setCoords(env_a);
			envAInflated.inflate(1000.0 * tolerance, 1000.0 * tolerance);
			envBInflated.setCoords(env_b);
			envBInflated.inflate(1000.0 * tolerance, 1000.0 * tolerance);
			envInter.setCoords(envAInflated);
			envInter.intersect(envBInflated);

			Polygon _polygonA;
			Polyline _polylineB;

			if (polygon_a.getPointCount() > 10) {
				_polygonA = (Polygon) (Clipper.clip(polygon_a, envInter,
						tolerance, 0.0));
				if (_polygonA.isEmpty()) {
					return false;
				}
			} else {
				_polygonA = polygon_a;
			}

			if (polyline_b.getPointCount() > 10) {
				_polylineB = (Polyline) (Clipper.clip(polyline_b, envInter,
						tolerance, 0.0));
				if (_polylineB.isEmpty()) {
					return false;
				}
			} else {
				_polylineB = polyline_b;
			}

			String scl = "T********";
			boolean bRelation = RelationalOperationsMatrix
					.polygonRelatePolyline_(_polygonA, _polylineB, tolerance,
							scl, progressTracker);
			return bRelation;
		}

		String scl = "T*****T**";
		boolean bRelation = RelationalOperationsMatrix.polygonRelatePolyline_(
				polygon_a, polyline_b, tolerance, scl, progressTracker);

		return bRelation;
	}

	private static boolean polygonContainsPolylineImpl_(Polygon polygon_a,
			Polyline polyline_b, double tolerance,
			ProgressTracker progress_tracker) {
        boolean[] b_result_known = new boolean[1];
        b_result_known[0] = false;
        boolean res = polygonContainsMultiPath_(polygon_a, polyline_b, tolerance, b_result_known, progress_tracker);

        if (b_result_known[0])
            return res;

        // We can clip polygon_a to the extent of polyline_b

        Envelope2D envBInflated = new Envelope2D();
        polyline_b.queryEnvelope2D(envBInflated);
        envBInflated.inflate(1000.0 * tolerance, 1000.0 * tolerance);

        Polygon _polygonA = null;

        if (polygon_a.getPointCount() > 10)
        {
            _polygonA = (Polygon)Clipper.clip(polygon_a, envBInflated, tolerance, 0.0);
            if (_polygonA.isEmpty())
                return false;
        }
        else
        {
            _polygonA = polygon_a;
        }

        boolean bContains = RelationalOperationsMatrix.polygonContainsPolyline_(_polygonA, polyline_b, tolerance, progress_tracker);
        return bContains;
	}

	private static boolean polygonContainsPointImpl_(Polygon polygon_a,
			Point2D pt_b, double tolerance, ProgressTracker progressTracker) {
		PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(
				polygon_a, pt_b, tolerance);

		if (result == PolygonUtils.PiPResult.PiPInside)
			return true;

		return false;
	}

	private static boolean polygonTouchesPointImpl_(Polygon polygon_a,
			Point2D pt_b, double tolerance, ProgressTracker progressTracker) {
		PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(
				polygon_a, pt_b, tolerance);

		if (result == PolygonUtils.PiPResult.PiPBoundary)
			return true;

		return false;
	}

	static boolean multiPointDisjointPointImpl_(MultiPoint multipoint_a,
			Point2D pt_b, double tolerance, ProgressTracker progressTracker) {
		Point2D pt_a = new Point2D();
		double tolerance_sq = tolerance * tolerance;

		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, pt_a);

			if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance_sq)
				return false;
		}

		return true;
	}

	private static final class OverlapEvent {
		int m_ivertex_a;
		int m_ipath_a;
		double m_scalar_a_0;
		double m_scalar_a_1;
		int m_ivertex_b;
		int m_ipath_b;
		double m_scalar_b_0;
		double m_scalar_b_1;

		static OverlapEvent construct(int ivertex_a, int ipath_a,
				double scalar_a_0, double scalar_a_1, int ivertex_b,
				int ipath_b, double scalar_b_0, double scalar_b_1) {
			OverlapEvent overlapEvent = new OverlapEvent();
			overlapEvent.m_ivertex_a = ivertex_a;
			overlapEvent.m_ipath_a = ipath_a;
			overlapEvent.m_scalar_a_0 = scalar_a_0;
			overlapEvent.m_scalar_a_1 = scalar_a_1;
			overlapEvent.m_ivertex_b = ivertex_b;
			overlapEvent.m_ipath_b = ipath_b;
			overlapEvent.m_scalar_b_0 = scalar_b_0;
			overlapEvent.m_scalar_b_1 = scalar_b_1;
			return overlapEvent;
		}
	}

	ArrayList<OverlapEvent> m_overlap_events;

	private RelationalOperations() {
		m_overlap_events = new ArrayList<OverlapEvent>();
	}

	private static class OverlapComparer extends
			AttributeStreamOfInt32.IntComparator {
		OverlapComparer(RelationalOperations rel_ops) {
			m_rel_ops = rel_ops;
		}

		@Override
		public int compare(int o_1, int o_2) {
			return m_rel_ops.compareOverlapEvents_(o_1, o_2);
		}

		private RelationalOperations m_rel_ops;
	}

	int compareOverlapEvents_(int o_1, int o_2) {
		OverlapEvent overlapEvent1 = m_overlap_events.get(o_1);
		OverlapEvent overlapEvent2 = m_overlap_events.get(o_2);

		if (overlapEvent1.m_ipath_a < overlapEvent2.m_ipath_a)
			return -1;

		if (overlapEvent1.m_ipath_a == overlapEvent2.m_ipath_a) {
			if (overlapEvent1.m_ivertex_a < overlapEvent2.m_ivertex_a)
				return -1;

			if (overlapEvent1.m_ivertex_a == overlapEvent2.m_ivertex_a) {
				if (overlapEvent1.m_scalar_a_0 < overlapEvent2.m_scalar_a_0)
					return -1;

				if (overlapEvent1.m_scalar_a_0 == overlapEvent2.m_scalar_a_0) {
					if (overlapEvent1.m_scalar_a_1 < overlapEvent2.m_scalar_a_1)
						return -1;

					if (overlapEvent1.m_scalar_a_1 == overlapEvent2.m_scalar_a_1) {
						if (overlapEvent1.m_ivertex_b < overlapEvent2.m_ivertex_b)
							return -1;
					}
				}
			}
		}

		return 1;
	}

	static final class Accelerate_helper {
		static boolean accelerate_geometry(Geometry geometry,
				SpatialReference sr,
				Geometry.GeometryAccelerationDegree accel_degree) {
			if (!can_accelerate_geometry(geometry))
				return false;

			double tol = InternalUtils.calculateToleranceFromGeometry(sr,
					geometry, false);
			boolean bAccelerated = false;
			if (GeometryAccelerators.canUseRasterizedGeometry(geometry))
				bAccelerated |= ((MultiVertexGeometryImpl) geometry._getImpl())
						._buildRasterizedGeometryAccelerator(tol, accel_degree);

			Geometry.Type type = geometry.getType();
			if ((type == Geometry.Type.Polygon || type == Geometry.Type.Polyline)
					&& GeometryAccelerators.canUseQuadTree(geometry)
					&& accel_degree != Geometry.GeometryAccelerationDegree.enumMild)
				bAccelerated |= ((MultiVertexGeometryImpl) geometry._getImpl())
						._buildQuadTreeAccelerator(accel_degree);

			if ((type == Geometry.Type.Polygon || type == Geometry.Type.Polyline)
					&& GeometryAccelerators.canUseQuadTreeForPaths(geometry)
					&& accel_degree != Geometry.GeometryAccelerationDegree.enumMild)
				bAccelerated |= ((MultiPathImpl) geometry._getImpl())
						._buildQuadTreeForPathsAccelerator(accel_degree);

			return bAccelerated;
		}

        static boolean can_accelerate_geometry(Geometry geometry) {
            return GeometryAccelerators.canUseRasterizedGeometry(geometry)
                    || GeometryAccelerators.canUseQuadTree(geometry) || GeometryAccelerators.canUseQuadTreeForPaths(geometry);
        }
    }
}
