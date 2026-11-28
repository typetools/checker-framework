package org.checkerframework.framework.testchecker.unboxingweakens;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.unboxingweakens.qual.UWTop;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** The type factory of the UnboxingWeakens Checker. */
public class UnboxingWeakensAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @UWTop} annotation. */
  private final AnnotationMirror UWTOP = AnnotationBuilder.fromClass(elements, UWTop.class);

  /**
   * Creates a new UnboxingWeakensAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public UnboxingWeakensAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  /**
   * Returns {@code @UWTop}: an unboxing conversion weakens every qualifier to the top qualifier.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public AnnotationMirrorSet getUnboxedAnnotations(
      AnnotationMirrorSet annos, TypeKind primitiveKind) {
    return AnnotationMirrorSet.singleton(UWTOP);
  }
}
