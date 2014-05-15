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

import com.esri.core.geometry.Operator.Type;

/**
 *Creates buffer polygons around geometries.
 */
public abstract class OperatorBuffer extends Operator {
	@Override
	public Type getType() {
		return Type.Buffer;
	}

	/**
	 *Creates a buffer around the input geometries
	 *
	 *@param inputGeometries The geometries to buffer.
	 *@param sr The SpatialReference of the Geometries.
	 *@param distances The buffer distances for the Geometries. If the size of the distances array is less than the number of geometries in the inputGeometries, the last distance value is used for the rest of geometries.
	 *@param bUnion If True, the buffered geometries will be unioned, otherwise they wont be unioned.
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeometries,
			SpatialReference sr, double[] distances, boolean bUnion,
			ProgressTracker progressTracker);

	/**
	 *Creates a buffer around the input geometry
	 *
	 *@param inputGeometry The geometry to buffer.
	 *@param sr The SpatialReference of the Geometry.
	 *@param distance The buffer distance for the Geometry.
	 */
	public abstract Geometry execute(Geometry inputGeometry,
			SpatialReference sr, double distance,
			ProgressTracker progressTracker);

	public static OperatorBuffer local() {
		return (OperatorBuffer) OperatorFactoryLocal.getInstance().getOperator(
				Type.Buffer);
	}
}
