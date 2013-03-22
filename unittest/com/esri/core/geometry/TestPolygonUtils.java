package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestPolygonUtils extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testPointInAnyOuterRing() {
		Polygon polygon = new Polygon();
		// outer ring1
		polygon.startPath(-200, -100);
		polygon.lineTo(200, -100);
		polygon.lineTo(200, 100);
		polygon.lineTo(-190, 100);
		polygon.lineTo(-190, 90);
		polygon.lineTo(-200, 90);

		// hole
		polygon.startPath(-100, 50);
		polygon.lineTo(100, 50);
		polygon.lineTo(100, -40);
		polygon.lineTo(90, -40);
		polygon.lineTo(90, -50);
		polygon.lineTo(-100, -50);

		// island
		polygon.startPath(-10, -10);
		polygon.lineTo(10, -10);
		polygon.lineTo(10, 10);
		polygon.lineTo(-10, 10);

		// outer ring2
		polygon.startPath(300, 300);
		polygon.lineTo(310, 300);
		polygon.lineTo(310, 310);
		polygon.lineTo(300, 310);

		polygon.reverseAllPaths();

		Point2D testPointIn1 = new Point2D(1, 2); // inside the island
		Point2D testPointIn2 = new Point2D(190, 90); // inside, betwen outer
														// ring1 and the hole
		Point2D testPointIn3 = new Point2D(305, 305); // inside the outer ring2
		Point2D testPointOut1 = new Point2D(300, 2); // outside any
		Point2D testPointOut2 = new Point2D(-195, 95); // outside any (in the
														// concave area of outer
														// ring 2)
		Point2D testPointOut3 = new Point2D(99, 49); // outside (in the hole)

		PolygonUtils.PiPResult res;
		// is_point_in_polygon_2D
		res = PolygonUtils.isPointInPolygon2D(polygon, testPointIn1, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);
		res = PolygonUtils.isPointInPolygon2D(polygon, testPointIn2, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);
		res = PolygonUtils.isPointInPolygon2D(polygon, testPointIn3, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);

		res = PolygonUtils.isPointInPolygon2D(polygon, testPointOut1, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
		res = PolygonUtils.isPointInPolygon2D(polygon, testPointOut2, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
		res = PolygonUtils.isPointInPolygon2D(polygon, testPointOut3, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPOutside);

		// Ispoint_in_any_outer_ring
		res = PolygonUtils.isPointInAnyOuterRing(polygon, testPointIn1, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);
		res = PolygonUtils.isPointInAnyOuterRing(polygon, testPointIn2, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);
		res = PolygonUtils.isPointInAnyOuterRing(polygon, testPointIn3, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);

		res = PolygonUtils.isPointInAnyOuterRing(polygon, testPointOut1, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
		res = PolygonUtils.isPointInAnyOuterRing(polygon, testPointOut2, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
		res = PolygonUtils.isPointInAnyOuterRing(polygon, testPointOut3, 0);
		assertTrue(res == PolygonUtils.PiPResult.PiPInside);// inside of outer
															// ring
	}

	@Test
	public static void testPointInPolygonBugCR181840() {
		PolygonUtils.PiPResult res;
		{// pointInPolygonBugCR181840 - point in polygon bug
			Polygon polygon = new Polygon();
			// outer ring1
			polygon.startPath(0, 0);
			polygon.lineTo(10, 10);
			polygon.lineTo(20, 0);

			res = PolygonUtils.isPointInPolygon2D(polygon,
					Point2D.construct(15, 10), 0);
			assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
			res = PolygonUtils.isPointInPolygon2D(polygon,
					Point2D.construct(2, 10), 0);
			assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
			res = PolygonUtils.isPointInPolygon2D(polygon,
					Point2D.construct(5, 5), 0);
			assertTrue(res == PolygonUtils.PiPResult.PiPInside);
		}

		{// CR181840 - point in polygon bug
			Polygon polygon = new Polygon();
			// outer ring1
			polygon.startPath(10, 10);
			polygon.lineTo(20, 0);
			polygon.lineTo(0, 0);

			res = PolygonUtils.isPointInPolygon2D(polygon,
					Point2D.construct(15, 10), 0);
			assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
			res = PolygonUtils.isPointInPolygon2D(polygon,
					Point2D.construct(2, 10), 0);
			assertTrue(res == PolygonUtils.PiPResult.PiPOutside);
			res = PolygonUtils.isPointInPolygon2D(polygon,
					Point2D.construct(5, 5), 0);
			assertTrue(res == PolygonUtils.PiPResult.PiPInside);
		}
	}
}
