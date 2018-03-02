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

import static com.esri.core.geometry.SizeOf.SIZE_OF_ATTRIBUTE_STREAM_OF_INT64;
import static com.esri.core.geometry.SizeOf.sizeOfLongArray;

final class AttributeStreamOfInt64 extends AttributeStreamBase {
	private long[] m_buffer = null;
	private int m_size;

	public int size() {
		return m_size;
	}

	public void reserve(int reserve)// only in Java
	{
		if (reserve <= 0)
			return;
		if (m_buffer == null)
			m_buffer = new long[reserve];
		else {
			if (reserve <= m_buffer.length)
				return;
			long[] buf = new long[reserve];
			System.arraycopy(m_buffer, 0, buf, 0, m_size);
			m_buffer = buf;
		}

	}

	public int capacity() {
		return m_buffer != null ? m_buffer.length : 0;
	}
	
	public AttributeStreamOfInt64(int size) {
		int sz = size;
		if (sz < 2)
			sz = 2;
		m_buffer = new long[sz];
		m_size = size;
	}

	public AttributeStreamOfInt64(int size, long defaultValue) {
		int sz = size;
		if (sz < 2)
			sz = 2;
		m_buffer = new long[sz];
		m_size = size;
		for (int i = 0; i < size; i++)
			m_buffer[i] = defaultValue;
	}

	public AttributeStreamOfInt64(AttributeStreamOfInt64 other) {
		m_buffer = other.m_buffer.clone();
		m_size = other.m_size;
	}

	public AttributeStreamOfInt64(AttributeStreamOfInt64 other, int maxSize) {
		m_size = other.size();
		if (m_size > maxSize)
			m_size = maxSize;
		int sz = m_size;
		if (sz < 2)
			sz = 2;
		m_buffer = new long[sz];
		System.arraycopy(other.m_buffer, 0, m_buffer, 0, m_size);
	}

