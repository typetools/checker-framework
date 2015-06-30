package org.checkerframework.common.value;

/**
 * This file contains methods that simulate the functions of the Binary and
 * Unary operators in java (e.g. +, -, &lt;, ++, etc.). The purpose of doing this
 * is to streamline the code for evaluating operators in
 * ValueAnnotatedTypeFactory and make it more similar to the code for evaluating
 * methods as well. The naming is the same name as the operator has when
 * BinaryTree.getKind() is called.
 *
 */
public class Operators {

    public static boolean LOGICAL_COMPLEMENT(Boolean a) {
        return !a;
    }
    public static boolean LOGICAL_COMPLEMENT(boolean a) {
        return !a;
    }


    public static long BITWISE_COMPLEMENT(Long a) {
        return ~a;
    }

    public static int BITWISE_COMPLEMENT(Integer a) {
        return ~a;
    }

    /**
     *
     * NOTE ON POSTFIX OPERATORS: Because the postfix increment/decrement would
     * take place after the value is returned, the method does not actually
     * perform a postfix increment/decrement; this is correctly handled by the
     * org.checkerframework.dataflow analysis elsewhere.
     *
     */
    public static byte POSTFIX_INCREMENT(Byte a) {
        return a;
    }

    public static short POSTFIX_INCREMENT(Short a) {
        return a;
    }

    public static char POSTFIX_INCREMENT(Character a) {
        return a;
    }

    public static int POSTFIX_INCREMENT(Integer a) {
        return a;
    }

    public static long POSTFIX_INCREMENT(Long a) {
        return a;
    }

    public static float POSTFIX_INCREMENT(Float a) {
        return a;
    }

    public static double POSTFIX_INCREMENT(Double a) {
        return a;
    }

    public static byte PREFIX_INCREMENT(Byte a) {
        return ++a;
    }

    public static short PREFIX_INCREMENT(Short a) {
        return ++a;
    }

    public static char PREFIX_INCREMENT(Character a) {
        return ++a;
    }

    public static int PREFIX_INCREMENT(Integer a) {
        return ++a;
    }

    public static long PREFIX_INCREMENT(Long a) {
        return ++a;
    }

    public static float PREFIX_INCREMENT(Float a) {
        return ++a;
    }

    public static double PREFIX_INCREMENT(Double a) {
        return ++a;
    }

    public static byte POSTFIX_DECREMENT(Byte a) {
        return a;
    }

    public static short POSTFIX_DECREMENT(Short a) {
        return a;
    }

    public static char POSTFIX_DECREMENT(Character a) {
        return a;
    }

    public static int POSTFIX_DECREMENT(Integer a) {
        return a;
    }

    public static long POSTFIX_DECREMENT(Long a) {
        return a;
    }

    public static float POSTFIX_DECREMENT(Float a) {
        return a;
    }

    public static double POSTFIX_DECREMENT(Double a) {
        return a;
    }

    public static byte PREFIX_DECREMENT(Byte a) {
        return --a;
    }

    public static short PREFIX_DECREMENT(Short a) {
        return --a;
    }

    public static char PREFIX_DECREMENT(Character a) {
        return --a;
    }

    public static int PREFIX_DECREMENT(Integer a) {
        return --a;
    }

    public static long PREFIX_DECREMENT(Long a) {
        return --a;
    }

    public static float PREFIX_DECREMENT(Float a) {
        return --a;
    }

    public static double PREFIX_DECREMENT(Double a) {
        return --a;
    }

    public static int UNARY_MINUS(Integer a) {
        return -a;
    }

    public static long UNARY_MINUS(Long a) {
        return -a;
    }

    public static float UNARY_MINUS(Float a) {
        return -a;
    }

    public static double UNARY_MINUS(Double a) {
        return -a;
    }

    public static int UNARY_PLUS(Integer a) {
        return +a;
    }

    public static long UNARY_PLUS(Long a) {
        return +a;
    }

    public static float UNARY_PLUS(Float a) {
        return +a;
    }

    public static double UNARY_PLUS(Double a) {
        return +a;
    }

