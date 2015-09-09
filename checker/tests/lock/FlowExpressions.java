import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class BlockedByteArrayInputStream {
    private @GuardedBy({"itself"}) Object o;
    private @GuardedBy({"nonexistentfield"}) Object o2;
    @Pure
    private @GuardedBy({"itself"}) Object m() { return o; }

    public void method() {
        //:: error: (contracts.precondition.not.satisfied)
        m().toString();
        //:: error: (contracts.precondition.not.satisfied.field)
        o.toString();
        //:: error: (flowexpr.parse.error)
        o2.toString();
        synchronized(o) {
            o.toString();
        }
        synchronized(m()) {
            m().toString();
        }

        {
            Object itself = new Object();
            //:: error: (contracts.precondition.not.satisfied.field)
            o.toString();
            //:: error: (contracts.precondition.not.satisfied)
            m().toString();
            synchronized(itself){
                o.toString();
                m().toString();
            }
        }
    }
}
