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
import org.checkerframework.javacutil.TypeSystemError;

/**
 * A qualifier hierarchy where qualifiers may be represented by annotations with elements.
 *
 * <p>Subclasses must implement {@link #isSubtype(AnnotationMirror, QualifierKind, AnnotationMirror,
 * QualifierKind)}, {@link #leastUpperBound(AnnotationMirror, QualifierKind, AnnotationMirror,
 * QualifierKind)}, and {@link #greatestLowerBound(AnnotationMirror, QualifierKind,
 * AnnotationMirror, QualifierKind)} for cases when the annotations have elements. For cases where
 * the annotations have no elements, the {@link org.checkerframework.framework.qual.SubtypeOf}
 * meta-annotation is used.
 *
 * <p>ComplexQualifierHierarchy uses a {@link QualifierKindHierarchy} to model the relationships
 * between qualifiers. Subclasses can override {@link #createQualifierKindHierarchy(Collection)} to
 * return a subclass of QualifierKindHierarchy.
 */
public abstract class ComplexQualifierHierarchy extends QualifierHierarchy {

    /** {@link org.checkerframework.javacutil.ElementUtils} */
    private Elements elements;

    /** {@link QualifierKindHierarchy}. */
    protected final QualifierKindHierarchy qualifierKindHierarchy;

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
    protected final Map<QualifierKind, AnnotationMirror> qualifierMap;

    /**
     * Creates a type hierarchy from the given classes.
     *
     * @param qualifierClasses class of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     */
    protected ComplexQualifierHierarchy(
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

        for (AnnotationMirror top : tops) {
            // This throws an error if poly is a qualifier that has an element.
            getPolymorphicAnnotation(top);
        }
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
     * Creates a mapping from QualifierKind to AnnotationMirror for all qualifiers whose annotations
     * do not have elements.
     *
     * @return the mapping
     */
    protected Map<QualifierKind, AnnotationMirror> createQualifiers() {
        Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.allQualifierKinds()) {
            if (!kind.hasElements()) {
                quals.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
            }
        }
        return Collections.unmodifiableMap(quals);
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is top.
     *
     * <p>Subclasses must override this if the top annotation has elements and provides no default.
     *
     * @return a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is top
     */
    protected Map<QualifierKind, AnnotationMirror> createTops() {
        Map<QualifierKind, AnnotationMirror> topsMap = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getTops()) {
            topsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return topsMap;
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is bottom.
     *
     * <p>Subclasses must override this if the bottom annotation has elements and provides not
     * default.
     *
     * @return a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is bottom
     */
    protected Map<QualifierKind, AnnotationMirror> createBottoms() {
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
     * Returns the qualifier kind for the annotation with the fully qualified name {@code name}.
     *
     * @param name fully qualified annotation name
     * @return the qualifier kind for the annotation with {@code name}
     */
    protected QualifierKind getQualifierKind(String name) {
        QualifierKind kind = qualifierKindHierarchy.getQualifierKind(name);
        if (kind == null) {
            throw new BugInCF("QualifierKind not in hierarchy: %s", name);
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
        QualifierKind poly = qualifierKindHierarchy.getTopToPoly().get(kind);
        if (poly == null) {
            return null;
        }
        if (qualifierMap.containsKey(poly)) {
            return qualifierMap.get(poly);
        } else {
            throw new TypeSystemError(
                    "Poly has an element. Override ComplexQualifierHierarchy#getPolymorphicAnnotation. Poly: %s",
                    poly);
        }
    }

    @Override
    public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
        return getQualifierKind(qualifier).isPoly();
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return bottomsMap.get(kind.getBottom());
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

    /**
     * Returns whether or not {@code subAnno} is a subtype of {@code superAnno}. Both {@code
     * subAnno} and {@code superAnno} are annotations with elements.
     *
     * @param subAnno possible subtype annotation; has elements
     * @param subKind QualifierKind of{@code subAnno}
     * @param superAnno possible super annotation; has elements
     * @param superKind QualifierKind of{@code superAnno}
     * @return whether or not {@code subAnno} is a subtype of {@code superAnno}
     */
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

    /**
     * Returns the least upper bound between {@code a1} and {@code a2}.
     *
     * <p>This method is only called when the lub is an annotation with elements.
     *
     * @param a1 first annotation
     * @param qual1 QualifierKind for {@code a1}
     * @param a2 second annotation
     * @param qual2 QualifierKind for {@code a2}
     * @return the least upper bound between {@code a1} and {@code a2}.
     */
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

    /**
     * Returns the greatest lower bound between {@code a1} and {@code a2}.
     *
     * <p>This method is only called when the glb is an annotation with elements.
     *
     * @param a1 first annotation
     * @param qual1 QualifierKind for {@code a1}
     * @param a2 second annotation
     * @param qual2 QualifierKind for {@code a2}
     * @return the greatest lower bound between {@code a1} and {@code a2}.
     */
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
