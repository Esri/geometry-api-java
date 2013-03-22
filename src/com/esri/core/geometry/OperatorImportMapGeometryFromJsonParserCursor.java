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

import java.io.IOException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

class OperatorImportMapGeometryFromJsonParserCursor extends MapGeometryCursor {
	JsonParserCursor m_inputJsonParsers;

	int m_type;

	int m_index;

	public OperatorImportMapGeometryFromJsonParserCursor(int type,
			JsonParserCursor jsonParsers) {
		m_index = -1;
		if (jsonParsers == null)
			throw new IllegalArgumentException();

		m_type = type;
		m_inputJsonParsers = jsonParsers;
	}

	@Override
	public int getGeometryID() {
		return m_index;
	}

	@Override
	public MapGeometry next() {
		JsonParser jsonParser;
		if ((jsonParser = m_inputJsonParsers.next()) != null) {
			m_index = m_inputJsonParsers.getID();
			return importFromJsonParser(jsonParser);
		}
		return null;
	}

	private MapGeometry importFromJsonParser(JsonParser parser) {
		try {
			if (!JSONUtils.isObjectStart(parser))
				return null;

			switch (m_type) {

			case Geometry.GeometryType.Envelope:
				return fromJsonToEnvelope(parser);

			case Geometry.GeometryType.Point:
				return fromJsonToPoint(parser);

			case Geometry.GeometryType.Polygon:
				return fromJsonToPolygon(parser);

			case Geometry.GeometryType.Polyline:
				return fromJsonToPolyline(parser);

			case Geometry.GeometryType.MultiPoint:
				return fromJsonToMultiPoint(parser);

			case Geometry.GeometryType.Unknown:
				return fromJsonToUnknown(parser);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	public static MapGeometry fromJsonToUnknown(JsonParser parser)
			throws Exception {

		SpatialReference spatialReference = null;
		Geometry geom = null;

		boolean isPoint = false;
		boolean isEnvelope = false;

		double x = Double.NaN;
		double y = Double.NaN;
		double xmin = Double.NaN;
		double ymin = Double.NaN;
		double xmax = Double.NaN;
		double ymax = Double.NaN;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			parser.nextToken();

			if ("paths".equals(fieldName))
				geom = parsePolyline(parser);
			else if ("rings".equals(fieldName))
				geom = parsePolygon(parser);
			else if ("points".equals(fieldName))
				geom = parseMultiPoint(parser);
			else if ("spatialReference".equals(fieldName))
				spatialReference = SpatialReference.fromJson(parser);
			else if ("x".equals(fieldName)) {
				isPoint = true;
				x = JSONUtils.readDouble(parser);// x = parser.getDoubleValue();
			} else if ("y".equals(fieldName))
				y = JSONUtils.readDouble(parser);
			else if ("xmin".equals(fieldName)) {
				isEnvelope = true;
				xmin = JSONUtils.readDouble(parser);
			} else if ("ymin".equals(fieldName))
				ymin = JSONUtils.readDouble(parser);
			else if ("xmax".equals(fieldName))
				xmax = JSONUtils.readDouble(parser);
			else if ("ymax".equals(fieldName))
				ymax = JSONUtils.readDouble(parser);
			else
				windup(parser);
		}

		if (isPoint && !NumberUtils.isNaN(x) && !NumberUtils.isNaN(y))
			geom = new Point(x, y);
		else if (isPoint)
			geom = new Point();
		else if (isEnvelope && !NumberUtils.isNaN(xmin)
				&& !NumberUtils.isNaN(ymin) && !NumberUtils.isNaN(xmax)
				&& !NumberUtils.isNaN(ymax))
			geom = new Envelope(xmin, ymin, xmax, ymax);
		else if (isEnvelope)
			geom = new Envelope();

		return new MapGeometry(geom, spatialReference);
	}

	public static MapGeometry fromJsonToEnvelope(JsonParser parser)
			throws Exception {
		SpatialReference spatialReference = null;

		double xmin = Double.NaN;
		double ymin = Double.NaN;
		double xmax = Double.NaN;
		double ymax = Double.NaN;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			// Let's move to value
			parser.nextToken();

			if ("xmin".equals(fieldName))
				xmin = JSONUtils.readDouble(parser);
			else if ("ymin".equals(fieldName))
				ymin = JSONUtils.readDouble(parser);
			else if ("xmax".equals(fieldName))
				xmax = JSONUtils.readDouble(parser);
			else if ("ymax".equals(fieldName))
				ymax = JSONUtils.readDouble(parser);
			else if ("spatialReference".equals(fieldName))
				spatialReference = SpatialReference.fromJson(parser);
			else
				windup(parser);
		}

		Envelope env;
		if (!NumberUtils.isNaN(xmin) && !NumberUtils.isNaN(ymin)
				&& !NumberUtils.isNaN(xmax) && !NumberUtils.isNaN(ymax))
			env = new Envelope(xmin, ymin, xmax, ymax);
		else
			env = new Envelope();

		return new MapGeometry(env, spatialReference);
	}

	public static MapGeometry fromJsonToPoint(JsonParser parser)
			throws Exception {
		SpatialReference spatialReference = null;

		double x = Double.NaN;
		double y = Double.NaN;
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			// Let's move to value
			parser.nextToken();

			if ("x".equals(fieldName))
				x = JSONUtils.readDouble(parser);
			else if ("y".equals(fieldName))
				y = JSONUtils.readDouble(parser);
			else if ("spatialReference".equals(fieldName))
				spatialReference = SpatialReference.fromJson(parser);
			else
				windup(parser);
		}

		Point pt;
		if (!NumberUtils.isNaN(x) && !NumberUtils.isNaN(y))
			pt = new Point(x, y);
		else
			pt = new Point();

		return new MapGeometry(pt, spatialReference);
	}

