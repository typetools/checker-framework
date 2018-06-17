// Test case for issue 411:
// https://github.com/typetools/checker-framework/issues/411

import org.checkerframework.checker.nullness.qual.*;

class Test {

    @MonotonicNonNull Object field1 = null;
    final @Nullable Object field2 = null;

    void m() {
        if (field1 != null) {
            new Object() {
                void f() {
                    field1.toString();
                }
            };
        }
    }

    void n() {
        if (field2 != null) {
            new Object() {
                void f() {
                    field2.toString();
                }
            };
        }
    }
}
