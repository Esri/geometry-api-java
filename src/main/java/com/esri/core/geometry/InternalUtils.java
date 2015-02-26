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

import java.util.ArrayList;

final class InternalUtils {

	// p0 and p1 have to be on left/right boundary of fullRange2D (since this
	// fuction can be called recursively, p0 or p1 can also be fullRange2D
	// corners)
	static int addPointsToArray(Point2D p0In, Point2D p1In,
			Point2D[] pointsArray, int idx, Envelope2D fullRange2D,
			boolean clockwise, double densifyDist)// PointerOfArrayOf(Point2D)
													// pointsArray, int idx,
													// Envelope2D fullRange2D,
													// boolean clockwise, double
													// densifyDist)
	{
		Point2D p0 = new Point2D();
		p0.setCoords(p0In);
		Point2D p1 = new Point2D();
		p1.setCoords(p1In);
		fullRange2D._snapToBoundary(p0);
		fullRange2D._snapToBoundary(p1);
		// //_ASSERT((p0.x == fullRange2D.xmin || p0.x == fullRange2D.xmax) &&
		// (p1.x == fullRange2D.xmin || p1.x == fullRange2D.xmax));
		double boundDist0 = fullRange2D._boundaryDistance(p0);
		double boundDist1 = fullRange2D._boundaryDistance(p1);
		if (boundDist1 == 0.0)
			boundDist1 = fullRange2D.getLength();

		if ((p0.x == p1.x || p0.y == p1.y
				&& (p0.y == fullRange2D.ymin || p0.y == fullRange2D.ymax))
				&& (boundDist1 > boundDist0) == clockwise) {
			Point2D delta = new Point2D();
			delta.setCoords(p1.x - p0.x, p1.y - p0.y);
			if (densifyDist != 0)// if (densifyDist)
			{
				long cPoints = (long) (delta._norm(0) / densifyDist);
				if (cPoints > 0) // if (cPoints)
				{
					delta.scale(1.0 / (cPoints + 1));
					for (long i = 0; i < cPoints; i++) {
						p0.add(delta);
						pointsArray[idx++].setCoords(p0.x, p0.y);// ARRAYELEMENT(pointsArray,
																	// idx++).setCoords(p0.x,
																	// p0.y);
					}
				}
			}
		} else {
			int side0 = fullRange2D._envelopeSide(p0);
			int side1 = fullRange2D._envelopeSide(p1);
			// create up to four corner points; the order depends on boolean
			// clockwise
			Point2D corner;
			int deltaSide = clockwise ? 1 : 3; // 3 is equivalent to -1
			do {
				side0 = (side0 + deltaSide) & 3;
				corner = fullRange2D.queryCorner(side0);
				if (densifyDist != 0)// if (densifyDist)
				{
					idx = addPointsToArray(p0, corner, pointsArray, idx,
							fullRange2D, clockwise, densifyDist);
				}
				pointsArray[idx++].setCoords(corner.x, corner.y);// ARRAYELEMENT(pointsArray,
																	// idx++).setCoords(corner.x,
																	// corner.y);
				p0 = corner;
			} while ((side0 & 3) != side1);

			if (densifyDist != 0)// if (densifyDist)
				idx = addPointsToArray(p0, p1, pointsArray, idx, fullRange2D,
						clockwise, densifyDist);
		}

		return idx;
	}

	void shiftPath(MultiPath inputGeom, int iPath, double shift) {
		MultiVertexGeometryImpl vertexGeometryImpl = (MultiVertexGeometryImpl) inputGeom
				._getImpl();
		AttributeStreamOfDbl xyStream = (AttributeStreamOfDbl) vertexGeometryImpl
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);

		int i1 = inputGeom.getPathStart(iPath);
		int i2 = inputGeom.getPathEnd(iPath);
		Point2D pt = new Point2D();

