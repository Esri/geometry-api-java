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
 * Proximity operators are used to find the distance between two geometries or
 * the distance from a given point to the nearest point on another geometry.
 */
public class Proximity2DResult {
	Point2D m_coordinate = new Point2D();
	int m_vertexIndex;
	double m_distance;
	int m_info;

	/**
	 * Sets the right_side info to true or false.
	 * 
	 * @param bRight
	 *            Whether the nearest coordinate is to the right or left of the
	 *            geometry.
	 */
	public void setRightSide(boolean bRight) {
		if (bRight)
			m_info |= (int) OperatorProximity2D.ProxResultInfo.rightSide;
		else
			m_info &= ~(int) OperatorProximity2D.ProxResultInfo.rightSide;
	}

	/**
	 * Returns TRUE if the Proximity2DResult is empty. This only happens if the
	 * Geometry passed to the Proximity operator is empty.
	 */
	public boolean isEmpty() {
		return m_vertexIndex < 0;
	}

	/**
	 * Returns the closest coordinate for
	 * OperatorProximity2D.getNearestCoordinate or the vertex coordinates for
	 * the OperatorProximity2D.getNearestVertex and
	 * OperatorProximity2D.getNearestVertices.
	 */
	public Point getCoordinate() {
		if (isEmpty())
			throw new GeometryException("invalid call");

		return new Point(m_coordinate.x, m_coordinate.y);
	}

	/**
	 * Returns the vertex index. For OperatorProximity2D.getNearestCoordinate
	 * the behavior is: When the input is a polygon or an envelope and the
	 * bTestPolygonInterior is true, the value is zero. When the input is a
	 * polygon or an Envelope and the bTestPolygonInterior is false, the value
	 * is the start vertex index of a segment with the closest coordinate. When
	 * the input is a polyline, the value is the start vertex index of a segment
	 * with the closest coordinate. When the input is a point, the value is 0.
	 * When the input is a multipoint, the value is the closest vertex.
	 */
	public int getVertexIndex() {
		if (isEmpty())
			throw new GeometryException("invalid call");

		return m_vertexIndex;
	}

	/**
	 * Returns the distance to the closest vertex or coordinate.
	 */
	public double getDistance() {
		if (isEmpty())
			throw new GeometryException("invalid call");

		return m_distance;
	}

	/**
	 *Returns true if the closest coordinate is to the right of the MultiPath.
	 */
	public boolean isRightSide() {
		return (m_info & (int) OperatorProximity2D.ProxResultInfo.rightSide) != 0;
	}

	void _setParams(double x, double y, int vertexIndex, double distance) {
		m_coordinate.x = x;
		m_coordinate.y = y;
		m_vertexIndex = vertexIndex;
		m_distance = distance;
	}

	// static int _compare(Proximity2DResult v1, Proximity2DResult v2)
	// {
	// if (v1.m_distance < v2.m_distance)
	// return -1;
	// if (v1.m_distance == v2.m_distance)
	// return 0;
	//
	// return 1;
	// }

	Proximity2DResult() {
		m_vertexIndex = -1;
	}

	Proximity2DResult(Point2D coordinate, int vertexIndex, double distance) {
		m_coordinate.setCoords(coordinate);
		m_vertexIndex = vertexIndex;
		m_distance = distance;
		m_info = 0;
	}
}
