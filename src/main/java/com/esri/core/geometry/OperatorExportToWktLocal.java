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
package com.esri.core.geometry;

class OperatorExportToWktLocal extends OperatorExportToWkt {
	@Override
	public String execute(int export_flags, Geometry geometry,
			ProgressTracker progress_tracker) {
		StringBuilder string = new StringBuilder();
		exportToWkt(export_flags, geometry, string);

		return string.toString();
	}

	static void exportToWkt(int export_flags, Geometry geometry,
			StringBuilder string) {
		int type = geometry.getType().value();
		switch (type) {
		case Geometry.GeometryType.Polygon:
			if ((export_flags & WktExportFlags.wktExportLineString) != 0
					|| (export_flags & WktExportFlags.wktExportMultiLineString) != 0
					|| (export_flags & WktExportFlags.wktExportPoint) != 0
					|| (export_flags & WktExportFlags.wktExportMultiPoint) != 0)
				throw new IllegalArgumentException("Cannot export a Polygon as (Multi)LineString/(Multi)Point : "+export_flags);

			exportPolygonToWkt(export_flags, (Polygon) geometry, string);
			return;

		case Geometry.GeometryType.Polyline:
			if ((export_flags & WktExportFlags.wktExportPolygon) != 0
					|| (export_flags & WktExportFlags.wktExportMultiPolygon) != 0
					|| (export_flags & WktExportFlags.wktExportPoint) != 0
					|| (export_flags & WktExportFlags.wktExportMultiPoint) != 0)
				throw new IllegalArgumentException("Cannot export a Polyline as (Multi)Polygon/(Multi)Point : "+export_flags);

			exportPolylineToWkt(export_flags, (Polyline) geometry, string);
			return;

		case Geometry.GeometryType.MultiPoint:
			if ((export_flags & WktExportFlags.wktExportLineString) != 0
					|| (export_flags & WktExportFlags.wktExportMultiLineString) != 0
					|| (export_flags & WktExportFlags.wktExportPolygon) != 0
					|| (export_flags & WktExportFlags.wktExportMultiPolygon) != 0)
				throw new IllegalArgumentException("Cannot export a MultiPoint as (Multi)LineString/(Multi)Polygon: "+export_flags);

			exportMultiPointToWkt(export_flags, (MultiPoint) geometry, string);
			return;

		case Geometry.GeometryType.Point:
			if ((export_flags & WktExportFlags.wktExportLineString) != 0
					|| (export_flags & WktExportFlags.wktExportMultiLineString) != 0
					|| (export_flags & WktExportFlags.wktExportPolygon) != 0
					|| (export_flags & WktExportFlags.wktExportMultiPolygon) != 0)
				throw new IllegalArgumentException("Cannot export a Point as (Multi)LineString/(Multi)Polygon: "+export_flags);

			exportPointToWkt(export_flags, (Point) geometry, string);
			return;

		case Geometry.GeometryType.Envelope:
			if ((export_flags & WktExportFlags.wktExportLineString) != 0
					|| (export_flags & WktExportFlags.wktExportMultiLineString) != 0
					|| (export_flags & WktExportFlags.wktExportPoint) != 0
					|| (export_flags & WktExportFlags.wktExportMultiPoint) != 0)
				throw new IllegalArgumentException("Cannot export an Envelope as (Multi)LineString/(Multi)Point: "+export_flags);

			exportEnvelopeToWkt(export_flags, (Envelope) geometry, string);
			return;

		default: {
			throw GeometryException.GeometryInternalError();
		}
		}
	}

