/*
 Copyright 1995-2015 Esri

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

final class PointInPolygonHelper {

	private Point2D m_inputPoint;
	private int m_windnum;
	private SegmentBuffer[] m_monotoneParts = null;
	private double[] m_xOrds = null;
	private double m_tolerance;
	private double m_toleranceSqr;
	private double m_miny;
	private double m_maxy;
	private boolean m_bAlternate;
	private boolean m_bTestBorder;
	private boolean m_bBreak;
	private boolean m_bPointInAnyOuterRingTest;

	private int result() {
		return m_windnum != 0 ? 1 : 0;
	}

	private boolean _testBorder(Segment seg) {
		double t = seg.getClosestCoordinate(m_inputPoint, false);
		Point2D pt = seg.getCoord2D(t);
		if (Point2D.sqrDistance(pt, m_inputPoint) <= m_toleranceSqr) {
			return true;
		}
		return false;
	}

	private void doOne(Segment seg) {
		if (!m_bTestBorder) {
			// test if point is on the boundary
			if (m_bAlternate && m_inputPoint.isEqual(seg.getStartXY())
					|| m_inputPoint.isEqual(seg.getEndXY())) {// test if the
																// point
																// coincides
																// with a vertex
				m_bBreak = true;
				return;
			}
		}

		if (seg.getStartY() == m_inputPoint.y
				&& seg.getStartY() == seg.getEndY()) {// skip horizontal
														// segments. test if the
														// point lies on a
														// horizontal segment
			if (m_bAlternate && !m_bTestBorder) {
				double minx = Math.min(seg.getStartX(), seg.getEndX());
				double maxx = Math.max(seg.getStartX(), seg.getEndX());
				if (m_inputPoint.x > minx && m_inputPoint.x < maxx)
					m_bBreak = true;
			}

			return;// skip horizontal segments
		}

		boolean bToTheRight = false;
		double maxx = Math.max(seg.getStartX(), seg.getEndX());
		if (m_inputPoint.x > maxx) {
			bToTheRight = true;
		} else {
			if (m_inputPoint.x >= Math.min(seg.getStartX(), seg.getEndX())) {
				int n = seg.intersectionWithAxis2D(true, m_inputPoint.y,
						m_xOrds, null);
				bToTheRight = n > 0 && m_xOrds[0] <= m_inputPoint.x;
			}
		}

		if (bToTheRight) {
			// to prevent double counting, when the ray crosses a vertex, count
			// only the segments that are below the ray.
			if (m_inputPoint.y == seg.getStartXY().y) {
				if (m_inputPoint.y < seg.getEndXY().y)
					return;
			} else if (m_inputPoint.y == seg.getEndXY().y) {
				if (m_inputPoint.y < seg.getStartXY().y)
					return;
			}

			if (m_bAlternate)
				m_windnum ^= 1;
			else
				m_windnum += (seg.getStartXY().y > seg.getEndXY().y) ? 1 : -1;
		}
	}

	public PointInPolygonHelper(boolean bFillRule_Alternate,
			Point2D inputPoint, double tolerance) {
		// //_ASSERT(tolerance >= 0);
		m_inputPoint = inputPoint;
		m_miny = inputPoint.y - tolerance;
		m_maxy = inputPoint.y + tolerance;
		m_windnum = 0;
		m_bAlternate = bFillRule_Alternate;
		m_tolerance = tolerance;
		m_toleranceSqr = tolerance * tolerance;
		m_bTestBorder = tolerance != 0;//
		m_bBreak = false;
	}

	private boolean processSegment(Segment segment) {
		Envelope1D yrange = segment.queryInterval(
				(int) VertexDescription.Semantics.POSITION, 1);
		if (yrange.vmin > m_maxy || yrange.vmax < m_miny) {
			return false;
		}

		if (m_bTestBorder && _testBorder(segment))
			return true;

		if (yrange.vmin > m_inputPoint.y || yrange.vmax < m_inputPoint.y) {
			return false;
		}

		if (m_monotoneParts == null)
			m_monotoneParts = new SegmentBuffer[5];
		if (m_xOrds == null)
			m_xOrds = new double[3];

		int nparts = segment.getYMonotonicParts(m_monotoneParts);
		if (nparts > 0) {// the segment is a curve and has been broken in
							// ymonotone parts
			for (int i = 0; i < nparts; i++) {
				Segment part = m_monotoneParts[i].get();
				doOne(part);
				if (m_bBreak)
					return true;
			}
		} else {// the segment is a line or it is y monotone curve
			doOne(segment);
			if (m_bBreak)
				return true;
		}

		return false;
	}

	private static int _isPointInPolygonInternal(Polygon inputPolygon,
			Point2D inputPoint, double tolerance) {

		boolean bAltenate = inputPolygon.getFillRule() == Polygon.FillRule.enumFillRuleOddEven;
		PointInPolygonHelper helper = new PointInPolygonHelper(bAltenate,
				inputPoint, tolerance);
		MultiPathImpl mpImpl = (MultiPathImpl) inputPolygon._getImpl();
		SegmentIteratorImpl iter = mpImpl.querySegmentIterator();
		while (iter.nextPath()) {
			while (iter.hasNextSegment()) {
				Segment segment = iter.nextSegment();
				if (helper.processSegment(segment))
					return -1; // point on boundary
			}
		}

		return helper.result();
	}

	private static int _isPointInPolygonInternalWithQuadTree(
			Polygon inputPolygon, QuadTreeImpl quadTree, Point2D inputPoint,
			double tolerance) {
		Envelope2D envPoly = new Envelope2D();
		inputPolygon.queryLooseEnvelope(envPoly);
		envPoly.inflate(tolerance, tolerance);

		boolean bAltenate = inputPolygon.getFillRule() == Polygon.FillRule.enumFillRuleOddEven;
		PointInPolygonHelper helper = new PointInPolygonHelper(bAltenate,
				inputPoint, tolerance);

		MultiPathImpl mpImpl = (MultiPathImpl) inputPolygon._getImpl();
		SegmentIteratorImpl iter = mpImpl.querySegmentIterator();
		Envelope2D queryEnv = new Envelope2D();
		queryEnv.setCoords(envPoly);
		queryEnv.xmax = inputPoint.x + tolerance;// no need to query segments to
													// the right of the point.
													// Only segments to the left
													// matter.
		queryEnv.ymin = inputPoint.y - tolerance;
		queryEnv.ymax = inputPoint.y + tolerance;
		QuadTreeImpl.QuadTreeIteratorImpl qiter = quadTree.getIterator(
				queryEnv, tolerance);
		for (int qhandle = qiter.next(); qhandle != -1; qhandle = qiter.next()) {
			iter.resetToVertex(quadTree.getElement(qhandle));
			if (iter.hasNextSegment()) {
				Segment segment = iter.nextSegment();
				if (helper.processSegment(segment))
					return -1; // point on boundary
			}
		}

		return helper.result();
	}

	public static int isPointInPolygon(Polygon inputPolygon,
			Point2D inputPoint, double tolerance) {
		if (inputPolygon.isEmpty())
			return 0;

		Envelope2D env = new Envelope2D();
		inputPolygon.queryLooseEnvelope(env);
		env.inflate(tolerance, tolerance);
		if (!env.contains(inputPoint))
			return 0;

		MultiPathImpl mpImpl = (MultiPathImpl) inputPolygon._getImpl();
		GeometryAccelerators accel = mpImpl._getAccelerators();
		if (accel != null) {
			// geometry has spatial indices built. Try using them.
			RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
			if (rgeom != null) {
				RasterizedGeometry2D.HitType hit = rgeom.queryPointInGeometry(
						inputPoint.x, inputPoint.y);
				if (hit == RasterizedGeometry2D.HitType.Inside)
					return 1;
				else if (hit == RasterizedGeometry2D.HitType.Outside)
					return 0;
			}

			QuadTreeImpl qtree = accel.getQuadTree();
			if (qtree != null) {
				return _isPointInPolygonInternalWithQuadTree(inputPolygon,
						qtree, inputPoint, tolerance);
			}
		}

		return _isPointInPolygonInternal(inputPolygon, inputPoint, tolerance);
	}

	static int isPointInPolygon(Polygon inputPolygon, double inputPointXVal,
			double inputPointYVal, double tolerance) {
		if (inputPolygon.isEmpty())
			return 0;

		Envelope2D env = new Envelope2D();
		inputPolygon.queryLooseEnvelope(env);
		env.inflate(tolerance, tolerance);
		if (!env.contains(inputPointXVal, inputPointYVal))
			return 0;

		MultiPathImpl mpImpl = (MultiPathImpl) inputPolygon._getImpl();
		GeometryAccelerators accel = mpImpl._getAccelerators();
		if (accel != null) {
			RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
			if (rgeom != null) {
				RasterizedGeometry2D.HitType hit = rgeom.queryPointInGeometry(
						inputPointXVal, inputPointYVal);
				if (hit == RasterizedGeometry2D.HitType.Inside)
					return 1;
				else if (hit == RasterizedGeometry2D.HitType.Outside)
					return 0;
			}
		}

		return _isPointInPolygonInternal(inputPolygon, new Point2D(
				inputPointXVal, inputPointYVal), tolerance);
	}

	public static int isPointInRing(MultiPathImpl inputPolygonImpl, int iRing,
			Point2D inputPoint, double tolerance, QuadTree quadTree) {
		Envelope2D env = new Envelope2D();
		inputPolygonImpl.queryLooseEnvelope2D(env);
		env.inflate(tolerance, tolerance);
		if (!env.contains(inputPoint))
			return 0;

		boolean bAltenate = true;
		PointInPolygonHelper helper = new PointInPolygonHelper(bAltenate,
				inputPoint, tolerance);

		if (quadTree != null) {
			Envelope2D queryEnv = new Envelope2D();
			queryEnv.setCoords(env);
			queryEnv.xmax = inputPoint.x + tolerance;// no need to query
														// segments to
														// the right of the
														// point.
														// Only segments to the
														// left
														// matter.
			queryEnv.ymin = inputPoint.y - tolerance;
			queryEnv.ymax = inputPoint.y + tolerance;
			SegmentIteratorImpl iter = inputPolygonImpl.querySegmentIterator();
			QuadTree.QuadTreeIterator qiter = quadTree.getIterator(queryEnv,
					tolerance);

			for (int qhandle = qiter.next(); qhandle != -1; qhandle = qiter
					.next()) {
				iter.resetToVertex(quadTree.getElement(qhandle), iRing);
				if (iter.hasNextSegment()) {
					if (iter.getPathIndex() != iRing)
						continue;

					Segment segment = iter.nextSegment();
					if (helper.processSegment(segment))
						return -1; // point on boundary
				}
			}

			return helper.result();
		} else {
			SegmentIteratorImpl iter = inputPolygonImpl.querySegmentIterator();
			iter.resetToPath(iRing);

			if (iter.nextPath()) {
				while (iter.hasNextSegment()) {
					Segment segment = iter.nextSegment();
					if (helper.processSegment(segment))
						return -1; // point on boundary
				}
			}

			return helper.result();
		}
	}

	public static int isPointInPolygon(Polygon inputPolygon, Point inputPoint,
			double tolerance) {
		if (inputPoint.isEmpty())
			return 0;

		return isPointInPolygon(inputPolygon, inputPoint.getXY(), tolerance);
	}

	public static int isPointInAnyOuterRing(Polygon inputPolygon,
			Point2D inputPoint, double tolerance) {
		Envelope2D env = new Envelope2D();
		inputPolygon.queryLooseEnvelope(env);
		env.inflate(tolerance, tolerance);
		if (!env.contains(inputPoint))
			return 0;

		// Note:
		// Wolfgang had noted that this could be optimized if the exterior rings
		// have positive area:
		// Only test the positive rings and bail out immediately when in a
		// positive ring.
		// The worst case complexity is still O(n), but on average for polygons
		// with holes, that would be faster.
		// However, that method would not work if polygon is reversed, while the
		// one here works fine same as PointInPolygon.

		boolean bAltenate = false;// use winding in this test
		PointInPolygonHelper helper = new PointInPolygonHelper(bAltenate,
				inputPoint, tolerance);
		MultiPathImpl mpImpl = (MultiPathImpl) inputPolygon._getImpl();
		SegmentIteratorImpl iter = mpImpl.querySegmentIterator();
		while (iter.nextPath()) {
			double ringArea = mpImpl.calculateRingArea2D(iter.getPathIndex());
			boolean bIsHole = ringArea < 0;
			if (!bIsHole) {
				helper.m_windnum = 0;
				while (iter.hasNextSegment()) {
					Segment segment = iter.nextSegment();
					if (helper.processSegment(segment))
						return -1; // point on boundary
				}

				if (helper.m_windnum != 0)
					return 1;
			}
		}

		return helper.result();
	}

	// Tests if Ring1 is inside Ring2.
	// We assume here that the Polygon is Weak Simple. That is if one point of
	// Ring1 is found to be inside of Ring2, then
	// we assume that all of Ring1 is inside Ring2.
	static boolean _isRingInRing2D(MultiPath polygon, int iRing1, int iRing2,
			double tolerance, QuadTree quadTree) {
		MultiPathImpl polygonImpl = (MultiPathImpl) polygon._getImpl();
		SegmentIteratorImpl segIter = polygonImpl.querySegmentIterator();
		segIter.resetToPath(iRing1);
		if (!segIter.nextPath() || !segIter.hasNextSegment())
			throw new GeometryException("corrupted geometry");

		int res = 2;

		while (res == 2 && segIter.hasNextSegment()) {
			Segment segment = segIter.nextSegment();
			Point2D point = segment.getCoord2D(0.5);
			res = PointInPolygonHelper.isPointInRing(polygonImpl, iRing2,
					point, tolerance, quadTree);
		}

		if (res == 2)
			throw GeometryException.GeometryInternalError();
		if (res == 1)
			return true;

		return false;
	}

    static boolean quadTreeWillHelp(Polygon polygon, int c_queries)
    {
        int n = polygon.getPointCount();

        if (n < 16)
            return false;

        double c_build_quad_tree = 2.0; // what's a good constant?
        double c_query_quad_tree = 1.0; // what's a good constant?
        double c_point_in_polygon_brute_force = 1.0; // what's a good constant?

        double c_quad_tree = c_build_quad_tree * n + c_query_quad_tree * (Math.log((double)n) / Math.log(2.0)) * c_queries;
        double c_brute_force = c_point_in_polygon_brute_force * n * c_queries;

        return c_quad_tree < c_brute_force;
    }

}
