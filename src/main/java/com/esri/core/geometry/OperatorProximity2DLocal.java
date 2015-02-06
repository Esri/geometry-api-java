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

import com.esri.core.geometry.VertexDescription.Semantics;
import java.util.ArrayList;
import java.util.Collections;

class OperatorProximity2DLocal extends OperatorProximity2D {

	class Side_helper {
		int m_i1;
		int m_i2;
		boolean m_bRight1;
		boolean m_bRight2;

		void reset() {
			m_i1 = -1;
			m_i2 = -1;
			m_bRight1 = false;
			m_bRight2 = false;
		}

		int find_non_degenerate(SegmentIterator segIter, int vertexIndex,
				int pathIndex) {
			segIter.resetToVertex(vertexIndex, pathIndex);

			while (segIter.hasNextSegment()) {
				Segment segment = segIter.nextSegment();
				double length = segment.calculateLength2D();

				if (length != 0.0)
					return segIter.getStartPointIndex();
			}

			segIter.resetToVertex(vertexIndex, pathIndex);

			while (segIter.hasPreviousSegment()) {
				Segment segment = segIter.previousSegment();
				double length = segment.calculateLength2D();

				if (length != 0)
					return segIter.getStartPointIndex();
			}

			return -1;
		}

		int find_prev_non_degenerate(SegmentIterator segIter, int index) {
			segIter.resetToVertex(index, -1);

			while (segIter.hasPreviousSegment()) {
				Segment segment = segIter.previousSegment();
				double length = segment.calculateLength2D();

				if (length != 0)
					return segIter.getStartPointIndex();
			}

			return -1;
		}

		int find_next_non_degenerate(SegmentIterator segIter, int index) {
			segIter.resetToVertex(index, -1);
			segIter.nextSegment();

			while (segIter.hasNextSegment()) {
				Segment segment = segIter.nextSegment();
				double length = segment.calculateLength2D();

				if (length != 0)
					return segIter.getStartPointIndex();
			}

			return -1;
		}

		void find_analysis_pair_from_index(Point2D inputPoint,
				SegmentIterator segIter, int vertexIndex, int pathIndex) {
			m_i1 = find_non_degenerate(segIter, vertexIndex, pathIndex);

			if (m_i1 != -1) {
				segIter.resetToVertex(m_i1, -1);
				Segment segment1 = segIter.nextSegment();
				double t1 = segment1.getClosestCoordinate(inputPoint, false);
				Point2D p1 = segment1.getCoord2D(t1);
				double d1 = Point2D.sqrDistance(p1, inputPoint);
				Point2D pq = new Point2D();
				pq.setCoords(p1);
				pq.sub(segment1.getStartXY());
				Point2D pr = new Point2D();
				pr.setCoords(inputPoint);
				pr.sub(segment1.getStartXY());
				m_bRight1 = (pq.crossProduct(pr) < 0);

				m_i2 = find_next_non_degenerate(segIter, m_i1);
				if (m_i2 != -1) {
					segIter.resetToVertex(m_i2, -1);
					Segment segment2 = segIter.nextSegment();
					double t2 = segment2
							.getClosestCoordinate(inputPoint, false);
					Point2D p2 = segment2.getCoord2D(t2);
					double d2 = Point2D.sqrDistance(p2, inputPoint);

					if (d2 > d1) {
						m_i2 = -1;
					} else {
						pq.setCoords(p2);
						pq.sub(segment2.getStartXY());
						pr.setCoords(inputPoint);
						pr.sub(segment2.getStartXY());
						m_bRight2 = (pq.crossProduct(pr) < 0);
					}
				}

				if (m_i2 == -1) {
					m_i2 = find_prev_non_degenerate(segIter, m_i1);
					if (m_i2 != -1) {
						segIter.resetToVertex(m_i2, -1);
						Segment segment2 = segIter.nextSegment();
						double t2 = segment2.getClosestCoordinate(inputPoint,
								false);
						Point2D p2 = segment2.getCoord2D(t2);
						double d2 = Point2D.sqrDistance(p2, inputPoint);

						if (d2 > d1)
							m_i2 = -1;
						else {
							pq.setCoords(p2);
							pq.sub(segment2.getStartXY());
							pr.setCoords(inputPoint);
							pr.sub(segment2.getStartXY());
							m_bRight2 = (pq.crossProduct(pr) < 0);

							int itemp = m_i1;
							m_i1 = m_i2;
							m_i2 = itemp;

							boolean btemp = m_bRight1;
							m_bRight1 = m_bRight2;
							m_bRight2 = btemp;
						}
					}
				}
			}
		}

