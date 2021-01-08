// This test case is for issue 44: https://github.com/kelloggm/checker-framework/issues/44

import org.checkerframework.checker.index.qual.*;

public class ArrayCreationChecks {

    void test1(@Positive int x, @Positive int y) {
        int[] newArray = new int[x + y];
        @IndexFor("newArray") int i = x;
        @IndexFor("newArray") int j = y;
    }

    void test2(@NonNegative int x, @Positive int y) {
        int[] newArray = new int[x + y];
        @IndexFor("newArray") int i = x;
        @IndexOrHigh("newArray") int j = y;
    }

    void test3(@NonNegative int x, @NonNegative int y) {
        int[] newArray = new int[x + y];
        @IndexOrHigh("newArray") int i = x;
        @IndexOrHigh("newArray") int j = y;
    }

    void test4(@GTENegativeOne int x, @NonNegative int y) {
        // :: error: (array.length.negative)
        int[] newArray = new int[x + y];
        @LTEqLengthOf("newArray") int i = x;
        // :: error: (assignment.type.incompatible)
        @IndexOrHigh("newArray") int j = y;
    }

    void test5(@GTENegativeOne int x, @GTENegativeOne int y) {
        // :: error: (array.length.negative)
        int[] newArray = new int[x + y];
        // :: error: (assignment.type.incompatible)
        @IndexOrHigh("newArray") int i = x;
        // :: error: (assignment.type.incompatible)
        @IndexOrHigh("newArray") int j = y;
    }

    void test6(int x, int y) {
        // :: error: (array.length.negative)
        int[] newArray = new int[x + y];
        // :: error: (assignment.type.incompatible)
        @IndexFor("newArray") int i = x;
        // :: error: (assignment.type.incompatible)
        @IndexOrHigh("newArray") int j = y;
    }
}
