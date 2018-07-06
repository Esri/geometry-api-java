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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

class QuadTreeImpl implements Serializable {
	private static final long serialVersionUID = 1L;
	
	static final class QuadTreeIteratorImpl {
		/**
		 * Resets the iterator to an starting state on the Quad_tree_impl. If
		 * the input Geometry is a Line segment, then the query will be the
		 * segment. Otherwise the query will be the Envelope_2D bounding the
		 * Geometry. \param query The Geometry used for the query. \param
		 * tolerance The tolerance used for the intersection tests. \param
		 * tolerance The tolerance used for the intersection tests.
		 */
		void resetIterator(Geometry query, double tolerance) {
			m_quads_stack.resize(0);
			m_extents_stack.clear();
			m_current_element_handle = -1;
			query.queryLooseEnvelope2D(m_query_box);
			m_query_box.inflate(tolerance, tolerance);

			if (m_quad_tree.m_root != -1 && m_query_box.isIntersecting(m_quad_tree.m_extent)) {
				int type = query.getType().value();
				m_b_linear = Geometry.isSegment(type);

				if (m_b_linear) {
					Segment segment = (Segment) query;
					m_query_start = segment.getStartXY();
					m_query_end = segment.getEndXY();
					m_tolerance = tolerance;
				} else {
					m_tolerance = NumberUtils.NaN(); // we don't need it
				}

				m_quads_stack.add(m_quad_tree.m_root);
				m_extents_stack.add(m_quad_tree.m_extent);
				m_next_element_handle = m_quad_tree.get_first_element_(m_quad_tree.m_root);
			} else
				m_next_element_handle = -1;
		}

		/**
		 * Resets the iterator to a starting state on the Quad_tree_impl using
		 * the input Envelope_2D as the query. \param query The Envelope_2D used
		 * for the query. \param tolerance The tolerance used for the
		 * intersection tests.
		 */
		void resetIterator(Envelope2D query, double tolerance) {
			m_quads_stack.resize(0);
			m_extents_stack.clear();
			m_current_element_handle = -1;
			m_query_box.setCoords(query);
			m_query_box.inflate(tolerance, tolerance);
			m_tolerance = NumberUtils.NaN(); // we don't need it

			if (m_quad_tree.m_root != -1 && m_query_box.isIntersecting(m_quad_tree.m_extent)) {
				m_quads_stack.add(m_quad_tree.m_root);
				m_extents_stack.add(m_quad_tree.m_extent);
				m_next_element_handle = m_quad_tree.get_first_element_(m_quad_tree.m_root);
				m_b_linear = false;
			} else
				m_next_element_handle = -1;
		}

		/**
		 * Moves the iterator to the next int and returns the int.
		 */
		int next() {
			// If the node stack is empty, then we've exhausted our search

			if (m_quads_stack.size() == 0)
				return -1;

			m_current_element_handle = m_next_element_handle;

			Point2D start = null;
			Point2D end = null;
			Envelope2D bounding_box;
			Envelope2D extent_inf = null;
			Envelope2D[] child_extents = null;

			if (m_b_linear) {
				start = new Point2D();
				end = new Point2D();
				extent_inf = new Envelope2D();
			}

			boolean b_found_hit = false;
			while (!b_found_hit) {
				while (m_current_element_handle != -1) {
					int current_data_handle = m_quad_tree.get_data_(m_current_element_handle);
					bounding_box = m_quad_tree.get_bounding_box_value_(current_data_handle);

					if (bounding_box.isIntersecting(m_query_box)) {
						if (m_b_linear) {
							start.setCoords(m_query_start);
							end.setCoords(m_query_end);
							extent_inf.setCoords(bounding_box);

							extent_inf.inflate(m_tolerance, m_tolerance);
							if (extent_inf.clipLine(start, end) > 0) {
								b_found_hit = true;
								break;
							}
						} else {
							b_found_hit = true;
							break;
						}
					}

					// get next element_handle
					m_current_element_handle = m_quad_tree.get_next_element_(m_current_element_handle);
				}

				// If m_current_element_handle equals -1, then we've exhausted our search in the current quadtree node
				if (m_current_element_handle == -1) {
					// get the last node from the stack and add the children whose extent intersects m_query_box
					int current_quad = m_quads_stack.getLast();
					Envelope2D current_extent = m_extents_stack.get(m_extents_stack.size() - 1);

					if (child_extents == null) {
						child_extents = new Envelope2D[4];
						child_extents[0] = new Envelope2D();
						child_extents[1] = new Envelope2D();
						child_extents[2] = new Envelope2D();
						child_extents[3] = new Envelope2D();
					}

					set_child_extents_(current_extent, child_extents);
					m_quads_stack.removeLast();
					m_extents_stack.remove(m_extents_stack.size() - 1);

					for (int quadrant = 0; quadrant < 4; quadrant++) {
						int child_handle = m_quad_tree.get_child_(current_quad, quadrant);

						if (child_handle != -1 && m_quad_tree.getSubTreeElementCount(child_handle) > 0) {
							if (child_extents[quadrant].isIntersecting(m_query_box)) {
								if (m_b_linear) {
									start.setCoords(m_query_start);
									end.setCoords(m_query_end);

									extent_inf.setCoords(child_extents[quadrant]);
									extent_inf.inflate(m_tolerance, m_tolerance);
									if (extent_inf.clipLine(start, end) > 0) {
										Envelope2D child_extent = new Envelope2D();
										child_extent.setCoords(child_extents[quadrant]);
										m_quads_stack.add(child_handle);
										m_extents_stack.add(child_extent);
									}
								} else {
									Envelope2D child_extent = new Envelope2D();
									child_extent.setCoords(child_extents[quadrant]);
									m_quads_stack.add(child_handle);
									m_extents_stack.add(child_extent);
								}
							}
						}
					}

					assert (m_quads_stack.size() <= 4 * (m_quad_tree.m_height - 1));

					if (m_quads_stack.size() == 0)
						return -1;

					m_current_element_handle = m_quad_tree.get_first_element_(m_quads_stack.get(m_quads_stack.size() - 1));
				}
			}

			// We did not exhaust our search in the current node, so we return
			// the element at m_current_element_handle in m_element_nodes

			m_next_element_handle = m_quad_tree.get_next_element_(m_current_element_handle);
			return m_current_element_handle;
		}

