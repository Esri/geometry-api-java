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

class Envelope2DIntersectorImpl {
	/*
	 * Constructor for Envelope_2D_intersector.
	 */
	Envelope2DIntersectorImpl() {
		m_function = -1;
		m_tolerance = 0.0;
		reset_();
	}

	void startConstruction() {
		reset_();
		m_b_add_red_red = true;

		if (m_envelopes_red == null) {
			m_elements_red = new AttributeStreamOfInt32(0);
			m_envelopes_red = new ArrayList<Envelope2D>(0);
		} else {
			m_elements_red.resizePreserveCapacity(0);
			m_envelopes_red.clear();
		}
	}

	void addEnvelope(int element, Envelope2D envelope) {
		if (!m_b_add_red_red)
			throw new GeometryException("invalid call");

		Envelope2D e = new Envelope2D();
		e.setCoords(envelope);
		m_elements_red.add(element);
		m_envelopes_red.add(e);
	}

	void endConstruction() {
		if (!m_b_add_red_red)
			throw new GeometryException("invalid call");

		m_b_add_red_red = false;

		if (m_envelopes_red != null && m_envelopes_red.size() > 0) {
			m_function = State.initialize;
			m_b_done = false;
		}
	}

	void startRedConstruction() {
		reset_();
		m_b_add_red = true;

		if (m_envelopes_red == null) {
			m_elements_red = new AttributeStreamOfInt32(0);
			m_envelopes_red = new ArrayList<Envelope2D>(0);
		} else {
			m_elements_red.resizePreserveCapacity(0);
			m_envelopes_red.clear();
		}
	}

	void addRedEnvelope(int element, Envelope2D red_envelope) {
		if (!m_b_add_red)
			throw new GeometryException("invalid call");

		Envelope2D e = new Envelope2D();
		e.setCoords(red_envelope);
		m_elements_red.add(element);
		m_envelopes_red.add(e);
	}

	void endRedConstruction() {
		if (!m_b_add_red)
			throw new GeometryException("invalid call");

		m_b_add_red = false;

		if (m_envelopes_red != null && m_envelopes_red.size() > 0 && m_envelopes_blue != null && m_envelopes_blue.size() > 0) {
			if (m_function == -1)
				m_function = State.initializeRedBlue;
			else if (m_function == State.initializeBlue)
				m_function = State.initializeRedBlue;
			else if (m_function != State.initializeRedBlue)
				m_function = State.initializeRed;

			m_b_done = false;
		}
	}

	void startBlueConstruction() {
		reset_();
		m_b_add_blue = true;

		if (m_envelopes_blue == null) {
			m_elements_blue = new AttributeStreamOfInt32(0);
			m_envelopes_blue = new ArrayList<Envelope2D>(0);
		} else {
			m_elements_blue.resizePreserveCapacity(0);
			m_envelopes_blue.clear();
		}
	}

	void addBlueEnvelope(int element, Envelope2D blue_envelope) {
		if (!m_b_add_blue)
			throw new GeometryException("invalid call");

		Envelope2D e = new Envelope2D();
		e.setCoords(blue_envelope);
		m_elements_blue.add(element);
		m_envelopes_blue.add(e);
	}

	void endBlueConstruction() {
		if (!m_b_add_blue)
			throw new GeometryException("invalid call");

		m_b_add_blue = false;

		if (m_envelopes_red != null && m_envelopes_red.size() > 0 && m_envelopes_blue != null && m_envelopes_blue.size() > 0) {
			if (m_function == -1)
				m_function = State.initializeRedBlue;
			else if (m_function == State.initializeRed)
				m_function = State.initializeRedBlue;
			else if (m_function != State.initializeRedBlue)
				m_function = State.initializeBlue;

			m_b_done = false;
		}
	}

