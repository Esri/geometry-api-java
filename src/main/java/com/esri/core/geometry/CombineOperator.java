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

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ProgressTracker;

/**
 * Interface for operators that act on two geometries to produce a new geometry as result.
 */
public interface CombineOperator {

	/**
	 * Operation on two geometries, returning a third. Examples include
	 * Intersection, Difference, and so forth.
	 *
	 * @param geom1 is the geometry instance to be operated on.
	 * @param geom2 is the geometry instance to be operated on.
	 * @param sr The spatial reference to get the tolerance value from.
	 * When sr is null, the tolerance is calculated from the input geometries.
	 * @param progressTracker ProgressTracker instance that is used to cancel the lengthy operation. Can be null.
	 * @return Returns the result geoemtry. In some cases the returned value can point to geom1 or geom2
	 * instance. For example, the OperatorIntersection may return geom2 when it is completely
	 * inside of the geom1.
	 */
	public Geometry execute(Geometry geom1, Geometry geom2,
			SpatialReference sr, ProgressTracker progressTracker);

}
