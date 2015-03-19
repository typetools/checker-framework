package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * @see org.checkerframework.framework.util.typeinference.constraint.TUConstraint
 * A constraint of the form:
 * {@code T <: U}
 */
public class TSubU extends TUConstraint {
    public TSubU(AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType) {
        super(typeVariable, relatedType, 163);
    }

    @Override
    public String toString() {
        return "TSubU( " + typeVariable + " <: " + relatedType + " )";
    }
}
