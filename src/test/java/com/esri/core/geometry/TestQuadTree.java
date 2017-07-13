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
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

public class TestQuadTree extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void test1() {

		{
			QuadTree quad_tree = new QuadTree(Envelope2D.construct(-10, -10, 10, 10), 8);

			QuadTree.QuadTreeIterator qt = quad_tree.getIterator(true);
			assertTrue(qt.next() == -1);

			qt.resetIterator(Envelope2D.construct(0, 0, 0, 0), 0);

			assertTrue(quad_tree.getIntersectionCount(Envelope2D.construct(0, 0, 0, 0), 0, 10) == 0);
			assertTrue(quad_tree.getElementCount() == 0);
		}

		Polyline polyline;
		polyline = makePolyline();

		MultiPathImpl polylineImpl = (MultiPathImpl) polyline._getImpl();
		QuadTree quadtree = buildQuadTree_(polylineImpl, false);

		Line queryline = new Line(34, 9, 66, 46);
		QuadTree.QuadTreeIterator qtIter = quadtree.getIterator();
		assertTrue(qtIter.next() == -1);

		qtIter.resetIterator(queryline, 0.0);

		int element_handle = qtIter.next();
		while (element_handle > 0) {
			int index = quadtree.getElement(element_handle);
			assertTrue(index == 6 || index == 8 || index == 14);
			element_handle = qtIter.next();
		}

		Envelope2D envelope = new Envelope2D(34, 9, 66, 46);
		Polygon queryPolygon = new Polygon();
		queryPolygon.addEnvelope(envelope, true);

		qtIter.resetIterator(queryline, 0.0);

