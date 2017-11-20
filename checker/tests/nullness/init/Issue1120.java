// Test case for Issue #1120:
// https://github.com/typetools/checker-framework/issues/1120

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Raw;

class Issue1120Super {
    Object f = new Object();
}

final class Issue1120Sub extends Issue1120Super {
    Object g;

    Issue1120Sub() {
        this.party();
        // this is @UnderInitialization(A.class)
        g = new Object();
        // this is @Initialized now
        this.party();
        this.bar();
    }

    Issue1120Sub(int i) {
        // this is @UnderInitialization(A.class)
        this.party();
        // :: error: (method.invocation.invalid)
        this.bar();
        g = new Object();
    }

    void bar() {
        g.toString();
    }

    void party(@UnknownInitialization @Raw Issue1120Sub this) {}
}
