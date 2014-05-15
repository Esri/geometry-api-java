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

final class IntervalTreeImpl {
	static final class IntervalTreeIteratorImpl {
		/**
		 * Resets the iterator to a starting state on the Interval_tree_impl
		 * using the input Envelope_1D interval as the query \param query The
		 * Envelope_1D interval used for the query. \param tolerance The
		 * tolerance used for the intersection tests.
		 */
		void resetIterator(Envelope1D query, double tolerance) {
			m_query.vmin = query.vmin - tolerance;
			m_query.vmax = query.vmax + tolerance;
			m_tertiary_stack.resize(0);
			m_function_index = 0;
			m_function_stack[0] = State.initialize;
		}

		/**
		 * Resets the iterator to a starting state on the Interval_tree_impl
		 * using the input Envelope_1D interval as the query \param query The
		 * Envelope_1D interval used for the query. \param tolerance The
		 * tolerance used for the intersection tests.
		 */
		void resetIterator(double query_min, double query_max, double tolerance) {
			if (query_min > query_max)
				throw new IllegalArgumentException();

			m_query.vmin = query_min - tolerance;
			m_query.vmax = query_max + tolerance;
			m_tertiary_stack.resize(0);
			m_function_index = 0;
			m_function_stack[0] = State.initialize;
		}

		/**
		 * Resets the iterator to a starting state on the Interval_tree_impl
		 * using the input double as the stabbing query \param query The double
		 * used for the query. \param tolerance The tolerance used for the
		 * intersection tests.
		 */
		void resetIterator(double query, double tolerance) {
			m_query.vmin = query - tolerance;
			m_query.vmax = query + tolerance;
			m_tertiary_stack.resize(0);
			m_function_index = 0;
			m_function_stack[0] = State.initialize;
		}

		/**
		 * Iterates over all intervals which interset the query interval.
		 * Returns an index to an interval that intersects the query.
		 */
		int next() {
			if (!m_interval_tree.m_b_construction_ended)
				throw new GeometryException("invalid call");

			if (m_function_index < 0)
				return -1;

			boolean b_searching = true;

			while (b_searching) {
				switch (m_function_stack[m_function_index]) {
				case State.pIn:
					b_searching = pIn_();
					break;
				case State.pL:
					b_searching = pL_();
					break;
				case State.pR:
					b_searching = pR_();
					break;
				case State.pT:
					b_searching = pT_();
					break;
				case State.right:
					b_searching = right_();
					break;
				case State.left:
					b_searching = left_();
					break;
				case State.all:
					b_searching = all_();
					break;
				case State.initialize:
					b_searching = initialize_();
					break;
				default:
					throw new GeometryException("internal error");
				}
			}

			if (m_current_end_handle != -1)
				return getCurrentEndIndex_() >> 1;

			return -1;
		}

		// Creates an iterator on the input Interval_tree using the input
		// Envelope_1D interval as the query.
		IntervalTreeIteratorImpl(IntervalTreeImpl interval_tree,
				Envelope1D query, double tolerance) {
			m_interval_tree = interval_tree;
			m_tertiary_stack.reserve(20);
			resetIterator(query, tolerance);
		}

		// Creates an iterator on the input Interval_tree using the input double
		// as the stabbing query.
		IntervalTreeIteratorImpl(IntervalTreeImpl interval_tree, double query,
				double tolerance) {
			m_interval_tree = interval_tree;
			m_tertiary_stack.reserve(20);
			resetIterator(query, tolerance);
		}

		// Creates an iterator on the input Interval_tree.
		IntervalTreeIteratorImpl(IntervalTreeImpl interval_tree) {
			m_interval_tree = interval_tree;
			m_tertiary_stack.reserve(20);
			m_function_index = -1;
		}

		private IntervalTreeImpl m_interval_tree;
		private Envelope1D m_query = new Envelope1D();
		private int m_primary_handle;
		private int m_next_primary_handle;
		private int m_forked_handle;
		private int m_current_end_handle;
		private int m_next_end_handle;
		private AttributeStreamOfInt32 m_tertiary_stack = new AttributeStreamOfInt32(
				0);
		private int m_function_index;
		private int[] m_function_stack = new int[2];

		private interface State {
			static final int initialize = 0;
			static final int pIn = 1;
			static final int pL = 2;
			static final int pR = 3;
			static final int pT = 4;
			static final int right = 5;
			static final int left = 6;
			static final int all = 7;
		}

