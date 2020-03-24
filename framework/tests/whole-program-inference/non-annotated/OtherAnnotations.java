// Test that having other, unrelated annotations on fields/methods/etc doesn't foul up inference.

import org.checkerframework.common.value.qual.IntVal;
import testlib.wholeprograminference.qual.Sibling1;

class OtherAnnotations {

    void requireSibling1(@Sibling1 int a) {}

    @IntVal(5) int x;

    void assignX(@Sibling1 int y) {
        x = y;
    }

    void useX() {
        // :: error: argument.type.incompatible
        requireSibling1(x);
    }

    void methodWithAnnotatedParam(@IntVal(5) int z) {
        // :: error: argument.type.incompatible
        requireSibling1(z);
    }

    void useMethodWithAnnotatedParam(@Sibling1 int w) {
        methodWithAnnotatedParam(w);
    }

    @Sibling1 int getSibling1() {
        return 5;
    }

    @IntVal(5) int getIntVal5() {
        return getSibling1();
    }

    void useGetIntVal5() {
        // :: error: argument.type.incompatible
        requireSibling1(getIntVal5());
    }
}
