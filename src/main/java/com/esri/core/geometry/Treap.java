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

final class Treap {
	static abstract class Comparator {
		Comparator() {
			m_b_notify_on_actions = false;
		}

		Comparator(boolean bNotifyOnActions) {
			m_b_notify_on_actions = bNotifyOnActions;
		}

		// Compares the element elm to the element contained in the given node
		abstract int compare(Treap treap, int elm, int node);

		// These virtual methods are called only when Comparator(true) ctro has
		// been used.
		void onDelete(int elm) {
		}

		void onSet(int elm) {
		}

		void onEndSearch(int elm) {
		}

		void onAddUniqueElementFailed(int elm) {
		}

		private boolean m_b_notify_on_actions;

		// void operator=(const Comparator&); // do not allow operator =
		void onDeleteImpl_(Treap treap, int node) {
			if (m_b_notify_on_actions)
				onDelete(treap.getElement(node));
		}

		void onSetImpl_(Treap treap, int node) {
			if (m_b_notify_on_actions)
				onSet(treap.getElement(node));
		}

		void onAddUniqueElementFailedImpl_(int elm) {
			if (m_b_notify_on_actions)
				onAddUniqueElementFailed(elm);
		}

		void onEndSearchImpl_(int elm) {
			if (m_b_notify_on_actions)
				onEndSearch(elm);
		}
	};

	static abstract class MonikerComparator {
		// Compares the moniker, contained in the MonikerComparator with the
		// element contained in the given node.
		abstract int compare(Treap treap, int node);
	};

	public Treap() {
		m_random = 124234251;
		m_b_balancing = true;
		m_touchFlag = 0;
		m_defaultTreap = nullNode();
		m_treapData = new StridedIndexTypeCollection(7);
		m_comparator = null;
	}

	// Sets the comparator
	public void setComparator(Comparator comparator) {
		m_comparator = comparator;
	}

	// Returns the comparator
	public Comparator getComparator() {
		return m_comparator;
	}

	// Stops auto-balancing
	public void disableBalancing() {
		m_b_balancing = false;
	}

	// Reserves memory for nodes givne number of nodes
	public void setCapacity(int capacity) {
		m_treapData.setCapacity(capacity);
	}

	// Create a new treap and returns the treap handle.
	public int createTreap(int treap_data) {
		int treap = m_treapData.newElement();
		setSize_(0, treap);
		setTreapData_(treap_data, treap);
		return treap;
	}

	// Deletes the treap at the given treap handle.
	public void deleteTreap(int treap) {
		m_treapData.deleteElement(treap);
	}

	// Adds new element to the treap. Allows duplicates to be added.
	public int addElement(int element, int treap) {
		int treap_;
		if (treap == -1) {
			if (m_defaultTreap == nullNode())
				m_defaultTreap = createTreap(-1);
			treap_ = m_defaultTreap;
		} else {
			treap_ = treap;
		}

		return addElement_(element, 0, treap_);
	}

	// Adds new element to the treap if it is not equal to other elements.
	// If the return value is -1, then get_duplicate_element reutrns the node of
	// the already existing element equal to element.
	public int addUniqueElement(int element, int treap) {
		int treap_;
		if (treap == -1) {
			if (m_defaultTreap == nullNode())
				m_defaultTreap = createTreap(-1);
			treap_ = m_defaultTreap;
		} else {
			treap_ = treap;
		}

		return addElement_(element, 1, treap_);
	}

	// Adds a new element to the treap that is known to be bigger or equal of
	// all elements already in the treap.
	// Use this method when adding elements from a sorted list for maximum
	// performance (it does not call the treap comparator).
	public int addBiggestElement(int element, int treap) {
		int treap_;
		if (treap == -1) {
			if (m_defaultTreap == nullNode())
				m_defaultTreap = createTreap(-1);
			treap_ = m_defaultTreap;
		} else {
			treap_ = treap;
		}

		if (getRoot_(treap_) == nullNode()) {
			int newNode = newNode_(element);
			setRoot_(newNode, treap_);
			addToList_(-1, newNode, treap_);
			return newNode;
		}

		int cur = getLast_(treap_);
		int newNode = newNode_(element);
		setRight_(cur, newNode);
		setParent_(newNode, cur);
		assert (m_b_balancing);// don't use this method for unbalanced tree, or
								// the performance will be bad.
		bubbleUp_(newNode);
		if (getParent(newNode) == nullNode())
			setRoot_(newNode, treap_);

		addToList_(-1, newNode, treap_);
		return newNode;
	}

