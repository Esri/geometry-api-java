/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.core.geometry;

import com.esri.core.geometry.ogc.OGCGeometry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestOGCContains {
	@Test
	public void testPoint() {
		// point
		assertContains("POINT (1 2)", "POINT (1 2)");
		assertContains("POINT (1 2)", "GEOMETRYCOLLECTION (POINT (1 2))");
		assertNotContains("POINT (1 2)", "POINT EMPTY");
		assertNotContains("POINT (1 2)", "POINT (3 4)");

		// multi-point
		assertContains("MULTIPOINT (1 2, 3 4)", "MULTIPOINT (1 2, 3 4)");
		assertContains("MULTIPOINT (1 2, 3 4)", "MULTIPOINT (1 2)");
		assertContains("MULTIPOINT (1 2, 3 4)", "POINT (3 4)");
		assertContains("MULTIPOINT (1 2, 3 4)", "GEOMETRYCOLLECTION (MULTIPOINT (1 2), POINT (3 4))");
		assertContains("MULTIPOINT (1 2, 3 4)", "GEOMETRYCOLLECTION (POINT (1 2))");
		assertNotContains("MULTIPOINT (1 2, 3 4)", "MULTIPOINT EMPTY");
	}

	@Test
	public void testLineString() {
		// TODO Add more tests
		assertContains("LINESTRING (0 1, 5 1)", "POINT (2 1)");
	}

	@Test
	public void testPolygon() {
		// TODO Fill in
	}

	@Test
	public void testGeometryCollection() {
		// TODO Add more tests
		assertContains("GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (0 1, 5 1))",
				"GEOMETRYCOLLECTION (MULTIPOINT (0 0, 2 1))");
	}

	private void assertContains(String wkt, String otherWkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);

		assertTrue(geometry.contains(otherGeometry));
		assertTrue(otherGeometry.within(geometry));
		assertFalse(geometry.disjoint(otherGeometry));
	}

	private void assertNotContains(String wkt, String otherWkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
		assertFalse(geometry.contains(otherGeometry));
		assertFalse(otherGeometry.within(geometry));
	}
}

