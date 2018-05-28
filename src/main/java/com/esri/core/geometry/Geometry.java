/*
 Copyright 1995-2018 Esri

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

import java.io.ObjectStreamException;
import java.io.Serializable;

import static com.esri.core.geometry.SizeOf.sizeOfDoubleArray;

/**
 * Common properties and methods shared by all geometric objects. Geometries are
 * objects that define a spatial location and and associated geometric shape.
 */
public abstract class Geometry implements Serializable {
	VertexDescription m_description;
	volatile int m_touchFlag;

	Geometry() {
		m_description = null;
		m_touchFlag = 0;
	}

	/**
	 * Geometry types
	 */
	static public interface GeometryType {
		public final static int Unknown = 0;
		public final static int Point = 1 + 0x20; // points
		public final static int Line = 2 + 0x40 + 0x100; // lines, segment
		final static int Bezier = 3 + 0x40 + 0x100; // lines, segment
		final static int EllipticArc = 4 + 0x40 + 0x100; // lines, segment
		public final static int Envelope = 5 + 0x40 + 0x80; // lines, areas
		public final static int MultiPoint = 6 + 0x20 + 0x200; // points,
		// multivertex
		public final static int Polyline = 7 + 0x40 + 0x200 + 0x400; // lines,
																		// multivertex,
		// multipath
		public final static int Polygon = 8 + 0x40 + 0x80 + 0x200 + 0x400;
	}

	/**
	 * The type of this geometry.
	 */
	static public enum Type {
		/**
		 * Used to indicate that the geometry type is not known before executing
		 * a method.
		 */
		Unknown(GeometryType.Unknown),
		/**
		 * The value representing a point as geometry type.
		 */

		Point(GeometryType.Point),
		/**
		 * The value representing a line as geometry type.
		 */

		Line(GeometryType.Line),
		/**
		 * The value representing an envelope as geometry type.
		 */

		Envelope(GeometryType.Envelope),
		/**
		 * The value representing a multipoint as geometry type.
		 */

		MultiPoint(GeometryType.MultiPoint),
		/**
		 * The value representing a polyline as geometry type.
		 */

		Polyline(GeometryType.Polyline),
		/**
		 * The value representing a polygon as geometry type.
		 */

		Polygon(GeometryType.Polygon);

		private int enumValue;

		/**
		 * Returns the integer representation of the enumeration value.
		 */
		public int value() {
			return enumValue;
		}

		Type(int val) {
			enumValue = val;
		}
		
		static public Geometry.Type intToType(int geometryType)
		{
			Geometry.Type[] v = Geometry.Type.values();
            for(int i = 0; i < v.length; i++)
            {
                if(v[i].value() == geometryType)
                    return v[i];
            }
            
            throw new IllegalArgumentException();		
        }
	}

	/**
	 * Returns the geometry type.
	 * 
	 * @return Returns the geometry type.
	 */
	public abstract Geometry.Type getType();
	
	/**
	 * Returns the topological dimension of the geometry object based on the
	 * geometry's type.
	 * <p>
	 * Returns 0 for point and multipoint.
	 * <p>
	 * Returns 1 for lines and polylines.
	 * <p>
	 * Returns 2 for polygons and envelopes
	 * <p>
	 * Returns 3 for objects with volume
	 * 
	 * @return Returns the integer value of the dimension of geometry.
	 */
	public abstract int getDimension();

	/**
	 * Returns an estimate of this object size in bytes.
	 * <p>
	 * This estimate doesn't include the size of the {@link VertexDescription} object
	 * because instances of {@link VertexDescription} are shared among
	 * geometry objects.
	 * 
	 * @return Returns an estimate of this object size in bytes.
	 */
	public abstract long estimateMemorySize();

	protected static long estimateMemorySize(double[] attributes)
	{
		return attributes != null ? sizeOfDoubleArray(attributes.length) : 0;
	}

	/**
	 * Returns the VertexDescription of this geometry.
	 * @return VertexDescription
	 */
	public VertexDescription getDescription() {
		return m_description;
	}

	/**
	 * Assigns the new VertexDescription by adding or dropping attributes. The
	 * Geometry will have the src description as a result.
	 * @param src VertexDescription to assign.
	 */
	public void assignVertexDescription(VertexDescription src) {
		_touch();
		if (src == m_description)
			return;

		_assignVertexDescriptionImpl(src);
	}
	
	protected abstract void _assignVertexDescriptionImpl(VertexDescription src);