		// Try to find two segements that are not degenerate
		boolean calc_side(Point2D inputPoint, boolean bRight,
				MultiPath multipath, int vertexIndex, int pathIndex) {
			SegmentIterator segIter = multipath.querySegmentIterator();

			find_analysis_pair_from_index(inputPoint, segIter, vertexIndex,
					pathIndex);

			if (m_i1 != -1 && m_i2 == -1) {// could not find a pair of segments
				return m_bRight1;
			}

			if (m_i1 != -1 && m_i2 != -1) {
				if (m_bRight1 == m_bRight2)
					return m_bRight1;// no conflicting result for the side
				else {
					// the conflicting result, that we are trying to resolve,
					// happens in the obtuse (outer) side of the turn only.
					segIter.resetToVertex(m_i1, -1);
					Segment segment1 = segIter.nextSegment();
					Point2D tang1 = segment1._getTangent(1.0);

					segIter.resetToVertex(m_i2, -1);
					Segment segment2 = segIter.nextSegment();
					Point2D tang2 = segment2._getTangent(0.0);

					double cross = tang1.crossProduct(tang2);

					if (cross >= 0) // the obtuse angle is on the right side
					{
						return true;
					} else // the obtuse angle is on the right side
					{
						return false;
					}
				}
			} else {
				assert (m_i1 == -1 && m_i2 == -1);
				return bRight;// could not resolve the side. So just return the
								// old value.
			}
		}
	}

	@Override
	public Proximity2DResult getNearestCoordinate(Geometry geom,
			Point inputPoint, boolean bTestPolygonInterior) {

		return getNearestCoordinate(geom, inputPoint, bTestPolygonInterior,
				false);
	}

	@Override
	public Proximity2DResult getNearestCoordinate(Geometry geom,
			Point inputPoint, boolean bTestPolygonInterior,
			boolean bCalculateLeftRightSide) {
		if (geom.isEmpty())
			return new Proximity2DResult();

		Point2D inputPoint2D = inputPoint.getXY();

		Geometry proxmityTestGeom = geom;
		int gt = geom.getType().value();

		if (gt == Geometry.GeometryType.Envelope) {
			Polygon polygon = new Polygon();
			polygon.addEnvelope((Envelope) geom, false);
			proxmityTestGeom = polygon;
			gt = Geometry.GeometryType.Polygon;
		}
		switch (gt) {
		case Geometry.GeometryType.Point:
			return pointGetNearestVertex((Point) proxmityTestGeom, inputPoint2D);
		case Geometry.GeometryType.MultiPoint:
			return multiVertexGetNearestVertex(
					(MultiVertexGeometry) proxmityTestGeom, inputPoint2D);
		case Geometry.GeometryType.Polyline:
		case Geometry.GeometryType.Polygon:
			return multiPathGetNearestCoordinate((MultiPath) proxmityTestGeom,
					inputPoint2D, bTestPolygonInterior, bCalculateLeftRightSide);
		default: {
			throw new GeometryException("not implemented");
		}
		}
	}

	@Override
	public Proximity2DResult getNearestVertex(Geometry geom, Point inputPoint) {
		if (geom.isEmpty())
			return new Proximity2DResult();

		Point2D inputPoint2D = inputPoint.getXY();

		Geometry proxmityTestGeom = geom;
		int gt = geom.getType().value();

		if (gt == Geometry.GeometryType.Envelope) {
			Polygon polygon = new Polygon();
			polygon.addEnvelope((Envelope) geom, false);
			proxmityTestGeom = polygon;
			gt = Geometry.GeometryType.Polygon;
		}
		switch (gt) {
		case Geometry.GeometryType.Point:
			return pointGetNearestVertex((Point) proxmityTestGeom, inputPoint2D);
		case Geometry.GeometryType.MultiPoint:
		case Geometry.GeometryType.Polyline:
		case Geometry.GeometryType.Polygon:
			return multiVertexGetNearestVertex(
					(MultiVertexGeometry) proxmityTestGeom, inputPoint2D);
		default: {
			throw new GeometryException("not implemented");
		}
		}
	}

