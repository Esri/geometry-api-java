/*
 Copyright 2013-2015 Esri

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Simple scanline rasterizer. Caller provides a callback to draw pixels to actual surface.
 *
 */
public class SimpleRasterizer {
	
	/**
	 * Even odd fill rule
	 */
	public final static int EVEN_ODD = 0;
	
	/**
	 * Winding fill rule
	 */
	public final static int WINDING = 1;
	
	public static interface ScanCallback {
		/**
		 * Rasterizer calls this method for each scan it produced
		 * @param scans array of scans. Scans are triplets of numbers. The start X coordinate for the scan (inclusive),
		 * the end X coordinate of the scan (exclusive), the Y coordinate for the scan.
		 * @param scanCount3 The number of initialized elements in the scans array. The scan count is scanCount3 / 3. 
		 */
		public abstract void drawScan(int[] scans, int scanCount3);
	}
	
	public SimpleRasterizer() {
		width_ = -1;
		height_ = -1;
	}
	
	/**
	 * Sets up the rasterizer.
	 */
	public void setup(int width, int height, ScanCallback callback)
	{
		width_ = width; height_ = height;
		ySortedEdges_ = null;
		activeEdgesTable_ = null;
		numEdges_ = 0;
		callback_ = callback;
		if (scanBuffer_ == null)
			scanBuffer_ = new int[128 * 3];
		
		startAddingEdges();
	}
	
	public final int getWidth() {
		return width_;
	}
	
	public final int getHeight() {
		return height_;
	}

	/**
	 * Flushes any cached scans.
	 */
	public final void flush() {
		if (scanPtr_ > 0) {
			callback_.drawScan(scanBuffer_, scanPtr_);
			scanPtr_ = 0;
		}
	}
	
