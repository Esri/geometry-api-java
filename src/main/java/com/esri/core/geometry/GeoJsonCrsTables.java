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

import java.util.Arrays;

class GeoJsonCrsTables {
	static int getWkidFromCrsShortForm(String crs_identifier) {
		int last_colon = crs_identifier.lastIndexOf((int) ':'); // skip version

		if (last_colon == -1)
			return -1;

		int code_start = last_colon + 1;
		int wkid = getWkidFromCrsCode_(crs_identifier, code_start);
		return wkid;
	}

	static int getWkidFromCrsName(String crs_identifier) {
		int wkid = -1;

		int last_colon = crs_identifier.lastIndexOf((int) ':'); // skip
																// authority,
																// version, and
																// other things.
																// Just try to
																// get a wkid.
																// This works
																// for
																// short/long
																// form.

		if (last_colon == -1)
			return -1;

		int code_start = last_colon + 1;
		wkid = getWkidFromCrsCode_(crs_identifier, code_start);

		if (wkid != -1)
			return wkid;

		wkid = getWkidFromCrsOgcUrn(crs_identifier); // could be an OGC
														// "preferred" urn
		return wkid;
	}

	static int getWkidFromCrsOgcUrn(String crs_identifier) {
		int wkid = -1;
		if (crs_identifier.regionMatches(0, "urn:ogc:def:crs:OGC", 0, 19))
			wkid = getWkidFromCrsOgcUrn_(crs_identifier);

		return wkid;
	}

	private static int getWkidFromCrsCode_(String crs_identifier, int code_start) {
		assert(code_start > 0);

		int wkid = -1;
		int code_count = crs_identifier.length() - code_start;

		try {
			wkid = Integer.parseInt(crs_identifier.substring(code_start, code_start + code_count));
		} catch (Exception e) {
		}

		return (int) wkid;
	}

	private static int getWkidFromCrsOgcUrn_(String crs_identifier) {
		assert(crs_identifier.regionMatches(0, "urn:ogc:def:crs:OGC", 0, 19));

		int last_colon = crs_identifier.lastIndexOf((int) ':'); // skip version

		if (last_colon == -1)
			return -1;

		int ogc_code_start = last_colon + 1;
		int ogc_code_count = crs_identifier.length() - ogc_code_start;

		if (crs_identifier.regionMatches(ogc_code_start, "CRS84", 0, ogc_code_count))
			return 4326;

		if (crs_identifier.regionMatches(ogc_code_start, "CRS83", 0, ogc_code_count))
			return 4269;

		if (crs_identifier.regionMatches(ogc_code_start, "CRS27", 0, ogc_code_count))
			return 4267;

		return -1;
	}

	static int getWkidFromCrsHref(String crs_identifier) {
		int sr_org_code_start = -1;

		if (crs_identifier.regionMatches(0, "http://spatialreference.org/ref/epsg/", 0, 37))
			sr_org_code_start = 37;
		else if (crs_identifier.regionMatches(0, "www.spatialreference.org/ref/epsg/", 0, 34))
			sr_org_code_start = 34;
		else if (crs_identifier.regionMatches(0, "http://www.spatialreference.org/ref/epsg/", 0, 41))
			sr_org_code_start = 41;

		if (sr_org_code_start != -1) {
			int sr_org_code_end = crs_identifier.indexOf('/', sr_org_code_start);

			if (sr_org_code_end == -1)
				return -1;

			int count = sr_org_code_end - sr_org_code_start;
			int wkid = -1;

			try {
				wkid = Integer.parseInt(crs_identifier.substring(sr_org_code_start, sr_org_code_start + count));
			} catch (Exception e) {
			}

			return wkid;
		}

		int open_gis_epsg_slash_end = -1;

		if (crs_identifier.regionMatches(0, "http://opengis.net/def/crs/EPSG/", 0, 32))
			open_gis_epsg_slash_end = 32;
		else if (crs_identifier.regionMatches(0, "www.opengis.net/def/crs/EPSG/", 0, 29))
			open_gis_epsg_slash_end = 29;
		else if (crs_identifier.regionMatches(0, "http://www.opengis.net/def/crs/EPSG/", 0, 36))
			open_gis_epsg_slash_end = 36;

		if (open_gis_epsg_slash_end != -1) {
			int last_slash = crs_identifier.lastIndexOf('/'); // skip over the
																// "0/"

			if (last_slash == -1)
				return -1;

			int open_gis_code_start = last_slash + 1;

			int count = crs_identifier.length() - open_gis_code_start;
			int wkid = -1;

			try {
				wkid = Integer.parseInt(crs_identifier.substring(open_gis_code_start, open_gis_code_start + count));
			} catch (Exception e) {
			}

			return wkid;
		}

		if (crs_identifier.compareToIgnoreCase("http://spatialreference.org/ref/sr-org/6928/ogcwkt/") == 0)
			return 3857;

		return -1;
	}

	static String getWktFromCrsName(String crs_identifier) {
		int last_colon = crs_identifier.lastIndexOf((int) ':'); // skip
																// authority
		int wkt_start = last_colon + 1;
		int wkt_count = crs_identifier.length() - wkt_start;
		String wkt = crs_identifier.substring(wkt_start, wkt_start + wkt_count);
		return wkt;
	}
}
