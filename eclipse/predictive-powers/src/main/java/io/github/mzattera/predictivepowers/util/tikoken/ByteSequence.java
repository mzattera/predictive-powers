/*
 * Copyright (c) 2023 Mariusz Bernacki <info@didalgo.com>
 * SPDX-License-Identifier: MIT
 */

/*
 * MIT License
 * 
 * Copyright (c) 2023 didalgo2
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.mzattera.predictivepowers.util.tikoken;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents a sequences of bytes.
 *
 * @author Mariusz Bernacki
 *
 */
public class ByteSequence implements Comparable<ByteSequence> {

	private final byte[] bytes;

	private ByteSequence(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * Returns a new ByteSequence instance containing the specified byte array. The
	 * provided byte array is wrapped in an ImmutableByteSequence to ensure that the
	 * contents of the byte array are not modified after the ByteSequence is
	 * created.
	 *
	 * @param bytes the byte array to be used for the new ByteSequence
	 * @return a new ByteSequence instance containing the specified byte array
	 * @throws NullPointerException if the provided byte array is null
	 */
	static ByteSequence of(byte[] bytes) {
		return new ByteSequence(Arrays.copyOf(bytes, bytes.length));
	}

	/**
	 * Creates a ByteSequence from the specified String using the UTF-8 charset.
	 *
	 * @param text the String to be converted to a ByteSequence
	 * @return a new ByteSequence that represents the specified String using the
	 *         UTF-8 charset
	 * @throws NullPointerException if the provided text is null
	 */
	static ByteSequence from(String text) {
		return from(text, StandardCharsets.UTF_8);
	}

	/**
	 * Creates a ByteSequence from the specified String using the specified Charset.
	 *
	 * @param text    the String to be converted to a ByteSequence
	 * @param charset the Charset to be used for the conversion
	 * @return a new ByteSequence that represents the specified String using the
	 *         specified Charset
	 * @throws NullPointerException if the provided text or charset is null
	 */
	static ByteSequence from(String text, Charset charset) {
		return new ByteSequence(text.getBytes(charset));
	}

	/**
	 * Returns the length of the byte sequence.
	 *
	 * @return the number of bytes in the sequence
	 */
	int length() {
		return bytes.length;
	}

	/**
	 * Returns a new ByteSequence that is a sub-sequence of the current byte
	 * sequence. The sub-sequence starts with the byte value at the specified
	 * {@code start} index and extends to the byte value at index {@code end - 1}.
	 *
	 * @param start the beginning index, inclusive
	 * @param end   the ending index, exclusive
	 * @return a new ByteSequence that is a sub-sequence of this byte sequence
	 * @throws IndexOutOfBoundsException if the start or end index is invalid
	 */
	ByteSequence subSequence(int start, int end) {
		return new ByteSequence(Arrays.copyOfRange(bytes, start, end));
	}

	/**
	 * Returns a byte array representation of this byte sequence. The returned array
	 * will be a copy of the internal byte array, ensuring that modifications to the
	 * returned array do not affect the original byte sequence.
	 *
	 * @return a byte array representation of this byte sequence
	 */
	byte[] toByteArray() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	/**
	 * Compares the specified object with this byte sequence for equality. Returns
	 * {@code true} if and only if the specified object is also a byte sequence and
	 * both byte sequences have the same bytes in the same order.
	 *
	 * @param obj the object to be compared for equality with this byte sequence
	 * @return {@code true} if the specified object is equal to this byte sequence,
	 *         {@code false} otherwise
	 */
	@Override
	public boolean equals(Object other) {
		if ((other != null) && (other instanceof ByteSequence)) {
			return Arrays.equals(bytes, ((ByteSequence) other).bytes);
		}
		return false;
	}

	/**
	 * Returns a hash code value for this byte sequence.
	 *
	 * @return a hash code value for this byte sequence
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	@Override
	public int compareTo(ByteSequence other) {
		return Arrays.compare(bytes, other.bytes);
	}

	/**
	 * Converts the byte sequence to a String using the specified Charset.
	 *
	 * @param charset the Charset to be used for the conversion
	 * @return a String representation of this byte sequence using the specified
	 *         Charset
	 */
	public String toString(Charset charset) {
		return new String(bytes, charset);
	}

	@Override
	public String toString() {
		return toString(StandardCharsets.UTF_8);
	}
}
