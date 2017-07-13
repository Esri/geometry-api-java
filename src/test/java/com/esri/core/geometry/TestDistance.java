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

import junit.framework.TestCase;
import org.junit.Test;

public class TestDistance extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testDistanceBetweenVariousGeometries() {
		Polygon polygon = makePolygon();
		Polyline polyline = makePolyline();
		MultiPoint multipoint = makeMultiPoint();
		Point point = makePoint();
		// SpatialReference spatialRef =
		// SpatialReference.create(3857);//PCS_WGS_1984_WEB_MERCATOR_AUXSPHERE

		double distance;

		distance = GeometryEngine.distance(polygon, polyline, null);
		assertTrue(Math.abs(distance - 5.0) < 0.00001);

		distance = GeometryEngine.distance(polygon, multipoint, null);
		assertTrue(Math.abs(distance - 5.0) < 0.00001);

		distance = GeometryEngine.distance(polygon, point, null);
		assertTrue(Math.abs(distance - 5.0) < 0.00001);
	}

	@Test
	public static void testDistanceBetweenTriangles() {
		double distance;

		Polygon poly = new Polygon();
		Polygon poly2 = new Polygon();

		poly.startPath(0.0, 0.0);
		poly.lineTo(1.0, 2.0);
		poly.lineTo(0.0, 2.0);

		double xSeparation = 0.1;
		double ySeparation = 0.1;

		poly2.startPath(xSeparation + 1.0, 2.0 - ySeparation);
		poly2.lineTo(xSeparation + 2.0, 2.0 - ySeparation);
		poly2.lineTo(xSeparation + 2.0, 4.0 - ySeparation);

		distance = GeometryEngine.distance(poly, poly2, null);

		assertTrue(0.0 < distance && distance < xSeparation + ySeparation);
	}

	@Test
	public static void testDistanceBetweenPointAndEnvelope() {
		Envelope env = new Envelope(23,23, 23,23);
        Point pt = new Point(30, 30);
        double dist = GeometryEngine.distance(env, pt, null);  // expect just under 10.
		assertTrue(Math.abs(dist - 9.8994949) < 0.0001);
	}
	
	@Test
	public static void testDistanceBetweenHugeGeometries() {
		/* const */int N = 1000; // Should be even
		/* const */double theoreticalDistance = 0.77;

		Polygon poly = new Polygon();
		Polygon poly2 = new Polygon();

		double theta = 0.0;
		double thetaPlusPi = Math.PI;
		double dTheta = 2.0 * Math.PI / N;
		double distance;

		poly.startPath(Math.cos(theta), Math.sin(theta));
		// Add something so that poly2's bounding box is in poly's. Deleting
		// this should not affect answer.
		poly.lineTo(1.0, 1.5 + theoreticalDistance);
		poly.lineTo(3.5 + theoreticalDistance, 1.5 + theoreticalDistance);
		poly.lineTo(3.5 + theoreticalDistance, 2.0 + theoreticalDistance);
		poly.lineTo(0.95, 2.0 + theoreticalDistance);
		// ///////////////////////////////////////////////////////////
		poly2.startPath(2.0 + theoreticalDistance + Math.cos(thetaPlusPi),
				Math.sin(thetaPlusPi));
		for (double i = 1; i < N; i++) {
			theta += dTheta;
			thetaPlusPi += dTheta;
			poly.lineTo(Math.cos(theta), Math.sin(theta));
			poly2.lineTo(2.0 + theoreticalDistance + Math.cos(thetaPlusPi),
					Math.sin(thetaPlusPi));
		}

		distance = GeometryEngine.distance(poly, poly2, null);

		assertTrue(Math.abs(distance - theoreticalDistance) < 1.0e-10);
	}

	private static Polygon makePolygon() {
		Polygon poly = new Polygon();

		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		poly.startPath(3, 3);
		poly.lineTo(7, 3);
		poly.lineTo(7, 7);
		poly.lineTo(3, 7);

		return poly;
	}

	private static Polyline makePolyline() {
		Polyline poly = new Polyline();
		poly.startPath(0, 15);
		poly.lineTo(15, 15);
		return poly;
	}

	private static MultiPoint makeMultiPoint() {
		MultiPoint mpoint = new MultiPoint();
		mpoint.add(0, 30);
		mpoint.add(15, 15);
		mpoint.add(0, 15);
		return mpoint;
	}

	private static Point makePoint() {
		Point point = new Point();
		Point2D pt = new Point2D();
		pt.setCoords(0, 15);
		point.setXY(pt);
		return point;
	}

	@Test
	public static void testDistanceWithNullSpatialReference() {
		// There was a bug that distance op did not work with null Spatial
		// Reference.
		String str1 = "{\"paths\":[[[-117.138791850991,34.017492675023],[-117.138762336971,34.0174925550462]]]}";
		String str2 = "{\"paths\":[[[-117.138867827972,34.0174854109623],[-117.138850197027,34.0174929160126],[-117.138791850991,34.017492675023]]]}";
		MapGeometry geom1 = GeometryEngine.jsonToGeometry(JsonParserReader.createFromString(str1));
		MapGeometry geom2 = GeometryEngine.jsonToGeometry(JsonParserReader.createFromString(str2));
		double distance = GeometryEngine.distance(geom1.getGeometry(),
				geom2.getGeometry(), null);
		assertTrue(distance == 0);
	}
}
