package org.checkerframework.checker.index.upperbound;

import java.util.HashSet;
import java.util.LinkedHashSet;
import org.checkerframework.checker.index.inequality.LessThanChecker;
import org.checkerframework.checker.index.lowerbound.LowerBoundChecker;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
import org.checkerframework.checker.index.substringindex.SubstringIndexChecker;
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

    private HashSet<String> collectionBaseTypeNames;

    public UpperBoundChecker() {
        // These classes are bases for both mutable and immutable sequence collections, which
        // contain methods that change the length.
        // Upper bound checker warnings are skipped at uses of them.
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
        checkers.add(SubstringIndexChecker.class);
        checkers.add(SearchIndexChecker.class);
        checkers.add(SameLenChecker.class);
        checkers.add(LowerBoundChecker.class);
        checkers.add(ValueChecker.class);
        checkers.add(LessThanChecker.class);
        return checkers;
    }
}
