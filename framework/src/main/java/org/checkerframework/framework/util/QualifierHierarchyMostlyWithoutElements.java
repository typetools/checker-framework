package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * A {@link QualifierHierarchy} where qualifiers may be represented by annotations with elements,
 * but most of the qualifiers do not have elements. In contrast to {@link
 * QualifierHierarchyWithElements}, this class partially implements, {@link
 * #isSubtype(AnnotationMirror, AnnotationMirror)}, {@link #leastUpperBound(AnnotationMirror,
 * AnnotationMirror)}, and {@link #greatestLowerBound(AnnotationMirror, AnnotationMirror)} and calls
 * *WithElements when the result cannot be computing from the meta-annotations {@link
 * org.checkerframework.framework.qual.SubtypeOf}.
 *
 * <p>Subclasses must implement the following methods when annotations have elements:
 *
 * <ul>
 *   <li>{@link #isSubtypeWithElements(AnnotationMirror, QualifierKind, AnnotationMirror,
 *       QualifierKind)}
 *   <li>{@link #leastUpperBoundWithElements(AnnotationMirror, QualifierKind, AnnotationMirror,
 *       QualifierKind)}
 *   <li>{@link #greatestLowerBoundWithElements(AnnotationMirror, QualifierKind, AnnotationMirror,
 *       QualifierKind)}
 * </ul>
 *
 * <p>QualifierHierarchyWithElements uses a {@link QualifierKindHierarchy} to model the
 * relationships between qualifiers. Subclasses can override {@link
 * #createQualifierKindHierarchy(Collection)} to return a subclass of QualifierKindHierarchy.
 */
@AnnotatedFor("nullness")
public abstract class QualifierHierarchyMostlyWithoutElements
        extends QualifierHierarchyWithElements {

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
    protected QualifierHierarchyMostlyWithoutElements(
            Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
        super(qualifierClasses, elements);
        this.kindToElementLessQualifier = createElementLessQualifierMap();
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror for all qualifiers whose annotations
     * do not have elements.
     *
     * @return the mapping
     */
    @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
    protected Map<QualifierKind, AnnotationMirror> createElementLessQualifierMap(
            @UnderInitialization QualifierHierarchyMostlyWithoutElements this) {
        Map<QualifierKind, AnnotationMirror> quals = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.allQualifierKinds()) {
            if (!kind.hasElements()) {
                quals.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
            }
        }
        return Collections.unmodifiableMap(quals);
    }

    @Override
    public final boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        QualifierKind subKind = getQualifierKind(subAnno);
        QualifierKind superKind = getQualifierKind(superAnno);
        if (subKind.isSubtype(superKind)) {
            if (superKind.hasElements() && subKind.hasElements()) {
                return isSubtypeWithElements(subAnno, subKind, superAnno, superKind);
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
    protected abstract boolean isSubtypeWithElements(
            AnnotationMirror subAnno,
            QualifierKind subKind,
            AnnotationMirror superAnno,
            QualifierKind superKind);

    @Override
    public final @Nullable AnnotationMirror leastUpperBound(
            AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        QualifierKind lub = qualifierKindHierarchy.leastUpperBound(qual1, qual2);
        if (lub == null) {
            // Qualifiers are not in the same hierarchy.
            return null;
        }
        if (lub.hasElements()) {
            return leastUpperBoundWithElements(a1, qual1, a2, qual2);
        }
        return kindToElementLessQualifier.get(lub);
    }

    /**
     * Returns the least upper bound of {@code a1} and {@code a2} in cases where the lub of {@code
     * qualifierKind1} and {@code qualifierKind2} is a qualifier kind that has elements. If the lub
     * of {@code qualifierKind1} and {@code qualifierKind2} does not have elements, then {@link
     * #leastUpperBound(AnnotationMirror, AnnotationMirror)} returns the correct {@code
     * AnnotationMirror} without calling this method.
     *
     * @param a1 first annotation
     * @param qualifierKind1 QualifierKind for {@code a1}
     * @param a2 second annotation
     * @param qualifierKind2 QualifierKind for {@code a2}
     * @return the least upper bound between {@code a1} and {@code a2}
     */
    protected abstract AnnotationMirror leastUpperBoundWithElements(
            AnnotationMirror a1,
            QualifierKind qualifierKind1,
            AnnotationMirror a2,
            QualifierKind qualifierKind2);

    @Override
    public final @Nullable AnnotationMirror greatestLowerBound(
            AnnotationMirror a1, AnnotationMirror a2) {
        QualifierKind qual1 = getQualifierKind(a1);
        QualifierKind qual2 = getQualifierKind(a2);
        QualifierKind glb = qualifierKindHierarchy.greatestLowerBound(qual1, qual2);
        if (glb == null) {
            // Qualifiers are not in the same hierarchy.
            return null;
        }
        if (glb.hasElements()) {
            return greatestLowerBoundWithElements(a1, qual1, a2, qual2);
        }
        return kindToElementLessQualifier.get(glb);
    }

    /**
     * Returns the greatest lower bound of {@code a1} and {@code a2} in cases where the glb of
     * {@code qualifierKind1} and {@code qualifierKind2} is a qualifier kind that has elements. If
     * the glb of {@code qualifierKind1} and {@code qualifierKind2} does not have elements, then
     * {@link #greatestLowerBound(AnnotationMirror, AnnotationMirror)} returns the correct {@code
     * AnnotationMirror} without calling this method.
     *
     * @param a1 first annotation
     * @param qualifierKind1 QualifierKind for {@code a1}
     * @param a2 second annotation
     * @param qualifierKind2 QualifierKind for {@code a2}
     * @return the greatest lower bound between {@code a1} and {@code a2}
     */
    protected abstract AnnotationMirror greatestLowerBoundWithElements(
            AnnotationMirror a1,
            QualifierKind qualifierKind1,
            AnnotationMirror a2,
            QualifierKind qualifierKind2);
}
