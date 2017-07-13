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

public class TestAttributes extends TestCase{

	@Test
	public void testPoint() {
		Point pt = new Point();
		pt.setXY(100,  200);
		assertFalse(pt.hasAttribute(VertexDescription.Semantics.M));
		pt.addAttribute(VertexDescription.Semantics.M);
		assertTrue(pt.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(Double.isNaN(pt.getM()));
		pt.setAttribute(VertexDescription.Semantics.M, 0, 13);
		assertTrue(pt.getM() == 13);
		pt.addAttribute(VertexDescription.Semantics.Z);
		assertTrue(pt.getZ() == 0);
		assertTrue(pt.getM() == 13);
		pt.setAttribute(VertexDescription.Semantics.Z, 0, 11);
		assertTrue(pt.getZ() == 11);
		assertTrue(pt.getM() == 13);
		pt.addAttribute(VertexDescription.Semantics.ID);
		assertTrue(pt.getID() == 0);
		assertTrue(pt.getZ() == 11);
		assertTrue(pt.getM() == 13);
		pt.setAttribute(VertexDescription.Semantics.ID, 0, 1);
		assertTrue(pt.getID() == 1);
		assertTrue(pt.getZ() == 11);
		assertTrue(pt.getM() == 13);
		pt.dropAttribute(VertexDescription.Semantics.M);
		assertTrue(pt.getID() == 1);
		assertTrue(pt.getZ() == 11);
		assertFalse(pt.hasAttribute(VertexDescription.Semantics.M));
		
		Point pt1 = new Point();
		assertFalse(pt1.hasAttribute(VertexDescription.Semantics.M));
		assertFalse(pt1.hasAttribute(VertexDescription.Semantics.Z));
		assertFalse(pt1.hasAttribute(VertexDescription.Semantics.ID));
		pt1.mergeVertexDescription(pt.getDescription());
		assertFalse(pt1.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(pt1.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(pt1.hasAttribute(VertexDescription.Semantics.ID));
	}

	@Test
	public void testEnvelope() {
		Envelope env = new Envelope();
		env.setCoords(100,  200, 250, 300);
		assertFalse(env.hasAttribute(VertexDescription.Semantics.M));
		env.addAttribute(VertexDescription.Semantics.M);
		assertTrue(env.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(env.queryInterval(VertexDescription.Semantics.M, 0).isEmpty());
		env.setInterval(VertexDescription.Semantics.M, 0, 1, 2);
		assertTrue(env.queryInterval(VertexDescription.Semantics.M, 0).vmin == 1);
		assertTrue(env.queryInterval(VertexDescription.Semantics.M, 0).vmax == 2);

		assertFalse(env.hasAttribute(VertexDescription.Semantics.Z));
		env.addAttribute(VertexDescription.Semantics.Z);
		assertTrue(env.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmin == 0);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmax == 0);
		env.setInterval(VertexDescription.Semantics.Z, 0, 3, 4);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmin == 3);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmax == 4);
		

		assertFalse(env.hasAttribute(VertexDescription.Semantics.ID));
		env.addAttribute(VertexDescription.Semantics.ID);
		assertTrue(env.hasAttribute(VertexDescription.Semantics.ID));
		assertTrue(env.queryInterval(VertexDescription.Semantics.ID, 0).vmin == 0);
		assertTrue(env.queryInterval(VertexDescription.Semantics.ID, 0).vmax == 0);
		env.setInterval(VertexDescription.Semantics.ID, 0, 5, 6);
		assertTrue(env.queryInterval(VertexDescription.Semantics.ID, 0).vmin == 5);
		assertTrue(env.queryInterval(VertexDescription.Semantics.ID, 0).vmax == 6);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmin == 3);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmax == 4);
		assertTrue(env.queryInterval(VertexDescription.Semantics.M, 0).vmin == 1);
		assertTrue(env.queryInterval(VertexDescription.Semantics.M, 0).vmax == 2);
		
		env.dropAttribute(VertexDescription.Semantics.M);
		assertFalse(env.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(env.queryInterval(VertexDescription.Semantics.ID, 0).vmin == 5);
		assertTrue(env.queryInterval(VertexDescription.Semantics.ID, 0).vmax == 6);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmin == 3);
		assertTrue(env.queryInterval(VertexDescription.Semantics.Z, 0).vmax == 4);

