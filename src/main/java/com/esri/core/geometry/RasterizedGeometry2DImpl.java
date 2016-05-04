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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryException;
import com.esri.core.geometry.NumberUtils;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Segment;
import com.esri.core.geometry.SegmentIteratorImpl;
import com.esri.core.geometry.SimpleRasterizer;

final class RasterizedGeometry2DImpl extends RasterizedGeometry2D {
	int[] m_bitmap;
	int m_scanLineSize;
	int m_width;
	double m_dx;
	double m_dy;
	double m_x0;
	double m_y0;
	double m_toleranceXY;
	double m_stroke_half_widthX_pix;
	double m_stroke_half_widthY_pix;
	double m_stroke_half_width;

	Envelope2D m_geomEnv;// envelope of the raster in world coordinates
	Transformation2D m_transform;
	int m_dbgTestCount;
	SimpleRasterizer m_rasterizer;
	ScanCallbackImpl m_callback;

	class ScanCallbackImpl implements SimpleRasterizer.ScanCallback {
		int[] m_bitmap;
		int m_scanlineWidth;
		int m_color;

		public ScanCallbackImpl(int[] bitmap, int scanlineWidth) {
			m_scanlineWidth = scanlineWidth;
			m_bitmap = bitmap;
		}

		public void setColor(SimpleRasterizer rasterizer, int color) {
			if (m_color != color)
				rasterizer.flush();
			
			m_color = color;// set new color
		}

		@Override
		public void drawScan(int[] scans, int scanCount3) {
			for (int i = 0; i < scanCount3; ) {
				int x0 = scans[i++];
				int x1 = scans[i++];
				int y = scans[i++];
	
				int scanlineStart = y * m_scanlineWidth;
				for (int xx = x0; xx < x1; xx++) {
					m_bitmap[scanlineStart + (xx >> 4)] |= m_color << ((xx & 15) * 2);// 2
					// bit
					// per
					// color
			}
			}
		}
	}

	void fillMultiPath(SimpleRasterizer rasterizer, Transformation2D trans, MultiPathImpl polygon, boolean isWinding) {
		SegmentIteratorImpl segIter = polygon.querySegmentIterator();
		Point2D p1 = new Point2D();
		Point2D p2 = new Point2D();
		while (segIter.nextPath()) {
			while (segIter.hasNextSegment()) {
				Segment seg = segIter.nextSegment();
				if (seg.getType() != Geometry.Type.Line)
					throw GeometryException.GeometryInternalError(); // TODO:
				// densify
				// the
				// segment
				// here
				trans.transform(seg.getStartXY(), p1);
				trans.transform(seg.getEndXY(), p2);
				m_rasterizer.addEdge(p1.x, p1.y, p2.x, p2.y);
			}
		}
		
		m_rasterizer.renderEdges(isWinding ? SimpleRasterizer.WINDING : SimpleRasterizer.EVEN_ODD);
	}
	
	void fillPoints(SimpleRasterizer rasterizer, MultiPointImpl geom, double stroke_half_width) {
		throw GeometryException.GeometryInternalError();
	}

	void fillConvexPolygon(SimpleRasterizer rasterizer, Point2D[] fan, int len) {
		for (int i = 1, n = len; i < n; i++) {
			rasterizer.addEdge(fan[i-1].x, fan[i-1].y, fan[i].x, fan[i].y);
		}
		rasterizer.addEdge(fan[len-1].x, fan[len-1].y, fan[0].x, fan[0].y);
		m_rasterizer.renderEdges(SimpleRasterizer.EVEN_ODD);
	}

	void fillEnvelope(SimpleRasterizer rasterizer, Envelope2D envIn) {
		rasterizer.fillEnvelope(envIn);
	}
	
