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

import java.io.Serializable;

/**
 * The MulitPath class is a base class for polygons and polylines.
 */
public abstract class MultiPath extends MultiVertexGeometry implements
		Serializable {
	MultiPathImpl m_impl;

	@Override
	public VertexDescription getDescription() {
		return m_impl.getDescription();
	}

	@Override
	public void assignVertexDescription(VertexDescription src) {
		m_impl.assignVertexDescription(src);
	}

	@Override
	public void mergeVertexDescription(VertexDescription src) {
		m_impl.mergeVertexDescription(src);
	}

	@Override
	public void addAttribute(int semantics) {
		m_impl.addAttribute(semantics);
	}

	@Override
	public void dropAttribute(int semantics) {
		m_impl.dropAttribute(semantics);
	}

	@Override
	public void dropAllAttributes() {
		m_impl.dropAllAttributes();
	}

	@Override
	public int getPointCount() {
		return m_impl.getPointCount();
	}

	@Override
	public Point getPoint(int index) {
		return m_impl.getPoint(index);
	}

	@Override
	public void setPoint(int index, Point point) {
		m_impl.setPoint(index, point);
	}

	@Override
	public boolean isEmpty() {
		return m_impl.isEmptyImpl();
	}

	@Override
	public double calculateArea2D() {
		return m_impl.calculateArea2D();
	}

	@Override
	public double calculateLength2D() {
		return m_impl.calculateLength2D();
	}

	public double calculatePathLength2D(int pathIndex) {
		return m_impl.calculatePathLength2D(pathIndex);
	}

	@Override
	public double getAttributeAsDbl(int semantics, int index, int ordinate) {
		return m_impl.getAttributeAsDbl(semantics, index, ordinate);
	}

	@Override
	public int getAttributeAsInt(int semantics, int index, int ordinate) {
		return m_impl.getAttributeAsInt(semantics, index, ordinate);
	}

	@Override
	public void setAttribute(int semantics, int index, int ordinate,
			double value) {
		m_impl.setAttribute(semantics, index, ordinate, value);
	}

	@Override
	public void setAttribute(int semantics, int index, int ordinate, int value) {
		m_impl.setAttribute(semantics, index, ordinate, value);
	}

	@Override
	public Point2D getXY(int index) {
		return m_impl.getXY(index);
	}

	@Override
	public void getXY(int index, Point2D pt) {
		m_impl.getXY(index, pt);
	}

	@Override
	public void setXY(int index, Point2D pt) {
		m_impl.setXY(index, pt);
	}

	@Override
	Point3D getXYZ(int index) {
		return m_impl.getXYZ(index);
	}

	@Override
	void setXYZ(int index, Point3D pt) {
		m_impl.setXYZ(index, pt);
	}

	@Override
	public void queryEnvelope(Envelope env) {
		m_impl.queryEnvelope(env);
	}

	@Override
	public void queryEnvelope2D(Envelope2D env) {
		m_impl.queryEnvelope2D(env);
	}
	
	public void queryPathEnvelope2D(int pathIndex, Envelope2D env) {
		m_impl.queryPathEnvelope2D(pathIndex, env);
	}

	@Override
	void queryEnvelope3D(Envelope3D env) {
		m_impl.queryEnvelope3D(env);
	}

	public void queryLooseEnvelope(Envelope2D env) {
		m_impl.queryLooseEnvelope2D(env);
	}

	void queryLooseEnvelope(Envelope3D env) {
		m_impl.queryLooseEnvelope3D(env);
	}

	@Override
	public Envelope1D queryInterval(int semantics, int ordinate) {
		return m_impl.queryInterval(semantics, ordinate);
	}

	@Override
	public void copyTo(Geometry dst) {
		if (getType() != dst.getType())
			throw new IllegalArgumentException();

		m_impl.copyTo((Geometry) dst._getImpl());
	}

	@Override
	public Geometry getBoundary() {
		return m_impl.getBoundary();
	}

	@Override
	public void queryCoordinates(Point2D[] dst) {
		m_impl.queryCoordinates(dst);
	}
	
	public void queryCoordinates(Point2D[] dst, int dstSize, int beginIndex, int endIndex) {
	  m_impl.queryCoordinates(dst, dstSize, beginIndex, endIndex);
	}

	@Override
	void queryCoordinates(Point3D[] dst) {
		m_impl.queryCoordinates(dst);
	}

	@Override
	public void queryCoordinates(Point[] dst) {
		m_impl.queryCoordinates(dst);

	}

	/**
	 * Returns TRUE if the multipath contains non-linear segments.
	 */
	boolean hasNonLinearSegments() {
		return m_impl.hasNonLinearSegments();
	}

	/**
	 * Returns total segment count in the MultiPath.
	 */
	public int getSegmentCount() {
		return m_impl.getSegmentCount();
	}

	/**
	 * Returns the segment count in the given multipath path.
	 * 
	 * @param pathIndex
	 *            The path to determine the segment.
	 * @return The segment of the multipath.
	 */
	public int getSegmentCount(int pathIndex) {
		int segCount = getPathSize(pathIndex);
		if (!isClosedPath(pathIndex))
			segCount--;
		return segCount;
	}

	/**
	 * Appends all paths from another multipath.
	 * 
	 * @param src
	 *            The multipath to append to this multipath.
	 * @param bReversePaths
	 *            TRUE if the multipath is added should be added with its paths
	 *            reversed.
	 */
	public void add(MultiPath src, boolean bReversePaths) {
		m_impl.add((MultiPathImpl) src._getImpl(), bReversePaths);
	}

	/**
	 * Copies a path from another multipath.
	 * 
	 * @param src
	 *            The multipath to copy from.
	 * @param srcPathIndex
	 *            The index of the path in the the source MultiPath.
	 * @param bForward
	 *            When FALSE, the points are inserted in reverse order.
	 */
	public void addPath(MultiPath src, int srcPathIndex, boolean bForward) {
		m_impl.addPath((MultiPathImpl) src._getImpl(), srcPathIndex, bForward);
	}

	/**
	 * Adds a new path to this multipath.
	 * 
	 * @param points
	 *            The array of points to add to this multipath.
	 * @param count
	 *            The number of points added to the mulitpath.
	 * @param bForward
	 *            When FALSE, the points are inserted in reverse order.
	 */
	void addPath(Point2D[] points, int count, boolean bForward) {
		m_impl.addPath(points, count, bForward);
	}

	/**
	 * Adds segments from a source multipath to this MultiPath.
	 *
	 * @param src
	 *            The source MultiPath to add segments from.
	 * @param srcPathIndex
	 *            The index of the path in the the source MultiPath.
	 * @param srcSegmentFrom
	 *            The index of first segment in the path to start adding from.
	 *            The value has to be between 0 and
	 *            src.getSegmentCount(srcPathIndex) - 1.
	 * @param srcSegmentCount
	 *            The number of segments to add. If 0, the function does
	 *            nothing.
	 * @param bStartNewPath
	 *            When true, a new path is added and segments are added to it.
	 *            Otherwise the segments are added to the last path of this
	 *            MultiPath.
	 *
	 *            If bStartNewPath false, the first point of the first source
	 *            segment is not added. This is done to ensure proper connection
	 *            to existing segments. When the source path is closed, and the
	 *            closing segment is among those to be added, it is added also
	 *            as a closing segment, not as a real segment. Use add_segment
	 *            instead if you do not like that behavior.
	 *
	 *            This MultiPath obtains all missing attributes from the src
	 *            MultiPath.
	 */
	public void addSegmentsFromPath(MultiPath src, int srcPathIndex,
			int srcSegmentFrom, int srcSegmentCount, boolean bStartNewPath) {
		m_impl.addSegmentsFromPath((MultiPathImpl) src._getImpl(),
				srcPathIndex, srcSegmentFrom, srcSegmentCount, bStartNewPath);
	}

	/**
	 * Adds a new segment to this multipath.
	 * 
	 * @param segment
	 *            The segment to be added to this mulitpath.
	 * @param bStartNewPath
	 *            TRUE if a new path will be added.
	 */
	public void addSegment(Segment segment, boolean bStartNewPath) {
		m_impl.addSegment(segment, bStartNewPath);
	}

	/**
	 * Reverses the order of the vertices in each path.
	 */
	public void reverseAllPaths() {
		m_impl.reverseAllPaths();
	}

	/**
	 * Reverses the order of vertices in the path.
	 * 
	 * @param pathIndex
	 *            The start index of the path to reverse the order.
	 */
	public void reversePath(int pathIndex) {
		m_impl.reversePath(pathIndex);
	}

	/**
	 * Removes the path at the given index.
	 * 
	 * @param pathIndex
	 *            The start index to remove the path.
	 */
	public void removePath(int pathIndex) {
		m_impl.removePath(pathIndex);
	}

	/**
	 * Inserts a path from another multipath.
	 * 
	 * @param pathIndex
	 *            The start index of the multipath to insert.
	 * @param src
	 *            The multipath to insert into this multipath. Can be the same
	 *            as the multipath being modified.
	 * @param srcPathIndex
	 *            The start index to insert the path into the multipath.
	 * @param bForward
	 *            When FALSE, the points are inserted in reverse order.
	 */
	public void insertPath(int pathIndex, MultiPath src, int srcPathIndex,
			boolean bForward) {
		m_impl.insertPath(pathIndex, (MultiPathImpl) src._getImpl(),
				srcPathIndex, bForward);
	}

	/**
	 * Inserts a path from an array of 2D Points.
	 * 
	 * @param pathIndex
	 *            The path index of the multipath to place the new path.
	 * @param points
	 *            The array of points defining the new path.
	 * @param pointsOffset
	 *            The offset into the array to start reading.
	 * @param count
	 *            The number of points to insert into the new path.
	 * @param bForward
	 *            When FALSE, the points are inserted in reverse order.
	 */
	void insertPath(int pathIndex, Point2D[] points, int pointsOffset,
			int count, boolean bForward) {
		m_impl.insertPath(pathIndex, points, pointsOffset, count, bForward);
	}

	/**
	 * Inserts vertices from the given multipath into this multipath. All added
	 * vertices are connected by linear segments with each other and with the
	 * existing vertices.
	 * 
	 * @param pathIndex
	 *            The path index in this multipath to insert points to. Must
	 *            correspond to an existing path.
	 * @param beforePointIndex
	 *            The point index before all other vertices to insert in the
	 *            given path of this multipath. This value must be between 0 and
	 *            GetPathSize(pathIndex), or -1 to insert points at the end of
	 *            the given path.
	 * @param src
	 *            The source multipath.
	 * @param srcPathIndex
	 *            The source path index to copy points from.
	 * @param srcPointIndexFrom
	 *            The start point in the source path to start copying from.
	 * @param srcPointCount
	 *            The count of points to add.
	 * @param bForward
	 *            When FALSE, the points are inserted in reverse order.
	 */
	public void insertPoints(int pathIndex, int beforePointIndex,
			MultiPath src, int srcPathIndex, int srcPointIndexFrom,
			int srcPointCount, boolean bForward) {
		m_impl.insertPoints(pathIndex, beforePointIndex,
				(MultiPathImpl) src._getImpl(), srcPathIndex,
				srcPointIndexFrom, srcPointCount, bForward);
	}

	/**
	 * Inserts a part of a path from the given array.
	 * 
	 * @param pathIndex
	 *            The path index in this class to insert points to. Must
	 *            correspond to an existing path.
	 * @param beforePointIndex
	 *            The point index in the given path of this MultiPath before
	 *            which the vertices need to be inserted. This value must be
	 *            between 0 and GetPathSize(pathIndex), or -1 to insert points
	 *            at the end of the given path.
	 * @param src
	 *            The source array
	 * @param srcPointIndexFrom
	 *            The start point in the source array to start copying from.
	 * @param srcPointCount
	 *            The count of points to add.
	 * @param bForward
	 *            When FALSE, the points are inserted in reverse order.
	 */
	void insertPoints(int pathIndex, int beforePointIndex, Point2D[] src,
			int srcPointIndexFrom, int srcPointCount, boolean bForward) {
		m_impl.insertPoints(pathIndex, beforePointIndex, src,
				srcPointIndexFrom, srcPointCount, bForward);
	}

	/**
	 * Inserts a point.
	 * 
	 * @param pathIndex
	 *            The path index in this class to insert the point to. Must
	 *            correspond to an existing path.
	 * @param beforePointIndex
	 *            The point index in the given path of this multipath. This
	 *            value must be between 0 and GetPathSize(pathIndex), or -1 to
	 *            insert the point at the end of the given path.
	 * @param pt
	 *            The point to be inserted.
	 */
	void insertPoint(int pathIndex, int beforePointIndex, Point2D pt) {
		m_impl.insertPoint(pathIndex, beforePointIndex, pt);
	}

	/**
	 * Inserts a point.
	 * 
	 * @param pathIndex
	 *            The path index in this class to insert the point to. Must
	 *            correspond to an existing path.
	 * @param beforePointIndex
	 *            The point index in the given path of this multipath. This
	 *            value must be between 0 and GetPathSize(pathIndex), or -1 to
	 *            insert the point at the end of the given path.
	 * @param pt
	 *            The point to be inserted.
	 */
	public void insertPoint(int pathIndex, int beforePointIndex, Point pt) {
		m_impl.insertPoint(pathIndex, beforePointIndex, pt);
	}

	/**
	 * Removes a point at a given index.
	 * 
	 * @param pathIndex
	 *            The path from whom to remove the point.
	 * @param pointIndex
	 *            The index of the point to be removed.
	 */
	public void removePoint(int pathIndex, int pointIndex) {
		m_impl.removePoint(pathIndex, pointIndex);
	}

	/**
	 * Returns the number of paths in this multipath.
	 * 
	 * @return The number of paths in this multipath.
	 */
	public int getPathCount() {
		return m_impl.getPathCount();
	}

	/**
	 * Returns the number of vertices in a path.
	 * 
	 * @param pathIndex
	 *            The index of the path to return the number of vertices from.
	 * @return The number of vertices in a path.
	 */
	public int getPathSize(int pathIndex) {
		return m_impl.getPathSize(pathIndex);
	}

	/**
	 * Returns the start index of the path.
	 * 
	 * @param pathIndex
	 *            The index of the path to return the start index from.
	 * @return The start index of the path.
	 * 
	 */
	public int getPathStart(int pathIndex) {
		return m_impl.getPathStart(pathIndex);
	}

	/**
	 * Returns the index immediately following the last index of the path.
	 * 
	 * @param pathIndex
	 *            The index of the path to return the end index from.
	 * @return Integer index after last index of path
	 */
	public int getPathEnd(int pathIndex) {
		return m_impl.getPathEnd(pathIndex);
	}

	/**
	 * Returns the path index from the point index. This is O(log N) operation.
	 * 
	 * @param pointIndex
	 *            The index of the point.
	 * @return The index of the path.
	 */
	public int getPathIndexFromPointIndex(int pointIndex) {
		return m_impl.getPathIndexFromPointIndex(pointIndex);
	}

	/**
	 * Starts a new path at given coordinates.
	 * 
	 * @param x
	 *            The X coordinate of the start point.
	 * @param y
	 *            The Y coordinate of the start point.
	 */
	public void startPath(double x, double y) {
		m_impl.startPath(x, y);
	}

	void startPath(Point2D point) {
		m_impl.startPath(point);
	}

	void startPath(Point3D point) {
		m_impl.startPath(point);
	}

	/**
	 * Starts a new path at a point.
	 * 
	 * @param point
	 *            The point to start the path from.
	 */
	public void startPath(Point point) {
		m_impl.startPath(point);
	}

	/**
	 * Adds a line segment from the last point to the given end coordinates.
	 * 
	 * @param x
	 *            The X coordinate to the end point.
	 * @param y
	 *            The Y coordinate to the end point.
	 */
	public void lineTo(double x, double y) {
		m_impl.lineTo(x, y);
	}

	void lineTo(Point2D endPoint) {
		m_impl.lineTo(endPoint);
	}

	void lineTo(Point3D endPoint) {
		m_impl.lineTo(endPoint);
	}

	/**
	 * Adds a Line Segment to the given end point.
	 * 
	 * @param endPoint
	 *            The end point to which the newly added line segment should
	 *            point.
	 */
	public void lineTo(Point endPoint) {
		m_impl.lineTo(endPoint);
	}

	/**
	 * Adds a Cubic Bezier Segment to the current Path. The Bezier Segment
	 * connects the current last Point and the given endPoint.
	 */
	void bezierTo(Point2D controlPoint1, Point2D controlPoint2, Point2D endPoint) {
		m_impl.bezierTo(controlPoint1, controlPoint2, endPoint);
	}

	/**
	 * Closes the last path of this multipath with a line segment. The closing
	 * segment is a segment that connects the last and the first points of the
	 * path. This is a virtual segment. The first point is not duplicated to
	 * close the path.
	 * 
	 * Call this method only for polylines. For polygons this method is
	 * implicitly called for the Polygon class.
	 */
	public void closePathWithLine() {
		m_impl.closePathWithLine();
	}

	/**
	 * Closes last path of the MultiPath with the Bezier Segment.
	 * 
	 * The start point of the Bezier is the last point of the path and the last
	 * point of the bezier is the first point of the path.
	 */
	void closePathWithBezier(Point2D controlPoint1, Point2D controlPoint2) {
		m_impl.closePathWithBezier(controlPoint1, controlPoint2);
	}

	/**
	 * Closes last path of the MultiPath with the Arc Segment.
	 */
	void closePathWithArc() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * Closes all open paths by adding an implicit line segment from the end
	 * point to the start point. Call this method only for polylines.For
	 * polygons this method is implicitly called for the Polygon class.
	 */
	public void closeAllPaths() {
		m_impl.closeAllPaths();
	}

	/**
	 * Indicates if the given path is closed (represents a ring). A closed path
	 * has a virtual segment that connects the last and the first points of the
	 * path. The first point is not duplicated to close the path. Polygons
	 * always have all paths closed.
	 * 
	 * @param pathIndex
	 *            The index of the path to check to be closed.
	 * @return TRUE if the given path is closed (represents a Ring).
	 */
	public boolean isClosedPath(int pathIndex) {
		return m_impl.isClosedPath(pathIndex);
	}

	public boolean isClosedPathInXYPlane(int pathIndex) {
		return m_impl.isClosedPathInXYPlane(pathIndex);
	}

	/**
	 * Returns TRUE if the given path might have non-linear segments.
	 */
	boolean hasNonLinearSegments(int pathIndex) {
		return m_impl.hasNonLinearSegments(pathIndex);
	}

	/**
	 * Adds a rectangular closed Path to the MultiPathImpl.
	 * 
	 * @param envSrc
	 *            is the source rectangle.
	 * @param bReverse
	 *            Creates reversed path.
	 */
	public void addEnvelope(Envelope2D envSrc, boolean bReverse) {
		m_impl.addEnvelope(envSrc, bReverse);
	}

	/**
	 * Adds a rectangular closed path to this multipath.
	 * 
	 * @param envSrc
	 *            Is the envelope to add to this mulitpath.
	 * @param bReverse
	 *            Adds the path reversed (counter-clockwise).
	 */
	public void addEnvelope(Envelope envSrc, boolean bReverse) {
		m_impl.addEnvelope(envSrc, bReverse);
	}

	/**
	 * Returns a SegmentIterator that is set right before the beginning of the
	 * multipath. Calling nextPath() will set the iterator to the first path of
	 * this multipath.
	 * 
	 * @return The SegmentIterator for this mulitpath.
	 */
	public SegmentIterator querySegmentIterator() {
		return new SegmentIterator(m_impl.querySegmentIterator());
	}

	/**
	 * Returns a SegmentIterator that is set to a specific vertex of the
	 * MultiPath. The call to nextSegment() will return the segment that starts
	 * at the vertex. Calling PreviousSegment () will return the segment that
	 * starts at the previous vertex.
	 * 
	 * @param startVertexIndex
	 *            The start index of the SegementIterator.
	 * @return The SegmentIterator for this mulitpath at the specified vertex.
	 */
	public SegmentIterator querySegmentIteratorAtVertex(int startVertexIndex) {
		return new SegmentIterator(
				m_impl.querySegmentIteratorAtVertex(startVertexIndex));
	}

	@Override
	public void setEmpty() {
		m_impl.setEmpty();
	}

	@Override
	public void applyTransformation(Transformation2D transform) {
		m_impl.applyTransformation(transform);
	}

	@Override
	void applyTransformation(Transformation3D transform) {
		m_impl.applyTransformation(transform);
	}

	@Override
	protected Object _getImpl() {
		return m_impl;
	}

	/**
	 * Returns the hash code for the multipath.
	 */
	@Override
	public int hashCode() {
		return m_impl.hashCode();
	}

	@Override
	public void getPointByVal(int index, Point outPoint) {
		m_impl.getPointByVal(index, outPoint);
	}

	@Override
	public void setPointByVal(int index, Point point) {
		m_impl.setPointByVal(index, point);
	}

	@Override
	public int getStateFlag() {
		return m_impl.getStateFlag();
	}

    @Override
    public void replaceNaNs(int semantics, double value) {
    	m_impl.replaceNaNs(semantics, value);
    }
	
}
