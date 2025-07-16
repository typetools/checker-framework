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
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * A {@link QualifierHierarchy} where no qualifier has arguments; that is, no qualifier is
 * represented by an annotation with elements. The meta-annotation {@link
 * org.checkerframework.framework.qual.SubtypeOf} specifies the subtyping relationships.
 *
 * <p>It uses a {@link QualifierKindHierarchy} to model the relationships between qualifiers.
 * Subclasses can override {@link #createQualifierKindHierarchy(Collection)} to return a subclass of
 * QualifierKindHierarchy.
 */
@AnnotatedFor("nullness")
public class NoElementQualifierHierarchy extends QualifierHierarchy {

  /** {@link QualifierKindHierarchy}. */
  protected final QualifierKindHierarchy qualifierKindHierarchy;

  /** Set of top annotation mirrors. */
  protected final AnnotationMirrorSet tops;

  /** Set of bottom annotation mirrors. */
  protected final AnnotationMirrorSet bottoms;

  /** Mapping from {@link QualifierKind} to its corresponding {@link AnnotationMirror}. */
  protected final Map<QualifierKind, AnnotationMirror> kindToAnnotationMirror;

  /** Set of all annotations in all the hierarchies. */
  protected final Set<? extends AnnotationMirror> qualifiers;

  /**
   * Creates a NoElementQualifierHierarchy from the given classes.
   *
   * @param qualifierClasses classes of annotations that are the qualifiers
   * @param elements element utils
   * @param atypeFactory the associated type factory
   */
  @SuppressWarnings("this-escape")
  public NoElementQualifierHierarchy(
      Collection<Class<? extends Annotation>> qualifierClasses,
      Elements elements,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
    super(atypeFactory);

    this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

    this.kindToAnnotationMirror = createAnnotationMirrors(elements);
    this.qualifiers = AnnotationMirrorSet.unmodifiableSet(kindToAnnotationMirror.values());

    this.tops = createTops();
    this.bottoms = createBottoms();
  }

  /**
   * Create the {@link QualifierKindHierarchy}. (Subclasses may override to return a subclass of
   * QualifierKindHierarchy.)
   *
   * @param qualifierClasses classes of annotations that are the qualifiers
   * @return the newly created qualifier kind hierarchy
   */
  protected QualifierKindHierarchy createQualifierKindHierarchy(
      @UnderInitialization NoElementQualifierHierarchy this,
      Collection<Class<? extends Annotation>> qualifierClasses) {
    return new DefaultQualifierKindHierarchy(qualifierClasses);
  }

  /**
   * Creates and returns a mapping from qualifier kind to an annotation mirror created from the
   * qualifier kind's annotation class.
   *
   * @param elements element utils
   * @return a mapping from qualifier kind to its annotation mirror
   */
  @RequiresNonNull("this.qualifierKindHierarchy")
  protected Map<QualifierKind, AnnotationMirror> createAnnotationMirrors(
      @UnderInitialization NoElementQualifierHierarchy this, Elements elements) {
    Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
    for (QualifierKind kind : qualifierKindHierarchy.allQualifierKinds()) {
      if (kind.hasElements()) {
        throw new TypeSystemError(
            kind
                + " has elements, so the checker cannot use NoElementQualifierHierarchy."
                + " The checker should override createQualifierHierarchy().");
      }
      quals.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
    }
    return Collections.unmodifiableMap(quals);
  }

  /**
   * Creates and returns the unmodifiable set of top {@link AnnotationMirror}s.
   *
   * @return the unmodifiable set of top {@link AnnotationMirror}s
   */
  @RequiresNonNull({"this.kindToAnnotationMirror", "this.qualifierKindHierarchy"})
  protected AnnotationMirrorSet createTops(@UnderInitialization NoElementQualifierHierarchy this) {
    AnnotationMirrorSet tops = new AnnotationMirrorSet();
    for (QualifierKind top : qualifierKindHierarchy.getTops()) {
      @SuppressWarnings(
          "nullness:assignment" // All QualifierKinds are keys in kindToAnnotationMirror
      )
      @NonNull AnnotationMirror topAnno = kindToAnnotationMirror.get(top);
      tops.add(topAnno);
    }
    return AnnotationMirrorSet.unmodifiableSet(tops);
  }

  /**
   * Creates and returns the unmodifiable set of bottom {@link AnnotationMirror}s.
   *
   * @return the unmodifiable set of bottom {@link AnnotationMirror}s
   */
  @RequiresNonNull({"this.kindToAnnotationMirror", "this.qualifierKindHierarchy"})
  protected AnnotationMirrorSet createBottoms(
      @UnderInitialization NoElementQualifierHierarchy this) {
    AnnotationMirrorSet bottoms = new AnnotationMirrorSet();
    for (QualifierKind bottom : qualifierKindHierarchy.getBottoms()) {
      @SuppressWarnings(
          "nullness:assignment" // All QualifierKinds are keys in kindToAnnotationMirror
      )
      @NonNull AnnotationMirror bottomAnno = kindToAnnotationMirror.get(bottom);
      bottoms.add(bottomAnno);
    }
    return AnnotationMirrorSet.unmodifiableSet(bottoms);
  }

  /**
   * Returns the {@link QualifierKind} for the given annotation.
   *
   * @param anno an annotation that is a qualifier in this
   * @return the {@code QualifierKind} for the given annotation
   */
  protected QualifierKind getQualifierKind(AnnotationMirror anno) {
    String name = AnnotationUtils.annotationName(anno);
    QualifierKind kind = qualifierKindHierarchy.getQualifierKind(name);
    if (kind == null) {
      throw new BugInCF("Annotation not in hierarchy: %s", anno);
    }
    return kind;
  }

  @Override
  public @Nullable AnnotationMirror findAnnotationInSameHierarchy(
      Collection<? extends AnnotationMirror> annos, AnnotationMirror annotationMirror) {
    if (annos.isEmpty()) {
      return null;
    }
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

  @Override
  public AnnotationMirrorSet getTopAnnotations() {
    return tops;
  }

  @Override
  @SuppressWarnings("nullness:return" // every QualifierKind is a key in its corresponding
  // kindToAnnotationMirror
  )
  public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
    QualifierKind kind = getQualifierKind(start);
    return kindToAnnotationMirror.get(kind.getTop());
  }

  @Override
  public AnnotationMirrorSet getBottomAnnotations() {
    return bottoms;
  }

  @Override
  @SuppressWarnings("nullness:return" // every QualifierKind is a key in its corresponding
  // kindToAnnotationMirror
  )
  public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
    QualifierKind kind = getQualifierKind(start);
    return kindToAnnotationMirror.get(kind.getBottom());
  }

  @Override
  public @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
    QualifierKind poly = getQualifierKind(start).getPolymorphic();
    if (poly == null) {
      return null;
    }
    return kindToAnnotationMirror.get(poly);
  }

  @Override
  public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
    return getQualifierKind(qualifier).isPoly();
  }

  @Override
  public boolean isSubtypeQualifiers(AnnotationMirror subAnno, AnnotationMirror superAnno) {
    QualifierKind subKind = getQualifierKind(subAnno);
    QualifierKind superKind = getQualifierKind(superAnno);
    return subKind.isSubtypeOf(superKind);
  }

  @Override
  public @Nullable AnnotationMirror leastUpperBoundQualifiers(
      AnnotationMirror a1, AnnotationMirror a2) {
    QualifierKind qual1 = getQualifierKind(a1);
    QualifierKind qual2 = getQualifierKind(a2);

    QualifierKind lub = qualifierKindHierarchy.leastUpperBound(qual1, qual2);
    if (lub == null) {
      return null;
    }
    return kindToAnnotationMirror.get(lub);
  }

  @Override
  public @Nullable AnnotationMirror greatestLowerBoundQualifiers(
      AnnotationMirror a1, AnnotationMirror a2) {
    QualifierKind qual1 = getQualifierKind(a1);
    QualifierKind qual2 = getQualifierKind(a2);
    QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
    if (glb == null) {
      return null;
    }
    return kindToAnnotationMirror.get(glb);
  }
}
