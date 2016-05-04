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
 * This class is a base for geometries with many vertices.
 * 
 * The vertex attributes are stored in separate arrays of corresponding type.
 * There are as many arrays as there are attributes in the vertex.
 */
public abstract class MultiVertexGeometry extends Geometry implements
		Serializable {

	@Override
	protected void _assignVertexDescriptionImpl(VertexDescription newDescription) {
		throw new GeometryException("invalid call");
	}
	
	/**
	 * Returns the total vertex count in this Geometry.
	 */
	public abstract int getPointCount();

	/**
	 * Returns given vertex of the Geometry.
	 */
	public abstract Point getPoint(int index);// Java only

	/**
	 * Returns given vertex of the Geometry by value.
	 */
	public void getPoint(int index, Point ptOut) {
		getPointByVal(index, ptOut);
	}

	/**
	 * Sets the vertex at given index of the Geometry.
	 * 
	 * @param index
	 *            The index of the vertex being changed.
	 * @param pointSrc
	 *            The Point instance to set given vertex attributes from. The
	 *            pointSrc can not be empty. <br>
	 *            The method throws if the pointSrc is not of the Point type. <br>
	 *            The attributes, that are present in the pointSrc and missing
	 *            in this Geometry, will be added to the Geometry. <br>
	 *            The vertex attributes missing in the pointSrc but present in
	 *            the Geometry will be set to the default values (see
	 *            VertexDescription::GetDefaultValue).
	 */
	public abstract void setPoint(int index, Point pointSrc);// Java only

	/**
	 * Returns XY coordinates of the given vertex of the Geometry.
	 */
	public abstract Point2D getXY(int index);

	public abstract void getXY(int index, Point2D pt);

	/**
	 * Sets XY coordinates of the given vertex of the Geometry. All other
	 * attributes are unchanged.
	 */
	public abstract void setXY(int index, Point2D pt);

	/**
	 * Returns XYZ coordinates of the given vertex of the Geometry. If the
	 * Geometry has no Z's, the default value for Z is returned (0).
	 */
	abstract Point3D getXYZ(int index);

	/**
	 * Sets XYZ coordinates of the given vertex of the Geometry. If Z attribute
	 * is not present in this Geometry, it is added. All other attributes are
	 * unchanged.
	 */
	abstract void setXYZ(int index, Point3D pt);

	/**
	 * Returns XY coordinates as an array.
	 */
	public Point2D[] getCoordinates2D() {
		Point2D[] arr = new Point2D[getPointCount()];
		queryCoordinates(arr);
		return arr;
	}

	/**
	 * Returns XYZ coordinates as an array.
	 */
	Point3D[] getCoordinates3D() {
		Point3D[] arr = new Point3D[getPointCount()];
		queryCoordinates(arr);
		return arr;
	}

	public abstract void queryCoordinates(Point[] dst);

	/**
	 * Queries XY coordinates as an array. The array must be larg enough (See
	 * GetPointCount()).
	 */
	public abstract void queryCoordinates(Point2D[] dst);

	/**
	 * Queries XYZ coordinates as an array. The array must be larg enough (See
	 * GetPointCount()).
	 */
	abstract void queryCoordinates(Point3D[] dst);

	/**
	 * Returns value of the given vertex attribute as double.
	 * 
	 * @param semantics
	 *            The atribute semantics.
	 * @param index
	 *            is the vertex index in the Geometry.
	 * @param ordinate
	 *            is the ordinate of a vertex attribute (for example, y has
	 *            ordinate of 1, because it is second ordinate of POSITION)
	 * 
	 *            If attribute is not present, the default value is returned.
	 *            See VertexDescription::GetDefaultValue() method.
	 */
	abstract double getAttributeAsDbl(int semantics, int index,
			int ordinate);

	/**
	 * Returns value of the given vertex attribute as int.
	 * 
	 * @param semantics
	 *            The atribute semantics.
	 * @param index
	 *            is the vertex index in the Geometry.
	 * @param ordinate
	 *            is the ordinate of a vertex attribute (for example, y has
	 *            ordinate of 1, because it is second ordinate of POSITION)
	 * 
	 *            If attribute is not present, the default value is returned.
	 *            See VertexDescription::GetDefaultValue() method. Avoid using
	 *            this method on non-integer atributes.
	 */
	abstract int getAttributeAsInt(int semantics, int index, int ordinate);

	/**
	 * Sets the value of given attribute at given posisiotnsis.
	 * 
	 * @param semantics
	 *            The atribute semantics.
	 * @param index
	 *            is the vertex index in the Geometry.
	 * @param ordinate
	 *            is the ordinate of a vertex attribute (for example, y has
	 *            ordinate of 1, because it is seond ordinate of POSITION)
	 * @param value
	 *            is the value to set. as well as the number of components of
	 *            the attribute.
	 * 
	 *            If the attribute is not present in this Geometry, it is added.
	 */
	abstract void setAttribute(int semantics, int index, int ordinate,
			double value);

	/**
	 * Same as above, but works with ints. Avoid using this method on
	 * non-integer atributes because some double attributes may have NaN default
	 * values (e.g. Ms)
	 */
	abstract void setAttribute(int semantics, int index, int ordinate,
			int value);

	/**
	 * Returns given vertex of the Geometry. The outPoint will have same
	 * VertexDescription as this Geometry.
	 */
	public abstract void getPointByVal(int index, Point outPoint);

	/**
	 * Sets the vertex at given index of the Geometry.
	 * 
	 * @param index
	 *            The index of the vertex being changed.
	 * @param pointSrc
	 *            The Point instance to set given vertex attributes from. The
	 *            pointSrc can not be empty. <br>
	 *            The method throws if the pointSrc is not of the Point type. <br>
	 *            The attributes, that are present in the pointSrc and missing
	 *            in this Geometry, will be added to the Geometry. <br>
	 *            The vertex attributes missing in the pointSrc but present in
	 *            the Geometry will be set to the default values (see
	 *            VertexDescription::GetDefaultValue).
	 */
	public abstract void setPointByVal(int index, Point pointSrc);

}
