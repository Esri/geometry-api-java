/*
 Copyright 1995-2015 Esri

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

final class BucketSort {
	AttributeStreamOfInt32 m_buckets;
	AttributeStreamOfInt32 m_bucketed_indices;
	double m_min_value;
	double m_max_value;
	double m_dy;

	static int MAXBUCKETS = 65536;

	public BucketSort() {
		m_buckets = new AttributeStreamOfInt32(0);
		m_bucketed_indices = new AttributeStreamOfInt32(0);
		m_min_value = 1;
		m_max_value = -1;
		m_dy = NumberUtils.TheNaN;
	}

	/**
	 * Executes sort on the Bucket_sort. The result is fed into the indices
	 * array in the range between begin (inclusive) and end (exclusive). Uses
	 * user supplied sorter to execute sort on each bucket. Users either supply
	 * the sorter and use this method of Bucket_sort class, or use other methods
	 * to form the buckets and take care of bucket sorting themselves.
	 */
	public void sort(AttributeStreamOfInt32 indices, int begin, int end,
			ClassicSort sorter) {
		if (end - begin < 32) {
			sorter.userSort(begin, end, indices);
			return;
		}
		boolean b_fallback = true;
		try {
			double miny = NumberUtils.positiveInf();
			double maxy = NumberUtils.negativeInf();
			for (int i = begin; i < end; i++) {
				double y = sorter.getValue(indices.get(i));
				if (y < miny)
					miny = y;
				if (y > maxy)
					maxy = y;
			}

			if (reset(end - begin, miny, maxy, end - begin)) {
				for (int i = begin; i < end; i++) {
					int vertex = indices.get(i);
					double y = sorter.getValue(vertex);
					int bucket = getBucket(y);
					m_buckets.set(bucket, m_buckets.get(bucket) + 1);// counting
																		// values
																		// in a
																		// bucket.
					m_bucketed_indices.write(i - begin, vertex);
				}

				// Recalculate buckets to contain start positions of buckets.
				int c = m_buckets.get(0);
				m_buckets.set(0, 0);
				for (int i = 1, n = m_buckets.size(); i < n; i++) {
					int b = m_buckets.get(i);
					m_buckets.set(i, c);
					c += b;
				}

				for (int i = begin; i < end; i++) {
					int vertex = m_bucketed_indices.read(i - begin);
					double y = sorter.getValue(vertex);
					int bucket = getBucket(y);
					int bucket_index = m_buckets.get(bucket);
					indices.set(bucket_index + begin, vertex);
					m_buckets.set(bucket, bucket_index + 1);
				}

				b_fallback = false;
			}
		} catch (Exception e) {
			m_buckets.resize(0);
			m_bucketed_indices.resize(0);
		}

		if (b_fallback) {
			sorter.userSort(begin, end, indices);
			return;
		}

		int j = 0;
		for (int i = 0, n = m_buckets.size(); i < n; i++) {
			int j0 = j;
			j = m_buckets.get(i);
			if (j > j0)
				sorter.userSort(begin + j0, begin + j, indices);
		}
		assert (j == end);

		if (getBucketCount() > 100) // some heuristics to preserve memory
		{
			m_buckets.resize(0);
			m_bucketed_indices.resize(0);
		}
	}

	/**
	 * Clears and resets Bucket_sort to the new state, preparing for the
	 * accumulation of new data.
	 * 
	 * @param bucket_count
	 *            - the number of buckets. Usually equal to the number of
	 *            elements to sort.
	 * @param min_value
	 *            - the minimum value of elements to sort.
	 * @param max_value
	 *            - the maximum value of elements to sort.
	 * @param capacity
	 *            - the number of elements to sort (-1 if not known). The
	 *            bucket_count are usually equal.
	 * @return Returns False, if the bucket sort cannot be used with the given
	 *         parameters. The method also can throw out of memory exception. In
	 *         the later case, one should fall back to the regular sort.
	 */
	private boolean reset(int bucket_count, double min_value, double max_value,
			int capacity) {
		if (bucket_count < 2 || max_value == min_value)
			return false;

		int bc = Math.min(MAXBUCKETS, bucket_count);
		m_buckets.reserve(bc);
		m_buckets.resize(bc);
		m_buckets.setRange(0, 0, m_buckets.size());
		m_min_value = min_value;
		m_max_value = max_value;
		m_bucketed_indices.resize(capacity);

		m_dy = (max_value - min_value) / (bc - 1);
		return true;
	}

	/**
	 * Adds new element to the bucket builder. The value must be between
	 * min_value and max_value.
	 * 
	 * @param The
	 *            value used for bucketing.
	 * @param The
	 *            index of the element to store in the buffer. Usually it is an
	 *            index into some array, where the real elements are stored.
	 */
	private int getBucket(double value) {
		assert (value >= m_min_value && value <= m_max_value);
		int bucket = (int) ((value - m_min_value) / m_dy);
		return bucket;
	}

	/**
	 * Returns the bucket count.
	 */
	private int getBucketCount() {
		return m_buckets.size();
	}

}
