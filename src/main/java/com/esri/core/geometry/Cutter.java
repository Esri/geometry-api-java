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

import com.esri.core.geometry.OperatorCutLocal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

class Cutter {
	static class CompareVertices {
		int m_orderIndex;
		EditShape m_editShape;

		CompareVertices(int orderIndex, EditShape editShape) {
			m_orderIndex = orderIndex;
			m_editShape = editShape;
		}

		int _compareVertices(int v1, int v2) {
			Point2D pt1 = new Point2D();
			m_editShape.getXY(v1, pt1);
			Point2D pt2 = new Point2D();
			m_editShape.getXY(v2, pt2);
			int res = pt1.compare(pt2);
			if (res != 0)
				return res;
			int z1 = m_editShape.getUserIndex(v1, m_orderIndex);
			int z2 = m_editShape.getUserIndex(v2, m_orderIndex);
			if (z1 < z2)
				return -1;
			if (z1 == z2)
				return 0;
			return 1;
		}
	}

	static class CutterVertexComparer extends
			AttributeStreamOfInt32.IntComparator {
		CompareVertices m_compareVertices;

		CutterVertexComparer(CompareVertices _compareVertices) {
			m_compareVertices = _compareVertices;
		}

		@Override
		public int compare(int v1, int v2) {
			return m_compareVertices._compareVertices(v1, v2);
		}
	}

	static class CutEvent {
		int m_ivertexCuttee;
		int m_ipartCuttee;
		double m_scalarCuttee0;
		double m_scalarCuttee1;
		int m_count;
		int m_ivertexCutter;
		int m_ipartCutter;
		double m_scalarCutter0;
		double m_scalarCutter1;

		CutEvent(int ivertexCuttee, int ipartCuttee, double scalarCuttee0,
				double scalarCuttee1, int count, int ivertexCutter,
				int ipartCutter, double scalarCutter0, double scalarCutter1) {
			m_ivertexCuttee = ivertexCuttee;
			m_ipartCuttee = ipartCuttee;
			m_scalarCuttee0 = scalarCuttee0;
			m_scalarCuttee1 = scalarCuttee1;
			m_count = count;
			m_ivertexCutter = ivertexCutter;
			m_ipartCutter = ipartCutter;
			m_scalarCutter0 = scalarCutter0;
			m_scalarCutter1 = scalarCutter1;
		}
	}

	static EditShape CutPolyline(boolean bConsiderTouch, Polyline cuttee,
			Polyline cutter, double tolerance,
			ArrayList<OperatorCutLocal.CutPair> cutPairs,
			AttributeStreamOfInt32 segmentCounts, ProgressTracker progressTracker) {
		if (cuttee.isEmpty()) {
			OperatorCutLocal.CutPair cutPair;
			cutPair = new OperatorCutLocal.CutPair(cuttee,
					OperatorCutLocal.Side.Uncut, -1, -1, NumberUtils.NaN(),
					OperatorCutLocal.Side.Uncut, -1, -1, NumberUtils.NaN(), -1,
					-1, NumberUtils.NaN(), -1, -1, NumberUtils.NaN());
			cutPairs.add(cutPair);
			return null;
		}

		EditShape editShape = new EditShape();
		int cutteeHandle = editShape.addGeometry(cuttee);
		int cutterHandle = editShape.addGeometry(cutter);
		CrackAndCluster.execute(editShape, tolerance, progressTracker, true);

		int order = 0;
		int orderIndex = editShape.createUserIndex();
		for (int igeometry = editShape.getFirstGeometry(); igeometry != -1; igeometry = editShape
				.getNextGeometry(igeometry))
			for (int ipath = editShape.getFirstPath(igeometry); ipath != -1; ipath = editShape
					.getNextPath(ipath))
				for (int ivertex = editShape.getFirstVertex(ipath), i = 0, n = editShape
						.getPathSize(ipath); i < n; ivertex = editShape
						.getNextVertex(ivertex), i++)
					editShape.setUserIndex(ivertex, orderIndex, order++);

		ArrayList<CutEvent> cutEvents = _getCutEvents(orderIndex, editShape);
		_Cut(bConsiderTouch, false, cutEvents, editShape, cutPairs,
				segmentCounts);
		return editShape;
	}