    public static boolean CONDITIONAL_AND(Boolean a, Boolean b) {
        return a && b;
    }

    public static boolean CONDITIONAL_OR(Boolean a, Boolean b) {
        return a || b;
    }

    public static boolean CONDITIONAL_AND(boolean a, boolean b) {
        return a && b;
    }

    public static boolean CONDITIONAL_OR(boolean a, boolean b) {
        return a || b;
    }


    public static double PLUS(Double a, Double b) {
        return a + b;
    }

    public static long PLUS(Long a, Long b) {
        return a + b;
    }

    public static int PLUS(Integer a, Integer b) {
        return a + b;
    }

    public static float PLUS(Float a, Float b) {
        return a + b;
    }

    public static double PLUS(double a, double b) {
        return a + b;
    }

    public static long PLUS(long a, long b) {
        return a + b;
    }

    public static int PLUS(int a, int b) {
        return a + b;
    }

    public static float PLUS(float a, float b) {
        return a + b;
    }


    public static String PLUS(String a, String b) {
        return a + b;
    }

    public static double MINUS(Double a, Double b) {
        return a - b;
    }

    public static long MINUS(Long a, Long b) {
        return a - b;
    }

    public static int MINUS(Integer a, Integer b) {
        return a - b;
    }

    public static float MINUS(Float a, Float b) {
        return a - b;
    }

    public static double MINUS(double a, double b) {
        return a - b;
    }

    public static long MINUS(long a, long b) {
        return a - b;
    }

    public static int MINUS(int a, int b) {
        return a - b;
    }

    public static float MINUS(float a, float b) {
        return a - b;
    }

    public static double MULTIPLY(Double a, Double b) {
        return a * b;
    }

    public static long MULTIPLY(Long a, Long b) {
        return a * b;
    }

    public static int MULTIPLY(Integer a, Integer b) {
        return a * b;
    }

    public static float MULTIPLY(Float a, Float b) {
        return a * b;
    }

    public static double MULTIPLY(double a, double b) {
        return a * b;
    }

    public static long MULTIPLY(long a, long b) {
        return a * b;
    }

    public static int MULTIPLY(int a, int b) {
        return a * b;
    }

    public static float MULTIPLY(float a, float b) {
        return a * b;
    }

    public static double DIVIDE(Double a, Double b) {
        return a / b;
    }

    public static long DIVIDE(Long a, Long b) {
        return a / b;
    }

    public static int DIVIDE(Integer a, Integer b) {
        return a / b;
    }

    public static float DIVIDE(Float a, Float b) {
        return a / b;
    }

public static double DIVIDE(double a, double b) {
        return a / b;
    }

    public static long DIVIDE(long a, long b) {
        return a / b;
    }

    public static int DIVIDE(int a, int b) {
        return a / b;
    }

    public static float DIVIDE(float a, float b) {
        return a / b;
    }

    public static double REMAINDER(Double a, Double b) {
        return a % b;
    }

    public static long REMAINDER(Long a, Long b) {
        return a % b;
    }

    public static int REMAINDER(Integer a, Integer b) {
        return a % b;
    }

    public static float REMAINDER(Float a, Float b) {
        return a % b;
    }

    public static double REMAINDER(double a, double b) {
        return a % b;
    }

    public static long REMAINDER(long a, long b) {
        return a % b;
    }

    public static int REMAINDER(int a, int b) {
        return a % b;
    }

    public static float REMAINDER(float a, float b) {
        return a % b;
    }

    public static long LEFT_SHIFT(Long a, Long b) {
        return a << b;
    }

    public static int LEFT_SHIFT(Integer a, Integer b) {
        return a << b;
    }

    public static int LEFT_SHIFT(int a, int b) {
        return a << b;
    }

    public static long RIGHT_SHIFT(Long a, Long b) {
        return a >> b;
    }

    public static int RIGHT_SHIFT(Integer a, Integer b) {
        return a >> b;
    }

    public static long RIGHT_SHIFT(long a, long b) {
        return a >> b;
    }

    public static int RIGHT_SHIFT(int a, int b) {
        return a >> b;
    }

