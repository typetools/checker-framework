import org.checkerframework.checker.tainting.qual.*;

public class Issue3561 {
    void outerMethod(@Untainted Issue3561 this) {}

    class Inner {
        void innerMethod(@Untainted Issue3561.@Untainted Inner this) {
            Issue3561.this.outerMethod();
        }

        void innerMethod2(@Tainted Issue3561.@Untainted Inner this) {
            // :: error: (method.invocation.invalid)
            Issue3561.this.outerMethod();
        }
    }
}
