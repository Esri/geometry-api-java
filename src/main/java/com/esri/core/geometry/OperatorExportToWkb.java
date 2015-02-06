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
 *Export to WKB format.
 */
public abstract class OperatorExportToWkb extends Operator {
	@Override
	public Type getType() {
		return Type.ExportToWkb;
	}

	/**
	 * Performs the ExportToWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @return Returns a ByteBuffer object containing the Geometry in WKB format
	 */
	public abstract ByteBuffer execute(int exportFlags, Geometry geometry,
			ProgressTracker progressTracker);

	/**
	 * Performs the ExportToWKB operation.
	 * @param exportFlags Use the {@link WkbExportFlags} interface.
	 * @param geometry The Geometry being exported.
	 * @param wkbBuffer The ByteBuffer to contain the exported Geometry in WKB format.
	 * @return If the input buffer is null, then the size needed for the buffer is returned. Otherwise the number of bytes written to the buffer is returned.
	 */
	public abstract int execute(int exportFlags, Geometry geometry,
			ByteBuffer wkbBuffer, ProgressTracker progressTracker);

	public static OperatorExportToWkb local() {
		return (OperatorExportToWkb) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ExportToWkb);
	}

}
