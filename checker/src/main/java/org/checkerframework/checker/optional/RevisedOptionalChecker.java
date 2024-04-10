package org.checkerframework.checker.optional;

import java.util.Set;
import org.checkerframework.checker.nonempty.NonEmptyChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;

public class RevisedOptionalChecker extends BaseTypeChecker {

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(NonEmptyChecker.class);
    return checkers;
  }
}
