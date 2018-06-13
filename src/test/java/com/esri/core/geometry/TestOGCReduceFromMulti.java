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

import org.junit.Test;

import static com.esri.core.geometry.ogc.OGCGeometry.fromText;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class TestOGCReduceFromMulti
{
    @Test
    public void testPoint()
    {
        assertReduceFromMulti("POINT (1 2)", "POINT (1 2)");
        assertReduceFromMulti("POINT EMPTY", "POINT EMPTY");
        assertReduceFromMulti("MULTIPOINT (1 2)", "POINT (1 2)");
        assertReduceFromMulti("MULTIPOINT (1 2, 3 4)", "MULTIPOINT ((1 2), (3 4))");
        assertReduceFromMulti("MULTIPOINT EMPTY", "POINT EMPTY");
    }

    @Test
    public void testLineString()
    {
        assertReduceFromMulti("LINESTRING (0 0, 1 1, 2 3)", "LINESTRING (0 0, 1 1, 2 3)");
        assertReduceFromMulti("LINESTRING EMPTY", "LINESTRING EMPTY");
        assertReduceFromMulti("MULTILINESTRING ((0 0, 1 1, 2 3))", "LINESTRING (0 0, 1 1, 2 3)");
        assertReduceFromMulti("MULTILINESTRING ((0 0, 1 1, 2 3), (4 4, 5 5))", "MULTILINESTRING ((0 0, 1 1, 2 3), (4 4, 5 5))");
        assertReduceFromMulti("MULTILINESTRING EMPTY", "LINESTRING EMPTY");
    }

    @Test
    public void testPolygon()
    {
        assertReduceFromMulti("POLYGON ((0 0, 1 0, 1 1, 0 0))", "POLYGON ((0 0, 1 0, 1 1, 0 0))");
        assertReduceFromMulti("POLYGON EMPTY", "POLYGON EMPTY");
        assertReduceFromMulti("MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)))", "POLYGON ((0 0, 1 0, 1 1, 0 0))");
        assertReduceFromMulti("MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((2 2, 3 2, 3 3, 2 2)))", "MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((2 2, 3 2, 3 3, 2 2)))");
        assertReduceFromMulti("MULTIPOLYGON EMPTY", "POLYGON EMPTY");
    }

    @Test
    public void testGeometryCollection()
    {
        assertReduceFromMulti(gc("POINT (1 2)"), "POINT (1 2)");
        assertReduceFromMulti(gc("MULTIPOINT (1 2)"), "POINT (1 2)");
        assertReduceFromMulti(gc(gc("POINT (1 2)")), "POINT (1 2)");
        assertReduceFromMulti(gc("POINT EMPTY"), "POINT EMPTY");

        assertReduceFromMulti(gc("LINESTRING (0 0, 1 1, 2 3)"), "LINESTRING (0 0, 1 1, 2 3)");
        assertReduceFromMulti(gc("POLYGON ((0 0, 1 0, 1 1, 0 0))"), "POLYGON ((0 0, 1 0, 1 1, 0 0))");

        assertReduceFromMulti(gc("POINT (1 2), LINESTRING (0 0, 1 1, 2 3)"), gc("POINT (1 2), LINESTRING (0 0, 1 1, 2 3)"));

        assertReduceFromMulti("GEOMETRYCOLLECTION EMPTY", "GEOMETRYCOLLECTION EMPTY");
        assertReduceFromMulti(gc("GEOMETRYCOLLECTION EMPTY"), "GEOMETRYCOLLECTION EMPTY");
    }

    private void assertReduceFromMulti(String wkt, String reducedWkt)
    {
        assertEquals(reducedWkt, fromText(wkt).reduceFromMulti().asText());
    }

    private String gc(String wkts)
    {
        return format("GEOMETRYCOLLECTION (%s)", wkts);
    }
}
