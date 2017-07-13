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

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;

public class TestRelation extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testCreation() {
		{
			OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
			SpatialReference inputSR = SpatialReference.create(3857);

			Polygon poly1 = new Polygon();
			Envelope2D env1 = new Envelope2D();
			env1.setCoords(855277, 3892059, 855277 + 100, 3892059 + 100);
			poly1.addEnvelope(env1, false);

			Polygon poly2 = new Polygon();
			Envelope2D env2 = new Envelope2D();
			env2.setCoords(855277, 3892059, 855277 + 300, 3892059 + 200);
			poly2.addEnvelope(env2, false);

			{
				OperatorEquals operatorEquals = (OperatorEquals) (projEnv
						.getOperator(Operator.Type.Equals));
				boolean result = operatorEquals.execute(poly1, poly2, inputSR,
						null);
				assertTrue(!result);
				Polygon poly11 = new Polygon();
				poly1.copyTo(poly11);
				result = operatorEquals.execute(poly1, poly11, inputSR, null);
				assertTrue(result);
			}
			{
				OperatorCrosses operatorCrosses = (OperatorCrosses) (projEnv
						.getOperator(Operator.Type.Crosses));
				boolean result = operatorCrosses.execute(poly1, poly2, inputSR,
						null);
				assertTrue(!result);
			}
			{
				OperatorWithin operatorWithin = (OperatorWithin) (projEnv
						.getOperator(Operator.Type.Within));
				boolean result = operatorWithin.execute(poly1, poly2, inputSR,
						null);
				assertTrue(result);
			}

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				OperatorIntersects operatorIntersects = (OperatorIntersects) (projEnv
						.getOperator(Operator.Type.Intersects));
				boolean result = operatorDisjoint.execute(poly1, poly2,
						inputSR, null);
				assertTrue(!result);
				{
					result = operatorIntersects.execute(poly1, poly2, inputSR,
							null);
					assertTrue(result);
				}
			}

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				OperatorIntersects operatorIntersects = (OperatorIntersects) (projEnv
						.getOperator(Operator.Type.Intersects));
				Envelope2D env2D = new Envelope2D();
				poly2.queryEnvelope2D(env2D);
				Envelope envelope = new Envelope(env2D);
				boolean result = operatorDisjoint.execute(envelope, poly2,
						inputSR, null);
				assertTrue(!result);
				{
					result = operatorIntersects.execute(envelope, poly2,
							inputSR, null);
					assertTrue(result);
				}
			}

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				OperatorIntersects operatorIntersects = (OperatorIntersects) (projEnv
						.getOperator(Operator.Type.Intersects));
				Polygon poly = new Polygon();

				Envelope2D env2D = new Envelope2D();
				env2D.setCoords(855277, 3892059, 855277 + 100, 3892059 + 100);
				poly.addEnvelope(env2D, false);
				env2D.setCoords(855277 + 10, 3892059 + 10, 855277 + 90,
						3892059 + 90);
				poly.addEnvelope(env2D, true);

				env2D.setCoords(855277 + 20, 3892059 + 20, 855277 + 200,
						3892059 + 80);
				Envelope envelope = new Envelope(env2D);
				boolean result = operatorDisjoint.execute(envelope, poly,
						inputSR, null);
				assertTrue(!result);
				{
					result = operatorIntersects.execute(envelope, poly,
							inputSR, null);
					assertTrue(result);
				}
			}

			{
				OperatorTouches operatorTouches = (OperatorTouches) (projEnv
						.getOperator(Operator.Type.Touches));
				boolean result = operatorTouches.execute(poly1, poly2, inputSR,
						null);
				assertTrue(!result);
			}

		}
	}

	@Test
	public void testOperatorDisjoint() {
		{
			OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance();
			SpatialReference inputSR = SpatialReference.create(3857);

			Polygon poly1 = new Polygon();
			Envelope2D env1 = new Envelope2D();
			env1.setCoords(855277, 3892059, 855277 + 100, 3892059 + 100);
			poly1.addEnvelope(env1, false);

			Polygon poly2 = new Polygon();
			Envelope2D env2 = new Envelope2D();
			env2.setCoords(855277, 3892059, 855277 + 300, 3892059 + 200);
			poly2.addEnvelope(env2, false);

			Polygon poly3 = new Polygon();
			Envelope2D env3 = new Envelope2D();
			env3.setCoords(855277 + 100, 3892059 + 100, 855277 + 100 + 100,
					3892059 + 100 + 100);
			poly3.addEnvelope(env3, false);

			Polygon poly4 = new Polygon();
			Envelope2D env4 = new Envelope2D();
			env4.setCoords(855277 + 200, 3892059 + 200, 855277 + 200 + 100,
					3892059 + 200 + 100);
			poly4.addEnvelope(env4, false);

			Point point1 = new Point(855277, 3892059);
			Point point2 = new Point(855277 + 2, 3892059 + 3);
			Point point3 = new Point(855277 - 2, 3892059 - 3);

			{
				OperatorDisjoint operatorDisjoint = (OperatorDisjoint) (projEnv
						.getOperator(Operator.Type.Disjoint));
				boolean result = operatorDisjoint.execute(poly1, poly2,
						inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, poly3, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, poly4, inputSR, null);
				assertTrue(result);

				result = operatorDisjoint.execute(poly1, point1, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(point1, poly1, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, point2, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(point2, poly1, inputSR, null);
				assertTrue(!result);
				result = operatorDisjoint.execute(poly1, point3, inputSR, null);
				assertTrue(result);
				result = operatorDisjoint.execute(point3, poly1, inputSR, null);
				assertTrue(result);
			}
		}
	}

	@Test
	public void testTouchPointLineCR183227() {// Tests CR 183227
		OperatorTouches operatorTouches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));

		Geometry baseGeom = new Point(-130, 10);
		Polyline pl = new Polyline();
		// pl.startPath(std::make_shared<Point>(-130, 10));
		pl.startPath(-130, 10);
		pl.lineTo(-131, 15);
		pl.lineTo(-140, 20);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		isTouched = operatorTouches.execute(baseGeom, pl, sr, null);
		isTouched2 = operatorTouches.execute(pl, baseGeom, sr, null);
		assertTrue(isTouched && isTouched2);

		{
			baseGeom = new Point(-131, 15);
			isTouched = operatorTouches.execute(baseGeom, pl, sr, null);
			isTouched2 = operatorTouches.execute(pl, baseGeom, sr, null);
			assertTrue(!isTouched && !isTouched2);
		}
	}

	@Test
	public void testTouchPointLineClosed() {// Tests CR 183227
		OperatorTouches operatorTouches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));

		Geometry baseGeom = new Point(-130, 10);
		Polyline pl = new Polyline();
		pl.startPath(-130, 10);
		pl.lineTo(-131, 15);
		pl.lineTo(-140, 20);
		pl.lineTo(-130, 10);

		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		isTouched = operatorTouches.execute(baseGeom, pl, sr, null);
		isTouched2 = operatorTouches.execute(pl, baseGeom, sr, null);
		assertTrue(!isTouched && !isTouched2);// this may change in future

		{
			baseGeom = new Point(-131, 15);
			isTouched = operatorTouches.execute(baseGeom, pl, sr, null);
			isTouched2 = operatorTouches.execute(pl, baseGeom, sr, null);
			assertTrue(!isTouched && !isTouched2);
		}
	}

	@Test
	public void testTouchPolygonPolygon() {
		OperatorTouches operatorTouches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));

		Polygon pg = new Polygon();
		pg.startPath(-130, 10);
		pg.lineTo(-131, 15);
		pg.lineTo(-140, 20);

		Polygon pg2 = new Polygon();
		pg2.startPath(-130, 10);
		pg2.lineTo(-131, 15);
		pg2.lineTo(-120, 20);
		SpatialReference sr = SpatialReference.create(4326);

		boolean isTouched;
		boolean isTouched2;
		isTouched = operatorTouches.execute(pg, pg2, sr, null);
		isTouched2 = operatorTouches.execute(pg2, pg, sr, null);
		assertTrue(isTouched && isTouched2);
	}

	@Test
	public void testContainsFailureCR186456() {
		{
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			String str = "{\"rings\":[[[406944.399999999,287461.450000001],[406947.750000011,287462.299999997],[406946.44999999,287467.450000001],[406943.050000005,287466.550000005],[406927.799999992,287456.849999994],[406926.949999996,287456.599999995],[406924.800000005,287455.999999998],[406924.300000007,287455.849999999],[406924.200000008,287456.099999997],[406923.450000011,287458.449999987],[406922.999999987,287459.800000008],[406922.29999999,287462.099999998],[406921.949999991,287463.449999992],[406921.449999993,287465.050000011],[406920.749999996,287466.700000004],[406919.800000001,287468.599999996],[406919.050000004,287469.99999999],[406917.800000009,287471.800000008],[406916.04999999,287473.550000001],[406915.449999993,287473.999999999],[406913.700000001,287475.449999993],[406913.300000002,287475.899999991],[406912.050000008,287477.250000011],[406913.450000002,287478.150000007],[406915.199999994,287478.650000005],[406915.999999991,287478.800000005],[406918.300000007,287479.200000003],[406920.649999997,287479.450000002],[406923.100000013,287479.550000001],[406925.750000001,287479.450000002],[406928.39999999,287479.150000003],[406929.80000001,287478.950000004],[406932.449999998,287478.350000006],[406935.099999987,287477.60000001],[406938.699999998,287476.349999989],[406939.649999994,287473.949999999],[406939.799999993,287473.949999999],[406941.249999987,287473.75],[406942.700000007,287473.250000002],[406943.100000005,287473.100000003],[406943.950000001,287472.750000004],[406944.799999998,287472.300000006],[406944.999999997,287472.200000007],[406946.099999992,287471.200000011],[406946.299999991,287470.950000012],[406948.00000001,287468.599999996],[406948.10000001,287468.399999997],[406950.100000001,287465.050000011],[406951.949999993,287461.450000001],[406952.049999993,287461.300000001],[406952.69999999,287459.900000007],[406953.249999987,287458.549999987],[406953.349999987,287458.299999988],[406953.650000012,287457.299999992],[406953.900000011,287456.349999996],[406954.00000001,287455.300000001],[406954.00000001,287454.750000003],[406953.850000011,287453.750000008],[406953.550000012,287452.900000011],[406953.299999987,287452.299999988],[406954.500000008,287450.299999996],[406954.00000001,287449.000000002],[406953.399999987,287447.950000006],[406953.199999988,287447.550000008],[406952.69999999,287446.850000011],[406952.149999992,287446.099999988],[406951.499999995,287445.499999991],[406951.149999996,287445.249999992],[406950.449999999,287444.849999994],[406949.600000003,287444.599999995],[406949.350000004,287444.549999995],[406948.250000009,287444.499999995],[406947.149999987,287444.699999994],[406946.849999989,287444.749999994],[406945.899999993,287444.949999993],[406944.999999997,287445.349999991],[406944.499999999,287445.64999999],[406943.650000003,287446.349999987],[406942.900000006,287447.10000001],[406942.500000008,287447.800000007],[406942.00000001,287448.700000003],[406941.600000011,287449.599999999],[406941.350000013,287450.849999994],[406941.350000013,287451.84999999],[406941.450000012,287452.850000012],[406941.750000011,287453.850000007],[406941.800000011,287454.000000007],[406942.150000009,287454.850000003],[406942.650000007,287455.6],[406943.150000005,287456.299999997],[406944.499999999,287457.299999992],[406944.899999997,287457.599999991],[406945.299999995,287457.949999989],[406944.399999999,287461.450000001],[406941.750000011,287461.999999998],[406944.399999999,287461.450000001]],[[406944.399999999,287461.450000001],[406947.750000011,287462.299999997],[406946.44999999,287467.450000001],[406943.050000005,287466.550000005],[406927.799999992,287456.849999994],[406944.399999999,287461.450000001]]]}";
			MapGeometry mg = TestCommonMethods.fromJson(str);
			boolean res = op.execute((mg.getGeometry()), (mg.getGeometry()),
					null, null);
			assertTrue(res);
		}
	}

	@Test
	public void testWithin() {
		{
			OperatorWithin op = (OperatorWithin) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Within));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0],[0,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"x\":100,\"y\":100}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);

			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);

		}

		{// polygon
			OperatorWithin op = (OperatorWithin) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Within));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[100,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"rings\":[[[10,10],[10,100],[100,100],[100,10]]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);

			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}

		{// Multi_point
			OperatorWithin op = (OperatorWithin) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Within));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}

		{// Multi_point
			OperatorWithin op = (OperatorWithin) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Within));
			String str1 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}

		{// Multi_point
			OperatorWithin op = (OperatorWithin) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Within));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200], [1, 1]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}
	}

	@Test
	public void testContains() {
		{
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0],[0,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"x\":100,\"y\":100}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);

			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}

		{// polygon
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"rings\":[[[10,10],[10,100],[100,100],[10,10]]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);

			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}

		{// Multi_point
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}

		{// Multi_point
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			String str1 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}

		{// Multi_point
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200], [1, 1]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}
	}

	@Test
	public void testOverlaps() {
		{// empty polygon
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			Polygon poly1 = new Polygon();
			Polygon poly2 = new Polygon();

			boolean res = op.execute(poly1, poly2, null, null);
			assertTrue(!res);
			res = op.execute(poly1, poly2, null, null);
			assertTrue(!res);
		}
		{// polygon
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0],[0,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"x\":100,\"y\":100}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);

			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}
		{// polygon
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(300, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}
		{// polygon
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(30, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}
		{// polygon
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"rings\":[[[0,0],[0,200],[200,200],[200,0],[0,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(0, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}

		{// polyline
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"paths\":[[[0,0],[100,0],[200,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(0, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}

		{// polyline
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"paths\":[[[0,0],[100,0],[200,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(10, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}

		{// polyline
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"paths\":[[[0,0],[100,0],[200,0]]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(200, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}

		{// Multi_point
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200],[200,0]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			MapGeometry mg2 = TestCommonMethods.fromJson(str1);
			Transformation2D trans = new Transformation2D();
			trans.setShift(0, 0);
			mg2.getGeometry().applyTransformation(trans);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}
		{// Multi_point
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(!res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(!res);
		}
		{// Multi_point
			OperatorOverlaps op = (OperatorOverlaps) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Overlaps));
			String str1 = "{\"points\":[[0,0],[0,200],[200,200]]}";
			MapGeometry mg1 = TestCommonMethods.fromJson(str1);
			String str2 = "{\"points\":[[0,0],[0,200], [0,2]]}";
			MapGeometry mg2 = TestCommonMethods.fromJson(str2);
			boolean res = op.execute((mg2.getGeometry()), (mg1.getGeometry()),
					null, null);
			assertTrue(res);
			res = op.execute((mg1.getGeometry()), (mg2.getGeometry()), null,
					null);
			assertTrue(res);
		}
	}

	@Test
	public void testPolygonPolygonEquals() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polygon1 and Polygon2 are topologically equal, but have differing
		// number of vertices
		String str1 = "{\"rings\":[[[0,0],[0,5],[0,7],[0,10],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"rings\":[[[0,10],[10,10],[10,0],[0,0],[0,10]],[[9,1],[9,6],[9,9],[1,9],[1,1],[1,1],[9,1]]]}";

		Polygon polygon1 = (Polygon) TestCommonMethods.fromJson(str1)
				.getGeometry();
		Polygon polygon2 = (Polygon) TestCommonMethods.fromJson(str2)
				.getGeometry();
		// wiggleGeometry(polygon1, tolerance, 1982);
		// wiggleGeometry(polygon2, tolerance, 511);

		equals.accelerateGeometry(polygon1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);
		equals.accelerateGeometry(polygon2, sr,
				Geometry.GeometryAccelerationDegree.enumHot);

		boolean res = equals.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		equals.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		// The outer rings of Polygon1 and Polygon2 are equal, but Polygon1 has
		// a hole.
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[0,10],[10,10],[5,10],[10,10],[10,0],[0,0],[0,10]]]}";
		polygon1 = (Polygon) TestCommonMethods.fromJson(str1).getGeometry();
		polygon2 = (Polygon) TestCommonMethods.fromJson(str2).getGeometry();

		res = equals.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = equals.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// The rings are equal but rotated
		str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
		str2 = "{\"rings\":[[[0,10],[10,10],[10,0],[0,0],[0,10]]]}";

		polygon1 = (Polygon) TestCommonMethods.fromJson(str1).getGeometry();
		polygon2 = (Polygon) TestCommonMethods.fromJson(str2).getGeometry();

		res = equals.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = equals.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		// The rings are equal but opposite orientation
		str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
		str2 = "{\"rings\":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}";

		polygon1 = (Polygon) TestCommonMethods.fromJson(str1).getGeometry();
		polygon2 = (Polygon) TestCommonMethods.fromJson(str2).getGeometry();

		res = equals.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = equals.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// The rings are equal but first polygon has two rings stacked
		str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[0,10],[10,10],[10,0],[0,0],[0,10]]]}";
		str2 = "{\"rings\":[[[0,10],[10,10],[10,0],[0,0],[0,10]]]}";
		polygon1 = (Polygon) TestCommonMethods.fromJson(str1).getGeometry();
		polygon2 = (Polygon) TestCommonMethods.fromJson(str2).getGeometry();

		res = equals.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = equals.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testMultiPointMultiPointEquals() {
		OperatorEquals equals = (OperatorEquals) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals);
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		MultiPoint multipoint1 = new MultiPoint();
		MultiPoint multipoint2 = new MultiPoint();

		multipoint1.add(0, 0);
		multipoint1.add(1, 1);
		multipoint1.add(2, 2);
		multipoint1.add(3, 3);
		multipoint1.add(4, 4);
		multipoint1.add(1, 1);
		multipoint1.add(0, 0);

		multipoint2.add(4, 4);
		multipoint2.add(3, 3);
		multipoint2.add(2, 2);
		multipoint2.add(1, 1);
		multipoint2.add(0, 0);
		multipoint2.add(2, 2);

		wiggleGeometry(multipoint1, 0.001, 123);
		wiggleGeometry(multipoint2, 0.001, 5937);
		boolean res = equals.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = equals.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);

		multipoint1.add(1, 2);
		res = equals.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = equals.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testMultiPointPointEquals() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		MultiPoint multipoint1 = new MultiPoint();
		Point point2 = new Point();

		multipoint1.add(2, 2);
		multipoint1.add(2, 2);

		point2.setXY(2, 2);

		wiggleGeometry(multipoint1, 0.001, 123);
		boolean res = equals.execute(multipoint1, point2, sr, null);
		assertTrue(res);
		res = equals.execute(point2, multipoint1, sr, null);
		assertTrue(res);

		res = within.execute(multipoint1, point2, sr, null);
		assertTrue(res);
		res = within.execute(point2, multipoint1, sr, null);
		assertTrue(res);

		multipoint1.add(4, 4);
		res = equals.execute(multipoint1, point2, sr, null);
		assertTrue(!res);
		res = equals.execute(point2, multipoint1, sr, null);
		assertTrue(!res);

		res = within.execute(multipoint1, point2, sr, null);
		assertTrue(!res);
		res = within.execute(point2, multipoint1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPointPointEquals() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Point point1 = new Point();
		Point point2 = new Point();

		point1.setXY(2, 2);
		point2.setXY(2, 2);

		boolean res = equals.execute(point1, point2, sr, null);
		assertTrue(res);
		res = equals.execute(point2, point1, sr, null);
		assertTrue(res);

		res = within.execute(point1, point2, sr, null);
		assertTrue(res);
		res = within.execute(point2, point1, sr, null);
		assertTrue(res);

		res = contains.execute(point1, point2, sr, null);
		assertTrue(res);
		res = contains.execute(point2, point1, sr, null);
		assertTrue(res);

		res = disjoint.execute(point1, point2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(point2, point1, sr, null);
		assertTrue(!res);

		point2.setXY(2, 3);
		res = equals.execute(point1, point2, sr, null);
		assertTrue(!res);
		res = equals.execute(point2, point1, sr, null);
		assertTrue(!res);

		res = within.execute(point1, point2, sr, null);
		assertTrue(!res);
		res = within.execute(point2, point1, sr, null);
		assertTrue(!res);

		res = contains.execute(point1, point2, sr, null);
		assertTrue(!res);
		res = contains.execute(point2, point1, sr, null);
		assertTrue(!res);

		res = disjoint.execute(point1, point2, sr, null);
		assertTrue(res);
		res = disjoint.execute(point2, point1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPolygonDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polygon1 and Polygon2 are topologically equal, but have differing
		// number of vertices
		String str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"rings\":[[[0,10],[10,10],[10,0],[0,0],[0,10]],[[9,1],[9,6],[9,9],[1,9],[1,1],[1,1],[9,1]]]}";

		Polygon polygon1 = (Polygon) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polygon polygon2 = (Polygon) (TestCommonMethods.fromJson(str2)
				.getGeometry());

		boolean res = disjoint.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 and Polygon2 touch at a point
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[10,10],[10,15],[15,15],[15,10],[10,10]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = disjoint.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 and Polygon2 touch along the boundary
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[10,0],[10,10],[15,10],[15,0],[10,0]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = disjoint.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon2 is inside of the hole of polygon1
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[8,2],[2,2]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

		res = disjoint.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = disjoint.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		// Polygon2 is inside of the hole of polygon1
		str1 = "{\"rings\":[[[0,0],[0,5],[5,5],[5,0]],[[10,0],[10,10],[20,10],[20,0]]]}";
		str2 = "{\"rings\":[[[0,-10],[0,-5],[5,-5],[5,-10]],[[11,1],[11,9],[19,9],[19,1]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

		res = disjoint.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

        polygon1 = (Polygon)OperatorDensifyByLength.local().execute(polygon1, 0.5, null);
        disjoint.accelerateGeometry(polygon1, sr, GeometryAccelerationDegree.enumHot);
        res = disjoint.execute(polygon1, polygon2, sr, null);
        assertTrue(!res);
        res = disjoint.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        polygon1.reverseAllPaths();
        polygon2.reverseAllPaths();
        res = disjoint.execute(polygon1, polygon2, sr, null);
        assertTrue(!res);
        res = disjoint.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        // Polygon1 contains polygon2, but polygon2 is counterclockwise.
        str1 = "{\"rings\":[[[0,0],[10,0],[10,10],[0,10],[0,0]],[[11,0],[11,10],[21,10],[21,0],[11,0]]]}";
        str2 = "{\"rings\":[[[2,2],[8,2],[8,8],[2,8],[2,2]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());


        res = disjoint.execute(polygon1, polygon2, sr, null);
        assertTrue(!res);
        res = disjoint.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        polygon1 = (Polygon)OperatorDensifyByLength.local().execute(polygon1, 0.5, null);
        disjoint.accelerateGeometry(polygon1, sr, GeometryAccelerationDegree.enumHot);
        res = disjoint.execute(polygon1, polygon2, sr, null);
        assertTrue(!res);
        res = disjoint.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[0,20],[0,30],[10,30],[10,20],[0,20]],[[20,20],[20,30],[30,30],[30,20],[20,20]],[[20,0],[20,10],[30,10],[30,0],[20,0]]]}";
        str2 = "{\"rings\":[[[14,14],[14,16],[16,16],[16,14],[14,14]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());


        res = disjoint.execute(polygon1, polygon2, sr, null);
        assertTrue(res);
        res = disjoint.execute(polygon2, polygon1, sr, null);
        assertTrue(res);

        polygon1 = (Polygon)OperatorDensifyByLength.local().execute(polygon1, 0.5, null);
        disjoint.accelerateGeometry(polygon1, sr, GeometryAccelerationDegree.enumHot);
        res = disjoint.execute(polygon1, polygon2, sr, null);
        assertTrue(res);
        res = disjoint.execute(polygon2, polygon1, sr, null);
        assertTrue(res);
	}

	@Test
	public void testPolylinePolylineDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polyline1 and Polyline2 touch at a point
		String str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"paths\":[[[10,10],[10,15],[15,15],[15,10],[10,10]]]}";

		Polyline polyline1 = (Polyline) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polyline polyline2 = (Polyline) (TestCommonMethods.fromJson(str2)
				.getGeometry());
		wiggleGeometry(polyline1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		boolean res = disjoint.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		// Polyline1 and Polyline2 touch along the boundary
		str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[10,0],[10,10],[15,10],[15,0],[10,0]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polyline1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		res = disjoint.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		// Polyline2 does not intersect with Polyline1
		str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[2,2],[2,8],[8,8],[8,2],[2,2]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = disjoint.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = disjoint.execute(polyline2, polyline1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPolylineDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		Polyline polyline2 = new Polyline();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		polygon1.startPath(1, 1);
		polygon1.lineTo(9, 1);
		polygon1.lineTo(9, 9);
		polygon1.lineTo(1, 9);

		polyline2.startPath(3, 3);
		polyline2.lineTo(6, 6);

		boolean res = disjoint.execute(polyline2, polygon1, sr, null);
		assertTrue(res);
		res = disjoint.execute(polygon1, polyline2, sr, null);
		assertTrue(res);

		polyline2.startPath(0, 0);
		polyline2.lineTo(0, 5);

		res = disjoint.execute(polyline2, polygon1, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polygon1, polyline2, sr, null);
		assertTrue(!res);

		polygon1.setEmpty();
		polyline2.setEmpty();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		polyline2.startPath(2, 2);
		polyline2.lineTo(4, 4);

		OperatorFactoryLocal factory = OperatorFactoryLocal.getInstance();
		OperatorSimplify simplify_op = (OperatorSimplify) factory
				.getOperator(Operator.Type.Simplify);
		simplify_op.isSimpleAsFeature(polygon1, sr, null);
		simplify_op.isSimpleAsFeature(polyline2, sr, null);

		res = disjoint.execute(polyline2, polygon1, sr, null);
		assertTrue(!res);
		res = disjoint.execute(polygon1, polyline2, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolylineMultiPointDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		MultiPoint multipoint2 = new MultiPoint();

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline1.lineTo(4, 2);

		multipoint2.add(1, 1);
		multipoint2.add(2, 2);
		multipoint2.add(3, 0);

		boolean res = disjoint.execute(polyline1, multipoint2, sr, null);
		assertTrue(res);
		res = disjoint.execute(multipoint2, polyline1, sr, null);
		assertTrue(res);

		multipoint2.add(3, 1);
		res = disjoint.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.startPath(1, -4);
		polyline1.lineTo(1, -3);
		polyline1.lineTo(1, -2);
		polyline1.lineTo(1, -1);
		polyline1.lineTo(1, 0);
		polyline1.lineTo(1, 1);

		disjoint.accelerateGeometry(polyline1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);
		res = disjoint.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolylinePointDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		Point point2 = new Point();

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline1.lineTo(4, 2);

		point2.setXY(1, 1);

		boolean res = disjoint.execute(polyline1, point2, sr, null);
		assertTrue(res);
		res = disjoint.execute(point2, polyline1, sr, null);
		assertTrue(res);

		res = contains.execute(polyline1, point2, sr, null);
		assertTrue(!res);
		res = contains.execute(point2, polyline1, sr, null);
		assertTrue(!res);

		point2.setXY(4, 2);

		polyline1 = (Polyline) OperatorDensifyByLength.local().execute(
				polyline1, 0.1, null);
		disjoint.accelerateGeometry(polyline1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);

		res = disjoint.execute(polyline1, point2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(point2, polyline1, sr, null);
		assertTrue(!res);

		res = contains.execute(polyline1, point2, sr, null);
		assertTrue(!res);
		res = contains.execute(point2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.setEmpty();
		point2.setEmpty();

		polyline1.startPath(659062.37370000035, 153070.85220000148);
		polyline1.lineTo(660916.47940000147, 151481.10269999877);
		point2.setXY(659927.85020000115, 152328.77430000156);

		res = contains.execute(polyline1, point2,
				SpatialReference.create(54004), null);
		assertTrue(res);
	}

	@Test
	public void testMultiPointMultiPointDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		MultiPoint multipoint1 = new MultiPoint();
		MultiPoint multipoint2 = new MultiPoint();

		multipoint1.add(2, 2);
		multipoint1.add(2, 5);
		multipoint1.add(4, 1);
		multipoint1.add(4, 4);
		multipoint1.add(4, 7);
		multipoint1.add(6, 2);
		multipoint1.add(6, 6);
		multipoint1.add(4, 1);
		multipoint1.add(6, 6);

		multipoint2.add(0, 1);
		multipoint2.add(0, 7);
		multipoint2.add(4, 2);
		multipoint2.add(4, 6);
		multipoint2.add(6, 4);
		multipoint2.add(4, 2);
		multipoint2.add(0, 1);

		boolean res = disjoint.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = disjoint.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);

		multipoint2.add(2, 2);
		res = disjoint.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testMultiPointPointDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		MultiPoint multipoint1 = new MultiPoint();
		Point point2 = new Point();

		multipoint1.add(2, 2);
		multipoint1.add(2, 5);
		multipoint1.add(4, 1);
		multipoint1.add(4, 4);
		multipoint1.add(4, 7);
		multipoint1.add(6, 2);
		multipoint1.add(6, 6);
		multipoint1.add(4, 1);
		multipoint1.add(6, 6);

		point2.setXY(2, 6);

		boolean res = disjoint.execute(multipoint1, point2, sr, null);
		assertTrue(res);
		res = disjoint.execute(point2, multipoint1, sr, null);
		assertTrue(res);

		res = contains.execute(multipoint1, point2, sr, null);
		assertTrue(!res);
		res = contains.execute(point2, multipoint1, sr, null);
		assertTrue(!res);

		multipoint1.add(2, 6);
		res = disjoint.execute(multipoint1, point2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(point2, multipoint1, sr, null);
		assertTrue(!res);

		res = contains.execute(multipoint1, point2, sr, null);
		assertTrue(res);
		res = contains.execute(point2, multipoint1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolygonMultiPointDisjoint() {
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		MultiPoint multipoint2 = new MultiPoint();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);

		multipoint2.add(-1, 5);
		multipoint2.add(5, 11);
		multipoint2.add(11, 5);
		multipoint2.add(5, -1);

		boolean res = disjoint.execute(polygon1, multipoint2, sr, null);
		assertTrue(res);
		res = disjoint.execute(multipoint2, polygon1, sr, null);
		assertTrue(res);

		polygon1.startPath(15, 0);
		polygon1.lineTo(15, 10);
		polygon1.lineTo(25, 10);
		polygon1.lineTo(25, 0);

		multipoint2.add(14, 5);
		multipoint2.add(20, 11);
		multipoint2.add(26, 5);
		multipoint2.add(20, -1);

		res = disjoint.execute(polygon1, multipoint2, sr, null);
		assertTrue(res);
		res = disjoint.execute(multipoint2, polygon1, sr, null);
		assertTrue(res);

		multipoint2.add(20, 5);

		res = disjoint.execute(polygon1, multipoint2, sr, null);
		assertTrue(!res);
		res = disjoint.execute(multipoint2, polygon1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolygonMultiPointTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		MultiPoint multipoint2 = new MultiPoint();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		multipoint2.add(-1, 5);
		multipoint2.add(5, 11);
		multipoint2.add(11, 5);
		multipoint2.add(5, -1);

		boolean res = touches.execute(polygon1, multipoint2, sr, null);
		assertTrue(!res);
		res = touches.execute(multipoint2, polygon1, sr, null);
		assertTrue(!res);

		multipoint2.add(5, 10);

		res = touches.execute(polygon1, multipoint2, sr, null);
		assertTrue(res);
		res = touches.execute(multipoint2, polygon1, sr, null);
		assertTrue(res);

		multipoint2.add(5, 5);
		res = touches.execute(polygon1, multipoint2, sr, null);
		assertTrue(!res);
		res = touches.execute(multipoint2, polygon1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolygonPointTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		Point point2 = new Point();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		point2.setXY(5, 5);

		boolean res = touches.execute(polygon1, point2, sr, null);
		assertTrue(!res);
		res = touches.execute(point2, polygon1, sr, null);
		assertTrue(!res);

		point2.setXY(5, 10);

		res = touches.execute(polygon1, point2, sr, null);
		assertTrue(res);
		res = touches.execute(point2, polygon1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPolygonTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polygon1 and Polygon2 touch at a point
		String str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"rings\":[[[10,10],[10,15],[15,15],[15,10],[10,10]]]}";

		Polygon polygon1 = (Polygon) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polygon polygon2 = (Polygon) (TestCommonMethods.fromJson(str2)
				.getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		boolean res = touches.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = touches.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		// Polygon1 and Polygon2 touch along the boundary
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[10,0],[10,10],[15,10],[15,0],[10,0]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = touches.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = touches.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		// Polygon1 and Polygon2 touch at a corner of Polygon1 and a diagonal of
		// Polygon2
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = touches.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = touches.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		// Polygon1 and Polygon2 do not touch
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[5,5],[5,15],[15,15],[15,5],[5,5]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

		res = touches.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = touches.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		polygon1.setEmpty();
		polygon2.setEmpty();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 1);
		polygon1.lineTo(-1, 0);

		polygon2.startPath(0, 0);
		polygon2.lineTo(0, 1);
		polygon2.lineTo(1, 0);

		res = touches.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = touches.execute(polygon2, polygon1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPolylineTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polygon1 and Polyline2 touch at a point
		String str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"paths\":[[[10,10],[10,15],[15,15],[15,10]]]}";

		Polygon polygon1 = (Polygon) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polyline polyline2 = (Polyline) (TestCommonMethods.fromJson(str2)
				.getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		boolean res = touches.execute(polygon1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polygon1, sr, null);
		assertTrue(res);

		// Polygon1 and Polyline2 overlap along the boundary
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[10,0],[10,10],[15,10],[15,0]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		res = touches.execute(polygon1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polygon1, sr, null);
		assertTrue(res);

		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		res = touches.execute(polygon1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polygon1, sr, null);
		assertTrue(res);

		str1 = "{\"rings\":[[[10,10],[10,0],[0,0],[0,10],[10,10]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		res = touches.execute(polygon1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polygon1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolylinePolylineTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polyline1 and Polyline2 touch at a point
		String str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"paths\":[[[10,10],[10,15],[15,15],[15,10]]]}";

		Polyline polyline1 = (Polyline) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polyline polyline2 = (Polyline) (TestCommonMethods.fromJson(str2)
				.getGeometry());

		boolean res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		// Polyline1 and Polyline2 overlap along the boundary
		str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[10,0],[10,10],[15,10],[15,0],[10,0]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		// Polyline1 and Polyline2 intersect at interiors
		str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		// Polyline1 and Polyline2 touch at an endpoint of Polyline1 and
		// interior of Polyline2 (but Polyline1 is closed)
		str1 = "{\"paths\":[[[10,10],[10,0],[0,0],[0,10],[10,10]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		// Polyline1 and Polyline2 touch at an endpoint of Polyline1 and
		// interior of Polyline2 (same as previous case, but Polyline1 is not
		// closed)
		str1 = "{\"paths\":[[[10,10],[10,0],[0,0],[0,10]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		str1 = "{\"paths\":[[[10,10],[10,0],[0,0],[0,10]],[[1,1],[9,1],[9,9],[1,9],[6, 9]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		polyline1.setEmpty();
		polyline2.setEmpty();

		polyline1.startPath(-2, -2);
		polyline1.lineTo(-1, -1);
		polyline1.lineTo(1, 1);
		polyline1.lineTo(2, 2);

		polyline2.startPath(-2, 2);
		polyline2.lineTo(-1, 1);
		polyline2.lineTo(1, -1);
		polyline2.lineTo(2, -2);

		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.setEmpty();
		polyline2.setEmpty();

		polyline1.startPath(-2, -2);
		polyline1.lineTo(-1, -1);
		polyline1.lineTo(1, 1);
		polyline1.lineTo(2, 2);

		polyline2.startPath(-2, 2);
		polyline2.lineTo(-1, 1);
		polyline2.lineTo(1, -1);

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);

		polyline1.setEmpty();
		polyline2.setEmpty();

		polyline1.startPath(-1, -1);
		polyline1.lineTo(0, 0);
		polyline1.lineTo(1, 1);

		polyline2.startPath(-1, 1);
		polyline2.lineTo(0, 0);
		polyline2.lineTo(1, -1);

		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(0, 1);
		polyline1.lineTo(0, 0);
		polyline2.startPath(0, 1);
		polyline2.lineTo(0, 2);
		polyline2.lineTo(0, 1);

		res = touches.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		res = touches.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolylineMultiPointTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		MultiPoint multipoint2 = new MultiPoint();

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline1.lineTo(4, 2);

		multipoint2.add(1, 1);
		multipoint2.add(2, 2);
		multipoint2.add(3, 0);

		boolean res = touches.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = touches.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.startPath(1, -4);
		polyline1.lineTo(1, -3);
		polyline1.lineTo(1, -2);
		polyline1.lineTo(1, -1);
		polyline1.lineTo(1, 0);
		polyline1.lineTo(1, 1);

		touches.accelerateGeometry(polyline1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);
		res = touches.execute(polyline1, multipoint2, sr, null);
		assertTrue(res);
		res = touches.execute(multipoint2, polyline1, sr, null);
		assertTrue(res);

		multipoint2.add(3, 1);
		res = touches.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = touches.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);

		polyline1.startPath(2, 1);
		polyline1.lineTo(2, -1);

		multipoint2.add(2, 0);

		res = touches.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = touches.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolylineMultiPointCrosses() {
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		MultiPoint multipoint2 = new MultiPoint();

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline1.lineTo(4, 2);

		multipoint2.add(1, 1);
		multipoint2.add(2, 2);
		multipoint2.add(3, 0);
		multipoint2.add(0, 0);

		boolean res = crosses.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = crosses.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.startPath(1, -4);
		polyline1.lineTo(1, -3);
		polyline1.lineTo(1, -2);
		polyline1.lineTo(1, -1);
		polyline1.lineTo(1, 0);
		polyline1.lineTo(1, 1);

		res = crosses.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = crosses.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);

		crosses.accelerateGeometry(polyline1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);

		multipoint2.add(1, 0);
		res = crosses.execute(polyline1, multipoint2, sr, null);
		assertTrue(res);
		res = crosses.execute(multipoint2, polyline1, sr, null);
		assertTrue(res);

		multipoint2.add(3, 1);
		res = crosses.execute(polyline1, multipoint2, sr, null);
		assertTrue(res);
		res = crosses.execute(multipoint2, polyline1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolylinePointTouches() {
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		Point point2 = new Point();

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);

		polyline1.startPath(2, 1);
		polyline1.lineTo(2, -1);

		point2.setXY(2, 0);

		boolean res = touches.execute(polyline1, point2, sr, null);
		assertTrue(res);
		res = touches.execute(point2, polyline1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPolygonOverlaps() {
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polygon1 and Polygon2 touch at a point
		String str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"rings\":[[[10,10],[10,15],[15,15],[15,10],[10,10]]]}";

		Polygon polygon1 = (Polygon) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polygon polygon2 = (Polygon) (TestCommonMethods.fromJson(str2)
				.getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		boolean res = overlaps.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 and Polygon2 touch along the boundary
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[10,0],[10,10],[15,10],[15,0],[10,0]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = overlaps.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 and Polygon2 touch at a corner of Polygon1 and a diagonal of
		// Polygon2
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = overlaps.execute(polygon1, polygon2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 and Polygon2 overlap at the upper right corner
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[5,5],[5,15],[15,15],[15,5],[5,5]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

		res = overlaps.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = overlaps.execute(polygon2, polygon1, sr, null);
		assertTrue(res);

		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[4,4],[6,4],[6,6],[4,6],[4,4],[4,4]]]}";
		str2 = "{\"rings\":[[[1,1],[1,9],[9,9],[9,1],[1,1]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

		res = overlaps.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = overlaps.execute(polygon2, polygon1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPolylineWithin() {
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		Polyline polyline2 = new Polyline();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		polyline2.startPath(5, 0);
		polyline2.lineTo(5, 10);

		boolean res = within.execute(polygon1, polyline2, sr, null);
		assertTrue(!res);
		res = within.execute(polyline2, polygon1, sr, null);
		assertTrue(res);

		polyline2.setEmpty();
		polyline2.startPath(0, 1);
		polyline2.lineTo(0, 9);

		res = within.execute(polyline2, polygon1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testMultiPointMultiPointWithin() {
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		MultiPoint multipoint1 = new MultiPoint();
		MultiPoint multipoint2 = new MultiPoint();

		multipoint1.add(0, 0);
		multipoint1.add(3, 3);
		multipoint1.add(0, 0);
		multipoint1.add(5, 5);
		multipoint1.add(3, 3);
		multipoint1.add(2, 4);
		multipoint1.add(2, 8);

		multipoint2.add(0, 0);
		multipoint2.add(3, 3);
		multipoint2.add(2, 4);
		multipoint2.add(2, 8);
		multipoint2.add(5, 5);

		boolean res = within.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = within.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);

		multipoint2.add(10, 10);
		multipoint2.add(10, 10);

		res = within.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = within.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);

		multipoint1.add(10, 10);
		res = within.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = within.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);

		multipoint1.add(-10, -10);
		res = within.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = within.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolylinePolylineOverlaps() {
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		Polyline polyline2 = new Polyline();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline2.startPath(1, 0);
		polyline2.lineTo(3, 0);
		polyline2.lineTo(1, 1);
		polyline2.lineTo(1, -1);
		wiggleGeometry(polyline1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		boolean res = overlaps.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = overlaps.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline2.startPath(1.9989, 0);
		polyline2.lineTo(2.0011, 0);
		// wiggleGeometry(polyline1, tolerance, 1982);
		// wiggleGeometry(polyline2, tolerance, 511);

		res = overlaps.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = overlaps.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline2.startPath(1.9989, 0);
		polyline2.lineTo(2.0009, 0);
		wiggleGeometry(polyline1, tolerance, 1982);
		wiggleGeometry(polyline2, tolerance, 511);

		res = overlaps.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline2.startPath(0, 0);
		polyline2.lineTo(2, 0);
		polyline2.startPath(0, -1);
		polyline2.lineTo(2, -1);

		res = overlaps.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testMultiPointMultiPointOverlaps() {
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		MultiPoint multipoint1 = new MultiPoint();
		MultiPoint multipoint2 = new MultiPoint();

		multipoint1.add(4, 4);
		multipoint1.add(6, 4);

		multipoint2.add(6, 2);
		multipoint2.add(2, 6);

		boolean res = overlaps.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);

		multipoint1.add(10, 10);
		multipoint2.add(6, 2);

		res = overlaps.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);

		multipoint1.add(6, 2);
		res = overlaps.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = overlaps.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);

		multipoint1.add(2, 6);
		res = overlaps.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);

		multipoint2.add(1, 1);
		res = overlaps.execute(multipoint1, multipoint2, sr, null);
		assertTrue(res);
		res = overlaps.execute(multipoint2, multipoint1, sr, null);
		assertTrue(res);

		multipoint2.add(10, 10);
		multipoint2.add(4, 4);
		multipoint2.add(6, 4);
		res = overlaps.execute(multipoint1, multipoint2, sr, null);
		assertTrue(!res);
		res = overlaps.execute(multipoint2, multipoint1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolygonPolygonWithin() {
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polygon1 is within Polygon2
		String str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"rings\":[[[-1,-1],[-1,11],[11,11],[11,-1],[-1,-1]]]}";

		Polygon polygon1 = (Polygon) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polygon polygon2 = (Polygon) (TestCommonMethods.fromJson(str2)
				.getGeometry());

		boolean res = within.execute(polygon1, polygon2, sr, null);
		assertTrue(res);
		res = within.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 is within Polygon2, and the boundaries intersect
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[4,4],[6,4],[6,6],[4,6],[4,4],[4,4]]]}";
		str2 = "{\"rings\":[[[1,1],[1,9],[9,9],[9,1],[1,1]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = within.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

		// Polygon1 is within Polygon2, and the boundaries intersect
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[-1,0],[-1,11],[11,11],[11,0],[-1,0]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
		wiggleGeometry(polygon1, tolerance, 1982);
		wiggleGeometry(polygon2, tolerance, 511);

		res = within.execute(polygon1, polygon2, sr, null);
		assertTrue(res);

		// Polygon2 is inside of the hole of polygon1
		str1 = "{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[8,2],[2,2]]]}";

		polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
		polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

		res = within.execute(polygon2, polygon1, sr, null);
		assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[10,0],[10,10],[0,10]]]}";
        str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[8,2],[2,2],[8,2],[8,8],[2,8],[2,2]]]}";

        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0]],[[12,8],[12,10],[18,10],[18,8],[12,8]]]}";
        str2 = "{\"paths\":[[[2,2],[2,8],[8,8],[8,2]],[[12,2],[12,4],[18,4],[18,2]]]}";

        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        Polyline polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[4,4],[6,4],[6,6],[4,6],[4,4]]]}";
        str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[8,2],[2,2],[2,8],[8,8],[8,2],[2,2]]]}";

        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(res);

        // Same as above, but winding fill rule
        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[4,4],[6,4],[6,6],[4,6],[4,4]]]}";
        str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[8,2],[2,2],[2,8],[8,8],[8,2],[2,2]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
        polygon1.setFillRule(Polygon.FillRule.enumFillRuleWinding);
        polygon2.setFillRule(Polygon.FillRule.enumFillRuleWinding);

        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
        str2 = "{\"paths\":[[[2,2],[2,2]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[11,11],[11,20],[20,20],[20,11],[11,11]]]}";
        str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[15,15],[8,8],[8,2],[2,2]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[10,10],[10,20],[20,20],[20,10],[10,10]]]}";
        str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[15,15],[8,8],[8,2],[2,2]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
        str2 = "{\"rings\":[[[9.9999999925,4],[9.9999999925,6],[10.0000000075,6],[10.0000000075,4],[9.9999999925,4]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());

        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        res = OperatorOverlaps.local().execute(polygon1, polygon2, sr, null);
        assertTrue(!res);

        res = OperatorTouches.local().execute(polygon1, polygon2, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[10,10],[10,20],[20,20],[20,10],[10,10]]]}";
        str2 = "{\"rings\":[[[2,2],[2,8],[8,8],[15,15],[8,8],[8,2],[2,2]],[[15,5],[15,5],[15,5]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
        str2 = "{\"rings\":[[[2,2],[2,2],[2,2]],[[3,3],[3,3],[3,3]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
        str2 = "{\"rings\":[[[2,2],[2,2],[2,2],[2,2]],[[3,3],[3,3],[3,3],[3,3]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polygon2 = (Polygon) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polygon2, polygon1, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]]]}";
        str2 = "{\"paths\":[[[2,2],[2,2]],[[3,3],[3,3]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[10,10],[10,20],[20,20],[20,10],[10,10]]]}";
        str2 = "{\"paths\":[[[2,2],[2,8]],[[15,5],[15,5]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[10,10],[10,20],[20,20],[20,10],[10,10]]]}";
        str2 = "{\"paths\":[[[2,2],[2,8]],[[15,5],[15,5],[15,5],[15,5]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[10,10],[10,20],[20,20],[20,10],[10,10]]]}";
        str2 = "{\"paths\":[[[2,2],[2,2]],[[15,5],[15,6]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(!res);

        str1 = "{\"rings\":[[[0,0],[0,10],[10,10],[10,0],[0,0]],[[10,10],[10,20],[20,20],[20,10],[10,10]]]}";
        str2 = "{\"paths\":[[[2,2],[2,2],[2,2],[2,2]],[[15,5],[15,6]]]}";
        polygon1 = (Polygon) (TestCommonMethods.fromJson(str1).getGeometry());
        polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
        res = within.execute(polyline2, polygon1, sr, null);
        assertTrue(!res);
	}

	@Test
	public void testPolylinePolylineWithin() {
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		Polyline polyline2 = new Polyline();

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline2.startPath(1.9989, 0);
		polyline2.lineTo(2.0011, 0);

		boolean res = within.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		res = contains.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline2.startPath(1.9989, 0);
		polyline2.lineTo(2.001, 0);

		res = within.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		res = contains.execute(polyline1, polyline2, sr, null);
		assertTrue(res);

		polyline1.setEmpty();
		polyline2.setEmpty();
		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline1.lineTo(3, 0);
		polyline1.lineTo(4, 0);
		polyline1.lineTo(5, 0);
		polyline1.lineTo(6, 0);
		polyline1.lineTo(7, 0);
		polyline1.lineTo(8, 0);

		polyline2.startPath(0, 0);
		polyline2.lineTo(.1, 0);
		polyline2.lineTo(.2, 0);
		polyline2.lineTo(.4, 0);
		polyline2.lineTo(1.1, 0);
		polyline2.lineTo(2.5, 0);

		polyline2.startPath(2.7, 0);
		polyline2.lineTo(4, 0);

		res = within.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		res = contains.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolylineMultiPointWithin() {
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polyline polyline1 = new Polyline();
		MultiPoint multipoint2 = new MultiPoint();

		polyline1.startPath(0, 0);
		polyline1.lineTo(2, 0);
		polyline1.lineTo(4, 2);

		multipoint2.add(1, 0);
		multipoint2.add(2, 0);
		multipoint2.add(3, 1);
		multipoint2.add(2, 0);

		boolean res = within.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = within.execute(multipoint2, polyline1, sr, null);
		assertTrue(res);

		polyline1.startPath(1, -2);
		polyline1.lineTo(1, -1);
		polyline1.lineTo(1, 0);
		polyline1.lineTo(1, 1);

		res = within.execute(polyline1, multipoint2, sr, null);
		assertTrue(!res);
		res = within.execute(multipoint2, polyline1, sr, null);
		assertTrue(res);

		multipoint2.add(1, 2);
		res = within.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);

		multipoint2.add(-1, -1);
		multipoint2.add(4, 2);
		multipoint2.add(0, 0);

		res = within.execute(multipoint2, polyline1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolygonMultiPointWithin() {
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		MultiPoint multipoint2 = new MultiPoint();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		multipoint2.add(5, 0);
		multipoint2.add(5, 10);
		multipoint2.add(5, 5);

		boolean res = within.execute(polygon1, multipoint2, sr, null);
		assertTrue(!res);
		res = within.execute(multipoint2, polygon1, sr, null);
		assertTrue(res);

		multipoint2.add(5, 11);
		res = within.execute(multipoint2, polygon1, sr, null);
		assertTrue(!res);
	}

	@Test
	public void testPolygonPolylineCrosses() {
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		SpatialReference sr = SpatialReference.create(102100);
		@SuppressWarnings("unused")
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		Polygon polygon1 = new Polygon();
		Polyline polyline2 = new Polyline();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		polyline2.startPath(5, -5);
		polyline2.lineTo(5, 15);

		boolean res = crosses.execute(polygon1, polyline2, sr, null);
		assertTrue(res);
		res = crosses.execute(polyline2, polygon1, sr, null);
		assertTrue(res);

		polyline2.setEmpty();
		polyline2.startPath(5, 0);
		polyline2.lineTo(5, 10);

		res = crosses.execute(polygon1, polyline2, sr, null);
		assertTrue(!res);
		res = crosses.execute(polyline2, polygon1, sr, null);
		assertTrue(!res);

		polygon1.setEmpty();
		polyline2.setEmpty();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 8);
		polygon1.lineTo(15, 5);
		polygon1.lineTo(10, 2);
		polygon1.lineTo(10, 0);

		polyline2.startPath(10, 15);
		polyline2.lineTo(10, -5);

		res = crosses.execute(polygon1, polyline2, sr, null);
		assertTrue(res);
		res = crosses.execute(polyline2, polygon1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolylinePolylineCrosses() {
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		SpatialReference sr = SpatialReference.create(102100);
		double tolerance = sr
				.getTolerance(VertexDescription.Semantics.POSITION);

		// Polyline1 and Polyline2 touch at a point
		String str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		String str2 = "{\"paths\":[[[10,10],[10,15],[15,15],[15,10]]]}";

		Polyline polyline1 = (Polyline) (TestCommonMethods.fromJson(str1)
				.getGeometry());
		Polyline polyline2 = (Polyline) (TestCommonMethods.fromJson(str2)
				.getGeometry());

		boolean res = crosses.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = crosses.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		// Polyline1 and Polyline2 intersect at interiors
		str1 = "{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = crosses.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = crosses.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		// Polyline1 and Polyline2 touch at an endpoint of Polyline1 and
		// interior of Polyline2 (but Polyline1 is closed)
		str1 = "{\"paths\":[[[10,10],[10,0],[0,0],[0,10],[10,10]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());
		;

		res = crosses.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = crosses.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		// Polyline1 and Polyline2 touch at an endpoint of Polyline1 and
		// interior of Polyline2 (same as previous case, but Polyline1 is not
		// closed)
		str1 = "{\"paths\":[[[10,10],[10,0],[0,0],[0,10]],[[1,1],[9,1],[9,9],[1,9],[1,1],[1,1]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = crosses.execute(polyline1, polyline2, sr, null);
		assertTrue(!res);
		res = crosses.execute(polyline2, polyline1, sr, null);
		assertTrue(!res);

		str1 = "{\"paths\":[[[10,11],[10,0],[0,0],[0,10]],[[1,1],[9,1],[9,9],[1,9],[6, 9]]]}";
		str2 = "{\"paths\":[[[15,5],[5,15],[15,15],[15,5]]]}";

		polyline1 = (Polyline) (TestCommonMethods.fromJson(str1).getGeometry());
		polyline2 = (Polyline) (TestCommonMethods.fromJson(str2).getGeometry());

		res = crosses.execute(polyline1, polyline2, sr, null);
		assertTrue(res);
		res = crosses.execute(polyline2, polyline1, sr, null);
		assertTrue(res);

		polyline1.setEmpty();
		polyline2.setEmpty();

		polyline1.startPath(-2, -2);
		polyline1.lineTo(-1, -1);
		polyline1.lineTo(1, 1);
		polyline1.lineTo(2, 2);

		polyline2.startPath(-2, 2);
		polyline2.lineTo(-1, 1);
		polyline2.lineTo(1, -1);
		polyline2.lineTo(2, -2);

		res = crosses.execute(polyline2, polyline1, sr, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonEnvelope() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		@SuppressWarnings("unused")
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		OperatorDensifyByLength densify = (OperatorDensifyByLength) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.DensifyByLength));
		SpatialReference sr = SpatialReference.create(4326);

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(equals.execute(envelope, densified, sr, null)); // they
			// cover
			// the
			// same
			// space
			assertTrue(contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // the
			// polygon
			// contains
			// the
			// envelope,
			// but
			// they
			// aren't
			// equal
			assertTrue(contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":15,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // the
			// envelope
			// sticks
			// outside
			// of
			// the
			// polygon
			// but
			// they
			// intersect
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":15,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // the
			// envelope
			// sticks
			// outside
			// of
			// the
			// polygon
			// but
			// they
			// intersect
			// and
			// overlap
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":10,\"ymin\":0,\"xmax\":15,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // the
			// envelope
			// rides
			// the
			// side
			// of
			// the
			// polygon
			// (they
			// touch)
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(contains.execute(densified, envelope, sr, null)); // polygon
			// and
			// envelope
			// cover
			// the
			// same
			// space
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// sticks
			// outside
			// of
			// polygon,
			// but
			// the
			// envelopes
			// are
			// equal
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":15,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // the
			// polygon
			// envelope
			// doesn't
			// contain
			// the
			// envelope,
			// but
			// they
			// intersect
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// point
			// and
			// is
			// on
			// border
			// (i.e.
			// touches)
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":1,\"ymin\":1,\"xmax\":1,\"ymax\":1}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// point
			// and
			// is
			// properly
			// inside
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":-1,\"ymin\":-1,\"xmax\":-1,\"ymax\":-1}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// point
			// and
			// is
			// properly
			// outside
			assertTrue(disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":1,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// line
			// and
			// rides
			// the
			// bottom
			// of
			// the
			// polygon
			// (no
			// interior
			// intersection)
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":1,\"xmax\":1,\"ymax\":1}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// line,
			// touches
			// the
			// border
			// on
			// the
			// inside
			// yet
			// has
			// interior
			// intersection
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":5,\"xmax\":6,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// line,
			// touches
			// the
			// boundary,
			// and
			// is
			// outside
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":6,\"ymin\":5,\"xmax\":7,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// line,
			// and
			// is
			// outside
			assertTrue(disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polygon polygon = (Polygon) (TestCommonMethods
					.fromJson("{\"rings\":[[[0,0],[0,5],[0,10],[10,0],[0,0]]]}")
					.getGeometry());
			Polygon densified = (Polygon) (densify.execute(polygon, 1.0, null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":4,\"ymin\":5,\"xmax\":7,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null)); // envelope
			// degenerate
			// to
			// a
			// line,
			// and
			// crosses
			// polygon
			assertTrue(!disjoint.execute(densified, envelope, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(crosses.execute(envelope, densified, sr, null));
		}
	}

	@Test
	public void testPolylineEnvelope() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		@SuppressWarnings("unused")
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		OperatorDensifyByLength densify = (OperatorDensifyByLength) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.DensifyByLength));

		SpatialReference sr = SpatialReference.create(4326);

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]]]}")
					.getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // polyline
			// straddles
			// the
			// envelope
			// like
			// a hat
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[-10,0],[0,10]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(densified, envelope, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[-11,0],[1,12]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(densified, envelope, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[5,5],[6,6]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // polyline
			// properly
			// inside
			assertTrue(contains.execute(envelope, densified, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[5,5],[10,10]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(contains.execute(envelope, densified, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[-5,5],[15,5]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(envelope, densified, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(crosses.execute(envelope, densified, sr, null));
			assertTrue(crosses.execute(densified, envelope, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[5,5],[5,15]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // polyline
			// slices
			// through
			// the
			// envelope
			// (interior
			// and
			// exterior
			// intersection)
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[5,11],[5,15]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // polyline
			// outside
			// of
			// envelope
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]]]}")
					.getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // polyline
			// straddles
			// the
			// degenerate
			// envelope
			// like
			// a hat
			assertTrue(contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]]]}")
					.getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":-5,\"xmax\":0,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null));
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]]]}")
					.getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 511);
			wiggleGeometry(envelope, 0.00000001, 1982);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // degenerate
			// envelope
			// is at
			// the
			// end
			// point
			// of
			// polyline
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[0,0],[0,5],[0,10],[10,10],[10,0]]]}")
					.getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":5,\"xmax\":0,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // degenerate
			// envelope
			// is at
			// the
			// interior
			// of
			// polyline
			assertTrue(contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[2,-2],[2,2]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // degenerate
			// envelope
			// crosses
			// polyline
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[2,0],[2,2]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // degenerate
			// envelope
			// crosses
			// polyline
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[2,0],[2,2]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":2,\"ymin\":0,\"xmax\":2,\"ymax\":3}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // degenerate
			// envelope
			// contains
			// polyline
			assertTrue(!contains.execute(densified, envelope, sr, null));
			assertTrue(contains.execute(envelope, densified, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[5,5],[6,6]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":5,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, densified, sr, null)); // polyline
			// properly
			// inside
			assertTrue(!contains.execute(envelope, densified, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}

		{
			Polyline polyline = (Polyline) (TestCommonMethods
					.fromJson("{\"paths\":[[[5,5],[5,10]]]}").getGeometry());
			Polyline densified = (Polyline) (densify.execute(polyline, 1.0,
					null));
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":5,\"xmax\":5,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(densified, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(equals.execute(envelope, densified, sr, null)); // polyline
			// properly
			// inside
			assertTrue(contains.execute(envelope, densified, sr, null));
			assertTrue(!disjoint.execute(envelope, densified, sr, null));
			assertTrue(!touches.execute(envelope, densified, sr, null));
			assertTrue(!overlaps.execute(envelope, densified, sr, null));
			assertTrue(!crosses.execute(envelope, densified, sr, null));
		}
	}

	@Test
	public void testMultiPointEnvelope() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		@SuppressWarnings("unused")
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		@SuppressWarnings("unused")
		OperatorDensifyByLength densify = (OperatorDensifyByLength) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.DensifyByLength));

		SpatialReference sr = SpatialReference.create(4326);

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,0],[0,10],[10,10],[10,0]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // all
			// points
			// on
			// boundary
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,0],[0,10],[10,10],[5,5]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // points
			// on
			// boundary
			// and
			// one
			// point
			// in
			// interior
			assertTrue(contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,0],[0,10],[10,10],[5,5],[15,15]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // points
			// on
			// boundary,
			// one
			// interior,
			// one
			// exterior
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,0],[0,10],[10,10],[15,15]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // points
			// on
			// boundary,
			// one
			// exterior
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,-1],[0,11],[11,11],[15,15]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // all
			// points
			// exterior
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,0],[0,10],[10,10],[10,0]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // degenerate
			// envelope
			// slices
			// through
			// some
			// points,
			// but
			// some
			// points
			// are
			// off
			// the
			// line
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,0],[1,10],[10,10],[10,0]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // degenerate
			// envelope
			// slices
			// through
			// some
			// points,
			// but
			// some
			// points
			// are
			// off
			// the
			// line
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,10],[10,10]]}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // degenerate
			// envelopes
			// slices
			// through
			// all
			// the
			// points,
			// and
			// they
			// are
			// at
			// the
			// end
			// points
			// of
			// the
			// line
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[1,10],[9,10]]}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // degenerate
			// envelopes
			// slices
			// through
			// all
			// the
			// points,
			// and
			// they
			// are
			// in
			// the
			// interior
			// of
			// the
			// line
			assertTrue(contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,-1],[0,11],[11,11],[15,15]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // all
			// points
			// exterior
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,-1],[0,11],[11,11],[15,15]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":10,\"ymin\":10,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // all
			// points
			// exterior
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,-1],[0,11],[11,11],[15,15]]}")
					.getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":11,\"ymax\":11}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, multi_point, sr, null)); // all
			// points
			// exterior
			assertTrue(!contains.execute(multi_point, envelope, sr, null));
			assertTrue(!contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}

		{
			MultiPoint multi_point = (MultiPoint) (TestCommonMethods
					.fromJson("{\"points\":[[0,-1],[0,-1]]}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":-1,\"xmax\":0,\"ymax\":-1}")
					.getGeometry());
			wiggleGeometry(multi_point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(equals.execute(envelope, multi_point, sr, null)); // all
			// points
			// exterior
			assertTrue(contains.execute(multi_point, envelope, sr, null));
			assertTrue(contains.execute(envelope, multi_point, sr, null));
			assertTrue(!disjoint.execute(envelope, multi_point, sr, null));
			assertTrue(!touches.execute(envelope, multi_point, sr, null));
			assertTrue(!overlaps.execute(envelope, multi_point, sr, null));
			assertTrue(!crosses.execute(envelope, multi_point, sr, null));
		}
	}

	@Test
	public void testPointEnvelope() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		@SuppressWarnings("unused")
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(4326);

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":5,\"y\":6}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, point, sr, null));
			assertTrue(contains.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(point, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, point, sr, null));
			assertTrue(!touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":0,\"y\":10}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(point, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, point, sr, null));
			assertTrue(touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":0,\"y\":11}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(point, envelope, sr, null));
			assertTrue(disjoint.execute(envelope, point, sr, null));
			assertTrue(!touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":0,\"y\":0}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(point, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, point, sr, null));
			assertTrue(touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":5,\"y\":0}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, point, sr, null));
			assertTrue(contains.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(point, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, point, sr, null));
			assertTrue(!touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":11,\"y\":0}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(!equals.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(envelope, point, sr, null));
			assertTrue(!contains.execute(point, envelope, sr, null));
			assertTrue(disjoint.execute(envelope, point, sr, null));
			assertTrue(!touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}

		{
			Point point = (Point) (TestCommonMethods
					.fromJson("{\"x\":0,\"y\":0}").getGeometry());
			Envelope envelope = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(point, 0.00000001, 1982);
			wiggleGeometry(envelope, 0.00000001, 511);
			assertTrue(equals.execute(envelope, point, sr, null));
			assertTrue(contains.execute(envelope, point, sr, null));
			assertTrue(contains.execute(point, envelope, sr, null));
			assertTrue(!disjoint.execute(envelope, point, sr, null));
			assertTrue(!touches.execute(envelope, point, sr, null));
			assertTrue(!overlaps.execute(envelope, point, sr, null));
			assertTrue(!crosses.execute(envelope, point, sr, null));
		}
	}

	@Test
	public void testEnvelopeEnvelope() {
		OperatorEquals equals = (OperatorEquals) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Equals));
		OperatorContains contains = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
		OperatorDisjoint disjoint = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		OperatorCrosses crosses = (OperatorCrosses) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Crosses));
		@SuppressWarnings("unused")
		OperatorWithin within = (OperatorWithin) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Within));
		OperatorOverlaps overlaps = (OperatorOverlaps) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Overlaps));
		OperatorTouches touches = (OperatorTouches) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Touches));
		SpatialReference sr = SpatialReference.create(4326);

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(equals.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":5,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":5,\"xmax\":15,\"ymax\":15}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(overlaps.execute(env1, env2, sr, null));
			assertTrue(overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":10,\"ymin\":0,\"xmax\":20,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":10,\"ymin\":0,\"xmax\":20,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":10,\"ymin\":0,\"xmax\":20,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":10,\"xmax\":10,\"ymax\":20}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":15,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":-5,\"ymin\":5,\"xmax\":0,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":-5,\"ymin\":5,\"xmax\":5,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(crosses.execute(env1, env2, sr, null));
			assertTrue(crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":3,\"ymin\":5,\"xmax\":5,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":3,\"ymin\":5,\"xmax\":10,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":-5,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(equals.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":15,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":15,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":-5,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(overlaps.execute(env1, env2, sr, null));
			assertTrue(overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":-5,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":-5,\"xmax\":5,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(crosses.execute(env1, env2, sr, null));
			assertTrue(crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":10,\"ymin\":0,\"xmax\":20,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":5,\"ymax\":5}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":5,\"xmax\":5,\"ymax\":5}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":10}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":0,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env1, env2, sr, null));
			assertTrue(touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":0,\"ymin\":0,\"xmax\":10,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(!equals.execute(env1, env2, sr, null));
			assertTrue(!contains.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}

		{
			Envelope env1 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			Envelope env2 = (Envelope) (TestCommonMethods
					.fromJson("{\"xmin\":5,\"ymin\":0,\"xmax\":5,\"ymax\":0}")
					.getGeometry());
			wiggleGeometry(env1, 0.00000001, 1982);
			wiggleGeometry(env2, 0.00000001, 511);
			assertTrue(equals.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env1, env2, sr, null));
			assertTrue(contains.execute(env2, env1, sr, null));
			assertTrue(!disjoint.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env1, env2, sr, null));
			assertTrue(!touches.execute(env2, env1, sr, null));
			assertTrue(!overlaps.execute(env1, env2, sr, null));
			assertTrue(!overlaps.execute(env2, env1, sr, null));
			assertTrue(!crosses.execute(env1, env2, sr, null));
			assertTrue(!crosses.execute(env2, env1, sr, null));
		}
	}

	static void wiggleGeometry(Geometry geometry, double tolerance, int rand) {
		int type = geometry.getType().value();

		if (type == Geometry.GeometryType.Polygon
				|| type == Geometry.GeometryType.Polyline
				|| type == Geometry.GeometryType.MultiPoint) {
			MultiVertexGeometry mvGeom = (MultiVertexGeometry) geometry;
			for (int i = 0; i < mvGeom.getPointCount(); i++) {
				Point2D pt = mvGeom.getXY(i);

				// create random vector and normalize it to 0.49 * tolerance
				Point2D randomV = new Point2D();
				rand = NumberUtils.nextRand(rand);
				randomV.x = 1.0 * rand / NumberUtils.intMax() - 0.5;
				rand = NumberUtils.nextRand(rand);
				randomV.y = 1.0 * rand / NumberUtils.intMax() - 0.5;
				randomV.normalize();
				randomV.scale(0.45 * tolerance);
				pt.add(randomV);
				mvGeom.setXY(i, pt);
			}
		} else if (type == Geometry.GeometryType.Point) {
			Point ptGeom = (Point) (geometry);
			Point2D pt = ptGeom.getXY();
			// create random vector and normalize it to 0.49 * tolerance
			Point2D randomV = new Point2D();
			rand = NumberUtils.nextRand(rand);
			randomV.x = 1.0 * rand / NumberUtils.intMax() - 0.5;
			rand = NumberUtils.nextRand(rand);
			randomV.y = 1.0 * rand / NumberUtils.intMax() - 0.5;
			randomV.normalize();
			randomV.scale(0.45 * tolerance);
			pt.add(randomV);
			ptGeom.setXY(pt);
		} else if (type == Geometry.GeometryType.Envelope) {
			Envelope envGeom = (Envelope) (geometry);
			Envelope2D env = new Envelope2D();
			envGeom.queryEnvelope2D(env);
			double xmin, xmax, ymin, ymax;
			Point2D pt = new Point2D();
			env.queryLowerLeft(pt);
			// create random vector and normalize it to 0.49 * tolerance
			Point2D randomV = new Point2D();
			rand = NumberUtils.nextRand(rand);
			randomV.x = 1.0 * rand / NumberUtils.intMax() - 0.5;
			rand = NumberUtils.nextRand(rand);
			randomV.y = 1.0 * rand / NumberUtils.intMax() - 0.5;
			randomV.normalize();
			randomV.scale(0.45 * tolerance);
			xmin = (pt.x + randomV.x);
			ymin = (pt.y + randomV.y);

			env.queryUpperRight(pt);
			// create random vector and normalize it to 0.49 * tolerance
			rand = NumberUtils.nextRand(rand);
			randomV.x = 1.0 * rand / NumberUtils.intMax() - 0.5;
			rand = NumberUtils.nextRand(rand);
			randomV.y = 1.0 * rand / NumberUtils.intMax() - 0.5;
			randomV.normalize();
			randomV.scale(0.45 * tolerance);
			xmax = (pt.x + randomV.x);
			ymax = (pt.y + randomV.y);

			if (xmin > xmax) {
				double swap = xmin;
				xmin = xmax;
				xmax = swap;
			}

			if (ymin > ymax) {
				double swap = ymin;
				ymin = ymax;
				ymax = swap;
			}

			envGeom.setCoords(xmin, ymin, xmax, ymax);
		}

	}

	@Test
	public void testDisjointRelationFalse() {
		{
			OperatorDisjoint op = (OperatorDisjoint) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Disjoint));
			Envelope env1 = new Envelope(50, 50, 150, 150);
			Envelope env2 = new Envelope(25, 25, 175, 175);
			boolean result = op.execute(env1, env2,
					SpatialReference.create(4326), null);
			assertTrue(!result);
		}
		{
			OperatorIntersects op = (OperatorIntersects) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Intersects));
			Envelope env1 = new Envelope(50, 50, 150, 150);
			Envelope env2 = new Envelope(25, 25, 175, 175);
			boolean result = op.execute(env1, env2,
					SpatialReference.create(4326), null);
			assertTrue(result);
		}
		{
			OperatorContains op = (OperatorContains) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Contains));
			Envelope env1 = new Envelope(100, 175, 200, 225);
			Polyline polyline = new Polyline();
			polyline.startPath(200, 175);
			polyline.lineTo(200, 225);
			polyline.lineTo(125, 200);
			boolean result = op.execute(env1, polyline,
					SpatialReference.create(4326), null);
			assertTrue(result);
		}
		{
			OperatorTouches op = (OperatorTouches) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Touches));
			Envelope env1 = new Envelope(100, 200, 400, 400);
			Polyline polyline = new Polyline();
			polyline.startPath(300, 60);
			polyline.lineTo(300, 200);
			polyline.lineTo(400, 50);
			boolean result = op.execute(env1, polyline,
					SpatialReference.create(4326), null);
			assertTrue(result);
		}

		{
			OperatorTouches op = (OperatorTouches) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Touches));
			Envelope env1 = new Envelope(50, 50, 150, 150);
			Polyline polyline = new Polyline();
			polyline.startPath(100, 20);
			polyline.lineTo(100, 50);
			polyline.lineTo(150, 10);
			boolean result = op.execute(polyline, env1,
					SpatialReference.create(4326), null);
			assertTrue(result);
		}

		{
			OperatorDisjoint op = (OperatorDisjoint) (OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Disjoint));
			Polygon polygon = new Polygon();
			Polyline polyline = new Polyline();
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(10, 10);
			polygon.lineTo(10, 0);
			polyline.startPath(-5, 4);
			polyline.lineTo(5, -6);
			boolean result = op.execute(polyline, polygon,
					SpatialReference.create(4326), null);
			assertTrue(result);
		}
	}

	@Test
	public void testPolylinePolylineRelate() {
        OperatorRelate op = OperatorRelate.local();
        SpatialReference sr = SpatialReference.create(4326);
        boolean res;
        String scl;

        Polyline polyline1 = new Polyline();
        Polyline polyline2 = new Polyline();

        polyline1.startPath(0, 0);
        polyline1.lineTo(1, 1);

        polyline2.startPath(1, 1);
        polyline2.lineTo(2, 0);

        scl = "FF1FT01T2";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "****TF*T*";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "****F****";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "**1*0*T**";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "****1****";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "**T*001*T";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "T********";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "F********";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();

        polyline1.startPath(0, 0);
        polyline1.lineTo(1, 0);

        polyline2.startPath(0, 0);
        polyline2.lineTo(1, 0);

        scl = "1FFFTFFFT";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "1*T*T****";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "1T**T****";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        polyline1.setEmpty();
        polyline2.setEmpty();

        polyline1.startPath(0, 0);
        polyline1.lineTo(0.5, 0.5);
        polyline1.lineTo(1, 1);

        polyline2.startPath(1, 0);
        polyline2.lineTo(0.5, 0.5);
        polyline2.lineTo(0, 1);

        scl = "0F1FFTT0T";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "*T*******";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "*F*F*****";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();

        polyline1.startPath(0, 0);
        polyline1.lineTo(1, 0);

        polyline2.startPath(1, -1);
        polyline2.lineTo(1, 1);

        scl = "FT1TF01TT";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "***T*****";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();

        polyline1.startPath(0, 0);
        polyline1.lineTo(0, 20);
        polyline1.lineTo(20, 20);
        polyline1.lineTo(20, 0);
        polyline1.lineTo(0, 0); // has no boundary

        polyline2.startPath(3, 3);
        polyline2.lineTo(5, 5);

        op.accelerateGeometry(polyline1, sr, Geometry.GeometryAccelerationDegree.enumHot);

        scl = "FF1FFF102";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();

        polyline1.startPath(4, 0);
        polyline1.lineTo(0, 4);
        polyline1.lineTo(4, 8);
        polyline1.lineTo(8, 4);

        polyline2.startPath(8, 1);
        polyline2.lineTo(8, 2);

        op.accelerateGeometry(polyline1, sr, GeometryAccelerationDegree.enumHot);

        scl = "FF1FF0102";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();
        polyline1.startPath(4, 0);
        polyline1.lineTo(0, 4);
        polyline2.startPath(3, 2);
        polyline2.lineTo(3, 2);
        assertTrue(polyline2.getBoundary().isEmpty());

        scl = "******0F*";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline2.lineTo(3, 2);
        assertTrue(polyline2.getBoundary().isEmpty());

        scl = "******0F*";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);
        scl = "******0F*";

        polyline2.lineTo(3, 2);
        assertTrue(polyline2.getBoundary().isEmpty());

        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();
        polyline1.startPath(3, 3);
        polyline1.lineTo(3, 4);
        polyline1.lineTo(3, 3);
        polyline2.startPath(1, 1);
        polyline2.lineTo(1, 1);

        scl = "FF1FFF0F2";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);
        scl = "FF0FFF1F2";
        res = op.execute(polyline2, polyline1, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        polyline2.setEmpty();
        polyline1.startPath(4, 0);
        polyline1.lineTo(0, 4);
        polyline2.startPath(2, 2);
        polyline2.lineTo(2, 2);

        scl = "0F*******";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline2.lineTo(2, 2);

        scl = "0F*******";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);
        scl = "0F*******";
        res = op.execute(polyline1, polyline2, sr, scl, null);
        assertTrue(res);
	}

	@Test
	public void testPolygonPolylineRelate() {
        OperatorRelate op = OperatorRelate.local();
        SpatialReference sr = SpatialReference.create(4326);
        boolean res;
        String scl;

        Polygon polygon1 = new Polygon();
        Polyline polyline2 = new Polyline();

        polygon1.startPath(0, 0);
        polygon1.lineTo(0, 10);
        polygon1.lineTo(10, 10);
        polygon1.lineTo(10, 0);

        polyline2.startPath(-10, 0);
        polyline2.lineTo(0, 0);

        scl = "FF2F01102";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "**1*0110*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "T***T****";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "FF2FT****";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline2.setEmpty();
        polyline2.startPath(0, 0);
        polyline2.lineTo(10, 0);

        scl = "**21*1FF*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "F*21*1FF*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "0**1*1FF*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(!res);

        scl = "F**1*1TF*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(!res);

        polyline2.setEmpty();
        polyline2.startPath(1, 1);
        polyline2.lineTo(5, 5);

        scl = "TT2******";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "1T2FF1FF*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        scl = "1T1FF1FF*";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(!res);

        polyline2.setEmpty();
        polyline2.startPath(5, 5);
        polyline2.lineTo(15, 5);

        scl = "1T20F*T0T";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        polygon1.setEmpty();
        polyline2.setEmpty();

        polygon1.startPath(2, 0);
        polygon1.lineTo(0, 2);
        polygon1.lineTo(2, 4);
        polygon1.lineTo(4, 2);

        polyline2.startPath(1, 2);
        polyline2.lineTo(3, 2);

        op.accelerateGeometry(polygon1, sr, GeometryAccelerationDegree.enumHot);
        scl = "TTTFF****";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        polyline2.setEmpty();
        polyline2.startPath(5, 2);
        polyline2.lineTo(7, 2);
        scl = "FF2FFT***";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        polygon1.setEmpty();
        polyline2.setEmpty();
        polygon1.startPath(0, 0);
        polygon1.lineTo(0, 1);
        polygon1.lineTo(1, 0);
        polyline2.startPath(0, 10);
        polyline2.lineTo(0, 9);
        polyline2.startPath(10, 0);
        polyline2.lineTo(9, 0);
        polyline2.startPath(0, -10);
        polyline2.lineTo(0, -9);
        scl = "**2******";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);

        polygon1.setEmpty();
        polyline2.setEmpty();
        polygon1.startPath(0, 0);
        polygon1.lineTo(0, 1);
        polygon1.lineTo(0, 0);
        polyline2.startPath(0, 10);
        polyline2.lineTo(0, 9);
        scl = "**1******";
        res = op.execute(polygon1, polyline2, sr, scl, null);
        assertTrue(res);
	}

	@Test
	public void testPolygonPolygonRelate() {
		OperatorRelate op = (OperatorRelate) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Relate));
		SpatialReference sr = SpatialReference.create(4326);
		boolean res;
		String scl;

		Polygon polygon1 = new Polygon();
		Polygon polygon2 = new Polygon();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		polygon2.startPath(15, 0);
		polygon2.lineTo(15, 10);
		polygon2.lineTo(25, 10);
		polygon2.lineTo(25, 0);

		scl = "FFTFFT21T";
		res = op.execute(polygon1, polygon2, sr, scl, null);
		assertTrue(res);

		scl = "FFTFFT11T";
		res = op.execute(polygon1, polygon2, sr, scl, null);
		assertTrue(!res);

		polygon2.setEmpty();
		polygon2.startPath(5, 0);
		polygon2.lineTo(5, 10);
		polygon2.lineTo(15, 10);
		polygon2.lineTo(15, 0);

		scl = "21TT1121T";
		res = op.execute(polygon1, polygon2, sr, scl, null);
		assertTrue(res);

		polygon2.setEmpty();
		polygon2.startPath(1, 1);
		polygon2.lineTo(1, 9);
		polygon2.lineTo(9, 9);
		polygon2.lineTo(9, 1);

		scl = "212FF1FFT";
		res = op.execute(polygon1, polygon2, sr, scl, null);
		assertTrue(res);

        polygon1.setEmpty();
        polygon2.setEmpty();
        polygon1.startPath(3, 3);
        polygon1.lineTo(3, 4);
        polygon1.lineTo(3, 3);
        polygon2.startPath(1, 1);
        polygon2.lineTo(1, 1);

        scl = "FF1FFF0F2";
        res = op.execute(polygon1, polygon2, sr, scl, null);
        assertTrue(res);
        scl = "FF0FFF1F2";
        res = op.execute(polygon2, polygon1, sr, scl, null);
        assertTrue(res);

        polygon1.setEmpty();
        polygon2.setEmpty();
        polygon1.startPath(0, 0);
        polygon1.lineTo(0, 100);
        polygon1.lineTo(100, 100);
        polygon1.lineTo(100, 0);
        polygon2.startPath(50, 50);
        polygon2.lineTo(50, 50);
        polygon2.lineTo(50, 50);

        op.accelerateGeometry(polygon1, sr, GeometryAccelerationDegree.enumHot);

        scl = "0F2FF1FF2";
        res = op.execute(polygon1, polygon2, sr, scl, null);
        assertTrue(res);

        polygon2.lineTo(51, 50);
        scl = "1F2FF1FF2";
        res = op.execute(polygon1, polygon2, sr, scl, null);
        assertTrue(res);
	}

	@Test
	public void testMultiPointPointRelate() {
		OperatorRelate op = (OperatorRelate) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Relate));
		SpatialReference sr = SpatialReference.create(4326);
		boolean res;
		String scl;

		MultiPoint m1 = new MultiPoint();
		Point p2 = new Point();

		m1.add(0, 0);
		p2.setXY(0, 0);

		scl = "T*F***F**";
		res = op.execute(m1, p2, sr, scl, null);
		assertTrue(res);

		scl = "T*T***F**";
		res = op.execute(m1, p2, sr, scl, null);
		assertTrue(!res);

		m1.add(1, 1);
		res = op.execute(m1, p2, sr, scl, null);
		assertTrue(res);

        m1.setEmpty();

        m1.add(1, 1);
        m1.add(2, 2);

        scl = "FF0FFFTF2";
        res = op.execute(m1, p2, sr, scl, null);
        assertTrue(res);
	}

	@Test
	public void testPointPointRelate() {
		OperatorRelate op = (OperatorRelate) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Relate));
		SpatialReference sr = SpatialReference.create(4326);
		boolean res;
		String scl;

		Point p1 = new Point();
		Point p2 = new Point();

		p1.setXY(0, 0);
		p2.setXY(0, 0);

		scl = "T********";
		res = op.execute(p1, p2, sr, scl, null);
		assertTrue(res);

		p1.setXY(0, 0);
		p2.setXY(1, 0);
		res = op.execute(p1, p2, null, scl, null);
		assertTrue(!res);

        p1.setEmpty();
        p2.setEmpty();
        scl = "*********";
        res = op.execute(p1, p2, null, scl, null);
        assertTrue(res);
        scl = "FFFFFFFFF";
        res = op.execute(p1, p2, null, scl, null);
        assertTrue(res);
        scl = "FFFFFFFFT";
        res = op.execute(p1, p2, null, scl, null);
        assertTrue(!res);
	}

	@Test
	public void testPolygonMultiPointRelate() {
		OperatorRelate op = (OperatorRelate) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Relate));
		SpatialReference sr = SpatialReference.create(4326);
		boolean res;
		String scl;

		Polygon polygon1 = new Polygon();
		MultiPoint multipoint2 = new MultiPoint();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 10);
		polygon1.lineTo(10, 10);
		polygon1.lineTo(10, 0);

		multipoint2.add(0, 0);
		multipoint2.add(5, 5);

		scl = "TFT0F1FFT";
		res = op.execute(polygon1, multipoint2, sr, scl, null);
		assertTrue(res);

		scl = "T0FFFFT1T"; // transpose of above
		res = op.execute(multipoint2, polygon1, sr, scl, null);
		assertTrue(res);

		multipoint2.add(11, 11);

		scl = "TFT0F10FT";
		res = op.execute(polygon1, multipoint2, sr, scl, null);
		assertTrue(res);

		multipoint2.add(0, 5);

		scl = "TFT0F10FT";
		res = op.execute(polygon1, multipoint2, sr, scl, null);
		assertTrue(res);

		scl = "TFF0F10FT";
		res = op.execute(polygon1, multipoint2, sr, scl, null);
		assertTrue(!res);

		polygon1.setEmpty();
		multipoint2.setEmpty();

		polygon1.startPath(0, 0);
		polygon1.lineTo(0, 20);
		polygon1.lineTo(20, 20);
		polygon1.lineTo(20, 0);

		multipoint2.add(3, 3);
		multipoint2.add(5, 5);

		op.accelerateGeometry(polygon1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);

		scl = "TF2FF****";
		res = op.execute(polygon1, multipoint2, sr, scl, null);
		assertTrue(res);

		polygon1.setEmpty();
		multipoint2.setEmpty();

		polygon1.startPath(4, 0);
		polygon1.lineTo(0, 4);
		polygon1.lineTo(4, 8);
		polygon1.lineTo(8, 4);

		multipoint2.add(8, 1);
		multipoint2.add(8, 2);

		op.accelerateGeometry(polygon1, sr,
				Geometry.GeometryAccelerationDegree.enumHot);

		scl = "FF2FF10F2";
		res = op.execute(polygon1, multipoint2, sr, scl, null);
		assertTrue(res);
	}

	@Test
	public void testPolygonPointRelate() {
		OperatorRelate op = (OperatorRelate) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Relate));
		SpatialReference sr = SpatialReference.create(4326);
		boolean res;
		String scl;

		Polygon polygon = new Polygon();
		Point point = new Point();

		polygon.startPath(0, 0);
		polygon.lineTo(0, 10);
		polygon.lineTo(10, 10);
		polygon.lineTo(10, 0);

		point.setXY(0, 0);

		scl = "FF20FTFFT";
		res = op.execute(polygon, point, sr, scl, null);
		assertTrue(res);

        polygon.setEmpty();
        polygon.startPath(0, 0);
        polygon.lineTo(0, 0);
        polygon.lineTo(0, 0);
        scl = "0FFFFFFF2";
        res = op.execute(polygon, point, sr, scl, null);
        assertTrue(res);

        polygon.setEmpty();
        polygon.startPath(0, 0);
        polygon.lineTo(0, 1);
        polygon.lineTo(0, 0);
        scl = "0F1FFFFF2";
        res = op.execute(polygon, point, sr, scl, null);
        assertTrue(res);

        point.setXY(-1, 0);

        scl = "FF1FFF0F2";
        res = op.execute(polygon, point, sr, scl, null);
        assertTrue(res);

        polygon.setEmpty();
        polygon.startPath(0, 0);
        polygon.lineTo(0, 10);
        polygon.lineTo(0, 0);
        scl = "FF1FFFTFT";
        res = op.execute(polygon, point, sr, scl, null);
        assertTrue(res);

        polygon.setEmpty();
        polygon.startPath(0, 0);
        polygon.lineTo(0, 0);
        polygon.lineTo(0, 0);
        scl = "FF0FFF0F2";
        res = op.execute(polygon, point, sr, scl, null);
        assertTrue(res);
	}

	@Test
	public void testPolylineMultiPointRelate() {
        OperatorRelate op = OperatorRelate.local();
        SpatialReference sr = SpatialReference.create(4326);
        boolean res;
        String scl;

        Polyline polyline1 = new Polyline();
        MultiPoint multipoint2 = new MultiPoint();

        polyline1.startPath(0, 0);
        polyline1.lineTo(10, 0);

        multipoint2.add(0, 0);
        multipoint2.add(5, 5);

        scl = "FF10F00F2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        multipoint2.add(5, 0);

        scl = "0F10F00F2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        scl = "0F11F00F2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(!res);

        polyline1.setEmpty();
        multipoint2.setEmpty();

        polyline1.startPath(4, 0);
        polyline1.lineTo(0, 4);
        polyline1.lineTo(4, 8);
        polyline1.lineTo(8, 4);
        polyline1.lineTo(4, 0); // has no boundary

        multipoint2.add(8, 1);
        multipoint2.add(8, 2);

        op.accelerateGeometry(polyline1, sr, GeometryAccelerationDegree.enumHot);

        scl = "FF1FFF0F2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        multipoint2.setEmpty();

        polyline1.startPath(4, 0);
        polyline1.lineTo(4, 0);

        multipoint2.add(8, 1);
        multipoint2.add(8, 2);

        scl = "FF0FFF0F2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        multipoint2.add(-2, 0);
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        op.accelerateGeometry(polyline1, sr, GeometryAccelerationDegree.enumHot);
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        multipoint2.setEmpty();

        polyline1.startPath(10, 10);
        polyline1.lineTo(10, 10);
        multipoint2.add(10, 10);

        scl = "0FFFFFFF2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        polyline1.startPath(12, 12);
        polyline1.lineTo(12, 12);

        scl = "0F0FFFFF2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);

        polyline1.setEmpty();
        multipoint2.setEmpty();

        polyline1.startPath(10, 10);
        polyline1.lineTo(10, 10);
        multipoint2.add(0, 0);

        scl = "FF0FFF0F2";
        res = op.execute(polyline1, multipoint2, sr, scl, null);
        assertTrue(res);
	}

	@Test
	public void testMultiPointMultipointRelate() {
		OperatorRelate op = (OperatorRelate) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Relate));
		SpatialReference sr = SpatialReference.create(4326);
		boolean res;
		String scl;

		MultiPoint multipoint1 = new MultiPoint();
		MultiPoint multipoint2 = new MultiPoint();

		multipoint1.add(0, 0);

		multipoint2.add(0, 0);

		scl = "TFFFFFFF2";
		res = op.execute(multipoint1, multipoint2, sr, scl, null);
		assertTrue(res);

		multipoint2.add(5, 5);

		scl = "TFFFFFTF2";
		res = op.execute(multipoint1, multipoint2, sr, scl, null);
		assertTrue(res);

		multipoint1.add(-5, 0);

		scl = "0FTFFFTF2";
		res = op.execute(multipoint1, multipoint2, sr, scl, null);
		assertTrue(res);

		res = GeometryEngine.relate(multipoint1, multipoint2, sr, scl);
		assertTrue(res);

        multipoint1.setEmpty();
        multipoint2.setEmpty();

        multipoint1.add(0, 0);
        multipoint2.add(1, 1);

        scl = "FFTFFF0FT";
        res = op.execute(multipoint1, multipoint2, sr, scl, null);
        assertTrue(res);
	}

    @Test
    public void testPolylinePointRelate()
    {
        OperatorRelate op = OperatorRelate.local();
        SpatialReference sr = SpatialReference.create(4326);
        boolean res;
        String scl;

        Polyline polyline = new Polyline();
        Point point = new Point();

        polyline.startPath(0, 2);
        polyline.lineTo(0, 4);

        point.setXY(0, 3);

        scl = "0F1FF0FF2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        point.setXY(1, 3);

        scl = "FF1FF00F2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        polyline.lineTo(4, 4);
        polyline.lineTo(4, 2);
        polyline.lineTo(0, 2); // no bounadry
        point.setXY(0, 3);

        scl = "0F1FFFFF2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        scl = "0F1FFFFF2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        point.setXY(1, 3);

        scl = "FF1FFF0F2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        point.setXY(10, 10);

        scl = "FF1FFF0F2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        polyline.setEmpty();
        point.setEmpty();

        polyline.startPath(10, 10);
        polyline.lineTo(10, 10);
        point.setXY(10, 10);

        scl = "0FFFFFFF2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        polyline.startPath(12, 12);
        polyline.lineTo(12, 12);

        scl = "0F0FFFFF2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);

        polyline.setEmpty();
        point.setEmpty();

        polyline.startPath(10, 10);
        polyline.lineTo(10, 10);
        point.setXY(0, 0);

        scl = "FF0FFF0F2";
        res = op.execute(polyline, point, sr, scl, null);
        assertTrue(res);
    }

	@Test
	public void testCrosses_github_issue_40() {
		// Issue 40: Acceleration without a spatial reference changes the result
		// of relation operators
		Geometry geom1 = OperatorImportFromWkt.local().execute(0,
				Geometry.Type.Unknown, "LINESTRING (2 0, 2 3)", null);
		Geometry geom2 = OperatorImportFromWkt.local().execute(0,
				Geometry.Type.Unknown, "POLYGON ((1 1, 4 1, 4 4, 1 4, 1 1))",
				null);
		boolean answer1 = OperatorCrosses.local().execute(geom1, geom2, null,
				null);
		assertTrue(answer1);
		OperatorCrosses.local().accelerateGeometry(geom1, null,
				GeometryAccelerationDegree.enumHot);
		boolean answer2 = OperatorCrosses.local().execute(geom1, geom2, null,
				null);
		assertTrue(answer2);
	}
	
	@Test
	public void testDisjointCrash() {
		Polygon g1 = new Polygon();
		g1.addEnvelope(Envelope2D.construct(0,  0,  10,  10), false);
		Polygon g2 = new Polygon();
		g2.addEnvelope(Envelope2D.construct(10,  1,  21,  21), false);
		g1 = (Polygon)OperatorDensifyByLength.local().execute(g1, 0.1, null);
		OperatorDisjoint.local().accelerateGeometry(g1, SpatialReference.create(4267), GeometryAccelerationDegree.enumHot);
		boolean res = OperatorDisjoint.local().execute(g1, g2, SpatialReference.create(4267), null);
		assertTrue(!res);
	}
	
	@Test
	public void testDisjointFail() {
		MapGeometry geometry1 = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, "{\"paths\":[[[3,3],[3,3]]],\"spatialReference\":{\"wkid\":4326}}");
		MapGeometry geometry2 = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, "{\"rings\":[[[2,2],[2,4],[4,4],[4,2],[2,2]]],\"spatialReference\":{\"wkid\":4326}}");
		OperatorDisjoint.local().accelerateGeometry(geometry1.getGeometry(), geometry1.getSpatialReference(), GeometryAccelerationDegree.enumMedium);
		boolean res = OperatorDisjoint.local().execute(geometry1.getGeometry(), geometry2.getGeometry(), geometry1.getSpatialReference(), null);
		assertTrue(!res);
	}
}
