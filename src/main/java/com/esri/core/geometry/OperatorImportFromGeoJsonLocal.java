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
import java.util.ArrayList;

class OperatorImportFromGeoJsonLocal extends OperatorImportFromGeoJson {
	static enum GeoJsonType {
		Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection;
		static GeoJsonType fromGeoJsonValue(int v) {
			return GeoJsonType.values()[v - 1];
		}
		
		public int geogsjonvalue() {
			return ordinal() + 1;
		}
	};

	static interface GeoJsonValues {
		public final static int Point = GeoJsonType.Point.geogsjonvalue();
		public final static int LineString = GeoJsonType.LineString.geogsjonvalue();
		public final static int Polygon = GeoJsonType.Polygon.geogsjonvalue();
		public final static int MultiPoint = GeoJsonType.MultiPoint.geogsjonvalue();
		public final static int MultiLineString = GeoJsonType.MultiLineString.geogsjonvalue();
		public final static int MultiPolygon = GeoJsonType.MultiPolygon.geogsjonvalue();
		public final static int GeometryCollection = GeoJsonType.GeometryCollection.geogsjonvalue();
	};

	@Override
	public MapGeometry execute(int importFlags, Geometry.Type type,
			String geoJsonString, ProgressTracker progressTracker)
			throws JsonGeometryException {
		MapGeometry map_geometry = OperatorImportFromGeoJsonHelper
				.importFromGeoJson(importFlags, type, JsonParserReader.createFromString(geoJsonString), progressTracker, false);
		return map_geometry;
	}

	@Override
	public MapGeometry execute(int importFlags, Geometry.Type type,
			JsonReader jsonReader, ProgressTracker progressTracker)
			throws JsonGeometryException {
		if (jsonReader == null)
			return null;

		return OperatorImportFromGeoJsonHelper.importFromGeoJson(importFlags,
				type, jsonReader, progressTracker, false);
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

		int m_ogcType;

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
			m_ogcType = 0;
		}

		static MapGeometry importFromGeoJson(int importFlags,
				Geometry.Type type, JsonReader json_iterator,
				ProgressTracker progress_tracker, boolean skip_coordinates)
				throws JsonGeometryException {
			OperatorImportFromGeoJsonHelper geo_json_helper = new OperatorImportFromGeoJsonHelper();
			MapOGCStructure ms = geo_json_helper.importFromGeoJsonImpl(
					importFlags, type, json_iterator, progress_tracker,
					skip_coordinates, 0);
			
			if (geo_json_helper.m_ogcType == GeoJsonValues.GeometryCollection && !skip_coordinates)
				throw new JsonGeometryException("parsing error");

			return new MapGeometry(ms.m_ogcStructure.m_geometry,
					ms.m_spatialReference);
		}

