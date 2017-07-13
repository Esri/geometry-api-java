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

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import junit.framework.TestCase;
import org.junit.Test;

public class TestWkbImportOnPostgresST extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public static void testWkbImportOnPostgresST() throws Exception {
		try {
			Connection con = DriverManager.getConnection(
					"jdbc:postgresql://tb.esri.com:5432/new_gdb", "tb", "tb");
			OperatorFactoryLocal factory = OperatorFactoryLocal.getInstance();
			OperatorImportFromWkb operatorImport = (OperatorImportFromWkb) factory
					.getOperator(Operator.Type.ImportFromWkb);
			String stmt = "SELECT objectid,sde.st_asbinary(shape) FROM new_gdb.tb.interstates a WHERE objectid IN (2) AND (a.shape IS NULL OR sde.st_geometrytype(shape)::text IN ('ST_MULTILINESTRING','ST_LINESTRING'))  LIMIT 1000";
			PreparedStatement ps = con.prepareStatement(stmt);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				byte[] rsWkbGeom = rs.getBytes(2);
				@SuppressWarnings("unused")
				Geometry geomBorg = null;
				if (rsWkbGeom != null) {
					geomBorg = operatorImport.execute(0, Geometry.Type.Unknown,
							ByteBuffer.wrap(rsWkbGeom), null);
				}
			}

			ps.close();
			con.close();
		} catch (Exception e) {
		}
	}
}
