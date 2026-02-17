package org.checkerframework.common.delegation;

import java.lang.annotation.Annotation;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.delegation.qual.Delegate;

/** Annotated type factory for the Delegation Checker. */
public class DelegationAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** Create the type factory. */
  public DelegationAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(Delegate.class);
  }
}
