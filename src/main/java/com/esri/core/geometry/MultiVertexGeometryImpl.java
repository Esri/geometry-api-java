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

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;
import com.esri.core.geometry.VertexDescription.Semantics;

/**
 * This class is a base for geometries with many vertices.
 * 
 * The vertex attributes are stored in separate arrays of corresponding type.
 * There are as many arrays as there are attributes in the vertex. It uses lazy
 * allocation for the vertex attributes. This means, the actual AttributeStream
 * is allocated only when the users asks for it, or sets a non-default value.
 * 
 */
abstract class MultiVertexGeometryImpl extends MultiVertexGeometry {

	// HEADER DEFINED
	public interface GeometryXSimple {
		final int Unknown = -1; // not know if simple or not
		final int Not = 0; // not simple
		final int Weak = 1; // weak simple (no self intersections, ring
							// orientation is correct, but ring order is not)
		final int Strong = 2; // same as weak simple + OGC ring order.
	}

	// TODO Remove?
	/**
	 * \internal CHildren implement this method to copy additional information
	 */
	abstract void _copyToImpl(MultiVertexGeometryImpl mvg);

	protected abstract void _notifyModifiedAllImpl();

	/**
	 * \internal Called inside of the VerifyAllStreams to get a child class a
	 * chance to do additional verify.
	 */
	protected abstract void _verifyStreamsImpl();

	public interface DirtyFlags {
		public static final int DirtyIsKnownSimple = 1; // !<0 when IsWeakSimple
														// flag is valid
		public static final int IsWeakSimple = 2; // !<when DirtyIsKnownSimple
													// is 0, this flag indicates
													// whether the geometry is
													// weak simple or not
		public static final int IsStrongSimple = 4;
		public static final int DirtyOGCFlags = 8; // !<OGCFlags are set by
													// Simplify or WKB/WKT
													// import.

		public static final int DirtyVerifiedStreams = 32; // < at least one
															// stream is
															// unverified
		public static final int DirtyExactIntervals = 64; // < exact envelope is
															// dirty
		public static final int DirtyLooseIntervals = 128;
		public static final int DirtyIntervals = DirtyExactIntervals
				| DirtyLooseIntervals; // <
		// loose
		// and
		// dirty
		// envelopes
		// are
		// loose
		public static final int DirtyIsEnvelope = 256; // < the geometry is not
														// known to be an
														// envelope
		public static final int DirtyLength2D = 512; // < the geometry length
														// needs update
		// update
		public static final int DirtyRingAreas2D = 1024; // <
															// m_cachedRingAreas2D
															// need update
		public static final int DirtyCoordinates = DirtyIsKnownSimple
				| DirtyIntervals | DirtyIsEnvelope | DirtyLength2D
				| DirtyRingAreas2D | DirtyOGCFlags;
		public static final int DirtyAllInternal = 0xFFFF; // there has been no
															// change to the
															// streams from
															// outside.
		public static final int DirtyAll = 0xFFFFFF; // there has been a change
														// to one of attribute
														// streams from the
														// outside.

	}

	/**
	 * Returns the total vertex count in this Geometry.
	 * 
	 * @return total vertex count in this Geometry.
	 */
	@Override
	public int getPointCount() {
		return m_pointCount;
	}

	@Override
	public boolean isEmpty() {
		return isEmptyImpl();
	}

	public VertexDescription getDescriptionImpl() {
		return m_description;
	}

	boolean isEmptyImpl() {
		return m_pointCount == 0;
	}

	protected boolean _hasDirtyFlag(int flag) {
		return (m_flagsMask & flag) != 0;
	}

	protected void _setDirtyFlag(int flag, boolean bYesNo) {
		if (bYesNo)
			m_flagsMask |= flag;
		else
			m_flagsMask &= ~flag;
	}

	protected void _verifyAllStreams() {
		if (_hasDirtyFlag(DirtyFlags.DirtyVerifiedStreams))
			_verifyAllStreamsImpl();
	}

	protected void throwIfEmpty() {
		if (isEmptyImpl())
			// TODO fix exceptions
			throw new GeometryException(
					"This operation was performed on an Empty Geometry.");
	}

	private static final long serialVersionUID = 1L;

	AttributeStreamBase[] m_vertexAttributes;
	// TODO implement accelerators
	GeometryAccelerators m_accelerators;
	Envelope m_envelope; // the BBOX for all attributes
	protected int m_pointCount;
	protected int m_reservedPointCount;// the number of vertices reserved and
										// initialized to default value.
	protected int m_flagsMask;
	protected double m_simpleTolerance;

	// HEADER DEFINED

