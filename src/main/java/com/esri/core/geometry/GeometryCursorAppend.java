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

public class GeometryCursorAppend extends GeometryCursor {

	private GeometryCursor m_cur1;
	private GeometryCursor m_cur2;
	private GeometryCursor m_cur;

	public GeometryCursorAppend(GeometryCursor cur1, GeometryCursor cur2) {
		m_cur1 = cur1;
		m_cur2 = cur2;
		m_cur = m_cur1;
	}

	@Override
	public Geometry next() {
		Geometry g = m_cur.next();
		if (g == null && m_cur != m_cur2) {
			m_cur = m_cur2;
			return m_cur.next();
		}
		return g;
	}

	@Override
	public int getGeometryID() {
		return m_cur.getGeometryID();
	}
}
