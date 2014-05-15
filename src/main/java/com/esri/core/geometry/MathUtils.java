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

class MathUtils {
	/**
	 * The implementation of the Kahan summation algorithm. Use to get better
	 * precision when adding a lot of values.
	 */
	static final class KahanSummator {
		private double sum; // the accumulated sum
		private double compensation;
		private double startValue; // the Base (the class returns sum +
									// startValue)

		/**
		 * initialize to the given start value. \param startValue_ The value to
		 * be added to the accumulated sum.
		 */
		KahanSummator(double startValue_) {
			startValue = startValue_;
			reset();
		}

		/**
		 * Resets the accumulated sum to zero. The getResult() returns
		 * startValue_ after this call.
		 */
		void reset() {
			sum = 0;
			compensation = 0;
		}

		/**
		 * add a value.
		 */
		void add(double v) {
			double y = v - compensation;
			double t = sum + y;
			double h = t - sum;
			compensation = h - y;
			sum = t;
		}

		/**
		 * Subtracts a value.
		 */
		void sub(double v) {
			add(-v);
		}

		/**
		 * add another summator.
		 */
		void add(/* const */KahanSummator v) {
			double y = (v.getResult() + v.compensation) - compensation;
			double t = sum + y;
			double h = t - sum;
			compensation = h - y;
			sum = t;
		}

		/**
		 * Subtracts another summator.
		 */
		void sub(/* const */KahanSummator v) {
			double y = -(v.getResult() - v.compensation) - compensation;
			double t = sum + y;
			double h = t - sum;
			compensation = h - y;
			sum = t;
		}

		/**
		 * Returns current value of the sum.
		 */
		double getResult() /* const */{
			return startValue + sum;
		}

		KahanSummator plusEquals(double v) {
			add(v);
			return this;
		}

		KahanSummator minusEquals(double v) {
			add(-v);
			return this;
		}

		KahanSummator plusEquals(/* const */KahanSummator v) {
			add(v);
			return this;
		}

		KahanSummator minusEquals(/* const */KahanSummator v) {
			sub(v);
			return this;
		}
	}

	/**
	 * Returns one value with the sign of another (like copysign).
	 */
	static double copySign(double x, double y) {
		return y >= 0.0 ? Math.abs(x) : -Math.abs(x);
	}

	/**
	 * C fmod function.
	 */
	static double FMod(double x, double y) {
		return x - Math.floor(x / y) * y;
	}




	/**
	 * Rounds double to the closest integer value.
	 */
	static double round(double v) {
		return Math.floor(v + 0.5);
	}
	static double sqr(double v) {
		return v * v;
	}
}
