package checkers.javari;

import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.util.SubtypeRelation;

/**
 * Implements the Javari subtyping relation.
 */
public class JavariSubtypeRelation extends SubtypeRelation {

    protected AnnotationMirror READONLY, THISMUTABLE, MUTABLE, ROMAYBE, QREADONLY, ASSIGNABLE;

    /**
     * Initializes the relation with the relevant annotation mirrors
     */
    public JavariSubtypeRelation(AnnotationMirror READONLY, AnnotationMirror THISMUTABLE,
                                 AnnotationMirror MUTABLE, AnnotationMirror ROMAYBE,
                                 AnnotationMirror QREADONLY, AnnotationMirror ASSIGNABLE) {
        this.READONLY = READONLY;
        this.THISMUTABLE = THISMUTABLE;
        this.MUTABLE = MUTABLE;
        this.ROMAYBE = ROMAYBE;
        this.QREADONLY = QREADONLY;
        this.ASSIGNABLE = ASSIGNABLE;
    }

    /**
     * Implements the base-level subtype checking according to the Javari specifications.
     *
     * @param t1 the AnnotatedTypeMirror for the variable
     * @param t2 the AnnotatedTypeMirror for the value
     * @return true if the a value of type t2 can be legally assigned to a variable of type t1,
     * or false otherwise.
     */
    @Override
    protected boolean isSubtypeIgnoringTypeParameters(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {

        if (t2.hasAnnotation(MUTABLE))
            return true;
        if (t1.hasAnnotation(READONLY))
            return true;
        if (t2.hasAnnotation(READONLY) && !t1.hasAnnotation(READONLY))
            return false;
        if (t1.hasAnnotation(MUTABLE) && (t2.hasAnnotation(READONLY) || t2.hasAnnotation(ROMAYBE)))
            return false;
        return true;
    }

}
