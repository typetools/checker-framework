// Tests that String.length() is supported in the same situations as array length

import java.util.Random;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.common.value.qual.MinLen;

class StringLength {
    void testMinLenSubtractPositive(@MinLen(10) String s) {
        @Positive int i1 = s.length() - 9;
        @NonNegative int i0 = s.length() - 10;
        // ::  error: (assignment.type.incompatible)
        @NonNegative int im1 = s.length() - 11;
    }

    void testNewArraySameLen(String s) {
        int @SameLen("s") [] array = new int[s.length()]; // TODO
        // ::  error: (assignment.type.incompatible)
        int @SameLen("s") [] array1 = new int[s.length() + 1];
    }

    void testStringAssignSameLen(String s, String r) {
        @SameLen("s") String t = s;
        // ::  error: (assignment.type.incompatible)
        @SameLen("s") String tN = r;
    }

    void testStringLenEqualSameLen(String s, String r) {
        if (s.length() == r.length()) {
            @SameLen("s") String tN = r;
        }
    }

    void testStringEqualSameLen(String s, String r) {
        if (s == r) {
            @SameLen("s") String tN = r;
        }
    }

    void testOffsetRemoval(
            String s,
            String t,
            @LTLengthOf(value = "#1", offset = "#2.length()") int i,
            @LTLengthOf(value = "#2") int j,
            int k) {
        @LTLengthOf("s") int ij = i + j;
        // ::  error: (assignment.type.incompatible)
        @LTLengthOf("s") int ik = i + k;
    }

    void testLengthDivide(@MinLen(1) String s) {
        @IndexFor("s") int i = s.length() / 2;
    }

    void testAddDivide(@MinLen(1) String s, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        @IndexFor("s") int ij = (i + j) / 2;
    }

    void testRandomMultiply(@MinLen(1) String s, Random r) {
        @LTLengthOf("s") int i = (int) (Math.random() * s.length());
        @LTLengthOf("s") int j = (int) (r.nextDouble() * s.length());
    }

    void testNotEqualLength(String s, @IndexOrHigh("#1") int i, @IndexOrHigh("#1") int j) {
        if (i != s.length()) {
            @IndexFor("s") int in = i;
            // ::  error: (assignment.type.incompatible)
            @IndexFor("s") int jn = j;
        }
    }

    void testLength(String s) {
        @IndexOrHigh("s") int i = s.length();
        @LTLengthOf("s") int j = s.length() - 1;
    }
}
