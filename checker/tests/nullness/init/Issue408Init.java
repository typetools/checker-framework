// Test case for Issue 408:
// https://github.com/typetools/checker-framework/issues/408

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Raw;

public class Issue408Init {
    static class Bar {
        Bar() {
            doFoo();
        }

        String doFoo(@UnderInitialization @Raw Bar this) {
            return "";
        }
    }

    static class Baz extends Bar {
        String myString = "hello";

        @Override
        String doFoo(@UnderInitialization @Raw Baz this) {
            // :: error: (dereference.of.nullable)
            return myString.toLowerCase();
        }
    }

    public static void main(String[] args) {
        new Baz();
    }
}
