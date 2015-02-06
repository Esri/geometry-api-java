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

class OperatorDensifyByLengthCursor extends GeometryCursor {

	GeometryCursor m_inputGeoms;
	// SpatialReferenceImpl m_spatialReference;
	double m_maxLength;
	int m_index;

	public OperatorDensifyByLengthCursor(GeometryCursor inputGeoms1,
			double maxLength, ProgressTracker progressTracker) {
		m_index = -1;
		m_inputGeoms = inputGeoms1;
		m_maxLength = maxLength;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	@Override
	public Geometry next() {
		Geometry geom;
		if ((geom = m_inputGeoms.next()) != null) {
			m_index = m_inputGeoms.getGeometryID();
			return densifyByLength(geom);
		}
		return null;
	}

	private Geometry densifyByLength(Geometry geom) {
		if (geom.isEmpty() || geom.getDimension() < 1)
			return geom;

		int geometryType = geom.getType().value();

		// TODO implement IsMultiPath and remove Polygon and Polyline call to
		// match Native
		// if (Geometry.IsMultiPath(geometryType))
		if (geometryType == Geometry.GeometryType.Polygon)
			return densifyMultiPath((MultiPath) geom);
		else if (Geometry.GeometryType.Polyline == geometryType)
			return densifyMultiPath((MultiPath) geom);
		else if (Geometry.isSegment(geometryType))
			return densifySegment((Segment) geom);
		else if (geometryType == Geometry.GeometryType.Envelope)
			return densifyEnvelope((Envelope) geom);
		else
			// TODO fix geometry exception to match native implementation
			throw GeometryException.GeometryInternalError();// GEOMTHROW(internal_error);

		// unreachable in java
		// return null;
	}

	private Geometry densifySegment(Segment geom) {
		double length = geom.calculateLength2D();
		if (length <= m_maxLength)
			return (Geometry) geom;

		Polyline polyline = new Polyline(geom.getDescription());
		polyline.addSegment(geom, true);
		return densifyMultiPath((MultiPath) polyline);
	}

	private Geometry densifyEnvelope(Envelope geom) {
		Polygon polygon = new Polygon(geom.getDescription());
		polygon.addEnvelope(geom, false);

		Envelope2D env2D = new Envelope2D();
		geom.queryEnvelope2D(env2D);
		double w = env2D.getWidth();
		double h = env2D.getHeight();
		if (w <= m_maxLength && h <= m_maxLength)
			return (Geometry) polygon;

		return densifyMultiPath((MultiPath) polygon);
	}

	private Geometry densifyMultiPath(MultiPath geom) {
		MultiPath densifiedPoly = (MultiPath) geom.createInstance();
		SegmentIterator iter = geom.querySegmentIterator();
		while (iter.nextPath()) {
			boolean bStartNewPath = true;
			while (iter.hasNextSegment()) {
				Segment seg = iter.nextSegment();
				if (seg.getType().value() != Geometry.GeometryType.Line)
					throw new GeometryException("not implemented");

				boolean bIsClosing = iter.isClosingSegment();

				double len = seg.calculateLength2D();
				if (len > m_maxLength) {// need to split
					double dcount = Math.ceil(len / m_maxLength);

					Point point = new Point(geom.getDescription());// LOCALREFCLASS1(Point,
																	// VertexDescription,
																	// point,
																	// geom.getDescription());
					if (bStartNewPath) {
						bStartNewPath = false;
						seg.queryStart(point);
						densifiedPoly.startPath(point);
					}
					double dt = 1.0 / dcount;
					double t = dt;
					for (int i = 0, n = (int) dcount - 1; i < n; i++) {
						seg.queryCoord(t, point);
						densifiedPoly.lineTo(point);
						t += dt;
					}

					if (!bIsClosing) {
						seg.queryEnd(point);
						densifiedPoly.lineTo(point);
					} else {
						densifiedPoly.closePathWithLine();
					}

					bStartNewPath = false;
				} else {
					if (!bIsClosing)
						densifiedPoly.addSegment(seg, bStartNewPath);
					else
						densifiedPoly.closePathWithLine();

					bStartNewPath = false;
				}
			}
		}

		return (Geometry) densifiedPoly;
	}

}
