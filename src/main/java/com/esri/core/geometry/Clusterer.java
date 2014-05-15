/*
 Copyright 1995-2013 Esri

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

/**
 * Implementation for the vertex clustering.
 * 
 * Used by the TopoGraph and Simplify.
 */
final class Clusterer {
	// Clusters vertices of the shape. Returns True, if some vertices were moved
	// (clustered).
	// Uses reciprocal clustering (cluster vertices that are mutual nearest
	// neighbours)
	static boolean executeReciprocal(EditShape shape, double tolerance) {
		Clusterer clusterer = new Clusterer();
		clusterer.m_shape = shape;
		clusterer.m_tolerance = tolerance;
		clusterer.m_cell_size = 2 * tolerance;
		return clusterer.clusterReciprocal_();
	}

	// Clusters vertices of the shape. Returns True, if some vertices were moved
	// (clustered).
	// Uses non-reciprocal clustering (cluster any vertices that are closer than
	// the tolerance in the first-found-first-clustered order)
	static boolean executeNonReciprocal(EditShape shape, double tolerance) {
		Clusterer clusterer = new Clusterer();
		clusterer.m_shape = shape;
		clusterer.m_tolerance = tolerance;
		clusterer.m_cell_size = 2 * tolerance;// revisit this value. Probably
												// should be m_tolerance?
		return clusterer.clusterNonReciprocal_();
	}

	// Use b_conservative == True for simplify, and False for IsSimple. This
	// makes sure Simplified shape is more robust to transformations.
	static boolean isClusterCandidate(double x_1, double y1, double x2,
			double y2, double tolerance) {
		double dx = x_1 - x2;
		double dy = y1 - y2;
		return Math.sqrt(dx * dx + dy * dy) <= tolerance;
	}

	Point2D m_origin = new Point2D();
	double m_tolerance;
	double m_cell_size;
	int[] m_bucket_array = new int[4];// temporary 4 element array
	int m_dbg_candidate_check_count;
	int m_hash_values;

	static int hashFunction_(double xi, double yi) {
		int h = NumberUtils.hash(xi);
		return NumberUtils.hash(h, yi);
	}

	class ClusterHashFunction extends IndexHashTable.HashFunction {
		IndexMultiList m_clusters;
		EditShape m_shape;
		double m_tolerance;
		double m_cell_size;
		Point2D m_origin = new Point2D();
		Point2D m_pt = new Point2D();
		Point2D m_pt_2 = new Point2D();
		int m_hash_values;

		public ClusterHashFunction(IndexMultiList clusters, EditShape shape,
				Point2D origin, double tolerance, double cell_size,
				int hash_values) {
			m_clusters = clusters;
			m_shape = shape;
			m_tolerance = tolerance;
			m_cell_size = cell_size;
			m_origin = origin;
			m_hash_values = hash_values;
			m_pt.setNaN();
			m_pt_2.setNaN();
		}

		@Override
		public int getHash(int element) {
			int vertex = m_clusters.getFirstElement(element);
			return m_shape.getUserIndex(vertex, m_hash_values);
		}

		int calculateHash(int element) {
			int vertex = m_clusters.getFirstElement(element);
			m_shape.getXY(vertex, m_pt);
			double dx = m_pt.x - m_origin.x;
			double xi = Math.round(dx / m_cell_size);
			double dy = m_pt.y - m_origin.y;
			double yi = Math.round(dy / m_cell_size);
			return hashFunction_(xi, yi);
		}

		@Override
		public boolean equal(int element_1, int element_2) {
			int xyindex_1 = m_clusters.getFirstElement(element_1);
			m_shape.getXY(xyindex_1, m_pt);
			int xyindex_2 = m_clusters.getFirstElement(element_2);
			m_shape.getXY(xyindex_2, m_pt_2);
			return isClusterCandidate(m_pt.x, m_pt.y, m_pt_2.x, m_pt_2.y,
					m_tolerance);
		}

		@Override
		public int getHash(Object element_descriptor) {
			return 0;
		}

		@Override
		public boolean equal(Object element_descriptor, int element) {
			return false;
		}
	};

