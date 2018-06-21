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

import com.esri.core.geometry.ogc.OGCConcreteGeometryCollection;
import org.junit.Test;

import static com.esri.core.geometry.ogc.OGCGeometry.fromText;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestOGCGeometryCollectionFlatten
{
    @Test
    public void test()
    {
        assertFlatten("GEOMETRYCOLLECTION EMPTY", "GEOMETRYCOLLECTION EMPTY");
        assertFlatten(gc("POINT (1 2)"), gc("MULTIPOINT ((1 2))"));
        assertFlatten(gc("POINT (1 2), POINT EMPTY"), gc("MULTIPOINT ((1 2))"));
        assertFlatten(gc("POINT (1 2), MULTIPOINT (3 4, 5 6)"), gc("MULTIPOINT ((1 2), (3 4), (5 6))"));
        assertFlatten(gc("POINT (1 2), POINT (3 4), " + gc("POINT (5 6)")), gc("MULTIPOINT ((1 2), (3 4), (5 6))"));
    }

    private void assertFlatten(String wkt, String flattenedWkt)
    {
        OGCConcreteGeometryCollection collection = (OGCConcreteGeometryCollection) fromText(wkt);
        assertEquals(flattenedWkt, collection.flatten().asText());
        assertTrue(collection.flatten().isFlattened());
        assertEquals(flattenedWkt, collection.flatten().flatten().asText());
    }

    private String gc(String wkts)
    {
        return format("GEOMETRYCOLLECTION (%s)", wkts);
    }
}