	/**
	 * Merges the new VertexDescription by adding missing attributes from the
	 * src. The Geometry will have a union of the current and the src
	 * descriptions.
	 * @param src VertexDescription to merge.
	 */
	public void mergeVertexDescription(VertexDescription src) {
		_touch();
		if (src == m_description)
			return;

		// check if we need to do anything (if the src has same attributes)
		VertexDescription newdescription = VertexDescriptionDesignerImpl.getMergedVertexDescription(m_description, src);
		if (newdescription == m_description)
			return;
		
		_assignVertexDescriptionImpl(newdescription);
	}

	/**
	 * A shortcut for getDescription().hasAttribute()
	 * @param semantics The VertexDescription.Semantics to check.
	 * @return Return true if the attribute is present.
	 */
	public boolean hasAttribute(int semantics) {
		return getDescription().hasAttribute(semantics);
	}

	/**
	 * Adds a new attribute to the Geometry.
	 * 
	 * @param semantics The VertexDescription.Semantics to add.
	 */
	public void addAttribute(int semantics) {
		_touch();
		if (m_description.hasAttribute(semantics))
			return;
		
		VertexDescription newvd = VertexDescriptionDesignerImpl.getMergedVertexDescription(m_description, semantics);
		_assignVertexDescriptionImpl(newvd);
	}

	/**
	 * Drops an attribute from the Geometry. Dropping the attribute is
	 * equivalent to setting the attribute to the default value for each vertex,
	 * However, it is faster and the result Geometry has smaller memory
	 * footprint and smaller size when persisted.
	 * @param semantics The VertexDescription.Semantics to drop.
	 */
	public void dropAttribute(int semantics) {
		_touch();
		if (!m_description.hasAttribute(semantics))
			return;

		VertexDescription newvd = VertexDescriptionDesignerImpl.removeSemanticsFromVertexDescription(m_description, semantics);
		_assignVertexDescriptionImpl(newvd);
	}

	/**
	 * Drops all attributes from the Geometry with exception of POSITON.
	 */
	public void dropAllAttributes() {
		assignVertexDescription(VertexDescriptionDesignerImpl
				.getDefaultDescriptor2D());
	}

	/**
	 * Returns the min and max attribute values at the ordinate of the Geometry.
	 * @param semantics The semantics of the interval.
	 * @param ordinate The ordinate of the interval.
	 * @return The interval.
	 */
	public abstract Envelope1D queryInterval(int semantics, int ordinate);

	/**
	 * Returns the axis aligned bounding box of the geometry.
	 * 
	 * @param env
	 *            The envelope to return the result in.
	 */
	public abstract void queryEnvelope(Envelope env);

	/**
	 * Returns tight bbox of the Geometry in X, Y plane.
	 * @param env
	 *            The envelope to return the result in.
	 */
	public abstract void queryEnvelope2D(Envelope2D env);

	/**
	 * Returns tight bbox of the Geometry in 3D.
	 * @param env
	 *            The envelope to return the result in.
	 */
	abstract void queryEnvelope3D(Envelope3D env);

	/**
	 * Returns the conservative bbox of the Geometry in X, Y plane. This is a
	 * faster method than QueryEnvelope2D. However, the bbox could be larger
	 * than the tight box.
	 * @param env
	 *            The envelope to return the result in.
	 */
	public void queryLooseEnvelope2D(Envelope2D env) {
		queryEnvelope2D(env);
	}

	/**
	 * Returns tight conservative box of the Geometry in 3D. This is a faster
	 * method than the QueryEnvelope3D. However, the box could be larger than
	 * the tight box.
	 * @param env
	 *            The envelope to return the result in.
	 */
	void queryLooseEnvelope3D(Envelope3D env) {
		queryEnvelope3D(env);
	}

	/**
	 * IsEmpty returns TRUE when the Geometry object does not contain geometric
	 * information beyond its original initialization state.
	 * 
	 * @return boolean Returns TRUE if this geometry is empty.
	 */
	public abstract boolean isEmpty();

	/**
	 * Returns the geometry to its original initialization state by releasing
	 * all data referenced by the geometry.
	 */
	public abstract void setEmpty();

	/**
	 * Applies 2D affine transformation in XY plane.
	 * 
	 * @param transform
	 *            The affine transformation to be applied to this geometry.
	 */
	public abstract void applyTransformation(Transformation2D transform);

