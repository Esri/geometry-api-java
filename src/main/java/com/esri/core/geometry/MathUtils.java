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

final class MathUtils {
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
	 * Calculates sign of the given value. Returns 0 if the value is equal to 0.
	 */
	static int sign(double value) {
		return value < 0 ? -1 : (value > 0) ? 1 : 0;
	}

	/**
	 * Rounds towards zero.
	 */
	static double truncate(double v) {
		if (v >= 0)
			return Math.floor(v);
		else
			return -Math.floor(-v);
	}
	
	/**
	 * C fmod function.
	 */
	static double FMod(double x, double y) {
		return x - truncate(x / y) * y;
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

    /**
    *Computes interpolation between two values, using the interpolation factor t.
    *The interpolation formula is (end - start) * t + start.
    *However, the computation ensures that t = 0 produces exactly start, and t = 1, produces exactly end.
    *It also guarantees that for 0 <= t <= 1, the interpolated value v is between start and end.
    */
	static double lerp(double start_, double end_, double t) {
		// When end == start, we want result to be equal to start, for all t
		// values. At the same time, when end != start, we want the result to be
		// equal to start for t==0 and end for t == 1.0
		// The regular formula end_ * t + (1.0 - t) * start_, when end_ ==
		// start_, and t at 1/3, produces value different from start
		double v;
		if (t <= 0.5)
			v = start_ + (end_ - start_) * t;
		else
			v = end_ - (end_ - start_) * (1.0 - t);

		assert (t < 0 || t > 1.0 || (v >= start_ && v <= end_) || (v <= start_ && v >= end_) || NumberUtils.isNaN(start_) || NumberUtils.isNaN(end_));
		return v;
	}

    /**
    *Computes interpolation between two values, using the interpolation factor t.
    *The interpolation formula is (end - start) * t + start.
    *However, the computation ensures that t = 0 produces exactly start, and t = 1, produces exactly end.
    *It also guarantees that for 0 <= t <= 1, the interpolated value v is between start and end.
    */
	static void lerp(Point2D start_, Point2D end_, double t, Point2D result) {
		assert(start_ != result);
		// When end == start, we want result to be equal to start, for all t
		// values. At the same time, when end != start, we want the result to be
		// equal to start for t==0 and end for t == 1.0
		// The regular formula end_ * t + (1.0 - t) * start_, when end_ ==
		// start_, and t at 1/3, produces value different from start
		double rx, ry;
		if (t <= 0.5) {
			rx = start_.x + (end_.x - start_.x) * t;
			ry = start_.y + (end_.y - start_.y) * t;
		}
		else {
			rx = end_.x - (end_.x - start_.x) * (1.0 - t);
			ry = end_.y - (end_.y - start_.y) * (1.0 - t);
		}

		assert (t < 0 || t > 1.0 || (rx >= start_.x && rx <= end_.x) || (rx <= start_.x && rx >= end_.x));
		assert (t < 0 || t > 1.0 || (ry >= start_.y && ry <= end_.y) || (ry <= start_.y && ry >= end_.y));
		result.x = rx;
		result.y = ry;
	}

	static void lerp(double start_x, double start_y, double end_x, double end_y, double t, Point2D result) {
		// When end == start, we want result to be equal to start, for all t
		// values. At the same time, when end != start, we want the result to be
		// equal to start for t==0 and end for t == 1.0
		// The regular formula end_ * t + (1.0 - t) * start_, when end_ ==
		// start_, and t at 1/3, produces value different from start
		if (t <= 0.5) {
			result.x = start_x + (end_x - start_x) * t;
			result.y = start_y + (end_y - start_y) * t;
		}
		else {
			result.x = end_x - (end_x - start_x) * (1.0 - t);
			result.y = end_y - (end_y - start_y) * (1.0 - t);
		}

		assert (t < 0 || t > 1.0 || (result.x >= start_x && result.x <= end_x) || (result.x <= start_x && result.x >= end_x));
		assert (t < 0 || t > 1.0 || (result.y >= start_y && result.y <= end_y) || (result.y <= start_y && result.y >= end_y));
	}
	
}
