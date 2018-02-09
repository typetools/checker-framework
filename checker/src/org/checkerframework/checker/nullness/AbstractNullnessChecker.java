package org.checkerframework.checker.nullness;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;

/**
 * An implementation of the nullness type-system, parameterized by an initialization type-system for
 * safe initialization. It can use freedom-before-commitment or rawness as its initialization type
 * system.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SupportedLintOptions({
    AbstractNullnessChecker.LINT_NOINITFORMONOTONICNONNULL,
    AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
    // Temporary option to forbid non-null array component types,
    // which is allowed by default.
    // Forbidding is sound and will eventually be the default.
    // Allowing is unsound, as described in Section 3.3.4, "Nullness and arrays":
    //     https://checkerframework.org/manual/#nullness-arrays
    // It is the default temporarily, until we improve the analysis to reduce false positives or we
    // learn what advice to give programmers about avoid false positive warnings.
    // See issue #986: https://github.com/typetools/checker-framework/issues/986
    "forbidnonnullarraycomponents"
})
public abstract class AbstractNullnessChecker extends InitializationChecker {

    /** Should we be strict about initialization of {@link MonotonicNonNull} variables. */
    public static final String LINT_NOINITFORMONOTONICNONNULL = "noInitForMonotonicNonNull";

    /** Default for {@link #LINT_NOINITFORMONOTONICNONNULL}. */
    public static final boolean LINT_DEFAULT_NOINITFORMONOTONICNONNULL = false;

    /**
     * Warn about redundant comparisons of expressions with {@code null}, if the expressions is
     * known to be non-null.
     */
    public static final String LINT_REDUNDANTNULLCOMPARISON = "redundantNullComparison";

    /** Default for {@link #LINT_REDUNDANTNULLCOMPARISON}. */
    public static final boolean LINT_DEFAULT_REDUNDANTNULLCOMPARISON = false;

    public AbstractNullnessChecker(boolean useFbc) {
        super(useFbc);
    }

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(KeyForSubchecker.class);
        return checkers;
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        result.add("nullness");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new NullnessVisitor(this, useFbc);
    }
}