	EditShape m_shape;
	IndexMultiList m_clusters;
	ClusterHashFunction m_hash_function;
	IndexHashTable m_hash_table;

	static class ClusterCandidate {
		public int cluster;
		double distance;
	};

	void getNearestNeighbourCandidate_(int xyindex, Point2D pointOfInterest,
			int bucket_ptr, ClusterCandidate candidate) {
		candidate.cluster = IndexMultiList.nullNode();
		candidate.distance = NumberUtils.doubleMax();

		Point2D pt = new Point2D();
		for (int node = bucket_ptr; node != IndexHashTable.nullNode(); node = m_hash_table
				.getNextInBucket(node)) {
			int cluster = m_hash_table.getElement(node);
			int xyind = m_clusters.getFirstElement(cluster);
			if (xyindex == xyind)
				continue;
			m_shape.getXY(xyind, pt);
			if (isClusterCandidate(pointOfInterest.x, pointOfInterest.y, pt.x,
					pt.y, m_tolerance)) {
				pt.sub(pointOfInterest);
				double l = pt.length();
				if (l < candidate.distance) {
					candidate.distance = l;
					candidate.cluster = cluster;
				}
			}
		}
	}

	void findClusterCandidate_(int xyindex, ClusterCandidate candidate) {
		Point2D pointOfInterest = new Point2D();
		m_shape.getXY(xyindex, pointOfInterest);
		double x_0 = pointOfInterest.x - m_origin.x;
		double x = x_0 / m_cell_size;
		double y0 = pointOfInterest.y - m_origin.y;
		double y = y0 / m_cell_size;

		double xi = Math.round(x - 0.5);
		double yi = Math.round(y - 0.5);

		// find the nearest neighbour in the 4 neigbouring cells.

		candidate.cluster = IndexHashTable.nullNode();
		candidate.distance = NumberUtils.doubleMax();
		ClusterCandidate c = new ClusterCandidate();
		for (double dx = 0; dx <= 1.0; dx += 1.0) {
			for (double dy = 0; dy <= 1.0; dy += 1.0) {
				int bucket_ptr = m_hash_table.getFirstInBucket(hashFunction_(xi
						+ dx, yi + dy));
				if (bucket_ptr != IndexHashTable.nullNode()) {
					getNearestNeighbourCandidate_(xyindex, pointOfInterest,
							bucket_ptr, c);
					if (c.cluster != IndexHashTable.nullNode()
							&& c.distance < candidate.distance) {
						candidate = c;
					}
				}
			}
		}
	}

	void collectClusterCandidates_(int xyindex,
			AttributeStreamOfInt32 candidates) {
		Point2D pointOfInterest = new Point2D();
		m_shape.getXY(xyindex, pointOfInterest);
		double x_0 = pointOfInterest.x - m_origin.x;
		double x = x_0 / m_cell_size;
		double y0 = pointOfInterest.y - m_origin.y;
		double y = y0 / m_cell_size;

		double xi = Math.round(x - 0.5);
		double yi = Math.round(y - 0.5);
		for (int i = 0; i < 4; i++)
			m_bucket_array[i] = -1;
		int bucket_count = 0;
		// find all nearest neighbours in the 4 neigbouring cells.
		// Note, because we check four neighbours, there should be 4 times more
		// bins in the hash table to reduce collision probability in this loop.
		for (double dx = 0; dx <= 1.0; dx += 1.0) {
			for (double dy = 0; dy <= 1.0; dy += 1.0) {
				int bucket_ptr = m_hash_table.getFirstInBucket(hashFunction_(xi
						+ dx, yi + dy));
				if (bucket_ptr != IndexHashTable.nullNode()) {
					// Check if we already have this bucket.
					// There could be a hash collision for neighbouring buckets.
					for (int j = 0; j < bucket_count; j++) {
						if (m_bucket_array[j] == bucket_ptr) {
							bucket_ptr = -1;// hash values for two neighbouring
											// cells have collided.
							break;
						}
					}

					if (bucket_ptr != -1) {
						m_bucket_array[bucket_count] = bucket_ptr;
						bucket_count++;
					}
				}
			}
		}

		for (int i = 0; i < 4; i++) {
			int bucket_ptr = m_bucket_array[i];
			if (bucket_ptr == -1)
				break;

			collectNearestNeighbourCandidates_(xyindex, pointOfInterest,
					bucket_ptr, candidates);
		}
	}

