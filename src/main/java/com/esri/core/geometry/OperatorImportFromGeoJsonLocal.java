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
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

class OperatorImportFromGeoJsonLocal extends OperatorImportFromGeoJson {

	@Override
	public MapGeometry execute(int importFlags, Geometry.Type type, String geoJsonString,
			ProgressTracker progressTracker) throws JSONException {
		try {
			JsonFactory factory = new JsonFactory();
			JsonParser jsonParser = factory.createJsonParser(geoJsonString);

			jsonParser.nextToken();

			MapGeometry map_geometry = OperatorImportFromGeoJsonHelper.importFromGeoJson(importFlags, type,
					new JsonParserReader(jsonParser), progressTracker, false);
			return map_geometry;

		} catch (JSONException jsonException) {
			throw jsonException;
		} catch (JsonParseException jsonParseException) {
			throw new JSONException(jsonParseException.getMessage());
		} catch (IOException ioException) {
			throw new JSONException(ioException.getMessage());
		}
	}

	@Override
	public MapGeometry execute(int importFlags, Geometry.Type type, JSONObject jsonObject,
			ProgressTracker progressTracker) throws JSONException {
		if (jsonObject == null)
			return null;

		try {
			return OperatorImportFromGeoJsonHelper.importFromGeoJson(importFlags, type, new JsonValueReader(jsonObject),
					progressTracker, false);
		} catch (JSONException jsonException) {
			throw jsonException;
		} catch (JsonParseException jsonParseException) {
			throw new JSONException(jsonParseException.getMessage());
		} catch (IOException ioException) {
			throw new JSONException(ioException.getMessage());
		}
	}

	static final class OperatorImportFromGeoJsonHelper {

		private AttributeStreamOfDbl m_position;
		private AttributeStreamOfDbl m_zs;
		private AttributeStreamOfDbl m_ms;
		private AttributeStreamOfInt32 m_paths;
		private AttributeStreamOfInt8 m_path_flags;
		private Point m_point; // special case for Points
		private boolean m_b_has_zs;
		private boolean m_b_has_ms;
		private boolean m_b_has_zs_known;
		private boolean m_b_has_ms_known;
		private int m_num_embeddings;

		OperatorImportFromGeoJsonHelper() {
			m_position = null;
			m_zs = null;
			m_ms = null;
			m_paths = null;
			m_path_flags = null;
			m_point = null;
			m_b_has_zs = false;
			m_b_has_ms = false;
			m_b_has_zs_known = false;
			m_b_has_ms_known = false;
			m_num_embeddings = 0;
		}

		static MapGeometry importFromGeoJson(int importFlags, Geometry.Type type, JsonReader json_iterator,
				ProgressTracker progress_tracker, boolean skip_coordinates)
						throws JSONException, JsonParseException, IOException {
			assert(json_iterator.currentToken() == JsonToken.START_OBJECT);

			OperatorImportFromGeoJsonHelper geo_json_helper = new OperatorImportFromGeoJsonHelper();
			boolean b_type_found = false;
			boolean b_coordinates_found = false;
			boolean b_crs_found = false;
			boolean b_crsURN_found = false;
			String geo_json_type = null;

			Geometry geometry = null;
			SpatialReference spatial_reference = null;

			JsonToken current_token;
			String field_name = null;

			while ((current_token = json_iterator.nextToken()) != JsonToken.END_OBJECT) {
				field_name = json_iterator.currentString();

				if (field_name.equals("type")) {
					if (b_type_found) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_type_found = true;
					current_token = json_iterator.nextToken();

					if (current_token != JsonToken.VALUE_STRING) {
						throw new IllegalArgumentException("invalid argument");
					}

					geo_json_type = json_iterator.currentString();
				} else if (field_name.equals("coordinates")) {
					if (b_coordinates_found) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_coordinates_found = true;
					current_token = json_iterator.nextToken();

					if (skip_coordinates) {
						json_iterator.skipChildren();
					} else {// According to the spec, the value of the
							// coordinates must be an array. However, I do an
							// extra check for null too.
						if (current_token != JsonToken.VALUE_NULL) {
							if (current_token != JsonToken.START_ARRAY) {
								throw new IllegalArgumentException("invalid argument");
							}

							geo_json_helper.import_coordinates_(json_iterator, progress_tracker);
						}
					}
				} else if (field_name.equals("crs")) {
					if (b_crs_found || b_crsURN_found) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_crs_found = true;
					current_token = json_iterator.nextToken();

					if ((importFlags & GeoJsonImportFlags.geoJsonImportSkipCRS) == 0)
						spatial_reference = importSpatialReferenceFromCrs(json_iterator, progress_tracker);
					else
						json_iterator.skipChildren();
				} else if (field_name.equals("crsURN")) {
					if (b_crs_found || b_crsURN_found) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_crsURN_found = true;
					current_token = json_iterator.nextToken();

					spatial_reference = importSpatialReferenceFromCrsUrn_(json_iterator,
							progress_tracker);
				} else {
					json_iterator.nextToken();
					json_iterator.skipChildren();
				}
			}

			// According to the spec, a GeoJSON object must have both a type and
			// a coordinates array
			if (!b_type_found || (!b_coordinates_found && !skip_coordinates)) {
				throw new IllegalArgumentException("invalid argument");
			}

			if (!skip_coordinates)
				geometry = geo_json_helper.createGeometry_(geo_json_type, type.value());

			if (!b_crs_found && !b_crsURN_found && ((importFlags & GeoJsonImportFlags.geoJsonImportSkipCRS) == 0)
					&& ((importFlags & GeoJsonImportFlags.geoJsonImportNoWGS84Default) == 0)) {
				spatial_reference = SpatialReference.create(4326); // the spec
																	// gives a
																	// default
																	// of 4326
																	// if no crs
																	// is given
			}

			MapGeometry map_geometry = new MapGeometry(geometry, spatial_reference);

			assert(geo_json_helper.m_paths == null || (geo_json_helper.m_path_flags != null
					&& geo_json_helper.m_paths.size() == geo_json_helper.m_path_flags.size()));

			return map_geometry;
		}

