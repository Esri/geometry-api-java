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

public class QuadTree implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final class QuadTreeIterator {
		/**
		 * Resets the iterator to an starting state on the QuadTree. If the
		 * input Geometry is a Line segment, then the query will be the segment.
		 * Otherwise the query will be the Envelope2D bounding the Geometry.
		 * \param query The Geometry used for the query.
		 * \param tolerance The tolerance used for the intersection tests.
		 */
		public void resetIterator(Geometry query, double tolerance) {
			if (!m_b_sorted)
				((QuadTreeImpl.QuadTreeIteratorImpl) m_impl).resetIterator(query, tolerance);
			else
				((QuadTreeImpl.QuadTreeSortedIteratorImpl) m_impl).resetIterator(query, tolerance);
		}

		/**
		 * Resets the iterator to a starting state on the QuadTree using the
		 * input Envelope2D as the query.
		 * \param query The Envelope2D used for the query.
		 * \param tolerance The tolerance used for the intersection
		 * tests.
		 */
		public void resetIterator(Envelope2D query, double tolerance) {
			if (!m_b_sorted)
				((QuadTreeImpl.QuadTreeIteratorImpl) m_impl).resetIterator(query, tolerance);
			else
				((QuadTreeImpl.QuadTreeSortedIteratorImpl) m_impl).resetIterator(query, tolerance);
		}

		/**
		 * Moves the iterator to the next Element_handle and returns the
		 * Element_handle.
		 */
		public int next() {
			if (!m_b_sorted)
				return ((QuadTreeImpl.QuadTreeIteratorImpl) m_impl).next();
			else
				return ((QuadTreeImpl.QuadTreeSortedIteratorImpl) m_impl).next();
		}

		/**
		 * Returns a void* to the impl class.
		 */
		Object getImpl_() {
			return m_impl;
		}

		// Creates an iterator on the input QuadTreeImpl. The query will be
		// the Envelope2D bounding the input Geometry.
		private QuadTreeIterator(Object obj, boolean bSorted) {

			m_impl = obj;
			m_b_sorted = bSorted;
		}

