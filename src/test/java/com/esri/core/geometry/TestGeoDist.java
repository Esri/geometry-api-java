package com.esri.core.geometry;

import junit.framework.TestCase;
import com.esri.core.geometry.ogc.*;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TestGeoDist extends TestCase {

    GeoDist geoDist = new GeoDist();
    private static final double PE_PI = 3.14159265358979323846264;
    private static final double PE_PI2 = 1.57079632679489661923132;
    private static final double PE_2PI = 6.283185307179586476925287;
    private static final double PE_EPS = 3.55271367880050092935562e-15;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testNoValuesToCalculate() {
        // setup
        PeDouble p_dist = null, p_az12 = null, p_az21 = null;

        // Test
        GeoDist.geodesic_distance_ngs(0, 0, 0, 0, 0, 0, p_dist, p_az12, p_az21);
        assertEquals(p_dist, null);
        assertEquals(p_az12, null);
        assertEquals(p_az21, null);
    }

    @Test
    public void testIfPointsAreTheSame1() {
        // setup
        PeDouble p_dist = new PeDouble(1.0), p_az12 = new PeDouble(1.0), p_az21 = null;

        // test
        GeoDist.geodesic_distance_ngs(0, 0, 0, 0, 0, 0, p_dist, p_az12, p_az21);
        assertEquals(p_dist.val, 0.0);
        assertEquals(p_az12.val, 0.0);
        assertEquals(p_az21, null);
    }

    @Test
    public void testIfPointsAreTheSame2() {
        // setup
        PeDouble p_dist = null, p_az12 = null, p_az21 = new PeDouble(1.0);

        // test
        GeoDist.geodesic_distance_ngs(0, 0, 0, 0, 0, 0, p_dist, p_az12, p_az21);
        assertEquals(p_dist, null);
        assertEquals(p_az12, null);
        assertEquals(p_az21.val, 0.0);
    }

    /**
     * Tests if points are perfectly antipodal and on opposite poles
     * p_dist.val == 1.0, p_az12 & p_az21 == null
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOppositePoles1() {
        // setup
        PeDouble p_dist = new PeDouble(1.0), p_az12 = null, p_az21 = null;
        double a = 0, e2 = 0, lam1 = 0, phi1 = -1.57079632679489661923132, lam2 = 0, phi2 = 1.57079632679489661923132;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_dist.val, 0.0);
        assertEquals(p_az12, null);
        assertEquals(p_az21, null);

    }

    /**
     * Tests if points are perfectly antipodal and on opposite poles
     * p_dist == null, p_az12.val == 1.0
     * phi1 < 0.0
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOppositePoles2() {
        // setup
        PeDouble p_dist = null, p_az12 = new PeDouble(1.0), p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0, lam1 = 0, phi1 = -1.57079632679489661923132, lam2 = 0, phi2 = 1.57079632679489661923132;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az12.val, 0.0);
        assertEquals(p_az21.val, 3.141592653589793);
        assertEquals(p_dist, null);

    }

    /**
     * Tests if points are perfectly antipodal and opposite poles
     * p_dist == null, p_az12.val == 1.0
     * phi1 > 0.0
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOtherAntipodal3() {
        // setup
        PeDouble p_dist = null, p_az12 = new PeDouble(1.0), p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0, lam1 = 0, phi1 = 1.57079632679489661923132, lam2 = 0, phi2 = -1.57079632679489661923132;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az12.val, 3.141592653589793);
        assertEquals(p_az21.val, 0.0);
        assertEquals(p_dist, null);

    }

    /**
     * Tests if points are perfectly antipodal and on not poles
     * p_dist.val == 1.0, p_az12 & p_az21 == null
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOtherAntipodal1() {
        // setup
        PeDouble p_dist = new PeDouble(1.0), p_az12 = null, p_az21 = null;
        double a = 0, e2 = 0, lam1 = 0, phi1 = -1.57079632679489661923132, lam2 = 0, phi2 = 1.57079632679489661923132;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_dist.val, 0.0);
        assertEquals(p_az12, null);
        assertEquals(p_az21, null);

    }

    /**
     * Tests if points are perfectly antipodal and on not poles
     * p_dist == null, p_az12.val == 1.0
     * phi1 < 0.0
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOtherAntipodal2() {
        // setup
        PeDouble p_dist = null, p_az12 = new PeDouble(1.0), p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0, lam1 = 0, phi1 = -1.57079632679489661923132, lam2 = 0, phi2 = 1.57079632679489661923132;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az12.val, 0.0);
        assertEquals(p_az21.val, 3.141592653589793);
        assertEquals(p_dist, null);

    }

    /**
     * Tests if points are perfectly antipodal and on not poles
     * p_dist == null, p_az12.val == 1.0
     * phi1 > 0.0
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOppositePoles3() {
        // setup
        PeDouble p_dist = null, p_az12 = new PeDouble(1.0), p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0, lam1 = 0, phi1 = 1.57079632679489661923132, lam2 = 0, phi2 = -1.57079632679489661923132;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az12.val, 3.141592653589793);
        assertEquals(p_az21.val, 0.0);
        assertEquals(p_dist, null);

    }

    /**
     * Test if p_dist value is set according the value of "a" if Sphere.
     * e2 == 0.0, p_dist != null, a == 0
     * phi1 != -phi2
     */
    @Test
    public void testSphereWithPdist() {
        // setup
        PeDouble p_dist = new PeDouble(1.0), p_az12 = null, p_az21 = null;
        double a = 0, e2 = 0.0, lam1 = 0, phi1 = 1.57079632679489661923132, lam2 = 0, phi2 = 1;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_dist.val, 0.0);

    }

    /**
     * Test if p_az12 value is set to the correct values if sphere with origin
     * at N or S pole.
     * e2 == 0.0, _az12 != null, phi1 == PE_PI2 and phi1 == -PE_PI2 respectively
     */
    @Test
    public void testSphereWithOriginInNorthSouthPole() {
        // setup
        PeDouble p_dist = null, p_az12 = new PeDouble(1.0), p_az21 = null;
        double a = 0, e2 = 0.0, lam1 = 0, phi1 = -PE_PI2, lam2 = -PE_PI, phi2 = 1;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az12.val, lam2);

        phi1 = PE_PI2;
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az12.val, 0.0);

    }

    /**
     * Test if p_az12 value is set to the correct values if sphere with origin
     * NOT at N or S pole.
     * e2 == 0.0, p_az12 != null, phi1 != PE_PI2
     */
    @Test
    public void testSphereWithOriginNotInNorthSouthPole() {
        // setup
        PeDouble p_dist = null, p_az12 = new PeDouble(1.0), p_az21 = null;
        double a = 0, e2 = 0.0, lam1 = 0, phi1 = 3, lam2 = 0, phi2 = 1;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertNotEquals(p_az12.val, 1.0, 0.0001);
    }

    /**
     * Test if p_az21 value is set to the correct values if sphere with destination
     * at N or S pole.
     * e2 == 0.0, _az21 != null, phi2 == PE_PI2 and phi2 == -PE_PI2 respectively
     */
    @Test
    public void testSphereWithDestinationInNorthSouthPole() {
        // setup
        PeDouble p_dist = null, p_az12 = null, p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0.0, lam1 = -PE_PI, phi1 = 1, lam2 = 0, phi2 = -PE_PI2;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az21.val, lam1);

        phi2 = PE_PI2;
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_az21.val, 0.0);

    }

    /**
     * Test if p_az12 value is set to the correct values if sphere with origin
     * NOT at N or S pole.
     * e2 == 0.0, p_az21 != null, phi2 != PE_PI2
     */
    @Test
    public void testSphereWithDestinationNotInNorthSouthPole() {
        // setup
        PeDouble p_dist = null, p_az12 = null, p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0.0, lam1 = 0, phi1 = 1, lam2 = 0, phi2 = 3;

        // test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertNotEquals(p_az21.val, 1.0, 0.0001);
    }
}
