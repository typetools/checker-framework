package org.checkerframework.checker.units;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Interface that is used to specify the relation between units. A class that implements this
 * interface is the argument to the {@link org.checkerframework.checker.units.qual.UnitsRelations}
 * annotation.
 */
public interface UnitsRelations {
  /**
   * Initialize the object. Needs to be called before any other method.
   *
   * @param env the ProcessingEnvironment to use
   * @return a reference to "this"
   */
  UnitsRelations init(ProcessingEnvironment env);

  /**
   * Called for the multiplication of type lht and rht.
   *
   * @param lht left hand side in multiplication
   * @param rht right hand side in multiplication
   * @return the annotation to use for the result of the multiplication or null if no special
   *     relation is known
   */
  @Nullable AnnotationMirror multiplication(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht);

  /**
   * Called for the division of type lht and rht.
   *
   * @param lht left hand side in division
   * @param rht right hand side in division
   * @return the annotation to use for the result of the division or null if no special relation is
   *     known
   */
  @Nullable AnnotationMirror division(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht);
}
