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

import java.io.Serializable;

/**
 * A base class for segments. Presently only Line segments are supported.
 */
public abstract class Segment extends Geometry implements Serializable {
	double m_xStart;

	double m_yStart;

	double m_xEnd;

	double m_yEnd;

	double[] m_attributes;

	// Header Definitions
	/**
	 * Returns XY coordinates of the start point.
	 */
	public Point2D getStartXY() {
		return Point2D.construct(m_xStart, m_yStart);
	}

	public void getStartXY(Point2D pt) {
		pt.x = m_xStart;
		pt.y = m_yStart;
	}

	/**
	 * Sets the XY coordinates of the start point.
	 */
	public void setStartXY(Point2D pt) {
		_setXY(0, pt);
	}

	public void setStartXY(double x, double y) {
		_setXY(0, Point2D.construct(x, y));
	}

	/**
	 * Returns XYZ coordinates of the start point. Z if 0 if Z is missing.
	 */
	public Point3D getStartXYZ() {
		return _getXYZ(0);
	}

	/**
	 * Sets the XYZ coordinates of the start point.
	 */
	public void setStartXYZ(Point3D pt) {
		_setXYZ(0, pt);
	}

	public void setStartXYZ(double x, double y, double z) {
		_setXYZ(0, Point3D.construct(x, y, z));
	}

	/**
	 * Returns coordinates of the start point in a Point class.
	 */
	public void queryStart(Point dstPoint) {
		_get(0, dstPoint);
	}

	/**
	 * Sets the coordinates of the start point in this segment.
	 * 
	 * @param srcPoint
	 *            The new start point of this segment.
	 */
	public void setStart(Point srcPoint) {
		_set(0, srcPoint);
	}

	/**
	 * Returns value of the start vertex attribute's ordinate. Throws if the
	 * Point is empty.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate. For example, the y coordinate of the
	 *            NORMAL has ordinate of 1.
	 * @return Ordinate value as double.
	 */
	public double getStartAttributeAsDbl(int semantics, int ordinate) {
		return _getAttributeAsDbl(0, semantics, ordinate);
	}

	/**
	 * Returns the value of the start vertex attribute's ordinate. The ordinate
	 * is always 0 because integer attributes always have one component.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate. For example, the y coordinate of the
	 *            NORMAL has ordinate of 1.
	 * @return Ordinate value truncated to 32 bit integer.
	 */
	public int getStartAttributeAsInt(int semantics, int ordinate) {
		return _getAttributeAsInt(0, semantics, ordinate);
	}

	/**
	 * Sets the value of the start vertex attribute.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param value
	 *            is the array to write values to. The attribute type and the
	 *            number of elements must match the persistence type, as well as
	 *            the number of components of the attribute.
	 */
	public void setStartAttribute(int semantics, int ordinate, double value) {
		_setAttribute(0, semantics, ordinate, value);
	}

	public void setStartAttribute(int semantics, int ordinate, int value) {
		_setAttribute(0, semantics, ordinate, value);
	}

	/**
	 * Returns the X coordinate of starting point.
	 * 
	 * @return The X coordinate of starting point.
	 */
	public double getStartX() {
		return m_xStart;
	}

	/**
	 * Returns the Y coordinate of starting point.
	 * 
	 * @return The Y coordinate of starting point.
	 */
	public double getStartY() {
		return m_yStart;
	}

	/**
	 * Returns the X coordinate of ending point.
	 * 
	 * @return The X coordinate of ending point.
	 */
	public double getEndX() {
		return m_xEnd;
	}

	/**
	 * Returns the Y coordinate of ending point.
	 * 
	 * @return The Y coordinate of ending point.
	 */
	public double getEndY() {
		return m_yEnd;
	}

	/**
	 * Returns XY coordinates of the end point.
	 * 
	 * @return The XY coordinates of the end point.
	 */
	public Point2D getEndXY() {
		return Point2D.construct(m_xEnd, m_yEnd);
	}

	public void getEndXY(Point2D pt) {
		pt.x = m_xEnd;
		pt.y = m_yEnd;
	}

	/**
	 * Sets the XY coordinates of the end point.
	 * 
	 * @param pt
	 *            The end point of the segment.
	 */
	public void setEndXY(Point2D pt) {
		_setXY(1, pt);
	}

	public void setEndXY(double x, double y) {
		_setXY(1, Point2D.construct(x, y));
	}

