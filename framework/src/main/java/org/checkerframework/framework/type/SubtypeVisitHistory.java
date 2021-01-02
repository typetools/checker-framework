package org.checkerframework.framework.type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.javacutil.Pair;

/**
 * THIS CLASS IS DESIGNED FOR USE WITH DefaultTypeHierarchy, DefaultRawnessComparer, and
 * StructuralEqualityComparer ONLY.
 *
 * <p>VisitHistory tracks triples of (type1, type2, top), where type1 is a subtype of type2. It does
 * not track when type1 is not a subtype of type2; such entries are missing from the history.
 * Clients of this class can check whether or not they have visited an equivalent pair of
 * AnnotatedTypeMirrors already. This is necessary in order to halt visiting on recursive bounds.
 *
 * <p>This class is primarily used to implement isSubtype(ATM, ATM). The pair of types corresponds
 * to the subtype and the supertype being checked. A single subtype may be visited more than once,
 * but with a different supertype. For example, if the two types are {@code @A T extends @B
 * Serializable<T>} and {@code @C Serializable<?>}, then isSubtype is first called one those types
 * and then on {@code @B Serializable<T>} and {@code @C Serializable<?>}.
 */
// TODO: do we need to clear the history sometimes?
public class SubtypeVisitHistory {

    /**
     * The keys are pairs of types; the value is the set of qualifier hierarchy roots for which the
     * key is in a subtype relationship.
     */
    private final Map<Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>, Set<AnnotationMirror>>
            visited;

    public SubtypeVisitHistory() {
        this.visited = new HashMap<>();
    }

    /**
     * Put a visit for {@code type1}, {@code type2}, and {@code top} in the history. Has no effect
     * if isSubtype is false.
     *
     * @param type1 the first type
     * @param type2 the second type
     * @param currentTop the top of the relevant type hierarchy; only annotations from that
     *     hierarchy are considered
     * @param isSubtype whether {@code type1} is a subtype of {@code type2}; if false, this method
     *     does nothing
     */
    public void put(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror currentTop,
            boolean isSubtype) {
        if (!isSubtype) {
            // Only store information about subtype relations that hold.
            return;
        }
        Pair<AnnotatedTypeMirror, AnnotatedTypeMirror> key = Pair.of(type1, type2);
        Set<AnnotationMirror> hit = visited.get(key);

        if (hit != null) {
            hit.add(currentTop);
        } else {
            hit = new HashSet<>();
            hit.add(currentTop);
            this.visited.put(key, hit);
        }
    }

    /** Remove {@code type1} and {@code type2}. */
    public void remove(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror currentTop) {
        Pair<AnnotatedTypeMirror, AnnotatedTypeMirror> key = Pair.of(type1, type2);
        Set<AnnotationMirror> hit = visited.get(key);
        if (hit != null) {
            hit.remove(currentTop);
            if (hit.isEmpty()) {
                visited.remove(key);
            }
        }
    }

    /**
     * Returns true if type1 and type2 (or an equivalent pair) have been passed to the put method
     * previously.
     *
     * @return true if an equivalent pair has already been added to the history
     */
    public boolean contains(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror currentTop) {
        Pair<AnnotatedTypeMirror, AnnotatedTypeMirror> key = Pair.of(type1, type2);
        Set<AnnotationMirror> hit = visited.get(key);
        return hit != null && hit.contains(currentTop);
    }

    @Override
    public String toString() {
        return "VisitHistory( " + visited + " )";
    }
}
