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

class OperatorDistanceLocal extends OperatorDistance {

	/**
	 * Performs the Distance operation on two geometries
	 * 
	 * @return Returns a double.
	 */
	@Override
	public double execute(Geometry geom1, Geometry geom2,
			ProgressTracker progressTracker) {
		if (null == geom1 || null == geom2) {
			throw new IllegalArgumentException();
		}

		Geometry geometryA = geom1;
		Geometry geometryB = geom2;

		if (geometryA.isEmpty() || geometryB.isEmpty())
			return NumberUtils.TheNaN;		

		Polygon polygonA;
		Polygon polygonB;
		MultiPoint multiPointA;
		MultiPoint multiPointB;

		// if geometryA is an envelope use a polygon instead (if geom1 was
		// folded, then geometryA will already be a polygon)
		// if geometryA is a point use a multipoint instead
		Geometry.Type gtA = geometryA.getType();
		Geometry.Type gtB = geometryB.getType();
		if (gtA == Geometry.Type.Point) {
			if (gtB == Geometry.Type.Point) {
				return Point2D.distance(((Point)geometryA).getXY(), ((Point)geometryB).getXY());
			}
			else if (gtB == Geometry.Type.Envelope) {
				Envelope2D envB = new Envelope2D();
				geometryB.queryEnvelope2D(envB);
				return envB.distance(((Point)geometryA).getXY());
			}
			
			multiPointA = new MultiPoint();
			multiPointA.add((Point) geometryA);
			geometryA = multiPointA;
		} else if (gtA == Geometry.Type.Envelope) {
			if (gtB == Geometry.Type.Envelope) {
				Envelope2D envA = new Envelope2D();
				geometryA.queryEnvelope2D(envA);
				Envelope2D envB = new Envelope2D();
				geometryB.queryEnvelope2D(envB);
				return envB.distance(envA);
			}
			polygonA = new Polygon();
			polygonA.addEnvelope((Envelope) geometryA, false);
			geometryA = polygonA;
		}

		// if geom_2 is an envelope use a polygon instead
		// if geom_2 is a point use a multipoint instead
		if (gtB == Geometry.Type.Point) {
			multiPointB = new MultiPoint();
			multiPointB.add((Point) geometryB);
			geometryB = multiPointB;
		} else if (gtB == Geometry.Type.Envelope) {
			polygonB = new Polygon();
			polygonB.addEnvelope((Envelope) geometryB, false);
			geometryB = polygonB;
		}

		DistanceCalculator distanceCalculator = new DistanceCalculator(
				progressTracker);
		double distance = distanceCalculator.calculate(geometryA, geometryB);
		return distance;
	}

	// Implementation of distance algorithm.
	class DistanceCalculator {
		private ProgressTracker m_progressTracker;
		private Envelope2D m_env2DgeometryA;
		private Envelope2D m_env2DgeometryB;

		private void swapEnvelopes_() {
			double temp;
			// swap xmin
			temp = m_env2DgeometryA.xmin;
			m_env2DgeometryA.xmin = m_env2DgeometryB.xmin;
			m_env2DgeometryB.xmin = temp;
			// swap xmax
			temp = m_env2DgeometryA.xmax;
			m_env2DgeometryA.xmax = m_env2DgeometryB.xmax;
			m_env2DgeometryB.xmax = temp;
			// swap ymin
			temp = m_env2DgeometryA.ymin;
			m_env2DgeometryA.ymin = m_env2DgeometryB.ymin;
			m_env2DgeometryB.ymin = temp;
			// swap ymax
			temp = m_env2DgeometryA.ymax;
			m_env2DgeometryA.ymax = m_env2DgeometryB.ymax;
			m_env2DgeometryB.ymax = temp;
		}

