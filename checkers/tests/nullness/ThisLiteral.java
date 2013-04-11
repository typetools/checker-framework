import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class ThisLiteral {
    public ThisLiteral() {
        new Object() {
            void test() {
                @UnderInitializion @Raw ThisLiteral l1 = ThisLiteral.this;
                //:: error: (assignment.type.incompatible)
                @Initialized @NonRaw ThisLiteral l2 = ThisLiteral.this;

                ThisLiteral.this.foo();
                //:: error: (method.invocation.invalid)
                foo();
            }
        };
    }

    void foo() {}
}
