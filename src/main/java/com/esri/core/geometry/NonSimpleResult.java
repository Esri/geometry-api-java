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

class NonSimpleResult {
	public enum Reason {
		NotDetermined, // <!When is_simple_xxx returns true, the geometry is
						// simple. When is_simple_xxx returns false, the
						// geometry was already known to be non-simple. To make
						// it determine the reason of non-simple result, use
						// bForceTest == True.
		Structure, // <!non-simple, because the structure is bad (0 size path,
					// for example)
		DegenerateSegments, // <!non-simple, because there are degenerate
							// segments
		Clustering, // <!non-simple, because not clustered properly /multipoint,
					// polyline, polygon/
		Cracking, // <!non-simple, because not cracked properly (intersecting
					// segments, overlaping segments) /polyline, polygon/
		CrossOver, // <!non-simple, because there are crossovers (self
					// intersections that are not cracking case) /polygon/
		RingOrientation, // <!non-simple, because holes or exteriors have wrong
							// direction /polygon/
		RingOrder, // <!weak simple, but not strong simple, because exteriors
					// and holes are not in the correct order /polygon, weak
					// simple/
		OGCPolylineSelfTangency, // <!there is a self tangency or cross-over
									// situation /polyline, strong simple, but
									// not OGC simple/
		OGCPolygonSelfTangency, // <!there is a self tangency situation
								// /polygon, strong simple, but not OGC simple/
		OGCDisconnectedInterior
		// <!touching interioir rings make a
		// disconnected point set from polygon interioir
		// /polygon, strong simple, but not OGC simple/
	}

	public Reason m_reason;
	public int m_vertexIndex1;
	public int m_vertexIndex2;

	public NonSimpleResult() {
		m_reason = Reason.NotDetermined;
		m_vertexIndex1 = -1;
		m_vertexIndex2 = -1;
	}

	void Assign(NonSimpleResult src) {
		m_reason = src.m_reason;
		m_vertexIndex1 = src.m_vertexIndex1;
		m_vertexIndex2 = src.m_vertexIndex2;
	}

	NonSimpleResult(Reason reason, int index1, int index2) {
		m_reason = reason;
		m_vertexIndex1 = index1;
		m_vertexIndex2 = index2;
	}

}
