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

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;

public class TestRasterizedGeometry2D extends TestCase {
	boolean rgHelper(RasterizedGeometry2D rg, MultiPath mp) {
		SegmentIterator iter = mp.querySegmentIterator();
		while (iter.nextPath()) {
			while (iter.hasNextSegment()) {
				Segment seg = iter.nextSegment();
				int count = 20;

				for (int i = 0; i < count; i++) {
					double t = (1.0 * i / count);
					Point2D pt = seg.getCoord2D(t);
					RasterizedGeometry2D.HitType hit = rg.queryPointInGeometry(
							pt.x, pt.y);
					if (hit != RasterizedGeometry2D.HitType.Border)
						return false;
				}
			}
		}

		if (mp.getType() != Geometry.Type.Polygon)
			return true;

		Polygon poly = (Polygon) mp;
		Envelope2D env = new Envelope2D();
		poly.queryEnvelope2D(env);
		int count = 100;
		for (int iy = 0; iy < count; iy++) {
			double ty = 1.0 * iy / count;
			double y = env.ymin * (1.0 - ty) + ty * env.ymax;
			for (int ix = 0; ix < count; ix++) {
				double tx = 1.0 * ix / count;
				double x = env.xmin * (1.0 - tx) + tx * env.xmax;

				RasterizedGeometry2D.HitType hit = rg
						.queryPointInGeometry(x, y);
				PolygonUtils.PiPResult res = PolygonUtils.isPointInPolygon2D(
						poly, new Point2D(x, y), 0);
				if (res == PolygonUtils.PiPResult.PiPInside) {
					boolean bgood = (hit == RasterizedGeometry2D.HitType.Border || hit == RasterizedGeometry2D.HitType.Inside);
					if (!bgood)
						return false;
				} else if (res == PolygonUtils.PiPResult.PiPOutside) {
					boolean bgood = (hit == RasterizedGeometry2D.HitType.Border || hit == RasterizedGeometry2D.HitType.Outside);
					if (!bgood)
						return false;
				} else {
					boolean bgood = (hit == RasterizedGeometry2D.HitType.Border);
					if (!bgood)
						return false;
				}
			}
		}

		return true;
	}

	@Test
	public void test() {
		{
			Polygon poly = new Polygon();
			poly.startPath(10, 10);
			poly.lineTo(100, 10);
			poly.lineTo(100, 100);
			poly.lineTo(10, 100);

			// create using move semantics. Usually we do not use this
			// approach.
			RasterizedGeometry2D rg = RasterizedGeometry2D
					.create(poly, 0, 1024);
			//rg.dbgSaveToBitmap("c:/temp/_dbg.bmp");
			RasterizedGeometry2D.HitType res;
			res = rg.queryPointInGeometry(7, 10);
			assertTrue(res == RasterizedGeometry2D.HitType.Outside);
			res = rg.queryPointInGeometry(10, 10);
			assertTrue(res == RasterizedGeometry2D.HitType.Border);
			res = rg.queryPointInGeometry(50, 50);
			assertTrue(res == RasterizedGeometry2D.HitType.Inside);

			assertTrue(rgHelper(rg, poly));
		}

		{
			Polygon poly = new Polygon();
			// create a star (non-simple)
			poly.startPath(1, 0);
			poly.lineTo(5, 10);
			poly.lineTo(9, 0);
			poly.lineTo(0, 6);
			poly.lineTo(10, 6);

			RasterizedGeometry2D rg = RasterizedGeometry2D
					.create(poly, 0, 1024);
			//rg.dbgSaveToBitmap("c:/temp/_dbg.bmp");
			RasterizedGeometry2D.HitType res;
			res = rg.queryPointInGeometry(5, 5.5);
			assertTrue(res == RasterizedGeometry2D.HitType.Outside);
			res = rg.queryPointInGeometry(5, 8);
			assertTrue(res == RasterizedGeometry2D.HitType.Inside);
			res = rg.queryPointInGeometry(1.63, 0.77);
			assertTrue(res == RasterizedGeometry2D.HitType.Inside);
			res = rg.queryPointInGeometry(1, 3);
			assertTrue(res == RasterizedGeometry2D.HitType.Outside);
			res = rg.queryPointInGeometry(1.6, 0.1);
			assertTrue(res == RasterizedGeometry2D.HitType.Outside);
			assertTrue(rgHelper(rg, poly));
		}
		
		{
			Polygon poly = new Polygon();
			// create a star (non-simple)
			poly.startPath(1, 0);
			poly.lineTo(5, 10);
			poly.lineTo(9, 0);
			poly.lineTo(0, 6);
			poly.lineTo(10, 6);

			SpatialReference sr = SpatialReference.create(4326);
			poly = (Polygon)OperatorSimplify.local().execute(poly, sr, true, null);
			OperatorContains.local().accelerateGeometry(poly, sr, GeometryAccelerationDegree.enumMedium);
			assertFalse(OperatorContains.local().execute(poly, new Point(5,  5.5), sr, null));
			assertTrue(OperatorContains.local().execute(poly, new Point(5,  8), sr, null));
			assertTrue(OperatorContains.local().execute(poly, new Point(1.63,  0.77), sr, null));
			assertFalse(OperatorContains.local().execute(poly, new Point(1, 3), sr, null));
			assertFalse(OperatorContains.local().execute(poly, new Point(1.6,  0.1), sr, null));
		}
		
		/*
		{
			Geometry g = OperatorFactoryLocal.loadGeometryFromEsriShapeDbg("c:/temp/_poly_final.bin");
			RasterizedGeometry2D rg1 = RasterizedGeometry2D
					.create(g, 0, 1024);//warmup
			rg1 = null;
			
		    long t0 = System.nanoTime();
			RasterizedGeometry2D rg = RasterizedGeometry2D
					.create(g, 0, 1024 * 1024);
			long t1 = System.nanoTime();
			double d = (t1 - t0) / 1000000.0;
			System.out.printf("Time to rasterize the geometry: %f", d);
			
			rg.dbgSaveToBitmap("c:/temp/_dbg.bmp");
			for (;;){}
		}*/
	}
}
