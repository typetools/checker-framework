// Test case for Issue 804:
// https://github.com/typetools/checker-framework/issues/804

import java.util.concurrent.locks.*;
import org.checkerframework.checker.lock.qual.*;

public class Issue804 extends ReentrantLock {
    @Holding("this")
    @MayReleaseLocks
    void bar() {
        this.unlock();
    }

    @Holding("this")
    @MayReleaseLocks
    void method() {
        bar();
        //:: error: (contracts.precondition.not.satisfied)
        bar();
    }
}
