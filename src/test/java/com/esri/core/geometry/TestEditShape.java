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

import junit.framework.TestCase;
import org.junit.Test;

public class TestEditShape extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testEditShape() {
		{
			// Single part polygon
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(14, 15);
			poly.lineTo(10, 11);
			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			Polygon poly2 = (Polygon) editShape.getGeometry(geom);
			assertTrue(poly.equals(poly2));
		}

		{
			// Two part poly
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(14, 15);
			poly.lineTo(10, 11);

			poly.startPath(100, 10);
			poly.lineTo(100, 12);
			poly.lineTo(14, 150);
			poly.lineTo(10, 101);
			poly.lineTo(100, 11);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			Polygon poly2 = (Polygon) editShape.getGeometry(geom);
			assertTrue(poly.equals(poly2));
		}

		{
			// Single part polyline
			Polyline poly = new Polyline();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(14, 15);
			poly.lineTo(10, 11);
			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			Polyline poly2 = (Polyline) editShape.getGeometry(geom);
			assertTrue(poly.equals(poly2));
		}

		{
			// Two part poly
			Polyline poly = new Polyline();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(14, 15);
			poly.lineTo(10, 11);

			poly.startPath(100, 10);
			poly.lineTo(100, 12);
			poly.lineTo(14, 150);
			poly.lineTo(10, 101);
			poly.lineTo(100, 11);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			Polyline poly2 = (Polyline) editShape.getGeometry(geom);
			assertTrue(poly.equals(poly2));
		}

		{
			// Five part poly. Close one of parts to test if it works.
			Polyline poly = new Polyline();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(14, 15);
			poly.lineTo(10, 11);

			poly.startPath(100, 10);
			poly.lineTo(100, 12);
			poly.lineTo(14, 150);
			poly.lineTo(10, 101);
			poly.lineTo(100, 11);

			poly.startPath(1100, 101);
			poly.lineTo(1300, 132);
			poly.lineTo(144, 150);
			poly.lineTo(106, 1051);
			poly.lineTo(1600, 161);

			poly.startPath(100, 190);
			poly.lineTo(1800, 192);
			poly.lineTo(184, 8150);
			poly.lineTo(1080, 181);

			poly.startPath(1030, 10);
			poly.lineTo(1300, 132);
			poly.lineTo(314, 3150);
			poly.lineTo(310, 1301);
			poly.lineTo(3100, 311);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			editShape.setClosedPath(
					editShape.getNextPath(editShape.getFirstPath(geom)), true);
			((MultiPathImpl) poly._getImpl()).closePathWithLine(1);
			Polyline poly2 = (Polyline) editShape.getGeometry(geom);
			assertTrue(poly.equals(poly2));
		}

		{
			// Test erase
			Polyline poly = new Polyline();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(314, 3150);
			poly.lineTo(310, 1301);
			poly.lineTo(3100, 311);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			int vertex = editShape.getFirstVertex(editShape.getFirstPath(geom));
			vertex = editShape.removeVertex(vertex, true);
			vertex = editShape.getNextVertex(vertex);
			editShape.removeVertex(vertex, true);
			Polyline poly2 = (Polyline) editShape.getGeometry(geom);

			poly.setEmpty();
			poly.startPath(10, 12);
			poly.lineTo(310, 1301);
			poly.lineTo(3100, 311);

			assertTrue(poly.equals(poly2));
		}

		{
			// Test erase
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(10, 12);
			poly.lineTo(314, 3150);
			poly.lineTo(310, 1301);
			poly.lineTo(3100, 311);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			int vertex = editShape.getFirstVertex(editShape.getFirstPath(geom));
			vertex = editShape.removeVertex(vertex, true);
			vertex = editShape.getNextVertex(vertex);
			editShape.removeVertex(vertex, true);
			Polygon poly2 = (Polygon) editShape.getGeometry(geom);

			poly.setEmpty();
			poly.startPath(10, 12);
			poly.lineTo(310, 1301);
			poly.lineTo(3100, 311);

			assertTrue(poly.equals(poly2));
		}

