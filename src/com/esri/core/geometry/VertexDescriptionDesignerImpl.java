/*
 Copyright 1995-2013 Esri

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

import com.esri.core.geometry.VertexDescription.Semantics;

/**
 * This factory class allows to describe and create a VertexDescription
 * instance.
 */
class VertexDescriptionDesignerImpl extends VertexDescription {

	/**
	 * Designer default constructor produces XY vertex description (POSITION
	 * semantics only).
	 */
	public VertexDescriptionDesignerImpl() {
		super();
		m_semantics = new int[Semantics.MAXSEMANTICS];
		m_semantics[0] = Semantics.POSITION;
		m_attributeCount = 1;

		m_semanticsToIndexMap = new int[Semantics.MAXSEMANTICS];

		for (int i = 0; i < Semantics.MAXSEMANTICS; i++)
			m_semanticsToIndexMap[i] = -1;

		m_semanticsToIndexMap[m_semantics[0]] = 0;

		m_bModified = true;
	}

	/**
	 * Creates description designer and initializes it from the given
	 * description. Use this to add or remove attributes from the description.
	 */
	public VertexDescriptionDesignerImpl(VertexDescription other) {
		super(other.hashCode(), other);
		m_bModified = true;
	}

	/**
	 * Adds a new attribute to the VertexDescription.
	 * 
	 * @param semantics
	 *            Attribute semantics.
	 */
	public void addAttribute(int semantics) {
		if (hasAttribute(semantics))
			return;

		m_semanticsToIndexMap[semantics] = 0;// assign a value >= 0 to mark it
												// as existing
		_initMapping();
	}

	/**
	 * Removes given attribute.
	 * 
	 * @param semantics
	 *            Attribute semantics.
	 */
	void removeAttribute(int semantics) {

		if (semantics == Semantics.POSITION)
			throw new IllegalArgumentException(
					"Position attribue cannot be removed");// not allowed to
															// remove the xy

		if (!hasAttribute(semantics))
			return;

		m_semanticsToIndexMap[semantics] = -1;// assign a value < 0 to mark it
												// as removed
		_initMapping();
	}

	/**
	 * Removes all attributes from the designer with exception of the POSITION
	 * attribute.
	 */
	public void reset() {
		m_semantics[0] = Semantics.POSITION;
		m_attributeCount = 1;

		for (int i : m_semanticsToIndexMap)
			m_semanticsToIndexMap[i] = -1;

		m_semanticsToIndexMap[m_semantics[0]] = 0;
		m_bModified = true;
	}

	/**
	 * Returns a VertexDescription corresponding to the vertex design. <br>
	 * Note: the same instance of VertexDescription will be returned each time
	 * for the same same set of attributes and attribute properties. <br>
	 * The method searches for the VertexDescription in a global hash table. If
	 * found, it is returned. Else, a new instance of the VertexDescription is
	 * added to the has table and returned.
	 */
	public VertexDescription getDescription() {
		VertexDescriptionHash vdhash = VertexDescriptionHash.getInstance();
		VertexDescriptionDesignerImpl vdd = this;
		return vdhash.add(vdd);
	}

	/**
	 * Returns a default VertexDescription that has X and Y coordinates only.
	 */
	static VertexDescription getDefaultDescriptor2D() {
		VertexDescriptionHash vdhash = VertexDescriptionHash.getInstance();
		VertexDescription vd = vdhash.getVD2D();
		return vd;
	}

	/**
	 * Returns a default VertexDescription that has X, Y, and Z coordinates only
	 */
	static VertexDescription getDefaultDescriptor3D() {
		VertexDescriptionHash vdhash = VertexDescriptionHash.getInstance();
		VertexDescription vd = vdhash.getVD3D();
		return vd;
	}

	VertexDescription _createInternal() {
		int hash = hashCode();
		VertexDescription vd = new VertexDescription(hash, this);
		return vd;
	}

	protected boolean m_bModified;

	protected void _initMapping() {
		m_attributeCount = 0;
		for (int i = 0, j = 0; i < Semantics.MAXSEMANTICS; i++) {
			if (m_semanticsToIndexMap[i] >= 0) {
				m_semantics[j] = i;
				m_semanticsToIndexMap[i] = j;
				j++;
				m_attributeCount++;
			}
		}

		m_bModified = true;
	}

	@Override
	public int hashCode() {
		if (m_bModified) {
			m_hash = calculateHashImpl();
			m_bModified = false;
		}

		return m_hash;
	}

	@Override
	public boolean equals(Object _other) {
		if (_other == null)
			return false;
		if (_other == this)
			return true;
		if (_other.getClass() != getClass())
			return false;
		VertexDescriptionDesignerImpl other = (VertexDescriptionDesignerImpl) (_other);
		if (other.getAttributeCount() != getAttributeCount())
			return false;

		for (int i = 0; i < m_attributeCount; i++) {
			if (m_semantics[i] != other.m_semantics[i])
				return false;
		}
		if (m_bModified != other.m_bModified)
			return false;

		return true;
	}

	public boolean isDesignerFor(VertexDescription vd) {
		if (vd.getAttributeCount() != getAttributeCount())
			return false;

		for (int i = 0; i < m_attributeCount; i++) {
			if (m_semantics[i] != vd.m_semantics[i])
				return false;
		}

		return true;
	}
	
	// returns a mapping from the source attribute indices to the destination
	// attribute indices.
	static int[] mapAttributes(VertexDescription src, VertexDescription dest) {
		int[] srcToDst = new int[src.getAttributeCount()];
		Arrays.fill(srcToDst, -1);
		for (int i = 0, nsrc = src.getAttributeCount(); i < nsrc; i++) {
			srcToDst[i] = dest.getAttributeIndex(src.getSemantics(i));
		}
		return srcToDst;
	}

	static VertexDescription getMergedVertexDescription(VertexDescription src,
			int semanticsToAdd) {
		VertexDescriptionDesignerImpl vdd = new VertexDescriptionDesignerImpl(
				src);
		vdd.addAttribute(semanticsToAdd);
		return vdd.getDescription();
	}

	static VertexDescription getMergedVertexDescription(VertexDescription d1, VertexDescription d2) {
		VertexDescriptionDesignerImpl vdd = null;
		for (int semantics = Semantics.POSITION; semantics < Semantics.MAXSEMANTICS; semantics++) {
			if (!d1.hasAttribute(semantics) && d2.hasAttribute(semantics)) {
				if (vdd == null) {
					vdd = new VertexDescriptionDesignerImpl(d1);
				}
	
				vdd.addAttribute(semantics);
			}
		}
		
		if (vdd != null) {
			return vdd.getDescription();
		}
		
		return d1;
	}

	static VertexDescription removeSemanticsFromVertexDescription(
			VertexDescription src, int semanticsToRemove) {
		VertexDescriptionDesignerImpl vdd = new VertexDescriptionDesignerImpl(
				src);
		vdd.removeAttribute(semanticsToRemove);
		return vdd.getDescription();
	}
	
}

