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

class PairwiseIntersectorImpl {
    // Quad_tree
    private MultiPathImpl m_multi_path_impl_a;
    private MultiPathImpl m_multi_path_impl_b;
    private boolean m_b_paths;
    private boolean m_b_quad_tree;
    private boolean m_b_done;
    private boolean m_b_swap_elements;
    private double m_tolerance;
    private int m_path_index;
    private int m_element_handle;
    private Envelope2D m_paths_query = new Envelope2D(); // only used for m_b_paths == true case
    private QuadTreeImpl m_quad_tree;
    private QuadTreeImpl.QuadTreeIteratorImpl m_qt_iter;
    private SegmentIteratorImpl m_seg_iter;

    // Envelope_2D_intersector
    private Envelope2DIntersectorImpl m_intersector;

    private int m_function;

    private interface State {
        static final int nextPath = 0;
        static final int nextSegment = 1;
        static final int iterate = 2;
    }

    PairwiseIntersectorImpl(MultiPathImpl multi_path_impl_a, MultiPathImpl multi_path_impl_b, double tolerance, boolean b_paths) {
        m_multi_path_impl_a = multi_path_impl_a;
        m_multi_path_impl_b = multi_path_impl_b;

        m_b_paths = b_paths;
        m_path_index = -1;

        m_b_quad_tree = false;

        GeometryAccelerators geometry_accelerators_a = multi_path_impl_a._getAccelerators();

        if (geometry_accelerators_a != null) {
            QuadTreeImpl qtree_a = (!b_paths ? geometry_accelerators_a.getQuadTree() : geometry_accelerators_a.getQuadTreeForPaths());

            if (qtree_a != null) {
                m_b_done = false;
                m_tolerance = tolerance;
                m_quad_tree = qtree_a;
                m_qt_iter = m_quad_tree.getIterator();
                m_b_quad_tree = true;
                m_b_swap_elements = true;
                m_function = State.nextPath;

                if (!b_paths)
                    m_seg_iter = multi_path_impl_b.querySegmentIterator();
                else
                    m_path_index = multi_path_impl_b.getPathCount(); // we will iterate backwards until we hit -1
            }
        }

        if (!m_b_quad_tree) {
            GeometryAccelerators geometry_accelerators_b = multi_path_impl_b._getAccelerators();

            if (geometry_accelerators_b != null) {
                QuadTreeImpl qtree_b = (!b_paths ? geometry_accelerators_b.getQuadTree() : geometry_accelerators_b.getQuadTreeForPaths());

                if (qtree_b != null) {
                    m_b_done = false;
                    m_tolerance = tolerance;
                    m_quad_tree = qtree_b;
                    m_qt_iter = m_quad_tree.getIterator();
                    m_b_quad_tree = true;
                    m_b_swap_elements = false;
                    m_function = State.nextPath;

                    if (!b_paths)
                        m_seg_iter = multi_path_impl_a.querySegmentIterator();
                    else
                        m_path_index = multi_path_impl_a.getPathCount(); // we will iterate backwards until we hit -1
                }
            }
        }

        if (!m_b_quad_tree) {
            if (!b_paths) {
                m_intersector = InternalUtils.getEnvelope2DIntersector(multi_path_impl_a, multi_path_impl_b, tolerance);
            } else {
                boolean b_simple_a = multi_path_impl_a.getIsSimple(0.0) >= 1;
                boolean b_simple_b = multi_path_impl_b.getIsSimple(0.0) >= 1;
                m_intersector = InternalUtils.getEnvelope2DIntersectorForParts(multi_path_impl_a, multi_path_impl_b, tolerance, b_simple_a, b_simple_b);
            }
        }
    }

    boolean next() {
        if (m_b_quad_tree) {
            if (m_b_done)
                return false;

            boolean b_searching = true;
            while (b_searching) {
                switch (m_function) {
                    case State.nextPath:
                        b_searching = nextPath_();
                        break;
                    case State.nextSegment:
                        b_searching = nextSegment_();
                        break;
                    case State.iterate:
                        b_searching = iterate_();
                        break;
                    default:
                        throw GeometryException.GeometryInternalError();
                }
            }

            if (m_b_done)
                return false;

            return true;
        }

        if (m_intersector == null)
            return false;

        return m_intersector.next();
    }

    int getRedElement() {
        if (m_b_quad_tree) {
            if (!m_b_swap_elements)
                return (!m_b_paths ? m_seg_iter.getStartPointIndex() : m_path_index);

            return m_quad_tree.getElement(m_element_handle);
        }

        return m_intersector.getRedElement(m_intersector.getHandleA());
    }

    int getBlueElement() {
        if (m_b_quad_tree) {
            if (m_b_swap_elements)
                return (!m_b_paths ? m_seg_iter.getStartPointIndex() : m_path_index);

            return m_quad_tree.getElement(m_element_handle);
        }

        return m_intersector.getBlueElement(m_intersector.getHandleB());
    }

    Envelope2D getRedEnvelope() {
        if (!m_b_paths)
            throw GeometryException.GeometryInternalError();

        if (m_b_quad_tree) {
            if (!m_b_swap_elements)
                return m_paths_query;

            return m_quad_tree.getElementExtent(m_element_handle);
        }

        return m_intersector.getRedEnvelope(m_intersector.getHandleA());
    }

    Envelope2D getBlueEnvelope() {
        if (!m_b_paths)
            throw GeometryException.GeometryInternalError();

        if (m_b_quad_tree) {
            if (m_b_swap_elements)
                return m_paths_query;

            return m_quad_tree.getElementExtent(m_element_handle);
        }

        return m_intersector.getBlueEnvelope(m_intersector.getHandleB());
    }

    boolean nextPath_() {
        if (!m_b_paths) {
            if (!m_seg_iter.nextPath()) {
                m_b_done = true;
                return false;
            }

            m_function = State.nextSegment;
            return true;
        }

        if (--m_path_index == -1) {
            m_b_done = true;
            return false;
        }

        if (m_b_swap_elements)
            m_multi_path_impl_b.queryPathEnvelope2D(m_path_index, m_paths_query);
        else
            m_multi_path_impl_a.queryPathEnvelope2D(m_path_index, m_paths_query);

        m_qt_iter.resetIterator(m_paths_query, m_tolerance);
        m_function = State.iterate;
        return true;
    }

    boolean nextSegment_() {
        if (!m_seg_iter.hasNextSegment()) {
            m_function = State.nextPath;
            return true;
        }

        Segment segment = m_seg_iter.nextSegment();
        m_qt_iter.resetIterator(segment, m_tolerance);
        m_function = State.iterate;
        return true;
    }

    boolean iterate_() {
        m_element_handle = m_qt_iter.next();

        if (m_element_handle == -1) {
            m_function = (!m_b_paths ? State.nextSegment : State.nextPath);
            return true;
        }

        return false;
    }
}