		// Creates an iterator on the input Quad_tree_impl. The query will be
		// the Envelope_2D bounding the input Geometry.
		QuadTreeIteratorImpl(QuadTreeImpl quad_tree_impl, Geometry query, double tolerance) {
			m_quad_tree = quad_tree_impl;
			m_query_box = new Envelope2D();
			m_quads_stack = new AttributeStreamOfInt32(0);
			m_extents_stack = new ArrayList<Envelope2D>(0);
			resetIterator(query, tolerance);
		}

		// Creates an iterator on the input Quad_tree_impl using the input
		// Envelope_2D as the query.
		QuadTreeIteratorImpl(QuadTreeImpl quad_tree_impl, Envelope2D query, double tolerance) {
			m_quad_tree = quad_tree_impl;
			m_query_box = new Envelope2D();
			m_quads_stack = new AttributeStreamOfInt32(0);
			m_extents_stack = new ArrayList<Envelope2D>(0);
			resetIterator(query, tolerance);
		}

		// Creates an iterator on the input Quad_tree_impl.
		QuadTreeIteratorImpl(QuadTreeImpl quad_tree_impl) {
			m_quad_tree = quad_tree_impl;
			m_query_box = new Envelope2D();
			m_quads_stack = new AttributeStreamOfInt32(0);
			m_extents_stack = new ArrayList<Envelope2D>(0);
		}

		private boolean m_b_linear;
		private Point2D m_query_start;
		private Point2D m_query_end;
		private Envelope2D m_query_box;
		private double m_tolerance;
		private int m_current_element_handle;
		private int m_next_element_handle;
		private QuadTreeImpl m_quad_tree;
		private AttributeStreamOfInt32 m_quads_stack;
		private ArrayList<Envelope2D> m_extents_stack; // this won't grow bigger than 4 * (m_quad_tree->m_height - 1)
	}

	static final class QuadTreeSortedIteratorImpl {
		/**
		 * Resets the iterator to a starting state on the Quad_tree_impl. If the input Geometry is a Line segment, then the query will be the segment. Otherwise the query will be the Envelope_2D bounding the Geometry.
		 * \param query The Geometry used for the query.
		 * \param tolerance The tolerance used for the intersection tests.
		 * \param tolerance The tolerance used for the intersection tests.
		 */
		void resetIterator(Geometry query, double tolerance) {
			m_quad_tree_iterator_impl.resetIterator(query, tolerance);
			m_sorted_handles.resize(0);
			m_index = -1;
		}

		/**
		 * Resets the iterator to a starting state on the Quad_tree_impl using the input Envelope_2D as the query.
		 * \param query The Envelope_2D used for the query.
		 * \param tolerance The tolerance used for the intersection tests.
		 */
		void resetIterator(Envelope2D query, double tolerance) {
			m_quad_tree_iterator_impl.resetIterator(query, tolerance);
			m_sorted_handles.resize(0);
			m_index = -1;
		}

		/**
		 * Moves the iterator to the next Element_handle and returns the Element_handle.
		 */
		int next() {
			if (m_index == -1) {
				int element_handle = -1;
				while ((element_handle = m_quad_tree_iterator_impl.next()) != -1)
					m_sorted_handles.add(element_handle);

				m_bucket_sort.sort(m_sorted_handles, 0, m_sorted_handles.size(), new Sorter(m_quad_tree_iterator_impl.m_quad_tree));
			}

			if (m_index == m_sorted_handles.size() - 1)
				return -1;

			m_index++;
			return m_sorted_handles.get(m_index);
		}

		//Creates a sorted iterator on the input Quad_tree_iterator_impl
		QuadTreeSortedIteratorImpl(QuadTreeIteratorImpl quad_tree_iterator_impl) {
			m_bucket_sort = new BucketSort();
			m_sorted_handles = new AttributeStreamOfInt32(0);
			m_quad_tree_iterator_impl = quad_tree_iterator_impl;
			m_index = -1;
		}

		private class Sorter extends ClassicSort {
			public Sorter(QuadTreeImpl quad_tree) {
				m_quad_tree = quad_tree;
			}

			@Override
			public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
				indices.sort(begin, end);
			}

			@Override
			public double getValue(int e) {
				return m_quad_tree.getElement(e);
			}

			private QuadTreeImpl m_quad_tree;
		}

