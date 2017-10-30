import testlib.wholeprograminference.qual.*;

public class ConstructorTest {

    public ConstructorTest(int top) {}

    void test() {
        @Top int top = (@Top int) 0;
        // :: error: (argument.type.incompatible)
        new ConstructorTest(top);
    }
}