		Envelope env1 = new Envelope();
		env.copyTo(env1);
		assertFalse(env1.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(env1.queryInterval(VertexDescription.Semantics.ID, 0).vmin == 5);
		assertTrue(env1.queryInterval(VertexDescription.Semantics.ID, 0).vmax == 6);
		assertTrue(env1.queryInterval(VertexDescription.Semantics.Z, 0).vmin == 3);
		assertTrue(env1.queryInterval(VertexDescription.Semantics.Z, 0).vmax == 4);
	}

	@Test
	public void testLine() {
		Line env = new Line();
		env.setStartXY(100,  200);
		env.setEndXY(250, 300);
		assertFalse(env.hasAttribute(VertexDescription.Semantics.M));
		env.addAttribute(VertexDescription.Semantics.M);
		assertTrue(env.hasAttribute(VertexDescription.Semantics.M));
		env.setStartAttribute(VertexDescription.Semantics.M, 0, 1);
		env.setEndAttribute(VertexDescription.Semantics.M, 0, 2);
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.M, 0) == 1);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.M, 0) == 2);

		assertFalse(env.hasAttribute(VertexDescription.Semantics.Z));
		env.addAttribute(VertexDescription.Semantics.Z);
		assertTrue(env.hasAttribute(VertexDescription.Semantics.Z));
		env.setStartAttribute(VertexDescription.Semantics.Z, 0, 3);
		env.setEndAttribute(VertexDescription.Semantics.Z, 0, 4);
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 3);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 4);
		

		assertFalse(env.hasAttribute(VertexDescription.Semantics.ID));
		env.addAttribute(VertexDescription.Semantics.ID);
		assertTrue(env.hasAttribute(VertexDescription.Semantics.ID));
		env.setStartAttribute(VertexDescription.Semantics.ID, 0, 5);
		env.setEndAttribute(VertexDescription.Semantics.ID, 0, 6);
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.ID, 0) == 5);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.ID, 0) == 6);

		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.M, 0) == 1);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.M, 0) == 2);
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 3);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 4);
		
		env.dropAttribute(VertexDescription.Semantics.M);
		assertFalse(env.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.ID, 0) == 5);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.ID, 0) == 6);
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 3);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 4);

		Line env1 = new Line();
		env.copyTo(env1);
		assertFalse(env1.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.ID, 0) == 5);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.ID, 0) == 6);
		assertTrue(env.getStartAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 3);
		assertTrue(env.getEndAttributeAsDbl(VertexDescription.Semantics.Z, 0) == 4);
	}
	
	@Test
	public void testMultiPoint() {
		MultiPoint mp = new MultiPoint();
		mp.add(new Point(100, 200));
		mp.add(new Point(101, 201));
		mp.add(new Point(102, 202));
		assertFalse(mp.hasAttribute(VertexDescription.Semantics.M));
		mp.addAttribute(VertexDescription.Semantics.M);
		assertTrue(mp.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(Double.isNaN(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)));
		assertTrue(Double.isNaN(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)));
		assertTrue(Double.isNaN(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)));
		mp.setAttribute(VertexDescription.Semantics.M, 0, 0, 1);
		mp.setAttribute(VertexDescription.Semantics.M, 1, 0, 2);
		mp.setAttribute(VertexDescription.Semantics.M, 2, 0, 3);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)==1);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)==2);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)==3);

		assertFalse(mp.hasAttribute(VertexDescription.Semantics.Z));
		mp.addAttribute(VertexDescription.Semantics.Z);
		assertTrue(mp.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==0);
		mp.setAttribute(VertexDescription.Semantics.Z, 0, 0, 11);
		mp.setAttribute(VertexDescription.Semantics.Z, 1, 0, 21);
		mp.setAttribute(VertexDescription.Semantics.Z, 2, 0, 31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)==1);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)==2);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)==3);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);

		assertFalse(mp.hasAttribute(VertexDescription.Semantics.ID));
		mp.addAttribute(VertexDescription.Semantics.ID);
		assertTrue(mp.hasAttribute(VertexDescription.Semantics.ID));
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==0);
		mp.setAttribute(VertexDescription.Semantics.ID, 0, 0, -11);
		mp.setAttribute(VertexDescription.Semantics.ID, 1, 0, -21);
		mp.setAttribute(VertexDescription.Semantics.ID, 2, 0, -31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)==1);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)==2);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)==3);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==-11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==-21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==-31);
		
		mp.dropAttribute(VertexDescription.Semantics.M);
		assertFalse(mp.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==-11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==-21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==-31);
		
		MultiPoint mp1 = new MultiPoint();
		mp.copyTo(mp1);
		assertFalse(mp1.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==-11);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==-21);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==-31);
		
		mp1.dropAllAttributes();
		mp1.mergeVertexDescription(mp.getDescription());
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==0);
		assertTrue(Double.isNaN(mp1.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)));
		assertTrue(Double.isNaN(mp1.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)));
		assertTrue(Double.isNaN(mp1.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)));
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==0);
	
	}

	@Test
	public void testPolygon() {
		Polygon mp = new Polygon();
		mp.startPath(new Point(100, 200));
		mp.lineTo(new Point(101, 201));
		mp.lineTo(new Point(102, 202));
		assertFalse(mp.hasAttribute(VertexDescription.Semantics.M));
		mp.addAttribute(VertexDescription.Semantics.M);
		assertTrue(mp.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(Double.isNaN(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)));
		assertTrue(Double.isNaN(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)));
		assertTrue(Double.isNaN(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)));
		mp.setAttribute(VertexDescription.Semantics.M, 0, 0, 1);
		mp.setAttribute(VertexDescription.Semantics.M, 1, 0, 2);
		mp.setAttribute(VertexDescription.Semantics.M, 2, 0, 3);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)==1);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)==2);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)==3);

		assertFalse(mp.hasAttribute(VertexDescription.Semantics.Z));
		mp.addAttribute(VertexDescription.Semantics.Z);
		assertTrue(mp.hasAttribute(VertexDescription.Semantics.Z));
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==0);
		mp.setAttribute(VertexDescription.Semantics.Z, 0, 0, 11);
		mp.setAttribute(VertexDescription.Semantics.Z, 1, 0, 21);
		mp.setAttribute(VertexDescription.Semantics.Z, 2, 0, 31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)==1);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)==2);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)==3);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);

		assertFalse(mp.hasAttribute(VertexDescription.Semantics.ID));
		mp.addAttribute(VertexDescription.Semantics.ID);
		assertTrue(mp.hasAttribute(VertexDescription.Semantics.ID));
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==0);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==0);
		mp.setAttribute(VertexDescription.Semantics.ID, 0, 0, -11);
		mp.setAttribute(VertexDescription.Semantics.ID, 1, 0, -21);
		mp.setAttribute(VertexDescription.Semantics.ID, 2, 0, -31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)==1);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)==2);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)==3);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==-11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==-21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==-31);
		
		mp.dropAttribute(VertexDescription.Semantics.M);
		assertFalse(mp.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==-11);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==-21);
		assertTrue(mp.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==-31);
		
		Polygon mp1 = new Polygon();
		mp.copyTo(mp1);
		assertFalse(mp1.hasAttribute(VertexDescription.Semantics.M));
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==11);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==21);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==31);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==-11);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==-21);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==-31);

		mp1.dropAllAttributes();
		mp1.mergeVertexDescription(mp.getDescription());
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 0, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 1, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.Z, 2, 0)==0);
		assertTrue(Double.isNaN(mp1.getAttributeAsDbl(VertexDescription.Semantics.M, 0, 0)));
		assertTrue(Double.isNaN(mp1.getAttributeAsDbl(VertexDescription.Semantics.M, 1, 0)));
		assertTrue(Double.isNaN(mp1.getAttributeAsDbl(VertexDescription.Semantics.M, 2, 0)));
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 0, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 1, 0)==0);
		assertTrue(mp1.getAttributeAsDbl(VertexDescription.Semantics.ID, 2, 0)==0);
		
	}
	
}
