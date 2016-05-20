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

import java.util.Arrays;

/**
 * Describes the vertex format of a Geometry.
 * 
 * Geometry objects store vertices. The vertex is a multi attribute entity. It
 * has mandatory X, Y coordinates. In addition it may have Z, M, ID, and other
 * user specified attributes. Geometries point to VertexDescription instances.
 * If the two Geometries have same set of attributes, they point to the same
 * VertexDescription instance. <br>
 * To create a new VertexDescription use the VertexDescriptionDesigner class. <br>
 * The VertexDescription allows to add new attribute types easily (see ID2). <br>
 * The attributes are stored sorted by Semantics value. <br>
 * Note: You could also think of the VertexDescription as a schema of a database
 * table. You may look the vertices of a Geometry as if they are stored in a
 * database table, and the VertexDescription defines the fields of the table.
 */
public final class VertexDescription {
	/**
	 * Describes the attribute and, in case of predefined attributes, provides a
	 * hint of the attribute use.
	 */
	public interface Semantics {
		static final int POSITION = 0; // xy coordinates of a point (2D
												// vector of double, linear
												// interpolation)

		static final int Z = 1; // z coordinates of a point (double,
										// linear interpolation)

		static final int M = 2; // m attribute (double, linear
										// interpolation)

		static final int ID = 3; // id (int, no interpolation)

		static final int NORMAL = 4; // xyz coordinates of normal vector
											// (float, angular interpolation)

		static final int TEXTURE1D = 5; // u coordinates of texture
												// (float, linear interpolation)

		static final int TEXTURE2D = 6; // uv coordinates of texture
												// (float, linear interpolation)

		static final int TEXTURE3D = 7; // uvw coordinates of texture
												// (float, linear interpolation)

		static final int ID2 = 8; // two component ID

		static final int MAXSEMANTICS = 8; // the max semantics value
	}

	/**
	 * Specifies how the attribute is interpolated along the segments. are
	 * represented as int64
	 */
	interface Interpolation {
		public static final int NONE = 0;

		public static final int LINEAR = 1;

		public static final int ANGULAR = 2;
	}

	/**
	 * Specifies the type of the attribute.
	 */
	interface Persistence {
		public static final int enumFloat = 0;

		public static final int enumDouble = 1;

		public static final int enumInt32 = 2;

		public static final int enumInt64 = 3;

		public static final int enumInt8 = 4; // 8 bit integer. Can be signed or
												// unsigned depending on
												// platform.

		public static final int enumInt16 = 5;
	};

	/**
	 * Returns the attribute count of this description. The value is always
	 * greater or equal to 1. The first attribute is always a POSITION.
	 */
	public final int getAttributeCount() {
		return m_attributeCount;
	}

	/**
	 * Returns the semantics of the given attribute.
	 * 
	 * @param attributeIndex
	 *            The index of the attribute in the description. Max value is
	 *            getAttributeCount() - 1.
	 */
	public final int getSemantics(int attributeIndex) {
		return m_indexToSemantics[attributeIndex];
	}

	/**
	 * Returns the index the given attribute in the vertex description.
	 * 
	 * @param semantics
	 * @return Returns the attribute index or -1 of the attribute does not exist
	 */
	public final int getAttributeIndex(int semantics) {
		return m_semanticsToIndexMap[semantics];
	}

	/**
	 * Returns the interpolation type for the attribute.
	 * 
	 * @param semantics
	 *            The semantics of the attribute.
	 */
	static int getInterpolation(int semantics) {
		return _interpolation[semantics];
	}

	/**
	 * Returns the persistence type for the attribute.
	 * 
	 * @param semantics
	 *            The semantics of the attribute.
	 */
	static int getPersistence(int semantics) {
		return _persistence[semantics];
	}

	/**
	 * Returns the size of the persistence type in bytes.
	 * 
	 * @param persistence
	 *            The persistence type to query.
	 */
	static int getPersistenceSize(int persistence) {
		return _persistencesize[persistence];
	}

	/**
	 * Returns the size of the semantics in bytes.
	 */
	static int getPersistenceSizeSemantics(int semantics) {
		return getPersistenceSize(getPersistence(semantics))
				* getComponentCount(semantics);
	}

	/**
	 * Returns the number of the components of the given semantics. For example,
	 * it returns 2 for the POSITION.
	 * 
	 * @param semantics
	 *            The semantics of the attribute.
	 */
	public static int getComponentCount(int semantics) {
		return _components[semantics];
	}

	/**
	 * Returns True if the attribute with the given name and given set exists.
	 * 
	 * @param semantics
	 *            The semantics of the attribute.
	 */
	public boolean hasAttribute(int semantics) {
		return (m_semanticsBitArray & (1 << semantics)) != 0;
	}

	/**
	 * Returns True if this vertex description includes all attributes from the
	 * src.
	 *
	 * @param src
	 *            The Vertex_description to compare with.
	 * @return The function returns false, only when this description does not
	 *         have some of the attribute that src has.
	 */
	public final boolean hasAttributesFrom(VertexDescription src) {
		return (m_semanticsBitArray & src.m_semanticsBitArray) == src.m_semanticsBitArray;
	}

	/**
	 * Returns True, if the vertex has Z attribute.
	 */
	public final boolean hasZ() {
		return hasAttribute(Semantics.Z);
	}

	/**
	 * Returns True, if the vertex has M attribute.
	 */
	public final boolean hasM() {
		return hasAttribute(Semantics.M);
	}

