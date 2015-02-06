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

class Simplificator {
	private EditShape m_shape;
	private int m_geometry;
	private IndexMultiDCList m_sortedVertices;

	private AttributeStreamOfInt32 m_bunchEdgeEndPoints;
	private AttributeStreamOfInt32 m_bunchEdgeCenterPoints;
	private AttributeStreamOfInt32 m_bunchEdgeIndices;
	// private AttributeStreamOfInt32 m_orphanVertices;

	private int m_dbgCounter;
	private int m_sortedVerticesListIndex;
	private int m_userIndexSortedIndexToVertex;
	private int m_userIndexSortedAngleIndexToVertex;
	private int m_nextVertexToProcess;
	private int m_firstCoincidentVertex;
	private int m_knownSimpleResult;
	private boolean m_bWinding;
	private boolean m_fixSelfTangency;
	private ProgressTracker m_progressTracker;

	private void _beforeRemoveVertex(int vertex, boolean bChangePathFirst) {
		int vertexlistIndex = m_shape.getUserIndex(vertex,
				m_userIndexSortedIndexToVertex);

		if (m_nextVertexToProcess == vertexlistIndex) {
			m_nextVertexToProcess = m_sortedVertices
					.getNext(m_nextVertexToProcess);
		}

		if (m_firstCoincidentVertex == vertexlistIndex)
			m_firstCoincidentVertex = m_sortedVertices
					.getNext(m_firstCoincidentVertex);

		m_sortedVertices.deleteElement(m_sortedVerticesListIndex,
				vertexlistIndex);
		_removeAngleSortInfo(vertex);
		if (bChangePathFirst) {
			int path = m_shape.getPathFromVertex(vertex);
			if (path != -1) {
				int first = m_shape.getFirstVertex(path);
				if (first == vertex) {
					int next = m_shape.getNextVertex(vertex);
					if (next != vertex) {
						int p = m_shape.getPathFromVertex(next);
						if (p == path) {
							m_shape.setFirstVertex_(path, next);
							return;
						}
						else {
							int prev = m_shape.getPrevVertex(vertex);
							if (prev != vertex) {
								p = m_shape.getPathFromVertex(prev);
								if (p == path) {
									m_shape.setFirstVertex_(path, prev);
									return;
								}
							}
						}
					}

					m_shape.setFirstVertex_(path, -1);
					m_shape.setLastVertex_(path, -1);
				}
			}
		}
	}

	static class SimplificatorAngleComparer extends
			AttributeStreamOfInt32.IntComparator {
		Simplificator m_parent;

		public SimplificatorAngleComparer(Simplificator parent) {
			m_parent = parent;
		}

		@Override
		public int compare(int v1, int v2) {
			return m_parent._compareAngles(v1, v2);
		}

	}

