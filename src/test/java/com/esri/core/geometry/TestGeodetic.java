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

public class TestGeodetic extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testTriangleLength() {
		Point pt_0 = new Point(10, 10);
		Point pt_1 = new Point(20, 20);
		Point pt_2 = new Point(20, 10);
		double length = 0.0;
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_0, pt_1);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_1, pt_2);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_2, pt_0);
		assertTrue(Math.abs(length - 3744719.4094597572) < 1e-12 * 3744719.4094597572);
	}

	@Test
	public void testRotationInvariance() {
		Point pt_0 = new Point(10, 40);
		Point pt_1 = new Point(20, 60);
		Point pt_2 = new Point(20, 40);
		double length = 0.0;
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_0, pt_1);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_1, pt_2);
		length += GeometryEngine.geodesicDistanceOnWGS84(pt_2, pt_0);
		assertTrue(Math.abs(length - 5409156.3896271614) < 1e-12 * 5409156.3896271614);

		for (int i = -540; i < 540; i += 5) {
			pt_0.setXY(i + 10, 40);
			pt_1.setXY(i + 20, 60);
			pt_2.setXY(i + 20, 40);
			length = 0.0;
			length += GeometryEngine.geodesicDistanceOnWGS84(pt_0, pt_1);
			length += GeometryEngine.geodesicDistanceOnWGS84(pt_1, pt_2);
			length += GeometryEngine.geodesicDistanceOnWGS84(pt_2, pt_0);
			assertTrue(Math.abs(length - 5409156.3896271614) < 1e-12 * 5409156.3896271614);
		}
	}

	@Test
	public void testDistanceFailure() {
		{
			Point p1 = new Point(-60.668485, -31.996013333333334);
			Point p2 = new Point(119.13731666666666, 32.251583333333336);
			double d = GeometryEngine.geodesicDistanceOnWGS84(p1, p2);
			assertTrue(Math.abs(d - 19973410.50579736) < 1e-13 * 19973410.50579736);
		}

		{
			Point p1 = new Point(121.27343833333333, 27.467438333333334);
			Point p2 = new Point(-58.55804833333333, -27.035613333333334);
			double d = GeometryEngine.geodesicDistanceOnWGS84(p1, p2);
			assertTrue(Math.abs(d - 19954707.428360686) < 1e-13 * 19954707.428360686);
		}

		{
			Point p1 = new Point(-53.329865, -36.08110166666667);
			Point p2 = new Point(126.52895166666667, 35.97385);
			double d = GeometryEngine.geodesicDistanceOnWGS84(p1, p2);
			assertTrue(Math.abs(d - 19990586.700431127) < 1e-13 * 19990586.700431127);
		}

		{
			Point p1 = new Point(-4.7181166667, 36.1160166667);
			Point p2 = new Point(175.248925, -35.7606716667);
			double d = GeometryEngine.geodesicDistanceOnWGS84(p1, p2);
			assertTrue(Math.abs(d - 19964450.206594173) < 1e-12 * 19964450.206594173);
		}
	}
	
	@Test
	public void testLengthAccurateCR191313() {
		/*
		 * // random_test(); OperatorFactoryLocal engine =
		 * OperatorFactoryLocal.getInstance(); //TODO: Make this:
		 * OperatorShapePreservingLength geoLengthOp =
		 * (OperatorShapePreservingLength)
		 * factory.getOperator(Operator.Type.ShapePreservingLength);
		 * SpatialReference spatialRef = SpatialReference.create(102631);
		 * //[6097817.59407673
		 * ,17463475.2931517],[-1168053.34617516,11199801.3734424
		 * ]]],"spatialReference":{"wkid":102631}
		 * 
		 * Polyline polyline = new Polyline();
		 * polyline.startPath(6097817.59407673, 17463475.2931517);
		 * polyline.lineTo(-1168053.34617516, 11199801.3734424); double length =
		 * geoLengthOp.execute(polyline, spatialRef, null);
		 * assertTrue(Math.abs(length - 2738362.3249366437) < 2e-9 * length);
		 */
	}
	
}
