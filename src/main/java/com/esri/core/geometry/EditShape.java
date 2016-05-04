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

import com.esri.core.geometry.Geometry.GeometryType;

/**
 * A helper geometry structure that can store MultiPoint, Polyline, Polygon
 * geometries in linked lists. It allows constant time manipulation of geometry
 * vertices.
 */
final class EditShape {
	interface PathFlags_ {
		static final int closedPath = 1;
		static final int exteriorPath = 2;
		static final int ringAreaValid = 4;
	}

	private int m_geometryCount;
	private int m_path_count;
	private int m_point_count;
	private int m_first_geometry;
	private int m_last_geometry;

	private StridedIndexTypeCollection m_vertex_index_list;

	// ****************Vertex Data******************
	private MultiPoint m_vertices_mp; // vertex coordinates are stored here
	// Attribute_stream_of_index_type::SPtr m_indexRemap;
	private MultiPointImpl m_vertices; // Internals of m_vertices_mp
	AttributeStreamOfDbl m_xy_stream; // The xy stream of the m_vertices.
	VertexDescription m_vertex_description;// a shortcut to the vertex
											// description.
	boolean m_b_has_attributes; // a short cut to know if we have something in
								// addition to x and y.

	ArrayList<Segment> m_segments;// may be NULL if all segments a Lines,
									// otherwise contains NULLs for Line
									// segments. Curves are not NULL.
	AttributeStreamOfDbl m_weights;// may be NULL if no weights are provided.
									// NULL weights assumes weight value of 1.
	ArrayList<AttributeStreamOfInt32> m_indices;// user indices are here
	// ****************End Vertex Data**************
	StridedIndexTypeCollection m_path_index_list; // doubly connected list. Path
													// index into the Path Data
													// arrays, Prev path, next
													// path.
	// ******************Path Data******************
	AttributeStreamOfDbl m_path_areas;
	AttributeStreamOfDbl m_path_lengths;
	// Block_array<Envelope::SPtr>::SPtr m_path_envelopes;
	ArrayList<AttributeStreamOfInt32> m_pathindices;// path user indices are
													// here
	// *****************End Path Data***************
	StridedIndexTypeCollection m_geometry_index_list;
	ArrayList<AttributeStreamOfInt32> m_geometry_indices;// geometry user
															// indices are here

	// *********** Helpers for Bucket sort**************
	static class EditShapeBucketSortHelper extends ClassicSort {
		EditShape m_shape;

		EditShapeBucketSortHelper(EditShape shape) {
			m_shape = shape;
		}

		@Override
		public void userSort(int begin, int end, AttributeStreamOfInt32 indices) {
			m_shape.sortVerticesSimpleByYHelper_(indices, begin, end);
		}

		@Override
		public double getValue(int index) {
			return m_shape.getY(index);
		}
	};

	BucketSort m_bucket_sort;

	// Envelope::SPtr m_envelope; //the BBOX for all attributes
	Point m_helper_point; // a helper point for intermediate operations

	Segment getSegmentFromIndex_(int vindex) {
		return m_segments != null ? m_segments.get(vindex) : null;
	}

	void setSegmentToIndex_(int vindex, Segment seg) {
		if (m_segments == null) {
			if (seg == null)
				return;
			m_segments = new ArrayList<Segment>();
			for (int i = 0, n = m_vertices.getPointCount(); i < n; i++)
				m_segments.add(null);
		}
		m_segments.set(vindex, seg);
	}

	void setPrevPath_(int path, int prev) {
		m_path_index_list.setField(path, 1, prev);
	}

	void setNextPath_(int path, int next) {
		m_path_index_list.setField(path, 2, next);
	}

	void setPathFlags_(int path, int flags) {
		m_path_index_list.setField(path, 6, flags);
	}

	int getPathFlags_(int path) {
		return m_path_index_list.getField(path, 6);
	}

	void setPathGeometry_(int path, int geom) {
		m_path_index_list.setField(path, 7, geom);
	}

	int getPathIndex_(int path) {
		return m_path_index_list.getField(path, 0);
	}

	void setNextGeometry_(int geom, int next) {
		m_geometry_index_list.setField(geom, 1, next);
	}

	void setPrevGeometry_(int geom, int prev) {
		m_geometry_index_list.setField(geom, 0, prev);
	}

	int getGeometryIndex_(int geom) {
		return m_geometry_index_list.getField(geom, 7);
	}

	int getFirstPath_(int geom) {
		return m_geometry_index_list.getField(geom, 3);
	}

	void setFirstPath_(int geom, int firstPath) {
		m_geometry_index_list.setField(geom, 3, firstPath);
	}

	void setLastPath_(int geom, int path) {
		m_geometry_index_list.setField(geom, 4, path);
	}

	int newGeometry_(int gt) {
		// Index_type index = m_first_free_geometry;
		if (m_geometry_index_list == null)
			m_geometry_index_list = new StridedIndexTypeCollection(8);

		int index = m_geometry_index_list.newElement();
		// m_geometry_index_list.set(index + 0, -1);//prev
		// m_geometry_index_list.set(index + 1, -1);//next
		m_geometry_index_list.setField(index, 2, gt);// Geometry_type
		// m_geometry_index_list.set(index + 3, -1);//first path
		// m_geometry_index_list.set(index + 4, -1);//last path
		m_geometry_index_list.setField(index, 5, 0);// point count
		m_geometry_index_list.setField(index, 6, 0);// path count
		m_geometry_index_list.setField(index, 7,
				m_geometry_index_list.elementToIndex(index));// geometry index

		return index;
	}

	void freeGeometry_(int geom) {
		m_geometry_index_list.deleteElement(geom);
	}

	int newPath_(int geom) {
		if (m_path_index_list == null) {
			m_path_index_list = new StridedIndexTypeCollection(8);
			m_vertex_index_list = new StridedIndexTypeCollection(5);
			m_path_areas = new AttributeStreamOfDbl(0);
			m_path_lengths = new AttributeStreamOfDbl(0);
		}

		int index = m_path_index_list.newElement();
		int pindex = m_path_index_list.elementToIndex(index);
		m_path_index_list.setField(index, 0, pindex);// size
		// m_path_index_list.set(index + 1, -1);//prev
		// m_path_index_list.set(index + 2, -1);//next
		m_path_index_list.setField(index, 3, 0);// size
		// m_path_index_list.set(index + 4, -1);//first vertex handle
		// m_path_index_list.set(index + 5, -1);//last vertex handle
		m_path_index_list.setField(index, 6, 0);// path flags
		setPathGeometry_(index, geom);
		if (pindex >= m_path_areas.size()) {
			int sz = pindex < 16 ? 16 : (pindex * 3) / 2;
			m_path_areas.resize(sz);
			m_path_lengths.resize(sz);
			// if (m_path_envelopes)
			// m_path_envelopes.resize(sz);
		}
		m_path_areas.set(pindex, 0);
		m_path_lengths.set(pindex, 0);
		// if (m_path_envelopes)
		// m_path_envelopes.set(pindex, nullptr);

		m_path_count++;
		return index;
	}

	void freePath_(int path) {
		m_path_index_list.deleteElement(path);
		m_path_count--;
	}

	void freeVertex_(int vertex) {
		m_vertex_index_list.deleteElement(vertex);
		m_point_count--;
	}

	int newVertex_(int vindex) {
		assert (vindex >= 0 || vindex == -1);// vindex is not a handle

		if (m_path_index_list == null) {
			m_path_index_list = new StridedIndexTypeCollection(8);
			m_vertex_index_list = new StridedIndexTypeCollection(5);
			m_path_areas = new AttributeStreamOfDbl(0);
			m_path_lengths = new AttributeStreamOfDbl(0);
		}

		int index = m_vertex_index_list.newElement();
		int vi = vindex >= 0 ? vindex : m_vertex_index_list
				.elementToIndex(index);
		m_vertex_index_list.setField(index, 0, vi);
		if (vindex < 0) {
			if (vi >= m_vertices.getPointCount()) {
				int sz = vi < 16 ? 16 : (vi * 3) / 2;
				// m_vertices.reserveRounded(sz);
				m_vertices.resize(sz);
				if (m_segments != null) {
					for (int i = 0; i < sz; i++)
						m_segments.add(null);
				}

				if (m_weights != null)
					m_weights.resize(sz);

				m_xy_stream = (AttributeStreamOfDbl) m_vertices
						.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			}

			m_vertices.setXY(vi, -1e38, -1e38);

			if (m_segments != null)
				m_segments.set(vi, null);

			if (m_weights != null)
				m_weights.write(vi, 1.0);
		} else {
			// We do not set vertices or segments here, because we assume those
			// are set correctly already.
			// We only here to create linked list of indices on existing vertex
			// value.
			// m_segments->set(m_point_count, nullptr);
		}

		m_vertex_index_list.setField(index, 4, vi * 2);
		m_point_count++;
		return index;
	}

	void free_vertex_(int vertex) {
		m_vertex_index_list.deleteElement(vertex);
		m_point_count--;
	}

	int insertVertex_(int path, int before, Point point) {
		int prev = before != -1 ? getPrevVertex(before) : getLastVertex(path);
		int next = prev != -1 ? getNextVertex(prev) : -1;

		int vertex = newVertex_(point == null ? m_point_count : -1);
		int vindex = getVertexIndex(vertex);
		if (point != null)
			m_vertices.setPointByVal(vindex, point);

		setPathToVertex_(vertex, path);
		setNextVertex_(vertex, next);
		setPrevVertex_(vertex, prev);

		if (next != -1)
			setPrevVertex_(next, vertex);

		if (prev != -1)
			setNextVertex_(prev, vertex);

		boolean b_closed = isClosedPath(path);
		int first = getFirstVertex(path);
		if (before == -1)
			setLastVertex_(path, vertex);

		if (before == first)
			setFirstVertex_(path, vertex);

		if (b_closed && next == -1) {
			setNextVertex_(vertex, vertex);
			setPrevVertex_(vertex, vertex);
		}

		setPathSize_(path, getPathSize(path) + 1);
		int geometry = getGeometryFromPath(path);
		setGeometryVertexCount_(geometry, getPointCount(geometry) + 1);

		return vertex;
	}

