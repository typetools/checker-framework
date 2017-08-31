// Test case for Issue panacekcz#12:
// https://github.com/panacekcz/checker-framework/issues/12

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.IntVal;

class RefineNeqLength {
    void refineNeqLength(int[] array, @IndexOrHigh("#1") int i) {
        // Refines i <= array.length to i < array.length
        if (i != array.length) {
            refineNeqLengthMOne(array, i);
        }
        // No refinement
        if (i != array.length - 1) {
            //:: error: (argument.type.incompatible)
            refineNeqLengthMOne(array, i);
        }
    }

    void refineNeqLengthMOne(int[] array, @IndexFor("#1") int i) {
        // Refines i < array.length to i < array.length - 1
        if (i != array.length - 1) {
            refineNeqLengthMTwo(array, i);
            //:: error: (argument.type.incompatible)
            refineNeqLengthMThree(array, i);
        }
    }

    void refineNeqLengthMTwo(int[] array, @NonNegative @LTOMLengthOf("#1") int i) {
        // Refines i < array.length - 1 to i < array.length - 2
        if (i != array.length - 2) {
            refineNeqLengthMThree(array, i);
        }
        // No refinement
        if (i != array.length - 1) {
            //:: error: (argument.type.incompatible)
            refineNeqLengthMThree(array, i);
        }
    }

    void refineNeqLengthMTwoNonLiteral(
            int[] array,
            @NonNegative @LTOMLengthOf("#1") int i,
            @IntVal(3) int c3,
            @IntVal({2, 3}) int c23) {
        // Refines i < array.length - 1 to i < array.length - 2
        if (i != array.length - (5 - c3)) {
            refineNeqLengthMThree(array, i);
        }
        // No refinement
        if (i != array.length - c23) {
            //:: error: (argument.type.incompatible)
            refineNeqLengthMThree(array, i);
        }
    }

    @LTLengthOf(value = "#1", offset = "3") int refineNeqLengthMThree(
            int[] array, @NonNegative @LTLengthOf(value = "#1", offset = "2") int i) {
        // Refines i < array.length - 2 to i < array.length - 3
        if (i != array.length - 3) {
            return i;
        }
        //:: error: (return.type.incompatible)
        return i;
    }

    // The same test for a string.
    @LTLengthOf(value = "#1", offset = "3") int refineNeqLengthMThree(
            String str, @NonNegative @LTLengthOf(value = "#1", offset = "2") int i) {
        // Refines i < str.length() - 2 to i < str.length() - 3
        if (i != str.length() - 3) {
            return i;
        }
        //:: error: (return.type.incompatible)
        return i;
    }
}
