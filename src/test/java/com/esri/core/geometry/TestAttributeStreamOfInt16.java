package com.esri.core.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestAttributeStreamOfInt16 {
    
    // HELPERS:
    
    private void checkEquality(AttributeStreamOfInt16 actual, short[] expected) {
        assertEquals(actual.size(), expected.length);
        for (int i = 0; i < actual.size(); i++) {
            assertEquals(expected[i], actual.read(i));
        }
    }
    
    private void writeValues(AttributeStreamOfInt16 actual, short[] values) {
        assertEquals(actual.size(), values.length);
        for (int i = 0; i < actual.size(); i++) {
            actual.write(i, values[i]);
        }
    }
    
    // TESTS:
    
    /**
     * Going forward, writing part of _src into different part of target.
     */
    @Test
    public void testWriteRange_forward() {
        AttributeStreamOfInt16 target = new AttributeStreamOfInt16(8, (short)-1);
        int startElement = 2;
        int count = 5;
        AttributeStreamOfInt16 _src = new AttributeStreamOfInt16(10);
        int srcStart = 5;
        boolean bForward = true;
        int stride = 1;
        writeValues(_src, new short[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        
        target.writeRange(startElement, count, _src, srcStart, bForward, stride);
        checkEquality(target, new short[]{-1, -1, 15, 16, 17, 18, 19, -1});
    }
    
    /**
     * Going backwards, writing part of _src into different part of target.
     */
    @Test
    public void testWriteRange_backward() {
        AttributeStreamOfInt16 target = new AttributeStreamOfInt16(8, (short)-1);
        int startElement = 2;
        int count = 5;
        AttributeStreamOfInt16 _src = new AttributeStreamOfInt16(10);
        int srcStart = 5;
        boolean bForward = false;
        int stride = 1;
        writeValues(_src, new short[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        
        target.writeRange(startElement, count, _src, srcStart, bForward, stride);
        checkEquality(target, new short[]{-1, -1, 19, 18, 17, 16, 15, -1});
    }
    
    /**
     * Going backwards, writing part of _src into different part of target, with a stride over 1.
     */
    @Test
    public void testWriteRange_backward_stride() {
        AttributeStreamOfInt16 target = new AttributeStreamOfInt16(10, (short)-1);
        int startElement = 1;
        int count = 9;
        AttributeStreamOfInt16 _src = new AttributeStreamOfInt16(10);
        int srcStart = 0;
        boolean bForward = false;
        int stride = 3;
        writeValues(_src, new short[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        
        target.writeRange(startElement, count, _src, srcStart, bForward, stride);
        checkEquality(target, new short[]{-1, 16, 17, 18, 13, 14, 15, 10, 11, 12});
    }
    
    /**
     * Writing into self.
     */
    @Test
    public void testWriteRange_self() {
        AttributeStreamOfInt16 target = new AttributeStreamOfInt16(10);
        int startElement = 1;
        int count = 5;
        int srcStart = 3;
        boolean bForward = true;
        int stride = 1;
        writeValues(target, new short[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        
        target.writeRange(startElement, count, target, srcStart, bForward, stride);
        checkEquality(target, new short[]{10, 13, 14, 15, 16, 17, 16, 17, 18, 19});
    }
    
}