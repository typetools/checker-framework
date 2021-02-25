import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;

public class TestConcurrentSemantics1 {
    /* This class tests the following critical scenario.
     *
     * Suppose the following lines from method1 are executed on thread A.
     *
     * <pre>{@code
     * @GuardedBy("lock1") MyClass local;
     * m = local;
     * }</pre>
     *
     * Then a context switch occurs to method2 on thread B and the following lines are executed:
     *
     * <pre>{@code
     * @GuardedBy("lock2") MyClass local;
     * m = local;
     * }</pre>
     *
     * Then a context switch back to method1 on thread A occurs and the following lines are executed:
     *
     * <pre>{@code
     * lock1.lock();
     * m.field = new Object();
     * }</pre>
     *
     * In this case, it is absolutely critical that the dereference above not be allowed.
     *
     */

    @GuardedByUnknown MyClass m;
    final ReentrantLock lock1 = new ReentrantLock();
    final ReentrantLock lock2 = new ReentrantLock();

    void method1() {
        @GuardedBy("lock1") MyClass local = new MyClass();
        m = local;
        lock1.lock();
        // :: error: (lock.not.held)
        m.field = new Object();
    }

    void method2() {
        @GuardedBy("lock2") MyClass local = new MyClass();
        m = local;
    }

    class MyClass {
        Object field = new Object();
    }
}
