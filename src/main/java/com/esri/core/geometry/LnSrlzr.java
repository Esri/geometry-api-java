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

//This is a writeReplace class for Lin
public class LnSrlzr implements Serializable {
	private static final long serialVersionUID = 1L;
	double[] attribs;
	int descriptionBitMask;

	public Object readResolve() throws ObjectStreamException {
		Line ln = null;
		if (descriptionBitMask == -1)
			return null;
		
		try {
			VertexDescription vd = VertexDescriptionDesignerImpl
					.getVertexDescription(descriptionBitMask);
			ln = new Line(vd);
			if (attribs != null) {
				ln.setStartXY(attribs[0], attribs[1]);
				ln.setEndXY(attribs[2], attribs[3]);
				int index = 4;
				for (int i = 1, n = vd.getAttributeCount(); i < n; i++) {
					int semantics = vd.getSemantics(i);
					int comps = VertexDescription.getComponentCount(semantics);
					for (int ord = 0; ord < comps; ord++) {
						ln.setStartAttribute(semantics, ord, attribs[index++]);
						ln.setEndAttribute(semantics, ord, attribs[index++]);
					}
				}
			}
		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot read geometry from stream");
		}

		return ln;
	}

	public void setGeometryByValue(Line ln) throws ObjectStreamException {
		try {
			attribs = null;
			if (ln == null) {
				descriptionBitMask = -1;
			}

			VertexDescription vd = ln.getDescription();
			descriptionBitMask = vd.m_semanticsBitArray;

			attribs = new double[vd.getTotalComponentCount() * 2];
			attribs[0] = ln.getStartX();
			attribs[1] = ln.getStartY();
			attribs[2] = ln.getEndX();
			attribs[3] = ln.getEndY();
			int index = 4;
			for (int i = 1, n = vd.getAttributeCount(); i < n; i++) {
				int semantics = vd.getSemantics(i);
				int comps = VertexDescription.getComponentCount(semantics);
				for (int ord = 0; ord < comps; ord++) {
					attribs[index++] = ln.getStartAttributeAsDbl(semantics, ord);
					attribs[index++] = ln.getEndAttributeAsDbl(semantics, ord);
				}
			}
		} catch (Exception ex) {
			throw new InvalidObjectException("Cannot serialize this geometry");
		}
	}
}
