package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
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
public class SimpleQualifierHierarchy extends QualifierHierarchy {

    /** {@link QualifierKindHierarchy}. */
    protected final QualifierKindHierarchy qualifierKindHierarchy;

    /** Set of top annotation mirrors. */
    protected final Set<AnnotationMirror> tops;

    /** Set of bottom annotation mirrors. */
    protected final Set<AnnotationMirror> bottoms;

    /** Mapping from {@link QualifierKind} to its corresponding {@link AnnotationMirror}. */
    protected final Map<QualifierKind, AnnotationMirror> kindToAnnotationMirror;

    /** Set of all annotations in all the hierarchies. */
    protected final Set<? extends AnnotationMirror> qualifiers;

    /**
     * Creates a QualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers
     * @param elements element utils
     */
    public SimpleQualifierHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
        this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

        this.kindToAnnotationMirror = createAnnotationMirrors(elements);
        Set<AnnotationMirror> qualifiers = AnnotationUtils.createAnnotationSet();
        qualifiers.addAll(kindToAnnotationMirror.values());
        this.qualifiers = Collections.unmodifiableSet(qualifiers);

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
            @UnderInitialization SimpleQualifierHierarchy this,
            Collection<Class<? extends Annotation>> qualifierClasses) {
        return new QualifierKindHierarchy(qualifierClasses);
    }

    /**
     * Creates and returns a mapping from qualifier kind to an annotation mirror created from the
     * qualifier kind's annotation class.
     *
     * @param elements element utils
     * @return a mapping from qualifier kind to its annotation mirror
     */
    protected Map<QualifierKind, AnnotationMirror> createAnnotationMirrors(
            @UnderInitialization SimpleQualifierHierarchy this, Elements elements) {
        Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.allQualifierKinds()) {
            if (kind.hasElements()) {
                throw new TypeSystemError(
                        "SimpleQualifierHierarchy cannot be used with annotations that have elements. Found %s: ",
                        kind);
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
    protected Set<AnnotationMirror> createTops(@UnderInitialization SimpleQualifierHierarchy this) {
        Set<AnnotationMirror> tops = AnnotationUtils.createAnnotationSet();
        for (Map.Entry<QualifierKind, AnnotationMirror> entry : kindToAnnotationMirror.entrySet()) {
            if (entry.getKey().isTop()) {
                tops.add(entry.getValue());
            }
        }
        return Collections.unmodifiableSet(tops);
    }

    /**
     * Creates and returns the unmodifiable set of bottom {@link AnnotationMirror}s.
     *
     * @return the unmodifiable set of bottom {@link AnnotationMirror}s
     */
    protected Set<AnnotationMirror> createBottoms(
            @UnderInitialization SimpleQualifierHierarchy this) {
        Set<AnnotationMirror> bottoms = AnnotationUtils.createAnnotationSet();
        for (Map.Entry<QualifierKind, AnnotationMirror> entry : kindToAnnotationMirror.entrySet()) {
            if (entry.getKey().isBottom()) {
                bottoms.add(entry.getValue());
            }
        }
        return Collections.unmodifiableSet(bottoms);
    }

    /**
     * Returns the {@link QualifierKind} for the given annotation.
     *
     * @param anno an annotation that is a qualifier in this
     * @return the {@code QualifierKind}
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
    public @Nullable AnnotationMirror findAnnotationInHierarchy(
            Collection<? extends AnnotationMirror> annos, AnnotationMirror top) {
        return findAnnotationInSameHierarchy(annos, top);
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
    public Set<? extends AnnotationMirror> getTopAnnotations() {
        return tops;
    }

    @Override
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return kindToAnnotationMirror.get(kind.getTop());
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return bottoms;
    }

    @Override
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
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        QualifierKind subKind = getQualifierKind(subAnno);
        QualifierKind superKind = getQualifierKind(superAnno);
        return subKind.isSubtype(superKind);
    }

    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.isInSameHierarchyAs(qual2)) {
            return null;
        }
        QualifierKind lub = qualifierKindHierarchy.leastUpperBound(qual1, qual2);
        return kindToAnnotationMirror.get(lub);
    }

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.isInSameHierarchyAs(qual2)) {
            return null;
        }
        QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
        return kindToAnnotationMirror.get(glb);
    }
}
