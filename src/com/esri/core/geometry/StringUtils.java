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

class StringUtils {
	static void appendDouble(double value, int precision,
			StringBuilder stringBuilder) {
		if (precision < 0)
			precision = 0;
		else if (precision > 17)
			precision = 17;

		String format = "%." + precision + "g";

		String str_dbl = String.format(format, value);

		boolean b_found_dot = false;
		boolean b_found_exponent = false;

		for (int i = 0; i < str_dbl.length(); i++) {
			char c = str_dbl.charAt(i);

			if (c == '.')
				b_found_dot = true;
			else if (c == 'e' || c == 'E')
				b_found_exponent = true;
		}

		if (b_found_dot && !b_found_exponent) {
			StringBuilder buffer = new StringBuilder(str_dbl);
			int non_zero = buffer.length() - 1;

			while (buffer.charAt(non_zero) == '0')
				non_zero--;

			buffer.delete(non_zero + 1, buffer.length());

			if (buffer.charAt(non_zero) == '.')
				buffer.deleteCharAt(non_zero);

			stringBuilder.append(buffer);
		} else {
			stringBuilder.append(str_dbl);
		}
	}

}
