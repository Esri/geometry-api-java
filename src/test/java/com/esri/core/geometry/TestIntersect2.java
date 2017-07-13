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

import com.esri.core.geometry.Geometry.Type;
import junit.framework.TestCase;
import org.junit.Test;

public class TestIntersect2 extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	/**
	 * Intersect
	 * throw InvalidShapeException when applying between polyline and polygon
	 * 
	 * */
	public void testIntersectBetweenPolylineAndPolygon() {
		Polyline basePl = new Polyline();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-117, 10));
		basePl.lineTo(new Point(-130, 10));
		basePl.lineTo(new Point(-130, 20));
		basePl.lineTo(new Point(-117, 20));

		Polygon compPl = new Polygon();
		compPl.startPath(-116, 20);
		compPl.lineTo(-131, 10);
		compPl.lineTo(-121, 50);

		Geometry intersectGeom = null;

		@SuppressWarnings("unused")
		int noException = 1; // no exception
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));

		} catch (Exception ex) {
			noException = 0;
		}
		assertNotNull(intersectGeom);

		// Geometry[] geometries = new Geometry[1];
		// geometries[0] = basePl;
		// BorgGeometryUtils.getIntersectFromRestWS(geometries, compPl, 4326);
	}

	@Test
	public void testIntersectBetweenPolylines() {
		Polyline basePl = new Polyline();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-130, 10));
		basePl.lineTo(new Point(-120, 50));

		Polyline compPl = new Polyline();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		int noException = 1; // no exception
		try {
			@SuppressWarnings("unused")
			Geometry intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));

		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
	}

	@Test
	public void testPointAndPolyline1() {
		Point basePl = new Point(-116, 20);

		Polyline compPl = new Polyline();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.Point);

		Point ip = (Point) intersectGeom;
		assertEquals(ip.getX(), -116, 0.1E7);
		assertEquals(ip.getY(), 20, 0.1E7);
	}

	@Test
	public void testPointAndPolyline2() {
		Point basePl = new Point(-115, 20);
		Polyline compPl = new Polyline();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertTrue(intersectGeom.isEmpty());
	}

	@Test
	public void testPointAndPolygon1() {
		Point basePl = new Point(-116, 20);
		Polygon compPl = new Polygon();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));

		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.Point);

		Point ip = (Point) intersectGeom;
		assertEquals(ip.getX(), -116, 0.1E7);
		assertEquals(ip.getY(), 20, 0.1E7);
	}

	@Test
	public void testPointAndPolygon2() {
		Point basePl = new Point(-115, 20);
		Polygon compPl = new Polygon();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));

		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertTrue(intersectGeom.isEmpty());
	}

	@Test
	public void testPointAndPolygon3() {
		Point basePl = new Point(-121, 20);
		Polygon compPl = new Polygon();
		compPl.startPath(new Point(-116, 20));
		compPl.lineTo(new Point(-131, 10));
		compPl.lineTo(new Point(-121, 50));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));

		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.Point);

		Point ip = (Point) intersectGeom;
		assertEquals(ip.getX(), -121, 0.1E7);
		assertEquals(ip.getY(), 20, 0.1E7);
	}

	@Test
	public void testPointAndPoint1() {
		Point basePl = new Point(-116, 20);
		Point compPl = new Point(-116, 20);

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.Point);

		Point ip = (Point) intersectGeom;
		assertEquals(ip.getX(), -116, 0.1E7);
		assertEquals(ip.getY(), 20, 0.1E7);
	}

	@Test
	public void testPointAndPoint2() {
		Point basePl = new Point(-115, 20);
		Point compPl = new Point(-116, 20);

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertTrue(intersectGeom.isEmpty());
	}

	@Test
	public void testPointAndMultiPoint1() {
		Point basePl = new Point(-116, 20);
		MultiPoint compPl = new MultiPoint();
		compPl.add(new Point(-116, 20));
		compPl.add(new Point(-118, 21));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.Point);

		Point ip = (Point) intersectGeom;
		assertEquals(ip.getX(), -116, 0.1E7);
		assertEquals(ip.getY(), 20, 0.1E7);
	}

	@Test
	public void testPointAndMultiPoint2() {
		Point basePl = new Point(-115, 20);

		MultiPoint compPl = new MultiPoint();
		compPl.add(new Point(-116, 20));
		compPl.add(new Point(-117, 21));
		compPl.add(new Point(-118, 20));
		compPl.add(new Point(-119, 21));

		int noException = 1; // no exception
		Geometry intersectGeom = null;

		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertTrue(intersectGeom.isEmpty());
	}

	@Test
	public void testMultiPointAndMultiPoint1() {
		MultiPoint basePl = new MultiPoint();
		basePl.add(new Point(-116, 20));
		basePl.add(new Point(-117, 20));

		MultiPoint compPl = new MultiPoint();
		compPl.add(new Point(-116, 20));
		compPl.add(new Point(-118, 21));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.MultiPoint);

		MultiPoint imp = (MultiPoint) intersectGeom;
		assertEquals(imp.getCoordinates2D().length, 1);
		assertEquals(imp.getCoordinates2D()[0].x, -116, 0.0);
		assertEquals(imp.getCoordinates2D()[0].y, 20, 0.0);
	}

	@Test
	public void testMultiPointAndMultiPoint2() {
		MultiPoint basePl = new MultiPoint();
		basePl.add(new Point(-116, 20));
		basePl.add(new Point(-118, 21));

		MultiPoint compPl = new MultiPoint();
		compPl.add(new Point(-116, 20));
		compPl.add(new Point(-118, 21));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}

		assertEquals(noException, 1);
		assertNotNull(intersectGeom);
		assertTrue(intersectGeom.getType() == Type.MultiPoint);

		MultiPoint ip = (MultiPoint) intersectGeom;
		assertEquals(ip.getPoint(0).getX(), -116, 0.1E7);
		assertEquals(ip.getPoint(0).getY(), 20, 0.1E7);
		assertEquals(ip.getPoint(0).getX(), -118, 0.1E7);
		assertEquals(ip.getPoint(0).getY(), 21, 0.1E7);
	}

	@Test
	public void testMultiPointAndMultiPoint3() {
		MultiPoint basePl = new MultiPoint();
		basePl.add(new Point(-116, 21));
		basePl.add(new Point(-117, 20));

		MultiPoint compPl = new MultiPoint();
		compPl.add(new Point(-116, 20));
		compPl.add(new Point(-117, 21));
		compPl.add(new Point(-118, 20));
		compPl.add(new Point(-119, 21));

		int noException = 1; // no exception
		Geometry intersectGeom = null;
		try {
			intersectGeom = GeometryEngine.intersect(basePl, compPl,
					SpatialReference.create(4326));
		} catch (Exception ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
		assertTrue(intersectGeom.isEmpty());
	}
}
