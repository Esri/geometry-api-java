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

public class TestFailed extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testCenterXY() {
		Envelope env = new Envelope(-130, 30, -70, 50);
		assertEquals(-100, env.getCenterX(), 0);
		assertEquals(40, env.getCenterY(), 0);
	}

	@Test
	public void testGeometryOperationSupport() {
		Geometry baseGeom = new Point(-130, 10);
		Geometry comparisonGeom = new Point(-130, 10);
		SpatialReference sr = SpatialReference.create(4326);

		@SuppressWarnings("unused")
		Geometry diffGeom = null;
		int noException = 1; // no exception
		try {
			diffGeom = GeometryEngine.difference(baseGeom, comparisonGeom, sr);

		} catch (IllegalArgumentException ex) {
			noException = 0;
		} catch (GeometryException ex) {
			noException = 0;
		}
		assertEquals(noException, 1);
	}

	@Test
	public void TestIntersection() {
		OperatorIntersects op = (OperatorIntersects) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Intersects);
		Polygon polygon = new Polygon();
		// outer ring1
		polygon.startPath(0, 0);
		polygon.lineTo(10, 10);
		polygon.lineTo(20, 0);

		Point point1 = new Point(15, 10);
		Point point2 = new Point(2, 10);
		Point point3 = new Point(5, 5);
		boolean res = op.execute(polygon, point1, null, null);
		assertTrue(!res);
		res = op.execute(polygon, point2, null, null);
		assertTrue(!res);
		res = op.execute(polygon, point3, null, null);
		assertTrue(res);
	}
}