		// We have to import the coordinates in the most general way possible to
		// not assume the type of geometry we're parsing.
		// JSON allows for unordered objects, so it's possible that the
		// coordinates array can come before the type tag when parsing
		// sequentially, otherwise
		// we would have to parse using a JSON_object, which would be easier,
		// but not as space/time efficient. So this function blindly imports the
		// coordinates
		// into the attribute stream(s), and will later assign them to a
		// geometry after the type tag is found.
		private void import_coordinates_(JsonReader json_iterator, ProgressTracker progress_tracker)
				throws JSONException, JsonParseException, IOException {
			assert(json_iterator.currentToken() == JsonToken.START_ARRAY);

			int coordinates_level_lower = 1;
			int coordinates_level_upper = 4;

			json_iterator.nextToken();

			while (json_iterator.currentToken() != JsonToken.END_ARRAY) {
				if (isDouble_(json_iterator)) {
					if (coordinates_level_upper > 1) {
						coordinates_level_upper = 1;
					}
				} else if (json_iterator.currentToken() == JsonToken.START_ARRAY) {
					if (coordinates_level_lower < 2) {
						coordinates_level_lower = 2;
					}
				} else {
					throw new IllegalArgumentException("invalid argument");
				}

				if (coordinates_level_lower > coordinates_level_upper) {
					throw new IllegalArgumentException("invalid argument");
				}

				if (coordinates_level_lower == coordinates_level_upper && coordinates_level_lower == 1) {// special
																											// code
																											// for
																											// Points
					readCoordinateAsPoint_(json_iterator);
				} else {
					boolean b_add_path_level_3 = true;
					boolean b_polygon_start_level_4 = true;

					assert(json_iterator.currentToken() == JsonToken.START_ARRAY);
					json_iterator.nextToken();

					while (json_iterator.currentToken() != JsonToken.END_ARRAY) {
						if (isDouble_(json_iterator)) {
							if (coordinates_level_upper > 2) {
								coordinates_level_upper = 2;
							}
						} else if (json_iterator.currentToken() == JsonToken.START_ARRAY) {
							if (coordinates_level_lower < 3) {
								coordinates_level_lower = 3;
							}
						} else {
							throw new IllegalArgumentException("invalid argument");
						}

						if (coordinates_level_lower > coordinates_level_upper) {
							throw new IllegalArgumentException("invalid argument");
						}

						if (coordinates_level_lower == coordinates_level_upper && coordinates_level_lower == 2) {// LineString
																													// or
																													// MultiPoint
							addCoordinate_(json_iterator);
						} else {
							boolean b_add_path_level_4 = true;

							assert(json_iterator.currentToken() == JsonToken.START_ARRAY);
							json_iterator.nextToken();

							while (json_iterator.currentToken() != JsonToken.END_ARRAY) {
								if (isDouble_(json_iterator)) {
									if (coordinates_level_upper > 3) {
										coordinates_level_upper = 3;
									}
								} else if (json_iterator.currentToken() == JsonToken.START_ARRAY) {
									if (coordinates_level_lower < 4) {
										coordinates_level_lower = 4;
									}
								} else {
									throw new IllegalArgumentException("invalid argument");
								}

								if (coordinates_level_lower > coordinates_level_upper) {
									throw new IllegalArgumentException("invalid argument");
								}

								if (coordinates_level_lower == coordinates_level_upper
										&& coordinates_level_lower == 3) {// Polygon
																			// or
																			// MultiLineString
									if (b_add_path_level_3) {
										addPath_();
										b_add_path_level_3 = false;
									}

									addCoordinate_(json_iterator);
								} else {
									assert(json_iterator.currentToken() == JsonToken.START_ARRAY);
									json_iterator.nextToken();

									if (json_iterator.currentToken() != JsonToken.END_ARRAY) {
										if (!isDouble_(json_iterator)) {
											throw new IllegalArgumentException("invalid argument");
										}

										assert(coordinates_level_lower == coordinates_level_upper
												&& coordinates_level_lower == 4);
										// MultiPolygon

										if (b_add_path_level_4) {
											addPath_();
											addPathFlag_(b_polygon_start_level_4);
											b_add_path_level_4 = false;
											b_polygon_start_level_4 = false;
										}

										addCoordinate_(json_iterator);
									}

									json_iterator.nextToken();
								}
							}

							json_iterator.nextToken();
						}
					}

					json_iterator.nextToken();
				}
			}

			if (m_paths != null) {
				m_paths.add(m_position.size() / 2); // add final path size
			}
			if (m_path_flags != null) {
				m_path_flags.add((byte) 0); // to match the paths size
			}

			m_num_embeddings = coordinates_level_lower;
		}

		private void readCoordinateAsPoint_(JsonReader json_iterator)
				throws JSONException, JsonParseException, IOException {
			assert(isDouble_(json_iterator));

			m_point = new Point();

			double x = readDouble_(json_iterator);
			json_iterator.nextToken();
			double y = readDouble_(json_iterator);
			json_iterator.nextToken();

			if (NumberUtils.isNaN(y)) {
				x = NumberUtils.NaN();
			}

			m_point.setXY(x, y);

			if (isDouble_(json_iterator)) {
				double z = readDouble_(json_iterator);
				json_iterator.nextToken();
				m_point.setZ(z);
			}

			if (isDouble_(json_iterator)) {
				double m = readDouble_(json_iterator);
				json_iterator.nextToken();
				m_point.setM(m);
			}

			if (json_iterator.currentToken() != JsonToken.END_ARRAY) {
				throw new IllegalArgumentException("invalid argument");
			}
		}

		private void addCoordinate_(JsonReader json_iterator) throws JSONException, JsonParseException, IOException {
			assert(isDouble_(json_iterator));

			if (m_position == null) {
				m_position = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(0);
			}

			double x = readDouble_(json_iterator);
			json_iterator.nextToken();
			double y = readDouble_(json_iterator);
			json_iterator.nextToken();

			int size = m_position.size();

			m_position.add(x);
			m_position.add(y);

			if (isDouble_(json_iterator)) {
				if (!m_b_has_zs_known) {
					m_b_has_zs_known = true;
					m_b_has_zs = true;
					m_zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(0);
				} else {
					if (!m_b_has_zs) {
						m_zs = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(size >> 1,
								VertexDescription.getDefaultValue(Semantics.Z));
						m_b_has_zs = true;
					}
				}

				double z = readDouble_(json_iterator);
				json_iterator.nextToken();
				m_zs.add(z);
			} else {
				if (!m_b_has_zs_known) {
					m_b_has_zs_known = true;
					m_b_has_zs = false;
				} else {
					if (m_b_has_zs) {
						m_zs.add(VertexDescription.getDefaultValue(Semantics.Z));
					}
				}
			}

			if (isDouble_(json_iterator)) {
				if (!m_b_has_ms_known) {
					m_b_has_ms_known = true;
					m_b_has_ms = true;
					m_ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(0);
				} else {
					if (!m_b_has_ms) {
						m_ms = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(size >> 1,
								VertexDescription.getDefaultValue(Semantics.M));
						m_b_has_ms = true;
					}
				}

				double m = readDouble_(json_iterator);
				json_iterator.nextToken();
				m_ms.add(m);
			} else {
				if (!m_b_has_ms_known) {
					m_b_has_ms_known = true;
					m_b_has_ms = false;
				} else {
					if (m_b_has_ms) {
						m_zs.add(VertexDescription.getDefaultValue(Semantics.M));
					}
				}
			}

			if (json_iterator.currentToken() != JsonToken.END_ARRAY) {
				throw new IllegalArgumentException("invalid argument");
			}
		}

