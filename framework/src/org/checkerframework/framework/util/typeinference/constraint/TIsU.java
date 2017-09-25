package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * @see org.checkerframework.framework.util.typeinference.constraint.TUConstraint A constraint of
 *     the form: T = U
 */
public class TIsU extends TUConstraint {
    public TIsU(AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType) {
        super(typeVariable, relatedType, 173);
    }

    public TIsU(
            AnnotatedTypeVariable typeVariable, AnnotatedTypeMirror relatedType, boolean uIsArg) {
        super(typeVariable, relatedType, 173, uIsArg);
    }

    @Override
    public String toString() {
        return "TIsU( " + typeVariable + ", " + relatedType + " )";
    }
}
