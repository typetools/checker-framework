package org.checkerframework.framework.type;

import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;

public class SubtypeTypeArgVisitHistory {

    private final SubtypeVisitHistory trueHistory;
    private final SubtypeVisitHistory falseHistory;

    public SubtypeTypeArgVisitHistory() {
        this.trueHistory = new SubtypeVisitHistory();
        this.falseHistory = new SubtypeVisitHistory();
    }

    public void add(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror currentTop,
            Boolean b) {
        if (b) {
            trueHistory.add(type1, type2, currentTop, true);
            if (falseHistory.contains(type1, type2, currentTop)) {
                falseHistory.remove(type1, type2, currentTop);
            }
        } else {
            falseHistory.add(type1, type2, currentTop, true);
            if (trueHistory.contains(type1, type2, currentTop)) {
                trueHistory.remove(type1, type2, currentTop);
            }
        }
    }

    public Optional<Boolean> result(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            AnnotationMirror currentTop) {
        if (falseHistory.contains(type1, type2, currentTop)) {
            return Optional.of(false);
        } else if (trueHistory.contains(type1, type2, currentTop)) {
            return Optional.of(true);
        }
        return Optional.empty();
    }
}
