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
 * Splits the target polyline or polygon where it is crossed by the cutter polyline.
 */
public abstract class OperatorCut extends Operator {
  @Override
  public Type getType() {
    return Type.Cut;
  }

  /**
   * Performs the Cut operation on a geometry. 
   * @param bConsiderTouch Indicates whether we consider a touch event a cut. 
   * This only applies to polylines, but it's recommended to set this variable to True. 
   * @param cuttee The input geometry to be cut. 
   * @param cutter The polyline that will be used to divide the cuttee into 
   * pieces where it crosses the cutter. 
   * @return Returns a GeometryCursor of cut geometries. 
   * All left cuts will be grouped together in the first geometry. Right cuts and 
   * coincident cuts are grouped in the second geometry, and each undefined cut along
   * with any uncut parts are output as separate geometries. If there were no cuts
   * the cursor will return no geometry. If the left or right cut does not
   * exist, the returned geometry will be empty for this type of cut. An
   * undefined cut will only be produced if a left cut or right cut was
   * produced and there was a part left over after cutting or a cut is
   * bounded to the left and right of the cutter.
   */
  public abstract GeometryCursor execute(boolean bConsiderTouch,
          Geometry cuttee, Polyline cutter, SpatialReference spatialReference,
          ProgressTracker progressTracker);

  public static OperatorCut local() {
    return (OperatorCut) OperatorFactoryLocal.getInstance().getOperator(Type.Cut);
  }
}
