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

import com.esri.core.geometry.OperatorCutLocal.Side;
import com.esri.core.geometry.VertexDescription.Semantics;
import java.util.ArrayList;

class OperatorCutCursor extends GeometryCursor {
	boolean m_bConsiderTouch;
	Geometry m_cuttee;
	Polyline m_cutter;
	double m_tolerance;
	int m_cutIndex;
	ArrayList<MultiPath> m_cuts;

	OperatorCutCursor(boolean bConsiderTouch, Geometry cuttee, Polyline cutter,
			SpatialReference spatialReference, ProgressTracker progressTracker) {
		if (cuttee == null)
			throw new GeometryException("invalid argument");

		m_bConsiderTouch = bConsiderTouch;
		m_cuttee = cuttee;
		m_cutter = cutter;
		m_tolerance = spatialReference != null ? spatialReference
				.getTolerance(Semantics.POSITION) : InternalUtils
				.calculateToleranceFromGeometry(null, cuttee, false);
		if (m_tolerance > 0.001)
			m_tolerance = 0.001;
		m_cutIndex = -1;
		m_cuts = null;
	}

	@Override
	public int getGeometryID() {
		return 0;
	}

	@Override
	public Geometry next() {
		if (m_cuts == null) {
			int type = m_cuttee.getType().value();
			switch (type) {
			case Geometry.GeometryType.Polyline:
				m_cuts = _cutPolyline();
				break;

			case Geometry.GeometryType.Polygon:
				m_cuts = _cutPolygon();
				break;
			}
		}

		if (++m_cutIndex < m_cuts.size())
			return (Geometry) m_cuts.get(m_cutIndex);

		return null;
	}

	private ArrayList<MultiPath> _cutPolyline() {
		MultiPath left = new Polyline();
		MultiPath right = new Polyline();
		MultiPath uncut = new Polyline();

		ArrayList<MultiPath> cuts = new ArrayList<MultiPath>(2);
		cuts.add(left);
		cuts.add(right);

		ArrayList<OperatorCutLocal.CutPair> cutPairs = new ArrayList<OperatorCutLocal.CutPair>(
				0);
		Cutter.CutPolyline(m_bConsiderTouch, (Polyline) m_cuttee, m_cutter,
				m_tolerance, cutPairs, null);

		for (int icut = 0; icut < cutPairs.size(); icut++) {
			OperatorCutLocal.CutPair cutPair = cutPairs.get(icut);
			if (cutPair.m_side == Side.Left) {
				left.add((MultiPath) cutPair.m_geometry, false);
			} else if (cutPair.m_side == Side.Right
					|| cutPair.m_side == Side.Coincident) {
				right.add((MultiPath) cutPair.m_geometry, false);
			} else if (cutPair.m_side == Side.Undefined) {
				cuts.add((MultiPath) cutPair.m_geometry);
			} else {
				uncut.add((MultiPath) cutPair.m_geometry, false);
			}
		}

		if (!uncut.isEmpty()
				&& (!left.isEmpty() || !right.isEmpty() || cuts.size() >= 3))
			cuts.add(uncut);

		return cuts;
	}

	ArrayList<MultiPath> _cutPolygon() {
		AttributeStreamOfInt32 cutHandles = new AttributeStreamOfInt32(0);
		EditShape shape = new EditShape();
		int sideIndex = shape.createGeometryUserIndex();
		int cutteeHandle = shape.addGeometry(m_cuttee);
		int cutterHandle = shape.addGeometry(m_cutter);
		TopologicalOperations topoOp = new TopologicalOperations();
		topoOp.setEditShapeCrackAndCluster(shape, m_tolerance, null);
		topoOp.cut(sideIndex, cutteeHandle, cutterHandle, cutHandles);
		Polygon cutteeRemainder = (Polygon) shape.getGeometry(cutteeHandle);

		MultiPath left = new Polygon();
		MultiPath right = new Polygon();

		ArrayList<MultiPath> cuts = new ArrayList<MultiPath>(2);
		cuts.add(left);
		cuts.add(right);

		for (int icutIndex = 0; icutIndex < cutHandles.size(); icutIndex++) {
			Geometry cutGeometry;
			{
				// intersection
				EditShape shapeIntersect = new EditShape();
				int geometryA = shapeIntersect.addGeometry(cutteeRemainder);
				int geometryB = shapeIntersect.addGeometry(shape
						.getGeometry(cutHandles.get(icutIndex)));
				topoOp.setEditShape(shapeIntersect);
				int intersectHandle = topoOp.intersection(geometryA, geometryB);
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
					cuts.add((MultiPath) cutGeometry); // Undefined
			}

			{
				// difference
				EditShape shapeDifference = new EditShape();
				int geometryA = shapeDifference.addGeometry(cutteeRemainder);
				int geometryB = shapeDifference.addGeometry(shape
						.getGeometry(cutHandles.get(icutIndex)));
				topoOp.setEditShape(shapeDifference);
				cutteeRemainder = (Polygon) shapeDifference.getGeometry(topoOp
						.difference(geometryA, geometryB));
			}
		}

		if (!cutteeRemainder.isEmpty() && cutHandles.size() > 0)
			cuts.add((MultiPath) cutteeRemainder);

		return cuts;
	}

}
