package com.esri.core.geometry;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Scanner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.representation.Form;

public class GeometryUtils {
	static WebResource service4Proj;
	static String url4Proj = "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Geometry/GeometryServer/project";
	static WebResource service4Simplify;
	static String url4Simplify = "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Geometry/GeometryServer/simplify";
	static WebResource service4Relation;
	static String url4Relation = "http://sampleserver1.arcgisonline.com/arcgis/rest/services/Geometry/GeometryServer/relation";

	static {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		service4Proj = client.resource(getBaseURI4Proj());
		service4Simplify = client.resource(getBaseURI4Simplify());
		service4Relation = client.resource(url4Relation);
	}

	private static URI getBaseURI4Proj() {
		return UriBuilder.fromUri(url4Proj).build();
	}

	private static URI getBaseURI4Simplify() {
		return UriBuilder.fromUri(url4Simplify).build();
	}

	public static Geometry getGeometryForProjectFromRestWS(int srIn, int srOut,
			Geometry geomIn) {
		String jsonStr4Geom = GeometryEngine.geometryToJson(srIn, geomIn);
		String jsonStrNew = "{\"geometryType\":\"" + getGeometryType(geomIn)
				+ "\",\"geometries\":[" + jsonStr4Geom + "]}";

		Form f = new Form();
		f.add("inSR", srIn);
		f.add("outSR", srOut);
		f.add("geometries", jsonStrNew);
		f.add("f", "json");

		ClientResponse response = service4Proj.type(
				MediaType.APPLICATION_FORM_URLENCODED).post(
				ClientResponse.class, f);
		@SuppressWarnings("unused")
		boolean isOK = response.getClientResponseStatus() == ClientResponse.Status.OK;
		Object obj = response.getEntity(String.class);
		String jsonStr = obj.toString();
		int idx2 = jsonStr.lastIndexOf("]");
		int idx1 = jsonStr.indexOf("[");
		if (idx1 == -1 || idx2 == -1)
			return null;
		String jsonStrGeom = jsonStr.substring(idx1 + 1, idx2);
		Geometry geometryObj = getGeometryFromJSon(jsonStrGeom);
		return geometryObj;
	}

	public static Geometry getGeometryForSimplifyFromRestWS(int sr,
			Geometry geomIn) {
		String jsonStr4Geom = GeometryEngine.geometryToJson(
				SpatialReference.create(sr), geomIn);
		String jsonStrNew = "{\"geometryType\":\"" + getGeometryType(geomIn)
				+ "\",\"geometries\":[" + jsonStr4Geom + "]}";

		Form f = new Form();
		f.add("sr", sr);
		f.add("geometries", jsonStrNew);
		f.add("f", "json");

		ClientResponse response = service4Simplify.type(
				MediaType.APPLICATION_FORM_URLENCODED).post(
				ClientResponse.class, f);
		@SuppressWarnings("unused")
		boolean isOK = response.getClientResponseStatus() == ClientResponse.Status.OK;
		Object obj = response.getEntity(String.class);
		String jsonStr = obj.toString();
		int idx2 = jsonStr.lastIndexOf("]");
		int idx1 = jsonStr.indexOf("[");
		if (idx1 == -1 || idx2 == -1)
			return null;
		String jsonStrGeom = jsonStr.substring(idx1 + 1, idx2);
		Geometry geometryObj = getGeometryFromJSon(jsonStrGeom);
		return geometryObj;
	}

	static String getGeometryType(Geometry geomIn) {
		// there are five types: esriGeometryPoint
		// esriGeometryMultipoint
		// esriGeometryPolyline
		// esriGeometryPolygon
		// esriGeometryEnvelope
		if (geomIn instanceof Point)
			return "esriGeometryPoint";
		if (geomIn instanceof MultiPoint)
			return "esriGeometryMultipoint";
		if (geomIn instanceof Polyline)
			return "esriGeometryPolyline";
		if (geomIn instanceof Polygon)
			return "esriGeometryPolygon";
		if (geomIn instanceof Envelope)
			return "esriGeometryEnvelope";
		else
			return null;
	}

	static Geometry getGeometryFromJSon(String jsonStr) {
		JsonFactory jf = new JsonFactory();

		try {
			JsonParser jp = jf.createJsonParser(jsonStr);
			jp.nextToken();
			Geometry geom = GeometryEngine.jsonToGeometry(jp).getGeometry();
			return geom;
		} catch (Exception ex) {
			return null;
		}
	}

