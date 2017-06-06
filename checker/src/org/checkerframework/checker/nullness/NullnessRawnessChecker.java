package org.checkerframework.checker.nullness;

/** A concrete instantiation of {@link AbstractNullnessChecker} using rawness. */
public class NullnessRawnessChecker extends AbstractNullnessChecker {

    public NullnessRawnessChecker() {
        super(false);
    }
}
