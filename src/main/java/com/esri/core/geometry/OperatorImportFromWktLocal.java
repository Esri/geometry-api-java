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

import java.util.ArrayList;

class OperatorImportFromWktLocal extends OperatorImportFromWkt {
	@Override
	public Geometry execute(int import_flags, Geometry.Type type,
			String wkt_string, ProgressTracker progress_tracker) {
		WktParser wkt_parser = new WktParser(wkt_string);
		int current_token = wkt_parser.nextToken();
		return importFromWkt(import_flags, type, wkt_parser);
	}

	@Override
	public OGCStructure executeOGC(int import_flags, String wkt_string,
			ProgressTracker progress_tracker) {
		ArrayList<OGCStructure> stack = new ArrayList<OGCStructure>(0);
		WktParser wkt_parser = new WktParser(wkt_string);

		OGCStructure root = new OGCStructure();
		root.m_structures = new ArrayList<OGCStructure>(0);
		stack.add(root); // add dummy root

		while (wkt_parser.nextToken() != WktParser.WktToken.not_available) {
			int current_token = wkt_parser.currentToken();

			if (current_token == WktParser.WktToken.right_paren) {
				stack.remove(stack.size() - 1);
				continue;
			}

			int ogc_type = current_token;
			OGCStructure last = stack.get(stack.size() - 1);

			if (current_token == WktParser.WktToken.geometrycollection) {
				current_token = wkt_parser.nextToken();

				if (current_token == WktParser.WktToken.attribute_z
						|| current_token == WktParser.WktToken.attribute_m
						|| current_token == WktParser.WktToken.attribute_zm)
					wkt_parser.nextToken();

				OGCStructure next = new OGCStructure();
				next.m_type = ogc_type;
				next.m_structures = new ArrayList<OGCStructure>(0);
				last.m_structures.add(next);

				if (current_token != WktParser.WktToken.empty)
					stack.add(next);
				continue;
			}

			Geometry geometry = importFromWkt(import_flags,
					Geometry.Type.Unknown, wkt_parser);

			OGCStructure leaf = new OGCStructure();
			leaf.m_type = ogc_type;
			leaf.m_geometry = geometry;
			last.m_structures.add(leaf);
		}

		return root;
	}