	private static ArrayList<CutEvent> _getCutEvents(int orderIndex,
			EditShape editShape) {
		int pointCount = editShape.getTotalPointCount();

		// Sort vertices lexicographically
		// Firstly copy allvertices to an array.
		AttributeStreamOfInt32 vertices = new AttributeStreamOfInt32(0);

		for (int igeometry = editShape.getFirstGeometry(); igeometry != -1; igeometry = editShape
				.getNextGeometry(igeometry))
			for (int ipath = editShape.getFirstPath(igeometry); ipath != -1; ipath = editShape
					.getNextPath(ipath))
				for (int ivertex = editShape.getFirstVertex(ipath), i = 0, n = editShape
						.getPathSize(ipath); i < n; ivertex = editShape
						.getNextVertex(ivertex), i++)
					vertices.add(ivertex);

		// Sort
		CompareVertices compareVertices = new CompareVertices(orderIndex,
				editShape);
		vertices.Sort(0, pointCount, new CutterVertexComparer(compareVertices));
		// SORTDYNAMICARRAYEX(vertices, index_type, 0, pointCount,
		// CutterVertexComparer, compareVertices);

		// Find Cut Events
		ArrayList<CutEvent> cutEvents = new ArrayList<CutEvent>(0);
		ArrayList<CutEvent> cutEventsTemp = new ArrayList<CutEvent>(0);

		int eventIndex = editShape.createUserIndex();
		int eventIndexTemp = editShape.createUserIndex();

		int cutteeHandle = editShape.getFirstGeometry();
		int cutterHandle = editShape.getNextGeometry(cutteeHandle);

		Point2D pointCuttee = new Point2D();
		Point2D pointCutter = new Point2D();

		int ivertexCuttee = vertices.get(0);
		;
		int ipartCuttee = editShape.getPathFromVertex(ivertexCuttee);
		int igeometryCuttee = editShape.getGeometryFromPath(ipartCuttee);
		editShape.getXY(ivertexCuttee, pointCuttee);

		int istart = 1;
		int ivertex = 0;
		while (istart < pointCount - 1) {
			boolean bCutEvent = false;
			for (int i = istart; i < pointCount; i++) {
				if (i == ivertex)
					continue;

				int ivertexCutter = vertices.get(i);
				int ipartCutter = editShape.getPathFromVertex(ivertexCutter);
				int igeometryCutter = editShape
						.getGeometryFromPath(ipartCutter);
				editShape.getXY(ivertexCutter, pointCutter);

				if (pointCuttee.isEqual(pointCutter)) {
					boolean bCondition = igeometryCuttee == cutteeHandle
							&& igeometryCutter == cutterHandle;

					if (bCondition)
						bCutEvent = _cutteeCutterEvents(eventIndex,
								eventIndexTemp, editShape, cutEvents,
								cutEventsTemp, ipartCuttee, ivertexCuttee,
								ipartCutter, ivertexCutter);
				} else
					break;
			}

			if (bCutEvent || ivertex == istart - 1) {
				if (bCutEvent && (ivertex == istart - 1))
					istart--;

				if (++ivertex == pointCount)
					break;

				ivertexCuttee = vertices.get(ivertex);
				ipartCuttee = editShape.getPathFromVertex(ivertexCuttee);
				igeometryCuttee = editShape.getGeometryFromPath(ipartCuttee);
				editShape.getXY(ivertexCuttee, pointCuttee);
			}

			if (!bCutEvent)
				istart = ivertex + 1;
		}

		ArrayList<CutEvent> cutEventsSorted = new ArrayList<CutEvent>(0);

		// Sort CutEvents
		int icutEvent;
		int icutEventTemp;
		for (int igeometry = editShape.getFirstGeometry(); igeometry != -1; igeometry = editShape
				.getNextGeometry(igeometry)) {
			for (int ipath = editShape.getFirstPath(igeometry); ipath != -1; ipath = editShape
					.getNextPath(ipath)) {
				for (int iv = editShape.getFirstVertex(ipath), i = 0, n = editShape
						.getPathSize(ipath); i < n; iv = editShape
						.getNextVertex(iv), i++) {
					icutEventTemp = editShape.getUserIndex(iv, eventIndexTemp);
					if (icutEventTemp >= 0) {
						// _ASSERT(cutEventsTemp.get(icutEventTemp).m_ivertexCuttee
						// == iv);
						while (icutEventTemp < cutEventsTemp.size()
								&& cutEventsTemp.get(icutEventTemp).m_ivertexCuttee == iv)
							cutEventsSorted.add(cutEventsTemp
									.get(icutEventTemp++));
					}

					icutEvent = editShape.getUserIndex(iv, eventIndex);
					if (icutEvent >= 0) {
						// _ASSERT(cutEvents->Get(icutEvent)->m_ivertexCuttee ==
						// iv);
						while (icutEvent < cutEvents.size()
								&& cutEvents.get(icutEvent).m_ivertexCuttee == iv)
							cutEventsSorted.add(cutEvents.get(icutEvent++));
					}
				}
			}
		}

		// _ASSERT(cutEvents->Size() + cutEventsTemp->Size() ==
		// cutEventsSorted->Size());
		editShape.removeUserIndex(eventIndex);
		editShape.removeUserIndex(eventIndexTemp);
		return cutEventsSorted;
	}

	static boolean _cutteeCutterEvents(int eventIndex, int eventIndexTemp,
			EditShape editShape, ArrayList<CutEvent> cutEvents,
			ArrayList<CutEvent> cutEventsTemp, int ipartCuttee,
			int ivertexCuttee, int ipartCutter, int ivertexCutter) {
		int ilastVertexCuttee = editShape.getLastVertex(ipartCuttee);
		int ilastVertexCutter = editShape.getLastVertex(ipartCutter);
		int ifirstVertexCuttee = editShape.getFirstVertex(ipartCuttee);
		int ifirstVertexCutter = editShape.getFirstVertex(ipartCutter);
		int ivertexCutteePrev = editShape.getPrevVertex(ivertexCuttee);
		int ivertexCutterPrev = editShape.getPrevVertex(ivertexCutter);

		boolean bEndEnd = false;
		boolean bEndStart = false;
		boolean bStartEnd = false;
		boolean bStartStart = false;

		if (ivertexCuttee != ifirstVertexCuttee) {
			if (ivertexCutter != ifirstVertexCutter)
				bEndEnd = _cutteeEndCutterEndEvent(eventIndex, editShape,
						cutEvents, ipartCuttee, ivertexCutteePrev, ipartCutter,
						ivertexCutterPrev);

			if (ivertexCutter != ilastVertexCutter)
				bEndStart = _cutteeEndCutterStartEvent(eventIndex, editShape,
						cutEvents, ipartCuttee, ivertexCutteePrev, ipartCutter,
						ivertexCutter);
		}

		if (ivertexCuttee != ilastVertexCuttee) {
			if (ivertexCutter != ifirstVertexCutter)
				bStartEnd = _cutteeStartCutterEndEvent(eventIndexTemp,
						editShape, cutEventsTemp, ipartCuttee, ivertexCuttee,
						ipartCutter, ivertexCutterPrev, ifirstVertexCuttee);

			if (ivertexCutter != ilastVertexCutter)
				bStartStart = _cutteeStartCutterStartEvent(eventIndexTemp,
						editShape, cutEventsTemp, ipartCuttee, ivertexCuttee,
						ipartCutter, ivertexCutter, ifirstVertexCuttee);
		}

		if (bEndEnd && bEndStart && bStartEnd) {
			int iendstart = cutEvents.size() - 1;
			int istartend = (bStartStart ? cutEventsTemp.size() - 2
					: cutEventsTemp.size() - 1);

			if (cutEventsTemp.get(istartend).m_count == 2) {
				// Replace bEndEnd with bEndStart, and remove duplicate
				// bEndStart (get rid of bEndEnd)
				cutEvents.set(iendstart - 1, cutEvents.get(iendstart));
				cutEvents.remove(cutEvents.size() - 1);
			}
		} else if (bEndEnd && bEndStart && bStartStart) {
			int istartstart = cutEventsTemp.size() - 1;

			if (cutEventsTemp.get(istartstart).m_count == 2) {
				// Remove bEndStart
				CutEvent lastEvent = cutEvents.get(cutEvents.size() - 1);
				cutEvents.remove(cutEvents.get(cutEvents.size() - 1));
				int icutEvent = editShape.getUserIndex(
						lastEvent.m_ivertexCuttee, eventIndex);
				if (icutEvent == cutEvents.size())
					editShape.setUserIndex(lastEvent.m_ivertexCuttee,
							eventIndex, -1);
			}
		}

		return bEndEnd || bEndStart || bStartEnd || bStartStart;
	}