	/*
	 * Moves the iterator to the next intersecting pair of envelopes.Returns
	 * true if an intersecting pair is found. You can call get_handle_a() and
	 * get_handle_b() to get the index of each envelope in the current
	 * intersection. Otherwise if false is returned, then are no more
	 * intersections (if at all).
	 */
	boolean next() {
		if (m_b_done)
			return false;

		boolean b_searching = true;
		while (b_searching) {
			switch (m_function) {
			case State.initialize:
				b_searching = initialize_();
				break;
			case State.initializeRed:
				b_searching = initializeRed_();
				break;
			case State.initializeBlue:
				b_searching = initializeBlue_();
				break;
			case State.initializeRedBlue:
				b_searching = initializeRedBlue_();
				break;
			case State.sweep:
				b_searching = sweep_();
				break;
			case State.sweepBruteForce:
				b_searching = sweepBruteForce_();
				break;
			case State.sweepRedBlueBruteForce:
				b_searching = sweepRedBlueBruteForce_();
				break;
			case State.sweepRedBlue:
				b_searching = sweepRedBlue_();
				break;
			case State.sweepRed:
				b_searching = sweepRed_();
				break;
			case State.sweepBlue:
				b_searching = sweepBlue_();
				break;
			case State.iterate:
				b_searching = iterate_();
				break;
			case State.iterateRed:
				b_searching = iterateRed_();
				break;
			case State.iterateBlue:
				b_searching = iterateBlue_();
				break;
			case State.iterateBruteForce:
				b_searching = iterateBruteForce_();
				break;
			case State.iterateRedBlueBruteForce:
				b_searching = iterateRedBlueBruteForce_();
				break;
			case State.resetRed:
				b_searching = resetRed_();
				break;
			case State.resetBlue:
				b_searching = resetBlue_();
				break;
			default:
				throw GeometryException.GeometryInternalError();
			}
		}

		if (m_b_done)
			return false;

		return true;
	}

	/*
	 * Returns the index of the first envelope in the intersection. In the
	 * red/blue case, this will be an index to the red envelopes.
	 */
	int getHandleA() {
		return m_envelope_handle_a;
	}

	/*
	 * Returns the index of the second envelope in the intersection. In the
	 * red/blue case, this will be an index to the blue envelopes.
	 */
	int getHandleB() {
		return m_envelope_handle_b;
	}

	/*
	 * Sets the tolerance used for the intersection tests.\param tolerance The
	 * tolerance used to determine intersection.
	 */
	void setTolerance(double tolerance) {
		m_tolerance = tolerance;
	}

	/*
	 * Returns a reference to the envelope at the given handle. Use this for the red/red intersection case.
	 */
	Envelope2D getEnvelope(int handle) {
		return m_envelopes_red.get(handle);
	}

	/*
	 * Returns the user element associated with handle. Use this for the red/red intersection case.
	 */
	int getElement(int handle) {
		return m_elements_red.read(handle);
	}

	/*
	 * Returns a reference to the red envelope at handle_a.
	 */
	Envelope2D getRedEnvelope(int handle_a) {
		return m_envelopes_red.get(handle_a);
	}

	/*
	 * Returns a reference to the blue envelope at handle_b.
	 */
	Envelope2D getBlueEnvelope(int handle_b) {
		return m_envelopes_blue.get(handle_b);
	}

	/*
	 * Returns the user element associated with handle_a.
	 */
	int getRedElement(int handle_a) {
		return m_elements_red.read(handle_a);
	}

	/*
	 * Returns the user element associated with handle_b.
	 */
	int getBlueElement(int handle_b) {
		return m_elements_blue.read(handle_b);
	}

	private double m_tolerance;
	private int m_sweep_index_red;
	private int m_sweep_index_blue;
	private int m_envelope_handle_a;
	private int m_envelope_handle_b;
	private IntervalTreeImpl m_interval_tree_red;
	private IntervalTreeImpl m_interval_tree_blue;
	private IntervalTreeImpl.IntervalTreeIteratorImpl m_iterator_red;
	private IntervalTreeImpl.IntervalTreeIteratorImpl m_iterator_blue;
	private Envelope2D m_envelope_helper = new Envelope2D();

	private ArrayList<Envelope2D> m_envelopes_red;
	private ArrayList<Envelope2D> m_envelopes_blue;
	private AttributeStreamOfInt32 m_elements_red;
	private AttributeStreamOfInt32 m_elements_blue;

	private AttributeStreamOfInt32 m_sorted_end_indices_red;
	private AttributeStreamOfInt32 m_sorted_end_indices_blue;

	private int m_queued_list_red;
	private int m_queued_list_blue;
	private IndexMultiDCList m_queued_envelopes;
	private AttributeStreamOfInt32 m_queued_indices_red;
	private AttributeStreamOfInt32 m_queued_indices_blue;
	private boolean m_b_add_red;
	private boolean m_b_add_blue;
	private boolean m_b_add_red_red;
	private boolean m_b_done;

