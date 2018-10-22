package org.checkerframework.checker.index.lowerbound;

import java.util.HashSet;
import java.util.LinkedHashSet;
import org.checkerframework.checker.index.inequality.LessThanChecker;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * A type-checker for preventing fixed-length sequences such as arrays or strings from being
 * accessed with values that are too low. Normally bundled as part of the Index Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SuppressWarningsKeys({"index", "lowerbound"})
public class LowerBoundChecker extends BaseTypeChecker {
    private HashSet<String> collectionBaseTypeNames;

    /**
     * A type-checker for preventing fixed-length sequences such as arrays or strings from being
     * accessed with values that are too low. Normally bundled as part of the Index Checker.
     */
    public LowerBoundChecker() {
        // These classes are bases for both mutable and immutable sequence collections, which
        // contain methods that change the length.
        // Lower bound checker warnings are skipped at uses of them.
        Class<?>[] collectionBaseClasses = {java.util.List.class, java.util.AbstractList.class};
        collectionBaseTypeNames = new HashSet<>(collectionBaseClasses.length);
        for (Class<?> collectionBaseClass : collectionBaseClasses) {
            collectionBaseTypeNames.add(collectionBaseClass.getName());
        }
    }

    @Override
    public boolean shouldSkipUses(String typeName) {
        if (collectionBaseTypeNames.contains(typeName)) {
            return true;
        }
        return super.shouldSkipUses(typeName);
    }

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        checkers.add(LessThanChecker.class);
        checkers.add(SearchIndexChecker.class);
        return checkers;
    }
}