	// template <class Iterator> void build_from_sorted(const Iterator& begin,
	// const Iterator& end);
	// Adds new element to the treap at the known position, thus avoiding a call
	// to the comparator.
	// If bCallCompare is True, the comparator will be called at most twice,
	// once to compare with prevElement and once to compare with nextElement.
	// When bUnique is true, if the return value is -1, then
	// get_duplicate_element reutrns the node of the already existing element.
	public int addElementAtPosition(int prevNode, int nextNode, int element,
			boolean bUnique, boolean bCallCompare, int treap) {
		int treap_ = treap;
		if (treap_ == -1) {
			if (m_defaultTreap == nullNode())
				m_defaultTreap = createTreap(-1);
			treap_ = m_defaultTreap;
		}

		// dbg_check_(m_root);
		if (getRoot_(treap_) == nullNode()) {
			assert (nextNode == nullNode() && prevNode == nullNode());
			int root = newNode_(element);
			setRoot_(root, treap_);
			addToList_(-1, root, treap_);
			return root;
		}

		int cmpNext;
		int cmpPrev;
		if (bCallCompare) {
			cmpNext = nextNode != nullNode() ? m_comparator.compare(this,
					element, nextNode) : -1;
			assert (cmpNext <= 0);
			cmpPrev = prevNode != nullNode() ? m_comparator.compare(this,
					element, prevNode) : 1;
			// cmpPrev can be negative in plane sweep when intersection is
			// detected.
		} else {
			cmpNext = -1;
			cmpPrev = 1;
		}

		if (bUnique && (cmpNext == 0 || cmpPrev == 0)) {
			m_comparator.onAddUniqueElementFailedImpl_(element);
			int cur = cmpNext == 0 ? nextNode : prevNode;
			setDuplicateElement_(cur, treap_);
			return -1;// return negative value.
		}

		int cur;
		int cmp;
		boolean bNext;
		if (nextNode != nullNode() && prevNode != nullNode()) {
			// randomize the the cost to insert a node.
			bNext = m_random > NumberUtils.nextRand(m_random) >> 1;
		} else
			bNext = nextNode != nullNode();

		if (bNext) {
			cmp = cmpNext;
			cur = nextNode;
		} else {
			cmp = cmpPrev;
			cur = prevNode;
		}

		int newNode = -1;
		int before = -1;
		boolean b_first = true;
		for (;;) {
			if (cmp < 0) {
				int left = getLeft(cur);
				if (left != nullNode())
					cur = left;
				else {
					before = cur;
					newNode = newNode_(element);
					setLeft_(cur, newNode);
					setParent_(newNode, cur);
					break;
				}
			} else {
				int right = getRight(cur);
				if (right != nullNode())
					cur = right;
				else {
					before = getNext(cur);
					newNode = newNode_(element);
					setRight_(cur, newNode);
					setParent_(newNode, cur);
					break;
				}
			}

			if (b_first) {
				cmp *= -1;
				b_first = false;
			}
		}

		bubbleUp_(newNode);
		if (getParent(newNode) == nullNode())
			setRoot_(newNode, treap_);

		addToList_(before, newNode, treap_);
		// dbg_check_(m_root);
		return newNode;
	}

	// Get duplicate element
	public int getDuplicateElement(int treap) {
		if (treap == -1)
			return getDuplicateElement_(m_defaultTreap);

		return getDuplicateElement_(treap);
	}

	// Removes a node from the treap. Throws if doesn't exist.
	public void deleteNode(int treap_node_index, int treap) {
		touch_();
		// assert(isValidNode(treap_node_index));
		if (m_comparator != null)
			m_comparator.onDeleteImpl_(this, treap_node_index);

		int treap_;
		if (treap == -1)
			treap_ = m_defaultTreap;
		else
			treap_ = treap;

		if (!m_b_balancing) {
			unbalancedDelete_(treap_node_index, treap_);
		} else
			deleteNode_(treap_node_index, treap_);
	}

