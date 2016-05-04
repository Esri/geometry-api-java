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

    /**
    *Creates a buffer around the input geometries
    *
    *@param input_geometries The geometries to buffer.
    *@param sr The Spatial_reference of the Geometries. It is used to obtain the tolerance. Can be null.
    *@param distances The buffer distances for the Geometries. If the size of the distances array is less than the number of geometries in the input_geometries, the last distance value is used for the rest of geometries.
    *@param max_deviation The max deviation of the result buffer from the true buffer in the units of the sr.
    *When max_deviation is NaN or 0, it is replaced with 1e-5 * abs(distance).
    *When max_deviation is larger than MIN = 0.5 * abs(distance), it is replaced with MIN. See below for more information.
    *@param max_vertices_in_full_circle The maximum number of vertices in polygon produced from a buffered point. A value of 96 is used in methods that do not accept max_vertices_in_full_circle.
    *If the value is less than MIN=12, it is set to MIN. See below for more information.
    *@param b_union If True, the buffered geometries will be unioned, otherwise they wont be unioned.
    *@param progress_tracker The progress tracker that allows to cancel the operation. Pass null if not needed.
    *
    *The max_deviation and max_vertices_in_full_circle control the quality of round joins in the buffer. That is, the precision of the buffer is max_deviation unless
    *the number of required vertices is too large.
    *The max_vertices_in_full_circle controls how many vertices can be in each round join in the buffer. It is approximately equal to the number of vertices in the polygon around a
    *buffered point. It has a priority over max_deviation. The max deviation is the distance from the result polygon to a true buffer.
    *The real deviation is calculated as the max(max_deviation, abs(distance) * (1 - cos(PI / max_vertex_in_complete_circle))).
    *
    *Note that max_deviation can be exceeded because geometry is generalized with 0.25 * real_deviation, also input segments closer than 0.25 * real_deviation are 
    *snapped to a point.
    */
    abstract GeometryCursor execute(GeometryCursor input_geometries, SpatialReference sr, double[] distances, double max_deviation, int max_vertices_in_full_circle, boolean b_union, ProgressTracker progress_tracker);
	
	public static OperatorBuffer local() {
		return (OperatorBuffer) OperatorFactoryLocal.getInstance().getOperator(
				Type.Buffer);
	}
}
