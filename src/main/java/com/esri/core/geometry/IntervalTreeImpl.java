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

import java.util.ArrayList;

final class IntervalTreeImpl {
	private void sortEndIndices_(AttributeStreamOfInt32 end_indices, int begin_, int end_) {
		IntervalTreeBucketSortHelper sorter = new IntervalTreeBucketSortHelper(this);
		BucketSort bucket_sort = new BucketSort();
		bucket_sort.sort(end_indices, begin_, end_, sorter);
	}

	private void sortEndIndicesHelper_(AttributeStreamOfInt32 end_indices, int begin_, int end_) {
		end_indices.Sort(begin_, end_, new EndPointsComparer(this));
	}

	private double getValue_(int e) {
		if (!m_b_envelopes_ref) {
			Envelope1D interval = m_intervals.get(e >> 1);
			double v = (isLeft_(e) ? interval.vmin : interval.vmax);
			return v;
		}

		Envelope2D interval = m_envelopes_ref.get(e >> 1);
		double v = (isLeft_(e) ? interval.xmin : interval.xmax);
		return v;
	}

	private static final class EndPointsComparer extends AttributeStreamOfInt32.IntComparator { // For user sort

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

	private class IntervalTreeBucketSortHelper extends ClassicSort { // For bucket sort

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

	IntervalTreeImpl(boolean b_offline_dynamic) {
		m_b_envelopes_ref = false;
		m_b_offline_dynamic = b_offline_dynamic;
		m_b_constructing = false;
		m_b_construction_ended = false;
	}

	void addEnvelopesRef(ArrayList<Envelope2D> envelopes) {
		reset_(true, true);
		m_b_envelopes_ref = true;
		m_envelopes_ref = envelopes;

		m_b_constructing = false;
		m_b_construction_ended = true;

		if (!m_b_offline_dynamic) {
			insertIntervalsStatic_();
			m_c_count = m_envelopes_ref.size();
		}
	}

