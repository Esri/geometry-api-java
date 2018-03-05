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

import static com.esri.core.geometry.SizeOf.SIZE_OF_POINT;

/**
 * A Point is a zero-dimensional object that represents a specific (X,Y)
 * location in a two-dimensional XY-Plane. In case of Geographic Coordinate
 * Systems, the X coordinate is the longitude and the Y is the latitude.
 */
public class Point extends Geometry implements Serializable {
	//We are using writeReplace instead.
	//private static final long serialVersionUID = 2L;

	double[] m_attributes; // use doubles to store everything (long are bitcast)

	/**
	 * Creates an empty 2D point.
	 */
	public Point() {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
	}

	public Point(VertexDescription vd) {
		if (vd == null)
			throw new IllegalArgumentException();
		m_description = vd;
	}

	/**
	 * Creates a 2D Point with specified X and Y coordinates. In case of
	 * Geographic Coordinate Systems, the X coordinate is the longitude and the
	 * Y is the latitude.
	 * 
	 * @param x
	 *            The X coordinate of the new 2D point.
	 * @param y
	 *            The Y coordinate of the new 2D point.
	 */
	public Point(double x, double y) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		setXY(x, y);
	}
	public Point(Point2D pt) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		setXY(pt);
	}

	/**
	 * Creates a 3D point with specified X, Y and Z coordinates. In case of
	 * Geographic Coordinate Systems, the X coordinate is the longitude and the
	 * Y is the latitude.
	 * 
	 * @param x
	 *            The X coordinate of the new 3D point.
	 * @param y
	 *            The Y coordinate of the new 3D point.
	 * @param z
	 *            The Z coordinate of the new 3D point.
	 * 
	 */
	public Point(double x, double y, double z) {
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		Point3D pt = new Point3D();
		pt.setCoords(x, y, z);
		setXYZ(pt);

	}

	/**
	 * Returns XY coordinates of this point.
	 */
	public final Point2D getXY() {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation should not be performed on an empty geometry.");

		Point2D pt = new Point2D();
		pt.setCoords(m_attributes[0], m_attributes[1]);
		return pt;
	}

	/**
	 * Returns XY coordinates of this point.
	 */
	public final void getXY(Point2D pt) {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation should not be performed on an empty geometry.");

		pt.setCoords(m_attributes[0], m_attributes[1]);
	}
	
	/**
	 * Sets the XY coordinates of this point. param pt The point to create the X
	 * and Y coordinate from.
	 */
	public final void setXY(Point2D pt) {
		_touch();
		setXY(pt.x, pt.y);
	}

	/**
	 * Returns XYZ coordinates of the point. Z will be set to 0 if Z is missing.
	 */
	public Point3D getXYZ() {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation should not be performed on an empty geometry.");

		Point3D pt = new Point3D();
		pt.x = m_attributes[0];
		pt.y = m_attributes[1];
		if (m_description.hasZ())
			pt.z = m_attributes[2];
		else
			pt.z = VertexDescription.getDefaultValue(Semantics.Z);

		return pt;
	}

	/**
	 * Sets the XYZ coordinates of this point.
	 * 
	 * @param pt
	 *            The point to create the XYZ coordinate from.
	 */
	public void setXYZ(Point3D pt) {
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

		if (m_attributes == null)
			_setToDefault();

		m_attributes[0] = pt.x;
		m_attributes[1] = pt.y;
		if (bHasZ)
			m_attributes[2] = pt.z;
	}

	/**
	 * Returns the X coordinate of the point.
	 */
	public final double getX() {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation should not be performed on an empty geometry.");

		return m_attributes[0];
	}

	/**
	 * Sets the X coordinate of the point.
	 * 
	 * @param x
	 *            The X coordinate to be set for this point.
	 */
	public void setX(double x) {
		setAttribute(Semantics.POSITION, 0, x);
	}

	/**
	 * Returns the Y coordinate of this point.
	 */
	public final double getY() {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation should not be performed on an empty geometry.");

		return m_attributes[1];
	}

	/**
	 * Sets the Y coordinate of this point.
	 * 
	 * @param y
	 *            The Y coordinate to be set for this point.
	 */
	public void setY(double y) {
		setAttribute(Semantics.POSITION, 1, y);
	}

	/**
	 * Returns the Z coordinate of this point.
	 */
	public double getZ() {
		return getAttributeAsDbl(Semantics.Z, 0);
	}

	/**
	 * Sets the Z coordinate of this point.
	 * 
	 * @param z
	 *            The Z coordinate to be set for this point.
	 */
	public void setZ(double z) {
		setAttribute(Semantics.Z, 0, z);
	}

	/**
	 * Returns the attribute M of this point.
	 */
	public double getM() {
		return getAttributeAsDbl(Semantics.M, 0);
	}

	/**
	 * Sets the M coordinate of this point.
	 * 
	 * @param m
	 *            The M coordinate to be set for this point.
	 */
	public void setM(double m) {
		setAttribute(Semantics.M, 0, m);
	}

	/**
	 * Returns the ID of this point.
	 */
	public int getID() {
		return getAttributeAsInt(Semantics.ID, 0);
	}

	/**
	 * Sets the ID of this point.
	 * 
	 * @param id
	 *            The ID of this point.
	 */
	public void setID(int id) {
		setAttribute(Semantics.ID, 0, id);
	}

	/**
	 * Returns value of the given vertex attribute's ordinate.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate. For example, the Y coordinate of the
	 *            NORMAL has ordinate of 1.
	 * @return The ordinate as double value.
	 */
	public double getAttributeAsDbl(int semantics, int ordinate) {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation was performed on an Empty Geometry.");

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (attributeIndex >= 0)
			return m_attributes[m_description
					._getPointAttributeOffset(attributeIndex) + ordinate];
		else
			return VertexDescription.getDefaultValue(semantics);
	}

	/**
	 * Returns value of the given vertex attribute's ordinate. The ordinate is
	 * always 0 because integer attributes always have one component.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The attribute's ordinate. For example, the y coordinate of the
	 *            NORMAL has ordinate of 1.
	 * @return The ordinate value truncated to a 32 bit integer value.
	 */
	public int getAttributeAsInt(int semantics, int ordinate) {
		if (isEmptyImpl())
			throw new GeometryException(
					"This operation was performed on an Empty Geometry.");

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (attributeIndex >= 0)
			return (int) m_attributes[m_description
					._getPointAttributeOffset(attributeIndex) + ordinate];
		else
			return (int) VertexDescription.getDefaultValue(semantics);
	}

	/**
	 * Sets the value of the attribute.
	 * 
	 * @param semantics
	 *            The attribute semantics.
	 * @param ordinate
	 *            The ordinate of the attribute.
	 * @param value
	 *            Is the array to write values to. The attribute type and the
	 *            number of elements must match the persistence type, as well as
	 *            the number of components of the attribute.
	 */
	public void setAttribute(int semantics, int ordinate, double value) {
		_touch();
		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ncomps < ordinate)
			throw new IndexOutOfBoundsException();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (attributeIndex < 0) {
			addAttribute(semantics);
			attributeIndex = m_description.getAttributeIndex(semantics);
		}

		if (m_attributes == null)
			_setToDefault();

		m_attributes[m_description._getPointAttributeOffset(attributeIndex)
				+ ordinate] = value;
	}

	public void setAttribute(int semantics, int ordinate, int value) {
		setAttribute(semantics, ordinate, (double) value);
	}

	@Override
	public Geometry.Type getType() {
		return Type.Point;
	}

	@Override
	public int getDimension() {
		return 0;
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_POINT + estimateMemorySize(m_attributes);
	}

	@Override
	public void setEmpty() {
		_touch();
		if (m_attributes != null) {
			m_attributes[0] = NumberUtils.NaN();
			m_attributes[1] = NumberUtils.NaN();
		}
	}

	@Override
	protected void _assignVertexDescriptionImpl(VertexDescription newDescription) {
		if (m_attributes == null) {
			m_description = newDescription;
			return;
		}
		
		int[] mapping = VertexDescriptionDesignerImpl.mapAttributes(newDescription, m_description);
		
		double[] newAttributes = new double[newDescription.getTotalComponentCount()];
		
		int j = 0;
		for (int i = 0, n = newDescription.getAttributeCount(); i < n; i++) {
			int semantics = newDescription.getSemantics(i);
			int nords = VertexDescription.getComponentCount(semantics);
			if (mapping[i] == -1)
			{
				double d = VertexDescription.getDefaultValue(semantics);
				for (int ord = 0; ord < nords; ord++)
				{
					newAttributes[j] = d;
					j++;
				}
			}
			else {
				int m = mapping[i];
				int offset = m_description._getPointAttributeOffset(m);
				for (int ord = 0; ord < nords; ord++)
				{
					newAttributes[j] = m_attributes[offset];
					j++;
					offset++;
				}
			}
				 
		}
		
		m_attributes = newAttributes;
		m_description = newDescription;
	}

	/**
	 * Sets the Point to a default, non-empty state.
	 */
	void _setToDefault() {
		resizeAttributes(m_description.getTotalComponentCount());
		Point.attributeCopy(m_description._getDefaultPointAttributes(),
				m_attributes, m_description.getTotalComponentCount());
		m_attributes[0] = NumberUtils.NaN();
		m_attributes[1] = NumberUtils.NaN();
	}

	@Override
	public void applyTransformation(Transformation2D transform) {
		if (isEmptyImpl())
			return;

		Point2D pt = getXY();
		transform.transform(pt, pt);
		setXY(pt);
	}

	@Override
	void applyTransformation(Transformation3D transform) {
		if (isEmptyImpl())
			return;

		addAttribute(Semantics.Z);
		Point3D pt = getXYZ();
		setXYZ(transform.transform(pt));
	}

	@Override
	public void copyTo(Geometry dst) {
		if (dst.getType() != Type.Point)
			throw new IllegalArgumentException();

		Point pointDst = (Point) dst;
		dst._touch();

		if (m_attributes == null) {
			pointDst.setEmpty();
			pointDst.m_attributes = null;
			pointDst.assignVertexDescription(m_description);
		} else {
			pointDst.assignVertexDescription(m_description);
			pointDst.resizeAttributes(m_description.getTotalComponentCount());
			attributeCopy(m_attributes, pointDst.m_attributes,
					m_description.getTotalComponentCount());
		}
	}

	@Override
	public Geometry createInstance() {
		Point point = new Point(m_description);
		return point;
	}

	@Override
	public boolean isEmpty() {
		return isEmptyImpl();
	}

	final boolean isEmptyImpl() {
		return ((m_attributes == null) || NumberUtils.isNaN(m_attributes[0]) || NumberUtils
				.isNaN(m_attributes[1]));
	}

	@Override
	public void queryEnvelope(Envelope env) {
		env.setEmpty();
		if (m_description != env.m_description)
			env.assignVertexDescription(m_description);
		env.merge(this);
	}

	@Override
	public void queryEnvelope2D(Envelope2D env) {

		if (isEmptyImpl()) {
			env.setEmpty();
			return;
		}

		env.xmin = m_attributes[0];
		env.ymin = m_attributes[1];
		env.xmax = m_attributes[0];
		env.ymax = m_attributes[1];
	}

	@Override
	void queryEnvelope3D(Envelope3D env) {
		if (isEmptyImpl()) {
			env.setEmpty();
			return;
		}

		Point3D pt = getXYZ();
		env.xmin = pt.x;
		env.ymin = pt.y;
		env.zmin = pt.z;
		env.xmax = pt.x;
		env.ymax = pt.y;
		env.zmax = pt.z;
	}

	@Override
	public Envelope1D queryInterval(int semantics, int ordinate) {
		Envelope1D env = new Envelope1D();
		if (isEmptyImpl()) {
			env.setEmpty();
			return env;
		}

		double s = getAttributeAsDbl(semantics, ordinate);
		env.vmin = s;
		env.vmax = s;
		return env;
	}

	private void resizeAttributes(int newSize) {
		if (m_attributes == null) {
			m_attributes = new double[newSize];
		} else if (m_attributes.length < newSize) {
			double[] newbuffer = new double[newSize];
			System.arraycopy(m_attributes, 0, newbuffer, 0, m_attributes.length);
			m_attributes = newbuffer;
		}
	}

	static void attributeCopy(double[] src, double[] dst, int count) {
		if (count > 0)
			System.arraycopy(src, 0, dst, 0, count);
	}

	/**
	 * Set the X and Y coordinate of the point.
	 * 
	 * @param x
	 *            X coordinate of the point.
	 * @param y
	 *            Y coordinate of the point.
	 */
	public void setXY(double x, double y) {
		_touch();

		if (m_attributes == null)
			_setToDefault();

		m_attributes[0] = x;
		m_attributes[1] = y;
	}

	/**
	 * Returns TRUE when this geometry has exactly same type, properties, and
	 * coordinates as the other geometry.
	 */
	@Override
	public boolean equals(Object _other) {
		if (_other == this)
			return true;

		if (!(_other instanceof Point))
			return false;

		Point otherPt = (Point) _other;

		if (m_description != otherPt.m_description)
			return false;

		if (isEmptyImpl())
			if (otherPt.isEmptyImpl())
				return true;
			else
				return false;

		for (int i = 0, n = m_description.getTotalComponentCount(); i < n; i++)
			if (m_attributes[i] != otherPt.m_attributes[i])
				return false;

		return true;
	}

	/**
	 * Returns the hash code for the point.
	 */

	@Override
	public int hashCode() {
		int hashCode = m_description.hashCode();
		if (!isEmptyImpl()) {
			for (int i = 0, n = m_description.getTotalComponentCount(); i < n; i++) {
				long bits = Double.doubleToLongBits(m_attributes[i]);
				int hc = (int) (bits ^ (bits >>> 32));
				hashCode = NumberUtils.hash(hashCode, hc);
			}
		}
		return hashCode;
	}

	@Override
	public Geometry getBoundary() {
		return null;
	}

	@Override
	public void replaceNaNs(int semantics, double value) {
		addAttribute(semantics);
		if (isEmpty())
			return;

		int ncomps = VertexDescription.getComponentCount(semantics);
		for (int i = 0; i < ncomps; i++) {
			double v = getAttributeAsDbl(semantics, i);
			if (Double.isNaN(v))
				setAttribute(semantics, i, value);
		}
	}
}
