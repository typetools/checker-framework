import net.jcip.annotations.*;

// Smoke test for supporting JCIP annotations
public class JCIPAnnotations {

    Object unguardedField;
    void unguardedMethod() { }

    Object lock;
    @GuardedBy("lock") Object guardedField;

    @GuardedBy("lock")
    void guardedMethod() { }

    void testUnguardedAccess() {
        this.unguardedField.toString();
        //:: error: (unguarded.access)
        this.guardedField.toString();   // error
        this.unguardedMethod();
        //:: error: (unguarded.invocation)
        this.guardedMethod();   // error
    }

    void testGuardedAccess() {
        synchronized(lock) {
            this.unguardedField.toString();
            this.guardedField.toString();
            this.unguardedMethod();
            this.guardedMethod();
        }
    }
}
