package org.checkerframework.checker.signedness;

import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker that prevents mixing of unsigned and signed values, and prevents meaningless
 * operations on unsigned values.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@StubFiles({"junit-assertions.astub"})
public class SignednessChecker extends BaseTypeChecker {

  /** Creates a new SignednessChecker. */
  public SignednessChecker() {}

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
    checkers.add(ValueChecker.class);
    return checkers;
  }
}