		static MapOGCStructure importFromGeoJson(int importFlags,
				Geometry.Type type, JsonReader json_iterator,
				ProgressTracker progress_tracker, boolean skip_coordinates,
				int recursion) throws JsonGeometryException {
			OperatorImportFromGeoJsonHelper geo_json_helper = new OperatorImportFromGeoJsonHelper();
			MapOGCStructure ms = geo_json_helper.importFromGeoJsonImpl(
					importFlags, type, json_iterator, progress_tracker,
					skip_coordinates, recursion);
			
			if (geo_json_helper.m_ogcType == GeoJsonValues.GeometryCollection && !skip_coordinates)
				throw new JsonGeometryException("parsing error");

			return ms;
		}		
		MapOGCStructure importFromGeoJsonImpl(int importFlags,
				Geometry.Type type, JsonReader json_iterator,
				ProgressTracker progress_tracker, boolean skip_coordinates,
				int recursion) throws JsonGeometryException {
			OperatorImportFromGeoJsonHelper geo_json_helper = this;
			boolean b_type_found = false;
			boolean b_coordinates_found = false;
			boolean b_crs_found = false;
			boolean b_crsURN_found = false;
			boolean b_geometry_collection = false;
			boolean b_geometries_found = false;
			GeoJsonType geo_json_type = null;

			Geometry geometry = null;
			SpatialReference spatial_reference = null;

			JsonReader.Token current_token;
			String field_name = null;
			MapOGCStructure ms = new MapOGCStructure();

			while ((current_token = json_iterator.nextToken()) != JsonReader.Token.END_OBJECT) {
				field_name = json_iterator.currentString();

				if (field_name.equals("type")) {
					if (b_type_found) {
						throw new JsonGeometryException("parsing error");
					}

					b_type_found = true;
					current_token = json_iterator.nextToken();

					if (current_token != JsonReader.Token.VALUE_STRING) {
						throw new JsonGeometryException("parsing error");
					}

					String s = json_iterator.currentString();
					try {
						geo_json_type = GeoJsonType.valueOf(s);
					} catch (Exception ex) {
						throw new JsonGeometryException(s);
					}
					
					if (geo_json_type == GeoJsonType.GeometryCollection) {
						if (type != Geometry.Type.Unknown)
							throw new JsonGeometryException("parsing error");
						
						b_geometry_collection = true;
					}
				} else if (field_name.equals("geometries"))	{
					b_geometries_found = true;
					if (type != Geometry.Type.Unknown)
						throw new JsonGeometryException("parsing error");
					
					if (recursion > 10) {
						throw new JsonGeometryException("deep geojson");
					}
					
					if (skip_coordinates) {
						json_iterator.skipChildren();
					} else {
						current_token = json_iterator.nextToken();
	
						ms.m_ogcStructure = new OGCStructure();
						ms.m_ogcStructure.m_type = GeoJsonValues.GeometryCollection;
						ms.m_ogcStructure.m_structures = new ArrayList<OGCStructure>(
								0);
	
						if (current_token == JsonReader.Token.START_ARRAY) {
							current_token = json_iterator.nextToken();
							while (current_token != JsonReader.Token.END_ARRAY) {
								MapOGCStructure child = importFromGeoJson(
										importFlags
												| GeoJsonImportFlags.geoJsonImportSkipCRS,
										type, json_iterator,
										progress_tracker, false,
										recursion + 1);
								ms.m_ogcStructure.m_structures
										.add(child.m_ogcStructure);
								
								current_token = json_iterator.nextToken();
							}
						}
						else if (current_token != JsonReader.Token.VALUE_NULL) {
							throw new JsonGeometryException("parsing error");
						}
					}
				} else if (field_name.equals("coordinates")) {
					
					if (b_coordinates_found) {
						throw new JsonGeometryException("parsing error");
					}

					b_coordinates_found = true;
					current_token = json_iterator.nextToken();

					if (skip_coordinates) {
						json_iterator.skipChildren();
					} else {// According to the spec, the value of the
							// coordinates must be an array. However, I do an
							// extra check for null too.
						if (current_token != JsonReader.Token.VALUE_NULL) {
							if (current_token != JsonReader.Token.START_ARRAY) {
								throw new JsonGeometryException("parsing error");
							}

							geo_json_helper.import_coordinates_(json_iterator,
									progress_tracker);
						}
					}
				} else if (field_name.equals("crs")) {
					if (b_crs_found || b_crsURN_found) {
						throw new JsonGeometryException("parsing error");
					}

					b_crs_found = true;
					current_token = json_iterator.nextToken();

					if ((importFlags & GeoJsonImportFlags.geoJsonImportSkipCRS) == 0)
						spatial_reference = importSpatialReferenceFromCrs(
								json_iterator, progress_tracker);
					else
						json_iterator.skipChildren();
				} else if (field_name.equals("crsURN")) {
					if (b_crs_found || b_crsURN_found) {
						throw new JsonGeometryException("parsing error");
					}

					b_crsURN_found = true;
					current_token = json_iterator.nextToken();

					spatial_reference = importSpatialReferenceFromCrsUrn_(
							json_iterator, progress_tracker);
				} else {
					json_iterator.nextToken();
					json_iterator.skipChildren();
				}
			}

			// According to the spec, a GeoJSON object must have both a type and
			// a coordinates array
			if (!b_type_found || (!b_geometry_collection && !b_coordinates_found && !skip_coordinates)) {
				throw new JsonGeometryException("parsing error");
			}
			
			if ((!b_geometry_collection && b_geometries_found) || (b_geometry_collection && !b_geometries_found)) {
				throw new JsonGeometryException("parsing error");//found "geometries" but did not see "GeometryCollection"
			}
				

			if (!skip_coordinates && !b_geometry_collection) {
				geometry = geo_json_helper.createGeometry_(geo_json_type,
						type.value());

				ms.m_ogcStructure = new OGCStructure();
				ms.m_ogcStructure.m_type = m_ogcType;
				ms.m_ogcStructure.m_geometry = geometry;
			}

			if (!b_crs_found
					&& !b_crsURN_found
					&& ((importFlags & GeoJsonImportFlags.geoJsonImportSkipCRS) == 0)
					&& ((importFlags & GeoJsonImportFlags.geoJsonImportNoWGS84Default) == 0)) {
				spatial_reference = SpatialReference.create(4326); // the spec
																	// gives a
																	// default
																	// of 4326
																	// if no crs
																	// is given
			}

			ms.m_spatialReference = spatial_reference;
			return ms;
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
		private void import_coordinates_(JsonReader json_iterator,
				ProgressTracker progress_tracker) throws JsonGeometryException {
			assert (json_iterator.currentToken() == JsonReader.Token.START_ARRAY);

			int coordinates_level_lower = 1;
			int coordinates_level_upper = 4;

			json_iterator.nextToken();

			while (json_iterator.currentToken() != JsonReader.Token.END_ARRAY) {
				if (isDouble_(json_iterator)) {
					if (coordinates_level_upper > 1) {
						coordinates_level_upper = 1;
					}
				} else if (json_iterator.currentToken() == JsonReader.Token.START_ARRAY) {
					if (coordinates_level_lower < 2) {
						coordinates_level_lower = 2;
					}
				} else {
					throw new JsonGeometryException("parsing error");
				}

				if (coordinates_level_lower > coordinates_level_upper) {
					throw new IllegalArgumentException("invalid argument");
				}

				if (coordinates_level_lower == coordinates_level_upper
						&& coordinates_level_lower == 1) {// special
															// code
															// for
															// Points
					readCoordinateAsPoint_(json_iterator);
				} else {
					boolean b_add_path_level_3 = true;
					boolean b_polygon_start_level_4 = true;

					assert (json_iterator.currentToken() == JsonReader.Token.START_ARRAY);
					json_iterator.nextToken();

					while (json_iterator.currentToken() != JsonReader.Token.END_ARRAY) {
						if (isDouble_(json_iterator)) {
							if (coordinates_level_upper > 2) {
								coordinates_level_upper = 2;
							}
						} else if (json_iterator.currentToken() == JsonReader.Token.START_ARRAY) {
							if (coordinates_level_lower < 3) {
								coordinates_level_lower = 3;
							}
						} else {
							throw new JsonGeometryException("parsing error");
						}

						if (coordinates_level_lower > coordinates_level_upper) {
							throw new JsonGeometryException("parsing error");
						}

						if (coordinates_level_lower == coordinates_level_upper
								&& coordinates_level_lower == 2) {// LineString
																	// or
																	// MultiPoint
							addCoordinate_(json_iterator);
						} else {
							boolean b_add_path_level_4 = true;

							assert (json_iterator.currentToken() == JsonReader.Token.START_ARRAY);
							json_iterator.nextToken();

							while (json_iterator.currentToken() != JsonReader.Token.END_ARRAY) {
								if (isDouble_(json_iterator)) {
									if (coordinates_level_upper > 3) {
										coordinates_level_upper = 3;
									}
								} else if (json_iterator.currentToken() == JsonReader.Token.START_ARRAY) {
									if (coordinates_level_lower < 4) {
										coordinates_level_lower = 4;
									}
								} else {
									throw new JsonGeometryException("parsing error");
								}

								if (coordinates_level_lower > coordinates_level_upper) {
									throw new JsonGeometryException("parsing error");
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
									assert (json_iterator.currentToken() == JsonReader.Token.START_ARRAY);
									json_iterator.nextToken();

									if (json_iterator.currentToken() != JsonReader.Token.END_ARRAY) {
										if (!isDouble_(json_iterator)) {
											throw new JsonGeometryException("parsing error");
										}

										assert (coordinates_level_lower == coordinates_level_upper && coordinates_level_lower == 4);
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
				throws JsonGeometryException {
			assert (isDouble_(json_iterator));

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

			if (json_iterator.currentToken() != JsonReader.Token.END_ARRAY) {
				throw new JsonGeometryException("parsing error");
			}
		}

		private void addCoordinate_(JsonReader json_iterator)
				throws JsonGeometryException {
			assert (isDouble_(json_iterator));

			if (m_position == null) {
				m_position = (AttributeStreamOfDbl) AttributeStreamBase
						.createDoubleStream(0);
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
					m_zs = (AttributeStreamOfDbl) AttributeStreamBase
							.createDoubleStream(0);
				} else {
					if (!m_b_has_zs) {
						m_zs = (AttributeStreamOfDbl) AttributeStreamBase
								.createDoubleStream(size >> 1,
										VertexDescription
												.getDefaultValue(Semantics.Z));
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
					m_ms = (AttributeStreamOfDbl) AttributeStreamBase
							.createDoubleStream(0);
				} else {
					if (!m_b_has_ms) {
						m_ms = (AttributeStreamOfDbl) AttributeStreamBase
								.createDoubleStream(size >> 1,
										VertexDescription
												.getDefaultValue(Semantics.M));
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

			if (json_iterator.currentToken() != JsonReader.Token.END_ARRAY) {
				throw new JsonGeometryException("parsing error");
			}
		}

		private void addPath_() {
			if (m_paths == null) {
				m_paths = (AttributeStreamOfInt32) AttributeStreamBase
						.createIndexStream(0);
			}

			if (m_position == null) {
				m_paths.add(0);
			} else {
				m_paths.add(m_position.size() / 2);
			}
		}

		private void addPathFlag_(boolean b_polygon_start) {
			if (m_path_flags == null) {
				m_path_flags = (AttributeStreamOfInt8) AttributeStreamBase
						.createByteStream(0);
			}

			if (b_polygon_start) {
				m_path_flags
						.add((byte) (PathFlags.enumClosed | PathFlags.enumOGCStartPolygon));
			} else {
				m_path_flags.add((byte) PathFlags.enumClosed);
			}
		}

		private double readDouble_(JsonReader json_iterator)
				throws JsonGeometryException {
			JsonReader.Token current_token = json_iterator.currentToken();
			if (current_token == JsonReader.Token.VALUE_NULL
					|| (current_token == JsonReader.Token.VALUE_STRING && json_iterator
							.currentString().equals("NaN"))) {
				return NumberUtils.NaN();
			} else {
				return json_iterator.currentDoubleValue();
			}
		}

		private boolean isDouble_(JsonReader json_iterator)
				throws JsonGeometryException {
			JsonReader.Token current_token = json_iterator.currentToken();

			if (current_token == JsonReader.Token.VALUE_NUMBER_FLOAT) {
				return true;
			}

			if (current_token == JsonReader.Token.VALUE_NUMBER_INT) {
				return true;
			}

			if (current_token == JsonReader.Token.VALUE_NULL
					|| (current_token == JsonReader.Token.VALUE_STRING && json_iterator
							.currentString().equals("NaN"))) {
				return true;
			}

			return false;
		}

		//does not accept GeometryCollection
		private Geometry createGeometry_(GeoJsonType geo_json_type, int type)
				throws JsonGeometryException {
			Geometry geometry;

			if (type != Geometry.GeometryType.Unknown) {
				switch (type) {
				case Geometry.GeometryType.Polygon:
					if (geo_json_type != GeoJsonType.MultiPolygon
							&& geo_json_type != GeoJsonType.Polygon) {
						throw new GeometryException("invalid shape type");
					}
					break;
				case Geometry.GeometryType.Polyline:
					if (geo_json_type != GeoJsonType.MultiLineString
							&& geo_json_type != GeoJsonType.LineString) {
						throw new GeometryException("invalid shape type");
					}
					break;
				case Geometry.GeometryType.MultiPoint:
					if (geo_json_type != GeoJsonType.MultiPoint) {
						throw new GeometryException("invalid shape type");
					}
					break;
				case Geometry.GeometryType.Point:
					if (geo_json_type != GeoJsonType.Point) {
						throw new GeometryException("invalid shape type");
					}
					break;
				default:
					throw new GeometryException("invalid shape type");
				}
			}
			
			m_ogcType = geo_json_type.geogsjonvalue();
			if (geo_json_type == GeoJsonType.GeometryCollection)
				throw new IllegalArgumentException("invalid argument");
			
			if (m_position == null && m_point == null) {
				switch (geo_json_type)
				{
				case Point: {
					if (m_num_embeddings > 1) {
						throw new JsonGeometryException("parsing error");
					}

					geometry = new Point();
					break;
				}
				case MultiPoint: {
					if (m_num_embeddings > 2) {
						throw new JsonGeometryException("parsing error");
					}

					geometry = new MultiPoint();
					break;
				}
				case LineString: {
					if (m_num_embeddings > 2) {
						throw new JsonGeometryException("parsing error");
					}

					geometry = new Polyline();
					break;
				}
				case MultiLineString: {
					if (m_num_embeddings > 3) {
						throw new JsonGeometryException("parsing error");
					}

					geometry = new Polyline();
					break;
				}
				case Polygon: {
					if (m_num_embeddings > 3) {
						throw new JsonGeometryException("parsing error");
					}

					geometry = new Polygon();
					break;
				}
				case MultiPolygon: {
					assert (m_num_embeddings <= 4);
					geometry = new Polygon();
					break;
				}
				default:
					throw new JsonGeometryException("parsing error");
				}
			} else if (m_num_embeddings == 1) {
				if (geo_json_type != GeoJsonType.Point) {
					throw new JsonGeometryException("parsing error");
				}

				assert (m_point != null);
				geometry = m_point;
			} else if (m_num_embeddings == 2) {
				if (geo_json_type == GeoJsonType.MultiPoint) {
					geometry = createMultiPointFromStreams_();
				} else if (geo_json_type == GeoJsonType.LineString) {
					geometry = createPolylineFromStreams_();
				} else {
					throw new JsonGeometryException("parsing error");
				}
			} else if (m_num_embeddings == 3) {
				if (geo_json_type == GeoJsonType.Polygon) {
					geometry = createPolygonFromStreams_();
				} else if (geo_json_type == GeoJsonType.MultiLineString) {
					geometry = createPolylineFromStreams_();
				} else {
					throw new JsonGeometryException("parsing error");
				}
			} else {
				if (geo_json_type != GeoJsonType.MultiPolygon) {
					throw new JsonGeometryException("parsing error");
				}

				geometry = createPolygonFromStreams_();
			}

			return geometry;
		}

		private Geometry createPolygonFromStreams_() {
			assert (m_position != null);
			assert (m_paths != null);
			assert ((m_num_embeddings == 3 && m_path_flags == null) || (m_num_embeddings == 4 && m_path_flags != null));

			Polygon polygon = new Polygon();
			MultiPathImpl multi_path_impl = (MultiPathImpl) polygon._getImpl();

			checkPathPointCountsForMultiPath_(true);
			multi_path_impl.setAttributeStreamRef(Semantics.POSITION,
					m_position);

			if (m_b_has_zs) {
				assert (m_zs != null);
				multi_path_impl.setAttributeStreamRef(Semantics.Z, m_zs);
			}

			if (m_b_has_ms) {
				assert (m_ms != null);
				multi_path_impl.setAttributeStreamRef(Semantics.M, m_ms);
			}

			if (m_path_flags == null) {
				m_path_flags = (AttributeStreamOfInt8) AttributeStreamBase
						.createByteStream(m_paths.size(), (byte) 0);
				m_path_flags
						.setBits(
								0,
								(byte) (PathFlags.enumClosed | PathFlags.enumOGCStartPolygon));

				for (int i = 1; i < m_path_flags.size() - 1; i++) {
					m_path_flags.setBits(i, (byte) PathFlags.enumClosed);
				}
			}

			multi_path_impl.setPathStreamRef(m_paths);
			multi_path_impl.setPathFlagsStreamRef(m_path_flags);
			multi_path_impl
					.notifyModified(MultiVertexGeometryImpl.DirtyFlags.DirtyAll);

			AttributeStreamOfInt8 path_flags_clone = new AttributeStreamOfInt8(
					m_path_flags);

			for (int i = 0; i < path_flags_clone.size() - 1; i++) {
				assert ((path_flags_clone.read(i) & PathFlags.enumClosed) != 0);
				assert ((m_path_flags.read(i) & PathFlags.enumClosed) != 0);

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
			multi_path_impl.clearDirtyOGCFlags();

			return polygon;
		}

		private Geometry createPolylineFromStreams_() {
			assert (m_position != null);
			assert ((m_num_embeddings == 2 && m_paths == null) || (m_num_embeddings == 3 && m_paths != null));
			assert (m_path_flags == null);

			Polyline polyline = new Polyline();
			MultiPathImpl multi_path_impl = (MultiPathImpl) polyline._getImpl();

			if (m_paths == null) {
				m_paths = (AttributeStreamOfInt32) AttributeStreamBase
						.createIndexStream(0);
				m_paths.add(0);
				m_paths.add(m_position.size() / 2);
			}

			checkPathPointCountsForMultiPath_(false);
			multi_path_impl.setAttributeStreamRef(Semantics.POSITION,
					m_position);

			if (m_b_has_zs) {
				assert (m_zs != null);
				multi_path_impl.setAttributeStreamRef(Semantics.Z, m_zs);
			}

			if (m_b_has_ms) {
				assert (m_ms != null);
				multi_path_impl.setAttributeStreamRef(Semantics.M, m_ms);
			}

			m_path_flags = (AttributeStreamOfInt8) AttributeStreamBase
					.createByteStream(m_paths.size(), (byte) 0);

			multi_path_impl.setPathStreamRef(m_paths);
			multi_path_impl.setPathFlagsStreamRef(m_path_flags);
			multi_path_impl
					.notifyModified(MultiVertexGeometryImpl.DirtyFlags.DirtyAll);

			return polyline;
		}

		private Geometry createMultiPointFromStreams_() {
			assert (m_position != null);
			assert (m_paths == null);
			assert (m_path_flags == null);

			MultiPoint multi_point = new MultiPoint();
			MultiPointImpl multi_point_impl = (MultiPointImpl) multi_point
					._getImpl();
			multi_point_impl.setAttributeStreamRef(Semantics.POSITION,
					m_position);

			if (m_b_has_zs) {
				assert (m_zs != null);
				multi_point_impl.setAttributeStreamRef(Semantics.Z, m_zs);
			}

			if (m_b_has_ms) {
				assert (m_ms != null);
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
				assert (path_size != 0); // we should not have added empty parts
											// on import

				if (path_size == 1) {
					insertIntoAdjustedStreams_(adjusted_position, adjusted_zs,
							adjusted_ms, adjusted_start, path_start, path_size);
					insertIntoAdjustedStreams_(adjusted_position, adjusted_zs,
							adjusted_ms, adjusted_start + 1, path_start,
							path_size);
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

					if (pt1.equals(pt2)
							&& (NumberUtils.isNaN(z1) && NumberUtils.isNaN(z2) || z1 == z2)
							&& (NumberUtils.isNaN(m1) && NumberUtils.isNaN(m2) || m1 == m2)) {
						insertIntoAdjustedStreams_(adjusted_position,
								adjusted_zs, adjusted_ms, adjusted_start,
								path_start, path_size - 1);
						adjusted_start += path_size - 1;
					} else {
						insertIntoAdjustedStreams_(adjusted_position,
								adjusted_zs, adjusted_ms, adjusted_start,
								path_start, path_size);
						adjusted_start += path_size;
					}
				} else {
					insertIntoAdjustedStreams_(adjusted_position, adjusted_zs,
							adjusted_ms, adjusted_start, path_start, path_size);
					adjusted_start += path_size;
				}
				adjusted_paths.write(path + 1, adjusted_start);
			}

			m_position = adjusted_position;
			m_paths = adjusted_paths;
			m_zs = adjusted_zs;
			m_ms = adjusted_ms;
		}

		private void insertIntoAdjustedStreams_(
				AttributeStreamOfDbl adjusted_position,
				AttributeStreamOfDbl adjusted_zs,
				AttributeStreamOfDbl adjusted_ms, int adjusted_start,
				int path_start, int count) {
			adjusted_position.insertRange(adjusted_start * 2, m_position,
					path_start * 2, count * 2, true, 2, adjusted_start * 2);

			if (m_b_has_zs) {
				adjusted_zs.insertRange(adjusted_start, m_zs, path_start,
						count, true, 1, adjusted_start);
			}

			if (m_b_has_ms) {
				adjusted_ms.insertRange(adjusted_start, m_ms, path_start,
						count, true, 1, adjusted_start);
			}
		}

		static SpatialReference importSpatialReferenceFromCrs(
				JsonReader json_iterator, ProgressTracker progress_tracker)
				throws JsonGeometryException {
			// According to the spec, a null crs corresponds to no spatial
			// reference
			if (json_iterator.currentToken() == JsonReader.Token.VALUE_NULL) {
				return null;
			}

			if (json_iterator.currentToken() == JsonReader.Token.VALUE_STRING) {// see
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
				int wkid = GeoJsonCrsTables
						.getWkidFromCrsShortForm(crs_short_form);

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

			if (json_iterator.currentToken() != JsonReader.Token.START_OBJECT) {
				throw new JsonGeometryException("parsing error");
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
			String crs_field = null;
			String properties_field = null;
			String crs_identifier_name = null;
			String crs_identifier_urn = null;
			String crs_identifier_href = null;
			String crs_identifier_url = null;
			String esriwkt = null;
			int crs_identifier_code = -1;
			JsonReader.Token current_token;

			while (json_iterator.nextToken() != JsonReader.Token.END_OBJECT) {
				crs_field = json_iterator.currentString();

				if (crs_field.equals("type")) {
					if (b_found_type) {
						throw new JsonGeometryException("parsing error");
					}

					b_found_type = true;

					current_token = json_iterator.nextToken();

					if (current_token != JsonReader.Token.VALUE_STRING) {
						throw new JsonGeometryException("parsing error");
					}

					//type = json_iterator.currentString();
				} else if (crs_field.equals("properties")) {
					if (b_found_properties) {
						throw new JsonGeometryException("parsing error");
					}

					b_found_properties = true;

					current_token = json_iterator.nextToken();

					if (current_token != JsonReader.Token.START_OBJECT) {
						throw new JsonGeometryException("parsing error");
					}

					while (json_iterator.nextToken() != JsonReader.Token.END_OBJECT) {
						properties_field = json_iterator.currentString();

						if (properties_field.equals("name")) {
							if (b_found_properties_name) {
								throw new JsonGeometryException("parsing error");
							}

							b_found_properties_name = true;
							crs_identifier_name = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("href")) {
							if (b_found_properties_href) {
								throw new JsonGeometryException("parsing error");
							}

							b_found_properties_href = true;
							crs_identifier_href = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("urn")) {
							if (b_found_properties_urn) {
								throw new JsonGeometryException("parsing error");
							}

							b_found_properties_urn = true;
							crs_identifier_urn = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("url")) {
							if (b_found_properties_url) {
								throw new JsonGeometryException("parsing error");
							}

							b_found_properties_url = true;
							crs_identifier_url = getCrsIdentifier_(json_iterator);
						} else if (properties_field.equals("code")) {
							if (b_found_properties_code) {
								throw new JsonGeometryException("parsing error");
							}

							b_found_properties_code = true;

							current_token = json_iterator.nextToken();

							if (current_token != JsonReader.Token.VALUE_NUMBER_INT) {
								throw new JsonGeometryException("parsing error");
							}

							crs_identifier_code = json_iterator
									.currentIntValue();
						} else {
							json_iterator.nextToken();
							json_iterator.skipChildren();
						}
					}
				} else if (crs_field.equals("esriwkt")) {
					if (b_found_esriwkt) {
						throw new JsonGeometryException("parsing error");
					}

					b_found_esriwkt = true;

					current_token = json_iterator.nextToken();

					if (current_token != JsonReader.Token.VALUE_STRING) {
						throw new JsonGeometryException("parsing error");
					}

					esriwkt = json_iterator.currentString();
				} else {
					json_iterator.nextToken();
					json_iterator.skipChildren();
				}
			}

			if ((!b_found_type || !b_found_properties) && !b_found_esriwkt) {
				throw new JsonGeometryException("parsing error");
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
				wkid = GeoJsonCrsTables
						.getWkidFromCrsOgcUrn(crs_identifier_urn); // see
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
				throw new JsonGeometryException("parsing error");
			}

			if (wkid < 0 && !b_found_esriwkt && !b_found_properties_name) {
				throw new JsonGeometryException("parsing error");
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
						String potential_wkt = GeoJsonCrsTables
								.getWktFromCrsName(crs_identifier_name);
						spatial_reference = SpatialReference
								.create(potential_wkt);
					}
				} catch (Exception e) {
				}
			}

			return spatial_reference;
		}

		// see http://geojsonwg.github.io/draft-geojson/draft.html
		static SpatialReference importSpatialReferenceFromCrsUrn_(
				JsonReader json_iterator, ProgressTracker progress_tracker)
				throws JsonGeometryException {
			// According to the spec, a null crs corresponds to no spatial
			// reference
			if (json_iterator.currentToken() == JsonReader.Token.VALUE_NULL) {
				return null;
			}

			if (json_iterator.currentToken() != JsonReader.Token.VALUE_STRING) {
				throw new JsonGeometryException("parsing error");
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
				throws JsonGeometryException {
			JsonReader.Token current_token = json_iterator.nextToken();

			if (current_token != JsonReader.Token.VALUE_STRING) {
				throw new JsonGeometryException("parsing error");
			}

			return json_iterator.currentString();
		}

	}

	@Override
	public MapOGCStructure executeOGC(int import_flags, String geoJsonString,
			ProgressTracker progress_tracker) throws JsonGeometryException {
		return executeOGC(import_flags, JsonParserReader.createFromString(geoJsonString),
				progress_tracker);
	}
	
	public MapOGCStructure executeOGC(int import_flags,
			JsonReader json_iterator, ProgressTracker progress_tracker)
			throws JsonGeometryException {
		MapOGCStructure mapOGCStructure = OperatorImportFromGeoJsonHelper.importFromGeoJson(
				import_flags, Geometry.Type.Unknown, json_iterator,
				progress_tracker, false, 0);
		
		//This is to restore legacy behavior when we always return a geometry collection of one element.
		MapOGCStructure res = new MapOGCStructure();
		res.m_ogcStructure = new OGCStructure();
		res.m_ogcStructure.m_type = 0;
		res.m_ogcStructure.m_structures = new ArrayList<OGCStructure>();
		res.m_ogcStructure.m_structures.add(mapOGCStructure.m_ogcStructure);
		res.m_spatialReference = mapOGCStructure.m_spatialReference;
		return res;
	}

}
