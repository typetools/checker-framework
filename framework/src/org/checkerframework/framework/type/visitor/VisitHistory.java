package org.checkerframework.framework.type.visitor;

import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * IMPORTANT: DO NOT USE VisitHistory FOR VISITORS THAT UPDATE AN ANNOTATED TYPE MIRROR'S
 * ANNOTATIONS OR YOU VIOLATE THE CONTRACT OF equals/Hashcode.  THIS CLASS IS DESIGNED FOR
 * USE WITH The DefaultTypeHierarchy AND RELATED CLASSES
 *
 * VisitHistory keeps track of all visits and allows clients of this class to check whether or
 * not they have visited a pair of AnnotatedTypeMirrors already.  This is necessary in order to
 * halt visiting on recursive bounds.  Normally, this could be done using a HashSet; this class is
 * necessary because of two properties of wildcards:
 *
 * 1) there are times when we encounter wildcards that have recursive bounds.
 * 2) Since getEffectiveSuperBound and getEffectiveExtendsBound copy the bound before returning it,
 *    two calls that should return the same bound will NOT return objects that are .equals to each other.
 *    E.g.
 *      AnnotatedWildcardType wc = ...
 *      wc.getEffectiveSuperBound().equals(wc.getEffectiveSuperBound())
 *      // the above line will return false if the super bound is a wildcard
 *      // because two wildcards are .equals only if they are also referentially (==) equal
 *      // and each call to getEffectiveSuperBound returns a copy of the original bound
 *
 * When we encounter types with property 1, property 2 ensures we cannot stop recursively comparing
 * the bounds because the equals method will not return true when we encounter a copy of a bound we have
 * already explored.  This class defines a "Visit" inner class which consists of a pair of AnnotatedTypeMirrors.
 * Its equalityCompare method compares AnnotatedTypeMirrors in a way that identifies wildcards that
 * have already been compared.
 */
public class VisitHistory {

    private final Set<Visit> visited;

    public VisitHistory() {
        this.visited = new HashSet<>();
    }

    public void clear() {
        visited.clear();
    }

    /**
     * Add a visit for type1 and type2.
     */
    public void add(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        this.visited.add(new Visit(type1, type2));
    }

    /**
     * Returns true if type1 and type2 (or an equivalent pair) have been passed to the
     * add method previously.
     * @return true if an equivalent pair has already been added to the history
     */
    public boolean contains(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        return this.visited.contains(new Visit(type1, type2));
    }

    @Override
    public String toString() {
        return "VisitHistory( " + PluginUtil.join(", ", visited) + " )";
    }

    /**
     * Visit represents a pair of types that have been added to the history.  See class note for
     * VisitHistory (at the top of this file)
     */
    private static class Visit {
        public final AnnotatedTypeMirror type1;
        public final AnnotatedTypeMirror type2;

        private Visit(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
            this.type1 = type1;
            this.type2 = type2;
        }

        @Override
        public int hashCode() {
            return (type1 != null ? 31 * type1.hashCode() : 0)
                    + (type2 != null ? 31 * type2.hashCode() : 1);
        }

        @Override
        public boolean equals(final Object oThat) {
            if (oThat == null || !oThat.getClass().equals(this.getClass())) {
                return false;
            }
            final Visit that = (Visit) oThat;
            return equalityCompare(type1, that.type1) && equalityCompare(type2, that.type2);
        }

        /**
         * This is a replacement for AnnotatedTypeMirror.equals, read the class comment for VisitHistory
         */
        private boolean equalityCompare(
                final AnnotatedTypeMirror thisType, final AnnotatedTypeMirror thatType) {
            if (thisType == null) {
                return thatType == null;
            }

            if (thatType == null) {
                return false;
            }

            if (!thisType.getClass().equals(thatType.getClass())) {
                return false;
            }

            if (thisType.getClass().equals(AnnotatedTypeMirror.AnnotatedWildcardType.class)) {
                if (thisType.getUnderlyingType().equals(thatType.getUnderlyingType())) {
                    //TODO: Investigate WHY we get wildcards that are essentially recursive since I
                    //TODO: don't think we can write these wildcards. Perhaps it is related to our lack of
                    //TODO: capture conversion or inferring void methods
                    return true; // Handles the case of recursive wildcard types
                }
                if (!AnnotationUtils.areSame(
                        thisType.getAnnotations(), thatType.getAnnotations())) {
                    return false;
                } else {
                    //TODO: EXPLAIN CASCADING .contains if we don't do it this way
                    final AnnotatedWildcardType thisWc = (AnnotatedWildcardType) thisType;
                    final AnnotatedWildcardType thatWc = (AnnotatedWildcardType) thatType;
                    return equalityCompare(thisWc.getExtendsBound(), thatWc.getExtendsBound())
                            && equalityCompare(thisWc.getSuperBound(), thatWc.getSuperBound());
                }
            }

            return thisType.equals(thatType);
        }

        @Override
        public String toString() {
            return "( " + type1 + " => " + type2 + " )";
        }
    }
}