	private boolean _processBunch() {
		boolean bModified = false;
		int iter = 0;
		Point2D ptCenter = new Point2D();
		while (true) {
			m_dbgCounter++;// only for debugging
			iter++;
			// _ASSERT(iter < 10);
			if (m_bunchEdgeEndPoints == null) {
				m_bunchEdgeEndPoints = new AttributeStreamOfInt32(0);
				m_bunchEdgeCenterPoints = new AttributeStreamOfInt32(0);
				m_bunchEdgeIndices = new AttributeStreamOfInt32(0);
			} else {
				m_bunchEdgeEndPoints.clear(false);
				m_bunchEdgeCenterPoints.clear(false);
				m_bunchEdgeIndices.clear(false);
			}

			int currentVertex = m_firstCoincidentVertex;
			int index = 0;
			boolean bFirst = true;
			while (currentVertex != m_nextVertexToProcess) {
				int v = m_sortedVertices.getData(currentVertex);
				{// debug
					Point2D pt = new Point2D();
					m_shape.getXY(v, pt);
					double y = pt.x;
				}
				if (bFirst) {
					m_shape.getXY(v, ptCenter);
					bFirst = false;
				}
				int vertP = m_shape.getPrevVertex(v);
				int vertN = m_shape.getNextVertex(v);
				// _ASSERT(vertP != vertN || m_shape.getPrevVertex(vertN) == v
				// && m_shape.getNextVertex(vertP) == v);

				int id = m_shape.getUserIndex(vertP,
						m_userIndexSortedAngleIndexToVertex);
				if (id != 0xdeadbeef)// avoid adding a point twice
				{
					// _ASSERT(id == -1);
					m_bunchEdgeEndPoints.add(vertP);
					m_shape.setUserIndex(vertP,
							m_userIndexSortedAngleIndexToVertex, 0xdeadbeef);// mark
																				// that
																				// it
																				// has
																				// been
																				// already
																				// added
					m_bunchEdgeCenterPoints.add(v);
					m_bunchEdgeIndices.add(index++);
				}

				int id2 = m_shape.getUserIndex(vertN,
						m_userIndexSortedAngleIndexToVertex);
				if (id2 != 0xdeadbeef) // avoid adding a point twice
				{
					// _ASSERT(id2 == -1);
					m_bunchEdgeEndPoints.add(vertN);
					m_shape.setUserIndex(vertN,
							m_userIndexSortedAngleIndexToVertex, 0xdeadbeef);// mark
																				// that
																				// it
																				// has
																				// been
																				// already
																				// added
					m_bunchEdgeCenterPoints.add(v);
					m_bunchEdgeIndices.add(index++);
				}

				currentVertex = m_sortedVertices.getNext(currentVertex);
			}

			if (m_bunchEdgeEndPoints.size() < 2)
				break;

			// Sort the bunch edpoints by angle (angle between the axis x and
			// the edge, connecting the endpoint with the bunch center)
			m_bunchEdgeIndices.Sort(0, m_bunchEdgeIndices.size(),
					new SimplificatorAngleComparer(this));
			// SORTDYNAMICARRAYEX(m_bunchEdgeIndices, int, 0,
			// m_bunchEdgeIndices.size(), SimplificatorAngleComparer, this);

			for (int i = 0, n = m_bunchEdgeIndices.size(); i < n; i++) {
				int indexL = m_bunchEdgeIndices.get(i);
				int vertex = m_bunchEdgeEndPoints.get(indexL);
				m_shape.setUserIndex(vertex,
						m_userIndexSortedAngleIndexToVertex, i);// rember the
																// sort by angle
																// order
				{// debug
					Point2D pt = new Point2D();
					m_shape.getXY(vertex, pt);
					double y = pt.x;
				}
			}

			boolean bCrossOverResolved = _processCrossOvers(ptCenter);// see of
																		// there
																		// are
																		// crossing
																		// over
																		// edges.
			for (int i = 0, n = m_bunchEdgeIndices.size(); i < n; i++) {
				int indexL = m_bunchEdgeIndices.get(i);
				if (indexL == -1)
					continue;
				int vertex = m_bunchEdgeEndPoints.get(indexL);
				m_shape.setUserIndex(vertex,
						m_userIndexSortedAngleIndexToVertex, -1);// remove
																	// mapping
			}

			if (bCrossOverResolved) {
				bModified = true;
				continue;
			}

			break;
		}

		return bModified;
	}