	void startConstruction() {
		reset_(true, false);
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

	/*
	 * Resets the Interval_tree_impl to an empty state, but maintains a handle
	 * on the current intervals.
	 */
	void reset() {
		if (!m_b_offline_dynamic || !m_b_construction_ended)
			throw new IllegalArgumentException("invalid call");

		reset_(false, m_b_envelopes_ref);
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
		return new IntervalTreeImpl.IntervalTreeIteratorImpl(this, query, tolerance);
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
		return new IntervalTreeImpl.IntervalTreeIteratorImpl(this, query, tolerance);
	}

	/**
	 * Gets an iterator on the Interval_tree_impl.
	 */
	IntervalTreeIteratorImpl getIterator() {
		return new IntervalTreeImpl.IntervalTreeIteratorImpl(this);
	}

	private boolean m_b_envelopes_ref;
	private boolean m_b_offline_dynamic;
	private ArrayList<Envelope1D> m_intervals;
	private ArrayList<Envelope2D> m_envelopes_ref;
	private StridedIndexTypeCollection m_tertiary_nodes; // 5 elements for offline dynamic case, 4 elements for static case
	private StridedIndexTypeCollection m_interval_nodes; // 3 elements
	private AttributeStreamOfInt32 m_interval_handles; // for offline dynamic// case
	private IndexMultiDCList m_secondary_lists; // for static case
	private Treap m_secondary_treaps; // for off-line dynamic case
	private AttributeStreamOfInt32 m_end_indices_unique; // for both offline dynamic and static cases
	private int m_c_count;
	private int m_root;
	private boolean m_b_sort_intervals;
	private boolean m_b_constructing;
	private boolean m_b_construction_ended;

      /* m_tertiary_nodes
      * 0: m_discriminant_index_1
      * 1: m_secondary
      * 2: m_lptr
      * 3: m_rptr
      * 4: m_pptr
      */

	private void querySortedEndPointIndices_(AttributeStreamOfInt32 end_indices) {
		int size = (!m_b_envelopes_ref ? m_intervals.size() : m_envelopes_ref.size());

		for (int i = 0; i < 2 * size; i++)
			end_indices.add(i);

		sortEndIndices_(end_indices, 0, 2 * size);
	}

	private void querySortedDuplicatesRemoved_(AttributeStreamOfInt32 end_indices_sorted) {
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

	void insert(int index) {
		if (!m_b_offline_dynamic || !m_b_construction_ended)
			throw new IllegalArgumentException("invalid call");

		if (m_root == -1) {

			int size = (!m_b_envelopes_ref ? m_intervals.size() : m_envelopes_ref.size());

			if (m_b_sort_intervals) {
				// sort
				AttributeStreamOfInt32 end_point_indices_sorted = new AttributeStreamOfInt32(0);
				end_point_indices_sorted.reserve(2 * size);
				querySortedEndPointIndices_(end_point_indices_sorted);

				// remove duplicates
				m_end_indices_unique.resize(0);
				querySortedDuplicatesRemoved_(end_point_indices_sorted);
				m_interval_handles.resize(size, -1);
				m_interval_handles.setRange(-1, 0, size);
				m_b_sort_intervals = false;
			} else {
				m_interval_handles.setRange(-1, 0, size);
			}

			m_root = createRoot_();
		}

		int interval_handle = insertIntervalEnd_(index << 1, m_root);
		int secondary_handle = getSecondaryFromInterval_(interval_handle);
		int right_end_handle = m_secondary_treaps.addElement((index << 1) + 1, secondary_handle);
		setRightEnd_(interval_handle, right_end_handle);
		m_interval_handles.set(index, interval_handle);
		m_c_count++;
		// assert(check_validation_());
	}

	private void insertIntervalsStatic_() {
		int size = (!m_b_envelopes_ref ? m_intervals.size() : m_envelopes_ref.size());

		assert (m_b_sort_intervals);

		// sort
		AttributeStreamOfInt32 end_indices_sorted = new AttributeStreamOfInt32(0);
		end_indices_sorted.reserve(2 * size);
		querySortedEndPointIndices_(end_indices_sorted);

		// remove duplicates
		m_end_indices_unique.resize(0);
		querySortedDuplicatesRemoved_(end_indices_sorted);

		assert (m_tertiary_nodes.size() == 0);
		m_interval_nodes.setCapacity(size); // one for each interval being inserted. each element contains a tertiary node, a left secondary node, and a right secondary node.
		m_secondary_lists.reserveNodes(2 * size); // one for each end point of the original interval set (not the unique set)

		AttributeStreamOfInt32 interval_handles = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(size);
		interval_handles.setRange(-1, 0, size);

		m_root = createRoot_();

		for (int i = 0; i < end_indices_sorted.size(); i++) {
			int e = end_indices_sorted.get(i);
			int interval_handle = interval_handles.get(e >> 1);

			if (interval_handle != -1) {// insert the right end point
				assert (isRight_(e));
				int secondary_handle = getSecondaryFromInterval_(interval_handle);
				setRightEnd_(interval_handle, m_secondary_lists.addElement(secondary_handle, e));
			} else {// insert the left end point
				assert (isLeft_(e));
				interval_handle = insertIntervalEnd_(e, m_root);
				interval_handles.set(e >> 1, interval_handle);
			}
		}

		assert (m_secondary_lists.getNodeCount() == 2 * size);
	}

	private int createRoot_() {
		int discriminant_index_1 = calculateDiscriminantIndex1_(0, m_end_indices_unique.size() - 1);
		return createTertiaryNode_(discriminant_index_1);
	}

	private int insertIntervalEnd_(int end_index, int root) {
		assert (isLeft_(end_index));
		int pptr = -1;
		int ptr = root;
		int secondary_handle = -1;
		int interval_handle = -1;
		int il = 0, ir = m_end_indices_unique.size() - 1, im = 0;
		int index = end_index >> 1;
		double discriminant_pptr = NumberUtils.NaN();
		double discriminant_ptr = NumberUtils.NaN();
		boolean bSearching = true;

		double min = getMin_(index);
		double max = getMax_(index);

		int discriminant_index_1 = -1;

		while (bSearching) {
			im = il + (ir - il) / 2;
			assert (il != ir || min == max);
			discriminant_index_1 = calculateDiscriminantIndex1_(il, ir);
			double discriminant = getDiscriminantFromIndex1_(discriminant_index_1);
			assert (!NumberUtils.isNaN(discriminant));

			if (max < discriminant) {
				if (ptr != -1) {
					if (discriminant_index_1 == getDiscriminantIndex1_(ptr)) {
						assert (getDiscriminantFromIndex1_(discriminant_index_1) == getDiscriminant_(ptr));

						pptr = ptr;
						discriminant_pptr = discriminant;
						ptr = getLPTR_(ptr);

						if (ptr != -1)
							discriminant_ptr = getDiscriminant_(ptr);
						else
							discriminant_ptr = NumberUtils.NaN();
					} else if (discriminant_ptr > discriminant) {
						int tertiary_handle = createTertiaryNode_(discriminant_index_1);

						if (discriminant < discriminant_pptr)
							setLPTR_(pptr, tertiary_handle);
						else
							setRPTR_(pptr, tertiary_handle);

						setRPTR_(tertiary_handle, ptr);

						if (m_b_offline_dynamic) {
							setPPTR_(tertiary_handle, pptr);
							setPPTR_(ptr, tertiary_handle);
						}

						pptr = tertiary_handle;
						discriminant_pptr = discriminant;
						ptr = -1;
						discriminant_ptr = NumberUtils.NaN();
					}
				}

				ir = im;

				continue;
			}

			if (min > discriminant) {
				if (ptr != -1) {
					if (discriminant_index_1 == getDiscriminantIndex1_(ptr)) {
						assert (getDiscriminantFromIndex1_(discriminant_index_1) == getDiscriminant_(ptr));

						pptr = ptr;
						discriminant_pptr = discriminant;
						ptr = getRPTR_(ptr);

						if (ptr != -1)
							discriminant_ptr = getDiscriminant_(ptr);
						else
							discriminant_ptr = NumberUtils.NaN();
					} else if (discriminant_ptr < discriminant) {
						int tertiary_handle = createTertiaryNode_(discriminant_index_1);

						if (discriminant < discriminant_pptr)
							setLPTR_(pptr, tertiary_handle);
						else
							setRPTR_(pptr, tertiary_handle);

						setLPTR_(tertiary_handle, ptr);

						if (m_b_offline_dynamic) {
							setPPTR_(tertiary_handle, pptr);
							setPPTR_(ptr, tertiary_handle);
						}

						pptr = tertiary_handle;
						discriminant_pptr = discriminant;
						ptr = -1;
						discriminant_ptr = NumberUtils.NaN();
					}
				}

				il = im + 1;

				continue;
			}

			int tertiary_handle = -1;

			if (ptr == -1 || discriminant_index_1 != getDiscriminantIndex1_(ptr)) {
				tertiary_handle = createTertiaryNode_(discriminant_index_1);
			} else {
				tertiary_handle = ptr;
			}

			secondary_handle = getSecondaryFromTertiary_(tertiary_handle);

			if (secondary_handle == -1) {
				secondary_handle = createSecondary_(tertiary_handle);
				setSecondaryToTertiary_(tertiary_handle, secondary_handle);
			}

			int left_end_handle = addEndIndex_(secondary_handle, end_index);
			interval_handle = createIntervalNode_();
			setSecondaryToInterval_(interval_handle, secondary_handle);
			setLeftEnd_(interval_handle, left_end_handle);

			if (ptr == -1 || discriminant_index_1 != getDiscriminantIndex1_(ptr)) {
				assert (tertiary_handle != -1);
				assert (getLPTR_(tertiary_handle) == -1 && getRPTR_(tertiary_handle) == -1 && (!m_b_offline_dynamic || getPPTR_(tertiary_handle) == -1));

				if (discriminant < discriminant_pptr)
					setLPTR_(pptr, tertiary_handle);
				else
					setRPTR_(pptr, tertiary_handle);

				if (m_b_offline_dynamic)
					setPPTR_(tertiary_handle, pptr);

				if (ptr != -1) {
					if (discriminant_ptr < discriminant)
						setLPTR_(tertiary_handle, ptr);
					else
						setRPTR_(tertiary_handle, ptr);

					if (m_b_offline_dynamic)
						setPPTR_(ptr, tertiary_handle);
				}
			}

			bSearching = false;
			break;
		}

		return interval_handle;
	}

	void remove(int index) {
		if (!m_b_offline_dynamic || !m_b_construction_ended)
			throw new GeometryException("invalid call");

		int interval_handle = m_interval_handles.get(index);

		if (interval_handle == -1)
			throw new GeometryException("the interval does not exist in the interval tree");

		m_interval_handles.set(index, -1);

		assert (getSecondaryFromInterval_(interval_handle) != -1);
		assert (getLeftEnd_(interval_handle) != -1);
		assert (getRightEnd_(interval_handle) != -1);

		m_c_count--;

		int size;
		int secondary_handle = getSecondaryFromInterval_(interval_handle);
		int tertiary_handle = -1;

		tertiary_handle = m_secondary_treaps.getTreapData(secondary_handle);
		m_secondary_treaps.deleteNode(getLeftEnd_(interval_handle), secondary_handle);
		m_secondary_treaps.deleteNode(getRightEnd_(interval_handle), secondary_handle);
		size = m_secondary_treaps.size(secondary_handle);

		if (size == 0) {
			m_secondary_treaps.deleteTreap(secondary_handle);
			setSecondaryToTertiary_(tertiary_handle, -1);
		}

		m_interval_nodes.deleteElement(interval_handle);
		int pptr = getPPTR_(tertiary_handle);
		int lptr = getLPTR_(tertiary_handle);
		int rptr = getRPTR_(tertiary_handle);

		int iterations = 0;
		while (!(size > 0 || tertiary_handle == m_root || (lptr != -1 && rptr != -1))) {
			assert (size == 0);
			assert (lptr == -1 || rptr == -1);
			assert (tertiary_handle != 0);

			if (tertiary_handle == getLPTR_(pptr)) {
				if (lptr != -1) {
					setLPTR_(pptr, lptr);
					setPPTR_(lptr, pptr);
					setLPTR_(tertiary_handle, -1);
					setPPTR_(tertiary_handle, -1);
				} else if (rptr != -1) {
					setLPTR_(pptr, rptr);
					setPPTR_(rptr, pptr);
					setRPTR_(tertiary_handle, -1);
					setPPTR_(tertiary_handle, -1);
				} else {
					setLPTR_(pptr, -1);
					setPPTR_(tertiary_handle, -1);
				}
			} else {
				if (lptr != -1) {
					setRPTR_(pptr, lptr);
					setPPTR_(lptr, pptr);
					setLPTR_(tertiary_handle, -1);
					setPPTR_(tertiary_handle, -1);
				} else if (rptr != -1) {
					setRPTR_(pptr, rptr);
					setPPTR_(rptr, pptr);
					setRPTR_(tertiary_handle, -1);
					setPPTR_(tertiary_handle, -1);
				} else {
					setRPTR_(pptr, -1);
					setPPTR_(tertiary_handle, -1);
				}
			}

			m_tertiary_nodes.deleteElement(tertiary_handle);

			iterations++;
			tertiary_handle = pptr;
			secondary_handle = getSecondaryFromTertiary_(tertiary_handle);
			size = (secondary_handle != -1 ? m_secondary_treaps.size(secondary_handle) : 0);
			lptr = getLPTR_(tertiary_handle);
			rptr = getRPTR_(tertiary_handle);
			pptr = getPPTR_(tertiary_handle);
		}

		assert (iterations <= 2);
		//assert(check_validation_());
	}

	private void reset_(boolean b_new_intervals, boolean b_envelopes_ref) {
		if (b_new_intervals) {
			m_b_envelopes_ref = false;
			m_envelopes_ref = null;

			m_b_sort_intervals = true;
			m_b_constructing = true;
			m_b_construction_ended = false;

			if (m_end_indices_unique == null)
				m_end_indices_unique = (AttributeStreamOfInt32) (AttributeStreamBase.createIndexStream(0));
			else
				m_end_indices_unique.resize(0);

			if (!b_envelopes_ref) {
				if (m_intervals == null)
					m_intervals = new ArrayList<Envelope1D>(0);
				else
					m_intervals.clear();
			} else {
				if (m_intervals != null)
					m_intervals.clear();

				m_b_envelopes_ref = true;
			}
		} else {
			assert (m_b_offline_dynamic && m_b_construction_ended);
			m_b_sort_intervals = false;
		}

		if (m_b_offline_dynamic) {
			if (m_interval_handles == null) {
				m_interval_handles = (AttributeStreamOfInt32) (AttributeStreamBase.createIndexStream(0));
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

		if (m_tertiary_nodes == null) {
			m_interval_nodes = new StridedIndexTypeCollection(3);
			m_tertiary_nodes = new StridedIndexTypeCollection(m_b_offline_dynamic ? 5 : 4);
		} else {
			m_interval_nodes.deleteAll(false);
			m_tertiary_nodes.deleteAll(false);
		}

		m_root = -1;
		m_c_count = 0;
	}

	private double getDiscriminant_(int tertiary_handle) {
		int discriminant_index_1 = getDiscriminantIndex1_(tertiary_handle);
		return getDiscriminantFromIndex1_(discriminant_index_1);
	}

	private double getDiscriminantFromIndex1_(int discriminant_index_1) {
		if (discriminant_index_1 == -1)
			return NumberUtils.NaN();

		if (discriminant_index_1 > 0) {
			int j = discriminant_index_1 - 2;
			int e_1 = m_end_indices_unique.get(j);
			int e_2 = m_end_indices_unique.get(j + 1);

			double v_1 = getValue_(e_1);
			double v_2 = getValue_(e_2);
			assert (v_1 < v_2);

			return 0.5 * (v_1 + v_2);
		}

		int j = -discriminant_index_1 - 2;
		assert (j >= 0);
		int e = m_end_indices_unique.get(j);
		double v = getValue_(e);

		return v;
	}

	private int calculateDiscriminantIndex1_(int il, int ir) {
		int discriminant_index_1;

		if (il < ir) {
			int im = il + (ir - il) / 2;
			discriminant_index_1 = im + 2; // positive discriminant means use average of im and im + 1
		} else {
			discriminant_index_1 = -(il + 2); // negative discriminant just means use il (-(il + 2) will never be -1)
		}

		return discriminant_index_1;
	}

	static final class IntervalTreeIteratorImpl {

		private IntervalTreeImpl m_interval_tree;
		private Envelope1D m_query = new Envelope1D();
		private int m_tertiary_handle;
		private int m_next_tertiary_handle;
		private int m_forked_handle;
		private int m_current_end_handle;
		private int m_next_end_handle;
		private AttributeStreamOfInt32 m_tertiary_stack = new AttributeStreamOfInt32(0);
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

		private int getNext_() {
			if (!m_interval_tree.m_b_offline_dynamic)
				return m_interval_tree.m_secondary_lists.getNext(m_current_end_handle);

			return m_interval_tree.m_secondary_treaps.getNext(m_current_end_handle);
		}

		private int getPrev_() {
			if (!m_interval_tree.m_b_offline_dynamic)
				return m_interval_tree.m_secondary_lists.getPrev(m_current_end_handle);

			return m_interval_tree.m_secondary_treaps.getPrev(m_current_end_handle);
		}

		private int getCurrentEndIndex_() {
			if (!m_interval_tree.m_b_offline_dynamic)
				return m_interval_tree.m_secondary_lists.getData(m_current_end_handle);

			return m_interval_tree.m_secondary_treaps.getElement(m_current_end_handle);
		}

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
					throw GeometryException.GeometryInternalError();
				}
			}

			if (m_current_end_handle != -1)
				return getCurrentEndIndex_() >> 1;

			return -1;
		}

		private boolean initialize_() {
			m_tertiary_handle = -1;
			m_next_tertiary_handle = -1;
			m_forked_handle = -1;
			m_current_end_handle = -1;

			if (m_interval_tree.m_tertiary_nodes != null && m_interval_tree.m_tertiary_nodes.size() > 0) {
				m_function_stack[0] = State.pIn; // overwrite initialize
				m_next_tertiary_handle = m_interval_tree.m_root;
				return true;
			}

			m_function_index = -1;
			return false;
		}

		private boolean pIn_() {
			m_tertiary_handle = m_next_tertiary_handle;

			if (m_tertiary_handle == -1) {
				m_function_index = -1;
				m_current_end_handle = -1;
				return false;
			}

			double discriminant = m_interval_tree.getDiscriminant_(m_tertiary_handle);

			if (m_query.vmax < discriminant) {
				int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);
				m_next_tertiary_handle = m_interval_tree.getLPTR_(m_tertiary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
					m_function_stack[++m_function_index] = State.left;
				}

				return true;
			}

			if (discriminant < m_query.vmin) {
				int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);
				m_next_tertiary_handle = m_interval_tree.getRPTR_(m_tertiary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree.getLast_(secondary_handle);
					m_function_stack[++m_function_index] = State.right;
				}

				return true;
			}

			assert (m_query.contains(discriminant));

			m_function_stack[m_function_index] = State.pL; // overwrite pIn
			m_forked_handle = m_tertiary_handle;
			int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);
			m_next_tertiary_handle = m_interval_tree.getLPTR_(m_tertiary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			return true;
		}

