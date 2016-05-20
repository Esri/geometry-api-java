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

final class PolygonUtils {

	public enum PiPResult {
		PiPOutside, PiPInside, PiPBoundary
	};

	// enum_class PiPResult { PiPOutside = 0, PiPInside = 1, PiPBoundary = 2};
	/**
	 * Tests if Point is inside the Polygon. Returns PiPOutside if not in
	 * polygon, PiPInside if in the polygon, PiPBoundary is if on the border. It
	 * tests border only if the tolerance is greater than 0, otherwise PiPBoundary cannot
	 * be returned. Note: If the tolerance is not 0, the test is more expensive
	 * because it calculates closest distance from a point to each segment.
	 * 
	 * O(n) complexity, where n is the number of polygon segments.
	 */
	public static PiPResult isPointInPolygon2D(Polygon polygon,
			Point inputPoint, double tolerance) {
		int res = PointInPolygonHelper.isPointInPolygon(polygon, inputPoint,
				tolerance);
		if (res == 0)
			return PiPResult.PiPOutside;
		if (res == 1)
			return PiPResult.PiPInside;

		return PiPResult.PiPBoundary;
	}

	public static PiPResult isPointInPolygon2D(Polygon polygon,
			Point2D inputPoint, double tolerance) {
		int res = PointInPolygonHelper.isPointInPolygon(polygon, inputPoint,
				tolerance);
		if (res == 0)
			return PiPResult.PiPOutside;
		if (res == 1)
			return PiPResult.PiPInside;

		return PiPResult.PiPBoundary;
	}

	static PiPResult isPointInPolygon2D(Polygon polygon, double inputPointXVal,
			double inputPointYVal, double tolerance) {
		int res = PointInPolygonHelper.isPointInPolygon(polygon,
				inputPointXVal, inputPointYVal, tolerance);
		if (res == 0)
			return PiPResult.PiPOutside;
		if (res == 1)
			return PiPResult.PiPInside;

		return PiPResult.PiPBoundary;
	}

	/**
	 * Tests if Point is inside the Polygon's ring. Returns PiPOutside if not in
	 * ring, PiPInside if in the ring, PiPBoundary is if on the border. It tests
	 * border only if the tolerance is greater than 0, otherwise PiPBoundary cannot be
	 * returned. Note: If the tolerance is not 0, the test is more expensive
	 * because it calculates closest distance from a point to each segment.
	 * 
	 * O(n) complexity, where n is the number of ring segments.
	 */
	public static PiPResult isPointInRing2D(Polygon polygon, int iRing,
			Point2D inputPoint, double tolerance) {
		MultiPathImpl polygonImpl = (MultiPathImpl) polygon._getImpl();
		int res = PointInPolygonHelper.isPointInRing(polygonImpl, iRing,
				inputPoint, tolerance, null);
		if (res == 0)
			return PiPResult.PiPOutside;
		if (res == 1)
			return PiPResult.PiPInside;

		// return PiPResult.PiPBoundary;
		return PiPResult.PiPInside; // we do not return PiPBoundary. Overwise,
									// we would have to do more complex
									// calculations to differentiat between
									// internal and external boundaries.
	}

	/**
	 * Tests if Point is inside of the any outer ring of a Polygon. Returns
	 * PiPOutside if not in any outer ring, PiPInside if in the any outer ring,
	 * or on the boundary. PiPBoundary is never returned. Note: If the tolerance
	 * is not 0, the test is more expensive because it calculates closest
	 * distance from a point to each segment.
	 * 
	 * O(n) complexity, where n is the number of polygon segments.
	 */
	public static PiPResult isPointInAnyOuterRing(Polygon polygon,
			Point2D inputPoint, double tolerance) {
		int res = PointInPolygonHelper.isPointInAnyOuterRing(polygon,
				inputPoint, tolerance);
		if (res == 0)
			return PiPResult.PiPOutside;
		if (res == 1)
			return PiPResult.PiPInside;

		// return PiPResult.PiPBoundary;
		return PiPResult.PiPInside; // we do not return PiPBoundary. Overwise,
									// we would have to do more complex
									// calculations to differentiat between
									// internal and external boundaries.
	}

