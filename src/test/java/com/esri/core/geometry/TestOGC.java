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

import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCGeometryCollection;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCMultiCurve;
import com.esri.core.geometry.ogc.OGCMultiLineString;
import com.esri.core.geometry.ogc.OGCMultiPoint;
import com.esri.core.geometry.ogc.OGCMultiPolygon;
import com.esri.core.geometry.ogc.OGCPoint;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.esri.core.geometry.ogc.OGCConcreteGeometryCollection;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TestOGC extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testPoint() {
		OGCGeometry g = OGCGeometry.fromText("POINT(1 2)");
		assertTrue(g.geometryType().equals("Point"));
		OGCPoint p = (OGCPoint) g;
		assertTrue(p.X() == 1);
		assertTrue(p.Y() == 2);
		assertTrue(g.equals(OGCGeometry.fromText("POINT(1 2)")));
		assertTrue(!g.equals(OGCGeometry.fromText("POINT(1 3)")));
		assertTrue(g.equals((Object)OGCGeometry.fromText("POINT(1 2)")));
		assertTrue(!g.equals((Object)OGCGeometry.fromText("POINT(1 3)")));
		OGCGeometry buf = g.buffer(10);
		assertTrue(buf.geometryType().equals("Polygon"));
		OGCPolygon poly = (OGCPolygon) buf.envelope();
		double a = poly.area();
		assertTrue(Math.abs(a - 400) < 1e-1);
	}

	@Test
	public void testPolygon() throws Exception {
		OGCGeometry g = OGCGeometry
				.fromText("POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))");
		assertTrue(g.geometryType().equals("Polygon"));
		OGCPolygon p = (OGCPolygon) g;
		assertTrue(p.numInteriorRing() == 1);
		OGCLineString ls = p.exteriorRing();
		// assertTrue(ls.pointN(1).equals(OGCGeometry.fromText("POINT(10 -10)")));
		boolean b = ls
				.Equals(OGCGeometry
						.fromText("LINESTRING(-10 -10, 10 -10, 10 10, -10 10, -10 -10)"));
		assertTrue(b);
		OGCLineString lsi = p.interiorRingN(0);
		b = lsi.Equals(OGCGeometry
				.fromText("LINESTRING(-5 -5, -5 5, 5 5, 5 -5, -5 -5)"));
		assertTrue(b);
		b = lsi.equals((Object)OGCGeometry
				.fromText("LINESTRING(-5 -5, -5 5, 5 5, 5 -5, -5 -5)"));
		assertTrue(!lsi.Equals(ls));
		OGCMultiCurve boundary = p.boundary();
		String s = boundary.asText();
		assertTrue(s.equals("MULTILINESTRING ((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))"));

		{
			OGCGeometry g2 = OGCGeometry.fromGeoJson(
					"{\"type\": \"Polygon\", \"coordinates\": [[[1.00000001,1.00000001], [4.00000001,1.00000001], [4.00000001,4.00000001], [1.00000001,4.00000001]]]}");
			OGCGeometry
					.fromGeoJson(
							"{\"type\": \"LineString\", \"coordinates\": [[1.00000001,1.00000001], [7.00000001,8.00000001]]}")
					.intersects(g2);
			OGCGeometry
					.fromGeoJson(
							"{\"type\": \"LineString\", \"coordinates\": [[2.449,4.865], [7.00000001,8.00000001]]}")
					.intersects(g2);

			OGCGeometry g3 = OGCGeometry.fromGeoJson(
					"{\"type\": \"Polygon\", \"coordinates\": [[[1.00000001,1.00000001], [4.00000001,1.00000001], [4.00000001,4.00000001], [1.00000001,4.00000001]]]}");
			boolean bb = g2.equals((Object) g3);
			assertTrue(bb);
		}
	}

	@Test
	public void testGeometryCollection() throws Exception {
		OGCGeometry g = OGCGeometry
				.fromText("GEOMETRYCOLLECTION(POLYGON EMPTY, POINT(1 1), LINESTRING EMPTY, MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY)");
		assertTrue(g.geometryType().equals("GeometryCollection"));
		OGCConcreteGeometryCollection gc = (OGCConcreteGeometryCollection) g;
		assertTrue(gc.numGeometries() == 5);
		assertTrue(gc.geometryN(0).geometryType().equals("Polygon"));
		assertTrue(gc.geometryN(1).geometryType().equals("Point"));
		assertTrue(gc.geometryN(2).geometryType().equals("LineString"));
		assertTrue(gc.geometryN(3).geometryType().equals("MultiPolygon"));
		assertTrue(gc.geometryN(4).geometryType().equals("MultiLineString"));

		g = OGCGeometry
				.fromText("GEOMETRYCOLLECTION(POLYGON EMPTY, POINT(1 1), GEOMETRYCOLLECTION EMPTY, LINESTRING EMPTY, GEOMETRYCOLLECTION(POLYGON EMPTY, POINT(1 1), LINESTRING EMPTY, MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY, MULTIPOINT EMPTY), MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY)");
		assertTrue(g.geometryType().equals("GeometryCollection"));
		gc = (OGCConcreteGeometryCollection) g;
		assertTrue(gc.numGeometries() == 7);
		assertTrue(gc.geometryN(0).geometryType().equals("Polygon"));
		assertTrue(gc.geometryN(1).geometryType().equals("Point"));
		assertTrue(gc.geometryN(2).geometryType().equals("GeometryCollection"));
		assertTrue(gc.geometryN(3).geometryType().equals("LineString"));
		assertTrue(gc.geometryN(4).geometryType().equals("GeometryCollection"));
		assertTrue(gc.geometryN(5).geometryType().equals("MultiPolygon"));
		assertTrue(gc.geometryN(6).geometryType().equals("MultiLineString"));

		OGCConcreteGeometryCollection gc2 = (OGCConcreteGeometryCollection) gc
				.geometryN(4);
		assertTrue(gc2.numGeometries() == 6);
		assertTrue(gc2.geometryN(0).geometryType().equals("Polygon"));
		assertTrue(gc2.geometryN(1).geometryType().equals("Point"));
		assertTrue(gc2.geometryN(2).geometryType().equals("LineString"));
		assertTrue(gc2.geometryN(3).geometryType().equals("MultiPolygon"));
		assertTrue(gc2.geometryN(4).geometryType().equals("MultiLineString"));
		assertTrue(gc2.geometryN(5).geometryType().equals("MultiPoint"));

		ByteBuffer wkbBuffer = g.asBinary();
		g = OGCGeometry.fromBinary(wkbBuffer);

		assertTrue(g.geometryType().equals("GeometryCollection"));
		gc = (OGCConcreteGeometryCollection) g;
		assertTrue(gc.numGeometries() == 7);
		assertTrue(gc.geometryN(0).geometryType().equals("Polygon"));
		assertTrue(gc.geometryN(1).geometryType().equals("Point"));
		assertTrue(gc.geometryN(2).geometryType().equals("GeometryCollection"));
		assertTrue(gc.geometryN(3).geometryType().equals("LineString"));
		assertTrue(gc.geometryN(4).geometryType().equals("GeometryCollection"));
		assertTrue(gc.geometryN(5).geometryType().equals("MultiPolygon"));
		assertTrue(gc.geometryN(6).geometryType().equals("MultiLineString"));

		gc2 = (OGCConcreteGeometryCollection) gc.geometryN(4);
		assertTrue(gc2.numGeometries() == 6);
		assertTrue(gc2.geometryN(0).geometryType().equals("Polygon"));
		assertTrue(gc2.geometryN(1).geometryType().equals("Point"));
		assertTrue(gc2.geometryN(2).geometryType().equals("LineString"));
		assertTrue(gc2.geometryN(3).geometryType().equals("MultiPolygon"));
		assertTrue(gc2.geometryN(4).geometryType().equals("MultiLineString"));
		assertTrue(gc2.geometryN(5).geometryType().equals("MultiPoint"));

		String wktString = g.asText();
		assertTrue(wktString
				.equals("GEOMETRYCOLLECTION (POLYGON EMPTY, POINT (1 1), GEOMETRYCOLLECTION EMPTY, LINESTRING EMPTY, GEOMETRYCOLLECTION (POLYGON EMPTY, POINT (1 1), LINESTRING EMPTY, MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY, MULTIPOINT EMPTY), MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY)"));

		g = OGCGeometry
				.fromGeoJson("{\"type\" : \"GeometryCollection\", \"geometries\" : [{\"type\" : \"Polygon\", \"coordinates\" : []}, {\"type\" : \"Point\", \"coordinates\" : [1, 1]}, {\"type\" : \"GeometryCollection\", \"geometries\" : []}, {\"type\" : \"LineString\", \"coordinates\" : []}, {\"type\" : \"GeometryCollection\", \"geometries\" : [{\"type\": \"Polygon\", \"coordinates\" : []}, {\"type\" : \"Point\", \"coordinates\" : [1,1]}, {\"type\" : \"LineString\", \"coordinates\" : []}, {\"type\" : \"MultiPolygon\", \"coordinates\" : []}, {\"type\" : \"MultiLineString\", \"coordinates\" : []}, {\"type\" : \"MultiPoint\", \"coordinates\" : []}]}, {\"type\" : \"MultiPolygon\", \"coordinates\" : []}, {\"type\" : \"MultiLineString\", \"coordinates\" : []} ] }");

		wktString = g.asText();
		assertTrue(wktString
				.equals("GEOMETRYCOLLECTION (POLYGON EMPTY, POINT (1 1), GEOMETRYCOLLECTION EMPTY, LINESTRING EMPTY, GEOMETRYCOLLECTION (POLYGON EMPTY, POINT (1 1), LINESTRING EMPTY, MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY, MULTIPOINT EMPTY), MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY)"));
		
		assertTrue(g.equals((Object)OGCGeometry.fromText(wktString)));
		
		assertTrue(g.hashCode() == OGCGeometry.fromText(wktString).hashCode());

	}

	@Test
	public void testFirstPointOfPolygon() {
		OGCGeometry g = OGCGeometry
				.fromText("POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))");
		assertTrue(g.geometryType().equals("Polygon"));
		OGCPolygon p = (OGCPolygon) g;
		assertTrue(p.numInteriorRing() == 1);
		OGCLineString ls = p.exteriorRing();
		OGCPoint p1 = ls.pointN(1);
		assertTrue(ls.pointN(1).equals(OGCGeometry.fromText("POINT(10 -10)")));
		OGCPoint p2 = ls.pointN(3);
		assertTrue(ls.pointN(3).equals(OGCGeometry.fromText("POINT(-10 10)")));
		OGCPoint p0 = ls.pointN(0);
		assertTrue(ls.pointN(0).equals(OGCGeometry.fromText("POINT(-10 -10)")));
		String ms = g.convertToMulti().asText();
		assertTrue(ms.equals("MULTIPOLYGON (((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5)))"));

	}

	@Test
	public void testFirstPointOfLineString() {
		OGCGeometry g = OGCGeometry
				.fromText("LINESTRING(-10 -10, 10 -10, 10 10, -10 10, -10 -10)");
		assertTrue(g.geometryType().equals("LineString"));
		OGCLineString p = (OGCLineString) g;
		assertTrue(p.numPoints() == 5);
		assertTrue(p.isClosed());
		assertTrue(p.pointN(1).equals(OGCGeometry.fromText("POINT(10 -10)")));
		String ms = g.convertToMulti().asText();
		assertTrue(ms.equals("MULTILINESTRING ((-10 -10, 10 -10, 10 10, -10 10, -10 -10))"));
	}

	@Test
	public void testPointInPolygon() {
		OGCGeometry g = OGCGeometry
				.fromText("POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))");
		assertTrue(g.geometryType().equals("Polygon"));
		assertTrue(!g.contains(OGCGeometry.fromText("POINT(0 0)")));
		assertTrue(g.contains(OGCGeometry.fromText("POINT(9 9)")));
		assertTrue(!g.contains(OGCGeometry.fromText("POINT(-20 1)")));
		assertTrue(g.disjoint(OGCGeometry.fromText("POINT(0 0)")));
		assertTrue(!g.disjoint(OGCGeometry.fromText("POINT(9 9)")));
		assertTrue(g.disjoint(OGCGeometry.fromText("POINT(-20 1)")));
	}

	@Test
	public void testMultiPolygon() {
		{
			OGCGeometry g = OGCGeometry
					.fromText("MULTIPOLYGON(((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5)))");
			assertTrue(g.geometryType().equals("MultiPolygon")); // the type is
																	// reduced
			assertTrue(!g.contains(OGCGeometry.fromText("POINT(0 0)")));
			assertTrue(g.contains(OGCGeometry.fromText("POINT(9 9)")));
			assertTrue(!g.contains(OGCGeometry.fromText("POINT(-20 1)")));
			assertTrue(g.disjoint(OGCGeometry.fromText("POINT(0 0)")));
			assertTrue(!g.disjoint(OGCGeometry.fromText("POINT(9 9)")));
			assertTrue(g.disjoint(OGCGeometry.fromText("POINT(-20 1)")));
			assertTrue(g.convertToMulti() == g);
		}
		
		{
			OGCGeometry g = OGCGeometry
					.fromText("MULTIPOLYGON(((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5)), ((90 90, 110 90, 110 110, 90 110, 90 90), (95 95, 95 105, 105 105, 105 95, 95 95)))");
			assertTrue(g.geometryType().equals("MultiPolygon")); // the type is
			
			OGCMultiPolygon mp = (OGCMultiPolygon)g;
			assertTrue(mp.numGeometries() == 2);
			OGCGeometry p1 = mp.geometryN(0);
			assertTrue(p1.geometryType().equals("Polygon")); // the type is
			assertTrue(p1.contains(OGCGeometry.fromText("POINT(9 9)")));
			assertTrue(!p1.contains(OGCGeometry.fromText("POINT(109 109)")));
			OGCGeometry p2 = mp.geometryN(1);
			assertTrue(p2.geometryType().equals("Polygon")); // the type is
			assertTrue(!p2.contains(OGCGeometry.fromText("POINT(9 9)")));
			assertTrue(p2.contains(OGCGeometry.fromText("POINT(109 109)")));
		}
	}

	@Test
	public void testMultiPolygonUnion() {
		OGCGeometry g = OGCGeometry
				.fromText("POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))");
		OGCGeometry g2 = OGCGeometry
				.fromText("POLYGON((90 90, 110 90, 110 110, 90 110, 90 90))");
		OGCGeometry u = g.union(g2);
		assertTrue(u.geometryType().equals("MultiPolygon"));
		assertTrue(!u.contains(OGCGeometry.fromText("POINT(0 0)")));
		assertTrue(u.contains(OGCGeometry.fromText("POINT(9 9)")));
		assertTrue(!u.contains(OGCGeometry.fromText("POINT(-20 1)")));
		assertTrue(u.disjoint(OGCGeometry.fromText("POINT(0 0)")));
		assertTrue(!u.disjoint(OGCGeometry.fromText("POINT(9 9)")));
		assertTrue(u.disjoint(OGCGeometry.fromText("POINT(-20 1)")));
		assertTrue(u.contains(OGCGeometry.fromText("POINT(100 100)")));
	}

	@Test
	public void testIntersection() {
		OGCGeometry g = OGCGeometry.fromText("LINESTRING(0 0, 10 10)");
		OGCGeometry g2 = OGCGeometry.fromText("LINESTRING(10 0, 0 10)");
		OGCGeometry u = g.intersection(g2);
		assertTrue(u.dimension() == 0);
		String s = u.asText();
		assertTrue(u.equals(OGCGeometry.fromText("POINT(5 5)")));
	}

	@Test
	public void testPointSymDif() {
		OGCGeometry g1 = OGCGeometry.fromText("POINT(1 2)");
		OGCGeometry g2 = OGCGeometry.fromText("POINT(3 4)");
		OGCGeometry gg = g1.symDifference(g2);
		assertTrue(gg.equals(OGCGeometry.fromText("MULTIPOINT(1 2, 3 4)")));

		OGCGeometry g3 = OGCGeometry.fromText("POINT(1 2)");
		OGCGeometry gg1 = g1.symDifference(g3);
		assertTrue(gg1 == null || gg1.isEmpty());

	}

	@Test
	public void testNullSr() {
		String wkt = "point (0 0)";
		OGCGeometry g = OGCGeometry.fromText(wkt);
		g.setSpatialReference(null);
		assertTrue(g.SRID() < 1);
	}

	@Test
	public void testIsectPoint() {
		String wkt = "point (0 0)";
		String wk2 = "point (0 0)";
		OGCGeometry g0 = OGCGeometry.fromText(wkt);
		OGCGeometry g1 = OGCGeometry.fromText(wk2);
		g0.setSpatialReference(null);
		g1.setSpatialReference(null);
		try {
			OGCGeometry rslt = g0.intersection(g1); // ArrayIndexOutOfBoundsException
			assertTrue(rslt != null);
		} catch (Exception e) {
			assertTrue(false);
		}
	}

	@Test
	public void testIsectDisjoint() {
		String wk3 = "linestring (0 0, 1 1)";
		String wk4 = "linestring (2 2, 4 4)";
		OGCGeometry g0 = OGCGeometry.fromText(wk3);
		OGCGeometry g1 = OGCGeometry.fromText(wk4);
		g0.setSpatialReference(null);
		g1.setSpatialReference(null);
		try {
			OGCGeometry rslt = g0.intersection(g1); // null
			assertTrue(rslt != null);
		} catch (Exception e) {
			assertTrue(false);
		}
	}

	@Test
	public void test_polygon_is_simple_for_OGC() {
		try {
			{
				String s = "{\"rings\":[[[0, 0], [0, 10], [10, 10], [10, 0], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}

			{// exterior ring is self-tangent
				String s = "{\"rings\":[[[0, 0], [0, 10], [5, 5], [10, 10], [10, 0], [5, 5], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{// ring orientation (hole is cw)
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [5, 5], [10, 0], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(!g.isSimpleRelaxed());
			}
			{
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [10, 0], [5, 5], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}

			{// ring order
				String s = "{\"rings\":[[[0, 0], [10, 0], [5, 5], [0, 0]], [[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				// hole is self tangent
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [5, 5], [10, 0], [10, 10], [5, 5], [0, 10], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}
			{
				// two holes touch
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [10, 0], [5, 5], [0, 0]], [[10, 10], [0, 10], [5, 5], [10, 10]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}
			{
				// two holes touch, bad orientation
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [5, 5], [10, 0], [0, 0]], [[10, 10], [0, 10], [5, 5], [10, 10]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(!g.isSimpleRelaxed());

			}

			{
				// hole touches exterior in two spots
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [-100, -100]], [[0, -100], [10, 0], [0, 100], [-10, 0], [0, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				// hole touches exterior in one spot
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [-100, -100]], [[0, -100], [10, 0], [0, 90], [-10, 0], [0, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());

			}

			{
				// exterior has inversion (planar simple)
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [10, 0], [0, 90], [-10, 0], [0, -100], [-100, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				// two holes touch in one spot, and they also touch exterior in
				// two spots, producing disconnected interior
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [-100, -100]], [[0, -100], [10, -50], [0, 0], [-10, -50], [0, -100]], [[0, 0], [10, 50], [0, 100], [-10, 50], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}
		} catch (Exception ex) {
			assertTrue(false);
		}
	}

	@Test
	public void test_polygon_simplify_for_OGC() {
		try {
			{
				//degenerate
				String s = "{\"rings\":[[[0, 0], [0, 10], [10, 10], [10, 0], [20, 0], [10, 0], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				String res_str = og.asText();
				assertTrue(og.isSimple());
			}			
			{
				String s = "{\"rings\":[[[0, 0], [0, 10], [10, 10], [10, 0], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				String res_str = og.asText();
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 0);
				assertTrue(og.isSimple());
			}

			{// exterior ring is self-tangent
				String s = "{\"rings\":[[[0, 0], [0, 10], [5, 5], [10, 10], [10, 0], [5, 5], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				res = og.isSimple();
				assertTrue(res);
				assertTrue(og.geometryType().equals("MultiPolygon"));
				assertTrue(((OGCGeometryCollection)og).numGeometries() == 2);
			}

			{// ring orientation (hole is cw)
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [5, 5], [10, 0], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(!g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				res = og.isSimple();
				assertTrue(res);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 1);
			}

			{// ring order
				String s = "{\"rings\":[[[0, 0], [10, 0], [5, 5], [0, 0]], [[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				res = og.isSimple();
				assertTrue(res);
				assertTrue(og.geometryType().equals("Polygon"));
			}

			{
				// hole is self tangent
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [5, 5], [10, 0], [10, 10], [5, 5], [0, 10], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				String res_str = og.asText();
				res = og.isSimple();
				assertTrue(res);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 2);
			}
			{
				// two holes touch
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [10, 0], [5, 5], [0, 0]], [[10, 10], [0, 10], [5, 5], [10, 10]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 2);
			}
			{
				// two holes touch, bad orientation
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[0, 0], [5, 5], [10, 0], [0, 0]], [[10, 10], [0, 10], [5, 5], [10, 10]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(!g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 2);
			}

			{
				// hole touches exterior in two spots
				//OperatorSimplifyOGC produces a multipolygon with two polygons without holes.				
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [-100, -100]], [[0, -100], [10, 0], [0, 100], [-10, 0], [0, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("MultiPolygon"));
				assertTrue(((OGCMultiPolygon)og).numGeometries() == 2);
				assertTrue(((OGCPolygon)((OGCMultiPolygon)og).geometryN(0)).numInteriorRing() == 0);
				assertTrue(((OGCPolygon)((OGCMultiPolygon)og).geometryN(1)).numInteriorRing() == 0);
			}

			{
				// hole touches exterior in one spot
				//OperatorSimplifyOGC produces a polygons with a hole.				
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [-100, -100]], [[0, -100], [10, 0], [0, 90], [-10, 0], [0, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 1);
			}

			{
				// exterior has inversion (non simple for OGC)
				//OperatorSimplifyOGC produces a polygons with a hole.				
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [10, 0], [0, 90], [-10, 0], [0, -100], [-100, -100]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 1);
			}

			{
				// two holes touch in one spot, and they also touch exterior in
				// two spots, producing disconnected interior
				//OperatorSimplifyOGC produces two polygons with no holes.
				String s = "{\"rings\":[[[-100, -100], [-100, 100], [0, 100], [100, 100], [100, -100], [0, -100], [-100, -100]], [[0, -100], [10, -50], [0, 0], [-10, -50], [0, -100]], [[0, 0], [10, 50], [0, 100], [-10, 50], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("MultiPolygon"));
				assertTrue(((OGCMultiPolygon)og).numGeometries() == 2);
				assertTrue(((OGCPolygon)((OGCMultiPolygon)og).geometryN(0)).numInteriorRing() == 0);
				assertTrue(((OGCPolygon)((OGCMultiPolygon)og).geometryN(1)).numInteriorRing() == 0);
			}
			
			
			{
				OGCGeometry g = OGCGeometry.fromJson("{\"rings\":[[[-3,4],[6,4],[6,-3],[-3,-3],[-3,4]],[[0,2],[2,2],[0,0],[4,0],[4,2],[2,0],[2,2],[4,2],[3,3],[2,2],[1,3],[0,2]]], \"spatialReference\":{\"wkid\":4326}}");
				assertTrue(g.geometryType().equals("Polygon"));
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(!g.isSimpleRelaxed());
				OGCGeometry simpleG = g.makeSimple();
				assertTrue(simpleG.geometryType().equals("MultiPolygon"));
				assertTrue(simpleG.isSimple());
				OGCMultiPolygon mp = (OGCMultiPolygon)simpleG;
				assertTrue(mp.numGeometries() == 2);
				OGCPolygon g1 = (OGCPolygon)mp.geometryN(0);
				OGCPolygon g2 = (OGCPolygon)mp.geometryN(1);
				assertTrue((g1.numInteriorRing() == 0 && g1.numInteriorRing() == 2) ||
						(g1.numInteriorRing() == 2 && g2.numInteriorRing() == 0));
				
				OGCGeometry oldOutput = OGCGeometry.fromJson("{\"rings\":[[[-3,-3],[-3,4],[6,4],[6,-3],[-3,-3]],[[0,0],[2,0],[4,0],[4,2],[3,3],[2,2],[1,3],[0,2],[2,2],[0,0]],[[2,0],[2,2],[4,2],[2,0]]],\"spatialReference\":{\"wkid\":4326}}");
				assertTrue(oldOutput.isSimpleRelaxed());
				assertFalse(oldOutput.isSimple());
			}
		} catch (Exception ex) {
			assertTrue(false);
		}
	}
	
	@Test
	public void test_polyline_is_simple_for_OGC() {
		try {
			{
				String s = "{\"paths\":[[[0, 10], [8, 5], [5, 2], [6, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}
			{
				String s = "{\"paths\":[[[0, 10], [6,  0], [7, 5], [0, 3]]]}";// self
																				// intersection
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[0, 10], [6,  0], [0, 3], [0, 10]]]}"; // closed
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[0, 10], [5, 5], [6,  0], [0, 3], [5, 5], [0, 9], [0, 10]]]}"; // closed
																										// with
																										// self
																										// tangent
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[0, 10], [5, 2]], [[5, 2], [6,  0]]]}";// two
																				// paths
																				// connected
																				// at
																				// a
																				// point
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[0, 0], [3, 3], [5, 0], [0, 0]], [[0, 10], [3, 3], [10, 10], [0, 10]]]}";// two
																													// closed
																													// rings
																													// touch
																													// at
																													// one
																													// point
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[3, 3], [0, 0], [5, 0], [3, 3]], [[3, 3], [0, 10], [10, 10], [3, 3]]]}";
				// two closed rings touch at one point. The touch happens at the
				// endpoints of the paths.
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[0, 0], [10, 10]], [[0, 10], [10, 0]]]}";// two
																					// lines
																					// intersect
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}

			{
				String s = "{\"paths\":[[[0, 0], [5, 5], [0, 10]], [[10, 10], [5, 5], [10, 0]]]}";// two
																									// paths
																									// share
																									// mid
																									// point.
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}
		} catch (Exception ex) {
			assertTrue(false);
		}

	}

	@Test
	public void test_multipoint_is_simple_for_OGC() {
		try {

			SpatialReference sr = SpatialReference.create(4326);
			{
				String s = "{\"points\":[[0, 0], [5, 5], [0, 10]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
			}
			{
				String s = "{\"points\":[[0, 0], [5, 5], [0, 0], [0, 10]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(!g.isSimpleRelaxed());
			}
			{
				String s = "{\"points\":[[0, 0], [5, 5], [1e-10, -1e-10], [0, 10]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				g.setSpatialReference(sr);
				boolean res = g.isSimple();
				assertTrue(!res);
				assertTrue(g.isSimpleRelaxed());
			}
		} catch (Exception ex) {
			assertTrue(false);
		}

	}

	@Test
	public void testGeometryCollectionBuffer() {
		OGCGeometry g = OGCGeometry
				.fromText("GEOMETRYCOLLECTION(POINT(1 1), POINT(1 1), POINT(1 2), LINESTRING (0 0, 1 1, 1 0, 0 1), MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY)");
		OGCGeometry simpleG = g.buffer(0);
		String t = simpleG.geometryType();
		String rt = simpleG.asText();
		assertTrue(simpleG.geometryType().equals("GeometryCollection"));
	}

	@Test
	public void testIsectTria1() {
		String wkt = "polygon((1 0, 3 0, 1 2, 1 0))";
		String wk2 = "polygon((0 1, 2 1, 0 3, 0 1))";
		OGCGeometry g0 = OGCGeometry.fromText(wkt);
		OGCGeometry g1 = OGCGeometry.fromText(wk2);
		g0.setSpatialReference(SpatialReference.create(4326));
		g1.setSpatialReference(SpatialReference.create(4326));
		OGCGeometry rslt = g0.intersection(g1);
		assertTrue(rslt != null);
		assertTrue(rslt.geometryType().equals("Polygon"));
		assertTrue(rslt.esriSR.getID() == 4326);
		String s = rslt.asText();
	}

	@Test
	public void testIsectTriaJson1() throws Exception {
		String json1 = "{\"rings\":[[[1, 0], [3, 0], [1, 2], [1, 0]]], \"spatialReference\":{\"wkid\":4326}}";
		String json2 = "{\"rings\":[[[0, 1], [2, 1], [0, 3], [0, 1]]], \"spatialReference\":{\"wkid\":4326}}";
		OGCGeometry g0 = OGCGeometry.fromJson(json1);
		OGCGeometry g1 = OGCGeometry.fromJson(json2);
		OGCGeometry rslt = g0.intersection(g1);
		assertTrue(rslt != null);
		assertTrue(rslt.geometryType().equals("Polygon"));
		assertTrue(rslt.esriSR.getID() == 4326);
		String s = GeometryEngine.geometryToJson(rslt.getEsriSpatialReference().getID(), rslt.getEsriGeometry());
	}
	
	@Test
	public void testIsectTria2() {
		String wkt = "polygon((1 0, 3 0, 1 2, 1 0))";
		String wk2 = "polygon((0 3, 2 1, 3 1, 0 3))";
		OGCGeometry g0 = OGCGeometry.fromText(wkt);
		OGCGeometry g1 = OGCGeometry.fromText(wk2);
		g0.setSpatialReference(null);
		g1.setSpatialReference(null);
		OGCGeometry rslt = g0.intersection(g1);
		assertTrue(rslt != null);
		assertTrue(rslt.dimension() == 1);
		assertTrue(rslt.geometryType().equals("LineString"));
		String s = rslt.asText();
	}

	@Test
	public void testIsectTria3() {
		String wkt = "polygon((1 0, 3 0, 1 2, 1 0))";
		String wk2 = "polygon((2 2, 2 1, 3 1, 2 2))";
		OGCGeometry g0 = OGCGeometry.fromText(wkt);
		OGCGeometry g1 = OGCGeometry.fromText(wk2);
		g0.setSpatialReference(SpatialReference.create(4326));
		g1.setSpatialReference(SpatialReference.create(4326));
		OGCGeometry rslt = g0.intersection(g1);
		assertTrue(rslt != null);
		assertTrue(rslt.dimension() == 0);
		assertTrue(rslt.geometryType().equals("Point"));
		assertTrue(rslt.esriSR.getID() == 4326);
		String s = rslt.asText();
	}

	@Test
	public void testMultiPointSinglePoint() {
		String wkt = "multipoint((1 0))";
		OGCGeometry g0 = OGCGeometry.fromText(wkt);
		assertTrue(g0.dimension() == 0);
		String gt = g0.geometryType();
		assertTrue(gt.equals("MultiPoint"));
		OGCMultiPoint mp = (OGCMultiPoint)g0;
		assertTrue(mp.numGeometries() == 1);
		OGCGeometry p = mp.geometryN(0);
		String s = p.asText();
		assertTrue(s.equals("POINT (1 0)"));
		
		String ms = p.convertToMulti().asText();
		assertTrue(ms.equals("MULTIPOINT ((1 0))"));
		
	}
	
	@Test
	public void testWktMultiPolygon() {
		String restJson = "{\"rings\": [[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[-90, -90], [90, 90], [-90, 90], [90, -90], [-90, -90]],	[[-10, -10], [-10, 10], [10, 10], [10, -10], [-10, -10]]]}";
		MapGeometry g = null;
		g = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, restJson);
		String wkt = OperatorExportToWkt.local().execute(0, g.getGeometry(), null);
		assertTrue(wkt.equals("MULTIPOLYGON (((-100 -100, 100 -100, 100 100, -100 100, -100 -100), (-90 -90, 90 -90, -90 90, 90 90, -90 -90)), ((-10 -10, 10 -10, 10 10, -10 10, -10 -10)))"));
	}

	@Test
	public void testMultiPolygonArea() {
		//MultiPolygon Area #36 
		String wkt = "MULTIPOLYGON (((1001200 2432900, 1001420 2432691, 1001250 2432388, 1001498 2432325, 1001100 2432100, 1001500 2431900, 1002044 2431764, 1002059 2432120, 1002182 2432003, 1002400 2432300, 1002650 2432150, 1002610 2432323, 1002772 2432434, 1002410 2432821, 1002700 2433000, 1001824 2432866, 1001600 2433150, 1001200 2432900)), ((1000393 2433983, 1000914 2434018, 1000933 2433817, 1000568 2433834, 1000580 2433584, 1000700 2433750, 1000800 2433650, 1000700 2433450, 1000600 2433550, 1000200 2433350, 1000100 2433900, 1000393 2433983)), ((1001200 2432900, 1000878 2432891, 1000900 2433300, 1001659 2433509, 1001600 2433150, 1001200 2432900)), ((1002450 2431650, 1002300 2431650, 1002300 2431900, 1002500 2432100, 1002600 2431800, 1002450 2431800, 1002450 2431650)), ((999750 2433550, 999850 2433600, 999900 2433350, 999780 2433433, 999750 2433550)), ((1002950 2432050, 1003005 2431932, 1002850 2432250, 1002928 2432210, 1002950 2432050)), ((1002600 2431750, 1002642 2431882, 1002750 2431900, 1002750 2431750, 1002600 2431750)), ((1002950 2431750, 1003050 2431650, 1002968 2431609, 1002950 2431750)))";
		{
			OGCGeometry ogcg = OGCGeometry.fromText(wkt);
			assertTrue(ogcg.geometryType().equals("MultiPolygon"));
			OGCMultiPolygon mp = (OGCMultiPolygon)ogcg;
			double a = mp.area();
			assertTrue(Math.abs(mp.area() - 2037634.5) < a*1e-14);
		}

		{
			OGCGeometry ogcg = OGCGeometry.fromText(wkt);
			assertTrue(ogcg.geometryType().equals("MultiPolygon"));
			Geometry g = ogcg.getEsriGeometry();
			double a = g.calculateArea2D();
			assertTrue(Math.abs(a - 2037634.5) < a*1e-14);
		}
	}
	
	@Test
	public void testPolylineSimplifyIssueGithub52() throws Exception {
		String json = "{\"paths\":[[[2,0],[4,3],[5,1],[3.25,1.875],[1,3]]],\"spatialReference\":{\"wkid\":4326}}";
		{
			OGCGeometry g = OGCGeometry.fromJson(json);
			assertTrue(g.geometryType().equals("LineString"));
			OGCGeometry simpleG = g.makeSimple();//make ogc simple
			assertTrue(simpleG.geometryType().equals("MultiLineString"));			
			assertTrue(simpleG.isSimpleRelaxed());//geodatabase simple
			assertTrue(simpleG.isSimple());//ogc simple
			OGCMultiLineString mls =(OGCMultiLineString)simpleG;
			assertTrue(mls.numGeometries() == 4);
			OGCGeometry baseGeom = OGCGeometry.fromJson("{\"paths\":[[[2,0],[3.25,1.875]],[[3.25,1.875],[4,3],[5,1]],[[5,1],[3.25,1.875]],[[3.25,1.875],[1,3]]],\"spatialReference\":{\"wkid\":4326}}");
			assertTrue(simpleG.equals(baseGeom));
			
		}
	}
	
	@Test
	public void testEmptyBoundary() throws Exception {
		{
			OGCGeometry g = OGCGeometry.fromText("POINT EMPTY");
			OGCGeometry b = g.boundary();
			assertTrue(b.asText().compareTo("MULTIPOINT EMPTY") == 0);
		}
		{
			OGCGeometry g = OGCGeometry.fromText("MULTIPOINT EMPTY");
			OGCGeometry b = g.boundary();
			assertTrue(b.asText().compareTo("MULTIPOINT EMPTY") == 0);
		}
		{
			OGCGeometry g = OGCGeometry.fromText("LINESTRING EMPTY");
			OGCGeometry b = g.boundary();
			assertTrue(b.asText().compareTo("MULTIPOINT EMPTY") == 0);
		}
		{
			OGCGeometry g = OGCGeometry.fromText("POLYGON EMPTY");
			OGCGeometry b = g.boundary();
			assertTrue(b.asText().compareTo("MULTILINESTRING EMPTY") == 0);
		}
		{
			OGCGeometry g = OGCGeometry.fromText("MULTIPOLYGON EMPTY");
			OGCGeometry b = g.boundary();
			assertTrue(b.asText().compareTo("MULTILINESTRING EMPTY") == 0);
		}
	}
	
	@Test
	public void testUnionPointWithEmptyLineString() {
		assertUnion("POINT (1 2)", "LINESTRING EMPTY", "POINT (1 2)");
	}

	@Test
	public void testUnionPointWithLinestring() {
		assertUnion("POINT (1 2)", "LINESTRING (3 4, 5 6)", "GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (3 4, 5 6))");
	}

	@Test
	public void testUnionLinestringWithEmptyPolygon() {
		assertUnion("MULTILINESTRING ((1 2, 3 4))", "POLYGON EMPTY", "LINESTRING (1 2, 3 4)");
	}

	@Test
	public void testUnionLinestringWithPolygon() {
		assertUnion("LINESTRING (1 2, 3 4)", "POLYGON ((0 0, 1 1, 0 1, 0 0))",
				"GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4), POLYGON ((0 0, 1 1, 0 1, 0 0)))");
	}
	
	@Test
	public void testUnionGeometryCollectionWithGeometryCollection() {
		assertUnion("GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4), POLYGON ((0 0, 1 1, 0 1, 0 0)))", 
				"GEOMETRYCOLLECTION (POINT (1 2), POINT (2 3), POINT (0.5 0.5), POINT (3 5), LINESTRING (3 4, 5 6), POLYGON ((0 0, 1 0, 1 1, 0 0)))",
				"GEOMETRYCOLLECTION (POINT (3 5), LINESTRING (1 2, 2 3, 3 4, 5 6), POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0)))");
	}

	@Test
	public void testIntersectionGeometryCollectionWithGeometryCollection() {
		assertIntersection("GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4), POLYGON ((0 0, 1 1, 0 1, 0 0)))", 
				"GEOMETRYCOLLECTION (POINT (1 2), POINT (2 3), POINT (0.5 0.5), POINT (3 5), LINESTRING (3 4, 5 6), POLYGON ((0 0, 1 0, 1 1, 0 0)))",
				"GEOMETRYCOLLECTION (MULTIPOINT ((1 2), (2 3), (3 4)), LINESTRING (0 0, 0.5 0.5, 1 1))");
	}

	private void assertIntersection(String leftWkt, String rightWkt, String expectedWkt) {
		OGCGeometry intersection = OGCGeometry.fromText(leftWkt).intersection(OGCGeometry.fromText(rightWkt));
		assertEquals(expectedWkt, intersection.asText());
	}
	
	private void assertUnion(String leftWkt, String rightWkt, String expectedWkt) {
		OGCGeometry union = OGCGeometry.fromText(leftWkt).union(OGCGeometry.fromText(rightWkt));
		assertEquals(expectedWkt, union.asText());
	}
	
	@Test
	public void testDisjointOnGeometryCollection() {
		OGCGeometry ogcGeometry = OGCGeometry.fromText("GEOMETRYCOLLECTION (POINT (1 1))");
		assertFalse(ogcGeometry.disjoint(OGCGeometry.fromText("POINT (1 1)")));
	}

	@Test
	public void testContainsOnGeometryCollection() {
		OGCGeometry ogcGeometry = OGCGeometry.fromText("GEOMETRYCOLLECTION (POINT (1 1))");
		assertTrue(ogcGeometry.contains(OGCGeometry.fromText("POINT (1 1)")));
	}

	@Test
	public void testIntersectsOnGeometryCollection() {
		OGCGeometry ogcGeometry = OGCGeometry.fromText("GEOMETRYCOLLECTION (POINT (1 1))");
		assertTrue(ogcGeometry.intersects(OGCGeometry.fromText("POINT (1 1)")));
		ogcGeometry = OGCGeometry.fromText("POINT (1 1)");
		assertTrue(ogcGeometry.intersects(OGCGeometry.fromText("GEOMETRYCOLLECTION (POINT (1 1))")));
	}

	@Test
	public void testDistanceOnGeometryCollection() {
		OGCGeometry ogcGeometry = OGCGeometry.fromText("GEOMETRYCOLLECTION (POINT (1 1))");
		assertTrue(ogcGeometry.distance(OGCGeometry.fromText("POINT (1 1)")) == 0);
		
		//distance to empty is NAN
		ogcGeometry = OGCGeometry.fromText("GEOMETRYCOLLECTION (POINT (1 1))");
		assertTrue(Double.isNaN(ogcGeometry.distance(OGCGeometry.fromText("POINT EMPTY"))));
	}
	
	@Test
	public void testFlattened() {
		OGCConcreteGeometryCollection ogcGeometry = (OGCConcreteGeometryCollection)OGCGeometry.fromText("GEOMETRYCOLLECTION (MULTILINESTRING ((1 2, 3 4)), MULTIPOLYGON (((1 2, 3 4, 5 6, 1 2))), MULTIPOINT (1 1))");
		assertFalse(ogcGeometry.isFlattened());
		ogcGeometry = (OGCConcreteGeometryCollection)OGCGeometry.fromText("GEOMETRYCOLLECTION (MULTIPOINT (1 1), MULTILINESTRING ((1 2, 3 4)), MULTIPOLYGON (((1 2, 3 4, 5 6, 1 2))))");
		assertTrue(ogcGeometry.isFlattened());
	}
	
	@Test
	public void testIssue247IsSimple() {
		//https://github.com/Esri/geometry-api-java/issues/247
		String wkt = "MULTILINESTRING ((-103.4894322 25.6164519, -103.4889647 25.6159054, -103.489434 25.615654), (-103.489434 25.615654, -103.4894322 25.6164519), (-103.4897361 25.6168342, -103.4894322 25.6164519))";
		OGCGeometry ogcGeom = OGCGeometry.fromText(wkt);
		boolean b = ogcGeom.isSimple();
		assertTrue(b);		
	}
	
	@Test
	public void testOGCUnionLinePoint() {
		OGCGeometry point = OGCGeometry.fromText("POINT (-44.16176186699087 -19.943264803833348)");
		OGCGeometry lineString = OGCGeometry.fromText(
				"LINESTRING (-44.1247493 -19.9467657, -44.1247979 -19.9468385, -44.1249043 -19.946934, -44.1251096 -19.9470651, -44.1252609 -19.9471383, -44.1254992 -19.947204, -44.1257652 -19.947229, -44.1261292 -19.9471833, -44.1268946 -19.9470098, -44.1276847 -19.9468416, -44.127831 -19.9468143, -44.1282639 -19.9467366, -44.1284569 -19.9467237, -44.1287119 -19.9467261, -44.1289437 -19.9467665, -44.1291499 -19.9468221, -44.1293856 -19.9469396, -44.1298857 -19.9471497, -44.1300908 -19.9472071, -44.1302743 -19.9472331, -44.1305029 -19.9472364, -44.1306498 -19.9472275, -44.1308054 -19.947216, -44.1308553 -19.9472037, -44.1313206 -19.9471394, -44.1317889 -19.9470854, -44.1330422 -19.9468887, -44.1337465 -19.9467083, -44.1339922 -19.9466842, -44.1341506 -19.9466997, -44.1343621 -19.9467226, -44.1345134 -19.9467855, -44.1346494 -19.9468456, -44.1347295 -19.946881, -44.1347988 -19.9469299, -44.1350231 -19.9471131, -44.1355843 -19.9478307, -44.1357802 -19.9480557, -44.1366289 -19.949198, -44.1370384 -19.9497001, -44.137386 -19.9501921, -44.1374113 -19.9502263, -44.1380888 -19.9510925, -44.1381769 -19.9513526, -44.1382509 -19.9516202, -44.1383014 -19.9522136, -44.1383889 -19.9530931, -44.1384227 -19.9538784, -44.1384512 -19.9539653, -44.1384555 -19.9539807, -44.1384901 -19.9541928, -44.1385563 -19.9543859, -44.1386656 -19.9545781, -44.1387339 -19.9546889, -44.1389219 -19.9548661, -44.1391695 -19.9550384, -44.1393672 -19.9551414, -44.1397538 -19.9552208, -44.1401714 -19.9552332, -44.1405656 -19.9551143, -44.1406198 -19.9550853, -44.1407579 -19.9550224, -44.1409029 -19.9549201, -44.1410283 -19.9548257, -44.1413902 -19.9544132, -44.141835 -19.9539274, -44.142268 -19.953484, -44.1427036 -19.9531023, -44.1436229 -19.952259, -44.1437568 -19.9521565, -44.1441783 -19.9517273, -44.144644 -19.9512109, -44.1452538 -19.9505663, -44.1453541 -19.9504774, -44.1458653 -19.9500442, -44.1463563 -19.9496473, -44.1467534 -19.9492812, -44.1470553 -19.9490028, -44.1475804 -19.9485293, -44.1479838 -19.9482096, -44.1485003 -19.9478532, -44.1489451 -19.9477314, -44.1492225 -19.9477024, -44.149453 -19.9476684, -44.149694 -19.9476387, -44.1499556 -19.9475436, -44.1501398 -19.9474234, -44.1502723 -19.9473206, -44.150421 -19.9471473, -44.1505043 -19.9470004, -44.1507664 -19.9462594, -44.150867 -19.9459518, -44.1509225 -19.9457843, -44.1511168 -19.945466, -44.1513601 -19.9452272, -44.1516846 -19.944999, -44.15197 -19.9448738, -44.1525994 -19.9447263, -44.1536614 -19.9444791, -44.1544071 -19.9442671, -44.1548978 -19.9441275, -44.1556247 -19.9438304, -44.1565996 -19.9434083, -44.1570351 -19.9432556, -44.1573142 -19.9432091, -44.1575332 -19.9431645, -44.157931 -19.9431484, -44.1586408 -19.9431504, -44.1593575 -19.9431457, -44.1596498 -19.9431562, -44.1600991 -19.9431475, -44.1602331 -19.9431567, -44.1607926 -19.9432449, -44.1609723 -19.9432499, -44.1623815 -19.9432765, -44.1628299 -19.9433645, -44.1632475 -19.9435839, -44.1633456 -19.9436559, -44.1636261 -19.9439375, -44.1638186 -19.9442439, -44.1642535 -19.9451781, -44.165178 -19.947156, -44.1652928 -19.9474016, -44.1653074 -19.9474329, -44.1654026 -19.947766, -44.1654774 -19.9481718, -44.1655699 -19.9490241, -44.1656196 -19.9491538, -44.1659735 -19.9499097, -44.1662485 -19.9504925, -44.1662996 -19.9506347, -44.1663574 -19.9512961, -44.1664094 -19.9519273, -44.1664144 -19.9519881, -44.1664799 -19.9526399, -44.1666965 -19.9532586, -44.1671191 -19.9544126, -44.1672019 -19.9545869, -44.1673344 -19.9547603, -44.1675958 -19.9550466, -44.1692349 -19.9567775, -44.1694607 -19.9569284, -44.1718843 -19.9574147, -44.1719167 -19.9574206, -44.1721627 -19.9574748, -44.1723207 -19.9575386, -44.1724439 -19.9575883, -44.1742798 -19.9583293, -44.1748841 -19.9585688, -44.1751118 -19.9586796, -44.1752554 -19.9587769, -44.1752644 -19.9587881, -44.1756052 -19.9592143, -44.1766415 -19.9602689, -44.1774912 -19.9612387, -44.177663 -19.961364, -44.177856 -19.9614494, -44.178034 -19.9615125, -44.1782475 -19.9615423, -44.1785115 -19.9615155, -44.1795404 -19.9610879, -44.1796393 -19.9610759, -44.1798873 -19.9610459, -44.1802404 -19.961036, -44.1804714 -19.9609634, -44.181059 -19.9605365, -44.1815113 -19.9602333, -44.1826712 -19.9594067, -44.1829715 -19.9592551, -44.1837201 -19.9590611, -44.1839277 -19.9590073, -44.1853022 -19.9586512, -44.1856812 -19.9585316, -44.1862915 -19.9584212, -44.1866215 -19.9583494, -44.1867651 -19.9583391, -44.1868852 -19.9583372, -44.1872523 -19.9583313, -44.187823 -19.9583281, -44.1884457 -19.958351, -44.1889559 -19.958437, -44.1893825 -19.9585816, -44.1897582 -19.9587828, -44.1901186 -19.9590453, -44.1912457 -19.9602029, -44.1916575 -19.9606307, -44.1921624 -19.9611588, -44.1925367 -19.9615872, -44.1931832 -19.9622566, -44.1938468 -19.9629343, -44.194089 -19.9631996, -44.1943924 -19.9634141, -44.1946006 -19.9635104, -44.1948789 -19.963599, -44.1957402 -19.9637569, -44.1964094 -19.9638505, -44.1965875 -19.9639188, -44.1967865 -19.9640801, -44.197096 -19.9643572, -44.1972765 -19.964458, -44.1974407 -19.9644824, -44.1976234 -19.9644668, -44.1977654 -19.9644282, -44.1980715 -19.96417, -44.1984541 -19.9638069, -44.1986632 -19.9636002, -44.1988132 -19.9634172, -44.1989542 -19.9632962, -44.1991349 -19.9631081)");
		OGCGeometry result12 = point.union(lineString);
		String text12 = result12.asText();
		assertEquals(text12, "LINESTRING (-44.1247493 -19.9467657, -44.1247979 -19.9468385, -44.1249043 -19.946934, -44.1251096 -19.9470651, -44.1252609 -19.9471383, -44.1254992 -19.947204, -44.1257652 -19.947229, -44.1261292 -19.9471833, -44.1268946 -19.9470098, -44.1276847 -19.9468416, -44.127831 -19.9468143, -44.1282639 -19.9467366, -44.1284569 -19.9467237, -44.1287119 -19.9467261, -44.1289437 -19.9467665, -44.1291499 -19.9468221, -44.1293856 -19.9469396, -44.1298857 -19.9471497, -44.1300908 -19.9472071, -44.1302743 -19.9472331, -44.1305029 -19.9472364, -44.1306498 -19.9472275, -44.1308054 -19.947216, -44.1308553 -19.9472037, -44.1313206 -19.9471394, -44.1317889 -19.9470854, -44.1330422 -19.9468887, -44.1337465 -19.9467083, -44.1339922 -19.9466842, -44.1341506 -19.9466997, -44.1343621 -19.9467226, -44.1345134 -19.9467855, -44.1346494 -19.9468456, -44.1347295 -19.946881, -44.1347988 -19.9469299, -44.1350231 -19.9471131, -44.1355843 -19.9478307, -44.1357802 -19.9480557, -44.1366289 -19.949198, -44.1370384 -19.9497001, -44.137386 -19.9501921, -44.1374113 -19.9502263, -44.1380888 -19.9510925, -44.1381769 -19.9513526, -44.1382509 -19.9516202, -44.1383014 -19.9522136, -44.1383889 -19.9530931, -44.1384227 -19.9538784, -44.1384512 -19.9539653, -44.1384555 -19.9539807, -44.1384901 -19.9541928, -44.1385563 -19.9543859, -44.1386656 -19.9545781, -44.1387339 -19.9546889, -44.1389219 -19.9548661, -44.1391695 -19.9550384, -44.1393672 -19.9551414, -44.1397538 -19.9552208, -44.1401714 -19.9552332, -44.1405656 -19.9551143, -44.1406198 -19.9550853, -44.1407579 -19.9550224, -44.1409029 -19.9549201, -44.1410283 -19.9548257, -44.1413902 -19.9544132, -44.141835 -19.9539274, -44.142268 -19.953484, -44.1427036 -19.9531023, -44.1436229 -19.952259, -44.1437568 -19.9521565, -44.1441783 -19.9517273, -44.144644 -19.9512109, -44.1452538 -19.9505663, -44.1453541 -19.9504774, -44.1458653 -19.9500442, -44.1463563 -19.9496473, -44.1467534 -19.9492812, -44.1470553 -19.9490028, -44.1475804 -19.9485293, -44.1479838 -19.9482096, -44.1485003 -19.9478532, -44.1489451 -19.9477314, -44.1492225 -19.9477024, -44.149453 -19.9476684, -44.149694 -19.9476387, -44.1499556 -19.9475436, -44.1501398 -19.9474234, -44.1502723 -19.9473206, -44.150421 -19.9471473, -44.1505043 -19.9470004, -44.1507664 -19.9462594, -44.150867 -19.9459518, -44.1509225 -19.9457843, -44.1511168 -19.945466, -44.1513601 -19.9452272, -44.1516846 -19.944999, -44.15197 -19.9448738, -44.1525994 -19.9447263, -44.1536614 -19.9444791, -44.1544071 -19.9442671, -44.1548978 -19.9441275, -44.1556247 -19.9438304, -44.1565996 -19.9434083, -44.1570351 -19.9432556, -44.1573142 -19.9432091, -44.1575332 -19.9431645, -44.157931 -19.9431484, -44.1586408 -19.9431504, -44.1593575 -19.9431457, -44.1596498 -19.9431562, -44.1600991 -19.9431475, -44.1602331 -19.9431567, -44.1607926 -19.9432449, -44.1609723 -19.9432499, -44.16176186699087 -19.94326480383335, -44.1623815 -19.9432765, -44.1628299 -19.9433645, -44.1632475 -19.9435839, -44.1633456 -19.9436559, -44.1636261 -19.9439375, -44.1638186 -19.9442439, -44.1642535 -19.9451781, -44.165178 -19.947156, -44.1652928 -19.9474016, -44.1653074 -19.9474329, -44.1654026 -19.947766, -44.1654774 -19.9481718, -44.1655699 -19.9490241, -44.1656196 -19.9491538, -44.1659735 -19.9499097, -44.1662485 -19.9504925, -44.1662996 -19.9506347, -44.1663574 -19.9512961, -44.1664094 -19.9519273, -44.1664144 -19.9519881, -44.1664799 -19.9526399, -44.1666965 -19.9532586, -44.1671191 -19.9544126, -44.1672019 -19.9545869, -44.1673344 -19.9547603, -44.1675958 -19.9550466, -44.1692349 -19.9567775, -44.1694607 -19.9569284, -44.1718843 -19.9574147, -44.1719167 -19.9574206, -44.1721627 -19.9574748, -44.1723207 -19.9575386, -44.1724439 -19.9575883, -44.1742798 -19.9583293, -44.1748841 -19.9585688, -44.1751118 -19.9586796, -44.1752554 -19.9587769, -44.1752644 -19.9587881, -44.1756052 -19.9592143, -44.1766415 -19.9602689, -44.1774912 -19.9612387, -44.177663 -19.961364, -44.177856 -19.9614494, -44.178034 -19.9615125, -44.1782475 -19.9615423, -44.1785115 -19.9615155, -44.1795404 -19.9610879, -44.1796393 -19.9610759, -44.1798873 -19.9610459, -44.1802404 -19.961036, -44.1804714 -19.9609634, -44.181059 -19.9605365, -44.1815113 -19.9602333, -44.1826712 -19.9594067, -44.1829715 -19.9592551, -44.1837201 -19.9590611, -44.1839277 -19.9590073, -44.1853022 -19.9586512, -44.1856812 -19.9585316, -44.1862915 -19.9584212, -44.1866215 -19.9583494, -44.1867651 -19.9583391, -44.1868852 -19.9583372, -44.1872523 -19.9583313, -44.187823 -19.9583281, -44.1884457 -19.958351, -44.1889559 -19.958437, -44.1893825 -19.9585816, -44.1897582 -19.9587828, -44.1901186 -19.9590453, -44.1912457 -19.9602029, -44.1916575 -19.9606307, -44.1921624 -19.9611588, -44.1925367 -19.9615872, -44.1931832 -19.9622566, -44.1938468 -19.9629343, -44.194089 -19.9631996, -44.1943924 -19.9634141, -44.1946006 -19.9635104, -44.1948789 -19.963599, -44.1957402 -19.9637569, -44.1964094 -19.9638505, -44.1965875 -19.9639188, -44.1967865 -19.9640801, -44.197096 -19.9643572, -44.1972765 -19.964458, -44.1974407 -19.9644824, -44.1976234 -19.9644668, -44.1977654 -19.9644282, -44.1980715 -19.96417, -44.1984541 -19.9638069, -44.1986632 -19.9636002, -44.1988132 -19.9634172, -44.1989542 -19.9632962, -44.1991349 -19.9631081)");
	}
	
}
