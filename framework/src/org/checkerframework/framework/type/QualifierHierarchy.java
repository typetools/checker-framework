package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.interning.qual.*;
*/

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;


/**
 * Represents a type qualifier hierarchy.
 *
 * All method parameter annotations need to be type qualifiers recognized
 * within this hierarchy.
 *
 * This assumes that any particular annotated type in a program is annotated
 * with at least one qualifier from the hierarchy.
 */
public abstract class QualifierHierarchy {

    /**
     * Determine whether the instance is valid.
     * @return whether the instance is valid
     */
    public boolean isValid() {
        // For most QH the simplest check is that there are qualifiers.
        return getTypeQualifiers().size() > 0;
    }

    // **********************************************************************
    // Getter methods about this hierarchy
    // **********************************************************************

    /**
     * Returns the width of this hierarchy, i.e. the expected number of
     * annotations on any valid type.
     */
    public int getWidth() {
        return getTopAnnotations().size();
    }

    /**
     * @return  the top (ultimate super) type qualifiers in the type system
     */
    public abstract Set<? extends AnnotationMirror> getTopAnnotations();

    /**
     * Return the top qualifier for the given qualifier, that is, the qualifier
     * that is a supertype of start but no further supertypes exist.
     */
    public abstract AnnotationMirror getTopAnnotation(AnnotationMirror start);

    /**
     * Return the bottom for the given qualifier, that is, the qualifier that is a
     * subtype of start but no further subtypes exist.
     */
    public abstract AnnotationMirror getBottomAnnotation(AnnotationMirror start);

    /**
     * @return the bottom type qualifier in the hierarchy
     */
    public abstract Set<? extends AnnotationMirror> getBottomAnnotations();

