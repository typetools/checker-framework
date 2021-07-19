package org.checkerframework.checker.testchecker.wholeprograminference;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;

/**
 * Checker for a simple type system to test whole-program inference. Uses the Value Checker as a
 * subchecker to ensure that generated files contain annotations both from this checker and from the
 * Value Checker, to make certain that subchecker outputs aren't overwritten.
 */
public class WholeProgramInferenceTestChecker extends BaseTypeChecker {

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new WholeProgramInferenceTestVisitor(this);
  }

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(ValueChecker.class);
    return checkers;
  }
}
