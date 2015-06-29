package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A concrete instantiation of {@link AbstractNullnessChecker} using rawness.
 */
@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        NonRaw.class, Raw.class, PolyNull.class, PolyAll.class })
public class AbstractNullnessRawnessChecker extends AbstractNullnessChecker {

    public AbstractNullnessRawnessChecker() {
        super(false);
    }

}
