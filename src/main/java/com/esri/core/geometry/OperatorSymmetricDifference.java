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
 * Symmetric difference (XOR) operation between geometries. 
 *
 */
public abstract class OperatorSymmetricDifference extends Operator implements CombineOperator {
	@Override
	public Type getType() {
		return Type.Difference;
	}

	/**
	 *Performs the Symmetric Difference operation on the geometry set.
	 *@param inputGeometries is the set of Geometry instances to be XOR'd by rightGeometry.
	 *@param rightGeometry is the Geometry being XOR'd with the inputGeometies.
	 *@return Returns the result of the symmetric difference.
	 *
	 *The operator XOR's every geometry in inputGeometries with rightGeometry.
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeometries,
			GeometryCursor rightGeometry, SpatialReference sr,
			ProgressTracker progressTracker);

	/**
	 *Performs the Symmetric Difference operation on the two geometries.
	 *@param leftGeometry is one of the Geometry instances in the XOR operation.
	 *@param rightGeometry is one of the Geometry instances in the XOR operation.
	 *@return Returns the result of the symmetric difference.
	 */
	public abstract Geometry execute(Geometry leftGeometry,
			Geometry rightGeometry, SpatialReference sr,
			ProgressTracker progressTracker);

	public static OperatorSymmetricDifference local() {
		return (OperatorSymmetricDifference) OperatorFactoryLocal.getInstance()
				.getOperator(Type.SymmetricDifference);
	}

}
