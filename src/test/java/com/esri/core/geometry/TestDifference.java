/*
 Copyright 1995-2018 Esri

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

public class TestDifference extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testDifferenceAndSymmetricDifference() {
		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		OperatorDifference differenceOp = (OperatorDifference) engine
				.getOperator(Operator.Type.Difference);

		SpatialReference spatialRef = SpatialReference.create(102113);
		Polygon polygon1 = makePolygon1();
		Polygon polygon2 = makePolygon2();
		Polyline polyline1 = makePolyline1();
		MultiPoint multipoint1 = makeMultiPoint1();
		MultiPoint multipoint2 = makeMultiPoint2();
		MultiPoint multipoint3 = makeMultiPoint3();
		Point point1 = makePoint1();
		Point point2 = makePoint2();
		Envelope envelope1 = makeEnvelope1();
		Envelope envelope2 = makeEnvelope2();
		Envelope envelope3 = makeEnvelope3();

		Polygon outputPolygon = (Polygon) differenceOp.execute(polygon1,
				polygon2, spatialRef, null);
		double area = outputPolygon.calculateArea2D();
		assertTrue(Math.abs(area - 75) <= 0.001);

		{
			Point point_1 = new Point(-130, 10);
			Point point_2 = new Point(-130, 10);
			Geometry baseGeom = new Point(point_1.getX(), point_1.getY());
			Geometry comparisonGeom = new Point(point_2.getX(), point2.getY());
			SpatialReference sr = SpatialReference.create(4326);
			@SuppressWarnings("unused")
			Geometry geom = differenceOp.execute(baseGeom, comparisonGeom, sr,
					null);
		}

		OperatorSymmetricDifference symDifferenceOp = (OperatorSymmetricDifference) engine
				.getOperator(Operator.Type.SymmetricDifference);
		outputPolygon = (Polygon) symDifferenceOp.execute(polygon1, polygon2,
				spatialRef, null);

		area = outputPolygon.calculateArea2D();
		assertTrue(Math.abs(area - 150) <= 0.001);

		Polyline outputPolyline = (Polyline) differenceOp.execute(polyline1,
				polygon1, spatialRef, null);
		double length = outputPolyline.calculateLength2D();
		assertTrue(Math.abs(length * length - 50) < 0.001);

		MultiPoint outputMultiPoint = (MultiPoint) differenceOp.execute(
				multipoint1, polygon1, spatialRef, null);
		int pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 1);

		outputMultiPoint = (MultiPoint) (symDifferenceOp.execute(multipoint1,
				point1, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 2);

		outputMultiPoint = (MultiPoint) (symDifferenceOp.execute(multipoint1,
				point2, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 4);

		outputMultiPoint = (MultiPoint) (differenceOp.execute(multipoint1,
				point1, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 2);

		outputMultiPoint = (MultiPoint) (differenceOp.execute(multipoint1,
				point2, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 3);

		outputPolygon = (Polygon) (differenceOp.execute(polygon1, envelope1,
				spatialRef, null));
		area = outputPolygon.calculateArea2D();
		assertTrue(Math.abs(area - 75) <= 0.001);

		outputPolygon = (Polygon) (differenceOp.execute(polygon2, envelope2,
				spatialRef, null));
		area = outputPolygon.calculateArea2D();
		assertTrue(Math.abs(area - 75) <= 0.001);

		outputPolyline = (Polyline) (differenceOp.execute(polyline1, envelope2,
				spatialRef, null));
		length = outputPolyline.calculateLength2D();
		assertTrue(Math.abs(length * length - 50) <= 0.001);

		outputMultiPoint = (MultiPoint) (differenceOp.execute(multipoint1,
				envelope2, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 1);

		outputMultiPoint = (MultiPoint) (differenceOp.execute(multipoint2,
				envelope2, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 6);

		outputMultiPoint = (MultiPoint) (differenceOp.execute(multipoint3,
				envelope2, spatialRef, null));
		pointCount = outputMultiPoint.getPointCount();
		assertTrue(pointCount == 0);

		Point outputPoint = (Point) (differenceOp.execute(point1, envelope2,
				spatialRef, null));
		assertTrue(!outputPoint.isEmpty());

		outputPoint = (Point) (differenceOp.execute(point2, envelope2,
				spatialRef, null));
		assertTrue(outputPoint.isEmpty());

		outputPolygon = (Polygon) (differenceOp.execute(envelope3, envelope2,
				spatialRef, null));
		assertTrue(outputPolygon != null && outputPolygon.isEmpty());

		outputPolygon = (Polygon) (symDifferenceOp.execute(envelope3,
				envelope3, spatialRef, null));
		assertTrue(outputPolygon != null && outputPolygon.isEmpty());

		outputPoint = (Point) (differenceOp.execute(point1, polygon1,
				spatialRef, null));
		assertTrue(outputPoint != null);
	}

	@Test
	public static void testPointTypes() {
		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		OperatorDifference difference = (OperatorDifference) engine
				.getOperator(Operator.Type.Difference);
		OperatorSymmetricDifference sym_difference = (OperatorSymmetricDifference) engine
				.getOperator(Operator.Type.SymmetricDifference);

		{// point/point
			Point point_1 = new Point();
			Point point_2 = new Point();
			point_1.setXY(0, 0);
			point_2.setXY(0.000000009, 0.000000009);
			Point differenced = (Point) (difference.execute(point_1, point_2,
					SpatialReference.create(4326), null));
			assertTrue(differenced.isEmpty());

			MultiPoint sym_differenced = (MultiPoint) (sym_difference.execute(
					point_1, point_2, SpatialReference.create(4326), null));
			assertTrue(sym_differenced.isEmpty());
		}

		{// point/point
			Point point_1 = new Point();
			Point point_2 = new Point();
			point_1.setXY(0, 0);
			point_2.setXY(0.000000009, 0.0);
			Point differenced = (Point) (difference.execute(point_1, point_2,
					SpatialReference.create(4326), null));
			assertTrue(differenced.isEmpty());

			MultiPoint sym_differenced = (MultiPoint) (sym_difference.execute(
					point_1, point_2, SpatialReference.create(4326), null));
			assertTrue(sym_differenced.isEmpty());
		}

		{// point/point
			Point point_1 = new Point();
			Point point_2 = new Point();
			point_1.setXY(0, 0);
			point_2.setXY(0.00000002, 0.00000002);
			Point differenced_1 = (Point) (difference.execute(point_1, point_2,
					SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());

			Point differenced_2 = (Point) (difference.execute(point_2, point_1,
					SpatialReference.create(4326), null));
			assertTrue(!differenced_2.isEmpty());

			MultiPoint sym_differenced = (MultiPoint) (sym_difference.execute(
					point_1, point_2, SpatialReference.create(4326), null));
			assertTrue(!sym_differenced.isEmpty());
			assertTrue(sym_differenced.getXY(0).x == 0
					&& sym_differenced.getXY(0).y == 0);
			assertTrue(sym_differenced.getXY(1).x == 0.00000002
					&& sym_differenced.getXY(1).y == 0.00000002);
		}

		{// multi_point/point
			MultiPoint multi_point_1 = new MultiPoint();
			Point point_2 = new Point();
			multi_point_1.add(0, 0);
			multi_point_1.add(1, 1);
			point_2.setXY(0.000000009, 0.000000009);
			MultiPoint differenced_1 = (MultiPoint) (difference
					.execute(multi_point_1, point_2,
							SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1.getPointCount() == 1);
			assertTrue(differenced_1.getXY(0).x == 1
					&& differenced_1.getXY(0).y == 1);

			Point differenced_2 = (Point) (difference.execute(point_2,
					multi_point_1, SpatialReference.create(4326), null));
			assertTrue(differenced_2.isEmpty());
		}

		{// multi_point/point
			MultiPoint multi_point_1 = new MultiPoint();
			Point point_2 = new Point();
			multi_point_1.add(0, 0);
			multi_point_1.add(1, 1);
			point_2.setXY(0.000000009, 0.0);
			MultiPoint differenced_1 = (MultiPoint) (difference
					.execute(multi_point_1, point_2,
							SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1.getXY(0).x == 1.0
					&& differenced_1.getXY(0).y == 1.0);

			Point differenced_2 = (Point) (difference.execute(point_2,
					multi_point_1, SpatialReference.create(4326), null));
			assertTrue(differenced_2.isEmpty());

			MultiPoint sym_differenced = (MultiPoint) (sym_difference
					.execute(multi_point_1, point_2,
							SpatialReference.create(4326), null));
			assertTrue(!sym_differenced.isEmpty());
			assertTrue(sym_differenced.getPointCount() == 1);
			assertTrue(sym_differenced.getXY(0).x == 1
					&& sym_differenced.getXY(0).y == 1);
		}

		{// multi_point/point
			MultiPoint multi_point_1 = new MultiPoint();
			Point point_2 = new Point();
			multi_point_1.add(0, 0);
			multi_point_1.add(0, 0);
			point_2.setXY(0.000000009, 0.0);
			MultiPoint differenced_1 = (MultiPoint) (difference
					.execute(multi_point_1, point_2,
							SpatialReference.create(4326), null));
			assertTrue(differenced_1.isEmpty());

			MultiPoint sym_differenced = (MultiPoint) (sym_difference
					.execute(multi_point_1, point_2,
							SpatialReference.create(4326), null));
			assertTrue(sym_differenced.isEmpty());
		}

		{// multi_point/polygon
			MultiPoint multi_point_1 = new MultiPoint();
			Polygon polygon_2 = new Polygon();
			multi_point_1.add(0, 0);
			multi_point_1.add(0, 0);
			multi_point_1.add(2, 2);

			polygon_2.startPath(-1, -1);
			polygon_2.lineTo(-1, 1);
			polygon_2.lineTo(1, 1);
			polygon_2.lineTo(1, -1);
			MultiPoint differenced_1 = (MultiPoint) (difference.execute(
					multi_point_1, polygon_2, SpatialReference.create(4326),
					null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1.getPointCount() == 1);
			assertTrue(differenced_1.getXY(0).x == 2
					&& differenced_1.getXY(0).y == 2);
		}

		{// multi_point/polygon
			MultiPoint multi_point_1 = new MultiPoint();
			Polygon polygon_2 = new Polygon();
			multi_point_1.add(0, 0);
			multi_point_1.add(0, 0);
			multi_point_1.add(1, 1);

			polygon_2.startPath(-1, -1);
			polygon_2.lineTo(-1, 1);
			polygon_2.lineTo(1, 1);
			polygon_2.lineTo(1, -1);
			MultiPoint differenced_1 = (MultiPoint) (difference.execute(
					multi_point_1, polygon_2, SpatialReference.create(4326),
					null));
			assertTrue(differenced_1.isEmpty());
		}

		{// multi_point/envelope
			MultiPoint multi_point_1 = new MultiPoint();
			Envelope envelope_2 = new Envelope();
			multi_point_1.add(-2, 0);
			multi_point_1.add(0, 2);
			multi_point_1.add(2, 0);
			multi_point_1.add(0, -2);

			envelope_2.setCoords(-1, -1, 1, 1);
			MultiPoint differenced_1 = (MultiPoint) (difference.execute(
					multi_point_1, envelope_2, SpatialReference.create(4326),
					null));
			assertTrue(!differenced_1.isEmpty()
					&& differenced_1 == multi_point_1);
		}

		{// multi_point/polygon
			MultiPoint multi_point_1 = new MultiPoint();
			Polygon polygon_2 = new Polygon();
			multi_point_1.add(2, 2);
			multi_point_1.add(2, 2);
			multi_point_1.add(-2, -2);

			polygon_2.startPath(-1, -1);
			polygon_2.lineTo(-1, 1);
			polygon_2.lineTo(1, 1);
			polygon_2.lineTo(1, -1);
			MultiPoint differenced_1 = (MultiPoint) (difference.execute(
					multi_point_1, polygon_2, SpatialReference.create(4326),
					null));
			assertTrue(!differenced_1.isEmpty()
					&& differenced_1 == multi_point_1);
		}

		{// point/polygon
			Point point_1 = new Point();
			Polygon polygon_2 = new Polygon();
			point_1.setXY(0, 0);

			polygon_2.startPath(-1, -1);
			polygon_2.lineTo(-1, 1);
			polygon_2.lineTo(1, 1);
			polygon_2.lineTo(1, -1);
			Point differenced_1 = (Point) (difference.execute(point_1,
					polygon_2, SpatialReference.create(4326), null));
			assertTrue(differenced_1.isEmpty());

			polygon_2.setEmpty();
			polygon_2.startPath(1, 1);
			polygon_2.lineTo(1, 2);
			polygon_2.lineTo(2, 2);
			polygon_2.lineTo(2, 1);
			differenced_1 = (Point) (difference.execute(point_1, polygon_2,
					SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1 == point_1);
		}

		{// point/polygon
			Point point_1 = new Point();
			Polygon polygon_2 = new Polygon();
			point_1.setXY(0, 0);

			polygon_2.startPath(1, 0);
			polygon_2.lineTo(0, 1);
			polygon_2.lineTo(1, 1);
			Point differenced_1 = (Point) (difference.execute(point_1,
					polygon_2, SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1 == point_1);

			point_1.setEmpty();
			point_1.setXY(0.5, 0.5);

			polygon_2.setEmpty();
			polygon_2.startPath(1, 0);
			polygon_2.lineTo(0, 1);
			polygon_2.lineTo(1, 1);
			differenced_1 = (Point) (difference.execute(point_1, polygon_2,
					SpatialReference.create(4326), null));
			assertTrue(differenced_1.isEmpty());
		}

		{// point/envelope
			Point point_1 = new Point();
			Envelope envelope_2 = new Envelope();
			point_1.setXY(0, 0);

			envelope_2.setCoords(-1, -1, 1, 1);
			Point differenced_1 = (Point) (difference.execute(point_1,
					envelope_2, SpatialReference.create(4326), null));
			assertTrue(differenced_1.isEmpty());

			envelope_2.setEmpty();
			envelope_2.setCoords(1, 1, 2, 2);
			differenced_1 = (Point) (difference.execute(point_1, envelope_2,
					SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1 == point_1);
		}

		{// point/polyline
			Point point_1 = new Point();
			Polyline polyline_2 = new Polyline();
			point_1.setXY(0, 0);

			polyline_2.startPath(-1, 0);
			polyline_2.lineTo(1, 0);
			Point differenced_1 = (Point) (difference.execute(point_1,
					polyline_2, SpatialReference.create(4326), null));
			assertTrue(differenced_1.isEmpty());

			polyline_2.setEmpty();
			polyline_2.startPath(1, 0);
			polyline_2.lineTo(2, 0);
			differenced_1 = (Point) (difference.execute(point_1, polyline_2,
					SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1 == point_1);

			polyline_2.setEmpty();
			polyline_2.startPath(-1, -1);
			polyline_2.lineTo(-1, 1);
			polyline_2.lineTo(1, 1);
			polyline_2.lineTo(1, -1);
			differenced_1 = (Point) (difference.execute(point_1, polyline_2,
					SpatialReference.create(4326), null));
			assertTrue(!differenced_1.isEmpty());
			assertTrue(differenced_1 == point_1);
		}
	}

	@Test
	public static void testDifferenceOnPolyline() {
		// # * * #
		// # * @
		// # @ *
		// # *
		//
		// ///////////////////////////////
		//
		// The polyline drawn in *s represents basePl
		// The polyline drawn in #s represents compPl
		// The @ represents their intersection points, so that
		// the difference polyline will be basePl with two new vertices @ added.

		Polyline basePl = new Polyline();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-130, 10));
		basePl.lineTo(new Point(-120, 50));

		Polyline compPl = new Polyline();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		Geometry diffGeom = GeometryEngine.difference(basePl, compPl,
				SpatialReference.create(4326));
		assertTrue(diffGeom instanceof Polyline);
		Polyline diffPolyline = (Polyline) diffGeom;
		int pointCountDiffPolyline = diffPolyline.getPointCount();

		// first line in comp_pl is 3y = 2x + 292
		assertEquals(3 * 20, 2 * (-116) + 292);
		assertEquals(3 * 10, 2 * (-131) + 292);

		// new points should also lie on this line
		assertTrue(3.0 * diffPolyline.getCoordinates2D()[1].y - 2.0
				* diffPolyline.getCoordinates2D()[1].x - 292.0 == 0.0);
		assertTrue(3.0 * diffPolyline.getCoordinates2D()[3].y - 2.0
				* diffPolyline.getCoordinates2D()[3].x - 292.0 == 0.0);

		for (int i = 0; i < 3; i++) {
			assertTrue(basePl.getCoordinates2D()[i].x == diffPolyline
					.getCoordinates2D()[2 * i].x);
			assertTrue(basePl.getCoordinates2D()[i].y == diffPolyline
					.getCoordinates2D()[2 * i].y);
		}

		assertEquals(5, pointCountDiffPolyline);
	}
	
	@Test
	public static void testDifferencePolylineAlongPolygonBoundary() {
		Polyline polyline = (Polyline)GeometryEngine.geometryFromWkt("LINESTRING(0 0, 0 5, -2 5)", 0, Geometry.Type.Unknown);
		Polygon polygon = (Polygon)GeometryEngine.geometryFromWkt("POLYGON((0 0, 5 0, 5 5, 0 5, 0 0))", 0, Geometry.Type.Unknown);
		Geometry result = OperatorDifference.local().execute(polyline,  polygon, null,  null);
		assertEquals(GeometryEngine.geometryToJson(null, result), "{\"paths\":[[[0,5],[-2,5]]]}");
	}

	public static Polygon makePolygon1() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		return poly;
	}

	public static Polygon makePolygon2() {
		Polygon poly = new Polygon();

		poly.startPath(5, 5);
		poly.lineTo(5, 15);
		poly.lineTo(15, 15);
		poly.lineTo(15, 5);

		return poly;
	}

	public static Polyline makePolyline1() {
		Polyline poly = new Polyline();
		poly.startPath(0, 0);
		poly.lineTo(15, 15);

		return poly;
	}

	public static MultiPoint makeMultiPoint1() {
		MultiPoint mpoint = new MultiPoint();
		Point2D pt1 = new Point2D();
		pt1.x = 1.0;
		pt1.y = 1.0;

		Point2D pt2 = new Point2D();
		pt2.x = 5.0;
		pt2.y = 5.0;

		Point2D pt3 = new Point2D();
		pt3.x = 15.0;
		pt3.y = 15.0;

		mpoint.add(pt1.x, pt1.y);
		mpoint.add(pt2.x, pt2.y);
		mpoint.add(pt3.x, pt3.y);

		return mpoint;
	}

	public static MultiPoint makeMultiPoint2() {
		MultiPoint mpoint = new MultiPoint();
		Point2D pt1 = new Point2D();
		pt1.x = 1.0;
		pt1.y = 1.0;

		Point2D pt2 = new Point2D();
		pt2.x = 1.0;
		pt2.y = 1.0;

		Point2D pt3 = new Point2D();
		pt3.x = 15.0;
		pt3.y = 15.0;

		Point2D pt4 = new Point2D();
		pt4.x = 15.0;
		pt4.y = 15.0;

		Point2D pt5 = new Point2D();
		pt5.x = 1.0;
		pt5.y = 1.0;

		Point2D pt6 = new Point2D();
		pt6.x = 1.0;
		pt6.y = 1.0;

		Point2D pt7 = new Point2D();
		pt7.x = 15.0;
		pt7.y = 15.0;

		Point2D pt8 = new Point2D();
		pt8.x = 15.0;
		pt8.y = 15.0;

		Point2D pt9 = new Point2D();
		pt9.x = 15.0;
		pt9.y = 15.0;

		Point2D pt10 = new Point2D();
		pt10.x = 1.0;
		pt10.y = 1.0;

		Point2D pt11 = new Point2D();
		pt11.x = 15.0;
		pt11.y = 15.0;

		mpoint.add(pt1.x, pt1.y);
		mpoint.add(pt2.x, pt2.y);
		mpoint.add(pt3.x, pt3.y);
		mpoint.add(pt4.x, pt4.y);
		mpoint.add(pt5.x, pt5.y);
		mpoint.add(pt6.x, pt6.y);
		mpoint.add(pt7.x, pt7.y);
		mpoint.add(pt8.x, pt8.y);
		mpoint.add(pt9.x, pt9.y);
		mpoint.add(pt10.x, pt10.y);
		mpoint.add(pt11.x, pt11.y);

		return mpoint;
	}

	public static MultiPoint makeMultiPoint3() {
		MultiPoint mpoint = new MultiPoint();
		Point2D pt1 = new Point2D();
		pt1.x = 1.0;
		pt1.y = 1.0;

		Point2D pt2 = new Point2D();
		pt2.x = 5.0;
		pt2.y = 5.0;

		mpoint.add(pt1.x, pt1.y);
		mpoint.add(pt2.x, pt2.y);

		return mpoint;
	}

	public static Point makePoint1() {
		Point point = new Point();

		Point2D pt = new Point2D();
		pt.setCoords(15, 15);
		point.setXY(pt);

		return point;
	}

	public static Point makePoint2() {
		Point point = new Point();

		Point2D pt = new Point2D();
		pt.setCoords(7, 7);
		point.setXY(pt);

		return point;
	}

	public static Envelope makeEnvelope1() {
		Envelope2D env = new Envelope2D();
		env.setCoords(5, 5, 15, 15);
		Envelope envelope = new Envelope(env);

		return envelope;
	}

	public static Envelope makeEnvelope2() {
		Envelope2D env = new Envelope2D();
		env.setCoords(0, 0, 10, 10);
		Envelope envelope = new Envelope(env);

		return envelope;
	}

	public static Envelope makeEnvelope3() {
		Envelope2D env = new Envelope2D();
		env.setCoords(5, 5, 6, 6);
		Envelope envelope = new Envelope(env);

		return envelope;
	}
}
