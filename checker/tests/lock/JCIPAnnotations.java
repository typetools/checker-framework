import net.jcip.annotations.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockingFree;

// Smoke test for supporting JCIP and Javax annotations.
// Note that JCIP and Javax @GuardedBy can only be written on fields and methods.
public class JCIPAnnotations {
    class MyClass {
        Object field;

        @LockingFree
        void methodWithUnguardedReceiver() {}

        @LockingFree
        void methodWithGuardedReceiver(
                @org.checkerframework.checker.lock.qual.GuardedBy("lock") MyClass this) {}

        @LockingFree
        void methodWithGuardSatisfiedReceiver(@GuardSatisfied MyClass this) {}
    }

    final Object lock = new Object();

    @GuardedBy("lock") MyClass jcipGuardedField;

    @javax.annotation.concurrent.GuardedBy("lock") MyClass javaxGuardedField;

    // Tests that Javax and JCIP @GuardedBy(...) typecheck against the Lock Checker @GuardedBy on a
    // receiver.
    void testReceivers() {
        // :: error: (method.invocation.invalid)
        jcipGuardedField.methodWithUnguardedReceiver();
        // :: error: (method.invocation.invalid)
        jcipGuardedField.methodWithGuardedReceiver();
        // :: error: (lock.not.held)
        jcipGuardedField.methodWithGuardSatisfiedReceiver();
        // :: error: (method.invocation.invalid)
        javaxGuardedField.methodWithUnguardedReceiver();
        // :: error: (method.invocation.invalid)
        javaxGuardedField.methodWithGuardedReceiver();
        // :: error: (lock.not.held)
        javaxGuardedField.methodWithGuardSatisfiedReceiver();
    }

    void testDereferences() {
        // :: error: (lock.not.held)
        this.jcipGuardedField.field.toString();
        // :: error: (lock.not.held)
        this.javaxGuardedField.field.toString();
        synchronized (lock) {
            this.jcipGuardedField.field.toString();
            this.javaxGuardedField.field.toString();
        }
    }

    @GuardedBy("lock")
    void testGuardedByAsHolding() {
        this.jcipGuardedField.field.toString();
        this.javaxGuardedField.field.toString();
        jcipGuardedField.field.toString();
        javaxGuardedField.field.toString();
    }

    @GuardedBy("lock") Object testGuardedByAsHolding2(
            @org.checkerframework.checker.lock.qual.GuardedBy({}) Object param) {
        testGuardedByAsHolding();
        // Test that the JCIP GuardedBy applies to the method but not the return type.
        return param;
    }

    void testGuardedByAsHolding3() {
        synchronized (lock) {
            testGuardedByAsHolding();
        }
        // :: error: (contracts.precondition.not.satisfied)
        testGuardedByAsHolding();
    }

    @Holding("lock")
    @GuardedBy("lock")
    // :: error: (multiple.lock.precondition.annotations)
    void testMultipleMethodAnnotations1() {}

    @Holding("lock")
    @javax.annotation.concurrent.GuardedBy("lock")
    // :: error: (multiple.lock.precondition.annotations)
    void testMultipleMethodAnnotations2() {}

    @GuardedBy("lock") @javax.annotation.concurrent.GuardedBy("lock")
    // :: error: (multiple.lock.precondition.annotations)
    void testMultipleMethodAnnotations3() {}

    @Holding("lock")
    @GuardedBy("lock") @javax.annotation.concurrent.GuardedBy("lock")
    // :: error: (multiple.lock.precondition.annotations)
    void testMultipleMethodAnnotations4() {}

    @GuardedBy("lock") @org.checkerframework.checker.lock.qual.GuardedBy("lock")
    // :: error: (multiple.guardedby.annotations)
    Object fieldWithMultipleGuardedByAnnotations1;

    @javax.annotation.concurrent.GuardedBy("lock") @org.checkerframework.checker.lock.qual.GuardedBy("lock")
    // :: error: (multiple.guardedby.annotations)
    Object fieldWithMultipleGuardedByAnnotations2;

    @GuardedBy("lock") @javax.annotation.concurrent.GuardedBy("lock")
    // :: error: (multiple.guardedby.annotations)
    Object fieldWithMultipleGuardedByAnnotations3;

    @GuardedBy("lock") @javax.annotation.concurrent.GuardedBy("lock") @org.checkerframework.checker.lock.qual.GuardedBy("lock")
    // :: error: (multiple.guardedby.annotations)
    Object fieldWithMultipleGuardedByAnnotations4;
}
