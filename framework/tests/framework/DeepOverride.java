import org.checkerframework.framework.testchecker.util.*;

public class DeepOverride {
    public static class A {
        public @Odd String method() {
            return null;
        }
    }

    public static class B extends A {}

    public static class C extends B {
        @Override
        // :: error: (override.return.invalid)
        public String method() {
            return "";
        }
    }
}
