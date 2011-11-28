package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Long extends Number implements Comparable<Long> {
    public static final long MIN_VALUE = 0x8000000000000000L;
    public static final long MAX_VALUE = 0x7fffffffffffffffL;
    public static final  Class<Long>    TYPE = null;

    public static String toString(long i, int radix) {
        throw new RuntimeException("skeleton method");
    }

    public static String toHexString(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static String toOctalString(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static String toBinaryString(long i) {
        throw new RuntimeException("skeleton method");
    }

    private static String toUnsignedString(long i, int shift) {
        throw new RuntimeException("skeleton method");
    }

    public static String toString(long i) {
        throw new RuntimeException("skeleton method");
    }

    static void getChars(long i, int index, char[] buf) {
        throw new RuntimeException("skeleton method");
    }

    static int stringSize(long x) {
        throw new RuntimeException("skeleton method");
    }

    public static long parseLong(String s, int radix)
              throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static long parseLong(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Long valueOf(String s, int radix) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Long valueOf( String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    private static class LongCache {
    private LongCache(){ throw new RuntimeException("skeleton method"); }

    static final Long cache[] = new Long[-(-128) + 127 + 1];
    }

    public static Long valueOf(long l) {
        throw new RuntimeException("skeleton method");
    }

    public static Long decode(String nm) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    private final long value;

    public Long(long value) {
        throw new RuntimeException("skeleton method");
    }

    public Long(String s) throws NumberFormatException {
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

    public static Long getLong(String nm) {
        throw new RuntimeException("skeleton method");
    }

    public static Long getLong(String nm, long val) {
        throw new RuntimeException("skeleton method");
    }

    public static Long getLong(String nm, Long val) {
        throw new RuntimeException("skeleton method");
    }

    public int compareTo(Long anotherLong) {
        throw new RuntimeException("skeleton method");
    }

    public static final int SIZE = 64;

    public static long highestOneBit(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static long lowestOneBit(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static int numberOfLeadingZeros(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static int numberOfTrailingZeros(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static int bitCount(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static long rotateLeft(long i, int distance) {
        throw new RuntimeException("skeleton method");
    }

    public static long rotateRight(long i, int distance) {
        throw new RuntimeException("skeleton method");
    }

    public static long reverse(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static int signum(long i) {
        throw new RuntimeException("skeleton method");
    }

    public static long reverseBytes(long i) {
        throw new RuntimeException("skeleton method");
    }

    private static final long serialVersionUID = 4290774380558885855L;
}
