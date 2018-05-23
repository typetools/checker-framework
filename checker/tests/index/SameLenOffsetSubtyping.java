// Test case for SameLen subtyping and assignment

import org.checkerframework.checker.index.qual.SameLen;

public class SameLenOffsetSubtyping {
    void foo(int[] a, int @SameLen(value = "#1", offset = "#3") [] b, int c) {
        int
                        @SameLen(
                            value = {"a", "b"},
                            offset = {"c", "0"}
                        )
                        []
                d = b;
        int
                        @SameLen(
                            value = {"a", "b"},
                            offset = {"0", "c"}
                        )
                        []
                // :: error: (assignment.type.incompatible)
                e = b;
        int
                        @SameLen(
                            value = {"a"},
                            offset = {"c"}
                        )
                        []
                f = b;
        int
                        @SameLen(
                            value = {"a"},
                            offset = {"0"}
                        )
                        []
                // :: error: (assignment.type.incompatible)
                g = b;
    }

    void foo2(
            int[] a,
            int @SameLen(value = "#1", offset = "#3") [] b,
            int c,
            int @SameLen(value = "#1", offset = "#3") [] d) {
        int
                        @SameLen(
                            value = {"a", "b"},
                            offset = {"c", "0"}
                        )
                        []
                e = b;
        int
                        @SameLen(
                            value = {"a", "d"},
                            offset = {"c", "0"}
                        )
                        []
                f = d;

        int
                        @SameLen(
                            value = {"a", "b", "d"},
                            offset = {"0", "0", "0"}
                        )
                        []
                // :: error: (assignment.type.incompatible)
                g = b;

        int
                        @SameLen(
                            value = {"a", "b", "d"},
                            offset = {"0", "0", "0"}
                        )
                        []
                // :: error: (assignment.type.incompatible)
                h = d;
    }

    void lubTest(int[] a, int @SameLen(value = "#1", offset = "#3") [] b, int c) {
        int[] d;
        if (c == 10) {
            d = b;
        } else {
            d = a;
        }

        // :: error: (assignment.type.incompatible)
        int @SameLen("a") [] e = d;

        // :: error: (assignment.type.incompatible)
        int @SameLen("b") [] f = d;

        // :: error: (assignment.type.incompatible)
        int @SameLen(value = "a", offset = "c") [] g = d;
    }

    void lubTest2(
            int[] a,
            int @SameLen(value = "#1", offset = "#3") [] b,
            int c,
            int @SameLen(value = "#1", offset = "#3") [] d) {
        int[] e;
        if (c == 10) {
            e = b;
        } else {
            e = d;
        }

        // :: error: (assignment.type.incompatible)
        int @SameLen("a") [] f = e;

        int @SameLen(value = "a", offset = "c") [] h = d;

        int @SameLen(value = "a", offset = "c") [] j = b;

        int @SameLen(value = "a", offset = "c") [] k = e;
    }
}
