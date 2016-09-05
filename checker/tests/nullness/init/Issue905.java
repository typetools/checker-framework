// Test case for Issue 905:
// https://github.com/typetools/checker-framework/issues/905

// @skip-test

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Issue905 {
    final Object mBar;

    Issue905() {
        baz();
        mBar = "";
    }

    void baz(@UnknownInitialization(Issue905.class) Issue905 this) {
        //:: error: (dereference.of.nullable)
        mBar.toString();
    }

    public static void main(String[] args) {
        new Issue905();
    }
}
