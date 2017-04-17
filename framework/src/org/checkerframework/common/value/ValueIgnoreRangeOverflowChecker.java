package org.checkerframework.common.value;

import java.util.LinkedHashSet;
import java.util.Map;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * @author kelloggm
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@StubFiles("statically-executable.astub")
@SupportedOptions({ValueChecker.REPORT_EVAL_WARNS, ValueChecker.IGNORE_RANGE_OVERFLOW})
public class ValueIgnoreRangeOverflowChecker extends ValueChecker {
    public static final String REPORT_EVAL_WARNS = "reportEvalWarns";
    public static final String IGNORE_OVERFLOW_OPTION = "ignoreRangeOverflow";

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ValueVisitor(this);
    }

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        // Don't call super otherwise MethodVal will be added as a subChecker
        // which creates a circular dependency.
        return new LinkedHashSet<Class<? extends BaseTypeChecker>>();
    }

    @Override
    public Map<String, String> getOptions() {
        Map<String, String> options = super.getOptions();
        options.put(IGNORE_OVERFLOW_OPTION, null);
        return options;
    }

    @Override
    public boolean shouldResolveReflection() {
        // Because this checker is a subchecker of MethodVal,
        // reflection can't be resolved.
        return false;
    }
}
