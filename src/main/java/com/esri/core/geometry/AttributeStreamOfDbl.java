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

import com.esri.core.geometry.VertexDescription.Persistence;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esri.core.geometry.SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_DBL;
import static com.esri.core.geometry.SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT32;
import static com.esri.core.geometry.SizeOf.sizeOfDoubleArray;

final class AttributeStreamOfDbl extends AttributeStreamBase {

	private double[] m_buffer = null;
	private int m_size;

	public int size() {
		return m_size;
	}

	public void reserve(int reserve)
	{
		if (reserve <= 0)
			return;
		if (m_buffer == null)
			m_buffer = new double[reserve];
		else {
			if (reserve <= m_buffer.length)
				return;
			double[] buf = new double[reserve];
			System.arraycopy(m_buffer, 0, buf, 0, m_size);
			m_buffer = buf;
		}

	}

	public int capacity() {
		return m_buffer != null ? m_buffer.length : 0;
	}
	
	public AttributeStreamOfDbl(int size) {
		int sz = size;
		if (sz < 2)
			sz = 2;
		m_buffer = new double[sz];
		m_size = size;
	}

	public AttributeStreamOfDbl(int size, double defaultValue) {
		int sz = size;
		if (sz < 2)
			sz = 2;
		m_buffer = new double[sz];
		m_size = size;
		Arrays.fill(m_buffer, 0, size, defaultValue);
	}

	public AttributeStreamOfDbl(AttributeStreamOfDbl other) {
		m_buffer = other.m_buffer.clone();
		m_size = other.m_size;
	}

	public AttributeStreamOfDbl(AttributeStreamOfDbl other, int maxSize) {
		m_size = other.size();
		if (m_size > maxSize)
			m_size = maxSize;
		int sz = m_size;
		if (sz < 2)
			sz = 2;
		m_buffer = new double[sz];
		System.arraycopy(other.m_buffer, 0, m_buffer, 0, m_size);
	}

	/**
	 * Reads a value from the buffer at given offset.
	 *
	 * @param offset
	 *            is the element number in the stream.
	 */
	public double read(int offset) {
		return m_buffer[offset];
	}

	public double get(int offset) {
		return m_buffer[offset];
	}

	/**
	 * Overwrites given element with new value.
	 *
	 * @param offset
	 *            is the element number in the stream.
	 * @param value
	 *            is the value to write.
	 */
	public void write(int offset, double value) {
		if (m_bReadonly) {
			throw new RuntimeException("invalid_call");
		}
		m_buffer[offset] = value;
	}

	public void set(int offset, double value) {
		if (m_bReadonly) {
			throw new RuntimeException("invalid_call");
		}
		m_buffer[offset] = value;
	}

	/**
	 * Reads a value from the buffer at given offset.
	 *
	 * @param offset
	 *            is the element number in the stream.
	 */
	public void read(int offset, Point2D outPoint) {
		outPoint.x = m_buffer[offset];
		outPoint.y = m_buffer[offset + 1];
	}

	/**
	 * Overwrites given element with new value.
	 *
	 * @param offset
	 *            is the element number in the stream.
	 * @param value
	 *            is the value to write.
	 */
	void write(int offset, Point2D point) {
		if (m_bReadonly) {
			throw new RuntimeException("invalid_call");
		}
		m_buffer[offset] = point.x;
		m_buffer[offset + 1] = point.y;
	}

	/**
	 * Adds a new value at the end of the stream.
	 */
	public void add(double v) {
		resize(m_size + 1);
		m_buffer[m_size - 1] = v;
	}

	@Override
	public AttributeStreamBase restrictedClone(int maxsize) {
		AttributeStreamOfDbl clone = new AttributeStreamOfDbl(this, maxsize);
		return clone;
	}

	@Override
	public int virtualSize() {
		return size();
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_ATTRIBUTE_STREAM_OF_DBL + sizeOfDoubleArray(m_buffer.length);
	}

	// @Override
	// public void addRange(AttributeStreamBase src, int srcStartIndex, int
	// count) {
	// if ((src == this) || !(src instanceof AttributeStreamOfDbl))
	// throw new IllegalArgumentException();
	//
	// AttributeStreamOfDbl as = (AttributeStreamOfDbl) src;
	//
	// int len = as.size();
	// int oldSize = m_size;
	// resize(oldSize + len, 0);
	// for (int i = 0; i < len; i++) {
	// m_buffer[oldSize + i] = as.read(i);
	// }
	// }

