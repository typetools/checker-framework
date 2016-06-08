// Test case for Issue #770
// https://github.com/typetools/checker-framework/issues/770
// @skip-test
import org.checkerframework.checker.lock.qual.GuardedBy;

public class ViewpointAdaptation {
    //:: error: (flowexpr.parse.error)
    @GuardedBy("a")
    private final ViewpointAdaptation f = new ViewpointAdaptation();

    @GuardedBy("this.lock")
    private ViewpointAdaptation g = new ViewpointAdaptation();

    private final Object lock = new Object();

    private int counter;

    public void method1(final String a) {
        synchronized (a) {
            // The expression "a" from the @GuardedBy annotation
            // on f is not valid at the declaration site of f.
            //:: error: (flowexpr.parse.error)
            f.counter++;
        }
    }

    public void method2() {
        ViewpointAdaptation t = new ViewpointAdaptation();

        //:: error: (assignment.type.incompatible)
        t.g = g; // "t.lock" != "this.lock"

        synchronized (t.lock) {
            //:: error: (contracts.precondition.not.satisfied.field)
            g.counter++;
        }
    }

    public void method3() {
        final ViewpointAdaptation t = new ViewpointAdaptation();
        // The type of 'g' is refined from @GuardedByUnknown (the default for
        // a local variable due to CLIMB-to-top semantics) to @GuardedBy("t.g")
        final ViewpointAdaptation g = t.g;
        Object l = t.lock;

        synchronized (l) {
            // Aliasing of lock expressions is not tracked by the Lock Checker.
            // The Lock Checker does not know that l == t.lock
            //:: error: (contracts.precondition.not.satisfied.field)
            g.counter++;
        }
    }
}
