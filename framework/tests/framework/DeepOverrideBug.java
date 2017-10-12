import testlib.util.*;

// TODO: the output have a "missing return statement"?
public class DeepOverrideBug {

    public static interface I {
        @Odd String interfaceMethod();

        String abstractMethod();
    }

    public abstract static class A {
        public abstract @Odd String abstractMethod();

        public abstract String interfaceMethod();
    }

    public abstract static class B extends A implements I {}

    public static class C extends B {
        // :: error: (override.return.invalid)
        public String interfaceMethod() { // should emit error
            return null;
        }
        // :: error: (override.return.invalid)
        public String abstractMethod() { // should emit error
            return null;
        }
    }
}
