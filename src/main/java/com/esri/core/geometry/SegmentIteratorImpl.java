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

/**
 * Provides functionality to iterate over MultiPath segments.
 */
final class SegmentIteratorImpl {

	protected Line m_line;

	// Bezier m_bezier:
	// Arc m_arc;
	protected Segment m_currentSegment;
	protected Point2D m_dummyPoint;

	protected int m_currentPathIndex;

	protected int m_nextPathIndex;

	protected int m_prevPathIndex;

	protected int m_currentSegmentIndex;

	protected int m_nextSegmentIndex;

	protected int m_prevSegmentIndex;

	protected int m_segmentCount;

	protected int m_pathBegin;

	protected MultiPathImpl m_parent; // parent of the iterator.

	protected boolean m_bCirculator; // If true, the iterator circulates around
										// the current Path.

	protected boolean m_bNeedsUpdate;

	public SegmentIteratorImpl(MultiPathImpl parent) {
		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = 0;
		m_nextPathIndex = 0;
		m_currentPathIndex = -1;
		m_parent = parent;
		m_segmentCount = _getSegmentCount(m_nextPathIndex);
		m_bCirculator = false;
		m_currentSegment = null;
		m_pathBegin = -1;
		m_dummyPoint = new Point2D();
	}

	public SegmentIteratorImpl(MultiPathImpl parent, int pointIndex) {
		if (pointIndex < 0 || pointIndex >= parent.getPointCount())
			throw new IndexOutOfBoundsException();

		m_currentSegmentIndex = -1;
		int path = parent.getPathIndexFromPointIndex(pointIndex);
		m_nextSegmentIndex = pointIndex - parent.getPathStart(path);

		m_nextPathIndex = path + 1;
		m_currentPathIndex = path;
		m_parent = parent;
		m_segmentCount = _getSegmentCount(m_currentPathIndex);
		m_bCirculator = false;
		m_currentSegment = null;
		m_pathBegin = m_parent.getPathStart(m_currentPathIndex);
		m_dummyPoint = new Point2D();
	}

	public SegmentIteratorImpl(MultiPathImpl parent, int pathIndex,
			int segmentIndex) {
		if (pathIndex < 0 || pathIndex >= parent.getPathCount()
				|| segmentIndex < 0)
			throw new IndexOutOfBoundsException();

		int d = parent.isClosedPath(pathIndex) ? 0 : 1;
		if (segmentIndex >= parent.getPathSize(pathIndex) - d)
			throw new IndexOutOfBoundsException();

		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = segmentIndex;
		m_currentPathIndex = pathIndex;
		m_nextPathIndex = m_nextSegmentIndex + 1;
		m_parent = parent;
		m_segmentCount = _getSegmentCount(m_nextPathIndex);
		m_bCirculator = false;
		m_currentSegment = null;
		m_pathBegin = m_parent.getPathStart(m_currentPathIndex);
		m_dummyPoint = new Point2D();
	}

	void resetTo(SegmentIteratorImpl src) {
		if (m_parent != src.m_parent)
			throw new GeometryException("invalid_call");

		m_currentSegmentIndex = src.m_currentSegmentIndex;
		m_nextSegmentIndex = src.m_nextSegmentIndex;
		m_currentPathIndex = src.m_currentPathIndex;
		m_nextPathIndex = src.m_nextPathIndex;
		m_segmentCount = src.m_segmentCount;
		m_bCirculator = src.m_bCirculator;
		m_pathBegin = src.m_pathBegin;
		m_currentSegment = null;
	}

	/**
	 * Moves the iterator to the next curve segment and returns the segment.
	 * 
	 * The Segment is returned by value and is owned by the iterator. Note: The
	 * method can return null if there are no curves in the part.
	 */
	public Segment nextCurve() {
		return null;
		// TODO: Fix me. This method is supposed to go only through the curves
		// and skip the Line classes!!
		// It must be very efficient.
	}

	/**
	 * Moves the iterator to next segment and returns the segment.
	 * 
	 * The Segment is returned by value and is owned by the iterator.
	 */
	public Segment nextSegment() {
		if (m_currentSegmentIndex != m_nextSegmentIndex)
			_updateSegment();

		if (m_bCirculator) {
			m_nextSegmentIndex = (m_nextSegmentIndex + 1) % m_segmentCount;
		} else {
			if (m_nextSegmentIndex == m_segmentCount)
				throw new IndexOutOfBoundsException();

			m_nextSegmentIndex++;
		}

		return m_currentSegment;
	}

