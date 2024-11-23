package org.checkerframework.checker.optional;

import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A type-checker that prevents misuse of the {@link java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
// TODO: For a call to `@Optional#ofNullable`, if the argument has type
// @NonNull, make the return type have type @Present.
@RelevantJavaTypes(Optional.class)
@StubFiles({"javaparser.astub"})
@SupportedOptions("optionalMapAssumeNonNull")
public class OptionalImplChecker extends BaseTypeChecker {
  /** Create an OptionalChecker. */
  public OptionalImplChecker() {}

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> subcheckers = super.getImmediateSubcheckerClasses();
    subcheckers.add(AliasingChecker.class);
    return subcheckers;
  }

  /**
   * Ensures that the "optional" prefix in the argument to a {@link SuppressWarnings} annotation is
   * respected. Programmers should not have to use the "optionalimpl" prefix in suppression
   * annotations.
   *
   * @return the set of suppress warnings prefixes that are respected by this checker
   */
  @Override
  public NavigableSet<String> getSuppressWarningsPrefixes() {
    NavigableSet<String> prefixes = super.getSuppressWarningsPrefixes();
    prefixes.add("optional");
    return prefixes;
  }
}