	@Override
	public Proximity2DResult[] getNearestVertices(Geometry geom,
			Point inputPoint, double searchRadius, int maxVertexCountToReturn) {
		if (maxVertexCountToReturn < 0)
			throw new IllegalArgumentException();

		if (geom.isEmpty())
			return new Proximity2DResult[] {};

		Point2D inputPoint2D = inputPoint.getXY();

		Geometry proxmityTestGeom = geom;
		int gt = geom.getType().value();

		if (gt == Geometry.GeometryType.Envelope) {
			Polygon polygon = new Polygon();
			polygon.addEnvelope((Envelope) geom, false);
			proxmityTestGeom = polygon;
			gt = Geometry.GeometryType.Polygon;
		}
		switch (gt) {
		case Geometry.GeometryType.Point:
			return pointGetNearestVertices((Point) proxmityTestGeom,
					inputPoint2D, searchRadius, maxVertexCountToReturn);
		case Geometry.GeometryType.MultiPoint:
		case Geometry.GeometryType.Polyline:
		case Geometry.GeometryType.Polygon:
			return multiVertexGetNearestVertices(
					(MultiVertexGeometry) proxmityTestGeom, inputPoint2D,
					searchRadius, maxVertexCountToReturn);
		default: {
			throw new GeometryException("not implemented");
		}
		}
	}

	Proximity2DResult multiPathGetNearestCoordinate(MultiPath geom,
			Point2D inputPoint, boolean bTestPolygonInterior,
			boolean bCalculateLeftRightSide) {
		if (geom.getType() == Geometry.Type.Polygon && bTestPolygonInterior) {
			Envelope2D env = new Envelope2D();
			geom.queryEnvelope2D(env);
			double tolerance = InternalUtils.calculateToleranceFromGeometry(
					null, env, false);

			PolygonUtils.PiPResult pipResult;

			if (bCalculateLeftRightSide)
				pipResult = PolygonUtils.isPointInPolygon2D((Polygon) geom,
						inputPoint, 0.0);
			else
				pipResult = PolygonUtils.isPointInPolygon2D((Polygon) geom,
						inputPoint, tolerance);

			if (pipResult != PolygonUtils.PiPResult.PiPOutside) {
				Proximity2DResult result = new Proximity2DResult(inputPoint, 0,
						0.0);

				if (bCalculateLeftRightSide)
					result.setRightSide(true);

				return result;
			}
		}

		SegmentIterator segIter = geom.querySegmentIterator();

		Point2D closest = new Point2D();
		int closestVertexIndex = -1;
		int closestPathIndex = -1;
		double closestDistanceSq = NumberUtils.doubleMax();
		boolean bRight = false;
		int num_candidates = 0;

		while (segIter.nextPath()) {
			while (segIter.hasNextSegment()) {
				Segment segment = segIter.nextSegment();
				double t = segment.getClosestCoordinate(inputPoint, false);

				Point2D point = segment.getCoord2D(t);

				double distanceSq = Point2D.sqrDistance(point, inputPoint);
				if (distanceSq < closestDistanceSq) {
					num_candidates = 1;
					closest = point;
					closestVertexIndex = segIter.getStartPointIndex();
					closestPathIndex = segIter.getPathIndex();
					closestDistanceSq = distanceSq;
				} else if (distanceSq == closestDistanceSq) {
					num_candidates++;
				}
			}
		}

		Proximity2DResult result = new Proximity2DResult(closest,
				closestVertexIndex, Math.sqrt(closestDistanceSq));

		if (bCalculateLeftRightSide) {
			segIter.resetToVertex(closestVertexIndex, closestPathIndex);
			Segment segment = segIter.nextSegment();
			bRight = (Point2D.orientationRobust(inputPoint,
					segment.getStartXY(), segment.getEndXY()) < 0);

			if (num_candidates > 1) {
				Side_helper sideHelper = new Side_helper();
				sideHelper.reset();
				bRight = sideHelper.calc_side(inputPoint, bRight, geom,
						closestVertexIndex, closestPathIndex);
			}

			result.setRightSide(bRight);
		}

		return result;
	}

	Proximity2DResult pointGetNearestVertex(Point geom, Point2D input_point) {
		Point2D pt = geom.getXY();
		double distance = Point2D.distance(pt, input_point);
		return new Proximity2DResult(pt, 0, distance);
	}

	Proximity2DResult multiVertexGetNearestVertex(MultiVertexGeometry geom,
			Point2D inputPoint) {
		MultiVertexGeometryImpl mpImpl = (MultiVertexGeometryImpl) geom
				._getImpl();
		AttributeStreamOfDbl position = (AttributeStreamOfDbl) mpImpl
				.getAttributeStreamRef((Semantics.POSITION));
		int pointCount = geom.getPointCount();

		int closestIndex = 0;
		double closestx = 0.0;
		double closesty = 0.0;
		double closestDistanceSq = NumberUtils.doubleMax();
		for (int i = 0; i < pointCount; i++) {
			Point2D pt = new Point2D();
			position.read(2 * i, pt);

			double distanceSq = Point2D.sqrDistance(pt, inputPoint);
			if (distanceSq < closestDistanceSq) {
				closestx = pt.x;
				closesty = pt.y;
				closestIndex = i;
				closestDistanceSq = distanceSq;
			}
		}

		Proximity2DResult result = new Proximity2DResult();
		result._setParams(closestx, closesty, closestIndex,
				Math.sqrt(closestDistanceSq));

		return result;
	}

