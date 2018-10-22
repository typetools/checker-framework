package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents a type qualifier hierarchy.
 *
 * <p>All method parameter annotations need to be type qualifiers recognized within this hierarchy.
 *
 * <p>This assumes that any particular annotated type in a program is annotated with at least one
 * qualifier from the hierarchy.
 */
public abstract class QualifierHierarchy {

    /**
     * Determine whether the instance is valid.
     *
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
     * Returns the width of this hierarchy, i.e. the expected number of annotations on any valid
     * type.
     */
    public int getWidth() {
        return getTopAnnotations().size();
    }

    /** @return the top (ultimate super) type qualifiers in the type system */
    public abstract Set<? extends AnnotationMirror> getTopAnnotations();

    /**
     * Return the top qualifier for the given qualifier, that is, the qualifier that is a supertype
     * of start but no further supertypes exist.
     */
    public abstract AnnotationMirror getTopAnnotation(AnnotationMirror start);

    /**
     * Return the bottom for the given qualifier, that is, the qualifier that is a subtype of start
     * but no further subtypes exist.
     */
    public abstract AnnotationMirror getBottomAnnotation(AnnotationMirror start);

    /** @return the bottom type qualifier in the hierarchy */
    public abstract Set<? extends AnnotationMirror> getBottomAnnotations();

