import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;

public class SimpleLockTest {
    final Object lock1 = new Object(), lock2 = new Object();

    void testMethodCall(@GuardedBy("lock1") SimpleLockTest this) {
        // :: error: (lock.not.held)
        synchronized (lock1) {
        }
        // :: error: (lock.not.held)
        synchronized (this.lock1) {
        }
        // :: error: (lock.not.held)
        synchronized (lock2) {
        }
        // :: error: (lock.not.held)
        synchronized (this.lock2) {
        }

        @SuppressWarnings({
            "assignment",
            "method.invocation"
        }) // prevent flow-sensitive type refinement
        final @GuardedBy("myClass.field") MyClass myClass = someValue();
        // :: error: (lock.not.held)
        synchronized (myClass.field) {
        }
        synchronized (myClass) {
        }
    }

    @GuardedByUnknown MyClass someValue() {
        return new MyClass();
    }

    class MyClass {
        final Object field = new Object();
    }
}
