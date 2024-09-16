package org.checkerframework.checker.i18n;

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * A type-checker that enforces (and finds the violations of) two properties:
 *
 * <ol>
 *   <li value="1">Only localized output gets emitted to the user
 *   <li value="2">Only localizable keys (i.e. keys found in localizing resource bundles) get used
 *       as such.
 * </ol>
 *
 * @see I18nSubchecker
 * @see LocalizableKeyChecker
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
public class I18nChecker extends AggregateChecker {

  @Override
  protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
    Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(2);
    checkers.add(I18nSubchecker.class);
    checkers.add(LocalizableKeyChecker.class);
    return checkers;
  }
}
