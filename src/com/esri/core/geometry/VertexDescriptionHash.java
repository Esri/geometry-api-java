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

import com.esri.core.geometry.VertexDescription.Semantics;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A hash object singleton that stores all VertexDescription instances via
 * WeakReference. The purpose of the class is to keep track of created
 * VertexDescription instances to prevent duplicates.
 */
final class VertexDescriptionHash {
	Map<Integer, WeakReference<VertexDescription>> map = new HashMap<Integer, WeakReference<VertexDescription>>();

	private static VertexDescription m_vd2D;

	private static VertexDescription m_vd3D;

	private static final VertexDescriptionHash INSTANCE = new VertexDescriptionHash();

	private VertexDescriptionHash() {
		VertexDescriptionDesignerImpl vdd2D = new VertexDescriptionDesignerImpl();
		add(vdd2D);
		VertexDescriptionDesignerImpl vdd3D = new VertexDescriptionDesignerImpl();
		vdd3D.addAttribute(Semantics.Z);
		add(vdd3D);
	}

	public static VertexDescriptionHash getInstance() {
		return INSTANCE;
	}

	public VertexDescription getVD2D() {
		return m_vd2D;
	}

	public VertexDescription getVD3D() {
		return m_vd3D;
	}

	synchronized public VertexDescription add(VertexDescriptionDesignerImpl vdd) {
		// Firstly quick test for 2D/3D descriptors.
		int h = vdd.hashCode();

		if ((m_vd2D != null) && m_vd2D.hashCode() == h) {
			if (vdd.isDesignerFor(m_vd2D))
				return m_vd2D;
		}

		if ((m_vd3D != null) && (m_vd3D.hashCode() == h)) {
			if (vdd.isDesignerFor(m_vd3D))
				return m_vd3D;
		}

		// Now search in the hash.

		VertexDescription vd = null;
		if (map.containsKey(h)) {
			WeakReference<VertexDescription> vdweak = map.get(h);
			vd = vdweak.get();
			if (vd == null) // GC'd VertexDescription
				map.remove(h);
		}

		if (vd == null) { // either not in map to begin with, or has been GC'd
			vd = vdd._createInternal();

			if (vd.getAttributeCount() == 1) {
				m_vd2D = vd;
			} else if ((vd.getAttributeCount() == 2)
					&& (vd.getSemantics(1) == Semantics.Z)) {
				m_vd3D = vd;
			} else {
				WeakReference<VertexDescription> vdweak = new WeakReference<VertexDescription>(
						vd);

				map.put(h, vdweak);
			}

		}

		return vd;
	}
}
