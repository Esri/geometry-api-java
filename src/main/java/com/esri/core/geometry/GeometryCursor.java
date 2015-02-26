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
 *An abstract Geometry Cursor class.
 */
public abstract class GeometryCursor {
	/**
	 *Moves the cursor to the next Geometry. Returns null when reached the end.
	 *The behavior of the cursor is undefined after the method returns null.
	 */
	public abstract Geometry next();

	/**
	 *Returns the ID of the current geometry. The ID is propagated across the operations (when possible).
	 *
	 *Returns an ID associated with the current Geometry. The ID is passed along and is returned by some operators to preserve relationship between the input and output geometry classes.
	 *It is not always possible to preserve an ID during an operation.
	 */
	public abstract int getGeometryID();
	/**
	 *Executes a unit of work on the cursor.
	 *@return Returns true, if there is a geometry ready to be pulled using next().
	 *
	 *This method is to be used together with the tick() method on the ListeningGeometryCursor.
	 *Call tock() for each tick() on the ListeningGeometryCursor.
	 */
	public boolean tock() { return true; }
}
