/*
 Copyright 1995-2013 Esri

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

import com.esri.core.geometry.VertexDescription.Semantics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

class OperatorImportFromGeoJsonLocal extends OperatorImportFromGeoJson {
	@Override
	public MapGeometry execute(int importFlags, Geometry.Type type,
			String geoJsonString, ProgressTracker progress_tracker)
			throws JSONException {
		JSONObject geoJsonObject = new JSONObject(geoJsonString);
		Geometry geometry = importGeometryFromGeoJson_(importFlags, type,
				geoJsonObject);
		SpatialReference spatialReference = importSpatialReferenceFromGeoJson_(geoJsonObject);
		MapGeometry mapGeometry = new MapGeometry(geometry, spatialReference);
		return mapGeometry;
	}
	
	static JSONArray getJSONArray(JSONObject obj, String name) throws JSONException {
		if (obj.get(name) == JSONObject.NULL)
			return new JSONArray();
		else
			return obj.getJSONArray(name);
	}

	@Override
	public MapOGCStructure executeOGC(int import_flags, String geoJsonString,
			ProgressTracker progress_tracker) throws JSONException {
		JSONObject geoJsonObject = new JSONObject(geoJsonString);
		ArrayList<OGCStructure> structureStack = new ArrayList<OGCStructure>(0);
		ArrayList<JSONObject> objectStack = new ArrayList<JSONObject>(0);
		AttributeStreamOfInt32 indices = new AttributeStreamOfInt32(0);
		AttributeStreamOfInt32 numGeometries = new AttributeStreamOfInt32(0);

		OGCStructure root = new OGCStructure();
		root.m_structures = new ArrayList<OGCStructure>(0);
		structureStack.add(root); // add dummy root
		objectStack.add(geoJsonObject);
		indices.add(0);
		numGeometries.add(1);

		while (!objectStack.isEmpty()) {
			if (indices.getLast() == numGeometries.getLast()) {
				structureStack.remove(structureStack.size() - 1);
				indices.removeLast();
				numGeometries.removeLast();
				continue;
			}

			OGCStructure lastStructure = structureStack.get(structureStack
					.size() - 1);
			JSONObject lastObject = objectStack.get(objectStack.size() - 1);
			objectStack.remove(objectStack.size() - 1);
			indices.write(indices.size() - 1, indices.getLast() + 1);
			String typeString = lastObject.getString("type");

			if (typeString.equalsIgnoreCase("GeometryCollection")) {
				OGCStructure next = new OGCStructure();
				next.m_type = 7;
				next.m_structures = new ArrayList<OGCStructure>(0);
				lastStructure.m_structures.add(next);
				structureStack.add(next);

				JSONArray geometries = getJSONArray(lastObject, "geometries");
				indices.add(0);
				numGeometries.add(geometries.length());

				for (int i = geometries.length() - 1; i >= 0; i--)
					objectStack.add(geometries.getJSONObject(i));
			} else {
				int ogcType;

				if (typeString.equalsIgnoreCase("Point"))
					ogcType = 1;
				else if (typeString.equalsIgnoreCase("LineString"))
					ogcType = 2;
				else if (typeString.equalsIgnoreCase("Polygon"))
					ogcType = 3;
				else if (typeString.equalsIgnoreCase("MultiPoint"))
					ogcType = 4;
				else if (typeString.equalsIgnoreCase("MultiLineString"))
					ogcType = 5;
				else if (typeString.equalsIgnoreCase("MultiPolygon"))
					ogcType = 6;
				else
					throw new UnsupportedOperationException();

				Geometry geometry = importGeometryFromGeoJson_(import_flags,
						Geometry.Type.Unknown, lastObject);

				OGCStructure leaf = new OGCStructure();
				leaf.m_type = ogcType;
				leaf.m_geometry = geometry;
				lastStructure.m_structures.add(leaf);
			}
		}

		MapOGCStructure mapOGCStructure = new MapOGCStructure();
		mapOGCStructure.m_ogcStructure = root;
		mapOGCStructure.m_spatialReference = importSpatialReferenceFromGeoJson_(geoJsonObject);

		return mapOGCStructure;
	}

	private static SpatialReference importSpatialReferenceFromGeoJson_(
			JSONObject crsJSONObject) throws JSONException {
		String wkidString = crsJSONObject.optString("crs", "");

		if (wkidString.equals("")) {
			return SpatialReference.create(4326);
		}

		// wkidString will be of the form "EPSG:#" where # is an integer, the
		// EPSG ID.
		// If the ID is below 32,767, then the EPSG ID will agree with the
		// well-known (WKID).

		if (wkidString.length() <= 5) {
			throw new IllegalArgumentException();
		}

		// Throws a JSON exception if this cannot appropriately be converted to
		// an integer.
		int wkid = Integer.valueOf(wkidString.substring(5)).intValue();

		return SpatialReference.create(wkid);
	}

	private static Geometry importGeometryFromGeoJson_(int importFlags,
			Geometry.Type type, JSONObject geometryJSONObject)
			throws JSONException {
		String typeString = geometryJSONObject.getString("type");
		JSONArray coordinateArray = getJSONArray(geometryJSONObject, "coordinates");

		if (typeString.equalsIgnoreCase("MultiPolygon")) {
			if (type != Geometry.Type.Polygon && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return polygonTaggedText_(true, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("MultiLineString")) {
			if (type != Geometry.Type.Polyline && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return lineStringTaggedText_(true, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("MultiPoint")) {
			if (type != Geometry.Type.MultiPoint
					&& type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return multiPointTaggedText_(importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("Polygon")) {
			if (type != Geometry.Type.Polygon && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return polygonTaggedText_(false, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("LineString")) {
			if (type != Geometry.Type.Polyline && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return lineStringTaggedText_(false, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("Point")) {
			if (type != Geometry.Type.Point && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return pointTaggedText_(importFlags, coordinateArray);
		} else {
			return null;
		}
	}

	private static Geometry polygonTaggedText_(boolean bMultiPolygon,
			int importFlags, JSONArray coordinateArray) throws JSONException {
		MultiPath multiPath;
		MultiPathImpl multiPathImpl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;
		AttributeStreamOfInt32 paths;
		AttributeStreamOfInt8 path_flags;

		position = (AttributeStreamOfDbl) AttributeStreamBase
				.createDoubleStream(0);
		paths = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(
				1, 0);
		path_flags = (AttributeStreamOfInt8) AttributeStreamBase
				.createByteStream(1, (byte) 0);

		multiPath = new Polygon();
		multiPathImpl = (MultiPathImpl) multiPath._getImpl();

		int pointCount;

		if (bMultiPolygon) {
			pointCount = multiPolygonText_(zs, ms, position, paths, path_flags,
					coordinateArray);
		} else {
			pointCount = polygonText_(zs, ms, position, paths, path_flags, 0,
					coordinateArray);
		}

		if (pointCount != 0) {
			assert (2 * pointCount == position.size());
			multiPathImpl.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);
			multiPathImpl.setPathStreamRef(paths);
			multiPathImpl.setPathFlagsStreamRef(path_flags);

			if (zs != null) {
				multiPathImpl.setAttributeStreamRef(
						VertexDescription.Semantics.Z, zs);
			}

			if (ms != null) {
				multiPathImpl.setAttributeStreamRef(
						VertexDescription.Semantics.M, ms);
			}

			multiPathImpl.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);

			if (!InternalUtils.isClockwiseRing(multiPathImpl, 0)) {
				multiPathImpl.reverseAllPaths();
			}
		}

		if ((importFlags & (int) GeoJsonImportFlags.geoJsonImportNonTrusted) == 0) {
			multiPathImpl.setIsSimple(MultiPathImpl.GeometryXSimple.Weak, 0.0,
					false);
		}

		multiPathImpl.setDirtyOGCFlags(false);

		return multiPath;
	}

	private static Geometry lineStringTaggedText_(boolean bMultiLineString,
			int importFlags, JSONArray coordinateArray) throws JSONException {
		MultiPath multiPath;
		MultiPathImpl multiPathImpl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;
		AttributeStreamOfInt32 paths;
		AttributeStreamOfInt8 path_flags;

		position = (AttributeStreamOfDbl) AttributeStreamBase
				.createDoubleStream(0);
		paths = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(
				1, 0);
		path_flags = (AttributeStreamOfInt8) AttributeStreamBase
				.createByteStream(1, (byte) 0);

		multiPath = new Polyline();
		multiPathImpl = (MultiPathImpl) multiPath._getImpl();

		int pointCount;

		if (bMultiLineString) {
			pointCount = multiLineStringText_(zs, ms, position, paths,
					path_flags, coordinateArray);
		} else {
			pointCount = lineStringText_(false, zs, ms, position, paths,
					path_flags, coordinateArray);
		}

		if (pointCount != 0) {
			assert (2 * pointCount == position.size());
			multiPathImpl.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);
			multiPathImpl.setPathStreamRef(paths);
			multiPathImpl.setPathFlagsStreamRef(path_flags);

			if (zs != null) {
				multiPathImpl.setAttributeStreamRef(
						VertexDescription.Semantics.Z, zs);
			}

			if (ms != null) {
				multiPathImpl.setAttributeStreamRef(
						VertexDescription.Semantics.M, ms);
			}

			multiPathImpl.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);
		}

		return multiPath;
	}

	private static Geometry multiPointTaggedText_(int importFlags,
			JSONArray coordinateArray) throws JSONException {
		MultiPoint multiPoint;
		MultiPointImpl multiPointImpl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;

		position = (AttributeStreamOfDbl) AttributeStreamBase
				.createDoubleStream(0);

		multiPoint = new MultiPoint();
		multiPointImpl = (MultiPointImpl) multiPoint._getImpl();

		int pointCount = multiPointText_(zs, ms, position, coordinateArray);

		if (pointCount != 0) {
			assert (2 * pointCount == position.size());
			multiPointImpl.resize(pointCount);
			multiPointImpl.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);

			multiPointImpl.notifyModified(MultiPointImpl.DirtyFlags.DirtyAll);
		}

		return multiPoint;
	}

	private static Geometry pointTaggedText_(int importFlags,
			JSONArray coordinateArray) throws JSONException {
		Point point = new Point();

		int length = coordinateArray.length();

		if (length == 0) {
			point.setEmpty();
			return point;
		}

		point.setXY(getDouble_(coordinateArray, 0),
				getDouble_(coordinateArray, 1));

		return point;
	}

	private static int multiPolygonText_(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			JSONArray coordinateArray) throws JSONException {
		// At start of MultiPolygonText

		int totalPointCount = 0;
		int length = coordinateArray.length();

		if (length == 0)
			return totalPointCount;

		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {
				// Entry should be a JSONArray representing a polygon, but it is
				// not a JSONArray.
				throw new IllegalArgumentException("");
			}

			// At start of PolygonText

			totalPointCount = polygonText_(zs, ms, position, paths, path_flags,
					totalPointCount, subArray);
		}

		return totalPointCount;
	}

	private static int multiLineStringText_(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			JSONArray coordinateArray) throws JSONException {
		// At start of MultiLineStringText

		int totalPointCount = 0;
		int length = coordinateArray.length();

		if (length == 0)
			return totalPointCount;

		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {
				// Entry should be a JSONArray representing a line string, but
				// it is not a JSONArray.
				throw new IllegalArgumentException("");
			}

			// At start of LineStringText

			totalPointCount += lineStringText_(false, zs, ms, position, paths,
					path_flags, subArray);
		}

		return totalPointCount;
	}

	private static int multiPointText_(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			JSONArray coordinateArray) throws JSONException {
		// At start of MultiPointText

		int pointCount = 0;

		for (int current = 0; current < coordinateArray.length(); current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {
				// Entry should be a JSONArray representing a point, but it is
				// not a JSONArray.
				throw new IllegalArgumentException("");
			}

			pointCount += pointText_(zs, ms, position, subArray);
		}

		return pointCount;
	}

	private static int polygonText_(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			int totalPointCount, JSONArray coordinateArray)
			throws JSONException {
		// At start of PolygonText

		int length = coordinateArray.length();

		if (length == 0) {
			return totalPointCount;
		}

		boolean bFirstLineString = true;

		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {
				// Entry should be a JSONArray representing a line string, but
				// it is not a JSONArray.
				throw new IllegalArgumentException("");
			}

			// At start of LineStringText

			int pointCount = lineStringText_(true, zs, ms, position, paths,
					path_flags, subArray);

			if (pointCount != 0) {
				if (bFirstLineString) {
					bFirstLineString = false;
					path_flags.setBits(path_flags.size() - 2,
							(byte) PathFlags.enumOGCStartPolygon);
				}

				path_flags.setBits(path_flags.size() - 2,
						(byte) PathFlags.enumClosed);
				totalPointCount += pointCount;
			}
		}

		return totalPointCount;
	}

	private static int lineStringText_(boolean bRing, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			JSONArray coordinateArray) throws JSONException {
		// At start of LineStringText

		int pointCount = 0;
		int length = coordinateArray.length();

		if (length == 0)
			return pointCount;

		boolean bStartPath = true;
		double startX = NumberUtils.TheNaN;
		double startY = NumberUtils.TheNaN;
		double startZ = NumberUtils.TheNaN;
		double startM = NumberUtils.TheNaN;

		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {
				// Entry should be a JSONArray representing a single point, but
				// it is not a JSONArray.
				throw new IllegalArgumentException("");
			}

			// At start of x

			double x = getDouble_(subArray, 0);
			double y = getDouble_(subArray, 1);
			double z = NumberUtils.TheNaN;
			double m = NumberUtils.TheNaN;

			boolean bAddPoint = true;

			if (bRing && pointCount >= 2 && current == length - 1) {
				// If the last point in the ring is not equal to the start
				// point, then let's add it.

				if ((startX == x || (NumberUtils.isNaN(startX) && NumberUtils
						.isNaN(x)))
						&& (startY == y || (NumberUtils.isNaN(startY) && NumberUtils
								.isNaN(y)))) {
					bAddPoint = false;
				}
			}

			if (bAddPoint) {
				if (bStartPath) {
					bStartPath = false;
					startX = x;
					startY = y;
					startZ = z;
					startM = m;
				}

				pointCount++;
				addToStreams_(zs, ms, position, x, y, z, m);
			}
		}

		if (pointCount == 1) {
			pointCount++;
			addToStreams_(zs, ms, position, startX, startY, startZ, startM);
		}

		paths.add(position.size() / 2);
		path_flags.add((byte) 0);

		return pointCount;
	}

	private static int pointText_(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			JSONArray coordinateArray) throws JSONException {
		// At start of PointText

		int length = coordinateArray.length();

		if (length == 0)
			return 0;

		// At start of x

		double x = getDouble_(coordinateArray, 0);
		double y = getDouble_(coordinateArray, 1);
		double z = NumberUtils.TheNaN;
		double m = NumberUtils.TheNaN;

		addToStreams_(zs, ms, position, x, y, z, m);

		return 1;
	}

	private static void addToStreams_(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position, double x,
			double y, double z, double m) {
		position.add(x);
		position.add(y);

		if (zs != null)
			zs.add(z);

		if (ms != null)
			ms.add(m);
	}

	private static double getDouble_(JSONArray coordinateArray, int index)
			throws JSONException {
		if (index < 0 || index >= coordinateArray.length()) {
			throw new IllegalArgumentException("");
		}

		if (coordinateArray.isNull(index)) {
			return NumberUtils.TheNaN;
		}

		if (coordinateArray.optDouble(index, NumberUtils.TheNaN) != NumberUtils.TheNaN) {
			return coordinateArray.getDouble(index);
		}

		throw new IllegalArgumentException("");
	}
}
