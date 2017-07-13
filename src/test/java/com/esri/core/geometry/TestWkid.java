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


	@Test
	public void test_80() {
		SpatialReference sr = SpatialReference.create(3857);
		assertTrue(sr.getID() == 3857);
		assertTrue(sr.getLatestID() == 3857);
		assertTrue(sr.getOldID() == 102100);
		assertTrue(sr.getTolerance() == 0.001);
	}
	
}
