package org.checkerframework.checker.confidential;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.confidential.qual.NonConfidential;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Annotated type factory for the Confidential Checker. */
public class ConfidentialAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link NonConfidential} annotation mirror. */
  private final AnnotationMirror NONCONFIDENTIAL;

  /** A singleton set containing the {@code @}{@link NonConfidential} annotation mirror. */
  private final AnnotationMirrorSet setOfNonConfidential;

  /**
   * Creates a {@link ConfidentialAnnotatedTypeFactory}.
   *
   * @param checker the confidential checker
   */
  public ConfidentialAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.NONCONFIDENTIAL = AnnotationBuilder.fromClass(getElementUtils(), NonConfidential.class);
    this.setOfNonConfidential = AnnotationMirrorSet.singleton(NONCONFIDENTIAL);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfNonConfidential;
  }
}
