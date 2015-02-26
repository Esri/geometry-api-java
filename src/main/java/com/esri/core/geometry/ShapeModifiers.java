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

interface ShapeModifiers {
	public static final int ShapeHasZs = 0x80000000;
	public static final int ShapeHasMs = 0x40000000;
	public static final int ShapeHasCurves = 0x20000000;
	public static final int ShapeHasIDs = 0x10000000;
	public static final int ShapeHasNormals = 0x08000000;
	public static final int ShapeHasTextures = 0x04000000;
	public static final int ShapeHasPartIDs = 0x02000000;
	public static final int ShapeHasMaterials = 0x01000000;
	public static final int ShapeIsCompressed = 0x00800000;
	public static final int ShapeModifierMask = 0xFF000000;
	public static final int ShapeMultiPatchModifierMask = 0x0F00000;
	public static final int ShapeBasicTypeMask = 0x000000FF;
	public static final int ShapeBasicModifierMask = 0xC0000000;
	public static final int ShapeNonBasicModifierMask = 0x3F000000;
	public static final int ShapeExtendedModifierMask = 0xDD000000;
}
