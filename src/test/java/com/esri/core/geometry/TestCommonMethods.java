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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.TestCase;
import org.junit.Test;

public class TestCommonMethods extends TestCase {
	public static boolean compareDouble(double a, double b, double tol) {
		double diff = Math.abs(a - b);
		return diff <= tol * Math.abs(a) + tol;
	}

	public static Point[] pointsFromMultiPath(MultiPath geom) {
		int numberOfPoints = geom.getPointCount();
		Point[] points = new Point[numberOfPoints];
		for (int i = 0; i < geom.getPointCount(); i++) {
			points[i] = geom.getPoint(i);
		}
		return points;
	}

	@Test
	public static void compareGeometryContent(MultiVertexGeometry geom1,
			MultiVertexGeometry geom2) {
		// Geometry types
		assertTrue(geom1.getType().value() == geom2.getType().value());

		// Envelopes
		Envelope2D env1 = new Envelope2D();
		geom1.queryEnvelope2D(env1);

		Envelope2D env2 = new Envelope2D();
		geom2.queryEnvelope2D(env2);

		assertTrue(env1.xmin == env2.xmin && env1.xmax == env2.xmax
				&& env1.ymin == env2.ymin && env1.ymax == env2.ymax);

		int type = geom1.getType().value();
		if (type == Geometry.GeometryType.Polyline
				|| type == Geometry.GeometryType.Polygon) {
			// Part Count
			int partCount1 = ((MultiPath) geom1).getPathCount();
			int partCount2 = ((MultiPath) geom2).getPathCount();
			assertTrue(partCount1 == partCount2);

			// Part indices
			for (int i = 0; i < partCount1; i++) {
				int start1 = ((MultiPath) geom1).getPathStart(i);
				int start2 = ((MultiPath) geom2).getPathStart(i);
				assertTrue(start1 == start2);
				int end1 = ((MultiPath) geom1).getPathEnd(i);
				int end2 = ((MultiPath) geom2).getPathEnd(i);
				assertTrue(end1 == end2);
			}
		}

		// Point count
		int pointCount1 = geom1.getPointCount();
		int pointCount2 = geom2.getPointCount();
		assertTrue(pointCount1 == pointCount2);

		if (type == Geometry.GeometryType.MultiPoint
				|| type == Geometry.GeometryType.Polyline
				|| type == Geometry.GeometryType.Polygon) {
			// POSITION
			AttributeStreamBase positionStream1 = ((MultiVertexGeometryImpl) geom1
					._getImpl())
					.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			AttributeStreamOfDbl position1 = (AttributeStreamOfDbl) (positionStream1);

			AttributeStreamBase positionStream2 = ((MultiVertexGeometryImpl) geom2
					._getImpl())
					.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			AttributeStreamOfDbl position2 = (AttributeStreamOfDbl) (positionStream2);

			for (int i = 0; i < pointCount1; i++) {
				double x1 = position1.read(2 * i);
				double x2 = position2.read(2 * i);
				assertTrue(x1 == x2);

				double y1 = position1.read(2 * i + 1);
				double y2 = position2.read(2 * i + 1);
				assertTrue(y1 == y2);
			}

			// Zs
			boolean bHasZs1 = geom1.hasAttribute(VertexDescription.Semantics.Z);
			boolean bHasZs2 = geom2.hasAttribute(VertexDescription.Semantics.Z);
			assertTrue(bHasZs1 == bHasZs2);

			if (bHasZs1) {
				AttributeStreamBase zStream1 = ((MultiVertexGeometryImpl) geom1
						._getImpl())
						.getAttributeStreamRef(VertexDescription.Semantics.Z);
				AttributeStreamOfDbl zs1 = (AttributeStreamOfDbl) (zStream1);

				AttributeStreamBase zStream2 = ((MultiVertexGeometryImpl) geom2
						._getImpl())
						.getAttributeStreamRef(VertexDescription.Semantics.Z);
				AttributeStreamOfDbl zs2 = (AttributeStreamOfDbl) (zStream2);

				for (int i = 0; i < pointCount1; i++) {
					double z1 = zs1.read(i);
					double z2 = zs2.read(i);
					assertTrue(z1 == z2);
				}
			}

			// Ms
			boolean bHasMs1 = geom1.hasAttribute(VertexDescription.Semantics.M);
			boolean bHasMs2 = geom2.hasAttribute(VertexDescription.Semantics.M);
			assertTrue(bHasMs1 == bHasMs2);

			if (bHasMs1) {
				AttributeStreamBase mStream1 = ((MultiVertexGeometryImpl) geom1
						._getImpl())
						.getAttributeStreamRef(VertexDescription.Semantics.M);
				AttributeStreamOfDbl ms1 = (AttributeStreamOfDbl) (mStream1);

				AttributeStreamBase mStream2 = ((MultiVertexGeometryImpl) geom2
						._getImpl())
						.getAttributeStreamRef(VertexDescription.Semantics.M);
				AttributeStreamOfDbl ms2 = (AttributeStreamOfDbl) (mStream2);

				for (int i = 0; i < pointCount1; i++) {
					double m1 = ms1.read(i);
					double m2 = ms2.read(i);
					assertTrue(m1 == m2);
				}
			}

			// IDs
			boolean bHasIDs1 = geom1
					.hasAttribute(VertexDescription.Semantics.ID);
			boolean bHasIDs2 = geom2
					.hasAttribute(VertexDescription.Semantics.ID);
			assertTrue(bHasIDs1 == bHasIDs2);

			if (bHasIDs1) {
				AttributeStreamBase idStream1 = ((MultiVertexGeometryImpl) geom1
						._getImpl())
						.getAttributeStreamRef(VertexDescription.Semantics.ID);
				AttributeStreamOfInt32 ids1 = (AttributeStreamOfInt32) (idStream1);

				AttributeStreamBase idStream2 = ((MultiVertexGeometryImpl) geom2
						._getImpl())
						.getAttributeStreamRef(VertexDescription.Semantics.ID);
				AttributeStreamOfInt32 ids2 = (AttributeStreamOfInt32) (idStream2);

				for (int i = 0; i < pointCount1; i++) {
					int id1 = ids1.read(i);
					int id2 = ids2.read(i);
					assertTrue(id1 == id2);
				}
			}
		}
	}

