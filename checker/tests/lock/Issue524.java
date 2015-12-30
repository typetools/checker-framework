import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;

//https://github.com/typetools/checker-framework/issues/524

import java.util.concurrent.locks.*;

// Note that this test is nondeterministic - it sometimes passes when it should fail.
class Issue524 {
    class MyClass { public Object field; }

    void testLocalVariables() {
        @GuardedBy("localLock") MyClass q = new MyClass();
        @GuardedBy({}) ReentrantLock localLock = new ReentrantLock();
        localLock.lock();
        q.field.toString();
    }
}
