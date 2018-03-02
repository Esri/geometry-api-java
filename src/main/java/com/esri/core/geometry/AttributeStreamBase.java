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

import com.esri.core.geometry.VertexDescription.Persistence;
import java.nio.ByteBuffer;

/**
 * Base class for AttributeStream instances.
 */
abstract class AttributeStreamBase {

	protected boolean m_bLockedInSize;
	protected boolean m_bReadonly;

	public AttributeStreamBase() {
		m_bReadonly = false;
		m_bLockedInSize = false;
	}

	/**
	 * Returns the number of elements in the stream.
	 */
	public abstract int virtualSize();

	/**
	 * Returns an estimate of this object size in bytes.
	 *
	 * @return Returns an estimate of this object size in bytes.
	 */
	public abstract long estimateMemorySize();

	/**
	 * Returns the Persistence type of the stream.
	 */
	public abstract int getPersistence();

	/**
	 * Reads given element and returns it as double.
	 */
	public abstract double readAsDbl(int offset);

	/**
	 * Writes given element as double. The double is cast to the internal
	 * representation (truncated when int).
	 */
	public abstract void writeAsDbl(int offset, double d);

	/**
	 * Reads given element and returns it as int (truncated if double).
	 */
	public abstract int readAsInt(int offset);

	/**
	 * Writes given element as int. The int is cast to the internal
	 * representation.
	 */
	public abstract void writeAsInt(int offset, int d);

	/**
	 * Reads given element and returns it as int (truncated if double).
	 */
	public abstract long readAsInt64(int offset);

	/**
	 * Writes given element as int. The int is cast to the internal
	 * representation.
	 */
	public abstract void writeAsInt64(int offset, long d);

	/**
	 * Resizes the AttributeStream to the new size.
	 */
	public abstract void resize(int newSize, double defaultValue);

	/**
	 * Resizes the AttributeStream to the new size.
	 */
	public abstract void resize(int newSize);

	/**
	 * Resizes the AttributeStream to the new size. Does not change the capacity
	 * of the stream.
	 */
	public abstract void resizePreserveCapacity(int newSize);// java only method

	/**
	 * Same as resize(0)
	 */
	void clear(boolean bFreeMemory) {
		if (bFreeMemory)
			resize(0);
		else
			resizePreserveCapacity(0);
	}

	/**
	 * Adds a range of elements from the source stream. The streams must be of
	 * the same type.
	 * 
	 * @param src
	 *            The source stream to read elements from.
	 * @param srcStart
	 *            The index of the element in the source stream to start reading
	 *            from.
	 * @param count
	 *            The number of elements to add.
	 * @param bForward
	 *            True if adding the elements in order of the incoming source
	 *            stream. False if adding the elements in reverse.
	 * @param stride
	 *            The number of elements to be grouped together if adding the
	 *            elements in reverse.
	 */
	public abstract void addRange(AttributeStreamBase src, int srcStart,
			int count, boolean bForward, int stride);

	/**
	 * Inserts a range of elements from the source stream. The streams must be
	 * of the same type.
	 * 
	 * @param start
	 *            The index where to start the insert.
	 * @param src
	 *            The source stream to read elements from.
	 * @param srcStart
	 *            The index of the element in the source stream to start reading
	 *            from.
	 * @param count
	 *            The number of elements to read from the source stream.
	 * @param validSize
	 *            The number of valid elements in this stream.
	 */
	public abstract void insertRange(int start, AttributeStreamBase src,
			int srcStart, int count, boolean bForward, int stride, int validSize);

	/**
	 * Inserts a range of elements of the given value.
	 * 
	 * @param start
	 *            The index where to start the insert.
	 * @param value
	 *            The value to be inserted.
	 * @param count
	 *            The number of elements to be inserted.
	 * @param validSize
	 *            The number of valid elements in this stream.
	 */
	public abstract void insertRange(int start, double value, int count,
			int validSize);

	/**
	 * Inserts the attributes of a given semantics from a Point geometry.
	 * 
	 * @param start
	 *            The index where to start the insert.
	 * @param pt
	 *            The Point geometry holding the attributes to be inserted.
	 * @param semantics
	 *            The attribute semantics that are being inserted.
	 * @param validSize
	 *            The number of valid elements in this stream.
	 */
	public abstract void insertAttributes(int start, Point pt, int semantics,
			int validSize);

	/**
	 * Sets a range of values to given value.
	 * 
	 * @param value
	 *            The value to set stream elements to.
	 * @param start
	 *            The index of the element to start writing to.
	 * @param count
	 *            The number of elements to set.
	 */
	public abstract void setRange(double value, int start, int count);