	private static boolean _cutteeEndCutterEndEvent(int eventIndex,
			EditShape editShape, ArrayList<CutEvent> cutEvents,
			int ipartCuttee, int ivertexCuttee, int ipartCutter,
			int ivertexCutter) {
		Segment segmentCuttee;
		Segment segmentCutter;
		Line lineCuttee = new Line();
		Line lineCutter = new Line();
		double[] scalarsCuttee = new double[2];
		double[] scalarsCutter = new double[2];

		CutEvent cutEvent;

		segmentCuttee = editShape.getSegment(ivertexCuttee);
		if (segmentCuttee == null) {
			editShape.queryLineConnector(ivertexCuttee, lineCuttee);
			segmentCuttee = lineCuttee;
		}

		segmentCutter = editShape.getSegment(ivertexCutter);
		if (segmentCutter == null) {
			editShape.queryLineConnector(ivertexCutter, lineCutter);
			segmentCutter = lineCutter;
		}

		int count = segmentCuttee.intersect(segmentCutter, null, scalarsCuttee,
				scalarsCutter, 0.0);
		// _ASSERT(count > 0);
		int icutEvent;

		// If count == 2 (i.e. when they overlap), this this event would have
		// been discovered by _CutteeStartCutterStartEvent at the previous index
		if (count < 2) {
			cutEvent = new CutEvent(ivertexCuttee, ipartCuttee,
					scalarsCuttee[0], NumberUtils.NaN(), count, ivertexCutter,
					ipartCutter, scalarsCutter[0], NumberUtils.NaN());
			cutEvents.add(cutEvent);
			icutEvent = editShape.getUserIndex(ivertexCuttee, eventIndex);

			if (icutEvent < 0)
				editShape.setUserIndex(ivertexCuttee, eventIndex,
						cutEvents.size() - 1);
		}

		return true;
	}

	private static boolean _cutteeEndCutterStartEvent(int eventIndex,
			EditShape editShape, ArrayList<CutEvent> cutEvents,
			int ipartCuttee, int ivertexCuttee, int ipartCutter,
			int ivertexCutter) {
		Segment segmentCuttee;
		Segment segmentCutter;
		Line lineCuttee = new Line();
		Line lineCutter = new Line();
		double[] scalarsCuttee = new double[2];
		double[] scalarsCutter = new double[2];

		CutEvent cutEvent;

		segmentCuttee = editShape.getSegment(ivertexCuttee);
		if (segmentCuttee == null) {
			editShape.queryLineConnector(ivertexCuttee, lineCuttee);
			segmentCuttee = lineCuttee;
		}

		segmentCutter = editShape.getSegment(ivertexCutter);
		if (segmentCutter == null) {
			editShape.queryLineConnector(ivertexCutter, lineCutter);
			segmentCutter = lineCutter;
		}

		int count = segmentCuttee.intersect(segmentCutter, null, scalarsCuttee,
				scalarsCutter, 0.0);
		// _ASSERT(count > 0);
		int icutEvent;

		// If count == 2 (i.e. when they overlap), this this event would have
		// been discovered by _CutteeStartCutterEndEvent at the previous index
		if (count < 2) {
			cutEvent = new CutEvent(ivertexCuttee, ipartCuttee,
					scalarsCuttee[0], NumberUtils.NaN(), count, ivertexCutter,
					ipartCutter, scalarsCutter[0], NumberUtils.NaN());
			cutEvents.add(cutEvent);
			icutEvent = editShape.getUserIndex(ivertexCuttee, eventIndex);

			if (icutEvent < 0)
				editShape.setUserIndex(ivertexCuttee, eventIndex,
						cutEvents.size() - 1);

			return true;
		}

		return false;
	}

	private static boolean _cutteeStartCutterEndEvent(int eventIndex,
			EditShape editShape, ArrayList<CutEvent> cutEvents,
			int ipartCuttee, int ivertexCuttee, int ipartCutter,
			int ivertexCutter, int ifirstVertexCuttee) {
		Segment segmentCuttee;
		Segment segmentCutter;
		Line lineCuttee = new Line();
		Line lineCutter = new Line();
		double[] scalarsCuttee = new double[2];
		double[] scalarsCutter = new double[2];

		CutEvent cutEvent;

		segmentCuttee = editShape.getSegment(ivertexCuttee);
		if (segmentCuttee == null) {
			editShape.queryLineConnector(ivertexCuttee, lineCuttee);
			segmentCuttee = lineCuttee;
		}

		segmentCutter = editShape.getSegment(ivertexCutter);
		if (segmentCutter == null) {
			editShape.queryLineConnector(ivertexCutter, lineCutter);
			segmentCutter = lineCutter;
		}

		int count = segmentCuttee.intersect(segmentCutter, null, scalarsCuttee,
				scalarsCutter, 0.0);
		// _ASSERT(count > 0);
		int icutEvent;

		if (count == 2) {
			cutEvent = new CutEvent(ivertexCuttee, ipartCuttee,
					scalarsCuttee[0], scalarsCuttee[1], count, ivertexCutter,
					ipartCutter, scalarsCutter[0], scalarsCutter[1]);
			cutEvents.add(cutEvent);
			icutEvent = editShape.getUserIndex(ivertexCuttee, eventIndex);

			if (icutEvent < 0)
				editShape.setUserIndex(ivertexCuttee, eventIndex,
						cutEvents.size() - 1);

			return true;
		} else {
			boolean bCutEvent = false;

			if (ivertexCuttee == ifirstVertexCuttee) {
				cutEvent = new CutEvent(ivertexCuttee, ipartCuttee,
						scalarsCuttee[0], NumberUtils.NaN(), count,
						ivertexCutter, ipartCutter, scalarsCutter[0],
						NumberUtils.NaN());
				cutEvents.add(cutEvent);
				icutEvent = editShape.getUserIndex(ivertexCuttee, eventIndex);

				if (icutEvent < 0)
					editShape.setUserIndex(ivertexCuttee, eventIndex,
							cutEvents.size() - 1);

				bCutEvent = true;
			}

			return bCutEvent;
		}

	}

