package org.checkerframework.framework.type;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/** Shared implementation for qualifier hierarchies ordered by inclusion of string elements. */
abstract class AbstractSetInclusionQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

  /** The processing environment; used for creating annotations. */
  private final ProcessingEnvironment processingEnv;

  /** Whether a qualifier is a subtype when its elements are a subset of another qualifier's. */
  private final boolean subtypeIsSubset;

  /**
   * Creates an AbstractSetInclusionQualifierHierarchy.
   *
   * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
   * @param processingEnv processing environment
   * @param atypeFactory the associated type factory
   * @param subtypeIsSubset whether subtyping follows the subset relation rather than the superset
   *     relation
   */
  protected AbstractSetInclusionQualifierHierarchy(
      Collection<Class<? extends Annotation>> qualifierClasses,
      ProcessingEnvironment processingEnv,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      boolean subtypeIsSubset) {
    super(qualifierClasses, processingEnv.getElementUtils(), atypeFactory);
    this.processingEnv = processingEnv;
    this.subtypeIsSubset = subtypeIsSubset;
  }

  @Override
  protected boolean isSubtypeWithElements(
      AnnotationMirror subAnno,
      QualifierKind subKind,
      AnnotationMirror superAnno,
      QualifierKind superKind) {
    if (subKind == superKind) {
      List<String> superValues = valuesStringList(superAnno);
      List<String> subValues = valuesStringList(subAnno);
      return subtypeIsSubset
          ? superValues.containsAll(subValues)
          : subValues.containsAll(superValues);
    }
    return subKind.isSubtypeOf(superKind);
  }

  @Override
  protected AnnotationMirror leastUpperBoundWithElements(
      AnnotationMirror a1,
      QualifierKind qualifierKind1,
      AnnotationMirror a2,
      QualifierKind qualifierKind2,
      QualifierKind lubKind) {
    if (qualifierKind1 == qualifierKind2) {
      return combineAnnotationValues(a1, a2, lubKind, subtypeIsSubset);
    }
    return resultFromDifferentKinds(a1, qualifierKind1, a2, qualifierKind2, lubKind);
  }

  @Override
  protected AnnotationMirror greatestLowerBoundWithElements(
      AnnotationMirror a1,
      QualifierKind qualifierKind1,
      AnnotationMirror a2,
      QualifierKind qualifierKind2,
      QualifierKind glbKind) {
    if (qualifierKind1 == qualifierKind2) {
      return combineAnnotationValues(a1, a2, glbKind, !subtypeIsSubset);
    }
    return resultFromDifferentKinds(a1, qualifierKind1, a2, qualifierKind2, glbKind);
  }

  /** Returns the input annotation whose kind is {@code resultKind}. */
  private static AnnotationMirror resultFromDifferentKinds(
      AnnotationMirror a1,
      QualifierKind qualifierKind1,
      AnnotationMirror a2,
      QualifierKind qualifierKind2,
      QualifierKind resultKind) {
    if (resultKind == qualifierKind1) {
      return a1;
    } else if (resultKind == qualifierKind2) {
      return a2;
    }
    throw new BugInCF(
        "Unexpected QualifierKinds %s %s", qualifierKind1, qualifierKind2, resultKind);
  }

  /** Returns an annotation whose values are the union or intersection of the input values. */
  @SuppressWarnings("InvalidParam") // "value" is the annotation element's name.
  private AnnotationMirror combineAnnotationValues(
      AnnotationMirror a1, AnnotationMirror a2, QualifierKind resultKind, boolean union) {
    Set<String> values = new LinkedHashSet<>(valuesStringList(a1));
    if (union) {
      values.addAll(valuesStringList(a2));
    } else {
      values.retainAll(valuesStringList(a2));
    }
    AnnotationBuilder builder =
        new AnnotationBuilder(processingEnv, resultKind.getAnnotationClass());
    builder.setValue("value", values.toArray());
    return builder.build();
  }

  /** Returns a mutable list containing the annotation's {@code value} element. */
  private static List<String> valuesStringList(AnnotationMirror annotation) {
    @SuppressWarnings("deprecation") // concrete annotation class is not known
    List<String> result =
        AnnotationUtils.getElementValueArray(annotation, "value", String.class, true);
    return result;
  }
}