		private void addPath_() {
			if (m_paths == null) {
				m_paths = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(0);
			}

			if (m_position == null) {
				m_paths.add(0);
			} else {
				m_paths.add(m_position.size() / 2);
			}
		}

		private void addPathFlag_(boolean b_polygon_start) {
			if (m_path_flags == null) {
				m_path_flags = (AttributeStreamOfInt8) AttributeStreamBase.createByteStream(0);
			}

			if (b_polygon_start) {
				m_path_flags.add((byte) (PathFlags.enumClosed | PathFlags.enumOGCStartPolygon));
			} else {
				m_path_flags.add((byte) PathFlags.enumClosed);
			}
		}

		private double readDouble_(JsonReader json_iterator) throws JSONException, JsonParseException, IOException {
			JsonToken current_token = json_iterator.currentToken();
			if (current_token == JsonToken.VALUE_NULL
					|| (current_token == JsonToken.VALUE_STRING && json_iterator.currentString().equals("NaN"))) {
				return NumberUtils.NaN();
			} else {
				return json_iterator.currentDoubleValue();
			}
		}

		private boolean isDouble_(JsonReader json_iterator) throws JSONException, JsonParseException, IOException {
			JsonToken current_token = json_iterator.currentToken();

			if (current_token == JsonToken.VALUE_NUMBER_FLOAT) {
				return true;
			}

			if (current_token == JsonToken.VALUE_NUMBER_INT) {
				return true;
			}

			if (current_token == JsonToken.VALUE_NULL
					|| (current_token == JsonToken.VALUE_STRING && json_iterator.currentString().equals("NaN"))) {
				return true;
			}

			return false;
		}

		private Geometry createGeometry_(String geo_json_type, int type)
				throws JSONException, JsonParseException, IOException {
			Geometry geometry;

			if (type != Geometry.GeometryType.Unknown) {
				switch (type) {
				case Geometry.GeometryType.Polygon:
					if (!geo_json_type.equals("MultiPolygon") && !geo_json_type.equals("Polygon")) {
						throw new GeometryException("invalid shape type");
					}
					break;
				case Geometry.GeometryType.Polyline:
					if (!geo_json_type.equals("MultiLineString") && !geo_json_type.equals("LineString")) {
						throw new GeometryException("invalid shape type");
					}
					break;
				case Geometry.GeometryType.MultiPoint:
					if (!geo_json_type.equals("MultiPoint")) {
						throw new GeometryException("invalid shape type");
					}
					break;
				case Geometry.GeometryType.Point:
					if (!geo_json_type.equals("Point")) {
						throw new GeometryException("invalid shape type");
					}
					break;
				default:
					throw new GeometryException("invalid shape type");
				}
			}

			if (m_position == null && m_point == null) {
				if (geo_json_type.equals("Point")) {
					if (m_num_embeddings > 1) {
						throw new IllegalArgumentException("invalid argument");
					}

					geometry = new Point();
				} else if (geo_json_type.equals("MultiPoint")) {
					if (m_num_embeddings > 2) {
						throw new IllegalArgumentException("invalid argument");
					}

					geometry = new MultiPoint();
				} else if (geo_json_type.equals("LineString")) {
					if (m_num_embeddings > 2) {
						throw new IllegalArgumentException("invalid argument");
					}

					geometry = new Polyline();
				} else if (geo_json_type.equals("MultiLineString")) {
					if (m_num_embeddings > 3) {
						throw new IllegalArgumentException("invalid argument");
					}

					geometry = new Polyline();
				} else if (geo_json_type.equals("Polygon")) {
					if (m_num_embeddings > 3) {
						throw new IllegalArgumentException("invalid argument");
					}

					geometry = new Polygon();
				} else if (geo_json_type.equals("MultiPolygon")) {
					assert(m_num_embeddings <= 4);
					geometry = new Polygon();
				} else {
					throw new IllegalArgumentException("invalid argument");
				}
			} else if (m_num_embeddings == 1) {
				if (!geo_json_type.equals("Point")) {
					throw new IllegalArgumentException("invalid argument");
				}

				assert(m_point != null);
				geometry = m_point;
			} else if (m_num_embeddings == 2) {
				if (geo_json_type.equals("MultiPoint")) {
					geometry = createMultiPointFromStreams_();
				} else if (geo_json_type.equals("LineString")) {
					geometry = createPolylineFromStreams_();
				} else {
					throw new IllegalArgumentException("invalid argument");
				}
			} else if (m_num_embeddings == 3) {
				if (geo_json_type.equals("Polygon")) {
					geometry = createPolygonFromStreams_();
				} else if (geo_json_type.equals("MultiLineString")) {
					geometry = createPolylineFromStreams_();
				} else {
					throw new IllegalArgumentException("invalid argument");
				}
			} else {
				if (!geo_json_type.equals("MultiPolygon")) {
					throw new IllegalArgumentException("invalid argument");
				}

				geometry = createPolygonFromStreams_();
			}

			return geometry;
		}

		private Geometry createPolygonFromStreams_() {
			assert(m_position != null);
			assert(m_paths != null);
			assert((m_num_embeddings == 3 && m_path_flags == null) || (m_num_embeddings == 4 && m_path_flags != null));

			Polygon polygon = new Polygon();
			MultiPathImpl multi_path_impl = (MultiPathImpl) polygon._getImpl();

			checkPathPointCountsForMultiPath_(true);
			multi_path_impl.setAttributeStreamRef(Semantics.POSITION, m_position);

			if (m_b_has_zs) {
				assert(m_zs != null);
				multi_path_impl.setAttributeStreamRef(Semantics.Z, m_zs);
			}

			if (m_b_has_ms) {
				assert(m_ms != null);
				multi_path_impl.setAttributeStreamRef(Semantics.M, m_ms);
			}

			if (m_path_flags == null) {
				m_path_flags = (AttributeStreamOfInt8) AttributeStreamBase.createByteStream(m_paths.size(), (byte) 0);
				m_path_flags.setBits(0, (byte) (PathFlags.enumClosed | PathFlags.enumOGCStartPolygon));

				for (int i = 1; i < m_path_flags.size() - 1; i++) {
					m_path_flags.setBits(i, (byte) PathFlags.enumClosed);
				}
			}

			multi_path_impl.setPathStreamRef(m_paths);
			multi_path_impl.setPathFlagsStreamRef(m_path_flags);
			multi_path_impl.notifyModified(MultiVertexGeometryImpl.DirtyFlags.DirtyAll);

			AttributeStreamOfInt8 path_flags_clone = new AttributeStreamOfInt8(m_path_flags);

			for (int i = 0; i < path_flags_clone.size() - 1; i++) {
				assert((path_flags_clone.read(i) & PathFlags.enumClosed) != 0);
				assert((m_path_flags.read(i) & PathFlags.enumClosed) != 0);

				if ((path_flags_clone.read(i) & PathFlags.enumOGCStartPolygon) != 0) {// Should
																						// be
																						// clockwise
					if (!InternalUtils.isClockwiseRing(multi_path_impl, i)) {
						multi_path_impl.reversePath(i); // make clockwise
					}
				} else {// Should be counter-clockwise
					if (InternalUtils.isClockwiseRing(multi_path_impl, i)) {
						multi_path_impl.reversePath(i); // make
														// counter-clockwise
					}
				}
			}
			multi_path_impl.setPathFlagsStreamRef(path_flags_clone);
			multi_path_impl.setDirtyOGCFlags(false);

			return polygon;
		}

