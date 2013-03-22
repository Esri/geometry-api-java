package com.esri.core.geometry;

import static org.junit.Assert.*;
import junit.framework.TestCase;

import org.junit.Test;

public class TestWkid extends TestCase {
	@Test
	public void test() {
		SpatialReference sr = SpatialReference.create(102100);
		assertTrue(sr.getID() == 102100);
		assertTrue(sr.getLatestID() == 3857);
		assertTrue(sr.getOldID() == 102100);
		assertTrue(sr.getTolerance() == 0.001);

		SpatialReference sr84 = SpatialReference.create(4326);
		double tol84 = sr84.getTolerance();
		assertTrue(Math.abs(tol84 - 1e-8) < 1e-8 * 1e-8);
	}

}
