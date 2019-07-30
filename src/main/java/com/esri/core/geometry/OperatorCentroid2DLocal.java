/*
 Copyright 1995-2019 Esri

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

public class OperatorCentroid2DLocal extends OperatorCentroid2D {
	@Override
	public Point2D execute(Geometry geometry, ProgressTracker progressTracker) {
		if (geometry.isEmpty()) {
			return null;
		}

		Geometry.Type geometryType = geometry.getType();
		switch (geometryType) {
		case Point:
			return ((Point) geometry).getXY();
		case Line:
			return computeLineCentroid((Line) geometry);
		case Envelope:
			return ((Envelope) geometry).getCenterXY();
		case MultiPoint:
			return computePointsCentroid((MultiPoint) geometry);
		case Polyline:
			return computePolylineCentroid(((Polyline) geometry));
		case Polygon:
			return computePolygonCentroid((Polygon) geometry);
		default:
			throw new UnsupportedOperationException("Unexpected geometry type: " + geometryType);
		}
	}

	private static Point2D computeLineCentroid(Line line) {
		return new Point2D((line.getEndX() - line.getStartX()) / 2, (line.getEndY() - line.getStartY()) / 2);
	}

	// Points centroid is arithmetic mean of the input points
	private static Point2D computePointsCentroid(MultiVertexGeometry multiPoint) {
		double xSum = 0;
		double ySum = 0;
		int pointCount = multiPoint.getPointCount();
		Point2D point2D = new Point2D();
		for (int i = 0; i < pointCount; i++) {
			multiPoint.getXY(i, point2D);
			xSum += point2D.x;
			ySum += point2D.y;
		}
		return new Point2D(xSum / pointCount, ySum / pointCount);
	}

	// Lines centroid is weighted mean of each line segment, weight in terms of line
	// length
	private static Point2D computePolylineCentroid(MultiPath polyline) {
		double totalLength = polyline.calculateLength2D();
		if (totalLength == 0) {
			return computePointsCentroid(polyline);
		}
		
		MathUtils.KahanSummator xSum = new MathUtils.KahanSummator(0);
		MathUtils.KahanSummator ySum = new MathUtils.KahanSummator(0);
		Point2D point = new Point2D();
		SegmentIterator iter = polyline.querySegmentIterator();
		while (iter.nextPath()) {
			while (iter.hasNextSegment()) {
				Segment seg = iter.nextSegment();
				seg.getCoord2D(0.5, point);
				double length = seg.calculateLength2D();
				point.scale(length);
				xSum.add(point.x);
				ySum.add(point.y);
			}
		}
		
		return new Point2D(xSum.getResult() / totalLength, ySum.getResult() / totalLength);
	}

	// Polygon centroid: area weighted average of centroids
	private static Point2D computePolygonCentroid(Polygon polygon) {
		double totalArea = polygon.calculateArea2D();
		if (totalArea == 0)
		{
			return computePolylineCentroid(polygon);
		}
		
		MathUtils.KahanSummator xSum = new MathUtils.KahanSummator(0);
		MathUtils.KahanSummator ySum = new MathUtils.KahanSummator(0);
		Point2D startPoint = new Point2D();
		Point2D current = new Point2D();
		Point2D next = new Point2D();
		Point2D origin = polygon.getXY(0);

		for (int ipath = 0, npaths = polygon.getPathCount(); ipath < npaths; ipath++) {
			int startIndex = polygon.getPathStart(ipath);
			int endIndex = polygon.getPathEnd(ipath);
			int pointCount = endIndex - startIndex;
			if (pointCount < 3) {
				continue;
			}
			
			polygon.getXY(startIndex, startPoint);
			polygon.getXY(startIndex + 1, current);
			current.sub(startPoint);
			for (int i = startIndex + 2, n = endIndex; i < n; i++) {
				polygon.getXY(i, next);
				next.sub(startPoint);
				double twiceTriangleArea = next.x * current.y - current.x * next.y;
				xSum.add((current.x + next.x) * twiceTriangleArea);
				ySum.add((current.y + next.y) * twiceTriangleArea);
				current.setCoords(next);
			}
			
			startPoint.sub(origin);
			startPoint.scale(6.0 * polygon.calculateRingArea2D(ipath));
			//add weighted startPoint
			xSum.add(startPoint.x);
			ySum.add(startPoint.y);
		}

		totalArea *= 6.0;
		Point2D res = new Point2D(xSum.getResult() / totalArea, ySum.getResult() / totalArea);
		res.add(origin);
		return res;
	}
}