		private Geometry createPolylineFromStreams_() {
			assert(m_position != null);
			assert((m_num_embeddings == 2 && m_paths == null) || (m_num_embeddings == 3 && m_paths != null));
			assert(m_path_flags == null);

			Polyline polyline = new Polyline();
			MultiPathImpl multi_path_impl = (MultiPathImpl) polyline._getImpl();

			if (m_paths == null) {
				m_paths = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(0);
				m_paths.add(0);
				m_paths.add(m_position.size() / 2);
			}

			checkPathPointCountsForMultiPath_(false);
			multi_path_impl.setAttributeStreamRef(Semantics.POSITION, m_position);

			if (m_b_has_zs) {
				assert(m_zs != null);
				multi_path_impl.setAttributeStreamRef(Semantics.Z, m_zs);
			}

			if (m_b_has_ms) {
				assert(m_ms != null);
				multi_path_impl.setAttributeStreamRef(Semantics.M, m_ms);
			}

			m_path_flags = (AttributeStreamOfInt8) AttributeStreamBase.createByteStream(m_paths.size(), (byte) 0);

			multi_path_impl.setPathStreamRef(m_paths);
			multi_path_impl.setPathFlagsStreamRef(m_path_flags);
			multi_path_impl.notifyModified(MultiVertexGeometryImpl.DirtyFlags.DirtyAll);

			return polyline;
		}

		private Geometry createMultiPointFromStreams_() {
			assert(m_position != null);
			assert(m_paths == null);
			assert(m_path_flags == null);

			MultiPoint multi_point = new MultiPoint();
			MultiPointImpl multi_point_impl = (MultiPointImpl) multi_point._getImpl();
			multi_point_impl.setAttributeStreamRef(Semantics.POSITION, m_position);

			if (m_b_has_zs) {
				assert(m_zs != null);
				multi_point_impl.setAttributeStreamRef(Semantics.Z, m_zs);
			}

			if (m_b_has_ms) {
				assert(m_ms != null);
				multi_point_impl.setAttributeStreamRef(Semantics.M, m_ms);
			}
			multi_point_impl.resize(m_position.size() / 2);
			multi_point_impl.notifyModified(MultiVertexGeometryImpl.DirtyFlags.DirtyAll);

			return multi_point;
		}

		private void checkPathPointCountsForMultiPath_(boolean b_is_polygon) {
			Point2D pt1 = new Point2D(), pt2 = new Point2D();
			double z1 = 0.0, z2 = 0.0, m1 = 0.0, m2 = 0.0;
			int path_count = m_paths.size() - 1;
			int guess_adjustment = 0;

			if (b_is_polygon) {// Polygon
				guess_adjustment = path_count; // may remove up to path_count
												// number of points
			} else {// Polyline
				for (int path = 0; path < path_count; path++) {
					int path_size = m_paths.read(path + 1) - m_paths.read(path);

					if (path_size == 1) {
						guess_adjustment--; // will add a point for each path
											// containing only 1 point
					}
				}

				if (guess_adjustment == 0) {
					return; // all paths are okay
				}
			}

			AttributeStreamOfDbl adjusted_position = (AttributeStreamOfDbl) AttributeStreamBase
					.createDoubleStream(m_position.size() - guess_adjustment);
			AttributeStreamOfInt32 adjusted_paths = (AttributeStreamOfInt32) AttributeStreamBase
					.createIndexStream(m_paths.size());
			AttributeStreamOfDbl adjusted_zs = null;
			AttributeStreamOfDbl adjusted_ms = null;

			if (m_b_has_zs) {
				adjusted_zs = (AttributeStreamOfDbl) AttributeStreamBase
						.createDoubleStream(m_zs.size() - guess_adjustment);
			}

			if (m_b_has_ms) {
				adjusted_ms = (AttributeStreamOfDbl) AttributeStreamBase
						.createDoubleStream(m_ms.size() - guess_adjustment);
			}

			int adjusted_start = 0;
			adjusted_paths.write(0, 0);

			for (int path = 0; path < path_count; path++) {
				int path_start = m_paths.read(path);
				int path_end = m_paths.read(path + 1);
				int path_size = path_end - path_start;
				assert(path_size != 0); // we should not have added empty parts
										// on import

				if (path_size == 1) {
					insertIntoAdjustedStreams_(adjusted_position, adjusted_zs, adjusted_ms, adjusted_start, path_start,
							path_size);
					insertIntoAdjustedStreams_(adjusted_position, adjusted_zs, adjusted_ms, adjusted_start + 1,
							path_start, path_size);
					adjusted_start += 2;
				} else if (path_size >= 3 && b_is_polygon) {
					m_position.read(path_start * 2, pt1);
					m_position.read((path_end - 1) * 2, pt2);

					if (m_b_has_zs) {
						z1 = m_zs.readAsDbl(path_start);
						z2 = m_zs.readAsDbl(path_end - 1);
					}

					if (m_b_has_ms) {
						m1 = m_ms.readAsDbl(path_start);
						m2 = m_ms.readAsDbl(path_end - 1);
					}

					if (pt1.equals(pt2) && (NumberUtils.isNaN(z1) && NumberUtils.isNaN(z2) || z1 == z2)
							&& (NumberUtils.isNaN(m1) && NumberUtils.isNaN(m2) || m1 == m2)) {
						insertIntoAdjustedStreams_(adjusted_position, adjusted_zs, adjusted_ms, adjusted_start,
								path_start, path_size - 1);
						adjusted_start += path_size - 1;
					} else {
						insertIntoAdjustedStreams_(adjusted_position, adjusted_zs, adjusted_ms, adjusted_start,
								path_start, path_size);
						adjusted_start += path_size;
					}
				} else {
					insertIntoAdjustedStreams_(adjusted_position, adjusted_zs, adjusted_ms, adjusted_start, path_start,
							path_size);
					adjusted_start += path_size;
				}
				adjusted_paths.write(path + 1, adjusted_start);
			}

			m_position = adjusted_position;
			m_paths = adjusted_paths;
			m_zs = adjusted_zs;
			m_ms = adjusted_ms;
		}

