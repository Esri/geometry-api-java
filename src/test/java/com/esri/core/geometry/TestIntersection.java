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

import com.esri.core.geometry.PolygonUtils.PiPResult;

//import java.util.Random;

public class TestIntersection extends TestCase {
	static OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
	static int codeIn = 26910;// NAD_1983_UTM_Zone_10N : GCS 6269
	static int codeOut = 32610;// WGS_1984_UTM_Zone_10N; : GCS 4326
	static SpatialReference inputSR;
	static SpatialReference outputSR;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		projEnv = OperatorFactoryLocal.getInstance();
		inputSR = SpatialReference.create(codeIn);
		outputSR = SpatialReference.create(codeOut);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testIntersection1() {
		// OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
		// int codeIn = 26910;//NAD_1983_UTM_Zone_10N : GCS 6269
		// int codeOut = 32610;//WGS_1984_UTM_Zone_10N; : GCS 4326
		// int codeIn = SpatialReference::PCS_WGS_1984_UTM_10N;
		// int codeOut = SpatialReference::PCS_WORLD_MOLLWEIDE;
		// int codeOut = 102100;
		inputSR = SpatialReference.create(codeIn);
		assertTrue(inputSR.getID() == codeIn);
		outputSR = SpatialReference.create(codeOut);
		assertTrue(outputSR.getID() == codeOut);

		OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
				.getOperator(Operator.Type.Intersection);

		Polygon poly1 = new Polygon();
		Envelope env1 = new Envelope(855277, 3892059, 855277 + 100,
				3892059 + 100);
		// Envelope env1 = new Envelope(-1000000, -1000000, 1000000, 1000000);
		// env1.SetCoords(-8552770, -3892059, 855277 + 100, 3892059 + 100);
		poly1.addEnvelope(env1, false);

		Polygon poly2 = new Polygon();
		Envelope env2 = new Envelope(855277 + 1, 3892059 + 1, 855277 + 30,
				3892059 + 20);
		poly2.addEnvelope(env2, false);

		GeometryCursor cursor1 = new SimpleGeometryCursor(poly1);
		GeometryCursor cursor2 = new SimpleGeometryCursor(poly2);

		GeometryCursor outputGeoms = operatorIntersection.execute(cursor1,
				cursor2, inputSR, null);
		Geometry geomr = outputGeoms.next();
		assertNotNull(geomr);
		assertTrue(geomr.getType() == Geometry.Type.Polygon);
		Polygon geom = (Polygon) geomr;
		assertTrue(geom.getPointCount() == 4);
		Point[] points = TestCommonMethods.pointsFromMultiPath(geom);// SPtrOfArrayOf(Point2D)
																		// pts =
																		// geom.get.getCoordinates2D();
		assertTrue(Math.abs(points[0].getX() - 855278.000000000) < 1e-7);
		assertTrue(Math.abs(points[0].getY() - 3892060.0000000000) < 1e-7);
		assertTrue(Math.abs(points[2].getX() - 855307.00000000093) < 1e-7);
		assertTrue(Math.abs(points[2].getY() - 3892079.0000000000) < 1e-7);

		geomr = operatorIntersection.execute(poly1, poly2, inputSR, null);
		assertNotNull(geomr);
		assertTrue(geomr.getType() == Geometry.Type.Polygon);
		Polygon outputGeom = (Polygon) geomr;

		assertTrue(outputGeom.getPointCount() == 4);
		points = TestCommonMethods.pointsFromMultiPath(outputGeom);
		assertTrue(Math.abs(points[0].getX() - 855278.000000000) < 1e-7);
		assertTrue(Math.abs(points[0].getY() - 3892060.0000000000) < 1e-7);
		assertTrue(Math.abs(points[2].getX() - 855307.00000000093) < 1e-7);
		assertTrue(Math.abs(points[2].getY() - 3892079.0000000000) < 1e-7);
	}

	@Test
	public void testSelfIntersecting() {// Test that we do not fail if there is
										// self-intersection
										// OperatorFactoryLocal projEnv =
										// OperatorFactoryLocal.getInstance();
		OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
				.getOperator(Operator.Type.Intersection);
		SpatialReference sr = SpatialReference.create(4326);
		Polygon poly1 = new Polygon();
		Envelope2D env1 = new Envelope2D();
		env1.setCoords(0, 0, 20, 30);
		poly1.addEnvelope(env1, false);
		Polygon poly2 = new Polygon();
		poly2.startPath(0, 0);
		poly2.lineTo(10, 10);
		poly2.lineTo(0, 10);
		poly2.lineTo(10, 0);
		@SuppressWarnings("unused")
		Polygon res = (Polygon) (operatorIntersection.execute(poly1, poly2, sr,
				null));
		// Operator_equals equals =
		// (Operator_equals)projEnv.get_operator(Operator::equals);
		// assertTrue(equals.execute(res, poly2, sr, NULL) == true);
	}

