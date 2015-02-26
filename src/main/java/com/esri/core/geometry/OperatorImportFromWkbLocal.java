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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * OperatorImportFromWkbLocal implementation.
 */
class OperatorImportFromWkbLocal extends OperatorImportFromWkb {

	static final class WkbHelper {
		WkbHelper(ByteBuffer buffer) {
			wkbBuffer = buffer;
			adjustment = 0;
		}

		int getInt(int offset) {
			return wkbBuffer.getInt(adjustment + offset);
		}

		double getDouble(int offset) {
			return wkbBuffer.getDouble(adjustment + offset);
		}

		ByteBuffer wkbBuffer;
		int adjustment;
	}

	@Override
	public Geometry execute(int importFlags, Geometry.Type type,
			ByteBuffer wkbBuffer, ProgressTracker progress_tracker) {

		ByteOrder initialOrder = wkbBuffer.order();

		// read byte ordering
		int byteOrder = wkbBuffer.get(0);

		if (byteOrder == WkbByteOrder.wkbNDR)
			wkbBuffer.order(ByteOrder.LITTLE_ENDIAN);
		else
			wkbBuffer.order(ByteOrder.BIG_ENDIAN);

		WkbHelper wkbHelper = new WkbHelper(wkbBuffer);

		try {
			return importFromWkb(importFlags, type, wkbHelper);
		} finally {
			wkbBuffer.order(initialOrder);
		}
	}

	@Override
	public OGCStructure executeOGC(int importFlags, ByteBuffer wkbBuffer,
			ProgressTracker progress_tracker) {

		ByteOrder initialOrder = wkbBuffer.order();

		// read byte ordering
		int byteOrder = wkbBuffer.get(0);

		if (byteOrder == WkbByteOrder.wkbNDR)
			wkbBuffer.order(ByteOrder.LITTLE_ENDIAN);
		else
			wkbBuffer.order(ByteOrder.BIG_ENDIAN);

		ArrayList<OGCStructure> stack = new ArrayList<OGCStructure>(0);
		AttributeStreamOfInt32 numGeometries = new AttributeStreamOfInt32(0);
		AttributeStreamOfInt32 indices = new AttributeStreamOfInt32(0);
		WkbHelper wkbHelper = new WkbHelper(wkbBuffer);

		OGCStructure root = new OGCStructure();
		root.m_structures = new ArrayList<OGCStructure>(0);
		stack.add(root); // add dummy root
		numGeometries.add(1);
		indices.add(0);

		boolean bCheckConsistentAttributes = false;
		boolean bHasZs = false;
		boolean bHasMs = false;

		try {

			while (!stack.isEmpty()) {

				if (indices.getLast() == numGeometries.getLast()) {
					stack.remove(stack.size() - 1);
					indices.removeLast();
					numGeometries.removeLast();
					continue;
				}

				OGCStructure last = stack.get(stack.size() - 1);
				indices.write(indices.size() - 1, indices.getLast() + 1);
				Geometry geometry;

				int wkbType = wkbHelper.getInt(1);
				int ogcType;

				// strip away attributes from type identifier

				if (wkbType > 3000) {
					ogcType = wkbType - 3000;

					if (bCheckConsistentAttributes) {
						if (!bHasZs || !bHasMs)
							throw new IllegalArgumentException();
					} else {
						bHasZs = true;
						bHasMs = true;
						bCheckConsistentAttributes = true;
					}
				} else if (wkbType > 2000) {
					ogcType = wkbType - 2000;

					if (bCheckConsistentAttributes) {
						if (bHasZs || !bHasMs)
							throw new IllegalArgumentException();
					} else {
						bHasZs = false;
						bHasMs = true;
						bCheckConsistentAttributes = true;
					}
				} else if (wkbType > 1000) {
					ogcType = wkbType - 1000;

					if (bCheckConsistentAttributes) {
						if (!bHasZs || bHasMs)
							throw new IllegalArgumentException();
					} else {
						bHasZs = true;
						bHasMs = false;
						bCheckConsistentAttributes = true;
					}
				} else {
					ogcType = wkbType;

					if (bCheckConsistentAttributes) {
						if (bHasZs || bHasMs)
							throw new IllegalArgumentException();
					} else {
						bHasZs = false;
						bHasMs = false;
						bCheckConsistentAttributes = true;
					}
				}
				if (ogcType == 7) {
					int count = wkbHelper.getInt(5);
					wkbHelper.adjustment += 9;

					OGCStructure next = new OGCStructure();
					next.m_type = ogcType;
					next.m_structures = new ArrayList<OGCStructure>(0);
					last.m_structures.add(next);
					stack.add(next);
					indices.add(0);
					numGeometries.add(count);
				} else {
					geometry = importFromWkb(importFlags,
							Geometry.Type.Unknown, wkbHelper);
					OGCStructure leaf = new OGCStructure();
					leaf.m_type = ogcType;
					leaf.m_geometry = geometry;
					last.m_structures.add(leaf);
				}
			}
		} finally {
			wkbBuffer.order(initialOrder);
		}

		return root;
	}

