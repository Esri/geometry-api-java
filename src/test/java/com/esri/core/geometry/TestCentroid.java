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

public class TestCentroid {
	@Test
	public void testPoint() {
		assertCentroid(new Point(1, 2), new Point2D(1, 2));
	}

	@Test
	public void testLine() {
		assertCentroid(new Line(0, 0, 10, 20), new Point2D(5, 10));
	}

	@Test
	public void testEnvelope() {
		assertCentroid(new Envelope(1, 2, 3, 4), new Point2D(2, 3));
		assertCentroidEmpty(new Envelope());
	}

	@Test
	public void testMultiPoint() {
		MultiPoint multiPoint = new MultiPoint();
		multiPoint.add(0, 0);
		multiPoint.add(1, 2);
		multiPoint.add(3, 1);
		multiPoint.add(0, 1);

		assertCentroid(multiPoint, new Point2D(1, 1));
		assertCentroidEmpty(new MultiPoint());
	}

	@Test
	public void testPolyline() {
		Polyline polyline = new Polyline();
		polyline.startPath(0, 0);
		polyline.lineTo(1, 2);
		polyline.lineTo(3, 4);
		assertCentroid(polyline, new Point2D(1.3377223398316207, 2.1169631197754946));

		polyline.startPath(1, -1);
		polyline.lineTo(2, 0);
		polyline.lineTo(10, 1);
		assertCentroid(polyline, new Point2D(3.93851092460519, 0.9659173294165462));

		assertCentroidEmpty(new Polyline());
	}

	@Test
	public void testPolygon() {
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
		assertCentroid(polygon, new Point2D(2.5022670025188916, 1.9989924433249369));

		// add another polygon
		polygon.startPath(-1, -1);
		polygon.lineTo(3, -1);
		polygon.lineTo(0.5, -2);
		polygon.lineTo(-1, -1);
		assertCentroid(polygon, new Point2D(2.166465459423206, 1.3285043594902748));

		assertCentroidEmpty(new Polygon());
	}

	@Test
	public void testSmallPolygon() {
		// https://github.com/Esri/geometry-api-java/issues/225

		Polygon polygon = new Polygon();
		polygon.startPath(153.492818, -28.13729);
		polygon.lineTo(153.492821, -28.137291);
		polygon.lineTo(153.492816, -28.137289);
		polygon.lineTo(153.492818, -28.13729);

		assertCentroid(polygon, new Point2D(153.492818333333333, -28.13729));
	}

	@Test
	public void testZeroAreaPolygon() {
		Polygon polygon = new Polygon();
		polygon.startPath(153, 28);
		polygon.lineTo(163, 28);
		polygon.lineTo(153, 28);

		Polyline polyline = (Polyline) polygon.getBoundary();
		Point2D expectedCentroid = new Point2D(158, 28);

		assertCentroid(polyline, expectedCentroid);
		assertCentroid(polygon, expectedCentroid);
	}

	@Test
	public void testDegeneratesToPointPolygon() {
		Polygon polygon = new Polygon();
		polygon.startPath(-8406364, 560828);
		polygon.lineTo(-8406364, 560828);
		polygon.lineTo(-8406364, 560828);
		polygon.lineTo(-8406364, 560828);

		assertCentroid(polygon, new Point2D(-8406364, 560828));
	}

	@Test
	public void testZeroLengthPolyline() {
		Polyline polyline = new Polyline();
		polyline.startPath(153, 28);
		polyline.lineTo(153, 28);

		assertCentroid(polyline, new Point2D(153, 28));
	}

	@Test
	public void testDegeneratesToPointPolyline() {
		Polyline polyline = new Polyline();
		polyline.startPath(-8406364, 560828);
		polyline.lineTo(-8406364, 560828);

		assertCentroid(polyline, new Point2D(-8406364, 560828));
	}

	private static void assertCentroid(Geometry geometry, Point2D expectedCentroid) {

		Point2D actualCentroid = OperatorCentroid2D.local().execute(geometry, null);
		Assert.assertEquals(expectedCentroid.x, actualCentroid.x, 1e-13);
		Assert.assertEquals(expectedCentroid.y, actualCentroid.y, 1e-13);
	}

	private static void assertCentroidEmpty(Geometry geometry) {

		Point2D actualCentroid = OperatorCentroid2D.local().execute(geometry, null);
		Assert.assertTrue(actualCentroid == null);
	}
}
