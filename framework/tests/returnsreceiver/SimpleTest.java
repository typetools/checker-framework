import org.checkerframework.common.returnsreceiver.qual.*;

// Test basic subtyping relationships for the Returns Receiver Checker.
class SimpleTest {

    @This SimpleTest retNull() {
        // :: error: return.type.incompatible
        return null;
    }

    @This SimpleTest retThis() {
        return this;
    }

    @This SimpleTest retThisWrapper(@UnknownThis SimpleTest other, boolean flag) {
        if (flag) {
            // :: error: return.type.incompatible
            return other.retThis();
        } else {
            return this.retThis();
        }
    }

    @This SimpleTest retLocalThis() {
        SimpleTest x = this;
        return x;
    }

    @This SimpleTest retNewLocal() {
        SimpleTest x = new SimpleTest();
        // :: error: return.type.incompatible
        return x;
    }

    // :: error: invalid.this.location
    @This SimpleTest thisOnParam(@This SimpleTest x) {
        return x;
    }

    void thisOnLocal() {
        // :: error: invalid.this.location
        // :: error: assignment.type.incompatible
        @This SimpleTest x = new SimpleTest();

        // :: error: invalid.this.location
        // :: error: type.argument.type.incompatible
        java.util.List<@This String> l = null;
    }

    // :: error: invalid.this.location
    void thisOnReceiver(@This SimpleTest this) {}

    // :: error: invalid.this.location
    @This Object f;

    interface I {

        Object foo();

        SimpleTest.@This I setBar();
    }
}
