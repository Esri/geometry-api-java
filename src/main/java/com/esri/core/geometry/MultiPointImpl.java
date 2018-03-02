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

import com.esri.core.geometry.VertexDescription.Semantics;

import static com.esri.core.geometry.SizeOf.SIZE_OF_MULTI_POINT_IMPL;

/**
 * The MultiPoint is a collection of points.
 */
final class MultiPointImpl extends MultiVertexGeometryImpl {
	public MultiPointImpl() {
		super();
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		m_pointCount = 0;
	}

	public MultiPointImpl(VertexDescription description) {
		super();
		if (description == null)
			throw new IllegalArgumentException();

		m_description = description;
		m_pointCount = 0;
	}

	@Override
	public Geometry createInstance() {
		return new MultiPoint(m_description);
	}

	/**
	 * Adds a Point to this MultiPoint.
	 */
	public void add(Point point) {
		resize(m_pointCount + 1);
		setPoint(m_pointCount - 1, point);
	}

	/**
	 * Adds a Point to this MultiPoint with given x, y coordinates.
	 */
	public void add(double x, double y) {
		resize(m_pointCount + 1);
		Point2D pt = new Point2D();
		pt.setCoords(x, y);
		setXY(m_pointCount - 1, pt);
	}

	/**
	 * Adds a Point to this MultiPoint with given x, y, z coordinates.
	 */
	public void add(double x, double y, double z) {
		resize(m_pointCount + 1);
		Point3D pt = new Point3D();
		pt.setCoords(x, y, z);
		setXYZ(m_pointCount - 1, pt);
	}

	/**
	 * Appends points from another MultiVertexGeometryImpl at the end of this
	 * one.
	 * 
	 * @param src
	 *            The source MultiVertexGeometryImpl
	 */
	public void add(MultiVertexGeometryImpl src, int beginIndex, int endIndex) {
		int endIndexC = endIndex < 0 ? src.getPointCount() : endIndex;
		if (beginIndex < 0 || beginIndex > src.getPointCount()
				|| endIndexC < beginIndex)
			throw new IllegalArgumentException();

		if (beginIndex == endIndexC)
			return;

		mergeVertexDescription(src.getDescription());
		int count = endIndexC - beginIndex;
		int oldPointCount = m_pointCount;
		resize(m_pointCount + count);
		_verifyAllStreams();
		for (int iattrib = 0, nattrib = src.getDescription()
				.getAttributeCount(); iattrib < nattrib; iattrib++) {
			int semantics = src.getDescription()._getSemanticsImpl(iattrib);
			int ncomps = VertexDescription.getComponentCount(semantics);
			AttributeStreamBase stream = getAttributeStreamRef(semantics);
			AttributeStreamBase srcStream = src
					.getAttributeStreamRef(semantics);
			stream.insertRange(oldPointCount * ncomps, srcStream, beginIndex
					* ncomps, count * ncomps, true, 1, oldPointCount * ncomps);
		}
	}

	public void addPoints(Point2D[] points) {
		int count = points.length;
		int oldPointCount = m_pointCount;
		resize(m_pointCount + count);
		for (int i = 0; i < count; i++)
			setXY(oldPointCount + i, points[i]);
	}

	public void insertPoint(int beforePointIndex, Point pt) {
		if (beforePointIndex > getPointCount())
			throw new GeometryException("index out of bounds");

		if (beforePointIndex < 0)
			beforePointIndex = getPointCount();

		mergeVertexDescription(pt.getDescription());
		int oldPointCount = m_pointCount;
		_resizeImpl(m_pointCount + 1);
		_verifyAllStreams();

		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int comp = VertexDescription.getComponentCount(semantics);

			AttributeStreamBase stream = AttributeStreamBase
					.createAttributeStreamWithSemantics(semantics, 1);
			if (pt.hasAttribute(semantics)) {
				m_vertexAttributes[iattr]
						.insertAttributes(comp * beforePointIndex, pt,
								semantics, comp * oldPointCount);
			} else {
				// Need to make room for the attribute, so we copy a default
				// value in

				double v = VertexDescription.getDefaultValue(semantics);
				m_vertexAttributes[iattr].insertRange(comp * beforePointIndex,
						v, comp, comp * oldPointCount);
			}
		}

		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	void removePoint(int pointIndex) {
		if (pointIndex < 0 || pointIndex >= getPointCount())
			throw new GeometryException("index out of bounds");

		_verifyAllStreams();

		// Remove the attribute value for the path
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			if (m_vertexAttributes[iattr] != null) {
				int semantics = m_description._getSemanticsImpl(iattr);
				int comp = VertexDescription.getComponentCount(semantics);
				m_vertexAttributes[iattr].eraseRange(comp * pointIndex, comp,
						comp * m_pointCount);
			}
		}

