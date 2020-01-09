// Test case for Issue 152:
// https://github.com/typetools/checker-framework/issues/152
// @skip-test

import org.checkerframework.checker.lock.qual.GuardedBy;

public class Issue152 {
    static class SuperClass {
        protected final Object myLock = new Object();

        private @GuardedBy("myLock") Object locked;
    }

    static class SubClass extends SuperClass {
        private final Object myLock = new Object();

        private @GuardedBy("myLock") Object locked;

        void method() {
            // :: error: (assignment.type.incompatible)
            this.locked = super.locked;
        }
    }

    class OuterClass {
        private final Object lock = new Object();

        @GuardedBy("this.lock") Object field;

        class InnerClass {
            private final Object lock = new Object();
            // :: error: (assignment.type.incompatible)
            @GuardedBy("this.lock") Object field2 = field;
        }
    }
}
