import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.test.*;
import testlib.util.*;

// Disable the test.  The Checker Framework behaves correctly, but the
// compiler issues a warning because the test uses a deprecated class
// (checkers.nullness.quals.Pure), and this warning makes the test fail.
/** Various tests for annotation aliasing. */
class AnnotationAliasing {

    String f1, f2, f3;

    @Pure
    int pure1() {
        return 1;
    };

    @org.jmlspecs.annotation.Pure
    int pure2() {
        return 1;
    };

    // a method that is not pure (no annotation)
    void nonpure() {}

    @Pure
    String t1() {
        // :: error: (purity.not.deterministic.not.sideeffectfree.call.method)
        nonpure();
        return "";
    }

    @org.jmlspecs.annotation.Pure
    String t2() {
        // :: error: (purity.not.deterministic.not.sideeffectfree.call.method)
        nonpure();
        return "";
    }

    // check aliasing of Pure
    void t1(@Odd String p1, String p2) {
        f1 = p1;
        @Odd String l2 = f1;
        pure1();
        @Odd String l3 = f1;
        pure2();
        @Odd String l4 = f1;
    }
}
