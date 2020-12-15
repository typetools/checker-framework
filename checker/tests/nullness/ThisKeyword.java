import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class ThisKeyword {
    public ThisKeyword() {
        new Object() {
            void test() {
                @UnderInitialization ThisKeyword l1 = ThisKeyword.this;
                // :: error: (assignment.type.incompatible)
                @Initialized ThisKeyword l2 = ThisKeyword.this;

                // :: error: (method.invocation.invalid)
                ThisKeyword.this.foo();
                // :: error: (method.invocation.invalid)
                foo();
            }
        };
    }

    void foo() {}
}
