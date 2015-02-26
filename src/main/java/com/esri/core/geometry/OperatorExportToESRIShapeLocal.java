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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * OperatorExportToESRIShape implementation.
 */
class OperatorExportToESRIShapeLocal extends OperatorExportToESRIShape {

	@Override
	ByteBufferCursor execute(int exportFlags, GeometryCursor geometryCursor) {
		return new OperatorExportToESRIShapeCursor(exportFlags, geometryCursor);
	}

	@Override
	public ByteBuffer execute(int exportFlags, Geometry geometry) {
		ByteBuffer shapeBuffer = null;
		int size = OperatorExportToESRIShapeCursor.exportToESRIShape(
				exportFlags, geometry, shapeBuffer);
		shapeBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		OperatorExportToESRIShapeCursor.exportToESRIShape(exportFlags,
				geometry, shapeBuffer);
		return shapeBuffer;
	}

	@Override
	public int execute(int exportFlags, Geometry geometry,
			ByteBuffer shapeBuffer) {
		shapeBuffer.order(ByteOrder.LITTLE_ENDIAN);
		return OperatorExportToESRIShapeCursor.exportToESRIShape(exportFlags,
				geometry, shapeBuffer);
	}
}
