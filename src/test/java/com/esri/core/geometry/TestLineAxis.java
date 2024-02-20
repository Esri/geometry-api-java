package com.esri.core.geometry;

import com.esri.core.geometry.Geometry.Type;
import junit.framework.TestCase;
import org.junit.Test;

public class TestLineAxis extends TestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    @Test
    public void testAxisNegative() {
        // Regardless of axis the test should return zero if t is negative
        double ordinate = 10;
        double[] result_ordinates = {1.3,2.2};
        double[] parameters = null;
        Line l1 = new Line(0,0,5,5);
        int a = l1.intersectionWithAxis2D(true, ordinate, result_ordinates, parameters);
        int b = l1.intersectionWithAxis2D(false, ordinate, result_ordinates, parameters);

        // intersection should return 0
        assertEquals(a, 0);
        assertEquals(b, 0);

    }
    @Test
    public void testAxisCollapsingPoints() {
        // If line start and end point collapse, then regardless of axis, the result should be 0 or -1
        double ordinate = 10;
        double[] result_ordinates = {1.3,2.2};
        double[] parameters = null;

        // Line segment consists of p1=p2
        Line l1 = new Line(1,1,1,1);

        int a = l1.intersectionWithAxis2D(true, ordinate, result_ordinates, parameters);
        int b = l1.intersectionWithAxis2D(false, ordinate, result_ordinates, parameters);

        assertEquals(a, 0);
        assertEquals(b, 0);
    }

    @Test
    public void testAxisNullResults() {
        // If line start and end point collapse, then regardless of axis, the result should be 0 or -1
        double ordinate = 0;

        // Line segment consists of p1=p2
        Line l1 = new Line(-1,-1,1,1);
        
        int a = l1.intersectionWithAxis2D(true, ordinate, null, null);
        int b = l1.intersectionWithAxis2D(false, ordinate, null, null);

        assertEquals(a, 0);
        assertEquals(b, 0);
    }
}