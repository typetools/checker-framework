package org.checkerframework.checker.testchecker;

// Test case for Issue 343
// https://github.com/typetools/checker-framework/issues/343

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.i18n.I18nChecker;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

public class NestedAggregateChecker extends AggregateChecker {
    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        ArrayList<Class<? extends SourceChecker>> list =
                new ArrayList<Class<? extends SourceChecker>>();

        list.add(FenumChecker.class);
        list.add(I18nChecker.class); // The I18nChecker is an aggregate checker
        list.add(NullnessChecker.class);
        list.add(RegexChecker.class);

        return list;
    }
}
