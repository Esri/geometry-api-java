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

//Implementation of the cracking and clustering algorithm.
//Cracks and clusters all segments and vertices in the EditShape.

final class CrackAndCluster {
	private EditShape m_shape = null;
	private ProgressTracker m_progressTracker = null;
	private double m_tolerance;

	private CrackAndCluster(ProgressTracker progressTracker) {
		m_progressTracker = progressTracker;
	}

	public static boolean execute(EditShape shape, double tolerance,
			ProgressTracker progressTracker) {
		CrackAndCluster cracker = new CrackAndCluster(progressTracker);
		cracker.m_shape = shape;
		cracker.m_tolerance = tolerance;
		return cracker._do();
	}

	private boolean _cluster(double toleranceCluster) {
		boolean res = Clusterer.executeNonReciprocal(m_shape, toleranceCluster);
		// if (false)
		// {
		// Geometry geometry =
		// m_shape.getGeometry(m_shape.getFirstGeometry());//extract the result
		// of simplify
		// ((MultiPathImpl)geometry._GetImpl()).SaveToTextFileDbg("c:/temp/_simplifyDbg.txt");
		// }

		return res;
	}

	private boolean _crack() {
		boolean res = Cracker.execute(m_shape, m_tolerance, m_progressTracker);
		// if (false)
		// {
		// for (int geom = m_shape.getFirstGeometry(); geom != -1; geom =
		// m_shape.getNextGeometry(geom))
		// {
		// Geometry geometry = m_shape.getGeometry(geom);//extract the result of
		// simplify
		// ((MultiPathImpl)geometry._getImpl()).SaveToTextFileDbg("c:/temp/_simplifyDbg.txt");//NOTE:
		// It ovewrites the previous one!
		// }
		// }

		return res;
	}

	private boolean _do() {
		double toleranceCluster = m_tolerance * Math.sqrt(2.0) * 1.00001;
		boolean bChanged = false;
		int max_iter = m_shape.getTotalPointCount() + 10 > 30 ? 1000 : (m_shape
				.getTotalPointCount() + 10)
				* (m_shape.getTotalPointCount() + 10);
		int iter = 0;
		for (;; iter++) {
			if (iter > max_iter)
				throw new GeometryException(
						"Internal Error: max number of iterations exceeded");// too
																				// many
																				// iterations

			boolean bClustered = _cluster(toleranceCluster); // find close
																// vertices and
																// clamp them
																// together.
			bChanged |= bClustered;

			boolean bFiltered = (m_shape.filterClosePoints(toleranceCluster,
					true) != 0); // remove all degenerate segments.
			bChanged |= bFiltered;
			// _ASSERT(!m_shape.hasDegenerateSegments(toleranceCluster));
			boolean bCracked = _crack(); // crack all segments at intersection
											// points and touch points.
			bChanged |= bCracked;

			if (!bCracked)
				break;
			else {
				// Loop while cracking happens.
			}

			if (m_progressTracker != null
					&& !m_progressTracker.progress(-1, -1))
				throw new UserCancelException();
		}

		return bChanged;
	}

}
