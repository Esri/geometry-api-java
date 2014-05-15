package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;

public class TestMathUtils extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testKahanSummation() {
		double s = 0.0;
		for (int i = 0; i < 10000; i++) {
			if (i == 0) {
				s += 1e6;
			} else
				s += 1e-7;
		}

		double trueAnswer = 1e6 + 9999 * 1e-7;
		assertTrue(Math.abs(s - trueAnswer) > 1e-9); // precision loss
		MathUtils.KahanSummator sum = new MathUtils.KahanSummator(0);
		for (int i = 0; i < 10000; i++) {
			if (i == 0) {
				sum.add(1e6);
			} else
				sum.add(1e-7);
		}
		double kahanResult = sum.getResult();
		// 1000000.0009999000 //C++
		// 1000000.0009999 //Java
		assertTrue(kahanResult == trueAnswer); // nice answer!
	}
}
