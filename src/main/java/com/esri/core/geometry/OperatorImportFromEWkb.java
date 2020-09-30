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
 *Import from Extended WKB format.
 */
public abstract class OperatorImportFromEWkb extends Operator {
	SpatialReference m_spatialReference = null;

	@Override
	public Type getType() {
		return Type.ImportFromEWkb;
	}

	public abstract SpatialReference getSpatialReference();

	/**
	 * Performs the ImportFromWKB operation.
	 * @param importFlags Use the {@link WkbImportFlags} interface.
	 * @param type Use the {@link Geometry.Type} enum. 
	 * @param eWkbBuffer The buffer holding the Geometry in extended wkb format.
	 * @return Returns the imported Geometry.
	 */
	public abstract Geometry execute(int importFlags,
	                                 Geometry.Type type,
	                                 ByteBuffer eWkbBuffer,
	                                 ProgressTracker progress_tracker);

	public static OperatorImportFromEWkb local() {
		return (OperatorImportFromEWkb) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ImportFromEWkb);
	}

}
