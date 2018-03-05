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

import static com.esri.core.geometry.SizeOf.SIZE_OF_MAPGEOMETRY;

import java.io.Serializable;

/**
 * The MapGeometry class bundles the geometry with its spatial reference
 * together. To work with a geometry object in a map it is necessary to have a
 * spatial reference defined for this geometry.
 */
public class MapGeometry implements Serializable {
	private static final long serialVersionUID = 1L;

	Geometry m_geometry = null;
	SpatialReference sr = null;

	/**
	 * Construct a MapGeometry instance using the specified geometry instance
	 * and its corresponding spatial reference.
	 * 
	 * @param g
	 *            The geometry to construct the new MapGeometry object.
	 * @param _sr
	 *            The spatial reference of the geometry.
	 */
	public MapGeometry(Geometry g, SpatialReference _sr) {
		m_geometry = g;
		sr = _sr;
	}

	/**
	 * Gets the only geometry without the spatial reference from the
	 * MapGeometry.
	 */
	public Geometry getGeometry() {
		return m_geometry;
	}

	/**
	 * Sets the geometry for this MapGeometry.
	 * 
	 * @param geometry
	 *            The geometry.
	 */

	public void setGeometry(Geometry geometry) {
		this.m_geometry = geometry;
	}

	/**
	 * Sets the spatial reference for this MapGeometry.
	 * 
	 * @param sr
	 *            The spatial reference.
	 */
	public void setSpatialReference(SpatialReference sr) {
		this.sr = sr;
	}

	/**
	 * Gets the spatial reference for this MapGeometry.
	 */
	public SpatialReference getSpatialReference() {
		return sr;
	}

	/**
	 * The output of this method can be only used for debugging. It is subject to change without notice. 
	 */
	@Override
	public String toString() {
		String snippet = OperatorExportToJson.local().execute(getSpatialReference(), getGeometry());
		if (snippet.length() > 200) { 
			return snippet.substring(0, 197) + "... ("+snippet.length()+" characters)"; 
		}
		else {
			return snippet;
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;

		if (other == this)
			return true;

		if (other.getClass() != getClass())
			return false;

		MapGeometry omg = (MapGeometry)other;
		SpatialReference sr = getSpatialReference();
		Geometry g = getGeometry();
		SpatialReference osr = omg.getSpatialReference();
		Geometry og = omg.getGeometry();
		
		if (sr != osr) {
			if (sr == null || !sr.equals(osr))
				return false;
		}

		if (g != og) {
			if (g == null || !g.equals(og))
				return false;
		}
		
		return true;
	}

	/**
	 * Returns an estimate of this object size in bytes.
	 * <p>
	 * This estimate doesn't include the size of the {@link SpatialReference} object
	 * because instances of {@link SpatialReference} are expected to be shared among
	 * geometry objects.
	 *
	 * @return Returns an estimate of this object size in bytes.
	 */
	public long estimateMemorySize() {
		long sz = SIZE_OF_MAPGEOMETRY;
		if (m_geometry != null)
			sz += m_geometry.estimateMemorySize();
		return sz;
	}
	
	@Override
	public int hashCode() {
		SpatialReference sr = getSpatialReference();
		Geometry g = getGeometry();
		int hc = 0x2937912;
		if (sr != null)
			hc ^= sr.hashCode();
		if (g != null)
			hc ^= g.hashCode();
		
		return hc;
	}
}
