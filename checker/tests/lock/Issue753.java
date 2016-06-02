// @skip-test

// Test case for Issue 753:
// https://github.com/typetools/checker-framework/issues/753

import org.checkerframework.dataflow.qual.Pure;
import java.util.concurrent.locks.ReentrantLock;

public class Issue753 extends ReentrantLock {
    final Issue753 field = new Issue753();
    @Pure Issue753 getField(Object param) { return field; }
    void method() {
        getField(field.field).field.lock();
    }
}