	@Test
	public static void compareGeometryContent(MultiPoint geom1, MultiPoint geom2) {
		// Geometry types
		assertTrue(geom1.getType().value() == geom2.getType().value());

		// Envelopes
		Envelope env1 = new Envelope();
		geom1.queryEnvelope(env1);

		Envelope env2 = new Envelope();
		geom2.queryEnvelope(env2);

		assertTrue(env1.getXMin() == env2.getXMin()
				&& env1.getXMax() == env2.getXMax()
				&& env1.getYMin() == env2.getYMin()
				&& env1.getYMax() == env2.getYMax());

		// Point count
		int pointCount1 = geom1.getPointCount();
		int pointCount2 = geom2.getPointCount();
		assertTrue(pointCount1 == pointCount2);

		Point point1;
		Point point2;

		for (int i = 0; i < pointCount1; i++) {
			point1 = geom1.getPoint(i);
			point2 = geom2.getPoint(i);

			double x1 = point1.getX();
			double x2 = point2.getX();
			assertTrue(x1 == x2);

			double y1 = point1.getY();
			double y2 = point2.getY();
			assertTrue(y1 == y2);
		}
	}

	@Test
	public static void testNothing() {

	}

	public static boolean writeObjectToFile(String fileName, Object obj) {
		try {
			File f = new File(fileName);
			f.setWritable(true);

			FileOutputStream fout = new FileOutputStream(f);
			ObjectOutputStream oo = new ObjectOutputStream(fout);
			oo.writeObject(obj);
			fout.close();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static Object readObjectFromFile(String fileName) {
		try {
			File f = new File(fileName);
			f.setReadable(true);

			FileInputStream streamIn = new FileInputStream(f);
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Object obj = ii.readObject();
			streamIn.close();
			return obj;
		} catch (Exception ex) {
			return null;
		}
	}

	public static MapGeometry fromJson(String jsonString) {
		try {
			return OperatorImportFromJson.local().execute(Geometry.Type.Unknown, jsonString);
		} catch (Exception ex) {
		}

		return null;
	}
}
