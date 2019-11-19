import testlib.wholeprograminference.qual.*;

abstract class MethodDefinedInSupertype {

    void test() {
        MethodDefinedInSupertype i = new MethodOverrideInSubtype();
        // :: error: argument.type.incompatible
        expectsSibling1(shouldReturnSibling1());
    }

    public void expectsSibling1(@Sibling1 int t) {}

    public abstract int shouldReturnSibling1();
}
