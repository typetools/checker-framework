// This test ensures that annotations on different component types of multidimensional arrays
// are printed correctly.

import testlib.wholeprograminference.qual.Sibling1;
import testlib.wholeprograminference.qual.Sibling2;

class MultiDimensionalArrays {
    void requiresS1S2(@Sibling1 int @Sibling2 [] x) {}

    int[] multiDimArray;

    void testField() {
        // :: error: argument.type.incompatible
        requiresS1S2(multiDimArray);
    }

    void useField(@Sibling1 int @Sibling2 [] x) {
        multiDimArray = x;
    }

    void testParam(int[] x) {
        // :: error: argument.type.incompatible
        requiresS1S2(x);
    }

    void useParam(@Sibling1 int @Sibling2 [] x) {
        testParam(x);
    }
}
