import org.checkerframework.checker.lock.qual.GuardedBy;

public class Simple {
    final Object lock1 = new Object(), lock2 = new Object();

    void testMethodCall(@GuardedBy("lock1") Simple this) {
        //:: error: (contracts.precondition.not.satisfied.field)
        synchronized (lock1) {}
        //:: error: (contracts.precondition.not.satisfied.field)
        synchronized (this.lock1) {}
        //:: error: (contracts.precondition.not.satisfied.field)
        synchronized (lock2) {}
        //:: error: (contracts.precondition.not.satisfied.field)
        synchronized (this.lock2) {}

        final @GuardedBy("myClass.field") MyClass myClass = new MyClass();
        //:: error: (contracts.precondition.not.satisfied.field)
        synchronized (myClass.field) {}
        synchronized (myClass) {}
    }

    class MyClass {
        final Object field = new Object();
    }
}
