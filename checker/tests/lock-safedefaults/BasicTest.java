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

    @AnnotatedFor("lock")
    @MayReleaseLocks
    void test() {
        Object o = new Object(), p;
        //:: error: (assignment.type.incompatible) :: error: (argument.type.incompatible)
        p = myUnannotatedMethod(o);
        myAnnotatedMethod(o);

        // Now test that an unannotated method behaves as if it's annotated with @MayReleaseLocks
        ReentrantLock lock = new ReentrantLock();
        @GuardedBy("lock") MyClass q = new MyClass();
        lock.lock();
        myAnnotatedMethod2();
        q.field.toString();
        myUnannotatedMethod2();
        //:: error: (contracts.precondition.not.satisfied.field)
        q.field.toString();
    }
}
