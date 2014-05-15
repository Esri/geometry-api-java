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

/**
 * Simplifies geometry or determines if geometry is simple. Tries to follow OGC specification 1.2.1.
 * Uses tolerance to determine equal vertices or points of intersection. 
 *
 */
public abstract class OperatorSimplifyOGC extends Operator {
	@Override
	public Operator.Type getType() {
		return Operator.Type.SimplifyOGC;
	}

	/**
	 *Tests if the Geometry is simple for OGC spec 1.2.1.
	 *@param geom The Geometry to be tested.
	 *@param bForceTest When True, the Geometry will be tested regardless of the IsKnownSimple flag.
	 *
	 *Note: As other methods in the OperatorSimplify, this method uses tolerance from the spatial reference.
	 *Points that are within the tolerance are considered equal.
	 */
	public abstract boolean isSimpleOGC(Geometry geom,
			SpatialReference spatialRef, boolean bForceTest,
			NonSimpleResult result, ProgressTracker progressTracker);

	/**
	 * This method is still in development. Use Operator_simplify for now.
	 * 
	 *Performs the Simplify operation on the geometry set.
	 *@return Returns a GeometryCursor of simplified geometries.
	 */
	public abstract GeometryCursor execute(GeometryCursor geoms,
			SpatialReference sr, boolean bForceSimplify,
			ProgressTracker progressTracker);

	/**
	 * This method is still in development. Use Operator_simplify for now.
	 * 
	 *Performs the Simplify operation on a Geometry. 
	 *@return Returns a simple Geometry.
	 */
	public abstract Geometry execute(Geometry geom, SpatialReference sr,
			boolean bForceSimplify, ProgressTracker progressTracker);

	public static OperatorSimplifyOGC local() {
		return (OperatorSimplifyOGC) OperatorFactoryLocal.getInstance()
				.getOperator(Type.SimplifyOGC);
	}

}
