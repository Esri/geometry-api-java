/*
 Copyright 1995-2015 Esri

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
/*
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

class OperatorExportToGeoJsonCursor extends JsonCursor {
	GeometryCursor m_inputGeometryCursor;
	SpatialReference m_spatialReference;
	int m_index;
	int m_export_flags;

	public OperatorExportToGeoJsonCursor(int export_flags, SpatialReference spatialReference,
			GeometryCursor geometryCursor) {
		m_index = -1;
		if (geometryCursor == null)
			throw new IllegalArgumentException();

		m_export_flags = export_flags;
		m_spatialReference = spatialReference;
		m_inputGeometryCursor = geometryCursor;
	}

	@Override
	public int getID() {
		return m_index;
	}

	@Override
	public String next() {
		Geometry geometry;
		if ((geometry = m_inputGeometryCursor.next()) != null) {
			m_index = m_inputGeometryCursor.getGeometryID();
			return exportToGeoJson(m_export_flags, geometry, m_spatialReference);
		}
		return null;
	}

	// Mirrors wkt
	static String exportToGeoJson(int export_flags, Geometry geometry, SpatialReference spatial_reference) {

		if (geometry == null)
			throw new IllegalArgumentException("");

		JsonWriter json_writer = new JsonStringWriter();

		json_writer.startObject();

		exportGeometryToGeoJson_(export_flags, geometry, json_writer);

		if ((export_flags & GeoJsonExportFlags.geoJsonExportSkipCRS) == 0) {
			json_writer.addFieldName("crs");
			exportSpatialReference(export_flags, spatial_reference, json_writer);
		}

		json_writer.endObject();

		return (String) json_writer.getJson();
	}

	static String exportSpatialReference(int export_flags, SpatialReference spatial_reference) {
		if (spatial_reference == null || (export_flags & GeoJsonExportFlags.geoJsonExportSkipCRS) != 0)
			throw new IllegalArgumentException("");

		JsonWriter json_writer = new JsonStringWriter();
		exportSpatialReference(export_flags, spatial_reference, json_writer);

		return (String) json_writer.getJson();
	}

	private static void exportGeometryToGeoJson_(int export_flags, Geometry geometry, JsonWriter json_writer) {
		int type = geometry.getType().value();
		switch (type) {
		case Geometry.GeometryType.Polygon:
			exportPolygonToGeoJson_(export_flags, (Polygon) geometry, json_writer);
			return;

		case Geometry.GeometryType.Polyline:
			exportPolylineToGeoJson_(export_flags, (Polyline) geometry, json_writer);
			return;

		case Geometry.GeometryType.MultiPoint:
			exportMultiPointToGeoJson_(export_flags, (MultiPoint) geometry, json_writer);
			return;

		case Geometry.GeometryType.Point:
			exportPointToGeoJson_(export_flags, (Point) geometry, json_writer);
			return;

		case Geometry.GeometryType.Envelope:
			exportEnvelopeToGeoJson_(export_flags, (Envelope) geometry,
					json_writer);
			return;

		default:
			throw new RuntimeException("not implemented for this geometry type");
		}
	}

	private static void exportSpatialReference(int export_flags, SpatialReference spatial_reference,
			JsonWriter json_writer) {
		if (spatial_reference != null) {
			int wkid = spatial_reference.getLatestID();

			if (wkid <= 0)
				throw new GeometryException("invalid call");

			json_writer.startObject();

			json_writer.addFieldName("type");

			json_writer.addValueString("name");

			json_writer.addFieldName("properties");
			json_writer.startObject();

			json_writer.addFieldName("name");

			String authority = ((SpatialReferenceImpl) spatial_reference).getAuthority();
			authority = authority.toUpperCase();
			StringBuilder crs_identifier = new StringBuilder(authority);
			crs_identifier.append(':');
			crs_identifier.append(wkid);
			json_writer.addValueString(crs_identifier.toString());

			json_writer.endObject();

			json_writer.endObject();
		} else {
			json_writer.addValueNull();
		}
	}

	// Mirrors wkt
	private static void exportPolygonToGeoJson_(int export_flags, Polygon polygon, JsonWriter json_writer) {
		MultiPathImpl polygon_impl = (MultiPathImpl) (polygon._getImpl());

		if ((export_flags & GeoJsonExportFlags.geoJsonExportFailIfNotSimple) != 0) {
			int simple = polygon_impl.getIsSimple(0.0);

			if (simple != MultiPathImpl.GeometryXSimple.Strong)
				throw new GeometryException("corrupted geometry");
		}

		int point_count = polygon.getPointCount();
		int polygon_count = polygon_impl.getOGCPolygonCount();

		if (point_count > 0 && polygon_count == 0)
			throw new GeometryException("corrupted geometry");

		int precision = 17 - (31 & (export_flags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & export_flags) != 0;
		boolean b_export_zs = polygon_impl.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean b_export_ms = polygon_impl.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!b_export_zs && b_export_ms)
			throw new IllegalArgumentException("invalid argument");

		int path_count = 0;
		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt8 path_flags = null;
		AttributeStreamOfInt32 paths = null;

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) polygon_impl.getAttributeStreamRef(Semantics.POSITION);
			path_flags = polygon_impl.getPathFlagsStreamRef();
			paths = polygon_impl.getPathStreamRef();
			path_count = polygon_impl.getPathCount();

			if (b_export_zs) {
				if (polygon_impl._attributeStreamIsAllocated(Semantics.Z))
					zs = (AttributeStreamOfDbl) polygon_impl.getAttributeStreamRef(Semantics.Z);
			}

			if (b_export_ms) {
				if (polygon_impl._attributeStreamIsAllocated(Semantics.M))
					ms = (AttributeStreamOfDbl) polygon_impl.getAttributeStreamRef(Semantics.M);
			}
		}

		if ((export_flags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0 && polygon_count <= 1)
			polygonTaggedText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, path_count,
					json_writer);
		else
			multiPolygonTaggedText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, path_flags,
					paths, polygon_count, path_count, json_writer);
	}

	// Mirrors wkt
	private static void exportPolylineToGeoJson_(int export_flags, Polyline polyline, JsonWriter json_writer) {
		MultiPathImpl polyline_impl = (MultiPathImpl) polyline._getImpl();

		int point_count = polyline_impl.getPointCount();
		int path_count = polyline_impl.getPathCount();

		if (point_count > 0 && path_count == 0)
			throw new GeometryException("corrupted geometry");

		int precision = 17 - (31 & (export_flags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & export_flags) != 0;
		boolean b_export_zs = polyline_impl.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean b_export_ms = polyline_impl.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!b_export_zs && b_export_ms)
			throw new IllegalArgumentException("invalid argument");

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt8 path_flags = null;
		AttributeStreamOfInt32 paths = null;

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) polyline_impl.getAttributeStreamRef(Semantics.POSITION);
			path_flags = polyline_impl.getPathFlagsStreamRef();
			paths = polyline_impl.getPathStreamRef();

			if (b_export_zs) {
				if (polyline_impl._attributeStreamIsAllocated(Semantics.Z))
					zs = (AttributeStreamOfDbl) polyline_impl.getAttributeStreamRef(Semantics.Z);
			}

			if (b_export_ms) {
				if (polyline_impl._attributeStreamIsAllocated(Semantics.M))
					ms = (AttributeStreamOfDbl) polyline_impl.getAttributeStreamRef(Semantics.M);
			}
		}

		if ((export_flags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0 && path_count <= 1)
			lineStringTaggedText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, path_flags, paths,
					json_writer);
		else
			multiLineStringTaggedText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, path_flags,
					paths, path_count, json_writer);
	}

	// Mirrors wkt
	private static void exportMultiPointToGeoJson_(int export_flags, MultiPoint multipoint, JsonWriter json_writer) {
		MultiPointImpl multipoint_impl = (MultiPointImpl) multipoint._getImpl();

		int point_count = multipoint_impl.getPointCount();

		int precision = 17 - (31 & (export_flags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & export_flags) != 0;
		boolean b_export_zs = multipoint_impl.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean b_export_ms = multipoint_impl.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!b_export_zs && b_export_ms)
			throw new IllegalArgumentException("invalid argument");

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) multipoint_impl.getAttributeStreamRef(Semantics.POSITION);

			if (b_export_zs) {
				if (multipoint_impl._attributeStreamIsAllocated(Semantics.Z))
					zs = (AttributeStreamOfDbl) multipoint_impl.getAttributeStreamRef(Semantics.Z);
			}

			if (b_export_ms) {
				if (multipoint_impl._attributeStreamIsAllocated(Semantics.M))
					ms = (AttributeStreamOfDbl) multipoint_impl.getAttributeStreamRef(Semantics.M);
			}
		}

		multiPointTaggedText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, point_count,
				json_writer);
	}

	// Mirrors wkt
	private static void exportPointToGeoJson_(int export_flags, Point point, JsonWriter json_writer) {
		int precision = 17 - (31 & (export_flags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & export_flags) != 0;
		boolean b_export_zs = point.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean b_export_ms = point.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!b_export_zs && b_export_ms)
			throw new IllegalArgumentException("invalid argument");

		double x = NumberUtils.NaN();
		double y = NumberUtils.NaN();
		double z = NumberUtils.NaN();
		double m = NumberUtils.NaN();

		if (!point.isEmpty()) {
			x = point.getX();
			y = point.getY();

			if (b_export_zs)
				z = point.getZ();

			if (b_export_ms)
				m = point.getM();
		}

		if ((export_flags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0)
			pointTaggedText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);
		else
			multiPointTaggedTextFromPoint_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);
	}

	// Mirrors wkt
	private static void exportEnvelopeToGeoJson_(int export_flags, Envelope envelope, JsonWriter json_writer) {
		int precision = 17 - (31 & (export_flags >> 13));
		boolean bFixedPoint = (GeoJsonExportFlags.geoJsonExportPrecisionFixedPoint & export_flags) != 0;
		boolean b_export_zs = envelope.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripZs) == 0;
		boolean b_export_ms = envelope.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & GeoJsonExportFlags.geoJsonExportStripMs) == 0;

		if (!b_export_zs && b_export_ms)
			throw new IllegalArgumentException("invalid argument");

		double xmin = NumberUtils.NaN();
		double ymin = NumberUtils.NaN();
		double xmax = NumberUtils.NaN();
		double ymax = NumberUtils.NaN();
		double zmin = NumberUtils.NaN();
		double zmax = NumberUtils.NaN();
		double mmin = NumberUtils.NaN();
		double mmax = NumberUtils.NaN();

		if (!envelope.isEmpty()) {
			xmin = envelope.getXMin();
			ymin = envelope.getYMin();
			xmax = envelope.getXMax();
			ymax = envelope.getYMax();

			Envelope1D interval;

			if (b_export_zs) {
				interval = envelope.queryInterval(Semantics.Z, 0);
				zmin = interval.vmin;
				zmax = interval.vmax;
			}

			if (b_export_ms) {
				interval = envelope.queryInterval(Semantics.M, 0);
				mmin = interval.vmin;
				mmax = interval.vmax;
			}
		}

		if ((export_flags & GeoJsonExportFlags.geoJsonExportPreferMultiGeometry) == 0)
			polygonTaggedTextFromEnvelope_(precision, bFixedPoint, b_export_zs, b_export_ms, xmin, ymin, xmax, ymax,
					zmin, zmax, mmin, mmax, json_writer);
		else
			multiPolygonTaggedTextFromEnvelope_(precision, bFixedPoint, b_export_zs, b_export_ms, xmin, ymin, xmax,
					ymax, zmin, zmax, mmin, mmax, json_writer);
	}

	// Mirrors wkt
	private static void multiPolygonTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, int polygon_count, int path_count,
			JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("MultiPolygon");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		multiPolygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, path_flags, paths,
				polygon_count, path_count, json_writer);

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void multiPolygonTaggedTextFromEnvelope_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double xmin, double ymin, double xmax, double ymax, double zmin, double zmax,
			double mmin, double mmax, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("MultiPolygon");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(xmin)) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		writeEnvelopeAsGeoJsonPolygon_(precision, bFixedPoint, b_export_zs, b_export_ms, xmin, ymin, xmax, ymax, zmin,
				zmax, mmin, mmax, json_writer);

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void multiLineStringTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, int path_count, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("MultiLineString");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		multiLineStringText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, path_flags, paths,
				path_count, json_writer);

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void multiPointTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			int point_count, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("MultiPoint");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		lineStringText_(false, false, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, 0,
				point_count, json_writer);
	}

	// Mirrors wkt
	private static void multiPointTaggedTextFromPoint_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double x, double y, double z, double m, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("MultiPoint");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(x)) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void polygonTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, int path_count, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("Polygon");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		polygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, 0, path_count,
				json_writer);
	}

	// Mirrors wkt
	private static void polygonTaggedTextFromEnvelope_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double xmin, double ymin, double xmax, double ymax, double zmin, double zmax,
			double mmin, double mmax, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("Polygon");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(xmin)) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		writeEnvelopeAsGeoJsonPolygon_(precision, bFixedPoint, b_export_zs, b_export_ms, xmin, ymin, xmax, ymax, zmin,
				zmax, mmin, mmax, json_writer);
	}

	// Mirrors wkt
	private static void lineStringTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("LineString");

		json_writer.addFieldName("coordinates");

		if (position == null) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		boolean b_closed = ((path_flags.read(0) & PathFlags.enumClosed) != 0);

		lineStringText_(false, b_closed, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, 0,
				paths.read(1), json_writer);
	}

	// Mirrors wkt
	private static void pointTaggedText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			double x, double y, double z, double m, JsonWriter json_writer) {
		json_writer.addFieldName("type");
		json_writer.addValueString("Point");

		json_writer.addFieldName("coordinates");

		if (NumberUtils.isNaN(x)) {
			json_writer.startArray();
			json_writer.endArray();

			return;
		}

		pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);
	}

	// Mirrors wkt
	private static void multiPolygonText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, int polygon_count, int path_count,
			JsonWriter json_writer) {
		int polygon_start = 0;
		int polygon_end = 1;

		while (polygon_end < path_count && ((int) path_flags.read(polygon_end) & PathFlags.enumOGCStartPolygon) == 0)
			polygon_end++;

		polygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, polygon_start,
				polygon_end, json_writer);

		for (int ipolygon = 1; ipolygon < polygon_count; ipolygon++) {
			polygon_start = polygon_end;
			polygon_end++;

			while (polygon_end < path_count
					&& ((int) path_flags.read(polygon_end) & PathFlags.enumOGCStartPolygon) == 0)
				polygon_end++;

			polygonText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, paths, polygon_start,
					polygon_end, json_writer);
		}
	}

	// Mirrors wkt
	private static void multiLineStringText_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths, int path_count, JsonWriter json_writer) {
		boolean b_closed = ((path_flags.read(0) & PathFlags.enumClosed) != 0);

		lineStringText_(false, b_closed, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, 0,
				paths.read(1), json_writer);

		for (int path = 1; path < path_count; path++) {
			b_closed = ((path_flags.read(path) & PathFlags.enumClosed) != 0);

			int istart = paths.read(path);
			int iend = paths.read(path + 1);
			lineStringText_(false, b_closed, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart,
					iend, json_writer);
		}
	}

	// Mirrors wkt
	private static void polygonText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, int polygon_start, int polygon_end, JsonWriter json_writer) {
		json_writer.startArray();

		int istart = paths.read(polygon_start);
		int iend = paths.read(polygon_start + 1);
		lineStringText_(true, true, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, iend,
				json_writer);

		for (int path = polygon_start + 1; path < polygon_end; path++) {
			istart = paths.read(path);
			iend = paths.read(path + 1);
			lineStringText_(true, true, precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart,
					iend, json_writer);
		}

		json_writer.endArray();
	}

	// Mirrors wkt
	private static void lineStringText_(boolean bRing, boolean b_closed, int precision, boolean bFixedPoint,
			boolean b_export_zs, boolean b_export_ms, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, int istart, int iend, JsonWriter json_writer) {
		if (istart == iend) {
			json_writer.startArray();
			json_writer.endArray();
			return;
		}

		json_writer.startArray();

		if (bRing) {
			pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, json_writer);

			for (int point = iend - 1; point >= istart + 1; point--)
				pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, point, json_writer);

			pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, json_writer);
		} else {
			for (int point = istart; point < iend - 1; point++)
				pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, point, json_writer);

			pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, iend - 1, json_writer);

			if (b_closed)
				pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, zs, ms, position, istart, json_writer);
		}

		json_writer.endArray();
	}

	// Mirrors wkt
	private static int pointText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			double x, double y, double z, double m, JsonWriter json_writer) {

		json_writer.startArray();

		json_writer.addValueDouble(x, precision, bFixedPoint);
		json_writer.addValueDouble(y, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(z, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(m, precision, bFixedPoint);

		json_writer.endArray();

		return 1;
	}

	// Mirrors wkt
	private static void pointText_(int precision, boolean bFixedPoint, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position, int point,
			JsonWriter json_writer) {
		double x = position.readAsDbl(2 * point);
		double y = position.readAsDbl(2 * point + 1);
		double z = NumberUtils.NaN();
		double m = NumberUtils.NaN();

		if (b_export_zs)
			z = (zs != null ? zs.readAsDbl(point) : VertexDescription.getDefaultValue(Semantics.Z));

		if (b_export_ms)
			m = (ms != null ? ms.readAsDbl(point) : VertexDescription.getDefaultValue(Semantics.M));

		pointText_(precision, bFixedPoint, b_export_zs, b_export_ms, x, y, z, m, json_writer);
	}

	// Mirrors wkt
	private static void writeEnvelopeAsGeoJsonPolygon_(int precision, boolean bFixedPoint, boolean b_export_zs,
			boolean b_export_ms, double xmin, double ymin, double xmax, double ymax, double zmin, double zmax,
			double mmin, double mmax, JsonWriter json_writer) {
		json_writer.startArray();
		json_writer.startArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmin, precision, bFixedPoint);
		json_writer.addValueDouble(ymin, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmin, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmin, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmax, precision, bFixedPoint);
		json_writer.addValueDouble(ymin, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmax, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmax, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmax, precision, bFixedPoint);
		json_writer.addValueDouble(ymax, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmin, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmin, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmin, precision, bFixedPoint);
		json_writer.addValueDouble(ymax, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmax, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmax, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.startArray();
		json_writer.addValueDouble(xmin, precision, bFixedPoint);
		json_writer.addValueDouble(ymin, precision, bFixedPoint);

		if (b_export_zs)
			json_writer.addValueDouble(zmin, precision, bFixedPoint);

		if (b_export_ms)
			json_writer.addValueDouble(mmin, precision, bFixedPoint);

		json_writer.endArray();

		json_writer.endArray();
		json_writer.endArray();
	}
}
