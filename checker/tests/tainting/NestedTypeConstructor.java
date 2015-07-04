import org.checkerframework.checker.tainting.qual.*;

// Test case for Issue 275
// https://github.com/typetools/checker-framework/issues/275
// Not tainting-specific, but a convenient location.
class NestedTypeConstructor {
    class Inner {
        @Tainted Inner() { }
    }
}
