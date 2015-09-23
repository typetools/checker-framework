import net.jcip.annotations.*;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.Holding;

// Smoke test for supporting JCIP and Javax annotations
public class JCIPAnnotations {
    void testGuardedAccess5(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass x) {
        x = new MyClass();
    }

 class MyClass {
     Object field = new Object();
     @LockingFree
     Object method(MyClass this){return new Object();}
  }

    void testGuardedAccess1(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass x) {
        this.guardedReceiver();
    }

    Object lock;

    @GuardedBy("lock") MyClass guardedField;
    MyClass unguardedField;

    @LockingFree
    void guardedReceiver(@GuardedBy("lock") JCIPAnnotations this) { }
    @LockingFree
    void unguardedReceiver(@GuardedBy({}) JCIPAnnotations this) { }

    @LockingFree
    void guardedArg(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass arg) { }
    @LockingFree
    void guardedArgUnguardedReceiver(@GuardedBy("lock") MyClass arg) { }
    @LockingFree
    void unguardedArg(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy({}) MyClass arg) { }
    @LockingFree
    void unguardedArgAndReceiver(@GuardedBy({}) JCIPAnnotations this, @GuardedBy({}) MyClass arg) { }

    @LockingFree
    static void guardedStaticArg(@GuardedBy("lock") MyClass x) { }
    @LockingFree
    static void unguardedStaticArg(@GuardedBy({}) MyClass x) { }

    void testUnguardedAccess(@GuardedBy({}) JCIPAnnotations this, @GuardedBy({}) MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        this.unguardedField.field.toString();
        //:: error: (method.invocation.invalid)
        this.guardedReceiver();
        this.unguardedReceiver();
        //:: error: (argument.type.incompatible)
        this.guardedArgUnguardedReceiver(x);
        this.unguardedArgAndReceiver(x);
        unguardedStaticArg(x);
        //:: error: (argument.type.incompatible)
        guardedStaticArg(x);
    }

