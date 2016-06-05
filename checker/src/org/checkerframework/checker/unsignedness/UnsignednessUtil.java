package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;

import java.nio.ByteBuffer;

/**
 * Provides a series of static utility functions for users which extend and wrap
 * JDK methods which can be used correctly with unsigned integers. In particular
 * we offer methods which wrap existing JDK methods, containing warning suppressions
 * to make use of un-annotated JDK code. We also offer extended versions of existing JDK methods
 * to allow methods to be used in more situations.
 */
public final class UnsignednessUtil {

	/**
	 * Gets an Unsigned short from the ByteBuffer b. Wraps {@link #ByteBuffer#getShort()}.
	 * 
	 * This should be used when one would normally use {@link #ByteBuffer#getShort()},
	 * but the result should be interpreted as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static @Unsigned short getUnsignedShort(ByteBuffer b) {
		return b.getShort();
	}

	/**
	 * Gets an Unsigned byte from the ByteBuffer b. Wraps {@link #ByteBuffer#get()}.
	 * 
	 * This should be used when one would normally use {@link #ByteBuffer#get()},
	 * but the result should be interpreted as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static @Unsigned byte getUnsigned(ByteBuffer b) {
		return b.get();
	}

	/**
	 * Gets an array of Unsigned byte[] from the ByteBuffer b and stores in
	 * the array bs. Wraps {@link #ByteBuffer#get(byte[])}.
	 *
	 * This should be used when one would normally use {@link #ByteBuffer#get(byte[])},
	 * but the array of bytes should be interpreted as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
		b.get(bs);
	}

	/**
	 * Compares two Unsigned longs x and y. This returns a negative number iff
	 * x < y, a positive number iff x > y, and zero iff x == y. Wraps {@link #Long#compareUnsigned(long, long)}.
	 *
	 * This should be used when one would normally compare two longs with < or >
	 * but cannot as the longs are Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
		return Long.compareUnsigned(x, y);
	}

	/**
	 * Compares two Unsigned ints x and y. This returns a negative number iff
	 * x < y, a positive number iff x > y, and zero iff x == y. Wraps {@link #Int#compareUnsigned(int, int)}.
	 *
	 * This should be used when one would normally compare two ints with < or >
	 * but cannot as the ints are Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
		return Integer.compareUnsigned(x, y);
	}

	/**
	 * Compares two Unsigned shorts x and y. This returns a negative number iff
	 * x < y, a positive number iff x > y, and zero iff x == y. Extends {@link #Int#compareUnsigned(int, int)}
	 * to act on short arguments.
	 *
	 * This should be used when one would normally compare two shorts with < or >
	 * but cannot as the shorts are Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
		return Integer.compareUnsigned(
				Short.toUnsignedInt(x),
				Short.toUnsignedInt(y)
		);
	}

	/**
	 * Compares two Unsigned bytes x and y. This returns a negative number iff
	 * x < y, a positive number iff x > y, and zero iff x == y. Extends {@link #Int#compareUnsigned(int, int)}
	 * to act on byte arguments.
	 *
	 * This should be used when one would normally compare two bytes with < or >
	 * but cannot as the bytes are Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
		return Integer.compareUnsigned(
				Byte.toUnsignedInt(x),
				Byte.toUnsignedInt(y)
		);
	}
	
	/**
	 * Produces a string representation of the Unsigned long l. Wraps {@link #Long#toUnsignedString(long)}.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned long and cannot rely on Java to interpret the long as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned long l) {
		return Long.toUnsignedString(l);
	}
	
	/**
	 * Produces a string representation of the Unsigned long l in base radix. Wraps {@link #Long#toUnsignedString(long, int)}.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned long and cannot rely on Java to interpret the long as Unsigned, but also
	 * wants to represent it in a base other than ten.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned long l, int radix) {
		return Long.toUnsignedString(l, radix);
	}
	
	/**
	 * Produces a string representation of the Unsigned int i. Wraps {@link #Int#toUnsignedString(int)}.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned int and cannot rely on Java to interpret the int as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned int i) {
		return Integer.toUnsignedString(i);
	}
	
	/**
	 * Produces a string representation of the Unsigned int i in base radix. Wraps {@link #Int#toUnsignedString(int, int)}.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned int and cannot rely on Java to interpret the int as Unsigned, but also
	 * wants to represent it in a base other than ten.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned int i, int radix) {
		return Integer.toUnsignedString(i, radix);
	}
	
	/**
	 * Produces a string representation of the Unsigned short s. Extends {@link #Int#toUnsignedString(int)}
	 * to operate on a short argument.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned short and cannot rely on Java to interpret the short as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned short s) {
		return Integer.toUnsignedString(Short.toUnsignedInt(s));
	}
	
	/**
	 * Produces a string representation of the Unsigned short s in base radix. Extends {@link #Int#toUnsignedString(int, int)}
	 * to operate on a short argument.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned short and cannot rely on Java to interpret the short as Unsigned, but also
	 * wants to represent it in a base other than ten.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned short s, int radix) {
		return Integer.toUnsignedString(Short.toUnsignedInt(s), radix);
	}
	
	/**
	 * Produces a string representation of the Unsigned byte b. Extends {@link #Int#toUnsignedString(int)}
	 * to operate on a byte argument.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned byte and cannot rely on Java to interpret the byte as Unsigned.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned byte b) {
		return Integer.toUnsignedString(Byte.toUnsignedInt(b));
	}
	
	/**
	 * Produces a string representation of the Unsigned byte b in base radix. Extends {@link #Int#toUnsignedString(int, int)}
	 * to operate on a byte argument.
	 *
	 * This should be used when one needs to get the string representation of an
	 * Unsigned byte and cannot rely on Java to interpret the byte as Unsigned, but also
	 * wants to represent it in a base other than ten.
	 */
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned byte b, int radix) {
		return Integer.toUnsignedString(Byte.toUnsignedInt(b), radix);
	}
}
