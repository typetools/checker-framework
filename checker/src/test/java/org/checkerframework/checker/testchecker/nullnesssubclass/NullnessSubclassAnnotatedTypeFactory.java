package org.checkerframework.checker.testchecker.nullnesssubclass;

import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

/** The type factory for {@link NullnessSubclassChecker}. */
public class NullnessSubclassAnnotatedTypeFactory extends NullnessAnnotatedTypeFactory {

  /**
   * Creates a NullnessSubclassAnnotatedTypeFactory.
   *
   * @param checker the associated checker
   */
  public NullnessSubclassAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
  }
}