	/**
	 * Returns XYZ coordinates of the end point. Z if 0 if Z is missing.
	 * 
	 * @return The XYZ coordinates of the end point.
	 */
	public Point3D getEndXYZ() {
		return _getXYZ(1);
	}

	/**
	 * Sets the XYZ coordinates of the end point.
	 */
	public void setEndXYZ(Point3D pt) {
		_setXYZ(1, pt);
	}

	public void setEndXYZ(double x, double y, double z) {
		_setXYZ(1, Point3D.construct(x, y, z));
	}

	/**
	 * Returns coordinates of the end point in this segment.
	 * 
	 * @param dstPoint
	 *            The end point of this segment.
	 */
	public void queryEnd(Point dstPoint) {
		_get(1, dstPoint);
	}

	/**
	 * Sets the coordinates of the end point in a Point class.
	 * 
	 * @param srcPoint
	 *            The new end point of this segment.
	 */
	public void setEnd(Point srcPoint) {
		_set(1, srcPoint);
	}

	/**
	 * Returns value of the end vertex attribute's ordinate. Throws if the Point
	 * is empty.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate. For example, the y coordinate of the
	 *            NORMAL has ordinate of 1.
	 * @return Ordinate value as double.
	 */
	public double getEndAttributeAsDbl(int semantics, int ordinate) {
		return _getAttributeAsDbl(1, semantics, ordinate);
	}

	/**
	 * Returns the value of the end vertex attribute's ordinate. The ordinate is
	 * always 0 because integer attributes always have one component.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate. For example, the y coordinate of the
	 *            NORMAL has ordinate of 1.
	 * @return The ordinate value truncated to 32 bit integer.
	 */
	public int getEndAttributeAsInt(int semantics, int ordinate) {
		return _getAttributeAsInt(1, semantics, ordinate);
	}

	/**
	 * Sets the value of end vertex attribute.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate.
	 * @param value
	 *            Is the array to write values to. The attribute type and the
	 *            number of elements must match the persistence type, as well as
	 *            the number of components of the attribute.
	 */
	public void setEndAttribute(int semantics, int ordinate, double value) {
		_setAttribute(1, semantics, ordinate, value);
	}

	public void setEndAttribute(int semantics, int ordinate, int value) {
		_setAttribute(1, semantics, ordinate, value);
	}

	@Override
	public final int getDimension() {
		return 1;
	}

	@Override
	public final boolean isEmpty() {
		return isEmptyImpl();
	}

	@Override
	public final void setEmpty() {

	}

	@Override
	public double calculateArea2D() {
		return 0;
	}

	/**
	 * Calculates intersections of this segment with another segment.
	 * <p>
	 * Note: This is not a topological operation. It needs to be paired with the
	 * Segment.Overlap call.
	 * 
	 * @param other
	 *            The segment to calculate intersection with.
	 * @param intersectionPoints
	 *            The intersection points. Can be NULL.
	 * @param paramThis
	 *            The value of the parameter in the intersection points for this
	 *            Segment (between 0 and 1). Can be NULL.
	 * @param paramOther
	 *            The value of the parameter in the intersection points for the
	 *            other Segment (between 0 and 1). Can be NULL.
	 * @param tolerance
	 *            The tolerance value for the intersection calculation. Can be
	 *            0.
	 * @return The number of intersection points, 0 when no intersection points
	 *         exist.
	 */
	int intersect(Segment other, Point2D[] intersectionPoints,
			double[] paramThis, double[] paramOther, double tolerance) {
		return _intersect(other, intersectionPoints, paramThis, paramOther,
				tolerance);
	}

	/**
	 * Returns TRUE if this segment intersects with the other segment with the
	 * given tolerance.
	 */
	public boolean isIntersecting(Segment other, double tolerance) {
		return _isIntersecting(other, tolerance, false) != 0;
	}

	/**
	 * Returns TRUE if the point and segment intersect (not disjoint) for the
	 * given tolerance.
	 */
	public boolean isIntersecting(Point2D pt, double tolerance) {
		return _isIntersectingPoint(pt, tolerance, false);
	}

	/**
	 * Non public abstract version of the function.
	 */
	public boolean isEmptyImpl() {
		return false;
	}

	// Header Definitions

	// Cpp definitions
	/**
	 * Creates a segment with start and end points (0,0).
	 */
	public Segment() {
		m_xStart = 0;
		m_yStart = 0;
		m_xEnd = 0;
		m_yEnd = 0;
		m_attributes = null;
	}

