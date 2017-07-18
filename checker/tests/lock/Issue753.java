// Test case for Issue 753:
// https://github.com/typetools/checker-framework/issues/753

import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class Issue753 extends ReentrantLock {
    final Issue753 field = new Issue753();

    @Pure
    Issue753 getField(Object param) {
        return field;
    }

    @Pure
    Issue753 getField2() {
        return field;
    }

    void method() {
        getField(field.field).field.lock();
        method2();
        getField(field.field).getField(field.field).field.lock();
        method3();
        getField(field.field).getField2().field.lock();
        method4();
        getField2().getField2().field.lock();
        method5();
        getField2().getField2().lock();
        method6();
        getField(getField(getField2()).field).field.lock();
        method7();
    }

    @Holding("this.getField(this.field.field).field")
    void method2() {}

    @Holding("this.getField(this.field.field).getField(this.field.field).field")
    void method3() {}

    @Holding("this.getField(this.field.field).getField2().field")
    void method4() {}

    @Holding("this.getField2().getField2().field")
    void method5() {}

    @Holding("this.getField2().getField2()")
    void method6() {}

    @Holding("this.getField(this.getField(this.getField2()).field).field")
    void method7() {}
}
