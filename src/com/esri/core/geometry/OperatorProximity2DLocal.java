/*
 Copyright 1995-2013 Esri

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

	@Override
	public Proximity2DResult getNearestCoordinate(Geometry geom,
			Point inputPoint, boolean bTestPolygonInterior) {
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
			return polyPathGetNearestCoordinate((MultiPath) proxmityTestGeom,
					inputPoint2D, bTestPolygonInterior);
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

	Proximity2DResult polyPathGetNearestCoordinate(MultiPath geom,
			Point2D inputPoint, boolean bTestPolygonInterior) {
		Proximity2DResult result = new Proximity2DResult();

		if (geom.getType() == (Geometry.Type.Polygon) && bTestPolygonInterior) {
			OperatorFactoryLocal factory = OperatorFactoryLocal.getInstance();
			OperatorDisjoint operatorDisjoint = (OperatorDisjoint) factory
					.getOperator(Type.Disjoint);

			Point point = new Point(geom.getDescription());
			point.setXY(inputPoint.x, inputPoint.y);

			boolean disjoint = operatorDisjoint
					.execute(geom, point, null, null);
			if (!disjoint) {
				result._setParams(inputPoint.x, inputPoint.y, 0, 0.0);
				return result;
			}
		}

		MultiPathImpl mpImpl = (MultiPathImpl) geom._getImpl();
		SegmentIteratorImpl segIter = mpImpl.querySegmentIterator();

		Point2D closest = null;// new Point2D();
		int closestIndex = 0;
		double closestDistanceSq = NumberUtils.doubleMax();

		while (segIter.nextPath()) {
			while (segIter.hasNextSegment()) {
				Segment segment = segIter.nextSegment();
				double t = segment.getClosestCoordinate(inputPoint, false);
				Point2D point = segment.getCoord2D(t);

				double distanceSq = Point2D.sqrDistance(point, inputPoint);
				if (distanceSq < closestDistanceSq) {
					closest = point;
					closestIndex = segIter.getStartPointIndex();
					closestDistanceSq = distanceSq;
				}
			}
		}

		result._setParams(closest.x, closest.y, closestIndex,
				Math.sqrt(closestDistanceSq));

		return result;
	}

	Proximity2DResult pointGetNearestVertex(Point geom, Point2D inputPoint) {
		Proximity2DResult result = new Proximity2DResult();

		Point2D pt = geom.getXY();
		double distance = Point2D.distance(pt, inputPoint);
		result._setParams(pt.x, pt.y, 0, distance);

		return result;
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
