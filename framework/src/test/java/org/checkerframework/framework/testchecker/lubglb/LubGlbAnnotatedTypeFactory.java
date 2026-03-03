package org.checkerframework.framework.testchecker.lubglb;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbA;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbC;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbE;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbF;
import org.checkerframework.framework.testchecker.lubglb.quals.PolyLubglb;

public class LubGlbAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  @SuppressWarnings("this-escape")
  public LubGlbAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<Class<? extends Annotation>>(
        Arrays.asList(
            LubglbA.class,
            LubglbB.class,
            LubglbC.class,
            LubglbD.class,
            LubglbE.class,
            LubglbF.class,
            PolyLubglb.class));
  }
}
