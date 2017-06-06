package org.checkerframework.checker.index.upperbound;

import java.util.LinkedHashSet;
import org.checkerframework.checker.index.lowerbound.LowerBoundChecker;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
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
        checkers.add(SearchIndexChecker.class);
        checkers.add(SameLenChecker.class);
        checkers.add(LowerBoundChecker.class);
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
