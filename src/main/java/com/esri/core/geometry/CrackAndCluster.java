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

//Implementation of the cracking and clustering algorithm.
//Cracks and clusters all segments and vertices in the EditShape.

final class CrackAndCluster {
	private EditShape m_shape = null;
	private ProgressTracker m_progressTracker = null;
	private double m_tolerance;
	private boolean m_filter_degenerate_segments = true;

	private CrackAndCluster(ProgressTracker progressTracker) {
		m_progressTracker = progressTracker;
	}

    static boolean non_empty_points_need_to_cluster(double tolerance, Point pt1, Point pt2)
    {
      double tolerance_for_clustering = InternalUtils.adjust_tolerance_for_TE_clustering(tolerance);
      return Clusterer.isClusterCandidate_(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(), MathUtils.sqr(tolerance_for_clustering));
    }

    static Point cluster_non_empty_points(Point pt1, Point pt2, double w1, int rank1, double w2, int rank2)
    {
      if (rank1 > rank2)
      {
        return pt1;
      }
      else if (rank2 < rank1)
      {
        return pt2;
      }

      int [] rank = null;
      double [] w = null;
      Point pt = new Point();
      Clusterer.mergeVertices(pt1, pt2, w1, rank1, w2, rank2, pt, w, rank);
      return pt;
    }
	
	public static boolean execute(EditShape shape, double tolerance,
			ProgressTracker progressTracker, boolean filter_degenerate_segments) {
		CrackAndCluster cracker = new CrackAndCluster(progressTracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		cracker.m_filter_degenerate_segments = filter_degenerate_segments;
		return cracker._do();
	}

	private boolean _cluster(double toleranceCluster) {
		boolean res = Clusterer.executeNonReciprocal(m_shape, toleranceCluster);
		return res;
	}

	private boolean _crack(double tolerance_for_cracking) {
		boolean res = Cracker.execute(m_shape, tolerance_for_cracking, m_progressTracker);
		return res;
	}

	private boolean _do() {
		double tol = m_tolerance;

		// Use same tolerances as ArcObjects (2 * sqrt(2) * tolerance for
		// clustering)
		// sqrt(2) * tolerance for cracking.
		// Also, inflate the tolerances slightly to insure the simplified result
		// would not change after small rounding issues.

		final double c_factor = 1e-5;
		final double c_factor_for_needs_cracking = 1e-6;
		double tolerance_for_clustering = InternalUtils
				.adjust_tolerance_for_TE_clustering(tol);
		double tolerance_for_needs_cracking = InternalUtils
				.adjust_tolerance_for_TE_cracking(tol);
		double tolerance_for_cracking = tolerance_for_needs_cracking
				* (1.0 + c_factor);
		tolerance_for_needs_cracking *= (1.0 + c_factor_for_needs_cracking);

		// Require tolerance_for_clustering > tolerance_for_cracking >
		// tolerance_for_needs_cracking
		assert (tolerance_for_clustering > tolerance_for_cracking);
		assert (tolerance_for_cracking > tolerance_for_needs_cracking);

		// double toleranceCluster = m_tolerance * Math.sqrt(2.0) * 1.00001;
		boolean bChanged = false;
		int max_iter = m_shape.getTotalPointCount() + 10 > 30 ? 1000 : (m_shape
				.getTotalPointCount() + 10)
				* (m_shape.getTotalPointCount() + 10);
		int iter = 0;
		boolean has_point_features = m_shape.hasPointFeatures();
		for (;; iter++) {
			if (iter > max_iter)
				throw new GeometryException(
						"Internal Error: max number of iterations exceeded");// too
																				// many
																				// iterations

			boolean bClustered = _cluster(tolerance_for_clustering); // find
																		// close
			// vertices and
			// clamp them
			// together.
			bChanged |= bClustered;
			
			if (m_filter_degenerate_segments) {
				boolean bFiltered = (m_shape.filterClosePoints(
						tolerance_for_clustering, true, false) != 0); // remove all
																// degenerate
																// segments.
				bChanged |= bFiltered;
			}

			boolean b_cracked = false;
			if (iter == 0
					|| has_point_features
					|| Cracker.needsCracking(true, m_shape,
							tolerance_for_needs_cracking, null,
							m_progressTracker)) {
				// Cracks only if shape contains segments.
				b_cracked = _crack(tolerance_for_cracking); // crack all
															// segments at
															// intersection
															// points and touch
															// points. If
															// Cracked, then the
															// iteration will be
															// repeated.
				bChanged |= b_cracked;
			}

			if (!b_cracked)
				break;// was not cracked, so we can bail out.
			else {
				// Loop while cracking happens.
			}

			ProgressTracker.checkAndThrow(m_progressTracker);
		}

		return bChanged;
	}

}
