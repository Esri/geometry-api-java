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

public class TestOGCDistance
{
    @Test
    public void testPoint()
    {
        assertDistance("POINT (1 2)", "POINT (2 2)", 1);
        assertDistance("POINT (1 2)", "POINT (1 2)", 0);
        assertNanDistance("POINT (1 2)", "POINT EMPTY");

        assertDistance(gc("POINT (1 2)"), "POINT (2 2)", 1);
        assertDistance(gc("POINT (1 2)"), "POINT (1 2)", 0);
        assertNanDistance(gc("POINT (1 2)"), "POINT EMPTY");
        assertDistance(gc("POINT (1 2)"), gc("POINT (2 2)"), 1);

        assertDistance("MULTIPOINT (1 0, 2 0, 3 0)", "POINT (2 1)", 1);
        assertDistance(gc("MULTIPOINT (1 0, 2 0, 3 0)"), "POINT (2 1)", 1);
        assertDistance(gc("POINT (1 0), POINT (2 0), POINT (3 0))"), "POINT (2 1)", 1);

        assertDistance(gc("POINT (1 0), POINT EMPTY"), "POINT (2 0)", 1);
    }

    private void assertDistance(String wkt, String otherWkt, double distance)
    {
        assertEquals(distance, fromText(wkt).distance(fromText(otherWkt)), 0.000001);
        assertEquals(distance, fromText(otherWkt).distance(fromText(wkt)), 0.000001);
    }

    private void assertNanDistance(String wkt, String otherWkt)
    {
        assertEquals(Double.NaN, fromText(wkt).distance(fromText(otherWkt)), 0.000001);
        assertEquals(Double.NaN, fromText(otherWkt).distance(fromText(wkt)), 0.000001);
    }

    private String gc(String wkts)
    {
        return format("GEOMETRYCOLLECTION (%s)", wkts);
    }
}