	/**
	 * Returns True, if the vertex has ID attribute.
	 */
	public final boolean hasID() {
		return hasAttribute(Semantics.ID);
	}

	/**
	 * Returns default value for each ordinate of the vertex attribute with
	 * given semantics.
	 */
	public static double getDefaultValue(int semantics) {
		return _defaultValues[semantics];
	}

	int getPointAttributeOffset_(int attributeIndex) {
		return m_pointAttributeOffsets[attributeIndex];
	}

	/**
	 * Returns the total component count.
	 */
	public int getTotalComponentCount() {
		return m_totalComponentCount;
	}

	/**
	 * Checks if the given value is the default one. The simple equality test
	 * with GetDefaultValue does not work due to the use of NaNs as default
	 * value for some parameters.
	 */
	public static boolean isDefaultValue(int semantics, double v) {
		return NumberUtils.doubleToInt64Bits(_defaultValues[semantics]) == NumberUtils
				.doubleToInt64Bits(v);
	}

	static boolean isIntegerPersistence(int persistence) {
		return persistence >= Persistence.enumInt32;
	}

	static boolean isIntegerSemantics(int semantics) {
		return isIntegerPersistence(getPersistence(semantics));
	}
	
	@Override
	public boolean equals(Object _other) {
		return (Object) this == _other;
	}

	/**
	 * 
	 * Returns a packed array of double representation of all ordinates of
	 * attributes of a point, i.e.: X, Y, Z, ID, TEXTURE2D.u, TEXTURE2D.v
	 */
	double[] _getDefaultPointAttributes() {
		return m_defaultPointAttributes;
	}

	double _getDefaultPointAttributeValue(int attributeIndex, int ordinate) {
		return m_defaultPointAttributes[_getPointAttributeOffset(attributeIndex)
				+ ordinate];
	}

	/**
	 * 
	 * Returns an offset to the first ordinate of the given attribute. This
	 * method is used for the cases when one wants to have a packed array of
	 * ordinates of all attributes, i.e.: X, Y, Z, ID, TEXTURE2D.u, TEXTURE2D.v
	 */
	int _getPointAttributeOffset(int attributeIndex) {
		return m_pointAttributeOffsets[attributeIndex];
	}

	int _getPointAttributeOffsetFromSemantics(int semantics) {
		return m_pointAttributeOffsets[getAttributeIndex(semantics)];
	}

	@Override
	public int hashCode() {
		return m_hash;
	}

	int _getSemanticsImpl(int attributeIndex) {
		return m_indexToSemantics[attributeIndex];
	}

	VertexDescription(int bitMask) {
		m_semanticsBitArray = bitMask;
		m_attributeCount = 0;
		m_totalComponentCount = 0;
		m_semanticsToIndexMap = new int[Semantics.MAXSEMANTICS + 1];
		Arrays.fill(m_semanticsToIndexMap, -1);
		for (int i = 0, flag = 1, n = Semantics.MAXSEMANTICS + 1; i < n; i++) {
			if ((bitMask & flag) != 0) {
				m_semanticsToIndexMap[i] = m_attributeCount;
				m_attributeCount++;
				int comps = getComponentCount(i);
				m_totalComponentCount += comps;
			}

			flag <<= 1;
		}

		m_indexToSemantics = new int[m_attributeCount];
		for (int i = 0, n = Semantics.MAXSEMANTICS + 1; i < n; i++) {
			int attrib = m_semanticsToIndexMap[i];
			if (attrib >= 0)
				m_indexToSemantics[attrib] = i;
		}

		m_defaultPointAttributes = new double[m_totalComponentCount];
		m_pointAttributeOffsets = new int[m_attributeCount];
		int offset = 0;
		for (int i = 0, n = m_attributeCount; i < n; i++) {
			int semantics = getSemantics(i);
			int comps = getComponentCount(semantics);
			double v = getDefaultValue(semantics);
			m_pointAttributeOffsets[i] = offset;
			for (int icomp = 0; icomp < comps; icomp++) {
				m_defaultPointAttributes[offset] = v;
				offset++;
			}
		}

		m_hash = NumberUtils.hash(m_semanticsBitArray);
	}

	private int m_attributeCount;
	int m_semanticsBitArray; //the main component
	private int m_totalComponentCount;
	private int m_hash;

	private int[] m_semanticsToIndexMap;
	private int[] m_indexToSemantics;
	private int[] m_pointAttributeOffsets;
	private double[] m_defaultPointAttributes;

	static final double[] _defaultValues = { 0, 0, NumberUtils.NaN(), 0, 0, 0,
			0, 0, 0 };

	static final int[] _interpolation = { Interpolation.LINEAR,
			Interpolation.LINEAR, Interpolation.LINEAR, Interpolation.NONE,
			Interpolation.ANGULAR, Interpolation.LINEAR, Interpolation.LINEAR,
			Interpolation.LINEAR, Interpolation.NONE, };

	static final int[] _persistence = { Persistence.enumDouble,
			Persistence.enumDouble, Persistence.enumDouble,
			Persistence.enumInt32, Persistence.enumFloat,
			Persistence.enumFloat, Persistence.enumFloat,
			Persistence.enumFloat, Persistence.enumInt32, };

	static final int[] _persistencesize = { 4, 8, 4, 8, 1, 2 };

	static final int[] _components = { 2, 1, 1, 1, 3, 1, 2, 3, 2, };
}
