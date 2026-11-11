package org.checkerframework.framework.testchecker.promotion;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.promotion.qual.Promoted;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** The type factory for the Promotion Checker. */
public class PromotionAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @Promoted} annotation. */
  private final AnnotationMirror PROMOTED = AnnotationBuilder.fromClass(elements, Promoted.class);

  /**
   * Creates a PromotionAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public PromotionAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type) {
    AnnotatedPrimitiveType result = super.getUnboxedType(type);
    result.replaceAnnotation(PROMOTED);
    return result;
  }

  @Override
  public AnnotationMirrorSet getWidenedAnnotations(
      AnnotationMirrorSet annos, TypeKind typeKind, TypeKind widenedTypeKind) {
    return AnnotationMirrorSet.singleton(PROMOTED);
  }
}
