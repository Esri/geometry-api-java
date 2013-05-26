package com.esri.core.geometry;

class OperatorExportToGeoJsonLocal extends OperatorExportToGeoJson {
    @Override
    JsonCursor execute(SpatialReference spatialReference, GeometryCursor geometryCursor) {
        return new OperatorExportToGeoJsonCursor(spatialReference, geometryCursor);
    }

    @Override
    public String execute(SpatialReference spatialReference, Geometry geometry) {
        SimpleGeometryCursor gc = new SimpleGeometryCursor(geometry);
        JsonCursor cursor = new OperatorExportToGeoJsonCursor(spatialReference, gc);
        return cursor.next();
    }

    @Override
    public String execute(Geometry geometry) {
        SimpleGeometryCursor gc = new SimpleGeometryCursor(geometry);
        JsonCursor cursor = new OperatorExportToGeoJsonCursor(gc);
        return cursor.next();
    }
}
