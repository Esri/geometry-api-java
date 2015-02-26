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

class IndexMultiDCList {

	StridedIndexTypeCollection m_list_nodes; // stores lists and list elements.
												// Each list element is Index,
												// Prev, next.
	StridedIndexTypeCollection m_lists; // stores lists. Each list is Head,
										// Tail, PrevList, NextList, NodeCount,
										// ListData.
	int m_list_of_lists;
	boolean m_b_store_list_index_with_node;

	void freeNode_(int node) {
		m_list_nodes.deleteElement(node);
	}

	int newNode_() {
		int node = m_list_nodes.newElement();
		return node;
	}

	void freeList_(int list) {
		m_lists.deleteElement(list);
	}

	int newList_() {
		int list = m_lists.newElement();
		return list;
	}

	void setPrev_(int node, int prev) {
		m_list_nodes.setField(node, 1, prev);
	}

	void setNext_(int node, int next) {
		m_list_nodes.setField(node, 2, next);
	}

	void setData_(int node, int data) {
		m_list_nodes.setField(node, 0, data);
	}

	void setList_(int node, int list) {
		m_list_nodes.setField(node, 3, list);
	}

	void setListSize_(int list, int newsize) {
		m_lists.setField(list, 4, newsize);
	}

	void setNextList_(int list, int next) {
		m_lists.setField(list, 3, next);
	}

	void setPrevList_(int list, int prev) {
		m_lists.setField(list, 2, prev);
	}

	// Same as Index_multi_dc_list(true).
	IndexMultiDCList() {
		m_list_nodes = new StridedIndexTypeCollection(3);
		m_lists = new StridedIndexTypeCollection(6);
		m_b_store_list_index_with_node = false;
		m_list_of_lists = nullNode();
	}

	// When bStoreListIndexWithNode is true, the each node stores a pointer to
	// the list. Otherwise it does not.
	// The get_list() method cannot be used if bStoreListIndexWithNode is false.
	IndexMultiDCList(boolean b_store_list_index_with_node) {
		m_list_nodes = new StridedIndexTypeCollection(3);
		m_lists = new StridedIndexTypeCollection(6);
		m_b_store_list_index_with_node = false;
		m_list_of_lists = nullNode();
	}

	// Creates new list and returns it's handle.
	// listData is user's info associated with the list
	int createList(int listData) {
		int list = newList_();
		// m_lists.set_field(list, 0, null_node());//head
		// m_lists.set_field(list, 1, null_node());//tail
		// m_lists.set_field(list, 2, null_node());//prev list
		m_lists.setField(list, 3, m_list_of_lists); // next list
		m_lists.setField(list, 4, 0);// node count in the list
		m_lists.setField(list, 5, listData);
		if (m_list_of_lists != nullNode())
			setPrevList_(m_list_of_lists, list);

		m_list_of_lists = list;
		return list;
	}

	// Deletes a list and returns the index of the next list.
	int deleteList(int list) {
		clear(list);
		int prevList = m_lists.getField(list, 2);
		int nextList = m_lists.getField(list, 3);
		if (prevList != nullNode())
			setNextList_(prevList, nextList);
		else
			m_list_of_lists = nextList;

		if (nextList != nullNode())
			setPrevList_(nextList, prevList);

		freeList_(list);
		return nextList;
	}

	// Reserves memory for the given number of lists.
	void reserveLists(int listCount) {
		m_lists.setCapacity(listCount);
	}

	// returns user's data associated with the list
	int getListData(int list) {
		return m_lists.getField(list, 5);
	}

	// returns the list associated with the node_index. Do not use if list is
	// created with bStoreListIndexWithNode == false.
	int getList(int node_index) {
		assert (m_b_store_list_index_with_node);
		return m_list_nodes.getField(node_index, 3);
	}

	// sets the user data to the list
	void setListData(int list, int data) {
		m_lists.setField(list, 5, data);
	}

	// Adds element to a given list. The element is added to the end. Returns
	// the new
	int addElement(int list, int data) {
		return insertElement(list, -1, data);
	}

	// Inserts a new node before the given one .
	int insertElement(int list, int beforeNode, int data) {
		int node = newNode_();
		int prev = -1;
		if (beforeNode != nullNode()) {
			prev = getPrev(beforeNode);
			setPrev_(beforeNode, node);
		}

		setNext_(node, beforeNode);
		if (prev != nullNode())
			setNext_(prev, node);

		int head = m_lists.getField(list, 0);

		if (beforeNode == head)
			m_lists.setField(list, 0, node);
		if (beforeNode == nullNode()) {
			int tail = m_lists.getField(list, 1);
			setPrev_(node, tail);
			if (tail != -1)
				setNext_(tail, node);

			m_lists.setField(list, 1, node);
		}

		setData(node, data);
		setListSize_(list, getListSize(list) + 1);

		if (m_b_store_list_index_with_node)
			setList_(node, list);

		return node;
	}

	// Deletes a node from a list. Returns the next node after the deleted one.
	int deleteElement(int list, int node) {
		int prev = getPrev(node);
		int next = getNext(node);
		if (prev != nullNode())
			setNext_(prev, next);
		else
			m_lists.setField(list, 0, next);// change head
		if (next != nullNode())
			setPrev_(next, prev);
		else
			m_lists.setField(list, 1, prev);// change tail

		freeNode_(node);
		setListSize_(list, getListSize(list) - 1);
		return next;
	}

	// Reserves memory for the given number of nodes.
	void reserveNodes(int nodeCount) {
		m_list_nodes.setCapacity(nodeCount);
	}

	// Returns the data from the given list node.
	int getData(int node_index) {
		return m_list_nodes.getField(node_index, 0);
	}

	// Sets the data to the given list node.
	void setData(int node_index, int element) {
		m_list_nodes.setField(node_index, 0, element);
	}

	// Returns index of next node for the give node.
	int getNext(int node_index) {
		return m_list_nodes.getField(node_index, 2);
	}

	// Returns index of previous node for the give node.
	int getPrev(int node_index) {
		return m_list_nodes.getField(node_index, 1);
	}

	// Returns the first node in the list
	int getFirst(int list) {
		return m_lists.getField(list, 0);
	}

	// Returns the last node in the list
	int getLast(int list) {
		return m_lists.getField(list, 1);
	}

	// Check if the node is Null (does not exist)
	static int nullNode() {
		return -1;
	}

	// Clears all nodes and removes all lists.
	void clear() {
		for (int list = getFirstList(); list != -1;) {
			list = deleteList(list);
		}
	}

	// Clears all nodes from the list.
	void clear(int list) {
		int last = getLast(list);
		while (last != nullNode()) {
			int n = last;
			last = getPrev(n);
			freeNode_(n);
		}
		m_lists.setField(list, 0, -1);
		m_lists.setField(list, 1, -1);
		setListSize_(list, 0);
	}

	// Returns True if the given list is empty.
	boolean isEmpty(int list) {
		return m_lists.getField(list, 0) == -1;
	}

	// Returns True if the multilist is empty
	boolean isEmpty() {
		return m_list_nodes.size() == 0;
	}

	// Returns node count in all lists
	int getNodeCount() {
		return m_list_nodes.size();
	}

	// returns the number of lists
	int getListCount() {
		return m_lists.size();
	}

	// Returns the node count in the given list
	int getListSize(int list) {
		return m_lists.getField(list, 4);
	}

	// returns the first list
	int getFirstList() {
		return m_list_of_lists;
	}

	// returns the next list
	int getNextList(int list) {
		return m_lists.getField(list, 3);
	}
}
