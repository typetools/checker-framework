package org.checkerframework.common.value;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The Constant Value Checker is a constant propagation analysis: for each variable, it determines
 * whether that variable's value can be known at compile time.
 *
 * <p>The Constant Value Checker has no dependencies, but it does trust {@code
 * org.checkerframework.checker.index.qual.Positive} annotations from the Index Checker. This means
 * that if the Value Checker is run on code containing {@code Positive} annotations, then the Index
 * Checker also needs to be run to guarantee soundness.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SupportedOptions({
    ValueChecker.REPORT_EVAL_WARNS,
    ValueChecker.IGNORE_RANGE_OVERFLOW,
    ValueChecker.NON_NULL_STRINGS_CONCATENATION
})
public class ValueChecker extends BaseTypeChecker {
    /**
     * Command-line option to warn the user if a @StaticallyExecutable method can't load and run at
     * compile time.
     */
    public static final String REPORT_EVAL_WARNS = "reportEvalWarns";
    /** Command-line option to ignore the possibility of overflow for range annotations. */
    public static final String IGNORE_RANGE_OVERFLOW = "ignoreRangeOverflow";
    /** Command-line option that assumes most expressions in String concatenations can be null. */
    public static final String NON_NULL_STRINGS_CONCATENATION = "nonNullStringsConcatenation";

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ValueVisitor(this);
    }

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        // Don't call super otherwise MethodVal will be added as a subChecker
        // which creates a circular dependency.
        return new LinkedHashSet<>();
    }

    @Override
    public boolean shouldResolveReflection() {
        // Because this checker is a subchecker of MethodVal,
        // reflection can't be resolved.
        return false;
    }

    @Override
    public void typeProcessingOver() {
        // Reset ignore overflow.
        Range.ignoreOverflow = false;
        super.typeProcessingOver();
    }
}
