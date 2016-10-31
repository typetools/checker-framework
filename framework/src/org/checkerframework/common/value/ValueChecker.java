package org.checkerframework.common.value;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * @author plvines
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@StubFiles("statically-executable.astub")
@SupportedOptions(ValueChecker.REPORT_EVAL_WARNS)
public class ValueChecker extends BaseTypeChecker {
    public static final String REPORT_EVAL_WARNS = "reportEvalWarns";

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
    public boolean shouldResolveReflection() {
        // Because this checker is a subchecker of MethodVal,
        // reflection can't be resolved.
        return false;
    }
}
