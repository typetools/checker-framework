package org.checkerframework.checker.testchecker;

// Test case for Issue 343
// https://github.com/typetools/checker-framework/issues/343

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.i18n.I18nChecker;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

public class NestedAggregateChecker extends AggregateChecker {
  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = new LinkedHashSet<>(4);

    checkers.add(FenumChecker.class);
    checkers.add(I18nChecker.class); // The I18nChecker is an aggregate checker.
    checkers.add(NullnessChecker.class);
    checkers.add(RegexChecker.class);

    return checkers;
  }
}