	/**
	 * Applies 3D affine transformation. Adds Z attribute if it is missing.
	 * 
	 * @param transform
	 *            The affine transformation to be applied to this geometry.
	 */
	abstract void applyTransformation(Transformation3D transform);

	/**
	 * Creates an instance of an empty geometry of the same type.
	 * @return The new instance.
	 */
	public abstract Geometry createInstance();

	/**
	 * Copies this geometry to another geometry of the same type. The result
	 * geometry is an exact copy.
	 * @param dst The geometry instance to copy to.
	 * @exception GeometryException
	 *                invalid_argument if the geometry is of different type.
	 */
	public abstract void copyTo(Geometry dst);

	/**
	 * Calculates the area of the geometry. If the spatial reference is a
	 * Geographic Coordinate System (WGS84) then the 2D area calculation is
	 * defined in angular units.
	 * 
	 * @return A double value representing the 2D area of the geometry.
	 */
	public double calculateArea2D() {
		return 0;
	}

	/**
	 * Calculates the length of the geometry. If the spatial reference is a
	 * Geographic Coordinate System (a system where coordinates are defined
	 * using angular units such as longitude and latitude) then the 2D distance
	 * calculation is returned in angular units. In cases where length must be
	 * calculated on a Geographic Coordinate System consider the using the
	 * geodeticLength method on the {@link GeometryEngine}
	 * 
	 * @return A double value representing the 2D length of the geometry.
	 */
	public double calculateLength2D() {
		return 0;
	}

	protected Object _getImpl() {
		throw new RuntimeException("invalid call");
	}

	/**
	 * Adds the Z attribute to this Geometry
	 */
	void addZ() {
		addAttribute(VertexDescription.Semantics.Z);
	}

	/**
	 * Returns true if this Geometry has the Z attribute
	 * 
	 * @return true if this Geometry has the Z attribute
	 */
	public boolean hasZ() {
		return hasAttribute(VertexDescription.Semantics.Z);
	}

	/**
	 * Adds the M attribute to this Geometry
	 */
	public void addM() {
		addAttribute(VertexDescription.Semantics.M);
	}

	/**
	 * Returns true if this Geometry has an M attribute
	 * 
	 * @return true if this Geometry has an M attribute
	 */
	public boolean hasM() {
		return hasAttribute(VertexDescription.Semantics.M);
	}

	/**
	 * Adds the ID attribute to this Geometry
	 */
	public void addID() {
		addAttribute(VertexDescription.Semantics.ID);
	}

	/**
	 * Returns true if this Geometry has an ID attribute
	 * 
	 * @return true if this Geometry has an ID attribute
	 */
	public boolean hasID() {
		return hasAttribute(VertexDescription.Semantics.ID);
	}

	/**
	 * Returns this geometry's dimension.
	 * <p>
	 * Returns 0 for point and multipoint.
	 * <p>
	 * Returns 1 for lines and polylines.
	 * <p>
	 * Returns 2 for polygons and envelopes
	 * <p>
	 * Returns 3 for objects with volume
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return The integer dimension of this geometry.
	 */
	public static int getDimensionFromType(int type) {
		return (((type & (0x40 | 0x80)) >> 6) + 1) >> 1;
	}

	/**
	 * Indicates if the integer value of the enumeration is a point type
	 * (dimension 0).
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return TRUE if the geometry is a point (a Point or a Multipoint).
	 */
	public static boolean isPoint(int type) {
		return (type & 0x20) != 0;
	}

	/**
	 * Indicates if the integer value of the enumeration is linear (dimension
	 * 1).
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return TRUE if the geometry is a line.
	 */
	public static boolean isLinear(int type) {
		return (type & 0x40) != 0;
	}

	/**
	 * Indicates if the integer value of the enumeration is an area (dimension
	 * 2).
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return TRUE if the geometry is a polygon.
	 */
	public static boolean isArea(int type) {
		return (type & 0x80) != 0;
	}

	/**
	 * Indicates if the integer value of the enumeration is a segment.
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return TRUE if the geometry is a segment.
	 */
	public static boolean isSegment(int type) {
		return (type & 0x100) != 0;
	}

	/**
	 * Indicates if the integer value of the enumeration is a multivertex (ie,
	 * multipoint, line, or area).
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return TRUE if the geometry has multiple vertices.
	 */
	public static boolean isMultiVertex(int type) {
		return (type & 0x200) != 0;
	}

