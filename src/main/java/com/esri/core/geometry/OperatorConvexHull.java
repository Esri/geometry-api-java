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
 * Creates the convex hull of the input geometry.
 */
public abstract class OperatorConvexHull extends Operator {
  @Override
  public Operator.Type getType() {
    return Operator.Type.ConvexHull;
  }

  /** 
   * Calculates the convex hull.
   * @param geoms The input geometry cursor.
   * @param progress_tracker The progress tracker. Allows cancellation of a lengthy operation.
   * @param b_merge Put true if you want the convex hull of all the geometries in the cursor combined.
   * Put false if you want the convex hull of each geometry in the cursor individually.
   * @return Returns a cursor over result convex hulls.
   */
  abstract public GeometryCursor execute(GeometryCursor geoms, boolean b_merge,
          ProgressTracker progress_tracker);

  /** 
   * Calculates the convex hull geometry.
   * @param geom The input geometry.
   * @param progress_tracker The progress tracker. Allows cancellation of a lengthy operation.
   * @return Returns the convex hull.
   * 
   * Point - Returns the same point.
   * Envelope - returns the same envelope.
   * MultiPoint - If the point count is one, returns the same multipoint. If the point count is two, returns a polyline of the points. Otherwise, computes and returns the convex hull polygon.
   * Segment - Returns a polyline consisting of the segment.
   * Polyline - If consists of only one segment, returns the same polyline. Otherwise, computes and returns the convex hull polygon.
   * Polygon - If more than one path or if the path isn't already convex, computes and returns the convex hull polygon. Otherwise, returns the same polygon.
   */
  abstract public Geometry execute(Geometry geom,
          ProgressTracker progress_tracker);

  /** 
   * Checks whether a Geometry is convex.
   * @param geom The input geometry to test for convex.
   * @param progress_tracker The progress tracker.
   * @return Returns true if the geometry is convex.
   */
  abstract public boolean isConvex(Geometry geom,
          ProgressTracker progress_tracker);

  public static OperatorConvexHull local() {
    return (OperatorConvexHull) OperatorFactoryLocal.getInstance().getOperator(Type.ConvexHull);
  }
}
