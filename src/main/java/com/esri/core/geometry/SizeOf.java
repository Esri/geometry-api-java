/*
 Copyright 1995-2018 Esri

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

import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_BYTE_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_CHAR_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_INT_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;
import static sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;

public final class SizeOf {
	public static final int SIZE_OF_ATTRIBUTE_STREAM_OF_FLOAT = 24;

	public static final int SIZE_OF_ATTRIBUTE_STREAM_OF_DBL = 24;

	public static final int SIZE_OF_ATTRIBUTE_STREAM_OF_INT8 = 24;

	public static final int SIZE_OF_ATTRIBUTE_STREAM_OF_INT16 = 24;

	public static final int SIZE_OF_ATTRIBUTE_STREAM_OF_INT32 = 24;

	public static final int SIZE_OF_ATTRIBUTE_STREAM_OF_INT64 = 24;

	public static final int SIZE_OF_ENVELOPE = 32;

	public static final int SIZE_OF_ENVELOPE2D = 48;

	public static final int SIZE_OF_LINE = 56;

	public static final int SIZE_OF_MULTI_PATH = 24;

	public static final int SIZE_OF_MULTI_PATH_IMPL = 112;

	public static final int SIZE_OF_MULTI_POINT = 24;

	public static final int SIZE_OF_MULTI_POINT_IMPL = 56;

	public static final int SIZE_OF_POINT = 24;

	public static final int SIZE_OF_POLYGON = 24;

	public static final int SIZE_OF_POLYLINE = 24;

	public static final int SIZE_OF_OGC_CONCRETE_GEOMETRY_COLLECTION = 24;

	public static final int SIZE_OF_OGC_LINE_STRING = 24;

	public static final int SIZE_OF_OGC_MULTI_LINE_STRING = 24;

	public static final int SIZE_OF_OGC_MULTI_POINT = 24;

	public static final int SIZE_OF_OGC_MULTI_POLYGON = 24;

	public static final int SIZE_OF_OGC_POINT = 24;

	public static final int SIZE_OF_OGC_POLYGON = 24;
	
	public static final int SIZE_OF_MAPGEOMETRY = 24;

	public static long sizeOfByteArray(int length) {
		return ARRAY_BYTE_BASE_OFFSET + (((long) ARRAY_BYTE_INDEX_SCALE) * length);
	}

	public static long sizeOfShortArray(int length) {
		return ARRAY_SHORT_BASE_OFFSET + (((long) ARRAY_SHORT_INDEX_SCALE) * length);
	}

	public static long sizeOfCharArray(int length) {
		return ARRAY_CHAR_BASE_OFFSET + (((long) ARRAY_CHAR_INDEX_SCALE) * length);
	}

	public static long sizeOfIntArray(int length) {
		return ARRAY_INT_BASE_OFFSET + (((long) ARRAY_INT_INDEX_SCALE) * length);
	}

	public static long sizeOfLongArray(int length) {
		return ARRAY_LONG_BASE_OFFSET + (((long) ARRAY_LONG_INDEX_SCALE) * length);
	}

	public static long sizeOfFloatArray(int length) {
		return ARRAY_FLOAT_BASE_OFFSET + (((long) ARRAY_FLOAT_INDEX_SCALE) * length);
	}

	public static long sizeOfDoubleArray(int length) {
		return ARRAY_DOUBLE_BASE_OFFSET + (((long) ARRAY_DOUBLE_INDEX_SCALE) * length);
	}

	private SizeOf() {
	}
}
