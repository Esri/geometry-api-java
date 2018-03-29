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

		MultiPath path = (MultiPath)result;
		assertEquals(1, path.getPathCount());
		assertEquals(6, path.getPathEnd(0));
		assertEquals(new Point2D(10, 10), path.getXY(0));
		assertEquals(new Point2D(10, 50), path.getXY(1));
		assertEquals(new Point2D(30, 50), path.getXY(2));
		assertEquals(new Point2D(60, 50), path.getXY(3));
		assertEquals(new Point2D(60, 10), path.getXY(4));
		assertEquals(new Point2D(30, 10), path.getXY(5));
	}

	@Test
	public static void testUnionDistinctGeometries() {
		Envelope env = new Envelope(1, 5, 3, 10);

		Polygon polygon = new Polygon();
		polygon.startPath(new Point(4, 3));
		polygon.lineTo(new Point(7, 6));
		polygon.lineTo(new Point(6, 8));
		polygon.lineTo(new Point(4, 3));

		Geometry[] geomArray = new Geometry[] { env, polygon };
		SimpleGeometryCursor inputGeometries = new SimpleGeometryCursor(
				geomArray);
		OperatorUnion union = (OperatorUnion) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Union);
		SpatialReference sr = SpatialReference.create(4326);

		GeometryCursor outputCursor = union.execute(inputGeometries, sr, null);
		Geometry result = outputCursor.next();

		MultiPath path = (MultiPath)result;
		assertEquals(2, path.getPathCount());

		assertEquals(3, path.getPathEnd(0));
		assertEquals(7, path.getPathEnd(1));
		// from polygon
		assertEquals(new Point2D(4, 3), path.getXY(0));
		assertEquals(new Point2D(6, 8), path.getXY(1));
		assertEquals(new Point2D(7, 6), path.getXY(2));
		// from envelope
		assertEquals(new Point2D(1, 5), path.getXY(3));
		assertEquals(new Point2D(1, 10), path.getXY(4));
		assertEquals(new Point2D(3, 10), path.getXY(5));
		assertEquals(new Point2D(3, 5), path.getXY(6));
	}

	@Test
	public static void testUnionCoincidentPolygons() {
		Polygon polygon1 = new Polygon();
		polygon1.startPath(new Point(3, 2));
		polygon1.lineTo(new Point(1, 2));
		polygon1.lineTo(new Point(1, 4));
		polygon1.lineTo(new Point(3, 4));
		polygon1.lineTo(new Point(3, 2));

		Polygon polygon2 = new Polygon();
		polygon2.startPath(new Point(1, 2));
		polygon2.lineTo(new Point(1, 4));
		polygon2.lineTo(new Point(3, 4));
		polygon2.lineTo(new Point(3, 2));
		polygon2.lineTo(new Point(1, 2));

		Geometry[] geomArray = new Geometry[] { polygon1, polygon2 };
		SimpleGeometryCursor inputGeometries = new SimpleGeometryCursor(
				geomArray);
		OperatorUnion union = (OperatorUnion) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Union);
		SpatialReference sr = SpatialReference.create(4326);

		GeometryCursor outputCursor = union.execute(inputGeometries, sr, null);
		Geometry result = outputCursor.next();

		MultiPath path = (MultiPath)result;
		assertEquals(1, path.getPathCount());
		assertEquals(4, path.getPathEnd(0));
		assertEquals(new Point2D(1, 2), path.getXY(0));
		assertEquals(new Point2D(1, 4), path.getXY(1));
		assertEquals(new Point2D(3, 4), path.getXY(2));
		assertEquals(new Point2D(3, 2), path.getXY(3));
	}

	@Test
	public static void testUnionCoincidentPolygonsWithReverseWinding() {
		// Input polygons have CCW winding, result is always CW
		Polygon polygon1 = new Polygon();
		polygon1.startPath(new Point(3, 2));
		polygon1.lineTo(new Point(3, 4));
		polygon1.lineTo(new Point(1, 4));
		polygon1.lineTo(new Point(1, 2));
		polygon1.lineTo(new Point(3, 2));

		Polygon polygon2 = new Polygon();
		polygon2.startPath(new Point(1, 2));
		polygon2.lineTo(new Point(3, 2));
		polygon2.lineTo(new Point(3, 4));
		polygon2.lineTo(new Point(1, 4));
		polygon2.lineTo(new Point(1, 2));

		Polygon expectedPolygon = new Polygon();
		expectedPolygon.startPath(new Point(1, 2));
		expectedPolygon.lineTo(new Point(1, 4));
		expectedPolygon.lineTo(new Point(3, 4));
		expectedPolygon.lineTo(new Point(3, 2));
		expectedPolygon.lineTo(new Point(1, 2));

		Geometry[] geomArray = new Geometry[] { polygon1, polygon2 };
		SimpleGeometryCursor inputGeometries = new SimpleGeometryCursor(
				geomArray);
		OperatorUnion union = (OperatorUnion) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Union);
		SpatialReference sr = SpatialReference.create(4326);

		GeometryCursor outputCursor = union.execute(inputGeometries, sr, null);
		Geometry result = outputCursor.next();

		MultiPath path = (MultiPath)result;
		assertEquals(1, path.getPathCount());
		assertEquals(4, path.getPathEnd(0));
		assertEquals(new Point2D(1, 2), path.getXY(0));
		assertEquals(new Point2D(1, 4), path.getXY(1));
		assertEquals(new Point2D(3, 4), path.getXY(2));
		assertEquals(new Point2D(3, 2), path.getXY(3));
	}
}