	void collectNearestNeighbourCandidates_(int xyindex,
			Point2D pointOfInterest, int bucket_ptr,
			AttributeStreamOfInt32 candidates) {
		Point2D pt = new Point2D();
		for (int node = bucket_ptr; node != IndexHashTable.nullNode(); node = m_hash_table
				.getNextInBucket(node)) {
			int cluster = m_hash_table.getElement(node);
			int xyind = m_clusters.getFirstElement(cluster);
			if (xyindex == xyind)
				continue;
			m_shape.getXY(xyind, pt);
			m_dbg_candidate_check_count++;
			if (isClusterCandidate(pointOfInterest.x, pointOfInterest.y, pt.x,
					pt.y, m_tolerance)) {
				candidates.add(node);// note that we add the cluster node
										// instead of the cluster.
			}
		}
	}

	boolean mergeClusters_(int cluster_1, int cluster_2) {
		int xyindex_1 = m_clusters.getFirstElement(cluster_1);
		int xyindex_2 = m_clusters.getFirstElement(cluster_2);
		boolean res = mergeVertices_(xyindex_1, xyindex_2);
		m_clusters.concatenateLists(cluster_1, cluster_2);
		int hash = m_hash_function.calculateHash(cluster_1);
		m_shape.setUserIndex(xyindex_1, m_hash_values, hash);
		return res;
	}

	boolean mergeVertices_(int vert_1, int vert_2) {
		Point2D pt_1 = new Point2D();
		m_shape.getXY(vert_1, pt_1);
		Point2D pt_2 = new Point2D();
		m_shape.getXY(vert_2, pt_2);

		double w_1 = m_shape.getWeight(vert_1);
		double w_2 = m_shape.getWeight(vert_2);
		double w = w_1 + w_2;
		int r = 0;
		double x = pt_1.x;
		if (pt_1.x != pt_2.x) {
			x = (pt_1.x * w_1 + pt_2.x * w_2) / w;
			r++;
		}
		double y = pt_1.y;
		if (pt_1.y != pt_2.y) {
			y = (pt_1.y * w_1 + pt_2.y * w_2) / w;
			r++;
		}

		if (r > 0)
			m_shape.setXY(vert_1, x, y);
		m_shape.setWeight(vert_1, w);
		return r != 0;
	}

	boolean clusterReciprocal_() {
		m_hash_values = m_shape.createUserIndex();
		int point_count = m_shape.getTotalPointCount();

		m_shape.getXY(m_shape.getFirstVertex(m_shape.getFirstPath(m_shape
				.getFirstGeometry())), m_origin);

		// This holds clusters.
		m_clusters.clear();
		m_clusters.reserveLists(m_shape.getTotalPointCount());
		m_clusters.reserveNodes(m_shape.getTotalPointCount());

		// Make the hash table. It serves a purpose of fine grain grid.
		// Make it 25% larger than the point count to reduce the chance of
		// collision.
		m_hash_table = new IndexHashTable((point_count * 5) / 4,
				new ClusterHashFunction(m_clusters, m_shape, m_origin,
						m_tolerance, m_cell_size, m_hash_values));
		m_hash_table.reserveElements(m_shape.getTotalPointCount());

		boolean b_clustered = false;

		// Go through all vertices stored in the m_shape and put the handles of
		// the vertices into the clusters and the hash table.
		for (int geometry = m_shape.getFirstGeometry(); geometry != -1; geometry = m_shape
				.getNextGeometry(geometry)) {
			for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
					.getNextPath(path)) {
				int vertex = m_shape.getFirstVertex(path);
				for (int index = 0, nindex = m_shape.getPathSize(path); index < nindex; index++) {
					assert (vertex != -1);
					int cluster = m_clusters.createList();
					m_clusters.addElement(cluster, vertex); // initially each
															// cluster consist
															// of a single
															// vertex
					int hash = m_hash_function.calculateHash(cluster);
					m_shape.setUserIndex(vertex, m_hash_values, hash);
					m_hash_table.addElement(cluster); // add cluster to the hash
														// table
					vertex = m_shape.getNextVertex(vertex);
				}
			}
		}

