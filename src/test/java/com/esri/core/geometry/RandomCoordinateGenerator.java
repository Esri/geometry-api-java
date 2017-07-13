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

import java.util.Random;
import java.util.Vector;

public class RandomCoordinateGenerator {

	// final Point openString[] = { new Point(1220, 1320),
	// new Point(1220, 2320), new Point(3000, 2320),
	// new Point(3520, 1720), new Point(3000, 1320), };
	//
	// final Point3D openString3[] = { new Point3D(1220, 1320, 11),
	// new Point3D(1220, 2320, 2), new Point3D(3000, 2320, 3),
	// new Point3D(3520, 1720, 4), new Point3D(3000, 1320, 5), };

	Vector<Point> points;
	Envelope env;
	int maxcount;
	double tolerance;
	double maxlen;
	Random random = new Random(1973);

	Point _GenerateNewPoint() {
		if (points.size() == maxcount)
			return _RandomizeExisting();
		Point pt;
		double f = random.nextDouble() - 0.5;
		if (points.size() == 0)
			pt = env.getCenter();
		else
			pt = points.lastElement();
		// pt.x = pt.x + env.Width() * f;
		pt.setX(pt.getX() + maxlen * f);
		f = 1.0 * random.nextDouble() - 0.5;
		pt.setY(pt.getY() + env.getHeight() * f);
		pt.setY(pt.getY() + maxlen * f);
		pt = _snapClip(pt, env);
		points.add(pt);
		return pt;
	}

	Point _RandomizeExisting() {
		if (points.size() == 0) {
			return _GenerateNewPoint();
		}

		double f = random.nextDouble();
		int num = (int) (f * points.size());
		Point pt = points.get(num);

		f = random.nextDouble();

		// if (f > 0.9)
		if (f > 2) {
			f = random.nextDouble();
			pt.setX(pt.getX() + (1 - 2 * f) * 2 * tolerance);
			f = random.nextDouble();
			pt.setY(pt.getY() + (1 - 2 * f) * 2 * tolerance);
			pt = _snapClip(pt, env);
		}
		return pt;
	}

	public RandomCoordinateGenerator(int count, Envelope e, double tol) {
		env = e;
		maxlen = (env.getWidth() + env.getHeight()) / 2 * 0.1;
		points = new Vector<Point>();
		points.ensureCapacity(count);
		tolerance = tol;
		maxcount = count;
	}

	Point GetRandomCoord() {
		double f = random.nextDouble();
		Point pt;
		if (f > 0.9)
			pt = _RandomizeExisting();
		else
			pt = _GenerateNewPoint();

		return pt;
	}

	Point _snapClip(Point pt, Envelope env) {
		double x = pt.getX();
		if (x < env.getXMin())
			x = env.getXMin();
		if (x > env.getXMax())
			x = env.getXMax();
		double y = pt.getY();
		if (y < env.getYMin())
			y = env.getYMin();
		if (y > env.getYMax())
			y = env.getYMax();
		return new Point(x, y);
	}

	// void CompareGeometryContent(MultiVertexGeometry geom, Point buf[], int
	// sz) {
	// Assert.assertTrue(!geom.isEmpty());
	// Assert.assertTrue(geom.getPointCount() == sz);
	// // Go through the geometry points
	// for (int i = 0; i < geom.getPointCount(); i++) {
	// Point point = new Point(); // not a right pattern the point has to
	// // be created outside of the loop.
	// geom.getPointByVal(i, point);
	// Assert.assertTrue(point.getX() == buf[i].getX());
	// Assert.assertTrue(point.getY() == buf[i].getY());
	// Assert.assertTrue(point.getX() == buf[i].getX());
	// Assert.assertTrue(point.getY() == buf[i].getY());
	// }
	// if (geom.getType() == Geometry.Type.Polygon
	// || geom.getType() == Geometry.Type.Polyline) {
	// CompareGeometryContent((MultiPath) geom, buf, sz);
	// }
	// }
	//
	// void CompareGeometryContent(MultiPath geom, Point buf[], int sz) {
	// // Go through the geometry parts
	// int j = 0;
	// for (int ipart = 0; ipart < geom.getPathCount(); ipart++) {
	// int start = geom.getPathStart(ipart);
	// for (int i = 0; i < geom.getPathSize(ipart); i++, j++) {
	// Point point = geom.getPoint(start + i);
	// Assert.assertTrue(point.getX() == buf[j].getX());
	// Assert.assertTrue(point.getY() == buf[j].getY());
	//
	// }
	// }
	// }

