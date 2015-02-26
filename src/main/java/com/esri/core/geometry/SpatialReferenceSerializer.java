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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.esri.core.geometry.SpatialReference;

final class SpatialReferenceSerializer implements Serializable {
	private static final long serialVersionUID = 10000L;
	String wkt = null;
	int wkid = 0;

	Object readResolve() throws ObjectStreamException {
		SpatialReference sr = null;
		try {
			if (wkid > 0)
				sr = SpatialReference.create(wkid);
			else
				sr = SpatialReference.create(wkt);
		} catch (Exception ex) {
			throw new InvalidObjectException(
					"Cannot read spatial reference from stream");
		}
		return sr;
	}

	public void setSpatialReferenceByValue(SpatialReference sr)
			throws ObjectStreamException {
		try {
			if (sr.getID() > 0)
				wkid = sr.getID();
			else
				wkt = sr.getText();
		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot serialize this geometry");
		}
	}
}
