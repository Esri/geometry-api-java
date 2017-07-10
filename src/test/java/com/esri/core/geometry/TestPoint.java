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

import junit.framework.TestCase;

import org.junit.Test;

public class TestPoint extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testPt() {
		Point pt = new Point();
		assertTrue(pt.isEmpty());
		pt.setXY(10, 2);
		assertFalse(pt.isEmpty());
		
		pt.toString();
	}

	@Test
	public void testEnvelope2000() {
		Point points[] = new Point[2000];
		Random random = new Random(69);
		for (int i = 0; i < 2000; i++) {
			points[i] = new Point();
			points[i].setX(random.nextDouble() * 100);
			points[i].setY(random.nextDouble() * 100);
		}
		for (int iter = 0; iter < 2; iter++) {
			final long startTime = System.nanoTime();
			Envelope geomExtent = new Envelope();
			Envelope fullExtent = new Envelope();
			for (int i = 0; i < 2000; i++) {
				points[i].queryEnvelope(geomExtent);
				fullExtent.merge(geomExtent);
			}
			long endTime = System.nanoTime();
		}
	}

	@Test
	public void testBasic() {
		assertTrue(Geometry.getDimensionFromType(Geometry.Type.Polygon.value()) == 2);
		assertTrue(Geometry
				.getDimensionFromType(Geometry.Type.Polyline.value()) == 1);
		assertTrue(Geometry
				.getDimensionFromType(Geometry.Type.Envelope.value()) == 2);
		assertTrue(Geometry.getDimensionFromType(Geometry.Type.Line.value()) == 1);
		assertTrue(Geometry.getDimensionFromType(Geometry.Type.Point.value()) == 0);
		assertTrue(Geometry.getDimensionFromType(Geometry.Type.MultiPoint
				.value()) == 0);

		assertTrue(Geometry.isLinear(Geometry.Type.Polygon.value()));
		assertTrue(Geometry.isLinear(Geometry.Type.Polyline.value()));
		assertTrue(Geometry.isLinear(Geometry.Type.Envelope.value()));
		assertTrue(Geometry.isLinear(Geometry.Type.Line.value()));
		assertTrue(!Geometry.isLinear(Geometry.Type.Point.value()));
		assertTrue(!Geometry.isLinear(Geometry.Type.MultiPoint.value()));

		assertTrue(Geometry.isArea(Geometry.Type.Polygon.value()));
		assertTrue(!Geometry.isArea(Geometry.Type.Polyline.value()));
		assertTrue(Geometry.isArea(Geometry.Type.Envelope.value()));
		assertTrue(!Geometry.isArea(Geometry.Type.Line.value()));
		assertTrue(!Geometry.isArea(Geometry.Type.Point.value()));
		assertTrue(!Geometry.isArea(Geometry.Type.MultiPoint.value()));

		assertTrue(!Geometry.isPoint(Geometry.Type.Polygon.value()));
		assertTrue(!Geometry.isPoint(Geometry.Type.Polyline.value()));
		assertTrue(!Geometry.isPoint(Geometry.Type.Envelope.value()));
		assertTrue(!Geometry.isPoint(Geometry.Type.Line.value()));
		assertTrue(Geometry.isPoint(Geometry.Type.Point.value()));
		assertTrue(Geometry.isPoint(Geometry.Type.MultiPoint.value()));

		assertTrue(Geometry.isMultiVertex(Geometry.Type.Polygon.value()));
		assertTrue(Geometry.isMultiVertex(Geometry.Type.Polyline.value()));
		assertTrue(!Geometry.isMultiVertex(Geometry.Type.Envelope.value()));
		assertTrue(!Geometry.isMultiVertex(Geometry.Type.Line.value()));
		assertTrue(!Geometry.isMultiVertex(Geometry.Type.Point.value()));
		assertTrue(Geometry.isMultiVertex(Geometry.Type.MultiPoint.value()));

		assertTrue(Geometry.isMultiPath(Geometry.Type.Polygon.value()));
		assertTrue(Geometry.isMultiPath(Geometry.Type.Polyline.value()));
		assertTrue(!Geometry.isMultiPath(Geometry.Type.Envelope.value()));
		assertTrue(!Geometry.isMultiPath(Geometry.Type.Line.value()));
		assertTrue(!Geometry.isMultiPath(Geometry.Type.Point.value()));
		assertTrue(!Geometry.isMultiPath(Geometry.Type.MultiPoint.value()));

		assertTrue(!Geometry.isSegment(Geometry.Type.Polygon.value()));
		assertTrue(!Geometry.isSegment(Geometry.Type.Polyline.value()));
		assertTrue(!Geometry.isSegment(Geometry.Type.Envelope.value()));
		assertTrue(Geometry.isSegment(Geometry.Type.Line.value()));
		assertTrue(!Geometry.isSegment(Geometry.Type.Point.value()));
		assertTrue(!Geometry.isSegment(Geometry.Type.MultiPoint.value()));
	}

	@Test
	public void testCopy() {
		Point pt = new Point();
		Point copyPt = (Point) pt.copy();
		assertTrue(copyPt.equals(pt));

		pt.setXY(11, 13);
		copyPt = (Point) pt.copy();
		assertTrue(copyPt.equals(pt));
		assertTrue(copyPt.getXY().isEqual(new Point2D(11, 13)));
		
		assertTrue(copyPt.getXY().equals((Object)new Point2D(11, 13)));
	}

	@Test
	public void testEnvelope2D_corners() {
		Envelope2D env = new Envelope2D(0, 1, 2, 3);
		assertFalse(env.equals(null));
		assertTrue(env.equals((Object)new Envelope2D(0, 1, 2, 3)));
		
		Point2D pt2D = env.getLowerLeft();
		assertTrue(pt2D.equals(Point2D.construct(0, 1)));
		pt2D = env.getUpperLeft();
		assertTrue(pt2D.equals(Point2D.construct(0, 3)));
		pt2D = env.getUpperRight();
		assertTrue(pt2D.equals(Point2D.construct(2, 3)));
		pt2D = env.getLowerRight();
		assertTrue(pt2D.equals(Point2D.construct(2, 1)));

		{
			Point2D[] corners = new Point2D[4];
			env.queryCorners(corners);
			assertTrue(corners[0].equals(Point2D.construct(0, 1)));
			assertTrue(corners[1].equals(Point2D.construct(0, 3)));
			assertTrue(corners[2].equals(Point2D.construct(2, 3)));
			assertTrue(corners[3].equals(Point2D.construct(2, 1)));
	
			env.queryCorners(corners);
			assertTrue(corners[0].equals(env.queryCorner(0)));
			assertTrue(corners[1].equals(env.queryCorner(1)));
			assertTrue(corners[2].equals(env.queryCorner(2)));
			assertTrue(corners[3].equals(env.queryCorner(3)));
		}
		
		{
			Point2D[] corners = new Point2D[4];
			env.queryCornersReversed(corners);
			assertTrue(corners[0].equals(Point2D.construct(0, 1)));
			assertTrue(corners[1].equals(Point2D.construct(2, 1)));
			assertTrue(corners[2].equals(Point2D.construct(2, 3)));
			assertTrue(corners[3].equals(Point2D.construct(0, 3)));
			
			env.queryCornersReversed(corners);
			assertTrue(corners[0].equals(env.queryCorner(0)));
			assertTrue(corners[1].equals(env.queryCorner(3)));
			assertTrue(corners[2].equals(env.queryCorner(2)));
			assertTrue(corners[3].equals(env.queryCorner(1)));
		}
		
		assertTrue(env.getCenter().equals(Point2D.construct(1, 2)));
		
		assertFalse(env.containsExclusive(env.getUpperLeft()));
		assertTrue(env.contains(env.getUpperLeft()));
		assertTrue(env.containsExclusive(env.getCenter()));
	}
	
	@Test
	public void testReplaceNaNs() {
		Envelope env = new Envelope();
		Point pt = new Point();
		pt.setXY(1, 2);
		pt.setZ(Double.NaN);
		pt.queryEnvelope(env);
		pt.replaceNaNs(VertexDescription.Semantics.Z, 5);
		assertTrue(pt.equals(new Point(1, 2, 5)));

		assertTrue(env.hasZ());
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).isEmpty());
		env.replaceNaNs(VertexDescription.Semantics.Z, 5);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).equals(new Envelope1D(5, 5)));
	}	
}
