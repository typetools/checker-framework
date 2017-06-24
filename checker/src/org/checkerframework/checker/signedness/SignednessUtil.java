package org.checkerframework.checker.signedness;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.checkerframework.checker.signedness.qual.Unsigned;

/**
 * Provides static utility methods for unsigned values. Some re-implement functionality in JDK 8,
 * making it available in earlier versions of Java. Others provide new functionality.
 */
public final class SignednessUtil {

    private SignednessUtil() {
        throw new Error("Do not instantiate");
    }

    /** Gets the unsigned int width of a Java Dimension. */
    @SuppressWarnings("signedness")
    public static @Unsigned int dimensionUnsignedWidth(Dimension dim) {
        return dim.width;
    }

    /** Gets the unsigned int height of a Java Dimension. */
    @SuppressWarnings("signedness")
    public static @Unsigned int dimensionUnsignedHeight(Dimension dim) {
        return dim.height;
    }

    /**
     * Wraps an unsigned byte array into a ByteBuffer. This method is a wrapper around {@link
     * java.nio.ByteBuffer#wrap(byte[]) wrap(byte[])}, but assumes that the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer wrapUnsigned(@Unsigned byte[] array) {
        return ByteBuffer.wrap(array);
    }

    /**
     * Wraps an unsigned byte array into a ByteBuffer. This method is a wrapper around {@link
     * java.nio.ByteBuffer#wrap(byte[], int, int) wrap(byte[], int, int)}, but assumes that the
     * input should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer wrapUnsigned(@Unsigned byte[] array, int offset, int length) {
        return ByteBuffer.wrap(array, offset, length);
    }

    /**
     * Gets an unsigned int from the ByteBuffer b. This method is a wrapper around {@link
     * java.nio.ByteBuffer#getInt() getInt()}, but assumes that the result should be interpreted as
     * unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned int getUnsignedInt(ByteBuffer b) {
        return b.getInt();
    }

    /**
     * Gets an unsigned short from the ByteBuffer b. This method is a wrapper around {@link
     * java.nio.ByteBuffer#getShort() getShort()}, but assumes that the result should be interpreted
     * as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned short getUnsignedShort(ByteBuffer b) {
        return b.getShort();
    }

    /**
     * Gets an unsigned byte from the ByteBuffer b. This method is a wrapper around {@link
     * java.nio.ByteBuffer#get() get()}, but assumes that the result should be interpreted as
     * unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned byte getUnsigned(ByteBuffer b) {
        return b.get();
    }

    /**
     * Gets an unsigned byte from the ByteBuffer b at i. This method is a wrapper around {@link
     * java.nio.ByteBuffer#get(int) get(int)}, but assumes that the result should be interpreted as
     * unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned byte getUnsigned(ByteBuffer b, int i) {
        return b.get(i);
    }

    /**
     * Populates an unsigned byte array from the ByteBuffer b at i with l bytes. This method is a
     * wrapper around {@link java.nio.ByteBuffer#get(byte[] bs, int, int) get(byte[], int, int)},
     * but assumes that the bytes should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer getUnsigned(ByteBuffer b, byte[] bs, int i, int l) {
        return b.get(bs, i, l);
    }

    /**
     * Places an unsigned byte into the ByteBuffer b. This method is a wrapper around {@link
     * java.nio.ByteBuffer#put(byte) put(byte)}, but assumes that the input should be interpreted as
     * unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsigned(ByteBuffer b, @Unsigned byte ubyte) {
        return b.put(ubyte);
    }

    /**
     * Places an unsigned byte into the ByteBuffer b at i. This method is a wrapper around {@link
     * java.nio.ByteBuffer#put(int, byte) put(int, byte)}, but assumes that the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsigned(ByteBuffer b, int i, @Unsigned byte ubyte) {
        return b.put(i, ubyte);
    }

    /**
     * Places an unsigned int into the IntBuffer b. This method is a wrapper around {@link
     * java.nio.IntBuffer#put(int) put(int)}, but assumes that the input should be interpreted as
     * unsigned.
     */
    @SuppressWarnings("signedness")
    public static IntBuffer putUnsigned(IntBuffer b, @Unsigned int uint) {
        return b.put(uint);
    }

