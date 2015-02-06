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

class OperatorIntersectionCursor extends GeometryCursor {

	GeometryCursor m_inputGeoms;
	GeometryCursor m_smallCursor;
	ProgressTracker m_progress_tracker;
	SpatialReference m_spatial_reference;
	Geometry m_geomIntersector;
	Geometry m_geomIntersectorEmptyGeom;// holds empty geometry of intersector
										// type.
	int m_geomIntersectorType;
	int m_currentGeomType;
	int m_index;
	int m_dimensionMask;
	boolean m_bEmpty;

	OperatorIntersectionCursor(GeometryCursor inputGeoms,
			GeometryCursor geomIntersector, SpatialReference sr,
			ProgressTracker progress_tracker, int dimensionMask) {
		m_bEmpty = geomIntersector == null;
		m_index = -1;
		m_inputGeoms = inputGeoms;
		m_spatial_reference = sr;
		m_geomIntersector = geomIntersector.next();
		m_geomIntersectorType = m_geomIntersector.getType().value();
		m_currentGeomType = Geometry.Type.Unknown.value();
		m_progress_tracker = progress_tracker;
		m_dimensionMask = dimensionMask;
		if (m_dimensionMask != -1
				&& (m_dimensionMask <= 0 || m_dimensionMask > 7))
			throw new IllegalArgumentException("bad dimension mask");// dimension
																		// mask
																		// can
																		// be
																		// -1,
																		// for
																		// the
																		// default
																		// behavior,
																		// or a
																		// value
																		// between
																		// 1 and
																		// 7.
	}

