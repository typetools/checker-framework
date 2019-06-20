package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * @see org.checkerframework.framework.util.typeinference.constraint.AFConstraint A constraint of
 *     the form: A 《 F or F 》 A
 */
public class A2F extends AFConstraint {

    public A2F(AnnotatedTypeMirror argument, AnnotatedTypeMirror formalParameter) {
        super(argument, formalParameter, 107);
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