		private boolean initialize_() {
			m_primary_handle = -1;
			m_next_primary_handle = -1;
			m_forked_handle = -1;
			m_current_end_handle = -1;

			if (m_interval_tree.m_primary_nodes != null
					&& m_interval_tree.m_primary_nodes.size() > 0) {
				m_function_stack[0] = State.pIn; // overwrite initialize
				m_next_primary_handle = m_interval_tree.m_root;
				return true;
			}

			m_function_index = -1;
			return false;
		}

		private boolean pIn_() {
			m_primary_handle = m_next_primary_handle;

			if (m_primary_handle == -1) {
				m_function_index = -1;
				m_current_end_handle = -1;
				return false;
			}

			double discriminant = m_interval_tree
					.getDiscriminant_(m_primary_handle);

			if (m_query.vmax < discriminant) {
				int secondary_handle = m_interval_tree
						.getSecondaryFromPrimary(m_primary_handle);
				m_next_primary_handle = m_interval_tree
						.getLPTR_(m_primary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree
							.getFirst_(secondary_handle);
					m_function_stack[++m_function_index] = State.left;
				}

				return true;
			}

			if (discriminant < m_query.vmin) {
				int secondary_handle = m_interval_tree
						.getSecondaryFromPrimary(m_primary_handle);
				m_next_primary_handle = m_interval_tree
						.getRPTR_(m_primary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree
							.getLast_(secondary_handle);
					m_function_stack[++m_function_index] = State.right;
				}

				return true;
			}

			assert (m_query.contains(discriminant));

			m_function_stack[m_function_index] = State.pL; // overwrite pIn
			m_forked_handle = m_primary_handle;
			int secondary_handle = m_interval_tree
					.getSecondaryFromPrimary(m_primary_handle);
			m_next_primary_handle = m_interval_tree.getLPTR_(m_primary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			return true;
		}

		private boolean pL_() {
			m_primary_handle = m_next_primary_handle;

			if (m_primary_handle == -1) {
				m_function_stack[m_function_index] = State.pR; // overwrite pL
				m_next_primary_handle = m_interval_tree
						.getRPTR_(m_forked_handle);
				return true;
			}

			double discriminant = m_interval_tree
					.getDiscriminant_(m_primary_handle);

			if (discriminant < m_query.vmin) {
				int secondary_handle = m_interval_tree
						.getSecondaryFromPrimary(m_primary_handle);
				m_next_primary_handle = m_interval_tree
						.getRPTR_(m_primary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree
							.getLast_(secondary_handle);
					m_function_stack[++m_function_index] = State.right;
				}

				return true;
			}

			assert (m_query.contains(discriminant));

			int secondary_handle = m_interval_tree
					.getSecondaryFromPrimary(m_primary_handle);
			m_next_primary_handle = m_interval_tree.getLPTR_(m_primary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			int rptr = m_interval_tree.getRPTR_(m_primary_handle);

			if (rptr != -1) {
				m_tertiary_stack.add(rptr); // we'll search this in the pT state
			}

			return true;
		}

		private boolean pR_() {
			m_primary_handle = m_next_primary_handle;

			if (m_primary_handle == -1) {
				m_function_stack[m_function_index] = State.pT; // overwrite pR
				return true;
			}

			double discriminant = m_interval_tree
					.getDiscriminant_(m_primary_handle);

			if (m_query.vmax < discriminant) {
				int secondary_handle = m_interval_tree
						.getSecondaryFromPrimary(m_primary_handle);
				m_next_primary_handle = m_interval_tree
						.getLPTR_(m_primary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree
							.getFirst_(secondary_handle);
					m_function_stack[++m_function_index] = State.left;
				}

				return true;
			}

			assert (m_query.contains(discriminant));

			int secondary_handle = m_interval_tree
					.getSecondaryFromPrimary(m_primary_handle);

			m_next_primary_handle = m_interval_tree.getRPTR_(m_primary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			int lptr = m_interval_tree.getLPTR_(m_primary_handle);

			if (lptr != -1) {
				m_tertiary_stack.add(lptr); // we'll search this in the pT state
			}

			return true;
		}

		private boolean pT_() {
			if (m_tertiary_stack.size() == 0) {
				m_function_index = -1;
				m_current_end_handle = -1;
				return false;
			}

			m_primary_handle = m_tertiary_stack
					.get(m_tertiary_stack.size() - 1);
			m_tertiary_stack.resize(m_tertiary_stack.size() - 1);

			int secondary_handle = m_interval_tree
					.getSecondaryFromPrimary(m_primary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			if (m_interval_tree.getLPTR_(m_primary_handle) != -1)
				m_tertiary_stack
						.add(m_interval_tree.getLPTR_(m_primary_handle));

			if (m_interval_tree.getRPTR_(m_primary_handle) != -1)
				m_tertiary_stack
						.add(m_interval_tree.getRPTR_(m_primary_handle));

			return true;
		}

		private boolean left_() {
			m_current_end_handle = m_next_end_handle;

			if (m_current_end_handle != -1
					&& IntervalTreeImpl.isLeft_(getCurrentEndIndex_())
					&& m_interval_tree.getValue_(getCurrentEndIndex_()) <= m_query.vmax) {
				m_next_end_handle = getNext_();
				return false;
			}

			m_function_index--;
			return true;
		}

		private boolean right_() {
			m_current_end_handle = m_next_end_handle;

			if (m_current_end_handle != -1
					&& IntervalTreeImpl.isRight_(getCurrentEndIndex_())
					&& m_interval_tree.getValue_(getCurrentEndIndex_()) >= m_query.vmin) {
				m_next_end_handle = getPrev_();
				return false;
			}

			m_function_index--;
			return true;
		}

		private boolean all_() {
			m_current_end_handle = m_next_end_handle;

			if (m_current_end_handle != -1
					&& IntervalTreeImpl.isLeft_(getCurrentEndIndex_())) {
				m_next_end_handle = getNext_();
				return false;
			}

			m_function_index--;
			return true;
		}

		private int getNext_() {
			if (!m_interval_tree.m_b_offline_dynamic)
				return m_interval_tree.m_secondary_lists
						.getNext(m_current_end_handle);

			return m_interval_tree.m_secondary_treaps
					.getNext(m_current_end_handle);
		}

		private int getPrev_() {
			if (!m_interval_tree.m_b_offline_dynamic)
				return m_interval_tree.m_secondary_lists
						.getPrev(m_current_end_handle);

			return m_interval_tree.m_secondary_treaps
					.getPrev(m_current_end_handle);
		}

		private int getCurrentEndIndex_() {
			if (!m_interval_tree.m_b_offline_dynamic)
				return m_interval_tree.m_secondary_lists
						.getData(m_current_end_handle);

			return m_interval_tree.m_secondary_treaps
					.getElement(m_current_end_handle);
		}
	}

	IntervalTreeImpl(boolean b_offline_dynamic) {
		m_b_offline_dynamic = b_offline_dynamic;
		m_b_constructing = false;
		m_b_construction_ended = false;
	}

	void startConstruction() {
		reset_(true);
	}

	void addInterval(Envelope1D interval) {
		if (!m_b_constructing)
			throw new GeometryException("invalid call");

		m_intervals.add(interval);
	}

	void addInterval(double min, double max) {
		if (!m_b_constructing)
			throw new GeometryException("invald call");

		m_intervals.add(new Envelope1D(min, max));
	}

	void endConstruction() {
		if (!m_b_constructing)
			throw new GeometryException("invalid call");

		m_b_constructing = false;
		m_b_construction_ended = true;

		if (!m_b_offline_dynamic) {
			insertIntervalsStatic_();
			m_c_count = m_intervals.size();
		}
	}

	/**
	 * Inserts the interval from the given index into the Interval_tree_impl.
	 * This operation can only be performed in the offline dynamic case. \param
	 * index The index containing the interval to be inserted.
	 */
	void insert(int index) {
		if (!m_b_offline_dynamic || !m_b_construction_ended)
			throw new IllegalArgumentException("invalid call");

		if (m_root == -1) {

			int size = m_intervals.size();

			if (m_b_sort_intervals) {
				// sort
				AttributeStreamOfInt32 end_point_indices_sorted = new AttributeStreamOfInt32(
						0);
				end_point_indices_sorted.reserve(2 * size);
				querySortedEndPointIndices_(end_point_indices_sorted);

				// remove duplicates
				m_end_indices_unique.reserve(2 * size);
				m_end_indices_unique.resize(0);
				querySortedDuplicatesRemoved_(end_point_indices_sorted);
				m_interval_handles.resize(size, -1);
				m_interval_handles.setRange(-1, 0, size);
				m_b_sort_intervals = false;
			} else {
				m_interval_handles.setRange(-1, 0, size);
			}

			m_root = createPrimaryNode_();
		}

		int interval_handle = insertIntervalEnd_(index << 1, m_root);
		int secondary_handle = getSecondaryFromInterval_(interval_handle);
		int right_end_handle = m_secondary_treaps.addElement((index << 1) + 1,
				secondary_handle);
		setRightEnd_(interval_handle, right_end_handle);
		m_interval_handles.set(index, interval_handle);
		m_c_count++;
		// assert(check_validation_());
	}

	/**
	 * Deletes the interval from the Interval_tree_impl. \param index The index
	 * containing the interval to be deleted from the Interval_tree_impl.
	 */
	void remove(int index) {
		if (!m_b_offline_dynamic || !m_b_construction_ended)
			throw new GeometryException("invalid call");

		int interval_handle = m_interval_handles.get(index);

		if (interval_handle == -1)
			throw new IllegalArgumentException(
					"the interval does not exist in the interval tree");

		m_interval_handles.set(index, -1);

		assert (getSecondaryFromInterval_(interval_handle) != -1);
		assert (getLeftEnd_(interval_handle) != -1);
		assert (getRightEnd_(interval_handle) != -1);

		m_c_count--;

		int size;
		int secondary_handle = getSecondaryFromInterval_(interval_handle);
		int primary_handle;

		primary_handle = m_secondary_treaps.getTreapData(secondary_handle);
		m_secondary_treaps.deleteNode(getLeftEnd_(interval_handle),
				secondary_handle);
		m_secondary_treaps.deleteNode(getRightEnd_(interval_handle),
				secondary_handle);
		size = m_secondary_treaps.size(secondary_handle);

		if (size == 0) {
			m_secondary_treaps.deleteTreap(secondary_handle);
			setSecondaryToPrimary_(primary_handle, -1);
		}

		m_interval_nodes.deleteElement(interval_handle);
		int tertiary_handle = getPPTR_(primary_handle);
		int lptr = getLPTR_(primary_handle);
		int rptr = getRPTR_(primary_handle);

		int iterations = 0;
		while (!(size > 0 || primary_handle == m_root || (lptr != -1 && rptr != -1))) {
			assert (size == 0);
			assert (lptr == -1 || rptr == -1);
			assert (primary_handle != 0);

			if (primary_handle == getLPTR_(tertiary_handle)) {
				if (lptr != -1) {
					setLPTR_(tertiary_handle, lptr);
					setPPTR_(lptr, tertiary_handle);
					setLPTR_(primary_handle, -1);
					setPPTR_(primary_handle, -1);
				} else if (rptr != -1) {
					setLPTR_(tertiary_handle, rptr);
					setPPTR_(rptr, tertiary_handle);
					setRPTR_(primary_handle, -1);
					setPPTR_(primary_handle, -1);
				} else {
					setLPTR_(tertiary_handle, -1);
					setPPTR_(primary_handle, -1);
				}
			} else {
				if (lptr != -1) {
					setRPTR_(tertiary_handle, lptr);
					setPPTR_(lptr, tertiary_handle);
					setLPTR_(primary_handle, -1);
					setPPTR_(primary_handle, -1);
				} else if (rptr != -1) {
					setRPTR_(tertiary_handle, rptr);
					setPPTR_(rptr, tertiary_handle);
					setRPTR_(primary_handle, -1);
					setPPTR_(primary_handle, -1);
				} else {
					setRPTR_(tertiary_handle, -1);
					setPPTR_(primary_handle, -1);
				}
			}

			iterations++;
			primary_handle = tertiary_handle;
			secondary_handle = getSecondaryFromPrimary(primary_handle);
			size = (secondary_handle != -1 ? m_secondary_treaps
					.size(secondary_handle) : 0);
			lptr = getLPTR_(primary_handle);
			rptr = getRPTR_(primary_handle);
			tertiary_handle = getPPTR_(primary_handle);
		}

		assert (iterations <= 2);
		// assert(check_validation_());
	}

	/*
	 * Resets the Interval_tree_impl to an empty state, but maintains a handle
	 * on the current intervals.
	 */
	void reset() {
		if (!m_b_offline_dynamic || !m_b_construction_ended)
			throw new IllegalArgumentException("invalid call");

		reset_(false);
	}

	/**
	 * Returns the number of intervals stored in the Interval_tree_impl
	 */
	int size() {
		return m_c_count;
	}

	/**
	 * Gets an iterator on the Interval_tree_impl using the input Envelope_1D
	 * interval as the query. To reuse the existing iterator on the same
	 * Interval_tree_impl but with a new query, use the reset_iterator function
	 * on the Interval_tree_iterator_impl. \param query The Envelope_1D interval
	 * used for the query. \param tolerance The tolerance used for the
	 * intersection tests.
	 */
	IntervalTreeIteratorImpl getIterator(Envelope1D query, double tolerance) {
		return new IntervalTreeImpl.IntervalTreeIteratorImpl(this, query,
				tolerance);
	}

	/**
	 * Gets an iterator on the Interval_tree_impl using the input double as the
	 * stabbing query. To reuse the existing iterator on the same
	 * Interval_tree_impl but with a new query, use the reset_iterator function
	 * on the Interval_tree_iterator_impl. \param query The double used for the
	 * stabbing query. \param tolerance The tolerance used for the intersection
	 * tests.
	 */
	IntervalTreeIteratorImpl getIterator(double query, double tolerance) {
		return new IntervalTreeImpl.IntervalTreeIteratorImpl(this, query,
				tolerance);
	}

	/**
	 * Gets an iterator on the Interval_tree_impl.
	 */
	IntervalTreeIteratorImpl getIterator() {
		return new IntervalTreeImpl.IntervalTreeIteratorImpl(this);
	}

	private static final class SecondaryComparator extends Treap.Comparator {
		SecondaryComparator(IntervalTreeImpl interval_tree) {
			m_interval_tree = interval_tree;
		}

		@Override
		public int compare(Treap treap, int e_1, int node) {
			int e_2 = treap.getElement(node);
			double v_1 = m_interval_tree.getValue_(e_1);
			double v_2 = m_interval_tree.getValue_(e_2);

			if (v_1 < v_2)
				return -1;
			if (v_1 == v_2) {
				if (isLeft_(e_1) && isRight_(e_2))
					return -1;
				if (isLeft_(e_2) && isRight_(e_1))
					return 1;
				return 0;
			}
			return 1;
		}

		private IntervalTreeImpl m_interval_tree;
	};

	private boolean m_b_offline_dynamic;
	private ArrayList<Envelope1D> m_intervals;
	private StridedIndexTypeCollection m_primary_nodes; // 8 elements for
														// offline dynamic case,
														// 7 elements for static
														// case
	private StridedIndexTypeCollection m_interval_nodes; // 3 elements
	private AttributeStreamOfInt32 m_interval_handles; // for offline dynamic
														// case
	private IndexMultiDCList m_secondary_lists; // for static case
	private Treap m_secondary_treaps; // for off-line dynamic case
	private AttributeStreamOfInt32 m_end_indices_unique; // for both offline
															// dynamic and
															// static cases
	private int m_c_count;
	private int m_root;
	private boolean m_b_sort_intervals;
	private boolean m_b_constructing;
	private boolean m_b_construction_ended;

	private void querySortedEndPointIndices_(AttributeStreamOfInt32 end_indices) {
		int size = m_intervals.size();

		for (int i = 0; i < 2 * size; i++)
			end_indices.add(i);

		sortEndIndices_(end_indices, 0, 2 * size);
	}

	private void querySortedDuplicatesRemoved_(
			AttributeStreamOfInt32 end_indices_sorted) {
		// remove duplicates

		double prev = NumberUtils.TheNaN;
		for (int i = 0; i < end_indices_sorted.size(); i++) {
			int e = end_indices_sorted.get(i);
			double v = getValue_(e);

			if (v != prev) {
				m_end_indices_unique.add(e);
				prev = v;
			}
		}
	}

	private void insertIntervalsStatic_() {
		int size = m_intervals.size();

		assert (m_b_sort_intervals);

		// sort
		AttributeStreamOfInt32 end_indices_sorted = new AttributeStreamOfInt32(
				0);
		end_indices_sorted.reserve(2 * size);
		querySortedEndPointIndices_(end_indices_sorted);

		// remove duplicates
		m_end_indices_unique.reserve(2 * size);
		m_end_indices_unique.resize(0);
		querySortedDuplicatesRemoved_(end_indices_sorted);

		assert (m_primary_nodes.size() == 0);
		m_interval_nodes.setCapacity(size); // one for each interval being
											// inserted. each element contains a
											// primary node, a left secondary
											// node, and a right secondary node.
		m_secondary_lists.reserveNodes(2 * size); // one for each end point of
													// the original interval set
													// (not the unique set)

		AttributeStreamOfInt32 interval_handles = (AttributeStreamOfInt32) AttributeStreamBase
				.createIndexStream(size);
		interval_handles.setRange(-1, 0, size);

		m_root = createPrimaryNode_();

		for (int i = 0; i < end_indices_sorted.size(); i++) {
			int e = end_indices_sorted.get(i);
			int interval_handle = interval_handles.get(e >> 1);

			if (interval_handle != -1) {// insert the right end point
				assert (isRight_(e));
				int secondary_handle = getSecondaryFromInterval_(interval_handle);
				setRightEnd_(interval_handle,
						m_secondary_lists.addElement(secondary_handle, e));
			} else {// insert the left end point
				assert (isLeft_(e));
				interval_handle = insertIntervalEnd_(e, m_root);
				interval_handles.set(e >> 1, interval_handle);
			}
		}

		assert (m_secondary_lists.getNodeCount() == 2 * size);
	}

	private int insertIntervalEnd_(int end_index, int root) {
		assert (isLeft_(end_index));
		int primary_handle = root;
		int tertiary_handle = root;
		int ptr = root;
		int secondary_handle;
		int interval_handle = -1;
		int il = 0, ir = m_end_indices_unique.size() - 1, im = 0;
		int index = end_index >> 1;
		double discriminant_tertiary = NumberUtils.TheNaN;
		double discriminant_ptr = NumberUtils.TheNaN;
		boolean bSearching = true;

		double min = getMin_(index);
		double max = getMax_(index);

		while (bSearching) {
			if (il < ir) {
				im = il + (ir - il) / 2;

				if (getDiscriminantIndex1_(primary_handle) == -1)
					setDiscriminantIndices_(primary_handle,
							m_end_indices_unique.get(im),
							m_end_indices_unique.get(im + 1));
			} else {
				assert (il == ir);
				assert (min == max);

				if (getDiscriminantIndex1_(primary_handle) == -1)
					setDiscriminantIndices_(primary_handle,
							m_end_indices_unique.get(il),
							m_end_indices_unique.get(il));
			}

			double discriminant = getDiscriminant_(primary_handle);
			assert (!NumberUtils.isNaN(discriminant));

			if (max < discriminant) {
				if (ptr != -1) {
					if (ptr == primary_handle) {
						tertiary_handle = primary_handle;
						discriminant_tertiary = discriminant;
						ptr = getLPTR_(primary_handle);

						if (ptr != -1)
							discriminant_ptr = getDiscriminant_(ptr);
						else
							discriminant_ptr = NumberUtils.TheNaN;
					} else if (discriminant_ptr > discriminant) {
						if (discriminant < discriminant_tertiary)
							setLPTR_(tertiary_handle, primary_handle);
						else
							setRPTR_(tertiary_handle, primary_handle);

						setRPTR_(primary_handle, ptr);

						if (m_b_offline_dynamic) {
							setPPTR_(primary_handle, tertiary_handle);
							setPPTR_(ptr, primary_handle);
						}

						tertiary_handle = primary_handle;
						discriminant_tertiary = discriminant;
						ptr = -1;
						discriminant_ptr = NumberUtils.TheNaN;

						assert (getLPTR_(primary_handle) == -1);
						assert (getRightPrimary_(primary_handle) != -1);
					}
				}

				int left_handle = getLeftPrimary_(primary_handle);

				if (left_handle == -1) {
					left_handle = createPrimaryNode_();
					setLeftPrimary_(primary_handle, left_handle);
				}

				primary_handle = left_handle;
				ir = im;

				continue;
			}

			if (min > discriminant) {
				if (ptr != -1) {
					if (ptr == primary_handle) {
						tertiary_handle = primary_handle;
						discriminant_tertiary = discriminant;
						ptr = getRPTR_(primary_handle);

						if (ptr != -1)
							discriminant_ptr = getDiscriminant_(ptr);
						else
							discriminant_ptr = NumberUtils.TheNaN;
					} else if (discriminant_ptr < discriminant) {
						if (discriminant < discriminant_tertiary)
							setLPTR_(tertiary_handle, primary_handle);
						else
							setRPTR_(tertiary_handle, primary_handle);

						setLPTR_(primary_handle, ptr);

						if (m_b_offline_dynamic) {
							setPPTR_(primary_handle, tertiary_handle);
							setPPTR_(ptr, primary_handle);
						}

						tertiary_handle = primary_handle;
						discriminant_tertiary = discriminant;
						ptr = -1;
						discriminant_ptr = NumberUtils.TheNaN;

						assert (getRPTR_(primary_handle) == -1);
						assert (getLeftPrimary_(primary_handle) != -1);
					}
				}

				int right_handle = getRightPrimary_(primary_handle);

				if (right_handle == -1) {
					right_handle = createPrimaryNode_();
					setRightPrimary_(primary_handle, right_handle);
				}

				primary_handle = right_handle;
				il = im + 1;

				continue;
			}

			secondary_handle = getSecondaryFromPrimary(primary_handle);

			if (secondary_handle == -1) {
				secondary_handle = createSecondary_(primary_handle);
				setSecondaryToPrimary_(primary_handle, secondary_handle);
			}

			int left_end_handle = addEndIndex(secondary_handle, end_index);
			interval_handle = createIntervalNode_();
			setSecondaryToInterval_(interval_handle, secondary_handle);
			setLeftEnd_(interval_handle, left_end_handle);

			if (primary_handle != ptr) {
				assert (primary_handle != -1);
				assert (getLPTR_(primary_handle) == -1
						&& getRPTR_(primary_handle) == -1 && (!m_b_offline_dynamic || getPPTR_(primary_handle) == -1));

				if (discriminant < discriminant_tertiary)
					setLPTR_(tertiary_handle, primary_handle);
				else
					setRPTR_(tertiary_handle, primary_handle);

				if (m_b_offline_dynamic)
					setPPTR_(primary_handle, tertiary_handle);

				if (ptr != -1) {
					if (discriminant_ptr < discriminant)
						setLPTR_(primary_handle, ptr);
					else
						setRPTR_(primary_handle, ptr);

					if (m_b_offline_dynamic)
						setPPTR_(ptr, primary_handle);
				}
			}

			bSearching = false;
		}

		return interval_handle;
	}

	private int createPrimaryNode_() {
		return m_primary_nodes.newElement();
	}

	private int createSecondary_(int primary_handle) {
		if (!m_b_offline_dynamic)
			return m_secondary_lists.createList(primary_handle);

		return m_secondary_treaps.createTreap(primary_handle);
	}

	private int createIntervalNode_() {
		return m_interval_nodes.newElement();
	}

	private void reset_(boolean b_new_intervals) {
		if (b_new_intervals) {
			m_b_sort_intervals = true;
			m_b_constructing = true;
			m_b_construction_ended = false;

			if (m_end_indices_unique == null)
				m_end_indices_unique = (AttributeStreamOfInt32) (AttributeStreamBase
						.createIndexStream(0));
			else
				m_end_indices_unique.resize(0);

			if (m_intervals == null)
				m_intervals = new ArrayList<Envelope1D>(0);
			else
				m_intervals.clear();
		} else {
			assert (m_b_offline_dynamic && m_b_construction_ended);
			m_b_sort_intervals = false;
		}

		if (m_b_offline_dynamic) {
			if (m_interval_handles == null) {
				m_interval_handles = (AttributeStreamOfInt32) (AttributeStreamBase
						.createIndexStream(0));
				m_secondary_treaps = new Treap();
				m_secondary_treaps.setComparator(new SecondaryComparator(this));
			} else {
				m_secondary_treaps.clear();
			}
		} else {
			if (m_secondary_lists == null)
				m_secondary_lists = new IndexMultiDCList();
			else
				m_secondary_lists.clear();
		}

		if (m_primary_nodes == null) {
			m_interval_nodes = new StridedIndexTypeCollection(3);
			m_primary_nodes = new StridedIndexTypeCollection(
					m_b_offline_dynamic ? 8 : 7);
		} else {
			m_interval_nodes.deleteAll(false);
			m_primary_nodes.deleteAll(false);
		}

		m_root = -1;
		m_c_count = 0;
	}

	private void setDiscriminantIndices_(int primary_handle, int e_1, int e_2) {
		setDiscriminantIndex1_(primary_handle, e_1);
		setDiscriminantIndex2_(primary_handle, e_2);
	}

	private double getDiscriminant_(int primary_handle) {
		int e_1 = getDiscriminantIndex1_(primary_handle);
		if (e_1 == -1)
			return NumberUtils.TheNaN;

		int e_2 = getDiscriminantIndex2_(primary_handle);
		assert (e_2 != -1);

		double v_1 = getValue_(e_1);
		double v_2 = getValue_(e_2);

		if (v_1 == v_2)
			return v_1;

		return 0.5 * (v_1 + v_2);
	}

	private boolean isActive_(int primary_handle) {
		int secondary_handle = getSecondaryFromPrimary(primary_handle);

		if (secondary_handle != -1)
			return true;

		int left_handle = getLeftPrimary_(primary_handle);

		if (left_handle == -1)
			return false;

		int right_handle = getRightPrimary_(primary_handle);

		if (right_handle == -1)
			return false;

		return true;
	}

	private void setDiscriminantIndex1_(int primary_handle, int end_index) {
		m_primary_nodes.setField(primary_handle, 0, end_index);
	}

	private void setDiscriminantIndex2_(int primary_handle, int end_index) {
		m_primary_nodes.setField(primary_handle, 1, end_index);
	}

	private void setLeftPrimary_(int primary_handle, int left_handle) {
		m_primary_nodes.setField(primary_handle, 3, left_handle);
	}

	private void setRightPrimary_(int primary_handle, int right_handle) {
		m_primary_nodes.setField(primary_handle, 4, right_handle);
	}

	private void setSecondaryToPrimary_(int primary_handle, int secondary_handle) {
		m_primary_nodes.setField(primary_handle, 2, secondary_handle);
	}

	private void setLPTR_(int primary_handle, int lptr) {
		m_primary_nodes.setField(primary_handle, 5, lptr);
	}

	private void setRPTR_(int primary_handle, int rptr) {
		m_primary_nodes.setField(primary_handle, 6, rptr);
	}

	private void setPPTR_(int primary_handle, int pptr) {
		m_primary_nodes.setField(primary_handle, 7, pptr);
	}

	private void setSecondaryToInterval_(int interval_handle,
			int secondary_handle) {
		m_interval_nodes.setField(interval_handle, 0, secondary_handle);
	}

	private int addEndIndex(int secondary_handle, int end_index) {
		int end_index_handle;

		if (!m_b_offline_dynamic)
			end_index_handle = m_secondary_lists.addElement(secondary_handle,
					end_index);
		else
			end_index_handle = m_secondary_treaps.addElement(end_index,
					secondary_handle);

		return end_index_handle;
	}

	private void setLeftEnd_(int interval_handle, int left_end_handle) {
		m_interval_nodes.setField(interval_handle, 1, left_end_handle);
	}

	private void setRightEnd_(int interval_handle, int right_end_handle) {
		m_interval_nodes.setField(interval_handle, 2, right_end_handle);
	}

	private int getFirst_(int secondary_handle) {
		if (!m_b_offline_dynamic)
			return m_secondary_lists.getFirst(secondary_handle);

		return m_secondary_treaps.getFirst(secondary_handle);
	}

	private int getLast_(int secondary_handle) {
		if (!m_b_offline_dynamic)
			return m_secondary_lists.getLast(secondary_handle);

		return m_secondary_treaps.getLast(secondary_handle);
	}

	private static boolean isLeft_(int end_index) {
		return (end_index & 0x1) == 0;
	}

	private static boolean isRight_(int end_index) {
		return (end_index & 0x1) == 1;
	}

	private int getDiscriminantIndex1_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 0);
	}

	private int getDiscriminantIndex2_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 1);
	}

