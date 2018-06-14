// Test case for issue 266: https://github.com/typetools/checker-framework/issues/266
// The problem is limited refinement of final variables.

import org.checkerframework.checker.nullness.qual.*;

class FinalVar2 {

    static Object method1(@Nullable Object arg) {
        final Object tmp = arg;
        if (tmp == null) {
            return "hello";
        }
        return new Object() {
            public void useFinalVar() {
                // should be OK
                tmp.hashCode();
            }
        };
    }

    static Object method2(final @Nullable Object arg) {
        if (arg == null) {
            return "hello";
        }
        return new Object() {
            public void useFinalVar() {
                // should be OK
                arg.hashCode();
            }
        };
    }

    static Object method3(@Nullable Object arg) {
        final Object tmp = arg;
        Object result =
                new Object() {
                    public void useFinalVar() {
                        // :: error: (dereference.of.nullable)
                        tmp.hashCode();
                    }
                };
        if (tmp == null) {
            return "hello";
        }
        return result;
    }
}
