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
            g.writeNull();
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

        if (mp.isEmpty()) {
            g.writeNull();
        } else {
            g.writeStartArray();

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

            g.writeEndArray();
        }

        g.writeEndObject();
        g.close();
    }

    private void exportPolylineToGeoJson(JsonGenerator g, Polyline p) throws JsonGenerationException, IOException {
        g.writeStartObject();

        g.writeFieldName("type");
        g.writeString("LineString");

        g.writeFieldName("coordinates");

        if (p.isEmpty()) {
            g.writeNull();
        } else {
            g.writeStartArray();
            exportPathToGeoJson(g, p);
            g.writeEndArray();
        }

        g.writeEndObject();
        g.close();
    }

    private void exportPolygonToGeoJson(JsonGenerator g, Polygon p) throws JsonGenerationException, IOException {
        MultiPathImpl pImpl = (MultiPathImpl) p._getImpl();

        //int pointCount = pImpl.getPointCount();
        int polyCount = pImpl.getOGCPolygonCount();

        // check yo' polys playa

        g.writeStartObject();

        g.writeFieldName("type");

        if (polyCount >= 2) { // single polys seem to have a polyCount of 0, multi polys seem to be >= 2
            g.writeString("MultiPolygon");
        } else {
            g.writeString("Polygon");
        }

        g.writeFieldName("coordinates");

        if (p.isEmpty()) {
            g.writeNull();
        } else {
            g.writeStartArray();

            if (polyCount >= 2) {
                g.writeStartArray();
                exportMultiPolygonToGeoJson(g, p, pImpl);
                g.writeEndArray();
            } else {
                exportPathToGeoJson(g, p);
            }

            g.writeEndArray();
        }

        g.writeEndObject();
        g.close();
    }

    private void exportMultiPolygonToGeoJson(JsonGenerator g, Polygon p, MultiPathImpl pImpl) throws IOException {
        int startIndex;
        int vertices;

        //AttributeStreamOfDbl position = (AttributeStreamOfDbl) pImpl.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
        //AttributeStreamOfInt8 pathFlags = pImpl.getPathFlagsStreamRef();
        //AttributeStreamOfInt32 paths = pImpl.getPathStreamRef();
        int pathCount = pImpl.getPathCount();

        AttributeStreamOfDbl zs = p.hasAttribute(Semantics.Z) ? (AttributeStreamOfDbl) pImpl.getAttributeStreamRef(Semantics.Z) : null;

        Point2D pt = new Point2D();

        g.writeStartArray();

        startIndex = p.getPathStart(0);
        vertices = p.getPathSize(0);

        for (int i = startIndex; i < startIndex + vertices; i++) {
            p.getXY(i, pt);
            g.writeStartArray();
            writeDouble(pt.x, g);
            writeDouble(pt.y, g);

            if (zs != null)
                writeDouble(zs.get(i), g);

            g.writeEndArray();
        }

        p.getXY(startIndex, pt);
        g.writeStartArray();
        writeDouble(pt.x, g);
        writeDouble(pt.y, g);

        if (zs != null)
            writeDouble(zs.get(startIndex), g);

        g.writeEndArray();

        g.writeEndArray();

        for (int path = 1; path < pathCount; path++) {
            boolean isExtRing = p.isExteriorRing(path);
            startIndex = p.getPathStart(path);
            vertices = p.getPathSize(path);

            writePath(p, g, startIndex, vertices, zs, isExtRing);
        }
    }

    private void closePolygon(MultiPath mp, JsonGenerator g, int startIndex, AttributeStreamOfDbl zs) throws IOException {
        Point2D pt = new Point2D();

        // close ring
        mp.getXY(startIndex, pt);
        g.writeStartArray();
        writeDouble(pt.x, g);
        writeDouble(pt.y, g);

        if (zs != null)
            writeDouble(zs.get(startIndex), g);

        g.writeEndArray();
    }

    private void writePath(MultiPath mp, JsonGenerator g, int startIndex, int vertices, AttributeStreamOfDbl zs, boolean isExtRing) throws IOException {
        Point2D pt = new Point2D();

        boolean isPoly = mp instanceof Polygon;

        if (isPoly && isExtRing) {
            g.writeEndArray();
            g.writeStartArray();
        }

        g.writeStartArray();

        for (int i = startIndex; i < startIndex + vertices; i++) {
            mp.getXY(i, pt);
            g.writeStartArray();
            writeDouble(pt.x, g);
            writeDouble(pt.y, g);

            if (zs != null)
                writeDouble(zs.get(i), g);

            g.writeEndArray();
        }

        if (isPoly)
            closePolygon(mp, g, startIndex, zs);

        g.writeEndArray();
    }

    private void exportPathToGeoJson(JsonGenerator g, MultiPath mp) throws JsonGenerationException, IOException {
        boolean isPoly = mp instanceof Polygon;

        MultiPathImpl mpImpl = (MultiPathImpl) mp._getImpl();
        AttributeStreamOfDbl zs = mp.hasAttribute(Semantics.Z) ? (AttributeStreamOfDbl) mpImpl.getAttributeStreamRef(Semantics.Z) : null;

        Point2D p = new Point2D();

        int n = mp.getPathCount();

        for (int i = 0; i < n; i++) {
            if (isPoly)
                g.writeStartArray();

            int startIndex = mp.getPathStart(i);
            int vertices = mp.getPathSize(i);

            for (int j = startIndex; j < startIndex + vertices; j++) {
                mp.getXY(j, p);

                g.writeStartArray();

                writeDouble(p.x, g);
                writeDouble(p.y, g);

                if (zs != null)
                    writeDouble(zs.get(j), g);

                g.writeEndArray();
            }

            if (isPoly) {
                mp.getXY(startIndex, p);

                g.writeStartArray();

                writeDouble(p.x, g);
                writeDouble(p.y, g);

                if (zs != null)
                    writeDouble(zs.get(startIndex), g);

                g.writeEndArray();
            }

            if (isPoly)
                g.writeEndArray();
        }
    }

    private void exportEnvelopeToGeoJson(JsonGenerator g, Envelope e) throws JsonGenerationException, IOException {
        boolean empty = e.isEmpty();

        g.writeStartObject();
        g.writeFieldName("bbox");

        if (empty) {
            g.writeNull();
        } else {
            g.writeStartArray();
            writeDouble(e.getXMin(), g);
            writeDouble(e.getYMin(), g);
            writeDouble(e.getXMax(), g);
            writeDouble(e.getYMax(), g);
            g.writeEndArray();
        }

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