    /**
     * @param start any qualifier from the type hierarchy
     * @return the polymorphic qualifier for that hierarchy
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
     * Tests whether rhs is equal to or a sub-qualifier of lhs, according to the type qualifier
     * hierarchy. This checks only the qualifiers, not the Java type.
     *
     * @return true iff rhs is a sub qualifier of lhs
     */
    public abstract boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs);

    /**
     * Tests whether there is any annotation in lhs that is a super qualifier of some annotation in
     * rhs. lhs and rhs contain only the annotations, not the Java type.
     *
     * @return true iff an annotation in lhs is a super of one in rhs
     */
    public abstract boolean isSubtype(
            Collection<? extends AnnotationMirror> rhs, Collection<? extends AnnotationMirror> lhs);

    /**
     * Returns the least upper bound for the qualifiers a1 and a2.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rArr; Nullable
     * </ul>
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * @return the least restrictive qualifiers for both types
     */
    public abstract AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the number of iterations dataflow should perform before {@link
     * #widenedUpperBound(AnnotationMirror, AnnotationMirror)} is called or -1 if it should never be
     * called.
     *
     * <p>Subclasses overriding this method should return some positive number or -1.
     *
     * @return the number of iterations dataflow should perform before {@link
     *     #widenedUpperBound(AnnotationMirror, AnnotationMirror)} is called or -1 if it should
     *     never be called.
     */
    public int numberOfIterationsBeforeWidening() {
        return -1;
    }

    /**
     * If the type hierarchy has an infinite ascending chain, then the dataflow analysis might never
     * reach a fixed point. To prevent this, implement this method such that it returns an upper
     * bound for the two qualifiers that is a super type and not equal to the least upper bound. If
     * this method is implemented, also override {@link #numberOfIterationsBeforeWidening()} and
     * change its return to a positive number.
     *
     * <p>{@code newQualifier} is newest qualifier dataflow computed for some expression and {@code
     * previousQualifier} is the qualifier dataflow computed on the last iteration.
     *
     * <p>If the type hierarchy has no infinite ascending chain, returns the least upper bound of
     * the two annotations.
     *
     * @param newQualifier new qualifier dataflow computed for some expression
     * @param previousQualifier the previous qualifier dataflow computed on the last iteration
     * @return an upper bound that is wider than the least upper bound of newQualifier and
     *     previousQualifier (or the lub if the type hierarchy does not require this)
     */
    public AnnotationMirror widenedUpperBound(
            AnnotationMirror newQualifier, AnnotationMirror previousQualifier) {
        return leastUpperBound(newQualifier, previousQualifier);
    }

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * <p>The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * @param a1 first annotation
     * @param a2 second annotation
     * @return greatest lower bound of the two annotations
     */
    public abstract AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the least upper bound of two types. Each type is represented as a set of type
     * qualifiers, as is the result.
     *
     * <p>Annos1 and annos2 must have the same size, and each annotation in them must be from a
     * different type hierarchy.
     *
     * <p>This is necessary for determining the type of a conditional expression ({@code ?:}), where
     * the type of the expression is the least upper bound of the true and false clauses.
     *
     * @param annos1 first collection of qualifiers
     * @param annos2 second collection of qualifiers
     * @return pairwise least upper bounds of elements of the input collections (which need not be
     *     sorted in the same order)
     */
    public Set<? extends AnnotationMirror> leastUpperBounds(
            Collection<? extends AnnotationMirror> annos1,
            Collection<? extends AnnotationMirror> annos2) {
        annos1 = replacePolyAll(annos1);
        annos2 = replacePolyAll(annos2);
        if (annos1.size() != annos2.size()) {
            throw new BugInCF(
                    "QualifierHierarchy.leastUpperBounds: tried to determine LUB with sets of different sizes.\n"
                            + "    Set 1: "
                            + annos1
                            + " Set 2: "
                            + annos2);
        }
        if (annos1.isEmpty()) {
            throw new BugInCF(
                    "QualifierHierarchy.leastUpperBounds: tried to determine LUB with empty sets");
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

        assert result.size() == annos1.size()
                : "QualifierHierarchy.leastUpperBounds: resulting set has incorrect number of annotations.\n"
                        + "    Set 1: "
                        + annos1
                        + " Set 2: "
                        + annos2
                        + " LUB: "
                        + result;

        return result;
    }

    /**
     * Returns a new set that is the passed set, but PolyAll has been replaced by a polymorphic
     * qualifiers, for hierarchies that do not have an annotation in the set.
     *
     * @param annos set of annotations
     * @return a new set with same annotations as anno, but PolyAll has been replaced with
     *     polymorphic qualifiers
     */
    protected Collection<? extends AnnotationMirror> replacePolyAll(
            Collection<? extends AnnotationMirror> annos) {
        Set<AnnotationMirror> returnAnnos = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror top : getTopAnnotations()) {
            AnnotationMirror annotationInHierarchy = findAnnotationInHierarchy(annos, top);
            if (annotationInHierarchy != null) {
                returnAnnos.add(annotationInHierarchy);
            }
        }
        return returnAnnos;
    }

    /**
     * Returns the greatest lower bound of two types. Each type is represented as a set of type
     * qualifiers, as is the result.
     *
     * <p>Annos1 and annos2 must have the same size, and each annotation in them must be from a
     * different type hierarchy.
     *
     * @param annos1 first collection of qualifiers
     * @param annos2 second collection of qualifiers
     * @return pairwise greatest lower bounds of elements of the input collections (which need not
     *     be sorted in the same order)
     */
    public Set<? extends AnnotationMirror> greatestLowerBounds(
            Collection<? extends AnnotationMirror> annos1,
            Collection<? extends AnnotationMirror> annos2) {
        if (annos1.size() != annos2.size()) {
            throw new BugInCF(
                    "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with sets of different sizes.\n"
                            + "    Set 1: "
                            + annos1
                            + " Set 2: "
                            + annos2);
        }
        if (annos1.isEmpty()) {
            throw new BugInCF(
                    "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with empty sets");
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

        assert result.size() == annos1.size()
                : "QualifierHierarchy.greatestLowerBounds: resulting set has incorrect number of annotations.\n"
                        + "    Set 1: "
                        + annos1
                        + " Set 2: "
                        + annos2
                        + " GLB: "
                        + result;

        return result;
    }

    /**
     * Tests whether {@code subAnno} is a sub-qualifier of {@code superAnno}, according to the type
     * qualifier hierarchy. This checks only the qualifiers, not the Java type.
     *
     * <p>This method works even if the underlying Java type is a type variable. In that case, a
     * 'null' AnnnotationMirror and the empty set represent a meaningful value (namely, no
     * annotation).
     *
     * @return true iff {@code subAnno} is a sub qualifier of {@code superAnno}
     */
    public abstract boolean isSubtypeTypeVariable(
            AnnotationMirror subAnno, AnnotationMirror superAnno);

    /**
     * Tests whether there is any annotation in superAnnos that is a super qualifier of some
     * annotation in subAnnos. superAnnos and subAnnos contain only the annotations, not the Java
     * type.
     *
     * <p>This method works even if the underlying Java type is a type variable. In that case, a
     * 'null' AnnnotationMirror and the empty set represent a meaningful value (namely, no
     * annotation).
     *
     * @return true iff an annotation in superAnnos is a super of one in subAnnos
     */
    // This method requires more revision.
    public abstract boolean isSubtypeTypeVariable(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos);

    /**
     * Returns the least upper bound for the qualifiers a1 and a2.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rarr; Nullable
     * </ul>
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * <p>This method works even if the underlying Java type is a type variable. In that case, a
     * 'null' AnnnotationMirror and the empty set represent a meaningful value (namely, no
     * annotation).
     *
     * @return the least restrictive qualifiers for both types
     */
    public abstract AnnotationMirror leastUpperBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * <p>The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * <p>This method works even if the underlying Java type is a type variable. In that case, a
     * 'null' AnnnotationMirror and the empty set represent a meaningful value (namely, no
     * annotation).
     *
     * @param a1 first annotation
     * @param a2 second annotation
     * @return greatest lower bound of the two annotations
     */
    public abstract AnnotationMirror greatestLowerBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the type qualifiers that are the least upper bound of the qualifiers in annos1 and
     * annos2.
     *
     * <p>This is necessary for determining the type of a conditional expression ({@code ?:}), where
     * the type of the expression is the least upper bound of the true and false clauses.
     *
     * <p>This method works even if the underlying Java type is a type variable. In that case, a
     * 'null' AnnnotationMirror and the empty set represent a meaningful value (namely, no
     * annotation).
     *
     * @return the least upper bound of annos1 and annos2
     */
    public Set<? extends AnnotationMirror> leastUpperBoundsTypeVariable(
            Collection<? extends AnnotationMirror> annos1,
            Collection<? extends AnnotationMirror> annos2) {
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
     * Returns the type qualifiers that are the greatest lower bound of the qualifiers in annos1 and
     * annos2.
     *
     * <p>The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * <p>This method works even if the underlying Java type is a type variable. In that case, a
     * 'null' AnnnotationMirror and the empty set represent a meaningful value (namely, no
     * annotation).
     *
     * @param annos1 first collection of qualifiers
     * @param annos2 second collection of qualifiers
     * @return greatest lower bound of the two collections of qualifiers
     */
    public Set<? extends AnnotationMirror> greatestLowerBoundsTypeVariable(
            Collection<? extends AnnotationMirror> annos1,
            Collection<? extends AnnotationMirror> annos2) {
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
     * Returns true if and only if the given type can have empty annotation sets (and thus the
     * *TypeVariable methods need to be used).
     */
    public static boolean canHaveEmptyAnnotationSet(AnnotatedTypeMirror type) {
        return type.getKind() == TypeKind.TYPEVAR
                || type.getKind() == TypeKind.WILDCARD
                ||
                // TODO: or should the union/intersection be the LUB of the alternatives?
                type.getKind() == TypeKind.UNION
                || type.getKind() == TypeKind.INTERSECTION;
    }

    /**
     * Tests whether {@code subAnno} is a sub-qualifier of {@code superAnno}, according to the type
     * qualifier hierarchy. This checks only the qualifiers, not the Java type.
     *
     * <p>This method takes an annotated type to decide if the type variable version of the method
     * should be invoked, or if the normal version is sufficient (which provides more strict
     * checks).
     *
     * @return true iff {@code subAnno} is a sub qualifier of {@code superAnno}
     */
    public boolean isSubtype(
            AnnotatedTypeMirror subType,
            AnnotatedTypeMirror superType,
            AnnotationMirror subAnno,
            AnnotationMirror superAnno) {
        if (canHaveEmptyAnnotationSet(subType) || canHaveEmptyAnnotationSet(superType)) {
            return isSubtypeTypeVariable(subAnno, superAnno);
        } else {
            return isSubtype(subAnno, superAnno);
        }
    }

    /**
     * Tests whether there is any annotation in {@code supers} that is a super qualifier of some
     * annotation in {@code subs}. {@code supers} and {@code subs} contain only the annotations, not
     * the Java type.
     *
     * <p>This method takes an annotated type to decide if the type variable version of the method
     * should be invoked, or if the normal version is sufficient (which provides more strict
     * checks).
     *
     * @return true iff an annotation in {@code supers} is a super of one in {@code subs}
     */
    public boolean isSubtype(
            AnnotatedTypeMirror subType,
            AnnotatedTypeMirror superType,
            Collection<? extends AnnotationMirror> subs,
            Collection<AnnotationMirror> supers) {
        if (canHaveEmptyAnnotationSet(subType) || canHaveEmptyAnnotationSet(superType)) {
            return isSubtypeTypeVariable(subs, supers);
        } else {
            return isSubtype(subs, supers);
        }
    }

    /**
     * Returns the least upper bound for the qualifiers a1 and a2.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rarr; Nullable
     * </ul>
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * <p>This method takes an annotated type to decide if the type variable version of the method
     * should be invoked, or if the normal version is sufficient (which provides more strict
     * checks).
     *
     * @return the least restrictive qualifiers for both types
     */
    public AnnotationMirror leastUpperBound(
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            AnnotationMirror a1,
            AnnotationMirror a2) {
        if (canHaveEmptyAnnotationSet(type1) || canHaveEmptyAnnotationSet(type2)) {
            return leastUpperBoundTypeVariable(a1, a2);
        } else {
            return leastUpperBound(a1, a2);
        }
    }

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * <p>The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * <p>This method takes an annotated type to decide if the type variable version of the method
     * should be invoked, or if the normal version is sufficient (which provides more strict
     * checks).
     *
     * @param a1 first annotation
     * @param a2 second annotation
     * @return greatest lower bound of the two annotations
     */
    public AnnotationMirror greatestLowerBound(
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            AnnotationMirror a1,
            AnnotationMirror a2) {
        if (canHaveEmptyAnnotationSet(type1) || canHaveEmptyAnnotationSet(type2)) {
            return greatestLowerBoundTypeVariable(a1, a2);
        } else {
            return greatestLowerBound(a1, a2);
        }
    }

    /**
     * Returns the type qualifiers that are the least upper bound of the qualifiers in annos1 and
     * annos2.
     *
     * <p>This is necessary for determining the type of a conditional expression ({@code ?:}), where
     * the type of the expression is the least upper bound of the true and false clauses.
     *
     * <p>This method takes an annotated type to decide if the type variable version of the method
     * should be invoked, or if the normal version is sufficient (which provides more strict
     * checks).
     *
     * @return the least upper bound of annos1 and annos2
     */
    public Set<? extends AnnotationMirror> leastUpperBounds(
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            Collection<? extends AnnotationMirror> annos1,
            Collection<AnnotationMirror> annos2) {
        if (canHaveEmptyAnnotationSet(type1) || canHaveEmptyAnnotationSet(type2)) {
            return leastUpperBoundsTypeVariable(annos1, annos2);
        } else {
            return leastUpperBounds(annos1, annos2);
        }
    }

    /**
     * Returns the type qualifiers that are the greatest lower bound of the qualifiers in annos1 and
     * annos2.
     *
     * <p>The two qualifiers have to be from the same qualifier hierarchy. Otherwise, null will be
     * returned.
     *
     * <p>This method takes an annotated type to decide if the type variable version of the method
     * should be invoked, or if the normal version is sufficient (which provides more strict
     * checks).
     *
     * @param annos1 first collection of qualifiers
     * @param annos2 second collection of qualifiers
     * @return greatest lower bound of the two collections of qualifiers
     */
    public Set<? extends AnnotationMirror> greatestLowerBounds(
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            Collection<? extends AnnotationMirror> annos1,
            Collection<AnnotationMirror> annos2) {
        if (canHaveEmptyAnnotationSet(type1) || canHaveEmptyAnnotationSet(type2)) {
            return greatestLowerBoundsTypeVariable(annos1, annos2);
        } else {
            return greatestLowerBounds(annos1, annos2);
        }
    }

    /**
     * Returns the annotation in annos that is in the same hierarchy as annotationMirror.
     *
     * <p>If the annotation in the hierarchy is PolyAll, then the polymorphic qualifier in the
     * hierarchy is returned instead of PolyAll.
     *
     * @param annos set of annotations to search
     * @param annotationMirror annotation that is in the same hierarchy as the returned annotation
     * @return annotation in the same hierarchy as annotationMirror, or null if one is not found
     */
    public AnnotationMirror findAnnotationInSameHierarchy(
            Collection<? extends AnnotationMirror> annos, AnnotationMirror annotationMirror) {
        AnnotationMirror top = this.getTopAnnotation(annotationMirror);
        return findAnnotationInHierarchy(annos, top);
    }

    /**
     * Returns the annotation in annos that is in the hierarchy for which annotationMirror is top.
     *
     * <p>If the annotation in the hierarchy is PolyAll, then the polymorphic qualifier in the
     * hierarchy is returned instead of PolyAll.
     *
     * @param annos set of annotations to search
     * @param top the top annotation in the hierarchy to which the returned annotation belongs
     * @return annotation in the same hierarchy as annotationMirror, or null if one is not found
     */
    public AnnotationMirror findAnnotationInHierarchy(
            Collection<? extends AnnotationMirror> annos, AnnotationMirror top) {
        boolean hasPolyAll = false;
        for (AnnotationMirror anno : annos) {
            boolean isSubtype = isSubtype(anno, top);
            if (isSubtype && AnnotationUtils.areSameByClass(anno, PolyAll.class)) {
                // If the set contains @PolyAll, only return the polymorphic qualifier if annos
                // contains no other annotation in the hierarchy.
                hasPolyAll = true;
            } else if (isSubtype) {
                return anno;
            }
        }
        if (hasPolyAll) {
            return getPolymorphicAnnotation(top);
        }
        return null;
    }

    /**
     * Update a mapping from some key to a set of AnnotationMirrors. If the key already exists in
     * the mapping and the new qualifier is in the same qualifier hierarchy as any of the existing
     * qualifiers, do nothing and return false. If the key already exists in the mapping and the new
     * qualifier is not in the same qualifier hierarchy as any of the existing qualifiers, add the
     * qualifier to the existing set and return true. If the key does not exist in the mapping, add
     * the new qualifier as a singleton set and return true.
     *
     * @param map the mapping to modify
     * @param key the key to update
     * @param newQual the value to add
     * @return whether there was a qualifier hierarchy collision
     */
    public <T> boolean updateMappingToMutableSet(
            Map<T, Set<AnnotationMirror>> map, T key, AnnotationMirror newQual) {

        if (!map.containsKey(key)) {
            Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
            set.add(newQual);
            map.put(key, set);
        } else {
            Set<AnnotationMirror> prevs = map.get(key);
            for (AnnotationMirror p : prevs) {
                if (AnnotationUtils.areSame(getTopAnnotation(p), getTopAnnotation(newQual))) {
                    return false;
                }
            }
            prevs.add(newQual);
            map.put(key, prevs);
        }
        return true;
    }
}
