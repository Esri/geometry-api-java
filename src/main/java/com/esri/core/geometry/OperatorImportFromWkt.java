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

public abstract class OperatorImportFromWkt extends Operator {
	@Override
	public Type getType() {
		return Type.ImportFromWkb;
	}

	/**
	 * Performs the ImportFromWkt operation.
	 * @param import_flags Use the {@link WktImportFlags} interface.
	 * @param type Use the {@link Geometry.Type} enum. 
	 * @param wkt_string The string holding the Geometry in wkt format.
	 * @return Returns the imported Geometry.
	 */
	public abstract Geometry execute(int import_flags, Geometry.Type type,
			String wkt_string, ProgressTracker progress_tracker);

	/**
	 * Performs the ImportFromWkt operation.
	 * @param import_flags Use the {@link WktImportFlags} interface.
	 * @param wkt_string The string holding the Geometry in wkt format.
	 * @return Returns the imported OGCStructure.
	 */
	public abstract OGCStructure executeOGC(int import_flags,
			String wkt_string, ProgressTracker progress_tracker);

	public static OperatorImportFromWkt local() {
		return (OperatorImportFromWkt) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ImportFromWkt);
	}

}
