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

class RelationalOperationsMatrix {
	private TopoGraph m_topo_graph;
	private int[] m_matrix;
    private int[] m_max_dim;
	private boolean[] m_perform_predicates;
	private String m_scl;
	private int m_predicates_half_edge;
    private int m_predicates_cluster;
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

        if (scl.length() != 9)
            throw new GeometryException("relation string length has to be 9 characters");

        for (int i = 0; i < 9; i++)
        {
            char c = scl.charAt(i);

            if (c != '*' && c != 'T' && c != 'F' && c != '0' && c != '1' && c != '2')
                throw new GeometryException("relation string");
        }

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

        if (_geometryA.isEmpty() || _geometryB.isEmpty())
            return relateEmptyGeometries_(_geometryA, _geometryB, scl);

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
						getTransposeMatrix_(scl), progress_tracker);
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
						(Point) (_geometryA), tolerance,
						getTransposeMatrix_(scl), progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelatePoint_((Polyline) (_geometryB),
						(Point) (_geometryA), tolerance,
						getTransposeMatrix_(scl), progress_tracker);
				break;

			case Geometry.GeometryType.Point:
				bRelation = pointRelatePoint_((Point) (_geometryA),
						(Point) (_geometryB), tolerance, scl, progress_tracker);
				break;

			case Geometry.GeometryType.MultiPoint:
				bRelation = multiPointRelatePoint_((MultiPoint) (_geometryB),
						(Point) (_geometryA), tolerance,
						getTransposeMatrix_(scl), progress_tracker);
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
						getTransposeMatrix_(scl), progress_tracker);
				break;

			case Geometry.GeometryType.Polyline:
				bRelation = polylineRelateMultiPoint_((Polyline) (_geometryB),
						(MultiPoint) (_geometryA), tolerance,
						getTransposeMatrix_(scl), progress_tracker);
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
        m_max_dim = new int[9];
		m_perform_predicates = new boolean[9];
        m_predicates_half_edge = -1;
        m_predicates_cluster =  -1;
	}

	// Returns true if the relation holds.
	static boolean polygonRelatePolygon_(Polygon polygon_a, Polygon polygon_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setAreaAreaPredicates_();

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polygon_b.queryEnvelope2D(env_b);

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(
				env_a, env_b, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.areaAreaDisjointPredicates_(polygon_a, polygon_b);
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			// Quick rasterize test to see whether the the geometries are
			// disjoint, or if one is contained in the other.
			int relation = RelationalOperations
					.tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b,
							tolerance, false);

			if (relation == RelationalOperations.Relation.disjoint) {
				relOps.areaAreaDisjointPredicates_(polygon_a, polygon_b);
				bRelationKnown = true;
			} else if (relation == RelationalOperations.Relation.contains) {
				relOps.areaAreaContainsPredicates_(polygon_b);
				bRelationKnown = true;
			} else if (relation == RelationalOperations.Relation.within) {
				relOps.areaAreaWithinPredicates_(polygon_a);
				bRelationKnown = true;
			}
		}

		if (!bRelationKnown) {
			EditShape edit_shape = new EditShape();
			int geom_a = edit_shape.addGeometry(polygon_a);
			int geom_b = edit_shape.addGeometry(polygon_b);
			relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
					progress_tracker);
			relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
			relOps.m_topo_graph.removeShape();
		}

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

    // The relation is based on the simplified-Polygon A containing Polygon B, which may be non-simple.
    static boolean polygonContainsPolygon_(Polygon polygon_a, Polygon polygon_b, double tolerance, ProgressTracker progress_tracker)
    {
        assert(!polygon_a.isEmpty());
        assert(!polygon_b.isEmpty());

        RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
        relOps.resetMatrix_();
        relOps.setPredicates_("T*****F**");
        relOps.setAreaAreaPredicates_();

        Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
        polygon_a.queryEnvelope2D(env_a);
        polygon_b.queryEnvelope2D(env_b);

        boolean bRelationKnown = false;
        boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(env_a, env_b, tolerance, progress_tracker);

        if (b_disjoint)
        {
            relOps.areaAreaDisjointPredicates_(polygon_a, polygon_b);
            bRelationKnown = true;
        }

        if (!bRelationKnown)
        {
            // Quick rasterize test to see whether the the geometries are disjoint, or if one is contained in the other.
            int relation = RelationalOperations.tryRasterizedContainsOrDisjoint_(polygon_a, polygon_b, tolerance, false);

            if (relation == RelationalOperations.Relation.disjoint)
            {
                relOps.areaAreaDisjointPredicates_(polygon_a, polygon_b);
                bRelationKnown = true;
            }
            else if (relation == RelationalOperations.Relation.contains)
            {
                relOps.areaAreaContainsPredicates_(polygon_b);
                bRelationKnown = true;
            }
            else if (relation == RelationalOperations.Relation.within)
            {
                relOps.areaAreaWithinPredicates_(polygon_a);
                bRelationKnown = true;
            }
        }

        if (bRelationKnown)
        {
            boolean bContains = relationCompare_(relOps.m_matrix, relOps.m_scl);
            return bContains;
        }

        EditShape edit_shape = new EditShape();
        int geom_a = edit_shape.addGeometry(polygon_a);
        int geom_b = edit_shape.addGeometry(polygon_b);

        CrackAndCluster.execute(edit_shape, tolerance, progress_tracker, false);
        Polyline boundary_b = (Polyline)edit_shape.getGeometry(geom_b).getBoundary();
        edit_shape.filterClosePoints(0, true, true);
        Simplificator.execute(edit_shape, geom_a, -1, false, progress_tracker);

        // Make sure Polygon A has exterior
        // If the simplified Polygon A does not have interior, then it cannot contain anything.
        if (edit_shape.getPointCount(geom_a) == 0)
            return false;

        Simplificator.execute(edit_shape, geom_b, -1, false, progress_tracker);

        relOps.setEditShape_(edit_shape, progress_tracker);

        // We see if the simplified Polygon A contains the simplified Polygon B.

        boolean b_empty = edit_shape.getPointCount(geom_b) == 0;

        if (!b_empty)
        {//geom_b has interior
            relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
            relOps.m_topo_graph.removeShape();
            boolean bContains = relationCompare_(relOps.m_matrix, relOps.m_scl);

            if (!bContains)
                return bContains;
        }

        Polygon polygon_simple_a = (Polygon)edit_shape.getGeometry(geom_a);
        edit_shape = new EditShape();
        geom_a = edit_shape.addGeometry(polygon_simple_a);
        geom_b = edit_shape.addGeometry(boundary_b);
        relOps.setEditShape_(edit_shape, progress_tracker);

        // Check no interior lines of the boundary intersect the exterior
        relOps.m_predicate_count = 0;
        relOps.resetMatrix_();
        relOps.setPredicates_(b_empty ? "T*****F**" : "******F**");
        relOps.setAreaLinePredicates_();

        relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
        relOps.m_topo_graph.removeShape();
        boolean bContains = relationCompare_(relOps.m_matrix, relOps.m_scl);
        return bContains;
    }

	// Returns true if the relation holds.
	static boolean polygonRelatePolyline_(Polygon polygon_a,
			Polyline polyline_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setAreaLinePredicates_();

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(
				env_a, env_b, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.areaLineDisjointPredicates_(polygon_a, polyline_b); // passing polyline
															// to get boundary
															// information
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			// Quick rasterize test to see whether the the geometries are
			// disjoint, or if one is contained in the other.
			int relation = RelationalOperations
					.tryRasterizedContainsOrDisjoint_(polygon_a, polyline_b,
							tolerance, false);

			if (relation == RelationalOperations.Relation.disjoint) {
				relOps.areaLineDisjointPredicates_(polygon_a, polyline_b); // passing
																// polyline to
																// get boundary
																// information
				bRelationKnown = true;
			} else if (relation == RelationalOperations.Relation.contains) {
				relOps.areaLineContainsPredicates_(polygon_a, polyline_b); // passing
																// polyline to
																// get boundary
																// information
				bRelationKnown = true;
			}
		}

		if (!bRelationKnown) {
			EditShape edit_shape = new EditShape();
			int geom_a = edit_shape.addGeometry(polygon_a);
			int geom_b = edit_shape.addGeometry(polyline_b);
			relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
					progress_tracker);
			relOps.m_cluster_index_b = relOps.m_topo_graph
					.createUserIndexForClusters();
			markClusterEndPoints_(geom_b, relOps.m_topo_graph,
					relOps.m_cluster_index_b);
			relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
			relOps.m_topo_graph
					.deleteUserIndexForClusters(relOps.m_cluster_index_b);
			relOps.m_topo_graph.removeShape();
		}

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

    static boolean polygonContainsPolyline_(Polygon polygon_a, Polyline polyline_b, double tolerance, ProgressTracker progress_tracker)
    {
        RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
        relOps.resetMatrix_();
        relOps.setPredicates_("T*****F**");
        relOps.setAreaLinePredicates_();

        Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
        polygon_a.queryEnvelope2D(env_a);
        polyline_b.queryEnvelope2D(env_b);

        boolean bRelationKnown = false;
        boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(env_a, env_b, tolerance, progress_tracker);

        if (b_disjoint)
        {
            relOps.areaLineDisjointPredicates_(polygon_a, polyline_b); // passing polyline to get boundary information
            bRelationKnown = true;
        }

        if (!bRelationKnown)
        {
            // Quick rasterize test to see whether the the geometries are disjoint, or if one is contained in the other.
            int relation = RelationalOperations.tryRasterizedContainsOrDisjoint_(polygon_a, polyline_b, tolerance, false);

            if (relation == RelationalOperations.Relation.disjoint)
            {
                relOps.areaLineDisjointPredicates_(polygon_a, polyline_b); // passing polyline to get boundary information
                bRelationKnown = true;
            }
            else if (relation == RelationalOperations.Relation.contains)
            {
                relOps.areaLineContainsPredicates_(polygon_a, polyline_b); // passing polyline to get boundary information
                bRelationKnown = true;
            }
        }

        if (bRelationKnown)
        {
            boolean bContains = relationCompare_(relOps.m_matrix, relOps.m_scl);
            return bContains;
        }

        EditShape edit_shape = new EditShape();
        int geom_a = edit_shape.addGeometry(polygon_a);
        int geom_b = edit_shape.addGeometry(polyline_b);
        relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance, progress_tracker);

        // Make sure Polygon A has exterior
        // If the simplified Polygon A does not have interior, then it cannot contain anything.
        if (edit_shape.getPointCount(geom_a) == 0)
            return false;

        relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
        relOps.m_topo_graph.removeShape();

        boolean bContains = relationCompare_(relOps.m_matrix, relOps.m_scl);
        return bContains;
    }

	// Returns true if the relation holds
	static boolean polygonRelateMultiPoint_(Polygon polygon_a,
			MultiPoint multipoint_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setAreaPointPredicates_();

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polygon_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(
				env_a, env_b, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.areaPointDisjointPredicates_(polygon_a);
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			// Quick rasterize test to see whether the the geometries are
			// disjoint, or if one is contained in the other.
			int relation = RelationalOperations
					.tryRasterizedContainsOrDisjoint_(polygon_a, multipoint_b,
							tolerance, false);

			if (relation == RelationalOperations.Relation.disjoint) {
				relOps.areaPointDisjointPredicates_(polygon_a);
				bRelationKnown = true;
			} else if (relation == RelationalOperations.Relation.contains) {
				relOps.areaPointContainsPredicates_(polygon_a);
				bRelationKnown = true;
			}
		}

		if (!bRelationKnown) {
			EditShape edit_shape = new EditShape();
			int geom_a = edit_shape.addGeometry(polygon_a);
			int geom_b = edit_shape.addGeometry(multipoint_b);
			relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
					progress_tracker);
			relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
			relOps.m_topo_graph.removeShape();
		}

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

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		polyline_b.queryEnvelope2D(env_b);

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(
				env_a, env_b, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.lineLineDisjointPredicates_(polyline_a, polyline_b);
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			// Quick rasterize test to see whether the the geometries are
			// disjoint, or if one is contained in the other.
			int relation = RelationalOperations
					.tryRasterizedContainsOrDisjoint_(polyline_a, polyline_b,
							tolerance, false);

			if (relation == RelationalOperations.Relation.disjoint) {
				relOps.lineLineDisjointPredicates_(polyline_a, polyline_b);
				bRelationKnown = true;
			}
		}

		if (!bRelationKnown) {
			EditShape edit_shape = new EditShape();
			int geom_a = edit_shape.addGeometry(polyline_a);
			int geom_b = edit_shape.addGeometry(polyline_b);
			relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
					progress_tracker);
			relOps.m_cluster_index_a = relOps.m_topo_graph
					.createUserIndexForClusters();
			relOps.m_cluster_index_b = relOps.m_topo_graph
					.createUserIndexForClusters();
			markClusterEndPoints_(geom_a, relOps.m_topo_graph,
					relOps.m_cluster_index_a);
			markClusterEndPoints_(geom_b, relOps.m_topo_graph,
					relOps.m_cluster_index_b);
			relOps.computeMatrixTopoGraphHalfEdges_(geom_a, geom_b);
			relOps.m_topo_graph
					.deleteUserIndexForClusters(relOps.m_cluster_index_a);
			relOps.m_topo_graph
					.deleteUserIndexForClusters(relOps.m_cluster_index_b);
			relOps.m_topo_graph.removeShape();
		}

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

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		polyline_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(
				env_a, env_b, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.linePointDisjointPredicates_(polyline_a);
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			// Quick rasterize test to see whether the the geometries are
			// disjoint, or if one is contained in the other.
			int relation = RelationalOperations
					.tryRasterizedContainsOrDisjoint_(polyline_a, multipoint_b,
							tolerance, false);

			if (relation == RelationalOperations.Relation.disjoint) {
				relOps.linePointDisjointPredicates_(polyline_a);
				bRelationKnown = true;
			}
		}

		if (!bRelationKnown) {
			EditShape edit_shape = new EditShape();
			int geom_a = edit_shape.addGeometry(polyline_a);
			int geom_b = edit_shape.addGeometry(multipoint_b);
			relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
					progress_tracker);
			relOps.m_cluster_index_a = relOps.m_topo_graph
					.createUserIndexForClusters();
			markClusterEndPoints_(geom_a, relOps.m_topo_graph,
					relOps.m_cluster_index_a);
			relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
			relOps.m_topo_graph
					.deleteUserIndexForClusters(relOps.m_cluster_index_a);
			relOps.m_topo_graph.removeShape();
		}

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

		Envelope2D env_a = new Envelope2D(), env_b = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		multipoint_b.queryEnvelope2D(env_b);

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.envelopeDisjointEnvelope_(
				env_a, env_b, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.pointPointDisjointPredicates_();
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			EditShape edit_shape = new EditShape();
			int geom_a = edit_shape.addGeometry(multipoint_a);
			int geom_b = edit_shape.addGeometry(multipoint_b);
			relOps.setEditShapeCrackAndCluster_(edit_shape, tolerance,
					progress_tracker);
			relOps.computeMatrixTopoGraphClusters_(geom_a, geom_b);
			relOps.m_topo_graph.removeShape();
		}

		boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polygonRelatePoint_(Polygon polygon_a, Point point_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
        RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
        relOps.resetMatrix_();
        relOps.setPredicates_(scl);
        relOps.setAreaPointPredicates_();

        Envelope2D env_a = new Envelope2D();
        polygon_a.queryEnvelope2D(env_a);
        Point2D pt_b = point_b.getXY();

        boolean bRelationKnown = false;
        boolean b_disjoint = RelationalOperations.pointDisjointEnvelope_(pt_b, env_a, tolerance, progress_tracker);

        if (b_disjoint)
        {
            relOps.areaPointDisjointPredicates_(polygon_a);
            bRelationKnown = true;
        }

        if (!bRelationKnown)
        {
            PolygonUtils.PiPResult res = PolygonUtils.isPointInPolygon2D(polygon_a, pt_b, tolerance); // uses accelerator

            if (res == PolygonUtils.PiPResult.PiPInside)
            {// polygon must have area
                relOps.m_matrix[MatrixPredicate.InteriorInterior] = 0;
                relOps.m_matrix[MatrixPredicate.InteriorExterior] = 2;
                relOps.m_matrix[MatrixPredicate.BoundaryInterior] = -1;
                relOps.m_matrix[MatrixPredicate.BoundaryExterior] = 1;
                relOps.m_matrix[MatrixPredicate.ExteriorInterior] = -1;
            }
            else if (res == PolygonUtils.PiPResult.PiPBoundary)
            {
                relOps.m_matrix[MatrixPredicate.ExteriorInterior] = -1;

                double area = polygon_a.calculateArea2D();

                if (area != 0)
                {
                    relOps.m_matrix[MatrixPredicate.InteriorInterior] = -1;
                    relOps.m_matrix[MatrixPredicate.BoundaryInterior] = 0;
                    relOps.m_matrix[MatrixPredicate.InteriorExterior] = 2;
                    relOps.m_matrix[MatrixPredicate.BoundaryExterior] = 1;
                }
                else
                {
                    relOps.m_matrix[MatrixPredicate.InteriorInterior] = 0;
                    relOps.m_matrix[MatrixPredicate.BoundaryInterior] = -1;
                    relOps.m_matrix[MatrixPredicate.BoundaryExterior] = -1;

                    Envelope2D env = new Envelope2D();
                    polygon_a.queryEnvelope2D(env);
                    relOps.m_matrix[MatrixPredicate.InteriorExterior] = (env.getHeight() == 0.0 && env.getWidth() == 0.0 ? -1 : 1);
                }
            }
            else
            {
                relOps.areaPointDisjointPredicates_(polygon_a);
            }
        }

        boolean bRelation = relationCompare_(relOps.m_matrix, scl);
        return bRelation;
	}

	// Returns true if the relation holds.
	static boolean polylineRelatePoint_(Polyline polyline_a, Point point_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
        RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
        relOps.resetMatrix_();
        relOps.setPredicates_(scl);
        relOps.setLinePointPredicates_();

        Envelope2D env_a = new Envelope2D();
        polyline_a.queryEnvelope2D(env_a);
        Point2D pt_b = point_b.getXY();

        boolean bRelationKnown = false;
        boolean b_disjoint = RelationalOperations.pointDisjointEnvelope_(pt_b, env_a, tolerance, progress_tracker);

        if (b_disjoint)
        {
            relOps.linePointDisjointPredicates_(polyline_a);
            bRelationKnown = true;
        }

        if (!bRelationKnown)
        {
            MultiPoint boundary_a = null;
            boolean b_boundary_contains_point_known = false;
            boolean b_boundary_contains_point = false;

            if (relOps.m_perform_predicates[MatrixPredicate.InteriorInterior] || relOps.m_perform_predicates[MatrixPredicate.ExteriorInterior])
            {
                boolean b_intersects = RelationalOperations.linearPathIntersectsPoint_(polyline_a, pt_b, tolerance);

                if (b_intersects)
                {
                    if (relOps.m_perform_predicates[MatrixPredicate.InteriorInterior])
                    {
                        boundary_a = (MultiPoint)Boundary.calculate(polyline_a, progress_tracker);
                        b_boundary_contains_point = !RelationalOperations.multiPointDisjointPointImpl_(boundary_a, pt_b, tolerance, progress_tracker);
                        b_boundary_contains_point_known = true;

                        if (b_boundary_contains_point)
                            relOps.m_matrix[MatrixPredicate.InteriorInterior] = -1;
                        else
                            relOps.m_matrix[MatrixPredicate.InteriorInterior] = 0;
                    }

                    relOps.m_matrix[MatrixPredicate.ExteriorInterior] = -1;
                }
                else
                {
                    relOps.m_matrix[MatrixPredicate.InteriorInterior] = -1;
                    relOps.m_matrix[MatrixPredicate.ExteriorInterior] = 0;
                }
            }

            if (relOps.m_perform_predicates[MatrixPredicate.BoundaryInterior])
            {
                if (boundary_a != null && boundary_a.isEmpty())
                {
                    relOps.m_matrix[MatrixPredicate.BoundaryInterior] = -1;
                }
                else
                {
                    if (!b_boundary_contains_point_known)
                    {
                        if (boundary_a == null)
                            boundary_a = (MultiPoint)Boundary.calculate(polyline_a, progress_tracker);

                        b_boundary_contains_point = !RelationalOperations.multiPointDisjointPointImpl_(boundary_a, pt_b, tolerance, progress_tracker);
                        b_boundary_contains_point_known = true;
                    }

                    relOps.m_matrix[MatrixPredicate.BoundaryInterior] = (b_boundary_contains_point ? 0 : -1);
                }
            }

            if (relOps.m_perform_predicates[MatrixPredicate.BoundaryExterior])
            {
                if (boundary_a != null && boundary_a.isEmpty())
                {
                    relOps.m_matrix[MatrixPredicate.BoundaryExterior] = -1;
                }
                else
                {
                    if (b_boundary_contains_point_known && !b_boundary_contains_point)
                    {
                        relOps.m_matrix[MatrixPredicate.BoundaryExterior] = 0;
                    }
                    else
                    {
                        if (boundary_a == null)
                            boundary_a = (MultiPoint)Boundary.calculate(polyline_a, progress_tracker);

                        boolean b_boundary_equals_point = RelationalOperations.multiPointEqualsPoint_(boundary_a, point_b, tolerance, progress_tracker);
                        relOps.m_matrix[MatrixPredicate.BoundaryExterior] = (b_boundary_equals_point ? -1 : 0);
                    }
                }
            }

            if (relOps.m_perform_predicates[MatrixPredicate.InteriorExterior])
            {
                boolean b_has_length = polyline_a.calculateLength2D() != 0;

                if (b_has_length)
                {
                    relOps.m_matrix[MatrixPredicate.InteriorExterior] = 1;
                }
                else
                {
                    // all points are interior
                    MultiPoint interior_a = new MultiPoint(polyline_a.getDescription());
                    interior_a.add(polyline_a, 0, polyline_a.getPointCount());
                    boolean b_interior_equals_point = RelationalOperations.multiPointEqualsPoint_(interior_a, point_b, tolerance, progress_tracker);
                    relOps.m_matrix[MatrixPredicate.InteriorExterior] = (b_interior_equals_point ? -1 : 0);
                }
            }
        }

        boolean bRelation = relationCompare_(relOps.m_matrix, relOps.m_scl);
        return bRelation;
	}

	// Returns true if the relation holds.
	static boolean multiPointRelatePoint_(MultiPoint multipoint_a,
			Point point_b, double tolerance, String scl,
			ProgressTracker progress_tracker) {
		RelationalOperationsMatrix relOps = new RelationalOperationsMatrix();
		relOps.resetMatrix_();
		relOps.setPredicates_(scl);
		relOps.setPointPointPredicates_();

		Envelope2D env_a = new Envelope2D();
		multipoint_a.queryEnvelope2D(env_a);
		Point2D pt_b = point_b.getXY();

		boolean bRelationKnown = false;
		boolean b_disjoint = RelationalOperations.pointDisjointEnvelope_(pt_b,
				env_a, tolerance, progress_tracker);

		if (b_disjoint) {
			relOps.pointPointDisjointPredicates_();
			bRelationKnown = true;
		}

		if (!bRelationKnown) {
			boolean b_intersects = false;
			boolean b_multipoint_contained = true;
			double tolerance_sq = tolerance * tolerance;

			for (int i = 0; i < multipoint_a.getPointCount(); i++) {
				Point2D pt_a = multipoint_a.getXY(i);

				if (Point2D.sqrDistance(pt_a, pt_b) <= tolerance_sq)
					b_intersects = true;
				else
					b_multipoint_contained = false;

				if (b_intersects && !b_multipoint_contained)
					break;
			}

			if (b_intersects) {
				relOps.m_matrix[MatrixPredicate.InteriorInterior] = 0;

				if (!b_multipoint_contained)
					relOps.m_matrix[MatrixPredicate.InteriorExterior] = 0;
				else
					relOps.m_matrix[MatrixPredicate.InteriorExterior] = -1;

				relOps.m_matrix[MatrixPredicate.ExteriorInterior] = -1;
			} else {
				relOps.m_matrix[MatrixPredicate.InteriorInterior] = -1;
				relOps.m_matrix[MatrixPredicate.InteriorExterior] = 0;
				relOps.m_matrix[MatrixPredicate.ExteriorInterior] = 0;
			}
		}

		boolean bRelation = relationCompare_(relOps.m_matrix, scl);
		return bRelation;
	}

	// Returns true if the relation holds.
	static boolean pointRelatePoint_(Point point_a, Point point_b,
			double tolerance, String scl, ProgressTracker progress_tracker) {
		Point2D pt_a = point_a.getXY();
		Point2D pt_b = point_b.getXY();
		int[] matrix = new int[9];

		for (int i = 0; i < 9; i++)
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

			default:
				break;
			}
		}

		return true;
	}

    static boolean relateEmptyGeometries_(Geometry geometry_a, Geometry geometry_b, String scl)
    {
        int[] matrix = new int[9];

        if (geometry_a.isEmpty() && geometry_b.isEmpty())
        {
            for (int i = 0; i < 9; i++)
                matrix[i] = -1;

            return relationCompare_(matrix, scl);
        }

        boolean b_transpose = false;

        Geometry g_a;
        Geometry g_b;

        if (!geometry_a.isEmpty())
        {
            g_a = geometry_a;
            g_b = geometry_b;
        }
        else
        {
            g_a = geometry_b;
            g_b = geometry_a;
            b_transpose = true;
        }

        matrix[MatrixPredicate.InteriorInterior] = -1;
        matrix[MatrixPredicate.InteriorBoundary] = -1;
        matrix[MatrixPredicate.BoundaryInterior] = -1;
        matrix[MatrixPredicate.BoundaryBoundary] = -1;
        matrix[MatrixPredicate.ExteriorInterior] = -1;
        matrix[MatrixPredicate.ExteriorBoundary] = -1;

        matrix[MatrixPredicate.ExteriorExterior] = 2;

        int type = g_a.getType().value();

        if (Geometry.isMultiPath(type))
        {
            if (type == Geometry.GeometryType.Polygon)
            {
                double area = ((Polygon)g_a).calculateArea2D();

                if (area != 0)
                {
                    matrix[MatrixPredicate.InteriorExterior] = 2;
                    matrix[MatrixPredicate.BoundaryExterior] = 1;
                }
                else
                {
                    matrix[MatrixPredicate.BoundaryExterior] = -1;

                    Envelope2D env = new Envelope2D();
                    g_a.queryEnvelope2D(env);
                    matrix[MatrixPredicate.InteriorExterior] = (env.getHeight() == 0.0 && env.getWidth() == 0.0 ? 0 : 1);
                }
            }
            else
            {
                boolean b_has_length = ((Polyline)g_a).calculateLength2D() != 0;
                matrix[MatrixPredicate.InteriorExterior] = (b_has_length ? 1 : 0);
                matrix[MatrixPredicate.BoundaryExterior] = (Boundary.hasNonEmptyBoundary((Polyline)g_a, null) ? 0 : -1);
            }
        }
        else
        {
            matrix[MatrixPredicate.InteriorExterior] = 0;
            matrix[MatrixPredicate.BoundaryExterior] = -1;
        }

        if (b_transpose)
            transposeMatrix_(matrix);

        return relationCompare_(matrix, scl);
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
	private static void markClusterEndPoints_(int geometry,
			TopoGraph topoGraph, int clusterIndex) {
        int id = topoGraph.getGeometryID(geometry);

        for (int cluster = topoGraph.getFirstCluster(); cluster != -1; cluster = topoGraph.getNextCluster(cluster))
        {
            int cluster_parentage = topoGraph.getClusterParentage(cluster);

            if ((cluster_parentage & id) == 0)
                continue;

            int first_half_edge = topoGraph.getClusterHalfEdge(cluster);
            if (first_half_edge == -1)
            {
                topoGraph.setClusterUserIndex(cluster, clusterIndex, 0);
                continue;
            }

            int next_half_edge = first_half_edge;
            int index = 0;

            do
            {
                int half_edge = next_half_edge;
                int half_edge_parentage = topoGraph.getHalfEdgeParentage(half_edge);

                if ((half_edge_parentage & id) != 0)
                    index++;

                next_half_edge = topoGraph.getHalfEdgeNext(topoGraph.getHalfEdgeTwin(half_edge));

            } while (next_half_edge != first_half_edge);

            topoGraph.setClusterUserIndex(cluster, clusterIndex, index);
        }

        return;
	}

	private static String getTransposeMatrix_(String scl) {
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
        {
            m_matrix[i] = -2;
            m_max_dim[i] = -2;
        }
	}

	private static void transposeMatrix_(int[] matrix) {
		int temp1 = matrix[1];
		int temp2 = matrix[2];
		int temp5 = matrix[5];

		matrix[1] = matrix[3];
		matrix[2] = matrix[6];
		matrix[5] = matrix[7];

		matrix[3] = temp1;
		matrix[6] = temp2;
		matrix[7] = temp5;
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
	private boolean isPredicateKnown_(int predicate) {
        assert(m_scl.charAt(predicate) != '*');

        if (m_matrix[predicate] == -2)
            return false;

        if (m_matrix[predicate] == -1)
        {
            m_perform_predicates[predicate] = false;
            m_predicate_count--;
            return true;
        }

        if (m_scl.charAt(predicate) != 'T' && m_scl.charAt(predicate) != 'F')
        {
            if (m_matrix[predicate] < m_max_dim[predicate])
            {
                return false;
            }
            else
            {
                m_perform_predicates[predicate] = false;
                m_predicate_count--;
                return true;
            }
        }
        else
        {
            m_perform_predicates[predicate] = false;
            m_predicate_count--;
            return true;
        }
	}

	// Sets the area-area predicates function.
	private void setAreaAreaPredicates_() {
        m_predicates_half_edge = Predicates.AreaAreaPredicates;

        m_max_dim[MatrixPredicate.InteriorInterior] = 2;
        m_max_dim[MatrixPredicate.InteriorBoundary] = 1;
        m_max_dim[MatrixPredicate.InteriorExterior] = 2;
        m_max_dim[MatrixPredicate.BoundaryInterior] = 1;
        m_max_dim[MatrixPredicate.BoundaryBoundary] = 1;
        m_max_dim[MatrixPredicate.BoundaryExterior] = 1;
        m_max_dim[MatrixPredicate.ExteriorInterior] = 2;
        m_max_dim[MatrixPredicate.ExteriorBoundary] = 1;
        m_max_dim[MatrixPredicate.ExteriorExterior] = 2;

        // set predicates that are always true/false
        if (m_perform_predicates[MatrixPredicate.ExteriorExterior])
        {
            m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
            m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
            m_predicate_count--;
        }
	}

	// Sets the area-line predicate function.
	private void setAreaLinePredicates_() {
        m_predicates_half_edge = Predicates.AreaLinePredicates;
        m_predicates_cluster = Predicates.AreaPointPredicates;

        m_max_dim[MatrixPredicate.InteriorInterior] = 1;
        m_max_dim[MatrixPredicate.InteriorBoundary] = 0;
        m_max_dim[MatrixPredicate.InteriorExterior] = 2;
        m_max_dim[MatrixPredicate.BoundaryInterior] = 1;
        m_max_dim[MatrixPredicate.BoundaryBoundary] = 0;
        m_max_dim[MatrixPredicate.BoundaryExterior] = 1;
        m_max_dim[MatrixPredicate.ExteriorInterior] = 1;
        m_max_dim[MatrixPredicate.ExteriorBoundary] = 0;
        m_max_dim[MatrixPredicate.ExteriorExterior] = 2;

        if (m_perform_predicates[MatrixPredicate.ExteriorExterior])
        {
            m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
            m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
            m_predicate_count--;
        }
	}

	// Sets the line-line predicates function.
	private void setLineLinePredicates_() {
        m_predicates_half_edge = Predicates.LineLinePredicates;
        m_predicates_cluster = Predicates.LinePointPredicates;

        m_max_dim[MatrixPredicate.InteriorInterior] = 1;
        m_max_dim[MatrixPredicate.InteriorBoundary] = 0;
        m_max_dim[MatrixPredicate.InteriorExterior] = 1;
        m_max_dim[MatrixPredicate.BoundaryInterior] = 0;
        m_max_dim[MatrixPredicate.BoundaryBoundary] = 0;
        m_max_dim[MatrixPredicate.BoundaryExterior] = 0;
        m_max_dim[MatrixPredicate.ExteriorInterior] = 1;
        m_max_dim[MatrixPredicate.ExteriorBoundary] = 0;
        m_max_dim[MatrixPredicate.ExteriorExterior] = 2;

        // set predicates that are always true/false
        if (m_perform_predicates[MatrixPredicate.ExteriorExterior])
        {
            m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
            m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
            m_predicate_count--;
        }
	}

	// Sets the area-point predicate function.
	private void setAreaPointPredicates_() {
        m_predicates_cluster = Predicates.AreaPointPredicates;

        m_max_dim[MatrixPredicate.InteriorInterior] = 0;
        m_max_dim[MatrixPredicate.InteriorBoundary] = -1;
        m_max_dim[MatrixPredicate.InteriorExterior] = 2;
        m_max_dim[MatrixPredicate.BoundaryInterior] = 0;
        m_max_dim[MatrixPredicate.BoundaryBoundary] = -1;
        m_max_dim[MatrixPredicate.BoundaryExterior] = 1;
        m_max_dim[MatrixPredicate.ExteriorInterior] = 0;
        m_max_dim[MatrixPredicate.ExteriorBoundary] = -1;
        m_max_dim[MatrixPredicate.ExteriorExterior] = 2;

        // set predicates that are always true/false
        if (m_perform_predicates[MatrixPredicate.InteriorBoundary])
        {
            m_matrix[MatrixPredicate.InteriorBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.InteriorBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryBoundary])
        {
            m_matrix[MatrixPredicate.BoundaryBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.BoundaryBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            m_matrix[MatrixPredicate.ExteriorBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.ExteriorBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorExterior])
        {
            m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
            m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
            m_predicate_count--;
        }
	}

	// Sets the line-point predicates function.
	private void setLinePointPredicates_() {
        m_predicates_cluster = Predicates.LinePointPredicates;

        m_max_dim[MatrixPredicate.InteriorInterior] = 0;
        m_max_dim[MatrixPredicate.InteriorBoundary] = -1;
        m_max_dim[MatrixPredicate.InteriorExterior] = 1;
        m_max_dim[MatrixPredicate.BoundaryInterior] = 0;
        m_max_dim[MatrixPredicate.BoundaryBoundary] = -1;
        m_max_dim[MatrixPredicate.BoundaryExterior] = 0;
        m_max_dim[MatrixPredicate.ExteriorInterior] = 0;
        m_max_dim[MatrixPredicate.ExteriorBoundary] = -1;
        m_max_dim[MatrixPredicate.ExteriorExterior] = 2;

        // set predicates that are always true/false
        if (m_perform_predicates[MatrixPredicate.InteriorBoundary])
        {
            m_matrix[MatrixPredicate.InteriorBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.InteriorBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryBoundary])
        {
            m_matrix[MatrixPredicate.BoundaryBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.BoundaryBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            m_matrix[MatrixPredicate.ExteriorBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.ExteriorBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorExterior])
        {
            m_matrix[MatrixPredicate.ExteriorExterior] = 2; // Always true
            m_perform_predicates[MatrixPredicate.ExteriorExterior] = false;
            m_predicate_count--;
        }
	}

	// Sets the point-point predicates function.
	private void setPointPointPredicates_() {
        m_predicates_cluster = Predicates.PointPointPredicates;

        m_max_dim[MatrixPredicate.InteriorInterior] = 0;
        m_max_dim[MatrixPredicate.InteriorBoundary] = -1;
        m_max_dim[MatrixPredicate.InteriorExterior] = 0;
        m_max_dim[MatrixPredicate.BoundaryInterior] = -1;
        m_max_dim[MatrixPredicate.BoundaryBoundary] = -1;
        m_max_dim[MatrixPredicate.BoundaryExterior] = -1;
        m_max_dim[MatrixPredicate.ExteriorInterior] = 0;
        m_max_dim[MatrixPredicate.ExteriorBoundary] = -1;
        m_max_dim[MatrixPredicate.ExteriorExterior] = 2;

        // set predicates that are always true/false
        if (m_perform_predicates[MatrixPredicate.InteriorBoundary])
        {
            m_matrix[MatrixPredicate.InteriorBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.InteriorBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryInterior])
        {
            m_matrix[MatrixPredicate.BoundaryInterior] = -1; // Always false
            m_perform_predicates[MatrixPredicate.BoundaryInterior] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryBoundary])
        {
            m_matrix[MatrixPredicate.BoundaryBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.BoundaryBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            m_matrix[MatrixPredicate.BoundaryExterior] = -1; // Always false
            m_perform_predicates[MatrixPredicate.BoundaryExterior] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            m_matrix[MatrixPredicate.ExteriorBoundary] = -1; // Always false
            m_perform_predicates[MatrixPredicate.ExteriorBoundary] = false;
            m_predicate_count--;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorExterior])
        {
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
					MatrixPredicate.InteriorInterior);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorBoundary]) {
			interiorAreaBoundaryArea_(half_edge, id_a,
					MatrixPredicate.InteriorBoundary);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorBoundary);
		}

		if (m_perform_predicates[MatrixPredicate.InteriorExterior]) {
			interiorAreaExteriorArea_(half_edge, id_a, id_b,
					MatrixPredicate.InteriorExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.InteriorExterior);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryInterior]) {
			interiorAreaBoundaryArea_(half_edge, id_b,
					MatrixPredicate.BoundaryInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryInterior);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryBoundary]) {
			boundaryAreaBoundaryArea_(half_edge, id_a, id_b);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryBoundary);
		}

		if (m_perform_predicates[MatrixPredicate.BoundaryExterior]) {
			boundaryAreaExteriorArea_(half_edge, id_a, id_b,
					MatrixPredicate.BoundaryExterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.BoundaryExterior);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorInterior]) {
			interiorAreaExteriorArea_(half_edge, id_b, id_a,
					MatrixPredicate.ExteriorInterior);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorInterior);
		}

		if (m_perform_predicates[MatrixPredicate.ExteriorBoundary]) {
			boundaryAreaExteriorArea_(half_edge, id_b, id_a,
					MatrixPredicate.ExteriorBoundary);
			bRelationKnown &= isPredicateKnown_(
					MatrixPredicate.ExteriorBoundary);
		}

		return bRelationKnown;
	}

	private void areaAreaDisjointPredicates_(Polygon polygon_a, Polygon polygon_b) {
        m_matrix[MatrixPredicate.InteriorInterior] = -1;
        m_matrix[MatrixPredicate.InteriorBoundary] = -1;
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryBoundary] = -1;

        areaGeomContainsOrDisjointPredicates_(polygon_a, m_perform_predicates[MatrixPredicate.InteriorExterior] ? MatrixPredicate.InteriorExterior : -1, m_scl.charAt(MatrixPredicate.InteriorExterior), m_perform_predicates[MatrixPredicate.BoundaryExterior] ? MatrixPredicate.BoundaryExterior : -1, m_scl.charAt(MatrixPredicate.BoundaryExterior));
        areaGeomContainsOrDisjointPredicates_(polygon_b, m_perform_predicates[MatrixPredicate.ExteriorInterior] ? MatrixPredicate.ExteriorInterior : -1, m_scl.charAt(MatrixPredicate.ExteriorInterior), m_perform_predicates[MatrixPredicate.ExteriorBoundary] ? MatrixPredicate.ExteriorBoundary : -1, m_scl.charAt(MatrixPredicate.ExteriorBoundary));
	}

    private void areaGeomContainsOrDisjointPredicates_(Polygon polygon, int matrix_interior, char c1, int matrix_boundary, char c2)
    {
        if (matrix_interior != -1 || matrix_boundary != -1)
        {
            boolean has_area = ((c1 != 'T' && c1 != 'F' && matrix_interior != -1) || (c2 != 'T' && c2 != 'F' && matrix_boundary != -1) ? polygon.calculateArea2D() != 0 : true);

            if (has_area)
            {
                if (matrix_interior != -1)
                  m_matrix[matrix_interior] = 2;
                if (matrix_boundary != -1)
                  m_matrix[matrix_boundary] = 1;
            }
            else
            {
                if (matrix_boundary != -1)
                  m_matrix[matrix_boundary] = -1;

                if (matrix_interior != -1)
                {
                    Envelope2D env = new Envelope2D();
                    polygon.queryEnvelope2D(env);
                    m_matrix[matrix_interior] = (env.getHeight() == 0.0 && env.getWidth() == 0.0 ? 0 : 1);
                }
            }
        }
    }

	private void areaAreaContainsPredicates_(Polygon polygon_b) {
        m_matrix[MatrixPredicate.InteriorExterior] = 2; // im assuming its a proper contains.
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryBoundary] = -1;
        m_matrix[MatrixPredicate.BoundaryExterior] = 1;
        m_matrix[MatrixPredicate.ExteriorInterior] = -1;
        m_matrix[MatrixPredicate.ExteriorBoundary] = -1;

        areaGeomContainsOrDisjointPredicates_(polygon_b, m_perform_predicates[MatrixPredicate.InteriorInterior] ? MatrixPredicate.InteriorInterior : -1, m_scl.charAt(MatrixPredicate.InteriorInterior), m_perform_predicates[MatrixPredicate.InteriorBoundary] ? MatrixPredicate.InteriorBoundary : -1, m_scl.charAt(MatrixPredicate.InteriorBoundary));

        // all other predicates should already be set by set_area_area_predicates
    }

	private void areaAreaWithinPredicates_(Polygon polygon_a) {
		areaAreaContainsPredicates_(polygon_a);
		transposeMatrix_(m_matrix);
	}

	private void areaLineDisjointPredicates_(Polygon polygon, Polyline polyline) {
        m_matrix[MatrixPredicate.InteriorInterior] = -1;
        m_matrix[MatrixPredicate.InteriorBoundary] = -1;
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryBoundary] = -1;

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            char c = m_scl.charAt(MatrixPredicate.ExteriorInterior);
            boolean b_has_length = (c != 'T' && c != 'F' ? polyline.calculateLength2D() != 0 : true);
            m_matrix[MatrixPredicate.ExteriorInterior] = (b_has_length ? 1 : 0);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            boolean has_non_empty_boundary = Boundary.hasNonEmptyBoundary(polyline, null);
            m_matrix[MatrixPredicate.ExteriorBoundary] = has_non_empty_boundary ? 0 : -1;
        }

        areaGeomContainsOrDisjointPredicates_(polygon, m_perform_predicates[MatrixPredicate.InteriorExterior] ? MatrixPredicate.InteriorExterior : -1, m_scl.charAt(MatrixPredicate.InteriorExterior), m_perform_predicates[MatrixPredicate.BoundaryExterior] ? MatrixPredicate.BoundaryExterior : -1, m_scl.charAt(MatrixPredicate.BoundaryExterior));
	}

	private void areaLineContainsPredicates_(Polygon polygon, Polyline polyline) {
        if (m_perform_predicates[MatrixPredicate.InteriorInterior])
        {
            char c = m_scl.charAt(MatrixPredicate.InteriorInterior);
            boolean b_has_length = (c != 'T' && c != 'F' ? polyline.calculateLength2D() != 0 : true);
            m_matrix[MatrixPredicate.InteriorInterior] = (b_has_length ? 1 : 0);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorBoundary])
        {
            boolean has_non_empty_boundary = Boundary.hasNonEmptyBoundary(polyline, null);
            m_matrix[MatrixPredicate.InteriorBoundary] = has_non_empty_boundary ? 0 : -1;
        }

        m_matrix[MatrixPredicate.InteriorExterior] = 2; //assume polygon has area
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryBoundary] = -1;
        m_matrix[MatrixPredicate.BoundaryExterior] = 1; //assume polygon has area
        m_matrix[MatrixPredicate.ExteriorInterior] = -1;
        m_matrix[MatrixPredicate.ExteriorBoundary] = -1;
	}

	private void areaPointDisjointPredicates_(Polygon polygon) {
        m_matrix[MatrixPredicate.InteriorInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.ExteriorInterior] = 0;

        areaGeomContainsOrDisjointPredicates_(polygon, m_perform_predicates[MatrixPredicate.InteriorExterior] ? MatrixPredicate.InteriorExterior : -1, m_scl.charAt(MatrixPredicate.InteriorExterior), m_perform_predicates[MatrixPredicate.BoundaryExterior] ? MatrixPredicate.BoundaryExterior : -1, m_scl.charAt(MatrixPredicate.BoundaryExterior));
	}

	private void areaPointContainsPredicates_(Polygon polygon) {
        m_matrix[MatrixPredicate.InteriorInterior] = 0;
        m_matrix[MatrixPredicate.InteriorExterior] = 2; //assume polygon has area
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryExterior] = 1; //assume polygon has area
        m_matrix[MatrixPredicate.ExteriorInterior] = -1;
    }

	private void lineLineDisjointPredicates_(Polyline polyline_a,
			Polyline polyline_b) {
        m_matrix[MatrixPredicate.InteriorInterior] = -1;
        m_matrix[MatrixPredicate.InteriorBoundary] = -1;
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryBoundary] = -1;

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            char c = m_scl.charAt(MatrixPredicate.InteriorExterior);
            boolean b_has_length = (c != 'T' && c != 'F' ? polyline_a.calculateLength2D() != 0 : true);
            m_matrix[MatrixPredicate.InteriorExterior] = (b_has_length ? 1 : 0);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            boolean has_non_empty_boundary_a = Boundary.hasNonEmptyBoundary(polyline_a, null);
            m_matrix[MatrixPredicate.BoundaryExterior] = has_non_empty_boundary_a ? 0 : -1;
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            char c = m_scl.charAt(MatrixPredicate.ExteriorInterior);
            boolean b_has_length = (c != 'T' && c != 'F' ? polyline_b.calculateLength2D() != 0 : true);
            m_matrix[MatrixPredicate.ExteriorInterior] = (b_has_length ? 1 : 0);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            boolean has_non_empty_boundary_b = Boundary.hasNonEmptyBoundary(polyline_b, null);
            m_matrix[MatrixPredicate.ExteriorBoundary] = has_non_empty_boundary_b ? 0 : -1;
        }
	}

	private void linePointDisjointPredicates_(Polyline polyline) {
        m_matrix[MatrixPredicate.InteriorInterior] = -1;
        m_matrix[MatrixPredicate.BoundaryInterior] = -1;

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            char c = m_scl.charAt(MatrixPredicate.InteriorExterior);
            boolean b_has_length = (c != 'T' && c != 'F' ? polyline.calculateLength2D() != 0 : true);
            m_matrix[MatrixPredicate.InteriorExterior] = (b_has_length ? 1 : 0);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            boolean has_non_empty_boundary = Boundary.hasNonEmptyBoundary(polyline, null);
            m_matrix[MatrixPredicate.BoundaryExterior] = (has_non_empty_boundary ? 0 : -1);
        }

        m_matrix[MatrixPredicate.ExteriorInterior] = 0;
	}

	private void pointPointDisjointPredicates_() {
		m_matrix[MatrixPredicate.InteriorInterior] = -1;
		m_matrix[MatrixPredicate.InteriorExterior] = 0;
		m_matrix[MatrixPredicate.ExteriorInterior] = 0;
	}

	// Invokes the 9 relational predicates of area vs Line.
	private boolean areaLinePredicates_(int half_edge, int id_a, int id_b) {
        boolean bRelationKnown = true;

        if (m_perform_predicates[MatrixPredicate.InteriorInterior])
        {
            interiorAreaInteriorLine_(half_edge, id_a, id_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorBoundary])
        {
            interiorAreaBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorBoundary);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            interiorAreaExteriorLine_(half_edge, id_a, id_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorExterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryInterior])
        {
            boundaryAreaInteriorLine_(half_edge, id_a, id_b, m_cluster_index_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryInterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryBoundary])
        {
            boundaryAreaBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryBoundary);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            boundaryAreaExteriorLine_(half_edge, id_a, id_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryExterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            exteriorAreaInteriorLine_(half_edge, id_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            exteriorAreaBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorBoundary);
        }

        return bRelationKnown;
	}

	// Invokes the 9 relational predicates of Line vs Line.
	private boolean lineLinePredicates_(int half_edge, int id_a, int id_b) {
        boolean bRelationKnown = true;

        if (m_perform_predicates[MatrixPredicate.InteriorInterior])
        {
            interiorLineInteriorLine_(half_edge, id_a, id_b, m_cluster_index_a, m_cluster_index_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorBoundary])
        {
            interiorLineBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_a, m_cluster_index_b, MatrixPredicate.InteriorBoundary);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorBoundary);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            interiorLineExteriorLine_(half_edge, id_a, id_b, MatrixPredicate.InteriorExterior);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorExterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryInterior])
        {
            interiorLineBoundaryLine_(half_edge, id_b, id_a, m_cluster_index_b, m_cluster_index_a, MatrixPredicate.BoundaryInterior);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryInterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryBoundary])
        {
            boundaryLineBoundaryLine_(half_edge, id_a, id_b, m_cluster_index_a, m_cluster_index_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryBoundary);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            boundaryLineExteriorLine_(half_edge, id_a, id_b, m_cluster_index_a, MatrixPredicate.BoundaryExterior);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryExterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            interiorLineExteriorLine_(half_edge, id_b, id_a, MatrixPredicate.ExteriorInterior);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorBoundary])
        {
            boundaryLineExteriorLine_(half_edge, id_b, id_a, m_cluster_index_b, MatrixPredicate.ExteriorBoundary);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorBoundary);
        }

        return bRelationKnown;
	}

	// Invokes the 9 relational predicates of area vs Point.
	private boolean areaPointPredicates_(int cluster, int id_a, int id_b) {
        boolean bRelationKnown = true;

        if (m_perform_predicates[MatrixPredicate.InteriorInterior])
        {
            interiorAreaInteriorPoint_(cluster, id_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            interiorAreaExteriorPoint_(cluster, id_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorExterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryInterior])
        {
            boundaryAreaInteriorPoint_(cluster, id_a, id_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryInterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            boundaryAreaExteriorPoint_(cluster, id_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryExterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            exteriorAreaInteriorPoint_(cluster, id_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorInterior);
        }

        return bRelationKnown;
	}

	// Invokes the 9 relational predicates of Line vs Point.
	private boolean linePointPredicates_(int cluster, int id_a, int id_b) {
        boolean bRelationKnown = true;

        if (m_perform_predicates[MatrixPredicate.InteriorInterior])
        {
            interiorLineInteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            interiorLineExteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorExterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryInterior])
        {
            boundaryLineInteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryInterior);
        }

        if (m_perform_predicates[MatrixPredicate.BoundaryExterior])
        {
            boundaryLineExteriorPoint_(cluster, id_a, id_b, m_cluster_index_a);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.BoundaryExterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            exteriorLineInteriorPoint_(cluster, id_a, id_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorInterior);
        }

        return bRelationKnown;
	}

	// Invokes the 9 relational predicates of Point vs Point.
	private boolean pointPointPredicates_(int cluster, int id_a, int id_b) {
        boolean bRelationKnown = true;

        if (m_perform_predicates[MatrixPredicate.InteriorInterior])
        {
            interiorPointInteriorPoint_(cluster, id_a, id_b);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorInterior);
        }

        if (m_perform_predicates[MatrixPredicate.InteriorExterior])
        {
            interiorPointExteriorPoint_(cluster, id_a, id_b, MatrixPredicate.InteriorExterior);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.InteriorExterior);
        }

        if (m_perform_predicates[MatrixPredicate.ExteriorInterior])
        {
            interiorPointExteriorPoint_(cluster, id_b, id_a, MatrixPredicate.ExteriorInterior);
            bRelationKnown &= isPredicateKnown_(MatrixPredicate.ExteriorInterior);
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

    private void interiorAreaExteriorLine_(int half_edge, int id_a, int id_b)
    {
        if (m_matrix[MatrixPredicate.InteriorExterior] == 2)
            return;

        int half_edge_parentage = m_topo_graph.getHalfEdgeParentage(half_edge);

        if ((half_edge_parentage & id_a) != 0)
        {//half edge of polygon
            m_matrix[MatrixPredicate.InteriorExterior] = 2;
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

    private void interiorAreaExteriorPoint_(int cluster, int id_a)
    {
        if (m_matrix[MatrixPredicate.InteriorExterior] == 2)
            return;

        int cluster_parentage = m_topo_graph.getClusterParentage(cluster);

        if ((cluster_parentage & id_a) != 0)
        {
            m_matrix[MatrixPredicate.InteriorExterior] = 2;
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

    private void boundaryAreaExteriorPoint_(int cluster, int id_a)
    {
        if (m_matrix[MatrixPredicate.BoundaryExterior] == 1)
            return;

        int cluster_parentage = m_topo_graph.getClusterParentage(cluster);

        if ((cluster_parentage & id_a) != 0)
        {
            m_matrix[MatrixPredicate.BoundaryExterior] = 1;
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

    private void interiorLineExteriorPoint_(int cluster, int id_a, int id_b, int cluster_index_a)
    {
        if (m_matrix[MatrixPredicate.InteriorExterior] == 1)
            return;

        int half_edge_a = m_topo_graph.getClusterHalfEdge(cluster);

        if (half_edge_a != -1)
        {
            m_matrix[MatrixPredicate.InteriorExterior] = 1;
            return;
        }

        if (m_matrix[MatrixPredicate.InteriorExterior] != 0)
        {
            int clusterParentage = m_topo_graph.getClusterParentage(cluster);

            if ((clusterParentage & id_b) == 0)
            {
                assert(m_topo_graph.getClusterUserIndex(cluster, cluster_index_a) % 2 == 0);
                m_matrix[MatrixPredicate.InteriorExterior] = 0;
                return;
            }
        }

        return;
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
            if (first_half_edge == -1)
            {
                if (m_predicates_cluster != -1)
                {
                    // Treat cluster as an interior point
                    switch (m_predicates_cluster)
                    {
                        case Predicates.AreaPointPredicates:
                            bRelationKnown = areaPointPredicates_(cluster, id_a, id_b);
                            break;
                        case Predicates.LinePointPredicates:
                            bRelationKnown = linePointPredicates_(cluster, id_a, id_b);
                            break;
                        default:
                            throw GeometryException.GeometryInternalError();
                    }
                }

                continue;
            }

			int next_half_edge = first_half_edge;

			do {
				int half_edge = next_half_edge;
				int visited = m_topo_graph.getHalfEdgeUserIndex(half_edge,
						m_visited_index);

				if (visited != 1) {
					do {
						// Invoke relational predicates
						switch (m_predicates_half_edge) {
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
							throw GeometryException.GeometryInternalError();
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
			switch (m_predicates_cluster) {
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
				throw GeometryException.GeometryInternalError();
			}

			if (bRelationKnown)
				break;
		}

		if (!bRelationKnown)
			setRemainingPredicatesToFalse_();
	}

	// Call this method to set the edit shape, if the edit shape has been
	// cracked and clustered already.
	private void setEditShape_(EditShape shape, ProgressTracker progressTracker) {
		m_topo_graph.setEditShape(shape, progressTracker);
	}

	private void setEditShapeCrackAndCluster_(EditShape shape,
			double tolerance, ProgressTracker progress_tracker) {
		editShapeCrackAndCluster_(shape, tolerance, progress_tracker);
		setEditShape_(shape, progress_tracker);
	}

	private void editShapeCrackAndCluster_(EditShape shape, double tolerance,
			ProgressTracker progress_tracker) {
		CrackAndCluster.execute(shape, tolerance, progress_tracker, false); //do not filter degenerate segments.
        shape.filterClosePoints(0, true, true);//remove degeneracies from polygon geometries.
		for (int geometry = shape.getFirstGeometry(); geometry != -1; geometry = shape
				.getNextGeometry(geometry)) {
			if (shape.getGeometryType(geometry) == Geometry.Type.Polygon
					.value())
				Simplificator.execute(shape, geometry, -1, false, progress_tracker);
		}
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
