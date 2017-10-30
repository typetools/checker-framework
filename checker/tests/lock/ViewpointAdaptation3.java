// Test case for Issue #770
// https://github.com/typetools/checker-framework/issues/770

import org.checkerframework.checker.lock.qual.GuardedBy;

public class ViewpointAdaptation3 {

    class MyClass {
        Object field;
    }

    class LockExample {
        protected final Object myLock = new Object();

        protected @GuardedBy("myLock") MyClass locked;

        @GuardedBy("this.myLock") MyClass m1;

        protected @GuardedBy("this.myLock") MyClass locked2;

        public void accessLock() {
            synchronized (myLock) {
                this.locked.field = new Object();
            }
        }
    }

    class LockExampleSubclass extends LockExample {
        private final Object myLock = new Object();

        private @GuardedBy("this.myLock") MyClass locked;

        @GuardedBy("this.myLock") MyClass m2;

        public LockExampleSubclass() {
            final LockExampleSubclass les1 = new LockExampleSubclass();
            final LockExampleSubclass les2 = new LockExampleSubclass();
            final LockExampleSubclass les3 = les2;
            LockExample le1 = new LockExample();

            // :: error: (assignment.type.incompatible)
            les1.locked = le1.locked;
            // :: error: (assignment.type.incompatible)
            les1.locked = le1.locked2;

            // :: error: (assignment.type.incompatible)
            les1.locked = les2.locked;
        }
    }

    class Class1 {
        public final Object lock = new Object();

        @GuardedBy("lock") MyClass m = new MyClass();
    }

    class Class2 {
        public final Object lock = new Object();

        @GuardedBy("lock") MyClass m = new MyClass();

        void method(final Class1 a) {
            final Object lock = new Object();
            @GuardedBy("lock") MyClass local = new MyClass();

            // :: error: (assignment.type.incompatible)
            local = m;

            // :: error: (lock.not.held)
            local.field = new Object();

            synchronized (lock) {
                // :: error: (lock.not.held)
                a.m.field = new Object();
            }
            synchronized (this.lock) {
                // :: error: (lock.not.held)
                a.m.field = new Object();
            }
            synchronized (a.lock) {
                a.m.field = new Object();
            }

            synchronized (lock) {
                local.field = new Object();
            }
            synchronized (this.lock) {
                // :: error: (lock.not.held)
                local.field = new Object();
            }
            synchronized (a.lock) {
                // :: error: (lock.not.held)
                local.field = new Object();
            }

            synchronized (lock) {
                // :: error: (lock.not.held)
                this.m.field = new Object();
                // :: error: (lock.not.held)
                m.field = new Object();
            }
            synchronized (this.lock) {
                this.m.field = new Object();
                m.field = new Object();
            }
            synchronized (a.lock) {
                // :: error: (lock.not.held)
                this.m.field = new Object();
                // :: error: (lock.not.held)
                m.field = new Object();
            }
        }
    }
}
