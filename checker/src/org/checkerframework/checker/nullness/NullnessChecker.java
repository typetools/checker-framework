package org.checkerframework.checker.nullness;

import org.checkerframework.framework.qual.StubFiles;

/** A concrete instantiation of {@link AbstractNullnessChecker} using freedom-before-commitment. */
@StubFiles("javadoc.astub")
public class NullnessChecker extends AbstractNullnessChecker {

    public NullnessChecker() {
        super(true);
    }
}
