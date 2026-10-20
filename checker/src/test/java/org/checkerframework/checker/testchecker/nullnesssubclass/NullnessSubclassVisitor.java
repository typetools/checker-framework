package org.checkerframework.checker.testchecker.nullnesssubclass;

import org.checkerframework.checker.nullness.NullnessVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

/** The visitor for {@link NullnessSubclassChecker}. */
public class NullnessSubclassVisitor extends NullnessVisitor {

  /**
   * Creates a NullnessSubclassVisitor.
   *
   * @param checker the associated checker
   */
  public NullnessSubclassVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public NullnessSubclassAnnotatedTypeFactory createTypeFactory() {
    return new NullnessSubclassAnnotatedTypeFactory(checker);
  }
}
