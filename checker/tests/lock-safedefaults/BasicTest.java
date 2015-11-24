import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.framework.qual.*;
import java.util.concurrent.locks.*;

public class BasicTest {
    class MyClass {
        public Object field;
    }

    Object myUnannotatedMethod(Object param) {
        return param;
    }

    void myUnannotatedMethod2() {
    }

    @AnnotatedFor("lock")
    @GuardSatisfied Object myAnnotatedMethod(Object param) {
        return param;
    }

    @AnnotatedFor("lock")
    void myAnnotatedMethod2() {
    }

    ReentrantLock lockField = new ReentrantLock();
    @GuardedBy("lockField") MyClass m;

    Object o1 = new Object(), p1;

    @AnnotatedFor("lock")
    @MayReleaseLocks
    void testFields() {
        p1 = myUnannotatedMethod(o1);
        myAnnotatedMethod(o1);

        // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
        lockField.lock();
        myAnnotatedMethod2();
        m.field.toString();
        myUnannotatedMethod2();
        //:: error: (contracts.precondition.not.satisfied.field)
        m.field.toString();
    }

    void unannotatedReleaseLock(ReentrantLock lock) {
        lock.unlock();
    }

    // TODO: uncomment this method once issue 524 has been fixed.
    /* @AnnotatedFor("lock")
    @MayReleaseLocks
    void testLocalVariables() {
        Object o2 = new Object(), p2;
        p2 = myUnannotatedMethod(o2);
        myAnnotatedMethod(o2);

        // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
        ReentrantLock lock = new ReentrantLock();
        @GuardedBy("lock") MyClass q = new MyClass();
        lock.lock();
        myAnnotatedMethod2();
        q.field.toString();
        myUnannotatedMethod2(); // Should behave as @MayReleaseLocks, and *should* reset @LockHeld assumption about local variable lock.
        // TODO uncomment :: error: (contracts.precondition.not.satisfied.field)
        q.field.toString();
        lock.lock();
        unannotatedReleaseLock(lock); // Should behave as @MayReleaseLocks, and *should* reset @LockHeld assumption about local variable lock.
        // TODO uncomment :: error: (contracts.precondition.not.satisfied.field)
        q.field.toString();
    }*/
}
