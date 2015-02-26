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

import com.esri.core.geometry.RasterizedGeometry2D.HitType;

class OperatorInternalRelationUtils {

	interface Relation {
		public final int Unknown = 0;
		public final int Contains = 1;
		public final int Within = 2;
		public final int Equals = 3; // == Within | Contains tests both within
										// and contains
		public final int Disjoint = 4;
		public final int Touches = 8;
		public final int Crosses = 16;
		public final int Overlaps = 32;

		public final int NoThisRelation = 64; // returned when the relation is
												// not satisified
		public final int Intersects = 0x40000000;// this means not_disjoint.
													// Used for early bailout
		public final int IntersectsOrDisjoint = Intersects | Disjoint;
	}

	public static int quickTest2D(Geometry geomA, Geometry geomB,
			double tolerance, int testType) {
		if (geomB.isEmpty() || geomA.isEmpty())
			return (int) Relation.Disjoint;

		int geomAtype = geomA.getType().value();
		int geomBtype = geomB.getType().value();

		// We do not support segments directly for now. Convert to Polyline
		Polyline autoPolyA;
		if (Geometry.isSegment(geomAtype)) {
			autoPolyA = new Polyline(geomA.getDescription());
			geomA = (Geometry) autoPolyA;
			autoPolyA.addSegment((Segment) geomA, true);
		}

		Polyline autoPolyB;
		if (Geometry.isSegment(geomBtype)) {
			autoPolyB = new Polyline(geomB.getDescription());
			geomB = (Geometry) autoPolyB;
			autoPolyB.addSegment((Segment) geomB, true);
		}

		// Now process GeometryxGeometry case by case
		switch (geomAtype) {
		case Geometry.GeometryType.Point: {
			switch (geomBtype) {
			case Geometry.GeometryType.Point:
				return quickTest2DPointPoint((Point) geomA, (Point) geomB,
						tolerance);
			case Geometry.GeometryType.Envelope:
				return reverseResult(quickTest2DEnvelopePoint((Envelope) geomB,
						(Point) geomA, tolerance));
			case Geometry.GeometryType.MultiPoint:
				return reverseResult(quickTest2DMultiPointPoint(
						(MultiPoint) geomB, (Point) geomA, tolerance));
			case Geometry.GeometryType.Polyline:
				return reverseResult(quickTest2DPolylinePoint((Polyline) geomB,
						(Point) geomA, tolerance, testType));
			case Geometry.GeometryType.Polygon:
				return reverseResult(quickTest2DPolygonPoint((Polygon) geomB,
						(Point) geomA, tolerance));
			}
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);//what
															// else?
		}
		case Geometry.GeometryType.Envelope: {
			switch (geomBtype) {
			case Geometry.GeometryType.Point:
				return quickTest2DEnvelopePoint((Envelope) geomA,
						(Point) geomB, tolerance);
			case Geometry.GeometryType.Envelope:
				return quickTest2DEnvelopeEnvelope((Envelope) geomA,
						(Envelope) geomB, tolerance);
			case Geometry.GeometryType.MultiPoint:
				return reverseResult(quickTest2DMultiPointEnvelope(
						(MultiPoint) geomB, (Envelope) geomA, tolerance,
						testType));
			case Geometry.GeometryType.Polyline:
				return reverseResult(quickTest2DPolylineEnvelope(
						(Polyline) geomB, (Envelope) geomA, tolerance));
			case Geometry.GeometryType.Polygon:
				return reverseResult(quickTest2DPolygonEnvelope(
						(Polygon) geomB, (Envelope) geomA, tolerance));
			}
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);//what
															// else?
		}
		case Geometry.GeometryType.MultiPoint: {
			switch (geomBtype) {
			case Geometry.GeometryType.Point:
				return quickTest2DMultiPointPoint((MultiPoint) geomA,
						(Point) geomB, tolerance);
			case Geometry.GeometryType.Envelope:
				return quickTest2DMultiPointEnvelope((MultiPoint) geomA,
						(Envelope) geomB, tolerance, testType);
			case Geometry.GeometryType.MultiPoint:
				return quickTest2DMultiPointMultiPoint((MultiPoint) geomA,
						(MultiPoint) geomB, tolerance, testType);
			case Geometry.GeometryType.Polyline:
				return reverseResult(quickTest2DPolylineMultiPoint(
						(Polyline) geomB, (MultiPoint) geomA, tolerance));
			case Geometry.GeometryType.Polygon:
				return reverseResult(quickTest2DPolygonMultiPoint(
						(Polygon) geomB, (MultiPoint) geomA, tolerance));
			}
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);//what
															// else?
		}
		case Geometry.GeometryType.Polyline: {
			switch (geomBtype) {
			case Geometry.GeometryType.Point:
				return quickTest2DPolylinePoint((Polyline) geomA,
						(Point) geomB, tolerance, testType);
			case Geometry.GeometryType.Envelope:
				return quickTest2DPolylineEnvelope((Polyline) geomA,
						(Envelope) geomB, tolerance);
			case Geometry.GeometryType.MultiPoint:
				return quickTest2DPolylineMultiPoint((Polyline) geomA,
						(MultiPoint) geomB, tolerance);
			case Geometry.GeometryType.Polyline:
				return quickTest2DPolylinePolyline((Polyline) geomA,
						(Polyline) geomB, tolerance);
			case Geometry.GeometryType.Polygon:
				return reverseResult(quickTest2DPolygonPolyline(
						(Polygon) geomB, (Polyline) geomA, tolerance));
			}
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);//what
															// else?
		}
		case Geometry.GeometryType.Polygon: {
			switch (geomBtype) {
			case Geometry.GeometryType.Point:
				return quickTest2DPolygonPoint((Polygon) geomA, (Point) geomB,
						tolerance);
			case Geometry.GeometryType.Envelope:
				return quickTest2DPolygonEnvelope((Polygon) geomA,
						(Envelope) geomB, tolerance);
			case Geometry.GeometryType.MultiPoint:
				return quickTest2DPolygonMultiPoint((Polygon) geomA,
						(MultiPoint) geomB, tolerance);
			case Geometry.GeometryType.Polyline:
				return quickTest2DPolygonPolyline((Polygon) geomA,
						(Polyline) geomB, tolerance);
			case Geometry.GeometryType.Polygon:
				return quickTest2DPolygonPolygon((Polygon) geomA,
						(Polygon) geomB, tolerance);
			}
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);//what
															// else?
		}

		default:
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);//what
															// else?
			// return 0;
		}
	}

	private static int quickTest2DPointPoint(Point geomA, Point geomB,
			double tolerance) {
		Point2D ptA = geomA.getXY();
		Point2D ptB = geomB.getXY();
		return quickTest2DPointPoint(ptA, ptB, tolerance);
	}

	private static int quickTest2DPointPoint(Point2D ptA, Point2D ptB,
			double tolerance) {
		ptA.sub(ptB);
		double len = ptA.sqrLength();// Should we test against 2*tol or tol?
		if (len <= tolerance * tolerance)// Two points are equal if they are not
											// Disjoint. We consider a point to
											// be a disk of radius tolerance.
											// Any intersection of two disks
											// produces same disk.
			return (int) Relation.Within | (int) Relation.Contains;// ==Equals

		return (int) Relation.Disjoint;
	}

	private static int quickTest2DEnvelopePoint(Envelope geomA, Point geomB,
			double tolerance) {
		Envelope2D geomAEnv = new Envelope2D();
		geomA.queryEnvelope2D(geomAEnv);
		Point2D ptB;
		ptB = geomB.getXY();
		return quickTest2DEnvelopePoint(geomAEnv, ptB, tolerance);
	}

	private static int quickTest2DEnvelopePoint(Envelope2D geomAEnv,
			Point2D ptB, double tolerance) {
		Envelope2D envAMinus = geomAEnv;
		envAMinus.inflate(-tolerance, -tolerance);
		if (envAMinus.contains(ptB))
			return (int) Relation.Contains;// clementini's contains
		Envelope2D envAPlus = geomAEnv;
		envAPlus.inflate(tolerance, tolerance);
		if (envAPlus.contains(ptB))
			return (int) Relation.Touches;// clementini's touches

		return (int) Relation.Disjoint;// clementini's disjoint
	}

	private static int quickTest2DEnvelopePoint(Envelope2D envAPlus,
			Envelope2D envAMinus, Point2D ptB, double tolerance) {
		if (envAMinus.contains(ptB))
			return (int) Relation.Contains;// clementini's contains
		if (envAPlus.contains(ptB))
			return (int) Relation.Touches;// clementini's touches

		return (int) Relation.Disjoint;// clementini's disjoint
	}

	private static int quickTest2DEnvelopeEnvelope(Envelope geomA,
			Envelope geomB, double tolerance) {
		Envelope2D geomAEnv = new Envelope2D();
		geomA.queryEnvelope2D(geomAEnv);
		Envelope2D geomBEnv = new Envelope2D();
		geomB.queryEnvelope2D(geomBEnv);
		return quickTest2DEnvelopeEnvelope(geomAEnv, geomBEnv, tolerance);
	}

	private static int quickTest2DEnvelopeEnvelope(Envelope2D geomAEnv,
			Envelope2D geomBEnv, double tolerance) {
		// firstly check for contains and within to give a chance degenerate
		// envelopes to work.
		// otherwise, if there are two degenerate envelopes that are equal,
		// Touch relation may occur.
		int res = 0;
		if (geomAEnv.contains(geomBEnv))
			res |= (int) Relation.Contains;

		if (geomBEnv.contains(geomAEnv))
			res |= (int) Relation.Within;

		if (res != 0)
			return res;

		Envelope2D envAMinus = geomAEnv;
		envAMinus.inflate(-tolerance, -tolerance);// Envelope A interior
		Envelope2D envBMinus = geomBEnv;
		envBMinus.inflate(-tolerance, -tolerance);// Envelope B interior
		if (envAMinus.isIntersecting(envBMinus)) {
			Envelope2D envAPlus = geomAEnv;
			envAPlus.inflate(tolerance, tolerance);// Envelope A interior plus
													// boundary
			res = envAPlus.contains(geomBEnv) ? (int) Relation.Contains : 0;
			Envelope2D envBPlus = geomBEnv;
			envBPlus.inflate(tolerance, tolerance);// Envelope A interior plus
													// boundary
			res |= envBPlus.contains(geomAEnv) ? (int) Relation.Within : 0;
			if (res != 0)
				return res;

			return (int) Relation.Overlaps; // Clementini's Overlap
		} else {
			Envelope2D envAPlus = geomAEnv;
			envAPlus.inflate(tolerance, tolerance);// Envelope A interior plus
													// boundary
			Envelope2D envBPlus = geomBEnv;
			envBPlus.inflate(tolerance, tolerance);// Envelope A interior plus
													// boundary
			if (envAPlus.isIntersecting(envBPlus)) {
				return (int) Relation.Touches; // Clementini Touch
			} else {
				return (int) Relation.Disjoint; // Clementini Disjoint
			}
		}
	}

	private static int quickTest2DMultiPointPoint(MultiPoint geomA,
			Point geomB, double tolerance) {
		Point2D ptB;
		ptB = geomB.getXY();
		return quickTest2DMultiPointPoint(geomA, ptB, tolerance);
	}

	private static int quickTest2DMultiPointPoint(MultiPoint geomA,
			Point2D ptB, double tolerance) {
		// TODO: Add Geometry accelerator. (RasterizedGeometry + kd-tree or
		// alike)
		for (int i = 0, n = geomA.getPointCount(); i < n; i++) {
			Point2D ptA;
			ptA = geomA.getXY(i);
			int res = quickTest2DPointPoint(ptA, ptB, tolerance);
			if (res != (int) Relation.Disjoint) {
				if ((res & (int) Relation.Within) != 0 && n != 1) {
					// _ASSERT(res & (int)Relation.Contains);
					return (int) Relation.Contains;
				}

				return res;
			}
		}

		return (int) Relation.Disjoint;
	}

	private static int quickTest2DMultiPointEnvelope(MultiPoint geomA,
			Envelope geomB, double tolerance, int testType) {
		Envelope2D geomBEnv = new Envelope2D();
		geomB.queryEnvelope2D(geomBEnv);
		return quickTest2DMultiPointEnvelope(geomA, geomBEnv, tolerance,
				testType);
	}

	private static int quickTest2DMultiPointEnvelope(MultiPoint geomA,
			Envelope2D geomBEnv, double tolerance, int testType) {
		// Add early bailout for disjoint test.
		Envelope2D envBMinus = geomBEnv;
		envBMinus.inflate(-tolerance, -tolerance);
		Envelope2D envBPlus = geomBEnv;
		envBPlus.inflate(tolerance, tolerance);
		int dres = 0;
		for (int i = 0, n = geomA.getPointCount(); i < n; i++) {
			Point2D ptA;
			ptA = geomA.getXY(i);
			int res = reverseResult(quickTest2DEnvelopePoint(envBPlus,
					envBMinus, ptA, tolerance));
			if (res != (int) Relation.Disjoint) {
				dres |= res;
				if (testType == (int) Relation.Disjoint)
					return (int) Relation.Intersects;
			}
		}

		if (dres == 0)
			return (int) Relation.Disjoint;

		if (dres == (int) Relation.Within)
			return dres;

		return (int) Relation.Overlaps;
	}

	private static int quickTest2DMultiPointMultiPoint(MultiPoint geomA,
			MultiPoint geomB, double tolerance, int testType) {
		int counter = 0;
		for (int ib = 0, nb = geomB.getPointCount(); ib < nb; ib++) {
			Point2D ptB;
			ptB = geomB.getXY(ib);
			int res = quickTest2DMultiPointPoint(geomA, ptB, tolerance);
			if (res != (int) Relation.Disjoint) {
				counter++;
				if (testType == (int) Relation.Disjoint)
					return (int) Relation.Intersects;
			}
		}

		if (counter > 0) {
			if (counter == geomB.getPointCount())// every point from B is within
													// A. Means the A contains B
			{
				if (testType == (int) Relation.Equals) {// This is slow.
														// Refactor.
					int res = quickTest2DMultiPointMultiPoint(geomB, geomA,
							tolerance, (int) Relation.Contains);
					return res == (int) Relation.Contains ? (int) Relation.Equals
							: (int) Relation.Unknown;
				}
				return (int) Relation.Contains;
			} else {
				return (int) Relation.Overlaps;
			}
		}

		return 0;
	}

	private static int quickTest2DPolylinePoint(Polyline geomA, Point geomB,
			double tolerance, int testType) {
		Point2D ptB;
		ptB = geomB.getXY();
		return quickTest2DPolylinePoint(geomA, ptB, tolerance, testType);
	}

	private static int quickTest2DMVPointRasterOnly(MultiVertexGeometry geomA,
			Point2D ptB, double tolerance) {
		// Use rasterized Geometry:
		RasterizedGeometry2D rgeomA = null;
		MultiVertexGeometryImpl mpImpl = (MultiVertexGeometryImpl) geomA
				._getImpl();
		GeometryAccelerators gaccel = mpImpl._getAccelerators();
		if (gaccel != null) {
			rgeomA = gaccel.getRasterizedGeometry();
		}

		if (rgeomA != null) {
			RasterizedGeometry2D.HitType hitres = rgeomA.queryPointInGeometry(
					ptB.x, ptB.y);
			if (hitres == RasterizedGeometry2D.HitType.Outside)
				return (int) Relation.Disjoint;

			if (hitres == RasterizedGeometry2D.HitType.Inside)
				return (int) Relation.Contains;
		} else
			return -1;

		return 0;
	}

	private static int quickTest2DPolylinePoint(Polyline geomA, Point2D ptB,
			double tolerance, int testType) {
		int mask = Relation.Touches | Relation.Contains | Relation.Within
				| Relation.Disjoint | Relation.Intersects;

		if ((testType & mask) == 0)
			return Relation.NoThisRelation;

		int res = quickTest2DMVPointRasterOnly(geomA, ptB, tolerance);
		if (res > 0)
			return res;

		// Go through the segments:
		double toleranceSqr = tolerance * tolerance;
		MultiPathImpl mpImpl = (MultiPathImpl) geomA._getImpl();
		SegmentIteratorImpl iter = mpImpl.querySegmentIterator();
		while (iter.nextPath()) {
			int pathIndex = iter.getPathIndex();
			if (!geomA.isClosedPath(pathIndex)) {
				int pathSize = geomA.getPathSize(pathIndex);
				int pathStart = geomA.getPathStart(pathIndex);
				if (pathSize == 0)
					continue;

				if (Point2D.sqrDistance(geomA.getXY(pathStart), ptB) <= toleranceSqr
						|| (pathSize > 1 && Point2D.sqrDistance(
								geomA.getXY(pathStart + pathSize - 1), ptB) <= toleranceSqr)) {
					return (int) Relation.Touches;
				}
			}

			if (testType != Relation.Touches) {
				while (iter.hasNextSegment()) {
					Segment segment = iter.nextSegment();
					double t = segment.getClosestCoordinate(ptB, false);
					Point2D pt = segment.getCoord2D(t);
					if (Point2D.sqrDistance(pt, ptB) <= toleranceSqr) {
						if ((testType & Relation.IntersectsOrDisjoint) != 0) {
							return Relation.Intersects;
						}

						return (int) Relation.Contains;
					}
				}
			}
		}

		return (testType & Relation.IntersectsOrDisjoint) != 0 ? Relation.Disjoint
				: Relation.NoThisRelation;
	}

	private static int quickTest2DPolylineEnvelope(Polyline geomA,
			Envelope geomB, double tolerance) {
		Envelope2D geomBEnv = new Envelope2D();
		geomB.queryEnvelope2D(geomBEnv);
		return quickTest2DPolylineEnvelope(geomA, geomBEnv, tolerance);
	}

	private static int quickTest2DPolylineEnvelope(Polyline geomA,
			Envelope2D geomBEnv, double tolerance) {
		int res = quickTest2DMVEnvelopeRasterOnly(geomA, geomBEnv, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DMVEnvelopeRasterOnly(
			MultiVertexGeometry geomA, Envelope2D geomBEnv, double tolerance) {
		// Use rasterized Geometry only:
		RasterizedGeometry2D rgeomA;
		MultiVertexGeometryImpl mpImpl = (MultiVertexGeometryImpl) geomA
				._getImpl();
		GeometryAccelerators gaccel = mpImpl._getAccelerators();
		if (gaccel != null) {
			rgeomA = gaccel.getRasterizedGeometry();
		} else
			return -1;

		if (rgeomA != null) {
			HitType hitres = rgeomA.queryEnvelopeInGeometry(geomBEnv);
			if (hitres == RasterizedGeometry2D.HitType.Outside)
				return (int) Relation.Disjoint;

			if (hitres == RasterizedGeometry2D.HitType.Inside)
				return (int) Relation.Contains;
		} else
			return -1;

		return 0;
	}

	private static int quickTest2DPolylineMultiPoint(Polyline geomA,
			MultiPoint geomB, double tolerance) {
		Envelope2D geomBEnv = new Envelope2D();
		geomB.queryEnvelope2D(geomBEnv);
		int res = quickTest2DMVEnvelopeRasterOnly(geomA, geomBEnv, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DMVMVRasterOnly(MultiVertexGeometry geomA,
			MultiVertexGeometry geomB, double tolerance) {
		Envelope2D geomBEnv = new Envelope2D();
		geomB.queryEnvelope2D(geomBEnv);
		int res = quickTest2DMVEnvelopeRasterOnly(geomA, geomBEnv, tolerance);
		if (res > 0)
			return res;

		if (res == -1) {
			Envelope2D geomAEnv = new Envelope2D();
			geomA.queryEnvelope2D(geomAEnv);
			res = quickTest2DMVEnvelopeRasterOnly(geomB, geomAEnv, tolerance);
			if (res > 0)
				return reverseResult(res);
		}

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DPolylinePolyline(Polyline geomA,
			Polyline geomB, double tolerance) {
		int res = quickTest2DMVMVRasterOnly(geomA, geomB, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DPolygonPoint(Polygon geomA, Point geomB,
			double tolerance) {
		Point2D ptB;
		ptB = geomB.getXY();
		return quickTest2DPolygonPoint(geomA, ptB, tolerance);
	}

	private static int quickTest2DPolygonPoint(Polygon geomA, Point2D ptB,
			double tolerance) {
		PolygonUtils.PiPResult pipres = PolygonUtils.isPointInPolygon2D(geomA,
				ptB, tolerance);// this method uses the accelerator if available
		if (pipres == PolygonUtils.PiPResult.PiPOutside)
			return (int) Relation.Disjoint;// clementini's disjoint

		if (pipres == PolygonUtils.PiPResult.PiPInside)
			return (int) Relation.Contains;// clementini's contains

		if (pipres == PolygonUtils.PiPResult.PiPBoundary)
			return (int) Relation.Touches;// clementini's touches

		throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);
														// //what else
		// return 0;
	}

	private static int quickTest2DPolygonEnvelope(Polygon geomA,
			Envelope geomB, double tolerance) {
		Envelope2D geomBEnv = new Envelope2D();
		geomB.queryEnvelope2D(geomBEnv);
		return quickTest2DPolygonEnvelope(geomA, geomBEnv, tolerance);
	}

	private static int quickTest2DPolygonEnvelope(Polygon geomA,
			Envelope2D geomBEnv, double tolerance) {
		int res = quickTest2DMVEnvelopeRasterOnly(geomA, geomBEnv, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DPolygonMultiPoint(Polygon geomA,
			MultiPoint geomB, double tolerance) {
		int res = quickTest2DMVMVRasterOnly(geomA, geomB, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DPolygonPolyline(Polygon geomA,
			Polyline geomB, double tolerance) {
		int res = quickTest2DMVMVRasterOnly(geomA, geomB, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	private static int quickTest2DPolygonPolygon(Polygon geomA, Polygon geomB,
			double tolerance) {
		int res = quickTest2DMVMVRasterOnly(geomA, geomB, tolerance);
		if (res > 0)
			return res;

		// TODO: implement me
		return 0;
	}

	public static int quickTest2D_Accelerated_DisjointOrContains(
			Geometry geomA, Geometry geomB, double tolerance) {
		int gtA = geomA.getType().value();
		int gtB = geomB.getType().value();
		GeometryAccelerators accel;
		boolean endWhileStatement = false;
		do {
			if (Geometry.isMultiVertex(gtA)) {
				MultiVertexGeometryImpl impl = (MultiVertexGeometryImpl) geomA
						._getImpl();
				accel = impl._getAccelerators();
				if (accel != null) {
					RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
					if (rgeom != null) {
						if (gtB == Geometry.GeometryType.Point) {
							Point2D ptB = ((Point) geomB).getXY();
							HitType hit = rgeom.queryPointInGeometry(ptB.x,
									ptB.y);
							if (hit == RasterizedGeometry2D.HitType.Inside) {
								return (int) Relation.Contains;
							} else if (hit == RasterizedGeometry2D.HitType.Outside) {
								return (int) Relation.Disjoint;
							}

							break;
						}
						Envelope2D envB = new Envelope2D();
						geomB.queryEnvelope2D(envB);
						RasterizedGeometry2D.HitType hit = rgeom
								.queryEnvelopeInGeometry(envB);
						if (hit == RasterizedGeometry2D.HitType.Inside) {
							return (int) Relation.Contains;
						} else if (hit == RasterizedGeometry2D.HitType.Outside) {
							return (int) Relation.Disjoint;
						}

						break;
					}
				}
			}
		} while (endWhileStatement);

		accel = null;
		do {
			if (Geometry.isMultiVertex(gtB)) {
				MultiVertexGeometryImpl impl = (MultiVertexGeometryImpl) geomB
						._getImpl();
				accel = impl._getAccelerators();
				if (accel != null) {
					RasterizedGeometry2D rgeom = accel.getRasterizedGeometry();
					if (rgeom != null) {
						if (gtA == Geometry.GeometryType.Point) {
							Point2D ptA = ((Point) geomA).getXY();
							RasterizedGeometry2D.HitType hit = rgeom
									.queryPointInGeometry(ptA.x, ptA.y);
							if (hit == RasterizedGeometry2D.HitType.Inside) {
								return (int) Relation.Within;
							} else if (hit == RasterizedGeometry2D.HitType.Outside) {
								return (int) Relation.Disjoint;
							}

							break;
						}

						Envelope2D envA = new Envelope2D();
						geomA.queryEnvelope2D(envA);
						RasterizedGeometry2D.HitType hit = rgeom
								.queryEnvelopeInGeometry(envA);
						if (hit == RasterizedGeometry2D.HitType.Inside) {
							return (int) Relation.Within;
						} else if (hit == RasterizedGeometry2D.HitType.Outside) {
							return (int) Relation.Disjoint;
						}

						break;
					}
				}
			}
		} while (endWhileStatement);

		return 0;
	}

	private static int reverseResult(int resIn) {
		int res = resIn;
		if ((res & (int) Relation.Contains) != 0) {
			res &= ~(int) Relation.Contains;
			res |= (int) Relation.Within;
		}
		if ((res & (int) Relation.Within) != 0) {
			res &= ~(int) Relation.Within;
			res |= (int) Relation.Contains;
		}

		return res;
	}

}
