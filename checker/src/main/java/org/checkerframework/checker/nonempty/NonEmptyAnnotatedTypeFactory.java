package org.checkerframework.checker.nonempty;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

public class NonEmptyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a new {@link NonEmptyAnnotatedTypeFactory} that operates on a particular AST.
   *
   * @param checker the checker to use
   */
  public NonEmptyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);

    this.sideEffectsUnrefineAliases = true;

    this.postInit();
  }
}
