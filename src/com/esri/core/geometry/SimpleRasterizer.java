/*
 Copyright 2013 Esri

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
		 * @param y the Y coordinate for the scan
		 * @param x the X coordinate for the scan
		 * @param numPxls the number of pixels in the scan
		 */
		public abstract void drawScan(int y, int x, int numPxls);
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
		startAddingEdges();
	}
	
	public int getWidth() {
		return width_;
	}
	
	public int getHeight() {
		return height_;
	}

	/**
	 * Adds edges of a triangle.
	 */
	public void addTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
		addEdge(x1, y1, x2, y2);
		addEdge(x2, y2, x3, y3);
		addEdge(x1, y1, x3, y3);
	}
	
	/**
	 * Adds edges of the ring to the rasterizer.
	 * @param xy interleaved coordinates x1, y1, x2, y2,...
	 */
	public void addRing(double xy[]) {
		for (int i = 2; i < xy.length; i += 2) {
			addEdge(xy[i-2], xy[i - 1], xy[i], xy[i + 1]);
		}
	}
	
	/**
	 * Call before starting the edges.
	 * For example to render two polygons that consist of a single ring:
	 * startAddingEdges();
	 * addRing(...);
	 * renderEdges(Rasterizer.EVEN_ODD);
	 * addRing(...);
	 * renderEdges(Rasterizer.EVEN_ODD);
	 */
	public void startAddingEdges() {
		if (numEdges_ > 0) {
			ySortedEdges_ = null;
			activeEdgesTable_ = null;
		}
		
		minY_ = height_;
		maxY_ = -1;
		numEdges_ = 0;	
	}
	
	/**
	 * Renders all edges added so far, and removes them.
	 * @param fillMode
	 */
	public void renderEdges(int fillMode) {
		evenOdd_ = fillMode == EVEN_ODD;
		for (int line = minY_; line <= maxY_; line++) {
			advanceAET_();
			addNewEdgesToAET_(line);
			emitScans_();
		}
		
		numEdges_ = 0;
		if (activeEdgesTable_ != null)
			activeEdgesTable_.clear();
		
		startAddingEdges();//reset for new edges
	}
	
	/**
	 * Add a single edge. 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public void addEdge(double x1, double y1, double x2, double y2) {
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

		//We know that dxdy != 0, otherwise it would return earlier
		//do not clip x unless it is too small or too big
		int bigX = Math.max(width_ + 1, 0x7fffff);
		if (x1 < -0x7fffff) {
			
			y1 = (0 - x1) / dxdy + y1;
			x1 = 0;
		}
		else if (x1 > bigX) {
			//we know that dx != 0, otherwise it would return earlier
			y1 = (width_ - x1) / dxdy + y1;
			x1 = width_;
		}

		if (x2 < -0x7fffff) {
			//we know that dx != 0, otherwise it would return earlier
			y2 = (0 - x1) / dxdy + y1;
			x2 = 0;
		}
		else if (x2 > bigX) {
			//we know that dx != 0, otherwise it would return earlier
			y2 = (width_ - x1) / dxdy + y1;
			x2 = width_;
		}
		
		int ystart = (int)y1;
		int yend = (int)y2;
		if (ystart == yend)
			return;
		
		Edge e;
		if (recycledEdges_ != null && recycledEdges_.size() > 0)
			e = recycledEdges_.remove(recycledEdges_.size() - 1);
		else
			e = new Edge();
		
		e.x = (long)(x1 * 4294967296.0);
		e.y = ystart;
		e.ymax = yend;
		e.dxdy = (long)(dxdy * 4294967296.0);
		e.dir = dir;
		
		if (ySortedEdges_ == null) {
			ySortedEdges_ = new ArrayList<ArrayList<Edge>>();
			ySortedEdges_.ensureCapacity(height_);
			for (int i = 0; i < height_; i++) {
				ySortedEdges_.add(null);
			}
		}

		if (ySortedEdges_.get(e.y) == null) {
			ySortedEdges_.set(e.y, new ArrayList<Edge>());
		}
		
		ySortedEdges_.get(e.y).add(e);
		if (e.y < minY_)
			minY_ = e.y;
		
		if (e.ymax > maxY_)
			maxY_ = e.ymax;
	}
	
	class Edge {
		long x;
		long dxdy;
		int y;
		int ymax;
		int dir;
	}
	
	private void advanceAET_() {
		if (activeEdgesTable_ != null && activeEdgesTable_.size() > 0) {
			for (int i = 0, n = activeEdgesTable_.size(); i < n; i++) {
				Edge e = activeEdgesTable_.get(i);
				e.y++;
				if (e.y == e.ymax) {
					if (recycledEdges_ == null) {
						recycledEdges_ = new ArrayList<Edge>();
					}
					
					recycledEdges_.add(e);
					activeEdgesTable_.set(i, null);
					continue;
				}
				
				e.x += e.dxdy;
			}
		}
	}
	
	private void addNewEdgesToAET_(int y) {
		if (y >= ySortedEdges_.size())
			return;
		
		if (activeEdgesTable_ == null)
			activeEdgesTable_ = new ArrayList<Edge>();
		
		ArrayList<Edge> edgesOnLine = ySortedEdges_.get(y);
		if (edgesOnLine != null) {
			for (int i = 0, n = edgesOnLine.size(); i < n; i++) {
				activeEdgesTable_.add(edgesOnLine.get(i));
			}
			
			edgesOnLine.clear();
		}
	}

	static int snap_(int x, int mi, int ma) {
		return x < mi ? mi : x > ma ? ma : x;
	}
	private void emitScans_() {
		sortAET_();

		if (activeEdgesTable_ == null || activeEdgesTable_.size() == 0)
			return;
		
		int w = 0;
		Edge e0 = activeEdgesTable_.get(0);
		int x0 = (int)(e0.x >> 32);
		for (int i = 1; i < activeEdgesTable_.size(); i++) {
			Edge e = activeEdgesTable_.get(i);
			if (evenOdd_)
				w ^= 1;
			else
				w += e.dir;
			
			if (e.x > e0.x) {
				int x = (int)(e.x >> 32);
				if (w == 1) {
					int xx0 = snap_(x0, 0, width_);
					int xx = snap_(x, 0, width_);
					if (xx > xx0 && xx0 < width_) {
						callback_.drawScan(e.y, xx0, xx - xx0);
					}
				}
				
				e0 = e;
				x0 = x;
			}
		}
	}
	
	static class EdgeComparator implements Comparator<Edge> {
		@Override
		public int compare(Edge o1, Edge o2) {
			if (o1 == null)
				return o2 == null ? 0 : 1;
			else if (o2 == null)
				return -1;
				
			return o1.x < o2.x ? -1 : o1.x > o2.x ? 1 : 0;
		}
	}
	
	private static EdgeComparator edgeCompare_ = new EdgeComparator();
 	
	private void sortAET_() {
		if (!checkAETIsSorted_())
		{
			Collections.sort(activeEdgesTable_, edgeCompare_);
			while (activeEdgesTable_.size() > 0 && activeEdgesTable_.get(activeEdgesTable_.size() - 1) == null)
				activeEdgesTable_.remove(activeEdgesTable_.size() - 1);
		}
	}
	
	private boolean checkAETIsSorted_() {
		if (activeEdgesTable_ == null || activeEdgesTable_.size() == 0)
			return true;
		
		Edge e0 = activeEdgesTable_.get(0);
		if (e0 == null)
			return false;
		
		for (int i = 1; i < activeEdgesTable_.size(); i++) {
			Edge e = activeEdgesTable_.get(i);
			if (e == null || e.x < e0.x) {
				return false;
			}
			e0 = e;
		}
		
		return true;
	}
	
	private ArrayList<Edge> recycledEdges_;
	private ArrayList<Edge> activeEdgesTable_;
	private ArrayList<ArrayList<Edge>> ySortedEdges_;
	public ScanCallback callback_;
	private int width_;
	private int height_;
	private int minY_;
	private int maxY_;
	private int numEdges_;
	private boolean evenOdd_;
}
