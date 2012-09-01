package java.lang;
import checkers.javari.quals.*;

public final @ReadOnly class Integer extends Number implements Comparable<Integer> {
    public static final int   MIN_VALUE = 0x80000000;
    public static final int   MAX_VALUE = 0x7fffffff;
    public static final Class<Integer>  TYPE = null;

    final static char[] digits = {
    '0' , '1' , '2' , '3' , '4' , '5' ,
    '6' , '7' , '8' , '9' , 'a' , 'b' ,
    'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
    'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
    'o' , 'p' , 'q' , 'r' , 's' , 't' ,
    'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };

    public static String toString(int i, int radix) {
        throw new RuntimeException("skeleton method");
    }

    public static String toHexString(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static String toOctalString(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static String toBinaryString(int i) {
        throw new RuntimeException("skeleton method");
    }

    private static String toUnsignedString(int i, int shift) {
        throw new RuntimeException("skeleton method");
    }

    final static char [] DigitTens = {
    '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
    '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
    '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
    '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
    '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
    '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
    '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
    '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
    '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
    '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    final static char [] DigitOnes = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    public static String toString(int i) {
        throw new RuntimeException("skeleton method");
    }

    static void getChars(int i, int index, char[] buf) {
        throw new RuntimeException("skeleton method");
    }

    final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
                                      99999999, 999999999, Integer.MAX_VALUE };

    static int stringSize(int x) {
        throw new RuntimeException("skeleton method");
    }

    public static int parseInt(String s, int radix)
        throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static int parseInt(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Integer valueOf(String s, int radix) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public static Integer valueOf(String s) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    private static class IntegerCache {
        private IntegerCache() { throw new RuntimeException("skeleton method");}

        static final Integer cache[] = new Integer[-(-128) + 127 + 1];
    }

    public static Integer valueOf(int i) {
        throw new RuntimeException("skeleton method");
    }

    private final int value;

    public Integer(int value) {
        throw new RuntimeException("skeleton method");
    }

    public Integer(String s) throws NumberFormatException {
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

    public static Integer getInteger(String nm) {
        throw new RuntimeException("skeleton method");
    }

    public static Integer getInteger(String nm, int val) {
        throw new RuntimeException("skeleton method");
    }

    public static Integer getInteger(String nm, Integer val) {
        throw new RuntimeException("skeleton method");
    }

    public static Integer decode(String nm) throws NumberFormatException {
        throw new RuntimeException("skeleton method");
    }

    public int compareTo(Integer anotherInteger) {
        throw new RuntimeException("skeleton method");
    }

    public static final int SIZE = 32;

    public static int highestOneBit(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int lowestOneBit(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int numberOfLeadingZeros(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int numberOfTrailingZeros(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int bitCount(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int rotateLeft(int i, int distance) {
        throw new RuntimeException("skeleton method");
    }

    public static int rotateRight(int i, int distance) {
        throw new RuntimeException("skeleton method");
    }

    public static int reverse(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int signum(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static int reverseBytes(int i) {
        throw new RuntimeException("skeleton method");
    }

    private static final long serialVersionUID = 1360826667806852920L;
}
