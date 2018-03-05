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
 *Intersection of geometries by a given geometry.
 */
public abstract class OperatorIntersection extends Operator implements CombineOperator {
	@Override
	public Type getType() {
		return Type.Intersection;
	}

	/**
	 *Performs the Topological Intersection operation on the geometry set.
	 *@param inputGeometries is the set of Geometry instances to be intersected by the intersector.
	 *@param intersector is the intersector Geometry.
	 *
	 *The operator intersects every geometry in the inputGeometries with the first geometry of the intersector and returns the result.
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeometries,
			GeometryCursor intersector, SpatialReference sr,
			ProgressTracker progressTracker);

	/**
	 *Performs the Topological intersection operation on the geometry set.
	 *@param input_geometries is the set of Geometry instances to be intersected by the intersector.
	 *@param intersector is the intersector Geometry. Only single intersector is used, therefore, the intersector.next() is called only once.
	 *@param sr The spatial reference is used to get tolerance value. Can be null, then the tolerance is not used and the operation is performed with
	 *a small tolerance value just enough to make the operation robust.
	 *@param progress_tracker Allows to cancel the operation. Can be null.
	 *@param dimensionMask The dimension of the intersection. The value is either -1, or a bitmask mask of values (1 &lt;&lt; dim).
	 *The value of -1 means the lower dimension in the intersecting pair.
	 *This is a fastest option when intersecting polygons with polygons or polylines.
	 *The bitmask of values (1 &lt;&lt; dim), where dim is the desired dimension value, is used to indicate
	 *what dimensions of geometry one wants to be returned. For example, to return
	 *multipoints and lines only, pass (1 &lt;&lt; 0) | (1 &lt;&lt; 1), which is equivalen to 1 | 2, or 3.
	 *@return Returns the cursor of the intersection result. The cursors' getGeometryID method returns the current ID of the input geometry
	 *being processed. When dimensionMask is a bitmask, there will be n result geometries per one input geometry returned, where n is the number
	 *of bits set in the bitmask. For example, if the dimensionMask is 5, there will be two geometries per one input geometry.
	 *
	 *The operator intersects every geometry in the input_geometries with the first geometry of the intersector and returns the result.
	 *
	 *Note, when the dimensionMask is -1, then for each intersected pair of geometries,
	 *the result has the lower of dimentions of the two geometries. That is, the dimension of the Polyline/Polyline intersection
	 *is always 1 (that is, for polylines it never returns crossing points, but the overlaps only).
	 *If dimensionMask is 7, the operation will return any possible intersections.
	 */
	public abstract GeometryCursor execute(GeometryCursor input_geometries,
			GeometryCursor intersector, SpatialReference sr,
			ProgressTracker progress_tracker, int dimensionMask);

	/**
	 *Performs the Topological Intersection operation on the geometry.
	 *The result has the lower of dimentions of the two geometries. That is, the dimension of the
	 *Polyline/Polyline intersection is always 1 (that is, for polylines it never returns crossing
	 *points, but the overlaps only).
	 *The call is equivalent to calling the overloaded method using cursors:
	 *execute(new SimpleGeometryCursor(input_geometry), new SimpleGeometryCursor(intersector), sr, progress_tracker, mask).next();
	 *where mask can be either -1 or min(1 &lt;&lt; input_geometry.getDimension(), 1 &lt;&lt; intersector.getDimension());
	 *@param inputGeometry is the Geometry instance to be intersected by the intersector.
	 *@param intersector is the intersector Geometry.
	 *@param sr The spatial reference to get the tolerance value from. Can be null, then the tolerance is calculated from the input geometries.
	 *@return Returns the intersected Geometry.
	 */
	public abstract Geometry execute(Geometry inputGeometry,
			Geometry intersector, SpatialReference sr,
			ProgressTracker progressTracker);

	public static OperatorIntersection local() {
		return (OperatorIntersection) OperatorFactoryLocal.getInstance()
				.getOperator(Type.Intersection);
	}

}
