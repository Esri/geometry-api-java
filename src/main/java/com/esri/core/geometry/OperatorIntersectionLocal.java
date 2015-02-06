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

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;

class OperatorIntersectionLocal extends OperatorIntersection {

	@Override
	public GeometryCursor execute(GeometryCursor inputGeometries,
			GeometryCursor intersector, SpatialReference sr,
			ProgressTracker progressTracker) {

		return new OperatorIntersectionCursor(inputGeometries, intersector, sr,
				progressTracker, -1);
	}

	@Override
	public GeometryCursor execute(GeometryCursor input_geometries,
			GeometryCursor intersector, SpatialReference sr,
			ProgressTracker progress_tracker, int dimensionMask) {
		return new OperatorIntersectionCursor(input_geometries, intersector,
				sr, progress_tracker, dimensionMask);
	}

	@Override
	public Geometry execute(Geometry inputGeometry, Geometry intersector,
			SpatialReference sr, ProgressTracker progressTracker) {
		SimpleGeometryCursor inputGeomCurs = new SimpleGeometryCursor(
				inputGeometry);
		SimpleGeometryCursor intersectorCurs = new SimpleGeometryCursor(
				intersector);
		GeometryCursor geometryCursor = execute(inputGeomCurs, intersectorCurs,
				sr, progressTracker);

		return geometryCursor.next();
	}

	@Override
	public boolean accelerateGeometry(Geometry geometry,
			SpatialReference spatialReference,
			GeometryAccelerationDegree accelDegree) {
		if (!canAccelerateGeometry(geometry))
			return false;

		double tol = InternalUtils.calculateToleranceFromGeometry(spatialReference, geometry, false);
		boolean accelerated = ((MultiVertexGeometryImpl) geometry._getImpl())
				._buildQuadTreeAccelerator(accelDegree);
		accelerated |= ((MultiVertexGeometryImpl) geometry._getImpl())
				._buildRasterizedGeometryAccelerator(tol, accelDegree);
		return accelerated;
	}

	@Override
	public boolean canAccelerateGeometry(Geometry geometry) {
		return RasterizedGeometry2D.canUseAccelerator(geometry);
	}

}
