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

package com.esri.core.geometry.ogc;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonExportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryCursor;
import com.esri.core.geometry.GeometryException;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.MultiVertexGeometry;
import com.esri.core.geometry.NumberUtils;
import com.esri.core.geometry.OGCStructureInternal;
import com.esri.core.geometry.OperatorConvexHull;
import com.esri.core.geometry.OperatorDifference;
import com.esri.core.geometry.OperatorExportToGeoJson;
import com.esri.core.geometry.OperatorIntersection;
import com.esri.core.geometry.OperatorUnion;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SimpleGeometryCursor;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.VertexDescription;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.esri.core.geometry.SizeOf.SIZE_OF_OGC_CONCRETE_GEOMETRY_COLLECTION;

public class OGCConcreteGeometryCollection extends OGCGeometryCollection {
	static public String TYPE = "GeometryCollection";
	
	List<OGCGeometry> geometries;
	
	public OGCConcreteGeometryCollection(List<OGCGeometry> geoms,
			SpatialReference sr) {
		geometries = geoms;
		esriSR = sr;
	}
	
	public OGCConcreteGeometryCollection(GeometryCursor geoms,
			SpatialReference sr) {
		List<OGCGeometry> ogcGeoms = new ArrayList<OGCGeometry>(10);
		for (Geometry g = geoms.next(); g != null; g = geoms.next()) {
			ogcGeoms.add(createFromEsriGeometry(g, sr));
		}
		
		geometries = ogcGeoms;
		esriSR = sr;
	}

	public OGCConcreteGeometryCollection(OGCGeometry geom, SpatialReference sr) {
		geometries = new ArrayList<OGCGeometry>(1);
		geometries.add(geom);
		esriSR = sr;
	}

