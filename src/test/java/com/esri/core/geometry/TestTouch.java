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

public class TestTouch extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testTouchOnPointAndPolyline() {
		Geometry baseGeom = new Point(-130, 10);
		Polyline pl = new Polyline();
		pl.startPath(new Point(-130, 10));
		pl.lineTo(-131, 15);
		pl.lineTo(-140, 20);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		try {
			isTouched = GeometryEngine.touches(baseGeom, pl, sr);
			isTouched2 = GeometryEngine.touches(pl, baseGeom, sr);

		} catch (IllegalArgumentException ex) {
			isTouched = false;
			isTouched2 = false;
		}
		assertEquals(isTouched && isTouched2, true);
	}

	@Test
	public void testTouchOnPointAndPolygon() {
		Geometry baseGeom = new Point(-130, 10);
		Polygon pg = new Polygon();
		pg.startPath(new Point(-130, 10));
		pg.lineTo(-131, 15);
		pg.lineTo(-140, 20);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		try {
			isTouched = GeometryEngine.touches(baseGeom, pg, sr);
			isTouched2 = GeometryEngine.touches(pg, baseGeom, sr);

		} catch (IllegalArgumentException ex) {
			isTouched = false;
			isTouched2 = false;
		}
		assertEquals(isTouched && isTouched2, true);
	}

	@Test
	public void testTouchOnPolygons() {
		Polygon pg = new Polygon();
		pg.startPath(new Point(-130, 10));
		pg.lineTo(-131, 15);
		pg.lineTo(-140, 20);

		Polygon pg2 = new Polygon();
		pg2.startPath(new Point(-130, 10));
		pg2.lineTo(-131, 15);
		pg2.lineTo(-120, 20);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		try {
			isTouched = GeometryEngine.touches(pg, pg2, sr);
			isTouched2 = GeometryEngine.touches(pg2, pg, sr);

		} catch (IllegalArgumentException ex) {
			isTouched = false;
			isTouched2 = false;
		}
		assertEquals(isTouched && isTouched2, true);

		// boolean isTouchedFromRest = GeometryUtils.isRelationTrue(pg2, pg, sr,
		// GeometryUtils.SpatialRelationType.esriGeometryRelationTouch, "");
		// assertTrue(isTouchedFromRest==isTouched);
	}

	@Test
	public void testTouchesOnPolylines() {
		SpatialReference sr = SpatialReference.create(4326);

		Polyline basePl = new Polyline();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-100, 20));

		basePl.lineTo(new Point(-100, 10));
		basePl.lineTo(new Point(-117, 10));
		basePl.lineTo(new Point(-117, 20));

		Polyline compPl = new Polyline();
		compPl.startPath(new Point(-104, 20));

		compPl.lineTo(new Point(-108, 25));

		compPl.lineTo(new Point(-100, 20));
		// compPl.lineTo(new Point(-100, 30));
		// compPl.lineTo(new Point(-117, 30));
		// compPl.lineTo(new Point(-117, 20));

		boolean isTouched;
		try {
			isTouched = GeometryEngine.touches(basePl, compPl, sr);

		} catch (IllegalArgumentException ex) {
			isTouched = false;
		}
		assertEquals(isTouched, true);
	}

	@Test
	public void testTouchesOnPolylineAndPolygon() {
		SpatialReference sr = SpatialReference.create(4326);

		Polygon basePl = new Polygon();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-100, 20));

		basePl.lineTo(new Point(-100, 10));
		basePl.lineTo(new Point(-117, 10));

		Polyline compPl = new Polyline();

		compPl.startPath(new Point(-117, 20));
		compPl.lineTo(new Point(-108, 25));
		compPl.lineTo(new Point(-100, 20));
		compPl.lineTo(new Point(-100, 30));

		boolean isTouched;
		try {
			isTouched = GeometryEngine.touches(basePl, compPl, sr);
		} catch (IllegalArgumentException ex) {
			isTouched = false;
		}
		assertEquals(isTouched, true);

	}

	@Test
	public void testTouchOnEnvelopes() {
		// case1, not touched
		// Envelope env = new Envelope(new Point(-117,20), 12, 12);
		// Envelope env2 = new Envelope(-100,20,-80,30);

		// case2 touched
		Envelope env = new Envelope(new Point(-117, 20), 12, 12);
		Envelope env2 = new Envelope(-117, 26, -80, 30);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		try {
			isTouched = GeometryEngine.touches(env, env2, sr);
		} catch (IllegalArgumentException ex) {
			isTouched = false;
		}
		assertEquals(isTouched, true);

	}

	@Test
	public void testTouchesOnPolylineAndEnvelope() {
		SpatialReference sr = SpatialReference.create(4326);

		Polyline basePl = new Polyline();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-100, 20));

		basePl.lineTo(new Point(-100, 10));
		basePl.lineTo(new Point(-117, 10));
		basePl.lineTo(new Point(-117, 20));

		// Envelope env = new Envelope(new Point(-117,20), 12, 12);//not touched
		Envelope env = new Envelope(-100, 20, -80, 30);// touched

		boolean isTouched;
		try {
			isTouched = GeometryEngine.touches(basePl, env, sr);
		} catch (IllegalArgumentException ex) {
			isTouched = false;
		}
		assertEquals(isTouched, true);

	}

	@Test
	public void testTouchesOnPolygonAndEnvelope() {
		SpatialReference sr = SpatialReference.create(4326);

		Polygon basePl = new Polygon();
		basePl.startPath(new Point(-117, 20));
		basePl.lineTo(new Point(-100, 20));

		basePl.lineTo(new Point(-100, 10));
		basePl.lineTo(new Point(-117, 10));

		// Envelope env = new Envelope(new Point(-117,20), 12, 12);//not touched
		Envelope env = new Envelope(-100, 20, -80, 30);// touched

		boolean isTouched;
		try {
			isTouched = GeometryEngine.touches(basePl, env, sr);
		} catch (IllegalArgumentException ex) {
			isTouched = false;
		}
		assertEquals(isTouched, true);

	}

	@Test
	public void testTouchesOnPointAndEnvelope() {
		SpatialReference sr = SpatialReference.create(4326);

		Point p = new Point(-130, 10);

		// Envelope env = new Envelope(p, 12, 12);//not touched
		Envelope env = new Envelope(-130, 10, -110, 20);// touched

		boolean isTouched;
		try {
			isTouched = GeometryEngine.touches(p, env, sr);
		} catch (IllegalArgumentException ex) {
			isTouched = false;
		}
		assertEquals(isTouched, true);

	}

	@Test
	public void testRelationTouch() {
		SpatialReference sr = SpatialReference.create(4326);
		Polyline basePl = new Polyline();
		basePl.startPath(2, 2);
		basePl.lineTo(2, 10);

		Polyline compPl = new Polyline();
		compPl.startPath(2, 4);
		compPl.lineTo(9, 4);
		compPl.lineTo(9, 9);
		compPl.lineTo(2, 9);
		compPl.lineTo(2, 4);

		boolean isTouched = false;// GeometryEngine.relation(basePl, compPl, sr,
									// "G1 TOUCH G2");
		assertEquals(isTouched, false);

	}

	@Test
	/**
	 * test touches between point and polyline
	 * a point touches a polyline only if the point is 
	 * coincident with one of the polyline end points
	 * */
	public void testTouchesBetweenPointAndLine() {
		SpatialReference sr = SpatialReference.create(4326);
		Point p = new Point(2, 4);

		Polyline compPl = new Polyline();
		compPl.startPath(2, 4);

		compPl.lineTo(9, 4);
		compPl.lineTo(9, 9);
		compPl.lineTo(2, 9);
		compPl.lineTo(2, 4);

		boolean isTouched = GeometryEngine.touches(p, compPl, sr);
		assertTrue(!isTouched);

	}

	@Test
	/**
	 * test touches between polyline and polyline
	 * a polyline touches another polyline only if the end point(s) is 
	 * coincident with the end points of another polyline
	 * In this test case, the end points of the first polyline are concident
	 * with two end points of the second polyline
	 * */
	public void testTouchesBetweenPolylines() {
		SpatialReference sr = SpatialReference.create(4326);
		Polyline pl = new Polyline();
		pl.startPath(2, 4);
		pl.lineTo(9, 9);

		Polyline compPl = new Polyline();
		compPl.startPath(2, 4);

		compPl.lineTo(9, 4);
		compPl.lineTo(9, 9);
		compPl.lineTo(2, 9);
		compPl.lineTo(2, 4);

		boolean isTouched = GeometryEngine.touches(pl, compPl, sr);
		assertEquals(isTouched, true);

	}

	@Test
	/**
	 * test touches between polyline and polygon
	 * a polyline touches polygon only if the end point(s) is 
	 * coincident with the vertices of polygon
	 * In this test case, the end points of the polyline are co-incident
	 * with two vertices of the polygon which consists of two parts
	 * */
	public void testTouchesBetweenPolylineAndPolygon() {
		SpatialReference sr = SpatialReference.create(4326);
		Polyline pl = new Polyline();
		pl.startPath(2, 4);
		pl.lineTo(1, 10);
		pl.lineTo(6, 12);

		Polygon compPg = new Polygon();
		compPg.startPath(2, 4);

		compPg.lineTo(2, 9);
		compPg.lineTo(9, 9);
		compPg.lineTo(9, 4);

		compPg.startPath(2, 9);
		compPg.lineTo(6, 12);
		compPg.lineTo(9, 10);

		boolean isTouched = GeometryEngine.touches(pl, compPg, sr);
		assertEquals(isTouched, true);

	}

	@Test
	/**
	 * test touches between polylines who consists of two parts  
	 * */
	public void testTouchesBetweenMultipartPolylines() {
		SpatialReference sr = SpatialReference.create(4326);
		Polyline pl = new Polyline();
		pl.startPath(2, 4);
		pl.lineTo(1, 10);
		pl.lineTo(6, 12);

		pl.startPath(6, 12);
		pl.lineTo(12, 12);
		pl.lineTo(9, 9);

		Polyline compPl = new Polyline();
		compPl.startPath(2, 4);

		compPl.lineTo(2, 9);
		compPl.lineTo(9, 9);
		compPl.lineTo(9, 4);

		compPl.startPath(2, 9);
		compPl.lineTo(6, 12);
		compPl.lineTo(9, 10);

		boolean isTouched = GeometryEngine.touches(pl, compPl, sr);
		assertTrue(!isTouched);

		// boolean isTouchedFromRest = GeometryUtils.isRelationTrue(compPl, pl,
		// sr,
		// GeometryUtils.SpatialRelationType.esriGeometryRelationTouch, "");
		// assertTrue(isTouchedFromRest == isTouched);
	}

	@Test
	/**
	 * test touches between polygons who consists of two parts  
	 * */
	public void testTouchesBetweenMultipartPolygons2() {
		SpatialReference sr = SpatialReference.create(4326);
		Polygon pl = new Polygon();
		pl.startPath(2, 4);
		pl.lineTo(1, 9);
		pl.lineTo(2, 6);

		pl.startPath(2, 9);
		pl.lineTo(6, 14);
		pl.lineTo(6, 12);

		Polygon compPl = new Polygon();
		compPl.startPath(2, 4);

		compPl.lineTo(2, 9);
		compPl.lineTo(9, 9);
		compPl.lineTo(9, 4);

		compPl.startPath(2, 9);
		compPl.lineTo(6, 12);
		compPl.lineTo(9, 10);

		boolean isTouched = GeometryEngine.touches(pl, compPl, sr);
		assertEquals(isTouched, true);

	}

	@Test
	public void testTouchPointLineCR183227() {
		// Tests CR 183227
		Geometry baseGeom = new Point(-130, 10);
		Polyline pl = new Polyline();
		// pl.startPath(new Point(-130, 10));
		pl.startPath(-130, 10);
		pl.lineTo(-131, 15);
		pl.lineTo(-140, 20);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		isTouched = GeometryEngine.touches(baseGeom, pl, sr);
		isTouched2 = GeometryEngine.touches(pl, baseGeom, sr);
		assertTrue(isTouched && isTouched2);
		{
			Geometry baseGeom2 = (Geometry) new Point(-131, 15);
			boolean bIsTouched;
			boolean bIsTouched2;
			bIsTouched = GeometryEngine.touches(baseGeom2, pl, sr);
			bIsTouched2 = GeometryEngine.touches(pl, baseGeom2, sr);
			assertTrue(!bIsTouched && !bIsTouched2);
		}
	}
}
