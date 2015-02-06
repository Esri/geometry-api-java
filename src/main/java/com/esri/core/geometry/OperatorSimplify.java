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
 * Simplifies the geometry or determines if the geometry is simple. The goal of the OperatorSimplify is to produce a geometry that is
 * valid for the Geodatabase to store without additional processing.
 * 
 * The Geoprocessing tool CheckGeometries should accept geometries
 * produced by this operator's execute method. For Polylines the effect of execute is the same as
 * IPolyline6.NonPlanarSimplify, while for the Polygons and Multipoints it is same as ITopologicalOperator.Simplify.
 * For the Point class this operator does nothing, and the point is always simple.
 * 
 * The isSimpleAsFeature should return true after the execute method.
 * 
 * See also OperatorSimplifyOGC.
 *
 */
public abstract class OperatorSimplify extends Operator {
	@Override
	public Operator.Type getType() {
		return Operator.Type.Simplify;
	}

	/**
	 *Tests if the Geometry is simple.
	 *@param geom The Geometry to be tested.
	 *@param spatialRef Spatial reference from which the tolerance is obtained. Can be null, then a 
	 *very small tolerance value is derived from the geometry bounds.
	 *@param bForceTest When True, the Geometry will be tested regardless of the internal IsKnownSimple flag.
	 *@param result if not null, will contain the results of the check.
	 *@param progressTracker Allows cancellation of a long operation. Can be null. 
	 **/
	public abstract boolean isSimpleAsFeature(Geometry geom,
			SpatialReference spatialRef, boolean bForceTest,
			NonSimpleResult result, ProgressTracker progressTracker);

	/**
	 *Tests if the Geometry is simple (second call will use a cached IsKnownSimple flag and immediately return).
	 *@param geom The Geometry to be tested.
	 *@param spatialRef Spatial reference from which the tolerance is obtained. Can be null, then a 
	 *very small tolerance value is derived from the geometry bounds.
	 *@param progressTracker Allows cancellation of a long operation. Can be null.
	 *
	 */
	public boolean isSimpleAsFeature(Geometry geom,
			SpatialReference spatialRef, ProgressTracker progressTracker) {
		return isSimpleAsFeature(geom, spatialRef, false, null, progressTracker);
	}

	/**
	 *Performs the Simplify operation on the geometry cursor.
	 *@param geoms Geometries to simplify.
	 *@param sr Spatial reference from which the tolerance is obtained. When null, the tolerance
	 *will be derived individually for each geometry from its bounds.
	 *@param bForceSimplify When True, the Geometry will be simplified regardless of the internal IsKnownSimple flag.
	 *@param progressTracker Allows cancellation of a long operation. Can be null.
	 *@return Returns a GeometryCursor of simplified geometries.
	 *
	 *The isSimpleAsFeature returns true after this method.
	 */
	public abstract GeometryCursor execute(GeometryCursor geoms,
			SpatialReference sr, boolean bForceSimplify,
			ProgressTracker progressTracker);

	/**
	 *Performs the Simplify operation on the geometry.
	 *@param geom Geometry to simplify.
	 *@param sr Spatial reference from which the tolerance is obtained. When null, the tolerance
	 *will be derived individually for each geometry from its bounds.
	 *@param bForceSimplify When True, the Geometry will be simplified regardless of the internal IsKnownSimple flag.
	 *@param progressTracker Allows cancellation of a long operation. Can be null.
	 *@return Returns a simple geometry.
	 *
	 *The isSimpleAsFeature returns true after this method.
	 */
	public abstract Geometry execute(Geometry geom, SpatialReference sr,
			boolean bForceSimplify, ProgressTracker progressTracker);

	public static OperatorSimplify local() {
		return (OperatorSimplify) OperatorFactoryLocal.getInstance()
				.getOperator(Type.Simplify);
	}
}