	@Override
	public Geometry next() {
		if (m_bEmpty)
			return null;

		Geometry geom;
		if (m_smallCursor != null) {// when dimension mask is used, we produce a
			geom = m_smallCursor.next();
			if (geom != null)
				return geom;
			else
				m_smallCursor = null;// done with the small cursor
		}

		while ((geom = m_inputGeoms.next()) != null) {
			m_index = m_inputGeoms.getGeometryID();
			if (m_dimensionMask == -1) {
				Geometry resGeom = intersect(geom);
				assert (resGeom != null);
				return resGeom;
			} else {
				m_smallCursor = intersectEx(geom);
				Geometry resGeom = m_smallCursor.next();
				assert (resGeom != null);
				return resGeom;
			}
		}
		return null;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	Geometry intersect(Geometry input_geom) {
		Geometry dst_geom = tryNativeImplementation_(input_geom);
		if (dst_geom != null)
			return dst_geom;

		Envelope2D commonExtent = InternalUtils.getMergedExtent(
				m_geomIntersector, input_geom);

		// return Topological_operations::intersection(input_geom,
		// m_geomIntersector, m_spatial_reference, m_progress_tracker);
		// Preprocess geometries to be clipped to the extent of intersection to
		// get rid of extra segments.
		double t = InternalUtils.calculateToleranceFromGeometry(m_spatial_reference, commonExtent, true);
		Envelope2D env = new Envelope2D();
		m_geomIntersector.queryEnvelope2D(env);
		Envelope2D env1 = new Envelope2D();
		input_geom.queryEnvelope2D(env1);
		env.inflate(2.0 * t, 2.0 * t);
		env.intersect(env1);
		assert (!env.isEmpty());
		env.inflate(100 * t, 100 * t);
		double tol = 0;
		Geometry clippedIntersector = Clipper.clip(m_geomIntersector, env, tol,
				0.0);
		Geometry clippedInputGeom = Clipper.clip(input_geom, env, tol, 0.0);
		// perform the clip
		return TopologicalOperations.intersection(clippedInputGeom,
				clippedIntersector, m_spatial_reference, m_progress_tracker);
	}

	// Parses the input vector to ensure the out result contains only geometries
	// as indicated with the dimensionMask
	GeometryCursor prepareVector_(VertexDescription descr, int dimensionMask,
			Geometry[] res_vec) {
		int inext = 0;
		if ((dimensionMask & 1) != 0) {
			if (res_vec[0] == null)
				res_vec[0] = new MultiPoint(descr);
			inext++;
		} else {
			for (int i = 0; i < res_vec.length - 1; i++)
				res_vec[i] = res_vec[i + 1];
		}

		if ((dimensionMask & 2) != 0) {
			if (res_vec[inext] == null)
				res_vec[inext] = new Polyline(descr);
			inext++;
		} else {
			for (int i = inext; i < res_vec.length - 1; i++)
				res_vec[i] = res_vec[i + 1];
		}

		if ((dimensionMask & 4) != 0) {
			if (res_vec[inext] == null)
				res_vec[inext] = new Polygon(descr);
			inext++;
		} else {
			for (int i = inext; i < res_vec.length - 1; i++)
				res_vec[i] = res_vec[i + 1];
		}
		
		if (inext != 3) {
			Geometry[] r = new Geometry[inext];
			for (int i = 0; i < inext; i++)
				r[i] = res_vec[i];

			return new SimpleGeometryCursor(r);
		} else {
			return new SimpleGeometryCursor(res_vec);
		}
	}

	GeometryCursor intersectEx(Geometry input_geom) {
		assert (m_dimensionMask != -1);
		Geometry dst_geom = tryNativeImplementation_(input_geom);
		if (dst_geom != null) {
			Geometry[] res_vec = new Geometry[3];
			res_vec[dst_geom.getDimension()] = dst_geom;
			return prepareVector_(input_geom.getDescription(), m_dimensionMask,
					res_vec);
		}

		Envelope2D commonExtent = InternalUtils.getMergedExtent(
				m_geomIntersector, input_geom);
		double t = InternalUtils.calculateToleranceFromGeometry(
				m_spatial_reference, commonExtent, true);

		// Preprocess geometries to be clipped to the extent of intersection to
		// get rid of extra segments.
		
		Envelope2D env = new Envelope2D();
		m_geomIntersector.queryEnvelope2D(env);
		env.inflate(2 * t, 2 * t);
		Envelope2D env1 = new Envelope2D();
		input_geom.queryEnvelope2D(env1);
		env.intersect(env1);
		assert (!env.isEmpty());
		env.inflate(100 * t, 100 * t);
		double tol = 0;
		Geometry clippedIntersector = Clipper.clip(m_geomIntersector, env, tol,
				0.0);
		Geometry clippedInputGeom = Clipper.clip(input_geom, env, tol, 0.0);
		// perform the clip
		Geometry[] res_vec;
		res_vec = TopologicalOperations.intersectionEx(clippedInputGeom,
				clippedIntersector, m_spatial_reference, m_progress_tracker);
		return prepareVector_(input_geom.getDescription(), m_dimensionMask,
				res_vec);
	}

	Geometry tryNativeImplementation_(Geometry input_geom) {
		// A note on attributes:
		// 1. The geometry with lower dimension wins in regard to the
		// attributes.
		// 2. If the dimensions are the same, the input_geometry attributes win.
		// 3. The exception to the 2. is when the input is an Envelope, and the
		// intersector is a polygon, then the intersector wins.

		// A note on the tolerance:
		// This operator performs a simple intersection operation. Should it use
		// the tolerance?
		// Example: Point is intersected by the envelope.
		// If it is slightly outside of the envelope, should we still return it
		// if it is closer than the tolerance?
		// Should we do crack and cluster and snap the point coordinates to the
		// envelope boundary?
		//
		// Consider floating point arithmetics approach. When you compare
		// doubles, you should use an epsilon (equals means ::fabs(a - b) <
		// eps), however when you add/subtract, etc them, you do not use
		// epsilon.
		// Shouldn't we do same here? Relational operators use tolerance, but
		// the action operators don't.

		Envelope2D mergedExtent = InternalUtils.getMergedExtent(input_geom,
				m_geomIntersector);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				m_spatial_reference, mergedExtent, false);

		int gtInput = input_geom.getType().value();
		boolean bInputEmpty = input_geom.isEmpty();
		boolean bGeomIntersectorEmpty = m_geomIntersector.isEmpty();
		boolean bResultIsEmpty = bInputEmpty || bGeomIntersectorEmpty;
		if (!bResultIsEmpty) {// test envelopes
			Envelope2D env2D1 = new Envelope2D();
			input_geom.queryEnvelope2D(env2D1);
			Envelope2D env2D2 = new Envelope2D();
			m_geomIntersector.queryEnvelope2D(env2D2);
                        env2D2.inflate(2.0 * tolerance, 2.0 * tolerance);
			bResultIsEmpty = !env2D1.isIntersecting(env2D2);
		}

		if (!bResultIsEmpty) {// try accelerated test
			int res = OperatorInternalRelationUtils
					.quickTest2D_Accelerated_DisjointOrContains(
							m_geomIntersector, input_geom, tolerance);
			if (res == OperatorInternalRelationUtils.Relation.Disjoint) {// disjoint
				bResultIsEmpty = true;
			} else if ((res & OperatorInternalRelationUtils.Relation.Within) != 0) {// intersector
																					// is
																					// within
																					// the
																					// input_geom
																					// TODO:
																					// assign
																					// input_geom
																					// attributes
																					// first
				return m_geomIntersector;
			} else if ((res & OperatorInternalRelationUtils.Relation.Contains) != 0) {// intersector
																						// contains
																						// input_geom
				return input_geom;
			}
		}

		if (bResultIsEmpty) {// When one geometry or both are empty, we need to
								// return an empty geometry.
								// Here we do that end also ensure the type is
								// correct.
								// That is the lower dimension need to be
								// returned. Also, for Point vs Multi_point, an
								// empty Point need to be returned.
			int dim1 = Geometry.getDimensionFromType(gtInput);
			int dim2 = Geometry.getDimensionFromType(m_geomIntersectorType);
			if (dim1 < dim2)
				return returnEmpty_(input_geom, bInputEmpty);
			else if (dim1 > dim2)
				return returnEmptyIntersector_();
			else if (dim1 == 0) {
				if (gtInput == Geometry.GeometryType.MultiPoint
						&& m_geomIntersectorType == Geometry.GeometryType.Point) {// point
																					// vs
																					// Multi_point
																					// need
																					// special
																					// treatment
																					// to
																					// ensure
																					// Point
																					// is
																					// returned
																					// always.
					return returnEmptyIntersector_();
				} else
					// Both input and intersector have same gtype, or input is
					// Point.
					return returnEmpty_(input_geom, bInputEmpty);
			} else
				return returnEmpty_(input_geom, bInputEmpty);
		}

		// Note: No empty geometries after this point!

		// Warning: Do not try clip for polylines and polygons.

		// Try clip of Envelope with Envelope.
		if ((m_dimensionMask == -1 || m_dimensionMask == (1 << 2))
				&& gtInput == Geometry.GeometryType.Envelope
				&& m_geomIntersectorType == Geometry.GeometryType.Envelope) {
			Envelope env1 = (Envelope) input_geom;
			Envelope env2 = (Envelope) m_geomIntersector;
			Envelope2D env2D_1 = new Envelope2D();
			env1.queryEnvelope2D(env2D_1);
			Envelope2D env2D_2 = new Envelope2D();
			env2.queryEnvelope2D(env2D_2);
			env2D_1.intersect(env2D_2);
			Envelope result_env = new Envelope();
			env1.copyTo(result_env);
			result_env.setEnvelope2D(env2D_1);
			return result_env;
		}

		// Use clip for Point and Multi_point with Envelope
		if ((gtInput == Geometry.GeometryType.Envelope && Geometry
				.getDimensionFromType(m_geomIntersectorType) == 0)
				|| (m_geomIntersectorType == Geometry.GeometryType.Envelope && Geometry
						.getDimensionFromType(gtInput) == 0)) {
			Envelope env = gtInput == Geometry.GeometryType.Envelope ? (Envelope) input_geom
					: (Envelope) m_geomIntersector;
			Geometry other = gtInput == Geometry.GeometryType.Envelope ? m_geomIntersector
					: input_geom;
			Envelope2D env_2D = new Envelope2D();
			env.queryEnvelope2D(env_2D);
			return Clipper.clip(other, env_2D, tolerance, 0);
		}

		if ((Geometry.getDimensionFromType(gtInput) == 0 && Geometry
				.getDimensionFromType(m_geomIntersectorType) > 0)
				|| (Geometry.getDimensionFromType(gtInput) > 0 && Geometry
						.getDimensionFromType(m_geomIntersectorType) == 0)) {// multipoint
																				// intersection
			double tolerance1 = InternalUtils.calculateToleranceFromGeometry(
					m_spatial_reference, input_geom, false);
			if (gtInput == Geometry.GeometryType.MultiPoint)
				return TopologicalOperations.intersection(
						(MultiPoint) input_geom, m_geomIntersector, tolerance1);
			if (gtInput == Geometry.GeometryType.Point)
				return TopologicalOperations.intersection((Point) input_geom,
						m_geomIntersector, tolerance1);
			if (m_geomIntersectorType == Geometry.GeometryType.MultiPoint)
				return TopologicalOperations.intersection(
						(MultiPoint) m_geomIntersector, input_geom, tolerance1);
			if (m_geomIntersectorType == Geometry.GeometryType.Point)
				return TopologicalOperations.intersection(
						(Point) m_geomIntersector, input_geom, tolerance1);
			throw GeometryException.GeometryInternalError();
		}

		// Try Polyline vs Polygon
		if ((m_dimensionMask == -1 || m_dimensionMask == (1 << 1))
				&& (gtInput == Geometry.GeometryType.Polyline)
				&& (m_geomIntersectorType == Geometry.GeometryType.Polygon)) {
			return tryFastIntersectPolylinePolygon_((Polyline) (input_geom),
					(Polygon) (m_geomIntersector));
		}

		// Try Polygon vs Polyline
		if ((m_dimensionMask == -1 || m_dimensionMask == (1 << 1))
				&& (gtInput == Geometry.GeometryType.Polygon)
				&& (m_geomIntersectorType == Geometry.GeometryType.Polyline)) {
			return tryFastIntersectPolylinePolygon_(
					(Polyline) (m_geomIntersector), (Polygon) (input_geom));
		}

		return null;
	}

