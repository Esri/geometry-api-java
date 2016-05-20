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

/**
 * The 3D affine transformation class.
 * 
 * We use matrices for transformations of the vectors as rows. That means the
 * math expressions on the Geometry matrix operations should be writen like
 * this: v' = v * M1 * M2 * M3 = ( (v * M1) * M2 ) * M3, where v is a vector, Mn
 * are the matrices. This is equivalent to the following line of code:
 * ResultVector = (M1.Mul(M2).Mul(M3)).Transform(Vector)
 */
final class Transformation3D {

	public double xx, yx, zx, xd, xy, yy, zy, yd, xz, yz, zz, zd;

	public Transformation3D() {

	}

	/**
	 * Sets all elements to 0, thus producing and invalid transformation.
	 */
	public void setZero() {
		xx = 0.0;
		yx = 0.0;
		zx = 0.0;
		xy = 0.0;
		yy = 0.0;
		zy = 0.0;
		xz = 0.0;
		yz = 0.0;
		zz = 0.0;
		xd = 0.0;
		yd = 0.0;
		zd = 0.0;
	}

	public void setScale(double scaleX, double scaleY, double scaleZ) {
		xx = scaleX;
		yx = 0.0;
		zx = 0.0;
		xy = 0.0;
		yy = scaleY;
		zy = 0.0;
		xz = 0.0;
		yz = 0.0;
		zz = scaleZ;
		xd = 0.0;
		yd = 0.0;
		zd = 0.0;
	}

	public void setTranslate(double deltax, double deltay, double deltaz) {
		xx = 1.0;
		yx = 0.0;
		zx = 0.0;
		xy = 0.0;
		yy = 1.0;
		zy = 0.0;
		xz = 0.0;
		yz = 0.0;
		zz = 1.0;
		xd = deltax;
		yd = deltay;
		zd = deltaz;
	}

	public void translate(double deltax, double deltay, double deltaz) {
		xd += deltax;
		yd += deltay;
		zd += deltaz;
	}

	/**
	 * Transforms an envelope. The result is the bounding box of the transformed
	 * envelope.
	 */
	public Envelope3D transform(Envelope3D env) {

		if (env.isEmpty())
			return env;

		Point3D[] buf = new Point3D[8];
		env.queryCorners(buf);

		transform(buf, 8, buf);
		env.setFromPoints(buf);
		return env;
	}

	void transform(Point3D[] pointsIn, int count, Point3D[] pointsOut) {
		for (int i = 0; i < count; i++) {
			Point3D res = new Point3D();
			Point3D src = pointsIn[i];
			res.x = xx * src.x + xy * src.y + xz * src.z + xd;
			res.y = yx * src.x + yy * src.y + yz * src.z + yd;
			res.z = zx * src.x + zy * src.y + zz * src.z + zd;
			pointsOut[i] = res;
		}
	}

	public Point3D transform(Point3D src) {
		Point3D res = new Point3D();
		res.x = xx * src.x + xy * src.y + xz * src.z + xd;
		res.y = yx * src.x + yy * src.y + yz * src.z + yd;
		res.z = zx * src.x + zy * src.y + zz * src.z + zd;
		return res;
	}

	public void transform(Point3D[] points, int start, int count) {
		int n = Math.min(points.length, start + count);
		for (int i = start; i < n; i++) {
			Point3D res = new Point3D();
			Point3D src = points[i];
			res.x = xx * src.x + xy * src.y + xz * src.z + xd;
			res.y = yx * src.x + yy * src.y + yz * src.z + yd;
			res.z = zx * src.x + zy * src.y + zz * src.z + zd;
			points[i] = res;
		}
	}

	public void mul(Transformation3D right) {
		multiply(this, right, this);
	}

	public void mulLeft(Transformation3D left) {
		multiply(left, this, this);
	}