	// Cpp
	// Checked vs. Jan 11, 2011
	public MultiVertexGeometryImpl() {
		m_flagsMask = DirtyFlags.DirtyAllInternal;
		m_pointCount = 0;
		m_reservedPointCount = -1;
		m_accelerators = null;
	}

	@Override
	public void getPointByVal(int index, Point dst) {
		if (index < 0 || index >= m_pointCount)
			// TODO
			throw new GeometryException("index out of bounds");

		// _ASSERT(!IsEmpty());
		// _ASSERT(m_vertexAttributes != null);

		_verifyAllStreams();

		Point outPoint = dst;
		outPoint.assignVertexDescription(m_description);
		if (outPoint.isEmpty())
			outPoint._setToDefault();

		for (int attributeIndex = 0; attributeIndex < m_description
				.getAttributeCount(); attributeIndex++) {
			// fix semantics
			int semantics = m_description._getSemanticsImpl(attributeIndex);

			// VertexDescription.getComponentCount(semantics);
			for (int icomp = 0, ncomp = VertexDescription
					.getComponentCount(semantics); icomp < ncomp; icomp++) {
				double v = m_vertexAttributes[attributeIndex].readAsDbl(ncomp
						* index + icomp);
				outPoint.setAttribute(semantics, icomp, v);
			}
		}
	}

