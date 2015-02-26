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

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;

public abstract class RasterizedGeometry2D {

	public enum HitType {
		Outside(0), // the test geometry is well outside the geometry bounds
		Inside(1), // the test geometry is well inside the geomety bounds
		Border(2); // the test geometry is close to the bounds or intersects the
					// bounds

		int enumVal;

		private HitType(int val) {
			enumVal = val;
		};
	}

	/**
	 * Test a point against the RasterizedGeometry
	 */
	public abstract HitType queryPointInGeometry(double x, double y);

	/**
	 * Test an envelope against the RasterizedGeometry.
	 */
	public abstract HitType queryEnvelopeInGeometry(Envelope2D env);

	/**
	 * Creates a rasterized geometry from a given Geometry.
	 * 
	 * @param geom
	 *            The input geometry to rasterize. It has to be a MultiVertexGeometry instance.
	 * @param toleranceXY
	 *            The tolerance of the rasterization. Raster pixels that are
	 *            closer than given tolerance to the Geometry will be set.
	 * @param rasterSizeBytes
	 *            The max size of the raster in bytes. The raster has size of
	 *            rasterSize x rasterSize. Polygons are rasterized into 2 bpp
	 *            (bits per pixel) rasters while other geometries are rasterized
	 *            into 1 bpp rasters. 32x32 pixel raster for a polygon would
	 *            take 256 bytes of memory
	 */
	public static RasterizedGeometry2D create(Geometry geom,
			double toleranceXY, int rasterSizeBytes) {
		if (!canUseAccelerator(geom))
			throw new IllegalArgumentException();

		RasterizedGeometry2DImpl gc = RasterizedGeometry2DImpl.createImpl(geom,
				toleranceXY, rasterSizeBytes);
		return (RasterizedGeometry2D) gc;
	}

	static RasterizedGeometry2D create(MultiVertexGeometryImpl geom,
			double toleranceXY, int rasterSizeBytes) {
		if (!canUseAccelerator(geom))
			throw new IllegalArgumentException();

		RasterizedGeometry2DImpl gc = RasterizedGeometry2DImpl.createImpl(geom,
				toleranceXY, rasterSizeBytes);
		return (RasterizedGeometry2D) gc;

	}

	public static int rasterSizeFromAccelerationDegree(
			GeometryAccelerationDegree accelDegree) {
		int value = 0;
		switch (accelDegree) {
		case enumMild:
			value = 64 * 64 * 2 / 8;// 1k
			break;
		case enumMedium:
			value = 256 * 256 * 2 / 8;// 16k
			break;
		case enumHot:
			value = 1024 * 1024 * 2 / 8;// 256k
			break;
		default:
			throw GeometryException.GeometryInternalError();
		}

		return value;
	}

	/**
	 * Checks whether the RasterizedGeometry2D accelerator can be used with the
	 * given geometry.
	 */
	static boolean canUseAccelerator(Geometry geom) {
		if (geom.isEmpty()
				|| !(geom.getType() == Geometry.Type.Polyline || geom.getType() == Geometry.Type.Polygon))
			return false;

		return true;
	}

	/**
	 * Returns the tolerance for which the rasterized Geometry has been built.
	 */
	public abstract double getToleranceXY();

	/**
	 * Returns raster size in bytes
	 */
	public abstract int getRasterSize();

	/**
	 * Dumps the raster to a bmp file for debug purposes.
	 * 
	 * @param fileName
	 * @return true if success, false otherwise.
	 */
	public abstract boolean dbgSaveToBitmap(String fileName);

}