	private int getSecondaryFromPrimary(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 2);
	}

	private int getLeftPrimary_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 3);
	}

	private int getRightPrimary_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 4);
	}

	private int getLPTR_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 5);
	}

	private int getRPTR_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 6);
	}

	private int getPPTR_(int primary_handle) {
		return m_primary_nodes.getField(primary_handle, 7);
	}

	private int getSecondaryFromInterval_(int interval_handle) {
		return m_interval_nodes.getField(interval_handle, 0);
	}

	private int getLeftEnd_(int interval_handle) {
		return m_interval_nodes.getField(interval_handle, 1);
	}

	private int getRightEnd_(int interval_handle) {
		return m_interval_nodes.getField(interval_handle, 2);
	}

	private double getMin_(int i) {
		Envelope1D interval = m_intervals.get(i);
		return interval.vmin;
	}

	private double getMax_(int i) {
		Envelope1D interval = m_intervals.get(i);
		return interval.vmax;
	}

	// *********** Helpers for Bucket sort**************
	private BucketSort m_bucket_sort;

	private void sortEndIndices_(AttributeStreamOfInt32 end_indices,
			int begin_, int end_) {
		if (m_bucket_sort == null)
			m_bucket_sort = new BucketSort();

		IntervalTreeBucketSortHelper sorter = new IntervalTreeBucketSortHelper(
				this);
		m_bucket_sort.sort(end_indices, begin_, end_, sorter);
	}

	private void sortEndIndicesHelper_(AttributeStreamOfInt32 end_indices,
			int begin_, int end_) {
		end_indices.Sort(begin_, end_, new EndPointsComparer(this));
	}

	private double getValue_(int e) {
		Envelope1D interval = m_intervals.get(e >> 1);
		double v = (isLeft_(e) ? interval.vmin : interval.vmax);
		return v;
	}

	private static final class EndPointsComparer extends
			AttributeStreamOfInt32.IntComparator { // For user sort
		EndPointsComparer(IntervalTreeImpl interval_tree) {
			m_interval_tree = interval_tree;
		}

		@Override
		public int compare(int e_1, int e_2) {
			double v_1 = m_interval_tree.getValue_(e_1);
			double v_2 = m_interval_tree.getValue_(e_2);

			if (v_1 < v_2 || (v_1 == v_2 && isLeft_(e_1) && isRight_(e_2)))
				return -1;

			return 1;
		}

		private IntervalTreeImpl m_interval_tree;
	}

	private class IntervalTreeBucketSortHelper extends ClassicSort { // For
																		// bucket
																		// sort
		IntervalTreeBucketSortHelper(IntervalTreeImpl interval_tree) {
			m_interval_tree = interval_tree;
		}

		@Override
		public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
			m_interval_tree.sortEndIndicesHelper_(indices, begin, end);
		}

		@Override
		public double getValue(int e) {
			return m_interval_tree.getValue_(e);
		}

		private IntervalTreeImpl m_interval_tree;
	}
}