	private static boolean isTop_(int y_end_point_handle) {
		return (y_end_point_handle & 0x1) == 1;
	}

	private static boolean isBottom_(int y_end_point_handle) {
		return (y_end_point_handle & 0x1) == 0;
	}

	private void reset_() {
		m_b_add_red = false;
		m_b_add_blue = false;
		m_b_add_red_red = false;
		m_sweep_index_red = -1;
		m_sweep_index_blue = -1;
		m_queued_list_red = -1;
		m_queued_list_blue = -1;
		m_b_done = true;
	}

	private boolean initialize_() {
		m_envelope_handle_a = -1;
		m_envelope_handle_b = -1;

		if (m_envelopes_red.size() < 10) {
			m_sweep_index_red = m_envelopes_red.size();
			m_function = State.sweepBruteForce;
			return true;
		}

		if (m_interval_tree_red == null) {
			m_interval_tree_red = new IntervalTreeImpl(true);
			m_sorted_end_indices_red = new AttributeStreamOfInt32(0);
		}

		m_interval_tree_red.addEnvelopesRef(m_envelopes_red);

		if (m_iterator_red == null) {
			m_iterator_red = m_interval_tree_red.getIterator();
		}

		m_sorted_end_indices_red.reserve(2 * m_envelopes_red.size());
		m_sorted_end_indices_red.resize(0);

		for (int i = 0; i < 2 * m_envelopes_red.size(); i++)
			m_sorted_end_indices_red.add(i);

		sortYEndIndices_(m_sorted_end_indices_red, 0, 2 * m_envelopes_red.size(), true);

		m_sweep_index_red = 2 * m_envelopes_red.size();

		m_function = State.sweep; // overwrite initialize_

		return true;
	}

	private boolean initializeRed_() {
		m_envelope_handle_a = -1;
		m_envelope_handle_b = -1;

		if (m_envelopes_red.size() < 10 || m_envelopes_blue.size() < 10) {
			m_sweep_index_red = m_envelopes_red.size();
			m_function = State.sweepRedBlueBruteForce;
			return true;
		}

		if (m_interval_tree_red == null) {
			m_interval_tree_red = new IntervalTreeImpl(true);
			m_sorted_end_indices_red = new AttributeStreamOfInt32(0);
		}

		m_interval_tree_red.addEnvelopesRef(m_envelopes_red);

		if (m_iterator_red == null) {
			m_iterator_red = m_interval_tree_red.getIterator();
		}

		m_sorted_end_indices_red.reserve(2 * m_envelopes_red.size());
		m_sorted_end_indices_red.resize(0);

		for (int i = 0; i < 2 * m_envelopes_red.size(); i++)
			m_sorted_end_indices_red.add(i);

		sortYEndIndices_(m_sorted_end_indices_red, 0, m_sorted_end_indices_red.size(), true);
		m_sweep_index_red = m_sorted_end_indices_red.size();

		if (m_queued_list_red != -1) {
			m_queued_envelopes.deleteList(m_queued_list_red);
			m_queued_indices_red.resize(0);
			m_queued_list_red = -1;
		}

		m_function = State.sweepRedBlue; // overwrite initialize_

		return resetBlue_();
	}

	private boolean initializeBlue_() {
		m_envelope_handle_a = -1;
		m_envelope_handle_b = -1;

		if (m_envelopes_red.size() < 10 || m_envelopes_blue.size() < 10) {
			m_sweep_index_red = m_envelopes_red.size();
			m_function = State.sweepRedBlueBruteForce;
			return true;
		}

		if (m_interval_tree_blue == null) {
			m_interval_tree_blue = new IntervalTreeImpl(true);
			m_sorted_end_indices_blue = new AttributeStreamOfInt32(0);
		}

		m_interval_tree_blue.addEnvelopesRef(m_envelopes_blue);

		if (m_iterator_blue == null) {
			m_iterator_blue = m_interval_tree_blue.getIterator();
		}

		m_sorted_end_indices_blue.reserve(2 * m_envelopes_blue.size());
		m_sorted_end_indices_blue.resize(0);

		for (int i = 0; i < 2 * m_envelopes_blue.size(); i++)
			m_sorted_end_indices_blue.add(i);

		sortYEndIndices_(m_sorted_end_indices_blue, 0, m_sorted_end_indices_blue.size(), false);
		m_sweep_index_blue = m_sorted_end_indices_blue.size();

		if (m_queued_list_blue != -1) {
			m_queued_envelopes.deleteList(m_queued_list_blue);
			m_queued_indices_blue.resize(0);
			m_queued_list_blue = -1;
		}

		m_function = State.sweepRedBlue; // overwrite initialize_

		return resetRed_();
	}

