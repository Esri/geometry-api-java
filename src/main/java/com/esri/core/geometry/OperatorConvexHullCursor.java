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

class OperatorConvexHullCursor extends GeometryCursor {
	private ProgressTracker m_progress_tracker;
	private boolean m_b_merge;
	private boolean m_b_done;
	private GeometryCursor m_inputGeometryCursor;
	private int m_index;
	ConvexHull m_hull = new ConvexHull();

	OperatorConvexHullCursor(boolean b_merge, GeometryCursor geoms, ProgressTracker progress_tracker) {
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
				Geometry result = calculateConvexHullMerging_(m_inputGeometryCursor, m_progress_tracker);
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

	private Geometry calculateConvexHullMerging_(GeometryCursor geoms, ProgressTracker progress_tracker) {
		Geometry geometry;

		while ((geometry = geoms.next()) != null)
			m_hull.addGeometry(geometry);

		return m_hull.getBoundingGeometry();
	}

	@Override
	public boolean tock() {
		if (m_b_done)
			return true;

		if (!m_b_merge) {
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

	static Geometry calculateConvexHull_(Geometry geom, ProgressTracker progress_tracker) {
		if (geom.isEmpty())
			return geom.createInstance();

		Geometry.Type type = geom.getType();

		if (Geometry.isSegment(type.value())) {// Segments are always returned either as a Point or Polyline
			Segment segment = (Segment) geom;
			if (segment.getStartXY().equals(segment.getEndXY())) {
				Point point = new Point();
				segment.queryStart(point);
				return point;
			} else {
				Point pt = new Point();
				Polyline polyline = new Polyline(geom.getDescription());
				segment.queryStart(pt);
				polyline.startPath(pt);
				segment.queryEnd(pt);
				polyline.lineTo(pt);
				return polyline;
			}
		} else if (type == Geometry.Type.Envelope) {
			Envelope envelope = (Envelope) geom;
			Envelope2D env = new Envelope2D();
			envelope.queryEnvelope2D(env);
			if (env.xmin == env.xmax && env.ymin == env.ymax) {
				Point point = new Point();
				envelope.queryCornerByVal(0, point);
				return point;
			} else if (env.xmin == env.xmax || env.ymin == env.ymax) {
				Point pt = new Point();
				Polyline polyline = new Polyline(geom.getDescription());
				envelope.queryCornerByVal(0, pt);
				polyline.startPath(pt);
				envelope.queryCornerByVal(1, pt);
				polyline.lineTo(pt);
				return polyline;
			} else {
				Polygon polygon = new Polygon(geom.getDescription());
				polygon.addEnvelope(envelope, false);
				return polygon;
			}
		}

		if (isConvex_(geom, progress_tracker)) {
			if (type == Geometry.Type.MultiPoint) {// Downgrade to a Point for simplistic output
				MultiPoint multi_point = (MultiPoint) geom;
				Point point = new Point();
				multi_point.getPointByVal(0, point);
				return point;
			}

			return geom;
		}

		assert (Geometry.isMultiVertex(type.value()));

		Geometry convex_hull = ConvexHull.construct((MultiVertexGeometry) geom);
		return convex_hull;
	}

	static boolean isConvex_(Geometry geom, ProgressTracker progress_tracker) {
		if (geom.isEmpty())
			return true; // vacuously true

		Geometry.Type type = geom.getType();

		if (type == Geometry.Type.Point)
			return true; // vacuously true

		if (type == Geometry.Type.Envelope) {
			Envelope envelope = (Envelope) geom;
			if (envelope.getXMin() == envelope.getXMax() || envelope.getYMin() == envelope.getYMax())
				return false;

			return true;
		}

		if (MultiPath.isSegment(type.value())) {
			Segment segment = (Segment) geom;
			if (segment.getStartXY().equals(segment.getEndXY()))
				return false;

			return true; // true, but we will upgrade to a Polyline for the ConvexHull operation
		}

		if (type == Geometry.Type.MultiPoint) {
			MultiPoint multi_point = (MultiPoint) geom;

			if (multi_point.getPointCount() == 1)
				return true; // vacuously true, but we will downgrade to a Point for the ConvexHull operation

			return false;
		}

		if (type == Geometry.Type.Polyline) {
			Polyline polyline = (Polyline) geom;

			if (polyline.getPathCount() == 1 && polyline.getPointCount() == 2) {
				if (!polyline.getXY(0).equals(polyline.getXY(1)))
					return true; // vacuously true
			}

			return false; // create convex hull
		}

		Polygon polygon = (Polygon) geom;

		if (polygon.getPathCount() != 1 || polygon.getPointCount() < 3)
			return false;

		return ConvexHull.isPathConvex(polygon, 0, progress_tracker);
	}
}