	/**
	 * Performs multiplication of matrices a and b and places result into
	 * result. The a, b, and result could point to same objects. <br>
	 * Equivalent to result = a * b.
	 */
	// static
	public static void multiply(Transformation3D a, Transformation3D b,
			Transformation3D result) {
		double xx, yx, zx;
		double xy, yy, zy;
		double xz, yz, zz;
		double xd, yd, zd;

		xx = a.xx * b.xx + a.yx * b.xy + a.zx * b.xz;
		yx = a.xx * b.yx + a.yx * b.yy + a.zx * b.yz;
		zx = a.xx * b.zx + a.yx * b.zy + a.zx * b.zz;
		xy = a.xy * b.xx + a.yy * b.xy + a.zy * b.xz;
		yy = a.xy * b.yx + a.yy * b.yy + a.zy * b.yz;
		zy = a.xy * b.zx + a.yy * b.zy + a.zy * b.zz;
		xz = a.xz * b.xx + a.yz * b.xy + a.zz * b.xz;
		yz = a.xz * b.yx + a.yz * b.yy + a.zz * b.yz;
		zz = a.xz * b.zx + a.yz * b.zy + a.zz * b.zz;
		xd = a.xd * b.xx + a.yd * b.xy + a.zd * b.xz + b.xd;
		yd = a.xd * b.yx + a.yd * b.yy + a.zd * b.yz + b.yd;
		zd = a.xd * b.zx + a.yd * b.zy + a.zd * b.zz + b.zd;

		result.xx = xx;
		result.yx = yx;
		result.zx = zx;
		result.xy = xy;
		result.yy = yy;
		result.zy = zy;
		result.xz = xz;
		result.yz = yz;
		result.zz = zz;
		result.xd = xd;
		result.yd = yd;
		result.zd = zd;
	}

	/**
	 * Calculates the Inverse transformation.
	 * 
	 * @param src
	 *            The input transformation.
	 * @param result
	 *            The inverse of the input transformation. Throws the
	 *            GeometryException("math singularity") exception if the Inverse
	 *            can not be calculated.
	 */
	public static void inverse(Transformation3D src, Transformation3D result) {
		double det = src.xx * (src.yy * src.zz - src.zy * src.yz) - src.yx
				* (src.xy * src.zz - src.zy * src.xz) + src.zx
				* (src.xy * src.yz - src.yy * src.xz);
		if (det != 0) {
			double xx, yx, zx;
			double xy, yy, zy;
			double xz, yz, zz;
			double xd, yd, zd;

			double det_1 = 1.0 / det;
			xx = (src.yy * src.zz - src.zy * src.yz) * det_1;
			xy = -(src.xy * src.zz - src.zy * src.xz) * det_1;
			xz = (src.xy * src.yz - src.yy * src.xz) * det_1;

			yx = -(src.yx * src.zz - src.yz * src.zx) * det_1;
			yy = (src.xx * src.zz - src.zx * src.xz) * det_1;
			yz = -(src.xx * src.yz - src.yx * src.xz) * det_1;

			zx = (src.yx * src.zy - src.zx * src.yy) * det_1;
			zy = -(src.xx * src.zy - src.zx * src.xy) * det_1;
			zz = (src.xx * src.yy - src.yx * src.xy) * det_1;

			xd = -(src.xd * xx + src.yd * xy + src.zd * xz);
			yd = -(src.xd * yx + src.yd * yy + src.zd * yz);
			zd = -(src.xd * zx + src.yd * zy + src.zd * zz);

			result.xx = xx;
			result.yx = yx;
			result.zx = zx;
			result.xy = xy;
			result.yy = yy;
			result.zy = zy;
			result.xz = xz;
			result.yz = yz;
			result.zz = zz;
			result.xd = xd;
			result.yd = yd;
			result.zd = zd;
		} else {
			throw new GeometryException("math singularity");
		}
	}

	public Transformation3D copy() {
		Transformation3D result = new Transformation3D();
		result.xx = xx;
		result.yx = yx;
		result.zx = zx;
		result.xy = xy;
		result.yy = yy;
		result.zy = zy;
		result.xz = xz;
		result.yz = yz;
		result.zz = zz;
		result.xd = xd;
		result.yd = yd;
		result.zd = zd;
		return result;
	}
}