		element_handle = qtIter.next();
		while (element_handle > 0) {
			int index = quadtree.getElement(element_handle);
			assertTrue(index == 6 || index == 8 || index == 14);
			element_handle = qtIter.next();
		}
	}

	@Test
	public static void testQuadTreeWithDuplicates() {
		int pass_count = 10;
		int figure_size = 400;
		int figure_size2 = 100;
		Envelope extent1 = new Envelope();
		extent1.setCoords(-100000, -100000, 100000, 100000);

		RandomCoordinateGenerator generator1 = new RandomCoordinateGenerator(Math.max(figure_size, 10000), extent1, 0.001);
		Random random = new Random(2013);
		int rand_max = 32;

		Polygon poly_red = new Polygon();
		Polygon poly_blue = new Polygon();

		int r = figure_size;

		for (int c = 0; c < pass_count; c++) {
			Point pt;
			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean b_random_new = r > 10 && ((1.0 * rand) / rand_max > 0.95);
				pt = generator1.GetRandomCoord();
				if (j == 0 || b_random_new)
					poly_blue.startPath(pt);
				else
					poly_blue.lineTo(pt);
			}

			Envelope2D env = new Envelope2D();

			QuadTree quad_tree_blue = buildQuadTree_((MultiPathImpl) poly_blue._getImpl(), false);
			QuadTree quad_tree_blue_duplicates = buildQuadTree_((MultiPathImpl) poly_blue._getImpl(), true);

			Envelope2D e1 = quad_tree_blue.getDataExtent();
			Envelope2D e2 = quad_tree_blue_duplicates.getDataExtent();
			assertTrue(e1.equals(e2));
			assertTrue(quad_tree_blue.getElementCount() == poly_blue.getSegmentCount());

			SegmentIterator seg_iter_blue = poly_blue.querySegmentIterator();

			poly_red.setEmpty();

			r = figure_size2;
			if (r < 3)
				continue;

			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean b_random_new = r > 10 && ((1.0 * rand) / rand_max > 0.95);
				pt = generator1.GetRandomCoord();
				if (j == 0 || b_random_new)
					poly_red.startPath(pt);
				else
					poly_red.lineTo(pt);
			}

			QuadTree.QuadTreeIterator iterator = quad_tree_blue.getIterator();
			SegmentIteratorImpl seg_iter_red = ((MultiPathImpl) poly_red._getImpl()).querySegmentIterator();

			HashMap<Integer, Boolean> map1 = new HashMap<Integer, Boolean>(0);

			int count = 0;
			int intersections_per_query = 0;
			while (seg_iter_red.nextPath()) {
				while (seg_iter_red.hasNextSegment()) {
					Segment segment_red = seg_iter_red.nextSegment();
					segment_red.queryEnvelope2D(env);

					iterator.resetIterator(env, 0.0);

					int count_upper = 0;
					int element_handle;
					while ((element_handle = iterator.next()) != -1) {
						count_upper++;
						int index = quad_tree_blue.getElement(element_handle);
						Boolean iter = (Boolean) map1.get(new Integer(index));
						if (iter == null) {
							count++;
							map1.put(new Integer(index), new Boolean(true));
						}

						intersections_per_query++;
					}

					int intersection_count = quad_tree_blue.getIntersectionCount(env, 0.0, -1);
					assertTrue(intersection_count == count_upper);
				}
			}

			seg_iter_red.resetToFirstPath();

			HashMap<Integer, Boolean> map2 = new HashMap<Integer, Boolean>(0);
			QuadTree.QuadTreeIterator iterator_duplicates = quad_tree_blue_duplicates.getIterator();

			int count_duplicates = 0;
			int intersections_per_query_duplicates = 0;
			while (seg_iter_red.nextPath()) {
				while (seg_iter_red.hasNextSegment()) {
					Segment segment_red = seg_iter_red.nextSegment();
					segment_red.queryEnvelope2D(env);

					iterator_duplicates.resetIterator(env, 0.0);

					int count_lower = 0;
					HashMap<Integer, Boolean> map_per_query = new HashMap<Integer, Boolean>(0);

					int count_upper = 0;
					int element_handle;
					while ((element_handle = iterator_duplicates.next()) != -1) {
						count_upper++;
						int index = quad_tree_blue_duplicates.getElement(element_handle);
						Boolean iter = (Boolean) map2.get(new Integer(index));
						if (iter == null) {
							count_duplicates++;
							map2.put(new Integer(index), new Boolean(true));
						}

						Boolean iter_lower = (Boolean) map_per_query.get(index);
						if (iter_lower == null) {
							count_lower++;
							intersections_per_query_duplicates++;
							map_per_query.put(new Integer(index), new Boolean(true));
						}

						int q = quad_tree_blue_duplicates.getQuad(element_handle);
						assertTrue(quad_tree_blue_duplicates.getSubTreeElementCount(q) >= quad_tree_blue_duplicates.getContainedSubTreeElementCount(q));
					}

					int intersection_count = quad_tree_blue_duplicates.getIntersectionCount(env, 0.0, -1);
					boolean b_has_data = quad_tree_blue_duplicates.hasData(env, 0.0);
					assertTrue(b_has_data || intersection_count == 0);
					assertTrue(count_lower <= intersection_count && intersection_count <= count_upper);
					assertTrue(count_upper <= 4 * count_lower);
				}
			}

			assertTrue(count == count_duplicates);
			assertTrue(intersections_per_query == intersections_per_query_duplicates);
		}
	}

	@Test
	public static void testSortedIterator() {
		int pass_count = 10;
		int figure_size = 400;
		int figure_size2 = 100;
		Envelope extent1 = new Envelope();
		extent1.setCoords(-100000, -100000, 100000, 100000);

		RandomCoordinateGenerator generator1 = new RandomCoordinateGenerator(Math.max(figure_size, 10000), extent1, 0.001);

		Random random = new Random(2013);
		int rand_max = 32;

		Polygon poly_red = new Polygon();
		Polygon poly_blue = new Polygon();

		int r = figure_size;

		for (int c = 0; c < pass_count; c++) {
			Point pt;
			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean b_random_new = r > 10 && ((1.0 * rand) / rand_max > 0.95);
				pt = generator1.GetRandomCoord();
				if (j == 0 || b_random_new)
					poly_blue.startPath(pt);
				else
					poly_blue.lineTo(pt);
			}

			Envelope2D env = new Envelope2D();

			QuadTree quad_tree_blue = buildQuadTree_((MultiPathImpl) poly_blue._getImpl(), false);

			Envelope2D e1 = quad_tree_blue.getDataExtent();
			assertTrue(quad_tree_blue.getElementCount() == poly_blue.getSegmentCount());

			SegmentIterator seg_iter_blue = poly_blue.querySegmentIterator();

			poly_red.setEmpty();

			r = figure_size2;
			if (r < 3)
				continue;

			for (int j = 0; j < r; j++) {
				int rand = random.nextInt(rand_max);
				boolean b_random_new = r > 10 && ((1.0 * rand) / rand_max > 0.95);
				pt = generator1.GetRandomCoord();
				if (j == 0 || b_random_new)
					poly_red.startPath(pt);
				else
					poly_red.lineTo(pt);
			}

			QuadTree.QuadTreeIterator iterator = quad_tree_blue.getIterator();
			SegmentIteratorImpl seg_iter_red = ((MultiPathImpl) poly_red._getImpl()).querySegmentIterator();

			HashMap<Integer, Boolean> map1 = new HashMap<Integer, Boolean>(0);

			int count = 0;
			int intersections_per_query = 0;
			while (seg_iter_red.nextPath()) {
				while (seg_iter_red.hasNextSegment()) {
					Segment segment_red = seg_iter_red.nextSegment();
					segment_red.queryEnvelope2D(env);

					iterator.resetIterator(env, 0.0);

					int count_upper = 0;
					int element_handle;
					while ((element_handle = iterator.next()) != -1) {
						count_upper++;
						int index = quad_tree_blue.getElement(element_handle);
						Boolean iter = (Boolean) map1.get(index);
						if (iter == null) {
							count++;
							map1.put(new Integer(index), new Boolean(true));
						}

						intersections_per_query++;
					}

					int intersection_count = quad_tree_blue.getIntersectionCount(env, 0.0, -1);
					assertTrue(intersection_count == count_upper);
				}
			}

			seg_iter_red.resetToFirstPath();

			HashMap<Integer, Boolean> map2 = new HashMap<Integer, Boolean>(0);
			QuadTree.QuadTreeIterator sorted_iterator = quad_tree_blue.getIterator(true);

			int count_sorted = 0;
			int intersections_per_query_sorted = 0;
			while (seg_iter_red.nextPath()) {
				while (seg_iter_red.hasNextSegment()) {
					Segment segment_red = seg_iter_red.nextSegment();
					segment_red.queryEnvelope2D(env);

					sorted_iterator.resetIterator(env, 0.0);

					int count_upper_sorted = 0;
					int element_handle;
					int last_index = -1;
					while ((element_handle = sorted_iterator.next()) != -1) {
						count_upper_sorted++;
						int index = quad_tree_blue.getElement(element_handle);
						assertTrue(last_index < index); // ensure the element handles are returned in sorted order
						last_index = index;
						Boolean iter = (Boolean) map2.get(index);
						if (iter == null) {
							count_sorted++;
							map2.put(new Integer(index), new Boolean(true));
						}

						intersections_per_query_sorted++;
					}

					int intersection_count = quad_tree_blue.getIntersectionCount(env, 0.0, -1);
					assertTrue(intersection_count == count_upper_sorted);
				}
			}

			assertTrue(count == count_sorted);
			assertTrue(intersections_per_query == intersections_per_query_sorted);
		}
	}

	@Test
	public static void test_perf_quad_tree() {
		Envelope extent1 = new Envelope();
		extent1.setCoords(-1000, -1000, 1000, 1000);

		RandomCoordinateGenerator generator1 = new RandomCoordinateGenerator(1000, extent1, 0.001);
		//HiResTimer timer;
		for (int N = 16; N <= 1024/**1024*/; N *= 2) {
			//timer.StartMeasurement();

			Envelope2D extent = new Envelope2D();
			extent.setCoords(-1000, -1000, 1000, 1000);
			HashMap<Integer, Envelope2D> data = new HashMap<Integer, Envelope2D>(0);
			QuadTree qt = new QuadTree(extent, 10);
			for (int i = 0; i < N; i++) {
				Envelope2D env = new Envelope2D();
				Point2D center = generator1.GetRandomCoord().getXY();
				double w = 10;
				env.setCoords(center, w, w);
				env.intersect(extent);
				if (env.isEmpty())
					continue;

				int h = qt.insert(i, env);
				data.put(new Integer(h), env);
			}

			int ecount = 0;
			AttributeStreamOfInt32 handles = new AttributeStreamOfInt32(0);
			QuadTree.QuadTreeIterator iter = qt.getIterator();

			Iterator<Map.Entry<Integer, Envelope2D>> pairs = data.entrySet().iterator();
			while (pairs.hasNext()) {
				Map.Entry<Integer, Envelope2D> entry = pairs.next();
				iter.resetIterator((Envelope2D) entry.getValue(), 0.001);
				boolean remove_self = false;
				for (int h = iter.next(); h != -1; h = iter.next()) {
					if (h != entry.getKey().intValue())
						handles.add(h);
					else {
						remove_self = true;
					}

					ecount++;
				}

				for (int i = 0; i < handles.size(); i++) {
					qt.removeElement(handles.get(i));//remove elements that were selected.
				}

				if (remove_self)
					qt.removeElement(entry.getKey().intValue());
				handles.resize(0);
			}

			//printf("%d %0.3f (%I64d, %f, mem %I64d)\n", N, timer.GetMilliseconds(), ecount, ecount / double(N * N), memsize);
		}
	}

	@Test
	public static void test2() {
		MultiPoint multipoint = new MultiPoint();

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 100; j++) {
				multipoint.add(i, j);
			}
		}

		Envelope2D extent = new Envelope2D();
		multipoint.queryEnvelope2D(extent);

		MultiPointImpl multipointImpl = (MultiPointImpl) multipoint._getImpl();
		QuadTree quadtree = buildQuadTree_(multipointImpl);

		QuadTree.QuadTreeIterator qtIter = quadtree.getIterator();
		assertTrue(qtIter.next() == -1);

		int count = 0;
		qtIter.resetIterator(extent, 0.0);

		while (qtIter.next() != -1) {
			count++;
		}

		assertTrue(count == 10000);
	}


	public static Polyline makePolyline() {
		Polyline poly = new Polyline();

		// 0
		poly.startPath(0, 40);
		poly.lineTo(30, 0);

		// 1
		poly.startPath(20, 70);
		poly.lineTo(45, 100);

		// 2
		poly.startPath(50, 100);
		poly.lineTo(50, 60);

		// 3
		poly.startPath(35, 25);
		poly.lineTo(65, 45);

		// 4
		poly.startPath(60, 10);
		poly.lineTo(65, 35);

		// 5
		poly.startPath(60, 60);
		poly.lineTo(100, 60);

		// 6
		poly.startPath(80, 10);
		poly.lineTo(80, 99);

		// 7
		poly.startPath(60, 60);
		poly.lineTo(65, 35);

		return poly;
	}

	static QuadTree buildQuadTree_(MultiPathImpl multipathImpl, boolean bStoreDuplicates) {
		Envelope2D extent = new Envelope2D();
		multipathImpl.queryEnvelope2D(extent);
		QuadTree quadTree = new QuadTree(extent, 8, bStoreDuplicates);
		int hint_index = -1;
		Envelope2D boundingbox = new Envelope2D();
		SegmentIteratorImpl seg_iter = multipathImpl.querySegmentIterator();
		while (seg_iter.nextPath()) {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();
				int index = seg_iter.getStartPointIndex();
				segment.queryEnvelope2D(boundingbox);
				hint_index = quadTree.insert(index, boundingbox, hint_index);
			}
		}

		return quadTree;
	}

	static QuadTree buildQuadTree_(MultiPointImpl multipointImpl) {
		Envelope2D extent = new Envelope2D();
		multipointImpl.queryEnvelope2D(extent);
		QuadTree quadTree = new QuadTree(extent, 8);
		Envelope2D boundingbox = new Envelope2D();
		Point2D pt;

		for (int i = 0; i < multipointImpl.getPointCount(); i++) {
			pt = multipointImpl.getXY(i);
			boundingbox.setCoords(pt.x, pt.y, pt.x, pt.y);
			quadTree.insert(i, boundingbox, -1);
		}

		return quadTree;
	}
}
