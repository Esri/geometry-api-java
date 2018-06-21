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
import org.junit.Assert;
import org.junit.Test;

public class TestOGCGeometryCollection {
	@Test
	public void testUnionPoint() {
		// point - point
		assertUnion("POINT (1 2)", "POINT (1 2)", "POINT (1 2)");
		assertUnion("POINT (1 2)", "POINT EMPTY", "POINT (1 2)");
		assertUnion("POINT (1 2)", "POINT (3 4)", "MULTIPOINT ((1 2), (3 4))");

		// point - multi-point
		assertUnion("POINT (1 2)", "MULTIPOINT (1 2)", "POINT (1 2)");
		assertUnion("POINT (1 2)", "MULTIPOINT EMPTY", "POINT (1 2)");
		assertUnion("POINT (1 2)", "MULTIPOINT (3 4)", "MULTIPOINT ((1 2), (3 4))");
		assertUnion("POINT (1 2)", "MULTIPOINT (1 2, 3 4)", "MULTIPOINT ((1 2), (3 4))");
		assertUnion("POINT (1 2)", "MULTIPOINT (3 4, 5 6)", "MULTIPOINT ((1 2), (3 4), (5 6))");

		// point - linestring
		assertUnion("POINT (1 2)", "LINESTRING (3 4, 5 6)", "GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (3 4, 5 6))");
		assertUnion("POINT (1 2)", "LINESTRING EMPTY", "POINT (1 2)");
		assertUnion("POINT (1 2)", "LINESTRING (1 2, 3 4)", "LINESTRING (1 2, 3 4)");
		assertUnion("POINT (1 2)", "LINESTRING (1 1, 1 3, 3 4)", "LINESTRING (1 1, 1 2, 1 3, 3 4)");

		// point - multi-linestring
		assertUnion("POINT (1 2)", "MULTILINESTRING ((3 4, 5 6))",
				"GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (3 4, 5 6))");
		assertUnion("POINT (1 2)", "MULTILINESTRING EMPTY", "POINT (1 2)");
		assertUnion("POINT (1 2)", "MULTILINESTRING ((3 4, 5 6), (7 8, 9 10, 11 12))",
				"GEOMETRYCOLLECTION (POINT (1 2), MULTILINESTRING ((3 4, 5 6), (7 8, 9 10, 11 12)))");
		assertUnion("POINT (1 2)", "MULTILINESTRING ((1 2, 3 4))", "LINESTRING (1 2, 3 4)");
		assertUnion("POINT (1 2)", "MULTILINESTRING ((1 1, 1 3, 3 4), (7 8, 9 10, 11 12))",
				"MULTILINESTRING ((1 1, 1 2, 1 3, 3 4), (7 8, 9 10, 11 12))");

		// point - polygon
		assertUnion("POINT (1 2)", "POLYGON ((0 0, 1 0, 1 1, 0 0))",
				"GEOMETRYCOLLECTION (POINT (1 2), POLYGON ((0 0, 1 0, 1 1, 0 0)))");
		assertUnion("POINT (1 2)", "POLYGON EMPTY", "POINT (1 2)");
		// point inside polygon
		assertUnion("POINT (1 2)", "POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0))", "POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0))");
		// point inside polygon with a hole
		assertUnion("POINT (1 2)", "POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0), (2 2, 2 2.5, 2.5 2.5, 2.5 2, 2 2))",
				"POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0), (2 2, 2 2.5, 2.5 2.5, 2.5 2, 2 2))");
		// point inside polygon's hole
		assertUnion("POINT (1 2)", "POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0), (0.5 1, 0.5 2.5, 2.5 2.5, 2.5 1, 0.5 1))",
				"GEOMETRYCOLLECTION (POINT (1 2), POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0), (0.5 1, 0.5 2.5, 2.5 2.5, 2.5 1, 0.5 1)))");
		// point is a vertex of the polygon
		assertUnion("POINT (1 2)", "POLYGON ((1 2, 2 2, 2 3, 1 3, 1 2))", "POLYGON ((1 2, 2 2, 2 3, 1 3, 1 2))");
		// point on the boundary of the polybon
		assertUnion("POINT (1 2)", "POLYGON ((1 1, 2 1, 2 3, 1 3, 1 1))", "POLYGON ((1 1, 2 1, 2 3, 1 3, 1 2, 1 1))");

		// point - multi-polygon
		assertUnion("POINT (1 2)", "MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)))",
				"GEOMETRYCOLLECTION (POINT (1 2), POLYGON ((0 0, 1 0, 1 1, 0 0)))");
		assertUnion("POINT (1 2)", "MULTIPOLYGON EMPTY", "POINT (1 2)");
		assertUnion("POINT (1 2)", "MULTIPOLYGON (((0 0, 3 0, 3 3, 0 3, 0 0)))", "POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0))");
		assertUnion("POINT (1 2)", "MULTIPOLYGON (((0 0, 3 0, 3 3, 0 3, 0 0)), ((4 4, 5 4, 5 5, 4 4)))",
				"MULTIPOLYGON (((0 0, 3 0, 3 3, 0 3, 0 0)), ((4 4, 5 4, 5 5, 4 4)))");
		assertUnion("POINT (1 2)",
				"MULTIPOLYGON (((0 0, 3 0, 3 3, 0 3, 0 0), (0.5 1, 0.5 2.5, 2.5 2.5, 2.5 1, 0.5 1)))",
				"GEOMETRYCOLLECTION (POINT (1 2), POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0), (0.5 1, 0.5 2.5, 2.5 2.5, 2.5 1, 0.5 1)))");
		assertUnion("POINT (1 2)",
				"MULTIPOLYGON (((0 0, 3 0, 3 3, 0 3, 0 0), (0.5 1, 0.5 2.5, 2.5 2.5, 2.5 1, 0.5 1)), ((4 4, 5 4, 5 5, 4 4)))",
				"GEOMETRYCOLLECTION (POINT (1 2), MULTIPOLYGON (((0 0, 3 0, 3 3, 0 3, 0 0), (0.5 1, 0.5 2.5, 2.5 2.5, 2.5 1, 0.5 1)), ((4 4, 5 4, 5 5, 4 4))))");

		// point - geometry collection
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (POINT (1 2))", "POINT (1 2)");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION EMPTY", "POINT (1 2)");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (POINT (1 2), POINT (2 3))", "MULTIPOINT ((1 2), (2 3))");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (MULTIPOINT (1 2, 2 3))", "MULTIPOINT ((1 2), (2 3))");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4))", "LINESTRING (1 2, 3 4)");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (1 2, 3 4))",
				"GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (1 2, 3 4))");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0)))",
				"POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0))");
		assertUnion("POINT (1 2)", "GEOMETRYCOLLECTION (POINT (5 5), POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0)))",
				"GEOMETRYCOLLECTION (POINT (5 5), POLYGON ((0 0, 3 0, 3 3, 0 3, 0 0)))");
	}

	@Test
	public void testUnionLinestring() {
		// linestring - linestring
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (1 2, 3 4)", "LINESTRING (1 2, 3 4)");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING EMPTY", "LINESTRING (1 2, 3 4)");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (3 4, 1 2)", "LINESTRING (1 2, 3 4)");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (3 4, 5 6)", "LINESTRING (1 2, 3 4, 5 6)");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (5 6, 7 8)", "MULTILINESTRING ((1 2, 3 4), (5 6, 7 8))");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (2 1, 2 5)",
				"MULTILINESTRING ((2 1, 2 3), (1 2, 2 3), (2 3, 3 4), (2 3, 2 5))");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (1 2, 2 3, 4 3)",
				"MULTILINESTRING ((1 2, 2 3), (2 3, 4 3), (2 3, 3 4))");
		assertUnion("LINESTRING (1 2, 3 4)", "LINESTRING (2 3, 2.1 3.1)",
				"LINESTRING (1 2, 2 3, 2.0999999999999996 3.0999999999999996, 3 4)");

		// linestring - polygon
		assertUnion("LINESTRING (1 2, 3 4)", "POLYGON ((5 5, 6 5, 6 6, 5 5))",
				"GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4), POLYGON ((5 5, 6 5, 6 6, 5 5)))");
		assertUnion("LINESTRING (1 2, 3 4)", "POLYGON EMPTY", "LINESTRING (1 2, 3 4)");
		// linestring inside polygon
		assertUnion("LINESTRING (1 2, 3 4)", "POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))",
				"POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))");
		assertUnion("LINESTRING (0 0, 5 0)", "POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))",
				"POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))");
		// linestring crosses polygon's vertex
		assertUnion("LINESTRING (0 0, 6 6)", "POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))",
				"GEOMETRYCOLLECTION (LINESTRING (5 5, 6 6), POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0)))");
		assertUnion("LINESTRING (4 6, 6 4)", "POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))",
				"GEOMETRYCOLLECTION (LINESTRING (4 6, 5 5, 6 4), POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0)))");
		// linestring crosses polygon's boundary
		assertUnion("LINESTRING (1 1, 1 6)", "POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))",
				"GEOMETRYCOLLECTION (LINESTRING (1 5, 1 6), POLYGON ((0 0, 5 0, 5 5, 1 5, 0 5, 0 0)))");

		// linestring - geometry collection
		assertUnion("LINESTRING (1 2, 3 4)", "GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4))", "LINESTRING (1 2, 3 4)");
		assertUnion("LINESTRING (1 2, 3 4)", "GEOMETRYCOLLECTION EMPTY", "LINESTRING (1 2, 3 4)");
		assertUnion("LINESTRING (1 2, 3 4)", "GEOMETRYCOLLECTION (LINESTRING (3 4, 5 6))",
				"LINESTRING (1 2, 3 4, 5 6)");
		assertUnion("LINESTRING (1 2, 3 4)", "GEOMETRYCOLLECTION (LINESTRING (3 4, 5 6), LINESTRING (7 8, 9 10))",
				"MULTILINESTRING ((1 2, 3 4, 5 6), (7 8, 9 10))");
		assertUnion("LINESTRING (1 2, 3 4)", "GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (3 4, 5 6))",
				"LINESTRING (1 2, 3 4, 5 6)");
		assertUnion("LINESTRING (1 2, 3 4)",
				"GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (3 4, 5 6), POLYGON ((3 0, 4 0, 4 1, 3 0)))",
				"GEOMETRYCOLLECTION (LINESTRING (1 2, 3 4, 5 6), POLYGON ((3 0, 4 0, 4 1, 3 0)))");
	}

	@Test
	public void testUnionPolygon() {
		// polygon - polygon
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "POLYGON ((0 0, 1 0, 1 1, 0 0))",
				"POLYGON ((0 0, 1 0, 1 1, 0 0))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "POLYGON EMPTY", "POLYGON ((0 0, 1 0, 1 1, 0 0))");
		// one polygon contains the other
		assertUnion("POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))", "POLYGON ((1 1, 2 1, 2 2, 1 1))",
				"POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0))");
		// The following test fails because vertex order in the union geometry depends
		// on the order of union inputs
		// assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "POLYGON ((0 0, 0.5 0, 0.5 0.5,
		// 0 0))", "POLYGON ((0 0, 0.5 0, 1 0, 1 1, 0.49999999999999994
		// 0.49999999999999994, 0 0))");
		// polygons intersect
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "POLYGON ((0 0.5, 2 0.5, 2 2, 0 2, 0 0.5))",
				"POLYGON ((0 0, 1 0, 1 0.5, 2 0.5, 2 2, 0 2, 0 0.5, 0.5 0.5, 0 0))");
		// disjoint polygons
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "POLYGON ((3 3, 5 3, 5 5, 3 3))",
				"MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((3 3, 5 3, 5 5, 3 3)))");

		// polygon - multi-polygon
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)))",
				"POLYGON ((0 0, 1 0, 1 1, 0 0))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "MULTIPOLYGON EMPTY", "POLYGON ((0 0, 1 0, 1 1, 0 0))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "MULTIPOLYGON (((0 0.5, 2 0.5, 2 2, 0 2, 0 0.5)))",
				"POLYGON ((0 0, 1 0, 1 0.5, 2 0.5, 2 2, 0 2, 0 0.5, 0.5 0.5, 0 0))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "MULTIPOLYGON (((3 3, 5 3, 5 5, 3 3)))",
				"MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((3 3, 5 3, 5 5, 3 3)))");

		// polygon - geometry collection
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "GEOMETRYCOLLECTION (POLYGON ((0 0, 1 0, 1 1, 0 0)))",
				"POLYGON ((0 0, 1 0, 1 1, 0 0))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "GEOMETRYCOLLECTION EMPTY", "POLYGON ((0 0, 1 0, 1 1, 0 0))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))", "GEOMETRYCOLLECTION (POLYGON ((3 3, 5 3, 5 5, 3 3)))",
				"MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((3 3, 5 3, 5 5, 3 3)))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))",
				"GEOMETRYCOLLECTION (POINT (0 0), POLYGON ((3 3, 5 3, 5 5, 3 3)))",
				"MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((3 3, 5 3, 5 5, 3 3)))");
		assertUnion("POLYGON ((0 0, 1 0, 1 1, 0 0))",
				"GEOMETRYCOLLECTION (POINT (10 10), POLYGON ((3 3, 5 3, 5 5, 3 3)))",
				"GEOMETRYCOLLECTION (POINT (10 10), MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((3 3, 5 3, 5 5, 3 3))))");
	}

	private void assertUnion(String wkt, String otherWkt, String expectedWkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
		Assert.assertEquals(expectedWkt, geometry.union(otherGeometry).asText());
		Assert.assertEquals(expectedWkt, otherGeometry.union(geometry).asText());
	}

	@Test
	public void testGeometryCollectionOverlappingContains() {
		assertContains("GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (0 1, 5 1))",
				"GEOMETRYCOLLECTION (MULTIPOINT (0 0, 2 1))");
	}

	private void assertContains(String wkt, String otherWkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
		Assert.assertTrue(geometry.contains(otherGeometry));
		Assert.assertTrue(otherGeometry.within(geometry));
	}
	
	@Test
	public void testGeometryCollectionDisjoint() {
		assertDisjoint("GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (0 1, 5 1))",
				"GEOMETRYCOLLECTION (MULTIPOINT (10 0, 21 1), LINESTRING (30 0, 31 1), POLYGON ((40 0, 41 1, 40 1, 40 0)))");
	}
	
	private void assertDisjoint(String wkt, String otherWkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
		Assert.assertTrue(geometry.disjoint(otherGeometry));
		Assert.assertTrue(otherGeometry.disjoint(geometry));
	}
	
	@Test
	public void testGeometryCollectionIntersect() {
		assertIntersection("GEOMETRYCOLLECTION (POINT (1 2))", "POINT EMPTY", "GEOMETRYCOLLECTION EMPTY");
		assertIntersection("GEOMETRYCOLLECTION (POINT (1 2), MULTIPOINT (3 4, 5 6))", "POINT (3 4)",
				"POINT (3 4)");
		assertIntersection("GEOMETRYCOLLECTION (POINT (1 2), MULTIPOINT (3 4, 5 6))", "POINT (30 40)",
				"GEOMETRYCOLLECTION EMPTY");
		assertIntersection("POINT (30 40)", "GEOMETRYCOLLECTION (POINT (1 2), MULTIPOINT (3 4, 5 6))",
				"GEOMETRYCOLLECTION EMPTY");
	}

	private void assertIntersection(String wkt, String otherWkt, String expectedWkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
		OGCGeometry result = geometry.intersection(otherGeometry);
		Assert.assertEquals(expectedWkt, result.asText());
	}
}
