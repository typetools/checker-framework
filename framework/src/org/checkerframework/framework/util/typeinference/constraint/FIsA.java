package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * @see org.checkerframework.framework.util.typeinference.constraint.AFConstraint A constraint of
 *     the form: F = A or A = F
 */
public class FIsA extends AFConstraint {

    public FIsA(AnnotatedTypeMirror parameter, AnnotatedTypeMirror argument) {
        super(argument, parameter, 101);
    }

    @Override
    public TUConstraint toTUConstraint() {
        return new TIsU((AnnotatedTypeVariable) formalParameter, argument);
    }

    @Override
    protected FIsA construct(
            AnnotatedTypeMirror newArgument, AnnotatedTypeMirror newFormalParameter) {
        return new FIsA(newFormalParameter, newArgument);
    }

    @Override
    public String toString() {
        return "FisA( " + formalParameter + " = " + argument + " )";
    }
}
