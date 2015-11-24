import org.checkerframework.checker.lock.qual.GuardedBy;

public class NestedSynchronizedBlocks {
    class MyClass {
        public Object field;
    }

    @GuardedBy("lock1") MyClass m1;
    @GuardedBy("lock2") MyClass m2;
    @GuardedBy("lock3") MyClass m3;
    @GuardedBy("lock4") MyClass m4;

    Object lock1, lock2, lock3, lock4;

    void foo() {
        synchronized(lock1) {
            synchronized(lock2) {
                synchronized(lock3) {
                    synchronized(lock4) {
                    }
                }
            }
        }

        // Test that the locks are known to have been released.

        //:: error:(contracts.precondition.not.satisfied.field)
        m1.field = new Object();
        //:: error:(contracts.precondition.not.satisfied.field)
        m2.field = new Object();
        //:: error:(contracts.precondition.not.satisfied.field)
        m3.field = new Object();
        //:: error:(contracts.precondition.not.satisfied.field)
        m4.field = new Object();
    }
}
