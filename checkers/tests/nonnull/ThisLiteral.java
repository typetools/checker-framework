import checkers.nonnull.quals.*;
import checkers.initialization.quals.*;

public class ThisLiteral {
    public ThisLiteral() {
        new Object() {
            void test() {
                @Free ThisLiteral l1 = ThisLiteral.this;
                //:: error: (assignment.type.incompatible)
                @Committed ThisLiteral l2 = ThisLiteral.this;
                
                //:: error: (method.invocation.invalid)
                ThisLiteral.this.foo();
                //:: error: (method.invocation.invalid)
                foo();
            }
        };
    }
    
    void foo() {}
}
