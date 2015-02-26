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

import com.esri.core.geometry.Operator.Type;

/**
 *Export to ESRI shape format.
 */
public abstract class OperatorExportToESRIShape extends Operator {
	@Override
	public Type getType() {
		return Type.ExportToESRIShape;
	}

	/**
	 * Performs the ExportToESRIShape operation
	 * 
	 * @return Returns a ByteBufferCursor.
	 */
	abstract ByteBufferCursor execute(int exportFlags,
			GeometryCursor geometryCursor);

	/**
	 * Performs the ExportToESRIShape operation.
	 * @param exportFlags Use the {@link ShapeExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @return Returns a ByteBuffer object containing the Geometry in ESRIShape format.
	 */
	public abstract ByteBuffer execute(int exportFlags, Geometry geometry);

	/**
	 * Performs the ExportToESRIShape operation.
	 * @param exportFlags Use the {@link ShapeExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param shapeBuffer The ByteBuffer to contain the exported Geometry in ESRIShape format.
	 * @return If the input buffer is null, then the size needed for the buffer is returned. Otherwise the number of bytes written to the buffer is returned.
	 */
	public abstract int execute(int exportFlags, Geometry geometry,
			ByteBuffer shapeBuffer);
	
	public static OperatorExportToESRIShape local() {
		return (OperatorExportToESRIShape) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ExportToESRIShape);
	}	
}
