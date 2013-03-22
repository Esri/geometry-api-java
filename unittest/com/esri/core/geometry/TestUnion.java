package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestUnion extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testUnion() {
		Point pt = new Point(10, 20);
		System.out.println(pt.getX());

		Point pt2 = new Point();
		pt2.setXY(10, 10);

		Envelope env1 = new Envelope(10, 10, 30, 50);
		Envelope env2 = new Envelope(30, 10, 60, 50);
		Geometry[] geomArray = new Geometry[] { env1, env2 };
		SimpleGeometryCursor inputGeometries = new SimpleGeometryCursor(
				geomArray);
		OperatorUnion union = (OperatorUnion) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Union);

		SpatialReference sr = SpatialReference.create(4326);

		GeometryCursor outputCursor = union.execute(inputGeometries, sr, null);
		Geometry result = outputCursor.next();
		System.out.println(result);
	}
}