		private void insertIntoAdjustedStreams_(AttributeStreamOfDbl adjusted_position,
				AttributeStreamOfDbl adjusted_zs, AttributeStreamOfDbl adjusted_ms, int adjusted_start, int path_start,
				int count) {
			adjusted_position.insertRange(adjusted_start * 2, m_position, path_start * 2, count * 2, true, 2,
					adjusted_start * 2);

			if (m_b_has_zs) {
				adjusted_zs.insertRange(adjusted_start, m_zs, path_start, count, true, 1, adjusted_start);
			}

			if (m_b_has_ms) {
				adjusted_ms.insertRange(adjusted_start, m_ms, path_start, count, true, 1, adjusted_start);
			}
		}

		static SpatialReference importSpatialReferenceFromCrs(JsonReader json_iterator,
				ProgressTracker progress_tracker) throws JSONException, JsonParseException, IOException {
			// According to the spec, a null crs corresponds to no spatial
			// reference
			if (json_iterator.currentToken() == JsonToken.VALUE_NULL) {
				return null;
			}

			if (json_iterator.currentToken() == JsonToken.VALUE_STRING) {// see
																			// http://wiki.geojson.org/RFC-001
																			// (this
																			// is
																			// deprecated,
																			// but
																			// there
																			// may
																			// be
																			// data
																			// with
																			// this
																			// format)

				String crs_short_form = json_iterator.currentString();
				int wkid = GeoJsonCrsTables.getWkidFromCrsShortForm(crs_short_form);

				if (wkid == -1) {
					throw new GeometryException("not implemented");
				}

				SpatialReference spatial_reference = null;

				try {
					spatial_reference = SpatialReference.create(wkid);
				} catch (Exception e) {
				}

				return spatial_reference;
			}

			if (json_iterator.currentToken() != JsonToken.START_OBJECT) {
				throw new IllegalArgumentException("invalid argument");
			}

			// This is to support all cases of crs identifiers I've seen. Some
			// may be rare or are legacy formats, but all are simple to
			// accomodate.
			boolean b_found_type = false;
			boolean b_found_properties = false;
			boolean b_found_properties_name = false;
			boolean b_found_properties_href = false;
			boolean b_found_properties_urn = false;
			boolean b_found_properties_url = false;
			boolean b_found_properties_code = false;
			boolean b_found_esriwkt = false;
			String crs_field = null, properties_field = null, type = null, crs_identifier_name = null,
					crs_identifier_urn = null, crs_identifier_href = null, crs_identifier_url = null, esriwkt = null;
			int crs_identifier_code = -1;
			JsonToken current_token;

			while (json_iterator.nextToken() != JsonToken.END_OBJECT) {
				crs_field = json_iterator.currentString();

				if (crs_field.equals("type")) {
					if (b_found_type) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_found_type = true;

					current_token = json_iterator.nextToken();

					if (current_token != JsonToken.VALUE_STRING) {
						throw new IllegalArgumentException("invalid argument");
					}

					type = json_iterator.currentString();
				} else if (crs_field.equals("properties")) {
					if (b_found_properties) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_found_properties = true;

					current_token = json_iterator.nextToken();

					if (current_token != JsonToken.START_OBJECT) {
						throw new IllegalArgumentException("invalid argument");
					}

					while (json_iterator.nextToken() != JsonToken.END_OBJECT) {
						properties_field = json_iterator.currentString();

						if (properties_field.equals("name")) {
							if (b_found_properties_name) {
								throw new IllegalArgumentException("invalid argument");
							}

							b_found_properties_name = true;
							crs_identifier_name = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("href")) {
							if (b_found_properties_href) {
								throw new IllegalArgumentException("invalid argument");
							}

							b_found_properties_href = true;
							crs_identifier_href = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("urn")) {
							if (b_found_properties_urn) {
								throw new IllegalArgumentException("invalid argument");
							}

							b_found_properties_urn = true;
							crs_identifier_urn = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("url")) {
							if (b_found_properties_url) {
								throw new IllegalArgumentException("invalid argument");
							}

							b_found_properties_url = true;
							crs_identifier_url = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("code")) {
							if (b_found_properties_code) {
								throw new IllegalArgumentException("invalid argument");
							}

							b_found_properties_code = true;

							current_token = json_iterator.nextToken();

							if (current_token != JsonToken.VALUE_NUMBER_INT) {
								throw new IllegalArgumentException("invalid argument");
							}

							crs_identifier_code = json_iterator.currentIntValue();
						} else {
							json_iterator.nextToken();
							json_iterator.skipChildren();
						}
					}
				} else if (crs_field.equals("esriwkt")) {
					if (b_found_esriwkt) {
						throw new IllegalArgumentException("invalid argument");
					}

					b_found_esriwkt = true;

					current_token = json_iterator.nextToken();

					if (current_token != JsonToken.VALUE_STRING) {
						throw new IllegalArgumentException("invalid argument");
					}

					esriwkt = json_iterator.currentString();
				} else {
					json_iterator.nextToken();
					json_iterator.skipChildren();
				}
			}

			if ((!b_found_type || !b_found_properties) && !b_found_esriwkt) {
				throw new IllegalArgumentException("invalid argument");
			}

			int wkid = -1;

			if (b_found_properties_name) {
				wkid = GeoJsonCrsTables.getWkidFromCrsName(crs_identifier_name); // see
																					// http://wiki.geojson.org/GeoJSON_draft_version_6
																					// (most
																					// common)
			} else if (b_found_properties_href) {
				wkid = GeoJsonCrsTables.getWkidFromCrsHref(crs_identifier_href); // see
																					// http://wiki.geojson.org/GeoJSON_draft_version_6
																					// (somewhat
																					// common)
			} else if (b_found_properties_urn) {
				wkid = GeoJsonCrsTables.getWkidFromCrsOgcUrn(crs_identifier_urn); // see
																					// http://wiki.geojson.org/GeoJSON_draft_version_5
																					// (rare)
			} else if (b_found_properties_url) {
				wkid = GeoJsonCrsTables.getWkidFromCrsHref(crs_identifier_url); // see
																				// http://wiki.geojson.org/GeoJSON_draft_version_5
																				// (rare)
			} else if (b_found_properties_code) {
				wkid = crs_identifier_code; // see
											// http://wiki.geojson.org/GeoJSON_draft_version_5
											// (rare)
			} else if (!b_found_esriwkt) {
				throw new GeometryException("not implemented");
			}

			if (wkid < 0 && !b_found_esriwkt && !b_found_properties_name) {
				throw new GeometryException("not implemented");
			}

			SpatialReference spatial_reference = null;

			if (wkid > 0) {
				try {
					spatial_reference = SpatialReference.create(wkid);
				} catch (Exception e) {
				}
			}

			if (spatial_reference == null) {
				try {
					if (b_found_esriwkt) {// I exported crs wkt strings like
											// this
						spatial_reference = SpatialReference.create(esriwkt);
					} else if (b_found_properties_name) {// AGOL exported crs
															// wkt strings like
															// this where the
															// crs identifier of
															// the properties
															// name is like
															// "ESRI:<wkt>"
						String potential_wkt = GeoJsonCrsTables.getWktFromCrsName(crs_identifier_name);
						spatial_reference = SpatialReference.create(potential_wkt);
					}
				} catch (Exception e) {
				}
			}

			return spatial_reference;
		}