	private boolean _processCrossOvers(Point2D ptCenter) {
		boolean bFound = false;

		// Resolve all overlaps
		boolean bContinue = true;
		while (bContinue) {
			// The nearest pairts in the middle of the list
			bContinue = false;
			int index1 = 0;
			if (m_bunchEdgeIndices.get(index1) == -1)
				index1 = _getNextEdgeIndex(index1);

			int index2 = _getNextEdgeIndex(index1);

			for (int i = 0, n = m_bunchEdgeIndices.size(); i < n
					&& index1 != -1 && index2 != -1 && index1 != index2; i++) {
				int edgeindex1 = m_bunchEdgeIndices.get(index1);
				int edgeindex2 = m_bunchEdgeIndices.get(index2);

				int vertexB1 = m_bunchEdgeEndPoints.get(edgeindex1);
				int vertexB2 = m_bunchEdgeEndPoints.get(edgeindex2);
				// _ASSERT(vertexB2 != vertexB1);

				int vertexA1 = m_shape.getNextVertex(vertexB1);
				if (!m_shape.isEqualXY(vertexA1, ptCenter))
					vertexA1 = m_shape.getPrevVertex(vertexB1);
				int vertexA2 = m_shape.getNextVertex(vertexB2);
				if (!m_shape.isEqualXY(vertexA2, ptCenter))
					vertexA2 = m_shape.getPrevVertex(vertexB2);

				// _ASSERT(m_shape.isEqualXY(vertexA1, vertexA2));
				// _ASSERT(m_shape.isEqualXY(vertexA1, ptCenter));

				boolean bDirection1 = _getDirection(vertexA1, vertexB1);
				boolean bDirection2 = _getDirection(vertexA2, vertexB2);
				int vertexC1 = bDirection1 ? m_shape.getPrevVertex(vertexA1)
						: m_shape.getNextVertex(vertexA1);
				int vertexC2 = bDirection2 ? m_shape.getPrevVertex(vertexA2)
						: m_shape.getNextVertex(vertexA2);

				boolean bOverlap = false;
				if (_removeSpike(vertexA1))
					bOverlap = true;
				else if (_removeSpike(vertexA2))
					bOverlap = true;
				else if (_removeSpike(vertexB1))
					bOverlap = true;
				else if (_removeSpike(vertexB2))
					bOverlap = true;
				else if (_removeSpike(vertexC1))
					bOverlap = true;
				else if (_removeSpike(vertexC2))
					bOverlap = true;

				if (!bOverlap && m_shape.isEqualXY(vertexB1, vertexB2)) {
					bOverlap = true;
					_resolveOverlap(bDirection1, bDirection2, vertexA1,
							vertexB1, vertexA2, vertexB2);
				}

				if (!bOverlap && m_shape.isEqualXY(vertexC1, vertexC2)) {
					bOverlap = true;
					_resolveOverlap(!bDirection1, !bDirection2, vertexA1,
							vertexC1, vertexA2, vertexC2);
				}

				if (bOverlap)
					bFound = true;

				bContinue |= bOverlap;

				index1 = _getNextEdgeIndex(index1);
				index2 = _getNextEdgeIndex(index1);
			}
		}

		if (!bFound) {// resolve all cross overs
			int index1 = 0;
			if (m_bunchEdgeIndices.get(index1) == -1)
				index1 = _getNextEdgeIndex(index1);

			int index2 = _getNextEdgeIndex(index1);

			for (int i = 0, n = m_bunchEdgeIndices.size(); i < n
					&& index1 != -1 && index2 != -1 && index1 != index2; i++) {
				int edgeindex1 = m_bunchEdgeIndices.get(index1);
				int edgeindex2 = m_bunchEdgeIndices.get(index2);

				int vertexB1 = m_bunchEdgeEndPoints.get(edgeindex1);
				int vertexB2 = m_bunchEdgeEndPoints.get(edgeindex2);

				int vertexA1 = m_shape.getNextVertex(vertexB1);
				if (!m_shape.isEqualXY(vertexA1, ptCenter))
					vertexA1 = m_shape.getPrevVertex(vertexB1);
				int vertexA2 = m_shape.getNextVertex(vertexB2);
				if (!m_shape.isEqualXY(vertexA2, ptCenter))
					vertexA2 = m_shape.getPrevVertex(vertexB2);

				// _ASSERT(m_shape.isEqualXY(vertexA1, vertexA2));
				// _ASSERT(m_shape.isEqualXY(vertexA1, ptCenter));

				boolean bDirection1 = _getDirection(vertexA1, vertexB1);
				boolean bDirection2 = _getDirection(vertexA2, vertexB2);
				int vertexC1 = bDirection1 ? m_shape.getPrevVertex(vertexA1)
						: m_shape.getNextVertex(vertexA1);
				int vertexC2 = bDirection2 ? m_shape.getPrevVertex(vertexA2)
						: m_shape.getNextVertex(vertexA2);

				if (_detectAndResolveCrossOver(bDirection1, bDirection2,
						vertexB1, vertexA1, vertexC1, vertexB2, vertexA2,
						vertexC2)) {
					bFound = true;
				}

				index1 = _getNextEdgeIndex(index1);
				index2 = _getNextEdgeIndex(index1);
			}
		}

		return bFound;
	}

	static class SimplificatorVertexComparer extends
			AttributeStreamOfInt32.IntComparator {
		Simplificator m_parent;

		SimplificatorVertexComparer(Simplificator parent) {
			m_parent = parent;
		}

		@Override
		public int compare(int v1, int v2) {
			return m_parent._compareVerticesSimple(v1, v2);
		}

	}

