// A simple test that @RequiresCalledMethods works as expected.

import org.checkerframework.checker.calledmethods.qual.RequiresCalledMethods;

class RequiresCalledMethodsTest {

    Object foo;

    @RequiresCalledMethods(value = "this.foo", methods = "toString")
    void afterFooToString() {}

    void test_ok() {
        foo.toString();
        afterFooToString();
    }

    void test_bad() {
        // foo.toString();
        // :: error: contracts.precondition.not.satisfied
        afterFooToString();
    }
}
