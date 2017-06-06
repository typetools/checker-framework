// @skip-test
// Test case for Issue 352:
// https://github.com/typetools/checker-framework/issues/352

import org.checkerframework.checker.tainting.qual.Untainted;

class Outer {
    class Nested {
        @Untainted Outer context(@Untainted Outer.@Untainted Nested this) {
            return Outer.this;
        }
    }
}
