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

public class TestEquals extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testEqualsOnEnvelopes() {
		SpatialReference sr = SpatialReference.create(4326);

		Point p = new Point(-130, 10);
		Envelope env = new Envelope(p, 12, 12);
		Envelope env2 = new Envelope(-136, 4, -124, 16);

		boolean isEqual;
		try {
			isEqual = GeometryEngine.equals(env, env2, sr);

		} catch (IllegalArgumentException ex) {
			isEqual = false;
		}
		assertTrue(isEqual);
	}

	@Test
	public void testEqualsOnPoints() {
		SpatialReference sr = SpatialReference.create(4326);

		Point p1 = new Point(-116, 40);
		@SuppressWarnings("unused")
		Point p2 = new Point(-120, 39);
		@SuppressWarnings("unused")
		Point p3 = new Point(-121, 10);
		@SuppressWarnings("unused")
		Point p4 = new Point(-130, 12);
		@SuppressWarnings("unused")
		Point p5 = new Point(-108, 25);

		Point p12 = new Point(-116, 40);
		@SuppressWarnings("unused")
		Point p22 = new Point(-120, 39);
		@SuppressWarnings("unused")
		Point p32 = new Point(-121, 10);
		@SuppressWarnings("unused")
		Point p42 = new Point(-130, 12);
		@SuppressWarnings("unused")
		Point p52 = new Point(-108, 25);

		boolean isEqual1 = false;
		boolean isEqual2 = false;
		boolean isEqual3 = false;
		boolean isEqual4 = false;
		boolean isEqual5 = false;

		try {
			isEqual1 = GeometryEngine.equals(p1, p12, sr);
			isEqual2 = GeometryEngine.equals(p1, p12, sr);
			isEqual3 = GeometryEngine.equals(p1, p12, sr);
			isEqual4 = GeometryEngine.equals(p1, p12, sr);
			isEqual5 = GeometryEngine.equals(p1, p12, sr);
		} catch (IllegalArgumentException ex) {

		}

		assertTrue(isEqual1 && isEqual2 && isEqual3 && isEqual4 && isEqual5);
	}

	@Test
	public void testEqualsOnPolygons() {
		SpatialReference sr = SpatialReference.create(4326);

		Polygon baseMp = new Polygon();
		Polygon compMp = new Polygon();

		baseMp.startPath(-116, 40);
		baseMp.lineTo(-120, 39);
		baseMp.lineTo(-121, 10);
		baseMp.lineTo(-130, 12);
		baseMp.lineTo(-108, 25);

		compMp.startPath(-116, 40);
		compMp.lineTo(-120, 39);
		compMp.lineTo(-121, 10);
		compMp.lineTo(-130, 12);
		compMp.lineTo(-108, 25);

		boolean isEqual;

		try {
			isEqual = GeometryEngine.equals(baseMp, compMp, sr);

		} catch (IllegalArgumentException ex) {
			isEqual = false;
		}

		assertTrue(isEqual);
	}

	@Test
	public void testEqualsOnPolylines() {
		SpatialReference sr = SpatialReference.create(4326);

		Polyline baseMp = new Polyline();
		Polyline compMp = new Polyline();

		baseMp.startPath(-116, 40);
		baseMp.lineTo(-120, 39);
		baseMp.lineTo(-121, 10);
		baseMp.lineTo(-130, 12);
		baseMp.lineTo(-108, 25);

		compMp.startPath(-116, 40);
		compMp.lineTo(-120, 39);
		compMp.lineTo(-121, 10);
		compMp.lineTo(-130, 12);
		compMp.lineTo(-108, 25);

		boolean isEqual;

		try {
			isEqual = GeometryEngine.equals(baseMp, compMp, sr);
		} catch (IllegalArgumentException ex) {
			isEqual = false;
		}

		assertTrue(isEqual);
	}

	@Test
	public void testEqualsOnMultiPoints() {
		SpatialReference sr = SpatialReference.create(4326);

		MultiPoint baseMp = new MultiPoint();
		MultiPoint compMp = new MultiPoint();

		baseMp.add(new Point(-116, 40));
		baseMp.add(new Point(-120, 39));
		baseMp.add(new Point(-121, 10));
		baseMp.add(new Point(-130, 12));
		baseMp.add(new Point(-108, 25));

		compMp.add(new Point(-116, 40));
		compMp.add(new Point(-120, 39));
		compMp.add(new Point(-121, 10));
		compMp.add(new Point(-130, 12));
		compMp.add(new Point(-108, 25));

		boolean isEqual;

		try {
			isEqual = GeometryEngine.equals(baseMp, compMp, sr);
		} catch (IllegalArgumentException ex) {
			isEqual = false;
		}

		assertTrue(isEqual);
	}
}
