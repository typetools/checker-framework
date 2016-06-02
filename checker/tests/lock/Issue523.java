// Test case for Issue 523:
// https://github.com/typetools/checker-framework/issues/523

import org.checkerframework.checker.lock.qual.*;

class Issue523 {
    static class MyClass { Object field; }
    static final @GuardedBy("itself") MyClass m = new MyClass();

    static void foo() {
        Thread t = new Thread() {
            public void run() {
                synchronized(m) {
                    m.field = new Object();
                }
            }
        };
    }
}
