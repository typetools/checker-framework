import org.checkerframework.checker.lock.qual.EnsuresLockHeld;
import org.checkerframework.checker.lock.qual.EnsuresLockHeldIf;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;

class ReentrantLock {
    final Object myfield = new Object();

    @Holding("myfield")
    @EnsuresLockHeld("this")
    void lock() {
        this.lock();
    }

    @EnsuresLockHeld("this")
    void lock2() {
        this.lock2();
    }

    @Holding("myfield")
    void notLock() {}

    boolean b = false;

    @EnsuresLockHeldIf(expression = "this", result = true)
    boolean tryLock() {
        if (b) {
            lock2();
            return true;
        }
        return false;
    }
}

class Foo {
    final ReentrantLock myLock = new ReentrantLock();

    @GuardedBy("myLock") Bar bar = new Bar();

    @Holding("myLock.myfield")
    void lockTheLock() {
        myLock.lock();
        bar.field.toString();
    }

    void lockTheLock2() {
        myLock.lock2();
        bar.field.toString();
    }

    void doNotLock() {
        //:: error: (contracts.precondition.not.satisfied.field)
        bar.field.toString();
    }

    void tryTryLock() {
        if (myLock.tryLock()) {
            bar.field.toString();
        } else {
            //:: error: (contracts.precondition.not.satisfied.field)
            bar.field.toString();
        }
    }
}

class Bar {
    Object field;
}
