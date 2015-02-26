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
 *Import from ESRI shape format.
 */
public abstract class OperatorImportFromESRIShape extends Operator {
	@Override
	public Type getType() {
		return Type.ImportFromESRIShape;
	}

	/**
	 * Performs the ImportFromESRIShape operation on a stream of shape buffers
	 * @param importFlags Use the {@link ShapeImportFlags} interface. The default is 0, which means geometry comes from a trusted source and is topologically simple.
	 * If the geometry comes from non-trusted source (that is it can be non-simple), pass ShapeImportNonTrusted.
	 * @param type The geometry type that you want to import. Use the {@link Geometry.Type} enum. It can be Geometry.Type.Unknown if the type of geometry has to be
	 * figured out from the shape buffer.
	 * @param shapeBuffers The cursor over shape buffers that hold the Geometries in ESRIShape format.
	 * @return Returns a GeometryCursor.
	 */
	abstract GeometryCursor execute(int importFlags, Geometry.Type type,
			ByteBufferCursor shapeBuffers);

	/**
	 * Performs the ImportFromESRIShape operation.
	 * @param importFlags Use the {@link ShapeImportFlags} interface. The default is 0, which means geometry comes from a trusted source and is topologically simple.
	 * If the geometry comes from non-trusted source (that is it can be non-simple), pass ShapeImportNonTrusted.
	 * @param type The geometry type that you want to import. Use the {@link Geometry.Type} enum. It can be Geometry.Type.Unknown if the type of geometry has to be
	 * figured out from the shape buffer.
	 * @param shapeBuffer The buffer holding the Geometry in ESRIShape format.
	 * @return Returns the imported Geometry.
	 */
	public abstract Geometry execute(int importFlags, Geometry.Type type,
			ByteBuffer shapeBuffer);

	public static OperatorImportFromESRIShape local() {
		return (OperatorImportFromESRIShape) OperatorFactoryLocal.getInstance()
				.getOperator(Type.ImportFromESRIShape);
	}

}