	// void CompareGeometryContent(MultiVertexGeometry geom, Point3D buf[], int
	// sz) {
	// Assert.assertTrue(!geom.isEmpty());
	// Assert.assertTrue(geom.getPointCount() == sz);
	// // Go through the geometry points
	// for (int i = 0; i < geom.getPointCount(); i++) {
	// Point point = new Point(); // not a right pattern the point has to
	// // be created outside of the loop.
	// geom.getPointByVal(i, point);
	// Assert.assertTrue(point.getX() == buf[i].x);
	// Assert.assertTrue(point.getY() == buf[i].y);
	// Assert.assertTrue(point.getZ() == buf[i].z);
	// Point3D pt = point.getXYZ();
	// Assert.assertTrue(pt.x == buf[i].x);
	// Assert.assertTrue(pt.y == buf[i].y);
	// Assert.assertTrue(pt.z == buf[i].z);
	// }
	//
	// {
	// MultiVertexGeometryImpl mpGeom = (MultiVertexGeometryImpl) geom
	// ._getImpl();
	// AttributeStreamOfDbl streamPos = (AttributeStreamOfDbl) mpGeom
	// .getAttributeStreamRef(VertexDescription.Semantics.POSITION);
	// AttributeStreamOfDbl streamZ = (AttributeStreamOfDbl) mpGeom
	// .getAttributeStreamRef(VertexDescription.Semantics.Z);
	// for (int i = 0; i < geom.getPointCount(); i++) {
	// double x = streamPos.read(2 * i);
	// double y = streamPos.read(2 * i + 1);
	// double z = streamZ.read(i);
	//
	// Assert.assertTrue(x == buf[i].x);
	// Assert.assertTrue(y == buf[i].y);
	// Assert.assertTrue(z == buf[i].z);
	// }
	// }
	//
	// if (geom.getType() == Geometry.Type.Polygon
	// || geom.getType() == Geometry.Type.Polyline) {
	// CompareGeometryContent((MultiPath) geom, buf, sz);
	// }
	// }

	// void CompareGeometryContent(MultiPath geom, Point3D buf[], int sz) {
	// Assert.assertTrue(!geom.isEmpty());
	// Assert.assertTrue(geom.getPointCount() == sz);
	//
	// // Go through the geometry parts
	// int j = 0;
	// for (int ipart = 0; ipart < geom.getPathCount(); ipart++) {
	// int start = geom.getPathStart(ipart);
	// for (int i = 0; i < geom.getPathSize(ipart); i++, j++) {
	// double x = geom.getAttributeAsDbl(
	// VertexDescription.Semantics.POSITION, i + start, 0);
	// double y = geom.getAttributeAsDbl(
	// VertexDescription.Semantics.POSITION, i + start, 1);
	// double z = geom.getAttributeAsDbl(
	// VertexDescription.Semantics.Z, i + start, 0);
	// Assert.assertTrue(x == buf[j].x);
	// Assert.assertTrue(y == buf[j].y);
	// Assert.assertTrue(z == buf[j].z);
	//
	// Point point = new Point(); // not a right pattern the point has
	// // to be created outside of the
	// // loop.
	// geom.getPointByVal(start + i, point);
	// Assert.assertTrue(point.getX() == buf[j].x);
	// Assert.assertTrue(point.getY() == buf[j].y);
	// Assert.assertTrue(point.getZ() == buf[j].z);
	// Point3D pt = point.getXYZ();
	// Assert.assertTrue(pt.x == buf[j].x);
	// Assert.assertTrue(pt.y == buf[j].y);
	// Assert.assertTrue(pt.z == buf[j].z);
	// }
	// }
	// }

