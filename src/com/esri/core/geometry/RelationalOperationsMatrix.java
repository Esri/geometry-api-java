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

class RelationalOperationsMatrix {
	private TopoGraph m_topo_graph;
	private int[] m_matrix;
	private boolean[] m_perform_predicates;
	private String m_scl;
	private int m_predicates;
	private int m_predicate_count;
	private int m_cluster_index_a;
	private int m_cluster_index_b;
	private int m_visited_index;

	private interface MatrixPredicate {
		static final int InteriorInterior = 0;
		static final int InteriorBoundary = 1;
		static final int InteriorExterior = 2;
		static final int BoundaryInterior = 3;
		static final int BoundaryBoundary = 4;
		static final int BoundaryExterior = 5;
		static final int ExteriorInterior = 6;
		static final int ExteriorBoundary = 7;
		static final int ExteriorExterior = 8;
	}

	private interface Predicates {
		static final int AreaAreaPredicates = 0;
		static final int AreaLinePredicates = 1;
		static final int LineLinePredicates = 2;
		static final int AreaPointPredicates = 3;
		static final int LinePointPredicates = 4;
		static final int PointPointPredicates = 5;
	}

	// Computes the necessary 9 intersection relationships of boundary,
	// interior, and exterior of geometry_a vs geometry_b for the given scl
	// string.
	static boolean relate(Geometry geometry_a, Geometry geometry_b,
			SpatialReference sr, String scl, ProgressTracker progress_tracker) {
		int relation = getPredefinedRelation_(scl, geometry_a.getDimension(),
				geometry_b.getDimension());

		if (relation != RelationalOperations.Relation.unknown)
			return RelationalOperations.relate(geometry_a, geometry_b, sr,
					relation, progress_tracker);

		Envelope2D env1 = new Envelope2D();
		geometry_a.queryEnvelope2D(env1);
		Envelope2D env2 = new Envelope2D();
		geometry_b.queryEnvelope2D(env2);

		Envelope2D envMerged = new Envelope2D();
		envMerged.setCoords(env1);
		envMerged.merge(env2);
		double tolerance = InternalUtils.calculateToleranceFromGeometry(sr,
				envMerged, false);

		Geometry _geometryA = convertGeometry_(geometry_a, tolerance);
		Geometry _geometryB = convertGeometry_(geometry_b, tolerance);

		int typeA = _geometryA.getType().value();
		int typeB = _geometryB.getType().value();

		boolean bRelation = false;

		switch (typeA) {
		case Geometry.GeometryType.Polygon:
			switch (typeB) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePolygon_((Polygon) (_geometryA),
						(Polygon) (_geometryB), tolerance, scl,
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polygonRelatePolyline_((Polygon) (_geometryA),
						(Polyline) (_geometryB), tolerance, scl,
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = polygonRelatePoint_((Polygon) (_geometryA),
						(Point) (_geometryB), tolerance, scl, progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = polygonRelateMultiPoint_((Polygon) (_geometryA),
						(MultiPoint) (_geometryB), tolerance, scl,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Polyline:
			switch (typeB) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePolyline_((Polygon) (_geometryB),
						(Polyline) (_geometryA), tolerance,
						transposeMatrix_(scl), progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePolyline_((Polyline) (_geometryA),
						(Polyline) (_geometryB), tolerance, scl,
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = polylineRelatePoint_((Polyline) (_geometryA),
						(Point) (_geometryB), tolerance, scl, progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometryA),
						(MultiPoint) (_geometryB), tolerance, scl,
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.Point:
			switch (typeB) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelatePoint_((Polygon) (_geometryB),
						(Point) (_geometryA), tolerance, transposeMatrix_(scl),
						progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePoint_((Polyline) (_geometryB),
						(Point) (_geometryA), tolerance, transposeMatrix_(scl),
						progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = pointRelatePoint_((Point) (_geometryA),
						(Point) (_geometryB), tolerance, scl, progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometryB),
						(Point) (_geometryA), tolerance, transposeMatrix_(scl),
						progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;

		case Geometry.GeometryType.MultiPoint:
			switch (typeB) {
			case Geometry.GeometryType.Polygon:
				bRelation = polygonRelateMultiPoint_((Polygon) (_geometryB),
						(MultiPoint) (_geometryA), tolerance,
						transposeMatrix_(scl), progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometryB),
						(MultiPoint) (_geometryA), tolerance,
						transposeMatrix_(scl), progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelateMultiPoint_(
						(MultiPoint) (_geometryA), (MultiPoint) (_geometryB),
						tolerance, scl, progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometryA),
						(Point) (_geometryB), tolerance, scl, progress_tracker);
				break;

			default:
				break; // warning fix
			}
			break;
		default:
			bRelation = false;
			break;
		}

		return bRelation;
	}

	private RelationalOperationsMatrix() {
		m_predicate_count = 0;
		m_topo_graph = new TopoGraph();
		m_matrix = new int[9];
		m_perform_predicates = new boolean[9];
	}

	// Returns true if the relation holds.
	static boolean polygonRelatePolygon_(Polygon polygon_a, Polygon polygon_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setAreaAreaPredicates_();

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(polygon_a);
		int geom_b = edit_shape.addGeometry(polygon_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polygonRelatePolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setAreaLinePredicates_();

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(polygon_a);
		int geom_b = edit_shape.addGeometry(polyline_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.m_cluster_index_b = relOps.m_topo_graph
				.createUserIndexForClusters();
		markClusters_(geom_b, relOps.m_topo_graph, relOps.m_cluster_index_b);
		relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
		relOps.m_topo_graph
				.deleteUserIndexForClusters(relOps.m_cluster_index_b);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds
	static boolean polygonRelateMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setAreaPointPredicates_();

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(polygon_a);
		int geom_b = edit_shape.addGeometry(multipoint_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polylineRelatePolyline_(Polyline polyline_a,
			Polyline polyline_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setLineLinePredicates_();

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(polyline_a);
		int geom_b = edit_shape.addGeometry(polyline_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.m_cluster_index_a = relOps.m_topo_graph
				.createUserIndexForClusters();
		relOps.m_cluster_index_b = relOps.m_topo_graph
				.createUserIndexForClusters();
		markClusters_(geom_a, relOps.m_topo_graph, relOps.m_cluster_index_a);
		markClusters_(geom_b, relOps.m_topo_graph, relOps.m_cluster_index_b);
		relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
		relOps.m_topo_graph
				.deleteUserIndexForClusters(relOps.m_cluster_index_a);
		relOps.m_topo_graph
				.deleteUserIndexForClusters(relOps.m_cluster_index_b);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polylineRelateMultiPoint_(Polyline polyline_a,
			MultiPoint multipoint_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setLinePointPredicates_();

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(polyline_a);
		int geom_b = edit_shape.addGeometry(multipoint_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.m_cluster_index_a = relOps.m_topo_graph
				.createUserIndexForClusters();
		markClusters_(geom_a, relOps.m_topo_graph, relOps.m_cluster_index_a);
		relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
		relOps.m_topo_graph
				.deleteUserIndexForClusters(relOps.m_cluster_index_a);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean multiPointRelateMultiPoint_(MultiPoint multipoint_a,
			MultiPoint multipoint_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setPointPointPredicates_();

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(multipoint_a);
		int geom_b = edit_shape.addGeometry(multipoint_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polygonRelatePoint_(Polygon polygon_a, Point point_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
		Point2D pt_b = point_b.getXY();
		int[] matrix = new int[9];

		for (int i = 0; i < 8; i++)
			matrix[i] = -1;

		PolygonUtils.PiPResult res = PolygonUtils.isPointInPolygon2D(polygon_a,
				pt_b, tolerance);

		if (res == PolygonUtils.PiPResult.PiPInside)
			matrix[MatrixPredicate.InteriorInterior] = 0;
		else if (res == PolygonUtils.PiPResult.PiPBoundary)
			matrix[MatrixPredicate.BoundaryInterior] = 0;
		else
			matrix[MatrixPredicate.ExteriorInterior] = 0;

		matrix[MatrixPredicate.InteriorExterior] = 2;
		matrix[MatrixPredicate.BoundaryExterior] = 1;
		matrix[MatrixPredicate.ExteriorExterior] = 2;

		boolean bRelation = relationCompare_(matrix, scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polylineRelatePoint_(Polyline polyline_a, Point point_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setLinePointPredicates_();

		MultiPoint multipoint_b = new MultiPoint();
		multipoint_b.add(point_b);

		EditShape edit_shape = new EditShape();
		int geom_a = edit_shape.addGeometry(polyline_a);
		int geom_b = edit_shape.addGeometry(multipoint_b);
		relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
				progress_tracker);
		relOps.m_cluster_index_a = relOps.m_topo_graph
				.createUserIndexForClusters();
		markClusters_(geom_a, relOps.m_topo_graph, relOps.m_cluster_index_a);
		relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
		relOps.m_topo_graph
				.deleteUserIndexForClusters(relOps.m_cluster_index_a);
		relOps.m_topo_graph.removeShape();

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean multiPointRelatePoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		Point2D pt_b = point_b.getXY();
		int[] matrix = new int[9];

		for (int i = 0; i < 8; i++)
			matrix[i] = -1;

		boolean b_intersects = false;
		boolean b_multipoint_contained = true;
		double tolerance_sq = tolerance * tolerance;
		Point2D pt_a = new Point2D();

		for (int i = 0; i < multipoint_a.getPointCount(); i++) {
			multipoint_a.getXY(i, pt_a);

			if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance_sq) {
				b_intersects = true;
			} else {
				b_multipoint_contained = false;
			}

			if (b_intersects && !b_multipoint_contained)
				break;
		}

		if (b_intersects) {
			matrix[MatrixPredicate.InteriorInterior] = 0;

			if (!b_multipoint_contained)
				matrix[MatrixPredicate.InteriorExterior] = 0;
		} else {
			matrix[MatrixPredicate.InteriorExterior] = 0;
			matrix[MatrixPredicate.ExteriorInterior] = 0;
		}

		matrix[MatrixPredicate.ExteriorExterior] = 2;

		boolean bRelation = relationCompare_(matrix, scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean pointRelatePoint_(Point point_a, Point point_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
		Point2D pt_a = point_a.getXY();
		Point2D pt_b = point_b.getXY();
		int[] matrix = new int[9];

		for (int i = 1; i < 8; i++)
			matrix[i] = -1;

		if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance * tolerance) {
			matrix[MatrixPredicate.InteriorInterior] = 0;
		} else {
			matrix[MatrixPredicate.InteriorExterior] = 0;
			matrix[MatrixPredicate.ExteriorInterior] = 0;
		}

		matrix[MatrixPredicate.ExteriorExterior] = 2;

		boolean bRelation = relationCompare_(matrix, scl);
		return bRelation;
	}

	// Compares the DE-9I matrix against the scl string.
	private static boolean relationCompare_(int[] matrix, String scl) {
		for (int i = 0; i < 9; i++) {
			switch (scl.charAt(i)) {
			case 'T':
				assert (matrix[i] != -2);
				if (matrix[i] == -1)
					return false;
				break;

			case 'F':
				assert (matrix[i] != -2);
				if (matrix[i] != -1)
					return false;
				break;

			case '0':
				assert (matrix[i] != -2);
				if (matrix[i] != 0)
					return false;
				break;

			case '1':
				assert (matrix[i] != -2);
				if (matrix[i] != 1)
					return false;
				break;

			case '2':
				assert (matrix[i] != -2);
				if (matrix[i] != 2)
					return false;
				break;
			}
		}

		return true;
	}

	// Checks whether scl string is a predefined relation.
	private static int getPredefinedRelation_(String scl, int dim_a, int dim_b) {
		if (equals_(scl))
			return RelationalOperations.Relation.equals;

		if (disjoint_(scl))
			return RelationalOperations.Relation.disjoint;

		if (touches_(scl, dim_a, dim_b))
			return RelationalOperations.Relation.touches;

		if (crosses_(scl, dim_a, dim_b))
			return RelationalOperations.Relation.crosses;

		if (contains_(scl))
			return RelationalOperations.Relation.contains;

		if (overlaps_(scl, dim_a, dim_b))
			return RelationalOperations.Relation.overlaps;

		return RelationalOperations.Relation.unknown;
	}

	// Checks whether the scl string is the equals relation.
	private static boolean equals_(String scl) {
		// Valid for all
		if (scl.charAt(0) == 'T' && scl.charAt(1) == '*'
				&& scl.charAt(2) == 'F' && scl.charAt(3) == '*'
				&& scl.charAt(4) == '*' && scl.charAt(5) == 'F'
				&& scl.charAt(6) == 'F' && scl.charAt(7) == 'F'
				&& scl.charAt(8) == '*')
			return true;

		return false;
	}

	// Checks whether the scl string is the disjoint relation.
	private static boolean disjoint_(String scl) {
		if (scl.charAt(0) == 'F' && scl.charAt(1) == 'F'
				&& scl.charAt(2) == '*' && scl.charAt(3) == 'F'
				&& scl.charAt(4) == 'F' && scl.charAt(5) == '*'
				&& scl.charAt(6) == '*' && scl.charAt(7) == '*'
				&& scl.charAt(8) == '*')
			return true;

		return false;
	}

	// Checks whether the scl string is the touches relation.
	private static boolean touches_(String scl, int dim_a, int dim_b) {
		// Points cant touch
		if (dim_a == 0 && dim_b == 0)
			return false;

		if (!(dim_a == 2 && dim_b == 2)) {
			// Valid for area-Line, Line-Line, area-Point, and Line-Point
			if (scl.charAt(0) == 'F' && scl.charAt(1) == '*'
					&& scl.charAt(2) == '*' && scl.charAt(3) == 'T'
					&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
					&& scl.charAt(6) == '*' && scl.charAt(7) == '*'
					&& scl.charAt(8) == '*')
				return true;

			if (dim_a == 1 && dim_b == 1) {
				// Valid for Line-Line
				if (scl.charAt(0) == 'F' && scl.charAt(1) == 'T'
						&& scl.charAt(2) == '*' && scl.charAt(3) == '*'
						&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
						&& scl.charAt(6) == '*' && scl.charAt(7) == '*'
						&& scl.charAt(8) == '*')
					return true;
			}
		}

		// Valid for area-area, area-Line, Line-Line

		if (dim_b != 0) {
			if (scl.charAt(0) == 'F' && scl.charAt(1) == '*'
					&& scl.charAt(2) == '*' && scl.charAt(3) == '*'
					&& scl.charAt(4) == 'T' && scl.charAt(5) == '*'
					&& scl.charAt(6) == '*' && scl.charAt(7) == '*'
					&& scl.charAt(8) == '*')
				return true;
		}

		return false;
	}

	// Checks whether the scl string is the crosses relation.
	private static boolean crosses_(String scl, int dim_a, int dim_b) {
		if (dim_a > dim_b) {
			// Valid for area-Line, area-Point, Line-Point
			if (scl.charAt(0) == 'T' && scl.charAt(1) == '*'
					&& scl.charAt(2) == '*' && scl.charAt(3) == '*'
					&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
					&& scl.charAt(6) == 'T' && scl.charAt(7) == '*'
					&& scl.charAt(8) == '*')
				return true;

			return false;
		}

		if (dim_a == 1 && dim_b == 1) {
			// Valid for Line-Line
			if (scl.charAt(0) == '0' && scl.charAt(1) == '*'
					&& scl.charAt(2) == '*' && scl.charAt(3) == '*'
					&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
					&& scl.charAt(6) == '*' && scl.charAt(7) == '*'
					&& scl.charAt(8) == '*')
				return true;
		}

		return false;
	}

	// Checks whether the scl string is the contains relation.
	private static boolean contains_(String scl) {
		// Valid for all
		if (scl.charAt(0) == 'T' && scl.charAt(1) == '*'
				&& scl.charAt(2) == '*' && scl.charAt(3) == '*'
				&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
				&& scl.charAt(6) == 'F' && scl.charAt(7) == 'F'
				&& scl.charAt(8) == '*')
			return true;

		return false;
	}

	// Checks whether the scl string is the overlaps relation.
	private static boolean overlaps_(String scl, int dim_a, int dim_b) {
		if (dim_a == dim_b) {
			if (dim_a != 1) {
				// Valid for area-area, Point-Point
				if (scl.charAt(0) == 'T' && scl.charAt(1) == '*'
						&& scl.charAt(2) == 'T' && scl.charAt(3) == '*'
						&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
						&& scl.charAt(6) == 'T' && scl.charAt(7) == '*'
						&& scl.charAt(8) == '*')
					return true;

				return false;
			}

			// Valid for Line-Line
			if (scl.charAt(0) == '1' && scl.charAt(1) == '*'
					&& scl.charAt(2) == 'T' && scl.charAt(3) == '*'
					&& scl.charAt(4) == '*' && scl.charAt(5) == '*'
					&& scl.charAt(6) == 'T' && scl.charAt(7) == '*'
					&& scl.charAt(8) == '*')
				return true;
		}

		return false;
	}

	// Marks each cluster of the topoGraph as belonging to an interior vertex of
	// the geometry and/or a boundary index of the geometry.
	private static void markClusters_(int geometry, TopoGraph topoGraph,
			int clusterIndex) {
		EditShape edit_shape = topoGraph.getShape();

		for (int path = edit_shape.getFirstPath(geometry); path != -1; path = edit_shape
				.getNextPath(path)) {
			int vertexFirst = edit_shape.getFirstVertex(path);
			int vertexLast = edit_shape.getLastVertex(path);
			boolean b_closed = (vertexFirst == vertexLast);

			int vertex = vertexFirst;

			do {
				int cluster = topoGraph.getClusterFromVertex(vertex);
				int index = topoGraph
						.getClusterUserIndex(cluster, clusterIndex);

				if (!b_closed
						&& (vertex == vertexFirst || vertex == vertexLast)) {
					if (index == -1)
						topoGraph.setClusterUserIndex(cluster, clusterIndex, 1);
					else
						topoGraph.setClusterUserIndex(cluster, clusterIndex,
								index + 1);
				} else {
					if (index == -1)
						topoGraph.setClusterUserIndex(cluster, clusterIndex, 0);
				}

				vertex = edit_shape.getNextVertex(vertex);
			} while (vertex != vertexFirst && vertex != -1);
		}
	}

	private static String transposeMatrix_(String scl) {
		String transpose = new String();
		transpose += scl.charAt(0);
		transpose += scl.charAt(3);
		transpose += scl.charAt(6);
		transpose += scl.charAt(1);
		transpose += scl.charAt(4);
		transpose += scl.charAt(7);
		transpose += scl.charAt(2);
		transpose += scl.charAt(5);
		transpose += scl.charAt(8);
		return transpose;
	}

	// Allocates the matrix array if need be, and sets all entries to -2.
	// -2: Not Computed
	// -1: No intersection
	// 0: 0-dimension intersection
	// 1: 1-dimension intersection
	// 2: 2-dimension intersection
	private void resetMatrix_() {
		for (int i = 0; i < 9; i++)
			m_matrix[i] = -2;
	}

	// Sets the relation predicates from the scl string.
	private void setPredicates_(String scl) {
		m_scl = scl;

		for (int i = 0; i < 9; i++) {
			if (m_scl.charAt(i) != '*') {
				m_perform_predicates[i] = true;
				m_predicate_count++;
			} else
				m_perform_predicates[i] = false;
		}
	}

	// Sets the remaining predicates to false
	private void setRemainingPredicatesToFalse_() {
		for (int i = 0; i < 9; i++) {
			if (m_perform_predicates[i] && m_matrix[i] == -2) {
				m_matrix[i] = -1;
				m_perform_predicates[i] = false;
			}
		}
	}

	// Checks whether the predicate is known.
	private boolean isPredicateKnown_(int predicate, int dim) {
		assert (m_scl.charAt(predicate) != '*');

		if (m_matrix[predicate] == -1) {
			m_perform_predicates[predicate] = false;
			m_predicate_count--;
			return true;
		}

		if (m_scl.charAt(predicate) != 'T' && m_scl.charAt(predicate) != 'F') {
			if (m_matrix[predicate] < dim) {
				return false;
			} else {
				m_perform_predicates[predicate] = false;
				m_predicate_count--;
				return true;
			}
		} else {
			if (m_matrix[predicate] == -2) {
				return false;
			} else {
				m_perform_predicates[predicate] = false;
				m_predicate_count--;
				return true;
			}
		}
	}

	// Sets the area-area predicates function.
	private void setAreaAreaPredicates_() {
		m_predicates = Predicates.AreaAreaPredicates;

		// set predicates that are always true/false
		if (m_perform_predicates[MatrixPredicate.ExteriorExterior]) {
			m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
			m_predicate_count--;
		}
	}

	// Sets the area-line predicate function.
	private void setAreaLinePredicates_() {
		m_predicates = Predicates.AreaLinePredicates;

		// set predicates that are always true/false
		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			m_matrix[MatrixPredicate.InteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.InteriorExterior] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorExterior]) {
			m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
			m_predicate_count--;
		}
	}

	// Sets the line-line predicates function.
	private void setLineLinePredicates_() {
		m_predicates = Predicates.LineLinePredicates;

		// set predicates that are always true/false
		if (m_perform_predicates[MatrixPredicate.ExteriorExterior]) {
			m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
			m_predicate_count--;
		}
	}

	// Sets the area-point predicate function.
	private void setAreaPointPredicates_() {
		m_predicates = Predicates.AreaPointPredicates;

		// set predicates that are always true/false
		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			m_matrix[MatrixPredicate.InteriorBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.InteriorBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			m_matrix[MatrixPredicate.InteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.InteriorExterior] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			m_matrix[MatrixPredicate.BoundaryBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.BoundaryBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			m_matrix[MatrixPredicate.BoundaryExterior] = 1; // Always true
			m_perform_predicates[MatrixPredicate.BoundaryExterior] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			m_matrix[MatrixPredicate.ExteriorBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.ExteriorBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorExterior]) {
			m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
			m_predicate_count--;
		}
	}

	// Sets the line-point predicates function.
	private void setLinePointPredicates_() {
		m_predicates = Predicates.LinePointPredicates;

		// set predicates that are always true/false
		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			m_matrix[MatrixPredicate.InteriorBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.InteriorBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			m_matrix[MatrixPredicate.InteriorExterior] = 1; // Always true
			m_perform_predicates[MatrixPredicate.InteriorExterior] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			m_matrix[MatrixPredicate.BoundaryBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.BoundaryBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			m_matrix[MatrixPredicate.ExteriorBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.ExteriorBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorExterior]) {
			m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
			m_predicate_count--;
		}
	}

	// Sets the point-point predicates function.
	private void setPointPointPredicates_() {
		m_predicates = Predicates.PointPointPredicates;

		// set predicates that are always true/false
		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			m_matrix[MatrixPredicate.InteriorBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.InteriorBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			m_matrix[MatrixPredicate.BoundaryInterior] = -1; // Always false
			m_perform_predicates[MatrixPredicate.BoundaryInterior] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			m_matrix[MatrixPredicate.BoundaryBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.BoundaryBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			m_matrix[MatrixPredicate.BoundaryExterior] = -1; // Always false
			m_perform_predicates[MatrixPredicate.BoundaryExterior] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			m_matrix[MatrixPredicate.ExteriorBoundary] = -1; // Always false
			m_perform_predicates[MatrixPredicate.ExteriorBoundary] = false;
			m_predicate_count--;
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorExterior]) {
			m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
			m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
			m_predicate_count--;
		}
	}

	// Invokes the 9 relational predicates of area vs area.
	private boolean areaAreaPredicates_(int half_edge, int id_a, int id_b) {
		boolean bRelationKnown = true;

		if (m_perform_predicates[MatrixPredicate.InteriorInterior]) {
			interiorAreaInteriorArea_(half_edge, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorInterior, 2);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			interiorAreaBoundaryArea_(half_edge, id_a,
					MatrixPredicate.InteriorBoundary);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorBoundary, 1);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			interiorAreaExteriorArea_(half_edge, id_a, id_b,
					MatrixPredicate.InteriorExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorExterior, 2);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			interiorAreaBoundaryArea_(half_edge, id_b,
					MatrixPredicate.BoundaryInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryInterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			boundaryAreaBoundaryArea_(half_edge, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryBoundary, 1);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			boundaryAreaExteriorArea_(half_edge, id_a, id_b,
					MatrixPredicate.BoundaryExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryExterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			interiorAreaExteriorArea_(half_edge, id_b, id_a,
					MatrixPredicate.ExteriorInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior, 2);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			boundaryAreaExteriorArea_(half_edge, id_b, id_a,
					MatrixPredicate.ExteriorBoundary);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorBoundary, 1);
		}

		return bRelationKnown;
	}

	// Invokes the 9 relational predicates of area vs Line.
	private boolean areaLinePredicates_(int half_edge, int id_a, int id_b) {
		boolean bRelationKnown = true;

		if (m_perform_predicates[MatrixPredicate.InteriorInterior]) {
			interiorAreaInteriorLine_(half_edge, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorInterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			interiorAreaBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorBoundary, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			boundaryAreaInteriorLine_(half_edge, id_a, id_b, m_cluster_index_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryInterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			boundaryAreaBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryBoundary, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			boundaryAreaExteriorLine_(half_edge, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryExterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			exteriorAreaInteriorLine_(half_edge, id_a);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			exteriorAreaBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorBoundary, 0);
		}

		return bRelationKnown;
	}

	// Invokes the 9 relational predicates of Line vs Line.
	private boolean lineLinePredicates_(int half_edge, int id_a, int id_b) {
		boolean bRelationKnown = true;

		if (m_perform_predicates[MatrixPredicate.InteriorInterior]) {
			interiorLineInteriorLine_(half_edge, id_a, id_b, m_cluster_index_a,
					m_cluster_index_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorInterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			interiorLineBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_a,
					m_cluster_index_b, MatrixPredicate.InteriorBoundary);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorBoundary, 0);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			interiorLineExteriorLine_(half_edge, id_a, id_b,
					MatrixPredicate.InteriorExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorExterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			interiorLineBoundaryLine_(half_edge, id_b, id_a, m_cluster_index_b,
					m_cluster_index_a, MatrixPredicate.BoundaryInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryInterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			boundaryLineBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_a,
					m_cluster_index_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryBoundary, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			boundaryLineExteriorLine_(half_edge, id_a, id_b, m_cluster_index_a,
					MatrixPredicate.BoundaryExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryExterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			interiorLineExteriorLine_(half_edge, id_b, id_a,
					MatrixPredicate.ExteriorInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior, 1);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			boundaryLineExteriorLine_(half_edge, id_b, id_a, m_cluster_index_b,
					MatrixPredicate.ExteriorBoundary);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorBoundary, 0);
		}

		return bRelationKnown;
	}

	// Invokes the 9 relational predicates of area vs Point.
	private boolean areaPointPredicates_(int cluster, int id_a, int id_b) {
		boolean bRelationKnown = true;

		if (m_perform_predicates[MatrixPredicate.InteriorInterior]) {
			interiorAreaInteriorPoint_(cluster, id_a);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorInterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			boundaryAreaInteriorPoint_(cluster, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryInterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			exteriorAreaInteriorPoint_(cluster, id_a);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior, 0);
		}

		return bRelationKnown;
	}

	// Invokes the 9 relational predicates of Line vs Point.
	private boolean linePointPredicates_(int cluster, int id_a, int id_b) {
		boolean bRelationKnown = true;

		if (m_perform_predicates[MatrixPredicate.InteriorInterior]) {
			interiorLineInteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorInterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			boundaryLineInteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryInterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			boundaryLineExteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryExterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			exteriorLineInteriorPoint_(cluster, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior, 0);
		}

		return bRelationKnown;
	}

	// Invokes the 9 relational predicates of Point vs Point.
	private boolean pointPointPredicates_(int cluster, int id_a, int id_b) {
		boolean bRelationKnown = true;

		if (m_perform_predicates[MatrixPredicate.InteriorInterior]) {
			interiorPointInteriorPoint_(cluster, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorInterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			interiorPointExteriorPoint_(cluster, id_a, id_b,
					MatrixPredicate.InteriorExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorExterior, 0);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			interiorPointExteriorPoint_(cluster, id_b, id_a,
					MatrixPredicate.ExteriorInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior, 0);
		}

		return bRelationKnown;
	}

	// Relational predicate to determine if the interior of area A intersects
	// with the interior of area B.
	private void interiorAreaInteriorArea_(int half_edge, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.InteriorInterior] == 2)
			return;

		int faceParentage = m_topo_graph.getHalfEdgeFaceParentage(half_edge);

		if ((faceParentage & id_a) != 0 && (faceParentage & id_b) != 0)
			m_matrix[MatrixPredicate.InteriorInterior] = 2;
	}

	// Relational predicate to determine if the interior of area A intersects
	// with the boundary of area B.
	private void interiorAreaBoundaryArea_(int half_edge, int id_a,
			int predicate) {
		if (m_matrix[predicate] == 1)
			return;

		int faceParentage = m_topo_graph.getHalfEdgeFaceParentage(half_edge);
		int twinFaceParentage = m_topo_graph
				.getHalfEdgeFaceParentage(m_topo_graph
						.getHalfEdgeTwin(half_edge));

		if ((faceParentage & id_a) != 0 && (twinFaceParentage & id_a) != 0)
			m_matrix[predicate] = 1;
	}

	// Relational predicate to determine if the interior of area A intersects
	// with the exterior of area B.
	private void interiorAreaExteriorArea_(int half_edge, int id_a, int id_b,
			int predicate) {
		if (m_matrix[predicate] == 2)
			return;

		int faceParentage = m_topo_graph.getHalfEdgeFaceParentage(half_edge);

		if ((faceParentage & id_a) != 0 && (faceParentage & id_b) == 0)
			m_matrix[predicate] = 2;

	}

	// Relational predicate to determine if the boundary of area A intersects
	// with the boundary of area B.
	private void boundaryAreaBoundaryArea_(int half_edge, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.BoundaryBoundary] == 1)
			return;

		int parentage = m_topo_graph.getHalfEdgeParentage(half_edge);

		if ((parentage & id_a) != 0 && (parentage & id_b) != 0) {
			m_matrix[MatrixPredicate.BoundaryBoundary] = 1;
			return;
		}

		if (m_matrix[MatrixPredicate.BoundaryBoundary] != 0) {
			if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
					.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
					m_visited_index) != 1) {
				int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
				int clusterParentage = m_topo_graph
						.getClusterParentage(cluster);

				if ((clusterParentage & id_a) != 0
						&& (clusterParentage & id_b) != 0) {
					m_matrix[MatrixPredicate.BoundaryBoundary] = 0;
				}
			}
		}
	}

	// Relational predicate to determine if the boundary of area A intersects
	// with the exterior of area B.
	private void boundaryAreaExteriorArea_(int half_edge, int id_a, int id_b,
			int predicate) {
		if (m_matrix[predicate] == 1)
			return;

		int faceParentage = m_topo_graph.getHalfEdgeFaceParentage(half_edge);
		int twinFaceParentage = m_topo_graph
				.getHalfEdgeFaceParentage(m_topo_graph
						.getHalfEdgeTwin(half_edge));

		if ((faceParentage & id_b) == 0 && (twinFaceParentage & id_b) == 0)
			m_matrix[predicate] = 1;
	}

	// Relational predicate to determine if the interior of area A intersects
	// with the interior of Line B.
	private void interiorAreaInteriorLine_(int half_edge, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.InteriorInterior] == 1)
			return;

		int faceParentage = m_topo_graph.getHalfEdgeFaceParentage(half_edge);
		int twinFaceParentage = m_topo_graph
				.getHalfEdgeFaceParentage(m_topo_graph
						.getHalfEdgeTwin(half_edge));

		if ((faceParentage & id_a) != 0 && (twinFaceParentage & id_a) != 0)
			m_matrix[MatrixPredicate.InteriorInterior] = 1;
	}

	// Relational predicate to determine if the interior of area A intersects
	// with the boundary of Line B.
	private void interiorAreaBoundaryLine_(int half_edge, int id_a, int id_b,
			int cluster_index_b) {
		if (m_matrix[MatrixPredicate.InteriorBoundary] == 0)
			return;

		if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
				.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
				m_visited_index) != 1) {
			int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
			int clusterParentage = m_topo_graph.getClusterParentage(cluster);

			if ((clusterParentage & id_a) == 0) {
				int faceParentage = m_topo_graph
						.getHalfEdgeFaceParentage(half_edge);

				if ((faceParentage & id_a) != 0) {
					int index = m_topo_graph.getClusterUserIndex(cluster,
							cluster_index_b);

					if ((clusterParentage & id_b) != 0 && (index % 2 != 0)) {
						assert (index != -1);
						m_matrix[MatrixPredicate.InteriorBoundary] = 0;
					}
				}
			}
		}
	}

	// Relational predicate to determine if the boundary of area A intersects
	// with the interior of Line B.
	private void boundaryAreaInteriorLine_(int half_edge, int id_a, int id_b,
			int cluster_index_b) {
		if (m_matrix[MatrixPredicate.BoundaryInterior] == 1)
			return;

		int parentage = m_topo_graph.getHalfEdgeParentage(half_edge);

		if ((parentage & id_a) != 0 && (parentage & id_b) != 0) {
			m_matrix[MatrixPredicate.BoundaryInterior] = 1;
			return;
		}

		if (m_matrix[MatrixPredicate.BoundaryInterior] != 0) {
			if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
					.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
					m_visited_index) != 1) {
				int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
				int clusterParentage = m_topo_graph
						.getClusterParentage(cluster);

				if ((clusterParentage & id_a) != 0) {
					int index = m_topo_graph.getClusterUserIndex(cluster,
							cluster_index_b);

					if ((clusterParentage & id_b) != 0 && (index % 2 == 0)) {
						assert (index != -1);
						m_matrix[MatrixPredicate.BoundaryInterior] = 0;
					}
				}
			}
		}
	}

	// Relational predicate to determine if the boundary of area A intersects
	// with the boundary of Line B.
	private void boundaryAreaBoundaryLine_(int half_edge, int id_a, int id_b,
			int cluster_index_b) {
		if (m_matrix[MatrixPredicate.BoundaryBoundary] == 0)
			return;

		if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
				.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
				m_visited_index) != 1) {
			int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
			int clusterParentage = m_topo_graph.getClusterParentage(cluster);

			if ((clusterParentage & id_a) != 0) {
				int index = m_topo_graph.getClusterUserIndex(cluster,
						cluster_index_b);

				if ((clusterParentage & id_b) != 0 && (index % 2 != 0)) {
					assert (index != -1);
					m_matrix[MatrixPredicate.BoundaryBoundary] = 0;
				}
			}
		}
	}

	// Relational predicate to determine if the boundary of area A intersects
	// with the exterior of Line B.
	private void boundaryAreaExteriorLine_(int half_edge, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.BoundaryExterior] == 1)
			return;

		int parentage = m_topo_graph.getHalfEdgeParentage(half_edge);

		if ((parentage & id_a) != 0 && (parentage & id_b) == 0)
			m_matrix[MatrixPredicate.BoundaryExterior] = 1;

	}

	// Relational predicate to determine if the exterior of area A intersects
	// with the interior of Line B.
	private void exteriorAreaInteriorLine_(int half_edge, int id_a) {
		if (m_matrix[MatrixPredicate.ExteriorInterior] == 1)
			return;

		int faceParentage = m_topo_graph.getHalfEdgeFaceParentage(half_edge);
		int twinFaceParentage = m_topo_graph
				.getHalfEdgeFaceParentage(m_topo_graph
						.getHalfEdgeTwin(half_edge));

		if ((faceParentage & id_a) == 0 && (twinFaceParentage & id_a) == 0)
			m_matrix[MatrixPredicate.ExteriorInterior] = 1;
	}

	// Relational predicate to determine if the exterior of area A intersects
	// with the boundary of Line B.
	private void exteriorAreaBoundaryLine_(int half_edge, int id_a, int id_b,
			int cluster_index_b) {
		if (m_matrix[MatrixPredicate.ExteriorBoundary] == 0)
			return;

		if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
				.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
				m_visited_index) != 1) {
			int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
			int clusterParentage = m_topo_graph.getClusterParentage(cluster);

			if ((clusterParentage & id_a) == 0) {
				int faceParentage = m_topo_graph
						.getHalfEdgeFaceParentage(half_edge);

				if ((faceParentage & id_a) == 0) {
					assert ((m_topo_graph.getHalfEdgeParentage(m_topo_graph
							.getHalfEdgeTwin(half_edge)) & id_a) == 0);

					int index = m_topo_graph.getClusterUserIndex(cluster,
							cluster_index_b);

					if ((clusterParentage & id_b) != 0 && (index % 2 != 0)) {
						assert (index != -1);
						m_matrix[MatrixPredicate.ExteriorBoundary] = 0;
					}
				}
			}
		}
	}

	// Relational predicate to determine if the interior of Line A intersects
	// with the interior of Line B.
	private void interiorLineInteriorLine_(int half_edge, int id_a, int id_b,
			int cluster_index_a, int cluster_index_b) {
		if (m_matrix[MatrixPredicate.InteriorInterior] == 1)
			return;

		int parentage = m_topo_graph.getHalfEdgeParentage(half_edge);

		if ((parentage & id_a) != 0 && (parentage & id_b) != 0) {
			m_matrix[MatrixPredicate.InteriorInterior] = 1;
			return;
		}

		if (m_matrix[MatrixPredicate.InteriorInterior] != 0) {
			if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
					.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
					m_visited_index) != 1) {
				int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
				int clusterParentage = m_topo_graph
						.getClusterParentage(cluster);

				if ((clusterParentage & id_a) != 0
						&& (clusterParentage & id_b) != 0) {
					int index_a = m_topo_graph.getClusterUserIndex(cluster,
							cluster_index_a);
					int index_b = m_topo_graph.getClusterUserIndex(cluster,
							cluster_index_b);
					assert (index_a != -1);
					assert (index_b != -1);

					if ((index_a % 2 == 0) && (index_b % 2 == 0)) {
						assert ((m_topo_graph.getClusterParentage(cluster) & id_a) != 0 && (m_topo_graph
								.getClusterParentage(cluster) & id_b) != 0);
						m_matrix[MatrixPredicate.InteriorInterior] = 0;
					}
				}
			}
		}
	}

	// Relational predicate to determine of the interior of LineA intersects
	// with the boundary of Line B.
	private void interiorLineBoundaryLine_(int half_edge, int id_a, int id_b,
			int cluster_index_a, int cluster_index_b, int predicate) {
		if (m_matrix[predicate] == 0)
			return;

		if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
				.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
				m_visited_index) != 1) {
			int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
			int clusterParentage = m_topo_graph.getClusterParentage(cluster);

			if ((clusterParentage & id_a) != 0
					&& (clusterParentage & id_b) != 0) {
				int index_a = m_topo_graph.getClusterUserIndex(cluster,
						cluster_index_a);
				int index_b = m_topo_graph.getClusterUserIndex(cluster,
						cluster_index_b);
				assert (index_a != -1);
				assert (index_b != -1);

				if ((index_a % 2 == 0) && (index_b % 2 != 0)) {
					assert ((m_topo_graph.getClusterParentage(cluster) & id_a) != 0 && (m_topo_graph
							.getClusterParentage(cluster) & id_b) != 0);
					m_matrix[predicate] = 0;
				}
			}
		}
	}

