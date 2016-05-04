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
 * The affine transformation class for 2D.
 * 
 * Vector is a row: 
 * <code>
 * <br>           |m11 m12 0|
 * <br>| x y 1| * |m21 m22 0| = |m11 * x + m21 * y + m31   m12 * x + m22 * y + m32   1|
 * <br>           |m31 m32 1|
 * <br>Then elements of the Transformation2D are as follows:
 * <br>           |xx  yx  0|
 * <br>| x y 1| * |xy  yy  0| = |xx * x + xy * y + xd   yx * x + yy * y + yd    1|
 * <br>           |xd  yd  1|
 * <br>
 * </code> Matrices are used for transformations of the vectors as rows (case
 * 2). That means the math expressions on the Geometry matrix operations should
 * be writen like this: <br>
 * v' = v * M1 * M2 * M3 = ( (v * M1) * M2 ) * M3, where v is a vector, Mn are
 * the matrices. <br>
 * This is equivalent to the following line of code: <br>
 * ResultVector = (M1.mul(M2).mul(M3)).transform(Vector)
 */
public final class Transformation2D {

	/**
	 * Matrix coefficient XX of the transformation.
	 */
	public double xx;
	/**
	 * Matrix coefficient XY of the transformation.
	 */
	public double xy;
	/**
	 * X translation component of the transformation.
	 */
	public double xd;
	/**
	 * Matrix coefficient YX of the transformation.
	 */

	public double yx;
	/**
	 * Matrix coefficient YY of the transformation.
	 */
	public double yy;
	/**
	 * Y translation component of the transformation.
	 */

	public double yd;

	/**
	 * Creates a 2D affine transformation with identity transformation.
	 */
	public Transformation2D() {
		setIdentity();
	}

	/**
	 * Creates a 2D affine transformation with a specified scale.
	 * 
	 * @param scale
	 *            The scale to use for the transformation.
	 */
	public Transformation2D(double scale) {
		setScale(scale);
	}

	/**
	 * Initializes a zero transformation. Transforms any coordinate to (0, 0).
	 */
	public void setZero() {
		xx = 0;
		yy = 0;
		xy = 0;
		yx = 0;
		xd = 0;
		yd = 0;
	}

	void transform(Point2D psrc, Point2D pdst) {
		double x = xx * psrc.x + xy * psrc.y + xd;
		double y = yx * psrc.x + yy * psrc.y + yd;
		pdst.x = x;
		pdst.y = y;
	}

