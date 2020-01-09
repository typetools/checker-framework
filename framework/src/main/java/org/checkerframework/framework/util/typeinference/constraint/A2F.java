package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * A constraint of the form: A 《 F or F 》 A
 *
 * @see org.checkerframework.framework.util.typeinference.constraint.AFConstraint
 */
public class A2F extends AFConstraint {

    /** Create a constraint with an argument less than a formal. */
    public A2F(AnnotatedTypeMirror argument, AnnotatedTypeMirror formalParameter) {
        super(argument, formalParameter);
    }

    @Override
    public TUConstraint toTUConstraint() {
        return new TSuperU((AnnotatedTypeVariable) formalParameter, argument, true);
    }

    @Override
    protected A2F construct(
            AnnotatedTypeMirror newArgument, AnnotatedTypeMirror newFormalParameter) {
        return new A2F(newArgument, newFormalParameter);
    }

    @Override
    public String toString() {
        return "A2F( " + argument + " << " + formalParameter + " )";
    }
}