	static void exportPolygonToWkt(int export_flags, Polygon polygon,
			StringBuilder string) {
		MultiPathImpl polygon_impl = (MultiPathImpl) polygon._getImpl();

		if ((export_flags & WktExportFlags.wktExportFailIfNotSimple) != 0) {
			int simple = polygon_impl.getIsSimple(0.0);

			if (simple != MultiPathImpl.GeometryXSimple.Strong)
				throw new GeometryException("corrupted geometry");
		}

		int point_count = polygon.getPointCount();
		int polygon_count = polygon_impl.getOGCPolygonCount();

		if (point_count > 0 && polygon_count == 0)
			throw new GeometryException("corrupted geometry");

		int precision = 17 - (7 & (export_flags >> 13));
		boolean b_export_zs = polygon_impl
				.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & WktExportFlags.wktExportStripZs) == 0;
		boolean b_export_ms = polygon_impl
				.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & WktExportFlags.wktExportStripMs) == 0;

		int path_count = 0;
		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt8 path_flags = null;
		AttributeStreamOfInt32 paths = null;

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) (polygon_impl
					.getAttributeStreamRef(VertexDescription.Semantics.POSITION));
			path_flags = polygon_impl.getPathFlagsStreamRef();
			paths = polygon_impl.getPathStreamRef();
			path_count = polygon_impl.getPathCount();

			if (b_export_zs) {
				if (polygon_impl
						._attributeStreamIsAllocated(VertexDescription.Semantics.Z))
					zs = (AttributeStreamOfDbl) polygon_impl
							.getAttributeStreamRef(VertexDescription.Semantics.Z);
			}

			if (b_export_ms) {
				if (polygon_impl
						._attributeStreamIsAllocated(VertexDescription.Semantics.M))
					ms = (AttributeStreamOfDbl) polygon_impl
							.getAttributeStreamRef(VertexDescription.Semantics.M);
			}
		}

		if ((export_flags & WktExportFlags.wktExportPolygon) != 0) {
			if (polygon_count > 1)
				throw new IllegalArgumentException("Cannot export a Polygon with specified export flags: "+export_flags);

			polygonTaggedText_(precision, b_export_zs, b_export_ms, zs, ms,
					position, path_flags, paths, path_count, string);
		} else {
			multiPolygonTaggedText_(precision, b_export_zs, b_export_ms, zs,
					ms, position, path_flags, paths, polygon_count, path_count,
					string);
		}
	}

	static void exportPolylineToWkt(int export_flags, Polyline polyline,
			StringBuilder string) {
		MultiPathImpl polyline_impl = (MultiPathImpl) polyline._getImpl();

		int point_count = polyline_impl.getPointCount();
		int path_count = polyline_impl.getPathCount();

		if (point_count > 0 && path_count == 0)
			throw new GeometryException("corrupted geometry");

		int precision = 17 - (7 & (export_flags >> 13));
		boolean b_export_zs = polyline_impl
				.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & WktExportFlags.wktExportStripZs) == 0;
		boolean b_export_ms = polyline_impl
				.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & WktExportFlags.wktExportStripMs) == 0;

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt8 path_flags = null;
		AttributeStreamOfInt32 paths = null;

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) polyline_impl
					.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			path_flags = polyline_impl.getPathFlagsStreamRef();
			paths = polyline_impl.getPathStreamRef();

			if (b_export_zs) {
				if (polyline_impl
						._attributeStreamIsAllocated(VertexDescription.Semantics.Z))
					zs = (AttributeStreamOfDbl) (polyline_impl
							.getAttributeStreamRef(VertexDescription.Semantics.Z));
			}

			if (b_export_ms) {
				if (polyline_impl
						._attributeStreamIsAllocated(VertexDescription.Semantics.M))
					ms = (AttributeStreamOfDbl) (polyline_impl
							.getAttributeStreamRef(VertexDescription.Semantics.M));
			}
		}

		if ((export_flags & WktExportFlags.wktExportLineString) != 0) {
			if (path_count > 1)
				throw new IllegalArgumentException("Cannot export a LineString with specified export flags: "+export_flags);

			lineStringTaggedText_(precision, b_export_zs, b_export_ms, zs, ms,
					position, path_flags, paths, string);
		} else {
			multiLineStringTaggedText_(precision, b_export_zs, b_export_ms, zs,
					ms, position, path_flags, paths, path_count, string);
		}
	}

	static void exportMultiPointToWkt(int export_flags, MultiPoint multipoint,
			StringBuilder string) {
		MultiPointImpl multipoint_impl = (MultiPointImpl) multipoint._getImpl();

		int point_count = multipoint_impl.getPointCount();

		int precision = 17 - (7 & (export_flags >> 13));
		boolean b_export_zs = multipoint_impl
				.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & WktExportFlags.wktExportStripZs) == 0;
		boolean b_export_ms = multipoint_impl
				.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & WktExportFlags.wktExportStripMs) == 0;

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) (multipoint_impl
					.getAttributeStreamRef(VertexDescription.Semantics.POSITION));

			if (b_export_zs) {
				if (multipoint_impl
						._attributeStreamIsAllocated(VertexDescription.Semantics.Z))
					zs = (AttributeStreamOfDbl) (multipoint_impl
							.getAttributeStreamRef(VertexDescription.Semantics.Z));
			}

			if (b_export_ms) {
				if (multipoint_impl
						._attributeStreamIsAllocated(VertexDescription.Semantics.M))
					ms = (AttributeStreamOfDbl) (multipoint_impl
							.getAttributeStreamRef(VertexDescription.Semantics.M));
			}
		}

		if ((export_flags & WktExportFlags.wktExportPoint) != 0) {
			if (point_count > 1)
				throw new IllegalArgumentException("Cannot export a Point with specified export flags: "+export_flags);

			pointTaggedTextFromMultiPoint_(precision, b_export_zs, b_export_ms,
					zs, ms, position, string);
		} else {
			multiPointTaggedText_(precision, b_export_zs, b_export_ms, zs, ms,
					position, point_count, string);
		}
	}

	static void exportPointToWkt(int export_flags, Point point,
			StringBuilder string) {
		int precision = 17 - (7 & (export_flags >> 13));
		boolean b_export_zs = point.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & WktExportFlags.wktExportStripZs) == 0;
		boolean b_export_ms = point.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & WktExportFlags.wktExportStripMs) == 0;

		double x = NumberUtils.TheNaN;
		double y = NumberUtils.TheNaN;
		double z = NumberUtils.TheNaN;
		double m = NumberUtils.TheNaN;

		if (!point.isEmpty()) {
			x = point.getX();
			y = point.getY();

			if (b_export_zs)
				z = point.getZ();

			if (b_export_ms)
				m = point.getM();
		}

		if ((export_flags & WktExportFlags.wktExportMultiPoint) != 0) {
			multiPointTaggedTextFromPoint_(precision, b_export_zs, b_export_ms,
					x, y, z, m, string);
		} else {
			pointTaggedText_(precision, b_export_zs, b_export_ms, x, y, z, m,
					string);
		}
	}

	static void exportEnvelopeToWkt(int export_flags, Envelope envelope,
			StringBuilder string) {
		int precision = 17 - (7 & (export_flags >> 13));
		boolean b_export_zs = envelope
				.hasAttribute(VertexDescription.Semantics.Z)
				&& (export_flags & WktExportFlags.wktExportStripZs) == 0;
		boolean b_export_ms = envelope
				.hasAttribute(VertexDescription.Semantics.M)
				&& (export_flags & WktExportFlags.wktExportStripMs) == 0;

		double xmin = NumberUtils.TheNaN;
		double ymin = NumberUtils.TheNaN;
		double xmax = NumberUtils.TheNaN;
		double ymax = NumberUtils.TheNaN;
		double zmin = NumberUtils.TheNaN;
		double zmax = NumberUtils.TheNaN;
		double mmin = NumberUtils.TheNaN;
		double mmax = NumberUtils.TheNaN;
		Envelope1D interval;

		if (!envelope.isEmpty()) {
			xmin = envelope.getXMin();
			ymin = envelope.getYMin();
			xmax = envelope.getXMax();
			ymax = envelope.getYMax();

			if (b_export_zs) {
				interval = envelope.queryInterval(
						VertexDescription.Semantics.Z, 0);
				zmin = interval.vmin;
				zmax = interval.vmax;
			}

			if (b_export_ms) {
				interval = envelope.queryInterval(
						VertexDescription.Semantics.M, 0);
				mmin = interval.vmin;
				mmax = interval.vmax;
			}
		}

		if ((export_flags & WktExportFlags.wktExportMultiPolygon) != 0) {
			multiPolygonTaggedTextFromEnvelope_(precision, b_export_zs,
					b_export_ms, xmin, ymin, xmax, ymax, zmin, zmax, mmin,
					mmax, string);
		} else {
			polygonTaggedTextFromEnvelope_(precision, b_export_zs, b_export_ms,
					xmin, ymin, xmax, ymax, zmin, zmax, mmin, mmax, string);
		}
	}

	static void multiPolygonTaggedText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			int polygon_count, int path_count, StringBuilder string) {
		string.append("MULTIPOLYGON ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (position == null) {
			string.append("EMPTY");
			return;
		}

		string.append('(');

		multiPolygonText_(precision, b_export_zs, b_export_ms, zs, ms,
				position, path_flags, paths, polygon_count, path_count, string);

		string.append(')');
	}

	static void multiPolygonTaggedTextFromEnvelope_(int precision,
			boolean b_export_zs, boolean b_export_ms, double xmin, double ymin,
			double xmax, double ymax, double zmin, double zmax, double mmin,
			double mmax, StringBuilder string) {
		string.append("MULTIPOLYGON ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (NumberUtils.isNaN(xmin)) {
			string.append("EMPTY");
			return;
		}

		string.append('(');

		writeEnvelopeAsWktPolygon_(precision, b_export_zs, b_export_ms, xmin,
				ymin, xmax, ymax, zmin, zmax, mmin, mmax, string);

		string.append(')');
	}

	static void multiLineStringTaggedText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			int path_count, StringBuilder string) {
		string.append("MULTILINESTRING ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (position == null) {
			string.append("EMPTY");
			return;
		}

		string.append('(');

		multiLineStringText_(precision, b_export_zs, b_export_ms, zs, ms,
				position, path_flags, paths, path_count, string);

		string.append(')');
	}

	static void multiPointTaggedText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			int point_count, StringBuilder string) {
		string.append("MULTIPOINT ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (position == null) {
			string.append("EMPTY");
			return;
		}

		string.append('(');

		multiPointText_(precision, b_export_zs, b_export_ms, zs, ms, position,
				point_count, string);

		string.append(')');
	}

	static void multiPointTaggedTextFromPoint_(int precision,
			boolean b_export_zs, boolean b_export_ms, double x, double y,
			double z, double m, StringBuilder string) {
		string.append("MULTIPOINT ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (NumberUtils.isNaN(x)) {
			string.append("EMPTY");
			return;
		}

		string.append('(');

		pointText_(precision, b_export_zs, b_export_ms, x, y, z, m, string);

		string.append(')');
	}

	static void polygonTaggedText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			int path_count, StringBuilder string) {
		string.append("POLYGON ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (position == null) {
			string.append("EMPTY");
			return;
		}

		polygonText_(precision, b_export_zs, b_export_ms, zs, ms, position,
				path_flags, paths, 0, path_count, string);
	}

	static void polygonTaggedTextFromEnvelope_(int precision,
			boolean b_export_zs, boolean b_export_ms, double xmin, double ymin,
			double xmax, double ymax, double zmin, double zmax, double mmin,
			double mmax, StringBuilder string) {
		string.append("POLYGON ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (NumberUtils.isNaN(xmin)) {
			string.append("EMPTY");
			return;
		}

		writeEnvelopeAsWktPolygon_(precision, b_export_zs, b_export_ms, xmin,
				ymin, xmax, ymax, zmin, zmax, mmin, mmax, string);
	}

	static void lineStringTaggedText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			StringBuilder string) {
		string.append("LINESTRING ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (position == null) {
			string.append("EMPTY");
			return;
		}

		boolean b_closed = ((path_flags.read(0) & PathFlags.enumClosed) != 0);

		lineStringText_(false, b_closed, precision, b_export_zs, b_export_ms,
				zs, ms, position, paths, 0, string);
	}

	static void pointTaggedText_(int precision, boolean b_export_zs,
			boolean b_export_ms, double x, double y, double z, double m,
			StringBuilder string) {
		string.append("POINT ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (NumberUtils.isNaN(x)) {
			string.append("EMPTY");
			return;
		}

		pointText_(precision, b_export_zs, b_export_ms, x, y, z, m, string);
	}

	static void pointTaggedTextFromMultiPoint_(int precision,
			boolean b_export_zs, boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			StringBuilder string) {
		string.append("POINT ");

		if (b_export_zs && b_export_ms)
			string.append("ZM ");
		else if (b_export_zs && !b_export_ms)
			string.append("Z ");
		else if (!b_export_zs && b_export_ms)
			string.append("M ");

		if (position == null) {
			string.append("EMPTY");
			return;
		}

		pointText_(precision, b_export_zs, b_export_ms, zs, ms, position, 0,
				string);
	}

	static void multiPolygonText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			int polygon_count, int path_count, StringBuilder string) {
		int polygon_start = 0;
		int polygon_end = 1;

		while (polygon_end < path_count
				&& (path_flags.read(polygon_end) & PathFlags.enumOGCStartPolygon) == 0)
			polygon_end++;

		polygonText_(precision, b_export_zs, b_export_ms, zs, ms, position,
				path_flags, paths, polygon_start, polygon_end, string);

		for (int ipolygon = 1; ipolygon < polygon_count; ipolygon++) {
			polygon_start = polygon_end;
			polygon_end++;

			while (polygon_end < path_count
					&& (path_flags.read(polygon_end) & PathFlags.enumOGCStartPolygon) == 0)
				polygon_end++;

			string.append(", ");
			polygonText_(precision, b_export_zs, b_export_ms, zs, ms, position,
					path_flags, paths, polygon_start, polygon_end, string);
		}
	}

	static void multiLineStringText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			int path_count, StringBuilder string) {
		boolean b_closed = ((path_flags.read(0) & PathFlags.enumClosed) != 0);

		lineStringText_(false, b_closed, precision, b_export_zs, b_export_ms,
				zs, ms, position, paths, 0, string);

		for (int path = 1; path < path_count; path++) {
			string.append(", ");

			b_closed = ((path_flags.read(path) & PathFlags.enumClosed) != 0);

			lineStringText_(false, b_closed, precision, b_export_zs,
					b_export_ms, zs, ms, position, paths, path, string);
		}
	}

	static void multiPointText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			int point_count, StringBuilder string) {
		pointText_(precision, b_export_zs, b_export_ms, zs, ms, position, 0,
				string);

		for (int point = 1; point < point_count; point++) {
			string.append(", ");
			pointText_(precision, b_export_zs, b_export_ms, zs, ms, position,
					point, string);
		}
	}

	static void polygonText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt8 path_flags, AttributeStreamOfInt32 paths,
			int polygon_start, int polygon_end, StringBuilder string) {
		string.append('(');

		lineStringText_(true, true, precision, b_export_zs, b_export_ms, zs,
				ms, position, paths, polygon_start, string);

		for (int path = polygon_start + 1; path < polygon_end; path++) {
			string.append(", ");
			lineStringText_(true, true, precision, b_export_zs, b_export_ms,
					zs, ms, position, paths, path, string);
		}

		string.append(')');
	}

	static void lineStringText_(boolean bRing, boolean b_closed, int precision,
			boolean b_export_zs, boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, int path, StringBuilder string) {
		int istart = paths.read(path);
		int iend = paths.read(path + 1);

		if (istart == iend) {
			string.append("EMPTY");
			return;
		}

		string.append('(');

		if (bRing) {
			point_(precision, b_export_zs, b_export_ms, zs, ms, position,
					istart, string);
			string.append(", ");

			for (int point = iend - 1; point >= istart + 1; point--) {
				point_(precision, b_export_zs, b_export_ms, zs, ms, position,
						point, string);
				string.append(", ");
			}

			point_(precision, b_export_zs, b_export_ms, zs, ms, position,
					istart, string);
		} else {
			for (int point = istart; point < iend - 1; point++) {
				point_(precision, b_export_zs, b_export_ms, zs, ms, position,
						point, string);
				string.append(", ");
			}

			point_(precision, b_export_zs, b_export_ms, zs, ms, position,
					iend - 1, string);

			if (b_closed) {
				string.append(", ");
				point_(precision, b_export_zs, b_export_ms, zs, ms, position,
						istart, string);
			}
		}

		string.append(')');
	}

	static int pointText_(int precision, boolean b_export_zs,
			boolean b_export_ms, double x, double y, double z, double m,
			StringBuilder string) {
		string.append('(');
		point_(precision, b_export_zs, b_export_ms, x, y, z, m, string);
		string.append(')');

		return 1;
	}

	static void pointText_(int precision, boolean b_export_zs,
			boolean b_export_ms, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position, int point,
			StringBuilder string) {
		double x = position.read(2 * point);
		double y = position.read(2 * point + 1);
		double z = NumberUtils.TheNaN;
		double m = NumberUtils.TheNaN;

		if (b_export_zs)
			z = (zs != null ? zs.read(point) : VertexDescription
					.getDefaultValue(VertexDescription.Semantics.Z));

		if (b_export_ms)
			m = (ms != null ? ms.read(point) : VertexDescription
					.getDefaultValue(VertexDescription.Semantics.M));

		pointText_(precision, b_export_zs, b_export_ms, x, y, z, m, string);
	}

	static void point_(int precision, boolean b_export_zs, boolean b_export_ms,
			double x, double y, double z, double m, StringBuilder string) {
		writeSignedNumericLiteral_(x, precision, string);
		string.append(' ');
		writeSignedNumericLiteral_(y, precision, string);

		if (b_export_zs) {
			string.append(' ');
			writeSignedNumericLiteral_(z, precision, string);
		}

		if (b_export_ms) {
			string.append(' ');
			writeSignedNumericLiteral_(m, precision, string);
		}
	}

	static void point_(int precision, boolean b_export_zs, boolean b_export_ms,
			AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, int point, StringBuilder string) {
		double x = position.read(2 * point);
		double y = position.read(2 * point + 1);
		double z = NumberUtils.TheNaN;
		double m = NumberUtils.TheNaN;

		if (b_export_zs)
			z = (zs != null ? zs.read(point) : VertexDescription
					.getDefaultValue(VertexDescription.Semantics.Z));

		if (b_export_ms)
			m = (ms != null ? ms.read(point) : VertexDescription
					.getDefaultValue(VertexDescription.Semantics.M));

		point_(precision, b_export_zs, b_export_ms, x, y, z, m, string);
	}

	static boolean writeSignedNumericLiteral_(double v, int precision,
			StringBuilder string) {
		if (NumberUtils.isNaN(v)) {
			string.append("NAN");
			return false;
		}

		StringUtils.appendDouble(v, precision, string);
		return true;
	}

	static void writeEnvelopeAsWktPolygon_(int precision, boolean b_export_zs,
			boolean b_export_ms, double xmin, double ymin, double xmax,
			double ymax, double zmin, double zmax, double mmin, double mmax,
			StringBuilder string) {
		string.append("((");

		writeSignedNumericLiteral_(xmin, precision, string);
		string.append(' ');
		writeSignedNumericLiteral_(ymin, precision, string);

		if (b_export_zs) {
			string.append(' ');
			writeSignedNumericLiteral_(zmin, precision, string);
		}

		if (b_export_ms) {
			string.append(' ');
			writeSignedNumericLiteral_(mmin, precision, string);
		}

		string.append(", ");

		writeSignedNumericLiteral_(xmax, precision, string);
		string.append(' ');
		writeSignedNumericLiteral_(ymin, precision, string);

		if (b_export_zs) {
			string.append(' ');
			writeSignedNumericLiteral_(zmax, precision, string);
		}

		if (b_export_ms) {
			string.append(' ');
			writeSignedNumericLiteral_(mmax, precision, string);
		}

		string.append(", ");

		writeSignedNumericLiteral_(xmax, precision, string);
		string.append(' ');
		writeSignedNumericLiteral_(ymax, precision, string);

		if (b_export_zs) {
			string.append(' ');
			writeSignedNumericLiteral_(zmin, precision, string);
		}

		if (b_export_ms) {
			string.append(' ');
			writeSignedNumericLiteral_(mmin, precision, string);
		}

		string.append(", ");

		writeSignedNumericLiteral_(xmin, precision, string);
		string.append(' ');
		writeSignedNumericLiteral_(ymax, precision, string);

		if (b_export_zs) {
			string.append(' ');
			writeSignedNumericLiteral_(zmax, precision, string);
		}

		if (b_export_ms) {
			string.append(' ');
			writeSignedNumericLiteral_(mmax, precision, string);
		}

		string.append(", ");

		writeSignedNumericLiteral_(xmin, precision, string);
		string.append(' ');
		writeSignedNumericLiteral_(ymin, precision, string);

		if (b_export_zs) {
			string.append(' ');
			writeSignedNumericLiteral_(zmin, precision, string);
		}

		if (b_export_ms) {
			string.append(' ');
			writeSignedNumericLiteral_(mmin, precision, string);
		}

		string.append("))");
	}
}
