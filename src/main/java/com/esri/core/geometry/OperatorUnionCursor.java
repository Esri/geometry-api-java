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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class OperatorUnionCursor extends GeometryCursor {

	private GeometryCursor m_inputGeoms;
	private ProgressTracker m_progress_tracker;
	private SpatialReferenceImpl m_spatial_reference;
	private int m_index = -1;
	private boolean m_b_done = false;
	private boolean [] m_had_geometry = new boolean[4];
	private int [] m_dim_geom_counts = new int [4];
	private boolean m_b_union_all_dimensions = false;
	private int m_max_dimension = -1;
	private int m_added_geoms = 0;
	private int m_current_dim = -1;
	
    private final static class Geom_pair
    {
      void init() { geom = null; vertex_count = -1; unioned = false; }
      Geometry geom;
      int vertex_count;
      boolean unioned;//true if geometry is a result of union operation
    }

    final static class Bin_type //bin array and the total vertex count in the bin
    {
      int bin_vertex_count = 0;
      ArrayList<Geom_pair> geometries = new ArrayList<Geom_pair>();
      
      void add_pair(Geom_pair geom)
      {
        bin_vertex_count += geom.vertex_count;
        geometries.add(geom);
      }
      void pop_pair()
      {
        bin_vertex_count -= geometries.get(geometries.size() - 1).vertex_count;
        geometries.remove(geometries.size() - 1);
      }
      Geom_pair back_pair() { return geometries.get(geometries.size() - 1); }
      int geom_count() { return geometries.size(); }
    }
    
    ArrayList< TreeMap<Integer, Bin_type> > m_union_bins = new ArrayList< TreeMap<Integer, Bin_type> >();//for each dimension there is a list of bins sorted by level

	OperatorUnionCursor(GeometryCursor inputGeoms1, SpatialReference sr,
			ProgressTracker progress_tracker) {
		m_inputGeoms = inputGeoms1;
		m_spatial_reference = (SpatialReferenceImpl) (sr);
		m_progress_tracker = progress_tracker;
	}
	
	private Geometry get_result_geometry(int dim) {
		assert (m_dim_geom_counts[dim] > 0);
		java.util.TreeMap<Integer, Bin_type> map = m_union_bins.get(dim);
		Map.Entry<Integer, Bin_type> e = map.firstEntry();
		Bin_type bin = e.getValue();

		Geometry resG;
		resG = bin.back_pair().geom;
		boolean unioned = bin.back_pair().unioned;
		map.remove(e.getKey());

		if (unioned) {
			resG = OperatorSimplify.local().execute(resG, m_spatial_reference,
					false, m_progress_tracker);
			if (dim == 0 && resG.getType() == Geometry.Type.Point) {// must
																	// return
																	// multipoint
																	// for
																	// points
				MultiPoint mp = new MultiPoint(resG.getDescription());
				if (!resG.isEmpty())
					mp.add((Point) resG);

				resG = mp;
			}
		}

		return resG;
	}

	@Override
	public Geometry next() {
		if (m_b_done && m_current_dim == m_max_dimension)
			return null;

		while (!step_()) {
		}

		if (m_max_dimension == -1)
			return null;// empty input cursor

		if (m_b_union_all_dimensions) {
			m_current_dim++;
			while (true) {
				if (m_current_dim > m_max_dimension || m_current_dim < 0)
					throw GeometryException.GeometryInternalError();

				if (m_had_geometry[m_current_dim])
					break;
			}

			m_index++;
			return get_result_geometry(m_current_dim);
		} else {
			m_index = 0;
			assert (m_max_dimension >= 0);
			m_current_dim = m_max_dimension;
			return get_result_geometry(m_max_dimension);
		}
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}
	
	private boolean step_(){
		if (m_b_done)
			return true;

		Geometry geom = null;
		if (m_inputGeoms != null)
		{
			geom = m_inputGeoms.next();
			if (geom == null) {
				m_b_done = true;
				m_inputGeoms = null;
			}
		}
			
		ProgressTracker.checkAndThrow(m_progress_tracker);

		if (geom != null) {
			int dim = geom.getDimension();
			m_had_geometry[dim] = true;
			if (dim >= m_max_dimension && !m_b_union_all_dimensions)
			{
	          add_geom(dim, false, geom);
	          if (dim > m_max_dimension && !m_b_union_all_dimensions)
	          {
	            //this geometry has higher dimension than the previously processed one
	            //Therefore we delete all lower dimensions (unless m_b_union_all_dimensions is true).
	            remove_all_bins_with_lower_dimension(dim);
	          }
			}
			else
			{
				//this geometry is skipped
			}
		} else {
			//geom is null. do nothing
		}
		
		if (m_added_geoms > 0) {
			for (int dim = 0; dim <= m_max_dimension; dim++) {
				while (m_dim_geom_counts[dim] > 1) {
					ArrayList<Geometry> batch_to_union = collect_geometries_to_union(dim);
					boolean serial_execution = true;
					if (serial_execution) {
						if (batch_to_union.size() != 0) {
							Geometry geomRes = TopologicalOperations
									.dissolveDirty(batch_to_union,
											m_spatial_reference,
											m_progress_tracker);
							add_geom(dim, true, geomRes);
						} else {
							break;
						}
					}
				}
			}
		}
		
		return m_b_done;
	}
	
	ArrayList<Geometry> collect_geometries_to_union(int dim) {
		ArrayList<Geometry> batch_to_union = new ArrayList<Geometry>();
		ArrayList<Map.Entry<Integer, Bin_type>> entriesToRemove = new ArrayList<Map.Entry<Integer, Bin_type>>();
		Set<Map.Entry<Integer, Bin_type>> set = m_union_bins.get(dim)
				.entrySet();
		for (Map.Entry<Integer, Bin_type> e : set) {
			//int level = e.getKey();
			Bin_type bin = e.getValue();

			final int binVertexThreshold = 10000;

			if (m_b_done
					|| (bin.bin_vertex_count > binVertexThreshold && bin
							.geom_count() > 1)) {
				m_dim_geom_counts[dim] -= bin.geom_count();
				m_added_geoms -= bin.geom_count();
				while (bin.geometries.size() > 0) {
					// empty geometries will be unioned too.
					batch_to_union.add(bin.back_pair().geom);
					bin.pop_pair();
				}

				entriesToRemove.add(e);
			}
		}

		set.removeAll(entriesToRemove);
		return batch_to_union;
	}
	
	private void remove_all_bins_with_lower_dimension(int dim) {
		// this geometry has higher dimension than the previously processed one
		for (int i = 0; i < dim; i++) {
			m_union_bins.get(i).clear();
			m_added_geoms -= m_dim_geom_counts[i];
			m_dim_geom_counts[i] = 0;
		}
	}

	private void add_geom(int dimension, boolean unioned, Geometry geom) {
		Geom_pair pair = new Geom_pair();
		pair.init();
		pair.geom = geom;
		int sz = get_vertex_count_(geom);
		pair.vertex_count = sz;
		int level = get_level_(sz);
		if (dimension + 1 > (int) m_union_bins.size()) {
			for (int i = 0, n = Math.max(2, dimension + 1); i < n; i++) {
				m_union_bins.add(new TreeMap<Integer, Bin_type>());
			}
		}

		Bin_type bin = m_union_bins.get(dimension).get(level);//return null if level is abscent
		if (bin == null) {
			bin = new Bin_type();
			m_union_bins.get(dimension).put(level, bin);
		}

		pair.unioned = unioned;
		bin.add_pair(pair);

		// Update global cursor state
		m_dim_geom_counts[dimension]++;
		m_added_geoms++;
		m_max_dimension = Math.max(m_max_dimension, dimension);
	}

	private static int get_level_(int sz) {// calculates logarithm of sz to base
											// 4.
		return sz > 0 ? (int) (Math.log((double) sz) / Math.log(4.0) + 0.5)
				: (int) 0;
	}

	private static int get_vertex_count_(Geometry geom) {
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
			throw GeometryException.GeometryInternalError();
		}
	}
	
	@Override
	public boolean tock() {
		return step_();
	}
}