	public static MapGeometry fromJsonToPolygon(JsonParser parser)
			throws Exception {
		SpatialReference spatialReference = null;
		Geometry geom = null;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			parser.nextToken();

			if ("rings".equals(fieldName))
				geom = parsePolygon(parser);
			else if ("spatialReference".equals(fieldName))
				spatialReference = SpatialReference.fromJson(parser);
			else
				windup(parser);
		}
		return new MapGeometry(geom, spatialReference);
	}

	public static MapGeometry fromJsonToPolyline(JsonParser parser)
			throws Exception {
		SpatialReference spatialReference = null;
		Geometry geom = null;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			parser.nextToken();

			if ("paths".equals(fieldName))
				geom = parsePolyline(parser);
			else if ("spatialReference".equals(fieldName))
				spatialReference = SpatialReference.fromJson(parser);
			else
				windup(parser);
		}

		return new MapGeometry(geom, spatialReference);
	}

	public static MapGeometry fromJsonToMultiPoint(JsonParser parser)
			throws Exception {
		SpatialReference spatialReference = null;
		Geometry geom = null;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			parser.nextToken();

			if ("points".equals(fieldName))
				geom = parseMultiPoint(parser);
			else if ("spatialReference".equals(fieldName))
				spatialReference = SpatialReference.fromJson(parser);
			else
				windup(parser);

		}

		return new MapGeometry(geom, spatialReference);
	}

	private static void windup(JsonParser parser) throws IOException,
			JsonParseException {
		parser.skipChildren();
	}

	private static Geometry parseMultiPoint(JsonParser parser)// FIXME add when
																// z and m are
																// supported: ,
																// boolean
																// hasZs,
																// boolean
																// hasMs)
			throws Exception {
		MultiPoint mpt = new MultiPoint();

		double x;
		double y;
		// FIXME add when z and m are supported
		// double z = 0;
		// double m = 0;

		if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
					parser.nextToken(); // x
					x = JSONUtils.readDouble(parser);// parser.getDoubleValue();

					parser.nextToken(); // y
					y = JSONUtils.readDouble(parser);// parser.getDoubleValue();

					// move from VALUE_NUMBER_FLOAT to either END_ARRAY or
					// VALUE_NUMBER_FLOAT
					parser.nextToken();

					if (parser.getCurrentToken() != JsonToken.END_ARRAY) {
						// FIXME add when z and m are supported
						// if(hasZs)
						// z = JSONUtils.readDouble(parser);
						// else if(hasMs)
						// m = JSONUtils.readDouble(parser);

						// move from VALUE_NUMBER_FLOAT to either END_ARRAY or
						// VALUE_NUMBER_FLOAT
						parser.nextToken();
						if (parser.getCurrentToken() != JsonToken.END_ARRAY) {
							// FIXME add when z and m are supported
							// if(hasMs)
							// m = JSONUtils.readDouble(parser);

							// move from VALUE_NUMBER_FLOAT to END_ARRAY
							parser.nextToken();
						}
					}

					// // possible z token
					// if(parser.getCurrentToken() != JsonToken.END_ARRAY)
					// parser.nextToken();
					//
					// // possible m token
					// if(parser.getCurrentToken() != JsonToken.END_ARRAY)
					// parser.nextToken();

					mpt.add(x, y);

				}
			}
		}
		return mpt;
	}

	private static Geometry parsePolygon(JsonParser parser)// FIXME add when z
															// and m are
															// supported: ,
															// boolean hasZs,
															// boolean hasMs)
			throws Exception {
		Polygon polygon = new Polygon();
		if (parser.getCurrentToken() == JsonToken.START_ARRAY) {

			while (parser.nextToken() != JsonToken.END_ARRAY) {
				// System.out.println("ring start...");
				boolean newPath = true;
				double x = 0;
				double y = 0;
				// FIXME add when z and m are supported
				// double z = 0;
				// double m = 0;

				if (parser.getCurrentToken() == JsonToken.START_ARRAY) {

					JsonToken nextToken = parser.nextToken();
					while (nextToken != JsonToken.END_ARRAY) {
						if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
							parser.nextToken(); // x
							x = JSONUtils.readDouble(parser);
							parser.nextToken(); // y
							y = JSONUtils.readDouble(parser);
						}

						// move from VALUE_NUMBER_FLOAT to either END_ARRAY or
						// VALUE_NUMBER_FLOAT
						parser.nextToken();
						// possible m or z token
						if (parser.getCurrentToken() != JsonToken.END_ARRAY) {
							// FIXME add when z and m are supported
							// if(hasZs)
							// z = JSONUtils.readDouble(parser);
							// else if(hasMs)
							// m = JSONUtils.readDouble(parser);

							// move from VALUE_NUMBER_FLOAT to either END_ARRAY
							// or VALUE_NUMBER_FLOAT
							parser.nextToken();
							if (parser.getCurrentToken() != JsonToken.END_ARRAY) {
								// FIXME add when z and m are supported
								// if(hasMs)
								// m = JSONUtils.readDouble(parser);

								// move from VALUE_NUMBER_FLOAT to END_ARRAY
								parser.nextToken();
							}
						}

						nextToken = parser.nextToken();

						if (newPath) {
							newPath = false;
							polygon.startPath(x, y);
						} else {
							if (nextToken != JsonToken.END_ARRAY) // keep adding
																	// lines
																	// except
																	// for the
																	// last
																	// (repeated)
																	// point in
																	// the
								// ring
								polygon.lineTo(x, y);
						}

					}
					// polygon.closePathWithLine();
					// System.out.println("ring end...");
				}
			}
		}

		return polygon;
	}

	private static Geometry parsePolyline(JsonParser parser)// FIXME add when z
															// and m are
															// supported: ,
															// boolean hasZs,
															// boolean hasMs)
			throws Exception {
		Polyline polyline = new Polyline();
		if (parser.getCurrentToken() == JsonToken.START_ARRAY) {

			while (parser.nextToken() != JsonToken.END_ARRAY) {
				// System.out.println("ring start...");
				boolean newPath = true;
				double x;
				double y;
				// FIXME add when z and m are supported
				// double z = 0;
				// double m = 0;

				if (parser.getCurrentToken() == JsonToken.START_ARRAY) {

					while (parser.nextToken() != JsonToken.END_ARRAY) {
						if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
							parser.nextToken(); // x
							x = JSONUtils.readDouble(parser);
							parser.nextToken(); // y
							y = JSONUtils.readDouble(parser);

							// move from VALUE_NUMBER_FLOAT to either END_ARRAY
							// or VALUE_NUMBER_FLOAT
							parser.nextToken();
							// possible m or z token
							if (parser.getCurrentToken() != JsonToken.END_ARRAY) {
								// FIXME add when z and m are supported
								// if(hasZs)
								// z = JSONUtils.readDouble(parser);
								// else if(hasMs)
								// m = JSONUtils.readDouble(parser);

								// move from VALUE_NUMBER_FLOAT to either
								// END_ARRAY or VALUE_NUMBER_FLOAT
								parser.nextToken();
								if (parser.getCurrentToken() != JsonToken.END_ARRAY) {
									// FIXME add when z and m are supported
									// if(hasMs)
									// m = JSONUtils.readDouble(parser);

									// move from VALUE_NUMBER_FLOAT to END_ARRAY
									parser.nextToken();
								}
							}

							// // possible z token
							// if(parser.getCurrentToken() !=
							// JsonToken.END_ARRAY)
							// parser.nextToken();
							//
							// // possible m token
							// if(parser.getCurrentToken() !=
							// JsonToken.END_ARRAY)
							// parser.nextToken();

							if (newPath) {
								newPath = false;
								polyline.startPath(x, y);
							} else {
								polyline.lineTo(x, y);
							}

						}
					}
					// System.out.println("ring end...");
				}
			}
		}
		return polyline;
	}

}
