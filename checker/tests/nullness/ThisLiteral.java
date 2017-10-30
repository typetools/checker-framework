import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class ThisLiteral {
    public ThisLiteral() {
        new Object() {
            void test() {
                @UnderInitialization @Raw ThisLiteral l1 = ThisLiteral.this;
                // :: error: (assignment.type.incompatible)
                @Initialized @NonRaw ThisLiteral l2 = ThisLiteral.this;

                ThisLiteral.this.foo();
                // :: error: (method.invocation.invalid)
                foo();
            }
        };
    }

    void foo() {}
}
