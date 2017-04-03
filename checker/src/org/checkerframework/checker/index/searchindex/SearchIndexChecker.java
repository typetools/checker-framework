package org.checkerframework.checker.index.searchindex;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * An internal checker that assists the Index Checker in typing the results of calls to the JDK's
 * {@link java.util.Arrays#binarySearch(Object[],Object) Arrays.binarySearch} routine.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SuppressWarningsKeys({"index", "searchindex"})
public class SearchIndexChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
