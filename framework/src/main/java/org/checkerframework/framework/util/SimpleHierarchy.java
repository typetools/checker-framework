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

public class SimpleHierarchy extends QualifierHierarchy {
    private Elements elements;
    protected final QualifierKindHierarchy qualifierKindHierarchy;
    protected final Map<QualifierKind, AnnotationMirror> topsMap;
    protected final Set<AnnotationMirror> tops;
    protected final Map<QualifierKind, AnnotationMirror> bottomsMap;
    protected final Set<AnnotationMirror> bottoms;

    public SimpleHierarchy(
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

    private QualifierKind getQualifierKind(AnnotationMirror anno) {
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
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return bottomsMap.get(kind.getBottom());
    }

    @Override
    public Set<? extends AnnotationMirror> getTypeQualifiers() {
        return null;
    }

    @Override
    public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
        return false;
    }

    @Override
    public boolean isSubtype(
            Collection<? extends AnnotationMirror> rhs,
            Collection<? extends AnnotationMirror> lhs) {
        return false;
    }

    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }

    @Override
    public boolean isSubtypeTypeVariable(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        return false;
    }

    @Override
    public boolean isSubtypeTypeVariable(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos) {
        return false;
    }

    @Override
    public AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }

    @Override
    public AnnotationMirror greatestLowerBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }
}
