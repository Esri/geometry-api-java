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

class GeometryAccelerators {

	// /**
	// *Describes the degree of acceleration of the geometry.
	// */
	// enum GeometryAccelerationDegree
	// {
	// enumMild, //<!mild acceleration, takes least amount of memory.
	// enumMedium, //<!medium acceleration, takes more memory and takes more
	// time to accelerate, but may work faster.
	// enumHot //<!high acceleration, takes even more memory and may take
	// longest time to accelerate, but may work faster than the other two.
	// }

	private RasterizedGeometry2D m_rasterizedGeometry;
	private QuadTreeImpl m_quad_tree;

	public RasterizedGeometry2D getRasterizedGeometry() {
		return m_rasterizedGeometry;
	}

	public QuadTreeImpl getQuadTree() {
		return m_quad_tree;
	}

	void _setRasterizedGeometry(RasterizedGeometry2D rg) {
		m_rasterizedGeometry = rg;
	}

	void _setQuadTree(QuadTreeImpl quad_tree) {
		m_quad_tree = quad_tree;
	}
}
