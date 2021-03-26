package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * A constraint of the form: F = A or A = F
 *
 * @see org.checkerframework.framework.util.typeinference.constraint.AFConstraint
 */
public class FIsA extends AFConstraint {

  /** Create a constraint with an argument equal to a formal. */
  public FIsA(AnnotatedTypeMirror parameter, AnnotatedTypeMirror argument) {
    super(argument, parameter);
  }

  @Override
  public TUConstraint toTUConstraint() {
    return new TIsU((AnnotatedTypeVariable) formalParameter, argument, true);
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
