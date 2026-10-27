package org.checkerframework.framework.type;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * A {@link org.checkerframework.framework.type.QualifierHierarchy} where, when a qualifier has
 * arguments, the subtype relation is determined by a subset or superset test on the elements
 * (arguments). Each qualifier that has elements must have exactly one element, named {@code value},
 * whose type is {@code String[]}.
 *
 * <p>This assumes that if the lub or glb of two qualifier kinds has elements, then it is one of the
 * two qualifier kinds. Otherwise, computing the lub or glb throws {@link BugInCF}.
 *
 * @see SubtypeIsSubsetQualifierHierarchy
 * @see SubtypeIsSupersetQualifierHierarchy
 */
@AnnotatedFor("nullness")
public abstract class AbstractSetInclusionQualifierHierarchy
    extends MostlyNoElementQualifierHierarchy {

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

  /**
   * Returns the input annotation whose kind is {@code resultKind}.
   *
   * @param a1 the first annotation
   * @param qualifierKind1 the kind of {@code a1}
   * @param a2 the second annotation
   * @param qualifierKind2 the kind of {@code a2}
   * @param resultKind the kind of the result, which must be {@code qualifierKind1} or {@code
   *     qualifierKind2}
   * @return whichever of {@code a1} and {@code a2} has kind {@code resultKind}
   */
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
        "Unexpected QualifierKinds %s %s %s", qualifierKind1, qualifierKind2, resultKind);
  }

  /**
   * Returns an annotation whose values are the union or intersection of the input values. If the
   * union or intersection is the set of values of {@code a1} or of {@code a2}, returns that
   * argument. Otherwise, returns a new annotation whose values are sorted, so it does not depend on
   * the order of the arguments or their values.
   *
   * @param a1 the first annotation
   * @param a2 the second annotation
   * @param resultKind the kind of the result, which is the kind of both {@code a1} and {@code a2}
   * @param union if true, take the union of the values; if false, take the intersection
   * @return an annotation of kind {@code resultKind} whose values are the union or intersection of
   *     the values of {@code a1} and {@code a2}
   */
  private AnnotationMirror combineAnnotationValues(
      AnnotationMirror a1, AnnotationMirror a2, QualifierKind resultKind, boolean union) {
    Set<String> values1 = new HashSet<>(valuesStringList(a1));
    Set<String> values2 = new HashSet<>(valuesStringList(a2));
    Set<String> values = new TreeSet<>(values1);
    if (union) {
      values.addAll(values2);
    } else {
      values.retainAll(values2);
    }
    // Returning an argument avoids creating an annotation that has the same values as the argument
    // but is not the same according to AnnotationUtils.areSame, which compares values in order.
    if (values.equals(values1)) {
      return a1;
    } else if (values.equals(values2)) {
      return a2;
    }
    AnnotationBuilder builder =
        new AnnotationBuilder(atypeFactory.getProcessingEnv(), resultKind.getAnnotationClass());
    builder.setValue("value", values.toArray());
    return builder.build();
  }

  /**
   * Returns a mutable list containing the annotation's {@code value} element. The {@code value}
   * element must be an array of strings.
   *
   * @param annotation an annotation
   * @return a mutable list containing the annotation's {@code value} element; may be the empty list
   */
  private static List<String> valuesStringList(AnnotationMirror annotation) {
    @SuppressWarnings("deprecation") // concrete annotation class is not known
    List<String> result =
        AnnotationUtils.getElementValueArray(annotation, "value", String.class, true);
    return result;
  }
}