	/**
	 * Adds a range of elements from the source byte buffer. This stream is
	 * resized automatically to accomodate required number of elements.
	 * 
	 * @param startElement
	 *            the index of the element in this stream to start setting
	 *            elements from.
	 * @param count
	 *            The number of AttributeStream elements to read.
	 * @param src
	 *            The source ByteBuffer to read elements from.
	 * @param sourceStart
	 *            The offset from the start of the ByteBuffer in bytes.
	 * @param bForward
	 *            When False, the source is written in reversed order.
	 * @param stride
	 *            Used for reversed writing only to indicate the unit of
	 *            writing. elements inside a stride are not reversed. Only the
	 *            strides are reversed.
	 */
	public abstract void writeRange(int startElement, int count,
			AttributeStreamBase src, int sourceStart, boolean bForward,
			int stride);

	/**
	 * Adds a range of elements from the source byte buffer. The stream is
	 * resized automatically to accomodate required number of elements.
	 * 
	 * @param startElement
	 *            the index of the element in this stream to start setting
	 *            elements from.
	 * @param count
	 *            The number of AttributeStream elements to read.
	 * @param src
	 *            The source ByteBuffer to read elements from.
	 * @param offsetBytes
	 *            The offset from the start of the ByteBuffer in bytes.
	 */
	public abstract void writeRange(int startElement, int count,
			ByteBuffer src, int offsetBytes, boolean bForward);

	/**
	 * Write a range of elements to the source byte buffer.
	 * 
	 * @param srcStart
	 *            The element index to start writing from.
	 * @param count
	 *            The number of AttributeStream elements to write.
	 * @param dst
	 *            The destination ByteBuffer. The buffer must be large enough or
	 *            it will throw.
	 * @param dstOffsetBytes
	 *            The offset in the destination ByteBuffer to start write
	 *            elements from.
	 */
	public abstract void readRange(int srcStart, int count, ByteBuffer dst,
			int dstOffsetBytes, boolean bForward);

	/**
	 * Erases a range from the buffer and defragments the result.
	 * 
	 * @param index
	 *            The index in this stream where the erasing starts.
	 * @param count
	 *            The number of elements to be erased.
	 * @param validSize
	 *            The number of valid elements in this stream.
	 */
	public abstract void eraseRange(int index, int count, int validSize);

	/**
	 * Reverses a range from the buffer.
	 * 
	 * @param index
	 *            The index in this stream where the reversing starts.
	 * @param count
	 *            The number of elements to be reversed.
	 * @param stride
	 *            The number of elements to be grouped together when doing the
	 *            reverse.
	 */
	public abstract void reverseRange(int index, int count, int stride);

	/**
	 * Creates a new attribute stream for storing bytes.
	 * 
	 * @param size
	 *            The number of elements in the stream.
	 */
	public static AttributeStreamBase createByteStream(int size) {
		AttributeStreamBase newStream = new AttributeStreamOfInt8(size);
		return newStream;
	}

	/**
	 * Creates a new attribute stream for storing bytes.
	 * 
	 * @param size
	 *            The number of elements in the stream.
	 * @param defaultValue
	 *            The default value to fill the stream with.
	 */
	public static AttributeStreamBase createByteStream(int size,
			byte defaultValue) {
		AttributeStreamBase newStream = new AttributeStreamOfInt8(size,
				defaultValue);
		return newStream;

	}

	/**
	 * Creates a new attribute stream for storing doubles.
	 * 
	 * @param size
	 *            The number of elements in the stream.
	 */
	public static AttributeStreamBase createDoubleStream(int size) {
		AttributeStreamBase newStream = new AttributeStreamOfDbl(size);
		return newStream;
	}

	/**
	 * Creates a new attribute stream for storing doubles.
	 * 
	 * @param size
	 *            The number of elements in the stream.
	 * @param defaultValue
	 *            The default value to fill the stream with.
	 */
	public static AttributeStreamBase createDoubleStream(int size,
			double defaultValue) {
		AttributeStreamBase newStream = new AttributeStreamOfDbl(size,
				defaultValue);
		return newStream;
	}

	/**
	 * Creats a copy of the stream that contains upto maxsize elements.
	 */
	public abstract AttributeStreamBase restrictedClone(int maxsize);

	/**
	 * Makes the stream to be readonly. Any operation that changes the content
	 * or size of the stream will throw.
	 */
	public void setReadonly() {
		m_bReadonly = true;
		m_bLockedInSize = true;
	}

	public boolean isReadonly() {
		return m_bReadonly;
	}

	/**
	 * Lock the size of the stream. Any operation that changes the size of the
	 * stream will throw.
	 */
	public void lockSize() {
		m_bLockedInSize = true;
	}

	public boolean isLockedSize() {
		return m_bLockedInSize;
	}

