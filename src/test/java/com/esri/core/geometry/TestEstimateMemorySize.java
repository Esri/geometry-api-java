/*
 Copyright 1995-2018 Esri

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

import com.esri.core.geometry.ogc.OGCConcreteGeometryCollection;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCMultiLineString;
import com.esri.core.geometry.ogc.OGCMultiPoint;
import com.esri.core.geometry.ogc.OGCMultiPolygon;
import com.esri.core.geometry.ogc.OGCPoint;
import com.esri.core.geometry.ogc.OGCPolygon;
import org.junit.Test;
// ClassLayout is GPL with Classpath exception, see http://openjdk.java.net/legal/gplv2+ce.html
import org.openjdk.jol.info.ClassLayout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestEstimateMemorySize {
	@Test
	public void testInstanceSizes() {
		assertEquals(getInstanceSize(AttributeStreamOfFloat.class), SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_FLOAT);
		assertEquals(getInstanceSize(AttributeStreamOfDbl.class), SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_DBL);
		assertEquals(getInstanceSize(AttributeStreamOfInt8.class), SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT8);
		assertEquals(getInstanceSize(AttributeStreamOfInt16.class), SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT16);
		assertEquals(getInstanceSize(AttributeStreamOfInt32.class), SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT32);
		assertEquals(getInstanceSize(AttributeStreamOfInt64.class), SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT64);
		assertEquals(getInstanceSize(Envelope.class), SizeOf.SIZE_OF_ENVELOPE);
		assertEquals(getInstanceSize(Envelope2D.class), SizeOf.SIZE_OF_ENVELOPE2D);
		assertEquals(getInstanceSize(Line.class), SizeOf.SIZE_OF_LINE);
		assertEquals(getInstanceSize(MultiPath.class), SizeOf.SIZE_OF_MULTI_PATH);
		assertEquals(getInstanceSize(MultiPathImpl.class), SizeOf.SIZE_OF_MULTI_PATH_IMPL);
		assertEquals(getInstanceSize(MultiPoint.class), SizeOf.SIZE_OF_MULTI_POINT);
		assertEquals(getInstanceSize(MultiPointImpl.class), SizeOf.SIZE_OF_MULTI_POINT_IMPL);
		assertEquals(getInstanceSize(Point.class), SizeOf.SIZE_OF_POINT);
		assertEquals(getInstanceSize(Polygon.class), SizeOf.SIZE_OF_POLYGON);
		assertEquals(getInstanceSize(Polyline.class), SizeOf.SIZE_OF_POLYLINE);
		assertEquals(getInstanceSize(OGCConcreteGeometryCollection.class),
				SizeOf.SIZE_OF_OGC_CONCRETE_GEOMETRY_COLLECTION);
		assertEquals(getInstanceSize(OGCLineString.class), SizeOf.SIZE_OF_OGC_LINE_STRING);
		assertEquals(getInstanceSize(OGCMultiLineString.class), SizeOf.SIZE_OF_OGC_MULTI_LINE_STRING);
		assertEquals(getInstanceSize(OGCMultiPoint.class), SizeOf.SIZE_OF_OGC_MULTI_POINT);
		assertEquals(getInstanceSize(OGCMultiPolygon.class), SizeOf.SIZE_OF_OGC_MULTI_POLYGON);
		assertEquals(getInstanceSize(OGCPoint.class), SizeOf.SIZE_OF_OGC_POINT);
		assertEquals(getInstanceSize(OGCPolygon.class), SizeOf.SIZE_OF_OGC_POLYGON);
	}

	private static <T> long getInstanceSize(Class<T> clazz) {
		return ClassLayout.parseClass(clazz).instanceSize();
	}

	@Test
	public void testPoint() {
		testGeometry(parseWkt("POINT (1 2)"));
	}

	@Test
	public void testMultiPoint() {
		testGeometry(parseWkt("MULTIPOINT (0 0, 1 1, 2 3)"));
	}

	@Test
	public void testLineString() {
		testGeometry(parseWkt("LINESTRING (0 1, 2 3, 4 5)"));
	}

	@Test
	public void testMultiLineString() {
		testGeometry(parseWkt("MULTILINESTRING ((0 1, 2 3, 4 5), (1 1, 2 2))"));
	}

	@Test
	public void testPolygon() {
		testGeometry(parseWkt("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))"));
	}

	@Test
	public void testMultiPolygon() {
		testGeometry(parseWkt("MULTIPOLYGON (((30 20, 45 40, 10 40, 30 20)), ((15 5, 40 10, 10 20, 5 10, 15 5)))"));
	}

	@Test
	public void testGeometryCollection() {
		testGeometry(parseWkt("GEOMETRYCOLLECTION (POINT(4 6), LINESTRING(4 6,7 10))"));
	}

	private void testGeometry(OGCGeometry geometry) {
		assertTrue(geometry.estimateMemorySize() > 0);
	}

	private static OGCGeometry parseWkt(String wkt) {
		OGCGeometry geometry = OGCGeometry.fromText(wkt);
		geometry.setSpatialReference(null);
		return geometry;
	}
}
