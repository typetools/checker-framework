package org.checkerframework.framework.testchecker.qualifierequality;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.qualifierequality.quals.PolyQualEq;
import org.checkerframework.framework.testchecker.qualifierequality.quals.QualEqBottom;
import org.checkerframework.framework.testchecker.qualifierequality.quals.QualEqTop;

/** The annotated type factory for the {@link QualifierEqualityChecker}. */
public class QualifierEqualityAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a QualifierEqualityAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public QualifierEqualityAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<Class<? extends Annotation>>(
        Arrays.asList(QualEqTop.class, QualEqBottom.class, PolyQualEq.class));
  }
}
