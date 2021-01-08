// Test case for Issue 352:
// https://github.com/typetools/checker-framework/issues/352

import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue352 {
    class Nested {
        @Untainted Issue352 context(@Untainted Issue352.@Untainted Nested this) {
            return Issue352.this;
        }
    }
}
