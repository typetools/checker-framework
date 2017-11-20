import org.checkerframework.checker.lock.qual.GuardedBy;

public class NestedSynchronizedBlocks {
    class MyClass {
        public Object field;
    }

    @GuardedBy("lock1") MyClass m1;

    @GuardedBy("lock2") MyClass m2;

    @GuardedBy("lock3") MyClass m3;

    @GuardedBy("lock4") MyClass m4;

    final Object lock1 = new Object(),
            lock2 = new Object(),
            lock3 = new Object(),
            lock4 = new Object();

    void foo() {
        synchronized (lock1) {
            synchronized (lock2) {
                synchronized (lock3) {
                    synchronized (lock4) {
                    }
                }
            }
        }

        // Test that the locks are known to have been released.

        // :: error:(lock.not.held)
        m1.field = new Object();
        // :: error:(lock.not.held)
        m2.field = new Object();
        // :: error:(lock.not.held)
        m3.field = new Object();
        // :: error:(lock.not.held)
        m4.field = new Object();
    }
}
