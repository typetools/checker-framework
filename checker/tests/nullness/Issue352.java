// Test case for Issue 352:
// https://github.com/typetools/checker-framework/issues/352

import org.checkerframework.checker.nullness.qual.*;
class Outer {

    class Nested {
        @NonNull Outer context(@NonNull Outer.@NonNull Nested this) {
            return Outer.this;
        }
    }
}
