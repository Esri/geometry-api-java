package com.esri.core.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestAttributeStreamOfDbl {

    @Test
    public void testWriteRangeForward() {
        AttributeStreamOfDbl a = new AttributeStreamOfDbl(10);
        a.write(0, -2);
        a.write(1, -1);
        a.write(2, 0);
        a.write(3, 1);
        a.write(4, 2);
        a.write(5, 3);
        a.write(6, 4);
        a.write(7, 5);
        a.write(8, 6);
        a.write(9, 7);

        AttributeStreamOfDbl b = new AttributeStreamOfDbl(6, (short) 0);

        b.writeRange(0, 6, a, 3, true, 0);
        for (int i = 0; i < 6; ++i) {
            assertTrue(b.read(i) == i + 1);

        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithIllegalArgument() {
        AttributeStreamOfDbl a = new AttributeStreamOfDbl(10);
        AttributeStreamOfDbl b = new AttributeStreamOfDbl(10, (short) 0);

        b.writeRange(0, 5, a, 7, true, 0);

    }

    @Test
    public void testWriteRangeBackWardStride1() {
        AttributeStreamOfDbl a = new AttributeStreamOfDbl(10);
        a.write(0, -2);
        a.write(1, -1);
        a.write(2, 0);
        a.write(3, 1);
        a.write(4, 2);
        a.write(5, 3);
        a.write(6, 4);
        a.write(7, 5);
        a.write(8, 6);
        a.write(9, 7);
        AttributeStreamOfDbl b = new AttributeStreamOfDbl(4, (short) 0);

        b.writeRange(0, 4, a, 6, false, 1);
        for (int i = 0; i < 4; ++i) {
            assertTrue(b.read(i) == 7 - i);
        }
    }

    @Test
    public void testWriteRangeBackWardStride4() {
        AttributeStreamOfDbl a = new AttributeStreamOfDbl(10);
        a.write(0, -2);
        a.write(1, -1);
        a.write(2, 0);
        a.write(3, 1);
        a.write(4, 2);
        a.write(5, 3);
        a.write(6, 4);
        a.write(7, 5);
        a.write(8, 6);
        a.write(9, 7);
        AttributeStreamOfDbl b = new AttributeStreamOfDbl(10, (short) 0);

        b.writeRange(0, 6, a, 0, false, 3);

        assertTrue(b.read(0) == 1);
        assertTrue(b.read(1) == 2);
        assertTrue(b.read(2) == 3);
        assertTrue(b.read(3) == -2);
        assertTrue(b.read(4) == -1);
        assertTrue(b.read(5) == 0);

    }

}