	/**
	 * Indicates if the integer value of the enumeration is a multipath (ie,
	 * line or area).
	 * 
	 * @param type
	 *            The integer value from geometry enumeration. You can use the
	 *            method {@link Type#value()} to get at the integer value.
	 * @return TRUE if the geometry is a multipath.
	 */
	public static boolean isMultiPath(int type) {
		return (type & 0x400) != 0;
	}

	/**
	 * Creates a copy of the geometry.
	 * 
	 * @return Returns a copy of this geometry.
	 */
	public Geometry copy() {
		Geometry geom = createInstance();
		this.copyTo(geom);
		return geom;
	}

	/**
	 * Returns boundary of this geometry.
	 *
	 * Polygon and Envelope boundary is a Polyline. For Polyline and Line, the
	 * boundary is a Multi_point consisting of path end points. For Multi_point and
	 * Point null is returned.
	 * @return The boundary geometry.
	 */
	public abstract Geometry getBoundary();

	/**
	 * Replaces NaNs in the attribute with the given value.
	 * If the geometry is not empty, it adds the attribute if geometry does not have it yet, and replaces the values.
	 * If the geometry is empty, it adds the attribute and does not set any values.
	 * @param semantics The semantics for which to replace the NaNs.
	 * @param value The value to replace NaNs with. 
	 */
	public abstract void replaceNaNs(int semantics, double value);

	static Geometry _clone(Geometry src) {
		Geometry geom = src.createInstance();
		src.copyTo(geom);
		return geom;
	}

	/**
	 * The stateFlag value changes with changes applied to this geometry. This
	 * allows the user to keep track of the geometry's state.
	 * 
	 * @return The state of the geometry.
	 */
	public int getStateFlag() {
		m_touchFlag &= 0x7FFFFFFF;
		return m_touchFlag;
	}

	// Called whenever geometry changes
	synchronized void _touch() {
		if (m_touchFlag >= 0) {
			m_touchFlag += 0x80000001;
		}
	}

	/**
	 * Describes the degree of acceleration of the geometry.
	 * Acceleration usually builds a raster and a quadtree.
	 */
	static public enum GeometryAccelerationDegree {
		/**
		 * mild acceleration, takes least amount of memory. (64x64x2 bit raster)
		 */
		enumMild,
		/**
		 * medium acceleration, takes more memory and takes more time to accelerate, but may work faster.
		 * (256x256x2 bit raster and a quad tree for segments)
		 */
		enumMedium,
		/**
		*high acceleration, takes even more memory and may take
		*longest time to accelerate, but may work faster than the
		*other two.
		*(1024x1024x2 bit raster and a quad tree for segments)
		*/
		enumHot
	}

	Object writeReplace() throws ObjectStreamException {
		Type gt = getType();
		if (gt == Geometry.Type.Point)
		{
			PtSrlzr pt = new PtSrlzr();
			pt.setGeometryByValue((Point)this);
			return pt;
		}
		else if (gt == Geometry.Type.Envelope)
		{
			EnvSrlzr e = new EnvSrlzr();
			e.setGeometryByValue((Envelope)this);
			return e;
		}
		else if (gt == Geometry.Type.Line)
		{
			LnSrlzr ln = new LnSrlzr();
			ln.setGeometryByValue((Line)this);
			return ln;
		}
		
		GenericGeometrySerializer geomSerializer = new GenericGeometrySerializer();
		geomSerializer.setGeometryByValue(this);
		return geomSerializer;
	}

	/**
	 * The output of this method can be only used for debugging. It is subject to change without notice. 
	 */
	@Override
	public String toString() {
		String snippet = OperatorExportToJson.local().execute(null, this);
		if (snippet.length() > 200) { 
			return snippet.substring(0, 197) + "... ("+snippet.length()+" characters)"; 
		}
		else {
			return snippet;
		}
	}

	/**
	 * Returns count of geometry vertices: 1 for Point, 4 for Envelope,
	 * get_point_count for MultiVertexGeometry types, 2 for segment types Returns 0
	 * if geometry is empty.
	 * @param geom The geometry to get the vertex count for.
	 * @return The vertex count.
	 */
	public static int vertex_count(Geometry geom) {
		Geometry.Type gt = geom.getType();
		if (Geometry.isMultiVertex(gt.value()))
			return ((MultiVertexGeometry) geom).getPointCount();

		if (geom.isEmpty())
			return 0;

		if (gt == Geometry.Type.Envelope)
			return 4;

		if (gt == Geometry.Type.Point)
			return 1;

		if (Geometry.isSegment(gt.value()))
			return 2;

		throw new GeometryException("missing type");
	}

}