	void strokeDrawPolyPath(SimpleRasterizer rasterizer,
			MultiPathImpl polyPath, double tol) {

		Point2D[] fan = new Point2D[4];
		for (int i = 0; i < fan.length; i++)
			fan[i] = new Point2D();

		SegmentIteratorImpl segIter = polyPath.querySegmentIterator();
		double strokeHalfWidth = m_transform.transform(tol) + 1.5;
		double shortSegment = 0.25;
		Point2D vec = new Point2D();
		Point2D vecA = new Point2D();
		Point2D vecB = new Point2D();

		Point2D ptStart = new Point2D();
		Point2D ptEnd = new Point2D();
		Point2D prev_start = new Point2D();
		Point2D prev_end = new Point2D();
		double[] helper_xy_10_elm = new double[10];
		Envelope2D segEnv = new Envelope2D();
		Point2D ptOld = new Point2D();
		while (segIter.nextPath()) {
			boolean hasFan = false;
			boolean first = true;
			ptOld.setCoords(0, 0);
			while (segIter.hasNextSegment()) {
				Segment seg = segIter.nextSegment();
				ptStart.x = seg.getStartX();
				ptStart.y = seg.getStartY();
				ptEnd.x = seg.getEndX();
				ptEnd.y = seg.getEndY();
				segEnv.setEmpty();
				segEnv.merge(ptStart.x, ptStart.y);
				segEnv.mergeNE(ptEnd.x, ptEnd.y);
				if (!m_geomEnv.isIntersectingNE(segEnv)) {
					if (hasFan) {
						rasterizer.startAddingEdges();
						rasterizer.addSegmentStroke(prev_start.x, prev_start.y,
								prev_end.x, prev_end.y, strokeHalfWidth, false,
								helper_xy_10_elm);
						rasterizer.renderEdges(SimpleRasterizer.EVEN_ODD);
						hasFan = false;
					}

					first = true;
					continue;
				}

				m_transform.transform(ptEnd, ptEnd);

				if (first) {
					m_transform.transform(ptStart, ptStart);
					ptOld.setCoords(ptStart);
					first = false;
				} else {
					ptStart.setCoords(ptOld);
				}

				prev_start.setCoords(ptStart);
				prev_end.setCoords(ptEnd);

				rasterizer.startAddingEdges();
				hasFan = !rasterizer.addSegmentStroke(prev_start.x,
						prev_start.y, prev_end.x, prev_end.y, strokeHalfWidth,
						true, helper_xy_10_elm);
				rasterizer.renderEdges(SimpleRasterizer.EVEN_ODD);
				if (!hasFan)
					ptOld.setCoords(prev_end);
			}

			if (hasFan) {
				rasterizer.startAddingEdges();
				hasFan = !rasterizer.addSegmentStroke(prev_start.x,
						prev_start.y, prev_end.x, prev_end.y, strokeHalfWidth,
						false, helper_xy_10_elm);
				rasterizer.renderEdges(SimpleRasterizer.EVEN_ODD);
			}
		}
	}

	int worldToPixX(double x) {
		return (int) (x * m_dx + m_x0);
	}

	int worldToPixY(double y) {
		return (int) (y * m_dy + m_y0);
	}

	RasterizedGeometry2DImpl(Geometry geom, double toleranceXY,
			int rasterSizeBytes) {
		// //_ASSERT(CanUseAccelerator(geom));
		init((MultiVertexGeometryImpl) geom._getImpl(), toleranceXY,
				rasterSizeBytes);
	}

	static RasterizedGeometry2DImpl createImpl(Geometry geom,
			double toleranceXY, int rasterSizeBytes) {
		RasterizedGeometry2DImpl rgImpl = new RasterizedGeometry2DImpl(geom,
				toleranceXY, rasterSizeBytes);

		return rgImpl;
	}

	private RasterizedGeometry2DImpl(MultiVertexGeometryImpl geom,
			double toleranceXY, int rasterSizeBytes) {
		init(geom, toleranceXY, rasterSizeBytes);
	}

	static RasterizedGeometry2DImpl createImpl(MultiVertexGeometryImpl geom,
			double toleranceXY, int rasterSizeBytes) {
		RasterizedGeometry2DImpl rgImpl = new RasterizedGeometry2DImpl(geom,
				toleranceXY, rasterSizeBytes);
		return rgImpl;
	}
	
