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

import com.esri.core.geometry.VertexDescription.Semantics;
import java.io.IOException;
import java.io.StringWriter;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

class OperatorExportToJsonCursor extends JsonCursor {
	GeometryCursor m_inputGeometryCursor;
	int m_index;
	int m_wkid = -1;
	int m_latest_wkid = -1;
	String m_wkt = null;

	private static JsonFactory factory = new JsonFactory();

	public OperatorExportToJsonCursor(SpatialReference spatialReference,
			GeometryCursor geometryCursor) {
		m_index = -1;
		if (geometryCursor == null)
			throw new IllegalArgumentException();
		if (spatialReference != null && !spatialReference.isLocal()) {
			m_wkid = spatialReference.getOldID();
			m_wkt = spatialReference.getText();
			m_latest_wkid = spatialReference.getLatestID();
		}
		m_inputGeometryCursor = geometryCursor;
	}

	@Override
	public int getID() {
		return m_index;
	}

	@Override
	public String next() {
		Geometry geometry;
		if ((geometry = m_inputGeometryCursor.next()) != null) {
			m_index = m_inputGeometryCursor.getGeometryID();
			return exportToJson(geometry);
		}
		return null;
	}

	private String exportToJson(Geometry geometry) {
		StringWriter sw = new StringWriter();
		try {
			JsonGenerator gen = factory.createJsonGenerator(sw);
			int type = geometry.getType().value();
			switch (type) {
			case Geometry.GeometryType.Point:
				exportPointToJson(gen, (Point) geometry);
				break;

			case Geometry.GeometryType.MultiPoint:
				exportMultiPointToJson(gen, (MultiPoint) geometry);
				break;

			case Geometry.GeometryType.Polyline:
				exportPolylineToJson(gen, (Polyline) geometry);
				break;

			case Geometry.GeometryType.Polygon:
				exportPolygonToJson(gen, (Polygon) geometry);
				break;

			case Geometry.GeometryType.Envelope:
				exportEnvelopeToJson(gen, (Envelope) geometry);
				break;

			default:
				throw new RuntimeException(
						"not implemented for this geometry type");
			}

			return sw.getBuffer().toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	private void exportPolygonToJson(JsonGenerator g, Polygon pp)
			throws JsonGenerationException, IOException {
		exportPolypathToJson(g, pp, "rings");
	}

	private void exportPolylineToJson(JsonGenerator g, Polyline pp)
			throws JsonGenerationException, IOException {
		exportPolypathToJson(g, pp, "paths");
	}

	private void exportPolypathToJson(JsonGenerator g, MultiPath pp, String name)
			throws JsonGenerationException, IOException {
		boolean bExportZs = pp.hasAttribute(Semantics.Z);
		boolean bExportMs = pp.hasAttribute(Semantics.M);

		g.writeStartObject();

		if (bExportZs) {
			g.writeFieldName("hasZ");
			g.writeBoolean(true);
		}

		if (bExportMs) {
			g.writeFieldName("hasM");
			g.writeBoolean(true);
		}

		g.writeFieldName(name);

		g.writeStartArray();

		if (!pp.isEmpty()) {
			int n = pp.getPathCount(); // rings or paths

			MultiPathImpl mpImpl = (MultiPathImpl) pp._getImpl();// get impl for
																	// faster
																	// access
			AttributeStreamOfDbl zs = null;
			AttributeStreamOfDbl ms = null;

			if (bExportZs)
				zs = (AttributeStreamOfDbl) mpImpl
						.getAttributeStreamRef(Semantics.Z);

			if (bExportMs)
				ms = (AttributeStreamOfDbl) mpImpl
						.getAttributeStreamRef(Semantics.M);

			boolean bPolygon = pp instanceof Polygon;
			Point2D pt = new Point2D();

			for (int i = 0; i < n; i++) {
				g.writeStartArray();
				int startindex = pp.getPathStart(i);
				int numVertices = pp.getPathSize(i);
				for (int j = startindex; j < startindex + numVertices; j++) {
					pp.getXY(j, pt);

					g.writeStartArray();

					writeDouble(pt.x, g);
					writeDouble(pt.y, g);

					if (bExportZs) {
						double z = zs.get(j);
						writeDouble(z, g);
					}

					if (bExportMs) {
						double m = ms.get(j);
						writeDouble(m, g);
					}

					g.writeEndArray();
				}

				// Close the Path/Ring by writing the Point at the start index
				if (bPolygon) {
					pp.getXY(startindex, pt);
					// getPoint(startindex);
					g.writeStartArray();

					g.writeNumber(pt.x);
					g.writeNumber(pt.y);

					if (bExportZs) {
						double z = zs.get(startindex);
						writeDouble(z, g);
					}

					if (bExportMs) {
						double m = ms.get(startindex);
						writeDouble(m, g);
					}

					g.writeEndArray();
				}

				g.writeEndArray();
			}
		}

		g.writeEndArray();

		writeSR(g);

		g.writeEndObject();
		g.close();
	}

	private void exportMultiPointToJson(JsonGenerator g, MultiPoint mpt)
			throws JsonGenerationException, IOException {
		boolean bExportZs = mpt.hasAttribute(Semantics.Z);
		boolean bExportMs = mpt.hasAttribute(Semantics.M);

		g.writeStartObject();

		if (bExportZs) {
			g.writeFieldName("hasZ");
			g.writeBoolean(true);
		}

		if (bExportMs) {
			g.writeFieldName("hasM");
			g.writeBoolean(true);
		}

		g.writeFieldName("points");

		g.writeStartArray();

		if (!mpt.isEmpty()) {
			MultiPointImpl mpImpl = (MultiPointImpl) mpt._getImpl();// get impl
																	// for
																	// faster
																	// access
			AttributeStreamOfDbl zs = null;
			AttributeStreamOfDbl ms = null;

			if (bExportZs)
				zs = (AttributeStreamOfDbl) mpImpl
						.getAttributeStreamRef(Semantics.Z);

			if (bExportMs)
				ms = (AttributeStreamOfDbl) mpImpl
						.getAttributeStreamRef(Semantics.M);

			Point2D pt = new Point2D();
			int n = mpt.getPointCount();
			for (int i = 0; i < n; i++) {
				mpt.getXY(i, pt);

				g.writeStartArray();

				writeDouble(pt.x, g);
				writeDouble(pt.y, g);

				if (bExportZs) {
					double z = zs.get(i);
					writeDouble(z, g);
				}

				if (bExportMs) {
					double m = ms.get(i);
					writeDouble(m, g);
				}

				g.writeEndArray();
			}
		}

		g.writeEndArray();

		writeSR(g);

		g.writeEndObject();
		g.close();
	}

	private void exportPointToJson(JsonGenerator g, Point pt)
			throws JsonGenerationException, IOException {
		boolean bExportZs = pt.hasAttribute(Semantics.Z);
		boolean bExportMs = pt.hasAttribute(Semantics.M);

		g.writeStartObject();

		if (pt.isEmpty()) {
			g.writeFieldName("x");
			g.writeNull();
			g.writeFieldName("y");
			g.writeNull();

			if (bExportZs) {
				g.writeFieldName("z");
				g.writeNull();
			}

			if (bExportMs) {
				g.writeFieldName("m");
				g.writeNull();
			}
		} else {
			g.writeFieldName("x");
			writeDouble(pt.getX(), g);
			g.writeFieldName("y");
			writeDouble(pt.getY(), g);

			if (bExportZs) {
				g.writeFieldName("z");
				writeDouble(pt.getZ(), g);
			}

			if (bExportMs) {
				g.writeFieldName("m");
				writeDouble(pt.getM(), g);
			}
		}

		writeSR(g);

		g.writeEndObject();
		g.close();
	}

	private void exportEnvelopeToJson(JsonGenerator g, Envelope env)
			throws JsonGenerationException, IOException {
		boolean bExportZs = env.hasAttribute(Semantics.Z);
		boolean bExportMs = env.hasAttribute(Semantics.M);

		g.writeStartObject();

		if (env.isEmpty()) {
			g.writeFieldName("xmin");
			g.writeNull();
			g.writeFieldName("ymin");
			g.writeNull();
			g.writeFieldName("xmax");
			g.writeNull();
			g.writeFieldName("ymax");
			g.writeNull();

			if (bExportZs) {
				g.writeFieldName("zmin");
				g.writeNull();
				g.writeFieldName("zmax");
				g.writeNull();
			}

			if (bExportMs) {
				g.writeFieldName("mmin");
				g.writeNull();
				g.writeFieldName("mmax");
				g.writeNull();
			}
		} else {
			g.writeFieldName("xmin");
			writeDouble(env.getXMin(), g);
			g.writeFieldName("ymin");
			writeDouble(env.getYMin(), g);
			g.writeFieldName("xmax");
			writeDouble(env.getXMax(), g);
			g.writeFieldName("ymax");
			writeDouble(env.getYMax(), g);

			if (bExportZs) {
				Envelope1D z = env.queryInterval(Semantics.Z, 0);
				g.writeFieldName("zmin");
				writeDouble(z.vmin, g);
				g.writeFieldName("zmax");
				writeDouble(z.vmax, g);
			}

			if (bExportMs) {
				Envelope1D m = env.queryInterval(Semantics.M, 0);
				g.writeFieldName("mmin");
				writeDouble(m.vmin, g);
				g.writeFieldName("mmax");
				writeDouble(m.vmax, g);
			}
		}

		writeSR(g);

		g.writeEndObject();
		g.close();
	}

	private void writeDouble(double d, JsonGenerator g) throws IOException,
			JsonGenerationException {
		if (NumberUtils.isNaN(d)) {
			g.writeNull();
		} else {
			g.writeNumber(d);
		}

		return;
	}

	private void writeSR(JsonGenerator g) throws IOException,
			JsonGenerationException {
		if (m_wkid > 0) {
			g.writeFieldName("spatialReference");
			g.writeStartObject();

			g.writeFieldName("wkid");
			g.writeNumber(m_wkid);

			if (m_latest_wkid > 0 && m_latest_wkid != m_wkid) {
				g.writeFieldName("latestWkid");
				g.writeNumber(m_latest_wkid);
			}

			g.writeEndObject();
		} else if (m_wkt != null) {
			g.writeFieldName("spatialReference");
			g.writeStartObject();
			g.writeFieldName("wkt");
			g.writeString(m_wkt);
			g.writeEndObject();
		} else
			return;
	}

}
