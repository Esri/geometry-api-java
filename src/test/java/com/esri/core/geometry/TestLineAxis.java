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

    /*
     * Tests to make sure proper action is taken when intersection point is negative
     */
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
    /*
     * Tests to make sure 0 is returned when endpoints collapse
     * (i.e p1=p2)
     */
    @Test
    public void testAxisCollapsingPoints() {
        // If line start and end point collapse, then regardless of axis, the result should be 0
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

    /*
     * Tests to make sure -1 is returned when endpoints collapse
     * and ordinate is the same as the endpoint
     * (This will check the ternary operation return)
     */
    @Test
    public void testEndpointEqualsOrdinate() {
        // If line start and end point collapse, then regardless of axis, the result should be -1
        double ordinate = 1;
        double[] result_ordinates = {1.3,2.2};
        double[] parameters = {1.3,2.2};

        // Line segment consists of p1=p2
        Line l1 = new Line(1,1,1,1);

        int a = l1.intersectionWithAxis2D(true, ordinate, result_ordinates, parameters);
        int b = l1.intersectionWithAxis2D(false, ordinate, result_ordinates, parameters);

        assertEquals(a, -1);
        assertEquals(b, -1);
    }

    /*
     * Tests to make sure 1 is returned when t is in the interval 0<t<1
     */
    @Test
    public void testTinInterval() {
        double ordinate = 0.2001;
        double[] result_ordinates = null;
        double[] parameters = null;

        // Line segment consists of p1 = (0.1,0.1) and p2 = (0.2,0.21)
        Line l1 = new Line(0.1,0.1,0.2,0.21);
        System.out.println("hejsan");

        int b = l1.intersectionWithAxis2D(true, ordinate, result_ordinates, parameters);
        assertEquals(b, 1);
    }
}