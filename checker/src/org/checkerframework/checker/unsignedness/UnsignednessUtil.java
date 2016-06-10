package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Provides static utility functions for unsigned integers,
 * similar to ones in the JDK that work on signed integers.
 */
public final class UnsignednessUtil {

    /**
     * Gets an Unsigned short from the ByteBuffer b. Wraps {@link java.nio.ByteBuffer#getShort() getShort()}.
     *
     * This should be used when one would normally use {@link java.nio.ByteBuffer#getShort() getShort()},
     * but the result should be interpreted as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static @Unsigned short getUnsignedShort(ByteBuffer b) {
        return b.getShort();
    }

    /**
     * Gets an Unsigned byte from the ByteBuffer b. Wraps {@link java.nio.ByteBuffer#get() get()}.
     *
     * This should be used when one would normally use {@link java.nio.ByteBuffer#get() get()},
     * but the result should be interpreted as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static @Unsigned byte getUnsigned(ByteBuffer b) {
        return b.get();
    }

    /**
     * Gets an array of Unsigned byte[] from the ByteBuffer b and stores in
     * the array bs. Wraps {@link java.nio.ByteBuffer#get(byte[]) get(byte[])}.
     *
     * This should be used when one would normally use {@link java.nio.ByteBuffer#get(byte[]) get(byte[])},
     * but the array of bytes should be interpreted as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static void getUnsigned(ByteBuffer b, @Unsigned byte[] bs) {
        b.get(bs);
    }

    /**
     * Compares two Unsigned longs x and y. This returns a negative number iff
     * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
     * x == y. Annotates Long::compareUnsigned(long, long).
     *
     * This should be used when one would normally compare two longs with {@literal <}
     * or {@literal >} but cannot as the longs are Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

    /**
     * Compares two Unsigned ints x and y. This returns a negative number iff
     * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
     * x == y. Annotates Integer::compareUnsigned(int, int).
     *
     * This should be used when one would normally compare two ints with {@literal <}
     * or {@literal >} but cannot as the ints are Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    /**
     * Compares two Unsigned shorts x and y. This returns a negative number iff
     * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
     * x == y. Extends Integer::compareUnsigned(int, int).
     * to act on short arguments.
     *
     * This should be used when one would normally compare two shorts with {@literal <}
     * or {@literal >} but cannot as the shorts are Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
        return compareUnsigned(
                                                     toUnsignedInt(x),
                                                     toUnsignedInt(y)
                                                     );
    }

    /**
     * Compares two Unsigned bytes x and y. This returns a negative number iff
     * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
     * x == y. Extends Integer::compareUnsigned(int, int).
     * to act on byte arguments.
     *
     * This should be used when one would normally compare two bytes with {@literal <}
     * or {@literal >} but cannot as the bytes are Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
        return compareUnsigned(toUnsignedInt(x), toUnsignedInt(y));
    }

    /**
     * Produces a string representation of the Unsigned long l.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned long and cannot rely on Java to interpret the long as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned long l) {
        return toUnsignedBigInteger(l).toString();
    }

    /**
     * Produces a string representation of the Unsigned long l in base radix.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned long and cannot rely on Java to interpret the long as Unsigned, but also
     * wants to represent it in a base other than ten.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned long l, int radix) {
        return toUnsignedBigInteger(l).toString(radix);
    }

    /**
     * Produces a string representation of the Unsigned int i.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned int and cannot rely on Java to interpret the int as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned int i) {
        return Long.toString(toUnsignedLong(i));
    }

    /**
     * Produces a string representation of the Unsigned int i in base radix.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned int and cannot rely on Java to interpret the int as Unsigned, but also
     * wants to represent it in a base other than ten.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned int i, int radix) {
        return Long.toString(toUnsignedLong(i), radix);
    }

    /**
     * Produces a string representation of the Unsigned short s.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned short and cannot rely on Java to interpret the short as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned short s) {
        return Long.toString(toUnsignedLong(s));
    }

    /**
     * Produces a string representation of the Unsigned short s in base radix.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned short and cannot rely on Java to interpret the short as Unsigned, but also
     * wants to represent it in a base other than ten.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned short s, int radix) {
        return Long.toString(toUnsignedLong(s), radix);
    }

    /**
     * Produces a string representation of the Unsigned byte b.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned byte and cannot rely on Java to interpret the byte as Unsigned.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned byte b) {
        return Long.toString(toUnsignedLong(b));
    }

    /**
     * Produces a string representation of the Unsigned byte b in base radix.
     *
     * This should be used when one needs to get the string representation of an
     * Unsigned byte and cannot rely on Java to interpret the byte as Unsigned, but also
     * wants to represent it in a base other than ten.
     */
    @SuppressWarnings("unsignedness")
    public static String toUnsignedString(@Unsigned byte b, int radix) {
        return Long.toString(toUnsignedLong(b), radix);
    }

