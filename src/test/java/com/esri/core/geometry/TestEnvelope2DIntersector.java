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

import java.util.ArrayList;
import java.util.Random;
import junit.framework.TestCase;
import org.junit.Test;

public class TestEnvelope2DIntersector extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testEnvelope2Dintersector() {
		ArrayList<Envelope2D> envelopes = new ArrayList<Envelope2D>(0);

		Envelope2D env0 = new Envelope2D(2, 3, 4, 4);
		Envelope2D env1 = new Envelope2D(5, 13, 9, 15);
		Envelope2D env2 = new Envelope2D(6, 9, 11, 12);
		Envelope2D env3 = new Envelope2D(8, 10, 9, 17);
		Envelope2D env4 = new Envelope2D(11.001, 12, 14, 14);
		Envelope2D env5 = new Envelope2D(1, 3, 3, 4);
		Envelope2D env6 = new Envelope2D(0, 2, 5, 10);
		Envelope2D env7 = new Envelope2D(4, 7, 5, 10);
		Envelope2D env8 = new Envelope2D(3, 15, 15, 15);
		Envelope2D env9 = new Envelope2D(0, 9, 14, 9);
		Envelope2D env10 = new Envelope2D(0, 8.999, 14, 8.999);

		envelopes.add(env0);
		envelopes.add(env1);
		envelopes.add(env2);
		envelopes.add(env3);
		envelopes.add(env4);
		envelopes.add(env5);
		envelopes.add(env6);
		envelopes.add(env7);
		envelopes.add(env8);
		envelopes.add(env9);
		envelopes.add(env10);

		Envelope2DIntersectorImpl intersector = new Envelope2DIntersectorImpl();
		intersector.setTolerance(0.001);

		intersector.startConstruction();
		for (int i = 0; i < envelopes.size(); i++)
			intersector.addEnvelope(i, envelopes.get(i));
		intersector.endConstruction();

		int count = 0;
		while (intersector.next()) {
			int env_a = intersector.getHandleA();
			int env_b = intersector.getHandleB();
			count++;
			Envelope2D env = new Envelope2D();
			env.setCoords(envelopes.get(env_a));
			env.inflate(0.001, 0.001);
			assertTrue(env.isIntersecting(envelopes.get(env_b)));
		}

		assert (count == 16);

		Envelope2DIntersectorImpl intersector2 = new Envelope2DIntersectorImpl();
		intersector2.setTolerance(0.0);
		intersector2.startConstruction();
		for (int i = 0; i < envelopes.size(); i++)
			intersector2.addEnvelope(i, envelopes.get(i));
		intersector2.endConstruction();

		count = 0;
		while (intersector2.next()) {
			int env_a = intersector2.getHandleA();
			int env_b = intersector2.getHandleB();
			count++;
			Envelope2D env = new Envelope2D();
			env.setCoords(envelopes.get(env_a));
			assertTrue(env.isIntersecting(envelopes.get(env_b)));
		}

		assert (count == 13);

		env0 = new Envelope2D(0, 0, 0, 10);
		env1 = new Envelope2D(0, 10, 10, 10);
		env2 = new Envelope2D(10, 0, 10, 10);
		env3 = new Envelope2D(0, 0, 10, 0);
		envelopes.clear();

		envelopes.add(env0);
		envelopes.add(env1);
		envelopes.add(env2);
		envelopes.add(env3);

		Envelope2DIntersectorImpl intersector3 = new Envelope2DIntersectorImpl();
		intersector3.setTolerance(0.001);

		intersector3.startConstruction();
		for (int i = 0; i < envelopes.size(); i++)
			intersector3.addEnvelope(i, envelopes.get(i));
		intersector3.endConstruction();
		;
		count = 0;
		while (intersector3.next()) {
			int env_a = intersector3.getHandleA();
			int env_b = intersector3.getHandleB();
			count++;
			Envelope2D env = new Envelope2D();
			env.setCoords(envelopes.get(env_a));
			assertTrue(env.isIntersecting(envelopes.get(env_b)));
		}

		assertTrue(count == 4);

		env0 = new Envelope2D(0, 0, 0, 10);
		envelopes.clear();

		envelopes.add(env0);
		envelopes.add(env0);
		envelopes.add(env0);
		envelopes.add(env0);

		Envelope2DIntersectorImpl intersector4 = new Envelope2DIntersectorImpl();
		intersector4.setTolerance(0.001);

		intersector4.startConstruction();
		for (int i = 0; i < envelopes.size(); i++)
			intersector4.addEnvelope(i, envelopes.get(i));
		intersector4.endConstruction();

		count = 0;
		while (intersector4.next()) {
			int env_a = intersector4.getHandleA();
			int env_b = intersector4.getHandleB();
			count++;
			Envelope2D env = new Envelope2D();
			env.setCoords(envelopes.get(env_a));
			assertTrue(env.isIntersecting(envelopes.get(env_b)));
		}

		assert (count == 6);

		env0 = new Envelope2D(0, 10, 10, 10);
		envelopes.clear();

		envelopes.add(env0);
		envelopes.add(env0);
		envelopes.add(env0);
		envelopes.add(env0);

		Envelope2DIntersectorImpl intersector5 = new Envelope2DIntersectorImpl();
		intersector5.setTolerance(0.001);

		intersector5.startConstruction();
		for (int i = 0; i < envelopes.size(); i++)
			intersector5.addEnvelope(i, envelopes.get(i));
		intersector5.endConstruction();

		count = 0;
		while (intersector5.next()) {
			int env_a = intersector5.getHandleA();
			int env_b = intersector5.getHandleB();
			count++;
			Envelope2D env = new Envelope2D();
			env.setCoords(envelopes.get(env_a));
			assertTrue(env.isIntersecting(envelopes.get(env_b)));
		}

