package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A class for functionality common to multiple type-checkers that are used by the Index Checker.
 */
public abstract class BaseAnnotatedTypeFactoryForIndexChecker extends BaseAnnotatedTypeFactory {

  /** The from() element/field of a @HasSubsequence annotation. */
  protected final ExecutableElement hasSubsequenceFromElement =
      TreeUtils.getMethod(HasSubsequence.class, "from", 0, processingEnv);

  /** The to() element/field of a @HasSubsequence annotation. */
  protected final ExecutableElement hasSubsequenceToElement =
      TreeUtils.getMethod(HasSubsequence.class, "to", 0, processingEnv);

  /** The subsequence() element/field of a @HasSubsequence annotation. */
  protected final ExecutableElement hasSubsequenceSubsequenceElement =
      TreeUtils.getMethod(HasSubsequence.class, "subsequence", 0, processingEnv);

  /**
   * Creates a new BaseAnnotatedTypeFactoryForIndexChecker.
   *
   * @param checker the checker
   */
  public BaseAnnotatedTypeFactoryForIndexChecker(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Gets the from() element/field out of a HasSubsequence annotation.
   *
   * @param anno a HasSubsequence annotation
   * @return its from() element/field
   */
  public String hasSubsequenceFromValue(AnnotationMirror anno) {
    String result = AnnotationUtils.getElementValue(anno, hasSubsequenceFromElement, String.class);
    assert result != null : "@AssumeAssertion(nullness)";
    return result;
  }

  /**
   * Gets the to() element/field out of a HasSubsequence annotation.
   *
   * @param anno a HasSubsequence annotation
   * @return its to() element/field
   */
  public String hasSubsequenceToValue(AnnotationMirror anno) {
    String result = AnnotationUtils.getElementValue(anno, hasSubsequenceToElement, String.class);
    assert result != null : "@AssumeAssertion(nullness)";
    return result;
  }

  /**
   * Gets the subsequence() element/field out of a HasSubsequence annotation.
   *
   * @param anno a HasSubsequence annotation
   * @return its subsequence() element/field
   */
  public String hasSubsequenceSubsequenceValue(AnnotationMirror anno) {
    String result =
        AnnotationUtils.getElementValue(anno, hasSubsequenceSubsequenceElement, String.class);
    assert result != null : "@AssumeAssertion(nullness)";
    return result;
  }

  /**
   * Returns the annotation on {@code type} in the hierarchy whose top is {@code top}. If {@code
   * type} is a type variable or wildcard, returns the annotation on its upper bound.
   *
   * @param type an annotated type
   * @param top the top annotation of a hierarchy
   * @return the annotation on {@code type} in the hierarchy of {@code top}
   * @throws BugInCF if {@code type} has no annotation in the hierarchy of {@code top}
   */
  public AnnotationMirror getAnnotationInHierarchyNonNull(
      AnnotatedTypeMirror type, AnnotationMirror top) {
    AnnotationMirror anno = type.getAnnotationInHierarchy(top);
    if (anno == null) {
      throw new BugInCF("no annotation in hierarchy of %s: %s", top, type);
    }
    return anno;
  }
}