    /*
     * Upcasts an Unsigned long into an Unsigned BigInteger.
     *
     * Copies the private method Long::toUnsignedBigInteger(long).
     */
    @SuppressWarnings("unsignedness")
    private static @Unsigned BigInteger toUnsignedBigInteger(@Unsigned long i) {
        if (i >= 0L)
            return BigInteger.valueOf(i);
        else {
            int upper = (int) (i >>> 32);
            int lower = (int) i;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(toUnsignedLong(upper))).shiftLeft(32).
                add(BigInteger.valueOf(toUnsignedLong(lower)));
        }
    }

    /**
     * Upcasts an Unsigned int into an Unsigned long.
     * Annotates Integer::toUnsignedLong(int).
     *
     * This should be used only when you would normally cast an int to a long,
     * but cannot rely on Java to interpret the int as Unsigned.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned int i) {
        return ((long) i) & 0xffffffffL;
    }

    /**
     * Upcasts an Unsigned short into an Unsigned long.
     * Annotates Short::toUnsignedLong(short).
     *
     * This should be used only when you would normally cast a short to a long,
     * but cannot rely on Java to interpret the short as Unsigned.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned short s) {
        return ((long) s) & 0xffffL;
    }

    /**
     * Upcasts an Unsigned short into an Unsigned int.
     * Annotates Short::toUnsignedInt(short).
     *
     * This should be used only when you would normally cast a short to an int,
     * but cannot rely on Java to interpret the short as Unsigned.
     */
    public static @Unsigned int toUnsignedInt(@Unsigned short s) {
        return ((int) s) & 0xffff;
    }

    /**
     * Upcasts an Unsigned byte into an Unsigned long.
     * Annotates Byte::toUnsignedLong(byte).
     *
     * This should be used only when you would normally cast a byte to a long,
     * but cannot rely on Java to interpret the byte as Unsigned.
     */
    public static @Unsigned long toUnsignedLong(@Unsigned byte b) {
        return ((long) b) & 0xffL;
    }

    /**
     * Upcasts an Unsigned byte into an Unsigned int.
     * Annotates Byte::toUnsignedInt(byte).
     *
     * This should be used only when you would normally cast a byte to an int,
     * but cannot rely on Java to interpret the byte as Unsigned.
     */
    public static @Unsigned int toUnsignedInt(@Unsigned byte b) {
        return ((int) b) & 0xff;
    }

    /**
     * Upcasts an Unsigned byte into an Unsigned short.
     *
     * This should be used only when you would normally cast a byte to a short,
     * but cannot rely on Java to interpret the byte as Unsigned.
     */
    public static @Unsigned short toUnsignedShort(@Unsigned byte b) {
        return (short) (((int) b) & 0xff);
    }

    //////////////////////////////////////////////////////////////////////////
    /// JAVA 8 RELIANT CODE //////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    /*
     * The code below is commented out because it relies heavily on methods
     * provided in Java 8. It should be uncommented and replace the corresponding
     * code above when the Checker Framework no longer supports Java 7
     */

    // /**
    //  * Compares two Unsigned longs x and y. This returns a negative number iff
    //  * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
    //  * x == y. Wraps {@link Long#compareUnsigned(long, long) compareUnsigned(long, long)}.
    //  *
    //  * This should be used when one would normally compare two longs with {@literal <}
    //  * or {@literal >} but cannot as the longs are Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static int compareUnsigned(@Unsigned long x, @Unsigned long y) {
    //      return Long.compareUnsigned(x, y);
    //  }

    // /**
    //  * Compares two Unsigned ints x and y. This returns a negative number iff
    //  * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
    //  * x == y. Wraps {@link Integer#compareUnsigned(int, int) compareUnsigned(int, int)}.
    //  *
    //  * This should be used when one would normally compare two ints with {@literal <}
    //  * or {@literal >} but cannot as the ints are Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static int compareUnsigned(@Unsigned int x, @Unsigned int y) {
    //      return Integer.compareUnsigned(x, y);
    //  }

    // /**
    //  * Compares two Unsigned shorts x and y. This returns a negative number iff
    //  * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
    //  * x == y. Extends {@link Integer#compareUnsigned(int, int) compareUnsigned(int, int)}
    //  * to act on short arguments.
    //  *
    //  * This should be used when one would normally compare two shorts with {@literal <}
    //  * or {@literal >} but cannot as the shorts are Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static int compareUnsigned(@Unsigned short x, @Unsigned short y) {
    //      return Integer.compareUnsigned(
    //              Short.toUnsignedInt(x),
    //              Short.toUnsignedInt(y)
    //      );
    //  }

    // /**
    //  * Compares two Unsigned bytes x and y. This returns a negative number iff
    //  * x {@literal <} y, a positive number iff x {@literal >} y, and zero iff
    //  * x == y. Extends {@link Integer#compareUnsigned(int, int) compareUnsigned(int, int)}
    //  * to act on byte arguments.
    //  *
    //  * This should be used when one would normally compare two bytes with {@literal <}
    //  * or {@literal >} but cannot as the bytes are Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static int compareUnsigned(@Unsigned byte x, @Unsigned byte y) {
    //      return Integer.compareUnsigned(
    //              Byte.toUnsignedInt(x),
    //              Byte.toUnsignedInt(y)
    //      );
    //  }

    // /**
    //  * Produces a string representation of the Unsigned long l. Wraps {@link Long#toUnsignedString(long) toUnsignedString(long)}.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned long and cannot rely on Java to interpret the long as Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned long l) {
    //      return Long.toUnsignedString(l);
    //  }

    // /**
    //  * Produces a string representation of the Unsigned long l in base radix. Wraps {@link Long#toUnsignedString(long, int) toUnsignedString(long, int)}.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned long and cannot rely on Java to interpret the long as Unsigned, but also
    //  * wants to represent it in a base other than ten.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned long l, int radix) {
    //      return Long.toUnsignedString(l, radix);
    //  }

    // /**
    //  * Produces a string representation of the Unsigned int i. Wraps {@link Integer#toUnsignedString(int) toUnsignedString(int)}.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned int and cannot rely on Java to interpret the int as Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned int i) {
    //      return Integer.toUnsignedString(i);
    //  }

    // /**
    //  * Produces a string representation of the Unsigned int i in base radix. Wraps {@link Integer#toUnsignedString(int, int) toUnsignedString(int, int)}.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned int and cannot rely on Java to interpret the int as Unsigned, but also
    //  * wants to represent it in a base other than ten.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned int i, int radix) {
    //      return Integer.toUnsignedString(i, radix);
    //  }

    // /**
    //  * Produces a string representation of the Unsigned short s. Extends {@link Integer#toUnsignedString(int) toUnsignedString(int)}
    //  * to operate on a short argument.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned short and cannot rely on Java to interpret the short as Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned short s) {
    //      return Integer.toUnsignedString(Short.toUnsignedInt(s));
    //  }

    // /**
    //  * Produces a string representation of the Unsigned short s in base radix. Extends {@link Integer#toUnsignedString(int, int) toUnsignedString(int, int)}
    //  * to operate on a short argument.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned short and cannot rely on Java to interpret the short as Unsigned, but also
    //  * wants to represent it in a base other than ten.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned short s, int radix) {
    //      return Integer.toUnsignedString(Short.toUnsignedInt(s), radix);
    //  }

    // /**
    //  * Produces a string representation of the Unsigned byte b. Extends {@link Integer#toUnsignedString(int) toUnsignedString(int)}
    //  * to operate on a byte argument.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned byte and cannot rely on Java to interpret the byte as Unsigned.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned byte b) {
    //      return Integer.toUnsignedString(Byte.toUnsignedInt(b));
    //  }

    // /**
    //  * Produces a string representation of the Unsigned byte b in base radix.
    //  * Extends {@link Integer#toUnsignedString(int, int) toUnsignedString(int, int)}
    //  * to operate on a byte argument.
    //  *
    //  * This should be used when one needs to get the string representation of an
    //  * Unsigned byte and cannot rely on Java to interpret the byte as Unsigned, but also
    //  * wants to represent it in a base other than ten.
    //  */
    //  @SuppressWarnings("unsignedness")
    //  public static String toUnsignedString(@Unsigned byte b, int radix) {
    //      return Integer.toUnsignedString(Byte.toUnsignedInt(b), radix);
    //  }
}