	/**
	 * Returns True when all members of this transformation are equal to the
	 * corresponding members of the other.
	 */

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Transformation2D))
			return false;
		Transformation2D that = (Transformation2D) other;

		return (xx == that.xx && xy == that.xy && xd == that.xd
				&& yx == that.yx && yy == that.yy && yd == that.yd);
	}

	/**
	 * Returns the hash code for the 2D transformation.
	 */

	@Override
	public int hashCode() {
		int hash = NumberUtils.hash(xx);
		hash = NumberUtils.hash(hash, xy);
		hash = NumberUtils.hash(hash, xd);
		hash = NumberUtils.hash(hash, yx);
		hash = NumberUtils.hash(hash, yy);
		hash = NumberUtils.hash(hash, yd);
		return hash;
	}

	void transform(Point2D[] points, int start, int count) {
		int n = Math.min(points.length, start + count);
		for (int i = count; i < n; i++) {
			transform(points[i], points[i]);
		}
	}

	/**
	 * Transforms an array of points.
	 * 
	 * @param pointsIn
	 *            The points to be transformed.
	 * @param count
	 *            The number of points to transform.
	 * @param pointsOut
	 *            The transformed points are returned using this array. It
	 *            should have the same or greater size as the input array.
	 */
	public void transform(Point[] pointsIn, int count, Point[] pointsOut) {
		Point2D res = new Point2D();
		for (int i = 0; i < count; i++) {
			Point2D p = pointsIn[i].getXY();
			res.x = xx * p.x + xy * p.y + xd;
			res.y = yx * p.x + yy * p.y + yd;
			pointsOut[i] = new Point(res.x, res.y);
		}
	}

	/**
	 * Transforms an array of points stored in an array of doubles as
	 * interleaved XY coordinates.
	 * 
	 * @param pointsXYInterleaved
	 *            The array of points with interleaved X, Y values to be
	 *            transformed.
	 * @param start
	 *            The start point index to transform from (the actual element
	 *            index is 2 * start).
	 * @param count
	 *            The number of points to transform (the actual element count is
	 *            2 * count).
	 */
	public void transform(double[] pointsXYInterleaved, int start, int count) {
		int n = Math.min(pointsXYInterleaved.length, (start + count) * 2) / 2;
		for (int i = count; i < n; i++) {
			double px = pointsXYInterleaved[2 * i];
			double py = pointsXYInterleaved[2 * i + 1];
			pointsXYInterleaved[2 * i] = xx * px + xy * py + xd;
			pointsXYInterleaved[2 * i + 1] = yx * px + yy * py + yd;
		}
	}

	/**
	 * Multiplies this matrix on the right with the "right" matrix. Stores the
	 * result into this matrix and returns a reference to it. <br>
	 * Equivalent to this *= right.
	 * 
	 * @param right
	 *            The matrix to be multiplied with.
	 */
	public void multiply(Transformation2D right) {
		multiply(this, right, this);
	}

	/**
	 * Multiplies this matrix on the left with the "left" matrix. Stores the
	 * result into this matrix and returns a reference to it. <br>
	 * Equivalent to this = left * this.
	 * 
	 * @param left
	 *            The matrix to be multiplied with.
	 */
	public void mulLeft(Transformation2D left) {
		multiply(left, this, this);
	}

	/**
	 * Performs multiplication of matrices a and b and places the result into
	 * this matrix. The a, b, and result could point to same objects. <br>
	 * Equivalent to result = a * b.
	 * 
	 * @param a
	 *            The 2D transformation to be multiplied.
	 * @param b
	 *            The 2D transformation to be multiplied.
	 * @param result
	 *            The 2D transformation created by multiplication of matrices.
	 */
	public static void multiply(Transformation2D a, Transformation2D b,
			Transformation2D result) {
		double xx, xy, xd, yx, yy, yd;

		xx = a.xx * b.xx + a.yx * b.xy;
		xy = a.xy * b.xx + a.yy * b.xy;
		xd = a.xd * b.xx + a.yd * b.xy + b.xd;
		yx = a.xx * b.yx + a.yx * b.yy;
		yy = a.xy * b.yx + a.yy * b.yy;
		yd = a.xd * b.yx + a.yd * b.yy + b.yd;

		result.xx = xx;
		result.xy = xy;
		result.xd = xd;
		result.yx = yx;
		result.yy = yy;
		result.yd = yd;
	}

	/**
	 * Returns a copy of the Transformation2D object.
	 * 
	 * @return A copy of this object.
	 */
	public Transformation2D copy() {
		Transformation2D result = new Transformation2D();
		result.xx = xx;
		result.xy = xy;
		result.xd = xd;
		result.yx = yx;
		result.yy = yy;
		result.yd = yd;
		return result;
	}

	/**
	 * Writes the matrix coefficients in the order XX, XY, XD, YX, YY, YD into
	 * the given array.
	 * 
	 * @param coefs
	 *            The array into which the coefficients are returned. Should be
	 *            of size 6 elements.
	 */
	public void getCoefficients(double[] coefs) {
		if (coefs.length < 6)
			throw new GeometryException(
					"Buffer is too small. coefs needs 6 members");

		coefs[0] = xx;
		coefs[1] = xy;
		coefs[2] = xd;
		coefs[3] = yx;
		coefs[4] = yy;
		coefs[5] = yd;
	}

	/**
	 * Transforms envelope
	 * 
	 * @param env
	 *            The envelope that is to be transformed
	 */
	void transform(Envelope2D env) {

		if (env.isEmpty())
			return;

		Point2D[] buf = new Point2D[4];
		env.queryCorners(buf);
		transform(buf, buf);
		env.setFromPoints(buf, 4);
	}

	void transform(Point2D[] pointsIn, Point2D[] pointsOut) {
		for (int i = 0; i < pointsIn.length; i++) {
			Point2D res = new Point2D();
			Point2D p = pointsIn[i];
			res.x = xx * p.x + xy * p.y + xd;
			res.y = yx * p.x + yy * p.y + yd;
			pointsOut[i] = res;
		}
	}

	/**
	 * Initialize transformation from two rectangles.
	 */
	void initializeFromRect(Envelope2D src, Envelope2D dest) {
		if (src.isEmpty() || dest.isEmpty() || 0 == src.getWidth()
				|| 0 == src.getHeight())
			setZero();
		else {
			xy = yx = 0;
			xx = dest.getWidth() / src.getWidth();
			yy = dest.getHeight() / src.getHeight();
			xd = dest.xmin - src.xmin * xx;
			yd = dest.ymin - src.ymin * yy;
		}
	}

	/**
	 * Initializes an orhtonormal transformation from the Src and Dest
	 * rectangles.
	 * 
	 * The result transformation proportionally fits the Src into the Dest. The
	 * center of the Src will be in the center of the Dest.
	 */
	void initializeFromRectIsotropic(Envelope2D src, Envelope2D dest) {

		if (src.isEmpty() || dest.isEmpty() || 0 == src.getWidth()
				|| 0 == src.getHeight())
			setZero();
		else {
			yx = 0;
			xy = 0;
			xx = dest.getWidth() / src.getWidth();
			yy = dest.getHeight() / src.getHeight();
			if (xx > yy)
				xx = yy;
			else
				yy = xx;

			Point2D destCenter = dest.getCenter();
			Point2D srcCenter = src.getCenter();
			xd = destCenter.x - srcCenter.x * xx;
			yd = destCenter.y - srcCenter.y * yy;
		}
	}

	/**
	 * Initializes transformation from Position, Tangent vector and offset
	 * value. Tangent vector must have unity length
	 */
	void initializeFromCurveParameters(Point2D Position, Point2D Tangent,
			double Offset) {
		// TODO
	}

	/**
	 * Transforms size.
	 * 
	 * Creates an AABB with width of SizeSrc.x and height of SizeSrc.y.
	 * Transforms that AABB and gets a quadrangle in new coordinate system. The
	 * result x contains the length of the quadrangle edge, which were parallel
	 * to X in the original system, and y contains the length of the edge, that
	 * were parallel to the Y axis in the original system.
	 */
	Point2D transformSize(Point2D SizeSrc) {
		Point2D pt = new Point2D();
		pt.x = Math.sqrt(xx * xx + yx * yx) * SizeSrc.x;
		pt.y = Math.sqrt(xy * xy + yy * yy) * SizeSrc.y;
		return pt;
	}

	/**
	 * Transforms a tolerance value.
	 * 
	 * @param tolerance
	 *            The tolerance value.
	 */
	public double transform(double tolerance) {
		// the function should be implemented as follows: find encompassing
		// circle for the transformed circle of radius = Tolerance.

		// this is approximation.
		Point2D pt1 = new Point2D();
		Point2D pt2 = new Point2D();
		/*
		 * pt[0].Set(0, 0); pt[1].Set(1, 0); pt[2].Set(0, 1); Transform(pt);
		 * pt[1] -= pt[0]; pt[2] -= pt[0];
		 */

		pt1.setCoords(xx, yx);
		pt2.setCoords(xy, yy);
		pt1.sub(pt1);
		double d1 = pt1.sqrLength() * 0.5;
		pt1.setCoords(xx, yx);
		pt2.setCoords(xy, yy);
		pt1.add(pt2);
		double d2 = pt1.sqrLength() * 0.5;
		return tolerance * ((d1 > d2) ? Math.sqrt(d1) : Math.sqrt(d2));
	}

	// Performs linear part of the transformation only. Same as if xd, yd would
	// be zeroed.
	void transformWithoutShift(Point2D[] pointsIn, int from, int count,
			Point2D[] pointsOut) {
		for (int i = from, n = from + count; i < n; i++) {
			Point2D p = pointsIn[i];
			double new_x = xx * p.x + xy * p.y;
			double new_y = yx * p.x + yy * p.y;
			pointsOut[i].setCoords(new_x, new_y);
		}
	}

	Point2D transformWithoutShift(Point2D srcPoint) {
		double new_x = xx * srcPoint.x + xy * srcPoint.y;
		double new_y = yx * srcPoint.x + yy * srcPoint.y;
		return Point2D.construct(new_x, new_y);
	}

	/**
	 * Sets this matrix to be the identity matrix.
	 */
	public void setIdentity() {
		xx = 1.0;
		xy = 0;
		xd = 0;
		yx = 0;
		yy = 1.0;
		yd = 0;
	}

	/**
	 * Returns TRUE if this matrix is the identity matrix.
	 */
	public boolean isIdentity() {
		return xx == 1.0 && yy == 1.0
				&& (0 == xy && 0 == xd && 0 == yx && 0 == yd);
	}

	/**
	 * Returns TRUE if this matrix is an identity matrix within the given
	 * tolerance.
	 * 
	 * @param tol
	 *            The tolerance value.
	 */
	public boolean isIdentity(double tol) {
		Point2D pt = Point2D.construct(0., 1.);
		transform(pt, pt);
		pt.sub(Point2D.construct(0., 1.));
		if (pt.sqrLength() > tol * tol)
			return false;

		pt.setCoords(0, 0);
		transform(pt, pt);
		if (pt.sqrLength() > tol * tol)
			return false;

		pt.setCoords(1.0, 0.0);
		transform(pt, pt);
		pt.sub(Point2D.construct(1.0, 0.0));
		return pt.sqrLength() <= tol * tol;
	}

	/**
	 * Returns TRUE for reflective transformations. It inverts the sign of
	 * vector cross product.
	 */
	public boolean isReflective() {
		return xx * yy - yx * xy < 0;
	}

	/**
	 * Returns TRUE if this transformation is a uniform transformation.
	 * 
	 * The uniform transformation is a transformation, which transforms a square
	 * to a square.
	 */
	public boolean isUniform(double eps) {
		double v1 = xx * xx + yx * yx;
		double v2 = xy * xy + yy * yy;
		double e = (v1 + v2) * eps;
		return Math.abs(v1 - v2) <= e && Math.abs(xx * xy + yx * yy) <= e;
	}

	/**
	 * Returns TRUE if this transformation is a shift transformation. The shift
	 * transformation performs shift only.
	 */
	public boolean isShift() {
		return xx == 1.0 && yy == 1.0 && 0 == xy && 0 == yx;
	}

	/**
	 * Returns TRUE if this transformation is a shift transformation within the
	 * given tolerance.
	 * 
	 * @param tol
	 *            The tolerance value.
	 */
	public boolean isShift(double tol) {
		Point2D pt = transformWithoutShift(Point2D.construct(0.0, 1.0));
		pt.y -= 1.0;
		if (pt.sqrLength() > tol * tol)
			return false;

		pt = transformWithoutShift(Point2D.construct(1.0, 0.0));
		pt.x -= 1.0;
		return pt.sqrLength() <= tol * tol;
	}

	/**
	 * Returns TRUE if this is an orthonormal transformation with the given
	 * tolerance. The orthonormal: Rotation or rotoinversion and shift
	 * (preserves lengths of vectors and angles between vectors).
	 * 
	 * @param tol
	 *            The tolerance value.
	 */
	public boolean isOrthonormal(double tol) {
		Transformation2D r = new Transformation2D();
		r.xx = xx * xx + xy * xy;
		r.xy = xx * yx + xy * yy;
		r.yx = yx * xx + yy * xy;
		r.yy = yx * yx + yy * yy;
		r.xd = 0;
		r.yd = 0;

		return r.isIdentity(tol);
	}

	/**
	 * Returns TRUE if this matrix is degenerated (does not have an inverse)
	 * within the given tolerance.
	 * 
	 * @param tol
	 *            The tolerance value.
	 */
	public boolean isDegenerate(double tol) {
		return Math.abs(xx * yy - yx * xy) <= 2 * tol
				* (Math.abs(xx * yy) + Math.abs(yx * xy));
	}

	/**
	 * Returns TRUE, if this transformation does not have rotation and shear
	 * within the given tolerance.
	 * 
	 * @param tol
	 *            The tolerance value.
	 */
	public boolean isScaleAndShift(double tol) {
		return xy * xy + yx * yx < (xx * xx + yy * yy) * tol;
	}

	/**
	 * Set this transformation to be a shift.
	 * 
	 * @param x
	 *            The X coordinate to shift to.
	 * @param y
	 *            The Y coordinate to shift to.
	 */
	public void setShift(double x, double y) {
		xx = 1;
		xy = 0;
		xd = x;
		yx = 0;
		yy = 1;
		yd = y;
	}

	/**
	 * Set this transformation to be a scale.
	 * 
	 * @param x
	 *            The X coordinate to scale to.
	 * @param y
	 *            The Y coordinate to scale to.
	 */
	public void setScale(double x, double y) {
		xx = x;
		xy = 0;
		xd = 0;
		yx = 0;
		yy = y;
		yd = 0;
	}

	/**
	 * Set transformation to be a uniform scale.
	 * 
	 * @param _scale
	 *            The scale of the transformation.
	 */
	public void setScale(double _scale) {
		setScale(_scale, _scale);
	}

	/**
	 * Sets the transformation to be a flip around the X axis. Flips the X
	 * coordinates so that the x0 becomes x1 and vice verse.
	 * 
	 * @param x0
	 *            The X coordinate to flip.
	 * @param x1
	 *            The X coordinate to flip to.
	 */
	public void setFlipX(double x0, double x1) {
		xx = -1;
		xy = 0;
		xd = x0 + x1;
		yx = 0;
		yy = 1;
		yd = 0;
	}

	/**
	 * Sets the transformation to be a flip around the Y axis. Flips the Y
	 * coordinates so that the y0 becomes y1 and vice verse.
	 * 
	 * @param y0
	 *            The Y coordinate to flip.
	 * @param y1
	 *            The Y coordinate to flip to.
	 */
	public void setFlipY(double y0, double y1) {
		xx = 1;
		xy = 0;
		xd = 0;
		yx = 0;
		yy = -1;
		yd = y0 + y1;
	}

	/**
	 * Set transformation to a shear.
	 * 
	 * @param proportionX
	 *            The proportion of shearing in x direction.
	 * @param proportionY
	 *            The proportion of shearing in y direction.
	 */
	public void setShear(double proportionX, double proportionY) {
		xx = 1;
		xy = proportionX;
		xd = 0;
		yx = proportionY;
		yy = 1;
		yd = 0;
	}

	/**
	 * Sets this transformation to be a rotation around point (0, 0).
	 * 
	 * When the axis Y is directed up and X is directed to the right, the
	 * positive angle corresponds to the anti-clockwise rotation. When the axis
	 * Y is directed down and X is directed to the right, the positive angle
	 * corresponds to the clockwise rotation.
	 * 
	 * @param angle_in_Radians
	 *            The rotation angle in radian.
	 */
	public void setRotate(double angle_in_Radians) {
		setRotate(Math.cos(angle_in_Radians), Math.sin(angle_in_Radians));
	}

	/**
	 * Produces a transformation that swaps x and y coordinate values. xx = 0.0;
	 * xy = 1.0; xd = 0; yx = 1.0; yy = 0.0; yd = 0;
	 */
	Transformation2D setSwapCoordinates() {
		xx = 0.0;
		xy = 1.0;
		xd = 0;
		yx = 1.0;
		yy = 0.0;
		yd = 0;
		return this;
	}

	/**
	 * Sets this transformation to be a rotation around point rotationCenter.
	 * 
	 * When the axis Y is directed up and X is directed to the right, the
	 * positive angle corresponds to the anti-clockwise rotation. When the axis
	 * Y is directed down and X is directed to the right, the positive angle
	 * corresponds to the clockwise rotation.
	 * 
	 * @param angle_in_Radians
	 *            The rotation angle in radian.
	 * @param rotationCenter
	 *            The center point of the rotation.
	 */
	void setRotate(double angle_in_Radians, Point2D rotationCenter) {
		setRotate(Math.cos(angle_in_Radians), Math.sin(angle_in_Radians),
				rotationCenter);
	}

	/**
	 * Sets rotation for this transformation.
	 * 
	 * When the axis Y is directed up and X is directed to the right, the
	 * positive angle corresponds to the anti-clockwise rotation. When the axis
	 * Y is directed down and X is directed to the right, the positive angle
	 * corresponds to the clockwise rotation.
	 * 
	 * @param cosA
	 *            The rotation angle.
	 * @param sinA
	 *            The rotation angle.
	 */

	public void setRotate(double cosA, double sinA) {
		xx = cosA;
		xy = -sinA;
		xd = 0;
		yx = sinA;
		yy = cosA;
		yd = 0;
	}

	/**
	 * Sets this transformation to be a rotation around point rotationCenter.
	 * 
	 * When the axis Y is directed up and X is directed to the right, the
	 * positive angle corresponds to the anti-clockwise rotation. When the axis
	 * Y is directed down and X is directed to the right, the positive angle
	 * corresponds to the clockwise rotation.
	 * 
	 * @param cosA
	 *            The cos of the rotation angle.
	 * @param sinA
	 *            The sin of the rotation angle.
	 * @param rotationCenter
	 *            The center point of the rotation.
	 */
	void setRotate(double cosA, double sinA, Point2D rotationCenter) {
		setShift(-rotationCenter.x, -rotationCenter.y);
		Transformation2D temp = new Transformation2D();
		temp.setRotate(cosA, sinA);
		multiply(temp);
		shift(rotationCenter.x, rotationCenter.y);
	}

	/**
	 * Shifts the transformation.
	 * 
	 * @param x
	 *            The shift factor in X direction.
	 * @param y
	 *            The shift factor in Y direction.
	 */
	public void shift(double x, double y) {
		xd += x;
		yd += y;
	}

	/**
	 * Scales the transformation.
	 * 
	 * @param x
	 *            The scale factor in X direction.
	 * @param y
	 *            The scale factor in Y direction.
	 */
	public void scale(double x, double y) {
		xx *= x;
		xy *= x;
		xd *= x;
		yx *= y;
		yy *= y;
		yd *= y;
	}

	/**
	 * Flips the transformation around the X axis.
	 * 
	 * @param x0
	 *            The X coordinate to flip.
	 * @param x1
	 *            The X coordinate to flip to.
	 */
	public void flipX(double x0, double x1) {
		xx = -xx;
		xy = -xy;
		xd = x0 + x1 - xd;
	}

	/**
	 * Flips the transformation around the Y axis.
	 * 
	 * @param y0
	 *            The Y coordinate to flip.
	 * @param y1
	 *            The Y coordinate to flip to.
	 */
	public void flipY(double y0, double y1) {
		yx = -yx;
		yy = -yy;
		yd = y0 + y1 - yd;
	}

	/**
	 * Shears the transformation.
	 * 
	 * @param proportionX
	 *            The proportion of shearing in x direction.
	 * @param proportionY
	 *            The proportion of shearing in y direction.
	 */
	public void shear(double proportionX, double proportionY) {
		Transformation2D temp = new Transformation2D();
		temp.setShear(proportionX, proportionY);
		multiply(temp);
	}

	/**
	 * Rotates the transformation.
	 * 
	 * @param angle_in_Radians
	 *            The rotation angle in radian.
	 */
	public void rotate(double angle_in_Radians) {
		Transformation2D temp = new Transformation2D();
		temp.setRotate(angle_in_Radians);
		multiply(temp);
	}

	/**
	 * Rotates the transformation.
	 * 
	 * @param cos
	 *            The cos angle of the rotation.
	 * @param sin
	 *            The sin angle of the rotation.
	 */
	public void rotate(double cos, double sin) {
		Transformation2D temp = new Transformation2D();
		temp.setRotate(cos, sin);
		multiply(temp);
	}

	/**
	 * Rotates the transformation aroung a center point.
	 * 
	 * @param cos
	 *            The cos angle of the rotation.
	 * @param sin
	 *            sin angle of the rotation.
	 * @param rotationCenter
	 *            The center point of the rotation.
	 */
	public void rotate(double cos, double sin, Point2D rotationCenter) {
		Transformation2D temp = new Transformation2D();
		temp.setRotate(cos, sin, rotationCenter);
		multiply(temp);
	}

	/**
	 * Produces inverse matrix for this matrix and puts result into the inverse
	 * parameter.
	 * 
	 * @param inverse
	 *            The result inverse matrix.
	 */
	public void inverse(Transformation2D inverse) {
		double det = xx * yy - xy * yx;

		if (det == 0) {
			inverse.setZero();
			return;
		}

		det = 1 / det;

		inverse.xd = (xy * yd - xd * yy) * det;
		inverse.yd = (xd * yx - xx * yd) * det;
		inverse.xx = yy * det;
		inverse.xy = -xy * det;
		inverse.yx = -yx * det;
		inverse.yy = xx * det;
	}

	/**
	 * Inverses the matrix.
	 * 
	 */
	public void inverse() {
		inverse(this);
	}

	/**
	 * Extracts scaling part of the transformation. this == scale *
	 * rotateNshearNshift.
	 * 
	 * @param scale
	 *            The destination matrix where the scale part is copied.
	 * @param rotateNshearNshift
	 *            The destination matrix where the part excluding rotation is
	 *            copied.
	 */
	public void extractScaleTransform(Transformation2D scale,
			Transformation2D rotateNshearNshift) {

		scale.setScale(Math.sqrt(xx * xx + xy * xy),
				Math.sqrt(yx * yx + yy * yy));
		rotateNshearNshift.setScale(1.0 / scale.xx, 1.0 / scale.yy);
		rotateNshearNshift.multiply(this);
	}

}
