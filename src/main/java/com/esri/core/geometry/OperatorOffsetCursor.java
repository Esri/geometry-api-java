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

class OperatorOffsetCursor extends GeometryCursor {
	GeometryCursor m_inputGeoms;
	SpatialReferenceImpl m_spatialReference;
	ProgressTracker m_progressTracker;
	double m_distance;
	double m_miterLimit;
	OperatorOffset.JoinType m_joins;
	double m_flattenError;
	int m_index;

	OperatorOffsetCursor(GeometryCursor inputGeometries, SpatialReference sr,
			double distance, OperatorOffset.JoinType joins, double bevelRatio,
			double flattenError, ProgressTracker progressTracker) {
		m_index = -1;
		m_inputGeoms = inputGeometries;
		m_spatialReference = (SpatialReferenceImpl) sr;
		m_distance = distance;
		m_joins = joins;
		m_miterLimit = bevelRatio;
		m_flattenError = flattenError;
		m_progressTracker = progressTracker;
	}

	public Geometry next() {
		Geometry geom = m_inputGeoms.next();
		if (geom != null) {
			m_index = m_inputGeoms.getGeometryID();
			return Offset(geom);
		}
		return null;
	}

	public int getGeometryID() {
		return m_index;
	}

	Geometry Offset(Geometry geom) {
		double tolerance;
		if (m_flattenError <= 0)
			tolerance = InternalUtils.calculateToleranceFromGeometry(
					m_spatialReference, geom, false);
		else
			tolerance = m_flattenError;
		return ConstructOffset.execute(geom, m_distance, m_joins, m_miterLimit,
				tolerance, m_progressTracker);
	}

}
