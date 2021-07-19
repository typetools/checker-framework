import org.checkerframework.framework.testchecker.util.*;

public class DeepOverrideAbstract {

    public static interface I {
        @Odd String interfaceMethod();
    }

    public abstract static class A {
        public abstract @Odd String abstractMethod();
    }

    public abstract static class B extends A implements I {}

    public static class C extends B {
        public @Odd String interfaceMethod() {
            return null;
        }
        // :: error: (override.return.invalid)
        public String abstractMethod() {
            return "";
        }
    }
}
