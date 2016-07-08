import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Test case for Issue 275
// https://github.com/typetools/checker-framework/issues/275
// Not regex-specific, but a convenient location.
class NestedTypeConstructor {
    class Inner {
        @Regex Inner() {}
    }
}
