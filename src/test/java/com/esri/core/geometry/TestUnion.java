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

public class TestUnion extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testUnion() {
		Point pt = new Point(10, 20);

		Point pt2 = new Point();
		pt2.setXY(10, 10);

		Envelope env1 = new Envelope(10, 10, 30, 50);
		Envelope env2 = new Envelope(30, 10, 60, 50);
		Geometry[] geomArray = new Geometry[] { env1, env2 };
		SimpleGeometryCursor inputGeometries = new SimpleGeometryCursor(
				geomArray);
		OperatorUnion union = (OperatorUnion) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Union);

		SpatialReference sr = SpatialReference.create(4326);

		GeometryCursor outputCursor = union.execute(inputGeometries, sr, null);
		Geometry result = outputCursor.next();
	}
}
