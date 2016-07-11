package org.checkerframework.checker.lowerbound;

import java.util.LinkedHashSet;

import org.checkerframework.common.value.ValueChecker;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker for preventing arrays from being accessed with
 * values that are too low.
 *
 * @checker_framework.manual #lowerbound-checker Lower Bound Checker
 */
public class LowerBoundChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers
            = super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