    /**
     * Places an unsigned int into the IntBuffer b at i. This method is a wrapper around {@link
     * java.nio.IntBuffer#put(int, int) put(int, int)}, but assumes that the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static IntBuffer putUnsigned(IntBuffer b, int i, @Unsigned int uint) {
        return b.put(i, uint);
    }

    /**
     * Places an unsigned int array into the IntBuffer b. This method is a wrapper around {@link
     * java.nio.IntBuffer#put(int[]) put(int[])}, but assumes that the input should be interpreted
     * as unsigned.
     */
    @SuppressWarnings("signedness")
    public static IntBuffer putUnsigned(IntBuffer b, @Unsigned int[] uints) {
        return b.put(uints);
    }

    /**
     * Places an unsigned int array into the IntBuffer b at i with length l. This method is a
     * wrapper around {@link java.nio.IntBuffer#put(int[], int, int) put(int[], int, int)}, but
     * assumes that the input should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static IntBuffer putUnsigned(IntBuffer b, @Unsigned int[] uints, int i, int l) {
        return b.put(uints, i, l);
    }

    /**
     * Gets an unsigned int from the IntBuffer b at i. This method is a wrapper around {@link
     * java.nio.IntBuffer#get(int) get(int)}, but assumes that the output should be interpreted as
     * unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned int getUnsigned(IntBuffer b, int i) {
        return b.get(i);
    }

    /**
     * Places an unsigned short into the ByteBuffer b. This method is a wrapper around {@link
     * java.nio.ByteBuffer#putShort(short) putShort(short)}, but assumes that the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsignedShort(ByteBuffer b, @Unsigned short ushort) {
        return b.putShort(ushort);
    }

    /**
     * Places an unsigned short into the ByteBuffer b at i. This method is a wrapper around {@link
     * java.nio.ByteBuffer#putShort(int, short) putShort(int, short)}, but assumes that the input
     * should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsignedShort(ByteBuffer b, int i, @Unsigned short ushort) {
        return b.putShort(i, ushort);
    }

    /**
     * Places an unsigned int into the ByteBuffer b. This method is a wrapper around {@link
     * java.nio.ByteBuffer#putInt(int) putInt(int)}, but assumes that the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsignedInt(ByteBuffer b, @Unsigned int uint) {
        return b.putInt(uint);
    }

    /**
     * Places an unsigned int into the ByteBuffer b at i. This method is a wrapper around {@link
     * java.nio.ByteBuffer#putInt(int, int) putInt(int, int)}, but assumes that the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsignedInt(ByteBuffer b, int i, @Unsigned int uint) {
        return b.putInt(i, uint);
    }

    /**
     * Places an unsigned long into the ByteBuffer b at i. This method is a wrapper around {@link
     * java.nio.ByteBuffer#putLong(int, long) putLong(int, long)}, but assumes that the input should
     * be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static ByteBuffer putUnsignedLong(ByteBuffer b, int i, @Unsigned long ulong) {
        return b.putLong(i, ulong);
    }

    /**
     * Sets rgb of BufferedImage b given unsigned ints. This method is a wrapper around {@link
     * java.awt.image.BufferedImage#setRGB(int, int, int, int, int[], int, int) setRGB(int, int,
     * int, int, int[], int, int)}, but assumes that the input should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void setUnsignedRGB(
            BufferedImage b,
            int startX,
            int startY,
            int w,
            int h,
            @Unsigned int[] rgbArray,
            int offset,
            int scansize) {
        b.setRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    /**
     * Gets rgb of BufferedImage b as unsigned ints. This method is a wrapper around {@link
     * java.awt.image.BufferedImage#getRGB(int, int, int, int, int[], int, int) getRGB(int, int,
     * int, int, int[], int, int)}, but assumes that the output should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned int[] getUnsignedRGB(
            BufferedImage b,
            int startX,
            int startY,
            int w,
            int h,
            @Unsigned int[] rgbArray,
            int offset,
            int scansize) {
        return b.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    /**
     * Reads an unsigned byte from the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#readByte() readByte()}, but assumes the output should be interpreted
     * as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned byte readUnsignedByte(RandomAccessFile f) throws IOException {
        return f.readByte();
    }

    /**
     * Reads an unsigned char from the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#readChar() readChar()}, but assumes the output should be interpreted
     * as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned char readUnsignedChar(RandomAccessFile f) throws IOException {
        return f.readChar();
    }

    /**
     * Reads an unsigned short from the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#readShort() readShort()}, but assumes the output should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned short readUnsignedShort(RandomAccessFile f) throws IOException {
        return f.readShort();
    }

    /**
     * Reads an unsigned int from the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#readInt() readInt()}, but assumes the output should be interpreted
     * as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned int readUnsignedInt(RandomAccessFile f) throws IOException {
        return f.readInt();
    }

    /**
     * Reads an unsigned long from the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#readLong() readLong()}, but assumes the output should be interpreted
     * as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned long readUnsignedLong(RandomAccessFile f) throws IOException {
        return f.readLong();
    }

    /**
     * Reads up to {@code len} bytes of data from this file into an unsigned array of bytes. This
     * method is a wrapper around {@link java.io.RandomAccessFile#read(byte[], int, int)
     * read(byte[], int, int)}, but assumes the output should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static int readUnsigned(RandomAccessFile f, @Unsigned byte b[], int off, int len)
            throws IOException {
        return f.read(b, off, len);
    }

    /**
     * Reads a file fully into an unsigned byte array. This method is a wrapper around {@link
     * java.io.RandomAccessFile#readFully(byte[]) readFully(byte[])}, but assumes the output should
     * be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void readFullyUnsigned(RandomAccessFile f, @Unsigned byte b[])
            throws IOException {
        f.readFully(b);
    }

    /**
     * Writes len unsigned bytes to the RandomAccessFile f at offset off. This method is a wrapper
     * around {@link java.io.RandomAccessFile#write(byte[], int, int) write(byte[], int, int)}, but
     * assumes the input should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void writeUnsigned(RandomAccessFile f, @Unsigned byte[] bs, int off, int len)
            throws IOException {
        f.write(bs, off, len);
    }

    /**
     * Writes an unsigned byte to the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#writeByte(int) writeByte(int)}, but assumes the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void writeUnsignedByte(RandomAccessFile f, @Unsigned byte b) throws IOException {
        f.writeByte(toUnsignedInt(b));
    }

    /**
     * Writes an unsigned char to the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#writeChar(int) writeChar(int)}, but assumes the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void writeUnsignedChar(RandomAccessFile f, @Unsigned char c) throws IOException {
        f.writeChar(toUnsignedInt(c));
    }

    /**
     * Writes an unsigned short to the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#writeShort(int) writeShort(int)}, but assumes the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void writeUnsignedShort(RandomAccessFile f, @Unsigned short s)
            throws IOException {
        f.writeShort(toUnsignedInt(s));
    }

    /**
     * Writes an unsigned byte to the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#writeInt(int) writeInt(int)}, but assumes the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void writeUnsignedInt(RandomAccessFile f, @Unsigned int i) throws IOException {
        f.writeInt(i);
    }

    /**
     * Writes an unsigned byte to the RandomAccessFile f. This method is a wrapper around {@link
     * java.io.RandomAccessFile#writeLong(long) writeLong(long)}, but assumes the input should be
     * interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void writeUnsignedLong(RandomAccessFile f, @Unsigned long l) throws IOException {
        f.writeLong(l);
    }

    /**
     * Gets an array of unsigned bytes from the ByteBuffer b and stores them in the array bs. This
     * method is a wrapper around {@link java.nio.ByteBuffer#get(byte[]) get(byte[])}, but assumes
     * that the array of bytes should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
        b.get(bs);
    }

    /**
     * Compares two unsigned longs x and y.
     *
     * <p>This is a reimplementation of Java 8's {@code Long.compareUnsigned(long, long)}.
     *
     * @return a negative number iff x {@literal <} y, a positive number iff x {@literal >} y, and
     *     zero iff x == y.
     */
    @SuppressWarnings("signedness")
    public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
        // Java 8 version: return Long.compareUnsigned(x, y);
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

