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

/** Qualifiers may be represented by annotations with out elements. */
public abstract class ComplexHierarchy extends QualifierHierarchy {
    private Elements elements;
    protected final QualifierKindHierarchy qualifierKindHierarchy;
    protected final Map<QualifierKind, AnnotationMirror> topsMap;
    protected final Set<AnnotationMirror> tops;
    protected final Map<QualifierKind, AnnotationMirror> bottomsMap;
    protected final Set<AnnotationMirror> bottoms;
    protected final Map<QualifierKind, AnnotationMirror> qualifierMap;

    public ComplexHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
        this.elements = elements;
        this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

        this.topsMap = Collections.unmodifiableMap(createTops());
        Set<AnnotationMirror> tops = AnnotationUtils.createAnnotationSet();
        tops.addAll(topsMap.values());
        this.tops = Collections.unmodifiableSet(tops);

        this.bottomsMap = Collections.unmodifiableMap(createBottoms());
        Set<AnnotationMirror> bottoms = AnnotationUtils.createAnnotationSet();
        bottoms.addAll(bottomsMap.values());
        this.bottoms = Collections.unmodifiableSet(bottoms);
        this.qualifierMap = createQualifiers();
    }

    @Override
    public boolean isValid() {
        for (AnnotationMirror top : tops) {
            // This throws an error if poly is a qualifier that has an element.
            getPolymorphicAnnotation(top);
        }
        return true;
    }

    protected Map<QualifierKind, AnnotationMirror> createQualifiers() {
        Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getQualifierKindMap().values()) {
            if (!kind.hasElements()) {
                quals.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
            }
        }
        return Collections.unmodifiableMap(quals);
    }

    protected Map<QualifierKind, AnnotationMirror> createTops() {
        Map<QualifierKind, AnnotationMirror> topsMap = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getTops()) {
            topsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return topsMap;
    }

    protected Map<QualifierKind, AnnotationMirror> createBottoms() {
        Map<QualifierKind, AnnotationMirror> bottomsMap = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getBottoms()) {
            bottomsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return bottomsMap;
    }

    protected QualifierKindHierarchy createQualifierKindHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses) {
        return new QualifierKindHierarchy(qualifierClasses);
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
    public Set<? extends AnnotationMirror> getTopAnnotations() {
        return tops;
    }

    @Override
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return topsMap.get(kind.getTop());
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return bottoms;
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        QualifierKind poly = qualifierKindHierarchy.getPolyMap().get(kind);
        if (poly == null) {
            return null;
        }
        if (qualifierMap.containsKey(poly)) {
            return qualifierMap.get(poly);
        } else {
            throw new BugInCF(
                    "Poly has an element. Override ComplexHierarchy#getPolymorphicAnnotation. Poly: %s",
                    poly);
        }
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return bottomsMap.get(kind.getBottom());
    }

    @Override
    @Deprecated
    public Set<? extends AnnotationMirror> getTypeQualifiers() {
        return null;
    }

    @Override
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        QualifierKind subKind = getQualifierKind(subAnno);
        QualifierKind superKind = getQualifierKind(superAnno);
        if (subKind.isSubtype(superKind)) {
            if (superKind.hasElements() && subKind.hasElements()) {
                return isSubtype(subAnno, subKind, superAnno, superKind);
            } else {
                return true;
            }
        }
        return false;
    }

    protected abstract boolean isSubtype(
            AnnotationMirror subAnno,
            QualifierKind subKind,
            AnnotationMirror superAnno,
            QualifierKind superKind);

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
                    if (isSubtype(subAnno, superAnno)) {
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
        if (lub.hasElements()) {
            return leastUpperBound(a1, qual1, a2, qual2);
        }
        return qualifierMap.get(lub);
    }

    protected abstract AnnotationMirror leastUpperBound(
            AnnotationMirror a1, QualifierKind qual1, AnnotationMirror a2, QualifierKind qual2);

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.areInSameHierarchy(qual2)) {
            return null;
        }
        QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
        if (glb.hasElements()) {
            return greatestLowerBound(a1, qual1, a2, qual2);
        }
        return qualifierMap.get(glb);
    }

    protected abstract AnnotationMirror greatestLowerBound(
            AnnotationMirror a1, QualifierKind qual1, AnnotationMirror a2, QualifierKind qual2);

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
                    if (isSubtype(subAnno, superAnno)) {
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
        if (a1 == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return a2;
        }
        if (a2 == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return a1;
        }
        return greatestLowerBound(a1, a2);
    }
}
