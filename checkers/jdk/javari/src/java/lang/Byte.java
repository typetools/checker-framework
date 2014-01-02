package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Byte extends Number implements Comparable<Byte> {

    public static final byte   MIN_VALUE = -128;
    public static final byte   MAX_VALUE = 127;
    public static final Class<Byte>    TYPE = null;

    public static String toString(byte b) {
        throw new RuntimeException("skeleton method");
    }

    private static class ByteCache {
    private ByteCache(){throw new RuntimeException("skeleton method");}

    static final Byte cache[] = new Byte[-(-128) + 127 + 1];
    }

    public static Byte valueOf(byte b) {
        throw new RuntimeException("skeleton method");
    }

    public static byte parseByte(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static byte parseByte(String s, int radix)
    throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Byte valueOf(String s, int radix)
    throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Byte valueOf(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Byte decode(String nm) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    private final byte value;

    public Byte(byte value) {
        throw new RuntimeException("skeleton method");
    }

    public Byte( String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public byte byteValue() {
        throw new RuntimeException("skeleton method");
    }

    public short shortValue() {
        throw new RuntimeException("skeleton method");
    }

    public int intValue() {
        throw new RuntimeException("skeleton method");
    }

    public long longValue() {
        throw new RuntimeException("skeleton method");
    }

    public float floatValue() {
        throw new RuntimeException("skeleton method");
    }

    public double doubleValue() {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public int compareTo(Byte anotherByte) {
        throw new RuntimeException("skeleton method");
    }

    public static final int SIZE = 8;
    private static final long serialVersionUID = -7183698231559129828L;
}
