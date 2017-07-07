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

import com.esri.core.geometry.MultiVertexGeometryImpl.DirtyFlags;
import com.esri.core.geometry.VertexDescription.Semantics;

class OperatorImportFromJsonCursor extends MapGeometryCursor {
	JsonReaderCursor m_inputJsonParsers;

	int m_type;

	int m_index;

	public OperatorImportFromJsonCursor(int type, JsonReaderCursor jsonParsers) {
		m_index = -1;
		if (jsonParsers == null)
			throw new IllegalArgumentException();

		m_type = type;
		m_inputJsonParsers = jsonParsers;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	@Override
	public MapGeometry next() {
		JsonReader jsonParser;
		if ((jsonParser = m_inputJsonParsers.next()) != null) {
			m_index = m_inputJsonParsers.getID();
			return importFromJsonParser(m_type, jsonParser);
		}
		return null;
	}

	static MapGeometry importFromJsonParser(int gt, JsonReader parser) {
		MapGeometry mp;

		try {
			if (!JSONUtils.isObjectStart(parser))
				return null;

			boolean bFoundSpatial_reference = false;
			boolean bFoundHasZ = false;
			boolean bFoundHasM = false;
			boolean bFoundPolygon = false;
			boolean bFoundPolyline = false;
			boolean bFoundMultiPoint = false;
			boolean bFoundX = false;
			boolean bFoundY = false;
			boolean bFoundZ = false;
			boolean bFoundM = false;
			boolean bFoundXMin = false;
			boolean bFoundYMin = false;
			boolean bFoundXMax = false;
			boolean bFoundYMax = false;
			boolean bFoundZMin = false;
			boolean bFoundZMax = false;
			boolean bFoundMMin = false;
			boolean bFoundMMax = false;
			double x = NumberUtils.NaN();
			double y = NumberUtils.NaN();
			double z = NumberUtils.NaN();
			double m = NumberUtils.NaN();
			double xmin = NumberUtils.NaN();
			double ymin = NumberUtils.NaN();
			double xmax = NumberUtils.NaN();
			double ymax = NumberUtils.NaN();
			double zmin = NumberUtils.NaN();
			double zmax = NumberUtils.NaN();
			double mmin = NumberUtils.NaN();
			double mmax = NumberUtils.NaN();
			boolean bHasZ = false;
			boolean bHasM = false;
			AttributeStreamOfDbl as = (AttributeStreamOfDbl) AttributeStreamBase
					.createDoubleStream(0);
			AttributeStreamOfDbl bs = (AttributeStreamOfDbl) AttributeStreamBase
					.createDoubleStream(0);

			Geometry geometry = null;
			SpatialReference spatial_reference = null;

			while (parser.nextToken() != JsonReader.Token.END_OBJECT) {
				String name = parser.currentString();
				parser.nextToken();

				if (!bFoundSpatial_reference && name.equals("spatialReference")) {
					bFoundSpatial_reference = true;

					if (parser.currentToken() == JsonReader.Token.START_OBJECT) {
						spatial_reference = SpatialReference.fromJson(parser);
					} else {
						if (parser.currentToken() != JsonReader.Token.VALUE_NULL)
							throw new GeometryException(
									"failed to parse spatial reference: object or null is expected");
					}
				} else if (!bFoundHasZ && name.equals("hasZ")) {
					bFoundHasZ = true;
					bHasZ = (parser.currentToken() == JsonReader.Token.VALUE_TRUE);
				} else if (!bFoundHasM && name.equals("hasM")) {
					bFoundHasM = true;
					bHasM = (parser.currentToken() == JsonReader.Token.VALUE_TRUE);
				} else if (!bFoundPolygon
						&& name.equals("rings")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Polygon)) {
					bFoundPolygon = true;
					geometry = importFromJsonMultiPath(true, parser, as, bs);
					continue;
				} else if (!bFoundPolyline
						&& name.equals("paths")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Polyline)) {
					bFoundPolyline = true;
					geometry = importFromJsonMultiPath(false, parser, as, bs);
					continue;
				} else if (!bFoundMultiPoint
						&& name.equals("points")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.MultiPoint)) {
					bFoundMultiPoint = true;
					geometry = importFromJsonMultiPoint(parser, as, bs);
					continue;
				} else if (!bFoundX
						&& name.equals("x")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Point)) {
					bFoundX = true;
					x = readDouble(parser);
				} else if (!bFoundY
						&& name.equals("y")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Point)) {
					bFoundY = true;
					y = readDouble(parser);
				} else if (!bFoundZ
						&& name.equals("z")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Point)) {
					bFoundZ = true;
					z = readDouble(parser);
				} else if (!bFoundM
						&& name.equals("m")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Point)) {
					bFoundM = true;
					m = readDouble(parser);
				}
				if (!bFoundXMin
						&& name.equals("xmin")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundXMin = true;
					xmin = readDouble(parser);
				} else if (!bFoundYMin
						&& name.equals("ymin")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundYMin = true;
					ymin = readDouble(parser);
				} else if (!bFoundMMin
						&& name.equals("mmin")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundMMin = true;
					mmin = readDouble(parser);
				} else if (!bFoundZMin
						&& name.equals("zmin")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundZMin = true;
					zmin = readDouble(parser);
				} else if (!bFoundXMax
						&& name.equals("xmax")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundXMax = true;
					xmax = readDouble(parser);
				} else if (!bFoundYMax
						&& name.equals("ymax")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundYMax = true;
					ymax = readDouble(parser);
				} else if (!bFoundMMax
						&& name.equals("mmax")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundMMax = true;
					mmax = readDouble(parser);
				} else if (!bFoundZMax
						&& name.equals("zmax")
						&& (gt == Geometry.GeometryType.Unknown || gt == Geometry.GeometryType.Envelope)) {
					bFoundZMax = true;
					zmax = readDouble(parser);
				} else {
					windup(parser);
				}
			}

			if (bFoundPolygon || bFoundPolyline || bFoundMultiPoint) {
				assert (geometry != null);
				MultiVertexGeometryImpl mvImpl = (MultiVertexGeometryImpl) geometry
						._getImpl();

				AttributeStreamBase zs = null;
				AttributeStreamBase ms = null;

				if (bHasZ) {
					geometry.addAttribute(Semantics.Z);
					zs = as;
				}
				if (bHasM) {
					geometry.addAttribute(Semantics.M);
					ms = !bHasZ ? as : bs;
				}

				if (bHasZ && zs != null) {
					mvImpl.setAttributeStreamRef(Semantics.Z, zs);
				}

				if (bHasM && ms != null) {
					mvImpl.setAttributeStreamRef(Semantics.M, ms);
				}

				mvImpl.notifyModified(DirtyFlags.DirtyAll);
			} else if (bFoundX || bFoundY || bFoundY || bFoundZ) {
				if (NumberUtils.isNaN(y))
					x = NumberUtils.NaN();

				Point p = new Point(x, y);

				if (bFoundZ)
					p.setZ(z);

				if (bFoundM)
					p.setM(m);

				geometry = p;
			} else if (bFoundXMin || bFoundYMin || bFoundXMax || bFoundYMax
					|| bFoundZMin || bFoundZMax || bFoundMMin || bFoundMMax) {
				if (NumberUtils.isNaN(ymin) || NumberUtils.isNaN(xmax)
						|| NumberUtils.isNaN(ymax))
					xmin = NumberUtils.NaN();

				Envelope e = new Envelope(xmin, ymin, xmax, ymax);

				if (bFoundZMin && bFoundZMax)
					e.setInterval(Semantics.Z, 0, zmin, zmax);

				if (bFoundMMin && bFoundMMax)
					e.setInterval(Semantics.M, 0, mmin, mmax);

				geometry = e;
			}

			mp = new MapGeometry(geometry, spatial_reference);

		} catch (Exception e) {
			return null;
		}

		return mp;
	}

	public static MapGeometry fromJsonToUnknown(JsonReader parser)
			throws Exception {

		return importFromJsonParser(Geometry.GeometryType.Unknown, parser);
	}

	public static MapGeometry fromJsonToEnvelope(JsonReader parser)
			throws Exception {
		return importFromJsonParser(Geometry.GeometryType.Envelope, parser);
	}

	public static MapGeometry fromJsonToPoint(JsonReader parser)
			throws Exception {
		return importFromJsonParser(Geometry.GeometryType.Point, parser);
	}

	public static MapGeometry fromJsonToPolygon(JsonReader parser)
			throws Exception {
		return importFromJsonParser(Geometry.GeometryType.Polygon, parser);
	}

	public static MapGeometry fromJsonToPolyline(JsonReader parser)
			throws Exception {
		return importFromJsonParser(Geometry.GeometryType.Polyline, parser);
	}

	public static MapGeometry fromJsonToMultiPoint(JsonReader parser)
			throws Exception {
		return importFromJsonParser(Geometry.GeometryType.MultiPoint, parser);
	}

	private static void windup(JsonReader parser) {
		parser.skipChildren();
	}

	private static double readDouble(JsonReader parser) {
		if (parser.currentToken() == JsonReader.Token.VALUE_NULL
				|| parser.currentToken() == JsonReader.Token.VALUE_STRING
				&& parser.currentString().equals("NaN"))
			return NumberUtils.NaN();
		else
			return parser.currentDoubleValue();
	}

	private static Geometry importFromJsonMultiPoint(JsonReader parser,
			AttributeStreamOfDbl as, AttributeStreamOfDbl bs) throws Exception {
		if (parser.currentToken() != JsonReader.Token.START_ARRAY)
			throw new GeometryException(
					"failed to parse multipoint: array of vertices is expected");

		int point_count = 0;
		MultiPoint multipoint;

		multipoint = new MultiPoint();

		AttributeStreamOfDbl position = (AttributeStreamOfDbl) (AttributeStreamBase
				.createDoubleStream(2, 0));

		// At start of rings
		int sz;
		double[] buf = new double[4];
		while (parser.nextToken() != JsonReader.Token.END_ARRAY) {
			if (parser.currentToken() != JsonReader.Token.START_ARRAY)
				throw new GeometryException(
						"failed to parse multipoint: array is expected, multipoint vertices consist of arrays of cooridinates");

			sz = 0;
			while (parser.nextToken() != JsonReader.Token.END_ARRAY) {
				buf[sz++] = readDouble(parser);
			}

			if (sz < 2)
				throw new GeometryException(
						"failed to parse multipoint: each vertex array has to have at least 2 elements");

			if (position.size() == 2 * point_count) {
				int c = point_count * 3;
				if (c % 2 != 0)
					c++;// have to be even
				position.resize(c);
			}

			position.write(2 * point_count, buf[0]);
			position.write(2 * point_count + 1, buf[1]);

			if (as.size() == point_count) {
				int c = (point_count * 3) / 2;
				if (c < 4)
					c = 4;
				else if (c < 16)
					c = 16;

				as.resize(c);
			}

			if (sz > 2) {
				as.write(point_count, buf[2]);
			} else
				as.write(point_count, NumberUtils.NaN());

			if (bs.size() == point_count) {
				int c = (point_count * 3) / 2;
				if (c < 4)
					c = 4;
				else if (c < 16)
					c = 16;

				bs.resize(c);
			}

			if (sz > 3) {
				bs.write(point_count, buf[3]);
			} else
				bs.write(point_count, NumberUtils.NaN());

			point_count++;
		}

		if (point_count != 0) {
			MultiPointImpl mp_impl = (MultiPointImpl) multipoint._getImpl();
			mp_impl.resize(point_count);
			mp_impl.setAttributeStreamRef(Semantics.POSITION, position);
		}
		return multipoint;
	}

	private static Geometry importFromJsonMultiPath(boolean b_polygon,
			JsonReader parser, AttributeStreamOfDbl as, AttributeStreamOfDbl bs)
			throws Exception {
		if (parser.currentToken() != JsonReader.Token.START_ARRAY)
			throw new GeometryException(
					"failed to parse multipath: array of array of vertices is expected");

		MultiPath multipath;

		if (b_polygon)
			multipath = new Polygon();
		else
			multipath = new Polyline();

		AttributeStreamOfInt32 parts = (AttributeStreamOfInt32) AttributeStreamBase
				.createIndexStream(0);
		AttributeStreamOfDbl position = (AttributeStreamOfDbl) AttributeStreamBase
				.createDoubleStream(2, 0);
		AttributeStreamOfInt8 pathFlags = (AttributeStreamOfInt8) AttributeStreamBase
				.createByteStream(0);

		// set up min max variables
		double[] buf = new double[4];
		double[] start = new double[4];

		int point_count = 0;
		int path_count = 0;
		byte pathFlag = b_polygon ? (byte) PathFlags.enumClosed : 0;
		int requiredSize = b_polygon ? 3 : 2;

		// At start of rings
		while (parser.nextToken() != JsonReader.Token.END_ARRAY) {
			if (parser.currentToken() != JsonReader.Token.START_ARRAY)
				throw new GeometryException(
						"failed to parse multipath: ring/path array is expected");

			int pathPointCount = 0;
			boolean b_first = true;
			int sz = 0;
			int szstart = 0;

			parser.nextToken();
			while (parser.currentToken() != JsonReader.Token.END_ARRAY) {
				if (parser.currentToken() != JsonReader.Token.START_ARRAY)
					throw new GeometryException(
							"failed to parse multipath: array is expected, rings/paths vertices consist of arrays of cooridinates");

				sz = 0;
				while (parser.nextToken() != JsonReader.Token.END_ARRAY) {
					buf[sz++] = readDouble(parser);
				}

				if (sz < 2)
					throw new GeometryException(
							"failed to parse multipath: each vertex array has to have at least 2 elements");

				parser.nextToken();

				do {
					if (position.size() == point_count * 2) {
						int c = point_count * 3;

						if (c % 2 != 0)
							c++;// have to be even
						if (c < 8)
							c = 8;
						else if (c < 32)
							c = 32;

						position.resize(c);
					}

					position.write(2 * point_count, buf[0]);
					position.write(2 * point_count + 1, buf[1]);

					if (as.size() == point_count) {
						int c = (point_count * 3) / 2;// have to be even
						if (c < 4)
							c = 4;
						else if (c < 16)
							c = 16;
						as.resize(c);
					}

					if (sz > 2) {
						as.write(point_count, buf[2]);
					} else
						as.write(point_count, NumberUtils.NaN());

					if (bs.size() == point_count) {
						int c = (point_count * 3) / 2;// have to be even
						if (c < 4)
							c = 4;
						else if (c < 16)
							c = 16;
						bs.resize(c);
					}

					if (sz > 3) {
						bs.write(point_count, buf[3]);
					} else
						bs.write(point_count, NumberUtils.NaN());

					if (b_first) {
						path_count++;
						parts.add(point_count);
						pathFlags.add(pathFlag);
						b_first = false;
						szstart = sz;
						start[0] = buf[0];
						start[1] = buf[1];
						start[2] = buf[2];
						start[3] = buf[3];
					}
					point_count++;
					pathPointCount++;
				} while (pathPointCount < requiredSize
						&& parser.currentToken() == JsonReader.Token.END_ARRAY);
			}

			if (b_polygon && pathPointCount > requiredSize && sz == szstart
					&& start[0] == buf[0] && start[1] == buf[1]
					&& start[2] == buf[2] && start[3] == buf[3]) {
				// remove the end point that is equal to the start point.
				point_count--;
				pathPointCount--;
			}

			if (pathPointCount == 0)
				continue;// skip empty paths
		}

		if (point_count != 0) {
			parts.resize(path_count);
			pathFlags.resize(path_count);

			if (point_count > 0) {
				parts.add(point_count);
				pathFlags.add((byte) 0);
			}

			MultiPathImpl mp_impl = (MultiPathImpl) multipath._getImpl();
			mp_impl.setAttributeStreamRef(Semantics.POSITION, position);
			mp_impl.setPathFlagsStreamRef(pathFlags);
			mp_impl.setPathStreamRef(parts);
		}
		return multipath;
	}

}
