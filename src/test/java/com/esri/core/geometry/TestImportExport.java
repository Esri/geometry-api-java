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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.TestCase;
import org.junit.Test;

public class TestImportExport extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testImportExportShapePolygon() {
//		{
//			String s = "MULTIPOLYGON (((-1.4337158203098852 53.42590083930004, -1.4346462383651897 53.42590083930004, -1.4349713164114632 53.42426406667512, -1.4344808816770183 53.42391134176576, -1.4337158203098852 53.424339319373516, -1.4337158203098852 53.42590083930004, -1.4282226562499147 53.42590083930004, -1.4282226562499147 53.42262754610009, -1.423659941537096 53.42262754610009, -1.4227294921872726 53.42418897437618, -1.4199829101572732 53.42265258737483, -1.4172363281222147 53.42418897437334, -1.4144897460898278 53.42265258737625, -1.4144897460898278 53.42099079900008, -1.4117431640598568 53.42099079712516, -1.4117431640598568 53.41849780932388, -1.4112778948070286 53.41771711805022, -1.4114404909237805 53.41689867267529, -1.411277890108579 53.416080187950215, -1.4117431640598568 53.4152995338453, -1.4117431657531654 53.40953184824072, -1.41723632610001 53.40953184402311, -1.4172363281199125 53.406257299700044, -1.4227294921899158 53.406257299700044, -1.4227294921899158 53.40789459668797, -1.4254760767598498 53.40789460061099, -1.4262193642339867 53.40914148401417, -1.4273828468095076 53.409531853100034, -1.4337158203098852 53.409531790075235, -1.4337158203098852 53.41280609140024, -1.4392089843723568 53.41280609140024, -1.439208984371362 53.41608014067522, -1.441160015802268 53.41935368587538, -1.4427511170075604 53.41935368587538, -1.4447021484373863 53.42099064750012, -1.4501953124999432 53.42099064750012, -1.4501953124999432 53.43214683850347, -1.4513643355446106 53.434108816701794, -1.4502702625278232 53.43636597733034, -1.4494587195580948 53.437354845300334, -1.4431075935937656 53.437354845300334, -1.4372459179209045 53.43244635455021, -1.433996276212838 53.42917388040006, -1.4337158203098852 53.42917388040006, -1.4337158203098852 53.42590083930004)))";
//			Geometry g = OperatorImportFromWkt.local().execute(0,  Geometry.Type.Unknown, s, null);
//			boolean result1 = OperatorSimplify.local().isSimpleAsFeature(g, null, null);
//			boolean result2 = OperatorSimplifyOGC.local().isSimpleOGC(g, null, true, null, null);
//			Geometry simple = OperatorSimplifyOGC.local().execute(g, null, true, null);
//			OperatorFactoryLocal.saveToWKTFileDbg("c:/temp/simplifiedeeee",  simple, null);
//			int i = 0;
//		}
		OperatorExportToESRIShape exporterShape = (OperatorExportToESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToESRIShape);
		OperatorImportFromESRIShape importerShape = (OperatorImportFromESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromESRIShape);

		Polygon polygon = makePolygon();

		byte[] esriShape = GeometryEngine.geometryToEsriShape(polygon);
		Geometry imported = GeometryEngine.geometryFromEsriShape(esriShape, Geometry.Type.Unknown);
		TestCommonMethods.compareGeometryContent((MultiPath) imported, polygon);

		// Test Import Polygon from Polygon
		ByteBuffer polygonShapeBuffer = exporterShape.execute(0, polygon);
		Geometry polygonShapeGeometry = importerShape.execute(0, Geometry.Type.Polygon, polygonShapeBuffer);

		TestCommonMethods.compareGeometryContent((MultiPath) polygonShapeGeometry, polygon);

		// Test Import Envelope from Polygon
		Geometry envelopeShapeGeometry = importerShape.execute(0, Geometry.Type.Envelope, polygonShapeBuffer);
		Envelope envelope = (Envelope) envelopeShapeGeometry;

		@SuppressWarnings("unused") Envelope env = new Envelope(), otherenv = new Envelope();
		polygon.queryEnvelope(otherenv);
		assertTrue(envelope.getXMin() == otherenv.getXMin());
		assertTrue(envelope.getXMax() == otherenv.getXMax());
		assertTrue(envelope.getYMin() == otherenv.getYMin());
		assertTrue(envelope.getYMax() == otherenv.getYMax());

