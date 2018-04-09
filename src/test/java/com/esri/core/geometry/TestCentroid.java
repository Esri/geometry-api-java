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

import org.junit.Assert;
import org.junit.Test;

public class TestCentroid
{
    @Test
    public void testPoint()
    {
        assertCentroid(new Point(1, 2), new Point2D(1, 2));
    }

    @Test
    public void testLine()
    {
        assertCentroid(new Line(0, 0, 10, 20), new Point2D(5, 10));
    }

    @Test
    public void testEnvelope()
    {
        assertCentroid(new Envelope(1, 2, 3,4), new Point2D(2, 3));
        assertCentroid(new Envelope(), null);
    }

    @Test
    public void testMultiPoint()
    {
        MultiPoint multiPoint = new MultiPoint();
        multiPoint.add(0, 0);
        multiPoint.add(1, 2);
        multiPoint.add(3, 1);
        multiPoint.add(0, 1);

        assertCentroid(multiPoint, new Point2D(1, 1));
        assertCentroid(new MultiPoint(), null);
    }

    @Test
    public void testPolyline()
    {
        Polyline polyline = new Polyline();
        polyline.startPath(0, 0);
        polyline.lineTo(1, 2);
        polyline.lineTo(3, 4);
        assertCentroid(polyline, new Point2D(1.5, 2));

        polyline.startPath(1, -1);
        polyline.lineTo(2, 0);
        polyline.lineTo(10, 1);
        assertCentroid(polyline, new Point2D(4.093485180902371 , 0.7032574095488145));

        assertCentroid(new Polyline(), null);
    }

    @Test
    public void testPolygon()
    {
        Polygon polygon = new Polygon();
        polygon.startPath(0, 0);
        polygon.lineTo(1, 2);
        polygon.lineTo(3, 4);
        polygon.lineTo(5, 2);
        polygon.lineTo(0, 0);
        assertCentroid(polygon, new Point2D(2.5, 2));

        // add a hole
        polygon.startPath(2, 2);
        polygon.lineTo(2.3, 2);
        polygon.lineTo(2.3, 2.4);
        polygon.lineTo(2, 2);
        assertCentroid(polygon, new Point2D(2.5022670025188916 , 1.9989924433249369));

        // add another polygon
        polygon.startPath(-1, -1);
        polygon.lineTo(3, -1);
        polygon.lineTo(0.5, -2);
        polygon.lineTo(-1, -1);
        assertCentroid(polygon, new Point2D(2.166465459423206 , 1.3285043594902748));

        assertCentroid(new Polygon(), null);
    }

    private static void assertCentroid(Geometry geometry, Point2D expectedCentroid)
    {
        OperatorCentroid2D operator = (OperatorCentroid2D) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.Centroid2D);

        Point2D actualCentroid = operator.execute(geometry, null);
        Assert.assertEquals(expectedCentroid, actualCentroid);
    }
}
