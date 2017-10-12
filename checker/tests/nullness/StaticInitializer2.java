// Test for Checker Framework issue 353:
// https://github.com/typetools/checker-framework/issues/353
// There are also a couple of tests commented out in
// checker/tests/nullness/java8/lambda/Initialization.java

// @skip-test until the issue is fixed

import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class StaticInitializer2 {

    static String a;

    static {
        // :: error: (dereference.of.nullable)
        a.toString();
    }
}