		private boolean pL_() {
			m_tertiary_handle = m_next_tertiary_handle;

			if (m_tertiary_handle == -1) {
				m_function_stack[m_function_index] = State.pR; // overwrite pL
				m_next_tertiary_handle = m_interval_tree.getRPTR_(m_forked_handle);
				return true;
			}

			double discriminant = m_interval_tree.getDiscriminant_(m_tertiary_handle);

			if (discriminant < m_query.vmin) {
				int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);
				m_next_tertiary_handle = m_interval_tree.getRPTR_(m_tertiary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree.getLast_(secondary_handle);
					m_function_stack[++m_function_index] = State.right;
				}

				return true;
			}

			assert (m_query.contains(discriminant));

			int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);
			m_next_tertiary_handle = m_interval_tree.getLPTR_(m_tertiary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			int rptr = m_interval_tree.getRPTR_(m_tertiary_handle);

			if (rptr != -1) {
				m_tertiary_stack.add(rptr); // we'll search this in the pT state
			}

			return true;
		}

		private boolean pR_() {
			m_tertiary_handle = m_next_tertiary_handle;

			if (m_tertiary_handle == -1) {
				m_function_stack[m_function_index] = State.pT; // overwrite pR
				return true;
			}

			double discriminant = m_interval_tree.getDiscriminant_(m_tertiary_handle);

			if (m_query.vmax < discriminant) {
				int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);
				m_next_tertiary_handle = m_interval_tree.getLPTR_(m_tertiary_handle);

				if (secondary_handle != -1) {
					m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
					m_function_stack[++m_function_index] = State.left;
				}

				return true;
			}