		private double executeBruteForce_(/* const */Geometry geometryA, /* const */
		Geometry geometryB) {
			if ((m_progressTracker != null)
					&& !(m_progressTracker.progress(-1, -1)))
				throw new RuntimeException("user_canceled");

			boolean geometriesAreDisjoint = !m_env2DgeometryA
					.isIntersecting(m_env2DgeometryB);
			if (Geometry.isMultiPath(geometryA.getType().value())
					&& Geometry.isMultiPath(geometryB.getType().value())) { // MultiPath
																			// vs.
																			// MultiPath
																			// choose
																			// the
																			// multipath
																			// with
																			// more
																			// points
																			// to
																			// be
																			// geometryA,
																			// this
																			// way
																			// more
																			// of
																			// geometryA
																			// segments
																			// can
																			// be
																			// disqualified
																			// more
																			// quickly
																			// by
																			// testing
																			// segmentA
																			// envelope
																			// vs.
																			// geometryB
																			// envelope
				if (((MultiPath) geometryA).getPointCount() > ((MultiPath) geometryB)
						.getPointCount())
					return bruteForceMultiPathMultiPath_((MultiPath) geometryA,
							(MultiPath) geometryB, geometriesAreDisjoint);
				swapEnvelopes_();
				double answer = bruteForceMultiPathMultiPath_(
						(MultiPath) geometryB, (MultiPath) geometryA,
						geometriesAreDisjoint);
				swapEnvelopes_();
				return answer;
			} else if (geometryA.getType() == Geometry.Type.MultiPoint
					&& Geometry.isMultiPath(geometryB.getType().value())) { // MultiPoint
																			// vs.
																			// MultiPath
				swapEnvelopes_();
				double answer = bruteForceMultiPathMultiPoint_(
						(MultiPath) geometryB, (MultiPoint) geometryA,
						geometriesAreDisjoint);
				swapEnvelopes_();
				return answer;
			} else if (geometryB.getType() == Geometry.Type.MultiPoint
					&& Geometry.isMultiPath(geometryA.getType().value())) { // MultiPath
																			// vs.
																			// MultiPoint
				return bruteForceMultiPathMultiPoint_((MultiPath) geometryA,
						(MultiPoint) geometryB, geometriesAreDisjoint);
			} else if (geometryA.getType() == Geometry.Type.MultiPoint
					&& geometryB.getType() == Geometry.Type.MultiPoint) { // MultiPoint
																			// vs.
																			// MultiPoint
																			// choose
																			// the
																			// multipoint
																			// with
																			// more
																			// vertices
																			// to
																			// be
																			// the
																			// "geometryA",
																			// this
																			// way
																			// more
																			// points
																			// can
																			// be
																			// potentially
																			// excluded
																			// by
																			// envelope
																			// distance
																			// tests.
				if (((MultiPoint) geometryA).getPointCount() > ((MultiPoint) geometryB)
						.getPointCount())
					return bruteForceMultiPointMultiPoint_(
							(MultiPoint) geometryA, (MultiPoint) geometryB,
							geometriesAreDisjoint);
				swapEnvelopes_();
				double answer = bruteForceMultiPointMultiPoint_(
						(MultiPoint) geometryB, (MultiPoint) geometryA,
						geometriesAreDisjoint);
				swapEnvelopes_();
				return answer;
			}
			return 0.0;
		}

		private double bruteForceMultiPathMultiPath_(
		/* const */MultiPath geometryA, /* const */MultiPath geometryB,
				boolean geometriesAreDisjoint) {
			// It may be beneficial to have the geometry with less vertices
			// always be geometryA.
			SegmentIterator segIterA = geometryA.querySegmentIterator();
			SegmentIterator segIterB = geometryB.querySegmentIterator();
			Envelope2D env2DSegmentA = new Envelope2D();
			Envelope2D env2DSegmentB = new Envelope2D();

			double minSqrDistance = NumberUtils.doubleMax();

			if (!geometriesAreDisjoint) {
				// Geometries might be non-disjoint. Check if they intersect
				// using point-in-polygon tests
				if (weakIntersectionTest_(geometryA, geometryB, segIterA,
						segIterB))
					return 0.0;
			}

			// if geometries are known disjoint, don't bother to do any tests
			// for polygon containment

			// nested while-loop insanity
			while (segIterA.nextPath()) {
				while (segIterA.hasNextSegment()) {
					/* const */Segment segmentA = segIterA.nextSegment();
					segmentA.queryEnvelope2D(env2DSegmentA);
					if (env2DSegmentA.sqrDistance(m_env2DgeometryB) > minSqrDistance)
						continue;

					while (segIterB.nextPath()) {
						while (segIterB.hasNextSegment()) {
							/* const */Segment segmentB = segIterB
									.nextSegment();
							segmentB.queryEnvelope2D(env2DSegmentB);
							if (env2DSegmentA.sqrDistance(env2DSegmentB) < minSqrDistance) {
								// get distance between segments
								double sqrDistance = segmentA.distance(
										segmentB, geometriesAreDisjoint);
								sqrDistance *= sqrDistance;
								if (sqrDistance < minSqrDistance) {
									if (sqrDistance == 0.0)
										return 0.0;

									minSqrDistance = sqrDistance;
								}
							}
						}
					}
					segIterB.resetToFirstPath();
				}
			}

			return Math.sqrt(minSqrDistance);
		}

