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

class OperatorSymmetricDifferenceLocal extends OperatorSymmetricDifference {

	@Override
	public GeometryCursor execute(GeometryCursor inputGeometries,
			GeometryCursor rightGeometry, SpatialReference sr,
			ProgressTracker progressTracker) {
		return new OperatorSymmetricDifferenceCursor(inputGeometries,
				rightGeometry, sr, progressTracker);
	}

	@Override
	public Geometry execute(Geometry leftGeometry, Geometry rightGeometry,
			SpatialReference sr, ProgressTracker progressTracker) {
		SimpleGeometryCursor leftGeomCurs = new SimpleGeometryCursor(
				leftGeometry);
		SimpleGeometryCursor rightGeomCurs = new SimpleGeometryCursor(
				rightGeometry);
		GeometryCursor geometryCursor = execute(leftGeomCurs, rightGeomCurs,
				sr, progressTracker);
		return geometryCursor.next();
	}

	static Geometry symmetricDifference(Geometry geometry_a,
			Geometry geometry_b, SpatialReference spatial_reference,
			ProgressTracker progress_tracker) {
		int dim_a = geometry_a.getDimension();
		int dim_b = geometry_b.getDimension();

		if (geometry_a.isEmpty() && geometry_b.isEmpty())
			return dim_a > dim_b ? geometry_a : geometry_b;

		if (geometry_a.isEmpty())
			return geometry_b;
		if (geometry_b.isEmpty())
			return geometry_a;

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D(), env_merged = new Envelope2D();
		geometry_a.queryEnvelope2D(env_a);
		geometry_b.queryEnvelope2D(env_b);
		env_merged.setCoords(env_a);
		env_merged.merge(env_b);

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				spatial_reference, env_merged, false);

		int type_a = geometry_a.getType().value();
		int type_b = geometry_b.getType().value();

		if (type_a == Geometry.GeometryType.Point
				&& type_b == Geometry.GeometryType.Point)
			return pointSymDiffPoint_((Point) (geometry_a),
					(Point) (geometry_b), tolerance, progress_tracker);

		if (type_a != type_b) {
			if (dim_a > 0 || dim_b > 0)
				return dim_a > dim_b ? geometry_a : geometry_b;

			// Multi_point/Point case

			if (type_a == Geometry.GeometryType.MultiPoint)
				return multiPointSymDiffPoint_((MultiPoint) (geometry_a),
						(Point) (geometry_b), tolerance, progress_tracker);

			return multiPointSymDiffPoint_((MultiPoint) (geometry_b),
					(Point) (geometry_a), tolerance, progress_tracker);
		}

		return TopologicalOperations.symmetricDifference(geometry_a,
				geometry_b, spatial_reference, progress_tracker);
	}

	static Geometry pointSymDiffPoint_(Point point_a, Point point_b,
			double tolerance, ProgressTracker progress_tracker) {
		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;
		double tolerance_cluster_sq = tolerance_cluster * tolerance_cluster;

		Point2D pt_a = point_a.getXY();
		Point2D pt_b = point_b.getXY();

		MultiPoint multi_point = new MultiPoint(point_a.getDescription());

		if (Point2D.sqrDistance(pt_a, pt_b) > tolerance_cluster_sq) {
			multi_point.add(point_a);
			multi_point.add(point_b);
		}

		return multi_point;
	}

	static Geometry multiPointSymDiffPoint_(MultiPoint multi_point,
			Point point, double tolerance, ProgressTracker progress_tracker) {
		MultiPointImpl multipointImpl = (MultiPointImpl) (multi_point
				._getImpl());
		AttributeStreamOfDbl position = (AttributeStreamOfDbl) (multipointImpl
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION));
		int point_count = multi_point.getPointCount();
		Point2D point2D = point.getXY();

		MultiPoint new_multipoint = (MultiPoint) (multi_point.createInstance());
		double tolerance_cluster = tolerance * Math.sqrt(2.0) * 1.00001;

		Envelope2D env = new Envelope2D();
		multi_point.queryEnvelope2D(env);
		env.inflate(tolerance_cluster, tolerance_cluster);

		if (env.contains(point2D)) {
			double tolerance_cluster_sq = tolerance_cluster * tolerance_cluster;

			boolean b_found_covered = false;
			boolean[] covered = new boolean[point_count];
			for (int i = 0; i < point_count; i++)
				covered[i] = false;

			for (int i = 0; i < point_count; i++) {
				double x = position.read(2 * i);
				double y = position.read(2 * i + 1);

				double dx = x - point2D.x;
				double dy = y - point2D.y;

				if (dx * dx + dy * dy <= tolerance_cluster_sq) {
					b_found_covered = true;
					covered[i] = true;
				}
			}

			if (!b_found_covered) {
				new_multipoint.add(multi_point, 0, point_count);
				new_multipoint.add(point);
			} else {
				for (int i = 0; i < point_count; i++) {
					if (!covered[i])
						new_multipoint.add(multi_point, i, i + 1);
				}
			}
		} else {
			new_multipoint.add(multi_point, 0, point_count);
			new_multipoint.add(point);
		}

		return new_multipoint;
	}
}
