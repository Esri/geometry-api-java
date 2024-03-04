package com.esri.core.geometry;

import org.junit.Test;
import junit.framework.TestCase;
import org.mockito.Mockito;
public class TestJSONUtils extends TestCase{

    @Test
    public void testReadDoubleWithFloatValue() {
        JsonReader parser = Mockito.mock(JsonReader.class);
        Mockito.when(parser.currentToken()).thenReturn(JsonReader.Token.VALUE_NUMBER_FLOAT);
        Mockito.when(parser.currentDoubleValue()).thenReturn(3.14);

        double result = JSONUtils.readDouble(parser);
        assertEquals(3.14, result, 0.0001);
    }

    @Test
    public void testReadDoubleWithIntValue() {
        JsonReader parser = Mockito.mock(JsonReader.class);
        Mockito.when(parser.currentToken()).thenReturn(JsonReader.Token.VALUE_NUMBER_INT);
        Mockito.when(parser.currentIntValue()).thenReturn(42);

        double result = JSONUtils.readDouble(parser);
        assertEquals(42.0, result, 0.0001);
    }

    @Test
    public void testReadDoubleWithNullValue() {
        JsonReader parser = Mockito.mock(JsonReader.class);
        Mockito.when(parser.currentToken()).thenReturn(JsonReader.Token.VALUE_NULL);

        double result = JSONUtils.readDouble(parser);
        assertTrue(Double.isNaN(result));
    }

    @Test
    public void testReadDoubleWithNaNString() {
        JsonReader parser = Mockito.mock(JsonReader.class);
        Mockito.when(parser.currentToken()).thenReturn(JsonReader.Token.VALUE_STRING);
        Mockito.when(parser.currentString()).thenReturn("NaN");

        double result = JSONUtils.readDouble(parser);
        assertTrue(Double.isNaN(result));
    }

}
