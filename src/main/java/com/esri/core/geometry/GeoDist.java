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

import com.esri.core.geometry.PeDouble;

final class GeoDist {
	private static final double PE_PI = 3.14159265358979323846264;
	private static final double PE_PI2 = 1.57079632679489661923132;
	private static final double PE_2PI = 6.283185307179586476925287;
	private static final double PE_EPS = 3.55271367880050092935562e-15;

	/** Get the absolute value of a number */
	static private double PE_ABS(double a) {
		return (a < 0) ? -a : a;
	}

	/** Assign the sign of the second number to the first */
	static private double PE_SGN(double a, double b) {
		return (b >= 0) ? PE_ABS(a) : -PE_ABS(a);
	}

	/** Determine if two doubles are equal within a default tolerance */
	static private boolean PE_EQ(double a, double b) {
		return (a == b)
				|| PE_ABS(a - b) <= PE_EPS * (1 + (PE_ABS(a) + PE_ABS(b)) / 2);
	}

	/** Determine if a double is within a given tolerance of zero */
	static private boolean PE_ZERO(double a) {
		return (a == 0.0) || (PE_ABS(a) <= PE_EPS);
	}

	static private double lam_delta(double lam) {
		double d = Math.IEEEremainder(lam, PE_2PI);

		return (PE_ABS(d) <= PE_PI) ? d : ((d < 0) ? d + PE_2PI : d - PE_2PI);
	}

	static private void lam_phi_reduction(PeDouble p_lam, PeDouble p_phi) {
		p_lam.val = lam_delta(p_lam.val);
		p_phi.val = lam_delta(p_phi.val);

		if (PE_ABS(p_phi.val) > PE_PI2) {
			p_lam.val = lam_delta(p_lam.val + PE_PI);
			p_phi.val = PE_SGN(PE_PI, p_phi.val) - p_phi.val;
		}
	}

	static private double q90(double a, double e2) {
		/*
		 * Rapp // Geometric Geodesy (Part I) // p. 39. Adams, O.S. // Latitude
		 * Developments ... // pp. 122-127. Terms extended past n4 by David
		 * Burrows, ESRI
		 */

		/* Calculate meridional arc distance from equator to pole */

		/*
		 * q90 = a * PE_PI2 * (1 + 1/4 n2 + 1/64 n4 + 1/256 n6 + 25/16384 n8 +
		 * 49/65536 n10 + ...)/(1.0 + n)
		 */

		double t = Math.sqrt(1.0 - e2);
		double n = (1.0 - t) / (1.0 + t);
		double n2 = n * n;

		return a / (1.0 + n)
				* (1.0 + n2 * (1.0 / 4.0 + n2 * (1.0 / 64.0 + n2 * (1.0 / 256.0))))
				* PE_PI2;
	}

