package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Float extends Number implements Comparable<Float> {
    public static final float POSITIVE_INFINITY = 1.0f / 0.0f;
    public static final float NEGATIVE_INFINITY = -1.0f / 0.0f;
    public static final float NaN = 0.0f / 0.0f;
    public static final float MAX_VALUE = 0x1.fffffeP+127f; // 3.4028235e+38f
    public static final float MIN_NORMAL = 0x1.0p-126f; // 1.17549435E-38f
    public static final float MIN_VALUE = 0x0.000002P-126f; // 1.4e-45f
    public static final int MAX_EXPONENT = 127;
    public static final int MIN_EXPONENT = -126;
    public static final int SIZE = 32;
    public static final  Class<Float> TYPE = null;

    public static String toString(float f) {
        throw new RuntimeException("skeleton method");
    }

    public static String toHexString(float f) {
        throw new RuntimeException("skeleton method");
    }

    public static Float valueOf(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Float valueOf(float f) {
        throw new RuntimeException("skeleton method");
    }

    public static float parseFloat( String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    static public boolean isNaN(float v) {
        throw new RuntimeException("skeleton method");
    }

    static public boolean isInfinite(float v) {
        throw new RuntimeException("skeleton method");
    }

    private final float value;

    public Float(float value) {
        throw new RuntimeException("skeleton method");
    }

    public Float(double value) {
        throw new RuntimeException("skeleton method");
    }

    public Float(String s) throws NumberFormatException {
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

    public static int floatToIntBits(float value) {
        throw new RuntimeException("skeleton method");
    }

    public static native int floatToRawIntBits(float value);
    public static native float intBitsToFloat(int bits);

    public int compareTo( Float anotherFloat) {
        throw new RuntimeException("skeleton method");
    }

    public static int compare(float f1, float f2) {
        throw new RuntimeException("skeleton method");
    }

    private static final long serialVersionUID = -2671257302660747028L;
}
