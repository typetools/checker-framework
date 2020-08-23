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
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.QualifierHierarchy;
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
public class QualifierHierarchyWithoutElements implements QualifierHierarchy {

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
     * Creates a QualifierHierarchyWithoutElements from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers
     * @param elements element utils
     */
    public QualifierHierarchyWithoutElements(
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
            @UnderInitialization QualifierHierarchyWithoutElements this,
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
            @UnderInitialization QualifierHierarchyWithoutElements this, Elements elements) {
        Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.allQualifierKinds()) {
            if (kind.hasElements()) {
                throw new TypeSystemError(kind + "has elements");
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
    @RequiresNonNull("this.kindToAnnotationMirror")
    protected Set<AnnotationMirror> createTops(
            @UnderInitialization QualifierHierarchyWithoutElements this) {
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
    @RequiresNonNull("this.kindToAnnotationMirror")
    protected Set<AnnotationMirror> createBottoms(
            @UnderInitialization QualifierHierarchyWithoutElements this) {
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
    @SuppressWarnings(
            "nullness:return.type.incompatible" // every QualifierKind is a key in its corresponding
    // kindToAnnotationMirror
    )
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return kindToAnnotationMirror.get(kind.getTop());
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return bottoms;
    }

    @Override
    @SuppressWarnings(
            "nullness:return.type.incompatible" // every QualifierKind is a key in its corresponding
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
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        QualifierKind subKind = getQualifierKind(subAnno);
        QualifierKind superKind = getQualifierKind(superAnno);
        return subKind.isSubtype(superKind);
    }

    @Override
    public @Nullable AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);

        QualifierKind lub = qualifierKindHierarchy.leastUpperBound(qual1, qual2);
        if (lub == null) {
            return null;
        }
        return kindToAnnotationMirror.get(lub);
    }

    @Override
    public @Nullable AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
        if (glb == null) {
            return null;
        }
        return kindToAnnotationMirror.get(glb);
    }
}
