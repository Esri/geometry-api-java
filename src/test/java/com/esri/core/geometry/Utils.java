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

public class Utils {
	static void showProjectedGeometryInfo(MapGeometry mapGeom) {
		return;
		/*
		System.out.println("\n");
		MapGeometry geom = mapGeom;
		int wkid = geom.getSpatialReference() != null ? geom
				.getSpatialReference().getID() : -1;
		// while ((geom = geomCursor.next()) != null) {

		if (geom.getGeometry() instanceof Point) {
			Point pnt = (Point) geom.getGeometry();
			System.out
					.println("Point(" + pnt.getX() + " , " + pnt.getY() + ")");
			if (geom.getSpatialReference() == null)
				System.out.println("No spatial reference");
			else
				System.out.println("wkid: "
						+ geom.getSpatialReference().getID());

		} else if (geom.getGeometry() instanceof MultiPoint) {
			MultiPoint mp = (MultiPoint) geom.getGeometry();
			System.out.println("Multipoint has " + mp.getPointCount()
					+ " points.");

			System.out.println("wkid: " + wkid);

		} else if (geom.getGeometry() instanceof Polygon) {
			Polygon mp = (Polygon) geom.getGeometry();
			System.out.println("Polygon has " + mp.getPointCount()
					+ " points and " + mp.getPathCount() + " parts.");
			if (mp.getPathCount() > 1) {
				System.out.println("Part start of 2nd segment : "
						+ mp.getPathStart(1));
				System.out.println("Part end of 2nd segment   : "
						+ mp.getPathEnd(1));
				System.out.println("Part size of 2nd segment  : "
						+ mp.getPathSize(1));

				int start = mp.getPathStart(1);
				int end = mp.getPathEnd(1);
				for (int i = start; i < end; i++) {
					Point pp = mp.getPoint(i);
					System.out.println("Point(" + i + ") = (" + pp.getX()
							+ ", " + pp.getY() + ")");
				}
			}
			System.out.println("wkid: " + wkid);

		} else if (geom.getGeometry() instanceof Polyline) {
			Polyline mp = (Polyline) geom.getGeometry();
			System.out.println("Polyline has " + mp.getPointCount()
					+ " points and " + mp.getPathCount() + " parts.");
			System.out.println("Part start of 2nd segment : "
					+ mp.getPathStart(1));
			System.out.println("Part end of 2nd segment   : "
					+ mp.getPathEnd(1));
			System.out.println("Part size of 2nd segment  : "
					+ mp.getPathSize(1));
			int start = mp.getPathStart(1);
			int end = mp.getPathEnd(1);
			for (int i = start; i < end; i++) {
				Point pp = mp.getPoint(i);
				System.out.println("Point(" + i + ") = (" + pp.getX() + ", "
						+ pp.getY() + ")");
			}

			System.out.println("wkid: " + wkid);
		}*/

	}

}