    /**
     *
     * @param start Any qualifier from the type hierarchy.
     * @return The polymorphic qualifier for that hierarchy
     */
    public abstract AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start);

    /**
     * Returns all type qualifiers in this type qualifier hierarchy.
     *
     * @return the fully qualified name represented in this hierarchy
     */
    public abstract Set<? extends AnnotationMirror> getTypeQualifiers();


    // **********************************************************************
    // Qualifier Hierarchy Queries
    // **********************************************************************

    /**
     * Tests whether rhs is a sub-qualifier of lhs, according to the type
     * qualifier hierarchy. This checks only the qualifiers, not the Java type.
     *
     * @return true iff rhs is a sub qualifier of lhs
     */
    public abstract boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs);

    /**
     * Tests whether there is any annotation in lhs that is a super qualifier
     * of some annotation in rhs.
     * lhs and rhs contain only the annotations, not the Java type.
     *
     * @return true iff an annotation in lhs is a super of one in rhs
     */
    public abstract boolean isSubtype(Collection<? extends AnnotationMirror> rhs, Collection<? extends AnnotationMirror> lhs);

    /**
     * Returns the least upper bound for the qualifiers a1 and a2.
     * <p>
     *
     * Examples:
     * <ul>
     * <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rArr; Nullable</li>
     * </ul>
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * @return  the least restrictive qualifiers for both types
     */
    public abstract AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * @param a1 First annotation
     * @param a2 Second annotation
     * @return Greatest lower bound of the two annotations
     */
    public abstract AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the least upper bound of two types.  Each type is
     * represented as a set of type qualifiers, as is the result.
     * <p>
     *
     * Annos1 and annos2 must have the same size, and each annotation in
     * them must be from a different type hierarchy.
     * <p>
     *
     * This is necessary for determining the type of a conditional
     * expression (<code>?:</code>), where the type of the expression is the
     * least upper bound of the true and false clauses.
     *
     * @param annos1 First collection of qualifiers
     * @param annos2 Second collection of qualifiers
     * @return pairwise least upper bounds of elements of the input
     * collections (which need not be sorted in the same order)
     */
    public Set<? extends AnnotationMirror>
    leastUpperBounds(Collection<? extends AnnotationMirror> annos1, Collection<? extends AnnotationMirror> annos2) {
        if (annos1.size() != annos2.size()) {
            ErrorReporter.errorAbort(
            "QualifierHierarchy.leastUpperBounds: tried to determine LUB with sets of different sizes!\n" +
                    "    Set 1: " + annos1 + " Set 2: " + annos2);
        }
        if (annos1.isEmpty()) {
            throw new Error(
            "QualifierHierarchy.leastUpperBounds: tried to determine LUB with empty sets!");
        }

        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror a1 : annos1) {
            for (AnnotationMirror a2 : annos2) {
                AnnotationMirror lub = leastUpperBound(a1, a2);
                if (lub != null) {
                    result.add(lub);
                }
            }
        }

        assert result.size() == annos1.size() : "QualifierHierarchy.leastUpperBounds: resulting set has incorrect number of annotations!\n" +
                "    Set 1: " + annos1 + " Set 2: " + annos2 + " LUB: " + result;

        return result;
    }

    /**
     * Returns the greatest lower bound of two types.  Each type is
     * represented as a set of type qualifiers, as is the result.
     * <p>
     *
     * Annos1 and annos2 must have the same size, and each annotation in
     * them must be from a different type hierarchy.
     *
     * @param annos1 First collection of qualifiers
     * @param annos2 Second collection of qualifiers
     * @return pairwise greatest lower bounds of elements of the input
     * collections (which need not be sorted in the same order)
     */
    public Set<? extends AnnotationMirror>
    greatestLowerBounds(Collection<? extends AnnotationMirror> annos1, Collection<? extends AnnotationMirror> annos2) {
        if (annos1.size() != annos2.size()) {
            ErrorReporter.errorAbort(
            "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with sets of different sizes!\n" +
                    "    Set 1: " + annos1 + " Set 2: " + annos2);
        }
        if (annos1.isEmpty()) {
            ErrorReporter.errorAbort(
            "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with empty sets!");
        }

        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror a1 : annos1) {
            for (AnnotationMirror a2 : annos2) {
                AnnotationMirror glb = greatestLowerBound(a1, a2);
                if (glb != null) {
                    result.add(glb);
                }
            }
        }

        assert result.size() == annos1.size() : "QualifierHierarchy.greatestLowerBounds: resulting set has incorrect number of annotations!\n" +
                "    Set 1: " + annos1 + " Set 2: " + annos2 + " LUB: " + result;

        return result;
    }

    /**
     * Tests whether anno1 is a sub-qualifier of anno2, according to the
     * type qualifier hierarchy.  This checks only the qualifiers, not the
     * Java type.
     *
     * <p>
     * This method works even if the underlying Java type is a type variable.
     * In that case, a 'null' AnnnotationMirror and the empty set represent a meaningful
     * value (namely, no annotation).
     *
     * @return true iff anno1 is a sub qualifier of anno2
     */
    public abstract boolean isSubtypeTypeVariable(AnnotationMirror anno1, AnnotationMirror anno2);

    /**
     * Tests whether there is any annotation in lhs that is a super qualifier
     * of some annotation in rhs.
     * lhs and rhs contain only the annotations, not the Java type.
     *
     * <p>
     * This method works even if the underlying Java type is a type variable.
     * In that case, a 'null' AnnnotationMirror and the empty set represent a meaningful
     * value (namely, no annotation).
     *
     * @return true iff an annotation in lhs is a super of one in rhs
     */
    // This method requires more revision.
    public abstract boolean isSubtypeTypeVariable(Collection<? extends AnnotationMirror> rhs,
            Collection<? extends AnnotationMirror> lhs);

    /**
     * Returns the least upper bound for the qualifiers a1 and a2.
     * <p>
     *
     * Examples:
     * <ul>
     * <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rarr; Nullable</li>
     * </ul>
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * <p>
     * This method works even if the underlying Java type is a type variable.
     * In that case, a 'null' AnnnotationMirror and the empty set represent a meaningful
     * value (namely, no annotation).
     *
     * @return  the least restrictive qualifiers for both types
     */
    public abstract AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * <p>
     * This method works even if the underlying Java type is a type variable.
     * In that case, a 'null' AnnnotationMirror and the empty set represent a meaningful
     * value (namely, no annotation).
     *
     * @param a1 First annotation
     * @param a2 Second annotation
     * @return Greatest lower bound of the two annotations
     */
    public abstract AnnotationMirror greatestLowerBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the type qualifiers that are the least upper bound of
     * the qualifiers in annos1 and annos2.
     * <p>
     *
     * This is necessary for determining the type of a conditional
     * expression (<code>?:</code>), where the type of the expression is the
     * least upper bound of the true and false clauses.
     *
     * <p>
     * This method works even if the underlying Java type is a type variable.
     * In that case, a 'null' AnnnotationMirror and the empty set represent a meaningful
     * value (namely, no annotation).
     *
     * @return the least upper bound of annos1 and annos2
     */
    public Set<? extends AnnotationMirror>
    leastUpperBoundsTypeVariable(Collection<? extends AnnotationMirror> annos1, Collection<? extends AnnotationMirror> annos2) {
        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror top : getTopAnnotations()) {
            AnnotationMirror anno1ForTop = null;
            for (AnnotationMirror anno1 : annos1) {
                if (isSubtypeTypeVariable(anno1, top)) {
                    anno1ForTop = anno1;
                }
            }
            AnnotationMirror anno2ForTop = null;
            for (AnnotationMirror anno2 : annos2) {
                if (isSubtypeTypeVariable(anno2, top)) {
                    anno2ForTop = anno2;
                }
            }
            AnnotationMirror t = leastUpperBoundTypeVariable(anno1ForTop, anno2ForTop);
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Returns the type qualifiers that are the greatest lower bound of
     * the qualifiers in annos1 and annos2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * <p>
     * This method works even if the underlying Java type is a type variable.
     * In that case, a 'null' AnnnotationMirror and the empty set represent a meaningful
     * value (namely, no annotation).
     *
     * @param annos1 First collection of qualifiers
     * @param annos2 Second collection of qualifiers
     * @return Greatest lower bound of the two collections of qualifiers
     */
    public Set<? extends AnnotationMirror>
    greatestLowerBoundsTypeVariable(Collection<? extends AnnotationMirror> annos1, Collection<? extends AnnotationMirror> annos2) {
        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror top : getTopAnnotations()) {
            AnnotationMirror anno1ForTop = null;
            for (AnnotationMirror anno1 : annos1) {
                if (isSubtypeTypeVariable(anno1, top)) {
                    anno1ForTop = anno1;
                }
            }
            AnnotationMirror anno2ForTop = null;
            for (AnnotationMirror anno2 : annos2) {
                if (isSubtypeTypeVariable(anno2, top)) {
                    anno2ForTop = anno2;
                }
            }
            AnnotationMirror t = greatestLowerBoundTypeVariable(anno1ForTop, anno2ForTop);
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Returns true if and only if the given type can have empty annotation sets
     * (and thus the *TypeVariable methods need to be used).
     */
    public static boolean canHaveEmptyAnnotationSet(AnnotatedTypeMirror type) {
        return type.getKind() == TypeKind.TYPEVAR || type.getKind() == TypeKind.WILDCARD ||
                // TODO: or should the union/intersection be the LUB of the alternatives?
                type.getKind() == TypeKind.UNION ||
                type.getKind() == TypeKind.INTERSECTION;
    }

    /**
     * Tests whether anno1 is a sub-qualifier of anno2, according to the
     * type qualifier hierarchy.  This checks only the qualifiers, not the
     * Java type.
     *
     * <p>
     * This method takes an annotated type to decide if the type variable version of
     * the method should be invoked, or if the normal version is sufficient (which
     * provides more strict checks).
     *
     * @return true iff anno1 is a sub qualifier of anno2
     */
    public boolean isSubtype(AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2, AnnotationMirror anno1,
            AnnotationMirror anno2) {
        if (canHaveEmptyAnnotationSet(type1)
                || canHaveEmptyAnnotationSet(type2)) {
            return isSubtypeTypeVariable(anno1, anno2);
        } else {
            return isSubtype(anno1, anno2);
        }
    }

    /**
     * Tests whether there is any annotation in lhs that is a super qualifier of
     * some annotation in rhs. lhs and rhs contain only the annotations, not the
     * Java type.
     *
     * <p>
     * This method takes an annotated type to decide if the type variable
     * version of the method should be invoked, or if the normal version is
     * sufficient (which provides more strict checks).
     *
     * @return true iff an annotation in lhs is a super of one in rhs
     */
    public boolean isSubtype(AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2, Collection<? extends AnnotationMirror> rhs,
            Collection<AnnotationMirror> lhs) {
        if (canHaveEmptyAnnotationSet(type1)
                || canHaveEmptyAnnotationSet(type2)) {
            return isSubtypeTypeVariable(rhs, lhs);
        } else {
            return isSubtype(rhs, lhs);
        }
    }

    /**
     * Returns the least upper bound for the qualifiers a1 and a2.
     * <p>
     *
     * Examples:
     * <ul>
     * <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rarr; Nullable</li>
     * </ul>
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * <p>
     * This method takes an annotated type to decide if the type variable version of
     * the method should be invoked, or if the normal version is sufficient (which
     * provides more strict checks).
     *
     * @return  the least restrictive qualifiers for both types
     */
    public AnnotationMirror leastUpperBound(AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2, AnnotationMirror a1, AnnotationMirror a2) {
        if (canHaveEmptyAnnotationSet(type1)
                || canHaveEmptyAnnotationSet(type2)) {
            return leastUpperBoundTypeVariable(a1, a2);
        } else {
            return leastUpperBound(a1, a2);
        }
    }

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * <p>
     * This method takes an annotated type to decide if the type variable version of
     * the method should be invoked, or if the normal version is sufficient (which
     * provides more strict checks).
     *
     * @param a1 First annotation
     * @param a2 Second annotation
     * @return Greatest lower bound of the two annotations
     */
    public AnnotationMirror greatestLowerBound(AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2, AnnotationMirror a1, AnnotationMirror a2) {
        if (canHaveEmptyAnnotationSet(type1)
                || canHaveEmptyAnnotationSet(type2)) {
            return greatestLowerBoundTypeVariable(a1, a2);
        } else {
            return greatestLowerBound(a1, a2);
        }
    }

    /**
     * Returns the type qualifiers that are the least upper bound of
     * the qualifiers in annos1 and annos2.
     * <p>
     *
     * This is necessary for determining the type of a conditional
     * expression (<code>?:</code>), where the type of the expression is the
     * least upper bound of the true and false clauses.
     *
     * <p>
     * This method takes an annotated type to decide if the type variable version of
     * the method should be invoked, or if the normal version is sufficient (which
     * provides more strict checks).
     *
     * @return the least upper bound of annos1 and annos2
     */
    public Set<? extends AnnotationMirror> leastUpperBounds(AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2, Collection<? extends AnnotationMirror> annos1,
            Collection<AnnotationMirror> annos2) {
        if (canHaveEmptyAnnotationSet(type1)
                || canHaveEmptyAnnotationSet(type2)) {
            return leastUpperBoundsTypeVariable(annos1, annos2);
        } else {
            return leastUpperBounds(annos1, annos2);
        }
    }

    /**
     * Returns the type qualifiers that are the greatest lower bound of
     * the qualifiers in annos1 and annos2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     *
     * <p>
     * This method takes an annotated type to decide if the type variable version of
     * the method should be invoked, or if the normal version is sufficient (which
     * provides more strict checks).
     *
     * @param annos1 First collection of qualifiers
     * @param annos2 Second collection of qualifiers
     * @return Greatest lower bound of the two collections of qualifiers
     */
    public Set<? extends AnnotationMirror> greatestLowerBounds(AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2, Collection<? extends AnnotationMirror> annos1,
            Collection<AnnotationMirror> annos2) {
        if (canHaveEmptyAnnotationSet(type1)
                || canHaveEmptyAnnotationSet(type2)) {
            return greatestLowerBoundsTypeVariable(annos1, annos2);
        } else {
            return greatestLowerBounds(annos1, annos2);
        }
    }

    public AnnotationMirror findCorrespondingAnnotation(
            AnnotationMirror aliased, Collection<? extends AnnotationMirror> a) {
        AnnotationMirror top = this.getTopAnnotation(aliased);
        for (AnnotationMirror anno : a) {
            if (this.isSubtype(anno, top)) {
                return anno;
            }
        }
        return null;
    }

    /**
     * Returns the annotation from the hierarchy identified by its 'top' annotation
     * from a set of annotations, using this QualifierHierarchy for subtype tests.
     *
     * @param annos
     *            The set of annotations.
     * @param top
     *            The top annotation of the hierarchy to consider.
     */
    public AnnotationMirror getAnnotationInHierarchy(
            Collection<? extends AnnotationMirror> annos, AnnotationMirror top) {
        AnnotationMirror annoInHierarchy = null;
        for (AnnotationMirror rhsAnno : annos) {
            if (isSubtype(rhsAnno, top)) {
                annoInHierarchy = rhsAnno;
            }
        }
        return annoInHierarchy;
    }

    /**
     * Update a mapping from some key to a set of AnnotationMirrors.
     * If the key already exists in the mapping and the new qualifier
     * is in the same qualifier hierarchy as any of the existing qualifiers,
     * do nothing and return false.
     * If the key already exists in the mapping and the new qualifier
     * is not in the same qualifier hierarchy as any of the existing qualifiers,
     * add the qualifier to the existing set and return true.
     * If the key does not exist in the mapping, add the new qualifier as a
     * singleton set and return true.
     *
     * @param map The mapping to modify.
     * @param key The key to update.
     * @param newQual The value to add.
     * @return Whether there was a qualifier hierarchy collision.
     */
    public <T> boolean updateMappingToMutableSet(
            Map<T, Set<AnnotationMirror>> map,
            T key, AnnotationMirror newQual) {

        if (!map.containsKey(key)) {
            Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
            set.add(newQual);
            map.put(key, set);
        } else {
            Set<AnnotationMirror> prevs = map.get(key);
            for (AnnotationMirror p : prevs) {
                if (AnnotationUtils.areSame(getTopAnnotation(p),
                        getTopAnnotation(newQual))) {
                    return false;
                }
            }
            prevs.add(newQual);
            map.put(key, prevs);
        }
        return true;
    }
}
