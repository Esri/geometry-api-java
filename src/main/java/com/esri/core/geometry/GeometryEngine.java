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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonParser;

/**
 * Provides services that operate on geometry instances. The methods of GeometryEngine class call corresponding OperatorXXX classes.
 * Consider using OperatorXXX classes directly as they often provide more functionality and better performance. For example, some Operators accept
 * GeometryCursor class that could be implemented to wrap a feature cursor and make it feed geometries directly into an Operator.
 * Also, some operators provide a way to accelerate an operation by using Operator.accelerateGeometry method. 
 */
public class GeometryEngine {

	private static OperatorFactoryLocal factory = OperatorFactoryLocal
			.getInstance();


	/**
	 * Imports the MapGeometry from its JSON representation. M and Z values are
	 * not imported from JSON representation.
	 * 
	 * See OperatorImportFromJson.
	 * 
	 * @param json
	 *            The JSON representation of the geometry (with spatial
	 *            reference).
	 * @return The MapGeometry instance containing the imported geometry and its
	 *         spatial reference.
	 */
	public static MapGeometry jsonToGeometry(JsonParser json) {
		MapGeometry geom = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, new JsonParserReader(json));
		return geom;
	}

	/**
	 * Imports the MapGeometry from its JSON representation. M and Z values are
	 * not imported from JSON representation.
	 * 
	 * See OperatorImportFromJson.
	 * 
	 * @param json
	 *            The JSON representation of the geometry (with spatial
	 *            reference).
	 * @return The MapGeometry instance containing the imported geometry and its
	 *         spatial reference.
	 */
	public static MapGeometry jsonToGeometry(JsonReader json) {
		MapGeometry geom = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, json);
		return geom;
	}
	
	/**
	 * Imports the MapGeometry from its JSON representation. M and Z values are
	 * not imported from JSON representation.
	 * 
	 * See OperatorImportFromJson.
	 * 
	 * @param json
	 *            The JSON representation of the geometry (with spatial
	 *            reference).
	 * @return The MapGeometry instance containing the imported geometry and its
	 *         spatial reference.
	 */
	public static MapGeometry jsonToGeometry(String json) {
		MapGeometry geom = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, json);
		return geom;
	}
	
	/**
	 * Exports the specified geometry instance to it's JSON representation.
	 * 
	 * See OperatorExportToJson.
	 * 
	 * @see GeometryEngine#geometryToJson(SpatialReference spatialiReference,
	 *      Geometry geometry)
	 * @param wkid
	 *            The spatial reference Well Known ID to be used for the JSON
	 *            representation.
	 * @param geometry
	 *            The geometry to be exported to JSON.
	 * @return The JSON representation of the specified Geometry.
	 */
	public static String geometryToJson(int wkid, Geometry geometry) {
		return GeometryEngine.geometryToJson(
				wkid > 0 ? SpatialReference.create(wkid) : null, geometry);
	}

	/**
	 * Exports the specified geometry instance to it's JSON representation. M
	 * and Z values are not imported from JSON representation.
	 * 
	 * See OperatorExportToJson.
	 * 
	 * @param spatialReference
	 *            The spatial reference of associated object.
	 * @param geometry
	 *            The geometry.
	 * @return The JSON representation of the specified geometry.
	 */
	public static String geometryToJson(SpatialReference spatialReference,
			Geometry geometry) {
		OperatorExportToJson exporter = (OperatorExportToJson) factory
				.getOperator(Operator.Type.ExportToJson);

		return exporter.execute(spatialReference, geometry);
	}

    public static String geometryToGeoJson(Geometry geometry) {
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory
                .getOperator(Operator.Type.ExportToGeoJson);

        return exporter.execute(geometry);
    }

	/**
	 * Imports the MapGeometry from its JSON representation. M and Z values are
	 * not imported from JSON representation.
	 * 
	 * See OperatorImportFromJson.
	 * 
	 * @param json
	 *            The JSON representation of the geometry (with spatial
	 *            reference).
	 * @return The MapGeometry instance containing the imported geometry and its
	 *         spatial reference.
	 */
	public static MapGeometry geoJsonToGeometry(String json, int importFlags, Geometry.Type type) {
		MapGeometry geom = OperatorImportFromGeoJson.local().execute(importFlags, type, json, null);
		return geom;
	}

	/**
	 * Exports the specified geometry instance to its GeoJSON representation.
	 *
	 * See OperatorExportToGeoJson.
	 *
	 * @see GeometryEngine#geometryToGeoJson(SpatialReference spatialReference,
	 *      Geometry geometry)
	 *
	 * @param wkid
	 *            The spatial reference Well Known ID to be used for the GeoJSON
	 *            representation.
	 * @param geometry
	 *            The geometry to be exported to GeoJSON.
	 * @return The GeoJSON representation of the specified geometry.
	 */
	public static String geometryToGeoJson(int wkid, Geometry geometry) {
		return GeometryEngine.geometryToGeoJson(wkid > 0 ? SpatialReference.create(wkid) : null, geometry);
	}

	/**
	 * Exports the specified geometry instance to it's JSON representation.
	 *
	 * See OperatorImportFromGeoJson.
	 *
	 * @param spatialReference
	 *            The spatial reference of associated object.
	 * @param geometry
	 *            The geometry.
	 * @return The GeoJSON representation of the specified geometry.
	 */
	public static String geometryToGeoJson(SpatialReference spatialReference, Geometry geometry) {
		OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);

		return exporter.execute(spatialReference, geometry);
	}

	/**
	 * Imports geometry from the ESRI shape file format.
	 * 
	 * See OperatorImportFromESRIShape.
	 * 
	 * @param esriShapeBuffer
	 *            The buffer containing geometry in the ESRI shape file format.
	 * @param geometryType
	 *            The required type of the Geometry to be imported. Use
	 *            Geometry.Type.Unknown if the geometry type needs to be
	 *            determined from the buffer content.
	 * @return The geometry or null if the buffer contains null shape.
	 * @throws GeometryException
	 *             when the geometryType is not Geometry.Type.Unknown and the
	 *             buffer contains geometry that cannot be converted to the
	 *             given geometryType. or the buffer is corrupt. Another
	 *             exception possible is IllegalArgumentsException.
	 */
	public static Geometry geometryFromEsriShape(byte[] esriShapeBuffer,
			Geometry.Type geometryType) {
		OperatorImportFromESRIShape op = (OperatorImportFromESRIShape) factory
				.getOperator(Operator.Type.ImportFromESRIShape);
		return op
				.execute(
						ShapeImportFlags.ShapeImportNonTrusted,
						geometryType,
						ByteBuffer.wrap(esriShapeBuffer).order(
								ByteOrder.LITTLE_ENDIAN));
	}

	/**
	 * Exports geometry to the ESRI shape file format.
	 * 
	 * See OperatorExportToESRIShape.
	 * 
	 * @param geometry
	 *            The geometry to export. (null value is not allowed)
	 * @return Array containing the exported ESRI shape file.
	 */
	public static byte[] geometryToEsriShape(Geometry geometry) {
		if (geometry == null)
			throw new IllegalArgumentException();
		OperatorExportToESRIShape op = (OperatorExportToESRIShape) factory
				.getOperator(Operator.Type.ExportToESRIShape);
		return op.execute(0, geometry).array();
	}

	/**
	 * Imports a geometry from a WKT string.
	 * 
	 * See OperatorImportFromWkt.
	 * 
	 * @param wkt The string containing the geometry in WKT format.
	 * @param importFlags Use the {@link WktImportFlags} interface.
	 * @param geometryType The required type of the Geometry to be imported. Use Geometry.Type.Unknown if the geometry type needs to be determined from the WKT context.
	 * @return The geometry.
	 * @throws GeometryException when the geometryType is not Geometry.Type.Unknown and the WKT contains a geometry that cannot be converted to the given geometryType.
	 * @throws IllegalArgumentException if an error is found while parsing the WKT string.
	 */
	public static Geometry geometryFromWkt(String wkt, int importFlags,
			Geometry.Type geometryType) {
		OperatorImportFromWkt op = (OperatorImportFromWkt) factory
				.getOperator(Operator.Type.ImportFromWkt);
		return op.execute(importFlags, geometryType, wkt, null);
	}

	/**
	 * Exports a geometry to a string in WKT format.
	 * 
	 * See OperatorExportToWkt.
	 * 
	 * @param geometry The geometry to export. (null value is not allowed)
	 * @param exportFlags Use the {@link WktExportFlags} interface.
	 * @return A String containing the exported geometry in WKT format.
	 */
	public static String geometryToWkt(Geometry geometry, int exportFlags) {
		OperatorExportToWkt op = (OperatorExportToWkt) factory
				.getOperator(Operator.Type.ExportToWkt);
		return op.execute(exportFlags, geometry, null);
	}

	/**
	 * Constructs a new geometry by union an array of geometries. All inputs
	 * must be of the same type of geometries and share one spatial reference.
	 * 
	 * See OperatorUnion.
	 * 
	 * @param geometries
	 *            The geometries to union.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return The geometry object representing the resultant union.
	 */
	public static Geometry union(Geometry[] geometries,
			SpatialReference spatialReference) {
		OperatorUnion op = (OperatorUnion) factory
				.getOperator(Operator.Type.Union);

		SimpleGeometryCursor inputGeometries = new SimpleGeometryCursor(
				geometries);
		GeometryCursor result = op.execute(inputGeometries, spatialReference,
				null);
		return result.next();
	}

	/**
	 * Creates the difference of two geometries. The dimension of geometry2 has
	 * to be equal to or greater than that of geometry1.
	 * 
	 * See OperatorDifference.
	 * 
	 * @param geometry1
	 *            The geometry being subtracted.
	 * @param substractor
	 *            The geometry object to subtract from.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return The geometry of the differences.
	 */
	public static Geometry difference(Geometry geometry1, Geometry substractor,
			SpatialReference spatialReference) {
		OperatorDifference op = (OperatorDifference) factory
				.getOperator(Operator.Type.Difference);
		Geometry result = op.execute(geometry1, substractor, spatialReference,
				null);
		return result;
	}

	/**
	 * Creates the symmetric difference of two geometries.
	 * 
	 * See OperatorSymmetricDifference.
	 * 
	 * @param leftGeometry
	 *            is one of the Geometry instances in the XOR operation.
	 * @param rightGeometry
	 *            is one of the Geometry instances in the XOR operation.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return Returns the result of the symmetric difference.
	 */
	public static Geometry symmetricDifference(Geometry leftGeometry,
			Geometry rightGeometry, SpatialReference spatialReference) {
		OperatorSymmetricDifference op = (OperatorSymmetricDifference) factory
				.getOperator(Operator.Type.SymmetricDifference);
		Geometry result = op.execute(leftGeometry, rightGeometry,
				spatialReference, null);
		return result;
	}

	/**
	 * Indicates if two geometries are equal.
	 * 
	 * See OperatorEquals.
	 * 
	 * @param geometry1
	 *            Geometry.
	 * @param geometry2
	 *            Geometry.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return TRUE if both geometry objects are equal.
	 */
	public static boolean equals(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorEquals op = (OperatorEquals) factory
				.getOperator(Operator.Type.Equals);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * See OperatorDisjoint.
	 * 
	 */
	public static boolean disjoint(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorDisjoint op = (OperatorDisjoint) factory
				.getOperator(Operator.Type.Disjoint);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * Constructs the set-theoretic intersection between an array of geometries
	 * and another geometry.
	 * 
	 * See OperatorIntersection (also for dimension specific intersection).
	 * 
	 * @param inputGeometries
	 *            An array of geometry objects.
	 * @param geometry
	 *            The geometry object.
	 * @return Any array of geometry objects showing the intersection.
	 */
	static Geometry[] intersect(Geometry[] inputGeometries, Geometry geometry,
			SpatialReference spatialReference) {
		OperatorIntersection op = (OperatorIntersection) factory
				.getOperator(Operator.Type.Intersection);
		SimpleGeometryCursor inputGeometriesCursor = new SimpleGeometryCursor(
				inputGeometries);
		SimpleGeometryCursor intersectorCursor = new SimpleGeometryCursor(
				geometry);
		GeometryCursor result = op.execute(inputGeometriesCursor,
				intersectorCursor, spatialReference, null);

		ArrayList<Geometry> resultGeoms = new ArrayList<Geometry>();
		Geometry g;
		while ((g = result.next()) != null) {
			resultGeoms.add(g);
		}

		Geometry[] resultarr = resultGeoms.toArray(new Geometry[0]);
		return resultarr;
	}

	/**
	 * Creates a geometry through intersection between two geometries.
	 * 
	 * See OperatorIntersection.
	 * 
	 * @param geometry1
	 *            The first geometry.
	 * @param intersector
	 *            The geometry to intersect the first geometry.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return The geometry created through intersection.
	 */
	public static Geometry intersect(Geometry geometry1, Geometry intersector,
			SpatialReference spatialReference) {
		OperatorIntersection op = (OperatorIntersection) factory
				.getOperator(Operator.Type.Intersection);
		Geometry result = op.execute(geometry1, intersector, spatialReference,
				null);
		return result;
	}

	/**
	 * Indicates if one geometry is within another geometry.
	 * 
	 * See OperatorWithin.
	 * 
	 * @param geometry1
	 *            The base geometry that is tested for within relationship to
	 *            the other geometry.
	 * @param geometry2
	 *            The comparison geometry that is tested for the contains
	 *            relationship to the other geometry.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return TRUE if the first geometry is within the other geometry.
	 */
	public static boolean within(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorWithin op = (OperatorWithin) factory
				.getOperator(Operator.Type.Within);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * Indicates if one geometry contains another geometry.
	 * 
	 * See OperatorContains.
	 * 
	 * @param geometry1
	 *            The geometry that is tested for the contains relationship to
	 *            the other geometry..
	 * @param geometry2
	 *            The geometry that is tested for within relationship to the
	 *            other geometry.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return TRUE if geometry1 contains geometry2.
	 */
	public static boolean contains(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorContains op = (OperatorContains) factory
				.getOperator(Operator.Type.Contains);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * Indicates if one geometry crosses another geometry.
	 * 
	 * See OperatorCrosses.
	 * 
	 * @param geometry1
	 *            The geometry to cross.
	 * @param geometry2
	 *            The geometry being crossed.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return TRUE if geometry1 crosses geometry2.
	 */
	public static boolean crosses(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorCrosses op = (OperatorCrosses) factory
				.getOperator(Operator.Type.Crosses);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * Indicates if one geometry touches another geometry.
	 * 
	 * See OperatorTouches.
	 * 
	 * @param geometry1
	 *            The geometry to touch.
	 * @param geometry2
	 *            The geometry to be touched.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return TRUE if geometry1 touches geometry2.
	 */
	public static boolean touches(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorTouches op = (OperatorTouches) factory
				.getOperator(Operator.Type.Touches);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * Indicates if one geometry overlaps another geometry.
	 * 
	 * See OperatorOverlaps.
	 * 
	 * @param geometry1
	 *            The geometry to overlap.
	 * @param geometry2
	 *            The geometry to be overlapped.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return TRUE if geometry1 overlaps geometry2.
	 */
	public static boolean overlaps(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorOverlaps op = (OperatorOverlaps) factory
				.getOperator(Operator.Type.Overlaps);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				null);
		return result;
	}

	/**
	 * Indicates if the given relation holds for the two geometries.
	 * 
	 * See OperatorRelate.
	 * 
	 * @param geometry1
	 *            The first geometry for the relation.
	 * @param geometry2
	 *            The second geometry for the relation.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @param relation
	 *            The DE-9IM relation.
	 * @return TRUE if the given relation holds between geometry1 and geometry2.
	 */
	public static boolean relate(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference, String relation) {
		OperatorRelate op = (OperatorRelate) factory
				.getOperator(Operator.Type.Relate);
		boolean result = op.execute(geometry1, geometry2, spatialReference,
				relation, null);
		return result;
	}

	/**
	 * Calculates the 2D planar distance between two geometries.
	 * 
	 * See OperatorDistance.
	 * 
	 * @param geometry1
	 *            Geometry.
	 * @param geometry2
	 *            Geometry.
	 * @param spatialReference
	 *            The spatial reference of the geometries. This parameter is not
	 *            used and can be null.
	 * @return The distance between the two geometries.
	 */
	public static double distance(Geometry geometry1, Geometry geometry2,
			SpatialReference spatialReference) {
		OperatorDistance op = (OperatorDistance) factory
				.getOperator(Operator.Type.Distance);
		double result = op.execute(geometry1, geometry2, null);
		return result;
	}

	/**
	 * Calculates the clipped geometry from a target geometry using an envelope.
	 * 
	 * See OperatorClip.
	 * 
	 * @param geometry
	 *            The geometry to be clipped.
	 * @param envelope
	 *            The envelope used to clip.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return The geometry created by clipping.
	 */
	public static Geometry clip(Geometry geometry, Envelope envelope,
			SpatialReference spatialReference) {
		OperatorClip op = (OperatorClip) factory
				.getOperator(Operator.Type.Clip);
		Geometry result = op.execute(geometry, Envelope2D.construct(
				envelope.getXMin(), envelope.getYMin(), envelope.getXMax(),
				envelope.getYMax()), spatialReference, null);
		return result;
	}

	/**
	 * Calculates the cut geometry from a target geometry using a polyline. For
	 * Polylines, all left cuts will be grouped together in the first Geometry,
	 * Right cuts and coincident cuts are grouped in the second Geometry, and
	 * each undefined cut, along with any uncut parts, are output as separate
	 * Polylines. For Polygons, all left cuts are grouped in the first Polygon,
	 * all right cuts are in the second Polygon, and each undefined cut, along
	 * with any left-over parts after cutting, are output as a separate Polygon.
	 * If there were no cuts then the array will be empty. An undefined cut will
	 * only be produced if a left cut or right cut was produced, and there was a
	 * part left over after cutting or a cut is bounded to the left and right of
	 * the cutter.
	 * 
	 * See OperatorCut.
	 * 
	 * @param cuttee
	 *            The geometry to be cut.
	 * @param cutter
	 *            The polyline to cut the geometry.
	 * @param spatialReference
	 *            The spatial reference of the geometries.
	 * @return An array of geometries created from cutting.
	 */
	public static Geometry[] cut(Geometry cuttee, Polyline cutter,
			SpatialReference spatialReference) {
		if (cuttee == null || cutter == null)
			return null;

		OperatorCut op = (OperatorCut) factory.getOperator(Operator.Type.Cut);
		GeometryCursor cursor = op.execute(true, cuttee, cutter,
				spatialReference, null);
		ArrayList<Geometry> cutsList = new ArrayList<Geometry>();

		Geometry geometry;
		while ((geometry = cursor.next()) != null) {
			if (!geometry.isEmpty()) {
				cutsList.add(geometry);
			}
		}

		return cutsList.toArray(new Geometry[0]);
	}
	/**
	 * Calculates a buffer polygon for each geometry at each of the 
	 * corresponding specified distances.  It is assumed that all geometries have
	 * the same spatial reference. There is an option to union the 
	 * returned geometries.
	 * 
	 * See OperatorBuffer.
	 * 
	 * @param geometries An array of geometries to be buffered.
	 * @param spatialReference The spatial reference of the geometries.
	 * @param distances The corresponding distances for the input geometries to be buffered.
	 * @param toUnionResults TRUE if all geometries buffered at a given distance are to be unioned into a single polygon.
	 * @return The buffer of the geometries.
	 */
	public static Polygon[] buffer(Geometry[] geometries,
			SpatialReference spatialReference, double[] distances,
			boolean toUnionResults) {
		// initially assume distances are in unit of spatial reference
		double[] bufferDistances = distances;

		OperatorBuffer op = (OperatorBuffer) factory
				.getOperator(Operator.Type.Buffer);

		if (toUnionResults) {
			SimpleGeometryCursor inputGeometriesCursor = new SimpleGeometryCursor(
					geometries);
			GeometryCursor result = op.execute(inputGeometriesCursor,
					spatialReference, bufferDistances, toUnionResults, null);

			ArrayList<Polygon> resultGeoms = new ArrayList<Polygon>();
			Geometry g;
			while ((g = result.next()) != null) {
				resultGeoms.add((Polygon) g);
			}
			Polygon[] buffers = resultGeoms.toArray(new Polygon[0]);
			return buffers;
		} else {
			Polygon[] buffers = new Polygon[geometries.length];
			for (int i = 0; i < geometries.length; i++) {
				buffers[i] = (Polygon) op.execute(geometries[i],
						spatialReference, bufferDistances[i], null);
			}
			return buffers;
		}
	}

	/**
	 * Calculates a buffer polygon of the geometry as specified by the 
	 * distance input. The buffer is implemented in the xy-plane.
	 * 
	 * See OperatorBuffer
	 * 
	 * @param geometry Geometry to be buffered.
	 * @param spatialReference The spatial reference of the geometry.
	 * @param distance The specified distance for buffer. Same units as the spatial reference.
	 * @return The buffer polygon at the specified distances.
	 */
	public static Polygon buffer(Geometry geometry,
			SpatialReference spatialReference, double distance) {
		double bufferDistance = distance;

		OperatorBuffer op = (OperatorBuffer) factory
				.getOperator(Operator.Type.Buffer);
		Geometry result = op.execute(geometry, spatialReference,
				bufferDistance, null);
		return (Polygon) result;
	}

	/**
	 * Calculates the convex hull geometry.
	 * 
	 * See OperatorConvexHull.
	 * 
	 * @param geometry The input geometry.
	 * @return Returns the convex hull.
	 * 
	 *            For a Point - returns the same point. For an Envelope -
	 *            returns the same envelope. For a MultiPoint - If the point
	 *            count is one, returns the same multipoint. If the point count
	 *            is two, returns a polyline of the points. Otherwise computes
	 *            and returns the convex hull polygon. For a Segment - returns a
	 *            polyline consisting of the segment. For a Polyline - If
	 *            consists of only one segment, returns the same polyline.
	 *            Otherwise computes and returns the convex hull polygon. For a
	 *            Polygon - If more than one path, or if the path isn't already
	 *            convex, computes and returns the convex hull polygon.
	 *            Otherwise returns the same polygon.
	 */
	public static Geometry convexHull(Geometry geometry) {
		OperatorConvexHull op = (OperatorConvexHull) factory
				.getOperator(Operator.Type.ConvexHull);
		return op.execute(geometry, null);
	}

	/**
	 * Calculates the convex hull.
	 * 
	 * See OperatorConvexHull
	 * 
	 * @param geometries
	 *            The input geometry array.
	 * @param b_merge
	 *            Put true if you want the convex hull of all the geometries in
	 *            the array combined. Put false if you want the convex hull of
	 *            each geometry in the array individually.
	 * @return Returns an array of convex hulls. If b_merge is true, the result
	 *         will be a one element array consisting of the merged convex hull.
	 */
	public static Geometry[] convexHull(Geometry[] geometries, boolean b_merge) {
		OperatorConvexHull op = (OperatorConvexHull) factory
				.getOperator(Operator.Type.ConvexHull);
		SimpleGeometryCursor simple_cursor = new SimpleGeometryCursor(
				geometries);
		GeometryCursor cursor = op.execute(simple_cursor, b_merge, null);

		ArrayList<Geometry> resultGeoms = new ArrayList<Geometry>();
		Geometry g;
		while ((g = cursor.next()) != null) {
			resultGeoms.add(g);
		}

		Geometry[] output = new Geometry[resultGeoms.size()];

		for (int i = 0; i < resultGeoms.size(); i++)
			output[i] = resultGeoms.get(i);

		return output;
	}

	/**
	 * Finds the coordinate of the geometry which is closest to the specified
	 * point.
	 *
	 * See OperatorProximity2D.
	 * 
	 * @param inputPoint
	 *            The point to find the nearest coordinate in the geometry for.
	 * @param geometry
	 *            The geometry to consider.
	 * @return Proximity2DResult containing the nearest coordinate.
	 */
	public static Proximity2DResult getNearestCoordinate(Geometry geometry,
			Point inputPoint, boolean bTestPolygonInterior) {

		OperatorProximity2D proximity = (OperatorProximity2D) factory
				.getOperator(com.esri.core.geometry.Operator.Type.Proximity2D);
		Proximity2DResult result = proximity.getNearestCoordinate(geometry,
				inputPoint, bTestPolygonInterior);
		return result;
	}

	/**
	 * Finds nearest vertex on the geometry which is closed to the specified
	 * point.
	 * 
	 * See OperatorProximity2D.
	 * 
	 * @param inputPoint
	 *            The point to find the nearest vertex of the geometry for.
	 * @param geometry
	 *            The geometry to consider.
	 * @return Proximity2DResult containing the nearest vertex.
	 */
	public static Proximity2DResult getNearestVertex(Geometry geometry,
			Point inputPoint) {
		OperatorProximity2D proximity = (OperatorProximity2D) factory
				.getOperator(com.esri.core.geometry.Operator.Type.Proximity2D);
		Proximity2DResult result = proximity.getNearestVertex(geometry,
				inputPoint);
		return result;
	}

	/**
	 * Finds all vertices in the given distance from the specified point, sorted
	 * from the closest to the furthest.
	 * 
	 * See OperatorProximity2D.
	 * 
	 * @param inputPoint
	 *            The point to start from.
	 * @param geometry
	 *            The geometry to consider.
	 * @param searchRadius
	 *            The search radius.
	 * @param maxVertexCountToReturn
	 *            The maximum number number of vertices to return.
	 * @return Proximity2DResult containing the array of nearest vertices.
	 */
	public static Proximity2DResult[] getNearestVertices(Geometry geometry,
			Point inputPoint, double searchRadius, int maxVertexCountToReturn) {
		OperatorProximity2D proximity = (OperatorProximity2D) factory
				.getOperator(com.esri.core.geometry.Operator.Type.Proximity2D);

		Proximity2DResult[] results = proximity.getNearestVertices(geometry,
				inputPoint, searchRadius, maxVertexCountToReturn);

		return results;
	}

	/**
	 * Performs the simplify operation on the geometry.
	 *
	 * See OperatorSimplify and See OperatorSimplifyOGC.
	 * 
	 * @param geometry
	 *            The geometry to be simplified.
	 * @param spatialReference
	 *            The spatial reference of the geometry to be simplified.
	 * @return The simplified geometry.
	 */
	public static Geometry simplify(Geometry geometry,
			SpatialReference spatialReference) {
		OperatorSimplify op = (OperatorSimplify) factory
				.getOperator(Operator.Type.Simplify);
		Geometry result = op.execute(geometry, spatialReference, false, null);
		return result;
	}

	/**
	 * Checks if the Geometry is simple.
	 * 
	 * See OperatorSimplify.
	 * 
	 * @param geometry
	 *            The geometry to be checked.
	 * @param spatialReference
	 *            The spatial reference of the geometry.
	 * @return TRUE if the geometry is simple.
	 */
	static boolean isSimple(Geometry geometry, SpatialReference spatialReference) {
		OperatorSimplify op = (OperatorSimplify) factory
				.getOperator(Operator.Type.Simplify);
		boolean result = op.isSimpleAsFeature(geometry, spatialReference, null);
		return result;
	}

	/**
	 * A geodesic distance is the shortest distance between any two points on the earth's surface when the earth's
	 * surface is approximated by a spheroid. The function returns the shortest distance between two points on the
	 * WGS84 spheroid.    
	 * @param ptFrom The "from" point: long, lat in degrees.
	 * @param ptTo The "to" point: long, lat in degrees.
	 * @return The geodesic distance between two points in meters.
	 */
	public static double geodesicDistanceOnWGS84(Point ptFrom, Point ptTo) {
		return SpatialReferenceImpl.geodesicDistanceOnWGS84Impl(ptFrom, ptTo);
	}
}