		while (i1 < i2) {
			xyStream.read(i1, pt);
			pt.x += shift;
			xyStream.write(i1, pt);
			i1++;
		}
	}

	static double calculateToleranceFromGeometry(SpatialReference sr,
			Envelope2D env2D, boolean bConservative) {
		double gtolerance = env2D._calculateToleranceFromEnvelope();
		double stolerance = sr != null ? sr
				.getTolerance(VertexDescription.Semantics.POSITION) : 0;
		if (bConservative) {
			gtolerance *= 4;
			stolerance *= 1.1;
		}
		return Math.max(stolerance, gtolerance);
	}

    static double adjust_tolerance_for_TE_clustering(double tol) { return 2.0 * Math.sqrt(2.0) * tol; }

    static double adjust_tolerance_for_TE_cracking(double tol) { return Math.sqrt(2.0) * tol; }
	
	static double calculateToleranceFromGeometry(SpatialReference sr,
			Geometry geometry, boolean bConservative) {
		Envelope2D env2D = new Envelope2D();
		geometry.queryEnvelope2D(env2D);
		return calculateToleranceFromGeometry(sr, env2D, bConservative);
	}

	static double calculateZToleranceFromGeometry(SpatialReference sr,
			Geometry geometry, boolean bConservative) {
		Envelope1D env1D = geometry.queryInterval(
				VertexDescription.Semantics.Z, 0);
		double gtolerance = env1D._calculateToleranceFromEnvelope();
		double stolerance = sr != null ? sr
				.getTolerance(VertexDescription.Semantics.Z) : 0;
		if (bConservative) {
			gtolerance *= 4;
			stolerance *= 1.1;
		}
		return Math.max(stolerance, gtolerance);
	}

	double calculateZToleranceFromGeometry(SpatialReference sr,
			Geometry geometry) {
		Envelope1D env1D = geometry.queryInterval(
				VertexDescription.Semantics.Z, 0);
		double tolerance = env1D._calculateToleranceFromEnvelope();
		return Math
				.max(sr != null ? sr
						.getTolerance(VertexDescription.Semantics.Z) : 0,
						tolerance);
	}

	public static Envelope2D getMergedExtent(Geometry geom1, Envelope2D env2) {
		Envelope2D env1 = new Envelope2D();
		geom1.queryLooseEnvelope2D(env1);
		env1.merge(env2);
		return env1;
	}

	public static Envelope2D getMergedExtent(Geometry geom1, Geometry geom2) {
		Envelope2D env1 = new Envelope2D();
		geom1.queryLooseEnvelope2D(env1);
		Envelope2D env2 = new Envelope2D();
		geom2.queryLooseEnvelope2D(env2);
		env1.merge(env2);
		return env1;
	}

	public static Geometry createGeometry(int gt, VertexDescription vdIn) {
		VertexDescription vd = vdIn;
		if (vd == null)
			vd = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		switch (gt) {
		case Geometry.GeometryType.Point:
			return new Point(vd);
		case Geometry.GeometryType.Line:
			return new Line(vd);
			// case enum_value2(Geometry, GeometryType, enumBezier):
			// break;
			// case enum_value2(Geometry, GeometryType, enumEllipticArc):
			// break;
		case Geometry.GeometryType.Envelope:
			return new Envelope(vd);
		case Geometry.GeometryType.MultiPoint:
			return new MultiPoint(vd);
		case Geometry.GeometryType.Polyline:
			return new Polyline(vd);
		case Geometry.GeometryType.Polygon:
			return new Polygon(vd);
		default:
			throw new GeometryException("invalid argument.");
		}
	}

	static boolean isClockwiseRing(MultiPathImpl polygon, int iring) {
		int high_point_index = polygon.getHighestPointIndex(iring);
		int path_start = polygon.getPathStart(iring);
		int path_end = polygon.getPathEnd(iring);

		Point2D q = polygon.getXY(high_point_index);
		Point2D p, r;

		if (high_point_index == path_start) {
			p = polygon.getXY(path_end - 1);
			r = polygon.getXY(path_start + 1);
		} else if (high_point_index == path_end - 1) {
			p = polygon.getXY(high_point_index - 1);
			r = polygon.getXY(path_start);
		} else {
			p = polygon.getXY(high_point_index - 1);
			r = polygon.getXY(high_point_index + 1);
		}

		int orientation = Point2D.orientationRobust(p, q, r);

		if (orientation == 0)
			return polygon.calculateRingArea2D(iring) > 0.0;

		return orientation == -1;
	}

	static QuadTreeImpl buildQuadTree(MultiPathImpl multipathImpl) {
		Envelope2D extent = new Envelope2D();
		multipathImpl.queryLooseEnvelope2D(extent);
		QuadTreeImpl quad_tree_impl = new QuadTreeImpl(extent, 8);
		int hint_index = -1;
		SegmentIteratorImpl seg_iter = multipathImpl.querySegmentIterator();
		Envelope2D boundingbox = new Envelope2D();
		boolean resized_extent = false;
		
		while (seg_iter.nextPath()) {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();
				int index = seg_iter.getStartPointIndex();
				segment.queryEnvelope2D(boundingbox);
				hint_index = quad_tree_impl.insert(index, boundingbox,
						hint_index);

				if (hint_index == -1) {
					if (resized_extent)
						throw GeometryException.GeometryInternalError();

					// resize extent
					multipathImpl.calculateEnvelope2D(extent, false);
					resized_extent = true;
					quad_tree_impl.reset(extent, 8);
					seg_iter.resetToFirstPath();
					break;
				}
			}
		}

		return quad_tree_impl;
	}

	static QuadTreeImpl buildQuadTree(MultiPathImpl multipathImpl,
			Envelope2D extentOfInterest) {
		Envelope2D extent = new Envelope2D();
		multipathImpl.queryLooseEnvelope2D(extent);
		QuadTreeImpl quad_tree_impl = new QuadTreeImpl(extent, 8);
		int hint_index = -1;
		Envelope2D boundingbox = new Envelope2D();
		SegmentIteratorImpl seg_iter = multipathImpl.querySegmentIterator();

		boolean resized_extent = false;
		while (seg_iter.nextPath()) {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();
				int index = seg_iter.getStartPointIndex();
				segment.queryEnvelope2D(boundingbox);

				if (boundingbox.isIntersecting(extentOfInterest)) {
					hint_index = quad_tree_impl.insert(index, boundingbox,
							hint_index);

					if (hint_index == -1) {
						if (resized_extent)
							throw GeometryException.GeometryInternalError();

						// resize extent
						multipathImpl.calculateEnvelope2D(extent, false);
						resized_extent = true;
						quad_tree_impl.reset(extent, 8);
						seg_iter.resetToFirstPath();
						break;
					}
				}
			}
		}

		return quad_tree_impl;
	}

    static QuadTreeImpl buildQuadTreeForPaths(MultiPathImpl multipathImpl)
    {
        Envelope2D extent = new Envelope2D();
        multipathImpl.queryLooseEnvelope2D(extent);
        if (extent.isEmpty())
            return null;

        QuadTreeImpl quad_tree_impl = new QuadTreeImpl(extent, 8);
        int hint_index = -1;
        Envelope2D boundingbox = new Envelope2D();

        boolean resized_extent = false;
        do
        {
            for (int ipath = 0, npaths = multipathImpl.getPathCount(); ipath < npaths; ipath++)
            {
                multipathImpl.queryPathEnvelope2D(ipath, boundingbox);
                hint_index = quad_tree_impl.insert(ipath, boundingbox, hint_index);

                if (hint_index == -1)
                {
                    if (resized_extent)
                        throw GeometryException.GeometryInternalError();

                    //This is usually happens because esri shape buffer contains geometry extent which is slightly different from the true extent.
                    //Recalculate extent
                    multipathImpl.calculateEnvelope2D(extent, false);
                    resized_extent = true;
                    quad_tree_impl.reset(extent, 8);
                    break; //break the for loop
                }
                else
                {
                    resized_extent = false;
                }
            }

        } while(resized_extent);

        return quad_tree_impl;
    }

	static QuadTreeImpl buildQuadTree(MultiPointImpl multipointImpl) {
		Envelope2D extent = new Envelope2D();
		multipointImpl.queryLooseEnvelope2D(extent);
		QuadTreeImpl quad_tree_impl = new QuadTreeImpl(extent, 8);

		Point2D pt = new Point2D();
		Envelope2D boundingbox = new Envelope2D();
		boolean resized_extent = false;
		for (int i = 0; i < multipointImpl.getPointCount(); i++) {
			multipointImpl.getXY(i, pt);
			boundingbox.setCoords(pt);
			int element_handle = quad_tree_impl.insert(i, boundingbox);

			if (element_handle == -1) {
				if (resized_extent)
					throw GeometryException.GeometryInternalError();

				// resize extent
				multipointImpl.calculateEnvelope2D(extent, false);
				resized_extent = true;
				quad_tree_impl.reset(extent, 8);
				i = -1; // resets the for-loop
				continue;
			}
		}

		return quad_tree_impl;
	}

	static QuadTreeImpl buildQuadTree(MultiPointImpl multipointImpl,
			Envelope2D extentOfInterest) {
		QuadTreeImpl quad_tree_impl = new QuadTreeImpl(extentOfInterest, 8);
		Point2D pt = new Point2D();
		boolean resized_extent = false;
		Envelope2D boundingbox = new Envelope2D();
		for (int i = 0; i < multipointImpl.getPointCount(); i++) {
			multipointImpl.getXY(i, pt);

			if (!extentOfInterest.contains(pt))
				continue;

			boundingbox.setCoords(pt);
			int element_handle = quad_tree_impl.insert(i, boundingbox);

			if (element_handle == -1) {
				if (resized_extent)
					throw GeometryException.GeometryInternalError();

				// resize extent
				resized_extent = true;
				Envelope2D extent = new Envelope2D();
				multipointImpl.calculateEnvelope2D(extent, false);
				quad_tree_impl.reset(extent, 8);
				i = -1; // resets the for-loop
				continue;
			}
		}

		return quad_tree_impl;
	}

	static Envelope2DIntersectorImpl getEnvelope2DIntersector(
			MultiPathImpl multipathImplA, MultiPathImpl multipathImplB,
			double tolerance) {
		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipathImplA.queryLooseEnvelope2D(env_a);
		multipathImplB.queryLooseEnvelope2D(env_b);
		env_a.inflate(tolerance, tolerance);
		env_b.inflate(tolerance, tolerance);

		Envelope2D envInter = new Envelope2D();
		envInter.setCoords(env_a);
		envInter.intersect(env_b);

		SegmentIteratorImpl segIterA = multipathImplA.querySegmentIterator();
		SegmentIteratorImpl segIterB = multipathImplB.querySegmentIterator();

		Envelope2DIntersectorImpl intersector = new Envelope2DIntersectorImpl();
		intersector.setTolerance(tolerance);

		boolean b_found_red = false;
		intersector.startRedConstruction();
		while (segIterA.nextPath()) {
			while (segIterA.hasNextSegment()) {
				Segment segmentA = segIterA.nextSegment();
				segmentA.queryEnvelope2D(env_a);

				if (!env_a.isIntersecting(envInter))
					continue;

				b_found_red = true;
				Envelope2D env = new Envelope2D();
				env.setCoords(env_a);
				intersector.addRedEnvelope(segIterA.getStartPointIndex(), env);
			}
		}
		intersector.endRedConstruction();

		if (!b_found_red)
			return null;

		boolean b_found_blue = false;
		intersector.startBlueConstruction();
		while (segIterB.nextPath()) {
			while (segIterB.hasNextSegment()) {
				Segment segmentB = segIterB.nextSegment();
				segmentB.queryEnvelope2D(env_b);

				if (!env_b.isIntersecting(envInter))
					continue;

				b_found_blue = true;
				Envelope2D env = new Envelope2D();
				env.setCoords(env_b);
				intersector.addBlueEnvelope(segIterB.getStartPointIndex(), env);
			}
		}
		intersector.endBlueConstruction();

		if (!b_found_blue)
			return null;

		return intersector;
	}

    static Envelope2DIntersectorImpl getEnvelope2DIntersectorForParts(
            MultiPathImpl multipathImplA, MultiPathImpl multipathImplB,
            double tolerance, boolean bExteriorOnlyA, boolean bExteriorOnlyB) {
        int type_a = multipathImplA.getType().value();
        int type_b = multipathImplB.getType().value();

        Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
        multipathImplA.queryLooseEnvelope2D(env_a);
        multipathImplB.queryLooseEnvelope2D(env_b);
        env_a.inflate(tolerance, tolerance);
        env_b.inflate(tolerance, tolerance);

        Envelope2D envInter = new Envelope2D();
        envInter.setCoords(env_a);
        envInter.intersect(env_b);

        Envelope2DIntersectorImpl intersector = new Envelope2DIntersectorImpl();
        intersector.setTolerance(tolerance);

        boolean b_found_red = false;
        intersector.startRedConstruction();
        for (int ipath_a = 0, npaths = multipathImplA.getPathCount(); ipath_a < npaths; ipath_a++) {
            if (bExteriorOnlyA && type_a == Geometry.GeometryType.Polygon && !multipathImplA.isExteriorRing(ipath_a))
                continue;

            multipathImplA.queryPathEnvelope2D(ipath_a, env_a);

            if (!env_a.isIntersecting(envInter))
                continue;

            b_found_red = true;
            intersector.addRedEnvelope(ipath_a, env_a);
        }
        intersector.endRedConstruction();

        if (!b_found_red)
            return null;

        boolean b_found_blue = false;
        intersector.startBlueConstruction();
        for (int ipath_b = 0, npaths = multipathImplB.getPathCount(); ipath_b < npaths; ipath_b++) {
            if (bExteriorOnlyB && type_b == Geometry.GeometryType.Polygon && !multipathImplB.isExteriorRing(ipath_b))
                continue;

            multipathImplB.queryPathEnvelope2D(ipath_b, env_b);

            if (!env_b.isIntersecting(envInter))
                continue;

            b_found_blue = true;
            intersector.addBlueEnvelope(ipath_b, env_b);
        }
        intersector.endBlueConstruction();

        if (!b_found_blue)
            return null;

        return intersector;
    }

	static boolean isWeakSimple(MultiVertexGeometry geom, double tol) {
		return ((MultiVertexGeometryImpl) geom._getImpl()).getIsSimple(tol) > 0;
	}
	
	static QuadTree buildQuadTreeForOnePath(MultiPathImpl multipathImpl, int path) {
		Envelope2D extent = new Envelope2D();
		multipathImpl.queryLoosePathEnvelope2D(path, extent);
		QuadTree quad_tree = new QuadTree(extent, 8);
		int hint_index = -1;
		Envelope2D boundingbox = new Envelope2D();
		SegmentIteratorImpl seg_iter = multipathImpl.querySegmentIterator();

		seg_iter.resetToPath(path);
		if (seg_iter.nextPath()) {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();
				int index = seg_iter.getStartPointIndex();
				segment.queryLooseEnvelope2D(boundingbox);
				hint_index = quad_tree.insert(index, boundingbox, hint_index);

				if (hint_index == -1) {
					throw new GeometryException("internal error");
				}
			}
		}

		return quad_tree;
	}
	
}

