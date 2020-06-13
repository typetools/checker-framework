// This test makes sure that if a class has two methods with the same name,
// the parameters are inferred correctly and are not confused.

import testlib.wholeprograminference.qual.Sibling1;
import testlib.wholeprograminference.qual.Sibling2;

class TwoMethodsSameName {

    void test(int x, int y) {
        // :: error: assignment.type.incompatible
        @Sibling1 int x1 = x;
        // :: error: assignment.type.incompatible
        @Sibling2 int y1 = y;
    }

    void test(int z) {
        // :: error: assignment.type.incompatible
        @Sibling2 int z1 = z;
    }

    void uses(@Sibling1 int a, @Sibling2 int b) {
        test(a, b);
        test(b);
    }
}
