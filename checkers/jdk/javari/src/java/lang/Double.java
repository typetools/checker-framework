package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Double extends Number implements Comparable<Double> {
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;
    public static final double NaN = 0.0d / 0.0;
    public static final double MAX_VALUE = 0x1.fffffffffffffP+1023; // 1.7976931348623157e+308
    public static final double MIN_NORMAL = 0x1.0p-1022; // 2.2250738585072014E-308
    public static final double MIN_VALUE = 0x0.0000000000001P-1022; // 4.9e-324
    public static final int MAX_EXPONENT = 1023;
    public static final int MIN_EXPONENT = -1022;
    public static final int SIZE = 64;
    public static final Class<Double>  TYPE = null;

    public static String toString(double d) {
        throw new RuntimeException("skeleton method");
    }

    public static String toHexString(double d) {
        throw new RuntimeException("skeleton method");
    }

    public static Double valueOf(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Double valueOf(double d) {
        throw new RuntimeException("skeleton method");
    }

    public static double parseDouble(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    static public boolean isNaN(double v) {
        throw new RuntimeException("skeleton method");
    }

    static public boolean isInfinite(double v) {
        throw new RuntimeException("skeleton method");
    }

    private final double value;

    public Double(double value) {
        throw new RuntimeException("skeleton method");
    }

    public Double(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public boolean isNaN() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isInfinite() {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
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

    public int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public static long doubleToLongBits(double value) {
        throw new RuntimeException("skeleton method");
    }

    public static native long doubleToRawLongBits(double value);
    public static native double longBitsToDouble(long bits);

    public int compareTo(Double anotherDouble) {
        throw new RuntimeException("skeleton method");
    }

    public static int compare(double d1, double d2) {
        throw new RuntimeException("skeleton method");
    }

    private static final long serialVersionUID = -9172774392245257468L;
}
