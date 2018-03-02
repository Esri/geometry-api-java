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

import static com.esri.core.geometry.SizeOf.SIZE_OF_MULTI_PATH_IMPL;

final class MultiPathImpl extends MultiVertexGeometryImpl {
	protected boolean m_bPolygon;
	protected Point m_moveToPoint;
	protected double m_cachedLength2D;
	protected double m_cachedArea2D;

	protected AttributeStreamOfDbl m_cachedRingAreas2D;
	protected boolean m_bPathStarted;

	// Contains starting points of the parts. The size is getPartCount() + 1.
	// First element is 0, last element is equal to the getPointCount().
	protected AttributeStreamOfInt32 m_paths;
	// same size as m_parts. Holds flags for each part (whether the part is
	// closed, etc. See PathFlags)
	protected AttributeStreamOfInt8 m_pathFlags;
	// The segment flags. Size is getPointCount(). This is not a vertex
	// attribute, because we may want to use indexed access later (via an index
	// buffer).
	// Can be NULL if the MultiPathImpl contains straight lines only.
	protected AttributeStreamOfInt8 m_segmentFlags;
	// An index into the m_segmentParams stream. Size is getPointCount(). Can be
	// NULL if the MultiPathImpl contains straight lines only.
	protected AttributeStreamOfInt32 m_segmentParamIndex;
	protected AttributeStreamOfDbl m_segmentParams;
	protected int m_curveParamwritePoint;
	private int m_currentPathIndex;
	private int m_fill_rule = Polygon.FillRule.enumFillRuleOddEven;

	static int[] _segmentParamSizes = { 0, 0, 6, 0, 8, 0 }; // None, Line,
															// Bezier, XXX, Arc,
															// XXX;

	@Override
	public long estimateMemorySize()
	{
		long size = SIZE_OF_MULTI_PATH_IMPL +
			+ (m_envelope != null ? m_envelope.estimateMemorySize() : 0)
			+ (m_moveToPoint != null ? m_moveToPoint.estimateMemorySize() : 0)
			+ (m_cachedRingAreas2D != null ? m_cachedRingAreas2D.estimateMemorySize() : 0)
			+ m_paths.estimateMemorySize()
			+ m_pathFlags.estimateMemorySize()
			+ (m_segmentFlags != null ? m_segmentFlags.estimateMemorySize() : 0)
			+ (m_segmentParamIndex != null ? m_segmentParamIndex.estimateMemorySize() : 0)
			+ (m_segmentParams != null ? m_segmentParams.estimateMemorySize() : 0);

		if (m_vertexAttributes != null) {
			for (int i = 0; i < m_vertexAttributes.length; i++) {
				size += m_vertexAttributes[i].estimateMemorySize();
			}
		}
		return size;
	}

	public boolean hasNonLinearSegments() {
		return m_curveParamwritePoint > 0;
	}

	// / Cpp ///
	// Reviewed vs. Native Jan 11, 2011
	public MultiPathImpl(boolean bPolygon) {
		m_bPolygon = bPolygon;

		m_bPathStarted = false;
		m_curveParamwritePoint = 0;
		m_cachedLength2D = 0;
		m_cachedArea2D = 0;
		m_pointCount = 0;
		m_description = VertexDescriptionDesignerImpl.getDefaultDescriptor2D();
		m_cachedRingAreas2D = null;
		m_currentPathIndex = 0;
	}

	// Reviewed vs. Native Jan 11, 2011
	public MultiPathImpl(boolean bPolygon, VertexDescription description) {
		if (description == null)
			throw new IllegalArgumentException();

		m_bPolygon = bPolygon;

		m_bPathStarted = false;
		m_curveParamwritePoint = 0;
		m_cachedLength2D = 0;
		m_cachedArea2D = 0;
		m_pointCount = 0;
		m_description = description;
		m_cachedRingAreas2D = null;
		m_currentPathIndex = 0;
	}

