package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * This interface holds information about the subtyping relationships between kinds of qualifiers. A
 * "kind" of qualifier is its annotation class and is represented by the {@link QualifierKind}
 * class. If a type system has more than one hierarchy, information about all hierarchies is stored
 * in this class.
 *
 * <p>The qualifier kind subtyping relationship may be an over-approximation of the qualifier
 * subtyping relationship, for qualifiers that have elements/arguments. In other words, if a
 * qualifier kind is a subtype of another qualifier kind, then qualifiers of those kinds may or may
 * not be subtypes, depending on the values of any elements of the qualifiers. If qualifier kinds
 * are not subtypes, then qualifiers of those kinds are never subtypes.
 *
 * <p>This interface is used by {@link NoElementQualifierHierarchy} and {@link
 * ElementQualifierHierarchy} (but <em>not</em> {@link MostlyNoElementQualifierHierarchy}) to
 * implement methods that compare {@link javax.lang.model.element.AnnotationMirror}s, such as {@link
 * org.checkerframework.framework.type.QualifierHierarchy#isSubtype(AnnotationMirror,
 * AnnotationMirror)}.
 *
 * @see DefaultQualifierKindHierarchy
 * @see org.checkerframework.framework.util.DefaultQualifierKindHierarchy.DefaultQualifierKind
 */
@AnnotatedFor("nullness")
public interface QualifierKindHierarchy {

    /**
     * Returns the qualifier kinds that are the top qualifier in their hierarchies.
     *
     * @return the qualifier kinds that are the top qualifier in their hierarchies
     */
    Set<? extends QualifierKind> getTops();

    /**
     * Returns the qualifier kinds that are the bottom qualifier in their hierarchies.
     *
     * @return the qualifier kinds that are the bottom qualifier in their hierarchies
     */
    Set<? extends QualifierKind> getBottoms();

    /**
     * Returns the least upper bound of {@code q1} and {@code q2}, or {@code null} if the qualifier
     * kinds are not in the same hierarchy. Ignores elements/arguments (as QualifierKind always
     * does).
     *
     * @param q1 a qualifier kind
     * @param q2 a qualifier kind
     * @return the least upper bound of {@code q1} and {@code q2}, or {@code null} if the qualifier
     *     kinds are not in the same hierarchy
     */
    @Nullable QualifierKind leastUpperBound(QualifierKind q1, QualifierKind q2);

    /**
     * Returns the greatest lower bound of {@code q1} and {@code q2}, or {@code null} if the
     * qualifier kinds are not in the same hierarchy. Ignores elements/arguments (as QualifierKind
     * always does).
     *
     * @param q1 a qualifier kind
     * @param q2 a qualifier kind
     * @return the greatest lower bound of {@code q1} and {@code q2}, or {@code null} if the
     *     qualifier kinds are not in the same hierarchy
     */
    @Nullable QualifierKind greatestLowerBound(QualifierKind q1, QualifierKind q2);

    /**
     * Returns a list of all {@link QualifierKind}s sorted in ascending order.
     *
     * @return a list of all {@link QualifierKind}s sorted in ascending order
     */
    List<? extends QualifierKind> allQualifierKinds();

    /**
     * Returns the {@link QualifierKind} for the given annotation class name, or null if one does
     * not exist.
     *
     * @param name canonical name of an annotation class
     * @return the {@link QualifierKind} for the given annotation class name, or null if one does
     *     not exist
     */
    @Nullable QualifierKind getQualifierKind(@CanonicalName String name);

    /**
     * Returns the canonical name of {@code clazz}. Throws a {@link TypeSystemError} if {@code
     * clazz} is anonymous or otherwise does not have a name.
     *
     * @param clazz annotation class
     * @return the canonical name of {@code clazz}
     */
    static @CanonicalName String annotationClassName(Class<? extends Annotation> clazz) {
        String name = clazz.getCanonicalName();
        if (name == null) {
            throw new TypeSystemError("Qualifier classes must not be anonymous.");
        }
        return name;
    }
}
