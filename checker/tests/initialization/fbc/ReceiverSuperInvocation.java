// Test case for issue 2263
// https://github.com/typetools/checker-framework/issues/2263

import org.checkerframework.checker.initialization.qual.UnderInitialization;

class ReceiverSuperInvocation {
    void foo(@UnderInitialization(ReceiverSuperInvocation.class) ReceiverSuperInvocation this) {}
}

class ReceiverSuperInvocationSubclass extends ReceiverSuperInvocation {
    @Override
    void foo(@UnderInitialization(Object.class) ReceiverSuperInvocationSubclass this) {
        // :: error: (method.invocation.invalid)
        super.foo();
    }
}