	private boolean _simplify() {
		if (m_shape.getGeometryType(m_geometry) == Polygon.Type.Polygon.value()
				&& m_shape.getFillRule(m_geometry) == Polygon.FillRule.enumFillRuleWinding)

		{
			TopologicalOperations ops = new TopologicalOperations();
			ops.planarSimplifyNoCrackingAndCluster(m_fixSelfTangency,
					m_shape, m_geometry, m_progressTracker);
			assert (m_shape.getFillRule(m_geometry) == Polygon.FillRule.enumFillRuleOddEven);
		}
		boolean bChanged = false;
		boolean bNeedWindingRepeat = true;
		boolean bWinding = false;

		m_userIndexSortedIndexToVertex = -1;
		m_userIndexSortedAngleIndexToVertex = -1;

		int pointCount = m_shape.getPointCount(m_geometry);

		// Sort vertices lexicographically
		// Firstly copy allvertices to an array.
		AttributeStreamOfInt32 verticesSorter = new AttributeStreamOfInt32(0);
		verticesSorter.reserve(pointCount);

		for (int path = m_shape.getFirstPath(m_geometry); path != -1; path = m_shape
				.getNextPath(path)) {
			int vertex = m_shape.getFirstVertex(path);
			for (int index = 0, n = m_shape.getPathSize(path); index < n; index++) {
				verticesSorter.add(vertex);
				vertex = m_shape.getNextVertex(vertex);
			}
		}

		// Sort
		verticesSorter.Sort(0, pointCount,
				new SimplificatorVertexComparer(this));
		// SORTDYNAMICARRAYEX(verticesSorter, int, 0, pointCount,
		// SimplificatorVertexComparer, this);

		// Copy sorted vertices to the m_sortedVertices list. Make a mapping
		// from the edit shape vertices to the sorted vertices.
		m_userIndexSortedIndexToVertex = m_shape.createUserIndex();// this index
																	// is used
																	// to map
																	// from edit
																	// shape
																	// vertex to
																	// the
																	// m_sortedVertices
																	// list
		m_sortedVertices = new IndexMultiDCList();
		m_sortedVerticesListIndex = m_sortedVertices.createList(0);
		for (int i = 0; i < pointCount; i++) {
			int vertex = verticesSorter.get(i);
			{// debug
				Point2D pt = new Point2D();
				m_shape.getXY(vertex, pt);// for debugging
				double y = pt.x;
			}
			int vertexlistIndex = m_sortedVertices.addElement(
					m_sortedVerticesListIndex, vertex);
			m_shape.setUserIndex(vertex, m_userIndexSortedIndexToVertex,
					vertexlistIndex);// remember the sorted list element on the
										// vertex.
			// When we remove a vertex, we also remove associated sorted list
			// element.
		}

		m_userIndexSortedAngleIndexToVertex = m_shape.createUserIndex();// create
																		// additional
																		// list
																		// to
																		// store
																		// angular
																		// sort
																		// mapping.

		m_nextVertexToProcess = -1;

		if (_cleanupSpikes())// cleanup any spikes on the polygon.
			bChanged = true;

		// External iteration loop for the simplificator.
		// ST. I am not sure if it actually needs this loop. TODO: figure this
		// out.
		while (bNeedWindingRepeat) {
			bNeedWindingRepeat = false;

			int max_iter = m_shape.getPointCount(m_geometry) + 10 > 30 ? 1000
					: (m_shape.getPointCount(m_geometry) + 10)
							* (m_shape.getPointCount(m_geometry) + 10);

			// Simplify polygon
			int iRepeatNum = 0;
			boolean bNeedRepeat = false;

			// Internal iteration loop for the simplificator.
			// ST. I am not sure if it actually needs this loop. TODO: figure
			// this out.
			do// while (bNeedRepeat);
			{
				bNeedRepeat = false;

				boolean bVertexRecheck = false;
				m_firstCoincidentVertex = -1;
				int coincidentCount = 0;
				Point2D ptFirst = new Point2D();
				Point2D pt = new Point2D();
				// Main loop of the simplificator. Go through the vertices and
				// for those that have same coordinates,
				for (int vlistindex = m_sortedVertices
						.getFirst(m_sortedVerticesListIndex); vlistindex != IndexMultiDCList
						.nullNode();) {
					int vertex = m_sortedVertices.getData(vlistindex);
					{// debug
						// Point2D pt = new Point2D();
						m_shape.getXY(vertex, pt);
						double d = pt.x;
					}

					if (m_firstCoincidentVertex != -1) {
						// Point2D pt = new Point2D();
						m_shape.getXY(vertex, pt);
						if (ptFirst.isEqual(pt)) {
							coincidentCount++;
						} else {
							ptFirst.setCoords(pt);
							m_nextVertexToProcess = vlistindex;// we remeber the
																// next index in
																// the member
																// variable to
																// allow it to
																// be updated if
																// a vertex is
																// removed
																// inside of the
																// _ProcessBunch.
							if (coincidentCount > 0) {
								boolean result = _processBunch();// process a
																	// bunch of
																	// coinciding
																	// vertices
								if (result) {// something has changed.
												// Note that ProcessBunch may
												// change m_nextVertexToProcess
												// and m_firstCoincidentVertex.
									bNeedRepeat = true;
									if (m_nextVertexToProcess != IndexMultiDCList
											.nullNode()) {
										int v = m_sortedVertices
												.getData(m_nextVertexToProcess);
										m_shape.getXY(v, ptFirst);
									}
								}
							}

							vlistindex = m_nextVertexToProcess;
							m_firstCoincidentVertex = vlistindex;
							coincidentCount = 0;
						}
					} else {
						m_firstCoincidentVertex = vlistindex;
						m_shape.getXY(m_sortedVertices.getData(vlistindex),
								ptFirst);
						coincidentCount = 0;
					}

					if (vlistindex != -1)//vlistindex can be set to -1 after ProcessBunch call above
						vlistindex = m_sortedVertices.getNext(vlistindex);
				}

				m_nextVertexToProcess = -1;

				if (coincidentCount > 0) {
					boolean result = _processBunch();
					if (result)
						bNeedRepeat = true;
				}

				if (iRepeatNum++ > 10) {
					throw GeometryException.GeometryInternalError();
				}

				if (bNeedRepeat)
					_fixOrphanVertices();// fix broken structure of the shape

				if (_cleanupSpikes())
					bNeedRepeat = true;

				bNeedWindingRepeat |= bNeedRepeat && bWinding;

				bChanged |= bNeedRepeat;

			} while (bNeedRepeat);

		}// while (bNeedWindingRepeat)

		// Now process rings. Fix ring orientation and determine rings that need
		// to be deleted.

		m_shape.removeUserIndex(m_userIndexSortedIndexToVertex);
		m_shape.removeUserIndex(m_userIndexSortedAngleIndexToVertex);

		bChanged |= RingOrientationFixer.execute(m_shape, m_geometry,
				m_sortedVertices, m_fixSelfTangency);

		return bChanged;
	}

