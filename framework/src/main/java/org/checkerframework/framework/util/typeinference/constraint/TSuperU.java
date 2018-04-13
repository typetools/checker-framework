package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * @see org.checkerframework.framework.util.typeinference.constraint.TUConstraint A constraint of
 *     the form: {@code T :> U}
 */
public class TSuperU extends TUConstraint {
    public TSuperU(AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType) {
        this(typeVariable, relatedType, false);
    }

    public TSuperU(
            AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType, boolean uIsArg) {
        super(typeVariable, relatedType, 167, uIsArg);
    }

    @Override
    public String toString() {
        return "TSuperU( " + typeVariable + " :> " + relatedType + " )";
    }
}