	// Relational predicate to determine if the interior of Line A intersects
	// with the exterior of Line B.
	private void interiorLineExteriorLine_(int half_edge, int id_a, int id_b,
			int predicate) {
		if (m_matrix[predicate] == 1)
			return;

		int parentage = m_topo_graph.getHalfEdgeParentage(half_edge);

		if ((parentage & id_a) != 0 && (parentage & id_b) == 0)
			m_matrix[predicate] = 1;
	}

	// Relational predicate to determine if the boundary of Line A intersects
	// with the boundary of Line B.
	private void boundaryLineBoundaryLine_(int half_edge, int id_a, int id_b,
			int cluster_index_a, int cluster_index_b) {
		if (m_matrix[MatrixPredicate.BoundaryBoundary] == 0)
			return;

		if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
				.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
				m_visited_index) != 1) {
			int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
			int clusterParentage = m_topo_graph.getClusterParentage(cluster);

			if ((clusterParentage & id_a) != 0
					&& (clusterParentage & id_b) != 0) {
				int index_a = m_topo_graph.getClusterUserIndex(cluster,
						cluster_index_a);
				int index_b = m_topo_graph.getClusterUserIndex(cluster,
						cluster_index_b);
				assert (index_a != -1);
				assert (index_b != -1);

				if ((index_a % 2 != 0) && (index_b % 2 != 0)) {
					assert ((m_topo_graph.getClusterParentage(cluster) & id_a) != 0 && (m_topo_graph
							.getClusterParentage(cluster) & id_b) != 0);
					m_matrix[MatrixPredicate.BoundaryBoundary] = 0;
				}
			}
		}
	}

	// Relational predicate to determine if the boundary of Line A intersects
	// with the exterior of Line B.
	private void boundaryLineExteriorLine_(int half_edge, int id_a, int id_b,
			int cluster_index_a, int predicate) {
		if (m_matrix[predicate] == 0)
			return;

		if (m_topo_graph.getHalfEdgeUserIndex(m_topo_graph
				.getHalfEdgePrev(m_topo_graph.getHalfEdgeTwin(half_edge)),
				m_visited_index) != 1) {
			int cluster = m_topo_graph.getHalfEdgeTo(half_edge);
			int clusterParentage = m_topo_graph.getClusterParentage(cluster);

			if ((clusterParentage & id_b) == 0) {
				int index = m_topo_graph.getClusterUserIndex(cluster,
						cluster_index_a);
				assert (index != -1);

				if (index % 2 != 0) {
					assert ((m_topo_graph.getClusterParentage(cluster) & id_a) != 0);
					m_matrix[predicate] = 0;
				}
			}
		}
	}

	// Relational predicate to determine if the interior of area A intersects
	// with the interior of Point B.
	private void interiorAreaInteriorPoint_(int cluster, int id_a) {
		if (m_matrix[MatrixPredicate.InteriorInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) == 0) {
			int chain = m_topo_graph.getClusterChain(cluster);
			int chainParentage = m_topo_graph.getChainParentage(chain);

			if ((chainParentage & id_a) != 0) {
				m_matrix[MatrixPredicate.InteriorInterior] = 0;
			}
		}
	}

	// Relational predicate to determine if the boundary of area A intersects
	// with the interior of Point B.
	private void boundaryAreaInteriorPoint_(int cluster, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.BoundaryInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) != 0 && (clusterParentage & id_b) != 0) {
			m_matrix[MatrixPredicate.BoundaryInterior] = 0;
		}
	}

	// Relational predicate to determine if the exterior of area A intersects
	// with the interior of Point B.
	private void exteriorAreaInteriorPoint_(int cluster, int id_a) {
		if (m_matrix[MatrixPredicate.ExteriorInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) == 0) {
			int chain = m_topo_graph.getClusterChain(cluster);
			int chainParentage = m_topo_graph.getChainParentage(chain);

			if ((chainParentage & id_a) == 0) {
				m_matrix[MatrixPredicate.ExteriorInterior] = 0;
			}
		}
	}

	// Relational predicate to determine if the interior of Line A intersects
	// with the interior of Point B.
	private void interiorLineInteriorPoint_(int cluster, int id_a, int id_b,
			int cluster_index_a) {
		if (m_matrix[MatrixPredicate.InteriorInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) != 0 && (clusterParentage & id_b) != 0) {
			int index = m_topo_graph.getClusterUserIndex(cluster,
					cluster_index_a);

			if (index % 2 == 0) {
				m_matrix[MatrixPredicate.InteriorInterior] = 0;
			}
		}
	}

	// Relational predicate to determine if the boundary of Line A intersects
	// with the interior of Point B.
	private void boundaryLineInteriorPoint_(int cluster, int id_a, int id_b,
			int cluster_index_a) {
		if (m_matrix[MatrixPredicate.BoundaryInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) != 0 && (clusterParentage & id_b) != 0) {
			int index = m_topo_graph.getClusterUserIndex(cluster,
					cluster_index_a);

			if (index % 2 != 0) {
				m_matrix[MatrixPredicate.BoundaryInterior] = 0;
			}
		}
	}

	// Relational predicate to determine if the boundary of Line A intersects
	// with the exterior of Point B.
	private void boundaryLineExteriorPoint_(int cluster, int id_a, int id_b,
			int cluster_index_a) {
		if (m_matrix[MatrixPredicate.BoundaryExterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) != 0 && (clusterParentage & id_b) == 0) {
			int index = m_topo_graph.getClusterUserIndex(cluster,
					cluster_index_a);

			if (index % 2 != 0) {
				m_matrix[MatrixPredicate.BoundaryExterior] = 0;
			}
		}
	}

	// Relational predicate to determine if the exterior of Line A intersects
	// with the interior of Point B.
	private void exteriorLineInteriorPoint_(int cluster, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.ExteriorInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) == 0 && (clusterParentage & id_b) != 0) {
			m_matrix[MatrixPredicate.ExteriorInterior] = 0;
		}
	}

	// Relational predicate to determine if the interior of Point A intersects
	// with the interior of Point B.
	private void interiorPointInteriorPoint_(int cluster, int id_a, int id_b) {
		if (m_matrix[MatrixPredicate.InteriorInterior] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) != 0 && (clusterParentage & id_b) != 0) {
			m_matrix[MatrixPredicate.InteriorInterior] = 0;
		}
	}

	// Relational predicate to determine if the interior of Point A intersects
	// with the exterior of Point B.
	private void interiorPointExteriorPoint_(int cluster, int id_a, int id_b,
			int predicate) {
		if (m_matrix[predicate] == 0)
			return;

		int clusterParentage = m_topo_graph.getClusterParentage(cluster);

		if ((clusterParentage & id_a) != 0 && (clusterParentage & id_b) == 0) {
			m_matrix[predicate] = 0;
		}
	}

	// Computes the 9 intersection relationships of boundary, interior, and
	// exterior of geometry_a vs geometry_b using the Topo_graph for area/area,
	// area/Line, and Line/Line relations
	private void computeMatrixTopoGraphHalfEdges_(int geometry_a, int geometry_b) {
		boolean bRelationKnown = false;

		int id_a = m_topo_graph.getGeometryID(geometry_a);
		int id_b = m_topo_graph.getGeometryID(geometry_b);

		m_visited_index = m_topo_graph.createUserIndexForHalfEdges();

		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			int first_half_edge = m_topo_graph.getClusterHalfEdge(cluster);
			int next_half_edge = first_half_edge;

			do {
				int half_edge = next_half_edge;
				int visited = m_topo_graph.getHalfEdgeUserIndex(half_edge,
						m_visited_index);

				if (visited != 1) {
					do {
						// Invoke relational predicates
						switch (m_predicates) {
						case Predicates.AreaAreaPredicates:
							bRelationKnown = areaAreaPredicates_(half_edge,
									id_a, id_b);
							break;
						case Predicates.AreaLinePredicates:
							bRelationKnown = areaLinePredicates_(half_edge,
									id_a, id_b);
							break;
						case Predicates.LineLinePredicates:
							bRelationKnown = lineLinePredicates_(half_edge,
									id_a, id_b);
							break;
						default:
							throw new GeometryException("internal error");
						}

						if (bRelationKnown)
							break;

						m_topo_graph.setHalfEdgeUserIndex(half_edge,
								m_visited_index, 1);
						half_edge = m_topo_graph.getHalfEdgeNext(half_edge);
					} while (half_edge != next_half_edge && !bRelationKnown);
				}

				if (bRelationKnown)
					break;

				next_half_edge = m_topo_graph.getHalfEdgeNext(m_topo_graph
						.getHalfEdgeTwin(half_edge));
			} while (next_half_edge != first_half_edge);

			if (bRelationKnown)
				break;
		}

		if (!bRelationKnown)
			setRemainingPredicatesToFalse_();

		m_topo_graph.deleteUserIndexForHalfEdges(m_visited_index);
	}

	// Computes the 9 intersection relationships of boundary, interior, and
	// exterior of geometry_a vs geometry_b using the Topo_graph for area/Point,
	// Line/Point, and Point/Point relations
	private void computeMatrixTopoGraphClusters_(int geometry_a, int geometry_b) {
		boolean bRelationKnown = false;

		int id_a = m_topo_graph.getGeometryID(geometry_a);
		int id_b = m_topo_graph.getGeometryID(geometry_b);

		for (int cluster = m_topo_graph.getFirstCluster(); cluster != -1; cluster = m_topo_graph
				.getNextCluster(cluster)) {
			// Invoke relational predicates
			switch (m_predicates) {
			case Predicates.AreaPointPredicates:
				bRelationKnown = areaPointPredicates_(cluster, id_a, id_b);
				break;
			case Predicates.LinePointPredicates:
				bRelationKnown = linePointPredicates_(cluster, id_a, id_b);
				break;
			case Predicates.PointPointPredicates:
				bRelationKnown = pointPointPredicates_(cluster, id_a, id_b);
				break;
			default:
				throw new GeometryException("internal error");
			}

			if (bRelationKnown)
				break;
		}

		if (!bRelationKnown)
			setRemainingPredicatesToFalse_();
	}

	// Call this method to set the edit shape, if the edit shape has been
	// cracked and clustered already.
	private void setEditShape_(EditShape shape) {
		m_topo_graph.setEditShape(shape, null);
	}

	// Call this method to set the edit shape, if the edit shape has not been
	// cracked and clustered already.
	private void setEditShapeCrackAndCluster_(EditShape shape,
			double tolerance, ProgressTracker progress_tracker) {
		CrackAndCluster.execute(shape, tolerance, progress_tracker);
		for (int geometry = shape.getFirstGeometry(); geometry != -1; geometry = shape
				.getNextGeometry(geometry)) {
			if (shape.getGeometryType(geometry) == Geometry.GeometryType.Polygon)
				Simplificator.execute(shape, geometry, -1);
		}
		setEditShape_(shape);
	}

	// Upgrades the geometry to a feature geometry.
	private static Geometry convertGeometry_(Geometry geometry, double tolerance) {
		int gt = geometry.getType().value();

		if (Geometry.isSegment(gt)) {
			Polyline polyline = new Polyline(geometry.getDescription());
			polyline.addSegment((Segment) geometry, true);
			return polyline;
		}

		if (gt == Geometry.GeometryType.Envelope) {
			Envelope envelope = (Envelope) (geometry);
			Envelope2D env = new Envelope2D();
			geometry.queryEnvelope2D(env);

			if (env.getHeight() <= tolerance && env.getWidth() <= tolerance) {// treat
																				// as
																				// point
				Point point = new Point(geometry.getDescription());
				envelope.getCenter(point);
				return point;
			}

			if (env.getHeight() <= tolerance || env.getWidth() <= tolerance) {// treat
																				// as
																				// line
				Polyline polyline = new Polyline(geometry.getDescription());
				Point p = new Point();
				envelope.queryCornerByVal(0, p);
				polyline.startPath(p);
				envelope.queryCornerByVal(2, p);
				polyline.lineTo(p);
				return polyline;
			}

			// treat as polygon
			Polygon polygon = new Polygon(geometry.getDescription());
			polygon.addEnvelope(envelope, false);
			return polygon;
		}

		return geometry;
	}
}
