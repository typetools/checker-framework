package org.checkerframework.framework.util.typeinference.solver;

import java.util.Collection;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationMirrorSet;

/**
 * When one of the constraint solvers infers that a the target has a given type/target in ALL
 * qualifier hierarchies or that given an additional set of annotations that we know the target must
 * hold we have covered all hierarchies then it creates an InferredValue to represent this
 * inference.
 *
 * <p>There are subclasses to represent two cases:
 *
 * <ul>
 *   <li>The target was inferred to be an AnnotatedTypeMirror
 *   <li>The target was inferred to be equal to another target
 * </ul>
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
   * not overridden by additionalAnnotations.
   */
  public static class InferredTarget extends InferredValue {
    public final TypeVariable target;

    /**
     * Indicates that the inferred type should have these primary annotations and the remainder
     * should come from the annotations inferred for target.
     */
    public final AnnotationMirrorSet additionalAnnotations;

    public InferredTarget(
        final TypeVariable target,
        final Collection<? extends AnnotationMirror> additionalAnnotations) {
      this.target = target;
      this.additionalAnnotations = new AnnotationMirrorSet(additionalAnnotations);
    }
  }
}
