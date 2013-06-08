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

import java.util.ArrayList;

class OperatorUnionCursor extends GeometryCursor {

	private GeometryCursor m_inputGeoms;
	private ProgressTracker m_progress_tracker;
	private SpatialReferenceImpl m_spatial_reference;
	private int m_index;
	private boolean m_b_done;

	OperatorUnionCursor(GeometryCursor inputGeoms1, SpatialReference sr,
			ProgressTracker progress_tracker) {
		m_index = -1;
		m_b_done = false;
		m_inputGeoms = inputGeoms1;
		m_spatial_reference = (SpatialReferenceImpl) (sr);
		m_progress_tracker = progress_tracker;
		// Otherwise, unsupported use patternes could be produced.

		startDissolve();
	}

	@Override
	public Geometry next() {
		if (m_b_done)
			return null;

		m_b_done = true;// m_b_done is added to avoid calling
						// m_inputGeoms->next() second time after it returned
						// NULL.
		
		return dissolve_();
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}
	
	private void step(){
		if (!bFinished) {
			Geometry geom = m_inputGeoms.next();

			if ((m_progress_tracker != null)
					&& !(m_progress_tracker.progress(-1, -1)))
				throw new RuntimeException("user_canceled");

			if (geom != null) {
				if (geom.getDimension() > dimension)
				{
					GeomPair pair = new GeomPair();
					pair.init();
					pair.geom = geom;
					int sz = getVertexCount_(geom);
					pair.vertex_count = sz;
					int level = getLevel_(sz);

					{
						unionBins.clear();
						int resize = Math.max(16, level + 1);
						for (int i = 0; i < resize; i++)
							unionBins.add(null);
						binSizes.resize(resize, 0);
						unionBins.set(level, new ArrayList<GeomPair>(0));
						unionBins.get(level).add(pair);
						binSizes.set(level, sz);
						totalToUnion = 1;
						totalVertexCount = sz;
						dimension = geom.getDimension();
					}
				} else if (!geom.isEmpty()
						&& geom.getDimension() == dimension) {
					GeomPair pair = new GeomPair();
					pair.init();
					pair.geom = geom;
					int sz = getVertexCount_(geom);
					pair.vertex_count = sz;
					int level = getLevel_(sz);

					{
						int resize = Math.max(unionBins.size(), level + 1);
						if (resize > unionBins.size()) {
							int grow = resize - unionBins.size();
							for (int i = 0; i < grow; i++)
								unionBins.add(null);
						}
						binSizes.resize(resize, 0);
						if (unionBins.get(level) == null)
							unionBins
									.set(level, new ArrayList<GeomPair>(0));

						unionBins.get(level).add(pair);
						binSizes.write(level, binSizes.read(level) + sz);

						totalToUnion++;
						totalVertexCount += sz;
					}
				} else {
					// skip empty or geometries of lower dimension
				}
			} else {
				bFinished = true;
			}
		}

		while (true)// union features that are in the unionBins
		{
			if (!bFinished) {// when we are still loading geometries, union
								// geometries of the same level, starting
								// with the biggest level.
				int imax = -1;
				int maxSz = 0;
				// Find a bin that contains more than one geometry and has
				// the max vertex count.
				for (int i = 0, n = unionBins.size(); i < n; i++) {
					if (unionBins.get(i) != null
							&& unionBins.get(i).size() > 1
							&& binSizes.read(i) > binVertexThreshold) {
						if (maxSz < binSizes.read(i)) {
							maxSz = binSizes.read(i);
							imax = i;
						}
					}
				}

				if (maxSz > 0) {
					// load the found bin into the batchToUnion.
					while (unionBins.get(imax).size() > 0) {
						ArrayList<GeomPair> bin = unionBins.get(imax);
						batchToUnion.add(bin.get(bin.size() - 1));
						bin.remove(bin.size() - 1);
						totalVertexCount -= batchToUnion.get(batchToUnion
								.size() - 1).vertex_count;
						binSizes.write(
								imax,
								binSizes.read(imax)
										- batchToUnion.get(batchToUnion
												.size() - 1).vertex_count);
					}
				}
			} else if (totalToUnion > 1) {// bFinished_shared == true - we
											// loaded all geometries
				int level = 0;
				int vertexCount = 0;
				for (int i = 0, n = unionBins.size(); i < n
						&& (batchToUnion.size() < 2 || vertexCount < binVertexThreshold); i++) {
					if (unionBins.get(i) != null) {
						while (!unionBins.get(i).isEmpty()
								&& (batchToUnion.size() < 2 || vertexCount < binVertexThreshold)) {
							ArrayList<GeomPair> bin = unionBins.get(i);
							batchToUnion.add(bin.get(bin.size() - 1));
							bin.remove(bin.size() - 1);
							level = i;
							totalVertexCount -= batchToUnion
									.get(batchToUnion.size() - 1).vertex_count;
							vertexCount += batchToUnion.get(batchToUnion
									.size() - 1).vertex_count;
							binSizes.write(
									i,
									binSizes.read(i)
											- batchToUnion.get(batchToUnion
													.size() - 1).vertex_count);
							continue;
						}
					}
				}

				if (batchToUnion.size() == 1)// never happens?
				{// only one element. Put it back.
					unionBins.get(level).add(
							batchToUnion.get(batchToUnion.size() - 1));
					totalVertexCount += batchToUnion.get(batchToUnion
							.size() - 1).vertex_count;
					binSizes.write(
							level,
							binSizes.read(level)
									+ batchToUnion.get(batchToUnion.size() - 1).vertex_count);
					batchToUnion.remove(batchToUnion.size() - 1);
				}
			}

			if (!batchToUnion.isEmpty()) {
				Geometry resGeom;
				int resDim;
				ArrayList<Geometry> geoms = new ArrayList<Geometry>(0);
				geoms.ensureCapacity(batchToUnion.size());
				for (int i = 0, n = batchToUnion.size(); i < n; i++) {
					geoms.add(batchToUnion.get(i).geom);
				}

				resGeom = TopologicalOperations.dissolveDirty(geoms,
						m_spatial_reference, m_progress_tracker);
				// assert(Operator_factory_local::get_instance()->CanDoNewTopo(pair1.geom->get_geometry_type(),
				// pair2.geom->get_geometry_type()));
				// resGeom =
				// Topological_operations::dissolve(batchToUnion[0].geom,
				// batchToUnion[1].geom, m_spatial_reference,
				// m_progress_tracker);
				// Operator_factory_local::SaveJSONToTextFileDbg("c:/temp/buffer_dissolve.txt",
				// *resGeom, nullptr);
				resDim = resGeom.getDimension();

				dissolved_something = true;
				GeomPair pair = new GeomPair();
				pair.init();
				pair.geom = resGeom;
				int sz = getVertexCount_(resGeom);
				pair.vertex_count = sz;
				int level = getLevel_(sz);

				int resize = Math.max(unionBins.size() + 1, level);
				if (resize > unionBins.size()) {
					int grow = resize - unionBins.size();
					for (int i = 0; i < grow; i++)
						unionBins.add(null);
				}
				binSizes.resize(resize, 0);

				if (unionBins.get(level) == null)
					unionBins.set(level, new ArrayList<GeomPair>(0));

				unionBins.get(level).add(pair);
				binSizes.write(level, binSizes.read(level) + sz);
				totalToUnion -= (batchToUnion.size() - 1);

				batchToUnion.clear();
			} else {
				boolean bCanGo = totalToUnion == 1;
				if (bFinished)
					bLocalDone = true;

				break;
			}
		}
	}
	
