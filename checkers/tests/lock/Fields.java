import checkers.lock.quals.*;

public class Fields {

    @GuardedBy("lockingObject") Object locked;
    Object lockingObject;

    synchronized void wrongLocks() {
        // without locking
        //:: error: (unguarded.access)
        locked.toString();    // error

        // locking over wrong lock
        synchronized(this) {
            //:: error: (unguarded.access)
            locked.toString();    // error
        }
    }

    void rightLock() {
        synchronized(lockingObject) {
            locked.toString();
        }

        // accessing after the synchronized object
        //:: error: (unguarded.access)
        locked.toString();    // error
    }

    @Holding("lockingObject")
    void usingHolding() {
        locked.toString();
    }

    @GuardedBy("this") Object lockedByThis;

    void wrongLocksb() {
        // without locking
        //:: error: (unguarded.access)
        lockedByThis.toString();    // error

        synchronized(Fields.class) {
            //:: error: (unguarded.access)
            lockedByThis.toString();    // error
        }
    }

    void rightLockb() {
        synchronized(this) {
            lockedByThis.toString();
        }

        // accessing after the synchronized object
        //:: error: (unguarded.access)
        lockedByThis.toString();    // error
    }

    synchronized void synchronizedMethodb() {
        lockedByThis.toString();
    }

    void test() {
        // synchronized over the right object
        Fields a = new Fields();
        Fields b = new Fields();

        synchronized(this) {
            lockedByThis.toString();
            //:: error: (unguarded.access)
            a.lockedByThis.toString();  // error
            //:: error: (unguarded.access)
            b.lockedByThis.toString();  // error
        }

        synchronized(a) {
            //:: error: (unguarded.access)
            lockedByThis.toString();    // error
            a.lockedByThis.toString();
            //:: error: (unguarded.access)
            b.lockedByThis.toString();  // error
        }

        synchronized(b) {
            //:: error: (unguarded.access)
            lockedByThis.toString();    // error
            //:: error: (unguarded.access)
            a.lockedByThis.toString();  // error
            b.lockedByThis.toString();
        }

    }
}
