import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class ThisLiteral {
    public ThisLiteral() {
        new Object() {
            void test() {
                @Free @Raw ThisLiteral l1 = ThisLiteral.this;
                //:: error: (assignment.type.incompatible)
                @Committed @NonRaw ThisLiteral l2 = ThisLiteral.this;

                //:: error: (method.invocation.invalid)
                ThisLiteral.this.foo();
                //:: error: (method.invocation.invalid)
                foo();
            }
        };
    }

    void foo() {}
}
