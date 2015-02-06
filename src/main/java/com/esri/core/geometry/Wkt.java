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

final class Wkt {
	public static double find_tolerance_from_wkt(String buffer) {
		double tolerance = -1.0;

		if (buffer != null && buffer.length() > 0) {
			int n1, n2;

			n1 = buffer.indexOf("PROJCS");
			if (n1 >= 0) {
				double factor = 0.0;

				n1 = buffer.lastIndexOf("UNIT");
				if (n1 >= 0) {
					n1 = buffer.indexOf(',', n1 + 4);
					if (n1 > 0) {
						n1++;
						n2 = buffer.indexOf(']', n1 + 1);
						if (n2 > 0) {
							try {
								factor = Double.parseDouble(buffer.substring(
										n1, n2));
							} catch (NumberFormatException e) {
								factor = 0.0;
							}
						}
					}
				}

				if (factor > 0.0)
					tolerance = (0.001 / factor);
			}

			else {
				n1 = buffer.indexOf("GEOGCS");
				if (n1 >= 0) {
					double axis = 0.0;
					double factor = 0.0;

					n1 = buffer.indexOf("SPHEROID", n1 + 6);
					if (n1 > 0) {
						n1 = buffer.indexOf(',', n1 + 8);
						if (n1 > 0) {
							n1++;
							n2 = buffer.indexOf(',', n1 + 1);
							if (n2 > 0) {
								try {
									axis = Double.parseDouble(buffer.substring(
											n1, n2));
								} catch (NumberFormatException e) {
									axis = 0.0;
								}
							}

							if (axis > 0.0) {
								n1 = buffer.indexOf("UNIT", n2 + 1);
								if (n1 >= 0) {
									n1 = buffer.indexOf(',', n1 + 4);
									if (n1 > 0) {
										n1++;
										n2 = buffer.indexOf(']', n1 + 1);
										if (n2 > 0) {
											try {
												factor = Double
														.parseDouble(buffer
																.substring(n1,
																		n2));
											} catch (NumberFormatException e) {
												factor = 0.0;
											}
										}
									}
								}
							}
						}
					}

					if (axis > 0.0 && factor > 0.0)
						tolerance = (0.001 / (axis * factor));
				}
			}
		}

		return tolerance;
	}
}