	@Test
	public void testMultipoint() {
		Polygon poly1 = new Polygon();
		Envelope env1 = new Envelope(855277, 3892059, 855277 + 100,
				3892059 + 100);

		poly1.addEnvelope(env1, false);
		MultiPoint multiPoint = new MultiPoint();
		multiPoint.add(855277 + 10, 3892059 + 10);
		multiPoint.add(855277, 3892059);
		multiPoint.add(855277 + 100, 3892059 + 100);
		multiPoint.add(855277 + 100, 3892059 + 101);
		multiPoint.add(855277 + 101, 3892059 + 100);
		multiPoint.add(855277 + 101, 3892059 + 101);
		OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
				.getOperator(Operator.Type.Intersection);

		MultiPoint mpResult = (MultiPoint) operatorIntersection.execute(poly1,
				multiPoint, inputSR, null);
		assertTrue(mpResult.getPointCount() == 3);
		assertTrue(mpResult.getPoint(0).getX() == 855277 + 10
				&& mpResult.getPoint(0).getY() == 3892059 + 10);
		assertTrue(mpResult.getPoint(1).getX() == 855277
				&& mpResult.getPoint(1).getY() == 3892059);
		assertTrue(mpResult.getPoint(2).getX() == 855277 + 100
				&& mpResult.getPoint(2).getY() == 3892059 + 100);

		// Test intersection of Polygon with Envelope (calls Clip)
		Polygon poly = new Polygon();
		poly.startPath(0, 0);
		poly.lineTo(10, 10);
		poly.lineTo(20, 0);

		env1.setXMin(0);
		env1.setXMax(20);
		env1.setYMin(5);
		env1.setYMax(15);

		Envelope envelope1 = env1;

		Polygon clippedPoly = (Polygon) operatorIntersection.execute(poly,
				envelope1, inputSR, null);
		double area = clippedPoly.calculateArea2D();
		assertTrue(Math.abs(area - 25) < 0.00001);

		// Geometry res = GeometryEngine.difference(poly, envelope1, inputSR);
		Envelope env2 = new Envelope(855277 + 1, 3892059 + 1, 855277 + 30,
				3892059 + 20);
		env2.setXMin(5);
		env2.setXMax(10);
		env2.setYMin(0);
		env2.setYMax(20);

		Envelope envelope2 = env2;

		Envelope clippedEnvelope = (Envelope) operatorIntersection.execute(
				envelope1, envelope2, inputSR, null);
		area = clippedEnvelope.calculateArea2D();
		assertTrue(Math.abs(area - 50) < 0.00001);
	}

	@Test
	public void testDifferenceOnPolyline() {
		Polyline basePl = new Polyline();
		basePl.startPath(-117, 20);
		basePl.lineTo(-130, 10);
		basePl.lineTo(-120, 50);

		Polyline compPl = new Polyline();
		compPl.startPath(-116, 20);
		compPl.lineTo(-131, 10);
		compPl.lineTo(-121, 50);

		// OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
		OperatorDifference op = (OperatorDifference) projEnv
				.getOperator(Operator.Type.Difference);
		Polyline diffGeom = (Polyline) (op.execute(basePl, compPl,
				SpatialReference.create(4326), null));
		int pc = diffGeom.getPointCount();
		assertTrue(pc == 5);
	}

	@Test
	public void testDifferenceOnPolyline2() {
		Polyline basePl = new Polyline();
		basePl.startPath(0, 0);
		basePl.lineTo(10, 10);
		basePl.lineTo(20, 20);
		basePl.lineTo(10, 0);
		basePl.lineTo(20, 10);

		Polyline compPl = new Polyline();
		compPl.startPath(5, 0);
		compPl.lineTo(5, 10);
		compPl.lineTo(0, 10);
		compPl.lineTo(7.5, 2.5);

		// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/basePl.txt",
		// *basePl, null);
		// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/compPl.txt",
		// *compPl, null);
		// OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
		OperatorDifference op = (OperatorDifference) projEnv
				.getOperator(Operator.Type.Difference);
		Polyline diffGeom = (Polyline) (op.execute(basePl, compPl,
				SpatialReference.create(4326), null));
		// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/diffGeom.txt",
		// *diffGeom, null);
		int pathc = diffGeom.getPathCount();
		assertTrue(pathc == 1);
		int pc = diffGeom.getPointCount();
		assertTrue(pc == 6);

		Polyline resPl = new Polyline();
		resPl.startPath(0, 0);
		resPl.lineTo(5, 5);
		resPl.lineTo(10, 10);
		resPl.lineTo(20, 20);
		resPl.lineTo(10, 0);
		resPl.lineTo(20, 10);
		// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/resPl.txt",
		// *resPl, null);
		assertTrue(resPl.equals(diffGeom));
	}

