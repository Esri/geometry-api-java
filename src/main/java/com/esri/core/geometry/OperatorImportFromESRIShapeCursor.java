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

import com.esri.core.geometry.MultiVertexGeometryImpl.GeometryXSimple;
import com.esri.core.geometry.VertexDescription.Semantics;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class OperatorImportFromESRIShapeCursor extends GeometryCursor {

	ByteBufferCursor m_inputShapeBuffers;
	int m_importFlags;
	int m_type;
	int m_index;

	public OperatorImportFromESRIShapeCursor(int importFlags, int type,
			ByteBufferCursor shapeBuffers) {
		m_index = -1;
		if (shapeBuffers == null)
			throw new GeometryException("invalid argument");

		m_importFlags = importFlags;
		m_type = type;
		m_inputShapeBuffers = shapeBuffers;
	}

	@Override
	public Geometry next() {
		ByteBuffer shapeBuffer = m_inputShapeBuffers.next();
		if (shapeBuffer != null) {
			m_index = m_inputShapeBuffers.getByteBufferID();
			return importFromESRIShape(shapeBuffer);
		}
		return null;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	private Geometry importFromESRIShape(ByteBuffer shapeBuffer) {
		ByteOrder initialOrder = shapeBuffer.order();
		shapeBuffer.order(ByteOrder.LITTLE_ENDIAN);

		try {
			// read type
			int shapetype = shapeBuffer.getInt(0);

			// Extract general type and modifiers
			int generaltype;
			int modifiers;
			switch (shapetype & ShapeModifiers.ShapeBasicTypeMask) {
			// Polygon
			case ShapeType.ShapePolygon:
				generaltype = ShapeType.ShapeGeneralPolygon;
				modifiers = 0;
				break;
			case ShapeType.ShapePolygonZM:
				generaltype = ShapeType.ShapeGeneralPolygon;
				modifiers = ShapeModifiers.ShapeHasZs | ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapePolygonM:
				generaltype = ShapeType.ShapeGeneralPolygon;
				modifiers = ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapePolygonZ:
				generaltype = ShapeType.ShapeGeneralPolygon;
				modifiers = ShapeModifiers.ShapeHasZs;
				break;
			case ShapeType.ShapeGeneralPolygon:
				generaltype = ShapeType.ShapeGeneralPolygon;
				modifiers = shapetype & ShapeModifiers.ShapeModifierMask;
				break;

			// Polyline
			case ShapeType.ShapePolyline:
				generaltype = ShapeType.ShapeGeneralPolyline;
				modifiers = 0;
				break;
			case ShapeType.ShapePolylineZM:
				generaltype = ShapeType.ShapeGeneralPolyline;
				modifiers = ShapeModifiers.ShapeHasZs
						| (int) ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapePolylineM:
				generaltype = ShapeType.ShapeGeneralPolyline;
				modifiers = ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapePolylineZ:
				generaltype = ShapeType.ShapeGeneralPolyline;
				modifiers = ShapeModifiers.ShapeHasZs;
				break;
			case ShapeType.ShapeGeneralPolyline:
				generaltype = ShapeType.ShapeGeneralPolyline;
				modifiers = shapetype & ShapeModifiers.ShapeModifierMask;
				break;

			// MultiPoint
			case ShapeType.ShapeMultiPoint:
				generaltype = ShapeType.ShapeGeneralMultiPoint;
				modifiers = 0;
				break;
			case ShapeType.ShapeMultiPointZM:
				generaltype = ShapeType.ShapeGeneralMultiPoint;
				modifiers = (int) ShapeModifiers.ShapeHasZs
						| (int) ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapeMultiPointM:
				generaltype = ShapeType.ShapeGeneralMultiPoint;
				modifiers = ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapeMultiPointZ:
				generaltype = ShapeType.ShapeGeneralMultiPoint;
				modifiers = ShapeModifiers.ShapeHasZs;
				break;
			case ShapeType.ShapeGeneralMultiPoint:
				generaltype = ShapeType.ShapeGeneralMultiPoint;
				modifiers = shapetype & ShapeModifiers.ShapeModifierMask;
				break;

			// Point
			case ShapeType.ShapePoint:
				generaltype = ShapeType.ShapeGeneralPoint;
				modifiers = 0;
				break;
			case ShapeType.ShapePointZM:
				generaltype = ShapeType.ShapeGeneralPoint;
				modifiers = ShapeModifiers.ShapeHasZs
						| (int) ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapePointM:
				generaltype = ShapeType.ShapeGeneralPoint;
				modifiers = ShapeModifiers.ShapeHasMs;
				break;
			case ShapeType.ShapePointZ:
				generaltype = ShapeType.ShapeGeneralPoint;
				modifiers = ShapeModifiers.ShapeHasZs;
				break;
			case ShapeType.ShapeGeneralPoint:
				generaltype = ShapeType.ShapeGeneralPoint;
				modifiers = shapetype & ShapeModifiers.ShapeModifierMask;
				break;

			// Null Geometry
			case ShapeType.ShapeNull:
				return null;

			default:
				throw new GeometryException("invalid shape type");
			}

			switch (generaltype) {
			case ShapeType.ShapeGeneralPolygon:
				if (m_type != Geometry.GeometryType.Polygon
						&& m_type != Geometry.GeometryType.Unknown
						&& m_type != Geometry.GeometryType.Envelope)
					throw new GeometryException("invalid shape type");
				return importFromESRIShapeMultiPath(true, modifiers,
						shapeBuffer);

			case ShapeType.ShapeGeneralPolyline:
				if (m_type != Geometry.GeometryType.Polyline
						&& m_type != Geometry.GeometryType.Unknown
						&& m_type != Geometry.GeometryType.Envelope)
					throw new GeometryException("invalid shape type");
				return importFromESRIShapeMultiPath(false, modifiers,
						shapeBuffer);

			case ShapeType.ShapeGeneralMultiPoint:
				if (m_type != Geometry.GeometryType.MultiPoint
						&& m_type != Geometry.GeometryType.Unknown
						&& m_type != Geometry.GeometryType.Envelope)
					throw new GeometryException("invalid shape type");
				return importFromESRIShapeMultiPoint(modifiers, shapeBuffer);

			case ShapeType.ShapeGeneralPoint:
				if (m_type != Geometry.GeometryType.Point
						&& m_type != Geometry.GeometryType.MultiPoint
						&& m_type != Geometry.GeometryType.Unknown
						&& m_type != Geometry.GeometryType.Envelope)
					throw new GeometryException("invalid shape type");
				return importFromESRIShapePoint(modifiers, shapeBuffer);
			}

			return null;
		} finally {
			shapeBuffer.order(initialOrder);
		}
	}

	private Geometry importFromESRIShapeMultiPath(boolean bPolygon,
			int modifiers, ByteBuffer shapeBuffer) {
		int offset = 4;

		boolean bZs = (modifiers & (int) ShapeModifiers.ShapeHasZs) != 0;
		boolean bMs = (modifiers & (int) ShapeModifiers.ShapeHasMs) != 0;
		boolean bIDs = (modifiers & (int) ShapeModifiers.ShapeHasIDs) != 0;

		boolean bHasAttributes = bZs || bMs || bIDs;
		boolean bHasBadRings = false;

		// read Envelope
		double xmin = shapeBuffer.getDouble(offset);
		offset += 8;
		double ymin = shapeBuffer.getDouble(offset);
		offset += 8;
		double xmax = shapeBuffer.getDouble(offset);
		offset += 8;
		double ymax = shapeBuffer.getDouble(offset);
		offset += 8;

		// read part count
		int originalPartCount = shapeBuffer.getInt(offset);
		offset += 4;
		int partCount = 0;

		// read point count
		int pointCount = shapeBuffer.getInt(offset);
		offset += 4;

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt32 ids = null;
		AttributeStreamOfInt32 parts = null;
		AttributeStreamOfInt8 pathFlags = null;

		Envelope bbox = null;
		MultiPath multipath = null;
		MultiPathImpl multipathImpl = null;
		if (m_type == Geometry.GeometryType.Polygon
				|| m_type == Geometry.GeometryType.Polyline
				|| m_type == Geometry.GeometryType.Unknown) {
			if (bPolygon)
				multipath = new Polygon();
			else
				multipath = new Polyline();

			multipathImpl = (MultiPathImpl) multipath._getImpl();

			if (pointCount > 0) {
				bbox = new Envelope();
				bbox.setCoords(xmin, ymin, xmax, ymax);
				parts = (AttributeStreamOfInt32) AttributeStreamBase
						.createIndexStream(originalPartCount + 1);

				int previstart = -1;
				int lastCount = 0;
				for (int i = 0; i < originalPartCount; i++) {
					int istart = shapeBuffer.getInt(offset);
					offset += 4;
					lastCount = istart;
					if (previstart > istart || istart < 0)// check that the part
															// indices in the
															// buffer are not
															// corrupted
						throw new GeometryException("corrupted geometry");

					if (istart != previstart) {
						parts.write(partCount, istart);
						previstart = istart;
						partCount++;
					}
				}

				parts.resize(partCount + 1);
				if (pointCount < lastCount)// check that the point count in the
											// buffer is not corrupted
					throw new GeometryException("corrupted geometry");

				parts.write(partCount, pointCount);
				pathFlags = (AttributeStreamOfInt8) AttributeStreamBase
						.createByteStream(parts.size(), (byte) 0);

				// Create empty position stream
				position = (AttributeStreamOfDbl) AttributeStreamBase
						.createAttributeStreamWithSemantics(Semantics.POSITION,
								pointCount);

				int startpart = parts.read(0);
				// read xy coordinates
				int xyindex = 0;
				for (int ipart = 0; ipart < partCount; ipart++) {
					int endpartActual = parts.read(ipart + 1);
					// for polygons we read one point less, then analyze if the
					// polygon is closed.
					int endpart = (bPolygon) ? endpartActual - 1
							: endpartActual;

					double startx = shapeBuffer.getDouble(offset);
					offset += 8;
					double starty = shapeBuffer.getDouble(offset);
					offset += 8;
					position.write(2 * xyindex, startx);
					position.write(2 * xyindex + 1, starty);
					xyindex++;

					for (int i = startpart + 1; i < endpart; i++) {
						double x = shapeBuffer.getDouble(offset);
						offset += 8;
						double y = shapeBuffer.getDouble(offset);
						offset += 8;
						position.write(2 * xyindex, x);
						position.write(2 * xyindex + 1, y);
						xyindex++;
					}

					if (endpart - startpart < 2) {// a part with only one point
						multipathImpl.setIsSimple(GeometryXSimple.Unknown, 0.0,
								false);
					}

					if (bPolygon) {// read the last point of the part to decide
									// if we need to close the polygon
						if (startpart == endpart) {// a part with only one point
							parts.write(ipart + 1, xyindex);
						} else {
							double x = shapeBuffer.getDouble(offset);
							offset += 8;
							double y = shapeBuffer.getDouble(offset);
							offset += 8;

							if (x != startx || y != starty) {// bad polygon. The
																// last point is
																// not the same
																// as the last
																// one. We need
																// to add it so
																// that we do
																// not loose it.
								position.write(2 * xyindex, x);
								position.write(2 * xyindex + 1, y);
								xyindex++;
								multipathImpl.setIsSimple(
										GeometryXSimple.Unknown, 0.0, false);
								bHasBadRings = true;
								// write part count to indicate we need to
								// account for one extra point
								// The count will be fixed after the attributes
								// are processed. So we write negative only when
								// there are attributes.
								parts.write(ipart + 1,
										bHasAttributes ? -xyindex : xyindex);
							} else
								parts.write(ipart + 1, xyindex);
						}

						pathFlags.setBits(ipart, (byte) PathFlags.enumClosed);
					}

					startpart = endpartActual;
				}

				if (bZs)
					bbox.addAttribute(Semantics.Z);

				if (bMs)
					bbox.addAttribute(Semantics.M);

				if (bIDs)
					bbox.addAttribute(Semantics.ID);
			}
		} else {
			bbox = new Envelope();

			if (bZs)
				bbox.addAttribute(Semantics.Z);

			if (bMs)
				bbox.addAttribute(Semantics.M);

			if (bIDs)
				bbox.addAttribute(Semantics.ID);

			if (pointCount > 0) {
				bbox.setCoords(xmin, ymin, xmax, ymax);
				offset += pointCount * 16 + originalPartCount * 4;
			} else
				return (Geometry) bbox;
		}

		// read Zs
		if (bZs) {
			if (pointCount > 0) {
				double zmin = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;
				double zmax = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;

				Envelope1D env = new Envelope1D();
				env.setCoords(zmin, zmax);
				bbox.setInterval(Semantics.Z, 0, env);

				if (m_type == Geometry.GeometryType.Polygon
						|| m_type == Geometry.GeometryType.Polyline
						|| m_type == Geometry.GeometryType.Unknown) {
					zs = (AttributeStreamOfDbl) AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.Z,
									pointCount);

					boolean bCreate = false;
					int startpart = parts.read(0);
					for (int ipart = 0; ipart < partCount; ipart++) {
						int endpartActual = parts.read(ipart + 1);
						int endpart = Math.abs(endpartActual);

						double startz = Interop.translateFromAVNaN(shapeBuffer
								.getDouble(offset));
						offset += 8;
						zs.write(startpart, startz);
						if (!VertexDescription.isDefaultValue(Semantics.Z,
								startz))
							bCreate = true;

						for (int i = startpart + 1; i < endpart; i++) {
							double z = Interop.translateFromAVNaN(shapeBuffer
									.getDouble(offset));
							offset += 8;
							zs.write(i, z);
							if (!VertexDescription.isDefaultValue(Semantics.Z,
									z))
								bCreate = true;
						}

						if (bPolygon && endpartActual > 0) {
							offset += 8;
						}

						startpart = endpart;
					}

					if (!bCreate)
						zs = null;
				} else
					offset += pointCount * 8;
			}

			if (m_type == Geometry.GeometryType.Polygon
					|| m_type == Geometry.GeometryType.Polyline
					|| m_type == Geometry.GeometryType.Unknown)
				multipathImpl.setAttributeStreamRef(Semantics.Z, zs);
		}

		// read Ms
		if (bMs) {
			if (pointCount > 0) {
				double mmin = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;
				double mmax = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;

				Envelope1D env = new Envelope1D();
				env.setCoords(mmin, mmax);
				bbox.setInterval(Semantics.M, 0, env);

				if (m_type == Geometry.GeometryType.Polygon
						|| m_type == Geometry.GeometryType.Polyline
						|| m_type == Geometry.GeometryType.Unknown) {
					ms = (AttributeStreamOfDbl) AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.M,
									pointCount);

					boolean bCreate = false;
					int startpart = parts.read(0);
					for (int ipart = 0; ipart < partCount; ipart++) {
						int endpartActual = parts.read(ipart + 1);
						int endpart = Math.abs(endpartActual);

						double startm = Interop.translateFromAVNaN(shapeBuffer
								.getDouble(offset));
						offset += 8;
						ms.write(startpart, startm);
						if (!VertexDescription.isDefaultValue(Semantics.M,
								startm))
							bCreate = true;

						for (int i = startpart + 1; i < endpart; i++) {
							double m = Interop.translateFromAVNaN(shapeBuffer
									.getDouble(offset));
							offset += 8;
							ms.write(i, m);
							if (!VertexDescription.isDefaultValue(Semantics.M,
									m))
								bCreate = true;
						}

						if (bPolygon && endpartActual > 0) {
							offset += 8;
						}

						startpart = endpart;
					}

					if (!bCreate)
						ms = null;
				} else
					offset += pointCount * 8;
			}

			if (m_type == Geometry.GeometryType.Polygon
					|| m_type == Geometry.GeometryType.Polyline
					|| m_type == Geometry.GeometryType.Unknown)
				multipathImpl.setAttributeStreamRef(Semantics.M, ms);
		}

		// read IDs
		if (bIDs) {
			if (pointCount > 0) {
				double idmin = NumberUtils.doubleMax();
				double idmax = -NumberUtils.doubleMax();

				if (m_type == Geometry.GeometryType.Polygon
						|| m_type == Geometry.GeometryType.Polyline
						|| m_type == Geometry.GeometryType.Unknown) {
					ids = (AttributeStreamOfInt32) AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.ID,
									pointCount);

					boolean bCreate = false;
					int startpart = parts.read(0);
					for (int ipart = 0; ipart < partCount; ipart++) {
						int endpartActual = parts.read(ipart + 1);
						int endpart = Math.abs(endpartActual);

						int startid = shapeBuffer.getInt(offset);
						offset += 4;
						ids.write(startpart, startid);
						if (!VertexDescription.isDefaultValue(Semantics.ID,
								startid))
							bCreate = true;

						for (int i = startpart + 1; i < endpart; i++) {
							int id = shapeBuffer.getInt(offset);
							offset += 4;
							ids.write(i, id);
							if (!bCreate
									&& !VertexDescription.isDefaultValue(
											Semantics.ID, id))
								bCreate = true;

							if (idmin > id)
								idmin = id;
							else if (idmax < id)
								idmax = id;
						}

						if (bPolygon && endpartActual > 0) {
							offset += 4;
						}

						startpart = endpart;
					}

					if (!bCreate)
						ids = null;
				} else {
					for (int i = 0; i < pointCount; i++) {
						int id = shapeBuffer.getInt(offset);
						offset += 4;

						if (idmin > id)
							idmin = id;
						else if (idmax < id)
							idmax = id;
					}
				}

				Envelope1D env = new Envelope1D();
				env.setCoords(idmin, idmax);
				bbox.setInterval(Semantics.ID, 0, env);
			}

			if (m_type == Geometry.GeometryType.Polygon
					|| m_type == Geometry.GeometryType.Polyline
					|| m_type == Geometry.GeometryType.Unknown)
				multipathImpl.setAttributeStreamRef(Semantics.ID, ids);
		}

		if (bHasBadRings && bHasAttributes) {// revert our hack for bad polygons
			for (int ipart = 1; ipart < partCount + 1; ipart++) {
				int v = parts.read(ipart);
				if (v < 0)
					parts.write(ipart, -v);
			}
		}

		if (m_type == Geometry.GeometryType.Envelope)
			return (Geometry) bbox;

		if (pointCount > 0) {
			multipathImpl.setPathStreamRef(parts);
			multipathImpl.setPathFlagsStreamRef(pathFlags);
			multipathImpl.setAttributeStreamRef(Semantics.POSITION, position);
			multipathImpl.setEnvelope(bbox);
		}

		if ((m_importFlags & ShapeImportFlags.ShapeImportNonTrusted) == 0)
			multipathImpl.setIsSimple(GeometryXSimple.Weak, 0.0, false);// We
																		// use
																		// tolerance
																		// of 0.
																		// What
																		// should
																		// we
																		// instead?

		return (Geometry) multipath;
	}

	private Geometry importFromESRIShapeMultiPoint(int modifiers,
			ByteBuffer shapeBuffer) {
		int offset = 4;

		boolean bZs = (modifiers & (int) ShapeModifiers.ShapeHasZs) != 0;
		boolean bMs = (modifiers & (int) ShapeModifiers.ShapeHasMs) != 0;
		boolean bIDs = (modifiers & modifiers & (int) ShapeModifiers.ShapeHasIDs) != 0;

		double xmin = shapeBuffer.getDouble(offset);
		offset += 8;
		double ymin = shapeBuffer.getDouble(offset);
		offset += 8;
		double xmax = shapeBuffer.getDouble(offset);
		offset += 8;
		double ymax = shapeBuffer.getDouble(offset);
		offset += 8;

		int cPoints = shapeBuffer.getInt(offset);
		offset += 4;

		AttributeStreamOfDbl position = null;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfInt32 ids = null;

		Envelope bbox = null;
		MultiPoint multipoint = null;
		MultiPointImpl multipointImpl = null;

		if (m_type == Geometry.GeometryType.MultiPoint
				|| m_type == Geometry.GeometryType.Unknown) {
			multipoint = new MultiPoint();
			multipointImpl = (MultiPointImpl) multipoint._getImpl();

			if (cPoints > 0) {
				bbox = new Envelope();
				multipointImpl.resize(cPoints);
				position = (AttributeStreamOfDbl) AttributeStreamBase
						.createAttributeStreamWithSemantics(Semantics.POSITION,
								cPoints);

				for (int i = 0; i < cPoints; i++) {
					double x = shapeBuffer.getDouble(offset);
					offset += 8;
					double y = shapeBuffer.getDouble(offset);
					offset += 8;
					position.write(2 * i, x);
					position.write(2 * i + 1, y);
				}

				multipointImpl.resize(cPoints);
				bbox.setCoords(xmin, ymin, xmax, ymax);

				if (bZs)
					bbox.addAttribute(Semantics.Z);

				if (bMs)
					bbox.addAttribute(Semantics.M);

				if (bIDs)
					bbox.addAttribute(Semantics.ID);
			}
		} else {
			bbox = new Envelope();

			if (bZs)
				bbox.addAttribute(Semantics.Z);

			if (bMs)
				bbox.addAttribute(Semantics.M);

			if (bIDs)
				bbox.addAttribute(Semantics.ID);

			if (cPoints > 0) {
				bbox.setCoords(xmin, ymin, xmax, ymax);
				offset += cPoints * 16;
			} else
				return (Geometry) bbox;
		}

		if (bZs) {
			if (cPoints > 0) {
				double zmin = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;
				double zmax = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;

				Envelope1D env = new Envelope1D();
				env.setCoords(zmin, zmax);
				bbox.setInterval(Semantics.Z, 0, env);

				if (m_type == Geometry.GeometryType.MultiPoint
						|| m_type == Geometry.GeometryType.Unknown) {
					zs = (AttributeStreamOfDbl) AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.Z,
									cPoints);

					boolean bCreate = false;
					for (int i = 0; i < cPoints; i++) {
						double value = Interop.translateFromAVNaN(shapeBuffer
								.getDouble(offset));
						offset += 8;
						zs.write(i, value);
						if (!VertexDescription.isDefaultValue(Semantics.Z,
								value))
							bCreate = true;
					}

					if (!bCreate)
						zs = null;
				} else
					offset += cPoints * 8;
			}

			if (m_type == Geometry.GeometryType.MultiPoint
					|| m_type == Geometry.GeometryType.Unknown)
				multipointImpl.setAttributeStreamRef(Semantics.Z, zs);
		}

		if (bMs) {
			if (cPoints > 0) {
				double mmin = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;
				double mmax = Interop.translateFromAVNaN(shapeBuffer
						.getDouble(offset));
				offset += 8;

				Envelope1D env = new Envelope1D();
				env.setCoords(mmin, mmax);
				bbox.setInterval(Semantics.M, 0, env);
				if (m_type == Geometry.GeometryType.MultiPoint
						|| m_type == Geometry.GeometryType.Unknown) {
					ms = (AttributeStreamOfDbl) AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.M,
									cPoints);

					boolean bCreate = false;
					for (int i = 0; i < cPoints; i++) {
						double value = Interop.translateFromAVNaN(shapeBuffer
								.getDouble(offset));
						offset += 8;
						ms.write(i, value);
						if (!VertexDescription.isDefaultValue(Semantics.M,
								value))
							bCreate = true;
					}

					if (!bCreate)
						ms = null;
				} else
					offset += cPoints * 8;
			}

			if (m_type == Geometry.GeometryType.MultiPoint
					|| m_type == Geometry.GeometryType.Unknown)
				multipointImpl.setAttributeStreamRef(Semantics.M, ms);
		}

		if (bIDs) {
			if (cPoints > 0) {
				double idmin = NumberUtils.doubleMax();
				double idmax = -NumberUtils.doubleMax();

				if (m_type == Geometry.GeometryType.MultiPoint
						|| m_type == Geometry.GeometryType.Unknown) {
					ids = (AttributeStreamOfInt32) AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.ID,
									cPoints);

					boolean bCreate = false;
					for (int i = 0; i < cPoints; i++) {
						int value = shapeBuffer.getInt(offset);
						offset += 4;
						ids.write(i, value);
						if (!VertexDescription.isDefaultValue(Semantics.ID,
								value))
							bCreate = true;

						if (idmin > value)
							idmin = value;
						else if (idmax < value)
							idmax = value;
					}

					if (!bCreate)
						ids = null;
				} else {
					for (int i = 0; i < cPoints; i++) {
						int id = shapeBuffer.getInt(offset);
						offset += 4;

						if (idmin > id)
							idmin = id;
						else if (idmax < id)
							idmax = id;
					}
				}

				Envelope1D env = new Envelope1D();
				env.setCoords(idmin, idmax);
				bbox.setInterval(Semantics.ID, 0, env);
			}

			if (m_type == Geometry.GeometryType.MultiPoint
					|| m_type == Geometry.GeometryType.Unknown)
				multipointImpl.setAttributeStreamRef(Semantics.ID, ids);
		}

		if (m_type == Geometry.GeometryType.Envelope)
			return (Geometry) bbox;

		if (cPoints > 0) {
			multipointImpl.setAttributeStreamRef(Semantics.POSITION, position);
			multipointImpl.setEnvelope(bbox);
		}

		return (Geometry) multipoint;
	}

	private Geometry importFromESRIShapePoint(int modifiers,
			ByteBuffer shapeBuffer) {
		int offset = 4;

		boolean bZs = (modifiers & (int) ShapeModifiers.ShapeHasZs) != 0;
		boolean bMs = (modifiers & (int) ShapeModifiers.ShapeHasMs) != 0;
		boolean bIDs = (modifiers & modifiers & (int) ShapeModifiers.ShapeHasIDs) != 0;

		// read XY
		double x = shapeBuffer.getDouble(offset);
		offset += 8;
		double y = shapeBuffer.getDouble(offset);
		offset += 8;

		boolean bEmpty = NumberUtils.isNaN(x);

		double z = NumberUtils.NaN();
		if (bZs) {
			z = Interop.translateFromAVNaN(shapeBuffer.getDouble(offset));
			offset += 8;
		}

		double m = NumberUtils.NaN();
		if (bMs) {
			m = Interop.translateFromAVNaN(shapeBuffer.getDouble(offset));
			offset += 8;
		}

		int id = -1;
		if (bIDs) {
			id = shapeBuffer.getInt(offset);
			offset += 4;
		}

		if (m_type == Geometry.GeometryType.MultiPoint) {
			MultiPoint newmultipoint = new MultiPoint();
			MultiPointImpl multipointImpl = (MultiPointImpl) newmultipoint
					._getImpl();

			if (!bEmpty) {
				AttributeStreamBase newPositionStream = AttributeStreamBase
						.createAttributeStreamWithSemantics(Semantics.POSITION,
								1);
				AttributeStreamOfDbl position = (AttributeStreamOfDbl) newPositionStream;
				position.write(0, x);
				position.write(1, y);

				multipointImpl.setAttributeStreamRef(Semantics.POSITION,
						newPositionStream);
				multipointImpl.resize(1);
			}

			if (bZs) {
				multipointImpl.addAttribute(Semantics.Z);
				if (!bEmpty
						&& !VertexDescription.isDefaultValue(Semantics.Z, z)) {
					AttributeStreamBase newZStream = AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.Z, 1);
					newZStream.writeAsDbl(0, z);
					multipointImpl.setAttributeStreamRef(Semantics.Z,
							newZStream);
				}
			}

			if (bMs) {
				multipointImpl.addAttribute(Semantics.M);
				if (!bEmpty
						&& !VertexDescription.isDefaultValue(Semantics.M, m)) {
					AttributeStreamBase newMStream = AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.M, 1);
					newMStream.writeAsDbl(0, m);
					multipointImpl.setAttributeStreamRef(Semantics.M,
							newMStream);
				}
			}

			if (bIDs) {
				multipointImpl.addAttribute(Semantics.ID);
				if (!bEmpty
						&& !VertexDescription.isDefaultValue(Semantics.ID, id)) {
					AttributeStreamBase newIDStream = AttributeStreamBase
							.createAttributeStreamWithSemantics(Semantics.ID, 1);
					newIDStream.writeAsInt(0, id);
					multipointImpl.setAttributeStreamRef(Semantics.ID,
							newIDStream);
				}
			}

			return (Geometry) newmultipoint;
		} else if (m_type == Geometry.GeometryType.Envelope) {
			Envelope envelope = new Envelope();
			envelope.setCoords(x, y, x, y);

			if (bZs) {
				Envelope1D interval = new Envelope1D();
				interval.vmin = z;
				interval.vmax = z;
				envelope.addAttribute(Semantics.Z);
				envelope.setInterval(Semantics.Z, 0, interval);
			}

			if (bMs) {
				Envelope1D interval = new Envelope1D();
				interval.vmin = m;
				interval.vmax = m;
				envelope.addAttribute(Semantics.M);
				envelope.setInterval(Semantics.M, 0, interval);
			}

			if (bIDs) {
				Envelope1D interval = new Envelope1D();
				interval.vmin = id;
				interval.vmax = id;
				envelope.addAttribute(Semantics.ID);
				envelope.setInterval(Semantics.ID, 0, interval);
			}

			return (Geometry) envelope;
		}

		Point point = new Point();

		if (!bEmpty) {
			point.setX(Interop.translateFromAVNaN(x));
			point.setY(Interop.translateFromAVNaN(y));
		}

		// read Z
		if (bZs) {
			point.addAttribute(Semantics.Z);
			if (!bEmpty)
				point.setZ(Interop.translateFromAVNaN(z));
		}

		// read M
		if (bMs) {
			point.addAttribute(Semantics.M);
			if (!bEmpty)
				point.setM(Interop.translateFromAVNaN(m));
		}

		// read ID
		if (bIDs) {
			point.addAttribute(Semantics.ID);
			if (!bEmpty)
				point.setID(id);
		}

		return (Geometry) point;
	}

}
