package org.checkerframework.framework.util.typeinference.constraint;

import java.util.Set;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * A2FReducer takes an A2F constraint that is not irreducible (@see AFConstraint.isIrreducible) and
 * reduces it by one step. The resulting constraint may still be reducible.
 *
 * <p>Generally reductions should map to corresponding rules in
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.12.2.7
 */
public class A2FReducer implements AFReducer {

  protected final A2FReducingVisitor visitor;

  public A2FReducer(final AnnotatedTypeFactory typeFactory) {
    this.visitor = new A2FReducingVisitor(typeFactory);
  }

  @Override
  public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints) {
    if (constraint instanceof A2F) {
      final A2F a2f = (A2F) constraint;
      visitor.visit(a2f.argument, a2f.formalParameter, newConstraints);
      return true;

    } else {
      return false;
    }
  }

  /**
   * Given an A2F constraint of the form: A2F( typeFromMethodArgument, typeFromFormalParameter )
   *
   * <p>A2FReducingVisitor visits the constraint as follows: visit ( typeFromMethodArgument,
   * typeFromFormalParameter, newConstraints )
   *
   * <p>The visit method will determine if the given constraint should either:
   *
   * <ul>
   *   <li>be discarded -- in this case, the visitor just returns
   *   <li>reduced to a simpler constraint or set of constraints -- in this case, the new constraint
   *       or set of constraints is added to newConstraints
   * </ul>
   */
  private static class A2FReducingVisitor extends AFReducingVisitor {

    public A2FReducingVisitor(AnnotatedTypeFactory typeFactory) {
      super(A2F.class, typeFactory);
    }

    @Override
    public AFConstraint makeConstraint(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
      return new A2F(subtype, supertype);
    }

    @Override
    public AFConstraint makeInverseConstraint(
        AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
      return new F2A(subtype, supertype);
    }

    @Override
    public AFConstraint makeEqualityConstraint(
        AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
      return new FIsA(supertype, subtype);
    }
  }
}
