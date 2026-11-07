package org.checkerframework.framework.type;

import java.lang.annotation.Annotation;
import java.util.Collection;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * A {@link org.checkerframework.framework.type.QualifierHierarchy} where, when a qualifier has
 * arguments, the subtype relation is determined by a subset test on the elements (arguments). The
 * elements must be strings.
 */
@AnnotatedFor("nullness")
public class SubtypeIsSubsetQualifierHierarchy extends AbstractSetInclusionQualifierHierarchy {

  /**
   * Creates a SubtypeIsSubsetQualifierHierarchy from the given classes.
   *
   * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
   * @param processingEnv processing environment
   * @param atypeFactory the associated type factory
   */
  @SuppressWarnings({
    "nullness:argument", // order-dependent false positive: same type as the parameter
    "keyfor:argument", // order-dependent false positive: same type as the parameter
    "nullness:unneeded.suppression" // the false positives are not issued in every build
  })
  public SubtypeIsSubsetQualifierHierarchy(
      Collection<Class<? extends Annotation>> qualifierClasses,
      ProcessingEnvironment processingEnv,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
    super(qualifierClasses, processingEnv, atypeFactory, true);
  }
}