			assert (m_query.contains(discriminant));

			int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);

			m_next_tertiary_handle = m_interval_tree.getRPTR_(m_tertiary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			int lptr = m_interval_tree.getLPTR_(m_tertiary_handle);

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

			m_tertiary_handle = m_tertiary_stack.get(m_tertiary_stack.size() - 1);
			m_tertiary_stack.resize(m_tertiary_stack.size() - 1);

			int secondary_handle = m_interval_tree.getSecondaryFromTertiary_(m_tertiary_handle);

			if (secondary_handle != -1) {
				m_next_end_handle = m_interval_tree.getFirst_(secondary_handle);
				m_function_stack[++m_function_index] = State.all;
			}

			if (m_interval_tree.getLPTR_(m_tertiary_handle) != -1)
				m_tertiary_stack.add(m_interval_tree.getLPTR_(m_tertiary_handle));

			if (m_interval_tree.getRPTR_(m_tertiary_handle) != -1)
				m_tertiary_stack.add(m_interval_tree.getRPTR_(m_tertiary_handle));

			return true;
		}

		private boolean left_() {
			m_current_end_handle = m_next_end_handle;

			if (m_current_end_handle != -1 && IntervalTreeImpl.isLeft_(getCurrentEndIndex_()) && m_interval_tree.getValue_(getCurrentEndIndex_()) <= m_query.vmax) {
				m_next_end_handle = getNext_();
				return false;
			}

			m_function_index--;
			return true;
		}

