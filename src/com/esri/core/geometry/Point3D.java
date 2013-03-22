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

/**
 * 
 * Basic 3D point class.
 * 
 */
final class Point3D {
	public double x;
	public double y;
	public double z;

	public Point3D() {
	}

	public static Point3D construct(double x, double y, double z) {
		Point3D pt = new Point3D();
		pt.x = x;
		pt.y = y;
		pt.z = z;
		return pt;
	}

	public void setCoords(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setZero() {
		x = 0.0;
		y = 0.0;
		z = 0.0;
	}

	public void normalize() {
		double len = length();
		if (len != 0)
			return;

		x /= len;
		y /= len;
		z /= len;
	}

	double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	Point3D sub(Point3D other) {
		return new Point3D(x - other.x, y - other.y, z - other.z);
	}

	Point3D mul(double factor) {
		return new Point3D(x * factor, y * factor, z * factor);
	}

	void _setNan() {
		x = NumberUtils.NaN();
	}

	boolean _isNan() {
		return NumberUtils.isNaN(x);
	}

}
