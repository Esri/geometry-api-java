/*
 Copyright 1995-2017 Esri

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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Test;

public class TestJSonGeometry extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testGetSpatialReferenceFor4326() {
		String completeStr = "GEOGCS[\"GCS_Sphere\",DATUM[\"D_Sphere\","
				+ "SPHEROID[\"Sphere\",6371000.0,0.0]],PRIMEM[\"Greenwich\",0.0],"
				+ "UNIT[\"Degree\",0.0174532925199433]]";

		// 4326 GCS_WGS_1984
		SpatialReference sr = SpatialReference.create(completeStr);
		assertNotNull(sr);
	}

}

final class HashMapClassForTesting {
	static Map<Integer, String> SR_WKI_WKTs = new HashMap<Integer, String>() {
		/**
		 * added to get rid of warning
		 */
		private static final long serialVersionUID = 8630934425353750539L;

		{
			put(4035,
					"GEOGCS[\"GCS_Sphere\",DATUM[\"D_Sphere\","
							+ "SPHEROID[\"Sphere\",6371000.0,0.0]],PRIMEM[\"Greenwich\",0.0],"
							+ "UNIT[\"Degree\",0.0174532925199433]]");
		}
	};
}
