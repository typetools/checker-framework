package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * A constraint of the form: {@code T <: U}
 *
 * @see org.checkerframework.framework.util.typeinference.constraint.TUConstraint
 */
public class TSubU extends TUConstraint {
    public TSubU(AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType) {
        this(typeVariable, relatedType, false);
    }

    /** Create a constraint with a variable less than a type. */
    public TSubU(
            AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType, boolean uIsArg) {
        super(typeVariable, relatedType, uIsArg);
    }

    @Override
    public String toString() {
        return "TSubU( " + typeVariable + " <: " + relatedType + " )";
    }
}
