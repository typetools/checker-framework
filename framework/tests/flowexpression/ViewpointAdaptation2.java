import testlib.flowexpression.qual.FlowExp;

public class ViewpointAdaptation2 {
    class LockExample {
        protected final Object myLock = new Object();

        protected @FlowExp("myLock") Object locked;

        protected @FlowExp("this.myLock") Object locked2;

        public @FlowExp("myLock") Object getLocked() {
            return locked;
        }
    }

    class Use {
        final LockExample lockExample1 = new LockExample();
        final Object myLock = new Object();

        @FlowExp("lockExample1.myLock")
        Object o1 = lockExample1.locked;

        @FlowExp("lockExample1.myLock")
        Object o2 = lockExample1.locked2;
        //:: error: (assignment.type.incompatible)
        @FlowExp("myLock")
        Object o3 = lockExample1.locked;
        //:: error: (assignment.type.incompatible)
        @FlowExp("this.myLock")
        Object o4 = lockExample1.locked2;

        @FlowExp("lockExample1.myLock")
        Object oM1 = lockExample1.getLocked();
        //:: error: (assignment.type.incompatible)
        @FlowExp("myLock")
        Object oM2 = lockExample1.getLocked();
        //:: error: (assignment.type.incompatible)
        @FlowExp("this.myLock")
        Object oM3 = lockExample1.getLocked();

        void uses() {
            lockExample1.locked = o1;
            //:: error: (assignment.type.incompatible)
            lockExample1.locked = o3;
        }
    }
}
