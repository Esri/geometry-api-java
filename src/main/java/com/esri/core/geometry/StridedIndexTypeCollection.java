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

/**
 * A collection of strides of Index_type elements. To be used when one needs a
 * collection of homogeneous elements that contain only integer fields (i.e.
 * structs with Index_type members) Recycles the strides. Allows for constant
 * time creation and deletion of an element.
 */
final class StridedIndexTypeCollection {

	private int m_firstFree;
	private int m_last;
	private int m_stride;
	private int m_realStride;
	private int m_realBlockSize;
	private int m_blockSize;
	private int m_blockMask;
	private int m_blockPower;
	private int m_size;
	private int m_capacity;
	private ArrayList<int[]> m_buffer;

	StridedIndexTypeCollection(int stride) {
		m_firstFree = -1;
		m_last = 0;
		m_size = 0;
		m_stride = stride;
		m_realStride = stride;
		m_realBlockSize = 2048;//if you change this, change m_blockSize, m_blockPower, m_blockMask, and st_sizes
		m_blockPower = 11;
		m_blockMask = 0x7FF;
		m_blockSize = 2048 / m_realStride;
		m_capacity = 0;
		m_buffer = null;
	}

	void deleteElement(int element) {
		assert dbgdelete_(element);
		int totalStrides = (element >> m_blockPower) * m_blockSize
				* m_realStride + (element & m_blockMask);
		if (totalStrides < m_last * m_realStride) {
			m_buffer.get(element >> m_blockPower)[element & m_blockMask] = m_firstFree;
			m_firstFree = element;
		} else {
			assert (totalStrides == m_last * m_realStride);
			m_last--;
		}
		m_size--;
	}

	// Returns the given field of the element.
	int getField(int element, int field) {
		return m_buffer.get(element >> m_blockPower)[(element & m_blockMask)
				+ field];
	}

	// Sets the given field of the element.
	void setField(int element, int field, int value) {
		m_buffer.get(element >> m_blockPower)[(element & m_blockMask) + field] = value;
	}

	// Returns the stride size
	int getStride() {
		return m_stride;
	}

	// Creates the new element. This is a constant time operation.
	// All fields are initialized to -1.
	int newElement() {
		int element = m_firstFree;
		if (element == -1) {
			if (m_last == m_capacity) {
				long newcap = m_capacity != 0 ? (((long)m_capacity + 1) * 3 / 2) : (long)1;
				if (newcap > Integer.MAX_VALUE)
					newcap = Integer.MAX_VALUE;//cannot grow past 2gb elements presently
				
				if (newcap == m_capacity)
					throw new IndexOutOfBoundsException();
				
				grow_(newcap);
			}

			element = ((m_last / m_blockSize) << m_blockPower)
					+ (m_last % m_blockSize) * m_realStride;
			m_last++;
		} else {
			m_firstFree = m_buffer.get(element >> m_blockPower)[element
					& m_blockMask];
		}

		m_size++;
		int ar[] = m_buffer.get(element >> m_blockPower);
		int ind = element & m_blockMask;
		for (int i = 0; i < m_stride; i++) {
			ar[ind + i] = -1;
		}
		return element;
	}

	int elementToIndex(int element) {
		return (element >> m_blockPower) * m_blockSize
				+ (element & m_blockMask) / m_realStride;
	}

	// Deletes all elements and frees all the memory if b_free_memory is True.
	void deleteAll(boolean b_free_memory) {
		m_firstFree = -1;
		m_last = 0;
		m_size = 0;
		if (b_free_memory) {
			m_buffer = null;
			m_capacity = 0;
		}
	}

	// Returns the count of existing elements
	int size() {
		return m_size;
	}

	// Sets the capcity of the collection. Only applied if current capacity is
	// smaller.
	void setCapacity(int capacity) {
		if (capacity > m_capacity)
			grow_(capacity);
	}

	// Returns the capacity of the collection
	int capacity() {
		return m_capacity;
	}

	// Swaps content of two elements (each field of the stride)
	void swap(int element1, int element2) {
		int ar1[] = m_buffer.get(element1 >> m_blockPower);
		int ar2[] = m_buffer.get(element2 >> m_blockPower);
		int ind1 = element1 & m_blockMask;
		int ind2 = element2 & m_blockMask;
		for (int i = 0; i < m_stride; i++) {
			int tmp = ar1[ind1 + i];
			ar1[ind1 + i] = ar2[ind2 + i];
			ar2[ind2 + i] = tmp;
		}
	}

	// Swaps content of two fields
	void swapField(int element1, int element2, int field) {
		int ar1[] = m_buffer.get(element1 >> m_blockPower);
		int ar2[] = m_buffer.get(element2 >> m_blockPower);
		int ind1 = (element1 & m_blockMask) + field;
		int ind2 = (element2 & m_blockMask) + field;
		int tmp = ar1[ind1];
		ar1[ind1] = ar2[ind2];
		ar2[ind2] = tmp;
	}

	// Returns a value of the index, that never will be returned by new_element
	// and is neither -1 nor impossible_index_3.
	static int impossibleIndex2() {
		return -2;
	}

	// Returns a value of the index, that never will be returned by new_element
	// and is neither -1 nor impossible_index_2.
	static int impossibleIndex3() {
		return -3;
	}

	static boolean isValidElement(int element) {
		return element >= 0;
	}

	private boolean dbgdelete_(int element) {
		setField(element, 1, 0x7eadbeed);
		return true;
	}

	final static int[] st_sizes = {16, 32, 64, 128, 256, 512, 1024, 2048};

	private void grow_(long newsize) {
		if (m_buffer == null) {
			m_buffer = new ArrayList<int[]>();
		}

		assert (newsize > m_capacity);

		long nblocks = (newsize + m_blockSize - 1) / m_blockSize;
		if (nblocks > Integer.MAX_VALUE)
			throw new IndexOutOfBoundsException();
		
		m_buffer.ensureCapacity((int)nblocks);
		if (nblocks == 1) {
			// When less than one block is needed we allocate smaller arrays
			// than m_realBlockSize to avoid initialization cost.
			int oldsz = m_capacity > 0 ? m_capacity : 0;
			assert (oldsz < newsize);
			int i = 0;
			int realnewsize = (int)newsize * m_realStride;
			while (realnewsize > st_sizes[i])
				// get the size to allocate. Using fixed sizes to reduce
				// fragmentation.
				i++;
			int[] b = new int[st_sizes[i]];
			if (m_buffer.size() == 1) {
				System.arraycopy(m_buffer.get(0), 0, b, 0, m_size
						* m_realStride);
				m_buffer.set(0, b);
			} else {
				m_buffer.add(b);
			}
			m_capacity = b.length / m_realStride;
		} else {
			if (nblocks * m_blockSize > Integer.MAX_VALUE)
				throw new IndexOutOfBoundsException();
			
			if (m_buffer.size() == 1) {
				if (m_buffer.get(0).length < m_realBlockSize) {
					// resize the first buffer to ensure it is equal the
					// m_realBlockSize.
					int[] b = new int[m_realBlockSize];
					System.arraycopy(m_buffer.get(0), 0, b, 0, m_size
							* m_realStride);
					m_buffer.set(0, b);
					m_capacity = m_blockSize;
				}
			}

			while (m_buffer.size() < nblocks) {
				m_buffer.add(new int[m_realBlockSize]);
				m_capacity += m_blockSize;
			}
		}
	}

}
