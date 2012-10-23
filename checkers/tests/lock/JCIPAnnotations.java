import net.jcip.annotations.*;

// Smoke test for supporting JCIP annotations
public class JCIPAnnotations {

    Object lock;

    @GuardedBy("lock") Object guardedField;
    Object unguardedField;

    void guardedReceiver(@GuardedBy("lock") JCIPAnnotations this) { }
    void unguardedReceiver(JCIPAnnotations this) { }

    void guardedArg(@GuardedBy("lock") Object arg) { }
    void unguardedArg(Object arg) { }

    static void guardedStaticArg(@GuardedBy("lock") Object x) { }
    static void unguardedStaticArg(Object x) { }

    void testUnguardedAccess(Object x) {
        //:: error: (unguarded.access)
        this.guardedField.toString();   // error
        this.unguardedField.toString();
        this.guardedReceiver();
        this.unguardedReceiver();
        this.guardedArg(x);
        this.unguardedArg(x);
        unguardedStaticArg(x);
        guardedStaticArg(x);
    }

    void testGuardedAccess(@GuardedBy("lock") JCIPAnnotations this, @GuardedBy("lock") Object x) {
        //:: error: (unguarded.access)
        this.guardedField.toString();
        //:: error: (unguarded.access)
        this.unguardedField.toString();
        //:: error: (unguarded.access)
        this.guardedReceiver();
        //:: error: (unguarded.access)
        this.unguardedReceiver();
        //:: error: (unguarded.access)
        this.guardedArg(x);
        //:: error: (unguarded.access) :: error: (argument.type.incompatible)
        this.unguardedArg(x);
        //:: error: (argument.type.incompatible)
        unguardedStaticArg(x);
        //:: error: (unguarded.access)
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

    void testSemiGuardedAccess(@GuardedBy("lock") JCIPAnnotations this, Object x) {
        //:: error: (unguarded.access)
        this.guardedField.toString();
        //:: error: (unguarded.access)
        this.unguardedField.toString();
        //:: error: (unguarded.access)
        this.guardedReceiver();
        //:: error: (unguarded.access)
        this.unguardedReceiver();
        //:: error: (unguarded.access)
        this.guardedArg(x);
        //:: error: (unguarded.access)
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
