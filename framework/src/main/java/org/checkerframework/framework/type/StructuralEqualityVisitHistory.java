package org.checkerframework.framework.type;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.AnnotationMirror;

/**
 * Stores the result of {@link StructuralEqualityComparer} for type arguments.
 *
 * <p>This is similar to {@link SubtypeVisitHistory}, but both true and false results are stored.
 */
public class StructuralEqualityVisitHistory {

    /**
     * Types in this history are structurally equal. (Use {@link SubtypeVisitHistory} because it
     * implements a {@code Map<Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>,
     * Set<AnnotationMirror>>})
     */
    private final SubtypeVisitHistory trueHistory;
    /**
     * Types in this history are not structurally equal. (Use {@link SubtypeVisitHistory} because it
     * implements a {@code Map<Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>,
     * Set<AnnotationMirror>>})
     */
    private final SubtypeVisitHistory falseHistory;

    /** Creates an empty StructuralEqualityVisitHistory. */
    public StructuralEqualityVisitHistory() {
        this.trueHistory = new SubtypeVisitHistory();
        this.falseHistory = new SubtypeVisitHistory();
    }

    /**
     * Put result of comparing {@code type1} and {@code type2} for structural equality for the given
     * hierarchy.
     *
     * @param type1 the first type
     * @param type2 the second type
     * @param hierarchy the top of the relevant type hierarchy; only annotations from that hierarchy
     *     are considered
     * @param result whether {@code type1} is structurally equal to {@code type2}
     */
    public void put(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror hierarchy,
            boolean result) {
        if (result) {
            trueHistory.put(type1, type2, hierarchy, true);
            if (falseHistory.contains(type1, type2, hierarchy)) {
                falseHistory.remove(type1, type2, hierarchy);
            }
        } else {
            falseHistory.put(type1, type2, hierarchy, true);
            if (trueHistory.contains(type1, type2, hierarchy)) {
                trueHistory.remove(type1, type2, hierarchy);
            }
        }
    }

    /**
     * Return whether or not the two types are structurally equal for the given hierarchy or {@code
     * null} if the types have not been visited for the given hierarchy.
     *
     * @param type1 the first type
     * @param type2 the second type
     * @param hierarchy the top of the relevant type hierarchy; only annotations from that hierarchy
     *     are considered
     * @return whether or not the two types are structurally equal for the given hierarchy or {@code
     *     null} if the types have not been visited for the given hierarchy
     */
    public @Nullable Boolean get(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror hierarchy) {
        if (falseHistory.contains(type1, type2, hierarchy)) {
            return false;
        } else if (trueHistory.contains(type1, type2, hierarchy)) {
            return true;
        }
        return null;
    }
}
