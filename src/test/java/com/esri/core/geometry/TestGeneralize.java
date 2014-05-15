package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestGeneralize extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void test1() {
		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		OperatorGeneralize op = (OperatorGeneralize) engine
				.getOperator(Operator.Type.Generalize);

		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(1, 1);
		poly.lineTo(2, 0);
		poly.lineTo(3, 2);
		poly.lineTo(4, 1);
		poly.lineTo(5, 0);
		poly.lineTo(5, 10);
		poly.lineTo(0, 10);
		Geometry geom = op.execute(poly, 2, true, null);
		Polygon p = (Polygon) geom;
		Point2D[] points = p.getCoordinates2D();
		assertTrue(points.length == 4);
		assertTrue(points[0].x == 0 && points[0].y == 0);
		assertTrue(points[1].x == 5 && points[1].y == 0);
		assertTrue(points[2].x == 5 && points[2].y == 10);
		assertTrue(points[3].x == 0 && points[3].y == 10);

		Geometry geom1 = op.execute(geom, 5, false, null);
		p = (Polygon) geom1;
		points = p.getCoordinates2D();
		assertTrue(points.length == 3);
		assertTrue(points[0].x == 0 && points[0].y == 0);
		assertTrue(points[1].x == 5 && points[1].y == 10);
		assertTrue(points[2].x == 5 && points[2].y == 10);

		geom1 = op.execute(geom, 5, true, null);
		p = (Polygon) geom1;
		points = p.getCoordinates2D();
		assertTrue(points.length == 0);
	}

	@Test
	public static void test2() {
		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		OperatorGeneralize op = (OperatorGeneralize) engine
				.getOperator(Operator.Type.Generalize);

		Polyline polyline = new Polyline();
		polyline.startPath(0, 0);
		polyline.lineTo(1, 1);
		polyline.lineTo(2, 0);
		polyline.lineTo(3, 2);
		polyline.lineTo(4, 1);
		polyline.lineTo(5, 0);
		polyline.lineTo(5, 10);
		polyline.lineTo(0, 10);
		Geometry geom = op.execute(polyline, 2, true, null);
		Polyline p = (Polyline) geom;
		Point2D[] points = p.getCoordinates2D();
		assertTrue(points.length == 4);
		assertTrue(points[0].x == 0 && points[0].y == 0);
		assertTrue(points[1].x == 5 && points[1].y == 0);
		assertTrue(points[2].x == 5 && points[2].y == 10);
		assertTrue(points[3].x == 0 && points[3].y == 10);

		Geometry geom1 = op.execute(geom, 5, false, null);
		p = (Polyline) geom1;
		points = p.getCoordinates2D();
		assertTrue(points.length == 2);
		assertTrue(points[0].x == 0 && points[0].y == 0);
		assertTrue(points[1].x == 0 && points[1].y == 10);

		geom1 = op.execute(geom, 5, true, null);
		p = (Polyline) geom1;
		points = p.getCoordinates2D();
		assertTrue(points.length == 2);
		assertTrue(points[0].x == 0 && points[0].y == 0);
		assertTrue(points[1].x == 0 && points[1].y == 10);
	}
}
