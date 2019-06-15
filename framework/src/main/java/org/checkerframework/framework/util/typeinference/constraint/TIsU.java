package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * A constraint of the form: T = U
 *
 * @see org.checkerframework.framework.util.typeinference.constraint.TUConstraint
 */
public class TIsU extends TUConstraint {
    public TIsU(AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType) {
        this(typeVariable, relatedType, false);
    }

    /** Create a constraint with a variable equal to a type. */
    public TIsU(
            AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType, boolean uIsArg) {
        super(typeVariable, relatedType, uIsArg);
    }

    @Override
    public String toString() {
        return "TIsU( " + typeVariable + ", " + relatedType + " )";
    }
}