	@Override
	public void setPointByVal(int index, Point src) {
		if (index < 0 || index >= m_pointCount)
			throw new GeometryException("index out of bounds");

		Point point = src;

		if (src.isEmpty())// can not assign an empty point to a multipoint
							// vertex
			throw new IllegalArgumentException();

		_verifyAllStreams();// verify all allocated streams are of necessary
							// size.
		VertexDescription vdin = point.getDescription();
		for (int attributeIndex = 0; attributeIndex < vdin.getAttributeCount(); attributeIndex++) {
			int semantics = vdin._getSemanticsImpl(attributeIndex);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int icomp = 0; icomp < ncomp; icomp++) {
				double v = point.getAttributeAsDbl(semantics, icomp);
				setAttribute(semantics, index, icomp, v);
			}
		}
	}

	// Checked vs. Jan 11, 2011
	@Override
	public Point2D getXY(int index) {
		Point2D pt = new Point2D();
		getXY(index, pt);
		return pt;
	}

	@Override
	public void getXY(int index, Point2D pt) {
		if (index < 0 || index >= getPointCount())
			throw new IndexOutOfBoundsException();

		_verifyAllStreams();
		// AttributeStreamOfDbl v = (AttributeStreamOfDbl)
		// m_vertexAttributes[0];
		AttributeStreamOfDbl v = (AttributeStreamOfDbl) m_vertexAttributes[0];
		v.read(index * 2, pt);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public void setXY(int index, Point2D pt) {
		if (index < 0 || index >= m_pointCount)
			// TODO exception
			throw new IndexOutOfBoundsException();

		_verifyAllStreams();
		// AttributeStreamOfDbl v = (AttributeStreamOfDbl)
		// m_vertexAttributes[0];
		AttributeStreamOfDbl v = (AttributeStreamOfDbl) m_vertexAttributes[0];
		v.write(index * 2, pt);
		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	// Checked vs. Jan 11, 2011
	public void setXY(int index, double x, double y) {
		if (index < 0 || index >= m_pointCount)
			// TODO exc
			throw new IndexOutOfBoundsException();

		_verifyAllStreams();
		// AttributeStreamOfDbl v = (AttributeStreamOfDbl)
		// m_vertexAttributes[0];
		// TODO ask sergey about casts
		AttributeStreamOfDbl v = (AttributeStreamOfDbl) m_vertexAttributes[0];
		v.write(index * 2, x);
		v.write(index * 2 + 1, y);
		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public Point3D getXYZ(int index) {
		if (index < 0 || index >= getPointCount())
			throw new IndexOutOfBoundsException();

		_verifyAllStreams();
		AttributeStreamOfDbl v = (AttributeStreamOfDbl) m_vertexAttributes[0];
		Point3D pt = new Point3D();
		pt.x = v.read(index * 2);
		pt.y = v.read(index * 2 + 1);

		// TODO check excluded if statement componenet
		if (hasAttribute(Semantics.Z))// && (m_vertexAttributes[1] != null))
			pt.z = m_vertexAttributes[1].readAsDbl(index);
		else
			pt.z = VertexDescription.getDefaultValue(Semantics.Z);

		return pt;
	}

	// Checked vs. Jan 11, 2011
	@Override
	public void setXYZ(int index, Point3D pt) {
		if (index < 0 || index >= getPointCount())
			throw new IndexOutOfBoundsException();

		addAttribute(Semantics.Z);

		_verifyAllStreams();
		notifyModified(DirtyFlags.DirtyCoordinates);
		AttributeStreamOfDbl v = (AttributeStreamOfDbl) m_vertexAttributes[0];
		v.write(index * 2, pt.x);
		v.write(index * 2 + 1, pt.y);
		m_vertexAttributes[1].writeAsDbl(index, pt.z);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public double getAttributeAsDbl(int semantics, int offset, int ordinate) {
		if (offset < 0 || offset >= m_pointCount)
			throw new IndexOutOfBoundsException();

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		_verifyAllStreams();
		int attributeIndex = m_description.getAttributeIndex(semantics);
		// TODO check if statement
		if (attributeIndex >= 0)// && m_vertexAttributes[attributeIndex] !=
								// null) {
		{
			return m_vertexAttributes[attributeIndex].readAsDbl(offset * ncomps
					+ ordinate);
		}
		return VertexDescription.getDefaultValue(semantics);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public int getAttributeAsInt(int semantics, int offset, int ordinate) {
		return (int) getAttributeAsDbl(semantics, offset, ordinate);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public void setAttribute(int semantics, int offset, int ordinate,
			double value) {
		if (offset < 0 || offset >= m_pointCount)
			throw new IndexOutOfBoundsException();

		int ncomps = VertexDescription.getComponentCount(semantics);
		if (ordinate >= ncomps)
			throw new IndexOutOfBoundsException();

		addAttribute(semantics);
		_verifyAllStreams();
		int attributeIndex = m_description.getAttributeIndex(semantics);
		notifyModified(DirtyFlags.DirtyCoordinates);
		m_vertexAttributes[attributeIndex].writeAsDbl(offset * ncomps
				+ ordinate, value);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public void setAttribute(int semantics, int offset, int ordinate, int value) {
		setAttribute(semantics, offset, ordinate, (double) value);
	}

	public AttributeStreamBase getAttributeStreamRef(int semantics) {
		throwIfEmpty();

		addAttribute(semantics);
		_verifyAllStreams();

		int attributeIndex = m_description.getAttributeIndex(semantics);
		return m_vertexAttributes[attributeIndex];
	}

	/**
	 * Sets a reference to the given AttributeStream of the Geometry. Once the
	 * buffer has been obtained, the vertices of the Geometry can be manipulated
	 * directly. The AttributeStream parameters are not checked for the size. <br>
	 * If the attribute is missing, it will be added. <br>
	 * Note, that this method does not change the vertex count in the Geometry. <br>
	 * The stream can have more elements, than the Geometry point count, but
	 * only necessary part will be saved when exporting to a ESRI shape or other
	 * format. @param semantics Semantics of the attribute to assign the stream
	 * to. @param stream The input AttributeStream that will be assigned by
	 * reference. If one changes the stream later through the reference, one has
	 * to call NotifyStreamChanged. \exception Throws invalid_argument exception
	 * if the input stream type does not match that of the semantics
	 * persistence.
	 */
	public void setAttributeStreamRef(int semantics, AttributeStreamBase stream) {
		// int test1 = VertexDescription.getPersistence(semantics);
		// int test2 = stream.getPersistence();

		if ((stream != null)
				&& VertexDescription.getPersistence(semantics) != stream
						.getPersistence())// input stream has wrong persistence
			throw new IllegalArgumentException();

		// Do not check for the stream size here to allow several streams to be
		// attached before the point count is changed.
		addAttribute(semantics);
		int attributeIndex = m_description.getAttributeIndex(semantics);
		if (m_vertexAttributes == null)
			m_vertexAttributes = new AttributeStreamBase[m_description
					.getAttributeCount()];

		m_vertexAttributes[attributeIndex] = stream;
		notifyModified(DirtyFlags.DirtyAll);
	}

	@Override
	protected void _assignVertexDescriptionImpl(VertexDescription newDescription) {
		AttributeStreamBase[] newAttributes = null;
		
		if (m_vertexAttributes != null) {
			int[] mapping = VertexDescriptionDesignerImpl.mapAttributes(
					newDescription, m_description);
			
			newAttributes = new AttributeStreamBase[newDescription
			                    					.getAttributeCount()];

			for (int i = 0, n = newDescription.getAttributeCount(); i < n; i++) {
				if (mapping[i] != -1) {
					int m = mapping[i];
					newAttributes[i] = m_vertexAttributes[m];
				}

			}
		}
		else {
			//if there are no streams we do not create them
		}
		
		m_description = newDescription;
		m_vertexAttributes = newAttributes; // late assignment to try to stay
		m_reservedPointCount = -1;// we need to recreate the new attribute then
		notifyModified(DirtyFlags.DirtyAll);
	}
	
	// Checked vs. Jan 11, 2011
	protected void _updateEnvelope(Envelope2D env) {
		_updateAllDirtyIntervals(true);
		m_envelope.queryEnvelope2D(env);
	} // note: overload for polylines/polygons with curves

	// Checked vs. Jan 11, 2011
	protected void _updateEnvelope(Envelope3D env) {
		_updateAllDirtyIntervals(true);
		m_envelope.queryEnvelope3D(env);
	} // note: overload for polylines/polygons with curves

	// Checked vs. Jan 11, 2011
	protected void _updateLooseEnvelope(Envelope2D env) {
		// TODO ROHIT has this set to true?
		_updateAllDirtyIntervals(false);
		m_envelope.queryEnvelope2D(env);
	} // note: overload for polylines/polygons with curves

	// Checked vs. Jan 11, 2011
	/**
	 * \internal Calculates loose envelope. Returns True if the calculation
	 * renders exact envelope.
	 */
	protected void _updateLooseEnvelope(Envelope3D env) {
		// TODO ROHIT has this set to true?
		_updateAllDirtyIntervals(false);
		m_envelope.queryEnvelope3D(env);
	} // note: overload for polylines/polygons with curves

	// Checked vs. Jan 11, 2011
	@Override
	public void queryEnvelope(Envelope env) {
		_updateAllDirtyIntervals(true);
		m_envelope.copyTo(env);
	}

	// TODO rename to remove 2D
	// Checked vs. Jan 11, 2011
	@Override
	public void queryEnvelope2D(Envelope2D env) {
		_updateEnvelope(env);
	}

	// Checked vs. Jan 11, 2011
	// TODO rename to remove 3D
	@Override
	public void queryEnvelope3D(Envelope3D env) {
		_updateEnvelope(env);
	}

	// Checked vs. Jan 11, 2011
	// TODO rename to remove 2D
	@Override
	public void queryLooseEnvelope2D(Envelope2D env) {
		_updateLooseEnvelope(env);
	}

	// Checked vs. Jan 11, 2011
	// TODO rename to remove 3D
	@Override
	public void queryLooseEnvelope3D(Envelope3D env) {
		_updateLooseEnvelope(env);
	}

	// Checked vs. Jan 11, 2011
	@Override
	public Envelope1D queryInterval(int semantics, int ordinate) {
		Envelope1D env = new Envelope1D();
		if (isEmptyImpl()) {
			env.setEmpty();
			return env;
		}

		_updateAllDirtyIntervals(true);
		return m_envelope.queryInterval(semantics, ordinate);
	}

	// Checked vs. Jan 11, 2011
	// TODO Rename to getHashCode
	@Override
	public int hashCode() {
		int hashCode = m_description.hashCode();

		if (!isEmptyImpl()) {
			int pointCount = getPointCount();
			for (int i = 0, n = m_description.getAttributeCount(); i < n; i++) {
				int components = VertexDescription
						.getComponentCount(m_description._getSemanticsImpl(i));
				AttributeStreamBase stream = m_vertexAttributes[i];
				hashCode = stream.calculateHashImpl(hashCode, 0, pointCount
						* components);
			}
		}

		return hashCode;
	}

	// Checked vs. Jan 11, 2011
	@Override
	public boolean equals(Object other) {
		// Java checks
		if (other == this)
			return true;

		if (!(other instanceof MultiVertexGeometryImpl))
			return false;

		MultiVertexGeometryImpl otherMulti = (MultiVertexGeometryImpl) other;

		if (!(m_description.equals(otherMulti.m_description)))
			return false;

		if (isEmptyImpl() != otherMulti.isEmptyImpl())
			return false;

		if (isEmptyImpl())
			return true; // both geometries are empty

		int pointCount = getPointCount();
		int pointCountOther = otherMulti.getPointCount();

		if (pointCount != pointCountOther)
			return false;

		for (int i = 0; i < m_description.getAttributeCount(); i++) {
			int semantics = m_description.getSemantics(i);

			AttributeStreamBase stream = getAttributeStreamRef(semantics);
			AttributeStreamBase streamOther = otherMulti
					.getAttributeStreamRef(semantics);

			int components = VertexDescription.getComponentCount(semantics);

			if (!stream.equals(streamOther, 0, pointCount * components))
				return false;
		}

		return true;
	}

	// Checked vs. Jan 11, 2011
	/**
	 * Sets the envelope of the Geometry. The Envelope description must match
	 * that of the Geometry.
	 */
	public void setEnvelope(Envelope env) {
		if (!m_description.equals(env.getDescription()))
			throw new IllegalArgumentException();

		// m_envelope = (Envelope) env.clone();
		m_envelope = (Envelope) env.createInstance();
		env.copyTo(m_envelope);
		_setDirtyFlag(DirtyFlags.DirtyIntervals, false);
	}

	@Override
	public void copyTo(Geometry dstGeom) {
		MultiVertexGeometryImpl dst = (MultiVertexGeometryImpl) dstGeom;
		if (dst.getType() != getType())
			throw new IllegalArgumentException();
		
		_copyToUnsafe(dst);
	}
	
	//Does not check geometry type. Used to copy Polygon to Polyline
	void _copyToUnsafe(MultiVertexGeometryImpl dst) {
		_verifyAllStreams();
		dst.m_description = m_description;
		dst.m_vertexAttributes = null;
		int nattrib = m_description.getAttributeCount();
		AttributeStreamBase[] cloneAttributes = null;
		if (m_vertexAttributes != null) {
			cloneAttributes = new AttributeStreamBase[nattrib];
			for (int i = 0; i < nattrib; i++) {
				if (m_vertexAttributes[i] != null) {
					int ncomps = VertexDescription
							.getComponentCount(m_description
									._getSemanticsImpl(i));
					cloneAttributes[i] = m_vertexAttributes[i]
							.restrictedClone(getPointCount() * ncomps);
				}
			}
		}

		if (m_envelope != null) {
			dst.m_envelope = (Envelope) m_envelope.createInstance();
			m_envelope.copyTo(dst.m_envelope);
			// dst.m_envelope = (Envelope) m_envelope.clone();
		} else
			dst.m_envelope = null;

		dst.m_pointCount = m_pointCount;
		dst.m_flagsMask = m_flagsMask;
		dst.m_vertexAttributes = cloneAttributes;

		try {
			_copyToImpl(dst); // copy child props
		} catch (Exception ex) {
			dst.setEmpty();
			throw new RuntimeException(ex);
		}
	}

	// Checked vs. Jan 11, 2011
	public boolean _attributeStreamIsAllocated(int semantics) {
		throwIfEmpty();

		int attributeIndex = m_description.getAttributeIndex(semantics);

		if (attributeIndex >= 0 && m_vertexAttributes[attributeIndex] != null)
			return true;

		return false;
	}

	// Checked vs. Jan 11, 2011
	void _setEmptyImpl() {
		m_pointCount = 0;
		m_reservedPointCount = -1;
		m_vertexAttributes = null;// release it all streams.
		notifyModified(DirtyFlags.DirtyAll);
	}

	// Checked vs. Jan 11, 2011
	/**
	 * Notifies the Geometry of changes made to the vertices so that it could
	 * reset cached structures.
	 */
	public void notifyModified(int flags) {
		if (flags == DirtyFlags.DirtyAll) {
			m_reservedPointCount = -1;// forget the reserved point number
			_notifyModifiedAllImpl();
		}
		m_flagsMask |= flags;

		_clearAccelerators();
		_touch();
	}

	// Checked vs. Jan 11, 2011
	/**
	 * @param bExact
	 *            True, when the exact envelope need to be calculated and false
	 *            for the loose one.
	 */
	protected void _updateAllDirtyIntervals(boolean bExact) {
		_verifyAllStreams();
		if (_hasDirtyFlag(DirtyFlags.DirtyIntervals)) {
			if (null == m_envelope)
				m_envelope = new Envelope(m_description);
			else
				m_envelope.assignVertexDescription(m_description);

			if (isEmpty()) {
				m_envelope.setEmpty();
				return;
			}

			_updateXYImpl(bExact);// efficient method for xy's
			// now go through other attribues.
			for (int attributeIndex = 1; attributeIndex < m_description
					.getAttributeCount(); attributeIndex++) {
				int semantics = m_description._getSemanticsImpl(attributeIndex);
				int ncomps = VertexDescription.getComponentCount(semantics);
				AttributeStreamBase stream = m_vertexAttributes[attributeIndex];
				for (int iord = 0; iord < ncomps; iord++) {
					Envelope1D interval = new Envelope1D();
					interval.setEmpty();
					for (int i = 0; i < m_pointCount; i++) {
						double value = stream.readAsDbl(i * ncomps + iord);// some
																			// optimization
																			// is
																			// possible
																			// if
																			// non-virtual
																			// method
																			// is
																			// used
						interval.merge(value);
					}
					m_envelope.setInterval(semantics, iord, interval);
				}
			}
			if (bExact)
				_setDirtyFlag(DirtyFlags.DirtyIntervals, false);
		}
	}

	// Checked vs. Jan 11, 2011
	/**
	 * \internal Updates x, y intervals.
	 */
	public void _updateXYImpl(boolean bExact) {
		m_envelope.setEmpty();
		AttributeStreamOfDbl stream = (AttributeStreamOfDbl) m_vertexAttributes[0];
		Point2D pt = new Point2D();
		for (int i = 0; i < m_pointCount; i++) {
			stream.read(2 * i, pt);
			m_envelope.merge(pt);
		}
	}

	void calculateEnvelope2D(Envelope2D env, boolean bExact) {
		env.setEmpty();
		AttributeStreamOfDbl stream = (AttributeStreamOfDbl) m_vertexAttributes[0];
		Point2D pt = new Point2D();
		for (int i = 0; i < m_pointCount; i++) {
			stream.read(2 * i, pt);
			env.merge(pt);
		}
	}

	// Checked vs. Jan 11, 2011 lots of changes
	/**
	 * \internal Verifies all streams (calls _VerifyStream for every attribute).
	 */
	protected void _verifyAllStreamsImpl() {
		// This method checks that the streams are of correct size.
		// It resizes the streams to ensure they are not shorter than
		// m_PointCount
		// _ASSERT(_HasDirtyFlag(enum_value1(DirtyFlags,
		// DirtyVerifiedStreams)));
		if (m_reservedPointCount < m_pointCount) // an optimization to skip this
													// expensive loop when
													// adding point by point
		{
			if (m_vertexAttributes == null)
				m_vertexAttributes = new AttributeStreamBase[m_description
						.getAttributeCount()];

			m_reservedPointCount = NumberUtils.intMax();
			for (int attributeIndex = 0; attributeIndex < m_description
					.getAttributeCount(); attributeIndex++) {
				int semantics = m_description._getSemanticsImpl(attributeIndex);
				if (m_vertexAttributes[attributeIndex] != null) {
					int ncomp = VertexDescription.getComponentCount(semantics);
					int size = m_vertexAttributes[attributeIndex].virtualSize()
							/ ncomp;
					if (size < m_pointCount) {
						size = (m_reservedPointCount > m_pointCount + 5) ? (m_pointCount * 5 + 3) / 4
								: m_pointCount;// reserve 25% more than user
												// asks
						m_vertexAttributes[attributeIndex].resize(size * ncomp,
								VertexDescription.getDefaultValue(semantics));
					}

					if (size < m_reservedPointCount)
						m_reservedPointCount = size;
				} else {
					m_vertexAttributes[attributeIndex] = AttributeStreamBase
							.createAttributeStreamWithSemantics(semantics,
									m_pointCount);
					m_reservedPointCount = m_pointCount;
				}
			}
		}
		_verifyStreamsImpl();

		_setDirtyFlag(DirtyFlags.DirtyVerifiedStreams, false);
	}

	// Checked vs. Jan 11, 2011
	void _resizeImpl(int pointCount) {
		if (pointCount < 0)
			throw new IllegalArgumentException();

		if (pointCount == m_pointCount)
			return;

		m_pointCount = pointCount;
		notifyModified(DirtyFlags.DirtyAllInternal);
	}

	// Checked vs. Jan 11, 2011
	int queryCoordinates(Point2D[] dst, int dstSize, int beginIndex,
			int endIndex) {
		int endIndexC = endIndex < 0 ? m_pointCount : endIndex;
		endIndexC = Math.min(endIndexC, beginIndex + dstSize);

		if (beginIndex < 0 || beginIndex >= m_pointCount
				|| endIndexC < beginIndex)
			throw new IllegalArgumentException();

		AttributeStreamOfDbl xy = (AttributeStreamOfDbl) getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		int j = 0;
		double[] dstArray = new double[dst.length * 2];
		xy.readRange(2 * beginIndex, (endIndexC - beginIndex) * 2, dstArray, j, true);

		for (int i = 0; i < dst.length; i++) {
			dst[i] = new Point2D(dstArray[i * 2], dstArray[i * 2 + 1]);
		}

		// for (int i = beginIndex; i < endIndexC; i++, j++)
		// {
		// xy.read(2 * i, dst[j]);
		// }

		return endIndexC;
	}

	// Checked vs. Jan 11, 2011
	int QueryCoordinates(Point3D[] dst, int dstSize, int beginIndex,
			int endIndex) {
		int endIndexC = endIndex < 0 ? m_pointCount : endIndex;
		endIndexC = Math.min(endIndexC, beginIndex + dstSize);

		if (beginIndex < 0 || beginIndex >= m_pointCount
				|| endIndexC < beginIndex)
			// TODO replace geometry exc
			throw new IllegalArgumentException();

		AttributeStreamOfDbl xy = (AttributeStreamOfDbl) getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		AttributeStreamOfDbl z = null;
		double v = VertexDescription
				.getDefaultValue(VertexDescription.Semantics.Z);
		boolean bHasZ = hasAttribute(VertexDescription.Semantics.Z);
		if (bHasZ)
			z = (AttributeStreamOfDbl) getAttributeStreamRef(VertexDescription.Semantics.Z);
		int j = 0;
		for (int i = beginIndex; i < endIndexC; i++, j++) {
			dst[j].x = xy.read(2 * i);
			dst[j].y = xy.read(2 * i + 1);
			dst[j].z = bHasZ ? z.read(i) : v;

			dst[j] = getXYZ(i);
		}

		return endIndexC;
	}

	// Checked vs. Jan 11, 2011
	// -1 : DirtySimple is true (whether or not the MultiPath is Simple is
	// unknown)
	// 0 : DirtySimple is false and the MultiPath is not Weak Simple
	// 1 : DirtySimple is false and the MultiPath is Weak Simple but not ring
	// ordering may be invalid
	// 2 : DirtySimple is false and the MultiPath is Strong Simple (Weak Simple
	// and valid ring ordering)
	public int getIsSimple(double tolerance) {
		if (!_hasDirtyFlag(DirtyFlags.DirtyIsKnownSimple)) {
			if (!_hasDirtyFlag(DirtyFlags.IsWeakSimple)) {
				return 0;
			}
			if (m_simpleTolerance >= tolerance) {
				if (!_hasDirtyFlag(DirtyFlags.DirtyOGCFlags))
					return 2;

				return 1;
			}

			return -1;
		}
		return -1;
	}

	void setIsSimple(int isSimpleRes, double tolerance, boolean ogc_known) {
		m_simpleTolerance = tolerance;
		if (isSimpleRes == GeometryXSimple.Unknown) {
			_setDirtyFlag(DirtyFlags.DirtyIsKnownSimple, true);
			_setDirtyFlag(DirtyFlags.DirtyOGCFlags, true);
			return;
		}
		_setDirtyFlag(DirtyFlags.DirtyIsKnownSimple, false);

		if (!ogc_known)
			_setDirtyFlag(DirtyFlags.DirtyOGCFlags, true);

		if (isSimpleRes == GeometryXSimple.Not) {
			_setDirtyFlag(DirtyFlags.IsWeakSimple, false);
			_setDirtyFlag(DirtyFlags.IsStrongSimple, false);
		} else if (isSimpleRes == GeometryXSimple.Weak) {
			_setDirtyFlag(DirtyFlags.IsWeakSimple, true);
			_setDirtyFlag(DirtyFlags.IsStrongSimple, false);
		} else if (isSimpleRes == GeometryXSimple.Strong) {
			_setDirtyFlag(DirtyFlags.IsWeakSimple, true);
			_setDirtyFlag(DirtyFlags.IsStrongSimple, true);
		} else
			throw GeometryException.GeometryInternalError();// what?
	}

	double _getSimpleTolerance() {
		return m_simpleTolerance;
	}

	public GeometryAccelerators _getAccelerators() {
		return m_accelerators;
	}

	void _clearAccelerators() {
		if (m_accelerators != null)
			m_accelerators = null;
	}

	void _interpolateTwoVertices(int vertex1, int vertex2, double f,
			Point outPoint) {
		if (vertex1 < 0 || vertex1 >= m_pointCount)
			throw new GeometryException("index out of bounds.");
		if (vertex2 < 0 || vertex2 >= m_pointCount)
			throw new GeometryException("index out of bounds.");

		// _ASSERT(!IsEmpty());
		// _ASSERT(m_vertexAttributes != NULLPTR);

		_verifyAllStreams();

		outPoint.assignVertexDescription(m_description);
		if (outPoint.isEmpty())
			outPoint._setToDefault();

		for (int attributeIndex = 0; attributeIndex < m_description
				.getAttributeCount(); attributeIndex++) {
			int semantics = m_description._getSemanticsImpl(attributeIndex);
			for (int icomp = 0, ncomp = VertexDescription
					.getComponentCount(semantics); icomp < ncomp; icomp++) {
				double v1 = m_vertexAttributes[attributeIndex].readAsDbl(ncomp
						* vertex1 + icomp);
				double v2 = m_vertexAttributes[attributeIndex].readAsDbl(ncomp
						* vertex2 + icomp);
				outPoint.setAttribute(semantics, icomp, MathUtils.lerp(v1,  v2,  f));
			}
		}
	}

	double _getShortestDistance(int vertex1, int vertex2) {
		Point2D pt = getXY(vertex1);
		pt.sub(getXY(vertex2));
		return pt.length();
	}

	// ////////////////// METHODS To REMOVE ///////////////////////
	@Override
	public Point getPoint(int index) {
		if (index < 0 || index >= m_pointCount)
			throw new IndexOutOfBoundsException();

		_verifyAllStreams();

		Point outPoint = new Point();
		outPoint.assignVertexDescription(m_description);
		if (outPoint.isEmpty())
			outPoint._setToDefault();

		for (int attributeIndex = 0; attributeIndex < m_description
				.getAttributeCount(); attributeIndex++) {
			int semantics = m_description.getSemantics(attributeIndex);
			for (int icomp = 0, ncomp = VertexDescription
					.getComponentCount(semantics); icomp < ncomp; icomp++) {
				double v = m_vertexAttributes[attributeIndex].readAsDbl(ncomp
						* index + icomp);
				outPoint.setAttribute(semantics, icomp, v);
			}
		}
		return outPoint;
	}

	@Override
	public void setPoint(int index, Point src) {
		if (index < 0 || index >= m_pointCount)
			throw new IndexOutOfBoundsException();

		Point point = src;

		if (src.isEmpty())// can not assign an empty point to a multipoint
							// vertex
			throw new IllegalArgumentException();

		_verifyAllStreams();// verify all allocated streams are of necessary
							// size.
		VertexDescription vdin = point.getDescription();
		for (int attributeIndex = 0; attributeIndex < vdin.getAttributeCount(); attributeIndex++) {
			int semantics = vdin.getSemantics(attributeIndex);
			int ncomp = VertexDescription.getComponentCount(semantics);
			for (int icomp = 0; icomp < ncomp; icomp++) {
				double v = point.getAttributeAsDbl(semantics, icomp);
				setAttribute(semantics, index, icomp, v);
			}
		}
	}

	@Override
	public void queryCoordinates(Point[] dst) {
		int sz = m_pointCount;
		if (dst.length < sz)
			throw new IllegalArgumentException();

		// TODO: refactor to a better AttributeAray call (ReadRange?)
		for (int i = 0; i < sz; i++) {
			dst[i] = getPoint(i);
		}
	}

	@Override
	public void queryCoordinates(Point2D[] dst) {
		int sz = m_pointCount;
		if (dst.length < sz)
			throw new IllegalArgumentException();

		// TODO: refactor to a better AttributeAray call (ReadRange?)
		for (int i = 0; i < sz; i++) {
			dst[i] = getXY(i);
		}
	}

	@Override
	public void queryCoordinates(Point3D[] dst) {
		int sz = m_pointCount;
		if (dst.length < sz)
			throw new IllegalArgumentException();

		// TODO: refactor to a better AttributeAray call (ReadRange?)
		for (int i = 0; i < sz; i++) {
			dst[i] = getXYZ(i);
		}
	}
	
    @Override
    public void replaceNaNs(int semantics, double value) {
    	addAttribute(semantics);
    	if (isEmpty())
    		return;
    	
    	boolean modified = false;
    	int ncomps = VertexDescription.getComponentCount(semantics);
    	for (int i = 0; i < ncomps; i++) {
    		AttributeStreamBase streamBase = getAttributeStreamRef(semantics);
    		if (streamBase instanceof AttributeStreamOfDbl)	{
    			AttributeStreamOfDbl dblStream = (AttributeStreamOfDbl)streamBase;
    			for (int ivert = 0, n = m_pointCount * ncomps; ivert < n; ivert++) {
    				double v = dblStream.read(ivert);
    				if (Double.isNaN(v)) {
    					dblStream.write(ivert, value);
    					modified = true;
    				}
    			}
    		}
    		else {
    			for (int ivert = 0, n = m_pointCount * ncomps; ivert < n; ivert++) {
    				double v = streamBase.readAsDbl(ivert);
    				if (Double.isNaN(v)) {
    					streamBase.writeAsDbl(ivert, value);
    					modified = true;
    				}
    			}
    		}
    	}
    	
    	if (modified) {
    		notifyModified(DirtyFlags.DirtyCoordinates);
    	}
    }

	public abstract boolean _buildRasterizedGeometryAccelerator(
			double toleranceXY, GeometryAccelerationDegree accelDegree);

	public abstract boolean _buildQuadTreeAccelerator(
			GeometryAccelerationDegree d);

	@Override
	public String toString() {
		return "MultiVertexGeometryImpl";
	}	
}
