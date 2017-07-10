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

import static org.junit.Assert.*;
import junit.framework.TestCase;

import org.junit.Test;

public class TestWktParser extends TestCase {

	@Test
	public void testGeometryCollection() {
		String s = "   geometrycollection    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.geometrycollection);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);

		s = "   geometrycollection zm (  geometrycollection zm ( POINT ZM ( 5.  +1.e+0004 13 17) ), LineString  zm  emPty, MULTIpolyGON zM (((1 1 1 1))) ) ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.geometrycollection);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.geometrycollection);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.point);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.linestring);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.multipolygon);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);
	}

	@Test
	public void testMultiPolygon() {
		String s = "   MultIPolYgOn    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.multipolygon);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		s = "  MULTIpolyGON zM ( empty , (  empty, ( 5.  +1.e+0004  13 17, -.1e07  .006 13 17 ) , empty  , (4  003. 13 17, 02E-3 .3e2 13 17)  ) , empty, ( ( 5.  +1.e+0004  13 17, -.1e07  .006  13 17) , (4  003. 13 17 , 02E-3 .3e2 13 17)  ) )   ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.multipolygon);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		// Start first polygon
		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		// End of First polygon

		// Start Second Polygon
		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);
	}

	@Test
	public void testMultiLineString() {
		String s = "   MultiLineString    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.multilinestring);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		s = "  MultiLineString Z (  empty, ( 5.  +1.e+0004  13, -.1e07  .006 13 ) , empty  , (4  003. 13 , 02E-3 .3e2 13 ) , empty, ( 5.  +1.e+0004  13 , -.1e07  .006  13) , (4  003. 13 , 02E-3 .3e2 13 )  )   ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.multilinestring);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_z);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);
	}

	@Test
	public void testMultiPoint() {
		String s = "   MultipoInt    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.multipoint);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		s = "  Multipoint Z (  empty, ( 5.  +1.e+0004  13 ), (-.1e07  .006 13 ) , empty  , (4  003. 13 ), (02E-3 .3e2 13 )  )   ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken(); // bug here
		assertTrue(currentToken == WktParser.WktToken.multipoint);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_z);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);
	}

	@Test
	public void testPolygon() {
		String s = "   Polygon    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.polygon);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		s = "  polyGON M (  empty, ( 5.  +1.e+0004  13, -.1e07  .006 13 ) , empty  , (4  003. 13 , 02E-3 .3e2 13 ) , empty, ( 5.  +1.e+0004  13 , -.1e07  .006  13) , (4  003. 13 , 02E-3 .3e2 13 )  )   ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.polygon);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_m);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 4.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 3.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 2.0e-3);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.3e2);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);
	}

	@Test
	public void testLineString() {
		String s = "   LineString    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.linestring);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		s = "  LineString ( 5.  +1.e+0004 , -.1e07  .006 )   ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.linestring);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == -0.1e7);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 0.006);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);
	}

	@Test
	public void testPoint() {
		String s = "   PoInT    emPty ";
		WktParser wktParser = new WktParser();
		wktParser.resetParser(s);

		int currentToken;
		double value;

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.point);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.empty);

		s = "  POINT ZM ( 5.  +1.e+0004 13 17)   ";
		wktParser.resetParser(s);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.point);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.attribute_zm);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.left_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.x_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 5.0);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.y_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 1.0e4);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.z_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 13);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.m_literal);

		value = wktParser.currentNumericLiteral();
		assertTrue(value == 17);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.right_paren);

		currentToken = wktParser.nextToken();
		assertTrue(currentToken == WktParser.WktToken.not_available);

		s = "   PoInt ";
		wktParser.resetParser(s);

		wktParser.nextToken();
	}

}
