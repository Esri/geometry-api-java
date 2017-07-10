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

//import java.io.FileOutputStream;
//import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;

public class TestSimplify extends TestCase {
	OperatorFactoryLocal factory = null;
	OperatorSimplify simplifyOp = null;
	OperatorSimplifyOGC simplifyOpOGC = null;
	SpatialReference sr102100 = null;
	SpatialReference sr4326 = null;
	SpatialReference sr3857 = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		factory = OperatorFactoryLocal.getInstance();
		simplifyOp = (OperatorSimplify) factory
				.getOperator(Operator.Type.Simplify);
		simplifyOpOGC = (OperatorSimplifyOGC) factory
				.getOperator(Operator.Type.SimplifyOGC);
		sr102100 = SpatialReference.create(102100);
		sr3857 = SpatialReference.create(3857);// PE_PCS_WGS_1984_WEB_MERCATOR_AUXSPHERE);
		sr4326 = SpatialReference.create(4326);// enum_value2(SpatialReference,
												// Code, GCS_WGS_1984));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public Polygon makeNonSimplePolygon2() {
		//MapGeometry mg = OperatorFactoryLocal.loadGeometryFromJSONFileDbg("c:/temp/simplify_polygon_gnomonic.txt");
		//Geometry res = OperatorSimplify.local().execute(mg.getGeometry(), mg.getSpatialReference(), true, null);
		
		
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 15);
		poly.lineTo(15, 15);
		poly.lineTo(15, 0);

		// This is an interior ring but it is clockwise
		poly.startPath(5, 5);
		poly.lineTo(5, 6);
		poly.lineTo(6, 6);
		poly.lineTo(6, 5);

		return poly;
	}// done

	/*
	 * ------------>---------------->--------------- | | | (1) | | | | --->---
	 * ------->------- | | | | | (5) | | | | | | --<-- | | | | (2) | | | | | | |
	 * | | | | (4) | | | | | | | -->-- | | --<-- | ---<--- | | | | | |
	 * -------<------- | | (3) | -------------<---------------<---------------
	 * -->--
	 */

	// Bowtie case with vertices at intersection

	public Polygon makeNonSimplePolygon5() {
		Polygon poly = new Polygon();
		poly.startPath(10, 0);
		poly.lineTo(0, 0);
		poly.lineTo(5, 5);
		poly.lineTo(10, 10);
		poly.lineTo(0, 10);
		poly.lineTo(5, 5);

		return poly;
	}// done

	@Test
	public void test0() {
		Polygon poly1 = new Polygon();
		poly1.addEnvelope(new Envelope(10, 10, 40, 20), false);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		boolean res = simplifyOp.isSimpleAsFeature(poly2, null, true, null,
				null);
		assertTrue(res);
		// assertTrue(poly1.equals(poly2));
	}// done

	@Test
	public void test0Poly() {// simple
		Polygon poly1 = new Polygon();
		poly1.addEnvelope(new Envelope(10, 10, 40, 20), false);
		poly1.addEnvelope(new Envelope(50, 10, 100, 20), false);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		boolean res = simplifyOp.isSimpleAsFeature(poly2, null, true, null,
				null);
		assertTrue(res);
		// assertTrue(poly1.equals(poly2));
	}// done

	@Test
	public void test0Polygon_Spike1() {// non-simple (spike)
		Polygon poly1 = new Polygon();
		poly1.startPath(10, 10);
		poly1.lineTo(10, 20);
		poly1.lineTo(40, 20);
		poly1.lineTo(40, 10);
		poly1.lineTo(60, 10);
		poly1.lineTo(70, 10);

		boolean res = simplifyOp.isSimpleAsFeature(poly1, null, true, null,
				null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(poly2.getPointCount() == 4);
	}// done

	@Test
	public void test0Polygon_Spike2() {// non-simple (spikes)
		Polygon poly1 = new Polygon();
		// rectangle with a spike
		poly1.startPath(10, 10);
		poly1.lineTo(10, 20);
		poly1.lineTo(40, 20);
		poly1.lineTo(40, 10);
		poly1.lineTo(60, 10);
		poly1.lineTo(70, 10);

		// degenerate
		poly1.startPath(100, 100);
		poly1.lineTo(100, 120);
		poly1.lineTo(100, 130);

		boolean res = simplifyOp.isSimpleAsFeature(poly1, null, true, null,
				null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(poly2.getPointCount() == 4);
	}// done

	@Test
	public void test0Polygon_Spike3() {// non-simple (spikes)
		Polygon poly1 = new Polygon();
		// degenerate
		poly1.startPath(100, 100);
		poly1.lineTo(100, 120);
		poly1.lineTo(100, 130);

		boolean res = simplifyOp.isSimpleAsFeature(poly1, null, true, null,
				null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonSelfIntersect1() {// non-simple (self-intersection)
		Polygon poly1 = new Polygon();
		// touch uncracked
		poly1.startPath(0, 0);
		poly1.lineTo(0, 100);
		poly1.lineTo(100, 100);
		poly1.lineTo(0, 50);
		poly1.lineTo(100, 0);

		boolean res = simplifyOp.isSimpleAsFeature(poly1, null, true, null,
				null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonSelfIntersect2() {// non-simple (self-intersection)
		Polygon poly1 = new Polygon();
		poly1.startPath(0, 0);
		poly1.lineTo(0, 100);
		poly1.lineTo(100, 100);
		poly1.lineTo(-100, 0);
		// poly1.lineTo(100, 0);

		boolean res = simplifyOp.isSimpleAsFeature(poly1, null, true, null,
				null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly1, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonSelfIntersect3() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 15);
		poly.lineTo(15, 15);
		poly.lineTo(15, 0);

		// This part intersects with the first part
		poly.startPath(10, 10);
		poly.lineTo(10, 20);
		poly.lineTo(20, 20);
		poly.lineTo(20, 10);

		boolean res = simplifyOp
				.isSimpleAsFeature(poly, null, true, null, null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonInteriorRing1() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 15);
		poly.lineTo(15, 15);
		poly.lineTo(15, 0);

		// This is an interior ring but it is clockwise
		poly.startPath(5, 5);
		poly.lineTo(5, 6);
		poly.lineTo(6, 6);
		poly.lineTo(6, 5);

		boolean res = simplifyOp
				.isSimpleAsFeature(poly, null, true, null, null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonInteriorRing2() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 15);
		poly.lineTo(15, 15);
		poly.lineTo(15, 0);

		// This is an interior ring but it is clockwise
		poly.startPath(5, 5);
		poly.lineTo(5, 6);
		poly.lineTo(6, 6);
		poly.lineTo(6, 5);

		// This part intersects with the first part
		poly.startPath(10, 10);
		poly.lineTo(10, 20);
		poly.lineTo(20, 20);
		poly.lineTo(20, 10);

		boolean res = simplifyOp
				.isSimpleAsFeature(poly, null, true, null, null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonInteriorRingWithCommonBoundary1() {
		// Two rings have common boundary
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		poly.startPath(10, 0);
		poly.lineTo(10, 10);
		poly.lineTo(20, 10);
		poly.lineTo(20, 0);

		boolean res = simplifyOp
				.isSimpleAsFeature(poly, null, true, null, null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void test0PolygonInteriorRingWithCommonBoundary2() {
		// Two rings have common boundary
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		poly.startPath(10, 5);
		poly.lineTo(10, 6);
		poly.lineTo(20, 6);
		poly.lineTo(20, 5);

		boolean res = simplifyOp
				.isSimpleAsFeature(poly, null, true, null, null);
		assertTrue(!res);
		Polygon poly2 = (Polygon) simplifyOp.execute(poly, null, false, null);
		res = simplifyOp.isSimpleAsFeature(poly2, null, true, null, null);
		assertTrue(res);
		assertTrue(!poly2.isEmpty());
	}// done

	@Test
	public void testPolygon() {
		Polygon nonSimplePolygon = makeNonSimplePolygon();
		Polygon simplePolygon = (Polygon) simplifyOp.execute(nonSimplePolygon,
				sr3857, false, null);

		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon, sr3857, true,
				null, null);
		assertTrue(res);

		@SuppressWarnings("unused")
		int partCount = simplePolygon.getPathCount();
		// assertTrue(partCount == 2);

		double area = simplePolygon.calculateRingArea2D(0);
		assertTrue(Math.abs(area - 300) <= 0.0001);

		area = simplePolygon.calculateRingArea2D(1);
		assertTrue(Math.abs(area - (-25.0)) <= 0.0001);
	}// done

	@Test
	public void testPolygon2() {
		Polygon nonSimplePolygon2 = makeNonSimplePolygon2();
		double area = nonSimplePolygon2.calculateRingArea2D(1);
		assertTrue(Math.abs(area - 1.0) <= 0.0001);

		Polygon simplePolygon2 = (Polygon) simplifyOp.execute(
				nonSimplePolygon2, sr3857, false, null);

		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon2, sr3857,
				true, null, null);
		assertTrue(res);

		area = simplePolygon2.calculateRingArea2D(0);
		assertTrue(Math.abs(area - 225) <= 0.0001);

		area = simplePolygon2.calculateRingArea2D(1);
		assertTrue(Math.abs(area - (-1.0)) <= 0.0001);
	}// done

	@Test
	public void testPolygon3() {
		Polygon nonSimplePolygon3 = makeNonSimplePolygon3();
		Polygon simplePolygon3 = (Polygon) simplifyOp.execute(
				nonSimplePolygon3, sr3857, false, null);

		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon3, sr3857,
				true, null, null);
		assertTrue(res);

		double area = simplePolygon3.calculateRingArea2D(0);
		assertTrue(Math.abs(area - 875) <= 0.0001);

		area = simplePolygon3.calculateRingArea2D(1);
		assertTrue(Math.abs(area - (-225)) <= 0.0001
				|| Math.abs(area - (-50.0)) <= 0.0001);

		area = simplePolygon3.calculateRingArea2D(2);
		assertTrue(Math.abs(area - (-225)) <= 0.0001
				|| Math.abs(area - (-50.0)) <= 0.0001);

		area = simplePolygon3.calculateRingArea2D(3);
		assertTrue(Math.abs(area - 25) <= 0.0001);

		area = simplePolygon3.calculateRingArea2D(4);
		assertTrue(Math.abs(area - 25) <= 0.0001);
	}// done

	@Test
	public void testPolyline() {
		Polyline nonSimplePolyline = makeNonSimplePolyline();
		Polyline simplePolyline = (Polyline) simplifyOp.execute(
				nonSimplePolyline, sr3857, false, null);

		int segmentCount = simplePolyline.getSegmentCount();
		assertTrue(segmentCount == 4);
	}// done

	@Test
	public void testPolygon4() {
		Polygon nonSimplePolygon4 = makeNonSimplePolygon4();
		Polygon simplePolygon4 = (Polygon) simplifyOp.execute(
				nonSimplePolygon4, sr3857, false, null);
		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon4, sr3857,
				true, null, null);
		assertTrue(res);

		assertTrue(simplePolygon4.getPointCount() == 5);
		Point point = nonSimplePolygon4.getPoint(0);
		assertTrue(point.getX() == 0.0 && point.getY() == 0.0);
		point = nonSimplePolygon4.getPoint(1);
		assertTrue(point.getX() == 0.0 && point.getY() == 10.0);
		point = nonSimplePolygon4.getPoint(2);
		assertTrue(point.getX() == 10.0 && point.getY() == 10.0);
		point = nonSimplePolygon4.getPoint(3);
		assertTrue(point.getX() == 10.0 && point.getY() == 0.0);
		point = nonSimplePolygon4.getPoint(4);
		assertTrue(point.getX() == 5.0 && point.getY() == 0.0);
	}// done

	@Test
	public void testPolygon5() {
		Polygon nonSimplePolygon5 = makeNonSimplePolygon5();
		Polygon simplePolygon5 = (Polygon) simplifyOp.execute(
				nonSimplePolygon5, sr3857, false, null);
		assertTrue(simplePolygon5 != null);

		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon5, sr3857,
				true, null, null);
		assertTrue(res);

		int pointCount = simplePolygon5.getPointCount();
		assertTrue(pointCount == 6);

		double area = simplePolygon5.calculateArea2D();
		assertTrue(Math.abs(area - 50.0) <= 0.001);

	}// done

	@Test
	public void testPolygon6() {
		Polygon nonSimplePolygon6 = makeNonSimplePolygon6();
		Polygon simplePolygon6 = (Polygon) simplifyOp.execute(
				nonSimplePolygon6, sr3857, false, null);

		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon6, sr3857,
				true, null, null);
		assertTrue(res);
	}

	@Test
	public void testPolygon7() {
		Polygon nonSimplePolygon7 = makeNonSimplePolygon7();
		Polygon simplePolygon7 = (Polygon) simplifyOp.execute(
				nonSimplePolygon7, sr3857, false, null);

		boolean res = simplifyOp.isSimpleAsFeature(simplePolygon7, sr3857,
				true, null, null);
		assertTrue(res);
	}

	public Polygon makeNonSimplePolygon() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 15);
		poly.lineTo(15, 15);
		poly.lineTo(15, 0);

		// This is an interior ring but it is clockwise
		poly.startPath(5, 5);
		poly.lineTo(5, 6);
		poly.lineTo(6, 6);
		poly.lineTo(6, 5);

		// This part intersects with the first part
		poly.startPath(10, 10);
		poly.lineTo(10, 20);
		poly.lineTo(20, 20);
		poly.lineTo(20, 10);

		return poly;
	}// done

	/*
	 * ------------>---------------->--------------- | | | (1) | | | | --->---
	 * ------->------- | | | | | (5) | | | | | | --<-- | | | | (2) | | | | | | |
	 * | | | | (4) | | | | | | | -->-- | | --<-- | ---<--- | | | | | |
	 * -------<------- | | (3) | -------------<---------------<---------------
	 * -->--
	 */

	public Polygon makeNonSimplePolygon3() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 25);
		poly.lineTo(35, 25);
		poly.lineTo(35, 0);

		poly.startPath(5, 5);
		poly.lineTo(5, 15);
		poly.lineTo(10, 15);
		poly.lineTo(10, 5);

		poly.startPath(40, 0);
		poly.lineTo(45, 0);
		poly.lineTo(45, 5);
		poly.lineTo(40, 5);

		poly.startPath(20, 10);
		poly.lineTo(25, 10);
		poly.lineTo(25, 15);
		poly.lineTo(20, 15);

		poly.startPath(15, 5);
		poly.lineTo(15, 20);
		poly.lineTo(30, 20);
		poly.lineTo(30, 5);

		return poly;
	}// done

	public Polygon makeNonSimplePolygon4() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);
		poly.lineTo(5, 0);
		poly.lineTo(5, 5);
		poly.lineTo(5, 0);

		return poly;
	}// done

	public Polygon makeNonSimplePolygon6() {
		Polygon poly = new Polygon();
		poly.startPath(35.34407570857744, 54.00551247713412);
		poly.lineTo(41.07663499357954, 20.0);
		poly.lineTo(40.66372033705177, 26.217432321849017);

		poly.startPath(42.81936574509338, 20.0);
		poly.lineTo(43.58226670584747, 20.0);
		poly.lineTo(39.29611825817084, 22.64634933678729);
		poly.lineTo(44.369873312241346, 25.81893670527215);
		poly.lineTo(42.68845660737179, 20.0);
		poly.lineTo(38.569549792944244, 56.47456192829393);
		poly.lineTo(42.79274114188401, 45.45117792578003);
		poly.lineTo(41.09512147544657, 70.0);

		return poly;
	}

	public Polygon makeNonSimplePolygon7() {
		Polygon poly = new Polygon();

		poly.startPath(41.987895433319686, 53.75822619011542);
		poly.lineTo(41.98789542535497, 53.75822618803151);
		poly.lineTo(40.15120412113667, 68.12604154722113);
		poly.lineTo(37.72272697311022, 67.92767094118877);
		poly.lineTo(37.147347454283086, 49.497473094145505);
		poly.lineTo(38.636627026664385, 51.036687142232736);

		poly.startPath(39.00920080789793, 62.063425518369016);
		poly.lineTo(38.604912643136885, 70.0);
		poly.lineTo(40.71826863485308, 43.60337143116787);
		poly.lineTo(35.34407570857744, 54.005512477134126);
		poly.lineTo(39.29611825817084, 22.64634933678729);

		return poly;
	}

	public Polyline makeNonSimplePolyline() {
		// This polyline has a short segment
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(10, 0);
		poly.lineTo(10, 10);
		poly.lineTo(10, 5);
		poly.lineTo(-5, 5);

		return poly;
	}// done

	@Test
	public void testIsSimpleBasicsPoint() {
		boolean result;
		// point is always simple
		Point pt = new Point();
		result = simplifyOp.isSimpleAsFeature(pt, sr4326, false, null, null);
		assertTrue(result);
		pt.setXY(0, 0);
		result = simplifyOp.isSimpleAsFeature(pt, sr4326, false, null, null);
		assertTrue(result);
		pt.setXY(100000, 10000);
		result = simplifyOp.isSimpleAsFeature(pt, sr4326, false, null, null);
		assertTrue(result);
	}// done

	@Test
	public void testIsSimpleBasicsEnvelope() {
		// Envelope is simple, when it's width and height are not degenerate
		Envelope env = new Envelope();
		boolean result = simplifyOp.isSimpleAsFeature(env, sr4326, false, null,
				null); // Empty is simple
		assertTrue(result);
		env.setCoords(0, 0, 10, 10);
		result = simplifyOp.isSimpleAsFeature(env, sr4326, false, null, null);
		assertTrue(result);
		// sliver but still simple
		env.setCoords(0, 0, 0 + sr4326.getTolerance() * 2, 10);
		result = simplifyOp.isSimpleAsFeature(env, sr4326, false, null, null);
		assertTrue(result);
		// sliver and not simple
		env.setCoords(0, 0, 0 + sr4326.getTolerance() * 0.5, 10);
		result = simplifyOp.isSimpleAsFeature(env, sr4326, false, null, null);
		assertTrue(!result);
	}// done

	@Test
	public void testIsSimpleBasicsLine() {
		Line line = new Line();
		boolean result = simplifyOp.isSimpleAsFeature(line, sr4326, false,
				null, null);
		assertTrue(!result);

		line.setStart(new Point(0, 0));
		// line.setEndXY(0, 0);
		result = simplifyOp.isSimpleAsFeature(line, sr4326, false, null, null);
		assertTrue(!result);
		line.setEnd(new Point(1, 0));
		result = simplifyOp.isSimpleAsFeature(line, sr4326, false, null, null);
		assertTrue(result);
	}// done

	@Test
	public void testIsSimpleMultiPoint1() {
		MultiPoint mp = new MultiPoint();
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(result);// empty is simple
		result = simplifyOp.isSimpleAsFeature(
				simplifyOp.execute(mp, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
	}// done

	@Test
	public void testIsSimpleMultiPoint2FarApart() {
		// Two point test: far apart
		MultiPoint mp = new MultiPoint();
		mp.add(20, 10);
		mp.add(100, 100);
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(result);
		result = simplifyOp.isSimpleAsFeature(
				simplifyOp.execute(mp, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
		assertTrue(mp.getPointCount() == 2);
	}// done

	@Test
	public void testIsSimpleMultiPointCoincident() {
		// Two point test: coincident
		MultiPoint mp = new MultiPoint();
		mp.add(100, 100);
		mp.add(100, 100);
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(!result);
		MultiPoint mpS;
		result = simplifyOp.isSimpleAsFeature(
				mpS = (MultiPoint) simplifyOp.execute(mp, sr4326, false, null),
				sr4326, false, null, null);
		assertTrue(result);
		assertTrue(mpS.getPointCount() == 1);
	}// done

	@Test
	public void testMultiPointSR4326_CR184439() {
		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		OperatorSimplify simpOp = (OperatorSimplify) engine
				.getOperator(Operator.Type.Simplify);
		NonSimpleResult nonSimpResult = new NonSimpleResult();
		nonSimpResult.m_reason = NonSimpleResult.Reason.NotDetermined;
		MultiPoint multiPoint = new MultiPoint();
		multiPoint.add(0, 0);
		multiPoint.add(0, 1);
		multiPoint.add(0, 0);
		Boolean multiPointIsSimple = simpOp.isSimpleAsFeature(multiPoint,
				SpatialReference.create(4326), true, nonSimpResult, null);
		assertFalse(multiPointIsSimple);
		assertTrue(nonSimpResult.m_reason == NonSimpleResult.Reason.Clustering);
		assertTrue(nonSimpResult.m_vertexIndex1 == 0);
		assertTrue(nonSimpResult.m_vertexIndex2 == 2);
	}

	@Test
	public void testIsSimpleMultiPointCloserThanTolerance() {
		// Two point test: closer than tolerance
		MultiPoint mp = new MultiPoint();
		MultiPoint mpS;
		mp.add(100, 100);
		mp.add(100, 100 + sr4326.getTolerance() * .5);
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(result);
		result = simplifyOp.isSimpleAsFeature(
				mpS = (MultiPoint) simplifyOp.execute(mp, sr4326, false, null),
				sr4326, false, null, null);
		assertTrue(result);
		assertTrue(mpS.getPointCount() == 2);
	}// done

	@Test
	public void testIsSimpleMultiPointFarApart2() {
		// 5 point test: far apart
		MultiPoint mp = new MultiPoint();
		mp.add(100, 100);
		mp.add(100, 101);
		mp.add(101, 101);
		mp.add(11, 1);
		mp.add(11, 14);
		MultiPoint mpS;
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(result);
		result = simplifyOp.isSimpleAsFeature(
				mpS = (MultiPoint) simplifyOp.execute(mp, sr4326, false, null),
				sr4326, false, null, null);
		assertTrue(result);
		assertTrue(mpS.getPointCount() == 5);
	}// done

	@Test
	public void testIsSimpleMultiPoint_coincident2() {
		// 5 point test: coincident
		MultiPoint mp = new MultiPoint();
		mp.add(100, 100);
		mp.add(100, 101);
		mp.add(100, 100);
		mp.add(11, 1);
		mp.add(11, 14);
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(!result);
		MultiPoint mpS;
		result = simplifyOp.isSimpleAsFeature(
				mpS = (MultiPoint) simplifyOp.execute(mp, sr4326, false, null),
				sr4326, false, null, null);
		assertTrue(result);
		assertTrue(mpS.getPointCount() == 4);
		assertEquals(mpS.getPoint(0).getX(), 100, 1e-7);
		assertEquals(mpS.getPoint(0).getY(), 100, 1e-7);
		assertEquals(mpS.getPoint(1).getX(), 100, 1e-7);
		assertEquals(mpS.getPoint(1).getY(), 101, 1e-7);
		assertEquals(mpS.getPoint(2).getX(), 11, 1e-7);
		assertEquals(mpS.getPoint(2).getY(), 1, 1e-7);
		assertEquals(mpS.getPoint(3).getX(), 11, 1e-7);
		assertEquals(mpS.getPoint(3).getY(), 14, 1e-7);
	}// done

	@Test
	public void testIsSimpleMultiPointCloserThanTolerance2() {
		// 5 point test: closer than tolerance
		MultiPoint mp = new MultiPoint();
		mp.add(100, 100);
		mp.add(100, 101);
		mp.add(100, 100 + sr4326.getTolerance() / 2);
		mp.add(11, 1);
		mp.add(11, 14);
		MultiPoint mpS;
		boolean result = simplifyOp.isSimpleAsFeature(mp, sr4326, false, null,
				null);
		assertTrue(result);
		result = simplifyOp.isSimpleAsFeature(
				mpS = (MultiPoint) simplifyOp.execute(mp, sr4326, false, null),
				sr4326, false, null, null);
		assertTrue(result);
		assertTrue(mpS.getPointCount() == 5);
	}// done

	@Test
	public void testIsSimplePolyline() {
		Polyline poly = new Polyline();
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// empty is simple
	}

	@Test
	public void testIsSimplePolylineFarApart() {
		// Two point test: far apart
		Polyline poly = new Polyline();
		poly.startPath(20, 10);
		poly.lineTo(100, 100);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
	}

	@Test
	public void testIsSimplePolylineCoincident() {
		// Two point test: coincident
		Polyline poly = new Polyline();
		poly.startPath(100, 100);
		poly.lineTo(100, 100);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		@SuppressWarnings("unused")
		Polyline polyS;
		result = simplifyOp.isSimpleAsFeature(
				polyS = (Polyline) simplifyOp
						.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
	}

	@Test
	public void testIsSimplePolylineCloserThanTolerance() {
		// Two point test: closer than tolerance
		Polyline poly = new Polyline();
		poly.startPath(100, 100);
		poly.lineTo(100, 100 + sr4326.getTolerance() / 2);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		@SuppressWarnings("unused")
		Polyline polyS;
		result = simplifyOp.isSimpleAsFeature(
				polyS = (Polyline) simplifyOp
						.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
	}

	@Test
	public void testIsSimplePolylineFarApartSelfOverlap0() {
		// 3 point test: far apart, self overlapping
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(0, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// TO CONFIRM should be false
	}

	@Test
	public void testIsSimplePolylineSelfIntersect() {
		// 4 point test: far apart, self intersecting
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(0, 100);
		poly.lineTo(100, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// TO CONFIRM should be false
	}

	@Test
	public void testIsSimplePolylineDegenerateSegment() {
		// 4 point test: degenerate segment
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(100, 100 + sr4326.getTolerance() / 2);
		poly.lineTo(100, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		@SuppressWarnings("unused")
		Polyline polyS;
		result = simplifyOp.isSimpleAsFeature(
				polyS = (Polyline) simplifyOp
						.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
		{
			Polyline other = new Polyline();
			other.startPath(0, 0);
			other.lineTo(100, 100);
			other.lineTo(100, 0);
			other.equals(poly);
		}
	}

	@Test
	public void testIsSimplePolylineFarApartSelfOverlap() {
		// 3 point test: far apart, self overlapping
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(0, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// TO CONFIRM should be false
	}

	@Test
	public void testIsSimplePolylineFarApartIntersect() {
		// 4 point 2 parts test: far apart, intersecting parts
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.startPath(100, 0);
		poly.lineTo(0, 100);

		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// TO CONFIRM should be false
	}

	@Test
	public void testIsSimplePolylineFarApartOverlap2() {
		// 4 point 2 parts test: far apart, overlapping parts. second part
		// starts where first one ends
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.startPath(100, 100);
		poly.lineTo(0, 100);

		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// TO CONFIRM should be false
	}

	@Test
	public void testIsSimplePolylineDegenerateVertical() {
		// 3 point test: degenerate vertical line
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(new Point(100, 100));
		poly.lineTo(new Point(100, 100));
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		Polyline polyS;
		result = simplifyOp.isSimpleAsFeature(
				polyS = (Polyline) simplifyOp
						.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
		assertTrue(polyS.getPointCount() == 2);
	}

	@Test
	public void testIsSimplePolylineEmptyPath() {
		// TODO: any way to test this?
		// Empty path
		// Polyline poly = new Polyline();
		// assertTrue(poly.isEmpty());
		// poly.addPath(new Polyline(), 0, true);
		// assertTrue(poly.isEmpty());
		// boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
		// null, null);
		// assertTrue(result);
	}

	@Test
	public void testIsSimplePolylineSinglePointInPath() {
		// Single point in path
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.removePoint(0, 1);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		Polyline polyS;
		result = simplifyOp.isSimpleAsFeature(
				polyS = (Polyline) simplifyOp
						.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
		assertTrue(polyS.isEmpty());
	}

	@Test
	public void testIsSimplePolygon() {
		Polygon poly = new Polygon();
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);// empty is simple
		result = simplifyOp.isSimpleAsFeature(
				simplifyOp.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);// empty is simple
	}

	@Test
	public void testIsSimplePolygonEmptyPath() {
		// TODO:
		// Empty path
		// Polygon poly = new Polygon();
		// poly.addPath(new Polyline(), 0, true);
		// assertTrue(poly.getPathCount() == 1);
		// boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
		// null,
		// null);
		// assertTrue(result);
		// result = simplifyOp.isSimpleAsFeature(simplifyOp.execute(poly,
		// sr4326, false, null), sr4326, false, null, null);
		// assertTrue(result);// empty is simple
		// assertTrue(poly.getPathCount() == 1);
	}

	@Test
	public void testIsSimplePolygonIncomplete1() {
		// Incomplete polygon 1
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		// poly.removePoint(0, 1);//TO CONFIRM no removePoint method in Java
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonIncomplete2() {
		// Incomplete polygon 2
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonDegenerateTriangle() {
		// Degenerate triangle (self overlap)
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(0, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonSelfIntersect() {
		// Self intersection - cracking is needed
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(0, 100);
		poly.lineTo(100, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonRectangleHole() {
		// Rectangle and rectangular hole that has one segment overlapping
		// with the with the exterior ring. Cracking is needed.
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(-200, -100, 200, 100), false);
		poly.addEnvelope(new Envelope(-100, -100, 100, 50), true);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonRectangleHole2() {
		// Rectangle and rectangular hole
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(-200, -100, 200, 100), false);
		poly.addEnvelope(new Envelope(-100, -50, 100, 50), true);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonSelfIntersectAtVertex() {
		// Self intersection at vertex
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(50, 50);
		poly.lineTo(100, 100);
		poly.lineTo(0, 100);
		poly.lineTo(50, 50);
		poly.lineTo(100, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		result = simplifyOp.isSimpleAsFeature(
				simplifyOp.execute(poly, sr4326, false, null), sr4326, false,
				null, null);
		assertTrue(result);
	}

	@Test
	public void testIsSimplePolygon_2EdgesTouchAtVertex() {
		// No self-intersection, but more than two edges touch at the same
		// vertex. Simple for ArcGIS, not simple for OGC
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(50, 50);
		poly.lineTo(0, 100);
		poly.lineTo(100, 100);
		poly.lineTo(50, 50);
		poly.lineTo(100, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
	}

	@Test
	public void testIsSimplePolygonTriangle() {
		// Triangle
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(100, 100);
		poly.lineTo(100, 0);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonRectangle() {
		// Rectangle
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(-200, -100, 100, 200), false);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonRectangleHoleWrongDirection() {
		// Rectangle and rectangular hole that has wrong direction
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(-200, -100, 200, 100), false);
		poly.addEnvelope(new Envelope(-100, -50, 100, 50), false);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(!result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygon_2RectanglesSideBySide() {
		// Two rectangles side by side, simple
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(-200, -100, 200, 100), false);
		poly.addEnvelope(new Envelope(220, -50, 300, 50), false);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testIsSimplePolygonRectangleOneBelow() {
		// Two rectangles one below another, simple
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(50, 50, 100, 100), false);
		poly.addEnvelope(new Envelope(50, 200, 100, 250), false);
		boolean result = simplifyOp.isSimpleAsFeature(poly, sr4326, false,
				null, null);
		assertTrue(result);
		poly.reverseAllPaths();
		result = simplifyOp.isSimpleAsFeature(poly, sr4326, false, null, null);
		assertTrue(!result);
	}

	@Test
	public void testisSimpleOGC() {
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(10, 0);
		boolean result = simplifyOpOGC.isSimpleOGC(poly, sr4326, true, null,
				null);
		assertTrue(result);

		poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(10, 10);
		poly.lineTo(0, 10);
		poly.lineTo(10, 0);
		NonSimpleResult nsr = new NonSimpleResult();
		result = simplifyOpOGC.isSimpleOGC(poly, sr4326, true, nsr, null);
		assertTrue(!result);
		assertTrue(nsr.m_reason == NonSimpleResult.Reason.Cracking);

		MultiPoint mp = new MultiPoint();
		mp.add(0, 0);
		mp.add(10, 0);
		result = simplifyOpOGC.isSimpleOGC(mp, sr4326, true, null, null);
		assertTrue(result);

		mp = new MultiPoint();
		mp.add(10, 0);
		mp.add(10, 0);
		nsr = new NonSimpleResult();
		result = simplifyOpOGC.isSimpleOGC(mp, sr4326, true, nsr, null);
		assertTrue(!result);
		assertTrue(nsr.m_reason == NonSimpleResult.Reason.Clustering);
	}

	@Test
	public void testPolylineIsSimpleForOGC() {
		OperatorImportFromJson importerJson = (OperatorImportFromJson) factory
				.getOperator(Operator.Type.ImportFromJson);
		OperatorSimplify simplify = (OperatorSimplify) factory
				.getOperator(Operator.Type.Simplify);

		{
			String s = "{\"paths\":[[[0, 10], [8, 5], [5, 2], [6, 0]]]}";
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(res);
		}
		{
			String s = "{\"paths\":[[[0, 10], [6,  0], [7, 5], [0, 3]]]}";// self
																			// intersection
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(!res);
		}

		{
			String s = "{\"paths\":[[[0, 10], [6,  0], [0, 3], [0, 10]]]}"; // closed
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(res);
		}

		{
			String s = "{\"paths\":[[[0, 10], [5, 5], [6,  0], [0, 3], [5, 5], [0, 9], [0, 10]]]}"; // closed
																									// with
																									// self
																									// tangent
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(!res);
		}

		{
			String s = "{\"paths\":[[[0, 10], [5, 2]], [[5, 2], [6,  0]]]}";// two
																			// paths
																			// connected
																			// at
																			// a
																			// point
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(res);
		}

		{
			String s = "{\"paths\":[[[0, 0], [3, 3], [5, 0], [0, 0]], [[0, 10], [3, 3], [10, 10], [0, 10]]]}";// two
																												// closed
																												// rings
																												// touch
																												// at
																												// one
																												// point
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(!res);
		}

		{
			String s = "{\"paths\":[[[0, 0], [10, 10]], [[0, 10], [10, 0]]]}";// two
																				// lines
																				// intersect
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(!res);
		}

		{
			String s = "{\"paths\":[[[0, 0], [5, 5], [0, 10]], [[10, 10], [5, 5], [10, 0]]]}";// two
																								// paths
																								// share
																								// mid
																								// point.
			Geometry g = importerJson.execute(Geometry.Type.Unknown,
					JsonParserReader.createFromString(s)).getGeometry();
			boolean res = simplifyOpOGC.isSimpleOGC(g, null, true, null, null);
			assertTrue(!res);
		}

	}
	
	@Test
	public void testFillRule() {
		//self intersecting star shape
		MapGeometry mg = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, "{\"rings\":[[[0,0], [5,10], [10, 0], [0, 7], [10, 7], [0, 0]]]}");
		Polygon poly = (Polygon)mg.getGeometry();
		assertTrue(poly.getFillRule() == Polygon.FillRule.enumFillRuleOddEven);
		poly.setFillRule(Polygon.FillRule.enumFillRuleWinding);
		assertTrue(poly.getFillRule() == Polygon.FillRule.enumFillRuleWinding);
		Geometry simpleResult = OperatorSimplify.local().execute(poly, null, true, null);
		assertTrue(((Polygon)simpleResult).getFillRule() == Polygon.FillRule.enumFillRuleOddEven);
		//solid start without holes:
		MapGeometry mg1 = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, "{\"rings\":[[[0,0],[2.5925925925925926,5.185185185185185],[0,7],[3.5,7],[5,10],[6.5,7],[10,7],[7.407407407407407,5.185185185185185],[10,0],[5,3.5],[0,0]]]}");
		boolean equals = OperatorEquals.local().execute(mg1.getGeometry(), simpleResult, null, null);
		assertTrue(equals);
	}

}
