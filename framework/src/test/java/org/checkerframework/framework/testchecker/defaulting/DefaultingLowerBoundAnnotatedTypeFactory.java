package org.checkerframework.framework.testchecker.defaulting;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.defaulting.LowerBoundQual.LbBottom;
import org.checkerframework.framework.testchecker.defaulting.LowerBoundQual.LbExplicit;
import org.checkerframework.framework.testchecker.defaulting.LowerBoundQual.LbImplicit;
import org.checkerframework.framework.testchecker.defaulting.LowerBoundQual.LbTop;

public class DefaultingLowerBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  public DefaultingLowerBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<Class<? extends Annotation>>(
        Arrays.asList(LbTop.class, LbExplicit.class, LbImplicit.class, LbBottom.class));
  }
}
