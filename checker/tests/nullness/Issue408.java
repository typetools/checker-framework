// Test case for Issue 408
// https://github.com/typetools/checker-framework/issues/408

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Raw;

class Issue408 {
    static class Bar {
        Bar() {
            doIssue408();
        }

        String doIssue408(@UnderInitialization @Raw(Issue408.Bar.class) Bar this) {
            return "";
        }
    }

    static class Baz extends Bar {
        String myString = "hello";

        @Override
        String doIssue408(@UnderInitialization @Raw Baz this) {
            // :: error: (dereference.of.nullable)
            return myString.toLowerCase();
        }
    }

    public static void main(String[] args) {
        new Baz();
    }
}
