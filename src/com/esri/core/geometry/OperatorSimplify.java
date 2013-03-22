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
 * Simplifies geometry or determines if geometry is simple. 
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
	 *@param bForceTest When True, the Geometry will be tested regardless of the IsKnownSimple flag.
	 */
	public abstract boolean isSimpleAsFeature(Geometry geom,
			SpatialReference spatialRef, boolean bForceTest,
			NonSimpleResult result, ProgressTracker progressTracker);

	// Reviewed vs. Feb 8 2011
	public boolean isSimpleAsFeature(Geometry geom,
			SpatialReference spatialRef, ProgressTracker progressTracker) {
		return isSimpleAsFeature(geom, spatialRef, false, null, progressTracker);
	}

	/**
	 *Performs the Simplify operation on the geometry set.
	 *@return Returns a GeometryCursor of simplified geometries.
	 */
	public abstract GeometryCursor execute(GeometryCursor geoms,
			SpatialReference sr, boolean bForceSimplify,
			ProgressTracker progressTracker);

	/**
	 *Performs the Simplify operation on a Geometry
	 *@return Returns a simple Geometry.
	 */
	public abstract Geometry execute(Geometry geom, SpatialReference sr,
			boolean bForceSimplify, ProgressTracker progressTracker);

	public static OperatorSimplify local() {
		return (OperatorSimplify) OperatorFactoryLocal.getInstance()
				.getOperator(Type.Simplify);
	}
}
