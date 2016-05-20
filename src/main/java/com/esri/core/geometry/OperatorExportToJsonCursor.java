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

import com.esri.core.geometry.VertexDescription.Semantics;

import java.io.IOException;
import java.util.Map;

class OperatorExportToJsonCursor extends JsonCursor {

	GeometryCursor m_inputGeometryCursor;
	SpatialReference m_spatialReference;
	int m_index;

	public OperatorExportToJsonCursor(SpatialReference spatialReference, GeometryCursor geometryCursor) {
		m_index = -1;
		if (geometryCursor == null) {
			throw new IllegalArgumentException();
		}

		m_inputGeometryCursor = geometryCursor;
		m_spatialReference = spatialReference;
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
			return exportToString(geometry, m_spatialReference, null);
		}
		return null;
	}

	static String exportToString(Geometry geometry, SpatialReference spatialReference, Map<String, Object> exportProperties) {
		JsonWriter jsonWriter = new JsonStringWriter();
		exportToJson_(geometry, spatialReference, jsonWriter, exportProperties);
		return (String) jsonWriter.getJson();
	}

	private static void exportToJson_(Geometry geometry, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		try {
			int type = geometry.getType().value();
			switch (type) {
			case Geometry.GeometryType.Point:
				exportPointToJson((Point) geometry, spatialReference, jsonWriter, exportProperties);
				break;

			case Geometry.GeometryType.MultiPoint:
				exportMultiPointToJson((MultiPoint) geometry, spatialReference, jsonWriter, exportProperties);
				break;

			case Geometry.GeometryType.Polyline:
				exportPolylineToJson((Polyline) geometry, spatialReference, jsonWriter, exportProperties);
				break;

			case Geometry.GeometryType.Polygon:
				exportPolygonToJson((Polygon) geometry, spatialReference, jsonWriter, exportProperties);
				break;

			case Geometry.GeometryType.Envelope:
				exportEnvelopeToJson((Envelope) geometry, spatialReference, jsonWriter, exportProperties);
				break;

			default:
				throw new RuntimeException("not implemented for this geometry type");
			}

		} catch (Exception e) {
		}

	}

	private static void exportPolygonToJson(Polygon pp, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		exportPolypathToJson(pp, "rings", spatialReference, jsonWriter, exportProperties);
	}

	private static void exportPolylineToJson(Polyline pp, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		exportPolypathToJson(pp, "paths", spatialReference, jsonWriter, exportProperties);
	}

	private static void exportPolypathToJson(MultiPath pp, String name, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		boolean bExportZs = pp.hasAttribute(Semantics.Z);
		boolean bExportMs = pp.hasAttribute(Semantics.M);

		boolean bPositionAsF = false;
		int decimals = 17;

		if (exportProperties != null) {
			Object numberOfDecimalsXY = exportProperties.get("numberOfDecimalsXY");
			if (numberOfDecimalsXY != null && numberOfDecimalsXY instanceof Number) {
				bPositionAsF = true;
				decimals = ((Number) numberOfDecimalsXY).intValue();
			}
		}

		jsonWriter.startObject();

		if (bExportZs) {
			jsonWriter.addPairBoolean("hasZ", true);
		}

		if (bExportMs) {
			jsonWriter.addPairBoolean("hasM", true);
		}

		jsonWriter.addPairArray(name);

		if (!pp.isEmpty()) {
			int n = pp.getPathCount(); // rings or paths

			MultiPathImpl mpImpl = (MultiPathImpl) pp._getImpl();// get impl for
			// faster
			// access
			AttributeStreamOfDbl zs = null;
			AttributeStreamOfDbl ms = null;

			if (bExportZs) {
				zs = (AttributeStreamOfDbl) mpImpl.getAttributeStreamRef(Semantics.Z);
			}

			if (bExportMs) {
				ms = (AttributeStreamOfDbl) mpImpl.getAttributeStreamRef(Semantics.M);
			}

			boolean bPolygon = pp instanceof Polygon;
			Point2D pt = new Point2D();

			for (int i = 0; i < n; i++) {
				jsonWriter.addValueArray();
				int startindex = pp.getPathStart(i);
				int numVertices = pp.getPathSize(i);
				double startx = 0.0, starty = 0.0, startz = NumberUtils.NaN(), startm = NumberUtils.NaN();
				double z = NumberUtils.NaN(), m = NumberUtils.NaN();
				boolean bClosed = pp.isClosedPath(i);
				for (int j = startindex; j < startindex + numVertices; j++) {
					pp.getXY(j, pt);

					jsonWriter.addValueArray();

					if (bPositionAsF) {
						jsonWriter.addValueDouble(pt.x, decimals, true);
						jsonWriter.addValueDouble(pt.y, decimals, true);
					} else {
						jsonWriter.addValueDouble(pt.x);
						jsonWriter.addValueDouble(pt.y);
					}

					if (bExportZs) {
						z = zs.get(j);
						jsonWriter.addValueDouble(z);
					}

					if (bExportMs) {
						m = ms.get(j);
						jsonWriter.addValueDouble(m);
					}

					if (j == startindex && bClosed) {
						startx = pt.x;
						starty = pt.y;
						startz = z;
						startm = m;
					}

					jsonWriter.endArray();
				}

				// Close the Path/Ring by writing the Point at the start index
				if (bClosed && (startx != pt.x || starty != pt.y || (bExportZs && !(NumberUtils.isNaN(startz) && NumberUtils.isNaN(z)) && startz != z) || (bExportMs && !(NumberUtils.isNaN(startm) && NumberUtils.isNaN(m)) && startm != m))) {
					pp.getXY(startindex, pt);
					// getPoint(startindex);
					jsonWriter.addValueArray();

					if (bPositionAsF) {
						jsonWriter.addValueDouble(pt.x, decimals, true);
						jsonWriter.addValueDouble(pt.y, decimals, true);
					} else {
						jsonWriter.addValueDouble(pt.x);
						jsonWriter.addValueDouble(pt.y);
					}

					if (bExportZs) {
						z = zs.get(startindex);
						jsonWriter.addValueDouble(z);
					}

					if (bExportMs) {
						m = ms.get(startindex);
						jsonWriter.addValueDouble(m);
					}

					jsonWriter.endArray();
				}

				jsonWriter.endArray();
			}
		}

		jsonWriter.endArray();

		if (spatialReference != null) {
			writeSR(spatialReference, jsonWriter);
		}

		jsonWriter.endObject();
	}

	private static void exportMultiPointToJson(MultiPoint mpt, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		boolean bExportZs = mpt.hasAttribute(Semantics.Z);
		boolean bExportMs = mpt.hasAttribute(Semantics.M);

		boolean bPositionAsF = false;
		int decimals = 17;

		if (exportProperties != null) {
			Object numberOfDecimalsXY = exportProperties.get("numberOfDecimalsXY");
			if (numberOfDecimalsXY != null && numberOfDecimalsXY instanceof Number) {
				bPositionAsF = true;
				decimals = ((Number) numberOfDecimalsXY).intValue();
			}
		}

		jsonWriter.startObject();

		if (bExportZs) {
			jsonWriter.addPairBoolean("hasZ", true);
		}

		if (bExportMs) {
			jsonWriter.addPairBoolean("hasM", true);
		}

		jsonWriter.addPairArray("points");

		if (!mpt.isEmpty()) {
			MultiPointImpl mpImpl = (MultiPointImpl) mpt._getImpl();// get impl
			// for
			// faster
			// access
			AttributeStreamOfDbl zs = null;
			AttributeStreamOfDbl ms = null;

			if (bExportZs) {
				zs = (AttributeStreamOfDbl) mpImpl.getAttributeStreamRef(Semantics.Z);
			}

			if (bExportMs) {
				ms = (AttributeStreamOfDbl) mpImpl.getAttributeStreamRef(Semantics.M);
			}

			Point2D pt = new Point2D();
			int n = mpt.getPointCount();
			for (int i = 0; i < n; i++) {
				mpt.getXY(i, pt);

				jsonWriter.addValueArray();

				if (bPositionAsF) {
					jsonWriter.addValueDouble(pt.x, decimals, true);
					jsonWriter.addValueDouble(pt.y, decimals, true);
				} else {
					jsonWriter.addValueDouble(pt.x);
					jsonWriter.addValueDouble(pt.y);
				}

				if (bExportZs) {
					double z = zs.get(i);
					jsonWriter.addValueDouble(z);
				}

				if (bExportMs) {
					double m = ms.get(i);
					jsonWriter.addValueDouble(m);
				}

				jsonWriter.endArray();
			}
		}

		jsonWriter.endArray();

		if (spatialReference != null) {
			writeSR(spatialReference, jsonWriter);
		}

		jsonWriter.endObject();
	}

	private static void exportPointToJson(Point pt, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		boolean bExportZs = pt.hasAttribute(Semantics.Z);
		boolean bExportMs = pt.hasAttribute(Semantics.M);

		boolean bPositionAsF = false;
		int decimals = 17;

		if (exportProperties != null) {
			Object numberOfDecimalsXY = exportProperties.get("numberOfDecimalsXY");
			if (numberOfDecimalsXY != null && numberOfDecimalsXY instanceof Number) {
				bPositionAsF = true;
				decimals = ((Number) numberOfDecimalsXY).intValue();
			}
		}

		jsonWriter.startObject();

		if (pt.isEmpty()) {
			jsonWriter.addPairNull("x");
			jsonWriter.addPairNull("y");

			if (bExportZs) {
				jsonWriter.addPairNull("z");
			}

			if (bExportMs) {
				jsonWriter.addPairNull("m");
			}
		} else {

			if (bPositionAsF) {
				jsonWriter.addPairDouble("x", pt.getX(), decimals, true);
				jsonWriter.addPairDouble("y", pt.getY(), decimals, true);
			} else {
				jsonWriter.addPairDouble("x", pt.getX());
				jsonWriter.addPairDouble("y", pt.getY());
			}

			if (bExportZs) {
				jsonWriter.addPairDouble("z", pt.getZ());
			}

			if (bExportMs) {
				jsonWriter.addPairDouble("m", pt.getM());
			}
		}

		if (spatialReference != null) {
			writeSR(spatialReference, jsonWriter);
		}

		jsonWriter.endObject();
	}

	private static void exportEnvelopeToJson(Envelope env, SpatialReference spatialReference, JsonWriter jsonWriter, Map<String, Object> exportProperties) {
		boolean bExportZs = env.hasAttribute(Semantics.Z);
		boolean bExportMs = env.hasAttribute(Semantics.M);

		boolean bPositionAsF = false;
		int decimals = 17;

		if (exportProperties != null) {
			Object numberOfDecimalsXY = exportProperties.get("numberOfDecimalsXY");
			if (numberOfDecimalsXY != null && numberOfDecimalsXY instanceof Number) {
				bPositionAsF = true;
				decimals = ((Number) numberOfDecimalsXY).intValue();
			}
		}

		jsonWriter.startObject();

		if (env.isEmpty()) {
			jsonWriter.addPairNull("xmin");
			jsonWriter.addPairNull("ymin");
			jsonWriter.addPairNull("xmax");
			jsonWriter.addPairNull("ymax");

			if (bExportZs) {
				jsonWriter.addPairNull("zmin");
				jsonWriter.addPairNull("zmax");
			}

			if (bExportMs) {
				jsonWriter.addPairNull("mmin");
				jsonWriter.addPairNull("mmax");
			}
		} else {

			if (bPositionAsF) {
				jsonWriter.addPairDouble("xmin", env.getXMin(), decimals, true);
				jsonWriter.addPairDouble("ymin", env.getYMin(), decimals, true);
				jsonWriter.addPairDouble("xmax", env.getXMax(), decimals, true);
				jsonWriter.addPairDouble("ymax", env.getYMax(), decimals, true);
			} else {
				jsonWriter.addPairDouble("xmin", env.getXMin());
				jsonWriter.addPairDouble("ymin", env.getYMin());
				jsonWriter.addPairDouble("xmax", env.getXMax());
				jsonWriter.addPairDouble("ymax", env.getYMax());
			}

			if (bExportZs) {
				Envelope1D z = env.queryInterval(Semantics.Z, 0);
				jsonWriter.addPairDouble("zmin", z.vmin);
				jsonWriter.addPairDouble("zmax", z.vmax);
			}

			if (bExportMs) {
				Envelope1D m = env.queryInterval(Semantics.M, 0);
				jsonWriter.addPairDouble("mmin", m.vmin);
				jsonWriter.addPairDouble("mmax", m.vmax);
			}
		}

		if (spatialReference != null) {
			writeSR(spatialReference, jsonWriter);
		}

		jsonWriter.endObject();
	}

	private static void writeSR(SpatialReference spatialReference, JsonWriter jsonWriter) {
		int wkid = spatialReference.getOldID();
		if (wkid > 0) {
			jsonWriter.addPairObject("spatialReference");

			jsonWriter.addPairInt("wkid", wkid);

			int latest_wkid = spatialReference.getLatestID();
			if (latest_wkid > 0 && latest_wkid != wkid) {
				jsonWriter.addPairInt("latestWkid", latest_wkid);
			}

			jsonWriter.endObject();
		} else {
			String wkt = spatialReference.getText();
			if (wkt != null) {
				jsonWriter.addPairObject("spatialReference");
				jsonWriter.addPairString("wkt", wkt);
				jsonWriter.endObject();
			}
		}
	}
}
