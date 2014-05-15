package com.esri.core.geometry;

import com.esri.core.geometry.OperatorOffset.JoinType;
import junit.framework.TestCase;
import org.junit.Test;

public class TestOffset extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testOffsetPoint() {
		try {
			Point point = new Point();
			point.setXY(0, 0);

			OperatorOffset offset = (OperatorOffset) OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Offset);

			Geometry outputGeom = offset.execute(point, null, 2,
					JoinType.Round, 2, 0, null);

			assertNull(outputGeom);
		} catch (Exception ex) {
		}

		try {
			MultiPoint mp = new MultiPoint();
			mp.add(0, 0);
			mp.add(10, 10);

			OperatorOffset offset = (OperatorOffset) OperatorFactoryLocal
					.getInstance().getOperator(Operator.Type.Offset);

			Geometry outputGeom = offset.execute(mp, null, 2, JoinType.Round,
					2, 0, null);

			assertNull(outputGeom);
		} catch (Exception ex) {
		}
	}

	@Test
	public void testOffsetPolyline() {
		for (long i = -5; i <= 5; i++) {
			try {
				OffsetPolyline_(i, JoinType.Round);
			} catch (Exception ex) {
				fail("OffsetPolyline(Round) failure");
			}

			try {
				OffsetPolyline_(i, JoinType.Miter);
			} catch (Exception ex) {
				fail("OffsetPolyline(Miter) failure");
			}

			try {
				OffsetPolyline_(i, JoinType.Bevel);
			} catch (Exception ex) {
				fail("OffsetPolyline(Bevel) failure");
			}

			try {
				OffsetPolyline_(i, JoinType.Square);
			} catch (Exception ex) {
				fail("OffsetPolyline(Square) failure");
			}
		}
	}

	public void OffsetPolyline_(double distance, JoinType joins) {
		Polyline polyline = new Polyline();
		polyline.startPath(0, 0);
		polyline.lineTo(6, 0);
		polyline.lineTo(6, 1);
		polyline.lineTo(4, 1);
		polyline.lineTo(4, 2);
		polyline.lineTo(10, 2);

		OperatorOffset offset = (OperatorOffset) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Offset);

		Geometry outputGeom = offset.execute(polyline, null, distance, joins,
				2, 0, null);

		assertNotNull(outputGeom);
	}

	@Test
	public void testOffsetPolygon() {
		for (long i = -5; i <= 5; i++) {
			try {
				OffsetPolygon_(i, JoinType.Round);
			} catch (Exception ex) {
				fail("OffsetPolyline(Round) failure");
			}

			try {
				OffsetPolygon_(i, JoinType.Miter);
			} catch (Exception ex) {
				fail("OffsetPolyline(Miter) failure");
			}

			try {
				OffsetPolygon_(i, JoinType.Bevel);
			} catch (Exception ex) {
				fail("OffsetPolyline(Bevel) failure");
			}

			try {
				OffsetPolygon_(i, JoinType.Square);
			} catch (Exception ex) {
				fail("OffsetPolyline(Square) failure");
			}
		}
	}

	public void OffsetPolygon_(double distance, JoinType joins) {
		Polygon polygon = new Polygon();
		polygon.startPath(0, 0);
		polygon.lineTo(0, 16);
		polygon.lineTo(16, 16);
		polygon.lineTo(16, 11);
		polygon.lineTo(10, 10);
		polygon.lineTo(10, 12);
		polygon.lineTo(3, 12);
		polygon.lineTo(3, 4);
		polygon.lineTo(10, 4);
		polygon.lineTo(10, 6);
		polygon.lineTo(16, 5);
		polygon.lineTo(16, 0);

		OperatorOffset offset = (OperatorOffset) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Offset);

		Geometry outputGeom = offset.execute(polygon, null, distance, joins, 2,
				0, null);
		System.out.println(GeometryUtils.getJSonStringFromGeometry(outputGeom,
				null));

		assertNotNull(outputGeom);
		if (distance > 2) {
			assertTrue(outputGeom.isEmpty());
		}
	}
}
