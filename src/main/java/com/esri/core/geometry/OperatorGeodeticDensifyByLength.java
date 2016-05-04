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

/**
 * Densifies the line segments by length, making them run along specified geodetic curves.
 * 
* Use this operator to construct geodetic curves.
 */
abstract class OperatorGeodeticDensifyByLength extends Operator {

	@Override
	public Type getType() {
		return Type.GeodeticDensifyByLength;
	}

	/**
	 * Densifies input geometries. Attributes are interpolated along the scalar t-values of the input segments obtained from the length ratios along the densified segments.
	 *
	 * @param geoms The geometries to be densified.
	 * @param maxSegmentLengthMeters The maximum segment length (in meters) allowed. Must be a positive value.
	 * @param sr The SpatialReference of the Geometry.
	 * @param curveType The interpretation of a line connecting two points.
	 * @return Returns the densified geometries (It does nothing to geometries with dim less than 1, but simply passes them along).
	 *
	 * Note the behavior is not determined for any geodetic curve segments that connect two poles, or for loxodrome segments that connect to any pole.
	 */
	public abstract GeometryCursor execute(GeometryCursor geoms, double maxSegmentLengthMeters, SpatialReference sr, int curveType, ProgressTracker progressTracker);

	/**
	 * Same as above, but works with a single geometry.
	 */
	public abstract Geometry execute(Geometry geom, double maxSegmentLengthMeters, SpatialReference sr, int curveType, ProgressTracker progressTracker);

	public static OperatorGeodeticDensifyByLength local() {
		return (OperatorGeodeticDensifyByLength) OperatorFactoryLocal.getInstance()
						.getOperator(Type.GeodeticDensifyByLength);
	}
}
