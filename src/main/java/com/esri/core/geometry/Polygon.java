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

import java.io.Serializable;

import static com.esri.core.geometry.SizeOf.SIZE_OF_POLYGON;

/**
 * A polygon is a collection of one or many interior or exterior rings.
 */
public class Polygon extends MultiPath implements Serializable {
	private static final long serialVersionUID = 2L;// TODO:remove as we use
													// writeReplace and
													// GeometrySerializer

	/**
	 * Creates a polygon.
	 */
	public Polygon() {
		m_impl = new MultiPathImpl(true);
	}

	public Polygon(VertexDescription vd) {
		m_impl = new MultiPathImpl(true, vd);
	}

	@Override
	public Geometry createInstance() {
		return new Polygon(getDescription());
	}

	@Override
	public int getDimension() {
		return 2;
	}

	@Override
	public Geometry.Type getType() {
		return Type.Polygon;
	}

	@Override
	public long estimateMemorySize() {
		return SIZE_OF_POLYGON + m_impl.estimateMemorySize();
	}

	/**
	 * Calculates the ring area for this ring.
	 * 
	 * @param ringIndex
	 *            The index of this ring.
	 * @return The ring area for this ring.
	 */
	public double calculateRingArea2D(int ringIndex) {
		return m_impl.calculateRingArea2D(ringIndex);
	}

	/**
	 * Returns TRUE if the ring is an exterior ring. Valid only for simple
	 * polygons.
	 */
	public boolean isExteriorRing(int partIndex) {
		return m_impl.isExteriorRing(partIndex);
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

		return m_impl.equals(((Polygon) other)._getImpl());
	}

	/**
	 * Returns a hash code value for this polygon.
	 */

	@Override
	public int hashCode() {
		return m_impl.hashCode();
	}

	/**
	 * Sets a new vertex for the polygon.
	 * 
	 * @param i
	 *            The index of the new vertex.
	 * @param x
	 *            The X coordinate for the new vertex.
	 * @param y
	 *            The Y coordinate for the new vertex.
	 */
	public void setXY(int i, double x, double y) {
		m_impl.setXY(i, x, y);

	}

	public void interpolateAttributes(int path_index, int from_point_index,
			int to_point_index) {
		m_impl.interpolateAttributes(path_index, from_point_index,
				to_point_index);
	}

	public void interpolateAttributes(int semantics, int path_index,
			int from_point_index, int to_point_index) {
		m_impl.interpolateAttributesForSemantics(semantics, path_index,
				from_point_index, to_point_index);
	}

	public int getExteriorRingCount() {
		return m_impl.getOGCPolygonCount();
	}
	
	public interface FillRule {
		/**
		 * odd-even fill rule. This is the default value. A point is in the polygon
		 * interior if a ray from this point to infinity crosses odd number of segments
		 * of the polygon.
		 */
		public final static int enumFillRuleOddEven = 0;
		/**
		 * winding fill rule (aka non-zero winding rule). A point is in the polygon
		 * interior if a winding number is not zero. To compute a winding number for a
		 * point, draw a ray from this point to infinity. If N is the number of times
		 * the ray crosses segments directed up and the M is the number of times it
		 * crosses segments directed down, then the winding number is equal to N-M.
		 */
		public final static int enumFillRuleWinding = 1;
	};

	/**
	 * Fill rule for the polygon that defines the interior of the self intersecting
	 * polygon. It affects the Simplify operation. Can be use by drawing code to
	 * pass around the fill rule of graphic path. This property is not persisted in
	 * any format yet. See also Polygon.FillRule.
	 */
	public void setFillRule(int rule) {
		m_impl.setFillRule(rule);
	}

	/**
	 * Fill rule for the polygon that defines the interior of the self intersecting
	 * polygon. It affects the Simplify operation. Changing the fill rule on the
	 * polygon that has no self intersections has no physical effect. Can be use by
	 * drawing code to pass around the fill rule of graphic path. This property is
	 * not persisted in any format yet. See also Polygon.FillRule.
	 */
	public int getFillRule() {
		return m_impl.getFillRule();
	}
}