		AttributeStreamOfInt32 nn_chain = new AttributeStreamOfInt32(0);
		AttributeStreamOfDbl nn_chain_distances = new AttributeStreamOfDbl(0);// array
																				// of
																				// distances
																				// between
																				// neighbour
																				// elements
																				// on
																				// nn_chain.
																				// nn_chain_distances->size()
																				// +
																				// 1
																				// ==
																				// nn_chain->size()

		ClusterCandidate candidate = new ClusterCandidate();

		// Reciprocal nearest neighbour clustering, using a hash table.
		while (m_hash_table.size() != 0 || nn_chain.size() != 0) {
			if (nn_chain.size() == 0) {
				int cluster = m_hash_table.getAnyElement();
				nn_chain.add(cluster);
				continue;
			}

			int cluster_1 = nn_chain.getLast();
			int xyindex = m_clusters.getFirstElement(cluster_1);
			findClusterCandidate_(xyindex, candidate);
			if (candidate.cluster == IndexHashTable.nullNode()) {// no candidate
																	// for
																	// clustering
																	// has been
																	// found for
																	// the
																	// cluster_1.
				assert (nn_chain.size() == 1);
				nn_chain.removeLast();
				continue;
			}

			if (nn_chain.size() == 1) {
				m_hash_table.deleteElement(candidate.cluster);

				if (candidate.distance == 0) {// coincident points. Cluster them
												// at once.
												// cluster xyNearestNeighbour
												// with xyindex. The coordinates
												// do not need to be changed,
												// but weight need to be doubled
					m_clusters.concatenateLists(cluster_1, candidate.cluster);
					int cluster_weight_1 = m_clusters
							.getFirstElement(cluster_1);
					int cluster_weight_2 = m_clusters
							.getFirstElement(candidate.cluster);
					m_shape.setWeight(
							cluster_weight_1,
							m_shape.getWeight(cluster_weight_1)
									+ m_shape.getWeight(cluster_weight_2));
				} else
					nn_chain.add(candidate.cluster);

				continue;
			}

			assert (nn_chain.size() > 1);
			if (nn_chain.get(nn_chain.size() - 2) == candidate.cluster) {// reciprocal
																			// NN
				nn_chain.clear(false);
				b_clustered |= mergeClusters_(cluster_1, candidate.cluster);
				m_hash_table.addElement(cluster_1);
			} else {
				if (nn_chain_distances.get(nn_chain_distances.size()) <= candidate.distance) {// this
																								// neighbour
																								// is
																								// not
																								// better
																								// than
																								// the
																								// previous
																								// one
																								// (can
																								// happen
																								// when
																								// there
																								// are
																								// equidistant
																								// points).
					nn_chain.clear(false);
					b_clustered |= mergeClusters_(cluster_1, candidate.cluster);
					m_hash_table.addElement(cluster_1);
				} else {
					nn_chain.add(candidate.cluster);
					nn_chain_distances.add(candidate.distance);
				}
			}
		}// while (hashTable->size() != 0 || nn_chain->size() != 0)

		if (b_clustered) {
			applyClusterPositions_();
		}

