package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestShapePreserving extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testBoxAreasWithinToleranceCR186615() {
		/*
		 * //TODO: Implement these OperatorShapePreservingArea shapeAreaOp =
		 * OperatorShapePreservingArea.local(); OperatorShapePreservingLength
		 * shapeLengthOp = OperatorShapePreservingLength.local();
		 * 
		 * Polyline polyline2 = new Polyline(); polyline2.startPath(190, 0);
		 * polyline2.lineTo(200,0); SpatialReference spatialRefWGS =
		 * SpatialReference.create(4326); double lengthEquator10Degree =
		 * shapeLengthOp.execute(polyline2, spatialRefWGS, null);
		 * assertTrue(lengthEquator10Degree != 0.00);
		 * 
		 * Polyline polylineEquator2 = new Polyline();
		 * polylineEquator2.startPath(170, 0); polylineEquator2.lineTo(180,0);
		 * double lengthEquator10Degree2 =
		 * shapeLengthOp.execute(polylineEquator2, spatialRefWGS, null);
		 * assertTrue(GeomCommonMethods.compareDouble(lengthEquator10Degree2,
		 * lengthEquator10Degree, Math.pow(10.0,-10)));
		 * 
		 * SpatialReference spatialRefWGSMerc = SpatialReference.create(102100);
		 * double PCS5 = 111319.49079327358 * 5; double PCS180 =
		 * 20037508.342789244; double CSYMax = 30240970.0; double CSYMin =
		 * -30240970.0;
		 * 
		 * Polyline polylineEquator3 = new Polyline();
		 * polylineEquator3.startPath(-PCS180 - 4*PCS5, 0);
		 * polylineEquator3.lineTo(-PCS180 - 2*PCS5, 0); double
		 * lengthEquatorMercDegree = shapeLengthOp.execute(*polylineEquator3,
		 * spatialRefWGSMerc, null);
		 * assertTrue(GeomCommonMethods.compareDouble(lengthEquatorMercDegree,
		 * lengthEquator10Degree, Math.pow(10.0,-10)));
		 * 
		 * Polyline polylineBox = new Polyline(); polylineBox.startPath(PCS180 -
		 * 2*PCS5, 30240970.0 / 9); polylineBox.lineTo(PCS180 + 2*PCS5,
		 * 30240970.0 / 9); polylineBox.lineTo(PCS180 + 2*PCS5, -30240970.0 /
		 * 9); polylineBox.lineTo(PCS180 - 2*PCS5, -30240970.0 / 9);
		 * polylineBox.lineTo(PCS180 - 2*PCS5, 30240970.0 / 9);
		 * 
		 * Polygon polygonBox = new Polygon(); polygonBox.startPath(PCS180 -
		 * 2*PCS5, 30240970.0 / 9); polygonBox.lineTo(PCS180 + 2*PCS5,
		 * 30240970.0 / 9); polygonBox.lineTo(PCS180 + 2*PCS5, -30240970.0 / 9);
		 * polygonBox.lineTo(PCS180 - 2*PCS5, -30240970.0 / 9);
		 * polygonBox.lineTo(PCS180 - 2*PCS5, 30240970.0 / 9);
		 * 
		 * Envelope envelopeBox = new Envelope();
		 * polygonBox.queryEnvelope(envelopeBox);
		 * 
		 * double lengthBox1 = shapeLengthOp.execute(polylineBox,
		 * spatialRefWGSMerc, null); double lengthBox2 =
		 * shapeLengthOp.execute(polygonBox, spatialRefWGSMerc, null); double
		 * lengthBox3 = shapeLengthOp.execute(envelopeBox, spatialRefWGSMerc,
		 * null); assertTrue(GeomCommonMethods.compareDouble(lengthBox1,
		 * lengthBox2, Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(lengthBox1, lengthBox3,
		 * Math.pow(10.0,-10)));
		 * 
		 * // Repeated polygon area Polygon polygonBox1 = new Polygon();
		 * polygonBox1.startPath(-PCS180 - 6 * PCS5, 30240970.0 / 9);
		 * polygonBox1.lineTo(-PCS180 - 4 * PCS5, 30240970.0 / 9);
		 * polygonBox1.lineTo(-PCS180 - 4 * PCS5, -30240970.0 / 9);
		 * polygonBox1.lineTo(-PCS180 - 6 * PCS5, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox2 = new Polygon(); polygonBox2.startPath(-PCS180 -
		 * 2 * PCS5, 30240970.0 / 9); polygonBox2.lineTo(-PCS180, 30240970.0 /
		 * 9); polygonBox2.lineTo(-PCS180, -30240970.0 / 9);
		 * polygonBox2.lineTo(-PCS180 - 2 * PCS5, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox3 = new Polygon(); polygonBox3.startPath(-PCS180 -
		 * PCS5, 30240970.0 / 9); polygonBox3.lineTo(-PCS180 + PCS5, 30240970.0
		 * / 9); polygonBox3.lineTo(-PCS180 + PCS5, -30240970.0 / 9);
		 * polygonBox3.lineTo(-PCS180 - PCS5, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox4 = new Polygon(); polygonBox4.startPath(-PCS180,
		 * 30240970.0 / 9); polygonBox4.lineTo(-PCS180 + 2 * PCS5, 30240970.0 /
		 * 9); polygonBox4.lineTo(-PCS180 + 2 * PCS5, -30240970.0 / 9);
		 * polygonBox4.lineTo(-PCS180, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox5 = new Polygon(); polygonBox5.startPath(PCS180 - 6
		 * * PCS5, 30240970.0 / 9); polygonBox5.lineTo(PCS180 - 4 * PCS5,
		 * 30240970.0 / 9); polygonBox5.lineTo(PCS180 - 4 * PCS5, -30240970.0 /
		 * 9); polygonBox5.lineTo(PCS180 - 6 * PCS5, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox6 = new Polygon(); polygonBox6.startPath(PCS180 - 2
		 * * PCS5, 30240970.0 / 9); polygonBox6.lineTo(PCS180, 30240970.0 / 9);
		 * polygonBox6.lineTo(PCS180, -30240970.0 / 9);
		 * polygonBox6.lineTo(PCS180 - 2 * PCS5, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox7 = new Polygon(); polygonBox7.startPath(PCS180 -
		 * PCS5, 30240970.0 / 9); polygonBox7.lineTo(PCS180 + PCS5, 30240970.0 /
		 * 9); polygonBox7.lineTo(PCS180 + PCS5, -30240970.0 / 9);
		 * polygonBox7.lineTo(PCS180 - PCS5, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox8 = new Polygon(); polygonBox8.startPath(PCS180,
		 * 30240970.0 / 9); polygonBox8.lineTo(PCS180 + 2 * PCS5, 30240970.0 /
		 * 9); polygonBox8.lineTo(PCS180 + 2 * PCS5, -30240970.0 / 9);
		 * polygonBox8.lineTo(PCS180, -30240970.0 / 9);
		 * 
		 * Polygon polygonBox9 = new Polygon(); polygonBox9.startPath(PCS180 + 2
		 * * PCS5, 30240970.0 / 9); polygonBox9.lineTo(PCS180 + 4 * PCS5,
		 * 30240970.0 / 9); polygonBox9.lineTo(PCS180 + 4 * PCS5, -30240970.0 /
		 * 9); polygonBox9.lineTo(PCS180 + 2 * PCS5, -30240970.0 / 9);
		 * 
		 * double area1 = shapeAreaOp.execute(polygonBox1, spatialRefWGSMerc,
		 * null); double area2 = shapeAreaOp.execute(polygonBox2,
		 * spatialRefWGSMerc, null); double area3 =
		 * shapeAreaOp.execute(polygonBox3, spatialRefWGSMerc, null); double
		 * area4 = shapeAreaOp.execute(polygonBox4, spatialRefWGSMerc, null);
		 * double area5 = shapeAreaOp.execute(polygonBox5, spatialRefWGSMerc,
		 * null); double area6 = shapeAreaOp.execute(polygonBox6,
		 * spatialRefWGSMerc, null); double area7 =
		 * shapeAreaOp.execute(polygonBox7, spatialRefWGSMerc, null); double
		 * area8 = shapeAreaOp.execute(polygonBox8, spatialRefWGSMerc, null);
		 * double area9 = shapeAreaOp.execute(polygonBox9, spatialRefWGSMerc,
		 * null);
		 * 
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area2,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area3,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area4,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area5,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area6,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area7,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area8,
		 * Math.pow(10.0,-10)));
		 * assertTrue(GeomCommonMethods.compareDouble(area1, area9,
		 * Math.pow(10.0,-10)));
		 */
	}
}