	private boolean _getDirection(int vert1, int vert2) {
		if (m_shape.getNextVertex(vert2) == vert1) {
			// _ASSERT(m_shape.getPrevVertex(vert1) == vert2);
			return false;
		} else {
			// _ASSERT(m_shape.getPrevVertex(vert2) == vert1);
			// _ASSERT(m_shape.getNextVertex(vert1) == vert2);
			return true;
		}
	}

	private boolean _detectAndResolveCrossOver(boolean bDirection1,
			boolean bDirection2, int vertexB1, int vertexA1, int vertexC1,
			int vertexB2, int vertexA2, int vertexC2) {
		// _ASSERT(!m_shape.isEqualXY(vertexB1, vertexB2));
		// _ASSERT(!m_shape.isEqualXY(vertexC1, vertexC2));

		if (vertexA1 == vertexA2) {
			_removeAngleSortInfo(vertexB1);
			_removeAngleSortInfo(vertexB2);
			return false;
		}

		// _ASSERT(!m_shape.isEqualXY(vertexB1, vertexC2));
		// _ASSERT(!m_shape.isEqualXY(vertexB1, vertexC1));
		// _ASSERT(!m_shape.isEqualXY(vertexB2, vertexC2));
		// _ASSERT(!m_shape.isEqualXY(vertexB2, vertexC1));
		// _ASSERT(!m_shape.isEqualXY(vertexA1, vertexB1));
		// _ASSERT(!m_shape.isEqualXY(vertexA1, vertexC1));
		// _ASSERT(!m_shape.isEqualXY(vertexA2, vertexB2));
		// _ASSERT(!m_shape.isEqualXY(vertexA2, vertexC2));

		// _ASSERT(m_shape.isEqualXY(vertexA1, vertexA2));

		// get indices of the vertices for the angle sort.
		int iB1 = m_shape.getUserIndex(vertexB1,
				m_userIndexSortedAngleIndexToVertex);
		int iC1 = m_shape.getUserIndex(vertexC1,
				m_userIndexSortedAngleIndexToVertex);
		int iB2 = m_shape.getUserIndex(vertexB2,
				m_userIndexSortedAngleIndexToVertex);
		int iC2 = m_shape.getUserIndex(vertexC2,
				m_userIndexSortedAngleIndexToVertex);
		// _ASSERT(iB1 >= 0);
		// _ASSERT(iC1 >= 0);
		// _ASSERT(iB2 >= 0);
		// _ASSERT(iC2 >= 0);
		// Sort the indices to restore the angle-sort order
		int[] ar = new int[8];
		int[] br = new int[4];

		ar[0] = 0;
		br[0] = iB1;
		ar[1] = 0;
		br[1] = iC1;
		ar[2] = 1;
		br[2] = iB2;
		ar[3] = 1;
		br[3] = iC2;
		for (int j = 1; j < 4; j++)// insertion sort
		{
			int key = br[j];
			int data = ar[j];
			int i = j - 1;
			while (i >= 0 && br[i] > key) {
				br[i + 1] = br[i];
				ar[i + 1] = ar[i];
				i--;
			}
			br[i + 1] = key;
			ar[i + 1] = data;
		}

		int detector = 0;
		if (ar[0] != 0)
			detector |= 1;
		if (ar[1] != 0)
			detector |= 2;
		if (ar[2] != 0)
			detector |= 4;
		if (ar[3] != 0)
			detector |= 8;
		if (detector != 5 && detector != 10)// not an overlap
			return false;

		if (bDirection1 == bDirection2) {
			if (bDirection1) {
				m_shape.setNextVertex_(vertexC2, vertexA1); // B1< >B2
				m_shape.setPrevVertex_(vertexA1, vertexC2); // \ /
				m_shape.setNextVertex_(vertexC1, vertexA2); // A1A2
				m_shape.setPrevVertex_(vertexA2, vertexC1); // / \ //
															// C2> <C1
			} else {
				m_shape.setPrevVertex_(vertexC2, vertexA1); // B1> <B2
				m_shape.setNextVertex_(vertexA1, vertexC2); // \ /
				m_shape.setPrevVertex_(vertexC1, vertexA2); // A1A2
				m_shape.setNextVertex_(vertexA2, vertexC1); // / \ //
															// C2< >C1
			}
		} else {
			if (bDirection1) {
				m_shape.setPrevVertex_(vertexA1, vertexB2); // B1< <B2
				m_shape.setNextVertex_(vertexB2, vertexA1); // \ /
				m_shape.setPrevVertex_(vertexA2, vertexC1); // A1A2
				m_shape.setNextVertex_(vertexC1, vertexA2); // / \ //
															// C2< <C1

			} else {
				m_shape.setNextVertex_(vertexA1, vertexB2); // B1> >B2
				m_shape.setPrevVertex_(vertexB2, vertexA1); // \ /
				m_shape.setNextVertex_(vertexA2, vertexC1); // A1A2
				m_shape.setPrevVertex_(vertexC1, vertexA2); // / \ //
															// C2> >C1

			}
		}

		return true;
	}