	@Test
	public void testDifferencePointPolyline() {
		Polyline basePl = new Polyline();
		basePl.startPath(0, 0);
		basePl.lineTo(10, 10);
		basePl.lineTo(20, 20);
		basePl.lineTo(10, 0);
		basePl.lineTo(20, 10);

		Point compPl = new Point(5, 5);

		// OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
		OperatorDifference op = (OperatorDifference) projEnv
				.getOperator(Operator.Type.Difference);
		Polyline diffGeom = (Polyline) (op.execute(basePl, compPl,
				SpatialReference.create(4326), null));
		int pathc = diffGeom.getPathCount();
		assertTrue(pathc == 1);
		int pc = diffGeom.getPointCount();
		assertTrue(pc == 5);

		Polyline resPl = new Polyline();
		resPl.startPath(0, 0);
		resPl.lineTo(10, 10);
		resPl.lineTo(20, 20);
		resPl.lineTo(10, 0);
		resPl.lineTo(20, 10);
		assertTrue(resPl.equals(diffGeom));// no change happens to the original
											// polyline
	}

	@Test
	public void testIntersectionPolylinePolygon() {
		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			polygon.addAttribute(VertexDescription.Semantics.Z);
			polygon.setAttribute(VertexDescription.Semantics.Z, 0, 0, 3);
			polygon.setAttribute(VertexDescription.Semantics.Z, 3, 0, 3);
			polygon.interpolateAttributes(0, 0, 3);
			Polyline polyline = new Polyline();
			polyline.startPath(0, 10);
			polyline.lineTo(5, 5);
			polyline.lineTo(6, 4);
			polyline.lineTo(7, -1);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 3, 0, 5);
			polyline.interpolateAttributes(0, 0, 0, 3);

