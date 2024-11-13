package org.checkerframework.checker.optional;

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.checker.nonempty.NonEmptyChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/** A version of the Optional Checker that runs the NonEmptyChecker as a subchecker. */
// TODO: This is effectively the new Optional Checker and the name should reflect this fact.
public class OptionalChecker extends AggregateChecker {

  /** Creates a RevisedOptionalChecker. */
  public OptionalChecker() {}

  @Override
  protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
    Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(2);
    checkers.add(NonEmptyChecker.class);
    return checkers;
  }
}
