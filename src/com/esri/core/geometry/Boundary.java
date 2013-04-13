/*
 Copyright 1995-2013 Esri

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

class Boundary {
	static Geometry calculate(Geometry geom, ProgressTracker progress_tracker) {
		int gt = geom.getType().value();
		if (gt == Geometry.GeometryType.Polygon) {
			Polyline dst = new Polyline(geom.getDescription());
			if (!geom.isEmpty()) { 
				((MultiPathImpl)geom._getImpl())._copyToUnsafe((MultiPathImpl)dst._getImpl());
			}

			return dst;
		} else if (gt == Geometry.GeometryType.Polyline) {
			return calculatePolylineBoundary_(geom._getImpl(), progress_tracker);
		} else if (gt == Geometry.GeometryType.Envelope) {
			Polyline dst = new Polyline(geom.getDescription());
			if (!geom.isEmpty())
				dst.addEnvelope((Envelope) geom, false);

			return dst;
		} else if (Geometry.isSegment(gt)) {
			MultiPoint mp = new MultiPoint(geom.getDescription());
			if (!geom.isEmpty() && !((Segment) geom).isClosed()) {
				Point pt = new Point();
				((Segment) geom).queryStart(pt);
				mp.add(pt);
				((Segment) geom).queryEnd(pt);
				mp.add(pt);
			}
			return mp;
		} else if (Geometry.isPoint(gt)) {
			// returns empty point for points and multipoints.
			return null;
		}

		throw new IllegalArgumentException();
	}

	private static final class MultiPathImplBoundarySorter extends ClassicSort {
		AttributeStreamOfDbl m_xy;

		static final class CompareIndices extends
				AttributeStreamOfInt32.IntComparator {
			AttributeStreamOfDbl m_xy;
			Point2D pt1_helper;
			Point2D pt2_helper;

			CompareIndices(AttributeStreamOfDbl xy) {
				m_xy = xy;
				pt1_helper = new Point2D();
				pt2_helper = new Point2D();
			}

			@Override
			public int compare(int v1, int v2) {
				m_xy.read(2 * v1, pt1_helper);
				m_xy.read(2 * v2, pt2_helper);
				return pt1_helper.compare(pt2_helper);
			}
		}

		MultiPathImplBoundarySorter(AttributeStreamOfDbl xy) {
			m_xy = xy;
		}

		@Override
		public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
			indices.Sort(begin, end, new CompareIndices(m_xy));
		}

		@Override
		public double getValue(int index) {
			return m_xy.read(2 * index + 1);
		}
	}

	static MultiPoint calculatePolylineBoundary_(Object impl,
			ProgressTracker progress_tracker) {
		MultiPathImpl mpImpl = (MultiPathImpl) impl;
		MultiPoint dst = new MultiPoint(mpImpl.getDescription());
		if (!mpImpl.isEmpty()) {
			AttributeStreamOfInt32 indices = new AttributeStreamOfInt32(0);
			indices.reserve(mpImpl.getPathCount() * 2);
			for (int ipath = 0, nPathCount = mpImpl.getPathCount(); ipath < nPathCount; ipath++) {
				int path_size = mpImpl.getPathSize(ipath);
				if (path_size > 0 && !mpImpl.isClosedPathInXYPlane(ipath))// closed
																			// paths
																			// of
																			// polyline
																			// do
																			// not
																			// contribute
																			// to
																			// the
																			// boundary.
				{
					int start = mpImpl.getPathStart(ipath);
					indices.add(start);
					int end = mpImpl.getPathEnd(ipath) - 1;
					indices.add(end);
				}
			}
			if (indices.size() > 0) {
				BucketSort sorter = new BucketSort();
				AttributeStreamOfDbl xy = (AttributeStreamOfDbl) (mpImpl
						.getAttributeStreamRef(VertexDescription.Semantics.POSITION));
				sorter.sort(indices, 0, indices.size(),
						new MultiPathImplBoundarySorter(xy));
				Point2D ptPrev = new Point2D();
				xy.read(2 * indices.get(0), ptPrev);
				int ind = 0;
				int counter = 1;
				Point point = new Point();
				Point2D pt = new Point2D();
				for (int i = 1, n = indices.size(); i < n; i++) {
					xy.read(2 * indices.get(i), pt);
					if (pt.isEqual(ptPrev)) {
						if (indices.get(ind) > indices.get(i)) {
							// remove duplicate point
							indices.set(ind, NumberUtils.intMax());
							ind = i;// just for the heck of it, have the first
									// point in the order to be added to the
									// boundary.
						} else
							indices.set(i, NumberUtils.intMax());

						counter++;
					} else {
						if ((counter & 1) == 0) {// remove boundary point
							indices.set(ind, NumberUtils.intMax());
						}

						ptPrev.setCoords(pt);
						ind = i;
						counter = 1;
					}
				}

				if ((counter & 1) == 0) {// remove the point
					indices.set(ind, NumberUtils.intMax());
				}

				indices.sort(0, indices.size());

				for (int i = 0, n = indices.size(); i < n; i++) {
					if (indices.get(i) == NumberUtils.intMax())
						break;

					mpImpl.getPointByVal(indices.get(i), point);
					dst.add(point);
				}
			}
		}

		return dst;
	}
}
