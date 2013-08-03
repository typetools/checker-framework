package daikon.tools.runtimechecker;

import java.util.ArrayList;
import java.util.List;

/**
 * If a class has been instrumented with the instrumenter (
 * invariant violations are added to the <code>violations</code> list.
 *
 */
public class Runtime {

    /**
     * A list of throwables seen when attempting to evaluate properties.
     */
    public static List<Throwable> internalInvariantEvaluationErrors = new ArrayList<Throwable>();

    private static List<Violation> violations = new ArrayList<Violation>();

    // The number of times that an invariant was checked (whether the
    // check succeeded or failed).
    public static long numEvaluations = 0;

    // The number of entry program points traversed.
    public static long numPptEntries = 0;

    // The number of normal-exit program points traversed.
    public static long numNormalPptExits = 0;

    // The number of exceptional-exit program points traversed.
    public static long numExceptionalPptExits = 0;

    /**
     * Returns the list of violations.
     */
    public static synchronized List<Violation> getViolations() {
	List<Violation> retval = new ArrayList<Violation>();
	for (Violation v : violations) {
	    retval.add(v);
	}
	return retval;
    }

    /**
     * Empty the violations list.
     */
    public static synchronized void resetViolations() {
	violations = new ArrayList<Violation>();
    }

    /**
     * True if the violations list is empty.
     */
    public static synchronized boolean violationsEmpty() {
	return violations.isEmpty();
    }

    /**
     * Add a violation to the violations list.
     */
    public static synchronized void violationsAdd(Violation v) {
	violations.add(v);
    }

}
