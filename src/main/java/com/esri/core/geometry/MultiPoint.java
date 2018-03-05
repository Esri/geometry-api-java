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

import static com.esri.core.geometry.SizeOf.SIZE_OF_MULTI_POINT;

/**
 * A Multipoint is a collection of points. A multipoint is a one-dimensional
 * geometry object. Multipoints can be used to store a collection of point-based
 * information where the order and individual identity of each point is not an
 * essential characteristic of the point set.
 */
public class MultiPoint extends MultiVertexGeometry implements
		Serializable {

	private static final long serialVersionUID = 2L;

	private MultiPointImpl m_impl;

	/**
	 * Creates a new empty multipoint.
	 */
	public MultiPoint() {
		m_impl = new MultiPointImpl();
	}

	public MultiPoint(VertexDescription description) {
		m_impl = new MultiPointImpl(description);
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
	public Point getPoint(int index) {
		return m_impl.getPoint(index);
	}

	@Override
	public int getPointCount() {
		return m_impl.getPointCount();
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
	Point3D getXYZ(int index) {
		return m_impl.getXYZ(index);
	}

	@Override
	public void queryCoordinates(Point2D[] dst) {
		m_impl.queryCoordinates(dst);
	}

	@Override
	public void queryCoordinates(Point[] dst) {
		m_impl.queryCoordinates(dst);
	}

	@Override
	protected Object _getImpl() {
		return m_impl;
	}

	/**
	 * Adds a point multipoint.
	 * 
	 * @param point
	 *            The Point to be added to this multipoint.
	 */
	public void add(Point point) {
		m_impl.add(point);
	}

	/**
	 * Adds a point with the specified X, Y coordinates to this multipoint.
	 * 
	 * @param x
	 *            The new Point's X coordinate.
	 * @param y
	 *            The new Point's Y coordinate.
	 */
	public void add(double x, double y) {
		m_impl.add(x, y);
	}

	/**
	 * Adds a point with the specified X, Y coordinates to this multipoint.
	 * 
	 * @param pt the point to add
	 */
	public void add(Point2D pt) {
		m_impl.add(pt.x, pt.y);
	}
	
	/**
	 * Adds a 3DPoint with the specified X, Y, Z coordinates to this multipoint.
	 * 
	 * @param x
	 *            The new Point's X coordinate.
	 * @param y
	 *            The new Point's Y coordinate.
	 * @param z
	 *            The new Point's Z coordinate.
	 */
	void add(double x, double y, double z) {
		m_impl.add(x, y, z);
	}

	/**
	 * Appends points from another multipoint at the end of this multipoint.
	 * 
	 * @param src
	 *            The mulitpoint to append to this multipoint.
	 * @param srcFrom
	 *            The start index in the source multipoint from which to start
	 *            appending points.
	 * @param srcTo
	 *            The end index in the source multipoint right after the last
	 *            point to be appended. Use -1 to indicate the rest of the
	 *            source multipoint.
	 */
	public void add(MultiVertexGeometry src, int srcFrom, int srcTo) {
		m_impl.add((MultiVertexGeometryImpl) src._getImpl(), srcFrom, srcTo);
	}

	void addPoints(Point2D[] points) {
		m_impl.addPoints(points);
	}

	void addPoints(Point[] points) {
		m_impl.addPoints(points);
	}

	/**
	 * Inserts a point to this multipoint.
	 * 
	 * @param beforePointIndex
	 *            The index right before the new point to insert.
	 * @param pt
	 *            The point to insert.
	 */
	public void insertPoint(int beforePointIndex, Point pt) {
		m_impl.insertPoint(beforePointIndex, pt);
	} // inserts a point. The point is connected with Lines

	/**
	 * Removes a point from this multipoint.
	 * 
	 * @param pointIndex
	 *            The index of the point to be removed.
	 */
	public void removePoint(int pointIndex) {
		m_impl.removePoint(pointIndex);
	}

	/**
	 * Resizes the multipoint to have the given size.
	 * 
	 * @param pointCount
	 *            - The number of points in this multipoint.
	 */
	public void resize(int pointCount) {
		m_impl.resize(pointCount);
	}

	@Override
	void queryCoordinates(Point3D[] dst) {
		m_impl.queryCoordinates(dst);
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
	public void setPoint(int index, Point pointSrc) {
		m_impl.setPoint(index, pointSrc);
	}

	@Override
	public void setXY(int index, Point2D pt) {
		m_impl.setXY(index, pt);
	}

	@Override
	void setXYZ(int index, Point3D pt) {
		m_impl.setXYZ(index, pt);
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
	public void copyTo(Geometry dst) {
		m_impl.copyTo((Geometry) dst._getImpl());
	}

	@Override
	public Geometry createInstance() {
		return new MultiPoint(getDescription());
	}

	@Override
	public int getDimension() {
		return 0;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_MULTI_POINT + m_impl.estimateMemorySize();
	}

	@Override
	public Geometry.Type getType() {
		return Type.MultiPoint;
	}

	@Override
	public VertexDescription getDescription() {
		return m_impl.getDescription();
	}

	@Override
	public void addAttribute(int semantics) {
		m_impl.addAttribute(semantics);
	}

	@Override
	public void assignVertexDescription(VertexDescription src) {
		m_impl.assignVertexDescription(src);
	}

	@Override
	public void dropAllAttributes() {
		m_impl.dropAllAttributes();
	}

	@Override
	public void dropAttribute(int semantics) {
		m_impl.dropAttribute(semantics);
	}

	@Override
	public void mergeVertexDescription(VertexDescription src) {
		m_impl.mergeVertexDescription(src);
	}

	@Override
	public boolean isEmpty() {
		return m_impl.isEmpty();
	}

	@Override
	public void queryEnvelope(Envelope env) {
		m_impl.queryEnvelope(env);
	}

	@Override
	public void queryEnvelope2D(Envelope2D env) {
		m_impl.queryEnvelope2D(env);
	}

	@Override
	void queryEnvelope3D(Envelope3D env) {
		m_impl.queryEnvelope3D(env);
	}

	@Override
	public Envelope1D queryInterval(int semantics, int ordinate) {
		return m_impl.queryInterval(semantics, ordinate);
	}

	@Override
	public void setEmpty() {
		m_impl.setEmpty();
	}

	/**
	 * Returns TRUE when this geometry has exactly same type, properties, and
	 * coordinates as the other geometry.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		if (other == this)
			return true;

		if (other.getClass() != getClass())
			return false;

		return m_impl.equals(((MultiPoint) other)._getImpl());
	}

	/**
	 * Returns a hash code value for this multipoint.
	 */
	@Override
	public int hashCode() {
		return m_impl.hashCode();
	}

	int queryCoordinates(Point2D[] dst, int dstSize, int beginIndex,
			int endIndex) {
		return m_impl.queryCoordinates(dst, dstSize, beginIndex, endIndex);
	}

	@Override
	public void getPointByVal(int index, Point outPoint) {
		m_impl.getPointByVal(index, outPoint);
	}

	@Override
	public void setPointByVal(int index, Point pointSrc) {
		m_impl.setPointByVal(index, pointSrc);
	}

	@Override
	public int getStateFlag() {
		return m_impl.getStateFlag();
	}

    @Override
    public Geometry getBoundary() {
        return m_impl.getBoundary();
    }
    
    @Override
    public void replaceNaNs(int semantics, double value) {
    	m_impl.replaceNaNs(semantics, value);
    }
}
