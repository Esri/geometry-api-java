package com.esri.core.geometry;

import junit.framework.TestCase;
import com.esri.core.geometry.ogc.*;

import org.junit.Test;

public class TestGeoDist extends TestCase {

    GeoDist geoDist = new GeoDist();

    @Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

    @Test
    public void testNoValuesToCalculate(){
        //setup
        PeDouble p_dist = null, p_az12 = null, p_az21 = null;
        
        //Test
        GeoDist.geodesic_distance_ngs(0, 0, 0, 0, 0, 0, p_dist, p_az12, p_az21);
        assertEquals(p_dist, null);
        assertEquals(p_az12, null);
        assertEquals(p_az21, null);
    }
    
    @Test
    public void testIfPointsAreTheSame1() {
        //setup
        PeDouble p_dist = new PeDouble(1.0), p_az12 = new PeDouble(1.0), p_az21 = null;
        
        //test
        GeoDist.geodesic_distance_ngs(0, 0, 0, 0, 0, 0, p_dist, p_az12, p_az21);
        assertEquals(p_dist.val, 0.0);
        assertEquals(p_az12.val, 0.0);
        assertEquals(p_az21, null);
    }

    @Test
    public void testIfPointsAreTheSame2() {
        //setup
        PeDouble p_dist = null, p_az12 = null, p_az21 = new PeDouble(1.0);
        
        //test
        GeoDist.geodesic_distance_ngs(0, 0, 0, 0, 0, 0, p_dist, p_az12, p_az21);
        assertEquals(p_dist, null);
        assertEquals(p_az12, null);
        assertEquals(p_az21.val, 0.0);
    }
    
    /**
     * Tests if points are perfectly antipodal and on opposite poles
     */
    @Test
    public void testIfPointsArePerfectlyAntipodalOppositePoles1(){
        //setup
        PeDouble p_dist = new PeDouble(1.0), p_az12 = new PeDouble(1.0), p_az21 = new PeDouble(1.0);
        double a = 0, e2 = 0, lam1 = 0, phi1 = -1.57079632679489661923132, lam2 = 0,  phi2 = 1.57079632679489661923132;
        
        //test
        GeoDist.geodesic_distance_ngs(a, e2, lam1, phi1, lam2, phi2, p_dist, p_az12, p_az21);
        assertEquals(p_dist.val, 0);
        
    }
}