	// Finds an element in the treap and returns its node or -1.
	public int search(int data, int treap) {
		int cur = getRoot(treap);
		while (cur != nullNode()) {
			int res = m_comparator.compare(this, data, cur);
			if (res == 0)
				return cur;
			else if (res < 0)
				cur = getLeft(cur);
			else
				cur = getRight(cur);
		}

		m_comparator.onEndSearchImpl_(data);
		return nullNode();
	}

	// Find a first node in the treap which is less or equal the moniker.
	// Returns closest smaller (Comparator::compare returns -1) or any equal.
	public int searchLowerBound(MonikerComparator moniker, int treap) {
		int cur = getRoot(treap);
		int bound = -1;
		while (cur != nullNode()) {
			int res = moniker.compare(this, cur);
			if (res == 0)
				return cur;
			else if (res < 0)
				cur = getLeft(cur);
			else {
				bound = cur;
				cur = getRight(cur);
			}
		}

		return bound;
	}

	// Find a first node in the treap which is greater or equal the moniker.
	// Returns closest greater (Comparator::compare returns 1) or any equal.
	public int searchUpperBound(MonikerComparator moniker, int treap) {
		int cur = getRoot(treap);
		int bound = -1;
		while (cur != nullNode()) {
			int res = moniker.compare(this, cur);
			if (res == 0)
				return cur;
			else if (res < 0) {
				bound = cur;
				cur = getLeft(cur);
			} else {
				cur = getRight(cur);
			}
		}

		return bound;
	}

