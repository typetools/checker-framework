import testlib.wholeprograminference.qual.Parent;
import testlib.wholeprograminference.qual.Sibling1;

abstract class MethodDefinedInSupertype {

    void test() {
        // :: error: argument.type.incompatible
        expectsSibling1(shouldReturnSibling1());
    }

    public void expectsSibling1(@Sibling1 int t) {}

    public abstract int shouldReturnSibling1();

    void testMultipleOverrides() {
        // :: error: argument.type.incompatible
        expectsParent(shouldReturnParent());
    }

    public void expectsParent(@Parent int t1) {}

    public abstract int shouldReturnParent();
}
