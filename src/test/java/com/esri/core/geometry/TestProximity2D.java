package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestProximity2D extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testProximity_2D_1() {
		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();

		@SuppressWarnings("unused")
		OperatorProximity2D proximityOp = (OperatorProximity2D) engine
				.getOperator(Operator.Type.Proximity2D);

		Point inputPoint = new Point(3, 2);

		Point point0 = new Point(2.75, 2);
		// Point point1 = new Point(3, 2.5);
		// Point point2 = new Point(3.75, 2);
		// Point point3 = new Point(2.25, 2.5);
		// Point point4 = new Point(4, 2.25);

		// GetNearestVertices for Polygon (Native and DotNet)
		Polygon polygon = MakePolygon();

		Proximity2DResult[] resultArray = GeometryEngine.getNearestVertices(
				polygon, inputPoint, 2.0, 8);
		assertTrue(resultArray.length == 8);

		double lastdistance;
		double distance;

		Proximity2DResult result0 = resultArray[0];
		lastdistance = result0.getDistance();
		assertTrue(lastdistance <= 2.0);

		Proximity2DResult result1 = resultArray[1];
		distance = result1.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		lastdistance = distance;

		Proximity2DResult result2 = resultArray[2];
		distance = result2.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		lastdistance = distance;

		Proximity2DResult result3 = resultArray[3];
		distance = result3.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		lastdistance = distance;

		Proximity2DResult result4 = resultArray[4];
		distance = result4.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		lastdistance = distance;

		Proximity2DResult result5 = resultArray[5];
		distance = result5.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		lastdistance = distance;

		Proximity2DResult result6 = resultArray[6];
		distance = result6.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		lastdistance = distance;

		Proximity2DResult result7 = resultArray[7];
		distance = result7.getDistance();
		assertTrue(distance <= 2.0 && distance >= lastdistance);
		// lastdistance = distance;

		// Point[] coordinates = polygon.get.getCoordinates2D();
		// int pointCount = polygon.getPointCount();
		//
		// int hits = 0;
		// for (int i = 0; i < pointCount; i++)
		// {
		// Point ipoint = coordinates[i];
		// distance = Point::Distance(ipoint, inputPoint);
		//
		// if (distance < lastdistance)
		// hits++;
		// }

		// assertTrue(hits < 8);

		// GetNearestVertices for Point
		Point point = MakePoint();
		resultArray = GeometryEngine.getNearestVertices(point, inputPoint, 1.0,
				1);
		assertTrue(resultArray.length == 1);
		result0 = resultArray[0];
		Point resultPoint0 = result0.getCoordinate();
		assertTrue(resultPoint0.getX() == point.getX()
				&& resultPoint0.getY() == point.getY());

		// GetNearestVertex for Polygon
		result0 = GeometryEngine.getNearestVertex(polygon, inputPoint);
		resultPoint0 = result0.getCoordinate();
		assertTrue(resultPoint0.getX() == point0.getX()
				&& resultPoint0.getY() == point0.getY());

		// GetNearestVertex for Point
		result0 = GeometryEngine.getNearestVertex(point, inputPoint);
		resultPoint0 = result0.getCoordinate();
		assertTrue(resultPoint0.getX() == point.getX()
				&& resultPoint0.getY() == point.getY());

		// GetNearestCoordinate for Polygon
		Polygon polygon2 = MakePolygon2();
		result0 = GeometryEngine.getNearestCoordinate(polygon2, inputPoint,
				true);
		resultPoint0 = result0.getCoordinate();
		assertTrue(resultPoint0.getX() == inputPoint.getX()
				&& resultPoint0.getY() == inputPoint.getY());

		// GetNearestCoordinate for Polyline
		Polyline polyline = MakePolyline();
		result0 = GeometryEngine.getNearestCoordinate(polyline, inputPoint,
				true);
		resultPoint0 = result0.getCoordinate();
		assertTrue(resultPoint0.getX() == 0.0 && resultPoint0.getY() == 2.0);
	}

	Polygon MakePolygon() {
		Polygon poly = new Polygon();
		poly.startPath(3, -2);
		poly.lineTo(2, -1);
		poly.lineTo(3, 0);
		poly.lineTo(4, 0);

		poly.startPath(1.75, 1);
		poly.lineTo(0.75, 2);
		poly.lineTo(1.75, 3);
		poly.lineTo(2.25, 2.5);
		poly.lineTo(2.75, 2);

		poly.startPath(3, 2.5);
		poly.lineTo(2.5, 3);
		poly.lineTo(2, 3.5);
		poly.lineTo(3, 4.5);
		poly.lineTo(4, 3.5);

		poly.startPath(4.75, 1);
		poly.lineTo(3.75, 2);
		poly.lineTo(4, 2.25);
		poly.lineTo(4.75, 3);
		poly.lineTo(5.75, 2);

		return poly;
	}

	Polygon MakePolygon2() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		return poly;
	}

	Polyline MakePolyline() {
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		return poly;
	}

	Point MakePoint() {
		Point point = new Point(3, 2.5);

		return point;
	}

	@Test
	public void testProximity2D_2() {
		Point point1 = new Point(3, 2);
		Point point2 = new Point(2, 4);
		Envelope envelope = new Envelope();
		envelope.setCoords(4, 3, 7, 6);
		Polygon polygonToTest = new Polygon();
		polygonToTest.addEnvelope(envelope, false);
		Proximity2DResult prxResult1 = GeometryEngine.getNearestVertex(
				envelope, point1);
		Proximity2DResult prxResult2 = GeometryEngine.getNearestVertex(
				polygonToTest, point1);
		Proximity2DResult prxResult3 = GeometryEngine.getNearestCoordinate(
				envelope, point2, false);
		Proximity2DResult prxResult4 = GeometryEngine.getNearestCoordinate(
				polygonToTest, point2, false);

		Point result1 = prxResult1.getCoordinate();
		Point result2 = prxResult2.getCoordinate();
		assertTrue(result1.getX() == result2.getX());
		Point result3 = prxResult3.getCoordinate();
		Point result4 = prxResult4.getCoordinate();
		assertTrue(result3.getX() == result4.getX());
	}

	@Test
	public static void testProximity2D_3() {
		OperatorFactoryLocal factory = OperatorFactoryLocal.getInstance();
		OperatorProximity2D proximity = (OperatorProximity2D) factory
				.getOperator(com.esri.core.geometry.Operator.Type.Proximity2D);

		Polygon polygon = new Polygon();
		polygon.startPath(new Point(-120, 22));
		polygon.lineTo(new Point(-120, 10));
		polygon.lineTo(new Point(-110, 10));
		polygon.lineTo(new Point(-110, 22));

		Point point = new Point();
		point.setXY(-110, 20);
		Proximity2DResult result = proximity.getNearestCoordinate(polygon,
				point, false);
		System.out.println("The closest coordinate is "
				+ result.getCoordinate());
		System.out.println("The closest point is " + result.getDistance());
		Point point2 = new Point();
		point2.setXY(-120, 12);
		@SuppressWarnings("unused")
		Proximity2DResult[] results = proximity.getNearestVertices(polygon,
				point2, 10, 12);
	}
}