	Geometry tryFastIntersectPolylinePolygon_(Polyline polyline, Polygon polygon) {
		MultiPathImpl polylineImpl = (MultiPathImpl) polyline._getImpl();
		MultiPathImpl polygonImpl = (MultiPathImpl) polygon._getImpl();

		double tolerance = InternalUtils.calculateToleranceFromGeometry(
				m_spatial_reference, polygon, false);
		Envelope2D clipEnvelope = new Envelope2D();
		{
			polygonImpl.queryEnvelope2D(clipEnvelope);
			Envelope2D env1 = new Envelope2D();
			polylineImpl.queryEnvelope2D(env1);
                        env1.inflate(2.0 * tolerance, 2.0 * tolerance);
			clipEnvelope.intersect(env1);
			assert (!clipEnvelope.isEmpty());
		}

		clipEnvelope.inflate(10 * tolerance, 10 * tolerance);

		if (true) {
			double tol = 0;
			Geometry clippedPolyline = Clipper.clip(polyline, clipEnvelope,
					tol, 0.0);
			polyline = (Polyline) clippedPolyline;
			polylineImpl = (MultiPathImpl) polyline._getImpl();
		}

		AttributeStreamOfInt32 clipResult = new AttributeStreamOfInt32(0);
		int unresolvedSegments = -1;
		GeometryAccelerators accel = polygonImpl._getAccelerators();
		if (accel != null) {
			RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
			if (rgeom != null) {
				unresolvedSegments = 0;
				clipResult.reserve(polylineImpl.getPointCount()
						+ polylineImpl.getPathCount());
				Envelope2D seg_env = new Envelope2D();
				SegmentIteratorImpl iter = polylineImpl.querySegmentIterator();
				while (iter.nextPath()) {
					while (iter.hasNextSegment()) {
						Segment seg = iter.nextSegment();
						seg.queryEnvelope2D(seg_env);
						RasterizedGeometry2D.HitType hit = rgeom
								.queryEnvelopeInGeometry(seg_env);
						if (hit == RasterizedGeometry2D.HitType.Inside) {
							clipResult.add(1);
						} else if (hit == RasterizedGeometry2D.HitType.Outside) {
							clipResult.add(0);
						} else {
							clipResult.add(-1);
							unresolvedSegments++;
						}
					}
				}
			}
		}

		if (polygon.getPointCount() > 5) {
			double tol = 0;
			Geometry clippedPolygon = Clipper.clip(polygon, clipEnvelope, tol,
					0.0);

			polygon = (Polygon) clippedPolygon;
			polygonImpl = (MultiPathImpl) polygon._getImpl();
            accel = polygonImpl._getAccelerators();//update accelerators
		}

		if (unresolvedSegments < 0) {
			unresolvedSegments = polylineImpl.getSegmentCount();
		}

		// Some heuristics to decide if it makes sense to go with fast intersect
		// vs going with the regular planesweep.
		double totalPoints = (double) (polylineImpl.getPointCount() + polygonImpl
				.getPointCount());
		double thisAlgorithmComplexity = ((double) unresolvedSegments * polygonImpl
				.getPointCount());// assume the worst case.
		double planesweepComplexity = Math.log(totalPoints) * totalPoints;
		double empiricConstantFactorPlaneSweep = 4;
		if (thisAlgorithmComplexity > planesweepComplexity
				* empiricConstantFactorPlaneSweep) {
			// Based on the number of input points, we deduced that the
			// plansweep performance should be better than the brute force
			// performance.
			return null; // resort to planesweep if quadtree does not help
		}

		QuadTreeImpl polygonQuadTree = null;
		SegmentIteratorImpl polygonIter = polygonImpl.querySegmentIterator();
		// Some logic to decide if it makes sense to build a quadtree on the
		// polygon segments
		if (accel != null && accel.getQuadTree() != null)
			polygonQuadTree = accel.getQuadTree();

		if (polygonQuadTree == null && polygonImpl.getPointCount() > 20) {
			polygonQuadTree = InternalUtils.buildQuadTree(polygonImpl);
		}

		Polyline result_polyline = (Polyline) polyline.createInstance();
		MultiPathImpl resultPolylineImpl = (MultiPathImpl) result_polyline
				._getImpl();
		QuadTreeImpl.QuadTreeIteratorImpl qIter = null;
		SegmentIteratorImpl polylineIter = polylineImpl.querySegmentIterator();
		double[] params = new double[9];
		AttributeStreamOfDbl intersections = new AttributeStreamOfDbl(0);
		SegmentBuffer segmentBuffer = new SegmentBuffer();
		int start_index = -1;
		int inCount = 0;
		int segIndex = 0;
		boolean bOptimized = clipResult.size() > 0;

		// The algorithm is like that:
		// Loop through all the segments of the polyline.
		// For each polyline segment, intersect it with each of the polygon
		// segments.
		// If no intersections found then,
		// If the polyline segment is completely inside, it is added to the
		// result polyline.
		// If it is outside, it is thrown out.
		// If it intersects, then cut the polyline segment to pieces and test
		// each part of the intersected result.
		// The cut pieces will either have one point inside, or one point
		// outside, or the middle point inside/outside.
		//
		int polylinePathIndex = -1;

		while (polylineIter.nextPath()) {
			polylinePathIndex = polylineIter.getPathIndex();
			int stateNewPath = 0;
			int stateAddSegment = 1;
			int stateManySegments = 2;
			int stateManySegmentsContinuePath = 2;
			int stateManySegmentsNewPath = 3;
			int state = stateNewPath;
			start_index = -1;
			inCount = 0;

			while (polylineIter.hasNextSegment()) {
				int clipStatus = bOptimized ? (int) clipResult.get(segIndex)
						: -1;
				segIndex++;
				Segment polylineSeg = polylineIter.nextSegment();
				if (clipStatus < 0) {
					assert (clipStatus == -1);
					// Analyse polyline segment for intersection with the
					// polygon.
					if (polygonQuadTree != null) {
						if (qIter == null) {
							qIter = polygonQuadTree.getIterator(polylineSeg,
									tolerance);
						} else {
							qIter.resetIterator(polylineSeg, tolerance);
						}

						int path_index = -1;
						for (int ind = qIter.next(); ind != -1; ind = qIter
								.next()) {
							polygonIter.resetToVertex(polygonQuadTree
									.getElement(ind)); // path_index
							path_index = polygonIter.getPathIndex();
							Segment polygonSeg = polygonIter.nextSegment();
							// intersect polylineSeg and polygonSeg.
							int count = polylineSeg.intersect(polygonSeg, null,
									params, null, tolerance);
							for (int i = 0; i < count; i++)
								intersections.add(params[i]);
						}
					} else {// no quadtree built
						polygonIter.resetToFirstPath();
						while (polygonIter.nextPath()) {
							while (polygonIter.hasNextSegment()) {
								Segment polygonSeg = polygonIter.nextSegment();
								// intersect polylineSeg and polygonSeg.
								int count = polylineSeg.intersect(polygonSeg,
										null, params, null, tolerance);
								for (int i = 0; i < count; i++)
									intersections.add(params[i]);
							}
						}
					}

					if (intersections.size() > 0) {// intersections detected.
						intersections.sort(0, intersections.size()); // std::sort(intersections.begin(),
																		// intersections.end());

						double t0 = 0;
						intersections.add(1.0);
						int status = -1;
						for (int i = 0, n = intersections.size(); i < n; i++) {
							double t = intersections.get(i);
							if (t == t0) {
								continue;
							}
							boolean bWholeSegment = false;
							Segment resSeg;
							if (t0 != 0 || t != 1.0) {
								polylineSeg.cut(t0, t, segmentBuffer);
								resSeg = segmentBuffer.get();
							} else {
								resSeg = polylineSeg;
								bWholeSegment = true;
							}

							if (state >= stateManySegments) {
								resultPolylineImpl.addSegmentsFromPath(
										polylineImpl, polylinePathIndex,
										start_index, inCount,
										state == stateManySegmentsNewPath);
								if (analyseClipSegment_(polygon,
										resSeg.getStartXY(), tolerance) != 1) {
									if (analyseClipSegment_(polygon, resSeg,
											tolerance) != 1) {
										return null;  //someting went wrong we'll falback to slower but robust planesweep code.
									}
								}

								resultPolylineImpl.addSegment(resSeg, false);
								state = stateAddSegment;
								inCount = 0;
							} else {
								status = analyseClipSegment_(polygon, resSeg,
										tolerance);
								switch (status) {
								case 1:
									if (!bWholeSegment) {
										resultPolylineImpl.addSegment(resSeg,
												state == stateNewPath);
										state = stateAddSegment;
									} else {
										if (state < stateManySegments) {
											start_index = polylineIter
													.getStartPointIndex()
													- polylineImpl
															.getPathStart(polylinePathIndex);
											inCount = 1;

											if (state == stateNewPath)
												state = stateManySegmentsNewPath;
											else {
												assert (state == stateAddSegment);
												state = stateManySegmentsContinuePath;
											}
										} else
											inCount++;
									}

									break;
								case 0:
									state = stateNewPath;
									start_index = -1;
									inCount = 0;
									break;
								default:
									return null;// may happen if a segment
												// coincides with the border.
								}
							}

							t0 = t;
						}
					} else {
						clipStatus = analyseClipSegment_(polygon,
								polylineSeg.getStartXY(), tolerance);// simple
																		// case
																		// no
																		// intersection.
																		// Both
																		// points
																		// must
																		// be
																		// inside.
						if (clipStatus < 0) {
							assert (clipStatus >= 0);
							return null;// something goes wrong, resort to
										// planesweep
						}

						assert (analyseClipSegment_(polygon,
								polylineSeg.getEndXY(), tolerance) == clipStatus);
						if (clipStatus == 1) {// the whole segment inside
							if (state < stateManySegments) {
								assert (inCount == 0);
								start_index = polylineIter.getStartPointIndex()
										- polylineImpl
												.getPathStart(polylinePathIndex);
								if (state == stateNewPath)
									state = stateManySegmentsNewPath;
								else {
									assert (state == stateAddSegment);
									state = stateManySegmentsContinuePath;
								}
							}

							inCount++;
						} else {
							assert (state < stateManySegments);
							start_index = -1;
							inCount = 0;
						}
					}

					intersections.clear(false);
				} else {// clip status is determined by other means
					if (clipStatus == 0) {// outside
						assert (analyseClipSegment_(polygon, polylineSeg,
								tolerance) == 0);
						assert (start_index < 0);
						assert (inCount == 0);
						continue;
					}

					if (clipStatus == 1) {
						assert (analyseClipSegment_(polygon, polylineSeg,
								tolerance) == 1);
						if (state == stateNewPath) {
							state = stateManySegmentsNewPath;
							start_index = polylineIter.getStartPointIndex()
									- polylineImpl
											.getPathStart(polylinePathIndex);
						} else if (state == stateAddSegment) {
							state = stateManySegmentsContinuePath;
							start_index = polylineIter.getStartPointIndex()
									- polylineImpl
											.getPathStart(polylinePathIndex);
						} else
							assert (state >= stateManySegments);

						inCount++;
						continue;
					}
				}
			}

			if (state >= stateManySegments) {
				resultPolylineImpl.addSegmentsFromPath(polylineImpl,
						polylinePathIndex, start_index, inCount,
						state == stateManySegmentsNewPath);
				start_index = -1;
			}
		}

		return result_polyline;
	}

