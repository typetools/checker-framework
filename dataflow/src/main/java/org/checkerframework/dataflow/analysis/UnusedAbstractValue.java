package org.checkerframework.dataflow.analysis;

import org.checkerframework.javacutil.BugInCF;

/**
 * UnusedAbstractValue is an AbstractValue that is not involved in any lub computation during
 * dataflow analysis. For those analyses which handle lub computation at a higher level (e.g., store
 * level), it is sufficient to use UnusedAbstractValue and unnecessary to implement another specific
 * AbstractValue. Example analysis using UnusedAbstractValue is LiveVariable analysis. This is a
 * workaround for issue https://github.com/eisop/checker-framework/issues/200
 */
public final class UnusedAbstractValue implements AbstractValue<UnusedAbstractValue> {

    /** This class cannot be instantiated */
    private UnusedAbstractValue() {
        throw new AssertionError("Class UnusedAbstractValue cannot be instantiated.");
    }

    @Override
    public UnusedAbstractValue leastUpperBound(UnusedAbstractValue other) {
        throw new BugInCF("UnusedAbstractValue.leastUpperBound was called!");
    }
}