    /**
     * Compares two unsigned ints x and y.
     *
     * <p>This is a reimplementation of Java 8's {@code Integer.compareUnsigned(int, int)}.
     *
     * @return a negative number iff x {@literal <} y, a positive number iff x {@literal >} y, and
     *     zero iff x == y.
     */
    @SuppressWarnings("signedness")
    public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
        // Java 8 version: return Integer.compareUnsigned(x, y);
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    /**
     * Compares two unsigned shorts x and y.
     *
     * @return a negative number iff x {@literal <} y, a positive number iff x {@literal >} y, and
     *     zero iff x == y.
     */
    @SuppressWarnings("signedness")
    public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
        // Java 8 version: return Integer.compareUnsigned(Short.toUnsignedInt(x), Short.toUnsignedInt(y));
        return compareUnsigned(toUnsignedInt(x), toUnsignedInt(y));
    }

    /**
     * Compares two unsigned bytes x and y.
     *
     * @return a negative number iff x {@literal <} y, a positive number iff x {@literal >} y, and
     *     zero iff x == y.
     */
    @SuppressWarnings("signedness")
    public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
        // Java 8 version: return Integer.compareUnsigned(Byte.toUnsignedInt(x), Byte.toUnsignedInt(y));
        return compareUnsigned(toUnsignedInt(x), toUnsignedInt(y));
    }

    /**
     * Produces a string representation of the unsigned long l.
     *
     * <p>This is a reimplementation of Java 8's {@code Long.toUnsignedString(long)}.
     */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned long l) {
        // Java 8 version: return Long.toUnsignedString(l);
        return toUnsignedBigInteger(l).toString();
    }

    /**
     * Produces a string representation of the unsigned long l in base radix.
     *
     * <p>This is a reimplementation of Java 8's {@code Long.toUnsignedString(long, int)}.
     */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned long l, int radix) {
        // Java 8 version: return Long.toUnsignedString(l, radix);
        return toUnsignedBigInteger(l).toString(radix);
    }

    /**
     * Produces a string representation of the unsigned int i.
     *
     * <p>This is a reimplementation of Java 8's {@code Integer.toUnsignedString(int)}.
     */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned int i) {
        // Java 8 version: return Integer.toUnsignedString(i);
        return Long.toString(toUnsignedLong(i));
    }

    /**
     * Produces a string representation of the unsigned int i in base radix.
     *
     * <p>This is a reimplementation of Java 8's {@code Integer.toUnsignedString(int, int)}.
     */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned int i, int radix) {
        // Java 8 version: return Integer.toUnsignedString(i, radix);
        return Long.toString(toUnsignedLong(i), radix);
    }

    /** Produces a string representation of the unsigned short s. */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned short s) {
        // Java 8 version: return Integer.toUnsignedString(Short.toUnsignedInt(s));
        return Long.toString(toUnsignedLong(s));
    }

    /** Produces a string representation of the unsigned short s in base radix. */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned short s, int radix) {
        // Java 8 version: return Integer.toUnsignedString(Short.toUnsignedInt(s), radix);
        return Long.toString(toUnsignedLong(s), radix);
    }

    /** Produces a string representation of the unsigned byte b. */
    @SuppressWarnings("signedness")
    public static String toUnsignedString(@Unsigned byte b) {
        // Java 8 version: return Integer.toUnsignedString(Byte.toUnsignedInt(b));
        return Long.toString(toUnsignedLong(b));
    }

    /** Produces a string representation of the unsigned byte b in base radix. */
    @SuppressWarnings("signedness")
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
    @SuppressWarnings("signedness")
    private static @Unsigned BigInteger toUnsignedBigInteger(@Unsigned long l) {
        // Java 8 version: return Long.toUnsignedBigInteger(l);
        if (l >= 0L) {
            return BigInteger.valueOf(l);
        } else {
            int upper = (int) (l >>> 32);
            int lower = (int) l;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(toUnsignedLong(upper)))
                    .shiftLeft(32)
                    .add(BigInteger.valueOf(toUnsignedLong(lower)));
        }
    }

    /**
     * Returns an unsigned long representing the same value as an unsigned int.
     *
     * <p>This is a reimplementation of Java 8's {@code Integer.toUnsignedLong(int)}.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned int i) {
        // Java 8 version: Integer.toUnsignedLong(i)
        return ((long) i) & 0xffffffffL;
    }

    /** Returns an unsigned long representing the same value as an unsigned short. */
    public static @Unsigned long toUnsignedLong(@Unsigned short s) {
        return ((long) s) & 0xffffL;
    }

    /** Returns an unsigned int representing the same value as an unsigned short. */
    public static @Unsigned int toUnsignedInt(@Unsigned short s) {
        return ((int) s) & 0xffff;
    }

    /** Returns an unsigned long representing the same value as an unsigned byte. */
    public static @Unsigned long toUnsignedLong(@Unsigned byte b) {
        return ((long) b) & 0xffL;
    }

    /** Returns an unsigned int representing the same value as an unsigned byte. */
    public static @Unsigned int toUnsignedInt(@Unsigned byte b) {
        return ((int) b) & 0xff;
    }

    /** Returns an unsigned short representing the same value as an unsigned byte. */
    public static @Unsigned short toUnsignedShort(@Unsigned byte b) {
        return (short) (((int) b) & 0xff);
    }

    /** Returns an unsigned long representing the same value as an unsigned char. */
    public static @Unsigned long toUnsignedLong(@Unsigned char c) {
        return ((long) c) & 0xffL;
    }

    /** Returns an unsigned int representing the same value as an unsigned char. */
    public static @Unsigned int toUnsignedInt(@Unsigned char c) {
        return ((int) c) & 0xff;
    }

    /** Returns an unsigned short representing the same value as an unsigned char. */
    public static @Unsigned short toUnsignedShort(@Unsigned char c) {
        return (short) (((int) c) & 0xff);
    }

    /** Returns a float representing the same value as the unsigned byte. */
    public static float toFloat(@Unsigned byte b) {
        return toUnsignedBigInteger(toUnsignedLong(b)).floatValue();
    }

    /** Returns a float representing the same value as the unsigned short. */
    public static float toFloat(@Unsigned short s) {
        return toUnsignedBigInteger(toUnsignedLong(s)).floatValue();
    }

    /** Returns a float representing the same value as the unsigned int. */
    public static float toFloat(@Unsigned int i) {
        return toUnsignedBigInteger(toUnsignedLong(i)).floatValue();
    }

    /** Returns a float representing the same value as the unsigned long. */
    public static float toFloat(@Unsigned long l) {
        return toUnsignedBigInteger(l).floatValue();
    }

    /** Returns a double representing the same value as the unsigned byte. */
    public static double toDouble(@Unsigned byte b) {
        return toUnsignedBigInteger(toUnsignedLong(b)).doubleValue();
    }

    /** Returns a double representing the same value as the unsigned short. */
    public static double toDouble(@Unsigned short s) {
        return toUnsignedBigInteger(toUnsignedLong(s)).doubleValue();
    }

    /** Returns a double representing the same value as the unsigned int. */
    public static double toDouble(@Unsigned int i) {
        return toUnsignedBigInteger(toUnsignedLong(i)).doubleValue();
    }

    /** Returns a double representing the same value as the unsigned long. */
    public static double toDouble(@Unsigned long l) {
        return toUnsignedBigInteger(l).doubleValue();
    }

    /** Returns an unsigned byte representing the same value as the float. */
    @SuppressWarnings("signedness")
    public static @Unsigned byte byteFromFloat(float f) {
        assert f >= 0;
        return (byte) f;
    }

    /** Returns an unsigned short representing the same value as the float. */
    @SuppressWarnings("signedness")
    public static @Unsigned short shortFromFloat(float f) {
        assert f >= 0;
        return (short) f;
    }

    /** Returns an unsigned int representing the same value as the float. */
    @SuppressWarnings("signedness")
    public static @Unsigned int intFromFloat(float f) {
        assert f >= 0;
        return (int) f;
    }

    /** Returns an unsigned long representing the same value as the float. */
    @SuppressWarnings("signedness")
    public static @Unsigned long longFromFloat(float f) {
        assert f >= 0;
        return (long) f;
    }

    /** Returns an unsigned byte representing the same value as the double. */
    @SuppressWarnings("signedness")
    public static @Unsigned byte byteFromDouble(double d) {
        assert d >= 0;
        return (byte) d;
    }

    /** Returns an unsigned short representing the same value as the double. */
    @SuppressWarnings("signedness")
    public static @Unsigned short shortFromDouble(double d) {
        assert d >= 0;
        return (short) d;
    }

    /** Returns an unsigned int representing the same value as the double. */
    @SuppressWarnings("signedness")
    public static @Unsigned int intFromDouble(double d) {
        assert d >= 0;
        return (int) d;
    }

    /** Returns an unsigned long representing the same value as the double. */
    @SuppressWarnings("signedness")
    public static @Unsigned long longFromDouble(double d) {
        assert d >= 0;
        return (long) d;
    }
}
