// Test case for Issue 408:
// https://github.com/typetools/checker-framework/issues/408

import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class Issue408Init {
    static class Bar {
        Bar() {
            doFoo();
        }

        String doFoo(@UnderInitialization Bar this) {
            return "";
        }
    }

    static class Baz extends Bar {
        String myString = "hello";

        @Override
        String doFoo(@UnderInitialization Baz this) {
            // :: error: (dereference.of.nullable)
            return myString.toLowerCase();
        }
    }

    public static void main(String[] args) {
        new Baz();
    }
}