	void _resizeAttributes(int newSize) {
		_touch();
		if (m_attributes == null && newSize > 0) {
			m_attributes = new double[newSize * 2];
		} else if (m_attributes != null && m_attributes.length < newSize * 2) {
			double[] newbuffer = new double[newSize * 2];
			System.arraycopy(m_attributes, 0, newbuffer, 0, m_attributes.length);
			m_attributes = newbuffer;
		}
	}

	static void _attributeCopy(double[] src, int srcStart, double[] dst,
			int dstStart, int count) {
		if (count > 0)
			System.arraycopy(src, srcStart, dst, dstStart, count);
	}

	private Point2D _getXY(int endPoint) {
		Point2D pt = new Point2D();
		if (endPoint != 0) {
			pt.setCoords(m_xEnd, m_yEnd);
		} else {
			pt.setCoords(m_xStart, m_yStart);
		}
		return pt;
	}

	private void _setXY(int endPoint, Point2D pt) {
		if (endPoint != 0) {
			m_xEnd = pt.x;
			m_yEnd = pt.y;
		} else {
			m_xStart = pt.x;
			m_yStart = pt.y;
		}
	}

	private Point3D _getXYZ(int endPoint) {
		Point3D pt = new Point3D();
		if (endPoint != 0) {
			pt.x = m_xEnd;
			pt.y = m_yEnd;
		} else {
			pt.x = m_xStart;
			pt.y = m_yStart;
		}

		if (m_description.hasZ())
			pt.z = m_attributes[_getEndPointOffset(m_description, endPoint)];
		else
			pt.z = VertexDescription.getDefaultValue(Semantics.Z);

		return pt;
	}

	private void _setXYZ(int endPoint, Point3D pt) {
		_touch();
		boolean bHasZ = hasAttribute(Semantics.Z);
		if (!bHasZ && !VertexDescription.isDefaultValue(Semantics.Z, pt.z)) {// add
																				// Z
																				// only
																				// if
																				// pt.z
																				// is
																				// not
																				// a
																				// default
																				// value.
			addAttribute(Semantics.Z);
			bHasZ = true;
		}

		if (endPoint != 0) {
			m_xEnd = pt.x;
			m_yEnd = pt.y;
		} else {
			m_xStart = pt.x;
			m_yStart = pt.y;
		}

		if (bHasZ)
			m_attributes[_getEndPointOffset(m_description, endPoint)] = pt.z;

	}

	@Override
	protected void _assignVertexDescriptionImpl(VertexDescription newDescription) {
		if (m_attributes == null) {
			m_description = newDescription;
			return;
		}
		
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
		m_description = newDescription;
	}

	private void _get(int endPoint, Point outPoint) {
		if (isEmptyImpl())
			throw new GeometryException("empty geometry");// ._setToDefault();

		outPoint.assignVertexDescription(m_description);

		if (outPoint.isEmptyImpl())
			outPoint._setToDefault();

		for (int attributeIndex = 0; attributeIndex < m_description
				.getAttributeCount(); attributeIndex++) {
			int semantics = m_description._getSemanticsImpl(attributeIndex);
			for (int icomp = 0, ncomp = VertexDescription
					.getComponentCount(semantics); icomp < ncomp; icomp++) {
				double v = _getAttributeAsDbl(endPoint, semantics, icomp);
				outPoint.setAttribute(semantics, icomp, v);
			}
		}
	}

