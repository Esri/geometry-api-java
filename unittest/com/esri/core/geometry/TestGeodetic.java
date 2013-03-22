package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestGeodetic extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testTriangleLength() {
		Point pt_0 = new Point(10, 10);
		Point pt_1 = new Point(20, 20);
		Point pt_2 = new Point(20, 10);
		double length = 0.0;
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_0, pt_1);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_1, pt_2);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_2, pt_0);
		assertTrue(Math.abs(length - 3744719.4094597572) < 1e-13 * 3744719.4094597572);
	}

	@Test
	public static void testRotationInvariance() {
		Point pt_0 = new Point(10, 40);
		Point pt_1 = new Point(20, 60);
		Point pt_2 = new Point(20, 40);
		double length = 0.0;
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_0, pt_1);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_1, pt_2);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_2, pt_0);
		assertTrue(Math.abs(length - 5409156.3896271614) < 1e-13 * 5409156.3896271614);

		for (int i = -540; i < 540; i += 5) {
			pt_0.setXY(i + 10, 40);
			pt_1.setXY(i + 20, 60);
			pt_2.setXY(i + 20, 40);
			length = 0.0;
			length += GeometryEngine.geodesicDistanceOnWGS84(pt_0, pt_1);
			length += GeometryEngine.geodesicDistanceOnWGS84(pt_1, pt_2);
			length += GeometryEngine.geodesicDistanceOnWGS84(pt_2, pt_0);
			assertTrue(Math.abs(length - 5409156.3896271614) < 1e-13 * 5409156.3896271614);
		}
	}

	@Test
	public static void testLengthAccurateCR191313() {
		/*
		 * // random_test(); OperatorFactoryLocal engine =
		 * OperatorFactoryLocal.getInstance(); //TODO: Make this:
		 * OperatorShapePreservingLength geoLengthOp =
		 * (OperatorShapePreservingLength)
		 * factory.getOperator(Operator.Type.ShapePreservingLength);
		 * SpatialReference spatialRef = SpatialReference.create(102631);
		 * //[6097817.59407673
		 * ,17463475.2931517],[-1168053.34617516,11199801.3734424
		 * ]]],"spatialReference":{"wkid":102631}
		 * 
		 * Polyline polyline = new Polyline();
		 * polyline.startPath(6097817.59407673, 17463475.2931517);
		 * polyline.lineTo(-1168053.34617516, 11199801.3734424); double length =
		 * geoLengthOp.execute(polyline, spatialRef, null);
		 * assertTrue(Math.abs(length - 2738362.3249366437) < 2e-9 * length);
		 */
	}
}
