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

import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPoint;
import com.esri.core.geometry.ogc.OGCMultiPoint;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;

public class TestGeomToGeoJson extends TestCase {
    OperatorFactoryLocal factory = OperatorFactoryLocal.getInstance();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testPoint() {
        Point p = new Point(10.0, 20.0);
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[10.0,20.0]}", result);
    }

    @Test
    public void testEmptyPoint() {
        Point p = new Point();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Point\",\"coordinates\":null}", result);
    }

    @Test
    public void testPointGeometryEngine() {
        Point p = new Point(10.0, 20.0);
        String result = GeometryEngine.geometryToGeoJson(p);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[10.0,20.0]}", result);
    }

    @Test
    public void testOGCPoint() {
        Point p = new Point(10.0, 20.0);
        OGCGeometry ogcPoint = new OGCPoint(p, null);
        String result = ogcPoint.asGeoJson();
        assertEquals("{\"type\":\"Point\",\"coordinates\":[10.0,20.0]}", result);
    }

    @Test
    public void testMultiPoint() {
        MultiPoint mp = new MultiPoint();
        mp.add(10.0, 20.0);
        mp.add(20.0, 30.0);
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(mp);
        assertEquals("{\"type\":\"MultiPoint\",\"coordinates\":[[10.0,20.0],[20.0,30.0]]}", result);
    }

    @Test
    public void testEmptyMultiPoint() {
        MultiPoint mp = new MultiPoint();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(mp);
        assertEquals("{\"type\":\"MultiPoint\",\"coordinates\":null}", result);
    }

    @Test
    public void testMultiPointGeometryEngine() {
        MultiPoint mp = new MultiPoint();
        mp.add(10.0, 20.0);
        mp.add(20.0, 30.0);
        String result = GeometryEngine.geometryToGeoJson(mp);
        assertEquals("{\"type\":\"MultiPoint\",\"coordinates\":[[10.0,20.0],[20.0,30.0]]}", result);
    }

    @Test
    public void testOGCMultiPoint() {
        MultiPoint mp = new MultiPoint();
        mp.add(10.0, 20.0);
        mp.add(20.0, 30.0);
        OGCMultiPoint ogcMultiPoint = new OGCMultiPoint(mp, null);
        String result = ogcMultiPoint.asGeoJson();
        assertEquals("{\"type\":\"MultiPoint\",\"coordinates\":[[10.0,20.0],[20.0,30.0]]}", result);
    }

    @Test
    public void testPolyline() {
        Polyline p = new Polyline();
        p.startPath(100.0, 0.0);
        p.lineTo(101.0, 0.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(100.0, 1.0);
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0]]}", result);
    }

    @Test
    public void testEmptyPolyline() {
        Polyline p = new Polyline();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"LineString\",\"coordinates\":null}", result);
    }

    @Test
    public void testPolylineGeometryEngine() {
        Polyline p = new Polyline();
        p.startPath(100.0, 0.0);
        p.lineTo(101.0, 0.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(100.0, 1.0);
        String result = GeometryEngine.geometryToGeoJson(p);
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0]]}", result);
    }

    @Test
    public void testOGCLineString() {
        Polyline p = new Polyline();
        p.startPath(100.0, 0.0);
        p.lineTo(101.0, 0.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(100.0, 1.0);
        OGCLineString ogcLineString = new OGCLineString(p, 0, null);
        String result = ogcLineString.asGeoJson();
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0]]}", result);
    }

    @Test
    public void testPolygon() {
        Polygon p = new Polygon();
        p.startPath(100.0, 0.0);
        p.lineTo(101.0, 0.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(100.0, 1.0);
        p.closePathWithLine();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]}", result);
    }

    @Test
    public void testPolygonWithHole() {
        Polygon p = new Polygon();
        
        //exterior ring - has to be clockwise for Esri
        p.startPath(100.0, 0.0);
        p.lineTo(100.0, 1.0);        
        p.lineTo(101.0, 1.0);        
        p.lineTo(101.0, 0.0);
        p.closePathWithLine();

        //hole - counterclockwise for Esri
        p.startPath(100.2, 0.2);
        p.lineTo(100.8, 0.2);
        p.lineTo(100.8, 0.8);
        p.lineTo(100.2, 0.8);
        p.closePathWithLine();
        
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[100.0,1.0],[101.0,1.0],[101.0,0.0],[100.0,0.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]}", result);
    }

    @Test
    public void testPolygonWithHoleReversed() {
        Polygon p = new Polygon();
        
        //exterior ring - has to be clockwise for Esri
        p.startPath(100.0, 0.0);
        p.lineTo(100.0, 1.0);        
        p.lineTo(101.0, 1.0);        
        p.lineTo(101.0, 0.0);
        p.closePathWithLine();

        //hole - counterclockwise for Esri
        p.startPath(100.2, 0.2);
        p.lineTo(100.8, 0.2);
        p.lineTo(100.8, 0.8);
        p.lineTo(100.2, 0.8);
        p.closePathWithLine();
        
        p.reverseAllPaths();//make it reversed. Exterior ring - ccw, hole - cw.
        
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]],[[100.2,0.2],[100.2,0.8],[100.8,0.8],[100.8,0.2],[100.2,0.2]]]}", result);
    }
    
    @Test
    public void testMultiPolygon() throws IOException {
        JsonFactory jsonFactory = new JsonFactory();

        String geoJsonPolygon = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-100.0,-100.0],[-100.0,100.0],[100.0,100.0],[100.0,-100.0],[-100.0,-100.0]],[[-90.0,-90.0],[90.0,90.0],[-90.0,90.0],[90.0,-90.0],[-90.0,-90.0]]],[[[-10.0,-10.0],[-10.0,10.0],[10.0,10.0],[10.0,-10.0],[-10.0,-10.0]]]]}";
        String esriJsonPolygon = "{\"rings\": [[[-100, -100], [-100, 100], [100, 100], [100, -100], [-100, -100]], [[-90, -90], [90, 90], [-90, 90], [90, -90], [-90, -90]], [[-10, -10], [-10, 10], [10, 10], [10, -10], [-10, -10]]]}";

        JsonParser parser = jsonFactory.createJsonParser(esriJsonPolygon);
        MapGeometry parsedPoly = GeometryEngine.jsonToGeometry(parser);
        //MapGeometry parsedPoly = GeometryEngine.geometryFromGeoJson(jsonPolygon, 0, Geometry.Type.Polygon);

        Polygon poly = (Polygon) parsedPoly.getGeometry();

        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        //String result = exporter.execute(parsedPoly.getGeometry());
        String result = exporter.execute(poly);
        assertEquals(geoJsonPolygon, result);
    }



    @Test
    public void testEmptyPolygon() throws JSONException {
        Polygon p = new Polygon();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":null}", result);
        
        MapGeometry imported = OperatorImportFromGeoJson.local().execute(0, Geometry.Type.Unknown, result, null);
        assertTrue(imported.getGeometry().isEmpty());
        assertTrue(imported.getGeometry().getType() == Geometry.Type.Polygon);
    }

    @Test
    public void testPolygonGeometryEngine() {
        Polygon p = new Polygon();
        p.startPath(100.0, 0.0);
        p.lineTo(101.0, 0.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(100.0, 1.0);
        p.closePathWithLine();
        String result = GeometryEngine.geometryToGeoJson(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]}", result);
    }

    @Test
    public void testOGCPolygon() {
        Polygon p = new Polygon();
        p.startPath(100.0, 0.0);
        p.lineTo(101.0, 0.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(100.0, 1.0);
        p.closePathWithLine();
        OGCPolygon ogcPolygon = new OGCPolygon(p, null);
        String result = ogcPolygon.asGeoJson();
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[101.0,0.0],[101.0,1.0],[100.0,1.0],[100.0,0.0]]]}", result);
    }

    @Test
    public void testPolygonWithHoleGeometryEngine() {
        Polygon p = new Polygon();

        p.startPath(100.0, 0.0);//clockwise exterior
        p.lineTo(100.0, 1.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(101.0, 0.0);
        p.closePathWithLine();

        p.startPath(100.2, 0.2);//counterclockwise hole
        p.lineTo(100.8, 0.2);
        p.lineTo(100.8, 0.8);
        p.lineTo(100.2, 0.8);
        p.closePathWithLine();

        String result = GeometryEngine.geometryToGeoJson(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[100.0,1.0],[101.0,1.0],[101.0,0.0],[100.0,0.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]}", result);
    }

    @Test
    public void testPolylineWithTwoPaths() {
        Polyline p = new Polyline();

        p.startPath(100.0, 0.0);
        p.lineTo(100.0, 1.0);

        p.startPath(100.2, 0.2);
        p.lineTo(100.8, 0.2);

        String result = GeometryEngine.geometryToGeoJson(p);
        assertEquals("{\"type\":\"MultiLineString\",\"coordinates\":[[[100.0,0.0],[100.0,1.0]],[[100.2,0.2],[100.8,0.2]]]}", result);
    }
    
    @Test
    public void testOGCPolygonWithHole() {
        Polygon p = new Polygon();

        p.startPath(100.0, 0.0);
        p.lineTo(100.0, 1.0);
        p.lineTo(101.0, 1.0);
        p.lineTo(101.0, 0.0);
        p.closePathWithLine();

        p.startPath(100.2, 0.2);
        p.lineTo(100.8, 0.2);
        p.lineTo(100.8, 0.8);
        p.lineTo(100.2, 0.8);
        p.closePathWithLine();

        OGCPolygon ogcPolygon = new OGCPolygon(p, null);
        String result = ogcPolygon.asGeoJson();
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[100.0,0.0],[100.0,1.0],[101.0,1.0],[101.0,0.0],[100.0,0.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]}", result);
    }

    @Test
    public void testEnvelope() {
        Envelope e = new Envelope();
        e.setCoords(-180.0, -90.0, 180.0, 90.0);
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(e);
        assertEquals("{\"bbox\":[-180.0,-90.0,180.0,90.0]}", result);
    }

    @Test
    public void testEmptyEnvelope() {
        Envelope e = new Envelope();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(e);
        assertEquals("{\"bbox\":null}", result);
    }

    @Test
    public void testEnvelopeGeometryEngine() {
        Envelope e = new Envelope();
        e.setCoords(-180.0, -90.0, 180.0, 90.0);
        String result = GeometryEngine.geometryToGeoJson(e);
        assertEquals("{\"bbox\":[-180.0,-90.0,180.0,90.0]}", result);
    }

}