	private void _resolveOverlap(boolean bDirection1, boolean bDirection2,
			int vertexA1, int vertexB1, int vertexA2, int vertexB2) {
		if (m_bWinding) {
			_resolveOverlapWinding(bDirection1, bDirection2, vertexA1,
					vertexB1, vertexA2, vertexB2);
		} else {
			_resolveOverlapOddEven(bDirection1, bDirection2, vertexA1,
					vertexB1, vertexA2, vertexB2);
		}
	}

	private void _resolveOverlapWinding(boolean bDirection1,
			boolean bDirection2, int vertexA1, int vertexB1, int vertexA2,
			int vertexB2) {
		throw new GeometryException("not implemented.");
	}

	private void _resolveOverlapOddEven(boolean bDirection1,
			boolean bDirection2, int vertexA1, int vertexB1, int vertexA2,
			int vertexB2) {
		if (bDirection1 != bDirection2) {
			if (bDirection1) {
				// _ASSERT(m_shape.getNextVertex(vertexA1) == vertexB1);
				// _ASSERT(m_shape.getNextVertex(vertexB2) == vertexA2);
				m_shape.setNextVertex_(vertexA1, vertexA2); // B1< B2
				m_shape.setPrevVertex_(vertexA2, vertexA1); // | |
				m_shape.setNextVertex_(vertexB2, vertexB1); // | |
				m_shape.setPrevVertex_(vertexB1, vertexB2); // A1 >A2

				_transferVertexData(vertexA2, vertexA1);
				_beforeRemoveVertex(vertexA2, true);
				m_shape.removeVertexInternal_(vertexA2, true);
				_removeAngleSortInfo(vertexA1);
				_transferVertexData(vertexB2, vertexB1);
				_beforeRemoveVertex(vertexB2, true);
				m_shape.removeVertexInternal_(vertexB2, false);
				_removeAngleSortInfo(vertexB1);
			} else {
				m_shape.setNextVertex_(vertexA2, vertexA1); // B1 B2<
				m_shape.setPrevVertex_(vertexA1, vertexA2); // | |
				m_shape.setNextVertex_(vertexB1, vertexB2); // | |
				m_shape.setPrevVertex_(vertexB2, vertexB1); // A1< A2

				_transferVertexData(vertexA2, vertexA1);
				_beforeRemoveVertex(vertexA2, true);
				m_shape.removeVertexInternal_(vertexA2, false);
				_removeAngleSortInfo(vertexA1);
				_transferVertexData(vertexB2, vertexB1);
				_beforeRemoveVertex(vertexB2, true);
				m_shape.removeVertexInternal_(vertexB2, true);
				_removeAngleSortInfo(vertexB1);
			}
		} else// bDirection1 == bDirection2
		{
			if (!bDirection1) {
				// _ASSERT(m_shape.getNextVertex(vertexB1) == vertexA1);
				// _ASSERT(m_shape.getNextVertex(vertexB2) == vertexA2);
			} else {
				// _ASSERT(m_shape.getNextVertex(vertexA1) == vertexB1);
				// _ASSERT(m_shape.getNextVertex(vertexA2) == vertexB2);
			}

			// if (m_shape._RingParentageCheckInternal(vertexA1, vertexA2))
			{
				int a1 = bDirection1 ? vertexA1 : vertexB1;
				int a2 = bDirection2 ? vertexA2 : vertexB2;
				int b1 = bDirection1 ? vertexB1 : vertexA1;
				int b2 = bDirection2 ? vertexB2 : vertexA2;

				// m_shape.dbgVerifyIntegrity(a1);//debug
				// m_shape.dbgVerifyIntegrity(a2);//debug

				boolean bVisitedA1 = false;
				m_shape.setNextVertex_(a1, a2);
				m_shape.setNextVertex_(a2, a1);
				m_shape.setPrevVertex_(b1, b2);
				m_shape.setPrevVertex_(b2, b1);
				int v = b2;
				while (v != a2)
				{
					int prev = m_shape.getPrevVertex(v);
					int next = m_shape.getNextVertex(v);

					m_shape.setPrevVertex_(v, next);
					m_shape.setNextVertex_(v, prev);
					bVisitedA1 |= v == a1;
					v = next;
				}

				if (!bVisitedA1) {
					// a case of two rings being merged
					int prev = m_shape.getPrevVertex(a2);
					int next = m_shape.getNextVertex(a2);
					m_shape.setPrevVertex_(a2, next);
					m_shape.setNextVertex_(a2, prev);
				} else {
					// merge happend on the same ring.
				}

				// m_shape.dbgVerifyIntegrity(b1);//debug
				// m_shape.dbgVerifyIntegrity(a1);//debug

				_transferVertexData(a2, a1);
				_beforeRemoveVertex(a2, true);
				m_shape.removeVertexInternal_(a2, false);
				_removeAngleSortInfo(a1);
				_transferVertexData(b2, b1);
				_beforeRemoveVertex(b2, true);
				m_shape.removeVertexInternal_(b2, false);
				_removeAngleSortInfo(b1);

				// m_shape.dbgVerifyIntegrity(b1);//debug
				// m_shape.dbgVerifyIntegrity(a1);//debug
			}
		}
	}

