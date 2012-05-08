package checkers.units;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotationUtils;

/**
 * Interface that is used to specify the relation between units.
 */
public interface UnitsRelations {
    /**
     * Initialize the object. Needs to be called before any other method.
     *
     * @param annos The AnnotationUtils to use.
     * @param env The ProcessingEnvironment to use.
     * @return A reference to "this".
     */
    UnitsRelations init(AnnotationUtils annos, ProcessingEnvironment env);

    /**
     * Called for the multiplication of type p1 and p2.
     *
     * @param p1 LHS in multiplication.
     * @param p2 RHS in multiplication.
     * @return The annotation to use for the result of the multiplication or
     *      null if no special relation is known.
     */
    AnnotationMirror multiplication(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2);

    /**
     * Called for the division of type p1 and p2.
     *
     * @param p1 LHS in division.
     * @param p2 RHS in division.
     * @return The annotation to use for the result of the division or
     *      null if no special relation is known.
     */
    AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2);
}