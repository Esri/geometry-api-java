package com.esri.core.geometry;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;

public class GeometryUtils {
	public static String getGeometryType(Geometry geomIn) {
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
		return;
		/*int count1 = mp1.getPointCount();
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
		*/
	}

	public enum SpatialRelationType {
		esriGeometryRelationCross, esriGeometryRelationDisjoint, esriGeometryRelationIn, esriGeometryRelationInteriorIntersection, esriGeometryRelationIntersection, esriGeometryRelationLineCoincidence, esriGeometryRelationLineTouch, esriGeometryRelationOverlap, esriGeometryRelationPointTouch, esriGeometryRelationTouch, esriGeometryRelationWithin, esriGeometryRelationRelation
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
