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
 * 
 * Union of geometries.
 *
 */
public abstract class OperatorUnion extends Operator implements CombineOperator{
	@Override
	public Type getType() {
		return Type.Union;
	}

	/**
	 *Performs the Topological Union operation on the geometry set.
	 *@param inputGeometries is the set of Geometry instances to be unioned.
	 *
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeometries,
			SpatialReference sr, ProgressTracker progressTracker);

	/**
	 *Performs the Topological Union operation on two geometries.
	 *@param geom1 and geom2 are the geometry instances to be unioned.
	 *
	 */
	public abstract Geometry execute(Geometry geom1, Geometry geom2,
			SpatialReference sr, ProgressTracker progressTracker);

	public static OperatorUnion local() {
		return (OperatorUnion) OperatorFactoryLocal.getInstance().getOperator(
				Type.Union);
	}

}
