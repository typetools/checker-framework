package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

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
     * Add result of comparing {@code type1} and {@code type2} for structural equality for the given
     * hierarchy.
     */
    public void add(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror hierarchy,
            boolean result) {
        if (result) {
            trueHistory.add(type1, type2, hierarchy, true);
            if (falseHistory.contains(type1, type2, hierarchy)) {
                falseHistory.remove(type1, type2, hierarchy);
            }
        } else {
            falseHistory.add(type1, type2, hierarchy, true);
            if (trueHistory.contains(type1, type2, hierarchy)) {
                trueHistory.remove(type1, type2, hierarchy);
            }
        }
    }

    /**
     * Return whether or not the two types are structurally equal for the given hierarchy or {@code
     * null} if no result exists for the types.
     */
    public @Nullable Boolean result(
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