		m_shape.removeUserIndex(m_hash_values);
		return b_clustered;
	}

	boolean clusterNonReciprocal_() {
		int point_count = m_shape.getTotalPointCount();

		{
			int geometry = m_shape.getFirstGeometry();
			int path = m_shape.getFirstPath(geometry);
			int vertex = m_shape.getFirstVertex(path);
			m_shape.getXY(vertex, m_origin);
		}

		// This holds clusters.
		if (m_clusters == null)
			m_clusters = new IndexMultiList();
		m_clusters.clear();
		m_clusters.reserveLists(m_shape.getTotalPointCount());
		m_clusters.reserveNodes(m_shape.getTotalPointCount());

		m_hash_values = m_shape.createUserIndex();

		// Make the hash table. It serves a purpose of fine grain grid.
		// Make it 25% larger than the 4 times point count to reduce the chance
		// of collision.
		// The 4 times comes from the fact that we check four neighbouring cells
		// in the grid for each point.
		m_hash_function = new ClusterHashFunction(m_clusters, m_shape,
				m_origin, m_tolerance, m_cell_size, m_hash_values);
		m_hash_table = new IndexHashTable(point_count * 5, m_hash_function); // N
																				// *
																				// 4
																				// *
																				// 1.25
																				// =
																				// N
																				// *
																				// 5.
		m_hash_table.reserveElements(m_shape.getTotalPointCount());

		boolean b_clustered = false;

		// Go through all vertices stored in the m_shape and put the handles of
		// the vertices into the clusters and the hash table.
		for (int geometry = m_shape.getFirstGeometry(); geometry != -1; geometry = m_shape
				.getNextGeometry(geometry)) {
			for (int path = m_shape.getFirstPath(geometry); path != -1; path = m_shape
					.getNextPath(path)) {
				int vertex = m_shape.getFirstVertex(path);
				for (int index = 0, nindex = m_shape.getPathSize(path); index < nindex; index++) {
					assert (vertex != -1);
					int cluster = m_clusters.createList();
					m_clusters.addElement(cluster, vertex); // initially each
															// cluster consist
															// of a single
															// vertex
					int hash = m_hash_function.calculateHash(cluster);
					m_shape.setUserIndex(vertex, m_hash_values, hash);
					m_hash_table.addElement(cluster); // add cluster to the hash
														// table
					vertex = m_shape.getNextVertex(vertex);
				}
			}
		}

		// m_hash_table.dbg_print_bucket_histogram_();

		{// scope for candidates array
			AttributeStreamOfInt32 candidates = new AttributeStreamOfInt32(0);
			while (m_hash_table.size() != 0) {
				int node = m_hash_table.getAnyNode();
				assert (node != IndexHashTable.nullNode());
				int cluster_1 = m_hash_table.getElement(node);
				m_hash_table.deleteNode(node);
				int xyindex = m_clusters.getFirstElement(cluster_1);
				collectClusterCandidates_(xyindex, candidates);
				if (candidates.size() == 0) {// no candidate for clustering has
												// been found for the cluster_1.
					continue;
				}

				for (int candidate_index = 0, ncandidates = candidates.size(); candidate_index < ncandidates; candidate_index++) {
					int cluster_node = candidates.get(candidate_index);
					int cluster = m_hash_table.getElement(cluster_node);
					m_hash_table.deleteNode(cluster_node);
					b_clustered |= mergeClusters_(cluster_1, cluster);
				}
				m_hash_table.addElement(cluster_1);
				candidates.clear(false);
			}
		}

		if (b_clustered) {
			applyClusterPositions_();
		}

		// m_hash_table.reset();
		// m_hash_function.reset();
		m_shape.removeUserIndex(m_hash_values);

		return b_clustered;
	}

	void applyClusterPositions_() {
		Point2D cluster_pt = new Point2D();
		// move vertices to the clustered positions.
		for (int list = m_clusters.getFirstList(); list != IndexMultiList
				.nullNode(); list = m_clusters.getNextList(list)) {
			int node = m_clusters.getFirst(list);
			assert (node != IndexMultiList.nullNode());
			int vertex = m_clusters.getElement(node);
			m_shape.getXY(vertex, cluster_pt);
			for (node = m_clusters.getNext(node); node != IndexMultiList
					.nullNode(); node = m_clusters.getNext(node)) {
				int vertex_1 = m_clusters.getElement(node);
				m_shape.setXY(vertex_1, cluster_pt);
			}
		}
	}

	Clusterer() {
		m_hash_values = -1;
		m_dbg_candidate_check_count = 0;
	}

}
