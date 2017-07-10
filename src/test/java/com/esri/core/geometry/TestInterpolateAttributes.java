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

public class TestInterpolateAttributes extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void test1() {
		Polyline poly = new Polyline();

		poly.startPath(0, 0);
		poly.lineTo(0, 1.0 / 3.0);
		poly.lineTo(0, 2.0 / 3.0);
		poly.lineTo(0, 4.0 / 3.0);
		poly.lineTo(0, Math.sqrt(6.0));
		poly.lineTo(0, Math.sqrt(7.0));

		poly.setAttribute(VertexDescription.Semantics.M, 0, 0, 3);
		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, 5);
		poly.setAttribute(VertexDescription.Semantics.M, 2, 0, 7);
		poly.setAttribute(VertexDescription.Semantics.M, 5, 0, 11);

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 1, 0, 1);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0) == 3);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == 5);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0) == 7);
		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 3, 0)));
		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 4, 0)));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 5, 0) == 11);

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 1, 0, 2);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0) == 3);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == 5);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0) == 7);
		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 3, 0)));
		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 4, 0)));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 5, 0) == 11);

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 2, 0, 5);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0) == 3);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == 5);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0) == 7);
		double a3 = poly.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0);
		assertTrue(a3 > 7 && a3 < 11);
		double a4 = poly.getAttributeAsDbl(VertexDescription.Semantics.M, 4, 0);
		assertTrue(a4 > a3 && a4 < 11);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 5, 0) == 11);

		poly.startPath(0, Math.sqrt(8.0));
		poly.lineTo(0, Math.sqrt(10.0));
		poly.lineTo(0, Math.sqrt(11.0));
	}

	@Test
	public static void test2() {
		Polyline poly = new Polyline();

		poly.startPath(0, 0);
		poly.lineTo(0, 1.0 / 3.0);

		poly.startPath(0, Math.sqrt(8.0));
		poly.lineTo(0, Math.sqrt(10.0));

		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, Math.sqrt(3.0));

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 1, 1, 0);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == Math
				.sqrt(3.0));
		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 2, 0)));

		poly.setAttribute(VertexDescription.Semantics.M, 3, 0, Math.sqrt(5.0));
		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 1, 1, 1);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == Math
				.sqrt(3.0));
		double a2 = poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0);
		assertTrue(a2 == Math.sqrt(3.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0) == Math
				.sqrt(5.0));
	}

	@Test
	public static void test3() {
		Polyline poly = new Polyline();

		poly.startPath(0, Math.sqrt(0.0));
		poly.lineTo(0, Math.sqrt(5.0));

		poly.startPath(0, Math.sqrt(8.0));
		poly.lineTo(0, Math.sqrt(10.0));

		poly.setAttribute(VertexDescription.Semantics.M, 0, 0, Math.sqrt(3.0));
		poly.setAttribute(VertexDescription.Semantics.M, 2, 0, Math.sqrt(5.0));

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 0, 1, 0);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0) == Math
				.sqrt(3.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == Math
				.sqrt(5.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0) == Math
				.sqrt(5.0));
	}

	@Test
	public static void test4() {
		Polyline poly = new Polyline();

		poly.startPath(0, Math.sqrt(0.0));
		poly.lineTo(0, Math.sqrt(1.0));

		poly.startPath(0, Math.sqrt(1.0));
		poly.lineTo(0, Math.sqrt(2.0));

		poly.startPath(0, Math.sqrt(2.0));
		poly.lineTo(0, Math.sqrt(3.0));

		poly.startPath(0, Math.sqrt(3.0));
		poly.lineTo(0, Math.sqrt(4.0));

		poly.startPath(0, Math.sqrt(4.0));
		poly.lineTo(0, Math.sqrt(5.0));

		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, Math.sqrt(1.0));
		poly.setAttribute(VertexDescription.Semantics.M, 8, 0, Math.sqrt(4.0));

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 1, 4, 0);

		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 0, 0)));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == Math
				.sqrt(1.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0) == Math
				.sqrt(1.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0) == Math
				.sqrt(2.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 4, 0) == Math
				.sqrt(2.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 5, 0) == Math
				.sqrt(3.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 6, 0) == Math
				.sqrt(3.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 7, 0) == Math
				.sqrt(4.0));
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 8, 0) == Math
				.sqrt(4.0));
		assertTrue(NumberUtils.isNaN(poly.getAttributeAsDbl(
				VertexDescription.Semantics.M, 9, 0)));
	}

	@Test
	public static void test5() {
		Polygon poly = new Polygon();

		poly.startPath(0, 0);
		poly.lineTo(0, 1);
		poly.lineTo(1, 1);
		poly.lineTo(1, 0);

		poly.startPath(2, 0);
		poly.lineTo(2, 1);
		poly.lineTo(3, 1);
		poly.lineTo(3, 0);

		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, 1);
		poly.setAttribute(VertexDescription.Semantics.M, 3, 0, 3);

		poly.setAttribute(VertexDescription.Semantics.M, 6, 0, 1);
		poly.setAttribute(VertexDescription.Semantics.M, 5, 0, 4);

		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 3, 1);
		poly.interpolateAttributes(VertexDescription.Semantics.M, 0, 1, 3);
		poly.interpolateAttributes(VertexDescription.Semantics.M, 1, 2, 1);

		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0) == 2);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0) == 1);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0) == 2);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0) == 3);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 4, 0) == 3);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 5, 0) == 4);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 6, 0) == 1);
		assertTrue(poly.getAttributeAsDbl(VertexDescription.Semantics.M, 7, 0) == 2);
	}
}
