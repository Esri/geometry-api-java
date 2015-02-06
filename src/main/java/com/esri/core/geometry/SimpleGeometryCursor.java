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

import java.util.Arrays;
import java.util.List;

/**
 * A simple GeometryCursor implementation that wraps a single Geometry or
 * an array of Geometry classes
 */
public class SimpleGeometryCursor extends GeometryCursor {

	Geometry m_geom;
	List<Geometry> m_geomArray;

	int m_index;
	int m_count;

	public SimpleGeometryCursor(Geometry geom) {
		m_geom = geom;
		m_index = -1;
		m_count = 1;
	}

	public SimpleGeometryCursor(Geometry[] geoms) {
		m_geomArray = Arrays.asList(geoms);
		m_index = -1;
		m_count = geoms.length;
	}

	public SimpleGeometryCursor(List<Geometry> geoms) {
		m_geomArray = geoms;
		m_index = -1;
		m_count = geoms.size();
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	@Override
	public Geometry next() {
		if (m_index < m_count - 1) {
			m_index++;
			return m_geom != null ? m_geom : m_geomArray.get(m_index);
		}

		return null;
	}
}
