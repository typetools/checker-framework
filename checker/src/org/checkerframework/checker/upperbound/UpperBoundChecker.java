package org.checkerframework.checker.upperbound;

import java.util.LinkedHashSet;
import org.checkerframework.checker.minlen.MinLenChecker;
import org.checkerframework.checker.samelen.SameLenChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * A type-checker for preventing arrays from being accessed with values that are too high.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SuppressWarningsKeys({"index", "upperbound"})
public class UpperBoundChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        checkers.add(MinLenChecker.class);
        checkers.add(SameLenChecker.class);
        return checkers;
    }
}
