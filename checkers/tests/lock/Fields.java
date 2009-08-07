import checkers.lock.quals.*;

public class Fields {

    @GuardedBy("lockingObject") Object locked;
    Object lockingObject;

    synchronized void wrongLocks() {
        // without locking
        locked.toString();    // error

        // locking over wrong lock
        synchronized(this) {
            locked.toString();    // error
        }
    }

    void rightLock() {
        synchronized(lockingObject) {
            locked.toString();
        }

        // accessing after the synchronized object
        locked.toString();    // error
    }

    @Holding("lockingObject")
    void usingHolding() {
        locked.toString();
    }

    @GuardedBy("this") Object lockedByThis;

    void wrongLocksb() {
        // without locking
        lockedByThis.toString();    // error

        synchronized(Fields.class) {
            lockedByThis.toString();    // error
        }
    }

    void rightLockb() {
        synchronized(this) {
            lockedByThis.toString();
        }

        // accessing after the synchronized object
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
            a.lockedByThis.toString();  // error
            b.lockedByThis.toString();  // error
        }

        synchronized(a) {
            lockedByThis.toString();    // error
            a.lockedByThis.toString();
            b.lockedByThis.toString();  // error
        }

        synchronized(b) {
            lockedByThis.toString();    // error
            a.lockedByThis.toString();  // error
            b.lockedByThis.toString();
        }

    }
}