		private Object m_impl;
		private boolean m_b_sorted;
	}

	/**
	 * Creates a QuadTree with the root having the extent of the input
	 * Envelope2D, and height of the input height, where the root starts at height 0.
	 * \param extent The extent of the QuadTree.
	 * \param height The max height of the QuadTree.
	 */
	public QuadTree(Envelope2D extent, int height) {
		m_impl = new QuadTreeImpl(extent, height);
	}

	/**
	 * Creates a QuadTree with the root having the extent of the input Envelope2D, and height of the input height, where the root starts at height 0.
	 * \param extent The extent of the QuadTreeImpl.
	 * \param height The max height of the QuadTreeImpl.
	 * \param bStoreDuplicates Put true to place elements deeper into the quad tree at intesecting quads, duplicates will be stored. Put false to only place elements into quads that can contain it..
	 */
	public QuadTree(Envelope2D extent, int height, boolean bStoreDuplicates) {
		m_impl = new QuadTreeImpl(extent, height, bStoreDuplicates);
	}

	/**
	 * Inserts the element and bounding_box into the QuadTree. Note that a copy
	 * will me made of the input bounding_box. Note that this will invalidate
	 * any active iterator on the QuadTree. Returns an Element_handle
	 * corresponding to the element and bounding_box.
	 * \param element The element of the Geometry to be inserted.
	 * \param bounding_box The bounding_box of
	 * the Geometry to be inserted.
	 */
	public int insert(int element, Envelope2D boundingBox) {
		return m_impl.insert(element, boundingBox);
	}

	/**
	 * Inserts the element and bounding_box into the QuadTree at the given
	 * quad_handle. Note that a copy will me made of the input bounding_box.
	 * Note that this will invalidate any active iterator on the QuadTree.
	 * Returns an Element_handle corresponding to the element and bounding_box.
	 * \param element The element of the Geometry to be inserted.
	 * \param bounding_box The bounding_box of the Geometry to be inserted.
	 * \param hint_index A handle used as a hint where to place the element. This can
	 * be a handle obtained from a previous insertion and is useful on data
	 * having strong locality such as segments of a Polygon.
	 */
	public int insert(int element, Envelope2D boundingBox, int hintIndex) {
		return m_impl.insert(element, boundingBox, hintIndex);
	}

	/**
	 * Removes the element and bounding_box at the given element_handle. Note
	 * that this will invalidate any active iterator on the QuadTree.
	 * \param element_handle The handle corresponding to the element and bounding_box
	 * to be removed.
	 */
	public void removeElement(int elementHandle) {
		m_impl.removeElement(elementHandle);
	}

	/**
	 * Returns the element at the given element_handle.
	 * \param element_handle The handle corresponding to the element to be retrieved.
	 */
	public int getElement(int elementHandle) {
		return m_impl.getElement(elementHandle);
	}

	/**
	 * Returns the element extent at the given element_handle.
	 * \param element_handle The handle corresponding to the element extent to be retrieved.
	 */
	public Envelope2D getElementExtent(int elementHandle) {
		return m_impl.getElementExtent(elementHandle);
	}

	/**
	 * Returns the extent of all elements in the quad tree.
	 */
	public Envelope2D getDataExtent() {
		return m_impl.getDataExtent();
	}

	/**
	 * Returns the extent of the quad tree.
	 */
	public Envelope2D getQuadTreeExtent() {
		return m_impl.getQuadTreeExtent();
	}

	/**
	 * Returns the number of elements in the subtree rooted at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	public int getSubTreeElementCount(int quadHandle) {
		return m_impl.getSubTreeElementCount(quadHandle);
	}

	/**
	 * Returns the number of elements contained in the subtree rooted at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	public int getContainedSubTreeElementCount(int quadHandle) {
		return m_impl.getContainedSubTreeElementCount(quadHandle);
	}

	/**
	 * Returns the number of elements in the quad tree that intersect the qiven query. Some elements may be duplicated if the quad tree stores duplicates.
	 * \param query The Envelope2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 * \param max_count If the intersection count becomes greater than or equal to the max_count, then max_count is returned.
	 */
	public int getIntersectionCount(Envelope2D query, double tolerance, int maxCount) {
		return m_impl.getIntersectionCount(query, tolerance, maxCount);
	}

	/**
	 * Returns true if the quad tree has data intersecting the given query.
	 * \param query The Envelope2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	public boolean hasData(Envelope2D query, double tolerance) {
		return m_impl.hasData(query, tolerance);
	}

	/**
	 * Returns the height of the quad at the given quad_handle. \param
	 * quad_handle The handle corresponding to the quad.
	 */
	public int getHeight(int quadHandle) {
		return m_impl.getHeight(quadHandle);
	}

	/**
	 * Returns the max height the quad tree can grow to.
	 */
	public int getMaxHeight() {
		return m_impl.getMaxHeight();
	}

	/**
	 * Returns the extent of the quad at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	public Envelope2D getExtent(int quadHandle) {
		return m_impl.getExtent(quadHandle);
	}

	/**
	 * Returns the Quad_handle of the quad containing the given element_handle.
	 * \param element_handle The handle corresponding to the element.
	 */
	public int getQuad(int elementHandle) {
		return m_impl.getQuad(elementHandle);
	}

	/**
	 * Returns the number of elements in the QuadTree.
	 */
	public int getElementCount() {
		return m_impl.getElementCount();
	}

	/**
	 * Gets an iterator on the QuadTree. The query will be the Envelope2D that
	 * bounds the input Geometry. To reuse the existing iterator on the same
	 * QuadTree but with a new query, use the reset_iterator function on the
	 * QuadTree_iterator.
	 * \param query The Geometry used for the query. If the
	 * Geometry is a Line segment, then the query will be the segment. Otherwise
	 * the query will be the Envelope2D bounding the Geometry.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	public QuadTreeIterator getIterator(Geometry query, double tolerance) {
		QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator(query, tolerance);
		return new QuadTreeIterator(iterator, false);
	}

	/**
	 * Gets an iterator on the QuadTree using the input Envelope2D as the
	 * query. To reuse the existing iterator on the same QuadTree but with a
	 * new query, use the reset_iterator function on the QuadTree_iterator.
	 * \param query The Envelope2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	public QuadTreeIterator getIterator(Envelope2D query, double tolerance) {
		QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator(query, tolerance);
		return new QuadTreeIterator(iterator, false);
	}

	/**
	 * Gets an iterator on the QuadTree.
	 */
	public QuadTreeIterator getIterator() {
		QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator();
		return new QuadTreeIterator(iterator, false);
	}

	/**
	 * Gets an iterator on the QuadTree. The query will be the Envelope2D that bounds the input Geometry.
	 * To reuse the existing iterator on the same QuadTree but with a new query, use the reset_iterator function on the QuadTree_iterator.
	 * \param query The Geometry used for the query. If the Geometry is a Line segment, then the query will be the segment. Otherwise the query will be the Envelope2D bounding the Geometry.
	 * \param tolerance The tolerance used for the intersection tests.
	 * \param bSorted Put true to iterate the quad tree in the order of the Element_types.
	 */
	public QuadTreeIterator getIterator(Geometry query, double tolerance, boolean bSorted) {
		if (!bSorted) {
			QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator(query, tolerance);
			return new QuadTreeIterator(iterator, false);
		} else {
			QuadTreeImpl.QuadTreeSortedIteratorImpl iterator = m_impl.getSortedIterator(query, tolerance);
			return new QuadTreeIterator(iterator, true);
		}
	}

	/**
	 * Gets an iterator on the QuadTree using the input Envelope2D as the query.
	 * To reuse the existing iterator on the same QuadTree but with a new query, use the reset_iterator function on the QuadTree_iterator.
	 * \param query The Envelope2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 * \param bSorted Put true to iterate the quad tree in the order of the Element_types.
	 */
	public QuadTreeIterator getIterator(Envelope2D query, double tolerance, boolean bSorted) {
		if (!bSorted) {
			QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator(query, tolerance);
			return new QuadTreeIterator(iterator, false);
		} else {
			QuadTreeImpl.QuadTreeSortedIteratorImpl iterator = m_impl.getSortedIterator(query, tolerance);
			return new QuadTreeIterator(iterator, true);
		}
	}

	/**
	 * Gets an iterator on the QuadTree.
	 * \param bSorted Put true to iterate the quad tree in the order of the Element_types.
	 */
	public QuadTreeIterator getIterator(boolean bSorted) {
		if (!bSorted) {
			QuadTreeImpl.QuadTreeIteratorImpl iterator = m_impl.getIterator();
			return new QuadTreeIterator(iterator, false);
		} else {
			QuadTreeImpl.QuadTreeSortedIteratorImpl iterator = m_impl.getSortedIterator();
			return new QuadTreeIterator(iterator, true);
		}
	}

	/**
	 * Returns a void* to the impl class.
	 */
	Object getImpl_() {
		return m_impl;
	}

	private QuadTreeImpl m_impl;
}
