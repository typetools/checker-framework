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
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SupportedLintOptions;

/**
 * A concrete instantiation of {@link AbstractNullnessChecker} using
 * freedom-before-commitment.
 */
@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        UnderInitialization.class, Initialized.class, UnknownInitialization.class,
        FBCBottom.class, PolyNull.class, PolyAll.class })
@SupportedLintOptions({
        AbstractNullnessChecker.LINT_NOINITFORMONOTONICNONNULL,
        AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
        // Temporary option to forbid non-null array component types,
        // which is allowed by default.
        // Forbidding is sound and will eventually be the only possibility.
        // Allowing is unsound but permitted until flow-sensitivity changes are
        // made.
        // See issues 154 and 322.
        "arrays:forbidnonnullcomponents" })
@StubFiles("astubs/gnu-getopt.astub")
public class AbstractNullnessFbcChecker extends AbstractNullnessChecker {

    public AbstractNullnessFbcChecker() {
        super(true);
    }

}
