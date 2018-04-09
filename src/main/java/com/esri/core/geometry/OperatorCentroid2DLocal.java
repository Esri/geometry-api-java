/*
 Copyright 1995-2017 Esri

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

import static java.lang.Math.sqrt;

public class OperatorCentroid2DLocal extends OperatorCentroid2D
{
    @Override
    public Point2D execute(Geometry geometry, ProgressTracker progressTracker)
    {
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

    private static Point2D computeLineCentroid(Line line)
    {
        return new Point2D((line.getEndX() - line.getStartX()) / 2, (line.getEndY() - line.getStartY()) / 2);
    }

    // Points centroid is arithmetic mean of the input points
    private static Point2D computePointsCentroid(MultiPoint multiPoint)
    {
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

    // Lines centroid is weighted mean of each line segment, weight in terms of line length
    private static Point2D computePolylineCentroid(Polyline polyline)
    {
        double xSum = 0;
        double ySum = 0;
        double weightSum = 0;

        Point2D startPoint = new Point2D();
        Point2D endPoint = new Point2D();
        for (int i = 0; i < polyline.getPathCount(); i++) {
            polyline.getXY(polyline.getPathStart(i), startPoint);
            polyline.getXY(polyline.getPathEnd(i) - 1, endPoint);
            double dx = endPoint.x - startPoint.x;
            double dy = endPoint.y - startPoint.y;
            double length = sqrt(dx * dx + dy * dy);
            weightSum += length;
            xSum += (startPoint.x + endPoint.x) * length / 2;
            ySum += (startPoint.y + endPoint.y) * length / 2;
        }
        return new Point2D(xSum / weightSum, ySum / weightSum);
    }

    // Polygon centroid: area weighted average of centroids in case of holes
    private static Point2D computePolygonCentroid(Polygon polygon)
    {
        int pathCount = polygon.getPathCount();

        if (pathCount == 1) {
            return getPolygonSansHolesCentroid(polygon);
        }

        double xSum = 0;
        double ySum = 0;
        double areaSum = 0;

        for (int i = 0; i < pathCount; i++) {
            int startIndex = polygon.getPathStart(i);
            int endIndex = polygon.getPathEnd(i);

            Polygon sansHoles = getSubPolygon(polygon, startIndex, endIndex);

            Point2D centroid = getPolygonSansHolesCentroid(sansHoles);
            double area = sansHoles.calculateArea2D();

            xSum += centroid.x * area;
            ySum += centroid.y * area;
            areaSum += area;
        }

        return new Point2D(xSum / areaSum, ySum / areaSum);
    }

    private static Polygon getSubPolygon(Polygon polygon, int startIndex, int endIndex)
    {
        Polyline boundary = new Polyline();
        boundary.startPath(polygon.getPoint(startIndex));
        for (int i = startIndex + 1; i < endIndex; i++) {
            Point current = polygon.getPoint(i);
            boundary.lineTo(current);
        }

        final Polygon newPolygon = new Polygon();
        newPolygon.add(boundary, false);
        return newPolygon;
    }

    // Polygon sans holes centroid:
    // c[x] = (Sigma(x[i] + x[i + 1]) * (x[i] * y[i + 1] - x[i + 1] * y[i]), for i = 0 to N - 1) / (6 * signedArea)
    // c[y] = (Sigma(y[i] + y[i + 1]) * (x[i] * y[i + 1] - x[i + 1] * y[i]), for i = 0 to N - 1) / (6 * signedArea)
    private static Point2D getPolygonSansHolesCentroid(Polygon polygon)
    {
        int pointCount = polygon.getPointCount();
        double xSum = 0;
        double ySum = 0;
        double signedArea = 0;

        Point2D current = new Point2D();
        Point2D next = new Point2D();
        for (int i = 0; i < pointCount; i++) {
            polygon.getXY(i, current);
            polygon.getXY((i + 1) % pointCount, next);
            double ladder = current.x * next.y - next.x * current.y;
            xSum += (current.x + next.x) * ladder;
            ySum += (current.y + next.y) * ladder;
            signedArea += ladder / 2;
        }
        return new Point2D(xSum / (signedArea * 6), ySum / (signedArea * 6));
    }
}
