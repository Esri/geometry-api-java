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

import java.util.Comparator;

class PolylinePath {
	Point2D m_fromPoint;
	Point2D m_toPoint;
	double m_fromDist; // from lower left corner; -1.0 if point is not on
						// clipping bounday
	double m_toDist; // from lower left corner; -1.0 if point is not on clipping
						// bounday
	int m_path; // from polyline
	boolean m_used;

	public PolylinePath() {
	}

	public PolylinePath(Point2D fromPoint, Point2D toPoint, double fromDist,
			double toDist, int path) {
		m_fromPoint = fromPoint;
		m_toPoint = toPoint;
		m_fromDist = fromDist;
		m_toDist = toDist;
		m_path = path;
		m_used = false;
	}

	void setValues(Point2D fromPoint, Point2D toPoint, double fromDist,
			double toDist, int path) {
		m_fromPoint = fromPoint;
		m_toPoint = toPoint;
		m_fromDist = fromDist;
		m_toDist = toDist;
		m_path = path;
		m_used = false;
	}

	// to be used in Use SORTARRAY

}

class PolylinePathComparator implements Comparator<PolylinePath> {
	@Override
	public int compare(PolylinePath v1, PolylinePath v2) {
		if ((v1).m_fromDist < (v2).m_fromDist)
			return -1;
		else if ((v1).m_fromDist > (v2).m_fromDist)
			return 1;
		else
			return 0;
	}

}