		// see http://geojsonwg.github.io/draft-geojson/draft.html
		static SpatialReference importSpatialReferenceFromCrsUrn_(JsonReader json_iterator,
				ProgressTracker progress_tracker) throws JSONException, JsonParseException, IOException {
			// According to the spec, a null crs corresponds to no spatial
			// reference
			if (json_iterator.currentToken() == JsonToken.VALUE_NULL) {
				return null;
			}

			if (json_iterator.currentToken() != JsonToken.VALUE_STRING) {
				throw new IllegalArgumentException("invalid argument");
			}

			String crs_identifier_urn = json_iterator.currentString();

			int wkid = GeoJsonCrsTables.getWkidFromCrsName(crs_identifier_urn); // This
																				// will
																				// check
																				// for
																				// short
																				// form
																				// name,
																				// as
																				// well
																				// as
																				// long
																				// form
																				// URNs

			if (wkid == -1) {
				throw new GeometryException("not implemented");
			}

			SpatialReference spatial_reference = SpatialReference.create(wkid);

			return spatial_reference;
		}

		private static String getCrsIdentifier_(JsonReader json_iterator)
				throws JSONException, JsonParseException, IOException {
			JsonToken current_token = json_iterator.nextToken();

			if (current_token != JsonToken.VALUE_STRING) {
				throw new IllegalArgumentException("invalid argument");
			}

			return json_iterator.currentString();
		}

	}

	static JSONArray getJSONArray(JSONObject obj, String name) throws JSONException {
		if (obj.get(name) == JSONObject.NULL)
			return new JSONArray();
		else
			return obj.getJSONArray(name);
	}

	@Override
	public MapOGCStructure executeOGC(int import_flags, String geoJsonString, ProgressTracker progress_tracker)
			throws JSONException {
		MapOGCStructure mapOGCStructure = null;
		try {
			JSONObject geoJsonObject = new JSONObject(geoJsonString);
			ArrayList<OGCStructure> structureStack = new ArrayList<OGCStructure>(0);
			ArrayList<JSONObject> objectStack = new ArrayList<JSONObject>(0);
			AttributeStreamOfInt32 indices = new AttributeStreamOfInt32(0);
			AttributeStreamOfInt32 numGeometries = new AttributeStreamOfInt32(0);
			OGCStructure root = new OGCStructure();
			root.m_structures = new ArrayList<OGCStructure>(0);
			structureStack.add(root); // add dummy root
			objectStack.add(geoJsonObject);
			indices.add(0);
			numGeometries.add(1);
			while (!objectStack.isEmpty()) {
				if (indices.getLast() == numGeometries.getLast()) {
					structureStack.remove(structureStack.size() - 1);
					indices.removeLast();
					numGeometries.removeLast();
					continue;
				}
				OGCStructure lastStructure = structureStack.get(structureStack.size() - 1);
				JSONObject lastObject = objectStack.get(objectStack.size() - 1);
				objectStack.remove(objectStack.size() - 1);
				indices.write(indices.size() - 1, indices.getLast() + 1);
				String typeString = lastObject.getString("type");
				if (typeString.equalsIgnoreCase("GeometryCollection")) {
					OGCStructure next = new OGCStructure();
					next.m_type = 7;
					next.m_structures = new ArrayList<OGCStructure>(0);
					lastStructure.m_structures.add(next);
					structureStack.add(next);
					JSONArray geometries = getJSONArray(lastObject, "geometries");
					indices.add(0);
					numGeometries.add(geometries.length());
					for (int i = geometries.length() - 1; i >= 0; i--)
						objectStack.add(geometries.getJSONObject(i));
				} else {
					int ogcType;
					if (typeString.equalsIgnoreCase("Point"))
						ogcType = 1;
					else if (typeString.equalsIgnoreCase("LineString"))
						ogcType = 2;
					else if (typeString.equalsIgnoreCase("Polygon"))
						ogcType = 3;
					else if (typeString.equalsIgnoreCase("MultiPoint"))
						ogcType = 4;
					else if (typeString.equalsIgnoreCase("MultiLineString"))
						ogcType = 5;
					else if (typeString.equalsIgnoreCase("MultiPolygon"))
						ogcType = 6;
					else
						throw new UnsupportedOperationException();

					MapGeometry map_geometry = execute(import_flags | GeoJsonImportFlags.geoJsonImportSkipCRS,
							Geometry.Type.Unknown, lastObject, null);
					OGCStructure leaf = new OGCStructure();
					leaf.m_type = ogcType;
					leaf.m_geometry = map_geometry.getGeometry();
					lastStructure.m_structures.add(leaf);
				}
			}
			mapOGCStructure = new MapOGCStructure();
			mapOGCStructure.m_ogcStructure = root;

			if ((import_flags & GeoJsonImportFlags.geoJsonImportSkipCRS) == 0)
				mapOGCStructure.m_spatialReference = importSpatialReferenceFromGeoJson_(import_flags, geoJsonObject);
		} catch (JSONException jsonException) {
			throw jsonException;
		} catch (JsonParseException jsonParseException) {
			throw new JSONException(jsonParseException.getMessage());
		} catch (IOException ioException) {
			throw new JSONException(ioException.getMessage());
		}

		return mapOGCStructure;
	}

