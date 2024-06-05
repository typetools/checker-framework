package org.checkerframework.checker.optional;

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.checker.nonempty.NonEmptyChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

public class RevisedOptionalChecker extends AggregateChecker {

  @Override
  protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
    Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(2);
    checkers.add(NonEmptyChecker.class);
    return checkers;
  }
}