	private void _set(int endPoint, Point src) {
		_touch();
		Point point = src;

		if (src.isEmptyImpl())// can not assign an empty point
			throw new GeometryException("empty_Geometry");

		VertexDescription vdin = point.getDescription();
		for (int attributeIndex = 0, nattrib = vdin.getAttributeCount(); attributeIndex < nattrib; attributeIndex++) {
			int semantics = vdin._getSemanticsImpl(attributeIndex);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int icomp = 0; icomp < ncomp; icomp++) {
				double v = point.getAttributeAsDbl(semantics, icomp);
				_setAttribute(endPoint, semantics, icomp, v);
			}
		}
	}

	double _getAttributeAsDbl(int endPoint, int semantics, int ordinate) {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation was performed on an Empty Geometry.");

		if (semantics == Semantics.POSITION) {
			if (endPoint != 0) {
				return (ordinate != 0) ? m_yEnd : m_xEnd;
			} else {
				return (ordinate != 0) ? m_yStart : m_xStart;
			}
		}

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (attributeIndex >= 0) {
			if (m_attributes != null)
				_resizeAttributes(m_description.getTotalComponentCount() - 2);

			return m_attributes[_getEndPointOffset(m_description, endPoint)
					+ m_description._getPointAttributeOffset(attributeIndex)
					- 2 + ordinate];
		} else
			return VertexDescription.getDefaultValue(semantics);
	}

	private int _getAttributeAsInt(int endPoint, int semantics, int ordinate) {
		if (isEmptyImpl())
			throw new GeometryException("Empty_Geometry.");

		return (int) _getAttributeAsDbl(endPoint, semantics, ordinate);
	}

	void _setAttribute(int endPoint, int semantics, int ordinate, double value) {
		_touch();
		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (attributeIndex < 0) {
			addAttribute(semantics);
			attributeIndex = m_description.getAttributeIndex(semantics);
		}

		if (semantics == Semantics.POSITION) {
			if (endPoint != 0) {
				if (ordinate != 0)
					m_yEnd = value;
				else
					m_xEnd = value;
			}
			else if (ordinate != 0)
				m_yStart = value;
			else
				m_xStart = value;
			return;
		}

		if (m_attributes == null)
			_resizeAttributes(m_description.getTotalComponentCount() - 2);

		m_attributes[_getEndPointOffset(m_description, endPoint)
				+ m_description._getPointAttributeOffset(attributeIndex) - 2
				+ ordinate] = value;

	}

	void _setAttribute(int endPoint, int semantics, int ordinate, int value) {
		_setAttribute(endPoint, semantics, ordinate, (double) value);
	}

	@Override
	public void copyTo(Geometry dst) {
		if (dst.getType() != getType())
			throw new IllegalArgumentException();

		Segment segDst = (Segment) dst;
		segDst.m_description = m_description;
		segDst._resizeAttributes(m_description.getTotalComponentCount() - 2);
		_attributeCopy(m_attributes, 0, segDst.m_attributes, 0,
				(m_description.getTotalComponentCount() - 2) * 2);
		segDst.m_xStart = m_xStart;
		segDst.m_yStart = m_yStart;
		segDst.m_xEnd = m_xEnd;
		segDst.m_yEnd = m_yEnd;
		dst._touch();

		_copyToImpl(segDst);
	}

	@Override
	public Envelope1D queryInterval(int semantics, int ordinate) {
		Envelope1D env = new Envelope1D();
		if (isEmptyImpl()) {
			env.setEmpty();
			return env;
		}

		env.vmin = _getAttributeAsDbl(0, semantics, ordinate);
		env.vmax = env.vmin;
		env.mergeNE(_getAttributeAsDbl(1, semantics, ordinate));
		return env;
	}

	void queryCoord(double t, Point point) {
		point.assignVertexDescription(m_description);
		point.setXY(getCoord2D(t));
		for (int iattrib = 1, nattrib = m_description.getAttributeCount(); iattrib < nattrib; iattrib++) {
			int semantics = m_description._getSemanticsImpl(iattrib);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int iord = 0; iord < ncomp; iord++) {
				double value = getAttributeAsDbl(t, semantics, iord);
				point.setAttribute(semantics, iord, value);
			}
		}
	}

	boolean _equalsImpl(Segment other) {
		if (m_description != other.m_description)
			return false;

		if (m_xStart != other.m_xStart || m_xEnd != other.m_xEnd
				|| m_yStart != other.m_yStart || m_yEnd != other.m_yEnd)
			return false;
		for (int i = 0; i < (m_description.getTotalComponentCount() - 2) * 2; i++)
			if (m_attributes[i] != other.m_attributes[i])
				return false;

		return true;
	}

	/**
	 * Returns true, when this segment is a closed curve (start point is equal
	 * to end point exactly).
	 * 
	 * Note, this will return true for lines, that are degenerate to a point
	 * too.
	 */
	boolean isClosed() {
		return m_xStart == m_xEnd && m_yStart == m_yEnd;
	}

	void reverse() {
		_reverseImpl();
		double origxStart = m_xStart;
		double origxEnd = m_xEnd;
		m_xStart = origxEnd;
		m_xEnd = origxStart;
		double origyStart = m_yStart;
		double origyEnd = m_yEnd;
		m_yStart = origyEnd;
		m_yEnd = origyStart;

		for (int i = 1, n = m_description.getAttributeCount(); i < n; i++) {
			int semantics = m_description.getSemantics(i);// VertexDescription.Semantics
															// semantics =
															// m_description.getSemantics(i);
			for (int iord = 0, nord = VertexDescription
					.getComponentCount(semantics); iord < nord; iord++) {
				double v1 = _getAttributeAsDbl(0, semantics, iord);
				double v2 = _getAttributeAsDbl(1, semantics, iord);
				_setAttribute(0, semantics, iord, v2);
				_setAttribute(1, semantics, iord, v1);
			}
		}
	}

	int _isIntersecting(Segment other, double tolerance,
			boolean bExcludeExactEndpoints) {
		int gtThis = getType().value();
		int gtOther = other.getType().value();
		switch (gtThis) {
		case Geometry.GeometryType.Line:
			if (gtOther == Geometry.GeometryType.Line)
				return Line._isIntersectingLineLine((Line) this, (Line) other,
						tolerance, bExcludeExactEndpoints);
			else
				throw GeometryException.GeometryInternalError();
		default:
			throw GeometryException.GeometryInternalError();
		}
	}

	int _intersect(Segment other, Point2D[] intersectionPoints,
			double[] paramThis, double[] paramOther, double tolerance) {
		int gtThis = getType().value();
		int gtOther = other.getType().value();
		switch (gtThis) {
		case Geometry.GeometryType.Line:
			if (gtOther == Geometry.GeometryType.Line)
				return Line._intersectLineLine((Line) this, (Line) other,
						intersectionPoints, paramThis, paramOther, tolerance);
			else
				throw GeometryException.GeometryInternalError();
		default:
			throw GeometryException.GeometryInternalError();
		}
	}

	/**
	 * A helper function for area calculation. Calculates the Integral(y(t) *
	 * x'(t) * dt) for t = [0, 1]. The area of a ring is caluclated as a sum of
	 * the results of CalculateArea2DHelper.
	 */
	abstract double _calculateArea2DHelper(double xorg, double yorg);

	static int _getEndPointOffset(VertexDescription vd, int endPoint) {
		return endPoint * (vd.getTotalComponentCount() - 2);
	}

	/**
	 * Returns the coordinate of the point on this segment for the given
	 * parameter value.
	 */
	public Point2D getCoord2D(double t) {
		Point2D pt = new Point2D();
		getCoord2D(t, pt);
		return pt;
	}

	/**
	 * Returns the coordinate of the point on this segment for the given
	 * parameter value (segments are parametric curves).
	 * 
	 * @param t
	 *            the parameter coordinate along the segment from 0.0 to 1.0.
	 *            Value of 0 returns the start point, 1 returns end point.
	 * @param dst
	 *            the coordinate where result will be placed.
	 */
	public abstract void getCoord2D(double t, Point2D dst);

	/**
	 * Finds a closest coordinate on this segment.
	 * 
	 * @param inputPoint
	 *            The 2D point to find the closest coordinate on this segment.
	 * @param bExtrapolate
	 *            TRUE if the segment is extrapolated at the end points along
	 *            the end point tangents. Otherwise the result is limited to
	 *            values between 0 and 1.
	 * @return The parametric coordinate t on the segment (0 corresponds to the
	 *         start point, 1 corresponds to the end point). Use getCoord2D to
	 *         obtain the 2D coordinate on the segment from t. To find the
	 *         distance, call (inputPoint.sub(seg.getCoord2D(t))).length();
	 */
	public abstract double getClosestCoordinate(Point2D inputPoint,
			boolean bExtrapolate);

	/**
	 * Splits this segment into Y monotonic parts and places them into the input
	 * array.
	 * 
	 * @param monotonicSegments
	 *            The in/out array of SegmentBuffer structures that will be
	 *            filled with the monotonic parts. The monotonicSegments array
	 *            must contain at least 3 elements.
	 * @return The number of monotonic parts if the split had happened. Returns
	 *         0 if the segment is already monotonic.
	 */
	abstract int getYMonotonicParts(SegmentBuffer[] monotonicSegments);

	/**
	 * Calculates intersection points of this segment with an infinite line,
	 * parallel to one of the axes.
	 * 
	 * @param bAxisX
	 *            TRUE if the function works with the line parallel to the axis
	 *            X.
	 * @param ordinate
	 *            The ordinate value of the line (x for axis Y, y for axis X).
	 * @param resultOrdinates
	 *            The value of ordinate in the intersection points One ordinate
	 *            is equal to the ordinate parameter. This parameter can be
	 *            NULL.
	 * @param parameters
	 *            The value of the parameter in the intersection points (between
	 *            0 and 1). This parameter can be NULL.
	 * @return The number of intersection points, 0 when no intersection points
	 *         exist, -1 when the segment coincides with the line (infinite
	 *         number of intersection points).
	 */
	public abstract int intersectionWithAxis2D(boolean bAxisX, double ordinate,
			double[] resultOrdinates, double[] parameters);

	void _reverseImpl() {
	}

	/**
	 * Returns True if the segment is degenerate to a point with relation to the
	 * given tolerance. For Lines this means the line length is not longer than
	 * the tolerance. For the curves, the distance between the segment endpoints
	 * should not be longer than the tolerance and the distance from the line,
	 * connecting the endpoints to the furtherst point on the segment is not
	 * larger than the tolerance.
	 */
	abstract boolean isDegenerate(double tolerance);

	// Cpp definitions

	abstract boolean isCurve();

	abstract Point2D _getTangent(double t);

	abstract boolean _isDegenerate(double tolerance);

	double _calculateSubLength(double t) { return tToLength(t); }
	
	double _calculateSubLength(double t1, double t2) { return tToLength(t2) - tToLength(t1); }

	abstract void _copyToImpl(Segment dst);

	/**
	 * Returns subsegment between parameters t1 and t2. The attributes are
	 * interpolated along the length of the curve.
	 */
	public abstract Segment cut(double t1, double t2);

	/**
	 * Calculates the subsegment between parameters t1 and t2, and stores the
	 * result in subSegmentBuffer. The attributes are interpolated along the
	 * length of the curve.
	 */
	abstract void cut(double t1, double t2, SegmentBuffer subSegmentBuffer);

	/**
	 * Returns the attribute on the segment for the given parameter value. The
	 * interpolation of attribute is given by the attribute interpolation type.
	 */
	public abstract double getAttributeAsDbl(double t, int semantics,
			int ordinate);

	abstract boolean _isIntersectingPoint(Point2D pt, double tolerance,
			boolean bExcludeExactEndpoints);

	/**
	 * Calculates intersection point of this segment with an infinite line,
	 * parallel to axis X. This segment must be to be y-monotonic (or
	 * horizontal).
	 * 
	 * @param y
	 *            The y coordinate of the line.
	 * @param xParallel
	 *            For segments, that are horizontal, and have y coordinate, this
	 *            value is returned.
	 * @return X coordinate of the intersection, or NaN, if no intersection.
	 */
	abstract double intersectionOfYMonotonicWithAxisX(double y, double xParallel);
  
	/**
	 * Converts curves parameter t to the curve length. Can be expensive for curves.
	 */
	abstract double tToLength(double t);

	abstract double lengthToT(double len);

	public double distance(/* const */Segment otherSegment,
			boolean bSegmentsKnownDisjoint)
	{
		// if the segments are not known to be disjoint, and
		// the segments are found to touch in any way, then return 0.0
		if (!bSegmentsKnownDisjoint
				&& _isIntersecting(otherSegment, 0, false) != 0) {
			return 0.0;
		}

		double minDistance = NumberUtils.doubleMax();

		Point2D input_point;
		double t;
		double distance;

		input_point = getStartXY();
		t = otherSegment.getClosestCoordinate(input_point, false);
		input_point.sub(otherSegment.getCoord2D(t));
		distance = input_point.length();
		if (distance < minDistance)
			minDistance = distance;

		input_point = getEndXY();
		t = otherSegment.getClosestCoordinate(input_point, false);
		input_point.sub(otherSegment.getCoord2D(t));
		distance = input_point.length();
		if (distance < minDistance)
			minDistance = distance;

		input_point = otherSegment.getStartXY();
		t = getClosestCoordinate(input_point, false);
		input_point.sub(getCoord2D(t));
		distance = input_point.length();
		if (distance < minDistance)
			minDistance = distance;

		input_point = otherSegment.getEndXY();
		t = getClosestCoordinate(input_point, false);
		input_point.sub(getCoord2D(t));
		distance = input_point.length();
		if (distance < minDistance)
			minDistance = distance;

		return minDistance;
	}    

    public Geometry getBoundary() {
        return Boundary.calculate(this, null);
    }


}
