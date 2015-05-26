//@skip-test
// Test for Checker Framework issue 353:
// http://code.google.com/p/checker-framework/issues/detail?id=353
// There are also a couple of tests commented out in checker/tests/nullness/java8/lambda/Initialization.java

import java.util.ArrayList;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;
import org.checkerframework.checker.initialization.qual.*;


class StaticInitializer2 {

    static String a;

    static {
        //:: error: (dereference.of.nullable)
        a.toString();
    }
}