		assertTrue(count == 6);
	}

	@Test
	public static void testRandom() {
		int passcount = 10;
		int figureSize = 100;
		int figureSize2 = 100;
		Envelope extent1 = new Envelope(), extent2 = new Envelope(), extent3 = new Envelope(), extent4 = new Envelope();
		extent1.setCoords(-10000, 5000, 10000, 25000);// red
		extent2.setCoords(-10000, 2000, 10000, 8000);// blue
		extent3.setCoords(-10000, -8000, 10000, -2000);// blue
		extent4.setCoords(-10000, -25000, 10000, -5000);// red

		RandomCoordinateGenerator generator1 = new RandomCoordinateGenerator(
				Math.max(figureSize, 10000), extent1, 0.001);
		RandomCoordinateGenerator generator2 = new RandomCoordinateGenerator(
				Math.max(figureSize, 10000), extent2, 0.001);
		RandomCoordinateGenerator generator3 = new RandomCoordinateGenerator(
				Math.max(figureSize, 10000), extent3, 0.001);
		RandomCoordinateGenerator generator4 = new RandomCoordinateGenerator(
				Math.max(figureSize, 10000), extent4, 0.001);

		Random random = new Random(1982);
		int rand_max = 511;

		int qCount = 0;
		int eCount = 0;
		@SuppressWarnings("unused")
		int bCount = 0;
		for (int c = 0; c < passcount; c++) {
			Polygon polyRed = new Polygon();
			Polygon polyBlue = new Polygon();

			int r = figureSize;
			if (r < 3)
				continue;

			Point pt;
			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean bRandomNew = (r > 10)
						&& ((1.0 * rand) / rand_max > 0.95);
				pt = generator1.GetRandomCoord();
				if (j == 0 || bRandomNew)
					polyRed.startPath(pt);
				else
					polyRed.lineTo(pt);
			}

			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean bRandomNew = (r > 10)
						&& ((1.0 * rand) / rand_max > 0.95);
				pt = generator4.GetRandomCoord();
				if (j == 0 || bRandomNew)
					polyRed.startPath(pt);
				else
					polyRed.lineTo(pt);
			}

			r = figureSize2;
			if (r < 3)
				continue;

			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean bRandomNew = (r > 10)
						&& ((1.0 * rand) / rand_max > 0.95);
				pt = generator2.GetRandomCoord();
				if (j == 0 || bRandomNew)
					polyBlue.startPath(pt);
				else
					polyBlue.lineTo(pt);
			}

			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean bRandomNew = (r > 10)
						&& ((1.0 * rand) / rand_max > 0.95);
				pt = generator3.GetRandomCoord();
				if (j == 0 || bRandomNew)
					polyBlue.startPath(pt);
				else
					polyBlue.lineTo(pt);
			}

			Envelope2D env = new Envelope2D();

			// Quad_tree
			QuadTree quadTree = buildQuadTree(polyBlue);
			QuadTree.QuadTreeIterator iterator = quadTree.getIterator();

			SegmentIteratorImpl _segIterRed = ((MultiPathImpl) polyRed
					._getImpl()).querySegmentIterator();

			while (_segIterRed.nextPath()) {
				while (_segIterRed.hasNextSegment()) {
					Segment segmentRed = _segIterRed.nextSegment();
					segmentRed.queryEnvelope2D(env);
					iterator.resetIterator(env, 0.001);
					while (iterator.next() != -1)
						qCount++;
				}
			}

			// Envelope_2D_intersector

			ArrayList<Envelope2D> envelopes_red = new ArrayList<Envelope2D>();
			ArrayList<Envelope2D> envelopes_blue = new ArrayList<Envelope2D>();

			SegmentIterator segIterRed = polyRed.querySegmentIterator();
			while (segIterRed.nextPath()) {
				while (segIterRed.hasNextSegment()) {
					Segment segment = segIterRed.nextSegment();
					env = new Envelope2D();
					segment.queryEnvelope2D(env);
					envelopes_red.add(env);
				}
			}

			SegmentIterator segIterBlue = polyBlue.querySegmentIterator();
			while (segIterBlue.nextPath()) {
				while (segIterBlue.hasNextSegment()) {
					Segment segment = segIterBlue.nextSegment();
					env = new Envelope2D();
					segment.queryEnvelope2D(env);
					envelopes_blue.add(env);
				}
			}

			Envelope2DIntersectorImpl intersector = new Envelope2DIntersectorImpl();
			intersector.setTolerance(0.001);

			intersector.startRedConstruction();
			for (int i = 0; i < envelopes_red.size(); i++)
				intersector.addRedEnvelope(i, envelopes_red.get(i));
			intersector.endRedConstruction();

			intersector.startBlueConstruction();
			for (int i = 0; i < envelopes_blue.size(); i++)
				intersector.addBlueEnvelope(i, envelopes_blue.get(i));
			intersector.endBlueConstruction();

			while (intersector.next())
				eCount++;

			assertTrue(qCount == eCount);
		}
	}

	public static QuadTree buildQuadTree(MultiPath multipath) {
		Envelope2D extent = new Envelope2D();
		multipath.queryEnvelope2D(extent);
		QuadTree quadTree = new QuadTree(extent, 8);
		int hint_index = -1;
		SegmentIterator seg_iter = multipath.querySegmentIterator();
		while (seg_iter.nextPath()) {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();
				int index = seg_iter.getStartPointIndex();
				Envelope2D boundingbox = new Envelope2D();
				segment.queryEnvelope2D(boundingbox);
				hint_index = quadTree.insert(index, boundingbox, hint_index);
			}
		}

		return quadTree;
	}
}