	private static SpatialReference importSpatialReferenceFromGeoJson_(int importFlags, JSONObject crsJSONObject)
			throws JSONException, JsonParseException, IOException {

		SpatialReference spatial_reference = null;
		boolean b_crs_found = false, b_crsURN_found = false;

		Object opt = crsJSONObject.opt("crs");

		if (opt != null) {
			b_crs_found = true;
			JSONObject crs_object = new JSONObject();
			crs_object.put("crs", opt);
			JsonValueReader json_iterator = new JsonValueReader(crs_object);
			json_iterator.nextToken();
			json_iterator.nextToken();
			return OperatorImportFromGeoJsonHelper.importSpatialReferenceFromCrs(json_iterator, null);
		}

		opt = crsJSONObject.opt("crsURN");

		if (opt != null) {
			b_crsURN_found = true;
			JSONObject crs_object = new JSONObject();
			crs_object.put("crsURN", opt);
			JsonValueReader json_iterator = new JsonValueReader(crs_object);
			json_iterator.nextToken();
			json_iterator.nextToken();
			return OperatorImportFromGeoJsonHelper.importSpatialReferenceFromCrs(json_iterator, null);
		}

		if ((importFlags & GeoJsonImportFlags.geoJsonImportNoWGS84Default) == 0) {
			spatial_reference = SpatialReference.create(4326);
		}

		return spatial_reference;
	}

