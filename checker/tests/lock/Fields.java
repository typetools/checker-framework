import org.checkerframework.checker.lock.qual.*;

public class Fields {
    class MyClass {
        public Object field;
    }

    static @GuardedBy("Fields.class") MyClass lockedStatically;

    static synchronized void ssMethod() {
        lockedStatically.field = new Object();
    }

    @GuardedBy("lockingObject") MyClass locked;
    final Object lockingObject = new Object();

    synchronized void wrongLock1() {
        // locking over wrong lock
        //:: error: (contracts.precondition.not.satisfied.field)
        locked.field = new Object();    // error
    }

    synchronized void wrongLock2() {
        // locking over wrong lock
        synchronized(this) {
            //:: error: (contracts.precondition.not.satisfied.field)
            locked.field = new Object();    // error
        }
    }

    void rightLock() {
        synchronized(lockingObject) {
            locked.field = new Object();
        }

        // accessing after the synchronized object
        //:: error: (contracts.precondition.not.satisfied.field)
        locked.field = new Object();    // error
    }

    @Holding("lockingObject")
    void usingHolding() {
        locked.field = new Object();
    }

    @GuardedBy("this") MyClass lockedByThis;

    void wrongLocksb() {
        // without locking
        //:: error: (contracts.precondition.not.satisfied.field)
        lockedByThis.field = new Object();    // error

        synchronized(Fields.class) {
            //:: error: (contracts.precondition.not.satisfied.field)
            lockedByThis.field = new Object();    // error
        }
    }

    void rightLockb() {
        synchronized(this) {
            lockedByThis.field = new Object();
        }

        // accessing after the synchronized object
        //:: error: (contracts.precondition.not.satisfied.field)
        lockedByThis.field = new Object();    // error
    }

    synchronized void synchronizedMethodb() {
        lockedByThis.field = new Object();
    }

    void test() {
        // synchronized over the right object
        final Fields a = new Fields();
        final Fields b = new Fields();

        synchronized(this) {
            lockedByThis.field = new Object();
            //:: error: (contracts.precondition.not.satisfied.field)
            a.lockedByThis.field = new Object();  // error
            //:: error: (contracts.precondition.not.satisfied.field)
            b.lockedByThis.field = new Object();  // error
        }

        synchronized(a) {
            //:: error: (contracts.precondition.not.satisfied.field)
            lockedByThis.field = new Object();    // error
            a.lockedByThis.field = new Object();
            //:: error: (contracts.precondition.not.satisfied.field)
            b.lockedByThis.field = new Object();  // error
        }

        synchronized(b) {
            //:: error: (contracts.precondition.not.satisfied.field)
            lockedByThis.field = new Object();    // error
            //:: error: (contracts.precondition.not.satisfied.field)
            a.lockedByThis.field = new Object();  // error
            b.lockedByThis.field = new Object();
        }

    }
}
