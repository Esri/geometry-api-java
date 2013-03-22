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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

final class GeometrySerializer implements Serializable {
	private static final long serialVersionUID = 1L;

	static class BaseGeometryData implements Serializable {
		Geometry.Type geometryType;
		byte[] esriShape = null;
	}

	static class MultiVertexData extends BaseGeometryData {
		int simpleFlag = 0;
		double tolerance = 0;
	}

	static class MultiPathData extends MultiVertexData {
		boolean[] ogcFlags = null;
	}

	BaseGeometryData geometryData;

	Object readResolve() throws ObjectStreamException {
		Geometry geometry = null;
		try {
			geometry = GeometryEngine.geometryFromEsriShape(
					geometryData.esriShape, geometryData.geometryType);
			if (Geometry.isMultiVertex(geometry.getType().value())) {
				MultiVertexData mvd = (MultiVertexData) geometryData;
				MultiVertexGeometryImpl mvImpl = (MultiVertexGeometryImpl) geometry
						._getImpl();
				if (!geometry.isEmpty()
						&& Geometry.isMultiPath(geometry.getType().value())) {
					MultiPathData mpd = (MultiPathData) geometryData;
					MultiPathImpl mpImpl = (MultiPathImpl) geometry._getImpl();
					AttributeStreamOfInt8 pathFlags = mpImpl
							.getPathFlagsStreamRef();
					for (int i = 0, n = mpImpl.getPathCount(); i < n; i++) {
						if (mpd.ogcFlags[i])
							pathFlags.setBits(i,
									(byte) PathFlags.enumOGCStartPolygon);
					}
				}
				mvImpl.setIsSimple(mvd.simpleFlag, mvd.tolerance, false);
			}

		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot read geometry from stream");
		}
		return geometry;
	}

	public void setGeometryByValue(Geometry geometry)
			throws ObjectStreamException {
		try {
			if (Geometry.isMultiPath(geometry.getType().value())) {
				geometryData = new MultiPathData();
			} else if (Geometry.isMultiVertex(geometry.getType().value())) {
				geometryData = new MultiVertexData();
			} else {
				geometryData = new BaseGeometryData();
			}
			geometryData.esriShape = GeometryEngine
					.geometryToEsriShape(geometry);
			geometryData.geometryType = geometry.getType();
			if (Geometry.isMultiVertex(geometryData.geometryType.value())) {
				MultiVertexData mvd = (MultiVertexData) geometryData;
				MultiVertexGeometryImpl mvImpl = (MultiVertexGeometryImpl) geometry
						._getImpl();
				mvd.tolerance = mvImpl.m_simpleTolerance;
				mvd.simpleFlag = mvImpl.getIsSimple(0);
				if (!geometry.isEmpty()
						&& Geometry.isMultiPath(geometryData.geometryType
								.value())) {
					MultiPathData mpd = (MultiPathData) geometryData;
					MultiPathImpl mpImpl = (MultiPathImpl) geometry._getImpl();
					mpd.ogcFlags = new boolean[mpImpl.getPathCount()];
					AttributeStreamOfInt8 pathFlags = mpImpl
							.getPathFlagsStreamRef();
					for (int i = 0, n = mpImpl.getPathCount(); i < n; i++) {
						mpd.ogcFlags[i] = (pathFlags.read(i) & (byte) PathFlags.enumOGCStartPolygon) != 0;
					}
				}

			}
		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot serialize this geometry");
		}
	}
}
