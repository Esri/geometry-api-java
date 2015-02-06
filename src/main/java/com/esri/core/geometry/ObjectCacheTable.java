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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ObjectCacheTable<K, T> {
	private Map<K, T> m_hashTable = Collections
			.synchronizedMap(new HashMap<K, T>());
	private Object[] m_lru;
	private boolean[] m_places;
	private int m_index;

	public ObjectCacheTable(int maxSize) {
		m_lru = new Object[maxSize];
		m_places = new boolean[maxSize];
		m_index = 0;
		for (int i = 0; i < maxSize; i++)
			m_places[i] = false;
	}

	boolean contains(K key) {
		return m_hashTable.containsKey(key);
	}

	T get(K key) {
		return m_hashTable.get(key);
	}

	void add(K key, T value) {
		if (m_places[m_index]) {// remove existing element from the cache
			m_places[m_index] = false;
			m_hashTable.remove(m_lru[m_index]);
		}

		m_hashTable.put(key, value);
		m_lru[m_index] = key;
		m_places[m_index] = true;
		m_index = (m_index + 1) % m_lru.length;
	}

}
