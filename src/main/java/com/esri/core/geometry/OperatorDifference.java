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
 * Difference of geometries.
 */
public abstract class OperatorDifference extends Operator implements CombineOperator {

	@Override
	public Type getType() {
		return Type.Difference;
	}

	/**
	 * Performs the Topological Difference operation on the geometry set.
	 * 
	 * @param inputGeometries
	 *            is the set of Geometry instances to be subtracted by the
	 *            subtractor
	 * @param subtractor
	 *            is the Geometry being subtracted.
	 * @return Returns the result of the subtraction.
	 * 
	 *         The operator subtracts subtractor from every geometry in
	 *         inputGeometries.
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeometries,
			GeometryCursor subtractor, SpatialReference sr,
			ProgressTracker progressTracker);

	/**
	 * Performs the Topological Difference operation on the two geometries.
	 * 
	 * @param inputGeometry
	 *            is the Geometry instance on the left hand side of the
	 *            subtraction.
	 * @param subtractor
	 *            is the Geometry on the right hand side being subtracted.
	 * @return Returns the result of subtraction.
	 */
	public abstract Geometry execute(Geometry inputGeometry,
			Geometry subtractor, SpatialReference sr,
			ProgressTracker progressTracker);

	public static OperatorDifference local() {
		return (OperatorDifference) OperatorFactoryLocal.getInstance()
				.getOperator(Type.Difference);
	}

}