			// OperatorFactoryLocal projEnv =
			// OperatorFactoryLocal.getInstance();
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) (geom);
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// std::shared_ptr<Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[0,10],[5,5],[6,4],[6.7999999999999998,4.4408922169635528e-016]]]}");
		}

		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			polygon.addAttribute(VertexDescription.Semantics.Z);
			polygon.setAttribute(VertexDescription.Semantics.Z, 0, 0, 3);
			polygon.setAttribute(VertexDescription.Semantics.Z, 3, 0, 3);
			polygon.interpolateAttributes(0, 0, 3);
			Polyline polyline = new Polyline();
			polyline.startPath(0, 10);
			polyline.lineTo(20, 0);
			polyline.lineTo(5, 5);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 1, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);

			// OperatorFactoryLocal projEnv =
			// OperatorFactoryLocal.getInstance();
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) (geom);
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[0,10],[20,0],[5,5]]]}");
		}

		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			polygon.addAttribute(VertexDescription.Semantics.Z);
			polygon.setAttribute(VertexDescription.Semantics.Z, 0, 0, 3);
			polygon.setAttribute(VertexDescription.Semantics.Z, 3, 0, 3);
			polygon.interpolateAttributes(0, 0, 3);
			Polyline polyline = new Polyline();
			polyline.startPath(0, 0);
			polyline.lineTo(0, 10);
			polyline.lineTo(20, 10);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 1, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);

			// OperatorFactoryLocal projEnv =
			// OperatorFactoryLocal.getInstance();
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) (geom);
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[0,0],[0,10],[20,10]]]}");
		}

		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			Polyline polyline = new Polyline();
			polyline.startPath(3, -1);
			polyline.lineTo(17, 1);
			polyline.lineTo(10, 8);
			polyline.lineTo(-1, 5);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 1, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 2, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 3, 0, 5);

			// OperatorFactoryLocal projEnv =
			// OperatorFactoryLocal.getInstance();
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) geom;
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[10,0],[17,1],[10,8],[4.7377092701401439e-024,5.2727272727272734]]]}");
		}

		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			Polyline polyline = new Polyline();
			polyline.startPath(0, 15);
			polyline.lineTo(3, -1);
			polyline.lineTo(17, 1);
			polyline.lineTo(10, 8);
			polyline.lineTo(-1, 5);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 4, 0, 5);
			polyline.interpolateAttributes(0, 0, 0, 4);
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) geom;
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[0.9375,10],[2.8125,9.476226333847234e-024]],[[10,0],[17,1],[10,8],[4.7377092701401439e-024,5.2727272727272734]]]}");
		}

		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			Polyline polyline = new Polyline();
			polyline.startPath(5, 5);
			polyline.lineTo(1, 1);
			polyline.lineTo(-1, 1);
			polyline.lineTo(-1, 10);
			polyline.lineTo(0, 10);
			polyline.lineTo(6, 6);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 5, 0, 5);
			polyline.interpolateAttributes(0, 0, 0, 5);

			// OperatorFactoryLocal projEnv =
			// OperatorFactoryLocal.getInstance();
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) geom;
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[5,5],[1,1],[4.738113166923617e-023,1]],[[0,10],[6,6]]]}");
		}

		{
			Polygon polygon = new Polygon();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(20, 10);
			polygon.lineTo(20, 0);
			Polyline polyline = new Polyline();
			polyline.startPath(0, 15);
			polyline.lineTo(3, -1);
			polyline.lineTo(17, 1);
			polyline.lineTo(10, 8);
			polyline.lineTo(-1, 5);
			polyline.startPath(19, 15);
			polyline.lineTo(29, 9);
			polyline.startPath(19, 15);
			polyline.lineTo(29, 9);
			polyline.startPath(5, 5);
			polyline.lineTo(1, 1);
			polyline.lineTo(-1, 1);
			polyline.lineTo(-1, 10);
			polyline.lineTo(0, 10);
			polyline.lineTo(6, 6);
			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 5);
			polyline.setAttribute(VertexDescription.Semantics.Z, 14, 0, 5);
			polyline.interpolateAttributes(0, 0, 3, 5);

			// OperatorFactoryLocal projEnv =
			// OperatorFactoryLocal.getInstance();
			OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			Geometry geom = operatorIntersection.execute(polyline, polygon,
					null, null);
			assertTrue(!geom.isEmpty());
			Polyline poly = (Polyline) geom;
			for (int i = 0; i < poly.getPointCount(); i++)
				assertTrue(poly.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0) == 5);

			// Operator_export_to_JSON> jsonExport =
			// (Operator_export_to_JSON>)Operator_factory_local::get_instance().get_operator(Operator::Operator_type::export_to_JSON);
			// std::string str = jsonExport.execute(0, geom, null, null);
			// OutputDebugStringA(str.c_str());
			// OutputDebugString(L"\n");
			// assertTrue(str=="{\"paths\":[[[0.9375,10],[2.8125,9.476226333847234e-024]],[[10,0],[17,1],[10,8],[4.7377092701401439e-024,5.2727272727272734]],[[5,5],[1,1],[4.738113166923617e-023,1]],[[0,10],[6,6]]]}");
		}
	}

	@Test
	public void testMultiPointPolyline() {
		Polyline polyline = new Polyline();
		polyline.startPath(0, 0);
		polyline.lineTo(0, 10);
		polyline.lineTo(20, 10);
		polyline.lineTo(20, 0);
		MultiPoint mp = new MultiPoint();
		mp.add(0, 10, 7);
		mp.add(0, 5, 7);
		mp.add(1, 5, 7);
		// OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
		OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
				.getOperator(Operator.Type.Intersection);
		OperatorDifference operatorDifference = (OperatorDifference) projEnv
				.getOperator(Operator.Type.Difference);

		{// intersect
			Geometry geom = operatorIntersection.execute(polyline, mp, null,
					null);
			MultiPoint res = (MultiPoint) geom;
			assertTrue(res.getPointCount() == 2);
			Point2D pt_1 = res.getXY(0);
			Point2D pt_2 = res.getXY(1);
			assertTrue(Point2D.distance(pt_1, new Point2D(0, 10)) < 1e-10
					&& Point2D.distance(pt_2, new Point2D(0, 5)) < 1e-10
					|| Point2D.distance(pt_2, new Point2D(0, 10)) < 1e-10
					&& Point2D.distance(pt_1, new Point2D(0, 5)) < 1e-10);

			assertTrue(res.getAttributeAsDbl(VertexDescription.Semantics.Z, 0,
					0) == 7);
			assertTrue(res.getAttributeAsDbl(VertexDescription.Semantics.Z, 1,
					0) == 7);
		}

		{// difference
			Geometry geom = operatorDifference
					.execute(polyline, mp, null, null);
			// assertTrue(geom.getGeometryType() ==
			// Geometry.GeometryType.Polyline);
			Polyline res = (Polyline) geom;
			assertTrue(res.getPointCount() == 4);
		}
		{// difference
			Geometry geom = operatorDifference
					.execute(mp, polyline, null, null);
			// assertTrue(geom.getType() == Geometry.GeometryType.MultiPoint);
			MultiPoint res = (MultiPoint) geom;
			assertTrue(res.getPointCount() == 1);
			Point2D pt_1 = res.getXY(0);
			assertTrue(Point2D.distance(pt_1, new Point2D(1, 5)) < 1e-10);
		}
		{// difference (subtract empty)
			Geometry geom = operatorDifference.execute(mp, new Polyline(),
					null, null);
			// assertTrue(geom.getGeometryType() ==
			// Geometry.GeometryType.MultiPoint);
			MultiPoint res = (MultiPoint) geom;
			assertTrue(res.getPointCount() == 3);
			Point2D pt_1 = res.getXY(0);
			assertTrue(Point2D.distance(pt_1, new Point2D(0, 10)) < 1e-10);
		}

	}

	@Test
	public void testPointPolyline() {
		Polyline polyline = new Polyline();
		polyline.startPath(0, 0);
		polyline.lineTo(0, 10);
		polyline.lineTo(20, 10);
		polyline.lineTo(20, 0);
		Point p_1 = new Point(0, 5, 7);
		Point p_2 = new Point(0, 10, 7);
		Point p3 = new Point(1, 5, 7);
		// OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
		OperatorIntersection operatorIntersection = (OperatorIntersection) projEnv
				.getOperator(Operator.Type.Intersection);
		OperatorDifference operatorDiff = (OperatorDifference) projEnv
				.getOperator(Operator.Type.Difference);
		OperatorUnion operatorUnion = (OperatorUnion) projEnv
				.getOperator(Operator.Type.Union);
		OperatorSymmetricDifference operatorSymDiff = (OperatorSymmetricDifference) projEnv
				.getOperator(Operator.Type.SymmetricDifference);

		{// intersect case1
			Geometry geom = operatorIntersection.execute(polyline, p_1, null,
					null);
			// assertTrue(geom.getType() == Geometry::enum_point);
			Point res = (Point) geom;
			Point2D pt_1 = res.getXY();
			assertTrue(Point2D.distance(pt_1, new Point2D(0, 5)) < 1e-10);
			assertTrue(res.getAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 7);
		}
		{// intersect case2
			Geometry geom = operatorIntersection.execute(polyline, p_2, null,
					null);
			// assertTrue(geom.getType() == Geometry::enum_point);
			Point res = (Point) geom;
			Point2D pt_1 = res.getXY();
			assertTrue(Point2D.distance(pt_1, new Point2D(0, 10)) < 1e-10);
			assertTrue(res.getAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 7);
		}
		{// intersect case3
			Geometry geom = operatorIntersection.execute(polyline, p3, null,
					null);
			// assertTrue(geom.getType() == Geometry::enum_point);
			assertTrue(geom.isEmpty());
			assertTrue(geom.hasAttribute(VertexDescription.Semantics.Z));
		}

		{// difference case1
			Geometry geom = operatorDiff.execute(polyline, p_1, null, null);
			// assertTrue(geom.getType() == Geometry.GeometryType.Polyline);
			Polyline res = (Polyline) geom;
			assertTrue(res.getPointCount() == 4);
		}
		{// difference case2
			Geometry geom = operatorDiff.execute(p_1, polyline, null, null);
			// assertTrue(geom.getType() == Geometry::enum_point);
			Point res = (Point) geom;
			assertTrue(res.isEmpty());
		}
		{// difference case3
			Geometry geom = operatorDiff.execute(p_2, polyline, null, null);
			// assertTrue(geom.getType() == Geometry::enum_point);
			Point res = (Point) geom;
			assertTrue(res.isEmpty());
		}
		{// difference case4
			Geometry geom = operatorDiff.execute(p3, polyline, null, null);
			// assertTrue(geom.getType() == Geometry::enum_point);
			Point res = (Point) geom;
			Point2D pt_1 = res.getXY();
			assertTrue(Point2D.distance(pt_1, new Point2D(1, 5)) < 1e-10);
		}

		{// union case1
			Geometry geom = operatorUnion.execute(p_1, polyline, null, null);
			// assertTrue(geom.getType() == Geometry.GeometryType.Polyline);
			Polyline res = (Polyline) geom;
			assertTrue(!res.isEmpty());
		}
		{// union case2
			Geometry geom = operatorUnion.execute(polyline, p_1, null, null);
			// assertTrue(geom.getType() == Geometry.GeometryType.Polyline);
			Polyline res = (Polyline) geom;
			assertTrue(!res.isEmpty());
		}

		{// symmetric difference case1
			Geometry geom = operatorSymDiff.execute(polyline, p_1, null, null);
			assertTrue(geom.getType().value() == Geometry.GeometryType.Polyline);
			Polyline res = (Polyline) (geom);
			assertTrue(!res.isEmpty());
		}
		{// symmetric difference case2
			Geometry geom = operatorSymDiff.execute(p_1, polyline, null, null);
			assertTrue(geom.getType().value() == Geometry.GeometryType.Polyline);
			Polyline res = (Polyline) (geom);
			assertTrue(!res.isEmpty());
		}
	}

	@Test
	public void testPolylinePolylineIntersectionExtended() {
		{// crossing intersection
			Polyline basePl = new Polyline();
			basePl.startPath(0, 10);
			basePl.lineTo(100, 10);

			Polyline compPl = new Polyline();
			compPl.startPath(50, 0);
			compPl.lineTo(50, 100);

			OperatorIntersection op = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			GeometryCursor result_cursor = op.execute(new SimpleGeometryCursor(
					basePl), new SimpleGeometryCursor(compPl), SpatialReference
					.create(4326), null, 3);

			// dimension is 3, means it has to return a point and a polyline
			Geometry geom1 = result_cursor.next();
			assertTrue(geom1 != null);
			assertTrue(geom1.getDimension() == 0);
			assertTrue(geom1.getType().value() == Geometry.GeometryType.MultiPoint);
			assertTrue(((MultiPoint) geom1).getPointCount() == 1);

			Geometry geom2 = result_cursor.next();
			assertTrue(geom2 != null);
			assertTrue(geom2.getDimension() == 1);
			assertTrue(geom2.getType().value() == Geometry.GeometryType.Polyline);
			assertTrue(((Polyline) geom2).getPointCount() == 0);

			Geometry geom3 = result_cursor.next();
			assertTrue(geom3 == null);
		}

		{// crossing + overlapping intersection
			Polyline basePl = new Polyline();
			basePl.startPath(0, 10);
			basePl.lineTo(100, 10);

			Polyline compPl = new Polyline();
			compPl.startPath(50, 0);
			compPl.lineTo(50, 100);
			compPl.lineTo(70, 10);
			compPl.lineTo(100, 10);

			OperatorIntersection op = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			GeometryCursor result_cursor = op.execute(new SimpleGeometryCursor(
					basePl), new SimpleGeometryCursor(compPl), SpatialReference
					.create(4326), null, 3);

			// dimension is 3, means it has to return a point and a polyline
			Geometry geom1 = result_cursor.next();
			assertTrue(geom1 != null);
			assertTrue(geom1.getDimension() == 0);
			assertTrue(geom1.getType().value() == Geometry.GeometryType.MultiPoint);
			assertTrue(((MultiPoint) geom1).getPointCount() == 1);

			Geometry geom2 = result_cursor.next();
			assertTrue(geom2 != null);
			assertTrue(geom2.getDimension() == 1);
			assertTrue(geom2.getType().value() == Geometry.GeometryType.Polyline);
			assertTrue(((Polyline) geom2).getPathCount() == 1);
			assertTrue(((Polyline) geom2).getPointCount() == 2);

			Geometry geom3 = result_cursor.next();
			assertTrue(geom3 == null);
		}
	}

	@Test
	public void testPolygonPolygonIntersectionExtended() {
		{// crossing intersection
			Polygon basePl = new Polygon();
			basePl.startPath(0, 0);
			basePl.lineTo(100, 0);
			basePl.lineTo(100, 100);
			basePl.lineTo(0, 100);

			Polygon compPl = new Polygon();
			compPl.startPath(100, 100);
			compPl.lineTo(200, 100);
			compPl.lineTo(200, 200);
			compPl.lineTo(100, 200);

			OperatorIntersection op = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			GeometryCursor result_cursor = op.execute(new SimpleGeometryCursor(
					basePl), new SimpleGeometryCursor(compPl), SpatialReference
					.create(4326), null, 7);

			Geometry geom1 = result_cursor.next();
			assertTrue(geom1 != null);
			assertTrue(geom1.getDimension() == 0);
			assertTrue(geom1.getType().value() == Geometry.GeometryType.MultiPoint);
			assertTrue(((MultiPoint) geom1).getPointCount() == 1);

			Geometry geom2 = result_cursor.next();
			assertTrue(geom2 != null);
			assertTrue(geom2.getDimension() == 1);
			assertTrue(geom2.getType().value() == Geometry.GeometryType.Polyline);
			assertTrue(((Polyline) geom2).getPointCount() == 0);

			Geometry geom3 = result_cursor.next();
			assertTrue(geom3 != null);
			assertTrue(geom3.getDimension() == 2);
			assertTrue(geom3.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(((Polygon) geom3).getPointCount() == 0);

			Geometry geom4 = result_cursor.next();
			assertTrue(geom4 == null);
		}

		{// crossing + overlapping intersection
			Polygon basePl = new Polygon();
			basePl.startPath(0, 0);
			basePl.lineTo(100, 0);
			basePl.lineTo(100, 100);
			basePl.lineTo(0, 100);

			Polygon compPl = new Polygon();
			compPl.startPath(100, 100);
			compPl.lineTo(200, 100);
			compPl.lineTo(200, 200);
			compPl.lineTo(100, 200);

			compPl.startPath(100, 20);
			compPl.lineTo(200, 20);
			compPl.lineTo(200, 40);
			compPl.lineTo(100, 40);

			compPl.startPath(-10, -10);
			compPl.lineTo(-10, 10);
			compPl.lineTo(10, 10);
			compPl.lineTo(10, -10);

			OperatorIntersection op = (OperatorIntersection) projEnv
					.getOperator(Operator.Type.Intersection);
			GeometryCursor result_cursor = op.execute(new SimpleGeometryCursor(
					basePl), new SimpleGeometryCursor(compPl), SpatialReference
					.create(4326), null, 7);

			// dimension is 3, means it has to return a point and a polyline
			Geometry geom1 = result_cursor.next();
			assertTrue(geom1 != null);
			assertTrue(geom1.getDimension() == 0);
			assertTrue(geom1.getType().value() == Geometry.GeometryType.MultiPoint);
			assertTrue(((MultiPoint) geom1).getPointCount() == 1);

			Geometry geom2 = result_cursor.next();
			assertTrue(geom2 != null);
			assertTrue(geom2.getDimension() == 1);
			assertTrue(geom2.getType().value() == Geometry.GeometryType.Polyline);
			assertTrue(((Polyline) geom2).getPathCount() == 1);
			assertTrue(((Polyline) geom2).getPointCount() == 2);

			Geometry geom3 = result_cursor.next();
			assertTrue(geom3 != null);
			assertTrue(geom3.getDimension() == 2);
			assertTrue(geom3.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(((Polygon) geom3).getPathCount() == 1);
			assertTrue(((Polygon) geom3).getPointCount() == 4);

			Geometry geom4 = result_cursor.next();
			assertTrue(geom4 == null);
		}
	}

	@Test
	public void testFromProjection() {
		MultiPoint multiPointInitial = new MultiPoint();
		multiPointInitial.add(-20037508.342789244, 3360107.7777777780);
		multiPointInitial.add(-18924313.434856508, 3360107.7777777780);
		multiPointInitial.add(-18924313.434856508, -3360107.7777777780);
		multiPointInitial.add(-20037508.342789244, -3360107.7777777780);
		Geometry geom1 = ((MultiPoint) multiPointInitial);

		SpatialReference sr = SpatialReference.create(102100);

		Envelope2D env = new Envelope2D();
		env.setCoords(/* xmin */-20037508.342788246, /* ymin */
		-30240971.958386172, /* xmax */20037508.342788246, /* ymax */
		30240971.958386205);
		// /*xmin*/ -20037508.342788246
		// /*ymin*/ -30240971.958386172
		// /*xmax*/ 20037508.342788246
		// /*ymax*/ 30240971.958386205

		Polygon poly = new Polygon();
		poly.startPath(env.xmin, env.ymin);
		poly.lineTo(env.xmin, env.ymax);
		poly.lineTo(env.xmax, env.ymax);
		poly.lineTo(env.xmax, env.ymin);

		Geometry geom2 = new Envelope(env);
		// Geometry geom2 = poly;

		OperatorIntersection operatorIntersection = (OperatorIntersection) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Intersection);

		MultiPoint multiPointOut = (MultiPoint) (operatorIntersection.execute(
				geom1, geom2, sr, null));

		assertTrue(multiPointOut.getCoordinates2D().length == 2);
		assertTrue(multiPointOut.getCoordinates2D()[0].x == -18924313.434856508);
		assertTrue(multiPointOut.getCoordinates2D()[0].y == 3360107.7777777780);
		assertTrue(multiPointOut.getCoordinates2D()[1].x == -18924313.434856508);
		assertTrue(multiPointOut.getCoordinates2D()[1].y == -3360107.7777777780);
	}

	@Test
	public void testIssue258128() {
		Polygon poly1 = new Polygon();
		poly1.startPath(0, 0);
		poly1.lineTo(0, 10);
		poly1.lineTo(10, 10);
		poly1.lineTo(10, 0);

		Polygon poly2 = new Polygon();
		poly2.startPath(10.5, 4);
		poly2.lineTo(10.5, 8);
		poly2.lineTo(14, 10);

		try {
			GeometryCursor result_cursor = OperatorIntersection.local().execute(new SimpleGeometryCursor(
					poly1), new SimpleGeometryCursor(poly2), SpatialReference
					.create(4326), null, 1);
			while (result_cursor.next() != null) {
				
			}
		} catch (Exception e) {
			assertTrue(false);
		}
	}

	@Test
	public void testUnionTickTock() {
		Polygon poly1 = new Polygon();
		poly1.startPath(0, 0);
		poly1.lineTo(0, 10);
		poly1.lineTo(10, 10);
		poly1.lineTo(10, 0);

		Polygon poly2 = new Polygon();
		poly2.startPath(10.5, 4);
		poly2.lineTo(10.5, 8);
		poly2.lineTo(14, 10);

		Transformation2D trans = new Transformation2D();

		Polygon poly3 = (Polygon) poly1.copy();
		trans.setShift(2, 3);
		poly3.applyTransformation(trans);

		Polygon poly4 = (Polygon) poly1.copy();
		trans.setShift(-2, -3);
		poly4.applyTransformation(trans);

		// Create
		ListeningGeometryCursor gc = new ListeningGeometryCursor();
		GeometryCursor ticktock = OperatorUnion.local().execute(gc, null, null);

		// Use tick-tock to push a geometry and do a piece of work.
		gc.tick(poly1);
		ticktock.tock();
		gc.tick(poly2);
		gc.tick(poly3);// skiped one tock just for testing.
		ticktock.tock();
		gc.tick(poly4);
		ticktock.tock();
		// Get the result
		Geometry result = ticktock.next();

		// Use ListeningGeometryCursor to put all geometries in.
		ListeningGeometryCursor gc2 = new ListeningGeometryCursor();
		gc2.tick(poly1);
		gc2.tick(poly2);
		gc2.tick(poly3);
		gc2.tick(poly4);

		GeometryCursor res = OperatorUnion.local().execute(gc2, null, null);
		// Calling next will process all geometries at once.
		Geometry result2 = res.next();
		assertTrue(result.equals(result2));
	}

	@Test
	public void testIntersectionIssueLinePoly1() {
		String wkt1 = new String("polygon((0 0, 10 0, 10 10, 0 10, 0 0))");
		String wkt2 = new String("linestring(9 5, 10 5, 9 4, 8 3)");
		Geometry g1 = OperatorImportFromWkt.local().execute(0, Geometry.Type.Unknown, wkt1, null);
		Geometry g2 = OperatorImportFromWkt.local().execute(0, Geometry.Type.Unknown, wkt2, null);
		Geometry res = OperatorIntersection.local().execute(g1, g2, null, null);
		assertTrue(((Polyline)res).getPathCount() == 1);
		assertTrue(((Polyline)res).getPointCount() == 4);
	}	
        
        @Test
        public void testSharedEdgeIntersection_13()
        {
            String s1 = "{\"rings\":[[[0.099604024000029767,0.2107958250000479],[0.14626826900007472,0.2107958250000479],[0.14626826900007472,0.18285316400005058],[0.099604024000029767,0.18285316400005058],[0.099604024000029767,0.2107958250000479]]]}";
            String s2 = "{\"paths\":[[[0.095692051000071388,0.15910190100004229],[0.10324853600002371,0.18285316400004228],[0.12359292700006108,0.18285316400004228],[0.12782611200003657,0.1705583920000322],[0.13537063000007138,0.18285316400004228]]]}";
            
            Polygon polygon = (Polygon)TestCommonMethods.fromJson(s1).getGeometry();
            Polyline polyline = (Polyline)TestCommonMethods.fromJson(s2).getGeometry();
            SpatialReference sr = SpatialReference.create(4326);
            
            Geometry g = OperatorIntersection.local().execute(polygon, polyline, sr, null);
            assertTrue(!g.isEmpty());
        }
        
        @Test
        public void testIntersectionIssue2() {
            String s1 = "{\"rings\":[[[-97.174860352323378,48.717174479818425],[-97.020624513410553,58.210155436624177],[-94.087641114245969,58.210155436624177],[-94.087641114245969,48.639781902013226],[-97.174860352323378,48.717174479818425]]]}";
            String s2 = "{\"rings\":[[[-94.08764111399995,52.68342763000004],[-94.08764111399995,56.835188018000053],[-90.285921520999977,62.345706350000057],[-94.08764111399995,52.68342763000004]]]}";
            
            Polygon polygon1 = (Polygon)TestCommonMethods.fromJson(s1).getGeometry();
            Polygon polygon2 = (Polygon)TestCommonMethods.fromJson(s2).getGeometry();
            SpatialReference sr = SpatialReference.create(4326);
            
            GeometryCursor res = OperatorIntersection.local().execute(new SimpleGeometryCursor(polygon1), new SimpleGeometryCursor(polygon2), sr, null, 2);
            Geometry g = res.next();
            assertTrue(g != null);
            assertTrue(!g.isEmpty());
            Geometry g2 = res.next();
            assertTrue(g2 == null);
            
            String ss = "{\"paths\":[[[-94.08764111412296,52.68342763000004],[-94.08764111410767,56.83518801800005]]]}";
            Polyline polyline = (Polyline)TestCommonMethods.fromJson(ss).getGeometry();
            boolean eq = OperatorEquals.local().execute(g, polyline, sr, null);
            assertTrue(eq);
        }

        /*
        Point2D uniqueIntersectionPointOfNonDisjointGeometries(Geometry g1, Geometry g2, SpatialReference sr) {
        	Geometry g1Test = g1;
        	boolean g1Polygon = g1.getType() == Geometry.Type.Polygon;
        	boolean g2Polygon = g2.getType() == Geometry.Type.Polygon;

        	if (g1Polygon || g2Polygon)
        	{
        		if (g1Polygon) {
        			Point2D p = getFirstPoint(g2);
        			if (PolygonUtils.isPointInPolygon2D((Polygon)g1, p, 0) != PiPResult.PiPOutside)
        				return p;
        		}
        		if (g2Polygon) {
        			Point2D p = getFirstPoint(g1);
        			if (PolygonUtils.isPointInPolygon2D((Polygon)g2, p, 0) != PiPResult.PiPOutside)
        				return p;
        		}
        	}

        	if (g1Polygon)
        	{
        		Polyline polyline = new Polyline();
        		polyline.add((MultiPath)g1, false);
        		g1Test = polyline;
        	}
        	Geometry g2Test = g2;
        	if (g2Polygon)
        	{
        		Polyline polyline = new Polyline();
        		polyline.add((MultiPath)g2, false);
        		g2Test = polyline;
        	}
        	
        	GeometryCursor gc = OperatorIntersection.local().execute(new SimpleGeometryCursor(g1Test), new SimpleGeometryCursor(g2Test), sr, null, 3);
        	for (Geometry res = gc.next(); res != null; res = gc.next()) {
        		return getFirstPoint(res);
        	}
        	
        	throw new GeometryException("internal error");
        }*/

}
