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

class OperatorSimplifyLocal extends OperatorSimplify {

	// Reviewed vs. Feb 8 2011
	@Override
	public GeometryCursor execute(GeometryCursor geoms,
			SpatialReference spatialRef, boolean bForceSimplify,
			ProgressTracker progressTracker) {
		return new OperatorSimplifyCursor(geoms, spatialRef, bForceSimplify,
				progressTracker);
	}

	// Reviewed vs. Feb 8 2011
	@Override
	public boolean isSimpleAsFeature(Geometry geom,
			SpatialReference spatialRef, boolean bForceTest,
			NonSimpleResult result, ProgressTracker progressTracker) {
		int res = OperatorSimplifyLocalHelper.isSimpleAsFeature(geom,
				spatialRef, bForceTest, result, progressTracker);
		return res > 0;
	}

	// Reviewed vs. Feb 8 2011
	@Override
	public Geometry execute(Geometry geom, SpatialReference spatialRef,
			boolean bForceSimplify, ProgressTracker progressTracker) {
		SimpleGeometryCursor inputCursor = new SimpleGeometryCursor(geom);
		GeometryCursor outputCursor = execute(inputCursor, spatialRef,
				bForceSimplify, progressTracker);

		return outputCursor.next();
	}
}