	Point getHelperPoint_() {
		if (m_helper_point == null)
			m_helper_point = new Point(m_vertices.getDescription());
		return m_helper_point;
	}
	
	void setFillRule(int geom, int rule) {
	      int t = m_geometry_index_list.getField(geom, 2);
	      t &= ~(0x8000000);
	      t |= rule == Polygon.FillRule.enumFillRuleWinding ? 0x8000000 : 0;
	      m_geometry_index_list.setField(geom, 2, t);//fill rule combined with geometry type
	}

    int getFillRule(int geom) {
      int t = m_geometry_index_list.getField(geom, 2);
      return (t & 0x8000000) != 0 ? Polygon.FillRule.enumFillRuleWinding : Polygon.FillRule.enumFillRuleOddEven;
    }
    
	int addMultiPath_(MultiPath multi_path) {
		int newgeom = createGeometry(multi_path.getType(),
				multi_path.getDescription());
		if (multi_path.getType() == Geometry.Type.Polygon)
			setFillRule(newgeom, ((Polygon)multi_path).getFillRule());
		
		appendMultiPath_(newgeom, multi_path);
		return newgeom;
	}

	int addMultiPoint_(MultiPoint multi_point) {
		int newgeometry = createGeometry(multi_point.getType(),
				multi_point.getDescription());
		appendMultiPoint_(newgeometry, multi_point);
		return newgeometry;
	}

	void appendMultiPath_(int dstGeom, MultiPath multi_path) {
		MultiPathImpl mp_impl = (MultiPathImpl) multi_path._getImpl();
		// m_vertices->reserve_rounded(m_vertices->get_point_count() +
		// mp_impl->get_point_count());//ensure reallocation happens by blocks
		// so that already allocated vertices do not get reallocated.
		m_vertices_mp.add(multi_path, 0, mp_impl.getPointCount());
		m_xy_stream = (AttributeStreamOfDbl) m_vertices
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);
		boolean b_some_segments = m_segments != null
				&& mp_impl.getSegmentFlagsStreamRef() != null;

		for (int ipath = 0, npath = mp_impl.getPathCount(); ipath < npath; ipath++) {
			if (mp_impl.getPathSize(ipath) < 2) // CR249862 - Clipping geometry
												// which has empty part produces
												// a crash
				continue;

			int path = insertPath(dstGeom, -1);
			setClosedPath(path, mp_impl.isClosedPath(ipath));
			for (int ivertex = mp_impl.getPathStart(ipath), iend = mp_impl
					.getPathEnd(ipath); ivertex < iend; ivertex++) {
				int vertex = insertVertex_(path, -1, null);
				if (b_some_segments) {
					int vindex = getVertexIndex(vertex);
					if ((mp_impl.getSegmentFlags(ivertex) & (byte) SegmentFlags.enumLineSeg) != 0) {
						setSegmentToIndex_(vindex, null);
					} else {
						SegmentBuffer seg_buffer = new SegmentBuffer();
						mp_impl.getSegment(ivertex, seg_buffer, true);
						setSegmentToIndex_(vindex, seg_buffer.get());
					}
				}
			}
		}

