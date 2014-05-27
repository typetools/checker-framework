import org.checkerframework.checker.lock.qual.*;

public class Fields {

    @GuardedBy("lockingObject") Object locked;
    Object lockingObject;

    synchronized void wrongLock1() {
        // locking over wrong lock
        //:: error: (contracts.precondition.not.satisfied.field)
        locked.toString();    // error
    }

    synchronized void wrongLock2() {
        // locking over wrong lock
        synchronized(this) {
            //:: error: (contracts.precondition.not.satisfied.field)
            locked.toString();    // error
        }
    }

    void rightLock() {
        synchronized(lockingObject) {
            locked.toString();
        }

        // accessing after the synchronized object
        //:: error: (contracts.precondition.not.satisfied.field)
        locked.toString();    // error
    }

    @Holding("lockingObject")
    void usingHolding() {
        locked.toString();
    }

    @GuardedBy("this") Object lockedByThis;

    void wrongLocksb() {
        // without locking
        //:: error: (contracts.precondition.not.satisfied.field)
        lockedByThis.toString();    // error

        synchronized(Fields.class) {
            //:: error: (contracts.precondition.not.satisfied.field)
            lockedByThis.toString();    // error
        }
    }

    void rightLockb() {
        synchronized(this) {
            lockedByThis.toString();
        }

        // accessing after the synchronized object
        //:: error: (contracts.precondition.not.satisfied.field)
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
            //:: error: (contracts.precondition.not.satisfied.field)
            a.lockedByThis.toString();  // error
            //:: error: (contracts.precondition.not.satisfied.field)
            b.lockedByThis.toString();  // error
        }

        synchronized(a) {
            //:: error: (contracts.precondition.not.satisfied.field)
            lockedByThis.toString();    // error
            a.lockedByThis.toString();
            //:: error: (contracts.precondition.not.satisfied.field)
            b.lockedByThis.toString();  // error
        }

        synchronized(b) {
            //:: error: (contracts.precondition.not.satisfied.field)
            lockedByThis.toString();    // error
            //:: error: (contracts.precondition.not.satisfied.field)
            a.lockedByThis.toString();  // error
            b.lockedByThis.toString();
        }

    }
}