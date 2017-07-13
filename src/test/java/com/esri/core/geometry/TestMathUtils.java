/*
 Copyright 1995-2017 Esri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */

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
