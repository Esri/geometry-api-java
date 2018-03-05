/*
 Copyright 1995-2018 Esri

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

import java.io.Serializable;

import static com.esri.core.geometry.SizeOf.SIZE_OF_ENVELOPE;

/**
 * An envelope is an axis-aligned rectangle.
 */
public class Envelope extends Geometry implements Serializable {
	//We are using writeReplace instead.
	//private static final long serialVersionUID = 2L;

	Envelope2D m_envelope = new Envelope2D();

	double[] m_attributes;// use doubles to store everything

	/**
	 * Creates an envelope by defining its center, width, and height.
	 * 
	 * @param center
	 *            The center point of the envelope.
	 * @param width
	 *            The width of the envelope.
	 * @param height
	 *            The height of the envelope.
	 */
	public Envelope(Point center, double width, double height) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		m_envelope.setEmpty();
		if (center.isEmpty())
			return;

		_setFromPoint(center, width, height);
	}

	public Envelope(Envelope2D env2D) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		m_envelope.setCoords(env2D);
		m_envelope.normalize();
	}

	public Envelope(VertexDescription vd) {
		if (vd == null)
			throw new IllegalArgumentException();
		m_description = vd;
		m_envelope.setEmpty();
	}

	public Envelope(VertexDescription vd, Envelope2D env2D) {
		if (vd == null)
			throw new IllegalArgumentException();
		m_description = vd;
		m_envelope.setCoords(env2D);
		m_envelope.normalize();
	}

	/**
	 * Constructs an empty envelope.
	 */
	public Envelope() {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		m_envelope.setEmpty();
	}

	/**
	 * Constructs an envelope that covers the given point. The coordinates of
	 * the point are used to set the extent of the envelope.
	 * 
	 * @param point The point that the envelope covers.
	 */
	public Envelope(Point point) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		m_envelope.setEmpty();
		if (point.isEmpty())
			return;

		_setFromPoint(point);
	}

	/**
	 * Constructs an envelope with the specified X and Y extents.
	 * 
	 * @param xmin
	 *            The minimum x-coordinate of the envelope.
	 * @param ymin
	 *            The minimum y-coordinate of the envelope.
	 * @param xmax
	 *            The maximum x-coordinate of the envelope.
	 * @param ymax
	 *            The maximum y-coordinate of the envelope.
	 */
	public Envelope(double xmin, double ymin, double xmax, double ymax) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		setCoords(xmin, ymin, xmax, ymax);
	}

	/**
	 * Sets the 2-dimensional extents of the envelope.
	 * 
	 * @param xmin
	 *            The minimum x-coordinate of the envelope.
	 * @param ymin
	 *            The minimum y-coordinate of the envelope.
	 * @param xmax
	 *            The maximum x-coordinate of the envelope.
	 * @param ymax
	 *            The maximum y-coordinate of the envelope.
	 */
	public void setCoords(double xmin, double ymin, double xmax, double ymax) {
		_touch();
		m_envelope.setCoords(xmin, ymin, xmax, ymax);
	}

	/**
	 * Sets the envelope from the array of points. The result envelope is a
	 * bounding box of all the points in the array. If the array has zero
	 * length, the envelope will be empty.
	 * 
	 * @param points
	 *            The point array.
	 */
	void setCoords(Point[] points) {
		_touch();
		setEmpty();
		for (int i = 0, n = points.length; i < n; i++)
			merge(points[i]);
	}

	void setEnvelope2D(Envelope2D e2d) {
		_touch();
		if (!e2d.isValid())
			throw new IllegalArgumentException();

		m_envelope.setCoords(e2d);
	}

	/**
	 * Removes all points from this geometry.
	 */
	@Override
	public void setEmpty() {
		_touch();
		m_envelope.setEmpty();
	}

	/**
	 * Indicates whether this envelope contains any points.
	 * 
	 * @return boolean Returns true if the envelope is empty.
	 */
	@Override
	public boolean isEmpty() {
		return m_envelope.isEmpty();
	}

	/**
	 * The width of the envelope.
	 * 
	 * @return The width of the envelope.
	 */

	public double getWidth() {
		return m_envelope.getWidth();
	}

	/**
	 * The height of the envelope.
	 * 
	 * @return The height of the envelope.
	 */
	public double getHeight() {
		return m_envelope.getHeight();
	}

	/**
	 * The x-coordinate of the center of the envelope.
	 * 
	 * @return The x-coordinate of the center of the envelope.
	 */
	public double getCenterX() {
		return m_envelope.getCenterX();
	}

	/**
	 * The y-coordinate of center of the envelope.
	 * 
	 * @return The y-coordinate of center of the envelope.
	 */
	public double getCenterY() {
		return m_envelope.getCenterY();
	}

	/**
	 * The x and y-coordinates of the center of the envelope.
	 * 
	 * @return A point whose x and y-coordinates are that of the center of the envelope.
	 */
	public Point2D getCenterXY() {
		return m_envelope.getCenter();
	}

	public void getCenter(Point point_out) {
		point_out.assignVertexDescription(m_description);
		if (isEmpty()) {
			point_out.setEmpty();
			return;
		}

		int nattrib = m_description.getAttributeCount();
		for (int i = 1; i < nattrib; i++) {
			int semantics = m_description.getSemantics(i);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomp; iord++) {
				double v = 0.5 * (getAttributeAsDblImpl_(0, semantics, iord) + getAttributeAsDblImpl_(
						1, semantics, iord));
				point_out.setAttribute(semantics, iord, v);
			}
		}
		point_out.setXY(m_envelope.getCenter());
	}

	public void merge(Point2D pt) {
		_touch();
		m_envelope.merge(pt);
	}

	/**
	 * Merges this envelope with the extent of the given envelope. If this
	 * envelope is empty, the coordinates of the given envelope 
	 * are assigned. If the given envelope is empty, this envelope is unchanged.
	 * 
	 * @param other
	 *            The envelope to merge.
	 */
	public void merge(Envelope other) {
		_touch();
		if (other.isEmpty())
			return;

		VertexDescription otherVD = other.m_description;
		if (otherVD != m_description)
			mergeVertexDescription(otherVD);
		m_envelope.merge(other.m_envelope);
		for (int iattrib = 1, nattrib = otherVD.getAttributeCount(); iattrib < nattrib; iattrib++) {
			int semantics = otherVD.getSemantics(iattrib);
			int ncomps = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomps; iord++) {
				Envelope1D intervalOther = other.queryInterval(semantics, iord);
				Envelope1D interval = queryInterval(semantics, iord);
				interval.merge(intervalOther);
				setInterval(semantics, iord, interval);
			}
		}
	}

	/**
	 * Merges this envelope with the point. The boundary of the envelope is
	 * increased to include the point. If the envelope is empty, the coordinates
	 * of the point to merge are assigned. If the point is empty, the original
	 * envelope is unchanged.
	 * 
	 * @param point
	 *            The point to be merged.
	 */
	public void merge(Point point) {
		_touch();
		if (point.isEmptyImpl())
			return;

		VertexDescription pointVD = point.m_description;
		if (m_description != pointVD)
			mergeVertexDescription(pointVD);

		if (isEmpty()) {
			_setFromPoint(point);
			return;
		}

		m_envelope.merge(point.getXY());
		for (int iattrib = 1, nattrib = pointVD.getAttributeCount(); iattrib < nattrib; iattrib++) {
			int semantics = pointVD._getSemanticsImpl(iattrib);
			int ncomps = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomps; iord++) {
				double v = point.getAttributeAsDbl(semantics, iord);
				Envelope1D interval = queryInterval(semantics, iord);
				interval.merge(v);
				setInterval(semantics, iord, interval);
			}
		}
	}

	void _setFromPoint(Point centerPoint, double width, double height) {
		m_envelope.setCoords(centerPoint.getXY(), width, height);
		VertexDescription pointVD = centerPoint.m_description;
		for (int iattrib = 1, nattrib = pointVD.getAttributeCount(); iattrib < nattrib; iattrib++) {
			int semantics = pointVD._getSemanticsImpl(iattrib);
			int ncomps = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomps; iord++) {
				double v = centerPoint.getAttributeAsDbl(semantics, iord);
				setInterval(semantics, iord, v, v);
			}
		}
	}

	void _setFromPoint(Point centerPoint) {
		m_envelope.setCoords(centerPoint.m_attributes[0],
				centerPoint.m_attributes[1]);
		VertexDescription pointVD = centerPoint.m_description;
		for (int iattrib = 1, nattrib = pointVD.getAttributeCount(); iattrib < nattrib; iattrib++) {
			int semantics = pointVD._getSemanticsImpl(iattrib);
			int ncomps = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomps; iord++) {
				double v = centerPoint.getAttributeAsDbl(semantics, iord);
				setInterval(semantics, iord, v, v);
			}
		}
	}

	public void merge(Envelope2D other) {
		_touch();
		m_envelope.merge(other);
	}

	public void setInterval(int semantics, int ordinate, double vmin,
			double vmax) {
		setInterval(semantics, ordinate, new Envelope1D(vmin, vmax));
	}

	/**
	 * Re-aspects this envelope to fit within the specified width and height.
	 * 
	 * @param arWidth
	 *            The width within which to fit the envelope.
	 * @param arHeight
	 *            The height within which to fit the envelope.
	 */
	public void reaspect(double arWidth, double arHeight) {
		_touch();
		m_envelope.reaspect(arWidth, arHeight);
	}

	/**
	 * Changes the dimensions of the envelope while preserving the center. New width
	 * is Width + 2 * dx, new height is Height + 2 * dy. If the result envelope
	 * width or height becomes negative, the envelope is set to be empty.
	 * 
	 * @param dx
	 *            The inflation along the x-axis.
	 * @param dy
	 *            The inflation along the y-axis.
	 */
	public void inflate(double dx, double dy) {
		_touch();
		m_envelope.inflate(dx, dy);
	}

	@Override
	public void applyTransformation(Transformation2D transform) {
		_touch();
		transform.transform(m_envelope);
	}

	@Override
	void applyTransformation(Transformation3D transform) {
		_touch();
		if (!m_envelope.isEmpty()) {
			Envelope3D env = new Envelope3D();
			queryEnvelope3D(env);
			if (env.isEmptyZ())
				env.setEmpty(); // Z components is empty, the
								// AffineTransformation3D makes the whole
								// envelope empty. Consider
			// throwing an assert instead.
			else
				transform.transform(env);
		}
	}

	@Override
	public void copyTo(Geometry dst) {
		if (dst.getType() != getType())
			throw new IllegalArgumentException();

		Envelope envDst = (Envelope) dst;
		dst._touch();
		envDst.m_description = m_description;
		envDst.m_envelope.setCoords(m_envelope);
		envDst.m_attributes = null;
		if (m_attributes != null)
		{
			envDst._ensureAttributes();
			System.arraycopy(m_attributes, 0, envDst.m_attributes, 0,
					(m_description.getTotalComponentCount() - 2) * 2);
		}
	}

	@Override
	public Geometry createInstance() {
		return new Envelope(m_description);
	}

	@Override
	public double calculateArea2D() {
		return m_envelope.getArea();
	}

	@Override
	public double calculateLength2D() {
		return m_envelope.getLength();
	}

	@Override
	public Geometry.Type getType() {
		return Type.Envelope;
	}

	@Override
	public int getDimension() {
		return 2;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_ENVELOPE + m_envelope.estimateMemorySize() + estimateMemorySize(m_attributes);
	}

	@Override
	public void queryEnvelope(Envelope env) {
		copyTo(env);
	}

	@Override
	public void queryEnvelope2D(Envelope2D env) {
		env.xmin = m_envelope.xmin;
		env.ymin = m_envelope.ymin;
		env.xmax = m_envelope.xmax;
		env.ymax = m_envelope.ymax;
	}

	@Override
	void queryEnvelope3D(Envelope3D env) {
		env.xmin = m_envelope.xmin;
		env.ymin = m_envelope.ymin;
		env.xmax = m_envelope.xmax;
		env.ymax = m_envelope.ymax;
		env.setCoords(m_envelope.xmin, m_envelope.ymin,
				_getAttributeAsDbl(0, Semantics.Z, 0), m_envelope.xmax,
				m_envelope.ymax, _getAttributeAsDbl(1, Semantics.Z, 0));
	}

	@Override
	public Envelope1D queryInterval(int semantics, int ordinate) {
		Envelope1D env = new Envelope1D();
		env.setCoords(_getAttributeAsDbl(0, semantics, ordinate),
				_getAttributeAsDbl(1, semantics, ordinate));
		return env;
	}

	public void setInterval(int semantics, int ordinate, Envelope1D env) {
		_touch();
		if (semantics == Semantics.POSITION) {
			if (ordinate == 0) {
				m_envelope.xmin = env.vmin;
				m_envelope.xmax = env.vmax;
			} else if (ordinate == 1) {
				m_envelope.ymin = env.vmin;
				m_envelope.ymax = env.vmax;
			} else
				throw new IndexOutOfBoundsException();
		} else {
			_setAttributeAsDbl(0, semantics, ordinate, env.vmin);
			_setAttributeAsDbl(1, semantics, ordinate, env.vmax);
		}
	}

	public void queryCoordinates(Point2D[] dst) {
		if (dst == null || dst.length < 4 || m_envelope.isEmpty())
			throw new IllegalArgumentException();

		m_envelope.queryCorners(dst);
	}

	/**
	 * Sets the point's coordinates to the coordinates of the envelope at the
	 * given corner.
	 * 
	 * @param index
	 *            The index of the envelope's corners from 0 to 3.
	 *            <p>
	 *            0 = lower left corner
	 *            <p>
	 *            1 = top-left corner
	 *            <p>
	 *            2 = top right corner
	 *            <p>
	 *            3 = bottom right corner
	 * @param ptDst
	 *            The point whose coordinates are used to set the envelope's
	 *            coordinate at a specified corner.
	 */
	public void queryCornerByVal(int index, Point ptDst) {
		ptDst.assignVertexDescription(m_description);
		int nattrib = getDescription().getAttributeCount() - 1;
		switch (index) {
		case 0: {
			for (int i = 0; i < nattrib; i++) {
				int semantics = m_description.getSemantics(i);
				int ncomp = VertexDescription.getComponentCount(semantics);
				for (int iord = 0; iord < ncomp; iord++)
					ptDst.setAttribute(semantics, iord,
							_getAttributeAsDbl(0, semantics, iord));
			}
			ptDst.setXY(m_envelope.xmin, m_envelope.ymin);
			return;
		}

		case 1: {
			for (int i = 0; i < nattrib; i++) {
				int semantics = m_description.getSemantics(i);
				int ncomp = VertexDescription.getComponentCount(semantics);
				for (int iord = 0; iord < ncomp; iord++)
					ptDst.setAttribute(semantics, iord,
							_getAttributeAsDbl(1, semantics, iord));
			}
			ptDst.setXY(m_envelope.xmin, m_envelope.ymax);
			return;
		}
		case 2: {
			for (int i = 0; i < nattrib; i++) {
				int semantics = m_description.getSemantics(i);
				int ncomp = VertexDescription.getComponentCount(semantics);
				for (int iord = 0; iord < ncomp; iord++)
					ptDst.setAttribute(semantics, iord,
							_getAttributeAsDbl(0, semantics, iord));
			}
			ptDst.setXY(m_envelope.xmax, m_envelope.ymax);

			return;
		}
		case 3: {
			for (int i = 0; i < nattrib; i++) {
				int semantics = m_description.getSemantics(i);
				int ncomp = VertexDescription.getComponentCount(semantics);
				for (int iord = 0; iord < ncomp; iord++)
					ptDst.setAttribute(semantics, iord,
							_getAttributeAsDbl(1, semantics, iord));
			}
			ptDst.setXY(m_envelope.xmax, m_envelope.ymin);
			return;
		}
		default:
			throw new IndexOutOfBoundsException();
		}
	}

	public void queryCorner(int index, Point2D ptDst) {
		Point2D p = m_envelope.queryCorner(index);
		ptDst.setCoords(p.x, p.y);
	}

	int getEndPointOffset(VertexDescription descr, int end_point) {
		return end_point * (descr.getTotalComponentCount() - 2);
	}

	double getAttributeAsDblImpl_(int end_point, int semantics, int ordinate) {
		if (m_envelope.isEmpty())
			throw new GeometryException("empty geometry");

		assert (end_point == 0 || end_point == 1);

		if (semantics == VertexDescription.Semantics.POSITION) {
			if (end_point != 0) {
				return ordinate != 0 ? m_envelope.ymax : m_envelope.xmax;
			} else {
				return ordinate != 0 ? m_envelope.ymin : m_envelope.xmin;
			}
		}

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IllegalArgumentException();

		int attribute_index = m_description.getAttributeIndex(semantics);
		_ensureAttributes();
		if (attribute_index >= 0) {
			return m_attributes[getEndPointOffset(m_description, end_point)
					+ m_description.getPointAttributeOffset_(attribute_index)
					- 2 + ordinate];
		}

		return VertexDescription.getDefaultValue(semantics);
	}

	void setAttributeAsDblImpl_(int end_point, int semantics, int ordinate,
			double value) {
		assert (end_point == 0 || end_point == 1);

		if (semantics == VertexDescription.Semantics.POSITION) {
			if (end_point != 0) {
				if (ordinate != 0)
					m_envelope.ymax = value;
				else
					m_envelope.xmax = value;
			} else {
				if (ordinate != 0)
					m_envelope.ymin = value;
				else
					m_envelope.xmin = value;
			}
			
			return;
		}

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IllegalArgumentException();

		addAttribute(semantics);
		_ensureAttributes();
		int attribute_index = m_description.getAttributeIndex(semantics);
		m_attributes[getEndPointOffset(m_description, end_point)
				+ m_description.getPointAttributeOffset_(attribute_index) - 2
				+ ordinate] = value;
	}

	void _ensureAttributes() {
		_touch();
		if (m_attributes == null && m_description.getTotalComponentCount() > 2) {
			m_attributes = new double[(m_description.getTotalComponentCount() - 2) * 2];
			int offset0 = _getEndPointOffset(m_description, 0);
			int offset1 = _getEndPointOffset(m_description, 1);
			
			int j = 0;
			for (int i = 1, n = m_description.getAttributeCount(); i < n; i++) {
				int semantics = m_description.getSemantics(i);
				int nords = VertexDescription.getComponentCount(semantics);
				double d = VertexDescription.getDefaultValue(semantics);
				for (int ord = 0; ord < nords; ord++)
				{
					m_attributes[offset0 + j] = d;
					m_attributes[offset1 + j] = d;
					j++;
				}
			}
		}
	}

	@Override
	protected void _assignVertexDescriptionImpl(VertexDescription newDescription) {
		if (m_attributes == null) {
			m_description = newDescription;
			return;
		}
		
		if (newDescription.getTotalComponentCount() > 2) {
			int[] mapping = VertexDescriptionDesignerImpl.mapAttributes(newDescription, m_description);
			
			double[] newAttributes = new double[(newDescription.getTotalComponentCount() - 2) * 2];
			
			int old_offset0 = _getEndPointOffset(m_description, 0);
			int old_offset1 = _getEndPointOffset(m_description, 1);
	
			int new_offset0 = _getEndPointOffset(newDescription, 0);
			int new_offset1 = _getEndPointOffset(newDescription, 1);
			
			int j = 0;
			for (int i = 1, n = newDescription.getAttributeCount(); i < n; i++) {
				int semantics = newDescription.getSemantics(i);
				int nords = VertexDescription.getComponentCount(semantics);
				if (mapping[i] == -1)
				{
					double d = VertexDescription.getDefaultValue(semantics);
					for (int ord = 0; ord < nords; ord++)
					{
						newAttributes[new_offset0 + j] = d;
						newAttributes[new_offset1 + j] = d;
						j++;
					}
				}
				else {
					int m = mapping[i];
					int offset = m_description._getPointAttributeOffset(m) - 2;
					for (int ord = 0; ord < nords; ord++)
					{
						newAttributes[new_offset0 + j] = m_attributes[old_offset0 + offset];
						newAttributes[new_offset1 + j] = m_attributes[old_offset1 + offset];
						j++;
						offset++;
					}
				}
					 
			}
			
			m_attributes = newAttributes;
		}
		else {
			m_attributes = null;
		}
		
		m_description = newDescription;
	}

	double _getAttributeAsDbl(int endPoint, int semantics, int ordinate) {
		if (m_envelope.isEmpty())
			throw new GeometryException(
					"This operation was performed on an Empty Geometry.");

		// _ASSERT(endPoint == 0 || endPoint == 1);

		if (semantics == Semantics.POSITION) {
			if (endPoint != 0) {
				return ordinate != 0 ? m_envelope.ymax : m_envelope.xmax;
			} else {
				return ordinate != 0 ? m_envelope.ymin : m_envelope.xmin;
			}
		}

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (attributeIndex >= 0) {
			_ensureAttributes();
			return m_attributes[_getEndPointOffset(m_description, endPoint)
					+ m_description._getPointAttributeOffset(attributeIndex)
					- 2 + ordinate];
		} else
			return VertexDescription.getDefaultValue(semantics);
	}

	void _setAttributeAsDbl(int endPoint, int semantics, int ordinate,
			double value) {
		_touch();
		// _ASSERT(endPoint == 0 || endPoint == 1);

		if (semantics == Semantics.POSITION) {
			if (endPoint != 0) {
				if (ordinate != 0)
					m_envelope.ymax = value;
				else
					m_envelope.xmax = value;
			} else {
				if (ordinate != 0)
					m_envelope.ymin = value;
				else
					m_envelope.xmin = value;
			}
			
			return;
		}

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		if (!hasAttribute(semantics)) {
			if (VertexDescription.isDefaultValue(semantics, value))
				return;
			
			addAttribute(semantics);
		}

		int attributeIndex = m_description.getAttributeIndex(semantics);
		_ensureAttributes();
		m_attributes[_getEndPointOffset(m_description, endPoint)
				+ m_description._getPointAttributeOffset(attributeIndex) - 2
				+ ordinate] = value;
	}

	int _getAttributeAsInt(int endPoint, int semantics, int ordinate) {
		return (int) _getAttributeAsDbl(endPoint, semantics, ordinate);
	}

	static int _getEndPointOffset(VertexDescription vd, int endPoint) {
		return endPoint * (vd.getTotalComponentCount() - 2);
	}

	public boolean isIntersecting(Envelope2D other) {
		return m_envelope.isIntersecting(other);
	}

	/**
	 * Changes this envelope to be the intersection of itself with the other
	 * envelope.
	 * 
	 * @param other
	 *            The envelope to intersect.
	 * @return Returns true if the result is not empty.
	 */
	public boolean intersect(Envelope other) {
		_touch();
		Envelope2D e2d = new Envelope2D();
		other.queryEnvelope2D(e2d);
		return m_envelope.intersect(e2d);
	}

	/**
	 * Returns true if the envelope and the other given envelope intersect.
	 * 
	 * @param other
	 *            The envelope to with which to test intersection.
	 * @return Returns true if the two envelopes intersect.
	 */
	public boolean isIntersecting(Envelope other) {// TODO: attributes.
		return m_envelope.isIntersecting(other.m_envelope);
	}

	/**
	 * Sets the envelope's corners to be centered around the specified point,
	 * using its center, width, and height.
	 * 
	 * @param c
	 *            The point around which to center the envelope.
	 * @param w
	 *            The width to be set for the envelope.
	 * @param h
	 *            The height to be set for this envelope.
	 */
	public void centerAt(Point c, double w, double h) {
		_touch();
		if (c.isEmpty()) {
			setEmpty();
			return;
		}

		_setFromPoint(c, w, h);
	}

	/**
	 * Offsets the envelope by the specified distances along x and y-coordinates.
	 * 
	 * @param dx
	 *            The X offset to be applied.
	 * @param dy
	 *            The Y offset to be applied.
	 */
	public void offset(double dx, double dy) {
		_touch();
		m_envelope.offset(dx, dy);
	}

	/**
	 * Normalizes envelopes if the minimum dimension is larger than the
	 * maximum dimension.
	 */
	public void normalize() {// TODO: attributes
		_touch();
		m_envelope.normalize();
	}

	/**
	 * Gets the center point of the envelope. The center point occurs at: ((XMin
	 * + XMax) / 2, (YMin + YMax) / 2).
	 * 
	 * @return The center point of the envelope.
	 */
	public Point2D getCenter2D() {
		return m_envelope.getCenter();
	}

	/**
	 * Returns the center point of the envelope.
	 * 
	 * @return The center point of the envelope.
	 */
	public Point getCenter() {
		Point pointOut = new Point(m_description);
		if (isEmpty()) {
			return pointOut;
		}
		int nattrib = m_description.getAttributeCount();
		for (int i = 1; i < nattrib; i++) {
			int semantics = m_description._getSemanticsImpl(i);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomp; iord++) {
				double v = 0.5 * (_getAttributeAsDbl(0, semantics, iord) + _getAttributeAsDbl(
						1, semantics, iord));
				pointOut.setAttribute(semantics, iord, v);
			}
		}
		pointOut.setXY(m_envelope.getCenterX(), m_envelope.getCenterY());
		return pointOut;
	}

	/**
	 * Centers the envelope around the specified point preserving the envelope's
	 * width and height.
	 * 
	 * @param c
	 *            The new center point.
	 */
	public void centerAt(Point c) {
		_touch();
		if (c.isEmpty()) {
			setEmpty();
			return;
		}
		m_envelope.centerAt(c.getX(), c.getY());
	}

	/**
	 * Returns the envelope's lower left corner point.
	 * 
	 * @return Returns the lower left corner point.
	 */
	public Point getLowerLeft() {
		return new Point(m_envelope.getLowerLeft());
	}

	/**
	 * Returns the envelope's upper right corner point.
	 * 
	 * @return Returns the upper right corner point.
	 */
	public Point getUpperRight() {
		return new Point(m_envelope.getUpperRight());
	}

	/**
	 * Returns the envelope's lower right corner point.
	 * 
	 * @return Returns the lower right corner point.
	 */
	public Point getLowerRight() {
		return new Point(m_envelope.getLowerRight());
	}

	/**
	 * Returns the envelope's upper left corner point.
	 * 
	 * @return Returns the upper left corner point.
	 */
	public Point getUpperLeft() {
		return new Point(m_envelope.getUpperLeft());
	}

	/**
	 * Checks if this envelope contains (covers) the specified point.
	 * 
	 * @param p
	 *            The Point to be tested for coverage.
	 * @return TRUE if this envelope contains (covers) the specified point.
	 */
	public boolean contains(Point p) {
		if (p.isEmpty())
			return false;
		return m_envelope.contains(p.getX(), p.getY());
	}

	/**
	 * Checks if this envelope contains (covers) other envelope.
	 * 
	 * @param env
	 *            The envelope to be tested for coverage.
	 * @return TRUE if this envelope contains (covers) the specified envelope.
	 */
	public boolean contains(Envelope env) {
		return m_envelope.contains(env.m_envelope);
	}

	/**
	 * Returns TRUE when this geometry has exactly same type, properties, and
	 * coordinates as the other geometry.
	 */
	@Override
	public boolean equals(Object _other) {
		if (_other == this)
			return true;

		if (!(_other instanceof Envelope))
			return false;

		Envelope other = (Envelope) _other;

		if (m_description != other.m_description)
			return false;

		if (isEmpty())
			if (other.isEmpty())
				return true;
			else
				return false;

		if (!this.m_envelope.equals(other.m_envelope))
			return false;

		for (int i = 0, n = (m_description.getTotalComponentCount() - 2) * 2; i < n; i++)
			if (m_attributes[i] != other.m_attributes[i])
				return false;

		return true;
	}

	/**
	 * Returns a hash code value for this envelope.
	 * 
	 * @return A hash code value for this envelope.
	 */
	@Override
	public int hashCode() {
		int hashCode = m_description.hashCode();
		hashCode = NumberUtils.hash(hashCode, m_envelope.hashCode());
		if (!isEmpty() && m_attributes != null) {
			for (int i = 0, n = (m_description.getTotalComponentCount() - 2) * 2; i < n; i++) {
				hashCode = NumberUtils.hash(hashCode, m_attributes[i]);
			}
		}
		return hashCode;
	}

	/**
	 * Returns the X coordinate of the left corners of the envelope.
	 * 
	 * @return The X coordinate of the left corners.
	 */
	public final double getXMin() {
		return m_envelope.xmin;
	}

	/**
	 * Returns the Y coordinate of the bottom corners of the envelope.
	 * 
	 * @return The Y coordinate of the bottom corners.
	 */
	public final double getYMin() {
		return m_envelope.ymin;
	}

	/**
	 * Returns the X coordinate of the right corners of the envelope.
	 * 
	 * @return The X coordinate of the right corners.
	 */
	public final double getXMax() {
		return m_envelope.xmax;
	}

	/**
	 * Returns the Y coordinate of the top corners of the envelope.
	 * 
	 * @return The Y coordinate of the top corners.
	 */
	public final double getYMax() {
		return m_envelope.ymax;
	}

	/**
	 * Sets the left X coordinate.
	 * 
	 * @param x
	 *            The X coordinate of the left corner
	 */
	public void setXMin(double x) {
		_touch();
		m_envelope.xmin = x;
	}

	/**
	 * Sets the right X coordinate.
	 * 
	 * @param x
	 *            The X coordinate of the right corner.
	 */
	public void setXMax(double x) {
		_touch();
		m_envelope.xmax = x;
	}

	/**
	 * Sets the bottom Y coordinate.
	 * 
	 * @param y
	 *            the Y coordinate of the bottom corner.
	 */
	public void setYMin(double y) {
		_touch();
		m_envelope.ymin = y;
	}

	/**
	 * Sets the top Y coordinate.
	 * 
	 * @param y
	 *            The Y coordinate of the top corner.
	 */
	public void setYMax(double y) {
		_touch();
		m_envelope.ymax = y;
	}

	@Override
	public Geometry getBoundary() {
		return Boundary.calculate(this, null);
	}

	@Override
	public void replaceNaNs(int semantics, double value) {
		addAttribute(semantics);
		if (isEmpty())
			return;

		int ncomps = VertexDescription.getComponentCount(semantics);
		for (int i = 0; i < ncomps; i++) {
			Envelope1D interval = queryInterval(semantics, i);
			if (interval.isEmpty()) {
				interval.vmin = value;
				interval.vmax = value;
				setInterval(semantics, i, interval);
			}
		}
	}

	/**
	 * The output of this method can be only used for debugging. It is subject to change without notice. 
	 */
	@Override
	public String toString() {
		if (isEmpty())
			return "Envelope: []";
		
		String s = "Envelope: [" + m_envelope.xmin + ", " + m_envelope.ymin + ", " + m_envelope.xmax + ", " + m_envelope.ymax +"]"; 
		return s;
	}
    
}
