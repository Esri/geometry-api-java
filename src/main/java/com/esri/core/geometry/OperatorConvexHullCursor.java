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

class OperatorConvexHullCursor extends GeometryCursor {
	private ProgressTracker m_progress_tracker;
	private boolean m_b_merge;
	private boolean m_b_done;
	private GeometryCursor m_inputGeometryCursor;
	private int m_index;
	ConvexHull m_hull = new ConvexHull();
	
	OperatorConvexHullCursor(boolean b_merge, GeometryCursor geoms,
			ProgressTracker progress_tracker) {
		m_index = -1;
		if (geoms == null)
			throw new IllegalArgumentException();

		m_b_merge = b_merge;
		m_b_done = false;
		m_inputGeometryCursor = geoms;
		m_progress_tracker = progress_tracker;
	}

	@Override
	public Geometry next() {
		if (m_b_merge) {
			if (!m_b_done) {
				Geometry result = calculateConvexHullMerging_(
						m_inputGeometryCursor, m_progress_tracker);
				m_b_done = true;
				return result;
			}

			return null;
		}

		if (!m_b_done) {
			Geometry geometry = m_inputGeometryCursor.next();
			if (geometry != null) {
				m_index = m_inputGeometryCursor.getGeometryID();
				return calculateConvexHull_(geometry, m_progress_tracker);
			}

			m_b_done = true;
		}

		return null;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	private Geometry calculateConvexHullMerging_(GeometryCursor geoms,
			ProgressTracker progress_tracker) {
		Geometry geometry;

		while ((geometry = geoms.next()) != null)
			m_hull.addGeometry(geometry);

		return m_hull.getBoundingGeometry();
	}
	
	@Override
	public boolean tock() {
		if (m_b_done)
			return true;
		
		if (!m_b_merge)
		{
			//Do not use tick/tock with the non-merging convex hull.
			//Call tick/next instead,
			//because tick pushes geometry into the cursor, and next performs a single convex hull on it. 
			throw new GeometryException("Invalid call for non merging convex hull.");
		}
		
		Geometry geometry = m_inputGeometryCursor.next();
		if (geometry != null) {
			m_hull.addGeometry(geometry);
			return true;
		} else {
			throw new GeometryException("Expects a non-null geometry.");
		}
	}

	static Geometry calculateConvexHull_(Geometry geom,
			ProgressTracker progress_tracker) {
		if (isConvex_(geom, progress_tracker))
			return geom;

		int type = geom.getType().value();

		if (MultiPath.isSegment(type)) {
			Polyline polyline = new Polyline(geom.getDescription());
			polyline.addSegment((Segment) geom, true);
			return polyline;
		}

		if (type == Geometry.GeometryType.MultiPoint) {
			MultiPoint multi_point = (MultiPoint) geom;
			if (multi_point.getPointCount() == 2) {
				Point pt = new Point();
				Polyline polyline = new Polyline(geom.getDescription());
				multi_point.getPointByVal(0, pt);
				polyline.startPath(pt);
				multi_point.getPointByVal(1, pt);
				polyline.lineTo(pt);
				return polyline;
			}
		}

		Polygon convex_hull = ConvexHull.construct((MultiVertexGeometry) geom);
		return convex_hull;
	}

	static boolean isConvex_(Geometry geom, ProgressTracker progress_tracker) {
		if (geom.isEmpty())
			return true; // vacuously true

		int type = geom.getType().value();

		if (type == Geometry.GeometryType.Point)
			return true; // vacuously true

		if (type == Geometry.GeometryType.Envelope)
			return true; // always convex

		if (MultiPath.isSegment(type))
			return false; // upgrade to polyline

		if (type == Geometry.GeometryType.MultiPoint) {
			MultiPoint multi_point = (MultiPoint) geom;

			if (multi_point.getPointCount() == 1)
				return true; // vacuously true

			return false; // upgrade to polyline if point count is 2, otherwise
							// create convex hull
		}

		if (type == Geometry.GeometryType.Polyline) {
			Polyline polyline = (Polyline) geom;

			if (polyline.getPathCount() == 1 && polyline.getPointCount() <= 2)
				return true; // vacuously true

			return false; // create convex hull
		}

		Polygon polygon = (Polygon) geom;

		if (polygon.getPathCount() != 1)
			return false;

		if (polygon.getPointCount() <= 2)
			return true; // vacuously true

		return ConvexHull.isPathConvex(polygon, 0, progress_tracker);
	}
}
