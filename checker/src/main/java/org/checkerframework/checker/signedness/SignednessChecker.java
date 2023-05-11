package org.checkerframework.checker.signedness;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker that prevents mixing of unsigned and signed values, and prevents meaningless
 * operations on unsigned values.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@RelevantJavaTypes({
  Byte.class,
  Short.class,
  Integer.class,
  Long.class,
  byte.class,
  short.class,
  int.class,
  long.class,
})
@StubFiles({"junit-assertions.astub"})
public class SignednessChecker extends BaseTypeChecker {

  /** Creates a new SignednessChecker. */
  public SignednessChecker() {}

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(ValueChecker.class);
    return checkers;
  }
}
