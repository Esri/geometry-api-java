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
			System.out.println((endTime - startTime) / 1.0e6);
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
	}
}
