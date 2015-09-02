package org.checkerframework.checker.units;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

/**
 * Interface that is used to specify the relation between units.
 */
public interface UnitsRelations {
    /**
     * Initialize the object. Needs to be called before any other method.
     *
     * @param env The ProcessingEnvironment to use.
     * @return A reference to "this".
     */
    UnitsRelations init(ProcessingEnvironment env);

    /**
     * Called for the multiplication of type lht and rht.
     *
     * @param lht Left hand side in multiplication.
     * @param rht Right hand side in multiplication.
     * @return The annotation to use for the result of the multiplication or
     *      null if no special relation is known.
     */
    /*@Nullable*/ AnnotationMirror multiplication(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht);

    /**
     * Called for the division of type lht and rht.
     *
     * @param lht Left hand side in division.
     * @param rht Right hand side in division.
     * @return The annotation to use for the result of the division or
     *      null if no special relation is known.
     */
    /*@Nullable*/ AnnotationMirror division(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht);
}