	/*
	private static Geometry importGeometryFromGeoJson_(int importFlags, Geometry.Type type,
			JSONObject geometryJSONObject) throws JSONException {
		String typeString = geometryJSONObject.getString("type");
		JSONArray coordinateArray = getJSONArray(geometryJSONObject, "coordinates");
		if (typeString.equalsIgnoreCase("MultiPolygon")) {
			if (type != Geometry.Type.Polygon && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return polygonTaggedText_(true, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("MultiLineString")) {
			if (type != Geometry.Type.Polyline && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return lineStringTaggedText_(true, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("MultiPoint")) {
			if (type != Geometry.Type.MultiPoint && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return multiPointTaggedText_(importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("Polygon")) {
			if (type != Geometry.Type.Polygon && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return polygonTaggedText_(false, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("LineString")) {
			if (type != Geometry.Type.Polyline && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return lineStringTaggedText_(false, importFlags, coordinateArray);
		} else if (typeString.equalsIgnoreCase("Point")) {
			if (type != Geometry.Type.Point && type != Geometry.Type.Unknown)
				throw new IllegalArgumentException("invalid shapetype");
			return pointTaggedText_(importFlags, coordinateArray);
		} else {
			return null;
		}
	}

	private static Geometry polygonTaggedText_(boolean bMultiPolygon, int importFlags, JSONArray coordinateArray)
			throws JSONException {
		MultiPath multiPath;
		MultiPathImpl multiPathImpl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;
		AttributeStreamOfInt32 paths;
		AttributeStreamOfInt8 path_flags;
		position = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(0);
		paths = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(1, 0);
		path_flags = (AttributeStreamOfInt8) AttributeStreamBase.createByteStream(1, (byte) 0);
		multiPath = new Polygon();
		multiPathImpl = (MultiPathImpl) multiPath._getImpl();
		int pointCount;
		if (bMultiPolygon) {
			pointCount = multiPolygonText_(zs, ms, position, paths, path_flags, coordinateArray);
		} else {
			pointCount = polygonText_(zs, ms, position, paths, path_flags, 0, coordinateArray);
		}
		if (pointCount != 0) {
			assert(2 * pointCount == position.size());
			multiPathImpl.setAttributeStreamRef(VertexDescription.Semantics.POSITION, position);
			multiPathImpl.setPathStreamRef(paths);
			multiPathImpl.setPathFlagsStreamRef(path_flags);
			if (zs != null) {
				multiPathImpl.setAttributeStreamRef(VertexDescription.Semantics.Z, zs);
			}
			if (ms != null) {
				multiPathImpl.setAttributeStreamRef(VertexDescription.Semantics.M, ms);
			}
			multiPathImpl.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);
			AttributeStreamOfInt8 path_flags_clone = new AttributeStreamOfInt8(path_flags);
			for (int i = 0; i < path_flags_clone.size() - 1; i++) {
				if (((int) path_flags_clone.read(i) & (int) PathFlags.enumOGCStartPolygon) != 0) {// Should
																									// be
																									// clockwise
					if (!InternalUtils.isClockwiseRing(multiPathImpl, i))
						multiPathImpl.reversePath(i); // make clockwise
				} else {// Should be counter-clockwise
					if (InternalUtils.isClockwiseRing(multiPathImpl, i))
						multiPathImpl.reversePath(i); // make counter-clockwise
				}
			}
			multiPathImpl.setPathFlagsStreamRef(path_flags_clone);
		}
		if ((importFlags & (int) GeoJsonImportFlags.geoJsonImportNonTrusted) == 0) {
			multiPathImpl.setIsSimple(MultiPathImpl.GeometryXSimple.Weak, 0.0, false);
		}
		multiPathImpl.setDirtyOGCFlags(false);
		return multiPath;
	}

	private static Geometry lineStringTaggedText_(boolean bMultiLineString, int importFlags, JSONArray coordinateArray)
			throws JSONException {
		MultiPath multiPath;
		MultiPathImpl multiPathImpl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;
		AttributeStreamOfInt32 paths;
		AttributeStreamOfInt8 path_flags;
		position = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(0);
		paths = (AttributeStreamOfInt32) AttributeStreamBase.createIndexStream(1, 0);
		path_flags = (AttributeStreamOfInt8) AttributeStreamBase.createByteStream(1, (byte) 0);
		multiPath = new Polyline();
		multiPathImpl = (MultiPathImpl) multiPath._getImpl();
		int pointCount;
		if (bMultiLineString) {
			pointCount = multiLineStringText_(zs, ms, position, paths, path_flags, coordinateArray);
		} else {
			pointCount = lineStringText_(false, zs, ms, position, paths, path_flags, coordinateArray);
		}
		if (pointCount != 0) {
			assert(2 * pointCount == position.size());
			multiPathImpl.setAttributeStreamRef(VertexDescription.Semantics.POSITION, position);
			multiPathImpl.setPathStreamRef(paths);
			multiPathImpl.setPathFlagsStreamRef(path_flags);
			if (zs != null) {
				multiPathImpl.setAttributeStreamRef(VertexDescription.Semantics.Z, zs);
			}
			if (ms != null) {
				multiPathImpl.setAttributeStreamRef(VertexDescription.Semantics.M, ms);
			}
			multiPathImpl.notifyModified(MultiPathImpl.DirtyFlags.DirtyAll);
		}
		return multiPath;
	}

	private static Geometry multiPointTaggedText_(int importFlags, JSONArray coordinateArray) throws JSONException {
		MultiPoint multiPoint;
		MultiPointImpl multiPointImpl;
		AttributeStreamOfDbl zs = null;
		AttributeStreamOfDbl ms = null;
		AttributeStreamOfDbl position;
		position = (AttributeStreamOfDbl) AttributeStreamBase.createDoubleStream(0);
		multiPoint = new MultiPoint();
		multiPointImpl = (MultiPointImpl) multiPoint._getImpl();
		int pointCount = multiPointText_(zs, ms, position, coordinateArray);
		if (pointCount != 0) {
			assert(2 * pointCount == position.size());
			multiPointImpl.resize(pointCount);
			multiPointImpl.setAttributeStreamRef(VertexDescription.Semantics.POSITION, position);
			multiPointImpl.notifyModified(MultiPointImpl.DirtyFlags.DirtyAll);
		}
		return multiPoint;
	}

	private static Geometry pointTaggedText_(int importFlags, JSONArray coordinateArray) throws JSONException {
		Point point = new Point();
		int length = coordinateArray.length();
		if (length == 0) {
			point.setEmpty();
			return point;
		}
		point.setXY(getDouble_(coordinateArray, 0), getDouble_(coordinateArray, 1));
		return point;
	}

	private static int multiPolygonText_(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			JSONArray coordinateArray) throws JSONException {
		// At start of MultiPolygonText
		int totalPointCount = 0;
		int length = coordinateArray.length();
		if (length == 0)
			return totalPointCount;
		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {// Entry should be a JSONArray representing a
									// polygon, but it is not a JSONArray.
				throw new IllegalArgumentException("");
			}

			// At start of PolygonText
			totalPointCount = polygonText_(zs, ms, position, paths, path_flags, totalPointCount, subArray);
		}
		return totalPointCount;
	}

	private static int multiLineStringText_(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			JSONArray coordinateArray) throws JSONException {
		// At start of MultiLineStringText
		int totalPointCount = 0;
		int length = coordinateArray.length();
		if (length == 0)
			return totalPointCount;
		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {// Entry should be a JSONArray representing a
									// line string, but it is not a JSONArray.
				throw new IllegalArgumentException("");
			}

			// At start of LineStringText
			totalPointCount += lineStringText_(false, zs, ms, position, paths, path_flags, subArray);
		}
		return totalPointCount;
	}
	
	private static int multiPointText_(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			JSONArray coordinateArray) throws JSONException {
		// At start of MultiPointText
		int pointCount = 0;
		for (int current = 0; current < coordinateArray.length(); current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {// Entry should be a JSONArray representing a
									// point, but it is not a JSONArray.
				throw new IllegalArgumentException("");
			}
			pointCount += pointText_(zs, ms, position, subArray);
		}
		return pointCount;
	}

	private static int polygonText_(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags, int totalPointCount,
			JSONArray coordinateArray) throws JSONException {
		// At start of PolygonText
		int length = coordinateArray.length();
		if (length == 0) {
			return totalPointCount;
		}
		boolean bFirstLineString = true;
		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {// Entry should be a JSONArray representing a
									// line string, but it is not a JSONArray.
				throw new IllegalArgumentException("");
			}
			// At start of LineStringText
			int pointCount = lineStringText_(true, zs, ms, position, paths, path_flags, subArray);
			if (pointCount != 0) {
				if (bFirstLineString) {
					bFirstLineString = false;
					path_flags.setBits(path_flags.size() - 2, (byte) PathFlags.enumOGCStartPolygon);
				}
				path_flags.setBits(path_flags.size() - 2, (byte) PathFlags.enumClosed);
				totalPointCount += pointCount;
			}
		}
		return totalPointCount;
	}
	
	private static int lineStringText_(boolean bRing, AttributeStreamOfDbl zs, AttributeStreamOfDbl ms,
			AttributeStreamOfDbl position, AttributeStreamOfInt32 paths, AttributeStreamOfInt8 path_flags,
			JSONArray coordinateArray) throws JSONException {
		// At start of LineStringText
		int pointCount = 0;
		int length = coordinateArray.length();
		if (length == 0)
			return pointCount;
		boolean bStartPath = true;
		double startX = NumberUtils.TheNaN;
		double startY = NumberUtils.TheNaN;
		double startZ = NumberUtils.TheNaN;
		double startM = NumberUtils.TheNaN;
		for (int current = 0; current < length; current++) {
			JSONArray subArray = coordinateArray.optJSONArray(current);
			if (subArray == null) {// Entry should be a JSONArray representing a
									// single point, but it is not a JSONArray.
				throw new IllegalArgumentException("");
			}
			// At start of x
			double x = getDouble_(subArray, 0);
			double y = getDouble_(subArray, 1);
			double z = NumberUtils.TheNaN;
			double m = NumberUtils.TheNaN;
			boolean bAddPoint = true;
			if (bRing && pointCount >= 2 && current == length - 1) {// If the
																	// last
																	// point in
																	// the ring
																	// is not
																	// equal to
																	// the start
																	// point,
																	// then
																	// let's add
																	// it.
				if ((startX == x || (NumberUtils.isNaN(startX) && NumberUtils.isNaN(x)))
						&& (startY == y || (NumberUtils.isNaN(startY) && NumberUtils.isNaN(y)))) {
					bAddPoint = false;
				}
			}
			if (bAddPoint) {
				if (bStartPath) {
					bStartPath = false;
					startX = x;
					startY = y;
					startZ = z;
					startM = m;
				}
				pointCount++;
				addToStreams_(zs, ms, position, x, y, z, m);
			}
		}
		if (pointCount == 1) {
			pointCount++;
			addToStreams_(zs, ms, position, startX, startY, startZ, startM);
		}
		paths.add(position.size() / 2);
		path_flags.add((byte) 0);
		return pointCount;
	}

	private static int pointText_(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			JSONArray coordinateArray) throws JSONException {
		// At start of PointText
		int length = coordinateArray.length();
		if (length == 0)
			return 0;
		// At start of x
		double x = getDouble_(coordinateArray, 0);
		double y = getDouble_(coordinateArray, 1);
		double z = NumberUtils.TheNaN;
		double m = NumberUtils.TheNaN;
		addToStreams_(zs, ms, position, x, y, z, m);
		return 1;
	}

	private static void addToStreams_(AttributeStreamOfDbl zs, AttributeStreamOfDbl ms, AttributeStreamOfDbl position,
			double x, double y, double z, double m) {
		position.add(x);
		position.add(y);
		if (zs != null)
			zs.add(z);
		if (ms != null)
			ms.add(m);
	}

	private static double getDouble_(JSONArray coordinateArray, int index) throws JSONException {
		if (index < 0 || index >= coordinateArray.length()) {
			throw new IllegalArgumentException("");
		}
		if (coordinateArray.isNull(index)) {
			return NumberUtils.TheNaN;
		}
		if (coordinateArray.optDouble(index, NumberUtils.TheNaN) != NumberUtils.TheNaN) {
			return coordinateArray.getDouble(index);
		}
		throw new IllegalArgumentException("");
	}*/
}
