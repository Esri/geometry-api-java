/*
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
 *Export to GeoJson format.
 */
public abstract class OperatorExportToGeoJson extends Operator {
	@Override
	public Type getType() {
		return Type.ExportToGeoJson;
	}

	/**
	 * Performs the ExportToGeoJson operation
	 * @param spatialReference The SpatialReference of the Geometry. Will be written as "crs":null if the spatialReference is null.
	 * @param geometryCursor The cursor of geometries to write as GeoJson.
	 * @return Returns a JsonCursor.
	 */
	public abstract JsonCursor execute(SpatialReference spatialReference, GeometryCursor geometryCursor);

	/**
	 * Performs the ExportToGeoJson operation
	 * @param spatialReference The SpatialReference of the Geometry. Will be written as "crs":null if the spatialReference is null.
	 * @param geometry The Geometry to write as GeoJson.
	 * @return Returns a string in GeoJson format.
	 */
	public abstract String execute(SpatialReference spatialReference, Geometry geometry);

	/**
	 * Performs the ExportToGeoJson operation
	 * @param exportFlags Use the {@link GeoJsonExportFlags} interface.
	 * @param spatialReference The SpatialReference of the Geometry. Will be written as "crs":null if the spatialReference is null.
	 * @param geometry The Geometry to write as GeoJson.
	 * @return Returns a string in GeoJson format.
	 */
	public abstract String execute(int exportFlags, SpatialReference spatialReference, Geometry geometry);

	/**
	 * Performs the ExportToGeoJson operation. Will not write out a spatial reference or crs tag. Assumes the geometry is in wgs84.
	 * @param geometry The Geometry to write as GeoJson.
	 * @return Returns a string in GeoJson format.
	 */
	public abstract String execute(Geometry geometry);

	/**
	 * Performs the ExportToGeoJson operation on a spatial reference.
	 *
	 * @param export_flags      The flags used for the export.
	 * @param spatial_reference The spatial reference being exported. Cannot be null.
	 * @return Returns the crs value object.
	 */
	public abstract String exportSpatialReference(int export_flags, SpatialReference spatial_reference);

	public static OperatorExportToGeoJson local() {
		return (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance().getOperator(Type.ExportToGeoJson);
	}
}
