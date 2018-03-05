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
 * Densifies MultiPath geometries by length so that no segments are longer than
 * given threshold value.
 */
public abstract class OperatorDensifyByLength extends Operator {
	@Override
	public Type getType() {
		return Type.DensifyByLength;
	}

	/**
	 * Performs the Densify operation on the geometry set.
	 * 
	 * @param inputGeometries
	 *            The geometries to be densified.
	 * @param maxLength
	 *            The maximum segment length allowed. Must be a positive value.
	 *            Curves are densified to straight segments using the
	 *            maxSegmentLength. Curves are split into shorter subcurves such
	 *            that the length of subcurves is shorter than maxSegmentLength.
	 *            After that the curves are replaced with straight segments.
	 * @param progressTracker
	 * @return Returns the densified geometries (It does nothing to geometries
	 *         with dim &lt; 1, but simply passes them along).
	 */
	public abstract GeometryCursor execute(GeometryCursor inputGeometries,
			double maxLength, ProgressTracker progressTracker);

	/**
	 * Performs the Densify operation on the geometry set.
	 * 
	 * @param inputGeometry
	 *            The geometry to be densified.
	 * @param maxLength
	 *            The maximum segment length allowed. Must be a positive value.
	 *            Curves are densified to straight segments using the
	 *            maxSegmentLength. Curves are split into shorter subcurves such
	 *            that the length of subcurves is shorter than maxSegmentLength.
	 *            After that the curves are replaced with straight segments.
	 * @param progressTracker
	 * @return Returns the densified geometry. (It does nothing to geometries
	 *         with dim &lt; 1, but simply passes them along).
	 */
	public abstract Geometry execute(Geometry inputGeometry, double maxLength,
			ProgressTracker progressTracker);

	public static OperatorDensifyByLength local() {
		return (OperatorDensifyByLength) OperatorFactoryLocal.getInstance()
				.getOperator(Type.DensifyByLength);
	}

}