	private boolean _cleanupSpikes() {
		boolean bModified = false;
		for (int path = m_shape.getFirstPath(m_geometry); path != -1;) {
			int vertex = m_shape.getFirstVertex(path);
			for (int vindex = 0, n = m_shape.getPathSize(path); vindex < n
					&& n > 1;) {
				int prev = m_shape.getPrevVertex(vertex);
				int next = m_shape.getNextVertex(vertex);
				if (m_shape.isEqualXY(prev, next)) {
					bModified = true;
					_beforeRemoveVertex(vertex, false);
					m_shape.removeVertex(vertex, true);// not internal, because
														// path is valid at this
														// point
					_beforeRemoveVertex(next, false);
					m_shape.removeVertex(next, true);
					vertex = prev;
					vindex = 0;
					n = m_shape.getPathSize(path);
				} else {
					vertex = next;
					vindex++;
				}
			}

			if (m_shape.getPathSize(path) < 2) {
				int vertexL = m_shape.getFirstVertex(path);
				for (int vindex = 0, n = m_shape.getPathSize(path); vindex < n; vindex++) {
					_beforeRemoveVertex(vertexL, false);
					vertexL = m_shape.getNextVertex(vertexL);
				}

				path = m_shape.removePath(path);
				bModified = true;
			} else
				path = m_shape.getNextPath(path);
		}

		return bModified;
	}

	private boolean _removeSpike(int vertexIn) {
		// m_shape.dbgVerifyIntegrity(vertex);//debug
		int vertex = vertexIn;

		// _ASSERT(m_shape.isEqualXY(m_shape.getNextVertex(vertex),
		// m_shape.getPrevVertex(vertex)));
		boolean bFound = false;
		while (true) {
			int next = m_shape.getNextVertex(vertex);
			int prev = m_shape.getPrevVertex(vertex);
			if (next == vertex) {// last vertex in a ring
				_beforeRemoveVertex(vertex, true);
				m_shape.removeVertexInternal_(vertex, false);
				return true;
			}

			if (!m_shape.isEqualXY(next, prev))
				break;

			bFound = true;
			_removeAngleSortInfo(prev);
			_removeAngleSortInfo(next);
			_beforeRemoveVertex(vertex, true);
			m_shape.removeVertexInternal_(vertex, false);
			// m_shape.dbgVerifyIntegrity(prev);//debug
			_transferVertexData(next, prev);
			_beforeRemoveVertex(next, true);
			m_shape.removeVertexInternal_(next, true);
			if (next == prev)
				break;// deleted the last vertex

			// m_shape.dbgVerifyIntegrity(prev);//debug

			vertex = prev;
		}
		return bFound;
	}