	int analyseClipSegment_(Polygon polygon, Point2D pt, double tol) {
		int v = PointInPolygonHelper.isPointInPolygon(polygon, pt, tol);
		return v;
	}

	int analyseClipSegment_(Polygon polygon, Segment seg, double tol) {
		Point2D pt_1 = seg.getStartXY();
		Point2D pt_2 = seg.getEndXY();
		int v_1 = PointInPolygonHelper.isPointInPolygon(polygon, pt_1, tol);
		int v_2 = PointInPolygonHelper.isPointInPolygon(polygon, pt_2, tol);
		if ((v_1 == 1 && v_2 == 0) || (v_1 == 0 && v_2 == 1)) {
			// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/badPointInPolygon.json",
			// polygon, m_spatial_reference);
			assert (false);// if happens
			return -1;// something went wrong. One point is inside, the other is
						// outside. Should not happen. We'll resort to
						// planesweep.
		}
		if (v_1 == 0 || v_2 == 0)
			return 0;
		if (v_1 == 1 || v_2 == 1)
			return 1;

		Point2D midPt = new Point2D();
		midPt.add(pt_1, pt_2);
		midPt.scale(0.5);// calculate midpoint
		int v = PointInPolygonHelper.isPointInPolygon(polygon, midPt, tol);
		if (v == 0) {
			return 0;
		}

		if (v == 1) {
			return 1;
		}

		return -1;
	}

	Geometry normalizeIntersectionOutput(Geometry geom, int GT_1, int GT_2) {
		if (GT_1 == Geometry.GeometryType.Point
				|| GT_2 == Geometry.GeometryType.Point) {
			assert (geom.getType().value() == Geometry.GeometryType.Point);
		}
		if (GT_1 == Geometry.GeometryType.MultiPoint) {
			if (geom.getType().value() == Geometry.GeometryType.Point) {
				MultiPoint mp = new MultiPoint(geom.getDescription());
				if (!geom.isEmpty())
					mp.add((Point) geom);
				return mp;
			}
		}

		return geom;
	}

	static Geometry returnEmpty_(Geometry geom, boolean bEmpty) {
		return bEmpty ? geom : geom.createInstance();
	}

	Geometry returnEmptyIntersector_() {
		if (m_geomIntersectorEmptyGeom == null)
			m_geomIntersectorEmptyGeom = m_geomIntersector.createInstance();

		return m_geomIntersectorEmptyGeom;
	}

	// virtual boolean IsRecycling() OVERRIDE { return false; }
}
