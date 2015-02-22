// Test case for Issue 393
// http://code.google.com/p/checker-framework/issues/detail?id=393

@SuppressWarnings("oigj") //This may be from improper annotations from the PostTreeAnnotator
abstract class TypeVarTaintCheck {

    void test() {
        wrap(new Object());
    }

    abstract <T, U extends T> void wrap(U u);
}