	static public void geodesic_distance_ngs(double a, double e2, double lam1,
			double phi1, double lam2, double phi2, PeDouble p_dist,
			PeDouble p_az12, PeDouble p_az21) {
		/* Highly edited version (plus lots of additions) of NGS FORTRAN code */

		/*
		 * inverse for long-line and antipodal cases.* latitudes may be 90
		 * degrees exactly.* latitude positive north, longitude positive east,
		 * radians.* azimuth clockwise from north, radians.* original programmed
		 * by thaddeus vincenty, 1975, 1976* removed back side solution option,
		 * debugged, revised -- 2011may01 -- dgm* this version of code is
		 * interim -- antipodal boundary needs work
		 * 
		 * * output (besides az12, az21, and dist):* These have been removed
		 * from this esri version of the ngs code* it, iteration count* sigma,
		 * spherical distance on auxiliary sphere* lam_sph, longitude difference
		 * on auxiliary sphere* kind, solution flag: kind=1, long-line; kind=2,
		 * antipodal
		 * 
		 * 
		 * All references to Rapp are Part II
		 */

		double tol = 1.0e-14;
		double eps = 1.0e-15;

		double boa = 0.0;
		double dlam = 0.0;
		double eta1 = 0.0, sin_eta1 = 0.0, cos_eta1 = 0.0;
		double eta2 = 0.0, sin_eta2 = 0.0, cos_eta2 = 0.0;
		double prev = 0.0, test = 0.0;
		double sin_lam_sph = 0.0, cos_lam_sph = 0.0, temp = 0.0, sin_sigma = 0.0, cos_sigma = 0.0;
		double sin_azeq = 0.0, cos2_azeq = 0.0, costm = 0.0, costm2 = 0.0, c = 0.0, d = 0.0;
		double tem1 = 0.0, tem2 = 0.0, ep2 = 0.0, bige = 0.0, bigf = 0.0, biga = 0.0, bigb = 0.0, z = 0.0, dsigma = 0.0;
		boolean q_continue_looping;

		double f = 0.0;

		double az12 = 0.0, az21 = 0.0, dist = 0.0;
		double sigma = 0.0, lam_sph = 0.0;
		int it = 0, kind = 0;

		PeDouble lam = new PeDouble();
		PeDouble phi = new PeDouble();

		/* Are there any values to calculate? */
		if (p_dist == null && p_az12 == null && p_az21 == null) {
			return;
		}

		/* Normalize point 1 and 2 */
		lam.val = lam1;
		phi.val = phi1;
		lam_phi_reduction(lam, phi);
		lam1 = lam.val;
		phi1 = phi.val;

		lam.val = lam2;
		phi.val = phi2;
		lam_phi_reduction(lam, phi);
		lam2 = lam.val;
		phi2 = phi.val;

		dlam = lam_delta(lam2 - lam1); /* longitude difference [-Pi, Pi] */

		if (PE_EQ(phi1, phi2) && (PE_ZERO(dlam) || PE_EQ(PE_ABS(phi1), PE_PI2))) {
			/* Check that the points are not the same */
			if (p_dist != null)
				p_dist.val = 0.0;
			if (p_az12 != null)
				p_az12.val = 0.0;
			if (p_az21 != null)
				p_az21.val = 0.0;

			return;
		} else if (PE_EQ(phi1, -phi2)) {
			/* Check if they are perfectly antipodal */
			if (PE_EQ(PE_ABS(phi1), PE_PI2)) {
				/* Check if they are at opposite poles */
				if (p_dist != null)
					p_dist.val = 2.0 * q90(a, e2);

				if (p_az12 != null)
					p_az12.val = phi1 > 0.0 ? lam_delta(PE_PI - lam_delta(lam2))
							: lam_delta(lam2);

				if (p_az21 != null)
					p_az21.val = phi1 > 0.0 ? lam_delta(lam2) : lam_delta(PE_PI
							- lam_delta(lam2));

				return;
			} else if (PE_EQ(PE_ABS(dlam), PE_PI)) {
				/* Other antipodal */
				if (p_dist != null)
					p_dist.val = 2.0 * q90(a, e2);
				if (p_az12 != null)
					p_az12.val = 0.0;
				if (p_az21 != null)
					p_az21.val = 0.0;
				return;
			}
		}

		if (PE_ZERO(e2)) /* Sphere */
		{
			double cos_phi1, cos_phi2;
			double sin_phi1, sin_phi2;

			cos_phi1 = Math.cos(phi1);
			cos_phi2 = Math.cos(phi2);
			sin_phi1 = Math.sin(phi1);
			sin_phi2 = Math.sin(phi2);

			if (p_dist != null) {
				tem1 = Math.sin((phi2 - phi1) / 2.0);
				tem2 = Math.sin(dlam / 2.0);
				sigma = 2.0 * Math.asin(Math.sqrt(tem1 * tem1 + cos_phi1
						* cos_phi2 * tem2 * tem2));
				p_dist.val = sigma * a;
			}

			if (p_az12 != null) {
				if (PE_EQ(PE_ABS(phi1), PE_PI2)) /* Origin at N or S Pole */
				{
					p_az12.val = phi1 < 0.0 ? lam2 : lam_delta(PE_PI - lam2);
				} else {
					p_az12.val = Math.atan2(cos_phi2 * Math.sin(dlam), cos_phi1
							* sin_phi2 - sin_phi1 * cos_phi2 * Math.cos(dlam));
				}
			}

			if (p_az21 != null) {
				if (PE_EQ(PE_ABS(phi2), PE_PI2)) /* Destination at N or S Pole */
				{
					p_az21.val = phi2 < 0.0 ? lam1 : lam_delta(PE_PI - lam1);
				} else {
					p_az21.val = Math.atan2(cos_phi1 * Math.sin(dlam), sin_phi2
							* cos_phi1 * Math.cos(dlam) - cos_phi2 * sin_phi1);
					p_az21.val = lam_delta(p_az21.val + PE_PI);
				}
			}

			return;
		}

		f = 1.0 - Math.sqrt(1.0 - e2);
		boa = 1.0 - f;

		eta1 = Math.atan(boa * Math.tan(phi1)); /* better reduced latitude */
		sin_eta1 = Math.sin(eta1);
		cos_eta1 = Math.cos(eta1);

		eta2 = Math.atan(boa * Math.tan(phi2)); /* better reduced latitude */
		sin_eta2 = Math.sin(eta2);
		cos_eta2 = Math.cos(eta2);

		prev = dlam;
		test = dlam;
		it = 0;
		kind = 1;
		lam_sph = dlam; /* v13 (Rapp ) */

		/* top of the long-line loop (kind = 1) */

		q_continue_looping = true;
		while (q_continue_looping && it < 100) {
			it = it + 1;

			if (kind == 1) {
				sin_lam_sph = Math.sin(lam_sph);

				/*
				 * if ( PE_ABS(PE_PI - PE_ABS(dlam)) < 2.0e-11 ) sin_lam_sph =
				 * 0.0 no--troublesome
				 */

				cos_lam_sph = Math.cos(lam_sph);
				tem1 = cos_eta2 * sin_lam_sph;
				temp = cos_eta1 * sin_eta2 - sin_eta1 * cos_eta2 * cos_lam_sph;
				sin_sigma = Math.sqrt(tem1 * tem1 + temp * temp); /*
																 * v14 (Rapp
																 * 1.87)
																 */
				cos_sigma = sin_eta1 * sin_eta2 + cos_eta1 * cos_eta2
						* cos_lam_sph; /* v15 (Rapp 1.88) */
				sigma = Math.atan2(sin_sigma, cos_sigma); /* (Rapp 1.89) */

				if (PE_ABS(sin_sigma) < eps) /* avoid division by 0 */
				{
					sin_azeq = cos_eta1 * cos_eta2 * sin_lam_sph
							/ PE_SGN(eps, sin_sigma);
				} else {
					sin_azeq = cos_eta1 * cos_eta2 * sin_lam_sph / sin_sigma;
					/* v17 (Rapp 1.90) */
				}

				cos2_azeq = 1.0 - sin_azeq * sin_azeq;

				if (PE_ABS(cos2_azeq) < eps) /* avoid division by 0 */
				{
					costm = cos_sigma - 2.0
							* (sin_eta1 * sin_eta2 / PE_SGN(eps, cos2_azeq));
				} else {
					costm = cos_sigma - 2.0 * (sin_eta1 * sin_eta2 / cos2_azeq);
					/* v18 (Rapp 1.91) */
				}
				costm2 = costm * costm;
				c = ((-3.0 * cos2_azeq + 4.0) * f + 4.0) * cos2_azeq * f / 16.0; /*
																				 * v10
																				 * (
																				 * Rapp
																				 * 1.83
																				 * )
																				 */
			}

			/* entry point of the antipodal loop (kind = 2) */
			d = (1.0 - c)
					* f
					* (sigma + c * sin_sigma
							* (costm + cos_sigma * c * (2.0 * costm2 - 1.0)));
			/* v11 (Rapp 1.84) */

			if (kind == 1) {
				lam_sph = dlam + d * sin_azeq;
				if (PE_ABS(lam_sph - test) < tol) {
					q_continue_looping = false;
					continue;
				}

				if (PE_ABS(lam_sph) > PE_PI) {
					kind = 2;
					lam_sph = PE_PI;
					if (dlam < 0.0) {
						lam_sph = -lam_sph;
					}
					sin_azeq = 0.0;
					cos2_azeq = 1.0;
					test = 2.0;
					prev = test;

					sigma = PE_PI
							- PE_ABS(Math.atan(sin_eta1 / cos_eta1)
									+ Math.atan(sin_eta2 / cos_eta2));
					sin_sigma = Math.sin(sigma);
					cos_sigma = Math.cos(sigma);
					c = ((-3.0 * cos2_azeq + 4.0) * f + 4.0) * cos2_azeq * f
							/ 16.0; /* v10 (Rapp 1.83) */

					if (PE_ABS(sin_azeq - prev) < tol) {
						q_continue_looping = false;
						continue;
					}
					if (PE_ABS(cos2_azeq) < eps) /* avoid division by 0 */
					{
						costm = cos_sigma
								- 2.0
								* (sin_eta1 * sin_eta2 / PE_SGN(eps, cos2_azeq));
					} else {
						costm = cos_sigma - 2.0
								* (sin_eta1 * sin_eta2 / cos2_azeq);
						/* v18 (Rapp 1.91) */
					}
					costm2 = costm * costm;
					continue;
				}

				if (((lam_sph - test) * (test - prev)) < 0.0 && it > 5) {
					/* refined converge */
					lam_sph = (2.0 * lam_sph + 3.0 * test + prev) / 6.0;
				}
				prev = test;
				test = lam_sph;
				continue;
			} else /* kind == 2 */
			{
				sin_azeq = (lam_sph - dlam) / d;
				if (((sin_azeq - test) * (test - prev)) < 0.0 && it > 5) {
					/* refined converge */
					sin_azeq = (2.0 * sin_azeq + 3.0 * test + prev) / 6.0;
				}
				prev = test;
				test = sin_azeq;
				cos2_azeq = 1.0 - sin_azeq * sin_azeq;
				sin_lam_sph = sin_azeq * sin_sigma / (cos_eta1 * cos_eta2);
				cos_lam_sph = -Math
						.sqrt(PE_ABS(1.0 - sin_lam_sph * sin_lam_sph));
				lam_sph = Math.atan2(sin_lam_sph, cos_lam_sph);
				tem1 = cos_eta2 * sin_lam_sph;
				temp = cos_eta1 * sin_eta2 - sin_eta1 * cos_eta2 * cos_lam_sph;
				sin_sigma = Math.sqrt(tem1 * tem1 + temp * temp);
				cos_sigma = sin_eta1 * sin_eta2 + cos_eta1 * cos_eta2
						* cos_lam_sph;
				sigma = Math.atan2(sin_sigma, cos_sigma);
				c = ((-3.0 * cos2_azeq + 4.0) * f + 4.0) * cos2_azeq * f / 16.0; /*
																				 * v10
																				 * (
																				 * Rapp
																				 * 1.83
																				 * )
																				 */
				if (PE_ABS(sin_azeq - prev) < tol) {
					q_continue_looping = false;
					continue;
				}
				if (PE_ABS(cos2_azeq) < eps) /* avoid division by 0 */
				{
					costm = cos_sigma - 2.0
							* (sin_eta1 * sin_eta2 / PE_SGN(eps, cos2_azeq));
				} else {
					costm = cos_sigma - 2.0 * (sin_eta1 * sin_eta2 / cos2_azeq);
					/* v18 (Rapp 1.91) */
				}
				costm2 = costm * costm;
				continue;
			}
		} /* End of while q_continue_looping */

		/* Convergence */

		if (p_dist != null) {
			/*
			 * Helmert 1880 from Vincenty's
			 * "Geodetic inverse solution between antipodal points"
			 */

			ep2 = 1.0 / (boa * boa) - 1.0;
			bige = Math.sqrt(1.0 + ep2 * cos2_azeq); /* 15 */
			bigf = (bige - 1.0) / (bige + 1.0); /* 16 */
			biga = (1.0 + bigf * bigf / 4.0) / (1.0 - bigf); /* 17 */
			bigb = bigf * (1.0 - 0.375 * bigf * bigf); /* 18 */
			z = bigb / 6.0 * costm * (-3.0 + 4.0 * sin_sigma * sin_sigma)
					* (-3.0 + 4.0 * costm2);
			dsigma = bigb
					* sin_sigma
					* (costm + bigb / 4.0
							* (cos_sigma * (-1.0 + 2.0 * costm2) - z)); /* 19 */
			dist = (boa * a) * biga * (sigma - dsigma); /* 20 */

			p_dist.val = dist;
		}

		if (p_az12 != null || p_az21 != null) {
			if (kind == 2) /* antipodal */
			{
				az12 = sin_azeq / cos_eta1;
				az21 = Math.sqrt(1.0 - az12 * az12);
				if (temp < 0.0) {
					az21 = -az21;
				}
				az12 = Math.atan2(az12, az21);
				tem1 = -sin_azeq;
				tem2 = sin_eta1 * sin_sigma - cos_eta1 * cos_sigma * az21;
				az21 = Math.atan2(tem1, tem2);
			} else /* long-line */
			{
				tem1 = cos_eta2 * sin_lam_sph;
				tem2 = cos_eta1 * sin_eta2 - sin_eta1 * cos_eta2 * cos_lam_sph;
				az12 = Math.atan2(tem1, tem2);
				tem1 = -cos_eta1 * sin_lam_sph;
				tem2 = sin_eta1 * cos_eta2 - cos_eta1 * sin_eta2 * cos_lam_sph;
				az21 = Math.atan2(tem1, tem2);
			}

			if (p_az12 != null) {
				p_az12.val = lam_delta(az12);
			}
			if (p_az21 != null) {
				p_az21.val = lam_delta(az21);
			}
		}
	}
}
