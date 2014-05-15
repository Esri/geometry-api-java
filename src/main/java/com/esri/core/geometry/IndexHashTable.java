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

class IndexHashTable {
	// The hash function abstract class that user need to define to use the
	// IndexHashTable.
	public static abstract class HashFunction {
		public abstract int getHash(int element);

		public abstract boolean equal(int element1, int element2);

		public abstract int getHash(Object elementDescriptor);

		public abstract boolean equal(Object elementDescriptor, int element);
	}

	int m_random;
	AttributeStreamOfInt32 m_hashBuckets;
	IndexMultiList m_lists;
	HashFunction m_hash;

	// Create hash table. size is the bin count in the table. The hashFunction
	// is the function to use.
	public IndexHashTable(int size, HashFunction hashFunction) {
		m_hashBuckets = new AttributeStreamOfInt32(size, nullNode());
		m_lists = new IndexMultiList();
		m_hash = hashFunction;
	}

	public void reserveElements(int capacity) {
		m_lists.reserveLists(Math.min(m_hashBuckets.size(), capacity));
		m_lists.reserveNodes(capacity);
	}

	// Adds new element to the hash table.
	public void addElement(int element) {
		int hash = m_hash.getHash(element);
		int bucket = hash % m_hashBuckets.size();
		int list = m_hashBuckets.get(bucket);
		if (list == IndexMultiList.nullNode()) {
			list = m_lists.createList();
			m_hashBuckets.set(bucket, list);
		}
		m_lists.addElement(list, element);
	}

	// Removes element from the hash table.
	public void deleteElement(int element) {
		int hash = m_hash.getHash(element);
		int bucket = hash % m_hashBuckets.size();
		int list = m_hashBuckets.get(bucket);
		if (list == IndexMultiList.nullNode())
			throw new IllegalArgumentException();

		int ptr = m_lists.getFirst(list);
		int prev = IndexMultiList.nullNode();
		while (ptr != IndexMultiList.nullNode()) {
			int e = m_lists.getElement(ptr);
			int nextptr = m_lists.getNext(ptr);
			if (e == element) {
				m_lists.deleteElement(list, prev, ptr);
				if (m_lists.getFirst(list) == IndexMultiList.nullNode()) {
					m_lists.deleteList(list);// do not keep empty lists
					m_hashBuckets.set(bucket, IndexMultiList.nullNode());
				}
			} else {
				prev = ptr;
			}
			ptr = nextptr;
		}

	}

	// Returns the first node in the hash table bucket defined by the given
	// hashValue.
	public int getFirstInBucket(int hashValue) {
		int bucket = hashValue % m_hashBuckets.size();
		int list = m_hashBuckets.get(bucket);
		if (list == IndexMultiList.nullNode())
			return IndexMultiList.nullNode();

		return m_lists.getFirst(list);

	}

	// Returns next node in a bucket. Can be used together with GetFirstInBucket
	// only.
	public int getNextInBucket(int elementHandle) {
		return m_lists.getNext(elementHandle);
	}

	// Returns a node of the first element in the hash table, that is equal to
	// the given one.
	public int findNode(int element) {
		int hash = m_hash.getHash(element);
		int bucket = hash % m_hashBuckets.size();
		int list = m_hashBuckets.get(bucket);
		if (list == IndexMultiList.nullNode())
			return IndexMultiList.nullNode();

		int ptr = m_lists.getFirst(list);
		while (ptr != IndexMultiList.nullNode()) {
			int e = m_lists.getElement(ptr);
			if (m_hash.equal(e, element)) {
				return ptr;
			}
			ptr = m_lists.getNext(ptr);
		}

		return IndexMultiList.nullNode();

	}

	// Returns a node to the first element in the hash table, that is equal to
	// the given element descriptor.
	public int findNode(Object elementDescriptor) {
		int hash = m_hash.getHash(elementDescriptor);
		int bucket = hash % m_hashBuckets.size();
		int list = m_hashBuckets.get(bucket);
		if (list == IndexMultiList.nullNode())
			return IndexMultiList.nullNode();

		int ptr = m_lists.getFirst(list);
		while (ptr != IndexMultiList.nullNode()) {
			int e = m_lists.getElement(ptr);
			if (m_hash.equal(elementDescriptor, e)) {
				return ptr;
			}
			ptr = m_lists.getNext(ptr);
		}

		return IndexMultiList.nullNode();

	}

	// Gets next equal node.
	public int getNextNode(int elementHandle) {
		int element = m_lists.getElement(elementHandle);
		int ptr = m_lists.getNext(elementHandle);
		while (ptr != IndexMultiList.nullNode()) {
			int e = m_lists.getElement(ptr);
			if (m_hash.equal(e, element)) {
				return ptr;
			}
			ptr = m_lists.getNext(ptr);
		}

		return IndexMultiList.nullNode();

	}

	// Removes a node.
	public void deleteNode(int node) {
		int element = getElement(node);
		int hash = m_hash.getHash(element);
		int bucket = hash % m_hashBuckets.size();
		int list = m_hashBuckets.get(bucket);
		if (list == IndexMultiList.nullNode())
			throw new IllegalArgumentException();

		int ptr = m_lists.getFirst(list);
		int prev = IndexMultiList.nullNode();
		while (ptr != IndexMultiList.nullNode()) {
			if (ptr == node) {
				m_lists.deleteElement(list, prev, ptr);
				if (m_lists.getFirst(list) == IndexMultiList.nullNode()) {
					m_lists.deleteList(list);// do not keep empty lists
					m_hashBuckets.set(bucket, IndexMultiList.nullNode());
				}
				return;
			}
			prev = ptr;
			ptr = m_lists.getNext(ptr);
		}

		throw new IllegalArgumentException();

	}

	// Returns a value of the element stored in the given node.
	public int getElement(int elementHandle) {
		return m_lists.getElement(elementHandle);
	}

	// Returns any existing element from the hash table. Throws if the table is
	// empty.
	public int getAnyElement() {
		return m_lists.getFirstElement(m_lists.getFirstList());
	}

	// Returns a node for any existing element from the hash table or NullNode
	// if the table is empty.
	public int getAnyNode() {
		return m_lists.getFirst(m_lists.getFirstList());
	}

	public static int nullNode() {
		return IndexMultiList.nullNode();
	}

	// Removes all elements from the hash table.
	public void clear() {
		m_hashBuckets = new AttributeStreamOfInt32(m_hashBuckets.size(),
				nullNode());
		m_lists.clear();

	}

	// Returns the number of elements in the hash table
	public int size() {
		return m_lists.getNodeCount();
	}
}