		private double bruteForceMultiPathMultiPoint_(
		/* const */MultiPath geometryA, /* const */
		MultiPoint geometryB, boolean geometriesAreDisjoint) {
			SegmentIterator segIterA = geometryA.querySegmentIterator();

			Envelope2D env2DSegmentA = new Envelope2D();

			double minSqrDistance = NumberUtils.doubleMax();

			Point2D inputPoint = new Point2D();
			double t = -1;
			double sqrDistance = minSqrDistance;
			/* const */MultiPointImpl multiPointImplB = (MultiPointImpl) geometryB
					._getImpl();
			int pointCountB = multiPointImplB.getPointCount();
			boolean bDoPiPTest = !geometriesAreDisjoint
					&& (geometryA.getType() == Geometry.Type.Polygon);

			while (segIterA.nextPath()) {
				while (segIterA.hasNextSegment()) {
					/* const */Segment segmentA = segIterA.nextSegment();
					segmentA.queryEnvelope2D(env2DSegmentA);
					// if multipointB has only 1 vertex then it is faster to not
					// test for
					// env2DSegmentA.distance(env2DgeometryB)
					if (pointCountB > 1
							&& env2DSegmentA.sqrDistance(m_env2DgeometryB) > minSqrDistance)
						continue;

					for (int i = 0; i < pointCountB; i++) {
						multiPointImplB.getXY(i, inputPoint);
						if (bDoPiPTest) {
							// Test for polygon containment. This takes the
							// place of a more general intersection test at the
							// beginning of the operator
							if (PolygonUtils.isPointInPolygon2D(
									(Polygon) geometryA, inputPoint, 0) != PolygonUtils.PiPResult.PiPOutside)
								return 0.0;
						}

						t = segmentA.getClosestCoordinate(inputPoint, false);
						inputPoint.sub(segmentA.getCoord2D(t));
						sqrDistance = inputPoint.sqrLength();
						if (sqrDistance < minSqrDistance) {
							if (sqrDistance == 0.0)
								return 0.0;

							minSqrDistance = sqrDistance;
						}
					}

					// No need to do point-in-polygon anymore (if it is a
					// polygon vs polyline)
					bDoPiPTest = false;
				}
			}
			return Math.sqrt(minSqrDistance);
		}

		private double bruteForceMultiPointMultiPoint_(
		/* const */MultiPoint geometryA, /* const */
		MultiPoint geometryB, boolean geometriesAreDisjoint) {
			double minSqrDistance = NumberUtils.doubleMax();

			Point2D pointA = new Point2D();
			Point2D pointB = new Point2D();

			double sqrDistance = minSqrDistance;
			/* const */MultiPointImpl multiPointImplA = (/* const */MultiPointImpl) geometryA
					._getImpl();
			/* const */MultiPointImpl multiPointImplB = (/* const */MultiPointImpl) geometryB
					._getImpl();
			int pointCountA = multiPointImplA.getPointCount();
			int pointCountB = multiPointImplB.getPointCount();
			for (int i = 0; i < pointCountA; i++) {
				multiPointImplA.getXY(i, pointA);

				if (pointCountB > 1
						&& m_env2DgeometryB.sqrDistance(pointA) > minSqrDistance)
					continue;

				for (int j = 0; j < pointCountB; j++) {
					multiPointImplB.getXY(j, pointB);
					sqrDistance = Point2D.sqrDistance(pointA, pointB);
					if (sqrDistance < minSqrDistance) {
						if (sqrDistance == 0.0)
							return 0.0;

						minSqrDistance = sqrDistance;
					}
				}
			}

			return Math.sqrt(minSqrDistance);
		}

		// resets Iterators if they are used.
		private boolean weakIntersectionTest_(/* const */Geometry geometryA, /* const */
		Geometry geometryB, SegmentIterator segIterA, SegmentIterator segIterB) {
			if (geometryA.getType() == Geometry.Type.Polygon) {
				// test PolygonA vs. first segment of each of geometryB's paths
				while (segIterB.nextPath()) {
					if (segIterB.hasNextSegment()) {
						/* const */Segment segmentB = segIterB.nextSegment();
						if (PolygonUtils.isPointInPolygon2D(
								(Polygon) geometryA, segmentB.getEndXY(), 0) != PolygonUtils.PiPResult.PiPOutside)
							return true;
					}
				}
				segIterB.resetToFirstPath();
			}

			if (geometryB.getType() == Geometry.Type.Polygon) {
				// test PolygonB vs. first segment of each of geometryA's paths
				while (segIterA.nextPath()) {
					if (segIterA.hasNextSegment()) {
						/* const */Segment segmentA = segIterA.nextSegment();
						if (PolygonUtils.isPointInPolygon2D(
								(Polygon) geometryB, segmentA.getEndXY(), 0) != PolygonUtils.PiPResult.PiPOutside)
							return true;
					}
				}
				segIterA.resetToFirstPath();
			}
			return false;
		}

		DistanceCalculator(ProgressTracker progressTracker) {
			m_progressTracker = progressTracker;
			m_env2DgeometryA = new Envelope2D();
			m_env2DgeometryA.setEmpty();
			m_env2DgeometryB = new Envelope2D();
			m_env2DgeometryB.setEmpty();
		}

		double calculate(/* const */Geometry geometryA, /* const */
		Geometry geometryB) {
			if (geometryA.isEmpty() || geometryB.isEmpty())
				return NumberUtils.TheNaN;

			geometryA.queryEnvelope2D(m_env2DgeometryA);
			geometryB.queryEnvelope2D(m_env2DgeometryB);
			return executeBruteForce_(geometryA, geometryB);
		}
	}
}
