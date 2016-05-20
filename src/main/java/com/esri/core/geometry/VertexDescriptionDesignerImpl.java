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

import java.util.Arrays;

import com.esri.core.geometry.VertexDescription.Semantics;

/**
 * This factory class allows to describe and create a VertexDescription
 * instance.
 */
final class VertexDescriptionDesignerImpl {
	static VertexDescription getVertexDescription(int descriptionBitMask) {
		return VertexDescriptionHash.getInstance()
				.FindOrAdd(descriptionBitMask);
	}

	static VertexDescription getMergedVertexDescription(
			VertexDescription descr1, VertexDescription descr2) {
		int mask = descr1.m_semanticsBitArray | descr2.m_semanticsBitArray;
		if ((mask & descr1.m_semanticsBitArray) == mask) {
			return descr1;
		} else if ((mask & descr2.m_semanticsBitArray) == mask) {
			return descr2;
		}

		return getVertexDescription(mask);
	}

	static VertexDescription getMergedVertexDescription(
			VertexDescription descr, int semantics) {
		int mask = descr.m_semanticsBitArray | (1 << semantics);
		if ((mask & descr.m_semanticsBitArray) == mask) {
			return descr;
		}

		return getVertexDescription(mask);
	}

	static VertexDescription removeSemanticsFromVertexDescription(
			VertexDescription descr, int semanticsToRemove) {
		int mask = (descr.m_semanticsBitArray | (1 << (int) semanticsToRemove))
				- (1 << (int) semanticsToRemove);
		if (mask == descr.m_semanticsBitArray) {
			return descr;
		}

		return getVertexDescription(mask);
	}

	static VertexDescription getDefaultDescriptor2D() {
		return VertexDescriptionHash.getInstance().getVD2D();
	}

	static VertexDescription getDefaultDescriptor3D() {
		return VertexDescriptionHash.getInstance().getVD3D();
	}

	static int[] mapAttributes(VertexDescription src, VertexDescription dest) {
		int[] srcToDst = new int[src.getAttributeCount()];
		Arrays.fill(srcToDst, -1);
		for (int i = 0, nsrc = src.getAttributeCount(); i < nsrc; i++) {
			srcToDst[i] = dest.getAttributeIndex(src.getSemantics(i));
		}
		return srcToDst;
	}
}
