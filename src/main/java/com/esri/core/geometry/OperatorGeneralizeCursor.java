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

final class OperatorGeneralizeCursor extends GeometryCursor {
	ProgressTracker m_progressTracker;
	GeometryCursor m_geoms;
	double m_maxDeviation;
	boolean m_bRemoveDegenerateParts;

	public OperatorGeneralizeCursor(GeometryCursor geoms, double maxDeviation,
			boolean bRemoveDegenerateParts, ProgressTracker progressTracker) {
		m_geoms = geoms;
		m_maxDeviation = maxDeviation;
		m_progressTracker = progressTracker;
		m_bRemoveDegenerateParts = bRemoveDegenerateParts;
	}

	@Override
	public Geometry next() {
		// TODO Auto-generated method stub
		Geometry geom = m_geoms.next();
		if (geom == null)
			return null;
		return Generalize(geom);
	}

	@Override
	public int getGeometryID() {
		// TODO Auto-generated method stub
		return m_geoms.getGeometryID();
	}

	private Geometry Generalize(Geometry geom) {
		Geometry.Type gt = geom.getType();
		if (Geometry.isPoint(gt.value()))
			return geom;
		if (gt == Geometry.Type.Envelope) {
			Polygon poly = new Polygon(geom.getDescription());
			poly.addEnvelope((Envelope) geom, false);
			return Generalize(poly);
		}
		if (geom.isEmpty())
			return geom;
		MultiPath mp = (MultiPath) geom;
		MultiPath dstmp = (MultiPath) geom.createInstance();
		Line line = new Line();
		for (int ipath = 0, npath = mp.getPathCount(); ipath < npath; ipath++) {
			GeneralizePath((MultiPathImpl) mp._getImpl(), ipath,
					(MultiPathImpl) dstmp._getImpl(), line);
		}

		return dstmp;
	}

	private void GeneralizePath(MultiPathImpl mpsrc, int ipath,
			MultiPathImpl mpdst, Line lineHelper) {
		if (mpsrc.getPathSize(ipath) < 2)
			return;
		int start = mpsrc.getPathStart(ipath);
		int end = mpsrc.getPathEnd(ipath) - 1;
		AttributeStreamOfDbl xy = (AttributeStreamOfDbl) mpsrc
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		boolean bClosed = mpsrc.isClosedPath(ipath);

		AttributeStreamOfInt32 stack = new AttributeStreamOfInt32(0);
		stack.reserve(mpsrc.getPathSize(ipath) + 1);
		AttributeStreamOfInt32 resultStack = new AttributeStreamOfInt32(0);
		resultStack.reserve(mpsrc.getPathSize(ipath) + 1);
		stack.add(bClosed ? start : end);
		stack.add(start);
		Point2D pt = new Point2D();
		while (stack.size() > 1) {
			int i1 = stack.getLast();
			stack.removeLast();
			int i2 = stack.getLast();
			mpsrc.getXY(i1, pt);
			lineHelper.setStartXY(pt);
			mpsrc.getXY(i2, pt);
			lineHelper.setEndXY(pt);
			int mid = FindGreatestDistance(lineHelper, pt, xy, i1, i2, end);
			if (mid >= 0) {
				stack.add(mid);
				stack.add(i1);
			} else {
				resultStack.add(i1);
			}
		}

		if (!bClosed)
			resultStack.add(stack.get(0));

		int rs_size = resultStack.size();
		int path_size = mpsrc.getPathSize(ipath);
		if (rs_size == path_size && rs_size == stack.size()) {
			mpdst.addPath(mpsrc, ipath, true);
		} else {
			if (resultStack.size() > 0) {
				if (m_bRemoveDegenerateParts && resultStack.size() <= 2) {
					if (bClosed || resultStack.size() == 1)
						return;

					double d = Point2D.distance(
							mpsrc.getXY(resultStack.get(0)),
							mpsrc.getXY(resultStack.get(1)));
					if (d <= m_maxDeviation)
						return;
				}

				Point point = new Point();
				for (int i = 0, n = resultStack.size(); i < n; i++) {
					mpsrc.getPointByVal(resultStack.get(i), point);
					if (i == 0)
						mpdst.startPath(point);
					else
						mpdst.lineTo(point);
				}

				if (bClosed) {
					for (int i = resultStack.size(); i < 3; i++)
						mpdst.lineTo(point);

					mpdst.closePathWithLine();
				}
			}
		}
	}

	private int FindGreatestDistance(Line line, Point2D ptHelper,
			AttributeStreamOfDbl xy, int start, int end, int pathEnd) {
		int to = end - 1;
		if (end <= start) {// closed path case. end is equal to the path start.
			to = pathEnd;
		}
		int mid = -1;
		double maxd = -1.0;
		for (int i = start + 1; i <= to; i++) {
			xy.read(2 * i, ptHelper);
			double x1 = ptHelper.x;
			double y1 = ptHelper.y;
			double t = line.getClosestCoordinate(ptHelper, false);
			line.getCoord2D(t, ptHelper);
			ptHelper.x -= x1;
			ptHelper.y -= y1;
			double dist = ptHelper.length();
			if (dist > m_maxDeviation && dist > maxd) {
				mid = i;
				maxd = dist;
			}
		}
		return mid;
	}
}