	// Returns treap node data (element) from the given node index.
	public int getElement(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 3);// no error checking
															// here
	}

	// Returns treap node for the left node for the given treap node index
	public int getLeft(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 0);// no error checking
															// here
	}

	// Returns treap index for the right node for the given treap node index
	public int getRight(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 1);// no error checking
															// here
	}

	// Returns treap index for the parent node for the given treap node index
	public int getParent(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 2);// no error checking
															// here
	}

	// Returns next treap index. Allows to navigate Treap in the sorted order
	public int getNext(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 6);
	}

	// Returns prev treap index. Allows to navigate Treap in the sorted order
	// backwards
	public int getPrev(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 5);
	}

	// Returns the first element in the treap (least one). Used together with
	// get_next to write a loop
	public int getFirst(int treap) {
		if (treap == -1)
			return getFirst_(m_defaultTreap);

		return getFirst_(treap);
	}

	// Returns the last element in the treap (greatest one). Used together with
	// get_prev to write a loop
	public int getLast(int treap) {
		if (treap == -1)
			return getLast_(m_defaultTreap);

		return getLast_(treap);
	}

	// Gets the treap data associated with the treap.
	public int getTreapData(int treap) {
		if (treap == -1)
			return getTreapData_(m_defaultTreap);

		return getTreapData_(treap);
	}

	// Change the element value. Note: do not call this method if setting the
	// element will change the sorted order.
	public void setElement(int treap_node_index, int newElement) {
		if (m_comparator != null)
			m_comparator.onSetImpl_(this, treap_node_index);
		setElement_(treap_node_index, newElement);
	}

	// Returns the root of the treap.
	public int getRoot(int treap) {
		if (treap == -1)
			return getRoot_(m_defaultTreap);

		return getRoot_(treap);
	}

	// Check if the node is Null (does not exist).
	public static int nullNode() {
		return -1;
	}

	// Clears all nodes
	public void clear() {
		m_treapData.deleteAll(false);
		m_defaultTreap = nullNode();
	}

	// Total number of nodes
	public int size(int treap) {
		if (treap == -1)
			return getSize_(m_defaultTreap);

		return getSize_(treap);
	}

	// Returns the maximum depth of this Treap at given moment
	public int getMaxDepth(int treap) {
		return getMaxDepthHelper_(getRoot(treap));
	}

	public int getStateFlag() {
		m_touchFlag &= 0x7FFFFFFF;
		return m_touchFlag;
	}

	private int m_defaultTreap;
	private int m_random;
	private Treap.Comparator m_comparator;// comparator used to arrange the
											// nodes
	private StridedIndexTypeCollection m_treapData; // m_left (0), m_right (1),
													// m_parent (2), m_element
													// (3), m_priority (4),
													// m_prev (5), m_next (6)
													// (optional: m_root (0),
													// m_first (1), m_last (2),
													// m_duplicate_element (3),
													// m_treap_size (4),
													// m_treapData (5))
	private int m_touchFlag;
	private boolean m_b_balancing;

	private void touch_() {
		if (m_touchFlag >= 0) {
			m_touchFlag += 0x80000001;
		}
	}

	private int getPriority_(int treap_node_index) {
		return m_treapData.getField(treap_node_index, 4);// no error checking
															// here
	}

	private void bubbleDown_(int treap_node_index) {
		int left = getLeft(treap_node_index);
		int right = getRight(treap_node_index);
		int priority = getPriority_(treap_node_index);
		while (left != nullNode() || right != nullNode()) {
			int lcprior = left != nullNode() ? getPriority_(left) : NumberUtils
					.intMax();
			int rcprior = right != nullNode() ? getPriority_(right)
					: NumberUtils.intMax();
			int minprior = Math.min(lcprior, rcprior);

			if (priority <= minprior)
				return;

			if (lcprior <= rcprior)
				rotateRight_(left);
			else
				rotateLeft_(treap_node_index);

			left = getLeft(treap_node_index);
			right = getRight(treap_node_index);
		}
	}

	private void bubbleUp_(int node) {
		if (!m_b_balancing)
			return;
		int priority = getPriority_(node);
		int parent = getParent(node);
		while (parent != nullNode() && getPriority_(parent) > priority) {
			if (getLeft(parent) == node)
				rotateRight_(node);
			else
				rotateLeft_(parent);

			parent = getParent(node);
		}
	}

	private void rotateLeft_(int treap_node_index) {
		int px = treap_node_index;
		int py = getRight(treap_node_index);
		int ptemp;
		setParent_(py, getParent(px));
		setParent_(px, py);

		ptemp = getLeft(py);
		setRight_(px, ptemp);

		if (ptemp != nullNode())
			setParent_(ptemp, px);

		setLeft_(py, px);

		ptemp = getParent(py);
		if (ptemp != nullNode()) {
			if (getLeft(ptemp) == px)
				setLeft_(ptemp, py);
			else {
				assert (getRight(ptemp) == px);
				setRight_(ptemp, py);
			}
		}
	}

	private void rotateRight_(int treap_node_index) {
		int py = getParent(treap_node_index);
		int px = treap_node_index;
		int ptemp;

		setParent_(px, getParent(py));
		setParent_(py, px);

		ptemp = getRight(px);
		setLeft_(py, ptemp);

		if (ptemp != nullNode())
			setParent_(ptemp, py);

		setRight_(px, py);

		ptemp = getParent(px);
		if (ptemp != nullNode()) {
			if (getLeft(ptemp) == py)
				setLeft_(ptemp, px);
			else {
				assert (getRight(ptemp) == py);
				setRight_(ptemp, px);
			}
		}
	}

	private void setParent_(int treap_node_index, int new_parent) {
		m_treapData.setField(treap_node_index, 2, new_parent); // no error
																// checking here
	}

	private void setLeft_(int treap_node_index, int new_left) {
		m_treapData.setField(treap_node_index, 0, new_left); // no error
																// checking here
	}

	private void setRight_(int treap_node_index, int new_right) {
		m_treapData.setField(treap_node_index, 1, new_right); // no error
																// checking here
	}

	private void setPriority_(int treap_node_index, int new_priority) {
		m_treapData.setField(treap_node_index, 4, new_priority); // no error
																	// checking
																	// here
	}

	private void setPrev_(int treap_node_index, int prev) {
		assert (prev != treap_node_index);
		m_treapData.setField(treap_node_index, 5, prev); // no error checking
															// here
	}

	private void setNext_(int treap_node_index, int next) {
		assert (next != treap_node_index);
		m_treapData.setField(treap_node_index, 6, next); // no error checking
															// here
	}

	private void setRoot_(int root, int treap) {
		m_treapData.setField(treap, 0, root);
	}

	private void setFirst_(int first, int treap) {
		m_treapData.setField(treap, 1, first);
	}

	private void setLast_(int last, int treap) {
		m_treapData.setField(treap, 2, last);
	}

	private void setDuplicateElement_(int duplicate_element, int treap) {
		m_treapData.setField(treap, 3, duplicate_element);
	}

	private void setSize_(int size, int treap) {
		m_treapData.setField(treap, 4, size);
	}

	private void setTreapData_(int treap_data, int treap) {
		m_treapData.setField(treap, 5, treap_data);
	}

	private int getRoot_(int treap) {
		if (treap == -1)
			return nullNode();

		return m_treapData.getField(treap, 0);
	}

	private int getFirst_(int treap) {
		if (treap == -1)
			return nullNode();

		return m_treapData.getField(treap, 1);
	}

	private int getLast_(int treap) {
		if (treap == -1)
			return nullNode();

		return m_treapData.getField(treap, 2);
	}

	private int getDuplicateElement_(int treap) {
		if (treap == -1)
			return nullNode();

		return m_treapData.getField(treap, 3);
	}

	private int getSize_(int treap) {
		if (treap == -1)
			return 0;

		return m_treapData.getField(treap, 4);
	}

	private int getTreapData_(int treap) {
		return m_treapData.getField(treap, 5);
	}

	private int newNode_(int element) {
		touch_();
		int newNode = m_treapData.newElement();
		setPriority_(newNode, generatePriority_());
		setElement_(newNode, element);
		return newNode;
	}

	private void freeNode_(int treap_node_index, int treap) {
		if (treap_node_index == nullNode())
			return;

		m_treapData.deleteElement(treap_node_index);
	}

	private int generatePriority_() {
		m_random = NumberUtils.nextRand(m_random);
		return m_random & (NumberUtils.intMax() >> 1);
	}

	private int getMaxDepthHelper_(int node) {
		if (node == nullNode())
			return 0;

		return 1 + Math.max(getMaxDepthHelper_(getLeft(node)),
				getMaxDepthHelper_(getRight(node)));
	}

	private int addElement_(int element, int kind, int treap) {
		// dbg_check_(m_root);
		if (getRoot_(treap) == nullNode()) {
			int newNode = newNode_(element);
			setRoot_(newNode, treap);
			addToList_(-1, newNode, treap);
			return newNode;
		}

		int cur = getRoot_(treap);
		int newNode = -1;
		int before = -1;

		for (;;) {
			int cmp = kind == -1 ? 1 : m_comparator.compare(this, element, cur);
			if (cmp < 0) {
				int left = getLeft(cur);
				if (left != nullNode())
					cur = left;
				else {
					before = cur;
					newNode = newNode_(element);
					setLeft_(cur, newNode);
					setParent_(newNode, cur);
					break;
				}
			} else {
				if (kind == 1 && cmp == 0) {
					m_comparator.onAddUniqueElementFailedImpl_(element);
					setDuplicateElement_(cur, treap);
					return -1;// return negative value.
				}

				int right = getRight(cur);
				if (right != nullNode())
					cur = right;
				else {
					before = getNext(cur);
					newNode = newNode_(element);
					setRight_(cur, newNode);
					setParent_(newNode, cur);
					break;
				}
			}
		}

		bubbleUp_(newNode);
		if (getParent(newNode) == nullNode())
			setRoot_(newNode, treap);

		addToList_(before, newNode, treap);
		// dbg_check_(m_root);
		return newNode;
	}

	private void addToList_(int before, int node, int treap) {
		assert (before != node);
		int prev;
		if (before != -1) {
			prev = getPrev(before);
			setPrev_(before, node);
		} else
			prev = getLast_(treap);

		setPrev_(node, prev);
		if (prev != -1)
			setNext_(prev, node);
		setNext_(node, before);

		if (before == getFirst_(treap)) {
			setFirst_(node, treap);
		}
		if (before == -1) {
			setLast_(node, treap);
		}

		setSize_(getSize_(treap) + 1, treap);
	}

	private void removeFromList_(int node, int treap) {
		int prev = getPrev(node);
		int next = getNext(node);
		if (prev != -1)
			setNext_(prev, next);
		else
			setFirst_(next, treap);

		if (next != -1)
			setPrev_(next, prev);
		else
			setLast_(prev, treap);

		setSize_(getSize_(treap) - 1, treap);
	}

	private void unbalancedDelete_(int treap_node_index, int treap) {
		assert (!m_b_balancing);
		// dbg_check_(m_root);
		removeFromList_(treap_node_index, treap);
		int left = getLeft(treap_node_index);
		int right = getRight(treap_node_index);
		int parent = getParent(treap_node_index);
		int x = treap_node_index;
		if (left != -1 && right != -1) {
			m_random = NumberUtils.nextRand(m_random);
			int R;
			if (m_random > (NumberUtils.intMax() >> 1))
				R = getNext(treap_node_index);
			else
				R = getPrev(treap_node_index);

			assert (R != -1);// cannot be NULL becaus the node has left and
								// right

			boolean bFixMe = getParent(R) == treap_node_index;

			// swap left, right, and parent
			m_treapData.swapField(treap_node_index, R, 0);
			m_treapData.swapField(treap_node_index, R, 1);
			m_treapData.swapField(treap_node_index, R, 2);

			if (parent != -1) {
				// Connect ex-parent of int to R.
				if (getLeft(parent) == treap_node_index) {
					setLeft_(parent, R);
				} else {
					assert (getRight(parent) == treap_node_index);
					setRight_(parent, R);
				}
			} else {// int was the root. Make R the Root.
				setRoot_(R, treap);
			}

			if (bFixMe) {// R was a child of int
				if (left == R) {
					setLeft_(R, treap_node_index);
					setParent_(right, R);
				} else if (right == R) {
					setRight_(R, treap_node_index);
					setParent_(left, R);
				}

				setParent_(treap_node_index, R);
				parent = R;
			} else {
				setParent_(left, R);
				setParent_(right, R);
				parent = getParent(treap_node_index);
				x = R;
			}

			assert (parent != -1);
			left = getLeft(treap_node_index);
			right = getRight(treap_node_index);
			if (left != -1)
				setParent_(left, treap_node_index);
			if (right != -1)
				setParent_(right, treap_node_index);

			assert (left == -1 || right == -1);
		}

		// At most one child is not NULL.
		int child = left != -1 ? left : right;

		if (parent == -1) {
			setRoot_(child, treap);
		} else {
			if (getLeft(parent) == x) {
				setLeft_(parent, child);
			} else {
				assert (getRight(parent) == x);
				setRight_(parent, child);
			}
		}

		if (child != -1)
			setParent_(child, parent);

		freeNode_(treap_node_index, treap);
		// dbg_check_(m_root);
	}

	private void deleteNode_(int treap_node_index, int treap) {
		assert (m_b_balancing);
		setPriority_(treap_node_index, NumberUtils.intMax()); // set the node
																// priority high
		int prl = nullNode();
		int prr = nullNode();
		int root = getRoot_(treap);
		boolean isroot = (root == treap_node_index);

		if (isroot) {
			// remember children of the root node, if the root node is to be
			// deleted
			prl = getLeft(root);
			prr = getRight(root);

			if (prl == nullNode() && prr == nullNode()) {
				removeFromList_(root, treap);
				freeNode_(root, treap);
				setRoot_(nullNode(), treap);
				return;
			}
		}

		bubbleDown_(treap_node_index); // let the node to slide to the leaves of
										// tree

		int p = getParent(treap_node_index);

		if (p != nullNode()) {
			if (getLeft(p) == treap_node_index)
				setLeft_(p, nullNode());
			else
				setRight_(p, nullNode());
		}

		removeFromList_(treap_node_index, treap);
		freeNode_(treap_node_index, treap);

		if (isroot) // if the root node is deleted, assign new root
			setRoot_((prl == nullNode() || getParent(prl) != nullNode()) ? prr
					: prl, treap);

		assert (getParent(getRoot(treap)) == nullNode());
	}

	private void setElement_(int treap_node_index, int newElement) {
		touch_();
		m_treapData.setField(treap_node_index, 3, newElement);// no error
																// checking here
	}
}