	private static boolean _cutteeStartCutterStartEvent(int eventIndex,
			EditShape editShape, ArrayList<CutEvent> cutEvents,
			int ipartCuttee, int ivertexCuttee, int ipartCutter,
			int ivertexCutter, int ifirstVertexCuttee) {
		Segment segmentCuttee;
		Segment segmentCutter;
		Line lineCuttee = new Line();
		Line lineCutter = new Line();
		double[] scalarsCuttee = new double[2];
		double[] scalarsCutter = new double[2];

		CutEvent cutEvent;

		segmentCuttee = editShape.getSegment(ivertexCuttee);
		if (segmentCuttee == null) {
			editShape.queryLineConnector(ivertexCuttee, lineCuttee);
			segmentCuttee = lineCuttee;
		}

		segmentCutter = editShape.getSegment(ivertexCutter);
		if (segmentCutter == null) {
			editShape.queryLineConnector(ivertexCutter, lineCutter);
			segmentCutter = lineCutter;
		}

		int count = segmentCuttee.intersect(segmentCutter, null, scalarsCuttee,
				scalarsCutter, 0.0);
		// _ASSERT(count > 0);
		int icutEvent;

		if (count == 2) {
			cutEvent = new CutEvent(ivertexCuttee, ipartCuttee,
					scalarsCuttee[0], scalarsCuttee[1], count, ivertexCutter,
					ipartCutter, scalarsCutter[0], scalarsCutter[1]);
			cutEvents.add(cutEvent);
			icutEvent = editShape.getUserIndex(ivertexCuttee, eventIndex);

			if (icutEvent < 0)
				editShape.setUserIndex(ivertexCuttee, eventIndex,
						cutEvents.size() - 1);

			return true;
		} else {
			boolean bCutEvent = false;

			if (ivertexCuttee == ifirstVertexCuttee) {
				cutEvent = new CutEvent(ivertexCuttee, ipartCuttee,
						scalarsCuttee[0], NumberUtils.NaN(), count,
						ivertexCutter, ipartCutter, scalarsCutter[0],
						NumberUtils.NaN());
				cutEvents.add(cutEvent);
				icutEvent = editShape.getUserIndex(ivertexCuttee, eventIndex);

				if (icutEvent < 0)
					editShape.setUserIndex(ivertexCuttee, eventIndex,
							cutEvents.size() - 1);

				bCutEvent = true;
			}

			return bCutEvent;
		}

	}