    void testGuardedAccess(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        // Error because 'lock' is not known to be held, and 'lock' is guarding 'this':
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.field.toString();
        this.guardedReceiver();
        //:: error: (method.invocation.invalid)
        this.unguardedReceiver();
        this.guardedArg(x);
        //:: error: (argument.type.incompatible)
        this.unguardedArg(x);
        //:: error: (argument.type.incompatible)
        unguardedStaticArg(x);
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.field.toString();
            this.unguardedField.field.toString();
            this.guardedReceiver();
            //:: error: (method.invocation.invalid)
            this.unguardedReceiver();
            this.guardedArg(x);
            //:: error: (argument.type.incompatible)
            this.unguardedArg(x);
            //:: error: (argument.type.incompatible)
            unguardedStaticArg(x);
            guardedStaticArg(x);
        }
    }

    void testSemiGuardedAccess(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy({}) MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        // Error because 'lock' is not known to be held, and 'lock' is guarding 'this':
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.field.toString();
        this.guardedReceiver();
        //:: error: (method.invocation.invalid)
        this.unguardedReceiver();
        //:: error: (argument.type.incompatible)
        this.guardedArg(x);
        this.unguardedArg(x);
        unguardedStaticArg(x);
        //:: error: (argument.type.incompatible)
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.field.toString();
            this.unguardedField.field.toString();
            this.guardedReceiver();
            //:: error: (method.invocation.invalid)
            this.unguardedReceiver();
            //:: error: (argument.type.incompatible)
            this.guardedArg(x);
            this.unguardedArg(x);
            unguardedStaticArg(x);
            //:: error: (argument.type.incompatible)
            guardedStaticArg(x);
        }
    }

    void testGuardedAccessAgainstJavaxGuardedBy(@javax.annotation.concurrent.GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        // Error because 'lock' is not known to be held, and 'lock' is guarding 'this':
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.field.toString();
        this.guardedReceiver();
        //:: error: (method.invocation.invalid)
        this.unguardedReceiver();
        this.guardedArg(x);
        //:: error: (argument.type.incompatible)
        this.unguardedArg(x);
        //:: error: (argument.type.incompatible)
        unguardedStaticArg(x);
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.field.toString();
            this.unguardedField.field.toString();
            this.guardedReceiver();
            //:: error: (method.invocation.invalid)
            this.unguardedReceiver();
            this.guardedArg(x);
            //:: error: (argument.type.incompatible)
            this.unguardedArg(x);
            //:: error: (argument.type.incompatible)
            unguardedStaticArg(x);
            guardedStaticArg(x);
        }
    }

    void testSemiGuardedAccessAgainstJavaxGuardedBy(@javax.annotation.concurrent.GuardedBy("lock") JCIPAnnotations this, @GuardedBy({}) MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        // Error because 'lock' is not known to be held, and 'lock' is guarding 'this':
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.field.toString();
        this.guardedReceiver();
        //:: error: (method.invocation.invalid)
        this.unguardedReceiver();
        //:: error: (argument.type.incompatible)
        this.guardedArg(x);
        this.unguardedArg(x);
        unguardedStaticArg(x);
        //:: error: (argument.type.incompatible)
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.field.toString();
            this.unguardedField.field.toString();
            this.guardedReceiver();
            //:: error: (method.invocation.invalid)
            this.unguardedReceiver();
            //:: error: (argument.type.incompatible)
            this.guardedArg(x);
            this.unguardedArg(x);
            unguardedStaticArg(x);
            //:: error: (argument.type.incompatible)
            guardedStaticArg(x);
        }
    }

    void testGuardedAccessAgainstCheckerGuardedBy(@org.checkerframework.checker.lock.qual.GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        // Error because 'lock' is not known to be held, and 'lock' is guarding 'this':
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.field.toString();
        this.guardedReceiver();
        //:: error: (method.invocation.invalid)
        this.unguardedReceiver();
        this.guardedArg(x);
        //:: error: (argument.type.incompatible)
        this.unguardedArg(x);
        //:: error: (argument.type.incompatible)
        unguardedStaticArg(x);
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.field.toString();
            this.unguardedField.field.toString();
            this.guardedReceiver();
            //:: error: (method.invocation.invalid)
            this.unguardedReceiver();
            this.guardedArg(x);
            //:: error: (argument.type.incompatible)
            this.unguardedArg(x);
            //:: error: (argument.type.incompatible)
            unguardedStaticArg(x);
            guardedStaticArg(x);
        }
    }

    void testSemiGuardedAccessAgainstCheckerGuardedBy(@org.checkerframework.checker.lock.qual.GuardedBy("lock") JCIPAnnotations this, @GuardedBy({}) MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.field.toString();
        // Error because 'lock' is not known to be held, and 'lock' is guarding 'this':
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.field.toString();
        this.guardedReceiver();
        //:: error: (method.invocation.invalid)
        this.unguardedReceiver();
        //:: error: (argument.type.incompatible)
        this.guardedArg(x);
        this.unguardedArg(x);
        unguardedStaticArg(x);
        //:: error: (argument.type.incompatible)
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.field.toString();
            this.unguardedField.field.toString();
            this.guardedReceiver();
            //:: error: (method.invocation.invalid)
            this.unguardedReceiver();
            //:: error: (argument.type.incompatible)
            this.guardedArg(x);
            this.unguardedArg(x);
            unguardedStaticArg(x);
            //:: error: (argument.type.incompatible)
            guardedStaticArg(x);
        }
    }

    @GuardedBy("lock")
    void testGuardedByAsHolding() {
        this.guardedField.field.toString();
        guardedField.field.toString();
    }

    @GuardedBy("lock") // JCIP GuardedBy applies to both the method and the return type, unfortunately.
    Object testGuardedByAsHolding2() {
        this.guardedField.field.toString();
        guardedField.field.toString();
        testGuardedByAsHolding();
        @GuardedBy("lock") Object o = new Object();
        return o;
    }

    void testGuardedByAsHolding3() {
        synchronized(lock) {
            testGuardedByAsHolding();
        }
        //:: error: (contracts.precondition.not.satisfied)
        testGuardedByAsHolding();
    }
}
