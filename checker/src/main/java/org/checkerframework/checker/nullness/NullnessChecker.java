package org.checkerframework.checker.nullness;

import java.util.LinkedHashSet;
import java.util.SortedSet;
import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * An implementation of the nullness type-system, parameterized by an initialization type-system for
 * safe initialization. It uses freedom-before-commitment, augmented by type frames (which are
 * crucial to obtain acceptable precision), as its initialization type system.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SupportedLintOptions({
    NullnessChecker.LINT_NOINITFORMONOTONICNONNULL,
    NullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
    // Temporary option to forbid non-null array component types,
    // which is allowed by default.
    // Forbidding is sound and will eventually be the default.
    // Allowing is unsound, as described in Section 3.3.4, "Nullness and arrays":
    //     https://checkerframework.org/manual/#nullness-arrays
    // It is the default temporarily, until we improve the analysis to reduce false positives or we
    // learn what advice to give programmers about avoid false positive warnings.
    // See issue #986: https://github.com/typetools/checker-framework/issues/986
    "soundArrayCreationNullness",
    // Old name for soundArrayCreationNullness, for backward compatibility; remove in January 2021.
    "forbidnonnullarraycomponents",
    NullnessChecker.LINT_TRUSTARRAYLENZERO,
    NullnessChecker.LINT_PERMITCLEARPROPERTY
})
@SupportedOptions({NullnessChecker.ENABLE_REGEX_CHECKER})
public class NullnessChecker extends InitializationChecker {

    /** Should we be strict about initialization of {@link MonotonicNonNull} variables? */
    public static final String LINT_NOINITFORMONOTONICNONNULL = "noInitForMonotonicNonNull";

    /** Default for {@link #LINT_NOINITFORMONOTONICNONNULL}. */
    public static final boolean LINT_DEFAULT_NOINITFORMONOTONICNONNULL = false;

    /**
     * Warn about redundant comparisons of an expression with {@code null}, if the expression is
     * known to be non-null.
     */
    public static final String LINT_REDUNDANTNULLCOMPARISON = "redundantNullComparison";

    /** Default for {@link #LINT_REDUNDANTNULLCOMPARISON}. */
    public static final boolean LINT_DEFAULT_REDUNDANTNULLCOMPARISON = false;

    /**
     * Should the Nullness Checker unsoundly trust {@code @ArrayLen(0)} annotations to improve
     * handling of {@link java.util.Collection#toArray()} by {@link CollectionToArrayHeuristics}?
     */
    public static final String LINT_TRUSTARRAYLENZERO = "trustArrayLenZero";

    /** Default for {@link #LINT_TRUSTARRAYLENZERO}. */
    public static final boolean LINT_DEFAULT_TRUSTARRAYLENZERO = false;

    /**
     * If true, client code may clear system properties. If false (the default), some calls to
     * {@code System.getProperty} are refined to return @NonNull.
     */
    public static final String LINT_PERMITCLEARPROPERTY = "permitClearProperty";

    /** Default for {@link #LINT_PERMITCLEARPROPERTY}. */
    public static final boolean LINT_DEFAULT_PERMITCLEARPROPERTY = false;

    /**
     * If this option is specified, the Regex Checker is run as a subchecker of the Nullness Checker
     * to infer whether calls to {@code Matcher.group(int)} are valid or not.
     */
    public static final String ENABLE_REGEX_CHECKER = "enableRegexChecker";

    /**
     * Whether the {@code -AenableRegexChecker} option was passed on the command line. Do not access
     * this variable directly, instead call {@link #isRegexCheckerEnabled()}.
     */
    private @MonotonicNonNull Boolean regexCheckerEnabled = null;

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(KeyForSubchecker.class);
        if (isRegexCheckerEnabled()) {
            checkers.add(RegexChecker.class);
        }
        return checkers;
    }

    @Override
    public SortedSet<String> getSuppressWarningsPrefixes() {
        SortedSet<String> result = super.getSuppressWarningsPrefixes();
        result.add("nullness");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new NullnessVisitor(this);
    }

    /**
     * Was the Regex Checker enabled on the command line?
     *
     * @return whether the {@code -AenableRegexChecker} option was passed on the command line
     */
    private boolean isRegexCheckerEnabled() {
        if (regexCheckerEnabled == null) {
            regexCheckerEnabled =
                    this.processingEnv.getOptions().containsKey(ENABLE_REGEX_CHECKER)
                            || this.processingEnv
                                    .getOptions()
                                    .containsKey(
                                            this.getClass().getSimpleName()
                                                    + "_"
                                                    + ENABLE_REGEX_CHECKER);
        }
        return regexCheckerEnabled;
    }
}