	/**
	 * Adds edges of a triangle.
	 */
	public final void addTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
		addEdge(x1, y1, x2, y2);
		addEdge(x2, y2, x3, y3);
		addEdge(x1, y1, x3, y3);
	}

	/**
	 * Adds edges of the ring to the rasterizer.
	 * @param xy interleaved coordinates x1, y1, x2, y2,...
	 */
	public final void addRing(double xy[]) {
		for (int i = 2; i < xy.length; i += 2) {
			addEdge(xy[i-2], xy[i - 1], xy[i], xy[i + 1]);
		}
	}
	
	/**
	 * Call before starting the edges.
	 *
	 * For example to render two polygons that consist of a single ring:
	 * startAddingEdges();
	 * addRing(...);
	 * renderEdges(Rasterizer.EVEN_ODD);
	 * addRing(...);
	 * renderEdges(Rasterizer.EVEN_ODD);
	 *
	 * For example to render a polygon consisting of three rings:
	 * startAddingEdges();
	 * addRing(...);
	 * addRing(...);
	 * addRing(...);
	 * renderEdges(Rasterizer.EVEN_ODD);
	 */
	public final void startAddingEdges() {
		if (numEdges_ > 0) {
			for (int i = 0; i < height_; i++) {
				for (Edge e = ySortedEdges_[i]; e != null;) {
					Edge p = e;
					e = e.next;
					p.next = null;
				}
				
				ySortedEdges_[i] = null;
			}

			activeEdgesTable_ = null;
		}
		
		minY_ = height_;
		maxY_ = -1;
		numEdges_ = 0;	
	}
	
	/**
	 * Renders all edges added so far, and removes them.
	 * Calls startAddingEdges after it's done.
	 * @param fillMode Fill mode for the polygon fill can be one of two values: EVEN_ODD or WINDING.
	 *
	 * Note, as any other graphics algorithm, the scan line rasterizer doesn't require polygons
	 * to be topologically simple, or have correct ring orientation.
	 */
	public final void renderEdges(int fillMode) {
		evenOdd_ = fillMode == EVEN_ODD;
		for (int line = minY_; line <= maxY_; line++) {
			advanceAET_();
			addNewEdgesToAET_(line);
			emitScans_();
		}
		
		startAddingEdges();//reset for new edges
	}
	
	/**
	 * Add a single edge. 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public final void addEdge(double x1, double y1, double x2, double y2) {
		if (y1 == y2)
			return;
			
		int dir = 1;
		if (y1 > y2) {
			double temp;
			temp = x1; x1 = x2; x2 = temp;
			temp = y1; y1 = y2; y2 = temp;
			dir = -1;
		}
		
		if (y2 < 0 || y1 >= height_)
			return;

		if (x1 < 0 && x2 < 0)
		{
			x1 = -1; x2 = -1;
		}
		else if (x1 >= width_ && x2 >= width_)
		{
			x1 = width_; x2 = width_;
		}
		
		//clip to extent
		double dxdy = (x2 - x1) / (y2 - y1);

		if (y2 > height_) {
			y2 = height_;
			x2 = dxdy * (y2 - y1) + x1;
		}
		
		if (y1 < 0) {
			x1 = dxdy * (0 - y1) + x1;
			y1 = 0;
		}

		//do not clip x unless it is too small or too big
		int bigX = Math.max(width_ + 1, 0x7fffff);
		if (x1 < -0x7fffff) {
		    //from earlier logic, x2 >= -1, therefore dxdy is not 0
			y1 = (0 - x1) / dxdy + y1;
			x1 = 0;
		}
		else if (x1 > bigX) {
			//from earlier logic, x2 <= width_, therefore dxdy is not 0
			y1 = (width_ - x1) / dxdy + y1;
			x1 = width_;
		}

		if (x2 < -0x7fffff) {
			//from earlier logic, x1 >= -1, therefore dxdy is not 0
			y2 = (0 - x1) / dxdy + y1;
			x2 = 0;
		}
		else if (x2 > bigX) {
			//from earlier logic, x1 <= width_, therefore dxdy is not 0
			y2 = (width_ - x1) / dxdy + y1;
			x2 = width_;
		}
		
		int ystart = (int)y1;
		int yend = (int)y2;
		if (ystart == yend)
			return;
		
		Edge e = new Edge();
		
		e.x = (long)(x1 * 4294967296.0);
		e.y = ystart;
		e.ymax = yend;
		e.dxdy = (long)(dxdy * 4294967296.0);
		e.dir = dir;
		
		if (ySortedEdges_ == null) {
			ySortedEdges_ = new Edge[height_];
		}

		e.next = ySortedEdges_[e.y];
		ySortedEdges_[e.y] = e;
		
		if (e.y < minY_)
			minY_ = e.y;
		
		if (e.ymax > maxY_)
			maxY_ = e.ymax;
		
		numEdges_++;
	}
	
	public final void fillEnvelope(Envelope2D envIn) {
		Envelope2D env = new Envelope2D(0, 0, width_, height_);
		if (!env.intersect(envIn))
			return;

		int x0 = (int)env.xmin;
		int x = (int)env.xmax;

		int xn = NumberUtils.snap(x0, 0, width_);
		int xm = NumberUtils.snap(x, 0, width_);
		if (x0 < width_ && xn < xm) {
			int y0 = (int)env.ymin;
			int y1 = (int)env.ymax;
			y0 = NumberUtils.snap(y0, 0, height_);
			y1 = NumberUtils.snap(y1, 0, height_);
			if (y0 < height_) {
				for (int y = y0; y < y1; y++) {
					scanBuffer_[scanPtr_++] = xn;
					scanBuffer_[scanPtr_++] = xm;
					scanBuffer_[scanPtr_++] = y;
					if (scanPtr_ == scanBuffer_.length) {
						callback_.drawScan(scanBuffer_, scanPtr_);
						scanPtr_ = 0;
					}
				}
			}
		}
	}
	
    final boolean addSegmentStroke(double x1, double y1, double x2, double y2, double half_width, boolean skip_short, double[] helper_xy_10_elm)
    {
      double vec_x = x2 - x1;
      double vec_y = y2 - y1;
      double len = Math.sqrt(vec_x * vec_x + vec_y * vec_y);
      if (skip_short && len < 0.5)
        return false;

      boolean bshort = len < 0.00001;
      if (bshort)
      {
        len = 0.00001;
        vec_x = len;
        vec_y = 0.0;
      }

      double f = half_width / len;
      vec_x *= f; vec_y *= f;
      double vecA_x = -vec_y;
      double vecA_y = vec_x;
      double vecB_x = vec_y;
      double vecB_y = -vec_x;
      //extend by half width
      x1 -= vec_x;
      y1 -= vec_y;
      x2 += vec_x;
      y2 += vec_y;
      //create rotated rectangle
      double[] fan = helper_xy_10_elm;
      assert(fan.length == 10);
      fan[0] = x1 + vecA_x;
      fan[1] = y1 + vecA_y;//fan[0].add(pt_start, vecA);
      fan[2] = x1 + vecB_x;
      fan[3] = y1 + vecB_y;//fan[1].add(pt_start, vecB);
      fan[4] = x2 + vecB_x;
      fan[5] = y2 + vecB_y;//fan[2].add(pt_end, vecB)
      fan[6] = x2 + vecA_x;
      fan[7] = y2 + vecA_y;//fan[3].add(pt_end, vecA)
      fan[8] = fan[0];
      fan[9] = fan[1];
      addRing(fan);
      return true;
    }
	
	public final ScanCallback getScanCallback() { return callback_; }
	
	
	//PRIVATE
	
	private static class Edge {
		long x;
		long dxdy;
		int y;
		int ymax;
		int dir;
		Edge next;
	}
	
	private final void advanceAET_() {
		if (activeEdgesTable_ == null)
			return;
		
		boolean needSort = false;
		Edge prev = null;
		for (Edge e = activeEdgesTable_; e != null; ) {
			e.y++;
			if (e.y == e.ymax) {
				Edge p = e; e = e.next;
				if (prev != null)
					prev.next = e;
				else
					activeEdgesTable_ = e;
				
				p.next = null;
				continue;
			}
			
			e.x += e.dxdy;
			if (prev != null && prev.x > e.x)
				needSort = true;
			
			prev = e;
			e = e.next;
		}
		
		if (needSort) {
			//resort to fix the order
			activeEdgesTable_ = sortAET_(activeEdgesTable_);
		}
	}
	
	private final void addNewEdgesToAET_(int y) {
		if (y >= height_)
			return;

		Edge edgesOnLine = ySortedEdges_[y];
		if (edgesOnLine != null) {
			ySortedEdges_[y] = null;
			edgesOnLine = sortAET_(edgesOnLine);//sort new edges
			numEdges_ -= sortedNum_;//set in the sortAET

			// merge the edges with sorted AET - O(n) operation
			Edge aet = activeEdgesTable_;
			boolean first = true; 
			Edge newEdge = edgesOnLine;
			Edge prev_aet = null;
			while (aet != null && newEdge != null) {
				if (aet.x > newEdge.x) {
					if (first)
						activeEdgesTable_ = newEdge;
					
					Edge p = newEdge.next;
					newEdge.next = aet;
					if (prev_aet != null) {
						prev_aet.next = newEdge;
					}
					
					prev_aet = newEdge;
					newEdge = p;
				} else { // aet.x <= newEdges.x
					Edge p = aet.next;
					aet.next = newEdge;
					if (prev_aet != null)
						prev_aet.next = aet;
					
					prev_aet = aet;
					aet = p;
				}
				
				first = false;
			}
			
			if (activeEdgesTable_ == null)
				activeEdgesTable_ = edgesOnLine;
		}
	}

	private static int snap_(int x, int mi, int ma) {
		return x < mi ? mi : x > ma ? ma : x;
	}
	
	private final void emitScans_() {
		if (activeEdgesTable_ == null)
			return;
		
		int w = 0;
		Edge e0 = activeEdgesTable_;
		int x0 = (int)(e0.x >> 32);
		for (Edge e = e0.next; e != null; e = e.next) {
			if (evenOdd_)
				w ^= 1;
			else
				w += e.dir;
			
			if (e.x > e0.x) {
				int x = (int)(e.x >> 32);
				if (w != 0) {
					int xx0 = snap_(x0, 0, width_);
					int xx = snap_(x, 0, width_);
					if (xx > xx0 && xx0 < width_) {
						scanBuffer_[scanPtr_++] = xx0;
						scanBuffer_[scanPtr_++] = xx;
						scanBuffer_[scanPtr_++] = e.y;
						if (scanPtr_ == scanBuffer_.length) {
							callback_.drawScan(scanBuffer_, scanPtr_);
							scanPtr_ = 0;
						}
					}
				}
				
				e0 = e;
				x0 = x;
			}
		}
	}
	
	static private class EdgeComparator implements Comparator<Edge> {
		@Override
		public int compare(Edge o1, Edge o2) {
			if (o1 == o2)
				return 0;
			
			return o1.x < o2.x ? -1 : o1.x > o2.x ? 1 : 0;
		}
	}
	
	private final static EdgeComparator edgeCompare_ = new EdgeComparator();
 	
	private final Edge sortAET_(Edge aet) {
		int num = 0;
		for (Edge e = aet; e != null; e = e.next)
			num++;
		
		sortedNum_ = num;
		if (num == 1)
			return aet;
		
		if (sortBuffer_ == null)
			sortBuffer_ = new Edge[Math.max(num, 16)];
		
		else if (sortBuffer_.length < num)
			sortBuffer_ = new Edge[Math.max(num, sortBuffer_.length * 2)];
		
		{
			int i = 0;
			for (Edge e = aet; e != null; e = e.next)
				sortBuffer_[i++] = e;
		}
		
		if (num == 2) {
			if (sortBuffer_[0].x > sortBuffer_[1].x) {
				Edge tmp = sortBuffer_[0];
				sortBuffer_[0] = sortBuffer_[1];
				sortBuffer_[1] = tmp;
			}
		}
		else {
			Arrays.sort(sortBuffer_, 0, num, edgeCompare_);
		}

		aet = sortBuffer_[0]; sortBuffer_[0] = null;
		Edge prev = aet;
		for (int i = 1; i < num; i++) {
			prev.next = sortBuffer_[i];
			prev = sortBuffer_[i];
			sortBuffer_[i] = null;
		}
		
		prev.next = null;
		return aet;
	}
		
	private Edge activeEdgesTable_;
	private Edge[] ySortedEdges_;
	private Edge[] sortBuffer_;
	private int[] scanBuffer_;
	int scanPtr_;
	private ScanCallback callback_;
	private int width_;
	private int height_;
	private int minY_;
	private int maxY_;
	private int numEdges_;
	private int sortedNum_;
	private boolean evenOdd_;
}

