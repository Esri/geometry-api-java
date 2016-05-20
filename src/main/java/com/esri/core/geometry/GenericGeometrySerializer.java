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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

//This is a writeReplace class for MultiPoint, Polyline, and Polygon
public class GenericGeometrySerializer implements Serializable {
	private static final long serialVersionUID = 1L;
	int geometryType;
	byte[] esriShape = null;
	int simpleFlag = 0;
	double tolerance = 0;
	boolean[] ogcFlags = null;
	
	public Object readResolve() throws ObjectStreamException {
		Geometry geometry = null;
		try {
			geometry = GeometryEngine.geometryFromEsriShape(
					esriShape, Geometry.Type.intToType(geometryType));
			
			if (Geometry.isMultiVertex(geometryType)) {
				MultiVertexGeometryImpl mvImpl = (MultiVertexGeometryImpl) geometry
						._getImpl();
				if (!geometry.isEmpty()
						&& Geometry.isMultiPath(geometryType)) {
					MultiPathImpl mpImpl = (MultiPathImpl) geometry._getImpl();
					AttributeStreamOfInt8 pathFlags = mpImpl
							.getPathFlagsStreamRef();
					for (int i = 0, n = mpImpl.getPathCount(); i < n; i++) {
						if (ogcFlags[i])
							pathFlags.setBits(i,
									(byte) PathFlags.enumOGCStartPolygon);
					}
				}
				mvImpl.setIsSimple(simpleFlag, tolerance, false);
			}
		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot read geometry from stream");
		}
		return geometry;
	}

	public void setGeometryByValue(Geometry geometry)
			throws ObjectStreamException {
		try {
			esriShape = GeometryEngine
					.geometryToEsriShape(geometry);
			geometryType = geometry.getType().value();
			if (Geometry.isMultiVertex(geometryType)) {
				MultiVertexGeometryImpl mvImpl = (MultiVertexGeometryImpl) geometry
						._getImpl();
				tolerance = mvImpl.m_simpleTolerance;
				simpleFlag = mvImpl.getIsSimple(0);
				if (!geometry.isEmpty()
						&& Geometry.isMultiPath(geometryType)) {
					MultiPathImpl mpImpl = (MultiPathImpl) geometry._getImpl();
					ogcFlags = new boolean[mpImpl.getPathCount()];
					AttributeStreamOfInt8 pathFlags = mpImpl
							.getPathFlagsStreamRef();
					for (int i = 0, n = mpImpl.getPathCount(); i < n; i++) {
						ogcFlags[i] = (pathFlags.read(i) & (byte) PathFlags.enumOGCStartPolygon) != 0;
					}
				}

			}
		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot serialize this geometry");
		}
	}
}
