package org.checkerframework.checker.i18n;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.framework.source.CompositeChecker;
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
public class I18nChecker extends CompositeChecker {

  /** Default constructor. */
  public I18nChecker() {}

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = new LinkedHashSet<>(2);
    checkers.add(I18nSubchecker.class);
    checkers.add(LocalizableKeyChecker.class);
    return checkers;
  }
}
