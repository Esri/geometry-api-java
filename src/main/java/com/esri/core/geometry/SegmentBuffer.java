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

import com.esri.core.geometry.Geometry.Type;
import java.io.Serializable;

/**
 * A helper class to provide reusable segment, line, etc instances.
 */
class SegmentBuffer implements Serializable {

	private static final long serialVersionUID = 1L;

	Line m_line;

	// PointerOf(Bezier) m_bez;
	Segment m_seg;

	public SegmentBuffer() {
		m_line = null;
		m_seg = null;
	}

	public Segment get() {
		return m_seg;
	}

	public void set(Segment seg) {
		m_seg = seg;
		if (seg != null) {
			if (seg.getType() == Type.Line) {
				Line ln = (Line) seg;
				m_line = ln;
			}
			throw GeometryException.GeometryInternalError();
		}
	}
	
	public void create(Geometry.Type type)
	{
		if (type == Geometry.Type.Line)
			createLine();
		else
			throw new GeometryException("not implemented");
	}

	public void createLine() {
		if (null == m_line) {
			m_line = new Line();

		}
		m_seg = m_line;
	}
}