	static void _Cut(boolean bConsiderTouch, boolean bLocalCutsOnly,
			ArrayList<CutEvent> cutEvents, EditShape shape,
			ArrayList<OperatorCutLocal.CutPair> cutPairs,
			AttributeStreamOfInt32 segmentCounts) {
		OperatorCutLocal.CutPair cutPair;

		Point2D[] tangents = new Point2D[4];
		tangents[0] = new Point2D();
		tangents[1] = new Point2D();
		tangents[2] = new Point2D();
		tangents[3] = new Point2D();
		Point2D tangent0 = new Point2D();
		Point2D tangent1 = new Point2D();
		Point2D tangent2 = new Point2D();
		Point2D tangent3 = new Point2D();

		SegmentBuffer segmentBufferCuttee = null;
		if (cutPairs != null) {
			segmentBufferCuttee = new SegmentBuffer();
			segmentBufferCuttee.createLine();
		}

		Segment segmentCuttee = null;
		int icutEvent = 0;
		MultiPath multipath = null;

		Line lineCuttee = new Line();
		Line lineCutter = new Line();

		int polyline = shape.getFirstGeometry();
		for (int ipath = shape.getFirstPath(polyline); ipath != -1; ipath = shape
				.getNextPath(ipath)) {
			int cut;
			int cutPrev = OperatorCutLocal.Side.Uncut;
			int ipartCuttee = -1;
			int ivertexCuttee = -1;
			double scalarCuttee = NumberUtils.NaN();
			int ipartCutteePrev = -1;
			int ivertexCutteePrev = -1;
			double scalarCutteePrev = NumberUtils.NaN();
			int ipartCutter = -1;
			int ivertexCutter = -1;
			double scalarCutter = NumberUtils.NaN();
			int ipartCutterPrev = -1;
			int ivertexCutterPrev = -1;
			double scalarCutterPrev = NumberUtils.NaN();
			boolean bNoCutYet = true; // Indicates whether a cut as occured for
										// the current part
			boolean bCoincidentNotAdded = false; // Indicates whether the
													// current coincident
													// multipath has been added
													// to cutPairs
			boolean bCurrentMultiPathNotAdded = true; // Indicates whether there
														// is a multipath not
														// yet added to cutPairs
														// (left, right, or
														// undefined)
			boolean bStartNewPath = true;
			boolean bCreateNewMultiPath = true;
			int segmentCount = 0;

			ipartCutteePrev = ipath;
			scalarCutteePrev = 0.0;

			for (int ivertex = shape.getFirstVertex(ipath), n = shape
					.getPathSize(ipath), i = 0; i < n; ivertex = shape
					.getNextVertex(ivertex), i++) {
				segmentCuttee = shape.getSegment(ivertex);
				if (segmentCuttee == null) {
					if (!shape.queryLineConnector(ivertex, lineCuttee))
						continue;
					segmentCuttee = lineCuttee;
				}

				if (ivertexCutteePrev == -1)
					ivertexCutteePrev = ivertex;

				double lastScalarCuttee = 0.0; // last scalar along the current
												// segment

				while (icutEvent < cutEvents.size()
						&& ivertex == cutEvents.get(icutEvent).m_ivertexCuttee) {
					ipartCuttee = cutEvents.get(icutEvent).m_ipartCuttee;
					ivertexCuttee = cutEvents.get(icutEvent).m_ivertexCuttee;
					scalarCuttee = cutEvents.get(icutEvent).m_scalarCuttee0;
					ipartCutter = cutEvents.get(icutEvent).m_ipartCutter;
					ivertexCutter = cutEvents.get(icutEvent).m_ivertexCutter;
					scalarCutter = cutEvents.get(icutEvent).m_scalarCutter0;

					if (cutEvents.get(icutEvent).m_count == 2) {
						// We have an overlap

						if (!bCoincidentNotAdded) {
							ipartCutteePrev = ipartCuttee;
							ivertexCutteePrev = ivertexCuttee;
							scalarCutteePrev = scalarCuttee;
							ipartCutterPrev = ipartCutter;
							ivertexCutterPrev = ivertexCutter;
							scalarCutterPrev = scalarCutter;
							cutPrev = OperatorCutLocal.Side.Coincident;

							// Create new multipath
							if (cutPairs != null)
								multipath = new Polyline();
							else
								segmentCount = 0;

							bCreateNewMultiPath = false;
							bStartNewPath = true;
						}

						scalarCuttee = cutEvents.get(icutEvent).m_scalarCuttee1;
						scalarCutter = cutEvents.get(icutEvent).m_scalarCutter1;

						if (cutPairs != null) {
							segmentCuttee.cut(lastScalarCuttee,
									cutEvents.get(icutEvent).m_scalarCuttee1,
									segmentBufferCuttee);
							multipath.addSegment(segmentBufferCuttee.get(),
									bStartNewPath);
						} else
							segmentCount++;

						lastScalarCuttee = scalarCuttee;

						bCoincidentNotAdded = true;
						bNoCutYet = false;
						bStartNewPath = false;

						if (icutEvent + 1 == cutEvents.size()
								|| cutEvents.get(icutEvent + 1).m_count != 2
								|| cutEvents.get(icutEvent + 1).m_ivertexCuttee == ivertexCuttee
								&& cutEvents.get(icutEvent + 1).m_scalarCuttee0 != lastScalarCuttee) {
							if (cutPairs != null) {
								cutPair = new OperatorCutLocal.CutPair(
										(Geometry) multipath,
										OperatorCutLocal.Side.Coincident,
										ipartCuttee, ivertexCuttee,
										scalarCuttee, cutPrev, ipartCutteePrev,
										ivertexCutteePrev, scalarCutteePrev,
										ipartCutter, ivertexCutter,
										scalarCutter, ipartCutterPrev,
										ivertexCutterPrev, scalarCutterPrev);
								cutPairs.add(cutPair);
							} else {
								segmentCounts.add(segmentCount);
							}

							ipartCutteePrev = ipartCuttee;
							ivertexCutteePrev = ivertexCuttee;
							scalarCutteePrev = scalarCuttee;
							ipartCutterPrev = ipartCutter;
							ivertexCutterPrev = ivertexCutter;
							scalarCutterPrev = scalarCutter;
							cutPrev = OperatorCutLocal.Side.Coincident;

							bNoCutYet = false;
							bCoincidentNotAdded = false;
							bCreateNewMultiPath = true;
							bStartNewPath = true;
						}

						icutEvent++;
						continue;
					}

					int ivertexCutteePlus = shape.getNextVertex(ivertexCuttee);
					int ivertexCutterPlus = shape.getNextVertex(ivertexCutter);
					int ivertexCutterMinus = shape.getPrevVertex(ivertexCutter);

					if (icutEvent < cutEvents.size() - 1
							&& cutEvents.get(icutEvent + 1).m_ivertexCuttee == ivertexCutteePlus
							&& cutEvents.get(icutEvent + 1).m_ivertexCutter == ivertexCutter
							&& cutEvents.get(icutEvent + 1).m_count == 2) {
						if (scalarCuttee != lastScalarCuttee) {
							if (bCreateNewMultiPath) {
								if (cutPairs != null)
									multipath = new Polyline();
								else
									segmentCount = 0;
							}

							if (icutEvent > 0
									&& cutEvents.get(icutEvent - 1).m_ipartCuttee == ipartCuttee) {
								if (cutPrev == OperatorCutLocal.Side.Right)
									cut = OperatorCutLocal.Side.Left;
								else if (cutPrev == OperatorCutLocal.Side.Left)
									cut = OperatorCutLocal.Side.Right;
								else
									cut = OperatorCutLocal.Side.Undefined;
							} else
								cut = OperatorCutLocal.Side.Undefined;

							if (cutPairs != null) {
								segmentCuttee.cut(lastScalarCuttee,
										scalarCuttee, segmentBufferCuttee);
								multipath.addSegment(segmentBufferCuttee.get(),
										bStartNewPath);
								cutPair = new OperatorCutLocal.CutPair(
										multipath, cut, ipartCuttee,
										ivertexCuttee, scalarCuttee, cutPrev,
										ipartCutteePrev, ivertexCutteePrev,
										scalarCutteePrev, ipartCutter,
										ivertexCutter, scalarCutter,
										ipartCutterPrev, ivertexCutterPrev,
										scalarCutterPrev);
								cutPairs.add(cutPair);
							} else {
								segmentCount++;
								segmentCounts.add(segmentCount);
							}

							lastScalarCuttee = scalarCuttee;

							ipartCutteePrev = ipartCuttee;
							ivertexCutteePrev = ivertexCuttee;
							scalarCutteePrev = scalarCuttee;
							ipartCutterPrev = ipartCutter;
							ivertexCutterPrev = ivertexCutter;
							scalarCutterPrev = scalarCutter;
							cutPrev = cut;

							bCurrentMultiPathNotAdded = false;
							bNoCutYet = false;
							bCreateNewMultiPath = true;
							bStartNewPath = true;
						}

						icutEvent++;
						continue;
					}

					boolean bContinue = _cutterTangents(bConsiderTouch, shape,
							cutEvents, icutEvent, tangent0, tangent1);
					if (bContinue) {
						icutEvent++;
						continue;
					}

					_cutteeTangents(shape, cutEvents, icutEvent, ipath,
							ivertex, tangent2, tangent3);

					boolean bCut = false;
					boolean bTouch = false;
					boolean bCutRight = true;

					if (!tangent0.isEqual(tangent2)
							&& !tangent1.isEqual(tangent2)
							&& !tangent0.isEqual(tangent3)
							&& !tangent1.isEqual(tangent3)) {
						tangents[0].setCoords(tangent0);
						tangents[1].setCoords(tangent1);
						tangents[2].setCoords(tangent2);
						tangents[3].setCoords(tangent3);

						Arrays.sort(tangents, new Point2D.CompareVectors());
						// SORTARRAY(tangents, Point2D,
						// Point2D::_CompareVectors);

						Point2D value0 = (Point2D) tangents[0];
						Point2D value1 = (Point2D) tangents[1];
						Point2D value2 = (Point2D) tangents[2];
						Point2D value3 = (Point2D) tangents[3];

						if (value0.isEqual(tangent0)) {
							if (value1.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = false;
								}
							} else if (value3.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = true;
								}
							} else {
								bCut = true;
								bCutRight = value1.isEqual(tangent2);
							}
						} else if (value1.isEqual(tangent0)) {
							if (value2.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = false;
								}
							} else if (value0.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = true;
								}
							} else {
								bCut = true;
								bCutRight = value2.isEqual(tangent2);
							}
						} else if (value2.isEqual(tangent0)) {
							if (value3.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = false;
								}
							} else if (value1.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = true;
								}
							} else {
								bCut = true;
								bCutRight = value3.isEqual(tangent2);
							}
						} else {
							if (value0.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = false;
								}
							} else if (value2.isEqual(tangent1)) {
								if (!bConsiderTouch)
									bCut = false;
								else {
									bCut = true;
									bTouch = true;
									bCutRight = true;
								}
							} else {
								bCut = true;
								bCutRight = value0.isEqual(tangent2);
							}
						}
					}

					if (bCut) {
						boolean bIsFirstSegmentInPath = (ivertex == ivertexCuttee);

						if (scalarCuttee != lastScalarCuttee
								|| bIsFirstSegmentInPath
								&& lastScalarCuttee == 0.0) {
							if (bCreateNewMultiPath) {
								if (cutPairs != null)
									multipath = new Polyline();
								else
									segmentCount = 0;
							}

							if (cutPairs != null) {
								segmentCuttee.cut(lastScalarCuttee,
										scalarCuttee, segmentBufferCuttee);
								multipath.addSegment(segmentBufferCuttee.get(),
										bStartNewPath);
							} else
								segmentCount++;
						}

						if (bCutRight) {
							if (cutPrev != OperatorCutLocal.Side.Right
									|| bLocalCutsOnly) {
								if (scalarCuttee != lastScalarCuttee
										|| bIsFirstSegmentInPath
										&& lastScalarCuttee == 0.0
										|| bLocalCutsOnly) {
									if (cutPairs != null) {
										cutPair = new OperatorCutLocal.CutPair(
												multipath,
												OperatorCutLocal.Side.Right,
												ipartCuttee, ivertexCuttee,
												scalarCuttee, cutPrev,
												ipartCutteePrev,
												ivertexCutteePrev,
												scalarCutteePrev, ipartCutter,
												ivertexCutter, scalarCutter,
												ipartCutterPrev,
												ivertexCutterPrev,
												scalarCutterPrev);
										cutPairs.add(cutPair);
									} else {
										segmentCounts.add(segmentCount);
									}
								}

								if (!bTouch)
									cutPrev = OperatorCutLocal.Side.Right;
								else if (icutEvent == cutEvents.size() - 2
										|| cutEvents.get(icutEvent + 2).m_ipartCuttee != ipartCuttee)
									cutPrev = OperatorCutLocal.Side.Left;
							} else {
								if (scalarCuttee != lastScalarCuttee
										|| bIsFirstSegmentInPath
										&& lastScalarCuttee == 0.0
										|| bLocalCutsOnly) {
									if (cutPairs != null) {
										cutPair = new OperatorCutLocal.CutPair(
												multipath,
												OperatorCutLocal.Side.Undefined,
												ipartCuttee, ivertexCuttee,
												scalarCuttee, cutPrev,
												ipartCutteePrev,
												ivertexCutteePrev,
												scalarCutteePrev, ipartCutter,
												ivertexCutter, scalarCutter,
												ipartCutterPrev,
												ivertexCutterPrev,
												scalarCutterPrev);
										cutPairs.add(cutPair);
									} else {
										segmentCounts.add(segmentCount);
									}
								}

								cutPrev = OperatorCutLocal.Side.Right;
							}
						} else {
							if (cutPrev != OperatorCutLocal.Side.Left
									|| bLocalCutsOnly) {
								if (scalarCuttee != lastScalarCuttee
										|| bIsFirstSegmentInPath
										&& lastScalarCuttee == 0.0
										|| bLocalCutsOnly) {
									if (cutPairs != null) {
										cutPair = new OperatorCutLocal.CutPair(
												multipath,
												OperatorCutLocal.Side.Left,
												ipartCuttee, ivertexCuttee,
												scalarCuttee, cutPrev,
												ipartCutteePrev,
												ivertexCutteePrev,
												scalarCutteePrev, ipartCutter,
												ivertexCutter, scalarCutter,
												ipartCutterPrev,
												ivertexCutterPrev,
												scalarCutterPrev);
										cutPairs.add(cutPair);
									} else {
										segmentCounts.add(segmentCount);
									}
								}

								if (!bTouch)
									cutPrev = OperatorCutLocal.Side.Left;
								else if (icutEvent == cutEvents.size() - 2
										|| cutEvents.get(icutEvent + 2).m_ipartCuttee != ipartCuttee)
									cutPrev = OperatorCutLocal.Side.Right;
							} else {
								if (scalarCuttee != lastScalarCuttee
										|| bIsFirstSegmentInPath
										&& lastScalarCuttee == 0.0
										|| bLocalCutsOnly) {
									if (cutPairs != null) {
										cutPair = new OperatorCutLocal.CutPair(
												multipath,
												OperatorCutLocal.Side.Undefined,
												ipartCuttee, ivertexCuttee,
												scalarCuttee, cutPrev,
												ipartCutteePrev,
												ivertexCutteePrev,
												scalarCutteePrev, ipartCutter,
												ivertexCutter, scalarCutter,
												ipartCutterPrev,
												ivertexCutterPrev,
												scalarCutterPrev);
										cutPairs.add(cutPair);
									} else {
										segmentCounts.add(segmentCount);
									}
								}

								cutPrev = OperatorCutLocal.Side.Left;
							}
						}

						if (scalarCuttee != lastScalarCuttee
								|| bIsFirstSegmentInPath
								&& lastScalarCuttee == 0.0 || bLocalCutsOnly) {
							lastScalarCuttee = scalarCuttee;

							ipartCutteePrev = ipartCuttee;
							ivertexCutteePrev = ivertexCuttee;
							scalarCutteePrev = scalarCuttee;
							ipartCutterPrev = ipartCutter;
							ivertexCutterPrev = ivertexCutter;
							scalarCutterPrev = scalarCutter;

							bCurrentMultiPathNotAdded = false;
							bNoCutYet = false;
							bCreateNewMultiPath = true;
							bStartNewPath = true;
						}
					}

					icutEvent++;
				}

				if (lastScalarCuttee != 1.0) {
					if (bCreateNewMultiPath) {
						if (cutPairs != null)
							multipath = new Polyline();
						else
							segmentCount = 0;
					}

					if (cutPairs != null) {
						segmentCuttee.cut(lastScalarCuttee, 1.0,
								segmentBufferCuttee);
						multipath.addSegment(segmentBufferCuttee.get(),
								bStartNewPath);
					} else
						segmentCount++;

					bCreateNewMultiPath = false;
					bStartNewPath = false;
					bCurrentMultiPathNotAdded = true;
				}
			}

			if (bCurrentMultiPathNotAdded) {
				scalarCuttee = 1.0;
				ivertexCuttee = shape.getLastVertex(ipath);
				ivertexCuttee = shape.getPrevVertex(ivertexCuttee);

				ipartCutter = -1;
				ivertexCutter = -1;
				scalarCutter = NumberUtils.NaN();

				if (bNoCutYet) {
					if (cutPairs != null) {
						cutPair = new OperatorCutLocal.CutPair(multipath,
								OperatorCutLocal.Side.Uncut, ipartCuttee,
								ivertexCuttee, scalarCuttee, cutPrev,
								ipartCutteePrev, ivertexCutteePrev,
								scalarCutteePrev, ipartCutter, ivertexCutter,
								scalarCutter, ipartCutterPrev,
								ivertexCutterPrev, scalarCutterPrev);
						cutPairs.add(cutPair);
					} else {
						segmentCounts.add(segmentCount);
					}
				} else {
					if (cutPrev == OperatorCutLocal.Side.Right)
						cut = OperatorCutLocal.Side.Left;
					else if (cutPrev == OperatorCutLocal.Side.Left)
						cut = OperatorCutLocal.Side.Right;
					else
						cut = OperatorCutLocal.Side.Undefined;

					if (cutPairs != null) {
						cutPair = new OperatorCutLocal.CutPair(multipath, cut,
								ipartCuttee, ivertexCuttee, scalarCuttee,
								cutPrev, ipartCutteePrev, ivertexCutteePrev,
								scalarCutteePrev, ipartCutter, ivertexCutter,
								scalarCutter, ipartCutterPrev,
								ivertexCutterPrev, scalarCutterPrev);
						cutPairs.add(cutPair);
					} else {
						segmentCounts.add(segmentCount);
					}
				}
			}
		}
	}

	static boolean _cutterTangents(boolean bConsiderTouch, EditShape shape,
			ArrayList<CutEvent> cutEvents, int icutEvent, Point2D tangent0,
			Point2D tangent1) {
		double scalarCutter = cutEvents.get(icutEvent).m_scalarCutter0;

		if (scalarCutter == 1.0)
			return _cutterEndTangents(bConsiderTouch, shape, cutEvents,
					icutEvent, tangent0, tangent1);

		if (scalarCutter == 0.0)
			return _cutterStartTangents(bConsiderTouch, shape, cutEvents,
					icutEvent, tangent0, tangent1);

		throw GeometryException.GeometryInternalError();
	}

	static boolean _cutterEndTangents(boolean bConsiderTouch, EditShape shape,
			ArrayList<CutEvent> cutEvents, int icutEvent, Point2D tangent0,
			Point2D tangent1) {
		Line lineCutter = new Line();
		Segment segmentCutter;

		int ivertexCuttee = cutEvents.get(icutEvent).m_ivertexCuttee;
		int ipartCutter = cutEvents.get(icutEvent).m_ipartCutter;
		int ivertexCutter = cutEvents.get(icutEvent).m_ivertexCutter;

		int ivertexCutteePrev = -1;
		int ipartCutterPrev = -1;
		int ivertexCutterPrev = -1;
		int countPrev = -1;

		if (!bConsiderTouch && icutEvent > 0) {
			CutEvent cutEvent = cutEvents.get(icutEvent - 1);
			ivertexCutteePrev = cutEvent.m_ivertexCuttee;
			ipartCutterPrev = cutEvent.m_ipartCutter;
			ivertexCutterPrev = cutEvent.m_ivertexCutter;
			countPrev = cutEvent.m_count;
		}

		int ivertexCutteeNext = -1;
		int ipartCutterNext = -1;
		int ivertexCutterNext = -1;
		int countNext = -1;

		if (icutEvent < cutEvents.size() - 1) {
			CutEvent cutEvent = cutEvents.get(icutEvent + 1);
			ivertexCutteeNext = cutEvent.m_ivertexCuttee;
			ipartCutterNext = cutEvent.m_ipartCutter;
			ivertexCutterNext = cutEvent.m_ivertexCutter;
			countNext = cutEvent.m_count;
		}

		int ivertexCutteePlus = shape.getNextVertex(ivertexCuttee);
		int ivertexCutterPlus = shape.getNextVertex(ivertexCutter);

		if (!bConsiderTouch) {
			if ((icutEvent > 0 && ivertexCutteePrev == ivertexCuttee
					&& ipartCutterPrev == ipartCutter
					&& ivertexCutterPrev == ivertexCutterPlus && countPrev == 2)
					|| (icutEvent < cutEvents.size() - 1
							&& ivertexCutteeNext == ivertexCutteePlus
							&& ipartCutterNext == ipartCutter
							&& ivertexCutterNext == ivertexCutterPlus && countNext == 2)) {
				segmentCutter = shape.getSegment(ivertexCutter);
				if (segmentCutter == null) {
					shape.queryLineConnector(ivertexCutter, lineCutter);
					segmentCutter = lineCutter;
				}

				tangent1.setCoords(segmentCutter._getTangent(1.0));
				tangent0.negate(tangent1);
				tangent1.normalize();
				tangent0.normalize();

				return false;
			}

			if (icutEvent < cutEvents.size() - 1
					&& ivertexCutteeNext == ivertexCuttee
					&& ipartCutterNext == ipartCutter
					&& ivertexCutterNext == ivertexCutterPlus) {
				segmentCutter = shape.getSegment(ivertexCutter);
				if (segmentCutter == null) {
					shape.queryLineConnector(ivertexCutter, lineCutter);
					segmentCutter = lineCutter;
				}

				tangent0.setCoords(segmentCutter._getTangent(1.0));

				segmentCutter = shape.getSegment(ivertexCutterPlus);
				if (segmentCutter == null) {
					shape.queryLineConnector(ivertexCutterPlus, lineCutter);
					segmentCutter = lineCutter;
				}

				tangent1.setCoords(segmentCutter._getTangent(0.0));
				tangent0.negate();
				tangent1.normalize();
				tangent0.normalize();

				return false;
			}

			return true;
		}

		if (icutEvent == cutEvents.size() - 1
				|| ivertexCutteeNext != ivertexCuttee
				|| ipartCutterNext != ipartCutter
				|| ivertexCutterNext != ivertexCutterPlus || countNext == 2) {
			segmentCutter = shape.getSegment(ivertexCutter);
			if (segmentCutter == null) {
				shape.queryLineConnector(ivertexCutter, lineCutter);
				segmentCutter = lineCutter;
			}

			tangent1.setCoords(segmentCutter._getTangent(1.0));
			tangent0.negate(tangent1);
			tangent1.normalize();
			tangent0.normalize();

			return false;
		}

		segmentCutter = shape.getSegment(ivertexCutter);
		if (segmentCutter == null) {
			shape.queryLineConnector(ivertexCutter, lineCutter);
			segmentCutter = lineCutter;
		}

		tangent0.setCoords(segmentCutter._getTangent(1.0));

		segmentCutter = shape.getSegment(ivertexCutterPlus);
		if (segmentCutter == null) {
			shape.queryLineConnector(ivertexCutterPlus, lineCutter);
			segmentCutter = lineCutter;
		}

		tangent1.setCoords(segmentCutter._getTangent(0.0));
		tangent0.negate();
		tangent1.normalize();
		tangent0.normalize();

		return false;
	}

	static boolean _cutterStartTangents(boolean bConsiderTouch,
			EditShape shape, ArrayList<CutEvent> cutEvents, int icutEvent,
			Point2D tangent0, Point2D tangent1) {
		Line lineCutter = new Line();
		Segment segmentCutter;

		int ivertexCuttee = cutEvents.get(icutEvent).m_ivertexCuttee;
		int ipartCutter = cutEvents.get(icutEvent).m_ipartCutter;
		int ivertexCutter = cutEvents.get(icutEvent).m_ivertexCutter;

		int ivertexCutteeNext = -1;
		int ipartCutterNext = -1;
		int ivertexCutterNext = -1;
		int countNext = -1;

		if (!bConsiderTouch && icutEvent < cutEvents.size() - 1) {
			CutEvent cutEvent = cutEvents.get(icutEvent + 1);
			ivertexCutteeNext = cutEvent.m_ivertexCuttee;
			ipartCutterNext = cutEvent.m_ipartCutter;
			ivertexCutterNext = cutEvent.m_ivertexCutter;
			countNext = cutEvent.m_count;
		}

		int ivertexCutteePrev = -1;
		int ipartCutterPrev = -1;
		int ivertexCutterPrev = -1;
		int countPrev = -1;

		if (icutEvent > 0) {
			CutEvent cutEvent = cutEvents.get(icutEvent - 1);
			ivertexCutteePrev = cutEvent.m_ivertexCuttee;
			ipartCutterPrev = cutEvent.m_ipartCutter;
			ivertexCutterPrev = cutEvent.m_ivertexCutter;
			countPrev = cutEvent.m_count;
		}

		int ivertexCutteePlus = shape.getNextVertex(ivertexCuttee);
		int ivertexCutterMinus = shape.getPrevVertex(ivertexCutter);

		if (!bConsiderTouch) {
			if ((icutEvent > 0 && ivertexCutteePrev == ivertexCuttee
					&& ipartCutterPrev == ipartCutter
					&& ivertexCutterPrev == ivertexCutterMinus && countPrev == 2)
					|| (icutEvent < cutEvents.size() - 1
							&& ivertexCutteeNext == ivertexCutteePlus
							&& ipartCutterNext == ipartCutter
							&& ivertexCutterNext == ivertexCutterMinus && countNext == 2)) {
				segmentCutter = shape.getSegment(ivertexCutter);
				if (segmentCutter == null) {
					shape.queryLineConnector(ivertexCutter, lineCutter);
					segmentCutter = lineCutter;
				}

				tangent1.setCoords(segmentCutter._getTangent(0.0));
				tangent0.negate(tangent1);
				tangent1.normalize();
				tangent0.normalize();

				return false;
			}

			return true;
		}

		if (icutEvent == 0 || ivertexCutteePrev != ivertexCuttee
				|| ipartCutterPrev != ipartCutter
				|| ivertexCutterPrev != ivertexCutterMinus || countPrev == 2) {
			segmentCutter = shape.getSegment(ivertexCutter);
			if (segmentCutter == null) {
				shape.queryLineConnector(ivertexCutter, lineCutter);
				segmentCutter = lineCutter;
			}

			tangent1.setCoords(segmentCutter._getTangent(0.0));
			tangent0.negate(tangent1);
			tangent1.normalize();
			tangent0.normalize();

			return false;
		}

		// Already processed the event

		return true;
	}

	static boolean _cutteeTangents(EditShape shape,
			ArrayList<CutEvent> cutEvents, int icutEvent, int ipath,
			int ivertex, Point2D tangent2, Point2D tangent3) {
		Line lineCuttee = new Line();
		Segment segmentCuttee = shape.getSegment(ivertex);
		if (segmentCuttee == null) {
			shape.queryLineConnector(ivertex, lineCuttee);
			segmentCuttee = lineCuttee;
		}

		CutEvent cutEvent = cutEvents.get(icutEvent);
		int ivertexCuttee = cutEvent.m_ivertexCuttee;
		double scalarCuttee = cutEvent.m_scalarCuttee0;

		int ivertexCutteePlus = shape.getNextVertex(ivertexCuttee);

		if (scalarCuttee == 1.0) {
			tangent2.setCoords(segmentCuttee._getTangent(1.0));

			if (ivertexCutteePlus != -1
					&& ivertexCutteePlus != shape.getLastVertex(ipath)) {
				segmentCuttee = shape.getSegment(ivertexCutteePlus);
				if (segmentCuttee == null) {
					shape.queryLineConnector(ivertexCutteePlus, lineCuttee);
					segmentCuttee = lineCuttee;
				}

				tangent3.setCoords(segmentCuttee._getTangent(0.0));

				segmentCuttee = shape.getSegment(ivertexCuttee);
				if (segmentCuttee == null) {
					shape.queryLineConnector(ivertexCuttee, lineCuttee);
					segmentCuttee = lineCuttee;
				}
			} else
				tangent3.setCoords(tangent2);

			tangent2.negate();

			tangent3.normalize();
			tangent2.normalize();

			return false;
		}

		if (scalarCuttee == 0.0) {
			tangent3.setCoords(segmentCuttee._getTangent(scalarCuttee));
			tangent2.negate(tangent3);
			tangent3.normalize();
			tangent2.normalize();

			return false;
		}

		throw GeometryException.GeometryInternalError();
	}
}
