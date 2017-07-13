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

public class TestBuffer extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testBufferPoint() {
		SpatialReference sr = SpatialReference.create(4326);
		Point inputGeom = new Point(12, 120);
		OperatorBuffer buffer = (OperatorBuffer) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Buffer);
		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		Geometry result = buffer.execute(inputGeom, sr, 40.0, null);
		assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
		Polygon poly = (Polygon) result;
		int pathCount = poly.getPathCount();
		assertTrue(pathCount == 1);
		int pointCount = poly.getPointCount();
		assertTrue(Math.abs(pointCount - 100.0) < 10);
		Envelope2D env2D = new Envelope2D();
		result.queryEnvelope2D(env2D);
		assertTrue(Math.abs(env2D.getWidth() - 80) < 0.01
				&& Math.abs(env2D.getHeight() - 80) < 0.01);
		assertTrue(Math.abs(env2D.getCenterX() - 12) < 0.001
				&& Math.abs(env2D.getCenterY() - 120) < 0.001);
		NonSimpleResult nsr = new NonSimpleResult();
		boolean is_simple = simplify.isSimpleAsFeature(result, sr, true, nsr,
				null);
		assertTrue(is_simple);

		{
			result = buffer.execute(inputGeom, sr, 0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			result = buffer.execute(inputGeom, sr, -1, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}
	}

	@Test
	public void testBufferEnvelope() {
		SpatialReference sr = SpatialReference.create(4326);
		Envelope inputGeom = new Envelope(1, 0, 200, 400);
		OperatorBuffer buffer = (OperatorBuffer) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Buffer);
		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		Geometry result = buffer.execute(inputGeom, sr, 40.0, null);
		assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
		Polygon poly = (Polygon) (result);
		Envelope2D env2D = new Envelope2D();
		result.queryEnvelope2D(env2D);
		assertTrue(Math.abs(env2D.getWidth() - (80 + 199)) < 0.001
				&& Math.abs(env2D.getHeight() - (80 + 400)) < 0.001);
		assertTrue(Math.abs(env2D.getCenterX() - 201.0 / 2) < 0.001
				&& Math.abs(env2D.getCenterY() - 400 / 2.0) < 0.001);
		int pathCount = poly.getPathCount();
		assertTrue(pathCount == 1);
		int pointCount = poly.getPointCount();
		assertTrue(Math.abs(pointCount - 104.0) < 10);
		NonSimpleResult nsr = new NonSimpleResult();
		assertTrue(simplify.isSimpleAsFeature(result, sr, true, nsr, null));

		{
			result = buffer.execute(inputGeom, sr, -200.0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			result = buffer.execute(inputGeom, sr, -200.0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			result = buffer.execute(inputGeom, sr, -199 / 2.0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			result = buffer.execute(inputGeom, sr, -50.0, null);
			poly = (Polygon) (result);
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() - (199 - 100)) < 0.001
					&& Math.abs(env2D.getHeight() - (400 - 100)) < 0.001);
			assertTrue(Math.abs(env2D.getCenterX() - 201.0 / 2) < 0.001
					&& Math.abs(env2D.getCenterY() - 400 / 2.0) < 0.001);
			pathCount = poly.getPathCount();
			assertTrue(pathCount == 1);
			pointCount = poly.getPointCount();
			assertTrue(Math.abs(pointCount - 4.0) < 10);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}
	}

	@Test
	public void testBufferMultiPoint() {
		SpatialReference sr = SpatialReference.create(4326);
		OperatorBuffer buffer = (OperatorBuffer) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Buffer);
		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		MultiPoint inputGeom = new MultiPoint();
		inputGeom.add(12, 120);
		inputGeom.add(20, 120);
		Geometry result = buffer.execute(inputGeom, sr, 40.0, null);
		assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
		Polygon poly = (Polygon) (result);
		Envelope2D env2D = new Envelope2D();
		result.queryEnvelope2D(env2D);
		assertTrue(Math.abs(env2D.getWidth() - 80 - 8) < 0.001
				&& Math.abs(env2D.getHeight() - 80) < 0.001);
		assertTrue(Math.abs(env2D.getCenterX() - 16) < 0.001
				&& Math.abs(env2D.getCenterY() - 120) < 0.001);
		int pathCount = poly.getPathCount();
		assertTrue(pathCount == 1);
		int pointCount = poly.getPointCount();
		assertTrue(Math.abs(pointCount - 108.0) < 10);
		assertTrue(simplify.isSimpleAsFeature(result, sr, null));

		{
			result = buffer.execute(inputGeom, sr, 0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			result = buffer.execute(inputGeom, sr, -1, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}
	}

	@Test
	public void testBufferLine() {
		SpatialReference sr = SpatialReference.create(4326);
		Line inputGeom = new Line(12, 120, 20, 120);
		OperatorBuffer buffer = (OperatorBuffer) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Buffer);
		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		Geometry result = buffer.execute(inputGeom, sr, 40.0, null);
		assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
		Polygon poly = (Polygon) (result);
		Envelope2D env2D = new Envelope2D();
		result.queryEnvelope2D(env2D);
		assertTrue(Math.abs(env2D.getWidth() - 80 - 8) < 0.001
				&& Math.abs(env2D.getHeight() - 80) < 0.001);
		assertTrue(Math.abs(env2D.getCenterX() - 16) < 0.001
				&& Math.abs(env2D.getCenterY() - 120) < 0.001);
		int pathCount = poly.getPathCount();
		assertTrue(pathCount == 1);
		int pointCount = poly.getPointCount();
		assertTrue(Math.abs(pointCount - 100.0) < 10);
		assertTrue(simplify.isSimpleAsFeature(result, sr, null));

		{
			result = buffer.execute(inputGeom, sr, 0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			result = buffer.execute(inputGeom, sr, -1, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}
	}

	@Test
	public void testBufferPolyline() {
		SpatialReference sr = SpatialReference.create(4326);
		Polyline inputGeom = new Polyline();
		OperatorBuffer buffer = (OperatorBuffer) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Buffer);
		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		inputGeom.startPath(0, 0);
		inputGeom.lineTo(50, 50);
		inputGeom.lineTo(50, 0);
		inputGeom.lineTo(0, 50);

		{
			Geometry result = buffer.execute(inputGeom, sr, 0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			Geometry result = buffer.execute(inputGeom, sr, -1, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result.isEmpty());
		}

		{
			Geometry result = buffer.execute(inputGeom, sr, 40.0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			Polygon poly = (Polygon) (result);
			Envelope2D env2D = new Envelope2D();
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() - 80 - 50) < 0.1
					&& Math.abs(env2D.getHeight() - 80 - 50) < 0.1);
			assertTrue(Math.abs(env2D.getCenterX() - 25) < 0.1
					&& Math.abs(env2D.getCenterY() - 25) < 0.1);
			int pathCount = poly.getPathCount();
			assertTrue(pathCount == 1);
			int pointCount = poly.getPointCount();
			assertTrue(Math.abs(pointCount - 171.0) < 10);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}

		{
			Geometry result = buffer.execute(inputGeom, sr, 4.0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			Polygon poly = (Polygon) (result);
			Envelope2D env2D = new Envelope2D();
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() - 8 - 50) < 0.1
					&& Math.abs(env2D.getHeight() - 8 - 50) < 0.1);
			assertTrue(Math.abs(env2D.getCenterX() - 25) < 0.1
					&& Math.abs(env2D.getCenterY() - 25) < 0.1);
			int pathCount = poly.getPathCount();
			assertTrue(pathCount == 2);
			int pointCount = poly.getPointCount();
			assertTrue(Math.abs(pointCount - 186.0) < 10);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}

		{
			inputGeom = new Polyline();
			inputGeom.startPath(0, 0);
			inputGeom.lineTo(50, 50);
			inputGeom.startPath(50, 0);
			inputGeom.lineTo(0, 50);

			Geometry result = buffer.execute(inputGeom, sr, 4.0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			Polygon poly = (Polygon) (result);
			Envelope2D env2D = new Envelope2D();
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() - 8 - 50) < 0.1
					&& Math.abs(env2D.getHeight() - 8 - 50) < 0.1);
			assertTrue(Math.abs(env2D.getCenterX() - 25) < 0.1
					&& Math.abs(env2D.getCenterY() - 25) < 0.1);
			int pathCount = poly.getPathCount();
			assertTrue(pathCount == 1);
			int pointCount = poly.getPointCount();
			assertTrue(Math.abs(pointCount - 208.0) < 10);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}

		{
			inputGeom = new Polyline();
			inputGeom.startPath(1.762614,0.607368);
			inputGeom.lineTo(1.762414,0.606655);
			inputGeom.lineTo(1.763006,0.607034);
			inputGeom.lineTo(1.762548,0.607135);

			Geometry result = buffer.execute(inputGeom, sr, 0.005, null);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}
	}

	@Test
	public void testBufferPolygon() {
		SpatialReference sr = SpatialReference.create(4326);
		Polygon inputGeom = new Polygon();
		OperatorBuffer buffer = (OperatorBuffer) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Buffer);
		OperatorSimplify simplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		inputGeom.startPath(0, 0);
		inputGeom.lineTo(50, 50);
		inputGeom.lineTo(50, 0);

		{
			Geometry result = buffer.execute(inputGeom, sr, 0, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			assertTrue(result == inputGeom);
		}

		{
			Geometry result = buffer.execute(inputGeom, sr, 10, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			Polygon poly = (Polygon) (result);
			Envelope2D env2D = new Envelope2D();
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() - 20 - 50) < 0.1
					&& Math.abs(env2D.getHeight() - 20 - 50) < 0.1);
			assertTrue(Math.abs(env2D.getCenterX() - 25) < 0.1
					&& Math.abs(env2D.getCenterY() - 25) < 0.1);
			int pathCount = poly.getPathCount();
			assertTrue(pathCount == 1);
			int pointCount = poly.getPointCount();
			assertTrue(Math.abs(pointCount - 104.0) < 10);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}

		{
			sr = SpatialReference.create(4326);
			inputGeom = new Polygon();
			inputGeom.startPath(0, 0);
			inputGeom.lineTo(50, 50);
			inputGeom.lineTo(50, 0);

			Geometry result = buffer.execute(inputGeom, sr, -10, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			Polygon poly = (Polygon) (result);
			Envelope2D env2D = new Envelope2D();
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() - 15.85) < 0.1
					&& Math.abs(env2D.getHeight() - 15.85) < 0.1);
			assertTrue(Math.abs(env2D.getCenterX() - 32.07) < 0.1
					&& Math.abs(env2D.getCenterY() - 17.93) < 0.1);
			int pathCount = poly.getPathCount();
			assertTrue(pathCount == 1);
			int pointCount = poly.getPointCount();
			assertTrue(pointCount == 3);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}

		{
			sr = SpatialReference.create(4326);
			inputGeom = new Polygon();
			inputGeom.startPath(0, 0);
			inputGeom.lineTo(0, 50);
			inputGeom.lineTo(50, 50);
			inputGeom.lineTo(50, 0);
			inputGeom.startPath(10, 10);
			inputGeom.lineTo(40, 10);
			inputGeom.lineTo(40, 40);
			inputGeom.lineTo(10, 40);

			Geometry result = buffer.execute(inputGeom, sr, -2, null);
			assertTrue(result.getType().value() == Geometry.GeometryType.Polygon);
			Polygon poly = (Polygon) (result);
			Envelope2D env2D = new Envelope2D();
			result.queryEnvelope2D(env2D);
			assertTrue(Math.abs(env2D.getWidth() + 4 - 50) < 0.1
					&& Math.abs(env2D.getHeight() + 4 - 50) < 0.1);
			assertTrue(Math.abs(env2D.getCenterX() - 25) < 0.1
					&& Math.abs(env2D.getCenterY() - 25) < 0.1);
			int pathCount = poly.getPathCount();
			assertTrue(pathCount == 2);
			int pointCount = poly.getPointCount();
			assertTrue(Math.abs(pointCount - 108) < 10);
			assertTrue(simplify.isSimpleAsFeature(result, sr, null));
		}
	}
}
