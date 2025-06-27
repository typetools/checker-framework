package org.checkerframework.checker.testchecker.disbaruse;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUseBottom;
import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUseTop;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

/** The type factory for forbidding use of the DisbarUse type. */
public class DisbarUseTypeFactory extends BaseAnnotatedTypeFactory {
  /**
   * Creates a new DisbarUseTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public DisbarUseTypeFactory(BaseTypeChecker checker) {
    super(checker);
    postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(Arrays.asList(DisbarUseTop.class, DisbarUseBottom.class));
  }
}