	/**
	 * Moves the iterator to previous segment and returns the segment.
	 * 
	 * The Segment is returned by value and is owned by the iterator.
	 */
	public Segment previousSegment() {
		if (m_bCirculator) {
			m_nextSegmentIndex = (m_segmentCount + m_nextSegmentIndex - 1)
					% m_segmentCount;
		} else {
			if (m_nextSegmentIndex == 0)
				throw new IndexOutOfBoundsException();
			m_nextSegmentIndex--;
		}

		if (m_nextSegmentIndex != m_currentSegmentIndex)
			_updateSegment();

		return m_currentSegment;
	}

	/**
	 * Resets the iterator so that the call to NextSegment will return the first
	 * segment of the current path.
	 */
	public void resetToFirstSegment() {
		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = 0;
	}

	/**
	 * Resets the iterator so that the call to PreviousSegment will return the
	 * last segment of the current path.
	 */
	public void resetToLastSegment() {
		m_nextSegmentIndex = m_segmentCount;
		m_currentSegmentIndex = -1;
	}

	public void resetToVertex(int vertexIndex) {
		resetToVertex(vertexIndex, -1);
	}

	public void resetToVertex(int vertexIndex, int _pathIndex) {
		if (m_currentPathIndex >= 0
				&& m_currentPathIndex < m_parent.getPathCount()) {// check if we
																	// are in
																	// the
																	// current
																	// path
			int start = _getPathBegin();
			if (vertexIndex >= start
					&& vertexIndex < m_parent.getPathEnd(m_currentPathIndex)) {
				m_currentSegmentIndex = -1;
				m_nextSegmentIndex = vertexIndex - start;
				return;
			}
		}

		int path_index;
		if (_pathIndex >= 0 && _pathIndex < m_parent.getPathCount()
				&& vertexIndex >= m_parent.getPathStart(_pathIndex)
				&& vertexIndex < m_parent.getPathEnd(_pathIndex)) {
			path_index = _pathIndex;
		} else {
			path_index = m_parent.getPathIndexFromPointIndex(vertexIndex);
		}

		m_nextPathIndex = path_index + 1;
		m_currentPathIndex = path_index;
		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = vertexIndex - m_parent.getPathStart(path_index);
		m_segmentCount = _getSegmentCount(path_index);
		m_pathBegin = m_parent.getPathStart(m_currentPathIndex);
	}

	/**
	 * Moves the iterator to next path and returns true if successful.
	 * 
	 */
	public boolean nextPath() {
		// post-increment
		m_currentPathIndex = m_nextPathIndex;
		if (m_currentPathIndex >= m_parent.getPathCount())
			return false;

		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = 0;
		m_segmentCount = _getSegmentCount(m_currentPathIndex);
		m_pathBegin = m_parent.getPathStart(m_currentPathIndex);
		m_nextPathIndex++;
		return true;
	}

	/**
	 * Moves the iterator to next path and returns true if successful.
	 * 
	 */
	public boolean previousPath() {
		// pre-decrement
		if (m_nextPathIndex == 0)
			return false;

		m_nextPathIndex--;
		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = 0;
		m_segmentCount = _getSegmentCount(m_nextPathIndex);
		m_currentPathIndex = m_nextPathIndex;
		m_pathBegin = m_parent.getPathStart(m_currentPathIndex);
		resetToLastSegment();
		return true;
	}

	/**
	 * Resets the iterator such that the subsequent call to the NextPath will
	 * set the iterator to the first segment of the first path.
	 */
	public void resetToFirstPath() {

		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = -1;
		m_segmentCount = -1;
		m_nextPathIndex = 0;
		m_currentPathIndex = -1;
		m_pathBegin = -1;
	}

	/**
	 * Resets the iterator such that the subsequent call to the PreviousPath
	 * will set the iterator to the last segment of the last path.
	 */
	public void resetToLastPath() {
		m_nextPathIndex = m_parent.getPathCount();
		m_currentPathIndex = -1;
		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = -1;
		m_segmentCount = -1;
		m_pathBegin = -1;
	}

	/**
	 * Resets the iterator such that the subsequent call to the NextPath will
	 * set the iterator to the first segment of the given path. The call to
	 * PreviousPath will reset the iterator to the last segment of path
	 * pathIndex - 1.
	 */
	public void resetToPath(int pathIndex) {
		if (pathIndex < 0)
			throw new IndexOutOfBoundsException();

		m_nextPathIndex = pathIndex;
		m_currentPathIndex = -1;
		m_currentSegmentIndex = -1;
		m_nextSegmentIndex = -1;
		m_segmentCount = -1;
		m_pathBegin = -1;
	}

	public int _getSegmentCount(int pathIndex) {
		if (m_parent.isEmptyImpl())
			return 0;

		int d = 1;
		if (m_parent.isClosedPath(pathIndex))
			d = 0;

		return m_parent.getPathSize(pathIndex) - d;
	}

