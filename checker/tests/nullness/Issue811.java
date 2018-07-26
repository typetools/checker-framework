// Test case for Issue 266
// https://github.com/typetools/checker-framework/issues/266

import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue811 {
    static class T {
        void xyz() {}
    }

    interface U {
        void method();
    }

    private final @NonNull T tField;
    private U uField;

    public Issue811(@NonNull T t) {
        tField = t;
        uField =
                new U() {
                    @Override
                    public void method() {
                        tField.xyz();
                    }
                };
    }
}
