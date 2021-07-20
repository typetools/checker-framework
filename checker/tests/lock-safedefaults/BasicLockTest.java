import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.framework.qual.AnnotatedFor;

import java.util.concurrent.locks.*;

public class BasicLockTest {
    class MyClass {
        public Object field;
    }

    MyClass myUnannotatedMethod(MyClass param) {
        return param;
    }

    void myUnannotatedMethod2() {}

    @AnnotatedFor("lock")
    MyClass myAnnotatedMethod(MyClass param) {
        return param;
    }

    @AnnotatedFor("lock")
    void myAnnotatedMethod2() {}

    final @GuardedBy({}) ReentrantLock lockField = new ReentrantLock();

    @GuardedBy("lockField") MyClass m;

    @GuardedBy({}) MyClass o1 = new MyClass(), p1;

    @AnnotatedFor("lock")
    @MayReleaseLocks
    void testFields() {
        // Test in two ways that return values are @GuardedByUnknown.
        // The first way is more durable as cannot.dereference is tied specifically to
        // @GuardedByUnknown (and @GuardedByBottom, but it is unlikely to become the default for
        // return values on unannotated methods).
        // :: error: (lock.not.held) :: error: (argument.type.incompatible)
        myUnannotatedMethod(o1).field = new Object();
        // The second way is less durable because the default for fields is currently @GuardedBy({})
        // but could be changed to @GuardedByUnknown.
        // :: error: (assignment.type.incompatible) :: error: (argument.type.incompatible)
        p1 = myUnannotatedMethod(o1);

        // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
        lockField.lock();
        myAnnotatedMethod2();
        m.field = new Object();
        myUnannotatedMethod2();
        // :: error: (lock.not.held)
        m.field = new Object();
    }

    void unannotatedReleaseLock(ReentrantLock lock) {
        lock.unlock();
    }

    @AnnotatedFor("lock")
    @MayReleaseLocks
    void testLocalVariables() {
        MyClass o2 = new MyClass(), p2;
        // :: error: (argument.type.incompatible)
        p2 = myUnannotatedMethod(o2);
        MyClass o3 = new MyClass();
        myAnnotatedMethod(o3);

        // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
        final @GuardedBy({}) ReentrantLock lock = new ReentrantLock();
        @GuardedBy("lock") MyClass q = new MyClass();
        lock.lock();
        myAnnotatedMethod2();
        q.field = new Object();
        // Should behave as @MayReleaseLocks, and *should* reset @LockHeld assumption about local
        // variable lock.
        myUnannotatedMethod2();
        // :: error: (lock.not.held)
        q.field = new Object();
        lock.lock();
        // Should behave as @MayReleaseLocks, and *should* reset @LockHeld assumption about local
        // variable lock.
        // :: error: (argument.type.incompatible)
        unannotatedReleaseLock(lock);
        // :: error: (lock.not.held)
        q.field = new Object();
    }
}
