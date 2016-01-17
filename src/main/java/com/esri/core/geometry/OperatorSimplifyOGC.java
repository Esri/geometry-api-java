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
 * Simplifies the geometry or determines if the geometry is simple. Follows the OGC specification for the Simple Feature Access
 * v. 1.2.1 (06-103r4).
 * Uses tolerance to determine equal vertices or points of intersection. 
 *
 */
public abstract class OperatorSimplifyOGC extends Operator {
	@Override
	public Operator.Type getType() {
		return Operator.Type.SimplifyOGC;
	}

	/**
	 * Tests if the Geometry is simple for OGC specification.
	 *
	 * Note: As other methods in the OperatorSimplifyOGC, this method uses
	 * tolerance from the spatial reference.
	 * Points that are within the tolerance are considered equal.
	 *
	 * When this method returns true, the OperatorSimplify.isSimpleAsFeature
	 * will return true also (this does not necessary happen the other way
	 * around).
	 *
	 * @param  geom
	 *         The Geometry to be tested.
	 *
	 * @param  spatialRef
	 *         Spatial reference to obtain the tolerance from. When null, the tolerance
	 *         will be derived individually from geometry bounds.
	 *
	 * @param  bForceTest
	 *         When True, the Geometry will be tested regardless of the
	 *         IsKnownSimple flag.
	 *
	 * @param  result
	 *         The non simple result TODO
	 *
	 * @param  progressTracker
	 *         Allows cancellation of a long operation. Can be null.
	 *
	 * @return {@code true} if the Geometry is simple according to the OGC specification.
	 */
	public abstract boolean isSimpleOGC(Geometry geom,
			SpatialReference spatialRef, boolean bForceTest,
			NonSimpleResult result, ProgressTracker progressTracker);

	/**
	 * Processes geometry cursor to ensure its geometries are simple for OGC specification.
	 *
	 * The isSimpleOGC returns true after this call.
	 *
	 * @param  geoms
	 *         Geometries to be simplified.
	 *
	 * @param  sr
	 *         Spatial reference to obtain the tolerance from. When null, the
	 *         tolerance will be derived individually for each geometry from
	 *         its bounds.
	 *
	 * @param  bForceSimplify
	 *         When True, the Geometry will be simplified regardless of the
	 *         internal IsKnownSimple flag.
	 *
	 * @param  progressTracker
	 *         Allows cancellation of a long operation. Can be null.
	 *
	 * @return a GeometryCursor of simplified geometries.
	 */
	public abstract GeometryCursor execute(GeometryCursor geoms,
			SpatialReference sr, boolean bForceSimplify,
			ProgressTracker progressTracker);

	/**
	 * Processes geometry to ensure it is simple for OGC specification.
	 *
	 * The isSimpleOGC returns true after this call.
	 *
	 * @param  geom
	 *         The geometry to be simplified.
	 *
	 * @param  sr
	 *         Spatial reference to obtain the tolerance from. When null, the
	 *         tolerance will be derived individually from geometry bounds.
	 *
	 * @param  bForceSimplify
	 *         When True, the Geometry will be simplified regardless of the
	 *         internal IsKnownSimple flag.
	 *
	 * @param  progressTracker
	 *         Allows cancellation of a long operation. Can be null.
	 *
	 * @return a simple Geometry that should be visually equivalent to the
	 *         input geometry.
	 */
	public abstract Geometry execute(Geometry geom, SpatialReference sr,
			boolean bForceSimplify, ProgressTracker progressTracker);

	public static OperatorSimplifyOGC local() {
		return (OperatorSimplifyOGC) OperatorFactoryLocal.getInstance()
				.getOperator(Type.SimplifyOGC);
	}

}