		{
			// Test Filter Close Points
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(10, 10.001);
			poly.lineTo(10.001, 10);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			editShape.filterClosePoints(0.002, true, false);
			Polygon poly2 = (Polygon) editShape.getGeometry(geom);
			assertTrue(poly2.isEmpty());
		}

		{
			// Test Filter Close Points
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(10, 10.0025);
			poly.lineTo(11.0, 10);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			editShape.filterClosePoints(0.002, true, false);
			Polygon poly2 = (Polygon) editShape.getGeometry(geom);
			assertTrue(!poly2.isEmpty());
		}

		{
			// Test Filter Close Points
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(10, 10.001);
			poly.lineTo(11.0, 10);

			EditShape editShape = new EditShape();
			int geom = editShape.addGeometry(poly);
			editShape.filterClosePoints(0.002, true, false);
			Polygon poly2 = (Polygon) editShape.getGeometry(geom);
			assertTrue(poly2.isEmpty());
		}

		{
			// Test attribute splitting 1
			Polyline polyline = new Polyline();
			polyline.startPath(0, 0);
			polyline.lineTo(1, 1);
			polyline.lineTo(2, 2);
			polyline.lineTo(3, 3);
			polyline.lineTo(4, 4);

			polyline.startPath(5, 5);
			polyline.lineTo(6, 6);
			polyline.lineTo(7, 7);
			polyline.lineTo(8, 8);
			polyline.lineTo(9, 9);

			polyline.addAttribute(VertexDescription.Semantics.Z);
			polyline.setAttribute(VertexDescription.Semantics.Z, 0, 0, 4);
			polyline.setAttribute(VertexDescription.Semantics.Z, 1, 0, 8);
			polyline.setAttribute(VertexDescription.Semantics.Z, 2, 0, 12);
			polyline.setAttribute(VertexDescription.Semantics.Z, 3, 0, 16);
			polyline.setAttribute(VertexDescription.Semantics.Z, 4, 0, 20);

			polyline.setAttribute(VertexDescription.Semantics.Z, 5, 0, 22);
			polyline.setAttribute(VertexDescription.Semantics.Z, 6, 0, 26);
			polyline.setAttribute(VertexDescription.Semantics.Z, 7, 0, 30);
			polyline.setAttribute(VertexDescription.Semantics.Z, 8, 0, 34);
			polyline.setAttribute(VertexDescription.Semantics.Z, 9, 0, 38);

			EditShape shape = new EditShape();
			int geometry = shape.addGeometry(polyline);

			AttributeStreamOfInt32 vertex_handles = new AttributeStreamOfInt32(
					0);

			for (int path = shape.getFirstPath(geometry); path != -1; path = shape
					.getNextPath(path)) {
				for (int vertex = shape.getFirstVertex(path); vertex != -1; vertex = shape
						.getNextVertex(vertex)) {
					if (vertex != shape.getLastVertex(path))
						vertex_handles.add(vertex);
				}
			}

			double[] t = new double[1];
			for (int i = 0; i < vertex_handles.size(); i++) {
				int vertex = vertex_handles.read(i);
				t[0] = 0.5;
				shape.splitSegment(vertex, t, 1);
			}

			Polyline chopped_polyline = (Polyline) shape.getGeometry(geometry);
			assertTrue(chopped_polyline.getPointCount() == 18);

			double att_ = 4;
			for (int i = 0; i < 18; i++) {
				double att = chopped_polyline.getAttributeAsDbl(
						VertexDescription.Semantics.Z, i, 0);
				assertTrue(att == att_);
				att_ += 2;
			}

		}

