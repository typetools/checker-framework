package org.checkerframework.framework.type;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * A {@link QualifierHierarchy} where qualifiers may be represented by annotations with elements.
 *
 * <p>ElementQualifierHierarchy uses a {@link QualifierKindHierarchy} to model the relationships
 * between qualifiers. (By contrast, {@link MostlyNoElementQualifierHierarchy} uses the {@link
 * QualifierKindHierarchy} to implement {@code isSubtype}, {@code leastUpperBound}, and {@code
 * greatestLowerBound} methods for qualifiers without elements.)
 *
 * <p>Subclasses can override {@link #createQualifierKindHierarchy(Collection)} to return a subclass
 * of QualifierKindHierarchy.
 */
@AnnotatedFor("nullness")
public abstract class ElementQualifierHierarchy implements QualifierHierarchy {

  /** {@link org.checkerframework.javacutil.ElementUtils}. */
  private Elements elements;

  /** {@link QualifierKindHierarchy}. */
  protected final QualifierKindHierarchy qualifierKindHierarchy;

  // The following fields duplicate information in qualifierKindHierarchy, but using
  // AnnotationMirrors instead of QualifierKinds.

  /** A mapping from top QualifierKinds to their corresponding AnnotationMirror. */
  protected final Map<QualifierKind, AnnotationMirror> topsMap;

  /** The set of top annotation mirrors. */
  protected final Set<AnnotationMirror> tops;

  /** A mapping from bottom QualifierKinds to their corresponding AnnotationMirror. */
  protected final Map<QualifierKind, AnnotationMirror> bottomsMap;

  /** The set of bottom annotation mirrors. */
  protected final Set<AnnotationMirror> bottoms;

  /**
   * A mapping from QualifierKind to AnnotationMirror for all qualifiers whose annotations do not
   * have elements.
   */
  protected final Map<QualifierKind, AnnotationMirror> kindToElementlessQualifier;

  /**
   * Creates a ElementQualifierHierarchy from the given classes.
   *
   * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
   * @param elements element utils
   */
  protected ElementQualifierHierarchy(
      Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
    this.elements = elements;
    this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

    this.topsMap = Collections.unmodifiableMap(createTopsMap());
    this.tops = AnnotationUtils.createUnmodifiableAnnotationSet(topsMap.values());

    this.bottomsMap = Collections.unmodifiableMap(createBottomsMap());
    this.bottoms = AnnotationUtils.createUnmodifiableAnnotationSet(bottomsMap.values());

    this.kindToElementlessQualifier = createElementlessQualifierMap();
  }

  @Override
  public boolean isValid() {
    for (AnnotationMirror top : tops) {
      // This throws an error if poly is a qualifier that has an element.
      getPolymorphicAnnotation(top);
    }
    return true;
  }

  /**
   * Create the {@link QualifierKindHierarchy}. (Subclasses may override to return a subclass of
   * QualifierKindHierarchy.)
   *
   * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
   * @return the newly created qualifier kind hierarchy
   */
  protected QualifierKindHierarchy createQualifierKindHierarchy(
      @UnderInitialization ElementQualifierHierarchy this,
      Collection<Class<? extends Annotation>> qualifierClasses) {
    return new DefaultQualifierKindHierarchy(qualifierClasses);
  }

  /**
   * Creates a mapping from QualifierKind to AnnotationMirror for all qualifiers whose annotations
   * do not have elements.
   *
   * @return the mapping
   */
  @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
  protected Map<QualifierKind, AnnotationMirror> createElementlessQualifierMap(
      @UnderInitialization ElementQualifierHierarchy this) {
    Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
    for (QualifierKind kind : qualifierKindHierarchy.allQualifierKinds()) {
      if (!kind.hasElements()) {
        quals.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
      }
    }
    return Collections.unmodifiableMap(quals);
  }

  /**
   * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is top and
   * the AnnotationMirror is top in their respective hierarchies.
   *
   * <p>This implementation works if the top annotation has no elements, or if it has elements,
   * provides a default, and that default is the top. Otherwise, subclasses must override this.
   *
   * @return a mapping from top QualifierKind to top AnnotationMirror
   */
  @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
  protected Map<QualifierKind, AnnotationMirror> createTopsMap(
      @UnderInitialization ElementQualifierHierarchy this) {
    Map<QualifierKind, AnnotationMirror> topsMap = new TreeMap<>();
    for (QualifierKind kind : qualifierKindHierarchy.getTops()) {
      topsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
    }
    return topsMap;
  }

  /**
   * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is bottom and
   * the AnnotationMirror is bottom in their respective hierarchies.
   *
   * <p>This implementation works if the bottom annotation has no elements, or if it has elements,
   * provides a default, and that default is the bottom. Otherwise, subclasses must override this.
   *
   * @return a mapping from bottom QualifierKind to bottom AnnotationMirror
   */
  @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
  protected Map<QualifierKind, AnnotationMirror> createBottomsMap(
      @UnderInitialization ElementQualifierHierarchy this) {
    Map<QualifierKind, AnnotationMirror> bottomsMap = new TreeMap<>();
    for (QualifierKind kind : qualifierKindHierarchy.getBottoms()) {
      bottomsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
    }
    return bottomsMap;
  }

  /**
   * Returns the qualifier kind for the given annotation.
   *
   * @param anno annotation mirror
   * @return the qualifier kind for the given annotation
   */
  protected QualifierKind getQualifierKind(AnnotationMirror anno) {
    String name = AnnotationUtils.annotationName(anno);
    return getQualifierKind(name);
  }

  /**
   * Returns the qualifier kind for the annotation with the canonical name {@code name}.
   *
   * @param name fully qualified annotation name
   * @return the qualifier kind for the annotation named {@code name}
   */
  protected QualifierKind getQualifierKind(@CanonicalName String name) {
    QualifierKind kind = qualifierKindHierarchy.getQualifierKind(name);
    if (kind == null) {
      throw new BugInCF("QualifierKind %s not in hierarchy", name);
    }
    return kind;
  }

  @Override
  public Set<? extends AnnotationMirror> getTopAnnotations() {
    return tops;
  }

  @Override
  public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
    QualifierKind kind = getQualifierKind(start);
    @SuppressWarnings("nullness:assignment.type.incompatible") // All tops are a key for topsMap.
    @NonNull AnnotationMirror result = topsMap.get(kind.getTop());
    return result;
  }

  @Override
  public Set<? extends AnnotationMirror> getBottomAnnotations() {
    return bottoms;
  }

  @Override
  public @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
    QualifierKind polyKind = getQualifierKind(start).getPolymorphic();
    if (polyKind == null) {
      return null;
    }
    AnnotationMirror poly = kindToElementlessQualifier.get(polyKind);
    if (poly == null) {
      throw new TypeSystemError(
          "Poly %s has an element. Override ElementQualifierHierarchy#getPolymorphicAnnotation.",
          polyKind);
    }
    return poly;
  }

  @Override
  public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
    return getQualifierKind(qualifier).isPoly();
  }

  @Override
  public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
    QualifierKind kind = getQualifierKind(start);
    @SuppressWarnings(
        "nullness:assignment.type.incompatible") // All bottoms are keys for bottomsMap.
    @NonNull AnnotationMirror result = bottomsMap.get(kind.getBottom());
    return result;
  }

  @Override
  public @Nullable AnnotationMirror findAnnotationInSameHierarchy(
      Collection<? extends AnnotationMirror> annos, AnnotationMirror annotationMirror) {
    QualifierKind kind = getQualifierKind(annotationMirror);
    for (AnnotationMirror candidate : annos) {
      QualifierKind candidateKind = getQualifierKind(candidate);
      if (candidateKind.isInSameHierarchyAs(kind)) {
        return candidate;
      }
    }
    return null;
  }

  @Override
  public @Nullable AnnotationMirror findAnnotationInHierarchy(
      Collection<? extends AnnotationMirror> annos, AnnotationMirror top) {
    return findAnnotationInSameHierarchy(annos, top);
  }
}
