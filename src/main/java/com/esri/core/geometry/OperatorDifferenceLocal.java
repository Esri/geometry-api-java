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

class OperatorDifferenceLocal extends OperatorDifference {

	@Override
	public GeometryCursor execute(GeometryCursor inputGeometries,
			GeometryCursor subtractor, SpatialReference sr,
			ProgressTracker progressTracker) {
		return new OperatorDifferenceCursor(inputGeometries, subtractor, sr,
				progressTracker);
	}

	@Override
	public Geometry execute(Geometry inputGeometry, Geometry subtractor,
			SpatialReference sr, ProgressTracker progressTracker) {
		SimpleGeometryCursor inputGeomCurs = new SimpleGeometryCursor(
				inputGeometry);
		SimpleGeometryCursor subractorCurs = new SimpleGeometryCursor(
				subtractor);
		GeometryCursor geometryCursor = execute(inputGeomCurs, subractorCurs,
				sr, progressTracker);

		return geometryCursor.next();
	}

	static Geometry difference(Geometry geometry_a, Geometry geometry_b,
			SpatialReference spatial_reference, ProgressTracker progress_tracker) {
		if (geometry_a.isEmpty() || geometry_b.isEmpty())
			return geometry_a;

		int dimension_a = geometry_a.getDimension();
		int dimension_b = geometry_b.getDimension();

		if (dimension_a > dimension_b)
			return geometry_a;

		int type_a = geometry_a.getType().value();
		int type_b = geometry_b.getType().value();

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), env_merged = new Envelope2D();
		geometry_a.queryEnvelope2D(env_a);
		geometry_b.queryEnvelope2D(env_b);
		env_merged.setCoords(env_a);
		env_merged.merge(env_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatial_reference, env_merged, false);
		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;

		Envelope2D env_a_inflated = new Envelope2D();
		env_a_inflated.setCoords(env_a);
		env_a_inflated.inflate(tolerance_cluster, tolerance_cluster); // inflate
																		// by
																		// cluster
																		// tolerance

		if (!env_a_inflated.isIntersecting(env_b))
			return geometry_a;

