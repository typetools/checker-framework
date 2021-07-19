import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class ThisNodeTest {
    public ThisNodeTest() {
        new Object() {
            void test() {
                @UnderInitialization ThisNodeTest l1 = ThisNodeTest.this;
                // :: error: (assignment.type.incompatible)
                @Initialized ThisNodeTest l2 = ThisNodeTest.this;

                // :: error: (method.invocation.invalid)
                ThisNodeTest.this.foo();
                // :: error: (method.invocation.invalid)
                foo();
            }
        };
    }

    void foo() {}
}
