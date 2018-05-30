/*
 Copyright 1995-2018 Esri

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

//An internal helper class. Do not use.
public class OGCStructureInternal {
	private static class EditShapeCursor extends GeometryCursor {
		EditShape m_shape;
		int m_geom;
		int m_index;
		
		EditShapeCursor(EditShape shape, int index) {
			m_shape = shape;
			m_geom = -1;
			m_index = index;
		}
		@Override
		public Geometry next() {
			if (m_shape != null) {
				if (m_geom == -1)
					m_geom = m_shape.getFirstGeometry();
				else
					m_geom = m_shape.getNextGeometry(m_geom);
				
				if (m_geom == -1) {
					m_shape = null;
				}
				else {
					return m_shape.getGeometry(m_geom);
				}
					
			}
			
			return null;
		}

		@Override
		public int getGeometryID() {
			return m_shape.getGeometryUserIndex(m_geom, m_index);
		}
		
	};
	
	public static GeometryCursor prepare_for_ops_(GeometryCursor geoms, SpatialReference sr) {
		assert(geoms != null);
		EditShape editShape = new EditShape();
		int geomIndex = editShape.createGeometryUserIndex();
		for (Geometry g = geoms.next(); g != null; g = geoms.next()) {
			int egeom = editShape.addGeometry(g);
			editShape.setGeometryUserIndex(egeom, geomIndex, geoms.getGeometryID());
		}

		Envelope2D env = editShape.getEnvelope2D();
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				env, true);

		CrackAndCluster.execute(editShape, tolerance, null, true);
		return OperatorSimplifyOGC.local().execute(new EditShapeCursor(editShape, geomIndex), sr, false, null);
	}
}

