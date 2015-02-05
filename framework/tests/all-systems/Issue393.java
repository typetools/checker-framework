// Test case for Issue 393
// http://code.google.com/p/checker-framework/issues/detail?id=393

abstract class TypeVarTaintCheck {

    void test() {
        wrap(new Object());
    }

    abstract <T, U extends T> void wrap(U u);
}