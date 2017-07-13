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

public class TestIntervalTree extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	static void construct(IntervalTreeImpl interval_tree,
			ArrayList<Envelope1D> intervals) {
		interval_tree.startConstruction();
		for (int i = 0; i < intervals.size(); i++)
			interval_tree.addInterval(intervals.get(i));
		interval_tree.endConstruction();
	}

	@Test
	public static void testIntervalTree() {
		ArrayList<Envelope1D> intervals = new ArrayList<Envelope1D>(0);

		Envelope1D env0 = new Envelope1D(2, 3);
		Envelope1D env1 = new Envelope1D(5, 13);
		Envelope1D env2 = new Envelope1D(6, 9);
		Envelope1D env3 = new Envelope1D(8, 10);
		Envelope1D env4 = new Envelope1D(11, 12);
		Envelope1D env5 = new Envelope1D(1, 3);
		Envelope1D env6 = new Envelope1D(0, 2);
		Envelope1D env7 = new Envelope1D(4, 7);
		Envelope1D env8;

		intervals.add(env0);
		intervals.add(env1);
		intervals.add(env2);
		intervals.add(env3);
		intervals.add(env4);
		intervals.add(env5);
		intervals.add(env6);
		intervals.add(env7);

		int counter;
		IntervalTreeImpl intervalTree = new IntervalTreeImpl(false);
		construct(intervalTree, intervals);
		IntervalTreeImpl.IntervalTreeIteratorImpl iterator = intervalTree
				.getIterator(new Envelope1D(-1, 14), 0.0);

		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 8);

		iterator.resetIterator(new Envelope1D(2.5, 10.5), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 6);

		iterator.resetIterator(5.0, 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 2);

		iterator.resetIterator(7, 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 3);

		iterator.resetIterator(new Envelope1D(2.0, 10.5), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 7);

		iterator.resetIterator(new Envelope1D(2.5, 11), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 7);

		iterator.resetIterator(new Envelope1D(2.1, 2.5), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 2);

		iterator.resetIterator(new Envelope1D(2.1, 5), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 4);

		iterator.resetIterator(new Envelope1D(2.0, 5), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 5);

		iterator.resetIterator(new Envelope1D(5.0, 11), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 5);

		iterator.resetIterator(new Envelope1D(8, 10.5), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 3);

		iterator.resetIterator(new Envelope1D(10, 11), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 3);

		iterator.resetIterator(new Envelope1D(10, 10.9), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 2);

		iterator.resetIterator(new Envelope1D(11.5, 12), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 2);

		env0 = new Envelope1D(0, 4);
		env1 = new Envelope1D(6, 7);
		env2 = new Envelope1D(9, 10);
		env3 = new Envelope1D(9, 11);
		env4 = new Envelope1D(7, 12);
		env5 = new Envelope1D(13, 15);
		env6 = new Envelope1D(1, 6);
		env7 = new Envelope1D(3, 3);
		env8 = new Envelope1D(8, 8);

		intervals.clear();
		intervals.add(env0);
		intervals.add(env1);
		intervals.add(env2);
		intervals.add(env3);
		intervals.add(env4);
		intervals.add(env5);
		intervals.add(env6);
		intervals.add(env7);
		intervals.add(env8);

		IntervalTreeImpl intervalTree2 = new IntervalTreeImpl(true);
		construct(intervalTree2, intervals);

		intervalTree2.insert(0);
		intervalTree2.insert(1);
		intervalTree2.insert(2);
		intervalTree2.insert(3);
		intervalTree2.insert(4);
		intervalTree2.insert(5);
		intervalTree2.insert(6);
		intervalTree2.insert(7);
		intervalTree2.insert(8);

		iterator = intervalTree2.getIterator(new Envelope1D(8, 8), 0.0);

		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 2);

		iterator.resetIterator(new Envelope1D(3, 7), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 5);

		iterator.resetIterator(new Envelope1D(1, 3), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 3);

		iterator.resetIterator(new Envelope1D(6, 9), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 6);

		iterator.resetIterator(new Envelope1D(10, 14), 0.0);
		counter = 0;
		while (iterator.next() != -1)
			counter++;
		assertTrue(counter == 4);

		env0 = new Envelope1D(11, 14);
		env1 = new Envelope1D(21, 36);
		env2 = new Envelope1D(15, 19);
		env3 = new Envelope1D(3, 8);
		env4 = new Envelope1D(34, 38);
		env5 = new Envelope1D(23, 27);
		env6 = new Envelope1D(6, 36);

		intervals.clear();
		intervals.add(env0);
		intervals.add(env1);
		intervals.add(env2);
		intervals.add(env3);
		intervals.add(env4);
		intervals.add(env5);
		intervals.add(env6);

		IntervalTreeImpl intervalTree3 = new IntervalTreeImpl(false);
		construct(intervalTree3, intervals);
		iterator = intervalTree3.getIterator(new Envelope1D(50, 50), 0.0);
		assert (iterator.next() == -1);
	}

	@Test
	public static void testIntervalTreeRandomConstruction() {
		@SuppressWarnings("unused")
		int pointcount = 0;
		int passcount = 1000;
		int figureSize = 50;
		Envelope env = new Envelope();
		env.setCoords(-10000, -10000, 10000, 10000);
		RandomCoordinateGenerator generator = new RandomCoordinateGenerator(
				Math.max(figureSize, 10000), env, 0.001);
		Random random = new Random(2013);
		int rand_max = 98765;
		ArrayList<Envelope1D> intervals = new ArrayList<Envelope1D>();
		AttributeStreamOfInt8 intervalsFound = new AttributeStreamOfInt8(0);

		for (int i = 0; i < passcount; i++) {
			int r = figureSize;
			if (r < 3)
				continue;
			Polygon poly = new Polygon();
			Point pt;
			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean bRandomNew = (r > 10)
						&& ((1.0 * rand) / rand_max > 0.95);
				pt = generator.GetRandomCoord();
				if (j == 0 || bRandomNew)
					poly.startPath(pt);
				else
					poly.lineTo(pt);
			}

			{
				intervals.clear();
				SegmentIterator seg_iter = poly.querySegmentIterator();
				Envelope1D interval;

				Envelope1D range = poly.queryInterval(
						VertexDescription.Semantics.POSITION, 0);
				range.vmin -= 0.01;
				range.vmax += 0.01;

				while (seg_iter.nextPath()) {
					while (seg_iter.hasNextSegment()) {
						Segment segment = seg_iter.nextSegment();
						interval = segment.queryInterval(
								VertexDescription.Semantics.POSITION, 0);
						intervals.add(interval);
					}
				}

				intervalsFound.resize(intervals.size(), 0);

				// Just test construction for assertions
				IntervalTreeImpl intervalTree = new IntervalTreeImpl(true);
				construct(intervalTree, intervals);

				for (int j = 0; j < intervals.size(); j++)
					intervalTree.insert(j);

				IntervalTreeImpl.IntervalTreeIteratorImpl iterator = intervalTree
						.getIterator(range, 0.0);

				int count = 0;
				int handle;
				while ((handle = iterator.next()) != -1) {
					count++;
					intervalsFound.write(handle, (byte) 1);
				}

				assertTrue(count == intervals.size());

				for (int j = 0; j < intervalsFound.size(); j++) {
					interval = intervals.get(j);
					assertTrue(intervalsFound.read(j) == 1);
				}

				for (int j = 0; j < intervals.size() >> 1; j++)
					intervalTree.remove(j);

				iterator.resetIterator(range, 0.0);

				count = 0;
				while ((handle = iterator.next()) != -1) {
					count++;
					intervalsFound.write(handle, (byte) 1);
				}

				assertTrue(count == intervals.size() - (intervals.size() >> 1));

				for (int j = (intervals.size() >> 1); j < intervals.size(); j++)
					intervalTree.remove(j);
			}
		}
	}
}