	private static Geometry importFromWkb(int importFlags, Geometry.Type type,
			WkbHelper wkbHelper) {

		// read type
		int wkbType = wkbHelper.getInt(1);

		switch (wkbType) {
		case WkbGeometryType.wkbPolygon:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(false, importFlags, false, false,
					wkbHelper);

		case WkbGeometryType.wkbPolygonM:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(false, importFlags, false, true,
					wkbHelper);

		case WkbGeometryType.wkbPolygonZ:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(false, importFlags, true, false,
					wkbHelper);

		case WkbGeometryType.wkbPolygonZM:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(false, importFlags, true, true,
					wkbHelper);

		case WkbGeometryType.wkbMultiPolygon:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(true, importFlags, false, false,
					wkbHelper);

		case WkbGeometryType.wkbMultiPolygonM:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(true, importFlags, false, true,
					wkbHelper);

		case WkbGeometryType.wkbMultiPolygonZ:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(true, importFlags, true, false,
					wkbHelper);

		case WkbGeometryType.wkbMultiPolygonZM:
			if (type.value() != Geometry.GeometryType.Polygon
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolygon(true, importFlags, true, true,
					wkbHelper);

		case WkbGeometryType.wkbLineString:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(false, importFlags, false, false,
					wkbHelper);

		case WkbGeometryType.wkbLineStringM:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(false, importFlags, false, true,
					wkbHelper);

		case WkbGeometryType.wkbLineStringZ:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(false, importFlags, true, false,
					wkbHelper);

		case WkbGeometryType.wkbLineStringZM:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(false, importFlags, true, true,
					wkbHelper);

		case WkbGeometryType.wkbMultiLineString:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(true, importFlags, false, false,
					wkbHelper);

		case WkbGeometryType.wkbMultiLineStringM:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(true, importFlags, false, true,
					wkbHelper);

		case WkbGeometryType.wkbMultiLineStringZ:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(true, importFlags, true, false,
					wkbHelper);

		case WkbGeometryType.wkbMultiLineStringZM:
			if (type.value() != Geometry.GeometryType.Polyline
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPolyline(true, importFlags, true, true,
					wkbHelper);

		case WkbGeometryType.wkbMultiPoint:
			if (type.value() != Geometry.GeometryType.MultiPoint
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbMultiPoint(importFlags, false, false, wkbHelper);

		case WkbGeometryType.wkbMultiPointM:
			if (type.value() != Geometry.GeometryType.MultiPoint
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbMultiPoint(importFlags, false, true, wkbHelper);

		case WkbGeometryType.wkbMultiPointZ:
			if (type.value() != Geometry.GeometryType.MultiPoint
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbMultiPoint(importFlags, true, false, wkbHelper);

		case WkbGeometryType.wkbMultiPointZM:
			if (type.value() != Geometry.GeometryType.MultiPoint
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbMultiPoint(importFlags, true, true, wkbHelper);

		case WkbGeometryType.wkbPoint:
			if (type.value() != Geometry.GeometryType.Point
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPoint(importFlags, false, false, wkbHelper);

		case WkbGeometryType.wkbPointM:
			if (type.value() != Geometry.GeometryType.Point
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPoint(importFlags, false, true, wkbHelper);

		case WkbGeometryType.wkbPointZ:
			if (type.value() != Geometry.GeometryType.Point
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPoint(importFlags, true, false, wkbHelper);

		case WkbGeometryType.wkbPointZM:
			if (type.value() != Geometry.GeometryType.Point
					&& type.value() != Geometry.GeometryType.Unknown)
				throw new GeometryException("invalid shape type");
			return importFromWkbPoint(importFlags, true, true, wkbHelper);

		default:
			throw new GeometryException("invalid shape type");
		}
	}

	private static Geometry importFromWkbPolygon(boolean bMultiPolygon,
			int importFlags, boolean bZs, boolean bMs, WkbHelper wkbHelper) {
		int offset;
		int polygonCount;

		if (bMultiPolygon) {
			polygonCount = wkbHelper.getInt(5);
			offset = 9;
		} else {
			polygonCount = 1;
			offset = 0;
		}

		// Find total point count and part count
		int point_count = 0;
		int partCount = 0;
		int tempOffset = offset;
		for (int ipolygon = 0; ipolygon < polygonCount; ipolygon++) {
			tempOffset += 5; // skip redundant byte order and type fields
			int ipartcount = wkbHelper.getInt(tempOffset);
			tempOffset += 4;

			for (int ipart = 0; ipart < ipartcount; ipart++) {
				int ipointcount = wkbHelper.getInt(tempOffset);
				tempOffset += 4;

				// If ipointcount == 0, then we have an empty part
				if (ipointcount == 0)
					continue;

				if (ipointcount <= 2) {
					tempOffset += ipointcount * 2 * 8;

					if (bZs)
						tempOffset += ipointcount * 8;

					if (bMs)
						tempOffset += ipointcount * 8;

					if (ipointcount == 1)
						point_count += ipointcount + 1;
					else
						point_count += ipointcount;

					partCount++;

					continue;
				}

				double startx = wkbHelper.getDouble(tempOffset);
				tempOffset += 8;
				double starty = wkbHelper.getDouble(tempOffset);
				tempOffset += 8;
				double startz = NumberUtils.TheNaN;
				double startm = NumberUtils.TheNaN;

				if (bZs) {
					startz = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
				}

				if (bMs) {
					startm = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
				}

				tempOffset += (ipointcount - 2) * 2 * 8;

				if (bZs)
					tempOffset += (ipointcount - 2) * 8;

				if (bMs)
					tempOffset += (ipointcount - 2) * 8;

				double endx = wkbHelper.getDouble(tempOffset);
				tempOffset += 8;
				double endy = wkbHelper.getDouble(tempOffset);
				tempOffset += 8;
				double endz = NumberUtils.TheNaN;
				double endm = NumberUtils.TheNaN;

				if (bZs) {
					endz = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
				}

				if (bMs) {
					endm = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
				}

				if ((startx == endx || (NumberUtils.isNaN(startx) && NumberUtils
						.isNaN(endx)))
						&& (starty == endy || (NumberUtils.isNaN(starty) && NumberUtils
								.isNaN(endy)))
						&& (!bZs || startz == endz || (NumberUtils
								.isNaN(startz) && NumberUtils.isNaN(endz)))
						&& (!bMs || startm == endm || (NumberUtils
								.isNaN(startm) && NumberUtils.isNaN(endm)))) {
					point_count += ipointcount - 1;
				} else {
					point_count += ipointcount;
				}

				partCount++;
			}
		}

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt32 parts = null;
		AttributeStreamOfInt8 pathFlags = null;

		Geometry newPolygon;
		MultiPathImpl polygon;

		newPolygon = new Polygon();
		polygon = (MultiPathImpl) newPolygon._getImpl();

		if (bZs)
			polygon.addAttribute(VertexDescription.Semantics.Z);

		if (bMs)
			polygon.addAttribute(VertexDescription.Semantics.M);

		if (point_count > 0) {
			parts = (AttributeStreamOfInt32) (AttributeStreamBase
					.createIndexStream(partCount + 1, 0));
			pathFlags = (AttributeStreamOfInt8) (AttributeStreamBase
					.createByteStream(parts.size(), (byte) PathFlags.enumClosed));
			position = (AttributeStreamOfDbl) (AttributeStreamBase
					.createAttributeStreamWithSemantics(
							VertexDescription.Semantics.POSITION, point_count));

			if (bZs)
				zs = (AttributeStreamOfDbl) (AttributeStreamBase
						.createAttributeStreamWithSemantics(
								VertexDescription.Semantics.Z, point_count));

			if (bMs)
				ms = (AttributeStreamOfDbl) (AttributeStreamBase
						.createAttributeStreamWithSemantics(
								VertexDescription.Semantics.M, point_count));
		}

		boolean bCreateMs = false, bCreateZs = false;
		int ipartend = 0;
		int ipolygonend = 0;
		int part_index = 0;

		// read Coordinates
		for (int ipolygon = 0; ipolygon < polygonCount; ipolygon++) {
			offset += 5; // skip redundant byte order and type fields
			int ipartcount = wkbHelper.getInt(offset);
			offset += 4;
			int ipolygonstart = ipolygonend;
			ipolygonend = ipolygonstart + ipartcount;

			for (int ipart = ipolygonstart; ipart < ipolygonend; ipart++) {
				int ipointcount = wkbHelper.getInt(offset);
				offset += 4;

				if (ipointcount == 0)
					continue;

				int ipartstart = ipartend;
				ipartend += ipointcount;
				boolean bSkipLastPoint = true;

				if (ipointcount == 1) {
					ipartstart++;
					ipartend++;
					bSkipLastPoint = false;
				} else if (ipointcount == 2) {
					bSkipLastPoint = false;
				} else {
					// Check if start point is equal to end point

					tempOffset = offset;

					double startx = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
					double starty = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
					double startz = NumberUtils.TheNaN;
					double startm = NumberUtils.TheNaN;

					if (bZs) {
						startz = wkbHelper.getDouble(tempOffset);
						tempOffset += 8;
					}

					if (bMs) {
						startm = wkbHelper.getDouble(tempOffset);
						tempOffset += 8;
					}

					tempOffset += (ipointcount - 2) * 2 * 8;

					if (bZs)
						tempOffset += (ipointcount - 2) * 8;

					if (bMs)
						tempOffset += (ipointcount - 2) * 8;

					double endx = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
					double endy = wkbHelper.getDouble(tempOffset);
					tempOffset += 8;
					double endz = NumberUtils.TheNaN;
					double endm = NumberUtils.TheNaN;

					if (bZs) {
						endz = wkbHelper.getDouble(tempOffset);
						tempOffset += 8;
					}

					if (bMs) {
						endm = wkbHelper.getDouble(tempOffset);
						tempOffset += 8;
					}

					if ((startx == endx || (NumberUtils.isNaN(startx) && NumberUtils
							.isNaN(endx)))
							&& (starty == endy || (NumberUtils.isNaN(starty) && NumberUtils
									.isNaN(endy)))
							&& (!bZs || startz == endz || (NumberUtils
									.isNaN(startz) && NumberUtils.isNaN(endz)))
							&& (!bMs || startm == endm || (NumberUtils
									.isNaN(startm) && NumberUtils.isNaN(endm))))
						ipartend--;
					else
						bSkipLastPoint = false;
				}

				if (ipart == ipolygonstart)
					pathFlags.setBits(ipart,
							(byte) PathFlags.enumOGCStartPolygon);

				parts.write(++part_index, ipartend);

				// We must write from the buffer backwards - ogc polygon
				// format is opposite of shapefile format
				for (int i = ipartstart; i < ipartend; i++) {
					double x = wkbHelper.getDouble(offset);
					offset += 8;
					double y = wkbHelper.getDouble(offset);
					offset += 8;

					position.write(2 * i, x);
					position.write(2 * i + 1, y);

					if (bZs) {
						double z = wkbHelper.getDouble(offset);
						offset += 8;

						zs.write(i, z);
						if (!VertexDescription.isDefaultValue(
								VertexDescription.Semantics.Z, z))
							bCreateZs = true;
					}

					if (bMs) {
						double m = wkbHelper.getDouble(offset);
						offset += 8;

						ms.write(i, m);
						if (!VertexDescription.isDefaultValue(
								VertexDescription.Semantics.M, m))
							bCreateMs = true;
					}
				}

				if (bSkipLastPoint) {
					offset += 2 * 8;

					if (bZs)
						offset += 8;

					if (bMs)
						offset += 8;
				} else if (ipointcount == 1) {
					double x = position.read(2 * ipartstart);
					double y = position.read(2 * ipartstart + 1);
					position.write(2 * (ipartstart - 1), x);
					position.write(2 * (ipartstart - 1) + 1, y);

					if (bZs) {
						double z = zs.read(ipartstart);
						zs.write(ipartstart - 1, z);
					}

					if (bMs) {
						double m = ms.read(ipartstart);
						ms.write(ipartstart - 1, m);
					}
				}
			}
		}

		// set envelopes and assign AttributeStreams

		if (point_count > 0) {
			polygon.setPathStreamRef(parts); // sets m_parts
			polygon.setPathFlagsStreamRef(pathFlags);
			polygon.setAttributeStreamRef(VertexDescription.Semantics.POSITION,
					position);

			if (bZs) {
				if (!bCreateZs)
					zs = null;

				polygon.setAttributeStreamRef(VertexDescription.Semantics.Z, zs);
			}

			if (bMs) {
				if (!bCreateMs)
					ms = null;

				polygon.setAttributeStreamRef(VertexDescription.Semantics.M, ms);
			}

			polygon.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);

			AttributeStreamOfInt8 path_flags_clone = new AttributeStreamOfInt8(
					pathFlags);

			for (int i = 0; i < path_flags_clone.size() - 1; i++) {
				if (((int) path_flags_clone.read(i) & (int) PathFlags.enumOGCStartPolygon) != 0) {// Should
																									// be
																									// clockwise
					if (!InternalUtils.isClockwiseRing(polygon, i))
						polygon.reversePath(i); // make clockwise
				} else {// Should be counter-clockwise
					if (InternalUtils.isClockwiseRing(polygon, i))
						polygon.reversePath(i); // make counter-clockwise
				}
			}

			polygon.setPathFlagsStreamRef(path_flags_clone);
		}

		if ((importFlags & (int) WkbImportFlags.wkbImportNonTrusted) == 0)
			polygon.setIsSimple(MultiVertexGeometryImpl.GeometryXSimple.Weak,
					0.0, false);

		polygon.setDirtyOGCFlags(false);
		wkbHelper.adjustment += offset;

		return newPolygon;
	}

	private static Geometry importFromWkbPolyline(boolean bMultiPolyline,
			int importFlags, boolean bZs, boolean bMs, WkbHelper wkbHelper) {
		int offset;
		int originalPartCount;

		if (bMultiPolyline) {
			originalPartCount = wkbHelper.getInt(5);
			offset = 9;
		} else {
			originalPartCount = 1;
			offset = 0;
		}

		// Find total point count and part count
		int point_count = 0;
		int partCount = 0;
		int tempOffset = offset;
		for (int ipart = 0; ipart < originalPartCount; ipart++) {
			tempOffset += 5; // skip redundant byte order and type fields
			int ipointcount = wkbHelper.getInt(tempOffset);
			tempOffset += 4;

			// If ipointcount == 0, then we have an empty part
			if (ipointcount == 0)
				continue;

			point_count += ipointcount;
			partCount++;

			if (ipointcount == 1)
				point_count++;

			tempOffset += ipointcount * 2 * 8;

			if (bZs)
				tempOffset += ipointcount * 8;

			if (bMs)
				tempOffset += ipointcount * 8;
		}

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt32 parts = null;
		AttributeStreamOfInt8 pathFlags = null;

		Polyline newpolyline;
		MultiPathImpl polyline;

		newpolyline = new Polyline();
		polyline = (MultiPathImpl) newpolyline._getImpl();

		if (bZs)
			polyline.addAttribute(VertexDescription.Semantics.Z);

		if (bMs)
			polyline.addAttribute(VertexDescription.Semantics.M);

		if (point_count > 0) {
			parts = (AttributeStreamOfInt32) (AttributeStreamBase
					.createIndexStream(partCount + 1, 0));
			pathFlags = (AttributeStreamOfInt8) (AttributeStreamBase
					.createByteStream(parts.size(), (byte) 0));
			position = (AttributeStreamOfDbl) (AttributeStreamBase
					.createAttributeStreamWithSemantics(
							VertexDescription.Semantics.POSITION, point_count));

			if (bZs)
				zs = (AttributeStreamOfDbl) (AttributeStreamBase
						.createAttributeStreamWithSemantics(
								VertexDescription.Semantics.Z, point_count));

			if (bMs)
				ms = (AttributeStreamOfDbl) (AttributeStreamBase
						.createAttributeStreamWithSemantics(
								VertexDescription.Semantics.M, point_count));
		}

		boolean bCreateMs = false, bCreateZs = false;
		int ipartend = 0;
		int part_index = 0;

		// read Coordinates
		for (int ipart = 0; ipart < originalPartCount; ipart++) {
			offset += 5; // skip redundant byte order and type fields

			int ipointcount = wkbHelper.getInt(offset);
			offset += 4;

			if (ipointcount == 0)
				continue;

			int ipartstart = ipartend;
			ipartend = ipartstart + ipointcount;

			if (ipointcount == 1) {
				ipartstart++;
				ipartend++;
			}

			parts.write(++part_index, ipartend);

			for (int i = ipartstart; i < ipartend; i++) {
				double x = wkbHelper.getDouble(offset);
				offset += 8;
				double y = wkbHelper.getDouble(offset);
				offset += 8;

				position.write(2 * i, x);
				position.write(2 * i + 1, y);

				if (bZs) {
					double z = wkbHelper.getDouble(offset);
					offset += 8;

					zs.write(i, z);
					if (!VertexDescription.isDefaultValue(
							VertexDescription.Semantics.Z, z))
						bCreateZs = true;
				}

				if (bMs) {
					double m = wkbHelper.getDouble(offset);
					offset += 8;

					ms.write(i, m);
					if (!VertexDescription.isDefaultValue(
							VertexDescription.Semantics.M, m))
						bCreateMs = true;
				}
			}

			if (ipointcount == 1) {
				double x = position.read(2 * ipartstart);
				double y = position.read(2 * ipartstart + 1);
				position.write(2 * (ipartstart - 1), x);
				position.write(2 * (ipartstart - 1) + 1, y);

				if (bZs) {
					double z = zs.read(ipartstart);
					zs.write(ipartstart - 1, z);
				}

				if (bMs) {
					double m = ms.read(ipartstart);
					ms.write(ipartstart - 1, m);
				}
			}
		}

		// set envelopes and assign AttributeStreams

		if (point_count > 0) {
			polyline.setPathStreamRef(parts); // sets m_parts
			polyline.setPathFlagsStreamRef(pathFlags);
			polyline.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);

			if (bZs) {
				if (!bCreateZs)
					zs = null;

				polyline.setAttributeStreamRef(VertexDescription.Semantics.Z,
						zs);
			}

			if (bMs) {
				if (!bCreateMs)
					ms = null;

				polyline.setAttributeStreamRef(VertexDescription.Semantics.M,
						ms);
			}

			polyline.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);
		}

		wkbHelper.adjustment += offset;

		return newpolyline;
	}

	private static Geometry importFromWkbMultiPoint(int importFlags,
			boolean bZs, boolean bMs, WkbHelper wkbHelper) {
		int offset = 5; // skip byte order and type

		// set point count
		int point_count = wkbHelper.getInt(offset);
		offset += 4;

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;

		MultiPoint newmultipoint;
		MultiPointImpl multipoint;

		newmultipoint = new MultiPoint();
		multipoint = (MultiPointImpl) newmultipoint._getImpl();

		if (bZs)
			multipoint.addAttribute(VertexDescription.Semantics.Z);

		if (bMs)
			multipoint.addAttribute(VertexDescription.Semantics.M);

		if (point_count > 0) {
			position = (AttributeStreamOfDbl) (AttributeStreamBase
					.createAttributeStreamWithSemantics(
							VertexDescription.Semantics.POSITION, point_count));

			if (bZs)
				zs = (AttributeStreamOfDbl) (AttributeStreamBase
						.createAttributeStreamWithSemantics(
								VertexDescription.Semantics.Z, point_count));

			if (bMs)
				ms = (AttributeStreamOfDbl) (AttributeStreamBase
						.createAttributeStreamWithSemantics(
								VertexDescription.Semantics.M, point_count));
		}

		boolean bCreateMs = false, bCreateZs = false;
		for (int i = 0; i < point_count; i++) {
			offset += 5; // skip redundant byte order and type fields

			// read xy coordinates
			double x = wkbHelper.getDouble(offset);
			offset += 8;
			double y = wkbHelper.getDouble(offset);
			offset += 8;

			position.write(2 * i, x);
			position.write(2 * i + 1, y);

			if (bZs) {
				double z = wkbHelper.getDouble(offset);
				offset += 8;

				zs.write(i, z);
				if (!VertexDescription.isDefaultValue(
						VertexDescription.Semantics.Z, z))
					bCreateZs = true;
			}

			if (bMs) {
				double m = wkbHelper.getDouble(offset);
				offset += 8;

				ms.write(i, m);
				if (!VertexDescription.isDefaultValue(
						VertexDescription.Semantics.M, m))
					bCreateMs = true;
			}
		}

		// set envelopes and assign AttributeStreams

		if (point_count > 0) {
			multipoint.resize(point_count);
			multipoint.setAttributeStreamRef(
					VertexDescription.Semantics.POSITION, position);

			if (bZs) {
				if (!bCreateZs)
					zs = null;

				multipoint.setAttributeStreamRef(VertexDescription.Semantics.Z,
						zs);
			}

			if (bMs) {
				if (!bCreateMs)
					ms = null;

				multipoint.setAttributeStreamRef(VertexDescription.Semantics.M,
						ms);
			}

			multipoint.notifyModified(MultiPointImpl.DirtyFlags.DirtyAll);
		}

		wkbHelper.adjustment += offset;

		return newmultipoint;
	}

	private static Geometry importFromWkbPoint(int importFlags, boolean bZs,
			boolean bMs, WkbHelper wkbHelper) {
		int offset = 5; // skip byte order and type

		// set xy coordinate
		double x = wkbHelper.getDouble(offset);
		offset += 8;
		double y = wkbHelper.getDouble(offset);
		offset += 8;

		double z = NumberUtils.TheNaN;
		if (bZs) {
			z = wkbHelper.getDouble(offset);
			offset += 8;
		}

		double m = NumberUtils.TheNaN;
		if (bMs) {
			m = wkbHelper.getDouble(offset);
			offset += 8;
		}

		boolean bEmpty = NumberUtils.isNaN(x);
		Point point = new Point();

		if (!bEmpty) {
			point.setX(x);
			point.setY(y);
		}

		// set Z
		if (bZs) {
			point.addAttribute(VertexDescription.Semantics.Z);
			if (!bEmpty)
				point.setZ(z);
		}

		// set M
		if (bMs) {
			point.addAttribute(VertexDescription.Semantics.M);
			if (!bEmpty)
				point.setM(m);
		}

		wkbHelper.adjustment += offset;

		return point;
	}
}
