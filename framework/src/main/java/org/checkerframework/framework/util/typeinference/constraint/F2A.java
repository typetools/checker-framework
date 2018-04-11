package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * @see org.checkerframework.framework.util.typeinference.constraint.AFConstraint A constraint of
 *     the form: F 《 A or A 》 F
 */
public class F2A extends AFConstraint {

    public F2A(AnnotatedTypeMirror formalParameter, AnnotatedTypeMirror argument) {
        super(argument, formalParameter, 37);
    }

    @Override
    public TUConstraint toTUConstraint() {
        return new TSubU((AnnotatedTypeVariable) formalParameter, argument, true);
    }

    @Override
    protected F2A construct(
            AnnotatedTypeMirror newArgument, AnnotatedTypeMirror newFormalParameter) {
        return new F2A(newFormalParameter, newArgument);
    }

    @Override
    public String toString() {
        return "F2A( " + formalParameter + " << " + argument + " )";
    }
}
