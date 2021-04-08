package org.checkerframework.framework.testchecker.variablenamedefault;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.variablenamedefault.quals.PolyVariableNameDefault;
import org.checkerframework.framework.testchecker.variablenamedefault.quals.VariableNameDefaultBottom;
import org.checkerframework.framework.testchecker.variablenamedefault.quals.VariableNameDefaultMiddle;
import org.checkerframework.framework.testchecker.variablenamedefault.quals.VariableNameDefaultTop;

public class VariableNameDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
  public VariableNameDefaultAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<>(
        Arrays.asList(
            VariableNameDefaultTop.class,
            VariableNameDefaultMiddle.class,
            VariableNameDefaultBottom.class,
            PolyVariableNameDefault.class));
  }
}