		private boolean right_() {
			m_current_end_handle = m_next_end_handle;

			if (m_current_end_handle != -1 && IntervalTreeImpl.isRight_(getCurrentEndIndex_()) && m_interval_tree.getValue_(getCurrentEndIndex_()) >= m_query.vmin) {
				m_next_end_handle = getPrev_();
				return false;
			}

			m_function_index--;
			return true;
		}

		private boolean all_() {
			m_current_end_handle = m_next_end_handle;

			if (m_current_end_handle != -1 && IntervalTreeImpl.isLeft_(getCurrentEndIndex_())) {
				m_next_end_handle = getNext_();
				return false;
			}

			m_function_index--;
			return true;
		}

		IntervalTreeIteratorImpl(IntervalTreeImpl interval_tree, Envelope1D query, double tolerance) {
			m_interval_tree = interval_tree;
			m_tertiary_stack.reserve(20);
			resetIterator(query, tolerance);
		}

		IntervalTreeIteratorImpl(IntervalTreeImpl interval_tree, double query, double tolerance) {
			m_interval_tree = interval_tree;
			m_tertiary_stack.reserve(20);
			resetIterator(query, tolerance);
		}

		IntervalTreeIteratorImpl(IntervalTreeImpl interval_tree) {
			m_interval_tree = interval_tree;
			m_tertiary_stack.reserve(20);
			m_function_index = -1;
		}

