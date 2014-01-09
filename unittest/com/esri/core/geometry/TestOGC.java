package com.esri.core.geometry;

import junit.framework.TestCase;

import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCGeometryCollection;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCMultiCurve;
import com.esri.core.geometry.ogc.OGCMultiPoint;
import com.esri.core.geometry.ogc.OGCMultiPolygon;
import com.esri.core.geometry.ogc.OGCPoint;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.esri.core.geometry.ogc.OGCConcreteGeometryCollection;

import org.codehaus.jackson.JsonParseException;
import org.json.JSONException;

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

	public void testPoint() {
		OGCGeometry g = OGCGeometry.fromText("POINT(1 2)");
		assertTrue(g.geometryType().equals("Point"));
		OGCPoint p = (OGCPoint) g;
		assertTrue(p.X() == 1);
		assertTrue(p.Y() == 2);
		assertTrue(g.equals(OGCGeometry.fromText("POINT(1 2)")));
		assertTrue(!g.equals(OGCGeometry.fromText("POINT(1 3)")));
		OGCGeometry buf = g.buffer(10);
		assertTrue(buf.geometryType().equals("Polygon"));
		OGCPolygon poly = (OGCPolygon) buf.envelope();
		double a = poly.area();
		assertTrue(Math.abs(a - 400) < 1e-1);
	}

	public void testPolygon() {
		OGCGeometry g = OGCGeometry
				.fromText("POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))");
		assertTrue(g.geometryType().equals("Polygon"));
		OGCPolygon p = (OGCPolygon) g;
		assertTrue(p.numInteriorRing() == 1);
		OGCLineString ls = p.exterorRing();
		// assertTrue(ls.pointN(1).equals(OGCGeometry.fromText("POINT(10 -10)")));
		boolean b = ls
				.equals(OGCGeometry
						.fromText("LINESTRING(-10 -10, 10 -10, 10 10, -10 10, -10 -10)"));
		assertTrue(b);
		OGCLineString lsi = p.interiorRingN(0);
		b = lsi.equals(OGCGeometry
				.fromText("LINESTRING(-5 -5, -5 5, 5 5, 5 -5, -5 -5)"));
		assertTrue(b);
		assertTrue(!lsi.equals(ls));
		OGCMultiCurve boundary = p.boundary();
		String s = boundary.asText();
		assertTrue(s.equals("MULTILINESTRING ((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))"));
	}

	public void testGeometryCollection() throws JSONException {
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

	}

	public void testFirstPointOfPolygon() {
		OGCGeometry g = OGCGeometry
				.fromText("POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))");
		assertTrue(g.geometryType().equals("Polygon"));
		OGCPolygon p = (OGCPolygon) g;
		assertTrue(p.numInteriorRing() == 1);
		OGCLineString ls = p.exterorRing();
		OGCPoint p1 = ls.pointN(1);
		assertTrue(ls.pointN(1).equals(OGCGeometry.fromText("POINT(10 -10)")));
		OGCPoint p2 = ls.pointN(3);
		assertTrue(ls.pointN(3).equals(OGCGeometry.fromText("POINT(-10 10)")));
		OGCPoint p0 = ls.pointN(0);
		assertTrue(ls.pointN(0).equals(OGCGeometry.fromText("POINT(-10 -10)")));
		String ms = g.convertToMulti().asText();
		assertTrue(ms.equals("MULTIPOLYGON (((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5)))"));

	}

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

	public void testIntersection() {
		OGCGeometry g = OGCGeometry.fromText("LINESTRING(0 0, 10 10)");
		OGCGeometry g2 = OGCGeometry.fromText("LINESTRING(10 0, 0 10)");
		OGCGeometry u = g.intersection(g2);
		assertTrue(u.dimension() == 0);
		String s = u.asText();
		assertTrue(u.equals(OGCGeometry.fromText("POINT(5 5)")));
	}

	public void testPointSymDif() {
		OGCGeometry g1 = OGCGeometry.fromText("POINT(1 2)");
		OGCGeometry g2 = OGCGeometry.fromText("POINT(3 4)");
		OGCGeometry gg = g1.symDifference(g2);
		assertTrue(gg.equals(OGCGeometry.fromText("MULTIPOINT(1 2, 3 4)")));

		OGCGeometry g3 = OGCGeometry.fromText("POINT(1 2)");
		OGCGeometry gg1 = g1.symDifference(g3);
		assertTrue(gg1 == null || gg1.isEmpty());

	}

	public void testNullSr() {
		String wkt = "point (0 0)";
		OGCGeometry g = OGCGeometry.fromText(wkt);
		g.setSpatialReference(null);
		assertTrue(g.SRID() < 1);
	}

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

	/*
	 This will fail
	public void test_polygon_simplify_for_OGC() {
		try {
			{
				String s = "{\"rings\":[[[0, 0], [0, 10], [10, 10], [10, 0], [0, 0]]]}";
				OGCGeometry g = OGCGeometry.fromJson(s);
				boolean res = g.isSimple();
				assertTrue(res);
				assertTrue(g.isSimpleRelaxed());
				Geometry resg = OperatorSimplifyOGC.local().execute(g.getEsriGeometry(), null, true, null);
				OGCGeometry og = OGCGeometry.createFromEsriGeometry(resg, null);
				assertTrue(og.geometryType().equals("Polygon"));
				assertTrue(((OGCPolygon)og).numInteriorRing() == 0);
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
		} catch (Exception ex) {
			assertTrue(false);
		}
	}
	*/
	
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

	public void testGeometryCollectionBuffer() {
		OGCGeometry g = OGCGeometry
				.fromText("GEOMETRYCOLLECTION(POINT(1 1), POINT(1 1), POINT(1 2), LINESTRING (0 0, 1 1, 1 0, 0 1), MULTIPOLYGON EMPTY, MULTILINESTRING EMPTY)");
		OGCGeometry simpleG = g.buffer(0);
		String t = simpleG.geometryType();
		String rt = simpleG.asText();
		assertTrue(simpleG.geometryType().equals("GeometryCollection"));
	}

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

	public void testIsectTriaJson1() throws JsonParseException, IOException {
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
	
	public void testWktMultiPolygon() {
		String restJson = "{\"rings\": [[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[-90, -90], [90, 90], [-90, 90], [90, -90], [-90, -90]],	[[-10, -10], [-10, 10], [10, 10], [10, -10], [-10, -10]]]}";
		MapGeometry g = null;
		try {
			g = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, restJson);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String wkt = OperatorExportToWkt.local().execute(0, g.getGeometry(), null);
		assertTrue(wkt.equals("MULTIPOLYGON (((-100 -100, 100 -100, 100 100, -100 100, -100 -100), (-90 -90, 90 -90, -90 90, 90 90, -90 -90)), ((-10 -10, 10 -10, 10 10, -10 10, -10 -10)))"));
	}
	
}
