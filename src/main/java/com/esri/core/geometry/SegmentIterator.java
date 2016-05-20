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
 * This class provides functionality to iterate over MultiPath segments.
 * 
 * Example:
 * <pre><code>
 * SegmentIterator iterator = polygon.querySegmentIterator();
 * while (iterator.nextPath()) {
 *   while (iterator.hasNextSegment()) {
 *     Segment segment = iterator.nextSegment();
 *   }
 * }
 * </code></pre>
 */
public class SegmentIterator {
	private SegmentIteratorImpl m_impl;

	SegmentIterator(Object obj) {
		m_impl = (SegmentIteratorImpl) obj;
	}

	/**
	 * Moves the iterator to the next path. Returns the TRUE if successful.
	 * 
	 * @return TRUE if the next path exists.
	 */
	public boolean nextPath() {
		return m_impl.nextPath();
	}

	/**
	 * Moves the iterator to the previous path. Returns the TRUE if successful.
	 * 
	 * @return TRUE if the previous path exists.
	 */
	public boolean previousPath() {
		return m_impl.previousPath();
	}

	/**
	 * Resets the iterator such that a subsequent call to NextPath will set the
	 * iterator to the first path.
	 */
	public void resetToFirstPath() {
		m_impl.resetToFirstPath();
	}

	/**
	 * Resets the iterator such that a subsequent call to PreviousPath will set
	 * the iterator to the last path.
	 */
	public void resetToLastPath() {
		m_impl.resetToLastPath();
	}

	/**
	 * Resets the iterator such that a subsequent call to NextPath will set the
	 * iterator to the given path index. A call to PreviousPath will set the
	 * iterator to the path at pathIndex - 1.
	 */
	public void resetToPath(int pathIndex) {
		m_impl.resetToPath(pathIndex);
	}

	/**
	 * Indicates whether the iterator points to the first segment in the current
	 * path.
	 * 
	 * @return TRUE if the iterator points to the first segment in the current
	 *         path.
	 */
	public boolean isFirstSegmentInPath() {
		return m_impl.isFirstSegmentInPath();
	}

	/**
	 * Indicates whether the iterator points to the last segment in the current
	 * path.
	 * 
	 * @return TRUE if the iterator points to the last segment in the current
	 *         path.
	 */
	public boolean isLastSegmentInPath() {
		return m_impl.isLastSegmentInPath();
	}

	/**
	 * Resets the iterator so that the call to NextSegment will return the first
	 * segment of the current path.
	 */
	public void resetToFirstSegment() {
		m_impl.resetToFirstSegment();
	}

	/**
	 * Resets the iterator so that the call to PreviousSegment will return the
	 * last segment of the current path.
	 */
	public void resetToLastSegment() {
		m_impl.resetToLastSegment();
	}

	/**
	 *Resets the iterator to a specific vertex.
	 *The call to next_segment will return the segment that starts at the vertex.
	 *Call to previous_segment will return the segment that starts at the previous vertex.
	 *@param vertexIndex The vertex index to reset the iterator to.
	 *@param pathIndex The path index to reset the iterator to. Used as a hint. If the path_index is wrong or -1, then the path_index is automatically calculated.
	 *
	 */
	public void resetToVertex(int vertexIndex, int pathIndex) {
		m_impl.resetToVertex(vertexIndex, pathIndex);
	}

	/**
	 * Indicates whether a next segment exists for the path.
	 * 
	 * @return TRUE is the next segment exists.
	 */
	public boolean hasNextSegment() {
		return m_impl.hasNextSegment();
	}

	/**
	 * Indicates whether a previous segment exists in the path.
	 * 
	 * @return TRUE if the previous segment exists.
	 */
	public boolean hasPreviousSegment() {
		return m_impl.hasPreviousSegment();
	}

	/**
	 * Moves the iterator to the next segment and returns the segment.
	 * 
	 * The Segment is returned by value and is owned by the iterator.
	 */
	public Segment nextSegment() {
		return m_impl.nextSegment();
	}

	/**
	 * Moves the iterator to previous segment and returns the segment.
	 * 
	 * The Segment is returned by value and is owned by the iterator.
	 */
	public Segment previousSegment() {
		return m_impl.previousSegment();
	}

	/**
	 * Returns the index of the current path.
	 */
	public int getPathIndex() {
		return m_impl.getPathIndex();
	}

	/**
	 * Returns the index of the start point of this segment.
	 */
	public int getStartPointIndex() {
		return m_impl.getStartPointIndex();
	}

	/**
	 * Returns the index of the end point of the current segment.
	 */
	public int getEndPointIndex() {
		return m_impl.getEndPointIndex();
	}

	/**
	 * Returns TRUE, if the segment is the closing segment of the closed path
	 */
	public boolean isClosingSegment() {
		return m_impl.isClosingSegment();
	}

	/**
	 * Switches the iterator to navigation mode.
	 * 
	 * @param bYesNo
	 *            If TRUE, the iterator loops over the current path infinitely
	 *            (unless the multipath is empty).
	 */
	public void setCirculator(boolean bYesNo) {
		m_impl.setCirculator(bYesNo);
	}

	/**
	 * Copies this SegmentIterator.
	 * 
	 * @return SegmentIterator.
	 */
	public Object copy() {
		return new SegmentIterator(m_impl.copy());
	}

	protected Object _getImpl() {
		return (SegmentIteratorImpl) m_impl;
	}
}
