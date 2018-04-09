/*
 Copyright 1995-2017 Esri

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

/**
 * The base class for Geometry Operators.
 */
public abstract class Operator {
	/**
	 * The operator type enum.
	 */
	public enum Type {
		Project,

		ExportToJson, ImportFromJson,
		ExportToESRIShape, ImportFromESRIShape,

		Union, Difference,

		Proximity2D, Centroid2D,

		Relate, Equals, Disjoint, Intersects, Within, Contains, Crosses, Touches, Overlaps,

		Buffer, Distance, Intersection, Clip, Cut, DensifyByLength,
		DensifyByAngle, LabelPoint,

		GeodesicBuffer, GeodeticDensifyByLength, ShapePreservingDensify, GeodeticLength, GeodeticArea,

		Simplify, SimplifyOGC, Offset, Generalize,

		ExportToWkb, ImportFromWkb, ExportToWkt, ImportFromWkt, ImportFromGeoJson, ExportToGeoJson, SymmetricDifference, ConvexHull, Boundary

	}

	public abstract Type getType();

	/**
	 * Processes Geometry to accelerate operations on it. The Geometry and it's
	 * copies remain accelerated until modified. The acceleration of Geometry
	 * can be a time consuming operation. The accelerated geometry also takes
	 * more memory. Some operators share the same accelerator, some require
	 * a different one. If the accelerator is built for the given parameters,
	 * the method returns immediately.
	 * 
	 * @param geometry
	 *            The geometry to be accelerated
	 * @param spatialReference
	 *            The spatial reference of that geometry
	 * @param accelDegree The acceleration degree for geometry.
	 */
	public boolean accelerateGeometry(Geometry geometry,
			SpatialReference spatialReference,
			GeometryAccelerationDegree accelDegree) {
		// Override at specific Operator level
		return false;
	}

	/**
	 * Returns true if the geometry can be accelerated.
	 * 
	 * @param geometry
	 * @return true for geometries that can be accelerated, false for geometries
	 *         that cannot
	 */
	public boolean canAccelerateGeometry(Geometry geometry) {
		// Override at specific Operator level
		return false;
	}

	/**
	 * Removes accelerators from given geometry.
	 * @param geometry The geometry instance to remove accelerators from.
	 */
	public static void deaccelerateGeometry(Geometry geometry) {
		Geometry.Type gt = geometry.getType();
		if (Geometry.isMultiVertex(gt.value()))
		{
			GeometryAccelerators accel = ((MultiVertexGeometryImpl) geometry
					._getImpl())._getAccelerators();
			if (accel != null){
				accel._setRasterizedGeometry(null);
				accel._setQuadTree(null);
			}
		}
	}

}
