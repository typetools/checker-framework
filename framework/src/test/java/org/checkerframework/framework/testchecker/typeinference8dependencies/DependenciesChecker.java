package org.checkerframework.framework.testchecker.typeinference8dependencies;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * A checker whose only purpose is to run {@link DependenciesVisitor}, which tests {@link
 * org.checkerframework.framework.util.typeinference8.types.Dependencies}.
 *
 * <p>The qualifier hierarchy of this checker (see {@link DependenciesAnnotatedTypeFactory}) is
 * irrelevant; it exists only because every {@code BaseTypeChecker} needs one.
 */
public class DependenciesChecker extends BaseTypeChecker {

  /** Creates a {@code DependenciesChecker}. */
  public DependenciesChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new DependenciesVisitor(this);
  }

  @Override
  public void typeProcessingOver() {
    if (!DependenciesVisitor.testsRan) {
      throw new AssertionError(
          "DependenciesChecker ran no test; the test input contains no class "
              + DependenciesVisitor.TEST_CLASS_NAME
              + ".");
    }
    super.typeProcessingOver();
  }
}
