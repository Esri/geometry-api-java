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

public class NumberUtils {

	public static int snap(int v, int minv, int maxv) {
		return v < minv ? minv : v > maxv ? maxv : v;
	}

	public static long snap(long v, long minv, long maxv) {
		return v < minv ? minv : v > maxv ? maxv : v;
	}

	public static double snap(double v, double minv, double maxv) {
		return v < minv ? minv : v > maxv ? maxv : v;
	}

	static int sizeOf(double v) {
		return 8;
	}

	static int sizeOfDouble() {
		return 8;
	}

	static int sizeOf(int v) {
		return 4;
	}

	static int sizeOf(long v) {
		return 8;
	}

	static int sizeOf(byte v) {
		return 1;
	}

	static boolean isNaN(double d) {
		return Double.isNaN(d);
	}

	final static double TheNaN = Double.NaN;

	static double NaN() {
		return Double.NaN;
	}

	//combines two hash values
	public static int hashCombine(int hash1, int hash2) {
		return (hash1 * 31 + hash2) & 0x7FFFFFFF;
	}
	
	//makes a hash out of an int
	static int hash(int n) {
		int hash = 5381;
		hash = ((hash << 5) + hash) + (n & 0xFF); /* hash * 33 + c */
		hash = ((hash << 5) + hash) + ((n >> 8) & 0xFF);
		hash = ((hash << 5) + hash) + ((n >> 16) & 0xFF);
		hash = ((hash << 5) + hash) + ((n >> 24) & 0xFF);
		hash &= 0x7FFFFFFF;
		return hash;
	}

	//	//makes a hash out of an double
	static int hash(double d) {
		long bits = Double.doubleToLongBits(d);
		int hc = (int) (bits ^ (bits >>> 32));
		return hash(hc);
	}

	//adds an int to a hash value
	static int hash(int hashIn, int n) {
		int hash = ((hashIn << 5) + hashIn) + (n & 0xFF); /* hash * 33 + c */
		hash = ((hash << 5) + hash) + ((n >> 8) & 0xFF);
		hash = ((hash << 5) + hash) + ((n >> 16) & 0xFF);
		hash = ((hash << 5) + hash) + ((n >> 24) & 0xFF);
		hash &= 0x7FFFFFFF;
		return hash;
	}

	//adds a double to a hash value
	static int hash(int hash, double d) {
		long bits = Double.doubleToLongBits(d);
		int hc = (int) (bits ^ (bits >>> 32));
		return hash(hash, hc);
	}

	static long doubleToInt64Bits(double d) {
		return Double.doubleToLongBits(d);
	}

	static double negativeInf() {
		return Double.NEGATIVE_INFINITY;
	}

	static double positiveInf() {
		return Double.POSITIVE_INFINITY;
	}

	static int intMax() {
		return Integer.MAX_VALUE;
	}

	static double doubleEps() {
		return 2.2204460492503131e-016;
	}

	static double doubleMax() {
		return Double.MAX_VALUE;
	}

	static int nextRand(int prevRand) {
		return (1103515245 * prevRand + 12345) & intMax(); // according to Wiki,
															// this is gcc's
	}

}
