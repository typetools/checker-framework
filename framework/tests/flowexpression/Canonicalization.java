import testlib.flowexpression.qual.FlowExp;

public class Canonicalization {

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

        @FlowExp("lockExample1.myLock") Object o1 = lockExample1.locked;

        @FlowExp("lockExample1.myLock") Object o2 = lockExample1.locked2;

        @FlowExp("myLock")
        //:: error: (assignment.type.incompatible)
        Object o3 = lockExample1.locked;

        @FlowExp("this.myLock")
        //:: error: (assignment.type.incompatible)
        Object o4 = lockExample1.locked2;

        @FlowExp("lockExample1.myLock") Object oM1 = lockExample1.getLocked();

        @FlowExp("myLock")
        //:: error: (assignment.type.incompatible)
        Object oM2 = lockExample1.getLocked();

        @FlowExp("this.myLock")
        //:: error: (assignment.type.incompatible)
        Object oM3 = lockExample1.getLocked();
    }
}