	void init(MultiVertexGeometryImpl geom, double toleranceXY,
			int rasterSizeBytes) {
		// _ASSERT(CanUseAccelerator(geom));
		m_width = Math.max((int) (Math.sqrt(rasterSizeBytes) * 2 + 0.5), 64);
		m_scanLineSize = (m_width * 2 + 31) / 32; // 2 bits per pixel
		m_geomEnv = new Envelope2D();

		m_toleranceXY = toleranceXY;

		// calculate bitmap size
		int size = 0;
		int width = m_width;
		int scanLineSize = m_scanLineSize;
		while (width >= 8) {
			size += width * scanLineSize;
			width /= 2;
			scanLineSize = (width * 2 + 31) / 32;
		}

		// allocate the bitmap, that contains the base and the mip-levels
		m_bitmap = new int[size];
		for (int i = 0; i < size; i++)
			m_bitmap[i] = 0;

		m_rasterizer = new SimpleRasterizer();
		ScanCallbackImpl callback = new ScanCallbackImpl(m_bitmap,
				m_scanLineSize);
		m_callback = callback;
		m_rasterizer.setup(m_width, m_width, callback);
		geom.queryEnvelope2D(m_geomEnv);
		if (m_geomEnv.getWidth() > m_width * m_geomEnv.getHeight()
				|| m_geomEnv.getHeight() > m_geomEnv.getWidth() * m_width) {
			// the geometry is thin and the rasterizer is not needed.
		}
		m_geomEnv.inflate(toleranceXY, toleranceXY);
		Envelope2D worldEnv = new Envelope2D();

		Envelope2D pixEnv = Envelope2D
				.construct(1, 1, m_width - 2, m_width - 2);

		double minWidth = toleranceXY * pixEnv.getWidth(); // min width is such
		// that the size of
		// one pixel is
		// equal to the
		// tolerance
		double minHeight = toleranceXY * pixEnv.getHeight();

		worldEnv.setCoords(m_geomEnv.getCenter(),
				Math.max(minWidth, m_geomEnv.getWidth()),
				Math.max(minHeight, m_geomEnv.getHeight()));

		m_stroke_half_widthX_pix = worldEnv.getWidth() / pixEnv.getWidth();
		m_stroke_half_widthY_pix = worldEnv.getHeight() / pixEnv.getHeight();

		// The stroke half width. Later it will be inflated to account for
		// pixels size.
		m_stroke_half_width = m_toleranceXY;

		m_transform = new Transformation2D();
		m_transform.initializeFromRect(worldEnv, pixEnv);// geom to pixels

		Transformation2D identityTransform = new Transformation2D();

		switch (geom.getType().value()) {
		case Geometry.GeometryType.MultiPoint:
			callback.setColor(m_rasterizer, 2);
     		fillPoints(m_rasterizer, (MultiPointImpl) geom, m_stroke_half_width);
			break;
		case Geometry.GeometryType.Polyline:
			callback.setColor(m_rasterizer, 2);
			strokeDrawPolyPath(m_rasterizer, (MultiPathImpl) geom._getImpl(),
					m_stroke_half_width);
			break;
		case Geometry.GeometryType.Polygon: {
			boolean isWinding = false;// NOTE: change when winding is supported
			callback.setColor(m_rasterizer, 1);
			fillMultiPath(m_rasterizer, m_transform, (MultiPathImpl) geom, isWinding);
			callback.setColor(m_rasterizer, 2);
			strokeDrawPolyPath(m_rasterizer, (MultiPathImpl) geom._getImpl(),
					m_stroke_half_width);
		}
			break;
		}

		m_dx = m_transform.xx;
		m_dy = m_transform.yy;
		m_x0 = m_transform.xd;
		m_y0 = m_transform.yd;
		buildLevels();
		//dbgSaveToBitmap("c:/temp/_dbg.bmp");
	}

