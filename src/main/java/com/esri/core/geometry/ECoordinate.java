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

class ECoordinate {
	private double m_value;
	private double m_eps;

	ECoordinate() {
		set(0.0, 0.0);
	}

	ECoordinate(double v) {
		set(v);
	}

	ECoordinate(ECoordinate v) {
		set(v);
	}

	double epsCoordinate() {
		return NumberUtils.doubleEps();
	}

	void scaleError(double f) {
		m_eps *= f;
	}

	void setError(double e) {
		m_eps = e;
	}

	void set(double v, double e) {
		m_value = v;
		m_eps = e;
	}

	void set(double v) {
		m_value = v;
		m_eps = 0;
	}

	void set(ECoordinate v) {
		m_value = v.m_value;
		m_eps = v.m_eps;
	}

	double value() {
		return m_value;
	}

	double eps() {
		return m_eps;
	}

	void resetError() {
		m_eps = 0;
	}

	void add(ECoordinate v) // +=
	{
		double r = m_value + v.m_value;
		double e = m_eps + v.m_eps + epsCoordinate() * Math.abs(r);
		m_value = r;
		m_eps = e;
	}

	void add(double v) // +=
	{
		double r = m_value + v;
		double e = m_eps + epsCoordinate() * Math.abs(r);
		m_value = r;
		m_eps = e;
	}

	void sub(ECoordinate v) // -=
	{
		double r = m_value - v.m_value;
		double e = m_eps + v.m_eps + epsCoordinate() * Math.abs(r);
		m_value = r;
		m_eps = e;
	}

	void sub(double v) // -=
	{
		double r = m_value - v;
		double e = m_eps + epsCoordinate() * Math.abs(r);
		m_value = r;
		m_eps = e;
	}

	void add(ECoordinate v_1, ECoordinate v_2) // +
	{
		m_value = v_1.m_value + v_2.m_value;
		m_eps = v_1.m_eps + v_2.m_eps + epsCoordinate() * Math.abs(m_value);
	}

	void add(double v_1, double v_2) // +
	{
		m_value = v_1 + v_2;
		m_eps = epsCoordinate() * Math.abs(m_value);
	}

	void add(ECoordinate v_1, double v_2) // +
	{
		m_value = v_1.m_value + v_2;
		m_eps = v_1.m_eps + epsCoordinate() * Math.abs(m_value);
	}

	void add(double v_1, ECoordinate v_2) // +
	{
		m_value = v_1 + v_2.m_value;
		m_eps = v_2.m_eps + epsCoordinate() * Math.abs(m_value);
	}

	void sub(ECoordinate v_1, ECoordinate v_2) // -
	{
		m_value = v_1.m_value - v_2.m_value;
		m_eps = v_1.m_eps + v_2.m_eps + epsCoordinate() * Math.abs(m_value);
	}

	void sub(double v_1, double v_2) // -
	{
		m_value = v_1 - v_2;
		m_eps = epsCoordinate() * Math.abs(m_value);
	}

	void sub(ECoordinate v_1, double v_2) // -
	{
		m_value = v_1.m_value - v_2;
		m_eps = v_1.m_eps + epsCoordinate() * Math.abs(m_value);
	}

	void sub(double v_1, ECoordinate v_2) // -
	{
		m_value = v_1 - v_2.m_value;
		m_eps = v_2.m_eps + epsCoordinate() * Math.abs(m_value);
	}

	void mul(ECoordinate v) {
		double r = m_value * v.m_value;
		m_eps = m_eps * Math.abs(v.m_value) + v.m_eps * Math.abs(m_value)
				+ m_eps * v.m_eps + epsCoordinate() * Math.abs(r);
		m_value = r;
	}

	void mul(double v) {
		double r = m_value * v;
		m_eps = m_eps * Math.abs(v) + epsCoordinate() * Math.abs(r);
		m_value = r;
	}

	void mul(ECoordinate v_1, ECoordinate v_2) {
		double r = v_1.m_value * v_2.m_value;
		m_eps = v_1.m_eps * Math.abs(v_2.m_value) + v_2.m_eps
				* Math.abs(v_1.m_value) + v_1.m_eps * v_2.m_eps
				+ epsCoordinate() * Math.abs(r);
		m_value = r;
	}

	void mul(double v_1, double v_2) {
		m_value = v_1 * v_2;
		m_eps = epsCoordinate() * Math.abs(m_value);
	}

	void mul(ECoordinate v_1, double v_2) {
		set(v_1);
		mul(v_2);
	}

	void mul(double v_1, ECoordinate v_2) {
		set(v_2);
		mul(v_1);
	}

	void div(ECoordinate divis) {
		double fabsdivis = Math.abs(divis.m_value);
		double r = m_value / divis.m_value;
		double e = (m_eps + Math.abs(r) * divis.m_eps) / fabsdivis;
		if (divis.m_eps > 0.01 * fabsdivis) {// more accurate error calculation
												// for very inaccurate divisor
			double rr = divis.m_eps / fabsdivis;
			e *= (1.0 + (1.0 + rr) * rr);
		}
		m_value = r;
		m_eps = e + epsCoordinate() * Math.abs(r);
	}

