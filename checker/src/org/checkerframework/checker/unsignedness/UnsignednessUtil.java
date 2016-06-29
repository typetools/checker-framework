package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Provides static utility methods for unsigned values.
 * Some re-implement functionality in JDK 8,
 * making it available in earlier versions of Java.
 * Others provide new functionality.
 */
public final class UnsignednessUtil {

    private UnsignednessUtil() {
        throw new Error("Do not instantiate");
    }

    /**
     * Gets an unsigned short from the ByteBuffer b.
     * Wraps {@link java.nio.ByteBuffer#getShort() getShort()},
     * but assumes that the result should be interpreted as unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static @Unsigned short getUnsignedShort(ByteBuffer b) {
        return b.getShort();
    }

    /**
     * Gets an unsigned byte from the ByteBuffer b.
     * Wraps {@link java.nio.ByteBuffer#get() get()},
     * but assumes that the result should be interpreted as unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static @Unsigned byte getUnsigned(ByteBuffer b) {
        return b.get();
    }

    /**
     * Gets an array of unsigned bytes from the ByteBuffer b
     * and stores them in the array bs.
     * Wraps {@link java.nio.ByteBuffer#get(byte[]) get(byte[])},
     * but assumes that the array of bytes should be interpreted as unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
        b.get(bs);
    }

    /**
     * Compares two unsigned longs x and y.
     *
     * This is a reimplementation of Java 8's
     * {@code Long.compareUnsigned(long, long)}.
     *
     * @return a negative number iff x {@literal <} y,
     *         a positive number iff x {@literal >} y,
     *         and zero iff x == y.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
        // Java 8 version: return Long.compareUnsigned(x, y);
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

    /**
     * Compares two unsigned ints x and y.
     *
     * This is a reimplementation of Java 8's
     * {@code Integer.compareUnsigned(int, int)}.
     *
     * @return a negative number iff x {@literal <} y,
     *         a positive number iff x {@literal >} y,
     *         and zero iff x == y.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
        // Java 8 version: return Integer.compareUnsigned(x, y);
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    /**
     * Compares two unsigned shorts x and y.
     *
     * @return a negative number iff x {@literal <} y,
     *         a positive number iff x {@literal >} y,
     *         and zero iff x == y.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
        // Java 8 version: return Integer.compareUnsigned(Short.toUnsignedInt(x), Short.toUnsignedInt(y));
        return compareUnsigned(toUnsignedInt(x), toUnsignedInt(y));
    }

    /**
     * Compares two unsigned bytes x and y.
     *
     * @return a negative number iff x {@literal <} y,
     *         a positive number iff x {@literal >} y,
     *         and zero iff x == y.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
        // Java 8 version: return Integer.compareUnsigned(Byte.toUnsignedInt(x), Byte.toUnsignedInt(y));
        return compareUnsigned(toUnsignedInt(x), toUnsignedInt(y));
    }

    /**
     * Produces a string representation of the unsigned long l.
     *
     * This is a reimplementation of Java 8's
     * {@code Long.toUnsignedString(long)}.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned long l) {
        // Java 8 version: return Long.toUnsignedString(l);
        return toUnsignedBigInteger(l).toString();
    }

    /**
     * Produces a string representation of the unsigned long l in base radix.
     *
     * This is a reimplementation of Java 8's
     * {@code Long.toUnsignedString(long, int)}.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned long l, int radix) {
        // Java 8 version: return Long.toUnsignedString(l, radix);
        return toUnsignedBigInteger(l).toString(radix);
    }

    /**
     * Produces a string representation of the unsigned int i.
     *
     * This is a reimplementation of Java 8's
     * {@code Integer.toUnsignedString(int)}.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned int i) {
        // Java 8 version: return Integer.toUnsignedString(i);
        return Long.toString(toUnsignedLong(i));
    }

    /**
     * Produces a string representation of the unsigned int i in base radix.
     *
     * This is a reimplementation of Java 8's
     * {@code Integer.toUnsignedString(int, int)}.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned int i, int radix) {
        // Java 8 version: return Integer.toUnsignedString(i, radix);
        return Long.toString(toUnsignedLong(i), radix);
    }

    /**
     * Produces a string representation of the unsigned short s.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned short s) {
        // Java 8 version: return Integer.toUnsignedString(Short.toUnsignedInt(s));
        return Long.toString(toUnsignedLong(s));
    }

    /**
     * Produces a string representation of the unsigned short s in base radix.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned short s, int radix) {
        // Java 8 version: return Integer.toUnsignedString(Short.toUnsignedInt(s), radix);
        return Long.toString(toUnsignedLong(s), radix);
    }

    /**
     * Produces a string representation of the unsigned byte b.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned byte b) {
        // Java 8 version: return Integer.toUnsignedString(Byte.toUnsignedInt(b));
        return Long.toString(toUnsignedLong(b));
    }

    /**
     * Produces a string representation of the unsigned byte b in base radix.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned byte b, int radix) {
        // Java 8 version: return Integer.toUnsignedString(Byte.toUnsignedInt(b), radix);
        return Long.toString(toUnsignedLong(b), radix);
    }

    /*
     * Creates a BigInteger representing the same value as unsigned long.
     *
     * This is a reimplementation of Java 8's
     * {@code Long.toUnsignedBigInteger(long)}.
     */
    @SuppressWarnings("unsignedness")
    private static @Unsigned BigInteger toUnsignedBigInteger(@Unsigned long l) {
        // Java 8 version: return Long.toUnsignedBigInteger(l);
        if (l >= 0L)
            return BigInteger.valueOf(l);
        else {
            int upper = (int) (l >>> 32);
            int lower = (int) l;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(toUnsignedLong(upper))).shiftLeft(32).
                add(BigInteger.valueOf(toUnsignedLong(lower)));
        }
    }

    /**
     * Returns an unsigned long representing the same value as an unsigned int.
     *
     * This is a reimplementation of Java 8's
     * {@code Integer.toUnsignedLong(int)}.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned int i) {
        // Java 8 version: Integer.toUnsignedLong(i)
        return ((long) i) & 0xffffffffL;
    }

    /**
     * Returns an unsigned long representing the same value as an unsigned short.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned short s) {
        return ((long) s) & 0xffffL;
    }

    /**
     * Returns an unsigned int representing the same value as an unsigned short.
     */
    public static @Unsigned int toUnsignedInt(@Unsigned short s) {
        return ((int) s) & 0xffff;
    }

    /**
     * Returns an unsigned long representing the same value as an unsigned byte.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned byte b) {
        return ((long) b) & 0xffL;
    }

    /**
     * Returns an unsigned int representing the same value as an unsigned byte.
     */
    public static @Unsigned int toUnsignedInt(@Unsigned byte b) {
        return ((int) b) & 0xff;
    }

    /**
     * Returns an unsigned short representing the same value as an unsigned byte.
     */
    public static @Unsigned short toUnsignedShort(@Unsigned byte b) {
        return (short) (((int) b) & 0xff);
    }

}
