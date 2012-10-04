import checkers.lock.quals.*;

public class Methods {

    Object lock;

    @Holding("lock")
    void lockedByLock() { }

    @Holding("this")
    void lockedByThis() { }

    // unguarded calls
    void unguardedCalls() {
        //:: error: (unguarded.invocation)
        lockedByLock();     // error
        //:: error: (unguarded.invocation)
        lockedByThis();     // error
    }

    @Holding("lock")
    void usingHolding1() {
        lockedByLock();
        //:: error: (unguarded.invocation)
        lockedByThis();     // error
    }

    @Holding("this")
    void usingHolding2() {
        //:: error: (unguarded.invocation)
        lockedByLock();     // error
        lockedByThis();
    }

    void usingSynchronization1() {
        synchronized(lock) {
            lockedByLock();
            //:: error: (unguarded.invocation)
            lockedByThis(); // error
        }
    }

    void usingSynchronization2() {
        synchronized(this) {
            //:: error: (unguarded.invocation)
            lockedByLock(); // error
            lockedByThis();
        }
    }

    synchronized void usingMethodModifier() {
        //:: error: (unguarded.invocation)
        lockedByLock();     // error
        lockedByThis();
    }

}
