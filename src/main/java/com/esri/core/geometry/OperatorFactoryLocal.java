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

package com.esri.core.geometry;

import com.esri.core.geometry.Operator.Type;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 *An abstract class that represent the basic OperatorFactory interface.
 */
public class OperatorFactoryLocal extends OperatorFactory {
	private static final OperatorFactoryLocal INSTANCE = new OperatorFactoryLocal();

	private static final HashMap<Operator.Type, Operator> st_supportedOperators = new HashMap<Operator.Type, Operator>();

	static {
		// Register all implemented operator allocators in the dictionary

		st_supportedOperators.put(Type.Project, new OperatorProjectLocal());
		st_supportedOperators.put(Type.ExportToJson,
				new OperatorExportToJsonLocal());
		st_supportedOperators.put(Type.ImportFromJson,
				new OperatorImportFromJsonLocal());
		st_supportedOperators.put(Type.ExportToESRIShape,
				new OperatorExportToESRIShapeLocal());
		st_supportedOperators.put(Type.ImportFromESRIShape,
				new OperatorImportFromESRIShapeLocal());

		st_supportedOperators.put(Type.Proximity2D,
				new OperatorProximity2DLocal());
		st_supportedOperators.put(Type.Centroid2D,
				new OperatorCentroid2DLocal());
		st_supportedOperators.put(Type.DensifyByLength,
				new OperatorDensifyByLengthLocal());

		st_supportedOperators.put(Type.Relate, new OperatorRelateLocal());
		st_supportedOperators.put(Type.Equals, new OperatorEqualsLocal());
		st_supportedOperators.put(Type.Disjoint, new OperatorDisjointLocal());

		st_supportedOperators.put(Type.Intersects,
				new OperatorIntersectsLocal());
		st_supportedOperators.put(Type.Within, new OperatorWithinLocal());
		st_supportedOperators.put(Type.Contains, new OperatorContainsLocal());
		st_supportedOperators.put(Type.Crosses, new OperatorCrossesLocal());
		st_supportedOperators.put(Type.Touches, new OperatorTouchesLocal());
		st_supportedOperators.put(Type.Overlaps, new OperatorOverlapsLocal());

		st_supportedOperators.put(Type.SimplifyOGC,
				new OperatorSimplifyLocalOGC());
		st_supportedOperators.put(Type.Simplify, new OperatorSimplifyLocal());
		st_supportedOperators.put(Type.Offset, new OperatorOffsetLocal());

		st_supportedOperators.put(Type.GeodeticDensifyByLength,
				new OperatorGeodeticDensifyLocal());
		
	  st_supportedOperators.put(Type.ShapePreservingDensify,
		    new OperatorShapePreservingDensifyLocal());	
		
	  st_supportedOperators.put(Type.GeodesicBuffer,
		    new OperatorGeodesicBufferLocal());	

		st_supportedOperators.put(Type.GeodeticLength,
				new OperatorGeodeticLengthLocal());
		st_supportedOperators.put(Type.GeodeticArea,
				new OperatorGeodeticAreaLocal());

		st_supportedOperators.put(Type.Buffer, new OperatorBufferLocal());
		st_supportedOperators.put(Type.Distance, new OperatorDistanceLocal());
		st_supportedOperators.put(Type.Intersection,
				new OperatorIntersectionLocal());
		st_supportedOperators.put(Type.Difference,
				new OperatorDifferenceLocal());
		st_supportedOperators.put(Type.SymmetricDifference,
				new OperatorSymmetricDifferenceLocal());
		st_supportedOperators.put(Type.Clip, new OperatorClipLocal());
		st_supportedOperators.put(Type.Cut, new OperatorCutLocal());
		st_supportedOperators.put(Type.ExportToWkb,
				new OperatorExportToWkbLocal());
		st_supportedOperators.put(Type.ImportFromWkb,
				new OperatorImportFromWkbLocal());
		st_supportedOperators.put(Type.ExportToWkt,
				new OperatorExportToWktLocal());
		st_supportedOperators.put(Type.ImportFromWkt,
				new OperatorImportFromWktLocal());
		st_supportedOperators.put(Type.ImportFromGeoJson,
				new OperatorImportFromGeoJsonLocal());
        st_supportedOperators.put(Type.ExportToGeoJson,
                new OperatorExportToGeoJsonLocal());
		st_supportedOperators.put(Type.Union, new OperatorUnionLocal());

		st_supportedOperators.put(Type.Generalize,
				new OperatorGeneralizeLocal());
		st_supportedOperators.put(Type.ConvexHull,
				new OperatorConvexHullLocal());
		st_supportedOperators.put(Type.Boundary, new OperatorBoundaryLocal());

		// LabelPoint, - not ported

	}

