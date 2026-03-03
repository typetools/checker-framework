package org.checkerframework.checker.optional;

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.checker.nonempty.NonEmptyChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

// The Optional Checker aggregates two checkers, the Non-Empty Checker and the
// OptionalImplChecker. The Optional Checker behaves as the ultimate parent of those two checkers
// and has no qualifiers or transfer function class of its own.
//
// When the Non-Empty Checker is executed as a subchecker of the Optional Checker, it runs the
// OptionalImplChecker. It only type-checks the methods that the OptionalImplChecker passes to it.
//
// The OptionalImplChecker implements a type system for Optional values (i.e., what users would
// expect the Optional Checker to implement). It reads programmer-written annotations from the
// Non-Empty type system, and keeps track of the methods that the Non-Empty Checker should check.
/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of the {@link
 * java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
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
