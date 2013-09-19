package com.esri.core.geometry.ogc;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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
	 * isSimpleRelaxed is not supported for the GeometryCollection instance.
	 * 
	 */
	@Override
	public boolean isSimpleRelaxed() {
		throw new UnsupportedOperationException();
	}

	/**
	 * MakeSimpleRelaxed is not supported for the GeometryCollection instance.
	 * 
	 */
	@Override
	public OGCGeometry MakeSimpleRelaxed(boolean forceProcessing) {
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
	public OGCGeometry convertToMulti()
	{
		return this;
	}
	
}
