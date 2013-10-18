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

import java.util.ArrayList;

class QuadTreeImpl {
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

			if (m_query_box.isIntersecting(m_quad_tree.m_extent)) {
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
				m_next_element_handle = m_quad_tree
						.getFirstElement_(m_quad_tree.m_root);
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

			if (m_query_box.isIntersecting(m_quad_tree.m_extent)) {
				m_quads_stack.add(m_quad_tree.m_root);
				m_extents_stack.add(m_quad_tree.m_extent);
				m_next_element_handle = m_quad_tree
						.getFirstElement_(m_quad_tree.m_root);
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

			if (m_b_linear) {// Should this memory be cached for reuse?
				start = new Point2D();
				end = new Point2D();
				extent_inf = new Envelope2D();
			}

			boolean b_found_hit = false;
			while (!b_found_hit) {
				while (m_current_element_handle != -1) {
					int current_box_handle = m_quad_tree
							.getBoxHandle_(m_current_element_handle);
					bounding_box = m_quad_tree
							.getBoundingBox_(current_box_handle);

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
					m_current_element_handle = m_quad_tree
							.getNextElement_(m_current_element_handle);
				}

				// If m_current_element_handle equals -1, then we've exhausted
				// our search in the current quadtree node
				if (m_current_element_handle == -1) {
					// get the last node from the stack and add the children
					// whose extent intersects m_query_box
					int current_quad = m_quads_stack.getLast();
					Envelope2D current_extent = m_extents_stack
							.get(m_extents_stack.size() - 1);

					double x_mid = 0.5 * (current_extent.xmin + current_extent.xmax);
					double y_mid = 0.5 * (current_extent.ymin + current_extent.ymax);

					if (child_extents == null) {
						child_extents = new Envelope2D[4];
						child_extents[0] = new Envelope2D();
						child_extents[1] = new Envelope2D();
						child_extents[2] = new Envelope2D();
						child_extents[3] = new Envelope2D();
					}
					child_extents[0].setCoords(x_mid, y_mid,
							current_extent.xmax, current_extent.ymax); // northeast
					child_extents[1].setCoords(current_extent.xmin, y_mid,
							x_mid, current_extent.ymax); // northwest
					child_extents[2].setCoords(current_extent.xmin,
							current_extent.ymin, x_mid, y_mid); // southwest
					child_extents[3].setCoords(x_mid, current_extent.ymin,
							current_extent.xmax, y_mid); // southeast

					m_quads_stack.removeLast();
					m_extents_stack.remove(m_extents_stack.size() - 1);

					for (int quadrant = 0; quadrant < 4; quadrant++) {
						int child_handle = m_quad_tree.getChild_(current_quad,
								quadrant);

						if (child_handle != -1
								&& m_quad_tree
										.getSubTreeElementCount(child_handle) > 0) {
							if (child_extents[quadrant]
									.isIntersecting(m_query_box)) {
								if (m_b_linear) {
									start.setCoords(m_query_start);
									end.setCoords(m_query_end);

									extent_inf
											.setCoords(child_extents[quadrant]);
									extent_inf
											.inflate(m_tolerance, m_tolerance);
									if (extent_inf.clipLine(start, end) > 0) {
										Envelope2D child_extent = new Envelope2D();
										child_extent
												.setCoords(child_extents[quadrant]);
										m_quads_stack.add(child_handle);
										m_extents_stack.add(child_extent);
									}
								} else {
									Envelope2D child_extent = new Envelope2D();
									child_extent
											.setCoords(child_extents[quadrant]);
									m_quads_stack.add(child_handle);
									m_extents_stack.add(child_extent);
								}
							}
						}
					}

					assert (m_quads_stack.size() <= 4 * (m_quad_tree.m_height - 1));

					if (m_quads_stack.size() == 0)
						return -1;

					m_current_element_handle = m_quad_tree
							.getFirstElement_(m_quads_stack.get(m_quads_stack
									.size() - 1));
				}
			}

			// We did not exhaust our search in the current node, so we return
			// the element at m_current_element_handle in m_element_nodes

			m_next_element_handle = m_quad_tree
					.getNextElement_(m_current_element_handle);
			return m_current_element_handle;
		}

		// Creates an iterator on the input Quad_tree_impl. The query will be
		// the Envelope_2D bounding the input Geometry.
		QuadTreeIteratorImpl(QuadTreeImpl quad_tree_impl, Geometry query,
				double tolerance) {
			m_quad_tree = quad_tree_impl;
			m_query_box = new Envelope2D();
			m_quads_stack = new AttributeStreamOfInt32(0);
			m_extents_stack = new ArrayList<Envelope2D>(0);
			resetIterator(query, tolerance);
		}

		// Creates an iterator on the input Quad_tree_impl using the input
		// Envelope_2D as the query.
		QuadTreeIteratorImpl(QuadTreeImpl quad_tree_impl, Envelope2D query,
				double tolerance) {
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
		private ArrayList<Envelope2D> m_extents_stack; // this won't grow bigger
														// than 4 *
														// (m_quad_tree->m_height
														// - 1)
	}

	/**
	 * Creates a Quad_tree_impl with the root having the extent of the input
	 * Envelope_2D, and height of the input height, where the root starts at
	 * height 0. Note that the height cannot be larger than 16 if on a 32 bit
	 * platform and 32 if on a 64 bit platform. \param extent The extent of the
	 * Quad_tree_impl. \param height The max height of the Quad_tree_impl.
	 */
	QuadTreeImpl(Envelope2D extent, int height) {
		m_quad_tree_nodes = new StridedIndexTypeCollection(11);
		m_element_nodes = new StridedIndexTypeCollection(5);
		m_boxes = new ArrayList<Envelope2D>(0);
		m_free_boxes = new AttributeStreamOfInt32(0);
		m_extent = new Envelope2D();
		reset_(extent, height);
	}

	/**
	 * Resets the Quad_tree_impl to the given extent and height. \param extent
	 * The extent of the Quad_tree_impl. \param height The max height of the
	 * Quad_tree_impl.
	 */
	void reset(Envelope2D extent, int height) {
		m_quad_tree_nodes.deleteAll(false);
		m_element_nodes.deleteAll(false);
		m_boxes.clear();
		m_free_boxes.clear(false);
		reset_(extent, height);
	}

	/**
	 * Inserts the element and bounding_box into the Quad_tree_impl. Note that
	 * this will invalidate any active iterator on the Quad_tree_impl. Returns
	 * an int corresponding to the element and bounding_box. \param element The
	 * element of the Geometry to be inserted. \param bounding_box The
	 * bounding_box of the Geometry to be inserted.
	 */
	int insert(int element, Envelope2D bounding_box) {
		return insert_(element, bounding_box, 0, m_extent, m_root, false, -1);
	}

	/**
	 * Inserts the element and bounding_box into the Quad_tree_impl at the given
	 * quad_handle. Note that this will invalidate any active iterator on the
	 * Quad_tree_impl. Returns an int corresponding to the element and
	 * bounding_box. \param element The element of the Geometry to be inserted.
	 * \param bounding_box The bounding_box of the Geometry to be inserted.
	 * \param hint_index A handle used as a hint where to place the element.
	 * This can be a handle obtained from a previous insertion and is useful on
	 * data having strong locality such as segments of a Polygon.
	 */
	int insert(int element, Envelope2D bounding_box, int hint_index) {
		int quad_handle;

		if (hint_index == -1)
			quad_handle = m_root;
		else
			quad_handle = getQuad_(hint_index);

		int quad_height = getHeight(quad_handle);
		Envelope2D quad_extent = getExtent(quad_handle);
		return insert_(element, bounding_box, quad_height, quad_extent,
				quad_handle, false, -1);
	}

	/**
	 * Removes the element and bounding_box at the given element_handle. Note
	 * that this will invalidate any active iterator on the Quad_tree_impl.
	 * \param element_handle The handle corresponding to the element and
	 * bounding_box to be removed.
	 */
	void removeElement(int element_handle) {
		int quad_handle = getQuad_(element_handle);
		int nextElementHandle = disconnectElementHandle_(element_handle);
		freeElementAndBoxNode_(element_handle);

		for (int q = quad_handle; q != -1; q = getParent_(q)) {
			setSubTreeElementCount_(q, getSubTreeElementCount_(q) - 1);
			assert (getSubTreeElementCount_(q) >= 0);
		}
	}

	/**
	 * Removes the quad and all its children corresponding to the input
	 * quad_handle. \param quad_handle The handle corresponding to the quad to
	 * be removed.
	 */
	void removeQuad(int quad_handle) {
		removeQuad_(quad_handle);
	}

	/**
	 * Returns the element at the given element_handle. \param element_handle
	 * The handle corresponding to the element to be retrieved.
	 */
	int getElement(int element_handle) {
		return getElement_(element_handle);
	}

	/**
	 * Returns the height of the quad at the given quad_handle. \param
	 * quad_handle The handle corresponding to the quad.
	 */
	int getHeight(int quad_handle) {
		return getHeight_(quad_handle);
	}

	/**
	 * Returns the extent of the quad at the given quad_handle. \param
	 * quad_handle The handle corresponding to the quad.
	 */
	Envelope2D getExtent(int quad_handle) {
		Envelope2D quad_extent = new Envelope2D();
		quad_extent.setCoords(m_extent);

		int height = getHeight_(quad_handle);
		int morten_number = getMortenNumber_(quad_handle);
		int mask = 3;

		for (int i = 0; i < 2 * height; i += 2) {
			int child = (int) (mask & (morten_number >> i));

			if (child == 0) {// northeast
				quad_extent.xmin = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymin = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			} else if (child == 1) {// northwest
				quad_extent.xmax = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymin = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			} else if (child == 2) {// southwest
				quad_extent.xmax = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymax = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			} else {// southeast
				quad_extent.xmin = 0.5 * (quad_extent.xmin + quad_extent.xmax);
				quad_extent.ymax = 0.5 * (quad_extent.ymin + quad_extent.ymax);
			}
		}

		return quad_extent;
	}

	/**
	 * Returns the int of the quad containing the given element_handle. \param
	 * element_handle The handle corresponding to the element.
	 */
	int getQuad(int element_handle) {
		return getQuad_(element_handle);
	}

	/**
	 * Returns the number of elements in the Quad_tree_impl.
	 */
	int getElementCount() {
		return getSubTreeElementCount_(m_root);
	}

	/**
	 * Returns the number of elements in the subtree rooted at the given
	 * quad_handle. \param quad_handle The handle corresponding to the quad.
	 */
	int getSubTreeElementCount(int quad_handle) {
		return getSubTreeElementCount_(quad_handle);
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

	private void reset_(Envelope2D extent, int height) {
		// We need 2 * height bits for the morten number, which is of type
		// Index_type (more than enough).
		if (height < 0 || 2 * height > 8 * 4)
			throw new IllegalArgumentException("invalid height");

		m_height = height;
		m_extent.setCoords(extent);
		m_root = m_quad_tree_nodes.newElement();
		setSubTreeElementCount_(m_root, 0);
		setLocalElementCount_(m_root, 0);
		setMortenNumber_(m_root, 0);
		setHeight_(m_root, 0);
	}

	private int insert_(int element, Envelope2D bounding_box, int height,
			Envelope2D quad_extent, int quad_handle, boolean b_flushing,
			int flushed_element_handle) {
		Point2D bb_center = new Point2D();
		bounding_box.queryCenter(bb_center);

		Envelope2D current_extent = new Envelope2D();
		current_extent.setCoords(quad_extent);

		int current_quad_handle = quad_handle;
		if (!current_extent.contains(bounding_box)) {
			if (height == 0)
				return -1;

			return insert_(element, bounding_box, 0, m_extent, m_root,
					b_flushing, flushed_element_handle);
		}

		// Should this memory be cached for reuse?
		Point2D quad_center = new Point2D();
		Envelope2D[] child_extents = new Envelope2D[4];
		child_extents[0] = new Envelope2D();
		child_extents[1] = new Envelope2D();
		child_extents[2] = new Envelope2D();
		child_extents[3] = new Envelope2D();

		int current_height;
		for (current_height = height; current_height < m_height
				&& canPushDown_(current_quad_handle); current_height++) {
			double x_mid = 0.5 * (current_extent.xmin + current_extent.xmax);
			double y_mid = 0.5 * (current_extent.ymin + current_extent.ymax);

			child_extents[0].setCoords(x_mid, y_mid, current_extent.xmax,
					current_extent.ymax); // northeast
			child_extents[1].setCoords(current_extent.xmin, y_mid, x_mid,
					current_extent.ymax); // northwest
			child_extents[2].setCoords(current_extent.xmin,
					current_extent.ymin, x_mid, y_mid); // southwest
			child_extents[3].setCoords(x_mid, current_extent.ymin,
					current_extent.xmax, y_mid); // southeast

			// Find the first child quadrant that contains the bounding box, and
			// recursively insert into that child (greedy algorithm)
			double mind = NumberUtils.doubleMax();
			int quadrant = -1;
			for (int i = 0; i < 4; i++) {
				child_extents[i].queryCenter(quad_center);
				double d = Point2D.sqrDistance(quad_center, bb_center);
				if (d < mind) {
					mind = d;
					quadrant = i;
				}
			}

			if (child_extents[quadrant].contains(bounding_box)) {
				int child_handle = getChild_(current_quad_handle, quadrant);
				if (child_handle == -1)
					child_handle = createChild_(current_quad_handle, quadrant);

				current_quad_handle = child_handle;
				current_extent.setCoords(child_extents[quadrant]);
				setSubTreeElementCount_(current_quad_handle,
						getSubTreeElementCount_(current_quad_handle) + 1);
				continue;
			}

			break;
		}

		// If the bounding box is not contained in any of the current_node's
		// children, or if the current_height is m_height, then insert the
		// element and
		// bounding box into the current_node

		int head_element_handle = getFirstElement_(current_quad_handle);
		int tail_element_handle = getLastElement_(current_quad_handle);
		int element_handle;

		if (b_flushing) {
			assert (flushed_element_handle != -1);

			if (current_quad_handle == quad_handle)
				return flushed_element_handle;

			disconnectElementHandle_(flushed_element_handle); // Take it out of
																// the incoming
																// quad_handle,
																// and place in
																// current_quad_handle
			element_handle = flushed_element_handle;
		} else {
			element_handle = createElementAndBoxNode_();
			setElement_(element_handle, element); // insert element at the new
													// tail of the list
													// (next_element_handle).
			setBoundingBox_(getBoxHandle_(element_handle), bounding_box); // insert
																			// bounding_box
			setSubTreeElementCount_(quad_handle,
					getSubTreeElementCount_(quad_handle) + 1);
		}

		assert (!b_flushing || element_handle == flushed_element_handle);

		setQuad_(element_handle, current_quad_handle); // set parent quad
														// (needed for removal
														// of element)

		// assign the prev pointer of the new tail to point at the old tail
		// (tail_element_handle)
		// assign the next pointer of the old tail to point at the new tail
		// (next_element_handle)
		if (tail_element_handle != -1) {
			setPrevElement_(element_handle, tail_element_handle);
			setNextElement_(tail_element_handle, element_handle);
		} else {
			assert (head_element_handle == -1);
			setFirstElement_(current_quad_handle, element_handle);
		}

		// assign the new tail
		setLastElement_(current_quad_handle, element_handle);

		setLocalElementCount_(current_quad_handle,
				getLocalElementCount_(current_quad_handle) + 1);

		if (canFlush_(current_quad_handle))
			flush_(current_height, current_extent, current_quad_handle);

		return element_handle;
	}

	private int disconnectElementHandle_(int element_handle) {
		assert (element_handle != -1);
		int quad_handle = getQuad_(element_handle);
		int head_element_handle = getFirstElement_(quad_handle);
		int tail_element_handle = getLastElement_(quad_handle);
		int prev_element_handle = getPrevElement_(element_handle);
		int next_element_handle = getNextElement_(element_handle);
		assert (head_element_handle != -1 && tail_element_handle != -1);

		if (head_element_handle == element_handle) {
			if (next_element_handle != -1)
				setPrevElement_(next_element_handle, -1);
			else {
				assert (head_element_handle == tail_element_handle);
				assert (getLocalElementCount_(quad_handle) == 1);
				setLastElement_(quad_handle, -1);
			}

			setFirstElement_(quad_handle, next_element_handle);
		} else if (tail_element_handle == element_handle) {
			assert (prev_element_handle != -1);
			assert (getLocalElementCount_(quad_handle) >= 2);
			setNextElement_(prev_element_handle, -1);
			setLastElement_(quad_handle, prev_element_handle);
		} else {
			assert (next_element_handle != -1 && prev_element_handle != -1);
			assert (getLocalElementCount_(quad_handle) >= 3);
			setPrevElement_(next_element_handle, prev_element_handle);
			setNextElement_(prev_element_handle, next_element_handle);
		}

		setPrevElement_(element_handle, -1);
		setNextElement_(element_handle, -1);

		setLocalElementCount_(quad_handle,
				getLocalElementCount_(quad_handle) - 1);
		assert (getLocalElementCount_(quad_handle) >= 0);

		return next_element_handle;
	}

	private void removeQuad_(int quad_handle) {
		int subTreeElementCount = getSubTreeElementCount_(quad_handle);
		if (subTreeElementCount > 0)
			for (int q = getParent_(quad_handle); q != -1; q = getParent_(q))
				setSubTreeElementCount_(q, getSubTreeElementCount_(q)
						- subTreeElementCount);

		removeQuadHelper_(quad_handle);

		int parent = getParent_(quad_handle);

		if (parent != -1) {
			for (int quadrant = 0; quadrant < 4; quadrant++) {
				if (getChild_(parent, quadrant) == quad_handle) {
					setChild_(parent, quadrant, -1);
					break;
				}
			}
		}
	}

	private void removeQuadHelper_(int quad_handle) {
		for (int element_handle = getFirstElement_(quad_handle); element_handle != -1; element_handle = getNextElement_(element_handle)) {
			m_free_boxes.add(getBoxHandle_(element_handle));
			m_element_nodes.deleteElement(element_handle);
		}

		for (int quadrant = 0; quadrant < 4; quadrant++) {
			int child_handle = getChild_(quad_handle, quadrant);
			if (child_handle != -1) {
				removeQuadHelper_(child_handle);
				setChild_(quad_handle, quadrant, -1);
			}
		}

		if (quad_handle != m_root) {
			m_quad_tree_nodes.deleteElement(quad_handle);
		} else {
			setSubTreeElementCount_(m_root, 0);
			setLocalElementCount_(m_root, 0);
			setFirstElement_(m_root, -1);
			setLastElement_(m_root, -1);
		}
	}

	private boolean canFlush_(int quad_handle) {
		return getLocalElementCount_(quad_handle) == 8
				&& !hasChildren_(quad_handle);
	}

	private void flush_(int height, Envelope2D extent, int quad_handle) {
		int element;
		Envelope2D bounding_box;

		assert (quad_handle != -1);

		int element_handle = getFirstElement_(quad_handle), next_handle;
		int box_handle;
		assert (element_handle != -1);

		do {
			box_handle = getBoxHandle_(element_handle);
			element = m_element_nodes.getField(element_handle, 0);
			bounding_box = getBoundingBox_(box_handle);
			insert_(element, bounding_box, height, extent, quad_handle, true,
					element_handle);

			next_handle = getNextElement_(element_handle);
			element_handle = next_handle;

		} while (element_handle != -1);
	}

	boolean canPushDown_(int quad_handle) {
		return getLocalElementCount_(quad_handle) >= 8
				|| hasChildren_(quad_handle);
	}

	boolean hasChildren_(int parent) {
		return getChild_(parent, 0) != -1 || getChild_(parent, 1) != -1
				|| getChild_(parent, 2) != -1 || getChild_(parent, 3) != -1;
	}

	private int createChild_(int parent, int quadrant) {
		int child = m_quad_tree_nodes.newElement();
		setChild_(parent, quadrant, child);
		setSubTreeElementCount_(child, 0);
		setLocalElementCount_(child, 0);
		setParent_(child, parent);
		setHeight_(child, getHeight_(parent) + 1);
		setMortenNumber_(child, (quadrant << (2 * getHeight_(parent)))
				| getMortenNumber_(parent));
		return child;
	}

	private int createElementAndBoxNode_() {
		int element_handle = m_element_nodes.newElement();
		int box_handle;

		if (m_free_boxes.size() > 0) {
			box_handle = m_free_boxes.getLast();
			m_free_boxes.removeLast();
		} else {
			box_handle = m_boxes.size();
			m_boxes.add(new Envelope2D());
		}

		setBoxHandle_(element_handle, box_handle);
		return element_handle;
	}

	private void freeElementAndBoxNode_(int element_handle) {
		m_free_boxes.add(getBoxHandle_(element_handle));
		m_element_nodes.deleteElement(element_handle);
	}

	private int getChild_(int quad_handle, int quadrant) {
		return m_quad_tree_nodes.getField(quad_handle, quadrant);
	}

	private void setChild_(int parent, int quadrant, int child) {
		m_quad_tree_nodes.setField(parent, quadrant, child);
	}

	private int getFirstElement_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 4);
	}

