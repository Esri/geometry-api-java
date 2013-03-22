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

public class QuadTree {
	public static final class QuadTreeIterator {
		/**
		 * Resets the iterator to an starting state on the Quad_tree. If the
		 * input Geometry is a Line segment, then the query will be the segment.
		 * Otherwise the query will be the Envelope_2D bounding the Geometry.
		 * \param query The Geometry used for the query. \param tolerance The
		 * tolerance used for the intersection tests.
		 */
		public void resetIterator(Geometry query, double tolerance) {
			m_impl.resetIterator(query, tolerance);
		}

		/**
		 * Resets the iterator to a starting state on the Quad_tree using the
		 * input Envelope_2D as the query. \param query The Envelope_2D used for
		 * the query. \param tolerance The tolerance used for the intersection
		 * tests.
		 */
		public void resetIterator(Envelope2D query, double tolerance) {
			m_impl.resetIterator(query, tolerance);
		}

		/**
		 * Moves the iterator to the next Element_handle and returns the
		 * Element_handle.
		 */
		public int next() {
			return m_impl.next();
		}

		/**
		 * Returns a void* to the impl class.
		 */
		Object getImpl_() {
			return m_impl;
		}

		// Creates an iterator on the input Quad_tree_impl. The query will be
		// the Envelope_2D bounding the input Geometry.
		private QuadTreeIterator(Object obj) {
			m_impl = (QuadTreeImpl.QuadTreeIteratorImpl) obj;
		}

		private QuadTreeImpl.QuadTreeIteratorImpl m_impl;
	};

	/**
	 * Creates a Quad_tree with the root having the extent of the input
	 * Envelope_2D, and height of the input height, where the root starts at
	 * height 0. Note that the height cannot be larger than 16 if on a 32 bit
	 * platform and 32 if on a 64 bit platform. \param extent The extent of the
	 * Quad_tree. \param height The max height of the Quad_tree.
	 */
	public QuadTree(Envelope2D extent, int height) {
		m_impl = new QuadTreeImpl(extent, height);
	}

	/**
	 * Inserts the element and bounding_box into the Quad_tree. Note that a copy
	 * will me made of the input bounding_box. Note that this will invalidate
	 * any active iterator on the Quad_tree. Returns an Element_handle
	 * corresponding to the element and bounding_box. \param element The element
	 * of the Geometry to be inserted. \param bounding_box The bounding_box of
	 * the Geometry to be inserted.
	 */
	public int insert(int element, Envelope2D bounding_box) {
		return m_impl.insert(element, bounding_box);
	}

	/**
	 * Inserts the element and bounding_box into the Quad_tree at the given
	 * quad_handle. Note that a copy will me made of the input bounding_box.
	 * Note that this will invalidate any active iterator on the Quad_tree.
	 * Returns an Element_handle corresponding to the element and bounding_box.
	 * \param element The element of the Geometry to be inserted. \param
	 * bounding_box The bounding_box of the Geometry to be inserted. \param
	 * hint_index A handle used as a hint where to place the element. This can
	 * be a handle obtained from a previous insertion and is useful on data
	 * having strong locality such as segments of a Polygon.
	 */
	public int insert(int element, Envelope2D bounding_box, int hint_index) {
		return m_impl.insert(element, bounding_box, hint_index);
	}

	/**
	 * Removes the element and bounding_box at the given element_handle. Note
	 * that this will invalidate any active iterator on the Quad_tree. \param
	 * element_handle The handle corresponding to the element and bounding_box
	 * to be removed.
	 */
	public void removeElement(int element_handle) {
		m_impl.removeElement(element_handle);
	}

	/**
	 * Returns the element at the given element_handle. \param element_handle
	 * The handle corresponding to the element to be retrieved.
	 */
	public int getElement(int element_handle) {
		return m_impl.getElement(element_handle);
	}

	/**
	 * Returns the height of the quad at the given quad_handle. \param
	 * quad_handle The handle corresponding to the quad.
	 */
	public int getHeight(int quad_handle) {
		return m_impl.getHeight(quad_handle);
	}

	/**
	 * Returns the extent of the quad at the given quad_handle. \param
	 * quad_handle The handle corresponding to the quad.
	 */
	public Envelope2D getExtent(int quad_handle) {
		return m_impl.getExtent(quad_handle);
	}

	/**
	 * Returns the Quad_handle of the quad containing the given element_handle.
	 * \param element_handle The handle corresponding to the element.
	 */
	public int getQuad(int element_handle) {
		return m_impl.getQuad(element_handle);
	}

	/**
	 * Returns the number of elements in the Quad_tree.
	 */
	public int getElementCount() {
		return m_impl.getElementCount();
	}

	/**
	 * Gets an iterator on the Quad_tree. The query will be the Envelope_2D that
	 * bounds the input Geometry. To reuse the existing iterator on the same
	 * Quad_tree but with a new query, use the reset_iterator function on the
	 * Quad_tree_iterator. \param query The Geometry used for the query. If the
	 * Geometry is a Line segment, then the query will be the segment. Otherwise
	 * the query will be the Envelope_2D bounding the Geometry. \param tolerance
	 * The tolerance used for the intersection tests.
	 */
	public QuadTreeIterator getIterator(Geometry query, double tolerance) {
		QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator(query,
				tolerance);
		return new QuadTreeIterator(iterator);
	}

	/**
	 * Gets an iterator on the Quad_tree using the input Envelope_2D as the
	 * query. To reuse the existing iterator on the same Quad_tree but with a
	 * new query, use the reset_iterator function on the Quad_tree_iterator.
	 * \param query The Envelope_2D used for the query. \param tolerance The
	 * tolerance used for the intersection tests.
	 */
	public QuadTreeIterator getIterator(Envelope2D query, double tolerance) {
		QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator(query,
				tolerance);
		return new QuadTreeIterator(iterator);
	}

	/**
	 * Gets an iterator on the Quad_tree.
	 */
	public QuadTreeIterator getIterator() {
		QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator();
		return new QuadTreeIterator(iterator);
	}

	/**
	 * Returns a void* to the impl class.
	 */
	Object getImpl_() {
		return m_impl;
	}

	private QuadTreeImpl m_impl;
}
