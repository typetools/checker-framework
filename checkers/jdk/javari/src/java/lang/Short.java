package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Short extends Number implements Comparable<Short> {

    public static final short   MIN_VALUE = -32768;
    public static final short   MAX_VALUE = 32767;
    public static final  Class<Short>   TYPE = null;

    public static String toString(short s) {
        throw new RuntimeException("skeleton method");
    }

    public static short parseShort(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static short parseShort(String s, int radix) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Short valueOf(String s, int radix) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Short valueOf(String s) throws NumberFormatException {
         throw new RuntimeException("skeleton method");
    }

    private static class ShortCache {
    private ShortCache(){ throw new RuntimeException("skeleton method"); }

    static final Short cache[] = new Short[-(-128) + 127 + 1];
    }

    public static Short valueOf(short s) {
        throw new RuntimeException("skeleton method");
    }

    public static Short decode(String nm) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    private final short value;

    public Short(short value) {
        throw new RuntimeException("skeleton method");
    }

    public Short(String s) throws NumberFormatException {
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

    public int compareTo(Short anotherShort) {
        throw new RuntimeException("skeleton method");
    }

    public static final int SIZE = 16;

    public static short reverseBytes(short i) {
        throw new RuntimeException("skeleton method");
    }

    private static final long serialVersionUID = 7515723908773894738L;
}