	boolean tryRenderAsSmallEnvelope_(Envelope2D env) {
		if (!env.isIntersecting(m_geomEnv))
			return true;

		Envelope2D envPix = new Envelope2D();
		envPix.setCoords(env);
		m_transform.transform(env);
		double strokeHalfWidthPixX = m_stroke_half_widthX_pix;
		double strokeHalfWidthPixY = m_stroke_half_widthY_pix;
		if (envPix.getWidth() > 2 * strokeHalfWidthPixX + 1
				|| envPix.getHeight() > 2 * strokeHalfWidthPixY + 1)
			return false;

		// This envelope is too narrow/small, so that it can be just drawn as a
		// rectangle using only boundary color.

		envPix.inflate(strokeHalfWidthPixX, strokeHalfWidthPixY);
		envPix.xmax += 1.0;
		envPix.ymax += 1.0;// take into account that it does not draw right and
		// bottom edges.

		m_callback.setColor(m_rasterizer, 2);
		fillEnvelope(m_rasterizer, envPix);
		return true;
	}

	void buildLevels() {
		m_rasterizer.flush();
		int iStart = 0;
		int iStartNext = m_width * m_scanLineSize;
		int width = m_width;
		int widthNext = m_width / 2;
		int scanLineSize = m_scanLineSize;
		int scanLineSizeNext = (widthNext * 2 + 31) / 32;
		while (width > 8) {
			for (int iy = 0; iy < widthNext; iy++) {
				int iysrc1 = iy * 2;
				int iysrc2 = iy * 2 + 1;
				for (int ix = 0; ix < widthNext; ix++) {
					int ixsrc1 = ix * 2;
					int ixsrc2 = ix * 2 + 1;
					int divix1 = ixsrc1 >> 4;
					int modix1 = (ixsrc1 & 15) * 2;
					int divix2 = ixsrc2 >> 4;
					int modix2 = (ixsrc2 & 15) * 2;
					int res = (m_bitmap[iStart + scanLineSize * iysrc1 + divix1] >> modix1) & 3;
					res |= (m_bitmap[iStart + scanLineSize * iysrc1 + divix2] >> modix2) & 3;
					res |= (m_bitmap[iStart + scanLineSize * iysrc2 + divix1] >> modix1) & 3;
					res |= (m_bitmap[iStart + scanLineSize * iysrc2 + divix2] >> modix2) & 3;
					int divixDst = ix >> 4;
					int modixDst = (ix & 15) * 2;
					m_bitmap[iStartNext + scanLineSizeNext * iy + divixDst] |= res << modixDst;
				}
			}

			width = widthNext;
			scanLineSize = scanLineSizeNext;
			iStart = iStartNext;
			widthNext = width / 2;
			scanLineSizeNext = (widthNext * 2 + 31) / 32;
			iStartNext = iStart + scanLineSize * width;
		}
	}

	@Override
	public HitType queryPointInGeometry(double x, double y) {
		if (!m_geomEnv.contains(x, y))
			return HitType.Outside;
		
		int ix = worldToPixX(x);
		int iy = worldToPixY(y);
		if (ix < 0 || ix >= m_width || iy < 0 || iy >= m_width)
			return HitType.Outside;
		int divix = ix >> 4;
		int modix = (ix & 15) * 2;
		int res = (m_bitmap[m_scanLineSize * iy + divix] >> modix) & 3;
		if (res == 0)
			return HitType.Outside;
		else if (res == 1)
			return HitType.Inside;
		else
			return HitType.Border;
	}

