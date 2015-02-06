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
 * Projection of geometries to different coordinate systems.
 */
abstract class OperatorProject extends Operator {

	@Override
	public Type getType() {
		return Type.Project;
	}

	/**
	 * Performs the Project operation on a geometry cursor
	 * 
	 * @return Returns a GeometryCursor.
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeoms,
			ProjectionTransformation projection, ProgressTracker progressTracker);

	/**
	 * Performs the Project operation on a single geometry instance
	 * 
	 * @return Returns the Geometry after projection
	 */
	public abstract Geometry execute(Geometry geometry,
			ProjectionTransformation projection, ProgressTracker progressTracker);

	/**
	 * Transforms an array of points. Returns the number of points transformed.
	 */
	public abstract int transform(ProjectionTransformation transform,
			Point[] coordsSrc, int length, Point[] coordsDst);

	/**
	 * Transforms an array of 2D points and returns it. The points are stored in
	 * an interleaved array (x0, y0, x1, y1, x2, y2, ...).
	 * 
	 * @param transform
	 *            ProjectionTransformation
	 * @param coordsSrc
	 *            source coordinates to project.
	 * @param pointCount
	 *            the point count in the coordSrc. THere has to be at least
	 *            pointCount * 2 elements in the coordsSrc array.
	 * @return projected coordinates in the interleaved form.
	 */
	public abstract double[] transform(ProjectionTransformation transform,
					double[] coordsSrc, int pointCount);

	/**
	 * Folds a geometry into the 360 degree range of the associated spatial reference. If the spatial reference be a 'pannable' PCS or GCS. For other spatial types, the function throws an invalid
	 * argument exception. A pannable PCS it a Rectangular PCS where the x coordinate range is equivalent to a 360 degree range on the defining geographic Coordinate System(GCS). If the spatial
	 * reference is a GCS then it is always pannable(default 360 range for spatial reference in GCS coordinates is -180 to 180)
	 *
	 * If the geometry is an Envelope fold_into_360_range returns a polygon, unless the Envelope is empty, in which case the empty envelope is returned. The result geometry will be completely inside of
	 * the coordinate system extent. The folding happens where geometry intersects the min or max meridian of the spatial reference and when geometry is completely outside of the min-max meridian range.
	 * Folding does not preserve geodetic area or length. Folding does not preserve perimeter of a polygon.
	 *
	 * @param geom The geometry to be folded.
	 * @param pannableSR The pannable Spatial Reference.
	 * @return Folded geometry.
	 */
	public abstract Geometry foldInto360Range(Geometry geom, SpatialReference pannableSR);

	/**
	 * Same as fold_into_360_range. The difference is that this function preserves geodetic area of polygons and geodetic length of polylines. It does not preserve regular area and length or perimeter
	 * of polygons. Also, this function might change tangent of the lines at the points of folding.
	 *
	 * If the geometry is an Envelope fold_into_360_range returns a polygon, unless the Envelope is empty, in which case the empty envelope is returned. The result geometry will be completely inside of
	 * the coordinate system extent. The folding happens where geometry intersects the min or max meridian of the spatial reference and when geometry is completely outside of the min-max meridian range.
	 *
	 * @param geom The geometry to be folded.
	 * @param pannableSR The pannable Spatial Reference.
	 * @param curveType The type of geodetic curve to use to produce vertices at the points of folding. \return Folded geometry.
	 */
	public abstract Geometry foldInto360RangeGeodetic(Geometry geom, SpatialReference pannableSR, int curveType);

	public static OperatorProject local() {
		return (OperatorProject) OperatorFactoryLocal.getInstance()
						.getOperator(Type.Project);
	}

}
