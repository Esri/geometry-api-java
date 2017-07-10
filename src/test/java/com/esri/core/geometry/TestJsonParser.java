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

import java.util.Hashtable;
import java.io.IOException;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class TestJsonParser extends TestCase {

	JsonFactory factory = new JsonFactory();
	SpatialReference spatialReferenceWebMerc1 = SpatialReference.create(102100);
	SpatialReference spatialReferenceWebMerc2 = SpatialReference.create(spatialReferenceWebMerc1.getLatestID());
	SpatialReference spatialReferenceWGS84 = SpatialReference.create(4326);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void test3DPoint() throws JsonParseException, IOException {
		String jsonString3DPt = "{\"x\" : -118.15, \"y\" : 33.80, \"z\" : 10.0, \"spatialReference\" : {\"wkid\" : 4326}}";

		JsonParser jsonParser3DPt = factory.createParser(jsonString3DPt);
		MapGeometry point3DMP = GeometryEngine.jsonToGeometry(jsonParser3DPt);
		assertTrue(-118.15 == ((Point) point3DMP.getGeometry()).getX());
		assertTrue(33.80 == ((Point) point3DMP.getGeometry()).getY());
		assertTrue(spatialReferenceWGS84.getID() == point3DMP.getSpatialReference().getID());
	}

	@Test
	public void test3DPoint1() throws JsonParseException, IOException {
		Point point1 = new Point(10.0, 20.0);
		Point pointEmpty = new Point();
		{
			JsonParser pointWebMerc1Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWebMerc1, point1));
			MapGeometry pointWebMerc1MP = GeometryEngine.jsonToGeometry(pointWebMerc1Parser);
			assertTrue(point1.getX() == ((Point) pointWebMerc1MP.getGeometry()).getX());
			assertTrue(point1.getY() == ((Point) pointWebMerc1MP.getGeometry()).getY());
			int srIdOri = spatialReferenceWebMerc1.getID();
			int srIdAfter = pointWebMerc1MP.getSpatialReference().getID();
			assertTrue(srIdOri == srIdAfter || srIdAfter == 3857);

			pointWebMerc1Parser = factory.createJsonParser(GeometryEngine.geometryToJson(null, point1));
			pointWebMerc1MP = GeometryEngine.jsonToGeometry(pointWebMerc1Parser);
			assertTrue(null == pointWebMerc1MP.getSpatialReference());

			String pointEmptyString = GeometryEngine.geometryToJson(spatialReferenceWebMerc1, pointEmpty);
			pointWebMerc1Parser = factory.createJsonParser(pointEmptyString);

			pointWebMerc1MP = GeometryEngine.jsonToGeometry(pointWebMerc1Parser);
			assertTrue(pointWebMerc1MP.getGeometry().isEmpty());
			int srIdOri2 = spatialReferenceWebMerc1.getID();
			int srIdAfter2 = pointWebMerc1MP.getSpatialReference().getID();
			assertTrue(srIdOri2 == srIdAfter2 || srIdAfter2 == 3857);
		}
	}

	@Test
	public void test3DPoint2() throws JsonParseException, IOException {
		{
			Point point1 = new Point(10.0, 20.0);
			JsonParser pointWebMerc2Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWebMerc2, point1));
			MapGeometry pointWebMerc2MP = GeometryEngine.jsonToGeometry(pointWebMerc2Parser);
			assertTrue(point1.getX() == ((Point) pointWebMerc2MP.getGeometry()).getX());
			assertTrue(point1.getY() == ((Point) pointWebMerc2MP.getGeometry()).getY());
			assertTrue(spatialReferenceWebMerc2.getLatestID() == pointWebMerc2MP.getSpatialReference().getLatestID());
		}
	}

	@Test
	public void test3DPoint3() throws JsonParseException, IOException {
		{
			Point point1 = new Point(10.0, 20.0);
			JsonParser pointWgs84Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWGS84, point1));
			MapGeometry pointWgs84MP = GeometryEngine.jsonToGeometry(pointWgs84Parser);
			assertTrue(point1.getX() == ((Point) pointWgs84MP.getGeometry()).getX());
			assertTrue(point1.getY() == ((Point) pointWgs84MP.getGeometry()).getY());
			assertTrue(spatialReferenceWGS84.getID() == pointWgs84MP.getSpatialReference().getID());
		}
	}

	@Test
	public void testMultiPoint() throws JsonParseException, IOException {
		MultiPoint multiPoint1 = new MultiPoint();
		multiPoint1.add(-97.06138, 32.837);
		multiPoint1.add(-97.06133, 32.836);
		multiPoint1.add(-97.06124, 32.834);
		multiPoint1.add(-97.06127, 32.832);

		{
			JsonParser mPointWgs84Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWGS84, multiPoint1));
			MapGeometry mPointWgs84MP = GeometryEngine.jsonToGeometry(mPointWgs84Parser);
			assertTrue(multiPoint1.getPointCount() == ((MultiPoint) mPointWgs84MP.getGeometry()).getPointCount());
			assertTrue(multiPoint1.getPoint(0).getX() == ((MultiPoint) mPointWgs84MP.getGeometry()).getPoint(0).getX());
			assertTrue(multiPoint1.getPoint(0).getY() == ((MultiPoint) mPointWgs84MP.getGeometry()).getPoint(0).getY());
			int lastIndex = multiPoint1.getPointCount() - 1;
			assertTrue(multiPoint1.getPoint(lastIndex).getX() == ((MultiPoint) mPointWgs84MP.getGeometry())
					.getPoint(lastIndex).getX());
			assertTrue(multiPoint1.getPoint(lastIndex).getY() == ((MultiPoint) mPointWgs84MP.getGeometry())
					.getPoint(lastIndex).getY());

			assertTrue(spatialReferenceWGS84.getID() == mPointWgs84MP.getSpatialReference().getID());

			MultiPoint mPointEmpty = new MultiPoint();
			String mPointEmptyString = GeometryEngine.geometryToJson(spatialReferenceWGS84, mPointEmpty);
			mPointWgs84Parser = factory.createJsonParser(mPointEmptyString);

			mPointWgs84MP = GeometryEngine.jsonToGeometry(mPointWgs84Parser);
			assertTrue(mPointWgs84MP.getGeometry().isEmpty());
			assertTrue(spatialReferenceWGS84.getID() == mPointWgs84MP.getSpatialReference().getID());

		}
	}

	@Test
	public void testPolyline() throws JsonParseException, IOException {
		Polyline polyline = new Polyline();
		polyline.startPath(-97.06138, 32.837);
		polyline.lineTo(-97.06133, 32.836);
		polyline.lineTo(-97.06124, 32.834);
		polyline.lineTo(-97.06127, 32.832);

		polyline.startPath(-97.06326, 32.759);
		polyline.lineTo(-97.06298, 32.755);

		{
			JsonParser polylinePathsWgs84Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWGS84, polyline));
			MapGeometry mPolylineWGS84MP = GeometryEngine.jsonToGeometry(polylinePathsWgs84Parser);

			assertTrue(polyline.getPointCount() == ((Polyline) mPolylineWGS84MP.getGeometry()).getPointCount());
			assertTrue(polyline.getPoint(0).getX() == ((Polyline) mPolylineWGS84MP.getGeometry()).getPoint(0).getX());
			assertTrue(polyline.getPoint(0).getY() == ((Polyline) mPolylineWGS84MP.getGeometry()).getPoint(0).getY());

			assertTrue(polyline.getPathCount() == ((Polyline) mPolylineWGS84MP.getGeometry()).getPathCount());
			assertTrue(polyline.getSegmentCount() == ((Polyline) mPolylineWGS84MP.getGeometry()).getSegmentCount());
			assertTrue(polyline.getSegmentCount(0) == ((Polyline) mPolylineWGS84MP.getGeometry()).getSegmentCount(0));
			assertTrue(polyline.getSegmentCount(1) == ((Polyline) mPolylineWGS84MP.getGeometry()).getSegmentCount(1));

			int lastIndex = polyline.getPointCount() - 1;
			assertTrue(polyline.getPoint(lastIndex).getX() == ((Polyline) mPolylineWGS84MP.getGeometry())
					.getPoint(lastIndex).getX());
			assertTrue(polyline.getPoint(lastIndex).getY() == ((Polyline) mPolylineWGS84MP.getGeometry())
					.getPoint(lastIndex).getY());

			assertTrue(spatialReferenceWGS84.getID() == mPolylineWGS84MP.getSpatialReference().getID());

			Polyline emptyPolyline = new Polyline();
			String emptyString = GeometryEngine.geometryToJson(spatialReferenceWGS84, emptyPolyline);
			mPolylineWGS84MP = GeometryEngine.jsonToGeometry(factory.createJsonParser(emptyString));
			assertTrue(mPolylineWGS84MP.getGeometry().isEmpty());
			assertTrue(spatialReferenceWGS84.getID() == mPolylineWGS84MP.getSpatialReference().getID());
		}
	}

	@Test
	public void testPolygon() throws JsonParseException, IOException {
		Polygon polygon = new Polygon();
		polygon.startPath(-97.06138, 32.837);
		polygon.lineTo(-97.06133, 32.836);
		polygon.lineTo(-97.06124, 32.834);
		polygon.lineTo(-97.06127, 32.832);

		polygon.startPath(-97.06326, 32.759);
		polygon.lineTo(-97.06298, 32.755);

		{
			JsonParser polygonPathsWgs84Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWGS84, polygon));
			MapGeometry mPolygonWGS84MP = GeometryEngine.jsonToGeometry(polygonPathsWgs84Parser);

			assertTrue(polygon.getPointCount() + 1 == ((Polygon) mPolygonWGS84MP.getGeometry()).getPointCount());
			assertTrue(polygon.getPoint(0).getX() == ((Polygon) mPolygonWGS84MP.getGeometry()).getPoint(0).getX());
			assertTrue(polygon.getPoint(0).getY() == ((Polygon) mPolygonWGS84MP.getGeometry()).getPoint(0).getY());

			assertTrue(polygon.getPathCount() == ((Polygon) mPolygonWGS84MP.getGeometry()).getPathCount());
			assertTrue(polygon.getSegmentCount() + 1 == ((Polygon) mPolygonWGS84MP.getGeometry()).getSegmentCount());
			assertTrue(polygon.getSegmentCount(0) == ((Polygon) mPolygonWGS84MP.getGeometry()).getSegmentCount(0));
			assertTrue(polygon.getSegmentCount(1) + 1 == ((Polygon) mPolygonWGS84MP.getGeometry()).getSegmentCount(1));

			int lastIndex = polygon.getPointCount() - 1;
			assertTrue(polygon.getPoint(lastIndex).getX() == ((Polygon) mPolygonWGS84MP.getGeometry())
					.getPoint(lastIndex).getX());
			assertTrue(polygon.getPoint(lastIndex).getY() == ((Polygon) mPolygonWGS84MP.getGeometry())
					.getPoint(lastIndex).getY());

			assertTrue(spatialReferenceWGS84.getID() == mPolygonWGS84MP.getSpatialReference().getID());

			Polygon emptyPolygon = new Polygon();
			String emptyPolygonString = GeometryEngine.geometryToJson(spatialReferenceWGS84, emptyPolygon);
			polygonPathsWgs84Parser = factory.createJsonParser(emptyPolygonString);
			mPolygonWGS84MP = GeometryEngine.jsonToGeometry(polygonPathsWgs84Parser);

			assertTrue(mPolygonWGS84MP.getGeometry().isEmpty());
			assertTrue(spatialReferenceWGS84.getID() == mPolygonWGS84MP.getSpatialReference().getID());
		}
	}

	@Test
	public void testEnvelope() throws JsonParseException, IOException {
		Envelope envelope = new Envelope();
		envelope.setCoords(-109.55, 25.76, -86.39, 49.94);

		{
			JsonParser envelopeWGS84Parser = factory
					.createJsonParser(GeometryEngine.geometryToJson(spatialReferenceWGS84, envelope));
			MapGeometry envelopeWGS84MP = GeometryEngine.jsonToGeometry(envelopeWGS84Parser);
			assertTrue(envelope.isEmpty() == envelopeWGS84MP.getGeometry().isEmpty());
			assertTrue(envelope.getXMax() == ((Envelope) envelopeWGS84MP.getGeometry()).getXMax());
			assertTrue(envelope.getYMax() == ((Envelope) envelopeWGS84MP.getGeometry()).getYMax());
			assertTrue(envelope.getXMin() == ((Envelope) envelopeWGS84MP.getGeometry()).getXMin());
			assertTrue(envelope.getYMin() == ((Envelope) envelopeWGS84MP.getGeometry()).getYMin());
			assertTrue(spatialReferenceWGS84.getID() == envelopeWGS84MP.getSpatialReference().getID());

			Envelope emptyEnvelope = new Envelope();
			String emptyEnvString = GeometryEngine.geometryToJson(spatialReferenceWGS84, emptyEnvelope);
			envelopeWGS84Parser = factory.createJsonParser(emptyEnvString);
			envelopeWGS84MP = GeometryEngine.jsonToGeometry(envelopeWGS84Parser);

			assertTrue(envelopeWGS84MP.getGeometry().isEmpty());
			assertTrue(spatialReferenceWGS84.getID() == envelopeWGS84MP.getSpatialReference().getID());
		}
	}

	@Test
	public void testCR181369() throws JsonParseException, IOException {
		// CR181369
		{
			String jsonStringPointAndWKT = "{\"x\":10.0,\"y\":20.0,\"spatialReference\":{\"wkt\" : \"PROJCS[\\\"NAD83_UTM_zone_15N\\\",GEOGCS[\\\"GCS_North_American_1983\\\",DATUM[\\\"D_North_American_1983\\\",SPHEROID[\\\"GRS_1980\\\",6378137.0,298.257222101]],PRIMEM[\\\"Greenwich\\\",0.0],UNIT[\\\"Degree\\\",0.0174532925199433]],PROJECTION[\\\"Transverse_Mercator\\\"],PARAMETER[\\\"false_easting\\\",500000.0],PARAMETER[\\\"false_northing\\\",0.0],PARAMETER[\\\"central_meridian\\\",-93.0],PARAMETER[\\\"scale_factor\\\",0.9996],PARAMETER[\\\"latitude_of_origin\\\",0.0],UNIT[\\\"Meter\\\",1.0]]\"} }";
			JsonParser jsonParserPointAndWKT = factory.createJsonParser(jsonStringPointAndWKT);
			MapGeometry mapGeom2 = GeometryEngine.jsonToGeometry(jsonParserPointAndWKT);
			String jsonStringPointAndWKT2 = GeometryEngine.geometryToJson(mapGeom2.getSpatialReference(),
					mapGeom2.getGeometry());
			JsonParser jsonParserPointAndWKT2 = factory.createJsonParser(jsonStringPointAndWKT2);
			MapGeometry mapGeom3 = GeometryEngine.jsonToGeometry(jsonParserPointAndWKT2);
			assertTrue(((Point) mapGeom2.getGeometry()).getX() == ((Point) mapGeom3.getGeometry()).getX());
			assertTrue(((Point) mapGeom2.getGeometry()).getY() == ((Point) mapGeom3.getGeometry()).getY());
			assertTrue(mapGeom2.getSpatialReference().getText().equals(mapGeom3.getSpatialReference().getText()));
			assertTrue(mapGeom2.getSpatialReference().getID() == mapGeom3.getSpatialReference().getID());
		}
	}

	@Test
	public void testSpatialRef() throws JsonParseException, IOException {
		// String jsonStringPt =
		// "{\"x\":-20037508.342787,\"y\":20037508.342787},\"spatialReference\":{\"wkid\":102100}}";
		String jsonStringPt = "{\"x\":10.0,\"y\":20.0,\"spatialReference\":{\"wkid\": 102100}}";// 102100
		@SuppressWarnings("unused")
		String jsonStringPt2 = "{\"x\":10.0,\"y\":20.0,\"spatialReference\":{\"wkid\":4326}}";
		String jsonStringMpt = "{ \"points\" : [ [-97.06138,32.837], [-97.06133,32.836], [-97.06124,32.834], [-97.06127,32.832] ], \"spatialReference\" : {\"wkid\" : 4326}}";// 4326
		String jsonStringMpt3D = "{\"hasZs\" : true,\"points\" : [ [-97.06138,32.837,35.0], [-97.06133,32.836,35.1], [-97.06124,32.834,35.2], [-97.06127,32.832,35.3] ],\"spatialReference\" : {\"wkid\" : 4326}}";
		String jsonStringPl = "{\"paths\" : [  [ [-97.06138,32.837], [-97.06133,32.836], [-97.06124,32.834], [-97.06127,32.832] ],  [ [-97.06326,32.759], [-97.06298,32.755] ]],\"spatialReference\" : {\"wkid\" : 4326}}";
		String jsonStringPl3D = "{\"hasMs\" : true,\"paths\" : [[ [-97.06138,32.837,5], [-97.06133,32.836,6], [-97.06124,32.834,7], [-97.06127,32.832,8] ],[ [-97.06326,32.759], [-97.06298,32.755] ]],\"spatialReference\" : {\"wkid\" : 4326}}";
		String jsonStringPg = "{ \"rings\" :[  [ [-97.06138,32.837], [-97.06133,32.836], [-97.06124,32.834], [-97.06127,32.832], [-97.06138,32.837] ],  [ [-97.06326,32.759], [-97.06298,32.755], [-97.06153,32.749], [-97.06326,32.759] ]], \"spatialReference\" : {\"wkt\" : \"\"}}";
		String jsonStringPg3D = "{\"hasZs\" : true,\"hasMs\" : true,\"rings\" : [ [ [-97.06138, 32.837, 35.1, 4], [-97.06133, 32.836, 35.2, 4.1], [-97.06124, 32.834, 35.3, 4.2], [-97.06127, 32.832, 35.2, 44.3], [-97.06138, 32.837, 35.1, 4] ],[ [-97.06326, 32.759, 35.4], [-97.06298, 32.755, 35.5], [-97.06153, 32.749, 35.6], [-97.06326, 32.759, 35.4] ]],\"spatialReference\" : {\"wkid\" : 4326}}";
		String jsonStringPg2 = "{ \"spatialReference\" : {\"wkid\" : 4326}, \"rings\" : [[[-118.35,32.81],[-118.42,32.806],[-118.511,32.892],[-118.35,32.81]]]}";
		String jsonStringPg3 = "{ \"spatialReference\": {\"layerName\":\"GAS_POINTS\",\"name\":null,\"sdesrid\":102100,\"wkid\":102100,\"wkt\":null}}";
		String jsonString2SpatialReferences = "{ \"spatialReference\": {\"layerName\":\"GAS_POINTS\",\"name\":null,\"sdesrid\":102100,\"wkid\":102100,\"wkt\":\"GEOGCS[\\\"GCS_WGS_1984\\\",DATUM[\\\"D_WGS_1984\\\",SPHEROID[\\\"WGS_1984\\\",6378137,298.257223563]],PRIMEM[\\\"Greenwich\\\",0],UNIT[\\\"Degree\\\",0.017453292519943295]]\"}}";
		String jsonString2SpatialReferences2 = "{ \"spatialReference\": {\"layerName\":\"GAS_POINTS\",\"name\":null,\"sdesrid\":10,\"wkid\":10,\"wkt\":\"GEOGCS[\\\"GCS_WGS_1984\\\",DATUM[\\\"D_WGS_1984\\\",SPHEROID[\\\"WGS_1984\\\",6378137,298.257223563]],PRIMEM[\\\"Greenwich\\\",0],UNIT[\\\"Degree\\\",0.017453292519943295]]\"}}";
		String jsonStringSR = "{\"wkid\" : 4326}";
		String jsonStringEnv = "{\"xmin\" : -109.55, \"ymin\" : 25.76, \"xmax\" : -86.39, \"ymax\" : 49.94,\"spatialReference\" : {\"wkid\" : 4326}}";
		String jsonStringHongKon = "{\"xmin\" : -122.55, \"ymin\" : 37.65, \"xmax\" : -122.28, \"ymax\" : 37.84,\"spatialReference\" : {\"wkid\" : 4326}}";
		@SuppressWarnings("unused")
		String jsonStringWKT = " {\"wkt\" : \"GEOGCS[\\\"GCS_WGS_1984\\\",DATUM[\\\"D_WGS_1984\\\",SPHEROID[\\\"WGS_1984\\\",6378137,298.257223563]],PRIMEM[\\\"Greenwich\\\",0],UNIT[\\\"Degree\\\",0.017453292519943295]]\"}";
		String jsonStringInvalidWKID = "{\"x\":10.0,\"y\":20.0},\"spatialReference\":{\"wkid\":35253523}}";
		String jsonStringOregon = "{\"xmin\":7531831.219849482,\"ymin\":585702.9799639136,\"xmax\":7750143.589982405,\"ymax\":733289.6299999952,\"spatialReference\":{\"wkid\":102726}}";

		JsonParser jsonParserPt = factory.createJsonParser(jsonStringPt);
		JsonParser jsonParserMpt = factory.createJsonParser(jsonStringMpt);
		JsonParser jsonParserMpt3D = factory.createJsonParser(jsonStringMpt3D);
		JsonParser jsonParserPl = factory.createJsonParser(jsonStringPl);
		JsonParser jsonParserPl3D = factory.createJsonParser(jsonStringPl3D);
		JsonParser jsonParserPg = factory.createJsonParser(jsonStringPg);
		JsonParser jsonParserPg3D = factory.createJsonParser(jsonStringPg3D);
		JsonParser jsonParserPg2 = factory.createJsonParser(jsonStringPg2);
		@SuppressWarnings("unused")
		JsonParser jsonParserSR = factory.createJsonParser(jsonStringSR);
		JsonParser jsonParserEnv = factory.createJsonParser(jsonStringEnv);
		JsonParser jsonParserPg3 = factory.createJsonParser(jsonStringPg3);
		@SuppressWarnings("unused")
		JsonParser jsonParserCrazy1 = factory.createJsonParser(jsonString2SpatialReferences);
		@SuppressWarnings("unused")
		JsonParser jsonParserCrazy2 = factory.createJsonParser(jsonString2SpatialReferences2);
		JsonParser jsonParserInvalidWKID = factory.createJsonParser(jsonStringInvalidWKID);
		@SuppressWarnings("unused")
		JsonParser jsonParseHongKon = factory.createJsonParser(jsonStringHongKon);
		JsonParser jsonParseOregon = factory.createJsonParser(jsonStringOregon);

		MapGeometry mapGeom = GeometryEngine.jsonToGeometry(jsonParserPt);
		// showProjectedGeometryInfo(mapGeom);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 102100);

		MapGeometry mapGeomOregon = GeometryEngine.jsonToGeometry(jsonParseOregon);
		Assert.assertTrue(mapGeomOregon.getSpatialReference().getID() == 102726);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserMpt);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 4326);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserMpt3D);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 4326);
		{
			Assert.assertTrue(((MultiPoint) mapGeom.getGeometry()).getPoint(0).getX() == -97.06138);
			Assert.assertTrue(((MultiPoint) mapGeom.getGeometry()).getPoint(0).getY() == 32.837);
			Assert.assertTrue(((MultiPoint) mapGeom.getGeometry()).getPoint(3).getX() == -97.06127);
			Assert.assertTrue(((MultiPoint) mapGeom.getGeometry()).getPoint(3).getY() == 32.832);
		}
		// showProjectedGeometryInfo(mapGeom);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserPl);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 4326);
		// showProjectedGeometryInfo(mapGeom);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserPl3D);
		{
			// [[ [-97.06138,32.837,5], [-97.06133,32.836,6],
			// [-97.06124,32.834,7], [-97.06127,32.832,8] ],
			// [ [-97.06326,32.759], [-97.06298,32.755] ]]";
			Assert.assertTrue(((Polyline) mapGeom.getGeometry()).getPoint(0).getX() == -97.06138);
			Assert.assertTrue(((Polyline) mapGeom.getGeometry()).getPoint(0).getY() == 32.837);
			int lastIndex = ((Polyline) mapGeom.getGeometry()).getPointCount() - 1;
			Assert.assertTrue(((Polyline) mapGeom.getGeometry()).getPoint(lastIndex).getX() == -97.06298);// -97.06153,
																											// 32.749
			Assert.assertTrue(((Polyline) mapGeom.getGeometry()).getPoint(lastIndex).getY() == 32.755);
			int lastIndexFirstLine = ((Polyline) mapGeom.getGeometry()).getPathEnd(0) - 1;
			Assert.assertTrue(((Polyline) mapGeom.getGeometry()).getPoint(lastIndexFirstLine).getX() == -97.06127);// -97.06153,
			// 32.749
			Assert.assertTrue(((Polyline) mapGeom.getGeometry()).getPoint(lastIndexFirstLine).getY() == 32.832);
		}

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserPg);
		Assert.assertTrue(mapGeom.getSpatialReference() == null);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserPg3D);
		{
			Assert.assertTrue(((Polygon) mapGeom.getGeometry()).getPoint(0).getX() == -97.06138);
			Assert.assertTrue(((Polygon) mapGeom.getGeometry()).getPoint(0).getY() == 32.837);
			int lastIndex = ((Polygon) mapGeom.getGeometry()).getPointCount() - 1;
			Assert.assertTrue(((Polygon) mapGeom.getGeometry()).getPoint(lastIndex).getX() == -97.06153);// -97.06153,
																											// 32.749
			Assert.assertTrue(((Polygon) mapGeom.getGeometry()).getPoint(lastIndex).getY() == 32.749);
		}

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserPg2);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 4326);
		// showProjectedGeometryInfo(mapGeom);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserPg3);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 102100);
		// showProjectedGeometryInfo(mapGeom);

		// mapGeom = GeometryEngine.jsonToGeometry(jsonParserCrazy1);
		// Assert.assertTrue(mapGeom.getSpatialReference().getText().equals(""));
		// showProjectedGeometryInfo(mapGeom);

		mapGeom = GeometryEngine.jsonToGeometry(jsonParserEnv);
		Assert.assertTrue(mapGeom.getSpatialReference().getID() == 4326);
		// showProjectedGeometryInfo(mapGeom);

		try {
			GeometryEngine.jsonToGeometry(jsonParserInvalidWKID);
		} catch (Exception ex) {
			Assert.assertTrue("Should not throw for invalid wkid", false);
		}
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

		SpatialReference spatialReference = SpatialReference.create(4326);

		try {
			String jSonStr = GeometryEngine.geometryToJson(spatialReference, pg);
			JsonFactory jf = new JsonFactory();

			JsonParser jp = jf.createJsonParser(jSonStr);
			jp.nextToken();
			MapGeometry mg = GeometryEngine.jsonToGeometry(jp);
			Geometry gm = mg.getGeometry();
			Assert.assertEquals(Geometry.Type.Polygon, gm.getType());
			Assert.assertTrue(mg.getSpatialReference().getID() == 4326);

			Polygon pgNew = (Polygon) gm;

			Assert.assertEquals(pgNew.getPathCount(), pg.getPathCount());
			Assert.assertEquals(pgNew.getPointCount(), pg.getPointCount());
			Assert.assertEquals(pgNew.getSegmentCount(), pg.getSegmentCount());

			Assert.assertEquals(pgNew.getPoint(0).getX(), pg.getPoint(0).getX(), 0.000000001);
			Assert.assertEquals(pgNew.getPoint(1).getX(), pg.getPoint(1).getX(), 0.000000001);
			Assert.assertEquals(pgNew.getPoint(2).getX(), pg.getPoint(2).getX(), 0.000000001);
			Assert.assertEquals(pgNew.getPoint(3).getX(), pg.getPoint(3).getX(), 0.000000001);

			Assert.assertEquals(pgNew.getPoint(0).getY(), pg.getPoint(0).getY(), 0.000000001);
			Assert.assertEquals(pgNew.getPoint(1).getY(), pg.getPoint(1).getY(), 0.000000001);
			Assert.assertEquals(pgNew.getPoint(2).getY(), pg.getPoint(2).getY(), 0.000000001);
			Assert.assertEquals(pgNew.getPoint(3).getY(), pg.getPoint(3).getY(), 0.000000001);
		} catch (Exception ex) {
			String err = ex.getMessage();
			System.out.print(err);
			throw ex;
		}
	}

	@Test
	public static int fromJsonToWkid(JsonParser parser) throws JsonParseException, IOException {
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

	@SuppressWarnings("unused")
	private static void showProjectedGeometryInfo(MapGeometry mapGeom) {
		System.out.println("\n");
		MapGeometry geom = mapGeom;
		// while ((geom = geomCursor.next()) != null) {

		if (geom.getGeometry() instanceof Point) {
			Point pnt = (Point) geom.getGeometry();
			System.out.println("Point(" + pnt.getX() + " , " + pnt.getY() + ")");
			if (geom.getSpatialReference() == null) {
				System.out.println("No spatial reference");
			} else {
				System.out.println("wkid: " + geom.getSpatialReference().getID());
			}

		} else if (geom.getGeometry() instanceof MultiPoint) {
			MultiPoint mp = (MultiPoint) geom.getGeometry();
			System.out.println("Multipoint has " + mp.getPointCount() + " points.");

			System.out.println("wkid: " + geom.getSpatialReference().getID());

		} else if (geom.getGeometry() instanceof Polygon) {
			Polygon mp = (Polygon) geom.getGeometry();
			System.out.println("Polygon has " + mp.getPointCount() + " points and " + mp.getPathCount() + " parts.");
			if (mp.getPathCount() > 1) {
				System.out.println("Part start of 2nd segment : " + mp.getPathStart(1));
				System.out.println("Part end of 2nd segment   : " + mp.getPathEnd(1));
				System.out.println("Part size of 2nd segment  : " + mp.getPathSize(1));

				int start = mp.getPathStart(1);
				int end = mp.getPathEnd(1);
				for (int i = start; i < end; i++) {
					Point pp = mp.getPoint(i);
					System.out.println("Point(" + i + ") = (" + pp.getX() + ", " + pp.getY() + ")");
				}
			}
			System.out.println("wkid: " + geom.getSpatialReference().getID());

		} else if (geom.getGeometry() instanceof Polyline) {
			Polyline mp = (Polyline) geom.getGeometry();
			System.out.println("Polyline has " + mp.getPointCount() + " points and " + mp.getPathCount() + " parts.");
			System.out.println("Part start of 2nd segment : " + mp.getPathStart(1));
			System.out.println("Part end of 2nd segment   : " + mp.getPathEnd(1));
			System.out.println("Part size of 2nd segment  : " + mp.getPathSize(1));
			int start = mp.getPathStart(1);
			int end = mp.getPathEnd(1);
			for (int i = start; i < end; i++) {
				Point pp = mp.getPoint(i);
				System.out.println("Point(" + i + ") = (" + pp.getX() + ", " + pp.getY() + ")");
			}

			System.out.println("wkid: " + geom.getSpatialReference().getID());
		}
	}

	@Test
	public void testGeometryToJSON() {
		Polygon geom = new Polygon();
		geom.startPath(new Point(-113, 34));
		geom.lineTo(new Point(-105, 34));
		geom.lineTo(new Point(-108, 40));

		String outputPolygon1 = GeometryEngine.geometryToJson(-1, geom);// Test
		// WKID
		// == -1
		// System.out.println("Geom JSON STRING is" + outputPolygon1);
		String correctPolygon1 = "{\"rings\":[[[-113,34],[-105,34],[-108,40],[-113,34]]]}";

		assertEquals(correctPolygon1, outputPolygon1);

		String outputPolygon2 = GeometryEngine.geometryToJson(4326, geom);
		// System.out.println("Geom JSON STRING is" + outputPolygon2);

		String correctPolygon2 = "{\"rings\":[[[-113,34],[-105,34],[-108,40],[-113,34]]],\"spatialReference\":{\"wkid\":4326}}";
		assertEquals(correctPolygon2, outputPolygon2);
	}

	@Test
	public void testGeometryToJSONOldID() throws Exception {// CR
		Polygon geom = new Polygon();
		geom.startPath(new Point(-113, 34));
		geom.lineTo(new Point(-105, 34));
		geom.lineTo(new Point(-108, 40));
		String outputPolygon = GeometryEngine.geometryToJson(SpatialReference.create(3857), geom);// Test
																									// WKID
																									// ==
																									// -1
		String correctPolygon = "{\"rings\":[[[-113,34],[-105,34],[-108,40],[-113,34]]],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}";
		assertTrue(outputPolygon.equals(correctPolygon));
		JsonFactory jf = new JsonFactory();
		JsonParser jp = jf.createJsonParser(outputPolygon);
		jp.nextToken();
		MapGeometry mg = GeometryEngine.jsonToGeometry(jp);
		@SuppressWarnings("unused")
		int srId = mg.getSpatialReference().getID();
		@SuppressWarnings("unused")
		int srOldId = mg.getSpatialReference().getOldID();
		Assert.assertTrue(mg.getSpatialReference().getID() == 3857);
		Assert.assertTrue(mg.getSpatialReference().getLatestID() == 3857);
		Assert.assertTrue(mg.getSpatialReference().getOldID() == 102100);
	}
}