    public static long UNSIGNED_RIGHT_SHIFT(Long a, Long b) {
        return a >>> b;
    }

    public static int UNSIGNED_RIGHT_SHIFT(Integer a, Integer b) {
        return a >>> b;
    }

    public static long UNSIGNED_RIGHT_SHIFT(long a, long b) {
        return a >>> b;
    }

    public static int UNSIGNED_RIGHT_SHIFT(int a, int b) {
        return a >>> b;
    }

    public static boolean AND(Boolean a, Boolean b) {
        return a & b;
    }

    public static boolean AND(boolean a, boolean b) {
        return a & b;
    }

    public static long AND(Long a, Long b) {
        return a & b;
    }

    public static int AND(Integer a, Integer b) {
        return a & b;
    }

    public static long AND(long a, long b) {
        return a & b;
    }

    public static int AND(int a, int b) {
        return a & b;
    }

    public static boolean OR(Boolean a, Boolean b) {
        return a | b;
    }

    public static long OR(Long a, Long b) {
        return a | b;
    }

    public static int OR(Integer a, Integer b) {
        return a | b;
    }

    public static boolean OR(boolean a, boolean b) {
        return a | b;
    }

    public static long OR(long a, long b) {
        return a | b;
    }

    public static int OR(int a, int b) {
        return a | b;
    }


    public static boolean XOR(Boolean a, Boolean b) {
        return a ^ b;
    }

    public static long XOR(Long a, Long b) {
        return a ^ b;
    }

    public static int XOR(Integer a, Integer b) {
        return a ^ b;
    }

    public static boolean XOR(boolean a, boolean b) {
        return a ^ b;
    }

    public static long XOR(long a, long b) {
        return a ^ b;
    }

    public static int XOR(int a, int b) {
        return a ^ b;
    }

    public static boolean EQUAL_TO(Double a, Double b) {
        return a == b;
    }

    public static boolean EQUAL_TO(Character a, Character b) {
        return a == b;
    }

    public static boolean EQUAL_TO(Long a, Long b) {
        return a == b;
    }

    public static boolean NOT_EQUAL_TO(Double a, Double b) {
        return a != b;
    }

    public static boolean NOT_EQUAL_TO(Character a, Character b) {
        return a != b;
    }

    public static boolean NOT_EQUAL_TO(Long a, Long b) {
        return a != b;
    }

    public static boolean GREATER_THAN(Double a, Double b) {
        return a > b;
    }

    public static boolean GREATER_THAN(Character a, Character b) {
        return a > b;
    }

    public static boolean GREATER_THAN(Long a, Long b) {
        return a > b;
    }

    public static boolean GREATER_THAN_EQUAL(Double a, Double b) {
        return a >= b;
    }

    public static boolean GREATER_THAN_EQUAL(Character a, Character b) {
        return a >= b;
    }

    public static boolean GREATER_THAN_EQUAL(Long a, Long b) {
        return a >= b;
    }

    public static boolean LESS_THAN(Double a, Double b) {
        return a < b;
    }

    public static boolean LESS_THAN(Character a, Character b) {
        return a < b;
    }

    public static boolean LESS_THAN(Long a, Long b) {
        return a < b;
    }

    public static boolean LESS_THAN_EQUAL(Double a, Double b) {
        return a <= b;
    }

    public static boolean LESS_THAN_EQUAL(Character a, Character b) {
        return a <= b;
    }

    public static boolean LESS_THAN_EQUAL(Long a, Long b) {
        return a <= b;
    }

    public static boolean EQUAL_TO(Boolean a, Boolean b) {
        return a == b;
    }
    public static boolean EQUAL_TO(boolean a, boolean b) {
        return a == b;
    }


    public static boolean NOT_EQUAL_TO(Boolean a, Boolean b) {
        return a != b;
    }

    public static boolean NOT_EQUAL_TO(boolean a, boolean b) {
        return a != b;
    }


    public static boolean EQUAL_TO(String a, String b) {
        return a == b;
    }

    public static boolean NOT_EQUAL_TO(String a, String b) {
        return a != b;
    }

}
