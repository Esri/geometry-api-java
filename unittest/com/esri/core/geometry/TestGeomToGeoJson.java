package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

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
        assertEquals("{\"type\":\"Point\",\"coordinates\":[]}", result);
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
        assertEquals("{\"type\":\"MultiPoint\",\"coordinates\":[]}", result);
    }

    @Test
    public void testEmptyPolyline() {
        Polyline p = new Polyline();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[]}", result);
    }

    @Test
    public void testEmptyPolygon() {
        Polygon p = new Polygon();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(p);
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[]}", result);
    }

    @Test
    public void testEmptyEnvelope() {
        Envelope e = new Envelope();
        OperatorExportToGeoJson exporter = (OperatorExportToGeoJson) factory.getOperator(Operator.Type.ExportToGeoJson);
        String result = exporter.execute(e);
        assertEquals("{\"bbox\":[],\"type\":\"Polygon\",\"coordinates\":[]}", result);
    }
}