	private void _fixOrphanVertices() {
		int pathCount = 0;
		// clean any path info
		for (int node = m_sortedVertices.getFirst(m_sortedVertices
				.getFirstList()); node != -1; node = m_sortedVertices
				.getNext(node)) {
			int vertex = m_sortedVertices.getData(node);
			m_shape.setPathToVertex_(vertex, -1);
		}
		int geometrySize = 0;
		for (int path = m_shape.getFirstPath(m_geometry); path != -1;) {
			int first = m_shape.getFirstVertex(path);
			if (first == -1 || m_shape.getPathFromVertex(first) != -1) {
				int p = path;
				path = m_shape.getNextPath(path);
				m_shape.removePathOnly_(p);
				continue;
			}

			m_shape.setPathToVertex_(first, path);
			int pathSize = 1;
			for (int vertex = m_shape.getNextVertex(first); vertex != first; vertex = m_shape
					.getNextVertex(vertex)) {
				m_shape.setPathToVertex_(vertex, path);
				pathSize++;
			}
			m_shape.setRingAreaValid_(path,false);
			m_shape.setPathSize_(path, pathSize);
			m_shape.setLastVertex_(path, m_shape.getPrevVertex(first));
			geometrySize += pathSize;
			pathCount++;
			path = m_shape.getNextPath(path);
		}

		// Some vertices do not belong to any path. We have to create new path
		// objects for those.
		// Produce new paths for the orphan vertices.
		for (int node = m_sortedVertices.getFirst(m_sortedVertices
				.getFirstList()); node != -1; node = m_sortedVertices
				.getNext(node)) {
			int vertex = m_sortedVertices.getData(node);
			if (m_shape.getPathFromVertex(vertex) != -1)
				continue;
			
			int path = m_shape.insertClosedPath_(m_geometry, -1, vertex, vertex, null);
			geometrySize += m_shape.getPathSize(path);
			pathCount++;
		}
		
		m_shape.setGeometryPathCount_(m_geometry, pathCount);
		m_shape.setGeometryVertexCount_(m_geometry, geometrySize);
		int totalPointCount = 0;
		for (int geometry = m_shape.getFirstGeometry(); geometry != -1; geometry = m_shape.getNextGeometry(geometry)) {
			totalPointCount += m_shape.getPointCount(geometry);
		}
		
		m_shape.setTotalPointCount_(totalPointCount);
	}

	private int _getNextEdgeIndex(int indexIn) {
		int index = indexIn;
		for (int i = 0, n = m_bunchEdgeIndices.size() - 1; i < n; i++) {
			index = (index + 1) % m_bunchEdgeIndices.size();
			if (m_bunchEdgeIndices.get(index) != -1)
				return index;
		}
		return -1;
	}

	private void _transferVertexData(int vertexFrom, int vertexTo) {
		int v1 = m_shape.getUserIndex(vertexTo, m_userIndexSortedIndexToVertex);
		int v2 = m_shape.getUserIndex(vertexTo,
				m_userIndexSortedAngleIndexToVertex);
		m_shape.transferAllDataToTheVertex(vertexFrom, vertexTo);
		m_shape.setUserIndex(vertexTo, m_userIndexSortedIndexToVertex, v1);
		m_shape.setUserIndex(vertexTo, m_userIndexSortedAngleIndexToVertex, v2);
	}

	private void _removeAngleSortInfo(int vertex) {
		int angleIndex = m_shape.getUserIndex(vertex,
				m_userIndexSortedAngleIndexToVertex);
		if (angleIndex != -1) {
			m_bunchEdgeIndices.set(angleIndex, -1);
			m_shape.setUserIndex(vertex, m_userIndexSortedAngleIndexToVertex,
					-1);
		}
	}

	protected Simplificator() {
		m_dbgCounter = 0;
	}

	public static boolean execute(EditShape shape, int geometry,
			int knownSimpleResult, boolean fixSelfTangency, ProgressTracker progressTracker) {
		Simplificator simplificator = new Simplificator();
		simplificator.m_shape = shape;
		// simplificator.m_bWinding = bWinding;
		simplificator.m_geometry = geometry;
		simplificator.m_knownSimpleResult = knownSimpleResult;
		simplificator.m_fixSelfTangency = fixSelfTangency;
		simplificator.m_progressTracker = progressTracker;
		return simplificator._simplify();
	}

	int _compareVerticesSimple(int v1, int v2) {
		Point2D pt1 = new Point2D();
		m_shape.getXY(v1, pt1);
		Point2D pt2 = new Point2D();
		m_shape.getXY(v2, pt2);
		int res = pt1.compare(pt2);
		if (res == 0) {// sort equal vertices by the path ID
			int i1 = m_shape.getPathFromVertex(v1);
			int i2 = m_shape.getPathFromVertex(v2);
			res = i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
		}

		return res;
	}

	int _compareAngles(int index1, int index2) {
		int vert1 = m_bunchEdgeEndPoints.get(index1);
		Point2D pt1 = new Point2D();
		m_shape.getXY(vert1, pt1);
		Point2D pt2 = new Point2D();
		int vert2 = m_bunchEdgeEndPoints.get(index2);
		m_shape.getXY(vert2, pt2);

		if (pt1.isEqual(pt2))
			return 0;// overlap case

		int vert10 = m_bunchEdgeCenterPoints.get(index1);
		Point2D pt10 = new Point2D();
		m_shape.getXY(vert10, pt10);

		int vert20 = m_bunchEdgeCenterPoints.get(index2);
		Point2D pt20 = new Point2D();
		m_shape.getXY(vert20, pt20);
		// _ASSERT(pt10.isEqual(pt20));

		Point2D v1 = new Point2D();
		v1.sub(pt1, pt10);
		Point2D v2 = new Point2D();
		v2.sub(pt2, pt20);
		int result = Point2D._compareVectors(v1, v2);
		return result;
	}
}