		Envelope1D interval, otherinterval;
		interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		otherinterval = polygon.queryInterval(VertexDescription.Semantics.Z, 0);
		assertTrue(interval.vmin == otherinterval.vmin);
		assertTrue(interval.vmax == otherinterval.vmax);
	}

	@Test
	public static void testImportExportShapePolyline() {
		OperatorExportToESRIShape exporterShape = (OperatorExportToESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToESRIShape);
		OperatorImportFromESRIShape importerShape = (OperatorImportFromESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromESRIShape);

		Polyline polyline = makePolyline();

		// Test Import Polyline from Polyline
		ByteBuffer polylineShapeBuffer = exporterShape.execute(0, polyline);
		Geometry polylineShapeGeometry = importerShape.execute(0, Geometry.Type.Polyline, polylineShapeBuffer);

		// TODO test this
		TestCommonMethods.compareGeometryContent((MultiPath) polylineShapeGeometry, polyline);

		// Test Import Envelope from Polyline;
		Geometry envelopeShapeGeometry = importerShape.execute(0, Geometry.Type.Envelope, polylineShapeBuffer);
		Envelope envelope = (Envelope) envelopeShapeGeometry;

		Envelope env = new Envelope(), otherenv = new Envelope();
		envelope.queryEnvelope(env);
		polyline.queryEnvelope(otherenv);
		assertTrue(env.getXMin() == otherenv.getXMin());
		assertTrue(env.getXMax() == otherenv.getXMax());
		assertTrue(env.getYMin() == otherenv.getYMin());
		assertTrue(env.getYMax() == otherenv.getYMax());

		Envelope1D interval, otherinterval;
		interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		otherinterval = polyline.queryInterval(VertexDescription.Semantics.Z, 0);
		assertTrue(interval.vmin == otherinterval.vmin);
		assertTrue(interval.vmax == otherinterval.vmax);
	}

	@Test
	public static void testImportExportShapeMultiPoint() {
		OperatorExportToESRIShape exporterShape = (OperatorExportToESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToESRIShape);
		OperatorImportFromESRIShape importerShape = (OperatorImportFromESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromESRIShape);

		MultiPoint multipoint = makeMultiPoint();

		// Test Import MultiPoint from MultiPoint
		ByteBuffer multipointShapeBuffer = exporterShape.execute(0, multipoint);
		MultiPoint multipointShapeGeometry = (MultiPoint) importerShape.execute(0, Geometry.Type.MultiPoint, multipointShapeBuffer);

		TestCommonMethods.compareGeometryContent((MultiPoint) multipointShapeGeometry, multipoint);

		// Test Import Envelope from MultiPoint
		Geometry envelopeShapeGeometry = importerShape.execute(0, Geometry.Type.Envelope, multipointShapeBuffer);
		Envelope envelope = (Envelope) envelopeShapeGeometry;

		Envelope env = new Envelope(), otherenv = new Envelope();
		envelope.queryEnvelope(env);
		multipoint.queryEnvelope(otherenv);
		assertTrue(env.getXMin() == otherenv.getXMin());
		assertTrue(env.getXMax() == otherenv.getXMax());
		assertTrue(env.getYMin() == otherenv.getYMin());
		assertTrue(env.getYMax() == otherenv.getYMax());

		Envelope1D interval, otherinterval;
		interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		otherinterval = multipoint.queryInterval(VertexDescription.Semantics.Z, 0);
		assertTrue(interval.vmin == otherinterval.vmin);
		assertTrue(interval.vmax == otherinterval.vmax);

		interval = envelope.queryInterval(VertexDescription.Semantics.ID, 0);
		otherinterval = multipoint.queryInterval(VertexDescription.Semantics.ID, 0);
		assertTrue(interval.vmin == otherinterval.vmin);
		assertTrue(interval.vmax == otherinterval.vmax);
	}

	@Test
	public static void testImportExportShapePoint() {
		OperatorExportToESRIShape exporterShape = (OperatorExportToESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToESRIShape);
		OperatorImportFromESRIShape importerShape = (OperatorImportFromESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromESRIShape);

		// Point
		Point point = makePoint();

		// Test Import Point from Point
		ByteBuffer pointShapeBuffer = exporterShape.execute(0, point);
		Point pointShapeGeometry = (Point) importerShape.execute(0, Geometry.Type.Point, pointShapeBuffer);

		double x1 = point.getX();
		double x2 = pointShapeGeometry.getX();
		assertTrue(x1 == x2);

		double y1 = point.getY();
		double y2 = pointShapeGeometry.getY();
		assertTrue(y1 == y2);

		double z1 = point.getZ();
		double z2 = pointShapeGeometry.getZ();
		assertTrue(z1 == z2);

		double m1 = point.getM();
		double m2 = pointShapeGeometry.getM();
		assertTrue(m1 == m2);

		int id1 = point.getID();
		int id2 = pointShapeGeometry.getID();
		assertTrue(id1 == id2);

		// Test Import Multipoint from Point
		MultiPoint multipointShapeGeometry = (MultiPoint) importerShape.execute(0, Geometry.Type.MultiPoint, pointShapeBuffer);
		Point point2d = multipointShapeGeometry.getPoint(0);
		assertTrue(x1 == point2d.getX() && y1 == point2d.getY());

		int pointCount = multipointShapeGeometry.getPointCount();
		assertTrue(pointCount == 1);

		z2 = multipointShapeGeometry.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0);
		assertTrue(z1 == z2);

		m2 = multipointShapeGeometry.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0);
		assertTrue(m1 == m2);

		id2 = multipointShapeGeometry.getAttributeAsInt(VertexDescription.Semantics.ID, 0, 0);
		assertTrue(id1 == id2);

		// Test Import Envelope from Point
		Geometry envelopeShapeGeometry = importerShape.execute(0, Geometry.Type.Envelope, pointShapeBuffer);
		Envelope envelope = (Envelope) envelopeShapeGeometry;

		Envelope env = new Envelope(), otherenv = new Envelope();
		envelope.queryEnvelope(env);
		point.queryEnvelope(otherenv);
		assertTrue(env.getXMin() == otherenv.getXMin());
		assertTrue(env.getXMax() == otherenv.getXMax());
		assertTrue(env.getYMin() == otherenv.getYMin());
		assertTrue(env.getYMax() == otherenv.getYMax());

		Envelope1D interval, otherinterval;
		interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		otherinterval = point.queryInterval(VertexDescription.Semantics.Z, 0);
		assertTrue(interval.vmin == otherinterval.vmin);
		assertTrue(interval.vmax == otherinterval.vmax);

		interval = envelope.queryInterval(VertexDescription.Semantics.ID, 0);
		otherinterval = point.queryInterval(VertexDescription.Semantics.ID, 0);
		assertTrue(interval.vmin == otherinterval.vmin);
		assertTrue(interval.vmax == otherinterval.vmax);
	}

	@Test
	public static void testImportExportShapeEnvelope() {
		OperatorExportToESRIShape exporterShape = (OperatorExportToESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToESRIShape);
		OperatorImportFromESRIShape importerShape = (OperatorImportFromESRIShape) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromESRIShape);

		// Test Export Envelope to Polygon
		Envelope envelope = makeEnvelope();

		ByteBuffer polygonShapeBuffer = exporterShape.execute(0, envelope);
		Polygon polygon = (Polygon) importerShape.execute(0, Geometry.Type.Polygon, polygonShapeBuffer);
		int pointCount = polygon.getPointCount();
		assertTrue(pointCount == 4);

		Envelope env = new Envelope();

		envelope.queryEnvelope(env);
		// interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		Point point3d;
		point3d = polygon.getPoint(0);
		assertTrue(point3d.getX() == env.getXMin() && point3d.getY() == env.getYMin());// && point3d.z ==
		// interval.vmin);
		point3d = polygon.getPoint(1);
		assertTrue(point3d.getX() == env.getXMin() && point3d.getY() == env.getYMax());// && point3d.z ==
		// interval.vmax);
		point3d = polygon.getPoint(2);
		assertTrue(point3d.getX() == env.getXMax() && point3d.getY() == env.getYMax());// && point3d.z ==
		// interval.vmin);
		point3d = polygon.getPoint(3);
		assertTrue(point3d.getX() == env.getXMax() && point3d.getY() == env.getYMin());// && point3d.z ==
		// interval.vmax);

		Envelope1D interval;
		interval = envelope.queryInterval(VertexDescription.Semantics.M, 0);
		double m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0);
		assertTrue(m == interval.vmin);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0);
		assertTrue(m == interval.vmax);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0);
		assertTrue(m == interval.vmin);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0);
		assertTrue(m == interval.vmax);

		interval = envelope.queryInterval(VertexDescription.Semantics.ID, 0);
		double id = polygon.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0);
		assertTrue(id == interval.vmin);
		id = polygon.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0);
		assertTrue(id == interval.vmax);
		id = polygon.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0);
		assertTrue(id == interval.vmin);
		id = polygon.getAttributeAsDbl(VertexDescription.Semantics.ID, 3, 0);
		assertTrue(id == interval.vmax);
	}

	@Test
	public static void testImportExportWkbGeometryCollection() {
		OperatorImportFromWkb importerWKB = (OperatorImportFromWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkb);

		int offset = 0;
		ByteBuffer wkbBuffer = ByteBuffer.allocate(600).order(ByteOrder.nativeOrder());
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbGeometryCollection);
		offset += 4; // type
		wkbBuffer.putInt(offset, 3); // 3 geometries
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbPoint);
		offset += 4;
		wkbBuffer.putDouble(offset, 0);
		offset += 8;
		wkbBuffer.putDouble(offset, 0);
		offset += 8;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbGeometryCollection);
		offset += 4; // type
		wkbBuffer.putInt(offset, 7); // 7 empty geometries
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbLineString);
		offset += 4;
		wkbBuffer.putInt(offset, 0); // 0 points, for empty linestring
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbPolygon);
		offset += 4;
		wkbBuffer.putInt(offset, 0); // 0 points, for empty polygon
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbMultiPolygon);
		offset += 4;
		wkbBuffer.putInt(offset, 0); // 0 points, for empty multipolygon
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbMultiLineString);
		offset += 4;
		wkbBuffer.putInt(offset, 0); // 0 points, for empty multilinestring
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbGeometryCollection);
		offset += 4;
		wkbBuffer.putInt(offset, 0); // 0 geometries, for empty
		// geometrycollection
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbMultiPoint);
		offset += 4;
		wkbBuffer.putInt(offset, 0); // 0 points, for empty multipoint
		offset += 4;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbPoint);
		offset += 4;
		wkbBuffer.putDouble(offset, 66);
		offset += 8;
		wkbBuffer.putDouble(offset, 88);
		offset += 8;
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1;
		wkbBuffer.putInt(offset, WkbGeometryType.wkbPoint);
		offset += 4;
		wkbBuffer.putDouble(offset, 13);
		offset += 8;
		wkbBuffer.putDouble(offset, 17);
		offset += 8;

		// "GeometryCollection( Point (0 0),  GeometryCollection( LineString empty, Polygon empty, MultiPolygon empty, MultiLineString empty, MultiPoint empty ), Point (13 17) )";
		OGCStructure structure = importerWKB.executeOGC(0, wkbBuffer, null).m_structures.get(0);

		assertTrue(structure.m_type == 7);
		assertTrue(structure.m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_type == 7);
		assertTrue(structure.m_structures.get(2).m_type == 1);

		assertTrue(structure.m_structures.get(1).m_structures.get(0).m_type == 2);
		assertTrue(structure.m_structures.get(1).m_structures.get(1).m_type == 3);
		assertTrue(structure.m_structures.get(1).m_structures.get(2).m_type == 6);
		assertTrue(structure.m_structures.get(1).m_structures.get(3).m_type == 5);
		assertTrue(structure.m_structures.get(1).m_structures.get(4).m_type == 7);
		assertTrue(structure.m_structures.get(1).m_structures.get(5).m_type == 4);
		assertTrue(structure.m_structures.get(1).m_structures.get(6).m_type == 1);

		Point p = (Point) structure.m_structures.get(1).m_structures.get(6).m_geometry;
		assertTrue(p.getX() == 66);
		assertTrue(p.getY() == 88);

		p = (Point) structure.m_structures.get(2).m_geometry;
		assertTrue(p.getX() == 13);
		assertTrue(p.getY() == 17);
	}

	@Test
	public static void testImportExportWKBPolygon() {
		OperatorExportToWkb exporterWKB = (OperatorExportToWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkb);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);
		OperatorImportFromWkb importerWKB = (OperatorImportFromWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkb);

		// Test Import Polygon with bad rings
		int offset = 0;
		ByteBuffer wkbBuffer = ByteBuffer.allocate(500).order(ByteOrder.nativeOrder());
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbPolygon);
		offset += 4; // type
		wkbBuffer.putInt(offset, 8);
		offset += 4; // num rings
		wkbBuffer.putInt(offset, 4);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 0.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 0.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 0.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 10.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 10.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 10.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 0.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 0.0);
		offset += 8; // y
		wkbBuffer.putInt(offset, 1);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 36.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 17.0);
		offset += 8; // y
		wkbBuffer.putInt(offset, 2);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 19.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 19.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, -19.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, -19.0);
		offset += 8; // y
		wkbBuffer.putInt(offset, 4);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 23.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 13.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 43.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 59.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 79.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 83.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 87.0);
		offset += 8; // y
		wkbBuffer.putInt(offset, 3);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 23.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 43.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 67.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 79.0);
		offset += 8; // y
		wkbBuffer.putInt(offset, 0);
		offset += 4; // num points
		wkbBuffer.putInt(offset, 3);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 23.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 43.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 67.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // y
		wkbBuffer.putInt(offset, 2);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 23.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 67.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 43.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 67.0);
		offset += 8; // y

		Geometry p = importerWKB.execute(0, Geometry.Type.Polygon, wkbBuffer, null);
		int pc = ((Polygon) p).getPathCount();
		String wktString = exporterWKT.execute(0, p, null);
		assertTrue(wktString.equals("MULTIPOLYGON (((0 0, 10 10, 0 10, 0 0), (36 17, 36 17, 36 17), (19 19, -19 -19, 19 19), (23 88, 83 87, 59 79, 13 43, 23 88), (23 88, 67 79, 88 43, 23 88), (23 88, 67 88, 88 43, 23 88), (23 67, 43 67, 23 67)))"));

		wktString = exporterWKT.execute(WktExportFlags.wktExportPolygon, p, null);
		assertTrue(wktString.equals("POLYGON ((0 0, 10 10, 0 10, 0 0), (36 17, 36 17, 36 17), (19 19, -19 -19, 19 19), (23 88, 83 87, 59 79, 13 43, 23 88), (23 88, 67 79, 88 43, 23 88), (23 88, 67 88, 88 43, 23 88), (23 67, 43 67, 23 67))"));

		Polygon polygon = makePolygon();

		// Test Import Polygon from Polygon8
		ByteBuffer polygonWKBBuffer = exporterWKB.execute(0, polygon, null);
		int wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPolygonZM);
		Geometry polygonWKBGeometry = importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null);
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) polygonWKBGeometry, polygon);

		// Test WKB_export_multi_polygon on nonempty single part polygon
		Polygon polygon2 = makePolygon2();
		assertTrue(polygon2.getPathCount() == 1);
		polygonWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportMultiPolygon, polygon2, null);
		polygonWKBGeometry = importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null);
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) polygonWKBGeometry, polygon2);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPolygonZM);

		// Test WKB_export_polygon on nonempty single part polygon
		assertTrue(polygon2.getPathCount() == 1);
		polygonWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPolygon, polygon2, null);
		polygonWKBGeometry = importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null);
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) polygonWKBGeometry, polygon2);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPolygonZM);

		// Test WKB_export_polygon on empty polygon
		Polygon polygon3 = new Polygon();
		polygonWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPolygon, polygon3, null);
		polygonWKBGeometry = importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null);
		assertTrue(polygonWKBGeometry.isEmpty() == true);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPolygon);

		// Test WKB_export_defaults on empty polygon
		polygonWKBBuffer = exporterWKB.execute(0, polygon3, null);
		polygonWKBGeometry = importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null);
		assertTrue(polygonWKBGeometry.isEmpty() == true);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPolygon);
	}

	@Test
	public static void testImportExportWKBPolyline() {
		OperatorExportToWkb exporterWKB = (OperatorExportToWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkb);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);
		OperatorImportFromWkb importerWKB = (OperatorImportFromWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkb);

		// Test Import Polyline with bad paths (i.e. paths with one point or
		// zero points)
		int offset = 0;
		ByteBuffer wkbBuffer = ByteBuffer.allocate(500).order(ByteOrder.nativeOrder());
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbMultiLineString);
		offset += 4; // type
		wkbBuffer.putInt(offset, 4);
		offset += 4; // num paths
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbLineString);
		offset += 4; // type
		wkbBuffer.putInt(offset, 1);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 36.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 17.0);
		offset += 8; // y
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbLineString);
		offset += 4; // type
		wkbBuffer.putInt(offset, 0);
		offset += 4; // num points
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbLineString);
		offset += 4; // type
		wkbBuffer.putInt(offset, 1);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 19.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 19.0);
		offset += 8; // y
		wkbBuffer.put(offset, (byte) WkbByteOrder.wkbNDR);
		offset += 1; // byte order
		wkbBuffer.putInt(offset, WkbGeometryType.wkbLineString);
		offset += 4; // type
		wkbBuffer.putInt(offset, 3);
		offset += 4; // num points
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 29.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 13.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 43.0);
		offset += 8; // y
		wkbBuffer.putDouble(offset, 59.0);
		offset += 8; // x
		wkbBuffer.putDouble(offset, 88);
		offset += 8; // y

		Polyline p = (Polyline) (importerWKB.execute(0, Geometry.Type.Polyline, wkbBuffer, null));
		int pc = p.getPointCount();
		int pac = p.getPathCount();
		assertTrue(p.getPointCount() == 7);
		assertTrue(p.getPathCount() == 3);

		String wktString = exporterWKT.execute(0, p, null);
		assertTrue(wktString.equals("MULTILINESTRING ((36 17, 36 17), (19 19, 19 19), (88 29, 13 43, 59 88))"));

		Polyline polyline = makePolyline();
		polyline.dropAttribute(VertexDescription.Semantics.ID);

		// Test Import Polyline from Polyline
		ByteBuffer polylineWKBBuffer = exporterWKB.execute(0, polyline, null);
		int wkbType = polylineWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiLineStringZM);
		Geometry polylineWKBGeometry = importerWKB.execute(0, Geometry.Type.Polyline, polylineWKBBuffer, null);
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) polylineWKBGeometry, polyline);

		// Test wkbExportMultiPolyline on nonempty single part polyline
		Polyline polyline2 = makePolyline2();
		assertTrue(polyline2.getPathCount() == 1);
		polylineWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportMultiLineString, polyline2, null);
		polylineWKBGeometry = importerWKB.execute(0, Geometry.Type.Polyline, polylineWKBBuffer, null);
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) polylineWKBGeometry, polyline2);
		wkbType = polylineWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiLineStringZM);

		// Test wkbExportPolyline on nonempty single part polyline
		assertTrue(polyline2.getPathCount() == 1);
		polylineWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportLineString, polyline2, null);
		polylineWKBGeometry = importerWKB.execute(0, Geometry.Type.Polyline, polylineWKBBuffer, null);
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) polylineWKBGeometry, polyline2);
		wkbType = polylineWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbLineStringZM);

		// Test wkbExportPolyline on empty polyline
		Polyline polyline3 = new Polyline();
		polylineWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportLineString, polyline3, null);
		polylineWKBGeometry = importerWKB.execute(0, Geometry.Type.Polyline, polylineWKBBuffer, null);
		assertTrue(polylineWKBGeometry.isEmpty() == true);
		wkbType = polylineWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbLineString);

		// Test WKB_export_defaults on empty polyline
		polylineWKBBuffer = exporterWKB.execute(0, polyline3, null);
		polylineWKBGeometry = importerWKB.execute(0, Geometry.Type.Polyline, polylineWKBBuffer, null);
		assertTrue(polylineWKBGeometry.isEmpty() == true);
		wkbType = polylineWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiLineString);
	}

	@Test
	public static void testImportExportWKBMultiPoint() {
		OperatorExportToWkb exporterWKB = (OperatorExportToWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkb);
		OperatorImportFromWkb importerWKB = (OperatorImportFromWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkb);

		MultiPoint multipoint = makeMultiPoint();
		multipoint.dropAttribute(VertexDescription.Semantics.ID);

		// Test Import Multi_point from Multi_point
		ByteBuffer multipointWKBBuffer = exporterWKB.execute(0, multipoint, null);
		int wkbType = multipointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPointZ);
		MultiPoint multipointWKBGeometry = (MultiPoint) (importerWKB.execute(0, Geometry.Type.MultiPoint, multipointWKBBuffer, null));
		TestCommonMethods.compareGeometryContent((MultiVertexGeometry) multipointWKBGeometry, multipoint);

		// Test WKB_export_point on nonempty single point Multi_point
		MultiPoint multipoint2 = makeMultiPoint2();
		assertTrue(multipoint2.getPointCount() == 1);
		ByteBuffer pointWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPoint, multipoint2, null);
		Point pointWKBGeometry = (Point) (importerWKB.execute(0, Geometry.Type.Point, pointWKBBuffer, null));
		Point3D point3d, mpoint3d;
		point3d = pointWKBGeometry.getXYZ();
		mpoint3d = multipoint2.getXYZ(0);
		assertTrue(point3d.x == mpoint3d.x && point3d.y == mpoint3d.y && point3d.z == mpoint3d.z);
		wkbType = pointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPointZ);

		// Test WKB_export_point on empty Multi_point
		MultiPoint multipoint3 = new MultiPoint();
		pointWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPoint, multipoint3, null);
		pointWKBGeometry = (Point) (importerWKB.execute(0, Geometry.Type.Point, pointWKBBuffer, null));
		assertTrue(pointWKBGeometry.isEmpty() == true);
		wkbType = pointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPoint);

		// Test WKB_export_defaults on empty Multi_point
		multipointWKBBuffer = exporterWKB.execute(0, multipoint3, null);
		multipointWKBGeometry = (MultiPoint) (importerWKB.execute(0, Geometry.Type.MultiPoint, multipointWKBBuffer, null));
		assertTrue(multipointWKBGeometry.isEmpty() == true);
		wkbType = multipointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPoint);
	}

	@Test
	public static void testImportExportWKBPoint() {
		OperatorExportToWkb exporterWKB = (OperatorExportToWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkb);
		OperatorImportFromWkb importerWKB = (OperatorImportFromWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkb);

		// Point
		Point point = makePoint();

		// Test Import Point from Point
		ByteBuffer pointWKBBuffer = exporterWKB.execute(0, point, null);
		int wkbType = pointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPointZM);
		Point pointWKBGeometry = (Point) (importerWKB.execute(0, Geometry.Type.Point, pointWKBBuffer, null));

		double x_1 = point.getX();
		double x2 = pointWKBGeometry.getX();
		assertTrue(x_1 == x2);

		double y1 = point.getY();
		double y2 = pointWKBGeometry.getY();
		assertTrue(y1 == y2);

		double z_1 = point.getZ();
		double z_2 = pointWKBGeometry.getZ();
		assertTrue(z_1 == z_2);

		double m1 = point.getM();
		double m2 = pointWKBGeometry.getM();
		assertTrue(m1 == m2);

		// Test WKB_export_defaults on empty point
		Point point2 = new Point();
		pointWKBBuffer = exporterWKB.execute(0, point2, null);
		pointWKBGeometry = (Point) (importerWKB.execute(0, Geometry.Type.Point, pointWKBBuffer, null));
		assertTrue(pointWKBGeometry.isEmpty() == true);
		wkbType = pointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPoint);

		// Test WKB_export_point on empty point
		pointWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPoint, point2, null);
		pointWKBGeometry = (Point) (importerWKB.execute(0, Geometry.Type.Point, pointWKBBuffer, null));
		assertTrue(pointWKBGeometry.isEmpty() == true);
		wkbType = pointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPoint);

		// Test WKB_export_multi_point on empty point
		MultiPoint multipoint = new MultiPoint();
		ByteBuffer multipointWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportMultiPoint, multipoint, null);
		MultiPoint multipointWKBGeometry = (MultiPoint) (importerWKB.execute(0, Geometry.Type.MultiPoint, multipointWKBBuffer, null));
		assertTrue(multipointWKBGeometry.isEmpty() == true);
		wkbType = multipointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPoint);

		// Test WKB_export_point on nonempty single point Multi_point
		MultiPoint multipoint2 = makeMultiPoint2();
		assertTrue(multipoint2.getPointCount() == 1);
		pointWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPoint, multipoint2, null);
		pointWKBGeometry = (Point) (importerWKB.execute(0, Geometry.Type.Point, pointWKBBuffer, null));
		Point3D point3d, mpoint3d;
		point3d = pointWKBGeometry.getXYZ();
		mpoint3d = multipoint2.getXYZ(0);
		assertTrue(point3d.x == mpoint3d.x && point3d.y == mpoint3d.y && point3d.z == mpoint3d.z);
		wkbType = pointWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPointZ);
	}

	@Test
	public static void testImportExportWKBEnvelope() {
		OperatorExportToWkb exporterWKB = (OperatorExportToWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkb);
		OperatorImportFromWkb importerWKB = (OperatorImportFromWkb) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkb);

		// Test Export Envelope to Polygon (WKB_export_defaults)
		Envelope envelope = makeEnvelope();
		envelope.dropAttribute(VertexDescription.Semantics.ID);

		ByteBuffer polygonWKBBuffer = exporterWKB.execute(0, envelope, null);
		int wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPolygonZM);
		Polygon polygon = (Polygon) (importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null));
		int point_count = polygon.getPointCount();
		assertTrue(point_count == 4);

		Envelope2D env = new Envelope2D();
		Envelope1D interval;

		envelope.queryEnvelope2D(env);
		interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		Point3D point3d;
		point3d = polygon.getXYZ(0);
		assertTrue(point3d.x == env.xmin && point3d.y == env.ymin && point3d.z == interval.vmin);
		point3d = polygon.getXYZ(1);
		assertTrue(point3d.x == env.xmin && point3d.y == env.ymax && point3d.z == interval.vmax);
		point3d = polygon.getXYZ(2);
		assertTrue(point3d.x == env.xmax && point3d.y == env.ymax && point3d.z == interval.vmin);
		point3d = polygon.getXYZ(3);
		assertTrue(point3d.x == env.xmax && point3d.y == env.ymin && point3d.z == interval.vmax);

		interval = envelope.queryInterval(VertexDescription.Semantics.M, 0);
		double m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0);
		assertTrue(m == interval.vmin);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0);
		assertTrue(m == interval.vmax);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0);
		assertTrue(m == interval.vmin);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0);
		assertTrue(m == interval.vmax);

		// Test WKB_export_multi_polygon on nonempty Envelope
		polygonWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportMultiPolygon, envelope, null);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbMultiPolygonZM);
		polygon = (Polygon) (importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null));
		point_count = polygon.getPointCount();
		assertTrue(point_count == 4);

		envelope.queryEnvelope2D(env);
		interval = envelope.queryInterval(VertexDescription.Semantics.Z, 0);
		point3d = polygon.getXYZ(0);
		assertTrue(point3d.x == env.xmin && point3d.y == env.ymin && point3d.z == interval.vmin);
		point3d = polygon.getXYZ(1);
		assertTrue(point3d.x == env.xmin && point3d.y == env.ymax && point3d.z == interval.vmax);
		point3d = polygon.getXYZ(2);
		assertTrue(point3d.x == env.xmax && point3d.y == env.ymax && point3d.z == interval.vmin);
		point3d = polygon.getXYZ(3);
		assertTrue(point3d.x == env.xmax && point3d.y == env.ymin && point3d.z == interval.vmax);

		interval = envelope.queryInterval(VertexDescription.Semantics.M, 0);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0);
		assertTrue(m == interval.vmin);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0);
		assertTrue(m == interval.vmax);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0);
		assertTrue(m == interval.vmin);
		m = polygon.getAttributeAsDbl(VertexDescription.Semantics.M, 3, 0);
		assertTrue(m == interval.vmax);

		// Test WKB_export_defaults on empty Envelope
		Envelope envelope2 = new Envelope();
		polygonWKBBuffer = exporterWKB.execute(0, envelope2, null);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPolygon);
		polygon = (Polygon) (importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null));
		assertTrue(polygon.isEmpty());

		// Test WKB_export_polygon on empty Envelope
		polygonWKBBuffer = exporterWKB.execute(WkbExportFlags.wkbExportPolygon, envelope2, null);
		wkbType = polygonWKBBuffer.getInt(1);
		assertTrue(wkbType == WkbGeometryType.wkbPolygon);
		polygon = (Polygon) (importerWKB.execute(0, Geometry.Type.Polygon, polygonWKBBuffer, null));
		assertTrue(polygon.isEmpty());
	}

	@Test
	public static void testImportExportWktGeometryCollection() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		String wktString;
		Envelope2D envelope = new Envelope2D();
		WktParser wktParser = new WktParser();

		wktString = "GeometryCollection( Point (0 0),  GeometryCollection( Point (0 0) ,  Point (1 1) , Point (2 2), LineString empty ), Point (1 1),  Point (2 2) )";
		OGCStructure structure = importerWKT.executeOGC(0, wktString, null).m_structures.get(0);

		assertTrue(structure.m_type == 7);
		assertTrue(structure.m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_type == 7);
		assertTrue(structure.m_structures.get(2).m_type == 1);
		assertTrue(structure.m_structures.get(3).m_type == 1);

		assertTrue(structure.m_structures.get(1).m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_structures.get(1).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_structures.get(2).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_structures.get(3).m_type == 2);
	}

	@Test
	public static void testImportExportWktMultiPolygon() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		Polygon polygon;
		String wktString;
		Envelope2D envelope = new Envelope2D();
		WktParser wktParser = new WktParser();

		// Test Import from MultiPolygon
		wktString = "Multipolygon M empty";
		polygon = (Polygon) importerWKT.execute(0, Geometry.Type.Polygon, wktString, null);
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.M));

		polygon = (Polygon) GeometryEngine.geometryFromWkt(wktString, 0, Geometry.Type.Unknown);
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.M));

		wktString = exporterWKT.execute(0, polygon, null);
		assertTrue(wktString.equals("MULTIPOLYGON M EMPTY"));

		wktString = GeometryEngine.geometryToWkt(polygon, 0);
		assertTrue(wktString.equals("MULTIPOLYGON M EMPTY"));

		wktString = "Multipolygon Z (empty, (empty, (10 10 5, 20 10 5, 20 20 5, 10 20 5, 10 10 5), (12 12 3), empty, (10 10 1, 12 12 1)), empty, ((90 90 88, 60 90 7, 60 60 7), empty, (70 70 7, 80 80 7, 70 80 7, 70 70 7)), empty)";
		polygon = (Polygon) (importerWKT.execute(0, Geometry.Type.Polygon, wktString, null));
		assertTrue(polygon != null);
		polygon.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 90 && envelope.ymin == 10 && envelope.ymax == 90);
		assertTrue(polygon.getPointCount() == 14);
		assertTrue(polygon.getPathCount() == 5);
		// assertTrue(polygon.calculate_area_2D() > 0.0);
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));

		double z = polygon.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0);
		assertTrue(z == 5);

		// Test Export to WKT MultiPolygon
		wktString = exporterWKT.execute(0, polygon, null);
		assertTrue(wktString.equals("MULTIPOLYGON Z (((10 10 5, 20 10 5, 20 20 5, 10 20 5, 10 10 5), (12 12 3, 12 12 3, 12 12 3), (10 10 1, 12 12 1, 10 10 1)), ((90 90 88, 60 90 7, 60 60 7, 90 90 88), (70 70 7, 70 80 7, 80 80 7, 70 70 7)))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		// Test import Polygon
		wktString = "POLYGON z (EMPTY, EMPTY, (10 10 5, 10 20 5, 20 20 5, 20 10 5), (12 12 3), EMPTY, (10 10 1, 12 12 1), EMPTY, (60 60 7, 60 90 7, 90 90 7, 60 60 7), EMPTY, (70 70 7, 70 80 7, 80 80 7), EMPTY)";
		polygon = (Polygon) (importerWKT.execute(0, Geometry.Type.Polygon, wktString, null));
		assertTrue(polygon != null);
		assertTrue(polygon.getPointCount() == 14);
		assertTrue(polygon.getPathCount() == 5);
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));

		// Test Export to WKT Polygon
		wktString = exporterWKT.execute(WktExportFlags.wktExportPolygon, polygon, null);
		assertTrue(wktString.equals("POLYGON Z ((10 10 5, 20 10 5, 20 20 5, 10 20 5, 10 10 5), (12 12 3, 12 12 3, 12 12 3), (10 10 1, 12 12 1, 10 10 1), (60 60 7, 60 90 7, 90 90 7, 60 60 7), (70 70 7, 70 80 7, 80 80 7, 70 70 7))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		Envelope env = new Envelope();
		env.addAttribute(VertexDescription.Semantics.Z);
		polygon.queryEnvelope(env);

		wktString = exporterWKT.execute(0, env, null);
		assertTrue(wktString.equals("POLYGON Z ((10 10 1, 90 10 7, 90 90 1, 10 90 7, 10 10 1))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = exporterWKT.execute(WktExportFlags.wktExportMultiPolygon, env, null);
		assertTrue(wktString.equals("MULTIPOLYGON Z (((10 10 1, 90 10 7, 90 90 1, 10 90 7, 10 10 1)))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		env.setEmpty();

		wktString = exporterWKT.execute(0, env, null);
		assertTrue(wktString.equals("POLYGON Z EMPTY"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = exporterWKT.execute(WktExportFlags.wktExportMultiPolygon, env, null);
		assertTrue(wktString.equals("MULTIPOLYGON Z EMPTY"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = "MULTIPOLYGON (((5 10, 8 10, 10 10, 10 0, 0 0, 0 10, 2 10, 5 10)))"; // ring
		// is
		// oriented
		// clockwise
		polygon = (Polygon) (importerWKT.execute(0, Geometry.Type.Polygon, wktString, null));
		assertTrue(polygon != null);
		assertTrue(polygon.calculateArea2D() > 0);

		wktString = "MULTIPOLYGON Z (((90 10 7, 10 10 1, 10 90 7, 90 90 1, 90 10 7)))"; // ring
		// is
		// oriented
		// clockwise
		polygon = (Polygon) (importerWKT.execute(0, Geometry.Type.Polygon, wktString, null));
		assertTrue(polygon != null);
		assertTrue(polygon.getPointCount() == 4);
		assertTrue(polygon.getPathCount() == 1);
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(polygon.calculateArea2D() > 0);

		wktString = exporterWKT.execute(WktExportFlags.wktExportMultiPolygon, polygon, null);
		assertTrue(wktString.equals("MULTIPOLYGON Z (((90 10 7, 90 90 1, 10 90 7, 10 10 1, 90 10 7)))"));
	}

	@Test
	public static void testImportExportWktPolygon() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		// OperatorExportToWkt exporterWKT =
		// (OperatorExportToWkt)OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		Polygon polygon;
		String wktString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from Polygon
		wktString = "Polygon ZM empty";
		polygon = (Polygon) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.M));

		wktString = "Polygon z (empty, (10 10 5, 20 10 5, 20 20 5, 10 20 5, 10 10 5), (12 12 3), empty, (10 10 1, 12 12 1))";
		polygon = (Polygon) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polygon != null);
		polygon.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 20 && envelope.ymin == 10 && envelope.ymax == 20);
		assertTrue(polygon.getPointCount() == 8);
		assertTrue(polygon.getPathCount() == 3);
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));

		wktString = "polygon ((35 10, 10 20, 15 40, 45 45, 35 10), (20 30, 35 35, 30 20, 20 30))";
		Polygon polygon2 = (Polygon) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polygon2 != null);

		// wktString = exporterWKT.execute(0, *polygon2, null);
	}

	@Test
	public static void testImportExportWktLineString() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		// OperatorExportToWkt exporterWKT =
		// (OperatorExportToWkt)OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		Polyline polyline;
		String wktString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from LineString
		wktString = "LineString ZM empty";
		polyline = (Polyline) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polyline != null);
		assertTrue(polyline.isEmpty());
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.M));

		wktString = "LineString m (10 10 5, 10 20 5, 20 20 5, 20 10 5)";
		polyline = (Polyline) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polyline != null);
		polyline.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 20 && envelope.ymin == 10 && envelope.ymax == 20);
		assertTrue(polyline.getPointCount() == 4);
		assertTrue(polyline.getPathCount() == 1);
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.M));
	}

	@Test
	public static void testImportExportWktMultiLineString() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		Polyline polyline;
		String wktString;
		Envelope2D envelope = new Envelope2D();
		WktParser wktParser = new WktParser();

		// Test Import from MultiLineString
		wktString = "MultiLineStringZMempty";
		polyline = (Polyline) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polyline != null);
		assertTrue(polyline.isEmpty());
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.M));

		wktString = "MultiLineStringm(empty, empty, (10 10 5, 10 20 5, 20 88 5, 20 10 5), (12 88 3), empty, (10 10 1, 12 12 1), empty, (88 60 7, 60 90 7, 90 90 7), empty, (70 70 7, 70 80 7, 80 80 7), empty)";
		polyline = (Polyline) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polyline != null);
		polyline.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 90 && envelope.ymin == 10 && envelope.ymax == 90);
		assertTrue(polyline.getPointCount() == 14);
		assertTrue(polyline.getPathCount() == 5);
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.M));

		wktString = exporterWKT.execute(0, polyline, null);
		assertTrue(wktString.equals("MULTILINESTRING M ((10 10 5, 10 20 5, 20 88 5, 20 10 5), (12 88 3, 12 88 3), (10 10 1, 12 12 1), (88 60 7, 60 90 7, 90 90 7), (70 70 7, 70 80 7, 80 80 7))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		// Test Import LineString
		wktString = "Linestring Z(10 10 5, 10 20 5, 20 20 5, 20 10 5)";
		polyline = (Polyline) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(polyline.getPointCount() == 4);
		wktString = exporterWKT.execute(WktExportFlags.wktExportLineString, polyline, null);
		assertTrue(wktString.equals("LINESTRING Z (10 10 5, 10 20 5, 20 20 5, 20 10 5)"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = exporterWKT.execute(0, polyline, null);
		assertTrue(wktString.equals("MULTILINESTRING Z ((10 10 5, 10 20 5, 20 20 5, 20 10 5))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}
	}

	@Test
	public static void testImportExportWktMultiPoint() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		MultiPoint multipoint;
		String wktString;
		Envelope2D envelope = new Envelope2D();
		WktParser wktParser = new WktParser();

		// Test Import from Multi_point
		wktString = "  MultiPoint ZM empty";
		multipoint = (MultiPoint) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(multipoint != null);
		assertTrue(multipoint.isEmpty());
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.M));

		wktString = exporterWKT.execute(0, multipoint, null);
		assertTrue(wktString.equals("MULTIPOINT ZM EMPTY"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = exporterWKT.execute(WktExportFlags.wktExportPoint, multipoint, null);
		assertTrue(wktString.equals("POINT ZM EMPTY"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		multipoint = new MultiPoint();
		multipoint.add(118.15114354234563, 33.82234433423462345);
		multipoint.add(88, 88);

		wktString = exporterWKT.execute(WktExportFlags.wktExportPrecision10, multipoint, null);
		assertTrue(wktString.equals("MULTIPOINT ((118.1511435 33.82234433), (88 88))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		multipoint = new MultiPoint();
		multipoint.add(88, 2);
		multipoint.add(88, 88);

		wktString = exporterWKT.execute(0, multipoint, null);
		assertTrue(wktString.equals("MULTIPOINT ((88 2), (88 88))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = "Multipoint zm (empty, empty, (10 88 88 33), (10 20 5 33), (20 20 5 33), (20 10 5 33), (12 12 3 33), empty, (10 10 1 33), (12 12 1 33), empty, (60 60 7 33), (60 90.1 7 33), (90 90 7 33), empty, (70 70 7 33), (70 80 7 33), (80 80 7 33), empty)";
		multipoint = (MultiPoint) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(multipoint != null);
		multipoint.queryEnvelope2D(envelope);
		// assertTrue(envelope.xmin == 10 && envelope.xmax == 90 &&
		// envelope.ymin == 10 && Math.abs(envelope.ymax - 90.1) <= 0.001);
		assertTrue(multipoint.getPointCount() == 13);
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.M));

		wktString = "Multipoint zm (10 88 88 33, 10 20 5 33, 20 20 5 33, 20 10 5 33, 12 12 3 33, 10 10 1 33, 12 12 1 33, 60 60 7 33, 60 90.1 7 33, 90 90 7 33, 70 70 7 33, 70 80 7 33, 80 80 7 33)";
		multipoint = (MultiPoint) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(multipoint != null);
		// assertTrue(envelope.xmin == 10 && envelope.xmax == 90 &&
		// envelope.ymin == 10 && ::fabs(envelope.ymax - 90.1) <= 0.001);
		assertTrue(multipoint.getPointCount() == 13);
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.M));

		wktString = exporterWKT.execute(WktExportFlags.wktExportPrecision15, multipoint, null);
		assertTrue(wktString.equals("MULTIPOINT ZM ((10 88 88 33), (10 20 5 33), (20 20 5 33), (20 10 5 33), (12 12 3 33), (10 10 1 33), (12 12 1 33), (60 60 7 33), (60 90.1 7 33), (90 90 7 33), (70 70 7 33), (70 80 7 33), (80 80 7 33))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = "Multipoint zm (empty, empty, (10 10 5 33))";
		multipoint = (MultiPoint) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));

		wktString = exporterWKT.execute(WktExportFlags.wktExportPoint, multipoint, null);
		assertTrue(wktString.equals("POINT ZM (10 10 5 33)"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}
	}

	@Test
	public static void testImportExportWktPoint() {
		OperatorImportFromWkt importerWKT = (OperatorImportFromWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromWkt);
		OperatorExportToWkt exporterWKT = (OperatorExportToWkt) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToWkt);

		Point point;
		String wktString;
		WktParser wktParser = new WktParser();

		// Test Import from Point
		wktString = "Point ZM empty";
		point = (Point) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(point != null);
		assertTrue(point.isEmpty());
		assertTrue(point.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(point.hasAttribute(VertexDescription.Semantics.M));

		wktString = exporterWKT.execute(0, point, null);
		assertTrue(wktString.equals("POINT ZM EMPTY"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = exporterWKT.execute(WktExportFlags.wktExportMultiPoint, point, null);
		assertTrue(wktString.equals("MULTIPOINT ZM EMPTY"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = "Point zm (30.1 10.6 5.1 33.1)";
		point = (Point) (importerWKT.execute(0, Geometry.Type.Unknown, wktString, null));
		assertTrue(point != null);
		assertTrue(point.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(point.hasAttribute(VertexDescription.Semantics.M));
		double x = point.getX();
		double y = point.getY();
		double z = point.getZ();
		double m = point.getM();

		assertTrue(x == 30.1);
		assertTrue(y == 10.6);
		assertTrue(z == 5.1);
		assertTrue(m == 33.1);

		wktString = exporterWKT.execute(WktExportFlags.wktExportPrecision15, point, null);
		assertTrue(wktString.equals("POINT ZM (30.1 10.6 5.1 33.1)"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}

		wktString = exporterWKT.execute(WktExportFlags.wktExportMultiPoint | WktExportFlags.wktExportPrecision15, point, null);
		assertTrue(wktString.equals("MULTIPOINT ZM ((30.1 10.6 5.1 33.1))"));
		wktParser.resetParser(wktString);
		while (wktParser.nextToken() != WktParser.WktToken.not_available) {
		}
	}

	@Deprecated
	@Test
	public static void testImportGeoJsonGeometryCollection() {
		OperatorImportFromGeoJson importer = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);

		String geoJsonString;
		Envelope2D envelope = new Envelope2D();
		WktParser wktParser = new WktParser();

		geoJsonString = "{\"type\" : \"GeometryCollection\", \"geometries\" : [{\"type\" : \"Point\", \"coordinates\": [0,0]},  {\"type\" : \"GeometryCollection\" , \"geometries\" : [ {\"type\" : \"Point\", \"coordinates\" : [0, 0]} ,  {\"type\" : \"Point\", \"coordinates\" : [1, 1]} ,{ \"type\" : \"Point\", \"coordinates\" : [2, 2]}, {\"type\" : \"LineString\", \"coordinates\" :  []}]} , {\"type\" : \"Point\", \"coordinates\" : [1, 1]},  {\"type\" : \"Point\" , \"coordinates\" : [2, 2]} ] }";
		OGCStructure structure = importer.executeOGC(0, geoJsonString, null).m_ogcStructure.m_structures.get(0);

		assertTrue(structure.m_type == 7);
		assertTrue(structure.m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_type == 7);
		assertTrue(structure.m_structures.get(2).m_type == 1);
		assertTrue(structure.m_structures.get(3).m_type == 1);

		assertTrue(structure.m_structures.get(1).m_structures.get(0).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_structures.get(1).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_structures.get(2).m_type == 1);
		assertTrue(structure.m_structures.get(1).m_structures.get(3).m_type == 2);
	}

	@Test
	public static void testImportGeoJsonMultiPolygon() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);
		OperatorExportToGeoJson exporterGeoJson = (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToGeoJson);

		MapGeometry map_geometry;
		Polygon polygon;
		SpatialReference spatial_reference;
		String geoJsonString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from MultiPolygon
		geoJsonString = "{\"type\": \"MultiPolygon\", \"coordinates\": []}";
		polygon = (Polygon) (importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null).getGeometry());
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(!polygon.hasAttribute(VertexDescription.Semantics.M));

		geoJsonString = "{\"coordinates\" : [], \"type\": \"MultiPolygon\", \"crs\": {\"type\": \"name\", \"some\": \"stuff\", \"properties\": {\"some\" : \"stuff\", \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(spatial_reference.getLatestID() == 4326);

		geoJsonString = "{\"coordinates\" : null, \"crs\": null, \"type\": \"MultiPolygon\"}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(spatial_reference == null);

		geoJsonString = "{\"type\": \"MultiPolygon\", \"coordinates\" : [[], [], [[[]]]], \"crsURN\": \"urn:ogc:def:crs:OGC:1.3:CRS27\"}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(spatial_reference != null);
		assertTrue(spatial_reference.getLatestID() == 4267);

		geoJsonString = "{\"coordinates\" : [[], [[], [[10, 10, 5], [20, 10, 5], [20, 20, 5], [10, 20, 5], [10, 10, 5]], [[12, 12, 3]], [], [[10, 10, 1], [12, 12, 1]]], [], [[[90, 90, 88], [60, 90, 7], [60, 60, 7]], [], [[70, 70, 7], [80, 80, 7], [70, 80, 7], [70, 70, 7]]], []], \"crs\": {\"type\": \"link\", \"properties\": {\"href\": \"http://spatialreference.org/ref/sr-org/6928/ogcwkt/\"}}, \"type\": \"MultiPolygon\"}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polygon != null);
		polygon.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 90 && envelope.ymin == 10 && envelope.ymax == 90);
		assertTrue(polygon.getPointCount() == 14);
		assertTrue(polygon.getPathCount() == 5);
		assertTrue(spatial_reference.getLatestID() == 3857);

		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polygon != null);
		polygon.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 90 && envelope.ymin == 10 && envelope.ymax == 90);
		assertTrue(polygon.getPointCount() == 14);
		assertTrue(polygon.getPathCount() == 5);
		assertTrue(spatial_reference.getLatestID() == 3857);

		// Test Export to GeoJSON MultiPolygon
		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportSkipCRS, spatial_reference, polygon);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPolygon\",\"coordinates\":[[[[10,10,5],[20,10,5],[20,20,5],[10,20,5],[10,10,5]],[[12,12,3],[12,12,3],[12,12,3]],[[10,10,1],[12,12,1],[10,10,1]]],[[[90,90,88],[60,90,7],[60,60,7],[90,90,88]],[[70,70,7],[70,80,7],[80,80,7],[70,70,7]]]]}"));

		geoJsonString = exporterGeoJson.execute(0, spatial_reference, polygon);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPolygon\",\"coordinates\":[[[[10,10,5],[20,10,5],[20,20,5],[10,20,5],[10,10,5]],[[12,12,3],[12,12,3],[12,12,3]],[[10,10,1],[12,12,1],[10,10,1]]],[[[90,90,88],[60,90,7],[60,60,7],[90,90,88]],[[70,70,7],[70,80,7],[80,80,7],[70,70,7]]]],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:3857\"}}}"));

		geoJsonString = "{\"type\": \"MultiPolygon\", \"coordinates\": [[[[90, 10, 7], [10, 10, 1], [10, 90, 7], [90, 90, 1], [90, 10, 7]]]] }"; // ring
		// i																																															// clockwise
		polygon = (Polygon) (importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null).getGeometry());
		assertTrue(polygon != null);
		assertTrue(polygon.getPointCount() == 4);
		assertTrue(polygon.getPathCount() == 1);
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(polygon.calculateArea2D() > 0);

		// Test import Polygon
		geoJsonString = "{\"type\": \"Polygon\", \"coordinates\": [[], [], [[10, 10, 5], [10, 20, 5], [20, 20, 5], [20, 10, 5]], [[12, 12, 3]], [], [[10, 10, 1], [12, 12, 1]], [], [[60, 60, 7], [60, 90, 7], [90, 90, 7], [60, 60, 7]], [], [[70, 70, 7], [70, 80, 7], [80, 80, 7]], []] }";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polygon != null);
		assertTrue(polygon.getPointCount() == 14);
		assertTrue(polygon.getPathCount() == 5);
		assertTrue(spatial_reference.getLatestID() == 4326);

		geoJsonString = exporterGeoJson.execute(0, spatial_reference, polygon);
		assertTrue(geoJsonString.equals("{\"type\":\"Polygon\",\"coordinates\":[[[10,10,5],[20,10,5],[20,20,5],[10,20,5],[10,10,5]],[[12,12,3],[12,12,3],[12,12,3]],[[10,10,1],[12,12,1],[10,10,1]],[[60,60,7],[60,90,7],[90,90,7],[60,60,7]],[[70,70,7],[70,80,7],[80,80,7],[70,70,7]]],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:4326\"}}}"));

		Envelope env = new Envelope();
		env.addAttribute(VertexDescription.Semantics.Z);
		polygon.queryEnvelope(env);

		geoJsonString = "{\"coordinates\" : [], \"type\": \"MultiPolygon\", \"crs\":{\"esriwkt\":\"PROJCS[\\\"Gnomonic\\\",GEOGCS[\\\"GCS_WGS_1984\\\",DATUM[\\\"D_WGS_1984\\\",SPHEROID[\\\"WGS_1984\\\",6378137.0,298.257223563]],PRIMEM[\\\"Greenwich\\\",0.0],UNIT[\\\"Degree\\\",0.0174532925199433]],PROJECTION[\\\"Gnomonic\\\"],PARAMETER[\\\"Longitude_Of_Center\\\",0.0],PARAMETER[\\\"Latitude_Of_Center\\\",-45.0],UNIT[\\\"Meter\\\",1.0]]\"}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		String wkt = spatial_reference.getText();
		assertTrue(wkt.equals(
				"PROJCS[\"Gnomonic\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Gnomonic\"],PARAMETER[\"Longitude_Of_Center\",0.0],PARAMETER[\"Latitude_Of_Center\",-45.0],UNIT[\"Meter\",1.0]]"));

		geoJsonString = "{\"coordinates\" : [], \"type\": \"MultiPolygon\", \"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"PROJCS[\\\"Gnomonic\\\",GEOGCS[\\\"GCS_WGS_1984\\\",DATUM[\\\"D_WGS_1984\\\",SPHEROID[\\\"WGS_1984\\\",6378137.0,298.257223563]],PRIMEM[\\\"Greenwich\\\",0.0],UNIT[\\\"Degree\\\",0.0174532925199433]],PROJECTION[\\\"Gnomonic\\\"],PARAMETER[\\\"Longitude_Of_Center\\\",0.0],PARAMETER[\\\"Latitude_Of_Center\\\",-45.0],UNIT[\\\"Meter\\\",1.0]]\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		wkt = spatial_reference.getText();
		assertTrue(wkt.equals(
				"PROJCS[\"Gnomonic\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Gnomonic\"],PARAMETER[\"Longitude_Of_Center\",0.0],PARAMETER[\"Latitude_Of_Center\",-45.0],UNIT[\"Meter\",1.0]]"));
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());

		// AGOL exports wkt like this...
		geoJsonString = "{\"coordinates\" : [], \"type\": \"MultiPolygon\", \"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"ESRI:PROJCS[\\\"Gnomonic\\\",GEOGCS[\\\"GCS_WGS_1984\\\",DATUM[\\\"D_WGS_1984\\\",SPHEROID[\\\"WGS_1984\\\",6378137.0,298.257223563]],PRIMEM[\\\"Greenwich\\\",0.0],UNIT[\\\"Degree\\\",0.0174532925199433]],PROJECTION[\\\"Gnomonic\\\"],PARAMETER[\\\"Longitude_Of_Center\\\",0.0],PARAMETER[\\\"Latitude_Of_Center\\\",-45.0],UNIT[\\\"Meter\\\",1.0]]\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Polygon, geoJsonString, null);
		polygon = (Polygon) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		wkt = spatial_reference.getText();
		assertTrue(wkt.equals(
				"PROJCS[\"Gnomonic\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Gnomonic\"],PARAMETER[\"Longitude_Of_Center\",0.0],PARAMETER[\"Latitude_Of_Center\",-45.0],UNIT[\"Meter\",1.0]]"));
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());

		boolean exceptionThrownNoWKT = false;

		try {
			geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPreferMultiGeometry,
					spatial_reference, polygon);
		} catch (Exception e) {
			exceptionThrownNoWKT = true;
		}

		assertTrue(exceptionThrownNoWKT);
	}

	@Test
	public static void testImportGeoJsonMultiLineString() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);
		OperatorExportToGeoJson exporterGeoJson = (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToGeoJson);
		MapGeometry map_geometry;
		Polyline polyline;
		SpatialReference spatial_reference;
		String geoJsonString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from MultiLineString
		geoJsonString = "{\"type\":\"MultiLineString\",\"coordinates\":[], \"crs\" : {\"type\" : \"URL\", \"properties\" : {\"url\" : \"http://www.opengis.net/def/crs/EPSG/0/3857\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		polyline = (Polyline) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polyline != null);
		assertTrue(spatial_reference != null);
		assertTrue(polyline.isEmpty());
		assertTrue(spatial_reference.getLatestID() == 3857);

		geoJsonString = "{\"crs\" : {\"type\" : \"link\", \"properties\" : {\"href\" : \"www.spatialreference.org/ref/epsg/4309/\"}}, \"type\":\"MultiLineString\",\"coordinates\":[[], [], [[10, 10, 5], [10, 20, 5], [20, 88, 5], [20, 10, 5]], [[12, 88, 3]], [], [[10, 10, 1], [12, 12, 1]], [], [[88, 60, 7], [60, 90, 7], [90, 90, 7]], [], [[70, 70, 7], [70, 80, 7], [80, 80, 7]], []]}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		polyline = (Polyline) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polyline != null);
		polyline.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 90 && envelope.ymin == 10 && envelope.ymax == 90);
		assertTrue(polyline.getPointCount() == 14);
		assertTrue(polyline.getPathCount() == 5);
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(spatial_reference.getLatestID() == 4309);

		geoJsonString = exporterGeoJson.execute(0, spatial_reference, polyline);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiLineString\",\"coordinates\":[[[10,10,5],[10,20,5],[20,88,5],[20,10,5]],[[12,88,3],[12,88,3]],[[10,10,1],[12,12,1]],[[88,60,7],[60,90,7],[90,90,7]],[[70,70,7],[70,80,7],[80,80,7]]],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:4309\"}}}"));

		// Test Import LineString
		geoJsonString = "{\"type\": \"LineString\", \"coordinates\": [[10, 10, 5], [10, 20, 5], [20, 20, 5], [20, 10, 5]]}";
		polyline = (Polyline) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polyline.getPointCount() == 4);
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.Z));

		geoJsonString = "{\"type\": \"LineString\", \"coordinates\": [[10, 10, 5], [10, 20, 5, 3], [20, 20, 5], [20, 10, 5]]}";
		polyline = (Polyline) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polyline.getPointCount() == 4);
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(polyline.hasAttribute(VertexDescription.Semantics.M));

		geoJsonString = "{\"type\":\"LineString\",\"coordinates\": [[10, 10, 5], [10, 20, 5], [20, 20, 5], [], [20, 10, 5]],\"crs\" : {\"type\" : \"link\", \"properties\" : {\"href\" : \"www.opengis.net/def/crs/EPSG/0/3857\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		polyline = (Polyline) (map_geometry.getGeometry());
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(polyline.getPointCount() == 4);
		assertTrue(spatial_reference.getLatestID() == 3857);
		geoJsonString = exporterGeoJson.execute(0, spatial_reference, polyline);
		assertTrue(geoJsonString.equals("{\"type\":\"LineString\",\"coordinates\":[[10,10,5],[10,20,5],[20,20,5],[20,10,5]],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:3857\"}}}"));

		geoJsonString = exporterGeoJson.execute(0, null, polyline);
		assertTrue(geoJsonString.equals("{\"type\":\"LineString\",\"coordinates\":[[10,10,5],[10,20,5],[20,20,5],[20,10,5]],\"crs\":null}"));
	}

	@Test
	public static void testImportGeoJsonMultiPoint() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);
		OperatorExportToGeoJson exporterGeoJson = (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToGeoJson);
		MapGeometry map_geometry;
		MultiPoint multipoint;
		SpatialReference spatial_reference;
		String geoJsonString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from Multi_point

		geoJsonString = "{\"type\":\"MultiPoint\",\"coordinates\":[]}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		multipoint = (MultiPoint) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(multipoint != null);
		assertTrue(multipoint.isEmpty());
		assertTrue(spatial_reference.getLatestID() == 4326);

		geoJsonString = exporterGeoJson.execute(0, null, multipoint);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[],\"crs\":null}"));

		multipoint = new MultiPoint();
		multipoint.add(118.15, 2);
		multipoint.add(88, 88);

		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPrecision16, SpatialReference.create(4269), multipoint);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[[118.15,2],[88,88]],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:4269\"}}}"));

		multipoint.setEmpty();
		multipoint.add(88, 2);
		multipoint.add(88, 88);

		geoJsonString = exporterGeoJson.execute(0, SpatialReference.create(102100), multipoint);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[[88,2],[88,88]],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:3857\"}}}"));

		geoJsonString = "{\"type\":\"MultiPoint\",\"coordinates\":[[], [], [10, 88, 88, 33], [10, 20, 5, 33], [20, 20, 5, 33], [20, 10, 5, 33], [12, 12, 3, 33], [], [10, 10, 1, 33], [12, 12, 1, 33], [], [60, 60, 7, 33], [60, 90.1, 7, 33], [90, 90, 7, 33], [], [70, 70, 7, 33], [70, 80, 7, 33], [80, 80, 7, 33], []],\"crs\":{\"type\":\"OGC\",\"properties\":{\"urn\":\"urn:ogc:def:crs:OGC:1.3:CRS83\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		multipoint = (MultiPoint) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(multipoint != null);
		assertTrue(multipoint.getPointCount() == 13);
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(spatial_reference.getLatestID() == 4269);

		geoJsonString = "{\"type\":\"MultiPoint\",\"coordinates\": [[10, 88, 88, 33], [10, 20, 5, 33], [20, 20, 5, 33], [], [20, 10, 5, 33], [12, 12, 3, 33], [], [10, 10, 1, 33], [12, 12, 1, 33], [60, 60, 7, 33], [60, 90.1, 7, 33], [90, 90, 7, 33], [70, 70, 7, 33], [70, 80, 7, 33], [80, 80, 7, 33]]}";
		multipoint = (MultiPoint) importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry();
		assertTrue(multipoint != null);
		assertTrue(multipoint.getPointCount() == 13);
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(multipoint.hasAttribute(VertexDescription.Semantics.M));

		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPrecision15, null, multipoint);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[[10,88,88,33],[10,20,5,33],[20,20,5,33],[20,10,5,33],[12,12,3,33],[10,10,1,33],[12,12,1,33],[60,60,7,33],[60,90.1,7,33],[90,90,7,33],[70,70,7,33],[70,80,7,33],[80,80,7,33]],\"crs\":null}"));

		geoJsonString = "{\"type\":\"MultiPoint\",\"coordinates\":[[], [], [10, 10, 5, 33]]}";
		multipoint = (MultiPoint) importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry();

		geoJsonString = exporterGeoJson.execute(0, null, multipoint);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[[10,10,5,33]],\"crs\":null}"));
	}

	@Test
	public static void testImportGeoJsonPolygon() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);

		Polygon polygon;
		String geoJsonString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from Polygon
		geoJsonString = "{\"type\": \"Polygon\", \"coordinates\": []}";
		polygon = (Polygon) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polygon != null);
		assertTrue(polygon.isEmpty());
		assertTrue(!polygon.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(!polygon.hasAttribute(VertexDescription.Semantics.M));

		geoJsonString = "{\"type\": \"Polygon\", \"coordinates\": [[], [[10, 10, 5], [20, 10, 5], [20, 20, 5], [10, 20, 5], [10, 10, 5]], [[12, 12, 3]], [], [[10, 10, 1], [12, 12, 1]]]}";
		polygon = (Polygon) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polygon != null);
		polygon.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 20 && envelope.ymin == 10 && envelope.ymax == 20);
		assertTrue(polygon.getPointCount() == 8);
		assertTrue(polygon.getPathCount() == 3);
		assertTrue(polygon.hasAttribute(VertexDescription.Semantics.Z));

		geoJsonString = "{\"type\": \"Polygon\", \"coordinates\": [[[35, 10], [10, 20], [15, 40], [45, 45], [35, 10]], [[20, 30], [35, 35], [30, 20], [20, 30]]]}";
		Polygon polygon2 = (Polygon) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polygon2 != null);
	}

	@Test
	public static void testImportGeoJsonLineString() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);

		Polyline polyline;
		String geoJsonString;
		Envelope2D envelope = new Envelope2D();

		// Test Import from LineString
		geoJsonString = "{\"type\": \"LineString\", \"coordinates\": []}";
		polyline = (Polyline) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polyline != null);
		assertTrue(polyline.isEmpty());
		assertTrue(!polyline.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(!polyline.hasAttribute(VertexDescription.Semantics.M));

		geoJsonString = "{\"type\": \"LineString\", \"coordinates\": [[10, 10, 5], [10, 20, 5], [20, 20, 5], [20, 10, 5]]}";
		polyline = (Polyline) (importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null).getGeometry());
		assertTrue(polyline != null);
		polyline.queryEnvelope2D(envelope);
		assertTrue(envelope.xmin == 10 && envelope.xmax == 20 && envelope.ymin == 10 && envelope.ymax == 20);
		assertTrue(polyline.getPointCount() == 4);
		assertTrue(polyline.getPathCount() == 1);
		assertTrue(!polyline.hasAttribute(VertexDescription.Semantics.M));
	}

	@Test
	public static void testImportGeoJsonPoint() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);
		OperatorExportToGeoJson exporterGeoJson = (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToGeoJson);
		MapGeometry map_geometry;
		SpatialReference spatial_reference;
		Point point;
		String geoJsonString;

		// Test Import from Point
		geoJsonString = "{\"type\":\"Point\",\"coordinates\":[],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:3857\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		point = (Point) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(spatial_reference.getLatestID() == 3857);

		assertTrue(point != null);
		assertTrue(point.isEmpty());

		geoJsonString = exporterGeoJson.execute(0, null, point);
		assertTrue(geoJsonString.equals("{\"type\":\"Point\",\"coordinates\":[],\"crs\":null}"));

		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPreferMultiGeometry, null, point);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[],\"crs\":null}"));

		geoJsonString = "{\"type\":\"Point\",\"coordinates\":[30.1,10.6,5.1,33.1],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"urn:ogc:def:crs:ESRI::54051\"}}}";
		map_geometry = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
		point = (Point) map_geometry.getGeometry();
		spatial_reference = map_geometry.getSpatialReference();
		assertTrue(point != null);
		assertTrue(point.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(point.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(spatial_reference.getLatestID() == 54051);
		double x = point.getX();
		double y = point.getY();
		double z = point.getZ();
		double m = point.getM();

		assertTrue(x == 30.1);
		assertTrue(y == 10.6);
		assertTrue(z == 5.1);
		assertTrue(m == 33.1);

		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPrecision15, spatial_reference, point);
		assertTrue(geoJsonString.equals("{\"type\":\"Point\",\"coordinates\":[30.1,10.6,5.1,33.1],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"ESRI:54051\"}}}"));

		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPrecision15, SpatialReference.create(4287), point);
		assertTrue(geoJsonString.equals("{\"type\":\"Point\",\"coordinates\":[30.1,10.6,5.1,33.1],\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:4287\"}}}"));

		geoJsonString = exporterGeoJson.execute(GeoJsonExportFlags.geoJsonExportPreferMultiGeometry | GeoJsonExportFlags.geoJsonExportPrecision15, null, point);
		assertTrue(geoJsonString.equals("{\"type\":\"MultiPoint\",\"coordinates\":[[30.1,10.6,5.1,33.1]],\"crs\":null}"));
	}

	@Test
	public static void testImportExportGeoJsonMalformed() {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);
		OperatorExportToGeoJson exporterGeoJson = (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ExportToGeoJson);

		String geoJsonString;

		try {
			geoJsonString = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[2,2,2],[3,3,3],[4,4,4],[2,2,2]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"Polygon\",\"coordinates\":[[2,2,2],[3,3,3],[4,4,4],[2,2,2]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"Polygon\",\"coordinates\":[[[2,2,2],[3,3,3],[4,4,4],[2,2,2]],2,4]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"MultiPoint\",\"coordinates\":[[[2,2,2],[3,3,3],[4,4,4],[2,2,2]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"LineString\",\"coordinates\":[[[2,2,2],[3,3,3],[4,4,4],[2,2,2]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"MultiPoint\",\"coordinates\":[[2,2,2],[3,3,3],[4,4,4],[2,2,2],[[]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[2,2,2],[3,3,3],[4,4,4],[2,2,2],[[]]]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[2,2,2],[3,3,3],[4,4,4],[2,2,2]],[1,1,1],[2,2,2],[3,3,3],[1,1,1]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"Polygon\",\"coordinates\":[[[2,2,2],[3,3,3],[4,4,4],[2,2,2]],[1,1,1],[2,2,2],[3,3,3],[1,1,1]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[[]]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[{}]]]}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}

		try {
			geoJsonString = "{\"type\":\"Point\",\"coordinates\":[30.1,10.6,[],33.1],\"crs\":\"EPSG:3857\"}";
			importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString, null);
			assertTrue(false);
		} catch (Exception e) {
		}
	}

	@Test
	public static void testImportGeoJsonSpatialReference() throws Exception {
		OperatorImportFromGeoJson importerGeoJson = (OperatorImportFromGeoJson) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.ImportFromGeoJson);

		String geoJsonString4326;
		String geoJsonString3857;

		// Test Import from Point
		geoJsonString4326 = "{\"type\": \"Point\", \"coordinates\": [3.0, 5.0], \"crs\": \"EPSG:4326\"}";
		geoJsonString3857 = "{\"type\": \"Point\", \"coordinates\": [3.0, 5.0], \"crs\": \"EPSG:3857\"}";

		MapGeometry mapGeometry4326 = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString4326, null);
		MapGeometry mapGeometry3857 = importerGeoJson.execute(0, Geometry.Type.Unknown, geoJsonString3857, null);

		assertTrue(mapGeometry4326.equals(mapGeometry3857) == false);
		assertTrue(mapGeometry4326.getGeometry().equals(mapGeometry3857.getGeometry()));
	}

	public static Polygon makePolygon() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		poly.startPath(3, 3);
		poly.lineTo(7, 3);
		poly.lineTo(7, 7);
		poly.lineTo(3, 7);

		poly.startPath(15, 0);
		poly.lineTo(15, 15);
		poly.lineTo(30, 15);
		poly.lineTo(30, 0);

		poly.setAttribute(VertexDescription.Semantics.Z, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.Z, 1, 0, 3);
		poly.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);
		poly.setAttribute(VertexDescription.Semantics.Z, 3, 0, 7);
		poly.setAttribute(VertexDescription.Semantics.Z, 4, 0, 11);
		poly.setAttribute(VertexDescription.Semantics.Z, 5, 0, 13);
		poly.setAttribute(VertexDescription.Semantics.Z, 6, 0, 17);
		poly.setAttribute(VertexDescription.Semantics.Z, 7, 0, 19);
		poly.setAttribute(VertexDescription.Semantics.Z, 8, 0, 23);
		poly.setAttribute(VertexDescription.Semantics.Z, 9, 0, 29);
		poly.setAttribute(VertexDescription.Semantics.Z, 10, 0, 31);
		poly.setAttribute(VertexDescription.Semantics.Z, 11, 0, 37);

		poly.setAttribute(VertexDescription.Semantics.M, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, 4);
		poly.setAttribute(VertexDescription.Semantics.M, 2, 0, 8);
		poly.setAttribute(VertexDescription.Semantics.M, 3, 0, 16);
		poly.setAttribute(VertexDescription.Semantics.M, 4, 0, 32);
		poly.setAttribute(VertexDescription.Semantics.M, 5, 0, 64);
		poly.setAttribute(VertexDescription.Semantics.M, 6, 0, 128);
		poly.setAttribute(VertexDescription.Semantics.M, 7, 0, 256);
		poly.setAttribute(VertexDescription.Semantics.M, 8, 0, 512);
		poly.setAttribute(VertexDescription.Semantics.M, 9, 0, 1024);
		poly.setAttribute(VertexDescription.Semantics.M, 10, 0, 2048);
		poly.setAttribute(VertexDescription.Semantics.M, 11, 0, 4096);

		return poly;
	}

	public static Polygon makePolygon2() {
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(0, 10);
		poly.lineTo(10, 10);
		poly.lineTo(10, 0);

		poly.setAttribute(VertexDescription.Semantics.Z, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.Z, 1, 0, 3);
		poly.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);
		poly.setAttribute(VertexDescription.Semantics.Z, 3, 0, 7);

		poly.setAttribute(VertexDescription.Semantics.M, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, 4);
		poly.setAttribute(VertexDescription.Semantics.M, 2, 0, 8);
		poly.setAttribute(VertexDescription.Semantics.M, 3, 0, 16);

		return poly;
	}

	public static Polyline makePolyline() {
		Polyline poly = new Polyline();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.startPath(20, 13);
		poly.lineTo(150, 120);
		poly.lineTo(300, 414);
		poly.lineTo(610, 14);

		poly.setAttribute(VertexDescription.Semantics.Z, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.Z, 1, 0, 3);
		poly.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);
		poly.setAttribute(VertexDescription.Semantics.Z, 3, 0, 7);
		poly.setAttribute(VertexDescription.Semantics.Z, 4, 0, 11);
		poly.setAttribute(VertexDescription.Semantics.Z, 5, 0, 13);
		poly.setAttribute(VertexDescription.Semantics.Z, 6, 0, 17);
		poly.setAttribute(VertexDescription.Semantics.Z, 7, 0, 19);

		poly.setAttribute(VertexDescription.Semantics.M, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, 4);
		poly.setAttribute(VertexDescription.Semantics.M, 2, 0, 8);
		poly.setAttribute(VertexDescription.Semantics.M, 3, 0, 16);
		poly.setAttribute(VertexDescription.Semantics.M, 4, 0, 32);
		poly.setAttribute(VertexDescription.Semantics.M, 5, 0, 64);
		poly.setAttribute(VertexDescription.Semantics.M, 6, 0, 128);
		poly.setAttribute(VertexDescription.Semantics.M, 7, 0, 256);

		poly.setAttribute(VertexDescription.Semantics.ID, 0, 0, 1);
		poly.setAttribute(VertexDescription.Semantics.ID, 1, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.ID, 2, 0, 3);
		poly.setAttribute(VertexDescription.Semantics.ID, 3, 0, 5);
		poly.setAttribute(VertexDescription.Semantics.ID, 4, 0, 8);
		poly.setAttribute(VertexDescription.Semantics.ID, 5, 0, 13);
		poly.setAttribute(VertexDescription.Semantics.ID, 6, 0, 21);
		poly.setAttribute(VertexDescription.Semantics.ID, 7, 0, 34);

		return poly;
	}

	public static Polyline makePolyline2() {
		Polyline poly = new Polyline();
		poly.startPath(10, 1);
		poly.lineTo(15, 20);
		poly.lineTo(30, 14);
		poly.lineTo(60, 144);

		poly.setAttribute(VertexDescription.Semantics.Z, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.Z, 1, 0, 3);
		poly.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);
		poly.setAttribute(VertexDescription.Semantics.Z, 3, 0, 7);

		poly.setAttribute(VertexDescription.Semantics.M, 0, 0, 2);
		poly.setAttribute(VertexDescription.Semantics.M, 1, 0, 4);
		poly.setAttribute(VertexDescription.Semantics.M, 2, 0, 8);
		poly.setAttribute(VertexDescription.Semantics.M, 3, 0, 16);

		return poly;
	}

	public static Point makePoint() {
		Point point = new Point();
		point.setXY(11, 13);

		point.setZ(32);
		point.setM(243);
		point.setID(1024);

		return point;
	}

	public static MultiPoint makeMultiPoint() {
		MultiPoint mpoint = new MultiPoint();
		Point pt1 = new Point();
		pt1.setXY(0, 0);
		pt1.setZ(-1);

		Point pt2 = new Point();
		pt2.setXY(0, 0);
		pt2.setZ(1);

		Point pt3 = new Point();
		pt3.setXY(0, 1);
		pt3.setZ(1);

		mpoint.add(pt1);
		mpoint.add(pt2);
		mpoint.add(pt3);

		mpoint.setAttribute(VertexDescription.Semantics.ID, 0, 0, 7);
		mpoint.setAttribute(VertexDescription.Semantics.ID, 1, 0, 11);
		mpoint.setAttribute(VertexDescription.Semantics.ID, 2, 0, 13);

		return mpoint;
	}

	public static MultiPoint makeMultiPoint2() {
		MultiPoint mpoint = new MultiPoint();
		Point pt1 = new Point();
		pt1.setX(0.0);
		pt1.setY(0.0);
		pt1.setZ(-1.0);

		mpoint.add(pt1);

		return mpoint;
	}

	public static Envelope makeEnvelope() {
		Envelope envelope;

		Envelope env = new Envelope(0.0, 0.0, 5.0, 5.0);
		envelope = env;

		Envelope1D interval = new Envelope1D();
		interval.vmin = -3.0;
		interval.vmax = -7.0;
		envelope.setInterval(VertexDescription.Semantics.Z, 0, interval);

		interval.vmin = 16.0;
		interval.vmax = 32.0;
		envelope.setInterval(VertexDescription.Semantics.M, 0, interval);

		interval.vmin = 5.0;
		interval.vmax = 11.0;
		envelope.setInterval(VertexDescription.Semantics.ID, 0, interval);

		return envelope;
	}
}