	public static void testMultiplePath(MultiPath mp1, MultiPath mp2) {
		int count1 = mp1.getPointCount();
		int count2 = mp2.getPointCount();

		System.out.println("From Rest vertices count: " + count1);
		System.out.println("From Borg count: " + count2);
		// Assert.assertTrue(count1==count2);

		int len = mp1.getPointCount();

		for (int i = 0; i < len; i++) {
			Point p = mp1.getPoint(i);
			Point p2 = mp2.getPoint(i);
			System.out.println("for rest: [" + p.getX() + "," + p.getY() + "]");
			System.out.println("for proj: [" + p2.getX() + "," + p2.getY()
					+ "]");
			@SuppressWarnings("unused")
			double deltaX = p2.getX() - p.getX();
			@SuppressWarnings("unused")
			double deltaY = p2.getY() - p.getY();

			// Assert.assertTrue(deltaX<1e-7);
			// Assert.assertTrue(deltaY<1e-7);
		}
	}

	public enum SpatialRelationType {
		esriGeometryRelationCross, esriGeometryRelationDisjoint, esriGeometryRelationIn, esriGeometryRelationInteriorIntersection, esriGeometryRelationIntersection, esriGeometryRelationLineCoincidence, esriGeometryRelationLineTouch, esriGeometryRelationOverlap, esriGeometryRelationPointTouch, esriGeometryRelationTouch, esriGeometryRelationWithin, esriGeometryRelationRelation
	}

	public static boolean isRelationTrue(Geometry geometry1,
			Geometry geometry2, SpatialReference sr,
			SpatialRelationType relation, String relationParam) {
		String jsonStr4Geom1 = getJSonStringFromGeometry(geometry1, sr);
		String jsonStr4Geom2 = getJSonStringFromGeometry(geometry2, sr);

		Form f = new Form();
		f.add("sr", sr.getID());
		f.add("geometries1", jsonStr4Geom1);
		f.add("geometries2", jsonStr4Geom2);

		@SuppressWarnings("unused")
		String enumName = relation.name();

		f.add("relation", relation.name());
		f.add("f", "json");
		f.add("relationParam", relationParam);

		ClientResponse response = service4Relation.type(
				MediaType.APPLICATION_FORM_URLENCODED).post(
				ClientResponse.class, f);
		@SuppressWarnings("unused")
		boolean isOK = response.getClientResponseStatus() == ClientResponse.Status.OK;
		Object obj = response.getEntity(String.class);
		String jsonStr = obj.toString();
		int idx = jsonStr
				.lastIndexOf("geometry1Index\":0,\"geometry2Index\":0");

		if (idx == -1) {
			return false;
		} else {
			return true;
		}
	}

	static String getJSonStringFromGeometry(Geometry geomIn, SpatialReference sr) {
		String jsonStr4Geom = GeometryEngine.geometryToJson(sr, geomIn);
		String jsonStrNew = "{\"geometryType\":\"" + getGeometryType(geomIn)
				+ "\",\"geometries\":[" + jsonStr4Geom + "]}";
		return jsonStrNew;
	}

	public static Geometry loadFromTextFileDbg(String textFileName)
			throws FileNotFoundException {
		String fullPath = textFileName;
		// string fullCSVPathName = System.IO.Path.Combine( directoryPath ,
		// CsvFileName);
		File fileInfo = new File(fullPath);

		Scanner scanner = new Scanner(fileInfo);

		Geometry geom = null;

		// grab first line
		String line = scanner.nextLine();
		String geomTypeString = line.substring(1);
		if (geomTypeString.equalsIgnoreCase("polygon"))
			geom = new Polygon();
		else if (geomTypeString.equalsIgnoreCase("polyline"))
			geom = new Polyline();
		else if (geomTypeString.equalsIgnoreCase("multipoint"))
			geom = new MultiPoint();
		else if (geomTypeString.equalsIgnoreCase("point"))
			geom = new Point();

		while (line.startsWith("*"))
			if (scanner.hasNextLine())
				line = scanner.nextLine();

		int j = 0;
		Geometry.Type geomType = geom.getType();
		while (scanner.hasNextLine()) {
			String[] parsedLine = line.split("\\s+");
			double xVal = Double.parseDouble(parsedLine[0]);
			double yVal = Double.parseDouble(parsedLine[1]);
			if (j == 0
					&& (geomType == Geometry.Type.Polygon || geomType == Geometry.Type.Polyline))
				((MultiPath) geom).startPath(xVal, yVal);
			else {
				if (geomType == Geometry.Type.Polygon
						|| geomType == Geometry.Type.Polyline)
					((MultiPath) geom).lineTo(xVal, yVal);
				else if (geomType == Geometry.Type.MultiPoint)
					((MultiPoint) geom).add(xVal, yVal);
				// else if(geomType == Geometry.Type.Point)
				// Point geom = null;//new Point(xVal, yVal);
			}
			j++;
			line = scanner.nextLine();
		}

		scanner.close();

		return geom;
	}
}
