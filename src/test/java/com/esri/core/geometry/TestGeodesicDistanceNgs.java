package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestGeodesicDistanceNgs extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void test1() {
		PeDouble p1 = new PeDouble(0);
		PeDouble p2 = new PeDouble(0);
		PeDouble p3 = new PeDouble(0);
		GeoDist.geodesic_distance_ngs(1.0, 2.0, 3.0, 1.57079632679489661923132, 5.0, 1.57079632679489661923132, p1, p2, p3);
		assertEquals(0.0, p3.val);
		assertEquals(0.0, p2.val);
		assertEquals(0.0, p1.val);
	}
	
	@Test
	public void test2() {
		PeDouble p1 = new PeDouble(0);
		PeDouble p2 = new PeDouble(0);
		PeDouble p3 = new PeDouble(0);
		GeoDist.geodesic_distance_ngs(50.0, 0.0067, 3.0, -1.57079632679489661923132, 5.0, 1.57079632679489661923132, p1, p2, p3);
		assertEquals(-1.8584073464102069, p3.val);
		assertEquals(-1.2831853071795862, p2.val);
		assertEquals(156.8161928387186, p1.val);
	}
	
	@Test
	public void test3() {
		PeDouble p1 = new PeDouble(0);
		PeDouble p2 = new PeDouble(0);
		PeDouble p3 = new PeDouble(0);
		GeoDist.geodesic_distance_ngs(50.0, 0.0067, 3.14159265358979323846264*3, -1, 0, 1, p1, p2, p3);
		assertEquals(0.0, p3.val);
		assertEquals(0.0, p2.val);
		assertEquals(156.8161928387186, p1.val);
	}
	
	@Test
	public void test4() {
		PeDouble p1 = new PeDouble(0);
		PeDouble p2 = new PeDouble(0);
		PeDouble p3 = new PeDouble(0);
		GeoDist.geodesic_distance_ngs(1.0, 0, 3.0, 1, 5.0, 1.57079632679489661923132, p1, p2, p3);
		assertEquals(0.14159265358979312, p3.val);
		assertEquals(1.0305047481203615E-16, p2.val);
		assertEquals(0.5707963267948967, p1.val);
	}
}
