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

class IndexMultiList {

	StridedIndexTypeCollection m_listNodes; // stores lists and list elements.
											// Each list element is Index, next.
	StridedIndexTypeCollection m_lists; // stores lists. Each list is Head,
										// Tail, [PrevList, NextList].
	int m_list_of_lists;
	boolean m_b_allow_navigation_between_lists;// when False, get_first_list,
												// get_next_list return -1.

	void freeNode_(int node) {
		m_listNodes.deleteElement(node);
	}

	int newNode_() {
		int node = m_listNodes.newElement();
		return node;
	}

	void freeList_(int list) {
		m_lists.deleteElement(list);
	}

	int newList_() {
		int list = m_lists.newElement();
		return list;
	}

	// Same as Index_multi_list(true);
	IndexMultiList() {
		m_listNodes = new StridedIndexTypeCollection(2);
		m_lists = new StridedIndexTypeCollection(4);
		m_list_of_lists = nullNode();
		m_b_allow_navigation_between_lists = true;
	}

	// When b_allow_navigation_between_lists is False, the get_first_list and
	// get_next_list do not work.
	// There will be two Index_type elements per list and two Index_type
	// elements per list element
	// When b_allow_navigation_between_lists is True, the get_first_list and
	// get_next_list will work.
	// There will be four Index_type elements per list and two Index_type
	// elements per list element
	IndexMultiList(boolean b_allow_navigation_between_lists) {
		m_listNodes = new StridedIndexTypeCollection(2);
		m_lists = new StridedIndexTypeCollection(
				b_allow_navigation_between_lists ? 4 : 2);
		m_list_of_lists = nullNode();
		m_b_allow_navigation_between_lists = b_allow_navigation_between_lists;
	}

	// Creates new list and returns it's handle.
	int createList() {
		int node = newList_();
		if (m_b_allow_navigation_between_lists) {
			m_lists.setField(node, 3, m_list_of_lists);
			if (m_list_of_lists != nullNode())
				m_lists.setField(m_list_of_lists, 2, node);
			m_list_of_lists = node;
		}

		return node;
	}

	// Deletes a list.
	void deleteList(int list) {
		int ptr = getFirst(list);
		while (ptr != nullNode()) {
			int p = ptr;
			ptr = getNext(ptr);
			freeNode_(p);
		}

		if (m_b_allow_navigation_between_lists) {
			int prevList = m_lists.getField(list, 2);
			int nextList = m_lists.getField(list, 3);
			if (prevList != nullNode())
				m_lists.setField(prevList, 3, nextList);
			else
				m_list_of_lists = nextList;

			if (nextList != nullNode())
				m_lists.setField(nextList, 2, prevList);
		}

		freeList_(list);
	}

	// Reserves memory for the given number of lists.
	void reserveLists(int listCount) {
		m_lists.setCapacity(listCount);
	}

	// Adds element to a given list. The element is added to the end. Returns
	// the new
	int addElement(int list, int element) {
		int head = m_lists.getField(list, 0);
		int tail = m_lists.getField(list, 1);
		int node = newNode_();
		if (tail != nullNode()) {
			assert (head != nullNode());
			m_listNodes.setField(tail, 1, node);
			m_lists.setField(list, 1, node);
		} else {// empty list
			assert (head == nullNode());
			m_lists.setField(list, 0, node);
			m_lists.setField(list, 1, node);
		}

		m_listNodes.setField(node, 0, element);
		return node;
	}

	// Reserves memory for the given number of nodes.
	void reserveNodes(int nodeCount) {
		m_listNodes.setCapacity(nodeCount);
	}

	// Deletes a node from a list, given the previous node (previous node is
	// required, because the list is singly connected).
	void deleteElement(int list, int prevNode, int node) {
		if (prevNode != nullNode()) {
			assert (m_listNodes.getField(prevNode, 1) == node);
			m_listNodes.setField(prevNode, 1, m_listNodes.getField(node, 1));
			if (m_lists.getField(list, 1) == node)// deleting a tail
			{
				m_lists.setField(list, 1, prevNode);
			}
		} else {
			assert (m_lists.getField(list, 0) == node);
			m_lists.setField(list, 0, m_listNodes.getField(node, 1));
			if (m_lists.getField(list, 1) == node) {// removing last element
				assert (m_listNodes.getField(node, 1) == nullNode());
				m_lists.setField(list, 1, nullNode());
			}
		}
		freeNode_(node);
	}

	// Concatenates list1 and list2. The nodes of list2 are added to the end of
	// list1. The list2 index becomes invalid.
	// Returns list1.
	int concatenateLists(int list1, int list2) {
		int tailNode1 = m_lists.getField(list1, 1);
		int headNode2 = m_lists.getField(list2, 0);
		if (headNode2 != nullNode())// do not concatenate empty lists
		{
			if (tailNode1 != nullNode()) {
				// connect head of list2 to the tail of list1.
				m_listNodes.setField(tailNode1, 1, headNode2);
				// set the tail of the list1 to be the tail of list2.
				m_lists.setField(list1, 1, m_lists.getField(list2, 1));
			} else {// list1 is empty, while list2 is not.
				m_lists.setField(list1, 0, headNode2);
				m_lists.setField(list1, 1, m_lists.getField(list2, 1));
			}
		}

		if (m_b_allow_navigation_between_lists) {
			int prevList = m_lists.getField(list2, 2);
			int nextList = m_lists.getField(list2, 3);
			if (prevList != nullNode())
				m_lists.setField(prevList, 3, nextList);
			else
				m_list_of_lists = nextList;

			if (nextList != nullNode())
				m_lists.setField(nextList, 2, prevList);
		}

		freeList_(list2);
		return list1;
	}

	// Returns the data from the given list node.
	int getElement(int node_index) {
		return m_listNodes.getField(node_index, 0);
	}

	// Sets the data to the given list node.
	void setElement(int node_index, int element) {
		m_listNodes.setField(node_index, 0, element);
	}

	// Returns index of next node for the give node.
	int getNext(int node_index) {
		return m_listNodes.getField(node_index, 1);
	}

	// Returns the first node in the least
	int getFirst(int list) {
		return m_lists.getField(list, 0);
	}

	// Returns the element from the first node in the least. Equivalent to
	// get_element(get_first(list));
	int getFirstElement(int list) {
		int f = getFirst(list);
		return getElement(f);
	}

	// Check if the node is Null (does not exist)
	static int nullNode() {
		return -1;
	}

	// Clears all nodes and removes all lists. Frees the memory.
	void clear() {
		m_listNodes.deleteAll(true);
		m_lists.deleteAll(true);
		m_list_of_lists = nullNode();
	}

	// Returns True if the given list is empty.
	boolean isEmpty(int list) {
		return m_lists.getField(list, 0) == nullNode();
	}

	boolean isEmpty() {
		return m_listNodes.size() == 0;
	}

	int getNodeCount() {
		return m_listNodes.size();
	}

	int getListCount() {
		return m_lists.size();
	}

	int getFirstList() {
		assert (m_b_allow_navigation_between_lists);
		return m_list_of_lists;
	}

	int getNextList(int list) {
		assert (m_b_allow_navigation_between_lists);
		return m_lists.getField(list, 3);
	}
}
