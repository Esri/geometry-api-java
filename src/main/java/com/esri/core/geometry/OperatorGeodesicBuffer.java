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

abstract class OperatorGeodesicBuffer extends Operator {

	@Override
	public Operator.Type getType() {
		return Operator.Type.GeodesicBuffer;
	}

	/**
	 * Creates a geodesic buffer around the input geometries
	 *
	 * @param inputGeometries The geometries to buffer.
	 * @param sr The Spatial_reference of the Geometries.
	 * @param curveType The geodetic curve type of the segments. If the curve_type is Geodetic_curve::shape_preserving, then the segments are densified in the projection where they are defined before
	 * buffering.
	 * @param distancesMeters The buffer distances in meters for the Geometries. If the size of the distances array is less than the number of geometries in the input_geometries, the last distance value
	 * is used for the rest of geometries.
	 * @param maxDeviationMeters The deviation offset to use for convergence. The geodesic arcs of the resulting buffer will be closer than the max deviation of the true buffer. Pass in NaN to use the
	 * default deviation.
	 * @param bReserved Must be false. Reserved for future development. Will throw an exception if not false.
	 * @param bUnion If True, the buffered geometries will be unioned, otherwise they wont be unioned.
	 * @param progressTracker Can be null. Allows to cancel lengthy operation.
	 * @return Geometry cursor over result buffers.
	 */
	abstract public GeometryCursor execute(GeometryCursor inputGeometries, SpatialReference sr, int curveType, double[] distancesMeters, double maxDeviationMeters, boolean bReserved, boolean bUnion, ProgressTracker progressTracker);

	/**
	 * Creates a geodesic buffer around the input geometry
	 *
	 * @param inputGeometry The geometry to buffer.
	 * @param sr The Spatial_reference of the Geometry.
	 * @param curveType The geodetic curve type of the segments. If the curve_type is Geodetic_curve::shape_preserving, then the segments are densified in the projection where they are defined before
	 * buffering.
	 * @param distanceMeters The buffer distance in meters for the Geometry.
	 * @param maxDeviationMeters The deviation offset to use for convergence. The geodesic arcs of the resulting buffer will be closer than the max deviation of the true buffer. Pass in NaN to use the
	 * default deviation.
	 * @param bReserved Must be false. Reserved for future development. Will throw an exception if not false.
	 * @param progressTracker Can be null. Allows to cancel lengthy operation.
	 * @return Returns result buffer.
	 */
	abstract public Geometry execute(Geometry inputGeometry, SpatialReference sr, int curveType, double distanceMeters, double maxDeviationMeters, boolean bReserved, ProgressTracker progressTracker);

	public static OperatorGeodesicBuffer local() {
		return (OperatorGeodesicBuffer) OperatorFactoryLocal.getInstance()
						.getOperator(Type.GeodesicBuffer);
	}
}
