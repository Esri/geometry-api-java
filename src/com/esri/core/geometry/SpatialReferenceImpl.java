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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import java.lang.ref.*;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.GeoDist;
import com.esri.core.geometry.GeometryException;
import com.esri.core.geometry.PeDouble;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.SpatialReferenceImpl;
import com.esri.core.geometry.VertexDescription.Semantics;

class SpatialReferenceImpl extends SpatialReference {
	static boolean no_projection_engine = true;
	public static int c_SULIMIT32 = 2147483645;
	public static long c_SULIMIT64 = 9007199254740990L;

	enum Precision {
		Integer32, Integer64, FloatingPoint
	};

	int m_userWkid;// this wkid is provided by user to the create method.
	int m_userLatestWkid;
	int m_userOldestWkid;
	String m_userWkt;// a string, the well-known text.

	// public SgCoordRef m_sgCoordRef;

	private final static ReentrantLock m_lock = new ReentrantLock();

	// TODO If one was going to create member object for locking it would be
	// here.
	SpatialReferenceImpl() {
		m_userWkid = 0;
		m_userLatestWkid = -1;
		m_userOldestWkid = -1;
		m_userWkt = null;
	}

	@Override
	public int getID() {
		return m_userWkid;
	}

	double getFalseX() {
		return 0;
	}

	double getFalseY() {
		return 0;
	}

	double getFalseZ() {
		return 0;
	}

	double getFalseM() {
		return 0;
	}

	double getGridUnitsXY() {
		return 1 / (1.0e-9 * 0.0174532925199433/* getOneDegreeGCSUnit() */);
	}

	double getGridUnitsZ() {
		return 1 / 0.001;
	}

	double getGridUnitsM() {
		return 1 / 0.001;
	}

	Precision getPrecision() {
		return Precision.Integer64;
	}

	@Override
	double getTolerance(int semantics) {
		double tolerance = 0.001;
		if (m_userWkid != 0) {
			tolerance = Wkid.find_tolerance_from_wkid(m_userWkid);
		} else if (m_userWkt != null) {
			tolerance = Wkt.find_tolerance_from_wkt(m_userWkt);
		}
		return tolerance;
	}

	public void queryValidCoordinateRange(Envelope2D env2D) {
		double delta = 0;
		switch (getPrecision()) {
		case Integer32:
			delta = c_SULIMIT32 / getGridUnitsXY();
			break;
		case Integer64:
			delta = c_SULIMIT64 / getGridUnitsXY();
			break;
		default:
			// TODO
			throw new GeometryException("internal error");// fixme
		}

		env2D.setCoords(getFalseX(), getFalseY(), getFalseX() + delta,
				getFalseY() + delta);
	}

	public boolean requiresReSimplify(SpatialReference dst) {
		return dst != this;
	}

	@Override
	public String getText() {
		return m_userWkt;
	}

	/**
	 * Returns the oldest value of the well known ID for the horizontal
	 * coordinate system of the spatial reference. This ID is used for JSON
	 * serialization. Not public.
	 */
	@Override
	int getOldID() {
		int ID_ = getID();

		if (m_userOldestWkid != -1)
			return m_userOldestWkid;

		m_userOldestWkid = Wkid.wkid_to_old(ID_);

		if (m_userOldestWkid != -1)
			return m_userOldestWkid;

		return ID_;
	}

	/**
	 * Returns the latest value of the well known ID for the horizontal
	 * coordinate system of the spatial reference. This ID is used for JSON
	 * serialization. Not public.
	 */
	@Override
	int getLatestID() {
		int ID_ = getID();

		if (m_userLatestWkid != -1)
			return m_userLatestWkid;

		m_userLatestWkid = Wkid.wkid_to_new(ID_);

		if (m_userLatestWkid != -1)
			return m_userLatestWkid;

		return ID_;
	}

	public static SpatialReferenceImpl createImpl(int wkid) {
		if (wkid <= 0)
			throw new IllegalArgumentException("Invalid or unsupported wkid: "
					+ wkid);

		SpatialReferenceImpl spatRef = new SpatialReferenceImpl();
		spatRef.m_userWkid = wkid;

		return spatRef;
	}

	public static SpatialReferenceImpl createImpl(String wkt) {
		if (wkt == null || wkt.length() == 0)
			throw new IllegalArgumentException(
					"Cannot create SpatialReference from null or empty text.");

		SpatialReferenceImpl spatRef = new SpatialReferenceImpl();
		spatRef.m_userWkt = wkt;

		return spatRef;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		SpatialReferenceImpl sr = (SpatialReferenceImpl) obj;
		if (m_userWkid != sr.m_userWkid)
			return false;

		if (m_userWkid == 0) {
			if (!m_userWkt.equals(m_userWkt))// m_userWkt cannot be null here!
				return false;
		}

		return true;
	}

	static double geodesicDistanceOnWGS84Impl(Point ptFrom, Point ptTo) {
		double a = 6378137.0; // radius of spheroid for WGS_1984
		double e2 = 0.0066943799901413165; // ellipticity for WGS_1984
		double rpu = Math.PI / 180.0;
		PeDouble answer = new PeDouble();
		GeoDist.geodesic_distance_ngs(a, e2, ptFrom.getXY().x * rpu,
				ptFrom.getXY().y * rpu, ptTo.getXY().x * rpu, ptTo.getXY().y
						* rpu, answer, null, null);
		return answer.val;
	}

}
