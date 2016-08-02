package org.checkerframework.checker.minlen;

import java.util.LinkedHashSet;

import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.common.value.ValueChecker;

/**
 * An internal checker that collects information about the minimum
 * lengths of arrays. It is used by the Upper Bound Checker.
 *
 * @checker_framework.manual #minlen-checker MinLen Checker
 */
public class MinLenChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers
            = super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
