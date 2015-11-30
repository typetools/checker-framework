import net.jcip.annotations.*;
import org.checkerframework.dataflow.qual.*;

// Smoke test for supporting JCIP annotations
public class JCIPAnnotations {

    Object lock;

    @GuardedBy("lock") Object guardedField;
    Object unguardedField;

    @LockingFree
    void guardedReceiver(@org.checkerframework.checker.lock.qual.GuardedBy("lock") JCIPAnnotations this) { }
    @LockingFree
    void unguardedReceiver(JCIPAnnotations this) { }

    @LockingFree
    void guardedArg(@GuardedBy("lock") Object arg) { }
    @LockingFree
    void unguardedArg(Object arg) { }

    @LockingFree
    static void guardedStaticArg(@GuardedBy("lock") Object x) { }
    @LockingFree
    static void unguardedStaticArg(Object x) { }

    void testUnguardedAccess(Object x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.toString();   // error
        this.unguardedField.toString();
        this.guardedReceiver();
        this.unguardedReceiver();
        this.guardedArg(x);
        this.unguardedArg(x);
        unguardedStaticArg(x);
        guardedStaticArg(x);
    }

    void testGuardedAccess(@org.checkerframework.checker.lock.qual.GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") Object x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.toString();
        //:: error: (contracts.precondition.not.satisfied)
        this.guardedReceiver();
        //:: error: (contracts.precondition.not.satisfied)
        this.unguardedReceiver();
        //:: error: (contracts.precondition.not.satisfied) :: error: (contracts.precondition.not.satisfied.field)
        this.guardedArg(x);
        //:: error: (contracts.precondition.not.satisfied) :: error: (contracts.precondition.not.satisfied.field)
        this.unguardedArg(x);
        //:: error: (contracts.precondition.not.satisfied.field)
        unguardedStaticArg(x);
        //:: error: (contracts.precondition.not.satisfied.field)
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.toString();
            this.unguardedField.toString();
            this.guardedReceiver();
            this.unguardedReceiver();
            this.guardedArg(x);
            this.unguardedArg(x);
            unguardedStaticArg(x);
            guardedStaticArg(x);
        }
    }

    void testSemiGuardedAccess(@org.checkerframework.checker.lock.qual.GuardedBy("lock") JCIPAnnotations this, Object x) {
        //:: error: (contracts.precondition.not.satisfied.field)
        this.guardedField.toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        this.unguardedField.toString();
        //:: error: (contracts.precondition.not.satisfied)
        this.guardedReceiver();
        //:: error: (contracts.precondition.not.satisfied)
        this.unguardedReceiver();
        //:: error: (contracts.precondition.not.satisfied)
        this.guardedArg(x);
        //:: error: (contracts.precondition.not.satisfied)
        this.unguardedArg(x);
        unguardedStaticArg(x);
        guardedStaticArg(x);
        synchronized(lock) {
            this.guardedField.toString();
            this.unguardedField.toString();
            this.guardedReceiver();
            this.unguardedReceiver();
            this.guardedArg(x);
            this.unguardedArg(x);
            unguardedStaticArg(x);
            guardedStaticArg(x);
        }
    }

}