	// Reviewed vs. Native Jan 11, 2011
	protected void _initPathStartPoint() {
		_touch();
		if (m_moveToPoint == null)
			m_moveToPoint = new Point(m_description);
		else
			m_moveToPoint.assignVertexDescription(m_description);
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * Starts a new Path at the Point.
	 */
	public void startPath(double x, double y) {
		Point2D endPoint = new Point2D();
		endPoint.x = x;
		endPoint.y = y;
		startPath(endPoint);
	}

	// Reviewed vs. Native Jan 11, 2011
	public void startPath(Point2D point) {
		_initPathStartPoint();
		m_moveToPoint.setXY(point);
		m_bPathStarted = true;
	}

	// Reviewed vs. Native Jan 11, 2011
	public void startPath(Point3D point) {
		_initPathStartPoint();
		m_moveToPoint.setXYZ(point);
		assignVertexDescription(m_moveToPoint.getDescription());
		m_bPathStarted = true;
	}

	// Reviewed vs. Native Jan 11, 2011
	public void startPath(Point point) {
		if (point.isEmpty())
			throw new IllegalArgumentException();// throw new
													// IllegalArgumentException();

		mergeVertexDescription(point.getDescription());
		_initPathStartPoint();
		point.copyTo(m_moveToPoint);

		// TODO check MultiPathImpl.cpp comment
		// "//the description will be merged later"
		// assignVertexDescription(m_moveToPoint.getDescription());
		m_bPathStarted = true;
	}

	// Reviewed vs. Native Jan 11, 2011
	protected void _beforeNewSegment(int resizeBy) {
		// Called for each new segment being added.
		if (m_bPathStarted) {
			_initPathStartPoint();// make sure the m_movetoPoint exists and has
									// right vertex description

			// The new path is started. Need to grow m_parts and m_pathFlags.
			if (m_paths == null) {
				m_paths = (AttributeStreamOfInt32) AttributeStreamBase
						.createIndexStream(2);
				m_paths.write(0, 0);
				m_pathFlags = (AttributeStreamOfInt8) AttributeStreamBase
						.createByteStream(2, (byte) 0);
			} else {
				// _ASSERT(m_parts.size() >= 2);
				m_paths.resize(m_paths.size() + 1, 0);
				m_pathFlags.resize(m_pathFlags.size() + 1, 0);
			}

			if (m_bPolygon) {
				// Mark the path as closed
				m_pathFlags.write(m_pathFlags.size() - 2,
						(byte) PathFlags.enumClosed);
			}

			resizeBy++; // +1 for the StartPath point.
		}

		int oldcount = m_pointCount;
		m_paths.write(m_paths.size() - 1, m_pointCount + resizeBy); // The
																	// NotifyModified
																	// will
																	// update
																	// the
																	// m_pointCount
																	// with this
																	// value.
		_resizeImpl(oldcount + resizeBy);
		m_pathFlags.write(m_paths.size() - 1, (byte) 0);

		if (m_bPathStarted) {
			setPointByVal(oldcount, m_moveToPoint);// setPoint(oldcount,
													// m_moveToPoint); //finally
													// set the start point to
													// the geometry
			m_bPathStarted = false;
		}
	}

	// Reviewed vs. Native Jan 11, 2011
	protected void _finishLineTo() {
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * adds a Line Segment from the last Point to the given endPoint.
	 */
	public void lineTo(double x, double y) {
		_beforeNewSegment(1);
		setXY(m_pointCount - 1, x, y);
		_finishLineTo();
		// Point2D endPoint = new Point2D();
		// endPoint.x = x; endPoint.y = y;
		// lineTo(endPoint);
	}

	// Reviewed vs. Native Jan 11, 2011
	public void lineTo(Point2D endPoint) {
		_beforeNewSegment(1);
		setXY(m_pointCount - 1, endPoint);
		_finishLineTo();
	}

	// Reviewed vs. Native Jan 11, 2011
	public void lineTo(Point3D endPoint) {
		_beforeNewSegment(1);
		setXYZ(m_pointCount - 1, endPoint);
		_finishLineTo();
	}

	// Reviewed vs. Native Jan 11, 2011
	public void lineTo(Point endPoint) {
		_beforeNewSegment(1);
		setPointByVal(m_pointCount - 1, endPoint);
		_finishLineTo();
	}

	// Reviewed vs. Native Jan 11, 2011
	protected void _initSegmentData(int sz) {
		if (m_segmentParamIndex == null) {
			m_segmentFlags = (AttributeStreamOfInt8) AttributeStreamBase
					.createByteStream(m_pointCount,
							(byte) SegmentFlags.enumLineSeg);
			m_segmentParamIndex = (AttributeStreamOfInt32) AttributeStreamBase
					.createIndexStream(m_pointCount, -1);
		}

		int size = m_curveParamwritePoint + sz;
		if (m_segmentParams == null) {
			m_segmentParams = (AttributeStreamOfDbl) AttributeStreamBase
					.createAttributeStreamWithPersistence(
							VertexDescription.Persistence.enumDouble, size);
		} else {
			m_segmentParams.resize(size, 0);
		}
	}

	// Reviewed vs. Native Jan 11, 2011
	protected void _finishBezierTo() {
		// _ASSERT(m_segmentFlags != null);
		// _ASSERT(m_segmentParamIndex != null);

		m_segmentFlags.write(m_pointCount - 2,
				(byte) SegmentFlags.enumBezierSeg);
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * adds a Cubic Bezier Segment to the current Path. The Bezier Segment
	 * connects the current last Point and the given endPoint.
	 */
	public void bezierTo(Point2D controlPoint1, Point2D controlPoint2,
			Point2D endPoint) {
		_beforeNewSegment(1);
		setXY(m_pointCount - 1, endPoint);
		double z;
		_initSegmentData(6);
		m_pathFlags.setBits(m_pathFlags.size() - 1,
				(byte) PathFlags.enumHasNonlinearSegments);
		m_segmentParamIndex.write(m_pointCount - 2, m_curveParamwritePoint);
		m_curveParamwritePoint += 6;
		int curveIndex = m_curveParamwritePoint;
		m_segmentParams.write(curveIndex, controlPoint1.x);
		m_segmentParams.write(curveIndex + 1, controlPoint1.y);
		z = 0;// TODO: calculate me.
		m_segmentParams.write(curveIndex + 2, z);
		m_segmentParams.write(curveIndex + 3, controlPoint2.x);
		m_segmentParams.write(curveIndex + 4, controlPoint2.y);
		z = 0;// TODO: calculate me.
		m_segmentParams.write(curveIndex + 5, z);
		_finishBezierTo();
	}

	// Reviewed vs. Native Jan 11, 2011
	public void openPath(int pathIndex) {
		_touch();
		if (m_bPolygon)
			throw GeometryException.GeometryInternalError();// do not call this
															// method on a
															// polygon

		int pathCount = getPathCount();
		if (pathIndex > getPathCount())
			throw new IllegalArgumentException();

		if (m_pathFlags == null)
			throw GeometryException.GeometryInternalError();

		m_pathFlags.clearBits(pathIndex, (byte) PathFlags.enumClosed);
	}

	public void openPathAndDuplicateStartVertex(int pathIndex) {
		_touch();
		if (m_bPolygon)
			throw GeometryException.GeometryInternalError();// do not call this
															// method on a
															// polygon

		int pathCount = getPathCount();
		if (pathIndex > pathCount)
			throw GeometryException.GeometryInternalError();

		if (!isClosedPath(pathIndex))
			return;// do not open if open

		if (m_pathFlags == null)// if (!m_pathFlags)
			throw GeometryException.GeometryInternalError();

		int oldPointCount = m_pointCount;
		int pathIndexStart = getPathStart(pathIndex);
		int pathIndexEnd = getPathEnd(pathIndex);
		_resizeImpl(m_pointCount + 1); // resize does not write into m_paths
										// anymore!
		_verifyAllStreams();
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			if (m_vertexAttributes[iattr] != null)// if
													// (m_vertexAttributes[iattr])
			{
				int semantics = m_description._getSemanticsImpl(iattr);
				int comp = VertexDescription.getComponentCount(semantics);
				m_vertexAttributes[iattr].insertRange(comp * pathIndexEnd,
						m_vertexAttributes[iattr], comp * pathIndexStart, comp,
						true, 1, comp * oldPointCount);
			}
		}

		for (int ipath = pathCount; ipath > pathIndex; ipath--) {
			int iend = m_paths.read(ipath);
			m_paths.write(ipath, iend + 1);
		}

		m_pathFlags.clearBits(pathIndex, (byte) PathFlags.enumClosed);
	}

	// Reviewed vs. Native Jan 11, 2011
	// Major Changes on 16th of January
	public void openAllPathsAndDuplicateStartVertex() {
		_touch();
		if (m_bPolygon)
			throw GeometryException.GeometryInternalError();// do not call this
															// method on a
															// polygon

		if (m_pathFlags == null)// if (!m_pathFlags)
			throw GeometryException.GeometryInternalError();

		_verifyAllStreams();

		int closedPathCount = 0;
		int pathCount = getPathCount();
		for (int i = 0; i < pathCount; i++) {
			if (m_pathFlags.read(i) == (byte) PathFlags.enumClosed) {
				closedPathCount++;
			}
		}

		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			if (m_vertexAttributes[iattr] != null) {
				int semantics = m_description._getSemanticsImpl(iattr);// int
																		// semantics
																		// =
																		// m_description._getSemanticsImpl(iattr);
				int comp = VertexDescription.getComponentCount(semantics);
				int newSize = comp * (m_pointCount + closedPathCount);
				m_vertexAttributes[iattr].resize(newSize);

				int offset = closedPathCount;
				int ipath = pathCount;
				for (int i = m_pointCount - 1; i >= 0; i--) {
					if (i + 1 == m_paths.read(ipath)) {
						ipath--;
						if (m_pathFlags.read(ipath) == (byte) PathFlags.enumClosed) {
							int istart = m_paths.read(ipath);

							for (int c = 0; c < comp; c++) {
								double v = m_vertexAttributes[iattr]
										.readAsDbl(comp * istart + c);
								m_vertexAttributes[iattr].writeAsDbl(comp
										* (offset + i) + c, v);
							}

							if (--offset == 0)
								break;
						}
					}

					for (int c = 0; c < comp; c++) {
						double v = m_vertexAttributes[iattr].readAsDbl(comp * i
								+ c);
						m_vertexAttributes[iattr].writeAsDbl(comp
								* (offset + i) + c, v);
					}
				}
			}
		}

		int offset = closedPathCount;
		for (int ipath = pathCount; ipath > 0; ipath--) {
			int iend = m_paths.read(ipath);
			m_paths.write(ipath, iend + offset);

			if (m_pathFlags.read(ipath - 1) == (byte) PathFlags.enumClosed) {
				m_pathFlags.clearBits(ipath - 1, (byte) PathFlags.enumClosed);

				if (--offset == 0) {
					break;
				}
			}
		}

		m_pointCount += closedPathCount;
	}

	void closePathWithLine(int path_index) {
		// touch_();
		throwIfEmpty();

		byte pf = m_pathFlags.read(path_index);
		m_pathFlags.write(path_index, (byte) (pf | PathFlags.enumClosed));
		if (m_segmentFlags != null) {
			int vindex = getPathEnd(path_index) - 1;
			m_segmentFlags.write(vindex, (byte) SegmentFlags.enumLineSeg);
			m_segmentParamIndex.write(vindex, -1);
		}
	}

	void closePathWithLine() {
		throwIfEmpty();
		m_bPathStarted = false;
		closePathWithLine(getPathCount() - 1);
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * Closes all open curves by adding an implicit line segment from the end
	 * point to the start point.
	 */
	public void closeAllPaths() {
		_touch();
		if (m_bPolygon || isEmptyImpl())
			return;

		m_bPathStarted = false;

		for (int ipath = 0, npart = m_paths.size() - 1; ipath < npart; ipath++) {
			if (isClosedPath(ipath))
				continue;

			byte pf = m_pathFlags.read(ipath);
			m_pathFlags.write(ipath, (byte) (pf | PathFlags.enumClosed));
			// if (m_segmentFlags)
			// {
			// m_segmentFlags.write(m_pointCount - 1,
			// (byte)SegmentFlags.LineSeg));
			// m_segmentParamIndex.write(m_pointCount - 1, -1);
			// }
		}
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * Returns the size of the segment data for the given segment type.
	 * 
	 * @param flag
	 *            is one of the segment flags from the SegmentFlags enum.
	 * @return the size of the segment params as the number of doubles.
	 */
	public static int getSegmentDataSize(byte flag) {
		return _segmentParamSizes[flag];
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * Closes last path of the MultiPathImpl with the Bezier Segment.
	 * 
	 * The start point of the Bezier is the last point of the path and the last
	 * point of the bezier is the first point of the path.
	 */
	public void closePathWithBezier(Point2D controlPoint1, Point2D controlPoint2) {
		_touch();
		if (isEmptyImpl())
			throw new GeometryException(
					"Invalid call. This operation cannot be performed on an empty geometry.");

		m_bPathStarted = false;

		int pathIndex = m_paths.size() - 2;
		byte pf = m_pathFlags.read(pathIndex);
		m_pathFlags
				.write(pathIndex,
						(byte) (pf | PathFlags.enumClosed | PathFlags.enumHasNonlinearSegments));
		_initSegmentData(6);

		byte oldType = m_segmentFlags
				.read((byte) ((m_pointCount - 1) & SegmentFlags.enumSegmentMask));
		m_segmentFlags.write(m_pointCount - 1,
				(byte) (SegmentFlags.enumBezierSeg));

		int curveIndex = m_curveParamwritePoint;
		if (getSegmentDataSize(oldType) < getSegmentDataSize((byte) SegmentFlags.enumBezierSeg)) {
			m_segmentParamIndex.write(m_pointCount - 1, m_curveParamwritePoint);
			m_curveParamwritePoint += 6;
		} else {
			// there was a closing bezier curve or an arc here. We can reuse the
			// storage.
			curveIndex = m_segmentParamIndex.read(m_pointCount - 1);
		}

		double z;
		m_segmentParams.write(curveIndex, controlPoint1.x);
		m_segmentParams.write(curveIndex + 1, controlPoint1.y);
		z = 0;// TODO: calculate me.
		m_segmentParams.write(curveIndex + 2, z);

		m_segmentParams.write(curveIndex + 3, controlPoint2.x);
		m_segmentParams.write(curveIndex + 4, controlPoint2.y);
		z = 0;// TODO: calculate me.
		m_segmentParams.write(curveIndex + 5, z);
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * Returns True if the given path is closed (represents a Ring).
	 */
	public boolean isClosedPath(int ipath) {
		// Should we make a function called _UpdateClosedPathFlags and call it
		// here?
		return ((byte) (m_pathFlags.read(ipath) & PathFlags.enumClosed)) != 0;
	}

	public boolean isClosedPathInXYPlane(int path_index) {
		if (isClosedPath(path_index))
			return true;
		int istart = getPathStart(path_index);
		int iend = getPathEnd(path_index) - 1;
		if (istart > iend)
			return false;
		Point2D ptS = getXY(istart);
		Point2D ptE = getXY(iend);
		return ptS.isEqual(ptE);
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * Returns True if the given path might have non-linear segments.
	 */
	public boolean hasNonLinearSegments(int ipath) {
		// Should we make a function called _UpdateHasNonLinearSegmentsFlags and
		// call it here?
		return (m_pathFlags.read(ipath) & PathFlags.enumHasNonlinearSegments) != 0;
	}

	// Reviewed vs. Native Jan 11, 2011
	public void addSegment(Segment segment, boolean bStartNewPath) {
		mergeVertexDescription(segment.getDescription());
		if (segment.getType() == Type.Line) {
			Point point = new Point();
			if (bStartNewPath || isEmpty()) {
				segment.queryStart(point);
				startPath(point);
			}

			segment.queryEnd(point);
			lineTo(point);
		} else {
			throw GeometryException.GeometryInternalError();
		}
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * adds a rectangular closed Path to the MultiPathImpl.
	 * 
	 * @param envSrc
	 *            is the source rectangle.
	 * @param bReverse
	 *            Creates reversed path.
	 */
	public void addEnvelope(Envelope2D envSrc, boolean bReverse) {
		boolean bWasEmpty = m_pointCount == 0;

		startPath(envSrc.xmin, envSrc.ymin);
		if (bReverse) {
			lineTo(envSrc.xmax, envSrc.ymin);
			lineTo(envSrc.xmax, envSrc.ymax);
			lineTo(envSrc.xmin, envSrc.ymax);
		} else {
			lineTo(envSrc.xmin, envSrc.ymax);
			lineTo(envSrc.xmax, envSrc.ymax);
			lineTo(envSrc.xmax, envSrc.ymin);
		}

		closePathWithLine();
		m_bPathStarted = false;

		if (bWasEmpty && !bReverse) {
			_setDirtyFlag(DirtyFlags.DirtyIsEnvelope, false);// now we no(sic?)
																// the polypath
																// is envelope
		}
	}

	// Reviewed vs. Native Jan 11, 2011
	/**
	 * adds a rectangular closed Path to the MultiPathImpl.
	 * 
	 * @param envSrc
	 *            is the source rectangle.
	 * @param bReverse
	 *            Creates reversed path.
	 */
	public void addEnvelope(Envelope envSrc, boolean bReverse) {
		if (envSrc.isEmpty())
			return;

		boolean bWasEmpty = m_pointCount == 0;
		Point pt = new Point(m_description);// getDescription());
		for (int i = 0, n = 4; i < n; i++) {
			int j = bReverse ? n - i - 1 : i;

			envSrc.queryCornerByVal(j, pt);
			if (i == 0)
				startPath(pt);
			else
				lineTo(pt);
		}

		closePathWithLine();
		m_bPathStarted = false;

		if (bWasEmpty && !bReverse)
			_setDirtyFlag(DirtyFlags.DirtyIsEnvelope, false);// now we know the
																// polypath is
																// envelope
	}

	// Reviewed vs. Native Jan 11, 2011
	public void add(MultiPathImpl src, boolean bReversePaths) {
		for (int i = 0; i < src.getPathCount(); i++)
			addPath(src, i, !bReversePaths);
	}

	public void addPath(MultiPathImpl src, int srcPathIndex, boolean bForward) {
		insertPath(-1, src, srcPathIndex, bForward);
	}

	// Reviewed vs. Native Jan 11, 2011 Significant changes to last for loop
	public void addPath(Point2D[] _points, int count, boolean bForward) {
		insertPath(-1, _points, 0, count, bForward);
	}

	public void addSegmentsFromPath(MultiPathImpl src, int src_path_index,
			int src_segment_from, int src_segment_count,
			boolean b_start_new_path) {
		if (!b_start_new_path && getPathCount() == 0)
			b_start_new_path = true;

		if (src_path_index < 0)
			src_path_index = src.getPathCount() - 1;

		if (src_path_index >= src.getPathCount() || src_segment_from < 0
				|| src_segment_count < 0
				|| src_segment_count > src.getSegmentCount(src_path_index))
			throw new GeometryException("index out of bounds");

		if (src_segment_count == 0)
			return;

		boolean bIncludesClosingSegment = src.isClosedPath(src_path_index)
				&& src_segment_from + src_segment_count == src
						.getSegmentCount(src_path_index);

		if (bIncludesClosingSegment && src_segment_count == 1)
			return;// cannot add a closing segment alone.

		m_bPathStarted = false;

		mergeVertexDescription(src.getDescription());
		int src_point_count = src_segment_count;
		int srcFromPoint = src.getPathStart(src_path_index) + src_segment_from
				+ 1;
		if (b_start_new_path)// adding a new path.
		{
			src_point_count++;// add start point.
			srcFromPoint--;
		}

		if (bIncludesClosingSegment) {
			src_point_count--;
		}

		int oldPointCount = m_pointCount;
		_resizeImpl(m_pointCount + src_point_count);
		_verifyAllStreams();

		if (b_start_new_path) {
			if (src_point_count == 0)
				return;// happens when adding a single closing segment to the
						// new path

			m_paths.add(m_pointCount);

			byte flags = src.m_pathFlags.read(src_path_index);
			flags &= ~(byte) PathFlags.enumCalcMask;// remove calculated flags

			if (m_bPolygon)
				flags |= (byte) PathFlags.enumClosed;

			m_pathFlags.write(m_pathFlags.size() - 1, flags);
			m_pathFlags.add((byte) 0);
		} else {
			m_paths.write(m_pathFlags.size() - 1, m_pointCount);
		}

		// Index_type absoluteIndex = pathStart + before_point_index;

		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description.getSemantics(iattr);
			int comp = VertexDescription.getComponentCount(semantics);

			int isrcAttr = src.m_description.getAttributeIndex(semantics);
			if (isrcAttr < 0 || src.m_vertexAttributes[isrcAttr] == null) {// The
																			// source
																			// does
																			// not
																			// have
																			// the
																			// attribute.
																			// insert
																			// default
																			// value
				double v = VertexDescription.getDefaultValue(semantics);
				m_vertexAttributes[iattr].insertRange(comp * oldPointCount, v,
						src_point_count * comp, comp * oldPointCount);
				continue;
			}

			// add vertices to the given stream
			boolean b_forward = true;
			m_vertexAttributes[iattr].insertRange(comp * oldPointCount,
					src.m_vertexAttributes[isrcAttr], comp * srcFromPoint,
					src_point_count * comp, b_forward, comp, comp
							* oldPointCount);
		}

		if (hasNonLinearSegments()) {
			// TODO: implement me. For example as a while loop over all curves.
			// Replace, calling ReplaceSegment
			throw GeometryException.GeometryInternalError();
			// m_segment_flags->write_range((get_path_start(path_index) +
			// before_point_index + src_point_count), (oldPointCount -
			// get_path_start(path_index) - before_point_index),
			// m_segment_flags, (get_path_start(path_index) +
			// before_point_index), true, 1);
			// m_segment_param_index->write_range((get_path_start(path_index) +
			// before_point_index + src_point_count), (oldPointCount -
			// get_path_start(path_index) - before_point_index),
			// m_segment_param_index, (get_path_start(path_index) +
			// before_point_index), true, 1);
			// for (Index_type i = get_path_start(path_index) +
			// before_point_index, n = get_path_start(path_index) +
			// before_point_index + src_point_count; i < n; i++)
			// {
			// m_segment_flags->write(i, (int8_t)enum_value1(Segment_flags,
			// enum_line_seg));
			// m_segment_param_index->write(i, -1);
			// }
		}

		if (src.hasNonLinearSegments(src_path_index)) {
			// TODO: implement me. For example as a while loop over all curves.
			// Replace, calling ReplaceSegment
			throw GeometryException.GeometryInternalError();
		}

		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	// Reviewed vs. Native Jan 11, 2011
	public void reverseAllPaths() {
		for (int i = 0, n = getPathCount(); i < n; i++) {
			reversePath(i);
		}
	}

	// Reviewed vs. Native Jan 11, 2011
	public void reversePath(int pathIndex) {
		_verifyAllStreams();
		int pathCount = getPathCount();
		if (pathIndex >= pathCount)
			throw new IllegalArgumentException();

		int reversedPathStart = getPathStart(pathIndex);
		int reversedPathSize = getPathSize(pathIndex);
		int offset = isClosedPath(pathIndex) ? 1 : 0;

		// TODO: a bug for the non linear segments here.
		// There could be an issue here if someone explicity closes the path
		// with the same start/end point.
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			if (m_vertexAttributes[iattr] != null) {
				int semantics = m_description._getSemanticsImpl(iattr);
				int comp = VertexDescription.getComponentCount(semantics);
				m_vertexAttributes[iattr].reverseRange(comp
						* (reversedPathStart + offset), comp
						* (reversedPathSize - offset), comp);
			}
		}

		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	// Reviewed vs. Native Jan 11, 2011
	// TODO: Nonlinearsegments
	public void removePath(int pathIndex) {
		_verifyAllStreams();
		int pathCount = getPathCount();

		if (pathIndex < 0)
			pathIndex = pathCount - 1;

		if (pathIndex >= pathCount)
			throw new IllegalArgumentException();

		boolean bDirtyRingAreas2D = _hasDirtyFlag(DirtyFlags.DirtyRingAreas2D);

		int removedPathStart = getPathStart(pathIndex);
		int removedPathSize = getPathSize(pathIndex);

		// Remove the attribute values for the path
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			if (m_vertexAttributes[iattr] != null) {
				int semantics = m_description._getSemanticsImpl(iattr);
				int comp = VertexDescription.getComponentCount(semantics);
				m_vertexAttributes[iattr].eraseRange(comp * removedPathStart,
						comp * removedPathSize, comp * m_pointCount);
			}
		}

		// Change the start of each path after the removed path
		for (int i = pathIndex + 1; i <= pathCount; i++) {
			int istart = m_paths.read(i);
			m_paths.write(i - 1, istart - removedPathSize);
		}

		if (m_pathFlags == null) {
			for (int i = pathIndex + 1; i <= pathCount; i++) {
				byte flags = m_pathFlags.read(i);
				m_pathFlags.write(i - 1, flags);
			}
		}

		m_paths.resize(pathCount);
		m_pathFlags.resize(pathCount);
		m_pointCount -= removedPathSize;
		m_reservedPointCount -= removedPathSize;

		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	// TODO: Nonlinearsegments
	public void insertPath(int pathIndex, MultiPathImpl src, int srcPathIndex,
			boolean bForward) {
		if (src == this)
			throw new IllegalArgumentException();

		if (srcPathIndex >= src.getPathCount())
			throw new IllegalArgumentException();

		int oldPathCount = getPathCount();
		if (pathIndex > oldPathCount)
			throw new IllegalArgumentException();

		if (pathIndex < 0)
			pathIndex = oldPathCount;

		if (srcPathIndex < 0)
			srcPathIndex = src.getPathCount() - 1;

		m_bPathStarted = false;

		mergeVertexDescription(src.m_description);// merge attributes from the
													// source

		src._verifyAllStreams();// the source need to be correct.

		int srcPathIndexStart = src.getPathStart(srcPathIndex);
		int srcPathSize = src.getPathSize(srcPathIndex);
		int oldPointCount = m_pointCount;
		int offset = src.isClosedPath(srcPathIndex) && !bForward ? 1 : 0;

		_resizeImpl(m_pointCount + srcPathSize);
		_verifyAllStreams();
		int pathIndexStart = pathIndex < oldPathCount ? getPathStart(pathIndex)
				: oldPointCount;

		// Copy all attribute values.
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int isrcAttr = src.m_description.getAttributeIndex(semantics);

			int comp = VertexDescription.getComponentCount(semantics);

			if (isrcAttr >= 0 && src.m_vertexAttributes[isrcAttr] != null) {
				if (offset != 0)
					m_vertexAttributes[iattr].insertRange(
							pathIndexStart * comp,
							src.m_vertexAttributes[isrcAttr], comp
									* srcPathIndexStart, comp, true, comp, comp
									* oldPointCount);
				m_vertexAttributes[iattr].insertRange((pathIndexStart + offset)
						* comp, src.m_vertexAttributes[isrcAttr], comp
						* (srcPathIndexStart + offset), comp
						* (srcPathSize - offset), bForward, comp, comp
						* (oldPointCount + offset));
			} else {
				// Need to make room for the attributes, so we copy default
				// values in

				double v = VertexDescription.getDefaultValue(semantics);
				m_vertexAttributes[iattr].insertRange(pathIndexStart * comp, v,
						comp * srcPathSize, comp * oldPointCount);
			}
		}

		int newPointCount = oldPointCount + srcPathSize;
		m_paths.add(newPointCount);

		for (int ipath = oldPathCount; ipath >= pathIndex + 1; ipath--) {
			int iend = m_paths.read(ipath - 1);
			m_paths.write(ipath, iend + srcPathSize);
		}

		// ========================== todo: NonLinearSegments =================
		if (src.hasNonLinearSegments(srcPathIndex)) {

		}

		m_pathFlags.add((byte) 0);

		// _ASSERT(m_pathFlags.size() == m_paths.size());

		for (int ipath = oldPathCount - 1; ipath >= pathIndex + 1; ipath--) {
			byte flags = m_pathFlags.read(ipath);
			flags &= ~(byte) PathFlags.enumCalcMask;// remove calculated flags
			m_pathFlags.write(ipath + 1, flags);
		}

		AttributeStreamOfInt8 srcPathFlags = src.getPathFlagsStreamRef();
		byte flags = srcPathFlags.read(srcPathIndex);
		flags &= ~(byte) PathFlags.enumCalcMask;// remove calculated flags

		if (m_bPolygon)
			flags |= (byte) PathFlags.enumClosed;

		m_pathFlags.write(pathIndex, flags);
	}

	public void insertPath(int pathIndex, Point2D[] points, int pointsOffset,
			int count, boolean bForward) {
		int oldPathCount = getPathCount();
		if (pathIndex > oldPathCount)
			throw new IllegalArgumentException();

		if (pathIndex < 0)
			pathIndex = oldPathCount;

		m_bPathStarted = false;

		int oldPointCount = m_pointCount;

		// Copy all attribute values.
		if (points != null) {
			_resizeImpl(m_pointCount + count);
			_verifyAllStreams();

			int pathStart = pathIndex < oldPathCount ? getPathStart(pathIndex)
					: oldPointCount;

			for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
				int semantics = m_description._getSemanticsImpl(iattr);

				if (semantics == VertexDescription.Semantics.POSITION) {
					// copy range to make place for new vertices
					m_vertexAttributes[iattr].writeRange(
							2 * (pathStart + count),
							2 * (oldPointCount - pathIndex),
							m_vertexAttributes[iattr], 2 * pathStart, true, 2);

					AttributeStreamOfDbl position = (AttributeStreamOfDbl) (AttributeStreamBase) getAttributeStreamRef(semantics);

					int j = pathStart;
					for (int i = 0; i < count; i++, j++) {
						int index = (bForward ? pointsOffset + i : pointsOffset
								+ count - i - 1);
						position.write(2 * j, points[index].x);
						position.write(2 * j + 1, points[index].y);
					}
				} else {
					// Need to make room for the attributes, so we copy default
					// values in

					int comp = VertexDescription.getComponentCount(semantics);
					double v = VertexDescription.getDefaultValue(semantics);
					m_vertexAttributes[iattr].insertRange(pathStart * comp, v,
							comp * count, comp * oldPointCount);
				}
			}
		} else {
			_verifyAllStreams();
		}

		m_paths.add(m_pointCount);

		for (int ipath = oldPathCount; ipath >= pathIndex + 1; ipath--) {
			int iend = m_paths.read(ipath - 1);
			m_paths.write(ipath, iend + count);
		}

		m_pathFlags.add((byte) 0);

		// _ASSERT(m_pathFlags.size() == m_paths.size());

		for (int ipath = oldPathCount - 1; ipath >= pathIndex + 1; ipath--) {
			byte flags = m_pathFlags.read(ipath);
			flags &= ~(byte) PathFlags.enumCalcMask;// remove calculated flags
			m_pathFlags.write(ipath + 1, flags);
		}

		if (m_bPolygon)
			m_pathFlags.write(pathIndex, (byte) PathFlags.enumClosed);
	}

	public void insertPoints(int pathIndex, int beforePointIndex,
			MultiPathImpl src, int srcPathIndex, int srcPointIndexFrom,
			int srcPointCount, boolean bForward) {
		if (pathIndex < 0)
			pathIndex = getPathCount();

		if (srcPathIndex < 0)
			srcPathIndex = src.getPathCount() - 1;

		if (pathIndex > getPathCount() || beforePointIndex >= 0
				&& beforePointIndex > getPathSize(pathIndex)
				|| srcPathIndex >= src.getPathCount()
				|| srcPointCount > src.getPathSize(srcPathIndex))
			throw new GeometryException("index out of bounds");

		if (srcPointCount == 0)
			return;

		mergeVertexDescription(src.m_description);

		if (pathIndex == getPathCount())// adding a new path.
		{
			m_paths.add(m_pointCount);

			byte flags = src.m_pathFlags.read(srcPathIndex);
			flags &= ~(byte) PathFlags.enumCalcMask;// remove calculated flags

			if (!m_bPolygon)
				m_pathFlags.add(flags);
			else
				m_pathFlags.add((byte) (flags | PathFlags.enumClosed));
		}

		if (beforePointIndex < 0)
			beforePointIndex = getPathSize(pathIndex);

		int oldPointCount = m_pointCount;
		_resizeImpl(m_pointCount + srcPointCount);
		_verifyAllStreams();
		src._verifyAllStreams();

		int pathStart = getPathStart(pathIndex);
		int absoluteIndex = pathStart + beforePointIndex;

		if (srcPointCount < 0)
			srcPointCount = src.getPathSize(srcPathIndex);

		int srcPathStart = src.getPathStart(srcPathIndex);
		int srcAbsoluteIndex = srcPathStart + srcPointCount;

		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int comp = VertexDescription.getComponentCount(semantics);

			int isrcAttr = src.m_description.getAttributeIndex(semantics);
			if (isrcAttr < 0 || src.m_vertexAttributes[isrcAttr] == null) // The
																			// source
																			// does
																			// not
																			// have
																			// the
																			// attribute.
			{
				double v = VertexDescription.getDefaultValue(semantics);
				m_vertexAttributes[iattr].insertRange(comp * absoluteIndex, v,
						srcAbsoluteIndex * comp, comp * oldPointCount);
				continue;
			}

			// add vertices to the given stream
			m_vertexAttributes[iattr].insertRange(comp
					* (pathStart + beforePointIndex),
					src.m_vertexAttributes[isrcAttr], comp
							* (srcPathStart + srcPointIndexFrom), srcPointCount
							* comp, bForward, comp, comp * oldPointCount);
		}

		if (hasNonLinearSegments()) {// TODO: probably a bug here when a new
										// path is added.
			m_segmentFlags.writeRange((getPathStart(pathIndex)
					+ beforePointIndex + srcPointCount), (oldPointCount
					- getPathStart(pathIndex) - beforePointIndex),
					m_segmentFlags,
					(getPathStart(pathIndex) + beforePointIndex), true, 1);
			m_segmentParamIndex.writeRange((getPathStart(pathIndex)
					+ beforePointIndex + srcPointCount), (oldPointCount
					- getPathStart(pathIndex) - beforePointIndex),
					m_segmentParamIndex,
					(getPathStart(pathIndex) + beforePointIndex), true, 1);
			for (int i = getPathStart(pathIndex) + beforePointIndex, n = getPathStart(pathIndex)
					+ beforePointIndex + srcPointCount; i < n; i++) {
				m_segmentFlags.write(i, (byte) SegmentFlags.enumLineSeg);
				m_segmentParamIndex.write(i, -1);
			}
		}

		if (src.hasNonLinearSegments(srcPathIndex)) {
			// TODO: implement me. For example as a while loop over all curves.
			// Replace, calling ReplaceSegment
			throw GeometryException.GeometryInternalError();
		}

		for (int ipath = pathIndex + 1, npaths = getPathCount(); ipath <= npaths; ipath++) {
			int num = m_paths.read(ipath);
			m_paths.write(ipath, num + srcPointCount);
		}
	}

	public void insertPoints(int pathIndex, int beforePointIndex,
			Point2D[] src, int srcPointIndexFrom, int srcPointCount,
			boolean bForward) {
		if (pathIndex < 0)
			pathIndex = getPathCount();

		if (pathIndex > getPathCount()
				|| beforePointIndex > getPathSize(pathIndex)
				|| srcPointIndexFrom < 0 || srcPointCount > src.length)
			throw new GeometryException("index out of bounds");

		if (srcPointCount == 0)
			return;

		if (pathIndex == getPathCount())// adding a new path.
		{
			m_paths.add(m_pointCount);

			if (!m_bPolygon)
				m_pathFlags.add((byte) 0);
			else
				m_pathFlags.add((byte) PathFlags.enumClosed);
		}

		if (beforePointIndex < 0)
			beforePointIndex = getPathSize(pathIndex);

		_verifyAllStreams();
		int oldPointCount = m_pointCount;
		_resizeImpl(m_pointCount + srcPointCount);
		_verifyAllStreams();
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int comp = VertexDescription.getComponentCount(semantics);
			// copy range to make place for new vertices
			m_vertexAttributes[iattr]
					.writeRange(
							comp
									* (getPathStart(pathIndex)
											+ beforePointIndex + srcPointCount),
							(oldPointCount - getPathStart(pathIndex) - beforePointIndex)
									* comp,
							m_vertexAttributes[iattr],
							comp * (getPathStart(pathIndex) + beforePointIndex),
							true, comp);

			if (iattr == 0) {
				// add vertices to the given stream
				((AttributeStreamOfDbl) (AttributeStreamBase) m_vertexAttributes[iattr])
						.writeRange(comp
								* (getPathStart(pathIndex) + beforePointIndex),
								srcPointCount, src, srcPointIndexFrom, bForward);
			} else {
				double v = VertexDescription.getDefaultValue(semantics);
				m_vertexAttributes[iattr].setRange(v,
						(getPathStart(pathIndex) + beforePointIndex) * comp,
						srcPointCount * comp);
			}
		}

		if (hasNonLinearSegments()) {
			m_segmentFlags.writeRange((getPathStart(pathIndex)
					+ beforePointIndex + srcPointCount), (oldPointCount
					- getPathStart(pathIndex) - beforePointIndex),
					m_segmentFlags,
					(getPathStart(pathIndex) + beforePointIndex), true, 1);
			m_segmentParamIndex.writeRange((getPathStart(pathIndex)
					+ beforePointIndex + srcPointCount), (oldPointCount
					- getPathStart(pathIndex) - beforePointIndex),
					m_segmentParamIndex,
					(getPathStart(pathIndex) + beforePointIndex), true, 1);
			m_segmentFlags.setRange((byte) SegmentFlags.enumLineSeg,
					getPathStart(pathIndex) + beforePointIndex, srcPointCount);
			m_segmentParamIndex.setRange(-1, getPathStart(pathIndex)
					+ beforePointIndex, srcPointCount);
		}

		for (int ipath = pathIndex + 1, npaths = getPathCount(); ipath <= npaths; ipath++) {
			m_paths.write(ipath, m_paths.read(ipath) + srcPointCount);
		}
	}

	public void insertPoint(int pathIndex, int beforePointIndex, Point2D pt) {
		int pathCount = getPathCount();

		if (pathIndex < 0)
			pathIndex = getPathCount();

		if (pathIndex >= pathCount || beforePointIndex > getPathSize(pathIndex))
			throw new GeometryException("index out of bounds");

		if (pathIndex == getPathCount())// adding a new path.
		{
			m_paths.add(m_pointCount);

			if (!m_bPolygon)
				m_pathFlags.add((byte) 0);
			else
				m_pathFlags.add((byte) PathFlags.enumClosed);
		}

		if (beforePointIndex < 0)
			beforePointIndex = getPathSize(pathIndex);

		int oldPointCount = m_pointCount;
		_resizeImpl(m_pointCount + 1);
		_verifyAllStreams();

		int pathStart = getPathStart(pathIndex);

		((AttributeStreamOfDbl) (AttributeStreamBase) m_vertexAttributes[0])
				.insert(2 * (pathStart + beforePointIndex), pt,
						2 * oldPointCount);

		for (int iattr = 1, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int comp = VertexDescription.getComponentCount(semantics);

			// Need to make room for the attribute, so we copy a default value
			// in
			double v = VertexDescription.getDefaultValue(semantics);
			m_vertexAttributes[iattr].insertRange(comp
					* (pathStart + beforePointIndex), v, comp, comp
					* oldPointCount);
		}

		for (int ipath = pathIndex + 1, npaths = pathCount; ipath <= npaths; ipath++) {
			m_paths.write(ipath, m_paths.read(ipath) + 1);
		}
	}

	public void insertPoint(int pathIndex, int beforePointIndex, Point pt) {
		int pathCount = getPathCount();

		if (pathIndex < 0)
			pathIndex = getPathCount();

		if (pathIndex >= pathCount || beforePointIndex > getPathSize(pathIndex))
			throw new GeometryException("index out of bounds");

		if (pathIndex == getPathCount())// adding a new path.
		{
			m_paths.add(m_pointCount);

			if (!m_bPolygon)
				m_pathFlags.add((byte) 0);
			else
				m_pathFlags.add((byte) PathFlags.enumClosed);
		}

		if (beforePointIndex < 0)
			beforePointIndex = getPathSize(pathIndex);

		mergeVertexDescription(pt.getDescription());
		int oldPointCount = m_pointCount;
		_resizeImpl(m_pointCount + 1);
		_verifyAllStreams();

		int pathStart = getPathStart(pathIndex);

		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			int semantics = m_description._getSemanticsImpl(iattr);
			int comp = VertexDescription.getComponentCount(semantics);

			if (pt.hasAttribute(semantics)) {
				m_vertexAttributes[iattr].insertAttributes(comp
						* (pathStart + beforePointIndex), pt, semantics, comp
						* oldPointCount);
			} else {
				// Need to make room for the attribute, so we copy a default
				// value in
				double v = VertexDescription.getDefaultValue(semantics);
				m_vertexAttributes[iattr].insertRange(comp
						* (pathStart + beforePointIndex), v, comp, comp
						* oldPointCount);
			}
		}

		for (int ipath = pathIndex + 1, npaths = pathCount; ipath <= npaths; ipath++) {
			m_paths.write(ipath, m_paths.read(ipath) + 1);
		}

		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	public void removePoint(int pathIndex, int pointIndex) {
		int pathCount = getPathCount();

		if (pathIndex < 0)
			pathIndex = pathCount - 1;

		if (pathIndex >= pathCount || pointIndex >= getPathSize(pathIndex))
			throw new GeometryException("index out of bounds");

		_verifyAllStreams();

		int pathStart = getPathStart(pathIndex);

		if (pointIndex < 0)
			pointIndex = getPathSize(pathIndex) - 1;

		int absoluteIndex = pathStart + pointIndex;

		// Remove the attribute values for the path
		for (int iattr = 0, nattr = m_description.getAttributeCount(); iattr < nattr; iattr++) {
			if (m_vertexAttributes[iattr] != null) {
				int semantics = m_description._getSemanticsImpl(iattr);
				int comp = VertexDescription.getComponentCount(semantics);
				m_vertexAttributes[iattr].eraseRange(comp * absoluteIndex,
						comp, comp * m_pointCount);
			}
		}

		for (int ipath = pathCount; ipath >= pathIndex + 1; ipath--) {
			int iend = m_paths.read(ipath);
			m_paths.write(ipath, iend - 1);
		}

		m_pointCount--;
		m_reservedPointCount--;
		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	public double calculatePathLength2D(int pathIndex) /* const */
	{
		SegmentIteratorImpl segIter = querySegmentIteratorAtVertex(getPathStart(pathIndex));

		MathUtils.KahanSummator len = new MathUtils.KahanSummator(0);
		while (segIter.hasNextSegment()) {
			len.add(segIter.nextSegment().calculateLength2D());
		}

		return len.getResult();
	}

	double calculateSubLength2D(int from_path_index, int from_point_index,
			int to_path_index, int to_point_index) {
		int absolute_from_index = getPathStart(from_path_index)
				+ from_point_index;
		int absolute_to_index = getPathStart(to_path_index) + to_point_index;

		if (absolute_to_index < absolute_from_index || absolute_from_index < 0
				|| absolute_to_index > getPointCount() - 1)
			throw new IllegalArgumentException();

		SegmentIteratorImpl seg_iter = querySegmentIterator();

		double sub_length = 0.0;

		seg_iter.resetToVertex(absolute_from_index);

		do {
			while (seg_iter.hasNextSegment()) {
				Segment segment = seg_iter.nextSegment();

				if (seg_iter.getStartPointIndex() == absolute_to_index)
					break;

				double segment_length = segment.calculateLength2D();
				sub_length += segment_length;
			}

			if (seg_iter.getStartPointIndex() == absolute_to_index)
				break;

		} while (seg_iter.nextPath());

		return sub_length;
	}

	double calculateSubLength2D(int path_index, int from_point_index,
			int to_point_index) {
		int absolute_from_index = getPathStart(path_index) + from_point_index;
		int absolute_to_index = getPathStart(path_index) + to_point_index;

		if (absolute_from_index < 0 || absolute_to_index > getPointCount() - 1)
			throw new IllegalArgumentException();

		SegmentIteratorImpl seg_iter = querySegmentIterator();

		if (absolute_from_index > absolute_to_index) {
			if (!isClosedPath(path_index))
				throw new IllegalArgumentException(
						"cannot iterate across an open path");

			seg_iter.setCirculator(true);
		}

		double prev_length = 0.0;
		double sub_length = 0.0;

		seg_iter.resetToVertex(absolute_from_index);

		do {
			assert (seg_iter.hasNextSegment());
			sub_length += prev_length;
			Segment segment = seg_iter.nextSegment();
			prev_length = segment.calculateLength2D();

		} while (seg_iter.getStartPointIndex() != absolute_to_index);

		return sub_length;
	}

	@Override
	public Geometry getBoundary() {
		return Boundary.calculate(this, null);
	}

	// TODO: Add code fore interpolation type (none and angular)
	void interpolateAttributes(int from_path_index, int from_point_index,
			int to_path_index, int to_point_index) {
		for (int ipath = from_path_index; ipath < to_path_index - 1; ipath++) {
			if (isClosedPath(ipath))
				throw new IllegalArgumentException(
						"cannot interpolate across closed paths");
		}

		int nattr = m_description.getAttributeCount();

		if (nattr == 1)
			return; // only has position

		double sub_length = calculateSubLength2D(from_path_index,
				from_point_index, to_path_index, to_point_index);

		if (sub_length == 0.0)
			return;

		for (int iattr = 1; iattr < nattr; iattr++) {
			int semantics = m_description.getSemantics(iattr);

			int interpolation = VertexDescription.getInterpolation(semantics);
			if (interpolation == VertexDescription.Interpolation.ANGULAR)
				continue;

			int components = VertexDescription.getComponentCount(semantics);

			for (int ordinate = 0; ordinate < components; ordinate++)
				interpolateAttributes_(semantics, from_path_index,
						from_point_index, to_path_index, to_point_index,
						sub_length, ordinate);
		}
	}

	// TODO: Add code for interpolation type (none and angular)
	void interpolateAttributesForSemantics(int semantics, int from_path_index,
			int from_point_index, int to_path_index, int to_point_index) {
		if (semantics == VertexDescription.Semantics.POSITION)
			return;

		if (!hasAttribute(semantics))
			throw new IllegalArgumentException(
					"does not have the given attribute");

		int interpolation = VertexDescription.getInterpolation(semantics);
		if (interpolation == VertexDescription.Interpolation.ANGULAR)
			throw new IllegalArgumentException(
					"not implemented for the given semantics");

		for (int ipath = from_path_index; ipath < to_path_index - 1; ipath++) {
			if (isClosedPath(ipath))
				throw new IllegalArgumentException(
						"cannot interpolate across closed paths");
		}

		double sub_length = calculateSubLength2D(from_path_index,
				from_point_index, to_path_index, to_point_index);

		if (sub_length == 0.0)
			return;

		int components = VertexDescription.getComponentCount(semantics);

		for (int ordinate = 0; ordinate < components; ordinate++)
			interpolateAttributes_(semantics, from_path_index,
					from_point_index, to_path_index, to_point_index,
					sub_length, ordinate);
	}

	void interpolateAttributes(int path_index, int from_point_index,
			int to_point_index) {
		int nattr = m_description.getAttributeCount();

		if (nattr == 1)
			return; // only has position

		double sub_length = calculateSubLength2D(path_index, from_point_index,
				to_point_index);

		if (sub_length == 0.0)
			return;

		for (int iattr = 1; iattr < nattr; iattr++) {
			int semantics = m_description.getSemantics(iattr);

			int interpolation = VertexDescription.getInterpolation(semantics);
			if (interpolation == VertexDescription.Interpolation.ANGULAR)
				continue;

			int components = VertexDescription.getComponentCount(semantics);

			for (int ordinate = 0; ordinate < components; ordinate++)
				interpolateAttributes_(semantics, path_index, from_point_index,
						to_point_index, sub_length, ordinate);
		}
	}

	void interpolateAttributesForSemantics(int semantics, int path_index,
			int from_point_index, int to_point_index) {
		if (semantics == VertexDescription.Semantics.POSITION)
			return;

		if (!hasAttribute(semantics))
			throw new IllegalArgumentException(
					"does not have the given attribute");

		int interpolation = VertexDescription.getInterpolation(semantics);
		if (interpolation == VertexDescription.Interpolation.ANGULAR)
			throw new IllegalArgumentException(
					"not implemented for the given semantics");

		double sub_length = calculateSubLength2D(path_index, from_point_index,
				to_point_index);

		if (sub_length == 0.0)
			return;

		int components = VertexDescription.getComponentCount(semantics);

		for (int ordinate = 0; ordinate < components; ordinate++)
			interpolateAttributes_(semantics, path_index, from_point_index,
					to_point_index, sub_length, ordinate);
	}

	// TODO: Add code fore interpolation type (none and angular)
	void interpolateAttributes_(int semantics, int from_path_index,
			int from_point_index, int to_path_index, int to_point_index,
			double sub_length, int ordinate) {
		SegmentIteratorImpl seg_iter = querySegmentIterator();

		int absolute_from_index = getPathStart(from_path_index)
				+ from_point_index;
		int absolute_to_index = getPathStart(to_path_index) + to_point_index;

		double from_attribute = getAttributeAsDbl(semantics,
				absolute_from_index, ordinate);
		double to_attribute = getAttributeAsDbl(semantics, absolute_to_index,
				ordinate);
		double interpolated_attribute = from_attribute;
		double cumulative_length = 0.0;

		seg_iter.resetToVertex(absolute_from_index);

		do {
			if (seg_iter.hasNextSegment()) {
				seg_iter.nextSegment();

				if (seg_iter.getStartPointIndex() == absolute_to_index)
					return;

				setAttribute(semantics, seg_iter.getStartPointIndex(),
						ordinate, interpolated_attribute);

				seg_iter.previousSegment();

				do {
					Segment segment = seg_iter.nextSegment();

					if (seg_iter.getEndPointIndex() == absolute_to_index)
						return;

					double segment_length = segment.calculateLength2D();
					cumulative_length += segment_length;
					double t = cumulative_length / sub_length;
					interpolated_attribute = MathUtils.lerp(from_attribute,  to_attribute, t);

					if (!seg_iter.isClosingSegment())
						setAttribute(semantics, seg_iter.getEndPointIndex(),
								ordinate, interpolated_attribute);

				} while (seg_iter.hasNextSegment());
			}

		} while (seg_iter.nextPath());
	}

	void interpolateAttributes_(int semantics, int path_index,
			int from_point_index, int to_point_index, double sub_length,
			int ordinate) {
		assert (m_bPolygon);
		SegmentIteratorImpl seg_iter = querySegmentIterator();

		int absolute_from_index = getPathStart(path_index) + from_point_index;
		int absolute_to_index = getPathStart(path_index) + to_point_index;

		if (absolute_to_index == absolute_from_index)
			return;

		double from_attribute = getAttributeAsDbl(semantics,
				absolute_from_index, ordinate);
		double to_attribute = getAttributeAsDbl(semantics, absolute_to_index,
				ordinate);
		double cumulative_length = 0.0;

		seg_iter.resetToVertex(absolute_from_index);
		seg_iter.setCirculator(true);

		double prev_interpolated_attribute = from_attribute;

		do {
			Segment segment = seg_iter.nextSegment();
			setAttribute(semantics, seg_iter.getStartPointIndex(), ordinate,
					prev_interpolated_attribute);

			double segment_length = segment.calculateLength2D();
			cumulative_length += segment_length;
			double t = cumulative_length / sub_length;
			prev_interpolated_attribute = MathUtils.lerp(from_attribute, to_attribute, t);

		} while (seg_iter.getEndPointIndex() != absolute_to_index);
	}

	@Override
	public void setEmpty() {
		m_curveParamwritePoint = 0;
		m_bPathStarted = false;
		m_paths = null;
		m_pathFlags = null;
		m_segmentParamIndex = null;
		m_segmentFlags = null;
		m_segmentParams = null;
		_setEmptyImpl();
	}

	@Override
	public void applyTransformation(Transformation2D transform) {
		applyTransformation(transform, -1);
	}

	public void applyTransformation(Transformation2D transform, int pathIndex) {
		if (isEmpty())
			return;

		if (transform.isIdentity())
			return;

		_verifyAllStreams();
		AttributeStreamOfDbl points = (AttributeStreamOfDbl) m_vertexAttributes[0];
		Point2D ptStart = new Point2D();
		Point2D ptControl = new Point2D();

		boolean bHasNonLinear;
		int fistIdx;
		int lastIdx;
		if (pathIndex < 0) {
			bHasNonLinear = hasNonLinearSegments();
			fistIdx = 0;
			lastIdx = m_pointCount;
		} else {
			bHasNonLinear = hasNonLinearSegments(pathIndex);
			fistIdx = getPathStart(pathIndex);
			lastIdx = getPathEnd(pathIndex);
		}

		for (int ipoint = fistIdx; ipoint < lastIdx; ipoint++) {
			ptStart.x = points.read(ipoint * 2);
			ptStart.y = points.read(ipoint * 2 + 1);

			if (bHasNonLinear) {
				int segIndex = m_segmentParamIndex.read(ipoint);
				if (segIndex >= 0) {
					int segmentType = (int) m_segmentFlags.read(ipoint);
					int type = segmentType & SegmentFlags.enumSegmentMask;
					switch (type) {
					case SegmentFlags.enumBezierSeg: {
						ptControl.x = m_segmentParams.read(segIndex);
						ptControl.y = m_segmentParams.read(segIndex + 1);
						transform.transform(ptControl, ptControl);
						m_segmentParams.write(segIndex, ptControl.x);
						m_segmentParams.write(segIndex + 1, ptControl.y);

						ptControl.x = m_segmentParams.read(segIndex + 3);
						ptControl.y = m_segmentParams.read(segIndex + 4);
						transform.transform(ptControl, ptControl);
						m_segmentParams.write(segIndex + 3, ptControl.x);
						m_segmentParams.write(segIndex + 4, ptControl.y);
					}
						break;
					case SegmentFlags.enumArcSeg:
						throw GeometryException.GeometryInternalError();

					}
				}
			}

			transform.transform(ptStart, ptStart);
			points.write(ipoint * 2, ptStart.x);
			points.write(ipoint * 2 + 1, ptStart.y);
		}

		notifyModified(DirtyFlags.DirtyCoordinates);
		// REFACTOR: reset the exact envelope only and transform the loose
		// envelope
	}

	@Override
	public void applyTransformation(Transformation3D transform) {
		if (isEmpty())
			return;

		addAttribute(VertexDescription.Semantics.Z);
		_verifyAllStreams();
		AttributeStreamOfDbl points = (AttributeStreamOfDbl) m_vertexAttributes[0];
		AttributeStreamOfDbl zs = (AttributeStreamOfDbl) m_vertexAttributes[1];
		Point3D ptStart = new Point3D();
		Point3D ptControl = new Point3D();
		boolean bHasNonLinear = hasNonLinearSegments();
		for (int ipoint = 0; ipoint < m_pointCount; ipoint++) {
			ptStart.x = points.read(ipoint * 2);
			ptStart.y = points.read(ipoint * 2 + 1);
			ptStart.z = zs.read(ipoint);

			if (bHasNonLinear) {
				int segIndex = m_segmentParamIndex.read(ipoint);
				if (segIndex >= 0) {
					int segmentType = (int) m_segmentFlags.read(ipoint);
					int type = segmentType & (int) SegmentFlags.enumSegmentMask;
					switch (type) {
					case SegmentFlags.enumBezierSeg: {
						ptControl.x = m_segmentParams.read(segIndex);
						ptControl.y = m_segmentParams.read(segIndex + 1);
						ptControl.z = m_segmentParams.read(segIndex + 2);
						ptControl = transform.transform(ptControl);
						m_segmentParams.write(segIndex, ptControl.x);
						m_segmentParams.write(segIndex + 1, ptControl.y);
						m_segmentParams.write(segIndex + 1, ptControl.z);

						ptControl.x = m_segmentParams.read(segIndex + 3);
						ptControl.y = m_segmentParams.read(segIndex + 4);
						ptControl.z = m_segmentParams.read(segIndex + 5);
						ptControl = transform.transform(ptControl);
						m_segmentParams.write(segIndex + 3, ptControl.x);
						m_segmentParams.write(segIndex + 4, ptControl.y);
						m_segmentParams.write(segIndex + 5, ptControl.z);
					}
						break;
					case SegmentFlags.enumArcSeg:
						throw GeometryException.GeometryInternalError();

					}
				}
			}

			ptStart = transform.transform(ptStart);
			points.write(ipoint * 2, ptStart.x);
			points.write(ipoint * 2 + 1, ptStart.y);
			zs.write(ipoint, ptStart.z);
		}

		// REFACTOR: reset the exact envelope only and transform the loose
		// envelope

		notifyModified(DirtyFlags.DirtyCoordinates);
	}

	@Override
	protected void _verifyStreamsImpl() {
		if (m_paths == null) {
			m_paths = (AttributeStreamOfInt32) AttributeStreamBase
					.createIndexStream(1, 0);
			m_pathFlags = (AttributeStreamOfInt8) AttributeStreamBase
					.createByteStream(1, (byte) 0);
		}

		if (m_segmentFlags != null) {
			m_segmentFlags.resize(m_reservedPointCount,
					(byte) SegmentFlags.enumLineSeg);
			m_segmentParamIndex.resize(m_reservedPointCount, -1);
		}
	}

	@Override
	void _copyToImpl(MultiVertexGeometryImpl dst) {
		MultiPathImpl dstPoly = (MultiPathImpl) dst;
		dstPoly.m_bPathStarted = false;
		dstPoly.m_curveParamwritePoint = m_curveParamwritePoint;
		dstPoly.m_fill_rule = m_fill_rule;
		
		if (m_paths != null)
			dstPoly.m_paths = new AttributeStreamOfInt32(m_paths);
		else
			dstPoly.m_paths = null;

		if (m_pathFlags != null)
			dstPoly.m_pathFlags = new AttributeStreamOfInt8(m_pathFlags);
		else
			dstPoly.m_pathFlags = null;

		if (m_segmentParamIndex != null)
			dstPoly.m_segmentParamIndex = new AttributeStreamOfInt32(
					m_segmentParamIndex);
		else
			dstPoly.m_segmentParamIndex = null;

		if (m_segmentFlags != null)
			dstPoly.m_segmentFlags = new AttributeStreamOfInt8(m_segmentFlags);
		else
			dstPoly.m_segmentFlags = null;

		if (m_segmentParams != null)
			dstPoly.m_segmentParams = new AttributeStreamOfDbl(m_segmentParams);
		else
			dstPoly.m_segmentParams = null;

		dstPoly.m_cachedLength2D = m_cachedLength2D;
		dstPoly.m_cachedArea2D = m_cachedArea2D;

		if (!_hasDirtyFlag(DirtyFlags.DirtyRingAreas2D)) {
			dstPoly.m_cachedRingAreas2D = (AttributeStreamOfDbl) m_cachedRingAreas2D;
		} else
			dstPoly.m_cachedRingAreas2D = null;

	}

	@Override
	public double calculateLength2D() {
		if (!_hasDirtyFlag(DirtyFlags.DirtyLength2D)) {
			return m_cachedLength2D;
		}

		SegmentIteratorImpl segIter = querySegmentIterator();
		MathUtils.KahanSummator len = new MathUtils.KahanSummator(0);
		while (segIter.nextPath()) {
			while (segIter.hasNextSegment()) {
				len.add(segIter.nextSegment().calculateLength2D());
			}
		}

		m_cachedLength2D = len.getResult();
		_setDirtyFlag(DirtyFlags.DirtyLength2D, false);

		return len.getResult();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;

		if (!(other instanceof MultiPathImpl))
			return false;

		if (!super.equals(other))
			return false;

		MultiPathImpl otherMultiPath = (MultiPathImpl) other;

		int pathCount = getPathCount();
		int pathCountOther = otherMultiPath.getPathCount();

		if (pathCount != pathCountOther)
			return false;

		if (pathCount > 0 && m_paths != null
				&& !m_paths.equals(otherMultiPath.m_paths, 0, pathCount + 1))
			return false;

		if (m_fill_rule != otherMultiPath.m_fill_rule)
			return false;

		{
			// Note: OGC flags do not participate in the equals operation by
			// design.
			// Because for the polygon pathFlags will have all enum_closed set,
			// we do not need to compare this stream. Only for polyline.
			// Polyline does not have OGC flags set.
			if (!m_bPolygon) {
				if (m_pathFlags != null
						&& !m_pathFlags.equals(otherMultiPath.m_pathFlags, 0,
								pathCount))
					return false;
			}
		}
	      
		return super.equals(other);
	}

	/**
	 * Returns a SegmentIterator that set to a specific vertex of the
	 * MultiPathImpl. The call to NextSegment will return the segment that
	 * starts at the vertex. Call to PreviousSegment will return the segment
	 * that starts at the previous vertex.
	 */
	public SegmentIteratorImpl querySegmentIteratorAtVertex(int startVertexIndex) {
		if (startVertexIndex < 0 || startVertexIndex >= getPointCount())
			throw new IndexOutOfBoundsException();

		SegmentIteratorImpl iter = new SegmentIteratorImpl(this,
				startVertexIndex);
		return iter;
	}

	// void QuerySegmentIterator(int fromVertex, SegmentIterator iterator);
	public SegmentIteratorImpl querySegmentIterator() {
		return new SegmentIteratorImpl(this);
	}

	@Override
	public void _updateXYImpl(boolean bExact) {
		super._updateXYImpl(bExact);
		boolean bHasCurves = hasNonLinearSegments();
		if (bHasCurves) {
			SegmentIteratorImpl segIter = querySegmentIterator();
			while (segIter.nextPath()) {
				while (segIter.hasNextSegment()) {
					Segment curve = segIter.nextCurve();
					if (curve != null) {
						Envelope2D env2D = new Envelope2D();
						curve.queryEnvelope2D(env2D);
						m_envelope.merge(env2D);
					} else
						break;
				}
			}
		}
	}

	@Override
	void calculateEnvelope2D(Envelope2D env, boolean bExact) {
		super.calculateEnvelope2D(env, bExact);
		boolean bHasCurves = hasNonLinearSegments();
		if (bHasCurves) {
			SegmentIteratorImpl segIter = querySegmentIterator();
			while (segIter.nextPath()) {
				while (segIter.hasNextSegment()) {
					Segment curve = segIter.nextCurve();
					if (curve != null) {
						Envelope2D env2D = new Envelope2D();
						curve.queryEnvelope2D(env2D);
						env.merge(env2D);
					} else
						break;
				}
			}
		}
	}

	@Override
	public void _notifyModifiedAllImpl() {
		if (m_paths == null || m_paths.size() == 0)// if (m_paths == null ||
													// !m_paths.size())
			m_pointCount = 0;
		else
			m_pointCount = m_paths.read(m_paths.size() - 1);
	}

	@Override
	public double calculateArea2D() {
		if (!m_bPolygon)
			return 0.0;

		_updateRingAreas2D();

		return m_cachedArea2D;
	}

	/**
	 * Returns True if the ring is an exterior ring. Valid only for simple
	 * polygons.
	 */
	public boolean isExteriorRing(int ringIndex) {
		if (!m_bPolygon)
			return false;

		if (!_hasDirtyFlag(DirtyFlags.DirtyOGCFlags))
			return (m_pathFlags.read(ringIndex) & (byte) PathFlags.enumOGCStartPolygon) != 0;

		_updateRingAreas2D();
		return m_cachedRingAreas2D.read(ringIndex) > 0;
		// Should we make a function called _UpdateHasNonLinearSegmentsFlags and
		// call it here?
	}

	public double calculateRingArea2D(int pathIndex) {
		if (!m_bPolygon)
			return 0.0;

		_updateRingAreas2D();

		return m_cachedRingAreas2D.read(pathIndex);
	}

	public void _updateRingAreas2D() {
		if (_hasDirtyFlag(DirtyFlags.DirtyRingAreas2D)) {
			int pathCount = getPathCount();

			if (m_cachedRingAreas2D == null)
				m_cachedRingAreas2D = new AttributeStreamOfDbl(pathCount);
			else if (m_cachedRingAreas2D.size() != pathCount)
				m_cachedRingAreas2D.resize(pathCount);

			MathUtils.KahanSummator totalArea = new MathUtils.KahanSummator(0);
			MathUtils.KahanSummator pathArea = new MathUtils.KahanSummator(0);
			Point2D pt = new Point2D();
			int ipath = 0;
			SegmentIteratorImpl segIter = querySegmentIterator();
			while (segIter.nextPath()) {
				pathArea.reset();
				getXY(getPathStart(segIter.getPathIndex()), pt);// get the area
																// calculation
																// origin to be
																// the origin of
																// the ring.
				while (segIter.hasNextSegment()) {
					pathArea.add(segIter.nextSegment()._calculateArea2DHelper(
							pt.x, pt.y));
				}

				totalArea.add(pathArea.getResult());

				int i = ipath++;
				m_cachedRingAreas2D.write(i, pathArea.getResult());
			}

			m_cachedArea2D = totalArea.getResult();
			_setDirtyFlag(DirtyFlags.DirtyRingAreas2D, false);
		}
	}

	int getOGCPolygonCount() {
		if (!m_bPolygon)
			return 0;

		_updateOGCFlags();

		int polygonCount = 0;
		int partCount = getPathCount();
		for (int ipart = 0; ipart < partCount; ipart++) {
			if (((int) m_pathFlags.read(ipart) & (int) PathFlags.enumOGCStartPolygon) != 0)
				polygonCount++;
		}

		return polygonCount;
	}

	protected void _updateOGCFlags() {
		if (_hasDirtyFlag(DirtyFlags.DirtyOGCFlags)) {
			_updateRingAreas2D();

			int pathCount = getPathCount();
			if (pathCount > 0 && (m_pathFlags == null || m_pathFlags.size() < pathCount))
				m_pathFlags = (AttributeStreamOfInt8) AttributeStreamBase
						.createByteStream(pathCount + 1);

			int firstSign = 1;
			for (int ipath = 0; ipath < pathCount; ipath++) {
				double area = m_cachedRingAreas2D.read(ipath);
				if (ipath == 0)
					firstSign = area > 0 ? 1 : -1;
				if (area * firstSign > 0.0)
					m_pathFlags.setBits(ipath,
							(byte) PathFlags.enumOGCStartPolygon);
				else
					m_pathFlags.clearBits(ipath,
							(byte) PathFlags.enumOGCStartPolygon);
			}
			_setDirtyFlag(DirtyFlags.DirtyOGCFlags, false);
		}
	}

	public int getPathIndexFromPointIndex(int pointIndex) {
		int positionHint = m_currentPathIndex;// in case of multithreading
												// thiswould simply produce an
												// invalid value
		int pathCount = getPathCount();

		// Try using the hint position first to get the path index.
		if (positionHint >= 0 && positionHint < pathCount) {
			if (pointIndex < getPathEnd(positionHint)) {
				if (pointIndex >= getPathStart(positionHint))
					return positionHint;
				positionHint--;
			} else {
				positionHint++;
			}

			if (positionHint >= 0 && positionHint < pathCount) {
				if (pointIndex >= getPathStart(positionHint)
						&& pointIndex < getPathEnd(positionHint)) {
					m_currentPathIndex = positionHint;
					return positionHint;
				}
			}
		}

		if (pathCount < 5) {// TODO: time the performance to choose when to use
							// linear search.
			for (int i = 0; i < pathCount; i++) {
				if (pointIndex < getPathEnd(i)) {
					m_currentPathIndex = i;
					return i;
				}
			}
			throw new GeometryException("corrupted geometry");
		}

		// Do binary search:
		int minPathIndex = 0;
		int maxPathIndex = pathCount - 1;
		while (maxPathIndex > minPathIndex) {
			int mid = minPathIndex + ((maxPathIndex - minPathIndex) >> 1);
			int pathStart = getPathStart(mid);
			if (pointIndex < pathStart)
				maxPathIndex = mid - 1;
			else {
				int pathEnd = getPathEnd(mid);
				if (pointIndex >= pathEnd)
					minPathIndex = mid + 1;
				else {
					m_currentPathIndex = mid;
					return mid;
				}
			}
		}

		m_currentPathIndex = minPathIndex;
		return minPathIndex;
	}

	int getHighestPointIndex(int path_index) {
		assert (path_index >= 0 && path_index < getPathCount());

		AttributeStreamOfDbl position = (AttributeStreamOfDbl) (getAttributeStreamRef(VertexDescription.Semantics.POSITION));
		AttributeStreamOfInt32 paths = (AttributeStreamOfInt32) (getPathStreamRef());

		int path_end = getPathEnd(path_index);
		int path_start = getPathStart(path_index);
		int max_index = -1;
		Point2D max_point = new Point2D(), pt = new Point2D();
		max_point.y = NumberUtils.negativeInf();
		max_point.x = NumberUtils.negativeInf();

		for (int i = path_start + 0; i < path_end; i++) {
			position.read(2 * i, pt);
			if (max_point.compare(pt) == -1) {
				max_index = i;
				max_point.setCoords(pt);
			}
		}

		return max_index;
	}

	/**
	 * Returns total segment count in the MultiPathImpl.
	 */
	public int getSegmentCount() {
		int segCount = getPointCount();
		if (!m_bPolygon) {
			segCount -= getPathCount();
			for (int i = 0, n = getPathCount(); i < n; i++)
				if (isClosedPath(i))
					segCount++;
		}

		return segCount;
	}

	public int getSegmentCount(int path_index) {
		int segCount = getPathSize(path_index);
		if (!isClosedPath(path_index))
			segCount--;
		return segCount;
	}

	// HEADER defintions
	@Override
	public Geometry createInstance() {
		return new MultiPathImpl(m_bPolygon, getDescription());
	}

	@Override
	public int getDimension() {
		return m_bPolygon ? 2 : 1;
	}

	@Override
	public Geometry.Type getType() {
		return m_bPolygon ? Type.Polygon : Type.Polyline;
	}

	/**
	 * Returns True if the class is envelope. THis is not an exact method. Only
	 * addEnvelope makes this true.
	 */
	public boolean isEnvelope() {
		return !_hasDirtyFlag(DirtyFlags.DirtyIsEnvelope);
	}

	/**
	 * Returns a reference to the AttributeStream of MultiPathImpl parts
	 * (Paths).
	 * 
	 * For the non empty MultiPathImpl, that stream contains start points of the
	 * MultiPathImpl curves. In addition, the last element is the total point
	 * count. The number of vertices in a given part is parts[i + 1] - parts[i].
	 */
	public AttributeStreamOfInt32 getPathStreamRef() {
		throwIfEmpty();
		return m_paths;
	}

	/**
	 * sets a reference to an AttributeStream of MultiPathImpl paths (Paths).
	 */
	public void setPathStreamRef(AttributeStreamOfInt32 paths) {
		m_paths = paths;
		notifyModified(DirtyFlags.DirtyAll);
	}

	/**
	 * Returns a reference to the AttributeStream of Segment flags (SegmentFlags
	 * flags). Can be NULL when no non-linear segments are present.
	 * 
	 * Segment flags indicate what kind of segment originates (starts) on the
	 * given point. The last vertices of open Path parts has enumNone flag.
	 */
	public AttributeStreamOfInt8 getSegmentFlagsStreamRef() {
		throwIfEmpty();
		return m_segmentFlags;
	}

	/**
	 * Returns a reference to the AttributeStream of Path flags (PathFlags
	 * flags).
	 * 
	 * Each start point of a path has a flag set to indicate if the Path is open
	 * or closed.
	 */
	public AttributeStreamOfInt8 getPathFlagsStreamRef() {
		throwIfEmpty();
		return m_pathFlags;
	}

	/**
	 * sets a reference to an AttributeStream of Path flags (PathFlags flags).
	 */
	public void setPathFlagsStreamRef(AttributeStreamOfInt8 pathFlags) {
		m_pathFlags = pathFlags;
		notifyModified(DirtyFlags.DirtyAll);
	}

	public AttributeStreamOfInt32 getSegmentIndexStreamRef() {
		throwIfEmpty();
		return m_segmentParamIndex;
	}

	public AttributeStreamOfDbl getSegmentDataStreamRef() {
		throwIfEmpty();
		return m_segmentParams;
	}

	public int getPathCount() {
		return (m_paths != null) ? m_paths.size() - 1 : 0;
	}

	public int getPathEnd(int partIndex) {
		return m_paths.read(partIndex + 1);
	}

	public int getPathSize(int partIndex) {
		return m_paths.read(partIndex + 1) - m_paths.read(partIndex);
	}

	public int getPathStart(int partIndex) {
		return m_paths.read(partIndex);
	}

	@Override
	public Object _getImpl() {
		return this;
	}

	public void setDirtyOGCFlags(boolean bYesNo) {
		_setDirtyFlag(DirtyFlags.DirtyOGCFlags, bYesNo);
	}

	public boolean hasDirtyOGCStartFlags() {
		return _hasDirtyFlag(DirtyFlags.DirtyOGCFlags);
	}

	public void setDirtyRingAreas2D(boolean bYesNo) {
		_setDirtyFlag(DirtyFlags.DirtyRingAreas2D, bYesNo);
	}

	public boolean hasDirtyRingAreas2D() {
		return _hasDirtyFlag(DirtyFlags.DirtyRingAreas2D);
	}

	public void setRingAreasStreamRef(AttributeStreamOfDbl ringAreas) {
		m_cachedRingAreas2D = ringAreas;
		_setDirtyFlag(DirtyFlags.DirtyRingAreas2D, false);
	}

	// HEADER defintions

	// // TODO check this against current implementation in native
	// public void notifyModified(int flags)
	// {
	// if(flags == DirtyFlags.DirtyAll)
	// {
	// m_reservedPointCount = -1;
	// _notifyModifiedAllImpl();
	// }
	// m_flagsMask |= flags;
	// _clearAccelerators();
	//
	//
	// // ROHIT's implementation
	// // if (m_paths == null || 0 == m_paths.size())
	// // m_pointCount = 0;
	// // else
	// // m_pointCount = m_paths.read(m_paths.size() - 1);
	// //
	// // super.notifyModified(flags);
	// }

	@Override
	public boolean _buildRasterizedGeometryAccelerator(double toleranceXY,
			GeometryAccelerationDegree accelDegree) {
		if (m_accelerators == null)// (!m_accelerators)
		{
			m_accelerators = new GeometryAccelerators();
		}

		int rasterSize = RasterizedGeometry2D
				.rasterSizeFromAccelerationDegree(accelDegree);
		RasterizedGeometry2D rgeom = m_accelerators.getRasterizedGeometry();
		if (rgeom != null) {
			if (rgeom.getToleranceXY() < toleranceXY
					|| rasterSize > rgeom.getRasterSize()) {
				m_accelerators._setRasterizedGeometry(null);
			} else
				return true;
		}

		rgeom = RasterizedGeometry2D.create(this, toleranceXY, rasterSize);
		m_accelerators._setRasterizedGeometry(rgeom);
		//rgeom.dbgSaveToBitmap("c:/temp/ddd.bmp");
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();

		if (!isEmptyImpl()) {
			int pathCount = getPathCount();

			if (m_paths != null)
				m_paths.calculateHashImpl(hashCode, 0, pathCount + 1);

			if (m_pathFlags != null)
				m_pathFlags.calculateHashImpl(hashCode, 0, pathCount);
		}

		return hashCode;
	}

	public byte getSegmentFlags(int ivertex) {
		if (m_segmentFlags != null)
			return m_segmentFlags.read(ivertex);
		else
			return (byte) SegmentFlags.enumLineSeg;
	}

	public void getSegment(int startVertexIndex, SegmentBuffer segBuffer,
			boolean bStripAttributes) {
		int ipath = getPathIndexFromPointIndex(startVertexIndex);
		if (startVertexIndex == getPathEnd(ipath) - 1 && !isClosedPath(ipath))
			throw new GeometryException("index out of bounds");

		_verifyAllStreams();
		AttributeStreamOfInt8 segFlagStream = getSegmentFlagsStreamRef();
		int segFlag = SegmentFlags.enumLineSeg;
		if (segFlagStream != null)
			segFlag = segFlagStream.read(startVertexIndex)
					& SegmentFlags.enumSegmentMask;

		switch (segFlag) {
		case SegmentFlags.enumLineSeg:
			segBuffer.createLine();
			break;
		case SegmentFlags.enumBezierSeg:
			throw GeometryException.GeometryInternalError();
		case SegmentFlags.enumArcSeg:
			throw GeometryException.GeometryInternalError();
		default:
			throw GeometryException.GeometryInternalError();
		}

		Segment currentSegment = segBuffer.get();
		if (!bStripAttributes)
			currentSegment.assignVertexDescription(m_description);
		else
			currentSegment
					.assignVertexDescription(VertexDescriptionDesignerImpl
							.getDefaultDescriptor2D());

		int endVertexIndex;
		if (startVertexIndex == getPathEnd(ipath) - 1 && isClosedPath(ipath)) {
			endVertexIndex = getPathStart(ipath);
		} else
			endVertexIndex = startVertexIndex + 1;

		Point2D pt = new Point2D();
		getXY(startVertexIndex, pt);
		currentSegment.setStartXY(pt);
		getXY(endVertexIndex, pt);
		currentSegment.setEndXY(pt);

		if (!bStripAttributes) {
			for (int i = 1, nattr = m_description.getAttributeCount(); i < nattr; i++) {
				int semantics = m_description._getSemanticsImpl(i);
				int ncomp = VertexDescription.getComponentCount(semantics);
				for (int ord = 0; ord < ncomp; ord++) {
					double vs = getAttributeAsDbl(semantics, startVertexIndex,
							ord);
					currentSegment.setStartAttribute(semantics, ord, vs);
					double ve = getAttributeAsDbl(semantics, endVertexIndex,
							ord);
					currentSegment.setEndAttribute(semantics, ord, ve);
				}
			}
		}
	}

	void queryPathEnvelope2D(int path_index, Envelope2D envelope) {
		if (path_index >= getPathCount())
			throw new IllegalArgumentException();

		if (isEmpty()) {
			envelope.setEmpty();
			return;
		}

		if (hasNonLinearSegments(path_index)) {
			throw new GeometryException("not implemented");
		} else {
			AttributeStreamOfDbl stream = (AttributeStreamOfDbl) getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			Point2D pt = new Point2D();
			Envelope2D env = new Envelope2D();
			env.setEmpty();
			for (int i = getPathStart(path_index), iend = getPathEnd(path_index); i < iend; i++) {
				stream.read(2 * i, pt);
				env.merge(pt);
			}
			envelope.setCoords(env);
		}
	}

	public void queryLoosePathEnvelope2D(int path_index, Envelope2D envelope) {
		if (path_index >= getPathCount())
			throw new IllegalArgumentException();

		if (isEmpty()) {
			envelope.setEmpty();
			return;
		}

		if (hasNonLinearSegments(path_index)) {
			throw new GeometryException("not implemented");
		} else {
			AttributeStreamOfDbl stream = (AttributeStreamOfDbl) getAttributeStreamRef(VertexDescription.Semantics.POSITION);
			Point2D pt = new Point2D();
			Envelope2D env = new Envelope2D();
			env.setEmpty();
			for (int i = getPathStart(path_index), iend = getPathEnd(path_index); i < iend; i++) {
				stream.read(2 * i, pt);
				env.merge(pt);
			}
			envelope.setCoords(env);
		}
	}
	
	@Override
	public boolean _buildQuadTreeAccelerator(GeometryAccelerationDegree d) {
		if (m_accelerators == null)// (!m_accelerators)
		{
			m_accelerators = new GeometryAccelerators();
		}

		if (d == GeometryAccelerationDegree.enumMild || getPointCount() < 16)
			return false;

		QuadTreeImpl quad_tree_impl = InternalUtils.buildQuadTree(this);
		m_accelerators._setQuadTree(quad_tree_impl);

		return true;
	}

	boolean _buildQuadTreeForPathsAccelerator(GeometryAccelerationDegree degree) {
		if (m_accelerators == null) {
			m_accelerators = new GeometryAccelerators();
		}

		// TODO: when less than two envelopes - no need to this.

		if (m_accelerators.getQuadTreeForPaths() != null)
			return true;

		m_accelerators._setQuadTreeForPaths(null);
		QuadTreeImpl quad_tree_impl = InternalUtils.buildQuadTreeForPaths(this);
		m_accelerators._setQuadTreeForPaths(quad_tree_impl);

		return true;
	}

	void setFillRule(int rule) {
		assert (m_bPolygon);
		m_fill_rule = rule;
	}

	int getFillRule() {
		return m_fill_rule;
	}

	void clearDirtyOGCFlags() { 
		_setDirtyFlag(DirtyFlags.DirtyOGCFlags, false);
	}
}