	@Override
	public HitType queryEnvelopeInGeometry(Envelope2D env) {
		if (!env.intersect(m_geomEnv))
			return HitType.Outside;
		
		int ixmin = worldToPixX(env.xmin);
		int ixmax = worldToPixX(env.xmax);
		int iymin = worldToPixY(env.ymin);
		int iymax = worldToPixY(env.ymax);
		if (ixmin < 0)
			ixmin = 0;
		if (iymin < 0)
			iymin = 0;
		if (ixmax >= m_width)
			ixmax = m_width - 1;
		if (iymax >= m_width)
			iymax = m_width - 1;

		if (ixmin > ixmax || iymin > iymax)
			return HitType.Outside;

		int area = Math.max(ixmax - ixmin, 1) * Math.max(iymax - iymin, 1);
		int iStart = 0;
		int scanLineSize = m_scanLineSize;
		int width = m_width;
		int res = 0;
		while (true) {
			if (area < 32 || width < 16) {
				for (int iy = iymin; iy <= iymax; iy++) {
					for (int ix = ixmin; ix <= ixmax; ix++) {
						int divix = ix >> 4;
						int modix = (ix & 15) * 2;
						res = (m_bitmap[iStart + scanLineSize * iy + divix] >> modix) & 3; // read
						// two
						// bit
						// color.
						if (res > 1)
							return HitType.Border;
					}
				}

				if (res == 0)
					return HitType.Outside;
				else if (res == 1)
					return HitType.Inside;
			}

			iStart += scanLineSize * width;
			width /= 2;
			scanLineSize = (width * 2 + 31) / 32;
			ixmin /= 2;
			iymin /= 2;
			ixmax /= 2;
			iymax /= 2;
			area = Math.max(ixmax - ixmin, 1) * Math.max(iymax - iymin, 1);
		}
	}

	@Override
	public double getToleranceXY() {
		return m_toleranceXY;
	}

	@Override
	public int getRasterSize() {
		return m_width * m_scanLineSize;
	}

	@Override
	public boolean dbgSaveToBitmap(String fileName) {
		try {
			FileOutputStream outfile = new FileOutputStream(fileName);

			int height = m_width;
			int width = m_width;
			int sz = 14 + 40 + 4 * m_width * height;
			// Write the BITMAPFILEHEADER
			ByteBuffer byteBuffer = ByteBuffer.allocate(sz);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			// byteBuffer.put((byte) 'M');
			byteBuffer.put((byte) 66);
			byteBuffer.put((byte) 77);
			// fwrite("BM", 1, 2, f); //bfType
			byteBuffer.putInt(sz);
			// fwrite(&sz, 1, 4, f);//bfSize
			short zero16 = 0;
			byteBuffer.putShort(zero16);
			// fwrite(&zero16, 1, 2, f);//bfReserved1
			byteBuffer.putShort(zero16);
			// fwrite(&zero16, 1, 2, f);//bfReserved2
			int offset = 14 + 40;
			byteBuffer.putInt(offset);
			// fwrite(&offset, 1, 4, f);//bfOffBits

			// Write the BITMAPINFOHEADER
			int biSize = 40;
			int biWidth = width;
			int biHeight = -height;
			short biPlanes = 1;
			short biBitCount = 32;
			int biCompression = 0;
			int biSizeImage = 4 * width * height;
			int biXPelsPerMeter = 0;
			int biYPelsPerMeter = 0;
			int biClrUsed = 0;
			int biClrImportant = 0;
			byteBuffer.putInt(biSize);
			byteBuffer.putInt(biWidth);
			byteBuffer.putInt(biHeight);
			byteBuffer.putShort(biPlanes);
			byteBuffer.putShort(biBitCount);
			byteBuffer.putInt(biCompression);
			byteBuffer.putInt(biSizeImage);
			byteBuffer.putInt(biXPelsPerMeter);
			byteBuffer.putInt(biYPelsPerMeter);
			byteBuffer.putInt(biClrUsed);
			byteBuffer.putInt(biClrImportant);

			int colors[] = { 0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFF00FF00 };
			// int32_t* rgb4 = (int32_t*)malloc(biSizeImage);
			for (int y = 0; y < height; y++) {
				int scanlineIn = y * ((width * 2 + 31) / 32);
				int scanlineOut = offset + width * y;

				for (int x = 0; x < width; x++) {
					int res = (m_bitmap[scanlineIn + (x >> 4)] >> ((x & 15) * 2)) & 3;
					byteBuffer.putInt(colors[res]);
				}
			}

			byte[] b = byteBuffer.array();
			outfile.write(b);
			outfile.close();
			return true;
		} catch (IOException ex) {
			return false;

		}
	}
}
