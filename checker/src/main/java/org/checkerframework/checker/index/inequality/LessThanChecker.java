package org.checkerframework.checker.index.inequality;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * An internal checker that estimates which expression's values are less than other expressions'
 * values.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SuppressWarningsKeys({"index", "lessthan"})
public class LessThanChecker extends BaseTypeChecker {
    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