		private BucketSort m_bucket_sort;
		private AttributeStreamOfInt32 m_sorted_handles;
		private QuadTreeIteratorImpl m_quad_tree_iterator_impl;
		int m_index;
	}

	/**
	 * Creates a Quad_tree_impl with the root having the extent of the input Envelope_2D, and height of the input height, where the root starts at height 0.
	 * \param extent The extent of the Quad_tree_impl.
	 * \param height The max height of the Quad_tree_impl.
	 */
	QuadTreeImpl(Envelope2D extent, int height) {
		m_quad_tree_nodes = new StridedIndexTypeCollection(10);
		m_element_nodes = new StridedIndexTypeCollection(4);
		m_data = new ArrayList<Data>(0);
		m_free_data = new AttributeStreamOfInt32(0);
		m_b_store_duplicates = false;

		m_extent = new Envelope2D();
		m_data_extent = new Envelope2D();

		reset_(extent, height);
	}

	/**
	 * Creates a Quad_tree_impl with the root having the extent of the input Envelope_2D, and height of the input height, where the root starts at height 0.
	 * \param extent The extent of the Quad_tree_impl.
	 * \param height The max height of the Quad_tree_impl.
	 * \param b_store_duplicates Put true to place elements deeper into the quad tree at intesecting quads, duplicates will be stored. Put false to only place elements into quads that can contain it.
	 */
	QuadTreeImpl(Envelope2D extent, int height, boolean b_store_duplicates) {
		m_quad_tree_nodes = (b_store_duplicates ? new StridedIndexTypeCollection(11) : new StridedIndexTypeCollection(10));
		m_element_nodes = new StridedIndexTypeCollection(4);
		m_data = new ArrayList<Data>(0);
		m_free_data = new AttributeStreamOfInt32(0);
		m_b_store_duplicates = b_store_duplicates;

		m_extent = new Envelope2D();
		m_data_extent = new Envelope2D();

		reset_(extent, height);
	}

	/**
	 * Resets the Quad_tree_impl to the given extent and height.
	 * \param extent The extent of the Quad_tree_impl.
	 * \param height The max height of the Quad_tree_impl.
	 */
	void reset(Envelope2D extent, int height) {
		m_quad_tree_nodes.deleteAll(false);
		m_element_nodes.deleteAll(false);
		m_data.clear();
		m_free_data.clear(false);
		reset_(extent, height);
	}

	/**
	 * Inserts the element and bounding_box into the Quad_tree_impl.
	 * Note that this will invalidate any active iterator on the Quad_tree_impl.
	 * Returns an Element_handle corresponding to the element and bounding_box.
	 * \param element The element of the Geometry to be inserted.
	 * \param bounding_box The bounding_box of the Geometry to be inserted.
	 */
	int insert(int element, Envelope2D bounding_box) {
		if (m_root == -1)
			create_root_();

		if (m_b_store_duplicates) {
			int success = insert_duplicates_(element, bounding_box, 0, m_extent, m_root, false, -1);

			if (success != -1) {
				if (m_data_extent.isEmpty())
					m_data_extent.setCoords(bounding_box);
				else
					m_data_extent.merge(bounding_box);
			}

			return success;
		}

		int element_handle = insert_(element, bounding_box, 0, m_extent, m_root, false, -1);

		if (element_handle != -1) {
			if (m_data_extent.isEmpty())
				m_data_extent.setCoords(bounding_box);
			else
				m_data_extent.merge(bounding_box);
		}

		return element_handle;
	}

	/**
	 * Inserts the element and bounding_box into the Quad_tree_impl at the given quad_handle.
	 * Note that this will invalidate any active iterator on the Quad_tree_impl.
	 * Returns an Element_handle corresponding to the element and bounding_box.
	 * \param element The element of the Geometry to be inserted.
	 * \param bounding_box The bounding_box of the Geometry to be inserted.
	 * \param hint_index A handle used as a hint where to place the element. This can be a handle obtained from a previous insertion and is useful on data having strong locality such as segments of a Polygon.
	 */
	int insert(int element, Envelope2D bounding_box, int hint_index) {
		if (m_root == -1)
			create_root_();

		if (m_b_store_duplicates) {
			int success = insert_duplicates_(element, bounding_box, 0, m_extent, m_root, false, -1);

			if (success != -1) {
				if (m_data_extent.isEmpty())
					m_data_extent.setCoords(bounding_box);
				else
					m_data_extent.merge(bounding_box);
			}
			return success;
		}

		int quad_handle;

		if (hint_index == -1)
			quad_handle = m_root;
		else
			quad_handle = get_quad_(hint_index);

		int quad_height = getHeight(quad_handle);
		Envelope2D quad_extent = getExtent(quad_handle);

		int element_handle = insert_(element, bounding_box, quad_height, quad_extent, quad_handle, false, -1);

		if (element_handle != -1) {
			if (m_data_extent.isEmpty())
				m_data_extent.setCoords(bounding_box);
			else
				m_data_extent.merge(bounding_box);
		}

		return element_handle;
	}

	/**
	 * Removes the element and bounding_box at the given element_handle.
	 * Note that this will invalidate any active iterator on the Quad_tree_impl.
	 * \param element_handle The handle corresponding to the element and bounding_box to be removed.
	 */
	void removeElement(int element_handle) {
		if (m_b_store_duplicates)
			throw new GeometryException("invalid call");

		int quad_handle = get_quad_(element_handle);
		disconnect_element_handle_(element_handle);
		free_element_and_box_node_(element_handle);

		int q = quad_handle;

		while (q != -1) {
			set_sub_tree_element_count_(q, get_sub_tree_element_count_(q) - 1);
			int parent = get_parent_(q);

			if (get_sub_tree_element_count_(q) == 0) {
				assert (get_local_element_count_(q) == 0);

				if (q != m_root) {
					int quadrant = get_quadrant_(q);
					m_quad_tree_nodes.deleteElement(q);
					set_child_(parent, quadrant, -1);
				}
			}

			q = parent;
		}
	}

	/**
	 * Returns the element at the given element_handle.
	 * \param element_handle The handle corresponding to the element to be retrieved.
	 */
	int getElement(int element_handle) {
		return get_element_value_(get_data_(element_handle));
	}

	/**
	 * Returns the ith unique element.
	 * \param i The index corresponding to the ith unique element.
	 */
	int getElementAtIndex(int i) {
		return m_data.get(i).element;
	}

	/**
	 * Returns the element extent at the given element_handle.
	 * \param element_handle The handle corresponding to the element extent to be retrieved.
	 */
	Envelope2D getElementExtent(int element_handle) {
		int data_handle = get_data_(element_handle);
		return get_bounding_box_value_(data_handle);
	}

	/**
	 * Returns the extent of the ith unique element.
	 * \param i The index corresponding to the ith unique element.
	 */
	Envelope2D getElementExtentAtIndex(int i) {
		return m_data.get(i).box;
	}

	/**
	 * Returns the extent of all elements in the quad tree.
	 */
	Envelope2D getDataExtent() {
		return m_data_extent;
	}

	/**
	 * Returns the extent of the quad tree.
	 */
	Envelope2D getQuadTreeExtent() {
		return m_extent;
	}

	/**
	 * Returns the height of the quad at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	int getHeight(int quad_handle) {
		return get_height_(quad_handle);
	}

	int getMaxHeight() {
		return m_height;
	}

	/**
	 * Returns the extent of the quad at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	Envelope2D getExtent(int quad_handle) {
		Envelope2D quad_extent = new Envelope2D();
		quad_extent.setCoords(m_extent);

		if (quad_handle == m_root)
			return quad_extent;

		AttributeStreamOfInt32 quadrants = new AttributeStreamOfInt32(0);

		int q = quad_handle;

		do {
			quadrants.add(get_quadrant_(q));
			q = get_parent_(q);

		} while (q != m_root);

		int sz = quadrants.size();
		assert (sz == getHeight(quad_handle));

		for (int i = 0; i < sz; i++) {
			int child = quadrants.getLast();
			quadrants.removeLast();

			if (child == 0) {//northeast
				quad_extent.xmin = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymin = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			} else if (child == 1) {//northwest
				quad_extent.xmax = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymin = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			} else if (child == 2) {//southwest
				quad_extent.xmax = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymax = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			} else {//southeast
				quad_extent.xmin = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymax = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			}
		}

		return quad_extent;
	}

	/**
	 * Returns the Quad_handle of the quad containing the given element_handle.
	 * \param element_handle The handle corresponding to the element.
	 */
	int getQuad(int element_handle) {
		return get_quad_(element_handle);
	}

	/**
	 * Returns the number of elements in the Quad_tree_impl.
	 */
	int getElementCount() {
		if (m_root == -1)
			return 0;

		assert (get_sub_tree_element_count_(m_root) == m_data.size());
		return get_sub_tree_element_count_(m_root);
	}

	/**
	 * Returns the number of elements in the subtree rooted at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	int getSubTreeElementCount(int quad_handle) {
		return get_sub_tree_element_count_(quad_handle);
	}

	/**
	 * Returns the number of elements contained in the subtree rooted at the given quad_handle.
	 * \param quad_handle The handle corresponding to the quad.
	 */
	int getContainedSubTreeElementCount(int quad_handle) {
		if (!m_b_store_duplicates)
			return get_sub_tree_element_count_(quad_handle);

		return get_contained_sub_tree_element_count_(quad_handle);
	}

	/**
	 * Returns the number of elements in the quad tree that intersect the qiven query. Some elements may be duplicated if the quad tree stores duplicates.
	 * \param query The Envelope_2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 * \param max_count If the intersection count becomes greater than or equal to the max_count, then max_count is returned.
	 */
	int getIntersectionCount(Envelope2D query, double tolerance, int max_count) {
		if (m_root == -1)
			return 0;

		Envelope2D query_inflated = new Envelope2D();
		query_inflated.setCoords(query);
		query_inflated.inflate(tolerance, tolerance);

		AttributeStreamOfInt32 quads_stack = new AttributeStreamOfInt32(0);
		ArrayList<Envelope2D> extents_stack = new ArrayList<Envelope2D>(0);
		quads_stack.add(m_root);
		extents_stack.add(new Envelope2D(m_extent.xmin, m_extent.ymin, m_extent.xmax, m_extent.ymax));

		Envelope2D[] child_extents = new Envelope2D[4];
		child_extents[0] = new Envelope2D();
		child_extents[1] = new Envelope2D();
		child_extents[2] = new Envelope2D();
		child_extents[3] = new Envelope2D();

		Envelope2D current_extent = new Envelope2D();

		int intersection_count = 0;

		while (quads_stack.size() > 0) {
			boolean b_subdivide = false;

			int current_quad_handle = quads_stack.getLast();
			current_extent.setCoords(extents_stack.get(extents_stack.size() - 1));

			quads_stack.removeLast();
			extents_stack.remove(extents_stack.size() - 1);


			if (query_inflated.contains(current_extent)) {
				intersection_count += getSubTreeElementCount(current_quad_handle);

				if (max_count > 0 && intersection_count >= max_count)
					return max_count;
			} else {
				if (query_inflated.isIntersecting(current_extent)) {
					for (int element_handle = get_first_element_(current_quad_handle); element_handle != -1; element_handle = get_next_element_(element_handle)) {
						int data_handle = get_data_(element_handle);
						Envelope2D env = get_bounding_box_value_(data_handle);

						if (env.isIntersecting(query_inflated)) {
							intersection_count++;

							if (max_count > 0 && intersection_count >= max_count)
								return max_count;
						}
					}

					b_subdivide = getHeight(current_quad_handle) + 1 <= m_height;
				}
			}

			if (b_subdivide) {
				set_child_extents_(current_extent, child_extents);

				for (int i = 0; i < 4; i++) {
					int child_handle = get_child_(current_quad_handle, i);

					if (child_handle != -1 && getSubTreeElementCount(child_handle) > 0) {
						boolean b_is_intersecting = query_inflated.isIntersecting(child_extents[i]);

						if (b_is_intersecting) {
							quads_stack.add(child_handle);
							extents_stack.add(new Envelope2D(child_extents[i].xmin, child_extents[i].ymin, child_extents[i].xmax, child_extents[i].ymax));
						}
					}
				}
			}
		}

		return intersection_count;
	}

	/**
	 * Returns true if the quad tree has data intersecting the given query.
	 * \param query The Envelope_2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	boolean hasData(Envelope2D query, double tolerance) {
		int count = getIntersectionCount(query, tolerance, 1);
		return count >= 1;
	}

	/**
	 * Gets an iterator on the Quad_tree_impl. The query will be the Envelope_2D
	 * that bounds the input Geometry. To reuse the existing iterator on the
	 * same Quad_tree_impl but with a new query, use the reset_iterator function
	 * on the Quad_tree_iterator_impl. \param query The Geometry used for the
	 * query. If the Geometry is a Line segment, then the query will be the
	 * segment. Otherwise the query will be the Envelope_2D bounding the
	 * Geometry. \param tolerance The tolerance used for the intersection tests.
	 */
	QuadTreeIteratorImpl getIterator(Geometry query, double tolerance) {
		return new QuadTreeIteratorImpl(this, query, tolerance);
	}

	/**
	 * Gets an iterator on the Quad_tree_impl using the input Envelope_2D as the
	 * query. To reuse the existing iterator on the same Quad_tree_impl but with
	 * a new query, use the reset_iterator function on the
	 * Quad_tree_iterator_impl. \param query The Envelope_2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	QuadTreeIteratorImpl getIterator(Envelope2D query, double tolerance) {
		return new QuadTreeIteratorImpl(this, query, tolerance);
	}

	/**
	 * Gets an iterator on the Quad_tree.
	 */
	QuadTreeIteratorImpl getIterator() {
		return new QuadTreeIteratorImpl(this);
	}

	/**
	 * Gets a sorted iterator on the Quad_tree_impl. The Element_handles will be returned in increasing order of their corresponding Element_types.
	 * The query will be the Envelope_2D that bounds the input Geometry.
	 * To reuse the existing iterator on the same Quad_tree_impl but with a new query, use the reset_iterator function on the Quad_tree_sorted_iterator_impl.
	 * \param query The Geometry used for the query. If the Geometry is a Line segment, then the query will be the segment. Otherwise the query will be the Envelope_2D bounding the Geometry.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	QuadTreeSortedIteratorImpl getSortedIterator(Geometry query, double tolerance) {
		return new QuadTreeSortedIteratorImpl(getIterator(query, tolerance));
	}

	/**
	 * Gets a sorted iterator on the Quad_tree_impl using the input Envelope_2D as the query. The Element_handles will be returned in increasing order of their corresponding Element_types.
	 * To reuse the existing iterator on the same Quad_tree_impl but with a new query, use the reset_iterator function on the Quad_tree_iterator_impl.
	 * \param query The Envelope_2D used for the query.
	 * \param tolerance The tolerance used for the intersection tests.
	 */
	QuadTreeSortedIteratorImpl getSortedIterator(Envelope2D query, double tolerance) {
		return new QuadTreeSortedIteratorImpl(getIterator(query, tolerance));
	}

	/**
	 * Gets a sorted iterator on the Quad_tree. The Element_handles will be returned in increasing order of their corresponding Element_types
	 */
	QuadTreeSortedIteratorImpl getSortedIterator() {
		return new QuadTreeSortedIteratorImpl(getIterator());
	}

	private void reset_(Envelope2D extent, int height) {
		if (height < 0 || height > 127)
			throw new IllegalArgumentException("invalid height");

		m_height = height;
		m_extent.setCoords(extent);
		m_root = m_quad_tree_nodes.newElement();
		m_data_extent.setEmpty();
		m_root = -1;
	}

	private int insert_(int element, Envelope2D bounding_box, int height, Envelope2D quad_extent, int quad_handle, boolean b_flushing, int flushed_element_handle) {
		if (!quad_extent.contains(bounding_box)) {
			assert (!b_flushing);

			if (height == 0)
				return -1;

			return insert_(element, bounding_box, 0, m_extent, m_root, b_flushing, flushed_element_handle);
		}

		if (!b_flushing) {
			for (int q = quad_handle; q != -1; q = get_parent_(q))
				set_sub_tree_element_count_(q, get_sub_tree_element_count_(q) + 1);
		}

		Envelope2D current_extent = new Envelope2D();
		current_extent.setCoords(quad_extent);

		int current_quad_handle = quad_handle;

		Envelope2D[] child_extents = new Envelope2D[4];
		child_extents[0] = new Envelope2D();
		child_extents[1] = new Envelope2D();
		child_extents[2] = new Envelope2D();
		child_extents[3] = new Envelope2D();

		int current_height;
		for (current_height = height; current_height < m_height && can_push_down_(current_quad_handle); current_height++) {
			set_child_extents_(current_extent, child_extents);

			boolean b_contains = false;

			for (int i = 0; i < 4; i++) {
				if (child_extents[i].contains(bounding_box)) {
					b_contains = true;

					int child_handle = get_child_(current_quad_handle, i);
					if (child_handle == -1)
						child_handle = create_child_(current_quad_handle, i);

					set_sub_tree_element_count_(child_handle, get_sub_tree_element_count_(child_handle) + 1);

					current_quad_handle = child_handle;
					current_extent.setCoords(child_extents[i]);
					break;
				}
			}

			if (!b_contains)
				break;
		}

		return insert_at_quad_(element, bounding_box, current_height, current_extent, current_quad_handle, b_flushing, quad_handle, flushed_element_handle, -1);
	}

	private int insert_duplicates_(int element, Envelope2D bounding_box, int height, Envelope2D quad_extent, int quad_handle, boolean b_flushing, int flushed_element_handle) {
		assert (b_flushing || m_root == quad_handle);

		if (!b_flushing) // If b_flushing is true, then the sub tree element counts are already accounted for since the element already lies in the current incoming quad
		{
			if (!quad_extent.contains(bounding_box))
				return -1;

			set_sub_tree_element_count_(quad_handle, get_sub_tree_element_count_(quad_handle) + 1);
			set_contained_sub_tree_element_count_(quad_handle, get_contained_sub_tree_element_count_(quad_handle) + 1);
		}

		double bounding_box_max_dim = Math.max(bounding_box.getWidth(), bounding_box.getHeight());

		int element_handle = -1;
		AttributeStreamOfInt32 quads_stack = new AttributeStreamOfInt32(0);
		ArrayList<Envelope2D> extents_stack = new ArrayList<Envelope2D>(0);
		AttributeStreamOfInt32 heights_stack = new AttributeStreamOfInt32(0);
		quads_stack.add(quad_handle);
		extents_stack.add(new Envelope2D(quad_extent.xmin, quad_extent.ymin, quad_extent.xmax, quad_extent.ymax));
		heights_stack.add(height);

		Envelope2D[] child_extents = new Envelope2D[4];
		child_extents[0] = new Envelope2D();
		child_extents[1] = new Envelope2D();
		child_extents[2] = new Envelope2D();
		child_extents[3] = new Envelope2D();

		Envelope2D current_extent = new Envelope2D();

		while (quads_stack.size() > 0) {
			boolean b_subdivide = false;

			int current_quad_handle = quads_stack.getLast();
			current_extent.setCoords(extents_stack.get(extents_stack.size() - 1));
			int current_height = heights_stack.getLast();

			quads_stack.removeLast();
			extents_stack.remove(extents_stack.size() - 1);
			heights_stack.removeLast();

			if (current_height + 1 < m_height && can_push_down_(current_quad_handle)) {
				double current_extent_max_dim = Math.max(current_extent.getWidth(), current_extent.getHeight());

				if (bounding_box_max_dim <= current_extent_max_dim / 2.0)
					b_subdivide = true;
			}

			if (b_subdivide) {
				set_child_extents_(current_extent, child_extents);

				boolean b_contains = false;

				for (int i = 0; i < 4; i++) {
					b_contains = child_extents[i].contains(bounding_box);

					if (b_contains) {
						int child_handle = get_child_(current_quad_handle, i);
						if (child_handle == -1)
							child_handle = create_child_(current_quad_handle, i);

						quads_stack.add(child_handle);
						extents_stack.add(new Envelope2D(child_extents[i].xmin, child_extents[i].ymin, child_extents[i].xmax, child_extents[i].ymax));
						heights_stack.add(current_height + 1);

						set_sub_tree_element_count_(child_handle, get_sub_tree_element_count_(child_handle) + 1);
						set_contained_sub_tree_element_count_(child_handle, get_contained_sub_tree_element_count_(child_handle) + 1);
						break;
					}
				}

				if (!b_contains) {
					for (int i = 0; i < 4; i++) {
						boolean b_intersects = child_extents[i].isIntersecting(bounding_box);

						if (b_intersects) {
							int child_handle = get_child_(current_quad_handle, i);
							if (child_handle == -1)
								child_handle = create_child_(current_quad_handle, i);

							quads_stack.add(child_handle);
							extents_stack.add(new Envelope2D(child_extents[i].xmin, child_extents[i].ymin, child_extents[i].xmax, child_extents[i].ymax));
							heights_stack.add(current_height + 1);

							set_sub_tree_element_count_(child_handle, get_sub_tree_element_count_(child_handle) + 1);
						}
					}
				}
			} else {
				element_handle = insert_at_quad_(element, bounding_box, current_height, current_extent, current_quad_handle, b_flushing, quad_handle, flushed_element_handle, element_handle);
				b_flushing = false; // flushing is false after the first inserted element has been flushed down, all subsequent inserts will be new
			}
		}

		return 0;
	}

	private int insert_at_quad_(int element, Envelope2D bounding_box, int current_height, Envelope2D current_extent, int current_quad_handle, boolean b_flushing, int quad_handle, int flushed_element_handle, int duplicate_element_handle) {
		// If the bounding box is not contained in any of the current_node's children, or if the current_height is m_height, then insert the element and
		// bounding box into the current_node

		int head_element_handle = get_first_element_(current_quad_handle);
		int tail_element_handle = get_last_element_(current_quad_handle);
		int element_handle = -1;

		if (b_flushing) {
			assert (flushed_element_handle != -1);

			if (current_quad_handle == quad_handle)
				return flushed_element_handle;

			disconnect_element_handle_(flushed_element_handle); // Take it out of the incoming quad_handle, and place in current_quad_handle
			element_handle = flushed_element_handle;
		} else {
			if (duplicate_element_handle == -1) {
				element_handle = create_element_();
				set_data_values_(get_data_(element_handle), element, bounding_box);
			} else {
				assert (m_b_store_duplicates);
				element_handle = create_element_from_duplicate_(duplicate_element_handle);
			}
		}

		assert (!b_flushing || element_handle == flushed_element_handle);

		set_quad_(element_handle, current_quad_handle); // set parent quad (needed for removal of element)

		// assign the prev pointer of the new tail to point at the old tail (tail_element_handle)
		// assign the next pointer of the old tail to point at the new tail (next_element_handle)
		if (tail_element_handle != -1) {
			set_prev_element_(element_handle, tail_element_handle);
			set_next_element_(tail_element_handle, element_handle);
		} else {
			assert (head_element_handle == -1);
			set_first_element_(current_quad_handle, element_handle);
		}

		// assign the new tail
		set_last_element_(current_quad_handle, element_handle);

		set_local_element_count_(current_quad_handle, get_local_element_count_(current_quad_handle) + 1);

		if (can_flush_(current_quad_handle))
			flush_(current_height, current_extent, current_quad_handle);

		return element_handle;
	}

	private static void set_child_extents_(Envelope2D current_extent, Envelope2D[] child_extents) {
		double x_mid = 0.5 * (current_extent.xmin + current_extent.xmax);
		double y_mid = 0.5 * (current_extent.ymin + current_extent.ymax);

		child_extents[0].setCoords(x_mid, y_mid, current_extent.xmax, current_extent.ymax); // northeast
		child_extents[1].setCoords(current_extent.xmin, y_mid, x_mid, current_extent.ymax); // northwest
		child_extents[2].setCoords(current_extent.xmin, current_extent.ymin, x_mid, y_mid); // southwest
		child_extents[3].setCoords(x_mid, current_extent.ymin, current_extent.xmax, y_mid); // southeast
	}

	private void disconnect_element_handle_(int element_handle) {
		assert (element_handle != -1);
		int quad_handle = get_quad_(element_handle);
		int head_element_handle = get_first_element_(quad_handle);
		int tail_element_handle = get_last_element_(quad_handle);
		int prev_element_handle = get_prev_element_(element_handle);
		int next_element_handle = get_next_element_(element_handle);
		assert (head_element_handle != -1 && tail_element_handle != -1);

		if (head_element_handle == element_handle) {
			if (next_element_handle != -1)
				set_prev_element_(next_element_handle, -1);
			else {
				assert (head_element_handle == tail_element_handle);
				assert (get_local_element_count_(quad_handle) == 1);
				set_last_element_(quad_handle, -1);
			}

			set_first_element_(quad_handle, next_element_handle);
		} else if (tail_element_handle == element_handle) {
			assert (prev_element_handle != -1);
			assert (get_local_element_count_(quad_handle) >= 2);
			set_next_element_(prev_element_handle, -1);
			set_last_element_(quad_handle, prev_element_handle);
		} else {
			assert (next_element_handle != -1 && prev_element_handle != -1);
			assert (get_local_element_count_(quad_handle) >= 3);
			set_prev_element_(next_element_handle, prev_element_handle);
			set_next_element_(prev_element_handle, next_element_handle);
		}

		set_prev_element_(element_handle, -1);
		set_next_element_(element_handle, -1);

		set_local_element_count_(quad_handle, get_local_element_count_(quad_handle) - 1);
		assert (get_local_element_count_(quad_handle) >= 0);
	}

	private boolean can_flush_(int quad_handle) {
		return get_local_element_count_(quad_handle) == m_flushing_count && !has_children_(quad_handle);
	}

	private void flush_(int height, Envelope2D extent, int quad_handle) {
		int element;
		Envelope2D bounding_box = new Envelope2D();

		assert (quad_handle != -1);

		int element_handle = get_first_element_(quad_handle), next_handle = -1;
		int data_handle = -1;
		assert (element_handle != -1);

		do {
			data_handle = get_data_(element_handle);
			element = get_element_value_(data_handle);
			bounding_box.setCoords(get_bounding_box_value_(data_handle));

			next_handle = get_next_element_(element_handle);

			if (!m_b_store_duplicates)
				insert_(element, bounding_box, height, extent, quad_handle, true, element_handle);
			else
				insert_duplicates_(element, bounding_box, height, extent, quad_handle, true, element_handle);

			element_handle = next_handle;

		} while (element_handle != -1);
	}

	private boolean can_push_down_(int quad_handle) {
		return get_local_element_count_(quad_handle) >= m_flushing_count || has_children_(quad_handle);
	}

	private boolean has_children_(int parent) {
		return get_child_(parent, 0) != -1 || get_child_(parent, 1) != -1 || get_child_(parent, 2) != -1 || get_child_(parent, 3) != -1;
	}

	private int create_child_(int parent, int quadrant) {
		int child = m_quad_tree_nodes.newElement();
		set_child_(parent, quadrant, child);
		set_sub_tree_element_count_(child, 0);
		set_local_element_count_(child, 0);
		set_parent_(child, parent);
		set_height_and_quadrant_(child, get_height_(parent) + 1, quadrant);

		if (m_b_store_duplicates)
			set_contained_sub_tree_element_count_(child, 0);

		return child;
	}

	private void create_root_() {
		m_root = m_quad_tree_nodes.newElement();
		set_sub_tree_element_count_(m_root, 0);
		set_local_element_count_(m_root, 0);
		set_height_and_quadrant_(m_root, 0, 0);

		if (m_b_store_duplicates)
			set_contained_sub_tree_element_count_(m_root, 0);
	}

	private int create_element_() {
		int element_handle = m_element_nodes.newElement();
		int data_handle;

		if (m_free_data.size() > 0) {
			data_handle = m_free_data.get(m_free_data.size() - 1);
			m_free_data.removeLast();
		} else {
			data_handle = m_data.size();
			m_data.add(null);
		}

		set_data_(element_handle, data_handle);
		return element_handle;
	}

	private int create_element_from_duplicate_(int duplicate_element_handle) {
		int element_handle = m_element_nodes.newElement();
		int data_handle = get_data_(duplicate_element_handle);
		set_data_(element_handle, data_handle);
		return element_handle;
	}

	private void free_element_and_box_node_(int element_handle) {
		int data_handle = get_data_(element_handle);
		m_free_data.add(data_handle);
		m_element_nodes.deleteElement(element_handle);
	}

	private int get_child_(int quad_handle, int quadrant) {
		return m_quad_tree_nodes.getField(quad_handle, quadrant);
	}

	private void set_child_(int parent, int quadrant, int child) {
		m_quad_tree_nodes.setField(parent, quadrant, child);
	}

	private int get_first_element_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 4);
	}

	private void set_first_element_(int quad_handle, int head) {
		m_quad_tree_nodes.setField(quad_handle, 4, head);
	}

	private int get_last_element_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 5);
	}

	private void set_last_element_(int quad_handle, int tail) {
		m_quad_tree_nodes.setField(quad_handle, 5, tail);
	}


	private int get_quadrant_(int quad_handle) {
		int height_quadrant_hybrid = m_quad_tree_nodes.getField(quad_handle, 6);
		int quadrant = height_quadrant_hybrid & m_quadrant_mask;
		return quadrant;
	}

	private int get_height_(int quad_handle) {
		int height_quadrant_hybrid = m_quad_tree_nodes.getField(quad_handle, 6);
		int height = height_quadrant_hybrid >> m_height_bit_shift;
		return height;
	}

	private void set_height_and_quadrant_(int quad_handle, int height, int quadrant) {
		assert (quadrant >= 0 && quadrant <= 3);
		int height_quadrant_hybrid = (int) ((height << m_height_bit_shift) | quadrant);
		m_quad_tree_nodes.setField(quad_handle, 6, height_quadrant_hybrid);
	}

	private int get_local_element_count_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 7);
	}

	private void set_local_element_count_(int quad_handle, int count) {
		m_quad_tree_nodes.setField(quad_handle, 7, count);
	}

	private int get_sub_tree_element_count_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 8);
	}

	private void set_sub_tree_element_count_(int quad_handle, int count) {
		m_quad_tree_nodes.setField(quad_handle, 8, count);
	}

	private int get_parent_(int child) {
		return m_quad_tree_nodes.getField(child, 9);
	}

	private void set_parent_(int child, int parent) {
		m_quad_tree_nodes.setField(child, 9, parent);
	}

	private int get_contained_sub_tree_element_count_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 10);
	}

	private void set_contained_sub_tree_element_count_(int quad_handle, int count) {
		m_quad_tree_nodes.setField(quad_handle, 10, count);
	}

	private int get_data_(int element_handle) {
		return m_element_nodes.getField(element_handle, 0);
	}

	private void set_data_(int element_handle, int data_handle) {
		m_element_nodes.setField(element_handle, 0, data_handle);
	}

	private int get_prev_element_(int element_handle) {
		return m_element_nodes.getField(element_handle, 1);
	}

	private int get_next_element_(int element_handle) {
		return m_element_nodes.getField(element_handle, 2);
	}

	private void set_prev_element_(int element_handle, int prev_handle) {
		m_element_nodes.setField(element_handle, 1, prev_handle);
	}

	private void set_next_element_(int element_handle, int next_handle) {
		m_element_nodes.setField(element_handle, 2, next_handle);
	}

	private int get_quad_(int element_handle) {
		return m_element_nodes.getField(element_handle, 3);
	}

	private void set_quad_(int element_handle, int parent) {
		m_element_nodes.setField(element_handle, 3, parent);
	}

	private int get_element_value_(int data_handle) {
		return m_data.get(data_handle).element;
	}

	private Envelope2D get_bounding_box_value_(int data_handle) {
		return m_data.get(data_handle).box;
	}

	private void set_data_values_(int data_handle, int element, Envelope2D bounding_box) {
		m_data.set(data_handle, new Data(element, bounding_box));
	}

	private Envelope2D m_extent;
	private Envelope2D m_data_extent;
	private StridedIndexTypeCollection m_quad_tree_nodes;
	private StridedIndexTypeCollection m_element_nodes;
	transient private ArrayList<Data> m_data;
	private AttributeStreamOfInt32 m_free_data;
	private int m_root;
	private int m_height;
	private boolean m_b_store_duplicates;

	final static private int m_quadrant_mask = 3;
	final static private int m_height_bit_shift = 2;
	final static private int m_flushing_count = 5;

	static final class Data {
		int element;
		Envelope2D box;
		
		Data() {
		}
		
		Data(int element_, double x1, double y1, double x2, double y2) {
			element = element_;
			box = new Envelope2D();
			box.setCoords(x1, y1, x2, y2);
		}

		Data(int element_, Envelope2D box_) {
			element = element_;
			box = new Envelope2D();
			box.setCoords(box_);
		}
	}

	private void writeObject(java.io.ObjectOutputStream stream)
			throws IOException {
		stream.defaultWriteObject();
		stream.writeInt(m_data.size());
		for (int i = 0, n = m_data.size(); i < n; ++i) {
			Data d = m_data.get(i);
			if (d != null) {
				stream.writeByte(1);
				stream.writeInt(d.element);
				stream.writeDouble(d.box.xmin);
				stream.writeDouble(d.box.ymin);
				stream.writeDouble(d.box.xmax);
				stream.writeDouble(d.box.ymax);
			}
			else {
				stream.writeByte(0);
			}
				
		}
	}

	private void readObject(java.io.ObjectInputStream stream)
			throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		int dataSize = stream.readInt();
		m_data = new ArrayList<Data>(dataSize);
		for (int i = 0, n = dataSize; i < n; ++i) {
			int b = stream.readByte();
			if (b == 1) {
				int elm = stream.readInt();
				double x1 = stream.readDouble();
				double y1 = stream.readDouble();
				double x2 = stream.readDouble();
				double y2 = stream.readDouble();
				Data d = new Data(elm, x1, y1, x2, y2);
				m_data.add(d);
			}
			else if (b == 0) {
				m_data.add(null);
			}
			else {
				throw new IOException();
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new InvalidObjectException("Stream data required");
	}

    /* m_quad_tree_nodes
    * 0: m_north_east_child
    * 1: m_north_west_child
    * 2: m_south_west_child
    * 3: m_south_east_child
    * 4: m_head_element
    * 5: m_tail_element
    * 6: m_quadrant_and_height
    * 7: m_local_element_count
    * 8: m_sub_tree_element_count
    * 9: m_parent_quad
    * 10: m_height
    */

    /* m_element_nodes
    * 0: m_data_handle
    * 1: m_prev
    * 2: m_next
    * 3: m_parent_quad
    */

    /* m_data
    * element
    * box
    */
}