	/**
	 * Creates a new attribute stream of given persistence type and size.
	 * 
	 * @param persistence
	 *            The persistence type of the stream (see VertexDescription).
	 * @param size
	 *            The number of elements (floats, doubles, or 32 bit integers)
	 *            of the given type in the stream.
	 */
	public static AttributeStreamBase createAttributeStreamWithPersistence(
			int persistence, int size) {
		AttributeStreamBase newStream;
		switch (persistence) {
		case (Persistence.enumFloat):
			newStream = new AttributeStreamOfFloat(size);
			break;
		case (Persistence.enumDouble):
			newStream = new AttributeStreamOfDbl(size);
			break;
		case (Persistence.enumInt32):
			newStream = new AttributeStreamOfInt32(size);
			break;
		case (Persistence.enumInt64):
			newStream = new AttributeStreamOfInt64(size);
			break;
		case (Persistence.enumInt8):
			newStream = new AttributeStreamOfInt8(size);
			break;
		case (Persistence.enumInt16):
			newStream = new AttributeStreamOfInt16(size);
			break;
		default:
			throw new GeometryException("Internal Error");
		}
		return newStream;
	}

	/**
	 * Creates a new attribute stream of given persistence type and size.
	 * 
	 * @param persistence
	 *            The persistence type of the stream (see VertexDescription).
	 * @param size
	 *            The number of elements (floats, doubles, or 32 bit integers)
	 *            of the given type in the stream.
	 * @param defaultValue
	 *            The default value to fill the stream with.
	 */
	public static AttributeStreamBase createAttributeStreamWithPersistence(
			int persistence, int size, double defaultValue) {
		AttributeStreamBase newStream;
		switch (persistence) {
		case (Persistence.enumFloat):
			newStream = new AttributeStreamOfFloat(size, (float) defaultValue);
			break;
		case (Persistence.enumDouble):
			newStream = new AttributeStreamOfDbl(size, (double) defaultValue);
			break;
		case (Persistence.enumInt32):
			newStream = new AttributeStreamOfInt32(size, (int) defaultValue);
			break;
		case (Persistence.enumInt64):
			newStream = new AttributeStreamOfInt64(size, (long) defaultValue);
			break;
		case (Persistence.enumInt8):
			newStream = new AttributeStreamOfInt8(size, (byte) defaultValue);
			break;
		case (Persistence.enumInt16):
			newStream = new AttributeStreamOfInt16(size, (short) defaultValue);
			break;
		default:
			throw new GeometryException("Internal Error");
		}
		return newStream;
	}

	/**
	 * Creates a new attribute stream for the given semantics and vertex count.
	 * 
	 * @param semantics
	 *            The semantics of the attribute (see VertexDescription).
	 * @param vertexCount
	 *            The number of vertices in the geometry. The actual number of
	 *            elements in the stream is vertexCount * ncomponents.
	 */
	public static AttributeStreamBase createAttributeStreamWithSemantics(
			int semantics, int vertexCount) {
		int ncomps = VertexDescription.getComponentCount(semantics);
		int persistence = VertexDescription.getPersistence(semantics);
		return createAttributeStreamWithPersistence(persistence, vertexCount
				* ncomps, VertexDescription.getDefaultValue(semantics));
	}

	/**
	 * Creates a new attribute stream for storing vertex indices.
	 * 
	 * @param size
	 *            The number of elements in the stream.
	 */
	public static AttributeStreamBase createIndexStream(int size) {
		int persistence = Persistence.enumInt32;// VertexDescription.getPersistenceFromInt(NumberUtils::SizeOf((int)0));
		AttributeStreamBase newStream;
		switch (persistence) {
		case (Persistence.enumInt32):
			newStream = new AttributeStreamOfInt32(size);
			break;
		case (Persistence.enumInt64):
			newStream = new AttributeStreamOfInt64(size);
			break;
		default:
			throw new GeometryException("Internal Error");
		}
		return newStream;

	}

	/**
	 * Creates a new attribute stream for storing vertex indices.
	 * 
	 * @param size
	 *            The number of elements in the stream.
	 * @param defaultValue
	 *            The default value to fill the stream with.
	 */
	public static AttributeStreamBase createIndexStream(int size,
			int defaultValue) {
		int persistence = Persistence.enumInt32;// VertexDescription.getPersistenceFromInt(NumberUtils::SizeOf((int)0));
		AttributeStreamBase newStream;
		switch (persistence) {
		case (Persistence.enumInt32):
			newStream = new AttributeStreamOfInt32(size, (int) defaultValue);
			break;
		case (Persistence.enumInt64):
			newStream = new AttributeStreamOfInt64(size, (long) defaultValue);
			break;
		default:
			throw new GeometryException("Internal Error");
		}
		return newStream;
	}

	public abstract int calculateHashImpl(int hashCode, int start, int end);

	public abstract boolean equals(AttributeStreamBase other, int start, int end);

}