		void resetIterator(Envelope1D query, double tolerance) {
			m_query.vmin = query.vmin - tolerance;
			m_query.vmax = query.vmax + tolerance;
			m_tertiary_stack.resize(0);
			m_function_index = 0;
			m_function_stack[0] = State.initialize;
		}

		void resetIterator(double query_min, double query_max, double tolerance) {
			m_query.vmin = query_min - tolerance;
			m_query.vmax = query_max + tolerance;
			m_tertiary_stack.resize(0);
			m_function_index = 0;
			m_function_stack[0] = State.initialize;
		}

		void resetIterator(double query, double tolerance) {
			m_query.vmin = query - tolerance;
			m_query.vmax = query + tolerance;
			m_tertiary_stack.resize(0);
			m_function_index = 0;
			m_function_stack[0] = State.initialize;
		}
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
	}

	private int createTertiaryNode_(int discriminant_index_1) {
		int tertiary_handle = m_tertiary_nodes.newElement();
		setDiscriminantIndex1_(tertiary_handle, discriminant_index_1);
		return tertiary_handle;
	}

	private int createSecondary_(int tertiary_handle) {
		if (!m_b_offline_dynamic)
			return m_secondary_lists.createList(tertiary_handle);

		return m_secondary_treaps.createTreap(tertiary_handle);
	}

	private int createIntervalNode_() {
		return m_interval_nodes.newElement();
	}

	private void setDiscriminantIndex1_(int tertiary_handle, int end_index) {
		m_tertiary_nodes.setField(tertiary_handle, 0, end_index);
	}

	private void setSecondaryToTertiary_(int tertiary_handle, int secondary_handle) {
		m_tertiary_nodes.setField(tertiary_handle, 1, secondary_handle);
	}

	private void setLPTR_(int tertiary_handle, int lptr) {
		m_tertiary_nodes.setField(tertiary_handle, 2, lptr);
	}

	private void setRPTR_(int tertiary_handle, int rptr) {
		m_tertiary_nodes.setField(tertiary_handle, 3, rptr);
	}

	private void setPPTR_(int tertiary_handle, int pptr) {
		m_tertiary_nodes.setField(tertiary_handle, 4, pptr);
	}

	private void setSecondaryToInterval_(int interval_handle, int secondary_handle) {
		m_interval_nodes.setField(interval_handle, 0, secondary_handle);
	}

	private int addEndIndex_(int secondary_handle, int end_index) {
		int end_index_handle;

		if (!m_b_offline_dynamic)
			end_index_handle = m_secondary_lists.addElement(secondary_handle, end_index);
		else
			end_index_handle = m_secondary_treaps.addElement(end_index, secondary_handle);

		return end_index_handle;
	}

	private void setLeftEnd_(int interval_handle, int left_end_handle) {
		m_interval_nodes.setField(interval_handle, 1, left_end_handle);
	}

	private void setRightEnd_(int interval_handle, int right_end_handle) {
		m_interval_nodes.setField(interval_handle, 2, right_end_handle);
	}

	private int getDiscriminantIndex1_(int tertiary_handle) {
		return m_tertiary_nodes.getField(tertiary_handle, 0);
	}

	private int getSecondaryFromTertiary_(int tertiary_handle) {
		return m_tertiary_nodes.getField(tertiary_handle, 1);
	}

	private int getLPTR_(int tertiary_handle) {
		return m_tertiary_nodes.getField(tertiary_handle, 2);
	}

	private int getRPTR_(int tertiary_handle) {
		return m_tertiary_nodes.getField(tertiary_handle, 3);
	}

	private int getPPTR_(int tertiary_handle) {
		return m_tertiary_nodes.getField(tertiary_handle, 4);
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
		return (!m_b_envelopes_ref ? m_intervals.get(i).vmin : m_envelopes_ref.get(i).xmin);
	}

	private double getMax_(int i) {
		return (!m_b_envelopes_ref ? m_intervals.get(i).vmax : m_envelopes_ref.get(i).xmax);
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
}