	public OGCConcreteGeometryCollection(SpatialReference sr) {
		geometries = new ArrayList<OGCGeometry>();
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
		return TYPE;
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

	private static class GeometryCursorOGC extends GeometryCursor {
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
			if (resultGeom != null && !resultGeom.isEmpty()) {
				Geometry[] geoms = { resultGeom, polygon };
				resultGeom = OperatorConvexHull.local().execute(
						new SimpleGeometryCursor(geoms), true, null).next();
			}
			else {
				resultGeom = OperatorConvexHull.local().execute(polygon, null);
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
	public OGCGeometry reduceFromMulti() {
		int n = numGeometries();
		if (n == 0) {
			return this;
		}
		
		if (n == 1) {
			return geometryN(0).reduceFromMulti();
		}
		
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
	
	@Override
	public double distance(OGCGeometry another) {
		if (this == another)
			return isEmpty() ? Double.NaN : 0;

		double minD = Double.NaN;
		for (int i = 0, n = numGeometries(); i < n; ++i) {
			// TODO Skip expensive distance computation if bounding boxes are further away than minD
			double d = geometryN(i).distance(another);
			if (d < minD || Double.isNaN(minD)) {
				minD = d;
				// TODO Replace zero with tolerance defined by the spatial reference
				if (minD == 0) {
					break;
				}
			}
		}

		return minD;
	}
	
	//
	//Relational operations
	@Override
	public boolean overlaps(OGCGeometry another) {
		//TODO
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean touches(OGCGeometry another) {
		//TODO
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean crosses(OGCGeometry another) {
		//TODO
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean relate(OGCGeometry another, String matrix) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean disjoint(OGCGeometry another) {
		if (isEmpty() || another.isEmpty())
			return true;

		if (this == another)
			return false;

		//TODO: a simple envelope test
		
		OGCConcreteGeometryCollection flattened1 = flatten();
		if (flattened1.isEmpty())
			return true;
		OGCConcreteGeometryCollection otherCol = new OGCConcreteGeometryCollection(another, esriSR);
		OGCConcreteGeometryCollection flattened2 = otherCol.flatten();
		if (flattened2.isEmpty())
			return true;
		
		for (int i = 0, n1 = flattened1.numGeometries(); i < n1; ++i) {
			OGCGeometry g1 = flattened1.geometryN(i);
			for (int j = 0, n2 = flattened2.numGeometries(); j < n2; ++j) {
				OGCGeometry g2 = flattened2.geometryN(j);
				if (!g1.disjoint(g2))
					return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean contains(OGCGeometry another) {
		if (isEmpty() || another.isEmpty())
			return false;
		
		if (this == another)
			return true;

		return another.difference(this).isEmpty();
	}
	
	@Override
	public boolean Equals(OGCGeometry another) {
		if (this == another)
			return !isEmpty();
		
		if (another == null)
			return false;
		

		OGCGeometry g1 = reduceFromMulti();
		String t1 = g1.geometryType();
		OGCGeometry g2 = reduceFromMulti();
		if (t1 != g2.geometryType()) {
			return false;
		}
		
		if (t1 != OGCConcreteGeometryCollection.TYPE) {
			return g1.Equals(g2);
		}
		
		OGCConcreteGeometryCollection gc1 = (OGCConcreteGeometryCollection)g1;
		OGCConcreteGeometryCollection gc2 = (OGCConcreteGeometryCollection)g2;
		// TODO Assuming input geometries are simple and valid, remove-overlaps would be a no-op.
		// Hence, calling flatten() should be sufficient.
		gc1 = gc1.flattenAndRemoveOverlaps();
		gc2 = gc2.flattenAndRemoveOverlaps();
		int n = gc1.numGeometries();
		if (n != gc2.numGeometries()) {
			return false;
		}
		
		for (int i = 0; i < n; ++i) {
			if (!gc1.geometryN(i).Equals(gc2.geometryN(i))) {
				return false;
			}
		}
		
		return n > 0;
	}

	private static OGCConcreteGeometryCollection toGeometryCollection(OGCGeometry geometry)
	{
		if (geometry.geometryType() != OGCConcreteGeometryCollection.TYPE) {
			return new OGCConcreteGeometryCollection(geometry, geometry.getEsriSpatialReference());
		}

		return (OGCConcreteGeometryCollection) geometry;
	}

	private static List<Geometry> toList(GeometryCursor cursor)
	{
		List<Geometry> geometries = new ArrayList<Geometry>();
		for (Geometry geometry = cursor.next(); geometry != null; geometry = cursor.next()) {
			geometries.add(geometry);
		}
		return geometries;
	}

	//Topological
	@Override
	public OGCGeometry difference(OGCGeometry another) {
		if (isEmpty() || another.isEmpty()) {
			return this;
		}

		List<Geometry> geometries = toList(prepare_for_ops_(toGeometryCollection(this)));
		List<Geometry> otherGeometries = toList(prepare_for_ops_(toGeometryCollection(another)));

		List<OGCGeometry> result = new ArrayList<OGCGeometry>();
		for (Geometry geometry : geometries) {
			for (Geometry otherGeometry : otherGeometries) {
				if (geometry.getDimension() > otherGeometry.getDimension()) {
					continue; //subtracting lower dimension has no effect.
				}

				geometry = OperatorDifference.local().execute(geometry, otherGeometry, esriSR, null);
				if (geometry.isEmpty()) {
					break;
				}
			}

			if (!geometry.isEmpty()) {
				result.add(OGCGeometry.createFromEsriGeometry(geometry, esriSR));
			}
		}

		if (result.size() == 1) {
			return result.get(0).reduceFromMulti();
		}
		
		return new OGCConcreteGeometryCollection(result, esriSR).flattenAndRemoveOverlaps();
	}
	
	@Override
	public OGCGeometry intersection(OGCGeometry another) {
		if (isEmpty() || another.isEmpty()) {
			return new OGCConcreteGeometryCollection(esriSR);
		}

		List<Geometry> geometries = toList(prepare_for_ops_(toGeometryCollection(this)));
		List<Geometry> otherGeometries = toList(prepare_for_ops_(toGeometryCollection(another)));

		List<OGCGeometry> result = new ArrayList<OGCGeometry>();
		for (Geometry geometry : geometries) {
			for (Geometry otherGeometry : otherGeometries) {
				GeometryCursor intersectionCursor = OperatorIntersection.local().execute(new SimpleGeometryCursor(geometry), new SimpleGeometryCursor(otherGeometry), esriSR, null, 7);
				OGCGeometry intersection = OGCGeometry.createFromEsriCursor(intersectionCursor, esriSR, true);
				if (!intersection.isEmpty()) {
					result.add(intersection);
				}
			}
		}
		
		if (result.size() == 1) {
			return result.get(0).reduceFromMulti();
		}

		return new OGCConcreteGeometryCollection(result, esriSR).flattenAndRemoveOverlaps();
	}
	
	@Override
	public OGCGeometry symDifference(OGCGeometry another) {
		//TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * Checks if collection is flattened.
	 * @return True for the flattened collection. A flattened collection contains up to three non-empty geometries:
	 * an OGCMultiPoint, an OGCMultiPolygon, and an OGCMultiLineString.
	 */
	public boolean isFlattened() {
		int n = numGeometries();
		if (n > 3)
			return false;

		int dimension = -1;
		for (int i = 0; i < n; ++i) {
			OGCGeometry g = geometryN(i);
			if (g.isEmpty())
				return false;//no empty allowed
			
			String t = g.geometryType();
			if (t != OGCMultiPoint.TYPE && t != OGCMultiPolygon.TYPE && t != OGCMultiLineString.TYPE)
				return false;
			
			//check strict order of geometry dimensions
			int d = g.dimension();
			if (d <= dimension)
				return false;

			dimension = d;
		}
		
		return true;
	}

	/**
	 * Flattens Geometry Collection.
	 * The result collection contains up to three geometries:
	 * an OGCMultiPoint, an OGCMultilineString, and an OGCMultiPolygon (in that order).
	 * @return A flattened Geometry Collection, or self if already flattened.
	 */
	public OGCConcreteGeometryCollection flatten() {
		if (isFlattened()) {
			return this;
		}
		
		OGCMultiPoint multiPoint = null;
		ArrayList<Geometry> polygons = null;
		OGCMultiLineString polyline = null;
		GeometryCursor gc = getEsriGeometryCursor();
		for (Geometry g = gc.next(); g != null; g = gc.next()) {
			if (g.isEmpty())
				continue;
			
			Geometry.Type t = g.getType();

			if (t == Geometry.Type.Point) {
				if (multiPoint == null) {
					multiPoint = new OGCMultiPoint(esriSR);
				}

				((MultiPoint)multiPoint.getEsriGeometry()).add((Point)g);
				continue;
			}

			if (t == Geometry.Type.MultiPoint) {
				if (multiPoint == null)
					multiPoint = new OGCMultiPoint(esriSR);
				
				((MultiPoint)multiPoint.getEsriGeometry()).add((MultiPoint)g, 0, -1);
				continue;
			}
			
			if (t == Geometry.Type.Polyline) {
				if (polyline == null)
					polyline = new OGCMultiLineString(esriSR);
				
				((MultiPath)polyline.getEsriGeometry()).add((Polyline)g, false);
				continue;
			}
			
			if (t == Geometry.Type.Polygon) {
				if (polygons == null)
					polygons = new ArrayList<Geometry>();
				
				polygons.add(g);
				continue;
			}
			
			throw new GeometryException("internal error");//what else?
		}

		List<OGCGeometry> list = new ArrayList<OGCGeometry>();

		if (multiPoint != null)
			list.add(multiPoint);

		if (polyline != null)
			list.add(polyline);
		
		if (polygons != null) {
			GeometryCursor unionedPolygons = OperatorUnion.local().execute(new SimpleGeometryCursor(polygons), esriSR, null);
			Geometry g = unionedPolygons.next();
			if (!g.isEmpty()) {
				list.add(new OGCMultiPolygon((Polygon)g, esriSR));
			}

		}

		return new OGCConcreteGeometryCollection(list, esriSR);
	}
	
	/**
	 * Fixes topological overlaps in the GeometryCollecion.
	 * This is equivalent to union of the geometry collection elements.
	 *
	 * TODO "flattened" collection is supposed to contain only mutli-geometries, but this method may return single geometries
	 * e.g. for GEOMETRYCOLLECTION (LINESTRING (...)) it returns GEOMETRYCOLLECTION (LINESTRING (...))
	 * and not GEOMETRYCOLLECTION (MULTILINESTRING (...))
	 * @return A geometry collection that is flattened and has no overlapping elements.
	 */
	public OGCConcreteGeometryCollection flattenAndRemoveOverlaps() {

		//flatten and crack/cluster
		GeometryCursor cursor = OGCStructureInternal.prepare_for_ops_(flatten().getEsriGeometryCursor(), esriSR);

		//make sure geometries don't overlap
		return new OGCConcreteGeometryCollection(removeOverlapsHelper_(toList(cursor)), esriSR);
	}

	private GeometryCursor removeOverlapsHelper_(List<Geometry> geoms) {
		List<Geometry> result = new ArrayList<Geometry>();
		for (int i = 0; i < geoms.size(); ++i) {
			Geometry current = geoms.get(i);
			if (current.isEmpty())
				continue;
			
			for (int j = i + 1; j < geoms.size(); ++j) {
				Geometry subG = geoms.get(j);
				current = OperatorDifference.local().execute(current, subG, esriSR, null);
				if (current.isEmpty())
					break;
			}
			
			if (current.isEmpty())
				continue;
			
			result.add(current);
		}
		
		return new SimpleGeometryCursor(result);
	}
	
	private static class FlatteningCollectionCursor extends GeometryCursor {
		private List<OGCConcreteGeometryCollection> m_collections;
		private GeometryCursor m_current;
		private int m_index;
		FlatteningCollectionCursor(List<OGCConcreteGeometryCollection> collections) {
			m_collections = collections;
			m_index = -1;
			m_current = null;
		}
		
		@Override
		public Geometry next() {
			while (m_collections != null) {
				if (m_current != null) {
					Geometry g = m_current.next();
					if (g == null) {
						m_current = null;
						continue;
					}
					
					return g;
				}
				else {
					m_index++;
					if (m_index < m_collections.size()) {
						m_current = m_collections.get(m_index).flatten().getEsriGeometryCursor();
						continue;
					}
					else {
						m_collections = null;
						m_index = -1;
					}
				}
			}
			
			return null;
		}

		@Override
		public int getGeometryID() {
			return m_index;
		}
		
	};
	
	//Collectively processes group of geometry collections (intersects all segments and clusters points).
	//Flattens collections, removes overlaps.
	//Once done, the result collections would work well for topological and relational operations.
	private GeometryCursor prepare_for_ops_(OGCConcreteGeometryCollection collection) {
		assert(collection != null && !collection.isEmpty());
		GeometryCursor prepared = OGCStructureInternal.prepare_for_ops_(collection.flatten().getEsriGeometryCursor(), esriSR);
		return removeOverlapsHelper_(toList(prepared));
	}
}
