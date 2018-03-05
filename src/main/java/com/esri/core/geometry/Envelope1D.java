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

import java.io.Serializable;

/**
 * A 1-dimensional interval.
 */
public final class Envelope1D implements Serializable {
	private static final long serialVersionUID = 1L;

	public double vmin;

	public double vmax;

	public Envelope1D() {

	}

	public Envelope1D(double _vmin, double _vmax) {
		setCoords(_vmin, _vmax);
	}

	public Envelope1D(Envelope1D other) {
		setCoords(other);
	}
	
	public void setCoords(double _vmin, double _vmax) {
		vmin = _vmin;
		vmax = _vmax;
		normalize();
	}

	public void setCoords(Envelope1D other) {
		setCoords(other.vmin, other.vmax);
	}
	
	public void normalize() {
		if (NumberUtils.isNaN(vmin))
			return;
		if (vmin > vmax) {
			double v = vmin;
			vmin = vmax;
			vmax = v;
		}
		if (NumberUtils.isNaN(vmax))// vmax can be NAN
		{
			setEmpty();
		}
	}

	public void setEmpty() {
		vmin = NumberUtils.NaN();
		vmax = NumberUtils.NaN();
	}

	public boolean isEmpty() {
		return NumberUtils.isNaN(vmin) || NumberUtils.isNaN(vmax);
	}

	public void setInfinite() {
		vmin = NumberUtils.negativeInf();
		vmax = NumberUtils.positiveInf();
	}

	public void merge(double v) {
		if (isEmpty()) {
			vmin = v;
			vmax = v;
			return;
		}

		// no need to check for NaN, because all comparisons with NaN are false.
		mergeNE(v);
	}

	public void merge(Envelope1D other) {
		if (other.isEmpty())
			return;

		if (isEmpty()) {
			vmin = other.vmin;
			vmax = other.vmax;
			return;
		}

		if (vmin > other.vmin)
			vmin = other.vmin;
		if (vmax < other.vmax)
			vmax = other.vmax;

		if (vmin > vmax)
			setEmpty();
	}

	public void mergeNE(double v) {
		// Note, if v is NaN, vmin and vmax are unchanged
		if (v < vmin)
			vmin = v;
		else if (v > vmax)
			vmax = v;
	}

	public boolean contains(double v) {
		// If vmin is NaN, return false. No need to check for isEmpty.
		return v >= vmin && v <= vmax;
	}

	/**
	 * Returns True if the envelope contains the other envelope (boundary
	 * inclusive). Note: Will return false if either envelope is empty.
	 * @param other The other envelope.
	 * @return Return true if this contains the other.
	 */
	public boolean contains(Envelope1D other)
	{
		return other.vmin >= vmin && other.vmax <= vmax;
	}

	public void intersect(Envelope1D other) {
		if (isEmpty() || other.isEmpty()) {
			setEmpty();
			return;
		}

		if (vmin < other.vmin)
			vmin = other.vmin;
		if (vmax > other.vmax)
			vmax = other.vmax;

		if (vmin > vmax)
			setEmpty();
	}

	public void inflate(double delta) {
		if (isEmpty())
			return;

		vmin -= delta;
		vmax += delta;
		if (vmax < vmin)
			setEmpty();
	}

	double _calculateToleranceFromEnvelope() {
		if (isEmpty())
			return NumberUtils.doubleEps() * 100.0; // GEOMTERYX_EPSFACTOR
													// 100.0;
		double r = Math.abs(vmin) + Math.abs(vmax) + 1;
		return r * NumberUtils.doubleEps() * 100.0; // GEOMTERYX_EPSFACTOR
													// 100.0;
	}

	void normalizeNoNaN_() {
		if (vmin > vmax) {
			double v = vmin;
			vmin = vmax;
			vmax = v;
		}
	}

	void setCoordsNoNaN_(double vmin_, double vmax_) {
		vmin = vmin_;
		vmax = vmax_;
		normalizeNoNaN_();
	}

	public double snapClip(double v) /* const */
	{
		return NumberUtils.snap(v, vmin, vmax);
	}

	public double getWidth() /* const */
	{
		return vmax - vmin;
	}

	public double getCenter() /* const */
	{
		return 0.5 * (vmin + vmax);
	}
	
	@Override
    public boolean equals(Object _other)
    {
		if (_other == this)
			return true;

		if (!(_other instanceof Envelope1D))
			return false;

		Envelope1D other = (Envelope1D) _other;
		if (isEmpty() && other.isEmpty())
			return true;

		if (vmin != other.vmin || vmax != other.vmax)
			return false;

		return true;
    }
	
	@Override
	public int hashCode() {
		return NumberUtils.hash(NumberUtils.hash(vmin), vmax);
	}
	
}