	private boolean initializeRedBlue_() {
		m_envelope_handle_a = -1;
		m_envelope_handle_b = -1;

		if (m_envelopes_red.size() < 10 || m_envelopes_blue.size() < 10) {
			m_sweep_index_red = m_envelopes_red.size();
			m_function = State.sweepRedBlueBruteForce;
			return true;
		}

		if (m_interval_tree_red == null) {
			m_interval_tree_red = new IntervalTreeImpl(true);
			m_sorted_end_indices_red = new AttributeStreamOfInt32(0);
		}

		if (m_interval_tree_blue == null) {
			m_interval_tree_blue = new IntervalTreeImpl(true);
			m_sorted_end_indices_blue = new AttributeStreamOfInt32(0);
		}

		m_interval_tree_red.addEnvelopesRef(m_envelopes_red);
		m_interval_tree_blue.addEnvelopesRef(m_envelopes_blue);

		if (m_iterator_red == null) {
			m_iterator_red = m_interval_tree_red.getIterator();
		}

		if (m_iterator_blue == null) {
			m_iterator_blue = m_interval_tree_blue.getIterator();
		}

		m_sorted_end_indices_red.reserve(2 * m_envelopes_red.size());
		m_sorted_end_indices_blue.reserve(2 * m_envelopes_blue.size());
		m_sorted_end_indices_red.resize(0);
		m_sorted_end_indices_blue.resize(0);

		for (int i = 0; i < 2 * m_envelopes_red.size(); i++)
			m_sorted_end_indices_red.add(i);

		for (int i = 0; i < 2 * m_envelopes_blue.size(); i++)
			m_sorted_end_indices_blue.add(i);

		sortYEndIndices_(m_sorted_end_indices_red, 0, m_sorted_end_indices_red.size(), true);
		sortYEndIndices_(m_sorted_end_indices_blue, 0, m_sorted_end_indices_blue.size(), false);

		m_sweep_index_red = m_sorted_end_indices_red.size();
		m_sweep_index_blue = m_sorted_end_indices_blue.size();

		if (m_queued_list_red != -1) {
			m_queued_envelopes.deleteList(m_queued_list_red);
			m_queued_indices_red.resize(0);
			m_queued_list_red = -1;
		}

		if (m_queued_list_blue != -1) {
			m_queued_envelopes.deleteList(m_queued_list_blue);
			m_queued_indices_blue.resize(0);
			m_queued_list_blue = -1;
		}

		m_function = State.sweepRedBlue; // overwrite initialize_

		return true;
	}

	private boolean sweep_() {
		int y_end_point_handle = m_sorted_end_indices_red.get(--m_sweep_index_red);
		int envelope_handle = y_end_point_handle >> 1;

		if (isBottom_(y_end_point_handle)) {
			m_interval_tree_red.remove(envelope_handle);

			if (m_sweep_index_red == 0) {
				m_envelope_handle_a = -1;
				m_envelope_handle_b = -1;
				m_b_done = true;
				return false;
			}

			return true;
		}

		m_iterator_red.resetIterator(m_envelopes_red.get(envelope_handle).xmin, m_envelopes_red.get(envelope_handle).xmax, m_tolerance);
		m_envelope_handle_a = envelope_handle;
		m_function = State.iterate;

		return true;
	}

	private boolean sweepBruteForce_() {// this isn't really a sweep, it just walks along the array of red envelopes backward.
		if (--m_sweep_index_red == -1) {
			m_envelope_handle_a = -1;
			m_envelope_handle_b = -1;
			m_b_done = true;
			return false;
		}

		m_envelope_handle_a = m_sweep_index_red;
		m_sweep_index_blue = m_sweep_index_red;
		m_function = State.iterateBruteForce;

		return true;
	}

