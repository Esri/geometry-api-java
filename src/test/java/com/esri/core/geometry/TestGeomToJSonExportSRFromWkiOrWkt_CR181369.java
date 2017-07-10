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

import java.io.IOException;
import junit.framework.TestCase;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

public class TestGeomToJSonExportSRFromWkiOrWkt_CR181369 extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	JsonFactory factory = new JsonFactory();
	SpatialReference spatialReferenceWebMerc1 = SpatialReference.create(102100);
	SpatialReference spatialReferenceWebMerc2 = SpatialReference
			.create(spatialReferenceWebMerc1.getLatestID());
	SpatialReference spatialReferenceWGS84 = SpatialReference.create(4326);

	@Test
	public void testLocalExport()
			throws JsonParseException, IOException {
		String s = OperatorExportToJson.local().execute(null, new Point(1000000.2, 2000000.3));
		//assertTrue(s.contains("."));
		//assertFalse(s.contains(","));
		Polyline line = new Polyline();
		line.startPath(1.1,  2.2);
		line.lineTo(2.3,  4.5);
		String s1 = OperatorExportToJson.local().execute(null, line);
		assertTrue(s.contains("."));
	}

	boolean testPoint() throws JsonParseException, IOException {
		boolean bAnswer = true;
		Point point1 = new Point(10.0, 20.0);
		Point pointEmpty = new Point();
		{
			JsonParser pointWebMerc1Parser = factory
					.createParser(GeometryEngine.geometryToJson(
							spatialReferenceWebMerc1, point1));
			MapGeometry pointWebMerc1MP = GeometryEngine
					.jsonToGeometry(pointWebMerc1Parser);
			assertTrue(point1.getX() == ((Point) pointWebMerc1MP.getGeometry())
					.getX());
			assertTrue(point1.getY() == ((Point) pointWebMerc1MP.getGeometry())
					.getY());
			assertTrue(spatialReferenceWebMerc1.getID() == pointWebMerc1MP
					.getSpatialReference().getID()
					|| pointWebMerc1MP.getSpatialReference().getID() == 3857);

			if (!checkResultSpatialRef(pointWebMerc1MP, 102100, 3857)) {
				bAnswer = false;
			}

			pointWebMerc1Parser = factory.createParser(GeometryEngine
					.geometryToJson(null, point1));
			pointWebMerc1MP = GeometryEngine
					.jsonToGeometry(pointWebMerc1Parser);
			assertTrue(null == pointWebMerc1MP.getSpatialReference());

			if (pointWebMerc1MP.getSpatialReference() != null) {
				if (!checkResultSpatialRef(pointWebMerc1MP, 102100, 3857)) {
					bAnswer = false;
				}
			}

			String pointEmptyString = GeometryEngine.geometryToJson(
					spatialReferenceWebMerc1, pointEmpty);
			pointWebMerc1Parser = factory.createParser(pointEmptyString);
		}

		JsonParser pointWebMerc2Parser = factory
				.createParser(GeometryEngine.geometryToJson(
						spatialReferenceWebMerc2, point1));
		MapGeometry pointWebMerc2MP = GeometryEngine
				.jsonToGeometry(pointWebMerc2Parser);
		assertTrue(point1.getX() == ((Point) pointWebMerc2MP.getGeometry())
				.getX());
		assertTrue(point1.getY() == ((Point) pointWebMerc2MP.getGeometry())
				.getY());
		assertTrue(spatialReferenceWebMerc2.getLatestID() == pointWebMerc2MP
				.getSpatialReference().getLatestID());
		if (!checkResultSpatialRef(pointWebMerc2MP,
				spatialReferenceWebMerc2.getLatestID(), 0)) {
			bAnswer = false;
		}

		{
			JsonParser pointWgs84Parser = factory
					.createParser(GeometryEngine.geometryToJson(
							spatialReferenceWGS84, point1));
			MapGeometry pointWgs84MP = GeometryEngine
					.jsonToGeometry(pointWgs84Parser);
			assertTrue(point1.getX() == ((Point) pointWgs84MP.getGeometry())
					.getX());
			assertTrue(point1.getY() == ((Point) pointWgs84MP.getGeometry())
					.getY());
			assertTrue(spatialReferenceWGS84.getID() == pointWgs84MP
					.getSpatialReference().getID());
			if (!checkResultSpatialRef(pointWgs84MP, 4326, 0)) {
				bAnswer = false;
			}
		}

		{
			Point p = new Point();
			String s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1,
					p);
			assertTrue(s
					.equals("{\"x\":null,\"y\":null,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));

			p.addAttribute(VertexDescription.Semantics.Z);
			p.addAttribute(VertexDescription.Semantics.M);
			s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1, p);
			assertTrue(s
					.equals("{\"x\":null,\"y\":null,\"z\":null,\"m\":null,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));

		}

		{
			Point p = new Point(10.0, 20.0, 30.0);
			p.addAttribute(VertexDescription.Semantics.M);
			String s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1,
					p);
			assertTrue(s
					.equals("{\"x\":10,\"y\":20,\"z\":30,\"m\":null,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));
		}

		{// import
			String s = "{\"x\":0.0,\"y\":1.0,\"z\":5.0,\"m\":11.0,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}";
			JsonParser parser = factory.createParser(s);
			MapGeometry map_pt = GeometryEngine.jsonToGeometry(parser);
			Point pt = (Point) map_pt.getGeometry();
			assertTrue(pt.getX() == 0.0);
			assertTrue(pt.getY() == 1.0);
			assertTrue(pt.getZ() == 5.0);
			assertTrue(pt.getM() == 11.0);
		}

		{
			String s = "{\"x\" : 5.0, \"y\" : null, \"spatialReference\" : {\"wkid\" : 4326}} ";
			JsonParser parser = factory.createParser(s);
			MapGeometry map_pt = GeometryEngine.jsonToGeometry(parser);
			Point pt = (Point) map_pt.getGeometry();
			assertTrue(pt.isEmpty());
			SpatialReference spatial_reference = map_pt.getSpatialReference();
			assertTrue(spatial_reference.getID() == 4326);
		}

		return bAnswer;
	}

	boolean testMultiPoint() throws JsonParseException, IOException {
		boolean bAnswer = true;

		MultiPoint multiPoint1 = new MultiPoint();
		multiPoint1.add(-97.06138, 32.837);
		multiPoint1.add(-97.06133, 32.836);
		multiPoint1.add(-97.06124, 32.834);
		multiPoint1.add(-97.06127, 32.832);

		{
			String s = GeometryEngine.geometryToJson(spatialReferenceWGS84,
					multiPoint1);
			JsonParser mPointWgs84Parser = factory.createParser(s);
			MapGeometry mPointWgs84MP = GeometryEngine
					.jsonToGeometry(mPointWgs84Parser);
			assertTrue(multiPoint1.getPointCount() == ((MultiPoint) mPointWgs84MP
					.getGeometry()).getPointCount());
			assertTrue(multiPoint1.getPoint(0).getX() == ((MultiPoint) mPointWgs84MP
					.getGeometry()).getPoint(0).getX());
			assertTrue(multiPoint1.getPoint(0).getY() == ((MultiPoint) mPointWgs84MP
					.getGeometry()).getPoint(0).getY());
			int lastIndex = multiPoint1.getPointCount() - 1;
			assertTrue(multiPoint1.getPoint(lastIndex).getX() == ((MultiPoint) mPointWgs84MP
					.getGeometry()).getPoint(lastIndex).getX());
			assertTrue(multiPoint1.getPoint(lastIndex).getY() == ((MultiPoint) mPointWgs84MP
					.getGeometry()).getPoint(lastIndex).getY());

			assertTrue(spatialReferenceWGS84.getID() == mPointWgs84MP
					.getSpatialReference().getID());
			if (!checkResultSpatialRef(mPointWgs84MP, 4326, 0)) {
				bAnswer = false;
			}

		}

		{
			MultiPoint p = new MultiPoint();
			p.addAttribute(VertexDescription.Semantics.Z);
			p.addAttribute(VertexDescription.Semantics.M);
			String s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1,
					p);
			assertTrue(s
					.equals("{\"hasZ\":true,\"hasM\":true,\"points\":[],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));

			p.add(10.0, 20.0, 30.0);
			p.add(20.0, 40.0, 60.0);
			s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1, p);
			assertTrue(s
					.equals("{\"hasZ\":true,\"hasM\":true,\"points\":[[10,20,30,null],[20,40,60,null]],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));
		}
		{
			String points = "{\"hasM\" : false, \"hasZ\" : true, \"uncle remus\" : null, \"points\" : [ [0,0,1], [0.0,10.0,1], [10.0,10.0,1], [10.0,0.0,1, 6666] ],\"spatialReference\" : {\"wkid\" : 4326}}";
			MapGeometry mp = GeometryEngine.jsonToGeometry(factory
					.createParser(points));
			MultiPoint multipoint = (MultiPoint) mp.getGeometry();
			assertTrue(multipoint.getPointCount() == 4);
			Point2D point2d;
			point2d = multipoint.getXY(0);
			assertTrue(point2d.x == 0.0 && point2d.y == 0.0);
			point2d = multipoint.getXY(1);
			assertTrue(point2d.x == 0.0 && point2d.y == 10.0);
			point2d = multipoint.getXY(2);
			assertTrue(point2d.x == 10.0 && point2d.y == 10.0);
			point2d = multipoint.getXY(3);
			assertTrue(point2d.x == 10.0 && point2d.y == 0.0);
			assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.Z));
			assertTrue(!multipoint.hasAttribute(VertexDescription.Semantics.M));
			double z = multipoint.getAttributeAsDbl(
					VertexDescription.Semantics.Z, 0, 0);
			assertTrue(z == 1);
			SpatialReference spatial_reference = mp.getSpatialReference();
			assertTrue(spatial_reference.getID() == 4326);
		}

		return bAnswer;
	}

	boolean testPolyline() throws JsonParseException, IOException {
		boolean bAnswer = true;

		Polyline polyline = new Polyline();
		polyline.startPath(-97.06138, 32.837);
		polyline.lineTo(-97.06133, 32.836);
		polyline.lineTo(-97.06124, 32.834);
		polyline.lineTo(-97.06127, 32.832);

		polyline.startPath(-97.06326, 32.759);
		polyline.lineTo(-97.06298, 32.755);

		{
			JsonParser polylinePathsWgs84Parser = factory
					.createParser(GeometryEngine.geometryToJson(
							spatialReferenceWGS84, polyline));
			MapGeometry mPolylineWGS84MP = GeometryEngine
					.jsonToGeometry(polylinePathsWgs84Parser);

			assertTrue(polyline.getPointCount() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getPointCount());
			assertTrue(polyline.getPoint(0).getX() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getPoint(0).getX());
			assertTrue(polyline.getPoint(0).getY() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getPoint(0).getY());

			assertTrue(polyline.getPathCount() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getPathCount());
			assertTrue(polyline.getSegmentCount() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getSegmentCount());
			assertTrue(polyline.getSegmentCount(0) == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getSegmentCount(0));
			assertTrue(polyline.getSegmentCount(1) == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getSegmentCount(1));

			int lastIndex = polyline.getPointCount() - 1;
			assertTrue(polyline.getPoint(lastIndex).getX() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getPoint(lastIndex).getX());
			assertTrue(polyline.getPoint(lastIndex).getY() == ((Polyline) mPolylineWGS84MP
					.getGeometry()).getPoint(lastIndex).getY());

			assertTrue(spatialReferenceWGS84.getID() == mPolylineWGS84MP
					.getSpatialReference().getID());

			if (!checkResultSpatialRef(mPolylineWGS84MP, 4326, 0)) {
				bAnswer = false;
			}
		}

		{
			Polyline p = new Polyline();
			p.addAttribute(VertexDescription.Semantics.Z);
			p.addAttribute(VertexDescription.Semantics.M);
			String s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1,
					p);
			assertTrue(s
					.equals("{\"hasZ\":true,\"hasM\":true,\"paths\":[],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));

			p.startPath(0, 0);
			p.lineTo(0, 1);
			p.startPath(2, 2);
			p.lineTo(3, 3);

			p.setAttribute(VertexDescription.Semantics.Z, 0, 0, 3);
			p.setAttribute(VertexDescription.Semantics.M, 1, 0, 5);
			s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1, p);
			assertTrue(s
					.equals("{\"hasZ\":true,\"hasM\":true,\"paths\":[[[0,0,3,null],[0,1,0,5]],[[2,2,0,null],[3,3,0,null]]],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));
		}

		{
			String paths = "{\"hasZ\" : true, \"paths\" : [ [ [0.0, 0.0,3], [0, 10.0,3], [10.0, 10.0,3, 6666], [10.0, 0.0,3, 6666] ], [ [1.0, 1,3], [1.0, 9.0,3], [9.0, 9.0,3], [1.0, 9.0,3] ] ], \"spatialReference\" : {\"wkid\" : 4326}, \"hasM\" : false}";
			MapGeometry mapGeometry = GeometryEngine.jsonToGeometry(factory
					.createParser(paths));
			Polyline p = (Polyline) mapGeometry.getGeometry();
			assertTrue(p.getPathCount() == 2);
			@SuppressWarnings("unused")
			int count = p.getPathCount();
			assertTrue(p.getPointCount() == 8);
			assertTrue(p.hasAttribute(VertexDescription.Semantics.Z));
			assertTrue(!p.hasAttribute(VertexDescription.Semantics.M));
			double z = p.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0);
			assertTrue(z == 3);
			double length = p.calculateLength2D();
			assertTrue(Math.abs(length - 54.0) <= 0.001);
			SpatialReference spatial_reference = mapGeometry
					.getSpatialReference();
			assertTrue(spatial_reference.getID() == 4326);
		}

		return bAnswer;
	}

	boolean testPolygon() throws JsonParseException, IOException {
		boolean bAnswer = true;

		Polygon polygon = new Polygon();
		polygon.startPath(-97.06138, 32.837);
		polygon.lineTo(-97.06133, 32.836);
		polygon.lineTo(-97.06124, 32.834);
		polygon.lineTo(-97.06127, 32.832);

		polygon.startPath(-97.06326, 32.759);
		polygon.lineTo(-97.06298, 32.755);

		{
			JsonParser polygonPathsWgs84Parser = factory
					.createParser(GeometryEngine.geometryToJson(
							spatialReferenceWGS84, polygon));
			MapGeometry mPolygonWGS84MP = GeometryEngine
					.jsonToGeometry(polygonPathsWgs84Parser);

			assertTrue(polygon.getPointCount() + 1 == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getPointCount());
			assertTrue(polygon.getPoint(0).getX() == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getPoint(0).getX());
			assertTrue(polygon.getPoint(0).getY() == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getPoint(0).getY());

			assertTrue(polygon.getPathCount() == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getPathCount());
			assertTrue(polygon.getSegmentCount() + 1 == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getSegmentCount());
			assertTrue(polygon.getSegmentCount(0) == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getSegmentCount(0));
			assertTrue(polygon.getSegmentCount(1) + 1 == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getSegmentCount(1));

			int lastIndex = polygon.getPointCount() - 1;
			assertTrue(polygon.getPoint(lastIndex).getX() == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getPoint(lastIndex).getX());
			assertTrue(polygon.getPoint(lastIndex).getY() == ((Polygon) mPolygonWGS84MP
					.getGeometry()).getPoint(lastIndex).getY());

			assertTrue(spatialReferenceWGS84.getID() == mPolygonWGS84MP
					.getSpatialReference().getID());

			if (!checkResultSpatialRef(mPolygonWGS84MP, 4326, 0)) {
				bAnswer = false;
			}
		}

		{
			Polygon p = new Polygon();
			p.addAttribute(VertexDescription.Semantics.Z);
			p.addAttribute(VertexDescription.Semantics.M);
			String s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1,
					p);
			assertTrue(s
					.equals("{\"hasZ\":true,\"hasM\":true,\"rings\":[],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));

			p.startPath(0, 0);
			p.lineTo(0, 1);
			p.lineTo(4, 4);
			p.startPath(2, 2);
			p.lineTo(3, 3);
			p.lineTo(7, 8);

			p.setAttribute(VertexDescription.Semantics.Z, 0, 0, 3);
			p.setAttribute(VertexDescription.Semantics.M, 1, 0, 7);
			p.setAttribute(VertexDescription.Semantics.M, 2, 0, 5);
			p.setAttribute(VertexDescription.Semantics.M, 5, 0, 5);
			s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1, p);
			assertTrue(s
					.equals("{\"hasZ\":true,\"hasM\":true,\"rings\":[[[0,0,3,null],[0,1,0,7],[4,4,0,5],[0,0,3,null]],[[2,2,0,null],[3,3,0,null],[7,8,0,5],[2,2,0,null]]],\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));
		}

		{
			// Test Import Polygon from Polygon
			String rings = "{\"hasZ\": true, \"rings\" : [ [ [0,0, 5], [0.0, 10.0, 5], [10.0,10.0, 5, 66666], [10.0,0.0, 5] ], [ [12, 12] ],  [ [13 , 17], [13 , 17] ], [ [1.0, 1.0, 5, 66666], [9.0,1.0, 5], [9.0,9.0, 5], [1.0,9.0, 5], [1.0, 1.0, 5] ] ] }";
			MapGeometry mapGeometry = GeometryEngine.jsonToGeometry(factory
					.createParser(rings));
			Polygon p = (Polygon) mapGeometry.getGeometry();
			@SuppressWarnings("unused")
			double area = p.calculateArea2D();
			@SuppressWarnings("unused")
			double length = p.calculateLength2D();
			assertTrue(p.getPathCount() == 4);
			int count = p.getPointCount();
			assertTrue(count == 15);
			assertTrue(p.hasAttribute(VertexDescription.Semantics.Z));
			assertTrue(!p.hasAttribute(VertexDescription.Semantics.M));
		}

		return bAnswer;
	}

	boolean testEnvelope() throws JsonParseException, IOException {
		boolean bAnswer = true;

		Envelope envelope = new Envelope();
		envelope.setCoords(-109.55, 25.76, -86.39, 49.94);

		{
			JsonParser envelopeWGS84Parser = factory
					.createParser(GeometryEngine.geometryToJson(
							spatialReferenceWGS84, envelope));
			MapGeometry envelopeWGS84MP = GeometryEngine
					.jsonToGeometry(envelopeWGS84Parser);
			assertTrue(envelope.isEmpty() == envelopeWGS84MP.getGeometry()
					.isEmpty());
			assertTrue(envelope.getXMax() == ((Envelope) envelopeWGS84MP
					.getGeometry()).getXMax());
			assertTrue(envelope.getYMax() == ((Envelope) envelopeWGS84MP
					.getGeometry()).getYMax());
			assertTrue(envelope.getXMin() == ((Envelope) envelopeWGS84MP
					.getGeometry()).getXMin());
			assertTrue(envelope.getYMin() == ((Envelope) envelopeWGS84MP
					.getGeometry()).getYMin());
			assertTrue(spatialReferenceWGS84.getID() == envelopeWGS84MP
					.getSpatialReference().getID());
			if (!checkResultSpatialRef(envelopeWGS84MP, 4326, 0)) {
				bAnswer = false;
			}
		}

		{// export
			Envelope e = new Envelope();
			e.addAttribute(VertexDescription.Semantics.Z);
			e.addAttribute(VertexDescription.Semantics.M);
			String s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1,
					e);
			assertTrue(s
					.equals("{\"xmin\":null,\"ymin\":null,\"xmax\":null,\"ymax\":null,\"zmin\":null,\"zmax\":null,\"mmin\":null,\"mmax\":null,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));

			e.setCoords(0, 1, 2, 3);

			Envelope1D z = new Envelope1D();
			Envelope1D m = new Envelope1D();
			z.setCoords(5, 7);
			m.setCoords(11, 13);

			e.setInterval(VertexDescription.Semantics.Z, 0, z);
			e.setInterval(VertexDescription.Semantics.M, 0, m);
			s = GeometryEngine.geometryToJson(spatialReferenceWebMerc1, e);
			assertTrue(s
					.equals("{\"xmin\":0,\"ymin\":1,\"xmax\":2,\"ymax\":3,\"zmin\":5,\"zmax\":7,\"mmin\":11,\"mmax\":13,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}"));
		}

		{// import
			String s = "{\"xmin\":0.0,\"ymin\":1.0,\"xmax\":2.0,\"ymax\":3.0,\"zmin\":5.0,\"zmax\":7.0,\"mmin\":11.0,\"mmax\":13.0,\"spatialReference\":{\"wkid\":102100,\"latestWkid\":3857}}";
			JsonParser parser = factory.createParser(s);
			MapGeometry map_env = GeometryEngine.jsonToGeometry(parser);
			Envelope env = (Envelope) map_env.getGeometry();
			Envelope1D z = env.queryInterval(VertexDescription.Semantics.Z, 0);
			Envelope1D m = env.queryInterval(VertexDescription.Semantics.M, 0);
			assertTrue(z.vmin == 5.0);
			assertTrue(z.vmax == 7.0);
			assertTrue(m.vmin == 11.0);
			assertTrue(m.vmax == 13.0);
		}

		{
			String s = "{ \"zmin\" : 33, \"xmin\" : -109.55, \"zmax\" : 53, \"ymin\" : 25.76, \"xmax\" : -86.39, \"ymax\" : 49.94, \"mmax\" : 13}";
			JsonParser parser = factory.createParser(s);
			MapGeometry map_env = GeometryEngine.jsonToGeometry(parser);
			Envelope env = (Envelope) map_env.getGeometry();
			Envelope2D e = new Envelope2D();
			env.queryEnvelope2D(e);
			assertTrue(e.xmin == -109.55 && e.ymin == 25.76 && e.xmax == -86.39
					&& e.ymax == 49.94);

			Envelope1D e1D;
			assertTrue(env.hasAttribute(VertexDescription.Semantics.Z));
			e1D = env.queryInterval(VertexDescription.Semantics.Z, 0);
			assertTrue(e1D.vmin == 33 && e1D.vmax == 53);

			assertTrue(!env.hasAttribute(VertexDescription.Semantics.M));
		}

		return bAnswer;
	}

	boolean testCR181369() throws JsonParseException, IOException {
		// CR181369
		boolean bAnswer = true;

		String jsonStringPointAndWKT = "{\"x\":10.0,\"y\":20.0,\"spatialReference\":{\"wkt\" : \"PROJCS[\\\"NAD83_UTM_zone_15N\\\",GEOGCS[\\\"GCS_North_American_1983\\\",DATUM[\\\"D_North_American_1983\\\",SPHEROID[\\\"GRS_1980\\\",6378137.0,298.257222101]],PRIMEM[\\\"Greenwich\\\",0.0],UNIT[\\\"Degree\\\",0.0174532925199433]],PROJECTION[\\\"Transverse_Mercator\\\"],PARAMETER[\\\"false_easting\\\",500000.0],PARAMETER[\\\"false_northing\\\",0.0],PARAMETER[\\\"central_meridian\\\",-93.0],PARAMETER[\\\"scale_factor\\\",0.9996],PARAMETER[\\\"latitude_of_origin\\\",0.0],UNIT[\\\"Meter\\\",1.0]]\"} }";
		JsonParser jsonParserPointAndWKT = factory
				.createParser(jsonStringPointAndWKT);
		MapGeometry mapGeom2 = GeometryEngine
				.jsonToGeometry(jsonParserPointAndWKT);
		String jsonStringPointAndWKT2 = GeometryEngine.geometryToJson(
				mapGeom2.getSpatialReference(), mapGeom2.getGeometry());
		JsonParser jsonParserPointAndWKT2 = factory
				.createParser(jsonStringPointAndWKT2);
		MapGeometry mapGeom3 = GeometryEngine
				.jsonToGeometry(jsonParserPointAndWKT2);
		assertTrue(((Point) mapGeom2.getGeometry()).getX() == ((Point) mapGeom3
				.getGeometry()).getX());
		assertTrue(((Point) mapGeom2.getGeometry()).getY() == ((Point) mapGeom3
				.getGeometry()).getY());

		String s1 = mapGeom2.getSpatialReference().getText();
		String s2 = mapGeom3.getSpatialReference().getText();
		assertTrue(s1.equals(s2));

		int id2 = mapGeom2.getSpatialReference().getID();
		int id3 = mapGeom3.getSpatialReference().getID();
		assertTrue(id2 == id3);
		if (!checkResultSpatialRef(mapGeom3, mapGeom2.getSpatialReference()
				.getID(), 0)) {
			bAnswer = false;
		}
		return bAnswer;
	}

	boolean checkResultSpatialRef(MapGeometry mapGeometry, int expectWki1,
			int expectWki2) {
		SpatialReference sr = mapGeometry.getSpatialReference();
		String Wkt = sr.getText();
		int wki1 = sr.getLatestID();
		if (!(wki1 == expectWki1 || wki1 == expectWki2))
			return false;
		if (!(Wkt != null && Wkt.length() > 0))
			return false;
		SpatialReference sr2 = SpatialReference.create(Wkt);
		int wki2 = sr2.getID();
		if (expectWki2 > 0) {
			if (!(wki2 == expectWki1 || wki2 == expectWki2))
				return false;
		} else {
			if (!(wki2 == expectWki1))
				return false;
		}
		return true;
	}
}
