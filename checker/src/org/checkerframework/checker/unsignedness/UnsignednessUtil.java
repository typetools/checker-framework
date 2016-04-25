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
	public static int compareUnsignedLongs(@Unsigned long x, @Unsigned long y) {
		return Long.compareUnsigned(x, y);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsignedInts(@Unsigned int x, @Unsigned int y) {
		return Integer.compareUnsigned(x, y);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsignedShorts(@Unsigned short x, @Unsigned short y) {
		return Integer.compareUnsigned(
				Short.toUnsignedInt(x),
				Short.toUnsignedInt(y)
		);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsignedBytes(@Unsigned byte x, @Unsigned byte y) {
		return Integer.compareUnsigned(
				Byte.toUnsignedInt(x),
				Byte.toUnsignedInt(y)
		);
	}
}
