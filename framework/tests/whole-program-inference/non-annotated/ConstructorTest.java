import org.checkerframework.framework.testchecker.wholeprograminference.qual.Top;

public class ConstructorTest {

    public ConstructorTest(int top) {}

    void test() {
        @Top int top = (@Top int) 0;
        // :: error: (argument.type.incompatible)
        new ConstructorTest(top);
    }
}
