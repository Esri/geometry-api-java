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

public abstract class OperatorBoundary extends Operator {
  @Override
  public Type getType() {
    return Type.Boundary;
  }

  /**
   * Calculates the boundary geometry.
   * @param geoms The input geometry cursor.
   * @param progress_tracker The progress tracker, that allows to cancel the lengthy operation.
   * @return Returns a cursor over boundaries for each geometry.
   */
  abstract public GeometryCursor execute(GeometryCursor geoms,
          ProgressTracker progress_tracker);

  /**
   * Calculates the boundary.
   * @param geom The input geometry.
   * @param progress_tracker The progress tracker, that allows to cancel the lengthy operation.
   * @return Returns the boundary.
   *
   * For Point - returns an empty point.
   * For Multi_point - returns an empty point.
   * For Envelope - returns a polyline, that bounds the envelope.
   * For Polyline - returns a multipoint, using OGC specification (includes path endpoints, using mod 2 rule).
   * For Polygon - returns a polyline that bounds the polygon (adds all rings of the polygon to a polyline).
   */
  abstract public Geometry execute(Geometry geom,
          ProgressTracker progress_tracker);

  public static OperatorBoundary local() {
    return (OperatorBoundary) OperatorFactoryLocal.getInstance().getOperator(Type.Boundary);
  }
}
