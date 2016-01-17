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
 * Generalizes geometries using Douglas-Peucker algorithm.
 */
public abstract class OperatorGeneralize extends Operator {
  @Override
  public Type getType() {
    return Type.Generalize;
  }

  /**
   * Performs the Generalize operation on a geometry set. Point and
   * multipoint geometries are left unchanged. An envelope is converted to a
   * polygon.
   *
   * @param  geoms
   *         the geometry set to generalize
   *
   * @param  maxDeviation
   *         The max deviation
   *
   * @param  bRemoveDegenerateParts
   *         {@code true} if degenerate parts should be removed
   *
   * @param  progressTracker
   *         ProgressTracker instance that is used to cancel the lengthy operation. Can be null.
   *
   * @return a cursor the resulting geometry set
   */
  public abstract GeometryCursor execute(GeometryCursor geoms,
          double maxDeviation, boolean bRemoveDegenerateParts,
          ProgressTracker progressTracker);

  /**
   * Performs the Generalize operation on a single geometry. Point and
   * multipoint geometries are left unchanged. An envelope is converted to a
   * polygon.
   *
   * @param  geom
   *         the geometry to generalize
   *
   * @param  maxDeviation
   *         The max deviation
   *
   * @param  bRemoveDegenerateParts
   *         {@code true} if degenerate parts should be removed
   *
   * @param  progressTracker
   *         ProgressTracker instance that is used to cancel the lengthy operation. Can be null.
   *
   * @return the resulting geometry
   */
  public abstract Geometry execute(Geometry geom, double maxDeviation,
          boolean bRemoveDegenerateParts, ProgressTracker progressTracker);

  public static OperatorGeneralize local() {
    return (OperatorGeneralize) OperatorFactoryLocal.getInstance().getOperator(Type.Generalize);
  }

}
