package com.esri.core.geometry;

import com.esri.core.geometry.VertexDescription.Semantics;
import java.io.IOException;
import java.io.StringWriter;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

class OperatorExportToGeoJsonCursor extends JsonCursor {
    GeometryCursor m_inputGeometryCursor;
    int m_index;
    int m_wkid = -1;
    int m_latest_wkid = -1;
    String m_wkt = null;

    private static JsonFactory factory = new JsonFactory();

    public OperatorExportToGeoJsonCursor(SpatialReference spatialReference, GeometryCursor geometryCursor) {
        m_index = -1;
        if (geometryCursor == null)
            throw new IllegalArgumentException();
        if (spatialReference != null && !spatialReference.isLocal()) {
            m_wkid = spatialReference.getOldID();
            m_wkt = spatialReference.getText();
            m_latest_wkid = spatialReference.getLatestID();
        }
        m_inputGeometryCursor = geometryCursor;
    }

    public OperatorExportToGeoJsonCursor(GeometryCursor geometryCursor) {
        m_index = -1;

        if (geometryCursor == null)
            throw new IllegalArgumentException();

        m_inputGeometryCursor = geometryCursor;
    }

    @Override
    public int getID() {
        return m_index;
    }

    @Override
    public String next() {
        Geometry geometry;
        if ((geometry = m_inputGeometryCursor.next()) != null) {
            m_index = m_inputGeometryCursor.getGeometryID();
            return exportToGeoJson(geometry);
        }
        return null;
    }

    private String exportToGeoJson(Geometry geometry) {
        StringWriter sw = new StringWriter();

        try {
            JsonGenerator g = factory.createJsonGenerator(sw);

            int type = geometry.getType().value();

            switch (type) {
            case Geometry.GeometryType.Point:
                exportPointToGeoJson(g, (Point) geometry);
                break;
            case Geometry.GeometryType.MultiPoint:
                exportMultiPointToGeoJson(g, (MultiPoint) geometry);
                break;
            case Geometry.GeometryType.Polyline:
                exportPolylineToGeoJson(g, (Polyline) geometry);
                break;
            case Geometry.GeometryType.Polygon:
                exportPolygonToGeoJson(g, (Polygon) geometry);
                break;
            case Geometry.GeometryType.Envelope:
                exportEnvelopeToGeoJson(g, (Envelope) geometry);
                break;
            default:
                throw new RuntimeException("not implemented for this geometry type");
            }

            return sw.getBuffer().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void exportPointToGeoJson(JsonGenerator g, Point p) throws JsonGenerationException, IOException {
        g.writeStartObject();

        g.writeFieldName("type");
        g.writeString("Point");

        g.writeFieldName("coordinates");

        if (p.isEmpty()) {
            g.writeStartArray();
            g.writeEndArray();
        } else {
            g.writeStartArray();
            writeDouble(p.getX(), g);
            writeDouble(p.getY(), g);

            if (p.hasAttribute(Semantics.Z))
                writeDouble(p.getZ(), g);

            g.writeEndArray();
        }

        g.writeEndObject();
        g.close();
    }

    private void exportMultiPointToGeoJson(JsonGenerator g, MultiPoint mp) throws JsonGenerationException, IOException {
        g.writeStartObject();

        g.writeFieldName("type");
        g.writeString("MultiPoint");

        g.writeFieldName("coordinates");
        g.writeStartArray();

        if (!mp.isEmpty()) {
            MultiPointImpl mpImpl = (MultiPointImpl) mp._getImpl();
            AttributeStreamOfDbl zs = mp.hasAttribute(Semantics.Z) ? (AttributeStreamOfDbl) mpImpl.getAttributeStreamRef(Semantics.Z) : null;

            Point2D p = new Point2D();

            int n = mp.getPointCount();

            for(int i = 0; i < n; i++) {
                mp.getXY(i, p);

                g.writeStartArray();

                writeDouble(p.x, g);
                writeDouble(p.y, g);

                if (zs != null)
                    writeDouble(zs.get(i), g);

                g.writeEndArray();
            }
        }

        g.writeEndArray();

        g.writeEndObject();
        g.close();
    }

    private void exportPolylineToGeoJson(JsonGenerator g, Polyline p) throws JsonGenerationException, IOException {
        g.writeStartObject();

        g.writeFieldName("type");
        g.writeString("LineString");

        g.writeFieldName("coordinates");

        if (p.isEmpty()) {
            g.writeStartArray();
            g.writeEndArray();
        }

        g.writeEndObject();
        g.close();
    }

    private void exportPolygonToGeoJson(JsonGenerator g, Polygon p) throws JsonGenerationException, IOException {
        g.writeStartObject();

        g.writeFieldName("type");
        g.writeString("Polygon");

        g.writeFieldName("coordinates");

        if (p.isEmpty()) {
            g.writeStartArray();
            g.writeEndArray();
        }

        g.writeEndObject();
        g.close();
    }

    private void exportEnvelopeToGeoJson(JsonGenerator g, Envelope e) throws JsonGenerationException, IOException {
        boolean empty = e.isEmpty();

        g.writeStartObject();

        g.writeFieldName("bbox");
        g.writeStartArray();

        if (!empty) {}

        g.writeEndArray();

        g.writeFieldName("type");
        g.writeString("Polygon");

        g.writeFieldName("coordinates");
        g.writeStartArray();

        if (!empty) {}

        g.writeEndArray();

        g.writeEndObject();
        g.close();
    }

    private void writeDouble(double d, JsonGenerator g) throws IOException, JsonGenerationException {
        if (NumberUtils.isNaN(d)) {
            g.writeNull();
        } else {
            g.writeNumber(d);
        }

        return;
    }
}
