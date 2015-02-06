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

/**
 * A simple MapGeometryCursor implementation that wraps a single MapGeometry or
 * an array of MapGeometry classes
 */
class SimpleMapGeometryCursor extends MapGeometryCursor {

	MapGeometry m_geom;
	MapGeometry[] m_geomArray;

	int m_index;
	int m_count;

	public SimpleMapGeometryCursor(MapGeometry geom) {
		m_geom = geom;
		m_index = -1;
		m_count = 1;
	}

	public SimpleMapGeometryCursor(MapGeometry[] geoms) {
		m_geomArray = geoms;
		m_index = -1;
		m_count = geoms.length;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	@Override
	public MapGeometry next() {
		if (m_index < m_count - 1) {
			m_index++;
			return m_geom != null ? m_geom : m_geomArray[m_index];
		}

		return null;
	}
}
