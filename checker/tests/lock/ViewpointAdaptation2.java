import org.checkerframework.checker.lock.qual.GuardedBy;

import java.lang.Object;

public class ViewpointAdaptation2 {
    class LockExample {
        protected final Object myLock = new Object();

        @GuardedBy("myLock")
        protected Object locked;

        @GuardedBy("this.myLock")
        protected Object locked2;

        public @GuardedBy("myLock") Object getLocked() {
            return locked;
        }
    }

    class Use {
        final LockExample lockExample1 = new LockExample();
        final Object myLock = new Object();
        @GuardedBy("lockExample1.myLock") Object o1 = lockExample1.locked;
        @GuardedBy("lockExample1.myLock") Object o2 = lockExample1.locked2;
        //:: error: (assignment.type.incompatible)
        @GuardedBy("myLock") Object o3 = lockExample1.locked;
        //:: error: (assignment.type.incompatible)
        @GuardedBy("this.myLock") Object o4 = lockExample1.locked2;

        @GuardedBy("lockExample1.myLock") Object oM1 = lockExample1.getLocked();
        //:: error: (assignment.type.incompatible)
        @GuardedBy("myLock") Object oM2 = lockExample1.getLocked();
        //:: error: (assignment.type.incompatible)
        @GuardedBy("this.myLock") Object oM3 = lockExample1.getLocked();

        void uses(){
            lockExample1.locked = o1;
            lockExample1.locked = o3;
        }
    }
}