	/**
	 * Reads a value from the buffer at given offset.
	 * 
	 * @param offset
	 *            is the element number in the stream.
	 */
	public long read(int offset) {
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
	public void write(int offset, long value) {
		if (m_bReadonly) {
			throw new RuntimeException("invalid_call");
		}
		m_buffer[offset] = value;
	}

	/**
	 * Adds a new value at the end of the stream.
	 * 
	 * @param offset
	 *            is the element number in the stream.
	 * @param value
	 *            is the value to write.
	 */
	public void add(long v) {
		resize(m_size + 1);
		m_buffer[m_size - 1] = v;
	}

	@Override
	public AttributeStreamBase restrictedClone(int maxsize) {
		int len = m_size;
		int newSize = maxsize < len ? maxsize : len;
		long[] newBuffer = new long[newSize];
		System.arraycopy(m_buffer, 0, newBuffer, 0, newSize);
		m_buffer = newBuffer;
		m_size = newSize;
		return this;
	}

	@Override
	public int virtualSize() {
		return size();
	}

	@Override
	public long estimateMemorySize()
	{
		return SIZE_OF_ATTRIBUTE_STREAM_OF_INT64 + sizeOfLongArray(m_buffer.length);
	}

	@Override
	public int getPersistence() {
		return Persistence.enumInt64;
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
				long[] newBuffer = new long[newSize];
				System.arraycopy(m_buffer, 0, newBuffer, 0, newSize);
				m_buffer = newBuffer;
			}
			m_size = newSize;
		} else {
			if (newSize > m_buffer.length) {
				int sz = (newSize < 64) ? Math.max(newSize * 2, 4)
						: (newSize * 5) / 4;
				long[] newBuffer = new long[sz];
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
				long[] newBuffer = new long[newSize];
				System.arraycopy(m_buffer, 0, newBuffer, 0, newSize);
				m_buffer = newBuffer;
			}
			m_size = newSize;
		} else {
			if (newSize > m_buffer.length) {
				int sz = (newSize < 64) ? Math.max(newSize * 2, 4)
						: (newSize * 5) / 4;
				long[] newBuffer = new long[sz];
				System.arraycopy(m_buffer, 0, newBuffer, 0, m_size);
				m_buffer = newBuffer;
			}

			for (int i = m_size; i < newSize; i++)
				m_buffer[i] = (long) defaultValue;

			m_size = newSize;
		}
	}

	@Override
	public void writeAsDbl(int offset, double d) {
		write(offset, (long) d);
	}

	@Override
	public void writeAsInt64(int offset, long d) {
		write(offset, (long) d);
	}

	@Override
	public void writeAsInt(int offset, int d) {
		write(offset, (long) d);
	}

	// @Override
	// public void writeRange(int srcStart, int count, ByteBuffer dst,
	// int dstOffsetBytes) {
	// // TODO Auto-generated method stub
	//
	// }

	@Override
	public int calculateHashImpl(int hashCode, int start, int end) {
		for (int i = start, n = size(); i < n && i < end; i++)
			hashCode = NumberUtils.hash(hashCode, read(i));

		return hashCode;
	}

	@Override
	public boolean equals(AttributeStreamBase other, int start, int end) {
		if (other == null)
			return false;

		if (!(other instanceof AttributeStreamOfInt64))
			return false;

		AttributeStreamOfInt64 _other = (AttributeStreamOfInt64) other;

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
			System.arraycopy(((AttributeStreamOfInt64) src).m_buffer, start,
					m_buffer, oldSize, count);
		} else {
			int n = count;

			for (int i = 0; i < count; i += stride) {
				n -= stride;

				for (int s = 0; s < stride; s++) {
					m_buffer[oldSize + i + s] = ((AttributeStreamOfInt64) src).m_buffer[start
							+ n + s];
				}
			}
		}
	}

	@Override
	public void insertRange(int start, AttributeStreamBase src, int srcStart,
			int count, boolean bForward, int stride, int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (!bForward && (stride < 1 || count % stride != 0))
			throw new IllegalArgumentException();

		System.arraycopy(m_buffer, start, m_buffer, start + count, validSize
				- start);

		if (m_buffer == ((AttributeStreamOfInt64) src).m_buffer) {
			if (start < srcStart)
				srcStart += count;
		}

		if (bForward) {
			System.arraycopy(((AttributeStreamOfInt64) src).m_buffer, srcStart,
					m_buffer, start, count);
		} else {
			int n = count;

			for (int i = 0; i < count; i += stride) {
				n -= stride;

				for (int s = 0; s < stride; s++) {
					m_buffer[start + i + s] = ((AttributeStreamOfInt64) src).m_buffer[srcStart
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

		long v = (long) value;
		for (int i = 0; i < count; i++) {
			m_buffer[start + i] = v;
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
			m_buffer[start + c] = (long) pt.getAttributeAsDbl(semantics, c);
		}
	}

	@Override
	public void eraseRange(int index, int count, int validSize) {
		if (m_bReadonly)
			throw new GeometryException("invalid_call");

		if (index + count > m_size)
			throw new GeometryException("invalid_call");

		System.arraycopy(m_buffer, index + count, m_buffer, index, validSize
				- (index + count));
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
			dst.putLong(offset, m_buffer[j]);
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
				long temp = m_buffer[index + i + s];
				m_buffer[index + i + s] = m_buffer[index + n + s];
				m_buffer[index + n + s] = temp;
			}
		}
	}

	@Override
	public void setRange(double value, int start, int count) {
		if (start < 0 || count < 0 || start < 0 || count + start > size())
			throw new IllegalArgumentException();

		long v = (long) value;
		for (int i = start, n = start + count; i < n; i++)
			write(i, v);
	}

	@Override
	public void writeRange(int startElement, int count,
			AttributeStreamBase _src, int srcStart, boolean bForward, int stride) {
		if (startElement < 0 || count < 0 || srcStart < 0)
			throw new IllegalArgumentException();

		if (!bForward && (stride <= 0 || (count % stride != 0)))
			throw new IllegalArgumentException();

		AttributeStreamOfInt64 src = (AttributeStreamOfInt64) _src; // the input
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

		int offset;
		int j;
		int dj;

		if (fromElement < toElement) {
			offset = fromElement + count - stride;
			j = toElement + count - stride;
			for (int i = 0, n = count / 2; i < n; i++) {
				for (int k = 0; k < stride; k++) {
					m_buffer[j + k] = m_buffer[offset + k];
				}
				j -= stride;
				offset -= stride;
			}
		} else {
			offset = fromElement;
			j = toElement;
			dj = 1;
			for (int i = 0; i < count; i++) {
				m_buffer[j] = m_buffer[offset];
				j += 1;
				offset++;
			}
		}

		if (!bForward) {
			// reverse what we written
			j = toElement;
			offset = toElement + count - stride;
			dj = stride;
			for (int i = 0, n = count / 2; i < n; i++) {
				for (int k = 0; k < stride; k++) {
					long v = m_buffer[j + k];
					m_buffer[j + k] = m_buffer[offset + k];
					m_buffer[offset + k] = v;
				}
				j += stride;
				offset -= stride;
			}
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
			m_buffer[j] = src.getLong(offset);
			j += dj;
		}

	}
}