	private boolean sweepRedBlueBruteForce_() {// this isn't really a sweep, it just walks along the array of red envelopes backward.
		if (--m_sweep_index_red == -1) {
			m_envelope_handle_a = -1;
			m_envelope_handle_b = -1;
			m_b_done = true;
			return false;
		}

		m_envelope_handle_a = m_sweep_index_red;
		m_sweep_index_blue = m_envelopes_blue.size();
		m_function = State.iterateRedBlueBruteForce;

		return true;
	}

	private boolean sweepRedBlue_() {// controls whether we want to sweep the red envelopes or sweep the blue envelopes
		int y_end_point_handle_red = m_sorted_end_indices_red.get(m_sweep_index_red - 1);
		int y_end_point_handle_blue = m_sorted_end_indices_blue.get(m_sweep_index_blue - 1);

		double y_red = getAdjustedValue_(y_end_point_handle_red, true);
		double y_blue = getAdjustedValue_(y_end_point_handle_blue, false);

		if (y_red > y_blue)
			return sweepRed_();
		if (y_red < y_blue)
			return sweepBlue_();

		if (isTop_(y_end_point_handle_red))
			return sweepRed_();
		if (isTop_(y_end_point_handle_blue))
			return sweepBlue_();

		return sweepRed_(); // arbitrary. can call sweep_blue_ instead and would also work correctly
	}

	private boolean sweepRed_() {
		int y_end_point_handle_red = m_sorted_end_indices_red.get(--m_sweep_index_red);
		int envelope_handle_red = y_end_point_handle_red >> 1;

		if (isBottom_(y_end_point_handle_red)) {
			if (m_queued_list_red != -1 && m_queued_indices_red.get(envelope_handle_red) != -1) {
				m_queued_envelopes.deleteElement(m_queued_list_red, m_queued_indices_red.get(envelope_handle_red));
				m_queued_indices_red.set(envelope_handle_red, -1);
			} else
				m_interval_tree_red.remove(envelope_handle_red);

			if (m_sweep_index_red == 0) {
				m_envelope_handle_a = -1;
				m_envelope_handle_b = -1;
				m_b_done = true;
				return false;
			}

			return true;
		}

		if (m_queued_list_blue != -1 && m_queued_envelopes.getListSize(m_queued_list_blue) > 0) {
			int node = m_queued_envelopes.getFirst(m_queued_list_blue);
			while (node != -1) {
				int e = m_queued_envelopes.getData(node);
				m_interval_tree_blue.insert(e);
				m_queued_indices_blue.set(e, -1);
				int next_node = m_queued_envelopes.getNext(node);
				m_queued_envelopes.deleteElement(m_queued_list_blue, node);
				node = next_node;
			}
		}

		if (m_interval_tree_blue.size() > 0) {
			m_iterator_blue.resetIterator(m_envelopes_red.get(envelope_handle_red).xmin, m_envelopes_red.get(envelope_handle_red).xmax, m_tolerance);
			m_envelope_handle_a = envelope_handle_red;
			m_function = State.iterateBlue;
		} else {
			if (m_queued_list_red == -1) {
				if (m_queued_envelopes == null)
					m_queued_envelopes = new IndexMultiDCList();

				m_queued_indices_red = new AttributeStreamOfInt32(0);
				m_queued_indices_red.resize(m_envelopes_red.size(), -1);
				m_queued_indices_red.setRange(-1, 0, m_envelopes_red.size());
				m_queued_list_red = m_queued_envelopes.createList(1);
			}

			m_queued_indices_red.set(envelope_handle_red, m_queued_envelopes.addElement(m_queued_list_red, envelope_handle_red));
			m_function = State.sweepRedBlue;
		}

		return true;
	}

