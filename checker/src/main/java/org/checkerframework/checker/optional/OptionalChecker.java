package org.checkerframework.checker.optional;

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.checker.nonempty.NonEmptyChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of the {@link
 * java.util.Optional} class.
 */
public class OptionalChecker extends AggregateChecker {

  /** Creates a new {@link org.checkerframework.checker.optional.OptionalChecker} */
  public OptionalChecker() {}

  @Override
  protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
    Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(2);
    checkers.add(NonEmptyChecker.class);
    return checkers;
  }
}
