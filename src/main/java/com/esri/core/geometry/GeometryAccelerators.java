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

class GeometryAccelerators {

	private RasterizedGeometry2D m_rasterizedGeometry;
	private QuadTreeImpl m_quad_tree;
    private QuadTreeImpl m_quad_tree_for_paths;

	public RasterizedGeometry2D getRasterizedGeometry() {
		return m_rasterizedGeometry;
	}

	public QuadTreeImpl getQuadTree() {
		return m_quad_tree;
	}

	public QuadTreeImpl getQuadTreeForPaths() {
		return m_quad_tree_for_paths;
	}

	void _setRasterizedGeometry(RasterizedGeometry2D rg) {
		m_rasterizedGeometry = rg;
	}

	void _setQuadTree(QuadTreeImpl quad_tree) {
		m_quad_tree = quad_tree;
	}

	void _setQuadTreeForPaths(QuadTreeImpl quad_tree) { m_quad_tree_for_paths = quad_tree; }

	static boolean canUseRasterizedGeometry(Geometry geom) {
		if (geom.isEmpty()
				|| !(geom.getType() == Geometry.Type.Polyline || geom.getType() == Geometry.Type.Polygon)) {
			return false;
		}

		return true;
	}

	static boolean canUseQuadTree(Geometry geom) {
		if (geom.isEmpty()
				|| !(geom.getType() == Geometry.Type.Polyline || geom.getType() == Geometry.Type.Polygon)) {
			return false;
		}

		if (((MultiVertexGeometry) geom).getPointCount() < 20) {
			return false;
		}

		return true;
	}

    static boolean canUseQuadTreeForPaths(Geometry geom) {
        if (geom.isEmpty() || !(geom.getType() == Geometry.Type.Polyline || geom.getType() == Geometry.Type.Polygon))
            return false;

        if (((MultiVertexGeometry) geom).getPointCount() < 20)
            return false;

        return true;
    }
}
