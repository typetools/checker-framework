// A simple test that the fluent API logic in the Accumulation Checker works.
// Copied from the Object Construction Checker.

import org.checkerframework.common.returnsreceiver.qual.*;
import testaccumulation.qual.*;

/* Simple inference of a fluent builder. */
class SimpleFluent {
    SimpleFluent build(@TestAccumulation({"a", "b"}) SimpleFluent this) {
        return this;
    }

    @This SimpleFluent a() {
        return this;
    }

    @This SimpleFluent b() {
        return this;
    }

    // intentionally does not have an @This annotation
    SimpleFluent c() {
        return this;
    }

    static void doStuffCorrect(@TestAccumulation({"a", "b"}) SimpleFluent s) {
        s.a().b().build();
    }

    static void doStuffWrong(@TestAccumulation({"a"}) SimpleFluent s) {
        s.a()
                // :: error: method.invocation.invalid
                .build();
    }

    static void noReturnsReceiverAnno(@TestAccumulation({"a", "b"}) SimpleFluent s) {
        s.a().b()
                .c()
                // :: error: method.invocation.invalid
                .build();
    }

    static void fluentLoop(SimpleFluent t) {
        SimpleFluent s = t.a();
        int i = 10;
        while (i > 0) {
            // :: error: method.invocation.invalid
            s.b().build();
            i--;
            s = new SimpleFluent();
        }
    }
}
