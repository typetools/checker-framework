package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;

/**
 * When one of the constraint solvers infers that a the target has a given type/target in ALL qualifier hierarchies
 * or that given an additional set of annotations that we know the target must hold we have covered all hierarchies
 * then it creates an InferredValue to represent this inference.
 *
 * There are subclasses to represent two cases:
 *   a) The target was inferred to be an AnnotatedTypeMirror
 *   b) The target was inferred to be equal to another target
 */
public class InferredValue {
    /**
     * Indicates that a corresponding target was inferred to be the field "type" in all hierarchies.
     */
    public static class InferredType extends InferredValue {
        public final AnnotatedTypeMirror type;

        public InferredType(final AnnotatedTypeMirror type) {
            this.type = type;
        }
    }

    /**
     * Indicates that a corresponding target was inferred to be the field "target" in the hierarchies
     * not overridden by additionalAnnotations
     */
    public static class InferredTarget extends InferredValue {
        public final TypeVariable target;

        //indicates that the inferred type should have these primary annotations and the remainder
        //should come from the annotations inferred for target
        public final Set<AnnotationMirror> additionalAnnotations;

        public InferredTarget(final TypeVariable target,
                              final Collection<? extends AnnotationMirror> additionalAnnotations) {
            this.target = target;
            this.additionalAnnotations = new HashSet<>(additionalAnnotations);
        }
    }
}