		if (type_a == Geometry.GeometryType.Point) {
			Geometry geometry_b_;
			if (MultiPath.isSegment(type_b)) {
				geometry_b_ = new Polyline(geometry_b.getDescription());
				((Polyline) (geometry_b_)).addSegment((Segment) (geometry_b),
						true);
			} else {
				geometry_b_ = geometry_b;
			}
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				return pointMinusPolygon_((Point) (geometry_a),
						(Polygon) (geometry_b_), tolerance, progress_tracker);
			case Geometry.GeometryType.Polyline:
				return pointMinusPolyline_((Point) (geometry_a),
						(Polyline) (geometry_b_), tolerance, progress_tracker);
			case Geometry.GeometryType.MultiPoint:
				return pointMinusMultiPoint_((Point) (geometry_a),
						(MultiPoint) (geometry_b_), tolerance, progress_tracker);
			case Geometry.GeometryType.Envelope:
				return pointMinusEnvelope_((Point) (geometry_a),
						(Envelope) (geometry_b_), tolerance, progress_tracker);
			case Geometry.GeometryType.Point:
				return pointMinusPoint_((Point) (geometry_a),
						(Point) (geometry_b_), tolerance, progress_tracker);
			default:
				throw new IllegalArgumentException();
			}
		} else if (type_a == Geometry.GeometryType.MultiPoint) {
			switch (type_b) {
			case Geometry.GeometryType.Polygon:
				return multiPointMinusPolygon_((MultiPoint) (geometry_a),
						(Polygon) (geometry_b), tolerance, progress_tracker);
			case Geometry.GeometryType.Envelope:
				return multiPointMinusEnvelope_((MultiPoint) (geometry_a),
						(Envelope) (geometry_b), tolerance, progress_tracker);
			case Geometry.GeometryType.Point:
				return multiPointMinusPoint_((MultiPoint) (geometry_a),
						(Point) (geometry_b), tolerance, progress_tracker);
			default:
				break;
			}
		}
		return TopologicalOperations.difference(geometry_a, geometry_b,
				spatial_reference, progress_tracker);
	}

	// these are special implementations, all others delegate to the topo-graph.
	static Geometry pointMinusPolygon_(Point point, Polygon polygon,
			double tolerance, ProgressTracker progress_tracker) {
		PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(
				polygon, point, tolerance);

		if (result == PolygonUtils.PiPResult.PiPOutside)
			return point;

		return point.createInstance();
	}

	static Geometry pointMinusPolyline_(Point point, Polyline polyline,
			double tolerance, ProgressTracker progress_tracker) {
		Point2D pt = point.getXY();
		SegmentIterator seg_iter = polyline.querySegmentIterator();

		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;
		double tolerance_cluster_sq = tolerance_cluster * tolerance_cluster;
		Envelope2D env = new Envelope2D();

		while (seg_iter.nextPath()) {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();

				segment.queryEnvelope2D(env);
				env.inflate(tolerance_cluster, tolerance_cluster);

				if (!env.contains(pt))
					continue;

				if (segment.isIntersecting(pt, tolerance))
					return point.createInstance();

				// check segment end points to the cluster tolerance
				Point2D end_point = segment.getStartXY();

				if (Point2D.sqrDistance(pt, end_point) <= tolerance_cluster_sq)
					return point.createInstance();

				end_point = segment.getEndXY();

				if (Point2D.sqrDistance(pt, end_point) <= tolerance_cluster_sq)
					return point.createInstance();
			}
		}

		return point;
	}

	static Geometry pointMinusMultiPoint_(Point point, MultiPoint multi_point,
			double tolerance, ProgressTracker progress_tracker) {
		MultiPointImpl multipointImpl = (MultiPointImpl) (multi_point
				._getImpl());
		AttributeStreamOfDbl position = (AttributeStreamOfDbl) multipointImpl
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		int point_count = multi_point.getPointCount();
		Point2D point2D = point.getXY();
		Point2D pt = new Point2D();

		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;
		double tolerance_cluster_sq = tolerance_cluster * tolerance_cluster;

		for (int i = 0; i < point_count; i++) {
			position.read(2 * i, pt);
			double sqr_dist = Point2D.sqrDistance(pt, point2D);
			if (sqr_dist <= tolerance_cluster_sq)
				return point.createInstance();// return an empty point.
		}

		return point;// return the input point
	}

	static Geometry pointMinusEnvelope_(Point point, Envelope envelope,
			double tolerance, ProgressTracker progress_tracker) {
		Envelope2D env = new Envelope2D();
		envelope.queryEnvelope2D(env);
		env.inflate(tolerance, tolerance);

		Point2D pt = point.getXY();

		if (!env.contains(pt))
			return point;

		return point.createInstance();
	}

	static Geometry pointMinusPoint_(Point point_a, Point point_b,
			double tolerance, ProgressTracker progress_tracker) {
		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;
		double tolerance_cluster_sq = tolerance_cluster * tolerance_cluster;

		Point2D pt_a = point_a.getXY();
		Point2D pt_b = point_b.getXY();

		if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance_cluster_sq)
			return point_a.createInstance(); // return empty point

		return point_a;
	}

	static Geometry multiPointMinusPolygon_(MultiPoint multi_point,
			Polygon polygon, double tolerance, ProgressTracker progress_tracker) {
		Envelope2D env = new Envelope2D();
		polygon.queryEnvelope2D(env);
		env.inflate(tolerance, tolerance);

		int point_count = multi_point.getPointCount();

		boolean b_found_covered = false;
		boolean[] covered = new boolean[point_count];
		for (int i = 0; i < point_count; i++)
			covered[i] = false;

		Point2D pt = new Point2D();

		for (int i = 0; i < point_count; i++) {
			multi_point.getXY(i, pt);

			if (!env.contains(pt))
				continue;

			PolygonUtils.PiPResult result = PolygonUtils.isPointInPolygon2D(
					polygon, pt, tolerance);

			if (result == PolygonUtils.PiPResult.PiPOutside)
				continue;

			b_found_covered = true;
			covered[i] = true;
		}

		if (!b_found_covered)
			return multi_point;

		MultiPoint new_multipoint = (MultiPoint) multi_point.createInstance();

		for (int i = 0; i < point_count; i++) {
			if (!covered[i])
				new_multipoint.add(multi_point, i, i + 1);
		}

		return new_multipoint;
	}

	static Geometry multiPointMinusEnvelope_(MultiPoint multi_point,
			Envelope envelope, double tolerance,
			ProgressTracker progress_tracker) {
		Envelope2D env = new Envelope2D();
		envelope.queryEnvelope2D(env);
		env.inflate(tolerance, tolerance);

		int point_count = multi_point.getPointCount();

		boolean b_found_covered = false;
		boolean[] covered = new boolean[point_count];
		for (int i = 0; i < point_count; i++)
			covered[i] = false;

		Point2D pt = new Point2D();

		for (int i = 0; i < point_count; i++) {
			multi_point.getXY(i, pt);

			if (!env.contains(pt))
				continue;

			b_found_covered = true;
			covered[i] = true;
		}

		if (!b_found_covered)
			return multi_point;

		MultiPoint new_multipoint = (MultiPoint) multi_point.createInstance();

		for (int i = 0; i < point_count; i++) {
			if (!covered[i])
				new_multipoint.add(multi_point, i, i + 1);
		}

		return new_multipoint;
	}

	static Geometry multiPointMinusPoint_(MultiPoint multi_point, Point point,
			double tolerance, ProgressTracker progress_tracker) {
		MultiPointImpl multipointImpl = (MultiPointImpl) (multi_point
				._getImpl());
		AttributeStreamOfDbl position = (AttributeStreamOfDbl) (multipointImpl
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION));
		int point_count = multi_point.getPointCount();
		Point2D point2D = point.getXY();
		Point2D pt = new Point2D();

		boolean b_found_covered = false;
		boolean[] covered = new boolean[point_count];
		for (int i = 0; i < point_count; i++)
			covered[i] = false;

		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;
		double tolerance_cluster_sq = tolerance_cluster * tolerance_cluster;

		for (int i = 0; i < point_count; i++) {
			position.read(2 * i, pt);

			double sqr_dist = Point2D.sqrDistance(pt, point2D);

			if (sqr_dist <= tolerance_cluster_sq) {
				b_found_covered = true;
				covered[i] = true;
			}
		}

		if (!b_found_covered)
			return multi_point;

		MultiPoint new_multipoint = (MultiPoint) (multi_point.createInstance());

		for (int i = 0; i < point_count; i++) {
			if (!covered[i])
				new_multipoint.add(multi_point, i, i + 1);
		}

		return new_multipoint;
	}
}

