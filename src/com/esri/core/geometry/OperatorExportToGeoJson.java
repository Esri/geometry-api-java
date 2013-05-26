package com.esri.core.geometry;

import com.esri.core.geometry.Operator.Type;

public abstract class OperatorExportToGeoJson extends Operator {
    @Override
    public Type getType() {
        return Type.ExportToGeoJson;
    }

    abstract JsonCursor execute(SpatialReference spatialReference, GeometryCursor geometryCursor);

    public abstract String execute(SpatialReference spatialReference, Geometry geometry);

    public abstract String execute(Geometry geometry);

    public static OperatorExportToGeoJson local() {
        return (OperatorExportToGeoJson) OperatorFactoryLocal.getInstance()
                .getOperator(Type.ExportToGeoJson);
    }
}
