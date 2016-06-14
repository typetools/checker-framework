// Test case for issue #764:
// https://github.com/typetools/checker-framework/issues/764

import org.checkerframework.checker.nullness.qual.*;

public class Issue764 {
    public static @Nullable Object field = null;

    @RequiresNonNull("field")
    public static void method() { }

    static class MyClass {
        public void otherMethod() {
            //:: error: (contracts.precondition.not.satisfied)
            method();
        }
    }
}
