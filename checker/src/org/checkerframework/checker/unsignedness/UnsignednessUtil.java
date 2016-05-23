package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;

import java.nio.ByteBuffer;

public final class UnsignednessUtil {

	@SuppressWarnings("unsignedness")
	public static @Unsigned short getUnsignedShort(ByteBuffer b) {
		return b.getShort();
	}

	@SuppressWarnings("unsignedness")
	public static @Unsigned byte getUnsigned(ByteBuffer b) {
		return b.get();
	}

	@SuppressWarnings("unsignedness")
	public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
		b.get(bs);
	}

	
	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
		return Long.compareUnsigned(x, y);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
		return Integer.compareUnsigned(x, y);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
		return Integer.compareUnsigned(
				Short.toUnsignedInt(x),
				Short.toUnsignedInt(y)
		);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
		return Integer.compareUnsigned(
				Byte.toUnsignedInt(x),
				Byte.toUnsignedInt(y)
		);
	}
	
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned long l) {
		return Long.toUnsignedString(l);
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned long l, int radix) {
		return Long.toUnsignedString(l, radix);
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned int i) {
		return Integer.toUnsignedString(i);
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned int i, int radix) {
		return Integer.toUnsignedString(i, radix);
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned short s) {
		return Integer.toUnsignedString(Short.toUnsignedInt(s));
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned short s, int radix) {
		return Integer.toUnsignedString(Short.toUnsignedInt(s), radix);
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned byte b) {
		return Integer.toUnsignedString(Byte.toUnsignedInt(b));
	}
	
	@SuppressWarnings("unsignedness")
	public static String toUnsignedString(@Unsigned byte b, int radix) {
		return Integer.toUnsignedString(Byte.toUnsignedInt(b), radix);
	}
}
