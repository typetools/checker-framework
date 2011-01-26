import checkers.lock.quals.*;

public class Methods {

    Object lock;

    @Holding("lock")
    void lockedByLock() { }

    @Holding("this")
    void lockedByThis() { }

    // unguarded calls
    void unguardedCalls() {
        //:: (unguarded.invocation)
        lockedByLock();     // error
        //:: (unguarded.invocation)
        lockedByThis();     // error
    }

    @Holding("lock")
    void usingHolding1() {
        lockedByLock();
        //:: (unguarded.invocation)
        lockedByThis();     // error
    }

    @Holding("this")
    void usingHolding2() {
        //:: (unguarded.invocation)
        lockedByLock();     // error
        lockedByThis();
    }

    void usingSynchronization1() {
        synchronized(lock) {
            lockedByLock();
            //:: (unguarded.invocation)
            lockedByThis(); // error
        }
    }

    void usingSynchronization2() {
        synchronized(this) {
            //:: (unguarded.invocation)
            lockedByLock(); // error
            lockedByThis();
        }
    }

    synchronized void usingMethodModifier() {
        //:: (unguarded.invocation)
        lockedByLock();     // error
        lockedByThis();
    }

}
