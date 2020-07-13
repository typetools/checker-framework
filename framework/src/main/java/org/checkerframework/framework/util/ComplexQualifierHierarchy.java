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
 * A {@link QualifierHierarchy} where qualifiers may be represented by annotations with elements.
 *
 * <p>Subclasses must implement the following methods when annotations have elements:
 *
 * <ul>
 *   <li>{@link #isSubtype(AnnotationMirror, QualifierKindHierarchy.QualifierKind, AnnotationMirror,
 *       QualifierKindHierarchy.QualifierKind)}
 *   <li>{@link #leastUpperBound(AnnotationMirror, QualifierKindHierarchy.QualifierKind,
 *       AnnotationMirror, QualifierKindHierarchy.QualifierKind)}
 *   <li>{@link #greatestLowerBound(AnnotationMirror, QualifierKindHierarchy.QualifierKind,
 *       AnnotationMirror, QualifierKindHierarchy.QualifierKind)}
 * </ul>
 *
 * For cases where the annotations have no elements, the {@link
 * org.checkerframework.framework.qual.SubtypeOf} meta-annotation is used.
 *
 * <p>ComplexQualifierHierarchy uses a {@link QualifierKindHierarchy} to model the relationships
 * between qualifiers. Subclasses can override {@link #createQualifierKindHierarchy(Collection)} to
 * return a subclass of QualifierKindHierarchy.
 */
@AnnotatedFor("nullness")
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
    protected final Map<QualifierKind, AnnotationMirror> kindToElementLessQualifier;

    /**
     * Creates a QualifierHierarchy from the given classes.
     *
     * @param qualifierClasses class of annotations that are the qualifiers
     * @param elements element utils
     */
    protected ComplexQualifierHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
        this.elements = elements;
        this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

        this.topsMap = Collections.unmodifiableMap(createTopsMap());
        Set<AnnotationMirror> tops = AnnotationUtils.createAnnotationSet();
        tops.addAll(topsMap.values());
        this.tops = Collections.unmodifiableSet(tops);

        this.bottomsMap = Collections.unmodifiableMap(createBottomsMao());
        Set<AnnotationMirror> bottoms = AnnotationUtils.createAnnotationSet();
        bottoms.addAll(bottomsMap.values());
        this.bottoms = Collections.unmodifiableSet(bottoms);

        this.kindToElementLessQualifier = createElementLessQualifierMap();

        for (AnnotationMirror top : tops) {
            // This throws an error if poly is a qualifier that has an element.
            getPolymorphicAnnotation(top);
        }
    }

    /**
     * Create the {@link QualifierKindHierarchy}. (Subclasses may override to return a subclass of
     * QualifierKindHierarchy.)
     *
     * @param qualifierClasses class of annotations that are the qualifiers
     * @return the newly created qualifier kind hierarchy
     */
    protected QualifierKindHierarchy createQualifierKindHierarchy(
            @UnderInitialization ComplexQualifierHierarchy this,
            Collection<Class<? extends Annotation>> qualifierClasses) {
        return new QualifierKindHierarchy(qualifierClasses);
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror for all qualifiers whose annotations
     * do not have elements.
     *
     * @return the mapping
     */
    protected Map<QualifierKind, AnnotationMirror> createElementLessQualifierMap() {
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
     * <p>This implementation works if the top annotation has no elements, or it has elements,
     * provides a default, and that default is the top. Otherwise, subclasses must override this.
     *
     * @return a mapping from top QualifierKind to top AnnotationMirror
     */
    protected Map<QualifierKind, AnnotationMirror> createTopsMap(
            @UnderInitialization ComplexQualifierHierarchy this) {
        Map<QualifierKind, AnnotationMirror> topsMap = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getTops()) {
            topsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return topsMap;
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is bottom
     * and the AnnotationMirror is bottom in their respective hierarchies.
     *
     * <p>This implementation works if the bottom annotation has no elements, or it has elements,
     * provides a default, and that default is the bottom. Otherwise, subclasses must override this.
     *
     * @return a mapping from bottom QualifierKind to bottom AnnotationMirror
     */
    protected Map<QualifierKind, AnnotationMirror> createBottomsMao() {
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
    public @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        QualifierKind polyKind = getQualifierKind(start).getPolymorphic();
        if (polyKind == null) {
            return null;
        }
        AnnotationMirror poly = kindToElementLessQualifier.get(polyKind);
        if (poly == null) {
            throw new TypeSystemError(
                    "Poly %s has an element. Override ComplexQualifierHierarchy#getPolymorphicAnnotation.",
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
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.isInSameHierarchyAs(qual2)) {
            return null;
        }
        QualifierKind lub = qualifierKindHierarchy.leastUpperBound(qual1, qual2);
        if (lub.hasElements()) {
            return leastUpperBound(a1, qual1, a2, qual2);
        }
        return kindToElementLessQualifier.get(lub);
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
     * @return the least upper bound between {@code a1} and {@code a2}
     */
    protected abstract AnnotationMirror leastUpperBound(
            AnnotationMirror a1, QualifierKind qual1, AnnotationMirror a2, QualifierKind qual2);

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        if (!qual1.isInSameHierarchyAs(qual2)) {
            return null;
        }
        QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
        if (glb.hasElements()) {
            return greatestLowerBound(a1, qual1, a2, qual2);
        }
        return kindToElementLessQualifier.get(glb);
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
     * @return the greatest lower bound between {@code a1} and {@code a2}
     */
    protected abstract AnnotationMirror greatestLowerBound(
            AnnotationMirror a1, QualifierKind qual1, AnnotationMirror a2, QualifierKind qual2);
}
