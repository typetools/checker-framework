package org.checkerframework.framework.type.visitor;

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.PluginUtil;

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
public class VisitHistory {

    private final Map<AnnotatedTypeMirror, Map<AnnotatedTypeMirror, Boolean>> visited;

    public VisitHistory() {
        this.visited = new HashMap<>();
    }

    public void clear() {
        visited.clear();
    }

    /** Add a visit for type1 and type2. */
    public void add(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2, Boolean b) {
        Map<AnnotatedTypeMirror, Boolean> hit = visited.get(type1);
        if (hit != null) {
            hit.put(type2, b);
        } else {
            hit = new HashMap<>();
            hit.put(type2, b);
            this.visited.put(type1, hit);
        }
    }

    /**
     * Returns true if type1 and type2 (or an equivalent pair) have been passed to the add method
     * previously.
     *
     * @return true if an equivalent pair has already been added to the history
     */
    public boolean contains(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        Map<AnnotatedTypeMirror, Boolean> hit = visited.get(type1);
        if (hit != null) {
            return hit.containsKey(type2);
        } else {
            return false;
        }
    }

    /**
     * Returns the value of a previous encounter of type1 and type2 or null if the pair hasn't
     * occurred before.
     */
    public Boolean getValue(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        Map<AnnotatedTypeMirror, Boolean> hit = visited.get(type1);
        if (hit != null) {
            return hit.get(type2);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "VisitHistory( " + PluginUtil.join(", ", visited.keySet()) + " )";
    }
}
