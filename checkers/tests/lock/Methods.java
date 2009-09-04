import checkers.lock.quals.*;

public class Methods {

    Object lock;

    @Holding("lock")
    void lockedByLock() { }

    @Holding("this")
    void lockedByThis() { }

    // unguarded calls
    void unguardedCalls() {
        lockedByLock();     // error
        lockedByThis();     // error
    }

    @Holding("lock")
    void usingHolding1() {
        lockedByLock();
        lockedByThis();     // error
    }

    @Holding("this")
    void usingHolding2() {
        lockedByLock();     // error
        lockedByThis();
    }

    void usingSynchronization1() {
        synchronized(lock) {
            lockedByLock();
            lockedByThis(); // error
        }
    }

    void usingSynchronization2() {
        synchronized(this) {
            lockedByLock(); // error
            lockedByThis();
        }
    }

    synchronized void usingMethodModifier() {
        lockedByLock();     // error
        lockedByThis();
    }

}
