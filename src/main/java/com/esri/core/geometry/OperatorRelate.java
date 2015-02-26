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

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;
import com.esri.core.geometry.Operator.Type;

/**
 *Performs the Relation operation between two geometries using the DE-9IM matrix encoded as a string.
 *
 */
public abstract class OperatorRelate extends Operator {
	@Override
	public Type getType() {
		return Type.Relate;
	}

    /**
    *Performs the Relation operation between two geometries using the DE-9IM matrix encoded as a string.
    *@param inputGeom1 The first geometry in the relation.
    *@param inputGeom2 The second geometry in the relation.
    *@param sr The spatial reference of the geometries.
    *@param de_9im_string The DE-9IM matrix relation encoded as a string.
    *@return Returns True if the relation holds, False otherwise.
    */
	public abstract boolean execute(Geometry inputGeom1, Geometry inputGeom2,
			SpatialReference sr, String de_9im_string, ProgressTracker progressTracker);

	public static OperatorRelate local() {
		return (OperatorRelate) OperatorFactoryLocal.getInstance().getOperator(
				Type.Relate);
	}
	
	@Override
	public boolean canAccelerateGeometry(Geometry geometry) {
		return RelationalOperations.Accelerate_helper
				.can_accelerate_geometry(geometry);
	}

	@Override
	public boolean accelerateGeometry(Geometry geometry,
			SpatialReference spatialReference,
			GeometryAccelerationDegree accelDegree) {
		return RelationalOperations.Accelerate_helper.accelerate_geometry(
				geometry, spatialReference, accelDegree);
	}	

}
