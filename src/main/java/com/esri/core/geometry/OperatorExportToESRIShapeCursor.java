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

import com.esri.core.geometry.VertexDescription.Semantics;

import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class OperatorExportToESRIShapeCursor extends ByteBufferCursor {
	GeometryCursor m_inputGeometryCursor;
	int m_exportFlags;
	int m_index;
	ByteBuffer m_shapeBuffer;

	public OperatorExportToESRIShapeCursor(int exportFlags,
			GeometryCursor geometryCursor) {
		m_index = -1;
		if (geometryCursor == null)
			throw new GeometryException("invalid argument");

		m_exportFlags = exportFlags;
		m_inputGeometryCursor = geometryCursor;
		m_shapeBuffer = null;
	}

	@Override
	public int getByteBufferID() {
		return m_index;
	}

	@Override
	public ByteBuffer next() {
		Geometry geometry = m_inputGeometryCursor.next();
		if (geometry != null) {
			m_index = m_inputGeometryCursor.getGeometryID();

			int size = exportToESRIShape(m_exportFlags, geometry, null);
			if (m_shapeBuffer == null || size > m_shapeBuffer.capacity())
				m_shapeBuffer = ByteBuffer.allocate(size).order(
						ByteOrder.LITTLE_ENDIAN);
			exportToESRIShape(m_exportFlags, geometry, m_shapeBuffer);
			return m_shapeBuffer;
		}
		return null;
	}

	static int exportToESRIShape(int exportFlags, Geometry geometry,
			ByteBuffer shapeBuffer) {
		if (geometry == null) {
			if (shapeBuffer != null)
				shapeBuffer.putInt(0, ShapeType.ShapeNull);

			return 4;
		}

		int type = geometry.getType().value();
		switch (type) {
		case Geometry.GeometryType.Polygon:
			return exportMultiPathToESRIShape(true, exportFlags,
					(MultiPath) geometry, shapeBuffer);
		case Geometry.GeometryType.Polyline:
			return exportMultiPathToESRIShape(false, exportFlags,
					(MultiPath) geometry, shapeBuffer);
		case Geometry.GeometryType.MultiPoint:
			return exportMultiPointToESRIShape(exportFlags,
					(MultiPoint) geometry, shapeBuffer);
		case Geometry.GeometryType.Point:
			return exportPointToESRIShape(exportFlags, (Point) geometry,
					shapeBuffer);
		case Geometry.GeometryType.Envelope:
			return exportEnvelopeToESRIShape(exportFlags, (Envelope) geometry,
					shapeBuffer);
		default: {
			throw GeometryException.GeometryInternalError();
			// return -1;
		}
		}
	}

	private static int exportEnvelopeToESRIShape(int exportFlags,
			Envelope envelope, ByteBuffer shapeBuffer) {
		boolean bExportZs = envelope.hasAttribute(Semantics.Z)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripZs) == 0;
		boolean bExportMs = envelope.hasAttribute(Semantics.M)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripMs) == 0;
		boolean bExportIDs = envelope.hasAttribute(Semantics.ID)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripIDs) == 0;
		boolean bArcViewNaNs = (exportFlags & ShapeExportFlags.ShapeExportTrueNaNs) == 0;

		boolean bEmpty = envelope.isEmpty();
		int partCount = bEmpty ? 0 : 1;
		int pointCount = bEmpty ? 0 : 5;

		int size = (4 /* type */) + (4 * 8 /* envelope */) + (4 /* part count */)
				+ (4 /* point count */) + (partCount * 4 /* start indices */)
				+ pointCount * 2 * 8 /* xy coordinates */;

		if (bExportZs)
			size += (2 * 8 /* min max */) + (pointCount * 8 /* zs */);
		if (bExportMs)
			size += (2 * 8 /* min max */) + (pointCount * 8 /* ms */);
		if (bExportIDs)
			size += (pointCount * 4 /* ids */);

		if (shapeBuffer == null)
			return size;
		else if (shapeBuffer.capacity() < size)
			throw new GeometryException("buffer is too small");

		int type;

		// Determine the shape type
		if (!bExportZs && !bExportMs) {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralPolygon
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePolygon;
		} else if (bExportZs && !bExportMs) {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralPolygon
						| ShapeModifiers.ShapeHasZs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePolygonZ;
		} else if (bExportMs && !bExportZs) {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralPolygon
						| ShapeModifiers.ShapeHasMs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePolygonM;
		} else {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralPolygon
						| ShapeModifiers.ShapeHasZs | ShapeModifiers.ShapeHasMs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePolygonZM;
		}

		int offset = 0;

		// write type
		shapeBuffer.putInt(offset, type);
		offset += 4;

		// write Envelope
		Envelope2D env = new Envelope2D();
		envelope.queryEnvelope2D(env); // calls _VerifyAllStreams
		shapeBuffer.putDouble(offset, env.xmin);
		offset += 8;
		shapeBuffer.putDouble(offset, env.ymin);
		offset += 8;
		shapeBuffer.putDouble(offset, env.xmax);
		offset += 8;
		shapeBuffer.putDouble(offset, env.ymax);
		offset += 8;

		// write part count
		shapeBuffer.putInt(offset, partCount);
		offset += 4;

		// write pointCount
		shapeBuffer.putInt(offset, pointCount);
		offset += 4;

		if (!bEmpty) {
			// write start index
			shapeBuffer.putInt(offset, 0);
			offset += 4;

			// write xy coordinates
			shapeBuffer.putDouble(offset, env.xmin);
			offset += 8;
			shapeBuffer.putDouble(offset, env.ymin);
			offset += 8;

			shapeBuffer.putDouble(offset, env.xmin);
			offset += 8;
			shapeBuffer.putDouble(offset, env.ymax);
			offset += 8;

			shapeBuffer.putDouble(offset, env.xmax);
			offset += 8;
			shapeBuffer.putDouble(offset, env.ymax);
			offset += 8;

			shapeBuffer.putDouble(offset, env.xmax);
			offset += 8;
			shapeBuffer.putDouble(offset, env.ymin);
			offset += 8;

			shapeBuffer.putDouble(offset, env.xmin);
			offset += 8;
			shapeBuffer.putDouble(offset, env.ymin);
			offset += 8;
		}
		// write Zs
		if (bExportZs) {
			Envelope1D zInterval;
			zInterval = envelope.queryInterval(Semantics.Z, 0);

			double zmin = bArcViewNaNs ? Interop
					.translateToAVNaN(zInterval.vmin) : zInterval.vmin;
			double zmax = bArcViewNaNs ? Interop
					.translateToAVNaN(zInterval.vmax) : zInterval.vmax;

			// write min max values
			shapeBuffer.putDouble(offset, zmin);
			offset += 8;
			shapeBuffer.putDouble(offset, zmax);
			offset += 8;

			if (!bEmpty) {
				// write arbitrary z values
				shapeBuffer.putDouble(offset, zmin);
				offset += 8;
				shapeBuffer.putDouble(offset, zmax);
				offset += 8;
				shapeBuffer.putDouble(offset, zmin);
				offset += 8;
				shapeBuffer.putDouble(offset, zmax);
				offset += 8;
				shapeBuffer.putDouble(offset, zmin);
				offset += 8;
			}
		}
		// write Ms
		if (bExportMs) {
			Envelope1D mInterval;
			mInterval = envelope.queryInterval(Semantics.M, 0);

			double mmin = bArcViewNaNs ? Interop
					.translateToAVNaN(mInterval.vmin) : mInterval.vmin;
			double mmax = bArcViewNaNs ? Interop
					.translateToAVNaN(mInterval.vmax) : mInterval.vmax;

			// write min max values
			shapeBuffer.putDouble(offset, mmin);
			offset += 8;
			shapeBuffer.putDouble(offset, mmax);
			offset += 8;

			if (!bEmpty) {
				// write arbitrary m values
				shapeBuffer.putDouble(offset, mmin);
				offset += 8;
				shapeBuffer.putDouble(offset, mmax);
				offset += 8;
				shapeBuffer.putDouble(offset, mmin);
				offset += 8;
				shapeBuffer.putDouble(offset, mmax);
				offset += 8;
				shapeBuffer.putDouble(offset, mmin);
				offset += 8;
			}
		}

		// write IDs
		if (bExportIDs && !bEmpty) {
			Envelope1D idInterval;
			idInterval = envelope.queryInterval(Semantics.ID, 0);

			int idmin = (int) idInterval.vmin;
			int idmax = (int) idInterval.vmax;

			// write arbitrary id values
			shapeBuffer.putInt(offset, idmin);
			offset += 4;
			shapeBuffer.putInt(offset, idmax);
			offset += 4;
			shapeBuffer.putInt(offset, idmin);
			offset += 4;
			shapeBuffer.putInt(offset, idmax);
			offset += 4;
			shapeBuffer.putInt(offset, idmin);
			offset += 4;
		}

		return offset;
	}

	private static int exportPointToESRIShape(int exportFlags, Point point,
			ByteBuffer shapeBuffer) {
		boolean bExportZ = point.hasAttribute(Semantics.Z)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripZs) == 0;
		boolean bExportM = point.hasAttribute(Semantics.M)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripMs) == 0;
		boolean bExportID = point.hasAttribute(Semantics.ID)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripIDs) == 0;
		boolean bArcViewNaNs = (exportFlags & ShapeExportFlags.ShapeExportTrueNaNs) == 0;

		int size = (4 /* type */) + (2 * 8 /* xy coordinate */);

		if (bExportZ)
			size += 8;
		if (bExportM)
			size += 8;
		if (bExportID)
			size += 4;

		if (shapeBuffer == null)
			return size;
		else if (shapeBuffer.capacity() < size)
			throw new GeometryException("buffer is too small");

		int type;

		// Determine the shape type
		if (!bExportZ && !bExportM) {
			if (bExportID)
				type = ShapeType.ShapeGeneralPoint | ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePoint;
		} else if (bExportZ && !bExportM) {
			if (bExportID)
				type = ShapeType.ShapeGeneralPoint | ShapeModifiers.ShapeHasZs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePointZ;
		} else if (bExportM && !bExportZ) {
			if (bExportID)
				type = ShapeType.ShapeGeneralPoint | ShapeModifiers.ShapeHasMs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePointM;
		} else {
			if (bExportID)
				type = ShapeType.ShapeGeneralPoint | ShapeModifiers.ShapeHasZs
						| ShapeModifiers.ShapeHasMs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapePointZM;
		}

		int offset = 0;

		// write type

		shapeBuffer.putInt(offset, type);
		offset += 4;

		boolean bEmpty = point.isEmpty();

		// write xy
		double x = !bEmpty ? point.getX() : NumberUtils.NaN();
		double y = !bEmpty ? point.getY() : NumberUtils.NaN();
		shapeBuffer.putDouble(offset,
				bArcViewNaNs ? Interop.translateToAVNaN(x) : x);
		offset += 8;
		shapeBuffer.putDouble(offset,
				bArcViewNaNs ? Interop.translateToAVNaN(y) : y);
		offset += 8;

		// write Z
		if (bExportZ) {
			double z = !bEmpty ? point.getZ() : NumberUtils.NaN();
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(z) : z);
			offset += 8;
		}

		// WriteM
		if (bExportM) {
			double m = !bEmpty ? point.getM() : NumberUtils.NaN();
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(m) : m);
			offset += 8;
		}

		// write ID
		if (bExportID) {
			int id = !bEmpty ? point.getID() : 0;
			shapeBuffer.putInt(offset, id);
			offset += 4;
		}

		return offset;
	}

	private static int exportMultiPointToESRIShape(int exportFlags,
			MultiPoint multipoint, ByteBuffer shapeBuffer) {
		MultiPointImpl multipointImpl = (MultiPointImpl) multipoint._getImpl();
		boolean bExportZs = multipointImpl.hasAttribute(Semantics.Z)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripZs) == 0;
		boolean bExportMs = multipointImpl.hasAttribute(Semantics.M)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripMs) == 0;
		boolean bExportIDs = multipointImpl.hasAttribute(Semantics.ID)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripIDs) == 0;
		boolean bArcViewNaNs = (exportFlags & ShapeExportFlags.ShapeExportTrueNaNs) == 0;

		int pointCount = multipointImpl.getPointCount();

		int size = (4 /* type */) + (4 * 8 /* envelope */) + (4 /* point count */)
				+ (pointCount * 2 * 8 /* xy coordinates */);

		if (bExportZs)
			size += (2 * 8 /* min max */) + (pointCount * 8 /* zs */);
		if (bExportMs)
			size += (2 * 8 /* min max */) + (pointCount * 8 /* ms */);
		if (bExportIDs)
			size += pointCount * 4 /* ids */;

		if (size >= NumberUtils.intMax())
			throw new GeometryException("invalid call");

		if (shapeBuffer == null)
			return size;
		else if (shapeBuffer.capacity() < size)
			throw new GeometryException("buffer is too small");

		int type;

		// Determine the shape type
		if (!bExportZs && !bExportMs) {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralMultiPoint
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapeMultiPoint;
		} else if (bExportZs && !bExportMs) {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralMultiPoint
						| ShapeModifiers.ShapeHasZs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapeMultiPointZ;
		} else if (bExportMs && !bExportZs) {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralMultiPoint
						| ShapeModifiers.ShapeHasMs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapeMultiPointM;
		} else {
			if (bExportIDs)
				type = ShapeType.ShapeGeneralMultiPoint
						| ShapeModifiers.ShapeHasZs | ShapeModifiers.ShapeHasMs
						| ShapeModifiers.ShapeHasIDs;
			else
				type = ShapeType.ShapeMultiPointZM;
		}

		// write type
		int offset = 0;

		shapeBuffer.putInt(offset, type);
		offset += 4;

		// write Envelope
		Envelope2D env = new Envelope2D();
		multipointImpl.queryEnvelope2D(env); // calls _VerifyAllStreams
		shapeBuffer.putDouble(offset, env.xmin);
		offset += 8;
		shapeBuffer.putDouble(offset, env.ymin);
		offset += 8;
		shapeBuffer.putDouble(offset, env.xmax);
		offset += 8;
		shapeBuffer.putDouble(offset, env.ymax);
		offset += 8;

		// write point count
		shapeBuffer.putInt(offset, pointCount);
		offset += 4;

		if (pointCount > 0) {
			// write xy coordinates
			AttributeStreamBase positionStream = multipointImpl
					.getAttributeStreamRef(Semantics.POSITION);
			AttributeStreamOfDbl position = (AttributeStreamOfDbl) positionStream;
			for (int i = 0; i < pointCount; i++) {
				double x = position.read(2 * i);
				double y = position.read(2 * i + 1);
				shapeBuffer.putDouble(offset, x);
				offset += 8;
				shapeBuffer.putDouble(offset, y);
				offset += 8;
			}
		}

		// write Zs
		if (bExportZs) {
			Envelope1D zInterval = multipointImpl.queryInterval(Semantics.Z, 0);
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(zInterval.vmin)
							: zInterval.vmin);
			offset += 8;
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(zInterval.vmax)
							: zInterval.vmax);
			offset += 8;

			if (pointCount > 0) {
				if (multipointImpl._attributeStreamIsAllocated(Semantics.Z)) {
					AttributeStreamOfDbl zs = (AttributeStreamOfDbl) multipointImpl
							.getAttributeStreamRef(Semantics.Z);
					for (int i = 0; i < pointCount; i++) {
						double z = zs.read(i);
						shapeBuffer.putDouble(offset,
								bArcViewNaNs ? Interop.translateToAVNaN(z) : z);
						offset += 8;
					}
				} else {
					double z = VertexDescription.getDefaultValue(Semantics.Z);

					if (bArcViewNaNs)
						z = Interop.translateToAVNaN(z);

					// Can we write a function that writes all these values at
					// once instead of doing a for loop?
					for (int i = 0; i < pointCount; i++)
						shapeBuffer.putDouble(offset, z);
					offset += 8;
				}
			}
		}

		// write Ms
		if (bExportMs) {
			Envelope1D mInterval = multipointImpl.queryInterval(Semantics.M, 0);
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(mInterval.vmin)
							: mInterval.vmin);
			offset += 8;
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(mInterval.vmax)
							: mInterval.vmax);
			offset += 8;

			if (pointCount > 0) {
				if (multipointImpl._attributeStreamIsAllocated(Semantics.M)) {
					AttributeStreamOfDbl ms = (AttributeStreamOfDbl) multipointImpl
							.getAttributeStreamRef(Semantics.M);
					for (int i = 0; i < pointCount; i++) {
						double m = ms.read(i);
						shapeBuffer.putDouble(offset,
								bArcViewNaNs ? Interop.translateToAVNaN(m) : m);
						offset += 8;
					}
				} else {
					double m = VertexDescription.getDefaultValue(Semantics.M);

					if (bArcViewNaNs)
						m = Interop.translateToAVNaN(m);

					for (int i = 0; i < pointCount; i++)
						shapeBuffer.putDouble(offset, m);
					offset += 8;
				}
			}
		}

		// write IDs
		if (bExportIDs) {
			if (pointCount > 0) {
				if (multipointImpl._attributeStreamIsAllocated(Semantics.ID)) {
					AttributeStreamOfInt32 ids = (AttributeStreamOfInt32) multipointImpl
							.getAttributeStreamRef(Semantics.ID);
					for (int i = 0; i < pointCount; i++) {
						int id = ids.read(i);
						shapeBuffer.putInt(offset, id);
						offset += 4;
					}
				} else {
					int id = (int) VertexDescription
							.getDefaultValue(Semantics.ID);
					for (int i = 0; i < pointCount; i++)
						shapeBuffer.putInt(offset, id);
					offset += 4;
				}
			}
		}

		return offset;
	}

	static private void coverageHelper(String id){
		String tempFilePath = "target/temp/coverage_exportMultiPathToESRIShape.txt";

		// Create a File object with the specified path
 		File tempFile = new File(tempFilePath);

 		try {
 			// Check if the file exists
 			if (!tempFile.exists()) {
 				// Create the file if it doesn't exist
 				if (tempFile.getParentFile() != null) {
 					tempFile.getParentFile().mkdirs(); // Create parent directories if necessary
 				}
 				tempFile.createNewFile(); // Create the file
 				System.out.println("Temporary file created at: " + tempFile.getAbsolutePath());
 			}
 			FileWriter writer = new FileWriter(tempFile, true);
 			// Write the new content to the file
 			writer.write(id);
 			writer.write(System.lineSeparator()); // Add a newline after the new content

 			// Close the FileWriter
 			writer.close();

 		} catch (Exception e) {
 			System.err.println(e.getMessage());
 		}
	}

	/**
	 * Cyclomatic Complexity
	 * Decisions: if: 42, &&: 6, ||: 9, ?: 14, while: 0, for: 13 = 84
	 * Exit points: return: 2, throw 2
	 * Result: 84 - 4 + 2 = 82
	 */
	private static int exportMultiPathToESRIShape(boolean bPolygon,
			int exportFlags, MultiPath multipath, ByteBuffer shapeBuffer) {
		coverageHelper("0");
		MultiPathImpl multipathImpl = (MultiPathImpl) multipath._getImpl();

		boolean bExportZs = multipathImpl.hasAttribute(Semantics.Z)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripZs) == 0;
		boolean bExportMs = multipathImpl.hasAttribute(Semantics.M)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripMs) == 0;
		boolean bExportIDs = multipathImpl.hasAttribute(Semantics.ID)
				&& (exportFlags & ShapeExportFlags.ShapeExportStripIDs) == 0;
		boolean bHasCurves = multipathImpl.hasNonLinearSegments();
		boolean bArcViewNaNs = (exportFlags & ShapeExportFlags.ShapeExportTrueNaNs) == 0;

		int partCount = multipathImpl.getPathCount();
		int pointCount = multipathImpl.getPointCount();

		if (!bPolygon) {
			coverageHelper("1");
			for (int ipart = 0; ipart < partCount; ipart++){
				coverageHelper("2");
				if (multipath.isClosedPath(ipart)){
					coverageHelper("3");
					pointCount++;
				}
				else{
					coverageHelper("4");
				}
			}
		} else{
			coverageHelper("5");
			pointCount += partCount;
		}

		int size = (4 /* type */) + (4 * 8 /* envelope */) + (4 /* part count */)
				+ (4 /* point count */) + (partCount * 4 /* start indices */)
				+ pointCount * 2 * 8 /* xy coordinates */;

		if (bExportZs){
			coverageHelper("6");
			size += (2 * 8 /* min max */) + (pointCount * 8 /* zs */);
		}
		else{
			coverageHelper("7");
		}
		if (bExportMs){
			coverageHelper("8");
			size += (2 * 8 /* min max */) + (pointCount * 8 /* ms */);
		}
		else{
			coverageHelper("9");
		}
		if (bExportIDs){
			coverageHelper("10");
			size += pointCount * 4 /* ids */;
		}
		else{
			coverageHelper("11");
		}
		if (bHasCurves) {
			coverageHelper("11");
			// to-do: curves
		}
		else{
			coverageHelper("12");
		}

		if (size >= NumberUtils.intMax()){
			coverageHelper("13");
			throw new GeometryException("invalid call");
		}
		else{
			coverageHelper("14");
		}
		if (shapeBuffer == null){
			coverageHelper("15");
			return size;
		}		
		else if (shapeBuffer.capacity() < size){
			coverageHelper("16");
			throw new GeometryException("buffer is too small");
		}
		else{
			coverageHelper("17");
		}
		int offset = 0;

		// Determine the shape type
		int type;
		if (!bExportZs && !bExportMs) {
			coverageHelper("18");
			if (bExportIDs || bHasCurves) {
				coverageHelper("19");
				type = bPolygon ? ShapeType.ShapeGeneralPolygon
						: ShapeType.ShapeGeneralPolyline;
				if (bExportIDs){
					coverageHelper("20");
					type |= ShapeModifiers.ShapeHasIDs;
				}
				else{
					coverageHelper("21");
				}
				if (bHasCurves){
					coverageHelper("22");
					type |= ShapeModifiers.ShapeHasCurves;
				}
				else{
					coverageHelper("23");
				}
			} else{
				coverageHelper("24");
				type = bPolygon ? ShapeType.ShapePolygon
				: ShapeType.ShapePolyline;
			}
		} else if (bExportZs && !bExportMs) {
			coverageHelper("25");
			if (bExportIDs || bHasCurves) {
				coverageHelper("26");
				type = bPolygon ? ShapeType.ShapeGeneralPolygon
						: ShapeType.ShapeGeneralPolyline;
				type |= ShapeModifiers.ShapeHasZs;
				if (bExportIDs){
					coverageHelper("27");
					type |= ShapeModifiers.ShapeHasIDs;
				}
				else{
					coverageHelper("28");
				}
				if (bHasCurves){
					coverageHelper("29");
					type |= ShapeModifiers.ShapeHasCurves;
				}
				else{
					coverageHelper("30");
				}
			} else{
				coverageHelper("31");
				type = bPolygon ? ShapeType.ShapePolygonZ
				: ShapeType.ShapePolylineZ;
			}
		} else if (bExportMs && !bExportZs) {
			coverageHelper("32");
			if (bExportIDs || bHasCurves) {
				coverageHelper("33");
				type = bPolygon ? ShapeType.ShapeGeneralPolygon
						: ShapeType.ShapeGeneralPolyline;
				type |= ShapeModifiers.ShapeHasMs;
				if (bExportIDs){
					coverageHelper("34");
					type |= ShapeModifiers.ShapeHasIDs;
				}
				else{
					coverageHelper("35");
				}
				if (bHasCurves){
					coverageHelper("36");
					type |= ShapeModifiers.ShapeHasCurves;
				}
				else{
					coverageHelper("37");
				}
			} else{
				coverageHelper("38");
				type = bPolygon ? ShapeType.ShapePolygonM
				: ShapeType.ShapePolylineM;
			}
		} else {
			coverageHelper("39");
			if (bExportIDs || bHasCurves) {
				coverageHelper("40");
				type = bPolygon ? ShapeType.ShapeGeneralPolygon
						: ShapeType.ShapeGeneralPolyline;
				type |= ShapeModifiers.ShapeHasZs | ShapeModifiers.ShapeHasMs;
				if (bExportIDs){
					coverageHelper("41");
					type |= ShapeModifiers.ShapeHasIDs;
				}
				else{
					coverageHelper("42");
				}
				if (bHasCurves){
					coverageHelper("43");
					type |= ShapeModifiers.ShapeHasCurves;
				}
				else{
					coverageHelper("44");
				}
			} else{
				coverageHelper("45");
				type = bPolygon ? ShapeType.ShapePolygonZM
				: ShapeType.ShapePolylineZM;
			}
		}

		// write type
		shapeBuffer.putInt(offset, type);
		offset += 4;

		// write Envelope
		Envelope2D env = new Envelope2D();
		multipathImpl.queryEnvelope2D(env); // calls _VerifyAllStreams
		shapeBuffer.putDouble(offset, env.xmin);
		offset += 8;
		shapeBuffer.putDouble(offset, env.ymin);
		offset += 8;
		shapeBuffer.putDouble(offset, env.xmax);
		offset += 8;
		shapeBuffer.putDouble(offset, env.ymax);
		offset += 8;

		// write part count
		shapeBuffer.putInt(offset, partCount);
		offset += 4; // to-do: return error if larger than 2^32 - 1

		// write pointCount
		shapeBuffer.putInt(offset, pointCount);
		offset += 4;

		// write start indices for each part
		int pointIndexDelta = 0;
		for (int ipart = 0; ipart < partCount; ipart++) {
			coverageHelper("46");
			int istart = multipathImpl.getPathStart(ipart) + pointIndexDelta;
			shapeBuffer.putInt(offset, istart);
			offset += 4;
			if (bPolygon || multipathImpl.isClosedPath(ipart)){
				coverageHelper("47");
				pointIndexDelta++;
			}
			else{
				coverageHelper("48");
			}
		}

		if (pointCount > 0) {
			coverageHelper("49");
			// write xy coordinates
			AttributeStreamBase positionStream = multipathImpl
					.getAttributeStreamRef(Semantics.POSITION);
			AttributeStreamOfDbl position = (AttributeStreamOfDbl) positionStream;

			for (int ipart = 0; ipart < partCount; ipart++) {
				coverageHelper("50");
				int partStart = multipathImpl.getPathStart(ipart);
				int partEnd = multipathImpl.getPathEnd(ipart);
				for (int i = partStart; i < partEnd; i++) {
					coverageHelper("51");
					double x = position.read(2 * i);
					double y = position.read(2 * i + 1);

					shapeBuffer.putDouble(offset, x);
					offset += 8;
					shapeBuffer.putDouble(offset, y);
					offset += 8;
				}

				// If the part is closed, then we need to duplicate the start
				// point
				if (bPolygon || multipathImpl.isClosedPath(ipart)) {
					coverageHelper("52");
					double x = position.read(2 * partStart);
					double y = position.read(2 * partStart + 1);

					shapeBuffer.putDouble(offset, x);
					offset += 8;
					shapeBuffer.putDouble(offset, y);
					offset += 8;
				}
				else{
					coverageHelper("53");
				}
			}
		}

		// write Zs
		if (bExportZs) {
			coverageHelper("54");
			Envelope1D zInterval = multipathImpl.queryInterval(Semantics.Z, 0);
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(zInterval.vmin)
							: zInterval.vmin);
			offset += 8;
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(zInterval.vmax)
							: zInterval.vmax);
			offset += 8;

			if (pointCount > 0) {
				coverageHelper("55");
				if (multipathImpl._attributeStreamIsAllocated(Semantics.Z)) {
					coverageHelper("56");
					AttributeStreamOfDbl zs = (AttributeStreamOfDbl) multipathImpl
							.getAttributeStreamRef(Semantics.Z);
					for (int ipart = 0; ipart < partCount; ipart++) {
						coverageHelper("57");
						int partStart = multipathImpl.getPathStart(ipart);
						int partEnd = multipathImpl.getPathEnd(ipart);
						for (int i = partStart; i < partEnd; i++) {
							coverageHelper("58");
							double z = zs.read(i);
							shapeBuffer.putDouble(offset,
									bArcViewNaNs ? Interop.translateToAVNaN(z)
											: z);
							offset += 8;
						}

						// If the part is closed, then we need to duplicate the
						// start z
						if (bPolygon || multipathImpl.isClosedPath(ipart)) {
							coverageHelper("59");
							double z = zs.read(partStart);
							shapeBuffer.putDouble(offset, z);
							offset += 8;
						}
						else{
							coverageHelper("60");
						}
					}
				} else {
					coverageHelper("61");
					double z = VertexDescription.getDefaultValue(Semantics.Z);

					if (bArcViewNaNs){
						coverageHelper("62");
						z = Interop.translateToAVNaN(z);
					}
					else{
						coverageHelper("63");
					}

					for (int i = 0; i < pointCount; i++){
						coverageHelper("64");
						shapeBuffer.putDouble(offset, z);
					}
					offset += 8;
				}
			}
		}
		else{
			coverageHelper("65");
		}

		// write Ms
		if (bExportMs) {
			coverageHelper("66");
			Envelope1D mInterval = multipathImpl.queryInterval(Semantics.M, 0);
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(mInterval.vmin)
							: mInterval.vmin);
			offset += 8;
			shapeBuffer.putDouble(offset,
					bArcViewNaNs ? Interop.translateToAVNaN(mInterval.vmax)
							: mInterval.vmax);
			offset += 8;

			if (pointCount > 0) {
				coverageHelper("67");
				if (multipathImpl._attributeStreamIsAllocated(Semantics.M)) {
					coverageHelper("68");
					AttributeStreamOfDbl ms = (AttributeStreamOfDbl) multipathImpl
							.getAttributeStreamRef(Semantics.M);
					for (int ipart = 0; ipart < partCount; ipart++) {
						coverageHelper("69");
						int partStart = multipathImpl.getPathStart(ipart);
						int partEnd = multipathImpl.getPathEnd(ipart);
						for (int i = partStart; i < partEnd; i++) {
							coverageHelper("70");
							double m = ms.read(i);
							shapeBuffer.putDouble(offset,
									bArcViewNaNs ? Interop.translateToAVNaN(m)
											: m);
							offset += 8;
						}

						// If the part is closed, then we need to duplicate the
						// start m
						if (bPolygon || multipathImpl.isClosedPath(ipart)) {
							coverageHelper("71");
							double m = ms.read(partStart);
							shapeBuffer.putDouble(offset, m);
							offset += 8;
						}
						else{
							coverageHelper("72");
						}
					}
				} else {
					coverageHelper("73");
					double m = VertexDescription.getDefaultValue(Semantics.M);

					if (bArcViewNaNs){
						coverageHelper("74");
						m = Interop.translateToAVNaN(m);
					}
					else{
						coverageHelper("75");
					}

					for (int i = 0; i < pointCount; i++){
						coverageHelper("76");
						shapeBuffer.putDouble(offset, m);
					}
					offset += 8;
				}
			}
		}
		else{
			coverageHelper("77");
		}

		// write Curves
		if (bHasCurves) {
			coverageHelper("78");
			// to-do: We'll finish this later
		}
		else{
			coverageHelper("79");
		}

		// write IDs
		if (bExportIDs) {
			coverageHelper("80");
			if (pointCount > 0) {
				coverageHelper("81");
				if (multipathImpl._attributeStreamIsAllocated(Semantics.ID)) {
					coverageHelper("82");
					AttributeStreamOfInt32 ids = (AttributeStreamOfInt32) multipathImpl
							.getAttributeStreamRef(Semantics.ID);
					for (int ipart = 0; ipart < partCount; ipart++) {
						coverageHelper("83");
						int partStart = multipathImpl.getPathStart(ipart);
						int partEnd = multipathImpl.getPathEnd(ipart);
						for (int i = partStart; i < partEnd; i++) {
							coverageHelper("84");
							int id = ids.read(i);
							shapeBuffer.putInt(offset, id);
							offset += 4;
						}

						// If the part is closed, then we need to duplicate the
						// start id
						if (bPolygon || multipathImpl.isClosedPath(ipart)) {
							coverageHelper("85");
							int id = ids.read(partStart);
							shapeBuffer.putInt(offset, id);
							offset += 4;
						}
						else{
							coverageHelper("86");
						}
					}
				} else {
					coverageHelper("87");
					int id = (int) VertexDescription
							.getDefaultValue(Semantics.ID);
					for (int i = 0; i < pointCount; i++){
						coverageHelper("88");
						shapeBuffer.putInt(offset, id);
					}
					offset += 4;
				}
			}
		}
		else{
			coverageHelper("89");
		}

		return offset;
	}

}
