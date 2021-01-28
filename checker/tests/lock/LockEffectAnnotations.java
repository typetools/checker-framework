import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class LockEffectAnnotations {
    class MyClass {
        Object field = new Object();
    }

    private @GuardedBy({}) MyClass myField;

    private final ReentrantLock myLock2 = new ReentrantLock();
    private @GuardedBy("myLock2") MyClass x3;

    // This method does not use locks or synchronization but cannot
    // be annotated as @SideEffectFree since it alters myField.
    @LockingFree
    void myMethod5() {
        myField = new MyClass();
    }

    @SideEffectFree
    int mySideEffectFreeMethod() {
        return 0;
    }

    @MayReleaseLocks
    void myUnlockingMethod() {
        myLock2.unlock();
    }

    @MayReleaseLocks
    void myReleaseLocksEmptyMethod() {}

    @MayReleaseLocks
    // :: error: (guardsatisfied.with.mayreleaselocks)
    void methodGuardSatisfiedReceiver(@GuardSatisfied LockEffectAnnotations this) {}

    @MayReleaseLocks
    // :: error: (guardsatisfied.with.mayreleaselocks)
    void methodGuardSatisfiedParameter(@GuardSatisfied Object o) {}

    @MayReleaseLocks
    void myOtherMethod() {
        if (myLock2.tryLock()) {
            x3.field = new Object(); // OK: the lock is held
            myMethod5();
            x3.field = new Object(); // OK: the lock is still held since myMethod is locking-free
            mySideEffectFreeMethod();
            x3.field = new Object(); // OK: the lock is still held since mySideEffectFreeMethod is
            // side-effect-free
            myUnlockingMethod();
            // :: error: (lock.not.held)
            x3.field = new Object(); // ILLEGAL: myLockingMethod is not locking-free
        }
        if (myLock2.tryLock()) {
            x3.field = new Object(); // OK: the lock is held
            myReleaseLocksEmptyMethod();
            // :: error: (lock.not.held)
            x3.field =
                    new Object(); // ILLEGAL: even though myUnannotatedEmptyMethod is empty, since
            // myReleaseLocksEmptyMethod() is annotated with @MayReleaseLocks and the Lock Checker
            // no longer knows the state of the lock.
            if (myLock2.isHeldByCurrentThread()) {
                x3.field = new Object(); // OK: the lock is known to be held
            }
        }
    }

    @ReleasesNoLocks
    void innerClassTest() {
        class InnerClass {
            @MayReleaseLocks
            void innerClassMethod() {}
        }

        InnerClass ic = new InnerClass();
        // :: error: (method.guarantee.violated)
        ic.innerClassMethod();
    }

    @MayReleaseLocks
    synchronized void mayReleaseLocksSynchronizedMethod() {}

    @ReleasesNoLocks
    synchronized void releasesNoLocksSynchronizedMethod() {}

    @LockingFree
    // :: error: (lockingfree.synchronized.method)
    synchronized void lockingFreeSynchronizedMethod() {}

    @SideEffectFree
    // :: error: (lockingfree.synchronized.method)
    synchronized void sideEffectFreeSynchronizedMethod() {}

    @Pure
    // :: error: (lockingfree.synchronized.method)
    synchronized void pureSynchronizedMethod() {}

    @MayReleaseLocks
    void mayReleaseLocksMethodWithSynchronizedBlock() {
        synchronized (this) {
        }
    }

    @ReleasesNoLocks
    void releasesNoLocksMethodWithSynchronizedBlock() {
        synchronized (this) {
        }
    }

    @LockingFree
    void lockingFreeMethodWithSynchronizedBlock() {
        // :: error: (synchronized.block.in.lockingfree.method)
        synchronized (this) {
        }
    }

    @SideEffectFree
    void sideEffectFreeMethodWithSynchronizedBlock() {
        // :: error: (synchronized.block.in.lockingfree.method)
        synchronized (this) {
        }
    }

    @Pure
    void pureMethodWithSynchronizedBlock() {
        // :: error: (synchronized.block.in.lockingfree.method)
        synchronized (this) {
        }
    }

    @GuardedByUnknown class MyClass2 {}

    // :: error: (expression.unparsable.type.invalid) :: error: (super.invocation.invalid)
    // :: warning: (inconsistent.constructor.type)
    @GuardedBy("lock") class MyClass3 {}

    @GuardedBy({}) class MyClass4 {}
    // :: error: (guardsatisfied.location.disallowed) :: error: (super.invocation.invalid)
    // :: warning: (inconsistent.constructor.type)
    @GuardSatisfied class MyClass5 {}

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @GuardedByBottom class MyClass6 {}
}
