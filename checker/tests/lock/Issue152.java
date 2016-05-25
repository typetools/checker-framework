// @skip-test

// Test case for Issue 152:
// https://github.com/typetools/checker-framework/issues/152

import org.checkerframework.checker.lock.qual.GuardedBy;

import java.util.concurrent.locks.ReentrantLock;

public class Issue152 {
     static class SuperClass {
         protected final Object myLock = new Object();

         @GuardedBy("myLock")
         private Object locked;
     }
     static class SubClass extends SuperClass {
         private final Object myLock = new Object();

         @GuardedBy("myLock")
         private Object locked;

         void method() {
             //:: error: (assignment.type.incompatible)
             this.locked = super.locked;
         }
     }
}
