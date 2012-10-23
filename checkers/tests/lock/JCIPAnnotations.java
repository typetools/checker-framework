import net.jcip.annotations.*;

// Smoke test for supporting JCIP annotations
public class JCIPAnnotations {

    Object unguardedField;
    void unguardedMethod() { }

    Object lock;
    @GuardedBy("lock") Object guardedField;

    void guardedMethod(@GuardedBy("lock") JCIPAnnotations this) { }

    static
    void guardedArgument(@GuardedBy("lock") Object x) { }

    void testUnguardedAccess(Object x) {
        this.unguardedField.toString();
        //:: error: (unguarded.access)
        this.guardedField.toString();   // error
        this.unguardedMethod();
        // TODO: Why isn't this error being reported? //:: error: (unguarded.invocation)
        this.guardedMethod();   // error
        // TODO: Why isn't this error being reported?
        guardedArgument(x);   // error
    }

    void testGuardedAccess(@GuardedBy("lock") Object x) {
        synchronized(lock) {
            this.unguardedField.toString();
            this.guardedField.toString();
            this.unguardedMethod();
            this.guardedMethod();
            guardedArgument(x);
        }
    }
}
