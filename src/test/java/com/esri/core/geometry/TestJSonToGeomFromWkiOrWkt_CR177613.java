package com.esri.core.geometry;

import java.io.IOException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import junit.framework.TestCase;
import org.junit.Test;

public class TestJSonToGeomFromWkiOrWkt_CR177613 extends TestCase {
	JsonFactory factory = new JsonFactory();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testPolygonWithEmptyWKT_NoWKI() throws JsonParseException,
			IOException {
		String jsonStringPg = "{ \"rings\" :[  [ [-97.06138,32.837], [-97.06133,32.836], "
				+ "[-97.06124,32.834], [-97.06127,32.832], [-97.06138,32.837] ],  "
				+ "[ [-97.06326,32.759], [-97.06298,32.755], [-97.06153,32.749], [-97.06326,32.759] ]], "
				+ "\"spatialReference\" : {\"wkt\" : \"\"}}";
		JsonParser jsonParserPg = factory.createJsonParser(jsonStringPg);
		jsonParserPg.nextToken();

		MapGeometry mapGeom = GeometryEngine.jsonToGeometry(jsonParserPg);
		Utils.showProjectedGeometryInfo(mapGeom);
		SpatialReference sr = mapGeom.getSpatialReference();
		assertTrue(sr == null);
	}

	@Test
	public void testOnlyWKI() throws JsonParseException, IOException {
		String jsonStringSR = "{\"wkid\" : 4326}";
		JsonParser jsonParserSR = factory.createJsonParser(jsonStringSR);
		jsonParserSR.nextToken();

		MapGeometry mapGeom = GeometryEngine.jsonToGeometry(jsonParserSR);
		Utils.showProjectedGeometryInfo(mapGeom);
		SpatialReference sr = mapGeom.getSpatialReference();
		assertTrue(sr == null);
	}

	@Test
	public void testMP2onCR175871() throws Exception {
		Polygon pg = new Polygon();
		pg.startPath(-50, 10);
		pg.lineTo(-50, 12);
		pg.lineTo(-45, 12);
		pg.lineTo(-45, 10);

		Polygon pg1 = new Polygon();
		pg1.startPath(-45, 10);
		pg1.lineTo(-40, 10);
		pg1.lineTo(-40, 8);
		pg.add(pg1, false);

		try {
			String jSonStr = GeometryEngine.geometryToJson(4326, pg);
			JsonFactory jf = new JsonFactory();

			JsonParser jp = jf.createJsonParser(jSonStr);
			jp.nextToken();
			MapGeometry mg = GeometryEngine.jsonToGeometry(jp);
			Geometry gm = mg.getGeometry();
			assertEquals(Geometry.Type.Polygon, gm.getType());

			Polygon pgNew = (Polygon) gm;

			assertEquals(pgNew.getPathCount(), pg.getPathCount());
			assertEquals(pgNew.getPointCount(), pg.getPointCount());
			assertEquals(pgNew.getSegmentCount(), pg.getSegmentCount());

			assertEquals(pgNew.getPoint(0).getX(), pg.getPoint(0).getX(),
					0.000000001);
			assertEquals(pgNew.getPoint(1).getX(), pg.getPoint(1).getX(),
					0.000000001);
			assertEquals(pgNew.getPoint(2).getX(), pg.getPoint(2).getX(),
					0.000000001);
			assertEquals(pgNew.getPoint(3).getX(), pg.getPoint(3).getX(),
					0.000000001);

			assertEquals(pgNew.getPoint(0).getY(), pg.getPoint(0).getY(),
					0.000000001);
			assertEquals(pgNew.getPoint(1).getY(), pg.getPoint(1).getY(),
					0.000000001);
			assertEquals(pgNew.getPoint(2).getY(), pg.getPoint(2).getY(),
					0.000000001);
			assertEquals(pgNew.getPoint(3).getY(), pg.getPoint(3).getY(),
					0.000000001);
		} catch (Exception ex) {
			String err = ex.getMessage();
			System.out.print(err);
			throw ex;
		}
	}

	public static int fromJsonToWkid(JsonParser parser)
			throws JsonParseException, IOException {
		int wkid = 0;
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			return 0;
		}

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();

			if ("wkid".equals(fieldName)) {
				parser.nextToken();
				wkid = parser.getIntValue();
			}
		}
		return wkid;
	}
}
