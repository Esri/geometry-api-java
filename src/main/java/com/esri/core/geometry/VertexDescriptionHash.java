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

import com.esri.core.geometry.VertexDescription.Semantics;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A hash object singleton that stores all VertexDescription instances via
 * WeakReference. The purpose of the class is to keep track of created
 * VertexDescription instances to prevent duplicates.
 */
final class VertexDescriptionHash {
	HashMap<Integer, VertexDescription> m_map = new HashMap<Integer, VertexDescription>();

	private static VertexDescription m_vd2D = new VertexDescription(1);
	private static VertexDescription m_vd3D = new VertexDescription(3);

	private static final VertexDescriptionHash INSTANCE = new VertexDescriptionHash();

	private VertexDescriptionHash() {
		m_map.put(1, m_vd2D);
		m_map.put(3, m_vd3D);
	}

	public static VertexDescriptionHash getInstance() {
		return INSTANCE;
	}

	public final VertexDescription getVD2D() {
		return m_vd2D;
	}

	public final VertexDescription getVD3D() {
		return m_vd3D;
	}

	public final VertexDescription FindOrAdd(int bitSet) {
		if (bitSet == 1)
			return m_vd2D;
		if (bitSet == 3)
			return m_vd3D;

		synchronized (this) {
			VertexDescription vd = m_map.get(bitSet);
			if (vd == null) {
				vd = new VertexDescription(bitSet);
				m_map.put(bitSet, vd);
			}

			return vd;
		}
	}

}
