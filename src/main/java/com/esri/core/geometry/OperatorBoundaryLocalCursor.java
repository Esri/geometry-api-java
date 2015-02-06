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

final class OperatorBoundaryLocalCursor extends GeometryCursor {
	ProgressTracker m_progress_tracker;
	boolean m_b_done;
	GeometryCursor m_inputGeometryCursor;
	int m_index;

	OperatorBoundaryLocalCursor(GeometryCursor inputGeoms,
			ProgressTracker tracker) {
		m_inputGeometryCursor = inputGeoms;
		m_progress_tracker = tracker;
		m_b_done = false;
		m_index = -1;
	}

	@Override
	public Geometry next() {
		if (!m_b_done) {
			Geometry geometry = m_inputGeometryCursor.next();
			if (geometry != null) {
				m_index = m_inputGeometryCursor.getGeometryID();
				return calculate_boundary(geometry, m_progress_tracker);
			}

			m_b_done = true;
		}

		return null;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	static Geometry calculate_boundary(Geometry geom,
			ProgressTracker progress_tracker) {
		Geometry res = Boundary.calculate(geom, progress_tracker);
		if (res == null)
			return new Point(geom.getDescription());// cannot return null
		else
			return res;
	}

}