	// void CompareGeometryContent(MultiVertexGeometry geom1,
	// MultiVertexGeometry geom2) {
	// // Geometry types
	// Assert.assertTrue(geom1.getType() == geom2.getType());
	//
	// // Envelopes
	// Envelope env1 = new Envelope();
	// geom1.queryEnvelope(env1);
	//
	// Envelope env2 = new Envelope();
	// geom2.queryEnvelope(env2);
	//
	// Assert.assertTrue(env1.getXMin() == env2.getXMin()
	// && env1.getXMax() == env2.getXMax()
	// && env1.getYMin() == env2.getYMin()
	// && env1.getYMax() == env2.getYMax());
	//
	// int type = geom1.getType();
	// if (type == Geometry.Type.Polyline || type == Geometry.Type.Polygon) {
	// // Part Count
	// int partCount1 = ((MultiPath) geom1).getPathCount();
	// int partCount2 = ((MultiPath) geom2).getPathCount();
	// Assert.assertTrue(partCount1 == partCount2);
	//
	// // Part indices
	// for (int i = 0; i < partCount1; i++) {
	// int start1 = ((MultiPath) geom1).getPathStart(i);
	// int start2 = ((MultiPath) geom2).getPathStart(i);
	// Assert.assertTrue(start1 == start2);
	// int end1 = ((MultiPath) geom1).getPathEnd(i);
	// int end2 = ((MultiPath) geom2).getPathEnd(i);
	// Assert.assertTrue(end1 == end2);
	// }
	// }
	//
	// // Point count
	// int pointCount1 = geom1.getPointCount();
	// int pointCount2 = geom2.getPointCount();
	// Assert.assertTrue(pointCount1 == pointCount2);
	//
	// if (type == Geometry.Type.MultiPoint || type == Geometry.Type.Polyline
	// || type == Geometry.Type.Polygon) {
	// MultiVertexGeometryImpl mpGeom1 = (MultiVertexGeometryImpl) geom1
	// ._getImpl();
	// MultiVertexGeometryImpl mpGeom2 = (MultiVertexGeometryImpl) geom2
	// ._getImpl();
	// // POSITION
	// AttributeStreamBase positionStream1 = mpGeom1
	// .getAttributeStreamRef(VertexDescription.Semantics.POSITION);
	// AttributeStreamOfDbl position1 = (AttributeStreamOfDbl) positionStream1;
	//
	// AttributeStreamBase positionStream2 = mpGeom2
	// .getAttributeStreamRef(VertexDescription.Semantics.POSITION);
	// AttributeStreamOfDbl position2 = (AttributeStreamOfDbl) positionStream2;
	//
	// for (int i = 0; i < pointCount1; i++) {
	// double x1 = position1.read(2 * i);
	// double x2 = position2.read(2 * i);
	// Assert.assertTrue(x1 == x2);
	//
	// double y1 = position1.read(2 * i + 1);
	// double y2 = position2.read(2 * i + 1);
	// Assert.assertTrue(y1 == y2);
	// }
	//
	// // Zs
	// boolean bHasZs1 = mpGeom1
	// .hasAttribute(VertexDescription.Semantics.Z);
	// boolean bHasZs2 = mpGeom2
	// .hasAttribute(VertexDescription.Semantics.Z);
	// Assert.assertTrue(bHasZs1 == bHasZs2);
	//
	// if (bHasZs1) {
	// AttributeStreamBase zStream1 = mpGeom1
	// .getAttributeStreamRef(VertexDescription.Semantics.Z);
	// AttributeStreamOfDbl zs1 = (AttributeStreamOfDbl) zStream1;
	//
	// AttributeStreamBase zStream2 = mpGeom2
	// .getAttributeStreamRef(VertexDescription.Semantics.Z);
	// AttributeStreamOfDbl zs2 = (AttributeStreamOfDbl) zStream2;
	//
	// for (int i = 0; i < pointCount1; i++) {
	// double z1 = zs1.read(i);
	// double z2 = zs2.read(i);
	// Assert.assertTrue(z1 == z2);
	// }
	// }
	//
	// // Ms
	// boolean bHasMs1 = mpGeom1
	// .hasAttribute(VertexDescription.Semantics.M);
	// boolean bHasMs2 = mpGeom2
	// .hasAttribute(VertexDescription.Semantics.M);
	// Assert.assertTrue(bHasMs1 == bHasMs2);
	//
	// if (bHasMs1) {
	// AttributeStreamBase mStream1 = mpGeom1
	// .getAttributeStreamRef(VertexDescription.Semantics.M);
	// AttributeStreamOfDbl ms1 = (AttributeStreamOfDbl) mStream1;
	//
	// AttributeStreamBase mStream2 = mpGeom2
	// .getAttributeStreamRef(VertexDescription.Semantics.M);
	// AttributeStreamOfDbl ms2 = (AttributeStreamOfDbl) mStream2;
	//
	// for (int i = 0; i < pointCount1; i++) {
	// double m1 = ms1.read(i);
	// double m2 = ms2.read(i);
	// Assert.assertTrue(m1 == m2);
	// }
	// }
	//
	// // IDs
	// boolean bHasIDs1 = mpGeom1
	// .hasAttribute(VertexDescription.Semantics.ID);
	// boolean bHasIDs2 = mpGeom2
	// .hasAttribute(VertexDescription.Semantics.ID);
	// Assert.assertTrue(bHasIDs1 == bHasIDs2);
	//
	// if (bHasIDs1) {
	// AttributeStreamBase idStream1 = mpGeom1
	// .getAttributeStreamRef(VertexDescription.Semantics.ID);
	// AttributeStreamOfInt32 ids1 = (AttributeStreamOfInt32) idStream1;
	//
	// AttributeStreamBase idStream2 = mpGeom2
	// .getAttributeStreamRef(VertexDescription.Semantics.ID);
	// AttributeStreamOfInt32 ids2 = (AttributeStreamOfInt32) idStream2;
	//
	// for (int i = 0; i < pointCount1; i++) {
	// int id1 = ids1.read(i);
	// int id2 = ids2.read(i);
	// Assert.assertTrue(id1 == id2);
	// }
	// }
	// }
	// }
	//
	// void SimpleTest(Geometry point) {
	// Assert.assertTrue(point != null);
	// point.addAttribute(VertexDescription.Semantics.Z);
	// Assert.assertTrue(point
	// .hasAttribute(VertexDescription.Semantics.POSITION));
	// Assert.assertTrue(point.hasAttribute(VertexDescription.Semantics.Z));
	// point.addAttribute(VertexDescription.Semantics.Z);// duplicate call
	// Assert.assertTrue(point.getDescription().getAttributeCount() == 2);
	// Assert
	// .assertTrue(point.getDescription().getSemantics(1) ==
	// VertexDescription.Semantics.Z);
	// point.dropAttribute(VertexDescription.Semantics.Z);
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.Z));
	// point.dropAttribute(VertexDescription.Semantics.Z);// duplicate call
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.Z));
	// Assert.assertTrue(point.getDescription().getAttributeCount() == 1);
	// Assert
	// .assertTrue(point.getDescription().getSemantics(0) ==
	// VertexDescription.Semantics.POSITION);
	//
	// point.addAttribute(VertexDescription.Semantics.M);
	// Assert.assertTrue(point
	// .hasAttribute(VertexDescription.Semantics.POSITION));
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.Z));
	// Assert.assertTrue(point.hasAttribute(VertexDescription.Semantics.M));
	// point.dropAttribute(VertexDescription.Semantics.M);
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.M));
	//
	// point.addAttribute(VertexDescription.Semantics.ID);
	// Assert.assertTrue(point
	// .hasAttribute(VertexDescription.Semantics.POSITION));
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.Z));
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.M));
	// point.dropAttribute(VertexDescription.Semantics.ID);
	// Assert.assertTrue(!point.hasAttribute(VertexDescription.Semantics.ID));
	//
	// // TEST_ASSERT(point->IsEmpty());
	// // TEST_ASSERT(point->GetPointCount() == 0);
	// // TEST_ASSERT(point->GetPartCount() == 0);
	//
	// point = null;
	// Assert.assertTrue(point == null);
	// }

}
