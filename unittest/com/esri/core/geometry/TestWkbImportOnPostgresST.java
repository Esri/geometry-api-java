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