	Proximity2DResult[] pointGetNearestVertices(Point geom, Point2D inputPoint,
			double searchRadius, int maxVertexCountToReturn) {
		Proximity2DResult[] resultArray;

		if (maxVertexCountToReturn == 0) {
			resultArray = new Proximity2DResult[] {};
			return resultArray;
		}

		double searchRadiusSq = searchRadius * searchRadius;
		Point2D pt = geom.getXY();

		double distanceSq = Point2D.sqrDistance(pt, inputPoint);
		if (distanceSq <= searchRadiusSq) {
			resultArray = new Proximity2DResult[1];

			Proximity2DResult result = new Proximity2DResult();
			result._setParams(pt.x, pt.y, 0, Math.sqrt(distanceSq));
			resultArray[0] = result;
		} else {
			resultArray = new Proximity2DResult[0];
		}

		return resultArray;
	}

	Proximity2DResult[] multiVertexGetNearestVertices(MultiVertexGeometry geom,
			Point2D inputPoint, double searchRadius, int maxVertexCountToReturn) {
		Proximity2DResult[] resultArray;

		if (maxVertexCountToReturn == 0) {
			resultArray = new Proximity2DResult[0];
			return resultArray;
		}

		MultiVertexGeometryImpl mpImpl = (MultiVertexGeometryImpl) geom
				._getImpl();
		AttributeStreamOfDbl position = (AttributeStreamOfDbl) mpImpl
				.getAttributeStreamRef((Semantics.POSITION));
		int pointCount = geom.getPointCount();

		ArrayList<Proximity2DResult> v = new ArrayList<Proximity2DResult>(
				maxVertexCountToReturn);

		int count = 0;
		double searchRadiusSq = searchRadius * searchRadius;
		for (int i = 0; i < pointCount; i++) {
			double x = position.read(2 * i);
			double y = position.read(2 * i + 1);

			double xDiff = inputPoint.x - x;
			double yDiff = inputPoint.y - y;

			double distanceSq = xDiff * xDiff + yDiff * yDiff;
			if (distanceSq <= searchRadiusSq) {
				Proximity2DResult result = new Proximity2DResult();
				result._setParams(x, y, i, Math.sqrt(distanceSq));

				count++;
				v.add(result);

			}
		}

		int vsize = v.size();
		Collections.sort(v, new Proximity2DResultComparator());

		if (maxVertexCountToReturn >= vsize)
			return v.toArray(new Proximity2DResult[0]);
		return v.subList(0, maxVertexCountToReturn).toArray(
				new Proximity2DResult[0]);

	}

	/*
	 * if (distanceSq <= searchRadiusSq) { if (count >= maxVertexCountToReturn +
	 * 1) { count++; double frontDistance = v.get(0).getDistance(); if
	 * (frontDistance * frontDistance <= distanceSq) continue; }
	 * 
	 * Proximity2DResult result = new Proximity2DResult(); result._setParams(x,
	 * y, i, Math.sqrt(distanceSq));
	 * 
	 * count++;
	 * 
	 * if (count <= maxVertexCountToReturn) { v.add(result); } // else // { //
	 * if (count == maxVertexCountToReturn + 1) // MAKEHEAP(v,
	 * Proximity2DResult, Proximity2DResult::_Compare); // // PUSHHEAP(v,
	 * result, Proximity2DResult, Proximity2DResult::_Compare); // POPHEAP(v,
	 * Proximity2DResult, Proximity2DResult::_Compare); // } } }
	 * 
	 * int vsize = v.size(); Collections.sort(v, new
	 * Proximity2DResultComparator());
	 * 
	 * // SORTDYNAMICARRAY(v, Proximity2DResult, 0, vsize,
	 * Proximity2DResult::_Compare); resultArray = new Proximity2DResult[vsize];
	 * for (int i = 0; i < vsize; i++) { resultArray[i] =
	 * (Proximity2DResult)v.get(i); }
	 * 
	 * return resultArray; }
	 */
}
