import net.jcip.annotations.*;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.GuardSatisfied;

// Smoke test for supporting JCIP and Javax annotations
public class JCIPAnnotations {
  class MyClass {
     Object field = new Object();
     @LockingFree
     Object method(@GuardSatisfied MyClass this){return new Object();}
  }

    Object lock;

    @GuardedBy("lock") MyClass guardedField;
    MyClass unguardedField;

    @LockingFree
    void guardedReceiver(@GuardedBy("lock") JCIPAnnotations this) { }
    @LockingFree
    void unguardedReceiver() { }

    @LockingFree
    void guardedArg(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") MyClass arg) { }
    @LockingFree
    void guardedArgUnguardedReceiver(@GuardedBy("lock") MyClass arg) { }
    @LockingFree
    void unguardedArg(@GuardedBy("lock") JCIPAnnotations this, MyClass arg) { }
    @LockingFree
    void unguardedArgAndReceiver(MyClass arg) { }

    @LockingFree
    static void guardedStaticArg(@GuardedBy("lock") MyClass x) { }
    @LockingFree
    static void unguardedStaticArg(MyClass x) { }

    void testUnguardedAccess(MyClass x) {
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

    void testSemiGuardedAccess(@GuardedBy("lock") JCIPAnnotations this, MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
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

    void testSemiGuardedAccessAgainstJavaxGuardedBy(@javax.annotation.concurrent.GuardedBy("lock") JCIPAnnotations this, MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
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

    void testSemiGuardedAccessAgainstCheckerGuardedBy(@org.checkerframework.checker.lock.qual.GuardedBy("lock") JCIPAnnotations this, MyClass x) {
        //:: error: (contracts.precondition.not.satisfied.field)
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

}
