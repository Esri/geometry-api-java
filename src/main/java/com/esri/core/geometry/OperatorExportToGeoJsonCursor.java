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
    boolean m_preferMulti = false;

    private static JsonFactory factory = new JsonFactory();

    public OperatorExportToGeoJsonCursor(boolean preferMulti, SpatialReference spatialReference, GeometryCursor geometryCursor) {
        m_index = -1;
        if (geometryCursor == null)
            throw new IllegalArgumentException();
        if (spatialReference != null && !spatialReference.isLocal()) {
            m_wkid = spatialReference.getOldID();
            m_wkt = spatialReference.getText();
            m_latest_wkid = spatialReference.getLatestID();
        }
        m_inputGeometryCursor = geometryCursor;
        m_preferMulti = preferMulti;
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
            	exportMultiPathToGeoJson(g, (Polyline) geometry);
                break;
            case Geometry.GeometryType.Polygon:
            	exportMultiPathToGeoJson(g, (Polygon) geometry);
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
    	if (m_preferMulti) {
    		MultiPoint mp = new MultiPoint();
    		mp.add(p);
    		exportMultiPointToGeoJson(g, mp);
    		return;
    	}
    	
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

    private void exportMultiPathToGeoJson(JsonGenerator g, MultiPath p) throws JsonGenerationException, IOException {
        MultiPathImpl pImpl = (MultiPathImpl) p._getImpl();

        boolean isPolygon = pImpl.m_bPolygon;
        int polyCount = isPolygon ? pImpl.getOGCPolygonCount() : 0;

        // check yo' polys playa

        g.writeStartObject();

        g.writeFieldName("type");

        boolean bCollection = false;
        if (isPolygon) {
	        if (polyCount >= 2 || m_preferMulti) { // single polys seem to have a polyCount of 0, multi polys seem to be >= 2
	            g.writeString("MultiPolygon");
	            bCollection = true;
	        } else {
	            g.writeString("Polygon");
	        }
        }
        else {
	        if (p.getPathCount() > 1 || m_preferMulti) { // single polys seem to have a polyCount of 0, multi polys seem to be >= 2
	            g.writeString("MultiLineString");
	            bCollection = true;
	        } else {
	            g.writeString("LineString");
	        }
        }

        g.writeFieldName("coordinates");

        if (p.isEmpty()) {
            g.writeNull();
        } else {
            exportMultiPathToGeoJson(g, pImpl, bCollection);
        }

        g.writeEndObject();
        g.close();
    }

    private void exportMultiPathToGeoJson(JsonGenerator g, MultiPathImpl pImpl, boolean bCollection) throws IOException {
        int startIndex;
        int vertices;

        if (bCollection)
        	g.writeStartArray();
        
        //AttributeStreamOfDbl position = (AttributeStreamOfDbl) pImpl.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
        //AttributeStreamOfInt8 pathFlags = pImpl.getPathFlagsStreamRef();
        //AttributeStreamOfInt32 paths = pImpl.getPathStreamRef();
        int pathCount = pImpl.getPathCount();
        boolean isPolygon = pImpl.m_bPolygon;
        AttributeStreamOfDbl zs = pImpl.hasAttribute(Semantics.Z) ? (AttributeStreamOfDbl) pImpl.getAttributeStreamRef(Semantics.Z) : null;

        for (int path = 0; path < pathCount; path++) {
            startIndex = pImpl.getPathStart(path);
            vertices = pImpl.getPathSize(path);

            boolean isExtRing = isPolygon && pImpl.isExteriorRing(path);
            if (isExtRing) {//only for polygons
                if (path > 0)
                	g.writeEndArray();//end of OGC polygon
                
                g.writeStartArray();//start of next OGC polygon
            }

            writePath(pImpl, g, path, startIndex, vertices, zs);
        }
        
        if (isPolygon)
        	g.writeEndArray();//end of last OGC polygon
        
        if (bCollection)
        	g.writeEndArray();
    }

    private void closePath(MultiPathImpl mp, JsonGenerator g, int startIndex, AttributeStreamOfDbl zs) throws IOException {
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

    private void writePath(MultiPathImpl mp, JsonGenerator g, int pathIndex, int startIndex, int vertices, AttributeStreamOfDbl zs) throws IOException {
        Point2D pt = new Point2D();

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

        if (mp.isClosedPath(pathIndex))
            closePath(mp, g, startIndex, zs);

        g.writeEndArray();
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