	private OperatorFactoryLocal() {

	}


	/**
	 *Returns a reference to the singleton.
	 */
	public static OperatorFactoryLocal getInstance() {
		return INSTANCE;
	}

	@Override
	public Operator getOperator(Type type) {
		if (st_supportedOperators.containsKey(type)) {
			return st_supportedOperators.get(type);
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean isOperatorSupported(Operator.Type type) {
		return st_supportedOperators.containsKey(type);
	}

	public static void saveJSONToTextFileDbg(String file_name,
			Geometry geometry, SpatialReference spatial_ref) {
		if (file_name == null) {
			throw new IllegalArgumentException();
		}

		OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		OperatorExportToJson exporterJSON = (OperatorExportToJson) engine
				.getOperator(Operator.Type.ExportToJson);
		String jsonString = exporterJSON.execute(spatial_ref, geometry);

		try {
			FileOutputStream outfile = new FileOutputStream(file_name);
			PrintStream p = new PrintStream(outfile);
			p.print(jsonString);
			p.close();
		} catch (Exception ex) {
		}
	}

	public static MapGeometry loadGeometryFromJSONFileDbg(String file_name) {
		if (file_name == null) {
			throw new IllegalArgumentException();
		}

		String jsonString = null;
		try {
			FileInputStream stream = new FileInputStream(file_name);
			Reader reader = new BufferedReader(new InputStreamReader(stream));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			stream.close();

			jsonString = builder.toString();
		} catch (Exception ex) {
		}

		MapGeometry mapGeom = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, jsonString);
		return mapGeom;
	}

	public static MapGeometry loadGeometryFromJSONStringDbg(String json) {
		if (json == null) {
			throw new IllegalArgumentException();
		}

		MapGeometry mapGeom = null;
		try {
			mapGeom = OperatorImportFromJson.local().execute(Geometry.Type.Unknown, json);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.toString());
		}
		return mapGeom;
	}
	
	public static Geometry loadGeometryFromEsriShapeDbg(String file_name) {
		if (file_name == null) {
			throw new IllegalArgumentException();
		}

		try {
			FileInputStream stream = new FileInputStream(file_name);
			FileChannel fchan = stream.getChannel();
			ByteBuffer bb = ByteBuffer.allocate((int) fchan.size());
			fchan.read(bb);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			Geometry g = OperatorImportFromESRIShape.local().execute(0,
					Geometry.Type.Unknown, bb);
			fchan.close();
			stream.close();
			return g;
		} catch (Exception ex) {
			throw new IllegalArgumentException();
		}
	}

	public static void saveGeometryToEsriShapeDbg(String file_name, Geometry geometry) {
		if (file_name == null) {
			throw new IllegalArgumentException();
		}

		try {
			ByteBuffer bb = OperatorExportToESRIShape.local().execute(0, geometry);
			FileOutputStream outfile = new FileOutputStream(file_name);
			FileChannel fchan = outfile.getChannel();
			fchan.write(bb);
			fchan.close();
			outfile.close();
		} catch (Exception ex) {
			throw new IllegalArgumentException();
		}
	}
	
	public static void saveToWKTFileDbg(String file_name,
			Geometry geometry, SpatialReference spatial_ref) {
		if (file_name == null) {
			throw new IllegalArgumentException();
		}

		String jsonString = OperatorExportToWkt.local().execute(0, geometry, null);

		try {
			FileOutputStream outfile = new FileOutputStream(file_name);
			PrintStream p = new PrintStream(outfile);
			p.print(jsonString);
			p.close();
		} catch (Exception ex) {
		}
	}

	public static Geometry loadGeometryFromWKTFileDbg(String file_name) {
		if (file_name == null) {
			throw new IllegalArgumentException();
		}

		String s = null;
		try {
			FileInputStream stream = new FileInputStream(file_name);
			Reader reader = new BufferedReader(new InputStreamReader(stream));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			stream.close();

			s = builder.toString();
		} catch (Exception ex) {
		}

		return OperatorImportFromWkt.local().execute(0, Geometry.Type.Unknown, s, null);
	}


}
