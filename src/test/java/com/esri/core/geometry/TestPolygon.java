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

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

import com.esri.core.geometry.ogc.OGCGeometry;

public class TestPolygon extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testCreation() {
		// simple create

		Polygon poly = new Polygon();
		@SuppressWarnings("unused")
		int number = poly.getStateFlag();

		assertTrue(poly != null);
		// assertTrue(poly.getClass() == Polygon.class);
		// assertFalse(poly.getClass() == Envelope.class);

		assertTrue(poly.getType() == Geometry.Type.Polygon);
		assertTrue(poly.isEmpty());
		assertTrue(poly.getPointCount() == 0);
		assertTrue(poly.getPathCount() == 0);
		number = poly.getStateFlag();
		poly = null;
		assertFalse(poly != null);

		// play with default attributes
		@SuppressWarnings("unused")
		Polygon poly2 = new Polygon();
		// SimpleTest(poly2);

		// creation1();
		// creation2();
		// addpath();
		// addpath2();
		// removepath();
		// reversepath();
		// reverseallpaths();
		// openallpaths();
		// openpath();
		// insertpath();
		// insertpoints();
		// insertpoint();
		// removepoint();
		// insertpointsfromaray();
		// createWithStreams();
		// testBug1();
	}

	@Test
	public void testCreation1() {
		// Simple area and length calcul test
		Polygon poly = new Polygon();
		@SuppressWarnings("unused")
		int number = poly.getStateFlag();
		Envelope env = new Envelope(1000, 2000, 1010, 2010);
		env.toString();
		poly.addEnvelope(env, false);
		poly.toString();
		number = poly.getStateFlag();
		assertTrue(Math.abs(poly.calculateArea2D() - 100) < 1e-12);
		assertTrue(Math.abs(poly.calculateLength2D() - 40) < 1e-12);
		poly.setEmpty();
		number = poly.getStateFlag();
		poly.addEnvelope(env, true);
		number = poly.getStateFlag();
		assertTrue(Math.abs(poly.calculateArea2D() + 100) < 1e-12);
		number = poly.getStateFlag();
	}

	@Test
	public void testCreation2() {
		Polygon poly = new Polygon();
		int state1 = poly.getStateFlag();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);
		poly.closePathWithLine();
		int state2 = poly.getStateFlag();
		assertTrue(state2 == state1 + 1);

		// MultiPathImpl::Pointer mpImpl =
		// (MultiPathImpl::Pointer)poly->_GetImpl();
		//
		// assertTrue(mpImpl.getPointCount() == 4);
		// assertTrue(mpImpl.getPathCount() == 1);
		// AttributeStreamBase xy =
		// mpImpl.getAttributeStreamRef(enum_value2(VertexDescription,
		// Semantics, POSITION));
		// double x, y;
		// x = xy.readAsDbl(2 * 2);
		// y = xy.readAsDbl(2 * 2 + 1);
		// assertTrue(x == 30); assertTrue(y == 14);
		//
		// AttributeStreamOfIndexType parts = mpImpl.getPathStreamRef();
		// assertTrue(parts.size() == 2);
		// assertTrue(parts.read(0) == 0);
		// assertTrue(parts.read(1) == 4);
		// assertTrue(mpImpl.isClosedPath(0));
		// assertTrue(mpImpl.getSegmentFlagsStreamRef() == NULLPTR);
		// assertTrue(mpImpl.getSegmentIndexStreamRef() == NULLPTR);
		// assertTrue(mpImpl.getSegmentDataStreamRef() == NULLPTR);

		poly.startPath(20, 13);
		poly.lineTo(150, 120);
		poly.lineTo(300, 414);
		poly.lineTo(610, 14);
		poly.lineTo(6210, 140);
		poly.closePathWithLine();

		// assertTrue(mpImpl.getPointCount() == 9);
		// assertTrue(mpImpl.getPathCount() == 2);
		// assertTrue(mpImpl.isClosedPath(1));
		// xy = mpImpl.getAttributeStreamRef(enum_value2(VertexDescription,
		// Semantics, POSITION));
		// x = xy.readAsDbl(2 * 3);
		// y = xy.readAsDbl(2 * 3 + 1);
		// assertTrue(x == 60); assertTrue(y == 144);
		//
		// x = xy.readAsDbl(2 * 6);
		// y = xy.readAsDbl(2 * 6 + 1);
		// assertTrue(x == 300); assertTrue(y == 414);

		// parts = mpImpl.getPathStreamRef();
		// assertTrue(parts.size() == 3);
		// assertTrue(parts.read(0) == 0);
		// assertTrue(parts.read(1) == 4);
		// assertTrue(parts.read(2) == 9);
		// assertTrue(mpImpl.getSegmentIndexStreamRef() == NULLPTR);
		// assertTrue(mpImpl.getSegmentFlagsStreamRef() == NULLPTR);
		// assertTrue(mpImpl.getSegmentDataStreamRef() == NULLPTR);

		poly.startPath(200, 1333);
		poly.lineTo(1150, 1120);
		poly.lineTo(300, 4114);
		poly.lineTo(6110, 114);
		poly.lineTo(61210, 1140);

		assertTrue(poly.isClosedPath(2) == true);
		poly.closeAllPaths();
		assertTrue(poly.isClosedPath(2) == true);

		{
			Polygon poly2 = new Polygon();
			poly2.startPath(10, 10);
			poly2.lineTo(100, 10);
			poly2.lineTo(100, 100);
			poly2.lineTo(10, 100);
		}

		{
			Polygon poly3 = new Polygon();
			// create a star (non-simple)
			poly3.startPath(1, 0);
			poly3.lineTo(5, 10);
			poly3.lineTo(9, 0);
			poly3.lineTo(0, 6);
			poly3.lineTo(10, 6);
		}
	}

	@Test
	public void testCreateWithStreams() {
		// Polygon poly = new Polygon();
		// poly.addAttribute((int)Semantics.M);
		// try
		// {
		// OutputDebugString(L"Test an assert\n");
		// GeometryException::m_assertOnException = false;
		// ((MultiPathImpl::Pointer)poly->_GetImpl()).getPathStreamRef();
		// }
		// catch(GeometryException except)
		// {
		// assertTrue(except->empty_geometry);
		// GeometryException::m_assertOnException = true;
		// }
		// try
		// {
		// OutputDebugString(L"Test an assert\n");
		// GeometryException::m_assertOnException = false;
		// ((MultiPathImpl::Pointer)poly->_GetImpl()).getAttributeStreamRef(enum_value2(VertexDescription,
		// Semantics, POSITION));
		// }
		// catch(GeometryException except)
		// {
		// assertTrue(except->empty_geometry);
		// GeometryException::m_assertOnException = true;
		// }
		//
		// MultiPathImpl::Pointer mpImpl =
		// (MultiPathImpl::Pointer)poly->_GetImpl();
		//
		// AttributeStreamOfIndexType parts =
		// (AttributeStreamOfIndexType)AttributeStreamBase::CreateIndexStream(3);
		// mpImpl.setPathStreamRef(parts);
		//
		// parts.write(0, 0); //first element is always 0
		// parts.write(1, 4); //second element is the index of the first vertex
		// of the second part
		// parts.write(2, 8); //the third element is the total point count.
		//
		// AttributeStreamOfInt8 flags =
		// (AttributeStreamOfInt8)AttributeStreamBase::CreateByteStream(3);
		// mpImpl.setPathFlagsStreamRef(flags);
		// flags.write(0, enum_value1(PathFlags, enumClosed));
		// flags.write(1, enum_value1(PathFlags, enumClosed));
		// flags.write(2, 0);
		//
		// AttributeStreamOfDbl xy =
		// (AttributeStreamOfDbl)AttributeStreamBase::CreateDoubleStream(16);
		// //16 doubles means 8 points
		// mpImpl.setAttributeStreamRef(enum_value2(VertexDescription,
		// Semantics, POSITION), xy);
		//
		// Envelope2D env;
		// env.SetCoords(-1000, -2000, 1000, 2000);
		// Point2D buf[4];
		// env.QueryCorners(buf);
		// xy.writeRange(0, 8, (double*)buf, 0, true);
		//
		// env.SetCoords(-100, -200, 100, 200);
		// env.QueryCornersReversed(buf); //make a hole by quering reversed
		// order
		// xy.writeRange(8, 8, (double*)buf, 0, true);
		//
		// mpImpl.notifyModified(MultiVertexGeometryImpl::DirtyAll); //notify
		// the path that the vertices had changed.
		//
		// assertTrue(poly.getPointCount() == 8);
		// assertTrue(poly.getPathCount() == 2);
		// assertTrue(poly.getPathSize(1) == 4);
		// assertTrue(poly.isClosedPath(0));
		// assertTrue(poly.isClosedPath(1));
	}

	@Test
	public void testCloneStuff() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);
		poly.closePathWithLine();

		Polygon clone = (Polygon) poly.copy();
		assertTrue(clone.getPathCount() == 3);
		assertTrue(clone.getPathStart(2) == 8);
		assertTrue(clone.isClosedPath(0));
		assertTrue(clone.isClosedPath(1));
		assertTrue(clone.isClosedPath(2));
		assertTrue(clone.getXY(5).isEqual(new Point2D(15, 20)));
	}

	@Test
	public void testCloneStuffEnvelope() {
		Envelope env = new Envelope(11, 12, 15, 24);
		Envelope eCopy = (Envelope) env.copy();
		assertTrue(eCopy.equals(env));
		assertTrue(eCopy.getXMin() == 11);
		assertTrue(eCopy.getYMin() == 12);
		assertTrue(eCopy.getXMax() == 15);
		assertTrue(eCopy.getYMax() == 24);
	}

	@Test
	public void testCloneStuffPolyline() {
		Polyline poly = new Polyline();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);
		poly.closePathWithLine();

		Polyline clone = (Polyline) poly.copy();
		assertTrue(clone.getPathCount() == 3);
		assertTrue(clone.getPathStart(2) == 8);
		assertTrue(!clone.isClosedPath(0));
		assertTrue(!clone.isClosedPath(1));
		assertTrue(clone.isClosedPath(2));
		assertTrue(clone.getXY(5).isEqual(new Point2D(15, 20)));
	}

	@Test
	public void testAddpath() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);

		Polygon poly1 = new Polygon();
		poly1.addPath(poly, 2, true);
		poly1.addPath(poly, 0, true);

		assertTrue(poly1.getPathCount() == 2);
		assertTrue(poly1.getPathStart(1) == 4);
		assertTrue(poly1.isClosedPath(0));
		assertTrue(poly1.isClosedPath(1));
		Point ptOut = poly1.getPoint(6);
		assertTrue(ptOut.getX() == 30 && ptOut.getY() == 14);
	}

	@Test
	public void testAddpath2() {
		Polygon polygon = new Polygon();
		polygon.startPath(-179, 34);
		polygon.lineTo(-154, 34);
		polygon.lineTo(-179, 36);
		polygon.lineTo(-180, 90);
		polygon.lineTo(180, 90);
		polygon.lineTo(180, 36);
		polygon.lineTo(70, 46);
		polygon.lineTo(-76, 80);
		polygon.lineTo(12, 38);
		polygon.lineTo(-69, 51);
		polygon.lineTo(-95, 29);
		polygon.lineTo(-105, 7);
		polygon.lineTo(-112, -27);
		polygon.lineTo(-149, -11);
		polygon.lineTo(-149, -11);
		polygon.lineTo(-166, -4);
		polygon.lineTo(-179, 5);

		Polyline polyline = new Polyline();
		polyline.startPath(180, 5);
		polyline.lineTo(140, 34);
		polyline.lineTo(180, 34);

		polygon.addPath(polyline, 0, true);

		Point startpoint = polygon.getPoint(17);
		assertTrue(startpoint.getX() == 180 && startpoint.getY() == 5);
	}

	@Test
	public void testRemovepath() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);

		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 0,
		// 0, 2);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 1,
		// 0, 3);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 2,
		// 0, 5);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 3,
		// 0, 7);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 4,
		// 0, 11);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 5,
		// 0, 13);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 6,
		// 0, 17);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 7,
		// 0, 19);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 8,
		// 0, 23);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 9,
		// 0, 29);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 10,
		// 0, 31);
		// poly->SetAttribute(enum_value2(VertexDescription, Semantics, Z), 11,
		// 0, 37);

		poly.removePath(1);

		assertTrue(poly.getPathCount() == 2);
		assertTrue(poly.getPathStart(1) == 4);
		assertTrue(poly.isClosedPath(0));
		assertTrue(poly.isClosedPath(1));
		Point ptOut = poly.getPoint(4);
		assertTrue(ptOut.getX() == 10 && ptOut.getY() == 1);
		poly.removePath(0);
		poly.removePath(0);
		assertTrue(poly.getPathCount() == 0);

		Polygon poly2 = new Polygon();
		poly2.startPath(0, 0);
		poly2.lineTo(0, 10);
		poly2.lineTo(10, 10);
		poly2.startPath(1, 1);
		poly2.lineTo(2, 2);
		poly2.removePath(0);
		// poly2->StartPath(0, 0);
		poly2.lineTo(0, 10);
		poly2.lineTo(10, 10);

		// Polygon polygon2 = new Polygon();
		// polygon2.addPath(poly, -1, true);
		// polygon2.addPath(poly, -1, true);
		// polygon2.addPath(poly, -1, true);
		// assertTrue(polygon2.getPathCount() == 3);
		// polygon2.removePath(0);
		// polygon2.removePath(0);
		// polygon2.removePath(0);
		// assertTrue(polygon2.getPathCount() == 0);
		// polygon2.addPath(poly, -1, true);

		// Point point1 = new Point();
		// Point point2 = new Point();
		// point1.setX(0);
		// point1.setY(0);
		// point2.setX(0);
		// point2.setY(0);
		// polygon2.addPath(poly2, 0, true);
		// polygon2.removePath(0);
		// polygon2.insertPoint(0, 0, point1);
		// polygon2.insertPoint(0, 0, point2);
		// assertTrue(polygon2.getPathCount() == 1);
		// assertTrue(polygon2.getPointCount() == 2);

		Polygon polygon3 = new Polygon();
		polygon3.startPath(0, 0);
		polygon3.lineTo(0, 10);
		polygon3.lineTo(10, 10);
		double area = polygon3.calculateArea2D();
		polygon3.removePath(0);

		polygon3.startPath(0, 0);
		polygon3.lineTo(0, 10);
		polygon3.lineTo(10, 10);
		area = polygon3.calculateArea2D();
		assertTrue(area > 0.0);
	}

	@Test
	public void testReversepath() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);

		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 0, 0,
		// 2);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 1, 0,
		// 3);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 2, 0,
		// 5);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 3, 0,
		// 7);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 4, 0,
		// 11);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 5, 0,
		// 13);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 6, 0,
		// 17);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 7, 0,
		// 19);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 8, 0,
		// 23);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 9, 0,
		// 29);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 10,
		// 0, 31);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 11,
		// 0, 37);

		poly.reversePath(1);

		assertTrue(poly.getPathCount() == 3);
		assertTrue(poly.getPathStart(1) == 4);
		assertTrue(poly.isClosedPath(0));
		assertTrue(poly.isClosedPath(1));
		Point ptOut = poly.getPoint(4);
		assertTrue(ptOut.getX() == 10 && ptOut.getY() == 1);
	}

	@Test
	public void testReverseAllPaths() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);

		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 0, 0,
		// 2);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 1, 0,
		// 3);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 2, 0,
		// 5);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 3, 0,
		// 7);
		//
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 4, 0,
		// 11);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 5, 0,
		// 13);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 6, 0,
		// 17);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 7, 0,
		// 19);
		//
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 8, 0,
		// 23);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 9, 0,
		// 29);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 10,
		// 0, 31);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 11,
		// 0, 37);

		double area = poly.calculateArea2D();
		poly.reverseAllPaths();
		double areaReversed = poly.calculateArea2D();
		assertTrue(Math.abs(area + areaReversed) <= 0.001);
	}

	@Test
	public void testOpenAllPaths() {
		Polyline poly = new Polyline();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);
		poly.closePathWithLine();

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);
		poly.closePathWithLine();

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);
		poly.closePathWithLine();

		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 0, 0,
		// 2);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 1, 0,
		// 3);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 2, 0,
		// 5);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 3, 0,
		// 7);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 4, 0,
		// 11);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 5, 0,
		// 13);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 6, 0,
		// 17);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 7, 0,
		// 19);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 8, 0,
		// 23);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 9, 0,
		// 29);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 10,
		// 0, 31);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 11,
		// 0, 37);

		// MultiPathImpl::Pointer mpImpl =
		// (MultiPathImpl::Pointer)poly->_GetImpl();
		// poly.openAllPathsAndDuplicateStartVertex();

		// assertTrue(poly.getPathCount() == 3);
		// assertTrue(poly.getPathStart(0) == 0);
		// assertTrue(poly.getPathStart(1) == 5);
		// assertTrue(poly.getPathStart(2) == 10);
		// assertTrue(poly.getPointCount() == 15);
		// Point ptstart = poly.getPoint(0);
		// Point ptend = poly.getPoint(4);
		// assertTrue(ptstart.getX() == ptend.getX() && ptstart.getY() ==
		// ptend.getY());
		// ptstart = poly.getPoint(5);
		// ptend = poly.getPoint(9);
		// assertTrue(ptstart.getX() == ptend.getX() && ptstart.getY() ==
		// ptend.getY());
		// ptstart = poly.getPoint(10);
		// ptend = poly.getPoint(14);
		// assertTrue(ptstart.getX() == ptend.getX() && ptstart.getY() ==
		// ptend.getY());
	}

	@Test
	public void testOpenPath() {
		Polyline poly = new Polyline();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);
		poly.closePathWithLine();

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(60, 144);
		poly.closePathWithLine();

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);
		poly.closePathWithLine();

		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 0, 0,
		// 2);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 1, 0,
		// 3);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 2, 0,
		// 5);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 3, 0,
		// 7);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 4, 0,
		// 11);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 5, 0,
		// 13);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 6, 0,
		// 17);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 7, 0,
		// 19);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 8, 0,
		// 23);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 9, 0,
		// 29);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 10,
		// 0, 31);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 11,
		// 0, 37);
		//
		// MultiPathImpl::Pointer mpImpl =
		// (MultiPathImpl::Pointer)poly->_GetImpl();
		// poly.openPathAndDuplicateStartVertex(1);

		// assertTrue(poly.getPathCount() == 3);
		// assertTrue(poly.getPathStart(0) == 0);
		// assertTrue(poly.getPathStart(1) == 4);
		// assertTrue(poly.getPathStart(2) == 9);
		// assertTrue(poly.getPointCount() == 13);
		// Point ptstart = poly.getPoint(4);
		// Point ptend = poly.getPoint(8);
		// assertTrue(ptstart.getX() == ptend.getX() && ptstart.getY() ==
		// ptend.getY());
		// ptstart = poly.getPoint(9);
		// assertTrue(ptstart.getX() == 10 && ptstart.getY() == 1);
	}

	@Test
	public void testInsertPath() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(12, 2);
		poly.lineTo(16, 21);
		poly.lineTo(301, 15);
		poly.lineTo(61, 145);

		poly.startPath(13, 3);
		poly.lineTo(126, 22);
		poly.lineTo(31, 16);
		poly.lineTo(601, 146);

		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 0, 0,
		// 2);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 1, 0,
		// 3);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 2, 0,
		// 5);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 3, 0,
		// 7);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 4, 0,
		// 11);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 5, 0,
		// 13);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 6, 0,
		// 17);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 7, 0,
		// 19);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 8, 0,
		// 23);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 9, 0,
		// 29);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 10,
		// 0, 31);
		// poly.setAttribute(enum_value2(VertexDescription, Semantics, Z), 11,
		// 0, 37);

		Polygon poly2 = new Polygon();
		poly2.startPath(12, 2);
		poly2.lineTo(16, 21);
		poly2.lineTo(301, 15);
		poly2.lineTo(61, 145);

		poly.insertPath(0, poly2, 0, false);

		assertTrue(poly.getPathCount() == 4);
		assertTrue(poly.getPathStart(0) == 0);
		assertTrue(poly.getPathStart(1) == 4);
		assertTrue(poly.getPathStart(2) == 8);
		assertTrue(poly.getPathStart(3) == 12);
		assertTrue(poly.getPointCount() == 16);

		Point2D pt0 = poly.getXY(0);
		assertTrue(pt0.x == 12 && pt0.y == 2);
		Point2D pt1 = poly.getXY(1);
		assertTrue(pt1.x == 61 && pt1.y == 145);
		Point2D pt2 = poly.getXY(2);
		assertTrue(pt2.x == 301 && pt2.y == 15);
		Point2D pt3 = poly.getXY(3);
		assertTrue(pt3.x == 16 && pt3.y == 21);

		Point pt2d = new Point(-27, -27);

		poly.insertPoint(1, 0, pt2d);
		assertTrue(poly.getPathCount() == 4);
		assertTrue(poly.getPathStart(0) == 0);
		assertTrue(poly.getPathStart(1) == 4);
		assertTrue(poly.getPathStart(2) == 9);
		assertTrue(poly.getPathStart(3) == 13);
		assertTrue(poly.getPointCount() == 17);
	}

	@Test
	public void testInsertPoints() {
		{// forward insertion
			Polygon poly = new Polygon();
			poly.startPath(10, 1);
			poly.lineTo(15, 20);
			poly.lineTo(30, 14);
			poly.lineTo(60, 144);

			poly.startPath(10, 1);
			poly.lineTo(15, 20);
			poly.lineTo(300, 14);
			poly.lineTo(314, 217);
			poly.lineTo(60, 144);

			poly.startPath(10, 1);
			poly.lineTo(125, 20);
			poly.lineTo(30, 14);
			poly.lineTo(600, 144);

			Polygon poly1 = new Polygon();
			poly1.startPath(1, 17);
			poly1.lineTo(1, 207);
			poly1.lineTo(3, 147);
			poly1.lineTo(6, 1447);

			poly1.startPath(1000, 17);
			poly1.lineTo(1250, 207);
			poly1.lineTo(300, 147);
			poly1.lineTo(6000, 1447);

			poly1.insertPoints(1, 2, poly, 1, 1, 3, true);// forward

			assertTrue(poly1.getPathCount() == 2);
			assertTrue(poly1.getPathStart(1) == 4);
			assertTrue(poly1.isClosedPath(0));
			assertTrue(poly1.isClosedPath(1));
			assertTrue(poly1.getPointCount() == 11);
			assertTrue(poly1.getPathSize(1) == 7);
			// Point2D ptOut;
			// ptOut = poly1.getXY(5);
			// assertTrue(ptOut.x == 1250 && ptOut.y == 207);
			// ptOut = poly1.getXY(6);
			// assertTrue(ptOut.x == 15 && ptOut.y == 20);
			// ptOut = poly1.getXY(7);
			// assertTrue(ptOut.x == 300 && ptOut.y == 14);
			// ptOut = poly1.getXY(8);
			// assertTrue(ptOut.x == 314 && ptOut.y == 217);
			// ptOut = poly1.getXY(9);
			// assertTrue(ptOut.x == 300 && ptOut.y == 147);
			// ptOut = poly1.getXY(10);
			// assertTrue(ptOut.x == 6000 && ptOut.y == 1447);
		}

		{// reverse insertion
			Polygon poly = new Polygon();
			poly.startPath(10, 1);
			poly.lineTo(15, 20);
			poly.lineTo(30, 14);
			poly.lineTo(60, 144);

			poly.startPath(10, 1);
			poly.lineTo(15, 20);
			poly.lineTo(300, 14);
			poly.lineTo(314, 217);
			poly.lineTo(60, 144);

			poly.startPath(10, 1);
			poly.lineTo(125, 20);
			poly.lineTo(30, 14);
			poly.lineTo(600, 144);

			Polygon poly1 = new Polygon();
			poly1.startPath(1, 17);
			poly1.lineTo(1, 207);
			poly1.lineTo(3, 147);
			poly1.lineTo(6, 1447);

			poly1.startPath(1000, 17);
			poly1.lineTo(1250, 207);
			poly1.lineTo(300, 147);
			poly1.lineTo(6000, 1447);

			poly1.insertPoints(1, 2, poly, 1, 1, 3, false);// reverse

			assertTrue(poly1.getPathCount() == 2);
			assertTrue(poly1.getPathStart(1) == 4);
			assertTrue(poly1.isClosedPath(0));
			assertTrue(poly1.isClosedPath(1));
			assertTrue(poly1.getPointCount() == 11);
			assertTrue(poly1.getPathSize(1) == 7);
			// Point2D ptOut;
			// ptOut = poly1.getXY(5);
			// assertTrue(ptOut.x == 1250 && ptOut.y == 207);
			// ptOut = poly1.getXY(6);
			// assertTrue(ptOut.x == 314 && ptOut.y == 217);
			// ptOut = poly1.getXY(7);
			// assertTrue(ptOut.x == 300 && ptOut.y == 14);
			// ptOut = poly1.getXY(8);
			// assertTrue(ptOut.x == 15 && ptOut.y == 20);
			// ptOut = poly1.getXY(9);
			// assertTrue(ptOut.x == 300 && ptOut.y == 147);
			// ptOut = poly1.getXY(10);
			// assertTrue(ptOut.x == 6000 && ptOut.y == 1447);
		}
	}

	@Test
	public void testInsertPoint() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(314, 217);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);

		Point pt = new Point(-33, -34);
		poly.insertPoint(1, 1, pt);

		pt = poly.getPoint(4);
		assertTrue(pt.getX() == 10 && pt.getY() == 1);
		pt = poly.getPoint(5);
		assertTrue(pt.getX() == -33 && pt.getY() == -34);
		pt = poly.getPoint(6);
		assertTrue(pt.getX() == 15 && pt.getY() == 20);

		assertTrue(poly.getPointCount() == 14);
		assertTrue(poly.getPathSize(1) == 6);
		assertTrue(poly.getPathSize(2) == 4);
		assertTrue(poly.getPathCount() == 3);
	}

	@Test
	public void testRemovePoint() {
		Polygon poly = new Polygon();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(300, 14);
		poly.lineTo(314, 217);
		poly.lineTo(60, 144);

		poly.startPath(10, 1);
		poly.lineTo(125, 20);
		poly.lineTo(30, 14);
		poly.lineTo(600, 144);

		poly.removePoint(1, 1);

		Point pt;

		pt = poly.getPoint(4);
		assertTrue(pt.getX() == 10 && pt.getY() == 1);
		pt = poly.getPoint(5);
		assertTrue(pt.getX() == 300 && pt.getY() == 14);

		assertTrue(poly.getPointCount() == 12);
		assertTrue(poly.getPathSize(0) == 4);
		assertTrue(poly.getPathSize(1) == 4);
		assertTrue(poly.getPathSize(2) == 4);
	}

	@Test
	public static void testPolygonAreaAndLength() {
		Polygon poly;

		/* const */double r = 1.0;
		/* const */double epsilon = 1.0e-14;
		/* const */int nMax = 40;

		// If r == 1.0 and nMax == 40 and epsilon == 1.0e-14, it will pass.
		// But if r == 1.0 and nMax == 40 and epsilon == 1.0e-15, it will fail.

		for (int n = 3; n < nMax; n++) {
			// regular polygon with n vertices and length from center to vertex
			// = r
			poly = new Polygon();
			double theta = 0.0;
			poly.startPath(r, 0.0);
			for (int k = 1; k <= n; k++) {
				theta -= 2 * Math.PI / n;
				poly.lineTo(r * Math.cos(theta), r * Math.sin(theta));
			}
			double sinPiOverN = Math.sin(Math.PI / n);
			double sinTwoPiOverN = Math.sin(2.0 * Math.PI / n);
			double analyticalLength = 2.0 * n * r * sinPiOverN;
			double analyticalArea = 0.5 * n * r * r * sinTwoPiOverN;
			double calculatedLength = poly.calculateLength2D();
			double calculatedArea = poly.calculateArea2D();
			assertTrue(Math.abs(analyticalLength - calculatedLength) < epsilon);
			assertTrue(Math.abs(analyticalArea - calculatedArea) < epsilon);
		}
	}

	@Test
	public void testInsertPointsFromArray() {
		{// Test forward insertion of an array of Point2D
			// ArrayOf(Point2D) arr = new ArrayOf(Point2D)(5);
			// arr[0].SetCoords(10, 1);
			// arr[1].SetCoords(15, 20);
			// arr[2].SetCoords(300, 14);
			// arr[3].SetCoords(314, 217);
			// arr[4].SetCoords(60, 144);

			Polygon poly1 = new Polygon();
			poly1.startPath(1, 17);
			poly1.lineTo(1, 207);
			poly1.lineTo(3, 147);
			poly1.lineTo(6, 1447);

			poly1.startPath(1000, 17);
			poly1.lineTo(1250, 207);
			poly1.lineTo(300, 147);
			poly1.lineTo(6000, 1447);

			assertTrue(poly1.getPathCount() == 2);
			assertTrue(poly1.getPathStart(1) == 4);
			assertTrue(poly1.isClosedPath(0));
			assertTrue(poly1.isClosedPath(1));
		}

		{// Test reversed insertion of an array of Point2D
		}
	}

	@Test
	public void testCR177477() {
		Polygon pg = new Polygon();
		pg.startPath(-130, 40);
		pg.lineTo(-70, 40);
		pg.lineTo(-70, 10);
		pg.lineTo(-130, 10);

		Polygon pg2 = new Polygon();
		pg2.startPath(-60, 40);
		pg2.lineTo(-50, 40);
		pg2.lineTo(-50, 10);
		pg2.lineTo(-60, 10);

		pg.add(pg2, false);
	}

	@Test
	public void testCR177477getPathEnd() {
		Polygon pg = new Polygon();
		pg.startPath(-130, 40);
		pg.lineTo(-70, 40);
		pg.lineTo(-70, 10);
		pg.lineTo(-130, 10);

		pg.startPath(-60, 40);
		pg.lineTo(-50, 40);
		pg.lineTo(-50, 10);
		pg.lineTo(-60, 10);

		pg.startPath(-40, 40);
		pg.lineTo(-30, 40);
		pg.lineTo(-30, 10);
		pg.lineTo(-40, 10);

		int pathCount = pg.getPathCount();
		assertTrue(pathCount == 3);

		// int startIndex = pg.getPathStart(pathCount - 1);

		// int endIndex = pg.getPathEnd(pathCount - 1);

		Line line = new Line();
		line.toString();
		
		line.setStart(new Point(0, 0));
		line.setEnd(new Point(1, 0));
		
		line.toString();

		double geoLength = GeometryEngine.geodesicDistanceOnWGS84(new Point(0,
				0), new Point(1, 0));
		assertTrue(Math.abs(geoLength - 111319) < 1);
	}

	@Test
	public void testBug1() {
		Polygon pg = new Polygon();
		pg.startPath(-130, 40);
		for (int i = 0; i < 1000; i++)
			pg.lineTo(-70, 40);
		for (int i = 0; i < 999; i++)
			pg.removePoint(0, pg.getPointCount() - 1);

		pg.lineTo(-70, 40);
	}

	@Test
	public void testGeometryCopy() {
		boolean noException = true;

		Polyline polyline = new Polyline();

		Point p1 = new Point(-85.59285621496956, 38.26004727491098);
		Point p2 = new Point(-85.56417866635002, 38.28084064314639);
		Point p3 = new Point(-85.56845156650877, 38.24659881865461);
		Point p4 = new Point(-85.55341069949853, 38.26671513050464);

		polyline.startPath(p1);
		try {
			polyline.lineTo(p2);
			polyline.copy();
			polyline.lineTo(p3);
			polyline.copy();
			polyline.lineTo(p4); // exception thrown here!!!

		} catch (Exception e) {
			e.printStackTrace();
			noException = false;
		}

		assertTrue(noException);
	}// end of method
	
	@Test
	public void testBoundary() {
		Geometry g = OperatorImportFromWkt
				.local()
				.execute(
						0,
						Geometry.Type.Unknown,
						"POLYGON((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))",
						null);

		Geometry boundary = OperatorBoundary.local().execute(g, null);
		Polyline polyline = (Polyline) boundary;
		polyline.reverseAllPaths();
		String s = OperatorExportToWkt.local().execute(0, boundary, null);
		assertTrue(s
				.equals("MULTILINESTRING ((-10 -10, 10 -10, 10 10, -10 10, -10 -10), (-5 -5, -5 5, 5 5, 5 -5, -5 -5))"));
	}
	
	@Test
	public void testReplaceNaNs() {
		{
		MultiPoint mp = new MultiPoint();
		Point pt = new Point();
		pt.setXY(1, 2);
		pt.setZ(Double.NaN);
		mp.add(pt);
		pt = new Point();
		pt.setXY(11, 12);
		pt.setZ(3);
		mp.add(pt);
		
		mp.replaceNaNs(VertexDescription.Semantics.Z, 5);
		assertTrue(mp.getPoint(0).equals(new Point(1, 2, 5)));
		assertTrue(mp.getPoint(1).equals(new Point(11, 12, 3)));
		}

		{
		Polygon mp = new Polygon();
		Point pt = new Point();
		pt.setXY(1, 2);
		pt.setZ(Double.NaN);
		mp.startPath(pt);
		pt = new Point();
		pt.setXY(11, 12);
		pt.setZ(3);
		mp.lineTo(pt);
		
		mp.replaceNaNs(VertexDescription.Semantics.Z, 5);
		assertTrue(mp.getPoint(0).equals(new Point(1, 2, 5)));
		assertTrue(mp.getPoint(1).equals(new Point(11, 12, 3)));
		}
		
		{
		Polygon mp = new Polygon();
		Point pt = new Point();
		pt.setXY(1, 2);
		pt.setM(Double.NaN);
		mp.startPath(pt);
		pt = new Point();
		pt.setXY(11, 12);
		pt.setM(3);
		mp.lineTo(pt);
		
		mp.replaceNaNs(VertexDescription.Semantics.M, 5);
		Point p = new Point(1, 2); p.setM(5);
		boolean b = mp.getPoint(0).equals(p);
		assertTrue(b);
		p = new Point(11, 12); p.setM(3);
		b = mp.getPoint(1).equals(p);
		assertTrue(b);
		}
		
	}

	@Test
	public void testPolygon2PolygonFails() {
		OperatorFactoryLocal factory = OperatorFactoryLocal.getInstance();
		OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory
				.getOperator(Operator.Type.ExportToGeoJson);
		String result = exporter.execute(birmingham());

		OperatorImportFromGeoJson importer = (OperatorImportFromGeoJson) factory
				.getOperator(Operator.Type.ImportFromGeoJson);
		MapGeometry mapGeometry = importer.execute(
				GeoJsonImportFlags.geoJsonImportDefaults,
				Geometry.Type.Polygon, result, null);
		Polygon polygon = (Polygon) mapGeometry.getGeometry();
		assertEquals(birmingham(), polygon);
	}

	@Test
	public void testPolygon2PolygonFails2() {
		String birminghamGeojson = GeometryEngine
				.geometryToGeoJson(birmingham());
		MapGeometry returnedGeometry = GeometryEngine.geoJsonToGeometry(
				birminghamGeojson, GeoJsonImportFlags.geoJsonImportDefaults,
				Geometry.Type.Polygon);
		Polygon polygon = (Polygon) returnedGeometry.getGeometry();
		assertEquals(polygon, birmingham());
	}

	@Test
	public void testPolygon2PolygonWorks() {
		String birminghamGeojson = GeometryEngine
				.geometryToGeoJson(birmingham());
		MapGeometry returnedGeometry = GeometryEngine.geoJsonToGeometry(
				birminghamGeojson, GeoJsonImportFlags.geoJsonImportDefaults,
				Geometry.Type.Polygon);
		Polygon polygon = (Polygon) returnedGeometry.getGeometry();
		assertEquals(polygon.toString(), birmingham().toString());
	}

	@Test
	public void testPolygon2Polygon2Works() {
		String birminghamJson = GeometryEngine.geometryToJson(4326,
				birmingham());
		MapGeometry returnedGeometry = GeometryEngine
				.jsonToGeometry(birminghamJson);
		Polygon polygon = (Polygon) returnedGeometry.getGeometry();
		assertEquals(polygon, birmingham());
		String s = polygon.toString();
	}

	@Test
	public void testSegmentIteratorCrash() {
		Polygon poly = new Polygon();

		// clockwise => outer ring
		poly.startPath(0, 0);
		poly.lineTo(-0.5, 0.5);
		poly.lineTo(0.5, 1);
		poly.lineTo(1, 0.5);
		poly.lineTo(0.5, 0);

		// hole
		poly.startPath(0.5, 0.2);
		poly.lineTo(0.6, 0.5);
		poly.lineTo(0.2, 0.9);
		poly.lineTo(-0.2, 0.5);
		poly.lineTo(0.1, 0.2);
		poly.lineTo(0.2, 0.3);

		// island
		poly.startPath(0.1, 0.7);
		poly.lineTo(0.3, 0.7);
		poly.lineTo(0.3, 0.4);
		poly.lineTo(0.1, 0.4);

		assertEquals(poly.getSegmentCount(), 15);
		assertEquals(poly.getPathCount(), 3);
		SegmentIterator segmentIterator = poly.querySegmentIterator();
		int paths = 0;
		int segments = 0;
		while (segmentIterator.nextPath()) {
			paths++;
			Segment segment;
			while (segmentIterator.hasNextSegment()) {
				segment = segmentIterator.nextSegment();
				segments++;
			}
		}
		assertEquals(paths, 3);
		assertEquals(segments, 15);
	}

	private static Polygon birmingham() {
		Polygon poly = new Polygon();
		poly.addEnvelope(new Envelope(-1.954245, 52.513531, -1.837357,
				52.450123), false);
		poly.addEnvelope(new Envelope(0, 0, 1, 1), false);
		return poly;
	}
}