	private boolean sweepBlue_() {
		int y_end_point_handle_blue = m_sorted_end_indices_blue.get(--m_sweep_index_blue);
		int envelope_handle_blue = y_end_point_handle_blue >> 1;

		if (isBottom_(y_end_point_handle_blue)) {
			if (m_queued_list_blue != -1 && m_queued_indices_blue.get(envelope_handle_blue) != -1) {
				m_queued_envelopes.deleteElement(m_queued_list_blue, m_queued_indices_blue.get(envelope_handle_blue));
				m_queued_indices_blue.set(envelope_handle_blue, -1);
			} else
				m_interval_tree_blue.remove(envelope_handle_blue);

			if (m_sweep_index_blue == 0) {
				m_envelope_handle_a = -1;
				m_envelope_handle_b = -1;
				m_b_done = true;
				return false;
			}

			return true;
		}

		if (m_queued_list_red != -1 && m_queued_envelopes.getListSize(m_queued_list_red) > 0) {
			int node = m_queued_envelopes.getFirst(m_queued_list_red);
			while (node != -1) {
				int e = m_queued_envelopes.getData(node);
				m_interval_tree_red.insert(e);
				m_queued_indices_red.set(e, -1);
				int next_node = m_queued_envelopes.getNext(node);
				m_queued_envelopes.deleteElement(m_queued_list_red, node);
				node = next_node;
			}
		}

		if (m_interval_tree_red.size() > 0) {
			m_iterator_red.resetIterator(m_envelopes_blue.get(envelope_handle_blue).xmin, m_envelopes_blue.get(envelope_handle_blue).xmax, m_tolerance);
			m_envelope_handle_b = envelope_handle_blue;
			m_function = State.iterateRed;
		} else {
			if (m_queued_list_blue == -1) {
				if (m_queued_envelopes == null)
					m_queued_envelopes = new IndexMultiDCList();

				m_queued_indices_blue = new AttributeStreamOfInt32(0);
				m_queued_indices_blue.resize(m_envelopes_blue.size(), -1);
				m_queued_indices_blue.setRange(-1, 0, m_envelopes_blue.size());
				m_queued_list_blue = m_queued_envelopes.createList(0);
			}

			m_queued_indices_blue.set(envelope_handle_blue, m_queued_envelopes.addElement(m_queued_list_blue, envelope_handle_blue));
			m_function = State.sweepRedBlue;
		}

		return true;
	}

	private boolean iterate_() {
		m_envelope_handle_b = m_iterator_red.next();
		if (m_envelope_handle_b != -1)
			return false;

		int envelope_handle = m_sorted_end_indices_red.get(m_sweep_index_red) >> 1;
		m_interval_tree_red.insert(envelope_handle);
		m_function = State.sweep;

		return true;
	}

	private boolean iterateRed_() {
		m_envelope_handle_a = m_iterator_red.next();
		if (m_envelope_handle_a != -1)
			return false;

		m_envelope_handle_a = -1;
		m_envelope_handle_b = -1;

		int envelope_handle_blue = m_sorted_end_indices_blue.get(m_sweep_index_blue) >> 1;
		m_interval_tree_blue.insert(envelope_handle_blue);
		m_function = State.sweepRedBlue;

		return true;
	}

	private boolean iterateBlue_() {
		m_envelope_handle_b = m_iterator_blue.next();
		if (m_envelope_handle_b != -1)
			return false;

		int envelope_handle_red = m_sorted_end_indices_red.get(m_sweep_index_red) >> 1;
		m_interval_tree_red.insert(envelope_handle_red);
		m_function = State.sweepRedBlue;

		return true;
	}

	private boolean iterateBruteForce_() {
		if (--m_sweep_index_blue == -1) {
			m_function = State.sweepBruteForce;
			return true;
		}

		m_envelope_helper.setCoords(m_envelopes_red.get(m_sweep_index_red));
		Envelope2D envelope_b = m_envelopes_red.get(m_sweep_index_blue);

		m_envelope_helper.inflate(m_tolerance, m_tolerance);
		if (m_envelope_helper.isIntersecting(envelope_b)) {
			m_envelope_handle_b = m_sweep_index_blue;
			return false;
		}

		return true;
	}

	private boolean iterateRedBlueBruteForce_() {
		if (--m_sweep_index_blue == -1) {
			m_function = State.sweepRedBlueBruteForce;
			return true;
		}

		m_envelope_helper.setCoords(m_envelopes_red.get(m_sweep_index_red));
		Envelope2D envelope_b = m_envelopes_blue.get(m_sweep_index_blue);

		m_envelope_helper.inflate(m_tolerance, m_tolerance);
		if (m_envelope_helper.isIntersecting(envelope_b)) {
			m_envelope_handle_b = m_sweep_index_blue;
			return false;
		}

		return true;
	}

