package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * A constraint of the form: F 《 A or A 》 F
 *
 * @see org.checkerframework.framework.util.typeinference.constraint.AFConstraint
 */
public class F2A extends AFConstraint {

    /** Create a constraint with an argument greater than a formal. */
    public F2A(AnnotatedTypeMirror formalParameter, AnnotatedTypeMirror argument) {
        super(argument, formalParameter);
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
