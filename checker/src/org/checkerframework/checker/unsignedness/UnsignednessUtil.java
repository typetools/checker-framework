package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;

import java.nio.ByteBuffer;

/**
 * Provides a series of static utility functions for users which extend and wrap
 * JDK methods which can be used correctly with unsigned integers. In particular
 * we offer methods which wrap existing JDK methods, containing warning suppression
 * to the wrapper method, and we offer extended versions of existing JDK methods
 * to allow methods to be used in more situations.
 */
public final class UnsignednessUtil {

	/**
	 * Gets an @Unsigned short from the ByteBuffer b. Wraps ByteBuffer::getShort.
	 *
	 * @param b
	 */
	@SuppressWarnings("unsignedness")
	public static @Unsigned short getUnsignedShort(ByteBuffer b) {
		return b.getShort();
	}

	/**
	 * Gets an @Unsigned byte from the ByteBuffer b. Wraps ByteBuffer::get.
	 *
	 * @param b
	 */
	@SuppressWarnings("unsignedness")
	public static @Unsigned byte getUnsigned(ByteBuffer b) {
		return b.get();
	}

	/**
	 * Gets an array of @Unsigned byte[] from the ByteBuffer b and stores in
	 * the array bs. Wraps ByteBuffer::get.
	 *
	 * @param b
	 */
	@SuppressWarnings("unsignedness")
	public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
		b.get(bs);
	}

	/**
	 * Compares two @Unsigned longs x and y. This returns a negative number if
	 * x < y, a positive number if x > y, and zero if x == y. Wraps Long::compareUnsigned.
	 *
	 * @param x
	 * @param y
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
		return Long.compareUnsigned(x, y);
	}

	/**
	 * Compares two @Unsigned ints x and y. This returns a negative number if
	 * x < y, a positive number if x > y, and zero if x == y. Wraps Integer::compareUnsigned.
	 *
	 * @param x
	 * @param y
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
		return Integer.compareUnsigned(x, y);
	}

	/**
	 * Compares two @Unsigned shorts x and y. This returns a negative number if
	 * x < y, a positive number if x > y, and zero if x == y. Extends Integer::compareUnsigned.
	 *
	 * @param x
	 * @param y
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
		return Integer.compareUnsigned(
				Short.toUnsignedInt(x),
				Short.toUnsignedInt(y)
		);
	}

	/**
	 * Compares two @Unsigned bytes x and y. This returns a negative number if
	 * x < y, a positive number if x > y, and zero if x == y. Extends Integer::compareUnsigned.
	 *
	 * @param x
	 * @param y
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
		return Integer.compareUnsigned(
				Byte.toUnsignedInt(x),
				Byte.toUnsignedInt(y)
		);
	}
	
	/**
	 * Produces a string representation of the @Unsigned long l. Wraps Long::toUnsignedString.
	 *
	 * @param l
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned long l) {
		return Long.toUnsignedString(l);
	}
	
	/**
	 * Produces a string representation of the @Unsigned long l in base radix. Wraps Long::toUnsignedString.
	 *
	 * @param l
	 * @param radix
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned long l, int radix) {
		return Long.toUnsignedString(l, radix);
	}
	
	/**
	 * Produces a string representation of the @Unsigned int i. Wraps Integer::toUnsignedString.
	 *
	 * @param i
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned int i) {
		return Integer.toUnsignedString(i);
	}
	
	/**
	 * Produces a string representation of the @Unsigned int i in base radix. Wraps Integer::toUnsignedString.
	 *
	 * @param i
	 * @param radix
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned int i, int radix) {
		return Integer.toUnsignedString(i, radix);
	}
	
	/**
	 * Produces a string representation of the @Unsigned short s. 
	 * Extends Integer::toUnsignedString.
	 *
	 * @param s
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned short s) {
		return Integer.toUnsignedString(Short.toUnsignedInt(s));
	}
	
	/**
	 * Produces a string representation of the @Unsigned short s in base radix. 
	 * Extends Integer::toUnsignedString.
	 *
	 * @param s
	 * @param radix
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned short s, int radix) {
		return Integer.toUnsignedString(Short.toUnsignedInt(s), radix);
	}
	
	/**
	 * Produces a string representation of the @Unsigned byte b. 
	 * Extends Integer::toUnsignedString.
	 *
	 * @param b
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned byte b) {
		return Integer.toUnsignedString(Byte.toUnsignedInt(b));
	}
	
	/**
	 * Produces a string representation of the @Unsigned byte b in base radix. 
	 * Extends Integer::toUnsignedString.
	 *
	 * @param b
	 * @param radix
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned byte b, int radix) {
		return Integer.toUnsignedString(Byte.toUnsignedInt(b), radix);
	}
}