		// {//debug
		// #ifdef DEBUG
		// for (Index_type geometry = get_first_geometry(); geometry != -1;
		// geometry = get_next_geometry(geometry))
		// {
		// for (Index_type path = get_first_path(geometry); path != -1; path =
		// get_next_path(path))
		// {
		// Index_type first = get_first_vertex(path);
		// Index_type v = first;
		// for (get_next_vertex(v); v != first; v = get_next_vertex(v))
		// {
		// assert(get_next_vertex(get_prev_vertex(v)) == v);
		// }
		// }
		// }
		// #endif
		// }
	}

	void appendMultiPoint_(int dstGeom, MultiPoint multi_point) {
		// m_vertices->reserve_rounded(m_vertices->get_point_count() +
		// multi_point.get_point_count());//ensure reallocation happens by
		// blocks so that already allocated vertices do not get reallocated.
		m_vertices_mp.add(multi_point, 0, multi_point.getPointCount());
		m_xy_stream = (AttributeStreamOfDbl) m_vertices
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);

		int path = insertPath(dstGeom, -1);
		for (int ivertex = 0, iend = multi_point.getPointCount(); ivertex < iend; ivertex++) {
			insertVertex_(path, -1, null);
		}
	}

	void splitSegmentForward_(int origin_vertex,
			SegmentIntersector intersector, int intersector_index) {
		int last_vertex = getNextVertex(origin_vertex);
		if (last_vertex == -1)
			throw GeometryException.GeometryInternalError();
		Point point = getHelperPoint_();
		int path = getPathFromVertex(origin_vertex);
		int vertex = origin_vertex;
		for (int i = 0, n = intersector
				.getResultSegmentCount(intersector_index); i < n; i++) {
			int vindex = getVertexIndex(vertex);
			int next_vertex = getNextVertex(vertex);
			Segment seg = intersector.getResultSegment(intersector_index, i);

			if (i == 0) {
				seg.queryStart(point);
				// #ifdef DEBUG
				// Point2D pt = new Point2D();
				// getXY(vertex, pt);
				// assert(Point2D.distance(point.getXY(), pt) <=
				// intersector.get_tolerance_());
				// #endif
				setPoint(vertex, point);
			}

			if (seg.getType().value() == Geometry.GeometryType.Line)
				setSegmentToIndex_(vindex, null);
			else
				setSegmentToIndex_(vindex, (Segment) Geometry._clone(seg));

			seg.queryEnd(point);
			if (i < n - 1) {
				int inserted_vertex = insertVertex_(path, next_vertex, point);
				vertex = inserted_vertex;
			} else {
				// #ifdef DEBUG
				// Point_2D pt;
				// get_xy(last_vertex, pt);
				// assert(Point_2D::distance(point->get_xy(), pt) <=
				// intersector.getTolerance_());
				// #endif
				setPoint(last_vertex, point);
				assert (last_vertex == next_vertex);
			}
		}
	}

	void splitSegmentBackward_(int origin_vertex,
			SegmentIntersector intersector, int intersector_index) {
		int last_vertex = getNextVertex(origin_vertex);
		if (last_vertex == -1)
			throw GeometryException.GeometryInternalError();
		
		Point point = getHelperPoint_();
		int path = getPathFromVertex(origin_vertex);
		int vertex = origin_vertex;
		for (int i = 0, n = intersector
				.getResultSegmentCount(intersector_index); i < n; i++) {
			int vindex = getVertexIndex(vertex);
			int next_vertex = getNextVertex(vertex);
			Segment seg = intersector.getResultSegment(intersector_index, n - i
					- 1);

			if (i == 0) {
				seg.queryEnd(point);
				// #ifdef DEBUG
				// Point2D pt = new Point2D();
				// getXY(vertex, pt);
				// assert(Point2D.distance(point.getXY(), pt) <=
				// intersector.getTolerance_());
				// #endif
				setPoint(vertex, point);
			}

			if (seg.getType().value() == Geometry.GeometryType.Line)
				setSegmentToIndex_(vindex, null);
			else
				setSegmentToIndex_(vindex, (Segment) Geometry._clone(seg));

			seg.queryStart(point);
			if (i < n - 1) {
				int inserted_vertex = insertVertex_(path, next_vertex, point);
				vertex = inserted_vertex;
			} else {
				// #ifdef DEBUG
				// Point2D pt = new Point2D();
				// getXY(last_vertex, pt);
				// assert(Point2D.distance(point.getXY(), pt) <=
				// intersector.getTolerance_());
				// #endif
				setPoint(last_vertex, point);
				assert (last_vertex == next_vertex);
			}
		}
	}

	EditShape() {
		m_path_count = 0;
		m_first_geometry = -1;
		m_last_geometry = -1;
		m_point_count = 0;
		m_geometryCount = 0;
		m_b_has_attributes = false;
		m_vertices = null;
		m_xy_stream = null;
		m_vertex_description = null;
	}

	// Total point count in all geometries
	int getTotalPointCount() {
		return m_point_count;
	}

	// Returns envelope of all coordinates.
	Envelope2D getEnvelope2D() {
		Envelope2D env = new Envelope2D();
		env.setEmpty();
		VertexIterator vert_iter = queryVertexIterator();
		Point2D pt = new Point2D();
		boolean b_first = true;
		for (int ivertex = vert_iter.next(); ivertex != -1; ivertex = vert_iter
				.next()) {
			getXY(ivertex, pt);
			if (b_first)
				env.merge(pt.x, pt.y);
			else
				env.mergeNE(pt.x, pt.y);

			b_first = false;
		}

		return env;
	}

	// Returns geometry count in the edit shape
	int getGeometryCount() {
		return m_geometryCount;
	}

	// Adds a Geometry to the Edit_shape
	int addGeometry(Geometry geometry) {
		Geometry.Type gt = geometry.getType();
		if (Geometry.isMultiPath(gt.value()))
			return addMultiPath_((MultiPath) geometry);
		if (gt == Geometry.Type.MultiPoint)
			return addMultiPoint_((MultiPoint) geometry);

		throw GeometryException.GeometryInternalError();
	}

	// Append a Geometry to the given geometry of the Edit_shape
	void appendGeometry(int dstGeometry, Geometry srcGeometry) {
		Geometry.Type gt = srcGeometry.getType();
		if (Geometry.isMultiPath(gt.value())) {
			appendMultiPath_(dstGeometry, (MultiPath) srcGeometry);
			return;
		} else if (gt.value() == Geometry.GeometryType.MultiPoint) {
			appendMultiPoint_(dstGeometry, (MultiPoint) srcGeometry);
			return;
		}

		throw GeometryException.GeometryInternalError();
	}

	// Adds a path
	int addPathFromMultiPath(MultiPath multi_path, int ipath, boolean as_polygon) {
		int newgeom = createGeometry(as_polygon ? Geometry.Type.Polygon
				: Geometry.Type.Polyline, multi_path.getDescription());

		MultiPathImpl mp_impl = (MultiPathImpl) multi_path._getImpl();
		if (multi_path.getPathSize(ipath) < 2)
			return newgeom; //return empty geometry

		// m_vertices->reserve_rounded(m_vertices->get_point_count() +
		// multi_path.get_path_size(ipath));//ensure reallocation happens by
		// blocks so that already allocated vertices do not get reallocated.
		m_vertices_mp.add(multi_path, multi_path.getPathStart(ipath),
				mp_impl.getPathEnd(ipath));
		m_xy_stream = (AttributeStreamOfDbl) m_vertices
				.getAttributeStreamRef(VertexDescription.Semantics.POSITION);

		int path = insertPath(newgeom, -1);
		setClosedPath(path, mp_impl.isClosedPath(ipath) || as_polygon);

		boolean b_some_segments = m_segments != null
				&& mp_impl.getSegmentFlagsStreamRef() != null;

		for (int ivertex = mp_impl.getPathStart(ipath), iend = mp_impl
				.getPathEnd(ipath); ivertex < iend; ivertex++) {
			int vertex = insertVertex_(path, -1, null);
			if (b_some_segments) {
				int vindex = getVertexIndex(vertex);
				if ((mp_impl.getSegmentFlags(ivertex) & SegmentFlags.enumLineSeg) != 0) {
					setSegmentToIndex_(vindex, null);
				} else {
					SegmentBuffer seg_buffer = new SegmentBuffer();
					mp_impl.getSegment(ivertex, seg_buffer, true);
					setSegmentToIndex_(vindex, seg_buffer.get());
				}
			}
		}

		return newgeom;
	}

	// Extracts a geometry from the Edit_shape. The method creates a new
	// Geometry instance and initializes it with the Edit_shape data for the
	// given geometry.
	Geometry getGeometry(int geometry) {
		int gt = getGeometryType(geometry);
		Geometry geom = InternalUtils.createGeometry(gt,
				m_vertices_mp.getDescription());
		int point_count = getPointCount(geometry);

		if (point_count == 0)
			return geom;

		if (Geometry.isMultiPath(gt)) {
			MultiPathImpl mp_impl = (MultiPathImpl) geom._getImpl();
			int path_count = getPathCount(geometry);
			AttributeStreamOfInt32 parts = (AttributeStreamOfInt32) (AttributeStreamBase
					.createIndexStream(path_count + 1));
			AttributeStreamOfInt8 pathFlags = (AttributeStreamOfInt8) (AttributeStreamBase
					.createByteStream(path_count + 1, (byte) 0));
			VertexDescription description = geom.getDescription();

			for (int iattrib = 0, nattrib = description.getAttributeCount(); iattrib < nattrib; iattrib++) {
				int semantics = description.getSemantics(iattrib);
				int ncomps = VertexDescription.getComponentCount(semantics);
				AttributeStreamBase dst_stream = AttributeStreamBase
						.createAttributeStreamWithSemantics(semantics,
								point_count);
				AttributeStreamBase src_stream = m_vertices
						.getAttributeStreamRef(semantics);
				int dst_index = 0;
				int ipath = 0;
				int nvert = 0;
				for (int path = getFirstPath(geometry); path != -1; path = getNextPath(path)) {
					byte flag_mask = 0;
					if (isClosedPath(path)) {
						flag_mask |= (byte) PathFlags.enumClosed;
					} else {
						assert (gt != Geometry.GeometryType.Polygon);
					}

					if (isExterior(path)) {
						flag_mask |= (byte) PathFlags.enumOGCStartPolygon;
					}

					if (flag_mask != 0)
						pathFlags.setBits(ipath, flag_mask);

					int path_size = getPathSize(path);
					parts.write(ipath++, nvert);
					nvert += path_size;
					if (semantics == VertexDescription.Semantics.POSITION) {
						AttributeStreamOfDbl src_stream_dbl = (AttributeStreamOfDbl) (src_stream);
						AttributeStreamOfDbl dst_stream_dbl = (AttributeStreamOfDbl) (dst_stream);
						Point2D pt = new Point2D();
						for (int vertex = getFirstVertex(path); dst_index < nvert; vertex = getNextVertex(vertex), dst_index++) {
							int src_index = getVertexIndex(vertex);
							src_stream_dbl.read(src_index * 2, pt);
							dst_stream_dbl.write(dst_index * 2, pt);
						}
					} else {
						for (int vertex = getFirstVertex(path); dst_index < nvert; vertex = getNextVertex(vertex), dst_index++) {
							int src_index = getVertexIndex(vertex);
							for (int icomp = 0; icomp < ncomps; icomp++) {
								double d = src_stream.readAsDbl(src_index
										* ncomps + icomp);
								dst_stream.writeAsDbl(dst_index * ncomps
										+ icomp, d);
							}
						}
					}
				}

				assert (nvert == point_count);// Inconsistent content in the
												// Edit_shape. Please, fix.
				assert (ipath == path_count);
				mp_impl.setAttributeStreamRef(semantics, dst_stream);
				parts.write(path_count, point_count);
			}

			mp_impl.setPathFlagsStreamRef(pathFlags);
			mp_impl.setPathStreamRef(parts);
			mp_impl.notifyModified(DirtyFlags.dirtyAll);
		} else if (gt == Geometry.GeometryType.MultiPoint) {
			MultiPointImpl mp_impl = (MultiPointImpl) geom._getImpl();
			VertexDescription description = geom.getDescription();
			// mp_impl.reserve(point_count);
			mp_impl.resize(point_count);

			for (int iattrib = 0, nattrib = description.getAttributeCount(); iattrib < nattrib; iattrib++) {
				int semantics = description.getSemantics(iattrib);
				int ncomps = VertexDescription.getComponentCount(semantics);
				AttributeStreamBase dst_stream = mp_impl
						.getAttributeStreamRef(semantics);
				// std::shared_ptr<Attribute_stream_base> dst_stream =
				// Attribute_stream_base::create_attribute_stream(semantics,
				// point_count);
				AttributeStreamBase src_stream = m_vertices
						.getAttributeStreamRef(semantics);
				int dst_index = 0;
				assert (getPathCount(geometry) == 1);
				int path = getFirstPath(geometry);
				int path_size = getPathSize(path);
				for (int vertex = getFirstVertex(path); dst_index < path_size; vertex = getNextVertex(vertex), dst_index++) {
					int src_index = getVertexIndex(vertex);
					for (int icomp = 0; icomp < ncomps; icomp++) {
						double d = src_stream.readAsDbl(src_index * ncomps
								+ icomp);
						dst_stream.writeAsDbl(dst_index * ncomps + icomp, d);
					}
				}

				mp_impl.setAttributeStreamRef(semantics, dst_stream);
			}

			mp_impl.notifyModified(DirtyFlags.dirtyAll);
		} else {
			assert (false);
		}

		return geom;
	}

	// create a new empty geometry of the given type
	int createGeometry(Geometry.Type geometry_type) {
		return createGeometry(geometry_type,
				VertexDescriptionDesignerImpl.getDefaultDescriptor2D());
	}

	// Deletes existing geometry from the edit shape and returns the next one.
	int removeGeometry(int geometry) {
		for (int path = getFirstPath(geometry); path != -1; path = removePath(path)) {
			// removing paths in a loop
		}

		int prev = getPrevGeometry(geometry);
		int next = getNextGeometry(geometry);
		if (prev != -1)
			setNextGeometry_(prev, next);
		else {
			m_first_geometry = next;
		}
		if (next != -1)
			setPrevGeometry_(next, prev);
		else {
			m_last_geometry = prev;
		}

		freeGeometry_(geometry);
		return next;
	}

	// create a new empty geometry of the given type and attribute set.
	int createGeometry(Geometry.Type geometry_type,
			VertexDescription description) {
		int newgeom = newGeometry_(geometry_type.value());
		if (m_vertices == null) {
			m_vertices_mp = new MultiPoint(description);
			m_vertices = (MultiPointImpl) m_vertices_mp._getImpl();
		} else
			m_vertices_mp.mergeVertexDescription(description);

		m_vertex_description = m_vertices_mp.getDescription();// this
																// description
																// will be a
																// merge of
																// existing
																// description
																// and the
																// description
																// of the
																// multi_path
		m_b_has_attributes = m_vertex_description.getAttributeCount() > 1;

		if (m_first_geometry == -1) {
			m_first_geometry = newgeom;
			m_last_geometry = newgeom;
		} else {
			setPrevGeometry_(newgeom, m_last_geometry);
			setNextGeometry_(m_last_geometry, newgeom);
			m_last_geometry = newgeom;
		}
		return newgeom;
	}

	// Returns the first geometry in the Edit_shape.
	int getFirstGeometry() {
		return m_first_geometry;
	}

	// Returns the next geometry in the Edit_shape. Returns -1 when there are no
	// more geometries.
	int getNextGeometry(int geom) {
		return m_geometry_index_list.getField(geom, 1);
	}

	// Returns the previous geometry in the Edit_shape. Returns -1 when there
	// are no more geometries.
	int getPrevGeometry(int geom) {
		return m_geometry_index_list.getField(geom, 0);
	}

	// Returns the type of the Geometry.
	int getGeometryType(int geom) {
		return m_geometry_index_list.getField(geom, 2) & 0x7FFFFFFF;
	}

	// Sets value to the given user index on a geometry.
	void setGeometryUserIndex(int geom, int index, int value) {
		AttributeStreamOfInt32 stream = m_geometry_indices.get(index);
		int pindex = getGeometryIndex_(geom);
		if (pindex >= stream.size())
			stream.resize(Math.max((int) (pindex * 1.25), (int) 16), -1);
		stream.write(pindex, value);
	}

	// Returns the value of the given user index of a geometry
	int getGeometryUserIndex(int geom, int index) {
		int pindex = getGeometryIndex_(geom);
		AttributeStreamOfInt32 stream = m_geometry_indices.get(index);
		if (pindex < stream.size())
			return stream.read(pindex);
		else
			return -1;
	}

	// Creates new user index on a geometry. The geometry index allows to store
	// an integer user value on the geometry.
	// Until set_geometry_user_index is called for a given geometry, the index
	// stores -1 for that geometry.
	int createGeometryUserIndex() {
		if (m_geometry_indices == null)
			m_geometry_indices = new ArrayList<AttributeStreamOfInt32>(4);

		// Try getting existing index. Use linear search. We do not expect many
		// indices to be created.
		for (int i = 0; i < m_geometry_indices.size(); i++) {
			if (m_geometry_indices.get(i) == null) {
				m_geometry_indices.set(i,
						(AttributeStreamOfInt32) AttributeStreamBase
								.createIndexStream(0));
				return i;
			}
		}

		m_geometry_indices.add((AttributeStreamOfInt32) AttributeStreamBase
				.createIndexStream(0));
		return m_geometry_indices.size() - 1;
	}

	// Removes the geometry user index.
	void removeGeometryUserIndex(int index) {
		m_geometry_indices.set(index, null);
	}

	// Returns the first path of the geometry.
	int getFirstPath(int geometry) {
		return m_geometry_index_list.getField(geometry, 3);
	}

	// Returns the first path of the geometry.
	int getLastPath(int geometry) {
		return m_geometry_index_list.getField(geometry, 4);
	}

	// Point count in a geometry
	int getPointCount(int geom) {
		return m_geometry_index_list.getField(geom, 5);
	}

	// Path count in a geometry
	int getPathCount(int geom) {
		return m_geometry_index_list.getField(geom, 6);
	}

	// Filters degenerate segments in all multipath geometries
	// Returns 1 if a non-zero length segment has been removed. -1, if only zero
	// length segments have been removed.
	// 0 if no segments have been removed.
	// When b_remove_last_vertices and the result path is < 3 for polygon or < 2
	// for polyline, it'll be removed.
	int filterClosePoints(double tolerance, boolean b_remove_last_vertices, boolean only_polygons) {
		int res = 0;
		for (int geometry = getFirstGeometry(); geometry != -1; geometry = getNextGeometry(geometry)) {
			int gt = getGeometryType(geometry);
			if (!Geometry.isMultiPath(gt))
				continue;
			if (only_polygons && gt != GeometryType.Polygon)
				continue;

			boolean b_polygon = getGeometryType(geometry) == Geometry.GeometryType.Polygon;

			for (int path = getFirstPath(geometry); path != -1;) {
				// We go from the start to the half of the path first, then we
				// go from the end to the half of the path.
				int vertex_counter = 0;
				for (int vertex = getFirstVertex(path); vertex_counter < getPathSize(path) / 2;) {
					int next = getNextVertex(vertex);
					if (next == -1)
						break;
					int vindex = getVertexIndex(vertex);
					Segment seg = getSegmentFromIndex_(vindex);
					double length = 0;
					if (seg != null) {
						length = seg.calculateLength2D();
					} else {
						int vindex_next = getVertexIndex(next);
						length = m_vertices._getShortestDistance(vindex,
								vindex_next);
					}

					if (length <= tolerance) {
						if (length == 0) {
							if (res == 0)
								res = -1;
						} else
							res = 1;

						if (next != getLastVertex(path)) {
							transferAllDataToTheVertex(next, vertex);
							removeVertex(next, true);
						}
					} else {
						vertex = getNextVertex(vertex);
					}
					vertex_counter++;
				}

				int first_vertex = getFirstVertex(path);
				for (int vertex = isClosedPath(path) ? first_vertex
						: getLastVertex(path); getPathSize(path) > 0;) {
					int prev = getPrevVertex(vertex);
					if (prev != -1) {
						int vindex_prev = getVertexIndex(prev);
						Segment seg = getSegmentFromIndex_(vindex_prev);
						double length = 0;
						if (seg != null) {
							length = seg.calculateLength2D();
						} else {
							int vindex = getVertexIndex(vertex);
							length = m_vertices._getShortestDistance(vindex,
									vindex_prev);
						}

						if (length <= tolerance) {
							if (length == 0) {
								if (res == 0)
									res = -1;
							} else
								res = 1;

							transferAllDataToTheVertex(prev, vertex);
							removeVertex(prev, false);
							if (first_vertex == prev)
								first_vertex = getFirstVertex(path);
						} else {
							vertex = getPrevVertex(vertex);
							if (vertex == first_vertex)
								break;
						}
					} else {
						removeVertex(vertex, true);// remove the last vertex in
													// the path
						if (res == 0)
							res = -1;
						break;
					}
				}

				int path_size = getPathSize(path);
				if (b_remove_last_vertices
						&& (b_polygon ? path_size < 3 : path_size < 2)) {
					path = removePath(path);
					res = path_size > 0 ? 1 : (res == 0 ? -1 : res);
				} else
					path = getNextPath(path);
			}
		}

		return res;
	}

	// Checks if there are degenerate segments in any of multipath geometries
	boolean hasDegenerateSegments(double tolerance) {
		for (int geometry = getFirstGeometry(); geometry != -1; geometry = getNextGeometry(geometry)) {
			if (!Geometry.isMultiPath(getGeometryType(geometry)))
				continue;

			boolean b_polygon = getGeometryType(geometry) == Geometry.GeometryType.Polygon;

			for (int path = getFirstPath(geometry); path != -1;) {
				int path_size = getPathSize(path);
				if (b_polygon ? path_size < 3 : path_size < 2)
					return true;

				int vertex = getFirstVertex(path);
				for (int index = 0; index < path_size; index++) {
					int next = getNextVertex(vertex);
					if (next == -1)
						break;
					int vindex = getVertexIndex(vertex);
					Segment seg = getSegmentFromIndex_(vindex);
					double length = 0;
					if (seg != null) {
						length = seg.calculateLength2D();
					} else {
						int vindex_next = getVertexIndex(next);
						length = m_vertices._getShortestDistance(vindex,
								vindex_next);
					}

					if (length <= tolerance)
						return true;

					vertex = next;
				}

				path = getNextPath(path);
			}
		}

		return false;
	}

	void transferAllDataToTheVertex(int from_vertex, int to_vertex) {
		int vindexFrom = getVertexIndex(from_vertex);
		int vindexTo = getVertexIndex(to_vertex);
		if (m_weights != null) {
			double weight = m_weights.read(vindexFrom);
			m_weights.write(vindexTo, weight);
		}

		if (m_b_has_attributes) {
			// TODO: implement copying of attributes with exception of x and y
			//
			// for (int i = 0, nattrib = 0; i < nattrib; i++)
			// {
			// m_vertices->get_attribute
			// }
		}
		// Copy user index data
		if (m_indices != null) {
			for (int i = 0, n = (int) m_indices.size(); i < n; i++) {
				if (m_indices.get(i) != null) {
					int value = getUserIndex(from_vertex, i);
					if (value != -1)
						setUserIndex(to_vertex, i, value);
				}
			}
		}
	}

	// Splits segment originating from the origingVertex split_count times at
	// splitScalar points and inserts new vertices into the shape.
	// The split is not done, when the splitScalar[i] is 0 or 1, or is equal to
	// the splitScalar[i - 1].
	// Returns the number of splits actually happend (0 if no splits have
	// happend).
	int splitSegment(int origin_vertex, double[] split_scalars, int split_count) {
		int actual_splits = 0;
		int next_vertex = getNextVertex(origin_vertex);
		if (next_vertex == -1)
			throw GeometryException.GeometryInternalError();

		int vindex = getVertexIndex(origin_vertex);
		int vindex_next = getVertexIndex(next_vertex);
		Segment seg = getSegmentFromIndex_(vindex);
		double seg_length = seg == null ? m_vertices._getShortestDistance(
				vindex, vindex_next) : seg.calculateLength2D();
		double told = 0.0;
		for (int i = 0; i < split_count; i++) {
			double t = split_scalars[i];
			if (told < t && t < 1.0) {
				double f = t;
				if (seg != null) {
					f = seg_length > 0 ? seg._calculateSubLength(t)
							/ seg_length : 0.0;
				}

				m_vertices._interpolateTwoVertices(vindex, vindex_next, f,
						getHelperPoint_());// use this call mainly to
											// interpolate the attributes. XYs
											// are interpolated incorrectly for
											// curves and are recalculated when
											// segment is cut below.
				int inserted_vertex = insertVertex_(
						getPathFromVertex(origin_vertex), next_vertex,
						getHelperPoint_());
				actual_splits++;
				if (seg != null) {
					Segment subseg = seg.cut(told, t);
					int prev_vertex = getPrevVertex(inserted_vertex);
					int vindex_prev = getVertexIndex(prev_vertex);
					setSegmentToIndex_(vindex_prev, subseg);
					setXY(inserted_vertex, subseg.getEndXY()); // fix XY
																// coordinates
																// to be
																// parameter
																// based
																// (interpolate_two_vertices_)
					if (i == split_count - 1 || split_scalars[i + 1] == 1.0) {// last
																				// chance
																				// to
																				// set
																				// last
																				// split
																				// segment
																				// here:
						Segment subseg_end = seg.cut(t, 1.0);
						setSegmentToIndex_(vindex_prev, subseg_end);
					}
				}
			}
		}

		return actual_splits;
	}

	// interpolates the attributes for the specified path between from_vertex
	// and to_vertex
	void interpolateAttributesForClosedPath(int path, int from_vertex,
			int to_vertex) {
		assert (isClosedPath(path));

		if (!m_b_has_attributes)
			return;

		double sub_length = calculateSubLength2D(path, from_vertex, to_vertex);

		if (sub_length == 0.0)
			return;

		int nattr = m_vertex_description.getAttributeCount();

		for (int iattr = 1; iattr < nattr; iattr++) {
			int semantics = m_vertex_description.getSemantics(iattr);

			int interpolation = VertexDescription.getInterpolation(semantics);
			if (interpolation == VertexDescription.Interpolation.ANGULAR)
				continue;

			int components = VertexDescription.getComponentCount(semantics);

			for (int ordinate = 0; ordinate < components; ordinate++)
				interpolateAttributesForClosedPath_(semantics, path,
						from_vertex, to_vertex, sub_length, ordinate);
		}

		return;
	}

	// calculates the length for the specified path between from_vertex and
	// to_vertex
	double calculateSubLength2D(int path, int from_vertex, int to_vertex) {
		int shape_from_index = getVertexIndex(from_vertex);
		int shape_to_index = getVertexIndex(to_vertex);

		if (shape_from_index < 0 || shape_to_index > getTotalPointCount() - 1)
			throw new IllegalArgumentException("invalid call");

		if (shape_from_index > shape_to_index) {
			if (!isClosedPath(path))
				throw new IllegalArgumentException(
						"cannot iterate across an open path");
		}

		double sub_length = 0.0;

		for (int vertex = from_vertex; vertex != to_vertex; vertex = getNextVertex(vertex)) {
			int vertex_index = getVertexIndex(vertex);
			Segment segment = getSegmentFromIndex_(vertex_index);
			if (segment != null) {
				sub_length += segment.calculateLength2D();
			} else {
				int next_vertex_index = getVertexIndex(getNextVertex(vertex));
				sub_length += m_vertices._getShortestDistance(vertex_index,
						next_vertex_index);
			}
		}

		return sub_length;
	}

	// set_point modifies the vertex and associated segments.
	void setPoint(int vertex, Point new_coord) {
		int vindex = getVertexIndex(vertex);
		m_vertices.setPointByVal(vindex, new_coord);
		Segment seg = getSegmentFromIndex_(vindex);
		if (seg != null) {
			seg.setStart(new_coord);
		}
		int prev = getPrevVertex(vertex);
		if (prev != -1) {
			int vindex_p = getVertexIndex(prev);
			Segment seg_p = getSegmentFromIndex_(vindex_p);
			if (seg_p != null) {
				seg.setEnd(new_coord);
			}
		}
	}

	// Queries point for a given vertex.
	void queryPoint(int vertex, Point point) {
		int vindex = getVertexIndex(vertex);
		m_vertices.getPointByVal(vindex, point);
		// assert(getXY(vertex) == point.getXY());
	}

	// set_xy modifies the vertex and associated segments.
	void setXY(int vertex, Point2D new_coord) {
		setXY(vertex, new_coord.x, new_coord.y);
	}

	// set_xy modifies the vertex and associated segments.
	void setXY(int vertex, double new_x, double new_y) {
		int vindex = getVertexIndex(vertex);
		m_vertices.setXY(vindex, new_x, new_y);
		Segment seg = getSegmentFromIndex_(vindex);
		if (seg != null) {
			seg.setStartXY(new_x, new_y);
		}
		int prev = getPrevVertex(vertex);
		if (prev != -1) {
			int vindex_p = getVertexIndex(prev);
			Segment seg_p = getSegmentFromIndex_(vindex_p);
			if (seg_p != null) {
				seg.setEndXY(new_x, new_y);
			}
		}
	}

	Point2D getXY(int vertex) {
		Point2D pt = new Point2D();
		int vindex = getVertexIndex(vertex);
		pt.setCoords(m_vertices.getXY(vindex));
		return pt;
	}

	// Returns the coordinates of the vertex.
	void getXY(int vertex, Point2D ptOut) {
		int vindex = getVertexIndex(vertex);
		ptOut.setCoords(m_vertices.getXY(vindex));
	}

	void getXYWithIndex(int index, Point2D ptOut) {
		m_xy_stream.read(2 * index, ptOut);
	}

	// Gets the attribute for the given semantics and ordinate.
	double getAttributeAsDbl(int semantics, int vertex, int ordinate) {
		return m_vertices.getAttributeAsDbl(semantics, getVertexIndex(vertex),
				ordinate);
	}

	// Sets the attribute for the given semantics and ordinate.
	void setAttribute(int semantics, int vertex, int ordinate, double value) {
		m_vertices.setAttribute(semantics, getVertexIndex(vertex), ordinate,
				value);
	}

	// Sets the attribute for the given semantics and ordinate.
	void setAttribute(int semantics, int vertex, int ordinate, int value) {
		m_vertices.setAttribute(semantics, getVertexIndex(vertex), ordinate,
				value);
	}

	// Returns a reference to the vertex description
	VertexDescription getVertexDescription() {
		return m_vertex_description;
	}

	int getMinPathVertexY(int path) {
		int first_vert = getFirstVertex(path);
		int minv = first_vert;
		int vert = getNextVertex(first_vert);
		while (vert != -1 && vert != first_vert) {
			if (compareVerticesSimpleY_(vert, minv) < 0)
				minv = vert;
			vert = getNextVertex(vert);
		}
		return minv;
	}

	// Returns an index value for the vertex inside of the underlying array of
	// vertices.
	// This index is for the use with the get_xy_with_index. get_xy is
	// equivalent to calling get_vertex_index and get_xy_with_index.
	int getVertexIndex(int vertex) {
		return m_vertex_index_list.getField(vertex, 0);
	}

	// Returns the y coordinate of the vertex.
	double getY(int vertex) {
		Point2D pt = new Point2D();
		getXY(vertex, pt);
		return pt.y;
	}

	// returns True if xy coordinates at vertices are equal.
	boolean isEqualXY(int vertex_1, int vertex_2) {
		int vindex1 = getVertexIndex(vertex_1);
		int vindex2 = getVertexIndex(vertex_2);
		return m_vertices.getXY(vindex1).isEqual(m_vertices.getXY(vindex2));
	}

	// returns True if xy coordinates at vertices are equal.
	boolean isEqualXY(int vertex, Point2D pt) {
		int vindex = getVertexIndex(vertex);
		return m_vertices.getXY(vindex).isEqual(pt);
	}

	// Sets weight to the vertex. Weight is used by clustering and cracking.
	void setWeight(int vertex, double weight) {
		if (weight < 1.0)
			weight = 1.0;

		if (m_weights == null) {
			if (weight == 1.0)
				return;

			m_weights = (AttributeStreamOfDbl) (AttributeStreamBase
					.createDoubleStream(m_vertices.getPointCount(), 1.0));
		}

		int vindex = getVertexIndex(vertex);
		if (vindex >= m_weights.size()) {
			m_weights.resize(vindex + 1, 1.0);
		}
		
		m_weights.write(vindex, weight);
	}

	double getWeight(int vertex) {
		int vindex = getVertexIndex(vertex);
		if (m_weights == null || vindex >= m_weights.size())
			return 1.0;
		
		return m_weights.read(vindex);
	}

	// Removes associated weights
	void removeWeights() {
		m_weights = null;
	}

	// Sets value to the given user index.
	void setUserIndex(int vertex, int index, int value) {
		// CHECKVERTEXHANDLE(vertex);
		AttributeStreamOfInt32 stream = m_indices.get(index);
		// assert(get_prev_vertex(vertex) != -0x7eadbeaf);//using deleted vertex
		int vindex = getVertexIndex(vertex);
		if (stream.size() < m_vertices.getPointCount())
			stream.resize(m_vertices.getPointCount(), -1);
		stream.write(vindex, value);
	}

	int getUserIndex(int vertex, int index) {
		// CHECKVERTEXHANDLE(vertex);
		int vindex = getVertexIndex(vertex);
		AttributeStreamOfInt32 stream = m_indices.get(index);
		if (vindex < stream.size()) {
			int val = stream.read(vindex);
			return val;
		} else
			return -1;
	}

	// Creates new user index. The index have random values. The index allows to
	// store an integer user value on the vertex.
	int createUserIndex() {
		if (m_indices == null)
			m_indices = new ArrayList<AttributeStreamOfInt32>(0);

		// Try getting existing index. Use linear search. We do not expect many
		// indices to be created.
		for (int i = 0; i < m_indices.size(); i++) {
			if (m_indices.get(i) == null) {
				m_indices.set(i, (AttributeStreamOfInt32) AttributeStreamBase
						.createIndexStream(0, -1));
				return i;
			}
		}

		m_indices.add((AttributeStreamOfInt32) AttributeStreamBase
				.createIndexStream(0, -1));
		return m_indices.size() - 1;
	}

	// Removes the user index.
	void removeUserIndex(int index) {
		m_indices.set(index, null);
	}

	// Returns segment, connecting currentVertex and next vertex. Returns NULL
	// if it is a Line.
	Segment getSegment(int vertex) {
		if (m_segments != null) {
			int vindex = getVertexIndex(vertex);
			return m_segments.get(vindex);
		}
		return null;
	}

	// Returns a straight line that connects this and next vertices. No
	// attributes. Returns false if no next vertex exists (end of polyline
	// part).
	// Can be used together with get_segment.
	boolean queryLineConnector(int vertex, Line line) {
		int next = getNextVertex(vertex);
		if (next == -1)
			return false;

		if (!m_b_has_attributes) {
			Point2D pt = new Point2D();
			getXY(vertex, pt);
			line.setStartXY(pt);
			getXY(next, pt);
			line.setEndXY(pt);
		} else {
			Point pt = new Point();
			queryPoint(vertex, pt);
			line.setStart(pt);
			queryPoint(next, pt);
			line.setEnd(pt);
		}

		return true;
	}

	// Inserts an empty path before the given one. If before_path is -1, adds
	// path at the end.
	int insertPath(int geometry, int before_path) {
		int prev = -1;

		if (before_path != -1) {
			if (geometry != getGeometryFromPath(before_path))
				throw GeometryException.GeometryInternalError();

			prev = getPrevPath(before_path);
		} else
			prev = getLastPath(geometry);

		int newpath = newPath_(geometry);
		if (before_path != -1)
			setPrevPath_(before_path, newpath);

		setNextPath_(newpath, before_path);
		setPrevPath_(newpath, prev);
		if (prev != -1)
			setNextPath_(prev, newpath);
		else
			setFirstPath_(geometry, newpath);

		if (before_path == -1)
			setLastPath_(geometry, newpath);

		setGeometryPathCount_(geometry, getPathCount(geometry) + 1);
		return newpath;
	}
	
    int insertClosedPath_(int geometry, int before_path, int first_vertex, int checked_vertex, boolean[] contains_checked_vertex)
    {
      int path = insertPath(geometry, -1);
      int path_size = 0;
      int vertex = first_vertex;
      boolean contains = false;
      
      while(true)
      {
        if (vertex == checked_vertex)
          contains = true;
        
        setPathToVertex_(vertex, path);
        path_size++;
        int next = getNextVertex(vertex);
        assert(getNextVertex(getPrevVertex(vertex)) == vertex);
        if (next == first_vertex)
          break;

        vertex = next;
      }

      setClosedPath(path, true);
      setPathSize_(path, path_size);
      if (contains)
        first_vertex = checked_vertex;

      setFirstVertex_(path, first_vertex);
      setLastVertex_(path, getPrevVertex(first_vertex));
      setRingAreaValid_(path, false);
      
      if (contains_checked_vertex != null) {
    	  contains_checked_vertex[0] = contains;
      }
      
      return path;
    }
	

	// Removes a path, gets rid of all its vertices, and returns the next one
	int removePath(int path) {
		int prev = getPrevPath(path);
		int next = getNextPath(path);
		int geometry = getGeometryFromPath(path);
		if (prev != -1)
			setNextPath_(prev, next);
		else {
			setFirstPath_(geometry, next);
		}
		if (next != -1)
			setPrevPath_(next, prev);
		else {
			setLastPath_(geometry, prev);
		}

		clearPath(path);

		setGeometryPathCount_(geometry, getPathCount(geometry) - 1);
		freePath_(path);
		return next;
	}

	// Clears all vertices from the path
	void clearPath(int path) {
		int first_vertex = getFirstVertex(path);
		if (first_vertex != -1) {
			// TODO: can ve do this in one shot?
			int vertex = first_vertex;
			for (int i = 0, n = getPathSize(path); i < n; i++) {
				int v = vertex;
				vertex = getNextVertex(vertex);
				freeVertex_(v);
			}
			int geometry = getGeometryFromPath(path);
			setGeometryVertexCount_(geometry, getPointCount(geometry)
					- getPathSize(path));
		}
		setPathSize_(path, 0);
	}

	// Returns the next path (-1 if there are no more paths in the geometry).
	int getNextPath(int currentPath) {
		return m_path_index_list.getField(currentPath, 2);
	}

	// Returns the previous path (-1 if there are no more paths in the
	// geometry).
	int getPrevPath(int currentPath) {
		return m_path_index_list.getField(currentPath, 1);
	}

	// Returns the number of vertices in the path.
	int getPathSize(int path) {
		return m_path_index_list.getField(path, 3);
	}

	// Returns True if the path is closed.
	boolean isClosedPath(int path) {
		return (getPathFlags_(path) & PathFlags_.closedPath) != 0;
	}

	// Makes path closed. Closed paths are circular lists. get_next_vertex
	// always succeeds
	void setClosedPath(int path, boolean b_yes_no) {
		if (isClosedPath(path) == b_yes_no)
			return;
		if (getPathSize(path) > 0) {
			int first = getFirstVertex(path);
			int last = getLastVertex(path);
			if (b_yes_no) {
				// make a circular list
				setNextVertex_(last, first);
				setPrevVertex_(first, last);
				// set segment to NULL (just in case)
				int vindex = getVertexIndex(last);
				setSegmentToIndex_(vindex, null);
			} else {
				setNextVertex_(last, -1);
				setPrevVertex_(first, -1);
				int vindex = getVertexIndex(last);
				setSegmentToIndex_(vindex, null);
			}
		}

		int oldflags = getPathFlags_(path);
		int flags = (oldflags | (int) PathFlags_.closedPath)
				- (int) PathFlags_.closedPath;// clear the bit;
		setPathFlags_(path, flags
				| (b_yes_no ? (int) PathFlags_.closedPath : 0));
	}

	// Closes all paths of the geometry (has to be a polyline or polygon).
	void closeAllPaths(int geometry) {
		if (getGeometryType(geometry) == Geometry.GeometryType.Polygon)
			return;
		if (!Geometry.isLinear(getGeometryType(geometry)))
			throw GeometryException.GeometryInternalError();

		for (int path = getFirstPath(geometry); path != -1; path = getNextPath(path)) {
			setClosedPath(path, true);
		}
	}

	// Returns geometry from path
	int getGeometryFromPath(int path) {
		return m_path_index_list.getField(path, 7);
	}

	// Returns True if the path is exterior.
	boolean isExterior(int path) {
		return (getPathFlags_(path) & PathFlags_.exteriorPath) != 0;
	}

	// Sets exterior flag
	void setExterior(int path, boolean b_yes_no) {
		int oldflags = getPathFlags_(path);
		int flags = (oldflags | (int) PathFlags_.exteriorPath)
				- (int) PathFlags_.exteriorPath;// clear the bit;
		setPathFlags_(path, flags
				| (b_yes_no ? (int) PathFlags_.exteriorPath : 0));
	}

	// Returns the ring area
	double getRingArea(int path) {
		if (isRingAreaValid_(path))
			return m_path_areas.get(getPathIndex_(path));

		Line line = new Line();
		int vertex = getFirstVertex(path);
		if (vertex == -1)
			return 0;
		Point2D pt0 = new Point2D();
		getXY(vertex, pt0);
		double area = 0;
		for (int i = 0, n = getPathSize(path); i < n; i++, vertex = getNextVertex(vertex)) {
			Segment seg = getSegment(vertex);
			if (seg == null) {
				if (!queryLineConnector(vertex, line))
					continue;

				seg = line;
			}

			double a = seg._calculateArea2DHelper(pt0.x, pt0.y);
			area += a;
		}

		setRingAreaValid_(path, true);
		m_path_areas.set(getPathIndex_(path), area);

		return area;
	}

	// Sets value to the given user index on a path.
	void setPathUserIndex(int path, int index, int value) {
		AttributeStreamOfInt32 stream = m_pathindices.get(index);
		int pindex = getPathIndex_(path);
		if (stream.size() < m_path_areas.size())
			stream.resize(m_path_areas.size(), -1);
		stream.write(pindex, value);
	}

	// Returns the value of the given user index of a path
	int getPathUserIndex(int path, int index) {
		int pindex = getPathIndex_(path);
		AttributeStreamOfInt32 stream = m_pathindices.get(index);
		if (pindex < stream.size())
			return stream.read(pindex);
		else
			return -1;
	}

	// Creates new user index on a path. The index have random values. The path
	// index allows to store an integer user value on the path.
	int createPathUserIndex() {
		if (m_pathindices == null)
			m_pathindices = new ArrayList<AttributeStreamOfInt32>(0);
		// Try getting existing index. Use linear search. We do not expect many
		// indices to be created.
		for (int i = 0; i < m_pathindices.size(); i++) {
			if (m_pathindices.get(i) == null) {
				m_pathindices.set(i,
						(AttributeStreamOfInt32) (AttributeStreamBase
								.createIndexStream(0)));
				return i;
			}
		}

		m_pathindices.add((AttributeStreamOfInt32) (AttributeStreamBase
				.createIndexStream(0)));
		return (int) (m_pathindices.size() - 1);
	}

	// Removes the path user index.
	void removePathUserIndex(int index) {
		m_pathindices.set(index, null);
	}

	// Moves a path from any geometry before a given path in the dst_geom
	// geometry. The path_handle do not change after the operation.
	// before_path can be -1, then the path is moved to the end of the dst_geom.
	void movePath(int geom, int before_path, int path_to_move) {
		if (path_to_move == -1)
			throw new IllegalArgumentException();

		if (before_path == path_to_move)
			return;

		int next = getNextPath(path_to_move);
		int prev = getPrevPath(path_to_move);
		int geom_src = getGeometryFromPath(path_to_move);
		if (prev == -1) {
			setFirstPath_(geom_src, next);
		} else {
			setNextPath_(prev, next);
		}

		if (next == -1) {
			setLastPath_(geom_src, prev);
		} else {
			setPrevPath_(next, prev);
		}

		setGeometryVertexCount_(geom_src, getPointCount(geom_src)
				- getPathSize(path_to_move));
		setGeometryPathCount_(geom_src, getPathCount(geom_src) - 1);

		if (before_path == -1)
			prev = getLastPath(geom);
		else
			prev = getPrevPath(before_path);

		setPrevPath_(path_to_move, prev);
		setNextPath_(path_to_move, before_path);
		if (before_path == -1)
			setLastPath_(geom, path_to_move);
		else
			setPrevPath_(before_path, path_to_move);
		if (prev == -1)
			setFirstPath_(geom, path_to_move);
		else
			setNextPath_(prev, path_to_move);
		setGeometryVertexCount_(geom, getPointCount(geom)
				+ getPathSize(path_to_move));
		setGeometryPathCount_(geom, getPathCount(geom) + 1);
		setPathGeometry_(path_to_move, geom);
	}

	// Adds a copy of a vertex to a path. Connects with a straight line.
	// Returns new vertex handle.
	int addVertex(int path, int vertex) {
		m_vertices.getPointByVal(getVertexIndex(vertex), getHelperPoint_());
		return insertVertex_(path, -1, getHelperPoint_());
	}

	// Removes vertex from path. Uses either left or right segments to
	// reconnect. Returns next vertex after erased one.
	int removeVertex(int vertex, boolean b_left_segment) {
		int path = getPathFromVertex(vertex);
		int prev = getPrevVertex(vertex);
		int next = getNextVertex(vertex);
		if (prev != -1)
			setNextVertex_(prev, next);

		int path_size = getPathSize(path);

		if (vertex == getFirstVertex(path)) {
			setFirstVertex_(path, path_size > 1 ? next : -1);
		}

		if (next != -1)
			setPrevVertex_(next, prev);

		if (vertex == getLastVertex(path)) {
			setLastVertex_(path, path_size > 1 ? prev : -1);
		}

		if (prev != -1 && next != -1) {
			int vindex_prev = getVertexIndex(prev);
			int vindex_next = getVertexIndex(next);
			if (b_left_segment) {
				Segment seg = getSegmentFromIndex_(vindex_prev);
				if (seg != null) {
					Point2D pt = new Point2D();
					m_vertices.getXY(vindex_next, pt);
					seg.setEndXY(pt);
				}
			} else {
				int vindex_erased = getVertexIndex(vertex);
				Segment seg = getSegmentFromIndex_(vindex_erased);
				setSegmentToIndex_(vindex_prev, seg);
				if (seg != null) {
					Point2D pt = m_vertices.getXY(vindex_prev);
					seg.setStartXY(pt);
				}
			}
		}

		setPathSize_(path, path_size - 1);
		int geometry = getGeometryFromPath(path);
		setGeometryVertexCount_(geometry, getPointCount(geometry) - 1);
		freeVertex_(vertex);
		return next;
	}

	// Returns first vertex of the given path.
	int getFirstVertex(int path) {
		return m_path_index_list.getField(path, 4);
	}

	// Returns last vertex of the given path. For the closed paths
	// get_next_vertex for the last vertex returns the first vertex.
	int getLastVertex(int path) {
		return m_path_index_list.getField(path, 5);
	}

	// Returns next vertex. Closed paths are circular lists, so get_next_vertex
	// always returns vertex. Open paths return -1 for last vertex.
	int getNextVertex(int currentVertex) {
		return m_vertex_index_list.getField(currentVertex, 2);
	}

	// Returns previous vertex. Closed paths are circular lists, so
	// get_prev_vertex always returns vertex. Open paths return -1 for first
	// vertex.
	int getPrevVertex(int currentVertex) {
		return m_vertex_index_list.getField(currentVertex, 1);
	}

	int getPrevVertex(int currentVertex, int dir) {
		return dir > 0 ? m_vertex_index_list.getField(currentVertex, 1) : m_vertex_index_list.getField(currentVertex, 2);
	}

	int getNextVertex(int currentVertex, int dir) {
		return dir > 0 ? m_vertex_index_list.getField(currentVertex, 2) : m_vertex_index_list.getField(currentVertex, 1);
	}
	
	// Returns a path the vertex belongs to.
	int getPathFromVertex(int vertex) {
		return m_vertex_index_list.getField(vertex, 3);
	}

	// Adds a copy of the point to a path. Connects with a straight line.
	// Returns new vertex handle.
	int addPoint(int path, Point point) {
		return insertVertex_(path, -1, point);
	}

	// Vertex iterator allows to go through all vertices of the Edit_shape.
	static class VertexIterator {
		private EditShape m_parent;
		private int m_geometry;
		private int m_path;
		private int m_vertex;
		private int m_first_vertex;
		private int m_index;
		boolean m_b_first;
		boolean m_b_skip_mulit_points;

		private VertexIterator(EditShape parent, int geometry, int path,
				int vertex, int first_vertex, int index,
				boolean b_skip_mulit_points) {
			m_parent = parent;
			m_geometry = geometry;
			m_path = path;
			m_vertex = vertex;
			m_index = index;
			m_b_skip_mulit_points = b_skip_mulit_points;
			m_first_vertex = first_vertex;
			m_b_first = true;
		}

		int moveToNext_() {
			if (m_b_first) {
				m_b_first = false;
				return m_vertex;
			}

			if (m_vertex != -1) {
				m_vertex = m_parent.getNextVertex(m_vertex);
				m_index++;
				if (m_vertex != -1 && m_vertex != m_first_vertex)
					return m_vertex;

				return moveToNextHelper_();// separate into another function for
											// inlining
			}

			return -1;
		}

		int moveToNextHelper_() {
			m_path = m_parent.getNextPath(m_path);
			m_index = 0;
			while (m_geometry != -1) {
				for (; m_path != -1; m_path = m_parent.getNextPath(m_path)) {
					m_vertex = m_parent.getFirstVertex(m_path);
					m_first_vertex = m_vertex;
					if (m_vertex != -1)
						return m_vertex;
				}

				m_geometry = m_parent.getNextGeometry(m_geometry);
				if (m_geometry == -1)
					break;

				if (m_b_skip_mulit_points
						&& !Geometry.isMultiPath(m_parent
								.getGeometryType(m_geometry))) {
					continue;
				}

				m_path = m_parent.getFirstPath(m_geometry);
			}

			return -1;
		}

		// moves to next vertex. Returns -1 when there are no more vertices.
		VertexIterator(VertexIterator source) {
			m_parent = source.m_parent;
			m_geometry = source.m_geometry;
			m_path = source.m_path;
			m_vertex = source.m_vertex;
			m_index = source.m_index;
			m_b_skip_mulit_points = source.m_b_skip_mulit_points;
			m_first_vertex = source.m_first_vertex;
			m_b_first = true;
		}

		public int next() {
			return moveToNext_();
		}

		public int currentGeometry() {
			assert (m_vertex != -1);
			return m_geometry;
		}

		public int currentPath() {
			assert (m_vertex != -1);
			return m_path;
		}

		public static VertexIterator create_(EditShape parent, int geometry,
				int path, int vertex, int first_vertex, int index,
				boolean b_skip_mulit_points) {
			return new VertexIterator(parent, geometry, path, vertex,
					first_vertex, index, b_skip_mulit_points);
		}
	};

	// Returns the vertex iterator that allows iteration through all vertices of
	// all paths of all geometries.
	VertexIterator queryVertexIterator() {
		return queryVertexIterator(false);
	}

	VertexIterator queryVertexIterator(VertexIterator source) {
		return new VertexIterator(source);
	}

	// Returns the vertex iterator that allows iteration through all vertices of
	// all paths of all geometries.
	// If bSkipMultiPoints is true, then the iterator will skip the Multi_point
	// vertices
	VertexIterator queryVertexIterator(boolean b_skip_multi_points) {
		int geometry = -1;
		int path = -1;
		int vertex = -1;
		int first_vertex = -1;
		int index = 0;
		boolean bFound = false;

		for (geometry = getFirstGeometry(); geometry != -1; geometry = getNextGeometry(geometry)) {
			if (b_skip_multi_points
					&& !Geometry.isMultiPath(getGeometryType(geometry)))
				continue;

			for (path = getFirstPath(geometry); path != -1; path = getNextPath(path)) {
				vertex = getFirstVertex(path);
				first_vertex = vertex;
				index = 0;
				if (vertex == -1)
					continue;

				bFound = true;
				break;
			}

			if (bFound)
				break;
		}

		return VertexIterator.create_(this, geometry, path, vertex,
				first_vertex, index, b_skip_multi_points);
	}

	// Applies affine transformation
	void applyTransformation(Transformation2D transform) {
		m_vertices_mp.applyTransformation(transform);
		if (m_segments != null) {
			for (int i = 0, n = m_segments.size(); i < n; i++) {
				if (m_segments.get(i) != null) {
					m_segments.get(i).applyTransformation(transform);
				}
			}
		}
	}

	void interpolateAttributesForClosedPath_(int semantics, int path,
			int from_vertex, int to_vertex, double sub_length, int ordinate) {
		if (from_vertex == to_vertex)
			return;

		double from_attribute = getAttributeAsDbl(semantics, from_vertex,
				ordinate);
		double to_attribute = getAttributeAsDbl(semantics, to_vertex, ordinate);
		double cumulative_length = 0.0;
		double prev_interpolated_attribute = from_attribute;

		for (int vertex = from_vertex; vertex != to_vertex; vertex = getNextVertex(vertex)) {
			setAttribute(semantics, vertex, ordinate,
					prev_interpolated_attribute);

			int vertex_index = getVertexIndex(vertex);
			Segment segment = getSegmentFromIndex_(vertex_index);
			double segment_length;

			if (segment != null) {
				segment_length = segment.calculateLength2D();
			} else {
				int next_vertex_index = getVertexIndex(getNextVertex(vertex));
				segment_length = m_vertices._getShortestDistance(vertex_index,
						next_vertex_index);
			}
			cumulative_length += segment_length;
			double t = cumulative_length / sub_length;
			prev_interpolated_attribute = MathUtils.lerp(from_attribute,  to_attribute,  t);
		}

		return;
	}

	void SetGeometryType_(int geom, int gt) {
		m_geometry_index_list.setField(geom, 2, gt);
	}

	void splitSegment_(int origin_vertex, SegmentIntersector intersector,
			int intersector_index, boolean b_forward) {
		if (b_forward) {
			splitSegmentForward_(origin_vertex, intersector, intersector_index);
		} else {
			splitSegmentBackward_(origin_vertex, intersector, intersector_index);
		}
	}

	void setPrevVertex_(int vertex, int prev) {
		m_vertex_index_list.setField(vertex, 1, prev);
	}

	void setNextVertex_(int vertex, int next) {
		m_vertex_index_list.setField(vertex, 2, next);
	}

	void setPathToVertex_(int vertex, int path) {
		m_vertex_index_list.setField(vertex, 3, path);
	}

	void setPathSize_(int path, int size) {
		m_path_index_list.setField(path, 3, size);
	}

	void setFirstVertex_(int path, int first_vertex) {
		m_path_index_list.setField(path, 4, first_vertex);
	}

	void setLastVertex_(int path, int last_vertex) {
		m_path_index_list.setField(path, 5, last_vertex);
	}

	void setGeometryPathCount_(int geom, int path_count) {
		m_geometry_index_list.setField(geom, 6, path_count);
	}

	void setGeometryVertexCount_(int geom, int vertex_count) {
		m_geometry_index_list.setField(geom, 5, vertex_count);
	}

	boolean ringParentageCheckInternal_(int vertex_1, int vertex_2) {
		if (vertex_1 == vertex_2)
			return true;
		int vprev_1 = vertex_1;
		int vprev_2 = vertex_2;
		for (int v_1 = getNextVertex(vertex_1), v_2 = getNextVertex(vertex_2); v_1 != vertex_1
				&& v_2 != vertex_2; v_1 = getNextVertex(v_1), v_2 = getNextVertex(v_2)) {
			if (v_1 == vertex_2)
				return true;
			if (v_2 == vertex_1)
				return true;

			assert (getPrevVertex(v_1) == vprev_1);// detect malformed list
			assert (getPrevVertex(v_2) == vprev_2);// detect malformed list
			vprev_1 = v_1;
			vprev_2 = v_2;
		}

		return false;
	}

	void reverseRingInternal_(int vertex) {
		int v = vertex;
		do {
			int prev = getPrevVertex(v);
			int next = getNextVertex(v);
			setNextVertex_(v, prev);
			setPrevVertex_(v, next);
			v = next;
		} while (v != vertex);
		// Path's last becomes invalid. Do not attempt to fix it here, because
		// this is not the intent of the method
		// Note: only last is invalid. other things sould not change.
	}

	void setTotalPointCount_(int count) {
		m_point_count = count;
	}

	void removePathOnly_(int path) {
		int prev = getPrevPath(path);
		int next = getNextPath(path);
		int geometry = getGeometryFromPath(path);
		if (prev != -1)
			setNextPath_(prev, next);
		else {
			setFirstPath_(geometry, next);
		}
		if (next != -1)
			setPrevPath_(next, prev);
		else {
			setLastPath_(geometry, prev);
		}

		setFirstVertex_(path, -1);
		setLastVertex_(path, -1);
		freePath_(path);
	}

	// void DbgVerifyIntegrity(int vertex);
	// void dbg_verify_vertex_counts();
	int removeVertexInternal_(int vertex, boolean b_left_segment) {
		int prev = getPrevVertex(vertex);
		int next = getNextVertex(vertex);
		if (prev != -1)
			setNextVertex_(prev, next);

		if (next != -1)
			setPrevVertex_(next, prev);

		if (prev != -1 && next != -1) {
			int vindex_prev = getVertexIndex(prev);
			int vindex_next = getVertexIndex(next);
			if (b_left_segment) {
				Segment seg = getSegmentFromIndex_(vindex_prev);
				if (seg != null) {
					Point2D pt = new Point2D();
					m_vertices.getXY(vindex_next, pt);
					seg.setEndXY(pt);
				}
			} else {
				int vindex_erased = getVertexIndex(vertex);
				Segment seg = getSegmentFromIndex_(vindex_erased);
				setSegmentToIndex_(vindex_prev, seg);
				if (seg != null) {
					Point2D pt = new Point2D();
					m_vertices.getXY(vindex_prev, pt);
					seg.setStartXY(pt);
				}
			}
		}
		freeVertex_(vertex);
		return next;
	}

	boolean isRingAreaValid_(int path) {
		return (getPathFlags_(path) & PathFlags_.ringAreaValid) != 0;
	}

	// Sets exterior flag
	void setRingAreaValid_(int path, boolean b_yes_no) {
		int oldflags = getPathFlags_(path);
		int flags = (oldflags | (int) PathFlags_.ringAreaValid)
				- (int) PathFlags_.ringAreaValid;// clear the bit;
		setPathFlags_(path, flags
				| (b_yes_no ? (int) PathFlags_.ringAreaValid : 0));
	}

	int compareVerticesSimpleY_(int v_1, int v_2) {
		Point2D pt_1 = new Point2D();
		getXY(v_1, pt_1);
		Point2D pt_2 = new Point2D();
		getXY(v_2, pt_2);
		int res = pt_1.compare(pt_2);
		return res;
	}

	int compareVerticesSimpleX_(int v_1, int v_2) {
		Point2D pt_1 = new Point2D();
		getXY(v_1, pt_1);
		Point2D pt_2 = new Point2D();
		getXY(v_2, pt_2);
		int res = pt_1.compare(pt_2);
		return res;
	}

	public static class SimplificatorVertexComparerY extends
			AttributeStreamOfInt32.IntComparator {
		EditShape parent;

		SimplificatorVertexComparerY(EditShape parent_) {
			parent = parent_;
		}

		@Override
		public int compare(int i_1, int i_2) {
			return parent.compareVerticesSimpleY_(i_1, i_2);
		}
	}

	public static class SimplificatorVertexComparerX extends
			AttributeStreamOfInt32.IntComparator {
		EditShape parent;

		SimplificatorVertexComparerX(EditShape parent_) {
			parent = parent_;
		}

		@Override
		public int compare(int i_1, int i_2) {
			return parent.compareVerticesSimpleX_(i_1, i_2);
		}
	}

	// void sort_vertices_simple_by_y_heap_merge(Dynamic_array<int>& points,
	// const Dynamic_array<int>* geoms);

	void sortVerticesSimpleByY_(AttributeStreamOfInt32 points, int begin_,
			int end_) {
		if (m_bucket_sort == null)
			m_bucket_sort = new BucketSort();
		m_bucket_sort.sort(points, begin_, end_, new EditShapeBucketSortHelper(
				this));
	}

	void sortVerticesSimpleByYHelper_(AttributeStreamOfInt32 points,
			int begin_, int end_) {
		points.Sort(begin_, end_, new SimplificatorVertexComparerY(this));
	}

	void sortVerticesSimpleByX_(AttributeStreamOfInt32 points, int begin_,
			int end_) {
		points.Sort(begin_, end_, new SimplificatorVertexComparerX(this));
	}

	// Approximate size of the structure in memory.
	// The estimated size can be very slightly less than the actual size.
	// int estimate_memory_size() const;

    boolean hasPointFeatures()
    {
      for (int geometry = getFirstGeometry(); geometry != -1; geometry = getNextGeometry(geometry))
      {
        if (!Geometry.isMultiPath(getGeometryType(geometry)))
          return true;
      }
      return false;
    }

    void swapGeometry(int geom1, int geom2)
    {
      int first_path1 = getFirstPath(geom1);
      int first_path2 = getFirstPath(geom2);
      int last_path1 = getLastPath(geom1);
      int last_path2 = getLastPath(geom2);

      for (int path = getFirstPath(geom1); path != -1; path = getNextPath(path))
      {
        setPathGeometry_(path, geom2);
      }

      for (int path = getFirstPath(geom2); path != -1; path = getNextPath(path))
      {
        setPathGeometry_(path, geom1);
      }

      setFirstPath_(geom1, first_path2);
      setFirstPath_(geom2, first_path1);
      setLastPath_(geom1, last_path2);
      setLastPath_(geom2, last_path1);

      int vc1 = getPointCount(geom1);
      int pc1 = getPathCount(geom1);
      int vc2 = getPointCount(geom2);
      int pc2 = getPathCount(geom2);

      setGeometryVertexCount_(geom1, vc2);
      setGeometryVertexCount_(geom2, vc1);
      setGeometryPathCount_(geom1, pc2);
      setGeometryPathCount_(geom2, pc1);

      int gt1 = m_geometry_index_list.getField(geom1, 2);
      int gt2 = m_geometry_index_list.getField(geom2, 2);
      m_geometry_index_list.setField(geom1, 2, gt2);
      m_geometry_index_list.setField(geom2, 2, gt1);
    }
    
}