	static Geometry importFromWkt(int import_flags, Geometry.Type type,
			WktParser wkt_parser) {
		int current_token = wkt_parser.currentToken();

		switch (current_token) {
		case WktParser.WktToken.multipolygon:
			if (type != Geometry.Type.Polygon && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return polygonTaggedText(true, import_flags, wkt_parser);

		case WktParser.WktToken.multilinestring:
			if (type != Geometry.Type.Polyline && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return lineStringTaggedText(true, import_flags, wkt_parser);

		case WktParser.WktToken.multipoint:
			if (type != Geometry.Type.MultiPoint
					&& type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return multiPointTaggedText(import_flags, wkt_parser);

		case WktParser.WktToken.polygon:
			if (type != Geometry.Type.Polygon && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return polygonTaggedText(false, import_flags, wkt_parser);

		case WktParser.WktToken.linestring:
			if (type != Geometry.Type.Polyline && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return lineStringTaggedText(false, import_flags, wkt_parser);

		case WktParser.WktToken.point:
			if (type != Geometry.Type.Point && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return pointTaggedText(import_flags, wkt_parser);

		default:
			break; // warning fix
		}

		return null;
	}

	static Geometry polygonTaggedText(boolean b_multi_polygon,
			int import_flags, WktParser wkt_parser) {
		MultiPath multi_path;
		MultiPathImpl multi_path_impl;
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

		multi_path = new Polygon();
		multi_path_impl = (MultiPathImpl) multi_path._getImpl();

		int current_token = wkt_parser.nextToken();

		if (current_token == WktParser.WktToken.attribute_z) {
			zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_path_impl.addAttribute(VertexDescription.Semantics.Z);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_m) {
			ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_path_impl.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_zm) {
			zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_path_impl.addAttribute(VertexDescription.Semantics.Z);
			multi_path_impl.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		}

		int point_count;

		if (b_multi_polygon)
			point_count = multiPolygonText(zs, ms, position, paths, path_flags,
					wkt_parser);
		else
			point_count = polygonText(zs, ms, position, paths, path_flags, 0,
					wkt_parser);

		if (point_count != 0) {
			assert (2 * point_count == position.size());
			multi_path_impl.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);
			multi_path_impl.setPathStreamRef(paths);
			multi_path_impl.setPathFlagsStreamRef(path_flags);

			if (zs != null)
				multi_path_impl.setAttributeStreamRef(
						VertexDescription.Semantics.Z, zs);

			if (ms != null)
				multi_path_impl.setAttributeStreamRef(
						VertexDescription.Semantics.M, ms);

			multi_path_impl.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);

			AttributeStreamOfInt8 path_flags_clone = new AttributeStreamOfInt8(
					path_flags);

			for (int i = 0; i < path_flags_clone.size() - 1; i++) {
				if (((int) path_flags_clone.read(i) & (int) PathFlags.enumOGCStartPolygon) != 0) {// Should
																									// be
																									// clockwise
					if (!InternalUtils.isClockwiseRing(multi_path_impl, i))
						multi_path_impl.reversePath(i); // make clockwise
				} else {// Should be counter-clockwise
					if (InternalUtils.isClockwiseRing(multi_path_impl, i))
						multi_path_impl.reversePath(i); // make
														// counter-clockwise
				}
			}

			multi_path_impl.setPathFlagsStreamRef(path_flags_clone);
		}

		if ((import_flags & (int) WktImportFlags.wktImportNonTrusted) == 0)
			multi_path_impl.setIsSimple(MultiPathImpl.GeometryXSimple.Weak,
					0.0, false);

		multi_path_impl.setDirtyOGCFlags(false);

		return multi_path;
	}

	static Geometry lineStringTaggedText(boolean b_multi_linestring,
			int import_flags, WktParser wkt_parser) {
		MultiPath multi_path;
		MultiPathImpl multi_path_impl;
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

		multi_path = new Polyline();
		multi_path_impl = (MultiPathImpl) multi_path._getImpl();

		int current_token = wkt_parser.nextToken();

		if (current_token == WktParser.WktToken.attribute_z) {
			zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_path_impl.addAttribute(VertexDescription.Semantics.Z);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_m) {
			ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_path_impl.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_zm) {
			zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_path_impl.addAttribute(VertexDescription.Semantics.Z);
			multi_path_impl.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		}

		int point_count;

		if (b_multi_linestring)
			point_count = multiLineStringText(zs, ms, position, paths,
					path_flags, wkt_parser);
		else
			point_count = lineStringText(false, zs, ms, position, paths,
					path_flags, wkt_parser);

		if (point_count != 0) {
			assert (2 * point_count == position.size());
			multi_path_impl.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);
			multi_path_impl.setPathStreamRef(paths);
			multi_path_impl.setPathFlagsStreamRef(path_flags);

			if (zs != null)
				multi_path_impl.setAttributeStreamRef(
						VertexDescription.Semantics.Z, zs);

			if (ms != null)
				multi_path_impl.setAttributeStreamRef(
						VertexDescription.Semantics.M, ms);

			multi_path_impl.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);
		}

		return multi_path;
	}

	static Geometry multiPointTaggedText(int import_flags, WktParser wkt_parser) {
		MultiPoint multi_point;
		MultiPointImpl multi_point_impl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;

		position = (AttributeStreamOfDbl) AttributeStreamBase
				.createDoubleStream(0);

		multi_point = new MultiPoint();
		multi_point_impl = (MultiPointImpl) multi_point._getImpl();

		int current_token = wkt_parser.nextToken();

		if (current_token == WktParser.WktToken.attribute_z) {
			zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_point_impl.addAttribute(VertexDescription.Semantics.Z);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_m) {
			ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_point_impl.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_zm) {
			zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(
					0, NumberUtils.TheNaN);
			multi_point_impl.addAttribute(VertexDescription.Semantics.Z);
			multi_point_impl.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		}

		int point_count = multiPointText(zs, ms, position, wkt_parser);

		if (point_count != 0) {
			assert (2 * point_count == position.size());
			multi_point_impl.resize(point_count);
			multi_point_impl.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);

			if (zs != null)
				multi_point_impl.setAttributeStreamRef(
						VertexDescription.Semantics.Z, zs);

			if (ms != null)
				multi_point_impl.setAttributeStreamRef(
						VertexDescription.Semantics.M, ms);

			multi_point_impl.notifyModified(MultiPointImpl.DirtyFlags.DirtyAll);
		}

		return multi_point;
	}

	static Geometry pointTaggedText(int import_flags, WktParser wkt_parser) {
		Point point = new Point();

		int current_token = wkt_parser.nextToken();

		if (current_token == WktParser.WktToken.attribute_z) {
			point.addAttribute(VertexDescription.Semantics.Z);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_m) {
			point.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		} else if (current_token == WktParser.WktToken.attribute_zm) {
			point.addAttribute(VertexDescription.Semantics.Z);
			point.addAttribute(VertexDescription.Semantics.M);
			wkt_parser.nextToken();
		}
		// At start of PointText

		current_token = wkt_parser.currentToken();

		if (current_token != WktParser.WktToken.empty) {
			wkt_parser.nextToken();

			double x = wkt_parser.currentNumericLiteral();
			wkt_parser.nextToken();

			double y = wkt_parser.currentNumericLiteral();
			wkt_parser.nextToken();

			point.setXY(x, y);

			if (wkt_parser.hasZs()) {
				double z = wkt_parser.currentNumericLiteral();
				wkt_parser.nextToken();
				point.setZ(z);
			}

			if (wkt_parser.hasMs()) {
				double m = wkt_parser.currentNumericLiteral();
				wkt_parser.nextToken();
				point.setM(m);
			}
		}

		return point;
	}

	static int multiPolygonText(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			WktParser wkt_parser) {
		// At start of MultiPolygonText

		int current_token = wkt_parser.currentToken();

		int total_point_count = 0;

		if (current_token == WktParser.WktToken.empty)
			return total_point_count;

		current_token = wkt_parser.nextToken();

		while (current_token != WktParser.WktToken.right_paren) {
			// At start of PolygonText

			total_point_count = polygonText(zs, ms, position, paths,
					path_flags, total_point_count, wkt_parser);
			current_token = wkt_parser.nextToken();
		}

		return total_point_count;
	}

	static int multiLineStringText(AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			WktParser wkt_parser) {
		// At start of MultiLineStringText

		int current_token = wkt_parser.currentToken();

		int total_point_count = 0;

		if (current_token == WktParser.WktToken.empty)
			return total_point_count;

		current_token = wkt_parser.nextToken();

		while (current_token != WktParser.WktToken.right_paren) {
			// At start of LineStringText

			int point_count = lineStringText(false, zs, ms, position, paths,
					path_flags, wkt_parser);
			total_point_count += point_count;

			current_token = wkt_parser.nextToken();
		}

		return total_point_count;
	}

	static int multiPointText(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, WktParser wkt_parser) {
		// At start of MultiPointText

		int current_token = wkt_parser.currentToken();

		int point_count = 0;

		if (current_token == WktParser.WktToken.empty)
			return point_count;

		current_token = wkt_parser.nextToken();

		while (current_token != WktParser.WktToken.right_paren) {
			// At start of PointText

			point_count += pointText(zs, ms, position, wkt_parser);

			if (current_token == WktParser.WktToken.left_paren
					|| current_token == WktParser.WktToken.empty)
				current_token = wkt_parser.nextToken(); // ogc standard
			else
				current_token = wkt_parser.currentToken(); // not ogc standard.
															// treat as
															// linestring
		}

		return point_count;
	}

	static int polygonText(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, AttributeStreamOfInt32 paths,
			AttributeStreamOfInt8 path_flags, int total_point_count,
			WktParser wkt_parser) {
		// At start of PolygonText

		int current_token = wkt_parser.currentToken();

		if (current_token == WktParser.WktToken.empty)
			return total_point_count;

		boolean b_first_line_string = true;

		current_token = wkt_parser.nextToken();

		while (current_token != WktParser.WktToken.right_paren) {
			// At start of LineStringText

			int point_count = lineStringText(true, zs, ms, position, paths,
					path_flags, wkt_parser);

			if (point_count != 0) {
				if (b_first_line_string) {
					b_first_line_string = false;
					path_flags.setBits(path_flags.size() - 2,
							(byte) PathFlags.enumOGCStartPolygon);
				}

				path_flags.setBits(path_flags.size() - 2,
						(byte) PathFlags.enumClosed);
				total_point_count += point_count;
			}

			current_token = wkt_parser.nextToken();
		}

		return total_point_count;
	}

	static int lineStringText(boolean b_ring, AttributeStreamOfDbl zs,
			AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			WktParser wkt_parser) {
		// At start of LineStringText

		int current_token = wkt_parser.currentToken();

		int point_count = 0;

		if (current_token == WktParser.WktToken.empty)
			return point_count;

		boolean b_start_path = true;
		double startx = NumberUtils.TheNaN;
		double starty = NumberUtils.TheNaN;
		double startz = NumberUtils.TheNaN;
		double startm = NumberUtils.TheNaN;

		current_token = wkt_parser.nextToken();

		while (current_token != WktParser.WktToken.right_paren) {
			// At start of x

			double x = wkt_parser.currentNumericLiteral();
			wkt_parser.nextToken();

			double y = wkt_parser.currentNumericLiteral();
			wkt_parser.nextToken();

			double z = NumberUtils.TheNaN, m = NumberUtils.TheNaN;

			if (wkt_parser.hasZs()) {
				z = wkt_parser.currentNumericLiteral();
				wkt_parser.nextToken();
			}

			if (wkt_parser.hasMs()) {
				m = wkt_parser.currentNumericLiteral();
				wkt_parser.nextToken();
			}

			current_token = wkt_parser.currentToken();
			boolean b_add_point = true;

			if (b_ring && point_count >= 2
					&& current_token == WktParser.WktToken.right_paren) {
				// If the last point in the ring is not equal to the start
				// point, then let's add it.

				if ((startx == x || (NumberUtils.isNaN(startx) && NumberUtils
						.isNaN(x)))
						&& (starty == y || (NumberUtils.isNaN(starty) && NumberUtils
								.isNaN(y)))
						&& (!wkt_parser.hasZs() || startz == z || (NumberUtils
								.isNaN(startz) && NumberUtils.isNaN(z)))
						&& (!wkt_parser.hasMs() || startm == m || (NumberUtils
								.isNaN(startm) && NumberUtils.isNaN(m))))
					b_add_point = false;
			}

			if (b_add_point) {
				if (b_start_path) {
					b_start_path = false;
					startx = x;
					starty = y;
					startz = z;
					startm = m;
				}

				point_count++;
				addToStreams(zs, ms, position, x, y, z, m);
			}
		}

		if (point_count == 1) {
			point_count++;
			addToStreams(zs, ms, position, startx, starty, startz, startm);
		}

		paths.add(position.size() / 2);
		path_flags.add((byte) 0);

		return point_count;
	}

	static int pointText(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, WktParser wkt_parser) {
		// At start of PointText

		int current_token = wkt_parser.currentToken();

		if (current_token == WktParser.WktToken.empty)
			return 0;

		if (current_token == WktParser.WktToken.left_paren)
			wkt_parser.nextToken(); // ogc standard

		// At start of x

		double x = wkt_parser.currentNumericLiteral();
		wkt_parser.nextToken();

		double y = wkt_parser.currentNumericLiteral();
		wkt_parser.nextToken();

		double z = NumberUtils.TheNaN;
		double m = NumberUtils.TheNaN;

		if (zs != null) {
			z = wkt_parser.currentNumericLiteral();
			wkt_parser.nextToken();
		}

		if (ms != null) {
			m = wkt_parser.currentNumericLiteral();
			wkt_parser.nextToken();
		}

		addToStreams(zs, ms, position, x, y, z, m);

		return 1;
	}

	static void addToStreams(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, double x, double y, double z,
			double m) {
		position.add(x);
		position.add(y);

		if (zs != null)
			zs.add(z);

		if (ms != null)
			ms.add(m);
	}
}
