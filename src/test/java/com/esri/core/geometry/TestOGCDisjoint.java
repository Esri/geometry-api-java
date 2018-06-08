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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestOGCDisjoint
{
    @Test
    public void testPoint()
    {
        // point
        assertDisjoint("POINT (1 2)", "POINT (3 4)");
        assertDisjoint("POINT (1 2)", "POINT EMPTY");
        assertNotDisjoint("POINT (1 2)", "POINT (1 2)", "POINT (1 2)");

        // multi-point
        assertDisjoint("POINT (1 2)", "MULTIPOINT (3 4, 5 6)");
        assertDisjoint("POINT (1 2)", "MULTIPOINT EMPTY");
        assertNotDisjoint("POINT (1 2)", "MULTIPOINT (1 2, 3 4, 5 6)", "POINT (1 2)");
        assertNotDisjoint("POINT (1 2)", "MULTIPOINT (1 2)", "POINT (1 2)");
    }

    @Test
    public void testLinestring()
    {
        // TODO Fill in
    }

    @Test
    public void testPolygon()
    {
        // TODO Fill in
    }

    @Test
    public void testGeometryCollection()
    {
        assertDisjoint("GEOMETRYCOLLECTION (POINT (1 2))", "POINT (3 4)");
        // GeometryException: internal error
        assertDisjoint("GEOMETRYCOLLECTION (POINT (1 2))", "POINT EMPTY");
        assertNotDisjoint("GEOMETRYCOLLECTION (POINT (1 2))", "POINT (1 2)", "POINT (1 2)");

        assertDisjoint("GEOMETRYCOLLECTION (POINT (1 2), MULTIPOINT (3 4, 5 6))", "POINT (0 0)");
        assertNotDisjoint("GEOMETRYCOLLECTION (POINT (1 2), MULTIPOINT (3 4, 5 6))", "POINT (3 4)", "POINT (3 4)");

        String wkt = "GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (0 0, 5 0), POLYGON ((2 2, 3 2, 3 3, 2 2)))";
        assertDisjoint(wkt, gc("POINT (0 2)"));

        assertNotDisjoint(wkt, gc("POINT (1 2)"), "POINT (1 2)");
        // point within the line
        assertNotDisjoint(wkt, gc("POINT (0 0)"), "POINT (0 0)");
        assertNotDisjoint(wkt, gc("POINT (1 0)"), "POINT (1 0)");
        // point within the polygon
        assertNotDisjoint(wkt, gc("POINT (2 2)"), "POINT (2 2)");
        assertNotDisjoint(wkt, gc("POINT (2.5 2)"), "POINT (2.5 2)");
        assertNotDisjoint(wkt, gc("POINT (2.5 2.1)"), "POINT (2.5 2.1)");

        assertDisjoint(wkt, gc("LINESTRING (0 2, 1 3)"));

        // line intersects the point
        assertNotDisjoint(wkt, gc("LINESTRING (0 1, 2 3)"), "POINT (1 2)");
        // line intersects the line
        assertNotDisjoint(wkt, gc("LINESTRING (0 0, 1 0)"), "LINESTRING (0 0, 1 0)");
        assertNotDisjoint(wkt, gc("LINESTRING (5 -1, 5 1)"), "POINT (5 0)");
        // line intersects the polygon
        assertNotDisjoint(wkt, gc("LINESTRING (0 0, 5 5)"), gc("POINT (0 0), LINESTRING (2 2, 3 3)"));
        assertNotDisjoint(wkt, gc("LINESTRING (0 2.5, 2.6 2.5)"), "LINESTRING (2.5 2.5, 2.6 2.5)");

        assertDisjoint(wkt, gc("POLYGON ((5 5, 6 5, 6 6, 5 5))"));
        assertDisjoint(wkt, gc("POLYGON ((-1 -1, 10 -1, 10 10, -1 10, -1 -1), (-0.1 -0.1, 5.1 -0.1, 5.1 5.1, -0.1 5.1, -0.1 -0.1))"));

        assertNotDisjoint(wkt, gc("POLYGON ((-1 -1, 10 -1, 10 10, -1 10, -1 -1))"), gc("POINT (1 2), LINESTRING (0 0, 5 0), POLYGON ((2 2, 3 2, 3 3, 2 2))"));
        assertNotDisjoint(wkt, gc("POLYGON ((2 -1, 4 -1, 4 1, 2 1, 2 -1))"), "LINESTRING (2 0, 4 0)");
        assertNotDisjoint(wkt, gc("POLYGON ((0 1, 1.5 1, 1.5 2.5, 0 2.5, 0 1))"), "POINT (1 2)");
        assertNotDisjoint(wkt, gc("POLYGON ((5 0, 6 0, 6 5, 5 0))"), "POINT (5 0)");
    }

    private String gc(String wkts)
    {
        return format("GEOMETRYCOLLECTION (%s)", wkts);
    }

    private void assertDisjoint(String wkt, String otherWkt)
    {
        OGCGeometry geometry = OGCGeometry.fromText(wkt);
        OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
        assertTrue(geometry.disjoint(otherGeometry));
        assertFalse(geometry.intersects(otherGeometry));
        assertTrue(geometry.intersection(otherGeometry).isEmpty());

        assertTrue(otherGeometry.disjoint(geometry));
        assertFalse(otherGeometry.intersects(geometry));
        assertTrue(otherGeometry.intersection(geometry).isEmpty());
    }

    private void assertNotDisjoint(String wkt, String otherWkt, String intersectionWkt)
    {
        OGCGeometry geometry = OGCGeometry.fromText(wkt);
        OGCGeometry otherGeometry = OGCGeometry.fromText(otherWkt);
        assertFalse(geometry.disjoint(otherGeometry));
        assertTrue(geometry.intersects(otherGeometry));
        assertEquals(intersectionWkt, geometry.intersection(otherGeometry).asText());

        assertFalse(otherGeometry.disjoint(geometry));
        assertTrue(otherGeometry.intersects(geometry));
        assertEquals(intersectionWkt, otherGeometry.intersection(geometry).asText());
    }
}
