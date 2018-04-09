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

import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPoint;
import org.junit.Assert;
import org.junit.Test;

public class TestOGCCentroid
{
    @Test
    public void testPoint()
    {
        assertCentroid("POINT (1 2)", new Point(1, 2));
        assertEmptyCentroid("POINT EMPTY");
    }

    @Test
    public void testLineString()
    {
        assertCentroid("LINESTRING (1 1, 2 2, 3 3)", new Point(2, 2));
        assertEmptyCentroid("LINESTRING EMPTY");
    }

    @Test
    public void testPolygon()
    {
        assertCentroid("POLYGON ((1 1, 1 4, 4 4, 4 1))'", new Point(2.5, 2.5));
        assertCentroid("POLYGON ((1 1, 5 1, 3 4))", new Point(3, 2));
        assertCentroid("POLYGON ((0 0, 0 5, 5 5, 5 0, 0 0), (1 1, 1 2, 2 2, 2 1, 1 1))", new Point(2.5416666666666665, 2.5416666666666665));
        assertEmptyCentroid("POLYGON EMPTY");
    }

    @Test
    public void testMultiPoint()
    {
        assertCentroid("MULTIPOINT (1 2, 2 4, 3 6, 4 8)", new Point(2.5, 5));
        assertEmptyCentroid("MULTIPOINT EMPTY");
    }

    @Test
    public void testMultiLineString()
    {
        assertCentroid("MULTILINESTRING ((1 1, 5 1), (2 4, 4 4))')))", new Point(3, 2));
        assertEmptyCentroid("MULTILINESTRING EMPTY");
    }

    @Test
    public void testMultiPolygon()
    {
        assertCentroid("MULTIPOLYGON (((1 1, 1 3, 3 3, 3 1)), ((2 4, 2 6, 6 6, 6 4)))", new Point (3.3333333333333335,4));
        assertEmptyCentroid("MULTIPOLYGON EMPTY");
    }

    private static void assertCentroid(String wkt, Point expectedCentroid)
    {
        OGCGeometry geometry = OGCGeometry.fromText(wkt);
        OGCGeometry centroid = geometry.centroid();
        Assert.assertEquals(centroid, new OGCPoint(expectedCentroid, geometry.getEsriSpatialReference()));
    }

    private static void assertEmptyCentroid(String wkt)
    {
        OGCGeometry geometry = OGCGeometry.fromText(wkt);
        OGCGeometry centroid = geometry.centroid();
        Assert.assertEquals(centroid, new OGCPoint(new Point(), geometry.getEsriSpatialReference()));
    }
}
