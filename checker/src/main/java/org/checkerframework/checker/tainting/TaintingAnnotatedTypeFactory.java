package org.checkerframework.checker.tainting;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class TaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
  private final AnnotationMirror UNTAINTED;
  private final AnnotationMirrorSet setOfUntainted;

  public TaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.UNTAINTED = AnnotationBuilder.fromClass(getElementUtils(), Untainted.class);
    this.setOfUntainted = AnnotationMirrorSet.singleton(UNTAINTED);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfUntainted;
  }
}
