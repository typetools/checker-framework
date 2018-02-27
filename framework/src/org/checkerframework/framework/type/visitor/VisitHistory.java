package org.checkerframework.framework.type.visitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

/**
 * IMPORTANT: DO NOT USE VisitHistory FOR VISITORS THAT UPDATE AN ANNOTATED TYPE MIRROR'S
 * ANNOTATIONS OR YOU VIOLATE THE CONTRACT OF equals/Hashcode. THIS CLASS IS DESIGNED FOR USE WITH
 * The DefaultTypeHierarchy AND RELATED CLASSES
 *
 * <p>VisitHistory keeps track of all visits and allows clients of this class to check whether or
 * not they have visited an equivalent pair of AnnotatedTypeMirrors already. This is necessary in
 * order to halt visiting on recursive bounds.
 *
 * <p>This class is primarily used to implement isSubtype(ATM, ATM). The pair of types corresponds
 * to the subtype and the supertype being checked. A single subtype may be visited more than once,
 * but with a different supertype. For example, if the two types are {@code @A T extends @B
 * Serializable<T>} and {@code @C Serializable<?>}, then isSubtype is first called one those types
 * and then on {@code @B Serializable<T>} and {@code @C Serializable<?>}.
 */
// After review: rename and move to org.checkerframework.framework.type
public class VisitHistory {

    // TODO: doc that only stores true subtypes
    private final Map<Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>, Set<AnnotationMirror>>
            visited;

    public VisitHistory() {
        this.visited = new HashMap<>();
    }

    public void clear() {
        visited.clear();
    }

    /** Add a visit for type1 and type2. */
    public void add(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror currentTop,
            Boolean b) {
        if (!b) {
            // We only store information about subtype relations that hold.
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

    /**
     * Returns true if type1 and type2 (or an equivalent pair) have been passed to the add method
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