	/**
	 * Tests point is inside the Polygon for an array of points. Returns
	 * PiPOutside if not in polygon, PiPInside if in the polygon, PiPBoundary is
	 * if on the border. It tests border only if the tolerance is greater than 0, otherwise
	 * PiPBoundary cannot be returned. Note: If the tolerance is not 0, the test
	 * is more expensive.
	 * 
	 * O(n*m) complexity, where n is the number of polygon segments, m is the
	 * number of input points.
	 */
	public static void testPointsInPolygon2D(Polygon polygon,
			Point2D[] inputPoints, int count, double tolerance,
			PiPResult[] testResults) {
		if (inputPoints.length < count || testResults.length < count)
			throw new IllegalArgumentException();// GEOMTHROW(invalid_argument);

		for (int i = 0; i < count; i++)
			testResults[i] = isPointInPolygon2D(polygon, inputPoints[i],
					tolerance);
	}

	static void testPointsInPolygon2D(Polygon polygon, double[] xyStreamBuffer,
			int pointCount, double tolerance, PiPResult[] testResults) {
		if (xyStreamBuffer.length / 2 < pointCount
				|| testResults.length < pointCount)
			throw new IllegalArgumentException();// GEOMTHROW(invalid_argument);

		for (int i = 0; i < pointCount; i++)
			testResults[i] = isPointInPolygon2D(polygon, xyStreamBuffer[i * 2],
					xyStreamBuffer[i * 2 + 1], tolerance);
	}

	/**
	 * Tests point is inside an Area Geometry (Envelope, Polygon) for an array
	 * of points. Returns PiPOutside if not in area, PiPInside if in the area,
	 * PiPBoundary is if on the border. It tests border only if the tolerance is
	 * greater than 0, otherwise PiPBoundary cannot be returned. Note: If the tolerance is
	 * not 0, the test is more expensive.
	 * 
	 * O(n*m) complexity, where n is the number of polygon segments, m is the
	 * number of input points.
	 */
	public static void testPointsInArea2D(Geometry polygon,
			Point2D[] inputPoints, int count, double tolerance,
			PiPResult[] testResults) {
		if (polygon.getType() == Geometry.Type.Polygon)
			testPointsInPolygon2D((Polygon) polygon, inputPoints, count,
					tolerance, testResults);
		else if (polygon.getType() == Geometry.Type.Envelope) {
			Envelope2D env2D = new Envelope2D();
			((Envelope) polygon).queryEnvelope2D(env2D);
			_testPointsInEnvelope2D(env2D, inputPoints, count, tolerance,
					testResults);
		} else
			throw new GeometryException("invalid_call");// GEOMTHROW(invalid_call);
	}

	public static void testPointsInArea2D(Geometry polygon,
			double[] xyStreamBuffer, int count, double tolerance,
			PiPResult[] testResults) {
		if (polygon.getType() == Geometry.Type.Polygon)
			testPointsInPolygon2D((Polygon) polygon, xyStreamBuffer, count,
					tolerance, testResults);
		else if (polygon.getType() == Geometry.Type.Envelope) {
			Envelope2D env2D = new Envelope2D();
			((Envelope) polygon).queryEnvelope2D(env2D);
			_testPointsInEnvelope2D(env2D, xyStreamBuffer, count, tolerance,
					testResults);
		} else
			throw new GeometryException("invalid_call");// GEOMTHROW(invalid_call);
	}

	private static void _testPointsInEnvelope2D(Envelope2D env2D,
			Point2D[] inputPoints, int count, double tolerance,
			PiPResult[] testResults) {
		if (inputPoints.length < count || testResults.length < count)
			throw new IllegalArgumentException();

		if (env2D.isEmpty()) {
			for (int i = 0; i < count; i++)
				testResults[i] = PiPResult.PiPOutside;
			return;
		}

		Envelope2D envIn = env2D; // note for java port - assignement by value
		envIn.inflate(-tolerance * 0.5, -tolerance * 0.5);
		Envelope2D envOut = env2D;// note for java port - assignement by value
		envOut.inflate(tolerance * 0.5, tolerance * 0.5);
		for (int i = 0; i < count; i++) {
			if (envIn.contains(inputPoints[i]))
				testResults[i] = PiPResult.PiPInside;
			else if (!envOut.contains(inputPoints[i]))
				testResults[i] = PiPResult.PiPOutside;
			else
				testResults[i] = PiPResult.PiPBoundary;
		}
	}

