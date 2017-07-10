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

import junit.framework.TestCase;
import org.junit.Test;

public class TestMultiPoint extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	static void simpleTest(Geometry point) {
		assertTrue(point != null);
		// point->AddAttribute(VertexDescription::Semantics::Z);
		// assertTrue(point->HasAttribute(VertexDescription::Semantics::POSITION));
		// assertTrue(point.->HasAttribute(VertexDescription::Semantics::Z));
		// point->AddAttribute(VertexDescription::Semantics::Z);//duplicate call
		// assertTrue(point->GetDescription()->GetAttributeCount() == 2);
		// assertTrue(point->GetDescription()->GetSemantics(1) ==
		// VertexDescription::Semantics::Z);
		// point->DropAttribute(VertexDescription::Semantics::Z);
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::Z));
		// point->DropAttribute(VertexDescription::Semantics::Z);//duplicate
		// call
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::Z));
		// assertTrue(point->GetDescription()->GetAttributeCount() == 1);
		// assertTrue(point->GetDescription()->GetSemantics(0) ==
		// VertexDescription::Semantics::POSITION);

		// point->AddAttribute(VertexDescription::Semantics::M);
		// assertTrue(point->HasAttribute(VertexDescription::Semantics::POSITION));
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::Z));
		// assertTrue(point->HasAttribute(VertexDescription::Semantics::M));
		// point->DropAttribute(VertexDescription::Semantics::M);
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::M));
		//
		// point->AddAttribute(VertexDescription::Semantics::ID);
		// assertTrue(point->HasAttribute(VertexDescription::Semantics::POSITION));
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::Z));
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::M));
		// point->DropAttribute(VertexDescription::Semantics::ID);
		// assertFalse(point->HasAttribute(VertexDescription::Semantics::ID));
		// /
		// assertTrue(point->IsEmpty());
		// assertTrue(point->GetPointCount() == 0);
		// assertTrue(point->GetPartCount() == 0);

		point = null;
		assertFalse(point != null);
	}

	@Test
	public static void testCreation() {
		{// simple create
			MultiPoint mpoint = new MultiPoint();
			assertTrue(mpoint.getClass() == MultiPoint.class);
			// assertFalse(mpoint.getClass() == Polyline.class);

			assertTrue(mpoint != null);
			assertTrue(mpoint.getType() == Geometry.Type.MultiPoint);
			assertTrue(mpoint.isEmpty());
			assertTrue(mpoint.getPointCount() == 0);
			mpoint = null;
			assertFalse(mpoint != null);
		}
		{// play with default attributes
			MultiPoint mpoint = new MultiPoint();
			simpleTest(mpoint);
		}

		{// simple create 2D
			MultiPoint mpoint = new MultiPoint();
			assertTrue(mpoint != null);


			MultiPoint mpoint1 = new MultiPoint();
			assertTrue(mpoint1 != null);

			mpoint.setEmpty();
			Point pt = new Point(0, 0);
			mpoint.add(pt);
			Point pt3 = mpoint.getPoint(0);
			assertTrue(pt3.getX() == 0 && pt3.getY() == 0/** && pt3.getZ() == 0 */
			);
			// assertFalse(mpoint->HasAttribute(VertexDescription::Semantics::Z));
			// pt3.setZ(115.0);
			mpoint.setPoint(0, pt3);
			pt3 = mpoint.getPoint(0);
			assertTrue(pt3.getX() == 0 && pt3.getY() == 0/* && pt3.getZ() == 115 */);
			// assertTrue(mpoint->HasAttribute(VertexDescription::Semantics::Z));
			// CompareGeometryContent(mpoint, &pt, 1);
		}

		{// move 3d
			MultiPoint mpoint = new MultiPoint();
			assertTrue(mpoint != null);
			Point pt = new Point(0, 0);
			mpoint.add(pt);
			Point pt3 = mpoint.getPoint(0);
			assertTrue(pt3.getX() == 0 && pt3.getY() == 0/* && pt3.getZ() == 0 */);
		}

		{ // test QueryInterval
			MultiPoint mpoint = new MultiPoint();

			Point pt1 = new Point(0.0, 0.0);
			// pt1.setZ(-1.0);

			Point pt2 = new Point(0.0, 0.0);
			// pt2.setZ(1.0);

			mpoint.add(pt1);
			mpoint.add(pt2);

			// Envelope1D e =
			// mpoint->QueryInterval(enum_value2(VertexDescription, Semantics,
			// Z), 0);
			Envelope e = new Envelope();
			mpoint.queryEnvelope(e);
			// assertTrue(e.get == -1.0 && e.vmax == 1.0);
		}

		{
			@SuppressWarnings("unused")
			MultiPoint geom = new MultiPoint();
			// int sz = sizeof(openString) / sizeof(openString[0]);
			// for (int i = 0; i < sz; i++)
			// geom.add(openString[i]);
			// CompareGeometryContent(geom, openString, sz);
		}

		{
			@SuppressWarnings("unused")
			MultiPoint geom = new MultiPoint();
			// int sz = sizeof(openString) / sizeof(openString[0]);
			// Point point = GCNEW Point;
			// for (int i = 0; i < sz; i++)
			// {
			// point.setXY(openString[i]);
			// geom.add(point);
			// }
			// CompareGeometryContent(geom, openString, sz);
		}

		// Test AddPoints
		{
			@SuppressWarnings("unused")
			MultiPoint geom = new MultiPoint();
			// int sz = sizeof(openString) / sizeof(openString[0]);
			// geom.addPoints(openString, sz, 0, -1);
			// CompareGeometryContent((MultiVertexGeometry)geom, openString,
			// sz);
		}

		// Test InsertPoint(Point2D)
		{
			MultiPoint mpoint = new MultiPoint();
			Point pt0 = new Point(0.0, 0.0);
			// pt0.setZ(-1.0);
			// pt0.setID(7);

			Point pt1 = new Point(0.0, 0.0);
			// pt1.setZ(1.0);
			// pt1.setID(11);

			Point pt2 = new Point(0.0, 1.0);
			// pt2.setZ(1.0);
			// pt2.setID(13);

			mpoint.add(pt0);
			mpoint.add(pt1);
			mpoint.add(pt2);

			Point pt3 = new Point(-11.0, -13.0);

			mpoint.add(pt3);
			mpoint.insertPoint(1, pt3);
			assertTrue(mpoint.getPointCount() == 5);

			Point pt;
			pt = mpoint.getPoint(0);
			assertTrue(pt.getX() == pt0.getX() && pt.getY() == pt0.getY()/*
																		 * &&
																		 * pt.
																		 * getZ
																		 * () ==
																		 * pt0
																		 * .getZ
																		 * ()
																		 */);

			pt = mpoint.getPoint(1);
			assertTrue(pt.getX() == pt3.getX() && pt.getY() == pt3.getY());

			pt = mpoint.getPoint(2);
			assertTrue(pt.getX() == pt1.getX() && pt.getY() == pt1.getY()/*
																		 * &&
																		 * pt.
																		 * getZ
																		 * () ==
																		 * pt1
																		 * .getZ
																		 * ()
																		 */);

			pt = mpoint.getPoint(3);
			assertTrue(pt.getX() == pt2.getX() && pt.getY() == pt2.getY()/*
																		 * &&
																		 * pt.
																		 * getZ
																		 * () ==
																		 * pt2
																		 * .getZ
																		 * ()
																		 */);

			Point point = new Point();
			point.setXY(17.0, 19.0);
			// point.setID(12);
			// point.setM(5);

			mpoint.insertPoint(2, point);
			mpoint.add(point);

			assertTrue(mpoint.getPointCount() == 7);

			// double m;
			// int id;
			// pt = mpoint.getXYZ(2);
			// assertTrue(pt.x == 17.0 && pt.y == 19.0 && pt.z == defaultZ);
			// m = mpoint.getAttributeAsDbl(enum_value2(VertexDescription,
			// Semantics, M), 2, 0);
			// assertTrue(m == 5);
			// id = mpoint.getAttributeAsInt(enum_value2(VertexDescription,
			// Semantics, ID), 2, 0);
			// assertTrue(id == 23);
			//
			// pt = mpoint.getXYZ(3);
			// assertTrue(pt.x == pt1.x && pt.y == pt1.y && pt.z == pt1.z);
			// m = mpoint.getAttributeAsDbl(enum_value2(VertexDescription,
			// Semantics, M), 3, 0);
			// assertTrue(NumberUtils::IsNaN(m));
			// id = mpoint.getAttributeAsInt(enum_value2(VertexDescription,
			// Semantics, ID), 3, 0);
			// assertTrue(id == 11);
		}

		MultiPoint mpoint = new MultiPoint();
		Point pt0 = new Point(0.0, 0.0, -1.0);

		Point pt1 = new Point(0.0, 0.0, 1.0);

		Point pt2 = new Point(0.0, 1.0, 1.0);

		mpoint.add(pt0);
		mpoint.add(pt1);
		mpoint.add(pt2);

		mpoint.removePoint(1);

		Point pt;
		pt = mpoint.getPoint(0);
		assertTrue(pt.getX() == pt0.getX() && pt.getY() == pt0.getY());
		pt = mpoint.getPoint(1);
		assertTrue(pt.getX() == pt2.getX() && pt.getY() == pt2.getY());

		assertTrue(mpoint.getPointCount() == 2);
	}

	@Test
	public static void testCopy() {
		MultiPoint mpoint = new MultiPoint();
		Point pt0 = new Point(0.0, 0.0, -1.0);
		Point pt1 = new Point(0.0, 0.0, 1.0);
		Point pt2 = new Point(0.0, 1.0, 1.0);

		mpoint.add(pt0);
		mpoint.add(pt1);
		mpoint.add(pt2);
		mpoint.removePoint(1);

		MultiPoint mpCopy = (MultiPoint) mpoint.copy();
		assertTrue(mpCopy.equals(mpoint));

		Point pt;
		pt = mpCopy.getPoint(0);
		assertTrue(pt.getX() == pt0.getX() && pt.getY() == pt0.getY());
		pt = mpCopy.getPoint(1);
		assertTrue(pt.getX() == pt2.getX() && pt.getY() == pt2.getY());

		assertTrue(mpCopy.getPointCount() == 2);
	}
}
