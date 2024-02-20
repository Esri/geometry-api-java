package com.esri.core.geometry;

import org.junit.Test;

public class appendQouteTest {
    JsonStringWriter jsw =  new JsonStringWriter();

    @Test
    public void testAppendQouteWithTabs() {
        jsw.startObject();
        jsw.addPairString("cool\tare", "tex\tabs");
        jsw.endObject();
        String testString = ("{\"cool\\tare\":\"tex\\tabs\"}");
        assert (jsw.getJson().equals(testString));
    }

    @Test
    public void testAppendQouteWithNewlines() {
        jsw.startObject();
        jsw.addPairString("cool\nare", "tex\nabs");
        jsw.endObject();
        String testString = ("{\"cool\\nare\":\"tex\\nabs\"}");
        assert (jsw.getJson().equals(testString));
    }

    @Test
    public void testAppendQouteWithBackslashes() {
        jsw.startObject();
        jsw.addPairString("cool\\are", "tex\\abs");
        jsw.endObject();
        String testString = ("{\"cool\\\\are\":\"tex\\\\abs\"}");
        assert (jsw.getJson().equals(testString));
    }

    @Test
    public void testAppendQouteWithBAndF() {
        jsw.startObject();
        jsw.addPairString("cool\bare", "tex\fabs");
        jsw.endObject();
        String testString = ("{\"cool\\bare\":\"tex\\fabs\"}");
        assert (jsw.getJson().equals(testString));
    }

    @Test
    public void testAppendQouteWithForwardPrecission() {
        jsw.startObject();
        jsw.addPairString("cool</are", "tex//abs");
        jsw.endObject();
        String testString = ("{\"cool<\\/are\":\"tex//abs\"}");
        assert (jsw.getJson().equals(testString));
    }


}
