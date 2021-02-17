import org.checkerframework.common.returnsreceiver.qual.*;

// Test basic subtyping relationships for the Returns Receiver Checker.
public class SimpleTest {

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

    // :: error: type.invalid.this.location
    @This SimpleTest thisOnParam(@This SimpleTest x) {
        return x;
    }

    void thisOnLocal() {
        // :: error: type.invalid.this.location
        // :: error: assignment.type.incompatible
        @This SimpleTest x = new SimpleTest();

        // :: error: type.invalid.this.location
        // :: error: type.argument.type.incompatible
        java.util.List<@This String> l = null;
    }

    // can write @This on receiver
    void thisOnReceiver(@This SimpleTest this) {}

    // :: error: type.invalid.this.location :: error: invalid.polymorphic.qualifier.use
    @This Object f;

    interface I {

        Object foo();

        SimpleTest.@This I setBar();
    }

    // :: error: type.invalid.this.location
    static @This Object thisOnStatic() {
        // :: error: return.type.incompatible
        return new Object();
    }
}