	private boolean resetRed_() {
		if (m_interval_tree_red == null) {
			m_b_done = true;
			return false;
		}

		m_sweep_index_red = m_sorted_end_indices_red.size();

		if (m_interval_tree_red.size() > 0)
			m_interval_tree_red.reset();

		if (m_queued_list_red != -1) {
			m_queued_envelopes.deleteList(m_queued_list_red);
			m_queued_indices_red.resize(0);
			m_queued_list_red = -1;
		}

		m_b_done = false;
		return true;
	}

	private boolean resetBlue_() {
		if (m_interval_tree_blue == null) {
			m_b_done = true;
			return false;
		}

		m_sweep_index_blue = m_sorted_end_indices_blue.size();

		if (m_interval_tree_blue.size() > 0)
			m_interval_tree_blue.reset();

		if (m_queued_list_blue != -1) {
			m_queued_envelopes.deleteList(m_queued_list_blue);
			m_queued_indices_blue.resize(0);
			m_queued_list_blue = -1;
		}

		m_b_done = false;
		return true;
	}

	private int m_function;

	private interface State {
		static final int initialize = 0;
		static final int initializeRed = 1;
		static final int initializeBlue = 2;
		static final int initializeRedBlue = 3;
		static final int sweep = 4;
		static final int sweepBruteForce = 5;
		static final int sweepRedBlueBruteForce = 6;
		static final int sweepRedBlue = 7;
		static final int sweepRed = 8;
		static final int sweepBlue = 9;
		static final int iterate = 10;
		static final int iterateRed = 11;
		static final int iterateBlue = 12;
		static final int iterateBruteForce = 13;
		static final int iterateRedBlueBruteForce = 14;
		static final int resetRed = 15;
		static final int resetBlue = 16;
	}

	// *********** Helpers for Bucket sort**************
	private BucketSort m_bucket_sort;

	private void sortYEndIndices_(AttributeStreamOfInt32 end_indices, int begin_, int end_, boolean b_red) {
		if (m_bucket_sort == null)
			m_bucket_sort = new BucketSort();

		Envelope2DBucketSortHelper sorter = new Envelope2DBucketSortHelper(this, b_red);
		m_bucket_sort.sort(end_indices, begin_, end_, sorter);
	}

	private void sortYEndIndicesHelper_(AttributeStreamOfInt32 end_indices, int begin_, int end_, boolean b_red) {
		end_indices.Sort(begin_, end_, new EndPointsComparer(this, b_red));
	}

	private double getAdjustedValue_(int e, boolean b_red) {
		double dy = 0.5 * m_tolerance;
		if (b_red) {
			Envelope2D envelope_red = m_envelopes_red.get(e >> 1);
			double y = (isBottom_(e) ? envelope_red.ymin - dy : envelope_red.ymax + dy);
			return y;
		}

		Envelope2D envelope_blue = m_envelopes_blue.get(e >> 1);
		double y = (isBottom_(e) ? envelope_blue.ymin - dy : envelope_blue.ymax + dy);
		return y;
	}

	private static final class EndPointsComparer extends AttributeStreamOfInt32.IntComparator {// For user sort

		EndPointsComparer(Envelope2DIntersectorImpl intersector, boolean b_red) {
			m_intersector = intersector;
			m_b_red = b_red;
		}

		@Override
		public int compare(int e_1, int e_2) {
			double y1 = m_intersector.getAdjustedValue_(e_1, m_b_red);
			double y2 = m_intersector.getAdjustedValue_(e_2, m_b_red);

			if (y1 < y2 || (y1 == y2 && isBottom_(e_1) && isTop_(e_2)))
				return -1;

			return 1;
		}

		private Envelope2DIntersectorImpl m_intersector;
		private boolean m_b_red;
	}

	private static final class Envelope2DBucketSortHelper extends ClassicSort {// For

		// bucket
		// sort
		Envelope2DBucketSortHelper(Envelope2DIntersectorImpl intersector, boolean b_red) {
			m_intersector = intersector;
			m_b_red = b_red;
		}

		@Override
		public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
			m_intersector.sortYEndIndicesHelper_(indices, begin, end, m_b_red);
		}

		@Override
		public double getValue(int index) {
			return m_intersector.getAdjustedValue_(index, m_b_red);
		}

		private Envelope2DIntersectorImpl m_intersector;
		private boolean m_b_red;
	}
}
