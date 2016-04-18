package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;

import java.nio.ByteBuffer;

public final class UnsignednessUtil {

	@SuppressWarnings("unsignedness")
	public static @Unsigned short getUnsignedShort(ByteBuffer b) {
		return (/*@Unsigned*/ short) b.getShort();
	}

	@SuppressWarnings("unsignedness")
	public static @Unsigned byte getUnsigned(ByteBuffer b) {
		return (/*@Unsigned*/ byte) b.get();
	}

	@SuppressWarnings("unsignedness")
	public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
		byte[] temp = (/*@Signed*/ byte[]) bs;
		b.get(temp);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsignedInts(@Unsigned int x, @Unsigned int y) {
		return Integer.compareUnsigned((/*@Signed*/ int) x, (/*@Signed*/ int) y);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsignedShorts(@Unsigned short x, @Unsigned short y) {
		return Integer.compareUnsigned(
				Short.toUnsignedInt((/*@Signed*/ short) x),
				Short.toUnsignedInt((/*@Signed*/ short) y)
		);
	}

	@SuppressWarnings("unsignedness")
	public static int compareUnsignedBytes(@Unsigned byte x, @Unsigned byte y) {
		return Integer.compareUnsigned(
				Byte.toUnsignedInt((/*@Signed*/ byte) x),
				Byte.toUnsignedInt((/*@Signed*/ byte) y)
		);
	}
}