	@Override
	public int getPersistence() {
		return Persistence.enumDouble;
	}

	@Override
	public double readAsDbl(int offset) {
		return read(offset);
	}

	@Override
	public int readAsInt(int offset) {
		return (int) read(offset);
	}

	@Override
	public long readAsInt64(int offset) {
		return (long) read(offset);
	}

	@Override
	public void resize(int newSize) {
		if (m_bLockedInSize)
			throw new GeometryException(
					"invalid call. Attribute Stream is locked and cannot be resized.");

		if (newSize <= m_size) {
			if ((newSize * 5) / 4 < m_buffer.length) {// decrease when the 25%
				// margin is exceeded
				double[] newBuffer = new double[newSize];
				System.arraycopy(m_buffer, 0, newBuffer, 0, newSize);
				m_buffer = newBuffer;
			}
			m_size = newSize;
		} else {
			if (newSize > m_buffer.length) {
				int sz = (newSize < 64) ? Math.max(newSize * 2, 4)
						: (newSize * 5) / 4;
				double[] newBuffer = new double[sz];
				System.arraycopy(m_buffer, 0, newBuffer, 0, m_size);
				m_buffer = newBuffer;
			}

			m_size = newSize;
		}
	}

	@Override
	public void resizePreserveCapacity(int newSize)// java only method
	{
		if (m_buffer == null || newSize > m_buffer.length)
			resize(newSize);
		if (m_bLockedInSize)
			throw new GeometryException(
					"invalid call. Attribute Stream is locked and cannot be resized.");

		m_size = newSize;
	}

	@Override
	public void resize(int newSize, double defaultValue) {
		if (m_bLockedInSize)
			throw new GeometryException(
					"invalid call. Attribute Stream is locked and cannot be resized.");
		if (newSize <= m_size) {
			if ((newSize * 5) / 4 < m_buffer.length) {// decrease when the 25%
				// margin is exceeded
				double[] newBuffer = new double[newSize];
				System.arraycopy(m_buffer, 0, newBuffer, 0, newSize);
				m_buffer = newBuffer;
			}
			m_size = newSize;
		} else {
			if (newSize > m_buffer.length) {
				int sz = (newSize < 64) ? Math.max(newSize * 2, 4)
						: (newSize * 5) / 4;
				double[] newBuffer = new double[sz];
				System.arraycopy(m_buffer, 0, newBuffer, 0, m_size);
				m_buffer = newBuffer;
			}

			Arrays.fill(m_buffer, m_size, newSize, defaultValue);

			m_size = newSize;
		}
	}

	@Override
	public void writeAsDbl(int offset, double d) {
		write(offset, d);
	}

	@Override
	public void writeAsInt64(int offset, long d) {
		write(offset, (double) d);
	}

	@Override
	public void writeAsInt(int offset, int d) {
		write(offset, (double) d);
	}

	/**
	 * Sets the envelope from the attribute stream. The attribute stream stores
	 * interleaved x and y. The envelope will be set to empty if the pointCount
	 * is zero.
	 */
	public void setEnvelopeFromPoints(int pointCount, Envelope2D inOutEnv) {
		if (pointCount == 0) {
			inOutEnv.setEmpty();
			return;
		}
		if (pointCount < 0)
			pointCount = size() / 2;
		else if (pointCount * 2 > size())
			throw new IllegalArgumentException();

		inOutEnv.setCoords(read(0), read(1));
		for (int i = 1; i < pointCount; i++) {
			inOutEnv.mergeNE(read(i * 2), read(i * 2 + 1));
		}
	}

	@Override
	public int calculateHashImpl(int hashCodeIn, int start, int end) {
		int hashCode = hashCodeIn;
		for (int i = start, n = size(); i < n && i < end; i++)
			hashCode = NumberUtils.hash(hashCode, read(i));

		return hashCode;
	}

	@Override
	public boolean equals(AttributeStreamBase other, int start, int end) {
		if (other == null)
			return false;

		if (!(other instanceof AttributeStreamOfDbl))
			return false;

		AttributeStreamOfDbl _other = (AttributeStreamOfDbl) other;

		int size = size();
		int sizeOther = _other.size();

		if (end > size || end > sizeOther && (size != sizeOther))
			return false;

		if (end > size)
			end = size;

		for (int i = start; i < end; i++)
			if (read(i) != _other.read(i))
				return false;

		return true;
	}

