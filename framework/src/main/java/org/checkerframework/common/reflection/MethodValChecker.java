package org.checkerframework.common.reflection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;

import java.util.LinkedHashSet;

/**
 * The MethodVal Checker provides a sound estimate of the signature of Method objects.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
public class MethodValChecker extends BaseTypeChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new MethodValVisitor(this);
    }

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        // Don't call super otherwise MethodVal will be added as a subChecker
        // which creates a circular dependency.
        LinkedHashSet<Class<? extends BaseTypeChecker>> subCheckers = new LinkedHashSet<>();
        subCheckers.add(ValueChecker.class);
        subCheckers.add(ClassValChecker.class);
        return subCheckers;
    }
}