		m_pointCount--;
		m_reservedPointCount--;
		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	/**
	 * Resizes the MultiPoint to have the given size.
	 */
	public void resize(int pointCount) {
		_resizeImpl(pointCount);
	}

	@Override
	void _copyToImpl(MultiVertexGeometryImpl mvg) {
	}

	@Override
	public void setEmpty() {
		super._setEmptyImpl();
	}

	@Override
	public void applyTransformation(Transformation2D transform) {
		if (isEmpty())
			return;

		_verifyAllStreams();
		AttributeStreamOfDbl points = (AttributeStreamOfDbl) m_vertexAttributes[0];
		Point2D pt2 = new Point2D();

		for (int ipoint = 0; ipoint < m_pointCount; ipoint++) {
			pt2.x = points.read(ipoint * 2);
			pt2.y = points.read(ipoint * 2 + 1);

			transform.transform(pt2, pt2);
			points.write(ipoint * 2, pt2.x);
			points.write(ipoint * 2 + 1, pt2.y);
		}

		// REFACTOR: reset the exact envelope only and transform the loose
		// envelope
		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	@Override
	void applyTransformation(Transformation3D transform) {
		if (isEmpty())
			return;

		_verifyAllStreams();
		addAttribute(Semantics.Z);
		_verifyAllStreams();
		AttributeStreamOfDbl points = (AttributeStreamOfDbl) m_vertexAttributes[0];
		AttributeStreamOfDbl zs = (AttributeStreamOfDbl) m_vertexAttributes[1];
		Point3D pt3 = new Point3D();
		for (int ipoint = 0; ipoint < m_pointCount; ipoint++) {
			pt3.x = points.read(ipoint * 2);
			pt3.y = points.read(ipoint * 2 + 1);
			pt3.z = zs.read(ipoint);

			Point3D res = transform.transform(pt3);
			points.write(ipoint * 2, res.x);
			points.write(ipoint * 2 + 1, res.y);
			zs.write(ipoint, res.z);
		}

		// REFACTOR: reset the exact envelope only and transform the loose
		// envelope
		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	@Override
	public int getDimension() {
		return 0;
	}

	@Override
	public long estimateMemorySize()
	{
		long size = SIZE_OF_MULTI_POINT_IMPL + (m_envelope != null ? m_envelope.estimateMemorySize() : 0);

		if (m_vertexAttributes != null) {
			for (int i = 0; i < m_vertexAttributes.length; i++) {
				size += m_vertexAttributes[i].estimateMemorySize();
			}
		}
		return size;
	}

	@Override
	public Geometry.Type getType() {
		return Type.MultiPoint;
	}

	@Override
	public double calculateArea2D() {
		return 0;
	}

	@Override
	public double calculateLength2D() {
		return 0;
	}

	@Override
	public Object _getImpl() {
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;

		if (!(other instanceof MultiPointImpl))
			return false;

		return super.equals(other);
	}

	public void addPoints(Point[] points) {
		int count = points.length;
		// int oldPointCount = m_pointCount;
		resize(m_pointCount + count);
		for (int i = 0; i < count; i++)
			setPoint(i, points[i]);
	}

	public int queryCoordinates(Point2D[] dst, int dstSize, int beginIndex,
			int endIndex) {
		int endIndexC = endIndex < 0 ? m_pointCount : endIndex;
		endIndexC = Math.min(endIndexC, beginIndex + dstSize);

		if (beginIndex < 0 || beginIndex >= m_pointCount
				|| endIndexC < beginIndex || dst.length != dstSize)
			throw new IllegalArgumentException();// GEOMTHROW(invalid_argument);

		AttributeStreamOfDbl xy = (AttributeStreamOfDbl) getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		int pointCountToRead = endIndexC - beginIndex;
		double[] dstArray = new double[pointCountToRead * 2];
		xy.readRange(2 * beginIndex, pointCountToRead * 2, dstArray, 0, true);

		for (int i = 0; i < pointCountToRead; i++) {
			dst[i] = new Point2D(dstArray[i * 2], dstArray[i * 2 + 1]);
		}

		return endIndexC;
	}

	@Override
	protected void _notifyModifiedAllImpl() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _verifyStreamsImpl() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean _buildRasterizedGeometryAccelerator(double toleranceXY,
			GeometryAccelerationDegree accelDegree) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean _buildQuadTreeAccelerator(GeometryAccelerationDegree accelDegree) {
		// TODO Auto-generated method stub
		return false;
	}
	
	// @Override
	// void _notifyModifiedAllImpl() {
	// // TODO Auto-generated method stub
	//
	// }

	// @Override
	// protected void _verifyStreamsImpl() {
	// // TODO Auto-generated method stub
	//
	// }

    @Override
    public Geometry getBoundary() {
        return null;
    }
}
