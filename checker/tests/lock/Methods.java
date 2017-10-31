import org.checkerframework.checker.lock.qual.*;

public class Methods {

    final Object lock = new Object();

    @Holding("lock")
    void lockedByLock() {}

    @Holding("this")
    void lockedByThis() {}

    // unguarded calls
    void unguardedCalls() {
        // :: error: (contracts.precondition.not.satisfied)
        lockedByLock(); // error
        // :: error: (contracts.precondition.not.satisfied)
        lockedByThis(); // error
    }

    @Holding("lock")
    void usingHolding1() {
        lockedByLock();
        // :: error: (contracts.precondition.not.satisfied)
        lockedByThis(); // error
    }

    @Holding("this")
    void usingHolding2() {
        // :: error: (contracts.precondition.not.satisfied)
        lockedByLock(); // error
        lockedByThis();
    }

    void usingSynchronization1() {
        synchronized (lock) {
            lockedByLock();
            // :: error: (contracts.precondition.not.satisfied)
            lockedByThis(); // error
        }
    }

    void usingSynchronization2() {
        synchronized (this) {
            // :: error: (contracts.precondition.not.satisfied)
            lockedByLock(); // error
            lockedByThis();
        }
    }

    synchronized void usingMethodModifier() {
        // :: error: (contracts.precondition.not.satisfied)
        lockedByLock(); // error
        lockedByThis();
    }
}