	private static void _testPointsInEnvelope2D(Envelope2D env2D,
			double[] xyStreamBuffer, int pointCount, double tolerance,
			PiPResult[] testResults) {
		if (xyStreamBuffer.length / 2 < pointCount
				|| testResults.length < pointCount)
			throw new IllegalArgumentException();

		if (env2D.isEmpty()) {
			for (int i = 0; i < pointCount; i++)
				testResults[i] = PiPResult.PiPOutside;
			return;
		}

		Envelope2D envIn = env2D; // note for java port - assignement by value
		envIn.inflate(-tolerance * 0.5, -tolerance * 0.5);
		Envelope2D envOut = env2D;// note for java port - assignement by value
		envOut.inflate(tolerance * 0.5, tolerance * 0.5);
		for (int i = 0; i < pointCount; i++) {
			if (envIn
					.contains(xyStreamBuffer[i * 2], xyStreamBuffer[i * 2 + 1]))
				testResults[i] = PiPResult.PiPInside;
			else if (!envIn.contains(xyStreamBuffer[i * 2],
					xyStreamBuffer[i * 2 + 1]))
				testResults[i] = PiPResult.PiPOutside;
			else
				testResults[i] = PiPResult.PiPBoundary;
		}
	}

	static void testPointsOnSegment_(Segment seg, Point2D[] input_points,
			int count, double tolerance, PolygonUtils.PiPResult[] test_results) {
		for (int i = 0; i < count; i++) {
			if (seg.isIntersecting(input_points[i], tolerance))
				test_results[i] = PiPResult.PiPBoundary;
			else
				test_results[i] = PiPResult.PiPOutside;
		}
	}

	static void testPointsOnPolyline2D_(Polyline poly, Point2D[] input_points,
			int count, double tolerance, PolygonUtils.PiPResult[] test_results) {
		MultiPathImpl mp_impl = (MultiPathImpl) poly._getImpl();
		GeometryAccelerators accel = mp_impl._getAccelerators();
		RasterizedGeometry2D rgeom = null;
		if (accel != null) {
			rgeom = accel.getRasterizedGeometry();
		}

		int pointsLeft = count;
		for (int i = 0; i < count; i++) {
			test_results[i] = PiPResult.PiPInside;// set to impossible value

			if (rgeom != null) {
				Point2D input_point = input_points[i];
				RasterizedGeometry2D.HitType hit = rgeom.queryPointInGeometry(
						input_point.x, input_point.y);
				if (hit == RasterizedGeometry2D.HitType.Outside) {
					test_results[i] = PiPResult.PiPOutside;
					pointsLeft--;
				}
			}
		}

		if (pointsLeft != 0) {
			SegmentIteratorImpl iter = mp_impl.querySegmentIterator();
			while (iter.nextPath() && pointsLeft != 0) {
				while (iter.hasNextSegment() && pointsLeft != 0) {
					Segment segment = iter.nextSegment();
					for (int i = 0; i < count && pointsLeft != 0; i++) {
						if (test_results[i] == PiPResult.PiPInside) {
							if (segment.isIntersecting(input_points[i],
									tolerance)) {
								test_results[i] = PiPResult.PiPBoundary;
								pointsLeft--;
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < count; i++) {
			if (test_results[i] == PiPResult.PiPInside)
				test_results[i] = PiPResult.PiPOutside;
		}
	}

	static void testPointsOnLine2D(Geometry line, Point2D[] input_points,
			int count, double tolerance, PolygonUtils.PiPResult[] test_results) {
		Geometry.Type gt = line.getType();
		if (gt == Geometry.Type.Polyline)
			testPointsOnPolyline2D_((Polyline) line, input_points, count,
					tolerance, test_results);
		else if (Geometry.isSegment(gt.value())) {
			testPointsOnSegment_((Segment) line, input_points, count,
					tolerance, test_results);
		} else
			throw new GeometryException("Invalid call.");
	}

}