		{ // Test attribute splitting 2
			Polyline line1 = new Polyline(), line2 = new Polyline();
			line1.addAttribute(VertexDescription.Semantics.M);
			line2.addAttribute(VertexDescription.Semantics.M);
			line1.startPath(0, 0);
			line1.lineTo(10, 10);
			line2.startPath(10, 0);
			line2.lineTo(0, 10);
			line1.setAttribute(VertexDescription.Semantics.M, 0, 0, 7);
			line1.setAttribute(VertexDescription.Semantics.M, 1, 0, 17);
			line2.setAttribute(VertexDescription.Semantics.M, 0, 0, 5);
			line2.setAttribute(VertexDescription.Semantics.M, 1, 0, 15);

			EditShape shape = new EditShape();
			int g1 = shape.addGeometry(line1);
			int g2 = shape.addGeometry(line2);
			CrackAndCluster.execute(shape, 0.001, null, true);

			Polyline chopped_line1 = (Polyline) shape.getGeometry(g1);
			Polyline chopped_line2 = (Polyline) shape.getGeometry(g2);

			double att1 = chopped_line1.getAttributeAsDbl(
					VertexDescription.Semantics.M, 1, 0);
			double att2 = chopped_line2.getAttributeAsDbl(
					VertexDescription.Semantics.M, 1, 0);
			assertTrue(att1 == 12);
			assertTrue(att2 == 10);
		}

		{ // Test attribute splitting 3
			Polygon polygon = new Polygon();
			polygon.addAttribute(VertexDescription.Semantics.M);
			polygon.startPath(0, 0);
			polygon.lineTo(0, 10);
			polygon.lineTo(10, 10);
			polygon.lineTo(10, 0);

			polygon.setAttribute(VertexDescription.Semantics.M, 0, 0, 7);
			polygon.setAttribute(VertexDescription.Semantics.M, 1, 0, 17);
			polygon.setAttribute(VertexDescription.Semantics.M, 2, 0, 23);
			polygon.setAttribute(VertexDescription.Semantics.M, 3, 0, 43);

			EditShape shape = new EditShape();
			int geometry = shape.addGeometry(polygon);

			AttributeStreamOfInt32 vertex_handles = new AttributeStreamOfInt32(
					0);

			int start_v = shape.getFirstVertex(shape.getFirstPath(geometry));
			int v = start_v;

			do {
				vertex_handles.add(v);
				v = shape.getNextVertex(v);
			} while (v != start_v);

			double[] t = new double[1];
			for (int i = 0; i < vertex_handles.size(); i++) {
				int v1 = vertex_handles.read(i);
				t[0] = 0.5;
				shape.splitSegment(v1, t, 1);
			}

			Polygon cut_polygon = (Polygon) shape.getGeometry(geometry);
			assertTrue(cut_polygon.getPointCount() == 8);

			@SuppressWarnings("unused")
			Point2D pt0 = cut_polygon.getXY(0);
			double a0 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 0, 0);
			assertTrue(a0 == 25);

			@SuppressWarnings("unused")
			Point2D pt1 = cut_polygon.getXY(1);
			double a1 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 1, 0);
			assertTrue(a1 == 7);

			@SuppressWarnings("unused")
			Point2D pt2 = cut_polygon.getXY(2);
			double a2 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 2, 0);
			assertTrue(a2 == 12);

			@SuppressWarnings("unused")
			Point2D pt3 = cut_polygon.getXY(3);
			double a3 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 3, 0);
			assertTrue(a3 == 17);

			@SuppressWarnings("unused")
			Point2D pt4 = cut_polygon.getXY(4);
			double a4 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 4, 0);
			assertTrue(a4 == 20);

			@SuppressWarnings("unused")
			Point2D pt5 = cut_polygon.getXY(5);
			double a5 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 5, 0);
			assertTrue(a5 == 23);

			@SuppressWarnings("unused")
			Point2D pt6 = cut_polygon.getXY(6);
			double a6 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 6, 0);
			assertTrue(a6 == 33);

			@SuppressWarnings("unused")
			Point2D pt7 = cut_polygon.getXY(7);
			double a7 = cut_polygon.getAttributeAsDbl(
					VertexDescription.Semantics.M, 7, 0);
			assertTrue(a7 == 43);
		}
	}
}
