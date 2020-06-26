package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.UserError;

/**
 * This is a qualifier hierarchy where no qualifiers are represented by annotations with elements.
 *
 * <p>It uses a {@link QualifierKindHierarchy} to model the relationships between qualifiers.
 * Subclasses can override {@link #createQualifierKindHierarchy(Collection)} to return a subclass of
 * QualifierKindHierarchy.
 */
public class SimpleHierarchy extends QualifierHierarchy {
    protected final QualifierKindHierarchy qualifierKindHierarchy;
    protected final Set<AnnotationMirror> tops;
    protected final Set<AnnotationMirror> bottoms;
    protected final Map<QualifierKind, AnnotationMirror> qualifierMap;
    protected final Set<? extends AnnotationMirror> qualifiers;

    /**
     * Creates a type hierarchy from the given classes.
     *
     * @param qualifierClasses class of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     */
    public SimpleHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
        this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

        this.qualifierMap = createAnnotationMirrors(elements);
        Set<AnnotationMirror> qualifiers = AnnotationUtils.createAnnotationSet();
        qualifiers.addAll(qualifierMap.values());
        this.qualifiers = Collections.unmodifiableSet(qualifiers);

        this.tops = createTops();
        this.bottoms = createBottoms();
    }

    /**
     * Create the {@link QualifierKindHierarchy}. (Subclasses may override to return a subclass of
     * QualifierKindHierarchy.)
     *
     * @param qualifierClasses class of annotations that are the qualifiers for this hierarchy
     * @return the newly created qualifier kind hierarchy
     */
    protected QualifierKindHierarchy createQualifierKindHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses) {
        return new QualifierKindHierarchy(qualifierClasses);
    }

    /**
     * Creates and returns a mapping from qualifier kind to an annotation mirror created from the
     * qualifier kind's annotation class.
     *
     * @param elements element utils
     * @return a mapping from qualifier kind to an annotation mirror created from the qualifier
     *     kind's annotation class
     */
    protected Map<QualifierKind, AnnotationMirror> createAnnotationMirrors(Elements elements) {
        Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getQualifierKindMap().values()) {
            if (kind.hasElements()) {
                throw new UserError(
                        "SimpleHierarchy cannot be used with annotations that have elements. Found %s: ",
                        kind);
            }
            quals.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return Collections.unmodifiableMap(quals);
    }

    protected Set<AnnotationMirror> createTops() {
        Set<AnnotationMirror> tops = AnnotationUtils.createAnnotationSet();
        for (Map.Entry<QualifierKind, AnnotationMirror> entry : qualifierMap.entrySet()) {
            if (entry.getKey().isTop()) {
                tops.add(entry.getValue());
            }
        }
        return Collections.unmodifiableSet(tops);
    }

    protected Set<AnnotationMirror> createBottoms() {
        Set<AnnotationMirror> bottoms = AnnotationUtils.createAnnotationSet();
        for (Map.Entry<QualifierKind, AnnotationMirror> entry : qualifierMap.entrySet()) {
            if (entry.getKey().isBottom()) {
                bottoms.add(entry.getValue());
            }
        }
        return Collections.unmodifiableSet(bottoms);
    }

    protected QualifierKind getQualifierKind(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        QualifierKind kind = qualifierKindHierarchy.getQualifierKindMap().get(name);
        if (name == null) {
            throw new BugInCF("Annotation not in hierarchy: %s", anno);
        }
        return kind;
    }

    @Override
    public AnnotationMirror findAnnotationInHierarchy(
            Collection<? extends AnnotationMirror> annos, AnnotationMirror top) {
        return findAnnotationInSameHierarchy(annos, top);
    }

    @Override
    public AnnotationMirror findAnnotationInSameHierarchy(
            Collection<? extends AnnotationMirror> annos, AnnotationMirror annotationMirror) {
        QualifierKind kind = getQualifierKind(annotationMirror);
        for (AnnotationMirror anno : annos) {
            QualifierKind annoKind = getQualifierKind(anno);
            if (annoKind.areInSameHierarchy(kind)) {
                return anno;
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
        return qualifierMap.get(kind.getTop());
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return bottoms;
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return qualifierMap.get(kind.getBottom());
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        QualifierKind poly = qualifierKindHierarchy.getPolyMap().get(kind);
        if (poly == null) {
            return null;
        }
        return qualifierMap.get(poly);
    }

    @Override
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        QualifierKind subKind = getQualifierKind(subAnno);
        QualifierKind superKind = getQualifierKind(superAnno);
        return subKind.isSubtype(superKind);
    }

    @Override
    public boolean isSubtype(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos) {
        int isSubtypeCount = 0;
        for (AnnotationMirror subAnno : subAnnos) {
            QualifierKind subKind = getQualifierKind(subAnno);
            for (AnnotationMirror superAnno : superAnnos) {
                QualifierKind superKind = getQualifierKind(superAnno);
                if (subKind.areInSameHierarchy(superKind)) {
                    if (subKind.isSubtype(superKind)) {
                        isSubtypeCount++;
                    } else {
                        return false;
                    }
                }
            }
        }
        return isSubtypeCount == superAnnos.size();
    }

    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.areInSameHierarchy(qual2)) {
            return null;
        }
        QualifierKind lub = qualifierKindHierarchy.leastUpperBound(qual1, qual2);
        return qualifierMap.get(lub);
    }

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.areInSameHierarchy(qual2)) {
            return null;
        }
        QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
        return qualifierMap.get(glb);
    }

    @Override
    public boolean isSubtypeTypeVariable(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        if (superAnno == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return true;
        }
        if (subAnno == null) {
            // [] is a subtype of no qualifier (only [])
            return false;
        }
        return isSubtype(subAnno, superAnno);
    }

    @Override
    public boolean isSubtypeTypeVariable(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos) {
        int isSubtypeCount = 0;
        for (AnnotationMirror subAnno : subAnnos) {
            QualifierKind subKind = getQualifierKind(subAnno);
            for (AnnotationMirror superAnno : superAnnos) {
                QualifierKind superKind = getQualifierKind(superAnno);
                if (subKind.areInSameHierarchy(superKind)) {
                    if (subKind.isSubtype(superKind)) {
                        isSubtypeCount++;
                    } else {
                        return false;
                    }
                }
            }
        }
        return isSubtypeCount == subAnnos.size();
    }

    @Override
    public AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 == null || a2 == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return null;
        }
        return leastUpperBound(a1, a2);
    }

    @Override
    public AnnotationMirror greatestLowerBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2) {
        throw new RuntimeException("Not implemented");
    }
}
