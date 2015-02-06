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

interface DirtyFlags {
	public static final int dirtyIsKnownSimple = 1; // !<0 when is_weak_simple
													// or is_strong_simple flag
													// is valid
	public static final int isWeakSimple = 2; // !<when dirty_is_known_simple is
												// 0, this flag indicates
												// whether the geometry is weak
												// simple or not
	public static final int isStrongSimple = 4; // !<when dirty_is_known_simple
												// is 0, this flag indicates
												// whether the geometry is
												// strong simple or not
	public static final int dirtyOGCFlags = 8; // !<OGCFlags are set by simplify
												// or WKB/WKT import.

	public static final int dirtyVerifiedStreams = 32; // < at least one stream
														// is unverified
	public static final int dirtyExactIntervals = 64; // < exact envelope is
														// dirty
	public static final int dirtyLooseIntervals = 128; // < loose envelope is
														// dirty
	public static final int dirtyIntervals = dirtyExactIntervals
			| dirtyLooseIntervals; // < loose and dirty envelopes are loose
	public static final int dirtyIsEnvelope = 256; // < the geometry is not
													// known to be an envelope
	public static final int dirtyLength2D = 512; // < the geometry length needs
													// update
	public static final int dirtyRingAreas2D = 1024; // < m_cached_ring_areas_2D
														// need update
	public static final int dirtyCoordinates = dirtyIsKnownSimple
			| dirtyIntervals | dirtyIsEnvelope | dirtyLength2D
			| dirtyRingAreas2D | dirtyOGCFlags;
	public static final int dirtyAllInternal = 0xFFFF; // there has been no
														// change to the streams
														// from outside.
	public static final int dirtyAll = 0xFFFFFF; // there has been a change to
													// one of attribute streams
													// from the outside.
}
