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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.TestCase;
import org.junit.Test;

public class TestSerialization extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testSerializePoint() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Point pt = new Point(10, 40);
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Point ptRes = (Point) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			fail("Point serialization failure");

		}

		 //try
		 //{
		 //FileOutputStream streamOut = new FileOutputStream("c:/temp/savedPoint1.txt");
		 //ObjectOutputStream oo = new ObjectOutputStream(streamOut);
		 //Point pt = new Point(10, 40, 2);
		 //oo.writeObject(pt);
		 //}
		 //catch(Exception ex)
		 //{
		 //fail("Point serialization failure");
		 //}

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedPoint.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Point ptRes = (Point) ii.readObject();
			assertTrue(ptRes.getX() == 10 && ptRes.getY() == 40);
		} catch (Exception ex) {
			fail("Point serialization failure");
		}

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedPoint1.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Point ptRes = (Point) ii.readObject();
			assertTrue(ptRes.getX() == 10 && ptRes.getY() == 40 && ptRes.getZ() == 2);
		} catch (Exception ex) {
			fail("Point serialization failure");
		}

	}

	@Test
	public void testSerializePolygon() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Polygon pt = new Polygon();
			pt.startPath(10, 10);
			pt.lineTo(100, 100);
			pt.lineTo(200, 100);
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Polygon ptRes = (Polygon) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			fail("Polygon serialization failure");
		}

		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Polygon pt = new Polygon();
			pt.startPath(10, 10);
			pt.lineTo(100, 100);
			pt.lineTo(200, 100);
			pt = (Polygon) GeometryEngine.simplify(pt, null);
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Polygon ptRes = (Polygon) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			fail("Polygon serialization failure");
		}

		 //try
		 //{
		 //FileOutputStream streamOut = new FileOutputStream("c:/temp/savedPolygon1.txt");
		 //ObjectOutputStream oo = new ObjectOutputStream(streamOut);
		 //Polygon pt = new Polygon();
		 //pt.startPath(10, 10);
		 //pt.lineTo(100, 100);
		 //pt.lineTo(200, 100);
		 //pt = (Polygon)GeometryEngine.simplify(pt, null);
		 //oo.writeObject(pt);
		 //}
		 //catch(Exception ex)
		 //{
		 //fail("Polygon serialization failure");
		 //}

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedPolygon.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Polygon ptRes = (Polygon) ii.readObject();
			assertTrue(ptRes != null);
		} catch (Exception ex) {
			fail("Polygon serialization failure");
		}
		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedPolygon1.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Polygon ptRes = (Polygon) ii.readObject();
			assertTrue(ptRes != null);
		} catch (Exception ex) {
			fail("Polygon serialization failure");
		}
	}

	@Test
	public void testSerializePolyline() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Polyline pt = new Polyline();
			pt.startPath(10, 10);
			pt.lineTo(100, 100);
			pt.lineTo(200, 100);
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Polyline ptRes = (Polyline) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			fail("Polyline serialization failure");
		}

		 //try
		 //{
		 //FileOutputStream streamOut = new FileOutputStream("c:/temp/savedPolyline1.txt");
		 //ObjectOutputStream oo = new ObjectOutputStream(streamOut);
		 //Polyline pt = new Polyline();
		 //pt.startPath(10, 10);
		 //pt.lineTo(100, 100);
		 //pt.lineTo(200, 100);
		 //oo.writeObject(pt);
		 //}
		 //catch(Exception ex)
		 //{
		 //fail("Polyline serialization failure");
		 //}

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedPolyline.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Polyline ptRes = (Polyline) ii.readObject();
			assertTrue(ptRes != null);
		} catch (Exception ex) {
			fail("Polyline serialization failure");
		}
		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedPolyline1.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Polyline ptRes = (Polyline) ii.readObject();
			assertTrue(ptRes != null);
		} catch (Exception ex) {
			fail("Polyline serialization failure");
		}
	}

	@Test
	public void testSerializeEnvelope() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Envelope pt = new Envelope(10, 10, 400, 300);
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Envelope ptRes = (Envelope) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			fail("Envelope serialization failure");
		}

		 //try
		 //{
		 //FileOutputStream streamOut = new FileOutputStream("c:/temp/savedEnvelope1.txt");
		 //ObjectOutputStream oo = new ObjectOutputStream(streamOut);
		 //Envelope pt = new Envelope(10, 10, 400, 300);
		 //oo.writeObject(pt);
		 //}
		 //catch(Exception ex)
		 //{
		 //fail("Envelope serialization failure");
		 //}

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedEnvelope.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Envelope ptRes = (Envelope) ii.readObject();
			assertTrue(ptRes.getXMax() == 400);
		} catch (Exception ex) {
			fail("Envelope serialization failure");
		}
		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedEnvelope1.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Envelope ptRes = (Envelope) ii.readObject();
			assertTrue(ptRes.getXMax() == 400);
		} catch (Exception ex) {
			fail("Envelope serialization failure");
		}
	}

	@Test
	public void testSerializeMultiPoint() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			MultiPoint pt = new MultiPoint();
			pt.add(10, 30);
			pt.add(120, 40);
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			MultiPoint ptRes = (MultiPoint) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			fail("MultiPoint serialization failure");
		}

		 //try
		 //{
		 //FileOutputStream streamOut = new FileOutputStream("c:/temp/savedMultiPoint1.txt");
		 //ObjectOutputStream oo = new ObjectOutputStream(streamOut);
		 //MultiPoint pt = new MultiPoint();
		 //pt.add(10, 30);
		 //pt.add(120, 40);
		 //oo.writeObject(pt);
		 //}
		 //catch(Exception ex)
		 //{
		 //fail("MultiPoint serialization failure");
		 //}

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedMultiPoint.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			MultiPoint ptRes = (MultiPoint) ii.readObject();
			assertTrue(ptRes.getPoint(1).getY() == 40);
		} catch (Exception ex) {
			fail("MultiPoint serialization failure");
		}
		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedMultiPoint1.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			MultiPoint ptRes = (MultiPoint) ii.readObject();
			assertTrue(ptRes.getPoint(1).getY() == 40);
		} catch (Exception ex) {
			fail("MultiPoint serialization failure");
		}
	}

	@Test
	public void testSerializeLine() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Line pt = new Line();
			pt.setStart(new Point(10, 30));
			pt.setEnd(new Point(120, 40));
			oo.writeObject(pt);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Line ptRes = (Line) ii.readObject();
			assertTrue(ptRes.equals(pt));
		} catch (Exception ex) {
			// fail("Line serialization failure");
			assertEquals(ex.getMessage(), "Cannot serialize this geometry");
		}
	}

	@Test
	public void testSerializeSR() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			SpatialReference sr = SpatialReference.create(102100);
			oo.writeObject(sr);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			SpatialReference ptRes = (SpatialReference) ii.readObject();
			assertTrue(ptRes.equals(sr));
		} catch (Exception ex) {
			fail("Spatial Reference serialization failure");
		}
	}

	@Test
	public void testSerializeEnvelope2D() {
		try {
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(streamOut);
			Envelope2D env = new Envelope2D(1.213948734, 2.213948734, 11.213948734, 12.213948734);
			oo.writeObject(env);
			ByteArrayInputStream streamIn = new ByteArrayInputStream(
					streamOut.toByteArray());
			ObjectInputStream ii = new ObjectInputStream(streamIn);
			Envelope2D envRes = (Envelope2D)ii.readObject();
			assertTrue(envRes.equals(env));
		} catch (Exception ex) {
			fail("Envelope2D serialization failure");
		}

//		try
//		{
//			 FileOutputStream streamOut = new FileOutputStream(
//			 "c:/temp/savedEnvelope2D.txt");
//			 ObjectOutputStream oo = new ObjectOutputStream(streamOut);
//			 Envelope2D e = new Envelope2D(177.123, 188.234, 999.122, 888.999);
//			 oo.writeObject(e);
//		 }
//		 catch(Exception ex)
//		 {
//		   fail("Envelope2D serialization failure");
//		 }

		try {
			InputStream s = TestSerialization.class
					.getResourceAsStream("savedEnvelope2D.txt");
			ObjectInputStream ii = new ObjectInputStream(s);
			Envelope2D e = (Envelope2D) ii
					.readObject();
			assertTrue(e != null);
			assertTrue(e.equals(new Envelope2D(177.123, 188.234, 999.122, 888.999)));
		} catch (Exception ex) {
			fail("Envelope2D serialization failure");
		}
	}

}
