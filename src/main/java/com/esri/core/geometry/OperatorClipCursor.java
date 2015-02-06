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

class OperatorClipCursor extends GeometryCursor {

	GeometryCursor m_inputGeometryCursor;
	SpatialReferenceImpl m_spatialRefImpl;
	Envelope2D m_envelope;
	double m_tolerance;
	int m_index;

	OperatorClipCursor(GeometryCursor geoms, Envelope2D envelope,
			SpatialReference spatial_ref, ProgressTracker progress_tracker) {
		m_index = -1;
		if (geoms == null)
			throw new IllegalArgumentException();

		m_envelope = envelope;
		m_inputGeometryCursor = geoms;
		m_spatialRefImpl = (SpatialReferenceImpl) spatial_ref;
		m_tolerance = InternalUtils.calculateToleranceFromGeometry(spatial_ref,
				envelope, false);
	}

	@Override
	public Geometry next() {
		Geometry geometry;
		if ((geometry = m_inputGeometryCursor.next()) != null) {
			m_index = m_inputGeometryCursor.getGeometryID();
			return Clipper.clip(geometry, m_envelope, m_tolerance, 0.0);
		}
		return null;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	// virtual bool IsRecycling() OVERRIDE { return false; }

}