	boolean bLocalDone = false;
	int dimension = -1;
	boolean bFinished = false;
	int totalToUnion = 0;
	int totalVertexCount = 0;
	int binVertexThreshold = 10000;
	boolean dissolved_something = false;

	ArrayList<GeomPair> batchToUnion = new ArrayList<GeomPair>(0);
	ArrayList<ArrayList<GeomPair>> unionBins = new ArrayList<ArrayList<GeomPair>>(
			0);
	AttributeStreamOfInt32 binSizes = new AttributeStreamOfInt32(0);

	private void startDissolve() {
		m_index = m_inputGeoms.getGeometryID();

		// Geometries are placed into the unionBins.
		// Each bin stores geometries of certain size range.
		// The bin number is calculated as log(N), where N is the number of
		// vertices in geoemtry and the log is to a
		// certain base (now it is 4).
		unionBins.ensureCapacity(128);
		binSizes.reserve(128);

		for (int i = 0; i < 16; i++)
			unionBins.add(null);

		batchToUnion.ensureCapacity(32);
	}
	
	@Override
	public boolean tock() {
		if (!m_b_done)
		{
			step();
		}
		return bFinished;
	}

	private Geometry dissolve_() {
		while (!bLocalDone) {
			step();
		}

		Geometry resGeom = null;
		for (int i = 0; i < unionBins.size(); i++) {
			if (unionBins.get(i) != null && unionBins.get(i).size() > 0)
				resGeom = unionBins.get(i).get(0).geom;
		}

		if (resGeom == null)
			return resGeom;

		if (dissolved_something) {
			OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
			OperatorSimplify simplify = (OperatorSimplify) engine
					.getOperator(Operator.Type.Simplify);
			resGeom = simplify.execute(resGeom, m_spatial_reference, false,
					m_progress_tracker);
		}

		if (resGeom.getType().value() == Geometry.GeometryType.Point) {// must
																		// return
																		// multipoint
																		// for
																		// points
			MultiPoint mp = new MultiPoint(resGeom.getDescription());
			if (!resGeom.isEmpty())
				mp.add((Point) resGeom);
			resGeom = mp;
		}

		return resGeom;
	}

	private static final class GeomPair {
		void init() {
			geom = null;
			vertex_count = -1;
		}

		Geometry geom;
		int vertex_count;
	}

	private static int getVertexCount_(Geometry geom) {
		int gt = geom.getType().value();
		if (Geometry.isMultiVertex(gt)) {
			return ((MultiVertexGeometry) geom).getPointCount();
		} else if (gt == Geometry.GeometryType.Point) {
			return 1;
		} else if (gt == Geometry.GeometryType.Envelope) {
			return 4;
		} else if (Geometry.isSegment(gt)) {
			return 2;
		} else {
			throw new GeometryException("internal error");
		}
	}

	private static int getLevel_(int sz) {// calculates logarithm of sz to base
											// 4.
		return sz > 0 ? (int) (Math.log((double) sz) / Math.log(4.0) + 0.5)
				: (int) 0;
	}
}
