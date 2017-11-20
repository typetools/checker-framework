package chapter;

import org.checkerframework.checker.lock.qual.GuardedBy;

class TestConcurrentSemantics2 {
    final Object a = new Object();
    final Object b = new Object();

    @GuardedBy("a") Object o;

    void method() {
        o = null;
        // Assume the following happens:
        //  * Context switch to a different thread.
        //  * bar() is called on the other thread.
        //  * Context switch back to this thread.
        // o is no longer null and an assignment.type.incompatible error should be issued.
        // :: error: (assignment.type.incompatible)
        @GuardedBy("b") Object o2 = o;
    }

    void bar() {
        o = new Object();
    }

    // Test that field assignments do not cause their type to be refined:
    @GuardedBy("a") Object myObject1 = null;
    // :: error: (assignment.type.incompatible)
    @GuardedBy("b") Object myObject2 = myObject1;
}