	@Override
	public void addRange(AttributeStreamBase src, int start, int count,
			boolean bForward, int stride) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (!bForward && (stride < 1 || count % stride != 0))
			throw new IllegalArgumentException();

		int oldSize = m_size;
		int newSize = oldSize + count;
		resize(newSize);

		if (bForward) {
			System.arraycopy(((AttributeStreamOfDbl) src).m_buffer, start,
					m_buffer, oldSize, count);
		} else {
			int n = count;

			for (int i = 0; i < count; i += stride) {
				n -= stride;

				for (int s = 0; s < stride; s++) {
					m_buffer[oldSize + i + s] = ((AttributeStreamOfDbl) src).m_buffer[start
							+ n + s];
				}
			}
		}
	}

	// public void addRange(AttributeStreamBase src, int start,
	// int count, boolean bForward, int stride) {
	//
	// if (m_bReadonly)
	// throw new GeometryException("invalid_call");
	//
	// if (!bForward && (stride < 1 || count % stride != 0))
	// throw new IllegalArgumentException();
	//
	// if (bForward)
	// {
	// double[] otherbuffer = ((AttributeStreamOfDbl) src).m_buffer;
	// // int newSize = size() + count;
	// // resize(newSize);
	// // System.arraycopy(otherbuffer, start, m_buffer, pos, count);
	// for (int i = 0; i < count; i++) {
	// add(otherbuffer[start + i]);
	// }
	// } else {
	// throw new GeometryException("not implemented for reverse add");
	// }
	// }

	@Override
	public void insertRange(int start, AttributeStreamBase src, int srcStart,
			int count, boolean bForward, int stride, int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (!bForward && (stride < 1 || count % stride != 0))
			throw new IllegalArgumentException();

		int excess_space = m_size - validSize;

		if (excess_space < count) {
			int original_size = m_size;
			resize(original_size + count - excess_space);
		}

		System.arraycopy(m_buffer, start, m_buffer, start + count, validSize
				- start);

		if (m_buffer == ((AttributeStreamOfDbl) src).m_buffer) {
			if (start < srcStart)
				srcStart += count;
		}

		if (bForward) {
			System.arraycopy(((AttributeStreamOfDbl) src).m_buffer, srcStart,
					m_buffer, start, count);
		} else {
			int n = count;

			for (int i = 0; i < count; i += stride) {
				n -= stride;

				for (int s = 0; s < stride; s++) {
					m_buffer[start + i + s] = ((AttributeStreamOfDbl) src).m_buffer[srcStart
							+ n + s];
				}
			}
		}
	}

	@Override
	public void insertRange(int start, double value, int count, int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		System.arraycopy(m_buffer, start, m_buffer, start + count, validSize
				- start);

		for (int i = 0; i < count; i++) {
			m_buffer[start + i] = value;
		}
	}

	@Override
	public void insertAttributes(int start, Point pt, int semantics,
			int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		int comp = VertexDescription.getComponentCount(semantics);

		System.arraycopy(m_buffer, start, m_buffer, start + comp, validSize
				- start);

		for (int c = 0; c < comp; c++) {
			m_buffer[start + c] = pt.getAttributeAsDbl(semantics, c);
		}
	}

	public void insert(int index, Point2D point, int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		System.arraycopy(m_buffer, index, m_buffer, index + 2, validSize
				- index);
		m_buffer[index] = point.x;
		m_buffer[index + 1] = point.y;
	}

	// special case for .net 2d array syntax [,]
	// writes count doubles, 2 at a time, into this stream. count is assumed to
	// be even, arrayOffset is an index of the zeroth dimension (i.e. you can't
	// start writing from dst[0,1])
	public void writeRange(int streamOffset, int count, double[][] src,
			int arrayOffset, boolean bForward) {
		if (streamOffset < 0 || count < 0 || arrayOffset < 0
				|| count > NumberUtils.intMax())
			throw new IllegalArgumentException();

		if (src.length * 2 < (int) ((arrayOffset << 1) + count))
			throw new IllegalArgumentException();
		if (count == 0)
			return;

		if (size() < count + streamOffset)
			resize(count + streamOffset);

		int j = streamOffset;
		if (!bForward)
			j += count - 1;

		final int dj = bForward ? 2 : -2;

		int end = arrayOffset + (count >> 1);
		for (int i = arrayOffset; i < end; i++) {
			m_buffer[j] = (double) src[i][0];
			m_buffer[j + 1] = (double) src[i][1];
			j += dj;
		}
	}

	public void writeRange(int streamOffset, int count, double[] src,
			int arrayOffset, boolean bForward) {
		if (streamOffset < 0 || count < 0 || arrayOffset < 0)
			throw new IllegalArgumentException();

		if (src.length < arrayOffset + count)
			throw new IllegalArgumentException();
		if (count == 0)
			return;

		if (size() < count + streamOffset)
			resize(count + streamOffset);

		if (bForward) {
			System.arraycopy(src, arrayOffset, m_buffer, streamOffset, count);
		} else {
			int j = streamOffset;
			if (!bForward)
				j += count - 1;

			int end = arrayOffset + count;
			for (int i = arrayOffset; i < end; i++) {
				m_buffer[j] = src[i];
				j--;
			}
		}
	}

	// reads count doubles, 2 at a time, into dst. count is assumed to be even,
	// arrayOffset is an index of the zeroth dimension (i.e. you can't start
	// reading into dst[0,1])
	// void AttributeStreamOfDbl::ReadRange(int streamOffset, int count,
	// array<double,2>^ dst, int arrayOffset, bool bForward)

	public void readRange(int streamOffset, int count, double[][] dst,
			int arrayOffset, boolean bForward) {
		if (streamOffset < 0 || count < 0 || arrayOffset < 0
				|| count > NumberUtils.intMax()
				|| size() < count + streamOffset)
			throw new IllegalArgumentException();

		if (dst.length * 2 < (int) ((arrayOffset << 1) + count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		int j = streamOffset;
		if (!bForward)
			j += count - 1;

		final int dj = bForward ? 2 : -2;

		int end = arrayOffset + (count >> 1);
		for (int i = arrayOffset; i < end; i++) {
			dst[i][0] = m_buffer[j];
			dst[i][1] = m_buffer[j + 1];
			j += dj;
		}

	}

	@Override
	public void eraseRange(int index, int count, int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (index + count > m_size)
			throw new GeometryException("invalid_call");

		if (validSize - (index + count) > 0) {
			System.arraycopy(m_buffer, index + count, m_buffer, index,
					validSize - (index + count));
		}
		m_size -= count;
	}

	@Override
	public void readRange(int srcStart, int count, ByteBuffer dst,
			int dstOffset, boolean bForward) {
		if (srcStart < 0 || count < 0 || dstOffset < 0
				|| size() < count + srcStart)
			throw new IllegalArgumentException();

		final int elmSize = NumberUtils.sizeOf((double) 0);

		if (dst.capacity() < (int) (dstOffset + elmSize * count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		int j = srcStart;
		if (!bForward)
			j += count - 1;

		final int dj = bForward ? 1 : -1;

		int offset = dstOffset;
		for (int i = 0; i < count; i++, offset += elmSize) {
			dst.putDouble(offset, m_buffer[j]);
			j += dj;
		}
	}

	@Override
	public void reverseRange(int index, int count, int stride) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (stride < 1 || count % stride != 0)
			throw new GeometryException("invalid_call");

		int cIterations = count >> 1;
		int n = count;

		for (int i = 0; i < cIterations; i += stride) {
			n -= stride;

			for (int s = 0; s < stride; s++) {
				double temp = m_buffer[index + i + s];
				m_buffer[index + i + s] = m_buffer[index + n + s];
				m_buffer[index + n + s] = temp;
			}
		}
	}

	@Override
	public void setRange(double value, int start, int count) {
		if (start < 0 || count < 0 || start < 0 || count + start > size())
			throw new IllegalArgumentException();

		double v = value;
		Arrays.fill(m_buffer, start, start + count, v);
		// for (int i = start, n = start + count; i < n; i++)
		// write(i, v);
	}

	@Override
	public void writeRange(int startElement, int count,
			AttributeStreamBase _src, int srcStart, boolean bForward, int stride) {
		if (startElement < 0 || count < 0 || srcStart < 0)
			throw new IllegalArgumentException();

		if (!bForward && (stride <= 0 || (count % stride != 0)))
			throw new IllegalArgumentException();

		AttributeStreamOfDbl src = (AttributeStreamOfDbl) _src; // the input
		// type must
		// match

		if (src.size() < (int) (srcStart + count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		if (size() < count + startElement)
			resize(count + startElement);

		if (_src == (AttributeStreamBase) this) {
			_selfWriteRangeImpl(startElement, count, srcStart, bForward, stride);
			return;
		}

		if (bForward) {
			int j = startElement;
			int offset = srcStart;
			for (int i = 0; i < count; i++) {
				m_buffer[j] = src.m_buffer[offset];
				j++;
				offset++;
			}
		} else {
			int j = startElement;
			int offset = srcStart + count - stride;
			if (stride == 1) {
				for (int i = 0; i < count; i++) {
					m_buffer[j] = src.m_buffer[offset];
					j++;
					offset--;
				}
			} else {
				for (int i = 0, n = count / stride; i < n; i++) {
					for (int k = 0; k < stride; k++)
						m_buffer[j + k] = src.m_buffer[offset + k];

					j += stride;
					offset -= stride;
				}
			}
		}
	}

	private void _selfWriteRangeImpl(int toElement, int count, int fromElement,
			boolean bForward, int stride) {

		// writing from to this stream.
		if (bForward) {
			if (toElement == fromElement)
				return;
		}

		System.arraycopy(m_buffer, fromElement, m_buffer, toElement, count);

		if (bForward)
			return;
		// reverse what we written
		int j = toElement;
		int offset = toElement + count - stride;
		int dj = stride;
		for (int i = 0, n = count / 2; i < n; i++) {
			for (int k = 0; k < stride; k++) {
				double v = m_buffer[j + k];
				m_buffer[j + k] = m_buffer[offset + k];
				m_buffer[offset + k] = v;
			}
			j += stride;
			offset -= stride;
		}

	}

	@Override
	public void writeRange(int startElement, int count, ByteBuffer src,
			int offsetBytes, boolean bForward) {
		if (startElement < 0 || count < 0 || offsetBytes < 0)
			throw new IllegalArgumentException();

		final int elmSize = NumberUtils.sizeOf((double) 0);
		if (src.capacity() < (int) (offsetBytes + elmSize * count))
			throw new IllegalArgumentException();

		if (count == 0)
			return;

		if (size() < count + startElement)
			resize(count + startElement);

		int j = startElement;
		if (!bForward)
			j += count - 1;

		final int dj = bForward ? 1 : -1;

		int offset = offsetBytes;
		for (int i = 0; i < count; i++, offset += elmSize) {
			m_buffer[j] = src.getDouble(offset);
			j += dj;
		}

	}

	public void writeRange(int streamOffset, int pointCount, Point2D[] src,
			int arrayOffset, boolean bForward) {
		if (streamOffset < 0 || pointCount < 0 || arrayOffset < 0)
			throw new IllegalArgumentException();

		// if (src->Length < (int)(arrayOffset + pointCount)) jt: we have lost
		// the length check, not sure about this
		// GEOMTHROW(invalid_argument);

		if (pointCount == 0)
			return;

		if (size() < (pointCount << 1) + streamOffset)
			resize((pointCount << 1) + streamOffset);

		int j = streamOffset;
		if (!bForward)
			j += (pointCount - 1) << 1;

		final int dj = bForward ? 2 : -2;

		// TODO: refactor to take advantage of the known block array structure

		final int i0 = arrayOffset;
		pointCount += i0;
		for (int i = i0; i < pointCount; i++) {
			m_buffer[j] = src[i].x;
			m_buffer[j + 1] = src[i].y;
			j += dj;
		}
	}

	// Less efficient as boolean bForward set to false, as it is looping through
	// half
	// of the elements of the array
	public void readRange(int srcStart, int count, double[] dst, int dstOffset,
			boolean bForward) {
		if (srcStart < 0 || count < 0 || dstOffset < 0
				|| size() < count + srcStart)
			throw new IllegalArgumentException();

		if (bForward)
			System.arraycopy(m_buffer, srcStart, dst, dstOffset, count);
		else {
			int j = dstOffset + count - 1;
			for (int i = srcStart; i < count; i++) {
				dst[j] = m_buffer[i];
				j--;
			}
		}
	}

	public void sort(int start, int end) {
		Arrays.sort(m_buffer, start, end);
	}
}