	/**
	 * Returns True, if the segment is the closing segment of the closed path
	 */
	public boolean isClosingSegment() {
		return m_currentSegmentIndex == m_segmentCount - 1
				&& m_parent.isClosedPath(m_currentPathIndex);
	}

	/**
	 * Switches the iterator navigation mode.
	 * 
	 * @param bYesNo
	 *            If True, the iterator loops over the current path infinitely
	 *            (unless the MultiPath is empty).
	 */
	public void setCirculator(boolean bYesNo) {
		m_bCirculator = bYesNo;
	}

	/**
	 * Returns the index of the current path.
	 */
	public int getPathIndex() {
		return m_currentPathIndex;
	}

	/**
	 * Returns the index of the start Point of the current Segment.
	 */
	public int getStartPointIndex() {
		return _getPathBegin() + m_currentSegmentIndex;
	}

	public int _getPathBegin() {
		return m_parent.getPathStart(m_currentPathIndex);
	}

	/**
	 * Returns the index of the end Point of the current Segment.
	 */
	public int getEndPointIndex() {
		if (isClosingSegment()) {
			return m_parent.getPathStart(m_currentPathIndex);
		} else {
			return getStartPointIndex() + 1;
		}
	}

	/**
	 * Returns True if the segment is first one in the current Path.
	 */
	public boolean isFirstSegmentInPath() {
		return m_currentSegmentIndex == 0;
	}

	/**
	 * Returns True if the segment is last one in the current Path.
	 */
	public boolean isLastSegmentInPath() {
		return m_currentSegmentIndex == m_segmentCount - 1;
	}

	/**
	 * Returns True if the call to the NextSegment will succeed.
	 */
	public boolean hasNextSegment() {
		return m_nextSegmentIndex < m_segmentCount;
	}

	/**
	 * Returns True if the call to the NextSegment will succeed.
	 */
	public boolean hasPreviousSegment() {
		return m_nextSegmentIndex > 0;
	}

	public SegmentIteratorImpl copy() {
		SegmentIteratorImpl clone = new SegmentIteratorImpl(m_parent);
		clone.m_currentSegmentIndex = m_currentSegmentIndex;
		clone.m_nextSegmentIndex = m_nextSegmentIndex;
		clone.m_segmentCount = m_segmentCount;
		clone.m_currentPathIndex = m_currentPathIndex;
		clone.m_nextPathIndex = m_nextPathIndex;
		clone.m_parent = m_parent;
		clone.m_bCirculator = m_bCirculator;
		return clone;
	}

	public void _updateSegment() {
		if (m_nextSegmentIndex < 0 || m_nextSegmentIndex >= m_segmentCount)
			throw new IndexOutOfBoundsException();
		m_currentSegmentIndex = m_nextSegmentIndex;

		int startVertexIndex = getStartPointIndex();
		m_parent._verifyAllStreams();
		AttributeStreamOfInt8 segFlagStream = m_parent
				.getSegmentFlagsStreamRef();

		int segFlag = SegmentFlags.enumLineSeg;
		if (segFlagStream != null)
			segFlag = (segFlagStream.read(startVertexIndex) & SegmentFlags.enumSegmentMask);

		VertexDescription vertexDescr = m_parent.getDescription();
		switch (segFlag) {
		case SegmentFlags.enumLineSeg:
			if (m_line == null)
				m_line = new Line();
			m_currentSegment = (Line) m_line;
			break;
		case SegmentFlags.enumBezierSeg:
			throw GeometryException.GeometryInternalError();
			// break;
		case SegmentFlags.enumArcSeg:
			throw GeometryException.GeometryInternalError();
			// break;
		default:
			throw GeometryException.GeometryInternalError();
		}

		m_currentSegment.assignVertexDescription(vertexDescr);

		int endVertexIndex = getEndPointIndex();
		m_parent.getXY(startVertexIndex, m_dummyPoint);
		m_currentSegment.setStartXY(m_dummyPoint);
		m_parent.getXY(endVertexIndex, m_dummyPoint);
		m_currentSegment.setEndXY(m_dummyPoint);

		for (int i = 1, nattr = vertexDescr.getAttributeCount(); i < nattr; i++) {
			int semantics = vertexDescr.getSemantics(i);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int ord = 0; ord < ncomp; ord++) {
				double vs = m_parent.getAttributeAsDbl(semantics,
						startVertexIndex, ord);
				m_currentSegment.setStartAttribute(semantics, ord, vs);
				double ve = m_parent.getAttributeAsDbl(semantics,
						endVertexIndex, ord);
				m_currentSegment.setEndAttribute(semantics, ord, ve);
			}
		}
	}

	boolean isLastPath() {
		return m_currentPathIndex == m_parent.getPathCount() - 1;
	}

}