	void div(double v) {
		double fabsdivis = Math.abs(v);
		m_value /= v;
		m_eps = m_eps / fabsdivis + epsCoordinate() * Math.abs(m_value);
	}

	void div(ECoordinate v_1, ECoordinate v_2) {
		set(v_1);
		div(v_2);
	}

	void div(double v_1, double v_2) {
		m_value = v_1 / v_2;
		m_eps = epsCoordinate() * Math.abs(m_value);
	}

	void div(ECoordinate v_1, double v_2) {
		set(v_1);
		div(v_2);
	}

	void div(double v_1, ECoordinate v_2) {
		set(v_1);
		div(v_2);
	}

	void sqrt() {
		double r, dr;

		if (m_value >= 0) { // assume non-negative input
			r = Math.sqrt(m_value);
			if (m_value > 10.0 * m_eps) {
				dr = 0.5 * m_eps / r;
			} else {
				dr = (m_value > m_eps) ? r - Math.sqrt(m_value - m_eps) : Math
						.max(r, Math.sqrt(m_value + m_eps) - r);
			}

			dr += epsCoordinate() * Math.abs(r);
		} else {
			if (m_value < -m_eps) { // Assume negative input. Return value
									// undefined
				r = NumberUtils.TheNaN;
				dr = NumberUtils.TheNaN;
			} else { // assume zero input
				r = 0.0;
				dr = Math.sqrt(m_eps);
			}
		}

		m_value = r;
		m_eps = dr;
	}

	void sqr() {
		double r = m_value * m_value;
		m_eps = 2 * m_eps * m_value + m_eps * m_eps + epsCoordinate() * r;
		m_value = r;
	}

	// Assigns sin(angle) to this coordinate.
	void sin(ECoordinate angle) {
		double sinv = Math.sin(angle.m_value);
		double cosv = Math.cos(angle.m_value);
		m_value = sinv;
		double absv = Math.abs(sinv);
		m_eps = (Math.abs(cosv) + absv * 0.5 * angle.m_eps) * angle.m_eps
				+ epsCoordinate() * absv;
	}

	// Assigns cos(angle) to this coordinate.
	void cos(ECoordinate angle) {
		double sinv = Math.sin(angle.m_value);
		double cosv = Math.cos(angle.m_value);
		m_value = cosv;
		double absv = Math.abs(cosv);
		m_eps = (Math.abs(sinv) + absv * 0.5 * angle.m_eps) * angle.m_eps
				+ epsCoordinate() * absv;
	}

	// Calculates natural log of v and assigns to this coordinate
	void log(ECoordinate v) {
		double d = v.m_eps / v.m_value;
		m_value = Math.log(v.m_value);
		m_eps = d * (1.0 + 0.5 * d) + epsCoordinate() * Math.abs(m_value);
	}

	// void SinAndCos(ECoordinate& _sin, ECoordinate& _cos);
	// ECoordinate abs();
	// ECoordinate exp();
	// ECoordinate acos();
	// ECoordinate asin();
	// ECoordinate atan();

	boolean eq(ECoordinate v) // ==
	{
		return Math.abs(m_value - v.m_value) <= m_eps + v.m_eps;
	}

	boolean ne(ECoordinate v) // !=
	{
		return !eq(v);
	}

	boolean GT(ECoordinate v) // >
	{
		return m_value - v.m_value > m_eps + v.m_eps;
	}

	boolean lt(ECoordinate v) // <
	{
		return v.m_value - m_value > m_eps + v.m_eps;
	}

	boolean ge(ECoordinate v) // >=
	{
		return !lt(v);
	}

	boolean le(ECoordinate v) // <=
	{
		return !GT(v);
	}

	// The following methods take into account the rounding erros as well as
	// user defined tolerance.
	boolean tolEq(ECoordinate v, double tolerance) // ! == with tolerance
	{
		return Math.abs(m_value - v.m_value) <= tolerance || eq(v);
	}

	boolean tol_ne(ECoordinate v, double tolerance) // ! !=
	{
		return !tolEq(v, tolerance);
	}

	boolean tolGT(ECoordinate v, double tolerance) // ! >
	{
		return (m_value - v.m_value > tolerance) && GT(v);
	}

	boolean tollt(ECoordinate v, double tolerance) // ! <
	{
		return (v.m_value - m_value > tolerance) && lt(v);
	}

	boolean tolge(ECoordinate v, double tolerance) // ! >=
	{
		return !tollt(v, tolerance);
	}

	boolean tolle(ECoordinate v, double tolerance) // ! <=
	{
		return !tolGT(v, tolerance);
	}

	boolean isZero() {
		return Math.abs(m_value) <= m_eps;
	}

	boolean isFuzzyZero() {
		return isZero() && m_eps != 0.0;
	}

	boolean tolIsZero(double tolerance) {
		return Math.abs(m_value) <= Math.max(m_eps, tolerance);
	}

	void setPi() {
		set(Math.PI, epsCoordinate());
	}

	void setE() {
		set(2.71828182845904523536, epsCoordinate());
	}
}
