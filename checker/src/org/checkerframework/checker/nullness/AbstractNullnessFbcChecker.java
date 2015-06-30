package org.checkerframework.checker.nullness;

import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A concrete instantiation of {@link AbstractNullnessChecker} using
 * freedom-before-commitment.
 */
@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        UnderInitialization.class, Initialized.class, UnknownInitialization.class,
        FBCBottom.class, PolyNull.class, PolyAll.class })
public class AbstractNullnessFbcChecker extends AbstractNullnessChecker {

    public AbstractNullnessFbcChecker() {
        super(true);
    }

}
