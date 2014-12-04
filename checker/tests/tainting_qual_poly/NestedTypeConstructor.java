import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test case for Issue 275
// https://code.google.com/p/checker-framework/issues/detail?id=275
// Not tainting-specific, but a convenient location.
class NestedTypeConstructor {
    class Inner {
        @Tainted Inner() { }
    }
}