	private void setFirstElement_(int quad_handle, int head) {
		m_quad_tree_nodes.setField(quad_handle, 4, head);
	}

	private int getLastElement_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 5);
	}

	private void setLastElement_(int quad_handle, int tail) {
		m_quad_tree_nodes.setField(quad_handle, 5, tail);
	}

	private int getMortenNumber_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 6);
	}

	private void setMortenNumber_(int quad_handle, int morten_number) {
		m_quad_tree_nodes.setField(quad_handle, 6, morten_number);
	}

	private int getLocalElementCount_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 7);
	}

	private int getSubTreeElementCount_(int quad_handle) {
		return m_quad_tree_nodes.getField(quad_handle, 8);
	}

	private void setLocalElementCount_(int quad_handle, int count) {
		m_quad_tree_nodes.setField(quad_handle, 7, count);
	}

	private void setSubTreeElementCount_(int quad_handle, int count) {
		m_quad_tree_nodes.setField(quad_handle, 8, count);
	}

	private int getParent_(int child) {
		return m_quad_tree_nodes.getField(child, 9);
	}

	private void setParent_(int child, int parent) {
		m_quad_tree_nodes.setField(child, 9, parent);
	}

	private int getHeight_(int quad_handle) {
		return (int) m_quad_tree_nodes.getField(quad_handle, 10);
	}

	private void setHeight_(int quad_handle, int height) {
		m_quad_tree_nodes.setField(quad_handle, 10, height);
	}

	private int getElement_(int element_handle) {
		return m_element_nodes.getField(element_handle, 0);
	}

	private void setElement_(int element_handle, int element) {
		m_element_nodes.setField(element_handle, 0, element);
	}

	private int getPrevElement_(int element_handle) {
		return m_element_nodes.getField(element_handle, 1);
	}

	private int getNextElement_(int element_handle) {
		return m_element_nodes.getField(element_handle, 2);
	}

	private void setPrevElement_(int element_handle, int prev_handle) {
		m_element_nodes.setField(element_handle, 1, prev_handle);
	}

	private void setNextElement_(int element_handle, int next_handle) {
		m_element_nodes.setField(element_handle, 2, next_handle);
	}

	private int getQuad_(int element_handle) {
		return m_element_nodes.getField(element_handle, 3);
	}

	private void setQuad_(int element_handle, int parent) {
		m_element_nodes.setField(element_handle, 3, parent);
	}

	private int getBoxHandle_(int element_handle) {
		return m_element_nodes.getField(element_handle, 4);
	}

	private void setBoxHandle_(int element_handle, int box_handle) {
		m_element_nodes.setField(element_handle, 4, box_handle);
	}

	private Envelope2D getBoundingBox_(int box_handle) {
		return m_boxes.get(box_handle);
	}

	private void setBoundingBox_(int box_handle, Envelope2D bounding_box) {
		m_boxes.get(box_handle).setCoords(bounding_box);
	}

	private int m_root;
	private Envelope2D m_extent;
	private int m_height;
	private StridedIndexTypeCollection m_quad_tree_nodes;
	private StridedIndexTypeCollection m_element_nodes;
	private ArrayList<Envelope2D> m_boxes;
	private AttributeStreamOfInt32 m_free_boxes;
}
