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

import com.esri.core.geometry.OperatorCutLocal.Side;
import com.esri.core.geometry.VertexDescription.Semantics;
import java.util.ArrayList;

class OperatorCutCursor extends GeometryCursor {
	boolean m_bConsiderTouch;
	Geometry m_cuttee;
	Polyline m_cutter;
	double m_tolerance;
	ProgressTracker m_progressTracker;
	int m_cutIndex;
	ArrayList<MultiPath> m_cuts = null;

	OperatorCutCursor(boolean bConsiderTouch, Geometry cuttee, Polyline cutter,
			SpatialReference spatialReference, ProgressTracker progressTracker) {
		if (cuttee == null || cutter == null)
			throw new GeometryException("invalid argument");

		m_bConsiderTouch = bConsiderTouch;
		m_cuttee = cuttee;
		m_cutter = cutter;
		Envelope2D e = InternalUtils.getMergedExtent(cuttee,  cutter);
		m_tolerance = InternalUtils.calculateToleranceFromGeometry(spatialReference, e, true);
		m_cutIndex = -1;
		m_progressTracker = progressTracker;
	}

	@Override
	public int getGeometryID() {
		return 0;
	}

	@Override
	public Geometry next() {
		generateCuts_();
		if (++m_cutIndex < m_cuts.size()) {
			return (Geometry)m_cuts.get(m_cutIndex);
		}
		
		return null;
	}

	private void generateCuts_() {
		if (m_cuts != null)
			return;
		
		m_cuts = new ArrayList<MultiPath>();
		
		Geometry.Type type = m_cuttee.getType();
		switch (type.value()) {
		case Geometry.GeometryType.Polyline:
			generate_polyline_cuts_();
			break;

		case Geometry.GeometryType.Polygon:
			generate_polygon_cuts_();
			break;

		default:
			break; // warning fix
		}
	}
	
	private void generate_polyline_cuts_() {
		MultiPath left = new Polyline();
		MultiPath right = new Polyline();
		MultiPath uncut = new Polyline();

		m_cuts.add(left);
		m_cuts.add(right);

		ArrayList<OperatorCutLocal.CutPair> cutPairs = new ArrayList<OperatorCutLocal.CutPair>(
				0);
		Cutter.CutPolyline(m_bConsiderTouch, (Polyline) m_cuttee, m_cutter,
				m_tolerance, cutPairs, null, m_progressTracker);

		for (int icut = 0; icut < cutPairs.size(); icut++) {
			OperatorCutLocal.CutPair cutPair = cutPairs.get(icut);
			if (cutPair.m_side == Side.Left) {
				left.add((MultiPath) cutPair.m_geometry, false);
			} else if (cutPair.m_side == Side.Right
					|| cutPair.m_side == Side.Coincident) {
				right.add((MultiPath) cutPair.m_geometry, false);
			} else if (cutPair.m_side == Side.Undefined) {
				m_cuts.add((MultiPath) cutPair.m_geometry);
			} else {
				uncut.add((MultiPath) cutPair.m_geometry, false);
			}
		}

		if (!uncut.isEmpty()
				&& (!left.isEmpty() || !right.isEmpty() || m_cuts.size() >= 3))
			m_cuts.add(uncut);

		if (left.isEmpty() && right.isEmpty() && m_cuts.size() < 3)
			m_cuts.clear(); // no cuts
	}

	private void generate_polygon_cuts_() {
		AttributeStreamOfInt32 cutHandles = new AttributeStreamOfInt32(0);
		EditShape shape = new EditShape();
		int sideIndex = shape.createGeometryUserIndex();
		int cutteeHandle = shape.addGeometry(m_cuttee);
		int cutterHandle = shape.addGeometry(m_cutter);
		TopologicalOperations topoOp = new TopologicalOperations();
		try {
			topoOp.setEditShapeCrackAndCluster(shape, m_tolerance,
					m_progressTracker);
			topoOp.cut(sideIndex, cutteeHandle, cutterHandle, cutHandles);
			Polygon cutteeRemainder = (Polygon) shape.getGeometry(cutteeHandle);

			MultiPath left = new Polygon();
			MultiPath right = new Polygon();

			m_cuts.clear();
			m_cuts.add(left);
			m_cuts.add(right);

			for (int icutIndex = 0; icutIndex < cutHandles.size(); icutIndex++) {
				Geometry cutGeometry;
				{
					// intersection
					EditShape shapeIntersect = new EditShape();
					int geometryA = shapeIntersect.addGeometry(cutteeRemainder);
					int geometryB = shapeIntersect.addGeometry(shape
							.getGeometry(cutHandles.get(icutIndex)));
					topoOp.setEditShape(shapeIntersect, m_progressTracker);
					int intersectHandle = topoOp.intersection(geometryA,
							geometryB);
					cutGeometry = shapeIntersect.getGeometry(intersectHandle);

					if (cutGeometry.isEmpty())
						continue;

					int side = shape.getGeometryUserIndex(
							cutHandles.get(icutIndex), sideIndex);
					if (side == 2)
						left.add((MultiPath) cutGeometry, false);
					else if (side == 1)
						right.add((MultiPath) cutGeometry, false);
					else
						m_cuts.add((MultiPath) cutGeometry); // Undefined
				}

				{
					// difference
					EditShape shapeDifference = new EditShape();
					int geometryA = shapeDifference
							.addGeometry(cutteeRemainder);
					int geometryB = shapeDifference.addGeometry(shape
							.getGeometry(cutHandles.get(icutIndex)));
					topoOp.setEditShape(shapeDifference, m_progressTracker);
					cutteeRemainder = (Polygon) shapeDifference
							.getGeometry(topoOp
									.difference(geometryA, geometryB));
				}
			}

			if (!cutteeRemainder.isEmpty() && cutHandles.size() > 0)
				m_cuts.add((MultiPath) cutteeRemainder);

			if (left.isEmpty() && right.isEmpty())
				m_cuts.clear(); // no cuts

		} finally {
			topoOp.removeShape();
		}
	}
}

