/*
 Copyright 1995-2017 Esri

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

package com.esri.core.geometry.ogc;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.GeometryException;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.MultiVertexGeometry;
import com.esri.core.geometry.NumberUtils;
import com.esri.core.geometry.OperatorConvexHull;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SimpleGeometryCursor;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.VertexDescription;
import com.esri.core.geometry.GeoJsonExportFlags;
import com.esri.core.geometry.OperatorExportToGeoJson;
import com.esri.core.geometry.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_CONCRETE_GEOMETRY_COLLECTION;

public class OGCConcreteGeometryCollection extends OGCGeometryCollection {
	public OGCConcreteGeometryCollection(List<OGCGeometry> geoms,
			SpatialReference sr) {
		geometries = geoms;
		esriSR = sr;
	}

	public OGCConcreteGeometryCollection(OGCGeometry geom, SpatialReference sr) {
		geometries = new ArrayList<OGCGeometry>(1);
		geometries.add(geom);
		esriSR = sr;
	}

	@Override
	public int dimension() {
		int maxD = 0;
		for (int i = 0, n = numGeometries(); i < n; i++)
			maxD = Math.max(geometryN(i).dimension(), maxD);

		return maxD;
	}

	@Override
	public int coordinateDimension() {
		return isEmpty() ? 2 : geometryN(0).coordinateDimension();
	}

	@Override
	public boolean is3D() {
		return !isEmpty() && geometries.get(0).is3D();
	}

	@Override
	public boolean isMeasured() {
		return !isEmpty() && geometries.get(0).isMeasured();
	}

	@Override
	public OGCGeometry envelope() {
		GeometryCursor gc = getEsriGeometryCursor();
		Envelope env = new Envelope();
		for (Geometry g = gc.next(); g != null; g = gc.next()) {
			Envelope e = new Envelope();
			g.queryEnvelope(e);
			env.merge(e);
		}

		Polygon polygon = new Polygon();
		polygon.addEnvelope(env, false);
		return new OGCPolygon(polygon, esriSR);
	}

	@Override
	public int numGeometries() {
		return geometries.size();
	}

	@Override
	public OGCGeometry geometryN(int n) {
		return geometries.get(n);
	}

	@Override
	public String geometryType() {
		return "GeometryCollection";
	}

	@Override
	public long estimateMemorySize()
	{
		long size = SIZE_OF_OGC_CONCRETE_GEOMETRY_COLLECTION;
		if (geometries != null) {
			for (OGCGeometry geometry : geometries) {
				size += geometry.estimateMemorySize();
			}
		}
		return size;
	}

	@Override
	public String asText() {
		StringBuilder sb = new StringBuilder("GEOMETRYCOLLECTION ");
		if (is3D()) {
			sb.append('Z');
		}
		if (isMeasured()) {
			sb.append('M');
		}
		if (is3D() || isMeasured())
			sb.append(' ');

		int n = numGeometries();

		if (n == 0) {
			sb.append("EMPTY");
			return sb.toString();
		}

		sb.append('(');
		for (int i = 0; i < n; i++) {
			if (i > 0)
				sb.append(", ");

			sb.append(geometryN(i).asText());
		}
		sb.append(')');

		return sb.toString();
	}

	@Override
	public ByteBuffer asBinary() {

		ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>(0);

		int size = 9;
		int n = numGeometries();
		for (int i = 0; i < n; i++) {
			ByteBuffer buffer = geometryN(i).asBinary();
			buffers.add(buffer);
			size += buffer.capacity();
		}

		ByteBuffer wkbBuffer = ByteBuffer.allocate(size).order(
				ByteOrder.nativeOrder());

		byte byteOrder = (byte) (wkbBuffer.order() == ByteOrder.LITTLE_ENDIAN ? 1
				: 0);
		int wkbType = 7;

		if (is3D())
			wkbType += 1000;
		if (isMeasured())
			wkbType += 2000;

		wkbBuffer.put(0, byteOrder);
		wkbBuffer.putInt(1, wkbType);
		wkbBuffer.putInt(5, n);

		int offset = 9;
		for (int i = 0; i < n; i++) {
			byte[] arr = buffers.get(i).array();
			System.arraycopy(arr, 0, wkbBuffer.array(), offset, arr.length);
			offset += arr.length;
		}

		return wkbBuffer;
	}

	@Override
	public String asGeoJson() {
		return asGeoJsonImpl(GeoJsonExportFlags.geoJsonExportDefaults);
	}

	@Override
	String asGeoJsonImpl(int export_flags) {
		StringBuilder sb = new StringBuilder();

		sb.append("{\"type\":\"GeometryCollection\",\"geometries\":");

		sb.append("[");
		for (int i = 0, n = numGeometries(); i < n; i++) {
			if (i > 0)
				sb.append(",");

			if (geometryN(i) != null)
				sb.append(geometryN(i).asGeoJsonImpl(GeoJsonExportFlags.geoJsonExportSkipCRS));
		}

		sb.append("],\"crs\":");

		if (esriSR != null) {
			String crs_value = OperatorExportToGeoJson.local().exportSpatialReference(0, esriSR);
			sb.append(crs_value);
		} else {
			sb.append("\"null\"");
		}

		sb.append("}");

		return sb.toString();
	}

	@Override
	public boolean isEmpty() {
		return numGeometries() == 0;
	}

	@Override
	public double MinZ() {
		double z = Double.NaN;
		for (int i = 0, n = numGeometries(); i < n; i++)
			z = i == 0 ? geometryN(i).MinZ() : Math.min(geometryN(i).MinZ(), z);
		return z;
	}

	@Override
	public double MaxZ() {
		double z = Double.NaN;
		for (int i = 0, n = numGeometries(); i < n; i++)
			z = i == 0 ? geometryN(i).MaxZ() : Math.min(geometryN(i).MaxZ(), z);
		return z;
	}

	@Override
	public double MinMeasure() {
		double z = Double.NaN;
		for (int i = 0, n = numGeometries(); i < n; i++)
			z = i == 0 ? geometryN(i).MinMeasure() : Math.min(geometryN(i)
					.MinMeasure(), z);
		return z;
	}

	@Override
	public double MaxMeasure() {
		double z = Double.NaN;
		for (int i = 0, n = numGeometries(); i < n; i++)
			z = i == 0 ? geometryN(i).MaxMeasure() : Math.min(geometryN(i)
					.MaxMeasure(), z);
		return z;
	}

	@Override
	public boolean isSimple() {
		for (int i = 0, n = numGeometries(); i < n; i++)
			if (!geometryN(i).isSimple())
				return false;
		return true;
	}

	/**
	 * makeSimpleRelaxed is not supported for the GeometryCollection instance.
	 * 
	 */
	@Override
	public OGCGeometry makeSimple() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSimpleRelaxed() {
		for (int i = 0, n = numGeometries(); i < n; i++)
			if (!geometryN(i).isSimpleRelaxed())
				return false;
		return true;
	}

	/**
	 * makeSimpleRelaxed is not supported for the GeometryCollection instance.
	 * 
	 */
	@Override
	public OGCGeometry makeSimpleRelaxed(boolean forceProcessing) {
		throw new UnsupportedOperationException();
	}

	@Override
	public OGCGeometry boundary() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OGCGeometry locateAlong(double mValue) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public OGCGeometry locateBetween(double mStart, double mEnd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Geometry getEsriGeometry() {
		return null;
	}

	@Override
	public GeometryCursor getEsriGeometryCursor() {
		return new GeometryCursorOGC(geometries);
	}

	@Override
	protected boolean isConcreteGeometryCollection() {
		return true;
	}

	static class GeometryCursorOGC extends GeometryCursor {
		private int m_index;
		private int m_ind;
		private List<OGCGeometry> m_geoms;
		GeometryCursor m_curs;

		GeometryCursorOGC(List<OGCGeometry> geoms) {
			m_geoms = geoms;
			m_index = -1;
			m_curs = null;
			m_ind = 0;
		}

		@Override
		public Geometry next() {
			while (true) {
				if (m_curs != null) {
					Geometry g = m_curs.next();
					if (g != null) {
						m_index++;
						return g;
					}
					m_curs = null;
				}
				if (m_ind >= m_geoms.size())
					return null;

				int i = m_ind;
				m_ind++;
				if (m_geoms.get(i) == null)
					continue;// filter out nulls
				if (!m_geoms.get(i).isConcreteGeometryCollection()) {
					m_index++;
					return m_geoms.get(i).getEsriGeometry();
				} else {
					OGCConcreteGeometryCollection gc = (OGCConcreteGeometryCollection) m_geoms
							.get(i);
					m_curs = new GeometryCursorOGC(gc.geometries);
					return next();
				}
			}
		}

		@Override
		public int getGeometryID() {
			return m_index;
		}

	}
	
	@Override
	public OGCGeometry convexHull() {
		GeometryCursor cursor = OperatorConvexHull.local().execute(
				getEsriGeometryCursor(), false, null);
		MultiPoint mp = new MultiPoint();
		Polygon polygon = new Polygon();
		VertexDescription vd = null;
		for (Geometry geom = cursor.next(); geom != null; geom = cursor.next()) {
			vd = geom.getDescription();
			if (geom.isEmpty())
				continue;

			if (geom.getType() == Geometry.Type.Polygon) {
				polygon.add((MultiPath) geom, false);
			}
			else if (geom.getType() == Geometry.Type.Polyline) {
				mp.add((MultiVertexGeometry) geom, 0, -1);
			}
			else if (geom.getType() == Geometry.Type.Point) {
				mp.add((Point) geom);
			}
			else {
				throw new GeometryException("internal error");
			}
		}

		Geometry resultGeom = null;
		if (!mp.isEmpty()) {
			resultGeom = OperatorConvexHull.local().execute(mp, null);
		}

		if (!polygon.isEmpty()) {
			if (!resultGeom.isEmpty()) {
				Geometry[] geoms = { resultGeom, polygon };
				resultGeom = OperatorConvexHull.local().execute(
						new SimpleGeometryCursor(geoms), true, null).next();
			}
			else {
				resultGeom = polygon;
			}
		}

		if (resultGeom == null) {
			Point pt = new Point();
			if (vd != null)
				pt.assignVertexDescription(vd);

			return new OGCPoint(pt, getEsriSpatialReference());
		}

		return OGCGeometry.createFromEsriGeometry(resultGeom, getEsriSpatialReference(), false);
	}

	List<OGCGeometry> geometries;

	@Override
	public void setSpatialReference(SpatialReference esriSR_) {
		esriSR = esriSR_;
		for (int i = 0, n = geometries.size(); i < n; i++) {
			if (geometries.get(i) != null)
				geometries.get(i).setSpatialReference(esriSR_);
		}
	}

	@Override
	public OGCGeometry convertToMulti() {
		return this;
	}

	@Override
	public String asJson() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object other)	{
		if (other == null)
			return false;

		if (other == this)
			return true;

		if (other.getClass() != getClass())
			return false;
		
		OGCConcreteGeometryCollection another = (OGCConcreteGeometryCollection)other;
		if (geometries != null) {		
			if (!geometries.equals(another.geometries))
				return false;
		}
		else if (another.geometries != null)
			return false;
		
		if (esriSR == another.esriSR) {
			return true;
		}
			
		if (esriSR != null && another.esriSR != null) {
			return esriSR.equals(another.esriSR);
		}
			
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		if (geometries != null)
			hash = geometries.hashCode();
		
		if (esriSR != null)
			hash = NumberUtils.hashCombine(hash, esriSR.hashCode());
		
		return hash;
	}
}
