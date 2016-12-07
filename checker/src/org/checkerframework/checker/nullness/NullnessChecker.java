package org.checkerframework.checker.nullness;

/** A concrete instantiation of {@link AbstractNullnessChecker} using freedom-before-commitment. */
public class NullnessChecker extends AbstractNullnessChecker {

    public NullnessChecker() {
        super(true);
    }
}
