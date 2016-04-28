import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class FlowExpressions {
    class MyClass {
        public Object field;
    }

    final private @GuardedBy({"itself"}) MyClass m = new MyClass();
    //private @GuardedBy({"nonexistentfield"}) MyClass m2;
    @Pure
    private @GuardedBy({"itself"}) MyClass getm() { return m; }

    public void method() {
        //:: error: (contracts.precondition.not.satisfied)
        getm().field = new Object();
        //:: error: (contracts.precondition.not.satisfied.field)
        m.field = new Object();
        // TODO: fix the Lock Checker code so that a flowexpr.parse.error is issued (due to the guard of "nonexistentfield" on m2)
        // m2.field = new Object();
        synchronized(m) {
            m.field = new Object();
        }
        synchronized(getm()) {
            getm().field = new Object();
        }

        {
            final Object itself = new Object(); // Test the flow expression parser's ability to find an object named 'itself'
            //:: error: (contracts.precondition.not.satisfied.field)
            m.field = new Object();
            //:: error: (contracts.precondition.not.satisfied)
            getm().field = new Object();
            synchronized(itself) {
                m.field = new Object();
                getm().field = new Object();
            }
        }
    }
}
